/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.AlertInfoHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.core.NameValueList;
import gov.nist.core.net.AddressResolver;
import gov.nist.javax.sip.address.AddressFactoryEx;
import gov.nist.javax.sip.address.AddressFactoryImpl;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.HeaderFactoryImpl;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.ims.ServiceRoute;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.impl.protocol.sip.net.AutoProxyConnection;
import net.java.sip.communicator.impl.protocol.sip.net.ProxyConnection;
import net.java.sip.communicator.impl.protocol.sip.security.SecurityHeaderFactory;
import net.java.sip.communicator.impl.protocol.sip.security.SipSecurityManager;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog.OnDismiss;
import net.java.sip.communicator.plugin.provisioning.ProvisioningServiceImpl;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.calljump.CallJumpService;
import net.java.sip.communicator.service.commportal.CPCos;
import net.java.sip.communicator.service.commportal.CPCosGetterCallback;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.protocol.AbstractProtocolProviderService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetAdvancedAutoAnswer;
import net.java.sip.communicator.service.protocol.OperationSetAdvancedTelephony;
import net.java.sip.communicator.service.protocol.OperationSetAvatar;
import net.java.sip.communicator.service.protocol.OperationSetBasicAutoAnswer;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetCallPark;
import net.java.sip.communicator.service.protocol.OperationSetDTMF;
import net.java.sip.communicator.service.protocol.OperationSetIncomingDTMF;
import net.java.sip.communicator.service.protocol.OperationSetMessageWaiting;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetSecureSDesTelephony;
import net.java.sip.communicator.service.protocol.OperationSetSecureZrtpTelephony;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredAccountInfo;
import net.java.sip.communicator.service.protocol.OperationSetTelephonyConferencing;
import net.java.sip.communicator.service.protocol.OperationSetVideoTelephony;
import net.java.sip.communicator.service.protocol.ProtocolIcon;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.NetworkUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.service.version.Version;
import org.jitsi.util.StringUtils;

/**
 * A SIP implementation of the Protocol Provider Service.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 * @author Alan Kelly
 * @author Grigorii Balutsel
 */
public class ProtocolProviderServiceSipImpl
  extends AbstractProtocolProviderService
  implements SipListener,
             RegistrationStateChangeListener
{
    private static final Logger logger = Logger.getLogger(ProtocolProviderServiceSipImpl.class);
    private static final ConfigurationService configService = SipActivator.getConfigurationService();

    /**
     * The identifier of the account that this provider represents.
     */
    private AccountID accountID = null;

    /**
     * The regular expression matching characters we strip from the sip address.
     */
    private static final String IGNORED_CHARS = configService.user().getString(
        "net.java.sip.communicator.impl.protocol.sip.SIP_ADDRESS_IGNORE_REGEX");

    /**
     * The re-register delay (in s) after a failed TCP connection. This provides
     * Perimeta enough time to get into a good state
     */
    private static final int REREGISTER_DELAY = 10;

    /**
     * We use this to lock access to initialization.
     */
    private final Object initializationLock = new Object();

    /**
     * indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized = false;

    /**
     * A list of all events registered for this provider.
     */
    private final List<String> registeredEvents = new ArrayList<>();

    /**
     * The AddressFactory used to create URLs and Address objects.
     */
    private AddressFactoryEx addressFactory;

    /**
     * The HeaderFactory used to create SIP message headers.
     */
    private HeaderFactory headerFactory;

    /**
     * The Message Factory used to create SIP messages.
     */
    private SipMessageFactory messageFactory;

    /**
     * The SecurityHeaderFactory used to create 3GPP security headers and store
     * header responses for later use.
     */
    private SecurityHeaderFactory securityHeaderFactory;

    /**
      * The class in charge of event dispatching and managing common JAIN-SIP
      * resources
      */
    private static SipStackSharing sipStackSharing = null;

    /**
     * Class holding information on the current reregister.  The purpose
     * is to re-invite any calls in case they can be saved following a
     * Perimeta SPS.
     */
    private WaitingForReregister waitingForReregister = null;

    /**
     * Time in ms to retry reregisters after a connection failure.
     *
     * This is because it can take that long before the Perimeta will
     * accept TLS connections.  Meanwhile the CFS will shoot the call
     * (depending on config) after slightly longer than 5 minutes.
     */
    private static final long REREGISTER_RETRY_TIME = 5 * 60 * 1000;

    /**
     * A table mapping SIP methods to method processors (every processor must
     * implement the SipListener interface). Whenever a new message arrives we
     * extract its method and hand it to the processor instance registered
     */
    private final Hashtable<String, List<MethodProcessor>> methodProcessors =
            new Hashtable<>();

    /**
     * The name of the property under which the user may specify a transport
     * to use for destinations whose preferred transport is unknown.
     */
    private static final String DEFAULT_TRANSPORT
        = "net.java.sip.communicator.impl.protocol.sip.DEFAULT_TRANSPORT";

    /**
     * Name for the property determining whether the client is set up to add
     * 3GPP-spec mediasec headers.
     */
    private static final String PROPERTY_3GPP_MEDIA_HEADERS =
            "net.java.sip.communicator.ENABLE_3GPP_MEDIA_HEADERS";

    /**
     * Name for the property determining whether the client is set up to use
     * SRTP for media.
     */
    private static final String PROPERTY_SRTP_ENABLED =
            "net.java.sip.communicator.ENABLE_AUDIO_SRTP";

    /**
     * Default number of times that our requests can be forwarded.
     */
    private static final int  MAX_FORWARDS = 70;

    /**
     * The default maxForwards header that we use in our requests.
     */
    private MaxForwardsHeader maxForwardsHeader = null;

    /**
     * The header that we use to identify ourselves.
     */
    private UserAgentHeader userAgentHeader = null;

    /**
     * The name that we want to send others when calling or chatting with them.
     */
    private String ourDisplayName = null;

    /**
     * Our current connection with the registrar.
     */
    private SipRegistrarConnection sipRegistrarConnection = null;

    /**
     * The SipSecurityManager instance that would be taking care of our
     * authentications.
     */
    private SipSecurityManager sipSecurityManager = null;

    /**
     * Address resolver for the outbound proxy connection.
     */
    private ProxyConnection connection;

    /**
     * The logo corresponding to the jabber protocol.
     */
    private ProtocolIconSipImpl protocolIcon;

    /**
     * The presence status set supported by this provider
     */
    private SipStatusEnum sipStatusEnum;

    private AddressResolver mAddressResolver;

    /**
     * A list of early processors that can do early processing of received
     * messages (requests or responses).
     */
    private final List<SipMessageProcessor> earlyProcessors =
            new ArrayList<>();

    /**
     * Whether we has enabled FORCE_PROXY_BYPASS for current account.
     */
    private boolean forceLooseRouting = false;

    /**
     * If we are asked to re-register, this will be set to true if our local
     * IP has changed since we last registered.  Currently this is used to
     * determine whether we need to re-subscribe to SIP presence notifications
     * immediately after re-registering.
     */
    private boolean mRouteToRegistrarChanged = false;

    /**
     * Lock object used to synchronise access to the above boolean and the
     * presence re-subscriptions that it may trigger, to avoid unnecessary
     * re-subscriptions.
     */
    private final Object mReSubscribeLock = new Object();

    /**
     * Whether the client is adding/receiving 3GPP mediasec headers in SIP
     * messages. Store as Boolean rather than boolean so we can detect whether
     * we've queried config yet.
     */
    private Boolean is3GPPMediaSecurityEnabled = null;
    private final ThreadingService threadingService = SipActivator.getThreadingService();

    /**
     * Returns the AccountID that uniquely identifies the account represented by
     * this instance of the ProtocolProviderService.
     * @return the id of the account represented by this provider.
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * Returns the Directory Number for the account represented by this
     * instance of the ProtocolProviderService
     *
     * @return the DN of the account represented by this provider.
     */
    public String getDirectoryNumber()
    {
        return getAccountID().getAccountPropertyString(
                          OperationSetBasicTelephony.SIP_DIRECTORY_NUMBER_PROP);
    }

    /**
     * Returns the state of the registration of this protocol provider with the
     * corresponding registration service.
     * @return ProviderRegistrationState
     */
    public RegistrationState getRegistrationState()
    {
        if(this.sipRegistrarConnection == null )
        {
            return RegistrationState.UNREGISTERED;
        }
        return sipRegistrarConnection.getRegistrationState();
    }

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM,  or others for
     * example). If the name of the protocol has been enumerated in
     * ProtocolNames then the value returned by this method must be the same as
     * the one in ProtocolNames.
     * @return a String containing the short name of the protocol this service
     * is implementing (most often that would be a name in ProtocolNames).
     */
    public String getProtocolName()
    {
        return ProtocolNames.SIP;
    }

    /**
     * Register a new event taken in account by this provider. This is useful
     * to generate the Allow-Events header of the OPTIONS responses and to
     * generate 489 responses.
     *
     * @param event The event to register
     */
    public void registerEvent(String event)
    {
        synchronized (this.registeredEvents)
        {
            if (!this.registeredEvents.contains(event))
            {
                this.registeredEvents.add(event);
            }
        }
    }

    /**
     * Returns the list of all the registered events for this provider.
     *
     * @return The list of all the registered events
     */
    public List<String> getKnownEventsList()
    {
        return this.registeredEvents;
    }

    /**
     * Starts the registration process. Connection details such as
     * registration server, user name/number are provided through the
     * configuration service through implementation specific properties.
     *
     * @param authority the security authority that will be used for resolving
     *        any security challenges that may be returned during the
     *        registration or at any moment while we're registered.
     *
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    public void register(SecurityAuthority authority)
        throws OperationFailedException
    {
        logger.info("Register " + this);

        // If phone service is disabled, we do not want to perform a SIP register with the
        // CFS/Perimeta as that will unnecessarily use resources and reduce scale; nor would we have
        // the SIP credentials to do so if we wished to.
        if (!ConfigurationUtils.isPhoneServiceEnabled())
        {
            logger.info("No phone service so no SIP register");
            return;
        }

        if(!isInitialized)
        {
            throw new OperationFailedException(
                "Provider must be initialized before being able to register."
                , OperationFailedException.GENERAL_ERROR);
        }

        if (isRegistered() ||
            getRegistrationState().equals(RegistrationState.REGISTERING))
        {
            return;
        }

         // Evaluate whether FORCE_PROXY_BYPASS is enabled for the account
         // before registering.
        forceLooseRouting = Boolean.parseBoolean((String)
                getAccountID().getAccountProperty(
                    ProtocolProviderFactory.FORCE_PROXY_BYPASS));

        sipStackSharing.addSipListener(this);
        // be warned when we will unregister, so that we can
        // then remove us as SipListener
        this.addRegistrationStateChangeListener(this);

        // Enable the user name modification. Setting this property to true
        // we'll allow the user to change the user name stored in the given
        //authority.
        authority.setUserNameEditable(true);

        //init the security manager before doing the actual registration to
        //avoid being asked for credentials before being ready to provide them
        sipSecurityManager.setSecurityAuthority(authority);

        initRegistrarConnection();

        //connect to the Registrar.
        connection = ProxyConnection.create(this);
        if (!registerUsingNextAddress())
        {
            logger.error("No address found for " + this);
            fireRegistrationStateChanged(
                RegistrationState.REGISTERING,
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                "Invalid or inaccessible server address.");
        }

        // Set the registering entity to allow us to use it for re-registering
        sipStackSharing.setRegistrar(this);
    }

    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     *
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    public void unregister()
        throws OperationFailedException
    {
        logger.info("Unregister " + this);

        if(getRegistrationState().equals(RegistrationState.UNREGISTERED)
            || getRegistrationState().equals(RegistrationState.UNREGISTERING)
            || getRegistrationState().equals(RegistrationState.CONNECTION_FAILED))
        {
            return;
        }

        sipRegistrarConnection.unregister();
        sipSecurityManager.setSecurityAuthority(null);
    }

    /**
     * Initializes the service implementation, and puts it in a state where it
     * could interoperate with other services.
     *
     * @param sipAddress the account id/uin/screenname of the account that we're
     * about to create
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     *
     * @throws OperationFailedException with code INTERNAL_ERROR if we fail
     * initializing the SIP Stack.
     * @throws java.lang.IllegalArgumentException if one or more of the account
     * properties have invalid values.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String             sipAddress,
                              final SipAccountID accountID)
        throws OperationFailedException, IllegalArgumentException
    {
        synchronized (initializationLock)
        {
            logger.debug("Initializing ProtocolProviderService for account ID "
                         + accountID.getLoggableAccountID());

            this.accountID = accountID;

            String protocolIconPath =
                accountID
                    .getAccountPropertyString(ProtocolProviderFactory.PROTOCOL_ICON_PATH);

            if (protocolIconPath == null)
                protocolIconPath = "resources/images/protocol/sip";

            this.protocolIcon = new ProtocolIconSipImpl(protocolIconPath);

            this.sipStatusEnum = new SipStatusEnum(protocolIconPath);

            // set our custom address resolver managing SRV records
            mAddressResolver = new AddressResolverImpl();

            if (sipStackSharing == null)
            {
                sipStackSharing = new SipStackSharing(mAddressResolver);
            }

            //create SIP factories.
            headerFactory = new HeaderFactoryImpl();
            addressFactory = new AddressFactoryImpl();
            securityHeaderFactory = new SecurityHeaderFactory();

            //initialize our display name
            ourDisplayName = accountID.getAccountPropertyString(
                                    ProtocolProviderFactory.DISPLAY_NAME);

            if(ourDisplayName == null
               || ourDisplayName.trim().length() == 0)
            {
                ourDisplayName = accountID.getUserID();
            }

            //initialize our OPTIONS handler
            new ClientCapabilities(this);

            //init the security manager
            sipSecurityManager = new SipSecurityManager(accountID, this);
            sipSecurityManager.setHeaderFactory(headerFactory);
            sipSecurityManager.setSecurityHeaderFactory(securityHeaderFactory);

            // Create our call processor. Note creating this adds it to the methodProcessors, though
            // we don't necessarily add this OpSet below (if client is CTD only), which is probably
            // bad.
            final OperationSetBasicTelephonySipImpl opSetBasicTelephonySipImpl
                = new OperationSetBasicTelephonySipImpl(this);

            // Finally, get the class of service, so that we know whether or not to register the
            // telephony, presence and MWI op sets.
            // Warning - despite passing in a callback here, the function doesn't return until the
            // CoS has been received and parsed by the callback function.
            ClassOfServiceService cos = SipActivator.getClassOfServiceService();
            cos.getClassOfService(new CPCosGetterCallback()
            {
                // Track whether we've ever received the CoS yet.
                private boolean cosAlreadyReceived = false;

                // Initialize the previous value for whether calling is
                // allowed to false.
                private boolean oldCallingAllowed = false;

                // The resources service.
                ResourceManagementService res = SipActivator.getResources();

                @Override
                public void onCosReceived(CPCos classOfService)
                {
                    logger.info("Class of service received");

                    // Initialize a string to contain a message to display to
                    // the user if calling has been enabled/disabled on their
                    // account.
                    String message = null;

                    // Whether the user has enabled direct calling (VoIP)
                    boolean voipEnabledByUser = ConfigurationUtils.isVoIPEnabledByUser();

                    // It isn't supported to have no VoIP and no CTD so, if
                    // VoIP has been disabled by the user, check whether CTD is
                    // still allowed in the CoS. If it isn't, change VoIP to be
                    // enabled, as the user is no longer allowed to disable it.
                    if (!voipEnabledByUser)
                    {
                        if (!(classOfService.getSubscribedMashups().isCtdAllowed() &&
                              classOfService.getClickToDialEnabled()))
                        {
                            logger.info(
                                "CTD has been disabled - setting VoIP to enabled");
                            configService.user().setProperty(
                                ProvisioningServiceImpl.VOIP_ENABLED_PROP, true);
                            voipEnabledByUser = true;
                        }
                    }

                    // Whether VoIP calling is enabled by the user and allowed
                    // in the CoS.
                    // We could use ConfigurationUtils.isVoIPEnabledInCoS()
                    // for the VoIP enabled by CoS setting, but there is a race
                    // condition between the CoS storeCosFieldsInConfig() method
                    // saving this setting to the config file, and this callback
                    // being called (where isVoIPEnabledInCoS() would then read
                    // that same config file).
                    boolean callingAllowed = voipEnabledByUser &&
                          classOfService.getSubscribedMashups().isVoipAllowed();

                    if (callingAllowed && !oldCallingAllowed)
                    {
                        logger.debug("Calling is enabled");

                        if (cosAlreadyReceived)
                        {
                            logger.info("Calling has been enabled - show dialog");

                            // If the previous value of callingAllowed was
                            // false and this isn't the first time we've
                            // received the CoS, this means that VoIP has been
                            // enabled on a client running with VoIP disabled.
                            // Set the message text so we will display a dialog
                            // to the user to ask them to restart the client so
                            // they can start using VoIP.
                            message =
                                res.getI18NString("service.gui.VOIP_ACTIVATED");
                        }
                        else
                        {
                            logger.info(
                                "Calling enabled - creating telephony op sets");

                            // Calling has been enabled on startup, so just add
                            // the telephony op sets.
                            createTelephonyOpSets(opSetBasicTelephonySipImpl);
                        }
                    }
                    else if (!callingAllowed && oldCallingAllowed)
                    {
                        logger.info("Calling has been disabled - show dialog");

                        // If calling used to be allowed on the client,
                        // set the message text so we will display a
                        // dialog to the user to warn them that VoIP
                        // has been disabled and asking them to restart
                        // the client.
                        message = res.getI18NString("service.gui.VOIP_DISABLED");
                    }

                    // We've finished processing VoIP information, so update
                    // the value of oldCallingAllowed.
                    oldCallingAllowed = callingAllowed;

                    if (message != null)
                    {
                        // The message text has been set, so display it.
                        String title = res.getI18NString("service.gui.INFORMATION");
                        String buttonText = res.getI18NString("service.gui.FORCE_QUIT");
                        final ErrorDialog dialog = new ErrorDialog(
                            title, message, OnDismiss.FORCE_EXIT);
                        dialog.setModal(true);
                        dialog.setButtonText(buttonText);

                        // Display the dialog on a new thread so it doesn't block start-up
                        new Thread("SipCosChange.ShowDialog")
                        {
                            public void run()
                            {
                                logger.info("Forcing quit of application (" +
                                            "SIP CoS change)");
                                dialog.showDialog();
                            }
                        }.start();
                    }

                    // We only want to set the presence fields the first time
                    // we receive the CoS, as changes to the presence fields
                    // require app restart
                    if (!cosAlreadyReceived)
                    {
                        if (classOfService.getMessagingAllowed() &&
                            classOfService.getNotifyMWIAvailable())
                        {
                            logger.info("CoS allows MWI");
                            createMessagingOpSets();
                        }
                        else
                        {
                            logger.info("CoS does not allow MWI");
                        }

                        if (!"IndividualLine".equals(classOfService.getSubscriberType()))
                        {
                            logger.info("CoS allows presence");
                            createPresenceOpSets(accountID, opSetBasicTelephonySipImpl);
                        }
                        else
                        {
                            logger.info("CoS does not allow presence");
                        }

                        cosAlreadyReceived = true;
                    }
                }
            });

            isInitialized = true;
        }
    }

    /**
     * Create the operation sets for telephony
     */
    private void createTelephonyOpSets(
        OperationSetBasicTelephonySipImpl opSetBasicTelephonySipImpl)
    {
        addSupportedOperationSet(
            OperationSetBasicTelephony.class,
            opSetBasicTelephonySipImpl);

        addSupportedOperationSet(
            OperationSetAdvancedTelephony.class,
            opSetBasicTelephonySipImpl);

        OperationSetAutoAnswerSipImpl autoAnswerOpSet
            = new OperationSetAutoAnswerSipImpl(this);
        // Make sure we auto-answer call pull requests made by this client.
        autoAnswerOpSet.setAutoAnswerCondition(
            AlertInfoHeader.NAME, CallJumpService.getCallJumpAlertInfoHeader(false, true));
        addSupportedOperationSet(
            OperationSetBasicAutoAnswer.class, autoAnswerOpSet);
        addSupportedOperationSet(
            OperationSetAdvancedAutoAnswer.class, autoAnswerOpSet);

        // init call security
        addSupportedOperationSet(
            OperationSetSecureZrtpTelephony.class,
            opSetBasicTelephonySipImpl);
        addSupportedOperationSet(
            OperationSetSecureSDesTelephony.class,
            opSetBasicTelephonySipImpl);

        // OperationSetVideoTelephony
        addSupportedOperationSet(
            OperationSetVideoTelephony.class,
            new OperationSetVideoTelephonySipImpl(
                    opSetBasicTelephonySipImpl));

        addSupportedOperationSet(
            OperationSetTelephonyConferencing.class,
            new OperationSetTelephonyConferencingSipImpl(this));

        // init DTMF (from JM Heitz)
        OperationSetDTMFSipImpl operationSetDTMFSip
            = new OperationSetDTMFSipImpl(this);
        addSupportedOperationSet(
            OperationSetDTMF.class, operationSetDTMFSip);

        addSupportedOperationSet(
            OperationSetIncomingDTMF.class,
            new OperationSetIncomingDTMFSipImpl(
                    this, operationSetDTMFSip));
    }

    /**
     * Create the operation sets for messaging
     */
    private void createMessagingOpSets()
    {
        addSupportedOperationSet(
            OperationSetMessageWaiting.class,
            new OperationSetMessageWaitingSipImpl(this));
    }

    /**
     * Create the operation sets for presence.
     *
     * @param accountID The account to create the op sets for
     * @param opSetBasicTelephonySipImpl The telephony op set
     */
    private void createPresenceOpSets(SipAccountID accountID,
        OperationSetBasicTelephonySipImpl opSetBasicTelephonySipImpl)
    {
        int pollingValue =
            accountID.getAccountPropertyInt(
                ProtocolProviderFactory.POLLING_PERIOD, 30);

        int subscriptionExpiration =
            accountID.getAccountPropertyInt(
                ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION, 3600);

        //init presence op set.
        OperationSetPersistentPresence opSetPersPresence
            = OperationSetPresenceSipImpl
                .createOperationSetPresenceSipImpl(
                    this,
                    true,
                    pollingValue,
                    subscriptionExpiration);

        addSupportedOperationSet(
            OperationSetPersistentPresence.class,
            opSetPersPresence);
        //also register with standard presence
        addSupportedOperationSet(
            OperationSetPresence.class,
            opSetPersPresence);

        OperationSetServerStoredAccountInfoSipImpl opSetSSAccountInfo =
            new OperationSetServerStoredAccountInfoSipImpl(this);

        // Set the display name.
        if(opSetSSAccountInfo != null)
            opSetSSAccountInfo.setOurDisplayName(ourDisplayName);

        // init avatar
        addSupportedOperationSet(
            OperationSetServerStoredAccountInfo.class,
            opSetSSAccountInfo);

        addSupportedOperationSet(
            OperationSetAvatar.class,
            new OperationSetAvatarSipImpl(this, opSetSSAccountInfo));

        // Call Park service always runs (but won't do anything if it is
        // disabled by the subscriber).
        // Surely wrong that this is added in the 'presence' OpSets, not the telephony ones. No
        // ill effects observed, though, so left as is for now.
        addSupportedOperationSet(OperationSetCallPark.class,
            new OperationSetCallParkSipImpl(
                opSetBasicTelephonySipImpl,
                (OperationSetPresenceSipImpl) opSetPersPresence));
    }

    /**
     * Adds a specific <tt>OperationSet</tt> implementation to the set of
     * supported <tt>OperationSet</tt>s of this instance. Serves as a type-safe
     * wrapper around <tt>supportedOperationSets</tt> which works with class
     * names instead of <tt>Class</tt> and also shortens the code which performs
     * such additions.
     *
     * @param <T> the exact type of the <tt>OperationSet</tt> implementation to
     * be added
     * @param opsetClass the <tt>Class</tt> of <tt>OperationSet</tt> under the
     * name of which the specified implementation is to be added
     * @param opset the <tt>OperationSet</tt> implementation to be added
     */
    @Override
    protected <T extends OperationSet> void addSupportedOperationSet(
            Class<T> opsetClass,
            T opset)
    {
        super.addSupportedOperationSet(opsetClass, opset);
    }

    /**
     * Removes an <tt>OperationSet</tt> implementation from the set of
     * supported <tt>OperationSet</tt>s for this instance.
     *
     * @param <T> the exact type of the <tt>OperationSet</tt> implementation to
     * be added
     * @param opsetClass the <tt>Class</tt> of <tt>OperationSet</tt> under the
     * name of which the specified implementation is to be added
     */
    @Override
    protected <T extends OperationSet> void removeSupportedOperationSet(
                                                Class<T> opsetClass)
    {
        super.removeSupportedOperationSet(opsetClass);
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent)
    {
        // This is hit when we aren't getting a response to our SIP messages.
        // We assume this is due to a Perimeta fail over so we re-register with
        // the Perimeta to reestablish the connection and avoid the CFS tearing
        // the call down.
        if (waitingForReregister != null) {
            // We are already re-registering, so no need to try again
            logger.debug("Already re-registering, no need to try again");
            return;
        }

        // Re-register as our connection has dropped
        waitingForReregister = new WaitingForReregister();
        sendReRegister(REREGISTER_DELAY);
    }

    /**
     * After a successful reregistration, reinvite all active calls to avoid
     * the CFS tearing them down because the Perimeta doesn't know about them.
     */
    public void reregistrationSuccess()
    {
        logger.debug("Reregistration success");

        WaitingForReregister waitingForReregisterLocal = waitingForReregister;

        if (waitingForReregisterLocal != null)
        {
            // We are now in a good state and can continue as normal
            waitingForReregisterLocal.retryRegisterSuccess();
        }
    }

    /**
     * After a failed reregistration, see if we want to retry, and return the
     * number of secs to wait before retrying.
     *
     * @return the number of seconds to wait before retrying registration, or 0
     *         if we should give up.
     */
    public int getReregistrationRetryTime()
    {
        logger.debug("Reregistration failed");
        WaitingForReregister waitingForReregisterLocal = waitingForReregister;

        if (waitingForReregisterLocal != null)
        {
            // The registration state is stored, see what it
            // tells us to do.
            return waitingForReregisterLocal.retryReregisterTime();
        }
        else
        {
            // We have no reregistration in progress, return 0.
            return 0;
        }
    }

    /**
     * Processes a Response received on a SipProvider upon which this
     * SipListener is registered.
     * <p>
     *
     * @param responseEvent the responseEvent fired from the SipProvider to the
     * SipListener representing a Response received from the network.
     */
    public void processResponse(ResponseEvent responseEvent)
    {
        ClientTransaction clientTransaction = responseEvent
            .getClientTransaction();
        if (clientTransaction == null)
        {
            logger.debug("ignoring a transactionless response");
            return;
        }

        Response response = responseEvent.getResponse();

        earlyProcessMessage(responseEvent);

        String method = ( (CSeqHeader) response.getHeader(CSeqHeader.NAME))
            .getMethod();

        synchronized (methodProcessors)
        {
            //find the object that is supposed to take care of responses with
            //the corresponding method
            List<MethodProcessor> processors = methodProcessors.get(method);

            if (processors != null)
            {
                logger.debug("Found " + processors.size()
                        + " processor(s) for method " + method);

                for (MethodProcessor processor : processors)
                    if (processor.processResponse(responseEvent))
                        break;
            }
        }
    }

    /**
     * Processes a retransmit or expiration Timeout of an underlying
     * {@link Transaction} handled by this SipListener. This Event notifies the
     * application that a retransmission or transaction Timer expired in the
     * SipProvider's transaction state machine. The TimeoutEvent encapsulates
     * the specific timeout type and the transaction identifier either client or
     * server upon which the timeout occurred. The type of Timeout can by
     * determined by:
     * <code>timeoutType = timeoutEvent.getTimeout().getValue();</code>
     *
     * @param timeoutEvent -
     *            the timeoutEvent received indicating either the message
     *            retransmit or transaction timed out.
     */
    public void processTimeout(TimeoutEvent timeoutEvent)
    {
        Transaction transaction;
        if(timeoutEvent.isServerTransaction())
            transaction = timeoutEvent.getServerTransaction();
        else
            transaction = timeoutEvent.getClientTransaction();

        if (transaction == null)
        {
            logger.debug("ignoring a transactionless timeout event");
            return;
        }

        earlyProcessMessage(timeoutEvent);

        Request request = transaction.getRequest();

        //find the object that is supposed to take care of responses with the
        //corresponding method
        String method = request.getMethod();
        callTimeoutProcessors(timeoutEvent, method);

        if (method.equalsIgnoreCase("INVITE"))
        {
            // If INVITE timed out then we should also timeout REGISTERs.
             callTimeoutProcessors(timeoutEvent, "REGISTER");
        }
    }

    /**
     * Call the registered listeners for timeout events.
     *
     * @param timeoutEvent The event that triggered this method
     * @param method The type of SIP event that timed out
     */
    private void callTimeoutProcessors(TimeoutEvent timeoutEvent, String method)
    {
        synchronized (methodProcessors)
        {
            List<MethodProcessor> processors = methodProcessors.get(method);
            if (processors != null)
            {
                logger.debug("Found " + processors.size()
                    + " processor(s) for method " + method);

                for (MethodProcessor processor : processors)
                {
                    if (processor.processTimeout(timeoutEvent))
                    {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent.
     * When a transaction transitions to the Terminated state, the stack
     * keeps no further records of the transaction. This notification can be used by
     * applications to clean up any auxiliary data that is being maintained
     * for the given transaction.
     *
     * @param transactionTerminatedEvent -- an event that indicates that the
     *       transaction has transitioned into the terminated state.
     * @since v1.2
     */
    public void processTransactionTerminated(TransactionTerminatedEvent
                                             transactionTerminatedEvent)
    {
        Transaction transaction;
        if(transactionTerminatedEvent.isServerTransaction())
            transaction = transactionTerminatedEvent.getServerTransaction();
        else
            transaction = transactionTerminatedEvent.getClientTransaction();

        if (transaction == null)
        {
            logger.debug("ignoring a transactionless transaction terminated event");
            return;
        }

        Request request = transaction.getRequest();

        //find the object that is supposed to take care of responses with the
        //corresponding method
        String method = request.getMethod();
        synchronized (methodProcessors)
        {
            List<MethodProcessor> processors = methodProcessors.get(method);

            if (processors != null)
            {
                logger.debug("Found " + processors.size()
                            + " processor(s) for method " + method);

                for (MethodProcessor processor : processors)
                {
                    if (processor.processTransactionTerminated(
                        transactionTerminatedEvent))
                    {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Process an asynchronously reported DialogTerminatedEvent.
     * When a dialog transitions to the Terminated state, the stack
     * keeps no further records of the dialog. This notification can be used by
     * applications to clean up any auxiliary data that is being maintained
     * for the given dialog.
     *
     * @param dialogTerminatedEvent -- an event that indicates that the
     *       dialog has transitioned into the terminated state.
     * @since v1.2
     */
    public void processDialogTerminated(DialogTerminatedEvent
                                        dialogTerminatedEvent)
    {
        logger.debug("Dialog terminated for req="
                + dialogTerminatedEvent.getDialog());
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener
     * is registered.
     * <p>
     * @param requestEvent requestEvent fired from the SipProvider to the
     * SipListener representing a Request received from the network.
     */
    public void processRequest(RequestEvent requestEvent)
    {
        Request request = requestEvent.getRequest();

        if(sipRegistrarConnection != null
           && !sipRegistrarConnection.isRegistrarless()
           && !sipRegistrarConnection.isRequestFromSameConnection(request)
           && !forceLooseRouting)
        {
            logger.warn("Received request not from our proxy, ignoring it! "
                + "Request:" + request);
            if (requestEvent.getServerTransaction() != null)
            {
                try
                {
                    requestEvent.getServerTransaction().terminate();
                }
                catch (Throwable e)
                {
                    logger.warn("Failed to properly terminate transaction for "
                                    +"a rogue request. Well ... so be it "
                                    + "Request:" + request);
                }
            }
            return;
        }

        earlyProcessMessage(requestEvent);

        // test if an Event header is present and known
        EventHeader eventHeader = (EventHeader)
            request.getHeader(EventHeader.NAME);

        if (eventHeader != null)
        {
            boolean eventKnown;

            synchronized (this.registeredEvents)
            {
                eventKnown = this.registeredEvents.contains(
                        eventHeader.getEventType());
            }

            if (!eventKnown)
            {
                // send a 489 / Bad Event response
                ServerTransaction serverTransaction = requestEvent
                    .getServerTransaction();

                if (serverTransaction == null)
                {
                    try
                    {
                        serverTransaction = SipStackSharing.
                            getOrCreateServerTransaction(requestEvent);
                    }
                    catch (TransactionAlreadyExistsException | TransactionUnavailableException ex)
                    {
                        //let's not scare the user and only log a message
                        logger.error("Failed to create a new server"
                            + "transaction for an incoming request\n"
                            + "(Next message contains the request)"
                            , ex);
                        return;
                    }
                }

                Response response = null;
                try
                {
                    response = this.getMessageFactory().createResponse(
                            Response.BAD_EVENT, request);
                }
                catch (ParseException e)
                {
                    logger.error("failed to create the 489 response", e);
                    return;
                }

                try
                {
                    serverTransaction.sendResponse(response);
                    return;
                }
                catch (SipException e)
                {
                    logger.error("failed to send the response", e);
                }
                catch (InvalidArgumentException e)
                {
                    // should not happen
                    logger.error("invalid argument provided while trying" +
                            " to send the response", e);
                }
            }
        }

        String method = request.getMethod();

        //raise this flag if at least one processor handles the request.
        boolean processedAtLeastOnce = false;

        synchronized (methodProcessors)
        {
            //find the object that is supposed to take care of responses with
            //the corresponding method
            List<MethodProcessor> processors = methodProcessors.get(method);

            if (processors != null)
            {
                logger.debug("Found " + processors.size()
                            + " processor(s) for method " + method);

                for (MethodProcessor processor : processors)
                {
                    if (processor.processRequest(requestEvent))
                    {
                        processedAtLeastOnce = true;
                        break;
                    }
                }
            }
        }

        //send an error response if no one processes this
        if (!processedAtLeastOnce)
        {
            ServerTransaction serverTransaction;
            try
            {
                serverTransaction = SipStackSharing.getOrCreateServerTransaction(requestEvent);

                if (serverTransaction == null)
                {
                    logger.warn("Could not create a transaction for a "
                               +"non-supported method " + request.getMethod());
                    return;
                }

                TransactionState state = serverTransaction.getState();

                if( TransactionState.TRYING.equals(state))
                {
                    Response response = this.getMessageFactory().createResponse(
                                    Response.NOT_IMPLEMENTED, request);
                    serverTransaction.sendResponse(response);
                }
            }
            catch (Throwable exc)
            {
                logger.warn("Could not respond to a non-supported method "
                                + request.getMethod(), exc);
            }
        }
    }

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown()
    {
        logger.info("Shutdown " + this);

        if(!isInitialized)
        {
            return;
        }

        // don't run in Thread cause shutting down may finish before
        // we were able to unregister
        threadingService.submit("SIP ShutdownThread", new ShutdownThread(this));
    }

    /**
     * The thread that we use in order to send our unREGISTER request upon
     * system shut down.
     */
    private class ShutdownThread implements Runnable
    {
        ProtocolProviderServiceSipImpl provider;

        private ShutdownThread(ProtocolProviderServiceSipImpl provider)
        {
            this.provider = provider;
        }

        /**
         * Shutdowns operation sets that need it then calls the
         * <tt>SipRegistrarConnection.unregister()</tt> method.
         */
        public void run()
        {
            logger.trace("Killing the SIP Protocol Provider.");
            //kill all active calls
            OperationSetBasicTelephonySipImpl telephony
                = (OperationSetBasicTelephonySipImpl)getOperationSet(
                    OperationSetBasicTelephony.class);

            if (telephony != null)
                telephony.shutdown();

            if (isRegistered())
            {
                try
                {
                    //create a listener that would notify us when
                    //un-registration has completed.
                    ShutdownUnregistrationBlockListener listener
                        = new ShutdownUnregistrationBlockListener();
                    addRegistrationStateChangeListener(listener);

                    //do the un-registration
                    unregister();

                    //leave ourselves time to complete un-registration (may include
                    //2 REGISTER requests in case notification is needed.)
                    listener.waitForEvent(3000L);
                }
                catch (OperationFailedException ex)
                {
                    //we're shutting down so we need to silence the exception here
                    logger.error(
                        "Failed to properly unregister before shutting down. "
                        + getAccountID()
                        , ex);
                }
            }

            headerFactory = null;
            messageFactory = null;
            addressFactory = null;
            sipSecurityManager = null;
            securityHeaderFactory = null;

            connection = null;

            synchronized (methodProcessors)
            {
                methodProcessors.clear();
            }

            sipStackSharing.removeSipListener(provider);
            isInitialized = false;
        }
    }

    /**
     * Initializes and returns an ArrayList with a single ViaHeader
     * containing a localhost address usable with the specified
     * s<tt>destination</tt>. This ArrayList may be used when sending
     * requests to that destination.
     * <p>
     * @param intendedDestination The address of the destination that the
     * request using the via headers will be sent to.
     *
     * @return ViaHeader-s list to be used when sending requests.
     * @throws OperationFailedException code INTERNAL_ERROR if a ParseException
     * occurs while initializing the array list.
     *
     */
    public List<ViaHeader> getLocalViaHeaders(Address intendedDestination)
        throws OperationFailedException
    {
        return getLocalViaHeaders((SipURI)intendedDestination.getURI());
    }

    /**
     * Initializes and returns an ArrayList with a single ViaHeader
     * containing a localhost address usable with the specified
     * s<tt>destination</tt>. This ArrayList may be used when sending
     * requests to that destination.
     * <p>
     * @param intendedDestination The address of the destination that the
     * request using the via headers will be sent to.
     *
     * @return ViaHeader-s list to be used when sending requests.
     * @throws OperationFailedException code INTERNAL_ERROR if a ParseException
     * occurs while initializing the array list.
     *
     */
    public List<ViaHeader> getLocalViaHeaders(SipURI intendedDestination)
        throws OperationFailedException
    {
        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();

        try
        {
            ListeningPoint srcListeningPoint
                    = getListeningPoint(intendedDestination.getTransportParam());

            InetSocketAddress targetAddress =
                        getIntendedDestination(intendedDestination);

            InetAddress localAddress = sipRegistrarConnection.getLocalIpToUse();

            if (localAddress == null)
            {
                // If this request is a REGISTER, and we have sent no previous
                // registers, the registrar connection won't yet have decided on
                // an IP to use.
                // Instead just let the OS pick a local IP that routes to the
                // target destination, and use that in the via header.
                // Store the route to the registrar so we can determine if it changes.
                NetworkAddressManagerService networkManager =
                                 SipActivator.getNetworkAddressManagerService();
                if (networkManager == null)
                {
                    logger.error("NetworkAddressManagerService was null when " +
                                 "choosing a local IP address to use for a " +
                                 "via header.");
                    return null;
                }

                localAddress = networkManager.getLocalHost(
                                                    targetAddress.getAddress());
                logger.info("No saved IP address to use for via header; use local address");
            }

            int localPort = srcListeningPoint.getPort();
            String transport = srcListeningPoint.getTransport();
            if (ListeningPoint.TCP.equalsIgnoreCase(transport)
                || ListeningPoint.TLS.equalsIgnoreCase(transport))
            {
                InetSocketAddress localSockAddr
                    = sipStackSharing.getLocalAddressForDestination(
                                    targetAddress.getAddress(),
                                    targetAddress.getPort(),
                                    localAddress,
                                    transport);
                localPort = localSockAddr.getPort();
            }

            ViaHeader viaHeader = headerFactory.createViaHeader(
                localAddress.getHostAddress(),
                localPort,
                transport,
                null
                );
            viaHeaders.add(viaHeader);
            return viaHeaders;
        }
        catch (ParseException ex)
        {
            logger.error(
                "A ParseException occurred while creating Via Headers!", ex);
            throw new OperationFailedException(
                "A ParseException occurred while creating Via Headers!"
                ,OperationFailedException.INTERNAL_ERROR
                ,ex);
        }
        catch (InvalidArgumentException ex)
        {
            throw createOperationFailedException(ex, OperationFailedException.INTERNAL_ERROR);
        }
        catch (IllegalStateException | java.io.IOException ex)
        {
            throw createOperationFailedException(ex, OperationFailedException.NETWORK_FAILURE);
        }
    }

    private OperationFailedException createOperationFailedException(Exception exception, int exceptionType)
    {
        String errorMessage;
        int udpPort;
        try
        {
            udpPort = sipStackSharing.getLP(ListeningPoint.UDP).getPort();
            errorMessage = "Unable to create a via header for port " + udpPort;
        }
        catch (IllegalStateException ex)
        {
            errorMessage = "Unable to create a via header. There are no listening points available " + ex;
        }

        logger.error(errorMessage, exception);
        return new OperationFailedException(errorMessage, exceptionType, exception);
    }

    /**
     * Initializes and returns this provider's default maxForwardsHeader field
     * using the value specified by MAX_FORWARDS.
     *
     * @return an instance of a MaxForwardsHeader that can be used when
     * sending requests
     *
     * @throws OperationFailedException with code INTERNAL_ERROR if MAX_FORWARDS
     * has an invalid value.
     */
    public MaxForwardsHeader getMaxForwardsHeader() throws
        OperationFailedException
    {
        if (maxForwardsHeader == null)
        {
            try
            {
                maxForwardsHeader = headerFactory.createMaxForwardsHeader(
                    MAX_FORWARDS);
                logger.debug("generated max forwards: "
                            + maxForwardsHeader.toString());
            }
            catch (InvalidArgumentException ex)
            {
                throw new OperationFailedException(
                    "A problem occurred while creating MaxForwardsHeader"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
            }
        }

        return maxForwardsHeader;
    }

    /**
     * Returns a SIP URI for the Contact header based on our IP address.
     *
     * @param intendedDestination the destination that we plan to be sending
     * this contact header to.
     *
     * @return the SIP URI to use in the Contact header.
     */
    public Address getContactURI(SipURI intendedDestination)
    {
        Address contactAddress = null;

        try
        {
            ListeningPoint srcListeningPoint
                    = getListeningPoint(intendedDestination);

            InetSocketAddress targetAddress =
                    getIntendedDestination(intendedDestination);

            //find the address to use with the target
            InetAddress localAddress = SipActivator
                .getNetworkAddressManagerService()
                .getLocalHost(targetAddress.getAddress());

            SipURI contactURI = addressFactory.createSipURI(
                getAccountID().getUserID()
                , localAddress.getHostAddress() );

            String transport = srcListeningPoint.getTransport();
            contactURI.setTransportParam(transport);

            int localPort = srcListeningPoint.getPort();

            //if we are using tcp, make sure that we include the port of the
            //socket that we are actually using and not that of LP
            if (ListeningPoint.TCP.equalsIgnoreCase(transport)
                || ListeningPoint.TLS.equalsIgnoreCase(transport))
            {
                InetSocketAddress localSockAddr
                    = sipStackSharing.getLocalAddressForDestination(
                                    targetAddress.getAddress(),
                                    targetAddress.getPort(),
                                    localAddress,
                                    transport);
                localPort = localSockAddr.getPort();
            }

            contactURI.setPort(localPort);

            // set a custom param to ease incoming requests dispatching in case
            // we have several registrar accounts with the same username
            String paramValue = getContactAddressCustomParamValue();
            if (paramValue != null)
            {
                contactURI.setParameter(
                        SipStackSharing.CONTACT_ADDRESS_CUSTOM_PARAM_NAME,
                        paramValue);
            }

            contactAddress = addressFactory.createAddress( contactURI );

            String ourDisplayName = getOurDisplayName();
            if (ourDisplayName != null)
            {
                contactAddress.setDisplayName(ourDisplayName);
            }
        }
        catch (ParseException ex)
        {
            logger.error(
                "A ParseException occurred while creating contact URI!", ex);
            throw new IllegalArgumentException(
                "A ParseException occurred while creating contact URI!", ex);
        }
        catch (java.io.IOException ex)
        {
            logger.error(
                "An IOException occurred while creating contact URI!", ex);
            throw new IllegalArgumentException(
                "An IOException occurred while creating contact URI!", ex);
        }
        catch (IllegalStateException ex)
        {
            logger.error(
                    "An IllegalStateException occurred while creating contact URI! " +
                    "We are probably offline and no listening points are available.", ex);
            throw new IllegalArgumentException(
                    "An IllegalStateException occurred while creating contact URI! " +
                    "We are probably offline and no listening points are available. ", ex);
        }

        return contactAddress;
    }

    /**
     * Returns a Contact header containing a sip URI based on a localhost
     * address and therefore usable in REGISTER requests only.
     *
     * @param intendedDestination the destination that we plan to be sending
     * this contact header to.
     *
     * @return a Contact header based upon a local inet address.
     */
    public ContactHeader getContactHeader(SipURI intendedDestination)
    {
        ContactHeader registrationContactHeader = null;
        Address contactAddress = null;

        // Try to get the contact address from our registrar connection so that
        // we use the same contact address throughout our connection,
        // regardless of changes of local IP address.
        SipRegistrarConnection src = sipRegistrarConnection;
        if (src != null)
        {
            contactAddress = src.getContactAddress(intendedDestination);
        }
        else
        {
            contactAddress = getContactURI(intendedDestination);
        }

        // Finally, build the contact header from the address.
        registrationContactHeader = headerFactory.createContactHeader(
            contactAddress);

        logger.debug("Generated contactHeader successfully.");

        return registrationContactHeader;
    }

    /**
     * Returns null for a registrarless account, a value for the contact address
     * custom parameter otherwise. This will help the dispatching of incoming
     * requests between accounts with the same username. For address-of-record
     * user@example.com, the returned value would be example_com.
     *
     * @return null for a registrarless account, a value for the
     * "registering_acc" contact address parameter otherwise
     */
    public String getContactAddressCustomParamValue()
    {
            SipRegistrarConnection src = sipRegistrarConnection;
            if (src != null && !src.isRegistrarless())
            {
                // if we don't replace the dots in the hostname, we get
                // "476 No Server Address in Contacts Allowed"
                // from certain registrars (ippi.fr for instance)
                String hostValue = ((SipURI) src.getAddressOfRecord().getURI())
                    .getHost().replace('.', '_');
                return hostValue;
            }
            return null;
    }

    /**
     * Creates and returns a RouteList object representing a list of Route
     * headers taken from a Service-Route header on a REGISTER response.
     *
     * @return a <tt>RouteList</tt> containing the header(s), may be empty
     * or null.
     */
    public RouteList getPreloadedRouteList()
    {
        RouteList routeList = new RouteList();
        try
        {
            ListIterator<ServiceRoute> serviceRouteList =
                                 getRegistrarConnection().getServiceRouteList();
            if ((serviceRouteList != null) && serviceRouteList.hasNext())
            {
                while (serviceRouteList.hasNext()) {
                    ServiceRoute sr = serviceRouteList.next();

                    Route route = new Route();
                    AddressImpl address = ((AddressImpl) ((AddressImpl) sr
                            .getAddress()).clone());

                    route.setAddress(address);
                    route.setHop(sr.getHop());
                    route.setParameters((NameValueList) sr.getParameters()
                            .clone());

                    routeList.add(route);
                }
            }
        }
        catch (Exception ex)
        {
           logger.error(
                  "A ParseException occurred while creating Route headers", ex);
           routeList = null;
        }

        return routeList;
    }

    /**
     * Returns the AddressFactory used to create URLs ans Address objects.
     *
     * @return the AddressFactory used to create URLs ans Address objects.
     */
    public AddressFactoryEx getAddressFactory()
    {
        return addressFactory;
    }

    /**
     * Returns the HeaderFactory used to create SIP message headers.
     *
     * @return the HeaderFactory used to create SIP message headers.
     */
    public HeaderFactory getHeaderFactory()
    {
        return headerFactory;
    }

    /**
     * Returns the Message Factory used to create SIP messages.
     *
     * @return the Message Factory used to create SIP messages.
     */
    public SipMessageFactory getMessageFactory()
    {
        if (messageFactory == null)
        {
            messageFactory =
                new SipMessageFactory(this, new MessageFactoryImpl());
        }
        return messageFactory;
    }

    /**
     * Returns the SecurityHeaderFactory used to create SIP security headers.
     *
     * @return the SecurityHeaderFactory used to create SIP security headers.
     */
    SecurityHeaderFactory getSecurityHeaderFactory()
    {
        return securityHeaderFactory;
    }

    /**
     * Returns all running instances of ProtocolProviderServiceSipImpl
     *
     * @return all running instances of ProtocolProviderServiceSipImpl
     */
    public static Set<ProtocolProviderServiceSipImpl> getAllInstances()
    {
        try
        {
            Set<ProtocolProviderServiceSipImpl> instances
                = new HashSet<>();
            BundleContext context = SipActivator.getBundleContext();
            ServiceReference<?>[] references = context.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    null
                    );
            for(ServiceReference<?> reference : references)
            {
                Object service = context.getService(reference);
                if(service instanceof ProtocolProviderServiceSipImpl)
                    instances.add((ProtocolProviderServiceSipImpl) service);
            }
            return instances;
        }
        catch(InvalidSyntaxException ex)
        {
            logger.debug("Problem parsing an osgi expression", ex);
            // should never happen so crash if it ever happens
            throw new RuntimeException(
                    "getServiceReferences() wasn't supposed to fail!"
                    );
        }
     }

    /**
     * Returns the default listening point that we use for communication over
     * <tt>transport</tt>.
     *
     * @param transport the transport that the returned listening point needs
     * to support.
     *
     * @return the default listening point that we use for communication over
     * <tt>transport</tt> or null if no such transport is supported.
     */
    public ListeningPoint getListeningPoint(String transport)
    {
        logger.trace("Query for a " + transport + " listening point");

        //override the transport in case we have an outbound proxy.
        if(connection.getAddress() != null)
        {
            logger.trace("Will use proxy address");
            transport = connection.getTransport();
        }

        if(!isValidTransport(transport))
        {
            transport = getDefaultTransport();
        }

        ListeningPoint lp = null;

        if(transport.equalsIgnoreCase(ListeningPoint.UDP))
        {
            lp = sipStackSharing.getLP(ListeningPoint.UDP);
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TCP))
        {
            lp = sipStackSharing.getLP(ListeningPoint.TCP);
        }
        else if(transport.equalsIgnoreCase(ListeningPoint.TLS))
        {
            lp = sipStackSharing.getLP(ListeningPoint.TLS);
        }

        logger.trace("Returning LP " + lp + " for transport ["
                            + transport + "] and ");
        return lp;
    }

    /**
     * Returns the default listening point that we should use to contact the
     * intended destination.
     *
     * @param intendedDestination the address that we will be trying to contact
     * through the listening point we are trying to obtain.
     *
     * @return the listening point that we should use to contact the
     * intended destination.
     */
    public ListeningPoint getListeningPoint(SipURI intendedDestination)
    {
        return getListeningPoint(intendedDestination.getTransportParam());
    }

    /**
     * Returns the default jain sip provider that we use for communication over
     * <tt>transport</tt>.
     *
     * @param transport the transport that the returned provider needs
     * to support.
     *
     * @return the default jain sip provider that we use for communication over
     * <tt>transport</tt> or null if no such transport is supported.
     */
    public SipProvider getJainSipProvider(String transport)
    {
        return sipStackSharing.getJainSipProvider(transport);
    }

    /**
     * Returns the currently valid sip security manager that everyone should
     * use to authenticate SIP Requests.
     * @return the currently valid instance of a SipSecurityManager that everyone
     * should use to authenticate SIP Requests.
     */
    public SipSecurityManager getSipSecurityManager()
    {
        return sipSecurityManager;
    }

    /**
     * Initializes the SipRegistrarConnection that this class will be using.
     *
     * @throws java.lang.IllegalArgumentException if one or more account
     * properties have invalid values.
     */
    private void initRegistrarConnection()
        throws IllegalArgumentException
    {
        //First init the registrar address
        String registrarAddressStr =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.SERVER_ADDRESS);

        //if there is no registrar address, parse the user_id and extract it
        //from the domain part of the SIP URI.
        if (registrarAddressStr == null)
        {
            String userID =
                accountID
                    .getAccountPropertyString(ProtocolProviderFactory.USER_ID);
            int index = userID.indexOf('@');
            if ( index > -1 )
                registrarAddressStr = userID.substring( index+1);
        }

        //if we still have no registrar address or if the registrar address
        //string is one of our local host addresses this means the users does
        //not want to use a registrar connection
        if(registrarAddressStr == null
           || registrarAddressStr.trim().length() == 0)
        {
            initRegistrarlessConnection();
            return;
        }

        //init registrar port
        int registrarPort = ListeningPoint.PORT_5060;

        // check if user has overridden the registrar port.
        registrarPort =
            accountID.getAccountPropertyInt(
                ProtocolProviderFactory.SERVER_PORT, registrarPort);
        if (registrarPort > NetworkUtils.MAX_PORT_NUMBER)
        {
            throw new IllegalArgumentException(registrarPort
                + " is larger than " + NetworkUtils.MAX_PORT_NUMBER
                + " and does not therefore represent a valid port number.");
        }

        //Initialize our connection with the registrar
        // we insert the default transport if none is specified
        // use it for registrar connection
        this.sipRegistrarConnection = new SipRegistrarConnection(
            registrarAddressStr
            , registrarPort
            , getDefaultTransport()
            , this);
    }

    /**
     * Initializes the SipRegistrarConnection that this class will be using.
     *
     * @throws java.lang.IllegalArgumentException if one or more account
     * properties have invalid values.
     */
    private void initRegistrarlessConnection()
        throws IllegalArgumentException
    {
        //registrar transport
        String registrarTransport =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.PREFERRED_TRANSPORT);

        if(registrarTransport != null && registrarTransport.length() > 0)
        {
            if( ! registrarTransport.equals(ListeningPoint.UDP)
                && !registrarTransport.equals(ListeningPoint.TCP)
                && !registrarTransport.equals(ListeningPoint.TLS))
            {
                throw new IllegalArgumentException(registrarTransport
                    + " is not a valid transport protocol. Transport must be "
                    +"left blanc or set to TCP, UDP or TLS.");
            }
        }
        else
        {
            registrarTransport = ListeningPoint.UDP;
        }

        //Initialize our connection with the registrar
        this.sipRegistrarConnection
               = new SipRegistrarlessConnection(this, registrarTransport);
    }

    /**
     * Returns the SIP address of record (Display Name <user@server.net>) that
     * this account is created for. The method takes into account whether or
     * not we are running in Register or "No Register" mode and either returns
     * the AOR we are using to register or an address constructed using the
     * local address.
     *
     * @param intendedDestination the destination that we would be using the
     * local address to communicate with.
     *
     * @return our Address Of Record that we should use in From headers.
     */
    public Address getOurSipAddress(Address intendedDestination)
    {
        return getOurSipAddress((SipURI)intendedDestination.getURI());
    }

    /**
     * Returns the SIP address of record (Display Name <user@server.net>) that
     * this account is created for. The method takes into account whether or
     * not we are running in Register or "No Register" mode and either returns
     * the AOR we are using to register or an address constructed using the
     * local address
     *
     * @param intendedDestination the destination that we would be using the
     * local address to communicate with.
     * .
     * @return our Address Of Record that we should use in From headers.
     */
    public Address getOurSipAddress(SipURI intendedDestination)
    {
        SipRegistrarConnection src = sipRegistrarConnection;
        if (src != null && !src.isRegistrarless())
            return src.getAddressOfRecord();

        //we are apparently running in "No Registrar" mode so let's create an
        //address by ourselves.
        InetSocketAddress destinationAddr
                    = getIntendedDestination(intendedDestination);

        InetAddress localHost = SipActivator.getNetworkAddressManagerService()
            .getLocalHost(destinationAddr.getAddress());

        String userID = getAccountID().getUserID();

        try
        {
            SipURI ourSipURI = getAddressFactory()
                .createSipURI(userID, localHost.getHostAddress());

            ListeningPoint lp = getListeningPoint(intendedDestination);

            ourSipURI.setTransportParam(lp.getTransport());
            ourSipURI.setPort(lp.getPort());

            Address ourSipAddress = getAddressFactory()
                .createAddress(getOurDisplayName(), ourSipURI);

            ourSipAddress.setDisplayName(getOurDisplayName());

            return ourSipAddress;
        }
        catch (ParseException exc)
        {
            logger.trace("Failed to create our SIP AOR address", exc);
            // this should never happen since we are using InetAddresses
            // everywhere so parsing could hardly go wrong.
            throw new IllegalArgumentException(
                            "Failed to create our SIP AOR address"
                            , exc);
        }
        catch (IllegalStateException ex)
        {
            logger.error(
                    "An IllegalStateException occurred while creating our SIP AOR address!" +
                    "We are probably offline and no listening points are available.", ex);
            throw new IllegalArgumentException(
                    "An IllegalStateException occurred while creating our SIP AOR address!" +
                    "We are probably offline and no listening points are available. ", ex);
        }
    }

    /**
     * Returns the <tt>ProxyConnection</tt>.
     *
     * @return the <tt>ProxyConnection</tt>
     */
    public ProxyConnection getConnection()
    {
        return connection;
    }

    /**
     * Indicates if the SIP transport channel is using a TLS secured socket.
     *
     * @return True when TLS is used the SIP transport protocol, false
     *         otherwise or when no proxy is being used.
     */
    public boolean isSignalingTransportSecure()
    {
        return ListeningPoint.TLS.equalsIgnoreCase(connection.getTransport());
    }

    /**
     * Determine whether we're using 3GPP mediasec headers in SIP messages (see
     * PRD 14452). Store as a variable to avoid extra config look-ups for every
     * SIP message.
     *
     * @return Whether the client is set up to use 3GPP media security headers
     */
    public boolean is3GPPMediaSecurityEnabled()
    {
        if (is3GPPMediaSecurityEnabled == null)
        {
            logger.debug("Getting initial 3GPP media and SRTP properties " +
                         "from config and adding property change listener");
            configService.user().addPropertyChangeListener(
                    PROPERTY_3GPP_MEDIA_HEADERS,
                    evt -> {
                        update3GPPMediaSecurity();
                    });
            configService.user().addPropertyChangeListener(
                    PROPERTY_SRTP_ENABLED,
                    evt -> {
                        update3GPPMediaSecurity();
                    });

            update3GPPMediaSecurity();
        }

        // Ensure the user's account supports SDES/SRTP as well.
        boolean isSDESEnabled = accountID.isEncryptionProtocolEnabled("SDES");
        return is3GPPMediaSecurityEnabled && isSDESEnabled;
    }

    /**
     * Update the property determining whether the client is using 3GPP media
     * security headers on config load or config update.
     */
    private void update3GPPMediaSecurity()
    {
        boolean use3GPP = configService.user()
                .getBoolean(PROPERTY_3GPP_MEDIA_HEADERS, false);

        // We should only use mediasec headers if SRTP is enabled. If it's not,
        // log a likely SIP PS misconfiguration.
        boolean useSRTP = configService.user()
                .getBoolean(PROPERTY_SRTP_ENABLED, false);

        if (use3GPP && !useSRTP)
        {
            logger.error("Set to use 3GPP media security headers but not" +
                         " using SRTP - possible misconfiguration?");
        }

        is3GPPMediaSecurityEnabled = use3GPP && useSRTP;
    }

    /**
     * Registers <tt>methodProcessor</tt> in the <tt>methodProcessors</tt> table
     * so that it would receive all messages in a transaction initiated by a
     * <tt>method</tt> request. If any previous processors exist for the same
     * method, they will be replaced by this one.
     *
     * @param method a String representing the SIP method that we're registering
     *            the processor for (e.g. INVITE, REGISTER, or SUBSCRIBE).
     * @param methodProcessor a <tt>MethodProcessor</tt> implementation that
     *            would handle all messages received within a <tt>method</tt>
     *            transaction.
     */
    public void registerMethodProcessor(String method,
        MethodProcessor methodProcessor)
    {
        synchronized (methodProcessors)
        {
            List<MethodProcessor> processors = methodProcessors.get(method);
            if (processors == null)
            {
                processors = new LinkedList<>();
                methodProcessors.put(method, processors);
            }
            else
            {
                /*
                 * Prevent the registering of multiple instances of one and the
                 * same OperationSet class and take only the latest registration
                 * into account.
                 */
                Iterator<MethodProcessor> processorIter = processors.iterator();
                Class<? extends MethodProcessor> methodProcessorClass
                    = methodProcessor.getClass();
                /*
                 * EventPackageSupport and its extenders provide a generic
                 * mechanism for building support for a specific event package
                 * so allow them to register multiple instances of one and the
                 * same class as long as they are handling different event
                 * packages.
                 */
                String eventPackage
                    = (methodProcessor instanceof EventPackageSupport)
                        ? ((EventPackageSupport) methodProcessor).getEventPackage()
                        : null;

                while (processorIter.hasNext())
                {
                    MethodProcessor processor = processorIter.next();

                    if (!processor.getClass().equals(methodProcessorClass))
                        continue;
                    if ((eventPackage != null)
                            && (processor instanceof EventPackageSupport)
                            && !eventPackage
                                    .equals(
                                        ((EventPackageSupport) processor)
                                            .getEventPackage()))
                        continue;

                    processorIter.remove();
                }
            }
            processors.add(methodProcessor);
        }
    }

    /**
     * Returns the transport that we should use if we have no clear idea of our
     * destination's preferred transport. The method would first check if
     * we are running behind an outbound proxy and if so return its transport.
     * If no outbound proxy is set, the method would check the contents of the
     * DEFAULT_TRANSPORT property and return it if not null. Otherwise the
     * method would return UDP;
     *
     * @return The first non null password of the following:
     * a) the transport we use to communicate with our registrar
     * b) the transport of our outbound proxy,
     * c) the transport specified by the DEFAULT_TRANSPORT property, UDP.
     */
    public String getDefaultTransport()
    {
        if(sipRegistrarConnection != null
            && !sipRegistrarConnection.isRegistrarless()
            && connection != null
            && connection.getAddress() != null
            && connection.getTransport() != null)
        {
            return connection.getTransport();
        }
        else
        {
            String userSpecifiedDefaultTransport =
                configService.user().getString(DEFAULT_TRANSPORT);

            if(userSpecifiedDefaultTransport != null)
            {
                return userSpecifiedDefaultTransport;
            }
            else
            {
                String defTransportDefaultValue = SipActivator.getResources()
                        .getSettingsString(DEFAULT_TRANSPORT);

                if(!StringUtils.isNullOrEmpty(defTransportDefaultValue))
                    return defTransportDefaultValue;
                else
                    return ListeningPoint.UDP;
            }
        }
    }

    /**
     * Returns the provider that corresponds to the transport returned by
     * getDefaultTransport(). Equivalent to calling
     * getJainSipProvider(getDefaultTransport())
     *
     * @return the Jain SipProvider that corresponds to the transport returned
     * by getDefaultTransport().
     */
    public SipProvider getDefaultJainSipProvider()
    {
        return getJainSipProvider(getDefaultTransport());
    }

    /**
     * Returns the display name string that the user has set as a display name
     * for this account.
     *
     * @return the display name string that the user has set as a display name
     * for this account.
     */
    public String getOurDisplayName()
    {
        return ourDisplayName;
    }

    /**
     * Changes the display name string.
     *
     * @return whether we have successfully changed the display name.
     */
    boolean setOurDisplayName(String newDisplayName)
    {
        // if we really want to change the display name
        // and it is existing, change it.
        if(newDisplayName != null && !ourDisplayName.equals(newDisplayName))
        {
            getAccountID().putAccountProperty(
                    ProtocolProviderFactory.DISPLAY_NAME,
                    newDisplayName);

            ourDisplayName = newDisplayName;

            OperationSetServerStoredAccountInfoSipImpl accountInfoOpSet
                = (OperationSetServerStoredAccountInfoSipImpl)getOperationSet(
                    OperationSetServerStoredAccountInfo.class);

            if(accountInfoOpSet != null)
                accountInfoOpSet.setOurDisplayName(newDisplayName);

            return true;
        }

        return false;
    }

    /**
     * Returns a User Agent header that could be used for signing our requests.
     *
     * @return a <tt>UserAgentHeader</tt> that could be used for signing our
     * requests.
     */
    public UserAgentHeader getSipCommUserAgentHeader()
    {
        if(userAgentHeader == null)
        {
            try
            {
                List<String> userAgentTokens = new LinkedList<>();

                Version ver =
                        SipActivator.getVersionService().getCurrentVersion();

                // Hardcoded User-Agent string because otherwise things break.
                // We do not want this to be branded.
                userAgentTokens.add("Accession Communicator");
                userAgentTokens.add(ver.toString());

                String osName = System.getProperty("os.name");
                userAgentTokens.add(osName);

                userAgentHeader
                    = this.headerFactory.createUserAgentHeader(userAgentTokens);
            }
            catch (ParseException ex)
            {
                //shouldn't happen
                return null;
            }
        }
        return userAgentHeader;
    }

    /**
     * Send an error response with the <tt>errorCode</tt> code using
     * <tt>serverTransaction</tt> and do not surface exceptions. The method
     * is useful when we are sending the error response in a stack initiated
     * operation and don't have the possibility to escalate potential
     * exceptions, so we can only log them.
     *
     * @param serverTransaction the transaction that we'd like to send an error
     * response in.
     * @param errorCode the code that the response should have.
     */
    public void sayErrorSilently(ServerTransaction serverTransaction,
                                 int               errorCode)
    {
        try
        {
            sayError(serverTransaction, errorCode);
        }
        catch (OperationFailedException exc)
        {
            logger.debug("Failed to send an error " + errorCode + " response",
                        exc);
        }
    }

    /**
     * Sends an ACK request in the specified dialog.
     *
     * @param clientTransaction the transaction that resulted in the ACK we are
     * about to send (MUST be an INVITE transaction).
     *
     * @throws InvalidArgumentException if there is a problem with the supplied
     * CSeq ( for example <= 0 ).
     * @throws SipException if the CSeq does not relate to a previously sent
     * INVITE or if the Method that created the Dialog is not an INVITE ( for
     * example SUBSCRIBE) or if we fail to send the INVITE for whatever reason.
     */
    public void sendAck(ClientTransaction clientTransaction)
        throws SipException, InvalidArgumentException
    {
            Request ack = messageFactory.createAck(clientTransaction);

            clientTransaction.getDialog().sendAck(ack);
    }

    /**
     * Send an error response with the <tt>errorCode</tt> code using
     * <tt>serverTransaction</tt>.
     *
     * @param serverTransaction the transaction that we'd like to send an error
     * response in.
     * @param errorCode the code that the response should have.
     *
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
     */
    public void sayError(ServerTransaction serverTransaction, int errorCode)
        throws OperationFailedException
    {
        sayError(serverTransaction, errorCode, null);
    }

    /**
     * Send an error response with the <tt>errorCode</tt> code using
     * <tt>serverTransaction</tt>.
     *
     * @param serverTransaction the transaction that we'd like to send an error
     * response in.
     * @param errorCode the code that the response should have.
     * @param header SIP header
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
     */
    public void sayError(ServerTransaction serverTransaction,
                         int errorCode,
                         Header header)
        throws OperationFailedException
    {
        Request request = serverTransaction.getRequest();
        Response errorResponse = null;
        try
        {
            errorResponse = getMessageFactory().createResponse(
                            errorCode, request);

            if(header != null)
                errorResponse.setHeader(header);

            //we used to be adding a To tag here and we shouldn't. 3261 says:
            //"Dialogs are created through [...] non-failure responses". and
            //we are using this method for failure responses only.
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to construct an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        try
        {
            serverTransaction.sendResponse(errorResponse);
            logger.debug("sent response");
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }
    }

    /**
     * Sends a specific <tt>Request</tt> through a given
     * <tt>SipProvider</tt> as part of the conversation associated with a
     * specific <tt>Dialog</tt>.
     *
     * @param sipProvider the <tt>SipProvider</tt> to send the specified
     * request through
     * @param request the <tt>Request</tt> to send through
     * <tt>sipProvider</tt>
     * @param dialog the <tt>Dialog</tt> as part of which the specified
     * <tt>request</tt> is to be sent
     *
     * @throws OperationFailedException if creating a transaction or sending
     * the <tt>request</tt> fails.
     */
    public void sendInDialogRequest(SipProvider sipProvider,
                                    Request request,
                                    Dialog dialog)
        throws OperationFailedException
    {
        ClientTransaction clientTransaction = null;
        try
        {
            clientTransaction = sipProvider.getNewClientTransaction(request);
        }
        catch (TransactionUnavailableException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create a client transaction for request:\n"
                + request, OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        try
        {
            dialog.sendRequest(clientTransaction);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send SIP request.",
                OperationFailedException.NETWORK_FAILURE,
                ex,
                logger);
        }

        logger.debug("Sent SIP request.");
    }

    /**
     * Returns a List of Strings corresponding to all methods that we have a
     * processor for.
     *
     * @return a List of methods that we support.
     */
    public List<String> getSupportedMethods()
    {
        Set<String> supportedMethods = null;

        // Take a copy of the key set before returning it to avoid
        // synchronization issues.
        synchronized (methodProcessors)
        {
            supportedMethods = new HashSet<>(methodProcessors.keySet());
        }

        return new ArrayList<>(supportedMethods);
    }

    /**
     * A utility class that allows us to block until we receive a
     * <tt>RegistrationStateChangeEvent</tt> notifying us of an unregistration.
     */
    private static class ShutdownUnregistrationBlockListener
        implements RegistrationStateChangeListener
    {
        /**
         * The list where we store <tt>RegistrationState</tt>s received while
         * waiting.
         */
        public List<RegistrationState> collectedNewStates =
                new LinkedList<>();

        /**
         * The method would simply register all received events so that they
         * could be available for later inspection by the unit tests. In the
         * case where a registration event notifying us of a completed
         * registration is seen, the method would call notifyAll().
         *
         * @param evt ProviderStatusChangeEvent the event describing the
         * status change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("Received a RegistrationStateChangeEvent: " + evt);

            collectedNewStates.add(evt.getNewState());

            if (evt.getNewState().equals(RegistrationState.UNREGISTERED))
            {
                logger.debug(
                          "We're unregistered and will notify those who wait");
                synchronized (this)
                {
                    notifyAll();
                }
            }
        }

        /**
         * Blocks until an event notifying us of the awaited state change is
         * received or until waitFor milliseconds pass (whichever happens first).
         *
         * @param waitFor the number of milliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            logger.trace("Waiting for a "
                        +"RegistrationStateChangeEvent.UNREGISTERED");

            synchronized (this)
            {
                if (collectedNewStates.contains(
                        RegistrationState.UNREGISTERED))
                {
                    logger.trace("Event already received. "
                                + collectedNewStates);
                    return;
                }

                try
                {
                    wait(waitFor);

                    if (collectedNewStates.size() > 0)
                        logger.trace(
                                    "Received a RegistrationStateChangeEvent.");
                    else
                        logger.trace(
                                    "No RegistrationStateChangeEvent received for "
                                    + waitFor + "ms.");
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                            "Interrupted while waiting for a "
                            +"RegistrationStateChangeEvent"
                            , ex);
                }
            }
        }
    }

    /**
     * Returns the sip protocol icon.
     * @return the sip protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return protocolIcon;
    }

    /**
     * Returns the current instance of <tt>SipStatusEnum</tt>.
     *
     * @return the current instance of <tt>SipStatusEnum</tt>.
     */
    SipStatusEnum getSipStatusEnum()
    {
        return sipStatusEnum;
    }
    /**
     * Returns the current instance of <tt>SipRegistrarConnection</tt>.
     * @return SipRegistrarConnection
     */
    SipRegistrarConnection getRegistrarConnection()
    {
        return sipRegistrarConnection;
    }

    /**
     * Parses the <tt>uriStr</tt> string and returns a JAIN SIP URI.
     *
     * @param uriStr a <tt>String</tt> containing the uri to parse.
     *
     * @return a URI object corresponding to the <tt>uriStr</tt> string.
     * @throws ParseException if uriStr is not properly formatted.
     */
    public Address parseAddressString(String uriStr)
        throws ParseException
    {
        uriStr = uriStr.trim();
        uriStr = uriStr.replaceAll(IGNORED_CHARS, "");

        if (uriStr.contains(":"))
        {
            // If this string contains : it means that we received a URI call
            // from the OS to trigger a phone call.  We're about to convert
            // whatever type of calling URI was used to invoke this code to a
            // sip: URI for simplicity, so here we log and send an analytic for
            // the specific URI used before we lose that information.  Note that
            // the mainline flow for URI calls received from the OS is via
            // URIHandlerSipImpl instead of here.
            String receivedURI = uriStr.split(":")[0];
            logger.info("Received URI to start a call: " + receivedURI);
            SipActivator.getAnalyticsService().onEvent(AnalyticsEventType.OUTBOUND_CALL,
                                                       "Calling from",
                                                       "URI: " + receivedURI);
        }

        // For simplicity, convert any calling URI to a sip: URI.
        if (uriStr.toLowerCase().startsWith("tel:"))
        {
            uriStr = "sip:" + uriStr.substring("tel:".length());
        }
        else if (uriStr.toLowerCase().startsWith("callto:"))
        {
            uriStr = "sip:" + uriStr.substring("callto:".length());
        }
        else if (uriStr.toLowerCase().startsWith("maxuccall:"))
        {
            uriStr = "sip:" + uriStr.substring("maxuccall:".length());
        }

        //Handle default domain name (i.e. transform 1234 -> 1234@sip.com)
        //assuming that if no domain name is specified then it should be the
        //same as ours.
        if (uriStr.indexOf('@') == -1)
        {
            //if we have a registrar, then we could append its domain name as
            //default
            SipRegistrarConnection src = sipRegistrarConnection;
            if(src != null && !src.isRegistrarless() )
            {
                uriStr = uriStr + "@"
                    + ((SipURI)src.getAddressOfRecord().getURI()).getHost();
            }

            //else this could only be a host ... but this should work as is.
        }

        //Let's be uri fault tolerant and add the sip: scheme if there is none.
        if (!uriStr.toLowerCase().startsWith("sip:")) //no sip scheme
        {
            uriStr = "sip:" + uriStr;
        }

        Address toAddress = getAddressFactory().createAddress(uriStr);

        if (toAddress.getURI().isSipURI())
        {
          SipUri uri = (SipUri) toAddress.getURI();

          if (uri != null && uri.getUser() != null)
          {
              String user = uri.getUser().replaceAll(IGNORED_CHARS, "");
              uri.setUser(user);
          }
        }

        return toAddress;
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be used
     * as a next hop when contacting the specified <tt>destination</tt>. This is
     * a utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers. The method also takes into account
     * the existence of an outbound proxy and in that case returns its address
     * as the next hop.
     *
     * @param destination the destination that we would contact.
     *
     * @return the <tt>InetSocketAddress</tt> that is most likely to be
     * used as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> is not a valid
     * host/ip/fqdn
     */
    public InetSocketAddress getIntendedDestination(Address destination)
        throws IllegalArgumentException
    {
        return getIntendedDestination((SipURI)destination.getURI());
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be used
     * as a next hop when contacting the specified <tt>destination</tt>. This is
     * a utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers. The method also takes into account
     * the existence of an outbound proxy and in that case returns its address
     * as the next hop.
     *
     * @param destination the destination that we would contact.
     *
     * @return the <tt>InetSocketAddress</tt> that is most likely to be
     * used as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> is not a valid
     * host/ip/fqdn
     */
    public InetSocketAddress getIntendedDestination(SipURI destination)
        throws IllegalArgumentException
    {
        return getIntendedDestination(destination.getHost());
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be used
     * as a next hop when contacting the specified <tt>destination</tt>. This is
     * a utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers. The method also takes into account
     * the existence of an outbound proxy and in that case returns its address
     * as the next hop.
     *
     * @param host the destination that we would contact.
     *
     * @return the <tt>InetSocketAddress</tt> that is most likely to be
     * used as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> can't be resolved. It may be a
     * domain domain which we failed to resolve due to e.g. network problems for DNS, so
     * IllegalArgumentException is an odd choice for this exception.
     */
    public InetSocketAddress getIntendedDestination(String host)
        throws IllegalArgumentException
    {
        // Address
        InetSocketAddress destinationInetAddress = null;

        //resolveSipAddress() verifies whether our destination is valid
        //but the destination could only be known to our outbound proxy
        //if we have one. If this is the case replace the destination
        //address with that of the proxy.(report by Dan Bogos)
        InetSocketAddress outboundProxy = connection.getAddress();

        if(outboundProxy != null)
        {
            // To avoid log spam, only log at most every 60 seconds, unless the
            // host or proxy change.
            logger.interval(60, host,
                            "Will use proxy address", outboundProxy);
            destinationInetAddress = outboundProxy;
        }
        else
        {
            ProxyConnection tempConn = new AutoProxyConnection(
                (SipAccountID)getAccountID(),
                host,
                getDefaultTransport());

            if (tempConn.getNextAddress())
            {
                destinationInetAddress = tempConn.getAddress();
            }
            else
            {
                // Note that getNextAddress returning false can be because of network problems causing DNS
                // queries to fail, not necessarily due to an invalid domain.
                throw new IllegalArgumentException(host + " could not be resolved to an internet address.");
            }
        }

        logger.debug("Returning address " + destinationInetAddress
             + " for destination " + host);

        return destinationInetAddress;
    }

    /**
     * Stops dispatching SIP messages to a SIP protocol provider service
     * once it's been unregistered.
     *
     * @param event the change event in the registration state of a provider.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent event)
    {
        if(event.getNewState() == RegistrationState.UNREGISTERED ||
           event.getNewState() == RegistrationState.CONNECTION_FAILED)
        {
            logger.info(this + " is now unregistered/failed");
            ProtocolProviderServiceSipImpl listener
                = (ProtocolProviderServiceSipImpl) event.getProvider();
            sipStackSharing.removeSipListener(listener);
            listener.removeRegistrationStateChangeListener(this);
        }
    }

    /**
     * Logs a specific message and associated <tt>Throwable</tt> cause as an
     * error using the current <tt>Logger</tt> and then throws a new
     * <tt>OperationFailedException</tt> with the message, a specific error code
     * and the cause.
     *
     * @param message the message to be logged and then wrapped in a new
     * <tt>OperationFailedException</tt>
     * @param errorCode the error code to be assigned to the new
     * <tt>OperationFailedException</tt>
     * @param cause the <tt>Throwable</tt> that has caused the necessity to log
     * an error and have a new <tt>OperationFailedException</tt> thrown
     * @param logger the logger that we'd like to log the error <tt>message</tt>
     * and <tt>cause</tt>.
     *
     * @throws OperationFailedException the exception that we wanted this method
     * to throw.
     */
    public static void throwOperationFailedException( String    message,
                                                      int       errorCode,
                                                      Throwable cause,
                                                      Logger    logger)
        throws OperationFailedException
    {
        logger.error(message, cause);

        if(cause == null)
            throw new OperationFailedException(message, errorCode);
        else
            throw new OperationFailedException(message, errorCode, cause);
    }

    /**
     * Early process an incoming message from interested listeners.
     * @param message the message to process.
     */
    void earlyProcessMessage(EventObject message)
    {
        synchronized(earlyProcessors)
        {
            for (SipMessageProcessor listener : earlyProcessors)
            {
                try
                {
                    if(message instanceof RequestEvent)
                        listener.processMessage((RequestEvent)message);
                    else if(message instanceof ResponseEvent)
                        listener.processResponse((ResponseEvent)message, null);
                    else  if(message instanceof TimeoutEvent)
                        listener.processTimeout((TimeoutEvent)message, null);
                }
                catch(Throwable t)
                {
                    logger.error("Error pre-processing message", t);
                }
            }
        }
    }

    /**
     * Finds the next address to retry registering. If it doesn't process anything
     * (we have already tried the last one) return false.
     *
     * @return <tt>true</tt> if we triggered new register with next address.
     */
    boolean registerUsingNextAddress()
    {
        if(connection == null)
            return false;

        try
        {
            if(sipRegistrarConnection.isRegistrarless())
            {
                sipRegistrarConnection.setTransport(getDefaultTransport());
                sipRegistrarConnection.register();
                return true;
            }
            else if(connection.getNextAddress())
            {
                sipRegistrarConnection.setTransport(connection.getTransport());
                sipRegistrarConnection.register();
                return true;
            }
        }
        catch (Throwable e)
        {
            logger.error("Cannot send register!", e);
            sipRegistrarConnection.setRegistrationState(
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                "A timeout occurred while trying to connect to the server.");
        }

        // as we reached the last address lets change it to the first one
        // so we don't get stuck to the last one forever, and the next time
        // use again the first one
        connection.reset();
        return false;
    }

    /**
     * If somewhere we got for example timeout of receiving answer to our
     * requests we consider problem with network and notify the provider.
     */
    protected void notifyConnectionFailed()
    {
        if(getRegistrationState().equals(RegistrationState.REGISTERED)
            && sipRegistrarConnection != null)
            sipRegistrarConnection.setRegistrationState(
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                "A timeout occurred while trying to connect to the server.");

        if(registerUsingNextAddress())
            return;

        // don't alert the user if we're already off
        if (!getRegistrationState().equals(RegistrationState.UNREGISTERED))
        {
            sipRegistrarConnection.setRegistrationState(
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                "A timeout occurred while trying to connect to the server.");
        }
    }

    /**
     * Determines whether the supplied transport is a known SIP transport method
     *
     * @param transport the SIP transport to check
     * @return True when transport is one of UDP, TCP or TLS.
     */
    public static boolean isValidTransport(String transport)
    {
        return ListeningPoint.UDP.equalsIgnoreCase(transport)
            || ListeningPoint.TLS.equalsIgnoreCase(transport)
            || ListeningPoint.TCP.equalsIgnoreCase(transport);
    }

    @Override
    public boolean supportsStatus()
    {
        // Status is based on presence - it is only supported if presence is
        // enabled
        return accountID.getAccountPropertyBoolean(
                             ProtocolProviderFactory.IS_PRESENCE_ENABLED, true);
    }

    public boolean hasIpChanged()
    {
        return sipRegistrarConnection.routeToRegistrarChanged();
    }

    /**
     * <p>This method has been implemented by the SIP protocol provider and,
     * when called, causes the provider to re-register and re-subscribe for
     * SIP presence.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public void pollConnection()
    {
        logger.debug("Polling connection");

        // Simply ask the registrar to re-register now.  There's no harm in
        // re-registering early if we are connected, and if the registration
        // fails then we will know that we aren't connected. We delay
        // this by 1s for safety
        sendReRegister(1);
    }

    /**
     * Send a re-register to the server, either to check whether our route to
     * the registrar has changed, or to reestablish connection after a Perimeta
     * SPS. If our route to the registrar has changed, we will also
     * re-subscribe for SIP notifys.
     *
     * @param delay how long to wait until sending the re-register
     */
    public void sendReRegister(int delay)
    {
        logger.debug("Asked to send re-register in " + delay + "s");
        if (sipRegistrarConnection != null)
        {
            synchronized (mReSubscribeLock)
            {
                // If routeToRegistrarChanged is already true, we must have
                // already done this so no need to repeat.
                if (!mRouteToRegistrarChanged)
                {
                    mRouteToRegistrarChanged =
                              sipRegistrarConnection.routeToRegistrarChanged();

                    logger.info("Route to registrar changed? " +
                                                   mRouteToRegistrarChanged);

                    if (mRouteToRegistrarChanged)
                    {
                        // The route to our registrar has changed since we
                        // last registered, so any existing SIP presence
                        // subscription won't be working anymore.  If we have
                        // a presence op set, tell it that we're unregistering
                        // so that we can cleanly re-subscribe when we
                        // re-register.
                        OperationSetPresenceSipImpl opSetPersPresence =
                            (OperationSetPresenceSipImpl) getOperationSet(
                                OperationSetPersistentPresence.class);
                        if (opSetPersPresence != null)
                        {
                            opSetPersPresence.registrationStateUnregistering();
                        }
                        else
                        {
                            logger.debug("Presence op set is null - " +
                                             "cannot unregister SIP presence");
                        }
                    }
                }
            }

            sipRegistrarConnection.reRegister(delay);
        }
        else
        {
            logger.warn("Unable to re-register as sipRegistrarConnection is null");
        }
    }

    /**
     * Inner class that handles the logic to work out whether we need to
     * retry reregisters following a connection failure.
     */
    private class WaitingForReregister
    {
        // Time we were created, so we can work out when to give up.
        private final long mStartTime;

        public WaitingForReregister()
        {
            mStartTime = System.currentTimeMillis();
        }

        /*
         * Time for the next reregister time (in seconds), or 0 if we
         * are done retrying.
         */
        int retryReregisterTime()
        {
            int result = 0;
            long timeNow = System.currentTimeMillis();

            logger.info("Should retry register?");

            // Are we within the retry time?
            // If so, are there any active calls?
            // If so, it's worth trying still, otherwise not (0).
            if (((timeNow - mStartTime) > 0) &&
                ((timeNow - mStartTime) < REREGISTER_RETRY_TIME))
            {
                logger.debug("Within retry time");

                OperationSetBasicTelephony<?> opSetBasicTelephony =
                    getOperationSet(OperationSetBasicTelephony.class);

                if (opSetBasicTelephony instanceof OperationSetBasicTelephonySipImpl)
                {
                    if (((OperationSetBasicTelephonySipImpl) opSetBasicTelephony).getActiveCalls().hasNext())
                    {
                        logger.debug("Have active calls");
                        result = REREGISTER_DELAY;
                    }
                }
            }

            // Null out the WaitingForReregister object as we are done
            // with retrying since we don't want to retry once one has failed.
            if (result == 0)
            {
                logger.debug("Setting waitingForRegister to null");
                waitingForReregister = null;
            }

            return result;
        }

        /*
         * Success! Reinvite all the active calls.
         */
        void retryRegisterSuccess()
        {
            logger.debug("We have registration success");

            // First set this back to null to make sure we don't get called
            // again while we're already re-inviting and re-subscribing.
            waitingForReregister = null;

            OperationSetBasicTelephony<?> opSetBasicTelephony =
                getOperationSet(OperationSetBasicTelephony.class);

            if (opSetBasicTelephony instanceof OperationSetBasicTelephonySipImpl)
            {
                logger.info("Reinvite active calls");
                ((OperationSetBasicTelephonySipImpl) opSetBasicTelephony).reinviteAllInProgressCalls();
            }

            synchronized (mReSubscribeLock)
            {
                // If the route to our registrar changed since our last
                // register and we have a presence op set, we will also need
                // to re-subscribe for SIP presence.
                if (mRouteToRegistrarChanged)
                {
                    logger.info(
                        "IP changed since last register so subscribe SIP presence");
                    OperationSetPresenceSipImpl opSetPersPresence =
                        (OperationSetPresenceSipImpl) getOperationSet(
                            OperationSetPersistentPresence.class);
                    if (opSetPersPresence != null)
                    {
                        // Set routeToRegistrarChanged to false to be sure we
                        // don't try to subscribe unnecessarily.
                        mRouteToRegistrarChanged = false;
                        opSetPersPresence.registrationStateRegistered();
                    }
                    else
                    {
                        logger.debug("Presence op set is null - " +
                                               "cannot register SIP presence");
                    }
                }
            }
        }
    }

}
