/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.awt.desktop.QuitResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.impl.gui.main.AbstractMainFrame;
import net.java.sip.communicator.impl.gui.main.CallPullButton;
import net.java.sip.communicator.impl.gui.main.call.CallManager;
import net.java.sip.communicator.impl.gui.main.contactlist.AddContactDialog;
import net.java.sip.communicator.impl.gui.main.contactlist.TreeContactList;
import net.java.sip.communicator.impl.gui.main.presence.GlobalStatusServiceImpl;
import net.java.sip.communicator.impl.gui.main.presence.avatar.SelectAvatarMenu;
import net.java.sip.communicator.impl.gui.main.presence.avatar.SelectAvatarService;
import net.java.sip.communicator.impl.gui.utils.ContactChooserServiceImpl;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.browserlauncher.BrowserLauncherService;
import net.java.sip.communicator.service.browserpanel.BrowserPanelService;
import net.java.sip.communicator.service.callhistory.CallHistoryService;
import net.java.sip.communicator.service.calljump.CallJumpService;
import net.java.sip.communicator.service.commportal.CPCos;
import net.java.sip.communicator.service.commportal.CPCosGetterCallback;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactsource.ContactSourceService;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.diagnostics.DiagnosticsService;
import net.java.sip.communicator.service.diagnostics.DiagnosticsServiceRegistrar;
import net.java.sip.communicator.service.diagnostics.StateDumper;
import net.java.sip.communicator.service.globaldisplaydetails.GlobalDisplayDetailsService;
import net.java.sip.communicator.service.gui.AlertUIService;
import net.java.sip.communicator.service.gui.ContactChooserService;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.PluginComponent;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.headsetmanager.HeadsetManagerService;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.insights.InsightsService;
import net.java.sip.communicator.service.managecontact.ManageContactService;
import net.java.sip.communicator.service.managecontact.ManageContactWindow;
import net.java.sip.communicator.service.managecontact.ViewContactService;
import net.java.sip.communicator.service.metahistory.MetaHistoryService;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.phonenumberutils.PhoneNumberUtilsService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.AuthorizationHandler;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;
import net.java.sip.communicator.service.replacement.ChatPartReplacementService;
import net.java.sip.communicator.service.replacement.TabbedImagesSelectorService;
import net.java.sip.communicator.service.reset.ResetService;
import net.java.sip.communicator.service.shutdown.ShutdownService;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.audionotifier.AudioNotifierService;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.fileaccess.FileAccessService;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.packetlogging.PacketLoggingService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;

/**
 * The GUI Activator class.
 *
 * @author Yana Stamcheva
 */
public class GuiActivator implements BundleActivator, StateDumper
{
    /**
     * The <tt>Logger</tt> used by the <tt>GuiActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(GuiActivator.class);

    private static AbstractUIServiceImpl uiServiceImpl = null;

    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private static ConfigurationService configService;

    private static MetaHistoryService metaHistoryService;

    private static MetaContactListService metaCListService;

    private static CallHistoryService callHistoryService;

    private static CallJumpService callJumpService;

    private static AudioNotifierService audioNotifierService;

    private static BrowserLauncherService browserLauncherService;

    private static ResourceManagementService resourcesService;

    private static FileAccessService fileAccessService;

    private static MediaService mediaService;

    private static TabbedImagesSelectorService iconInserterService;

    private static ManageContactService manageContactService;

    private static AccountManager accountManager;

    private static NotificationService notificationService;

    private static AuthorizationHandler authHandlerService;

    private static SecurityAuthority securityAuthority;

    private static AnalyticsService analyticsService;

    private static InsightsService insightsService;

    private static ResetService resetService;

    private static Boolean enterDialsNumber = null;

    private static GlobalDisplayDetailsService globalDisplayDetailsService;

    private static PhoneNumberUtilsService phoneNumberUtils;

    private static HeadsetManagerService headsetManager;

    private static CredentialsStorageService credsService;

    private static ViewContactService viewContactService;

    private static ClassOfServiceService cosService;

    private static MessageHistoryService messageHistoryService;

    private static ThreadingService threadingService;

    private static DiagnosticsService diagsService;

    private static PacketLoggingService packetLoggingService;

    private static BrowserPanelService browserPanelService;

    private static CommPortalService commPortalService;

    private static WISPAService wispaService;

    private static final Map<Object, ProtocolProviderFactory>
        providerFactoriesMap = new Hashtable<>();

    private static final Map<String, ChatPartReplacementService>
        replacementSourcesMap = new Hashtable<>();

    /**
     * Indicates if this bundle has been started.
     */
    public static boolean isStarted = false;

    /**
     * The image loader service
     */
    private static ImageLoaderService imageLoaderService;

    /**
     * The global status service implementation
     */
    private static GlobalStatusServiceImpl globalStatusServiceImpl;

    /**
     * Called when this bundle is started.
     *
     * @param bContext The execution context of the bundle being started.
     */
    public void start(BundleContext bContext)
    {
        isStarted = true;
        GuiActivator.bundleContext = bContext;

        try
        {
            logger.info("GlobalStatus Service ...[REGISTERED]");

            globalStatusServiceImpl = new GlobalStatusServiceImpl();
            bundleContext.registerService(GlobalStatusService.class.getName(),
                                          globalStatusServiceImpl,
                                          null);

            // Registers an implementation of the AlertUIService.
            bundleContext.registerService(AlertUIService.class.getName(),
                                          new AlertUIServiceImpl(),
                                          null);

            // Registers an implementation of the Contact chooser service.
            bundleContext.registerService(ContactChooserService.class.getName(),
                                          new ContactChooserServiceImpl(),
                                          null);

            GuiActivator.bundleContext.registerService(SelectAvatarService.class.getName(),
                                                       new SelectAvatarMenu(),
                                                       null);

            Hashtable<String, String> containerFilter = new Hashtable<>();
            containerFilter.put(Container.CONTAINER_ID, Container.CONTAINER_MAIN_WINDOW.getID());

            GuiActivator.bundleContext.registerService(PluginComponent.class.getName(),
                                                       new CallPullButton(),
                                                       containerFilter);

            // Make sure we have the CoS before checking the type of user the
            // subscriber is (standalone meeting user or not). This is to ensure
            // that the value of ConfigurationUtils.isStandaloneMeetingUser()
            // accurately reflects the latest CoS settings.
            ClassOfServiceService cos = getCosService();
            cos.getClassOfService(new CPCosGetterCallback()
            {
                @Override
                public void onCosReceived(CPCos classOfService)
                {
                    // UI should only be created once - unsubscribe from future
                    // CoS updates. Changes in cos that require a change in
                    // UI are handled by the UIService
                    cos.unregisterCallback(this);

                    // Create the ui service
                    if (ConfigurationUtils.isStandaloneMeetingUser())
                    {
                        logger.debug("User is a standalone meeting user. "
                            + "Starting up the standalone meeting UI");
                        uiServiceImpl = new StandaloneUIServiceImpl();
                    }
                    else
                    {
                        logger.debug("User is not a standalone meeting user. "
                            + "Starting up the standard Accession UI");
                        uiServiceImpl = new StandardUIServiceImpl();
                    }

                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            uiServiceImpl.loadApplicationGui();

                            GuiActivator.getConfigurationService().user()
                                        .addPropertyChangeListener(uiServiceImpl);

                            bundleContext.addServiceListener(uiServiceImpl);

                            // don't block the ui thread
                            // with registering services, as they are executed
                            // in the same thread as registering
                            new Thread(new Runnable()
                            {
                                public void run()
                                {
                                    logger.info("UI Service...[  STARTED ]");

                                    bundleContext.registerService(
                                        UIService.class.getName(),
                                        uiServiceImpl,
                                        null);

                                    logger.info("UI Service ...[REGISTERED]");

                                    // UIServiceImpl also implements ShutdownService.
                                    bundleContext.registerService(
                                        ShutdownService.class.getName(),
                                        uiServiceImpl,
                                        null);

                                    if (OSUtils.isMac())
                                    {
                                        registerMacOSQuitHandler();
                                    }
                                }
                             }).start();
                         }
                    });
                }

            });

            DiagnosticsServiceRegistrar.registerStateDumper(this, GuiActivator.bundleContext);

            // Request to be notified once HeadsetManagerService registered to register a listener.
            ServiceUtils.getService(bundleContext,
                    HeadsetManagerService.class,
                    new ServiceUtils.ServiceCallback<>()
                    {
                        @Override
                        public void onServiceRegistered(HeadsetManagerService service)
                        {
                            headsetManager = service;

                            // Register HeadsetManagerListener that changes the state of a call, as a result of a
                            // headset button press, or change in the headset state.
                            headsetManager.addHeadsetManagerListener(new CallManager.CallHeadsetManagerListener());
                        }
                    });

            logger.entry();
        }
        finally
        {
            logger.exit();
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bContext The execution context of the bundle being stopped.
     */
    public void stop(BundleContext bContext)
    {
        logger.info("UI Service ...[STOPPED]");
        isStarted = false;
        globalStatusServiceImpl.stop();

        GuiActivator.getConfigurationService()
            .user().removePropertyChangeListener(uiServiceImpl);

        bContext.removeServiceListener(uiServiceImpl);
    }

    /**
     * Registers the quit handler for macOS to perform graceful shutdown
     * of the app following an external shutdown request (i.e. via Sparkle for
     * system updates/when a user attempts to skip a mandatory update).
     */
    private static void registerMacOSQuitHandler()
    {
        Desktop.getDesktop().setQuitHandler((quitEvent, quitResponse)
                                                    -> quitMacSafely(quitResponse));
        logger.info("MacOS Quit Handler registered.");
    }

    /**
     * Helper method for shutting down app on macOS safely when receiving
     * an external call to shut down (i.e. a "terminate" call from Sparkle).
     */
    private static void quitMacSafely(QuitResponse quitResponse)
    {
        logger.info("Mac Quit request received. Begin shutdown: Logout? false" +
                    ", Mac QuitResponse: " + quitResponse);
        getUIService()
                .beginShutdown(false, false, quitResponse, true);
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
            serRefs
                = bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("LoginManager : " + e);
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
     * Returns a list of all known providers that support the given
     * OperationSet.
     *
     * @param operationSet    The operation set for which we're looking for
     * providers
     * @return a list of all known providers that support the given operation
     * set
     */
    public static List<ProtocolProviderService> getProviders(
        Class<? extends OperationSet> operationSet)
    {
        List<ProtocolProviderService> opSetProviders =
                new ArrayList<>();

        Map<Object, ProtocolProviderFactory> protocolProviderFactories =
            getProtocolProviderFactories();
        for (Object key : protocolProviderFactories.keySet())
        {
            ProtocolProviderFactory factory = protocolProviderFactories.get(key);
            for (AccountID accountID : factory.getRegisteredAccounts())
            {
                ServiceReference<?> serRef = factory.getProviderForAccount(
                                                                    accountID);
                ProtocolProviderService provider =
                      (ProtocolProviderService)bundleContext.getService(serRef);

                if (provider.getOperationSet(operationSet) != null)
                {
                    opSetProviders.add(provider);
                }
            }
        }

        return opSetProviders;
    }

    /**
     * Returns a list of all protocol providers that are registered with the
     * OSGI framework.  This differs from <tt>getProviders</tt> which returns
     * the providers whose accounts are registered with the underlying server.
     * <p/>
     * This method should be used instead of <tt>getProviders</tt> if you are
     * trying to get a provider that may not yet have been registered; and will
     * be using a ServiceListener to listen for it to be registered if so.
     *
     * @param opSetClass The (optional) class that the provider should implement
     * @return a list of the providers that is registered with OSGI
     */
    public static List<ProtocolProviderService> getRegisteredProviders(
                                       Class<? extends OperationSet> opSetClass)
    {
        List<ProtocolProviderService> providers =
                new ArrayList<>();
        try
        {
            String name = ProtocolProviderService.class.getName();
            ServiceReference<?>[] references =
                              bundleContext.getAllServiceReferences(name, null);

            if (references != null)
            {
                for (ServiceReference<?> ref : references)
                {
                    ProtocolProviderService service =
                             (ProtocolProviderService)bundleContext.getService(ref);

                    if (opSetClass == null ||
                        service.getOperationSet(opSetClass) != null)
                    {
                        providers.add(service);
                    }
                }
            }
        }
        catch (InvalidSyntaxException e)
        {
            // Shouldn't ever happen as we aren't providing a filter
            logger.error("Can't get the registered providers", e);
        }

        return providers;
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
     * Returns the <tt>MetaHistoryService</tt> obtained from the bundle
     * context.
     * @return the <tt>MetaHistoryService</tt> obtained from the bundle
     * context
     */
    public static MetaHistoryService getMetaHistoryService()
    {
        if (metaHistoryService == null)
        {
            metaHistoryService
                = ServiceUtils.getService(
                        bundleContext,
                        MetaHistoryService.class);
        }
        return metaHistoryService;
    }

    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     * context
     */
    public static MetaContactListService getContactListService()
    {
        if (metaCListService == null)
        {
            metaCListService
                = ServiceUtils.getService(
                        bundleContext,
                        MetaContactListService.class);
        }
        return metaCListService;
    }

    /**
     * Returns the <tt>CallHistoryService</tt> obtained from the bundle
     * context.
     * @return the <tt>CallHistoryService</tt> obtained from the bundle
     * context
     */
    public static CallHistoryService getCallHistoryService()
    {
        if (callHistoryService == null)
        {
            callHistoryService
                = ServiceUtils.getService(
                        bundleContext,
                        CallHistoryService.class);
        }
        return callHistoryService;
    }

    /**
     * Returns the <tt>CallJumpService</tt> obtained from the bundle
     * context.
     * @return the <tt>CallJumpService</tt> obtained from the bundle
     * context
     */
    public static CallJumpService getCallJumpService()
    {
        if (callJumpService == null)
        {
            callJumpService
                    = ServiceUtils.getService(
                    bundleContext,
                    CallJumpService.class);
        }
        return callJumpService;
    }

    /**
     * Returns the <tt>AudioNotifierService</tt> obtained from the bundle
     * context.
     * @return the <tt>AudioNotifierService</tt> obtained from the bundle
     * context
     */
    public static AudioNotifierService getAudioNotifier()
    {
        if (audioNotifierService == null)
        {
            audioNotifierService
                = ServiceUtils.getService(
                        bundleContext,
                        AudioNotifierService.class);
        }
        return audioNotifierService;
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            browserLauncherService
                = ServiceUtils.getService(
                        bundleContext,
                        BrowserLauncherService.class);
        }
        return browserLauncherService;
    }

    /**
     * @return the <tt>GlobalStatusService</tt> obtained from the bundle
     * context
     */
    public static GlobalStatusServiceImpl getGlobalStatusService()
    {
        return globalStatusServiceImpl;
    }

    /**
     * Returns the current implementation of the <tt>UIService</tt>.
     * @return the current implementation of the <tt>UIService</tt>
     */
    public static AbstractUIServiceImpl getUIService()
    {
        return uiServiceImpl;
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
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }

    /**
     * Returns an instance of the <tt>MediaService</tt> obtained from the
     * bundle context.
     * @return an instance of the <tt>MediaService</tt> obtained from the
     * bundle context
     */
    public static MediaService getMediaService()
    {
        if (mediaService == null)
        {
            mediaService
                = ServiceUtils.getService(bundleContext, MediaService.class);
        }
        return mediaService;
    }

    /**
     * Returns an instance of the <tt>PacketLoggingService</tt> obtained from the
     * bundle context.
     * @return an instance of the <tt>PacketLoggingService</tt> obtained from the
     * bundle context
     */
    public static PacketLoggingService getPacketLoggingService()
    {
        if (packetLoggingService == null)
        {
            packetLoggingService =
                ServiceUtils.getService(bundleContext, PacketLoggingService.class);
        }

        return packetLoggingService;
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
     * Returns a list of all registered contact sources.
     * @return a list of all registered contact sources
     */
    public static List<ContactSourceService> getContactSources()
    {
        List<ContactSourceService> contactSources =
                new ArrayList<>();

        ServiceReference<?>[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs =
                bundleContext.getServiceReferences(
                    ContactSourceService.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("GuiActivator : " + e);
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
     * Returns all <tt>ChatPartReplacementService</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ChatPartReplacementService</tt> implementation obtained from the
     *         bundle context
     */
    public static Map<String, ChatPartReplacementService> getReplacementSources()
    {
        ServiceReference<?>[] serRefs = null;
        try
        {
            // get all registered sources
            serRefs
                = bundleContext.getServiceReferences(ChatPartReplacementService.class
                    .getName(), null);

        }
        catch (InvalidSyntaxException e)
        {
            logger.error("Error : " + e);
        }

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                ChatPartReplacementService replacementSources =
                        (ChatPartReplacementService)bundleContext.getService(serRef);

                replacementSourcesMap.put(
                        (String)serRef.getProperty(ChatPartReplacementService.SOURCE_NAME),
                        replacementSources);
            }
        }
        return replacementSourcesMap;
    }

    /**
     * Returns the <tt>SmiliesReplacementService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>SmiliesReplacementService</tt> implementation obtained
     * from the bundle context
     */
    public static TabbedImagesSelectorService getSmiliesReplacementSource()
    {
        if (iconInserterService == null)
        {
            iconInserterService
                = ServiceUtils.getService(bundleContext,
                    TabbedImagesSelectorService.class);
        }
        return iconInserterService;
    }

    /**
     * Returns the <tt>AddContactService</tt> obtained from the bundle context.
     *
     * @return the <tt>AddContactService</tt> implementation obtained from the
     * bundle context.
     */
    public static ManageContactService getManageContactService()
    {
        if (manageContactService == null)
        {
            manageContactService = ServiceUtils.getService(
                bundleContext, ManageContactService.class);
        }

        return manageContactService;
    }

    /**
     * Returns an <tt>AddContactWindow</tt>, either by using an
     * <tt>ManageContactService</tt> to create one (is such a service is
     * available) or by using the default Jitsi implementation.
     *
     * @param contact The source contact containing details that the add window
     * will be populated with, or null if this window is for a new contact.
     * @return a new <tt>AddContactWindow</tt>.  Depending on the
     * AddContactService behaviour, this may be null.
     */
    public static ManageContactWindow getAddContactWindow(SourceContact contact)
    {
        ManageContactWindow window;
        ManageContactService service = getManageContactService();
        AbstractMainFrame mainFrame = getUIService().getMainFrame();

        if (service != null)
        {
            // There is an AddContactService; use it to create a window.
            window = service.createAddContactWindow(mainFrame, contact);
        }
        else
        {
            // Create the default version of the window.
            window = new AddContactDialog(mainFrame);
        }

        return window;
    }

    /**
     * Returns an <tt>EditContactWindow</tt>, either by using an
     * <tt>ManageContactService</tt> to create one or using the default
     * window if it has no metacontact info.
     *
     * @param metaContact the metacontact that the new contact will be edited
     * @return a new <tt>EditContactWindow</tt>.
     */
    public static ManageContactWindow getEditContactWindow(MetaContact metaContact)
    {
        ManageContactService service = getManageContactService();
        AbstractMainFrame mainFrame = getUIService().getMainFrame();

        if (service != null)
        {
            // There is a ManageContactService; use it to create a window.
            return service.createEditContactWindow(mainFrame, metaContact);
        }
        else
        {
            logger.warn("No ContactManagerService, cannot create EditContactWindow");
        }

        return null;
    }

    /**
     * Returns the <tt>SecurityAuthority</tt> implementation registered to
     * handle security authority events.
     *
     * @return the <tt>SecurityAuthority</tt> implementation obtained
     * from the bundle context
     */
    public static SecurityAuthority getSecurityAuthority()
    {
        if (securityAuthority == null)
        {
            securityAuthority
                = ServiceUtils.getService(bundleContext,
                    SecurityAuthority.class);
        }
        return securityAuthority;
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        if(notificationService == null)
        {
            // Get the notification service implementation
            ServiceReference<?> notifReference = bundleContext
                .getServiceReference(NotificationService.class.getName());

            notificationService = (NotificationService) bundleContext
                .getService(notifReference);
        }

        return notificationService;
    }

    /**
     * Returns a reference to an <tt>AuthorizationHandler</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * <tt>AuthorizationHandler</tt>.
     */
    public static AuthorizationHandler getAuthorizationHandlerService()
    {
        if (authHandlerService == null)
        {
            // We haven't already got an implementation of the
            // AuthorizationHandler service, so find out if one has been
            // registered.
            authHandlerService = ServiceUtils.getService(
                                                    bundleContext,
                                                    AuthorizationHandler.class);
        }

        return authHandlerService;
    }

    /**
     * Returns the <tt>SecurityAuthority</tt> implementation registered to
     * handle security authority events.
     *
     * @param protocolName protocol name
     * @return the <tt>SecurityAuthority</tt> implementation obtained
     * from the bundle context
     */
    public static SecurityAuthority getSecurityAuthority(String protocolName)
    {
        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + protocolName + ")";

        SecurityAuthority securityAuthority = null;
        try
        {
            ServiceReference<?>[] serRefs
                = bundleContext.getServiceReferences(
                    SecurityAuthority.class.getName(), osgiFilter);

            if (serRefs != null && serRefs.length > 0)
                securityAuthority
                    = (SecurityAuthority) bundleContext
                        .getService(serRefs[0]);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("GuiActivator : " + ex);
        }

        return securityAuthority;
    }

    /**
     * @return the component used to show the contact list in the main frame.
     */
    public static TreeContactList getMainFrameContactList()
    {
        TreeContactList contactList = null;

        if (getUIService() != null && getUIService().getMainFrame()!= null &&
            getUIService().getMainFrame().getContactListPanel() != null)
        {
            contactList = getUIService().getMainFrame().
                getContactListPanel().getContactList();
        }

        return contactList;
    }

    /**
     * Returns whether pressing 'enter' when typing in the search bar
     * should start a call to that number.
     *
     * @return whether pressing 'enter' in the search bar should call the
     * current number
     */
    public static boolean enterDialsNumber()
    {
        if (enterDialsNumber == null)
        {
            enterDialsNumber = getConfigurationService().user().getBoolean(
                "net.java.sip.communicator.impl.gui.ENTER_CALLS_NUMBER", false);
        }

        return enterDialsNumber;
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
     * Returns the <tt>HeadsetManagerService</tt> obtained from the bundle
     * context.
     * Asynchronous initialization requested in {@link #start(BundleContext)}.
     *
     * @return the <tt>HeadsetManagerService</tt> implementation obtained
     * from the bundle context.
     */
    public static HeadsetManagerService getHeadsetManager()
    {
        return headsetManager;
    }

    /**
     * Returns the <tt>CredentialsService</tt> obtained from the bundle
     * context
     *
     * @return the <tt>CredentialsService</tt> obtained from the bundle
     * context
     */
    public static CredentialsStorageService getCredentialsService()
    {
        if (credsService == null)
        {
            credsService = ServiceUtils.getService(bundleContext,
                                              CredentialsStorageService.class);
        }

        return credsService;
    }

    /**
     * @return the <tt>ViewContactService</tt> obtained from the bundle context
     */
    public static ViewContactService getViewContactService()
    {
        if (viewContactService == null)
        {
            viewContactService = ServiceUtils.getService(bundleContext,
                                                      ViewContactService.class);
        }

        return viewContactService;
    }

    /**
     * Gets the reset service.
     */
    public static ResetService getResetService()
    {
        if (resetService == null)
            resetService = ServiceUtils.getService(bundleContext,
                                                   ResetService.class);

        return resetService;
    }

    /**
     * @return the <tt>ClassOfServiceService</tt> obtained from the bundle context
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
     * @return the <tt>MessageHistoryService</tt> obtained from the bundle context
     */
    public static MessageHistoryService getMessageHistoryService()
    {
        if (messageHistoryService == null)
        {
            messageHistoryService = ServiceUtils.getService(bundleContext,
                                                   MessageHistoryService.class);
        }

        return messageHistoryService;
    }

    /**
     * @return the <tt>ImageLoaderService</tt> obtained from the bundle context
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
     * @return a reference to the registered threading service
     */
    public static ThreadingService getThreadingService()
    {
        if (threadingService == null)
            threadingService = ServiceUtils.getService(bundleContext,
                                                       ThreadingService.class);

        return threadingService;
    }

    /**
     * @return a reference to the registered conference service
     */
    public static ConferenceService getConferenceService()
    {
        return ServiceUtils.getConferenceService(bundleContext);
    }

    /**
     * @return a reference to the registered diagnostics service
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
     * @return a reference to the registered browser panel service
     */
    public static BrowserPanelService getBrowserPanelService()
    {
        if (browserPanelService == null)
        {
            browserPanelService = ServiceUtils.getService(bundleContext,
                                                     BrowserPanelService.class);
        }

        return browserPanelService;
    }

    /**
     * @return a reference to the CommPortal service
     */
    public static CommPortalService getCommPortalService()
    {
        if (commPortalService == null)
        {
            commPortalService = ServiceUtils.getService(
                    bundleContext,
                    CommPortalService.class);

            if (commPortalService == null)
            {
                logger.warn("CommPortalService is unexpectedly null");
            }
        }

        return commPortalService;
    }

    /**
     * @return a reference to the WISPA service.
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

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * with.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    @Override
    public String getStateDumpName()
    {
        return "GuiActivator";
    }

    @Override
    public String getState()
    {
        StringBuilder builder = new StringBuilder();

        Collection<Call> calls = CallManager.getInProgressCalls();

        // We can log call info without hashing, except display name.
        builder.append("Calls\n=====\n");
        for (Call call : calls)
        {
            builder.append("Call ID             ").append(call.getCallID()).append("\n")
                   .append("StartTime           ").append(call.getUserPerceivedCallStartTime()).append("\n")
                   .append("CallState           ").append(call.getCallState()).append("\n")
                   .append("ConferenceFocus     ").append(call.isConferenceFocus()).append("\n");

            builder.append("Peers\n-----\n");

            for (Iterator<? extends CallPeer> peerIterator = call.getCallPeers(); peerIterator.hasNext();)
            {
                CallPeer peer = peerIterator.next();
                builder.append("  Name              ").append(logHasher(peer.getDisplayName())).append("\n")
                       .append("  Address           ").append(peer.getAddress()).append("\n")
                       .append("  State             ").append(peer.getState()).append("\n")
                       .append("  -----").append("\n");
            }

            builder.append("=====\n");
        }
        return builder.toString();
    }
}
