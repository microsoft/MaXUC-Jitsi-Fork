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
import net.java.sip.communicator.service.gui.event.*;

/**
 * The <tt>FilterQuery</tt> gives information about a current filtering.
 *
 * @author Yana Stamcheva
 */
public class UIFilterQuery
    extends FilterQuery
    implements  ContactQueryListener,
                MetaContactQueryListener
{
    /**
     * A listener, which is notified when this query finishes.
     */
    private FilterQueryListener mFilterQueryListener;

    /**
     * Indicates if the query succeeded, i.e. if any of the filters associated
     * with this query has returned any results.
     */
    private boolean mIsSucceeded = false;

    /**
     * Indicates if this query has been canceled.
     */
    private boolean mIsCanceled = false;

    /**
     * Indicates if this query is currently running.
     */
    private boolean mIsRunning = false;

    /**
     * Indicates if this query is closed, means no more queries could be added
     * to it. A <tt>FilterQuery</tt>, which is closed knows that it has to wait
     * for a final number of queries to finish before notifying interested
     * parties of the result.
     */
    private boolean mIsClosed = false;

    /**
     * The list of filter queries.
     */
    private final Map<Object, List<SourceContact>> mFilterQueries
        = Collections.synchronizedMap(
            new Hashtable<>());

    /**
     * Indicates the number of running queries.
     */
    private int mRunningQueries = 0;

    /**
     * The parent contact list of this query.
     */
    private final ContactList mContactList;

    /**
     * Map of the created show more contacts for a query.
     * We stored them, so we can remove them (sometimes those
     * contacts are not added to UI, so they are not removed and
     * not cleared)
     */
    private final Map<ContactQuery,ShowMoreContact> mShowMoreContactMap
        = new HashMap<>();

    /**
     * Creates an instance of <tt>UIFilterQuery</tt> by specifying the parent
     * <tt>ContactList</tt>.
     *
     * @param contactList the <tt>ContactList</tt> on which the query is
     * performed
     */
    public UIFilterQuery(ContactList contactList)
    {
        mContactList = contactList;
    }

    /**
     * Adds the given <tt>contactQuery</tt> to the list of mFilterQueries.
     * @param contactQuery the <tt>ContactQuery</tt> to add
     */
    public void addContactQuery(Object contactQuery)
    {
        synchronized (mFilterQueries)
        {
            // If this filter query has been already canceled and someone wants
            // to add something to it, we just cancel the incoming query and
            // return.
            if (mIsCanceled)
            {
                cancelQuery(contactQuery);
                return;
            }

            List<SourceContact> queryResults = new ArrayList<>();

            if (contactQuery instanceof ContactQuery)
            {
                ContactQuery externalQuery = (ContactQuery) contactQuery;

                List<SourceContact> externalResults
                    = externalQuery.getQueryResults();

                if (externalResults != null && externalResults.size() > 0)
                    queryResults = new ArrayList<>(externalResults);

                externalQuery.addContactQueryListener(this);
            }
            else if (contactQuery instanceof MetaContactQuery)
                ((MetaContactQuery) contactQuery).addContactQueryListener(this);

            mIsRunning = true;
            mFilterQueries.put(contactQuery, queryResults);
            mRunningQueries++;
        }
    }

    /**
     * Sets the <tt>mIsSucceeded</tt> property.
     * @param isSucceeded indicates if this query has succeeded
     */
    public void setSucceeded(boolean isSucceeded)
    {
        mIsSucceeded = isSucceeded;
    }

    /**
     * Indicates if this query has succeeded.
     * @return <tt>true</tt> if this query has succeeded, <tt>false</tt> -
     * otherwise
     */
    public boolean isSucceeded()
    {
        return mIsSucceeded;
    }

    /**
     * Indicates if this query is canceled.
     * @return <tt>true</tt> if this query is canceled, <tt>false</tt> otherwise
     */
    public boolean isCanceled()
    {
        synchronized (mFilterQueries)
        {
            return mIsCanceled;
        }
    }

    /**
     * Indicates if this query is running.
     *
     * @return <tt>true</tt> if this query is running, <tt>false</tt> otherwise
     */
    public boolean isRunning()
    {
        synchronized (mFilterQueries)
        {
            return mIsRunning;
        }
    }

    /**
     * Cancels this filter query.
     */
    public void cancel()
    {
        synchronized(mFilterQueries)
        {
            mIsCanceled = true;

            Iterator<Object> queriesIter = mFilterQueries.keySet().iterator();

            while (queriesIter.hasNext())
                cancelQuery(queriesIter.next());
        }
    }

    /**
     * Closes this query to indicate that no more contact sub-queries would be
     * added to it.
     */
    public void close()
    {
        mIsClosed = true;

        if (mRunningQueries == 0)
            fireFilterQueryEvent();
    }

    /**
     * Sets the given <tt>FilterQueryListener</tt>.
     * @param l the <tt>FilterQueryListener</tt> to set
     */
    public void setQueryListener(FilterQueryListener l)
    {
        mFilterQueryListener = l;
    }

    /**
     * Notifies the <tt>FilterQueryListener</tt> of the result status of
     * this query.
     */
    private void fireFilterQueryEvent()
    {
        mIsRunning = false;

        if (mFilterQueryListener == null)
            return;

        if (mIsSucceeded)
            mFilterQueryListener.filterQuerySucceeded(this);
        else
            mFilterQueryListener.filterQueryFailed(this);
    }

    /**
     * Indicates that a query has changed its status.
     * @param event the <tt>ContactQueryStatusEvent</tt> that notified us
     */
    public void queryStatusChanged(ContactQueryStatusEvent event)
    {
        ContactQuery query = event.getQuerySource();

        // Check if this query is in our filter queries list.
        if (!mFilterQueries.containsKey(query)
                || event.getEventType() == ContactQuery.QUERY_IN_PROGRESS)
            return;

        removeQuery(query);
    }

    /**
     * Removes the given query from this filter query, updates the related data
     * and notifies interested parties if this was the last query to process.
     * @param query the <tt>ContactQuery</tt> to remove.
     */
    public void removeQuery(ContactQuery query)
    {
        // First set the mIsSucceeded property.
        if (!isSucceeded() && !query.getQueryResults().isEmpty())
            setSucceeded(true);

        // Then remove the wait result from the filterQuery.
        mRunningQueries--;
        query.removeContactQueryListener(this);

        // If no queries have rest we notify interested listeners that query
        // has finished.
        if (mRunningQueries == 0 && mIsClosed)
            fireFilterQueryEvent();
    }

    /**
     * Indicates that a query has changed its status.
     * @param event the <tt>ContactQueryStatusEvent</tt> that notified us
     */
    public void metaContactQueryStatusChanged(MetaContactQueryStatusEvent event)
    {
        MetaContactQuery query = event.getQuerySource();

        // Check if this query is in our filter queries list.
        if (!mFilterQueries.containsKey(query))
            return;

        // First set the mIsSucceeded property.
        if (!isSucceeded() && query.getResultCount() > 0)
            setSucceeded(true);

        // We don't remove the query from our list, because even if the query
        // has finished its GUI part is scheduled in the Swing thread and we
        // don't know anything about these events, so if someone calls cancel()
        // we need to explicitly cancel all contained queries even they are
        // finished.
        mRunningQueries--;
        query.removeContactQueryListener(this);

        // If no queries have rest we notify interested listeners that query
        // has finished.
        if (mRunningQueries == 0 && mIsClosed)
            fireFilterQueryEvent();
    }

    /**
     * Cancels the given query.
     * @param query the query to cancel
     */
    private void cancelQuery(Object query)
    {
        if (query instanceof ContactQuery)
        {
            ContactQuery contactQuery = (ContactQuery) query;
            contactQuery.cancel();
            contactQuery.removeContactQueryListener(mContactList);
            if (mContactList instanceof TreeContactList)
            {
                contactQuery.removeContactQueryListener(
                    ((TreeContactList) mContactList).getSearchFilter());
            }

            if (!mIsSucceeded && contactQuery.getQueryResults().size() > 0)
                mIsSucceeded = true;

            // removes ShowMoreContact and clears it
            ShowMoreContact showMoreContact
                = mShowMoreContactMap.remove(contactQuery);
            if(showMoreContact != null)
            {
                showMoreContact.setContactNode(null);
            }
        }
        else if (query instanceof MetaContactQuery)
        {
            MetaContactQuery metaContactQuery = (MetaContactQuery) query;
            metaContactQuery.cancel();
            metaContactQuery.removeContactQueryListener(mContactList);
            if (!mIsSucceeded && metaContactQuery.getResultCount() > 0)
                mIsSucceeded = true;
        }
    }

    /**
     * Verifies if the given query is contained in this filter query.
     *
     * @param query the query we're looking for
     * @return <tt>true</tt> if the given <tt>query</tt> is contained in this
     * filter query, <tt>false</tt> - otherwise
     */
    public boolean containsQuery(Object query)
    {
        return mFilterQueries.containsKey(query);
    }

    /**
     * Indicates that a contact has been received as a result of a query.
     *
     * @param event the <tt>ContactReceivedEvent</tt> that notified us
     */
    public void contactReceived(ContactReceivedEvent event)
    {
        ContactQuery query = event.getQuerySource();
        SourceContact contact = event.getContact();

        // First set the mIsSucceeded property.
        if (!isSucceeded() && !query.getQueryResults().isEmpty())
            setSucceeded(true);

        // Inform interested listeners that this query has succeeded.
        fireFilterQueryEvent();

        List<SourceContact> queryResults = mFilterQueries.get(query);

        queryResults.add(contact);

        if (getMaxResultShown() > -1
            && queryResults.size() == getMaxResultShown())
        {
            query.removeContactQueryListener(mContactList);
            if (mContactList instanceof TreeContactList)
            {
                query.removeContactQueryListener(
                    ((TreeContactList) mContactList).getSearchFilter());
            }

            ShowMoreContact moreInfoContact = new ShowMoreContact(
                mContactList, query, queryResults, getMaxResultShown());
            mShowMoreContactMap.put(query, moreInfoContact);

            ContactSourceService contactSource = query.getContactSource();

            mContactList.addContact(
                query,
                moreInfoContact,
                mContactList.getContactSource(contactSource).getUIGroup(),
                false);
        }
    }

    /**
     * Indicates that a contact has been removed after a search.
     * @param event the <tt>ContactQueryEvent</tt> containing information
     * about the received <tt>SourceContact</tt>
     */
    public void contactRemoved(ContactRemovedEvent event)
    {}

    /**
     * Indicates that a contact has been updated after a search.
     * @param event the <tt>ContactQueryEvent</tt> containing information
     * about the updated <tt>SourceContact</tt>
     */
    public void contactChanged(ContactChangedEvent event)
    {}

    public void metaContactReceived(MetaContactQueryEvent event)
    {
        if (!isSucceeded() && event.getQuerySource().getResultCount() > 0)
            setSucceeded(true);

        // Inform interested listeners that this query has succeeded.
        fireFilterQueryEvent();
    }

    public void metaGroupReceived(MetaGroupQueryEvent event)
    {
        if (!isSucceeded() && event.getQuerySource().getResultCount() > 0)
            setSucceeded(true);

        // Inform interested listeners that this query has succeeded.
        fireFilterQueryEvent();
    }
}
