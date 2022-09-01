/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.shutdowntimeout;

import java.io.*;
import java.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.util.*;

/**
 * In order to shut down SIP Communicator we kill the Felix system bundle.
 * However, this sometimes doesn't work for reason of running non-daemon
 * threads (such as the javasound event dispatcher). This results in having
 * instances of SIP Communicator running in the background.
 *
 * We use this shutdown timeout bundle in order to fix this problem. When our
 * stop method is called, we assume that a shutdown is executed and start a 10
 * seconds daemon thread. If the application is still running once these 10
 * seconds expire, we System.exit() the application.
 *
 * @author Emil Ivov
 */
public class ShutdownTimeout
    implements BundleActivator
{
    private static final Logger sLog
        = Logger.getLogger(ShutdownTimeout.class);

    /**
     * The system property which can be used to set custom timeout.
     */
    public static final String PROPERTY_SHUTDOWN_TIMEOUT =
        "net.java.sip.communicator.impl.shutdowntimeout.TIMEOUT";

    /**
     * The number of milliseconds that we wait before we force a shutdown.
     */
    public static final long SHUTDOWN_TIMEOUT_DEFAULT = 10000;//ms

    /**
     * The system property which can be used to set the exit code.
     */
    public static final String PROPERTY_EXIT_CODE =
        "net.java.sip.communicator.impl.shutdowntimeout.EXIT_CODE";

    /**
     * The code that we exit with if the application is not down in 10 seconds.
     * This code is set to 0 by default so that the user doesn't see an error.
     * Overridden with 500 in the beta, dev and internal resource packs to
     * generate an error window if this problem occurs in these versions.
     */
    public static final int SYSTEM_EXIT_CODE_DEFAULT = 0;

    /**
     * An implementation of the thread dump service
     */
    private ThreadDumpService mThreadDumper;

    /**
     * The location of the shutdown hang marker file.
     */
    private String mShutdownHangMarker;

    /**
     * A reference to the analytics service
     */
    private static AnalyticsService sAnalyticsService;

    /**
     * A reference to the bundle context
     */
    private static BundleContext sContext;

    /**
     * A reference to the config service
     */
    private static ConfigurationService sConfigService;

    /**
     * A reference to the diagnostics service
     */
    private static DiagnosticsService sDiagnosticsService;

    /**
     * The shutdown timeout
     */
    private long mShutDownTimeout;

    /**
     * The exit code generated on shutdown
     */
    private int mSystemExitCode;

    /**
     * A shutdown hook that is registered when we start.  It's only used as a
     * back-up in case the stop method is not called.
     */
    private final Thread mShutdownHook = new Thread("Shutdown Timeout Hook")
    {
        @Override
        public void run()
        {
            sLog.info("Creating shutdown thread");

            Thread shutdownTimeoutThread = new Thread("Shutdown Timeout")
            {
                public void run()
                {
                    synchronized(this)
                    {
                        try
                        {
                            sLog.info("Starting shutdown countdown of "
                                                    + mShutDownTimeout + "ms.");

                            wait(mShutDownTimeout);

                            sLog.error("Failed to gently shutdown. Forcing exit.");

                            recordShutdownFailure();
                        }
                        catch (InterruptedException ex)
                        {
                            sLog.debug("Interrupted shutdown timer.", ex);
                        }
                        finally
                        {
                            Runtime.getRuntime().halt(mSystemExitCode);
                        }
                    }
                }
            };

            sLog.trace("Created the shutdown timer thread: " +
                    shutdownTimeoutThread);

            shutdownTimeoutThread.setDaemon(true);
            shutdownTimeoutThread.start();
        }
    };

    /**
     * Impl of the bundle activator start method.
     *
     * @param context
     */
    public void start(BundleContext context)
    {
        sLog.debug("Starting the ShutdownTimeout service.");

        sContext = context;

        // Add a shutdown hook to detect client hangs
        Runtime.getRuntime().addShutdownHook(mShutdownHook);

        sConfigService = ServiceUtils
            .getService(context, ConfigurationService.class);

        mShutdownHangMarker =
            sConfigService.global().getScHomeDirLocation() + File.separator +
            sConfigService.global().getScHomeDirName() + File.separator +
            "shutdownHangMarker.txt";

        mThreadDumper = ServiceUtils.getService(sContext, ThreadDumpService.class);

        // Check for a custom value for the shutdown timeout and exit code
        mShutDownTimeout = sConfigService.global().getLong(PROPERTY_SHUTDOWN_TIMEOUT,
                                                           SHUTDOWN_TIMEOUT_DEFAULT);
        mSystemExitCode = sConfigService.global().getInt(PROPERTY_EXIT_CODE,
                                                         SYSTEM_EXIT_CODE_DEFAULT);

        checkForShutdownHang();
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
        sLog.info("Stop called on ShutdownTimeout");
    }

    /**
     * Check for a previous shutdown hang.
     *
     * Shutdown hangs are marked by a file written by the
     * <tt>ShutdownTimeout</tt> bundle.
     */
    private void checkForShutdownHang()
    {
        File shutdownHangMarker = new File(mShutdownHangMarker);

        if (! shutdownHangMarker.exists())
        {
            return;
        }

        // Get the time from the last error report
        String time = "";
        FileReader fr = null;
        BufferedReader br = null;
        try
        {
            fr = new FileReader(shutdownHangMarker);
            br = new BufferedReader(fr);
            time = br.readLine();
        }
        catch (IOException ex)
        {
            sLog.error("Error reading shutdown marker", ex);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }

                if (fr != null)
                {
                    fr.close();
                }
            }
            catch (IOException e)
            {
                sLog.error("Error closing shutdown file");
            }
        }

        // The last run of the program hung while shutting down, prepare an
        // error report.
        sLog.error("=== Last shutdown hung - " + time + " ===");

        // Send an analytics event and a report.  Don't delete the hang file
        // here, as the error report might not have been created yet.
        getAnalyticsService().onEvent(AnalyticsEventType.CLIENT_HUNG);

        // Ask the diagnostics service to show an error report frame to give
        // the user the option of reporting the hang if we are running in QA mode.
        getDiagnosticsService().openErrorReportFrame(ReportReason.HANG_ON_SHUTDOWN);

        if (shutdownHangMarker.delete())
        {
            sLog.info("Removed shutdown marker");
        }
        else
        {
            sLog.error("Failed to remove shutdown marker");
        }
    }

    /**
     * Record the fact that we failed to shutdown within the timeout
     *
     * This consists of a shutdown hang marker and a thread dump.
     */
    private void recordShutdownFailure()
    {
        try
        {
            PrintWriter out = new PrintWriter(mShutdownHangMarker);
            out.println(new Date());
            out.close();
        }
        catch (Exception ex)
        {
            sLog.error("Failed to write shutdown marker", ex);
        }

        if (mThreadDumper != null)
        {
            try
            {
                mThreadDumper.dumpThreads();
            }
            catch (Throwable t)
            {
                sLog.error("Failed to obtain thread dump", t);
            }
        }
        else
        {
            sLog.error("Unable to obtain thread dump");
        }
    }

    /**
     * @return a reference to the analytics service
     */
    static AnalyticsService getAnalyticsService()
    {
        if (sAnalyticsService == null)
        {
            sAnalyticsService =
                      ServiceUtils.getService(sContext, AnalyticsService.class);
        }

        return sAnalyticsService;
    }

    /**
     * @return a reference to the diagnostics service
     */
    static DiagnosticsService getDiagnosticsService()
    {
        if (sDiagnosticsService == null)
        {
            sDiagnosticsService =
                      ServiceUtils.getService(sContext, DiagnosticsService.class);
        }

        return sDiagnosticsService;
    }
}
