/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.accountinfo;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Starts the account info bundle.
 *
 * @author Adam Glodstein
 */
public class AccountInfoActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(AccountInfoActivator.class);

    /**
     * The OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private static BrowserLauncherService browserLauncherService;

    @Override
    public void start(BundleContext bc)
    {
        AccountInfoActivator.bundleContext = bc;
    }

    @Override
    public void stop(BundleContext bc)
    {
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     *         context
     */
    public static Map<Object, ProtocolProviderFactory> getProtocolProviderFactories()
    {
        Map<Object, ProtocolProviderFactory> providerFactoriesMap =
                new Hashtable<>();

        ServiceReference<?>[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("LoginManager : " + e);
        }

        for (ServiceReference<?> ref : serRefs)
        {
            ProtocolProviderFactory providerFactory =
                    (ProtocolProviderFactory) bundleContext.getService(ref);

            providerFactoriesMap
                    .put(ref.getProperty(ProtocolProviderFactory.PROTOCOL),
                         providerFactory);
        }

        return providerFactoriesMap;
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> currently registered.
     *
     * @return the <tt>BrowserLauncherService</tt>
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            ServiceReference<?> serviceReference =
                bundleContext.getServiceReference(BrowserLauncherService.class
                    .getName());

            browserLauncherService =
                (BrowserLauncherService) bundleContext
                    .getService(serviceReference);
        }

        return browserLauncherService;
    }
}
