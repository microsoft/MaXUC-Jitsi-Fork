/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import java.util.*;

/**
 * The <tt>ContactQuery</tt> corresponds to a particular query made through the
 * <tt>ContactSourceService</tt>. Each query once started could be
 * canceled. One could also register a listener in order to be notified for
 * changes in query status and query contact results.
 *
 * @author Yana Stamcheva
 */
public interface ContactQuery
{
    /**
     * Indicates that this query has been completed.
     */
    int QUERY_COMPLETED = 0;

    /**
     * Indicates that this query has been canceled.
     */
    int QUERY_CANCELED = 1;

    /**
     * Indicates that this query has been stopped because of an error.
     */
    int QUERY_ERROR = 2;

    /**
     * Indicates that this query is in progress.
     */
    int QUERY_IN_PROGRESS = 3;

    /**
     * Returns the <tt>ContactSourceService</tt>, where this query was first
     * initiated.
     * @return the <tt>ContactSourceService</tt>, where this query was first
     * initiated
     */
    ContactSourceService getContactSource();

    /**
     * Returns the query string, this query was created for.
     * @return the query string, this query was created for
     */
    String getQueryString();

    /**
     * Returns the list of <tt>SourceContact</tt>s returned by this query.
     * @return the list of <tt>SourceContact</tt>s returned by this query
     */
    List<SourceContact> getQueryResults();

    /**
     * Cancels this query.
     */
    void cancel();

    /**
     * Returns the status of this query. One of the static constants QUERY_XXXX
     * defined in this class.
     * @return the status of this query
     */
    int getStatus();

    /**
     * Adds the given <tt>ContactQueryListener</tt> to the list of registered
     * listeners. The <tt>ContactQueryListener</tt> would be notified each
     * time a new <tt>ContactQuery</tt> result has been received or if the
     * query has been completed or has been canceled by user or for any other
     * reason.
     * @param l the <tt>ContactQueryListener</tt> to add
     */
    void addContactQueryListener(ContactQueryListener l);

    /**
     * Removes the given <tt>ContactQueryListener</tt> to the list of
     * registered listeners. The <tt>ContactQueryListener</tt> would be
     * notified each time a new <tt>ContactQuery</tt> result has been received
     * or if the query has been completed or has been canceled by user or for
     * any other reason.
     * @param l the <tt>ContactQueryListener</tt> to remove
     */
    void removeContactQueryListener(ContactQueryListener l);
}
