/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import java.text.*;
import java.util.*;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.message.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;

import com.google.common.annotations.VisibleForTesting;

import gov.nist.javax.sip.stack.*;
import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.emergencylocation.EmergencyCallContext;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP implementation of the abstract <tt>Call</tt> class encapsulating SIP
 * dialogs.
 *
 * @author Emil Ivov
 * @author Hristo Terezov
 */
public class CallSipImpl
    extends MediaAwareCall<CallPeerSipImpl,
                           OperationSetBasicTelephonySipImpl,
                           ProtocolProviderServiceSipImpl>
    implements CallPeerListener, RegistrationStateChangeListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger.getLogger(CallSipImpl.class);

    /**
     * When starting call we may have quality preferences we must use
     * for the call.
     */
    private QualityPreset initialQualityPreferences;

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * The name of the property under which the user may specify the number of
     * milliseconds for the initial interval for retransmissions of response
     * 180.
     */
    private static final String RETRANSMITS_RINGING_INTERVAL
        = "net.java.sip.communicator.impl.protocol.sip"
                + ".RETRANSMITS_RINGING_INTERVAL";

    /**
    * The default amount of time (in milliseconds) for the initial interval for
    *  retransmissions of response 180.
    */
    private static final int DEFAULT_RETRANSMITS_RINGING_INTERVAL = 500;

    /**
     * The name of the property under which the user may specify the number of
     * retransmissions of response 180.
     */
    private static final String NUM_RETRANSMITS_RINGING
        = "net.java.sip.communicator.impl.protocol.sip.NUM_RETRANSMITS_RINGING";

    /**
     * Default maximum number of retransmissions that will be sent.
     */
    private static final int DEFAULT_NUM_RETRANSMITS_RINGING = 3;

    /**
    * The amount of time (in milliseconds) for the initial interval for
    * retransmissions of response 180.
    */
    private int retransmitsRingingInterval
        = DEFAULT_RETRANSMITS_RINGING_INTERVAL;

    /**
     * The number of retransmissions of response 180.
     */
     private int numRetransmitsRinging = DEFAULT_NUM_RETRANSMITS_RINGING;

     /**
      * The configuration service
      */
     private static final ConfigurationService configService =
                                          SipActivator.getConfigurationService();

    /**
     * Crates a CallSipImpl instance belonging to <tt>sourceProvider</tt> and
     * initiated by <tt>CallCreator</tt>.
     *
     * @param parentOpSet a reference to the operation set that's creating us
     * and that we would be able to use for even dispatching.
     */
    @VisibleForTesting
    public CallSipImpl(OperationSetBasicTelephonySipImpl parentOpSet)
    {
        super(parentOpSet);

        this.messageFactory = getProtocolProvider().getMessageFactory();

        getProtocolProvider().addRegistrationStateChangeListener(this);

        //let's add ourselves to the calls repo. we are doing it ourselves just
        //to make sure that no one ever forgets.
        parentOpSet.getActiveCallsRepository().addCall(this);

        retransmitsRingingInterval = configService.global().getInt(
            RETRANSMITS_RINGING_INTERVAL, DEFAULT_RETRANSMITS_RINGING_INTERVAL);
        numRetransmitsRinging = configService.global().getInt(
            NUM_RETRANSMITS_RINGING, DEFAULT_NUM_RETRANSMITS_RINGING);
    }

    /**
     * Returns <tt>true</tt> if <tt>dialog</tt> matches the jain sip dialog
     * established with one of the peers in this call.
     *
     * @param dialog the dialog whose corresponding peer we're looking for.
     *
     * @return true if this call contains a call peer whose jain sip
     * dialog is the same as the specified and false otherwise.
     */
    public boolean contains(Dialog dialog)
    {
        return findCallPeer(dialog) != null;
    }

    /**
     * Creates a new call peer associated with <tt>containingTransaction</tt>
     *
     * @param containingTransaction the transaction that created the call peer.
     * @param sourceProvider the provider that the containingTransaction belongs
     * to.
     * @param isAutoAnswered if true, this call is being auto answered,
     * otherwise the user will be prompted to respond to the call.
     * @return a new instance of a <tt>CallPeerSipImpl</tt> corresponding
     * to the <tt>containingTransaction</tt>.
     */
    private CallPeerSipImpl createCallPeerFor(
            Transaction containingTransaction,
            SipProvider sourceProvider,
            boolean isAutoAnswered)
    {
        CallPeerSipImpl callPeer
            = new CallPeerSipImpl(
                    containingTransaction.getDialog().getRemoteParty(),
                    this,
                    containingTransaction,
                    sourceProvider);

        addCallPeer(callPeer);

        boolean incomingCall
            = (containingTransaction instanceof ServerTransaction);

        callPeer.setState(
                incomingCall
                    ? CallPeerState.INCOMING_CALL
                    : CallPeerState.INITIATING_CALL);

        // if this was the first peer we added in this call then the call is
        // new and we also need to notify everyone of its creation.
        if(getCallPeerCount() == 1)
        {
            Map<MediaType, MediaDirection> mediaDirections
                = new HashMap<>();

            mediaDirections.put(MediaType.AUDIO, MediaDirection.INACTIVE);
            mediaDirections.put(MediaType.VIDEO, MediaDirection.INACTIVE);

            boolean hasZrtp = false;
            boolean hasSdes = false;

            //this check is not mandatory catch all to skip if a problem exists
            try
            {
                // lets check the supported media types.
                // for this call
                Request inviteReq = containingTransaction.getRequest();

                if(inviteReq != null && inviteReq.getRawContent() != null)
                {
                    String sdpStr = SdpUtils.getContentAsString(inviteReq);
                    SessionDescription sesDescr
                        = SdpUtils.parseSdpString(sdpStr);
                    List<MediaDescription> remoteDescriptions
                        = SdpUtils.extractMediaDescriptions(sesDescr);

                    for (MediaDescription mediaDescription : remoteDescriptions)
                    {
                        MediaType mediaType
                            = SdpUtils.getMediaType(mediaDescription);

                        mediaDirections.put(
                                mediaType,
                                SdpUtils.getDirection(mediaDescription));

                        // hasZrtp?
                        if (!hasZrtp)
                        {
                            hasZrtp
                                = (mediaDescription.getAttribute(
                                        SdpUtils.ZRTP_HASH_ATTR)
                                    != null);
                        }
                        // hasSdes?
                        if (!hasSdes)
                        {
                            Vector<Attribute> attrs
                                = mediaDescription.getAttributes(true);

                            for (Attribute attr : attrs)
                            {
                                try
                                {
                                    if ("crypto".equals(attr.getName()))
                                    {
                                        hasSdes = true;
                                        break;
                                    }
                                }
                                catch (SdpParseException spe)
                                {
                                    logger.error(
                                            "Failed to parse SDP attribute",
                                            spe);
                                }
                            }
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                logger.warn("Error getting media types", t);
            }

            if(hasZrtp)
            {
                callPeer.getMediaHandler().addAdvertisedEncryptionMethod(
                        SrtpControlType.ZRTP);
            }
            if(hasSdes)
            {
                callPeer.getMediaHandler().addAdvertisedEncryptionMethod(
                        SrtpControlType.SDES);
            }

            if (incomingCall && ConfigurationUtils.isVoIPEnabled())
            {
                // This is an incoming call and VOIP is enabled, so do the necessary set up.
                callPeer.preAnswer();
            }

            getParentOperationSet().fireCallEvent(
                    incomingCall
                        ? CallEvent.CALL_RECEIVED
                        : CallEvent.CALL_INITIATED,
                    this,
                    mediaDirections,
                    isAutoAnswered);
        }

        return callPeer;
    }

    /**
     * Returns the call peer whose associated jain sip dialog matches
     * <tt>dialog</tt>.
     *
     * @param dialog the jain sip dialog whose corresponding peer we're looking
     * for.
     * @return the call peer whose jain sip dialog is the same as the specified
     * or null if no such call peer was found.
     */
    public CallPeerSipImpl findCallPeer(Dialog dialog)
    {
        Iterator<CallPeerSipImpl> callPeers = this.getCallPeers();

        logger.trace("Looking for peer with dialog: " + dialog
            + "among " + getCallPeerCount() + " calls");

        while (callPeers.hasNext())
        {
            CallPeerSipImpl cp = callPeers.next();

            if (cp.getDialog() == dialog)
            {
                logger.trace("Returning cp=" + cp);
                return cp;
            }
            else
            {
                logger.trace("Ignoring cp=" + cp + " because cp.dialog="
                        + cp.getDialog() + " while dialog=" + dialog);
            }
        }

        return null;
    }

    /**
     * Returns a reference to the <tt>ProtocolProviderServiceSipImpl</tt>
     * instance that created this call.
     *
     * @return a reference to the <tt>ProtocolProviderServiceSipImpl</tt>
     * instance that created this call.
     */
    @Override
    public ProtocolProviderServiceSipImpl getProtocolProvider()
    {
        return super.getProtocolProvider();
    }

    /**
     * Creates a <tt>CallPeerSipImpl</tt> from <tt>calleeAddress</tt> and sends
     * them an invite request. The invite request will be initialized according
     * to any relevant parameters in the <tt>cause</tt> message (if different
     * from <tt>null</tt>) that is the reason for creating this call.
     *
     * @param calleeAddress the party that we would like to invite to this call.
     * @param cause the message (e.g. a Refer request), that is the reason for
     * this invite or <tt>null</tt> if this is a user-initiated invitation
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     *
     * @return the newly created <tt>CallPeer</tt> corresponding to
     * <tt>calleeAddress</tt>. All following state change events will be
     * delivered through this call peer.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     *  to create the call.
     */
    public CallPeerSipImpl invite(Address calleeAddress,
                                  javax.sip.message.Message cause,
                                  EmergencyCallContext emergency)
        throws OperationFailedException
    {
        // create the invite request
        Request invite = messageFactory
            .createInviteRequest(calleeAddress, cause);

        // Transaction
        ClientTransaction inviteTransaction = null;
        SipProvider jainSipProvider
            = getProtocolProvider().getDefaultJainSipProvider();
        try
        {
            inviteTransaction = jainSipProvider.getNewClientTransaction(invite);
        }
        catch (TransactionUnavailableException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create inviteTransaction.\n"
                    + "This is most probably a network connection error.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        // create the call peer
        CallPeerSipImpl callPeer
            = createCallPeerFor(inviteTransaction, jainSipProvider, false);
        CallPeerMediaHandlerSipImpl mediaHandler = callPeer.getMediaHandler();

        /* enable video if it is a videocall */
        mediaHandler.setLocalVideoTransmissionEnabled(localVideoAllowed);

        if(initialQualityPreferences != null)
        {
            // we are in situation where we init the call and we cannot
            // determine whether the other party supports changing quality
            // so we force it
            mediaHandler.setSupportQualityControls(true);
            mediaHandler.getQualityControl().setRemoteSendMaxPreset(
                    initialQualityPreferences);
        }

        try
        {
            callPeer.invite(emergency);
        }
        catch(OperationFailedException ex)
        {
            // if inviting call peer fails for some reason, change its state
            // if not already failed
            callPeer.setState(CallPeerState.FAILED);
            throw ex;
        }

        return callPeer;
    }

    /**
     * Creates a new call and sends a RINGING response.
     *
     * @param jainSipProvider the provider containing
     * <tt>sourceTransaction</tt>.
     * @param serverTran the transaction containing the received request.
     * @param isAutoAnswered if true, this call is being auto answered,
     * otherwise the user will be prompted to respond to the call.
     * @return CallPeerSipImpl the newly created call peer (the one that sent
     * the INVITE).
     */
    public CallPeerSipImpl processInvite(SipProvider       jainSipProvider,
                                         ServerTransaction serverTran,
                                         boolean           isAutoAnswered)
    {
        Request invite = serverTran.getRequest();

        final CallPeerSipImpl peer =
            createCallPeerFor(serverTran, jainSipProvider, isAutoAnswered);

        //send a ringing response
        Response response = null;
        try
        {
            // If VoIP is disabled we just reject the call.
            boolean voipActive = ConfigurationUtils.isVoIPEnabled();

            if (voipActive)
            {
                logger.trace("will send ringing response: ");
                response = messageFactory.createResponse(Response.RINGING, invite);
            }
            else
            {
                logger.info("Rejecting call as VoIP not allowed in CoS/user config");
                response = rejectCall(invite);
            }

            serverTran.sendResponse(response);

            if (voipActive)
            {
                // We may want to retransmit 180 Ringing responses if the
                // responses are not reliable and we are configured to do so.
                // In which case we kick off a timer to retransmit the response
                // until we've sent the maximum number or the call state has
                // changed.
                if ((serverTran instanceof SIPTransaction) &&
                    !((SIPTransaction)serverTran).isReliable() &&
                    (numRetransmitsRinging > 0))
                {
                    final Timer timer = new Timer("180 response retransmit timer");
                    CallPeerAdapter stateListener = new CallPeerAdapter()
                    {
                        public void peerStateChanged(CallPeerChangeEvent evt)
                        {
                            if(!evt.getNewValue()
                                    .equals(CallPeerState.INCOMING_CALL))
                            {
                                timer.cancel();
                                peer.removeCallPeerListener(this);
                            }
                        }
                    };
                    int interval = retransmitsRingingInterval;
                    int delay = 0;
                    for(int i = 0; i < numRetransmitsRinging; i++)
                    {
                        delay += interval;
                        timer.schedule(new RingingResponseTask(response,
                            serverTran, peer, timer, stateListener), delay);
                        interval *= 2;
                    }

                    peer.addCallPeerListener(stateListener);
                }
                logger.debug("sent a ringing response");
            }
        }
        catch (Exception ex)
        {
            logger.error("Error while trying to send a request", ex);
            peer.setState(CallPeerState.FAILED,
                "Internal Error: " + ex.getMessage());
            return peer;
        }

        return peer;
    }

    /**
     * Processes an incoming INVITE that is meant to replace an existing
     * <tt>CallPeerSipImpl</tt> that is participating in this call. Typically
     * this would happen as a result of an attended transfer.
     *
     * @param jainSipProvider the JAIN-SIP <tt>SipProvider</tt> that received
     * the request.
     * @param serverTransaction the transaction containing the INVITE request.
     * @param callPeerToReplace a reference to the <tt>CallPeer</tt> that this
     * INVITE is trying to replace.
     */
    public void processReplacingInvite(SipProvider       jainSipProvider,
                                       ServerTransaction serverTransaction,
                                       CallPeerSipImpl   callPeerToReplace)
    {
        CallPeerSipImpl newCallPeer
                 = createCallPeerFor(serverTransaction, jainSipProvider, false);
        try
        {
            newCallPeer.answer();
        }
        catch (OperationFailedException ex)
        {
            logger.error(
                    "Failed to auto-answer the referred call peer "
                        + newCallPeer,
                    ex);
            /*
             * RFC 3891 says an appropriate error response MUST be returned
             * and callPeerToReplace must be left unchanged.
             */
            //TODO should we send a response here?
            return;
        }

        //we just accepted the new peer and if we got here then it went well
        //now let's hangup the other call.
        try
        {
            logger.info("Hanging up call " + callPeerToReplace +
                        " due to replacement by " + newCallPeer);
            callPeerToReplace.hangup();
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to hangup the referer "
                            + callPeerToReplace, ex);
            callPeerToReplace.setState(
                            CallPeerState.FAILED, "Internal Error: " + ex);
        }
    }

    /**
     * Sends a re-INVITE request to all <tt>CallPeer</tt>s to reflect possible
     * changes in the media setup (video start/stop, ...).
     *
     * @throws OperationFailedException if a problem occurred during the SDP
     * generation or there was a network problem
     */
    public void reInvite() throws OperationFailedException
    {
        Iterator<CallPeerSipImpl> peers = getCallPeers();

        while (peers.hasNext())
        {
            CallPeerSipImpl peer = peers.next();
            logger.info("Re-inviting peer: " + sanitiseChatAddress(peer.getAddress()));
            peer.sendReInvite();
        }
    }

    /**
     * Set a quality preferences we may use when we start the call.
     * @param qualityPreferences the initial quality preferences.
     */
    public void setInitialQualityPreferences(QualityPreset qualityPreferences)
    {
        this.initialQualityPreferences = qualityPreferences;
    }

    public void registrationStateChanged(RegistrationStateChangeEvent event)
    {
        if (event.getNewState() == RegistrationState.UNREGISTERED ||
            event.getNewState() == RegistrationState.CONNECTION_FAILED)
         {
            // We've lost connectivity, so we need to mark all associated call
            // peers as 'failed'. This will cause the call to be torn down.
            logger.error("Lost connectivity in call, hanging up " + this.getCallID());

            // Since we're tearing the call down, we don't care about future
            // changes in registration state.
            getProtocolProvider().removeRegistrationStateChangeListener(this);

            UIService uiService = ProtocolProviderActivator.getUIService();
            uiService.hangupCall(this);
         }
    }

    /**
     * Create a response and reject this received call.
     *
     * @param serverTransaction the server transaction on which we send the
     *                          response.
     * @param invite the invite request received from the call being pulled.
     */
    public void autoReject(ServerTransaction serverTransaction, Request invite)
    {
        try
        {
            Response response = rejectCall(invite);
            serverTransaction.sendResponse(response);
        }
        catch(ParseException e)
        {
            logger.error("Failure creating response for invite: " + sanitiseInvite(invite.toString()), e);
        }
        catch(SipException e)
        {
            logger.error("Failure sending response for invite: " + sanitiseInvite(invite.toString()), e);
        }
        catch (InvalidArgumentException e)
        {
            logger.error("Invalid argument detected for invite: " + sanitiseInvite(invite.toString()), e);
        }
    }

    @Override
    public void setCallState(CallState state)
    {
        super.setCallState(state);

        // Stop listening to registration state, so the call can be garbage
        // collected.
        if (state.equals(CallState.CALL_ENDED))
        {
            getProtocolProvider().removeRegistrationStateChangeListener(this);
        }
    }

    @Override
    public void setCallState(CallState state, CallPeerChangeEvent changeEvent)
    {
        super.setCallState(state, changeEvent);

        // Stop listening to registration state, so the call can be garbage
        // collected.
        if (state.equals(CallState.CALL_ENDED))
        {
            getProtocolProvider().removeRegistrationStateChangeListener(this);
        }
    }

    private Response rejectCall(Request invite) throws ParseException
    {
        // Determine the response code to reject the call with
        boolean sendDeclineOnCallRejection = getProtocolProvider()
                .getAccountID()
                .getAccountPropertyBoolean(
                        ProtocolProviderFactory.SEND_DECLINE_ON_CALL_REJECTION,
                        false);

        if (sendDeclineOnCallRejection)
        {
            logger.info("Reject call with 603 Decline");
            return messageFactory.createResponse(Response.DECLINE, invite);
        }
        else
        {
            logger.info("Reject call with 486 Busy Here");
            return messageFactory.createResponse(Response.BUSY_HERE, invite);
        }
    }

    /**
     * Task that will retransmit ringing response
     */
    private class RingingResponseTask
        extends TimerTask
    {
        /**
         * The response that will be sent
         */
        private final Response response;

        /**
         * The transaction containing the received request.
         */
        private final ServerTransaction serverTran;

        /**
         * The peer corresponding to the transaction.
         */
        private final CallPeerSipImpl peer;

        /**
         * The timer that starts the task.
         */
        private final Timer timer;

        /**
         * Listener for the state of the peer.
         */
        private CallPeerAdapter stateListener;

        /**
         * Create ringing response task.
         * @param response the response.
         * @param serverTran the transaction.
         * @param peer the peer.
         * @param timer the timer.
         * @param stateListener the state listener.
         */
        RingingResponseTask(Response response, ServerTransaction serverTran,
            CallPeerSipImpl peer, Timer timer, CallPeerAdapter stateListener)
        {
            this.response = response;
            this.serverTran = serverTran;
            this.peer = peer;
            this.timer = timer;
            this.stateListener = stateListener;
        }

        /**
         * Sends the ringing response.
         */
        public void run()
        {
            try
            {
                serverTran.sendResponse(response);
            }
            catch (Exception ex)
            {
                timer.cancel();
                peer.removeCallPeerListener(stateListener);
            }
        }
    }
}
