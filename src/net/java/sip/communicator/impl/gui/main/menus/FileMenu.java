/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.desktop.QuitResponse;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.swing.*;
import javax.swing.JPopupMenu.Separator;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.drew.lang.annotations.Nullable;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.event.PluginComponentEvent;
import net.java.sip.communicator.impl.gui.event.PluginComponentListener;
import net.java.sip.communicator.impl.gui.main.account.NewAccountDialog;
import net.java.sip.communicator.impl.gui.main.chat.AddChatParticipantsDialog;
import net.java.sip.communicator.plugin.desktoputil.GroupContactMenuUtils;
import net.java.sip.communicator.plugin.desktoputil.ResMenuItem;
import net.java.sip.communicator.plugin.desktoputil.SIPCommMenu;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.PluginComponent;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.managecontact.ManageContactWindow;
import net.java.sip.communicator.service.notification.NotificationAction;
import net.java.sip.communicator.service.notification.NotificationHandler;
import net.java.sip.communicator.service.notification.NotificationService.HandlerAddedListener;
import net.java.sip.communicator.service.notification.SoundNotificationHandler;
import net.java.sip.communicator.service.notification.SoundNotificationHandler.MuteListener;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.AccountUtils;
import net.java.sip.communicator.util.skin.Skinnable;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;

/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class FileMenu
    extends SIPCommMenu
    implements  ActionListener,
                PluginComponentListener,
                Skinnable
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>FileMenu</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger = Logger.getLogger(FileMenu.class);

    // Menu item names
    private static final String CLOSE = "close";
    private static final String OFFLINE_ACCOUNT = "offlineAccount";
    private static final String NEW_GROUP_CHAT = "newGroupChat";
    private static final String NEW_ACCOUNT = "newAccount";
    private static final String ADD_CONTACT = "addContact";
    private static final String MUTE = "mute";
    private static final String JOIN_CONFERENCE = "joinConference";
    private static final String LOG_OUT = "logOut";

    private final Frame parentWindow;

    /**
     * Mute Notifications Item
     */
    private JMenuItem muteMenuItem;

    /**
     * Join conference menu item.
     */
    private JMenuItem joinConferenceItem;

    /**
     * Add new account menu item.
     */
    private final JMenuItem newAccountMenuItem;

    /**
     * Menu item that toggles the chat account offline/online.
     */
    private final JMenuItem chatOfflineMenuItem;

    /**
     * Add new contact menu item.
     */
    private JMenuItem addContactItem;

    /**
     * New group chat menu item.
     */
    private final JMenuItem newGroupChatItem;

    /**
     * Create group contact menu item.
     */
    private JMenuItem createGroupContactItem;

    /**
     * Separator after the chat menu
     */
    private final Separator chatItemsSeparator;

    /**
     * Log out menu item.
     */
    private JMenuItem logOutMenuItem;

    /**
     * Close menu item.
     */
    private JMenuItem closeMenuItem;

    /**
     * Indicates if this menu is shown for the chat window or the contact list
     * window.
     */
    private final boolean isChatMenu;

    /**
     * The configuration service.
     */
    private final ConfigurationService configService =
        GuiActivator.getConfigurationService();

    /**
     * The resources service.
     */
    private final ResourceManagementService resources =
        GuiActivator.getResources();

    /**
     * The conference service.
     */
    private final ConferenceService conferenceService =
        GuiActivator.getConferenceService();

    /**
     * The account manager.
     */
    private final AccountManager accountManager =
        GuiActivator.getAccountManager();

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(Frame parentWindow)
    {
        this(parentWindow, false);
    }

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * We add elements to the menu in blocks with separators in between.  Each block
     * is responsible for the separator AFTER itself: i.e. it is shown/hidden with
     * the rest of the block.
     *
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     * @param isChatMenu indicates if this menu would be shown for a chat
     * window
     */
    public FileMenu(Frame parentWindow, boolean isChatMenu)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.FILE"));

        this.isChatMenu = isChatMenu;

        this.parentWindow = parentWindow;

        if (ConfigurationUtils.isCallingOrImEnabled())
        {
            // Add the Mute Notifications option
            muteMenuItem = new ResMenuItem("service.gui.MUTE_NOTIFICATIONS");
            add(muteMenuItem);

            muteMenuItem.setName(MUTE);
            muteMenuItem.addActionListener(this);

            // Add Mute Listeners to allow us to change the displayed text when the
            // mute state changes
            GuiActivator.getNotificationService().addHandlerAddedListener(
                new HandlerAddedListener()
                {
                    @Override
                    public void onHandlerAdded(NotificationHandler handler)
                    {
                        addMuteListener(handler);
                    }
                }
            );

            for (NotificationHandler handler :
                            GuiActivator.getNotificationService()
                                .getActionHandlers(NotificationAction.ACTION_SOUND))
            {
                addMuteListener(handler);
            }

            addSeparator();
        }

        if (!isChatMenu &&
            conferenceService != null &&
            !conferenceService.isFullServiceEnabled() &&
            conferenceService.isJoinEnabled())
        {
            joinConferenceItem = new ResMenuItem("service.gui.conf.JOIN_CONFERENCE");
            add(joinConferenceItem);
            joinConferenceItem.setName(JOIN_CONFERENCE);
            joinConferenceItem.addActionListener(this);

            addSeparator();
        }

        if (ConfigurationUtils.isCallingOrImEnabled())
        {
            if (!isChatMenu)
            {
                addContactItem = new ResMenuItem("service.gui.ADD_NEW_CONTACT");
                add(addContactItem);
                addContactItem.setName(ADD_CONTACT);
                addContactItem.addActionListener(this);
            }

            createGroupContactItem = GroupContactMenuUtils.createNewGroupContactMenu();
            add(createGroupContactItem);

            addSeparator();
        }

        newAccountMenuItem = new ResMenuItem("service.gui.NEW_ACCOUNT");
        add(newAccountMenuItem);
        newAccountMenuItem.setName(NEW_ACCOUNT);
        newAccountMenuItem.addActionListener(this);

        newGroupChatItem = new ResMenuItem("service.gui.chat.CREATE_NEW_GROUP");
        add(newGroupChatItem);
        newGroupChatItem.setName(NEW_GROUP_CHAT);
        newGroupChatItem.addActionListener(this);

        // Set up the menu item that toggles the chat accounts offline and
        // online with default settings.
        String menuResource = ConfigurationUtils.isSmsEnabled() ?
            "service.gui.chat.ENABLE_DISABLE_CHAT_SMS" : "service.gui.chat.SIGN_IN_OUT_CHAT";
        chatOfflineMenuItem = new ResMenuItem(menuResource);
        add(chatOfflineMenuItem);
        chatOfflineMenuItem.setName(OFFLINE_ACCOUNT);
        chatOfflineMenuItem.addActionListener(this);

        // Keep a reference to this separator so we can change the visibility later.
        chatItemsSeparator = new Separator();
        add(chatItemsSeparator);

        registerLogOutMenuItem();
        registerCloseMenuItem();

        // All items are now instantiated and could safely load the skin.
        loadSkin();

        //this.addContactItem.setIcon(new ImageIcon(ImageLoader
        //        .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        this.setMnemonic(resources.getI18nMnemonic("service.gui.FILE"));

        initPluginComponents();
    }

    /**
     * Check the current mute state and listen to mute state changes if provided
     * a SoundNotificationHandler
     * @param handler
     */
    private void addMuteListener(NotificationHandler handler)
    {
        logger.debug("Adding mute listeners for File Menu");

        if (handler instanceof SoundNotificationHandler)
        {
            SoundNotificationHandler soundHandler = (SoundNotificationHandler) handler;

            setMuteText(soundHandler.isMute());

            soundHandler.addMuteListener(new MuteListener()
            {
                @Override
                public void onMuteChanged(boolean newMute)
                {
                    setMuteText(newMute);
                }
            });
        }
    }

    /**
     * Set the display text and mnemonic for the Mute Notifications menu option
     * @param mute Current mute state
     */
    private void setMuteText(boolean mute)
    {
        String res = mute ? "service.gui.UNMUTE_NOTIFICATIONS"  :
                "service.gui.MUTE_NOTIFICATIONS";

        String text = resources.getI18NString(res);

        logger.info("Setting mute text to: " + text);
        muteMenuItem.setText(text);
        muteMenuItem.setMnemonic(resources.getI18nMnemonic(res));
    }

    /**
     * Update the chat account menu items based on whether IM is enabled,
     * whether the chat account is configured to be set up by the user and
     * whether the user already has the maximum number of chat accounts.
     */
    private void updateChatMenuItems()
    {
        logger.debug("Updating chat menu items");

        // First check whether IM is enabled - if not, just hide all chat menu
        // items.
        if (ConfigurationUtils.isImEnabled())
        {
            logger.debug("IM is enabled");

            // Next determine whether we should be showing the "add chat account"
            // menu item.  Users are only allowed to have at most 1 chat account.
            // Also, they are only allowed to see any UI relating to adding an
            // account if they have a manually configured chat account.
            boolean isManual = "Manual".equals(ConfigurationUtils.getImProvSource());
            boolean existingImAccount = AccountUtils.getImAccount() != null;

            boolean showNewAccountMenuItem = !existingImAccount && isManual;
            logger.debug("New account permitted? ", showNewAccountMenuItem,
                         " is Manual ? ",  isManual,  "existing account? ", existingImAccount);

            newAccountMenuItem.setVisible(showNewAccountMenuItem );

            // Make the 'chat offline' menu item visible if the user has any
            // chat accounts.
            chatOfflineMenuItem.setVisible(existingImAccount);

            if (existingImAccount)
            {
                boolean isDisabled = false;
                boolean serverAddressValidated = false;

                // We have at least one chat account, so we need to find out 2
                // things. Firstly, whether it is enabled, so we can set the
                // correct text on the 'sign in/out of chat' menu item.
                // Secondly, whether the server address has been validated
                // (i.e. whether it has ever registered), as we need to disable
                // the 'sign out of chat' menu item if the account as never
                // registered.
                AccountManager accountManager = GuiActivator.getAccountManager();
                List<AccountID> jabberAccounts =
                        accountManager.getStoredAccountsforProtocol(ProtocolNames.JABBER);
                for (AccountID jabberAccount : jabberAccounts)
                {
                    isDisabled = !jabberAccount.isEnabled();
                    serverAddressValidated = jabberAccount.getAccountPropertyBoolean(
                        ProtocolProviderFactory.SERVER_ADDRESS_VALIDATED, false);
                    logger.debug(
                        "Jabber account " + jabberAccount.getAccountAddress() +
                        ", isDisabled? " + isDisabled +
                        ", serverAddressValidated? " + serverAddressValidated);

                    // If we've found a validated jabber account, there's no
                    // need to continue.
                    if (serverAddressValidated)
                    {
                        break;
                    }
                }

                chatOfflineMenuItem.setEnabled(serverAddressValidated);

                // Set the menu text to 'sign in' or 'sign out' depending on
                // whether the account has already been disabled by the user.
                boolean smsEnabled = ConfigurationUtils.isSmsEnabled();
                String enableResource = smsEnabled ?
                    "service.gui.chat.ENABLE_CHAT_SMS" : "service.gui.chat.SIGN_IN_CHAT";
                String disableResource = smsEnabled ?
                    "service.gui.chat.DISABLE_CHAT_SMS" : "service.gui.chat.SIGN_OUT_CHAT";

                String menuResource = isDisabled ? enableResource : disableResource;

                chatOfflineMenuItem.setText(resources.getI18NString(menuResource));
                chatOfflineMenuItem.setMnemonic(resources.getI18nMnemonic(menuResource));
            }

            // Only make the 'new group chat' menu option visible if multi
            // user chat is enabled.
            if (ConfigurationUtils.isMultiUserChatEnabled())
            {
                logger.debug("MUC is enabled - show 'new group chat' button");
                newGroupChatItem.setVisible(true);
            }
            else
            {
                logger.debug("MUC is disabled - hide 'new group chat' button");
                newGroupChatItem.setVisible(false);
            }

            // Disable the 'new group chat' option if there are no registered
            // chat providers.
            boolean registeredChatProviders = AccountUtils.isImProviderRegistered();
            logger.debug(
                "Registered chat providers = " + registeredChatProviders);
            newGroupChatItem.setEnabled(registeredChatProviders);
            chatItemsSeparator.setVisible(true);
        }
        else
        {
            logger.debug("IM is disabled");

            newAccountMenuItem.setVisible(false);
            chatOfflineMenuItem.setVisible(false);
            newGroupChatItem.setVisible(false);
            chatItemsSeparator.setVisible(false);
        }
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference<?>[] serRefs = null;

        Container container = Container.CONTAINER_FILE_MENU;
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
     * Adds the plugin component contained in the event to this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        logger.debug("Plugin component added");
        final PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_FILE_MENU))
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    // Add new items to the start, since Quit should always
                    // be at the end.
                    add((Component) c.getComponent(), 0);
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
        logger.debug("Plugin component removed");
        final PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_FILE_MENU))
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
     * Loads icons.
     */
    public void loadSkin()
    {
        if (ConfigurationUtils.isMenuIconsDisabled())
        {
            return;
        }

        ImageLoaderService imageLoaderService =
                                        GuiActivator.getImageLoaderService();

        if (newAccountMenuItem != null)
        {
            imageLoaderService.getImage(ImageLoaderService.ADD_ACCOUNT_MENU_ICON)
            .getImageIcon()
            .addToMenuItem(newAccountMenuItem);
        }

        if (addContactItem != null)
        {
            imageLoaderService.getImage(ImageLoaderService.ADD_CONTACT_16x16_ICON)
            .getImageIcon()
            .addToMenuItem(addContactItem);
        }

        if(logOutMenuItem != null)
        {
            imageLoaderService.getImage(ImageLoaderService.LOG_OUT_16x16_ICON)
            .getImageIcon()
            .addToMenuItem(logOutMenuItem);
        }

        if(closeMenuItem != null)
        {
            imageLoaderService.getImage(ImageLoaderService.QUIT_16x16_ICON)
            .getImageIcon()
            .addToMenuItem(closeMenuItem);
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        logger.user(itemName + " clicked in the File menu");

        switch (itemName)
        {
            case NEW_ACCOUNT:
                NewAccountDialog.showNewAccountDialog();
                break;
            case MUTE:
                // Get the mute state from the SoundNotificationHandler. There
                // should only be one
                for (NotificationHandler handler :
                        GuiActivator.getNotificationService()
                                .getActionHandlers(NotificationAction.ACTION_SOUND))
                {
                    if (handler instanceof SoundNotificationHandler)
                    {
                        SoundNotificationHandler soundHandler
                                = (SoundNotificationHandler) handler;
                        soundHandler.setMute(!soundHandler.isMute());
                        break;
                    }
                }
                break;
            case JOIN_CONFERENCE:
                conferenceService.showJoinConferenceDialog();
                break;
            case OFFLINE_ACCOUNT:
                List<AccountID> jabberAccounts =
                        accountManager.getStoredAccountsforProtocol("Jabber");

                for (AccountID jabberAccount : jabberAccounts)
                {
                    accountManager.toggleAccountEnabled(jabberAccount);
                }

                // Refresh message history so we don't display message history items
                // when signed out of chat.
                GuiActivator.getMainFrameContactList().messageHistoryChanged();
                break;
            case ADD_CONTACT:
                ManageContactWindow addContactWindow =
                        GuiActivator.getAddContactWindow(null);

                if (addContactWindow != null)
                {
                    addContactWindow.setVisible(true);
                }
                break;
            case NEW_GROUP_CHAT:
                new AddChatParticipantsDialog(
                        resources.getI18NString(
                                "service.gui.chat.CREATE_NEW_GROUP"),
                        resources.getI18NString("service.gui.CREATE_GROUP_CHAT"),
                        null,
                        true,
                        null).setVisible(true);
                break;
            case LOG_OUT:
                logOutActionPerformed();
                break;
            case CLOSE:
                closeActionPerformed();
                break;
        }
    }

    /**
     * Indicates that the log out menu has been selected.  This removes user
     * configuration and restarts Accession Desktop, in a similar manner to
     * Clear Data closing Accession Desktop.
     */
    private void logOutActionPerformed()
    {
        logger.info("Logout selected.");
        beginShutdown(true, null);
    }

    /**
     * Indicates that the close menu has been selected.
     */
    private void closeActionPerformed()
    {
        logger.info("Close selected so don't log out.");
        beginShutdown(false, null);
    }

    /**
     * Asks the UI service to shut down the client.
     *
     * @param logOut if true, log out the current user and restart showing an
     *               empty login dialog.  Otherwise, simply quit leaving the
     *               current user logged in.
     * @param macQuitResponse will be null unless the request to quit came from
     *                        the Mac OS.  In that case, we will call back to the
     *                        macQuitResponse once this method has finished executing.
     */
    void beginShutdown(boolean logOut, @Nullable QuitResponse macQuitResponse)
    {
        logger.info("Begin shutdown: Logout? " + logOut +
                     ", Mac QuitResponse: " + macQuitResponse);
        GuiActivator.getUIService().beginShutdown(logOut, false, macQuitResponse, true);
    }

    /**
     * Registers the close menu item.
     */
    private void registerCloseMenuItem()
    {
        UIService uiService = GuiActivator.getUIService();
        if ((uiService == null) || !uiService.useMacOSXScreenMenuBar()
            || !registerCloseMenuItemMacOSX())
        {
            registerCloseMenuItemNonMacOSX();
        }
    }

    /**
     * Registers the close menu item for the MacOSX platform.
     * @return <tt>true</tt> if the operation succeeded, <tt>false</tt> -
     * otherwise
     */
    private boolean registerCloseMenuItemMacOSX()
    {
        return registerMenuItemMacOSX("Quit", this);
    }

    /**
     * Registers the close menu item for the MacOSX platform.
     * @param menuItemText the name of the item
     * @param userData the user data
     * @return <tt>true</tt> if the operation succeeded, <tt>false</tt> -
     * otherwise
     */
    static boolean registerMenuItemMacOSX(String menuItemText, Object userData)
    {
        Exception exception = null;
        try
        {
            Class<?> clazz = Class.forName(
                "net.java.sip.communicator.impl.gui.main.menus.MacOSX"
                + menuItemText + "Registration");
            Method method = clazz.getMethod("run", new Class[]
            { Object.class });
            Object result = method.invoke(null, new Object[]
            { userData });

            if (result instanceof Boolean)
                return (Boolean) result;
        }
        catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex)
        {
            exception = ex;
        }
        if (exception != null)
            logger.error("Failed to register Mac OS X-specific " + menuItemText
                + " handling.", exception);
        return false;
    }

    /**
     * Registers the close menu item for all NON-MacOSX platforms.
     */
    private void registerCloseMenuItemNonMacOSX()
    {
        closeMenuItem = new ResMenuItem("service.gui.QUIT");

        if (!isChatMenu)
        {
            add(closeMenuItem);
            closeMenuItem.setName(CLOSE);
            closeMenuItem.addActionListener(this);
        }
    }

    /**
     * Registers the log out menu item.
     */
    private void registerLogOutMenuItem()
    {
        logOutMenuItem = new ResMenuItem("service.gui.LOG_OUT");

        if (!isChatMenu)
        {
            add(logOutMenuItem);
            logOutMenuItem.setName(LOG_OUT);
            logOutMenuItem.addActionListener(this);
        }
    }

    @Override
    public void fireMenuSelected()
    {
        logger.debug("Menu selected - refreshing menu items first");
        updateChatMenuItems();

        if (joinConferenceItem != null)
        {
            joinConferenceItem.setEnabled(conferenceService.isJoinEnabled());
        }

        if (createGroupContactItem != null)
        {
          createGroupContactItem.setVisible(ConfigurationUtils.groupContactsSupported());
          GroupContactMenuUtils.setMenuItemEnabled(createGroupContactItem);
        }

        super.fireMenuSelected();
    }
}
