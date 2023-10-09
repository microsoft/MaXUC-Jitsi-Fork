// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.beans.*;
import java.text.*;
import java.util.*;

import javax.sip.message.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbit;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbitState;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPANotion;
import net.java.sip.communicator.service.wispaservice.WISPANotionType;
import net.java.sip.communicator.service.wispaservice.WISPAService;
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
    /**
     * How long to disable the call park 'pick up' button after picking up a call.
     * During this time, the call is being set up and we want to avoid consecutive
     * clicks on the button from the user
     */
    private static final long PICKUP_DELAY_MS = 2000;

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

    private Timer timer = new Timer("Call Park Orbit timer");

    private TimerTask busyTimerTask;
    private final Object busyTimerTaskLock = new Object();

    private TimerTask freeTimerTask;
    private final Object freeTimerTaskLock = new Object();

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
        freeTimerTask = null;
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
            showSuccessPopup();
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

    private void showSuccessPopup()
    {
        ResourceManagementService resources = SipActivator.getResources();
        String title = resources
            .getI18NString("impl.protocol.sip.CALL_PARK_SUCCEEDED_TITLE");
        String message = resources.getI18NString(
            "impl.protocol.sip.CALL_PARK_SUCCEEDED_TEXT", new String[]
            { friendlyName });

        WISPAService wispaService = SipActivator.getWispaService();
        if (wispaService != null)
        {
            Object notification = new WISPANotion(WISPANotionType.NOTIFICATION,
                new NotificationInfo(title, message));
            wispaService.notify(WISPANamespace.EVENTS, WISPAAction.NOTION,
                notification);
        }
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

        String title = res.getI18NString(titleKey);
        String msg = res.getI18NString(messageKey, messageArgs);

        new ErrorDialog(title, msg).showDialog();
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
         * States: 0 unknown, 1 free, 2 busy_disabled, 3 busy, 4 free_disabled
         * Inputs: notif_free, notif_busy, notif_offline, user_picks_up_call, busy_timer_pop, free_timer_pop
         *
         *                      |  0|  1|  2|  3| 4
         * notif_busy           | A2| A2|  -|  -| -
         * notif_free           |BC1|  -|BC1|BC1|BC1
         * notif_offln          |BC0|BC0|BC0|BC0|BC0
         * busy_timer_pop       |  !|  !|  3|  -| -
         * free_timer_pop       |  -|  -|  3|  3|  3
         * user_picks_up_call   |  -|  -|  -|  4| -
         *
         * Actions:
         * A - start the busy timer
         * B - cancel the busy timer
         * C - cancel the free timer
         * (plus all state changes result in a call to callParkStateChanged())
         */
        PresenceStatus newStatus = evt.getNewStatus();
        logger.debug(orbitCode + " receiving presence notification: " + newStatus);
        CallParkOrbitState newState = state;

        boolean nowFree = newStatus.isAvailableForCalls();
        boolean offline = !newStatus.isOnline();

        if (offline)
        {
            if (!CallParkOrbitState.UNKNOWN.equals(state))
            {
                // "offline" counts as non-busy, so we should cancel the busy
                // timer (a busy state means the orbit is in use and a call may
                // be retrieved - when the orbit is offline that's definitely
                // not the case).
                cancelBusyTimer();
                // cancel the free timer as we don't want to update the
                // state to BUSY once the timer pops
                cancelFreeTimer();
                newState = CallParkOrbitState.UNKNOWN;
            }
        }
        else if (nowFree)
        {
            if (!CallParkOrbitState.FREE.equals(state))
            {
                // Moving from a busy to non-busy state
                cancelBusyTimer();
                // Orbit is free so the timer can be cancelled
                cancelFreeTimer();
                newState = CallParkOrbitState.FREE;
            }
        }
        else
        {
            if (CallParkOrbitState.FREE.equals(state) || CallParkOrbitState.UNKNOWN.equals(state))
            {
                // Moving to a busy state.  Start in 'disabled_busy'
                // to prevent too-quick-clicking resulting in
                // picking up a just-parked call
                startBusyTimer();
                newState = CallParkOrbitState.DISABLED_BUSY;
            }
        }

        updateOrbitState(newState);
    }

    @Override
    public void pickUpCall()
    {
        startFreeTimer();
        updateOrbitState(CallParkOrbitState.DISABLED_FREE);

        retrieveCall();
    }

    /**
     * Update the orbit state and inform any listeners if it has changed.
     * @param newState
     */
    private void updateOrbitState(CallParkOrbitState newState)
    {
        if (newState != state)
        {
            logger.debug("Call Park state of orbit " + orbitCode + " changed from " + state + " to " + newState);
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
                        if (!CallParkOrbitState.DISABLED_BUSY.equals(state))
                        {
                            // The timer shouldn't be running when in these states
                            logger.error("Timer popped when in incorrect state: " + state);
                        }

                        CallParkOrbitState newState = CallParkOrbitState.BUSY;
                        updateOrbitState(newState);

                        // Now the task has finished running.
                        busyTimerTask = null;
                    }
                };

                timer.schedule(busyTimerTask, RETRIEVE_DELAY_MS);
            }
            else
            {
                logger.debug("Ignore: timer already running");
            }
        }
    }

    /**
     * Start a timer when the 'pick up' button is clicked.
     * If the orbit is not free when the timer pops, move the orbit to state BUSY.
     */
    private void startFreeTimer()
    {
        synchronized (freeTimerTaskLock)
        {
            if (freeTimerTask == null)
            {
                freeTimerTask = new TimerTask()
                {
                    @Override
                    public synchronized void run()
                    {
                        if(CallParkOrbitState.FREE.equals(state) || CallParkOrbitState.UNKNOWN.equals(state)) {
                            logger.debug("Free timer popped when in expected state " + state);
                        }
                        else
                        {
                            // We expect to have been moved into state CallParkOrbitState.FREE,
                            // via a notif_free from the network, by the time the freeTimer pops.
                            // If that hasn't happened, we could be in one of two scenarios
                            // - in both cases we want to move back to state CallParkOrbitState.BUSY:
                            // 1. The call retrieval wasn't successful, so there will never be a notif_free
                            // for that attempt, and thus the orbit is still busy
                            // 2. There will be a notif_free but it hasn't arrived yet, in which case moving back
                            // to CallParkOrbitState.BUSY is still fine because when it does arrive we will move
                            // to CallParkOrbitState.FREE at that point.
                            logger.info("Free timer popped when in unexpected state " + state);
                            updateOrbitState(CallParkOrbitState.BUSY);
                        }

                        freeTimerTask = null;
                    }
                };

                timer.schedule(freeTimerTask, PICKUP_DELAY_MS);
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
     * Cancel the free timer for this orbit.
     */
    private void cancelFreeTimer()
    {
        synchronized (freeTimerTaskLock)
        {
            if (freeTimerTask != null)
            {
                logger.debug("Cancel free timer");
                freeTimerTask.cancel();
                freeTimerTask = null;
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
