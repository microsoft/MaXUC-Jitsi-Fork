/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.globalshortcut;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.service.wispaservice.*;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.util.*;

/**
 * Shortcut for call (take the call, hang up, ...).
 *
 * @author Sebastien Vincent
 * @author Vincent Lucas
 */
public class CallShortcut
    implements GlobalShortcutListener,
               CallListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallShortcut</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(CallShortcut.class);

    /**
     * Lists the call actions available: ANSWER or HANGUP.
     */
    private enum CallAction
    {
        // Answers an incoming call.
        ANSWER,
        // Hangs up a call.
        HANGUP,
        // call window takes keyboard focus
        FOCUS
    }

    /**
     * Keybindings service.
     */
    private final KeybindingsService keybindingsService
        = GlobalShortcutActivator.getKeybindingsService();

    private static final AnalyticsService analyticsService
        = GlobalShortcutActivator.getAnalyticsService();

    private static final WISPAService wispaService
            = GlobalShortcutActivator.getWISPAService();

    /**
     * List of incoming calls not yet answered.
     */
    private final List<Call> incomingCalls = new ArrayList<>();

    /**
     * List of answered calls: active (off hold) or on hold.
     */
    private final List<Call> answeredCalls = new ArrayList<>();

    /**
     * Next mute state action.
     */
    private boolean mute = true;

    /**
     * Push to talk state action.
     */
    private boolean ptt_pressed = false;

    /**
     * Initializes a new <tt>CallShortcut</tt> instance.
     */
    public CallShortcut()
    {
    }

    /**
     * Notifies this <tt>GlobalShortcutListener</tt> that a shortcut was
     * triggered.
     *
     * @param evt a <tt>GlobalShortcutEvent</tt> which describes the specifics
     * of the triggering of the shortcut
     */
    public void shortcutReceived(GlobalShortcutEvent evt)
    {
        AWTKeyStroke keystroke = evt.getKeyStroke();
        GlobalKeybindingSet set = keybindingsService.getGlobalBindings();
        for(Map.Entry<String, List<AWTKeyStroke>> entry
                : set.getBindings().entrySet())
        {
            for(AWTKeyStroke ks : entry.getValue())
            {
                if(ks == null)
                    continue;

                String entryKey = entry.getKey();
                if((entryKey.equals("answer") || entryKey.equals("answer_mac")) &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    logger.info("Answer global shortcut pressed");
                    // Analytic is sent later as more details are needed

                    // Try to answer the new incoming call, if there is any.
                    manageNextIncomingCall(CallAction.ANSWER);
                }
                else if((entryKey.equals("hangup") || entryKey.equals("hangup_mac")) &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    logger.info("Hangup global shortcut pressed");
                    // Analytic is sent later as more details are needed

                    // Try to hang up the new incoming call.
                    if(!manageNextIncomingCall(CallAction.HANGUP))
                    {
                        logger.debug("No incoming call - closing active calls");

                        // There was no new incoming call.
                        // Thus, we try to close all active calls.
                        if(!closeAnsweredCalls(true))
                        {
                            logger.debug("No active call - closing inactive calls");

                            // There was no active call.
                            // Thus, we close all answered (inactive, hold on)
                            // calls.
                            closeAnsweredCalls(false);
                        }
                    }
                }
                else if((entryKey.equals("answer_hangup") || entryKey.equals("answer_hangup_mac")) &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    logger.info("Answer / Hangup global shortcut pressed");
                    // Analytic is sent later as more details are needed

                    // Try to answer the new incoming call.
                    if(!manageNextIncomingCall(CallAction.ANSWER))
                    {
                        logger.debug("No active incoming call - closing active calls");

                        // There was no new incoming call.
                        // Thus, we try to close all active calls.
                        if(!closeAnsweredCalls(true))
                        {
                            logger.debug("No active call - closing inactive calls");

                            // There was no active call.
                            // Thus, we close all answered (inactive, hold on)
                            // calls.
                            closeAnsweredCalls(false);
                        }
                    }
                }
                else if(entryKey.equals("focus_call_mac") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    logger.info("Focus call global shortcut pressed");
                    // Analytic is sent later as more details are needed

                    // Try to focus the new incoming call.
                    if(!manageNextIncomingCall(CallAction.FOCUS))
                    {
                        logger.debug("No active incoming call - focus an active call");

                        // There was no new incoming call. Focus an onging call
                        focusAnsweredCall();
                    }
                }
                else if((entryKey.equals("mute") || entryKey.equals("mute_mac")) &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers())
                {
                    logger.info("Mute global shortcut pressed");
                    analyticsService.onEvent(AnalyticsEventType.MUTE_CALL_SHORTCUT);

                    try
                    {
                        handleAllCallsMute(mute);
                    }
                    finally
                    {
                        // next action will revert change done here (mute or
                        // unmute)
                        mute = !mute;
                    }
                }
                else if(entryKey.equals("push_to_talk") &&
                    keystroke.getKeyCode() == ks.getKeyCode() &&
                    keystroke.getModifiers() == ks.getModifiers() &&
                    evt.isReleased() == ptt_pressed
                    )
                {
                    logger.info("Push to talk global shortcut pressed: "
                                + ptt_pressed);
                    analyticsService.onEvent(AnalyticsEventType.PTT_CALL_SHORTCUT);

                    try
                    {
                        handleAllCallsMute(ptt_pressed);
                    }
                    finally
                    {
                        ptt_pressed = !ptt_pressed;
                    }
                }
            }
        }
    }

    /**
     * Sets the mute state for all calls.
     *
     * @param mute the state to be set
     */
    private void handleAllCallsMute(boolean mute)
    {
            synchronized(incomingCalls)
            {
                for(Call c : incomingCalls)
                    handleMute(c, mute);
            }
            synchronized(answeredCalls)
            {
                for(Call c : answeredCalls)
                    handleMute(c, mute);
            }
    }

    /**
     * This method is called by a protocol provider whenever an incoming call is
     * received.
     *
     * @param event a CallEvent instance describing the new incoming call
     */
    public void incomingCallReceived(CallEvent event)
    {
        addCall(event.getSourceCall(), this.incomingCalls);
    }

    /**
     * This method is called by a protocol provider upon initiation of an
     * outgoing call.
     * <p>
     *
     * @param event a CalldEvent instance describing the new incoming call.
     */
    public void outgoingCallCreated(CallEvent event)
    {
        addCall(event.getSourceCall(), this.answeredCalls);
    }

    /**
     * Adds a created call to the managed call list.
     *
     * @param call The call to add to the managed call list.
     * @param calls The managed call list.
     */
    private static void addCall(Call call, List<Call> calls)
    {
        synchronized(calls)
        {
            if(!calls.contains(call))
                calls.add(call);
        }
    }

    /**
     * Indicates that all peers have left the source call and that it has
     * been ended. The event may be considered redundant since there are already
     * events issued upon termination of a single call peer but we've
     * decided to keep it for listeners that are only interested in call
     * duration and don't want to follow other call details.
     *
     * @param event the <tt>CallEvent</tt> containing the source call.
     */
    public void callEnded(CallEvent event)
    {
        Call sourceCall = event.getSourceCall();

        removeCall(sourceCall, incomingCalls);
        removeCall(sourceCall, answeredCalls);
    }

    /**
     * Removes an ended call to the managed call list.
     *
     * @param call The call to remove from the managed call list.
     * @param calls The managed call list.
     */
    private static void removeCall(Call call, List<Call> calls)
    {
        synchronized(calls)
        {
            if(calls.contains(call))
                calls.remove(call);
        }
    }

    /**
     * Sets the mute state of a specific <tt>Call</tt> in accord with
     * {@link #mute}.
     *
     * @param call the <tt>Call</tt> to set the mute state of
     * @param mute indicates if the current state is mute or unmute
     */
    private void handleMute(Call call, boolean mute)
    {
        // handle only established call
        if(call.getCallState() != CallState.CALL_IN_PROGRESS)
            return;
        // handle only connected peer (no on hold peer)
        if(call.getCallPeers().next().getState() != CallPeerState.CONNECTED)
            return;

        OperationSetBasicTelephony<?> basicTelephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetBasicTelephony.class);

        if ((basicTelephony != null)
                && (mute != ((MediaAwareCall<?,?,?>) call).isMute()))
        {
            basicTelephony.setMute(call, mute);
        }
    }

    /**
     * Answers or puts on/off hold the given call.
     *
     * @param call  The call to answer, to put on hold, or to put off hold.
     * @param callAction The action (ANSWER or HANGUP) to do.
     */
    private static void doCallAction(
            final Call call,
            final CallAction callAction)
    {
        new Thread("CallActionThread")
        {
            public void run()
            {
                try
                {
                    for (Call aCall : CallConference.getCalls(call))
                    {
                        Iterator<? extends CallPeer> callPeers
                            = aCall.getCallPeers();
                        OperationSetBasicTelephony<?> basicTelephony
                            = aCall.getProtocolProvider().getOperationSet(
                                    OperationSetBasicTelephony.class);

                        while(callPeers.hasNext())
                        {
                            CallPeer callPeer = callPeers.next();

                            switch(callAction)
                            {
                            case ANSWER:
                                logger.info("Call answer shortcut pressed");
                                if (callPeer
                                    .getState() == CallPeerState.INCOMING_CALL)
                                {
                                    basicTelephony.answerCallPeer(callPeer);
                                    analyticsService.onEvent(
                                        AnalyticsEventType.INCOMING_ANSWERED_SHORTCUT);
                                }
                                break;
                            case HANGUP:
                                logger.info("Call hangup shortcut pressed");
                                if (callPeer
                                    .getState() == CallPeerState.INCOMING_CALL)
                                {
                                    analyticsService.onEvent(
                                        AnalyticsEventType.INCOMING_REJECT_SHORTCUT);
                                }
                                else
                                {
                                    analyticsService.onEvent(
                                        AnalyticsEventType.HANGUP_CALL_SHORTCUT);
                                }
                                basicTelephony.hangupCallPeer(callPeer);
                                break;
                            case FOCUS:
                                logger.info("Call focus shortcut pressed");
                                if (callPeer
                                    .getState() == CallPeerState.INCOMING_CALL)
                                {
                                    wispaService.notify(
                                        WISPANamespace.ACTIVE_CALLS,
                                        WISPAAction.MOTION, aCall.getCallID());
                                    analyticsService.onEvent(
                                        AnalyticsEventType.FOCUS_CALL_SHORTCUT,
                                        AnalyticsParameter.CALL_TYPE,
                                        AnalyticsParameter.VALUE_INCOMING);
                                }
                                else
                                {
                                    UIService mUIService =
                                        GlobalShortcutActivator.getUIService();
                                    mUIService.focusCall(aCall);
                                    analyticsService.onEvent(
                                        AnalyticsEventType.FOCUS_CALL_SHORTCUT,
                                        AnalyticsParameter.CALL_TYPE,
                                        AnalyticsParameter.VALUE_ONGOING);
                                }
                                break;
                            }
                        }
                    }
                }
                catch(OperationFailedException ofe)
                {
                    logger.error(
                            "Failed to answer/hangup call via global shortcut",
                            ofe);
                }
            }
        }.start();
    }

    /**
     * Answers or hangs up the next incoming call if any.
     *
     * @param callAction The action (ANSWER or HANGUP) to do.
     *
     * @return True if the next incoming call has been answered/hanged up. False
     * if there is no incoming call remaining.
     */
    private boolean manageNextIncomingCall(CallAction callAction)
    {
        synchronized (incomingCalls)
        {
            int i = incomingCalls.size();

            while (i != 0)
            {
                --i;

                Call call = incomingCalls.get(i);

                if (call.getCallState() != CallState.CALL_INITIALIZATION)
                {
                    // Update our lists: This "incoming" call has been answered
                    answeredCalls.add(call);
                    incomingCalls.remove(i);
                }
                else // This call is actually incoming
                {
                    if (callAction != CallAction.FOCUS)
                    {
                        // We are answering it now, so update our lists
                        answeredCalls.add(call);
                        incomingCalls.remove(i);
                    }
                    // Answer, hang up or focus the ringing call.
                    CallShortcut.doCallAction(call, callAction);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean focusAnsweredCall()
    {
        synchronized (answeredCalls)
        {
            boolean callFocussed = false;
            int numAnsweredCalls = answeredCalls.size();
            int i = numAnsweredCalls;

            // Look for the active call first, and focus it if any
            while (i != 0 && !callFocussed)
            {
                --i;
                Call call = answeredCalls.get(i);
                if (CallShortcut.isActiveCall(call))
                {
                    CallShortcut.doCallAction(call, CallAction.FOCUS);
                    callFocussed = true;
                }
            }

            // No active call. Focus an inactive call if any
            // this code usually focuses the call that was the last to be received,
            // which is not ideal but not worth overengineering
            if (!callFocussed && numAnsweredCalls != 0)
            {
                logger.debug("No active call - focus an inactive call");
                Call call = answeredCalls.get(0);
                CallShortcut.doCallAction(call, CallAction.FOCUS);
                callFocussed = true;
            }

            logger.debug("Found call to focus? " + callFocussed);
            return callFocussed;
        }
    }

    /**
     * Closes only active calls, or all answered calls depending on the
     * closeOnlyActiveCalls parameter.
     *
     * @param closeOnlyActiveCalls Boolean which must be set to true to only
     * removes the active calls. Otherwise, the whole answered calls will be
     * closed.
     *
     * @return True if there was at least one call closed. False otherwise.
     */
    private boolean closeAnsweredCalls(boolean closeOnlyActiveCalls)
    {
        boolean isAtLeastOneCallClosed = false;

        synchronized(answeredCalls)
        {
            int i = answeredCalls.size();

            while(i != 0)
            {
                --i;

                Call call = answeredCalls.get(i);

                // If we are not limited to active call, then we close all
                // answered calls. Otherwise, we close only active calls (hold
                // off calls).
                if(!closeOnlyActiveCalls || CallShortcut.isActiveCall(call))
                {
                    CallShortcut.doCallAction(call, CallAction.HANGUP);
                    answeredCalls.remove(i);
                    isAtLeastOneCallClosed = true;
                }
            }
        }

        return isAtLeastOneCallClosed;
    }

    /**
     * Returns <tt>true</tt> if a specific <tt>Call</tt> is active - at least
     * one <tt>CallPeer</tt> is active (i.e. not on hold).
     *
     * @param call the <tt>Call</tt> to be determined whether it is active
     * @return <tt>true</tt> if the specified <tt>call</tt> is active;
     * <tt>false</tt>, otherwise.
     */
    private static boolean isActiveCall(Call call)
    {
        for (Call conferenceCall : CallConference.getCalls(call))
        {
            // If at least one CallPeer is active, the whole call is active.
            if (isAtLeastOneActiveCallPeer(conferenceCall.getCallPeers()))
                return true;
        }
        return false;
    }

    /**
     * Returns <tt>true</tt> if at least one <tt>CallPeer</tt> in a list of
     * <tt>CallPeer</tt>s is active i.e. is not on hold; <tt>false</tt>,
     * otherwise.
     *
     * @param callPeers the list of <tt>CallPeer</tt>s to check for at least one
     * active <tt>CallPeer</tt>
     * @return <tt>true</tt> if at least one <tt>CallPeer</tt> in
     * <tt>callPeers</tt> is active i.e. is not on hold; <tt>false</tt>,
     * otherwise
     */
    private static boolean isAtLeastOneActiveCallPeer(
            Iterator<? extends CallPeer> callPeers)
    {
        while (callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();

            if (!CallPeerState.isOnHold(callPeer.getState()))
            {
                // If at least one peer is active, then the call is active.
                return true;
            }
        }
        return false;
    }
}
