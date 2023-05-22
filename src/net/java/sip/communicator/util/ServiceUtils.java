/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import static net.java.sip.communicator.util.UtilActivator.bundleContext;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.service.shutdown.ShutdownService;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPAMotion;
import net.java.sip.communicator.service.wispaservice.WISPAMotionType;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;

/**
 * Gathers utility functions related to OSGi services such as getting a service
 * registered in a BundleContext.
 *
 * @author Lubomir Marinov
 */
public class ServiceUtils
{
    private static final Logger logger = Logger.getLogger(ServiceUtils.class);

    /** Prevents the creation of <tt>ServiceUtils</tt> instances. */
    private ServiceUtils() {}

    /**
     * Gets an OSGi service registered in a specific <tt>BundleContext</tt> by
     * its <tt>Class</tt>
     *
     * @param <T> the very type of the OSGi service to get
     * @param bundleContext the <tt>BundleContext</tt> in which the service to
     * get has been registered
     * @param serviceClass the <tt>Class</tt> with which the service to get has
     * been registered in the <tt>bundleContext</tt>
     * @return the OSGi service registered in <tt>bundleContext</tt> with the
     * specified <tt>serviceClass</tt> if such a service exists there;
     * otherwise, <tt>null</tt>
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(
            BundleContext bundleContext,
            Class<T> serviceClass)
    {
        ServiceReference<?> serviceReference
            = bundleContext.getServiceReference(serviceClass.getName());

        return
            (serviceReference == null)
                ? null
                : (T) bundleContext.getService(serviceReference);
    }

    /**
     * Convenience method for getting a reference to an OSGI service that may or
     * may not be registered.
     *
     * @param context the <tt>BundleContext</tt> in which the service to get has
     *        been registered
     * @param serviceClass The type of the service that we are getting
     * @param callback object used to return the service when it has been obtained
     */
    public static <T> void getService(final BundleContext context,
                                      final Class<T> serviceClass,
                                      final ServiceCallback<T> callback)
    {
        ServiceListener listener = new ServiceListener()
        {
            @SuppressWarnings("unchecked")
            @Override
            public void serviceChanged(ServiceEvent event)
            {
                ServiceReference<?> refFromEvent = event.getServiceReference();
                ServiceReference<?> refToWatchedService =
                    context.getServiceReference(serviceClass.getName());

                if (refFromEvent.getBundle().getState() == Bundle.STOPPING)
                    // Event is caused by a bundle being stopped, ignore.
                    return;

                if (refFromEvent != refToWatchedService)
                    // Event is not for the service being watched, ignore.
                    return;

                callback.onServiceRegistered(
                    (T) context.getService(refToWatchedService));
                context.removeServiceListener(this);
            }
        };

        context.addServiceListener(listener);

        // Having added the service listener, check to see if the service has
        // already been registered
        T service = getService(context, serviceClass);
        if (service != null)
        {
            // Service already registered
            context.removeServiceListener(listener);
            callback.onServiceRegistered(service);
        }
    }

    /**
     * Wait for a list of services to be registered and call a single method
     * when all are ready. In essence, a more general version of getService()
     * above that does not pass any services as arguments to the eventual
     * callback.
     *
     * Behaviour common to both getService() and getServices() is commented in
     * getService() above. Only differences or new behaviour are documented
     * here.
     *
     * @param context The bundle context
     * @param serviceClasses A list of services we want to wait for
     * @param callback The callback to run when all services are registered
     */
    public static void getServices(final BundleContext context,
                                   final Class<?>[] serviceClasses,
                                   final MultiServiceCallback callback)
    {
        // Create an empty set of services that we know have registered and the
        // number of services we're waiting for, so we can identify when all are
        // registered. Also add a lock for set synchronisation.
        // This is a Set to ensure unique service entries.
        Set<Class<?>> serviceRegistrations = new HashSet<>();
        Object registrationLock = new Object();
        final int serviceCount = serviceClasses.length;

        ServiceListener listener = new ServiceListener()
        {
            @Override
            public void serviceChanged(ServiceEvent event)
            {
                ServiceReference<?> refFromEvent = event.getServiceReference();

                if (refFromEvent.getBundle().getState() == Bundle.STOPPING)
                    // Event is caused by a bundle being stopped, ignore.
                    return;

                for (Class<?> watchedServiceClass : serviceClasses)
                {
                    ServiceReference<?> refToWatchedService =
                        context.getServiceReference(watchedServiceClass.getName());

                    if (refFromEvent != refToWatchedService)
                        // Event is not for the service being watched, ignore.
                        continue;

                    if (event.getType() == ServiceEvent.REGISTERED)
                    {
                        synchronized (registrationLock)
                        {
                            serviceRegistrations.add(watchedServiceClass);
                        }
                    }
                    else if (event.getType() == ServiceEvent.UNREGISTERING)
                    {
                        synchronized (registrationLock)
                        {
                            serviceRegistrations.remove(watchedServiceClass);
                        }
                    }
                }

                synchronized (registrationLock)
                {
                    // If we have the expected number of registered services,
                    // activate the callback and remove the listener.
                    if (serviceRegistrations.size() == serviceCount)
                    {
                        context.removeServiceListener(this);
                        callback.onServicesRegistered();
                    }
                }
            }
        };

        context.addServiceListener(listener);

        // If a service was registered while we were adding the listener, and
        // hence getService() now returns a non-null value, add it to the list
        // of registrations.
        for (Class<?> serviceClass : serviceClasses)
        {
            if (getService(context, serviceClass) != null)
            {
                synchronized (registrationLock)
                {
                    serviceRegistrations.add(serviceClass);
                }
            }
        }

        synchronized (registrationLock)
        {
            // If we have the expected number of registered services,
            // activate the callback and remove the listener.
            if (serviceRegistrations.size() == serviceCount)
            {
                context.removeServiceListener(listener);
                callback.onServicesRegistered();
            }
        }
    }

    /**
     * A Callback use to get a reference to a service that may or may not be
     * registered yet
     *
     * @param <T> The class of the service that we are interested in
     */
    public abstract static class ServiceCallback<T>
    {
        /**
         * Called when the service has been registered
         *
         * @param service the implementation of the service that this callback
         *                was created for
         */
        public abstract void onServiceRegistered(T service);
    }

    /**
     * A callback used to get a reference to multiple services that may or may
     * not have been registered yet.
     */
    public abstract static class MultiServiceCallback
    {
        /**
         * Called when all services have been registered.
         */
        public abstract void onServicesRegistered();
    }

    /**
     * Cleanly shuts down the application.
     *
     * This should be used for shutdown in places where we cannot be certain
     * that the ShutdownService will have started (e.g cancelling login or CoS
     * check failure on startup).  It first tries to use the ShutdownService,
     * but if it isn't registered, it asks the Electron UI to shut down then
     * shuts down all of the OSGI bundles cleanly, one by one.
     *
     * @param callingContext the bundle context of the calling bundle, which we
     *                       use to ensure that we don't shut down the calling
     *                       bundle until all others (except this one) have been
     *                       shut down.
     */
    public static void shutdownAll(BundleContext callingContext)
    {
        shutdownAll(callingContext, false, false);
    }

    /**
     * Cleanly shuts down the application.
     *
     * This should be used for shutdown in places where we cannot be certain
     * that the ShutdownService will have started (e.g cancelling login or CoS
     * check failure on startup).  It first tries to use the ShutdownService,
     * but if it isn't registered, it asks the Electron UI to shut down then
     * shuts down all of the OSGI bundles cleanly, one by one.
     *
     * If the ShutdownService is available, we will respect the logOut and
     * electronTriggered booleans, and ignore them otherwise.
     *
     * @param callingContext
     * @param logOut
     * @param electronTriggered
     */
    public static void shutdownAll(BundleContext callingContext, boolean logOut, boolean electronTriggered)
    {
        // If the ShutdownService has registered, we should use that to shut
        // down the client in a clean and consistent way.
        ShutdownService shutdownService = UtilActivator.getShutdownService();
        if (shutdownService != null)
        {
            logger.info("Shutting down via shutdown service");
            shutdownService.beginShutdown(logOut, electronTriggered);
        }
        else
        {
            logger.info("Shutdown service not registered - " +
                         "shutting down bundles via BundleContext");

            // Before we start shutting down the Java OSGI bundles, we must first
            // send a WISPA message to ask the Electron UI to shut down.  Note
            // that we don't also need to shut down the Meeting client, as if
            // the ShutdownService isn't registered, neither will the
            // ConferenceService be registered.
            shutdownElectron();

            // Get all the application's bundles and stop them one by one.
            Bundle thisBundle = null;
            Bundle callingBundle = null;
            for (Bundle bundleToShutdown : bundleContext.getBundles())
            {
                // Don't close our bundle or our calling bundle yet, so that
                // we can keep closing other bundles.
                final BundleContext shutdownBundleContext =
                        bundleToShutdown.getBundleContext();
                if (bundleContext.equals(shutdownBundleContext))
                {
                    logger.info("Not shutting down this bundle " +
                                 bundleToShutdown + " " + bundleToShutdown.getLocation());
                    thisBundle = bundleToShutdown;
                }
                else if (callingContext != null &&
                         callingContext.equals(shutdownBundleContext))
                {
                    logger.info("Not shutting down calling bundle " +
                                 bundleToShutdown + " " + bundleToShutdown.getLocation());
                    callingBundle = bundleToShutdown;
                }
                else
                {
                    stopBundle(bundleToShutdown);
                }
            }

            // We've shut down all other bundles so we can safely shutdown our
            // calling bundle.
            if (callingBundle != null)
            {
                stopBundle(callingBundle);
            }

            // Finally, shut down this bundle.
            if (thisBundle != null)
            {
                stopBundle(thisBundle);
            }
        }
    }

    /**
     * Sends a WISPA message to ask the Electron UI to shut down.
     */
    private static void shutdownElectron()
    {
        WISPAService wispaService = UtilActivator.getWISPAService();
        if (wispaService != null)
        {
            logger.info("Sending shutdown request to Electron.");
            wispaService.notify(WISPANamespace.CORE,
                                WISPAAction.MOTION,
                                new WISPAMotion(WISPAMotionType.SHUTDOWN));
        }
        else
        {
            logger.error("Cannot quit Electron - WISPAService is null.");
        }
    }

    /**
     * Stops the given bundle.
     *
     * @param bundle the bundle to be stopped.
     */
    private static void stopBundle(Bundle bundle)
    {
        try
        {
            logger.debug("Stopping bundle: " + bundle +
                         " " + bundle.getLocation());
            bundle.stop();
        }
        catch (BundleException e)
        {
            logger.error("Exception stopping bundle: " +
                         bundle + " " + bundle.getLocation(), e);
        }
    }
}
