/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.globalshortcut;

import org.osgi.framework.*;

import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.util.*;

/**
 * OSGi Activator for global shortcut.
 *
 * @author Sebastien Vincent
 */
public class GlobalShortcutActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>GlobalShortcutActivator</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger =
        Logger.getLogger(GlobalShortcutActivator.class);

    /**
     * The OSGi <tt>ServiceRegistration</tt> of <tt>GlobalShortcut</tt>.
     */
    private ServiceRegistration<?> serviceRegistration;

    /**
     * The <tt>GlobalShortcutServiceImpl</tt>.
     */
    protected static GlobalShortcutServiceImpl globalShortcutService = null;

    /**
     * OSGi bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * Keybindings service reference.
     */
    private static KeybindingsService keybindingsService = null;

    /**
     * UI service reference.
     */
    private static UIService uiService = null;

    private static WISPAService sWISPAService;
    private static AnalyticsService sAnalyticsService;

    /**
     * Returns the <tt>KeybindingsService</tt> obtained from the bundle context.
     *
     * @return the <tt>KeybindingsService</tt> obtained from the bundle context
     */
    public static KeybindingsService getKeybindingsService()
    {
        if (keybindingsService == null)
        {
            keybindingsService
                = ServiceUtils.getService(
                        bundleContext,
                        KeybindingsService.class);
        }
        return keybindingsService;
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            uiService
                = ServiceUtils.getService(
                        bundleContext,
                        UIService.class);
        }
        return uiService;
    }

    public static WISPAService getWISPAService()
    {
        if (sWISPAService == null)
        {
            sWISPAService = ServiceUtils.getService(bundleContext,
                                                    WISPAService.class);
        }

        return sWISPAService;
    }

    public static AnalyticsService getAnalyticsService()
    {
        if (sAnalyticsService == null)
        {
            sAnalyticsService = ServiceUtils.getService(bundleContext,
                                                        AnalyticsService.class);
        }

        return sAnalyticsService;
    }

    /**
     * Starts the execution of this service bundle in the specified context.
     *
     * @param bundleContext the context in which the service bundle is to
     * start executing
     */
    public void start(BundleContext bundleContext)
    {
        GlobalShortcutActivator.bundleContext = bundleContext;
        serviceRegistration = null;
        globalShortcutService = new GlobalShortcutServiceImpl();
        globalShortcutService.start();
        bundleContext.registerService(GlobalShortcutService.class.getName(),
                globalShortcutService, null);

        globalShortcutService.reloadGlobalShortcuts();

        registerListenerWithProtocolProviderService();

        bundleContext.addServiceListener(new ServiceListener()
        {
            public void serviceChanged(ServiceEvent serviceEvent)
            {
                GlobalShortcutActivator.this.serviceChanged(serviceEvent);
            }
        });
        logger.debug("GlobalShortcut Service ... [REGISTERED]");
    }

    /**
     * Stops the execution of this service bundle in the specified context.
     *
     * @param bundleContext the context in which this service bundle is to
     * stop executing
     */
    public void stop(BundleContext bundleContext)
    {
        if (serviceRegistration != null)
        {
            globalShortcutService.stop();
            serviceRegistration.unregister();
            serviceRegistration = null;

            logger.debug("GlobalShortcut Service ... [UNREGISTERED]");
        }

        GlobalShortcutActivator.bundleContext = null;
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    private void serviceChanged(ServiceEvent event)
    {
        ServiceReference<?> serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object service = bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            this.handleProviderAdded((ProtocolProviderService) service);
            break;
        case ServiceEvent.UNREGISTERING:
            this.handleProviderRemoved((ProtocolProviderService) service);
            break;
        }
    }

    /**
     * Get all registered <tt>ProtocolProviderService</tt> and set our listener.
     */
    public void registerListenerWithProtocolProviderService()
    {
        ServiceReference<?>[] ppsRefs = null;

        try
        {
             ppsRefs
                 = bundleContext.getServiceReferences(
                         ProtocolProviderService.class.getName(),
                         null);
        }
        catch(InvalidSyntaxException ise)
        {
            logger.error(
                    "Failed to get ProtocolProviderService ServiceReferences",
                    ise);
        }

        if(ppsRefs == null)
            return;

        for(ServiceReference<?> ppsRef : ppsRefs)
        {
            ProtocolProviderService pps
                = (ProtocolProviderService) bundleContext.getService(ppsRef);
            OperationSetBasicTelephony<?> opSet
                = pps.getOperationSet(OperationSetBasicTelephony.class);

            if(opSet != null)
                opSet.addCallListener(globalShortcutService.getCallShortcut());
        }
    }

    /**
     * Notifies this manager that a specific
     * <tt>ProtocolProviderService</tt> has been registered as a service.
     *
     * @param provider the <tt>ProtocolProviderService</tt> which has been
     * registered as a service.
     */
    private void handleProviderAdded(final ProtocolProviderService provider)
    {
        OperationSetBasicTelephony<?> opSet =
            provider.getOperationSet(OperationSetBasicTelephony.class);
        if(opSet != null)
            opSet.addCallListener(globalShortcutService.getCallShortcut());
    }

    /**
     * Notifies this manager that a specific
     * <tt>ProtocolProviderService</tt> has been unregistered as a service.
     *
     * @param provider the <tt>ProtocolProviderService</tt> which has been
     * unregistered as a service.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicTelephony<?> opSet =
            provider.getOperationSet(OperationSetBasicTelephony.class);
        if(opSet != null)
            opSet.removeCallListener(globalShortcutService.getCallShortcut());
    }
}
