/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

import static net.java.sip.communicator.util.PrivacyUtils.sanitisePeerId;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.account.*;

/**
 * Represents an event pertaining to chat room message delivery, reception or
 * failure.
 */
public abstract class ChatRoomMessageEvent extends AbstractChatRoomMessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The chat room that has sent/received this message.
     */
    private final ChatRoom chatRoom;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the received <tt>Message</tt>.
     * @param chatRoom the <tt>ChatRoom</tt> for which the message is received.
     * @param identifier the ID of the <tt>ChatRoom</tt>.
     * @param subject the subject of this message (used as the chat room
     *                display name)
     * @param timestamp the exact date when the event occurred.
     * @param isRead Whether the message has been read.
     * @param isClosed Whether the conversation that this message belongs to
     *                 has closed
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE_RECEIVED static fields).
     */
    public ChatRoomMessageEvent(ImMessage         source,
                                ChatRoom        chatRoom,
                                String          identifier,
                                String          subject,
                                Date            timestamp,
                                boolean         isRead,
                                boolean         isClosed,
                                int             eventType)
    {
        super(source,
              ((identifier == null) ? chatRoom.getIdentifier().toString() : identifier),
              subject,
              timestamp,
              isRead,
              isClosed,
              eventType);

        this.chatRoom = chatRoom;

        // Default to the IM provider if there is no chat room as this will
        // most likely be the provider used
        this.protocolProvider = (chatRoom == null) ?
                    AccountUtils.getImProvider() : chatRoom.getParentProvider();

        // All chat room messages should be displayed
        this.isDisplayed = true;
    }

    /**
     * Returns the <tt>ChatRoom</tt> that triggered this event.
     * @return the <tt>ChatRoom</tt> that triggered this event.
     */
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("messageID  = " + this.getUID());
        builder.append(", peerID  = " + sanitisePeerId(this.getContactAddress()));
        builder.append(", isRead = " + this.isMessageRead());
        builder.append(", timestamp = " + this.getTimestamp());
        builder.append(", isClosed = " + this.isConversationClosed());
        builder.append(", chatRoom = " + this.getChatRoom());

        return builder.toString();
    }
}
