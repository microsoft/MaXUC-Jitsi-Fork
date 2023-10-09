/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.packetlogging.PacketLoggingService;

/**
 * The activator to manage the bundles between OSGi framework and the
 * Network address manager
 *
 * @author Emil Ivov
 */
public class NetaddrActivator
    implements BundleActivator
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(NetworkAddressManagerServiceImpl.class);

    /**
     * The OSGi bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * The network address manager implementation.
     */
    private NetworkAddressManagerServiceImpl networkAMS = null;

    /**
     * The configuration service.
     */
    private static ConfigurationService configurationService = null;

    /**
     * The CommPortal service.
     */
    private static CommPortalService commPortalService = null;

    /**
     * The OSGi <tt>PacketLoggingService</tt> in
     * {@link #bundleContext} and used for debugging.
     */
    private static PacketLoggingService packetLoggingService  = null;

    /**
     * The threading service
     */
    private static ThreadingService threadingService = null;

    /**
     * Creates a NetworkAddressManager, starts it, and registers it as a
     * NetworkAddressManagerService.
     *
     * @param bundleContext  OSGI bundle context
     */
    public void start(BundleContext bundleContext)
    {
        try{

            logger.entry();

            //in here we load static properties that should be else where
            //System.setProperty("java.net.preferIPv4Stack", "false");
            //System.setProperty("java.net.preferIPv6Addresses", "true");
            //end ugly property set

            //keep a reference to the bundle context for later usage.
            NetaddrActivator.bundleContext = bundleContext;

            //Create and start the network address manager.
            networkAMS =
                new NetworkAddressManagerServiceImpl();

            // give references to the NetworkAddressManager implementation
            networkAMS.start();

            logger.info("Network Address Manager         ...[  STARTED ]");

            bundleContext.registerService(
                NetworkAddressManagerService.class.getName(), networkAMS, null);

            logger.info("Network Address Manager Service ...[REGISTERED]");
        }
        finally
        {
            logger.exit();
        }
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
                = (ConfigurationService) bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a CommPortalService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the CommPortalService.
     */
    public static CommPortalService getCommPortalService()
    {
        if (commPortalService == null)
        {
            commPortalService = ServiceUtils.getService(
                    bundleContext,
                    CommPortalService.class);

            if (commPortalService == null)
            {
                logger.debug("CommPortalService is null - this is expected only early-on during start-up.");
            }
        }

        return commPortalService;
    }

    /**
     * @return the <tt>ThreadingService</tt> obtained from the bundle context
     */
    public static ThreadingService getThreadingService()
    {
        if (threadingService == null)
        {
            threadingService =
                    ServiceUtils.getService(bundleContext, ThreadingService.class);
        }
        return threadingService;
    }

    /**
     * Stops the Network Address Manager bundle
     *
     * @param bundleContext  the OSGI bundle context
     *
     */
    public void stop(BundleContext bundleContext)
    {
        if(networkAMS != null)
            networkAMS.stop();
        logger.info("Network Address Manager Service ...[STOPPED]");

        configurationService = null;
        commPortalService = null;
        packetLoggingService = null;
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     *
     * @return a reference to the BundleContext instance that we were started
     * with.
     */
    static BundleContext getBundleContext()
    {
        return bundleContext;
    }
}
