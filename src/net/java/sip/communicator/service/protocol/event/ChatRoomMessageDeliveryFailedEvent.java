/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.account.*;

/**
 * <tt>ChatRoomMessageDeliveredEvent</tt>s confirm successful delivery of an
 * instant message.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class ChatRoomMessageDeliveryFailedEvent extends ChatRoomMessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
      * The chat room member that this message has been sent to.
      */
     private ChatRoomMember to = null;

     /**
      * The chat room that has sent this message.
      */
     private final ChatRoom chatRoom;

     private final String reason;

     private final int errorCode;

     /**
      * Creates a <tt>ChatRoomMessageDeliveryFailedEvent</tt> indicating failure
      * of delivery of a message to the specified <tt>ChatRoomMember</tt> in the
      * specified <tt>ChatRoom</tt>.
     * @param source the received <tt>Message</tt>.
     * @param chatRoom the <tt>ChatRoom</tt> in which the message was sent
     * @param to the <tt>ChatRoomMember</tt> that this message was sent to.
     * @param errorCode an errorCode indicating the reason of the failure.
     * @param reason the reason of the failure
     * @param timestamp the exacte Date when it was determined that delivery
      * had failed.
      */
     public ChatRoomMessageDeliveryFailedEvent(ImMessage source,
                                               ChatRoom chatRoom,
                                               ChatRoomMember to,
                                               int errorCode,
                                               String reason,
                                               Date timestamp)
     {
         super(source,
               chatRoom,
               null,
               null,
               timestamp,
               true,
               !chatRoom.isJoined(),
               MessageEvent.GROUP_MESSAGE);

         this.reason = reason;
         this.errorCode = errorCode;
         this.setFailed(true);
         this.to = to;
         this.chatRoom = chatRoom;
     }

     /**
      * Returns a reference to the <tt>ChatRoomMember</tt> that the source
      * (failed) <tt>Message</tt> was sent to.
      *
      * @return a reference to the <tt>ChatRoomMember</tt> that the source
      * failed <tt>Message</tt> was sent to.
      */
     public ChatRoomMember getChatRoomMember()
     {
         return to;
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
         return Chat.OUTGOING_MESSAGE;
     }

     @Override
     public ProtocolProviderService getProtocolProvider()
     {
         ChatRoom chatRoom = getChatRoom();

         return (chatRoom == null) ?
                     AccountUtils.getImProvider() : chatRoom.getParentProvider();
     }

     @Override
     public String toString()
     {
         String chatRoomId = (chatRoom == null) ? null : chatRoom.getIdentifier().toString();
         String toAddress = (to == null) ? null : to.getContactAddressAsString();
         return super.toString() +
             ", chatRoom = " + chatRoomId + ", to = " + toAddress;
     }

     public String getSubject()
     {
         return super.getSourceMessage().getSubject();
     }

    public int getErrorCode()
    {
        return this.errorCode;
    }

    public String getReason()
    {
        return this.reason;
    }
}
