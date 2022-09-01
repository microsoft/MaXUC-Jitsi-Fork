/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.msghistory;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The Message History Service stores messages exchanged through the various protocols
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public interface MessageHistoryService
{
    String SUBJECT = "subject";

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact after the given date
     *
     * @param metaContact MetaContact
     * @param startDate Date the start date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByStartDate(MetaContact metaContact,
                                             Date startDate)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages after the given date
     * exchanged with the given SMS number and the metacontact (if any) that
     * contains that number.
     *
     * @param smsNumber the SMS number
     * @param startDate Date the start date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByStartDate(String smsNumber,
                                             Date startDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact before the given date
     *
     * @param metaContact MetaContact
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByEndDate(MetaContact metaContact,
                                           Date endDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged before the given date with the given
     * SMS number and the metacontact (if any) that contains that number.
     *
     * @param smsNumber the SMS number
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByEndDate(String smsNumber, Date endDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged during the given period with the given
     * SMS number and the metacontact (if any) that contains that number.
     *
     * @param smsNumber the SMS number
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByPeriod(String smsNumber,
                                          Date startDate,
                                          Date endDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact between the given dates
     *
     * @param metaContact MetaContact
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByPeriod(MetaContact metaContact,
                                          Date startDate,
                                          Date endDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keyword.
     *
     * @param metaContact MetaContact
     * @param keyword keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByKeyword(MetaContact metaContact,
                                           String keyword)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages having the given keyword
     * exchanged with the given SMS number and the metacontact (if any) that
     * contains that number.
     *
     * @param smsNumber the SMS number
     * @param keyword keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByKeyword(String smsNumber,
                                           String keyword)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages exchanged by all the contacts
     * in the supplied metacontact
     *
     * @param metaContact MetaContact
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    List<MessageEvent> findLast(MetaContact metaContact, int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages exchanged by the thread
     * associated with the supplied peer
     *
     * @param peerId the remote address of the thread
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    List<MessageEvent> findLastForThread(String peerId, int count)
        throws RuntimeException;

    /**
     * Returns the last message exchanged with each chat contact / SMS number
     * in the history.  If a query string has been specified, only messages for
     * histories whose contact name, contact address or SMS number contain the
     * query sting will be returned.  The most recent messages will be returned
     * and the total number of returned messages will be <= count.
     *
     * @param queryString the string to match to the contact / SMS number
     * @param count the maximum number of messages to return
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     */
    Collection<MessageEvent> findLastForAll(String queryString, int count);

    /**
     * Returns the supplied number of messages exchanged with each unique address (thread)
     * in the history. E.g. for 4 threads, and count of 25, we send back 100 messages.
     * One-to-one messages only.
     *
     * @param count the number of messages to return for each thread
     * @return Collection of collections of MessageReceivedEvents or MessageDeliveredEvents
     */
    List<List<MessageEvent>> findLastForAllThreads(int count);

    /**
     * Find the most recent chat message in a group chat.
     *
     * @param room the chatroom to query
     * @return a ChatRoomMessageReceivedEvent or ChatRoomMessageDeliveredEvent
     *         for the message, or null if no messages were found
     */
    ChatRoomMessageEvent findLatestChatMessage(ChatRoom room);

    /**
     * Find the oldest status message in a group chat
     *
     * @param room the chatroom to query
     * @return a ChatRoomMessageReceivedEvent or ChatRoomMessageDeliveredEvent,
     *         for the message, or null if no messages were found
     */
    ChatRoomMessageEvent findOldestStatus(ChatRoom room);

    /**
     * Returns a list of the identifiers of the chat rooms that least
     * recently received a message.
     *
     * @param count the number of chat room ids to return
     * @return List of chat room ids
     */
    List<String> findLeastActiveChatrooms(int count);

    /**
     * Returns the supplied number of recent messages after the given date
     * exchanged by all the contacts in the supplied metacontact
     *
     * @param metaContact MetaContact
     * @param date messages after date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findFirstMessagesAfter(MetaContact metaContact,
                                                    Date date,
                                                    int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages before the given date
     * exchanged by all the contacts in the supplied metacontact
     *
     * @param metaContact MetaContact
     * @param date messages before date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findLastMessagesBefore(MetaContact metaContact,
                                                    Date date,
                                                    int count)
        throws RuntimeException;

   /**
     * Returns all the messages exchanged in the supplied
     * chat room after the given date
     *
     * @param room The chat room
     * @param startDate Date the start date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
   Collection<MessageEvent> findByStartDate(ChatRoom room, Date startDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged
     * in the supplied chat room before the given date
     *
     * @param chatRoomWrapper The chatRoomWrapper representing the chat room
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByEndDate(ChatRoomWrapper chatRoomWrapper,
                                           Date endDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged
     * in the supplied chat room between the given dates
     *
     * @param chatRoomWrapper The chatRoomWrapper representing the chat room
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByPeriod(ChatRoomWrapper chatRoomWrapper,
                                          Date startDate,
                                          Date endDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged
     * in the supplied chat room having the given keyword.
     *
     * @param chatRoom The chat room
     * @param keyword keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByKeyword(ChatRoom chatRoom,
                                           String keyword)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged
     * in the supplied chat room having the given keyword.
     *
     * @param chatRoomWrapper The chatRoomWrapper representing the chat room
     * @param keyword keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findByKeyword(ChatRoomWrapper chatRoomWrapper,
                                           String keyword)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages (both group and status
     * messages) exchanged in the supplied chat room
     *
     * @param room The chat room
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findLast(ChatRoom room, int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages (both group and status
     * messages) exchanged in the supplied chat room
     * @param count messages count
     * @param chatRoomWrapper The chatRoomWrapper representing the chat room
     *
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findLast(ChatRoomWrapper chatRoomWrapper,
                                      int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages after the given date
     * exchanged in the supplied chat room
     *
     * @param room The chat room
     * @param date messages after date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findFirstMessagesAfter(ChatRoom room,
                                                    Date date,
                                                    int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages before the given date
     * exchanged in the supplied chat room
     *
     * @param room The chat room
     * @param date messages before date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findLastMessagesBefore(ChatRoom room,
                                                    Date date,
                                                    int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages exchanged with the given
     * SMS number and the metacontact (if any) that contains that number.
     *
     * @param smsNumber the SMS number
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    List<MessageEvent> findLast(String smsNumber, int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages before the given date
     * exchanged with the given SMS number and the metacontact (if any) that
     * contains that number.
     *
     * @param smsNumber the SMS number
     * @param date messages before date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findLastMessagesBefore(String smsNumber,
                                                    Date date,
                                                    int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages after the given date
     * exchanged with the given SMS number and the metacontact (if any) that
     * contains that number.
     *
     * @param smsNumber the SMS number
     * @param date messages after date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<MessageEvent> findFirstMessagesAfter(String smsNumber,
                                                    Date date,
                                                    int count)
        throws RuntimeException;

    /**
     * Returns the message to/from the given remoteJid that has the given
     * xmppId.
     *
     * @param remoteJid The JID the message was to/from.
     * @param xmppId the XMPP ID
     *
     * @return the MessageEvent representing the matching message
     */
    MessageEvent findByXmppId(String remoteJid, String xmppId);

    /**
     * Returns the message to/from the given remoteJid that has the given
     * smppId.
     *
     * @param remoteJid The JID the message was to/from.
     * @param smppId the SMPP ID
     *
     * @return the MessageEvent representing the matching message
     */
    MessageEvent findBySmppId(String remoteJid, String smppId);

    /**
     * Sets the given SMPP ID for the message with the given XMPP ID.
     *
     * @param remoteJid the address that the message was sent to
     * @param xmppId    the XMPP ID of the message
     * @param smppId    the SMPP ID of the message
     *
     * @return true if the update succeeded, false otherwise.
     */
    boolean updateSmppId(String remoteJid, String xmppId, String smppId);

    /**
     * Returns the addresses of all participants of the chat room with the
     * given identifier.
     *
     * @param roomJid The identifier of the chat room.
     * @return the addresses of all participants of the chat room with the
     * given identifier.
     */
    Set<String> getRoomParticipants(String roomJid);

    /**
     * Sets the entry for the most recent message in the supplied descriptor's
     * history as read or unread.
     *
     * @param descriptor The descriptor whose message is to be set as read
     * @param isRead If true, mark the message as read, otherwise mark it as
     * unread.
     * @param isRefresh If true, the existing history entry for this message
     * should be refreshed in its place in the 'Recent' tab, rather than moved
     * to the top of the list.
     */
    void setLastMessageReadStatus(Object descriptor,
                                  boolean isRead,
                                  boolean isRefresh);

    /**
     * Updates the 'isClosed' entry for the most recent message in the
     * supplied chat room's history.
     *
     * @param chatRoom The chat room.
     * @param closed If true, mark the chatroom has closed, otherwise mark it
     * as not closed.
     */
    void setChatroomClosedStatus(ChatRoom chatRoom, boolean closed);

    /**
     * Adds a 'You were removed from the conversation' status message to the
     * given chat room.
     *
     * @param chatRoom the chat room that the user was removed from.
     */
    void setRemovedFromChatroom(ChatRoom chatRoom);

    /**
     * Update a history record, marking a message as read or unread
     *
     * @param peerId the peerId of the message
     * @param xmppId the XMPP ID of the message
     * @param isRead if true, then mark the message as read
     */
    void updateReadStatus(String peerId, String xmppId, boolean isRead);

    /**
     * Update all history records for a given peer, marking each unread
     * message as read
     *
     * @param peerId the peerId of the messages
     */
    void updateReadStatusToReadForAllUnread(String peerId);

    /**
     * Registers a <tt>SourceContactChangeListener</tt> with this service so
     * that it gets notifications whenever a SourceContact is added or updated.
     *
     * @param listener the <tt>SourceContactChangeListener</tt> to register.
     */
    void addSourceContactChangeListener(SourceContactChangeListener listener);

    /**
     * Unregisters a <tt>SourceContactChangeListener</tt> with this service so
     * that it stops receiving notifications whenever a SourceContact is added
     * or updated.
     *
     * @param listener the <tt>SourceContactChangeListener</tt> to unregister.
     */
    void removeSourceContactChangeListener(SourceContactChangeListener listener);
}
