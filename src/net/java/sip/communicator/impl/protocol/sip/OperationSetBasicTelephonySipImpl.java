/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.Hasher.logHasher;

import java.lang.ref.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.header.extensions.*;
import gov.nist.javax.sip.stack.SIPDialog;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallConference;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.CallPeerState;
import net.java.sip.communicator.service.protocol.CallState;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.emergencylocation.EmergencyCallContext;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetAdvancedTelephony;
import net.java.sip.communicator.service.protocol.OperationSetBasicAutoAnswer;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetSecureSDesTelephony;
import net.java.sip.communicator.service.protocol.OperationSetSecureZrtpTelephony;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.TransferAuthority;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.configuration.ScopedConfigurationService;

/**
 * Implements all call management logic and exports basic telephony support by
 * implementing OperationSetBasicTelephony.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Alan Kelly
 * @author Emanuel Onica
 */
public class OperationSetBasicTelephonySipImpl
    extends AbstractOperationSetBasicTelephony<ProtocolProviderServiceSipImpl>
    implements MethodProcessor,
               OperationSetAdvancedTelephony<ProtocolProviderServiceSipImpl>,
               OperationSetSecureZrtpTelephony,
               OperationSetSecureSDesTelephony
{
    private static final Logger logger = Logger.getLogger(OperationSetBasicTelephonySipImpl.class);
    private final ConfigurationService configService = SipActivator.getConfigurationService();

    /**
     * Config key for whether the user has accepted the latest version of all
     * the EULAs they are required to (including extra EULAs).
     */
    private static final String LATEST_EULA_ACCEPTED =
            "net.java.sip.communicator.plugin.eula.LATEST_EULA_ACCEPTED";

    /**
     * Config key for whether the client is forced to upgrade to a newer version.
     */
    private static final String FORCE_UPDATE = "net.java.sip.communicator.plugin.update.FORCE_UPDATE";

    /**
     * Config key for whether the user's password has expired.
     */
    private static final String PASSWORD_EXPIRED = "net.java.sip.communicator.plugin.pw.PASSWORD_EXPIRED";

    /**
     * Config key for whether the user's CoS allows them to use MaX UC.
     */
    private static final String COS_ALLOWS_MAX_UC = "net.java.sip.communicator.impl.commportal.COS_ALLOWS_MAX_UC";

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance that
     * created us.
     */
    private final ProtocolProviderServiceSipImpl protocolProvider;

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * Contains references for all currently active (non ended) calls.
     */
    private final ActiveCallsRepositorySipImpl activeCallsRepository =
        new ActiveCallsRepositorySipImpl(this);

    /**
     * Transfer authority interacts with user to inform for incoming
     * REFERS for unknown calls.
     */
    private TransferAuthority transferAuthority = null;

    /**
     * The <tt>Timer</tt> which executes delayed tasks.  Used to retry
     * requests that received a "request pending" response.
     */
    private final TimerScheduler timer = new TimerScheduler();

    /**
     * Map "&lt;dialog id&gt;-&lt;cseq number&gt;" to a response handler that
     * may want to deal itself with the response to a request we sent.  In fact
     * we map to a WeakReference so that we don't prevent garbage collection for
     * cases where we don't get a response to the request (and hence never
     * remove the item from the map).
     */
    private final Map<String, WeakReference<ResponseHandler>> responseHandlers =
            new ConcurrentHashMap<>();

    /**
     * Creates a new instance and adds itself as an <tt>INVITE</tt> method
     * handler in the creating protocolProvider.
     *
     * @param protocolProvider a reference to the
     *            <tt>ProtocolProviderServiceSipImpl</tt> instance that created
     *            us.
     */
    public OperationSetBasicTelephonySipImpl(
        ProtocolProviderServiceSipImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
        this.messageFactory = protocolProvider.getMessageFactory();

        protocolProvider.registerMethodProcessor(Request.INVITE, this);
        protocolProvider.registerMethodProcessor(Request.CANCEL, this);
        protocolProvider.registerMethodProcessor(Request.ACK, this);
        protocolProvider.registerMethodProcessor(Request.BYE, this);
        protocolProvider.registerMethodProcessor(Request.REFER, this);
        protocolProvider.registerMethodProcessor(Request.NOTIFY, this);

        protocolProvider.registerEvent("refer");
    }

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param callee the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     * @throws ParseException if <tt>callee</tt> is not a valid SIP address
     * <tt>String</tt>
     */
    public Call createCall(String callee,
                           CallConference conference,
                           EmergencyCallContext emergency)
        throws OperationFailedException,
               ParseException
    {
        return createCall(callee, null, conference, emergency);
    }

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param callee the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @param displayName the user-visible display name of the callee (optional)
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     * @throws ParseException if <tt>callee</tt> is not a valid SIP address
     * <tt>String</tt>
     */
    public Call createCall(String callee,
                           String displayName,
                           CallConference conference,
                           EmergencyCallContext emergency)
        throws OperationFailedException,
               ParseException
    {
        Address toAddress = protocolProvider.parseAddressString(callee);
        toAddress.setDisplayName(displayName);
        // Don't need to hash callee address for explicit call.
        logger.info("Name '" + logHasher(displayName) +
                    "' obtained for callee " + logHasher(callee) + " from the UI.");

        return createOutgoingCall(toAddress, null, conference, emergency);
    }

    /**
     * Initializes a new outgoing <tt>Call</tt> with no peers in it. Intended
     * for use by other <tt>OperationSet</tt>s willing to initialize
     * <tt>Call</tt>s and willing to control their establishment in ways
     * different than {@link #createOutgoingCall(Address,
     * javax.sip.message.Message,CallConference,EmergencyCallContext)}.
     *
     * @return a new outgoing <tt>Call</tt> with no peers in it
     * @throws OperationFailedException if initializing the new outgoing
     * <tt>Call</tt> fails
     */
    protected synchronized CallSipImpl createOutgoingCall()
        throws OperationFailedException
    {
        assertRegistered();

        return new CallSipImpl(this);
    }

    /**
     * Initializes and establishes a new outgoing <tt>Call</tt> to a callee with
     * a specific <tt>Address</tt>.
     *
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     * @param cause the <tt>Message</tt>, if any, which is the cause for the
     * outgoing call to be placed and which carries additional information to be
     * included in the call initiation (e.g. a Referred-To header and token in a
     * Refer request)
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     *
     * @return CallPeer the CallPeer that will be represented by the specified uri.
     * All following state change events will be delivered through that call
     * peer. The Call that this peer is a member of could be retrieved from the
     * CallParticipant instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    private synchronized CallSipImpl createOutgoingCall(
            Address calleeAddress,
            javax.sip.message.Message cause,
            CallConference conference,
            EmergencyCallContext emergency)
        throws OperationFailedException
    {
        CallSipImpl call = createOutgoingCall();
        logger.info("Creating outgoing call to " +
                    sanitiseChatAddress(calleeAddress.toString()));

        if (conference != null)
        {
            call.setConference(conference);
        }

        call.invite(calleeAddress, cause, emergency);

        return call;
    }

    /**
     * Returns an iterator over all currently active calls.
     * NOTE this includes calls being initialized.
     *
     * @return an iterator over all currently active calls.
     */
    public Iterator<CallSipImpl> getActiveCalls()
    {
        return activeCallsRepository.getActiveCalls();
    }

    /**
     * @return The call with the specified ID or null if there isn't one.
     */
    @Override
    public Call getCall(String callID)
    {
        Iterator<? extends Call> activeCalls = activeCallsRepository.getActiveCalls();
        while(activeCalls.hasNext())
        {
            Call call = activeCalls.next();
            if (call.getCallID().equals(callID))
            {
                // Found it (there can only be 1) so return.
                return call;
            }
        }
        return null;
    }

    /**
     * Returns a reference to the {@link ActiveCallsRepositorySipImpl} that we are
     * currently using.
     *
     * @return a reference to the {@link ActiveCallsRepositorySipImpl} that we are
     * currently using.
     */
    protected ActiveCallsRepositorySipImpl getActiveCallsRepository()
    {
        return activeCallsRepository;
    }

    /**
     * Resumes communication with a call peer previously put on hold.
     *
     * @param peer the call peer to put on hold.
     *
     * @throws OperationFailedException if we fail to construct or send the
     * INVITE request putting the remote side on/off hold.
     */
    public void putOffHold(CallPeer peer)
        throws OperationFailedException
    {
        /*
         * XXX We do not need to mark the method putOffHold(CallPeer) as
         * synchronized because it merely delegates to another method.
         */
        putOnHold(peer, false);
    }

    /**
     * Puts the specified CallPeer "on hold".
     *
     * @param peer the peer that we'd like to put on hold.
     *
     * @throws OperationFailedException if we fail to construct or send the
     * INVITE request putting the remote side on/off hold.
     */
    public void putOnHold(CallPeer peer)
        throws OperationFailedException
    {
        /*
         * XXX We do not need to mark the method putOnHold(CallPeer) as
         * synchronized because it merely delegates to another method.
         */
        putOnHold(peer, true);
    }

    /**
     * Puts the specified <tt>CallPeer</tt> on or off hold.
     *
     * @param peer the <tt>CallPeer</tt> to be put on or off hold
     * @param on <tt>true</tt> to have the specified <tt>CallPeer</tt>
     * put on hold; <tt>false</tt>, otherwise
     *
     * @throws OperationFailedException if we fail to construct or send the
     * INVITE request putting the remote side on/off hold.
     */
    private void putOnHold(CallPeer peer, boolean on)
        throws OperationFailedException
    {
        synchronized (peer)
        {
            ((CallPeerSipImpl) peer).putOnHold(on);
        }
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener
     * is registered.
     * <p>
     *
     * @param requestEvent requestEvent fired from the SipProvider to the
     * <tt>SipListener</tt> representing a Request received from the network.
     *
     * @return <tt>true</tt> if the specified event has been handled by this
     * processor and shouldn't be offered to other processors registered for the
     * same method; <tt>false</tt>, otherwise
     */
    public boolean processRequest(RequestEvent requestEvent)
    {
        ServerTransaction serverTransaction =
            requestEvent.getServerTransaction();
        SipProvider jainSipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        String requestMethod = request.getMethod();
        logger.trace("Processing request " + requestMethod);

        if (serverTransaction == null)
        {
            try
            {
                serverTransaction =
                    SipStackSharing.getOrCreateServerTransaction(requestEvent);
            }
            catch (TransactionAlreadyExistsException | TransactionUnavailableException ex)
            {
                // let's not scare the user and only log a message
                logger.error("Failed to create a new server "
                    + "transaction for an incoming request\n"
                    + "(Next message contains the request)", ex);
                return false;
            }
        }

        boolean processed = false;

        // INVITE
        switch (requestMethod)
        {
            case Request.INVITE:
                logger.debug("received INVITE");

                DialogState dialogState = serverTransaction.getDialog().getState();
                if ((dialogState == null) || dialogState.equals(DialogState.CONFIRMED))
                {
                    logger.debug("request is an INVITE. Dialog state=" + dialogState);
                    processInvite(jainSipProvider, serverTransaction);
                    processed = true;
                }
                else
                {
                    logger.error("reINVITEs while the dialog is not confirmed are not currently supported.");
                }
                break;
            // ACK
            case Request.ACK:
                processAck(serverTransaction, request);
                processed = true;
                break;
            // BYE
            case Request.BYE:
                processBye(serverTransaction, request);
                processed = true;
                break;
            // CANCEL
            case Request.CANCEL:
                processCancel(serverTransaction, request);
                processed = true;
                break;
            // REFER
            case Request.REFER:
                logger.debug("received REFER");
                processRefer(serverTransaction, request, jainSipProvider);
                processed = true;
                break;
            // NOTIFY
            case Request.NOTIFY:
                logger.debug("received NOTIFY");
                processed = processNotify(serverTransaction, request);
                break;
        }

        return processed;
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent.
     *
     * @param transactionTerminatedEvent -- an event that indicates that the
     * transaction has transitioned into the terminated state.
     *
     * @return <tt>true</tt> if the specified event has been handled by this
     * processor and shouldn't be offered to other processors registered for the
     * same method; <tt>false</tt>, otherwise
     */
    public boolean processTransactionTerminated(
        TransactionTerminatedEvent transactionTerminatedEvent)
    {
        // nothing to do here.
        return false;
    }

    /**
     * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
     * proper event handler.
     *
     * @param responseEvent the responseEvent that we received from the
     * ProtocolProviderService.
     *
     * @return <tt>true</tt> if the specified event has been handled by this
     * processor and shouldn't be offered to other processors registered for the
     * same method; <tt>false</tt>, otherwise
     */
    public boolean processResponse(ResponseEvent responseEvent)
    {
        ClientTransaction clientTransaction =
            responseEvent.getClientTransaction();

        Response response = responseEvent.getResponse();

        CSeqHeader cseq = ((CSeqHeader) response.getHeader(CSeqHeader.NAME));

        if (cseq == null)
            logger.error("An incoming response did not contain a CSeq header");

        String method = cseq.getMethod();

        SipProvider sourceProvider = (SipProvider) responseEvent.getSource();

        int responseStatusCode = response.getStatusCode();
        boolean processed = false;

        String key = getResponseHandlerKey(clientTransaction.getDialog(), cseq);
        WeakReference<ResponseHandler> ref = responseHandlers.remove(key);
        ResponseHandler responseHandler = (ref != null) ? ref.get() : null;

        if (responseHandler != null)
        {
            processed = responseHandler.handleResponse(responseStatusCode);

            if (processed)
            {
                logger.debug("Response has been handled - exit");
                return processed;
            }
        }

        switch (responseStatusCode)
        {
        // OK
        case Response.OK:
            if (method.equals(Request.INVITE))
            {
                processInviteOK(clientTransaction, response);
                processed = true;
            }
            // Ignore the case of method.equals(Request.BYE)
            break;

        // Ringing
        case Response.RINGING:
            processRinging(clientTransaction, response);
            processed = true;
            break;

        // Session Progress
        case Response.SESSION_PROGRESS:
            processSessionProgress(clientTransaction, response);
            processed = true;
            break;

        // Trying
        case Response.TRYING:
            processTrying(clientTransaction, response);
            processed = true;
            break;

        // Busy
        case Response.BUSY_HERE:
        case Response.BUSY_EVERYWHERE:
        case Response.DECLINE:
            processBusyHere(clientTransaction, response);
            processed = true;
            break;

        // Accepted
        case Response.ACCEPTED:
            if (Request.REFER.equals(method))
            {
                processReferAccepted(clientTransaction, response);
                processed = true;
            }
            break;

        // 401 UNAUTHORIZED
        case Response.UNAUTHORIZED:
        case Response.PROXY_AUTHENTICATION_REQUIRED:
            processAuthenticationChallenge(clientTransaction, response,
                sourceProvider, responseHandler);
            processed = true;
            break;

        case Response.REQUEST_TERMINATED:
            {
                CallPeerSipImpl callPeer = activeCallsRepository.
                        findCallPeer(clientTransaction.getDialog());
                if (callPeer != null)
                {
                    String reasonPhrase = response.getReasonPhrase();

                    if(reasonPhrase == null
                       || reasonPhrase.trim().length() == 0)
                        reasonPhrase = "Request terminated by server!";

                    callPeer.setState(CallPeerState.FAILED, reasonPhrase);
                }
                processed = true;
            }
            break;
         // 301/302 FORWARD
        case Response.MOVED_TEMPORARILY:
        case Response.MOVED_PERMANENTLY:
            {
                CallPeerSipImpl callPeer = activeCallsRepository
                        .findCallPeer(clientTransaction.getDialog());

                if (callPeer == null)
                {
                    logger.error("Failed to find a forwarded call peer.");
                    processed = true;
                    break;
                }

                ContactHeader contactHeader = (ContactHeader)response
                    .getHeader(ContactHeader.NAME);

                if( contactHeader == null )
                {
                    logger.error("Received a forward with no Contact " +
                        "destination: " + response.getStatusCode()
                         + " " + response.getReasonPhrase());

                    callPeer.setState(CallPeerState.FAILED,
                                response.getReasonPhrase());

                    processed = true;
                    break;
                }

                Address redirectAddress = contactHeader.getAddress();

                //try to detect an immediate loop.
                if( callPeer.getPeerAddress().getURI().equals
                    (redirectAddress.getURI()) )
                {
                    String loggablePeer = sanitiseChatAddress(callPeer.getPeerAddress().getURI().toString());
                    logger.error("Redirect loop detected for: "
                                + loggablePeer);

                    callPeer.setState(CallPeerState.FAILED,
                                "Redirect loop detected for: "
                                + loggablePeer);

                    processed = true;
                    break;
                }

                //first start a call to the new address then end the old
                //one.
                CallSipImpl call = callPeer.getCall();
                try
                {
                    // TODO: RayBaum:
                    //  Do we need to re-add PIDF-LO when following a 3xx redirect?
                    call.invite(redirectAddress, null, null);
                }
                catch (OperationFailedException exc)
                {
                    logger.error("Call forwarding failed.", exc);
                    callPeer.setState(CallPeerState.DISCONNECTED,
                            "Call forwarding failed. " + exc.getMessage());
                }

                callPeer.setState(CallPeerState.DISCONNECTED,
                            "Call forwarded. " + response.getReasonPhrase());

                processed = true;
            }
            break;

        // Request Pending
        case Response.REQUEST_PENDING:
            // We received a 'request pending' response, so we should wait
            // then try to send the request again.  First get new transaction,
            // which is a clone of the original.
            ClientTransaction retryTran
                  = processRequestPending(clientTransaction,
                                          response,
                                          sourceProvider);

            // If a UAC receives a 491 response to a re-INVITE, it SHOULD start a
            // timer with a value T chosen as follows:
            //  1. If the UAC is the owner of the Call-ID of the dialog ID
            //     (meaning it generated the value), T has a randomly chosen value
            //     between 2.1 and 4 seconds in units of 10 ms.
            //  2. If the UAC is not the owner of the Call-ID of the dialog ID, T
            //     has a randomly chosen value of between 0 and 2 seconds in units
            //     of 10 ms.
            // When the timer fires, the UAC SHOULD attempt the re-INVITE once more,
            // if it still desires for that session modification to take place.
            Random rnd = new Random();
            int delay;
            SIPDialog dialog = (SIPDialog) clientTransaction.getDialog();
            boolean isOutgoingCall = (dialog != null) & (!dialog.isInitiallyServer());

            if (isOutgoingCall)
            {
                logger.debug("Outgoing call");
                delay = rnd.nextInt(190) * 10 + 2100; // random between 2.1-4s
            }
            else
            {
                logger.debug("Incoming call");
                delay = rnd.nextInt(200) * 10; // random between 0-2s
            }

            logger.info("Reschedule INVITE in " + delay + " ms");

            // Schedule the retry transaction to be sent after the delay.
            timer.schedule(new RequestPendingRetryTask(retryTran), delay);
            processed = true;
            break;

        // errors
        default:

            int responseStatusCodeRange = responseStatusCode / 100;

            /*
             * Maybe this one comes from desktop sharing session, it is
             * possible that keyboard and mouse notifications comes in
             * disorder as interval between two events can be very short
             * (especially for "mouse moved").
             *
             * We have to bypass SIP specifications in the SIP NOTIFY
             * message is desktop sharing specific and thus do not close the
             * call.
             *
             * XXX this is not an optimal solution, the ideal will be
             * to prevent disordering.
             */
            Request request = responseEvent.getClientTransaction().getRequest();
            if(responseStatusCode == 500
                && isRemoteControlNotification(request))
            {
                processed = true;
                break;
            }

            CallPeerSipImpl callPeer
            = activeCallsRepository.findCallPeer(clientTransaction
                    .getDialog());

            // Ignore 503 responses to reINVITES while in a call to avoid
            // tearing the call down unnecessarily. Initial INVITEs have no "tag"
            // so check for a tag in the request to identify reINVITEs. The call
            // peer exists even for the first INVITE.
            if(responseStatusCode == Response.SERVICE_UNAVAILABLE &&
               method.equals(Request.INVITE) &&
               ((ToHeader) request.getHeader(ToHeader.NAME)).getTag() != null &&
               callPeer != null)
            {
                processed = true;
                break;
            }

            // If this is a response to a BYE (probably a 481), just ignore the
            // unexpected response.
            if (Request.BYE.equals(request.getMethod()))
            {
                logger.info("Ignoring unexpected response " + responseStatusCode + " to BYE");
                break;
            }

            if ((responseStatusCodeRange == 4)
                || (responseStatusCodeRange == 5)
                || (responseStatusCodeRange == 6))
            {
                String reason = response.getReasonPhrase();

                WarningHeader warningHeader
                    = (WarningHeader)response.getHeader(WarningHeader.NAME);
                if(warningHeader != null)
                {
                    reason = warningHeader.getText();

                    logger.error("Received error: " + response.getStatusCode()
                                        + " " + response.getReasonPhrase()
                                        + " " + warningHeader.getText()
                                        + "-" + warningHeader.getAgent()
                                        + "-" + warningHeader.getName());
                }
                else
                {
                    logger.error("Received error: " + response.getStatusCode()
                                        + " " + response.getReasonPhrase());
                }

                if (callPeer != null)
                {
                    try
                    {
                        // Try to hang up the call because we've received an
                        // unexpected 4XX, 5XX or 6XX response
                        callPeer.hangup(HANGUP_REASON_UNEXPECTED_RESPONSE,
                            reason);
                    }
                    catch (OperationFailedException ex)
                    {
                        logger.error("Failed to hang up after receiving "
                                        + response.getStatusCode(),ex);

                        // Make sure the call is marked as failed
                        callPeer.setState(CallPeerState.FAILED, reason);
                    }
                }

                processed = true;

                if ((Response.FORBIDDEN == responseStatusCode) &&
                    (Request.INVITE.equals(request.getMethod())))
                {
                    try
                    {
                        logger.warn("Received " + responseStatusCode +
                                    " response to INVITE - " +
                                    "Unregistering before reconnect");
                        // We received 403 in response to an INVITE so the
                        // client has probably become unregistered without
                        // noticing.  Unregister the account to tidy up the
                        // connection and kick the ReconnectPlugin to re-register.
                        protocolProvider.unregister();
                    }
                    catch (OperationFailedException e)
                    {
                        logger.error("Error unregistering " +
                                                           protocolProvider, e);
                    }
                }
            }
            else if ((responseStatusCodeRange == 2)
                  || (responseStatusCodeRange == 3))
            {
                logger.error("Received an non-supported final response: "
                        + response.getStatusCode() + " "
                        + response.getReasonPhrase());

                if (callPeer != null)
                {
                    try
                    {
                        // Try to hang up the call because we've received an
                        // unsupported 2XX or 3XX response
                        callPeer.hangup(HANGUP_REASON_UNSUPPORTED_RESPONSE,
                            response.getReasonPhrase());
                    }
                    catch (OperationFailedException ex)
                    {
                        logger.error("Failed to hang up after receiving "
                                        + response.getStatusCode(),ex);

                        // Make sure the call is marked as failed
                        callPeer.setState(CallPeerState.FAILED,
                            response.getReasonPhrase());
                    }
                }

                processed = true;
            }

            // ignore everything else.
            break;
        }

        return processed;
    }

    /**
     * Get the key for the responseHandlers map, from the given dialog and cseq.
     * @param dialog
     * @param cseq
     * @return The key (may be null if dialog is null)
     */
    private String getResponseHandlerKey(Dialog dialog, CSeqHeader cseq)
    {
        if (dialog == null)
            return null;

        // Ideally we would use the cseq number as part of the key.  That way
        // we can tie a response to the transaction.  Unfortunately, when the
        // key is generated, the cseq number has not been finalised and might
        // later change.
        // Thus we use the method instead. This relies on the client not sending
        // multiple methods at the same time.  This is not a given, but is good
        // enough.
        return dialog.getDialogId() + "-" + cseq.getMethod();
    }

    /**
     * Processes a specific <tt>Response.ACCEPTED</tt> response of an earlier
     * <tt>Request.REFER</tt> request.
     *
     * @param clientTransaction the <tt>ClientTransaction</tt> which brought the
     * response
     *
     * @param accepted the <tt>Response.ACCEPTED</tt> response to an earlier
     * <tt>Request.REFER</tt> request
     */
    private void processReferAccepted(ClientTransaction clientTransaction,
        Response accepted)
    {
        try
        {
            EventPackageUtils.addSubscription(clientTransaction.getDialog(), "refer");
        }
        catch (SipException ex)
        {
            logger.error("Failed to make Accepted REFER response"
                        + " keep the dialog alive after BYE:\n"
                    + accepted, ex);
        }
    }

    /**
     * Updates the call state of the corresponding call peer.
     *
     * @param clientTransaction the transaction in which the response was
     * received.
     * @param response the trying response.
     */
    private void processTrying(ClientTransaction clientTransaction,
                               Response          response)
    {
        Dialog dialog = clientTransaction.getDialog();

        // find the call peer
        CallPeerSipImpl callPeer = activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {
            logger.debug("Received a stray trying response.");
            return;
        }

        // change status
        CallPeerState callPeerState = callPeer.getState();
        if (!CallPeerState.CONNECTED.equals(callPeerState)
            && !CallPeerState.isOnHold(callPeerState))
        {
            callPeer.setState(CallPeerState.CONNECTING);
        }
    }

    /**
     * Updates the call state of the corresponding call peer. We'll also
     * try to extract any details here that might be of use for call peer
     * presentation and that we didn't have when establishing the call.
     *
     * @param clientTransaction the transaction in which the response was
     * received.
     * @param response the Trying response.
     */
    private void processRinging(ClientTransaction clientTransaction,
        Response response)
    {
        Dialog dialog = clientTransaction.getDialog();
        // find the call peer
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {
            logger.debug("Received a stray trying response.");
            return;
        }

        // try to update the display name.
        ContactHeader remotePartyContactHeader =
            (ContactHeader) response.getHeader(ContactHeader.NAME);

        if (remotePartyContactHeader != null)
        {
            Address remotePartyAddress = remotePartyContactHeader.getAddress();

            String displayName = remotePartyAddress.getDisplayName();

            if (displayName != null && displayName.trim().length() > 0)
            {
                callPeer.setDisplayName(displayName);
            }
        }

        // change status.
        callPeer.setState(CallPeerState.ALERTING_REMOTE_SIDE);
    }

    /**
     * Handles early media in 183 Session Progress responses. Retrieves the SDP
     * and makes sure that we start transmitting and playing early media that we
     * receive. Puts the call into a CONNECTING_WITH_EARLY_MEDIA state.
     *
     * @param tran the <tt>ClientTransaction</tt> that the response arrived in.
     * @param response the 183 <tt>Response</tt> to process
     */
    private void processSessionProgress(ClientTransaction tran,
                                        Response          response)
    {
        Dialog dialog = tran.getDialog();
        // find the call
        CallPeerSipImpl callPeer = activeCallsRepository.findCallPeer(dialog);

        if (callPeer.getState() == CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
        {
            // This can happen if we are receiving early media for a second time.
            logger.warn("Ignoring invite 183 since call peer is "
                + "already exchanging early media.");
            return;
        }

        callPeer.processSessionProgress(tran, response);
    }

    /**
     * Sets to CONNECTED that state of the corresponding call peer and
     * sends an ACK.
     *
     * @param clientTransaction the <tt>ClientTransaction</tt> that the response
     * arrived in.
     * @param ok the OK <tt>Response</tt> to process
     */
    private void processInviteOK(ClientTransaction clientTransaction,
                                 Response          ok)
    {
        logger.debug("Processing INVITE OK " + clientTransaction);
        Dialog dialog = clientTransaction.getDialog();
        // find the call
        CallPeerSipImpl callPeer = activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {
            /*
             * Handle dialog forking (e.g. we got an early dialog from the
             * remote party's RINGING response and another, "confirmed" one with
             * the 200 OK that we received from a VoiceMail IVR). Try to find
             * a CallPeer with matching Call-ID and branch and update its
             * dialog.
             */
            callPeer = activeCallsRepository.findCallPeer(
                clientTransaction.getBranchId(),
                ok.getHeader(CallIdHeader.NAME));

            if (callPeer == null)
            {
                //there's definitely no dialog.
                logger.debug("Received a stray ok response.");
                return;
            }

            //dialog's been forked. the one that's stored in the peer must have
            //been in an early state so replace it with this one - confirmed.
            callPeer.setDialog(dialog);
        }

        callPeer.processInviteOK(clientTransaction, ok);
        logger.debug("Processing INVITE OK complete " + clientTransaction);
    }

    /**
     * Sets corresponding state to the call peer associated with this
     * transaction.
     *
     * @param clientTransaction the transaction in which
     * @param busyHere the busy here Response
     */
    private void processBusyHere(ClientTransaction clientTransaction,
                                 Response          busyHere)
    {
        Dialog dialog = clientTransaction.getDialog();
        // find the call
        CallPeerSipImpl callPeer = activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {
            logger.debug("Received a stray busyHere response.");
            return;
        }

        // change status
        callPeer.setState(CallPeerState.BUSY);
    }

    /**
     * Attempts to re-generate the corresponding request with the proper
     * credentials and terminates the call if it fails.
     *
     * @param clientTransaction the corresponding transaction
     * @param response the challenge
     * @param jainSipProvider the provider that received the challenge
     * @param handler ResponseHandler that wanted to handle the original
     * response.  May be <tt>null</tt>.
     */
    private void processAuthenticationChallenge(
        ClientTransaction clientTransaction, Response response,
        SipProvider jainSipProvider, ResponseHandler handler)
    {
        // First find the call and the call peer that this authentication
        // request concerns.
        CallPeerSipImpl callPeer = activeCallsRepository.findCallPeer(
                        clientTransaction.getDialog());

        try
        {
            logger.debug("Authenticating an INVITE request.");

            ClientTransaction retryTran = protocolProvider
                .getSipSecurityManager().handleChallenge(
                    response, clientTransaction, jainSipProvider);

            if (retryTran == null)
            {
                logger.trace("No password supplied or error occurred!");
                return;
            }

            if (callPeer != null)
            {
                callPeer.handleResendRequest(retryTran);
            }

            if (handler != null)
            {
                logger.trace("Pass response handler to re-auth request");
                // The caller wanted to know when the response to the original
                // message came back.  We add the passed-in handler to the
                // responseHandlers map now, and it'll be pulled out again in
                // processResponse() when the response arrives.
                Request req = retryTran.getRequest();
                CSeqHeader cseq = (req != null) ?
                                  (CSeqHeader) req.getHeader(CSeqHeader.NAME) :
                                  null;
                String key = (cseq != null) ?
                            getResponseHandlerKey(retryTran.getDialog(), cseq) :
                            null;
                if (key != null)
                {
                    logger.debug("Add response handler for dialog: " + key);
                    responseHandlers.put(key,
                                         new WeakReference<>(handler));
                }
            }

            retryTran.sendRequest();
        }
        catch (Exception exc)
        {
            // make sure that we fail the peer in case authentication doesn't
            //go well.
            if (callPeer != null)
            {
                callPeer.logAndFail("Failed to authenticate.", exc);
            }
        }
    }

    /**
     * Gets a clone of the request that received a 'request pending' response
     * and returns it so the calling method can schedule it to be resent.
     *
     * @param clientTransaction the corresponding transaction
     * @param response the 'request pending' response
     * @param jainSipProvider the provider that received the response
     */
    private ClientTransaction processRequestPending(
        ClientTransaction clientTransaction, Response response,
        SipProvider jainSipProvider)
    {
        // First find the call and the call peer that this request concerns.
        CallPeerSipImpl callPeer = activeCallsRepository.findCallPeer(
                        clientTransaction.getDialog());

        ClientTransaction retryTran = null;

        try
        {
            logger.debug("Resending a request that received a " +
                                                  "'request pending' response");

            Request pendingRequest = clientTransaction.getRequest();

            retryTran = RequestCloner.cloneRequest(pendingRequest,
                                                   jainSipProvider);

            if (retryTran == null)
            {
                logger.trace("Error occurred: Retry transaction is null.");
            }
            else if (callPeer != null)
            {
                callPeer.handleResendRequest(retryTran);
            }
        }
        catch (Exception exc)
        {
            // make sure that we fail the peer in case cloning the request fails
            if (callPeer != null)
            {
                callPeer.logAndFail("Failed to clone request ", exc);
            }
        }
        return retryTran;
    }

    /**
     * Processes a retransmit or expiration Timeout of an underlying
     * {@link Transaction}handled by this SipListener. This Event notifies the
     * application that a retransmission or transaction Timer expired in the
     * SipProvider's transaction state machine. The TimeoutEvent encapsulates
     * the specific timeout type and the transaction identifier either client or
     * server upon which the timeout occurred. The type of Timeout can by
     * determined by:
     * <tt>timeoutType = timeoutEvent.getTimeout().getValue();</tt>
     *
     * @param timeoutEvent the timeoutEvent received indicating either the
     * message retransmit or transaction timed out.
     *
     * @return <tt>true</tt> if the specified event has been handled by this
     * processor and shouldn't be offered to other processors registered for the
     * same method; <tt>false</tt>, otherwise
     */
    public boolean processTimeout(TimeoutEvent timeoutEvent)
    {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction())
        {
            // don't care. or maybe a stack bug?
            return false;
        }
        else
        {
            transaction = timeoutEvent.getClientTransaction();
        }

        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(transaction.getDialog());

        if (callPeer == null)
        {
            logger.debug("Got a headless timeout event." + timeoutEvent);
            return false;
        }

        /*
         * It's possible that this timeout comes for a desktop sharing NOTIFY
         * request and we don't want to fail the call in this case, so we're
         * consuming the event here.
         */
        Request request = timeoutEvent.getClientTransaction().getRequest();
        if (isRemoteControlNotification(request))
        {
            return true;
        }

        // Try to hangup the peer by indicating that the call has failed.
        try
        {
            hangupCallPeer(callPeer,
                           HANGUP_REASON_TIMEOUT,
                           SipActivator.getResources().getI18NString(
                                          "impl.protocol.sip.TIMEOUT_MESSAGE"));
        }
        catch (Throwable e)
        {
            // If the hangup fails, we just set the state to failed.
            callPeer.setState(CallPeerState.FAILED,
                              SipActivator.getResources().getI18NString(
                                          "impl.protocol.sip.TIMEOUT_MESSAGE"));
        }

        return true;
    }

    /**
     * Process an asynchronously reported IO Exception. Asynchronous IO
     * Exceptions may occur as a result of errors during retransmission of
     * requests. The transaction state machine requires to report IO Exceptions
     * to the application immediately (according to RFC 3261). This method
     * enables an implementation to propagate the asynchronous handling of IO
     * Exceptions to the application.
     *
     * @param exceptionEvent The Exception event that is reported to the
     * application.
     *
     * @return <tt>true</tt> if the specified event has been handled by this
     * processor and shouldn't be offered to other processors registered for the
     * same method; <tt>false</tt>, otherwise
     */
    public boolean processIOException(IOExceptionEvent exceptionEvent)
    {
        logger.error("Got an asynchronous exception event. host="
            + exceptionEvent.getHost() + " port=" + exceptionEvent.getPort());
        return true;
    }

    /**
     * Process an asynchronously reported DialogTerminatedEvent.
     *
     * @param dialogTerminatedEvent -- an event that indicates that the dialog
     * has transitioned into the terminated state.
     *
     * @return <tt>true</tt> if the specified event has been handled by this
     * processor and shouldn't be offered to other processors registered for the
     * same method; <tt>false</tt>, otherwise
     */
    public boolean processDialogTerminated(
        DialogTerminatedEvent dialogTerminatedEvent)
    {
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(dialogTerminatedEvent
                .getDialog());

        if (callPeer == null)
        {
            return false;
        }

        // change status
        callPeer.setState(CallPeerState.DISCONNECTED);
        return true;
    }

    /**
     * Creates a new call and sends a RINGING response.
     *
     * @param sourceProvider the provider containing <tt>sourceTransaction</tt>.
     * @param serverTransaction the transaction containing the received request.
     */
    private void processInvite(SipProvider       sourceProvider,
                               ServerTransaction serverTransaction)
    {
        logger.debug("Processing INVITE " + serverTransaction);

        //first check whether this is a reINVITE or a brand new one.
        Request     invite      = serverTransaction.getRequest();
        Dialog      dialog      = serverTransaction.getDialog();
        CallPeerSipImpl existingPeer
                                = activeCallsRepository.findCallPeer(dialog);

        logger.debug("Processing invite for peer " + existingPeer);

        if(existingPeer != null)
        {
            //this is a reINVITE concerning a particular peer. pass and be done.
            existingPeer.processReInvite(serverTransaction);
            return;
        }

        //this is not a reINVITE. check if it's a transfer
        //(i.e. replacing an existing call).
        ReplacesHeader replacesHeader =
            (ReplacesHeader) invite.getHeader(ReplacesHeader.NAME);

        if (replacesHeader != null)
        {
            //this is a transferred call which is replacing an
            // existing one (i.e. an attended transfer).
            existingPeer = activeCallsRepository.findCallPeer(
                                replacesHeader.getCallId(),
                                replacesHeader.getToTag(),
                                replacesHeader.getFromTag());

            if (existingPeer != null)
            {
                existingPeer.getCall().processReplacingInvite(
                    sourceProvider, serverTransaction, existingPeer);
            }
            else
            {
                protocolProvider.sayErrorSilently(
                    serverTransaction,
                    Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
            }
            return;
        }

        // This is not a reINVITE or transfer. Check whether EULA is accepted, password is not expired
        // and update is not forced, since we may need to block incoming calls.
        ScopedConfigurationService userConfig = configService.user();
        boolean userInteractionAllowed = userConfig.getBoolean(LATEST_EULA_ACCEPTED, false)
            && !userConfig.getBoolean(PASSWORD_EXPIRED, false)
            && !userConfig.getBoolean(FORCE_UPDATE, false)
            && userConfig.getBoolean(COS_ALLOWS_MAX_UC, true);
        if (!userInteractionAllowed)
        {
            logger.info("User interaction is not allowed yet so reject INVITE");
            // Use a 487 since other suitable return codes (e.g. 480, 486) would hang up the call on all endpoints if
            // cross-device rejection is enabled on CFS, which we don't want to happen. This behaviour is documented in
            // this ticket: https://[indeterminate link]
            protocolProvider.sayErrorSilently(serverTransaction, Response.REQUEST_TERMINATED);
            return;
        }

        // Next, check whether we should automatically respond to the invite and
        // then return, as no further processing will be required.
        CallSipImpl call = new CallSipImpl(this);

        OperationSetAutoAnswerSipImpl autoAnswerOpSet
            = (OperationSetAutoAnswerSipImpl) protocolProvider.getOperationSet(
                    OperationSetBasicAutoAnswer.class);

        if(autoAnswerOpSet != null)
        {
            if(autoAnswerOpSet.forwardCall(invite, serverTransaction))
            {
                logger.info("Auto-forwarding invite " + sanitiseInvite(invite.toString()));
                return;
            }
            else if(autoAnswerOpSet.autoReject(call, invite, serverTransaction))
            {
                logger.info("Auto-rejecting call " + call + " with invite " + sanitiseInvite(invite.toString()));
                return;
            }
            else if (autoAnswerOpSet.autoAnswer(
                               call, invite, serverTransaction, sourceProvider))
            {
                logger.info("Auto-answering call " + call + " with invite " + sanitiseInvite(invite.toString()));
                return;
            }
            else
            {
                logger.info("NOT auto responding to call " + call);
            }
        }

        //no redirection necessary. moving on with regular invite processing
        MediaAwareCallPeer<?,?,?> peer =
            call.processInvite(sourceProvider, serverTransaction, false);

        if(failServerTranForInsufficientSecurity(peer, serverTransaction))
        {
            //apparently we are in a high security (paranoia) mode and this
            //call didn't have what it takes. we are bailing.
            return;
        }
    }

    /**
     * Checks whether the <tt>peer</tt> is being invited with the necessary
     * security level, returns <tt>false</tt> if so and <tt>true</tt> otherwise.
     * Typically the method would indicate failure (i.e. return <tt>true</tt>)
     * when we are running in {@link ProtocolProviderFactory#MODE_PARANOIA} and
     * the call peer does not seem to offer any encryption possibilities. We
     * wouldn't care about encryption level in case we are not currently
     * paranoid.
     *
     * @param peer the peer whose security level we are checking.
     * @param serverTransaction the transaction that the call is being initiated
     * with
     *
     * @return <tt>true</tt> if the call should be failed and <tt>false</tt>
     * otherwise.
     */
    private boolean failServerTranForInsufficientSecurity(
        MediaAwareCallPeer<?, ?, ?> peer,
        ServerTransaction serverTransaction)
    {
        if(getProtocolProvider().getAccountID()
            .getAccountPropertyBoolean(
                ProtocolProviderFactory.MODE_PARANOIA, false)
            && peer.getMediaHandler()
                .getAdvertisedEncryptionMethods().length == 0)
        {
            // if in paranoia mode and we don't find any encryption
            // fail peer/call send error with warning explaining why
            String reasonText =
                SipActivator.getResources().getI18NString(
                    "service.gui.security.encryption.required");

            peer.setState(
                CallPeerState.FAILED,
                reasonText,
                Response.SESSION_NOT_ACCEPTABLE);

            // 606 Not acceptable
            // warning header : encryption required
            WarningHeader warning = null;
            try
            {
                //399 Miscellaneous warning
                warning = protocolProvider.getHeaderFactory()
                    .createWarningHeader(
                        protocolProvider.getAccountID().getService()
                        , 399, reasonText);
            }
            catch(InvalidArgumentException | ParseException e)
            {
                logger.error("Cannot create warning header", e);
            }

            try
            {
                protocolProvider.sayError(serverTransaction,
                                        Response.SESSION_NOT_ACCEPTABLE,
                                        warning);
            }
            catch(OperationFailedException e)
            {
                logger.error("Cannot send 606 error!", e);
            }

            return true;
        }

        return false;
    }

    /**
     * Sets the state of the corresponding call peer to DISCONNECTED and
     * sends an OK response.
     *
     * @param serverTransaction the ServerTransaction the BYE request
     * arrived in.
     * @param byeRequest the BYE request to process
     */
    private void processBye(ServerTransaction serverTransaction,
                            Request           byeRequest)
    {
        // find the call
        Dialog dialog = serverTransaction.getDialog();
        CallPeerSipImpl callPeer = activeCallsRepository.findCallPeer(dialog);

        if (callPeer == null)
        {
            logger.debug("Received a stray bye request.");
            return;
        }

        callPeer.processBye(serverTransaction);
    }

    /**
     * Sets the state of the specifies call peer as DISCONNECTED.
     *
     * @param serverTransaction the transaction that the cancel was received in.
     * @param cancelRequest the Request that we've just received.
     */
    private void processCancel(ServerTransaction serverTransaction,
                               Request cancelRequest)
    {
        // find the call
        CallPeerSipImpl callPeer =
            activeCallsRepository.findCallPeer(serverTransaction
                .getDialog());

        if (callPeer == null)
        {
            logger.debug("received a stray CANCEL req. ignoring");
            return;
        }

        callPeer.processCancel(serverTransaction);
    }

    /**
     * Updates the session description and sends the state of the corresponding
     * call peer to CONNECTED.
     *
     * @param serverTransaction the transaction that the ACK was received in.
     * @param ackRequest the ACK <tt>Request</tt> we need to process
     */
    private void processAck(ServerTransaction serverTransaction,
                            Request ackRequest)
    {
        logger.debug("Processing ACK " + serverTransaction);

        // find the call
        CallPeerSipImpl peer = activeCallsRepository.findCallPeer(
                    serverTransaction.getDialog());

        if (peer == null)
        {
            // this is most probably the ack for a killed call - don't signal it
            logger.debug("didn't find an ack's call, returning");
            return;
        }

        peer.processAck(serverTransaction, ackRequest);
    }

    /**
     * Processes a specific REFER request i.e. attempts to transfer the
     * call/call peer receiving the request to a specific transfer
     * target.
     *
     * @param serverTransaction the <tt>ServerTransaction</tt> containing
     * the REFER request
     * @param referRequest the very REFER request
     * @param sipProvider the provider containing <tt>serverTransaction</tt>
     */
    private void processRefer(ServerTransaction serverTransaction,
        final Request referRequest, final SipProvider sipProvider)
    {
        ReferToHeader referToHeader
            = (ReferToHeader) referRequest.getHeader(ReferToHeader.NAME);
        if (referToHeader == null)
        {
            logger.error("No Refer-To header in REFER request:\n"
                + referRequest);
            return;
        }
        Address referToAddress = referToHeader.getAddress();
        if (referToAddress == null)
        {
            logger.error("No address in REFER request Refer-To header:\n"
                + referRequest);
            return;
        }

        final Dialog dialog = serverTransaction.getDialog();

        CallPeerSipImpl callPeer = activeCallsRepository.findCallPeer(dialog);

        if(callPeer == null)
        {
            // if we are missing transfer authority to inform user, just
            // drop thi request cause possible attempt to compromise us.
            if(transferAuthority == null)
            {
                // ignore request and terminate transaction
                logger.warn("Ignoring REFER request without call for request:"
                    + referRequest);
                try
                {
                    serverTransaction.terminate();
                }
                catch (Throwable e)
                {
                    logger.warn("Failed to properly terminate transaction for "
                                    +"a rogue request. Well ... so be it "
                                    + "Request:" + referRequest);
                }

                return;
            }

            FromHeader fromHeader =
                    (FromHeader)referRequest.getHeader(FromHeader.NAME);

            OperationSetPresenceSipImpl opSetPersPresence =
                    (OperationSetPresenceSipImpl) protocolProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

            Contact from = null;
            if(opSetPersPresence != null)
                from = opSetPersPresence.resolveContactID(
                    fromHeader.getAddress().getURI().toString());

            if (from == null && opSetPersPresence != null)
            {
                // No need to hash from address for explicit call.
                logger.debug("received a message from an unknown contact: "
                            + fromHeader.getAddress().getURI().toString());
                //create the volatile contact
                from = opSetPersPresence.createVolatileContact(
                            fromHeader.getAddress().getURI().toString());
            }

            // found no call we must authorise this with user
            // if user don't want it, decline it.
            boolean allowTransfer;

            if(from != null)
                allowTransfer = transferAuthority.processTransfer(
                                    from, referToAddress.getURI().toString());
            else
                allowTransfer = transferAuthority.processTransfer(
                                    fromHeader.getAddress().getURI().toString(),
                                    referToAddress.getURI().toString());

            if(!allowTransfer)
            {
                // send decline
                Response declineResponse;
                try
                {
                    declineResponse = protocolProvider.getMessageFactory()
                        .createResponse(
                                Response.DECLINE,
                                referRequest);
                }
                catch (ParseException e)
                {
                    logger.error("Error while creating 603 response", e);
                    return;
                }

                try
                {
                    serverTransaction.sendResponse(declineResponse);
                }
                catch (Exception e)
                {
                    logger.error("Error while sending the response 603", e);
                    return;
                }

                return;
            }
        }

        //Send Accepted
        Response accepted = null;
        try
        {
            accepted = protocolProvider.getMessageFactory().createResponse(
                    Response.ACCEPTED, referRequest);
        }
        catch (ParseException ex)
        {
            logger.error(
                "Failed to create Accepted response to REFER request:\n"
                    + referRequest, ex);
            /*
             * TODO Should the call transfer not be attempted because the
             * Accepted couldn't be sent?
             */
        }
        boolean removeSubscription = false;
        if (accepted != null)
        {
            Throwable failure = null;
            try
            {
                serverTransaction.sendResponse(accepted);
            }
            catch (InvalidArgumentException | SipException ex)
            {
                failure = ex;
            }
            if (failure != null)
            {
                accepted = null;

                logger.error(
                    "Failed to send Accepted response to REFER request:\n"
                        + referRequest, failure);
                /*
                 * TODO Should the call transfer not be attempted because the
                 * Accepted couldn't be sent?
                 */
            }
            else
            {
                /*
                 * The REFER request has created a subscription. Take it into
                 * consideration in order to not disconnect on BYE but rather
                 * when the last subscription terminates.
                 */
                try
                {
                    removeSubscription = EventPackageUtils
                        .addSubscription(dialog, referRequest);
                }
                catch (SipException ex)
                {
                    logger.error("Failed to make the REFER request "
                                + "keep the dialog alive after BYE:\n"
                                + referRequest, ex);
                }

                // NOTIFY Trying
                try
                {
                    sendReferNotifyRequest(dialog,
                        SubscriptionStateHeader.ACTIVE, null,
                        "SIP/2.0 100 Trying", sipProvider);
                }
                catch (OperationFailedException ex)
                {
                    /*
                     * TODO Determine whether the failure to send the Trying
                     * refer NOTIFY should prevent the sending of the
                     * session-terminating refer NOTIFY.
                     */
                }
            }
        }

        // Before creating the outgoing call  to the refer address we change
        // the call state of the refer peer to referred.
        if (callPeer != null)
            callPeer.setState(CallPeerState.REFERRED);

        /*
         * Regardless of whether the Accepted, NOTIFY, etc. succeeded, try to
         * transfer the call because it's the most important goal.
         */
        CallSipImpl referToCall;
        try
        {
            referToCall = createOutgoingCall(referToAddress,
                                             referRequest,
                                             null,
                                             null);
        }
        catch (OperationFailedException ex)
        {
            referToCall = null;

            logger.error("Failed to create outgoing call to " + referToAddress,
                ex);
        }

        /*
         * Start monitoring the call in order to discover when the
         * subscription-terminating NOTIFY with the final result of the REFER is
         * to be sent.
         */
        final CallSipImpl referToCallListenerSource = referToCall;
        final boolean sendNotifyRequest = (accepted != null);
        final Object subscription = (removeSubscription ? referRequest : null);
        CallChangeListener referToCallListener = new CallChangeAdapter()
        {
            /**
             * The indicator which determines whether the job of this listener
             * has been done i.e. whether a single subscription-terminating
             * NOTIFY with the final result of the REFER has been sent.
             */
            private boolean done;

            /**
             * Notifies this <tt>CallChangeListener</tt> that a <tt>Call</tt>
             * which it has been registered with has changed it state.
             *
             * @param evt a <tt>CallChangeEvent</tt> which specifies the
             * <tt>Call</tt> which has changed its state
             */
            @Override
            public synchronized void callStateChanged(CallChangeEvent evt)
            {
                // we are interested only in CALL_STATE_CHANGEs
                if(evt != null && !evt.getEventType()
                    .equals(CallChangeEvent.CALL_STATE_CHANGE))
                    return;

                if (!done
                        && referToCallStateChanged(
                                referToCallListenerSource,
                                sendNotifyRequest,
                                dialog,
                                sipProvider,
                                subscription))
                {
                    done = true;
                    if (referToCallListenerSource != null)
                        referToCallListenerSource
                            .removeCallChangeListener(this);
                }
            }
        };
        if (referToCall != null)
            referToCall.addCallChangeListener(referToCallListener);
        referToCallListener.callStateChanged(null);
    }

    /**
     * Processes a specific <tt>Request.NOTIFY</tt> request for the purposes of
     * telephony.
     *
     * @param serverTransaction the <tt>ServerTransaction</tt> containing the
     * <tt>Request.NOTIFY</tt> request
     * @param notifyRequest the <tt>Request.NOTIFY</tt> request to be processed
     *
     * @return <tt>true</tt> if we have processed/consumed the request and
     * <tt>false</tt> otherwise.
     */
    private boolean processNotify(ServerTransaction serverTransaction,
        Request notifyRequest)
    {
        /*
         * We're only handling NOTIFY as part of call transfer (i.e. refer)
         * right now.
         */
        EventHeader eventHeader =
            (EventHeader) notifyRequest.getHeader(EventHeader.NAME);
        if ((eventHeader == null)
            || !"refer".equals(eventHeader.getEventType()))
        {
            return false;
        }

        SubscriptionStateHeader ssHeader = (SubscriptionStateHeader)
            notifyRequest.getHeader(SubscriptionStateHeader.NAME);

        if (ssHeader == null)
        {
            logger.error("NOTIFY of refer event type "
                        + "with no Subscription-State header.");

            return false;
        }

        Dialog dialog = serverTransaction.getDialog();
        CallPeerSipImpl peer = activeCallsRepository.findCallPeer(dialog);

        if (peer == null)
        {
            logger.debug("Received a stray refer NOTIFY request.");
            return false;
        }

        // OK
        Response ok;
        try
        {
            ok = messageFactory.createResponse(Response.OK, notifyRequest);
            serverTransaction.sendResponse(ok);
        }
        catch (ParseException ex)
        {
            String message = "Failed to create OK response to refer NOTIFY.";

            logger.error(message, ex);
            peer.setState(CallPeerState.DISCONNECTED, message);
            return false;
        }
        catch (Exception ex)
        {
            String message =
                "Failed to send OK response to refer NOTIFY request.";

            logger.error(message, ex);
            peer.setState(CallPeerState.DISCONNECTED, message);
            return false;
        }

        if (SubscriptionStateHeader.TERMINATED
                .equalsIgnoreCase(ssHeader.getState())
            && !EventPackageUtils
                .removeSubscriptionThenIsDialogAlive(dialog, "refer"))
        {
            peer.setState(CallPeerState.DISCONNECTED);
        }

        return true;
    }

    /**
     * Tracks the state changes of a specific <tt>Call</tt> and sends a
     * session-terminating NOTIFY request to the <tt>Dialog</tt> which referred
     * to the call in question as soon as the outcome of the refer is
     * determined.
     *
     * @param referToCall the <tt>Call</tt> to track and send a NOTIFY request
     * for
     * @param sendNotifyRequest <tt>true</tt> if a session-terminating NOTIFY
     * request should be sent to the <tt>Dialog</tt> which referred to
     * <tt>referToCall</tt>; <tt>false</tt> to send no such NOTIFY request
     * @param dialog the <tt>Dialog</tt> which initiated the specified call as
     * part of processing a REFER request
     * @param sipProvider the <tt>SipProvider</tt> to send the NOTIFY request
     * through
     * @param subscription the subscription to be terminated when the NOTIFY
     * request is sent
     *
     * @return <tt>true</tt> if a session-terminating NOTIFY request was sent
     * and the state of <tt>referToCall</tt> should no longer be tracked;
     * <tt>false</tt> if it's too early to send a session-terminating NOTIFY
     * request and the tracking of the state of <tt>referToCall</tt> should
     * continue
     */
    private boolean referToCallStateChanged(CallSipImpl referToCall,
        boolean sendNotifyRequest, Dialog dialog, SipProvider sipProvider,
        Object subscription)
    {
        CallState referToCallState =
            (referToCall == null) ? null : referToCall.getCallState();
        if (CallState.CALL_INITIALIZATION.equals(referToCallState))
        {
            return false;
        }

        /*
         * NOTIFY OK/Declined
         *
         * It doesn't sound like sending NOTIFY Service Unavailable is
         * appropriate because the REFER request has (presumably) already been
         * accepted.
         */
        if (sendNotifyRequest)
        {
            String referStatus =
                CallState.CALL_IN_PROGRESS.equals(referToCallState)
                    ? "SIP/2.0 200 OK"
                    : "SIP/2.0 603 Declined";
            try
            {
                sendReferNotifyRequest(dialog,
                    SubscriptionStateHeader.TERMINATED,
                    SubscriptionStateHeader.NO_RESOURCE, referStatus,
                    sipProvider);
            }
            catch (OperationFailedException ex)
            {
                // The exception has already been logged.
            }
        }

        /*
         * Whatever the status of the REFER is, the subscription created by it
         * is terminated with the final NOTIFY.
         */
        if (!EventPackageUtils.removeSubscriptionThenIsDialogAlive(dialog,
            subscription))
        {
            CallPeerSipImpl callPeer =
                activeCallsRepository.findCallPeer(dialog);
            if (callPeer != null)
            {
                callPeer.setState(CallPeerState.DISCONNECTED);
            }
        }
        return true;
    }

    /**
     * Sends a <tt>Request.NOTIFY</tt> request in a specific
     * <tt>Dialog</tt> as part of the communication associated with an
     * earlier-received <tt>Request.REFER</tt> request. The sent NOTIFY has
     * a specific <tt>Subscription-State</tt> header and reason, carries a
     * specific body content and is sent through a specific
     * <tt>SipProvider</tt>.
     *
     * @param dialog the <tt>Dialog</tt> to send the NOTIFY request in
     * @param subscriptionState the <tt>Subscription-State</tt> header to be
     * sent with the NOTIFY request
     * @param reasonCode the reason for the specified <tt>subscriptionState</tt>
     * if any; <tt>null</tt> otherwise
     * @param content the content to be carried in the body of the sent NOTIFY
     * request
     * @param sipProvider the <tt>SipProvider</tt> to send the NOTIFY
     * request through
     *
     * @throws OperationFailedException if sending the request fails.
     */
    private void sendReferNotifyRequest(Dialog dialog,
        String subscriptionState, String reasonCode, Object content,
        SipProvider sipProvider) throws OperationFailedException
    {
        Request notify = messageFactory.createRequest(dialog, Request.NOTIFY);
        HeaderFactory headerFactory = protocolProvider.getHeaderFactory();

        // Populate the request.
        String eventType = "refer";
        try
        {
            notify.setHeader(headerFactory.createEventHeader(eventType));
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create " + eventType + " Event header.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        SubscriptionStateHeader ssHeader = null;
        try
        {
            ssHeader =  headerFactory
                .createSubscriptionStateHeader(subscriptionState);
            if (reasonCode != null)
                ssHeader.setReasonCode(reasonCode);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create " + subscriptionState
                + " Subscription-State header.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        notify.setHeader(ssHeader);

        ContentTypeHeader ctHeader = null;
        try
        {
            ctHeader = headerFactory
                .createContentTypeHeader("message", "sipfrag");
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create Content-Type header.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
        try
        {
            notify.setContent(content, ctHeader);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to set NOTIFY body/content.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        protocolProvider.sendInDialogRequest(sipProvider, notify, dialog);
    }

    /**
     * Ends the call with the specified <tt>peer</tt>.
     *
     * @param peer the peer that we'd like to hang up on.
     *
     * @throws ClassCastException if peer is not an instance of this
     * CallPeerSipImpl.
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangupCallPeer(CallPeer peer)
        throws ClassCastException,
               OperationFailedException
    {
        // By default we hang up by indicating no failure has happened.
        hangupCallPeer(peer, HANGUP_REASON_NORMAL_CLEARING, null);
    }

    /**
     * Ends the call with the specified <tt>peer</tt>.
     *
     * @param peer the peer that we'd like to hang up on.
     * @param reasonCode indicates if the hangup is following to a call failure
     * or simply a disconnect indicate by the reason.
     * @param reason the reason of the hangup. If the hangup is due to a call
     * failure, then this string could indicate the reason of the failure
     *
     * @throws ClassCastException if peer is not an instance of this
     * CallPeerSipImpl.
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangupCallPeer(CallPeer peer, int reasonCode, String reason)
        throws ClassCastException,
               OperationFailedException
    {
        synchronized (peer)
        {
            ((CallPeerSipImpl) peer).hangup(reasonCode, reason);
        }
    }

    /**
     * Indicates a user request to answer an incoming call from the specified
     * CallPeer.
     *
     * Sends an OK response to <tt>callPeer</tt>. Make sure that the call
     * peer contains an SDP description when you call this method.
     *
     * @param peer the call peer that we need to send the ok to.
     *
     * @throws OperationFailedException if we fail to create or send the
     * response.
     * @throws ClassCastException if <tt>peer</tt> is not an instance of a
     * <tt>CallPeerSipImpl</tt>
     */
    public void answerCallPeer(CallPeer peer)
        throws OperationFailedException, ClassCastException
    {
        synchronized (peer)
        {
            ((CallPeerSipImpl) peer).answer();
        }
    }

    /**
     * Returns a string representation of this OperationSetBasicTelephony
     * instance including information that would permit to distinguish it among
     * other instances when reading a log file.
     *
     * @return a string representation of this operation set.
     */
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "-[dn="
            + protocolProvider.getOurDisplayName() + " addr=["
            + (protocolProvider.getRegistrarConnection() != null ?
             protocolProvider.getRegistrarConnection().getAddressOfRecord() :
             "no registrar")
            + "]";
    }

    /**
     * Shuts down this opset by hanging up all active calls.
     */
    public synchronized void shutdown()
    {
        logger.trace("Ending all active calls.");
        synchronized (activeCallsRepository)
        {
            Iterator<CallSipImpl> activeCalls = getActiveCalls();

            while (activeCalls.hasNext())
            {
                CallSipImpl call = activeCalls.next();

                Iterator<? extends CallPeer> callPeers  = call.getCallPeers();

                // go through all call peers and say bye to every one.
                while (callPeers.hasNext())
                {
                    CallPeer peer = callPeers.next();
                    try
                    {
                        logger.info("Hanging up call " + peer +
                                    " due to shutdown");
                        this.hangupCallPeer(peer);
                    }
                    catch (Exception ex)
                    {
                        logger.warn("Failed to properly hangup participant "
                            + peer, ex);
                    }
                }
            }
        }
    }

    /**
     * Returns <tt>true</tt> to indicate that the call associated with the
     * given peer is secured, otherwise returns <tt>false</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose security we'd like to check.
     * @return <tt>true</tt> to indicate that the call associated with the
     * given peer is secured, otherwise returns <tt>false</tt>.
     */
    public boolean isSecure(CallPeer peer)
    {
        return ((CallPeerSipImpl) peer).getMediaHandler().isSecure();
    }

    /**
     * Transfers (in the sense of call transfer) a specific
     * <tt>CallPeer</tt> to a specific callee address.
     *
     * @param peer the <tt>CallPeer</tt> to be transferred to the specified
     * callee address
     * @param target the <tt>Address</tt> the callee to transfer <tt>peer</tt>
     * to
     * @throws OperationFailedException if creating or sending the transferring
     * INVITE request fails.
     */
    private void transfer(CallPeer peer, Address target, ResponseHandler handler)
        throws OperationFailedException
    {
        CallPeerSipImpl sipPeer = (CallPeerSipImpl) peer;
        Dialog dialog = sipPeer.getDialog();
        Request refer = messageFactory.createRequest(dialog, Request.REFER);
        HeaderFactory headerFactory = protocolProvider.getHeaderFactory();

        // Refer-To is required
        refer.setHeader(headerFactory.createReferToHeader(target));

        /*
         * Referred-By is optional but only to the extent that the refer target
         * may choose to require a valid Referred-By token.
         *
         * It should represent the party doing the referring - i.e. us.
         */
        PhoneNumberUtilsService phoneNumberUtils =
                                             SipActivator.getPhoneNumberUtils();

        // Reformat the number as CFS does not support E.164 in refer-to/by
        // fields
        String targetStr = phoneNumberUtils.formatNumberForRefer(
            protocolProvider.getDirectoryNumber());

        Address targetAddress = parseAddressString(targetStr);

        Header referredBy = ((HeaderFactoryImpl) headerFactory)
            .createReferredByHeader(targetAddress);

        refer.addHeader(referredBy);

        if (handler != null)
        {
            // The caller wants to know when the response to this message comes
            // back.  We add the passed-in handler to the responseHandlers map
            // now, and it'll be pulled out again in processResponse() when the
            // response arrives.
            CSeqHeader cseq = (CSeqHeader) refer.getHeader(CSeqHeader.NAME);
            String key = getResponseHandlerKey(dialog, cseq);
            if (key != null)
            {
                logger.debug("Add response handler for dialog: " + key);
                responseHandlers.put(key,
                                     new WeakReference<>(handler));
            }
        }

        protocolProvider.sendInDialogRequest(
                        sipPeer.getJainSipProvider(), refer, dialog);
    }

    /**
     * Transfers the call we have with <tt>transferee</tt> to
     * <tt>transferTarget</tt>.
     *
     * @param transferee the <tt>CallPeer</tt> that we are about to transfer.
     * @param transferTarget the <tt>CallPeer</tt> that we are about to direct
     * <tt>transferee</tt> to.
     *
     * @throws OperationFailedException if the transfer fails.
     */
    public void transfer(CallPeer transferee, CallPeer transferTarget)
        throws OperationFailedException
    {
        logger.info("Transfer " + transferee + " to " + transferee);
        PhoneNumberUtilsService phoneNumberUtils = SipActivator.getPhoneNumberUtils();
        Address peerAddress = parseAddressString(transferTarget.getAddress());
        Dialog targetDialog = ((CallPeerSipImpl) transferTarget).getDialog();
        String remoteTag = targetDialog.getRemoteTag();
        String localTag = targetDialog.getLocalTag();
        Replaces replacesHeader = null;
        SipURI sipURI = (SipURI) peerAddress.getURI();

        String targetStr = phoneNumberUtils.formatNumberForRefer(
                                                             sipURI.getUser());

        sipURI = (SipURI) parseAddressString(targetStr).getURI();
        peerAddress.setURI(sipURI);

        try
        {
            replacesHeader = (Replaces)
                ((HeaderFactoryImpl) protocolProvider.getHeaderFactory())
                    .createReplacesHeader(
                        targetDialog.getCallId().getCallId(),
                        (remoteTag == null) ? "0" : remoteTag,
                        (localTag == null) ? "0" : localTag);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create Replaces header for target dialog "
                    + targetDialog,
                OperationFailedException.ILLEGAL_ARGUMENT, ex, logger);
        }
        try
        {
            sipURI.setHeader(ReplacesHeader.NAME,
                    URLEncoder.encode(replacesHeader.encodeBody(
                        new StringBuilder()).toString(), "UTF-8"));
        }
        catch (Exception ex)
        {
            //ParseException or UnsupportedEncodingException
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to set Replaces header " + replacesHeader
                + " to SipURI " + sipURI,
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        //transferee should already be on hold by now but let's make sure he is
        //just in case user changed default settings and we are still getting
        //media from him.
        putOnHold(transferee);

        putOnHold(transferTarget);

        transfer(transferee, peerAddress, null);
    }

    /**
     * Transfers (in the sense of call transfer) a specific
     * <tt>CallPeer</tt> to a specific callee address which already
     * participates in an active <tt>Call</tt>.
     * <p>
     * The method is suitable for providing the implementation of attended call
     * transfer (though no such requirement is imposed).
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> to be transferred to the specified
     * callee address
     * @param target the address in the form of <tt>CallPeer</tt> of the callee
     * to transfer <tt>peer</tt> to
     * @throws OperationFailedException if creating or sending the transferring
     * INVITE request fails.
     */
    public void transfer(CallPeer peer, String target)
        throws OperationFailedException
    {
        transfer(peer, parseAddressString(target), null);
    }

    /**
     * Park this call.  A call park is actually just a SIP transfer to the
     * relevant orbit code.
     * @param peer The call peer who is to be parked
     * @param orbitCode The orbit on which to park the call
     * @param handler Handler for the response from CFS
     * @throws OperationFailedException
     */
    public void parkCall(CallPeer peer, String orbitCode, ResponseHandler handler)
        throws OperationFailedException
    {
        transfer(peer, parseAddressString(orbitCode), handler);
    }

    /**
     * Parses a specific string into a JAIN SIP <tt>Address</tt>.
     *
     * @param addressString the <tt>String</tt> to be parsed into an
     * <tt>Address</tt>
     *
     * @return the <tt>Address</tt> representation of
     * <tt>addressString</tt>
     *
     * @throws OperationFailedException if <tt>addressString</tt> is not
     * properly formatted
     */
    private Address parseAddressString(String addressString)
        throws OperationFailedException
    {
        Address address = null;
        try
        {
            address = protocolProvider.parseAddressString(addressString);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                 "Failed to parse address string " + addressString,
                 OperationFailedException.ILLEGAL_ARGUMENT, ex, logger);
        }
        return address;
    }

    /**
     * Verifies that our protocol provider is properly registered and throws
     * an <tt>OperationFailedException</tt> if that's not the case.
     *
     * @throws OperationFailedException if the protocol provider that created us
     * is not registered.
     */
    private void assertRegistered()
        throws OperationFailedException
    {
        if(!protocolProvider.isRegistered())
        {
            throw new OperationFailedException(
                "The protocol provider should be registered "
                +"before placing an outgoing call.",
                OperationFailedException.PROVIDER_NOT_REGISTERED);
        }
    }

    /**
     * Returns the protocol provider that this operation set belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> that created
     * this operation set.
     */
    public ProtocolProviderServiceSipImpl getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Indicates if the given <tt>request</tt> is a desktop sharing related
     * request.
     *
     * @param request the <tt>Request</tt> to check
     * @return <tt>true</tt> if the given <tt>request</tt> is a desktop sharing
     * related request, <tt>false</tt> - otherwise
     */
    private boolean isRemoteControlNotification(Request request)
    {
        // We're only interested in notify requests.
        if (!request.getMethod().equals(Request.NOTIFY))
            return false;

        // If the request method is NOTIFY we check its content.
        byte raw[] = request.getRawContent();
        String content = new String(raw);

        return
            content.startsWith(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                        + "<remote-control>");
    }

    /**
     * Loop through all calls that are in progress and send re-invites to each of them.
     * This is used to reestablish calls after a Perimeta failover
     */
    public void reinviteAllInProgressCalls()
    {
        Iterator<CallSipImpl> calls = getActiveCalls();
        while (calls.hasNext())
        {
            CallSipImpl call = calls.next();

            if (call.getCallState() == CallState.CALL_IN_PROGRESS)
            {
                try
                {
                    logger.debug("Reinviting call " + call);
                    call.reInvite();
                }
                catch (OperationFailedException ex)
                {
                    logger.error("Failed to reinvite call " + call);
                }
            }
        }
    }

    /**
     * Task sending resending messages rejected with a "request pending"
     * response after a specified period of time.
     */
    private class RequestPendingRetryTask
        extends TimerTask
    {
        private ClientTransaction retryTran;

        public RequestPendingRetryTask(ClientTransaction retryTran)
        {
            this.retryTran = retryTran;
        }
        public void run()
        {
            try
            {
                // Get the dialog to send the request, as that will cause the
                // CSeq number to be updated to the latest free number.
                retryTran.getDialog().sendRequest(retryTran);
            }
            catch (Exception ex)
            {
                logger.error("Failed to start request pending retry task", ex);
            }
        }
    }

    /**
     * Callback for when a SIP response comes in response to a request we sent.
     */
    public interface ResponseHandler
    {
        /**
         * Callback when the SIP response is received.  The handler can perform
         * whatever actions are required, and then return, allowing the SIP code
         * to perform other actions or not, as required.
         * @param responseCode The SIP response code
         * @return <tt>true</tt> if the response has been handled and no further
         * action should be taken, <tt>false</tt> otherwise.
         */
        boolean handleResponse(int responseCode);
    }
}
