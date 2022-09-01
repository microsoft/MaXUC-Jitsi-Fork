/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.swingnotification;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

/**
 * Activator for the swing notification service.
 * @author Symphorien Wanko
 */
public class SwingNotificationActivator implements BundleActivator
{
    /**
     * The bundle context in which we started
     */
    public static BundleContext bundleContext;

    /**
     * A reference to the configuration service.
     */
    private static ConfigurationService configService;

    /**
     * Logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(SwingNotificationActivator.class);

    /**
     * A reference to the resource management service.
     */
    private static ResourceManagementService resourcesService;

    /**
     * A reference to the global status service
     */
    private static GlobalStatusService statusService;

    /**
     * Start the swing notification service
     * @param bc
     */
    public void start(BundleContext bc)
    {
        logger.info("Swing Notification ...[  STARTING ]");

        bundleContext = bc;

        PopupMessageHandler handler = null;
        handler = new PopupMessageHandlerSwingImpl();

        getConfigurationService();

        bc.registerService(
                PopupMessageHandler.class.getName()
                , handler
                , null);

        logger.info("Swing Notification ...[REGISTERED]");
    }

    public void stop(BundleContext arg0)
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
        if(configService == null) {
            ServiceReference<?> configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the bundle
     * context.
     * @return the <tt>ResourceManagementService</tt> obtained from the bundle
     * context
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }

    /**
     * @return the <tt>GlobalStatusService</tt> from the bundle context
     */
    public static GlobalStatusService getGlobalStatusService()
    {
        if (statusService == null)
            statusService = ServiceUtils
                          .getService(bundleContext, GlobalStatusService.class);

        return statusService;
    }
}
