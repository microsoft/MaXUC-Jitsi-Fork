/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.updatewindows;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.osgi.framework.BundleContext;

import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.browserlauncher.BrowserLauncherService;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.threading.CancellableRunnable;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.service.update.UpdateService;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.AbstractServiceDependentActivator;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.version.VersionService;

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
    private static final String DISABLE_AUTO_UPDATE_CHECKING_FOR_USER_PROP =
        "net.java.sip.communicator.plugin.update.DISABLE_AUTO_UPDATE_CHECKING";

    /**
     * Name of property to force update.
     */
    private static final String FORCE_UPDATE = "net.java.sip.communicator.plugin.update.FORCE_UPDATE";

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

    private static WISPAService wispaService;

    private static CancellableRunnable updateCheckRunnable;
    private static final long ONE_MINUTE_IN_MS = 60*1000L;
    private static final long ONE_DAY_IN_MS = 24*60*ONE_MINUTE_IN_MS;

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

        if (ConfigurationUtils.isAutoUpdateCheckingDisabledForUser() ||
            ConfigurationUtils.isAutoUpdateCheckingDisabledGlobally())
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
        cfg.user().addPropertyChangeListener(
                MIN_VERSION_PROP,
                 new PropertyChangeListener()
                 {
                     public void propertyChange(PropertyChangeEvent evt)
                     {
                         logger.info("Minimum version has changed");
                         boolean isOutOfDate = checkForceUpdate();

                         if (isOutOfDate)
                         {
                             logger.debug("Client is now out-of-date. Triggering update check.");
                             getUpdateService().checkForUpdates(false);
                         }
                     }
                 });

        // If the disable automatic update checking setting changes while the
        // client is running, schedule or cancel the update check as appropriate
        cfg.user().addPropertyChangeListener(DISABLE_AUTO_UPDATE_CHECKING_FOR_USER_PROP,
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
                             logger.info("Scheduled update checking disabled for this user");
                             cancelUpdateCheck();
                             getAnalyticsService().
                                     onEvent(AnalyticsEventType.AUTO_UPDATE_OFF);
                         }
                         else if (!ConfigurationUtils.isAutoUpdateCheckingDisabledGlobally())
                         {
                             // Auto update checking is now enabled
                             logger.info("Scheduled update checking enabled for this user");
                             scheduleUpdateCheck();
                             getAnalyticsService().
                                     onEvent(AnalyticsEventType.AUTO_UPDATE_ON);
                         }
                         else
                         {
                             logger.info("Scheduled update checking disabled globally");
                         }
                     }
                     else
                     {
                         logger.warn(
                                 "Disable automatic update checking property has changed to a null value.");
                     }
                 }
             });

        // Schedule a "check for updates" task (if not disabled)
        if (!ConfigurationUtils.isAutoUpdateCheckingDisabledForUser() &&
            !ConfigurationUtils.isAutoUpdateCheckingDisabledGlobally())
        {
            logger.info("Scheduled update checking enabled");
            scheduleUpdateCheck();
        }
        else
        {
            logger.info("Scheduled update checking disabled");
        }

        logger.info("Update checker [REGISTERED]");
    }

    /**
     * Check to see if we need to force the user to update the client
     * Note that the forcing of the update happens on another thread.
     * Whenever we call this function and we are forcing update, we also
     * send electron a motion to show the force update UI
     * @return true iff the client version is too low so an update is required.
     */
    static boolean checkForceUpdate()
    {
        boolean forcingUpdate = false;
        VersionService versionService = getVersionService();
        ConfigurationService cfg = getConfiguration();

        boolean isOutOfDate = versionService.isOutOfDate();

        if (isOutOfDate && updateService != null)
        {
            forcingUpdate = true;
        }

        logger.debug("Need to force update? " + forcingUpdate);
        if (forcingUpdate)
        {
            logger.debug("Version too low - forcing update on Windows");
            cfg.user().setProperty(FORCE_UPDATE, true);
        }
        else
        {
            logger.debug("Version is fine - removing FORCE_UPDATE property " +
                         "if it exists from a previously out-of-date client");
            cfg.user().removeProperty(FORCE_UPDATE);
        }

        return forcingUpdate;
    }

    /**
     * Schedule a "check for updates" task that will run once per day
     */
    private synchronized void scheduleUpdateCheck()
    {
        if (updateCheckRunnable != null && !updateCheckRunnable.isCancelled())
        {
            logger.warn("Trying to schedule an update check, but one already exists. "
                + "Cancelling it before scheduling another.");
            cancelUpdateCheck();
        }

        logger.info("Scheduling an update check");
        updateCheckRunnable = new CancellableRunnable()
        {
            public void run()
            {
                logger.info("Performing scheduled update check");
                getUpdateService().checkForUpdates(false);
            }
        };

        // If the client cannot contact the server for a mandatory update, the user is likely to
        // get stuck in a blocked state without an update option. We want to make sure they receive the
        // update as soon as the client regains connection to the server. If the user already triggered the
        // update download, sending another update prompt won't disrupt their update.
        boolean forceUpdate = checkForceUpdate();
        if (forceUpdate)
        {
            logger.info("Scheduled an update check every minute for mandatory update");
            getThreadingService().scheduleAtFixedRate("Force update check",
                                                      updateCheckRunnable,
                                                      0L,
                                                      ONE_MINUTE_IN_MS);
        }
        else
        {
            logger.info("Scheduled an update check once every 24 hours");
            getThreadingService().scheduleAtFixedRate("Update check",
                                                      updateCheckRunnable,
                                                      ONE_DAY_IN_MS,
                                                      ONE_DAY_IN_MS);
        }
    }

    /**
     * Cancel the existing scheduled update check
     */
    private synchronized void cancelUpdateCheck()
    {
        logger.info("Cancelling update check");
        if (updateCheckRunnable != null)
        {
            updateCheckRunnable.cancel();
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
     * Stop the bundle, cancelling any scheduled update check.
     * @param bundleContext <tt>BundleContext</tt> provided by OSGi framework
     */
    public void stop(BundleContext bundleContext)
    {
        logger.debug("Update checker [STOPPED]");
        cancelUpdateCheck();
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

    /**
     * Returns the WISPA service instance
     *
     * @return the WISPA service instance
     */
    static WISPAService getWISPAService()
    {
        if (wispaService == null)
        {
            wispaService = ServiceUtils.getService(bundleContext,
                                                   WISPAService.class);
        }
        return wispaService;
    }
}
