/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>CallHistoryContactSource</tt> is the contact source for the call
 * history.
 *
 * @author Yana Stamcheva
 */
public class CallHistoryContactSource
    implements ContactSourceService
{
    private static final Logger sLog =
        Logger.getLogger(CallHistoryContactSource.class);

    /**
     * Returns the display name of this contact source.
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return CallHistoryActivator.getResources().
            getI18NString("service.gui.CALL_HISTORY");
    }

    /**
     * Queries this contact source for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString)
    {
        sLog.debug("queryContactSource " + queryString);
        return queryContactSource(queryString, 50);
    }

    /**
     * Queries this contact source for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString, int contactCount)
    {
        sLog.debug("queryContactSource2 " + queryString);
        if (queryString != null && queryString.length() > 0)
        {
            return new CallHistoryContactQuery(
                queryString,
                CallHistoryActivator.getCallHistoryService()
                    .findByPeer(queryString, contactCount));
        }
        else
        {
            return new CallHistoryContactQuery(
                queryString,
                CallHistoryActivator.getCallHistoryService()
                    .findLast(contactCount));
        }
    }

    /**
     * The <tt>CallHistoryContactQuery</tt> contains information about a current
     * query to the contact source.
     */
    private class CallHistoryContactQuery
        implements ContactQuery
    {
        /**
         * A list of all registered query listeners.
         */
        private final List<ContactQueryListener> mQueryListeners
            = new LinkedList<>();

        /**
         * A list of all source contact results.
         */
        private final List<SourceContact> mSourceContacts
            = new LinkedList<>();

        /**
         * Indicates the status of this query. When created this query is in
         * progress.
         */
        private int mStatus = QUERY_IN_PROGRESS;

        private final String mQueryString;

        /**
         * Creates an instance of <tt>CallHistoryContactQuery</tt> by specifying
         * the list of call records results.
         * @param queryString The string used for the query.
         * @param callRecords the list of call records, which are the result
         * of this query
         */
        public CallHistoryContactQuery(String queryString,
                                       Collection<CallRecord> callRecords)
        {
            mQueryString = queryString;

            Iterator<CallRecord> recordsIter = callRecords.iterator();

            while (recordsIter.hasNext() && mStatus != QUERY_CANCELED)
            {
                CallRecord record = recordsIter.next();
                sLog.debug("Add call record " + record);

                mSourceContacts.add(
                    new CallHistorySourceContact(
                        CallHistoryContactSource.this,
                        record));
            }

            if (mStatus != QUERY_CANCELED)
                mStatus = QUERY_COMPLETED;
        }

        /**
         * Adds the given <tt>ContactQueryListener</tt> to the list of query
         * listeners.
         * @param l the <tt>ContactQueryListener</tt> to add
         */
        public void addContactQueryListener(ContactQueryListener l)
        {
            synchronized (mQueryListeners)
            {
                mQueryListeners.add(l);
            }
        }

        /**
         * This query could not be canceled.
         */
        public void cancel()
        {
            mStatus = QUERY_CANCELED;
        }

        /**
         * Returns the status of this query. One of the static constants defined
         * in this class.
         * @return the status of this query
         */
        public int getStatus()
        {
            return mStatus;
        }

        /**
         * Removes the given <tt>ContactQueryListener</tt> from the list of
         * query listeners.
         * @param l the <tt>ContactQueryListener</tt> to remove
         */
        public void removeContactQueryListener(ContactQueryListener l)
        {
            synchronized (mQueryListeners)
            {
                mQueryListeners.remove(l);
            }
        }

        /**
         * Returns a list containing the results of this query.
         * @return a list containing the results of this query
         */
        public List<SourceContact> getQueryResults()
        {
            return mSourceContacts;
        }

        /**
         * Returns the <tt>ContactSourceService</tt>, where this query was first
         * initiated.
         * @return the <tt>ContactSourceService</tt>, where this query was first
         * initiated
         */
        public ContactSourceService getContactSource()
        {
            return CallHistoryContactSource.this;
        }

        public String getQueryString()
        {
            return mQueryString;
        }
    }

    /**
     * Returns default type to indicate that this contact source can be queried
     * by default filters.
     *
     * @return the type of this contact source
     */
    public int getType()
    {
        return CALL_HISTORY_TYPE;
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
