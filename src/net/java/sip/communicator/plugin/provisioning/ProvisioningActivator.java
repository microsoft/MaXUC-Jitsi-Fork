/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.provisioning;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.provisioning.*;
import net.java.sip.communicator.service.update.*;
import net.java.sip.communicator.service.wispaservice.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;

/**
 * Activator the provisioning system. It will retrieve configuration file and
 * push properties to the <tt>ConfigurationService</tt>.
 */
public class ProvisioningActivator
    implements BundleActivator
{
    /**
     * Logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ProvisioningActivator.class);

    /**
     * The current BundleContext.
     */
    static BundleContext bundleContext = null;

    /**
     * A reference to the ConfigurationService implementation instance that
     * is currently registered with the bundle context.
     */
    private static ConfigurationService configurationService = null;

    /**
     * A reference to the CredentialsStorageService implementation instance
     * that is registered with the bundle context.
     */
    private static CredentialsStorageService credentialsService = null;

    /**
     * A reference to the NetworkAddressManagerService implementation instance
     * that is registered with the bundle context.
     */
    private static NetworkAddressManagerService netaddrService = null;

    /**
     * The user interface service.
     */
    private static UIService uiService;

    /**
     * The resource service.
     */
    private static ResourceManagementService resourceService;

    /**
     * Provisioning service.
     */
    private static ProvisioningServiceImpl provisioningService = null;

    /**
     * The analytics service
     */
    private static AnalyticsService analyticsService;

    /**
     * The update service
     */
    private static UpdateService updateService;

    /**
     * The account manager
     */
    private static AccountManager accountManager;

    /**
     * The WISPA Service
     */
    private static WISPAService wispaService;

    /**
     * Starts this bundle
     *
     * @param bundleContext BundleContext
     * @throws Exception if anything goes wrong during the start of the bundle
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        logger.debug("Provisioning discovery [STARTED]");

        ProvisioningActivator.bundleContext = bundleContext;
        provisioningService = new ProvisioningServiceImpl();

        String method = provisioningService.getProvisioningMethod();

        if (StringUtils.isNullOrEmpty(method, true) || method.equals("NONE"))
        {
            return;
        }

        provisioningService.start();

        bundleContext.registerService(
            ProvisioningService.class.getName(), provisioningService, null);

        logger.debug("Provisioning discovery [REGISTERED]");
    }

    /**
     * Stops this bundle
     *
     * @param bundleContext BundleContext
     */
    public void stop(BundleContext bundleContext)
    {
        ProvisioningActivator.bundleContext = null;
        provisioningService.stop();

        logger.debug("Provisioning discovery [STOPPED]");
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
            ServiceReference<?> uiReference =
                bundleContext.getServiceReference(UIService.class.getName());

            uiService =
                (UIService) bundleContext
                    .getService(uiReference);
        }

        return uiService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the
     * bundle context.
     *
     * @return the <tt>ResourceManagementService</tt> obtained from the
     * bundle context
     */
    public static ResourceManagementService getResourceService()
    {
        if (resourceService == null)
        {
            ServiceReference<?> resourceReference
                = bundleContext.getServiceReference(
                    ResourceManagementService.class.getName());

            resourceService =
                (ResourceManagementService) bundleContext
                    .getService(resourceReference);
        }

        return resourceService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            ServiceReference<?> confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService)bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a CredentialsStorageService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * CredentialsStorageService.
     */
    public static CredentialsStorageService getCredentialsStorageService()
    {
        if (credentialsService == null)
        {
            ServiceReference<?> credentialsReference
                = bundleContext.getServiceReference(
                    CredentialsStorageService.class.getName());
            credentialsService
                = (CredentialsStorageService) bundleContext
                                        .getService(credentialsReference);
        }
        return credentialsService;
    }

    /**
     * Returns a reference to a NetworkAddressManagerService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * NetworkAddressManagerService.
     */
    public static NetworkAddressManagerService getNetworkAddressManagerService()
    {
        if (netaddrService == null)
        {
            ServiceReference<?> netaddrReference
                = bundleContext.getServiceReference(
                    NetworkAddressManagerService.class.getName());
            netaddrService
                = (NetworkAddressManagerService) bundleContext
                                        .getService(netaddrReference);
        }
        return netaddrService;
    }

    /**
     * Returns a reference to a <tt>ProvisioningService</tt> implementation.
     *
     * @return a currently valid implementation of <tt>ProvisioningService</tt>
     */
    public static ProvisioningServiceImpl getProvisioningService()
    {
        return provisioningService;
    }

    /**
     * Returns a reference to a <tt>AnalyticsService</tt> implementation.
     *
     * @return a currently valid implementation of <tt>AnalyticsService</tt>
     */
    public static AnalyticsService getAnalyticsService()
    {
        if (analyticsService == null)
        {
            analyticsService = ServiceUtils.getService(bundleContext,
                                                       AnalyticsService.class);
        }

        return analyticsService;
    }

    /**
     * Returns a reference to a <tt>UpdateService</tt> implementation
     *
     * @return a currently valid implementation of <tt>UpdateService</tt>
     */
    public static UpdateService getUpdateService()
    {
        if (updateService == null)
        {
            updateService = ServiceUtils.getService(bundleContext,
                                                    UpdateService.class);
        }

        return updateService;
    }

    /**
     * Returns a reference to an <tt>AccountManager</tt> implementation
     *
     * @return a curently valid implementation of <tt>AccountManager</tt>
     */
    public static AccountManager getAccountManager()
    {
        if (accountManager == null)
        {
            accountManager = ServiceUtils.getService(bundleContext,
                                                     AccountManager.class);
        }

        return accountManager;
    }

    /**
     * Returns a reference to a <tt>WISPAService</tt> implementation
     *
     * @return a currently valid implementation of <tt>WISPAService</tt>
     */
    public static WISPAService getWISPAService()
    {
        if (wispaService == null)
        {
            wispaService = ServiceUtils.getService(bundleContext,
                                                   WISPAService.class);
        }

        return wispaService;
    }
}
