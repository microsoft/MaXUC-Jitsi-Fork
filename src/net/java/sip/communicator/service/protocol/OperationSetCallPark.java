// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.awt.event.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * An operation set that provides all actions for parking and retrieving calls
 * in a call park orbit.
 * <p/>
 * Note that call park orbits are implemented as contacts. Thus to get Call Park
 * information you must query this operation set with a contact whose protocol
 * provider supports this operation set.
 */
public interface OperationSetCallPark extends OperationSet
{
    /**
     * Prefix for all Call Park config in the config file.
     */
    String CALLPARK_CONFIG_PREFIX = "net.java.sip.communicator.plugin.callpark";

    /**
     * Prefix for all Call Park orbit config in the config file.
     */
    String CALLPARK_ORBIT_CONFIG_PREFIX = CALLPARK_CONFIG_PREFIX + ".orbit";

    /**
     * The key for the Call Park 'retrieve' access code.
     */
    String RETRIEVE_CODE_KEY = CALLPARK_CONFIG_PREFIX + ".retrieveaccesscode";

    /**
     * The name of the root group (as passed to us by SIP PS).
     */
    String CALL_PARK_GROUP = "None";

    /**
     * The key for the Call Park active config property.  If true, the user has
     * a VoIP line and has chosen to activate call park, so they should be given
     * access to a UI that allows them to park calls into, and pick calls up from,
     * call park orbits.
     */
    String CALL_PARK_ACTIVE = CALLPARK_CONFIG_PREFIX + ".CALL_PARK_ACTIVE";

    /**
     * The key used to open the call park window from within a call (when the
     * <tt>CALL_PARK_SHORTCUT_KEY_MODIFIERS</tt> are also pressed).
     * <p>
     * If changed, also update <tt>service.gui.PARK_BUTTON_TOOL_TIP</tt>.
     */
    int CALL_PARK_SHORTCUT_KEY = KeyEvent.VK_P;

    /**
     * The modifiers needed to activate the <tt>CALL_PARK_SHORTCUT_KEY</tt>.
     * <p>
     * If changed, also update <tt>service.gui.PARK_BUTTON_TOOL_TIP</tt>.
     */
    int CALL_PARK_SHORTCUT_KEY_MODIFIERS =
                                       (KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK);

    /**
     * @return <tt>true</tt> if this user has call park enabled or <tt>false
     * </tt> otherwise.
     */
    boolean isCallParkAvailable();

    /**
     * Called when the user changes call park state.
     *
     * @param enabled
     */
    void setEnabled(boolean enabled);

    /**
     * @return <tt>true</tt> if this user has enabled Call Park, or <tt>false
     * </tt> otherwise.
     */
    boolean isEnabled();

    /**
     * Get the orbit information for a particular contact
     *
     * @param contact The contact whose information we are interested in
     * @return The orbit information for that contact.
     */
    CallParkOrbit getOrbitForContact(Contact contact);

    /**
     * Find a friendly name for this orbit, if it's an orbit we know about.
     *
     * @param orbitCode The orbit code
     * @return The friendly name for this orbit, if it is known, else <tt>
     * orbitCode</tt> unchanged.
     */
    String getFriendlyNameFromOrbitCode(String orbitCode);

    /**
     * Register a CallParkListener that will be informed of changes to the Call
     * Park orbits
     *
     * @param listener The listener to register
     */
    void registerListener(CallParkListener listener);

    /**
     * Remove a registered CallParkListener
     *
     * @param listener the listener to remove
     */
    void unregisterListener(CallParkListener listener);

    /**
     * Interface which exposes the information that we need to display and
     * control a CallParkOrbit
     */
    interface CallParkOrbit
    {
        /**
         * @return the orbit code of this orbit
         */
        String getOrbitCode();

        /**
         * @return the (optional) friendly name of this orbit to display in the UI.
         */
        String getFriendlyName();

        /**
         * Called in order to park a call on this orbit.
         *
         * @param callPeer The call peer to park on this orbit
         */
        void parkCall(CallPeer callPeer);

        /**
         * Called in order to retrieve the call from this orbit
         */
        void retrieveCall();

        /**
         * @return the current state of this orbit
         */
        CallParkOrbitState getState();

        /**
         * Retrieves the call and sets a timer to set the state to FREE
         * after a certain amount of time to avoid consecutive clicks on the
         * 'pick up' button
         */
        void pickUpCall();

        /**
         * Called when a presence notification arrives for this orbit.
         *
         * @param evt The presence notification event
         */
        void onPresenceStatusChanged(ContactPresenceStatusChangeEvent evt);
    }

    /**
     * Interface which is implemented by classes that wish to be informed of
     * changes to the list of CallPark orbits
     */
    interface CallParkListener
    {
        /**
         * Called when the list of CallPark orbits has changed
         */
        void onOrbitListChanged();

        /**
         * Called when the state of a particular orbit has changed
         *
         * @param orbit The orbit that has changed
         * @param oldState The previous state of the orbit
         */
        void onOrbitStateChanged(CallParkOrbit orbit,
                                 CallParkOrbitState oldState);

        /**
         * Called when the availability of call park for this user has changed
         */
        void onCallParkAvailabilityChanged();

        /**
         * Called when the enabled state of call park is changed
         */
        void onCallParkEnabledChanged();
    }

    /**
     * Enum containing the various states that an Orbit can be in
     */
    enum CallParkOrbitState
    {
        /**
         * The orbit is not in use, and is available to have a call parked on it
         */
        FREE,

        /**
         * There is a call parked on this orbit, which can be retrieved
         */
        BUSY,

        /**
         * There is a call parked on this orbit, but it can't be retrieved yet.
         * This prevents an issue where two people attempt to park a call on the
         * same orbit at the same time, where the second person would end up
         * retrieving the first person's parked call, rather than retrieving
         * their own.
         */
        DISABLED_BUSY,

        /**
         * A call has just been picked up, so to avoid consecutive clicks
         * on the button the DISABLED_FREE state is used before the
         * orbit is set to be in state FREE
         */
        DISABLED_FREE,

        /**
         * The state of the orbit is not yet known.  This state should be
         * transient at start-of-day and indicates that we have not yet received
         * a NOTIFY indicating the state of the orbit.
         */
        UNKNOWN;

        /**
         * @return true if this state represents a busy state
         */
        public boolean isBusy()
        {
            return this == BUSY || this == DISABLED_BUSY;
        }
    }
}
