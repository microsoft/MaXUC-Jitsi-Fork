/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.browserlauncher;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the browserlauncher bundle.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class BrowserLauncherActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>BrowserLauncherActivator</tt> class and its instances for logging
     * output.
     */
    private static final Logger logger
        = Logger.getLogger(BrowserLauncherActivator.class);

    /**
     * Initialize and start the service.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     */
    public void start(BundleContext bundleContext)
    {
        //Create the browser launcher service
        BrowserLauncherService browserLauncher = new BrowserLauncherImpl();

        logger.info("Browser Launcher Service STARTED");

        bundleContext
            .registerService(
                BrowserLauncherService.class.getName(),
                browserLauncher,
                null);

        logger.info("Browser Launcher Service REGISTERED");
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     */
    public void stop(BundleContext bundleContext)
    {
    }
}
