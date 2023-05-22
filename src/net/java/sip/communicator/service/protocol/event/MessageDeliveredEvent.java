/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>MessageDeliveredEvent</tt>s confirm successful delivery of an IM or SMS.
 *
 * @author Emil Ivov
 */
public class MessageDeliveredEvent extends OneToOneMessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Text to display if there is an error in sending the message
     */
    private static final String MSG_DELIVERY_ERROR = ProtocolProviderActivator.
           getResourceService().getI18NString("service.gui.MSG_DELIVERY_ERROR");

     /**
      * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>to</tt> contact.
      *
      * @param source message source
      * @param peerContact the <tt>Contact</tt> that this message was sent to.
      * @param peerIdentifier the identifer (SMS number or IM address) that
      * this message was sent to.
      * @param eventType the type of message event that this instance represents
      * (one of the XXX_MESSAGE static fields).
      */
     public MessageDeliveredEvent(ImMessage source, Contact peerContact,
                                  String peerIdentifier, int eventType)
     {
         // If the peer identifier is null, use the contact's address as the
         // identifier
         this(source, peerContact, peerIdentifier, new Date(), null, eventType, false);
     }

     /**
      * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>to</tt> contact.
      *
      * @param source the <tt>Message</tt> whose delivery this event represents.
      * @param peerContact the <tt>Contact</tt> that this message was sent to.
      * @param timestamp a date indicating the exact moment when the event
      * occurred
      */
     public MessageDeliveredEvent(ImMessage source, Contact peerContact, Date timestamp)
     {
         this(source, peerContact, null, timestamp, null, CHAT_MESSAGE, false);
     }

     /**
      * Creates a <tt>MessageDeliveredEvent</tt> representing delivery of the
      * <tt>source</tt> message to the specified <tt>to</tt> contact.
      *
      * @param source the <tt>Message</tt> whose delivery this event represents.
      * @param peerContact the <tt>Contact</tt> that this message was sent to.
      * @param peerIdentifier the identifer (SMS number or IM address) that
      * this message was sent to.
      * @param timestamp a date indicating the exact moment when the event
      * occurred
      * @param correctedMessageUID The ID of the message being corrected, or
      * null if this is a new message and not a correction.
      * @param eventType the type of message event that this instance represents
      * (one of the XXX_MESSAGE static fields).
      * @param isFailed Whether the message failed to be delivered. Defaults to
      *                 'false' unless the server sends us a failure message.
      */
     public MessageDeliveredEvent(ImMessage source, Contact peerContact,
                                  String peerIdentifier, Date timestamp,
                                  String correctedMessageUID, int eventType,
                                  boolean isFailed)
     {
         // If the peer identifier is null, use the contact's address as the
         // identifier
         super(source,
               peerContact,
               peerIdentifier,
               timestamp,
               true,
               eventType);

         setCorrectedMessageUID(correctedMessageUID);
         setFailed(isFailed);
     }

     @Override
     public String getErrorMessage()
     {
         return isFailed() ? MSG_DELIVERY_ERROR : null;
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
        return isSms() ? Chat.OUTGOING_SMS_MESSAGE : Chat.OUTGOING_MESSAGE;
    }

    @Override
    public String toString(){
        StringBuffer buff = new StringBuffer("MessageDeliveredEvent - ");
        buff.append("messageID  = " + this.getUID());
        buff.append(", peerID  = " + logHasher(this.getContactAddress()));
        buff.append(", isRead = " + this.isMessageRead());
        buff.append(", timestamp = " + this.getTimestamp());

        return buff.toString();
    }
}
