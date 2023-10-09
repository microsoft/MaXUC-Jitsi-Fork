/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;

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
     * Sends a typing notification state.
     *
     * @param typingState the typing notification state to send
     *
     * @return the result of this operation. One of the TYPING_NOTIFICATION_XXX
     * constants defined in this class
     */
    void sendTypingNotification(TypingState typingState);

    /**
     * Invites a contact to join this chat.
     *
     * @param contactAddress the address of the contact we invite
     * @param reason the reason for the invite
     */
    void inviteChatContact(String contactAddress, String reason);

    /**
     * Disposes this chat transport.
     */
    void dispose();
}
