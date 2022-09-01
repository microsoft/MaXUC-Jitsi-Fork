/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.*;

/**
 * Activates the MessageHistoryService
 *
 * @author Damian Minkov
 */
public class MessageHistoryActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>MessageHistoryActivator</tt> class and its instances for logging
     * output.
     */
    private static Logger sLog =
        Logger.getLogger(MessageHistoryActivator.class);

    /** The <tt>MessageHistoryService</tt> reference. */
    private static MessageHistoryServiceImpl msgHistoryService = null;

    /** The <tt>ResourceManagementService</tt> reference. */
    private static ResourceManagementService resourcesService;

    /** The <tt>ConfigurationService</tt> reference. */
    private static ConfigurationService configService;

    /** The <tt>WISPAService</tt> reference. */
    private static WISPAService wispaService;

    /** The <tt>MetaContactListService</tt> reference. */
    private static MetaContactListService metaContactListService;

    /** The <tt>PhoneNumberUtilsService</tt> reference. */
    private static PhoneNumberUtilsService phoneNumberUtilsService;

    private static AnalyticsService analyticsService;

    /** The <tt>BundleContext</tt> of the service. */
    static BundleContext sBundleContext;

    /**
     * @return a reference to the BundleContext instance that we were started
     * with.
     */
    public static BundleContext getBundleContext()
    {
        return sBundleContext;
    }

    /**
     * Initialize and start message history
     *
     * @param bc the BundleContext
     */
    public void start(BundleContext bc)
    {
        sBundleContext = bc;
        try
        {
            sLog.entry();

            // Create and register the message history contact source.
            MessageHistoryContactSource msgHistoryContactSource =
                                              new MessageHistoryContactSource();

            sBundleContext.registerService(
                ContactSourceService.class.getName(),
                msgHistoryContactSource,
                null);

            //Create and start the message history service.
            msgHistoryService = new MessageHistoryServiceImpl(msgHistoryContactSource);

            msgHistoryService.start(sBundleContext);

            sBundleContext.registerService(
                MessageHistoryService.class.getName(), msgHistoryService, null);

            sLog.info("Message History Service ...[REGISTERED]");
        }
        finally
        {
            sLog.exit();
        }
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     */
    public void stop(BundleContext bundleContext)
    {
        if(msgHistoryService != null)
            msgHistoryService.stop(bundleContext);
    }

    /**
     * Returns the instance of <tt>MessageHistoryService</tt> created in this
     * activator.
     */
    public static MessageHistoryServiceImpl getMessageHistoryService()
    {
        return msgHistoryService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            resourcesService =
                ServiceUtils.getService(sBundleContext,
                                        ResourceManagementService.class);
        }

        return resourcesService;
    }

    /**
     * Returns the <tt>PhoneNumberUtilsService</tt> obtained from the bundle
     * context.
     */
    public static PhoneNumberUtilsService getPhoneNumberUtilsService()
    {
        if(phoneNumberUtilsService == null)
        {
            phoneNumberUtilsService =
                ServiceUtils.getService(sBundleContext,
                                        PhoneNumberUtilsService.class);
        }

        return phoneNumberUtilsService;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            configService =
                ServiceUtils.getService(sBundleContext,
                                        ConfigurationService.class);
        }

        return configService;
    }

    /**
     * Returns the <tt>WISPAService</tt> obtained from the bundle
     * context.
     */
    public static WISPAService getWISPAService()
    {
        if(wispaService == null)
        {
            wispaService =
                ServiceUtils.getService(sBundleContext,
                                        WISPAService.class);
        }

        return wispaService;
    }

    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     */
    public static MetaContactListService getContactListService()
    {
        if (metaContactListService == null)
        {
            metaContactListService =
                ServiceUtils.getService(sBundleContext,
                                        MetaContactListService.class);
        }

        return metaContactListService;
    }

    /**
     * @return a reference to the Analytics Service
     */
    public static AnalyticsService getAnalyticsService()
    {
        if (analyticsService == null)
        {
            analyticsService = ServiceUtils.getService(sBundleContext,
                                                       AnalyticsService.class);
        }

        return analyticsService;
    }
}
