/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.branding;

import java.lang.reflect.*;
import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.version.NightlyBuildID;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Branding bundle activator.
 */
public class BrandingActivator
    extends AbstractServiceDependentActivator
    implements BundleListener
{
    private final Logger logger = Logger.getLogger(BrandingActivator.class);

    private static BundleContext bundleContext;

    private static ResourceManagementService resourcesService;

    private static ImageLoaderService imageLoaderService;

    /**
     * The Java commit hash the build was made from, to be displayed in
     * CI-built clients whose branding has net.java.sip.communicator.GIT_HASH=true.
     */
    public static String javaCommitHash;

    /**
     * The Electron commit hash the build was made from, to be displayed in
     * CI-built clients whose branding has net.java.sip.communicator.GIT_HASH=true.
     */
    public static String electronCommitHash;

    public void start(BundleContext bc) throws Exception
    {
        super.start(bc);
        logger.info("Starting branding activator");

        if (getResources().getSettingsString(
                "service.gui.APPLICATION_NAME").equals("SIP Communicator"))
            new JitsiWarningWindow(null).setVisible(true);

        bundleContext.addBundleListener(this);
        logger.info("Started branding activator");
    }

    /**
     * Bundle has been started if welcome window is available and visible
     * update it to show the bundle activity.
     * @param evt
     */
    public synchronized void bundleChanged(BundleEvent evt)
    {
    }

    /**
     * Setting context to the activator, as soon as we have one.
     *
     * @param context the context to set.
     */
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    /**
     * This activator depends on UIService.
     * @return the class name of uiService.
     */
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * The dependent service is available and the bundle will start.
     * @param dependentService the UIService this activator is waiting.
     */
    public void start(Object dependentService)
    {
        logger.info("UI Service started");
        // UI-Service started.

        /*
         * Don't let bundleContext retain a reference to this
         * listener because it'll retain a reference to
         * welcomeWindow. Besides, we're no longer interested in
         * handling events so it doesn't make sense to even retain
         * this listener.
         */
        bundleContext.removeBundleListener(this);

        // register the about dialog menu entry
        registerMenuEntry((UIService)dependentService);
        logger.info("Registered about menu");

        bundleContext.registerService(BrandingService.class.getName(),
                                      new AboutWindow(null),
                                      new Hashtable<>());

        javaCommitHash = NightlyBuildID.JAVA_COMMIT_ID;
        logger.info("The Java git commit hash for this build is " + javaCommitHash);

        electronCommitHash = NightlyBuildID.ELECTRON_COMMIT_ID;
        logger.info("The Electron git commit hash for this build is " + electronCommitHash);
    }

    public void stop(BundleContext arg0)
    {
    }

    /**
     * Register the about menu entry.
     * @param uiService
     */
    private void registerMenuEntry(UIService uiService)
    {
        if ((uiService == null)
                || !uiService.useMacOSXScreenMenuBar()
                || !registerMenuEntryMacOSX(uiService))
        {
            registerMenuEntryNonMacOSX(uiService);
        }
    }

    private boolean registerMenuEntryMacOSX(UIService uiService)
    {
        Exception exception = null;
        try
        {
            Class<?> clazz =
                Class
                    .forName("net.java.sip.communicator.plugin.branding.MacOSXAboutRegistration");
            Method method = clazz.getMethod("run", (Class<?>[]) null);
            Object result = method.invoke(null, (Object[]) null);

            if (result instanceof Boolean)
                return (Boolean) result;
        }
        catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex)
        {
            exception = ex;
        }
        if (exception != null)
            logger.error(
                "Failed to register Mac OS X-specific About handling.",
                exception);
        return false;
    }

    private void registerMenuEntryNonMacOSX(UIService uiService)
    {
        // Register the about window plugin component in the main help menu.
        Hashtable<String, String> helpMenuFilter
            = new Hashtable<>();
        helpMenuFilter.put( Container.CONTAINER_ID,
                            Container.CONTAINER_HELP_MENU.getID());

        bundleContext.registerService(  PluginComponent.class.getName(),
                                        new AboutWindowPluginComponent(
                                            Container.CONTAINER_HELP_MENU),
                                        helpMenuFilter);

        logger.info("ABOUT WINDOW ... [REGISTERED]");

        // Register the about window plugin component in the chat help menu.
        Hashtable<String, String> chatHelpMenuFilter
            = new Hashtable<>();
        chatHelpMenuFilter.put( Container.CONTAINER_ID,
                                Container.CONTAINER_CHAT_HELP_MENU.getID());

        bundleContext.registerService(  PluginComponent.class.getName(),
                                        new AboutWindowPluginComponent(
                                            Container.CONTAINER_CHAT_HELP_MENU),
                                        chatHelpMenuFilter);

        logger.info("CHAT ABOUT WINDOW ... [REGISTERED]");
    }

    static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    public static ConfigurationService getConfigurationService()
    {
        ServiceReference<?> serRef
            = bundleContext
                .getServiceReference(ConfigurationService.class.getName());
        return
            (serRef == null)
                ? null
                : (ConfigurationService) bundleContext.getService(serRef);
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }

    /**
     * Returns the <tt>ImageLoaderService</tt>.
     *
     * @return the <tt>ImageLoaderService</tt>.
     */
    public static ImageLoaderService getImageLoaderService()
    {
        if (imageLoaderService == null)
            imageLoaderService = ServiceUtils.getService(
                                       bundleContext, ImageLoaderService.class);
        return imageLoaderService;
    }
}
