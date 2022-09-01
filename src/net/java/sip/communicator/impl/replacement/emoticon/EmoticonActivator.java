/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.emoticon;

import java.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Activator for the Emoticon source bundle.
 * @author Purvesh Sahoo
 */
public class EmoticonActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>EmoticonActivator</tt>
     * class.
     */
    private static final Logger logger =
        Logger.getLogger(EmoticonActivator.class);

    /**
     * The currently valid bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * The resources service
     */
    private static ResourceManagementService resourcesService;

    /**
     * The emoticon service registration.
     */
    private ServiceRegistration<?> emoticonServReg = null;

    /**
     * The source implementation reference.
     */
    private static ChatPartReplacementService emoticonSource = null;

    /**
     * Starts the emoticon replacement source bundle
     *
     * @param context the <tt>BundleContext</tt> as provided from the OSGi
     * framework
     */
    public void start(BundleContext context)
    {
        bundleContext = context;

        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put(ChatPartReplacementService.SOURCE_NAME,
            EmoticonReplacementService.EMOTICON_SERVICE);
        emoticonSource = new EmoticonReplacementService();

        emoticonServReg
            = context.registerService(ChatPartReplacementService.class.getName(),
                emoticonSource, hashtable);

        logger.info("Emoticon source implementation [STARTED].");

    }

    /**
     * Unregisters the Emoticon replacement service.
     *
     * @param context BundleContext
     */
    public void stop(BundleContext context)
    {
        emoticonServReg.unregister();
        logger.info("Emoticon source implementation [STOPPED].");
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     *         access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference<?> serviceReference =
                bundleContext
                    .getServiceReference(ResourceManagementService.class
                        .getName());

            if (serviceReference == null)
                return null;

            resourcesService =
                (ResourceManagementService) bundleContext
                    .getService(serviceReference);
        }
        return resourcesService;
    }
}
