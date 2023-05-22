/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents an event pertaining to chat room message delivery, reception or
 * failure.
 */
public abstract class AbstractChatRoomMessageEvent extends MessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Some services can fill our room with message history.
     */
    private boolean historyMessage = false;

    /**
     * If true, the conversation associated with this message is closed
     */
    private boolean isClosed = false;

    /**
     * The subject of the message, used as the chat room display name.
     */
    private String subject = "";

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     * @param source the received <tt>Message</tt>.
     * @param identifier the ID of the chat room.
     * @param subject the subject of this message (used as the chat room
     *                display name)
     * @param timestamp the exact date when the event occurred.
     * @param isRead Whether the message has been read.
     * @param isClosed Whether the conversation that this message belongs to
     *                 has closed
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE_RECEIVED static fields).
     */
    public AbstractChatRoomMessageEvent(ImMessage         source,
                                        String          identifier,
                                        String          subject,
                                        Date            timestamp,
                                        boolean         isRead,
                                        boolean         isClosed,
                                        int             eventType)
    {
        super(source, identifier, timestamp, isRead, eventType);

        this.isClosed = isClosed;
        this.subject = subject;
    }

    /**
     * Returns the subject of this message (used as the chat room display name)
     *
     * @return the subject of this message.
     */
    public String getSubject()
    {
        return subject;
    }

    /**
     * Is current event for history message.
     * @return is current event for history message.
     */
    public boolean isHistoryMessage()
    {
        return historyMessage;
    }

    /**
     * Changes property, whether this event is for a history message.
     *
     * @param historyMessage whether its event for history message.
     */
    public void setHistoryMessage(boolean historyMessage)
    {
        this.historyMessage = historyMessage;
    }

    /**
     * Returns true if the conversation that the <tt>Message</tt> represented
     * by this event has closed, or false otherwise.
     *
     * @return true if the conversation that the <tt>Message</tt> represented
     * by this event has closed, or false otherwise.
     */
    public boolean isConversationClosed()
    {
        return isClosed;
    }

    @Override
    public String toString()
    {
        return super.toString() + ", isClosed = " + isClosed;
    }
}
