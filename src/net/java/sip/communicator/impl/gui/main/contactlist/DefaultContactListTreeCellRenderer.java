/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.JPopupMenu.Separator;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import net.java.sip.communicator.impl.gui.AbstractUIServiceImpl;
import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.MetaUIContact;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.ShowMoreContact;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.SourceUIContact;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.plugin.desktoputil.AntialiasingManager;
import net.java.sip.communicator.plugin.desktoputil.ComponentUtils;
import net.java.sip.communicator.plugin.desktoputil.CreateConferenceMenu;
import net.java.sip.communicator.plugin.desktoputil.SIPCommButton;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.analytics.AnalyticsParameterSimple;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactsource.ContactChangedEvent;
import net.java.sip.communicator.service.contactsource.ContactDetail;
import net.java.sip.communicator.service.contactsource.ContactQuery;
import net.java.sip.communicator.service.contactsource.ContactQueryListener;
import net.java.sip.communicator.service.contactsource.ContactQueryStatusEvent;
import net.java.sip.communicator.service.contactsource.ContactReceivedEvent;
import net.java.sip.communicator.service.contactsource.ContactRemovedEvent;
import net.java.sip.communicator.service.contactsource.ContactSourceService;
import net.java.sip.communicator.service.contactsource.ExtendedContactSourceService;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.gui.ContactListFilter;
import net.java.sip.communicator.service.gui.UIContact;
import net.java.sip.communicator.service.gui.UIContactDetail;
import net.java.sip.communicator.service.gui.UIGroup;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.managecontact.ManageContactService;
import net.java.sip.communicator.service.phonenumberutils.PhoneNumberUtilsService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredContactInfo;
import net.java.sip.communicator.service.protocol.OperationSetVideoTelephony;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.FaxDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PagerDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PhoneNumberDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.VideoDetail;
import net.java.sip.communicator.service.resources.ImageID;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.AccountUtils;
import net.java.sip.communicator.util.skin.Skinnable;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.service.resources.ImageIconFuture;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.StringUtils;

/**
 * The <tt>ContactListCellRenderer</tt> is the custom cell renderer used in the
 * Jitsi's <tt>ContactList</tt>. It extends JPanel instead of JLabel,
 * which allows adding different buttons and icons to the contact cell. The cell
 * border and background are repainted.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class DefaultContactListTreeCellRenderer
    extends JPanel
    implements  TreeCellRenderer,
                Icon,
                Skinnable,
                ContactListTreeCellRenderer,
                ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Logger of this class
     */
    private static final Logger sLog
        = Logger.getLogger(ContactListTreeCellRenderer.class);

    AnalyticsService mAnalytics = GuiActivator.getAnalyticsService();
    static ResourceManagementService resources = GuiActivator.getResources();

    /**
     * Constants used to initialise default cell component sizes.
     */
    private static final int DEFAULT_ROW_HEIGHT = ScaleUtils.scaleInt(35);
    private static final int DEFAULT_SELECTED_ROW_HEIGHT = ScaleUtils.scaleInt(70);
    private static final int DEFAULT_SHOW_MORE_HEIGHT = ScaleUtils.scaleInt(20);
    private static final int DEFAULT_BORDER_SIZE = ScaleUtils.scaleInt(2);
    private static final int DEFAULT_LEFT_BORDER_SIZE = ScaleUtils.scaleInt(5);

    /**
     * The default height of the avatar.
     */
    protected int mAvatarHeight;

    /**
     * The default width of the avatar.
     */
    protected int mAvatarWidth;

    /**
     * The extended height of the avatar.
     */
    protected int mExtendedAvatarHeight;

    /**
     * The extended width of the avatar.
     */
    protected int mExtendedAvatarWidth;

    /**
     * The height of a row when unselected.
     */
    protected int mRowHeight;

    /**
     * The height of a row when selected.
     */
    protected int mSelectedRowHeight;

    /**
     * The height of the 'show more' row.
     */
    protected int mShowMoreHeight;

    /**
     * The default height of the button.
     */
    protected static final int BUTTON_HEIGHT = ScaleUtils.scaleInt(27);

    /**
     * Left border value.
     */
    protected int mLeftBorder;

    /**
     * Left border value.
     */
    protected int mTopBorder;

    /**
     * Bottom border value.
     */
    protected int mBottomBorder;

    /**
     * Right border value.
     */
    protected int mRightBorder;

    /**
     * Diameter of the rounded corner arcs for the 'selected' background.
     */
    private static final int SELECTION_BG_CORNER_DIAMETER =
                                                         ScaleUtils.scaleInt(6);

    /**
     * The horizontal gap between columns in pixels;
     */
    protected static final int H_GAP = ScaleUtils.scaleInt(2);

    /**
     * The vertical gap between rows in pixels;
     */
    protected static final int V_GAP = ScaleUtils.scaleInt(3);

    /**
     * The image loader service
     */
    protected static final ImageLoaderService sImageLoaderService =
                                           GuiActivator.getImageLoaderService();

    /**
     * Threading service
     */
    private static final ThreadingService sThreadingService =
                                             GuiActivator.getThreadingService();

    /**
     * Phone number utils, used for format phone numbers for display in the UI.
     */
    private static final PhoneNumberUtilsService sPhoneNumberUtils =
                                             GuiActivator.getPhoneNumberUtils();

    /**
     * Property to determine whether video-specific UI is to be shown.
     */
    private static final String DISABLE_VIDEO_UI_PROP =
        "net.java.sip.communicator.impl.gui.DISABLE_VIDEO_UI";

    /**
     * The resource string used for 'and'
     */
    private static final String AND = resources.getI18NString("service.gui.AND");

    /**
     * The default status icon to use if we don't know a contact's status.
     */
    protected static final ImageIconFuture USER_NO_STATUS_ICON =
        sImageLoaderService.getImage(
            ImageLoaderService.USER_NO_STATUS_ICON).getImageIcon();

    /**
     * The icon used for opened groups.
     */
    private ImageIconFuture mOpenedGroupIcon;

    /**
     * The icon used for closed groups.
     */
    private ImageIconFuture mClosedGroupIcon;

    /**
     * The foreground color for groups.
     */
    private Color mGroupForegroundColor;

    /**
     * The foreground color for contacts.
     */
    private Color mContactForegroundColor;

    /**
     * The UIContact that the mouse was last over
     */
    protected UIContact mMouseOverContact = null;

    /**
     * The component showing the name of the contact or group.
     */
    protected final JLabel mNameLabel = new JLabel();

    /**
     * The status message label.
     */
    protected final JLabel mDisplayDetailsLabel = new JLabel();

    /**
     * The selected/unselected icon
     */
    private final JLabel mSelectedLabel = new JLabel();

    /**
     * The call button.
     */
    protected final SIPCommButton mRolloverCallButton = new SIPCommButton();

    /**
     * The invite to meeting button.
     */
    protected final SIPCommButton mRolloverInviteToMeetingButton = new SIPCommButton();

    /**
     * The call video button.
     */
    protected final SIPCommButton mRolloverCallVideoButton = new SIPCommButton();

    /**
     * The chat button.
     */
    protected final SIPCommButton mRolloverChatButton = new SIPCommButton();

    /**
     * The add contact button.
     */
    protected final SIPCommButton mRolloverAddContactButton = new SIPCommButton();

    /**
     * The call button.
     */
    protected final SIPCommButton mCallButton = new SIPCommButton();

    /**
     * The invite to meeting button.
     */
    protected final SIPCommButton mInviteToMeetingButton = new SIPCommButton();

    /**
     * The call video button.
     */
    protected final SIPCommButton mCallVideoButton = new SIPCommButton();

    /**
     * The chat button.
     */
    protected final SIPCommButton mChatButton = new SIPCommButton();

    /**
     * The add contact button.
     */
    protected final SIPCommButton mAddContactButton = new SIPCommButton();

    /**
     * The constraints used to align the right label
     */
    protected final GridBagConstraints mAvatarLabelConstraints =
                                            new GridBagConstraints();

    /**
     * The constraints used to align the status label
     */
    protected final GridBagConstraints mStatusLabelConstraints =
                                            new GridBagConstraints();

    /**
     * The constraints used to align the display details label
     */
    protected final GridBagConstraints mDisplayDetailsLabelConstraints =
                                            new GridBagConstraints();

    /**
     * The constraints used to align the name label
     */
    protected final GridBagConstraints mNameLabelConstraints =
                                            new GridBagConstraints();

    /**
     * The common constraints used for the action buttons
     */
    protected final GridBagConstraints mActionButtonConstraints =
                                            new GridBagConstraints();

    /**
     * The common constraints used for the selected icon
     */
    protected final GridBagConstraints mSelectedIconConstraints =
                                            new GridBagConstraints();

    /**
     * The component showing the avatar or the contact count in the case of
     * groups.
     */
    protected final JLabel mAvatarLabel = new JLabel();

    /**
     * The message received image.
     */
    private BufferedImageFuture mMsgReceivedImage;

    /**
     * The label containing the status icon.
     */
    protected final JLabel mStatusLabel = new JLabel();

    /**
     * The icon showing the contact status.
     */
    protected ImageIconFuture mStatusIcon = null;

    /**
     * Indicates if the current list cell is selected.
     */
    protected boolean mIsSelected = false;

    /**
     * Indicates if the current cell contains a leaf or a group.
     */
    protected TreeNode mTreeNode = null;

    /**
     * The parent tree.
     */
    protected TreeContactList mTreeContactList;

    /**
     * A list of the custom action buttons for contacts UIContacts.
     */
    protected List<JButton> mCustomActionButtons;

    /**
     * A list of the custom action buttons for groups.
     */
    private List<JButton> mCustomActionButtonsUIGroup;

    /**
     * The last added button.
     */
    protected SIPCommButton mLastAddedButton;

    /**
     * Whether tooltips are currently enabled for this tree
     */
    private boolean mToolTipsEnabled = true;

    /**
     * The timer used to reset tooltips when moving between cells
     */
    private Timer mToolTipTimer = new Timer(50, this);

    /**
     * The tooltip to use for this cell renderer
     */
    private String mToolTip;

    /**
     * The image icon to display on a selected cell to show it is selected
     */
    private static ImageIconFuture SELECTED_ICON =
        sImageLoaderService.getImage(ImageLoaderService.CONTACT_LIST_SELECTED)
        .getImageIcon();

    /**
     * The image icon to display on an unselected cell to show it is not selected
     */
    private static ImageIconFuture UNSELECTED_ICON =
        sImageLoaderService.getImage(ImageLoaderService.CONTACT_LIST_UNSELECTED)
        .getImageIcon();

    /**
     * The image icon to display on an unselected disabled cell
     */
    private static ImageIconFuture DISABLED_UNSELECTED_ICON =
        sImageLoaderService.getImage(ImageLoaderService.CONTACT_LIST_DISABLED_UNSELECTED)
        .getImageIcon();

    /**
     * The image icon to display on a selected disabled cell
     */
    private static ImageIconFuture DISABLED_SELECTED_ICON =
        sImageLoaderService.getImage(ImageLoaderService.CONTACT_LIST_DISABLED_SELECTED)
        .getImageIcon();

    /**
     * Initializes the panel containing the node.
     */
    public DefaultContactListTreeCellRenderer()
    {
        super(new GridBagLayout());

        // Initialize class variables.
        mRowHeight = DEFAULT_ROW_HEIGHT;
        mSelectedRowHeight = DEFAULT_SELECTED_ROW_HEIGHT;
        mShowMoreHeight = DEFAULT_SHOW_MORE_HEIGHT;
        mLeftBorder = DEFAULT_LEFT_BORDER_SIZE;
        mTopBorder = DEFAULT_BORDER_SIZE;
        mBottomBorder = DEFAULT_BORDER_SIZE;
        mRightBorder = DEFAULT_BORDER_SIZE;

        initializeLabelConstraints();

        loadSkin();

        setOpaque(true);
        mNameLabel.setOpaque(false);

        ToolTipManager.sharedInstance().setReshowDelay(0);

        mDisplayDetailsLabel.setFont(getFont().deriveFont(
            ScaleUtils.getScaledFontSize(9f)));
        mDisplayDetailsLabel.setForeground(Color.GRAY);

        addLabels(mNameLabelConstraints.gridwidth);

        mCallButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Call button pressed");
                callButtonPressed();
            }
        });

        mRolloverCallButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Rollover call button pressed");
                callButtonPressed();
            }
        });

        mInviteToMeetingButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Start meeting from invite to meeting button");
                inviteButtonPressed();
            }
        });

        mRolloverInviteToMeetingButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Start meeting from invite to meeting rollover button");
                inviteButtonPressed();
            }
        });

        mCallVideoButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if ((mTreeNode != null) && (mTreeNode instanceof ContactNode))
                {
                    sLog.user("Start call from call video button");
                    call(mTreeNode, true);
                }
            }
        });

        mRolloverCallVideoButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if ((mTreeNode != null) && (mTreeNode instanceof ContactNode))
                {
                    sLog.user("Start call from rollover call video button");
                    call(mTreeNode, true);
                }
            }
        });

        mChatButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sLog.debug("Chat button pressed");
                chatButtonPressed();
            }
        });

        mRolloverChatButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sLog.debug("Rollover chat button pressed");
                chatButtonPressed();
            }
        });

        mAddContactButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Add contact button pressed");
                addContactButtonPressed();
            }
        });

        mRolloverAddContactButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Rollover add contact button pressed");
                addContactButtonPressed();
            }
        });

        initButtonToolTips();
    }

   /**
    * Tries to start a call to the UI contact when the call button is pressed.
    */
   private void callButtonPressed()
   {
       ContactListFilter filter = mTreeContactList.getCurrentFilter();

       String location =
           filter.equals(mTreeContactList.getFavoritesFilter()) ? "Favorites" :
           filter.equals(mTreeContactList.getAllHistoryFilter()) ? "All History" :
           filter.equals(mTreeContactList.getCallHistoryFilter()) ? "History" :
           filter.equals(mTreeContactList.getSearchFilter()) ? "Search" :
           "Contact";
       mAnalytics.onEvent(AnalyticsEventType.OUTBOUND_CALL,
                                                  "Calling from",
                                                  location);

       if ((mTreeNode != null) && (mTreeNode instanceof ContactNode))
       {
           sLog.info("Start call from call button");
           call(mTreeNode, false);
       }
   }

   /**
    * Tries to invite the UI contact to a meeting when the call button is pressed.
    */
   private void inviteButtonPressed()
   {
       sLog.user("Invite to meeting button pressed");

       if ((mTreeNode != null) && (mTreeNode instanceof ContactNode))
       {
           UIContact contactDescriptor =
                               ((ContactNode) mTreeNode).getContactDescriptor();

           // Get the metaContact associated with this contact cell.
           MetaContact metaContact = null;
           if (contactDescriptor.getDescriptor() instanceof MetaContact)
           {
               metaContact = (MetaContact) contactDescriptor.getDescriptor();
           }
           else if (contactDescriptor instanceof SourceUIContact)
           {
               SourceUIContact sourceUiContact =
                                           (SourceUIContact) contactDescriptor;

               metaContact =
                   mTreeContactList.getMetaContactFromSource(sourceUiContact);
           }

           // Invite just this metacontact if we have one, otherwise display
           // a dialog to choose who to invite.
           metaContact.createConference(metaContact != null);
       }
   }

    /**
     * Tries to start a chat to the UI contact when the chat button is pressed.
     */
    private void chatButtonPressed()
    {
        sLog.user("Chat button pressed");

        if ((mTreeNode != null) && (mTreeNode instanceof ContactNode))
        {
            UIContact contactDescriptor =
                                ((ContactNode) mTreeNode).getContactDescriptor();

            // Get the metaContact associated with this contact cell.
            MetaContact metaContact = null;
            AbstractUIServiceImpl uiService = GuiActivator.getUIService();
            if (contactDescriptor.getDescriptor() instanceof MetaContact)
            {
                metaContact = (MetaContact) contactDescriptor.getDescriptor();
            }
            else if (contactDescriptor instanceof SourceUIContact)
            {
                SourceUIContact sourceUiContact =
                                            (SourceUIContact) contactDescriptor;

                metaContact =
                    mTreeContactList.getMetaContactFromSource(sourceUiContact);

                if (metaContact == null)
                {
                    if (sourceUiContact.canBeMessaged())
                    {
                        SourceContact sourceContact = sourceUiContact.getSourceContact();

                        // Check whether this is a group chat or 1-to-1 chat
                        List<ContactDetail> mucContactDetails = new ArrayList<>();
                        mucContactDetails = sourceContact.
                                getContactDetails(OperationSetMultiUserChat.class);

                        if (mucContactDetails != null && mucContactDetails.size() > 0)
                        {
                            sLog.info("Start group chat from chat button");
                            String chatRoomId = mucContactDetails.get(0).getDetail();

                            // Send an analytic of us re-opening an existing
                            // group chat, with the number of
                            // active group chats as a parameter.
                            String activeChatRooms = "";
                            ProtocolProviderService imProvider =
                                                   AccountUtils.getImProvider();
                            if (imProvider != null)
                            {
                                OperationSetMultiUserChat opSetMuc =
                                    imProvider.getOperationSet(
                                        OperationSetMultiUserChat.class);

                                if (opSetMuc != null)
                                {
                                    activeChatRooms = String.valueOf(
                                        opSetMuc.getActiveChatRooms());
                                }
                            }

                            List<AnalyticsParameter> params =
                                    new ArrayList<>();

                            params.add(new AnalyticsParameterSimple(
                                AnalyticsParameter.NAME_COUNT_GROUP_IMS,
                                activeChatRooms));

                            mAnalytics.onEvent(
                                AnalyticsEventType.REOPEN_GROUP_IM, params);

                            uiService.startGroupChat(chatRoomId,
                                                     (Boolean)sourceContact.getData("isClosed"),
                                                     sourceContact.getDisplayName());
                        }
                        else
                        {
                            String smsNumber =
                                sourceUiContact.getSourceContact().getEmphasizedNumber();
                            sLog.info("Start chat to SMS number from chat button");
                            uiService.startChat(new String[]{smsNumber});
                        }
                    }
                }
            }

            if (metaContact != null)
            {
                sLog.info("Start chat to MetaContact from chat button " +
                                                                   metaContact);
                uiService.getChatWindowManager().startChat(metaContact);
            }
        }
    }

    /**
     * Tries to open an add contact window when the add contact button is
     * pressed.
     */
    private void addContactButtonPressed()
    {
        sLog.user("Add contact button pressed");

        if ((mTreeNode != null) && (mTreeNode instanceof ContactNode))
        {
            UIContact contactDescriptor
                = ((ContactNode) mTreeNode).getContactDescriptor();

            // The add contact function has only sense for external
            // source contacts.
            if (contactDescriptor instanceof SourceUIContact)
            {
                SourceUIContact sourceUIContact =
                                            (SourceUIContact) contactDescriptor;
                SourceContact sourceContact =
                                (SourceContact) sourceUIContact.getDescriptor();
                addContact(sourceContact);
            }
        }
    }

    /**
     * Sets up the constraints used for all labels and buttons
     */
    protected void initializeLabelConstraints()
    {
        // Initialize the GridBag constraints for each label
        mNameLabelConstraints.insets = new Insets(0, 0, 0, H_GAP);
        mNameLabelConstraints.anchor = GridBagConstraints.WEST;
        mNameLabelConstraints.fill = GridBagConstraints.NONE;
        mNameLabelConstraints.gridx = 1;
        mNameLabelConstraints.gridy = 0;
        mNameLabelConstraints.weightx = 1;
        mNameLabelConstraints.weighty = 0;
        mNameLabelConstraints.gridheight = 1;
        mNameLabelConstraints.gridwidth = 1;

        mAvatarLabelConstraints.insets = new Insets(0, 0, 0, H_GAP);
        mAvatarLabelConstraints.anchor = GridBagConstraints.NORTHEAST;
        mAvatarLabelConstraints.fill = GridBagConstraints.VERTICAL;
        mAvatarLabelConstraints.gridx = 1;
        mAvatarLabelConstraints.gridy = 0;
        mAvatarLabelConstraints.weightx = 0;
        mAvatarLabelConstraints.weighty = 1;
        mAvatarLabelConstraints.gridheight = 3;
        mAvatarLabelConstraints.gridwidth = 1;

        mStatusLabelConstraints.insets = new Insets(0, 0, 0, H_GAP);
        mStatusLabelConstraints.anchor = GridBagConstraints.WEST;
        mStatusLabelConstraints.fill = GridBagConstraints.NONE;
        mStatusLabelConstraints.gridx = 0;
        mStatusLabelConstraints.gridy = 0;
        mStatusLabelConstraints.weightx = 0;
        mStatusLabelConstraints.weighty = 1;
        mStatusLabelConstraints.gridheight = 1;
        mStatusLabelConstraints.gridwidth = 1;

        mDisplayDetailsLabelConstraints.insets = new Insets(0, 0, 0, H_GAP);
        mDisplayDetailsLabelConstraints.anchor = GridBagConstraints.WEST;
        mDisplayDetailsLabelConstraints.fill = GridBagConstraints.NONE;
        mDisplayDetailsLabelConstraints.gridx = 1;
        mDisplayDetailsLabelConstraints.gridy = 1;
        mDisplayDetailsLabelConstraints.weightx = 1;
        mDisplayDetailsLabelConstraints.weighty = 0;
        mDisplayDetailsLabelConstraints.gridheight = 1;
        mDisplayDetailsLabelConstraints.gridwidth = 1;

        // Initialize the common constraints for the action buttons
        mActionButtonConstraints.insets = new Insets(0, 0, V_GAP, 0);
        mActionButtonConstraints.anchor = GridBagConstraints.WEST;
        mActionButtonConstraints.fill = GridBagConstraints.NONE;
        mActionButtonConstraints.gridy = 2;
        mActionButtonConstraints.gridwidth = 1;
        mActionButtonConstraints.gridheight = 1;
        mActionButtonConstraints.weightx = 0;
        mActionButtonConstraints.weighty = 0;
    }

    /**
     * Returns this panel that has been configured to display the meta contact
     * and meta contact group cells.
     *
     * @param tree the source tree
     * @param value the tree node
     * @param selected indicates if the node is selected
     * @param expanded indicates if the node is expanded
     * @param leaf indicates if the node is a leaf
     * @param row indicates the row number of the node
     * @param hasFocus indicates if the node has the focus
     * @return this panel
     */
    @SuppressWarnings("unchecked")
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean selected, boolean expanded, boolean leaf, int row,
        boolean hasFocus)
    {
        mTreeContactList = (TreeContactList) tree;
        mTreeNode = (TreeNode) value;
        mIsSelected = selected && isNodeEnabled(mTreeNode);
        mAvatarLabel.setIcon(null);

        String additionalToolTipText = null;
        String emphasizedNumber = null;

        DefaultTreeContactList contactList = (DefaultTreeContactList) tree;

        setBorder();
        addLabels(mNameLabelConstraints.gridwidth);

        // Set background color.
        if (contactList instanceof TreeContactList)
        {
            if (value instanceof ContactNode &&
                row%2 == 0)
            {
                setBackground(Constants.CALL_HISTORY_EVEN_ROW_COLOR);
            }
            else
            {
                setBackground(Color.WHITE);
            }
        }

        UIContactImpl uiSourceContact = null;
        String iconInfo = null;
        boolean isSelected = false;

        // Make appropriate adjustments for contact nodes and group nodes.
        if (value instanceof ContactNode)
        {
            UIContactImpl contact
                = ((ContactNode) value).getContactDescriptor();
            boolean isHistoryContact = false;

            String displayName = contact.getDisplayName();
            boolean isEnabled = true;

            if (((displayName == null) || (displayName.trim().length() < 1)) &&
                  !(contact instanceof ShowMoreContact))
            {
                displayName = resources.getI18NString("service.gui.UNKNOWN");
            }

            // If this contact is from an external source then attempt to link
            // it with a local contact so that the correct action buttons and
            // right-click menu can be displayed.
            if (contact instanceof SourceUIContact)
            {
                SourceContact sourceContact =
                                       (SourceContact) contact.getDescriptor();

                // Get the additional toolTip information. If this source
                // contact is later mapped to a MetaContact then we will lose
                // information (e.g. call duration).
                additionalToolTipText = sourceContact.getTooltipDisplayDetails();
                emphasizedNumber = sourceContact.getEmphasizedNumber();

                ContactSourceService contactSourceService =
                                              sourceContact.getContactSource();
                isEnabled = sourceContact.isEnabled();

                List<MetaContact> metaContacts = null;

                // If this contact is from history then attempt to link with a
                // local contact
                if (ContactSourceService.CALL_HISTORY_TYPE == contactSourceService
                                                                    .getType() ||
                    ContactSourceService.MESSAGE_HISTORY_TYPE == contactSourceService
                                                                    .getType())
                {
                    isHistoryContact = true;
                    metaContacts = (List<MetaContact>)
                        sourceContact.getData(SourceContact.DATA_META_CONTACTS);

                    List<ContactDetail> contactDetails = sourceContact.getContactDetails();

                    boolean isConferenceCall = (contactDetails.size() > 1);

                    // We do not emphasize any numbers in call history tooltips
                    // if this is a multi-party call
                    if (isConferenceCall)
                    {
                        emphasizedNumber = null;
                    }

                    // The unmatchedContactDetails list is initialised as the
                    // full list of contact details. After matching details and
                    // metaContacts, this list will contain any contact details
                    // that do not have a matching metaContact
                    List<ContactDetail> unmatchedContactDetails =
                            new ArrayList<>(contactDetails);

                    // If some MetaContacts have been found, check that each of
                    // them still match - if the contact has been edited, and a
                    // number changed, then it may no longer do so..
                    if (metaContacts != null)
                    {
                        List<MetaContact> metaContactsToRemove = new ArrayList<>();

                        for (MetaContact metaContact : metaContacts)
                        {
                            List<Contact> matchingContacts = new ArrayList<>();

                            // This history entry may have multiple phone
                            // numbers associated with it. We must check whether
                            // this metaContact matches any of them.
                            for (ContactDetail contactDetail : contactDetails)
                            {
                                String callNumber = contactDetail.getDetail();

                                List<Contact> matchingMetaContacts =
                                    metaContact.getContactByPhoneNumber(callNumber);

                                if (matchingMetaContacts != null &&
                                    matchingMetaContacts.size() > 0)
                                {
                                    // We have made a match between this contact
                                    // detail and this metaContact
                                    matchingContacts.addAll(matchingMetaContacts);

                                    // This contact detail has a matching
                                    // metaContact so remove the contact detail
                                    // from the list of unmatched details
                                    unmatchedContactDetails.remove(contactDetail);
                                }
                            }

                            // We have checked all the contact details for this
                            // metaContact. If there is no match then add it to
                            // a list of MetaContacts to be removed.
                            if (matchingContacts.size() == 0)
                            {
                                metaContactsToRemove.add(metaContact);
                            }
                        }

                        // remove the metaContacts that had no match
                        for (MetaContact metaContact : metaContactsToRemove)
                        {
                            metaContacts.remove(metaContact);
                        }

                        MetaContactListService metaContactListService =
                            GuiActivator.getContactListService();

                        // For each contact detail that does not have a matching
                        // metaContact, attempt to find a matching metaContact
                        for (ContactDetail contactDetail : unmatchedContactDetails)
                        {
                            List<MetaContact> metaContactList =
                                metaContactListService.findMetaContactByNumber(
                                                     contactDetail.getDetail());

                            if (metaContactList != null)
                            {
                                metaContacts.addAll(metaContactList);
                            }
                        }
                    }

                    if (metaContacts == null || metaContacts.isEmpty())
                    {
                        metaContacts =
                                    findMetaContacts((SourceUIContact) contact);
                    }

                    // If MetaContact matches have been found for this source
                    // then create a link between the source contact and the
                    // MetaContacts.
                    if (metaContacts != null && !metaContacts.isEmpty())
                    {
                        // Create a key-value pair in the source contact so we
                        // can later associate this source with the MetaContacts
                        sourceContact.setData(SourceContact.DATA_META_CONTACTS,
                                              metaContacts);
                        ((ContactNode) value).setUserObject(contact);

                        // Update the display name
                        MetaContact metaContact = metaContacts.get(0);
                        if (metaContacts.size() == 1 && !isConferenceCall)
                        {
                            displayName = metaContact.getDisplayName();

                            // Update the UI contact so that we display correctly
                            uiSourceContact = contact;
                            contact = new MetaUIContact(metaContact,
                                   mTreeContactList.getMetaContactListSource());
                            contact.setContactNode((ContactNode) value);
                        }
                        else if (isConferenceCall && metaContacts.size() == 2)
                        {
                            // TODO: DUIR-5256 This is not properly localized,
                            //  you should not use string concatenation
                            displayName = metaContacts.get(0).getDisplayName() +
                                " " + AND + " " + metaContacts.get(1).getDisplayName();
                        }
                        else
                        {
                            // This was either a multi-party call, or we haven't
                            // been able to find a unique contact match for the
                            // number
                            String[] args = new String[]
                            {
                                metaContact.getDisplayName(),
                                String.valueOf(contactDetails.size() - 1)
                            };

                            String displayNameRes;
                            if (isConferenceCall)
                            {
                                // For multi-party calls of > 3 participants, we
                                // show the name as "<contact name> and X other(s)"
                                if (contactDetails.size() == 2)
                                {
                                    displayNameRes = "impl.protocol.sip.X_AND_1_OTHER";
                                }
                                else
                                {
                                    displayNameRes = "impl.protocol.sip.X_AND_N_OTHERS";
                                }
                            }
                            else if (metaContacts.size() == 2)
                            {
                                displayNameRes = "impl.protocol.sip.X_OR_1_OTHER";
                            }
                            else
                            {
                                displayNameRes = "impl.protocol.sip.X_OR_N_OTHERS";
                            }

                            displayName = resources.getI18NString(displayNameRes, args);
                        }

                        sourceContact.setData(
                                  SourceContact.DATA_DISABLE_ADD_CONTACT, true);
                        sourceContact.setDisplayName(displayName);
                    }
                    else
                    {
                        // No matching meta contact - make sure the data is null
                        sourceContact.setData(SourceContact.DATA_META_CONTACTS,
                                              null);
                        sourceContact.setData(
                                 SourceContact.DATA_DISABLE_ADD_CONTACT, false);

                        // Unset the display name
                        sourceContact.setDisplayName(null);

                        // As this doesn't match a MetaContact, the only thing
                        // we'll have to use as the display name is the phone
                        // number.  Therefore, format the number nicely before
                        // setting it in the UI.
                        displayName =
                            sPhoneNumberUtils.formatNumberForDisplay(displayName);
                    }

                    // If the contact is anonymous then mark it as so on the
                    // source contact
                    if (isContactAnonymous(contact))
                    {
                        sourceContact.setData(SourceContact.DATA_IS_ANONYMOUS,
                                              true);
                        displayName = resources
                            .getI18NString("service.gui.ANONYMOUS");
                    }
                }
            }

            // Disable HTML to prevent HTML injection in the contact/group names
            mNameLabel.putClientProperty("html.disable", Boolean.TRUE);

            mNameLabel.setText(displayName);

            // Add a star next to the name if this is a favourite contact.
            // Otherwise make sure there isn't a star present
            boolean contactIsFavourite = false;

            if (contact instanceof MetaUIContact)
            {
                MetaUIContact metaUIContact = (MetaUIContact)contact;
                MetaContact metaContact = metaUIContact.getMetaContact();
                isEnabled = metaUIContact.isEnabled();

                String favDetail = metaContact.
                               getDetail(MetaContact.CONTACT_FAVORITE_PROPERTY);

                contactIsFavourite = Boolean.parseBoolean(favDetail);
            }
            else if (contact instanceof SourceUIContact)
            {
                SourceUIContact sourceUIContact = (SourceUIContact)contact;
                SourceContact sourceContact = sourceUIContact.getSourceContact();
                List<MetaContact> data = (List<MetaContact>)
                          sourceContact.getData(SourceContact.DATA_META_CONTACTS);

                if (data != null)
                {
                    List<MetaContact> metaContacts = new ArrayList<>(data);
                    for (MetaContact metaContact : metaContacts)
                    {
                        String favDetail = metaContact.
                               getDetail(MetaContact.CONTACT_FAVORITE_PROPERTY);
                        contactIsFavourite = Boolean.parseBoolean(favDetail);

                        if (contactIsFavourite)
                            break;
                    }
                }
            }

            if (contactIsFavourite)
            {
                mNameLabel.setHorizontalTextPosition(SwingConstants.LEFT);
                sImageLoaderService.getImage(ImageLoaderService.FAVORITE_ICON)
                .getImageIcon()
                .addToLabel(mNameLabel);
            }
            else
            {
                mNameLabel.setIcon(null);
            }

            if ((mStatusIcon != null) &&
                (contactList.isContactActive(contact)) &&
                (mStatusIcon instanceof ImageIcon))
            {
                mStatusIcon = mMsgReceivedImage.getImageIcon();
            }
            else
            {
                mStatusIcon = contact.getStatusIcon();
            }

            // If IM is not enabled in the config, presence won't be enabled
            // so we don't want to display a status icon.
            if (!ConfigurationUtils.isImEnabled())
            {
                mStatusIcon = null;
            }

            if (mStatusIcon != null)
            {
                mStatusIcon.addToLabel(mStatusLabel);
            }

            mNameLabel.setFont(getFont().deriveFont(Font.PLAIN, ScaleUtils.getDefaultFontSize()));

            if (mContactForegroundColor != null)
                mNameLabel.setForeground(mContactForegroundColor);

            // Initializes status message components if the given meta contact
            // contains a status message.
            mDisplayDetailsLabelConstraints.gridwidth = 1;
            String displayDetails = (uiSourceContact != null) ?
                                          uiSourceContact.getDisplayDetails() :
                                          contact.getDisplayDetails();

            // If this isn't a history item display details will contain
            // presence status. If IM is not enabled in the config, presence
            // won't be enabled so we don't want to display presence status.
            if (!isHistoryContact && !ConfigurationUtils.isImEnabled())
            {
                displayDetails = "";
            }

            initDisplayDetails(displayDetails,
                               mDisplayDetailsLabel,
                               mDisplayDetailsLabelConstraints);

            if (mTreeContactList.isContactButtonsVisible())
                initButtonsPanel(contact, uiSourceContact);

            // Set the avatar appropriately. For history items this will be
            // the call status symbol, for contacts this will be their avatar.
            UIContactImpl avatarContact = (uiSourceContact != null) ?
                                                            uiSourceContact :
                                                            contact;

            // Get the text version of the info conveyed by the icon, to add to
            // the accessibility description
            iconInfo = avatarContact == null ? null :
                    avatarContact.getDescriptor() instanceof SourceContact ?
                            ((SourceContact)avatarContact.getDescriptor()).getImageDescription() :
                            null;

            ImageIconFuture avatar = avatarContact.getAvatar(false);

            // Set the avatar to the contact's avatar.
            if (avatar != null)
            {
                mAvatarLabel.setIcon(avatar.resolve());
            }

            // The compact display mode displays text/icons in every label.
            // Fill in any blank labels appropriately.
            populateBlankLabels(contact);

            // If this is a tree that supports multi-selection then show a
            // selected/unselected icon on each cell
            if (mTreeContactList.getSelectionModel().getSelectionMode() ==
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
            {
                if (!isEnabled)
                {
                    if (contact.isSelected())
                    {
                        DISABLED_SELECTED_ICON.addToLabel(mSelectedLabel);
                        isSelected = true;
                    }
                    else
                    {
                        DISABLED_UNSELECTED_ICON.addToLabel(mSelectedLabel);
                    }
                }
                else if (contact != null && contact.isSelected())
                {
                    SELECTED_ICON.addToLabel(mSelectedLabel);
                    isSelected = true;
                }
                else
                {
                    UNSELECTED_ICON.addToLabel(mSelectedLabel);
                }
                remove(mSelectedLabel);
                add(mSelectedLabel, mSelectedIconConstraints);
            }

            // If this is the 'show more' cell then set the text of the avatar
            // label and remove any icons.
            if (contact instanceof ShowMoreContact)
            {
                mStatusLabel.setIcon(null);
                mAvatarLabel.setIcon(null);

                mAvatarLabel.setFont(mAvatarLabel.getFont().deriveFont(
                    ScaleUtils.getDefaultFontSize()));
                mAvatarLabel.setForeground(Color.GRAY);
                mAvatarLabel.setText((String)contact.getDescriptor());
            }
            else if (!isEnabled)
            {
                mDisplayDetailsLabel.setForeground(Color.LIGHT_GRAY);
                mStatusLabel.setIcon(null);
                mNameLabel.setForeground(Color.LIGHT_GRAY);
                mIsSelected = false;
            }
            else
            {
                mAvatarLabel.setFont(mAvatarLabel.getFont().deriveFont(
                    ScaleUtils.getScaledFontSize(9f)));
                mAvatarLabel.setText("");
            }

            if (mToolTipsEnabled)
            {
                if (!StringUtils.isNullOrEmpty(mToolTip))
                {
                    setToolTipText(mToolTip);
                }
                else
                {
                    String contactToolTip = contact.getToolTip(
                                                          additionalToolTipText,
                                                          emphasizedNumber);
                    if (StringUtils.isNullOrEmpty(contactToolTip))
                    {
                        setToolTipText(null);
                    }
                    else
                    {
                        setToolTipText(contactToolTip);
                    }
                }
            }
        }
        else if (value instanceof GroupNode)
        {
            UIGroupImpl groupItem
                = ((GroupNode) value).getGroupDescriptor();

            String displayName = groupItem.getDisplayName();

            if (displayName == null || displayName.isBlank())
            {
                // There's no display name so there's nothing to display, but we
                // need to return something to avoid NPEs.  Make this panel
                // invisible and return it now, as we don't want to do any more
                // formatting or add any accessibility tags to it.
                setVisible(false);
                return this;
            }

            setToolTipText(null);

            JLabel groupNameLabel = getGroupNameLabel();
            groupNameLabel.setText(displayName);

            groupNameLabel.setFont(getFont().deriveFont(Font.BOLD));

            if (mGroupForegroundColor != null)
                groupNameLabel.setForeground(mGroupForegroundColor);

            remove(getGroupBlankLabel());
            remove(mCallButton);
            remove(mRolloverCallButton);
            remove(mInviteToMeetingButton);
            remove(mRolloverInviteToMeetingButton);
            remove(mCallVideoButton);
            remove(mRolloverCallVideoButton);
            remove(mChatButton);
            remove(mRolloverChatButton);
            remove(mAddContactButton);
            remove(mRolloverAddContactButton);

            clearCustomActionButtons();

            mStatusIcon = expanded
                                ? mOpenedGroupIcon
                                : mClosedGroupIcon;
            mStatusIcon.addToLabel(mStatusLabel);

            // We have no photo icon for groups.
            JLabel groupDetailsLabel = getGroupDetailsLabel();
            groupDetailsLabel.setIcon(null);
            groupDetailsLabel.setText("");
            mNameLabel.setIcon(null);

            if (groupItem.countChildContacts() >= 0)
            {
                groupDetailsLabel.setFont(groupDetailsLabel.getFont().
                    deriveFont(ScaleUtils.getScaledFontSize(9f)));
                groupDetailsLabel.setForeground(Color.BLACK);
                groupDetailsLabel.
                        setText(groupItem.countOnlineChildContacts() + "/" +
                                groupItem.countChildContacts());
            }

            initDisplayDetails(groupItem.getDisplayDetails(),
                               getGroupBlankLabel(),
                               getGroupBlankLabelConstraints());
            initButtonsPanel(groupItem);
        }

        if (mIsSelected && ConfigurationUtils.highlightListTextWhenSelected())
        {
            mNameLabel.setForeground(Constants.SELECTED_COLOR);
            mStatusLabel.setForeground(Constants.SELECTED_COLOR);
            mAvatarLabel.setForeground(Constants.SELECTED_COLOR);
            mDisplayDetailsLabel.setForeground(Constants.SELECTED_COLOR);
            mSelectedLabel.setForeground(Constants.SELECTED_COLOR);
        }

        // Let's set the screen reader content for this item
        // We always have usable text in mNameLabel (either a name or number)
        // Additional status comes from mDisplayDetailsLabel.
        // We also get the audio info that is normally conveyed by the icon image.
        String[] parameters = new String[]{
                mNameLabel.getText(),
                mDisplayDetailsLabel.getText(),
                iconInfo};

        String res = iconInfo == null ?
                (isSelected ? "service.gui.accessibility.LIST_ITEM_COMBINED_WITHOUT_ICON_SELECTED" :
                              "service.gui.accessibility.LIST_ITEM_COMBINED_WITHOUT_ICON") :
                (isSelected ? "service.gui.accessibility.LIST_ITEM_COMBINED_SELECTED" :
                              "service.gui.accessibility.LIST_ITEM_COMBINED");
        AccessibilityUtils.setName(this,
                                   resources.getI18NString(res, parameters));

        return this;
    }

    /**
     * Determine whether the node is enabled by examining the underlying source-
     * or meta- contact
     *
     * @param treeNode the tree node to examine
     * @return whether this node is enabled
     */
    private boolean isNodeEnabled(TreeNode treeNode)
    {
        boolean isEnabled = true;
        if (treeNode instanceof ContactNode)
        {
            UIContactImpl contact
                = ((ContactNode) treeNode).getContactDescriptor();

            if (contact instanceof SourceUIContact)
            {
                isEnabled = ((SourceContact) contact.getDescriptor()).isEnabled();
            }
            else if (contact instanceof MetaUIContact)
            {
                MetaUIContact metaUIContact = (MetaUIContact)contact;
                isEnabled = metaUIContact.isEnabled();
            }
        }

        return isEnabled;
    }

    /**
     * Fill in any blank labels appropriately. This is used by the compact
     * display mode to ensure the UI looks good
     *
     * @param contact the contact for this cell
     */
    private void populateBlankLabels(UIContactImpl contact)
    {
        // If there is no status message set then create one based on
        // the contact's status
        if (mDisplayDetailsLabel.getText().isEmpty())
        {
            // If this is a saved contact then find their status.
            // Otherwise display a helpful message.
            // Unless IM is disabled, in which case we don't want to display
            // presence text.
            if (contact instanceof MetaUIContact && ConfigurationUtils.isImEnabled())
            {
                // If the contact's presence status is null set their presence
                // text to "no presence info", otherwise use the presence text
                // from their presence status.
                String noPresenceInfo = resources.
                    getI18NString("service.gui.CONTACT_IM_NOT_SUPPORTED");
                PresenceStatus contactPresenceStatus =
                    ((MetaUIContact) contact).getContactPresenceStatus();
                String presenceText = (contactPresenceStatus == null) ?
                    noPresenceInfo : contactPresenceStatus.getStatusName();
                mDisplayDetailsLabel.setText(presenceText);

                // If the presence text is "no presence info" we want it to
                // appear grey, otherwise make the text black.
                Color presenceColor = noPresenceInfo.equals(presenceText) ?
                    Color.GRAY : Color.BLACK;
                mDisplayDetailsLabel.setForeground(presenceColor);
            }
            else if (contact instanceof SourceUIContact)
            {
                // This is a search result rather than a local contact, so
                // display a "not in contact list" message rather than their
                // presence.  We don't check whether IM is enabled here, as we
                // want to display this text in all cases.
                mDisplayDetailsLabel.setText(resources
                        .getI18NString("service.gui.CONTACT_NOT_IN_CONTACTS"));
                mDisplayDetailsLabel.setForeground(Color.GRAY);
                mStatusIcon = null;
                remove(mStatusLabel);
            }
        }

        // If we have been unable to get a status icon, use a greyed out
        // icon. This can happen if the contact does not support chat, or
        // if this contact is a search result. Don't do this if IM is
        // disabled or if the contact is a search result, as we don't want to
        // display any presence icons in those cases.
        if (mStatusIcon == null &&
            ConfigurationUtils.isImEnabled() &&
            !(contact instanceof SourceUIContact))
        {
            mStatusIcon = USER_NO_STATUS_ICON;
            mStatusIcon.addToLabel(mStatusLabel);
        }

        // If we still have no display details or status icon for the contact,
        // get rid of the display details and status labels and make the name
        // label take up the full height of the contact cell.
        if ((mDisplayDetailsLabel.getText().isEmpty()) && (mStatusIcon == null))
        {
            sLog.debug("Removing display details and status icon panel");

            remove(mNameLabel);
            remove(mDisplayDetailsLabel);
            remove(mStatusLabel);
            mNameLabelConstraints.gridheight = 2;
            add(mNameLabel, mNameLabelConstraints);
        }
    }

    /**
     * Get the GridBag constraints used for the blank group node label
     *
     * @return the GridBag constraints for the blank group node label
     */
    protected GridBagConstraints getGroupBlankLabelConstraints()
    {
        // Set the gridwidth as this may have changed if the previous cell was
        // a contact node
        mDisplayDetailsLabelConstraints.gridwidth = 1;
        return mDisplayDetailsLabelConstraints;
    }

    /**
     * Get the label used to display the group name
     *
     * @return the JLabel object used to display the group name
     */
    protected JLabel getGroupNameLabel()
    {
        // Group headers use the same objects as contacts, so map the group
        // name label to the contact name label.
        return mNameLabel;
    }

    /**
     * Get the label used to display the group details
     *
     * @return the JLabel object used to display the group details
     */
    protected JLabel getGroupDetailsLabel()
    {
        // Group headers use the same objects as contacts, so map the group
        // details label to the contact photo label.
        return mAvatarLabel;
    }

    /**
     * Get the label that remains blank for group nodes
     *
     * @return the JLabel object that is not used for group nodes
     */
    protected JLabel getGroupBlankLabel()
    {
        // Group headers use the same objects as contacts, there is less
        // information in the group nodes, so one label must be blank.
        return mDisplayDetailsLabel;
    }

    /**
     * Paints a customized background.
     *
     * @param g the <tt>Graphics</tt> object through which we paint
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();

        if (!(mTreeNode instanceof GroupNode) && !mIsSelected)
            return;

        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        try
        {
            internalPaintComponent(g2);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected.
     *
     * @param g2 the <tt>Graphics2D</tt> object through which we paint
     */
    private void internalPaintComponent(Graphics2D g2)
    {
        // If we should highlight the text, then don't highlight the cell
        if (ConfigurationUtils.highlightListTextWhenSelected())
        {
            return;
        }

        if (mIsSelected)
        {
            g2.setPaint(Constants.SELECTED_COLOR);
        }
        else if (mTreeNode instanceof GroupNode)
        {
            g2.setPaint(new GradientPaint(0, 0,
                Constants.CONTACT_LIST_GROUP_BG_GRADIENT_COLOR,
                0, getHeight(),
                Constants.CONTACT_LIST_GROUP_BG_COLOR));
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                         SELECTION_BG_CORNER_DIAMETER,
                         SELECTION_BG_CORNER_DIAMETER);
    }

    /**
     * Returns the height of this icon. Used for the drag&drop component.
     * @return the height of this icon
     */
    public int getIconHeight()
    {
        return getPreferredSize().height + 10;
    }

    /**
     * Returns the width of this icon. Used for the drag&drop component.
     * @return the widht of this icon
     */
    public int getIconWidth()
    {
        return mTreeContactList.getWidth() + 10;
    }

    /**
     * Returns the preferred size of this component.
     * @return the preferred size of this component
     */
    public Dimension getPreferredSize()
    {
        Dimension preferredSize = new Dimension();
        int preferredHeight;

        if (mTreeNode instanceof ContactNode)
        {
            UIContact contact
                = ((ContactNode) mTreeNode).getContactDescriptor();

            preferredHeight = contact.getPreferredHeight();

            if (preferredHeight > 0)
                preferredSize.height = preferredHeight;
            else if (contact instanceof ShowMoreContact)
                preferredSize.height = mShowMoreHeight;
            else if (mIsSelected && mTreeContactList.isContactButtonsVisible())
                preferredSize.height = mSelectedRowHeight;
            else
                preferredSize.height = mRowHeight;
        }
        else if (mTreeNode instanceof GroupNode)
        {
            UIGroup group
                = ((GroupNode) mTreeNode).getGroupDescriptor();

            preferredHeight = group.getPreferredHeight();

            if (mIsSelected
                    && mCustomActionButtonsUIGroup != null
                    && !mCustomActionButtonsUIGroup.isEmpty())
                preferredSize.height = 70;
            else if (preferredHeight > 0)
                preferredSize.height = preferredHeight;
            else
                preferredSize.height = mShowMoreHeight;
        }

        return preferredSize;
    }

    /**
     * Adds contact entry labels.
     *
     * @param nameLabelGridWidth the grid width of the contact entry name
     * label
     */
    protected void addLabels(int nameLabelGridWidth)
    {
        remove(mNameLabel);
        remove(mAvatarLabel);
        remove(mDisplayDetailsLabel);
        remove(mStatusLabel);

        // Re-initialize the label constraints for safety
        initializeLabelConstraints();

        // Set up the internal padding for contact nodes and group nodes
        Insets contactInsets = new Insets(0, 0, 0, H_GAP);
        Insets groupNodeInsets = new Insets(0, 0, V_GAP, H_GAP);

        if (mTreeNode instanceof GroupNode)
        {
            mNameLabelConstraints.insets = groupNodeInsets;
            mAvatarLabelConstraints.insets = groupNodeInsets;
            mStatusLabelConstraints.insets = groupNodeInsets;
            mDisplayDetailsLabelConstraints.insets = groupNodeInsets;
        }
        else
        {
            mNameLabelConstraints.insets = contactInsets;
            mAvatarLabelConstraints.insets = contactInsets;
            mStatusLabelConstraints.insets = contactInsets;
            mDisplayDetailsLabelConstraints.insets = contactInsets;
        }

        // Set up and add the labels used in this object
        mNameLabelConstraints.gridwidth = nameLabelGridWidth;
        add(mNameLabel, mNameLabelConstraints);

        mAvatarLabelConstraints.gridx = nameLabelGridWidth + 1;
        add(mAvatarLabel, mAvatarLabelConstraints);

        add(mStatusLabel, mStatusLabelConstraints);

        if (mTreeNode instanceof ContactNode)
        {
            mDisplayDetailsLabelConstraints.gridwidth = nameLabelGridWidth;
            add(mDisplayDetailsLabel, mDisplayDetailsLabelConstraints);
        }
        else if (mTreeNode != null && mTreeNode instanceof GroupNode)
        {
            mDisplayDetailsLabelConstraints.gridwidth = nameLabelGridWidth;
            add(mDisplayDetailsLabel, mDisplayDetailsLabelConstraints);
        }
    }

    /**
     * Initializes the display details component for the given
     * <tt>UIContact</tt>.
     * @param displayDetails the display details to show
     * @param detailsLabel the label used to display details
     */
    protected void initDisplayDetails(String displayDetails,
                                      JLabel detailsLabel,
                                      GridBagConstraints detailsLabelConstraints)
    {
        remove(detailsLabel);
        detailsLabel.setForeground(Color.BLACK);
        detailsLabel.setText("");

        if ((displayDetails != null) && (displayDetails.length() > 0))
        {
            // Replace all occurrences of new line with slash.
            displayDetails = displayDetails.replaceAll("\n|<br>|<br/>", " / ");

            detailsLabel.setText(displayDetails);
        }

        add(detailsLabel, detailsLabelConstraints);
    }

    /**
     * Initializes buttons panel.
     * @param uiContact the <tt>UIContact</tt> for which we initialize the
     * button panel
     */
    protected void initButtonsPanel(UIContact uiContact,
                                    UIContact uiSourceContact)
    {
        removeActionButtons(true);

        // If the cell is not selected, or the contact is anonymous then
        // there is nothing to do here
        if ((!mIsSelected) || isContactAnonymous(uiContact))
            return;

        int xCoord = (mStatusIcon == null ? 0 : mStatusIcon.resolve().getIconWidth())
                + mLeftBorder
                + H_GAP;

        // Set the horizontal starting point in the GridBag.
        int gridX = 1;

        if (isIMContact(uiContact, false))
        {
            xCoord += addButton(mChatButton, gridX, xCoord, false);
            gridX++;
        }

        boolean hasPhone = isTelephonyContact(uiContact);

        // Add the call button if appropriate
        if (canCallContact(uiContact, hasPhone))
        {
            xCoord += addButton(mCallButton, gridX, xCoord, false);
            gridX++;
        }

        // Add the invite to meeting button if appropriate
        if (canInviteContactToMeeting(uiContact, hasPhone))
        {
            xCoord += addButton(mInviteToMeetingButton, gridX, xCoord, false);
            gridX++;
        }

        // Add the video button if appropriate
        if (canVideoCallContact(uiContact, hasPhone))
        {
            xCoord += addButton(mCallVideoButton, gridX, xCoord, false);
            gridX++;
        }

        // Add the add contact button if appropriate
        if (canAddContact(uiContact))
        {
            xCoord += addButton(mAddContactButton, gridX, xCoord, false);
            gridX++;
        }

        // The list of the contact actions
        // we will create a button for every action
        Collection<SIPCommButton> contactActions
            = uiContact.getContactCustomActionButtons();

        int lastGridX = gridX;
        if ((contactActions != null) && (contactActions.size() > 0))
        {
            lastGridX = initContactActionButtons(contactActions, gridX, xCoord);
            gridX++;
        }
        else
        {
            addLabels(gridX);
        }

        if (mLastAddedButton != null)
            setButtonBg(mLastAddedButton, lastGridX, true);

        setBounds(0, 0, mTreeContactList.getWidth(),
                        getPreferredSize().height);
    }

    /**
     * Whether the contact should show the add contact button
     * @param uiContact the contact to check against
     * @return whether the contact should show the add contact
     * button
     */
    protected boolean canAddContact(UIContact uiContact)
    {
        List<ProtocolProviderService> contactProviderServices =
                                AccountUtils.getOpSetRegisteredProviders(
                                        OperationSetPersistentPresence.class,
                                        null,
                                        null);

        boolean canAddContact = false;
        // Enable add contact button if contact source has indicated
        // that this is possible
        if ((uiContact.getDescriptor() instanceof SourceContact) &&
                (uiContact.getDefaultContactDetail(
                OperationSetPersistentPresence.class) != null) &&
                (contactProviderServices.size() > 0))
        {
            // Check if the add contact button is disabled for this contact and
            // return false for IM history entries if the IM provider is null,
            // e.g. if the user has signed out of chat.
            SourceContact sourceContact = (SourceContact) uiContact.getDescriptor();
            Object addContactDisabledData = sourceContact.getData(SourceContact.DATA_DISABLE_ADD_CONTACT);
            Object contactTypeData = sourceContact.getData(SourceContact.DATA_TYPE);
            boolean isAddContactDisabled = addContactDisabledData != null && addContactDisabledData.equals(true);
            boolean isOfflineIMHistory = AccountUtils.getImProvider() == null && SourceContact.Type.IM_MESSAGE_HISTORY.equals(contactTypeData);

            canAddContact = !isAddContactDisabled && !isOfflineIMHistory;
        }

        return canAddContact;
    }

    /**
     * Whether the contact should show the video button
     * @param uiContact the contact to check against
     * @param hasPhone whether the contact supports telephony
     * @return whether the contact should show the video button
     */
    protected boolean canVideoCallContact(UIContact uiContact,
                                          boolean hasPhone)
    {
        UIContactDetail videoContact
                    = uiContact.getDefaultContactDetail(
                            OperationSetVideoTelephony.class);

        // Only display the video button if the contact is a video contact, or
        // if video-over-phone-line is enabled.
        // Never show the button if video UI is turned off in config.
        ConfigurationService cs =
            GuiActivator.getConfigurationService();
        boolean videoUIDisabled = cs.user().getBoolean(DISABLE_VIDEO_UI_PROP, false);

        boolean isVideoContact = (videoContact != null);

        if (!videoUIDisabled || !(isVideoContact))
        {
            return false;
        }

        return true;
    }

    /**
     * The contact should show the invite to meeting button only if
     * it will work and calls are disabled (to prevent both buttons
     * appearing together).
     * @param uiContact the contact to check against
     * @param hasPhone whether the contact supports telephony
     * @return whether the contact should show the invite to meeting button
     */
    protected boolean canInviteContactToMeeting(UIContact uiContact,
                                                boolean hasPhone)
    {
        // The contact must have IM, we must have Meeting function enabled
        // and we must not show the call button (we don't want to show too many)
        return (isIMContact(uiContact, hasPhone) &&
                CreateConferenceMenu.isConferenceInviteByImEnabled() &&
                !ConfigurationUtils.isCallingEnabled());
    }

    /**
     * Whether the contact should show the call button
     * @param uiContact the contact to check against
     * @param hasPhone whether the contact supports telephony
     * @return whether the contact should show the call button
     */
    protected boolean canCallContact(UIContact uiContact,
                                      boolean hasPhone)
    {
        UIContactDetail telephonyContact
                    = uiContact.getDefaultContactDetail(
                            OperationSetBasicTelephony.class);

        // for SourceContact in history that do not support telephony, we
        // show the button but disabled
        List<ProtocolProviderService> protocolProviderServices
                            = AccountUtils.getOpSetRegisteredProviders(
                                          OperationSetBasicTelephony.class,
                                          null,
                                          null);

        // Add the call button if any of the below conditions are true.
        // Whether the contact has a valid telephony contact address.
        boolean hasTelephonyAddress =
            telephonyContact != null && telephonyContact.getAddress() != null;

        // Whether the contact is a SourceContact but not a
        // MessageHistorySourceContact.
        boolean isCallableSourceContact = false;
        if (uiContact.getDescriptor() instanceof SourceContact)
        {
            int contactType = ((SourceUIContact) uiContact).getSourceContact().
                                                   getContactSource().getType();
            isCallableSourceContact =
                    !(contactType == ContactSourceService.MESSAGE_HISTORY_TYPE);
        }

        // Whether the contact supports telephony.
        boolean supportsTelephony = hasPhone && protocolProviderServices.size() > 0;

        if (hasTelephonyAddress || isCallableSourceContact || supportsTelephony)
        {
            return true;
        }

        return false;
    }

    /**
     * Whether the contact supports telephony
     * @param uiContact the contact to check against
     * @return whether the contact supports telephony
     */
    protected boolean isTelephonyContact(UIContact uiContact)
    {
        UIContactDetail telephonyContact
                        = uiContact.getDefaultContactDetail(
                                OperationSetBasicTelephony.class);

        boolean hasPhone = false;

        // check for phone stored in contact info only
        // if telephony contact is missing
        if ((uiContact.getDescriptor() instanceof MetaContact) &&
            (telephonyContact == null))
        {
            MetaContact metaContact =
                (MetaContact)uiContact.getDescriptor();
            Iterator<Contact> contacts = metaContact.getContacts();

            while (contacts.hasNext() && !hasPhone)
            {
                Contact contact = contacts.next();

                OperationSetServerStoredContactInfo infoOpSet =
                    contact.getProtocolProvider().getOperationSet(
                        OperationSetServerStoredContactInfo.class);
                Iterator<GenericDetail> details;

                if (infoOpSet != null)
                {
                    details = infoOpSet.requestAllDetailsForContact(
                        contact,
                        new DetailsListener(mTreeNode, mCallButton, uiContact));

                    if (details != null)
                    {
                        while (details.hasNext())
                        {
                            GenericDetail d = details.next();
                            if ((d instanceof PhoneNumberDetail) &&
                                !(d instanceof PagerDetail) &&
                                !(d instanceof FaxDetail))
                            {
                                PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                                if (pnd.getNumber() != null &&
                                    (pnd.getNumber().length() > 0))
                                {
                                    hasPhone = true;
                                }
                             }
                        }
                    }
                }
            }
        }

        return hasPhone;
    }

    /**
     * Whether the contact supports IM
     * @param uiContact the contact to check against
     * @param hasPhone whether the contact supports telephony
     * @return whether the contact supports IM
     */
    protected Boolean isIMContact(UIContact uiContact, boolean hasPhone)
    {
        boolean isImContact = false;
        UIContactDetail imContact = null;

        if ((uiContact.getDescriptor() instanceof MetaContact) ||
            (uiContact.canBeMessaged()))
        {
            imContact = uiContact.getDefaultContactDetail(
                                       OperationSetBasicInstantMessaging.class);

            // If SMS is enabled, we can also chat to contacts with phone numbers.
            if ((imContact == null) && ConfigurationUtils.isSmsEnabled())
            {
                if (hasPhone)
                {
                   isImContact = true;
                }
                else
                {
                    imContact = uiContact.getDefaultContactDetail(
                                              OperationSetBasicTelephony.class);
                }
            }
        }

        if (imContact != null)
        {
            isImContact = true;
        }

        return isImContact;
    }

    /**
     * Whether the contact is a Group Contact
     * @param uiContact the contact to check against
     * @return whether the contact is a Group Contact
     */
    protected boolean isGroupContact(UIContact uiContact)
    {
        boolean isGroupContact = false;

        if ((uiContact.getDescriptor() instanceof MetaContact) &&
            ((MetaContact)uiContact.getDescriptor()).getGroupContact() != null)
        {
            isGroupContact = true;
        }

        return isGroupContact;
    }

    /**
     * Removes all action buttons for a cell
     */
    protected void removeActionButtons(boolean includeRolloverButtons)
    {
        // Remove all standard buttons.
        remove(mChatButton);
        remove(mCallButton);
        remove(mInviteToMeetingButton);
        remove(mCallVideoButton);
        remove(mAddContactButton);

        if (includeRolloverButtons)
        {
            removeRolloverButtons();
        }

        // Remove all custom buttons.
        if ((mCustomActionButtons != null) && (mCustomActionButtons.size() > 0))
        {
            Iterator<JButton> buttonsIter = mCustomActionButtons.iterator();
            while (buttonsIter.hasNext())
            {
                remove(buttonsIter.next());
            }
            mCustomActionButtons.clear();
        }
    }

    private void removeRolloverButtons()
    {
        remove(mRolloverChatButton);
        remove(mRolloverCallButton);
        remove(mRolloverInviteToMeetingButton);
        remove(mRolloverCallVideoButton);
        remove(mRolloverAddContactButton);
    }

    /**
     * Initializes buttons panel.
     * @param uiGroup the <tt>UIGroup</tt> for which we initialize the
     * button panel
     */
    private void initButtonsPanel(UIGroup uiGroup)
    {
        int x = (mStatusIcon == null ? 0 : mStatusIcon.resolve().getIconWidth())
                + mLeftBorder
                + H_GAP;
        int gridX = 0;

        // The list of the actions
        // we will create a button for every action
        Collection<SIPCommButton> contactActions
            = uiGroup.getCustomActionButtons();

        int lastGridX = gridX;
        if (contactActions != null && contactActions.size() > 0)
        {
            lastGridX = initGroupActionButtons(contactActions, gridX, x);
        }
        else
        {
            addLabels(gridX);
        }

        if (mLastAddedButton != null)
            setButtonBg(mLastAddedButton, lastGridX, true);

        setBounds(0, 0, mTreeContactList.getWidth(), getPreferredSize().height);
    }

    /**
     * Clears the custom action buttons.
     */
    private void clearCustomActionButtons()
    {
        if (mCustomActionButtons != null && mCustomActionButtons.size() > 0)
        {
            Iterator<JButton> buttonsIter = mCustomActionButtons.iterator();
            while (buttonsIter.hasNext())
            {
                remove(buttonsIter.next());
            }
            mCustomActionButtons.clear();
        }

        if (mCustomActionButtonsUIGroup != null
            && mCustomActionButtonsUIGroup.size() > 0)
        {
            Iterator<JButton> buttonsIter =
                mCustomActionButtonsUIGroup.iterator();
            while (buttonsIter.hasNext())
            {
                remove(buttonsIter.next());
            }
            mCustomActionButtonsUIGroup.clear();
        }
    }

    /**
     * Initializes custom contact action buttons.
     *
     * @param contactActionButtons the list of buttons to initialize
     * @param gridX the X grid of the first button
     * @param xBounds the x bounds of the first button
     *
     * @return the new grid X coordinate after adding all the buttons
     */
    private int initGroupActionButtons(
        Collection<SIPCommButton> contactActionButtons,
        int gridX,
        int xBounds)
    {
        // Reinit the labels to take the whole horizontal space.
        addLabels(gridX + contactActionButtons.size());

        Iterator<SIPCommButton> actionsIter = contactActionButtons.iterator();
        while (actionsIter.hasNext())
        {
            final SIPCommButton actionButton = actionsIter.next();

            if (mCustomActionButtonsUIGroup == null)
                mCustomActionButtonsUIGroup = new LinkedList<>();

            mCustomActionButtonsUIGroup.add(actionButton);

            xBounds
                += addButton(actionButton, ++gridX, xBounds, false);
        }

        return gridX;
    }

    /**
     * Initializes custom contact action buttons.
     *
     * @param contactActionButtons the list of buttons to initialize
     * @param gridX the X grid of the first button
     * @param xBounds the x bounds of the first button
     *
     * @return the new grid X coordiante after adding all the buttons
     */
    protected int initContactActionButtons(
        Collection<SIPCommButton> contactActionButtons,
        int gridX,
        int xBounds)
    {
        // Reinit the labels to take the whole horizontal space.
        addLabels(gridX + contactActionButtons.size());

        Iterator<SIPCommButton> actionsIter = contactActionButtons.iterator();
        while (actionsIter.hasNext())
        {
            final SIPCommButton actionButton = actionsIter.next();

            if (mCustomActionButtons == null)
            {
                mCustomActionButtons = new LinkedList<>();
            }

            mCustomActionButtons.add(actionButton);

            xBounds += addButton(actionButton, ++gridX, xBounds, false);
        }

        return gridX;
    }

    /**
     * Draw the icon at the specified location. Paints this component as an
     * icon.
     * @param c the component which can be used as observer
     * @param g the <tt>Graphics</tt> object used for painting
     * @param x the position on the X coordinate
     * @param y the position on the Y coordinate
     */
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        g = g.create();
        try
        {
            Graphics2D g2 = (Graphics2D) g;
            AntialiasingManager.activateAntialiasing(g2);

            g2.setColor(Color.WHITE);
            g2.setComposite(AlphaComposite.
                getInstance(AlphaComposite.SRC_OVER, 0.8f));
            g2.fillRoundRect(x, y,
                            getIconWidth() - 1, getIconHeight() - 1,
                            10, 10);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(x, y,
                            getIconWidth() - 1, getIconHeight() - 1,
                            10, 10);

            // Indent component content from the border.
            g2.translate(x + 5, y + 5);

            super.paint(g2);

            g2.translate(x, y);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Returns true if the specified contact is anonymous, in the sense that they
     * have no actual contact information.  In reality, that means if they're an
     * entry in the call history with no associated number.
     *
     * @return whether the contact is anonymous
     */
    protected static boolean isContactAnonymous(UIContact uiContact)
    {
        // If this is not a SourceContact then return false
        if (!(uiContact instanceof SourceUIContact))
        {
            return false;
        }

        SourceContact sourceContact = (SourceContact) uiContact.getDescriptor();

        // "Anonymous" can only apply to entries in call history.
        // If it's not a call history SourceContact, return false.
        if (sourceContact.getContactSource().getType() != ContactSourceService.CALL_HISTORY_TYPE)
        {
            return false;
        }

        // Check for any phone number details on the call history contact.
        Pattern phoneNumberValidator = Pattern.compile(ConfigurationUtils.getPhoneNumberCallableRegex());
        List<ContactDetail> contactDetails = sourceContact.getContactDetails();
        for (ContactDetail contactDetail : contactDetails)
        {
            String userName = contactDetail.getDetail();

            if (phoneNumberValidator.matcher(userName).find())
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Calls the given treeNode.
     *
     * @param treeNode the <tt>TreeNode</tt> to call
     */
    private void call(TreeNode treeNode, boolean isVideo)
    {
        if (!(treeNode instanceof ContactNode))
        {
            return;
        }

        UIContact contactDescriptor
            = ((ContactNode) treeNode).getContactDescriptor();
        sLog.info("Calling from node with descriptor " + contactDescriptor);

        Point location = MouseInfo.getPointerInfo().getLocation();

        location.x += 4;
        location.y += 4;

        mTreeContactList.startCall(contactDescriptor, isVideo, location);
    }

    /**
     * Returns a list of MetaContacts that match the given SourceUIContact.
     * Matches are found by searching the MetaContactList by phone number and
     * IM address hence multiple matches may be found.
     *
     * @param sourceUIContact the source contact for which to find matching
     * MetaContacts.
     * @returns a list of MetaContacts that match the phone number or IM
     * address given in sourceUIContact, null if none are found.
     */
    private List<MetaContact> findMetaContacts(SourceUIContact sourceUIContact)
    {
        List<MetaContact> metaContactList = null;
        MetaContactListService metaContactListService =
                                           GuiActivator.getContactListService();
        SourceContact sourceContact = sourceUIContact.getSourceContact();

        if (!sourceContact.getContactDetails().isEmpty())
        {
            // First get the source contact's address
            ContactDetail contactDetail = sourceContact.getContactDetails().get(0);
            String contactAddress = contactDetail.getDetail();

            // Then find out if this address matches the phone number of any
            // MetaContacts.
            metaContactList =
                metaContactListService.findMetaContactByNumber(contactAddress);

            // If IM is enabled, find out if the source contact's address
            // matches the IM or SMS address of any MetaContacts.
            if (ConfigurationUtils.isImEnabled())
            {
                // SMS and IM always uses the IM provider.
                ProtocolProviderService imProvider = AccountUtils.getImProvider();

                if (imProvider != null)
                {
                    OperationSetPresence opSetPres =
                        imProvider.getOperationSet(OperationSetPresence.class);

                    // First look for a contact that matches the source
                    // contact's address.
                    Contact imContact = opSetPres.findContactByID(contactAddress);

                    if (imContact != null)
                    {
                        // Then look for the contact's MetaContact.
                        MetaContact metaContact = metaContactListService.
                                           findMetaContactByContact(imContact);

                        if (metaContact != null)
                        {
                            metaContactList.add(metaContact);
                        }
                    }
                }
            }
        }

        return metaContactList;
    }

    /**
     * Shows the appropriate user interface that would allow the user to add
     * the given <tt>SourceUIContact</tt> to their contact list.
     *
     * @param sourceContact the contact to add
     */
    private void addContact(SourceContact sourceContact)
    {
        List<ContactDetail> details = sourceContact.getContactDetails(
                    OperationSetPersistentPresence.class);
        int detailsCount = details.size();
        ManageContactService manageContactService =
            GuiActivator.getManageContactService();

        // Open an Add Contact window straight away if there is only one
        // contact detail or contacts that support multiple details are
        // supported, else show a popup menu of contact details.
        if ((detailsCount == 1) ||
            ((manageContactService != null) &&
             (manageContactService.supportsMultiFieldContacts())))
        {
            ContactDetail[] detailsArray = new ContactDetail[detailsCount];

            // 'Extra' details are those that cannot be used as addresses
            // (unlike ContactDetails, which must be usable addresses).  Load
            // any that are saved as data against the source contact.
            @SuppressWarnings("unchecked")
            Map<String, String> extraDetails =
                (Map<String, String>)sourceContact.getData(
                    SourceContact.DATA_EXTRA_DETAILS);

            String displayName = sourceContact.getDisplayName();
            if (sourceContact.getContactSource().getType() ==
                                      ContactSourceService.MESSAGE_HISTORY_TYPE &&
                !displayName.contains("@"))
            {
                // This is a message history entry and the contact's address
                // doesn't contain "@" so the address must be an SMS number.
                sLog.debug("Opening SMS 'add contact' window");
                TreeContactList.showAddSmsContactDialog(displayName);
            }
            else if (sourceContact.getContactSource().getType() ==
                                         ContactSourceService.CALL_HISTORY_TYPE)
            {
                sLog.debug("Type is history - looking for LDAP matches");
                addCallHistoryContact(displayName,
                                      extraDetails,
                                      details.toArray(detailsArray));
            }
            else
            {
                sLog.debug("Opening 'add contact' window");
                TreeContactList.showAddContactDialog(displayName,
                                                     extraDetails,
                                                     details.toArray(detailsArray));
            }
        }
        else if (detailsCount > 1)
        {
            JMenuItem addContactMenu = TreeContactList.createAddContactMenu(
                                                                 sourceContact);

            JPopupMenu popupMenu = ((JMenu) addContactMenu).getPopupMenu();

            // Add a title label.
            JLabel infoLabel = new JLabel();
            infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));
            infoLabel.setText(resources.getI18NString("service.gui.ADD_CONTACT"));

            popupMenu.insert(infoLabel, 0);
            popupMenu.insert(new Separator(), 1);

            popupMenu.setFocusable(true);
            popupMenu.setInvoker(mTreeContactList);

            Point location = new Point(mAddContactButton.getX(),
                mAddContactButton.getY() + mAddContactButton.getHeight());

            SwingUtilities.convertPointToScreen(location, mTreeContactList);

            location.y = location.y
                + mTreeContactList.getPathBounds(mTreeContactList.getSelectionPath()).y;

            popupMenu.setLocation(location.x + 8, location.y - 8);
            popupMenu.setVisible(true);
        }
    }

    /**
     * Creates the add contact window for a call history contact.  If we are
     * using LDAP contacts and not using BG contacts, then we will first look
     * for a matching contact from LDAP and use its details if possible.
     *
     * @param displayName the display name to be added
     * @param extraDetails additional details (i.e. not ContactDetails) to be added
     * @param contactDetails the contact details to be added
     */
    private void addCallHistoryContact(final String displayName,
                                       final Map<String, String> extraDetails,
                                       final ContactDetail[] contactDetails)
    {
        sLog.debug("Looking up contact " + logHasher(displayName));
        boolean doingLdapSearch = false;
        boolean usingBgContacts = GuiActivator.getConfigurationService().user().
             getBoolean("net.java.sip.communicator.BG_CONTACTS_ENABLED", false);

        final Object lock = new Object();
        final List<SourceContact> foundContacts = new ArrayList<>();

        // Phone number is found in the first element of the array
        String number = contactDetails[0].getDetail();
        Pattern query = Pattern.compile("\\Q" + number + "\\E");

        if (!usingBgContacts)
        {
            List<ContactSourceService> contactSourceServices =
                                               GuiActivator.getContactSources();

            for (ContactSourceService sourceService : contactSourceServices)
            {
                if (sourceService instanceof ExtendedContactSourceService)
                {
                    // We only check ExtendedContactSourceServices, as it is not
                    // possible to query other contact sources using patterns.
                    ExtendedContactSourceService source =
                                    (ExtendedContactSourceService)sourceService;
                    ContactQuery search = source.querySourceForNumber(query);

                    // Create a listener to handle the results of the search
                    // asynchronously.
                    search.addContactQueryListener(new ContactQueryListener()
                    {
                        @Override
                        public void queryStatusChanged(ContactQueryStatusEvent event)
                        {
                            sLog.debug("Contact look up complete");

                            synchronized (lock)
                            {
                                // Wake the thread; no need to block it any more
                                lock.notifyAll();
                            }

                            event.getQuerySource().removeContactQueryListener(this);
                        }

                        @Override
                        public void contactReceived(ContactReceivedEvent event)
                        {
                            sLog.debug("Found a matching contact");
                            boolean multipleMatches;
                            synchronized (foundContacts)
                            {
                                foundContacts.add(event.getContact());
                                multipleMatches = foundContacts.size() > 1;
                            }

                            // If we've got multiple matches, then there is no
                            // point in blocking the window creation any longer.
                            if (multipleMatches)
                            {
                                synchronized (lock)
                                {
                                    lock.notifyAll();
                                }
                            }
                        }

                        @Override
                        public void contactRemoved(ContactRemovedEvent event) {}

                        @Override
                        public void contactChanged(ContactChangedEvent event) {}
                    });

                    doingLdapSearch = true;
                }
            }
        }

        if (doingLdapSearch)
        {
            // LDAP searches are asynchronous. Thus we need a small delay before
            // we create the add contact window.  This method is called on the
            // EDT (as it results from a button press) therefore we have to
            // create a new thread that we can safely block that will make the
            // add contact window.
            sLog.debug("Doing delayed creation");
            sThreadingService.submit("Contact Adder " + displayName,
                                     new Runnable()
            {
                @Override
                public void run()
                {
                    synchronized (lock)
                    {
                        try
                        {
                            // 400 milliseconds should be sufficient to get a
                            // response from LDAP without being noticeable.
                            lock.wait(400);
                        }
                        catch (InterruptedException e)
                        {
                            // Don't care
                        }
                    }

                    // Now we've waited, do the actual lookup on the UI thread.
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            SourceContact foundContact;

                            synchronized (foundContacts)
                            {
                                foundContact = (foundContacts.size() == 1) ?
                                                    foundContacts.get(0) : null;
                                sLog.debug("Found a matching contact " +
                                                                  foundContact);
                            }

                            if (foundContact == null)
                            {
                                // No matching contact, so just use show the
                                // dialog with the details that we do know
                                TreeContactList.showAddContactDialog(
                                                                displayName,
                                                                extraDetails,
                                                                contactDetails);
                            }
                            else
                            {
                                // Got a matching contact!
                                // Go through the add contact method again, only
                                // this time use the LDAP contact that we just
                                // found.
                                addContact(foundContact);
                            }
                        }
                    }); // End of EDT runnable
                }
            }); // End of ThreadingService 'contact adder' runnable
        }
        else
        {
            sLog.debug("Creating window immediately");
            TreeContactList.showAddContactDialog(displayName,
                                                 extraDetails,
                                                 contactDetails);
        }
    }

    /**
     * Returns the drag icon used to represent a cell in all drag operations.
     *
     * @param tree the parent tree object
     * @param dragObject the dragged object
     * @param index the index of the dragged object in the tree
     *
     * @return the drag icon
     */
    public Icon getDragIcon(JTree tree, Object dragObject, int index)
    {
        DefaultContactListTreeCellRenderer dragC
            = (DefaultContactListTreeCellRenderer) getTreeCellRendererComponent(
                                                        tree,
                                                        dragObject,
                                                        false, // is selected
                                                        false, // is expanded
                                                        true, // is leaf
                                                        index,
                                                        true // has focus
                                                     );

        // We should explicitly set the bounds of all components in order that
        // they're correctly painted by paintIcon afterwards. This fixes empty
        // drag component in contact list!
        dragC.setBounds(0, 0, dragC.getIconWidth(), dragC.getIconHeight());

        Icon avatarLabelIcon = mAvatarLabel.getIcon();
        int imageHeight = 0;
        int imageWidth = 0;
        if (avatarLabelIcon != null)
        {
            imageWidth = avatarLabelIcon.getIconWidth();
            imageHeight = avatarLabelIcon.getIconHeight();
            dragC.mAvatarLabel.setBounds(
                tree.getWidth() - imageWidth, 0, imageWidth, imageHeight);
        }

        dragC.mStatusLabel.setBounds(0, 0,
                                    mStatusLabel.getWidth(),
                                    mStatusLabel.getHeight());

        dragC.mNameLabel.setBounds(mStatusLabel.getWidth(), 0,
            tree.getWidth() - imageWidth - ScaleUtils.scaleInt(5), mNameLabel.getHeight());

        dragC.mDisplayDetailsLabel.setBounds(
            mDisplayDetailsLabel.getX(),
            mNameLabel.getHeight(),
            mDisplayDetailsLabel.getWidth(),
            mDisplayDetailsLabel.getHeight());

        return dragC;
    }

    private void resetRolloverState(JButton button)
    {
        // The armed and pressed state must also be reset in case the user
        // clicked on a button and then moved off it.
        button.getModel().setRollover(false);
        button.getModel().setArmed(false);
        button.getModel().setPressed(false);
    }

    /**
     * Resets the rollover state of all rollover components in the current
     * cell. Also resets the pressed state of the components in
     */
    public void resetRolloverState()
    {
        resetRolloverState(mChatButton);
        resetRolloverState(mRolloverChatButton);
        resetRolloverState(mCallButton);
        resetRolloverState(mRolloverCallButton);
        resetRolloverState(mInviteToMeetingButton);
        resetRolloverState(mRolloverInviteToMeetingButton);
        resetRolloverState(mCallVideoButton);
        resetRolloverState(mRolloverCallVideoButton);
        resetRolloverState(mAddContactButton);
        resetRolloverState(mRolloverAddContactButton);

        if (mCustomActionButtons != null)
        {
            Iterator<JButton> buttonsIter = mCustomActionButtons.iterator();
            while (buttonsIter.hasNext())
            {
                JButton button = buttonsIter.next();
                resetRolloverState(button);
            }
        }

        if (mCustomActionButtonsUIGroup != null)
        {
            Iterator<JButton> buttonsIter = mCustomActionButtonsUIGroup.iterator();
            while (buttonsIter.hasNext())
            {
                JButton button = buttonsIter.next();
                button.getModel().setRollover(false);
            }
        }
    }

    private void resetRolloverState(JButton button, Component excludeComponent)
    {
        if (!button.equals(excludeComponent))
        {
            button.getModel().setRollover(false);
        }
    }

    /**
     * Resets the rollover state of all rollover components in the current cell
     * except the component given as a parameter.
     *
     * @param excludeComponent the component to exclude from the reset
     */
    public void resetRolloverState(Component excludeComponent)
    {
        resetRolloverState(mChatButton,            excludeComponent);
        resetRolloverState(mCallButton,            excludeComponent);
        resetRolloverState(mInviteToMeetingButton, excludeComponent);
        resetRolloverState(mCallVideoButton,       excludeComponent);
        resetRolloverState(mAddContactButton,      excludeComponent);

        if (mCustomActionButtons != null)
        {
            Iterator<JButton> buttonsIter = mCustomActionButtons.iterator();
            while (buttonsIter.hasNext())
            {
                JButton button = buttonsIter.next();
                resetRolloverState(button, excludeComponent);
            }
        }

        if (mCustomActionButtonsUIGroup != null)
        {
            Iterator<JButton> buttonsIter =
                mCustomActionButtonsUIGroup.iterator();
            while (buttonsIter.hasNext())
            {
                JButton button = buttonsIter.next();
                resetRolloverState(button, excludeComponent);
            }
        }
    }

    private void setImages(SIPCommButton button,
        ImageID iconImage,
        ImageID rolloverIcon,
        ImageID pressedIcon)
    {
        button.setIconImage(sImageLoaderService.getImage(iconImage));
        button.setRolloverIcon(sImageLoaderService.getImage(rolloverIcon));
        button.setPressedIcon(sImageLoaderService.getImage(pressedIcon));
    }

    /**
     * Loads all images and colors.
     */
    public void loadSkin()
    {
        mOpenedGroupIcon
            = sImageLoaderService.getImage(ImageLoaderService.OPENED_GROUP_ICON)
              .getImageIcon();

        mClosedGroupIcon
            = sImageLoaderService.getImage(ImageLoaderService.CLOSED_GROUP_ICON)
              .getImageIcon();

        setImages(mCallButton,
                  ImageLoaderService.CALL_BUTTON_SMALL,
                  ImageLoaderService.CALL_BUTTON_SMALL_ROLLOVER,
                  ImageLoaderService.CALL_BUTTON_SMALL_PRESSED);
        setImages(mRolloverCallButton,
                  ImageLoaderService.CALL_BUTTON_SMALL,
                  ImageLoaderService.CALL_BUTTON_SMALL_ROLLOVER,
                  ImageLoaderService.CALL_BUTTON_SMALL_PRESSED);
        setImages(mChatButton,
                  ImageLoaderService.CHAT_BUTTON_SMALL,
                  ImageLoaderService.CHAT_BUTTON_SMALL_ROLLOVER,
                  ImageLoaderService.CHAT_BUTTON_SMALL_PRESSED);
        setImages(mRolloverChatButton,
                  ImageLoaderService.CHAT_BUTTON_SMALL,
                  ImageLoaderService.CHAT_BUTTON_SMALL_ROLLOVER,
                  ImageLoaderService.CHAT_BUTTON_SMALL_PRESSED);

        mMsgReceivedImage
            = sImageLoaderService.getImage(ImageLoaderService.MESSAGE_RECEIVED_ICON);

        int groupForegroundProperty = resources
            .getColor("service.gui.DARK_TEXT");

        if (groupForegroundProperty > -1)
        {
            mGroupForegroundColor = new Color (groupForegroundProperty);
        }

        int contactForegroundProperty = resources
                .getColor("service.gui.DARK_TEXT");

        if (contactForegroundProperty > -1)
        {
            mContactForegroundColor = new Color(contactForegroundProperty);
        }

        setImages(mInviteToMeetingButton,
                  ImageLoaderService.CONFERENCE_BUTTON,
                  ImageLoaderService.CONFERENCE_BUTTON_ROLLOVER,
                  ImageLoaderService.CONFERENCE_BUTTON_PRESSED);

        setImages(mRolloverInviteToMeetingButton,
                  ImageLoaderService.CONFERENCE_BUTTON,
                  ImageLoaderService.CONFERENCE_BUTTON_ROLLOVER,
                  ImageLoaderService.CONFERENCE_BUTTON_PRESSED);

        setImages(mCallVideoButton,
                  ImageLoaderService.CALL_VIDEO_BUTTON_SMALL,
                  ImageLoaderService.CALL_VIDEO_BUTTON_SMALL_ROLLOVER,
                  ImageLoaderService.CALL_VIDEO_BUTTON_SMALL_PRESSED);

        setImages(mRolloverCallVideoButton,
                  ImageLoaderService.CALL_VIDEO_BUTTON_SMALL,
                  ImageLoaderService.CALL_VIDEO_BUTTON_SMALL_ROLLOVER,
                  ImageLoaderService.CALL_VIDEO_BUTTON_SMALL_PRESSED);

        setImages(mAddContactButton,
                  ImageLoaderService.ADD_CONTACT_BUTTON_SMALL,
                  ImageLoaderService.ADD_CONTACT_BUTTON_SMALL_ROLLOVER,
                  ImageLoaderService.ADD_CONTACT_BUTTON_SMALL_PRESSED);

        setImages(mRolloverAddContactButton,
                  ImageLoaderService.ADD_CONTACT_BUTTON_SMALL,
                  ImageLoaderService.ADD_CONTACT_BUTTON_SMALL_ROLLOVER,
                  ImageLoaderService.ADD_CONTACT_BUTTON_SMALL_PRESSED);
    }

    /**
     * Listens for contact details if not cached, we will receive when they
     * are retrieved to update current call button state, if meanwhile
     * user hasn't changed the current contact.
     */
    protected class DetailsListener
        implements OperationSetServerStoredContactInfo.DetailsResponseListener
    {
        /**
         * The source this listener is created for, if current tree node
         * changes ignore any event.
         */
        private Object mSource;

        /**
         * The button to change.
         */
        private JButton mCallButton;

        /**
         * The ui contact to update after changes.
         */
        private UIContact mUiContact;

        /**
         * Create listener.
         * @param source the contact this listener is for, if different
         *               than current ignore.
         * @param callButton
         * @param uiContact the contact to refresh
         */
        DetailsListener(Object source, JButton callButton, UIContact uiContact)
        {
            mSource = source;
            mCallButton = callButton;
            mUiContact = uiContact;
        }

        /**
         * Details have been retrieved.
         * @param details the details retrieved if any.
         */
        public void detailsRetrieved(Iterator<GenericDetail> details)
        {
            // if treenode has changed ignore
            if (!mSource.equals(mTreeNode))
            {
                return;
            }

            while (details.hasNext())
            {
                GenericDetail d = details.next();

                if (d instanceof PhoneNumberDetail &&
                    !(d instanceof PagerDetail) &&
                    !(d instanceof FaxDetail))
                {
                    final PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                    if (pnd.getNumber() != null &&
                        pnd.getNumber().length() > 0)
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                // Don't try to enable if it is hidden.
                                if (ConfigurationUtils.isCallingEnabled())
                                {
                                    mCallButton.setEnabled(true);
                                }

                                if(pnd instanceof VideoDetail)
                                {
                                    mCallVideoButton.setEnabled(true);
                                }

                                mTreeContactList.refreshContact(mUiContact);
                            }
                        });

                        return;
                    }
                 }
            }
        }
    }

    /**
     * Adds an action button to the current contact
     *
     * @param button the action button to add
     * @param gridX the position within the gridbag layout to add the button
     * @param xBounds the number of pixels from the left hand edge of the pane
     * to the left hand edge of the button
     * @param isLast indicates if this is the last button in the button bar
     */
    protected int addButton(SIPCommButton button,
                            int gridX,
                            int xBounds,
                            boolean isLast)
    {
        mLastAddedButton = button;
        mActionButtonConstraints.gridx = gridX;
        int yBounds = getButtonYBounds();

        add(button, mActionButtonConstraints);
        button.setBounds(xBounds, yBounds, button.getWidth(), button.getHeight());
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setButtonBg(button, gridX, isLast);

        return button.getWidth();
    }

    /**
     * Gets the number of vertical pixels from the top of the frame to the top
     * of the action buttons.
     */
    protected int getButtonYBounds()
    {
        return mTopBorder + mBottomBorder + 2*V_GAP +
               ComponentUtils.getStringSize(mNameLabel,
                                      mNameLabel.getText()).height +
               ComponentUtils.getStringSize(mDisplayDetailsLabel,
                                      mDisplayDetailsLabel.getText()).height;
    }

    /**
     * Sets the background of the button depending on its position in the button
     * bar.
     *
     * @param button the button which background to set
     * @param gridX the position of the button in the grid
     * @param isLast indicates if this is the last button in the button bar
     */
    protected void setButtonBg(SIPCommButton button,
                               int gridX,
                               boolean isLast)
    {
        if (!isLast)
        {
            if (gridX == 1)
                button.setBackgroundImage(sImageLoaderService.getImage(
                    ImageLoaderService.CONTACT_LIST_BUTTON_BG_LEFT));
            else if (gridX > 1)
                button.setBackgroundImage(sImageLoaderService.getImage(
                    ImageLoaderService.CONTACT_LIST_BUTTON_BG_MIDDLE));
        }
        else
        {
            if (gridX == 1) // We have only one button shown.
                button.setBackgroundImage(sImageLoaderService.getImage(
                    ImageLoaderService.CONTACT_LIST_ONE_BUTTON_BG));
            else // We set the background of the last button in the toolbar
                button.setBackgroundImage(sImageLoaderService.getImage(
                    ImageLoaderService.CONTACT_LIST_BUTTON_BG_RIGHT));
        }
    }

    /**
     * Sets the correct border depending on the contained object.
     */
    protected void setBorder()
    {
        /*
         * !!! When changing border values we should make sure that we
         * recalculate the X and Y coordinates of the buttons added in
         * initButtonsPanel and initContactActionButtons functions. If not
         * correctly calculated problems may occur when clicking buttons!
         */
        if (mTreeNode instanceof ContactNode
            && !(((ContactNode) mTreeNode).getContactDescriptor() instanceof
                    ShowMoreContact))
        {
                setBorder(BorderFactory
                    .createEmptyBorder(mTopBorder,
                                       mLeftBorder,
                                       mBottomBorder,
                                       mRightBorder));
        }
        else // GroupNode || ShowMoreContact
        {
            setBorder(BorderFactory
                .createEmptyBorder(0,
                                   mLeftBorder,
                                   0,
                                   mRightBorder));
        }
    }

    /**
     * Initializes button tool tips.
     */
    private void initButtonToolTips()
    {
        mCallButton.setToolTipText(resources
            .getI18NString("service.gui.CALL_CONTACT"));
        mRolloverCallButton.setToolTipText(resources
            .getI18NString("service.gui.CALL_CONTACT"));

        mInviteToMeetingButton.setToolTipText(resources
            .getI18NString("service.gui.INVITE_TO_MEETING"));
        mRolloverInviteToMeetingButton.setToolTipText(resources
            .getI18NString("service.gui.INVITE_TO_MEETING"));

        mCallVideoButton.setToolTipText(resources
            .getI18NString("service.gui.VIDEO_CALL"));
        mRolloverCallVideoButton.setToolTipText(resources
            .getI18NString("service.gui.VIDEO_CALL"));

        mChatButton.setToolTipText(resources
            .getI18NString("service.gui.SEND_MESSAGE"));
        mRolloverChatButton.setToolTipText(resources
            .getI18NString("service.gui.SEND_MESSAGE"));

        mAddContactButton.setToolTipText(resources
            .getI18NString("service.gui.ADD_CONTACT"));
        mRolloverAddContactButton.setToolTipText(resources
            .getI18NString("service.gui.ADD_CONTACT"));
    }

    public void setMouseOverContact(Object element)
    {
        if (element instanceof ContactNode)
        {
            UIContactImpl contact
                = ((ContactNode) element).getContactDescriptor();

            if (!contact.equals(mMouseOverContact))
            {
                setToolTipText(null);
                mToolTipTimer.restart();
                mToolTipsEnabled = false;
                mMouseOverContact = contact;
            }

            if (contact instanceof ShowMoreContact)
            {
                element = null;
            }
        }

        if (element == null)
        {
            mMouseOverContact = null;
            removeRolloverButtons();
            super.repaint();
        }
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        if (evt.getSource() == mToolTipTimer)
        {
            mToolTipsEnabled = true;
            mToolTipTimer.stop();
        }
    }

    /**
     * Sets the tooltip for this TreeContactList
     *
     * @param newTip the new tooltip text to use
     */
    public void setToolTip(String newTip)
    {
        if (mToolTip == null && newTip == null)
            return;

        // If the new tooltip and old tooltip are different, then temporarily
        // disable tooltips to restart the tooltip reshow timer.
        if ((mToolTip == null && newTip != null) ||
            (mToolTip != null && newTip == null) ||
            (!mToolTip.equals(newTip)))
        {
            mToolTipTimer.restart();
            mToolTipsEnabled = false;
            setToolTipText(null);
        }
        mToolTip = newTip;
    }
}
