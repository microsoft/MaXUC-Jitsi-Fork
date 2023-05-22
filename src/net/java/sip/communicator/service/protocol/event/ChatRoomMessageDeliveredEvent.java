/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>MessageDeliveredEvent</tt>s confirm successful delivery of an instant
 * message.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class ChatRoomMessageDeliveredEvent extends ChatRoomMessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
     * <tt>source</tt> message to the specified <tt>to</tt> contact.
     * @param source the message that triggered this event.
     * @param chatRoom the <tt>ChatRoom</tt> which triggered this event.
     * @param identifier the ID of the <tt>ChatRoom</tt>.
     * @param subject the subject of this message (used as the chat room
     *                display name)
     * @param timestamp a date indicating the exact moment when the event
     * occurred
     * @param isClosed Whether the conversation that this message belongs to
     *                 has closed
     * @param eventType indicating the type of the delivered event. It's
     * either an ACTION_MESSAGE_DELIVERED or a CONVERSATION_MESSAGE_DELIVERED.
     */
     public ChatRoomMessageDeliveredEvent(ImMessage   source,
                                          ChatRoom  chatRoom,
                                          String    identifier,
                                          String    subject,
                                          Date      timestamp,
                                          boolean   isClosed,
                                          int       eventType)
     {
         super(source,
               chatRoom,
               identifier,
               subject,
               timestamp,
               true,
               isClosed,
               eventType);
     }

    @Override
    public String getContactAddress()
    {
        // We are the sender, so get our account address
        if (getProtocolProvider() == null)
            return null;

        return getProtocolProvider().getAccountID().getAccountAddress();
    }

    @Override
    public String getContactDisplayName()
    {
        // We are the sender, so get our display name
        return displayDetailsService.getGlobalDisplayName();
    }

    @Override
    public String getMessageType()
    {
        return (getEventType() == MessageEvent.STATUS_MESSAGE) ?
            Chat.STATUS_MESSAGE : Chat.OUTGOING_MESSAGE;
    }

    @Override
    public String toString()
    {
        return "ChatRoomMessageDeliveredEvent - " + super.toString();
    }
}
