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
import net.java.sip.communicator.util.account.*;

/**
 * Represents an event pertaining to message delivery, reception or failure
 * for a OneToOne message.
 */
public abstract class OneToOneMessageEvent extends MessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The remote contact that has sent/received this message (may be null if
     * the peerIdentifier is not null, as an SMS can be sent to/from a number
     * that is not saved as a contact).
     */
    private Contact peerContact = null;

    /**
     * The ID of the message being corrected, or null if this is a new message
     * and not a correction.
     */
    private String correctedMessageUID = null;

    /**
     * Constructs a new <tt>MessageEvent</tt> instance.
     *
     * @param source the <tt>Message</tt> represented by this event.
     * @param peerContact the <tt>Contact</tt> that this message was sent to/from.
     * @param peerIdentifier the identifer (SMS number or IM address) that this
     * message was sent to/from.
     * @param timestamp a date indicating the exact moment when the event
     * occurred
     * @param isRead Whether the messae has been read.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE static fields).
    */
    public OneToOneMessageEvent(ImMessage source, Contact peerContact, String peerIdentifier,
                                Date timestamp, boolean isRead, int eventType)
    {
        super(source,
              ((peerIdentifier == null) ? peerContact.getAddress() : peerIdentifier),
              timestamp,
              isRead,
              eventType);

        this.peerContact = peerContact;

        // SMS messages use the default IM provider
        this.protocolProvider = isSms() ?
          AccountUtils.getImProvider() : getPeerContact().getProtocolProvider();

        // All one-to-one messages should be displayed to the user
        this.isDisplayed = true;
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

    /**
     * Returns the correctedMessageUID The ID of the message being corrected,
     * or null if this is a new message and not a correction.
     *
     * @return the correctedMessageUID The ID of the message being corrected,
     * or null if this is a new message and not a correction.
     */
    public String getCorrectedMessageUID()
    {
        return correctedMessageUID;
    }

   /**
    * Sets the ID of the message being corrected to the passed ID.
    *
    * @param correctedMessageUID The ID of the message being corrected.
    */
   public void setCorrectedMessageUID(String correctedMessageUID)
   {
       this.correctedMessageUID = correctedMessageUID;
   }

   /**
    * Returns true if the MessageEvent is an SMS message
    *
    * @return true if the MessageEvent is an SMS message
    */
   public boolean isSms()
   {
       return getEventType() == MessageEvent.SMS_MESSAGE;
   }

   @Override
   public String toString()
   {
       return super.toString() + ", correctMessageUID = " + correctedMessageUID;
   }
}
