/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.contactsource.ContactSourceService;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.phonenumberutils.PhoneNumberUtilsService;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.provisioning.ProvisioningService;
import net.java.sip.communicator.service.shutdown.ShutdownService;
import net.java.sip.communicator.service.systray.SystrayService;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.service.websocketserver.WebSocketApiErrorService;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.fileaccess.FileAccessService;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.packetlogging.PacketLoggingService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.service.version.VersionService;

/**
 * Activates the SIP package
 * @author Emil Ivov
 */
public class SipActivator
    implements BundleActivator
{
    private static Logger logger =
                                 Logger.getLogger(SipActivator.class.getName());

    private        ServiceRegistration<?>  sipPpFactoryServReg   = null;
            static BundleContext        bundleContext         = null;
    private static ConfigurationService configurationService  = null;
    private static NetworkAddressManagerService networkAddressManagerService
                                                              = null;
    private static MediaService         mediaService          = null;
    private static VersionService       versionService        = null;
    private static UIService            uiService             = null;
    private static PacketLoggingService packetLoggingService  = null;
    private static FileAccessService    fileService           = null;
    private static SystrayService       systrayService        = null;

    /**
     * The resource service. Used for checking for default values
     * and loding status icons.
     */
    private static ResourceManagementService resources        = null;

    private static ProtocolProviderFactorySipImpl sipProviderFactory = null;

    private static PhoneNumberUtilsService phoneNumberUtils;

    private UriHandlerSipImpl sipUriHandlerSipImpl = null;
    private UriHandlerSipImpl telUriHandlerSipImpl = null;
    private UriHandlerSipImpl calltoUriHandlerSipImpl = null;
    private UriHandlerSipImpl maxuccallUriHandlerSipImpl = null;

    private static ProvisioningService provisioningService;
    private static CertificateService certificateService;
    private static ClassOfServiceService classOfServiceService;

    private static CommPortalService commPortalService;

    private static ShutdownService shutdownService;

    private static WebSocketApiErrorService webSocketApiErrorService;

    private static AnalyticsService analyticsService;

    /**
     * The threading service, used to schedule tasks
     */
    private static ThreadingService threadingService;

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context)
    {
        logger.debug("Started.");

        SipActivator.bundleContext = context;

        sipProviderFactory = createProtocolProviderFactory();

        /*
         * Install the UriHandler prior to registering the factory service in
         * order to allow it to detect when the stored accounts are loaded
         * (because they may be asynchronously loaded).
         */
        sipUriHandlerSipImpl = new UriHandlerSipImpl(sipProviderFactory, "sip");
        telUriHandlerSipImpl = new UriHandlerSipImpl(sipProviderFactory, "tel");
        calltoUriHandlerSipImpl = new UriHandlerSipImpl(sipProviderFactory, "callto");
        maxuccallUriHandlerSipImpl = new UriHandlerSipImpl(sipProviderFactory, "maxuccall");

        //reg the sip account man.
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.SIP);
        sipPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    sipProviderFactory,
                    properties);

        logger.debug("SIP Protocol Provider Factory ... [REGISTERED]");
    }

    /**
     * Creates the ProtocolProviderFactory for this protocol.
     * @return The created factory.
     */
    private ProtocolProviderFactorySipImpl createProtocolProviderFactory()
    {
        return new ProtocolProviderFactorySipImpl();
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
        if(configurationService == null)
        {
            ServiceReference<?> confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService) bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a NetworkAddressManagerService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * NetworkAddressManagerService .
     */
    public static NetworkAddressManagerService getNetworkAddressManagerService()
    {
        if(networkAddressManagerService == null)
        {
            ServiceReference<?> confReference
                = bundleContext.getServiceReference(
                    NetworkAddressManagerService.class.getName());
            networkAddressManagerService = (NetworkAddressManagerService)
                bundleContext.getService(confReference);
        }
        return networkAddressManagerService;
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
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * witn.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Retrurns a reference to the protocol provider factory that we have
     * registered.
     * @return a reference to the <tt>ProtocolProviderFactorySipImpl</tt>
     * instance that we have registered from this package.
     */
    public static ProtocolProviderFactorySipImpl getProtocolProviderFactory()
    {
        return sipProviderFactory;
    }

    /**
     * Returns a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static MediaService getMediaService()
    {
        if(mediaService == null)
        {
            ServiceReference<?> mediaServiceReference
                = bundleContext.getServiceReference(
                    MediaService.class.getName());
            mediaService = (MediaService)bundleContext
                .getService(mediaServiceReference);
        }
        return mediaService;
    }

    /**
     * Returns a reference to a VersionService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a VersionService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static VersionService getVersionService()
    {
        if(versionService == null)
        {
            ServiceReference<?> versionServiceReference
                = bundleContext.getServiceReference(
                    VersionService.class.getName());
            versionService = (VersionService)bundleContext
                .getService(versionServiceReference);
        }
        return versionService;
    }

    /**
     * Returns a reference to the UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static UIService getUIService()
    {
        if(uiService == null)
        {
            ServiceReference<?> uiServiceReference
                = bundleContext.getServiceReference(
                    UIService.class.getName());
            uiService = (UIService)bundleContext
                .getService(uiServiceReference);
        }
        return uiService;
    }

    /**
     * Returns a reference to the ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
        {
            resources
                = ServiceUtils.getService(
                        bundleContext, ResourceManagementService.class);
        }
        return resources;
    }

    /**
     * Returns a reference to the <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     */
    static PacketLoggingService getPacketLogging()
    {
        if (packetLoggingService == null)
        {
            packetLoggingService
                = ServiceUtils.getService(
                        bundleContext, PacketLoggingService.class);
        }
        return packetLoggingService;
    }

    /**
     * Return the file access service impl.
     * @return the FileAccess Service.
     */
    public static FileAccessService getFileAccessService()
    {
        if(fileService == null)
        {
            fileService = ServiceUtils.getService(
                bundleContext, FileAccessService.class);
        }

        return fileService;
    }

    /**
     * Returns the <tt>PhoneNumberUtilsService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>PhoneNumberUtilsService</tt> implementation obtained
     * from the bundle context.
     */
    public static PhoneNumberUtilsService getPhoneNumberUtils()
    {
        if (phoneNumberUtils == null)
        {
            phoneNumberUtils = ServiceUtils.getService(bundleContext,
                                                PhoneNumberUtilsService.class);
        }

        return phoneNumberUtils;
    }

    /**
     * Returns the <tt>ProvisioningService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>ProvisioningService</tt> implementation obtained
     * from the bundle context.
     */
    public static ProvisioningService getProvisioningService()
    {
        if (provisioningService == null)
        {
            provisioningService = ServiceUtils.getService(bundleContext,
                                                     ProvisioningService.class);
        }

        return provisioningService;
    }

    public static CertificateService getCertificateService()
    {
        if (certificateService == null)
        {
            certificateService = ServiceUtils.getService(bundleContext, CertificateService.class);
        }

        return certificateService;
    }

    /**
     * Returns a list of all registered contact sources.
     *
     * @return a list of all registered contact sources
     */
    public static List<ContactSourceService> getContactSources()
    {
        List<ContactSourceService> contactSources = new ArrayList<>();

        ServiceReference<?>[] serRefs = null;
        try
        {
            // Get all registered contact source services.
            serRefs =
                bundleContext.getServiceReferences(
                    ContactSourceService.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("Failed to get contact sources.", e);
        }

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                ContactSourceService contactSource
                    = (ContactSourceService) bundleContext.getService(serRef);

                contactSources.add(contactSource);
            }
        }

        return contactSources;
    }

    /**
     * Returns the <tt>SystrayService</tt> obtained from the bundle context.
     *
     * @return the <tt>SystrayService</tt> obtained from the bundle context
     */
    static SystrayService getSystrayService()
    {
        if (systrayService == null)
        {
            systrayService =
                ServiceUtils.getService(bundleContext, SystrayService.class);
        }
        return systrayService;
    }

    /**
     * Returns the <tt>ClassOfServiceService</tt> obtained from the bundle context.
     *
     * @return the <tt>ClassOfServiceService</tt> obtained from the bundle context
     */
    static ClassOfServiceService getClassOfServiceService()
    {
        if (classOfServiceService == null)
        {
            classOfServiceService = ServiceUtils.getService(bundleContext, ClassOfServiceService.class);
        }

        return classOfServiceService;
    }

    /**
     * Returns the <tt>CommPortalService</tt> obtained from the bundle context.
     *
     * @return the <tt>CommPortalService</tt> obtained from the bundle context
     */
    public static CommPortalService getCommPortalService()
    {
        if (commPortalService == null)
        {
            commPortalService = ServiceUtils.getService(bundleContext,
                CommPortalService.class);

            if (commPortalService == null)
            {
                logger.warn("CommPortalService is unexpectedly null.");
            }
        }

        return commPortalService;
    }

    /**
     * Returns the <tt>ShutdownService</tt> obtained from the bundle context.
     *
     * @return the <tt>ShutdownService</tt> obtained from the bundle context
     */
    public static ShutdownService getShutdownService()
    {
        if (shutdownService == null)
        {
            shutdownService = ServiceUtils.getService(bundleContext, ShutdownService.class);
        }

        return shutdownService;
    }

    /**
     * Returns the <tt>WebSocketApiErrorService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>WebSocketApiErrorService</tt> obtained from the bundle
     * context.
     */
    static WebSocketApiErrorService getWebSocketApiErrorService()
    {
        if (webSocketApiErrorService == null)
        {
            webSocketApiErrorService = ServiceUtils.getService(
                    bundleContext,
                    WebSocketApiErrorService.class);
        }

        return webSocketApiErrorService;
    }

    /**
     * Returns the <tt>AnalyticsService</tt> obtained from the bundle context.
     *
     * @return the <tt>AnalyticsService</tt> obtained from the bundle context
     */
    public static AnalyticsService getAnalyticsService()
    {
        if (analyticsService == null)
        {
            analyticsService = ServiceUtils.getService(bundleContext, AnalyticsService.class);
        }

        return analyticsService;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
        sipProviderFactory.stop();
        sipPpFactoryServReg.unregister();

        if (sipUriHandlerSipImpl != null)
        {
            sipUriHandlerSipImpl.dispose();
            sipUriHandlerSipImpl = null;
        }

        if (telUriHandlerSipImpl != null)
        {
            telUriHandlerSipImpl.dispose();
            telUriHandlerSipImpl = null;
        }

        if (calltoUriHandlerSipImpl != null)
        {
            calltoUriHandlerSipImpl.dispose();
            calltoUriHandlerSipImpl = null;
        }

        if (maxuccallUriHandlerSipImpl != null)
        {
            maxuccallUriHandlerSipImpl.dispose();
            maxuccallUriHandlerSipImpl = null;
        }

        configurationService = null;
        networkAddressManagerService = null;
        mediaService = null;
        versionService = null;
        uiService = null;
        packetLoggingService = null;
        fileService = null;
    }
}
