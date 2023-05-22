/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.threading.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <code>BundleActivator</code> for the purposes of
 * protocol.jar/protocol.provider.manifest.mf and in order to register and start
 * services independent of the specifics of a particular protocol.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class ProtocolProviderActivator
    implements BundleActivator
{
    /**
     * The object used for logging.
     */
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderActivator.class);

    /**
     * The <code>ServiceRegistration</code> of the <code>AccountManager</code>
     * implementation registered as a service by this activator and cached so
     * that the service in question can be properly disposed of upon stopping
     * this activator.
     */
    private ServiceRegistration<?> accountManagerServiceRegistration;

    /**
     * The account manager.
     */
    private static AccountManager accountManager;

    /**
     * The <code>BundleContext</code> of the one and only
     * <code>ProtocolProviderActivator</code> instance which is currently
     * started.
     */
    public static BundleContext bundleContext;

    private static UIService uiService;

    /**
     * The <code>ConfigurationService</code> used by the classes in the bundle
     * represented by <code>ProtocolProviderActivator</code>.
     */
    private static ConfigurationService configurationService;

    /**
     * The GlobalDisplayDetailsService used to access details such as
     * the users display name
     */
    private static GlobalDisplayDetailsService globalDisplayDetailsService;

    /**
     * The resource service through which we obtain localized strings.
     */
    private static ResourceManagementService resourceService;

    /**
     * The diagnostics service, used to auto-send error reports
     */
    private static DiagnosticsService diagsService;

    /**
     * The image loader service
     */
    private static ImageLoaderService imageLoaderService;

    /**
     * The MetaContactListService, used to find MetaContacts
     */
    private static MetaContactListService metaContactListService;

    /**
     * The threading service, used to schedule tasks
     */
    private static ThreadingService threadingService;

    /**
     * The conference service, used to manage conferences
     */
    private static ConferenceService conferenceService;

    /**
     * The <code>SingleCallInProgressPolicy</code> making sure that the
     * <code>Call</code>s accessible in the <code>BundleContext</code> of this
     * activator will obey to the rule that a new <code>Call</code> should put
     * the other existing <code>Call</code>s on hold.
     */
    private SingleCallInProgressPolicy singleCallInProgressPolicy;

    /**
     * Gets the <code>ConfigurationService</code> to be used by the classes in
     * the bundle represented by <code>ProtocolProviderActivator</code>.
     *
     * @return the <code>ConfigurationService</code> to be used by the classes
     *         in the bundle represented by
     *         <code>ProtocolProviderActivator</code>
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = (ConfigurationService)
                    bundleContext.getService(
                        bundleContext.getServiceReference(
                            ConfigurationService.class.getName()));
        }
        return configurationService;
    }

    /**
     * Returns the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context.
     *
     * @return a reference to the currently registered
     * <tt>GlobalDisplayDetailsService</tt>
     */
    public static GlobalDisplayDetailsService getGlobalDisplayDetailsService()
    {
        if (globalDisplayDetailsService == null)
        {
            globalDisplayDetailsService
                = ServiceUtils.getService(
                        bundleContext,
                        GlobalDisplayDetailsService.class);
        }
        return globalDisplayDetailsService;
    }

    /**
     * Gets the <code>ResourceManagementService</code> to be used by the classes
     * in the bundle represented by <code>ProtocolProviderActivator</code>.
     *
     * @return the <code>ResourceManagementService</code> to be used by the
     *          classes in the bundle represented by
     *          <code>ProtocolProviderActivator</code>
     */
    public static ResourceManagementService getResourceService()
    {
        if (resourceService == null)
        {
            resourceService
                = (ResourceManagementService)
                    bundleContext.getService(
                        bundleContext.getServiceReference(
                            ResourceManagementService.class.getName()));
        }
        return resourceService;
    }

    /**
     * @return a reference to the currently registered diagnostics service
     */
    public static ImageLoaderService getImageLoaderService()
    {
        if (imageLoaderService == null)
        {
            imageLoaderService = ServiceUtils.getService(bundleContext,
                                                   ImageLoaderService.class);
        }

        return imageLoaderService;
    }

    /**
     * @return a reference to the currently registered diagnostics service
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
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     * @return a reference to the currently registered
     * <tt>MetaContactListService</tt>
     */
    public static MetaContactListService getContactListService()
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
     * Returns a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider.
     * @param protocolName the name of the protocol, which factory we're
     * looking for
     * @return a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider
     */
    public static ProtocolProviderFactory getProtocolProviderFactory(
            String protocolName)
    {
        String osgiFilter
            = "(" + ProtocolProviderFactory.PROTOCOL + "=" + protocolName + ")";
        ProtocolProviderFactory protocolProviderFactory = null;

        try
        {
            ServiceReference<?>[] serRefs
                = bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        osgiFilter);

            if ((serRefs != null) && (serRefs.length != 0))
            {
                protocolProviderFactory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(serRefs[0]);
            }
        }
        catch (InvalidSyntaxException ex)
        {
            logger.info("ProtocolProviderActivator : " + ex);
        }

        return protocolProviderFactory;
    }

    /**
     * Registers a new <code>AccountManagerImpl</code> instance as an
     * <code>AccountManager</code> service and starts a new
     * <code>SingleCallInProgressPolicy</code> instance to ensure that only one
     * of the <code>Call</code>s accessible in the <code>BundleContext</code>
     * in which this activator is to execute will be in progress and the others
     * will automatically be put on hold.
     *
     * @param bundleContext the <code>BundleContext</code> in which the bundle
     *            activation represented by this <code>BundleActivator</code>
     *            executes
     */
    public void start(BundleContext bundleContext)
    {
        ProtocolProviderActivator.bundleContext = bundleContext;

        accountManager = new AccountManager(bundleContext);
        accountManagerServiceRegistration =
            bundleContext.registerService(AccountManager.class.getName(),
                accountManager, null);

        singleCallInProgressPolicy =
            new SingleCallInProgressPolicy(bundleContext);
    }

    /**
     * Unregisters the <code>AccountManagerImpl</code> instance registered as an
     * <code>AccountManager</code> service in {@link #start(BundleContext)} and
     * stops the <code>SingleCallInProgressPolicy</code> started there as well.
     *
     * @param bundleContext the <code>BundleContext</code> in which the bundle
     *            activation represented by this <code>BundleActivator</code>
     *            executes
     */
    public void stop(BundleContext bundleContext)
    {
        if (accountManagerServiceRegistration != null)
        {
            accountManagerServiceRegistration.unregister();
            accountManagerServiceRegistration = null;
            accountManager = null;
        }

        if (singleCallInProgressPolicy != null)
        {
            singleCallInProgressPolicy.dispose();
            singleCallInProgressPolicy = null;
        }

        if (bundleContext.equals(ProtocolProviderActivator.bundleContext))
            ProtocolProviderActivator.bundleContext = null;

        configurationService = null;
        resourceService = null;
    }

    /**
     * Returns all protocol providers currently registered.
     * @return all protocol providers currently registered.
     */
    public static List<ProtocolProviderService>
        getProtocolProviders()
    {
        ServiceReference<?>[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("ProtocolProviderActivator : " + e);
        }

        List<ProtocolProviderService>
            providersList = new ArrayList<>();

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                ProtocolProviderService pp
                    = (ProtocolProviderService)bundleContext.getService(serRef);

                providersList.add(pp);
            }
        }
        return providersList;
    }

    /**
     * Get the <tt>AccountManager</tt> of the protocol.
     *
     * @return <tt>AccountManager</tt> of the protocol
     */
    public static AccountManager getAccountManager()
    {
        return accountManager;
    }

    /**
     * Returns the <tt>ThreadingService</tt> obtained from the bundle context
     *
     * @return the <tt>ThreadingService</tt> obtained from the bundle context
     */
    public static ThreadingService getThreadingService()
    {
        if (threadingService == null)
        {
            threadingService =
                 ServiceUtils.getService(bundleContext, ThreadingService.class);
        }

        return threadingService;
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        }

        return uiService;
    }

    /**
     * Returns the <tt>ConferenceService</tt> obtained from the bundle context.
     *
     * @return the <tt>ConferenceService</tt> obtained from the bundle context
     */
    public static ConferenceService getConferenceService()
    {
        if (conferenceService == null)
            conferenceService =
                ServiceUtils.getService(bundleContext, ConferenceService.class);

        return conferenceService;
    }
}
