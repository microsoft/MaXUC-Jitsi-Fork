// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.diagnostics;

import java.util.*;

import org.osgi.framework.*;

/**
 * An convenience class which makes the process of registering and unregistering
 * a state dumper on a DiagnosticsService implementation easier
 */
public class DiagnosticsServiceRegistrar
{
    /**
     * Lock preventing multiple access to the list of state dumpers that are
     * awaiting adding.
     */
    private static final Object sLock = new Object();

    /**
     * If true, then indicates that we are already trying to get the Diagnostics
     * Service
     */
    private static boolean sGettingDiagsService = false;

    /**
     * The current implementation of the diagnostics service - will be null
     * while we wait for the service to be registered
     */
    private static DiagnosticsService sDiagnosticsService;

    /**
     * A list of state dumpers that we tried to add while there was no registered
     * Diagnostics Service
     */
    private static List<StateDumper> sDumpersAwaitingAdd = new ArrayList<>();

    /**
     * Convenience method for registering a state dumper on the Diagnostics Service
     * If there currently is no registered Diagnostics Service, then the state
     * dumper will be added to the DiagsService when it does finally register.
     *
     * @param stateDumper The state dumper to register
     * @param context Context used to get the DiagsService if we don't yet have one.
     */
    public static void registerStateDumper(StateDumper stateDumper,
                                           BundleContext context)
    {
        synchronized (sLock)
        {
            if (sDiagnosticsService != null)
            {
                // We've got a diags service, so can add directly
                sDiagnosticsService.addStateDumper(stateDumper);
            }
            else if (!sGettingDiagsService)
            {
                // We've not got a diags service, and we're not yet getting one,
                // so get it.
                sDumpersAwaitingAdd.add(stateDumper);
                sGettingDiagsService = true;
                context.addServiceListener(new DiagnosticsServiceListener(context));
            }
            else
            {
                // We've not got a diags service yet, but we are listening for
                // one.  Just add this dumper to the list of waiting dumpers.
                sDumpersAwaitingAdd.add(stateDumper);
            }
        }
    }

    /**
     * Convenience method for removing a registered state dumper from the
     * Diagnostics Service
     *
     * @param stateDumper the state dumper to remove
     */
    public static void unregisterStateDumper(StateDumper stateDumper)
    {
        synchronized (sLock)
        {
            if (sDiagnosticsService != null)
            {
                sDiagnosticsService.removeStateDumper(stateDumper);
            }
            else
            {
                sDumpersAwaitingAdd.remove(stateDumper);
            }
        }
    }

    private static class DiagnosticsServiceListener implements ServiceListener
    {
        private final BundleContext mContext;

        public DiagnosticsServiceListener(BundleContext context)
        {
            mContext = context;
        }

        @Override
        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference<?> serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want
            // to know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
                return;

            Object service = mContext.getService(serviceRef);

            // we don't care if the source service is not the diags service
            if (!(service instanceof DiagnosticsService))
                return;

            mContext.removeServiceListener(this);

            synchronized (sLock)
            {
                sDiagnosticsService = (DiagnosticsService)service;
                sGettingDiagsService = false;

                for (StateDumper stateDumper : sDumpersAwaitingAdd)
                {
                    sDiagnosticsService.addStateDumper(stateDumper);
                }
            }
        }
    }
}
