/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.notificationwiring;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>NotificationActivator</tt> is the activator of the notification
 * bundle.
 *
 * @author Yana Stamcheva
 */
public class NotificationWiringActivator
    implements BundleActivator
{
    private final Logger logger =
        Logger.getLogger(NotificationWiringActivator.class);

    protected static BundleContext bundleContext;

    private static NotificationService notificationService;
    private static ResourceManagementService resourcesService;
    private static ConfigurationService configService;
    private static UIService uiService = null;
    private static MediaService mediaService;
    private static MetaContactListService contactListService;
    private static AnalyticsService sAnalyticsService;

    public void start(BundleContext bc)
    {
        bundleContext = bc;
        try
        {
            logger.entry();
            logger.info("Notification wiring plugin...[  STARTED ]");

            // Get the notification service implementation
            ServiceReference<?> notifReference = bundleContext
                .getServiceReference(NotificationService.class.getName());

            notificationService = (NotificationService) bundleContext
                .getService(notifReference);

            new NotificationManager().init();

            logger.info("Notification wiring plugin ...[REGISTERED]");
        }
        finally
        {
            logger.exit();
        }
    }

    public void stop(BundleContext bc)
    {
        logger.info("Notification handler Service ...[STOPPED]");
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
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
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }

    /**
     * Returns the <tt>ConfigurationService</tt>.
     *
     * @return the <tt>ConfigurationService</tt>.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null)
        {
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configService;
    }

    /**
     * Returns the current implementation of the <tt>UIService</tt>.
     * @return the current implementation of the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        }

        return uiService;
    }

    public static AnalyticsService getAnalyticsService()
    {
        if (sAnalyticsService == null)
        {
            sAnalyticsService =
                    ServiceUtils.getService(bundleContext, AnalyticsService.class);
        }

        return sAnalyticsService;
    }

    /**
     * Returns an instance of the <tt>MediaService</tt> obtained from the
     * bundle context.
     * @return an instance of the <tt>MediaService</tt> obtained from the
     * bundle context
     */
    public static MediaService getMediaService()
    {
        if (mediaService == null)
        {
            mediaService
                = ServiceUtils.getService(bundleContext, MediaService.class);
        }
        return mediaService;
    }

    /**
     * Returns a reference to a MetaContactListService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the MetaContactListService.
     */
    public static MetaContactListService getContactListService()
    {
        if (contactListService == null)
        {
            contactListService
                = ServiceUtils.getService(
                        bundleContext,
                        MetaContactListService.class);
        }
        return contactListService;
    }

    /**
     * @return a reference to the conference service
     */
    public static ConferenceService getConfService()
    {
        return ServiceUtils.getConferenceService(bundleContext);
    }
}
