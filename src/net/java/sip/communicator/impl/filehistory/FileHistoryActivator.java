/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.filehistory;

import org.osgi.framework.*;

import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.util.*;

/**
 *
 * @author Damian Minkov
 */
public class FileHistoryActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>FileHistoryActivator</tt> class and its instances for logging output.
     */
    private static Logger sLog = Logger.getLogger(FileHistoryActivator.class);

    /**
     * A <tt>FileHistoryService</tt> service reference.
     */
    private FileHistoryServiceImpl fileHistoryService = null;

    /** The <tt>BundleContext</tt> of the service. */
    static BundleContext sBundleContext;

    /** The <tt>MetaContactListService</tt> reference. */
    private static MetaContactListService metaContactListService;

    /**
     * Initialize and start file history
     *
     * @param bundleContext BundleContext
     */
    public void start(BundleContext bundleContext)
    {
        sBundleContext = bundleContext;
        try
        {
            sLog.entry();

            ServiceReference<?> refDatabase = sBundleContext.getServiceReference(
                DatabaseService.class.getName());

            DatabaseService databaseService = (DatabaseService)
            sBundleContext.getService(refDatabase);

            // Create and start the file history service.
            fileHistoryService = new FileHistoryServiceImpl(databaseService);
            fileHistoryService.start(sBundleContext);

            sBundleContext.registerService(
                FileHistoryService.class.getName(), fileHistoryService, null);

            sLog.info("File History Service ...[REGISTERED]");
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
        if (fileHistoryService != null)
        {
            fileHistoryService.stop(bundleContext);
        }
    }

    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     */
    public static MetaContactListService getContactListService()
    {
        if (metaContactListService == null)
        {
            metaContactListService =
                ServiceUtils.getService(sBundleContext,
                                        MetaContactListService.class);
        }

        return metaContactListService;
    }
}
