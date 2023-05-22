/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>ChatMessage</tt> class encapsulates message information in order to
 * provide a single object containing all data needed to display a chat message.
 *
 * @author Yana Stamcheva
 */
public class ChatMessage
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(ChatMessage.class);

    /**
     * The address of the contact sending the message.
     */
    private final String contactAddress;

    /**
     * The display name of the contact sending the message.
     */
    private String contactDisplayName;

    /**
     * The date and time of the message.
     */
    private Date date;

    /**
     * The type of the message.
     */
    private final String messageType;

    /**
     * The title of the message. This property is optional and could be used
     * to show a title for error messages.
     */
    private String messageTitle;

    /**
     * The content of the message.
     */
    private String message;

    /**
     * The content type of the message.
     */
    private final String contentType;

    /**
     * A unique identifier for this message.
     */
    private String messageUID;

    /**
     * The unique identifier of the message that this message should replace,
     * or <tt>null</tt> if this is a new message.
     */
    private String correctedMessageUID;

    /**
     * The unique identifier of the message that failed to send that this
     * message should replace, or <tt>null</tt> if this message isn't the
     * result of a message failure.
     */
    private String failedMessageUID;

    /**
     * The error message to display in the message that failed to send,
     * or <tt>null</tt> if this message isn't the result of a message failure.
     */
    private String errorMessage;

    /**
     * Boolean indicating that this message is an archive message
     */
    private final boolean isArchive;

    /**
     * Boolean indicating that this message is a carbon (XEP-0280) message
     */
    private final boolean isCarbon;

    /**
     * Creates a <tt>ChatMessage</tt> by specifying all parameters of the
     * message.
     * @param contactAddress the address of the contact
     * @param contactDisplayName the contact display name
     * @param date the date and time
     * @param messageType the type (INCOMING or OUTGOING)
     * @param messageTitle the title of the message
     * @param message the content
     * @param contentType the content type (e.g. "text", "text/html", etc.)
     * @param messageUID The ID of the message.
     * @param correctedMessageUID The ID of the message being replaced.
     * @param failedMessageUID The ID of the message that failed to send that
     *                         this message should replace (can be null).
     * @param errorMessage The error to display about the message failure
     *                     (can be null).
     * @param isArchive indicates that this message is an archive message
     * @param isCarbon indicates that this message is a carbon (XEP-0280) message
     */
    public ChatMessage(String contactAddress,
                       String contactDisplayName,
                       Date date,
                       String messageType,
                       String messageTitle,
                       String message,
                       String contentType,
                       String messageUID,
                       String correctedMessageUID,
                       String failedMessageUID,
                       String errorMessage,
                       boolean isArchive,
                       boolean isCarbon)
    {
        this.date = date;
        this.messageType = messageType;
        this.messageTitle = messageTitle;
        this.message = message;
        this.contentType = contentType;
        this.messageUID = messageUID;
        this.correctedMessageUID = correctedMessageUID;
        this.failedMessageUID = failedMessageUID;
        this.errorMessage = errorMessage;
        this.contactAddress = contactAddress;
        this.contactDisplayName = contactDisplayName;
        this.isArchive = isArchive;
        this.isCarbon = isCarbon;

        if (contactDisplayName == null)
        {
            logger.debug("Display name is null for " + logHasher(contactAddress));
        }
    }

    /**
     * Returns the address of the contact sending the message.
     *
     * @return the address of the contact sending the message.
     */
    public String getContactAddress()
    {
        return contactAddress;
    }

    /**
     * Returns the display name of the contact sending the message.
     *
     * @return the display name of the contact sending the message
     */
    public String getContactDisplayName()
    {
        return contactDisplayName;
    }

    /**
     * Returns the date and time of the message.
     *
     * @return the date and time of the message.
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * Sets the date and time of the message.
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * Returns the type of the message.
     *
     * @return the type of the message.
     */
    public String getMessageType()
    {
        return messageType;
    }

    /**
     * Returns the title of the message.
     *
     * @return the title of the message.
     */
    public String getMessageTitle()
    {
        return messageTitle;
    }

    /**
     * Returns the content of the message.
     *
     * @return the content of the message.
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Sets the content of the message.
     *
     * @param message the new content
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * Returns the content type (e.g. "text", "text/html", etc.).
     *
     * @return the content type
     */
    public String getContentType()
    {
        return contentType;
    }

    /**
     * Returns the UID of this message.
     *
     * @return the UID of this message.
     */
    public String getMessageUID()
    {
        return messageUID;
    }

    /**
     * Returns the UID of the message that this message replaces, or
     * <tt>null</tt> if this is a new message.
     *
     * @return the UID of the message that this message replaces, or
     * <tt>null</tt> if this is a new message.
     */
    public String getCorrectedMessageUID()
    {
        return correctedMessageUID;
    }

    /**
     * Returns the UID of the message that failed to send that this
     * message should replace, or <tt>null</tt> if this message isn't the
     * result of a message failure.
     *
     * @return the UID of the message that failed to send that this
     * message should replace, or <tt>null</tt> if this message isn't the
     * result of a message failure.
     */
    public String getFailedMessageUID()
    {
        return failedMessageUID;
    }

    /**
     * Returns the error message to display in the message that failed to send,
     * or <tt>null</tt> if this message isn't the result of a message failure.
     *
     * @return the error message to display in the message that failed to send,
     * or <tt>null</tt> if this message isn't the result of a message failure.
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }

    /**
     * @return true if this object represents a message received from the archive
     */
    public boolean isArchive()
    {
        return isArchive;
    }

    /**
     * @return true if this object represents a carbon copy message (XEP-0280)
     */
    public boolean isCarbon()
    {
        return isCarbon;
    }
}
