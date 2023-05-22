/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Provides basic functionality for sending and receiving InstantMessages.
 *
 * @author Emil Ivov
 */
public interface OperationSetBasicInstantMessaging
    extends OperationSet
{
    /**
     * Default encoding for outgoing messages.
     */
    String DEFAULT_MIME_ENCODING = "UTF-8";

    /**
     * Default mime type for outgoing messages.
     */
    String DEFAULT_MIME_TYPE = "text/plain";

    /**
     * HTML mime type for use with messages using html.
     */
    String HTML_MIME_TYPE = "text/html";

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *            subject.
     * @return the newly created message.
     */
    ImMessage createMessage(String content, String contentType,
                          String contentEncoding, String subject);

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    ImMessage createMessage(String messageText);

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * SMS number.
     * @param smsNumber the SMS number to which the message should be sent
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if no valid SMS domain has
     * been supplied.
     */
    void sendInstantMessage(String smsNumber, ImMessage message)
        throws IllegalStateException, IllegalArgumentException;

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    void sendInstantMessage(Contact to, ImMessage message)
        throws IllegalStateException, IllegalArgumentException;

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param resource the resource to which the message should be sent
     * @param message the <tt>Message</tt> to send.
     * @param fireEvent whether we should fire an event once the message
     * has been sent.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    void sendInstantMessage(Contact to,
                            ContactResource resource,
                            ImMessage message,
                            boolean fireEvent)
        throws IllegalStateException, IllegalArgumentException;

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact and the specific <tt>toResource</tt>.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param toResource the resource to which the message should be send
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    void sendInstantMessage(Contact to,
                            ContactResource toResource,
                            ImMessage message)
        throws IllegalStateException, IllegalArgumentException;

    /**
     * Registers a <tt>MessageListener</tt> with this operation set so that it
     * gets notifications of successful message delivery, failure or reception
     * of incoming messages.
     *
     * @param listener the <tt>MessageListener</tt> to register.
     */
    void addMessageListener(MessageListener listener);

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception of
     * incoming messages.
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
     */
    void removeMessageListener(MessageListener listener);

    /**
     * Determines whether the protocol provider (or the protocol itself) support
     * sending and receiving offline messages. Most often this method would
     * return true for protocols that support offline messages and false for
     * those that don't. It is however possible for a protocol to support these
     * messages and yet have a particular account that does not (i.e. feature
     * not enabled on the protocol server). In cases like this it is possible
     * for this method to return <tt>true</tt> even when offline messaging is
     * not supported, and then have the sendMessage method throw an
     * <tt>OperationFailedException</tt> with code
     * OFFLINE_MESSAGES_NOT_SUPPORTED.
     *
     * @return <tt>true</tt> if the protocol supports offline messages and
     * <tt>false</tt> otherwise.
     */
    boolean isOfflineMessagingSupported();

    /**
     * Determines whether the protocol supports the supplied content type
     * for the given contact.
     *
     * @param contentType the type we want to check
     * @param contact contact which is checked for supported contentType
     * @return <tt>true</tt> if the contact supports it and
     * <tt>false</tt> otherwise.
     */
    boolean isContentTypeSupported(String contentType, Contact contact);

    /**
     * Returns true if the given contact can receive instant messages, false
     * otherwise.
     *
     * @param contact the contact
     * @return true if the given contact can receive instant messages, false
     * otherwise
     */
    boolean isContactImCapable(Contact contact);
}
