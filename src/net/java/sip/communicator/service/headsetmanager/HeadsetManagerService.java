// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.headsetmanager;

import java.util.List;

/**
 * Service managing the communication with headsets. With this service, other
 * bundles are able to change the state of the active headset or perform action
 * on the active headset. They can also subscribe to get notified of headset
 * button presses and headset state changes by implementing HeadsetManagerListener
 * and registering as a listener.
 */
public interface HeadsetManagerService
{
    // The HeadsetResponseState to use when the OS used does not support headset
    // integration.
    HeadsetResponseState UNSUPPORTED_OS_HEADSET_RESPONSE =
        HeadsetResponseState.NEVER;

    /**
     * Add a listener to be informed whenever there are headset changes
     *
     * @param listener The HeadsetManagerListener to add
     */
    void addHeadsetManagerListener(HeadsetManagerListener listener);

    /**
     * Informs the headset of a microphone mute state change
     *
     * @param mute is the new microphone mute state.
     * @param ID The unique ID of the call or meeting
     */
    void microphoneMuteChanged(boolean mute, String ID);

    /**
     * Informs the headset of an incoming call or meeting
     * @param ID The unique ID of the call or meeting
     */
    void incoming(String ID);

    /**
     * Informs the headset that there is an outgoing call
     * @param ID The unique ID of the outgoing call or meeting
     */
    void outgoing(String ID);

    /**
     * Informs the headset that the outgoing call has been answered by the
     * other party
     * @param ID The unique ID of the answered call or meeting
     */
    void outgoingAnswered(String ID);

    /**
     * Informs the headset that a call or meeting has been answered using the
     * AD UI
     * @param ID The unique ID of the call or meeting
     */
    void incomingAnswered(String ID);

    /**
     * Informs the headset that a call or meeting has been rejected or ended
     * using the AD UI, or by the other participant in the call.
     * @param ID The unique ID of the call or meeting
     */
    void ended(String ID);

    /**
    * Updates the mapping between a call and its active/hold status if the call
    * hold state has changed.
    * @param ID The unique ID of the call or meeting
    * @param active True if the call has become active. False if it has been
    * put on hold.
    */
    void setActive(String ID, boolean active);

   /**
    * Gets the ID of the active call or meeting
    * @return The ID of the call which is currently active, and not on hold.
    * Returns null if no such call exists
    */
   String getActiveCall();

   /**
    * Gets the directories where the headset API logs are located.
    *
    * @return a list of the headset API log directories.
    */
   List<String> getLogDirs();

   /**
    * Returns the active call's mute state. If there is no active call, returns
    * false.
    *
    * @return the active call's mute state.
    */
   boolean getActiveCallMuteState();

    /**
     * Interface implemented by those users that wish to be informed of changes
     * to the headset such as button presses and/or headset state changes.
     */
    interface HeadsetManagerListener
    {
        /**
         * Called when the headset's mute state has changed
         * @param mute is the headset's new mute state
         * @param ID The call which the mute state should be updated
         */
        void muteStateChanged(boolean mute, String ID);

        /**
         * Called when a headset button has been pressed to hang up the
         * currently active call or meeting.
         *
         * When the call or meeting has been hung up, headset manager service
         * needs to be notified using ended(ID) even if the hang up was
         * triggered by the headset.
         *
         * @param ID The call which should be ended
         */
        void hangUp(String ID);

        /**
         * Called when a headset button has been pressed to answer the most
         * recent incoming call or meeting
         * @param ID THe call which should be answered
         */
        void answer(String ID);

        /**
         * Called when a headset button has been pressed to reject the most
         * recent incoming call or meeting
         *
         * When the call or meeting has ended, headset manager service
         * needs to be notified using ended(ID) even if the call was rejected
         * by the headset.
         *
         * @param ID The call which should be rejected
         */
        void rejectIncoming(String ID);

        /**
         * Called when a headset button has been pressed to end an unanswered
         * outgoing call
         *
         * When the call or meeting has ended, headset manager service
         * needs to be notified using ended(ID) even if the call was cancelled
         * by the headset.
         *
         * @param ID The unanswered, outgoing call which should be ended
         */
        void cancelOutgoing(String ID);
    }

    /**
     * Updates the setting for when the headset responds to button presses
     * @param headsetSetting The value chosen by the user, either
     * 'NEVER', 'ALWAYS', or 'WHILE_UNLOCKED'
     */
    void headsetResponseStateChanged(HeadsetResponseState headsetSetting);

    /**
     * Updates the lock state of the computer
     * @param locked true if the user's computer is locked. False otherwise.
     */
    void lockStateChanged(boolean locked);

    /**
     *  An enum representing the different headset response states the user
     *  can choose in the audio configuration panel.
     */
    enum HeadsetResponseState
    {
        ALWAYS,
        NEVER,
        WHILE_UNLOCKED
    }

    // Class used to hold the state of a headset
    class HeadsetState
    {
        // The current in call state of the headset
        private boolean mInCallState = false;

        // The current ringing state of the headset
        private boolean mRingingState = false;

        // The current mute state of the headset
        private boolean mMuteState = false;

        /**
         * Returns the cached in call state of the headset
         * @return whether the headset is in call in the cached headset state
         */
        public boolean getInCallState()
        {
            return mInCallState;
        }

        /**
         * Sets the cached in call state of the headset
         * @param inCallState true if the headset is in call
         */
        public void setInCallState(boolean inCallState)
        {
            this.mInCallState = inCallState;
        }

        /**
         * Returns the cached mute state of the headset
         * @return whether the headset is muted in the cached headset state
         */
        public boolean getMuteState()
        {
            return mMuteState;
        }

        /**
         * Sets the cached mute state of the headset
         * @param muteState true if the headset is muted
         */
        public void setMuteState(boolean muteState)
        {
            this.mMuteState = muteState;
        }

        /**
         * Returns the cached ringing state of the headset
         * @return whether the headset is ringing in the cached headset state
         */
        public boolean getRingingState()
        {
            return mRingingState;
        }

        /**
         * Sets the cached mute state of the headset
         * @param ringingState true if the headset is ringing
         */
        public void setRingingState(boolean ringingState)
        {
            this.mRingingState = ringingState;
        }

        @Override
        public String toString()
        {
            return (mInCallState ? "" : "not ") + "in call" +
                   (mRingingState ? ", ringing" : "") +
                   (mMuteState ? ", muted" : ", unmuted");
        }
    }
}
