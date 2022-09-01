/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

/**
 * The <tt>ContactSourceService</tt> interface is meant to be implemented
 * by modules supporting large lists of contacts and wanting to enable searching
 * from other modules.
 *
 * @author Yana Stamcheva
 */
public interface ContactSourceService
{
    /**
     * Type of a default source.
     */
    int DEFAULT_TYPE = 0;

    /**
     * Type of a search source. Queried only when searches are performed.
     */
    int LDAP_SEARCH_TYPE = 1;

    /**
     * Type of a call history source. Queries only when call history should be
     * shown.
     */
    int CALL_HISTORY_TYPE = 2;

    /**
     * Type of a messsage history source. Queries only when chat history should
     * be shown.
     */
    int MESSAGE_HISTORY_TYPE = 3;

    /**
     * The default number of results to return to a contact source query (50).
     */
    int DEFAULT_QUERY_SIZE = 50;

    /**
     * Returns the type of this contact source.
     *
     * @return the type of this contact source
     */
    int getType();

    /**
     * Returns a user-friendly string that identifies this contact source.
     *
     * @return the display name of this contact source
     */
    String getDisplayName();

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @return the created query
     */
    ContactQuery queryContactSource(String queryString);

    /**
     * Queries this search source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    ContactQuery queryContactSource(String queryString,
                                    int contactCount);

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    int getIndex();
}
