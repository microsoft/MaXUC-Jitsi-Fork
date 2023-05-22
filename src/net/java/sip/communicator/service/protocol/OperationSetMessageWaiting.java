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
 * Provides notifications for message waiting notifications.
 *
 * @author Damian Minkov
 */
public interface OperationSetMessageWaiting
    extends OperationSet
{
    /**
     * Registers a <tt>MessageWaitingListener</tt> with this operation set so
     * that it gets notifications of new messages waiting.
     *
     * @param listener the <tt>MessageWaitingListener</tt> to register.
     */
    void addMessageWaitingNotificationListener(
            MessageWaitingListener listener);

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon new messages waiting.
     *
     * @param listener the <tt>MessageWaitingListener</tt> to unregister.
     */
    void removeMessageWaitingNotificationListener(
            MessageWaitingListener listener);

    /**
     * Returns the last fired message waiting event or null if no such event
     * has been fired.
     *
     * @return the last message waiting event.
     */
    MessageWaitingEvent getLastMessageWaitingNotification();

    /**
     * Informs the operation set of the number of unread messages found in the
     * inbox, from a particular source. Fires a message waiting notification
     * containing the total number of unread messages from all sources.
     *
     * @param unreadMessages The number of unread messages found.
     */
    void totalMessagesAndFireNotification(int unreadMessages);
}
