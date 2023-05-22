/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat.conference;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import net.java.sip.communicator.impl.gui.main.chat.ChatTransport;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.account.AccountUtils;

/**
 * The conference implementation of the <tt>ChatTransport</tt> interface that
 * provides abstraction to access to protocol providers.
 *
 * @author Yana Stamcheva
 */
public class ConferenceChatTransport
    implements  ChatTransport
{
    private final ChatRoom mChatRoom;

    private String mChatRoomName;

    /**
     * Creates an instance of <tt>ConferenceChatTransport</tt> by specifying the
     * parent chat session and the chat room associated with this transport.
     *
     * @param chatRoom the chat room associated with this conference transport.
     */
    public ConferenceChatTransport(ChatRoom chatRoom)
    {
        mChatRoom = chatRoom;
        mChatRoomName = (chatRoom == null) ? null : chatRoom.getIdentifier().toString();
    }

    /**
     * Returns the contact address corresponding to this chat transport.
     *
     * @return The contact address corresponding to this chat transport.
     */
    public String getName()
    {
        if (mChatRoom != null)
        {
            mChatRoomName = mChatRoom.getIdentifier().toString();
        }

        return mChatRoomName;
    }

    /**
     * Returns the display name corresponding to this chat transport.
     *
     * @return The display name corresponding to this chat transport.
     */
    public String getDisplayName()
    {
        return getName();
    }

    /**
     * Returns the resource name of this chat transport. This is for example the
     * name of the user agent from which the contact is logged.
     *
     * @return The display name of this chat transport resource.
     */
    public String getResourceName()
    {
        return null;
    }

    /**
     * Returns the presence status of this transport.
     *
     * @return the presence status of this transport.
     */
    public PresenceStatus getStatus()
    {
         return null;
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     *
     * @return the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        // If there is no chat room associated with this transport, just
        // return the general IM provider (there should only ever be one so
        // this should make no difference).
        return (mChatRoom == null) ?
            AccountUtils.getImProvider() : mChatRoom.getParentProvider();
    }

    /**
     * Returns <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     */
    public boolean allowsInstantMessage()
    {
        // If the chat room is null, we can't send IMs!
        return (mChatRoom == null) ? false : mChatRoom.isJoined();
    }

    /**
     * Sending sms messages is not supported by this chat transport.
     * @return <code>false</code>.
     */
    public boolean allowsSmsMessage()
    {
        return false;
    }

    /**
     * Returns <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>.
     */
    public boolean allowsTypingNotifications()
    {
        return getProtocolProvider().getOperationSet(OperationSetTypingNotifications.class) != null;
    }

    /**
     * Sending typing notifications is not supported by this chat transport implementation.
     */
    public void sendTypingNotification(TypingState typingState)
    {
    }

    /**
     * Invites the given contact in this chat conference.
     *
     * @param contactAddress the address of the contact to invite
     * @param reason the reason for the invitation
     */
    public void inviteChatContact(String contactAddress, String reason)
    {
        if (mChatRoom != null)
            mChatRoom.invite(contactAddress, reason);
    }

    public void dispose()
    {}

    /**
     * Returns the descriptor of this chat transport.
     *
     * @return the descriptor of this chat transport
     */
    public Object getDescriptor()
    {
        return mChatRoom;
    }

    @Override
    public String toString()
    {
        return "<" + super.toString() + ", Chat Room = " + mChatRoom +
               ", Chat Room ID = " + sanitiseChatRoom(mChatRoomName) + ">";
    }
}
