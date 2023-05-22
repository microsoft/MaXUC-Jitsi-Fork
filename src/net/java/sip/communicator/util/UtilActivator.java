/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

//import java.awt.image.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.service.browserlauncher.BrowserLauncherService;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.diagnostics.DiagnosticsService;
import net.java.sip.communicator.service.gui.AlertUIService;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.resources.ResourceManagementServiceUtils;
import net.java.sip.communicator.service.shutdown.ShutdownService;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.fileaccess.FileAccessService;
import org.jitsi.service.neomedia.MediaConfigurationService;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;

/**
 * The only raison d'etre for this Activator is so that it would set a global
 * exception handler. It doesn't export any services and neither it runs any
 * initialization - all it does is call
 * <tt>Thread.setUncaughtExceptionHandler()</tt>
 *
 * @author Emil Ivov
 */
public class UtilActivator
    implements BundleActivator,
               Thread.UncaughtExceptionHandler
{
    /**
     * The <tt>Logger</tt> used by the <tt>UtilActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(UtilActivator.class);

    private static ConfigurationService configurationService;

    private static ResourceManagementService resourceService;

    private static UIService uiService;

    private static FileAccessService fileAccessService;

    private static MediaService mediaService;

    public static BundleContext bundleContext;

    private static AccountManager accountManager;

    private static AlertUIService alertUIService;

    private static DiagnosticsService diagnosticsService;

    private static ConferenceService conferenceService;

    private static BrowserLauncherService browserService;

    private static MetaContactListService metaCListService;

    private static WISPAService wispaService;

    private static ShutdownService shutdownService;

    private static CommPortalService commPortalService;

    private static PrintStream originalStdOut;

    /**
     * Calls <tt>Thread.setUncaughtExceptionHandler()</tt>
     *
     * @param context The execution context of the bundle being started
     * (unused).
     */
    public void start(BundleContext context)
    {
        logger.trace("Setting default uncaught exception handler.");

        bundleContext = context;

        Thread.setDefaultUncaughtExceptionHandler(this);

        // The mac application doesn't pick up the locale properly from the
        // environment so we need a workaround.
        if (OSUtils.IS_MAC)
        {
            new MacLocaleFixer().fixLocales();
        }

        try
        {
            originalStdOut = new PrintStream(System.out);
            System.setErr(new Interceptor(System.err, true));
            System.setOut(new Interceptor(System.out, false));
        }
        catch (SecurityException e)
        {
            logger.error("Unable to redirect output stream", e);
        }
    }

    /**
     * A simple class which intercepts lines from a print stream and writes them
     * to the logs.  This allows us to intercept System.out.println statements
     * and equivalent that OSGI makes and which would otherwise be lost
     */
    private static class Interceptor extends PrintStream
    {
        private final boolean mIsError;

        public Interceptor(OutputStream out, boolean isError)
        {
            super(out, true);
            mIsError = isError;
        }

        @Override
        public void print(String s)
        {
            originalStdOut.println(s);
            if (mIsError)
                logger.error("System error: " + s);
            else
                logger.info("System out: " + s);
        }
    }

    /**
     * Method invoked when a thread would terminate due to the given uncaught
     * exception. All we do here is simply log the exception using the system
     * logger.
     *
     * <p>Any exception thrown by this method will be ignored by the
     * Java Virtual Machine and thus won't mess up our application.
     *
     * @param thread the thread
     * @param exc the exception
     */
    public void uncaughtException(Thread thread, Throwable exc)
    {
        logger.error("An uncaught exception occurred in thread="
                     + thread
                     + " and message was: "
                     + exc.getMessage()
                     , exc);
    }

    /**
     * Doesn't do anything.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
    }

    /**
     * @return the <tt>ConfigurationService</tt> currently registered.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
    }

    /**
     * @return the service giving access to all application resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resourceService;
    }

    /**
     * @return the <tt>UIService</tt> instance registered in the
     * <tt>BundleContext</tt> of the <tt>UtilActivator</tt>
     */
    public static UIService getUIService()
    {
        if (uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }

    /**
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }

    /**
     * @return an instance of the <tt>MediaService</tt> obtained from the
     * bundle context
     */
    public static MediaService getMediaService()
    {
        if (mediaService == null)
        {
            mediaService
                = ServiceUtils.getService(bundleContext, MediaService.class);
        }
        return mediaService;
    }

    /**
     * @return the <tt>UIService</tt> instance registered in the
     * <tt>BundleContext</tt> of the <tt>UtilActivator</tt>
     */
    public static MediaConfigurationService getMediaConfiguration()
    {
        return ServiceUtils.getService(bundleContext,
                MediaConfigurationService.class);
    }

    /**
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     *         context
     */
    public static Map<Object, ProtocolProviderFactory>
        getProtocolProviderFactories()
    {
        Map<Object, ProtocolProviderFactory> providerFactoriesMap
            = new Hashtable<>();

        ServiceReference<?>[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException | IllegalStateException e)
        {
            logger.error("Failed to get all registered provider factories: " + e);
        }

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(serRef);

                providerFactoriesMap.put(
                        serRef.getProperty(ProtocolProviderFactory.PROTOCOL),
                        providerFactory);
            }
        }
        return providerFactoriesMap;
    }

    /**
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(accountManager == null)
        {
            accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);
        }
        return accountManager;
    }

    /**
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     * context
     */
    public static AlertUIService getAlertUIService()
    {
        if (alertUIService == null)
        {
            alertUIService
                = ServiceUtils.getService(
                        bundleContext,
                        AlertUIService.class);
        }
        return alertUIService;
    }

    /**
     * @return the <tt>DiagnosticsService</tt> obtained from the bundle
     * context
     */
    public static DiagnosticsService getDiagnosticsService()
    {
        if (diagnosticsService == null)
        {
            diagnosticsService
                = ServiceUtils.getService(
                        bundleContext,
                        DiagnosticsService.class);
        }
        return diagnosticsService;
    }

    /**
     * @return the <tt>ConferenceService</tt> obtained from the bundle
     * context
     */
    public static ConferenceService getConferenceService()
    {
        if (conferenceService == null)
        {
            conferenceService
                = ServiceUtils.getService(
                        bundleContext,
                        ConferenceService.class);
        }
        return conferenceService;
    }

    /**
     * @return a reference to the BrowserLauncherService
     */
    public static BrowserLauncherService getBrowserService()
    {
        if (browserService == null)
        {
            browserService = ServiceUtils.getService(bundleContext,
                                                  BrowserLauncherService.class);
        }

        return browserService;
    }

    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     * context
     */
    public static MetaContactListService getContactListService()
    {
        if (metaCListService == null)
        {
            metaCListService
                = ServiceUtils.getService(
                        bundleContext,
                        MetaContactListService.class);
        }
        return metaCListService;
    }

    /**
     * @return the <tt>WISPAService</tt> obtained from the bundle
     * context
     */
    public static WISPAService getWISPAService()
    {
        if (wispaService == null)
        {
            wispaService
                    = ServiceUtils.getService(
                    bundleContext,
                    WISPAService.class);
        }
        return wispaService;
    }

    /**
     * @return the <tt>ShutdownService</tt> obtained from the bundle
     * context
     */
    public static ShutdownService getShutdownService()
    {
        if (shutdownService == null)
        {
            shutdownService
                    = ServiceUtils.getService(
                    bundleContext,
                    ShutdownService.class);
        }
        return shutdownService;
    }

    /**
     * @return the <tt>CommPortalService</tt> obtained from the bundle
     * context
     */
    public static CommPortalService getCommPortalService()
    {
        if (commPortalService == null)
        {
            commPortalService
                    = ServiceUtils.getService(
                    bundleContext,
                    CommPortalService.class);
        }
        return commPortalService;
    }
}
