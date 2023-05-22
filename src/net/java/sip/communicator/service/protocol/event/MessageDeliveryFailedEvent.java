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
 */
public class MessageDeliveryFailedEvent extends MessageFailedEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The remote contact that has sent/received this message (may be null if
     * the peerIdentifier is not null, as an SMS can be sent to/from a number
     * that is not saved as a contact).
     */
    private Contact peerContact = null;

     /**
      * Constructor.
      *
      * @param source the message
      * @param peerContact the "to" contact
      * @param errorCode error code
      */
     public MessageDeliveryFailedEvent(ImMessage source,
                                       Contact peerContact,
                                       int errorCode)
     {
         this(source, peerContact, null, errorCode, new Date(), null, MessageEvent.CHAT_MESSAGE);
     }

     /**
      * Constructor.
      *
      * @param source the message
      * @param peerContact the "to" contact
      * @param peerIdentifier the identifer (SMS number or IM address) that
      * this message was sent to.
      * @param correctedMessageUID The ID of the message being corrected.
      * @param failedMessageUID The ID of the message that failed to be delivered.
      * @param errorCode error code
      * @param eventType the type of message event that this instance represents
      * (one of the XXX_MESSAGE static fields).
      */
     public MessageDeliveryFailedEvent(ImMessage source,
                                       Contact peerContact,
                                       String peerIdentifier,
                                       String correctedMessageUID,
                                       String failedMessageUID,
                                       int errorCode,
                                       int eventType)
     {
         // If the peer identifier is null, use the contact's address as the
         // identifier
         super(source,
              ((peerIdentifier == null) ? peerContact.getAddress() : peerIdentifier),
               correctedMessageUID,
               failedMessageUID,
               errorCode,
               new Date(),
               null,
               eventType);

         this.peerContact = peerContact;
     }

     /**
      * Creates a <tt>MessageDeliveryFailedEvent</tt> indicating failure of
      * delivery of the <tt>source</tt> message to the specified <tt>to</tt>
      * contact.
      *
      * @param source the <tt>Message</tt> whose delivery this event represents.
      * @param peerContact the <tt>Contact</tt> that this message was sent to.
      * @param peerIdentifier the identifer (SMS number or IM address) that
      * this message was sent to.
      * @param errorCode an errorCode indicating the reason of the failure.
      * @param timestamp the exact timestamp when it was determined that delivery
      * had failed.
      * @param reason a human readable message indicating the reason for the
      * failure or null if the reason is unknown.
      * @param eventType the type of message event that this instance represents
      * (one of the XXX_MESSAGE static fields).
      */
     public MessageDeliveryFailedEvent(ImMessage source,
                                       Contact peerContact,
                                       String peerIdentifier,
                                       int errorCode,
                                       Date timestamp,
                                       String reason,
                                       int eventType)
     {
         // If the peer identifier is null, use the contact's address as the
         // identifier
         super(source,
               ((peerIdentifier == null) ? peerContact.getAddress() : peerIdentifier),
               null,
               null,
               errorCode,
               timestamp,
               reason,
               eventType);

         this.peerContact = peerContact;
     }

     /**
      * Returns a reference to the <tt>Contact</tt> that has sent/received the
      * <tt>Message</tt> represented by this event
      *
      * @return a reference to the <tt>Contact</tt> that has sent/received the
      * <tt>Message</tt> represented by this event.
      */
     public Contact getPeerContact()
     {
         return peerContact;
     }

     @Override
     public String getContactAddress()
     {
         if (getProtocolProvider() == null)
             return null;

         return getProtocolProvider().getAccountID().getAccountAddress();
     }

     @Override
     public String getContactDisplayName()
     {
        return displayDetailsService.getGlobalDisplayName();
     }

     @Override
     public String getMessageType()
     {
         return getEventType() == MessageEvent.SMS_MESSAGE ?
                              Chat.OUTGOING_SMS_MESSAGE : Chat.OUTGOING_MESSAGE;
     }
}
