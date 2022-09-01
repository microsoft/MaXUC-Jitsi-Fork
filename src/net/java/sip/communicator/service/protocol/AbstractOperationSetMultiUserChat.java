/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import org.jxmpp.jid.Jid;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a default implementation of <code>OperationSetMultiUserChat</code>
 * in order to make it easier for implementers to provide complete solutions
 * while focusing on implementation-specific details.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetMultiUserChat
    implements OperationSetMultiUserChat
{
    /**
     * The list of the currently registered
     * <code>ChatRoomInvitationListener</code>s.
     */
    private final List<ChatRoomInvitationListener> invitationListeners
        = new Vector<>();

    /**
     * The list of <code>ChatRoomInvitationRejectionListener</code>s subscribed
     * for events indicating rejection of a multi user chat invitation sent by
     * us.
     */
    private final List<ChatRoomInvitationRejectionListener> invitationRejectionListeners
        = new Vector<>();

    /**
     * Listeners that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     */
    private final List<LocalUserChatRoomPresenceListener> presenceListeners
        = new Vector<>();

    /**
     * Listeners that will be notified when a chat room is created
     */
    private final List<ChatRoomCreatedListener> chatRoomCreatedListeners
        = new Vector<>();

    /*
     * Implements
     * OperationSetMultiUserChat#addInvitationListener(
     * ChatRoomInvitationListener).
     */
    public void addInvitationListener(ChatRoomInvitationListener listener)
    {
        synchronized (invitationListeners)
        {
            if (!invitationListeners.contains(listener))
                invitationListeners.add(listener);
        }
    }

    /*
     * ImplementsOperationSetMultiUserChat#addInvitationRejectionListener(
     * ChatRoomInvitationRejectionListener).
     */
    public void addInvitationRejectionListener(
        ChatRoomInvitationRejectionListener listener)
    {
        synchronized (invitationRejectionListeners)
        {
            if (!invitationRejectionListeners.contains(listener))
                invitationRejectionListeners.add(listener);
        }
    }

    /*
     * Implements OperationSetMultiUserChat#addPresenceListener(
     * LocalUserChatRoomPresenceListener).
     */
    public void addPresenceListener(LocalUserChatRoomPresenceListener listener)
    {
        synchronized (presenceListeners)
        {
            if (!presenceListeners.contains(listener))
                presenceListeners.add(listener);
        }
    }

    /**
     * Whether multi user chat is supported
     */
    public boolean isMultiChatSupported()
    {
        return true;
    }

    /**
     * Fires a new <code>ChatRoomInvitationReceivedEvent</code> to all currently
     * registered <code>ChatRoomInvitationListener</code>s to notify about the
     * receipt of a specific <code>ChatRoomInvitation</code>.
     *
     * @param invitation
     *            the <code>ChatRoomInvitation</code> which has been received
     */
    protected void fireInvitationReceived(ChatRoomInvitation invitation)
    {
        ChatRoomInvitationReceivedEvent evt
            = new ChatRoomInvitationReceivedEvent(
                    this,
                    invitation,
                    new Date(System.currentTimeMillis()));

        ChatRoomInvitationListener[] listeners;
        synchronized (invitationListeners)
        {
            listeners
                = invitationListeners
                    .toArray(
                        new ChatRoomInvitationListener[
                                invitationListeners.size()]);
        }

        for (ChatRoomInvitationListener listener : listeners)
            listener.invitationReceived(evt);
    }

    /**
     * Delivers a <tt>ChatRoomInvitationRejectedEvent</tt> to all
     * registered <tt>ChatRoomInvitationRejectionListener</tt>s.
     *
     * @param sourceChatRoom the room that invitation refers to
     * @param invitee the name of the invitee that rejected the invitation
     * @param reason the reason of the rejection
     */
    protected void fireInvitationRejectedEvent(ChatRoom sourceChatRoom,
                                            String invitee,
                                            String reason)
    {
        ChatRoomInvitationRejectedEvent evt
            = new ChatRoomInvitationRejectedEvent(
                    this,
                    sourceChatRoom,
                    invitee,
                    reason,
                    new Date(System.currentTimeMillis()));

        ChatRoomInvitationRejectionListener[] listeners;
        synchronized (invitationRejectionListeners)
        {
            listeners
                = invitationRejectionListeners
                    .toArray(
                        new ChatRoomInvitationRejectionListener[
                                invitationRejectionListeners.size()]);
        }

        for (ChatRoomInvitationRejectionListener listener : listeners)
            listener.invitationRejected(evt);
    }

    /**
     * Delivers a <tt>LocalUserChatRoomPresenceChangeEvent</tt> to all
     * registered <tt>LocalUserChatRoomPresenceListener</tt>s.
     *
     * @param chatRoom
     *            the <tt>ChatRoom</tt> which has been joined, left, etc.
     * @param localUserId the ID of the local user whose presence has changed.
     * @param eventType
     *            the type of this event; one of LOCAL_USER_JOINED,
     *            LOCAL_USER_LEFT, etc.
     * @param reason
     *            the reason
     */
    public void fireLocalUserPresenceEvent(
        ChatRoom chatRoom,
        Jid localUserId,
        String eventType,
        String reason)
    {
        fireLocalUserPresenceEvent(chatRoom, localUserId, eventType, reason, new Date());
    }

    /**
     * Delivers a <tt>LocalUserChatRoomPresenceChangeEvent</tt> to all
     * registered <tt>LocalUserChatRoomPresenceListener</tt>s.
     *
     * @param chatRoom
     *            the <tt>ChatRoom</tt> which has been joined, left, etc.
     * @param localUserId the ID of the local user whose presence has changed.
     * @param eventType
     *            the type of this event; one of LOCAL_USER_JOINED,
     *            LOCAL_USER_LEFT, etc.
     * @param reason
     *            the reason
     * @param timestamp
     *            the timestamp when the event occurred.
     */
    public void fireLocalUserPresenceEvent(
        ChatRoom chatRoom,
        Jid localUserId,
        String eventType,
        String reason,
        Date timestamp)
    {
        LocalUserChatRoomPresenceChangeEvent evt
            = new LocalUserChatRoomPresenceChangeEvent( this,
                                                        chatRoom,
                                                        localUserId,
                                                        eventType,
                                                        reason,
                                                        timestamp);

        LocalUserChatRoomPresenceListener[] listeners;
        synchronized (presenceListeners)
        {
            listeners
                = presenceListeners
                    .toArray(
                        new LocalUserChatRoomPresenceListener[
                                presenceListeners.size()]);
        }

        for (LocalUserChatRoomPresenceListener listener : listeners)
            listener.localUserPresenceChanged(evt);
    }

    /*
     * Implements
     * OperationSetMultiUserChat#removeInvitationListener(
     * ChatRoomInvitationListener).
     */
    public void removeInvitationListener(ChatRoomInvitationListener listener)
    {
        synchronized (invitationListeners)
        {
            invitationListeners.remove(listener);
        }
    }

    /*
     * Implements OperationSetMultiUserChat#removeInvitationRejectionListener(
     * ChatRoomInvitationRejectionListener).
     */
    public void removeInvitationRejectionListener(
        ChatRoomInvitationRejectionListener listener)
    {
        synchronized (invitationRejectionListeners)
        {
            invitationRejectionListeners.remove(listener);
        }
    }

    /*
     * Implements OperationSetMultiUserChat#removePresenceListener(
     * LocalUserChatRoomPresenceListener).
     */
    public void removePresenceListener(
        LocalUserChatRoomPresenceListener listener)
    {
        synchronized (presenceListeners)
        {
            presenceListeners.remove(listener);
        }
    }

    @Override
    public void addChatRoomCreatedListener(ChatRoomCreatedListener listener)
    {
        synchronized (chatRoomCreatedListeners)
        {
            if (!chatRoomCreatedListeners.contains(listener))
                chatRoomCreatedListeners.add(listener);
        }
    }

    @Override
    public void removeChatRoomCreatedListener(ChatRoomCreatedListener listener)
    {
        synchronized (chatRoomCreatedListeners)
        {
            chatRoomCreatedListeners.remove(listener);
        }
    }

    /**
     * Fires a new <code>ChatRoomCreatedEvent</code> to all currently
     * registered <code>ChatRoomCreatedListener</code>s
     *
     * @param event the <code>ChatRoomCreatedEvent</code> which has been received
     */
    protected void fireChatRoomCreated(ChatRoomCreatedEvent event)
    {
        ArrayList<ChatRoomCreatedListener> listeners;
        synchronized (chatRoomCreatedListeners)
        {
            listeners = new ArrayList<>(chatRoomCreatedListeners);
        }

        for (ChatRoomCreatedListener listener : listeners)
            listener.chatRoomCreated(event);
    }

    @Override
    public int getActiveChatRooms()
    {
        return 0;
    }
}
