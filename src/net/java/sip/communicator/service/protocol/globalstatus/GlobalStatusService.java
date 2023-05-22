/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.globalstatus;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Service managing global statuses, publishing status for
 * global statuses and for individual protocol providers, saving its last
 * state for future restore .
 *
 * @author Damian Minkov
 */
public interface GlobalStatusService extends ContactPresenceStatusListener
{
    /**
     * Returns the last status that was stored in the configuration for the
     * given protocol provider.
     *
     * @param protocolProvider the protocol provider
     * @return the last status that was stored in the configuration for the
     *         given protocol provider
     */
    PresenceStatus getLastPresenceStatus(
            ProtocolProviderService protocolProvider);

    /**
     * Returns the last contact status saved in the configuration.
     *
     * @param protocolProvider the protocol provider to which the status
     *            corresponds
     * @return the last contact status saved in the configuration.
     */
    String getLastStatusString(ProtocolProviderService protocolProvider);

    /**
     * Get the full status message including the global status and custom status
     * e.g. Busy - Back in 15mins
     *
     * @return the full status message to be shown
     */
    String getStatusMessage();

    /**
     * Get the GlobalStatusEnum for the current status
     *
     * @return the current GlobalStatusEnum
     */
    GlobalStatusEnum getGlobalStatus();

    /**
     * Set the custom status message to be appended to the current presence
     * status.
     *
     * @param customStatus the custom status message to be used
     */
    void setCustomStatus(String customStatus);

    /**
     * Get the custom status message e.g. Back in 15 mins
     *
     * @return the custom status message
     */
    String getCustomStatus();

    /**
     * If true, set 'do not disturb' to true then update the global status.
     *
     * @param enabled whether 'do not disturb' is enabled.
     */
    void setDoNotDisturbEnabled(boolean enabled);

    /**
     * @return true if do not disturb is enabled
     */
    boolean isDoNotDisturb();

    /**
     * @return true if the IM status is offline
     */
    boolean isOffline();

    /**
     * If true, set 'in a conference' to true then update the global status.
     *
     * @param isInConference whether the user is in a conference.
     */
    void setIsInConference(boolean isInConference);

    /**
     * @return true if the user is in a conference
     */
    boolean isInConference();

    /**
     * If true, set 'on the phone' to true then update the global status.
     *
     * @param onThePhone whether the user is on the phone.
     * @param isLocal whether the OnThePhone status relates to this client
     */
    void setIsOnThePhone(boolean onThePhone, boolean isLocal);

    /**
     * @return true if the user is on the phone anywhere
     */
    boolean isOnThePhone();

    /**
     * @return true if the user is on the phone locally
     */
    boolean isLocallyOnThePhone();

    /**
     * If true, set 'busy' to true, then update the global status.
     *
     * @param busy whether the user is busy or not
     */
    void setBusy(boolean busy);

    /**
     * @return true if the user is busy
     */
    boolean isBusy();

    /**
     * If true, set 'forwarding' to true then update the global status.
     *
     * @param isForwarding whether the user is forwarding calls.
     */
    void setIsForwarding(boolean isForwarding);

    /**
     * @return true if forwarding is enabled
     */
    boolean isForwarding();

    /**
     * If true, set 'in a meeting' to true then update the global status.
     *
     * @param inMeeting whether the user is in a meeting or not.
     */
    void setInMeeting(boolean inMeeting);

    /**
     * @return true if the user is in a meeting
     */
    boolean isInMeeting();

    /**
     * If true, set "away" to true then update the global status.
     *
     * @param away whether the user is away or not
     */
    void setAway(boolean away);

    /**
     * Notifies us that a contacts presence status has changed
     *
     * @param event the ContactPresenceStatusChangeEvent describing the status
     * change.
     */
    void contactPresenceStatusChanged(
            ContactPresenceStatusChangeEvent event);

    /**
     * Add a listener to be informed whenever the global status changes
     *
     * @param listener The listener to add
     */
    void addStatusChangeListener(GlobalStatusChangeListener listener);

    /**
     * Remove a registered listener from the  list of registered listeners
     *
     * @param listener The listener to remove
     */
    void removeStatusChangeListener(GlobalStatusChangeListener listener);

    /**
     * Interface implemented by those users that wish to be informed of changes
     * to the global status
     */
    interface GlobalStatusChangeListener
    {
        /**
         * Called when the global status has changed
         */
        void onStatusChanged();
    }
}
