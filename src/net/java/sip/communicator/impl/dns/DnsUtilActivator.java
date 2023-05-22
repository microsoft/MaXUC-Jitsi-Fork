/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.dns;

import static org.jitsi.util.Hasher.*;

import java.util.List;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;
import org.xbill.DNS.*;

import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.AccountManagerEvent;
import net.java.sip.communicator.service.protocol.event.AccountManagerListener;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * The DNS Util activator registers the DNS resolver.
 *
 * @author Emil Ivov
 * @author Ingo Bauersachs
 */
public class DnsUtilActivator
    implements BundleActivator,
               ServiceListener
{
    /** Class logger */
    private static final Logger logger
        = Logger.getLogger(DnsUtilActivator.class);

    private static ConfigurationService configurationService;
    private static ResourceManagementService resourceService;
    private static BundleContext bundleContext;
    private static AccountManager mAccountManager;

    /**
     * The address of the backup resolver we would use by default.
     */
    public static final String DEFAULT_BACKUP_RESOLVER
        = "backup-resolver.jitsi.net";

    /**
     * The name of the property that users may use to override the port
     * of our backup DNS resolver.
     */
    public static final String PNAME_BACKUP_RESOLVER_PORT
        = "net.java.sip.communicator.util.dns.BACKUP_RESOLVER_PORT";

    /**
     * The name of the property that users may use to override the
     * IP address of our backup DNS resolver. This is only used when the
     * backup resolver name cannot be determined.
     */
    public static final String PNAME_BACKUP_RESOLVER_FALLBACK_IP
        = "net.java.sip.communicator.util.dns.BACKUP_RESOLVER_FALLBACK_IP";

    /**
     * The default of the property that users may use to enable
     * our backup DNS resolver.
     */
    public static final boolean PDEFAULT_BACKUP_RESOLVER_ENABLED = false;

    /**
     * The name of the property that users may use to enable
     * our backup DNS resolver.
     */
    public static final String PNAME_BACKUP_RESOLVER_ENABLED
        = "net.java.sip.communicator.util.dns.BACKUP_RESOLVER_ENABLED";

    /**
     * The name of the property that users may use to override the
     * address of our backup DNS resolver.
     */
    public static final String PNAME_BACKUP_RESOLVER
        = "net.java.sip.communicator.util.dns.BACKUP_RESOLVER";

    /**
     * The default of the property that users may use to disable
     * our native DNS resolver.
     */
    public static final boolean PDEFAULT_NATIVE_RESOLVER_ENABLED = true;

    /**
     * The name of the property that users may use to disable
     * our native DNS resolver.
     */
    public static final String PNAME_NATIVE_RESOLVER_ENABLED
        = "net.java.sip.communicator.util.dns.NATIVE_RESOLVER_ENABLED";

    /**
     * Calls <tt>Thread.setUncaughtExceptionHandler()</tt>
     *
     * @param context The execution context of the bundle being started
     * (unused).
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context)
        throws Exception
    {
        logger.info("DNS service ... [STARTING]");
        bundleContext = context;
        context.addServiceListener(this);

        if (UtilActivator.getConfigurationService().global().getBoolean(
                    DnsUtilActivator.PNAME_BACKUP_RESOLVER_ENABLED,
                    DnsUtilActivator.PDEFAULT_BACKUP_RESOLVER_ENABLED))
        {
            bundleContext.registerService(
                CustomResolver.class.getName(),
                new ParallelResolverImpl(),
                null);
            logger.info("ParallelResolver ... [REGISTERED]");
        }
        else if (UtilActivator.getConfigurationService().global().getBoolean(
                    DnsUtilActivator.PNAME_NATIVE_RESOLVER_ENABLED,
                    DnsUtilActivator.PDEFAULT_NATIVE_RESOLVER_ENABLED))
        {
            bundleContext.registerService(
                CustomResolver.class.getName(),
                new NativeResolver(),
                null);
            logger.info("NativeResolver ... [REGISTERED]");
        }

        logger.info("DNS service ... [STARTED]");
    }

    /**
     * Listens for when network connectivity changes in any way and resets dns
     * configuration.
     */
    private static class NetworkListener
        implements NetworkConfigurationChangeListener
    {
        /**
         * Fired when a change has occurred in the
         * computer network configuration.
         *
         * @param event the change event.
         */
        public void configurationChanged(ChangeEvent event)
        {
            if (!event.isInitial())
            {
                reloadDnsResolverConfig();
            }
        }
    }

    /**
     * Listens for when user accounts are loaded and notifies the
     * CustomResolver.
     */
    private static class AccountListener
        implements AccountManagerListener
    {
        /**
         * Fired when a change has occurred in the loaded accounts.
         *
         * @param event the change event.
         */
        @Override
        public void handleAccountManagerEvent(AccountManagerEvent event)
        {
            if (event.getType() == AccountManagerEvent.ACCOUNT_LOADED)
            {
                notifyAccountLoaded();
            }
        }
    }

    /**
     * Reloads dns server configuration in the resolver.
     */
    public static void reloadDnsResolverConfig()
    {
        // reread system dns configuration
        ResolverConfig.refresh();

        StringBuilder sb = new StringBuilder();
        sb.append("Reloaded resolver config, default DNS servers are: ");
        sb.append(logCollectionHasher(List.of(
            NetworkUtils.convertServerListToStrings(
                ResolverConfig.getCurrentConfig().servers()))));
        logger.info(sb.toString());

        // now reset an eventually present custom resolver
        if (Lookup.getDefaultResolver() instanceof CustomResolver)
        {
            logger.info("Resetting custom resolver "
                    + Lookup.getDefaultResolver().getClass().getSimpleName());

            ((CustomResolver)Lookup.getDefaultResolver()).reset();
        }
        else
        {
            // or the default otherwise
            Lookup.refreshDefault();
        }
    }

    /**
     * Tells CustomResolvers to register for account changes.
     */
    public static void notifyAccountLoaded()
    {
        // now reset an eventually present custom resolver
        if(Lookup.getDefaultResolver() instanceof CustomResolver)
        {
            ((CustomResolver)Lookup.getDefaultResolver()).accountLoaded();
        }
        else
        {
            // or the default otherwise
            Lookup.refreshDefault();
        }
    }

    /**
     * Doesn't do anything.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context)
        throws Exception
    {
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
    }

    /**
     * Returns the service giving access to all application resources.
     *
     * @return the service giving access to all application resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resourceService;
    }

    /**
     * Listens on OSGi service changes and registers a listener for network
     * changes as soon as the change-notification service is available
     */
    public void serviceChanged(ServiceEvent event)
    {
        if (event.getType() != ServiceEvent.REGISTERED)
        {
            return;
        }

        Object service = bundleContext.getService(event.getServiceReference());
        if (service instanceof NetworkAddressManagerService)
        {
            logger.info("Bind DNS NetworkListener to new " +
                "NetworkAddressManagerService: " + service);
            ((NetworkAddressManagerService)service)
              .addNetworkConfigurationChangeListener(new NetworkListener());
        }
        else if (service instanceof ProtocolProviderService)
        {
            logger.info("Listen for Account changes on " +
                "ProtocolProviderService: " + service);
            getAccountManager().addListener(new AccountListener());
        }
    }

    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(mAccountManager == null)
        {
            mAccountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);
        }

        return mAccountManager;
    }
}
