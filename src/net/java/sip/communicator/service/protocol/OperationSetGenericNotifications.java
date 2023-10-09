/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Provides notification for custom/generic events and possibility to generate
 * such events.
 *
 * @author Damian Minkov
 */
public interface OperationSetGenericNotifications
    extends OperationSet
{

    /**
     * Generates new generic event notification and send it to the
     * supplied contact.
     * @param jid the contact jid which will receive the event notification.
     * @param eventName the event name of the notification.
     * @param eventValue the event value of the notification.
     * @param source the source that will be reported in the event.
     */
    void notifyForEvent(
            String jid,
            String eventName,
            String eventValue,
            String source);
}
