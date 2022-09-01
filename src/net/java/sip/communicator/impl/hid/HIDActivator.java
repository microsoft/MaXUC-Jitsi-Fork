/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.hid;

import org.osgi.framework.*;

import net.java.sip.communicator.service.hid.*;
import net.java.sip.communicator.util.*;

/**
 * OSGi activator for the HID service.
 *
 * @author Sebastien Vincent
 */
public class HIDActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>HIDActivator</tt> class and its
     * instances for logging output.
     */
    private final Logger logger = Logger.getLogger(HIDActivator.class);

    /**
     * The OSGi <tt>ServiceRegistration</tt> of <tt>HIDServiceImpl</tt>.
     */
    private ServiceRegistration<?> serviceRegistration;

    /**
     * Starts the execution of the <tt>hid</tt> bundle in the specified context.
     *
     * @param bundleContext the context in which the <tt>hid</tt> bundle is to
     * start executing
     */
    public void start(BundleContext bundleContext)
    {
        logger.debug("Started.");

        serviceRegistration =
            bundleContext.registerService(HIDService.class.getName(),
                new HIDServiceImpl(), null);

        logger.debug("HID Service ... [REGISTERED]");
    }

    /**
     * Stops the execution of the <tt>hid</tt> bundle in the specified context.
     *
     * @param bundleContext the context in which the <tt>hid</tt> bundle is to
     * stop executing
     */
    public void stop(BundleContext bundleContext)
    {
        if (serviceRegistration != null)
        {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
