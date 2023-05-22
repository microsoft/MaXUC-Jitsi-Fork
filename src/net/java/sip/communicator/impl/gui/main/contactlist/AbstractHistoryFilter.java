/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>AbstractHistoryFilter</tt> is a filter over the history contact
 * sources.
 */
public class AbstractHistoryFilter implements ContactListFilter
{
    /**
     * The query that we are filtering on
     */
    protected FilterQuery mFilterQuery;

    /**
     * The contact list that the filter is applied to.
     */
    protected final TreeContactList mContactList;

    /**
     * The MetaContactListSource associated with this filter's contact list.
     */
    protected final MetaContactListSource mMclSource;

    /**
     * A list of the types of history that we are filtering on
     */
    protected List<Integer> mHistoryTypes = new ArrayList<>();

    /**
     * The number of results from this query
     */
    protected int mResultCount = 0;

    /**
     * Creates an instance of AbstractHistoryFilter
     *
     * @param contactList the contact list that the filter is applied to.
     */
    public AbstractHistoryFilter(TreeContactList contactList)
    {
        mContactList = contactList;
        mMclSource = contactList.getMetaContactListSource();
    }

    @Override
    public void applyFilter(FilterQuery filterQuery)
    {
        mFilterQuery = filterQuery;

        // Get a list of all the contact sources that we're querying.
        List<UIContactSource> contactSources = new ArrayList<>();
        for (Integer historyType : mHistoryTypes)
        {
            contactSources.addAll(mContactList.getContactSources(historyType));
        }

        // A map to store UIContacts that match the query mapped to their
        // timestamp.
        Map<UIContact, Date> uiContacts = new HashMap<>();

        // A map to store the UIContacts sorted in date order according to
        // their timestamp by the HistoryResultComparator
        TreeMap<UIContact, Date> sortedUiContacts =
                new TreeMap<>(new HistoryResultComparator(uiContacts));

        for (UIContactSource contactSource : contactSources)
        {
            // Query the contact source.
            ContactQuery query = contactSource.getContactSourceService().
                queryContactSource("", ContactSourceService.DEFAULT_QUERY_SIZE);
            filterQuery.addContactQuery(query);

            // Get the timestamp for each UIContact that is returned and add it
            // to the map.
            List<SourceContact> sourceContacts = query.getQueryResults();
            for (SourceContact sourceContact : sourceContacts)
            {
                UIContact sourceUIContact =
                    contactSource.createUIContact(sourceContact, mMclSource);

                Date date = sourceContact.getTimestamp();
                uiContacts.put(sourceUIContact, date);
            }

            // Sort the UIContacts in date order.
            sortedUiContacts.putAll(uiContacts);

            // We know that this query should be finished here and we do not
            // expect any further results from it.
            filterQuery.removeQuery(query);
        }

        // Closes this filter to indicate that we finished adding queries to it.
        filterQuery.close();

        mResultCount = sortedUiContacts.size();

        addMatching(sortedUiContacts.keySet());
        mHistoryTypes.clear();
    }

    @Override
    public boolean isMatching(UIContact uiContact)
    {
        if (uiContact instanceof SourceUIContact)
        {
            SourceUIContact sourceUiContact = (SourceUIContact) uiContact;

            for (Integer historyType : mHistoryTypes)
            {
                if (sourceUiContact.getSourceContact().getContactSource().
                                                       getType() == historyType)
                    return true;
            }
        }

        return false;
    }

    @Override
    public boolean isMatching(UIGroup uiGroup)
    {
        // No group could match this filter.
        return false;
    }

    /**
     * Adds matching <tt>UIContact</tt>s to the result tree model.
     *
     * @param uiContacts the set of <tt>UIContact</tt>s to add
     */
    private void addMatching(Set<UIContact> uiContacts)
    {
        for(UIContact uiContact : uiContacts)
        {
            if (!mFilterQuery.isCanceled())
            {
                mContactList.addContact(uiContact, null, false, true);
            }
        }
    }

    /**
     * Used to compare history query reults to order them according to their
     * timestamp
     */
    private class HistoryResultComparator implements Comparator<UIContact>
    {
        private Map<UIContact, Date> mBase;

        protected HistoryResultComparator(Map<UIContact, Date> base)
        {
            mBase = base;
        }

        @Override
        public int compare(UIContact a, UIContact b)
        {
            Date date1 = mBase.get(a);
            Date date2 = mBase.get(b);

            if ((date1 == null) || (date2 == null))
                return 0;

            return date2.compareTo(date1);
        }
    }
}
