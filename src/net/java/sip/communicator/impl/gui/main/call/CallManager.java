/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.util.stream.Collectors;

import javax.swing.*;

import com.google.common.annotations.VisibleForTesting;
import com.metaswitch.maxanalytics.event.CallKt;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import static org.jitsi.util.Hasher.logHasher;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.insights.InsightEvent;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.emergencylocation.EmergencyCallContext;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.service.headsetmanager.HeadsetManagerService.*;
import net.java.sip.communicator.service.headsetmanager.HeadsetManagerService;

/**
 * The <tt>CallManager</tt> is the one that handles calls. It contains also
 * the "Call" and "Hang up" buttons panel. Here are handles incoming and
 * outgoing calls from and to the call operation set.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class CallManager
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallManager</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(CallManager.class);

    /**
     * The global presence status service.
     */
    private static final GlobalStatusService globalStatusService =
        GuiActivator.getGlobalStatusService();

    /**
     * The resource management service.
     */
    private static final ResourceManagementService resources =
        GuiActivator.getResources();

    /**
     * The name of the property which indicates whether the video is allowed
     * for multi-party calls.
     */
    private static final String multiPartyVideoDisabledProperty =
      "net.java.sip.communicator.impl.gui.main.call.MULTI_PARTY_VIDEO_DISABLED";

    /**
     * The <tt>CallFrame</tt>s opened by <tt>CallManager</tt> (because
     * <tt>CallConference</tt> does not give access to such lists.)
     */
    private static final Map<CallConference, CallFrame> callFrames
        = new HashMap<>();

    /**
     * The current incoming calls
     */
    private static final Map<String, ReceivedCallDialogCreator> incomingCalls = new HashMap<>();

    /**
     * The current outgoing calls
     */
    private static final Map<String, Call> outgoingCalls = new HashMap<>();

    /**
     * The regex pattern for characters to strip from phone numbers.
     */
    private static final String PHONE_NUMBER_IGNORE_REGEX =
                                ConfigurationUtils.getPhoneNumberIgnoreRegex();

    /**
     * The PhoneNumberUtilsService
     */
    private static final PhoneNumberUtilsService phoneNumberUtils =
                                            GuiActivator.getPhoneNumberUtils();

    /**
     * Keeps track of the CallDialogs that are currently active, so that we can
     * position new CallDialogs without obscuring other ones.
     */
    private static final WindowStacker windowStacker = new WindowStacker();

    /*
     * Constants to help distinguish video/audio
     */
    private static final boolean VIDEO = true;
    private static final boolean NOT_VIDEO = false;

    /**
     * A <tt>CallListener</tt>.
     */
    public static class GuiCallListener
        extends SwingCallListener
    {
        /**
         * Implements {@link CallListener#incomingCallReceived(CallEvent)}. When
         * a call is received, creates a <tt>ReceivedCallDialog</tt> and plays
         * the ring phone sound to the user.
         *
         * @param ev the <tt>CallEvent</tt>
         */
        @Override
        public void incomingCallReceivedInEventDispatchThread(CallEvent ev)
        {
            Call sourceCall = ev.getSourceCall();
            final ReceivedCallDialogCreator receivedCallDialog;

            /*
             * This bundle is loaded at the same time as the HeadsetManagerImpl bundle
             * which causes a race condition that can lead to a static field
             * for GuiActivator.getHeadsetManager() defined in CallManager to be null
             * when CallManager is loaded
             */
            HeadsetManagerService headsetManagerService = GuiActivator.getHeadsetManager();

            if (!ev.isAutoAnswered())
            {
                logger.debug("Showing received call dialog for NOT auto-answered call: " + sourceCall);

                try
                {
                    /*
                     * A race condition is possible when a CALL_RECEIVED is fired
                     * and just before creating a ReceivedCallDialogCreator
                     * a CALL_ENDED event is fired and removes the peers from the call
                     * causing the IllegalStateException in ReceivedCallDialogCreator
                     */
                    receivedCallDialog = new ReceivedCallDialogCreator(sourceCall);
                }
                catch (IllegalStateException ex)
                {
                    logger.warn("Could not create received call dialog: " + ex);
                    return;
                }

                incomingCalls.put(sourceCall.getCallID(), receivedCallDialog);

                ConfigurationService cfg = GuiActivator.getConfigurationService();
                if (cfg.global().getBoolean("plugin.wispa.SHOW_INCOMING_CALL_FRAME", false))
                {
                    receivedCallDialog.show();
                }

                headsetManagerService.incoming(sourceCall.getCallID());
            }
            else
            {
                logger.debug(
                    "NOT showing received call dialog for auto-answered call: " +
                                                                     sourceCall);
                receivedCallDialog = null;
            }

            Iterator<? extends CallPeer> peerIter = sourceCall.getCallPeers();

            if(!peerIter.hasNext())
            {
                if ((receivedCallDialog != null) && receivedCallDialog.isVisible())
                    receivedCallDialog.dispose();
                return;
            }

            CallPeer peer = peerIter.next();
            final String peerName = peer.getDisplayName();

            // Adds a call peer listener and a property change listener to
            // listen to changes in hold and mute states
            addCallPeerListeners(peer);

            final long callTime = System.currentTimeMillis();

            sourceCall.addCallChangeListener(new CallChangeAdapter()
            {
                @Override
                public void callStateChanged(final CallChangeEvent ev)
                {
                    if (!SwingUtilities.isEventDispatchThread())
                    {
                        SwingUtilities.invokeLater(()->callStateChanged(ev));
                        return;
                    }

                    // When the call state changes, we ensure here that the
                    // received call notification dialog is closed.
                    if ((receivedCallDialog != null) && receivedCallDialog.isVisible())
                    {
                        receivedCallDialog.dispose();
                    }

                    // Ensure that the CallDialog is created, because it is the
                    // one that listens for CallPeers.
                    Object newValue = ev.getNewValue();
                    Call call = ev.getSourceCall();
                    String callID = call.getCallID();

                    if (CallState.CALL_INITIALIZATION.equals(newValue)
                            || CallState.CALL_IN_PROGRESS.equals(newValue))
                    {
                        openCallContainerIfNecessary(call);

                        headsetManagerService.incomingAnswered(callID);
                        incomingCalls.remove(callID);

                        // Set the global presence status to 'on the phone'.
                        // This is done here, rather than at the start of
                        // "incomingCallReceivedInEventDispatchThread" so that
                        // the status is not set until the call is actually
                        // answered, rather than when it starts ringing.
                        try
                        {
                            globalStatusService.setIsOnThePhone(true, true);
                        }
                        catch (Exception e)
                        {
                            logger.error("Failed to set global call status to on the phone", e);
                        }
                    }
                    else if (CallState.CALL_ENDED.equals(newValue))
                    {
                        if (ev.getOldValue().equals(
                                CallState.CALL_INITIALIZATION))
                        {
                            // If the call was answered elsewhere, don't mark it
                            // as missed.
                            CallPeerChangeEvent cause = ev.getCause();

                            if ((cause == null)
                                    || (cause.getReasonCode()
                                            != CallPeerChangeEvent
                                                    .NORMAL_CALL_CLEARING))
                            {
                                addMissedCallNotification(peerName, callTime);
                            }
                        }

                        headsetManagerService.ended(callID);
                        call.removeCallChangeListener(this);
                        incomingCalls.remove(callID);
                    }
                }
            });

            /*
             * Notify the existing CallPanels about the CallEvent (in case they
             * need to update their UI, for example).
             */
            forwardCallEventToCallPanels(ev);
        }

        /**
         * Implements CallListener.callEnded. Stops sounds that are playing at
         * the moment if there are any. Removes the <tt>CallPanel</tt> and
         * disables the hang-up button.
         *
         * @param ev the <tt>CallEvent</tt> which specifies the <tt>Call</tt>
         * that has ended
         */
        @Override
        public void callEndedInEventDispatchThread(CallEvent ev)
        {
            CallConference callConference = ev.getCallConference();

            closeCallContainerIfNotNecessary(callConference);

            try
            {
                // This call has ended, so clear the 'on the phone' global
                // presence status, if there are no remaining active calls.
                List<Call> activeCalls = getActiveCalls();
                if (activeCalls.isEmpty())
                {
                    globalStatusService.setIsOnThePhone(false, true);
                }
                else if (activeCalls.size() == 1)
                {
                    // There is only one participant left, so re-enable video
                    // telephony
                    Call call = activeCalls.get(0);
                    OperationSetVideoTelephony videoTelephony =
                             call.getProtocolProvider().getOperationSet(
                                                  OperationSetVideoTelephony.class);

                    try
                    {
                        if (videoTelephony != null && isMultiPartyVideoDisabled())
                        {
                            videoTelephony.setRemoteVideoAllowed(call, true);
                        }
                    }
                    catch (OperationFailedException e)
                    {
                        logger.error("Could not re-enable video telephony for: " +
                                                            call.getCallPeers());
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Failed to set update global call status: ", e);
            }

            /*
             * Notify the existing CallPanels about the CallEvent (in case
             * they need to update their UI, for example).
             */
            forwardCallEventToCallPanels(ev);
        }

        /**
         * Creates and opens a call dialog. Implements
         * {@link CallListener#outgoingCallCreated(CallEvent)}.
         *
         * @param ev the <tt>CallEvent</tt>
         */
        @Override
        public void outgoingCallCreatedInEventDispatchThread(CallEvent ev)
        {
            Call sourceCall = ev.getSourceCall();

            HeadsetManagerService headsetManagerService = GuiActivator.getHeadsetManager();

            try
            {
                // We're creating a new outgoing call, so set the global presence
                // status to 'on the phone'.
                globalStatusService.setIsOnThePhone(true, true);
            }
            catch (Exception e)
            {
                logger.error("Failed to set update global call status: ", e);
            }

            outgoingCalls.put(sourceCall.getCallID(), sourceCall);

            headsetManagerService.outgoing(sourceCall.getCallID());

            // Adds a call peer listener and a property change listener to
            // listen to changes in hold and mute states
            Iterator<? extends CallPeer> callPeers = sourceCall.getCallPeers();
            if (callPeers.hasNext())
            {
                addCallPeerListeners(callPeers.next());
            }

            sourceCall.addCallChangeListener(new CallChangeAdapter()
            {
                @Override
                public void callStateChanged(final CallChangeEvent ev)
                {
                    if(!SwingUtilities.isEventDispatchThread())
                    {
                        SwingUtilities.invokeLater(
                                new Runnable()
                                {
                                    public void run()
                                    {
                                        callStateChanged(ev);
                                    }
                                });
                        return;
                    }
                    Call call = ev.getSourceCall();
                    String callID = call.getCallID();

                    if (CallState.CALL_IN_PROGRESS.equals(ev.getNewValue()))
                    {
                        headsetManagerService.outgoingAnswered(callID);
                        outgoingCalls.remove(callID);
                    }
                    if (CallState.CALL_ENDED.equals(ev.getNewValue()))
                    {
                        headsetManagerService.ended(callID);
                        call.removeCallChangeListener(this);
                        outgoingCalls.remove(callID);
                    }
                }
            });

            openCallContainerIfNecessary(sourceCall);

            /*
             * Notify the existing CallPanels about the CallEvent (in case they
             * need to update their UI, for example).
             */
            forwardCallEventToCallPanels(ev);
        }

        /**
         * Creates a new call peer listener which listens for changes to the
         * 'on hold' state of the call peer, and informs the Headset Manager
         * of any changes to this state.
         * @return A new Call Peer Listener
         */
        private CallPeerListener getCallPeerListener()
        {
            HeadsetManagerService headsetManagerService = GuiActivator.getHeadsetManager();

            return new CallPeerAdapter()
            {
                @Override
                public void peerStateChanged(CallPeerChangeEvent evt)
                {
                    Call call = evt.getSourceCallPeer().getCall();
                    if (call != null)
                    {
                        String callID = call.getCallID();
                        if (evt.getNewValue().equals(CallPeerState.ON_HOLD_LOCALLY))
                        {
                            // Call has just been put on hold.
                            headsetManagerService.setActive(callID, false);
                        }

                        if(evt.getNewValue().equals(CallPeerState.CONNECTED))
                        {
                            // Call has just been taken off hold.
                            headsetManagerService.setActive(callID, true);
                        }
                    }

                }
            };
        }

        /**
         * Creates a new property change listener which listens for changes to
         * the MUTE_PROPERTY_NAME property on the call peer and informs the
         * Headset Manager of any changes to this state.
         * @return A PropertyChangeListener
         */
        private PropertyChangeListener getCallPeerPropertyChangeListener()
        {
            HeadsetManagerService headsetManagerService = GuiActivator.getHeadsetManager();

            return new PropertyChangeListener()
            {
                /**
                 * When the MUTE_PROPERTY_NAME property has changed, we notify
                 * HeadsetManager of the change in mute state.
                 */
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    if (evt.getPropertyName().equals(CallPeer.MUTE_PROPERTY_NAME))
                    {
                        CallPeer sourcePeer = (CallPeer) evt.getSource();
                        headsetManagerService.microphoneMuteChanged(
                            (Boolean) evt.getNewValue(),
                            sourcePeer.getCall().getCallID()
                            );
                    }
                }
            };
        }

        /**
         * Adds a call peer listener that listens for hold state changes and
         * a property change listener that listens for mute state changes. The
         * listeners then report the changes to headset manager.
         * @param callPeer
         */
        private void addCallPeerListeners(CallPeer callPeer)
        {
            // Add a call peer listener so we are notified of changes to the
            // 'on hold' status of the call
            callPeer.addCallPeerListener(getCallPeerListener());

            // Add a property change listener so we are notified of changes
            // to the 'mute' state
            callPeer.addPropertyChangeListener(getCallPeerPropertyChangeListener());
        }
    }

    /**
     * A <tt>HeadsetManagerListener</tt>.
     */
    public static class CallHeadsetManagerListener
        implements HeadsetManagerListener
    {
        @Override
        public void muteStateChanged(boolean mute, String callID)
        {
            // Look for the call with the input callID and set that call's mute
            for (Call activeCall : getActiveCalls())
            {
                if (activeCall.getCallID().equals(callID))
                {
                    OperationSetBasicTelephony<?> telephony = activeCall
                        .getProtocolProvider()
                        .getOperationSet(OperationSetBasicTelephony.class);
                    telephony.setMute(activeCall, mute);
                }
            }
        }

        @Override
        public void hangUp(String callID)
        {
            // Headset Manager needs to be notified when the call has actually
            // been hung up.
            for (Call activeCall : getActiveCalls())
            {
                if (activeCall.getCallID().equals(callID))
                {
                    hangupCall(activeCall);
                }
            }
        }

        @Override
        public void cancelOutgoing(String callID)
        {
            // Headset Manager needs to be notified when the call has actually
            // been hung up.
            // hangupCall can handle null input
            hangupCall(outgoingCalls.get(callID));
        }

        @Override
        public void answer(String callID)
        {
            ReceivedCallDialogCreator dialog = incomingCalls.get(callID);
            if (dialog != null)
            {
                dialog.okButtonPressed(AnalyticsParameter.VALUE_HEADSET);
            }
        }

        @Override
        public void rejectIncoming(String callID)
        {
            // Headset Manager needs to be notified when the call has actually been hung up.
            ReceivedCallDialogCreator dialog = incomingCalls.get(callID);
            if (dialog != null)
            {
                dialog.cancelButtonPressed(AnalyticsParameter.VALUE_HEADSET);
            }
        }
    }

    /**
     * Answers the given call.
     *
     * @param call the call to answer
     */
    public static void answerCall(Call call)
    {
        answerCall(call, null, false /* without video */);
    }

    /**
     * Answers a specific <tt>Call</tt> with or without video and, optionally,
     * does that in a telephony conference with an existing <tt>Call</tt>.
     *
     * @param call
     * @param existingCall
     * @param video
     */
    private static void answerCall(Call call, Call existingCall, boolean video)
    {
        if (call != null)
        {
            OperationSetVideoTelephony osvt = call.getProtocolProvider().
                                  getOperationSet(OperationSetVideoTelephony.class);

            if (osvt != null && isMultiPartyVideoDisabled())
            {
                try
                {
                    osvt.setRemoteVideoAllowed(call, true);
                }
                catch (OperationFailedException e)
                {
                    logger.error("Failed to set remote video allowed for call: " +
                                 call);
                }
            }

            if (existingCall == null)
                openCallContainerIfNecessary(call);

            new AnswerCallThread(call, existingCall, video).start();
        }
        else
        {
            logger.info("Cannot answer null call");
        }
    }

    /**
     * Merges specific existing <tt>Call</tt>s into a specific telephony
     * conference.
     *
     * @param conference the conference
     * @param calls list of calls
     */
    public static void mergeExistingCalls(
            CallConference conference,
            Collection<Call> calls)
    {
        GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.ASKED_TO_MERGE);
        new MergeExistingCalls(conference, calls).start();
    }

    /**
     * Bring the call window to the front and take keyboard focus
     * Do nothing if no call window exists
     *
     * @param call the call to focus
     */
    public static void focusCall(Call call)
    {
        CallConference conference = call.getConference();
        CallFrame callFrame = findCallFrame(conference);
        if (callFrame != null) {
            activateApplication();
            callFrame.setState(Frame.NORMAL);
            callFrame.setVisible(true, false);
        }
    }

    /**
     * Java windows cannot take focus on Mac if the application is not active
     */
    private static void activateApplication()
    {
        try
        {
            String uri = ConfigurationUtils.getDesktopUriScheme();
            if (uri != null)
            {
                Runtime.getRuntime().exec("open " + uri + ":///");
            }
        }
        catch (IOException e)
        {
            logger.error("Exception hit when trying to activate application: " + e);
        }
    }

    /**
     * Hangs up the given call.
     *
     * @param call the call to hang up
     */
    public static void hangupCall(Call call)
    {
        if (call != null)
        {
            new HangupCallThread(call).start();
        }
        else
        {
            logger.info("Cannot hang up null call");
        }
    }

    /**
     * Hang ups the given <tt>callPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> to hang up
     */
    public static void hangupCallPeer(CallPeer peer)
    {
        new HangupCallThread(peer).start();
    }

    /**
     * Asynchronously hangs up the <tt>Call</tt>s participating in a specific
     * <tt>CallConference</tt>.
     *
     * @param conference the <tt>CallConference</tt> whose participating
     * <tt>Call</tt>s are to be hanged up
     */
    public static void hangupCalls(CallConference conference)
    {
        new HangupCallThread(conference).start();
    }

    /**
     * Creates a call to the given call string.
     *
     * @param callString the string to call
     */
    public static void createCall(String callString, Reformatting reformatNeeded)
    {
        createCall(callString, null, reformatNeeded);
    }

    /**
     * Creates a call to the given call string.
     *
     * @param callString the string to call
     * @param l listener that is notified when the call interface has been
     * started after call was created
     */
    public static void createCall(String callString,
                                  CallInterfaceListener l,
                                  Reformatting reformatNeeded)
    {
        callString = callString.trim();

        List<ProtocolProviderService> telephonyProviders
            = CallManager.getAllTelephonyProviders(callString);

        if (telephonyProviders.size() == 1 &&
            telephonyProviders.get(0).isRegistered())
        {
            CallManager.createCall(
                telephonyProviders.get(0), callString, reformatNeeded);

            if (l != null)
                l.callInterfaceStarted();
        }
        else if (telephonyProviders.size() > 1)
        {
            /*
             * Allow plugins which do not have a (Jitsi) UI to create calls by
             * automagically picking up a telephony provider.
             */
            new ChooseCallAccountDialog(callString,
                                        null,
                                        OperationSetBasicTelephony.class,
                                        telephonyProviders,
                                        reformatNeeded).setVisible(true);

            if (l != null)
                l.callInterfaceStarted();
        }
        else
        {
            logger.info("Tried to make call to offline provider");
            showNotOnlineWarning();
        }
    }

    /**
     * Creates a call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     */
    public static void createCall(ProtocolProviderService protocolProvider,
                                  String contact,
                                  Reformatting reformatNeeded)
    {
        new CreateCallThread(protocolProvider,
                             null,
                             null,
                             contact,
                             null,
                             NOT_VIDEO,
                             reformatNeeded).start();
    }

    /**
     * Creates a call to the contact represented by the given string, and
     * associates the call with the given contact
     *
     * @param protocolProvider the protocol provider to which this call belongs
     * @param contact the contact to associate the call with
     * @param contactDisplayName optional: the user-visible name of the contact
     * to call.
     * @param stringContact the contact address to call to
     */
    public static void createCall(ProtocolProviderService protocolProvider,
                                  Contact contact,
                                  String contactDisplayName,
                                  String stringContact,
                                  Reformatting reformatNeeded)
    {
        new CreateCallThread(protocolProvider,
                             contact,
                             null,
                             stringContact,
                             contactDisplayName,
                             NOT_VIDEO,
                             reformatNeeded).start();
    }

    /**
     * Creates a call to the contact represented by the given string, and
     * associates the call with the given contact
     *
     * @param protocolProvider the protocol provider to which this call belongs
     * @param stringContact the contact address to call to
     * @param displayName the string to display in the in-call window
     */
    public static void createCall(ProtocolProviderService protocolProvider,
                                  String stringContact,
                                  String displayName,
                                  Reformatting reformatNeeded)
    {
        new CreateCallThread(protocolProvider,
                             null,
                             null,
                             stringContact,
                             displayName,
                             NOT_VIDEO,
                             reformatNeeded).start();
    }

    /**
     * Creates a call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     */
    public static void createCall(ProtocolProviderService protocolProvider,
                                  Contact contact,
                                  Reformatting reformatNeeded)
    {
        new CreateCallThread(protocolProvider,
                             contact,
                             null,
                             null,
                             null,
                             NOT_VIDEO,
                             reformatNeeded).start();
    }

    /**
     * Creates a video call to the contact represented by the given string, and
     * associates the call with the given contact
     *
     * @param protocolProvider the protocol provider to which this call belongs
     * @param contact the contact to associate the call with
     * @param contactDisplayName optional - the user-visible name of the contact
     * to call.
     * @param stringContact the contact address to call to
     */
    public static void createVideoCall(ProtocolProviderService protocolProvider,
                                       Contact contact,
                                       String contactDisplayName,
                                       String stringContact,
                                       Reformatting reformatNeeded)
    {
        new CreateCallThread(protocolProvider,
                             contact,
                             null,
                             stringContact,
                             contactDisplayName,
                             VIDEO,
                             reformatNeeded).start();
    }

    /**
     * Enables/disables local video for a specific <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> to enable/disable to local video for
     * @param enable <tt>true</tt> to enable the local video; otherwise,
     * <tt>false</tt>
     */
    public static void enableLocalVideo(Call call, boolean enable)
    {
        if (enable)
        {
            AnalyticsService analytics = GuiActivator.getAnalyticsService();
            analytics.onEventWithIncrementingCount(AnalyticsEventType.ENABLE_VIDEO, new ArrayList<>());
        }

        GuiActivator.getInsightService().logEvent(
                new InsightEvent(CallKt.EVENT_CALL_SEND_VIDEO,
                                 CallKt.PARAM_CALL_SEND_VIDEO,
                                 String.valueOf(enable))
        );

        new EnableLocalVideoThread(call, enable).start();
    }

    /**
     * Indicates if local video  is currently enabled for the given<tt>call</tt>.
     *
     * @param call the <tt>Call</tt>, for which we would to check if local
     * video is currently enabled
     * @return <tt>true</tt> if local video is currently enabled for the
     * given <tt>call</tt>, <tt>false</tt> otherwise
     */
    public static boolean isLocalVideoEnabled(Call call)
    {
        OperationSetVideoTelephony telephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetVideoTelephony.class);

        return (telephony != null) && telephony.isLocalVideoAllowed(call);
    }

    /**
     * Updates the enabled state of the video buttons in all in progress call
     * frames. If any video devices are available, the buttons will be
     * enabled, otherwise they will be disabled.
     */
    public static void updateVideoButtonsEnabledState()
    {
        logger.info("Asked to update video button enabled state for all call frames");
        synchronized (callFrames)
        {
            Iterator<Map.Entry<CallConference, CallFrame>> callFramesIter =
                                              callFrames.entrySet().iterator();

            while(callFramesIter.hasNext())
            {
                Map.Entry<CallConference, CallFrame> entry = callFramesIter.next();
                CallFrame callFrame = entry.getValue();
                callFrame.updateVideoButtonEnabledState();
            }
        }
    }

    /**
     * Shows a warning that the selected provider is not online
     */
    public static void showNotOnlineWarning()
    {
        String message = resources.getI18NString("service.gui.NO_ONLINE_TELEPHONY_ACCOUNT");
        String title = resources.getI18NString("service.gui.WARNING");

        new ErrorDialog(title, message).showDialog();
    }

    /**
     * Invites the given list of <tt>callees</tt> to the given conference
     * <tt>call</tt>.
     * @param call the protocol provider to which this call belongs
     * @param callees the list of contacts to invite
     */
    public static void inviteToConferenceCall(Call call,
                                              Reformatting reformattingNeeded,
                                              String... callees)
    {
        Map<ProtocolProviderService, List<String>> crossProtocolCallees
            = new HashMap<>();

        crossProtocolCallees.put(
                call.getProtocolProvider(),
                Arrays.asList(callees));

        new InviteToConferenceCallThread(crossProtocolCallees, reformattingNeeded, call).start();
    }

    /**
     * Invites specific <tt>callees</tt> to a specific telephony conference.
     *
     * @param callees the list of contacts to invite
     * @param conference the telephony conference to invite the specified
     * <tt>callees</tt> into
     */
    public static void inviteToConferenceCall(
            Map<ProtocolProviderService, List<String>> callees,
            Reformatting reformattingNeeded,
            CallConference conference)
    {
        /*
         * InviteToConferenceCallThread takes a specific Call but actually
         * invites to the telephony conference associated with the specified
         * Call (if any). In order to not change the signature of its
         * constructor at this time, just pick up a Call participating in the
         * specified telephony conference (if any).
         */
        Call call = null;

        if (conference != null)
        {
            List<Call> calls = conference.getCalls();

            if (!calls.isEmpty())
                call = calls.get(0);
        }

        new InviteToConferenceCallThread(callees, reformattingNeeded, call).start();
    }

    /**
     * Puts on or off hold the given <tt>callPeer</tt>.
     * @param callPeer the peer to put on/off hold
     * @param isOnHold indicates the action (on hold or off hold)
     */
    public static void putOnHold(CallPeer callPeer, boolean isOnHold)
    {
        new PutOnHoldCallPeerThread(callPeer, isOnHold).start();
    }

    /**
     * Transfers the given <tt>peer</tt> to the given <tt>target</tt>.
     * @param peer the <tt>CallPeer</tt> to transfer
     * @param target the <tt>CallPeer</tt> target to transfer to
     */
    public static void transferCall(CallPeer peer, CallPeer target)
    {
        OperationSetAdvancedTelephony<?> telephony
            = peer.getCall().getProtocolProvider()
                .getOperationSet(OperationSetAdvancedTelephony.class);

        if (telephony != null)
        {
            try
            {
                telephony.transfer(peer, target);
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to transfer " + peer.getAddress()
                    + " to " + target, ex);
            }
        }
    }

    /**
     * Transfers the given <tt>peer</tt> to the given <tt>target</tt>.
     * @param peer the <tt>CallPeer</tt> to transfer
     * @param target the target of the transfer
     * @param reformattingNeeded whether the peer address should be reformatted
     * for the SIP refer-to header.
     */
    public static void transferCall(CallPeer peer,
                                    String target,
                                    Reformatting reformattingNeeded)
    {
        OperationSetAdvancedTelephony<?> telephony
            = peer.getCall().getProtocolProvider()
                .getOperationSet(OperationSetAdvancedTelephony.class);

        if (telephony != null)
        {
            try
            {
                // Clean the number
                target = target.replaceAll(PHONE_NUMBER_IGNORE_REGEX, "");

                // We want to reformat E164, as E.164 isn't supported
                // in a REFER.  We also want to reformat numbers that
                // have come out of a contact (as they may not have an
                // outside "9").
                if ((target.startsWith("+")) ||
                    (reformattingNeeded == Reformatting.NEEDED))
                {
                    target = phoneNumberUtils.formatNumberForRefer(target);
                }

                telephony.transfer(peer, target);
                GuiActivator.getAnalyticsService().onEvent(
                                          AnalyticsEventType.TRANSFERRED_CALL);
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to transfer " + peer.getAddress()
                    + " to " + target, ex);
            }
        }
    }

    /**
     * Closes the <tt>CallPanel</tt> of a specific <tt>Call</tt> if it is no
     * longer necessary (i.e. is not used by other <tt>Call</tt>s participating
     * in the same telephony conference as the specified <tt>Call</tt>.)
     *
     * @param callConference The <tt>CallConference</tt> which is to have its
     * associated <tt>CallPanel</tt>, if any, closed
     */
    private static void closeCallContainerIfNotNecessary(
            final CallConference callConference)
    {
        CallFrame callFrame = callFrames.get(callConference);

        if (callFrame != null)
        {
            closeCallContainerIfNotNecessary();
        }
    }

    /**
     * Closes all ended callFrames
     */
    private static void closeCallContainerIfNotNecessary()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    () -> closeCallContainerIfNotNecessary());
            return;
        }

        /*
         * XXX The integrity of the execution of the method may be compromised
         * if it is not invoked on the AWT event dispatching thread because
         * findCallPanel and callPanels.remove must be atomically executed. The
         * uninterrupted execution (with respect to the synchronization) is
         * guaranteed by requiring all modifications to callPanels to be made on
         * the AWT event dispatching thread.
         */

        for (Iterator<Map.Entry<CallConference, CallFrame>> entryIter
                    = callFrames.entrySet().iterator();
                entryIter.hasNext();)
        {
            Map.Entry<CallConference, CallFrame> entry = entryIter.next();
            CallConference aConference = entry.getKey();
            boolean notNecessary = aConference.isEnded();

            if (notNecessary)
            {
                CallFrame callFrame = entry.getValue();

                /*
                 * We allow non-modifications i.e. reads of callPanels on
                 * threads other than the AWT event dispatching thread so we
                 * have to make sure that we will not cause
                 * ConcurrentModificationException.
                 */
                synchronized (callFrames)
                {
                    entryIter.remove();
                }

                callFrame.dispose();
            }
        }
    }

    /**
     * Opens a <tt>CallPanel</tt> for a specific <tt>Call</tt> if there is none.
     * <p>
     * <b>Note</b>: The method can be called only on the AWT event dispatching
     * thread.
     * </p>
     *
     * @param call the <tt>Call</tt> to open a <tt>CallPanel</tt> for
     * @throws RuntimeException if the method is not called on the AWT event
     * dispatching thread
     */
    private static void openCallContainerIfNecessary(Call call)
    {
        /*
         * XXX The integrity of the execution of the method may be compromised
         * if it is not invoked on the AWT event dispatching thread because
         * findCallPanel and callPanels.put must be atomically executed. The
         * uninterrupted execution (with respect to the synchronization) is
         * guaranteed by requiring all modifications to callPanels to be made on
         * the AWT event dispatching thread.
         */
        assertIsEventDispatchingThread();

        /*
         * CallPanel displays a CallConference (which may contain multiple
         * Calls.)
         */
        CallConference conference = call.getConference();
        CallFrame callFrame = findCallFrame(conference);

        logger.info("Asked to open a call frame for call: " + call.getLoggableCallID());

        if (callFrame == null)
        {
            // Get the call peers for this call
            Iterator<? extends CallPeer> callPeers = call.getCallPeers();
            while (callPeers.hasNext())
            {
                CallPeer callPeer = callPeers.next();

                synchronized (callFrames)
                {
                    logger.info("Creating new call frame for peer: " +
                                callPeer.getLoggableAddress());
                    callFrame = new CallFrame(callPeer, conference);
                    windowStacker.addWindow(callFrame);

                    ConfigurationService cfg = GuiActivator.getConfigurationService();
                    if (cfg.global().getBoolean("plugin.wispa.SHOW_CALL_FRAME", true))
                    {
                        // Activate the application before setting the window to be visible and
                        // requesting focus, otherwise the window fails to take focus.
                        activateApplication();
                        callFrame.setVisible(true);
                    }

                    callFrames.put(conference, callFrame);
                }
            }

            // Alert all current call frames that a new call has started
            forwardCallEventToCallPanels(null);
        }
    }

    /**
     * Returns a list of all currently registered telephony providers.
     * @return a list of all currently registered telephony providers
     */
    public static List<ProtocolProviderService> getTelephonyProviders()
    {
        return AccountUtils
            .getRegisteredProviders(OperationSetBasicTelephony.class);
    }

    /**
     * Returns a list of all telephony providers, regardless of if they are
     * registered or not
     * @return a list of the telephony providers
     */
    public static List<ProtocolProviderService> getAllTelephonyProviders()
    {
        return AccountUtils.getAllProviders(OperationSetBasicTelephony.class);
    }

    /**
     * Returns a list of all telephony providers that can be used to call the given number,
     * regardless of if they are registered or not,
     * @return a list of the telephony providers
     */
    public static List<ProtocolProviderService> getAllTelephonyProviders(String number)
    {
        List<ProtocolProviderService> providers = getAllTelephonyProviders();

        if (GuiActivator.getCommPortalService().doesCallRequireLocationInformation(number))
        {
            // Only keep the telephony providers that support location information
            for (ProtocolProviderService provider : providers)
            {
                OperationSetBasicTelephony telephony = provider.getOperationSet(OperationSetBasicTelephony.class);

                if (!telephony.usesLocationInformation())
                {
                    if (providers.size() > 1)
                    {
                        logger.warn("Discard provider " + provider + " since location support not provided");
                        providers.remove(provider);
                    }
                    else
                    {
                        logger.warn("Not removing provider " + provider + " without location support since it is the only provider");
                    }
                }
            }
        }
        else if (!ConfigurationUtils.isVoIPEnabled())
        {
            // VOIP calling is disabled, so make sure we don't return the SIP provider
            for (ProtocolProviderService provider : providers)
            {
                // We can't use instanceof due to classloader issues with the
                // bundle and plugin architecture
                if (provider.getClass().getCanonicalName().equals("ProtocolProviderServiceSipImpl"))
                {
                    providers.remove(provider);
                    break;
                }
            }
        }

        return providers;
    }

    /**
     * Returns a list of all currently active calls.
     *
     * @return a list of all currently active calls
     */
    private static List<Call> getActiveCalls()
    {
        CallConference[] conferences;

        synchronized (callFrames)
        {
            Set<CallConference> keySet = callFrames.keySet();

            conferences = keySet.toArray(new CallConference[keySet.size()]);
        }

        List<Call> calls = new ArrayList<>();

        for (CallConference conference : conferences)
        {
            for (Call call : conference.getCalls())
            {
                if (call.getCallState() == CallState.CALL_IN_PROGRESS)
                    calls.add(call);
            }
        }
        return calls;
    }

    /**
     * Returns a collection of all currently in progress calls. A call is active
     * if it is in progress so the method merely delegates to
     * {@link #getActiveCalls()}.
     *
     * @return a collection of all currently in progress calls.
     */
    public static Collection<Call> getInProgressCalls()
    {
        return getActiveCalls();
    }

    /**
     * Returns a collection of all currently in progress video calls. A call is
     * active if it is in progress so the method merely delegates to
     * {@link #getActiveCalls()}.
     *
     * @return a collection of all currently in progress video calls.
     */
    public static Collection<Call> getInProgressVideoCalls()
    {
        return getActiveCalls()
            .stream()
            .filter(call -> call.getProtocolProvider()
                                .getOperationSet(OperationSetVideoTelephony.class) != null)
            .filter(call -> call.getProtocolProvider()
                                .getOperationSet(OperationSetVideoTelephony.class)
                                .isLocalVideoStreaming(call))
            .collect(Collectors.toList());
    }

    /**
     * Returns the <tt>CallContainer</tt> corresponding to the given
     * <tt>call</tt>. If the call has been finished and no active
     * <tt>CallContainer</tt> could be found it returns null.
     *
     * @param call the <tt>Call</tt>, which dialog we're looking for
     * @return the <tt>CallContainer</tt> corresponding to the given
     * <tt>call</tt>
     */
    public static CallFrame getActiveCallContainer(Call call)
    {
        return findCallFrame(call.getConference());
    }

    /**
     * Returns the image corresponding to the given <tt>peer</tt>.
     *
     * @param peer the call peer, for which we're returning an image
     * @return the peer image
     */
    public static BufferedImageFuture getPeerImage(CallPeer peer)
    {
        BufferedImageFuture image = null;
        // We search for a contact corresponding to this call peer and
        // try to get its image.
        if (peer.getContact() != null)
        {
            MetaContact metaContact = GuiActivator.getContactListService()
                .findMetaContactByContact(peer.getContact());

            image = metaContact.getAvatar();
        }

        // If the icon is still null we try to get an image from the call
        // peer.
        if ((image == null)
                && peer.getImage() != null)
            image = peer.getImage();

        return image;
    }

    /**
     * Opens a call transfer dialog to transfer the given <tt>peer</tt>.
     * @param peer the <tt>CallPeer</tt> to transfer
     */
    public static void openCallTransferDialog(CallPeer peer)
    {
        final TransferCallDialog dialog = new TransferCallDialog(peer);
        final Call call = peer.getCall();

        /*
         * Transferring a call works only when the call is in progress
         * so close the dialog (if it's not already closed, of course)
         * once the dialog ends.
         */
        CallChangeListener callChangeListener = new CallChangeAdapter()
        {
            /*
             * Overrides
             * CallChangeAdapter#callStateChanged(CallChangeEvent).
             */
            @Override
            public void callStateChanged(CallChangeEvent evt)
            {
                // we are interested only in CALL_STATE_CHANGEs
                if(!evt.getEventType().equals(
                    CallChangeEvent.CALL_STATE_CHANGE))
                    return;

                if (!CallState.CALL_IN_PROGRESS.equals(call.getCallState()))
                {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }
        };

        call.addCallChangeListener(callChangeListener);

        try
        {
            dialog.pack();
            dialog.setVisible(true);
        }
        finally
        {
            call.removeCallChangeListener(callChangeListener);
        }
    }

    /**
     * Adds a missed call notification.
     *
     * @param peerName the name of the peer
     * @param callTime the time of the call
     */
    private static void addMissedCallNotification(String peerName, long callTime)
    {
        // Do nothing if network call history is enabled - a missed call here
        // may have been answered elsewhere.
        if (ConfigurationUtils.isNetworkCallHistoryEnabled())
            return;

        logger.debug("Firing missed call notification");
        GuiActivator.getNotificationService().fireNotification("MissedCall");
    }

    /**
     * Creates a new (audio-only or video) <tt>Call</tt> to a contact specified
     * as a <tt>Contact</tt> instance or a <tt>String</tt> contact
     * address/identifier.
     */
    @VisibleForTesting
    static class CreateCallThread
        extends Thread
    {
        private static final Pattern PHONE_NUMBER_VALIDATER =
              Pattern.compile(ConfigurationUtils.getPhoneNumberCallableRegex());

        /**
         * The contact to call.
         */
        private final Contact contact;

        /**
         * The specific contact resource to call.
         */
        private final ContactResource contactResource;

        /**
         * The protocol provider through which the call goes.
         */
        private final ProtocolProviderService protocolProvider;

        /**
         * The string to call.
         */
        private final String stringContact;

        /**
         * The indicator which determines whether this instance is to create a
         * new video (as opposed to audio-only) <tt>Call</tt>.
         */
        private final boolean video;

        private final String displayName;

        /*
         * Whether we need to reformat the phone number to E164 or with the ELC.
         */
        private final Reformatting reformatting;

        /**
         * Initializes a new <tt>CreateCallThread</tt> instance which is to
         * create a new <tt>Call</tt> to a contact specified either as a
         * <tt>Contact</tt> instance or as a <tt>String</tt> contact
         * address/identifier.
         * <p>
         * The constructor is private because it relies on its arguments being
         * validated prior to its invocation.
         * </p>
         *
         * @param protocolProvider the <tt>ProtocolProviderService</tt> which is
         * to perform the establishment of the new <tt>Call</tt>
         * @param contact the contact to call
         * @param contactResource the specific contact resource to call
         * @param stringContact the string to call
         * @param displayName the display name of the contact to call (optional)
         * @param video <tt>true</tt> if this instance is to create a new video
         * (as opposed to audio-only) <tt>Call</tt>
         * @param reformatNeeded whether the number needs to be reformatted
         *        as E164 or with the ELC
         */
         @VisibleForTesting
         CreateCallThread(
                ProtocolProviderService protocolProvider,
                Contact contact,
                ContactResource contactResource,
                String stringContact,
                String displayName,
                boolean video,
                Reformatting reformatNeeded)
        {
            logger.debug("Creating call to " + logHasher(stringContact));

            // Remove whitespace characters from the phone number
            stringContact = stringContact.replaceAll(PHONE_NUMBER_IGNORE_REGEX, "");
            logger.debug("(Called number is '" + logHasher(stringContact) +
                                           "' after stripping invalid chars.)");

            this.protocolProvider = protocolProvider;
            this.contact = contact;
            this.contactResource = contactResource;
            this.stringContact = stringContact;
            this.video = video;
            this.displayName = displayName;
            this.reformatting = reformatNeeded;
        }

        @Override
        public void run()
        {
            logger.debug("Running with number " + logHasher(stringContact));

            if (!PHONE_NUMBER_VALIDATER.matcher(stringContact).find())
            {
                // Trying to dial a bad number - show an error message and bail.
                String title = resources.getI18NString("service.gui.ERROR");
                String msg = resources.getI18NString(
                            "service.gui.ERROR_INVALID_NUMBER_DIALED");

                logger.error("Trying to call bad number " + stringContact);
                new ErrorDialog(title, msg).showDialog();
                return;
            }

            if(!video && callWillRequireLocalMedia())
            {
                // if it is not video let's check for available audio codecs
                // and available audio devices, unless this call isn't going to
                // require local media anyway (e.g. it is CTD).
                MediaService mediaService = GuiActivator.getMediaService();
                MediaDevice dev = mediaService.getDefaultDevice(
                   MediaType.AUDIO, MediaUseCase.CALL);

                List<MediaFormat> formats
                    = AccountUtils.getAudioFormats(
                                         dev,
                                         protocolProvider,
                                         mediaService.
                                            createEmptyEncodingConfiguration());

                String errorMsg = null;

                if(!dev.getDirection().allowsSending())
                {
                    errorMsg = resources.getI18NString(
                        "service.gui.CALL_NO_AUDIO_DEVICE");
                }
                else if(formats.isEmpty())
                {
                    errorMsg = resources.getI18NString(
                        "service.gui.CALL_NO_AUDIO_CODEC");
                }

                if(errorMsg != null)
                {
                    if(GuiActivator.getUIService()
                        .getPopupDialog().showConfirmPopupDialog(
                                errorMsg + " " +
                                resources.getI18NString("service.gui.CALL_NO_DEVICE_CODECS_Q"),
                                resources.getI18NString("service.gui.CALL"),
                                PopupDialog.YES_NO_OPTION,
                                PopupDialog.QUESTION_MESSAGE)
                        == PopupDialog.NO_OPTION)
                    {
                        // Send a call failed analytic, then return
                        logger.user("Call attempt ended: " + errorMsg);
                        AnalyticsService analytics = ProtocolMediaActivator.getAnalyticsService();

                        List<AnalyticsParameter> params = new ArrayList<>();

                        params.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_FAILED_ERR,
                                                                AnalyticsParameter.VALUE_FAILED_CONFIG));
                        params.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_DIRECTION,
                                                                AnalyticsParameter.VALUE_OUTBOUND));
                        // Note, this location for sending a call failed event doesn't send network info or
                        // a SIP reason code (but CallPeerMediaHandler.callFailed does).

                        // And send the event.
                        ConfigurationService configService = ProtocolMediaActivator.getConfigurationService();
                        String userName =
                            "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";
                        String dn = configService.global().getString(userName, "Unknown");

                        // Use the same params list for both SAS and Msw targets as it doesn't contain
                        // any PII.
                        analytics.onEvent(AnalyticsEventType.CALL_FAILED,
                                          params,
                                          params,
                                          new int[]{AnalyticsParameter.SAS_MARKER_CALLING_DN},
                                          new String[]{dn});

                        return;
                    }
                }
            }

            Contact contact = this.contact;
            String stringContact = this.stringContact;

            // If stringContact and contact are both present then use the
            // given stringContact to initiate the call as contact may be
            // ambiguous.
            if (contact != null && stringContact != null)
            {
                contact = null;
            }

            try
            {
                if (video)
                {
                    callVideo(protocolProvider, contact, stringContact);
                }
                else
                {
                    EmergencyCallContext emergencyCallContext = null;

                    CommPortalService commPortalService = GuiActivator.getCommPortalService();
                    if (commPortalService.doesCallRequireLocationInformation(stringContact))
                    {
                        AccountID accountId = protocolProvider.getAccountID();
                        String contentId = accountId.getUserID() + "@" + accountId.getService();

                        emergencyCallContext =
                                commPortalService.determineEmergencyCallContext(contentId);

                        // The equivalent Mobile analytic includes additional parameters
                        // for the SIP response for the call, and an indication of
                        // which UI route was used to start the call.
                        // These do not apply to Desktop.
                        GuiActivator
                                .getAnalyticsService()
                                .onEvent(AnalyticsEventType.EMERGENCY_CALL_HANDLED_BY_APP,
                                         AnalyticsParameter.PARAM_EMERGENCY_CALL_LOCATION_SENT,
                                         "" + (emergencyCallContext != null));
                    }

                    Call call = call(protocolProvider,
                                     contact,
                                     stringContact,
                                     displayName,
                                     contactResource,
                                     emergencyCallContext,
                                     reformatting);

                    callSetupActions(call);

                    if ((emergencyCallContext != null) && (call != null) && (call.getCallPeerCount() > 0))
                    {
                        // We are placing an emergency call, including location information.
                        // Retry the call without location information, if it fails.
                        logger.debug("Set up emergency call retry listener");
                        call.getCallPeers().next().addCallPeerListener(
                                getEmergencyCallRetryListener(protocolProvider,
                                                              contact,
                                                              stringContact,
                                                              displayName,
                                                              contactResource,
                                                              reformatting));
                    }
                }
            }
            catch (Exception e)
            {
                handleCallCreationFailure(e);
            }
        }

        // Shared logic for handling failures from the call() method.
        private void handleCallCreationFailure(Exception e)
        {
            logger.error("The call could not be created: ", e);
            String message = resources.getI18NString("service.gui.CREATE_CALL_FAILED");
            new ErrorDialog(resources.getI18NString("service.gui.ERROR"), message).showDialog();
        }

        // Shared logic for setting options relating to a call started by the call() method.
        private void callSetupActions(Call call) throws OperationFailedException
        {
            // If the call has been successfully created then set
            // the call peer appropriately here
            if (call != null)
            {
                // Ensure video streaming is allowed on this call
                OperationSetVideoTelephony videoTelephony = protocolProvider.getOperationSet(
                        OperationSetVideoTelephony.class);

                if (videoTelephony != null && isMultiPartyVideoDisabled())
                {
                    videoTelephony.setRemoteVideoAllowed(call, true);
                }

                MetaContactListService mclService = GuiActivator.getContactListService();

                MetaContact metaContact = (contact != null) ?
                        mclService.findMetaContactByContact(contact) :
                        null;

                Iterator<? extends CallPeer> callPeers = call.getCallPeers();

                // There should only ever be one call peer at this
                // point.
                while (callPeers.hasNext())
                {
                    // If we know the metaContact for the call peer
                    // then set it.
                    CallPeer callPeer = callPeers.next();
                    if (metaContact != null)
                    {
                        callPeer.setMetaContact(metaContact);
                    }
                    else if (displayName != null && !(displayName.isEmpty()))
                    {
                        callPeer.setDisplayName(displayName);
                    }
                }
            }
        }

        // When we place a call to the emergency services that includes location information, the SIP INVITE
        // is larger than normal, because of the additional data in the body.  These larger messages are more
        // likely to breach the MTU size, and in poorly configured networks, this can lead to call failures.
        // Because of the importance of emergency calls getting through, we will retry any such call that fails,
        // but exclude the location information from the retry.  This acts as a backstop, allowing the call to
        // go through, even if we can't transmit the location information.
        private CallPeerListener getEmergencyCallRetryListener(ProtocolProviderService protocolProvider,
                                                               Contact contact,
                                                               String stringContact,
                                                               String displayName,
                                                               ContactResource contactResource,
                                                               Reformatting reformatting)
        {
            return new CallPeerAdapter()
            {
                @Override
                public void peerStateChanged(CallPeerChangeEvent evt)
                {
                    if (evt.getNewValue().equals(CallPeerState.FAILED))
                    {
                        // The call has failed.  Try it again, but without location information.  The retry is
                        // an entirely new call and will result in a second call dialog box appearing.
                        logger.warn("Emergency call failed - retry without location information");
                        try
                        {
                            Call call = call(protocolProvider,
                                             contact,
                                             stringContact,
                                             displayName,
                                             contactResource,
                                             null,
                                             reformatting);
                            callSetupActions(call);

                            // The equivalent Mobile analytic includes the SIP
                            // response code of the failed call.  We do not have
                            // that information to hand, so simply do not include it.
                            GuiActivator
                                    .getAnalyticsService()
                                    .onEvent(AnalyticsEventType.EMERGENCY_CALL_RETRY);
                        }
                        catch (Exception e)
                        {
                            handleCallCreationFailure(e);
                        }
                    }
                    else if (evt.getNewValue().equals(CallPeerState.CONNECTED))
                    {
                        // The emergency call connected successfully.  We stop listening now, as we don't want to retry
                        // the call if a failure occurs after the call has successfully connected.
                        logger.debug("Call connected successfully - remove retry listener");
                        evt.getSourceCallPeer().removeCallPeerListener(this);
                    }
                }
            };
        }

        /**
         * Determine whether this call will require media on the user's PC.
         * Typically true unless this is a CTD call.
         * @return <tt>true</tt> if this call will require media on the user's
         * PC, <tt>false</tt> otherwise.
         */
        private boolean callWillRequireLocalMedia()
        {
            boolean ret;

            OperationSetResourceAwareTelephony resourceTelephony
                = protocolProvider.getOperationSet(
                        OperationSetResourceAwareTelephony.class);

            if (resourceTelephony != null)
            {
                logger.debug("ResourceAwareTelephony (jabber) will require media");
                ret = true;
            }
            else
            {
                OperationSetBasicTelephony<?> telephony
                    = protocolProvider.getOperationSet(
                            OperationSetBasicTelephony.class);

                if (telephony != null)
                {
                    ret = telephony.requiresLocalMedia();
                    logger.debug("Basic telephony - requires media? " + ret);
                }
                else
                {
                    logger.warn("No telephony op set for this call!");
                    ret = false;
                }
            }

            return ret;
        }
    }

    /**
     * Creates a video call through the given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> through
     * which to make the call
     * @param contact the <tt>Contact</tt> to call
     * @param stringContact the contact string to call
     *
     * @throws OperationFailedException thrown if the call operation fails
     * @throws ParseException thrown if the contact string is malformated
     */
    private static void callVideo(  ProtocolProviderService protocolProvider,
                                    Contact contact,
                                    String stringContact)
        throws  OperationFailedException,
                ParseException
    {
        OperationSetVideoTelephony telephony
            = protocolProvider.getOperationSet(
                    OperationSetVideoTelephony.class);

        if (telephony != null)
        {
            if (contact != null)
            {
                telephony.createVideoCall(contact);
            }
            else if (stringContact != null)
                telephony.createVideoCall(stringContact);
        }
    }

    /**
     * Whether video calling is disabled for multi-party calls
     *
     * @return whether multi party video is disabled
     */
    public static boolean isMultiPartyVideoDisabled()
    {
        return GuiActivator.getConfigurationService().user().getBoolean(
                                                multiPartyVideoDisabledProperty,
                                                false);
    }

    /**
     * Creates a call through the given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> through
     * which to make the call
     * @param contact the <tt>Contact</tt> to call
     * @param stringContact the contact string to call
     * @param displayName the display name of the contact to call (optional)
     * @param contactResource the specific <tt>ContactResource</tt> to call
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     *
     * @throws OperationFailedException thrown if the call operation fails
     * @throws ParseException thrown if the contact string is malformated
     */
    private static Call call(ProtocolProviderService protocolProvider,
                             Contact contact,
                             String stringContact,
                             String displayName,
                             ContactResource contactResource,
                             EmergencyCallContext emergency,
                             Reformatting reformatting)
        throws  OperationFailedException,
                ParseException
    {
        OperationSetBasicTelephony<?> telephony
            = protocolProvider.getOperationSet(
                    OperationSetBasicTelephony.class);

        OperationSetResourceAwareTelephony resourceTelephony
            = protocolProvider.getOperationSet(
                    OperationSetResourceAwareTelephony.class);

        Call call = null;

        // Under DUIR-4210, we detect External Line Codes (ELCs) added to numbers of
        // saved contacts. The existing code does not currently handle ELCs that
        // start with a hash "#" or an asterisk "*".
        //
        // A check needs to be made whether the number needs to be formatted
        // as phone numbers dialed through the keypad should be dialed as is.
        // This covers a special case with Italian numbers where fixed lines
        // can have variable lengths and this might coincide with a mobile
        // number with ELC of 0.
        if (reformatting == Reformatting.NEEDED)
        {
            if (contact != null)
            {
                stringContact = contact.getAddress();
            }
            stringContact = phoneNumberUtils.formatNumberForDialing(stringContact);
        }

        if (resourceTelephony != null && contactResource != null)
        {
            if (!StringUtils.isNullOrEmpty(stringContact))
            {
                logger.info("Calling number:" + stringContact);
                call = resourceTelephony.createCall(stringContact,
                                                    contactResource.getResourceName(),
                                                    null,
                                                    emergency);
            }
        }
        else if (telephony != null)
        {
            if (!StringUtils.isNullOrEmpty(stringContact))
            {
                logger.info("Calling number:" + logHasher(stringContact));
                call = telephony.createCall(stringContact,
                                            displayName,
                                            null,
                                            emergency);
            }
        }

        return call;
    }

    /**
     * Answers to all <tt>CallPeer</tt>s associated with a specific
     * <tt>Call</tt> and, optionally, does that in a telephony conference with
     * an existing <tt>Call</tt>.
     */
    private static class AnswerCallThread
        extends Thread
    {
        /**
         * The <tt>Call</tt> which is to be answered.
         */
        private final Call call;

        /**
         * The existing <tt>Call</tt>, if any, which represents a telephony
         * conference in which {@link #call} is to be answered.
         */
        private final Call existingCall;

        /**
         * The indicator which determines whether this instance is to answer
         * {@link #call} with video.
         */
        private final boolean video;

        public AnswerCallThread(Call call, Call existingCall, boolean video)
        {
            this.call = call;
            this.existingCall = existingCall;
            this.video = video;
        }

        @Override
        public void run()
        {
            if (existingCall != null)
                call.setConference(existingCall.getConference());

            ProtocolProviderService pps = call.getProtocolProvider();
            Iterator<? extends CallPeer> peers = call.getCallPeers();

            while (peers.hasNext())
            {
                CallPeer peer = peers.next();

                if (video)
                {
                    OperationSetVideoTelephony telephony
                        = pps.getOperationSet(OperationSetVideoTelephony.class);

                    try
                    {
                        telephony.answerVideoCallPeer(peer);
                    }
                    catch (OperationFailedException ofe)
                    {
                        logger.error(
                                "Could not answer "
                                    + peer
                                    + " with video"
                                    + " because of the following exception: "
                                    + ofe);
                    }
                }
                else
                {
                    OperationSetBasicTelephony<?> telephony
                        = pps.getOperationSet(OperationSetBasicTelephony.class);

                    try
                    {
                        telephony.answerCallPeer(peer);
                    }
                    catch (OperationFailedException ofe)
                    {
                        logger.error(
                                "Could not answer "
                                    + peer
                                    + " because of the following exception: ",
                                ofe);
                    }
                }
            }
        }
    }

    /**
     * Invites a list of callees to a conference <tt>Call</tt>. If the specified
     * <tt>Call</tt> is <tt>null</tt>, creates a brand new telephony conference.
     */
    private static class InviteToConferenceCallThread
        extends Thread
    {
        /**
         * The addresses of the callees to be invited into the telephony
         * conference to be organized by this instance. For further details,
         * refer to the documentation on the <tt>callees</tt> parameter of the
         * respective <tt>InviteToConferenceCallThread</tt> constructor.
         */
        private final Map<ProtocolProviderService, List<String>>
            callees;

        /*
         * Whether callee numbers need reformatting.
         */
        private final Reformatting mReformattingNeeded;

        /**
         * The <tt>Call</tt>, if any, into the telephony conference of which
         * {@link #callees} are to be invited. If non-<tt>null</tt>, its
         * <tt>CallConference</tt> state will be shared with all <tt>Call</tt>s
         * established by this instance for the purposes of having the
         * <tt>callees</tt> into the same telephony conference.
         */
        private final Call call;

        /**
         * Initializes a new <tt>InviteToConferenceCallThread</tt> instance
         * which is to invite a list of callees to a conference <tt>Call</tt>.
         * If the specified <tt>call</tt> is <tt>null</tt>, creates a brand new
         * telephony conference.
         *
         * @param callees the addresses of the callees to be invited into a
         * telephony conference. The addresses are provided in multiple
         * <tt>List&lt;String&gt;</tt>s. Each such list of addresses is mapped
         * by the <tt>ProtocolProviderService</tt> through which they are to be
         * invited into the telephony conference. If there are multiple
         * <tt>ProtocolProviderService</tt>s in the specified <tt>Map</tt>, the
         * resulting telephony conference is known by the name
         * &quot;cross-protocol&quot;. It is also allowed to have a list of
         * addresses mapped to <tt>null</tt> which means that the new instance
         * will automatically choose a <tt>ProtocolProviderService</tt> to
         * invite the respective callees into the telephony conference.
         * @param call the <tt>Call</tt> to invite the specified
         * <tt>callees</tt> into. If <tt>null</tt>, this instance will create a
         * brand new telephony conference. Technically, a <tt>Call</tt> instance
         * is protocol/account-specific and it is possible to have
         * cross-protocol/account telephony conferences. That's why the
         * specified <tt>callees</tt> are invited into one and the same
         * <tt>CallConference</tt>: the one in which the specified <tt>call</tt>
         * is participating or a new one if <tt>call</tt> is <tt>null</tt>. Of
         * course, an attempt is made to have all callees from one and the same
         * protocol/account into one <tt>Call</tt> instance.
         */
        public InviteToConferenceCallThread(
                Map<ProtocolProviderService, List<String>> callees,
                Reformatting reformattingNeeded,
                Call call)
        {
            this.callees = callees;
            this.call = call;
            this.mReformattingNeeded = reformattingNeeded;
        }

        /**
         * Invites {@link #callees} into a telephony conference which is
         * optionally specified by {@link #call}.
         */
        @Override
        public void run()
        {
            CallConference conference
                = (call == null) ? null : call.getConference();

            for(Map.Entry<ProtocolProviderService, List<String>> entry
                    : callees.entrySet())
            {
                ProtocolProviderService pps = entry.getKey();

                /*
                 * We'd like to allow specifying callees without specifying an
                 * associated ProtocolProviderService.
                 */
                if (pps != null)
                {
                    OperationSetBasicTelephony<?> basicTelephony
                        = pps.getOperationSet(OperationSetBasicTelephony.class);

                    if(basicTelephony == null)
                        continue;
                }

                List<String> contactList = entry.getValue();
                String[] contactArray
                    = contactList.toArray(new String[contactList.size()]);
                var loggableContactArray = Arrays.asList(contactArray)
                                           .stream()
                                           .map(obj -> logHasher(obj))
                                           .collect(Collectors.toList());

                logger.info("Contacts in " + loggableContactArray);

                // Cleans up the numbers we are calling (analogous to same
                // call when you make a regular call).
                cleanupPhoneNumbers(contactArray);

                // Reformat the numbers if necessary.
                if (mReformattingNeeded == Reformatting.NEEDED)
                {
                    reformatPhoneNumbers(contactArray);
                }

                /* Try to have a single Call per ProtocolProviderService. */
                Call ppsCall;

                if ((call != null) && call.getProtocolProvider().equals(pps))
                    ppsCall = call;
                else
                {
                    ppsCall = null;
                    if (conference != null)
                    {
                        List<Call> conferenceCalls = conference.getCalls();

                        if (pps == null)
                        {
                            /*
                             * We'd like to allow specifying callees without
                             * specifying an associated ProtocolProviderService.
                             * The simplest approach is to just choose the first
                             * ProtocolProviderService involved in the telephony
                             * conference.
                             */
                            if (call == null)
                            {
                                if (!conferenceCalls.isEmpty())
                                {
                                    ppsCall = conferenceCalls.get(0);
                                    pps = ppsCall.getProtocolProvider();
                                }
                            }
                            else
                            {
                                ppsCall = call;
                                pps = ppsCall.getProtocolProvider();
                            }
                        }
                        else
                        {
                            for (Call conferenceCall : conferenceCalls)
                            {
                                if (pps.equals(
                                        conferenceCall.getProtocolProvider()))
                                {
                                    ppsCall = conferenceCall;
                                    break;
                                }
                            }
                        }
                    }
                }

                OperationSetTelephonyConferencing telephonyConferencing
                    = pps.getOperationSet(
                            OperationSetTelephonyConferencing.class);

                try
                {
                    if (ppsCall == null)
                    {
                        ppsCall
                            = telephonyConferencing.createConfCall(
                                    contactArray,
                                    conference);
                        if (conference == null)
                            conference = ppsCall.getConference();
                    }
                    else
                    {
                        for (String contact : contactArray)
                        {
                            logger.info("Inviting: " + logHasher(contact));
                            telephonyConferencing.inviteCalleeToCall(
                                    contact,
                                    ppsCall);
                        }
                    }
                }
                catch(Exception e)
                {
                    logger.error(
                            "Failed to invite callees: "
                                + Arrays.toString(contactArray),
                            e);
                    new ErrorDialog(
                        resources.getI18NString("service.gui.ERROR"),
                        e.getMessage()).showDialog();
                }
            }
        }
    }

    /**
     * Hangs up a specific <tt>Call</tt> (i.e. all <tt>CallPeer</tt>s associated
     * with a <tt>Call</tt>), <tt>CallConference</tt> (i.e. all <tt>Call</tt>s
     * participating in a <tt>CallConference</tt>), or <tt>CallPeer</tt>.
     */
    private static class HangupCallThread
        extends Thread
    {
        private Call call;
        private final CallConference conference;
        private final CallPeer peer;

        /**
         * Initializes a new <tt>HangupCallThread</tt> instance which is to hang
         * up a specific <tt>Call</tt> i.e. all <tt>CallPeer</tt>s associated
         * with the <tt>Call</tt>.
         *
         * @param call the <tt>Call</tt> whose associated <tt>CallPeer</tt>s are
         * to be hanged up
         */
        public HangupCallThread(Call call)
        {
            this(call, null, null);
        }

        /**
         * Initializes a new <tt>HangupCallThread</tt> instance which is to hang
         * up a specific <tt>CallConference</tt> i.e. all <tt>Call</tt>s
         * participating in the <tt>CallConference</tt>.
         *
         * @param conference the <tt>CallConference</tt> whose participating
         * <tt>Call</tt>s re to be hanged up
         */
        public HangupCallThread(CallConference conference)
        {
            this(null, conference, null);
        }

        /**
         * Initializes a new <tt>HangupCallThread</tt> instance which is to hang
         * up a specific <tt>CallPeer</tt>.
         *
         * @param peer the <tt>CallPeer</tt> to hang up
         */
        public HangupCallThread(CallPeer peer)
        {
            this(null, null, peer);
        }

        /**
         * Initializes a new <tt>HangupCallThread</tt> instance which is to hang
         * up a specific <tt>Call</tt>, <tt>CallConference</tt>, or
         * <tt>CallPeer</tt>.
         *
         * @param call the <tt>Call</tt> whose associated <tt>CallPeer</tt>s are
         * to be hanged up
         * @param conference the <tt>CallConference</tt> whose participating
         * <tt>Call</tt>s re to be hanged up
         * @param peer the <tt>CallPeer</tt> to hang up
         */
        private HangupCallThread(
                Call call,
                CallConference conference,
                CallPeer peer)
        {
            super("HangupCall " + (peer == null ? "" : peer.getAddress()));
            this.call = call;
            this.conference = conference;
            this.peer = peer;
        }

        @Override
        public void run()
        {
            logger.info("Run HangupCallThread");
            /*
             * There is only an OperationSet which hangs up a CallPeer at a time
             * so prepare a list of all CallPeers to be hanged up.
             */
            Set<CallPeer> peers = new HashSet<>();

            if (call != null)
            {
                Iterator<? extends CallPeer> peerIter = call.getCallPeers();

                while (peerIter.hasNext())
                    peers.add(peerIter.next());
            }

            if (conference != null)
                peers.addAll(conference.getCallPeers());

            boolean enableVideoForCall = false;

            if (peer != null)
            {
                // Remove a single peer from a conference call
                peers.add(peer);

                // If this peer was part of a conference and their removal
                // causes the call to become a two-party call, then we have to
                // re-enable video telephony.
                if (peer.getCall().getConference().getCallPeerCount() == 2)
                {
                    enableVideoForCall = true;
                    call = peer.getCall();
                }
            }

            for (CallPeer peer : peers)
            {
                OperationSetBasicTelephony<?> basicTelephony
                    = peer.getProtocolProvider().getOperationSet(
                            OperationSetBasicTelephony.class);

                // basicTelephony could be null if we never added it as an OpSet to
                // ProtocolProviderServiceSipImpl because the client is CTD only. Usually we
                // wouldn't come through here for a CTD call, but we do if the SIP connection
                // comes down whilst a CTD call is ongoing.
                if (basicTelephony != null)
                {
                    try
                    {
                        // Synchronize on the media handler to prevent deadlocks.
                        synchronized (((MediaAwareCallPeer<?,?,?>)peer).getMediaHandler())
                        {
                            basicTelephony.hangupCallPeer(peer);
                        }
                    }
                    catch (OperationFailedException ofe)
                    {
                        logger.error("Could not hang up: " + peer, ofe);
                    }
                }
            }

            // We have finished hanging up all peers, check if we need to
            // re-enable video telephony.
            if (enableVideoForCall)
            {
                OperationSetVideoTelephony videoTelephony =
                    call.getProtocolProvider().
                    getOperationSet(OperationSetVideoTelephony.class);

                try
                {
                    if (videoTelephony != null && isMultiPartyVideoDisabled())
                    {
                        videoTelephony.setRemoteVideoAllowed(call,
                                                             true);
                    }
                }
                catch (OperationFailedException e)
                {
                    logger.error("Could not re-enable video telephony for: " +
                                             call.getCallPeers());
                }
            }
        }
    }

    /**
     * Creates the enable local video call thread.
     */
    private static class EnableLocalVideoThread
        extends Thread
    {
        private final Call call;

        private final boolean enable;

        /**
         * Creates the enable local video call thread.
         *
         * @param call the call, for which to enable/disable
         * @param enable
         */
        public EnableLocalVideoThread(Call call, boolean enable)
        {
            this.call = call;
            this.enable = enable;
        }

        @Override
        public void run()
        {
            OperationSetVideoTelephony telephony
                = call.getProtocolProvider()
                    .getOperationSet(OperationSetVideoTelephony.class);
            boolean enableSucceeded = false;

            if (telephony != null)
            {
                try
                {
                    telephony.setLocalVideoAllowed(call, enable);
                    enableSucceeded = true;
                }
                catch (OperationFailedException ex)
                {
                    logger.error(
                        "Failed to toggle the streaming of local video.",
                        ex);
                }
            }

            // If the operation didn't succeeded for some reason, make sure the
            // video button doesn't remain selected.
            if (enable && !enableSucceeded)
            {
                getActiveCallContainer(call).setVideoButtonSelected(false);
            }
        }
    }

    /**
     * Puts on hold the given <tt>CallPeer</tt>.
     */
    private static class PutOnHoldCallPeerThread
        extends Thread
    {
        private final CallPeer callPeer;

        private final boolean isOnHold;

        public PutOnHoldCallPeerThread(CallPeer callPeer, boolean isOnHold)
        {
            this.callPeer = callPeer;
            this.isOnHold = isOnHold;
        }

        @Override
        public void run()
        {
            logger.info("Put " + callPeer + (isOnHold ? " on hold." : " off hold."));

            OperationSetBasicTelephony<?> telephony =
                callPeer.getProtocolProvider().getOperationSet(OperationSetBasicTelephony.class);

            try
            {
                if (isOnHold)
                {
                    telephony.putOnHold(callPeer);
                }
                else
                {
                    telephony.putOffHold(callPeer);
                }
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to put" + callPeer.getAddress() + (isOnHold ? " on hold." : " off hold."), ex);
            }
        }
    }

    /**
     * Merges specific existing <tt>Call</tt>s into a specific telephony
     * conference.
     */
    private static class MergeExistingCalls
        extends Thread
    {
        /**
         * The telephony conference in which {@link #calls} are to be merged.
         */
        private final CallConference conference;

        /**
         * Second call.
         */
        private final Collection<Call> calls;

        /**
         * Initializes a new <tt>MergeExistingCalls</tt> instance which is to
         * merge specific existing <tt>Call</tt>s into a specific telephony
         * conference.
         *
         * @param conference the telephony conference in which the specified
         * <tt>Call</tt>s are to be merged
         * @param calls the <tt>Call</tt>s to be merged into the specified
         * telephony conference
         */
        public MergeExistingCalls(
                CallConference conference,
                Collection<Call> calls)
        {
            this.conference = conference;
            this.calls = calls;
        }

        /**
         * Puts off hold the <tt>CallPeer</tt>s of a specific <tt>Call</tt>
         * which are locally on hold.
         *
         * @param call the <tt>Call</tt> which is to have its <tt>CallPeer</tt>s
         * put off hold
         */
        private void putOffHold(Call call)
        {
            Iterator<? extends CallPeer> peers = call.getCallPeers();
            OperationSetBasicTelephony<?> telephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            while (peers.hasNext())
            {
                CallPeer callPeer = peers.next();
                boolean putOffHold = true;

                if(callPeer instanceof MediaAwareCallPeer)
                {
                    putOffHold
                        = ((MediaAwareCallPeer<?,?,?>) callPeer)
                            .getMediaHandler()
                                .isLocallyOnHold();
                }
                if(putOffHold)
                {
                    try
                    {
                        telephony.putOffHold(callPeer);
                        Thread.sleep(400);
                    }
                    catch(Exception ofe)
                    {
                        logger.error("Failed to put off hold.", ofe);
                    }
                }
            }
        }

        @Override
        public void run()
        {
            // conference
            for (Call call : conference.getCalls())
                putOffHold(call);

            // calls
            if (!calls.isEmpty())
            {
                for(Call call : calls)
                {
                    if (conference.containsCall(call))
                        continue;

                    // Now add the call peer to the 'conference'
                    if (call.getCallPeers().hasNext())
                    {
                        putOffHold(call);

                        // At this point 'conference' is the call we want to
                        // merge in to and 'call' is the single call we want
                        // to merge in.

                        // First close the call we no longer want
                        callFrames.remove(call.getConference()).dispose();

                        CallPeer peerToMerge = call.getCallPeers().next();
                        call.setConference(conference);

                        callFrames.get(conference).addPeer(peerToMerge);
                    }
                }
            }
        }
    }

    /**
     * Cleans up the phone numbers (if any) in a list of <tt>String</tt>
     * contact addresses or phone numbers.  Removes brackets and stuff like that.
     *
     * @param callees the list of contact addresses or phone numbers to be
     * cleaned up
     */
    private static void cleanupPhoneNumbers(String callees[])
    {
        for (int ii=0; ii<callees.length; ii++)
        {
             callees[ii] = callees[ii].replaceAll(PHONE_NUMBER_IGNORE_REGEX, "");
        }
    }

    /**
     * Reformats the phone numbers (if any) in a list of <tt>String</tt>
     * contact addresses or phone numbers.  Adds an ELC, or converts to E.164
     * if need be.
     *
     * @param callees the list of phone numbers to be reformatted
     */
    private static void reformatPhoneNumbers(String callees[])
    {
        for (int ii=0; ii<callees.length; ii++)
        {
            callees[ii] = phoneNumberUtils.formatNumberForDialing(callees[ii]);
        }
    }

    /**
     * Throws a <tt>RuntimeException</tt> if the current thread is not the AWT
     * event dispatching thread.
     */
    public static void assertIsEventDispatchingThread()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            throw new RuntimeException(
                    "The method can be called only on the AWT event dispatching"
                        + " thread.");
        }
    }

    /**
     * Finds the <tt>CallFrame</tt>, if any, which depicts a specific
     * <tt>CallConference</tt>.
     * <p>
     * <b>Note</b>: The method can be called only on the AWT event dispatching
     * thread.
     * </p>
     *
     * @param conference the <tt>CallConference</tt> to find the depicting
     * <tt>CallFrame</tt> of
     * @return the <tt>CallFrame</tt> which depicts the specified
     * <tt>CallConference</tt> if such a <tt>CallFrame</tt> exists; otherwise,
     * <tt>null</tt>
     * @throws RuntimeException if the method is not called on the AWT event
     * dispatching thread
     */
    private static CallFrame findCallFrame(CallConference conference)
    {
        synchronized (callFrames)
        {
            return callFrames.get(conference);
        }
    }

    /**
     * Notifies {@link #callFrames} about a specific <tt>CallEvent</tt> received
     * by <tt>CallManager</tt> (because they may need to update their UI, for
     * example).
     * <p>
     * <b>Note</b>: The method can be called only on the AWT event dispatching
     * thread.
     * </p>
     *
     * @param ev the <tt>CallEvent</tt> received by <tt>CallManager</tt> which
     * is to be forwarded to <tt>callPanels</tt> for further
     * <tt>CallPanel</tt>-specific handling
     */
    static void forwardCallEventToCallPanels(final CallEvent ev)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            forwardCallEventToCallPanels(ev);
                        }
                    });
            return;
        }

        CallFrame[] callFrames;

        synchronized (CallManager.callFrames)
        {
            Collection<CallFrame> values = CallManager.callFrames.values();

            callFrames = values.toArray(new CallFrame[values.size()]);
        }

        for (CallFrame callFrame : callFrames)
        {
            try
            {
                callFrame.onCallEvent(ev);
            }
            catch (Exception ex)
            {
                /*
                 * There is no practical reason while the failure of a CallPanel
                 * to handle the CallEvent should cause the other CallPanels to
                 * be left out-of-date.
                 */
                logger.error("A CallPanel failed to handle a CallEvent", ex);
            }
        }
    }

    /**
     * Creates a call for the supplied operation set.
     * @param opSetClass the operation set to use to create call.
     * @param protocolProviderService the protocol provider
     * @param contact the contact associated with this call
     * @param contactDisplayName optional - the user-visible name of the
     * contact.
     * @param strContact the contact address to call
     */
    static void createCall(Class<? extends OperationSet> opSetClass,
                           ProtocolProviderService protocolProviderService,
                           Contact contact,
                           String contactDisplayName,
                           String strContact,
                           Reformatting reformatNeeded)
    {
        if (opSetClass.equals(OperationSetBasicTelephony.class))
        {
            createCall(protocolProviderService,
                       contact,
                       contactDisplayName,
                       strContact,
                       reformatNeeded);
        }
        else if (opSetClass.equals(OperationSetVideoTelephony.class))
        {
            createVideoCall(protocolProviderService,
                            contact,
                            contactDisplayName,
                            strContact,
                            reformatNeeded);
        }
    }

    /**
     * Creates a call for the default contact of the metacontact.
     *
     * This method will reformat the number.
     *
     * @param metaContact the metacontact that will be called.
     * @param stringContact the phone number to call.
     * @param isVideo if <tt>true</tt> will create video call.
     */
    public static void call(MetaContact metaContact,
                            String stringContact,
                            boolean isVideo)
    {
        Contact contact = metaContact
            .getDefaultContact(getOperationSetForCall(isVideo));

        logger.info("Calling " + stringContact);

        if (StringUtils.isNullOrEmpty(stringContact))
        {
            call(contact, isVideo);
        }
        else
        {
            String address = stringContact;

            ProtocolProviderService protocolProvider;
            List<ProtocolProviderService> providers =
                    CallManager.getAllTelephonyProviders(stringContact);

            if (contact == null && providers.size() > 1)
            {
                // No contact and multiple providers, ask the user to choose one
                logger.info("Calling " + address);
                new ChooseCallAccountDialog(
                                    address,
                                    null,
                                    getOperationSetForCall(isVideo),
                                    providers,
                                    Reformatting.NEEDED).setVisible(true);
            }
            else
            {
                // If we have a contact, use that provider, otherwise use the
                // first provider
                if (contact == null)
                {
                    protocolProvider = providers.get(0);
                    contact = metaContact.getDefaultContact();
                }
                else
                {
                    protocolProvider = contact.getProtocolProvider();
                }

                if (protocolProvider.isRegistered())
                {
                    new CreateCallThread(protocolProvider,
                                         contact,
                                         null,
                                         address,
                                         metaContact.getDisplayName(),
                                         isVideo,
                                         Reformatting.NEEDED).start();
                }
                else
                {
                    // Not online
                    logger.info("Tried to make call to offline provider " +
                                                              protocolProvider);
                    showNotOnlineWarning();
                }
            }
        }
    }

    /**
     * A particular contact has been selected no options to select
     * will just call it.
     *
     * This method will not reformat the number.
     *
     * @param contact the contact to call
     * @param contactResource the specific contact resource
     * @param isVideo is video enabled
     */
    public static void call(Contact contact,
                            ContactResource contactResource,
                            boolean isVideo)
    {
        new CreateCallThread(
                contact.getProtocolProvider(),
                contact,
                contactResource,
                null,
                null,
                isVideo,
                Reformatting.NOT_NEEDED).start();
    }

    /**
     * A particular contact has been selected no options to select
     * will just call it.
     *
     * This method will not reformat the number (in the contact).
     *
     * @param contact the contact to call
     * @param isVideo is video enabled
     */
    public static void call(Contact contact, boolean isVideo)
    {
        new CreateCallThread(contact.getProtocolProvider(),
                             contact,
                             null,
                             null,
                             null,
                             isVideo,
                             Reformatting.NOT_NEEDED).start();
    }

    /**
     * Calls a phone showing a dialog to choose a provider.
     * @param phone phone to call.
     *
     * This method will reformat the number in the phone number.
     *
     * @param isVideo if <tt>true</tt> will create video call.
     */
    public static void call(final String phone, boolean isVideo)
    {
        List<ProtocolProviderService> providers =
            CallManager.getAllTelephonyProviders(phone);

        if(providers.size() > 1)
        {
            ChooseCallAccountDialog chooseAccount =
                new ChooseCallAccountDialog(
                    phone,
                    null,
                    getOperationSetForCall(isVideo),
                    providers,
                    Reformatting.NEEDED)
            {
                    @Override
                    public void callButtonPressed()
                    {
                        super.callButtonPressed();
                    }
            };
            chooseAccount.setVisible(true);
        }
        else if (providers.get(0).isRegistered())
        {
            CallManager.createCall(providers.get(0),
                                   phone,
                                   Reformatting.NEEDED);
        }
        else
        {
            // Selected a not online provider
            logger.info("Tried to make call to offline provider");
            showNotOnlineWarning();
        }
    }

    /**
     * Call any of the supplied details. The number is reformatted to E.164.
     *
     * @param contactPhoneUtil the util (metacontact) to check what is enabled,
     *                         available.
     * @param uiContactDetailList the list with details to choose for calling
     * @param contactDisplayName the name of the contact to call. If null,
     * this defaults to a best guess based on the contact details given.
     * @param isVideo if <tt>true</tt> will create video call.
     * @param invoker the invoker component
     * @param location the location where this was invoked.
     */
    public static void call(ContactPhoneUtil contactPhoneUtil,
                            List<UIContactDetail> uiContactDetailList,
                            String contactDisplayName,
                            boolean isVideo,
                            JComponent invoker,
                            Point location)
    {
        Class<? extends OperationSet> opsetClass =
            getOperationSetForCall(isVideo);

        Contact callContact = null;

        if(contactPhoneUtil != null)
        {
            boolean addAdditionalNumbers = false;

            if (contactPhoneUtil.getMetaContact() != null)
            {
                callContact = contactPhoneUtil.getMetaContact().
                                                           getDefaultContact();
            }

            if(!isVideo)
            {
                addAdditionalNumbers = true;
            }
            else
            {
                if(isVideo)
                {
                    // lets check is video enabled in additional numbers
                    addAdditionalNumbers = contactPhoneUtil.isVideoCallEnabled();
                }
            }

            if(addAdditionalNumbers)
            {
                uiContactDetailList.addAll(
                    contactPhoneUtil.getAdditionalNumbers());
            }
        }

        if (uiContactDetailList.size() == 1)
        {
            UIContactDetail detail = uiContactDetailList.get(0);
            String address = detail.getAddress();

            logger.debug("Got address " + address);

            List<ProtocolProviderService> providers = getAllTelephonyProviders(address);

            // If our call didn't succeed, try to call through one of the other
            // protocol providers obtained above.
            if (providers != null)
            {
                int providersCount = providers.size();

                if (providersCount <= 0)
                {
                    new ErrorDialog(
                        resources.getI18NString("service.gui.CALL_FAILED"),
                        resources.getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                        .showDialog();
                }
                else if (providersCount == 1)
                {
                    ProtocolProviderService provider = providers.get(0);

                    if (provider.isRegistered())
                    {
                        createCall(opsetClass,
                                   provider,
                                   callContact,
                                   contactDisplayName,
                                   address,
                                   Reformatting.NEEDED);
                    }
                    else
                    {
                        logger.info("An account is not online " + provider);
                        showNotOnlineWarning();
                    }
                }
                else if (providersCount > 1)
                {
                    new ChooseCallAccountDialog(address,
                                                contactDisplayName,
                                                opsetClass,
                                                providers,
                                                Reformatting.NEEDED).setVisible(true);
                }
            }
        }
        else if (uiContactDetailList.size() > 1)
        {
            ChooseCallAccountPopupMenu chooseCallAccountPopupMenu =
                new ChooseCallAccountPopupMenu(invoker,
                                                 uiContactDetailList,
                                                 opsetClass,
                                                 callContact,
                                                 contactDisplayName);
            chooseCallAccountPopupMenu.showPopupMenu(location.x, location.y);
        }
    }

    /**
     * Call the ui contact. The number is reformatted to E.164.
     *
     * @param uiContact the contact to call.
     * @param isVideo if <tt>true</tt> will create video call.
     * @param invoker the invoker component
     * @param location the location where this was invoked.
     */
    public static void call(UIContact uiContact,
                            boolean isVideo,
                            JComponent invoker,
                            Point location)
    {
        ContactPhoneUtil contactPhoneUtil = null;
        if(uiContact.getDescriptor() instanceof MetaContact)
        {
            contactPhoneUtil = ContactPhoneUtil.getPhoneUtil(
                (MetaContact)uiContact.getDescriptor());
        }

        List<UIContactDetail> telephonyContacts
            = uiContact.getContactDetailsForOperationSet(
                getOperationSetForCall(isVideo));

        call(contactPhoneUtil,
             telephonyContacts,
             uiContact.getDisplayName(),
             isVideo,
             invoker,
             location);
    }

    /**
     * Obtain operation set checking the params.
     * @param isVideo if <tt>true</tt> use OperationSetVideoTelephony.
     * @return the operation set, default is OperationSetBasicTelephony.
     */
    public static Class<? extends OperationSet> getOperationSetForCall(
        boolean isVideo)
    {
        return isVideo ? OperationSetVideoTelephony.class :
                         OperationSetBasicTelephony.class;
    }

    /**
     * Called when a call frame is disposed. This method is a safety against
     * call frames being disposed of but the CallManager map not being updated.
     */
    public static void callFrameDisposed(CallConference callConference)
    {
        synchronized (callFrames)
        {
            callFrames.remove(callConference);
        }
    }

    /**
     * Exposes CallManager's internal CallFrames object
     * Only to be used in unit testing.
     */
    @VisibleForTesting
    static Map<CallConference, CallFrame> getCallFrames()
    {
        return callFrames;
    }
}
