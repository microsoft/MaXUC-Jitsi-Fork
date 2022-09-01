/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.version;

import org.jitsi.service.configuration.*;
import org.jitsi.service.version.Version;
import org.jitsi.service.version.VersionService;
import org.osgi.framework.*;

import net.java.sip.communicator.util.*;

/**
 * The entry point to the Version Service Implementation. We register the
 * VersionServiceImpl instance on the OSGi BUS.
 *
 * @author Emil Ivov
 */
public class VersionActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by this <tt>VersionActivator</tt> instance for
     * logging output.
     */
    private final Logger logger = Logger.getLogger(VersionActivator.class);

    /**
     * The OSGi <tt>BundleContext</tt>.
     */
    private static BundleContext bundleContext;

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context)
    {
        logger.debug("Started.");

        VersionActivator.bundleContext = context;

        context.registerService(
                VersionService.class.getName(),
                new VersionServiceImpl(),
                null);

        logger.debug("Jitsi Version Service ... [REGISTERED]");

        Version version = VersionImpl.currentVersion();
        String applicationName = version.getApplicationName();
        String versionString = version.toString();

        logger.info("Jitsi Version: " + applicationName + " " + versionString);

        //register properties for those that would like to use them
        ConfigurationService cfg = getConfigurationService();

        cfg.global().setProperty(Version.PNAME_APPLICATION_NAME, applicationName, true);
        cfg.global().setProperty(Version.PNAME_APPLICATION_VERSION, versionString, true);
    }

    /**
     * Gets a <tt>ConfigurationService</tt> implementation currently
     * registered in the <tt>BundleContext</tt> in which this bundle has been
     * started or <tt>null</tt> if no such implementation was found.
     *
     * @return a <tt>ConfigurationService</tt> implementation currently
     * registered in the <tt>BundleContext</tt> in which this bundle has been
     * started or <tt>null</tt> if no such implementation was found
     */
    private static ConfigurationService getConfigurationService()
    {
        return
            ServiceUtils.getService(bundleContext, ConfigurationService.class);
    }

    /**
     * Gets the <tt>BundleContext</tt> instance within which this bundle has
     * been started.
     *
     * @return the <tt>BundleContext</tt> instance within which this bundle has
     * been started
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
    }
}
