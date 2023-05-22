/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.osdependent;

import java.beans.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.notification.NotificationAction;
import net.java.sip.communicator.service.notification.UINotificationListener;
import net.java.sip.communicator.impl.osdependent.jdic.*;
import net.java.sip.communicator.impl.osdependent.macosx.*;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.NotificationHandler;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.notification.UINotificationHandler;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;

/**
 * Registers the <tt>Systray</tt> in the UI Service.
 *
 * @author Nicolas Chamouard
 * @author Lubomir Marinov
 */
public class OsDependentActivator
    implements BundleActivator,
        UINotificationListener
{
    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    public static UIService uiService;

    public static NotificationService notificationService;

    private static ConfigurationService configService;

    private static ClassOfServiceService classOfServiceService;

    private static ResourceManagementService resourcesService;

    private static GlobalStatusService globalStatusService;

    private static String applicationName;

    private static AccountManager accountManager;

    /**
     * The <tt>Logger</tt> used by the <tt>OsDependentActivator</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OsDependentActivator.class);

    /**
     * The Systray Service Impl started by this bundle
     */
    private SystrayServiceJdicImpl systrayService = null;

    /**
     * Called when this bundle is started.
     *
     * @param bc The execution context of the bundle being started.
     */
    public void start(BundleContext bc)
    {
        bundleContext = bc;
        logger.entry();

        try
        {
            // Adds a MacOSX specific dock icon listener in order to show main
            // contact list window on dock icon click.
            if (OSUtils.IS_MAC)
                MacOSXDockIcon.addDockIconListener();

            // Create the notification service implementation
            systrayService = new SystrayServiceJdicImpl();
            logger.info("Systray Service...[  STARTED ]");

            bundleContext.registerService(
                SystrayService.class.getName(),
                systrayService,
                null);

            logger.info("Systray Service ...[REGISTERED]");

            // Get the Global Status Service
            getGlobalStatusService();

            // Register for UI notifications from the NotificationService once
            // the relevant Handler has been added. We do not explicitly remove
            // these listeners as they are only added once and should persist
            // until the client shuts down.
            getNotificationService().addHandlerAddedListener(
                    new NotificationService.HandlerAddedListener() {
                        @Override
                        public void onHandlerAdded(NotificationHandler handler)
                        {
                            if (handler instanceof UINotificationHandler)
                            {
                                ((UINotificationHandler) handler)
                                        .addNotificationListener(OsDependentActivator.this);
                                logger.debug("Registered as UINotificationListener");
                            }
                        }
                    }
            );

            for (NotificationHandler handler :
                    getNotificationService().getActionHandlers(
                                    NotificationAction.ACTION_DISPLAY_UI_NOTIFICATIONS))
            {
                ((UINotificationHandler) handler)
                        .addNotificationListener(OsDependentActivator.this);
            }
        }
        finally
        {
            logger.exit();
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bc The execution context of the bundle being stopped.
     */
    public void stop(BundleContext bc)
    {
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null) {
            ServiceReference<?> configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }

    /**
     * Returns the <tt>ClassOfServiceService</tt> obtained from the bundle
     * context.
     * @return the <tt>ClassOfServiceService</tt> obtained from the bundle
     * context
     */
    public static ClassOfServiceService getClassOfServiceService()
    {
        if (classOfServiceService == null) {
            ServiceReference<?> cosReference = bundleContext
                .getServiceReference(ClassOfServiceService.class.getName());

            if (cosReference != null)
            {
                classOfServiceService = (ClassOfServiceService) bundleContext
                    .getService(cosReference);
            }
        }

        return classOfServiceService;
    }

    /**
     * Gets a reference to a <code>ShutdownService</code> implementation
     * currently registered in the bundle context of the active
     * <code>OsDependentActivator</code> instance.
     * <p>
     * The returned reference to <code>ShutdownService</code> is not being
     * cached.
     * </p>
     *
     * @return reference to a <code>ShutdownService</code> implementation
     *         currently registered in the bundle context of the active
     *         <code>OsDependentActivator</code> instance
     */
    public static ShutdownService getShutdownService()
    {
        return
            (ShutdownService)
                bundleContext.getService(
                    bundleContext.getServiceReference(
                        ShutdownService.class.getName()));
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle
     * context.
     * @return the <tt>UIService</tt> obtained from the bundle
     * context
     */
    public static UIService getUIService()
    {
        if(uiService == null)
        {
            ServiceReference<?> serviceRef = bundleContext
                .getServiceReference(UIService.class.getName());

            if (serviceRef != null)
                uiService = (UIService) bundleContext.getService(serviceRef);
        }

        return uiService;
    }

    public static NotificationService getNotificationService()
    {
        if(notificationService == null)
        {
            ServiceReference<?> serviceRef = bundleContext
                    .getServiceReference(NotificationService.class.getName());

            if (serviceRef != null)
                notificationService = (NotificationService) bundleContext.getService(serviceRef);
        }

        return notificationService;
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
            resourcesService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }

    /**
     * Returns the <tt>GlobalStatusService</tt> obtained from the bundle
     * context.
     * @return the <tt>GlobalStatusService</tt> obtained from the bundle
     * context
     */
    public static GlobalStatusService getGlobalStatusService()
    {
        if (globalStatusService == null)
        {
            globalStatusService
                = ServiceUtils.getService(
                bundleContext,
                GlobalStatusService.class);
        }
        return globalStatusService;
    }

    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(accountManager == null)
        {
            accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);
        }
        return accountManager;
    }

    /**
     * Returns this application's name
     *
     * @return this application's name
     */
    public static String getApplicationName()
    {
        if (applicationName == null)
        {
            if (resourcesService == null)
            {
                getResources();
            }

            applicationName = resourcesService.getSettingsString(
                "service.gui.APPLICATION_NAME");
        }

        return applicationName;
    }

    /**
     * Updates the taskbar overlay based on the number of outstanding user
     * notifications
     * @param callNotifications number of missed call notifications
     * @param chatNotifications number of unread IM notifications (max one per chat)
     * @param messageWaitingNotifications number of message waiting (usually voicemail) notifications
     */
    public void updateUI(int callNotifications,
                         int chatNotifications,
                         int messageWaitingNotifications)
    {
        systrayService.updateTaskbarOverlay(
                callNotifications + chatNotifications + messageWaitingNotifications);
    }

    public void providerStatusMessageChanged(PropertyChangeEvent evt)
    {
        // Nothing to do
    }
}
