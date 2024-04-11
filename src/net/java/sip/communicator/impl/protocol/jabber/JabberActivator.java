/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.Hashtable;
import java.util.regex.Pattern;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smackx.debugger.xmpp.XmppDebugger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.globaldisplaydetails.GlobalDisplayDetailsService;
import net.java.sip.communicator.service.gui.ContactSyncBarService;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.insights.InsightsService;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.phonenumberutils.PhoneNumberUtilsService;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.AuthorizationHandler;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;
import net.java.sip.communicator.service.resources.ResourceManagementServiceUtils;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.fileaccess.FileAccessService;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.packetlogging.PacketLoggingService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.service.version.VersionService;

/**
 * Loads the Jabber provider factory and registers it with  service in the OSGI
 * bundle context.
 *
 * @author Damian Minkov
 * @author Symphorien Wanko
 * @author Emil Ivov
 */
public class JabberActivator implements BundleActivator
{
    /**
     * Service reference for the currently valid Jabber provider factory.
     */
    private ServiceRegistration<?> jabberPpFactoryServReg = null;

    /**
     * Bundle context from OSGi.
     */
    static BundleContext bundleContext = null;

    /**
     * Configuration service.
     */
    private static ConfigurationService configurationService = null;

    /**
     * Media service.
     */
    private static MediaService mediaService = null;

    /**
     * A reference to the currently valid {@link NetworkAddressManagerService}.
     */
    private static NetworkAddressManagerService
                                        networkAddressManagerService = null;

    /**
     * A reference to the currently valid {@link CredentialsStorageService}.
     */
    private static CredentialsStorageService
                                        credentialsService = null;

    /**
     * The Jabber protocol provider factory.
     */
    private static ProtocolProviderFactoryJabberImpl
                                        jabberProviderFactory = null;

    /**
     * The <tt>UriHandler</tt> implementation that we use to handle "xmpp:" URIs
     */
    private UriHandlerJabberImpl uriHandlerImpl = null;

    /**
     * A reference to the currently valid <tt>UIService</tt>.
     */
    private static UIService uiService = null;

    /**
     * A reference to the currently valid <tt>ResoucreManagementService</tt>
     * instance.
     */
    private static ResourceManagementService resourcesService = null;

    /**
     * A reference to the currently valid <tt>PacketLoggingService</tt>
     * instance.
     */
    private static PacketLoggingService packetLoggingService = null;

    /**
     * A reference to the currently valid <tt>VersionService</tt>
     * instance.
     */
    private static VersionService versionService        = null;

    /**
     * A reference to the currently valid <tt>MetaContactListService</tt>
     * instance.
     */
    private static MetaContactListService metaContactListService = null;

    /**
     * A reference to the currently valid <tt>ContactSyncBarService</tt>
     * instance.
     */
    private static ContactSyncBarService contactSyncBarService = null;

    /**
     * A reference to the currently valid <tt>AuthorizationHandler</tt>
     * instance.
     */
    private static AuthorizationHandler authHandlerService = null;

    /**
     * The account manager
     */
    private static AccountManager accountManager;

    /**
     * The phone number utils service
     */
    private static PhoneNumberUtilsService phoneNumberUtils;

    /**
     * The image loader service
     */
    private static ImageLoaderService imageLoaderService;

    /**
     * The message history service
     */
    private static MessageHistoryService messageHistoryService;

    /**
     * The file access service
     */
    private static FileAccessService fileAccessService;

    /**
     * The message analytics service
     */
    private static AnalyticsService analyticsService;

    private static InsightsService insightsService;

    /**
     * The threading service
     */
    private static ThreadingService threadingService;

    /**
     * The global display details service
     */
    private static GlobalDisplayDetailsService globalDisplayDetailsService;

    /**
     * The global status service
     */
    private static GlobalStatusService globalStatusService;

    /**
     * The certificate service
     */
    private static CertificateService certificateService;

    /**
     * The WISPA service
     */
    private static WISPAService wispaService;

    /**
     * Regular expression to match 'special' messages that are used to pass on
     * custom system messages and should not be displayed to the user.
     *
     * These messages start with the text "*.accession.metaswitch.com:" (where
     * "*" can be any string of letters and digits).  One use of these
     * messages is to allow the client to stitch together a fragmented SMS that
     * has been split over several messages.
     */
    protected static final Pattern SPECIAL_MESSAGE_REGEX =
        Pattern.compile("([a-zA-Z0-9]+)\\.accession\\.metaswitch\\.com:(.*)",
                        Pattern.DOTALL);

    /**
     * The string used to identify correlator 'special' messages.  These are
     * received in response to every SMS that we send.  They contain in their
     * body an XMPP ID and an SMPP ID so that we can map the SMPP ID of errors
     * returned by the SMSC to the XMPP ID of the original SMS that was sent.
     */
    protected static final String CORRELATOR_ID = "correlator";

    /**
     * Regular expression to match correlator 'special' messages
     */
    protected static final Pattern CORRELATOR_REGEX =
        Pattern.compile("XMPP_ID=([^&]+)&SMPP_ID=([^&]+)");

    /**
     * Regular expression to match the SMPP ID in correlator 'special' messages
     */
    protected static final Pattern SMPP_ID_REGEX =
        Pattern.compile("id%3A([A-F0-9]+)\\+");

    /**
     * The string used as the XMPP ID in error messages from the SMSC to
     * indicate that we need to look for the SMPP ID in the error message body
     * to get the real message ID.
     */
    protected static final String FAKE_XMPP_ID = "fakexmpp_id";

    /**
     * The string used to identify fragment 'special' messages.  These are SMS
     * messages that are made up of separate parts split over multiple XMPP
     * messages.
     */
    protected static final String FRAGMENT_ID = "fragment";

    /**
     * Regular expression to match multi-part fragment 'special' messages.
     * These contain extra information to identify the fragment.
     */
    protected static final Pattern FRAGMENT_MULTIPART_REGEX =
        Pattern.compile("MessageID=([^&]+)&MessageParts=([^&]+)&ThisPart=([^&]+)&Fragment=(.*)$",
                        Pattern.DOTALL);

    /**
     * Regular expression to match single part fragment 'special' messages.
     * These are sent for SMSs that don't need to be split over multiple
     * messages but are still labelled by the server as type 'fragment' though
     * only contain a single message and no other fragment info.
     */
    protected static final Pattern FRAGMENT_SINGLEPART_REGEX =
        Pattern.compile("Fragment=(.*)$", Pattern.DOTALL);

    /**
     * The string used to identify group membership 'special' messages.  These
     * are group chat messages that are sent to the group when someone either
     * is invited to or chooses to permanently leave the group chat.
     */
    protected static final String GROUP_MEMBERSHIP_ID = "groupmembership";

    /**
     * The string used as the JID parameter that provides the user's JID in
     * group membership 'special' messages.
     */
    protected static final String GROUP_MEMBERSHIP_JID_PARM = "JID=";

    /**
     * The string used as the Action parameter that indicates whether the user
     * is joining or leaving the group in group membership 'special' messages.
     */
    protected static final String GROUP_MEMBERSHIP_ACTION_PARM = "&Action=";

    /**
     * Regular expression to match group membership 'special' messages
     */
    protected static final Pattern GROUP_MEMBERSHIP_REGEX =
        Pattern.compile("JID=([^&]+)&Action=(joined|left|created|banned)");

    /**
     * A simple enum for different group chat membership actions used in group
     * membership 'special' messages.
     */
    public enum GroupMembershipAction
    {
        created,
        joined,
        left,
        banned
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
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context)
    {
        JabberActivator.bundleContext = context;

        // disco items request does not receive a response for this class
        SmackConfiguration.addDisabledSmackClass("org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager");

        // Smack defaults to 5s timeout waiting for a reply. Lengthen that to 30s because AMS can be
        // slow when it's overloaded, and timing out will cause the client to try to reconnect -
        // adding even more traffic to AMS and exacerbating the problem.
        SmackConfiguration.setDefaultReplyTimeout(30000);

        // Configure XMPP traffic logger. Do this first so that we
        // have the logger as soon as possible
        XmppDebugger.initXmppDebugger(new SmackXmppLogger());

        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.JABBER);

        jabberProviderFactory = new ProtocolProviderFactoryJabberImpl();

         /*
         * Install the UriHandler prior to registering the factory service in
         * order to allow it to detect when the stored accounts are loaded
         * (because they may be asynchronously loaded).
         */
        uriHandlerImpl = new UriHandlerJabberImpl(jabberProviderFactory);

        //register the jabber account man.
        jabberPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    jabberProviderFactory,
                    hashtable);

        ConfigurationService cfg = getConfigurationService();
        if (cfg != null &&
            cfg.global().getBoolean("net.java.sip.communicator.impl.protocol.jabber.DEBUG", false))
        {
            // Enable the smack debugger
            System.setProperty("smack.debugEnabled", "true");
        }
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return ConfigurationService a currently valid implementation of the
     * configuration service.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService =
                ServiceUtils.getService(
                    bundleContext, ConfigurationService.class);
        }
        return configurationService;
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
     * @return a reference to the <tt>ProtocolProviderFactoryJabberImpl</tt>
     * instance that we have registered from this package.
     */
    public static ProtocolProviderFactoryJabberImpl getProtocolProviderFactory()
    {
        return jabberProviderFactory;
    }

    /**
     * Returns a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a MediaService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
//    public static MediaService getMediaService()
//    {
//        if(mediaService == null)
//        {
//            ServiceReference mediaServiceReference
//                = bundleContext.getServiceReference(
//                    MediaService.class.getName());
//
//            if (mediaServiceReference != null) {
//                mediaService = (MediaService)
//                    bundleContext.getService(mediaServiceReference);
//            }
//        }
//        return mediaService;
//    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
        jabberProviderFactory.stop();
        jabberPpFactoryServReg.unregister();

        if (uriHandlerImpl != null)
        {
            uriHandlerImpl.dispose();
            uriHandlerImpl = null;
        }

        configurationService = null;
        mediaService = null;
        networkAddressManagerService = null;
        credentialsService = null;
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
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        }
        return uiService;
    }

    /**
     * Returns a reference to the ResourceManagementService implementation
     * currently registered in the bundle context or <tt>null</tt> if no such
     * implementation was found.
     *
     * @return a reference to the ResourceManagementService implementation
     * currently registered in the bundle context or <tt>null</tt> if no such
     * implementation was found.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }

    /**
     * Returns a reference to a {@link MediaService} implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a reference to a {@link MediaService} implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     */
    public static MediaService getMediaService()
    {
        if(mediaService == null)
        {
            mediaService =
                ServiceUtils.getService(bundleContext, MediaService.class);
        }
        return mediaService;
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
            networkAddressManagerService =
                ServiceUtils.getService(
                    bundleContext, NetworkAddressManagerService.class);
        }
        return networkAddressManagerService;
    }

    /**
     * Returns a reference to a CredentialsStorageService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * CredentialsStorageService
     */
    public static CredentialsStorageService getCredentialsStorageService()
    {
        if(credentialsService == null)
        {
            credentialsService =
                ServiceUtils.getService(
                    bundleContext, CredentialsStorageService.class);
        }
        return credentialsService;
    }

    /**
     * Returns a reference to the PacketLoggingService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a PacketLoggingService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     */
    public static PacketLoggingService getPacketLogging()
    {
        if (packetLoggingService == null)
        {
            packetLoggingService =
                ServiceUtils.getService(
                    bundleContext, PacketLoggingService.class);
        }
        return packetLoggingService;
    }

    /**
     * Returns a reference to a VersionService implementation currently
     * registered in the bundle context or null if no such implementation
     * was found.
     *
     * @return a reference to a VersionService implementation currently
     * registered in the bundle context or null if no such implementation
     * was found.
     */
    public static VersionService getVersionService()
    {
        if(versionService == null)
        {
            versionService =
                ServiceUtils.getService(bundleContext, VersionService.class);
        }
        return versionService;
    }

    /**
     * Returns a reference to a MetaContactListService implementation currently
     * registered in the bundle context or null if no such implementation
     * was found.
     *
     * @return a reference to a MetaContactListService implementation currently
     * registered in the bundle context or null if no such implementation
     * was found.
     */
    public static MetaContactListService getMetaContactListService()
    {
        if(metaContactListService == null)
        {
            metaContactListService =
                ServiceUtils.getService(
                    bundleContext, MetaContactListService.class);
        }
        return metaContactListService;
    }

    /**
     * Returns a reference to a ContactSyncBarService implementation currently
     * registered in the bundle context or null if no such implementation
     * was found.
     *
     * @return a reference to a ContactSyncBarService implementation currently
     * registered in the bundle context or null if no such implementation
     * was found.
     */
    public static ContactSyncBarService getContactSyncBarService()
    {
        if(contactSyncBarService == null)
        {
            contactSyncBarService =
                ServiceUtils.getService(
                    bundleContext, ContactSyncBarService.class);
        }
        return contactSyncBarService;
    }

    /**
     * Returns a reference to a AuthorizationHandler implementation currently
     * registered in the bundle context or null if no such implementation
     * was found.
     *
     * @return a reference to a AuthorizationHandler implementation currently
     * registered in the bundle context or null if no such implementation
     * was found.
     */
    public static AuthorizationHandler getAuthorizationHandlerService()
    {
        if (authHandlerService == null)
        {
            authHandlerService = ServiceUtils.getService(
                                                    bundleContext,
                                                    AuthorizationHandler.class);
        }

        return authHandlerService;
    }

    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(accountManager == null)
        {
            accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);
        }
        return accountManager;
    }

    /**
     * Returns the <tt>PhoneNumberUtilsService</tt> obtained from the bundle context.
     * @return the <tt>PhoneNumberUtilsService</tt> obtained from the bundle context
     */
    public static PhoneNumberUtilsService getPhoneNumberUtils()
    {
        if(phoneNumberUtils == null)
        {
            phoneNumberUtils
                = ServiceUtils.getService(bundleContext, PhoneNumberUtilsService.class);
        }
        return phoneNumberUtils;
    }

    /**
     * Returns the <tt>MessageHistoryService</tt> obtained from the bundle context.
     *
     * @return the <tt>MessageHistoryService</tt> obtained from the bundle context.
     */
    public static MessageHistoryService getMessageHistoryService()
    {
        if(messageHistoryService == null)
        {
            messageHistoryService =
                ServiceUtils.getService(bundleContext, MessageHistoryService.class);
        }

        return messageHistoryService;
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context.
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
     * Returns the <tt>AnalyticsService</tt> obtained from the bundle context
     *
     * @return the <tt>AnalyticsService</tt> obtained from the bundle context
     */
    public static AnalyticsService getAnalyticsService()
    {
        if (analyticsService == null)
        {
            analyticsService =
                 ServiceUtils.getService(bundleContext, AnalyticsService.class);
        }
        return analyticsService;
    }

    public static InsightsService getInsightsService()
    {
        if (insightsService == null)
        {
            insightsService =
                 ServiceUtils.getService(bundleContext, InsightsService.class);
        }
        return insightsService;
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
     * Returns the <tt>ConferenceService</tt> obtained from the bundle context.
     *
     * @return the <tt>ConferenceService</tt> obtained from the bundle context
     */
    public static ConferenceService getConferenceService()
    {
        return ServiceUtils.getConferenceService(bundleContext);
    }

    /**
     * Returns the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context
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
     * Returns the <tt>GlobalStatusService</tt> obtained from the bundle context
     *
     * @return the <tt>GlobalStatusService</tt> obtained from the bundle context
     */
    public static GlobalStatusService getGlobalStatusService()
    {
        if (globalStatusService == null)
        {
            globalStatusService =
                 ServiceUtils.getService(bundleContext, GlobalStatusService.class);
        }
        return globalStatusService;
    }

    /**
     * Returns the <tt>CertificateService</tt> obtained from the bundle context
     *
     * @return the <tt>CertificateService</tt> obtained from the bundle context
     */
    public static CertificateService getCertificateService()
    {
        if (certificateService == null)
        {
            certificateService =
                    ServiceUtils.getService(bundleContext, CertificateService.class);
        }
        return certificateService;
    }

    /**
     * Returns the <tt>WISPAService</tt> obtained from the bundle context
     *
     * @return the <tt>WISPAService</tt> obtained from the bundle context
     */
    public static WISPAService getWispaService()
    {
        if (wispaService == null)
        {
            wispaService =
                    ServiceUtils.getService(bundleContext, WISPAService.class);
        }
        return wispaService;
    }
}
