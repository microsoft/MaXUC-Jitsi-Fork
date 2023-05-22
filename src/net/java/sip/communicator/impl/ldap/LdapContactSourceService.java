/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.ldap;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>ContactSourceService</tt> for LDAP.
 * <p>
 * In contrast to other contact source implementations like AddressBook and
 * Outlook the LDAP contact source implementation is explicitly moved to the
 * "impl.ldap" package in order to allow us to create LDAP contact sources
 * for ldap directories through the LDAP service.
 * </p>
 *
 * @author Sebastien Vincent
 */
public class LdapContactSourceService
    implements ExtendedContactSourceService
{
    private static final Logger logger = Logger.getLogger(LdapContactSourceService.class);

    /**
     * If we're running in QA mode, we can log details of what the user types
     * into the search box, otherwise we can't as that may contain PII.
     */
    private final boolean mQAMode;

    /**
     * The <tt>List</tt> of <tt>LdapContactQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<LdapContactQuery> queries
        = new LinkedList<>();

    /**
     * LDAP name.
     */
    private final LdapDirectory ldapDirectory;

    /**
     * Constructor.
     *
     * @param ldapDirectory LDAP directory
     */
    public LdapContactSourceService(LdapDirectory ldapDirectory)
    {
        this.ldapDirectory = ldapDirectory;
        mQAMode = LdapActivator.getConfigurationService().global().getBoolean(
                                   "net.java.sip.communicator.QA_MODE", false);
    }

    /**
     * Queries this search source for the given <tt>searchPattern</tt>.
     *
     * @param queryPattern the pattern to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(Pattern queryPattern)
    {
        logQuery("queryContactSource", queryPattern.toString());
        return querySourceForName(queryPattern,
                LdapContactNameQuery.LDAP_MAX_RESULTS);
    }

    /**
     * Queries this search source for the given <tt>searchPattern</tt>.
     *
     * @param queryPattern the pattern to search for
     * @param count maximum number of contact returned
     * @return the created query
     */
    public ContactQuery querySourceForName(Pattern queryPattern, int count)
    {
        logQuery("querySourceForName", queryPattern.toString());
        LdapContactNameQuery query = new LdapContactNameQuery(this,
                                                              queryPattern,
                                                              count);
        querySource(query);

        return query;
    }

    /**
     * Queries LDAP for the given Pattern, searching in all number-related
     * fields.
     * @param queryPattern the pattern to search for.
     * @return the created query.
     */
    public ContactQuery querySourceForNumber(Pattern queryPattern)
    {
        logQuery("querySourceForNumber", queryPattern.toString());
        return querySourceForNumber(queryPattern,
                                    LdapContactNumberQuery.LDAP_MAX_RESULTS);
    }

    /**
     * Queries LDAP for the given Pattern, searching in all number-related
     * fields.
     * @param queryPattern the pattern to search for.
     * @param count the maximum number of contacts to return.
     * @return the created query.
     */
    public ContactQuery querySourceForNumber(Pattern queryPattern, int count)
    {
        logQuery("querySourceForNumber2", queryPattern.toString());
        LdapContactNumberQuery query =
            new LdapContactNumberQuery(this, queryPattern, count);
        querySource(query);

        return query;
    }

    /**
     * Queries LDAP with the specified query.
     * @param query The query to use.
     */
    private void querySource(LdapContactQuery query)
    {
        synchronized (queries)
        {
            queries.add(query);
        }

        boolean hasStarted = false;

        try
        {
            query.start();
            hasStarted = true;
        }
        finally
        {
            if (!hasStarted)
            {
                synchronized (queries)
                {
                    if (queries.remove(query))
                        queries.notify();
                }
            }
        }
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return ldapDirectory.getSettings().getName();
    }

    /**
     * Returns the identifier of this contact source. Some of the common
     * identifiers are defined here (For example the CALL_HISTORY identifier
     * should be returned by all call history implementations of this interface)
     * @return the identifier of this contact source
     */
    public int getType()
    {
        return LDAP_SEARCH_TYPE;
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     * @param query the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String query)
    {
        logQuery("queryContactSource2", query);
        return querySourceForName(
            Pattern.compile(query), LdapContactNameQuery.LDAP_MAX_RESULTS);
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     *
     * @param query the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery queryContactSource(String query, int contactCount)
    {
        logQuery("queryContactSource3", query);
        return querySourceForName(Pattern.compile(query), contactCount);
    }

    /**
     * Log the query, redacting if in QA mode
     * @param query
     */
    private void logQuery(String identifier, String query)
    {
        String queryToLog = mQAMode ? query : "<redacted>";
        logger.debug(identifier + " " + queryToLog);
    }

    /**
     * Stops this <tt>ContactSourceService</tt> implementation and prepares it
     * for garbage collection.
     *
     * @see AsyncContactSourceService#stop()
     */
    public void stop()
    {
        boolean interrupted = false;

        synchronized (queries)
        {
            while (!queries.isEmpty())
            {
                queries.get(0).cancel();
                try
                {
                    queries.wait();
                }
                catch (InterruptedException iex)
                {
                    interrupted = true;
                }
            }
        }
        if (interrupted)
            Thread.currentThread().interrupt();
    }

    /**
     * Get LDAP directory.
     *
     * @return LDAP directory
     */
    public LdapDirectory getLdapDirectory()
    {
        return ldapDirectory;
    }

    /**
     * Returns the phoneNumber prefix for all phone numbers.
     *
     * @return the phoneNumber prefix for all phone numbers
     */
    public String getPhoneNumberPrefix()
    {
        return ldapDirectory.getSettings().getGlobalPhonePrefix();
    }

    /**
     * Notifies this <tt>LdapContactSourceService</tt> that a specific
     * <tt>LdapContactQuery</tt> has stopped.
     *
     * @param ldapContactQuery the <tt>LdapContactQuery</tt> which has stopped
     */
    void stopped(LdapContactQuery ldapContactQuery)
    {
        synchronized (queries)
        {
            if (queries.remove(ldapContactQuery))
                queries.notify();
        }
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex()
    {
        return -1;
    }
}
