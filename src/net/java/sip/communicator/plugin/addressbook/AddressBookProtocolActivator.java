/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;

import net.java.sip.communicator.service.analytics.AnalyticsService;
import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.threading.*;
import net.java.sip.communicator.util.*;

/**
 * Activator for the address book plugin
 */
public class AddressBookProtocolActivator
    implements BundleActivator
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(AddressBookProtocolActivator.class);

    /**
     * OSGI context.
     */
    private static BundleContext bundleContext;

    /**
     * Service reference for the currently valid Address Book provider factory.
     */
    private static ServiceRegistration<?> ppFactoryServReg = null;

    /**
     * The protocol provider factory.
     */
    private static ProtocolProviderFactoryAddressBookImpl providerFactory = null;

    /**
     * A mutual exclusion to restrict the creation and a single simultaneous
     * access to the provider factory.
     */
    private static Object providerFactoryLock = new Object();

    /**
     * The analytics service
     */
    private static AnalyticsService analyticsService;

    /**
     * The resources.
     */
    private static ResourceManagementService resourcesService;

    /**
     * The MetaContactListService
     */
    private static MetaContactListService metaContactListService;

    /**
     * The service giving access to files.
     */
    private static FileAccessService fileAccessService = null;

    /**
     * The <tt>ConfigurationService</tt> through which we access configuration
     * properties.
     */
    private static ConfigurationService configService;

    /**
     * The <tt>ContactSyncBarService</tt> we use to indicate that we are
     * synchronising with the contacts server
     */
    private static ContactSyncBarService contactSyncComponent;

    /**
     * The diagnostics service used to auto-send error reports
     */
    private static DiagnosticsService diagsService;

    /**
     * The global status service used to update the meeting status
     */
    private static GlobalStatusService globalStatusService;

    /**
     * The threading service used to schedule background tasks
     */
    private static ThreadingService threadingService;

    /**
     * Starts the bundle.
     *
     * @param bundleContext
     */
    public void start(BundleContext bundleContext)
    {
        logger.info("Address Book Protocol Activator ... [STARTED]");

        AddressBookProtocolActivator.bundleContext = bundleContext;

        logger.debug("Starting provider factory");
        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.ADDRESS_BOOK);

        // Add a shutdown hook to ensure graceful shutdown
        Runtime.getRuntime().addShutdownHook(mShutdownHook);

        synchronized(providerFactoryLock)
        {
            if(providerFactory == null && ppFactoryServReg == null)
            {
                providerFactory = new ProtocolProviderFactoryAddressBookImpl();
                providerFactory.init();

                // Register the protocol factory so the account manager will be
                // notified of it.
                ppFactoryServReg = bundleContext.registerService(
                        ProtocolProviderFactory.class.getName(),
                        providerFactory,
                        hashtable);

                logger.info(
                        "Contact source protocol activator ... [REGISTERED]");
            }
        }
    }

    /**
     * Stops the bundle.
     * @param bundleContext
     */
    public void stop(BundleContext bundleContext)
    {
        logger.debug("Stopping provider factory");
        stopAndUnregister();
        logger.info("Contact source protocol activator ... [UNREGISTERED]");
        logger.info("Address Book Protocol Activator ... [STOPPED]");
    }

    private void stopAndUnregister()
    {
        synchronized(providerFactoryLock)
        {
            if(providerFactory != null)
            {
                providerFactory.stop();
                providerFactory = null;
                logger.debug("ProviderFactory stopped");
            }
            else
            {
                logger.debug("ProviderFactory already stopped");
            }

            if(ppFactoryServReg != null)
            {
                ppFactoryServReg.unregister();
                ppFactoryServReg = null;
                logger.debug("ppFactoryServReg unregistered");
            }
            else
            {
                logger.debug("ppFactoryServReg already unregistered");
            }
        }
    }

    // In SFR542944, we saw problems when the addressbook uninitialization thread was
    // interrupted by the shutdown, leaving the outlook data file in a bad state.
    // We added a shutdown hook to try and ensure the address book was properly
    // uninitialised before we exit.
    private final Thread mShutdownHook = new Thread("AddressBook shutdown hook")
    {
        @Override
        public void run()
        {
            logger.info("Running AddressBookProtocolActivator shutdown hook");
            stopAndUnregister();
        }
    };

    /**
     * Gets the analytics service.
     */
    public static AnalyticsService getAnalyticsService() {
        if (analyticsService == null)
            analyticsService = ServiceUtils.getService(
                    bundleContext, AnalyticsService.class);

        return analyticsService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }

    /**
     * Gets the <tt>ConfigurationService</tt>.
     *
     * @return the <tt>ConfigurationService</tt> to be used by the
     * functionality of the addrbook plug-in
     */
    public static ConfigurationService getConfigService()
    {
        if (configService == null)
        {
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configService;
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * witn.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        return ServiceUtils.getService(bundleContext, AccountManager.class);
    }

    /**
     * Returns the MetaContactListService.
     *
     * @return The MetaContactListService.
     */
    public static MetaContactListService getMetaContactListService()
    {
        if (metaContactListService == null)
        {
            metaContactListService
            = ServiceUtils.getService(
                bundleContext,
                MetaContactListService.class);
        }

        return metaContactListService;
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService = ServiceUtils.getService(bundleContext,
                                                        FileAccessService.class);
        }

        return fileAccessService;
    }

    /**
     * Returns the <tt>ContactSyncBarService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>ContactSyncBarService</tt> obtained from the bundle
     * context
     */
    public static ContactSyncBarService getContactSyncBarService()
    {
        if (contactSyncComponent == null)
        {
            contactSyncComponent = ServiceUtils.getService(bundleContext,
                                                   ContactSyncBarService.class);
        }

        return contactSyncComponent;
    }

    /**
     * Returns the <tt>DiagnosticsService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>DiagnosticsService</tt> obtained from the bundle
     * context
     */
    public static DiagnosticsService getDiagsService()
    {
        if (diagsService == null)
        {
            diagsService = ServiceUtils.getService(bundleContext,
                                                   DiagnosticsService.class);
        }

        return diagsService;
    }

    /**
     * Returns the <tt>GlobalStatusService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>GlobalStatusService</tt> obtained from the bundle
     * context
     */
    public static GlobalStatusService getGlobalStatusService()
    {
        if (globalStatusService == null)
        {
            globalStatusService = ServiceUtils.getService(bundleContext,
                                                     GlobalStatusService.class);
        }

        return globalStatusService;
    }

    /**
     * @return the <tt>ThreadingService</tt> obtained from the bundle context
     */
    public static ThreadingService getThreadingService()
    {
        if (threadingService == null)
        {
            threadingService = ServiceUtils.getService(bundleContext,
                                                       ThreadingService.class);
        }

        return threadingService;
    }
}
