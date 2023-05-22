/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.sparkle;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URLEncoder;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.resources.ResourceManagementServiceUtils;
import net.java.sip.communicator.service.update.UpdateService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ProcessLogger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.version.VersionService;

/**
 * Activates the Sparkle Framework
 *
 * @author Romain Kuntz
 */
public class SparkleActivator
        implements BundleActivator
{
    private static final Logger logger = Logger.getLogger(SparkleActivator.class);

    private static final ProcessLogger processLogger = ProcessLogger.getLogger();

    /**
     * Name of the encrypted password in the configuration service.
     */
    private static final String PROPERTY_ENCRYPTED_PASSWORD
            = "net.java.sip.communicator.plugin.provisioning.auth";

    /**
     * Name of the username in the configuration service.
     */
    private static final String PROPERTY_USERNAME
            = "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";

    /**
     * Configuration property which indicates whether automatic update checking
     * has been disabled for this user. This property is part of the user
     * configuration and is set in SIP PS.
     */
    private static final String DISABLE_AUTO_UPDATE_CHECKING_FOR_USER_PROP =
            "net.java.sip.communicator.plugin.update.DISABLE_AUTO_UPDATE_CHECKING";

    /**
     * A reference to the ConfigurationService implementation instance that is
     * currently registered with the bundle context.
     */
    private static ConfigurationService configurationService = null;

    /**
     * The credentials service
     */
    private static CredentialsStorageService credsService;

    /**
     * The analytics service
     */
    private static AnalyticsService analyticsService;

    /**
     * The version service.
     */
    private static VersionService versionService = null;

    /**
     * The version service.
     */
    private static UpdateService updateService = null;

    /**
     * The current BundleContext.
     */
    private static BundleContext bundleContext = null;

    /**
     * Initialize Sparkle
     *
     * @param pathToSparkleFramework the path to the Sparkle framework
     * @param updateAtStartup        specifies whether Sparkle should be
     *                               checking for updates on startup.
     * @param checkInterval          specifies an interval for the update
     *                               checks.
     * @param downloadLink           a custom download link for sparkle (i.e.
     *                               the SUFeedURL). If null the default URL
     *                               will be chosen (the SUFeedURL parameter in
     *                               the .app/Contents/Info.pList).
     * @param menuItemTitle          localized string to be used for the menu
     *                               item title in macosx specific menu.
     * @param userAgentString        specifies a user-agent string for Sparkle
     *                               update checks.
     * @param isAppBelowMinVersion   whether the user is running an outdated version
     *                               of MaX UC.
     */
    public static native int initSparkle(String pathToSparkleFramework,
                                          boolean updateAtStartup,
                                          int checkInterval,
                                          String downloadLink,
                                          String menuItemTitle,
                                          String userAgentString,
                                          boolean isAppBelowMinVersion);

    /**
     * Set Sparkle Download Link
     *
     * @param downloadLink a custom download link for sparkle (i.e. the
     *                     SUFeedURL).
     */
    public static native void setDownloadLink(String downloadLink);

    /**
     * Trigger Sparkle to perform an update check and report back to the user
     * (through its own UI)
     */
    public static native void checkForUpdates();

    /**
     * Enable Sparkle automatic update checks (if disabled) and set the check
     * interval
     */
    public static native void setNewUpdateCheckInterval(int checkInterval);

    /**
     * Disable Sparkle automatic update checks (if enabled)
     */
    public static native void cancelScheduledUpdateChecks();

    /**
     * Tells Sparkle whether the client is below the minimum supported version
     */
    public static native void setIsAppBelowMinVersion(boolean isAppBelowMinVersion);

    /**
     * Whether updates are checked at startup
     */
    private boolean updateAtStartup = true;

    /**
     * Check interval period, in seconds The minimum value Sparkle will use is
     * 3600 A value of 0 will disable scheduled update checking
     */
    private final int CHECK_INTERVAL = 86400;  // 1 day

    /**
     * Internal flag that we use in order to determine whether the native
     * Sparkle libs have already been loaded.
     */
    private static boolean sparkleLibLoaded = false;

    /**
     * Property name for the update link in the configuration file.
     */
    private static final String PROP_UPDATE_LINK =
            "net.java.sip.communicator.UPDATE_LINK";

    /**
     * The name of the configuration property which indicates the minimum version
     * of the application the user is allowed to run.
     */
    private static final String MIN_VERSION_PROP =
            "net.java.sip.communicator.MIN_VERSION";

    /**
     * The native Mac command used to delete a value from the preferences plist file.
     */
    private static final String DELETE_COMMAND = "defaults delete";

    /**
     * The name of the Sparkle update URL property in the preferences plist file
     * we wish to set.
     */
    private static final String FEED_URL_NAME = "SUFeedURL";

    /**
     * Initialize and start Sparkle
     *
     * @param bundleContext BundleContext
     */
    public void start(BundleContext bundleContext)
    {
        logger.info("Mac update checker [STARTED]");

        SparkleActivator.bundleContext = bundleContext;

        /*
          Dynamically loads JNI object. Will fail if non-MacOSX
          or when libinit_sparkle.dylib is outside of the LD_LIBRARY_PATH
         */
        try
        {
            if (!SparkleActivator.sparkleLibLoaded)
            {
                System.loadLibrary("sparkle_init");
                SparkleActivator.sparkleLibLoaded = true;
            }
        }
        catch (Throwable t)
        {
            logger.warn("Couldn't load sparkle library.");
            logger.debug("Couldn't load sparkle library.", t);

            return;
        }

        String title = ResourceManagementServiceUtils.getService(bundleContext)
                .getI18NString("plugin.updatechecker.UPDATE_MENU_ENTRY");

        // add common suffix of this menu title
        if (title != null)
            title += "...";

        PropertyChangeListener listener = evt -> {
            logger.debug("Config changed: " + evt);
            updateLink();
        };

        ConfigurationService cfg = getConfigurationService();
        int checkInterval = CHECK_INTERVAL;

        // Synchronized so that we don't attempt to update the link at the same
        // time as initializing Sparkle.
        synchronized (SparkleActivator.class)
        {
            // If the link, username or password changes, update the link.
            cfg.user()
                    .addPropertyChangeListener(PROP_UPDATE_LINK, listener);
            cfg.global()
                    .addPropertyChangeListener(PROPERTY_USERNAME, listener);
            cfg.user()
                    .addPropertyChangeListener(PROPERTY_ENCRYPTED_PASSWORD,
                                               listener);
            cfg.user()
                    .addPropertyChangeListener(MIN_VERSION_PROP,
                                               SparkleActivator::onMinVersionUpdate);

            if (ConfigurationUtils.isAutoUpdateCheckingDisabledForUser() ||
                ConfigurationUtils.isAutoUpdateCheckingDisabledGlobally())
            {
                logger.info("Automatic update checking disabled");
                updateAtStartup = false;
                checkInterval = 0; // Periodic checking disabled
                getAnalyticsService().
                        onEvent(AnalyticsEventType.AUTO_UPDATE_OFF);
            }
            else
            {
                logger.info("Automatic update checking enabled");
                getAnalyticsService().
                        onEvent(AnalyticsEventType.AUTO_UPDATE_ON);
            }

            final String downloadLink = getUpdateLink();

            final String userAgent = "Accession Communicator"
                    + "/"
                    + System.getProperty("sip-communicator.version");

            // Logging only what we get from config as the rest of the information
            // has PII in it.
            final String updateLinkToLog = getConfigurationService().user()
                .getString(PROP_UPDATE_LINK);

            final boolean isAppBelowMinVersion = getVersionService().isOutOfDate();

            logger.info("Init sparkle with user agent string: " + userAgent
                        + " updateAtStartup: " + updateAtStartup
                        + " checkInterval: " + checkInterval
                        + " updateLink: " + updateLinkToLog
                        + " menu option title: " + title
                        + " isAppBelowMinVersion: " + isAppBelowMinVersion);

            // Sparkle used to write its feedURL into a user preferences file
            // We no longer use the URL from this file (as we provide the correct
            // URL to Sparkle dynamically using a delegate method in our JNI code)
            // If a feedURL is still hanging around in this file from an old
            // client version, it might contain sensitive user information,
            // so we delete it just in case (see SFR 536630).
            String identifier = System.getProperty(
                    "net.java.sip.communicator.BUNDLE_IDENTIFIER");
            try
            {
                logger.debug("Got bundle identifier " + identifier +
                             " to clear Sparkle update feed URL in " +
                             "~/Library/Preferences/" + identifier);
                String command = DELETE_COMMAND + " " + identifier + " " + FEED_URL_NAME;
                processLogger.traceExec(command);
                Runtime.getRuntime().exec(command);
            }
            catch (IOException e)
            {
                logger.debug("Caught exception " + e.getMessage() +
                             " while trying to clear " + FEED_URL_NAME +
                             " in ~/Library/Preferences/" + identifier);
            }

            // TODO: better way to get the Sparkle Framework path?
            initSparkle(System.getProperty("user.dir")
                                + "/../../Frameworks/Sparkle.framework",
                        updateAtStartup,
                        checkInterval,
                        downloadLink,
                        title,
                        userAgent,
                        isAppBelowMinVersion);
        }

        // If the disable automatic update checking setting changes while the
        // client is running, schedule or cancel the update check as appropriate
        cfg.user().addPropertyChangeListener(
                DISABLE_AUTO_UPDATE_CHECKING_FOR_USER_PROP,
                evt ->
                {
                    logger.info(
                            "Disable automatic update checking property has changed");
                    if (evt.getNewValue() != null)
                    {
                        boolean updateDisabled = Boolean.parseBoolean((String) evt.getNewValue());
                        if (updateDisabled)
                        {
                            logger.info("Scheduled update checking disabled for this user");
                            cancelScheduledUpdateChecks();
                            getAnalyticsService().
                                    onEvent(AnalyticsEventType.AUTO_UPDATE_OFF);
                        }
                        else if (!ConfigurationUtils.isAutoUpdateCheckingDisabledGlobally())
                        {
                            logger.info("Scheduled update checking enabled for this user");
                            setNewUpdateCheckInterval(CHECK_INTERVAL);
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
                });

        // Create and register service to expose Sparkle's checkForUpdates method to the rest of the app.
        UpdateService updateService = new UpdateServiceMacImpl();
        bundleContext.registerService(
                UpdateService.class.getName(),
                updateService,
                null);

        logger.info("Mac update checker ...[REGISTERED]");
    }

    /**
     * Update the downloaded updates link
     */
    private static void updateLink()
    {
        // Synchronized so that we don't attempt to update the link at the same
        // time as initializing Sparkle, or update the link twice at the same
        // time.
        synchronized (SparkleActivator.class)
        {
            logger.debug("Updating the download link");
            setDownloadLink(getUpdateLink());
        }
    }

    /**
     * Get the update link from the configuration service and process it,
     * replacing the username and password parameters with the correct values.
     * @return the processed update link
     */
    private static String getUpdateLink()
    {
        String updateLink = getConfigurationService().user()
                .getString(PROP_UPDATE_LINK);

        if (updateLink.contains("${password}"))
        {
            CredentialsStorageService creds = getCredsService();
            String password = creds.user().loadPassword(
                    PROPERTY_ENCRYPTED_PASSWORD);

            if (password != null)
            {
                logger.debug("Replacing password in update link");

                password = URLEncoder.encode(password);
                updateLink = updateLink.replace("${password}", password);
            }
        }

        if (updateLink.contains("${directorynumber}"))
        {
            ConfigurationService cfg = getConfigurationService();
            String username = cfg.global().getString(PROPERTY_USERNAME);

            if (username != null)
            {
                logger.debug("Replacing directory number in update link");
                updateLink = updateLink.replace("${directorynumber}", username);
            }
        }

        return updateLink;
    }

    /**
     * Called when the minimum supported version is changed. Updates Sparkle
     * JNI with the version status of the app (i.e. whether it is out-of-date).
     */
    private static void onMinVersionUpdate(PropertyChangeEvent evt)
    {
        logger.debug("Minimum allowed version changed. Update Sparkle JNI");
        final boolean isOutOfDate = versionService.isOutOfDate();
        setIsAppBelowMinVersion(isOutOfDate);

        // Ignore this update if automatic update checking is disabled.
        if (isOutOfDate &&
            !(ConfigurationUtils.isAutoUpdateCheckingDisabledForUser() ||
              ConfigurationUtils.isAutoUpdateCheckingDisabledGlobally()))
        {
            logger.debug("Client is now out-of-date. Triggering update check.");

            // We need to pass in true here, even though the update isn't user
            // triggered, otherwise this request won't get passed to Sparkle.
            getUpdateService().checkForUpdates(true);
        }
    }

    /**
     * Stops this bundle
     *
     * @param bundleContext a reference to the currently valid
     *                      <tt>BundleContext</tt>
     */
    public void stop(BundleContext bundleContext)
    {
        SparkleActivator.bundleContext = null;
        logger.info("Sparkle Plugin ...[Stopped]");
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
                    = (ConfigurationService) bundleContext.getService(
                    confReference);
        }
        return configurationService;
    }

    /**
     * Returns the credential storage service instance.
     *
     * @return the credential storage service instance.
     */
    private static CredentialsStorageService getCredsService()
    {
        if (credsService == null)
        {
            credsService = ServiceUtils.getService(bundleContext,
                                                   CredentialsStorageService.class);
        }

        return credsService;
    }

    static VersionService getVersionService()
    {
        if (versionService == null)
        {
            ServiceReference<?> versionReference
                    = bundleContext.getServiceReference(
                    VersionService.class.getName());
            versionService = (VersionService) bundleContext.getService(versionReference);
        }

        return versionService;
    }

    static UpdateService getUpdateService()
    {
        if (updateService == null)
        {
            ServiceReference<?> versionReference
                    = bundleContext.getServiceReference(
                    UpdateService.class.getName());
            updateService = (UpdateService) bundleContext.getService(versionReference);
        }

        return updateService;
    }
    /**
     * Returns the analytics service instance
     */
    public static AnalyticsService getAnalyticsService()
    {
        if (analyticsService == null)
        {
            analyticsService = ServiceUtils.getService(bundleContext,
                                                       AnalyticsService.class);
        }

        return analyticsService;
    }
}
