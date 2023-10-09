/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.osdependent;

import java.beans.*;

import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.osdependent.macosx.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.Logger;

/**
 * Registers the <tt>OSDependentService</tt> in the UI Service.
 *
 * @author Nicolas Chamouard
 * @author Lubomir Marinov
 */
public class OsDependentActivator
    implements BundleActivator
{
    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    public static UIService uiService;

    /**
     * The <tt>Logger</tt> used by the <tt>OsDependentActivator</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OsDependentActivator.class);

    /**
     * Called when this bundle is started.
     *
     * @param bc The execution context of the bundle being started.
     */
    public void start(BundleContext bc)
    {
        bundleContext = bc;
        logger.entry();

        try
        {
            // Adds a MacOSX specific dock icon listener in order to show main
            // contact list window on dock icon click.
            if (OSUtils.IS_MAC)
                MacOSXDockIcon.addDockIconListener();
        }
        finally
        {
            logger.exit();
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bc The execution context of the bundle being stopped.
     */
    public void stop(BundleContext bc)
    {
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle
     * context.
     * @return the <tt>UIService</tt> obtained from the bundle
     * context
     */
    public static UIService getUIService()
    {
        if(uiService == null)
        {
            ServiceReference<?> serviceRef = bundleContext
                .getServiceReference(UIService.class.getName());

            if (serviceRef != null)
                uiService = (UIService) bundleContext.getService(serviceRef);
        }

        return uiService;
    }

    public void providerStatusMessageChanged(PropertyChangeEvent evt)
    {
        // Nothing to do
    }
}
