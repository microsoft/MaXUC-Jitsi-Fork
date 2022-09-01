/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.io.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>ChatTransport</tt> is an abstraction of the transport method used
 * when sending messages, making calls, etc. through the chat window.
 *
 * @author Yana Stamcheva
 */
public interface ChatTransport
{
    /**
     * Returns the descriptor object of this ChatTransport.
     *
     * @return the descriptor object of this ChatTransport
     */
    Object getDescriptor();

    /**
     * Returns <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>
     */
    boolean allowsInstantMessage();

    /**
     * Returns <tt>true</tt> if this chat transport supports message
     * corrections and false otherwise.
     *
     * @return <code>true</code> if this chat transport supports message
     * corrections and false otherwise.
     */
    boolean allowsMessageCorrections();

    /**
     * Returns <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>
     */
    boolean allowsSmsMessage();

    /**
     * Returns <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>
     */
    boolean allowsTypingNotifications();

    /**
     * Returns the name of this chat transport. This is for example the name of
     * the contact in a single chat mode and the name of the chat room in the
     * multi-chat mode.
     *
     * @return The name of this chat transport.
     */
    String getName();

    /**
     * Returns the display name of this chat transport. This is for example the
     * name of the contact in a single chat mode and the name of the chat room
     * in the multi-chat mode.
     *
     * @return The display name of this chat transport.
     */
    String getDisplayName();

    /**
     * Returns the resource name of this chat transport. This is for example the
     * name of the user agent from which the contact is logged.
     *
     * @return The display name of this chat transport resource.
     */
    String getResourceName();

    /**
     * Indicates if the display name should only show the resource.
     *
     * @return <tt>true</tt> if the display name shows only the resource,
     * <tt>false</tt> - otherwise
     */
    boolean isDisplayResourceOnly();

    /**
     * Returns the presence status of this transport.
     *
     * @return the presence status of this transport.
     */
    PresenceStatus getStatus();

    /**
     * Returns the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     *
     * @return the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     */
    ProtocolProviderService getProtocolProvider();

    /**
     * Sends the given instant message trough this chat transport, by specifying
     * the mime type (html or plain text).
     *
     * @param message The message to send.
     * @param mimeType The mime type of the message to send: text/html or
     * text/plain.
     * @throws Exception if the send doesn't succeed
     */
    void sendInstantMessage(String message, String mimeType)
        throws Exception;

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
    void correctInstantMessage(String message, String mimeType,
                               String correctedMessageUID);

    /**
     * Determines whether this chat transport supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the chat transport supports it and
     * <tt>false</tt> otherwise.
     */
    boolean isContentTypeSupported(String contentType);

    /**
     * Sends the given SMS message trough this chat transport.
     *
     * @param phoneNumber the phone number to which to send the message
     * @param message The message to send.
     */
    void sendSmsMessage(String phoneNumber, String message);

    /**
     * Sends the given SMS message trough this chat transport.
     *
     * @param contact the phone number to which to send the message
     * @param message The message to send.
     */
    void sendSmsMessage(Contact contact, String message);

    /**
     * Sends a typing notification state.
     *
     * @param typingState the typing notification state to send
     *
     * @return the result of this operation. One of the TYPING_NOTIFICATION_XXX
     * constants defined in this class
     */
    void sendTypingNotification(TypingState typingState);

    /**
     * Sends the given file trough this chat transport.
     *
     * @param file the file to send
     * @param transferId the unique identifier of this file transfer.
     * @return the <tt>FileTransfer</tt> charged to transfer the given
     * <tt>file</tt>.
     * @throws Exception if the send doesn't succeed
     */
    FileTransfer sendFile(File file, String transferId)
        throws Exception;

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * @return the file length that is supported.
     */
    long getMaximumFileLength();

    /**
     * Invites a contact to join this chat.
     *
     * @param contactAddress the address of the contact we invite
     * @param reason the reason for the invite
     */
    void inviteChatContact(String contactAddress, String reason);

    /**
     * Joins this chat
     *
     * @throws OperationFailedException if the operation fails
     */
    void join()
        throws OperationFailedException;

    /**
     * Returns the parent session of this chat transport. A <tt>ChatSession</tt>
     * could contain more than one transports.
     *
     * @return the parent session of this chat transport
     */
    ChatSession getParentChatSession();

    /**
     * Adds an sms message listener to this chat transport.
     *
     * @param l The message listener to add.
     */
    void addSmsMessageListener(MessageListener l);

    /**
     * Adds an instant message listener to this chat transport.
     *
     * @param l The message listener to add.
     */
    void addInstantMessageListener(MessageListener l);

    /**
     * Removes the given sms message listener from this chat transport.
     *
     * @param l The message listener to remove.
     */
    void removeSmsMessageListener(MessageListener l);

    /**
     * Removes the instant message listener from this chat transport.
     *
     * @param l The message listener to remove.
     */
    void removeInstantMessageListener(MessageListener l);

    /**
     * Disposes this chat transport.
     */
    void dispose();
}
