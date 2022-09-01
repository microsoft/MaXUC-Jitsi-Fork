/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Provides functionality for correcting instant messages.
 *
 * @author Ivan Vergiliev
 */
public interface OperationSetMessageCorrection
    extends OperationSetBasicInstantMessaging
{
    /**
     * Replaces the message with ID <tt>correctedMessageUID</tt> sent to
     * the contact <tt>to</tt> at their <tt>ContactResource</tt>
     * <tt>resource</tt> with the message <tt>message</tt>
     *
     * @param to The contact to send the message to.
     * @param resource The contact resource to send the message to (if null,
     * the message will be sent to all of the contact's registered resources)
     * @param message The new message.
     * @param correctedMessageUID The ID of the message being replaced.
     */
    void correctMessage(Contact to,
                        ContactResource resource,
                        ImMessage message,
                        String correctedMessageUID);
}
