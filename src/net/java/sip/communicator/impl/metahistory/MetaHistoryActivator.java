/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.metahistory;

import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activates the MetaHistoryService
 *
 * @author Damian Minkov
 */
public class MetaHistoryActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>MetaHistoryActivator</tt> class and its instances for logging output.
     */
    private static Logger sLog =
        Logger.getLogger(MetaHistoryActivator.class);

    /**
     * The <tt>MetaHistoryService</tt> reference.
     */
    private MetaHistoryServiceImpl metaHistoryService = null;

    /**
     * Initialize and start meta history
     *
     * @param bundleContext BundleContext
     */
    public void start(BundleContext bundleContext)
    {
        try
        {
            sLog.entry();

            //Create and start the meta history service.
            metaHistoryService =
                new MetaHistoryServiceImpl();

            metaHistoryService.start(bundleContext);

            bundleContext.registerService(
                MetaHistoryService.class.getName(), metaHistoryService, null);

            sLog.info("Meta History Service ...[REGISTERED]");
        }
        finally
        {
            sLog.exit();
        }
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     */
    public void stop(BundleContext bundleContext)
    {
        if(metaHistoryService != null)
            metaHistoryService.stop(bundleContext);
    }
}
