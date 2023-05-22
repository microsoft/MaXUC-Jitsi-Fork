/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.reconnectplugin;

import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

import static org.jitsi.util.Hasher.logHasher;
import static org.jitsi.util.SanitiseUtils.sanitiseValuesInList;

import org.osgi.framework.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.*;

/**
 * Activates the reconnect plug-in.
 *
 * @author Damian Minkov
 */
public class ReconnectPluginActivator
    implements BundleActivator,
               ServiceListener,
               NetworkConfigurationChangeListener,
               RegistrationStateChangeListener
{
    /**
     * Logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ReconnectPluginActivator.class);

    /**
     * The current BundleContext.
     */
    private static BundleContext bundleContext = null;

    /**
     * The ui service.
     */
    private static UIService uiService;

    /**
     * The resources service.
     */
    private static ResourceManagementService resourcesService;

    /**
     * A reference to the ConfigurationService implementation instance that
     * is currently registered with the bundle context.
     */
    private static ConfigurationService configurationService = null;

    /**
     * A reference to the AccountManager implementation instance that
     * is currently registered with the bundle context.
     */
    private static AccountManager mAccountManager = null;

    /**
     * Network address manager service will inform us for changes in
     * network configuration.
     */
    private NetworkAddressManagerService networkAddressManagerService = null;

    /**
     * Holds every account which can be reconnected and
     * a list of the available and up interfaces when the account was
     * registered. When a provider is removed from osgi it is removed
     * from this collection.
     * Providers REMOVED:
     *  - When provider is removed from osgi
     * Providers ADDED:
     *  - When a provider is REGISTERED
     */
    private final Map<AccountID, List<String>>
        mAutoReconnEnabledAccounts = new HashMap<>();

    /**
     * Holds the currently reconnecting accounts and their reconnect tasks.
     * When they get connected they are removed from this collection.
     * Providers REMOVED:
     *  - When provider removed from osgi (unless only temporarily removed
     * when we attempt to reload the account).
     *  - When interface is UP, we remove accounts and schedule reconnect
     *  for them
     *  - When interface is DOWN, we remove all accounts and schedule reconnect
     *  - When last interface is DOWN, we remove all accounts and
     *  unregister them
     *  - On connection failed with no interface connected
     *  - Provider is Registered
     *  - Provider is Unregistered and is missing in unregistered accounts list
     *  - After provider is unregistered just before reconnecting, and there
     *  are no connected interfaces
     * Providers ADDED:
     *  - Before unregister (in new thread) when scheduling a reconnect task
     *  - After provider is unregistered just before reconnecting
     */
    private final Map<AccountID, ReconnectTask>
        mCurrentlyReconnecting = new HashMap<>();

    /**
     * If network is down we save here the accounts which need
     * to be reconnected.
     * Providers REMOVED:
     * - When provider removed from osgi.
     * - Remove all providers when interface is up and we will reconnect them
     * Providers ADDED:
     * - Interface is down, and there are still active interfaces, add all
     * auto reconnect enabled and all currently reconnecting
     * - Provider in connection failed and there are no connected interfaces
     * - Provider is unregistered or connection failed and there are no
     * connected interfaces.
     */
    private Set<AccountID> mNeedsReconnection = new HashSet<>();

    /**
     * A list of accounts on which we have called unregister. This is a
     * way to differ our unregister calls from calls coming from user, wanting
     * to stop all reconnects.
     * Providers REMOVED:
     * - Provider is Connection failed.
     * - Provider is registered/unregistered
     * Providers ADDED:
     * - Provider is about to be unregistered
     */
    private Set<AccountID> mUnregisteringAccounts = new HashSet<>();

    /**
     * A list of currently connected interfaces. If empty network is down.
     */
    private Set<String> connectedInterfaces = new HashSet<>();

    /**
     * Timer for scheduling all reconnect operations.  Must be static to ensure
     * we don't leak timers if this class is replaced without passing through
     * the stop method.
     */
    private static Timer sTimer = null;

    /**
     * Start of the delay interval when starting a reconnect.
     */
    private static final int RECONNECT_DELAY_MIN = 2; // sec

    /**
     * The end of the interval for the initial reconnect.
     */
    private static final int RECONNECT_DELAY_MAX = 4; // sec

    /**
     * Max value for growing the reconnect delay, all subsequent reconnects
     * use this maximum delay.
     */
    private static final int MAX_RECONNECT_DELAY = 60; // sec

    /**
     * Network notifications event type.
     */
    public static final String NETWORK_NOTIFICATIONS = "NetworkNotifications";

    /**
     *
     */
    public static final String ATLEAST_ONE_CONNECTION_PROP =
        "net.java.sip.communicator.plugin.reconnectplugin." +
            "ATLEAST_ONE_SUCCESSFUL_CONNECTION";

    private static ThreadingService threadingService;

    /**
     * Starts this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which this bundle is
     * to be started
     */
    public void start(BundleContext bundleContext)
    {
        try
        {
            logger.entry();
            ReconnectPluginActivator.bundleContext = bundleContext;
        }
        finally
        {
            logger.exit();
        }

        bundleContext.addServiceListener(this);

        if(sTimer == null)
            sTimer = new Timer("Reconnect timer", true);

        this.networkAddressManagerService
            = ServiceUtils.getService(
                    bundleContext,
                    NetworkAddressManagerService.class);
        this.networkAddressManagerService
            .addNetworkConfigurationChangeListener(this);
        threadingService = ServiceUtils.getService(bundleContext, ThreadingService.class);

        ServiceReference<?>[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bundleContext.getServiceReferences(
                ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            logger.debug("Found "
                     + protocolProviderRefs.length
                     + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) bundleContext
                        .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which this bundle is
     * to be stopped
     */
    public void stop(BundleContext bundleContext)
    {
        if(sTimer != null)
        {
            sTimer.cancel();
            sTimer = null;
        }

        // We're shutting down so stop monitoring accounts, as they may be
        // deleted on start-up and we will automatically start monitoring any
        // existing accounts again on start-up.
        getConfigurationService().user().removeProperty(ATLEAST_ONE_CONNECTION_PROP);
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            ServiceReference<?> uiReference =
                bundleContext.getServiceReference(UIService.class.getName());

            uiService =
                (UIService) bundleContext
                    .getService(uiReference);
        }

        return uiService;
    }

    /**
     * Returns resource service.
     * @return the resource service.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference<?> serviceReference = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService = (ResourceManagementService) bundleContext
                .getService(serviceReference);
        }

        return resourcesService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            ServiceReference<?> confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService) bundleContext
                                        .getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a AccountManager implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the AccountManager.
     */
    public static AccountManager getAccountManager()
    {
        if (mAccountManager == null)
        {
            ServiceReference<?> accountMgrReference
                = bundleContext.getServiceReference(
                    AccountManager.class.getName());
            mAccountManager
                = (AccountManager) bundleContext
                                        .getService(accountMgrReference);
        }
        return mAccountManager;
    }

    /**
     * When new protocol provider is registered we add needed listeners.
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        ServiceReference<?> serviceRef = serviceEvent.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know we are shutting down
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object sService = bundleContext.getService(serviceRef);

        if(sService instanceof NetworkAddressManagerService)
        {
            switch (serviceEvent.getType())
            {
                case ServiceEvent.REGISTERED:
                    if(this.networkAddressManagerService != null)
                        break;

                    this.networkAddressManagerService =
                        (NetworkAddressManagerService)sService;
                    networkAddressManagerService
                        .addNetworkConfigurationChangeListener(this);
                    break;
                case ServiceEvent.UNREGISTERING:
                    ((NetworkAddressManagerService)sService)
                        .removeNetworkConfigurationChangeListener(this);
                    break;
            }

            return;
        }

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
            return;

        ProtocolProviderService pp = (ProtocolProviderService)sService;
        logger.info("Protocol provider service changed: " +
                    logHasher(pp.getAccountID().getDisplayName()));

        switch (serviceEvent.getType())
        {
        case ServiceEvent.REGISTERED:
            this.handleProviderAdded((ProtocolProviderService)sService);
            break;

        case ServiceEvent.UNREGISTERING:
            this.handleProviderRemoved( (ProtocolProviderService) sService);
            break;
        }
    }

    /**
     * Add listeners to newly registered protocols.
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        if (!provider.supportsReconnection())
        {
            logger.info("Ignoring " + provider.getProtocolDisplayName() +
                        " because it does not support reconnection");
            return;
        }

        AccountID accountID = provider.getAccountID();
        logger.info("New protocol provider is coming " + accountID.getLoggableAccountID());

        logger.debug("Initialize account to unregistered");
        setAtLeastOneSuccessfulConnection(accountID, false);

        logger.debug("Listen for registration changes");
        provider.addRegistrationStateChangeListener(this);

        // Check whether the provider registered while we were setting up the
        // listener.
        if (provider.getRegistrationState() == RegistrationState.REGISTERED)
        {
            logger.debug("Provider already registered");
            handleProviderRegistered(provider);
        }
    }

    /**
     * Stop listening for events as the provider is removed.
     * Providers are removed this way only when there are modified
     * in the configuration. So as the provider is modified we will erase
     * every instance we got.
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        if (!provider.supportsReconnection())
        {
            logger.info("Ignoring " + provider.getProtocolDisplayName() +
                        " because it does not support reconnection");
            return;
        }

        AccountID accountID = provider.getAccountID();
        logger.info("Provider removed " + accountID.getLoggableAccountID());

        if(hasAtLeastOneSuccessfulConnection(accountID))
        {
            setAtLeastOneSuccessfulConnection(accountID, false);
        }

        provider.removeRegistrationStateChangeListener(this);

        mAutoReconnEnabledAccounts.remove(accountID);
        mNeedsReconnection.remove(accountID);

        // Unless the provider was removed in the process of being reloaded as
        // part of a reconnection attempt, remove this provider from the list
        // of reconnecting providers.
        boolean isReloading = accountID.isReloading();
        boolean isReconnecting = mCurrentlyReconnecting.containsKey(accountID);
        logger.info("Account " + accountID.getLoggableAccountID() +
                    " is reconnecting? " + isReconnecting +
                    ", is reloading? "  + isReloading);
        if(isReconnecting && !isReloading)
        {
            mCurrentlyReconnecting.remove(accountID).cancel();
        }
    }

    /**
     * The provider is registered so update the config to say so, add its
     * account to the list of registered accounts and remove from the list of
     * unregistering accounts.
     *
     * @param provider the ProtocolProviderService that has been registered.
     */
    private synchronized void handleProviderRegistered(ProtocolProviderService provider)
    {
        if (!provider.supportsReconnection())
        {
            logger.info("Ignoring " + provider.getProtocolDisplayName() +
                        " because it does not support reconnection");
            return;
        }

        AccountID accountID = provider.getAccountID();
        logger.debug(accountID.getLoggableAccountID() + " registered");

        if (!hasAtLeastOneSuccessfulConnection(accountID))
        {
            setAtLeastOneSuccessfulConnection(accountID, true);
        }

        mAutoReconnEnabledAccounts.put(
            accountID,
            new ArrayList<>(connectedInterfaces));

        if (mCurrentlyReconnecting.containsKey(accountID))
        {
            mCurrentlyReconnecting.remove(accountID).cancel();
        }

        mUnregisteringAccounts.remove(accountID);
    }

    /**
     * Fired when a change has occurred in the computer network configuration.
     *
     * @param event the change event.
     */
    public synchronized void configurationChanged(ChangeEvent event)
    {
        logger.info("Event received " + event +
                    " src=" + event.getSource() +
                    " by " + this);

        boolean overallConnectivityChanged = false;

        if(event.getType() == ChangeEvent.IFACE_UP)
        {
            logger.info("interface up");
            // no connection so one is up, lets connect
            if(connectedInterfaces.isEmpty())
            {
                overallConnectivityChanged = true;
                for (AccountID accountID : mNeedsReconnection)
                {
                    if(mCurrentlyReconnecting.containsKey(accountID))
                    {
                        // now lets cancel it and schedule it again
                        // so it will use this iface
                        mCurrentlyReconnecting.remove(accountID).cancel();
                    }

                    reconnect(accountID);
                }

                mNeedsReconnection.clear();
            }

            connectedInterfaces.add((String)event.getSource());
        }
        else if(event.getType() == ChangeEvent.IFACE_DOWN)
        {
            String ifaceName = (String)event.getSource();
            logger.info("interface down: " + ifaceName);

            connectedInterfaces.remove(ifaceName);

            if(connectedInterfaces.size() == 0)
            {
                overallConnectivityChanged = true;
                logger.info("none left; every pp will need reconnection");
                // we must disconnect every pp and put all to be need of reconnecting
                mNeedsReconnection.addAll(mAutoReconnEnabledAccounts.keySet());
                // there can by and some that are currently going to reconnect
                // must take care of them too, cause there is no net and they won't succeed
                mNeedsReconnection.addAll(mCurrentlyReconnecting.keySet());

                for (AccountID accountID : mNeedsReconnection)
                {
                    // if provider is scheduled for reconnect,
                    // cancel it there is no network
                    if(mCurrentlyReconnecting.containsKey(accountID))
                    {
                        mCurrentlyReconnecting.remove(accountID).cancel();
                    }

                    // don't reconnect just unregister if needed.
                    ProtocolProviderService pp =
                        getAccountManager().getProviderForAccount(accountID);

                    if (pp != null)
                    {
                        unregister(pp, false, false, null, -1);
                    }
                    else
                    {
                        logger.debug("Provider already null, so no need to " +
                           "unregister account: " + accountID.getLoggableAccountID());
                    }

                    // set the connection as failed.
                    setAtLeastOneSuccessfulConnection(accountID, false);
                }

                connectedInterfaces.clear();

                logger.info("Network is down!");
            }
            else
            {
                logger.debug(connectedInterfaces.size() + " connections remain");

                // Look to see if our providers are still registered
                for (AccountID accountID : mAutoReconnEnabledAccounts.keySet())
                {
                    ProtocolProviderService pp =
                        getAccountManager().getProviderForAccount(accountID);

                    // If provider is scheduled for reconnect, then leave it
                    // Similarly, if the provider is already null or
                    // unregistered, nothing to do.
                    if (mCurrentlyReconnecting.containsKey(accountID) ||
                        pp == null || !pp.isRegistered())
                    {
                        continue;
                    }

                    // Find out if the provider is still connected
                    pp.pollConnection();
                }
            }
        }

        if (!overallConnectivityChanged)
        {
            // Global connectivity hasn't changed - i.e. we haven't lost all
            // connectivity, or regained it - but individual accounts
            // may still need to reconnect if their local IP has changed,
            // as their route to host will be different.
            logger.info("Network state changed - check to see if protocols' " +
                        "routes have altered");

            for (AccountID accountID : mAutoReconnEnabledAccounts.keySet())
            {
                boolean reconnectAccount = !mNeedsReconnection.contains(accountID);

                try
                {
                    ProtocolProviderService provider =
                        getAccountManager().getProviderForAccount(accountID);
                    reconnectAccount &=
                        (provider != null) && provider.hasIpChanged();
                }
                catch (Exception e)
                {
                    logger.error("Failed to determine if account " +
                                 (accountID != null ? accountID.getLoggableAccountID() : null) +
                                 " should reconnect: ", e);
                    reconnectAccount = false;
                }

                if (reconnectAccount)
                {
                    // Only reconnect the account if the change in network
                    // configuration has actually affected the IP address it
                    // is exposing.
                    logger.info("Account ID " + accountID.getLoggableAccountID() +
                                             " has changed route. Reconnect.");
                    reconnect(accountID);
                }
                else
                {
                    logger.debug("Account ID " + (accountID != null ? accountID.getLoggableAccountID() : null) + " hasn't changed route");
                }
            }
        }
    }

    /**
     * Unregisters the account. Make sure to do it in separate thread
     * so we don't block other processing.
     * @param pp the protocol provider service to unregister.
     * @param reconnect if the account does not need unregistering
     *      shall we trigger reconnect. Its true when call called from
     *      reconnect.
     * @param reload if true and we have been asked to reconnect the account,
     * we should do so by reloading the account, rather than just
     * re-registering.
     * @param listener the listener used in reconnect method.
     * @param taskDelay the delay to be used for the reconnection task.
     */
    private void unregister(final ProtocolProviderService pp,
                            final boolean reconnect,
                            final boolean reload,
                            final RegistrationStateChangeListener listener,
                            final long taskDelay)
    {
        AccountID accountID = pp.getAccountID();
        logger.info("Start thread to unregister account: " + accountID.getLoggableAccountID());

        mUnregisteringAccounts.add(accountID);

        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    // We're about to unregister, so mark the connection as
                    // down
                    if(hasAtLeastOneSuccessfulConnection(accountID))
                    {
                        setAtLeastOneSuccessfulConnection(accountID, false);
                    }

                    RegistrationState registrationState =
                        pp.getRegistrationState();
                    logger.info(accountID.getLoggableAccountID() +
                                " registration state: " +
                                registrationState.getStateName());
                    logger.info("reconnect: " + reconnect);

                    // getRegistrationState() for some protocols(icq) can trigger
                    // registrationStateChanged so make checks here
                    // to prevent synchronize in registrationStateChanged
                    // and deadlock
                    if(registrationState.equals(
                           RegistrationState.UNREGISTERED) ||
                       registrationState.equals(
                           RegistrationState.CONNECTION_FAILED))
                    {
                        logger.debug(pp + " already " +
                            registrationState.getStateName() +
                            " so remove from unregistering providers");
                        mUnregisteringAccounts.remove(accountID);
                    }
                    else if (!registrationState.equals(
                                  RegistrationState.UNREGISTERING))
                    {
                        logger.debug("Attempting to unregister " + pp);
                        pp.unregister();
                    }
                }
                catch(Throwable t)
                {
                    logger.error("Error unregistering pp:" +
                                 pp, t);

                    // We tried but failed to unregister.  We don't want to
                    // retry, so remove the account ID from the list of
                    // unregistering accounts and add it to the accounts
                    // that need reconnection.
                    mUnregisteringAccounts.remove(accountID);
                    mNeedsReconnection.add(accountID);
                }

                if (reconnect)
                {
                    logger.debug("Asked to reconnect " + pp);
                    if(listener != null)
                        pp.removeRegistrationStateChangeListener(
                            listener);

                    if(taskDelay < 0)
                        return;

                    // Schedule the reconnect task with the given
                    // delay.
                    scheduleReconnectTask(accountID, taskDelay, reload);
                }
            }
        }, "ReconnectPluginUnregisterThread").start();
    }

    /**
     * Schedules a reconnect task for the given account ID with the
     * given delay (in ms) before trying to reconnect.
     *
     * @param accountID the ID of the account to schedule a reconnect task for.
     * @param taskDelay the delay to be used for the reconnect task.
     * @param reload if true we should reconnect by reloading the account,
     * rather than just re-registering.
     */
    private synchronized void scheduleReconnectTask(AccountID accountID,
                                                    long taskDelay,
                                                    boolean reload)
    {
        logger.info("Reconnect " + accountID.getLoggableAccountID() +
                    " after " + taskDelay + " ms. Reload? " + reload);

        if(sTimer != null)
        {
            // Cancel any existing task rather than just rescheduling it, as
            // it is not possible to schedule a task that has previously been
            // scheduled (or cancelled).
            if (mCurrentlyReconnecting.containsKey(accountID))
            {
                logger.debug("Reconnect task already scheduled for " +
                             accountID.getLoggableAccountID());
                mCurrentlyReconnecting.remove(accountID).cancel();
            }

            ReconnectTask task = new ReconnectTask(accountID, taskDelay, reload);
            mCurrentlyReconnecting.put(accountID, task);
            sTimer.schedule(task, taskDelay);
        }
        else
        {
            logger.warn("No timer to reconnect! " + accountID.getLoggableAccountID());
        }
    }

    /**
     * Logs the current status of the lists with account IDs,
     * that are currently in interest of the reconnect plugin.
     */
    @Override
    public String toString()
    {
        String loggableAutoReconnEnabledAccounts =
                sanitiseValuesInList(mAutoReconnEnabledAccounts.keySet(), AccountID::getLoggableAccountID);
        String loggableCurrentlyReconnecting =
                sanitiseValuesInList(mCurrentlyReconnecting.keySet(), AccountID::getLoggableAccountID);
        String loggableNeedsReconnectionAccounts =
                sanitiseValuesInList(mNeedsReconnection, AccountID::getLoggableAccountID);
        String loggableUnregisteringAccounts =
                sanitiseValuesInList(mUnregisteringAccounts, AccountID::getLoggableAccountID);

        return "ReconnectPluginActivator[ " +
                " hashCode: " + hashCode() +
                " connectedInterfaces: " + connectedInterfaces +
                " autoReconnEnabledAccounts: [" + loggableAutoReconnEnabledAccounts + "] " +
                " currentlyReconnecting: [" + loggableCurrentlyReconnecting + "] " +
                " needsReconnection: [" + loggableNeedsReconnectionAccounts + "] " +
                " unregisteringAccounts: [" + loggableUnregisteringAccounts + "]]";
    }

    /**
     * The method is called by a <code>ProtocolProviderService</code>
     * implementation whenever a change in the registration state of the
     * corresponding provider had occurred.
     *
     * @param evt the event describing the status change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getSource() instanceof ProtocolProviderService)
        {
            ProtocolProviderService pp = (ProtocolProviderService)evt.getSource();

            logger.info(pp.getAccountID().getLoggableAccountID() +
                        " registration state: " +
                        evt.getNewState().getStateName());
        }

        // we don't care about protocol providers that don't support
        // reconnection and we are interested only in few state changes
        if (!(evt.getSource() instanceof ProtocolProviderService) ||
            !(evt.getNewState().equals(RegistrationState.REGISTERED) ||
              evt.getNewState().equals(RegistrationState.UNREGISTERED) ||
              evt.getNewState().equals(RegistrationState.CONNECTION_FAILED)))
        {
            return;
        }

        // Delegate this to another thread to prevent deadlocks (current thread might have
        // SipRegistrarConnection lock and will be stuck trying to get 'this' lock if another
        // thread with 'this' lock is waiting for SipRegistrarConnection lock).
        threadingService.submit("ReconnectPluginHandleRegistrationStateChanged", new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    ProtocolProviderService pp = (ProtocolProviderService)evt.getSource();

                    if (evt.getNewState().equals(RegistrationState.CONNECTION_FAILED) ||
                        evt.getNewState().equals(RegistrationState.UNREGISTERED))
                    {
                        handleProviderConnectionFailedOrUnregistered(evt, pp);
                    }
                    else if(evt.getNewState().equals(RegistrationState.REGISTERED))
                    {
                        handleProviderRegistered(pp);
                    }
                }
                catch(Throwable ex)
                {
                    logger.error("Error dispatching protocol registration change", ex);
                }
            }
        });
    }

    /**
     * Handle a RegistrationState.CONNECTION_FAILED or a RegistrationState.UNREGISTERED event.
     * @param evt
     * @param pp
     */
    private synchronized void handleProviderConnectionFailedOrUnregistered(
        RegistrationStateChangeEvent evt, ProtocolProviderService pp)
    {
        AccountID accountID = pp.getAccountID();
        String loggableAccountID = accountID.getLoggableAccountID();
        logger.info(loggableAccountID + ": registration state changed to " + evt.getNewState().getStateName());

        if (!hasAtLeastOneSuccessfulConnection(accountID))
        {
            // Because this provider hasn't had a successful connection yet, the account is probably misconfigured.
            if (evt.getReasonCode() == RegistrationStateChangeEvent.REASON_NON_EXISTING_USER_ID)
            {
                logger.warn("Failed to connect account " + loggableAccountID +
                            " - no previous successful connection");
                return;
            }
        }

        // The connection has failed, so mark it as such.
        setAtLeastOneSuccessfulConnection(accountID, false);

        if (connectedInterfaces.isEmpty())
        {
            logger.info("Adding pp to needsReconnection " + loggableAccountID + " state: " + this);
            mNeedsReconnection.add(accountID);

            if (mCurrentlyReconnecting.containsKey(accountID))
            {
                logger.info("Removing " + loggableAccountID + " from currentlyReconnecting");
                mCurrentlyReconnecting.remove(accountID).cancel();
            }
        }
        else
        {
            // Network is up but we cannot reconnect, so try again after a delay.
            logger.info("Reconnect failed for " + loggableAccountID + " even though we have a network connection.");
            reconnect(accountID);
        }

        // unregister can finish and with connection failed, the protocol is unable to unregister
        mUnregisteringAccounts.remove(accountID);
        logger.info(this + " got Connection Failed for " + loggableAccountID);
    }

    /**
     * Method to schedule a reconnect for a account ID.
     * @param accountID the account ID
     */
    private void reconnect(final AccountID accountID)
    {
        ProtocolProviderService pp =
                          getAccountManager().getProviderForAccount(accountID);

        if (pp == null)
        {
            // This is expected if the account is shutting down, e.g. if the
            // user signs out of chat.
            logger.warn("Unable to reconnect - null provider for account: " +
                        accountID.getLoggableAccountID());
            return;
        }

        final long delay;
        final boolean reload;

        if (mCurrentlyReconnecting.containsKey(accountID))
        {
            ReconnectTask currentReconnectTask =
                                         mCurrentlyReconnecting.get(accountID);

            // If we've previously tried to reconnect, double the previous
            // delay until we reach the maximum delay.
            long currentDelay = currentReconnectTask.mDelay;
            delay = Math.min(currentDelay * 2, MAX_RECONNECT_DELAY*1000);

            // If we have hit the maximum reconnect delay, this means that
            // simply re-registering to try to reconnect is not working, so we
            // will try reloading the account instead.  If that fails, we'll
            // wait for the maximum reconnect delay before trying again,
            // alternating between reloading and re-registering until
            // registration succeeds.  Each time we schedule the reconnect
            // task, we set the reload value to the previous task's reload
            // value.  This is because, if this task has already run, it will
            // have already reversed its reload value.  If it was cancelled
            // before running, we want to set the same reload value again to
            // ensure we actually do alternate.
            reload = (delay == MAX_RECONNECT_DELAY*1000) &&
                      currentReconnectTask.mReload;
        }
        else
        {
            delay = (long)(RECONNECT_DELAY_MIN
                + Math.random() * RECONNECT_DELAY_MAX)*1000;
            reload = false;
        }

        logger.info("Schedule reconnect of " + accountID.getLoggableAccountID() +
                    " with delay of " + delay + "ms. Reloading? " + reload);

        // start registering after the pp has unregistered
        RegistrationStateChangeListener listener =
            new RegistrationStateChangeListener()
        {
            public void registrationStateChanged(RegistrationStateChangeEvent evt)
            {
                 logger.debug("Registration state changed for pp " + pp +
                                                            " evt " + evt);

                 if (evt.getSource() instanceof ProtocolProviderService)
                 {
                    if (evt.getNewState().equals(
                                            RegistrationState.UNREGISTERED)
                        || evt.getNewState().equals(
                                      RegistrationState.CONNECTION_FAILED))
                     {
                         synchronized(ReconnectPluginActivator.this)
                         {
                             // The connection has failed, so mark it as such.
                             if (hasAtLeastOneSuccessfulConnection(accountID))
                             {
                                 setAtLeastOneSuccessfulConnection(accountID, false);
                             }

                             pp.removeRegistrationStateChangeListener(this);

                             if(connectedInterfaces.size() == 0)
                             {
                                 // well there is no network we just need
                                 // this provider in needs reconnection when
                                 // there is one
                                 // means we started unregistering while
                                 // network was going down and meanwhile there
                                 // were no connected interface, this happens
                                 // when we have more than one connected
                                 // interface and we got 2 events for down iface
                                 mNeedsReconnection.add(accountID);

                                 if (mCurrentlyReconnecting.containsKey(accountID))
                                 {
                                    mCurrentlyReconnecting.remove(accountID).cancel();
                                 }

                                 logger.debug("No connected interface to restart");
                                 return;
                             }

                             // Schedule the reconnect task with the given
                             // delay and reload request.
                             scheduleReconnectTask(accountID, delay, reload);
                         }
                     }
                 }
            }
        };
        pp.addRegistrationStateChangeListener(listener);

        // as we will reconnect, lets unregister
        unregister(pp, true, reload, listener, delay);
    }

    /**
     * The task executed by the timer when time for reconnect comes.
     */
    private class ReconnectTask extends TimerTask
    {
        /**
         * The ID of the account to reconnect.
         */
        private final AccountID mAccountID;

        /**
         * The delay (in ms) with which this task was scheduled.
         */
        private final long mDelay;

        /**
         * If true, we should try to reload the account, rather than just
         * re-registering.
         */
        private boolean mReload;

        /**
         * The thread to execute this task.
         */
        private Thread mThread = null;

        /**
         * Creates the task.
         *
         * @param accountID the ID of the account to reconnect
         * @param delay the delay (in ms) with which this task was scheduled
         * @param reload if true, we should try to reload the account, rather
         * than just re-registering.
         */
        public ReconnectTask(AccountID accountID, long delay, boolean reload)
        {
            mAccountID = accountID;
            mDelay = delay;
            mReload = reload;
        }

        /**
         * Reconnects the provider.
         */
        public void run()
        {
            if(mThread == null || !Thread.currentThread().equals(mThread))
            {
                mThread = new Thread(this, "ReconnectPluginReconnectThread");
                mThread.start();
            }
            else
            {

                if (mAccountID.isEnabled())
                {
                    logger.info("Account is enabled for " +
                                mAccountID.getLoggableAccountID() +
                                ". Delay = " + mDelay + ", reload = "
                                + mReload + ".");

                    if ((mDelay == MAX_RECONNECT_DELAY*1000) && mReload)
                    {
                        // We've hit the maximum reconnect delay and we've
                        // been asked to reload the account, so try doing
                        // that, as simply trying to re-register isn't working.

                        logger.info("Maximum reconnect delay hit for " +
                                    mAccountID.getLoggableAccountID() +
                                    " - try reloading the account.");

                        // Set reload to false so that, if reloading fails to
                        // reconnect the account, we will try to re-register
                        // the account instead next time.
                        mReload = false;
                        logger.info("Set reload to " + mReload);

                        // Before unloading the account, add an account
                        // manager listener so that we will be notified once
                        // the account has been successfully unloaded.
                        AccountManagerListener accountManagerListener =
                                                   new AccountManagerListener()
                        {
                            @Override
                            public void handleAccountManagerEvent(AccountManagerEvent event)
                            {
                                if (event.getType() == AccountManagerEvent.ACCOUNT_REMOVED &&
                                    event.getFactory().getProtocolName().equals(
                                                          mAccountID.getProtocolName()))
                                {
                                    // The account has finished unloading, so
                                    // remove this listener then load it again.
                                    logger.info("Account removed for " +
                                                mAccountID.getLoggableAccountID());
                                    getAccountManager().removeListener(this);
                                    loadAccount();
                                }
                            }
                        };

                        getAccountManager().addListener(accountManagerListener);

                        try
                        {
                            // Before unloading the account, set the account
                            // reloading property to true so that it is clear
                            // that the account is disabled temporarily
                            // because we are trying to reconnect and not
                            // because the user has chosen to disable the
                            // account manually.
                            mAccountID.putAccountProperty(
                                ProtocolProviderFactory.IS_ACCOUNT_RELOADING,
                                String.valueOf(true));
                            getAccountManager().unloadAccount(mAccountID);
                        }
                        catch (OperationFailedException e)
                        {
                            // Something went wrong when unloading the
                            // account. All we can do is log and try to
                            // reconnect again when the timer next pops. Also,
                            // make sure we remove the account manager listener.
                            logger.error("Failed to unload account " +
                                         mAccountID.getLoggableAccountID(), e);
                            getAccountManager().removeListener(accountManagerListener);
                        }
                    }
                    else
                    {
                        // We either have not yet reached the maximum
                        // reconnect delay, or we have hit it and the previous
                        // attempt to reload the account failed, so we've been
                        // asked to try re-registering again this time instead.

                        logger.info("Not reloading, start re-registering " +
                                    mAccountID.getLoggableAccountID());

                        // Set reload to true so that, if re-registering fails
                        // to reconnect the account, we will try to reload
                        // the account instead next time.
                        mReload = true;
                        logger.info("Set reload to " + mReload);

                        try
                        {
                            ProtocolProviderService provider =
                                getAccountManager().getProviderForAccount(mAccountID);
                            provider.register(
                                getUIService().getDefaultSecurityAuthority(provider));
                        }
                        catch (Exception ex)
                        {
                            // We need to catch a generic exception here to be
                            // sure that we will handle any exception that may
                            // be hit during executing of this task.
                            // Otherwise, the reconnect plugin will be killed
                            // and the user will have to restart their client
                            // to recover any disconnected accounts.
                            logger.error(
                                "cannot re-register provider will keep going " +
                                    mAccountID.getLoggableAccountID(), ex);
                        }
                    }
                }
                else if (mAccountID.isReloading())
                {
                    // The account is disabled but reloading.  This means
                    // that we previously tried to reload the account but
                    // reloading failed.  All we can do is try to load it
                    // again.
                    logger.info("Account reloading - load account " +
                                mAccountID.getLoggableAccountID());
                    loadAccount();
                }
                else
                {
                    logger.info("Not reconnecting " +
                                mAccountID.getLoggableAccountID() +
                                " as account disabled");
                }
            }
        }

        /**
         * Loads the account associated with this ReconnectTask.
         */
        protected void loadAccount()
        {

            try
            {
                logger.info("Loading account for " +
                            mAccountID.getLoggableAccountID());
                getAccountManager().loadAccount(mAccountID);

                // We successfully loaded the account, so set the account
                // reloading property to false.  If we didn't do this and the
                // user were to choose to disable this account in future, the
                // reconnect plug-in would think it was disabled as part of an
                // automatic reload, so would automatically re-enable it.
                mAccountID.putAccountProperty(
                    ProtocolProviderFactory.IS_ACCOUNT_RELOADING,
                    String.valueOf(false));
            }
            catch (OperationFailedException e)
            {
                // Something went wrong when loading the account.  All we can
                // do is log and try to reconnect again when the timer next
                // pops.
                logger.error("Failed to load account " +
                             mAccountID.getLoggableAccountID(), e);
            }
        }
    }

    /**
     * Check does the supplied protocol has the property set for at least
     * one successful connection.
     * @param accountID the account ID
     * @return true if property exists.
     */
    private boolean hasAtLeastOneSuccessfulConnection(AccountID accountID)
    {
       String value = (String)getConfigurationService().user().getProperty(
           ATLEAST_ONE_CONNECTION_PROP + "."
           + accountID.getAccountUniqueID());

       if(value == null || !value.equals(Boolean.TRUE.toString()))
           return false;
       else
           return true;
    }

    /**
     * Changes the property about at least one successful connection.
     * @param accountID the account ID
     * @param value the new value true or false.
     */
    private void setAtLeastOneSuccessfulConnection(
        AccountID accountID, boolean value)
    {
       logger.debug("Setting successful connection to " + value + " for " +
           accountID.getProtocolDisplayName());

       getConfigurationService().user().setProperty(
           ATLEAST_ONE_CONNECTION_PROP + "."
            + accountID.getAccountUniqueID(),
           Boolean.valueOf(value).toString());
    }
}
