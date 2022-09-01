/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.contactsource.AsyncContactQuery;
import net.java.sip.communicator.service.contactsource.ContactDetail;
import net.java.sip.communicator.service.contactsource.ContactQuery;
import net.java.sip.communicator.service.contactsource.GenericSourceContact;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.ldap.LdapConstants;
import net.java.sip.communicator.service.ldap.LdapDirectory;
import net.java.sip.communicator.service.ldap.LdapFactory;
import net.java.sip.communicator.service.ldap.LdapPersonFound;
import net.java.sip.communicator.service.ldap.LdapQuery;
import net.java.sip.communicator.service.ldap.LdapSearchSettings;
import net.java.sip.communicator.service.ldap.event.LdapEvent;
import net.java.sip.communicator.service.ldap.event.LdapEvent.LdapEventCause;
import net.java.sip.communicator.service.ldap.event.LdapListener;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.PhoneNumberI18nService;

/**
 * Implements <tt>ContactQuery</tt> for LDAP.
 * <p>
 * In contrast to other contact source implementations like AddressBook and
 * Outlook the LDAP contact source implementation is explicitly moved to the
 * "impl.ldap" package in order to allow us to create LDAP contact sources
 * for ldap directories through the LDAP service.
 * </p>
 *
 * @author Sebastien Vincent
 * @author Yana Stamcheva
 */
public abstract class LdapContactQuery
    extends AsyncContactQuery<LdapContactSourceService>
{
    /**
     * Maximum results for LDAP query.
     */
    public static final int LDAP_MAX_RESULTS = 40;

    /**
     * Maximum number of results for this instance.
     */
    private final int count;

    /**
     * LDAP query.
     */
    private LdapQuery ldapQuery = null;

    /**
     * Object lock.
     */
    private final Object objLock = new Object();

    /**
     * Initializes a new <tt>LdapContactQuery</tt> instance which is to perform
     * a specific <tt>query</tt> on behalf of a specific <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is to
     * perform the new <tt>ContactQuery</tt> instance
     * @param query the <tt>Pattern</tt> for which <tt>contactSource</tt> is
     * being queried
     * @param count maximum number of results
     */
    protected LdapContactQuery(LdapContactSourceService contactSource,
                               Pattern query,
                               int count)
    {
        super(contactSource, query);
        this.count = count;
    }

    /**
     * Performs this <tt>AsyncContactQuery</tt> in a background <tt>Thread</tt>.
     *
     * @see AsyncContactQuery#run()
     */
    @Override
    protected void run()
    {
        if (query == null)
        {
            return;
        }

        /* query we get is delimited by \Q and \E
         * and we should not query LDAP server with a too small number of
         * characters
         */
        String queryStr = query.toString();
        if(queryStr.length() < (4))
        {
            return;
        }

        /* remove \Q and \E from the Pattern */
        String queryString = queryStr.substring(2, queryStr.length() - 2);
        LdapFactory factory = LdapActivator.getFactory();

        ldapQuery = factory.createQuery(queryString);
        LdapSearchSettings settings = factory.createSearchSettings();
        settings.setDelay(250);
        settings.setMaxResults(count);

        LdapListener caller = new LdapListener()
        {
            public void ldapEventReceived(LdapEvent evt)
            {
                processLdapResponse(evt);
            }
        };

        LdapDirectory ldapDir = getContactSource().getLdapDirectory();
        if(ldapDir == null)
        {
            return;
        }

        performSearch(ldapDir, ldapQuery, caller, settings);

        synchronized(objLock)
        {
            try
            {
                objLock.wait();
            }
            catch(InterruptedException e)
            {
            }
        }
    }

    /**
     * Performs an LDAP search for the given query on the given directory,
     * informing the given listener as matches are found.
     *
     * @param ldapDir The directory to query against.
     * @param query The query to perform.
     * @param caller The listener that should be informed of search status
     * updates.
     * @param settings Optional - custom search settings.
     */
    protected void performSearch(LdapDirectory ldapDir,
                                 LdapQuery query,
                                 LdapListener caller,
                                 LdapSearchSettings settings)
    {
        ldapDir.searchPerson(query, caller, settings);
    }

    /**
     * Gets the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>.
     *
     * @param person LDAP person
     * @return the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>
     */
    private List<ContactDetail> getContactDetails(LdapPersonFound person)
    {
        List<ContactDetail> ret = new LinkedList<>();

        ret.addAll(createContactDetails(person.getMail(),
                                        ContactDetail.Category.Email,
                                        null,
                                        OperationSetPersistentPresence.class));
        ret.addAll(createContactDetails(person.getHomePhone(),
                                        ContactDetail.Category.Phone,
                                        ContactDetail.SubCategory.Home,
                                        OperationSetBasicTelephony.class,
                                        OperationSetPersistentPresence.class));
        ret.addAll(createContactDetails(person.getWorkPhone(),
                                        ContactDetail.Category.Phone,
                                        ContactDetail.SubCategory.Work,
                                        OperationSetBasicTelephony.class,
                                        OperationSetPersistentPresence.class));
        ret.addAll(createContactDetails(person.getMobilePhone(),
                                        ContactDetail.Category.Phone,
                                        ContactDetail.SubCategory.Mobile,
                                        OperationSetBasicTelephony.class,
                                        OperationSetPersistentPresence.class));
        ret.addAll(createContactDetails(person.getOtherPhone(),
                                        ContactDetail.Category.Phone,
                                        null,
                                        OperationSetBasicTelephony.class,
                                        OperationSetPersistentPresence.class));
        ret.addAll(
            createContactDetails(person.getJabberIM(),
                                 ContactDetail.Category.InstantMessaging,
                                 null,
                                 OperationSetBasicInstantMessaging.class,
                                 OperationSetPersistentPresence.class));

        return ret;
    }

    /**
     * Build a list of <tt>ContactDetail</tt>s using the specified addresses
     * and other information.
     *
     * @param addresses the addresses to make <tt>ContactDetail</tt>s for
     * @param category the category to apply to all <tt>ContactDetail</tt>s
     * @param label the labels to apply to all <tt>ContactDetail</tt>s
     * @param operationSets the <tt>OperationSet</tt>s that are supported by
     * the details
     * @return a list of <tt>ContactDetail</tt>s
     */
    @SafeVarargs
    private List<ContactDetail> createContactDetails(
        Set<String> addresses,
        ContactDetail.Category category,
        ContactDetail.SubCategory label,
        Class<? extends OperationSet>... operationSets)
    {
        List<ContactDetail> details = new LinkedList<>();

        // Make a ContactDetails for each address and add it to the list.
        for (String address : addresses)
        {
            if (category == ContactDetail.Category.Phone)
            {
                // Any phone numbers should be normalized before making into a
                // ContactDetail
                address = PhoneNumberI18nService.normalize(address);
            }

            ContactDetail detail = new ContactDetail(
                address, category, new ContactDetail.SubCategory[]{label});
            detail.setSupportedOpSets(Arrays.asList(operationSets));
            details.add(detail);
        }

        return details;
    }

    /**
     * Gets the extra details (i.e. not usable as addresses, unlike
     * <tt>ContactDetail</tt>s) to be set on a <tt>SourceContact</tt>.
     *
     * @param person LDAP person
     * @return the extra details (non-<tt>ContactDetail</tt>s) to be set on a
     * <tt>SourceContact</tt>
     */
    private Map<String, String> extractExtraDetails(LdapPersonFound person)
    {
        HashMap<String, String> details = new HashMap<>();
        details.put(LdapConstants.TITLE, person.getTitle());
        details.put(LdapConstants.FIRSTNAME, person.getFirstName());
        details.put(LdapConstants.LASTNAME, person.getSurname());
        details.put(LdapConstants.ORG, person.getOrganization());
        details.put(LdapConstants.DEPARTMENT, person.getDepartment());
        details.put(LdapConstants.LOCATION, person.getLocation());
        // It's only these 'extra details' that make their way through to UserEditableContactDisplayer.setUIDetails(),
        // so add NICKNAME here as well, even though it's also parsed in processLdapResponse().
        details.put(LdapConstants.NICKNAME, person.getDisplayName());

        return details;
    }

    /**
     * Process LDAP event.
     *
     * @param evt LDAP event
     */
    private void processLdapResponse(LdapEvent evt)
    {
        LdapEventCause cause = evt.getCause();

        if((cause == LdapEvent.LdapEventCause.SEARCH_ACHIEVED) ||
           (cause == LdapEvent.LdapEventCause.SEARCH_CANCELLED))
        {
            synchronized(objLock)
            {
                objLock.notify();
            }
        }

        if (evt.getCause() == LdapEvent.LdapEventCause.SEARCH_ERROR)
        {
            // The status must be set to QUERY_ERROR and the thread allowed to
            // continue, otherwise the query will still appear to be in
            // progress.
            setStatus(ContactQuery.QUERY_ERROR);

            synchronized(objLock)
            {
                objLock.notify();
            }
        }

        if(cause == LdapEvent.LdapEventCause.NEW_SEARCH_RESULT)
        {
            LdapPersonFound person = (LdapPersonFound) evt.getContent();
            if (person == null)
            {
                return;
            }
            AnalyticsService analytics = LdapActivator.getAnalyticsService();
            analytics.onEventWithIncrementingCount(AnalyticsEventType.LDAP_CONTACT_FOUND, new ArrayList<>());

            String displayName = person.getFirstName() + " " + person.getSurname();

            List<ContactDetail> contactDetails = getContactDetails(person);
            if (!contactDetails.isEmpty())
            {
                GenericSourceContact sourceContact
                    = new GenericSourceContact(
                            getContactSource(),
                            displayName,
                            contactDetails);

                // Extract extra details (i.e. non-ContactDetails) from the
                // LDAP person and save them as data on the source contact.
                sourceContact.setData(
                    SourceContact.DATA_EXTRA_DETAILS,
                    extractExtraDetails(person));

                if (person.getOrganization() != null)
                {
                    sourceContact.setDisplayDetails(person.getOrganization());
                }

                addQueryResult(sourceContact);
            }
        }
    }

    /**
     * Notifies this <tt>LdapContactQuery</tt> that it has stopped performing
     * in the associated background <tt>Thread</tt>.
     *
     * @param completed <tt>true</tt> if this <tt>ContactQuery</tt> has
     * successfully completed, <tt>false</tt> if an error has been encountered
     * during its execution
     * @see AsyncContactQuery#stopped(boolean)
     */
    @Override
    protected void stopped(boolean completed)
    {
        try
        {
            super.stopped(completed);
        }
        finally
        {
            getContactSource().stopped(this);
        }
    }

    /**
     * Cancels this <tt>ContactQuery</tt>.
     *
     * @see ContactQuery#cancel()
     */
    @Override
    public void cancel()
    {
        if(ldapQuery != null)
        {
            ldapQuery.setState(LdapQuery.State.CANCELLED);
        }

        synchronized(objLock)
        {
            objLock.notify();
        }
        super.cancel();
    }
}
