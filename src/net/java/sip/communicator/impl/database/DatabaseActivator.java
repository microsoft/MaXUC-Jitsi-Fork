// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.database;

import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.util.*;

/**
 * Activates the DatabaseService.  Also does:
 * - One-off migration from the obsolete HistoryService (XML) into the database.
 * - Per-release upgrade whenever the database format changes.
 */
public class DatabaseActivator implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by this <tt>DatabaseActivator</tt> instance for
     * logging output.
     */
    private static final Logger sLog = Logger.getLogger(DatabaseActivator.class);

    /**
     * The <tt>ServiceRegistration</tt> reference.
     */
    private ServiceRegistration<?> mDatabaseServiceRegistration;

    /**
     * The <tt>DatabaseService</tt> reference.
     */
    private DatabaseServiceImpl mDatabaseService;

    /**
     * Bundle context from OSGi.
     */
    private static BundleContext mBundleContext;

    /**
     * Create the DatabaseService and make it available for other components to
     * use.
     *
     * @param bundleContext The <tt>BundleContext</tt>
     */
    public void start(BundleContext bundleContext)
    {
        mBundleContext = bundleContext;

        try
        {
            sLog.info("Starting database service");

            FileAccessService fileAccessService =
                ServiceUtils.getService(bundleContext,
                    FileAccessService.class);

            mDatabaseService = new DatabaseServiceImpl(fileAccessService);
            mDatabaseServiceRegistration =
                bundleContext.registerService(DatabaseService.class.getName(),
                                              mDatabaseService,
                                              null);
        }
        catch (Exception e)
        {
            sLog.error("Failed to start database service:", e);
        }
        finally
        {
            sLog.exit();
        }
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext The <tt>BundleContext</tt>.
     * @throws Exception if the stop operation goes wrong.
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        sLog.info("Stopping database service");

        if (mDatabaseServiceRegistration != null)
        {
            mDatabaseServiceRegistration.unregister();
            mDatabaseServiceRegistration = null;
        }

        if (mDatabaseService != null)
        {
            mDatabaseService.shutdown();
            mDatabaseService = null;
        }
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * with.
     */
    public static BundleContext getBundleContext()
    {
        return mBundleContext;
    }
}
