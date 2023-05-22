/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.resources;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.*;

/**
 * Starts Resource Management Service.
 * @author Damian Minkov
 */
public class ResourceManagementActivator
    implements BundleActivator
{
    private Logger logger =
        Logger.getLogger(ResourceManagementActivator.class);

    static BundleContext bundleContext;

    private ResourceManagementServiceImpl resPackImpl = null;

    private static ConfigurationService configService;

    private static ImageLoaderService imageLoaderService;

    /**
     * Starts this bundle.
     *
     * @param bc the osgi bundle context
     */
    public void start(BundleContext bc)
    {
        bundleContext = bc;

        resPackImpl =
            new ResourceManagementServiceImpl();

        bundleContext.registerService(
                ResourceManagementService.class.getName(),
                resPackImpl,
                null);

        logger.info("Resource manager ... [REGISTERED]");
    }

    /**
     * Stops this bundle.
     *
     * @param bc the osgi bundle context
     */
    public void stop(BundleContext bc)
    {
        bc.removeServiceListener(resPackImpl);

        configService = null;
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
     * Returns the <tt>ImageLoaderService</tt> obtained from the bundle context.
     *
     * @return the <tt>ImageLoaderService</tt> obtained from the bundle context
     */
    public static ImageLoaderService getImageLoaderService()
    {
        if(imageLoaderService == null)
        {
            imageLoaderService = ServiceUtils.getService(bundleContext,
                ImageLoaderService.class);
        }

        return imageLoaderService;
    }
}
