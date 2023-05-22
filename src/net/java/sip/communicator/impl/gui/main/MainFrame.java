/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.impl.gui.DefaultContactEventHandler;
import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.StandardUIServiceImpl;
import net.java.sip.communicator.impl.gui.event.PluginComponentEvent;
import net.java.sip.communicator.impl.gui.event.PluginComponentListener;
import net.java.sip.communicator.impl.gui.main.call.CallButton;
import net.java.sip.communicator.impl.gui.main.call.CallManager;
import net.java.sip.communicator.impl.gui.main.call.DTMFHandler;
import net.java.sip.communicator.impl.gui.main.chat.conference.ConferenceChatManager;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPane;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListSearchKeyDispatcher;
import net.java.sip.communicator.impl.gui.main.contactlist.NoFavoritesPanel;
import net.java.sip.communicator.impl.gui.main.contactlist.NoMessagesPanel;
import net.java.sip.communicator.impl.gui.main.contactlist.TreeContactList;
import net.java.sip.communicator.impl.gui.main.contactlist.UnknownContactPanel;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.plugin.desktoputil.ComponentUtils;
import net.java.sip.communicator.plugin.desktoputil.LegacyErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.SIPCommButton;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.desktoputil.event.TextFieldChangeListener;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.SearchField;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.SearchField.SearchFieldContainer;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.browserlauncher.BrowserLauncherService;
import net.java.sip.communicator.service.commportal.CPCos;
import net.java.sip.communicator.service.commportal.CPCos.SubscribedMashups;
import net.java.sip.communicator.service.commportal.CPCosGetterCallback;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.contacteventhandler.ContactEventHandler;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.gui.ContactListContainer;
import net.java.sip.communicator.service.gui.ContactListFilter;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.PluginComponent;
import net.java.sip.communicator.service.gui.UIService.Reformatting;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService.BSSIDAvailability;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountInfoUtils;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetFileTransfer;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredAccountInfo;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.OperationSetWebContactInfo;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.CallListener;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.PrivacyUtils;
import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.util.skin.Skinnable;
import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.service.resources.ImageIconFuture;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;
import org.jitsi.util.SanitiseUtils;

/**
 * The main application window. This class is the core of this UI
 * implementation. It stores all available protocol providers and their
 * operation sets, as well as all registered accounts, the
 * <tt>MetaContactListService</tt> and all sent messages that aren't
 * delivered yet.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class MainFrame
    extends AbstractMainFrame
    implements  ContactListContainer,
                SearchFieldContainer,
                PluginComponentListener,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(MainFrame.class);

    /**
     * The main container.
     */
    private final TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(0, 0));

    /**
     * The status bar panel.
     */
    private final TransparentPanel statusBarPanel
        = new TransparentPanel(new BorderLayout());

    /**
     * The center panel, containing the contact list.
     */
    private final JPanel centerPanel;

    /**
     * The panel containing the tabs.
     */
    private JPanel tabPanel;

    /**
     * The panel containing the dialer.
     */
    private TransparentPanel dialPanel = new TransparentPanel(new BorderLayout());

    /**
     * The search field shown above the contact list.
     */
    private SearchField searchField;

    /**
     * A mapping of <tt>ProtocolProviderService</tt>s and their indexes.
     */
    private final HashMap<ProtocolProviderService, Integer> protocolProviders
        = new LinkedHashMap<>();

    /**
     * The panel replacing the contact list, shown when no matching is found
     * for the search filter.
     */
    private UnknownContactPanel unknownContactPanel;

    /**
     * The panel replacing the contact list, shown when no matches are found
     * for the favorites panel.
     */
    private NoFavoritesPanel noFavoritesPanel;

    /**
     * The panel replacing the contact list, shown when no chat messages are
     * found for the 'Chats' panel
     */
    private NoMessagesPanel noMessagesPanel;

    /**
     * A mapping of <tt>ProtocolProviderService</tt>s and corresponding
     * <tt>ContactEventHandler</tt>s.
     */
    private final Map<ProtocolProviderService, ContactEventHandler>
        providerContactHandlers =
            new Hashtable<>();

    /**
     * A mapping of plug-in components and their corresponding native components.
     */
    private final Map<PluginComponent, Component> nativePluginsTable =
            new Hashtable<>();

    /**
     * The north plug-in panel.
     */
    private final JPanel pluginPanelNorth = new TransparentPanel();

    /**
     * The south plug-in panel.
     */
    private final JPanel pluginPanelSouth = new TransparentPanel();

    /**
     * The west plug-in panel.
     */
    private final JPanel pluginPanelWest = new TransparentPanel();

    /**
     * The east plug-in panel.
     */
    private final JPanel pluginPanelEast = new TransparentPanel();

    /**
     * The panel East of the search box
     */
    private final JPanel searchEastPanel = new TransparentPanel(
        new FlowLayout(FlowLayout.LEFT, 0, 0));

    /**
     * The container containing the contact list.
     */
    private ContactListPane contactListPanel;

    /**
     * The contact list
     */
    private TreeContactList contactList;

    /**
     * The user interface call listener.
     */
    private CallListener uiCallListener;

    /**
     * Contact list search key dispatcher;
     */
    private final ContactListSearchKeyDispatcher clKeyDispatcher;

    /**
     * The background image for the call button
     */
    BufferedImageFuture callButtonImage
        = GuiActivator.getImageLoaderService().getImage(
                                    ImageLoaderService.DIAL_PAD_CALL_BUTTON_BG);

    /**
     * The background image for the call button rolled over
     */
    BufferedImageFuture callButtonImageRollover
        = GuiActivator.getImageLoaderService().getImage(
                              ImageLoaderService.DIAL_PAD_CALL_BUTTON_ROLLOVER);

    /**
     * The background image for the call button when pressed
     */
    BufferedImageFuture callButtonImagePressed
        = GuiActivator.getImageLoaderService().getImage(
                               ImageLoaderService.DIAL_PAD_CALL_BUTTON_PRESSED);

    /**
     * A map from tab names to the tab components. Used for selecting a new tab
     * using keyboard shortcuts.
     */
    private HashMap<String, MainFrameTabComponent> tabComponents =
            new HashMap<>();

    /**
     * A map from sub-tab names to the tab components. Used for selecting a new
     * sub-tab
     */
    private HashMap<String, HistorySubTab> subTabComponents =
            new HashMap<>();

    /**
     * Whether the calls sub-tab is allowed based on CoS settings
     */
    protected boolean mCallsTabAllowed;

    /**
     * Whether the chats sub-tab is allowed based on CoS settings
     */
    protected boolean mChatsTabAllowed;

    /**
     * Whether we are starting up. This is required to perform the correct
     * 'recent' tab configuration the very first time the CoS is returned
     */
    private boolean isStartup = true;

    /**
     * The history tab
     */
    private MainFrameTabComponent historyTab;

    /**
     * Filters in the history tab.
     */
    private HistorySubTab allButton;
    private HistorySubTab callsButton;
    private HistorySubTab chatsButton;

    /**
     * The start colour used for the background of the main frame header
     */
    public static final Color HEADER_BACKGROUND = new Color(
        GuiActivator.getResources().getColor(
            "service.gui.MAIN_FRAME_HEADER_BACKGROUND_OVERRIDE",
            GuiActivator.getResources().getColor("service.gui.LIGHT_BACKGROUND"),
            false)
        );

    /**
     * The end colour used for the background of the main frame header
     */
    public static final Color HEADER_BACKGROUND_GRADIENT = new Color(
        GuiActivator.getResources().getColor(
            "service.gui.MAIN_FRAME_HEADER_BACKGROUND_GRADIENT_OVERRIDE",
            GuiActivator.getResources().getColor(
                "service.gui.LIGHT_BACKGROUND"),
            false)
        );

    /**
     * The colour to use for text in the main frame header
     */
    public static final Color HEADER_TEXT_COLOUR =
        Constants.MAIN_FRAME_HEADER_TEXT_COLOR;

    // We record the last values of these so we spot when they change
    private BSSIDAvailability mLastBssidAvailability = BSSIDAvailability.UNKNOWN;
    private boolean lastUseLocation = false;

    // We want to show the permissions dialog the first time it is needed
    private boolean showPermissionsDialog = true;

    /**
     * The prefix of all keys in the config that relate to jabber accounts.
     */
    private static final String JABBER_ACC_CONFIG_PREFIX =
        "net.java.sip.communicator.impl.protocol.jabber";

    /**
     * The suffix to account config strings that indicate whether the
     * configured account is disabled.
     */
    private static final String ACCOUNT_DISABLED_SUFFIX = ".IS_ACCOUNT_DISABLED";

    /**
     * The prefix of the config string used by the reconnect plugin to track
     * whether accounts are online.
     */
    private static final String RECONNECTPLUGIN_PREFIX =
        "net.java.sip.communicator.plugin.reconnectplugin." +
            "ATLEAST_ONE_SUCCESSFUL_CONNECTION.";

    /**
     * Creates an instance of <tt>MainFrame</tt>.
     */
    public MainFrame()
    {
        super(HEADER_BACKGROUND, HEADER_BACKGROUND_GRADIENT, true);

        contactListPanel = new ContactListPane(this);
        contactList = contactListPanel.getContactList();

        // This will show the default hint and button, which may be
        // updated when the CoS is received.
        this.searchField = new SearchField(this,
                                           contactList.getSearchFilter(),
                                           true);

        Color bgColor = new Color(
                GuiActivator.getResources()
                    .getColor("service.gui.LIGHT_BACKGROUND"));
        this.mainPanel.setBackground(bgColor);

        this.centerPanel = new JPanel(new BorderLayout(0, 0));
        this.centerPanel.setBackground(bgColor);

        clKeyDispatcher = new ContactListSearchKeyDispatcher(   keyManager,
                                                                searchField,
                                                                this);
        keyManager.addKeyEventDispatcher(clKeyDispatcher);

        // Listen for changes to whether the chat account has gone
        // online/offline or has been enabled/disabled by the user.
        configService.user().addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent e)
            {
                String propertyName = e.getPropertyName();

                boolean connectionChanged =
                    propertyName.startsWith(RECONNECTPLUGIN_PREFIX + "Jabber");

                boolean userChanged =
                    propertyName.startsWith(JABBER_ACC_CONFIG_PREFIX) &&
                    propertyName.endsWith(ACCOUNT_DISABLED_SUFFIX);

                if (connectionChanged || userChanged)
                {
                    logger.debug("IM account connectivity has changed");

                    String oldValue = (String) e.getOldValue();
                    String newValue = (String) e.getNewValue();

                    // If the value has changed or the returned values are
                    // null so we don't know whether they have changed, update
                    // the chat offline icon.
                    if (oldValue == null || !oldValue.equals(newValue))
                    {
                        String loggablePropertyName = connectionChanged ?
                                                      SanitiseUtils.sanitise(propertyName, PrivacyUtils.SUCCESSFUL_JABBER_CONNECTION) :
                                                      propertyName;
                        logger.debug(loggablePropertyName + " has changed from " +
                            oldValue + " to " + newValue +
                            ". Updating chat offline icon.");

                        // Send an analytics event to log the change.
                        AnalyticsEventType event = connectionChanged ?
                            AnalyticsEventType.IM_CONNECTED :
                            AnalyticsEventType.IM_SIGNED_IN;

                        GuiActivator.getAnalyticsService().onEvent(
                            event , "newValue", newValue);
                        GuiActivator.getAccountManager().refreshAccountInfo();
                    }
                }
            }
        });

        /*
         * If the application is configured to quit when this frame is closed,
         * do so.
         */
        this.addWindowListener(new WindowAdapter()
        {
            /**
             * Invoked when a window has been closed.
             */
            public void windowClosed(WindowEvent event)
            {
            }

            /**
             * Invoked when a window has been opened.
             */
            public void windowOpened(WindowEvent e)
            {
                Window focusedWindow = keyManager.getFocusedWindow();

                // If there's no other focused window we request the focus
                // in the contact list.
                if (focusedWindow == null)
                {
                    requestFocusInContactList();
                }
                // If some other window keeps the focus we'll wait until it's
                // closed.
                else if (!focusedWindow.equals(MainFrame.this))
                {
                    requestFocusLater(focusedWindow);
                }
            }
        });

        this.init();

        this.initPluginComponents();

        this.setContactList(GuiActivator.getContactListService());
    }

    /**
     * Adds a WindowListener to the given <tt>focusedWindow</tt> and once it's
     * closed we check again if we can request the focus.
     *
     * @param focusedWindow the currently focused window
     */
    private void requestFocusLater(Window focusedWindow)
    {
        focusedWindow.addWindowListener(new WindowAdapter()
        {
            /**
             * Invoked when a window has been closed.
             */
            public void windowClosed(WindowEvent event)
            {
                event.getWindow().removeWindowListener(this);

                Window focusedWindow = keyManager.getFocusedWindow();

                // If the focused window is null or it's the shared owner frame,
                // which keeps focus for closed dialogs without owner, we'll
                // request the focus in the contact list.
                if (focusedWindow == null
                    || focusedWindow.getClass().getName().equals(
                        "javax.swing.SwingUtilities$SharedOwnerFrame"))
                {
                    requestFocusInContactList();
                }
                else if (!focusedWindow.equals(MainFrame.this))
                {
                    requestFocusLater(focusedWindow);
                }
            }
        });
    }

    /**
     * Requests the focus in the center panel, which contains either the
     * contact list or the unknown contact panel.
     */
    public void requestFocusInContactList()
    {
        centerPanel.requestFocusInWindow();
        contactList.requestFocus();
    }

    /**
     * Initiates the content of this frame.
     */
    private void init()
    {
        registerKeyActions();

        JComponent northPanel = createTopComponent();

        TransparentPanel searchPanel
            = new TransparentPanel(new BorderLayout(0, 0));

        searchPanel.add(searchField);

        dialPanel = createInlineDialPanel();
        searchPanel.add(dialPanel, BorderLayout.SOUTH);

        // Only add the 'create new' button if group contacts, multi-user chat,
        // or the conference service are available so that the menu has
        // multiple entries.
        if (ConfigurationUtils.groupContactsSupported() ||
            ConfigurationUtils.isMultiUserChatEnabled() ||
            GuiActivator.getConferenceService().isFullServiceEnabled())
        {
            searchPanel.add(new CreateNewButton(), BorderLayout.WEST);
        }
        else
        {
            logger.debug("Not adding 'create new' button as insufficient " +
                                                      "features are supported");

            // The search field doesn't have a minimum height.  Therefore, if
            // there is no CreateNewButton, it can appear squashed.  Fix by
            // adding an empty, rigid area, of the right height.
            int height = CreateNewButton.BG_IMAGE.resolve().getHeight();
            searchPanel.add(Box.createRigidArea(new Dimension(1, height)),
                            BorderLayout.WEST);
        }

        searchPanel.add(createButtonPanel(), BorderLayout.EAST);

        northPanel.add(searchPanel, BorderLayout.SOUTH);

        logger.debug("Styling UI for tabs");
        boolean favoritesSupported =
            ConfigurationUtils.getContactFavoritesEnabled();

        final int numberTabs = favoritesSupported ? 3 : 2;
        final JPanel panel = new JPanel(new GridLayout(0, numberTabs));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        int bgColor = GuiActivator.getResources().getColor(
                                     "service.gui.LIGHT_BACKGROUND");
        panel.setBackground(new Color(bgColor));

        String lastTabIdentifier = ConfigurationUtils.getSelectedTab();
        MainFrameTabComponent lastSelectedTab = null;

        // Create the tabs
        String tabId;
        // 1. Favorites
        MainFrameTabComponent favoritesTab = null;
        tabPanel = new JPanel(new BorderLayout());
        tabPanel.setBackground(Color.WHITE);
        if (favoritesSupported)
        {
            tabId = MainFrameTabComponent.FAVOURITES_TAB_NAME;
            favoritesTab =
                new MainFrameTabComponent(tabId,
                                          contactList,
                                          contactList.getFavoritesFilter(),
                                          panel,
                                          null,
                                          searchField,
                                          null,
                                          tabPanel);
            panel.add(favoritesTab);
            tabComponents.put(tabId, favoritesTab);

            if (tabId.equals(lastTabIdentifier))
                lastSelectedTab = favoritesTab;
        }

        // 2. Contacts
        tabId = MainFrameTabComponent.CONTACTS_TAB_NAME;
        final MainFrameTabComponent contactsTab =
            new MainFrameTabComponent(tabId,
                                      contactList,
                                      contactList.getPresenceFilter(),
                                      panel,
                                      null,
                                      searchField,
                                      null,
                                      tabPanel);
        panel.add(contactsTab);
        tabComponents.put(tabId, contactsTab);

        if (tabId.equals(lastTabIdentifier))
            lastSelectedTab = contactsTab;

        // 3. Recents tab for call/message history
        createHistoryTab(panel);
        if (MainFrameTabComponent.HISTORY_TAB_NAME.equals(lastTabIdentifier))
            lastSelectedTab = historyTab;

        // And make sure that they have the right UI for their position
        MainFrameTabComponent firstTab = favoritesSupported ?
            favoritesTab : contactsTab;
        firstTab.setIsFirst(true);
        historyTab.setIsLast(true);

        // Set the selected tab to be the last selected tab, or the first
        // tab if that does not exist
        MainFrameTabComponent selectedTab = lastSelectedTab == null ?
            contactsTab : lastSelectedTab;
        selectedTab.setSelected(true);

        // Set-up the search bar and tabs
        ClassOfServiceService cos = GuiActivator.getCosService();
        cos.getClassOfService(new CPCosGetterCallback()
        {
            boolean historyTabAdded = true;
            int currentTabs = numberTabs;

            @Override
            public void onCosReceived(CPCos cos)
            {
                // Must be run on the EDT thread
                if (!SwingUtilities.isEventDispatchThread())
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            onCosReceived(cos);
                        }
                    });
                    return;
                }

                SubscribedMashups mashups = cos.getSubscribedMashups();
                // We could use ConfigurationUtils.isVoIPEnabledInCoS() for
                // the VoIP enabled by CoS setting, but there is a race
                // condition between the CoS storeCosFieldsInConfig() method
                // saving this setting to the config file, and this callback
                // being called (where isVoIPEnabledInCoS() would then read
                // that same config file).
                boolean voipEnabled = mashups.isVoipAllowed() &&
                        ConfigurationUtils.isVoIPEnabledByUser();
                boolean nchEnabled =
                          cos.getCallLogEnabled() &&
                          mashups.isNetworkCallHistoryAllowed() &&
                          ConfigurationUtils.isNetworkCallHistoryEnabled();
                boolean imAllowed = mashups.isImAllowed();
                boolean ctdAllowed = mashups.isCtdAllowed() &&
                                     cos.getClickToDialEnabled();

                boolean wasCallsTabAllowed = mCallsTabAllowed;
                boolean wasChatsTabAllowed = mChatsTabAllowed;
                mCallsTabAllowed = voipEnabled || nchEnabled;
                mChatsTabAllowed = imAllowed;

                logger.info("CoS received, enable calls tab " + mCallsTabAllowed +
                            " based on voipEnabled = " + voipEnabled +
                            " and nchEnabled = " + nchEnabled);

                logger.info("CoS received, enable chats tab " + mChatsTabAllowed +
                    " based on imAllowed = " + imAllowed);

                // If neither VoIP nor CTD is enabled then:
                //   - Remove the dialpad button as we can't dial numbers
                //   - Update the hint text in the search field
                //   - update the config service so other UI elements can listen
                // Otherwise ensure they are set-up for number dialling
                if (voipEnabled || ctdAllowed)
                {
                    logger.debug("Set-up search box WITH calling function");
                    searchField.setDialPadButtonEnabled(true);
                    String searchHintText = GuiActivator.getResources().getI18NString("service.gui.ENTER_NAME_OR_NUMBER");
                    searchField.setHintText(searchHintText);
                }
                else
                {
                    logger.debug("Set-up search box without calling function");
                    searchField.setDialPadButtonEnabled(false);
                    String searchHintText = GuiActivator.getResources().getI18NString("service.gui.ENTER_NAME");
                    searchField.setHintText(searchHintText);
                }

                // If the state of the history tab has changed (e.g. Chat
                // has been enabled) then recreate the history tab accordingly
                if ((wasCallsTabAllowed != mCallsTabAllowed) ||
                    (wasChatsTabAllowed != mChatsTabAllowed) ||
                    isStartup)
                {
                    // We are no longer in startup
                    isStartup = false;

                    // Need to remove the old history tab while we still
                    // have a reference to it
                    allButton.setFocusable(false);
                    callsButton.setFocusable(false);
                    chatsButton.setFocusable(false);
                    panel.remove(historyTab);
                    if (historyTabAdded)
                    {
                        currentTabs--;
                    }
                    createHistoryTab(panel);

                    // If the history tab is allowed then add it
                    if (mCallsTabAllowed || mChatsTabAllowed)
                    {
                        logger.debug("Adding history tab");
                        historyTabAdded = true;
                        currentTabs++;
                        panel.setLayout(new GridLayout(0, currentTabs));
                        panel.add(historyTab);

                        // Set the history tab as the last tab, instead of the
                        // contacts tab.
                        contactsTab.setIsLast(false);
                        historyTab.setIsLast(true);

                        // If the last selected tab was the history tab then
                        // make sure it is selected now
                        if (MainFrameTabComponent.HISTORY_TAB_NAME.equals(
                                       ConfigurationUtils.getSelectedTab()))
                        {
                            historyTab.setSelected(true);
                        }

                        panel.validate();
                    }
                    else
                    {
                        logger.info("Removing history tab");
                        historyTabAdded = false;
                        panel.setLayout(new GridLayout(0, currentTabs));

                        // Set the contacts tab as the last tab, instead of the
                        // history tab.
                        historyTab.setIsLast(false);
                        contactsTab.setIsLast(true);

                        // If the history tab is selected, deselect it and
                        // select the contacts tab instead.
                        if (MainFrameTabComponent.HISTORY_TAB_NAME.equals(
                                       ConfigurationUtils.getSelectedTab()))
                        {
                            historyTab.setSelected(false);
                            contactsTab.setSelected(true);
                        }

                        panel.validate();
                    }
                }

                // Check if we need to ask the user for location permissions
                getAnyRequiredLocationPermission();
            }
        });

        panel.setVisible(true);

        tabPanel.add(panel, BorderLayout.NORTH);
        centerPanel.add(tabPanel, BorderLayout.NORTH);

        centerPanel.add(contactListPanel, BorderLayout.CENTER);

        this.mainPanel.add(northPanel, BorderLayout.NORTH);

        this.mainPanel.add(centerPanel, BorderLayout.CENTER);

        java.awt.Container contentPane = getContentPane();
        contentPane.add(mainPanel, BorderLayout.CENTER);
        contentPane.add(statusBarPanel, BorderLayout.SOUTH);
    }

    private void getAnyRequiredLocationPermission()
    {
        if (OSUtils.IS_MAC)
        {
            logger.info("Starting timer to check location permissions");
            TimerTask timerTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    checkCurrentPermissionsState();
                }
            };

            Timer timer = new Timer("Permissions polling");
            long scheduleRate = 5000;
            timer.scheduleAtFixedRate(timerTask, scheduleRate, scheduleRate);
        }
    }

    // This is called at regular intervals, so should do minimal logging to
    // avoid spam.
    // Both the isEmergencyLocationSupportNeeded can change (because we
    // previously did not have the data, but it has now arrived), and the
    // user may update the location permission via system preferences.
    private void checkCurrentPermissionsState()
    {
        boolean useLocation =
                GuiActivator.getCommPortalService().isEmergencyLocationSupportNeeded();

        // We only want to log changes, not every time we check
        if (useLocation != lastUseLocation)
        {
            logger.info("Emergency Location support " + (useLocation ?  "" : "not ") + "in use");
            lastUseLocation = useLocation;
        }

        if (useLocation)
        {
            // We do need to support the emergency location feature, so we will
            // be accessing location information
            NetworkAddressManagerService networkAddressService =
                    ServiceUtils.getService(GuiActivator.bundleContext,
                                            NetworkAddressManagerService.class);

            // See if we can already get hold of the BSSID information
            BSSIDAvailability bssidAvailability = networkAddressService != null ?
                networkAddressService.getBSSIDAvailability() : BSSIDAvailability.UNKNOWN;;

            // Report any change to the permissions
            if (bssidAvailability != mLastBssidAvailability)
            {
                logger.info("BSSID availability changed to " + bssidAvailability.name());

                String reported =
                        bssidAvailability == BSSIDAvailability.AVAILABLE ? AnalyticsParameter.VALUE_PERMISSION_ACTION_TYPE_GRANTED :
                                AnalyticsParameter.VALUE_PERMISSION_ACTION_TYPE_DENIED;
                GuiActivator
                        .getAnalyticsService()
                        .onEvent(AnalyticsEventType.PERMISSION_ACTION,
                                 AnalyticsParameter.PARAM_PERMISSION_ACTION_PERMISSION_NAME,
                                 AnalyticsParameter.VALUE_PERMISSION_LOCATION,
                                 AnalyticsParameter.PARAM_PERMISSION_ACTION_TYPE,
                                 reported);

            }

            ResourceManagementService resources = GuiActivator.getResources();

            String textRes  = null;
            String actionTextRes = null;
            if (bssidAvailability == BSSIDAvailability.UNKNOWN)
            {
                // We have not yet asked for permissions, but need to
                textRes = "plugin.sipregistrationnotifier.REQUEST_PERMISSION_DESCRIPTION";
                actionTextRes = "plugin.sipregistrationnotifier.REQUEST_PERMISSION";
            }
            else if (bssidAvailability != BSSIDAvailability.AVAILABLE)
            {
                // Permissions have been blocked
                textRes = "plugin.sipregistrationnotifier.GO_TO_PRIVACY_SETTINGS_DESCRIPTION";
                actionTextRes = "plugin.sipregistrationnotifier.GO_TO_PRIVACY_SETTINGS";
            }

            if (showPermissionsDialog && textRes != null)
            {
                PermissionsDialog dialog = new PermissionsDialog(
                        resources.getI18NString("plugin.sipregistrationnotifier.LOCATION_TITLE"),
                        resources.getI18NString(textRes),
                        resources.getI18NString(actionTextRes));

                if (bssidAvailability == BSSIDAvailability.UNKNOWN)
                {
                    dialog.hideOKButton();

                    dialog.addActionListener(e -> {
                        logger.user("Request location permission");
                        networkAddressService.requestLocationPermission();

                        // Send an analytic
                        GuiActivator
                                .getAnalyticsService()
                                .onEvent(AnalyticsEventType.PERMISSION_ACTION,
                                         AnalyticsParameter.PARAM_PERMISSION_ACTION_PERMISSION_NAME,
                                         AnalyticsParameter.VALUE_PERMISSION_LOCATION,
                                         AnalyticsParameter.PARAM_PERMISSION_ACTION_TYPE,
                                         AnalyticsParameter.VALUE_PERMISSION_ACTION_TYPE_REQUESTED);
                    });
                }
                else
                {
                    dialog.addActionListener(e -> {
                        logger.user("Open Privacy location services");

                        BrowserLauncherService browserLauncherService = ServiceUtils.getService(
                                GuiActivator.bundleContext, BrowserLauncherService.class);

                        browserLauncherService.openURL(
                                "x-apple.systempreferences:com.apple.preference.security?Privacy_LocationServices");
                    });
                }

                // We only want to show a dialog one time
                showPermissionsDialog = false;
                mLastBssidAvailability = bssidAvailability;

                dialog.showDialog();
            }
            else
            {
                mLastBssidAvailability = bssidAvailability;

            }
        }
    }

    class PermissionsDialog extends LegacyErrorDialog
    {
        private static final long serialVersionUID = 1L;

        /**
         * @param title   Title for the pop-up
         * @param message Main body of text for the pop-up
         * @param actionText text for the action button
         */
        public PermissionsDialog(String title, String message, String actionText)
        {
            super(GuiActivator.getUIService().getMainFrame(),
                  title, message, ErrorType.LOCATION);

            getRootPane().setDefaultButton(actionButton);
            actionButton.setText(actionText);
            actionButton.setVisible(true);
            actionButton.getParent().setLayout(new FlowLayout(FlowLayout.RIGHT));
            actionButton.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JButton button = (JButton) e.getSource();
            if (button.equals(okButton))
            {
                super.actionPerformed(e);
            }
            else
            {
                logger.user("Clicked action button " + button.getText());
                this.dispose();
            }
        }

        public void hideOKButton()
        {
            okButton.setVisible(false);
        }

        public void addActionListener(ActionListener listener)
        {
            actionButton.addActionListener(listener);
        }
    }

    /**
     * Creates the history tab, including its subtabs if IM is enabled.
     *
     * @param panel The panel to add the history tab to
     */
    private void createHistoryTab(JPanel panel)
    {
        ResourceManagementService resources = GuiActivator.getResources();
        final Color backgroundColor =
            new Color(resources.getColor("service.gui.LIGHT_HIGHLIGHT"));

        // Some padding definitions for the various panels and labels set up
        // below

        // The padding between the buttonPanel and the insideButtonPanel
        int buttonPanelInternalPadding = 5;

        // The size of the corner on the rounded panels and labels
        int roundedCornerSize = 6;

        // The amount of padding on the rounded button panel. This is
        // required to prevent the inner components fouling the rounded
        // border
        int buttonPanelPadding = 3;

        // The amount of padding on the rounded labels. This is required to
        // prevent the text from fouling the rounded border.
        int labelPadding = 2;

        final JPanel buttonPanel;

        // Create the panel that contains the buttons for filtering the
        // history tab.
        buttonPanel = new HistorySubTabPanel(8, buttonPanelPadding);

        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill=GridBagConstraints.BOTH;
        c.insets = new Insets(buttonPanelInternalPadding,
                              buttonPanelInternalPadding,
                              buttonPanelInternalPadding,
                              buttonPanelInternalPadding);

        JPanel insideButtonPanel = new JPanel(new GridLayout(1, 3));
        insideButtonPanel.setBackground(backgroundColor);

        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setForeground(Color.LIGHT_GRAY);
        buttonPanel.setVisible(false);
        buttonPanel.add(insideButtonPanel, c);

        // All button
        allButton = new HistorySubTab(
                  resources.getI18NString("service.gui.ALL_HISTORY_BUTTON"),
                  HistorySubTab.allName,
                  contactList.getAllHistoryFilter(),
                  roundedCornerSize, labelPadding, Color.LIGHT_GRAY);

        subTabComponents.put(HistorySubTab.allName, allButton);

        allButton.setName(HistorySubTab.allName);
        allButton.setBackground(backgroundColor);
        allButton.setHorizontalAlignment(SwingConstants.CENTER);

        // Calculate the minimum size of this label, taking into account
        // additional padding required for the rounded edges
        Dimension labelSize = ComponentUtils.getStringSize(
                                allButton, allButton.getText());
        int labelHeight = (int) labelSize.getHeight() +
                          labelPadding +
                          buttonPanelInternalPadding;

        // Note that we only need to set the preferred size for one of the
        // labels in this panel as we are using a GridLayout. This sizes
        // every component the same.
        allButton.setPreferredSize(new Dimension(labelSize.width,labelHeight));
        insideButtonPanel.add(allButton);

        // Calls button
        callsButton = new HistorySubTab(
                resources.getI18NString("service.gui.CALLS_HISTORY_BUTTON"),
                HistorySubTab.callsName,
                contactList.getCallHistoryFilter(),
                roundedCornerSize, labelPadding, Color.LIGHT_GRAY);

        subTabComponents.put(HistorySubTab.callsName, callsButton);

        callsButton.setName(HistorySubTab.callsName);
        callsButton.setBackground(backgroundColor);
        callsButton.setHorizontalAlignment(SwingConstants.CENTER);
        insideButtonPanel.add(callsButton);

        // Chats button
        chatsButton = new HistorySubTab(
                resources.getI18NString("service.gui.CHATS_HISTORY_BUTTON"),
                HistorySubTab.chatsName,
                contactList.getMessageHistoryFilter(),
                roundedCornerSize, labelPadding, Color.LIGHT_GRAY);

        subTabComponents.put(HistorySubTab.chatsName, chatsButton);

        chatsButton.setName(HistorySubTab.chatsName);
        chatsButton.setBackground(backgroundColor);
        chatsButton.setHorizontalAlignment(SwingConstants.CENTER);
        insideButtonPanel.add(chatsButton);

        historyTab =
            new MainFrameTabComponent(MainFrameTabComponent.HISTORY_TAB_NAME,
                                      contactList,
                                      contactList.getAllHistoryFilter(),
                                      panel,
                                      new String[]{"MissedCalls", "MessageReceived"},
                                      searchField,
                                      buttonPanel,
                                      tabPanel);

        tabComponents.put(MainFrameTabComponent.HISTORY_TAB_NAME, historyTab);

        // Add listeners to all of the history buttons so we can add
        // highlighting to the selected button and remove highlighting from
        // the unselected buttons.
        MouseAdapter subTabMouseListener = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent evt)
            {
                selectButton(evt.getComponent());
            }

            private void selectButton(Component button)
            {
                logger.user(button.getName() + " tab clicked in Recents tab");
                selectSubTab(button.getName());

                // Ensure we aren't showing the no messages panel
                GuiActivator.getUIService().getMainFrame().enableUnknownContactView(false);
            }
        };

        // Set the selected history tab to be the last tab used, or to the
        // 'All' tab if none has been previously selected.
        String lastHistoryTabIdentifier =
                                 ConfigurationUtils.getSelectedHistoryTab();
        selectSubTab(lastHistoryTabIdentifier == null ? allButton.getName() : lastHistoryTabIdentifier);

        allButton.addMouseListener(subTabMouseListener);
        callsButton.addMouseListener(subTabMouseListener);
        chatsButton.addMouseListener(subTabMouseListener);

        tabPanel.add(buttonPanel, BorderLayout.CENTER);

        // Set the visibility of the buttons panel depending on the current CoS
        // settings
        if (mCallsTabAllowed && mChatsTabAllowed)
        {
            // Both calls and chats tabs are allowed so show the buttons panel
            if (historyTab.isSelected())
            {
                buttonPanel.setEnabled(true);
                buttonPanel.setVisible(true);
            }

            // We have sub-tabs, so make sure they appear in the focus order
            allButton.setFocusable(true);
            callsButton.setFocusable(true);
            chatsButton.setFocusable(true);
        }
        else if (!mCallsTabAllowed && mChatsTabAllowed)
        {
            // Calls tab is not allowed but chats is, so we hide the buttons
            // panel and set the filter to be chats only
            historyTab.setFilter(contactList.getMessageHistoryFilter());
            selectSubTab(chatsButton.mName);
            buttonPanel.setEnabled(false);
            buttonPanel.setVisible(false);
        }
        else if (mCallsTabAllowed && !mChatsTabAllowed)
        {
            // Calls tab is allowed but chats is not, so we hide the buttons
            // panel and set the filter to be calls only
            historyTab.setFilter(contactList.getCallHistoryFilter());
            selectSubTab(callsButton.getName());
            buttonPanel.setEnabled(false);
            buttonPanel.setVisible(false);
        }
    }

    /**
     * A panel that has rounded edges and a line border to contain HistorySubTabs.
     */
    public class HistorySubTabPanel extends JPanel
    {
        private static final long serialVersionUID = 0L;

        /**
         * The width of the arc on each corner
         */
        private int mWidth;

        /**
         * The amount of padding on each side of the panel
         */
        private int mPadding;

        /**
         * A label that has rounded edges and a line border
         *
         * @param width the width of the rounded edge
         * @param padding the amount of padding around the panel
         */
        public HistorySubTabPanel(int width, int padding)
        {
            super();
            mWidth = width;
            mPadding = padding;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
           super.paintComponent(g);
           Dimension arcs = new Dimension(mWidth, mWidth);
           int width = getWidth();
           int height = getHeight();
           Graphics2D graphics = (Graphics2D) g;
           graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON);

           // Paint the background
           graphics.setColor(getBackground());
           graphics.fillRoundRect(mPadding+1,
                                  mPadding+1,
                                  width-2-(2*mPadding),
                                  height-2-(2*mPadding),
                                  arcs.width,
                                  arcs.height);

           // Paint the border
           graphics.setColor(getForeground());
           graphics.drawRoundRect(mPadding,
                                  mPadding,
                                  width-1-(2*mPadding),
                                  height-1-(2*mPadding),
                                  arcs.width,
                                  arcs.height);
        }
    }

    /**
     * A label that has rounded edges and a line border to create history sub tabs.
     */
    public class HistorySubTab extends JLabel
    {
        private static final long serialVersionUID = 0L;

        /**
         * The string for the All tab
         */
        public static final String allName = "service.gui.SUB_TAB_ALL";

        /**
         * The string for the Calls tab
         */
        public static final String callsName = "service.gui.SUB_TAB_CALLS";

        /**
         * The string for the Chats tab
         */
        public static final String chatsName = "service.gui.SUB_TAB_CHATS";

        /**
         * The name of this sub tab (one of allName, callsName or chatsName)
         */
        private final String mName;

        /**
         * The filter applied when this sub tab is displayed
         */
        private final ContactListFilter mFilter;

        /**
         * The width of the arc on each corner
         */
        private int mWidth;

        /**
         * The amount of padding on each side of the label to use
         */
        private int mPadding;

        /**
         * The colour of the border
         */
        private Color mBorderColor;

        /**
         * The icon used to notify of new events on this sub tab
         */
        private final ImageIconFuture notifyIcon = GuiActivator.getResources().getImage(
                                       "service.gui.icons.SUBTAB_NOTIFICATION");
        /**
         * The padding used around the text to ensure it stays in the centre
         * of the sub tab, regardless of whether the notification icon is
         * being displayed.  This padding is the width of the icon, plus the
         * gap between the icon and the text (4px).
         */
        private final int notifyIconPadding = notifyIcon.resolve().getIconWidth() + 4;

        /**
         * Whether this label is selected
         */
        private boolean isSelected = false;
        private boolean hasNewEvents;

        /**
         * A label that has rounded edges and a line border
         *
         * @param text the text to display on the label
         * @param name TODO
         * @param filter TODO
         * @param width the width of the rounded edge
         */
        public HistorySubTab(String text, String name, ContactListFilter filter, int width, int padding, Color borderColor)
        {
            super(text);
            mName = name;
            mFilter = filter;
            mWidth = width;
            mPadding = padding;
            mBorderColor = borderColor;
            setOpaque(false);
            this.setForeground(Color.BLACK);
            ScaleUtils.scaleFontAsDefault(this);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
           Dimension arcs = new Dimension(mWidth, mWidth);
           int width = getWidth();
           int height = getHeight();
           Graphics2D graphics = (Graphics2D) g;
           graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON);

           // Paint the background
           if (isSelected)
           {
               graphics.setColor(Color.WHITE);
           }
           else
           {
               graphics.setColor(getBackground());
           }
           graphics.fillRoundRect(mPadding+1, mPadding+1, width-2-(2*mPadding), height-2-(2*mPadding), arcs.width, arcs.height);

           // Paint the border if selected
           if (isSelected)
           {
               graphics.setColor(mBorderColor);
               graphics.drawRoundRect(mPadding, mPadding, width-1-(2*mPadding), height-1-(2*mPadding), arcs.width, arcs.height);
           }

           super.paintComponent(g);
        }

        public void setSelected(boolean isSelected)
        {
            if (isSelected)
            {
                ConfigurationUtils.setSelectedHistoryTab(mName);
                historyTab.setFilter(mFilter);
            }
            this.isSelected = isSelected;

            updateAccessibilityAndRepaint();
        }

        private void updateAccessibilityAndRepaint()
        {
            AccessibilityUtils.setName(this, GuiActivator.getResources().getI18NString(
                    hasNewEvents ?
                            (isSelected ? "service.gui.accessibility.SUB_TAB_SELECTED_WITH_EVENTS" :
                                           "service.gui.accessibility.SUB_TAB_NOT_SELECTED_WITH_EVENTS") :
                    isSelected ? "service.gui.accessibility.SUB_TAB_SELECTED" :
                                 "service.gui.accessibility.SUB_TAB_NOT_SELECTED",
                    new String[] {this.getText()}));

            repaint();
        }

        /**
         * Adds or removes the new events notification from the sub tab
         *
         * @param hasNewEvents If true, the notification will be added,
         * otherwise it will be removed
         */
        public void updateNewEventsNotification(boolean hasNewEvents)
        {
            logger.debug("History sub tab " + mName +
                         " has new events = " + hasNewEvents);

            this.hasNewEvents = hasNewEvents;

            // If this tab contains new events that the user hasn't
            // acknowledged, display an icon to notify the user.
            if (hasNewEvents)
            {
                notifyIcon.addToLabel(this);
                setBorder(BorderFactory.createEmptyBorder(
                                                   0, 0, 0, notifyIconPadding));
            }
            else
            {
                setIcon(null);
                setBorder(BorderFactory.createEmptyBorder(
                                  0, notifyIconPadding, 0, notifyIconPadding));
            }

            updateAccessibilityAndRepaint();
        }
    }

    /**
     * Create the inline dial panel if required
     *
     * @return the created dial panel or null if the panel is not supported
     */
    private TransparentPanel createInlineDialPanel()
    {
        TransparentPanel dialPanel = new TransparentPanel(new BorderLayout());

        // Create the dial panel
        GeneralDialPanel dialButtons = new GeneralDialPanel(new DialPadButtonListener()
        {
            public void dialButtonPressed(String name)
            {
                // Just add the text to the search field
                String searchText = searchField.getText();
                String text = searchText != null ? searchText : "";
                searchField.setText(text + name);
            }
        }, new DTMFHandler());

        final DialPadCallButton callButton = new DialPadCallButton(
            GuiActivator.getResources().getI18NString("service.gui.CALL"),
            callButtonImage,
            callButtonImageRollover,
            callButtonImagePressed);

        callButton.setPreferredSize(new Dimension(
            callButtonImage.resolve().getWidth(null),
            callButtonImage.resolve().getHeight(null) + 5));

        addSearchFieldListener(callButton);

        callButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String number = searchField.getText();
                searchField.setText("");

                if (number != null && number.length() > 0)
                {
                    // We have a number so make an analytics event and call it.
                    GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.OUTBOUND_CALL,
                                                               "Calling from",
                                                               "Dialpad");
                    CallManager.createCall(number, Reformatting.NOT_NEEDED);
                }
            }
        });

        dialPanel.add(dialButtons, BorderLayout.CENTER);

        // Put call button in panel to fix width
        JPanel callButtonPanel = new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        callButtonPanel.add(callButton);
        dialPanel.add(callButtonPanel, BorderLayout.SOUTH);

        // Initially invisible as the dial button has not been pressed
        dialPanel.setVisible(false);

        return dialPanel;
    }

    private Component createButtonPanel()
    {
        boolean isCallButtonEnabled = false;

        // Indicates if the big call button outside the search is enabled.
        String callButtonEnabledString = GuiActivator.getResources()
            .getSettingsString("impl.gui.CALL_BUTTON_ENABLED");

        if (callButtonEnabledString != null
                && callButtonEnabledString.length() > 0)
        {
            isCallButtonEnabled
                = Boolean.valueOf(callButtonEnabledString);
        }

        JPanel panel
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        if (isCallButtonEnabled)
        {
            panel.add(new CallButton(this));
        }

        // Add the plugin panel to the right of the search box.
        panel.add(searchEastPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * Creates the toolbar panel for this chat window, depending on the current
     * operating system.
     *
     * @return the created toolbar
     */
    private JComponent createTopComponent()
    {
        JComponent topComponent = null;
        JPanel panel = new TransparentPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        topComponent = panel;
        return topComponent;
    }

    @Override
    public List<ProtocolProviderService> getTelephonyProviders()
    {
        return CallManager.getTelephonyProviders();
    }

    @Override
    public void createCall(String number, Reformatting reformatting)
    {
        CallManager.createCall(number, reformatting);
    }

    /**
     * Enters or exits the "unknown contact" view. This view will propose to
     * the user some specific operations if the current filter doesn't match
     * any contacts.
     *
     * This view will vary depending on the currently active filter.
     *
     * @param isEnabled <tt>true</tt> to enable the "unknown contact" view,
     * <tt>false</tt> - otherwise.
     */
    public void enableUnknownContactView(boolean isEnabled)
    {
        boolean favoritesFilterActive =
            (contactList.getCurrentFilter() == contactList.getFavoritesFilter());

        boolean messageHistoryFilterActive =
            (contactList.getCurrentFilter() == contactList.getMessageHistoryFilter());

        // Regardless of whether we are enabling or disabling, or whether the
        // favorites filter is active, remove any view currently showing
        if (noFavoritesPanel != null)
        {
            noFavoritesPanel.setVisible(false);
            centerPanel.remove(noFavoritesPanel);
        }
        if (unknownContactPanel != null)
        {
            unknownContactPanel.setVisible(false);
            centerPanel.remove(unknownContactPanel);
        }
        if (noMessagesPanel != null)
        {
            noMessagesPanel.setVisible(false);
            noMessagesPanel.dispose();
            centerPanel.remove(noMessagesPanel);
        }

        Component unknownPanel;

        // Get (and create if necessary) the unknown contact panel to display
        if (favoritesFilterActive)
        {
            if (noFavoritesPanel == null && isEnabled)
                noFavoritesPanel = new NoFavoritesPanel();

            unknownPanel = noFavoritesPanel;
        }
        else if (messageHistoryFilterActive)
        {
            if (isEnabled)
                noMessagesPanel = new NoMessagesPanel(contactList);

            unknownPanel = noMessagesPanel;
        }
        else
        {
            if (unknownContactPanel == null && isEnabled)
                unknownContactPanel = new UnknownContactPanel(this);

            unknownPanel = unknownContactPanel;
        }

        // And display or remove the unknown contact panel
        if (isEnabled)
        {
            if (unknownPanel.getParent() != centerPanel)
            {
                contactListPanel.setVisible(false);
                unknownPanel.setVisible(true);
                centerPanel.remove(contactListPanel);
                centerPanel.add(unknownPanel, BorderLayout.CENTER);
            }
        }
        else
        {
            if (contactListPanel.getParent() != centerPanel)
            {
                contactListPanel.setVisible(true);
                centerPanel.add(contactListPanel, BorderLayout.CENTER);
            }
        }
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    /**
     * Initializes the contact list panel.
     *
     * @param contactListService The <tt>MetaContactListService</tt> containing
     * the contact list data.
     */
    public void setContactList(MetaContactListService contactListService)
    {
        contactListPanel.initList(contactListService);

        searchField.setContactList(contactList);
        clKeyDispatcher.setContactList(contactList);
    }

    /**
     * Adds all protocol supported operation sets.
     *
     * @param protocolProvider The protocol provider.
     */
    public void addProtocolSupportedOperationSets(
            ProtocolProviderService protocolProvider)
    {
        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        String ppOpSetClassName = OperationSetPersistentPresence
                                    .class.getName();
        String pOpSetClassName = OperationSetPresence.class.getName();

        // Obtain the presence operation set.
        if (supportedOperationSets.containsKey(ppOpSetClassName)
                || supportedOperationSets.containsKey(pOpSetClassName))
        {
            OperationSetPresence presence = (OperationSetPresence)
                supportedOperationSets.get(ppOpSetClassName);

            if(presence == null) {
                presence = (OperationSetPresence)
                    supportedOperationSets.get(pOpSetClassName);
            }

            presence.addContactPresenceStatusListener(
                contactList.getMetaContactListSource());
            presence.addContactPresenceStatusListener(
                GuiActivator.getGlobalStatusService());
        }

        // Obtain the basic instant messaging operation set.
        String imOpSetClassName = OperationSetBasicInstantMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(imOpSetClassName))
        {
            OperationSetBasicInstantMessaging im
                = (OperationSetBasicInstantMessaging)
                    supportedOperationSets.get(imOpSetClassName);

            //Add to all instant messaging operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            im.addMessageListener(getContactListPanel());
        }

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.addTypingNotificationsListener(this.getContactListPanel());
        }

        // Obtain the basic telephony operation set.
        String telOpSetClassName = OperationSetBasicTelephony.class.getName();

        if (supportedOperationSets.containsKey(telOpSetClassName))
        {
            OperationSetBasicTelephony<?> telephony
                = (OperationSetBasicTelephony<?>)
                    supportedOperationSets.get(telOpSetClassName);

            uiCallListener = new CallManager.GuiCallListener();

            telephony.addCallListener(uiCallListener);
        }

        // Obtain the multi user chat operation set.
        String multiChatClassName = OperationSetMultiUserChat.class.getName();

        if (supportedOperationSets.containsKey(multiChatClassName))
        {
            OperationSetMultiUserChat multiUserChat
                = (OperationSetMultiUserChat)
                    supportedOperationSets.get(multiChatClassName);

            ConferenceChatManager conferenceManager
                = GuiActivator.getUIService().getConferenceChatManager();

            multiUserChat.addInvitationListener(conferenceManager);
            multiUserChat.addInvitationRejectionListener(conferenceManager);
            multiUserChat.addPresenceListener(conferenceManager);
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            fileTransferOpSet.addFileTransferListener(getContactListPanel());
        }
    }

    /**
     * Removes all protocol supported operation sets.
     *
     * @param protocolProvider The protocol provider.
     */
    public void removeProtocolSupportedOperationSets(
            ProtocolProviderService protocolProvider)
    {
        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        String ppOpSetClassName = OperationSetPersistentPresence
                                    .class.getName();
        String pOpSetClassName = OperationSetPresence.class.getName();

        // Obtain the presence operation set.
        if (supportedOperationSets.containsKey(ppOpSetClassName)
                || supportedOperationSets.containsKey(pOpSetClassName))
        {
            OperationSetPresence presence = (OperationSetPresence)
                supportedOperationSets.get(ppOpSetClassName);

            if(presence == null)
            {
                presence = (OperationSetPresence)
                    supportedOperationSets.get(pOpSetClassName);
            }

            presence.removeContactPresenceStatusListener(
                contactList.getMetaContactListSource());
        }

        // Obtain the basic instant messaging operation set.
        String imOpSetClassName = OperationSetBasicInstantMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(imOpSetClassName))
        {
            OperationSetBasicInstantMessaging im
                = (OperationSetBasicInstantMessaging)
                    supportedOperationSets.get(imOpSetClassName);

            im.removeMessageListener(getContactListPanel());
        }

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.removeTypingNotificationsListener(this.getContactListPanel());
        }

        // Obtain the basic telephony operation set.
        String telOpSetClassName = OperationSetBasicTelephony.class.getName();

        if (supportedOperationSets.containsKey(telOpSetClassName))
        {
            OperationSetBasicTelephony<?> telephony
                = (OperationSetBasicTelephony<?>)
                    supportedOperationSets.get(telOpSetClassName);

            if (uiCallListener != null)
                telephony.removeCallListener(uiCallListener);
        }

        // Obtain the multi user chat operation set.
        String multiChatClassName = OperationSetMultiUserChat.class.getName();

        if (supportedOperationSets.containsKey(multiChatClassName))
        {
            OperationSetMultiUserChat multiUserChat
                = (OperationSetMultiUserChat)
                    supportedOperationSets.get(multiChatClassName);

            ConferenceChatManager conferenceManager
                = GuiActivator.getUIService().getConferenceChatManager();

            multiUserChat.removeInvitationListener(conferenceManager);
            multiUserChat.removeInvitationRejectionListener(conferenceManager);
            multiUserChat.removePresenceListener(conferenceManager);
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            fileTransferOpSet.removeFileTransferListener(getContactListPanel());
        }
    }

    /**
     * Returns a set of all protocol providers.
     *
     * @return a set of all protocol providers.
     */
    public Iterator<ProtocolProviderService> getProtocolProviders()
    {
        return new LinkedList<>(
                protocolProviders.keySet())
                        .iterator();
    }

    /**
     * Returns the protocol provider associated to the account given
     * by the account user identifier.
     *
     * @param accountName The account user identifier.
     * @return The protocol provider associated to the given account.
     */
    public ProtocolProviderService getProtocolProviderForAccount(
            String accountName)
    {
        for (ProtocolProviderService pps : protocolProviders.keySet())
        {
            if (pps.getAccountID().getUserID().equals(accountName))
            {
               return pps;
            }
        }
        return null;
    }

    /**
     * Adds a protocol provider.
     * @param protocolProvider The protocol provider to add.
     */
    public void addProtocolProvider(ProtocolProviderService protocolProvider)
    {
        synchronized(this.protocolProviders)
        {
            if(this.protocolProviders.containsKey(protocolProvider))
                return;

            this.protocolProviders.put(protocolProvider,
                    initiateProviderIndex(protocolProvider));
        }

        logger.info("Add the following protocol provider to the gui: "
                     + protocolProvider.getAccountID().getLoggableAccountID()
                     + " , hashcode: " + Integer.toHexString(hashCode()));

        this.addProtocolSupportedOperationSets(protocolProvider);

        ContactEventHandler contactHandler
            = this.getContactHandlerForProvider(protocolProvider);

        if (contactHandler == null)
            contactHandler = new DefaultContactEventHandler();

        this.addProviderContactHandler(protocolProvider, contactHandler);
        GuiActivator.getGlobalStatusService().addProvider(protocolProvider);
    }

    /**
     * Checks whether we have already loaded the protocol provider.
     * @param protocolProvider the provider to check.
     * @return whether we have already loaded the specified provider.
     */
    public boolean hasProtocolProvider(
        ProtocolProviderService protocolProvider)
    {
        synchronized(this.protocolProviders)
        {
            return this.protocolProviders.containsKey(protocolProvider);
        }
    }

    /**
     * Removes an account from the application.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    public void removeProtocolProvider(ProtocolProviderService protocolProvider)
    {
        logger.info("Remove the following protocol provider from the gui: "
                    + protocolProvider.getAccountID().getLoggableAccountID()
                    + " , hashcode: " + protocolProvider.hashCode());

        synchronized(this.protocolProviders)
        {
            this.protocolProviders.remove(protocolProvider);
        }

        this.removeProtocolSupportedOperationSets(protocolProvider);

        removeProviderContactHandler(protocolProvider);

        this.updateProvidersIndexes(protocolProvider);

        GuiActivator.getGlobalStatusService().removeProvider(protocolProvider);
    }

    /**
     * Removes a protocol provider of a given account.
     *
     * @param accountID The unique account ID.
     */
    public void removeProtocolProvider(AccountID accountID)
    {
        logger.info("Attempt to remove protocol provider with account ID "
                    + (accountID != null ? accountID.getLoggableAccountID() : "null")
                    + " from the gui");

        ProtocolProviderService providerToRemove = null;

        for (ProtocolProviderService pp : this.protocolProviders.keySet())
        {
            // Find protocol provider for the account with the given ID
            if (pp.getAccountID() == accountID)
            {
                providerToRemove = pp;
                break;
            }
        }

        if (providerToRemove != null)
        {
            removeProtocolProvider(providerToRemove);
        }
    }

    /**
     * Returns the index of the given protocol provider.
     * @param protocolProvider the protocol provider to search for
     * @return the index of the given protocol provider
     */
    public int getProviderIndex(ProtocolProviderService protocolProvider)
    {
        Integer o = protocolProviders.get(protocolProvider);

        return (o != null) ? o : 0;
    }

    /**
     * Returns the account user id for the given protocol provider.
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     * @return The account user id for the given protocol provider.
     */
    public String getAccountAddress(ProtocolProviderService protocolProvider)
    {
        return protocolProvider.getAccountID().getAccountAddress();
    }

    /**
     * Returns the account user display name for the given protocol provider.
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     * @return The account user display name for the given protocol provider.
     */
    public String getAccountDisplayName(ProtocolProviderService protocolProvider)
    {
        final OperationSetServerStoredAccountInfo accountInfoOpSet
            = protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);

        try
        {
            if (accountInfoOpSet != null)
            {
                String displayName
                    = AccountInfoUtils.getDisplayName(accountInfoOpSet);
                if(displayName != null && displayName.length() > 0)
                    return displayName;
            }
        }
        catch(Throwable e)
        {
            logger.error("Cannot obtain display name through OPSet");
        }

        return protocolProvider.getAccountID().getDisplayName();
    }

    /**
     * Returns the Web Contact Info operation set for the given
     * protocol provider.
     *
     * @param protocolProvider The protocol provider for which the TN
     * is searched.
     * @return OperationSetWebContactInfo The Web Contact Info operation
     * set for the given protocol provider.
     */
    public OperationSetWebContactInfo getWebContactInfoOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetWebContactInfo.class);

        return (opSet instanceof OperationSetWebContactInfo)
            ? (OperationSetWebContactInfo) opSet
            : null;
    }

    /**
     * Returns the telephony operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the telephony
     * operation set is about.
     * @return OperationSetBasicTelephony The telephony operation
     * set for the given protocol provider.
     */
    public OperationSetBasicTelephony<?> getTelephonyOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetBasicTelephony.class);

        return (opSet instanceof OperationSetBasicTelephony<?>)
            ? (OperationSetBasicTelephony<?>) opSet
            : null;
    }

    /**
     * Returns the multi user chat operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the multi user
     * chat operation set is about.
     * @return OperationSetMultiUserChat The telephony operation
     * set for the given protocol provider.
     */
    public OperationSetMultiUserChat getMultiUserChatOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        return (opSet instanceof OperationSetMultiUserChat)
            ? (OperationSetMultiUserChat) opSet
            : null;
    }

    /**
     * Returns the panel containing the ContactList.
     * @return ContactListPanel the panel containing the ContactList
     */
    public ContactListPane getContactListPanel()
    {
        return this.contactListPanel;
    }

    /**
     * Returns the valid phone number currently shown in the search field.
     * @return the valid phone number currently shown in the search field, or
     * null if there isn't one
     */
    public String getCurrentSearchPhoneNumber()
    {
        return searchField.getPhoneNumber();
    }

    /**
     * Returns the text currently shown in the search field.
     * @return the text currently shown in the search field
     */
    public String getCurrentSearchText()
    {
        return searchField.getText();
    }

    /**
     * Clears the current text in the search field.
     */
    public void clearCurrentSearchText()
    {
        searchField.setText("");
    }

    /**
     * Adds the given <tt>TextFieldChangeListener</tt> to listen for any changes
     * that occur in the search field.
     * @param l the <tt>TextFieldChangeListener</tt> to add
     */
    public void addSearchFieldListener(TextFieldChangeListener l)
    {
        searchField.addTextChangeListener(l);
    }

    /**
     * Removes the given <tt>TextFieldChangeListener</tt> that listens for any
     * changes that occur in the search field.
     * @param l the <tt>TextFieldChangeListener</tt> to remove
     */
    public void removeSearchFieldListener(TextFieldChangeListener l)
    {
        searchField.addTextChangeListener(l);
    }

    /**
     * Sets the visibility of the dial pad.
     *
     * @param setVisible if true, makes the dial pad visible.
     */
    public void setDialPadVisibility(boolean setVisible)
    {
        logger.debug("Setting dialpad visibility to " + setVisible);
        dialPanel.setVisible(setVisible);
    }

    /**
     * Checks in the configuration xml if there is already stored index for
     * this provider and if yes, returns it, otherwise creates a new account
     * index and stores it.
     *
     * @param protocolProvider the protocol provider
     * @return the protocol provider index
     */
    private int initiateProviderIndex(
            ProtocolProviderService protocolProvider)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts = configService.user()
                .getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts) {
            String accountUID
                = configService.user().getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID()))
            {
                String  index = configService.user().getString(
                        accountRootPropName + ".accountIndex");

                if(index != null) {
                    //if we have found the accountIndex for this protocol provider
                    //return this index
                    return Integer.parseInt(index);
                }
                else
                {
                    //if there's no stored accountIndex for this protocol
                    //provider, calculate the index, set it in the configuration
                    //service and return it.

                    return createAccountIndex(protocolProvider,
                            accountRootPropName);
                }
            }
        }

        String accNodeName = "acc" + System.currentTimeMillis();

        String accountPackage
            = "net.java.sip.communicator.impl.gui.accounts."
                    + accNodeName;

        configService.user().setProperty(accountPackage,
                protocolProvider.getAccountID().getAccountUniqueID());

        return createAccountIndex(protocolProvider,
                accountPackage);
    }

    /**
     * Creates and calculates the account index for the given protocol
     * provider.
     * @param protocolProvider the protocol provider
     * @param accountRootPropName the path to where the index should be saved
     * in the configuration xml
     * @return the created index
     */
    private int createAccountIndex(ProtocolProviderService protocolProvider,
            String accountRootPropName)
    {
        int accountIndex = -1;

        for (ProtocolProviderService pps : protocolProviders.keySet())
        {
            if (pps.getProtocolDisplayName().equals(
                protocolProvider.getProtocolDisplayName())
                && !pps.equals(protocolProvider))
            {
                int index = protocolProviders.get(pps);

                if (accountIndex < index)
                    accountIndex = index;
            }
        }
        accountIndex++;
        configService.user().setProperty(
                accountRootPropName + ".accountIndex",
                accountIndex);

        return accountIndex;
    }

    /**
     * Updates the indexes in the configuration xml, when a provider has been
     * removed.
     * @param removedProvider the removed protocol provider
     */
    private void updateProvidersIndexes(ProtocolProviderService removedProvider)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        ProtocolProviderService currentProvider = null;
        int sameProtocolProvidersCount = 0;

        for (ProtocolProviderService pps : protocolProviders.keySet()) {
            if(pps.getProtocolDisplayName().equals(
                    removedProvider.getProtocolDisplayName())) {

                sameProtocolProvidersCount++;
                if(sameProtocolProvidersCount > 1) {
                    break;
                }
                currentProvider = pps;
            }
        }

        if(sameProtocolProvidersCount < 2 && currentProvider != null) {
            protocolProviders.put(currentProvider, 0);

            List<String> accounts = configService.user()
                .getPropertyNamesByPrefix(prefix, true);

            for (String rootPropName : accounts) {
                String accountUID
                    = configService.user().getString(rootPropName);

                if(accountUID.equals(currentProvider
                        .getAccountID().getAccountUniqueID())) {

                    configService.user().setProperty(
                            rootPropName + ".accountIndex",
                            0);
                }
            }
        }
    }

    /**
     * Overwrites the <tt>SIPCommFrame</tt> close method. This method is
     * invoked when user presses the Escape key.
     * @param isEscaped indicates if this window has been closed by pressing
     * the escape key
     */
    protected void close(boolean isEscaped)
    {
        Component contactListRightMenu = contactList.getRightButtonMenu();

        if (contactListRightMenu != null && contactListRightMenu.isVisible())
        {
            contactListRightMenu.setVisible(false);
        }
    }

    /**
     * Adds the given <tt>contactHandler</tt> to handle contact events for the
     * given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * contacts should be handled by the given <tt>contactHandler</tt>
     * @param contactHandler the <tt>ContactEventHandler</tt> that would handle
     * events coming from the UI for any contacts belonging to the given
     * provider
     */
    public void addProviderContactHandler(
        ProtocolProviderService protocolProvider,
        ContactEventHandler contactHandler)
    {
        providerContactHandlers.put(protocolProvider, contactHandler);
    }

    /**
     * Removes the <tt>ContactEventHandler</tt> corresponding to the given
     * <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the protocol provider, which contact handler
     * we would like to remove
     */
    public void removeProviderContactHandler(
        ProtocolProviderService protocolProvider)
    {
        providerContactHandlers.remove(protocolProvider);
    }

    /**
     * Returns the <tt>ContactEventHandler</tt> registered for this protocol
     * provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for which
     * we are searching a <tt>ContactEventHandler</tt>.
     * @return the <tt>ContactEventHandler</tt> registered for this protocol
     * provider
     */
    public ContactEventHandler getContactHandler(
        ProtocolProviderService protocolProvider)
    {
        return providerContactHandlers.get(protocolProvider);
    }

    /**
     * Returns the <tt>ContactEventHandler</tt> for contacts given by the
     * <tt>protocolProvider</tt>. The <tt>ContactEventHandler</tt> is meant to
     * be used from other bundles in order to change the default behavior of
     * events generated when clicking a contact.
     * @param protocolProvider the protocol provider for which we want to obtain
     * a contact event handler
     * @return the <tt>ContactEventHandler</tt> for contacts given by the
     * <tt>protocolProvider</tt>
     */
    private ContactEventHandler getContactHandlerForProvider(
        ProtocolProviderService protocolProvider)
    {
        ServiceReference<?>[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + protocolProvider.getProtocolName()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                ContactEventHandler.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("GuiActivator : " + ex);
        }

        if(serRefs == null)
            return null;

        return (ContactEventHandler) GuiActivator.bundleContext
            .getService(serRefs[0]);
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        pluginPanelSouth.setLayout(
            new BoxLayout(pluginPanelSouth, BoxLayout.Y_AXIS));
        pluginPanelNorth.setLayout(
            new BoxLayout(pluginPanelNorth, BoxLayout.Y_AXIS));
        pluginPanelEast.setLayout(
            new BoxLayout(pluginPanelEast, BoxLayout.Y_AXIS));
        pluginPanelWest.setLayout(
            new BoxLayout(pluginPanelWest, BoxLayout.Y_AXIS));

        java.awt.Container contentPane = getContentPane();
        contentPane.add(pluginPanelNorth, BorderLayout.NORTH);
        contentPane.add(pluginPanelEast, BorderLayout.EAST);
        contentPane.add(pluginPanelWest, BorderLayout.WEST);
        this.mainPanel.add(pluginPanelSouth, BorderLayout.SOUTH);

        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference<?>[] serRefs = null;

        try
        {
            serRefs
                = GuiActivator
                    .bundleContext
                        .getServiceReferences(
                            PluginComponent.class.getName(),
                            "(|("
                                + Container.CONTAINER_ID
                                + "="
                                + Container.CONTAINER_MAIN_WINDOW.getID()
                                + ")("
                                + Container.CONTAINER_ID
                                + "="
                                + Container.CONTAINER_STATUS_BAR.getID()
                                + ")("
                                + Container.CONTAINER_ID
                                + "="
                                + Container.CONTAINER_SEARCH_EAST.getID()
                                + "))");
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                PluginComponent c
                    = (PluginComponent)
                        GuiActivator.bundleContext.getService(serRef);

                if (c.isNativeComponent())
                    nativePluginsTable.put(c, new JPanel());
                else
                {
                    String pluginConstraints = c.getConstraints();
                    Object constraints;

                    if (pluginConstraints != null)
                        constraints
                            = StandardUIServiceImpl
                                .getBorderLayoutConstraintsFromContainer(
                                    pluginConstraints);
                    else
                        constraints = BorderLayout.SOUTH;

                    this.addPluginComponent((Component) c.getComponent(), c
                        .getContainer(), constraints);
                }
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Adds the associated with this <tt>PluginComponentEvent</tt> component to
     * the appropriate container.
     * @param event the <tt>PluginComponentEvent</tt> that has notified us of
     * the add of a plugin component
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent pluginComponent = event.getPluginComponent();
        Container pluginContainer = pluginComponent.getContainer();

        if (pluginContainer.equals(Container.CONTAINER_MAIN_WINDOW)
            || pluginContainer.equals(Container.CONTAINER_STATUS_BAR)
            || pluginContainer.equals(Container.CONTAINER_SEARCH_EAST))
        {
            String pluginConstraints = pluginComponent.getConstraints();
            Object constraints;

            if (pluginConstraints != null)
                constraints =
                    StandardUIServiceImpl
                        .getBorderLayoutConstraintsFromContainer(pluginConstraints);
            else
                constraints = BorderLayout.SOUTH;

            if (pluginComponent.isNativeComponent())
            {
                this.nativePluginsTable.put(pluginComponent, new JPanel());

                if (isFrameVisible())
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            addNativePlugins();
                        }
                    });
                }
            }
            else
            {
                this.addPluginComponent((Component) pluginComponent
                    .getComponent(), pluginContainer, constraints);
            }
        }
    }

    /**
     * Removes the associated with this <tt>PluginComponentEvent</tt> component
     * from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us of the
     * remove of a plugin component
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        final PluginComponent pluginComponent = event.getPluginComponent();
        final Container containerID = pluginComponent.getContainer();

        if (containerID.equals(Container.CONTAINER_MAIN_WINDOW))
        {
            Object constraints = StandardUIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(
                        pluginComponent.getConstraints());

            if (constraints == null)
                constraints = BorderLayout.SOUTH;

            if (pluginComponent.isNativeComponent())
            {
                if (nativePluginsTable.containsKey(pluginComponent))
                {
                    final Component c = nativePluginsTable.get(pluginComponent);
                    final Object finalConstraints = constraints;

                    Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            removePluginComponent(c, containerID,
                                finalConstraints);

                            getContentPane().repaint();
                        }
                    };

                    if (!SwingUtilities.isEventDispatchThread())
                    {
                        SwingUtilities.invokeLater(r);
                    }
                    else
                    {
                        r.run();
                    }
                }
            }
            else
            {
                if (!SwingUtilities.isEventDispatchThread())
                {
                    final Object finalConstraints = constraints;
                    SwingUtilities.invokeLater(() -> this.removePluginComponent((Component) pluginComponent.getComponent(), containerID, finalConstraints));
                }
                else
                {
                    this.removePluginComponent((Component) pluginComponent.getComponent(), containerID, constraints);
                }
            }

            nativePluginsTable.remove(pluginComponent);
        }
    }

    /**
     * Removes all native plugins from this container.
     */
    private void removeNativePlugins()
    {
        for (Map.Entry<PluginComponent, Component> entry
                : nativePluginsTable.entrySet())
        {
            PluginComponent pluginComponent = entry.getKey();
            Component c = entry.getValue();

            Object constraints
                = StandardUIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(pluginComponent
                        .getConstraints());

            if (constraints == null)
                constraints = BorderLayout.SOUTH;

            this.removePluginComponent(c, pluginComponent.getContainer(),
                constraints);

            this.getContentPane().repaint();
        }
    }

    /**
     * Adds all native plugins to this container.
     */
    public void addNativePlugins()
    {
        this.removeNativePlugins();

        for (Map.Entry<PluginComponent, Component> pluginEntry
                : nativePluginsTable.entrySet())
        {
            PluginComponent plugin = pluginEntry.getKey();
            Object constraints
                = StandardUIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(
                        plugin.getConstraints());

            Component c = (Component) plugin.getComponent();

            this.addPluginComponent(c, plugin.getContainer(), constraints);

            this.nativePluginsTable.put(plugin, c);
        }
    }

    /**
     * Adds the given component with to the container corresponding to the
     * given constraints.
     *
     * @param c the component to add
     * @param container the container to which to add the given component
     * @param constraints the constraints determining the container
     */
    private void addPluginComponent(Component c,
                                    Container container,
                                    Object constraints)
    {
        if (container.equals(Container.CONTAINER_MAIN_WINDOW))
        {
            if (constraints.equals(BorderLayout.NORTH))
            {
                pluginPanelNorth.add(c);
                pluginPanelNorth.repaint();
            }
            else if (constraints.equals(BorderLayout.SOUTH))
            {
                pluginPanelSouth.add(c);
                pluginPanelSouth.repaint();
            }
            else if (constraints.equals(BorderLayout.WEST))
            {
                pluginPanelWest.add(c);
                pluginPanelWest.repaint();
            }
            else if (constraints.equals(BorderLayout.EAST))
            {
                pluginPanelEast.add(c);
                pluginPanelEast.repaint();
            }
        }
        else if (container.equals(Container.CONTAINER_STATUS_BAR))
        {
            statusBarPanel.add(c);
        }
        else if (container.equals(Container.CONTAINER_SEARCH_EAST))
        {
            searchEastPanel.add(c);
            searchEastPanel.repaint();
        }

        this.getContentPane().repaint();
        this.getContentPane().validate();
    }

    /**
     * Removes the given component from the container corresponding to the given
     * constraints.
     *
     * @param c the component to remove
     * @param container the container from which to remove the given component
     * @param constraints the constraints determining the container
     */
    private void removePluginComponent(final Component c,
                                       final Container container,
                                       final Object constraints)
    {
        // Must be run on the EDT thread
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    removePluginComponent(c, container, constraints);
                }
            });
            return;
        }

        if (container.equals(Container.CONTAINER_MAIN_WINDOW))
        {
            if (constraints.equals(BorderLayout.NORTH))
                pluginPanelNorth.remove(c);
            else if (constraints.equals(BorderLayout.SOUTH))
                pluginPanelSouth.remove(c);
            else if (constraints.equals(BorderLayout.WEST))
                pluginPanelWest.remove(c);
            else if (constraints.equals(BorderLayout.EAST))
                pluginPanelEast.remove(c);
        }
        else if (container.equals(Container.CONTAINER_STATUS_BAR))
        {
            this.statusBarPanel.remove(c);
        }
    }

    /**
     * Called when the ENTER key was typed when this window was the focused
     * window. Performs the appropriate actions depending on what item had the focus,
     * and the current state of the contact list.
     * @param evt The KeyEvent of the ENTER key
     */
    public void enterKeyTyped(KeyEvent evt)
    {
        Component focus = getFocusOwner();
        logger.user("ENTER pressed in main window, with focus on " + focus.getClass().getSimpleName());

        if (focus instanceof JButton)
        {
            // For accessibility, enter on a button should activate that button
            ((JButton)focus).doClick();
        }
        else if (focus instanceof HistorySubTab)
        {
            // HistorySubTab only listens for mouse presses, so to act on a
            // keyboard event, we need to dummy up a mouse event!
            ComponentUtils.simulateMouseClick(focus);
        }
        else if (focus instanceof JLabel)
        {
            // Dispatch the key to the label if it has a listener
            KeyListener[] listeners = focus.getKeyListeners();
            if (listeners.length > 0)
            {
                listeners[0].keyTyped(evt);
            }
        }
        // Check whether pressing 'enter' is meant to start a call.
        else if (GuiActivator.enterDialsNumber())
        {
            // Get the phone number from the search box.
            String number = getCurrentSearchPhoneNumber();

            if ((contactListPanel.isVisible()) &&
                (contactList.getSelectedContact() != null))
            {
                // Make the contact list handle the key press.
                contactList.enterKeyTyped();
            }
            else if ((number != null) &&
                (ConfigurationUtils.isCallingEnabled()))
            {
                // The search box contains a valid phone number, so call it
                // (and make an analytics event).
                GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.OUTBOUND_CALL,
                                                           "Calling from",
                                                           "Search field");
                CallManager.createCall(number, Reformatting.NOT_NEEDED);
                clearCurrentSearchText();
            }
            else if (contactListPanel.isVisible() &&
                     (contactList.getSelectedGroup() == null))
            {
                // No contact or group is selected, and the number cannot be
                // called (this is probably because the user has entered a
                // name or IM address) so select the first contact and give
                // focus to the contact list.
                contactList.selectFirstContact();
                contactList.getComponent().requestFocus();
            }
        }
        else if (unknownContactPanel != null && unknownContactPanel.isVisible())
        {
            // There is no contact visible, so add the detail as a new contact.
            unknownContactPanel.addUnknownContact();
        }
        else if (contactListPanel.isVisible())
        {
            // Make the contact list handle the key press.
            contactList.enterKeyTyped();
        }
    }

    /**
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when this window
     * was the focused window. Performs the appropriate actions depending on the
     * current state of the contact list.
     */
    public void ctrlEnterKeyTyped()
    {
        logger.user("CTRL-ENTER or CMD-ENTER pressed in main window");

        if (contactListPanel.isVisible())
        {
            // Make the contact list handle the key press.
            contactList.ctrlEnterKeyTyped();
        }
        else if (unknownContactPanel != null &&
                 unknownContactPanel.isVisible() &&
                 !GuiActivator.enterDialsNumber())
        {
            // We are on the 'unknown contact' page and 'ctrl-enter' is used to
            // start a call, so call the number that was searched for.
            unknownContactPanel.startCall();
            clearCurrentSearchText();
        }
    }

    /**
     * Select the tab with the given tab name
     *
     * @param tabName the name of the tab to select
     */
    public void selectTab(String tabName)
    {
        MainFrameTabComponent tabComponent = tabComponents.get(tabName);

        if (tabComponent != null)
        {
            tabComponent.select();
        }
    }

    /**
     * Select the sub-tab with the given tab name
     *
     * @param tabName the name of the sub-tab to select
     */
    public void selectSubTab(String tabName)
    {
        HistorySubTab previousSelection = null;
        HistorySubTab newSelection = null;
        for (Entry<String, HistorySubTab> tabEntry : subTabComponents.entrySet())
        {
            HistorySubTab subTab = tabEntry.getValue();
            if (subTab.isSelected)
            {
                previousSelection = subTab;
            }
            if (tabEntry.getKey().equals(tabName))
            {
                newSelection = subTab;
            }
        }
        if (previousSelection != null)
        {
            previousSelection.setSelected(false);
        }
        if (newSelection != null)
        {
            newSelection.setSelected(true);
        }
    }

    /**
     * Sets or unsets the notification icon that indicates whether there are
     * new events in this sub tab that the user has not acknowledged.
     *
     * @param tabName the name of the sub tab to set or clear the notification
     * icon on.
     * @param notificationsActive if true, there are active notifications on
     * this tab so the icon should be visible.
     */
    public void setSubTabNotification(String tabName, boolean notificationsActive)
    {
        logger.debug("Setting notifications active = " + notificationsActive +
                                                     " for sub tab " + tabName);

        HistorySubTab subTab = subTabComponents.get(tabName);

        if (subTab != null)
        {
            subTab.updateNewEventsNotification(notificationsActive);
        }
    }

    /**
     * Registers key actions for this window.
     */
    private void registerKeyActions()
    {
        InputMap inputMap = getRootPane()
            .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Remove the default escape key mapping as it's a special
        // one for the main frame and the contactlist
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
    }

    /**
     * Reloads skin information
     */
    public void loadSkin()
    {
        this.mainPanel.setBackground(new Color(
                GuiActivator.getResources()
                    .getColor("service.gui.LIGHT_BACKGROUND")));
    }

    /**
     * A SIPCommButton that responds to changes in the search field by
     * enabling/disabling depending on whether the search string is a valid
     * phone number.
     */
    private class DialPadCallButton extends SIPCommButton
                                    implements TextFieldChangeListener
    {
        private static final long serialVersionUID = 4215401613024039010L;

        public DialPadCallButton(String text,
                         BufferedImageFuture callButtonImage,
                         BufferedImageFuture callButtonImageRollover,
                         BufferedImageFuture callButtonImagePressed)
        {
            super(text,
                  callButtonImage,
                  callButtonImageRollover,
                  callButtonImagePressed);

            updateButtonUI();
        }

        public void textRemoved()
        {
            updateButtonUI();
        }

        public void textInserted()
        {
            updateButtonUI();
        }

        /**
         * Updates the enabled state of this button if the search field
         * currently holds a valid phone number.
         */
        private void updateButtonUI()
        {
            // Don't check if calling is enabled, as this button is hidden
            // if not.
            String searchText = getCurrentSearchPhoneNumber();
            boolean isValidPhoneNumber = (searchText != null
                && searchText.length() > 0
                && CallManager.getTelephonyProviders().size() > 0);

            setEnabled(isValidPhoneNumber);
        }
    }

    @Override
    public void repaintWindow()
    {
        repaint();
        getContentPane().repaint();
        for (MainFrameTabComponent tab : tabComponents.values())
        {
            tab.restyle();
        }
    }

    @Override
    protected Dimension getMinimumBoundsSize()
    {
        int width = GuiActivator.getResources()
            .getScaledSize("impl.gui.MAIN_WINDOW_MIN_WIDTH");

        int height = GuiActivator.getResources()
            .getScaledSize("impl.gui.MAIN_WINDOW_MIN_HEIGHT");

        return new Dimension(width, height);

    }

    @Override
    protected Dimension getPreferredBoundsSize()
    {
        int width = GuiActivator.getResources()
            .getScaledSize("impl.gui.MAIN_WINDOW_WIDTH");

        int height = GuiActivator.getResources()
            .getScaledSize("impl.gui.MAIN_WINDOW_HEIGHT");

        return new Dimension(width, height);
    }

    @Override
    public void setDefaultFont(Font font)
    {
        JComponent layeredPane = getLayeredPane();

        for (int i = 0; i < layeredPane.getComponentCount(); i++)
        {
            layeredPane.getComponent(i).setFont(font);
        }
    }
}
