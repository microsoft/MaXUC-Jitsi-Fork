/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.service.insights.InsightsEventHint.JABBER_IM_LOG_IN;
import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatAddress;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509TrustManager;
import javax.swing.SwingUtilities;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.annotations.VisibleForTesting;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.javax.SASLPlainMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.SmackException.EndpointConnectionException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByClientException;
import org.jivesoftware.smack.SmackException.SmackSaslException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.impl.protocol.jabber.debugger.SmackPacketDebugger;
import net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive.KeepAliveManager;
import net.java.sip.communicator.impl.protocol.jabber.extensions.messagearchiving.ArchiveManager;
import net.java.sip.communicator.impl.protocol.jabber.extensions.messagecorrection.MessageCorrectionExtension;
import net.java.sip.communicator.impl.protocol.jabber.extensions.version.VersionManager;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.PreLoginUtils;
import net.java.sip.communicator.plugin.jabberaccregwizz.JabberAccountRegistrationWizard;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.gui.CreateAccountWindow;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.gui.WizardContainer;
import net.java.sip.communicator.service.insights.parameters.JabberParameterInfo;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.protocol.AbstractProtocolProviderService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetAvatar;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetChangePassword;
import net.java.sip.communicator.service.protocol.OperationSetContactCapabilities;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations;
import net.java.sip.communicator.service.protocol.OperationSetFileTransfer;
import net.java.sip.communicator.service.protocol.OperationSetGenericNotifications;
import net.java.sip.communicator.service.protocol.OperationSetInstantMessageTransform;
import net.java.sip.communicator.service.protocol.OperationSetInstantMessageTransformImpl;
import net.java.sip.communicator.service.protocol.OperationSetMessageCorrection;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredAccountInfo;
import net.java.sip.communicator.service.protocol.OperationSetSpecialMessaging;
import net.java.sip.communicator.service.protocol.OperationSetThumbnailedFileFactory;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.ProtocolIcon;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProxyInfo;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.jabberconstants.JabberStatusEnum;
import net.java.sip.communicator.service.threading.CancellableRunnable;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.JitsiStringUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.NetworkUtils;
import net.java.sip.communicator.util.SRVRecord;
import net.java.sip.communicator.util.ServerBackoff;
import net.java.sip.communicator.service.threading.ThreadFactoryBuilder;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;

/**
 * An implementation of the protocol provider service over the Jabber protocol
 *
 * @author Damian Minkov
 * @author Symphorien Wanko
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 * @author Emil Ivov
 */
public class ProtocolProviderServiceJabberImpl
    extends AbstractProtocolProviderService
    implements ServiceListener
{
    /**
     * Logger of this class
     */
    private static final Logger sLog =
        Logger.getLogger(ProtocolProviderServiceJabberImpl.class);

    /**
     * Roster requests can take longer than other messages to get a response, so use a custom
     * (larger) packet reply timeout for them.
     */
    public static final int CUSTOM_SMACK_PACKET_REPLY_TIMEOUT_FOR_ROSTER = 45000;

    /**
     * Property for vcard reply timeout. Time to wait before
     * we think vcard retrieving has timeouted, default value
     * of smack is 5000 (5 sec.).
     */
    public static final String VCARD_REPLY_TIMEOUT_PROPERTY =
        "net.java.sip.communicator.impl.protocol.jabber.VCARD_REPLY_TIMEOUT";

    /**
     * XMPP signalling DSCP configuration property name.
     */
    private static final String XMPP_DSCP_PROPERTY =
        "net.java.sip.communicator.impl.protocol.XMPP_DSCP";

    /**
     * Name of the provisioning username in the configuration service.
     */
    static final String PROPERTY_PROVISIONING_USERNAME
            = "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";

    /**
     * Used to connect to a XMPP server.
     */
    private XMPPTCPConnection mConnection;

    /**
     * The local IP address currently being used for sending and receiving XMPP
     * messages. May be null if we have failed to resolve our IP.
     */
    private InetAddress mCurrentLocalIp;

    /**
     * True if the XMPP connection is connected, and not disconnecting or just
     * about to be disconnected.  Required because closing the connection can
     * take some time
     */
    private boolean mIsConnected;

    /**
     * Indicates whether or not the provider is initialized and ready for use.
     */
    private boolean mIsInitialized = false;

    /**
     * We use this to lock access to initialization.
     */
    private final Object mInitializationLock = new Object();

    /**
     * The identifier of the account that this provider represents.
     */
    private AccountID mAccountID = null;

    /**
     * Used when we need to re-register
     */
    private SecurityAuthority mAuthority = null;

    /**
     * The resource we will use when connecting during this run.
     */
    private String mResource = null;

    /**
     * The icon corresponding to the jabber protocol.
     */
    private ProtocolIconJabberImpl mJabberIcon;

    /**
     * A set of features supported by our Jabber implementation.
     * In general, we add new feature(s) when we add new operation sets.
     * (see xep-0030 : http://www.xmpp.org/extensions/xep-0030.html#info).
     * Example : to tell the world that we support jingle, we simply have
     * to do :
     * supportedFeatures.add("http://www.xmpp.org/extensions/xep-0166.html#ns");
     * Beware there is no canonical mapping between op set and jabber features
     * (op set is a SC "concept"). This means that one op set in SC can
     * correspond to many jabber features. It is also possible that there is no
     * jabber feature corresponding to a SC op set or again,
     * we can currently support some features wich do not have a specific
     * op set in SC (the mandatory feature :
     * http://jabber.org/protocol/disco#info is one example).
     * We can find features corresponding to op set in the xep(s) related
     * to implemented functionality.
     */
    private final List<String> mSupportedFeatures = new ArrayList<>();

    /**
     * The <tt>OperationSetContactCapabilities</tt> of this
     * <tt>ProtocolProviderService</tt> which is the service-public counterpart
     * of {@link ServiceDiscoveryManager}.
     */
    private OperationSetContactCapabilitiesJabberImpl mOpsetContactCapabilities;

    /**
     * The jabber implementation of multi-user chat.
     */
    private OperationSetMultiUserChatJabberImpl mOpsetMuc;

    private OperationSetPersistentPresenceJabberImpl presenceOpSet;

    /**
     * The chat room manager, responsible for joining and leaving chat rooms.
     */
    private ChatRoomManager mChatRoomManager;

    /**
     * The statuses.
     */
    private JabberStatusEnum mJabberStatusEnum;

    private ConfigurationService configService;

    /**
     * Used with tls connecting when certificates are not trusted
     * and we ask the user to confirm connection. When some timeout expires
     * connect method returns, and we use abortConnecting to abort further
     * execution cause after user chooses we make further processing from there.
     */
    private boolean mAbortConnecting = false;

    /**
     * Flag indicating are we currently executing connectAndLogin method.
     */
    private boolean mInConnectAndLogin = false;

    /**
     * Object used to synchronize the flag inConnectAndLogin.
     */
    private final Object mConnectAndLoginLock = new Object();

    /**
     * If an event occurs during login we fire it at the end of the login
     * process (at the end of connectAndLogin method).
     */
    private RegistrationStateChangeEvent mEventDuringLogin;

    /**
     * Listens for connection closes or errors.
     */
    private JabberConnectionListener mConnectionListener;

    /**
     * The details of the proxy we are using to connect to the server (if any)
     */
    private org.jivesoftware.smack.proxy.ProxyInfo mProxy = null;

    /**
     * State for connect and login state.
     */
    enum ConnectState
    {
        /**
         * Abort any further connecting.
         */
        ABORT_CONNECTING,
        /**
         * Continue trying with next address.
         */
        CONTINUE_TRYING,
        /**
         * Stop trying we succeeded or just have a final state for
         * the whole connecting procedure.
         */
        STOP_TRYING
    }

    /**
     * The debugger who logs packets.
     */
    private SmackPacketDebugger mDebugger = null;

    /**
     * The currently running keepAliveManager if enabled.
     */
    private KeepAliveManager mKeepAliveManager = null;

    /**
     * The version manager.
     */
    private VersionManager mVersionManager = null;

    /**
     * The UI Service.
     */
    private static UIService sUiService = null;

    /**
     * The bundle context.
     */
    private static BundleContext sBundleContext = JabberActivator.getBundleContext();

    /**
     * boolean indicating whether or not the MclStorageManager has finished
     * loading unresolved contacts from the local store (contactlist.xml)
     */
    private boolean mLoadedAllUnresolvedContacts;

    /**
     * Archive Manager - used to tell if Message Archiving is supported and, if
     * so, make the archive requests and handle the results of said requests
     */
    private ArchiveManager mArchiveManager;

    private final ThreadingService threadingService = JabberActivator.getThreadingService();

    /**
     * Auxiliary executor service for the task that will reset the XMPP
     * connection on macOS when the system root certificate keychain is updated.
     */
    private static ExecutorService reloadExecutor =
            Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder()
                            .setName("xmpp-keychain-monitor-thread")
                            .build());

    /**
     * Backoff to use when dealing with authentication failures.
     */
    private static final String CONFIG_INITIAL_FAIL_BACKOFF =
        "net.java.sip.communicator.impl.protocol.jabber.initialfailbackoff";
    private static final String CONFIG_MAX_NUMBER_FAILURE_DOUBLES =
        "net.java.sip.communicator.impl.protocol.jabber.maxnumberfailuredoubles";
    private static final long DEFAULT_INITIAL_FAIL_BACKOFF = 2000;
    /** Double the backoff interval 8 times by default (~ 9 minutes). */
    private static final int DEFAULT_MAX_NUMBER_FAILURE_DOUBLES = 8;
    private final ServerBackoff mAuthenticationBackoff;

    public ProtocolProviderServiceJabberImpl ()
    {
        configService = JabberActivator.getConfigurationService();
        mAuthenticationBackoff = new ServerBackoff(
            "ProtocolProviderServiceJabberImpl",
            configService.global().getInt(CONFIG_MAX_NUMBER_FAILURE_DOUBLES, DEFAULT_MAX_NUMBER_FAILURE_DOUBLES),
            configService.global().getLong(CONFIG_INITIAL_FAIL_BACKOFF, DEFAULT_INITIAL_FAIL_BACKOFF));
        sLog.info("Created PPSJabberImpl " + this + " with backoff " + mAuthenticationBackoff);
    }

    /**
     * Returns the state of the registration of this protocol provider
     * @return the <tt>RegistrationState</tt> that this provider is
     * currently in or null in case it is in a unknown state.
     */
    public RegistrationState getRegistrationState()
    {
        if(mConnection == null)
            return RegistrationState.UNREGISTERED;
        else if(mConnection.isConnected() && mConnection.isAuthenticated())
            return RegistrationState.REGISTERED;
        else
            return RegistrationState.UNREGISTERED;
    }

    /**
     * Starts the registration process. Connection details such as
     * registration server, user name/number are provided through the
     * configuration service through implementation specific properties.
     *
     * @param authority the security authority that will be used for resolving
     *        any security challenges that may be returned during the
     *        registration or at any moment while we're registered.
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    public void register(final SecurityAuthority authority)
        throws OperationFailedException
    {
        sLog.info("Registering jabber " + this);

        synchronized (this)
        {
            if (!mLoadedAllUnresolvedContacts)
            {
                // The MclStorageManager hasn't yet finished loading all the
                // unresolved contacts from the local storage (contactlist.xml)
                // thus we don't want to start getting contacts yet.
                sLog.info("Not yet loaded all contacts " + this);
                return;
            }

            if (getRegistrationState().equals(RegistrationState.REGISTERED))
            {
                sLog.info("Jabber provider already registered");
                return;
            }

            if(authority == null)
                throw new IllegalArgumentException(
                    "The register method needs a valid non-null authority impl "
                    + " in order to be able and retrieve passwords.");

            this.mAuthority = authority;

            if (mArchiveManager != null)
            {
                mArchiveManager.stop();
            }

            mArchiveManager = new ArchiveManager(this);

            try
            {
                // reset states
                mAbortConnecting = false;

                // indicate we started connectAndLogin process
                synchronized(mConnectAndLoginLock)
                {
                    mInConnectAndLogin = true;
                }

                connectAndLogin(authority, SecurityAuthority.AUTHENTICATION_REQUIRED);

                onAuthenticationSucceeded();
            }
            catch (XMPPException | SmackException | InterruptedException | IOException
                ex)
            {
                sLog.error(
                    "Error registering " + this.getAccountID().getLoggableAccountID(), ex);

                mEventDuringLogin = null;

                fireRegistrationStateChanged(ex);
            }
            finally
            {
                synchronized(mConnectAndLoginLock)
                {
                    // Checks if an error has occurred during login, if so we
                    // fire it here in order to avoid a deadlock which occurs
                    // in reconnect plugin. The deadlock is cause we fired an
                    // event during login process and have locked
                    // initializationLock and we cannot unregister from
                    // reconnect, cause unregister method also needs this lock.
                    if(mEventDuringLogin != null)
                    {
                        if(mEventDuringLogin.getNewState().equals(
                                RegistrationState.CONNECTION_FAILED) ||
                            mEventDuringLogin.getNewState().equals(
                                RegistrationState.UNREGISTERED))
                            disconnectAndCleanConnection();

                        fireRegistrationStateChanged(
                            mEventDuringLogin.getOldState(),
                            mEventDuringLogin.getNewState(),
                            mEventDuringLogin.getReasonCode(),
                            mEventDuringLogin.getReason());

                        mEventDuringLogin = null;
                        mInConnectAndLogin = false;
                        return;
                    }

                    mInConnectAndLogin = false;
                }
            }
        }
    }

    /**
     * Connects and logins again to the server.
     *
     * @param authReasonCode indicates the reason of the re-authentication.
     */
    void reregister(int authReasonCode)
    {
        try
        {
            sLog.info("Trying to reregister: " + this);

            // sets this if any is tring to use us through registration
            // to know we are not registered
            this.unregister(false);

            // reset states
            this.mAbortConnecting = false;

            // indicate we started connectAndLogin process
            synchronized(mConnectAndLoginLock)
            {
                mInConnectAndLogin = true;
            }

            connectAndLogin(mAuthority,
                            authReasonCode);

            onAuthenticationSucceeded();
        }
        catch(OperationFailedException ex)
        {
            sLog.error("Error ReRegistering" + getAccountID().getLoggableAccountID(),
                         ex);

            mEventDuringLogin = null;

            disconnectAndCleanConnection();

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_INTERNAL_ERROR, null);
        }
        catch (XMPPException | InterruptedException | IOException | SmackException
            ex)
        {
            sLog.error("Error ReRegistering" + getAccountID().getLoggableAccountID(),
                         ex);

            mEventDuringLogin = null;

            fireRegistrationStateChanged(ex);
        }
        finally
        {
            synchronized(mConnectAndLoginLock)
            {
                // Checks if an error has occurred during login, if so we fire
                // it here in order to avoid a deadlock which occurs in
                // reconnect plugin. The deadlock is cause we fired an event
                // during login process and have locked initializationLock and
                // we cannot unregister from reconnect, cause unregister method
                // also needs this lock.
                if(mEventDuringLogin != null)
                {
                    if(mEventDuringLogin.getNewState().equals(
                            RegistrationState.CONNECTION_FAILED) ||
                        mEventDuringLogin.getNewState().equals(
                            RegistrationState.UNREGISTERED))
                        disconnectAndCleanConnection();

                    fireRegistrationStateChanged(
                        mEventDuringLogin.getOldState(),
                        mEventDuringLogin.getNewState(),
                        mEventDuringLogin.getReasonCode(),
                        mEventDuringLogin.getReason());

                    mEventDuringLogin = null;
                    mInConnectAndLogin = false;
                    return;
                }

                mInConnectAndLogin = false;
            }
        }
    }

    /**
     * Returns runnable that re-registers with the given {@code authReasonCode}
     * @param authReasonCode the reason why re-registration is being attempted.
     * @return a cancellable runnable to re-register with the given reason code.
     */
    CancellableRunnable getReregisterRunnable(final int authReasonCode)
    {
        return new CancellableRunnable()
        {
            @Override
            public void run()
            {
                reregister(authReasonCode);
            }
        };
    }

    /**
     * Indicates if the XMPP transport channel is using a TLS secured socket.
     *
     * @return True when TLS is used, false otherwise.
     */
    public boolean isSignalingTransportSecure()
    {
        return mConnection != null && mConnection.isSecureConnection();
    }

    /**
     * Connects and logins to the server
     * @param authority SecurityAuthority
     * @param reasonCode the authentication reason code. Indicates the reason of
     * this authentication.
     * @throws XMPPException if we cannot connect to the server - network problem
     * @throws  OperationFailedException if login parameters
     *          as server port are not correct
     */
    private void connectAndLogin(SecurityAuthority authority,
                                              int reasonCode)
        throws OperationFailedException, XMPPException, SmackException,
            InterruptedException, IOException
    {
        sLog.debug("connect with reason " + reasonCode);

        synchronized(mInitializationLock)
        {
            if (mArchiveManager == null)
            {
                mArchiveManager = new ArchiveManager(this);
            }

            JabberLoginStrategy loginStrategy = createLoginStrategy();
            loginStrategy.prepareLogin(authority, reasonCode);

            if (!loginStrategy.loginPreparationSuccessful())
                return;

            String serviceName
                = JitsiStringUtils.parseServer(getAccountID().getUserID());

            loadResource();
            loadProxy();
            Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);

            ConnectState state = ConnectState.CONTINUE_TRYING;

            // try connecting with auto-detection if enabled
            boolean isServerOverriden =
                getAccountID().getAccountPropertyBoolean(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN, false);

            if(!isServerOverriden)
            {
                state = connectUsingSRVRecords(serviceName, serviceName, loginStrategy);
                if(state == ConnectState.ABORT_CONNECTING
                    || state == ConnectState.STOP_TRYING)
                    return;
            }

            // check for custom xmpp domain which we will check for
            // SRV records for server addresses
            String customXMPPDomain = getAccountID()
                .getAccountPropertyString("CUSTOM_XMPP_DOMAIN");

            if (customXMPPDomain != null)
            {
                sLog.info("Connect using custom xmpp domain: " +
                    customXMPPDomain);

                state = connectUsingSRVRecords(customXMPPDomain, serviceName, loginStrategy);

                sLog.info("state for connectUsingSRVRecords: " + state);

                if(state == ConnectState.ABORT_CONNECTING
                    || state == ConnectState.STOP_TRYING)
                    return;
            }

            // connect with specified server name
            String serverAddressUserSetting
                = getAccountID().getAccountPropertyString(
                    ProtocolProviderFactory.SERVER_ADDRESS);

            int serverPort = getAccountID().getAccountPropertyInt(
                    ProtocolProviderFactory.SERVER_PORT, 5222);

            InetSocketAddress[] addrs = null;
            try
            {
                addrs = NetworkUtils.getAandAAAARecords(
                    serverAddressUserSetting,
                    serverPort
                );
            }
            catch (ParseException e)
            {
                sLog.error("Domain not resolved: " + serverAddressUserSetting,
                             e);
            }

            if (addrs == null || addrs.length == 0)
            {
                sLog.error("No server addresses found.  State = " + state);

                // Just throw an exception so that the calling code will tidy
                // up the connection and retry.
                throw new JitsiXmppException("No server addresses found");
            }
            else
            {
                for (InetSocketAddress isa : addrs)
                {
                    try
                    {
                        state = connectAndLogin(isa, serviceName,
                            loginStrategy);
                        if(state == ConnectState.ABORT_CONNECTING
                            || state == ConnectState.STOP_TRYING)
                            return;
                    }
                    catch(XMPPException | InterruptedException | IOException |
                        SmackException ex)
                    {
                        disconnectAndCleanConnection();
                        if(isAuthenticationFailed(ex))
                            throw ex;
                    }
                }
            }

            // If we've failed to connect, we need to ensure that the
            // connection is retried later so throw an exception here to kick
            // the calling code.
            sLog.warn("Failed to connect XMPP account " +
                                                   getAccountID().getLoggableAccountID());
            throw new JitsiXmppException("Failed to connect to XMPP server");
        }
    }

    /**
     * Creates the JabberLoginStrategy to use for the current account.
     */
    private JabberLoginStrategy createLoginStrategy()
    {
        return new LoginByPasswordStrategy(this, getAccountID());
    }

    /**
     * Connects using the domain specified and its SRV records.
     * @param domain the domain to use
     * @param serviceName the domain name of the user's login
     * @param loginStrategy the login strategy to use
     * @return whether to continue trying or stop.
     */
    private ConnectState connectUsingSRVRecords(
        String domain,
        String serviceName,
        JabberLoginStrategy loginStrategy)
        throws XMPPException, SmackException, InterruptedException, IOException
    {
        // check to see is there SRV records for this server domain
        SRVRecord srvRecords[] = null;
        try
        {
            srvRecords = NetworkUtils
                .getSRVRecords("xmpp-client", "tcp", domain);
        }
        catch (ParseException e)
        {
            sLog.error("SRV record not resolved for " + domain, e);
        }

        if(srvRecords != null)
        {
            StringBuilder logLine = new StringBuilder("Connect using SRV records: ");
            for (SRVRecord srv : srvRecords)
            {
                logLine.append(srv + ", ");
            }
            sLog.debug(logLine);

            for(SRVRecord srv : srvRecords)
            {
                InetSocketAddress[] addrs = null;
                try
                {
                    addrs =
                        NetworkUtils.getAandAAAARecords(
                            srv.getTarget(),
                            srv.getPort()
                        );
                }
                catch (ParseException e)
                {
                    sLog.error("Invalid SRV record target for " +
                                 srv.getTarget(), e);
                }

                if (addrs == null || addrs.length == 0)
                {
                    sLog.error("No A/AAAA addresses found for " +
                        srv.getTarget());
                    continue;
                }

                logLine = new StringBuilder("Attempt to connect using addresses: ");
                for (InetSocketAddress isa : addrs)
                {
                    logLine.append(isa + ", ");
                }
                sLog.debug(logLine);

                for (InetSocketAddress isa : addrs)
                {
                    try
                    {
                        ConnectState state = connectAndLogin(isa, serviceName, loginStrategy);
                        sLog.debug("Address " + isa + " connectState " + state);
                        return state;
                    }
                    catch (XMPPException | SmackException | InterruptedException | IOException
                        ex)
                    {
                        sLog.error("Error connecting to " + isa
                            + " for domain:" + domain
                            + " serviceName:" + serviceName, ex);

                        if(isAuthenticationFailed(ex))
                            throw ex;
                    }
                }
            }
        }
        else
        {
            sLog.warn("No SRV addresses found for _xmpp-client._tcp."
                + domain);
        }

        sLog.warn("XMPP connection failed via SRV records, will try A/AAAA");

        return ConnectState.CONTINUE_TRYING;
    }

    /**
     * Tries to login to the XMPP server with the supplied user ID. If the
     * protocol is Google Talk, the user ID including the service name is used.
     * For other protocols, if the login with the user ID without the service
     * name fails, a second attempt including the service name is made.
     *
     * @param currentAddress the IP address to connect to
     * @param serviceName the domain name of the user's login
     * @param loginStrategy the login strategy to use
     * @throws XMPPException
     * @throws IOException
     * @throws InterruptedException
     * @throws SmackException
     */
    private ConnectState connectAndLogin(InetSocketAddress currentAddress,
        String serviceName,
        JabberLoginStrategy loginStrategy)
        throws XMPPException, IOException, InterruptedException, SmackException
    {
        sLog.debug("connect to " + serviceName + " at " + currentAddress);

        String userID = JitsiStringUtils.parseName(getAccountID().getUserID());

        try
        {
            return connectAndLogin(
                currentAddress, serviceName,
                userID, mResource, loginStrategy);
        }
        catch(XMPPException | IOException | InterruptedException | SmackException ex)
        {
            // server disconnect us after such an error, do cleanup
            disconnectAndCleanConnection();

            // no need to check with a different username if the
            // socket could not be opened
            if (ex instanceof UnknownHostException || ex instanceof SocketException
                || ex instanceof ConnectionException)
            {
                //as we got an exception not handled in connectAndLogin
                //no state was set, so fire it here so we can continue
                //with the re-register process
                fireRegistrationStateChanged(getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND, null);
            }

            throw ex;
        }
    }

    /**
     * Initializes the Jabber Resource identifier.
     *
     * For Stream Management to work correctly, we should always connect to the
     * IM server using the same resource identifier. This allows the server to
     * merge multiple streams from the same client correctly.  Therefore we only
     * generate a new Jabber Resource Identifier if there is not one already
     * stored in our settings.
     */
    private void loadResource()
    {
        if(mResource == null)
        {
            // The resource ID must start with "jitsi", AMS relies on it to identify Accession clients.
            String defaultResource = "jitsi";
            mResource = getAccountID().getAccountPropertyString(
                    ProtocolProviderFactory.RESOURCE);
            String autoGenenerateResource =
                getAccountID().getAccountPropertyString(
                    ProtocolProviderFactory.AUTO_GENERATE_RESOURCE);

            if(mResource == null ||
               mResource.length() == 0 ||
               mResource.equals(defaultResource))
            {
                if(autoGenenerateResource == null ||
                    Boolean.parseBoolean(autoGenenerateResource))
                {
                    SecureRandom random = new SecureRandom();

                    mResource = defaultResource + "-" +
                        new BigInteger(32, random).toString(32);
                }
                else
                {
                    mResource = defaultResource;
                }
            }

            // Set the resource on the account to match the one we are using.
            mAccountID.putAccountProperty(ProtocolProviderFactory.RESOURCE,
                                         mResource);

            // Store the account so the property is stored in config and will
            // persist over restart.
            JabberActivator.getProtocolProviderFactory().storeAccount(mAccountID);
        }
    }

    /**
     * Sets the global proxy information based on the configuration
     *
     * @throws OperationFailedException
     */
    private void loadProxy() throws OperationFailedException
    {
        String globalProxyType =
            JabberActivator.getConfigurationService().global()
            .getString(ProxyInfo.CONNECTON_PROXY_TYPE_PROPERTY_NAME);

        if (globalProxyType == null || globalProxyType.equals(ProxyInfo.ProxyType.NONE.name()))
        {
            mProxy = null;
        }
        else
        {
            String globalProxyAddress =
                JabberActivator.getConfigurationService().global().getString(
                ProxyInfo.CONNECTON_PROXY_ADDRESS_PROPERTY_NAME);
            String globalProxyPortStr =
                JabberActivator.getConfigurationService().global().getString(
                ProxyInfo.CONNECTON_PROXY_PORT_PROPERTY_NAME);
            int globalProxyPort;

            try
            {
                globalProxyPort = Integer.parseInt(
                    globalProxyPortStr);
            }
            catch(NumberFormatException ex)
            {
                throw new OperationFailedException("Wrong proxy port, "
                        + globalProxyPortStr
                        + " does not represent an integer",
                    OperationFailedException.INVALID_ACCOUNT_PROPERTIES,
                    ex);
            }

            String globalProxyUsername =
                JabberActivator.getConfigurationService().global().getString(
                ProxyInfo.CONNECTON_PROXY_USERNAME_PROPERTY_NAME);
            String globalProxyPassword =
                JabberActivator.getConfigurationService().global().getString(
                ProxyInfo.CONNECTON_PROXY_PASSWORD_PROPERTY_NAME);

            if (globalProxyAddress == null || globalProxyAddress.length() <= 0)
            {
                throw new OperationFailedException(
                    "Missing Proxy Address",
                    OperationFailedException.INVALID_ACCOUNT_PROPERTIES);
            }

            try
            {
                mProxy = new org.jivesoftware.smack.proxy.ProxyInfo(
                    Enum.valueOf(org.jivesoftware.smack.proxy.ProxyInfo.
                        ProxyType.class, globalProxyType),
                    globalProxyAddress, globalProxyPort,
                    globalProxyUsername, globalProxyPassword);
            }
            catch (IllegalArgumentException ex)
            {
                sLog.error("Invalid value for smack proxy enum", ex);
                mProxy = null;
            }
        }
    }

    /**
     * Connects xmpp connection and login. Returning the state whether is it
     * final - Abort due to certificate cancel or keep trying cause only current
     * address has failed or stop trying cause we succeeded.
     * @param address the address to connect to
     * @param serviceName the service name to use
     * @param userName the username to use
     * @param resource and the resource.
     * @param loginStrategy the login strategy to use
     * @return return the state how to continue the connect process.
     * @throws XMPPException if we cannot connect for some reason
     */
    private ConnectState connectAndLogin(
            InetSocketAddress address, String serviceName,
            String userName, String resource,
            JabberLoginStrategy loginStrategy)
        throws XMPPException, InterruptedException, IOException, SmackException
    {
        sLog.debug("connect to " + serviceName + " at " + address);

        // First ensure that the socket timeout is appropriate:
        System.setProperty("smack.socketfactory.timeout", "5000");

        XMPPTCPConnectionConfiguration.Builder configBuilder =
                getXMPPConfigBuilder(address,
                                     serviceName,
                                     loginStrategy);

        XMPPTCPConnectionConfiguration config = configBuilder.build();

        if (mConnection != null)
        {
            sLog.error("Connection is not null and isConnected:"
                + mConnection.isConnected(),
                new Exception("Trace possible duplicate connections: " +
                    getAccountID().getLoggableAccountID()));
            disconnectAndCleanConnection();
        }

        mConnection = new XMPPTCPConnection(config);

        if (mDebugger == null)
        {
            mDebugger = new SmackPacketDebugger();
        }

        // sets the debugger
        mDebugger.setConnection(mConnection);
        mConnection.addStanzaListener(
            mDebugger.new SmackDebuggerListener(),
            null);
        mConnection.addStanzaSendingListener(
            mDebugger.new SmackDebuggerInterceptor(),
            null);

        // Update the isConnected flag as we are about to connect.
        mIsConnected = true;

        // Set up the multi-user chat operation set and chat room manager prior
        // to connecting to clear out data relating to chat rooms that we
        // joined on a previous connection.
        mOpsetMuc.prepareForConnection();
        mChatRoomManager.prepareForConnection();

        mConnection.connect();

        // Add listeners for capabilities change events
        mOpsetContactCapabilities.addCapabilitiesUpdateListener();

        // Add the smack invitation handler immediately after attempting to
        // connect to make sure we don't miss any chat room invites that were
        // sent while we were offline.
        mOpsetMuc.addSmackInvitationListener();
        sLog.info("Added smack invitation listener");

        // Try to set the presence authorization handler immediately after
        // attempting to connect to be sure we don't miss any authorization
        // request packets that the server may send as soon as we have
        // connected.
        if (presenceOpSet != null)
        {
            presenceOpSet.setAuthorizationHandler(JabberActivator.getAuthorizationHandlerService());
            sLog.info("Set jabber authorization handler");
        }
        else
        {
            sLog.warn("Jabber presence op set is null");
        }

        mCurrentLocalIp = getLocalIpToUse();

        setTrafficClass();
        logHandshakeParameters();

        if(mAbortConnecting)
        {
            mAbortConnecting = false;
            disconnectAndCleanConnection();

            return ConnectState.ABORT_CONNECTING;
        }

        registerServiceDiscoveryManager();

        if (mConnectionListener == null)
        {
            mConnectionListener = new JabberConnectionListener();
        }

        if (!mConnection.isConnected())
        {
            sLog.error("Connection not established, server not found! " +
                       mConnection.getHost());

            // connection is not connected, lets set state to our connection
            // as failed seems there is some lag/problem with network
            // and this way we will inform for it and later reconnect if needed
            // as IllegalStateException that is thrown within
            // addConnectionListener is not handled properly
            disconnectAndCleanConnection();

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND, null);

            return ConnectState.ABORT_CONNECTING;
        }
        else
        {
            mConnection.addConnectionListener(mConnectionListener);
        }

        // Add a listener that logs when we drop packets from the stream.
        OperationSetBasicInstantMessagingJabberImpl opSetBasInstMes =
            (OperationSetBasicInstantMessagingJabberImpl)
            this.getOperationSet(OperationSetBasicInstantMessaging.class);
        mConnection.addStanzaDroppedListener(opSetBasInstMes);

        if(mAbortConnecting)

        {
            mAbortConnecting = false;
            disconnectAndCleanConnection();

            return ConnectState.ABORT_CONNECTING;
        }

        fireRegistrationStateChanged(
                getRegistrationState()
                , RegistrationState.REGISTERING
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , null);

        if (!loginStrategy.login(mConnection, userName, resource))
        {
            disconnectAndCleanConnection();
            fireRegistrationStateChanged(
                getRegistrationState(),
                // not auth failed, or there would be no info-popup
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED,
                loginStrategy.getClass().getName() + " requests abort");

            return ConnectState.ABORT_CONNECTING;
        }

        // Now we have logged in, claim Carbons (XEP-0280) support for the new connection.
        CarbonManager carbonManager = CarbonManager.getInstanceFor(mConnection);
        boolean isCarbonSupported = carbonManager.isSupportedByServer();
        if (isCarbonSupported)
        {
            carbonManager.enableCarbons();
        }
        sLog.info("Are Carbon (XEP-0280) messages supported? " +
                  (isCarbonSupported ? "yes" : "no"));

        if (mConnection.isAuthenticated())
        {
            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.REGISTERED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED, null);

            // We've registered successfully so we know that the server address
            // provided is valid.
            mAccountID.putAccountProperty(
                               ProtocolProviderFactory.SERVER_ADDRESS_VALIDATED,
                               String.valueOf(true));

            // Store the account so the property is stored in config and will
            // persist over restart.
            JabberActivator.getProtocolProviderFactory().storeAccount(mAccountID);

            return ConnectState.STOP_TRYING;
        }
        else
        {
            disconnectAndCleanConnection();

            fireRegistrationStateChanged(
                getRegistrationState()
                , RegistrationState.UNREGISTERED
                , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
                , null);

            return ConnectState.CONTINUE_TRYING;
        }
    }

    /**
     * Provides the config needed to initialise the XMPPTCPConnection.
     * @param address the address to connect to
     * @param serviceName the service name to use
     * @param loginStrategy the desired login strategy
     * @return the XMPP config builder with the desired settings
     * @throws XmppStringprepException if the given {@code serviceName} is not
     * a bare JID
     * @throws JitsiXmppException if TLS is required but there is no
     * CertificateService available
     */
    @VisibleForTesting
    XMPPTCPConnectionConfiguration.Builder getXMPPConfigBuilder(
            InetSocketAddress address,
            String serviceName,
            JabberLoginStrategy loginStrategy)
            throws XmppStringprepException, JitsiXmppException
    {
        XMPPTCPConnectionConfiguration.Builder configBuilder =
            XMPPTCPConnectionConfiguration.builder();
        configBuilder.setHost(address.getAddress().getHostAddress());
        configBuilder.setPort(address.getPort());
        configBuilder.setXmppDomain(serviceName);
        configBuilder.setProxyInfo(mProxy);

        // Enable SASL-PLAIN as an auth mechanism. All registered mechanisms are
        // enabled by default unless we explicitly enable one over others.
        configBuilder.addEnabledSaslMechanism(SASLPlainMechanism.NAME);

        configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

        // If CommPortal provisioning is turned on and login was done via SSO,
        // we need to set authzid for our connection because we will be sending
        // access token instead of password and by authzid EAS can distinguish
        // between the two.
        boolean isCommPortalIM = "CommPortal".equals(ConfigurationUtils.getImProvSource());
        if (isCommPortalIM && PreLoginUtils.isLoggedInViaSSO())
        {
            String dn = configService.global().getString(PROPERTY_PROVISIONING_USERNAME);
            // "azuread" is a magic string which tells AMS that we are using AAD SSO.
            configBuilder.setAuthzid(JidCreate.entityBareFrom(dn + "@azuread"));
        }

        CertificateService cvs = JabberActivator.getCertificateService();
        CompletableFuture<Void> listener = new CompletableFuture<>();
        try
        {
            if (cvs != null)
            {
                X509TrustManager customTrustManager = getTrustManager(cvs, serviceName);
                configBuilder.setCustomX509TrustManager(customTrustManager);

                SSLContext sslContext = loginStrategy.createSslContext(cvs, customTrustManager);
                configBuilder.setSslContextFactory(() -> {return sslContext;});

                if (OSUtils.isMac())
                {
                    listener = prepareForReRegisterOnKeychainUpdate(cvs, listener);
                    listener.completeAsync(() -> null, reloadExecutor);
                }
            }
            else
            {
                throw new JitsiXmppException("Certificate verification service is  unavailable and TLS is required");
            }
        }
        catch (GeneralSecurityException ex)
        {
            sLog.error("Error creating custom trust manager", ex);
            throw new JitsiXmppException("Error creating custom trust manager", ex);
        }

        return configBuilder;
    }

    /**
     * Prepares a task that will re-register the XMPP connection on macOS when
     * its system root certificate keychain has been updated.
     * @param cvs the CertificateVerificationService
     * @param listener the CompletableFuture to add re-registration stages to
     * @return the CompletableFuture which will run the re-registration once
     * the keychain update has occurred.
     */
    private CompletableFuture<Void> prepareForReRegisterOnKeychainUpdate(
            final CertificateService cvs,
            final CompletableFuture<Void> listener)
    {
        return listener.thenComposeAsync((obj) -> cvs.getMacOSKeychainUpdateTrigger(),
                                         reloadExecutor)
                .thenRun(() -> getReregisterRunnable(SecurityAuthority.MAC_KEYCHAIN_UPDATED));
    }

    /**
     * Determine the local IP that will be used when sending XMPP messages to
     * the IM server.
     *
     * @return The local IP which will be used, or null if it could not be
     * resolved.
     */
    private InetAddress getLocalIpToUse()
    {
        NetworkAddressManagerService networkManager =
                              JabberActivator.getNetworkAddressManagerService();

        InetAddress nextHop = getNextHop();

        if (networkManager == null)
        {
            sLog.error("NetworkAddressManagerService was null when " +
                         "determining the IP address to use for XMPP");
            return null;
        }
        else if (nextHop == null)
        {
            sLog.warn("Unable to get IP address of next XMPP hop when " +
                        "deciding which local IP to use.");
            return null;
        }

        InetAddress localIp = networkManager.getLocalHost(getNextHop());

        return localIp;
    }

    /**
     * Gets the TrustManager that should be used for the specified service
     *
     * @param serviceName the service name
     * @param cvs The CertificateVerificationService to retrieve the
     *            trust manager
     * @return the trust manager
     */
    private X509TrustManager getTrustManager(CertificateService cvs,
        String serviceName)
        throws GeneralSecurityException
    {
        return new HostTrustManager(
            cvs.getTrustManager(
                Arrays.asList(serviceName,
                              "_xmpp-client." + serviceName)
            )
        );
    }

    /**
     * Registers our ServiceDiscoveryManager
     */
    private void registerServiceDiscoveryManager()
    {
        // we setup supported features no packets are actually sent
        //during feature registration so we'd better do it here so that
        //our first presence update would contain a caps with the right
        //features.
        String name = StringUtils.escapeForXml(
            System.getProperty(
                "sip-communicator.application.name", "SIP Communicator")
            + " "
            + System.getProperty("sip-communicator.version", "SVN")).toString();

        ServiceDiscoveryManager serviceDiscoveryManager =
                ServiceDiscoveryManager.getInstanceFor(mConnection);
        serviceDiscoveryManager
                .setIdentity(new DiscoverInfo.Identity("client", name, "pc"));

        serviceDiscoveryManager.removeFeature("http://jabber.org/protocol/commands");

        for (String feature : mSupportedFeatures)
        {
            if (!serviceDiscoveryManager.includesFeature(feature))
            {
                serviceDiscoveryManager.addFeature(feature);
            }
        }
    }

    /**
     * Used to disconnect current connection and clean it.
     */
    public void disconnectAndCleanConnection()
    {
        Socket socket = null;
        boolean isConnected = false;
        if (mConnection != null)
        {
            socket = mConnection.getSocket();
            isConnected = mConnection.isConnected();
        }

        sLog.info("Disconnecting and cleaning connection: Socket = " + socket + ". Connected? " + isConnected);

        if (mConnection != null && mIsConnected)
        {
            mConnection.removeConnectionListener(mConnectionListener);

            // disconnect anyway cause it will clear any listeners
            // that maybe added even if its not connected
            try
            {
                PresenceBuilder unavailablePresenceBuilder =
                    mConnection.getStanzaFactory()
                               .buildPresenceStanza()
                               .ofType(Presence.Type.unavailable);

                if (presenceOpSet != null &&
                    !StringUtils.isNullOrEmpty(presenceOpSet.getCurrentStatusMessage()))
                {
                    unavailablePresenceBuilder.setStatus(
                        presenceOpSet.getCurrentStatusMessage());
                }

                // Mark the connection as disconnected before calling disconnect
                // as disconnecting can take a while.
                mIsConnected = false;
                mConnection.disconnect(unavailablePresenceBuilder.build());
            }
            catch (Exception e)
            {
                sLog.error("Exception disconnecting from XMPP", e);
            }

            mConnectionListener = null;
            mConnection = null;
        }
    }

    /**
     * Ends the registration of this protocol provider with the service.
     */
    public void unregister()
    {
        unregister(true);
    }

    /**
     * Unregister and fire the event if requested
     * @param fireEvent boolean
     */
    public void unregister(final boolean fireEvent)
    {
        sLog.info("Unregistering jabber " + this);

        // Before we unregister, make sure we process any fragments of SMSs for
        // which we have not received all of the fragments so that we don't
        // discard them and the user will see them.  We need to do this before
        // unregistering, otherwise the ProtocolProvider will no longer exist
        // so we won't be able to do this.
        OperationSetBasicInstantMessagingJabberImpl opSetBasInstMes =
            (OperationSetBasicInstantMessagingJabberImpl)
            this.getOperationSet(OperationSetBasicInstantMessaging.class);

        if (opSetBasInstMes != null)
        {
            opSetBasInstMes.processPendingMessageFragments();
        }

        // Before we unregister, make sure we mark any active file transfers as failed
        ActiveFileTransferStore.failAllActiveFileTransfers();

        if (mArchiveManager != null)
        {
            mArchiveManager.stop();
            mArchiveManager = null;
        }

        if (SwingUtilities.isEventDispatchThread())
        {
            // Make sure we aren't going to block the EDT while we wait for the
            // initializationLock
            Thread cleanUpThread = new Thread("Jabber unregister")
            {
                public void run()
                {
                    unregister(fireEvent);
                }
            };

            cleanUpThread.setDaemon(true);
            cleanUpThread.start();
            return;
        }

        synchronized(mInitializationLock)
        {
            disconnectAndCleanConnection();

            RegistrationState currRegState = getRegistrationState();

            if(fireEvent)
            {
                fireRegistrationStateChanged(
                    currRegState,
                    RegistrationState.UNREGISTERED,
                    RegistrationStateChangeEvent.REASON_USER_REQUEST, null);
            }
        }
    }

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM, or others for
     * example).
     *
     * @return a String containing the short name of the protocol this
     *   service is taking care of.
     */
    public String getProtocolName()
    {
        return ProtocolNames.JABBER;
    }

    /**
     * Initialized the service implementation, and puts it in a state where it
     * could interoperate with other services. It is strongly recommended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     *
     * @param screenname the account id/uin/screenname of the account that
     * we're about to create
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected void initialize(String screenname,
                              AccountID accountID)
    {
        synchronized(mInitializationLock)
        {
            this.mAccountID = accountID;
            sLog.info("Initializing jabber " + this + " with accountID " + accountID.getLoggableAccountID());

            // in case of modified account, we clear list of supported features
            // and every state change listeners, otherwise we can have two
            // OperationSet for same feature and it can causes problem (i.e.
            // two OperationSetBasicTelephony can launch two ICE negociations
            // (with different ufrag/passwd) and peer will failed call. And
            // by the way user will see two dialog for answering/refusing the
            // call
            mSupportedFeatures.clear();
            this.clearRegistrationStateChangeListener();
            this.clearSupportedOperationSet();

            String protocolIconPath
                = accountID.getAccountPropertyString(
                        ProtocolProviderFactory.PROTOCOL_ICON_PATH);

            if (protocolIconPath == null)
                protocolIconPath = "resources/images/protocol/jabber";

            mJabberIcon = new ProtocolIconJabberImpl(protocolIconPath);

            mJabberStatusEnum
                = JabberStatusEnum.getJabberStatusEnum(protocolIconPath);

            String keepAliveStrValue
                = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.KEEP_ALIVE_METHOD);
            String resourcePriority
                = accountID.getAccountPropertyString(
                        ProtocolProviderFactory.RESOURCE_PRIORITY);

            InfoRetreiver infoRetreiver = new InfoRetreiver(this);

            mChatRoomManager = new ChatRoomManager(this);

            // Initialize the presence OperationSet
            presenceOpSet = new OperationSetPersistentPresenceJabberImpl(this, infoRetreiver);

            if(resourcePriority != null)
            {
                presenceOpSet.setResourcePriority(Integer.parseInt(resourcePriority));
            }

            addSupportedOperationSet(OperationSetPersistentPresence.class, presenceOpSet);
            addSupportedOperationSet(OperationSetPresence.class, presenceOpSet);

            //initialize the IM operation set
            OperationSetBasicInstantMessagingJabberImpl basicInstantMessaging =
                new OperationSetBasicInstantMessagingJabberImpl(this);

            if (keepAliveStrValue == null
                || keepAliveStrValue.equalsIgnoreCase("XEP-0199"))
            {
                if(mKeepAliveManager == null)
                    mKeepAliveManager = new KeepAliveManager(this);
            }

            addSupportedOperationSet(
                OperationSetBasicInstantMessaging.class,
                basicInstantMessaging);

            //initialize the Special IM operation set
            OperationSetSpecialMessagingJabberImpl specialInstantMessaging =
                new OperationSetSpecialMessagingJabberImpl();

            addSupportedOperationSet(
                OperationSetSpecialMessaging.class,
                specialInstantMessaging);

            // The http://jabber.org/protocol/xhtml-im feature is included
            // already in smack.

            addSupportedOperationSet(OperationSetExtendedAuthorizations.class,
                new OperationSetExtendedAuthorizationsJabberImpl(this, presenceOpSet));

            //initialize the typing notifications operation set
            addSupportedOperationSet(
                OperationSetTypingNotifications.class,
                new OperationSetTypingNotificationsJabberImpl(this));

            // The http://jabber.org/protocol/chatstates feature implemented in
            // OperationSetTypingNotifications is included already in smack.

            //initialize the multi user chat operation set
            mOpsetMuc = new OperationSetMultiUserChatJabberImpl(this);
            addSupportedOperationSet(
                OperationSetMultiUserChat.class,
                mOpsetMuc);

            OperationSetServerStoredAccountInfo accountInfo =
                new OperationSetServerStoredAccountInfoJabberImpl(this,
                        infoRetreiver,
                        screenname);

            addSupportedOperationSet(
                OperationSetServerStoredAccountInfo.class,
                accountInfo);

            // Initialize avatar operation set
            addSupportedOperationSet(
                OperationSetAvatar.class,
                new OperationSetAvatarJabberImpl(this, accountInfo));

            // initialize the file transfer operation set
            addSupportedOperationSet(
                OperationSetFileTransfer.class,
                new OperationSetFileTransferJabberImpl(this));

            addSupportedOperationSet(
                OperationSetInstantMessageTransform.class,
                new OperationSetInstantMessageTransformImpl());

            // Include features we're supporting in addition to the four
            // included by smack itself:
            // http://jabber.org/protocol/si/profile/file-transfer
            // http://jabber.org/protocol/si
            // http://jabber.org/protocol/bytestreams
            // http://jabber.org/protocol/ibb
            mSupportedFeatures.add("urn:xmpp:thumbs:0");
            mSupportedFeatures.add("urn:xmpp:bob");

            // initialize the thumbnailed file factory operation set
            addSupportedOperationSet(
                OperationSetThumbnailedFileFactory.class,
                new OperationSetThumbnailedFileFactoryImpl());

            // TODO: this is the "main" feature to advertise when a client
            // support muc. We have to add some features for
            // specific functionality we support in muc.
            // see http://www.xmpp.org/extensions/xep-0045.html

            // The http://jabber.org/protocol/muc feature is already included in
            // smack.
            mSupportedFeatures.add("http://jabber.org/protocol/muc#rooms");
            mSupportedFeatures.add("http://jabber.org/protocol/muc#traffic");

            // OperationSetContactCapabilities
            mOpsetContactCapabilities
                = new OperationSetContactCapabilitiesJabberImpl(this);
            addSupportedOperationSet(
                OperationSetContactCapabilities.class,
                mOpsetContactCapabilities);

            addSupportedOperationSet(
                OperationSetGenericNotifications.class,
                new OperationSetGenericNotificationsJabberImpl(this));

            mSupportedFeatures.add("jabber:iq:version");
            if(mVersionManager == null)
                mVersionManager = new VersionManager(this);

            if (ConfigurationUtils.isImCorrectionEnabled())
            {
                mSupportedFeatures.add(MessageCorrectionExtension.NAMESPACE);
                addSupportedOperationSet(OperationSetMessageCorrection.class,
                    basicInstantMessaging);
            }

            OperationSetChangePassword opsetChangePassword
                    = new OperationSetChangePasswordJabberImpl(this);
            addSupportedOperationSet(OperationSetChangePassword.class,
                    opsetChangePassword);

            mIsInitialized = true;
        }
    }

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for
     * shutdown/garbage collection.
     */
    public void shutdown()
    {
        sLog.info("Shutdown " + this);

        // Run the shutdown on a separate thread as it may block waiting for the
        // initialization to complete
        Thread thread = new Thread("Jabber shutdown thread")
        {
            @Override
            public void run()
            {
                doShutdown();
            }
        };
        threadingService.submit("Jabber shutdown thread", thread);
    }

    /**
     * Actually do the shutdown.
     */
    private void doShutdown()
    {
        synchronized(mInitializationLock)
        {
            sLog.info("Killing the Jabber Protocol Provider.");

            if (mArchiveManager != null)
            {
                mArchiveManager.stop();
                mArchiveManager = null;
            }

            // Remove the contact presence listener as we don't need to update
            // the presence of contacts while we are shutting down.
            if (mOpsetContactCapabilities != null)
            {
                mOpsetContactCapabilities.removeCapabilitiesUpdateListener();
                mOpsetContactCapabilities = null;
            }

            // This op set owns a BGIMAutoPopulationEngine object, which owns a timer, so needs to be
            // cleaned up
            if (presenceOpSet != null)
            {
                presenceOpSet.cleanUp();
                presenceOpSet = null;
            }

            // This op set owns a timer so needs to be cleaned up
            if (mOpsetMuc != null)
            {
                mOpsetMuc.cleanUp();
                mOpsetMuc = null;
            }

            // Timer in mChatRoomManager needs to be cleaned up
            if (mChatRoomManager != null)
            {
                mChatRoomManager.cleanUp();
                mChatRoomManager = null;
            }

            disconnectAndCleanConnection();

            mIsInitialized = false;
            reloadExecutor.shutdown();
        }
    }

    /**
     * Returns true if the provider service implementation is initialized and
     * ready for use by other services, and false otherwise.
     *
     * @return true if the provider is initialized and ready for use and false
     * otherwise
     */
    public boolean isInitialized()
    {
        return mIsInitialized;
    }

    /**
     * Returns the AccountID that uniquely identifies the account represented
     * by this instance of the ProtocolProviderService.
     * @return the id of the account represented by this provider.
     */
    public AccountID getAccountID()
    {
        return mAccountID;
    }

    /**
     * Returns the <tt>XMPPConnection</tt>opened by this provider
     * @return a reference to the <tt>XMPPConnection</tt> last opened by this
     * provider.
     */
    public XMPPConnection getConnection()
    {
        return mConnection;
    }

    /**
     * Returns the chat room manager, responsible for joining and leaving chat
     * rooms.
     * @return the chat room manager.
     */
    public ChatRoomManager getChatRoomManager()
    {
        return mChatRoomManager;
    }

    /**
     * Determines whether a specific <tt>Exception</tt> signals that
     * attempted authentication has failed.
     *
     * @param ex the <tt>Exception</tt> which is to be determined whether it
     * signals that attempted authentication has failed
     * @return <tt>true</tt> if the specified <tt>ex</tt> signals that attempted
     * authentication has failed; otherwise, <tt>false</tt>
     */
    private boolean isAuthenticationFailed(Exception ex)
    {
        return (ex instanceof SASLErrorException ||
            ex instanceof SmackSaslException);
    }

    /**
     * Determines whether a specific <tt>Exception</tt> signals that a
     * potentially temporary SASL auth failure occurred.
     *
     * @param ex the <tt>Exception</tt> which is to be determined whether it
     * signals that this may be a temporary SASL failure
     * @return <tt>true</tt> if the specified <tt>ex</tt> signals that further
     * attempts at authentication may succeed; otherwise, <tt>false</tt>
     */
    private boolean isMaybeTempSaslAuthFailure(Exception ex)
    {
        boolean isMaybeTempSaslAuthFailure = false;

        if (ex instanceof SASLErrorException)
        {
            SASLError saslError =
                ((SASLErrorException)ex).getSASLFailure().getSASLError();

            // We retry on "not-authorized" as this is what AMS can return
            // us when it's not ready to handle auth following a reboot.
            if (saslError == SASLError.not_authorized ||
                saslError == SASLError.temporary_auth_failure)
            {
                isMaybeTempSaslAuthFailure = true;
            }
        }

        return isMaybeTempSaslAuthFailure;
    }

    /**
     * Tries to determine the appropriate message and status to fire,
     * according the exception.
     *
     * @param ex the {@link Exception} that caused the state change.
     */
    private void fireRegistrationStateChanged(Exception ex)
    {
        int reason = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;
        RegistrationState regState = RegistrationState.UNREGISTERED;

        String reasonStrLowerCase = ex.getMessage().toLowerCase(Locale.ENGLISH);
        sLog.info("Exception during registration: " + reasonStrLowerCase, ex);

        if (ex instanceof UnknownHostException || ex instanceof SocketException)
        {
            reason = RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND;
            regState = RegistrationState.CONNECTION_FAILED;
        }
        if (ex instanceof EndpointConnectionException)
        {
            sLog.debug("Could not connect to any hosts: " + reasonStrLowerCase);
            reason = RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;
            regState = RegistrationState.CONNECTION_FAILED;
        }
        else
        {
            if (isAuthenticationFailed(ex))
            {
                sLog.debug("Authentication Failed: " + reasonStrLowerCase, ex);

                // Should reset auth backoff unless we hit a temporary SASL error.
                boolean shouldResetAuthBackoff = true;

                if (getAccountID().isEnabled())
                {
                    // Only try to reregister if the account is still enabled.
                    JabberActivator.getProtocolProviderFactory().
                        storePassword(getAccountID(), null);

                    reason = RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED;
                    regState = RegistrationState.AUTHENTICATION_FAILED;

                    fireRegistrationStateChanged(
                        getRegistrationState(), regState, reason, null);

                    if ((!"CommPortal".equals(ConfigurationUtils.getImProvSource())))
                    {
                        // Try to reregister and to ask user for a new password,
                        // unless we're using CommPortal provisioned IM, as we
                        // won't prompt the user and we'll just keep using the
                        // same password in that case.
                        sLog.debug("Potentially bad password, trying to re-register");
                        reregister(SecurityAuthority.WRONG_PASSWORD);
                    }
                    else if (isMaybeTempSaslAuthFailure(ex) &&
                             ("CommPortal".equals(ConfigurationUtils.getImProvSource())))
                    {
                        // Try reregistering with backoff. Do not
                        // do this for manual IM since an authentication problem
                        // is almost certainly user error and we don't want to
                        // show a popup asking for credentials each retry.
                        mAuthenticationBackoff.onError();

                        // Only retry registering if we haven't hit the maximum backoff limit.
                        if (!mAuthenticationBackoff.hasHitMaxDoubles())
                        {
                            shouldResetAuthBackoff = false;

                            if (mAuthenticationBackoff.shouldWait())
                            {
                                sLog.debug("Waiting to reregister as there are server authentication problems. Backoff " +
                                    mAuthenticationBackoff.getBackOffTime());
                                threadingService.schedule("ReregisterJabber",
                                                           getReregisterRunnable(SecurityAuthority.CONNECTION_FAILED),
                                                           mAuthenticationBackoff.getBackOffTime());
                            }
                            else
                            {
                                // Don't wait before executing
                                threadingService.submit("ReregisterJabber",
                                                        getReregisterRunnable(SecurityAuthority.CONNECTION_FAILED));
                            }
                        }
                        else
                        {
                            sLog.info("Give up trying to register. There are server authentication problems.");
                            JabberActivator.getAnalyticsService().onEvent(
                                    AnalyticsEventType.XMPP_AUTHENTICATION_BACKOFF_HIT_MAX_FAILURES,
                                    AnalyticsParameter.NAME_FINAL_BACKOFF_MS, String.valueOf(mAuthenticationBackoff.getBackOffTime()));

                            JabberActivator.getInsightsService().logEvent(
                                    JABBER_IM_LOG_IN.name(),
                                    Map.of(JabberParameterInfo.IM_LOGIN_EXCEPTION.name(), ex)
                            );
                        }
                    }
                }
                else
                {
                    sLog.info("Authentication Failed, but the account has been disabled.");
                }

                if (shouldResetAuthBackoff)
                {
                    mAuthenticationBackoff.resetBackoff();
                }
                return;
            }
            else
            {
                // This string matching in this else block matches XMPPExceptions
                // that we've thrown in the client code, and are not derived from
                // underlying libraries.

                boolean serverNotFound = reasonStrLowerCase.contains("no server addresses found");

                if (ex instanceof NoResponseException ||
                    reasonStrLowerCase.contains("connection failed") ||
                    reasonStrLowerCase.contains("failed to connect") ||
                    (serverNotFound))
                {
                    sLog.debug("Connection Failed: " + reasonStrLowerCase, ex);
                    reason = serverNotFound ?
                        RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND :
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED;
                    regState = RegistrationState.CONNECTION_FAILED;

                    // Check whether we have ever had a working connection to
                    // this server and, if not, delete the account and prompt
                    // the user to check their username and retry login.
                    handleConnectionFailed();
                }
                else if (ex instanceof SecurityRequiredByClientException ||
                         reasonStrLowerCase.contains("tls is required"))
                {
                    sLog.debug("TLS required: " + reasonStrLowerCase, ex);
                    regState = RegistrationState.AUTHENTICATION_FAILED;
                    reason = RegistrationStateChangeEvent.REASON_TLS_REQUIRED;
                }
            }
        }

        // We wouldn't reach this line of code if it were another potentially
        // temporary SASL auth error so reset backoff.
        mAuthenticationBackoff.resetBackoff();

        if ((regState == RegistrationState.UNREGISTERED) ||
            (regState == RegistrationState.CONNECTION_FAILED))
        {
            // we fired that for some reason we are going offline
            // lets clean the connection state for any future connections
            disconnectAndCleanConnection();
        }

        fireRegistrationStateChanged(
            getRegistrationState(), regState, reason, reasonStrLowerCase);
    }

    /**
     * Called when we fail to connect to the server.  This method first checks
     * whether we have ever had a working connection to this server.  If so, it
     * just returns and does nothing. If not, it deletes the chat account,
     * displays a dialog to the user asking them to check that their username
     * is valid, then redisplays the chat login box.
     */
    private void handleConnectionFailed()
    {
        boolean serverAddressValidated =
            mAccountID.getAccountPropertyBoolean(
                ProtocolProviderFactory.SERVER_ADDRESS_VALIDATED, false);

        if (serverAddressValidated)
        {
            sLog.debug("Server address validated - not deleting account");
            return;
        }

        sLog.debug("Failed to connect to server - deleting account " + mAccountID.getLoggableAccountID());
        mAccountID.deleteAccount(false);

        // Display the dialogs on a new thread so they don't block start-up
        new Thread("Jabber.ShowDialog")
        {
            public void run()
            {
                sLog.debug("Showing invalid username dialog for " + mAccountID.getLoggableAccountID());
                ResourceManagementService res = JabberActivator.getResources();
                String title = res.getI18NString("service.gui.ERROR");
                String message = res.getI18NString(
                    "plugin.jabberaccregwizz.INVALID_USERNAME", new String[]{ mAccountID.getUserID() });
                new ErrorDialog(title, message).showDialog();

                sLog.debug("Showing chat account login box");
                WizardContainer wizardContainer =
                    JabberActivator.getUIService().getAccountRegWizardContainer();
                JabberAccountRegistrationWizard jabberWizard =
                            new JabberAccountRegistrationWizard(wizardContainer);
                CreateAccountWindow createAccountWindow =
                         JabberActivator.getUIService().getCreateAccountWindow();
                createAccountWindow.setSelectedWizard(jabberWizard, true);
                createAccountWindow.setVisible(true);
            }
        }.start();
    }

    /**
     * Enable to listen for jabber connection events
     */
    private class JabberConnectionListener
        implements ConnectionListener
    {
        /**
         * Implements <tt>connectionClosed</tt> from <tt>ConnectionListener</tt>
         */
        public void connectionClosed()
        {
            // if we are in the middle of connecting process
            // do not fire events, will do it later when the method
            // connectAndLogin finishes its work
            synchronized(mConnectAndLoginLock)
            {
                if(mInConnectAndLogin)
                {
                    mEventDuringLogin = new RegistrationStateChangeEvent(
                        ProtocolProviderServiceJabberImpl.this,
                        getRegistrationState(),
                        RegistrationState.CONNECTION_FAILED,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                        null);
                     return;
                }
            }
            // fire that a connection failed, the reconnection mechanism
            // will look after us and will clean us, other wise we can do
            // a dead lock (connection closed is called
            // within xmppConneciton and calling disconnect again can lock it)
            fireRegistrationStateChanged(
                getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                null);
        }

        /**
         * Implements <tt>connectionClosedOnError</tt> from
         * <tt>ConnectionListener</tt>.
         *
         * @param exception contains information on the error.
         */
        public void connectionClosedOnError(Exception exception)
        {
            sLog.error("connectionClosedOnError " +
                         exception.getLocalizedMessage());

            if(exception instanceof StreamErrorException)
            {
                StreamError err = ((StreamErrorException)exception).getStreamError();

                if(err.getCondition().equals(StreamError.Condition.conflict))
                {
                    // if we are in the middle of connecting process
                    // do not fire events, will do it later when the method
                    // connectAndLogin finishes its work
                    synchronized(mConnectAndLoginLock)
                    {
                        if(mInConnectAndLogin)
                        {
                            mEventDuringLogin = new RegistrationStateChangeEvent(
                                ProtocolProviderServiceJabberImpl.this,
                                getRegistrationState(),
                                RegistrationState.UNREGISTERED,
                                RegistrationStateChangeEvent.REASON_MULTIPLE_CONNECTIONS,
                                "Connecting multiple times with the same resource");
                             return;
                        }
                    }

                    disconnectAndCleanConnection();

                    fireRegistrationStateChanged(getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_MULTIPLE_CONNECTIONS,
                        "Connecting multiple times with the same resource");

                    return;
                }
            } // Ignore certificate exceptions as we handle them elsewhere
            else if(exception instanceof SSLHandshakeException &&
                exception.getCause() instanceof CertificateException)
            {
                return;
            }

            // if we are in the middle of connecting process
            // do not fire events, will do it later when the method
            // connectAndLogin finishes its work
            synchronized(mConnectAndLoginLock)
            {
                if(mInConnectAndLogin)
                {
                    mEventDuringLogin = new RegistrationStateChangeEvent(
                        ProtocolProviderServiceJabberImpl.this,
                        getRegistrationState(),
                        RegistrationState.CONNECTION_FAILED,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                        exception.getMessage());
                     return;
                }
            }

            disconnectAndCleanConnection();

            fireRegistrationStateChanged(getRegistrationState(),
                RegistrationState.CONNECTION_FAILED,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                exception.getMessage());
        }
    }

    /**
     * Returns the jabber protocol icon.
     * @return the jabber protocol icon
     */
    public ProtocolIcon getProtocolIcon()
    {
        return mJabberIcon;
    }

    /**
     * Returns the current instance of <tt>JabberStatusEnum</tt>.
     *
     * @return the current instance of <tt>JabberStatusEnum</tt>.
     */
    JabberStatusEnum getJabberStatusEnum()
    {
        return mJabberStatusEnum;
    }

    /**
     * Determines if the given list of <tt>features</tt> is supported by the
     * specified jabber id.
     *
     * @param jid the jabber id for which to check
     * @param features the list of features to check for
     *
     * @return <tt>true</tt> if the list of features is supported; otherwise,
     * <tt>false</tt>
     */
    public boolean isFeatureListSupported(Jid jid, String... features)
    {
        boolean isFeatureListSupported = true;

        if (jid != null)
        {
            try
            {
                DiscoverInfo featureInfo =
                        ServiceDiscoveryManager.getInstanceFor(mConnection)
                                .discoverInfo(jid);

                for (String feature : features)
                {
                    if (!featureInfo.containsFeature(feature))
                    {
                        // If one is not supported we return false and don't check
                        // the others.
                        isFeatureListSupported = false;
                        break;
                    }
                }
            }
            catch (Exception e)
            {
                sLog.error("Failed to get discoverInfo.");
            }
        }

        return isFeatureListSupported;
    }

    /**
     * Returns the full jabber id (jid) corresponding to the given contact. If
     * the provider is not connected returns null.
     *
     * @param contact the contact, for which we're looking for a jid
     * @return the jid of the specified contact or null if the provider is not
     * yet connected; The Jid can still be bare if there's no presence available.
     * @throws XmppStringprepException
     */
    public Jid getFullJid(Contact contact) throws XmppStringprepException
    {
        return getFullJid(JidCreate.bareFrom(contact.getAddress()));
    }

    /**
     * Returns the full jabber id (jid) corresponding to the given bare jid. If
     * the provider is not connected returns null.
     *
     * @param bareJid the bare contact address (i.e. no resource) whose full
     * jid we are looking for.
     * @return the jid of the specified contact or null if the provider is not
     * yet connected; The Jid can still be bare if there's no presence available.
     */
    public Jid getFullJid(BareJid bareJid)
    {
        XMPPConnection connection = getConnection();

        // when we are not connected there is no full jid
        if (connection == null || !connection.isConnected())
        {
            return null;
        }

        Roster roster = Roster.getInstanceFor(connection);
        if (roster != null)
            return roster.getPresence(bareJid).getFrom();

        return null;
    }

    /**
     * The trust manager which asks the client whether to trust particular
     * certificate which is not globally trusted.
     */
    private class HostTrustManager
        implements X509TrustManager
    {
        /**
         * The default trust manager.
         */
        private final X509TrustManager tm;

        /**
         * Creates the custom trust manager.
         * @param tm the default trust manager.
         */
        HostTrustManager(X509TrustManager tm)
        {
            this.tm = tm;
        }

        /**
         * Not used.
         *
         * @return nothing.
         */
        public X509Certificate[] getAcceptedIssuers()
        {
            return new X509Certificate[0];
        }

        /**
         * Not used.
         * @param chain the cert chain.
         * @param authType authentication type like: RSA.
         * @throws UnsupportedOperationException always
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws UnsupportedOperationException
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Check whether a certificate is trusted, if not as user whether he
         * trust it.
         * @param chain the certificate chain.
         * @param authType authentication type like: RSA.
         * @throws CertificateException not trusted.
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException
        {
            mAbortConnecting = true;
            try
            {
                tm.checkServerTrusted(chain, authType);
            }
            catch(CertificateException e)
            {
                fireRegistrationStateChanged(getRegistrationState(),
                            RegistrationState.UNREGISTERED,
                            RegistrationStateChangeEvent.REASON_USER_REQUEST,
                            "Not trusted certificate");
                throw e;
            }

            if(mAbortConnecting)
            {
                // connect hasn't finished we will continue normally
                mAbortConnecting = false;
            }
            else
            {
                // in this situation connect method has finished
                // and it was disconnected so we want to connect.

                // register.connect in new thread so we can release the
                // current connecting thread, otherwise this blocks
                // jabber
                threadingService.submit("ReregisterJabber",
                                        getReregisterRunnable(SecurityAuthority.CONNECTION_FAILED));
            }
        }
    }

    /**
     * Returns our own Jabber ID as a Jid.
     * If we have a working connection to the server,
     * this returns our full JID (node@domain/resource).
     * If we don't, we construct and return our bare JID (node@domain).
     *
     * @return our own Jabber ID.
     */
    public Jid getOurJid()
    {
        Jid jid = null;
        String accountIDUserID = getAccountID().getUserID();

        if (mConnection != null)
        {
            jid = mConnection.getUser();
        }
        sLog.info("Fetched our JID from the server: " + sanitiseChatAddress(jid.toString()));

        if (jid == null)
        {
            // seems like the connection is not yet initialized so lets try to
            // construct our jid ourselves.
            try
            {
                jid = JidCreate.bareFrom(accountIDUserID);
                sLog.info("Connection to server not initialised so constructed our bare JID: " +
                          sanitiseChatAddress(jid.toString()));
            }
            catch (XmppStringprepException e)
            {
                sLog.error("Invalid JID", e);
                return null;
            }
        }

        return jid;
    }

    /**
     * Returns our own Jabber ID as a String (with DN removed, for privacy reasons).
     * If we have a working connection to the server,
     * this returns our full JID (node@domain/resource).
     * If we don't, we construct and return our bare JID (node@domain).
     *
     * @return our own Jabber ID.
     */
    public String getOurJidAsString()
    {
        Jid jid = getOurJid();

        return (jid != null) ?
                sanitiseChatAddress(jid.toString()) :
                null;
    }

    /**
     * Returns our own Jabber ID as a BareJid (node@domain)
     *
     * @return our own Jabber ID.
     */
    public BareJid getOurBareJid()
    {
        Jid jid = getOurJid();

        return (jid != null) ? jid.asBareJid() : null;
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting our XMPP server. This is an utility method
     * that is used whenever we have to choose one of our local addresses (e.g.
     * when trying to pick a best candidate for raw udp). It is based on the
     * assumption that, in absence of any more specific details, chances are
     * that we will be accessing remote destinations via the same interface
     * that we are using to access our jabber server.
     *
     * @return the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting our server.
     *
     * @throws IllegalArgumentException if we don't have a valid server.
     */
    public InetAddress getNextHop()
        throws IllegalArgumentException
    {
        InetAddress nextHop = null;
        String nextHopStr = null;

        if (mProxy != null)
        {
            nextHopStr = mProxy.getProxyAddress();
        }
        else
        {
            XMPPConnection connection = getConnection();
            if ((connection == null) || (connection.getHost() == null))
            {
                // No connectivity yet, so return a null address.
                sLog.debug("Yet to get XMPP connection with resolved host.");
                return null;
            }

            nextHopStr = connection.getHost();
        }

        try
        {
            nextHop = NetworkUtils.getInetAddress(nextHopStr);
        }
        catch (UnknownHostException ex)
        {
            throw new IllegalArgumentException("seems we don't have a valid next hop for string " + nextHopStr, ex);
        }

        sLog.debug("Returning address " + nextHop + " as next hop.");

        return nextHop;
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
     * Sets the traffic class for the XMPP signalling socket.
     */
    private void setTrafficClass()
    {
        if (mConnection == null)
        {
            // If the connection has been lost (e.g. another thread has
            // unregistered us) then we don't want to continue trying to
            // connect - the Reconnect Plugin will ensure we do later on
            mAbortConnecting = true;
            return;
        }
        Socket s = mConnection.getSocket();

        if(s != null)
        {
            ConfigurationService configService =
                JabberActivator.getConfigurationService();
            String dscp = configService.global().getString(XMPP_DSCP_PROPERTY);

            if(dscp != null)
            {
                try
                {
                    int dscpInt = Integer.parseInt(dscp) << 2;

                    if(dscpInt > 0)
                        s.setTrafficClass(dscpInt);
                }
                catch (Exception e)
                {
                    sLog.info("Failed to set trafficClass", e);
                }
            }
        }
    }

    /**
     * Logs TLS handshake parameters for the XMPP socket.
     */
    private void logHandshakeParameters()
    {
        if (mConnection.isSecureConnection())
        {
            final SSLSocket sslSocket = (SSLSocket) mConnection.getSocket();

            CertificateService certificateService = JabberActivator.getCertificateService();
            certificateService.notifySecureConnectionEstablished("XMPP", sslSocket.getSession());
        }
    }

    /**
     * @return true if the XMPP connection is connected and not disconnecting or
     *         just about to be disconnected.
     */
    boolean isConnected()
    {
        return mIsConnected;
    }

    /**
     * Called when the operation set tells us that all unresolved contacts have
     * been loaded from the local contact store (contactlist.xml) by the
     * MclStorageManager.
     */
    protected void onAllUnresovledContactsLoaded()
    {
        synchronized (this)
        {
            sLog.debug("All resolved contacts loaded " + this);
            mLoadedAllUnresolvedContacts = true;

            // Now that we've finished loading contacts from the local store,
            // we need to log in to the protocol provider so we can load any
            // server stored contacts. We need the UI service to get the login
            // manager, but the service might not be registered yet, so
            // register a ServiceListener.
            sBundleContext.addServiceListener(this);

            // Check whether the UI Service already registered before we added
            // the listener.
            sUiService = JabberActivator.getUIService();

            if (sUiService != null)
            {
                sLog.debug("UIService already registered - logging in");
                sBundleContext.removeServiceListener(this);
                sUiService.getLoginManager().login(this);
            }
        }
    }

    /**
     * Called after connectAndLogin succeeds with no exceptions. We reset
     * the authentication backoff and send an analytic if backoff was started,
     * so that backoff parameters can be optimised.
     */
    private void onAuthenticationSucceeded()
    {
        if (mAuthenticationBackoff.getBackOffTime() > 0)
        {
            JabberActivator.getAnalyticsService().onEvent(
                    AnalyticsEventType.XMPP_AUTHENTICATION_BACKOFF_SUCCESS,
                    AnalyticsParameter.NAME_FINAL_BACKOFF_MS, String.valueOf(mAuthenticationBackoff.getBackOffTime()));
        }
        mAuthenticationBackoff.onSuccess();

        JabberActivator.getInsightsService().logEvent(JABBER_IM_LOG_IN.name(), Collections.emptyMap());
    }

    /**
     * Listen to ServiceEvents so we can get the login manager and log the
     * jabber accont in once the UIService has registered.
     *
     * @param event the ServiceEvent
     */
    @Override
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference<?> serviceRef = event.getServiceReference();

        Object service = sBundleContext.getService(serviceRef);

        // If the event is caused by a bundle being stopped, we don't want to
        // know.
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            return;

        // We don't care if the service isn't the UI service.
        if (service instanceof UIService)
        {
            sLog.debug("Changed service is UI Service");
            sBundleContext.removeServiceListener(this);

            synchronized (this)
            {
                if (sUiService == null)
                {
                    sLog.debug("UIService registered - logging in");
                    sUiService = (UIService) service;
                    sUiService.getLoginManager().login(this);
                }
            }
        }
    }

    public boolean hasIpChanged()
    {
        boolean hasIpChanged;

        InetAddress localIpToUse = getLocalIpToUse();
        hasIpChanged = !Objects.equals(mCurrentLocalIp, localIpToUse);
        sLog.debug("hasIpChanged? " + hasIpChanged);

        if (hasIpChanged)
        {
            // The local IP has changed, so update our locally stored copy.
            mCurrentLocalIp = localIpToUse;
        }

        return hasIpChanged;
    }

    /**
     * Custom exception that allows us to throw specific instances of
     * XMPPException
     */
    public class JitsiXmppException extends XMPPException
    {
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new JitsiXMPPException with a description of the exception.
         *
         * @param message description of the exception.
         */
        public JitsiXmppException(String message)
        {
            super(message);
        }

        /**
         * Creates a new JitsiXMPPException with a description of the exception
         * and the Throwable that was the root cause of the exception.
         *
         * @param message a description of the exception.
         * @param wrappedThrowable the root cause of the exception.
         */
        public JitsiXmppException(String message, Throwable wrappedThrowable)
        {
            super(message, wrappedThrowable);
        }
    }
}
