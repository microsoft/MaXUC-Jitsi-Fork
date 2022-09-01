// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.imageloader;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.threading.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activator for the image loader service
 */
public class ImageLoaderActivator implements BundleActivator
{
    private static final Logger logger = Logger.getLogger(ImageLoaderActivator.class);

    /**
     * OSGi bundle context.
     */
    private static BundleContext sBundleContext;

    /**
     * The resource management service
     */
    private static ResourceManagementService sResourcesService;

    /**
     * The threading service
     */
    private static ThreadingService sThreadingService;

    /**
     * The UI Service
     */
    private static UIService uiService;

    @Override
    public void start(BundleContext context)
    {
        logger.info("Image Loader Service ...[STARTED]");
        sBundleContext = context;

        // Registers an implementation of the image loader service.
        sBundleContext.registerService(ImageLoaderService.class.getName(),
                                       new ImageLoaderServiceImpl(),
                                       null);
    }

    @Override
    public void stop(BundleContext bundleContext)
    {
        logger.info("Image Loader Service ...[STOPPED]");
    }

    /**
     * Returns the <tt>ThreadingService</tt>, through which we will
     * schedule tasks.
     *
     * @return the <tt>ThreadingService</tt>, through which we will
     * schedule tasks.
     */
    public static ThreadingService getThreadingService()
    {
        if (sThreadingService == null)
        {
            sThreadingService = ServiceUtils.getService(sBundleContext,
                ThreadingService.class);
        }
        return sThreadingService;
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
        if (sResourcesService == null)
        {
            sResourcesService
                = ServiceUtils.getService(
                    sBundleContext,
                        ResourceManagementService.class);
        }
        return sResourcesService;
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            uiService = ServiceUtils.getService(sBundleContext,
                                                UIService.class);
        }
        return uiService;
    }
}
