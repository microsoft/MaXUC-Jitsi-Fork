/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.notification;

/**
 * The <tt>SoundNotificationHandler</tt> interface is meant to be
 * implemented by the notification bundle in order to provide handling of
 * sound actions.
 *
 * @author Yana Stamcheva
 */
public interface SoundNotificationHandler
    extends NotificationHandler
{
    /**
     * Start playing the sound pointed by <tt>getDescriotor</tt>. This
     * method should check the loopInterval value to distinguish whether to play
     * a simple sound or to play it in loop.
     * @param action the action to act upon
     * @param data Additional data for the event.
     */
    void start(SoundNotificationAction action, NotificationData data);

    /**
     * Stops playing the sound pointing by <tt>getDescriptor</tt>. This method
     * is meant to be used to stop sounds that are played in loop.
     * @param data Additional data for the event.
     */
    void stop(NotificationData data);

    /**
     * Stops/Restores all currently playing sounds.
     *
     * @param isMute mute or not currently playing sounds
     */
    void setMute(boolean isMute);

    /**
     * Specifies if currently the sound is off.
     *
     * @return TRUE if currently the sound is off, FALSE otherwise
     */
    boolean isMute();

    /**
     * Tells if the given notification sound is currently played.
     *
     * @param data Additional data for the event.
     */
    boolean isPlaying(NotificationData data);

    /**
     * Add a listener to be informed whenever the mute state changes
     *
     * @param listener The listener to add
     */
    void addMuteListener(MuteListener listener);

    /**
     * Interface implemented by those users that wish to be informed of changes
     * to the mute state
     */
    interface MuteListener
    {
        /**
         * Called when the mute state has changed
         *
         * @param newMute the new value of mute
         */
        void onMuteChanged(boolean newMute);
    }
}
