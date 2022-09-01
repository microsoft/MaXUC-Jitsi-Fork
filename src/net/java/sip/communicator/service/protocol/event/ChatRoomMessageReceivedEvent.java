/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>MessageReceivedEvent</tt>s indicate reception of an instant message.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class ChatRoomMessageReceivedEvent extends ChatRoomMessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The chat room member that has sent this message.
     */
    private final ChatRoomMember from;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     * @param source the received <tt>Message</tt>.
     * @param chatRoom the <tt>ChatRoom</tt> for which the message is received.
     * @param identifier the ID of the <tt>ChatRoom</tt>.
     * @param subject the subject of this message (used as the chat room
     *                display name)
     * @param from the <tt>ChatRoomMember</tt> that has sent this message.
     * @param timestamp the exact date when the event occurred.
     * @param isRead Whether the message has been read.
     * @param isClosed Whether the conversation that this message belongs to
     *                 has closed
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE_RECEIVED static fields).
     */
    public ChatRoomMessageReceivedEvent(ImMessage         source,
                                        ChatRoom        chatRoom,
                                        String          identifier,
                                        String          subject,
                                        ChatRoomMember  from,
                                        Date            timestamp,
                                        boolean         isRead,
                                        boolean         isClosed,
                                        int             eventType)
    {
        super(source,
              chatRoom,
              identifier,
              subject,
              timestamp,
              isRead,
              isClosed,
              eventType);

        this.from = from;
    }

    /**
     * Returns a reference to the <tt>ChatRoomMember</tt> that has send the
     * <tt>Message</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>ChatRoomMember</tt> that has send the
     * <tt>Message</tt> whose reception this event represents.
     */
    public ChatRoomMember getChatRoomMember()
    {
        return from;
    }

    @Override
    public String getContactAddress()
    {
        return from.getContactAddressAsString();
    }

    @Override
    public String getContactDisplayName()
    {
        // Attempt to get the display name from the contact list service,
        // falling back to address if no display name found
        String displayName = getContactAddress();

        ProtocolProviderService imProvider = getProtocolProvider();
        if (imProvider != null)
        {
            MetaContact metaContact =
                contactListService.findMetaContactByContact(
                    displayName,
                    imProvider.getAccountID().getAccountUniqueID());

            if (metaContact != null)
            {
                displayName = metaContact.getDisplayName();
            }
        }

        return displayName;
    }

    @Override
    public String getMessageType()
    {
        return (getEventType() == MessageEvent.STATUS_MESSAGE) ?
                                    Chat.STATUS_MESSAGE : Chat.INCOMING_MESSAGE;
    }

    @Override
    public String toString()
    {
        return "ChatRoomMessageReceivedEvent - " + super.toString();
    }
}
