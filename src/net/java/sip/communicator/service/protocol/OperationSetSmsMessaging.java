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
 * Provides basic functionality for sending and receiving SMS Messages.
 *
 * @author Damian Minkov
 */
public interface OperationSetSmsMessaging
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
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @return the newly created message.
     */
    ImMessage createMessage(byte[] content, String contentType,
                          String contentEncoding);

    /**
     * Create a Message instance for sending a sms messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    ImMessage createMessage(String messageText);

    /**
     * Registers a MessageListener with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to register.
     */
    void addMessageListener(MessageListener listener);

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
     */
    void removeMessageListener(MessageListener listener);

}
