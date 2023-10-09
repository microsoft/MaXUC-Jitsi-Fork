/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

import org.apache.commons.lang3.*;

/**
 * The <tt>ProtocolContactSourceServiceImpl</tt>
 *
 * @author Yana Stamcheva
 */
public class ProtocolContactSourceServiceImpl
    implements ContactSourceService
{
    /**
     * The protocol provider, providing the contacts.
     */
    private final ProtocolProviderService protocolProvider;

    /**
     * The operation set class, we use to filter the capabilities of the
     * contacts.
     */
    private final Class<? extends OperationSet> opSetClass;

    /**
     * The <tt>MetaContactListService</tt>, providing the meta contact list.
     */
    MetaContactListService metaContactListService
        = GuiActivator.getContactListService();

    /**
     * The <tt>List</tt> of <tt>ProtocolContactQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<ProtocolCQuery> queries
        = new LinkedList<>();

    /**
     * Creates an instance of <tt>ProtocolContactSourceServiceImpl</tt>.
     *
     * @param protocolProvider the protocol provider which is the contact source
     * @param opSetClass the <tt>OperationSet</tt> class that is supported by
     * source contacts
     */
    public ProtocolContactSourceServiceImpl(
            ProtocolProviderService protocolProvider,
            Class<? extends OperationSet> opSetClass)
    {
        this.protocolProvider = protocolProvider;
        this.opSetClass = opSetClass;
    }

    /**
     * Returns the type of this contact source.
     *
     * @return the type of this contact source
     */
    public int getType()
    {
        return DEFAULT_TYPE;
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     *
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return GuiActivator.getResources().getI18NString("service.gui.CONTACTS")
            + " " + protocolProvider.getAccountID().getDisplayName();
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString)
    {
        return queryContactSource(queryString, -1);
    }

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery queryContactSource( String queryString,
                                            int contactCount)
    {
        if (queryString == null)
            queryString = "";

        ProtocolCQuery contactQuery = createQuery(queryString, contactCount);

        synchronized (queries)
        {
            queries.add(contactQuery);
        }

        boolean queryHasStarted = false;

        try
        {
            contactQuery.start();
            queryHasStarted = true;
        }
        finally
        {
            if (!queryHasStarted)
            {
                synchronized (queries)
                {
                    if (queries.remove(contactQuery))
                        queries.notify();
                }
            }
        }
        return contactQuery;
    }

    protected ProtocolCQuery createQuery(String queryString, int contactCount)
    {
        return new ProtocolCQuery(queryString, contactCount);
    }

    /**
     * The <tt>ProtocolCQuery</tt> performing the query for this contact source.
     */
    protected class ProtocolCQuery
        extends AsyncContactQuery<ProtocolContactSourceServiceImpl>
    {
        /**
         * The maximum number of contacts to return as result.
         */
        private int contactCount;

        /**
         * The query string used for filtering the results.
         */
        private final String queryString;

        /**
         * Creates an instance of <tt>ProtocolCQuery</tt>.
         *
         * @param queryString the query string
         * @param contactCount the maximum number of contacts to return as
         * result
         */
        public ProtocolCQuery(String queryString, int contactCount)
        {
            super(ProtocolContactSourceServiceImpl.this,
                Pattern.compile(queryString, Pattern.CASE_INSENSITIVE
                                | Pattern.LITERAL), true);

            this.queryString = queryString;
            this.contactCount = contactCount;
        }

        public void run()
        {
            Iterator<MetaContact> contactListIter
                = metaContactListService.findAllMetaContactsForProvider(
                        protocolProvider);

            while (contactListIter.hasNext())
            {
                MetaContact metaContact = contactListIter.next();

                if (getStatus() == QUERY_CANCELED)
                    return;

                this.addResultContact(metaContact);
            }

            if (getStatus() != QUERY_CANCELED)
                setStatus(QUERY_COMPLETED);
        }

        /**
         * Adds the result for the given group.
         *
         * @param metaContact the metaContact, which child protocol contacts
         * we'll be adding to the result
         */
        protected void addResultContact(MetaContact metaContact)
        {
            Iterator<Contact> contacts
                = metaContact.getContactsForProvider(protocolProvider);

            while (contacts.hasNext())
            {
                if (getStatus() == QUERY_CANCELED)
                    return;

                if(contactCount > 0 && getQueryResultCount() > contactCount)
                    break;

                Contact contact = contacts.next();
                String contactAddress = contact.getAddress();
                String contactDisplayName = contact.getDisplayName();

                if (queryString == null
                    || queryString.length() <= 0
                    || StringUtils.containsIgnoreCase(metaContact.getDisplayName(), queryString)
                    || StringUtils.containsIgnoreCase(contactAddress, queryString)
                    || StringUtils.containsIgnoreCase(contactDisplayName, queryString))
                {
                    ContactDetail contactDetail
                        = new ContactDetail(contactAddress);
                    List<Class<? extends OperationSet>> supportedOpSets
                        = new ArrayList<>();

                    supportedOpSets.add(opSetClass);
                    contactDetail.setSupportedOpSets(supportedOpSets);

                    List<ContactDetail> contactDetails
                        = new ArrayList<>();

                    contactDetails.add(contactDetail);

                    SortedGenericSourceContact sourceContact
                        = new SortedGenericSourceContact(
                                this,
                                ProtocolContactSourceServiceImpl.this,
                                contactDisplayName,
                                contactDetails);

                    if (!contactAddress.equals(contactDisplayName))
                        sourceContact.setDisplayDetails(contactAddress);

                    sourceContact.setImage(metaContact.getAvatar());
                    sourceContact.setPresenceStatus(
                            contact.getPresenceStatus());

                    // Save the MetaContact as a data entry on the Source
                    // Contact so it can be retrieved later
                    sourceContact.setData(SourceContact.DATA_FILTER_META_CONTACT, metaContact);

                    addContact(sourceContact, metaContact);
                }
            }
        }

        /**
         * Adds a contact to the result
         *
         * @param sourceContact The contact to add
         * @param metaContact The meta contact that is the source of the contact
         *        that is being added
         */
        protected void addContact(SortedGenericSourceContact sourceContact,
                                  MetaContact metaContact)
        {
            addQueryResult(sourceContact);
        }
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex()
    {
        return 1;
    }
}
