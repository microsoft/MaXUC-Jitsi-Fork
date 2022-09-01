/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.io.*;

import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The conference implementation of the <tt>ChatTransport</tt> interface that
 * provides abstraction to access to protocol providers.
 *
 * @author Yana Stamcheva
 */
public class ConferenceChatTransport
    implements  ChatTransport
{
    /**
     * The logger.
     */
    private static final Logger sLog =
        Logger.getLogger(ConferenceChatTransport.class);

    private final ChatSession mChatSession;

    private final ChatRoom mChatRoom;

    private String mChatRoomName;

    /**
     * Creates an instance of <tt>ConferenceChatTransport</tt> by specifying the
     * parent chat session and the chat room associated with this transport.
     *
     * @param chatSession the parent chat session.
     * @param chatRoom the chat room associated with this conference transport.
     */
    public ConferenceChatTransport(ChatSession chatSession, ChatRoom chatRoom)
    {
        mChatSession = chatSession;
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
     * Indicates if the display name should only show the resource.
     *
     * @return <tt>true</tt> if the display name shows only the resource,
     * <tt>false</tt> - otherwise
     */
    public boolean isDisplayResourceOnly()
    {
        return false;
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
     * Sends the given instant message trough this chat transport, by specifying
     * the mime type (html or plain text).
     *
     * @param messageText The message to send.
     * @param mimeType The mime type of the message to send: text/html or
     * text/plain.
     */
    public void sendInstantMessage(String messageText, String mimeType)
        throws Exception
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
        {
            sLog.debug("Not sending as this transport does not support IM");
            return;
        }

        ImMessage message = mChatRoom.createMessage(messageText, null);

        // Send the message but don't fire an event, otherwise we will display
        // our sent message in the UI, along with the one from us that we
        // receive from the group chat.
        mChatRoom.sendMessage(message, false);
    }

    /**
     * Determines whether this chat transport supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the chat transport supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        // we only support plain text for chat rooms for now
        return OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE
                .equals(contentType);
    }

    /**
     * Sending sms messages is not supported by this chat transport.
     */
    public void sendSmsMessage(String phoneNumber, String message)
    {}

    /**
     * Sending sms messages is not supported by this chat transport.
     */
    public void sendSmsMessage(Contact contact, String message)
    {}

    /**
     * Sending typing notifications is not supported by this chat transport implementation.
     */
    public void sendTypingNotification(TypingState typingState)
    {
    }

    /**
     * Sending files through a chat room is not yet supported by this chat
     * transport implementation.
     */
    public FileTransfer sendFile(File file, String transferId)
    {
        return null;
    }

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        return -1;
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

    @Override
    public void join() throws OperationFailedException
    {
        if (mChatRoom != null)
            mChatRoom.join();
    }

    /**
     * Returns the parent session of this chat transport. A <tt>ChatSession</tt>
     * could contain more than one transports.
     *
     * @return the parent session of this chat transport
     */
    public ChatSession getParentChatSession()
    {
        return mChatSession;
    }

    /**
     * Sending sms messages is not supported by this chat transport.
     * @param l The message listener to add.
     */
    public void addSmsMessageListener(MessageListener l)
    {
    }

    /**
     * Adds an instant message listener to this chat transport.
     *
     * @param l The message listener to add.
     */
    public void addInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet
            = getProtocolProvider()
                    .getOperationSet(OperationSetBasicInstantMessaging.class);

        imOpSet.addMessageListener(l);
    }

    /**
     * Sending sms messages is not supported by this chat transport.
     * @param l The message listener to remove.
     */
    public void removeSmsMessageListener(MessageListener l)
    {
    }

    /**
     * Removes the instant message listener from this chat transport.
     *
     * @param l The message listener to remove.
     */
    public void removeInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet
            = getProtocolProvider()
                    .getOperationSet(OperationSetBasicInstantMessaging.class);

        imOpSet.removeMessageListener(l);
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

    /**
     * Sends <tt>message</tt> as a message correction through this transport,
     * specifying the mime type (html or plain text) and the id of the
     * message to replace.
     *
     * @param message The message to send.
     * @param mimeType The mime type of the message to send: text/html or
     * text/plain.
     * @param correctedMessageUID The ID of the message being corrected by
     * this message.
     */
  public void correctInstantMessage(String message, String mimeType,
            String correctedMessageUID)
    {}

    /**
     * Returns <code>true</code> if this chat transport supports message
     * corrections and false otherwise.
     *
     * @return <code>true</code> if this chat transport supports message
     * corrections and false otherwise.
     */
    public boolean allowsMessageCorrections()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "<" + super.toString() + ", Chat Room = " + mChatRoom +
               ", Chat Room ID = " + mChatRoomName + ">";
    }
}
