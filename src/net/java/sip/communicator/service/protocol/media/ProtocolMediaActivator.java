/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.media;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.insights.InsightService;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.util.*;

/**
 * The activator doesn't really start anything as this service is mostly
 * stateless, it's simply here to allow us to obtain references to the services
 * that we may need.
 *
 * @author Emil Ivov
 */
public class ProtocolMediaActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>ProtocolMediaActivator</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(ProtocolMediaActivator.class.getName());
    /**
     * Bundle context from OSGi.
     */
    static BundleContext bundleContext = null;

    /**
     * Configuration service.
     */
    private static ConfigurationService configurationService = null;

    /**
     * Media service.
     */
    private static MediaService mediaService = null;

    /**
     * A reference to the currently valid {@link NetworkAddressManagerService}.
     */
    private static NetworkAddressManagerService
                                        networkAddressManagerService = null;

    /**
     * A reference to the analytics service
     */
    private static AnalyticsService analyticsService = null;

    private static InsightService insightService;

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context)
    {
        logger.debug("Started.");

        ProtocolMediaActivator.bundleContext = context;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
        configurationService = null;
        mediaService = null;
        networkAddressManagerService = null;
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
        if(configurationService == null)
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
     * Returns a reference to a NetworkAddressManagerService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * NetworkAddressManagerService .
     */
    public static NetworkAddressManagerService getNetworkAddressManagerService()
    {
        if(networkAddressManagerService == null)
        {
            ServiceReference<?> confReference
                = bundleContext.getServiceReference(
                    NetworkAddressManagerService.class.getName());
            networkAddressManagerService = (NetworkAddressManagerService)
                bundleContext.getService(confReference);
        }
        return networkAddressManagerService;
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     *
     * @return a reference to the BundleContext instance that we were started
     * with.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Returns a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static MediaService getMediaService()
    {
        if(mediaService == null)
        {
            ServiceReference<?> mediaServiceReference
                = bundleContext.getServiceReference(
                    MediaService.class.getName());
            mediaService = (MediaService)bundleContext
                .getService(mediaServiceReference);
        }
        return mediaService;
    }

    /**
     * Returns a reference to the analytics service
     *
     * @return a reference to the analytics service
     */
    public static AnalyticsService getAnalyticsService()
    {
        if (analyticsService == null)
        {
            ServiceReference<?> analyticsServiceReference
                = bundleContext.getServiceReference(
                    AnalyticsService.class.getName());
            analyticsService = (AnalyticsService)bundleContext
                .getService(analyticsServiceReference);
        }

        return analyticsService;
    }

    public static InsightService getInsightService()
    {
        if (insightService == null)
        {
            insightService = ServiceUtils.getService(bundleContext, InsightService.class);
        }
        return insightService;
    }
}
