/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.msofficecomm;

import java.beans.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;

/**
 * Activator for the MS Office Communication handler
 *
 * Service to handle communication with Outlook.
 * Manages being the default IM/calling app for Outlook.
 *
 * Implements {@link BundleActivator} for the <tt>msofficecomm</tt> bundle.
 *
 * @author Lyubomir Marinov
 */
public class MsOfficeCommActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>MsOfficeCommActivator</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MsOfficeCommActivator.class);

    /**
     * OSGi bundle context.
     */
    private static BundleContext context;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The resources service.
     */
    private static ResourceManagementService resourcesService;

    /**
     * The analytics service.
     */
    private static AnalyticsService analyticsService;

    /**
     * Indicates whether we should be the default application for Outlook
     * integration (for calls, IM and presence).
     */
    private static final String DEFAULT_OUTLOOK_INTEGRATION_APP_PROP =
        "plugin.msofficecomm.DEFAULT_OUTLOOK_INTEGRATION_APP";

    /**
     * True if the MsOfficeComm bundle has started successfully
     */
    private boolean started;

    /**
     * Starts the <tt>msofficecomm</tt> bundle in a specific
     * {@link BundleContext}.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the
     * <tt>msofficecomm</tt> bundle is to be started
     * @throws Exception if anything goes wrong while starting the
     * <tt>msofficecomm</tt> bundle in the specified <tt>BundleContext</tt>
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        // The msofficecomm bundle is available on Windows only.
        if (!OSUtils.IS_WINDOWS)
            return;

        logger.info("MsOfficeComm plugin ... [STARTED]");

        context = bundleContext;

        if (ConfigurationUtils.isCallingOrImEnabled())
        {
            boolean isDefaultConfigurationOutlookIntegrationApp = false;

            // Make sure the Jitsi config option and the Windows registry entry
            // that control whether we are the default application for
            // integration with Outlook (for calls, IM and presence) are in sync.
            String isDefaultOutlookIntegrationAppString =
                getConfigurationService().user().getString(
                    DEFAULT_OUTLOOK_INTEGRATION_APP_PROP);

            logger.debug("Default Outlook integration app config = " +
                                              isDefaultOutlookIntegrationAppString);

            if(isDefaultOutlookIntegrationAppString == null)
            {
                logger.debug("Current user has NO config option to specify default Outlook app.");

                // There is no Jitsi config option to say whether we should be the
                // default Outlook integration app, so we must be logging-in with this
                // user for the 1st time.

                // Issue 597018/SFR 541572 workaround:
                // We used to always set the app as the default Outlook integration app if
                // the user is logging in for the first time, but this caused issues for
                // users who using Outlook integration with other apps (e.g. Skype for Business).
                // As a workaround, now we don't set MaX UC as default app if we're
                // logging in for the first time with this user. User can still set MaX UC
                // as the default Outlook integration app from the settings menu.
                logger.info("Workaround for issue 597018: don't set Accession as default Outlook app.");
                getConfigurationService().user().setProperty(
                    DEFAULT_OUTLOOK_INTEGRATION_APP_PROP,
                    false);
            }
            else
            {
                logger.debug("Current user already has Outlook default config");

                // It's not the first time this user has logged in, since they have config set.
                // If the default IM provider is set to None or Accession in their registry,
                // we should overwrite it with the user config
                // However, if another app is set as the default IM provider in the registry
                // (probably because the user has chosen to change it), we should not overwrite it
                // and instead set the Accession configuration accordingly

                isDefaultConfigurationOutlookIntegrationApp =
                    Boolean.parseBoolean(isDefaultOutlookIntegrationAppString);

                String defaultRegistryOutlookApp =
                        DefaultOutlookIntegrationApp.getDefaultOutlookApp();

                if (defaultRegistryOutlookApp.equals(getApplicationName()) ^
                        isDefaultConfigurationOutlookIntegrationApp)
                {
                    // Conflict between registry and configuration settings
                    if (
                            (defaultRegistryOutlookApp.equals("") ||
                            defaultRegistryOutlookApp.equals("None"))
                            && isDefaultConfigurationOutlookIntegrationApp)
                    {
                        // Set to Accession in registry
                        setAsDefaultOutlookIntegrationApp();
                    }
                    else if (defaultRegistryOutlookApp.equals(getApplicationName()))
                    {
                        // Unset default app in registry
                        unsetAsDefaultOutlookIntegrationApp();
                    }
                    else
                    {
                        // Default app is set to a non-Accession app in registry, but user config
                        // thinks Accession is the default.
                        // Tell user config we are not the default Outlook integration app
                        getConfigurationService().user().setProperty(
                                DEFAULT_OUTLOOK_INTEGRATION_APP_PROP,
                                false);
                        logger.info("We are not the default IM app");
                    }
                }
            }

            // Add a listener so if the default outlook integration app config
            // changes we can update the registry entry accordingly.
            getConfigurationService().user().addPropertyChangeListener(
                DEFAULT_OUTLOOK_INTEGRATION_APP_PROP,new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent e)
                {
                    boolean newValue = (Boolean) e.getNewValue();
                    logger.debug(DEFAULT_OUTLOOK_INTEGRATION_APP_PROP +
                                                " has been changed to " + newValue);

                    try
                    {
                        if (newValue)
                        {
                            setAsDefaultOutlookIntegrationApp();
                            startProcesses(bundleContext);
                        }
                        else
                        {
                            stopProcesses(bundleContext);
                            unsetAsDefaultOutlookIntegrationApp();
                        }
                    }
                    catch(Exception ex)
                    {
                        logger.warn("Error changing messenger state", ex);
                    }
                }
            });

            // If we are the default IM app, start the integration process.
            if (DefaultOutlookIntegrationApp.isDefaultIMApp())
            {
                logger.debug("We are now the default IM app");
                startProcesses(bundleContext);
            }
        }
        else
        {
            logger.debug("User does not support calling or IM - unsetting "
                + "Accession as the default Outlook integration app.");

            getConfigurationService().user().setProperty(
                DEFAULT_OUTLOOK_INTEGRATION_APP_PROP,
                false);
            unsetAsDefaultOutlookIntegrationApp();
        }
    }

    /**
     * Start the Messenger and Outlook COM processes.
     *
     * @param bundleContext
     * @throws Exception
     */
    private boolean startProcesses(BundleContext bundleContext) throws Exception
    {
        boolean stopMessenger = false;

        if (!OSUtils.IS_WINDOWS || started)
            return false;

        try
        {
            logger.info("Starting processes...");
            Messenger.start(bundleContext);

            started = true;
            stopMessenger = true;
            int hresult = OutOfProcessServer.start(
                                  getApplicationName(),
                                  DefaultOutlookIntegrationApp.isLegacyMode());
            logger.info("MsOfficeComm started OutOfProcessServer HRESULT:"
                + hresult);

            if (hresult < 0)
                logger.error("HResult returned negative value: " + hresult);
            else
                stopMessenger = false;
        }
        catch (Error e)
        {
            // Most likely caused by a failure to start the messenger.  Just log
            logger.warn("Unable to start messenger ", e);
        }
        finally
        {
            if (stopMessenger)
            {
                Messenger.stop(bundleContext);
            }
        }
        return(!stopMessenger);
    }

    /**
     * Stop the Messenger and Outlook COM processes.
     *
     * @param bundleContext
     * @throws Exception
     */
    private void stopProcesses(BundleContext bundleContext) throws Exception
    {
        if (!OSUtils.IS_WINDOWS || !started)
            return;

        // Stop the process that provides Outlook Presence.
        started = false;
        try
        {
            logger.info("Stopping processes...");

            int hresult = OutOfProcessServer.stop();
            logger.info("MsOfficeComm stopped OutOfProcessServer HRESULT:" +
                        hresult);

            if (hresult < 0)
            {
                logger.error("Unable to stop process server: " + hresult);
            }
        }
        finally
        {
            Messenger.stop(bundleContext);
        }

        logger.info("MsOfficeComm plugin ... [UNREGISTERED]");
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            configService =
                ServiceUtils.getService(context, ConfigurationService.class);
        }

        return configService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            resourcesService =
                ServiceUtils.getService(context, ResourceManagementService.class);
        }

        return resourcesService;
    }

    /**
     * Returns the <tt>AnalyticsService</tt>.
     *
     * @return the <tt>AnalyticsService</tt>.
     */
    public static AnalyticsService getAnalyticsService()
    {
        if (analyticsService == null)
        {
            analyticsService =
                ServiceUtils.getService(context, AnalyticsService.class);
        }

        return analyticsService;
    }

    /**
     * Sets us as the default application for Outlook integration (for calls,
     * IM and presence).
     */
    public static void setAsDefaultOutlookIntegrationApp()
    {
        if (OSUtils.IS_WINDOWS)
        {
            logger.debug("Set us as the default Outlook integration app.");
            DefaultOutlookIntegrationApp.setApplicationAsDefaultApp();
        }
    }

    /**
     * Unsets us as the default application for Outlook integration (for calls,
     * IM and presence).
     */
    public static void unsetAsDefaultOutlookIntegrationApp()
    {
        if (OSUtils.IS_WINDOWS)
        {
            logger.debug("UNset us as the default Outlook integration app.");
            DefaultOutlookIntegrationApp.unsetDefaultApp();
        }
    }

    /**
     * Stops the <tt>msofficecomm</tt> bundle in a specific
     * {@link BundleContext}.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the
     * <tt>msofficecomm</tt> bundle is to be stopped
     * @throws Exception if anything goes wrong while stopping the
     * <tt>msofficecomm</tt> bundle in the specified <tt>BundleContext</tt>
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        logger.info("stop");

        // The msofficecomm bundle is available on Windows only.
        if (!OSUtils.IS_WINDOWS || !started)
            return;

        stopProcesses(bundleContext);
    }

    /**
     * Returns the application name.
     * @return the application name
     */
    private static String getApplicationName()
    {
        return MsOfficeCommActivator.getResources().getSettingsString(
            "service.gui.APPLICATION_NAME");
    }
}
