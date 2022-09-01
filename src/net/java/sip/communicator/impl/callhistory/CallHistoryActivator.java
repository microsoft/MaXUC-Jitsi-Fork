/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

/**
 * Activates the <tt>CallHistoryService</tt>.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class CallHistoryActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallHistoryActivator</tt> class and
     * its instances for logging output.
     */
    private static final Logger sLog
        = Logger.getLogger(CallHistoryActivator.class);

    /**
     * The bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The <tt>CallHistoryServiceImpl</tt> instantiated in the start method
     * of this bundle.
     */
    private static CallHistoryServiceImpl callHistoryService = null;

    /**
     * The service responsible for resources.
     */
    private static ResourceManagementService resourcesService;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The phone number utils service
     */
    private static PhoneNumberUtilsService phoneNumberUtilsService;

    /**
     * The CommPortal service
     */
    private static CommPortalService commPortalService;

    /**
     * The Class of Service service
     */
    private static ClassOfServiceService cosService;

    /**
     * The global status service
     */
    private static GlobalStatusService globalStatusService;

    /**
     * The network address service
     */
    private static NetworkAddressManagerService networkAddressService;

    /**
     * The contact source service
     */
    private static ContactSourceService contactSourceService;

    /**
     * The UI service
     */
    private static UIService uiService;

    /**
     * The notification service, which keeps track of the three types of
     * notification - missed calls, unread messages, and voicemails.
     */
    private static NotificationService notificationService;

    /**
     * The map containing all registered
     */
    private static final Map<Object, ProtocolProviderFactory>
        providerFactoriesMap = new Hashtable<>();

    /**
     * Initialize and start call history
     *
     * @param bc the <tt>BundleContext</tt>
     */
    public void start(BundleContext bc)
    {
        bundleContext = bc;

        try
        {
            sLog.entry();

            ServiceReference<?> refDatabase = bundleContext.getServiceReference(
                DatabaseService.class.getName());

            DatabaseService databaseService = (DatabaseService)
                bundleContext.getService(refDatabase);

            //Create and start the call history service.
            callHistoryService = new CallHistoryServiceImpl(databaseService);
            callHistoryService.start(bundleContext);

            bundleContext.registerService(
                CallHistoryService.class.getName(), callHistoryService, null);

            bundleContext.registerService(
                ContactSourceService.class.getName(),
                new CallHistoryContactSource(), null);

            sLog.info("Call History Service ...[REGISTERED]");
        }
        finally
        {
            sLog.exit();
        }
    }

    /**
     * Stops this bundle.
     * @param bundleContext the <tt>BundleContext</tt>
     */
    public void stop(BundleContext bundleContext)
    {
        if(callHistoryService != null)
            callHistoryService.stop(bundleContext);
    }

    /**
     * Returns the instance of <tt>CallHistoryService</tt> created in this
     * activator.
     * @return the instance of <tt>CallHistoryService</tt> created in this
     * activator
     */
    public static CallHistoryService getCallHistoryService()
    {
        return callHistoryService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }

    /**
     * Returns the <tt>PhoneNumberUtilsService</tt> obtained from the bundle
     * context.
     * @return the <tt>PhoneNumberUtilsService</tt> obtained from the bundle
     * context
     */
    public static PhoneNumberUtilsService getPhoneNumberUtilsService()
    {
        if(phoneNumberUtilsService == null)
        {
            phoneNumberUtilsService
                = ServiceUtils.getService(
                        bundleContext,
                        PhoneNumberUtilsService.class);
        }
        return phoneNumberUtilsService;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configService;
    }

    /**
     * Returns the <tt>CommPortalService</tt> obtained from the bundle
     * context.
     * @return the <tt>CommPortalService</tt> obtained from the bundle
     * context
     */
    public static CommPortalService getCommPortalService()
    {
        if (commPortalService == null)
        {
            commPortalService = ServiceUtils.getService(bundleContext,
                                                        CommPortalService.class);
        }

        return commPortalService;
    }

    /**
     * Returns the <tt>ClassOfServiceService</tt> obtained from the bundle
     * context.
     * @return the <tt>ClassOfServiceService</tt> obtained from the bundle
     * context
     */
    public static ClassOfServiceService getCosService()
    {
        if (cosService == null)
        {
            cosService = ServiceUtils.getService(bundleContext,
                                                 ClassOfServiceService.class);
        }

        return cosService;
    }

    /**
     * Returns the <tt>GlobalStatusService</tt> obtained from the bundle
     * context.
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
     * Returns the <tt>NetworkAddressManagerService</tt> obtained from the bundle
     * context.
     * @return the <tt>NetworkAddressManagerService</tt> obtained from the bundle
     * context
     */
    public static NetworkAddressManagerService getNetworkAddressService()
    {
        if (networkAddressService == null)
        {
            networkAddressService = ServiceUtils.getService(bundleContext,
                                            NetworkAddressManagerService.class);
        }

        return networkAddressService;
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
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

    public static NotificationService getNotificationService()
    {
        if (notificationService == null)
        {
            notificationService = ServiceUtils.getService(bundleContext, NotificationService.class);
        }

        return notificationService;
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     *         context
     */
    public static Map<Object, ProtocolProviderFactory>
        getProtocolProviderFactories()
    {
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
            sLog.error("LoginManager : ", e);
        }

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(serRef);

                providerFactoriesMap.put(
                        serRef.getProperty(ProtocolProviderFactory.PROTOCOL),
                        providerFactory);
            }
        }
        return providerFactoriesMap;
    }

    /**
     * Get a reference to the Contact Source Service of type Search - in most
     * cases, this will be the LDAP service
     *
     * @return the Contact Source Service
     */
    public static ContactSourceService getContactSearchService()
    {
        if (contactSourceService == null)
        {
            try
            {
                // There are multiple different ContactSourceServices (including
                // Call History).  Therefore, we have to get them all and search
                // through them to get the one that we want.
                String name = ContactSourceService.class.getName();
                ServiceReference<?>[] serviceReferences =
                                 bundleContext.getServiceReferences(name, null);
                if (serviceReferences != null)
                {
                    for (ServiceReference<?> ref : serviceReferences)
                    {
                        ContactSourceService source =
                            (ContactSourceService) bundleContext.getService(ref);

                        if (source.getType() == ContactSourceService.LDAP_SEARCH_TYPE)
                        {
                            contactSourceService = source;
                            break;
                        }
                    }
                }
            }
            catch (InvalidSyntaxException e)
            {
                sLog.error("Error getting the LDAP service", e);
            }
        }

        return contactSourceService;
    }

    /**
     * Get the provider that implements the MWI Op set.  Note that even if there
     * are multiple such providers, only the first will be returned.
     *
     * @return the protocol provider service that implements the Operation Set
     *         Message Waiting class.
     */
    public static ProtocolProviderService getProviderWithMwi()
    {
        Class<OperationSetMessageWaiting> operationSet = OperationSetMessageWaiting.class;
        Map<Object, ProtocolProviderFactory> factories = getProtocolProviderFactories();

        // Look through each factory:
        for (ProtocolProviderFactory factory : factories.values())
        {
            sLog.debug("Looking at factory " + factory);
            ArrayList<AccountID> accounts = factory.getRegisteredAccounts();

            // For each, account, see if it supports MWI
            for (AccountID account : accounts)
            {
                sLog.debug("Looking at account " + account);
                ServiceReference<?> serRef = factory.getProviderForAccount(account);
                ProtocolProviderService provider =
                       (ProtocolProviderService)bundleContext.getService(serRef);

                if (provider.getOperationSet(operationSet) != null)
                {
                    sLog.debug("Found provider " + provider);
                    return provider;
                }
            }
        }

        sLog.warn("Not found any providers");
        return null;
    }
}
