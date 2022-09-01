// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.awt.*;
import java.beans.*;
import java.text.*;
import java.util.*;

import javax.sip.message.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

/**
 * SIP implementation for a CallParkOrbit.
 */
public class CallParkOrbitImpl implements CallParkOrbit
{
    /**
     * How long to disable the call park 'retrieve' button after an orbit
     * becomes busy.  During this time, clicking the button will exhibit the
     * same behavior as trying to park a call on an orbit that is busy (rather
     * than regular retrieve behavior).  This prevents a user picking up someone
     * else's freshly-parked call when instead they meant to park their own.
     */
    private static final long RETRIEVE_DELAY_MS = 400;

    private static final Logger logger =
        Logger.getLogger(CallParkOrbitImpl.class);

    private static final ConfigurationService configService =
        SipActivator.getConfigurationService();

    /**
     * The access code used when retrieving a parked call.  Usage: INVITE to
     * &lt;access code&gt; &lt;orbit code&gt;.
     */
    private static String callParkRetrieveCode;

    /*
     * Get the call park access code, and update it if it ever changes.
     */
    static
    {
        updateAccessCodes();
        // This is unlikely to change, but just in case
        configService.user().addPropertyChangeListener(
            OperationSetCallPark.RETRIEVE_CODE_KEY,
            new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    logger.debug("Property changed: " + evt);
                    updateAccessCodes();
                }
            });
    }

    private static void updateAccessCodes()
    {
        callParkRetrieveCode = configService.user().getString(
            OperationSetCallPark.RETRIEVE_CODE_KEY, "*14");
    }

    private String orbitCode;
    private String department;
    private String departmentShort;
    private String friendlyName;

    private OperationSetCallParkSipImpl opSetCallParkSipImpl;
    private OperationSetBasicTelephonySipImpl opSetBasicTelSipImpl;
    private OperationSetPresenceSipImpl opSetPresSipImpl;

    private CallParkOrbitState state;
    private ContactGroupSipImpl group;
    private ContactSipImpl sipContact;

    private static Timer busyTimer = new Timer("Call Park Orbit busy timer");
    private TimerTask busyTimerTask;
    private final Object busyTimerTaskLock = new Object();

    /**
     * Create a new call park orbit object.<p>
     * After creation, the orbit should be activated by calling the
     * {@link #createContact} method.  This does not happen in the constructor to
     * allow for the case where an orbit changes details (e.g. name) but not
     * orbit code.  In this case, we will create a new orbit object but with the
     * same underlying SIP details.  If the old orbit were destroyed after the
     * new one were created, it would result in us un-subscribing from presence
     * for the new contact.  By separating out the subscribe step we allow the
     * whole orbit set to be sorted out before we subscribe.
     *
     * @param opSetCallPkSipImpl The operation set for call park
     * @param opSetBasicTelSipImpl The operation set for SIP telephony
     * @param opSetPresSipImpl The operation set for SIP presence
     * @param orbit The orbit code
     * @param name The friendly name for this orbit
     * @param dept The full department name for this orbit
     * @param deptShort The short department name for this orbit
     * @param grp The group to which this orbit belongs
     */
    public CallParkOrbitImpl(OperationSetCallParkSipImpl opSetCallPkSipImpl,
                             OperationSetBasicTelephonySipImpl opSetBasicTelSipImpl,
                             OperationSetPresenceSipImpl opSetPresSipImpl,
                             String orbit,
                             String name,
                             String dept,
                             String deptShort,
                             ContactGroupSipImpl grp)
    {
        this.opSetCallParkSipImpl = opSetCallPkSipImpl;
        this.opSetBasicTelSipImpl = opSetBasicTelSipImpl;
        this.opSetPresSipImpl = opSetPresSipImpl;

        orbitCode = orbit;
        department = dept;
        departmentShort = deptShort;
        group = grp;
        state = CallParkOrbitState.UNKNOWN;
        busyTimerTask = null;
        friendlyName = name;

        logger.debug("New call park orbit created: (" + orbit + ", '" + name + "' ['" + friendlyName + "'], " + dept + ")");
    }

    @Override
    public String getOrbitCode()
    {
        return orbitCode;
    }

    @Override
    public String getFriendlyName()
    {
        return friendlyName;
    }

    @Override
    public void parkCall(CallPeer callPeer)
    {
        logger.info("Park call on orbit " + orbitCode);

        try
        {
            opSetBasicTelSipImpl.parkCall(
                    callPeer,
                    orbitCode,
                    new OperationSetBasicTelephonySipImpl.ResponseHandler(){
                        @Override
                        public boolean handleResponse(int responseCode)
                        {
                            return handleParkCallResponse(responseCode);
                        }
                });
        }
        catch (OperationFailedException e)
        {
            logger.error("Error trying to park call: ", e);
            showErrorPopup("impl.protocol.sip.CALL_PARK_FAILED_ERROR_TITLE",
                           "impl.protocol.sip.CALL_PARK_FAILED_ERROR_TEXT",
                           friendlyName);
        }
    }

    /**
     * Handle the response to a call park attempt.
     * @param responseCode The SIP response code
     * @return <tt>true</tt> if no further processing is required by the SIP
     * stack, <tt>false</tt> otherwise.
     */
    private boolean handleParkCallResponse(int responseCode)
    {
        logger.debug("Call park response: " + responseCode);
        boolean handled = false;

        switch (responseCode)
        {
        case Response.ACCEPTED:
            // Display a notification toast, and let the SIP code handle sending
            // any necessary further messages.
            logger.info("Call park successful (orbit " + orbitCode + ")");

            ResourceManagementService resources = SipActivator.getResources();
            String title = resources.getI18NString(
                "impl.protocol.sip.CALL_PARK_SUCCEEDED_TITLE");
            String message = resources.getI18NString(
                   "impl.protocol.sip.CALL_PARK_SUCCEEDED_TEXT", new String[] {friendlyName});

            SystrayService systray = SipActivator.getSystrayService();
            systray.showPopupMessage(new PopupMessage(title, message));

            break;

        case Response.BUSY_HERE:
            // Display an error, and don't require any further processing in the
            // SIP code (in particular, we don't want to show the call peer as
            // busy).
            logger.info("Call park on orbit " + orbitCode + " failed: orbit busy");
            showErrorPopup("impl.protocol.sip.CALL_PARK_FAILED_BUSY_TITLE",
                           "impl.protocol.sip.CALL_PARK_FAILED_BUSY_TEXT",
                           friendlyName);

            handled = true;
            break;

        case Response.UNAUTHORIZED:
            // Let the SIP code handle authentication
            logger.debug("Ignore authentication challenge - SIP will handle");
            break;

        default:
            // For errors, handle ourselves (display a warning, but don't fail
            // the call).  Otherwise let the SIP code do the work.
            if (responseCode >= 400)
            {
                logger.info("Call park on orbit " + orbitCode + " failed with code: " + responseCode);
                showErrorPopup("impl.protocol.sip.CALL_PARK_FAILED_ERROR_TITLE",
                               "impl.protocol.sip.CALL_PARK_FAILED_ERROR_TEXT",
                               friendlyName);

                handled = true;
            }
            break;
        }

        return handled;
    }

    /**
     * Display an error popup
     *
     * @param titleKey Resources identifier for the dialog title
     * @param messageKey Resources identifier for the dialog text
     * @param messageArgs Optional arguments to add  to the message
     */
    private void showErrorPopup(String titleKey,
                                String messageKey,
                                String... messageArgs)
    {
        ResourceManagementService res = SipActivator.getResources();
        ExportedWindow callParkWindow = SipActivator.getUIService()
                            .getExportedWindow(ExportedWindow.CALL_PARK_WINDOW);

        String title = res.getI18NString(titleKey);
        String msg = res.getI18NString(messageKey, messageArgs);

        Frame parent = (Frame)callParkWindow;
        ErrorDialog errorDialog = new ErrorDialog(parent, title, msg);
        errorDialog.setVisible(true);
    }

    @Override
    public void retrieveCall()
    {
        logger.info("Retrieve call from orbit " + orbitCode);

        String dialString = callParkRetrieveCode + orbitCode;
        try
        {
            opSetBasicTelSipImpl.createCall(dialString);
        }
        catch (OperationFailedException e)
        {
            logger.error("Failed to retrieve a parked call", e);
        }
        catch (ParseException e)
        {
            logger.error("Bad URI: " + dialString, e);
        }
    }

    @Override
    public CallParkOrbitState getState()
    {
        return state;
    }

    /**
     * Equivalent to .equals() except can be called before creating the other
     * orbit object (i.e. just compare using all the info that would be used to
     * create the orbit object).
     * @param orbit The orbit code
     * @param name The friendly name
     * @param dept The full department name
     * @param deptShort The short department name
     * @return <tt>true</tt> if the supplied details match those of this
     * CallParkOrbit object, otherwise <tt>false</tt>
     */
    public boolean matches(String orbit, String name, String dept, String deptShort, ContactGroupSipImpl grp)
    {
        // Orbit code must be configured
        if (!orbitCode.equals(orbit))
            return false;

        // Friendly name can legitimately be null
        if (!objectsEqualOrNull(friendlyName, name))
            return false;

        // Department name can legitimately be null
        if (!objectsEqualOrNull(department, dept))
            return false;

        // Department short name can legitimately be null
        if (!objectsEqualOrNull(departmentShort, deptShort))
            return false;

        // Group should not be null, but let's cope if it is.
        if (!objectsEqualOrNull(group, grp))
            return false;

        return true;
    }

    /**
     * Generic .equals method that copes with either object being null.
     * @param thisOne
     * @param thatOne
     * @return <tt>true</tt> if both objects are null, or if they are both
     * non-null and are equal; or <tt>false</tt> if one is null and the other
     * is non-null, or if both are non-null but they are not equal.
     */
    private static boolean objectsEqualOrNull(Object thisOne, Object thatOne)
    {
        if (thisOne == null)
            return (thatOne == null);
        else
            return thisOne.equals(thatOne);
    }

    /**
     * Create the underlying SIP contact and subscribe to LSM.
     */
    public void createContact()
    {
        logger.debug("Create SIP contact to monitor orbit " + orbitCode);
        sipContact = opSetPresSipImpl.createCallParkSubscriber(orbitCode, group);
    }

    @Override
    public void onPresenceStatusChanged(ContactPresenceStatusChangeEvent evt)
    {
        /*
         * States: 0 unknown, 1 free, 2 busy_disabled, 3 busy
         * Inputs: notif_free, notif_busy, notif_offline, timer_pop
         *
         *            | 0| 1| 2| 3
         * notif_busy |A2|A2| -| -
         * notif_free |B1| -|B1|B1
         * notif_offln|B0|B0|B0|B0
         * timer_pop  | !| !| 3| -
         *
         * Actions:
         * A - start the timer
         * B - cancel the timer
         * (plus all state changes result in a call to callParkStateChanged())
         */
        PresenceStatus newStatus = evt.getNewStatus();
        logger.debug(orbitCode + " receiving presence notification: " + newStatus);
        CallParkOrbitState newState = state;

        boolean nowFree = newStatus.isAvailableForCalls();
        boolean offline = !newStatus.isOnline();

        if (offline)
        {
            if (!OperationSetCallPark.CallParkOrbitState.UNKNOWN.equals(state))
            {
                // "offline" counts as non-busy, so we should cancel the busy
                // timer (a busy state means the orbit is in use and a call may
                // be retrieved - when the orbit is offline that's definitely
                // not the case).
                cancelBusyTimer();
                newState = OperationSetCallPark.CallParkOrbitState.UNKNOWN;
            }
        }
        else if (nowFree)
        {
            if (!OperationSetCallPark.CallParkOrbitState.FREE.equals(state))
            {
                // Moving from a busy to non-busy state
                cancelBusyTimer();
                newState = OperationSetCallPark.CallParkOrbitState.FREE;
            }
        }
        else
        {
            if (OperationSetCallPark.CallParkOrbitState.FREE.equals(state) ||
                OperationSetCallPark.CallParkOrbitState.UNKNOWN.equals(state))
            {
                // Moving to a busy state.  Start in 'disabled_busy'
                // to prevent too-quick-clicking resulting in
                // picking up a just-parked call
                startBusyTimer();
                newState = OperationSetCallPark.CallParkOrbitState.DISABLED_BUSY;
            }
        }

        updateOrbitState(newState);
    }

    /**
     * Update the orbit state and inform any listeners if it has changed.
     * @param newState
     */
    private void updateOrbitState(CallParkOrbitState newState)
    {
        if (newState != state)
        {
            logger.debug("Call Park state of orbit " + orbitCode + " changed to " + newState);
            CallParkOrbitState oldState = state;
            state = newState;
            opSetCallParkSipImpl.callParkStateChanged(this, oldState);
        }
    }

    /**
     * Start a timer when a park orbit becomes busy.  When the timer pops, move
     * the orbit to state BUSY (from DISABLED_BUSY).
     */
    private void startBusyTimer()
    {
        synchronized (busyTimerTaskLock)
        {
            if (busyTimerTask == null)
            {
                busyTimerTask = new TimerTask()
                {
                    @Override
                    public synchronized void run()
                    {
                        if (!OperationSetCallPark.CallParkOrbitState.DISABLED_BUSY.equals(state))
                        {
                            // The timer shouldn't be running when in these states
                            logger.error("Timer popped when in incorrect state: " + state);
                        }

                        CallParkOrbitState newState = OperationSetCallPark.CallParkOrbitState.BUSY;
                        updateOrbitState(newState);

                        // Now the task has finished running.
                        busyTimerTask = null;
                    }
                };

                busyTimer.schedule(busyTimerTask, RETRIEVE_DELAY_MS);
            }
            else
            {
                logger.debug("Ignore: timer already running");
            }
        }
    }

    /**
     * Cancel the busy timer for this orbit.
     */
    private void cancelBusyTimer()
    {
        synchronized (busyTimerTaskLock)
        {
            if (busyTimerTask != null)
            {
                logger.debug("Cancel busy timer");
                busyTimerTask.cancel();
                busyTimerTask = null;
            }
        }
    }

    /**
     * Unsubscribe from LSM and destroy the underlying SIP contact.
     */
    public void destroyContact()
    {
        logger.debug("Destroy SIP contact for monitoring orbit " + orbitCode);
        if (sipContact != null)
        {
            try
            {
                opSetPresSipImpl.unsubscribe(sipContact);
            }
            catch (IllegalArgumentException e)
            {
                logger.error("IllegalArgumentException when unsubscribing", e);
            }
            catch (IllegalStateException e)
            {
                logger.error("IllegalStateException when unsubscribing", e);
            }
            catch (OperationFailedException e)
            {
                logger.error("OperationFailedException when unsubscribing", e);
            }

            opSetPresSipImpl.fireSubscriptionEvent(
                     sipContact, group, SubscriptionEvent.SUBSCRIPTION_REMOVED);
            sipContact = null;
        }
    }
}
