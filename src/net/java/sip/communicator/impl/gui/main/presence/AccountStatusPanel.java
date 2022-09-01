/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.image.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.globaldisplaydetails.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.threading.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The panel shown on the top of the contact list. It contains user name,
 * current status menu and the avatar of the user.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class AccountStatusPanel
    extends TransparentPanel
    implements  RegistrationStateChangeListener,
                PluginComponentListener,
                GlobalDisplayDetailsListener,
                Skinnable
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>AccountStatusPanel</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger =
        Logger.getLogger(AccountStatusPanel.class);

    /**
     * Config option which controls whether or not we should show the explanation
     * dialog for why we are in a meeting.
     */
    private static final String CFG_NOT_IN_MEETING =
                                "impl.gui.presence.SHOW_NOT_IN_MEETING_EXPLAIN";

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
     * Padding at top of panel.
     */
    private static final int PADDING_TOP = 7;

    /**
     * Padding at bottom of panel.
     */
    private static final int PADDING_BOTTOM = 9;

    /**
     * The prefix of the config string used by the reconnect plugin to track
     * whether accounts are online.
     */
    private static final String RECONNECTPLUGIN_PREFIX =
        "net.java.sip.communicator.plugin.reconnectplugin." +
            "ATLEAST_ONE_SUCCESSFUL_CONNECTION.";

    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID =
        AccountStatusPanel.class.getName() +  "OpaquePanelUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            SIPCommOpaquePanelUI.class.getName());
    }

    /**
     * Dialog shown to ask the user to confirm leaving a meeting
     */
    private final SIPCommConfirmDialog mConfirmDialog =
        new SIPCommConfirmDialog("service.gui.presence.IN_MEETING_DIALOG_TITLE",
                                 "service.gui.presence.IN_MEETING_DIALOG_MSG",
                                 "service.gui.presence.IN_MEETING_DIALOG_LEAVE",
                                 "service.gui.CANCEL",
                                 CFG_NOT_IN_MEETING);

    /**
     * The image object storing the avatar.
     */
    private final AvatarImageButton accountImageLabel;

    /**
     * The button showing the name of the user.
     */
    private final AccountStatusButton accountStatusButton;

    /**
     * The label showing the name of the user when status can't be changed
     */
    private final JLabel accountStatusLabel = new JLabel(GuiActivator
                       .getResources().getI18NString("service.gui.ACCOUNT_ME"));

    /**
     * The image used for the icon that indicates that the chat account is
     * offline.
     */
    private final JLabel offlineImage;

    /**
     * The label showing the "Sign into chat" link.
     */
    private final JLabel signIntoChatLabel;

    /**
     * The background color property.
     */
    private Color bgColor;

    /**
     * The background image property.
     */
    private BufferedImageFuture logoBgImage;

    /**
     * TexturePaint used to paint background image.
     */
    private TexturePaint texture;

    /**
     * The south plug-in container.
     */
    private final TransparentPanel southPluginPanel;

    /**
     * The account custom status panel to display the current status
     */
    private final AccountCustomStatusPanel customStatusPanel;

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
     * The status service.
     */
    private final GlobalStatusService statusService =
        GuiActivator.getGlobalStatusService();

    /**
     * True if there is a Call Manager UI - i.e. either ECM or BCM are enabled
     */
    private boolean mHaveCallManager;

    /**
     * Creates an instance of <tt>AccountStatusPanel</tt> by specifying the
     * main window, where this panel is added.
     * @param mainFrame the main window, where this panel is added
     */
    public AccountStatusPanel(MainFrame mainFrame)
    {
        super(new BorderLayout(5, 0));

        setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
                    new EmptyBorder(PADDING_TOP, 0, PADDING_BOTTOM, 0)));

        this.accountImageLabel = new AvatarImageButton(mainFrame);

        updateAvatarLabelVisibility();

        // Initialize the icon that indicates that the chat account has gone
        // offline to default values, then update it to reflect the current
        // state of the chat account.
        offlineImage = new JLabel();
        updateChatOfflineIcon();

        // Not in meeting label - shown when the user is in a meeting, clicking
        // on it allows the user to override the meeting state
        String notInMeetingString = resources.getI18NString(
                                  "service.protocol.status.gui.NOT_IN_MEETING");
        final JLabel notInMeetingLabel = createWarningLabel(notInMeetingString, true);

        // No connection label - shown when there is no connection.
        String noConnectionString = resources.getI18NString(
                             "plugin.sipregistrationnotifier.MAIN_WINDOW.GENERAL");
        final JLabel noConnectionLabel = createWarningLabel(noConnectionString, false);
        resources.getImage("service.gui.icons.CHAT_OFFLINE_ERROR_ICON").addToLabel(noConnectionLabel);

        // Sign into chat label - shown when the user has signed out of chat.
        // Clicking it signs the user back into chat
        String signIntoChatString = resources.getI18NString(
                                               "service.gui.chat.SIGN_IN_CHAT");
        signIntoChatLabel = createWarningLabel(signIntoChatString, true);
        signIntoChatLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                logger.user("Sign into chat clicked in main window");
                AccountID imAccount = AccountUtils.getImAccount();

                if (imAccount != null && !imAccount.isEnabled())
                {
                    GuiActivator.getAccountManager().toggleAccountEnabled(imAccount);
                }
            }
        });

        accountStatusButton = new AccountStatusButton(statusService);

        // Create the custom status panel
        customStatusPanel = new AccountCustomStatusPanel();

        JPanel centreWrapper = new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        centreWrapper.add(accountStatusButton);
        centreWrapper.add(accountStatusLabel);

        JPanel rightWrapper = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrapper.add(notInMeetingLabel);
        rightWrapper.add(noConnectionLabel);
        rightWrapper.add(signIntoChatLabel);

        TransparentPanel rightPanel = new TransparentPanel();
        rightPanel.setLayout(new BorderLayout(0, 0));
        rightPanel.add(centreWrapper, BorderLayout.CENTER);
        rightPanel.add(customStatusPanel, BorderLayout.SOUTH);
        rightPanel.add(rightWrapper, BorderLayout.EAST);

        statusService.addStatusChangeListener(new GlobalStatusChangeListener()
        {
            @Override
            public void onStatusChanged()
            {
                if (!SwingUtilities.isEventDispatchThread())
                {
                    SwingUtilities.invokeLater(() -> onStatusChanged());
                    return;
                }

                // Update the status labels
                updateStatusLabels();

                // Get the global status
                GlobalStatusEnum globalStatus = statusService.getGlobalStatus();

                // Update the meeting label
                boolean inMeeting = globalStatus == GlobalStatusEnum.IN_A_MEETING;
                notInMeetingLabel.setVisible(inMeeting);
                if (!inMeeting)
                {
                    mConfirmDialog.setVisible(false);
                }

                // No connection should be visible if we are offline and there is
                // either no IM account, or we have one but it is signed in.
                boolean offline = statusService.isOffline();
                AccountID imAccount = AccountUtils.getImAccount();
                boolean haveImAccount = (imAccount != null);
                boolean signedIntoChat = haveImAccount && imAccount.isEnabled();
                boolean noConnection = offline && (!haveImAccount || signedIntoChat);

                if (!noConnection)
                {
                    // We have a connection!  Thus hide the no connection object
                    noConnectionLabel.setVisible(false);
                }
                else
                {
                    // We don't have a connection.  But, we don't want to show
                    // the label immediately, as this could be transitory. So
                    // Check again in 3 seconds time.
                    ThreadingService threadingService =
                                             GuiActivator.getThreadingService();
                    threadingService.schedule("NoconnectionVisibiliser",
                                              new CancellableRunnable()
                    {
                        @Override
                        public void run()
                        {
                            logger.entry();
                            boolean offline = statusService.isOffline();
                            AccountID imAccount = AccountUtils.getImAccount();
                            boolean haveImAccount = imAccount != null;
                            boolean signedIntoChat = haveImAccount &&
                                                     imAccount.isEnabled();
                            noConnectionLabel.setVisible(offline &&
                                           (!haveImAccount || signedIntoChat));
                            logger.exit();
                        }
                    }, 3000);
                }
            }
        });

        notInMeetingLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                logger.user("'Not in a meeting' button clicked in main window");
                if (configService.user().getBoolean(CFG_NOT_IN_MEETING, false))
                {
                    // User has asked not to be asked again - just leave the
                    // meeting
                    logger.user("Do not ask again selected");
                    statusService.setInMeeting(false);
                }
                else
                {
                    logger.info("Will show user the meeting confirmation dialog");
                    // Show a dialog to the user explaining what this does
                    // Needs to be done on a new thread as the call can block:
                    new Thread("MeetingDialogConfirmation")
                    {
                        @Override
                        public void run()
                        {
                            logger.info("Displaying confirmation dialog");
                            if (mConfirmDialog.showDialog())
                            {
                                // User has confirmed they are not in a meeting
                                logger.user("Not in a meeting confirmed");
                                statusService.setInMeeting(false);
                            }
                            else
                            {
                                logger.user("Not in a meeting dialog cancelled");
                            }
                        }
                    }.start();
                }

                // Mark all current meetings as left.
                for (ProtocolProviderService provider :
                          GuiActivator.getProviders(OperationSetCalendar.class))
                {
                    if (provider != null)
                    {
                        provider.getOperationSet(OperationSetCalendar.class)
                                                          .markMeetingsAsLeft();
                    }
                }
            }
        });

        this.add(accountImageLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

        southPluginPanel = new TransparentPanel(new BorderLayout());

        new PluginContainer(
            southPluginPanel,
            Container.CONTAINER_ACCOUNT_SOUTH);

        this.add(southPluginPanel, BorderLayout.SOUTH);

        loadSkin();

        GuiActivator.getUIService().addPluginComponentListener(this);
        GuiActivator.getGlobalDisplayDetailsService()
            .addGlobalDisplayDetailsListener(this);

        String globalDisplayName
            = GuiActivator.getGlobalDisplayDetailsService()
                .getGlobalDisplayName();

        if(!StringUtils.isNullOrEmpty(globalDisplayName))
        {
            accountStatusButton.setText(globalDisplayName);
            accountStatusLabel.setText(globalDisplayName);
        }

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
                    if (oldValue == null || newValue == null ||
                        !oldValue.equals(newValue))
                    {
                        logger.debug(propertyName + " has changed from " +
                            oldValue + " to " + newValue +
                            ". Updating chat offline icon.");

                        // Send an analytics event to log the change.
                        AnalyticsEventType event = connectionChanged ?
                            AnalyticsEventType.IM_CONNECTED :
                            AnalyticsEventType.IM_SIGNED_IN;

                        GuiActivator.getAnalyticsService().onEvent(
                            event , "newValue", newValue);
                        AccountID imAccount = AccountUtils.getImAccount();
                        GuiActivator.getAccountManager().refreshAccountInfo();

                        // Either the reconnectplugin thinks the account has
                        // gone online/offline or the jabber account disabled
                        // config has changed, so we need to update the chat
                        // offline icon.
                        updateChatOfflineIcon();

                        // Update the main UI information to make sure we hide
                        // the "Sign into chat" option when the chat account
                        // is removed.
                        updateStatusLabels();
                    }
                }
            }
        });

        // Listen for the call manager being disabled:
        GuiActivator.getCosService().getClassOfService(new CPCosGetterCallback()
        {
            @Override
            public void onCosReceived(CPCos cos)
            {
                boolean ecm = cos.getIchAllowed() &&
                              "ecm".equals(cos.getIchServiceLevel());

                boolean bcm = !cos.getIchAllowed() &&
                              cos.getBCMSubscribed();

                mHaveCallManager = ecm || bcm;
                updateStatusLabels();
            }
        });
    }

    /**
     * Creates a warning label for displaying various states
     *
     * @param text The text of the label
     * @param clickable True if the text is clickable
     * @return the created label
     */
    private JLabel createWarningLabel(String text, boolean clickable)
    {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(GuiActivator.getResources().getColor(
                "service.gui.MAIN_FRAME_HEADER_TEXT",
                GuiActivator.getResources().getColor("service.gui.DARK_TEXT"),
                false)));

        // Set the font to be a light grey, italics.  Possibly underlined.
        Font font = label.getFont();

        if (clickable)
        {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Map<TextAttribute, Integer> fontAttributes = new HashMap<>();
            fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            font = font.deriveFont(fontAttributes);
        }

        label.setFont(font.deriveFont(Font.ITALIC).
            deriveFont(ScaleUtils.getScaledFontSize(11f)));

        return label;
    }

    /**
     * Updates the visibility of the status labels depending on whether or not
     * the user can change the status.
     * Also updates the visibility of the "Sign into chat" label, which is only
     * shown if the user is offline, has an IM account and is not signed into it.
     */
    private synchronized void updateStatusLabels()
    {
        AccountID imAccount = AccountUtils.getImAccount();
        boolean haveImAccount = (imAccount != null);
        boolean signedIntoChat = haveImAccount && imAccount.isEnabled();
        boolean offline = statusService.isOffline();
        boolean canChangeStatus = mHaveCallManager || haveImAccount;
        boolean canLogIntoChat = offline && haveImAccount && !signedIntoChat;

        logger.debug("Can change status now " + canChangeStatus);
        customStatusPanel.setVisible(canChangeStatus);
        accountStatusButton.setVisible(canChangeStatus);
        accountStatusLabel.setVisible(!canChangeStatus);

        logger.debug("Can log into chat now " + canLogIntoChat);
        signIntoChatLabel.setVisible(canLogIntoChat);
    }

    /**
     * Adds the account given by <tt>protocolProvider</tt> in the contained
     * status combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account to add
     */
    public void addAccount(final ProtocolProviderService protocolProvider)
    {
        updateAvatarLabelVisibility();
    }

    /**
     * Removes the account given by <tt>protocolProvider</tt> from the contained
     * status combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account to remove
     */
    public void removeAccount(final ProtocolProviderService protocolProvider)
    {
        updateAvatarLabelVisibility();
    }

    /**
     * Updates the image that is shown.
     * @param img the new image.
     */
    public void updateImage(ImageIcon img)
    {
        accountImageLabel.updateImage(img);
    }

    /**
     * Updates the image that is shown when the chat account is offline.
     */
    private void updateChatOfflineIcon()
    {
        // Updates an icon thus must be run on the EDT
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    updateChatOfflineIcon();
                }
            });

            return;
        }

        logger.debug("Updating the 'chat offline' icon");

        // Initialise the icon and tooltip resource to null and only set them
        // if we find that the chat account is offline.
        ImageIconFuture offlineIcon = null;
        String tooltipResource = null;

        // Get a list of all chat accounts.
        List<String> chatAccounts = getChatAccounts();

        if (chatAccounts.size() > 0)
        {
            logger.debug("Found a chat account");

            String chatAccount = null;
            boolean isDisabled = false;
            ImageID offlineImageID = null;

            // Find out whether the chat account has been disabled by the
            // user.
            for (String account : chatAccounts)
            {
                String propertyName = account + ACCOUNT_DISABLED_SUFFIX;
                String disabled = configService.user().getString(propertyName);
                chatAccount = account;

                if (disabled != null)
                {
                    isDisabled = Boolean.valueOf(disabled);

                    // We've found chat account config, so no need to keep
                    // looking.
                    break;
                }
            }

            // Update the text in the 'chat offline' menu item based on
            // whether the chat account is enabled.
            if (isDisabled)
            {
                logger.debug("User has taken the chat account offline");
                offlineImageID = ImageLoaderService.CHAT_OFFLINE_USER_ICON;
                tooltipResource = "service.gui.chat.CHAT_OFFLINE";
                offlineImage.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent evt)
                    {
                        // Select the chat history tab
                        AbstractMainFrame mainFrame = GuiActivator.getUIService().getMainFrame();
                        mainFrame.selectTab(MainFrameTabComponent.HISTORY_TAB_NAME);
                        mainFrame.selectSubTab(MainFrame.HistorySubTab.chatsName);
                    }
                });
            }
            else
            {
                logger.debug("User had not taken the chat account offline");

                // The user has not disabled the chat account, so check
                // whether the reconnect plugin thinks this account is
                // connected.
                String propertyName = chatAccount + ".ACCOUNT_UID";
                String accountUID = configService.user().getString(propertyName);

                String accountConnectedConfig =
                    RECONNECTPLUGIN_PREFIX + accountUID;

                if (!configService.user().getBoolean(accountConnectedConfig, true))
                {
                    logger.debug("Chat account has gone offline");
                    offlineImageID = ImageLoaderService.CHAT_OFFLINE_ERROR_ICON;
                    tooltipResource = "service.gui.chat.ERROR_CONTACTING_SERVER";
                }
            }

            if (offlineImageID != null)
            {
                // We've set an image ID for the offline icon, so set the
                // offlineIcon accordingly.
                offlineIcon = GuiActivator.getImageLoaderService().getImage(offlineImageID).getImageIcon();
            }
        }

        // If the offlineIcon or tooltipResource are still null, the account
        // is online, so make the offline image invisible.  Otherwise, update
        // the image to be the latest one.
        if (offlineIcon == null || tooltipResource == null)
        {
            logger.debug("New offline icon is null - hiding offline image");
            offlineImage.setVisible(false);
        }
        else
        {
            logger.debug("New offline icon is not null - updating offline image");
            offlineIcon.addToLabel(offlineImage);
            offlineImage.setToolTipText(resources.getI18NString(tooltipResource));
            offlineImage.setVisible(true);
            revalidate();
            repaint();
        }
    }

    /**
     * Starts connecting user interface for the given <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to start
     * connecting for
     */
    public void startConnecting(final ProtocolProviderService protocolProvider)
    {
        updateAvatarLabelVisibility();
    }

    /**
     * Stops connecting user interface for the given <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to stop
     * connecting for
     */
    public void stopConnecting(final ProtocolProviderService protocolProvider)
    {
        updateAvatarLabelVisibility();
    }

    /**
     * Paints this component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (logoBgImage != null)
        {
            Graphics2D g2 = (Graphics2D) g;

            g.setColor(bgColor);
            g2.setPaint(texture);
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());

            Image bgImage = logoBgImage.resolve();

            g.drawImage(
                bgImage,
                this.getWidth() - bgImage.getWidth(null),
                0,
                null);
        }
    }

    /**
     * Indicates that a plug-in component is registered to be added in a
     * container. If the plug-in component in the given event is registered for
     * this container then we add it.
     * @param event <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent pluginComponent = event.getPluginComponent();
        Container containerID = pluginComponent.getContainer();
        /*
        // avoid early creating of components by calling getComponent
        Object component = pluginComponent.getComponent();

        if (!(component instanceof Component))
            return;
        */

        if (containerID.equals(Container.CONTAINER_ACCOUNT_SOUTH) ||
            containerID.equals(Container.CONTAINER_ACCOUNT_STATUS_SOUTH))
        {
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Indicates that a plug-in component is registered to be removed from a
     * container. If the plug-in component in the given event is registered for
     * this container then we remove it.
     * @param event <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent pluginComponent = event.getPluginComponent();
        Container pluginContainer = pluginComponent.getContainer();
        /*Object component = pluginComponent.getComponent();

        if (!(component instanceof Component))
            return;
        */

        if (pluginContainer.equals(Container.CONTAINER_ACCOUNT_SOUTH) ||
            pluginContainer.equals(Container.CONTAINER_ACCOUNT_STATUS_SOUTH))
        {
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Called whenever a new avatar is defined for one of the protocols that we
     * have subscribed for.
     *
     * @param event the event containing the new image
     */
    public void globalDisplayNameChanged(
            final GlobalDisplayNameChangeEvent event)
    {
        if (!SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    globalDisplayNameChanged(event);
                }
            });

        String displayName = event.getNewDisplayName();

        if(!StringUtils.isNullOrEmpty(displayName))
        {
            accountStatusButton.setText(displayName);
            accountStatusLabel.setText(displayName);
        }
    }

    /**
     * Called whenever a new avatar is defined for one of the protocols that we
     * have subscribed for.
     *
     * @param event the event containing the new image
     */
    public void globalDisplayAvatarChanged(
            final GlobalAvatarChangeEvent event)
    {
        if (!SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    globalDisplayAvatarChanged(event);
                }
            });

        BufferedImageFuture avatarImage = event.getNewAvatar();

        // If there is no avatar image set, then displays the default one.
        if (avatarImage != null && avatarImage.resolve() != null)
        {
            logger.debug("Avatar has changed - set new icon");
            accountImageLabel.setImageIcon(avatarImage.getImageIcon());
        }
    }

    /**
     * Updates account information when a protocol provider is registered.
     * @param evt the <tt>RegistrationStateChangeEvent</tt> that notified us
     * of the change
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        ProtocolProviderService protocolProvider = evt.getProvider();

        // There is nothing we can do when account is registering,
        // will set only connecting state later.
        // While dispatching the registering if the state of the provider
        // changes to registered we may end with client logged off
        // this may happen if registered is coming too quickly after registered
        // Dispatching registering is doing some swing stuff which
        // is scheduled in EDT and so can be executing when already registered
        if (evt.getNewState().equals(RegistrationState.REGISTERING))
        {
            startConnecting(protocolProvider);
        }
    }

    /**
     * Loads images for the account status panel.
     */
    public void loadSkin()
    {
        ImageLoaderService imageLoaderService =
                        GuiActivator.getImageLoaderService();

        bgColor
            = new Color(GuiActivator.getResources()
                .getColor("service.gui.LOGO_BAR_BACKGROUND"));

        logoBgImage
            = imageLoaderService.getImage(ImageLoaderService.WINDOW_TITLE_BAR);

        // texture
        BufferedImage bgImage
            = imageLoaderService.getImage(ImageLoaderService.WINDOW_TITLE_BAR_BG)
            .resolve();
        texture
            = new TexturePaint(
                    bgImage,
                    new Rectangle(
                            0,
                            0,
                            bgImage.getWidth(null),
                            bgImage.getHeight(null)));

        GuiActivator.getUIService().addPluginComponentListener(this);

        BufferedImageFuture avatar = GuiActivator.getGlobalDisplayDetailsService()
            .getGlobalDisplayAvatar();
        if (avatar == null)
            accountImageLabel.setImageIcon(imageLoaderService
                .getImage(ImageLoaderService.DEFAULT_USER_PHOTO)
                .getImageIcon());
        else
            accountImageLabel.setImageIcon(avatar.getImageIcon());
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID()
    {
        if(ConfigurationUtils.isTransparentWindowEnabled())
            return uiClassID;
        else
            return super.getUIClassID();
    }

    /**
     * @return A list of the account IDs of all chat accounts in the current
     *         config.
     */
    private List<String> getChatAccounts()
    {
        logger.debug("Getting a list of jabber accounts");
        List<String> potentialJabberAccs =
            configService.user().getPropertyNamesByPrefix(JABBER_ACC_CONFIG_PREFIX ,
                                                   true);

        List<String> jabberAccs = new ArrayList<>();

        for (String potentialAcc : potentialJabberAccs)
        {
            String value = configService.user().getString(potentialAcc);

            if (value != null && value.startsWith("acc"))
            {
                jabberAccs.add(potentialAcc);
            }
        }

        return jabberAccs;
    }

    /**
     * Determine whether the framed avatar image should be visible, and update
     * the UI accordingly.
     */
    private synchronized void updateAvatarLabelVisibility()
    {
        final boolean shouldBeVisible = (getChatAccounts().size() != 0);

        if (shouldBeVisible != accountImageLabel.isVisible())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    accountImageLabel.setVisible(shouldBeVisible);
                }
            });
        }
    }
}
