/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.profiler4j;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * Activates the profiler plug-in.
 *
 * @author Vladimir Skarupelov
 */
public class ProfilerActivator implements BundleActivator {

    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    Logger logger = Logger.getLogger(ProfilerActivator.class);

    private ServiceRegistration<?> menuRegistration = null;

    public void start(BundleContext bc)
    {
        bundleContext = bc;

        SettingsWindowMenuEntry menuEntry = new SettingsWindowMenuEntry(
                Container.CONTAINER_TOOLS_MENU);

        Hashtable<String, String> toolsMenuFilter =
                new Hashtable<>();
        toolsMenuFilter.put(Container.CONTAINER_ID,
                Container.CONTAINER_TOOLS_MENU.getID());

        menuRegistration = bc.registerService(PluginComponent.class
                .getName(), menuEntry, toolsMenuFilter);

        logger.info("PROFILER4J [REGISTERED]");
    }

    public void stop(BundleContext bc)
    {
        if (menuRegistration != null)
        {
            menuRegistration.unregister();
            logger.info("PROFILER4J [UNREGISTERED]");
        }
    }
}
