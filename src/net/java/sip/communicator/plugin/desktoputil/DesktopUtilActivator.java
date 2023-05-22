/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.websocketserver.WebSocketApiCrmService;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.plugin.desktoputil.lookandfeel.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.diagnostics.DiagnosticsService;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.threading.*;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.ServiceUtils.*;

public class DesktopUtilActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>SwingUtilActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(DesktopUtilActivator.class);

    private static ConfigurationService configurationService;

    private static ResourceManagementService resourceService;

    private static KeybindingsService keybindingsService;

    private static BrowserLauncherService browserLauncherService;

    private static PhoneNumberUtilsService phoneNumberUtils;

    private static AnalyticsService analyticsService;

    private static ConferenceService conferenceService;

    /**
     * The notification service.
     */
    private static NotificationService sNotificationService;

    private static UIService uiService;

    private static ThreadingService threadingService;

    private static ImageLoaderService imageLoaderService;

    private static DiagnosticsService diagnosticsService;

    private static WebSocketApiCrmService webSocketApiCrmService;

    static BundleContext bundleContext;

    private static WISPAService sWISPAService;

    /**
     * Calls <tt>Thread.setUncaughtExceptionHandler()</tt>
     *
     * @param context The execution context of the bundle being started
     * (unused).
     */
    public void start(BundleContext context)
    {
        bundleContext = context;

        // Set up the look and feel for the application, once the resource
        // management service has loaded. We use resources for some of the look
        // and feel properties. The ResourceManagementService loads after
        // DesktopUtils as it relies on DesktopUtils
        ServiceUtils.getService(
            bundleContext,
            ResourceManagementService.class,
            new ServiceCallback<ResourceManagementService>()
            {
                @Override
                public void onServiceRegistered(ResourceManagementService service)
                {
                    logger.debug("Resource Management Service ready," +
                                                       "setting look and feel");

                    // Firstly, set the application scaling so that all UI
                    // components use the correct display scale
                    ScaleUtils.setScale();

                    // Setup the Look & Feel after the scale has been set as it
                    // uses the scale to set font sizes
                    LookAndFeelManager.setLookAndFeel();
                }
            });
    }

    /**
     * Doesn't do anything.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
    }

    /**
     * Returns the <tt>ConfigurationService</tt> currently registered.
     *
     * @return the <tt>ConfigurationService</tt>
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
    }

    /**
     * Returns the service giving access to all application resources.
     *
     * @return the service giving access to all application resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resourceService;
    }

    /**
     * Returns the image corresponding to the given <tt>imageID</tt>.
     *
     * @param imageID the identifier of the image
     * @return the image corresponding to the given <tt>imageID</tt>
     */
    public static BufferedImageFuture getImage(String imageID)
    {
        return getResources().getBufferedImage(imageID);
    }

    /**
     * Returns the <tt>KeybindingsService</tt> currently registered.
     *
     * @return the <tt>KeybindingsService</tt>
     */
    public static KeybindingsService getKeybindingsService()
    {
        if (keybindingsService == null)
        {
            keybindingsService
                = ServiceUtils.getService(
                        bundleContext,
                        KeybindingsService.class);
        }
        return keybindingsService;
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            browserLauncherService
                = ServiceUtils.getService(
                        bundleContext,
                        BrowserLauncherService.class);
        }
        return browserLauncherService;
    }

    public static WISPAService getWISPAService()
    {
        if (sWISPAService == null)
        {
            sWISPAService = ServiceUtils.getService(bundleContext,
                                                    WISPAService.class);
        }

        return sWISPAService;
    }

    /**
     * Gets the <tt>UIService</tt> instance registered in the
     * <tt>BundleContext</tt> of the <tt>UtilActivator</tt>.
     *
     * @return the <tt>UIService</tt> instance registered in the
     * <tt>BundleContext</tt> of the <tt>UtilActivator</tt>
     */
    public static UIService getUIService()
    {
        if (uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }

    /**
     * Returns the <tt>PhoneNumberUtilsService</tt> obtained from the bundle
     * context.
     * @return the <tt>PhoneNumberUtilsService</tt> obtained from the bundle
     * context
     */
    public static PhoneNumberUtilsService getPhoneNumberUtils()
    {
        if (phoneNumberUtils == null)
            phoneNumberUtils = ServiceUtils.getService(
                bundleContext, PhoneNumberUtilsService.class);
        return phoneNumberUtils;
    }

    /**
     * Returns the <tt>AnalyticsService</tt> obtained from the bundle context.
     *
     * @return the <tt>AnalyticsService</tt> obtained from the bundle context
     */
    public static AnalyticsService getAnalyticsService()
    {
        if (analyticsService == null)
            analyticsService = ServiceUtils.getService(
                bundleContext, AnalyticsService.class);
        return analyticsService;
    }

    /**
     * Returns the <tt>ConferenceService</tt> obtained from the bundle context.
     *
     * @return the <tt>ConferenceService</tt> obtained from the bundle context
     */
    public static ConferenceService getConferenceService()
    {
        if (conferenceService == null)
            conferenceService = ServiceUtils.getService(
                bundleContext, ConferenceService.class);

        return conferenceService;
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        if(sNotificationService == null)
        {
             sNotificationService = ServiceUtils.getService(
                    bundleContext, NotificationService.class);
        }

        return sNotificationService;
    }

    /**
     * @return a reference to the registered threading service
     */
    public static ThreadingService getThreadingService()
    {
        if (threadingService == null)
            threadingService = ServiceUtils.getService(bundleContext,
                                                       ThreadingService.class);

        return threadingService;
    }

    /**
     * @return a reference to the registered image loader service
     */
    public static ImageLoaderService getImageLoaderService()
    {
        if (imageLoaderService == null)
            imageLoaderService = ServiceUtils.getService(bundleContext,
                                                      ImageLoaderService.class);

        return imageLoaderService;
    }

    /**
     * @return a reference to the registered diagnostics service
     */
    public static DiagnosticsService getDiagnosticsService()
    {
        if (diagnosticsService == null)
        {
            diagnosticsService =
                ServiceUtils.getService(bundleContext, DiagnosticsService.class);
        }

        return diagnosticsService;
    }

    /**
     * @return a reference to the registered WebSocket API CRM service
     */
    public static WebSocketApiCrmService getWebSocketApiCrmService()
    {
        if (webSocketApiCrmService == null)
        {
            webSocketApiCrmService = ServiceUtils.getService(
                    bundleContext,
                    WebSocketApiCrmService.class);
        }
        return webSocketApiCrmService;
    }
}
