/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.windowscleanshutdown;

import java.util.concurrent.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.*;

/**
 * Tries to cleanly close the application on shutdown/logoff. The events used
 * here are only available on windows.
 *
 * If the application is still running once end session event is received
 * and we have give it time (currently 3 sec.) we System.exit() the application.
 *
 * @author Emil Ivov
 */
public class CleanShutdownActivator
    implements BundleActivator, ServiceListener
{
    private static final Logger logger = Logger.getLogger(CleanShutdownActivator.class);

    /**
     * Used to wait for stop.
     */
    final CountDownLatch synchShutdown = new CountDownLatch(1);

    /**
     * Our context.
     */
    private BundleContext context;

    /**
     * The system activity service.
     */
    SystemActivityNotificationsService sysActivityService = null;

    /**
     * Bundle activator start method.
     *
     */
    public void start(final BundleContext context)
    {
        this.context = context;

        logger.info("Starting the CleanShutdown service.");

        handleNewSystemActivityNotificationsService(getSystemActivityNotificationsService(context));

        // if missing will wait for it
        if (sysActivityService == null)
        {
            context.addServiceListener(this);
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
        // stop received.
        synchShutdown.countDown();
    }

    /**
     * Gets a reference to a <code>ShutdownService</code> implementation
     * currently registered in the bundle context of the active
     * <code>OsDependentActivator</code> instance.
     * <p>
     * The returned reference to <code>ShutdownService</code> is not being
     * cached.
     * </p>
     *
     * @return reference to a <code>ShutdownService</code> implementation
     *         currently registered in the bundle context of the active
     *         <code>OsDependentActivator</code> instance
     */
    private ShutdownService getShutdownService()
    {
        return(ShutdownService)context.getService(
            context.getServiceReference(ShutdownService.class.getName()));
    }

    /**
     * Gets a reference to a <code>SystemActivityNotificationsService</code>
     * implementation currently registered in the bundle context.
     * <p>
     * The returned reference to <code>SystemActivityNotificationsService</code>
     * is not being cached.
     * </p>
     *
     * @param context the bundle context.
     * @return reference to a <code>SystemActivityNotificationsService</code>
     *         implementation currently registered in the bundle context.
     */
    public static SystemActivityNotificationsService getSystemActivityNotificationsService(BundleContext context)
    {
        ServiceReference<?> ref = context.getServiceReference(SystemActivityNotificationsService.class.getName());

        if (ref == null)
        {
            return null;
        }
        else
        {
            return (SystemActivityNotificationsService) context.getService(ref);
        }
    }

    /**
     * Saves the reference for the service and
     * add a listener if the desired events are supported. Or start
     * the checking thread otherwise.
     * @param newService the service
     */
    private void handleNewSystemActivityNotificationsService(SystemActivityNotificationsService newService)
    {

        sysActivityService = newService;

        if (newService != null)
        {
            newService.addSystemActivityChangeListener(
                new SystemActivityChangeListener()
                {
                    public void activityChanged(SystemActivityEvent event)
                    {
                        if (event.getEventID() == SystemActivityEvent.EVENT_QUERY_ENDSESSION)
                        {
                            logger.info("Received shutdown query from OS; will quit app");

                            // Windows shutdown won't wait give us long, so don't wait for meeting to close.
                            getShutdownService().shutdownWithoutWaitingForMeeting();

                            // just wait a moment, or till we are stopped
                            try
                            {
                                synchronized(this)
                                {
                                    synchShutdown.await(1500, TimeUnit.MILLISECONDS);
                                }
                            }
                            catch(Throwable t)
                            {}
                        }
                        else if (event.getEventID() == SystemActivityEvent.EVENT_ENDSESSION)
                        {
                            logger.info("PC is shutting down. Await death.");
                            try
                            {
                                // wait till we are stopped or forced stopped
                                synchShutdown.await();
                            }
                            catch(Throwable t)
                            {}
                        }
                    }
                }
            );
        }
    }

    /**
     * When new SystemActivityNotificationsService
     * is registered we add needed listeners.
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        ServiceReference<?> serviceRef = serviceEvent.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know we are shutting down
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object sService = context
                .getService(serviceRef);

        if(sService instanceof SystemActivityNotificationsService)
        {
            switch (serviceEvent.getType())
            {
                case ServiceEvent.REGISTERED:
                    handleNewSystemActivityNotificationsService(
                        (SystemActivityNotificationsService)sService);
                    break;
                case ServiceEvent.UNREGISTERING:
                    //((SystemActivityNotificationsService)sService)
                    //    .removeSystemActivityChangeListener(this);
                    break;
            }

            return;
        }
    }
}
