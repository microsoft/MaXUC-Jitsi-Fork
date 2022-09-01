/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.util.*;

/**
 * The <tt>Chat</tt> interface is meant to be implemented by the GUI component
 * class representing a chat. Through the <i>isChatFocused</i> method the other
 * bundles could check the visibility of the chat component.
 *
 * @author Yana Stamcheva
 */
public interface Chat
{
    /**
     * The message type representing outgoing messages.
     */
    String OUTGOING_MESSAGE = "OutgoingMessage";
    /**
     * The message type representing incoming messages.
     */
    String INCOMING_MESSAGE = "IncomingMessage";
    /**
     * The message type representing status messages.
     */
    String STATUS_MESSAGE = "StatusMessage";
    /**
     * The message type representing action messages. These are message specific
     * for IRC, but could be used in other protocols also.
     */
    String ACTION_MESSAGE = "ActionMessage";
    /**
     * The message type representing system messages.
     */
    String SYSTEM_MESSAGE = "SystemMessage";
    /**
     * The message type representing incoming sms messages.
     */
    String INCOMING_SMS_MESSAGE = "SmsMessage";
    /**
     * The message type representing outgoing sms messages.
     */
    String OUTGOING_SMS_MESSAGE = "OutgoingSmsMessage";
    /**
     * The message type representing error messages.
     */
    String ERROR_MESSAGE = "ErrorMessage";

    /**
     * The maximum permitted number of characters in a chat message.
     */
    int MAX_CHAT_MESSAGE_LENGTH = 5000;

    /**
     * The maximum permitted number of characters in a chat room subject.
     */
    int MAX_CHAT_ROOM_SUBJECT_LENGTH = 60;

    /**
     * The maximum permitted number of characters in a group contact name.
     * This must be longer than the maximum chat room subject length, as if a
     * group contact is created from a group chat, its default subject will be
     * <chat_room_subject>-<date>.
     */
    int MAX_GROUP_CONTACT_NAME_LENGTH = 70;

    /**
     * Checks if this <tt>Chat</tt> is currently focused.
     *
     * @return TRUE if the chat is focused, FALSE - otherwise
     */
    boolean isChatFocused();

    /**
     * Checks if this <tt>Chat</tt> is currently in use.
     *
     * @return TRUE if the chat is in use, FALSE - otherwise
     */
    boolean isChatInUse();

    /**
     * Returns the message written by user in the chat write area.
     *
     * @return the message written by user in the chat write area
     */
    String getMessage();

    /**
     * Bring this chat to the front.
     */
    void setChatVisible();

    /**
     * Sets the given message as a message in the chat write area.
     *
     * @param message the text that would be set to the chat write area
     */
    void setMessage(String message);

    /**
     * Adds a message to this <tt>Chat</tt>.
     *
     * @param contactName the name of the contact sending the message
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message
     * @param message the message text
     * @param contentType the content type
     * @param isArchive indicates that this message is an archive message
     * @param isCarbon indicates that this message is a carbon message
     */
    void addMessage(String contactName,
                    Date date,
                    String messageType,
                    String message,
                    String contentType,
                    boolean isArchive,
                    boolean isCarbon);

    /**
     * Gets the caret position in the chat editor.
     * @return index of caret in message being composed
     */
    int getCaretPosition();

    /**
     * Causes the chat to validate its appearance (suggests a repaint operation
     * may be necessary).
     */
    void promptRepaint();
}
