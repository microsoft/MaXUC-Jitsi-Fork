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
 * The operation set allows user bundles (e.g. the user interface) to send
 * and receive typing notifications to and from other <tt>Contact</tt>s.
 * <p>
 * An important thing of typing notifications is that they do not have the
 * purpose of being relibable.
 * @author Emil Ivov
 */
public interface OperationSetTypingNotifications
    extends OperationSet
{
    public enum TypingState
    {
        /**
         * Indicates that the typing state of a source contact is not currently
         * known. You should not normally received events carrying such a state.
         */
        UNKNOWN,

        /**
         * Indicates that a source contact is currently typing a message to us.
         */
        TYPING,

        /**
         * Indicates that a source contact isn't typing a message to us.
         */
        NOT_TYPING,

        /**
         * Indicates that a source contact had been typing a message to us but
         * has just paused.
         */
        PAUSED;
    };

    /**
     * Adds <tt>l</tt> to the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s
     * @param l the <tt>TypingNotificationsListener</tt> listener that we'd like
     * to add
     */
    void addTypingNotificationsListener(TypingNotificationsListener l);

    /**
     * Removes <tt>l</tt> from the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s
     * @param l the <tt>TypingNotificationsListener</tt> listener that we'd like
     * to remove
     */
    void removeTypingNotificationsListener(TypingNotificationsListener l);

    /**
     * Sends a notification to <tt>notifiedContatct</tt> that we have entered
     * <tt>typingState</tt>.
     * @param notifiedContact the <tt>Contact</tt> to notify
     * @param typingState the typing state that we have entered.
     *
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>notifiedContact</tt> is
     * not an instance belonging to the underlying implementation.
     */
    void sendTypingNotification(Contact notifiedContact, TypingState typingState)
        throws IllegalStateException, IllegalArgumentException;

    /**
     * Sends a notification to <tt>notifiedContatct</tt> that we have entered
     * <tt>typingState</tt>.
     * @param notifiedContact the <tt>Contact</tt> to notify
     * @param resource the <tt>ContatctResource</tt> (only used in
     * protocol-specific implementations of this method - by default the
     * method will send the notification to all registered resources for the
     * <tt>Contact</tt>).
     * @param typingState the typing state that we have entered.
     *
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>notifiedContact</tt> is
     * not an instance belonging to the underlying implementation.
     */
    void sendTypingNotification(Contact notifiedContact,
                                ContactResource resource,
                                TypingState typingState)
        throws IllegalStateException, IllegalArgumentException;
}
