/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>MessageReceivedEvent</tt>s indicate reception of an IM or SMS.
 *
 * @author Emil Ivov
 */
public class MessageReceivedEvent extends OneToOneMessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>ContactResource</tt>, from which the message was sent.
     */
    private ContactResource fromResource = null;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param timestamp the exact date when the event ocurred.
     */
    public MessageReceivedEvent(ImMessage source,
                                Contact from,
                                Date timestamp)
    {
       this(source, from, timestamp, CHAT_MESSAGE);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param from the <tt>Contact</tt> that has sent this message.
     * @param timestamp the exact date when the event occurred.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE static fields).
     */
    public MessageReceivedEvent(ImMessage source,
                                Contact from,
                                Date timestamp,
                                int eventType)
    {
        this(source, null, from, timestamp, true, eventType);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param peerIdentifier the identifer (SMS number or IM address) that this
     * message was sent from.
     * @param peerContact the <tt>Contact</tt> that has sent this message.
     * @param timestamp the exact date when the event occurred.
     * @param isRead True if this message has been read, false otherwise.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE static fields).
     */
    public MessageReceivedEvent(ImMessage source,
                                String peerIdentifier,
                                Contact peerContact,
                                Date timestamp,
                                boolean isRead,
                                int eventType)
    {
        super(source,
              peerContact,
              peerIdentifier,
              timestamp,
              isRead,
              eventType);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     * @param peerContact the <tt>Contact</tt> that has sent this message.
     * @param fromResource the <tt>ContactResource</tt>, from which this message
     * was sent
     * @param timestamp the exact date when the event occurred.
     * @param correctedMessageUID The ID of the message being corrected, or null
     * if this is a new message and not a correction.
     * @param isRead True if this message has been read, false otherwise.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE static fields).
     */
    public MessageReceivedEvent(ImMessage source,
                                Contact peerContact,
                                ContactResource fromResource,
                                Date timestamp,
                                String correctedMessageUID,
                                boolean isRead,
                                int eventType)
    {
        super(source,
              peerContact,
              peerContact.getAddress(),
              timestamp,
              isRead,
              eventType);

        setCorrectedMessageUID(correctedMessageUID);
        this.fromResource = fromResource;
    }

    /**
     * Returns a reference to the <tt>ContactResource</tt> that has sent the
     * <tt>Message</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>ContactResource</tt> that has sent the
     * <tt>Message</tt> whose reception this event represents.
     */
    public ContactResource getContactResource()
    {
        return fromResource;
    }

    @Override
    public String getContactAddress()
    {
        String contactAddress;

        if (isSms())
        {
            // If SMS, we use the SMS number
            contactAddress = getPeerIdentifier();
        }
        else
        {
            // If not SMS, we get the address from the source contact
            Contact sourceContact = getPeerContact();
            contactAddress = sourceContact.getAddress();
        }

        return contactAddress;
    }

    @Override
    public String getContactDisplayName()
    {
        MetaContact sourceMetaContact;
        String defaultDisplayName;

        if (isSms())
        {
            // If SMS, we use the SMS number
            defaultDisplayName = getPeerIdentifier();
            sourceMetaContact = contactListService.
                                findMetaContactForSmsNumber(defaultDisplayName);
        }
        else
        {
            Contact sourceContact = getPeerContact();

            // If not SMS, we get information from the source contact
            defaultDisplayName = sourceContact.getDisplayName();
            sourceMetaContact =
                     contactListService.findMetaContactByContact(sourceContact);
        }

        // If the sourceContact is contained in a MetaContact, use the
        // MetaContact's display name, as that will be the name used
        // for the contact in the rest of the UI (and the display name
        // of a chat contact is often just the chat address).
        String contactDisplayName =
                sourceMetaContact ==  null ?
                    defaultDisplayName :
                        sourceMetaContact.getDisplayName();

        return contactDisplayName;
    }

    @Override
    public String getMessageType()
    {
        return isSms() ? Chat.INCOMING_SMS_MESSAGE: Chat.INCOMING_MESSAGE;
    }

    @Override
    public String toString()
    {
        return super.toString() + ", fromResource = " + fromResource;
    }
}
