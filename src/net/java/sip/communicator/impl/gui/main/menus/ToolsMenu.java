/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.event.PluginComponentEvent;
import net.java.sip.communicator.impl.gui.event.PluginComponentListener;
import net.java.sip.communicator.impl.gui.main.ServiceType;
import net.java.sip.communicator.impl.gui.main.UrlServiceTools;
import net.java.sip.communicator.impl.gui.main.callpark.CallParkWindow;
import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.ResMenuItem;
import net.java.sip.communicator.plugin.desktoputil.SIPCommMenu;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.commportal.CPCos;
import net.java.sip.communicator.service.commportal.CPCosGetterCallback;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.PluginComponent;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetCallPark;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkListener;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbit;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbitState;
import net.java.sip.communicator.service.protocol.OperationSetVideoBridge;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.AccountUtils;
import net.java.sip.communicator.util.skin.Skinnable;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ImageIconFuture;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;

/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
public class ToolsMenu
    extends SIPCommMenu
    implements  ActionListener,
                PluginComponentListener,
                ServiceListener,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Local logger.
     */
    private final Logger logger = Logger.getLogger(ToolsMenu.class);

    /**
     * Property to disable auto answer menu.
     */
    private static final String AUTO_ANSWER_MENU_DISABLED_PROP =
        "net.java.sip.communicator.impl.gui.main.menus.AUTO_ANSWER_MENU_DISABLED";

    /**
     * Property to disable the conference menu item.
     */
    private static final String SHOW_CONFERENCE_BUTTON_PROP =
        "net.java.sip.communicator.impl.gui.main.call.SHOW_CONFERENCE_BUTTON";

    /**
     * The configuration service
     */
    private static final ConfigurationService configService =
                                         GuiActivator.getConfigurationService();

    /**
     * The plugin container that this menu item is registered for.
     */
    private final Container container;

   /**
    * Conference call menu item.
    */
    private JMenuItem conferenceMenuItem;

    /**
     * Video bridge conference call menu. In the case of more than one account.
     */
    private JMenuItem videoBridgeMenuItem;

    /**
     * Call park window menu item
     */
    private JMenuItem callParkMenuItem;

    /**
    * Preferences menu item.
    */
    JMenuItem configMenuItem;

    /**
     * The <tt>SwingWorker</tt> creating the video bridge menu item depending
     * on the number of <tt>ProtocolProviderService</tt>-s supporting the video
     * bridge functionality.
     */
    private SwingWorker initVideoBridgeMenuWorker;

    /**
     * The menu listener that would update the video bridge menu item every
     * time the user clicks on the Tools menu.
     */
    private MenuListener videoBridgeMenuListener;

    /**
     * Indicates if this menu is shown for the chat window or the contact list
     * window.
     */
    private boolean isChatMenu;

    /**
     * Indicates whether Call Park is enabled and available for this account.
     * Initialized to 'false' so that we don't display any call park UI until
     * we know there is an OperationSet to control it.
     */
    private boolean callParkConfigured = false;

    /**
     * Whether the client is allowed to make calls (set in the CoS) and VoIP is
     * enabled by the user in options.  If so, we display call history contacts.
     * Assume VoIP is active until we have received the CoS and checked config.
     */
    private boolean voipEnabled = true;

    /**
     * The color chooser dialog
     */
    private ColorChooserDialog colorChooserDialog = new ColorChooserDialog();

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     */
    public ToolsMenu()
    {
        this(false);
    }

    /**
     * Creates an instance of <tt>FileMenu</tt>, by specifying if this menu
     * would be shown for a chat window.
     *
     * @param isChatMenu indicates if this menu would be shown for a chat
     * window
     */
    public ToolsMenu(boolean isChatMenu)
    {
        logger.debug("Create Tools menu " + isChatMenu);
        this.isChatMenu = isChatMenu;

        container = isChatMenu ? Container.CONTAINER_CHAT_TOOLS_MENU :
                                 Container.CONTAINER_TOOLS_MENU;

        ResourceManagementService r = GuiActivator.getResources();

        setText(r.getI18NString("service.gui.TOOLS"));
        setMnemonic(r.getI18nMnemonic("service.gui.TOOLS"));

        registerMenuItems();

        initPluginComponents();
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference<?>[] serRefs = null;

        String osgiFilter = "(" + Container.CONTAINER_ID
                                                + "=" + container.getID() + ")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                final PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRef);
                logger.debug("Adding component to " +
                                   component.getContainer() + ", " + component);

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        add((Component) component.getComponent());
                    }
                });
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        String itemName = menuItem.getName();

        logger.user(itemName + " clicked in the Tools menu");

        if (itemName.equalsIgnoreCase("config"))
        {
            configActionPerformed();
        }
        else if (itemName.equals("conference"))
        {
            ResourceManagementService r = GuiActivator.getResources();

            new ErrorDialog(
                    null,
                    r.getI18NString("service.gui.WARNING"),
                    r.getI18NString(
                            "service.gui.NO_ONLINE_CONFERENCING_ACCOUNT"))
                .showDialog();
        }
        else if (itemName.equals("callPark"))
        {
            CallParkWindow.showCallParkWindow();
        }
        else if (itemName.equals("personalise"))
        {
            colorChooserDialog.showDialog();
        }
        else
        {
            // See if this click belongs to one of the URL services
            for (ServiceType serviceType : ServiceType.values())
            {
                logger.debug("Check ServiceType " + serviceType.getConfigName());
                if (itemName.equals(serviceType.getConfigName()))
                {
                    serviceType.launchServiceFromName(menuItem.getText());
                }
            }
        }
    }

    /**
     * Shows the configuration window.
     */
    void configActionPerformed()
    {
        GuiActivator.getUIService()
            .getConfigurationContainer().setVisible(true);
    }

    /**
     * Adds the plugin component contained in the event to this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        final PluginComponent c = event.getPluginComponent();
        logger.debug("Plugin component added. Container: " +
                                                   c.getContainer() + ", " + c);

        if(c.getContainer().equals(container))
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    add((Component) c.getComponent());
                }
            });

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Indicates that a plugin component has been removed. Removes it from this
     * container if it is contained in it.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        final PluginComponent c = event.getPluginComponent();
        logger.debug("Plugin component removed. Container: " +
                                                   c.getContainer() + ", " + c);

        if(c.getContainer().equals(container))
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    remove((Component) c.getComponent());
                }
            });
        }
    }

    /**
     * Registers all menu items.
     */
    private void registerMenuItems()
    {
        // Track whether we've added any items to the menu so we know whether
        // to add a separator - it doesn't make sense to add a separator at the
        // top of the list.
        boolean addedItem = false;

        // We only add the options button if the property SHOW_OPTIONS_WINDOW
        // specifies so or if it's not set.
        if (configService.global().getBoolean(ConfigurationFrame.SHOW_OPTIONS_WINDOW_PROPERTY,
                                     true))
        {
            UIService uiService = GuiActivator.getUIService();

            if ((uiService == null)
                    || !uiService.useMacOSXScreenMenuBar()
                    || !registerConfigMenuItemMacOSX())
            {
                logger.debug("Register Options menu (non-OS-X)");
                registerConfigMenuItemNonMacOSX();
                addedItem = true;
            }
        }

        // Add a tools menu option for personalising the app if this is an
        // internal branding, or is QA mode.
        // This option is disabled for standalone meeting users (PRD13303)
        // as configuring the colour is not supported.
        if (!isChatMenu && !ConfigurationUtils.useNativeTheme() &&
            !ConfigurationUtils.isStandaloneMeetingUser())
        {
            String cpURL = configService.user().getString("net.java.sip.communicator.impl.commportal.URL");
            boolean isQAMode = configService.global().getBoolean("net.java.sip.communicator.QA_MODE", false);
            if ((cpURL != null && cpURL.contains("[indeterminate link]")) || isQAMode)
            {
                JMenuItem item = new ResMenuItem("service.gui.PERSONALIZE");
                item.addActionListener(this);
                item.setName("personalise");
                add(item);
                addedItem = true;
            }
        }

        ResourceManagementService r = GuiActivator.getResources();

        // Only create the conference menu item if the SHOW_CONFERENCE_BUTTON
        // config option is true.
        if(configService.global().getBoolean(SHOW_CONFERENCE_BUTTON_PROP, true))
        {
            conferenceMenuItem
            = new ResMenuItem("service.gui.CREATE_CONFERENCE_CALL");
            conferenceMenuItem.setName("conference");
            conferenceMenuItem.addActionListener(this);
            add(conferenceMenuItem);
            addedItem = true;
        }

        // Add a service listener in order to be notified when a new protocol
        // provider is added or removed and the list should be refreshed.
        GuiActivator.bundleContext.addServiceListener(this);

        initVideoBridgeMenu();

        if(!configService.global().getBoolean(AUTO_ANSWER_MENU_DISABLED_PROP, false))
        {
            if(ConfigurationUtils.isAutoAnswerDisableSubmenu())
            {
                this.addSeparator();
                AutoAnswerMenu.registerMenuItems(this);
            }
            else
            {
                AutoAnswerMenu autoAnswerMenu = new AutoAnswerMenu();
                this.add(autoAnswerMenu);
            }

            addedItem = true;
        }

        createCallParkMenuItem();

        boolean addedUrlsSeparator = false;

        // Don't add the URL items to the chat window Tools menu - we don't want
        // them there, and putting them in results in them being stolen from the
        // real Tools menu (since a component can only belong to a single
        // parent panel).
        if (!this.isChatMenu)
        {
            // This is the regular Tools menu, so add the URLs items.
            for (ServiceType serviceType : ServiceType.values())
            {
                if (serviceType == ServiceType.CRM ||
                   (serviceType == ServiceType.CLOUD_HOSTED && OSUtils.IS_MAC))
                {
                    // CRM doesn't appear in the Tools menu.
                    // Cloud hosted services are not currently supported on
                    // Mac
                    continue;
                }

                logger.debug("Create menu item for " + serviceType);
                List<JMenuItem> items = serviceType.getMenuItems();

                // Not all service types go in the tools menu
                if (!items.isEmpty())
                {
                    // If this is the first we're adding, also add a divider.
                    if (!addedUrlsSeparator)
                    {
                        addedUrlsSeparator = true;
                        JSeparator sep = new JPopupMenu.Separator();
                        this.add(sep);
                        UrlServiceTools.getUrlServiceTools().
                                                registerToolsMenuSeparator(sep);
                    }

                    for (JMenuItem item : items)
                    {
                        this.add(item);
                        item.addActionListener(this);
                        addedItem = true;
                        logger.debug("Added item: " + item);
                    }
                }
            }
        }

        // All items are now instantiated and we could safely load the skin if
        // we actually added any items.
        if (addedItem)
            loadSkin();
    }

    /**
     * Creates the call park menu item to display.  Initially invisible, it will
     * only be set visible when the OperationSetCallPark is found.
     */
    private void createCallParkMenuItem()
    {
        ResourceManagementService res = GuiActivator.getResources();

        // Create the call park menu item
        callParkMenuItem = new ResMenuItem("service.gui.CALL_PARK_TITLE");
        callParkMenuItem.setName("callPark");
        callParkMenuItem.addActionListener(this);
        add(callParkMenuItem);

        // Make the item invisible until we know there is an opset to control:
        callParkMenuItem.setVisible(false);

        List<ProtocolProviderService> providers =
                GuiActivator.getRegisteredProviders(OperationSetCallPark.class);

        if (providers.isEmpty())
        {
            // This can occasionally happen, when the Call Park opset is taking
            // a while to be registered.  In which case we need to listen for it
            // to be registered.
            logger.info("No Call Park OpSet on start up");

            ServiceListener serviceListener = new ServiceListener()
            {
                @Override
                public void serviceChanged(ServiceEvent event)
                {
                    logger.debug("Service changed " + event);
                    ServiceReference<?> serviceRef = event.getServiceReference();

                    // If the event is caused by a bundle being stopped, we
                    // don't want to know
                    if (serviceRef.getBundle().getState() == Bundle.STOPPING)
                        return;

                    Object service = GuiActivator.bundleContext.getService(serviceRef);

                    if (service instanceof ProtocolProviderService)
                    {
                        ProtocolProviderService provider =
                                              (ProtocolProviderService) service;
                        OperationSet operationSet =
                           provider.getOperationSet(OperationSetCallPark.class);

                        if (operationSet != null)
                        {
                            // We've got the call park op set so handle it.
                            // Unregister the service listener as we don't need it
                            handleCallParkProviderAdded(provider);
                            GuiActivator.bundleContext.removeServiceListener(this);
                        }
                    }
                }
            };

            GuiActivator.bundleContext.addServiceListener(serviceListener);

            // There is a tiny tiny window where the op set is registered after
            // we try to get the providers but before we register the listener.
            // This code covers that
            providers = GuiActivator.getRegisteredProviders(OperationSetCallPark.class);
            if (!providers.isEmpty())
            {
                handleCallParkProviderAdded(providers.get(0));
                GuiActivator.bundleContext.removeServiceListener(serviceListener);
            }
        }
        else if (providers.size() == 1)
        {
            // This is the most common case, the tools option is created after
            // the Call Park op set has been registered
            logger.debug("Singleton call park opset");
            handleCallParkProviderAdded(providers.get(0));
        }
        else
        {
            // Unexpected, should only ever be one.  As a guess, register anyway
            // but it may well be wrong.
            logger.error("Too many call park providers added! " + providers);
            handleCallParkProviderAdded(providers.get(0));
        }
    }

    /**
     * Handle the case that a protocol provider that implements the Call Park
     * operation set has been added
     *
     * @param provider the provider that has been added.
     */
    private void handleCallParkProviderAdded(ProtocolProviderService provider)
    {
        // This method creates the call park menu item.
        // Having done so, it registers a call park listener, and a class of
        // service listener so that it can hide or show the menu item depending
        // on if it is allowed or not.
        final OperationSetCallPark callParkOpSet =
                           provider.getOperationSet(OperationSetCallPark.class);

        // Lock to prevent both the CoS checking code and the call park config
        // checking code from try to set the visibility of the call park menu
        // item at the same time.
        final Object callParkLock = new Object();

        callParkOpSet.registerListener(new CallParkListener()
        {
            @Override
            public void onOrbitStateChanged(CallParkOrbit orbit, CallParkOrbitState oldState) {}

            @Override
            public void onOrbitListChanged() {}

            @Override
            public void onCallParkEnabledChanged()
            {
                logger.debug("Call park enabled changed to " + callParkOpSet.isEnabled());
                setCallParkVisibility();
            }

            @Override
            public void onCallParkAvailabilityChanged()
            {
                logger.debug("Call park availability changed to " + callParkOpSet.isCallParkAvailable());
                setCallParkVisibility();
            }

            private void setCallParkVisibility()
            {
                synchronized (callParkLock)
                {
                    callParkConfigured = callParkOpSet.isCallParkAvailable() &&
                                         callParkOpSet.isEnabled();
                    setCallParkMenuItemVisibility();
                }
            }
        });

        synchronized (callParkLock)
        {
            callParkConfigured = callParkOpSet.isCallParkAvailable() &&
                                 callParkOpSet.isEnabled();
            setCallParkMenuItemVisibility();
        }

        ClassOfServiceService cos = GuiActivator.getCosService();
        cos.getClassOfService(new CPCosGetterCallback()
        {
            @Override
            public void onCosReceived(CPCos classOfService)
            {
                synchronized (callParkLock)
                {
                    // We could use ConfigurationUtils.isVoIPEnabledInCoS() for
                    // the VoIP enabled by CoS setting, but there is a race
                    // condition between the CoS storeCosFieldsInConfig() method
                    // saving this setting to the config file, and this callback
                    // being called (where isVoIPEnabledInCoS() would then read
                    // that same config file).
                    voipEnabled =
                        classOfService.getSubscribedMashups().isVoipAllowed() &&
                        ConfigurationUtils.isVoIPEnabledByUser();
                    logger.info("CoS received, refreshing enabled state of call park menu item");
                    setCallParkMenuItemVisibility();
                }
            }
        });
    }

    private void setCallParkMenuItemVisibility()
    {
        logger.debug("Set call park menu item enabled " +
                     "based on voipEnabled (" + voipEnabled +
                     ") and callParkConfigured (" + callParkConfigured + ").");

        boolean menuItemVisible = callParkConfigured && voipEnabled;
        configService.user().setProperty(OperationSetCallPark.CALL_PARK_ACTIVE, menuItemVisible);
        callParkMenuItem.setVisible(menuItemVisible);

        // When we finally remove the Java UI, all the code in this area that
        // determines whether the call park menu item is visible should probably
        // be moved to somewhere more sensible.  Until then, we simply pass on
        // whether Electron should show its call park menu item by saving
        // that to config then sending a WISPA settings update.
        WISPAService wispaService = GuiActivator.getWISPAService();
        if (wispaService != null)
        {
            wispaService.notify(WISPANamespace.SETTINGS, WISPAAction.UPDATE);
        }
        else
        {
            logger.warn("Could not notify WISPA about call park state update.");
        }
    }

    /**
     * Returns a list of all available video bridge providers.
     *
     * @return a list of all available video bridge providers
     */
    private List<ProtocolProviderService> getVideoBridgeProviders()
    {
        List<ProtocolProviderService> activeBridgeProviders
            = new ArrayList<>();

        for (ProtocolProviderService videoBridgeProvider
                : AccountUtils.getRegisteredProviders(
                        OperationSetVideoBridge.class))
        {
            OperationSetVideoBridge videoBridgeOpSet
                = videoBridgeProvider.getOperationSet(
                    OperationSetVideoBridge.class);

            // Check if the video bridge is actually active before adding it to
            // the list of active providers.
            if (videoBridgeOpSet.isActive())
                activeBridgeProviders.add(videoBridgeProvider);
        }

        return activeBridgeProviders;
    }

    /**
     * Initializes the appropriate video bridge menu depending on how many
     * registered providers do we have that support the
     * <tt>OperationSetVideoBridge</tt>.
     */
    private void initVideoBridgeMenu()
    {
        // If video bridge is enabled in the config then add the menu item
        if (configService.global().getBoolean(
                       OperationSetVideoBridge.IS_VIDEO_BRIDGE_DISABLED, false))
        {
            return;
        }

        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    initVideoBridgeMenu();
                }
            });
            return;
        }

        // We create the video default video bridge menu item and set it
        // disabled until we have more information on video bridge support.
        if (videoBridgeMenuItem == null)
        {
            videoBridgeMenuItem = new VideoBridgeProviderMenuItem(
                    GuiActivator.getResources()
                        .getI18NString("service.gui.CREATE_VIDEO_BRIDGE"),
                        null);

            videoBridgeMenuItem.setEnabled(false);

            insert(videoBridgeMenuItem, 1);
        }

        // We re-init the video bridge menu item each time the
        // parent menu is selected in order to be able to refresh the list
        // of available video bridge active providers.
        if (videoBridgeMenuListener == null)
        {
            videoBridgeMenuListener = new VideoBridgeMenuListener();

            addMenuListener(videoBridgeMenuListener);
        }

        // Check the protocol providers supporting video bridge in a new thread.
        if (initVideoBridgeMenuWorker == null)
            initVideoBridgeMenuWorker
                = (OSUtils.IS_MAC)
                    ? new InitVideoBridgeMenuWorkerMacOSX()
                    : new InitVideoBridgeMenuWorker();
        else
            initVideoBridgeMenuWorker.interrupt();

        initVideoBridgeMenuWorker.start();
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        GuiActivator.bundleContext.removeServiceListener(this);

        GuiActivator.getUIService().removePluginComponentListener(this);

        /*
         * Let go of all Components contributed by PluginComponents because the
         * latter will still live in the contribution store.
         */
        removeAll();
    }

    /**
     * Initializes the video bridge menu on Mac OSX.
     */
    private class InitVideoBridgeMenuWorkerMacOSX
        extends SwingWorker
    {
        protected Object construct()
        {
            Boolean enableMenu = true;
            List<ProtocolProviderService> videoBridgeProviders
                = getVideoBridgeProviders();

            int videoBridgeProviderCount
                = (videoBridgeProviders == null)
                    ? 0 : videoBridgeProviders.size();

            VideoBridgeProviderMenuItem menuItem
                = ((VideoBridgeProviderMenuItem) videoBridgeMenuItem);

            if (videoBridgeProviderCount <= 0)
                enableMenu = false;
            else if (videoBridgeProviderCount == 1)
            {
                menuItem.setPreselectedProvider(videoBridgeProviders.get(0));
                enableMenu = true;
            }
            else if (videoBridgeProviderCount > 1)
            {
                menuItem.setPreselectedProvider(null);
                menuItem.setVideoBridgeProviders(videoBridgeProviders);
                enableMenu = true;
            }

            return enableMenu;
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        protected void finished()
        {
            Boolean enabled = (Boolean) get();
            if (enabled != null)
                videoBridgeMenuItem.setEnabled(enabled);
        }
    }

    /**
     * The <tt>InitVideoBridgeMenuWorker</tt> initializes the video bridge
     * menu item depending on the number of providers currently supporting video
     * bridge calls.
     */
    private class InitVideoBridgeMenuWorker
        extends SwingWorker
    {
        private ResourceManagementService r = GuiActivator.getResources();

        protected Object construct() //throws Exception
        {
            return getVideoBridgeProviders();
        }

        /**
         * Creates the menu item.
         * @param videoBridgeProviders the list of available providers.
         * @return the list of available providers.
         */
        private JMenuItem createNewMenuItem(
            List<ProtocolProviderService> videoBridgeProviders)
        {
            int videoBridgeProviderCount
                = (videoBridgeProviders == null)
                    ? 0 : videoBridgeProviders.size();

            JMenuItem newMenuItem = null;
            if (videoBridgeProviderCount <= 0)
            {
                newMenuItem
                    = new VideoBridgeProviderMenuItem(
                            r.getI18NString("service.gui.CREATE_VIDEO_BRIDGE"),
                            null);
                newMenuItem.setEnabled(false);
            }
            else if (videoBridgeProviderCount == 1)
            {
                newMenuItem
                    = new VideoBridgeProviderMenuItem(
                            r.getI18NString("service.gui.CREATE_VIDEO_BRIDGE"),
                            videoBridgeProviders.get(0));
                newMenuItem.setName("videoBridge");
                newMenuItem.addActionListener(ToolsMenu.this);
            }
            else if (videoBridgeProviderCount > 1)
            {
                newMenuItem
                    = new SIPCommMenu(
                            r.getI18NString(
                                    "service.gui.CREATE_VIDEO_BRIDGE_MENU"));

                for (ProtocolProviderService videoBridgeProvider
                        : videoBridgeProviders)
                {
                    VideoBridgeProviderMenuItem videoBridgeItem
                        = new VideoBridgeProviderMenuItem(videoBridgeProvider);

                    ((JMenu) newMenuItem).add(videoBridgeItem);

                    if (!ConfigurationUtils.isMenuIconsDisabled())
                    {
                        GuiActivator.getImageLoaderService().
                        getAccountStatusImage(videoBridgeProvider)
                        .addToButton(videoBridgeItem);
                    }
                }
            }
            return newMenuItem;
        }

        @SuppressWarnings("unchecked")
        protected void finished()
        {
            if (videoBridgeMenuItem != null)
            {
                // If the menu item is already created we're going to remove it
                // in order to reinitialize it.
                remove(videoBridgeMenuItem);
            }

            // If video bridge is enabled in the config then add the menu item
            if (!configService.global().getBoolean(
                       OperationSetVideoBridge.IS_VIDEO_BRIDGE_DISABLED, false))
            {
                // create the menu items in event dispatch thread
                videoBridgeMenuItem =
                  createNewMenuItem((List<ProtocolProviderService>)get());

                if (!ConfigurationUtils.isMenuIconsDisabled())
                {
                    r.getImage("service.gui.icons.VIDEO_BRIDGE").addToMenuItem(videoBridgeMenuItem);
                }

                videoBridgeMenuItem.setMnemonic(
                    r.getI18nMnemonic("service.gui.CREATE_VIDEO_BRIDGE"));

                insert(videoBridgeMenuItem, 1);

                if (isPopupMenuVisible())
                {
                    java.awt.Container c = videoBridgeMenuItem.getParent();

                    if (c instanceof JComponent)
                    {
                        ((JComponent) c).revalidate();
                    }
                    c.repaint();
                }
            }
        }
    }

    /**
     * Registers the preferences item in the MacOS X menu.
     * @return <tt>true</tt> if the operation succeeds, otherwise - returns
     * <tt>false</tt>
     */
    private boolean registerConfigMenuItemMacOSX()
    {
        return FileMenu.registerMenuItemMacOSX("Preferences", this);
    }

    /**
     * Registers the settings item for non-MacOS X OS.
     */
    private void registerConfigMenuItemNonMacOSX()
    {
        ResourceManagementService r = GuiActivator.getResources();

        ImageIconFuture icon = null;

        configMenuItem = new ResMenuItem("service.gui.SETTINGS");

        if (!ConfigurationUtils.isMenuIconsDisabled())
        {
            icon = r.getImage("service.gui.icons.CONFIGURE_ICON");
            icon.addToMenuItem(configMenuItem);
        }

        add(configMenuItem);
        configMenuItem.setName("config");
        configMenuItem.addActionListener(this);
    }

    /**
     * Loads menu item icons.
     */
    public void loadSkin()
    {
        if (ConfigurationUtils.isMenuIconsDisabled())
        {
            return;
        }

        ResourceManagementService r = GuiActivator.getResources();

        // Note any of these menu items may have been removed by config so don't
        // assume they exist.
        if (conferenceMenuItem != null)
        {
            r.getImage("service.gui.icons.CONFERENCE_CALL")
            .addToMenuItem(conferenceMenuItem);
        }

        if (configMenuItem != null)
        {
            r.getImage("service.gui.icons.CONFIGURE_ICON")
            .addToMenuItem(configMenuItem);
        }

        if (videoBridgeMenuItem != null)
        {
            r.getImage("service.gui.icons.VIDEO_BRIDGE")
            .addToMenuItem(videoBridgeMenuItem);
        }
    }

    /**
     * The <tt>VideoBridgeProviderMenuItem</tt> for each protocol provider.
     */
    @SuppressWarnings("serial")
    private class VideoBridgeProviderMenuItem
        extends JMenuItem
        implements ActionListener
    {
        /**
         * Creates an instance of <tt>VideoBridgeProviderMenuItem</tt> by
         * specifying the corresponding <tt>ProtocolProviderService</tt> that
         * provides the video bridge.
         *
         * @param protocolProvider the <tt>ProtocolProviderService</tt> that
         * provides the video bridge
         */
        public VideoBridgeProviderMenuItem(
            ProtocolProviderService protocolProvider)
        {
            this (null, protocolProvider);
        }

        /**
         * Creates an instance of <tt>VideoBridgeProviderMenuItem</tt> by
         * specifying the corresponding <tt>ProtocolProviderService</tt> that
         * provides the video bridge.
         *
         * @param name the name of the menu item
         * @param preselectedProvider the <tt>ProtocolProviderService</tt> that
         * provides the video bridge
         */
        public VideoBridgeProviderMenuItem(
                                    String name,
                                    ProtocolProviderService preselectedProvider)
        {
            if ((name != null) && name.length() > 0)
            {
                setText(name);
            }
            else
            {
                String displayName = null;
                AccountID accountId = null;

                if (preselectedProvider != null)
                {
                    accountId = preselectedProvider.getAccountID();
                }

                if (accountId != null)
                {
                    displayName = accountId.getDisplayName();
                }

                if ((displayName == null) || displayName.isEmpty())
                {
                    logger.error("Couldn't find account display name. " +
                                 preselectedProvider + ", " + accountId + ", " +
                                 displayName);
                    displayName = "- - -";
                }

                setText(displayName);
            }

            ResourceManagementService r = GuiActivator.getResources();

            if (!ConfigurationUtils.isMenuIconsDisabled())
            {
                r.getImage("service.gui.icons.VIDEO_BRIDGE")
                .addToMenuItem(this);
            }

            setMnemonic(r.getI18nMnemonic("service.gui.CREATE_VIDEO_BRIDGE"));

            addActionListener(this);
        }

        public void setPreselectedProvider(
                ProtocolProviderService protocolProvider)
        {
        }

        public void setVideoBridgeProviders(
                List<ProtocolProviderService> videoBridgeProviders)
        {
        }

        @Override
        public void actionPerformed(ActionEvent arg0)
        {
        }
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls in the menu.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference<?> serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object service = GuiActivator.bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        switch (event.getType())
        {
            case ServiceEvent.REGISTERED:
            case ServiceEvent.UNREGISTERING:
                initVideoBridgeMenu();
                break;
        }
    }

    private class VideoBridgeMenuListener implements MenuListener
    {
        public void menuSelected(MenuEvent arg0)
        {
            initVideoBridgeMenu();
        }

        public void menuDeselected(MenuEvent arg0) {}

        public void menuCanceled(MenuEvent arg0) {}
    }
}
