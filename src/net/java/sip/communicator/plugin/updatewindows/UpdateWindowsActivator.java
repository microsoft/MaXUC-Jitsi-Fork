/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.updatewindows;

import java.beans.*;
import java.util.*;
import java.util.concurrent.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.version.Version;
import org.jitsi.service.version.VersionService;
import org.osgi.framework.*;

import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.service.update.*;
import net.java.sip.communicator.util.AbstractServiceDependentActivator;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;

/**
 * Implements <tt>BundleActivator</tt> for the update plug-in.
 * Similar function on macOS can be found in the sparkle bundle.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class UpdateWindowsActivator
    extends AbstractServiceDependentActivator
{
    private static final Logger logger
        = Logger.getLogger(UpdateWindowsActivator.class);

    /**
     * The name of the configuration property which indicates whether the
     * "checking for updates" menu entry is disabled.
     */
    private static final String CHECK_FOR_UPDATES_MENU_DISABLED_PROP
        = "net.java.sip.communicator.plugin.update.checkforupdatesmenu.DISABLED";

    /**
     * The name of the configuration property which indicates whether the client
     * should automatically check for updates each day or not.
     */
    private static final String CHECK_FOR_UPDATES_DAILY_ENABLED_PROP =
    "net.java.sip.communicator.plugin.update.checkforupdatesmenu.daily.ENABLED";

    /**
     * The name of the configuration property which indicates the hour that
     * the client should check for updates (if daily update checking is enabled)
     */
    private static final String CHECK_FOR_UPDATES_DAILY_TIME_PROP =
       "net.java.sip.communicator.plugin.update.checkforupdatesmenu.daily.HOUR";

    /**
     * The name of the configuration property which indicates the minimum version
     * of the application the user is allowed to run.
     */
    private static final String MIN_VERSION_PROP =
        "net.java.sip.communicator.MIN_VERSION";

    /**
     * Configuration property which indicates whether automatic update
     * checking has been disabled for this user. This property is part of the user
     * configuration and is set in SIP PS.
     */
    private static final String DISABLE_UPDATE_CHECKING_PROP =
        "net.java.sip.communicator.plugin.update.DISABLE_AUTO_UPDATE_CHECKING";

    /**
     * Reference to the <tt>BrowserLauncherService</tt>.
     */
    private static BrowserLauncherService browserLauncher;

    /**
     * The <tt>BundleContext</tt> in which the one and only
     * <tt>UpdateWindowsActivator</tt> instance of the update plug-in has been started.
     */
    static BundleContext bundleContext;

    /**
     * Reference to the <tt>ConfigurationService</tt>.
     */
    private static ConfigurationService configuration;

    /**
     * Reference to the <tt>UIService</tt>.
     */
    private static UIService uiService;

    /**
     * The update service.
     */
    private static UpdateServiceWindowsImpl updateService;

    /**
     * The version service
     */
    private static VersionService versionService;

    /**
     * The credentials service
     */
    private static CredentialsStorageService credsService;

    /**
     * The analytics service
     */
    private static AnalyticsService analyticsService;

    private static ThreadingService threadingService;

    /**
     * A scheduler to check for updates once a day
     */
    private ScheduledExecutorService mUpdateExecutor = null;

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncher == null)
        {
            browserLauncher
                = ServiceUtils.getService(
                        bundleContext,
                        BrowserLauncherService.class);
        }
        return browserLauncher;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    static ConfigurationService getConfiguration()
    {
        if (configuration == null)
        {
            configuration
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configuration;
    }

    /**
     * Gets a reference to a <code>ShutdownService</code> implementation
     * currently registered in the bundle context of the active
     * <code>UpdateCheckActivator</code> instance.
     * <p>
     * The returned reference to <code>ShutdownService</code> is not being
     * cached.
     * </p>
     *
     * @return reference to a <code>ShutdownService</code> implementation
     *         currently registered in the bundle context of the active
     *         <code>UpdateCheckActivator</code> instance
     */
    static ShutdownService getShutdownService()
    {
        return ServiceUtils.getService(bundleContext, ShutdownService.class);
    }

    /**
     * Returns a reference to the UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    static UIService getUIService()
    {
        if(uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }

    /**
     * The dependent service is available and the bundle will start.
     * @param dependentService the UIService this activator is waiting.
     */
    @Override
    public void start(Object dependentService)
    {
        logger.info("Update checker [STARTED]");

        ConfigurationService cfg = getConfiguration();

        updateService = new UpdateServiceWindowsImpl();

        bundleContext.registerService(
                UpdateService.class.getName(),
                updateService,
                null);

        // Register the "Check for Updates" menu item if
        // the "Check for Updates" property isn't disabled.
        if (!cfg.global().getBoolean(CHECK_FOR_UPDATES_MENU_DISABLED_PROP, false))
        {
            // Register the "Check for Updates" menu item.
            CheckForUpdatesMenuItemComponent
                    checkForUpdatesMenuItemComponent
                    = new CheckForUpdatesMenuItemComponent(
                    Container.CONTAINER_HELP_MENU);

            Hashtable<String, String> toolsMenuFilter
                    = new Hashtable<>();
            toolsMenuFilter.put(
                    Container.CONTAINER_ID,
                    Container.CONTAINER_HELP_MENU.getID());

            bundleContext.registerService(
                    PluginComponent.class.getName(),
                    checkForUpdatesMenuItemComponent,
                    toolsMenuFilter);
        }

        if (ConfigurationUtils.isUpdateCheckingDisabled())
        {
            getAnalyticsService().onEvent(AnalyticsEventType.AUTO_UPDATE_OFF);
        }
        else
        {
            getAnalyticsService().onEvent(AnalyticsEventType.AUTO_UPDATE_ON);
        }

        // We shouldn't ever need to check the MINIMUM_VERSION in config because we should be
        // including our current version on SIP-PS requests, and if it's too low then SIP-PS should
        // return a ClientOutOfDate error which causes us to force an upgrade - see
        // ProvisioningServiceImpl.java.  However, this code is left in as a back-stop because
        // we really want to avoid failing to force someone to update when they need to.
        boolean forcingUpdate = false;
        if (!ConfigurationUtils.isUpdateCheckingDisabled())
        {
            cfg.user().addPropertyChangeListener(MIN_VERSION_PROP,
                                                 new PropertyChangeListener()
                                                 {
                                                     public void propertyChange(PropertyChangeEvent evt)
                                                     {
                                                         logger.info("Minimum version has changed");
                                                         checkForceUpdate();
                                                     }
                                                 });

            forcingUpdate = checkForceUpdate();
        }
        else
        {
            logger.info("Forced updates disabled");
        }

        // Check for software update upon startup if enabled and we're not already forcing one.
        if (!ConfigurationUtils.isUpdateCheckingDisabled() &&
            !forcingUpdate)
        {
            logger.info("Checking for updates on startup");
            updateService.checkForUpdates(false);
        }
        else
        {
            logger.info("Not checking for updates on startup - " +
                "disabled? " + ConfigurationUtils.isUpdateCheckingDisabled() + " forcingUpdate? " + forcingUpdate);
        }

        // Schedule a "check for updates" task (if not disabled)
        if (!ConfigurationUtils.isUpdateCheckingDisabled()
            && cfg.global().getBoolean(CHECK_FOR_UPDATES_DAILY_ENABLED_PROP, false))
        {
            logger.info("Scheduled update checking enabled");
            scheduleUpdateCheck();
        }
        else
        {
            logger.info("Scheduled update checking disabled");
        }

        // If the disable automatic update checking setting changes while the
        // client is running, schedule or cancel the update check as appropriate
        cfg.user().addPropertyChangeListener(DISABLE_UPDATE_CHECKING_PROP,
             new PropertyChangeListener()
             {
                 public void propertyChange(PropertyChangeEvent evt)
                 {
                     logger.info(
                             "Disable automatic update checking property has changed");
                     if (evt.getNewValue() != null)
                     {
                         boolean updateDisabled = Boolean.parseBoolean((String) evt.getNewValue());
                         if (updateDisabled)
                         {
                             // Auto update checking is now disabled
                             logger.info("Scheduled update checking disabled");
                             cancelUpdateCheck();
                             getAnalyticsService().
                                     onEvent(AnalyticsEventType.AUTO_UPDATE_OFF);
                         }
                         else
                         {
                             // Auto update checking is now enabled
                             logger.info("Scheduled update checking enabled");
                             scheduleUpdateCheck();
                             getAnalyticsService().
                                     onEvent(AnalyticsEventType.AUTO_UPDATE_ON);
                         }
                     }
                     else
                     {
                         logger.warn(
                                 "Disable automatic update checking property has changed to a null value.");
                     }
                 }
             });

        logger.info("Update checker [REGISTERED]");
    }

    /**
     * Check to see if we need to force the user to update the client
     * Note that the forcing of the update happens on another thread.
     * @return true iff the client version is too low so an update is required.
     */
    private boolean checkForceUpdate()
    {
        boolean forcingUpdate = false;
        VersionService versionService = getVersionService();
        Version currentVersion = versionService.getCurrentVersion();

        String minimumVersionString = getConfiguration().user()
                            .getString("net.java.sip.communicator.MIN_VERSION");
        Version minimumVersion = minimumVersionString == null ? null :
                        versionService.parseVersionString(minimumVersionString);

        logger.info("Comparing current and minimum versions: " +
                                        currentVersion + ", " + minimumVersion);

        boolean isOutOfDate = minimumVersion != null &&
                              currentVersion.compareTo(minimumVersion) < 0;

        if (isOutOfDate && updateService != null)
        {
            logger.info("Client version too low - forcing update");
            updateService.forceUpdate();
            forcingUpdate = true;
        }

        return forcingUpdate;
    }

    /**
     * Schedule a "check for updates" task that will run once per day
     */
    private synchronized void scheduleUpdateCheck()
    {
        if (mUpdateExecutor != null)
        {
            logger.warn("Trying to schedule an update check, but one already exists. "
                + "Cancelling it before scheduling another.");
            cancelUpdateCheck();
        }

        logger.info("Scheduling an update check");
        int hoursToWait = calcHoursToWait();
        Runnable updateRunnable = new Runnable()
        {
            public void run()
            {
                logger.info("Performing scheduled update check");
                getUpdateService().checkForUpdates(false);
            }
        };

        mUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
        mUpdateExecutor.scheduleAtFixedRate(updateRunnable,
                                            hoursToWait,
                                            24,
                                            TimeUnit.HOURS);
    }

    /**
     * Cancel the existing scheduled update check
     */
    private synchronized void cancelUpdateCheck()
    {
        if (mUpdateExecutor != null)
        {
            logger.info("Shutting down UpdateExecutor");
            mUpdateExecutor.shutdown();
            mUpdateExecutor = null;
        }
    }

    /**
     * This activator depends on UIService.
     * @return the class name of uiService.
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * Setting context to the activator, as soon as we have one.
     *
     * @param context the context to set.
     */
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    /**
     * Calculate the number of hour to wait until the first scheduled update
     * check.  This will only be called if daily checking for config updates
     * is enabled
     *
     * @return The number of hours to wait
     */
    private int calcHoursToWait()
    {
        // The hours to wait is the number of hours until midnight tonight (24
        // minus the current hour) plus the hour that the config says updates
        // should be
        return 24 - Calendar.getInstance().get(Calendar.HOUR_OF_DAY) +
                     configuration.user().getInt(CHECK_FOR_UPDATES_DAILY_TIME_PROP, 0);
    }

    /**
     * Stop the bundle. Nothing to stop for now.
     * @param bundleContext <tt>BundleContext</tt> provided by OSGi framework
     */
    public void stop(BundleContext bundleContext)
    {
        logger.debug("Update checker [STOPPED]");

        if (mUpdateExecutor != null)
        {
            mUpdateExecutor.shutdown();
            mUpdateExecutor = null;
        }
    }

    /**
     * Returns the update service instance.
     *
     * @return the update service instance
     */
    static UpdateService getUpdateService()
    {
        return updateService;
    }

    static VersionService getVersionService()
    {
        if (versionService == null)
        {
            versionService = ServiceUtils.getService(bundleContext, VersionService.class);
        }

        return versionService;
    }

    /**
     * Returns the credential storage service instance.
     *
     * @return the credential storage service instance.
     */
    static CredentialsStorageService getCredsService()
    {
        if (credsService == null)
        {
            credsService = ServiceUtils.getService(bundleContext,
                                               CredentialsStorageService.class);
        }

        return credsService;
    }

    /**
     * Returns the analytics service instance
     */
    static AnalyticsService getAnalyticsService()
    {
        if (analyticsService == null)
        {
            analyticsService = ServiceUtils.getService(bundleContext,
                                                AnalyticsService.class);
        }

        return analyticsService;
    }

    static ThreadingService getThreadingService()
    {
        if (threadingService == null)
        {
            threadingService = ServiceUtils.getService(bundleContext,
                                                       ThreadingService.class);
        }

        return threadingService;
    }
}
