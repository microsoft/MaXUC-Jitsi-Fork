/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static net.java.sip.communicator.impl.gui.main.call.CallManager.getInProgressCalls;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import com.google.common.annotations.VisibleForTesting;

import gov.nist.core.net.AddressResolver;
import gov.nist.javax.sip.*;
import gov.nist.javax.sip.header.*;

import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.threading.ThreadFactoryBuilder;
import net.java.sip.communicator.util.*;
import org.jitsi.util.OSUtils;

/**
 * This class is the <tt>SipListener</tt> for all JAIN-SIP
 * <tt>SipProvider</tt>s. It is in charge of dispatching the received messages
 * to the suitable <tt>ProtocolProviderServiceSipImpl</tt>s registered with
 * <tt>addSipListener</tt>. It also contains the JAIN-SIP pieces which are
 * common between all <tt>ProtocolProviderServiceSipImpl</tt>s (namely 1
 * <tt>SipStack</tt>, 2 <tt>SipProvider</tt>s, 3 <tt>ListeningPoint</tt>s).
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 * @author Alan Kelly
 * @author Sebastien Mazy
 */
public class SipStackSharing
    implements SipListener,
               NetworkConfigurationChangeListener
{
    /**
     * We set a custom parameter in the contact address for registrar accounts,
     * so as to ease dispatching of incoming requests in case several accounts
     * have the same username in their contact address, eg:
     * sip:username@192.168.0.1:5060;transport=udp;registering_acc=example_com
     */
    public static final String CONTACT_ADDRESS_CUSTOM_PARAM_NAME
        = "registering_acc";

    /**
     * Logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(SipStackSharing.class);

    /**
     * Our SIP stack (provided by JAIN-SIP).
     */
    private final SipStackExt stack;

    /**
     * The JAIN-SIP provider that we use for clear UDP/TCP.
     */
    private SipProvider clearJainSipProvider = null;
    /**
     *
     * The JAIN-SIP provider that we use for TLS.
     */
    private SipProvider secureJainSipProvider = null;

    /**
     * The candidate recipients to choose from when dispatching messages
     * received from one the JAIN-SIP <tt>SipProvider</tt>-s. for thread safety
     * issues reasons, better iterate on a copy of that set using
     * <tt>getSipListeners()</tt>.
     */
    private final Set<ProtocolProviderServiceSipImpl> listeners
        = new HashSet<>();

    /**
     * Lock that must be acquired to access the SIP {@code listeners}.
     */
    private final Object listenerLock = new Object();

    /**
     * Executor that runs the thread that will reset the SIP stack following an
     * update to the macOS keychain.
     */
    private final ExecutorService reloadExecutor =
            Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder()
                            .setName("sip-stack-keychain-monitor-thread")
                            .build());
    /**
     * The property indicating whether a random UDP and TCP port should
     * be used by default for clear communications.
     */
    private static final String RANDOM_CLEAR_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.SIP_RANDOM_CLEAR_PORT";

    /**
     * The property indicating whether a random TLS (TCP) port should
     * be used by default for secure communications.
     */
    private static final String RANDOM_SECURE_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.SIP_RANDOM_SECURE_PORT";

    /**
     * The property indicating the preferred UDP and TCP
     * port to bind to for clear communications.
     */
    private static final String PREFERRED_CLEAR_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.SIP_PREFERRED_CLEAR_PORT";

    /**
     * Constructor for this class. Creates the JAIN-SIP stack.
     * @param addressResolver - the AddressResolver object for
     * performing SRV lookups
     * @throws OperationFailedException if creating the stack fails.
     */
    SipStackSharing(AddressResolver addressResolver)
            throws OperationFailedException
    {
        // init of the stack
        try
        {
            SipFactory sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");

            Properties sipStackProperties = new SipStackProperties();

            // Create SipStack object
            stack = (SipStackExt)sipFactory.createSipStack(sipStackProperties);
            logger.trace("Created stack: " + stack);

            stack.setAddressResolver(addressResolver);

            // Rather than using the default set of ciphers that the SIP stack
            // chose in the distant past (which are now discredited as weak)
            // we pass in the full set of ciphers that Java supports, which
            // includes much stronger ones.
            CertificateService certificateService = SipActivator.getCertificateService();
            // certificateService will not normally be null, but it is in UTs so
            // we skip affecting the ciphers in that case
            if (certificateService != null)
            {
                SSLContext sslContext = certificateService.getSSLContext();
                SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters();
                String[] cipherSuites = defaultSSLParameters.getCipherSuites();
                stack.setEnabledCipherSuites(cipherSuites);
            }

            SipActivator.getNetworkAddressManagerService()
                    .addNetworkConfigurationChangeListener(this);
        }
        catch (Exception ex)
        {
            logger.fatal("Failed to get SIP Factory.", ex);
            throw new OperationFailedException("Failed to get SIP Factory"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
        }
    }

    /**
     * Adds this <tt>listener</tt> as a candidate recipient for the dispatching
     * of new messages received from the JAIN-SIP <tt>SipProvider</tt>s.
     *
     * @param listener a new possible target for the dispatching process.
     *
     * @throws OperationFailedException if creating one of the underlying
     * <tt>SipProvider</tt>s fails for whatever reason.
     */
    public void addSipListener(ProtocolProviderServiceSipImpl listener)
        throws OperationFailedException
    {
        boolean shouldStartListening = false;
        synchronized (listenerLock)
        {
            if (listeners.size() == 0)
            {
                shouldStartListening = true;
            }
            listeners.add(listener);
            logger.trace(listeners.size() + " listeners now");
        }

        if (shouldStartListening)
        {
            startListening();
            if (OSUtils.isMac())
            {
                startListeningForKeychainUpdates();
            }
        }
    }

    /**
     * Builds the task that will recreate the SIP stack when
     * the macOS system root certificate keychain is updated. This task will
     * be run by the {@code reloadExecutor}.
     */
    private void startListeningForKeychainUpdates()
    {
        final CertificateService cvs = SipActivator.getCertificateService();

        CompletableFuture.supplyAsync(() -> null, reloadExecutor)
                .thenComposeAsync((obj) -> cvs.getMacOSKeychainUpdateTrigger(),
                                reloadExecutor)
                .thenApply((obj) -> hasCallsInProgress())
                .thenCompose(this::scheduleSipStackReset)
                .thenRun(this::resetSipListeners)
                .exceptionally((ex) -> {
                    logger.error("Failed to reset SIP Stack", ex);
                    return null;
                });
    }

    /**
     * Resets the SIP stack by removing all listeners (forcing it to be
     * destroyed) and adding them back individually, thereby triggering
     * a rebuild of the stack.
     */
    private void resetSipListeners()
    {
        synchronized (listenerLock)
        {
            Set<ProtocolProviderServiceSipImpl> copiedListeners =
                    getSipListeners();

            for (var listener : copiedListeners)
            {
                removeSipListener(listener);
            }

            for (var listener : copiedListeners)
            {
                try
                {
                    addSipListener(listener);
                }
                catch (OperationFailedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Returns true if there are active calls in progress.
     */
    private boolean hasCallsInProgress()
    {
        return getInProgressCalls().size() != 0;
    }

    /**
     * Signals that the SIP stack can be reset by the next CompletionStage in
     * the pipeline this method is called from if there are no active calls for
     * this user; if a call is in progress, schedule a retry to check
     * the call status again after a delay.
     * @param callsInProgress true if there are any active calls
     * @return a completed CompletableFuture that signals that an immediate
     * reset of the SIP stack can occur, or an incomplete CompletableFuture
     * that will retry the check on the number of active calls
     * after a given delay.
     */
    private CompletableFuture<Void> scheduleSipStackReset(
            final boolean callsInProgress)
    {
        if (!callsInProgress)
        {
            return CompletableFuture.completedFuture(null);
        }
        final long SIP_STACK_RESET_DELAY = 5L;

        return CompletableFuture.supplyAsync(this::hasCallsInProgress,
                                             delayedExecutor(SIP_STACK_RESET_DELAY,
                                                             TimeUnit.MINUTES,
                                                             reloadExecutor))
                .thenCompose(this::scheduleSipStackReset);
    }

    /**
     * This <tt>listener</tt> will no longer be a candidate recipient for the
     * dispatching of new messages received from the JAIN-SIP
     * <tt>SipProvider</tt>s.
     *
     * @param listener possible target to remove for the dispatching process.
     */
    public void removeSipListener(ProtocolProviderServiceSipImpl listener)
    {
        boolean noMoreListeners;
        synchronized (listenerLock)
        {
            listeners.remove(listener);

            int listenerCount = listeners.size();
            noMoreListeners = listenerCount == 0;
            logger.trace(listenerCount + " listeners left");
        }

        if(noMoreListeners)
        {
            stopListening();
        }
    }

    /**
     * Returns a copy of the <tt>listeners</tt> (= candidate recipients) set.
     *
     * @return a copy of the <tt>listeners</tt> set.
     */
    private Set<ProtocolProviderServiceSipImpl> getSipListeners()
    {
        synchronized (listenerLock)
        {
            return new HashSet<>(listeners);
        }
    }

    /**
     * Returns the JAIN-SIP <tt>ListeningPoint</tt> associated to the given
     * transport string.
     *
     * @param transport a string like "UDP", "TCP" or "TLS".
     * @return the LP associated to the given transport.
     */
    public ListeningPoint getLP(String transport)
    {
        ListeningPoint lp;
        Iterator<? extends ListeningPoint> it = stack.getListeningPoints();

        if(!it.hasNext())
        {
            throw new IllegalStateException("No available listening points");
        }

        while(it.hasNext())
        {
            lp = it.next();
            // FIXME: JAIN-SIP stack is not consistent with case
            // (reported upstream)
            if(lp.getTransport().equalsIgnoreCase(transport))
                return lp;
        }

        throw new IllegalArgumentException("Invalid transport: " + transport);
    }

    /**
     * Put the stack in a state where it can receive data on three UDP/TCP ports
     * (2 for clear communication, 1 for TLS). That is to say create the related
     * JAIN-SIP <tt>ListeningPoint</tt>s and <tt>SipProvider</tt>s.
     *
     * @throws OperationFailedException if creating one of the underlying
     * <tt>SipProvider</tt>s fails for whatever reason.
     */
    private void startListening()
        throws OperationFailedException
    {
        try
        {
            int bindRetriesValue = getBindRetriesValue();

            createProvider(getPreferredClearPort(),
                           bindRetriesValue, false);
            createProvider(getPreferredSecurePort(),
                           bindRetriesValue, true);
            stack.start();
            logger.trace("started listening");
        }
        catch(Exception ex)
        {
            logger.error("An unexpected error happened while creating the SipProviders and ListeningPoints.", ex);
            throw new OperationFailedException("An unexpected error happened "
                    + "while initializing the SIP stack"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
        }
    }

    /**
     * Attach JAIN-SIP <tt>SipProvider</tt> and <tt>ListeningPoint</tt> to the
     * stack either for clear communications or TLS. Clear UDP and TCP
     * <tt>ListeningPoint</tt>s are not handled separately as the former is a
     * fallback for the latter (depending on the size of the data transmitted).
     * Both <tt>ListeningPoint</tt>s must be bound to the same address and port
     * in order for the related <tt>SipProvider</tt> to be created. If a UDP or
     * TCP <tt>ListeningPoint</tt> cannot bind, retry for both on another port.
     *
     * @param preferredPort which port to try first to bind.
     * @param retries how many times should we try to find a free port to bind
     * @param secure whether to create the TLS SipProvider.
     * or the clear UDP/TCP one.
     *
     * @throws TransportNotSupportedException in case we try to create a
     * provider for a transport not currently supported by jain-sip
     * @throws InvalidArgumentException if we try binding to an illegal port
     * (which we won't)
     * @throws ObjectInUseException if another <tt>SipProvider</tt> is already
     * associated with this <tt>ListeningPoint</tt>.
     * @throws TransportAlreadySupportedException if there is already a
     * ListeningPoint associated to this <tt>SipProvider</tt> with the same
     * transport of the <tt>ListeningPoint</tt>.
     * @throws TooManyListenersException if we try to add a new
     * <tt>SipListener</tt> with a <tt>SipProvider</tt> when one was already
     * registered.
     *
     */
    private void createProvider(int preferredPort, int retries, boolean secure)
        throws TransportNotSupportedException,
        InvalidArgumentException,
        ObjectInUseException,
        TransportAlreadySupportedException,
        TooManyListenersException
    {
        String context = (secure ? "TLS: " : "clear UDP/TCP: ");

        if(retries < 0)
        {
            // very unlikely to happen with the default 50 retries
            logger.error(context + " couldn't find free ports to listen on.");
            return;
        }

        ListeningPoint tlsLP;
        ListeningPoint udpLP = null;
        ListeningPoint tcpLP;

        try
        {
            if(secure)
            {
                tlsLP = stack.createListeningPoint(
                        NetworkUtils.IN_ADDR_ANY
                        , preferredPort
                        , ListeningPoint.TLS);
                logger.trace("TLS secure ListeningPoint has been created.");

                secureJainSipProvider = stack.createSipProvider(tlsLP);
                secureJainSipProvider.addSipListener(this);
            }
            else
            {
                udpLP = stack.createListeningPoint(
                        NetworkUtils.IN_ADDR_ANY
                        , preferredPort
                        , ListeningPoint.UDP);
                tcpLP = stack.createListeningPoint(
                        NetworkUtils.IN_ADDR_ANY
                        , preferredPort
                        , ListeningPoint.TCP);
                logger.trace("UDP and TCP clear ListeningPoints have "
                            + "been created.");

                clearJainSipProvider = stack.createSipProvider(udpLP);
                clearJainSipProvider.addListeningPoint(tcpLP);
                clearJainSipProvider.addSipListener(this);
            }

            logger.trace(context + "SipProvider has been created.");
        }
        catch (InvalidArgumentException ex)
        {
            // makes sure we didn't leave an open listener
            // as both UDP and TCP listener have to bind to the same port
            if (udpLP != null)
            {
                stack.deleteListeningPoint(udpLP);
            }

            // FIXME: "Address already in use" is not working
            // as ex.getMessage() displays in the locale language in SC
            // (getMessage() is always supposed to be English though)
            // this should be a temporary workaround
            //if (ex.getMessage().indexOf("Address already in use") != -1)
            // another software is probably using the port
            if(ex.getCause() instanceof java.io.IOException)
            {
                logger.debug("Port " + preferredPort
                        + " seems in use for either TCP or UDP.");

                // tries again on a new random port
                int currentlyTriedPort = NetworkUtils.getRandomPortNumber();
                logger.debug("Retrying bind on port " + currentlyTriedPort);
                createProvider(currentlyTriedPort, retries-1, secure);
            }
            else
                throw ex;
        }
    }

    /**
     * Put the JAIN-SIP stack in a state where it cannot receive any data and
     * frees the network ports used. That is to say remove JAIN-SIP
     * <tt>ListeningPoint</tt>s and <tt>SipProvider</tt>s.
     */
    private void stopListening()
    {
        try
        {
            if (stack != null)
            {
                if (secureJainSipProvider != null)
                {
                    secureJainSipProvider.removeSipListener(this);
                    stack.deleteSipProvider(secureJainSipProvider);
                    secureJainSipProvider = null;
                }
                else
                {
                    logger.warn("Failed to remove secureJainSipProvider as already null.");
                }

                if (clearJainSipProvider != null)
                {
                    clearJainSipProvider.removeSipListener(this);
                    stack.deleteSipProvider(clearJainSipProvider);
                    clearJainSipProvider = null;
                }
                else
                {
                    logger.warn("Failed to remove clearJainSipProvider as already null.");
                }

                Iterator<? extends ListeningPoint> it = stack.getListeningPoints();
                if (it != null)
                {
                    Vector<ListeningPoint> lpointsToRemove = new Vector<>();
                    while (it.hasNext())
                    {
                        lpointsToRemove.add(it.next());
                    }

                    it = lpointsToRemove.iterator();
                    while (it.hasNext())
                    {
                        stack.deleteListeningPoint(it.next());
                    }
                }
                else
                {
                    logger.warn("Failed to remove ListeningPoints as already null.");
                }

                stack.stop();
            }
            else
            {
                logger.warn("Failed to stop listening - stack already null.");
            }

            logger.trace("stopped listening");
        }
        catch(ObjectInUseException ex)
        {
            logger.fatal("Failed to stop listening", ex);
        }
    }

    /**
     * Returns the JAIN-SIP <tt>SipProvider</tt> in charge of this
     * <tt>transport</tt>.
     *
     * @param transport a <tt>String</tt> like "TCP", "UDP" or "TLS"
     * @return the corresponding <tt>SipProvider</tt>
     *
     * @throws IllegalArgumentException if an invalid transport is provided or if there is no available sip provider
     */
    public SipProvider getJainSipProvider(String transport)
    {
        if(!isValidTransport(transport))
        {
            throw new IllegalArgumentException("Invalid transport " + transport);
        }

        SipProvider sp = null;

        if(transport.equalsIgnoreCase(ListeningPoint.UDP) || transport.equalsIgnoreCase(ListeningPoint.TCP))
        {
            sp = clearJainSipProvider;
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TLS))
        {
            sp = secureJainSipProvider;
        }

        if(sp == null)
        {
            throw new IllegalArgumentException("There is no available sip provider. This is probably due to a lost network connection.");
        }

        return sp;
    }

    private boolean isValidTransport(String transport) {
        return ListeningPoint.TLS.equalsIgnoreCase(transport)
               || ListeningPoint.TCP.equalsIgnoreCase(transport)
               || ListeningPoint.UDP.equalsIgnoreCase(transport);
    }

    /**
     * Fetches the preferred UDP and TCP port for clear communications in the
     * user preferences or search is default value set in settings or
     * fallback on a default value.
     *
     * @return the preferred network port for clear communications.
     */
    private int getPreferredClearPort()
    {
        boolean useRandomPort = SipActivator.getConfigurationService().global().getBoolean(
            RANDOM_CLEAR_PORT_PROPERTY_NAME, false);

        if(useRandomPort)
        {
            int port = NetworkUtils.getRandomPortNumber();
            logger.debug("Using random port: " + port);
            return port;
        }
        else
        {
            int preferredPort = SipActivator.getConfigurationService().global().getInt(
                PREFERRED_CLEAR_PORT_PROPERTY_NAME, -1);

            if(preferredPort <= 1)
            {
                // check for default value
                preferredPort = SipActivator.getResources().getSettingsInt(
                    PREFERRED_CLEAR_PORT_PROPERTY_NAME);
            }

            if(preferredPort <= 1)
            {
                logger.debug("Using port: " + ListeningPoint.PORT_5060);
                return ListeningPoint.PORT_5060;
            }
            else
            {
                logger.debug("Using preferred port " + preferredPort);
                return preferredPort;
            }
        }
    }

    /**
     * Fetches the preferred TLS (TCP) port for secure communications in the
     * user preferences or search is default value set in settings or
     * fallback on a default value.
     *
     * @return the preferred network port for secure communications.
     */
    private int getPreferredSecurePort()
    {
        boolean useRandomPort = SipActivator.getConfigurationService().global().getBoolean(
            RANDOM_SECURE_PORT_PROPERTY_NAME, false);

        if(useRandomPort)
        {
            int port = NetworkUtils.getRandomPortNumber();
            logger.debug("Using random port: " + port);
            return port;
        }
        else
        {
            int preferredPort = SipActivator.getConfigurationService().global().getInt(
                PREFERRED_CLEAR_PORT_PROPERTY_NAME, -1);

            if(preferredPort <= 1)
            {
                // check for default value
                preferredPort = SipActivator.getResources().getSettingsInt(
                    PREFERRED_CLEAR_PORT_PROPERTY_NAME);
            }

            if(preferredPort <= 1)
            {
                logger.debug("Using port: " + ListeningPoint.PORT_5060);
                return ListeningPoint.PORT_5060;
            }
            else
            {
                logger.debug("Using preferred port " + preferredPort);
                return preferredPort;
            }
        }
    }

    /**
     * Fetches the number of times to retry when the binding of a JAIN-SIP
     * <tt>ListeningPoint</tt> fails. Looks in the user preferences or
     * fallbacks on a default value.
     *
     * @return the number of times to retry a failed bind.
     */
    private int getBindRetriesValue()
    {
        return SipActivator.getConfigurationService().global().getInt(
            ProtocolProviderService.BIND_RETRIES_PROPERTY_NAME,
            ProtocolProviderService.BIND_RETRIES_DEFAULT_VALUE);
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a
     * <tt>SipProvider</tt>.
     */
    public void processDialogTerminated(DialogTerminatedEvent event)
    {
        try
        {
            ProtocolProviderServiceSipImpl recipient
                = (ProtocolProviderServiceSipImpl) SipApplicationData
                    .getApplicationData(event.getDialog(),
                                        SipApplicationData.KEY_SERVICE);
            if(recipient == null)
            {
                logger.error("Dialog wasn't marked, please report this to "
                                + "dev@sip-communicator.dev.java.net");
            }
            else
            {
                logger.trace("service was found with dialog data");
                recipient.processDialogTerminated(event);
            }
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a <tt>SipProvider</tt>.
     */
    public void processIOException(IOExceptionEvent event)
    {
        try
        {
            logger.trace(event);

            // impossible to dispatch, log here
            logger.debug("@todo implement processIOException()");
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a <tt>SipProvider</tt>.
     */
    public void processRequest(RequestEvent event)
    {
        try
        {
            Request request = event.getRequest();
            logger.trace("received request: " + request.getMethod());

            /*
             * Create the transaction if it doesn't exist yet. If it is a
             * dialog-creating request, the dialog will also be automatically
             * created by the stack.
             */
            if (event.getServerTransaction() == null)
            {
                try
                {
                    // apply some hacks if needed on incoming request
                    // to be compliant with some servers/clients
                    // if needed stop further processing.
                    if(applyNonConformanceHacks(event))
                        return;

                    SipProvider source = (SipProvider) event.getSource();
                    ServerTransaction transaction
                        = source.getNewServerTransaction(request);

                    /*
                     * Update the event, otherwise getServerTransaction() and
                     * getDialog() will still return their previous value.
                     */
                    event
                        = new RequestEvent(
                                source,
                                transaction,
                                transaction.getDialog(),
                                request);
                }
                catch (SipException ex)
                {
                    logger.error(
                        "couldn't create transaction, please report "
                            + "this to dev@sip-communicator.dev.java.net",
                        ex);
                }
            }

            ProtocolProviderServiceSipImpl service
                = getServiceData(event.getServerTransaction());
            if (service != null)
            {
                service.processRequest(event);
            }
            else
            {
                service = findTargetFor(request);
                if (service == null)
                {
                    logger.error(
                        "couldn't find a ProtocolProviderServiceSipImpl "
                            + "to dispatch to");
                    if (event.getServerTransaction() != null)
                        event.getServerTransaction().terminate();
                }
                else
                {
                    /*
                     * Mark the dialog for the dispatching of later in-dialog
                     * requests. If there is no dialog, we need to mark the
                     * request to dispatch a possible timeout when sending the
                     * response.
                     */
                    Object container = event.getDialog();
                    if (container == null)
                        container = request;
                    SipApplicationData.setApplicationData(
                        container,
                        SipApplicationData.KEY_SERVICE,
                        service);

                    service.processRequest(event);
                }
            }
        }
        catch(Throwable exc)
        {
            /*
             * Any exception thrown within our code should be caught here so
             * that we could log it rather than interrupt stack activity with
             * it.
             */
            logApplicationException(DialogTerminatedEvent.class, exc);

            // Unfortunately, death can hardly be ignored.
            if (exc instanceof ThreadDeath)
                throw (ThreadDeath) exc;
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a <tt>SipProvider</tt>.
     */
    public void processResponse(ResponseEvent event)
    {
        try
        {
            // we don't have to accept the transaction since we
            //created the request
            ClientTransaction transaction = event.getClientTransaction();
            logger.trace("received response: "
                        + event.getResponse().getStatusCode()
                        + " " + event.getResponse().getReasonPhrase());

            if(transaction == null)
            {
                logger.warn("Transaction is null, probably already expired!");
                return;
            }

            ProtocolProviderServiceSipImpl service
                = getServiceData(transaction);
            if (service != null)
            {
                // Mark the dialog for the dispatching of later in-dialog
                // responses. If there is no dialog then the initial request
                // sure is marked otherwise we won't have found the service with
                // getServiceData(). The request has to be marked in case we
                // receive one more response in an out-of-dialog transaction.
                if (event.getDialog() != null)
                {
                    SipApplicationData.setApplicationData(event.getDialog(),
                                    SipApplicationData.KEY_SERVICE, service);
                }
                service.processResponse(event);
            }
            else
            {
                logger.error("We received a response which "
                                + "wasn't marked, please report this to "
                                + "dev@sip-communicator.dev.java.net");
            }
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a <tt>SipProvider</tt>.
     */
    public void processTimeout(TimeoutEvent event)
    {
        try
        {
            Transaction transaction;
            if (event.isServerTransaction())
            {
                transaction = event.getServerTransaction();
            }
            else
            {
                transaction = event.getClientTransaction();
            }

            ProtocolProviderServiceSipImpl recipient
                = getServiceData(transaction);
            if (recipient == null)
            {
                logger.error("We received a timeout which wasn't "
                                + "marked, please report this to "
                                + "dev@sip-communicator.dev.java.net");
            }
            else
            {
                recipient.processTimeout(event);
            }
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a
     * <tt>SipProvider</tt>.
     */
    public void processTransactionTerminated(TransactionTerminatedEvent event)
    {
        try
        {
            Transaction transaction;
            if (event.isServerTransaction())
                transaction = event.getServerTransaction();
            else
                transaction = event.getClientTransaction();

            ProtocolProviderServiceSipImpl recipient
                = getServiceData(transaction);

            if (recipient == null)
            {
                // We received a transaction terminated which wasn't marked.
                // We believe that this is benign, and we should just ignore it.
            }
            else
            {
                recipient.processTransactionTerminated(event);
            }
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Find the <tt>ProtocolProviderServiceSipImpl</tt> (one of our
     * "candidate recipient" listeners) which this <tt>request</tt> should be
     * dispatched to. The strategy is to look first at the request URI, and
     * then at the To field to find a matching candidate for dispatching.
     * Note that this method takes a <tt>Request</tt> as param, and not a
     * <tt>ServerTransaction</tt>, because sometimes <tt>RequestEvent</tt>s
     * have no associated <tt>ServerTransaction</tt>.
     *
     * @param request the <tt>Request</tt> to find a recipient for.
     * @return a suitable <tt>ProtocolProviderServiceSipImpl</tt>.
     */
    @VisibleForTesting
    ProtocolProviderServiceSipImpl findTargetFor(Request request)
    {
        if (request == null)
        {
            logger.error("request shouldn't be null.");
            return null;
        }

        List<ProtocolProviderServiceSipImpl> currentListenersCopy
            = new ArrayList<>(
                getSipListeners());

        // Let's first narrow down candidate choice by comparing
        // addresses and ports (no point in delivering to a provider with a
        // non matching IP address  since they will reject it anyway).
        filterByAddress(currentListenersCopy, request);

        if (currentListenersCopy.size() == 0)
        {
            logger.error("no listeners matching address");
            return null;
        }

        // Remove any choices that aren't registered.
        currentListenersCopy.removeIf(pps -> !pps.isRegistered());

        if (currentListenersCopy.size() == 0)
        {
            logger.error("no registered listeners");
            return null;
        }

        URI requestURI = request.getRequestURI();

        if(requestURI.isSipURI())
        {
            String requestUser = ((SipURI) requestURI).getUser();

            List<ProtocolProviderServiceSipImpl> candidates =
                    new ArrayList<>();

            // check if the Request-URI username is
            // one of ours usernames
            for(ProtocolProviderServiceSipImpl listener : currentListenersCopy)
            {
                String ourUserID = listener.getAccountID().getUserID();
                //logger.trace(ourUserID + " *** " + requestUser);
                if(ourUserID.equals(requestUser))
                {
                    logger.trace("suitable candidate found: "
                                + listener.getAccountID());
                    candidates.add(listener);
                }
            }

            // the perfect match
            // every other case is approximation
            if(candidates.size() == 1)
            {
                ProtocolProviderServiceSipImpl perfectMatch = candidates.get(0);

                logger.trace("Will dispatch to \""
                            + perfectMatch.getAccountID() + "\"");
                return perfectMatch;
            }

            // more than one account match
            if(candidates.size() > 1)
            {
                // check if a custom param exists in the contact
                // address (set for registrar accounts)
                for (ProtocolProviderServiceSipImpl candidate : candidates)
                {
                    String hostValue = ((SipURI) requestURI).getParameter(
                            SipStackSharing.CONTACT_ADDRESS_CUSTOM_PARAM_NAME);
                    if (hostValue == null)
                        continue;
                    if (hostValue.equals(candidate
                                .getContactAddressCustomParamValue()))
                    {
                        logger.trace("Will dispatch to \""
                                    + candidate.getAccountID() + "\" because "
                                    + "\" the custom param was set");
                        return candidate;
                    }
                }

                // Past this point, our guess is not reliable. We try to find
                // the "least worst" match based on parameters like the To field

                // check if the To header field host part
                // matches any of our SIP hosts
                for(ProtocolProviderServiceSipImpl candidate : candidates)
                {
                    URI fromURI = ((FromHeader) request
                            .getHeader(FromHeader.NAME)).getAddress().getURI();
                    if(!fromURI.isSipURI())
                        continue;
                    SipURI ourURI = (SipURI) candidate
                        .getOurSipAddress((SipURI) fromURI).getURI();
                    String ourHost = ourURI.getHost();

                    URI toURI = ((ToHeader) request
                            .getHeader(ToHeader.NAME)).getAddress().getURI();
                    if(!toURI.isSipURI())
                        continue;
                    String toHost = ((SipURI) toURI).getHost();

                    //logger.trace(toHost + "***" + ourHost);
                    if(toHost.equals(ourHost))
                    {
                        logger.trace("Will dispatch to \""
                                    + candidate.getAccountID() + "\" because "
                                    + "host in the To: is the same as in our AOR");
                        return candidate;
                    }
                }

                // fallback on the first candidate
                ProtocolProviderServiceSipImpl target =
                    candidates.iterator().next();
                logger.info("Will randomly dispatch to \""
                        + target.getAccountID()
                        + "\" because there is ambiguity on the username from"
                        + " the Request-URI");
                logger.trace("\n" + request);
                return target;
            }

            // fallback on any account
            ProtocolProviderServiceSipImpl target =
                currentListenersCopy.iterator().next();
            logger.debug("Will randomly dispatch to \"" + target
                    .getAccountID()
                    + "\" because the username in the Request-URI "
                    + "is unknown or empty");
            logger.trace("\n" + request);
            return target;
        }
        else
        {
            logger.error("Request-URI is not a SIP URI, dropping");
        }
        return null;
    }

    /**
     * Removes from the specified list of candidates providers connected to a
     * registrar that does not match the IP address that we are receiving a
     * request from.
     *
     * @param candidates the list of providers we've like to filter.
     * @param request the request that we are currently dispatching
     */
    private void filterByAddress(
                    List<ProtocolProviderServiceSipImpl> candidates,
                    Request                              request)
    {
        Iterator<ProtocolProviderServiceSipImpl> iterPP = candidates.iterator();
        while (iterPP.hasNext())
        {
            ProtocolProviderServiceSipImpl candidate = iterPP.next();

            if(candidate.getRegistrarConnection() == null)
            {
                //RegistrarLess connections are ok
                continue;
            }

            if (   !candidate.getRegistrarConnection().isRegistrarless()
                && !candidate.getRegistrarConnection()
                        .isRequestFromSameConnection(request))
            {
                iterPP.remove();
            }
        }
    }

    /**
     * Retrieves and returns that ProtocolProviderService that this transaction
     * belongs to, or <tt>null</tt> if we couldn't associate it with a provider
     * based on neither the request nor the transaction itself.
     *
     * @param transaction the transaction that we'd like to determine a provider
     * for.
     *
     * @return a reference to the <tt>ProtocolProviderServiceSipImpl</tt> that
     * <tt>transaction</tt> was associated with or <tt>null</tt> if we couldn't
     * determine which one it is.
     */
    private ProtocolProviderServiceSipImpl
        getServiceData(Transaction transaction)
    {
        ProtocolProviderServiceSipImpl service
            = (ProtocolProviderServiceSipImpl) SipApplicationData
            .getApplicationData(transaction.getRequest(),
                    SipApplicationData.KEY_SERVICE);

        if (service != null)
        {
            logger.trace("service was found in request data");
            return service;
        }

        service = (ProtocolProviderServiceSipImpl) SipApplicationData
            .getApplicationData(transaction.getDialog(),
                    SipApplicationData.KEY_SERVICE);
        if (service != null)
        {
            logger.trace("service was found in dialog data");
        }

        return service;
    }

    /**
     * Logs exceptions that have occurred in the application while processing
     * events originating from the stack.
     *
     * @param eventClass the class of the jain-sip event that we were handling
     * when the exception was thrown.
     * @param exc the exception that we need to log.
     */
    private void logApplicationException(
        Class<DialogTerminatedEvent> eventClass,
        Throwable exc)
    {
        String message
            = "An error occurred while processing event of type: "
                + eventClass.getName();

        logger.error(message, exc);
    }

    /**
     * Safely returns the transaction from the event if already exists.
     * If not a new transaction is created.
     *
     * @param event the request event
     * @return the server transaction
     * @throws javax.sip.TransactionAlreadyExistsException if transaction exists
     * @throws javax.sip.TransactionUnavailableException if unavailable
     */
    public static ServerTransaction getOrCreateServerTransaction(
                                                            RequestEvent event)
        throws TransactionAlreadyExistsException,
               TransactionUnavailableException
    {
        ServerTransaction serverTransaction = event.getServerTransaction();

        if(serverTransaction == null)
        {
            SipProvider jainSipProvider = (SipProvider) event.getSource();

            serverTransaction
                = jainSipProvider
                    .getNewServerTransaction(event.getRequest());
        }
        return serverTransaction;
    }

    /**
     * Returns a local address to use with the specified TCP destination.
     * The method forces the JAIN-SIP stack to create
     * s and binds (if necessary)
     * and return a socket connected to the specified destination address and
     * port and then return its local address.
     *
     * @param dst the destination address that the socket would need to connect
     *            to.
     * @param dstPort the port number that the connection would be established
     * with.
     * @param localAddress the address that we would like to bind on
     * (null for the "any" address).
     * @param transport the transport that will be used TCP ot TLS
     *
     * @return the SocketAddress that this handler would use when connecting to
     * the specified destination address and port.
     *
     * @throws IOException  if we fail binding the local socket
     */
    public java.net.InetSocketAddress getLocalAddressForDestination(
                    java.net.InetAddress dst,
                    int                  dstPort,
                    java.net.InetAddress localAddress,
                    String transport)
        throws IOException
    {
        if(ListeningPoint.TLS.equalsIgnoreCase(transport))
            return (java.net.InetSocketAddress)(((SipStackImpl)stack)
                .getLocalAddressForTlsDst(dst, dstPort, localAddress));
        else
            return (java.net.InetSocketAddress)(((SipStackImpl)stack)
            .getLocalAddressForTcpDst(dst, dstPort, localAddress, 0));
    }

    /**
     * Place to put some hacks if needed on incoming requests.
     *
     * @param event the incoming request event.
     * @return status <code>true</code> if we don't need to process this
     * message, just discard it and <code>false</code> otherwise.
     */
    private boolean applyNonConformanceHacks(RequestEvent event)
    {
        Request request = event.getRequest();
        try
        {
            /*
             * Max-Forwards is required, yet there are UAs which do not
             * place it. SipProvider#getNewServerTransaction(Request)
             * will throw an exception in the case of a missing
             * Max-Forwards header and this method will eventually just
             * log it thus ignoring the whole event.
             */
            if (request.getHeader(MaxForwardsHeader.NAME) == null)
            {
                // it appears that some buggy providers do send requests
                // with no Max-Forwards headers, as we are at application level
                // and we know there will be no endless loops
                // there is no problem of adding headers and process normally
                // this messages
                MaxForwardsHeader maxForwards = SipFactory
                    .getInstance().createHeaderFactory()
                        .createMaxForwardsHeader(70);
                request.setHeader(maxForwards);
            }
        }
        catch(Throwable ex)
        {
            logger.warn("Cannot apply incoming request modification!", ex);
        }

        try
        {
            // using asterisk voice mail initial notify for messages
            // is ok, but on the fly received messages their notify comes
            // without subscription-state, so we add it in order to be able to
            // process message.
            if(request.getMethod().equals(Request.NOTIFY)
               && request.getHeader(EventHeader.NAME) != null
               && ((EventHeader)request.getHeader(EventHeader.NAME))
                    .getEventType().equals(
                        OperationSetMessageWaitingSipImpl.EVENT_PACKAGE)
               && request.getHeader(SubscriptionStateHeader.NAME)
                    == null)
            {
                request.addHeader(
                        new HeaderFactoryImpl()
                            .createSubscriptionStateHeader(
                                SubscriptionStateHeader.ACTIVE));
            }
        }
        catch(Throwable ex)
        {
            logger.warn("Cannot apply incoming request modification!", ex);
        }

        try
        {
            // receiving notify message without subscription state
            // used for keep-alive pings, they have done their job
            // and are no more need. Skip processing them to avoid
            // filling logs with unneeded exceptions.
            if(request.getMethod().equals(Request.NOTIFY)
               && request.getHeader(SubscriptionStateHeader.NAME) == null)
            {
                return true;
            }
        }
        catch(Throwable ex)
        {
            logger.warn("Cannot apply incoming request modification!", ex);
        }

        return false;
    }

    /**
     * List of currently waiting timers that will monitor the protocol provider
     *
     */
    private final Map<String, TimerTask> resetListeningPointsTimers
            = new HashMap<>();

    /**
     * Listens for network changes and if we have a down interface
     * and we have a tcp/tls provider which is staying for 20 seconds in
     * unregistering state, it cannot unregister cause its using the old
     * address which is currently down, and we must recreate its listening
     * points so it can further reconnect.
     *
     * @param event the change event.
     */
    public void configurationChanged(ChangeEvent event)
    {
        if (event.isInitial())
        {
            return;
        }

        if (event.getType() == ChangeEvent.ADDRESS_DOWN)
        {
            for (final ProtocolProviderServiceSipImpl pp : getSipListeners())
            {
                final String transport = pp.getRegistrarConnection().getTransport();

                if ((transport != null) &&
                    (transport.equals(ListeningPoint.TCP) || transport.equals(ListeningPoint.TLS)))
                {
                    ResetListeningPoint reseter;
                    synchronized (resetListeningPointsTimers)
                    {
                        logger.info("SipStackSharing resetting listening point for " + pp);
                        // we do this only once for transport
                        if (resetListeningPointsTimers.containsKey(transport))
                            continue;

                        reseter = new ResetListeningPoint(pp);
                        resetListeningPointsTimers.put(transport, reseter);
                    }
                    pp.addRegistrationStateChangeListener(reseter);
                }
            }
        }
    }

    /**
     * Sets the registrar for this SIP stack
     *
     * @param registrar the registrar for this SIP stack
     */
    public void setRegistrar(SipListener registrar)
    {
        // Set the registrar for each SIP stack provider
        Iterator<? extends SipProvider> sipProviders = stack.getSipProviders();
        while (sipProviders.hasNext())
        {
            SipProvider sipProvider = sipProviders.next();
            sipProvider.setRegistrar(registrar);
        }
    }

    /**
     * If a tcp(tls) provider stays unregistering for a long time after
     * connection changed most probably it won't get registered after
     * unregistering fails, cause underlying listening point are connected
     * to wrong interfaces. So we will replace them.
     */
    private class ResetListeningPoint
            extends TimerTask
            implements RegistrationStateChangeListener
    {
        /**
         * The time we wait before checking is the provider still unregistering.
         */
        private static final int TIME_FOR_PP_TO_UNREGISTER = 20000;

        /**
         * The protocol provider we are checking.
         */
        private final ProtocolProviderServiceSipImpl protocolProvider;

        /**
         * Constructs this task.
         * @param pp protocol provider
         */
        ResetListeningPoint(ProtocolProviderServiceSipImpl pp)
        {
            protocolProvider = pp;
        }

        /**
         * Notified when registration state changed for a provider.
         * @param evt event
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if(evt.getNewState() == RegistrationState.UNREGISTERING)
            {
                new Timer("Unregister timer").schedule(this, TIME_FOR_PP_TO_UNREGISTER);
            }
            else
            {
                protocolProvider.removeRegistrationStateChangeListener(this);
                resetListeningPointsTimers.remove(
                    protocolProvider.getRegistrarConnection().getTransport());
            }
        }

        /**
         * The real task work, replace listening point.
         */
        public void run()
        {
            // if the provider is still unregistering it most probably won't
            // successes until we re-init the LP
            if(protocolProvider.getRegistrationState()
                == RegistrationState.UNREGISTERING)
            {
                String transport = protocolProvider.getRegistrarConnection()
                    .getTransport();

                try
                {
                    ListeningPoint old = getLP(transport);
                    stack.deleteListeningPoint(old);
                }
                catch(Throwable t)
                {
                    logger.warn("Error replacing ListeningPoint for "
                            + transport, t);
                }

                try
                {
                    ListeningPoint tcpLP =
                        stack.createListeningPoint(
                            NetworkUtils.IN_ADDR_ANY
                            , transport.equals(ListeningPoint.TCP)?
                                getPreferredClearPort(): getPreferredSecurePort()
                            , transport);
                    clearJainSipProvider.addListeningPoint(tcpLP);
                }
                catch(Throwable t)
                {
                    logger.warn("Error replacing ListeningPoint for " +
                        protocolProvider.getRegistrarConnection().getTransport(),
                            t);
                }
            }

            resetListeningPointsTimers.remove(
                    protocolProvider.getRegistrarConnection().getTransport());
        }
    }
}
