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

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Represents an event pertaining to message delivery, reception or failure.
 */
public abstract class MessageEvent extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The GlobalDisplayDetailsService to find our display name
     */
    protected static final GlobalDisplayDetailsService displayDetailsService =
                     ProtocolProviderActivator.getGlobalDisplayDetailsService();

    /**
     * The MetaContactListService to find display names for other contacts
     */
    protected static final MetaContactListService contactListService =
                              ProtocolProviderActivator.getContactListService();

    /**
     * An event type indicating that the message is a standard chat message
     * sent by another contact.
     */
    public static final int CHAT_MESSAGE = 1;

    /**
     * An event type indicating that the message is a system message being sent
     * by the server or a system administrator.
     */
    public static final int SYSTEM_MESSAGE = 2;

    /**
     * An event type indicating that the message is an SMS message.
     */
    public static final int SMS_MESSAGE = 3;

    /**
     * An event type indicating that the message is a special message that sent
     * by either another member or the server itself, indicating that some kind
     * of action (other than the delivery of a conversation message) has
     * occurred. Action messages are widely used in IRC through the /action and
     * /me commands
     */
    public static final int ACTION_MESSAGE = 4;

    /**
     * An event type indicating that the message is a chat message sent in a
     * group chat (chatroom).
     */
    public static final int GROUP_MESSAGE = 5;

    /**
     * An event type indicating that the message is a message indicating a
     * user's status (e.g. joining or leaving a chat room).
     */
    public static final int STATUS_MESSAGE = 6;

    /**
     * The identifer (SMS number, IM address or chat room address) that this
     * message was sent to/from.
     */
    private String peerIdentifier = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private Date timestamp = null;

    /**
     * The type of message event that this instance represents.
     */
    private int eventType = -1;

    /**
     * If true, this message has been read.
     */
    private boolean isRead = true;

    /**
     * If true, this message had failed to be sent, valid only for outgoing messages
     */
    private boolean isFailed = false;

    /**
     * The protocolProvider responsible for the message
     */
    protected ProtocolProviderService protocolProvider = null;

    /**
     * If true, this message should be displayed to the user in the chat panel.
     * Some failed messages aren't displayed.
     */
    protected boolean isDisplayed = false;

    /**
     * Constructs a new <tt>MessageEvent</tt> instance.
     *
     * @param source the <tt>Message</tt> represented by this event.
     * @param peerIdentifier the identifer (SMS number, IM address or chat room
     * address) that this message was sent to/from.
     * @param timestamp a date indicating the exact moment when the event
     * occurred
     * @param isRead Whether the message has been read.
     * @param eventType the type of message event that this instance represents
     * (one of the XXX_MESSAGE static fields).
    */
    public MessageEvent(ImMessage source, String peerIdentifier, Date timestamp,
                        boolean isRead, int eventType)
    {
        super(source);

        this.peerIdentifier = ((peerIdentifier != null) ? peerIdentifier : "");
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.eventType = eventType;
    }

    /**
     * Returns the message that triggered this event
     *
     * @return the <tt>Message</tt> that triggered this event.
     */
    public ImMessage getSourceMessage()
    {
        return (ImMessage) getSource();
    }

    /**
     * Returns the UID of the message that triggered this event.
     *
     * @return the UID of the message that triggered this event
     */
    public String getUID()
    {
        return ((ImMessage) getSource()).getMessageUID();
    }

    /**
     * Returns the content of the message that triggered this event.
     *
     * @return the content of the message that triggered this event
     */
    public String getContent()
    {
        return ((ImMessage) getSource()).getContent();
    }

    /**
     * Returns the identifer (SMS number, IM address or chat room address) of
     * whoever sent/received the <tt>Message</tt> represented by this event.
     *
     * @return the identifer (SMS number, IM address or chat room address) of
     * whoever sent/received the <tt>Message</tt> represented by this event.
     */
    public String getPeerIdentifier()
    {
        return peerIdentifier;
    }

    /**
     * A timestamp indicating the exact date when the event occurred.
     *
     * @return a Date indicating when the event occurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns the type of message event represented by this event instance.
     * Message event type is one of the XXX_MESSAGE fields of this class.
     *
     * @return one of the XXX_MESSAGE fields of this class indicating the type
     * of this event.
     */
    public int getEventType()
    {
        return eventType;
    }

    /**
     * Returns true if the <tt>Message</tt> represented by this event has been
     * read, or false otherwise.
     *
     * @return true if the <tt>Message</tt> represented by this event has been
     * read, or false otherwise.
     */
    public boolean isMessageRead()
    {
        return isRead;
    }

    /**
     * Provides an error message to display to the user if there was an error
     *
     * @return error message to display to the user
     */
    public String getErrorMessage()
    {
        // By default there is no error message
        return null;
    }

    /**
     * @return true if the Message should be displayed to the user
     */
    public boolean isDisplayed()
    {
        return isDisplayed;
    }

    /**
     * The address of the contact who sent the Message
     *
     * @return the address of the contact who sent this Message
     */
    public abstract String getContactAddress();

    /**
     * The display name of the contact who sent the Message
     *
     * @return the display name of the contact who sent this Message
     */
    public abstract String getContactDisplayName();

    /**
     * The Chat type of the Message Event
     *
     * @return the Chat type of the Message Event
     */
    public abstract String getMessageType();

    /**
     * The protocol provider used to send/receive this message
     *
     * @return the protocol provider used to send/receive this message
     */
    public ProtocolProviderService getProtocolProvider() {
        return protocolProvider;
    }

    @Override
    public String toString()
    {
        String messageID =
            (source == null) ? null : ((ImMessage) source).getMessageUID();

        return super.toString() + ": messageID = " + messageID +
            ", peerID = " + logHasher(peerIdentifier) + ", isRead = " + isRead +
            ", eventType = " + eventType + ", timestamp = " + timestamp;
    }

    public void setFailed(boolean failed)
    {
        isFailed = failed;
    }

    /**
     * @return true if the Message failed to be sent.
     */
    public boolean isFailed()
    {
        return isFailed;
    }
}
