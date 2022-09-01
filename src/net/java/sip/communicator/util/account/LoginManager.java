/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.account;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>LoginManager</tt> manages the login operation. Here we obtain the
 * <tt>ProtocolProviderFactory</tt>, we make the account installation and we
 * handle all events related to the registration state.
 * <p>
 * The <tt>LoginManager</tt> is the one that opens one or more
 * <tt>LoginWindow</tt>s for each <tt>ProtocolProviderFactory</tt>. The
 * <tt>LoginWindow</tt> is where user could enter an identifier and password.
 * <p>
 * Note that the behavior of this class will be changed when the Configuration
 * Service is ready.
 *
 * @author Yana Stamcheva
 */
public class LoginManager
    implements  ServiceListener,
                RegistrationStateChangeListener,
                AccountManagerListener
{
    private static final Logger logger = Logger.getLogger(LoginManager.class);

    private boolean manuallyDisconnected = false;

    private final LoginRenderer loginRenderer;

    private static final Set<ProtocolProviderService> sRegisteringProviders =
            new HashSet<>();

    /**
     * Creates an instance of the <tt>LoginManager</tt>, by specifying the main
     * application window.
     *
     * @param loginRenderer the main application window
     */
    public LoginManager(LoginRenderer loginRenderer)
    {
        this.loginRenderer = loginRenderer;
    }

    /**
     * Registers the given protocol provider.
     *
     * @param protocolProvider the ProtocolProviderService to register.
     */
    public void login(ProtocolProviderService protocolProvider)
    {
        loginRenderer.startConnectingUI(protocolProvider);

        synchronized (sRegisteringProviders)
        {
            if (!sRegisteringProviders.contains(protocolProvider))
            {
                // Only actually register if we aren't already registering
                sRegisteringProviders.add(protocolProvider);
                new RegisterProvider(protocolProvider,
                    loginRenderer.getSecurityAuthorityImpl(protocolProvider)).start();
            }
        }
    }

    /**
     * Unregisters the given protocol provider.
     *
     * @param protocolProvider the ProtocolProviderService to unregister
     */
    public void logoff(ProtocolProviderService protocolProvider)
    {
        try
        {
            protocolProvider.unregister();
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to unregister " +
                          protocolProvider.getProtocolDisplayName() +
                          ", error code: " + ex.getErrorCode(), ex);
        }
    }

    /**
     * Shows login window for each registered account.
     */
    public void runLogin()
    {
        // if someone is late registering catch it
        UtilActivator.getAccountManager().addListener(this);

        for (ProtocolProviderFactory providerFactory : UtilActivator
            .getProtocolProviderFactories().values())
        {
            logger.debug("About to add accounts for protocol provider " +
                         "factory " + providerFactory);

            addAccountsForProtocolProviderFactory(providerFactory);
        }

        // Register for service changes.
        UtilActivator.bundleContext.addServiceListener(this);

        // And check if there are any existing ones
        ServiceReference<?>[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs
                = UtilActivator.bundleContext.getServiceReferences(
                        ProtocolProviderService.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error("Error while retrieving service refs", ex);
        }

        // If we found any, handle them
        if (protocolProviderRefs != null)
        {
            logger.debug("Found " + protocolProviderRefs.length +
                         " already installed providers.");

            for (ServiceReference<?> protocolProviderRef : protocolProviderRefs)
            {
                ProtocolProviderService provider = (ProtocolProviderService)
                    UtilActivator.bundleContext.getService(protocolProviderRef);
                this.handleProviderAdded(provider);
            }
        }
    }

    /**
     * Notifies that the loading of the stored accounts of a specific
     * <code>ProtocolProviderFactory</code> has finished, or that an account has
     * been removed.
     *
     * @param event the <code>AccountManagerEvent</code> describing the
     *            <code>AccountManager</code> firing the notification and the
     *            other details of the specific notification.
     */
    public void handleAccountManagerEvent(AccountManagerEvent event)
    {
        logger.debug("Started 'handleAccountManagerEvent' for event type "
                     + event.getType());

        if (event.getType() == AccountManagerEvent.STORED_ACCOUNTS_LOADED)
        {
            logger.info("About to add accounts for protocol provider " +
                         "factory " + event.getFactory());

            addAccountsForProtocolProviderFactory(event.getFactory());
        }
        else if (event.getType() == AccountManagerEvent.ACCOUNT_REMOVED)
        {
            logger.info("About to try removing UI for protocol " +
                         "provider of account " + event.getAccountID());

            loginRenderer.removeProtocolProviderUI(event.getAccountID());
        }
    }

    /**
     * Handles stored accounts for a protocol provider factory and add them
     * to the UI and register them if needed.
     * @param providerFactory the factory to handle.
     */
    private void addAccountsForProtocolProviderFactory(
        ProtocolProviderFactory providerFactory)
    {
        ServiceReference<?> serRef;
        ProtocolProviderService protocolProvider;

        for (AccountID accountID : providerFactory.getRegisteredAccounts())
        {
            logger.debug("Adding " + accountID + " account");

            serRef = providerFactory.getProviderForAccount(accountID);

            protocolProvider =
                (ProtocolProviderService) UtilActivator.bundleContext
                    .getService(serRef);

            handleRegisterEvents(protocolProvider);
        }
    }

    /**
     * Adds a registration state listener for the given protocol provider
     * then runs the handleProviderRegistering and handleProviderRegistered
     * methods when it receives those events.  Also checks whether the provider
     * was already registering or registered when the listener was added then
     * runs handleProviderRegistering and handleProviderRegistered accordingly.
     *
     * @param protocolProvider    The protocol provider
     */
    private void handleRegisterEvents(ProtocolProviderService protocolProvider)
    {
        logger.info("Adding reg state change listener for " + protocolProvider);
        protocolProvider.addRegistrationStateChangeListener(this);
        loginRenderer.addProtocolProviderUI(protocolProvider);

        RegistrationState currentRegState = protocolProvider.getRegistrationState();
        if(RegistrationState.REGISTERING.equals(currentRegState))
        {
            logger.debug("Provider already registering " + protocolProvider);
            handleProviderRegistering(protocolProvider);
        }
        else if(RegistrationState.REGISTERED.equals(currentRegState))
        {
            // The provider is already registered so we missed both the
            // 'registering' and the 'registered' events.  Run the methods that
            // are triggered by those events now.
            logger.debug("Provider already registered " + protocolProvider);
            handleProviderRegistering(protocolProvider);
            handleProviderRegistered(protocolProvider);
        }

        Object status =
            AccountStatusUtils.getProtocolProviderLastStatus(protocolProvider);

        if (status == null ||
            (status instanceof String &&
                !status.equals(GlobalStatusEnum.OFFLINE_STATUS)) ||
            (status instanceof PresenceStatus
                && (((PresenceStatus) status)
                .getStatus() >= PresenceStatus.ONLINE_THRESHOLD)))
        {
            // The user was logged in last time they were registered,
            // so log in again now.
            logger.debug("About to log in " + protocolProvider);
            login(protocolProvider);
        }
        else
        {
            // The user wasn't logged in the last time they were registered,
            // so don't log in now.
            logger.warn("Not logging in " + protocolProvider + " as status is " + status);
        }
    }

    /**
     * Run when the protocol provider's registration state changes to
     * REGISTERING (or if the provider's state was already REGISTERED when the
     * LoginManager created a registration listener) and executes the
     * LoginRenderer's protcolProviderConnecting method for the given protocol
     * provider.
     *
     * @param protocolProvider    The protocol provider that is registering.
     */
    private void handleProviderRegistering(
        ProtocolProviderService protocolProvider)
    {
        logger.info("Provider registering " + protocolProvider);
        loginRenderer.protocolProviderConnecting(protocolProvider,
                                                 System.currentTimeMillis());
    }

    /**
     * Run when the protocol provider's registration state changes to
     * REGISTERED and executes the LoginRenderer's protcolProviderConnected
     * method for the given protocol provider.
     *
     * @param protocolProvider    The protocol provider that is registered.
     */
    private void handleProviderRegistered(
        ProtocolProviderService protocolProvider)
    {
        logger.info("Provider registered " + protocolProvider);
        loginRenderer.protocolProviderConnected(protocolProvider,
                                                System.currentTimeMillis());
    }

    /**
     * The method is called by a ProtocolProvider implementation whenever a
     * change in the registration state of the corresponding provider had
     * occurred.
     *
     * @param evt ProviderStatusChangeEvent the event describing the status
     *            change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState newState = evt.getNewState();
        ProtocolProviderService protocolProvider = evt.getProvider();
        AccountID accountID = protocolProvider.getAccountID();

        logger.info("Protocol provider: " + protocolProvider
            + " changed its state from " + evt.getOldState().getStateName()
            + " to: " + evt.getNewState().getStateName());

        if (newState.equals(RegistrationState.REGISTERED)
            || newState.equals(RegistrationState.UNREGISTERED)
            || newState.equals(RegistrationState.EXPIRED)
            || newState.equals(RegistrationState.AUTHENTICATION_FAILED)
            || newState.equals(RegistrationState.CONNECTION_FAILED)
            || newState.equals(RegistrationState.CHALLENGED_FOR_AUTHENTICATION))
        {
            loginRenderer.stopConnectingUI(protocolProvider);
        }

        if (newState.equals(RegistrationState.REGISTERING))
        {
            handleProviderRegistering(protocolProvider);
        }
        else if (newState.equals(RegistrationState.REGISTERED))
        {
            handleProviderRegistered(protocolProvider);
        }
        else
        {
            String msgText;
            if (newState.equals(RegistrationState.AUTHENTICATION_FAILED))
            {
                switch (evt.getReasonCode())
                {
                case RegistrationStateChangeEvent
                    .REASON_RECONNECTION_RATE_LIMIT_EXCEEDED:

                    msgText = UtilActivator.getResources().getI18NString(
                        "service.gui.RECONNECTION_LIMIT_EXCEEDED", new String[]
                        { accountID.getUserID(), accountID.getService() });

                    UtilActivator.getAlertUIService().showAlertDialog(
                        UtilActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        msgText);
                    break;

                case RegistrationStateChangeEvent.REASON_NON_EXISTING_USER_ID:
                    msgText = UtilActivator.getResources().getI18NString(
                        "service.gui.NON_EXISTING_USER_ID",
                        new String[]
                        { protocolProvider.getProtocolDisplayName() });

                    UtilActivator.getAlertUIService().showAlertDialog(
                        UtilActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        msgText);
                    break;
                case RegistrationStateChangeEvent.REASON_TLS_REQUIRED:
                    msgText = UtilActivator.getResources().getI18NString(
                        "service.gui.NON_SECURE_CONNECTION",
                        new String[]
                        { accountID.getAccountAddress() });

                    UtilActivator.getAlertUIService().showAlertDialog(
                        UtilActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        msgText);
                    break;
                default:
                    break;
                }

                logger.trace(evt.getReason());
            }
//            CONNECTION_FAILED events are now dispatched in reconnect plugin
//            else if (newState.equals(RegistrationState.CONNECTION_FAILED))
//            {
//                loginRenderer.protocolProviderConnectionFailed(
//                    protocolProvider,
//                    this);
//
//                logger.trace(evt.getReason());
//            }
            else if (newState.equals(RegistrationState.EXPIRED))
            {
                msgText = UtilActivator.getResources().getI18NString(
                    "service.gui.CONNECTION_EXPIRED_MSG",
                    new String[]
                    { protocolProvider.getProtocolDisplayName() });

                UtilActivator.getAlertUIService().showAlertDialog(
                    UtilActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    msgText);

                logger.error(evt.getReason());
            }
            else if (newState.equals(RegistrationState.UNREGISTERED))
            {
                if (!manuallyDisconnected)
                {
                    if (evt.getReasonCode() == RegistrationStateChangeEvent
                                                .REASON_MULTIPLE_LOGINS)
                    {
                        msgText = UtilActivator.getResources().getI18NString(
                            "service.gui.MULTIPLE_LOGINS",
                            new String[]
                            { accountID.getUserID(), accountID.getService() });

                        UtilActivator.getAlertUIService().showAlertDialog(
                            UtilActivator.getResources()
                                .getI18NString("service.gui.ERROR"),
                            msgText);
                    }
                    else if (evt.getReasonCode() == RegistrationStateChangeEvent
                                                .REASON_CLIENT_LIMIT_REACHED_FOR_IP)
                    {
                        msgText = UtilActivator.getResources().getI18NString(
                            "service.gui.LIMIT_REACHED_FOR_IP", new String[]
                            { protocolProvider.getProtocolDisplayName() });

                        UtilActivator.getAlertUIService().showAlertDialog(
                            UtilActivator.getResources()
                                .getI18NString("service.gui.ERROR"),
                            msgText);
                    }
                    else if (evt.getReasonCode() == RegistrationStateChangeEvent
                                                .REASON_USER_REQUEST)
                    {
                        // do nothing
                    }
//                    else
//                    {
//                        msgText = UtilActivator.getResources().getI18NString(
//                            "service.gui.UNREGISTERED_MESSAGE", new String[]
//                            { accountID.getUserID(), accountID.getService() });
//
//                        UtilActivator.getAlertUIService().showAlertDialog(
//                            UtilActivator.getResources()
//                                .getI18NString("service.gui.ERROR"),
//                            msgText);
//                    }
                    logger.trace(evt.getReason());
                }
            }
        }
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference<?> serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object service
            = UtilActivator.bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            logger.debug("Service " + service + " registered");
            this.handleProviderAdded((ProtocolProviderService) service);
            break;
        case ServiceEvent.UNREGISTERING:
            logger.debug("Service " + service + " unregistered");
            this.handleProviderRemoved((ProtocolProviderService) service);
            break;
        }
    }

    /**
     * Adds all UI components (status selector box, etc) related to the given
     * protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderAdded(ProtocolProviderService protocolProvider)
    {
        logger.trace("The following protocol provider was just added: "
            + protocolProvider.getAccountID().getAccountAddress());

        handleRegisterEvents(protocolProvider);
    }

    /**
     * Removes all UI components related to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderRemoved(ProtocolProviderService protocolProvider)
    {
        loginRenderer.removeProtocolProviderUI(protocolProvider);
    }

    /**
     * Returns <tt>true</tt> to indicate the jitsi has been manually
     * disconnected, <tt>false</tt> - otherwise.
     *
     * @return <tt>true</tt> to indicate the jitsi has been manually
     * disconnected, <tt>false</tt> - otherwise
     */
    public boolean isManuallyDisconnected()
    {
        return manuallyDisconnected;
    }

    /**
     * Sets the manually disconnected property.
     *
     * @param manuallyDisconnected <tt>true</tt> to indicate the jitsi has been
     * manually disconnected, <tt>false</tt> - otherwise
     */
    public void setManuallyDisconnected(boolean manuallyDisconnected)
    {
        this.manuallyDisconnected = manuallyDisconnected;
    }

    /**
     * Registers a protocol provider in a separate thread.
     */
    private class RegisterProvider
        extends Thread
    {
        private final ProtocolProviderService protocolProvider;

        private final SecurityAuthority secAuth;

        RegisterProvider(   ProtocolProviderService protocolProvider,
                            SecurityAuthority secAuth)
        {
            super("RegisterProviderThread");
            logger.debug("Creating RegisterProvider for protocol provider "
                         + protocolProvider + " and security authority " +
                         secAuth);
            this.protocolProvider = protocolProvider;
            this.secAuth = secAuth;
        }

        /**
         * Registers the contained protocol provider and process all possible
         * errors that may occur during the registration process.
         */
        public void run()
        {
            try
            {
                logger.debug("About to register protocol provider "
                             + protocolProvider + " with security authority "
                             + secAuth);
                protocolProvider.register(secAuth);
            }
            catch (OperationFailedException ex)
            {
                handleOperationFailedException(ex);
            }
            catch (Throwable ex)
            {
                logger.error("Failed to register protocol provider. ", ex);

                AccountID accountID = protocolProvider.getAccountID();
                UtilActivator.getAlertUIService().showAlertDialog(
                    UtilActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    UtilActivator.getResources()
                        .getI18NString("service.gui.LOGIN_GENERAL_ERROR",
                    new String[]
                    { accountID.getUserID(),
                      accountID.getProtocolName(),
                      accountID.getService() }));
            }
            finally
            {
                synchronized (sRegisteringProviders)
                {
                    sRegisteringProviders.remove(protocolProvider);
                }
            }
        }

        private void handleOperationFailedException(OperationFailedException ex)
        {
            String errorMessage = "";

            switch (ex.getErrorCode())
            {
            case OperationFailedException.GENERAL_ERROR:
            {
                logger.error("Provider could not be registered"
                    + " due to the following general error: ", ex);

                AccountID accountID = protocolProvider.getAccountID();
                errorMessage =
                    UtilActivator.getResources().getI18NString(
                        "service.gui.LOGIN_GENERAL_ERROR",
                        new String[]
                           { accountID.getUserID(),
                             accountID.getProtocolName(),
                             accountID.getService() });

                UtilActivator.getAlertUIService().showAlertDialog(
                    UtilActivator.getResources()
                        .getI18NString("service.gui.ERROR"), errorMessage, ex);
            }
                break;
            case OperationFailedException.INTERNAL_ERROR:
            {
                logger.error("Provider could not be registered"
                    + " due to the following internal error: ", ex);

                AccountID accountID = protocolProvider.getAccountID();
                errorMessage =
                    UtilActivator.getResources().getI18NString(
                        "service.gui.LOGIN_INTERNAL_ERROR",
                        new String[]
                           { accountID.getUserID(), accountID.getService() });

                UtilActivator.getAlertUIService().showAlertDialog(
                    UtilActivator.getResources().getI18NString(
                        "service.gui.ERROR"), errorMessage, ex);
            }
                break;
            case OperationFailedException.NETWORK_FAILURE:
            {
                logger.info("Provider could not be registered"
                            + " due to a network failure: " + ex);

                loginRenderer.protocolProviderConnectionFailed(
                    protocolProvider,
                    LoginManager.this);
            }
                break;
            case OperationFailedException.INVALID_ACCOUNT_PROPERTIES:
            {
                logger.error("Provider could not be registered"
                    + " due to an invalid account property: ", ex);

                AccountID accountID = protocolProvider.getAccountID();
                errorMessage =
                    UtilActivator.getResources().getI18NString(
                        "service.gui.LOGIN_INVALID_PROPERTIES_ERROR",
                        new String[]
                        { accountID.getUserID(), accountID.getService() });

                UtilActivator.getAlertUIService().showAlertDialog(
                    UtilActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    errorMessage, ex);
            }
                break;
            default:
                logger.error("Provider could not be registered.", ex);
            }
        }
    }
}
