/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import net.java.sip.communicator.plugin.generalconfig.autoaway.AutoAwayWatcher;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.diagnostics.DiagnosticsService;
import net.java.sip.communicator.service.gui.ConfigurationForm;
import net.java.sip.communicator.service.gui.LazyConfigurationForm;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.headsetmanager.HeadsetManagerService;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.phonenumberutils.PhoneNumberUtilsService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;
import net.java.sip.communicator.service.reset.ResetService;
import net.java.sip.communicator.service.resources.ResourceManagementServiceUtils;
import net.java.sip.communicator.service.shutdown.ShutdownService;
import net.java.sip.communicator.service.sysactivity.SystemActivityNotificationsService;
import net.java.sip.communicator.service.systray.SystrayService;
import net.java.sip.communicator.util.AbstractServiceDependentActivator;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.audionotifier.AudioNotifierService;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.Logger;
import org.jitsi.util.StringUtils;

/**
 * The general configuration form activator.
 *
 * @author Yana Stamcheva
 */
public class GeneralConfigPluginActivator
    extends AbstractServiceDependentActivator
    implements  ServiceListener
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(GeneralConfigPluginActivator.class);

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The account manager service.
     */
    private static AccountManager accountManager;

    /**
     * The systray service.
     */
    private static SystrayService systrayService;

    /**
     * The bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The user interface service.
     */
    private static UIService uiService;

    /**
     * The status update thread.
     */
    private static AutoAwayWatcher runner = null;

    /**
     * The indicator which determines whether {@link #startThread()} has been
     * called and thus prevents calling it more than once.
     */
    private static boolean startThreadIsCalled = false;

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourceService;

    /**
     * The notification service.
     */
    private static NotificationService notificationService;

    /**
     * The audio notifier service.
     */
    private static AudioNotifierService audioNotifierService;

    /**
     * The analytics service
     */
    private static AnalyticsService analyticsService;

    /**
     * The reset service
     */
    private static ResetService resetService;

    /**
     * The shutdown service
     */
    private static ShutdownService shutdownService;

    /**
     * The Cos service
     */
    private static ClassOfServiceService cosService;

    /**
     * The CommPortal service
     */
    private static CommPortalService commPortalService;

    private static DiagnosticsService diagsService;

    /**
     * The phone number utils service
     */
    private static PhoneNumberUtilsService phoneNumberService;

    /**
     * The system activity service
     */
    private static SystemActivityNotificationsService systemActivityService;

    /**
     * The global status service
     */
    private static GlobalStatusService globalStatusService;

    /**
     * The conference service
     */
    private static ConferenceService conferenceService;

    /**
     * The headset manager service
     */
    private static HeadsetManagerService headsetManagerService;

    /**
     * Indicates if the general configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP =
        "net.java.sip.communicator.plugin.generalconfig.DISABLED";

    /**
     * Indicates if the Chat configuration panel should be disabled, i.e.
     * not visible to the user.
     */
    private static final String CHAT_CONFIG_DISABLED_PROP =
        "net.java.sip.communicator.plugin.generalconfig.messageconfig.DISABLED";

    /**
     * Indicates if IM functionality is enabled in the client.
     */
    private static final String PROPERTY_IM_ENABLED =
                                      "net.java.sip.communicator.im.IM_ENABLED";

    /**
     * Indicates if the AutoAway configuration panel should be disabled, i.e.
     * not visible to the user.
     */
    private static final String AUTO_AWAY_CONFIG_DISABLED_PROP =
        "net.java.sip.communicator.plugin.generalconfig.autoawayconfig.DISABLED";

    /**
     * Indicates whether we should automatically log in when the application
     * starts (assuming we have valid log username and password).
     */
    private static final String DEFAULT_AUTO_LOG_IN =
        "net.java.sip.communicator.plugin.generalconfig.autologin.ENABLED";

    /**
     * Name of the provisioning username in the configuration service.
     */
    private static final String PROPERTY_PROVISIONING_USERNAME =
        "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";

    /**
     * Name of the provisioning password in the configuration service (HTTP
     * authentication).
     */
    private static final String PROPERTY_PROVISIONING_PASSWORD =
        "net.java.sip.communicator.plugin.provisioning.auth";

    /**
     * Name of the encrypted provisioning password in the configuration
     * service.
     */
    private static final String PROPERTY_PROVISIONING_ENCRYPTED_PASSWORD =
        "net.java.sip.communicator.plugin.provisioning.auth.ENCRYPTED_PASSWORD";

    /**
     * Name of the active user being used by configuration.
     */
    private static final String PROPERTY_ACTIVE_USER =
        "net.java.sip.communicator.plugin.provisioning.auth.ACTIVE_USER";

    /**
     * Service Registration for the Chat form. Needs to be stored so that we
     * can remove it if IM becomes disabled after the Chat tab was added.
     */
    private ServiceRegistration<?> mChatServiceRegistration;

    /**
     * Service Registration for the Calls form. Needs to be stored so that we
     * can remove it if calls become disabled after the Calls tab was added.
     */
    private ServiceRegistration<?> mCallsServiceRegistration;

    /**
     * The object that updates paths to ringtones stored in config in cases
     * where we need to modify them (e.g. over upgrade with name change).
     */
    private RingtonePathUpdater ringtonePathUpdater;

    /**
     * Starts this bundle.
     */
    public void start(Object dependentService)
    {
        ConfigurationService configService = getConfigurationService();

        uiService = (UIService)dependentService;

        ringtonePathUpdater = new RingtonePathUpdater();

        final Dictionary<String, String> generalProperties =
                new Hashtable<>();
        generalProperties.put(ConfigurationForm.FORM_TYPE,
                              ConfigurationForm.GENERAL_TYPE);

        // If the general configuration form is disabled don't continue.
        if (!configService.user().getBoolean(DISABLED_PROP, false))
        {
            // General tab
            bundleContext.registerService(
                                ConfigurationForm.class.getName(),
                                new LazyConfigurationForm(
                                    "net.java.sip.communicator.plugin." +
                                    "generalconfig.GeneralConfigurationPanel",
                                    getClass().getClassLoader(),
                                    "plugin.generalconfig.PLUGIN_ICON",
                                    "service.gui.GENERAL",
                                    0),
                                generalProperties);

            // Calls tab
            LazyConfigurationForm callsForm = new LazyConfigurationForm(
                "net.java.sip.communicator.plugin." +
                "generalconfig.CallsConfigurationPanel",
                getClass().getClassLoader(),
                "plugin.generalconfig.CALLS_ICON",
                "service.gui.CALLS",
                1);

            // CTD has no configuration options unless remote CTD is also enabled (that's the part
            // that allows the user to specify which phone to use for the 1st leg of the call).
            if (configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_VOIPALLOWED, true) ||
                (configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_CTD_ALLOWED, true) &&
                 configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_CTD_REMOTE_ENABLED, true) &&
                 configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_CTD_ENABLED, true)))
            {
                mCallsServiceRegistration = bundleContext.registerService(
                    ConfigurationForm.class.getName(),
                    callsForm,
                    generalProperties);
            }

            // Listen for changes to whether calls are enabled and change the
            // visibility of the Calls tab accordingly
            configService.user().addPropertyChangeListener(new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent e)
                {
                    // We care about changes to any of these properties
                    if ((e.getPropertyName().equals(ClassOfServiceService.CONFIG_COS_VOIPALLOWED)) ||
                        (e.getPropertyName().equals(ClassOfServiceService.CONFIG_COS_CTD_ALLOWED)) ||
                        (e.getPropertyName().equals(ClassOfServiceService.CONFIG_COS_CTD_REMOTE_ENABLED)) ||
                        (e.getPropertyName().equals(ClassOfServiceService.CONFIG_COS_CTD_ENABLED)))
                    {
                        Object oldValueObj = e.getOldValue();
                        Boolean oldValue = (oldValueObj == null) ?
                                null : Boolean.parseBoolean(oldValueObj.toString());
                        Object newValueObj = e.getNewValue();
                        Boolean newValue = (newValueObj == null) ?
                                null : Boolean.parseBoolean(newValueObj.toString());

                        if (newValue != oldValue)
                        {
                            logger.info(e.getPropertyName() + " has been changed");

                            if (configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_VOIPALLOWED, true) ||
                                (configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_CTD_ALLOWED, true) &&
                                 configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_CTD_REMOTE_ENABLED, true) &&
                                 configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_CTD_ENABLED, true)))
                            {
                                logger.info("Show calls tab");
                                mCallsServiceRegistration = bundleContext.registerService(
                                    ConfigurationForm.class.getName(),
                                    callsForm,
                                    generalProperties);
                            }
                            else
                            {
                                logger.info("Hide calls tab");
                                mCallsServiceRegistration.unregister();
                            }
                        }
                    }
                }
            });

            // Chat tab - only create it if at least one of its sections is enabled
            if (!configService.global().getBoolean(CHAT_CONFIG_DISABLED_PROP, false) ||
                !configService.global().getBoolean(AUTO_AWAY_CONFIG_DISABLED_PROP, false))
            {
                final LazyConfigurationForm chatForm =
                    new LazyConfigurationForm(
                        "net.java.sip.communicator.plugin." +
                        "generalconfig.ChatConfigurationPanel",
                        getClass().getClassLoader(),
                        "plugin.generalconfig.CHAT_ICON",
                        "service.gui.CHAT",
                        3);

                if (configService.user().getBoolean(PROPERTY_IM_ENABLED, false))
                {
                    mChatServiceRegistration = bundleContext.registerService(
                        ConfigurationForm.class.getName(),
                        chatForm,
                        generalProperties);
                }

                // Listen for changes to whether IM is enabled and change the
                // visibility of the Chat tab accordingly
                configService.user().addPropertyChangeListener(PROPERTY_IM_ENABLED,
                    new PropertyChangeListener()
                {
                    public void propertyChange(PropertyChangeEvent e)
                    {
                        Object oldValueObj = e.getOldValue();
                        Boolean oldValue = (oldValueObj == null) ?
                                null : Boolean.parseBoolean(oldValueObj.toString());
                        Object newValueObj = e.getNewValue();
                        Boolean newValue = (newValueObj == null) ?
                                null : Boolean.parseBoolean(newValueObj.toString());

                        if (newValue != oldValue)
                        {
                            logger.info(PROPERTY_IM_ENABLED + " has been changed to " +
                                        newValue + " - changing visibility of chat tab");

                            if (newValue)
                            {
                                mChatServiceRegistration = bundleContext.registerService(
                                    ConfigurationForm.class.getName(),
                                    chatForm,
                                    generalProperties);
                            }
                            else
                            {
                                mChatServiceRegistration.unregister();
                            }
                        }
                    }
                });
            }
        }

        try
        {
            /*
             * Wait for the first ProtocolProviderService to register in order to
             * start the auto-away functionality i.e. to call #startThread().
             */
            ServiceReference<?>[] protocolRefs = bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);
            if(protocolRefs != null && protocolRefs.length > 0)
            {
                synchronized (GeneralConfigPluginActivator.class)
                {
                    if (!startThreadIsCalled)
                    {
                        startThread();
                        startThreadIsCalled = true;
                    }
                }
            }
            else
            {
                bundleContext.addServiceListener(this);
            }
        }
        catch(Throwable t)
        {
            // not supposed to happen
            logger.error("Error starting auto away thread", t);
        }

        logger.info("PREFERENCES PLUGIN... [REGISTERED]");

        // If the app name changed over upgrade and the user previously had a custom
        // ringtone, we need to update all the relevant config properties to point
        // to the file now stored under the new app name.
        String oldName = System.getProperty("net.java.sip.communicator.SC_OLD_APP_NAME");
        String newName = System.getProperty("net.java.sip.communicator.SC_HOME_DIR_NAME");

        if (!StringUtils.isNullOrEmpty(oldName) && !oldName.equals(newName))
        {
            logger.debug("Name has changed, check if need to update path" +
                                 " to custom ringtone");
            String ringtonePath = ringtonePathUpdater.updateRingtonePaths(oldName,
                                                                          newName);

            // The ringtone path will be null if the user has never changed it.
            // We do not want to register a notification with a null value.
            if (!StringUtils.isNullOrEmpty(ringtonePath))
            {
                logger.debug("Ringtone path is not empty, so register " +
                        "a new notification for " + ringtonePath);
                // Wait until the NotificationService has started and register
                // the ringtone as a notification sound.
                ServiceUtils.getService(GeneralConfigPluginActivator.bundleContext,
                                        NotificationService.class,
                                        new ServiceUtils.ServiceCallback<NotificationService>()
                                        {
                                            @Override
                                            public void onServiceRegistered(
                                                    NotificationService service)
                                            {
                                                GeneralConfigPluginActivator
                                                        .getNotificationService();
                                                GeneralConfigPluginActivator
                                                        .notificationService.registerNewRingtoneNotification(
                                                                ringtonePath);
                                            }
                                        });
            }
        }
    }

    /**
     * The dependent class. We are waiting for the ui service.
     * @return
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * The bundle context to use.
     * @param context the context to set.
     */
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    /**
     * Stops this bundle.
     * @param bc the bundle context
     */
    public void stop(BundleContext bc)
    {
        // If 'auto login' is disabled, remove all passwords etc otherwise
        // it is possible to manually enabled auto-login and then AD will
        // log in as this user when next started.
        if (! configService.user().getBoolean(DEFAULT_AUTO_LOG_IN, true))
        {
            logger.info("No auto log in - remove passwords etc.");
            configService.global().removeProperty(PROPERTY_ACTIVE_USER);
            configService.global().removeProperty(
                PROPERTY_PROVISIONING_USERNAME);
            configService.user().removeProperty(
                PROPERTY_PROVISIONING_PASSWORD);
            configService.user().removeProperty(
                PROPERTY_PROVISIONING_ENCRYPTED_PASSWORD);
        }
        stopThread();
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService() {
        if(configService == null) {
            ServiceReference<?> configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }

    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(accountManager == null) {
            ServiceReference<?> configReference = bundleContext
                .getServiceReference(AccountManager.class.getName());

            accountManager = (AccountManager) bundleContext
                .getService(configReference);
        }

        return accountManager;
    }

    /**
     * Returns the <tt>SystrayService</tt> obtained from the bundle
     * context.
     * @return the <tt>SystrayService</tt> obtained from the bundle
     * context
     */
    static SystrayService getSystrayService()
    {
        if(systrayService == null) {
            ServiceReference<?> configReference = bundleContext
                .getServiceReference(SystrayService.class.getName());

            systrayService = (SystrayService) bundleContext
                .getService(configReference);
        }

        return systrayService;
    }

    /**
     * Returns the <tt>UIService</tt>.
     *
     * @return the <tt>UIService</tt>
     */
    static UIService getUIService()
    {
        return uiService;
    }

    /**
     * Returns the <tt>PhoneNumberUtilsService</tt>.
     *
     * @return the <tt>PhoneNumberUtilsService</tt>
     */
    static PhoneNumberUtilsService getPhoneNumberUtils()
    {
        if (phoneNumberService == null)
        {
            phoneNumberService = ServiceUtils.getService(bundleContext,
                                                 PhoneNumberUtilsService.class);
        }

        return phoneNumberService;
    }

    /**
     * Implements ServiceListener#serviceChanged(ServiceEvent). Waits for the
     * first ProtocolProviderService to register in order to start the auto-away
     * functionality i.e. to call #startThread().
     * @param serviceEvent the <tt>ServiceEvent</tt> that notified us
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        switch (serviceEvent.getType())
        {
        case ServiceEvent.MODIFIED:
        case ServiceEvent.REGISTERED:
            Object service
                = bundleContext.getService(serviceEvent.getServiceReference());
            if (service instanceof ProtocolProviderService)
            {
                synchronized (GeneralConfigPluginActivator.class)
                {
                    if (!startThreadIsCalled)
                    {
                        startThread();
                        startThreadIsCalled = true;
                    }
                }

                bundleContext.removeServiceListener(this);
            }
            break;

        default:
            break;
        }
    }

    /**
     * Starts the auto away thread.
     */
    private static void startThread()
    {
        if (runner == null)
            runner = new AutoAwayWatcher();
    }

    /**
     * Stops the auto away thread.
     */
    private static void stopThread()
    {
        if (runner != null)
        {
            runner.stop();
            runner = null;
        }
    }

    /**
     * Returns an array of all available protocol providers.
     * @return an array of all available protocol providers
     */
    public static ProtocolProviderService[] getProtocolProviders()
    {
        // get the protocol provider factory
        BundleContext bundleContext = GeneralConfigPluginActivator.bundleContext;

        ServiceReference<?>[] serRefs = null;
        // String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL + "="
        // + ProtocolNames.SIP + ")";

        try
        {
            // serRefs = bundleContext.getServiceReferences(
            // ProtocolProviderFactory.class.getName(), osgiFilter);
            serRefs = bundleContext.getAllServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error(ex);
        }

        if (serRefs == null || serRefs[0] == null)
        {
            return null;
        }

        Set<ProtocolProviderService> pps = new HashSet<>();

        for (ServiceReference<?> serviceReference : serRefs)
        {
            ProtocolProviderService protocolProvider
                = (ProtocolProviderService)
                    bundleContext.getService(serviceReference);
            pps.add(protocolProvider);
        }

        return pps.toArray(new ProtocolProviderService[0]);
    }

    /**
     * Get a list of all available personal contact stores.
     *
     * @return a list of available personal contact stores.
     */
    static List<AccountID> getContactStores()
    {
        // Ideally, we could fetch a list of ProtocolProviderServices that
        // support OperationSetServerStoredUpdatableContactInfo, but only
        // registered protocol providers can be found and we expect only one
        // contact store to be registered at a time.  We therefore look for
        // accounts that have been saved in config and filter by protocol name.
        List<AccountID> accounts = new ArrayList<>();

        for (AccountID accountID : getAccountManager().getStoredAccounts())
        {
            String accountProtocolName = accountID.getProtocolName();

            if ("CommPortal".equalsIgnoreCase(accountProtocolName) ||
                ProtocolNames.ADDRESS_BOOK.equalsIgnoreCase(accountProtocolName))
            {
                accounts.add(accountID);
            }
        }

        return accounts;
    }

    /**
     * Gets the service giving access to all application resources.
     *
     * @return the service giving access to all application resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourceService;
    }

    /**
     * Gets the service which handles registration of notification events,
     * such as playing ringtones when an incoming call is received.
     */
    public static NotificationService getNotificationService() {
        if (notificationService == null)
            notificationService = ServiceUtils.getService(
                                      bundleContext, NotificationService.class);

        return notificationService;
    }

    /**
     * Gets the service which plays audio clips.
     */
    public static AudioNotifierService getAudioNotifierService() {
        if (audioNotifierService == null)
            audioNotifierService = ServiceUtils.getService(
                                     bundleContext, AudioNotifierService.class);

        return audioNotifierService;
    }

    /**
     * Gets the analytics service.
     */
    public static AnalyticsService getAnalyticsService() {
        if (analyticsService == null)
            analyticsService = ServiceUtils.getService(
                                     bundleContext, AnalyticsService.class);

        return analyticsService;
    }

    /**
     * @return a reference to the registered Cos service
     */
    public static ClassOfServiceService getCosService()
    {
        if (cosService == null)
            cosService = ServiceUtils.getService(bundleContext,
                                                  ClassOfServiceService.class);

        return cosService;
    }

    /**
     * @return a reference to the registered CommPortal service
     */
    public static CommPortalService getCommPortalService()
    {
        if (commPortalService == null)
            commPortalService = ServiceUtils.getService(bundleContext,
                                                 CommPortalService.class);

        return commPortalService;
    }

    public static DiagnosticsService getDiagnosticsService()
    {
        if (diagsService == null)
        {
            diagsService = ServiceUtils.getService(bundleContext,
                                                   DiagnosticsService.class);
        }
        return diagsService;
    }

    /**
     * Gets the reset service.
     */
    public static ResetService getResetService()
    {
        if (resetService == null)
            resetService = ServiceUtils.getService(bundleContext,
                                                   ResetService.class);

        return resetService;
    }

    /**
     * Gets the shutdown service
     */
    public static ShutdownService getShutdownService()
    {
        if (shutdownService == null)
        {
            shutdownService = ServiceUtils.getService(bundleContext,
                                                      ShutdownService.class);
        }

        return shutdownService;
    }

    /**
     * @return the System Activity Notifications Service.
     */
    public static SystemActivityNotificationsService
                                         getSystemActivityNotificationsService()
    {
        if (systemActivityService == null)
        {
            systemActivityService = ServiceUtils.getService(bundleContext,
                                      SystemActivityNotificationsService.class);
        }

        return systemActivityService;
    }

    /**
     * @return the Global Status Service
     */
    public static GlobalStatusService getGlobalStatusService()
    {
        if (globalStatusService == null)
        {
            globalStatusService = ServiceUtils.getService(bundleContext,
                                                     GlobalStatusService.class);
        }

        return globalStatusService;
    }

    /**
     * @return the Conference Service
     */
    public static ConferenceService getConferenceService()
    {
        if (conferenceService == null)
        {
            conferenceService = ServiceUtils.getService(bundleContext,
                                                     ConferenceService.class);
        }

        return conferenceService;
    }

    /**
     * @return the Headset Manager Service
     */
    public static HeadsetManagerService getheadsetManagerService()
    {
        if (headsetManagerService == null)
        {
            headsetManagerService = ServiceUtils.getService(bundleContext,
                                                HeadsetManagerService.class);
        }

        return headsetManagerService;
    }
}
