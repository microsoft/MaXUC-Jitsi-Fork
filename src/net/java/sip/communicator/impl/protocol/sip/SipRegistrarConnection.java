/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Objects;

import javax.net.ssl.SSLHandshakeException;
import javax.sip.ClientTransaction;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.AllowHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.MinExpiresHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.core.NameValueList;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.ims.ServiceRoute;
import gov.nist.javax.sip.header.ims.ServiceRouteList;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.stack.HopImpl;
import net.java.sip.communicator.impl.protocol.sip.net.ProxyConnection;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.threading.CancellableRunnable;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.Logger;

/**
 * Contains all functionality that has anything to do with registering and
 * maintaining registrations with a SIP Registrar.
 *
 * @author Emil Ivov
 */
public class SipRegistrarConnection
    extends MethodProcessorAdapter
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(SipRegistrarConnection.class);

    /**
    * A reference to the sip provider that created us.
    */
    private ProtocolProviderServiceSipImpl sipProvider = null;

    /**
    * The SipURI containing the address port and transport of our registrar
    * server.
    */
    private SipURI registrarURI = null;

    /**
    * The registrar address host we are connecting to.
    */
    private String registrarName = null;

    /**
     * The name of the property under which the user may specify the number of
     * seconds that registrations take to expire.
     */
    private static final String REGISTRATION_EXPIRATION =
        "net.java.sip.communicator.impl.protocol.sip.REGISTRATION_EXPIRATION";

    private final ThreadingService mThreadingService;

    /**
    * The default amount of time (in seconds) that registration take to
    * expire or otherwise put - the number of seconds we wait before re-
    * registering.
    */
    private static final int DEFAULT_REGISTRATION_EXPIRATION = 600;

    /**
    * The amount of time (in seconds) that registration take to expire or
    * otherwise put - the number of seconds we wait before re-registering.
    */
    private int registrationsExpiration = DEFAULT_REGISTRATION_EXPIRATION;

    /**
    * Keeps our current registration state.
    */
    private RegistrationState currentRegistrationState
        = RegistrationState.UNREGISTERED;

    /**
    * A runnable task that can be scheduled to trigger re-register
    */
    private ReRegisterTask reRegisterTask;

    /**
    * A copy of our last sent register request. (used when unregistering)
    */
    private Request registerRequest = null;

    /**
    * The next long to use as a cseq header value.<br>
    * Access to this field should be synchronized on the {@link #cseqLock} to
    * prevent sending messages with the same cseq value.
    */
    private long nextCSeqValue = (long) 1;

    /**
     * Protects access to {@link #nextCSeqValue}, to help prevent sending
     * multiple requests with the same CSEQ.
     */
    private final Object cseqLock = new Object();

    /**
    * The client transaction that we used for sending the last REGISTER
    * request.
    */
    ClientTransaction regTrans = null;

    /**
    * Option for specifying keep-alive method
    */
    private static final String KEEP_ALIVE_METHOD = "KEEP_ALIVE_METHOD";

    /**
    * Option for keep-alive interval
    */
    private static final String KEEP_ALIVE_INTERVAL = "KEEP_ALIVE_INTERVAL";

    /**
    * Default value for keep-alive method - register
    */
    private static final int KEEP_ALIVE_INTERVAL_DEFAULT_VALUE = 25;

    /**
    * Specifies whether or not we should be using a route header in register
    * requests. This field is specified by the REGISTERS_USE_ROUTE account
    * property.
    */
    private boolean useRouteHeader = false;

    /**
    * The sip address that we're currently behind (the one that corresponds to
    * our account id). ATTENTION!!! This field must remain <tt>null</tt>
    * when this protocol provider is configured as "No Regsitrar" account and
    * only be initialized if we actually have a registrar.
    */
    private Address ourSipAddressOfRecord = null;

    /**
     * The SIP address that we're currently using as our contact for our SIP
     * registration to the registrar.  This should be used on all SIP requests
     * to the registrar for the duration of the connection.
     */
    private Address ourSipContactAddress = null;

    /**
     * callId must be unique from first register to last one, till we
     * unregister.
     */
    private CallIdHeader callIdHeader = null;

    /**
     * The transport to use.
     */
    private String registrationTransport = null;

    /**
     * Registrar port;
     */
    private int registrarPort = -1;

    /**
     * We remember the last received address that REGISTER OK is coming from.
     * For security reasons if we are registered this is the address
     * that all messages must come from.
     */
    private InetAddress lastRegisterAddressReceived = null;

    /**
     * We remember the last received port that REGISTER OK is coming from.
     * For security reasons if we are registered this is the port
     * that all messages must come from.
     */
    private int lastRegisterPortReceived = -1;

    /**
     * The local address we are currently using to send and receive messages
     * with the registrar. May be null if an address could not be resolved.
     */
    private InetAddress currentOutboundAddress;

    /**
     * The address of our SIP registrar (or the intermediary proxy if one is
     * used).
     */
    private InetAddress currentProxy;

    /**
     * The service route header provided to the  SIP registrar on the most
     * recent 200 class response to a register.
     */
    private ServiceRouteList preloadedServiceRouteList;

    /**
    * Creates a new instance of this class.
    *
    * @param registrarName the FQDN of the registrar we will
    * be registering with.
    * @param registrarPort the port of the registrar we will
    * be registering with.
    * @param registrationTransport the transport to use when sending our
    * REGISTER request to the server.
    * @param sipProviderCallback a reference to the
    * ProtocolProviderServiceSipImpl instance that created us.
    */
    public SipRegistrarConnection(
                            String       registrarName,
                            int          registrarPort,
                            String       registrationTransport,
                            ProtocolProviderServiceSipImpl sipProviderCallback)
    {
        this.registrarPort = registrarPort;
        this.registrationTransport = registrationTransport;
        this.registrarName = registrarName;
        this.sipProvider = sipProviderCallback;

        mThreadingService = SipActivator.getThreadingService();

        //init expiration timeout
        this.registrationsExpiration =
            SipActivator.getConfigurationService().global().getInt(
                REGISTRATION_EXPIRATION,
                DEFAULT_REGISTRATION_EXPIRATION);

        //init our address of record to save time later
        getAddressOfRecord();

        //now let's register ourselves as processor for REGISTER related
        //messages.
        sipProviderCallback.registerMethodProcessor(Request.REGISTER, this);
    }

    /**
    * Empty constructor that we only have in order to allow for classes like
    * SipRegistrarlessConnection to extend this class.
    */
    protected SipRegistrarConnection()
    {
        mThreadingService = SipActivator.getThreadingService();
    }

    /**
     * Changes transport of registrar connection and recreates
     * registrar URI.
     * @param newRegistrationTransport the new transport.
     */
    void setTransport(String newRegistrationTransport)
    {
        if(newRegistrationTransport.equals(registrationTransport))
            return;

        this.registrationTransport = newRegistrationTransport;

        if(!registrationTransport.equals(ListeningPoint.UDP))
            registrarURI = null; // re-create it
    }

    /**
    * Sends the REGISTER request to the server specified in the constructor.
    *
    * @throws OperationFailedException with the corresponding error code
    * if registration or construction of the Register request fail.
    */
    void register() throws OperationFailedException
    {
        logger.info("Registering...");

        // skip REGISTERING event if we are already registered
        // we are refreshing our registration
        if (getRegistrationState() != RegistrationState.REGISTERED)
        {
            logger.debug("Reg state is " + getRegistrationState());
            setRegistrationState(RegistrationState.REGISTERING,
                                 RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                                 null);
        }

        Request request;
        try
        {
            //We manage the Call ID Header ourselves.The rest will be handled
            //by our SipMessageFactory
            if (callIdHeader == null)
            {
                logger.debug("Null call Id");
                callIdHeader = this.getJainSipProvider().getNewCallId();
            }

            logger.debug("Creating register request");
            request = sipProvider.getMessageFactory().createRegisterRequest(
                                                       getAddressOfRecord(),
                                                       registrationsExpiration,
                                                       callIdHeader,
                                                       getNextCSeqValue());
        }
        catch (Exception exc)
        {
            logger.debug("Caught exception ", exc);

            if (exc.getCause() instanceof SocketException
               || exc.getCause() instanceof IOException
               || exc.getCause() instanceof SSLHandshakeException)
            {
                logger.debug("Caught exception " + exc.getCause());

                if (exc.getCause().getCause() instanceof CertificateException
                    || exc.getCause().getMessage()
                        .startsWith("Received fatal alert"))
                {
                    logger.debug("Fatal error");
                    setRegistrationState(
                              RegistrationState.UNREGISTERED,
                              RegistrationStateChangeEvent.REASON_USER_REQUEST,
                              exc.getMessage());
                    return;
                }

                if (sipProvider.registerUsingNextAddress())
                {
                    logger.debug("Register using next address, return");
                    return;
                }
            }

            //catches InvalidArgumentException, ParseExeption
            //this should never happen so let's just log and bail.
            logger.error("Failed to create a Register request." , exc);

            // Ask the SIP provider to see if we should retry (0 means no).
            int reregisterDelay = sipProvider.getReregistrationRetryTime();
            if (reregisterDelay != 0)
            {
                // OK, the sipProvider thinks we should retry.
                // Reschedule ask it requests.
                logger.info("Retrying reregistration in " + reregisterDelay);
                scheduleReRegistration(reregisterDelay);

                // We don't want to give up just yet, so avoid all
                // nasty error handling below.
                return;
            }

            setRegistrationState(RegistrationState.CONNECTION_FAILED,
                 RegistrationStateChangeEvent.REASON_INTERNAL_ERROR,
                 exc.getMessage());

            if (exc instanceof OperationFailedException)
            {
                logger.debug("Rethrowing op failed");
                throw (OperationFailedException)exc;
            }
            else
            {
                logger.debug("Throwing new OpFailed");
                throw new OperationFailedException(
                    "Failed to generate a from header for our register request.",
                    OperationFailedException.INTERNAL_ERROR, exc);
            }
        }

        //Transaction
        try
        {
            logger.debug("Getting new transaction");
            regTrans = getJainSipProvider().getNewClientTransaction(request);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error("Could not create a register transaction!\n"
                        + "Check that the Registrar address is correct!",
                        ex);

            setRegistrationState(RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_INTERNAL_ERROR,
                ex.getMessage());

            throw new OperationFailedException(
                "Could not create a register transaction!\n"
                + "Check that the Registrar address is correct!",
                OperationFailedException.NETWORK_FAILURE,
                ex);
        }

        try
        {
            logger.debug("Sending request");
            regTrans.sendRequest();
        }
        //we sometimes get a null pointer exception here so catch them all
        catch (Exception ex)
        {
            logger.debug("Send request exception " + ex);
            if (ex.getCause() instanceof SocketException
               || ex.getCause() instanceof IOException)
            {
                logger.debug("Socket or IO exception");

                if (sipProvider.registerUsingNextAddress())
                {
                    logger.debug("Using next address, return");
                    return;
                }
            }

            logger.error("Could not send out the register request!", ex);
            setRegistrationState(RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , ex.getMessage());

            throw new OperationFailedException(
                "Could not send out the register request!"
                , OperationFailedException.NETWORK_FAILURE
                , ex);
        }

        logger.debug("All OK");
        this.registerRequest = request;
    }

    /**
    * An ok here means that our registration has been accepted or terminated
    * (depending on the corresponding REGISTER request). We change state
    * notify listeners and (in the case of a new registration) schedule
    * reregistration.
    *
    * @param clientTransaction the ClientTransaction that we created when
    * sending the register request.
    * @param response the OK Response that we've just received.
    */
    @SuppressWarnings("unchecked") //legacy jain-sip code for handling multiple
                                   //headers of the same type
    public void processOK(ClientTransaction clientTransaction,
                        Response          response)
    {
        //first extract the expires value that we requested
        int requestedExpiration = 0;
        Request register = clientTransaction.getRequest();

        //first check for an expires param in the contact header
        ContactHeader contactHeader = (ContactHeader) register
            .getHeader(ContactHeader.NAME);
        if (contactHeader != null)
            requestedExpiration = contactHeader.getExpires();
        else
            requestedExpiration = 0;

        //Try the expires header only if there was no expiration interval in
        //the contact address. (Bug report and fix thereof Frederic Fournier)
        if(requestedExpiration <= 0)
        {
            ExpiresHeader expiresHeader = register.getExpires();
            if (expiresHeader != null)
                requestedExpiration = expiresHeader.getExpires();
        }

        FromHeader fromHeader = (FromHeader) register
            .getHeader(FromHeader.NAME);

        if(fromHeader != null && fromHeader.getAddress() != null)
        {
            if(sipProvider.setOurDisplayName(
                fromHeader.getAddress().getDisplayName()))
                    this.ourSipAddressOfRecord = null;
        }

        // Save off Service-Route header(s).  Note that we need to do an SRV lookup on
        // any service-route headers (and also the proxy address, which we also
        // use as a route header on outbound SIP requests), before we can use them.
        // This can block for several seconds if the lookups timeout so we do the following
        // to mitigate this:
        // - Perform the lookup now on the OK rather than waiting for the next outbound SIP
        //   message
        // - Perform the lookup on a separate thread to prevent blocking other messages
        //   (there is only one SIP receive thread)
        // - Note that we can't use the separate thread if we don't yet have a previous
        //   lookup result (i.e. start of day) as we need something to put on outbound
        //   headers. In that case it's OK to block as we're only delaying the initial
        //   registration.
        ListIterator<ServiceRoute> serviceRouteList = (ListIterator<ServiceRoute>) response
            .getHeaders(ServiceRoute.NAME);

        if (getRegistrationState() != RegistrationState.REGISTERED)
        {
            updateServiceRouteList(serviceRouteList);
        }
        else
        {
            mThreadingService.submit("ServiceRouteListUpdater",
                new Runnable()
                {
                    public void run()
                    {
                        updateServiceRouteList(serviceRouteList);
                    }
                }
            );
        }

        // Now check what expiration timeout the registrar has returned.
        int grantedExpiration = -1;

        // First we check to see if there's a matching contact header with an
        // expiration time set. If so, we use that. A matching contact header
        // is one where the address in our last request is the same as the one
        // in the response.
        if (contactHeader != null)
        {
            Address localContactAddress = contactHeader.getAddress();

            if (localContactAddress != null)
            {
                ListIterator<? extends Header> iterator = response.getHeaders(ContactHeader.NAME);

                while (iterator.hasNext())
                {
                    ContactHeader responseContactHdr = (ContactHeader) iterator.next();

                    if (responseContactHdr != null)
                    {
                        logger.debug("Contact header found with expiration: " +
                                     responseContactHdr.getExpires());

                        // We've found a matching Contact header, check whether it
                        // has a valid expiration time.
                        if (localContactAddress.equals(responseContactHdr.getAddress()))
                        {
                            grantedExpiration = responseContactHdr.getExpires();

                            if (grantedExpiration != -1)
                            {
                                logger.debug("Found expiration from Contact header");
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Next we check whether there was an Expires header set. If so we use that.
        if (grantedExpiration == -1)
        {
            ExpiresHeader expiresHeader = response.getExpires();

            if (expiresHeader != null)
            {
                grantedExpiration = expiresHeader.getExpires();
                logger.debug("Found expiration: " + grantedExpiration +
                             " from Expires header");
            }
        }

        // If we still haven't found a expiration, then we assume the timeout we asked
        // for in our last request has been applied unchanged.
        if (grantedExpiration == -1)
        {
            grantedExpiration = requestedExpiration;
            logger.warn("No expiration set in response to REGISTER, using: " +
                        grantedExpiration);
        }

        //If this is a response to a REGISTER request ending our registration
        //then expires would be 0.
        //fix by Luca Bincoletto <Luca.Bincoletto@tilab.com>

        synchronized(this)
        {
            RegistrationState currentState = getRegistrationState();

            // We also take into account the requested expiration since if it was 0.
            // We don't really care what the server replied (I have an asterisk here
            // that gives me 3600 even if I request 0).
            if (grantedExpiration <= 0 || requestedExpiration <= 0)
            {
                logger.debug("Account "
                        + sipProvider.getAccountID().getLoggableAccountID()
                        + " unregistered!");
                setRegistrationState(RegistrationState.UNREGISTERED
                    , RegistrationStateChangeEvent.REASON_USER_REQUEST
                    , "Registration terminated.");
            }
            else if(currentState == RegistrationState.REGISTERED ||
                    currentState == RegistrationState.REGISTERING)
            {
                // remember the address and port from which we received this
                // message
                lastRegisterAddressReceived =
                        ((SIPMessage)response).getRemoteAddress();
                lastRegisterPortReceived = ((SIPMessage)response).getRemotePort();

                //update the set of supported services
                ListIterator<AllowHeader> headerIter
                    = (ListIterator<AllowHeader>) response.getHeaders(AllowHeader.NAME);

                if(headerIter != null && headerIter.hasNext())
                    updateSupportedOperationSets(headerIter);

                if (getRegistrationState().equals(RegistrationState.REGISTERING))
                {
                    logger.debug("Account "
                            + sipProvider.getAccountID().getLoggableAccountID()
                            + " registered!");
                }

                setRegistrationState(
                    RegistrationState.REGISTERED
                    , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                    , null);

                int scheduleTime = grantedExpiration;

                // registration schedule interval can be forced to keep alive
                // with setting property KEEP_ALIVE_METHOD to register and
                // setting the interval with property KEEP_ALIVE_INTERVAL
                // to value in seconds, both properties are account props
                // this does not change expiration header
                // If KEEP_ALIVE_METHOD is null we default send registers on
                // interval of 25 seconds
                String keepAliveMethod =
                    sipProvider.getAccountID().getAccountPropertyString(
                        KEEP_ALIVE_METHOD);

                if((keepAliveMethod != null &&
                    keepAliveMethod.equalsIgnoreCase("register")))
                {
                    int registrationInterval =
                        sipProvider.getAccountID().getAccountPropertyInt(
                            KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL_DEFAULT_VALUE);

                    if (registrationInterval < grantedExpiration)
                    {
                        scheduleTime = registrationInterval;
                    }
                }

                // Schedule a re-registration.
                scheduleReRegistration(scheduleTime);
            }
            else
            {
                logger.info("Not re-registering." +
                            " State: " + currentState +
                            " Granted: " + grantedExpiration +
                            " Requested: " + requestedExpiration);
            }
        }
    }

    /**
     * Checks a particular request is it coming from the same proxy we are
     * currently using. A check for security reasons, preventing injection
     * of other messages in the PP.
     * @param request the request to check.
     * @return <tt>false</tt> if the request doesn't belong to our register
     *         or if we cannot decide we keep the old behaviour.
     */
    public boolean isRequestFromSameConnection(Request request)
    {
        SIPMessage msg = (SIPMessage)request;

        if(msg.getRemoteAddress() != null
            && lastRegisterAddressReceived != null)
        {
            if(!msg.getRemoteAddress().equals(lastRegisterAddressReceived)
                || msg.getRemotePort() != lastRegisterPortReceived)
            {
                return false;
            }
        }
        return true;
    }

    /**
    * Sends a unregistered request to the registrar thus ending our
    * registration.
    * @throws OperationFailedException with the corresponding code if sending
    * or constructing the request fails.
    */
    public void unregister() throws OperationFailedException
    {
        logger.debug("Unregister");
        unregister(true);
    }

    /**
    * Sends a unregistered request to the registrar thus ending our
    * registration.
    *
    * @param sendUnregister indicates whether we should actually send an
    * unREGISTER request or simply set our state to UNREGISTERED.
    *
    * @throws OperationFailedException with the corresponding code if sending
    * or constructing the request fails.
    */
    private void unregister(boolean sendUnregister)
        throws OperationFailedException
    {
        logger.debug("Unregister, sendUnregister=" + sendUnregister);
        if (getRegistrationState() == RegistrationState.UNREGISTERED)
        {
            logger.trace("Trying to unregister when already unresgistered");
            return;
        }

        // Synchronized to prevent another thread checking the registration
        // state and scheduling, between us cancelling pending registrations and setting
        // the state to be unregistering.
        synchronized (this)
        {
            cancelPendingRegistrations();

            if (this.registerRequest == null)
            {
                logger.error("Couldn't find the initial register request");
                setRegistrationState(RegistrationState.CONNECTION_FAILED
                    , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                    , "Could not find the initial regiest request.");
                throw new OperationFailedException(
                    "Could not find the initial register request."
                    , OperationFailedException.INTERNAL_ERROR);
            }

            setRegistrationState(RegistrationState.UNREGISTERING,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST, "");
        }

        if(!sendUnregister)
            return;

        //We are apparently registered so send an un-Register request.
        Request unregisterRequest;
        try
        {
            unregisterRequest = sipProvider.getMessageFactory()
                .createUnRegisterRequest(registerRequest, getNextCSeqValue());
        }
        catch (InvalidArgumentException ex)
        {
            logger.error("Unable to create an unREGISTER request.", ex);
            //Shouldn't happen
            setRegistrationState(
                RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , "Unable to set Expires Header");
            throw new OperationFailedException(
                "Unable to set Expires Header"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        ClientTransaction unregisterTransaction = null;
        try
        {
            unregisterTransaction = getJainSipProvider()
                .getNewClientTransaction(unregisterRequest);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error("Unable to create a unregister transaction", ex);
            setRegistrationState(
                RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , "Unable to create a unregister transaction");
            throw new OperationFailedException(
                "Unable to create a unregister transaction"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        try
        {
            // remove current call-id header
            // on next register we will create new one
            callIdHeader = null;

            unregisterTransaction.sendRequest();

            //if we're currently registered or in a process of unregistering
            //we'll wait for an ok response before changing the status.
            //otherwise we set it immediately.
            if(!(getRegistrationState().equals(RegistrationState.REGISTERED) ||
            getRegistrationState().equals(RegistrationState.UNREGISTERING)))
            {
                logger.info("Setting state to UNREGISTERED.");
                setRegistrationState(
                    RegistrationState.UNREGISTERED
                    , RegistrationStateChangeEvent.REASON_USER_REQUEST, null);

                //kill the registration tran in case it is still active
                if (regTrans != null
                    && regTrans.getState().getValue()
                            <= TransactionState.PROCEEDING.getValue())
                {
                    logger.trace("Will try to terminate reg tran ...");
                    regTrans.terminate();
                    logger.trace("Transaction terminated!");
                }
            }
        }
        catch (SipException ex)
        {
            logger.error("Failed to send unregister request", ex);
            setRegistrationState(
                RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                , "Unable to create a unregister transaction");
            throw new OperationFailedException(
                "Failed to send unregister request"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
    }

    /**
    * Returns the state of this connection.
    * @return a RegistrationState instance indicating the state of our
    * registration with the corresponding registrar.
    */
    public RegistrationState getRegistrationState()
    {
        return currentRegistrationState;
    }

    /**
    * Sets our registration state to <tt>newState</tt> and dispatches an event
    * through the protocol provider service impl.
    * <p>
    * @param newState a reference to the RegistrationState that we're currently
    * detaining.
    * @param reasonCode one of the REASON_XXX error codes specified in
    * {@link RegistrationStateChangeEvent}.
    * @param reason a reason String further explaining the reasonCode.
    */
    void setRegistrationState(RegistrationState newState,
                              int               reasonCode,
                              String            reason)
    {
        logger.debug("State change " + currentRegistrationState + "->" + newState +
                     "reason: " + reasonCode + " (" + reason + ")");

        if (!currentRegistrationState.equals(newState))
        {
            RegistrationState oldState = currentRegistrationState;
            this.currentRegistrationState = newState;

            sipProvider.fireRegistrationStateChanged(
                                        oldState, newState, reasonCode, reason);
        }
        else if (currentRegistrationState == RegistrationState.REGISTERED)
        {
            // Since registration was a success, trigger a re-registration
            // success in case we are waiting for a re-register.
            logger.info("Called rereg success");
            sipProvider.reregistrationSuccess();
        }
    }

    /**
    * The task is started once a registration has been created. It is
    * scheduled to run after the expiration timeout has come to an end when
    * it will resend the REGISTER request.
    */
    private class ReRegisterTask extends CancellableRunnable
    {
        /**
        * Simply calls the register method.
        */
        public void run()
        {
            try
            {
                logger.debug("Reg state is " + getRegistrationState());

                if (getRegistrationState() == RegistrationState.REGISTERED)
                {
                    logger.debug("Registering");
                    register();
                }
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to reRegister", ex);
                setRegistrationState(
                    RegistrationState.CONNECTION_FAILED
                    , RegistrationStateChangeEvent.REASON_INTERNAL_ERROR
                    , "Failed to re register with the SIP server.");
            }
        }
    }

    /**
    * Cancels all pending reregistrations. The method is useful when shutting
    * down.
    */
    private void cancelPendingRegistrations()
    {
        if (reRegisterTask != null)
        {
            reRegisterTask.cancel();
            reRegisterTask = null;
        }
    }

    /**
    * Schedules a re-registration for after almost <tt>expires</tt>
    * seconds.
    *
    * - If the expiry is more than 60 seconds, keep a margin of 10%.
    * - If it's more than 10 seconds, keep a margin of 5 seconds.
    * - Otherwise it's too short to have a margin, so just register on the expiry.
    *   (That's probably a misconfiguration of the line).
    *
    * <p>
    * @param expires the number of seconds that we specified in the
    * expires header when registering.
    */
    private void scheduleReRegistration(int expires)
    {
        synchronized (this)
        {
            RegistrationState currentState = getRegistrationState();

            // If we aren't registered, then don't schedule a re-registration,
            // otherwise we can end up with multiple registrations at the same
            // time if we register before the task is executed.
            if (currentState == RegistrationState.REGISTERED)
            {
                cancelPendingRegistrations();
                reRegisterTask = new ReRegisterTask();

                // java.util.Timer thinks in miliseconds and expires header contains
                // seconds

                // Margin as per JavaDoc.

                if (expires > 60)
                {
                    expires = expires * 900;
                }
                else if (expires > 10)
                {
                    expires = (expires - 5) * 1000;
                }
                else
                {
                    expires = expires * 1000;
                }

                logger.debug("Schedule re-registration in " + expires + " ms");
                mThreadingService.schedule("SIP re-registration", reRegisterTask, expires);
            }
            else
            {
                logger.info("Not scheduling re-registration as state is: " + currentState);
            }
        }
    }

    /**
     * Force a re-register to be sent now, rather than when the timer pops.
     */
    public synchronized void reRegisterNow()
    {
        reRegister(1);
    }

    /**
     * Force a re-register after expires seconds
     * @param expires number of seconds until re-register is sent
     */
    public synchronized void reRegister(int expires)
    {
        // First cancel any outstanding re-registers as we don't want to have
        // multiple timers going.
        cancelPendingRegistrations();
        scheduleReRegistration(expires);
    }

    /**
    * Returns the next long to use as a cseq header value.
    * @return the next long to use as a cseq header value.
    */
    private long getNextCSeqValue()
    {
        synchronized (cseqLock)
        {
           long cseqNum = nextCSeqValue;
           nextCSeqValue++;
           return cseqNum;
        }
    }

    /**
    * Handles a NOT_IMPLEMENTED response sent in reply of our register request.
    *
    * @param transatcion the transaction that our initial register request
    * belongs to.
    * @param response our initial register request.
    */
    public void processNotImplemented(ClientTransaction transatcion,
                                    Response response)
    {
            setRegistrationState(
                RegistrationState.CONNECTION_FAILED
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , registrarName
                + " does not appear to be a sip registrar. (Returned a "
                +"NOT_IMPLEMENTED response to a register request)");
    }

    /**
     * Analyzes the content of the <tt>allowHeader</tt> and determines whether
     * some of the operation sets in our provider need to be disabled.
     *
     * @param headerIter the list of <tt>AllowHeader</tt>s that we need to
     * analyze for supported features.
     */
    private void updateSupportedOperationSets(
                                        ListIterator<AllowHeader> headerIter)
    {
        HashSet<String> set = new HashSet<>();
        while(headerIter.hasNext())
        {
            set.add(headerIter.next().getMethod());
        }

        //Emil XXX: I am not sure how this would work out with most providers
        //so we are going to only start by implementing this feature for IM.
        if ( !set.contains(Request.MESSAGE) )
            sipProvider.removeSupportedOperationSet(
                            OperationSetBasicInstantMessaging.class);
    }

    /**
    * Returns the JAIN SIP provider that should be used for communication with
    * our current registrar.
    *
    * @return the JAIN SIP provider that should be used for communication with
    * our current registrar.
    */
    public SipProvider getJainSipProvider()
    {
        return sipProvider.getJainSipProvider(getTransport());
    }

    /**
    * Returns the transport that this connection is currently using to
    * communicate with the Registrar.
    *
    * @return the transport that this connection is using.
    */
    public String getTransport()
    {
        return this.registrationTransport;
    }

    /**
    * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
    * proper event handler.
    *
    * @param responseEvent the responseEvent that we received
    *            ProtocolProviderService.
    * @return <tt>true</tt> if the specified event has been handled by this
    *         processor and shouldn't be offered to other processors registered
    *         for the same method; <tt>false</tt>, otherwise
    */
    public boolean processResponse(ResponseEvent responseEvent)
    {
        logger.debug("Processing response " + responseEvent);
        ClientTransaction clientTransaction = responseEvent
            .getClientTransaction();

        Response response = responseEvent.getResponse();

        SipProvider sourceProvider = (SipProvider)responseEvent.getSource();
        boolean processed = false;

        //OK
        if (response.getStatusCode() == Response.OK) {
            processOK(clientTransaction, response);
            processed = true;
        }
        //NOT_IMPLEMENTED
        else if (response.getStatusCode() == Response.NOT_IMPLEMENTED) {
            processNotImplemented(clientTransaction, response);
            processed = true;
        }
        //Trying
        else if (response.getStatusCode() == Response.TRYING) {
            //do nothing
        }
        //401 UNAUTHORIZED,
        //407 PROXY_AUTHENTICATION_REQUIRED,
        else if (response.getStatusCode() == Response.UNAUTHORIZED
                || response.getStatusCode()
                                == Response.PROXY_AUTHENTICATION_REQUIRED)
        {
            processAuthenticationChallenge(
                    clientTransaction, response, sourceProvider);
            processed = true;
        }
        else if (response.getStatusCode() == Response.INTERVAL_TOO_BRIEF)
        {
            processIntervalTooBrief(response);
            processed = true;
        }
        else if ( response.getStatusCode() >= 400 )
        {
            logger.error("Received an error response ("
                    + response.getStatusCode() + ")" );

            int registrationStateReason =
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;

            if(response.getStatusCode() == Response.NOT_FOUND)
                registrationStateReason =
                    RegistrationStateChangeEvent.REASON_NON_EXISTING_USER_ID;

            //tell the others we couldn't register
            this.setRegistrationState(
                RegistrationState.CONNECTION_FAILED
                , registrationStateReason
                , "Received an error while trying to register. "
                + "Server returned error:" + response.getReasonPhrase()
            );
            processed = true;
        }
        //ignore everything else.

        return processed;
    }

    /**
    * Attempts to re-generate the corresponding request with the proper
    * credentials and terminates the call if it fails.
    *
    * @param clientTransaction the corresponding transaction
    * @param response the challenge
    * @param jainSipProvider the provider that received the challenge
    */
    private void processAuthenticationChallenge(
                        ClientTransaction clientTransaction,
                        Response          response,
                        SipProvider       jainSipProvider)
    {
        try
        {
            logger.debug("Authenticating a Register request.");

            ClientTransaction retryTran;

            // There's a window where we can send 2 REGISTERs with the
            // same CSEQ number, as follows.
            // - send a REGISTER with CSEQ N. nextCseqValue set to N+1.
            // - get 401, processed by thread-1. handleChallenge() creates a new
            //   register request, with the CSEQ set to one more than the last
            //   sent message (so N+1)
            // - register() called again on thread-2.  CSEQ set based on the
            //   value of nextCseqValue, which is still N+1.
            // - thread-1 calls updateRegisterSequenceNumber(), which now sets
            //   nextCseqValue to N+2.
            // - threads 1 and 2 both send messages with CSEQ of N+1.
            // The expected behavior is that thread-2 doesn't run until thread-1
            // has sent its message, in which case thread-2's request is sent
            // with cseq = N+2 and all is well.  We have seen cases where
            // registers are sent in quick succession which can then hit this
            // window.
            // To avoid this the next section is synchronized.
            synchronized (cseqLock)
            {
                // Respond to the challenge
                retryTran = sipProvider.getSipSecurityManager().handleChallenge(
                    response, clientTransaction, jainSipProvider);

                if(retryTran == null)
                {
                    logger.trace("No password supplied or error occured!");
                    unregister(false);
                    return;
                }

                //the security manager has most probably changed the sequence number
                //so let's make sure we update it here.
                updateRegisterSequenceNumber(retryTran);
            }

            retryTran.sendRequest();

            return;
        }
        catch (OperationFailedException exc)
        {
            if(exc.getErrorCode()
                == OperationFailedException.AUTHENTICATION_CANCELED)
            {
                this.setRegistrationState(
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST,
                    "User has canceled the authentication process.");
            }
            else
            {
                //tell the others we couldn't register
                this.setRegistrationState(
                    RegistrationState.AUTHENTICATION_FAILED,
                    RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED,
                    "We failed to authenticate with the server."
                );
            }
        }
        catch (Exception exc)
        {
            // An unexpected exception.  Hopefully this is something temporary
            // and login will work next time.  We don't want to say that
            // authentication has failed (in the SIP sense) as this means we
            // won't ever retry the connection, so just say the connection
            // failed.
            logger.error("Failed to authenticate a Register request", exc);
            this.setRegistrationState(
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                    "Failed to authenticate a Register request");
        }
    }

    /**
    * Processes a Request received on a SipProvider upon which this SipListener
    * is registered.
    * <p>
    *
    * @param requestEvent requestEvent fired from the SipProvider to the
    *            SipListener representing a Request received from the network.
    * @return <tt>true</tt> if the specified event has been handled by this
    *         processor and shouldn't be offered to other processors registered
    *         for the same method; <tt>false</tt>, otherwise
    */
    public boolean processRequest(RequestEvent requestEvent)
    {
        /** @todo send not implemented */
        return false;
    }

    /**
    * Processes a retransmit or expiration Timeout of an underlying
    * {@link Transaction}handled by this SipListener.
    *
    * @param timeoutEvent the timeoutEvent received indicating either the
    *            message retransmit or transaction timed out.
    * @return <tt>true</tt> if the specified event has been handled by this
    *         processor and shouldn't be offered to other processors registered
    *         for the same method; <tt>false</tt>, otherwise
    */
    public boolean processTimeout(TimeoutEvent timeoutEvent)
    {
        logger.debug("Processing timeout " + timeoutEvent);

        if (sipProvider.registerUsingNextAddress())
        {
            return false;
        }

        // don't alert the user if we're already off
        if (!getRegistrationState().equals(RegistrationState.UNREGISTERED))
        {
            setRegistrationState(RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                "A timeout occurred while trying to connect to the server.");
        }
        return true;
    }

    /**
    * Process an asynchronously reported IO Exception.
    *
    * @param exceptionEvent The Exception event that is reported to the
    *            application.
    * @return <tt>true</tt> if the specified event has been handled by this
    *         processor and shouldn't be offered to other processors registered
    *         for the same method; <tt>false</tt>, otherwise
    */
    public boolean processIOException(IOExceptionEvent exceptionEvent)
    {
        logger.debug("Processing IOException " + exceptionEvent);
        setRegistrationState(
            RegistrationState.CONNECTION_FAILED
            , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
            , "An error occurred while trying to connect to the server."
            + "[" + exceptionEvent.getHost() + "]:"
            + exceptionEvent.getPort() + "/"
            + exceptionEvent.getTransport());
        return true;
    }

    /**
     * Process error 423 Interval Too Brief. If there is minimum interval
     * specified use it. Check the specified interval is greater than the one
     * we used in our register.
     * @param response the response containing the min expires header.
     */
    private void processIntervalTooBrief(Response response)
    {
        logger.debug("Process interval too brief. SIP status code: " + response.getStatusCode());

        // interval is too brief, if we have specified correct interval
        // in the response use it and re-register
        MinExpiresHeader header =
                (MinExpiresHeader)response.getHeader(MinExpiresHeader.NAME);

        if(header != null)
        {
            int expires = header.getExpires();
            if(expires > registrationsExpiration)
            {
                registrationsExpiration = expires;

                try
                {
                    register();

                    return;
                }
                catch (Throwable e)
                {
                    logger.error("Cannot send register!", e);

                    setRegistrationState(
                        RegistrationState.CONNECTION_FAILED,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                            "A timeout occurred while trying to " +
                                "connect to the server.");

                    return;
                }
            }
        }

        //tell the others we couldn't register
        this.setRegistrationState(
            RegistrationState.CONNECTION_FAILED
            , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
            , "Received an error while trying to register. "
            + "Server returned error:" + response.getReasonPhrase()
        );
    }

    /**
    * Returns a string representation of this connection instance
    * instance including information that would permit to distinguish it among
    * other sip listeners when reading a log file.
    * <p>
    * @return  a string representation of this operation set.
    */
    public String toString()
    {
        String className = getClass().getName();
        try
        {
            className = className.substring(className.lastIndexOf('.') + 1);
        }
        catch (Exception ex)
        {
            // we don't want to fail in this method because we've messed up
            //something with indexes, so just ignore.
        }
        return className + "-[dn=" + sipProvider.getOurDisplayName()
            +" addr="+getAddressOfRecord() + "]";
    }

    /**
    * Updates our local sequence counter based on the value in the CSeq header
    * of the request that originated the <tt>lastClientTran</tt> transaction.
    * The method is used after running an authentication challenge through
    * the security manager. The Security manager would manually increment the
    * CSeq number of the request so we need to update our local counter or
    * otherwise the next REGISTER we send would have a wrong CSeq.
    *
    * @param lastClientTran the transaction that we should be using to update
    * our local sequence number
    */
    private void updateRegisterSequenceNumber(ClientTransaction lastClientTran)
    {
        Request req = lastClientTran.getRequest();

        CSeqHeader cSeqHeader = (CSeqHeader)req.getHeader(CSeqHeader.NAME);
        long sequenceNumber = cSeqHeader.getSeqNumber();

        //sequenceNumber is the value of the CSeq header in the request we just
        //sent so the next CSeq Value should be set to seqNum + 1.
        synchronized(cseqLock)
        {
          nextCSeqValue = sequenceNumber + 1;
        }
    }

    /**
    * Determines whether Register requests should be using a route header. The
    * return value of this method is specified by the REGISTERS_USE_ROUTE
    * account property.
    *
    * Jeroen van Bemmel: The reason this may needed, is that standards-
    * compliant registrars check the domain in the request URI. If it contains
    * an IP address, some registrars are unable to match/process it (they may
    * forward instead, and get into a forwarding loop)
    *
    * @return true if we should be using a route header.
    */
    public boolean isRouteHeaderEnabled()
    {
        return useRouteHeader;
    }

    /**
    * Returns true if this is a fake connection that is not actually using
    * a registrar. This method should be overridden in
    * <tt>SipRegistrarlessConnection</tt> and return <tt>true</tt> in there.
    *
    * @return true if this connection is really using a registrar and
    * false if it is a fake connection that doesn't really use a registrar.
    */
    public boolean isRegistrarless()
    {
        return false;
    }

    /**
     * The registrar URI we use for registrar connection.
     * @return the registrar URI.
     * * @throws ParseException in case the specified registrar address is not a
    * valid registrar address.
     */
    public SipURI getRegistrarURI()
        throws ParseException
    {
        if(registrarURI == null)
        {
            registrarURI = sipProvider.getAddressFactory().createSipURI(
                null, this.registrarName);

            if(registrarPort != ListeningPoint.PORT_5060)
                registrarURI.setPort(registrarPort);

            if(!registrationTransport.equals(ListeningPoint.UDP))
                registrarURI.setTransportParam(ListeningPoint.TCP);
        }
        return registrarURI;
    }

    /**
    * Returns the address of record that we are using to register against our
    * registrar or null if this is a fake or "Registrarless" connection. If
    * you are trying to obtain an address to put in your from header and don't
    * know what to do in the case of registrarless accounts - think about using
    * <tt>ProtocolProviderServiceSipImpl.createAddressOfRecord()</tt>.
    *
    * @return our Address Of Record
    */
    public Address getAddressOfRecord()
    {
        //if we have a registrar we should return our Address of record here.
        if(this.ourSipAddressOfRecord != null)
            return this.ourSipAddressOfRecord;

        // the connection would not have an address of record if it does not
        // have a registrar.
        if(isRegistrarless())
            return null;

        //create our own address.
        String ourUserID =
            sipProvider.getAccountID().getAccountPropertyString(
                ProtocolProviderFactory.USER_ID);

        String sipUriHost = null;
        if(ourUserID.contains("@")
            && ourUserID.indexOf("@") < ourUserID.length() -1 )
        {
            //use the domain in the SIP URI if possible.
            sipUriHost = ourUserID.substring( ourUserID.indexOf("@") + 1 );
            ourUserID = ourUserID.substring( 0, ourUserID.indexOf("@") );
        }

        //if there was no domain name in the SIP URI use the registrar address
        if(sipUriHost == null)
            sipUriHost = registrarName;

        SipURI ourSipURI;
        try
        {
            ourSipURI = sipProvider.getAddressFactory().createSipURI(
                            ourUserID, sipUriHost);
            ourSipAddressOfRecord = sipProvider.getAddressFactory()
                .createAddress(sipProvider.getOurDisplayName(), ourSipURI);
            ourSipAddressOfRecord.setDisplayName(sipProvider.getOurDisplayName());
        }
        catch (ParseException ex)
        {
            throw new IllegalArgumentException(
                "Could not create a SIP URI for user "
                + ourUserID + "@" + sipUriHost
                + " and registrar "
                + registrarName);
        }
        return ourSipAddressOfRecord;
    }

    /**
    * Returns the contact address to use for the current registration.  If this
    * is registrarless connection or we don't already have a contact address,
    * we call into the SIPProvider to create one based on our current IP
    * address.
    *
    * @param intendedDestination the destination that we are talking to
    * @return our contact address
    */
    public Address getContactAddress(SipURI intendedDestination)
    {
        // If we already have a contact address, just return it.  Otherwise, we
        // should create a new one.
        if (this.ourSipContactAddress != null)
        {
            return this.ourSipContactAddress;
        }

        Address contactAddress = sipProvider.getContactURI(intendedDestination);

        // Store the route to the registrar so we can determine if it changes.
        currentProxy =
            sipProvider.getIntendedDestination(intendedDestination).getAddress();
        currentOutboundAddress = getLocalIpToUse();

        // We only store the contact address if this connection has a
        // registrar, otherwise we should just create the address every time.
        if (!isRegistrarless())
        {
            this.ourSipContactAddress = contactAddress;
        }

        logger.info("SIP registrar generated new contact address");

        return contactAddress;
    }

    /**
    * Updates the list of ServiceRoute headers saved off on the most recent
    * 200 class response to a REGISTER.
    *
    * @param li ListIterator of ServiceRouteHeaders
    */
    private void updateServiceRouteList(ListIterator<ServiceRoute> li)
    {

        ServiceRouteList serviceRouteList = new ServiceRouteList();

        // As per TS24.229 5.1.2A.1.1 we should put the contact details of the
        // outbound proxy first in an IMS network. It's also implied in
        // RFC3261 and RFC3608 that this should also be the case in a non-IMS
        // network (and that matches AM behaviour).
        ProxyConnection proxyConnection = sipProvider.getConnection();
        if (proxyConnection != null)
        {
            try
            {
                InetSocketAddress inetAddress = proxyConnection.getAddress();
                String transport = proxyConnection.getTransport();
                int port = inetAddress.getPort();
                String host = inetAddress.getAddress().getHostName();
                // It appears that AD always does loose routing and there's no
                // configuration option covering it so set it as default.
                String proxy = "<sip:" + host + ":" + port + ";transport=" + transport + ";lr>";
                AddressImpl address = (AddressImpl)sipProvider.getAddressFactory().
                       createAddress(proxy);

                // This doesn't represent a Service-Route header but is the
                // most convenient way for it to be picked up alongside
                // the real Service-Route headers.
                ServiceRoute serviceRoute = new ServiceRoute();
                serviceRoute.setAddress(address);

                // Save this next hop for the route header - without
                // performing the SRV lookup that DefaultRouter.createHop()
                // would usually do on a Route header.
                //
                // This avoids us needing to do a lookup on the Route header
                // when we send an outbound SIP request with a Route header.
                // a) Doing that could add a long delay to sending the SIP
                // request as the DNS queries could take up to 2s each if they
                // time out, and there could be multiple such DNS queries
                // performed in different domains, creating noticeable delays
                // to e.g. call setup.
                // b) It can also cause problems if the customer has SRV
                // results for their SIP domain with a different port to what
                // is configured for their outbound proxy. Although this
                // sounds wrong, customers have gotten away with it prior to us
                // adding this Route header, and we don't want to break things
                // for them.
                //
                // Note we only save this for the first Route header - not
                // any service-route headers that we copy across below - as
                // only the first one would require an SRV lookup as per the
                // logic in DefaultRouter.getNextHop()
                serviceRoute.setHop(new HopImpl(host, port, transport));

                serviceRouteList.add(serviceRoute);
            }
            catch (Exception ex)
            {
                logger.debug("Failed to find proxy address for route header");
            }
        }

        // Populate Service Routes
        while(li.hasNext())
        {
            ServiceRoute currentServiceRoute = li.next();
            ServiceRoute serviceRoute = new ServiceRoute();
            AddressImpl address = ((AddressImpl) ((AddressImpl)
                currentServiceRoute.getAddress()).clone());

            serviceRoute.setAddress(address);

            serviceRoute.setParameters((NameValueList) currentServiceRoute.
                getParameters().clone());

            serviceRouteList.add(serviceRoute);
        }

        this.preloadedServiceRouteList = serviceRouteList;
    }

    /**
    * Returns a list iterator containing Service-Route headers from
    * the most recent registration event.
    *
    * @return ListIterator of ServiceRouteHeaders
    */
    public ListIterator<ServiceRoute> getServiceRouteList()
    {
        if (this.preloadedServiceRouteList != null)
        {
            return this.preloadedServiceRouteList.listIterator();
        }

        return null;
    }

    /**
     * Determine the local IP that will be used when sending SIP messages to
     * the registrar.
     *
     * @return The local IP which will be used, or null if it could not be
     * resolved.
     */
    public InetAddress getLocalIpToUse()
    {
        // Store the route to the registrar so we can determine if it changes.
        NetworkAddressManagerService networkManager =
                                 SipActivator.getNetworkAddressManagerService();

        if (networkManager == null)
        {
            logger.error("NetworkAddressManagerService was null when " +
                         "choosing a local IP address to use for SIP registration");
            return null;
        }

        if (currentProxy == null)
        {
            // If we don't currently have a SIP connection, return null to allow
            // the caller to decide what IP to use.
            logger.warn("SipRegistrarConnection was asked for our registered " +
                         "IP, but there was no saved value");
            return null;
        }

        return networkManager.getLocalHost(currentProxy);
    }

    /**
     * Determine if the local IP address we use to send and receive SIP messages
     * has changed.
     *
     * @return <tt>true</tt> if the local IP we're using has changed, and
     * <tt>false</tt> otherwise.
     */
    public boolean routeToRegistrarChanged()
    {
        return !Objects.equals(currentOutboundAddress, getLocalIpToUse());
    }
}
