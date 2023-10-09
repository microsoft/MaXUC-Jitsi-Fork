/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.ldap;

import static net.java.sip.communicator.util.PrivacyUtils.REDACTED;

import java.util.*;

import javax.naming.*;
import javax.naming.directory.*;

import org.jitsi.util.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.ldap.event.*;
import net.java.sip.communicator.service.ldap.event.LdapEvent.LdapEventCause;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.util.*;

/**
 * An LdapDirectory stores settings for one directory server
 * and performs ldap operations (search)
 *
 * @author Sebastien Mazy
 */
public class LdapDirectoryImpl
    implements LdapDirectory,
               LdapListener,
               LdapConstants
{
    /**
     * the logger for this class
     */
    private static final Logger logger = Logger.getLogger(LdapDirectoryImpl.class);

    static
    {
        logger.setLevelTrace();
    }

    /**
     * If we're running in QA mode, we can log details of what the user types
     * into the search box, otherwise we can't as that may contain PII.
     */
    protected boolean mQAMode;

    /**
     * The settings for this directory
     */
    private LdapDirectorySettings settings;

    /**
     * The service for manipulating phone numbers.
     */
    private final PhoneNumberUtilsService phoneNumberUtilsService = LdapActivator.getPhoneNumberUtilsService();

    /**
     * Stores the pending searches
     *
     * @see LdapPendingSearch
     */
    private HashMap<LdapQuery, LdapPendingSearch> pendingSearches = new HashMap<>();

    /**
     * The characters that need to be escaped in LDAP searches, mapped to the
     * escaped version of each character.
     * These need several levels of escape, since both java strings,
     * regexes and LDAP all require the backslash to be escaped.
     */
    private static final Map<String, String> LDAP_ESCAPES = new LinkedHashMap<>();
    static
    {
        LDAP_ESCAPES.put("\\\\", "\\\\\\\\");     // \   ->   \\
        LDAP_ESCAPES.put("\\*", "\\\\\\*");       // *   ->   \*
        LDAP_ESCAPES.put("=", "\\\\=");           // =   ->   \=
        LDAP_ESCAPES.put(",", "\\\\,");           // ,   ->   \,
        LDAP_ESCAPES.put(">", "\\\\>");           // >   ->   \>
        LDAP_ESCAPES.put("<", "\\\\<");           // <   ->   \<
        LDAP_ESCAPES.put("#", "\\\\#");           // #   ->   \#
        LDAP_ESCAPES.put(";", "\\\\;");           // ;   ->   \;
        LDAP_ESCAPES.put("\"", "\\\\\"");         // "   =>   \"
    }

    /**
     * Map that contains list of attributes that match a certain type of
     * attributes (i.e. all attributes that match a mail, a home phone, a
     * mobile phones, ...).
     */
    private Map<String, List<String> > attributesMap = new
            HashMap<>();

    /**
     * List of searchable attributes relating to the name of a person, e.g.
     * first name, surname, nickname, etc.  We search these for substring
     * matches, e.g. "*first* *second*"
     */
    private static Set<String> nameAttributes = new HashSet<>();
    static
    {
        nameAttributes.add("displayName");
        nameAttributes.add("cn");
        nameAttributes.add("commonname");
        nameAttributes.add("sn");
        nameAttributes.add("surname");
        nameAttributes.add("gn");
        nameAttributes.add("givenname");
        nameAttributes.add("uid");
    }

    /**
     * List of searchable attributes relating to the address (email or IM)
     * of the person.  This is only searched if a single word is entered
     * in the search (without spaces). We search these for prefix matches
     * e.g. "email.addre*"
     */
    private static Set<String> addressAttributes = new HashSet<>();

    /**
     * List of searchable attributes relating to a person's directory number,
     * e.g. work phone, home phone, etc.  We search this for substring matches
     * e.g. "*12345*"
     */
    private static Set<String> numberAttributes = new HashSet<>();
    static
    {
        numberAttributes.add("telephoneNumber");
        numberAttributes.add("mobile");
        numberAttributes.add("homeNumber");
        numberAttributes.add("otherTelephone");
    }

    /**
     * the env HashTable stores the settings used to create
     * an InitialDirContext (i.e. connect to the LDAP directory)
     */
    private final Hashtable<String, String> env = new Hashtable<>();

    /**
     * The constructor for this class.
     * Since this element is immutable (otherwise it would be a real pain
     * to use with a Set), it takes all the settings we could need to store
     * This constructor will not modify the <tt>settings</tt>
     * or save a reference to it, but may save a clone.
     *
     * @param settings settings for this new server
     *
     * @see LdapDirectorySettings
     */
    public LdapDirectoryImpl(LdapDirectorySettings settings)
    {
        mQAMode = LdapActivator.getConfigurationService().global().getBoolean(
            "net.java.sip.communicator.QA_MODE", false);

        String portText;

        settings.validateSettings();
        this.settings = settings.clone();

        if(this.settings.getPort() == 0)
            portText = ":" + this.settings.getEncryption().defaultPort();
        portText = ":" + this.settings.getPort();

        /* fills environment for InitialDirContext */
        this.env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        this.env.put("com.sun.jndi.ldap.connect.timeout", LDAP_CONNECT_TIMEOUT);
        this.env.put("com.sun.jndi.ldap.read.timeout", LDAP_READ_TIMEOUT);
        this.env.put(Context.PROVIDER_URL, settings.getEncryption().
                protocolString() + settings.getHostname() + portText +"/");
        //connection pooling
        this.env.put("com.sun.jndi.ldap.connect.pool", "true");

        /* TODO STARTTLS */
        switch(this.settings.getEncryption())
        {
            case CLEAR:
                break;
            case SSL:
                this.env.put(Context.SECURITY_PROTOCOL, "ssl");
                this.env.put("java.naming.ldap.factory.socket",
                    LdapSSLSocketFactoryDelegate.class.getName());
                break;
        }

        /* TODO SASL */
        switch(this.settings.getAuth())
        {
            case NONE:
                this.env.put(Context.SECURITY_AUTHENTICATION, "none");
                break;
            case SIMPLE:
                this.env.put(Context.SECURITY_AUTHENTICATION, "simple");
                this.env.put(Context.SECURITY_PRINCIPAL,
                        this.settings.getBindDN());
                this.env.put(Context.SECURITY_CREDENTIALS,
                        this.settings.getPassword());
                break;
        }

        attributesMap.put(
            LdapConstants.NICKNAME, settings.getDisplayNameSearchFields());
        attributesMap.put(
            LdapConstants.FIRSTNAME, settings.getFirstNameSearchFields());
        attributesMap.put(
            LdapConstants.LASTNAME, settings.getLastNameSearchFields());
        attributesMap.put(
            LdapConstants.TITLE, settings.getTitleSearchFields());
        attributesMap.put(
            LdapConstants.ORG, settings.getOrganizationSearchFields());
        attributesMap.put(
            LdapConstants.DEPARTMENT, settings.getDepartmentSearchFields());
        attributesMap.put(
            LdapConstants.LOCATION, settings.getLocationSearchFields());
        attributesMap.put(
            LdapConstants.EMAIL, settings.getMailSearchFields());
        attributesMap.put(
            LdapConstants.WORKPHONE, settings.getWorkPhoneSearchFields());
        attributesMap.put(
            LdapConstants.MOBILEPHONE, settings.getMobilePhoneSearchFields());
        attributesMap.put(
            LdapConstants.HOMEPHONE, settings.getHomePhoneSearchFields());
        attributesMap.put(
            LdapConstants.OTHERPHONE, settings.getOtherPhoneSearchFields());
        attributesMap.put(
            LdapConstants.JABBER, settings.getJabberSearchFields());

        for(String s : settings.getMailSearchFields())
        {
            addressAttributes.add(s);
        }

        for(String s : settings.getJabberSearchFields())
        {
            addressAttributes.add(s);
            numberAttributes.add(s);
        }
    }

    /**
     * Returns an LdapDirectorySettings object containing
     * a copy of the settings of this server
     *
     * @return a copy of this server settings
     *
     * @see LdapDirectorySettings
     * @see LdapDirectory#getSettings
     */
    public LdapDirectorySettings getSettings()
    {
        return this.settings.clone();
    }

    /**
     * Connects to the remote directory
     */
    private InitialDirContext connect() throws NamingException
    {
        logger.trace("connecting to directory \"" + this + "\"");
        long time0 = System.currentTimeMillis();
        InitialDirContext dirContext =
            new InitialDirContext(this.env);
        long time1 = System.currentTimeMillis();
        logger.trace("connection to directory \"" + this + "\" took " +
                (time1-time0)  + " ms");
        return dirContext;
    }

    /**
     * Closes the LDAP connection
     */
    private void disconnect(InitialDirContext dirContext)
    {
        if (dirContext != null)
        {
            try
            {
                dirContext.close();
            }
            catch (NamingException e)
            {
                logger.error("disconnection from directory \"" + this + "\" failed!", e);
            }

            logger.trace("disconnection achieved!");
        }
    }

    /**
     * Close an LDAP response resource.
     * @param resource
     */
    private void closeLdapResource(NamingEnumeration<?> resource)
    {
        if (resource != null)
        {
            try
            {
                resource.close();
            }
            catch (NamingException e)
            {
                logger.error("Failed to close LDAP response", e);
            }
        }
    }

    /**
     * Searches a person in the directory, based on a search string.
     * Since that method might take time to process, it should be
     * implemented asynchronously and send the results (LdapPersonFound)
     * with an LdapEvent to its listeners
     *
     * @param query assumed name (can be partial) of the person searched
     * e.g. "john", "doe", "john doe"
     * @param caller the LdapListener which called the method and will
     * receive results.
     * @param searchSettings custom settings for this search, null if you
     * want to stick with the defaults
     *
     * @see LdapDirectory#searchPerson
     * @see LdapPersonFound
     * @see LdapEvent
     */
    public void searchPerson(final LdapQuery query,
        final LdapListener caller,
        LdapSearchSettings searchSettings)
    {
        searchQuery(query, caller, searchSettings, false);
    }

    /**
     * Searches a number in the directory, based on a search string.
     * Since that method might take time to process, it should be
     * implemented asynchronously and send the results (LdapPersonFound)
     * with an LdapEvent to its listeners
     *
     * @param query number to search - can be partial.
     * @param caller the LdapListener which called the method and will
     * receive results.
     * @param searchSettings custom settings for this search, null if you
     * want to stick with the defaults
     *
     * @see LdapDirectory#searchNumber
     * @see LdapPersonFound
     * @see LdapEvent
     */
    public void searchNumber(final LdapQuery query,
        final LdapListener caller,
        LdapSearchSettings searchSettings)
    {
        logger.debug("Querying LDAP for a number: query=" + query);
        searchQuery(query, caller, searchSettings, true);
    }

    /**
     * @param query term(s) to search - can be partial.
     * @param caller the LdapListener which called the method and will
     * receive results.
     * @param searchSettings custom settings for this search, null if you
     * want to stick with the defaults
     * @param searchNumber true when this is a number to search, false if a name
     */
    private void searchQuery(final LdapQuery query,
        final LdapListener caller,
        LdapSearchSettings searchSettings,
        boolean searchNumber)
    {
        if (query == null)
        {
            throw new NullPointerException("query shouldn't be null!");
        }

        if (caller == null)
        {
            throw new NullPointerException("caller shouldn't be null!");
        }

        if (searchSettings == null)
        {
            searchSettings = new LdapSearchSettingsImpl();
        }

        String baseQuery = query.toString().trim();

        String[] formattedSearchStrings;
        Set<String> searchAttributes;
        String extraSearchString = null;
        Set<String> extraSearchAttributes = null;

        if (searchNumber)
        {
            // Reformat the number in various ways (national, E164, etc) so that
            // we send at least one query in the format the LDAP server is
            // expecting.
            formattedSearchStrings = buildNumberSearchStrings(baseQuery);
            searchAttributes = numberAttributes;
        }
        else
        {
            // if the initial query string was "john d",
            // the intermediate query strings could be:
            // "john* d*" and "d* john*"
            formattedSearchStrings = buildNameSearchStrings(baseQuery);
            searchAttributes = nameAttributes;

            // Address attributes are only searched for prefix matches. They never contain a
            // space so only include them in the search if we have a single word.
            if (!baseQuery.contains(" "))
            {
                extraSearchString = sanitize(baseQuery) + "*";
                extraSearchAttributes = addressAttributes;
            }
        }

        // the servers list contains this directory as many times as
        // the number of formatted search strings
        List<LdapDirectory> serversList = new ArrayList<>();
        for (int i = 0 ; i < formattedSearchStrings.length ; i++)
        {
            serversList.add(this);
        }

        // when the pendingSearches element will be empty, all formatted search strings will have
        // been searched and the search will be finished.
        this.pendingSearches.put(query, new LdapPendingSearch(serversList, caller));

        // really performs the search
        for (String searchString : formattedSearchStrings)
        {
            String searchFilter = buildSearchFilter(searchString,
                                                    searchAttributes,
                                                    extraSearchString,
                                                    extraSearchAttributes);

            performSearchInNewThread(query, searchFilter, searchSettings, this);
        }
    }

    @VisibleForTesting
    class LDAPSearchThread extends Thread
    {
        int cancelState = 0;
        LdapQuery query;
        String searchFilter;
        LdapSearchSettings searchSettings;
        LdapListener caller;

        LDAPSearchThread(LdapQuery query,
            String searchFilter,
            LdapSearchSettings searchSettings,
            LdapListener caller)
        {
            super("LDAPSearchThread");
            this.query = query;
            this.searchFilter = searchFilter;
            this.searchSettings = searchSettings;
            this.caller = caller;
        }

        public void run()
        {
            String searchFilterToLog = mQAMode ? searchFilter : REDACTED;
            String initialQueryToLog = mQAMode ? query.toString() : REDACTED;
            logger.trace("starting search for " + searchFilterToLog +
                    " (initial query: \"" + initialQueryToLog +
                    "\") on directory \"" + LdapDirectoryImpl.this + "\"");

            SearchControls searchControls =
                buildSearchControls(searchSettings);

            LdapEvent endEvent = null;
            InitialDirContext dirContext = null;
            NamingEnumeration<?> results = null;

            try
            {
                if (searchSettings.isDelaySet())
                {
                    int delay = searchSettings.getDelay();
                    logger.debug("Sleeping for " + delay);
                    Thread.sleep(delay);
                }

                checkCancel();
                dirContext = connect();
                checkCancel();

                long time0 = System.currentTimeMillis();

                results = dirContext.search(
                        LdapDirectoryImpl.this.settings.getBaseDN(),
                        searchFilter,
                        searchControls);
                logger.debug("Retrieved some results");

                checkCancel();
                List<LdapPersonFound> resultsList = new ArrayList<>();
                NamingException namingException = null;

                try
                {
                    while (results.hasMore())
                    {
                        checkCancel();

                        SearchResult searchResult =
                            (SearchResult) results.next();
                        Map<String, Set<String>> retrievedAttributes = retrieveAttributes(searchResult);
                        LdapPersonFound person = buildPerson(
                            query,
                            searchResult.getName(),
                            retrievedAttributes);

                        // Add the results to the list of results.  An event for each person will be
                        // fired after they have been sorted.
                        resultsList.add(person);
                    }
                }
                catch (NamingException e)
                {
                    // This exception is expected if the number of
                    // results is larger than the search controls allow
                    logger.trace("Naming exception while getting results");
                    namingException = e;
                }

                logger.debug("Parsed results into list of size " + resultsList.size());
                Collections.sort(resultsList);
                logger.debug("Sorted results");

                if (resultsList.size() > searchSettings.getMaxResults())
                {
                    // We've received more contacts than we should display.
                    // Drop the last results that we don't need and prepare
                    // to fire an exception after firing the new contacts
                    logger.trace("Results exceeded expected size");
                    namingException = new SizeLimitExceededException();
                    resultsList.subList(searchSettings.getMaxResults(),
                                        resultsList.size()).clear();
                }

                logger.debug("About to fire event for some contacts: " +
                                                        resultsList.size());

                for (LdapPersonFound person : resultsList)
                {
                    LdapEvent resultEvent =
                        new LdapEvent(LdapDirectoryImpl.this,
                                LdapEvent.LdapEventCause.NEW_SEARCH_RESULT,
                                (Object) person);
                    caller.ldapEventReceived(resultEvent);
                }

                long time1 = System.currentTimeMillis();
                logger.trace("search for real query \"" + searchFilter +
                        "\" (initial query: \"" + query.toString() +
                        "\") on directory \"" + LdapDirectoryImpl.this +
                        "\" took " + (time1-time0) + "ms");

                // If we had an exception while getting the results, throw
                // it here, now that we have sent out the events for the
                // results.
                if (namingException != null)
                {
                    logger.trace("Throwing caught name exception");
                    throw namingException;
                }

                endEvent = new LdapEvent(LdapDirectoryImpl.this,
                        LdapEvent.LdapEventCause.SEARCH_ACHIEVED, query);
            }
            catch (OperationNotSupportedException e)
            {
                logger.error(
                        "use bind DN without password during search" +
                        " for real query \"" +
                        searchFilter + "\" (initial query: \"" +
                        query.toString() + "\") on directory \"" +
                        LdapDirectoryImpl.this, e);
                endEvent = new LdapEvent(
                        LdapDirectoryImpl.this,
                        LdapEvent.LdapEventCause.SEARCH_ERROR,
                        query);
            }
            catch (AuthenticationException e)
            {
                logger.error(
                        "authentication failed during search" +
                        " for real query \"" +
                        searchFilter + "\" (initial query: \"" +
                        query.toString() + "\") on directory \"" +
                        LdapDirectoryImpl.this, e);
                endEvent = new LdapEvent(
                        LdapDirectoryImpl.this,
                        LdapEvent.LdapEventCause.SEARCH_ERROR,
                        query);
            }
            catch (LimitExceededException e)
            {
                logger.info("An external exception was thrown during search for " + query, e);
                endEvent = new LdapEvent(LdapDirectoryImpl.this,
                                         LdapEventCause.SEARCH_ACHIEVED,
                                         query);
            }
            catch (NamingException e)
            {
                logger.error(
                        "an external exception was thrown during search" +
                        " for real query \"" +
                        searchFilter + "\" (initial query: \"" +
                        query.toString() + "\") on directory \"" +
                        LdapDirectoryImpl.this, e);
                endEvent = new LdapEvent(
                        LdapDirectoryImpl.this,
                        LdapEvent.LdapEventCause.SEARCH_ERROR,
                        query
                        );
            }
            catch (LdapQueryCancelledException e)
            {
                logger.trace("search for real query \"" + searchFilter +
                        "\" (initial query: \"" + query.toString() +
                        "\") on " + LdapDirectoryImpl.this +
                        " cancelled at state " + cancelState, e);
                endEvent = new LdapEvent(
                        LdapDirectoryImpl.this,
                        LdapEvent.LdapEventCause.SEARCH_CANCELLED,
                        query
                        );
            }
            catch (InterruptedException e)
            {
                logger.info("Interrupted exception while searching", e);
                // whether sleep was interrupted
                // is not that important
            }
            finally
            {
                // We should always have an end event.
                if (endEvent == null)
                {
                    endEvent = new LdapEvent(
                            LdapDirectoryImpl.this,
                            LdapEvent.LdapEventCause.SEARCH_ERROR,
                            query);
                }

                caller.ldapEventReceived(endEvent);

                closeLdapResource(results);
                disconnect(dirContext);
            }
        }

        /**
         * Checks if the query that triggered this search has been marked as cancelled. If that's
         * the case, the search thread should be stopped and this method will send a search
         * cancelled event to the search initiator. This method should be called by the search
         * thread as often as possible to quickly interrupt when needed.
         */
        private void checkCancel() throws LdapQueryCancelledException
        {
            if (query.getState() == LdapQuery.State.CANCELLED)
            {
                throw new LdapQueryCancelledException();
            }
            this.cancelState++;
        }
    }

    private void performSearchInNewThread(final LdapQuery query,
            final String searchFilter,
            final LdapSearchSettings searchSettings,
            final LdapListener caller)
    {
        LDAPSearchThread searchThread = new LDAPSearchThread(query,
            searchFilter,
            searchSettings,
            caller);

        // setting the classloader is necessary so that the BundleContext can be
        // accessed from classes instantiated from JNDI (specifically from our
        // custom SocketFactory)
        searchThread.setContextClassLoader(getClass().getClassLoader());
        searchThread.setDaemon(true);
        searchThread.start();
    }

    @VisibleForTesting
    static String[] buildNameSearchStrings(String initialQueryString)
    {
        // search for "doe john" as well as "john doe"
        String[] rawWords = initialQueryString.split(" ");

        // Escape LDAP special characters in the words; e.g. if the user types
        // an '*' then assume it is a literal rather than a wildcard.
        List<String> words = new ArrayList<>();

        for (int ii = 0; ii < rawWords.length; ii++)
        {
            if (!StringUtils.isNullOrEmpty(rawWords[ii]))
            {
                words.add(sanitize(rawWords[ii]));
            }
        }

        String[] intermediateQueryStrings;

        if (words.size() == 2)
        {
            intermediateQueryStrings = new String[2];
            intermediateQueryStrings[0] = "*" + String.join("* *", words) + "*";
            intermediateQueryStrings[1] = "*" + String.join("* *", Lists.reverse(words)) + "*";
        }
        else
        {
            // one word or too many combinations
            intermediateQueryStrings = new String[1];
            intermediateQueryStrings[0] = "*" + String.join("* *", words) + "*";
        }

        return intermediateQueryStrings;
    }

    /**
     * Takes a string representing a phone number, and builds an array of
     * strings containing the possible ways in which that string might be
     * represented on the LDAP server. No guarantee is made as to the order of
     * this array.
     * Logic is as follows:<ol>
     *  <li>Any ELC present will first be stripped.</ol>
     *  <li>The number will then be included into the array without further
     *  modifications.</li>
     *  <li>The E164 representation of the number will be added to the array.
     *  </li>
     *  <li>If the number is local to the deployment, the national format of
     *  the number will be added to the array.</li></ol>
     *
     * @param phoneNumberString The base phone number, with any external line
     *        code removed.
     * @return An array of different representations of the same phone number
     * that the LDAP server might use.
     */
    private String[] buildNumberSearchStrings(String phoneNumberString)
    {
        Set<String> combinations = new HashSet<>();
        phoneNumberString = phoneNumberUtilsService.stripELC(phoneNumberString);

        combinations.add(phoneNumberString);

        try
        {
            if (phoneNumberUtilsService.isLocal(phoneNumberString))
            {
                String nationalFormat = phoneNumberUtilsService.
                    formatNumberToSendToCommPortal(phoneNumberString);
                combinations.add(nationalFormat);
            }
        }
        catch (InvalidPhoneNumberException e)
        {
            // Assume international if we can't parse the number.
            // Failing to match is better than matching wrongly.
            logger.error("Failed to parse phone number " + phoneNumberString +
                         "; will attempt to query LDAP with it unmodified.", e);
        }

        String e164Format = phoneNumberUtilsService.formatNumberToE164(
                                                             phoneNumberString);
        combinations.add(e164Format);

        String[] combinationsArray = combinations.toArray(
                                               new String[combinations.size()]);

        // Sanitize the number search strings so that the user can't enter LDAP
        // special characters (e.g. '*88' should not match any number ending
        // in '88').
        Iterator<String> iterator = combinations.iterator();
        for (int ii = 0; ii < combinations.size(); ii++)
        {
            combinationsArray[ii] = sanitize(iterator.next()) + "*";
        }

        logger.debug("Querying LDAP in the following formats " +
                                    Arrays.toString(combinationsArray));
        return combinationsArray;
    }

    /**
     * Escapes all LDAP special characters from the given query string.
     *
     * @param rawQuery The query string to sanitize.
     * @return The sanitized form of the query string provided.
     */
    private static String sanitize(String rawQuery)
    {
        String sanitizedQuery = rawQuery;

        for (String escape : LDAP_ESCAPES.keySet())
        {
            sanitizedQuery =
                    sanitizedQuery.replaceAll(escape, LDAP_ESCAPES.get(escape));
        }

        if (!rawQuery.equals(sanitizedQuery))
        {
            logger.debug("Sanitized query " + rawQuery + " for LDAP; is now " +
                         sanitizedQuery);
        }

        return sanitizedQuery;
    }

    /**
     * Fills the retrievedAttributes map with
     * "retrievable" string attributes (name, telephone number, ...)
     *
     * @param searchResult the results to browse for attributes
     * @return the attributes in a Map
     */
    private Map<String, Set<String>> retrieveAttributes(SearchResult searchResult)
        throws NamingException
    {
        Attributes attributes = searchResult.getAttributes();
        Map<String, Set<String>> retrievedAttributes = new HashMap<>();
        NamingEnumeration<String> ids = attributes.getIDs();
        NamingEnumeration<?> values = null;

        try
        {
            while (ids.hasMore())
            {
                String id = ids.next();
                if (containsAttribute(id))
                {
                    Set<String> valuesSet = new HashSet<>();
                    retrievedAttributes.put(id, valuesSet);
                    Attribute attribute = attributes.get(id);
                    values = attribute.getAll();
                    while (values.hasMore())
                    {
                        String value = (String) values.next();
                        valuesSet.add(value);
                    }
                }
            }
        }
        finally
        {
            closeLdapResource(ids);
            closeLdapResource(values);
        }

        return retrievedAttributes;
    }

    /**
     * Builds an LdapPersonFound with the retrieved attributes
     *
     * @param query the initial query issued
     * @param dn the distinguished name of the person in the directory
     * @return the LdapPersonFoulnd built
     */
    private LdapPersonFound buildPerson(LdapQuery query, String dn, Map<String, Set<String>> retrievedAttributes)
    {
        LdapPersonFound person =
            new LdapPersonFoundImpl(LdapDirectoryImpl.this, dn, query);

        person.setTitle(
            getAttr(retrievedAttributes, LdapConstants.TITLE));
        person.setFirstName(
            getAttr(retrievedAttributes, LdapConstants.FIRSTNAME));
        person.setSurname(
            getAttr(retrievedAttributes, LdapConstants.LASTNAME));
        person.setDisplayName(
            getAttr(retrievedAttributes, LdapConstants.NICKNAME));

        if (person.getDisplayName() == null)
        {
            person.setDisplayName("" + person.getFirstName() +
                person.getSurname());
        }
        //should never happen
        if (person.getDisplayName() == null)
        {
            throw new RuntimeException("display name is null!");
        }

        person.setOrganization(
            getAttr(retrievedAttributes, LdapConstants.ORG));
        person.setDepartment(
            getAttr(retrievedAttributes, LdapConstants.DEPARTMENT));
        person.setLocation(
            getAttr(retrievedAttributes, LdapConstants.LOCATION));

        for (String emailAddress : getAllAttr(
            retrievedAttributes, LdapConstants.EMAIL))
        {
            if (!emailAddress.contains("@"))
            {
                if (settings.getMailSuffix() != null)
                {
                    emailAddress += settings.getMailSuffix();
                }
                else
                    continue;
            }
            person.addMail(emailAddress);
        }

        for (String number : getAllAttr(
            retrievedAttributes, LdapConstants.WORKPHONE))
        {
            person.addWorkPhone(number);
        }

        for (String number : getAllAttr(
            retrievedAttributes, LdapConstants.MOBILEPHONE))
        {
            person.addMobilePhone(number);
        }

        for (String number : getAllAttr(
            retrievedAttributes, LdapConstants.HOMEPHONE))
        {
            person.addHomePhone(number);
        }

        for (String number : getAllAttr(
            retrievedAttributes, LdapConstants.OTHERPHONE))
        {
            person.addOtherPhone(number);
        }

        for (String im : getAllAttr(
            retrievedAttributes, LdapConstants.JABBER))
        {
            person.addJabberIM(im);
        }

        return person;
    }

    /**
     * Get a single value for an attribute type in an LDAP result
     *
     * @param retrievedAttributes the complete set of attributes and values for
     * a single LDAP search result
     * @param attributeType the type of attribute to find a value for
     * @return the LDAP result's value for this attribute
     */
    private String getAttr(
        Map<String, Set<String>> retrievedAttributes,
        String attributeType)
    {
        String chosenKey = null;

        for (String key : attributesMap.get(attributeType))
        {
            if (retrievedAttributes.containsKey(key))
            {
                chosenKey = key;
                break;
            }
        }

        Set<String> valueSet = retrievedAttributes.get(chosenKey);
        return (valueSet == null)? null : valueSet.iterator().next();
    }

    /**
     * Get all values for an attribute type in an LDAP result
     *
     * @param retrievedAttributes the complete set of attributes and values for
     * a single LDAP search result
     * @param attributeType the type of attribute to find values for
     * @return the LDAP result's value for this attribute
     */
    private List<String> getAllAttr(
        Map<String, Set<String>> retrievedAttributes,
        String attributeType)
    {
        List<String> chosenKeys = attributesMap.get(attributeType);
        List<String> values = new ArrayList<>();

        for (String key : chosenKeys)
        {
            if (retrievedAttributes.containsKey(key))
            {
                values.addAll(retrievedAttributes.get(key));
            }
        }

        return values;
    }

    /**
     * Turns LdapDirectoryImpl into a printable object
     * Used for debugging purposes
     *
     * @return a printable string
     *
     */
    public String toString()
    {
        return this.settings.getName();
    }

    /**
     * We override the equals method so we also do for
     * hashCode to keep consistent behavior
     */
    public int hashCode()
    {
        return this.settings.getName().hashCode();
    }

    /**
     * Builds an LDAP search filter, base on the query string entered
     * e.g. (|(cn=*query*)(sn=*query*)(givenname=*query*)) if querying using
     * name attributes.
     *
     * @param query String containing the main search query to use for searching each of the keys in
     * the attributes parameter.
     * @param attributes See "query"
     * @param extraQuery Optional extra search query to use for searching each of the keys in the
     * (optional) extraAttributes parameter.
     * @param extraAttributes See "extraQuery"
     * @return an LDAP search filter
     */
    private String buildSearchFilter(String query,
        Set<String> attributes,
        String extraQuery,
        Set<String> extraAttributes)
    {
        StringBuffer searchFilter = new StringBuffer();

        searchFilter.append("(|");

        /* cn=*query* OR sn=*query* OR ... */
        for (String attribute : attributes)
        {
            searchFilter.append("(");
            searchFilter.append(attribute);
            searchFilter.append("=");
            searchFilter.append(query);
            searchFilter.append(")");
        }

        // Add the extra query if we have it.
        if ((extraQuery != null) && (extraAttributes != null))
        {
            for (String attribute : extraAttributes)
            {
                searchFilter.append("(");
                searchFilter.append(attribute);
                searchFilter.append("=");
                searchFilter.append(extraQuery);
                searchFilter.append(")");
            }
        }

        searchFilter.append(")");
        return searchFilter.toString();
    }

    private SearchControls buildSearchControls(LdapSearchSettings searchSettings)
    {
        SearchControls searchControls = new SearchControls();

        if(searchSettings.isScopeSet())
        {
            // take value from searchSettings
            searchControls.setSearchScope(
                    searchSettings.getScope().getConstant()
                    );
        }
        else
        {
            //take default from directory
            searchControls.setSearchScope(
                    this.getSettings().getScope().getConstant()
                    );
        }

        List<String> retrievableAttrs = new ArrayList<>();

        for(String key : attributesMap.keySet())
        {
            List<String> attrs = attributesMap.get(key);
            if (attrs == null)
            {
                logger.warn("No attributes found for " + key);
                logger.debug(attributesMap.toString());

                continue;
            }

            for(String attr : attrs)
            {
                retrievableAttrs.add(attr);
            }
        }

        searchControls.setReturningAttributes(retrievableAttrs.toArray(
                new String[0]));

        return searchControls;
    }

    /**
     * Required by LdapListener.
     *
     * Dispatches event received from LdapDirectory-s to
     * real search initiators (the search dialog for example)
     *
     * @param event An LdapEvent probably sent by an LdapDirectory
     */
    public synchronized void ldapEventReceived(LdapEvent event)
    {
        LdapQuery query;
        switch(event.getCause())
        {
        case NEW_SEARCH_RESULT:
            LdapPersonFound result = (LdapPersonFound) event.getContent();
            query = result.getQuery();
            if (this.pendingSearches.get(query) != null)
            {
                pendingSearches.get(query).getCaller().ldapEventReceived(event);
                String queryToLog = mQAMode ? result.getQuery().toString() : REDACTED;
                logger.trace("result event for query \"" + queryToLog + "\" forwarded");
            }
            break;
        case SEARCH_ERROR:
        case SEARCH_CANCELLED:
        case SEARCH_ACHIEVED:
            query = (LdapQuery) event.getContent();
            if(this.pendingSearches.get(query) != null)
            {
                this.pendingSearches.get(query).getPendingServers().remove(event.getSource());
                int sizeLeft = pendingSearches.get(query).
                    getPendingServers().size();
                logger.trace("end event received for initial query \"" +
                        query.toString() + "\" on directory \"" +
                        event.getSource() + "\"\nthere is " + sizeLeft +
                        " search pending for this initial query on directory \"" +
                        event.getSource() + "\"");
                if (sizeLeft == 0)
                {
                    pendingSearches.get(query).getCaller().ldapEventReceived(event);
                    pendingSearches.remove(query);
                }
            }
            break;
        }
    }

    /**
     * Returns true if Map contains <tt>attribute</tt>.
     *
     * @param attribute attribute to search
     * @return true if Map contains <tt>attribute</tt>
     */
    private boolean containsAttribute(String attribute)
    {
        for(String key : attributesMap.keySet())
        {
            List<String> attrs = attributesMap.get(key);

            if(attrs != null && attrs.contains(attribute))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * A custom exception used internally by LdapDirectoryImpl
     * to indicate that a query was cancelled
     *
     * @author Sebastien Mazy
     */
    public class LdapQueryCancelledException extends Exception
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;
    }
}
