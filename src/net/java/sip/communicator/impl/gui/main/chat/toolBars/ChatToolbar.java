/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChatToolBar</tt> is a <tt>JToolBar</tt> which contains contact status,
 * and buttons for meeting, fileshare, calling etc.
 * It's the main toolbar in the <tt>ChatWindow</tt>.
 */
public class ChatToolbar
    extends TransparentPanel
    implements ActionListener,
               ChatChangeListener,
               ChatSessionChangeListener,
               Skinnable,
               ContactPresenceStatusListener,
               ComponentListener
{
    /**
     * The logger.
     */
    private static final Logger sLog = Logger.getLogger(ChatToolbar.class);

    /**
     *  Text color for presence information of the contact.
     *  Text color on a Mac is not brandable.
     */
    private static final Color TEXT_COLOR =
        ConfigurationUtils.useNativeTheme() ? Color.BLACK :
            new Color(GuiActivator.getResources().getColor("service.gui.DARK_TEXT"));

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -5572510509556499465L;

    /**
     * The default size of the chat participants panel.
     */
    private static final int CHAT_PARTICIPANT_PANEL_HEIGHT = 38;

    /**
     * The image loader service
     */
    private static final ImageLoaderService sImageLoaderService =
        GuiActivator.getImageLoaderService();

    /**
     * The history button.
     */
    private final SIPCommButton mHistoryButton = new SIPCommButton(
            null,
            sImageLoaderService.getImage(ImageLoaderService.HISTORY_BG),
            sImageLoaderService.getImage(ImageLoaderService.HISTORY_ROLLOVER),
            sImageLoaderService.getImage(ImageLoaderService.HISTORY_PRESSED));

    /**
     * The Web Conference button
     */
    private final SIPCommButton mWebConferenceButton = new SIPCommButton(
        null,
        sImageLoaderService.getImage(ImageLoaderService.WEB_CONFERENCE_BG),
        sImageLoaderService.getImage(ImageLoaderService.WEB_CONFERENCE_ROLLOVER),
        sImageLoaderService.getImage(ImageLoaderService.WEB_CONFERENCE_PRESSED));

    /**
     * The send file button.
     */
    private final SIPCommButton mSendFileButton = new SIPCommButton(
             null,
             sImageLoaderService.getImage(ImageLoaderService.SEND_FILE_BG),
             sImageLoaderService.getImage(ImageLoaderService.SEND_FILE_ROLLOVER),
             sImageLoaderService.getImage(ImageLoaderService.SEND_FILE_PRESSED))
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void setEnabled(boolean enabled)
        {
            super.setEnabled(enabled);

            String toolTipRes = enabled ? "service.gui.SEND_FILE" : "service.gui.SEND_FILE_UNAVAILABLE";
            setToolTipText(sResources.getI18NString(toolTipRes));
        }
    };

    /**
     * The call button.
     */
    private final SIPCommButton mCallButton = new SIPCommButton(
        null,
        sImageLoaderService.getImage(ImageLoaderService.CALL_BUTTON_SMALL),
        sImageLoaderService.getImage(ImageLoaderService.CALL_BUTTON_SMALL_ROLLOVER),
        sImageLoaderService.getImage(ImageLoaderService.CALL_BUTTON_SMALL_PRESSED))
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void setEnabled(boolean enabled)
        {
            super.setEnabled(enabled);

            String toolTipRes = enabled ? "service.gui.CALL_CONTACT" : "service.gui.CALL_CONTACT_NO_PHONE";
            setToolTipText(sResources.getI18NString(toolTipRes));
        }
    };

    /**
     * The invite button.
     */
    private final SIPCommButton mAddParticipantButton =
        new SIPCommButton(
            null,
            sImageLoaderService.getImage(ImageLoaderService.ADD_TO_CHAT_ICON),
            sImageLoaderService.getImage(ImageLoaderService.ADD_TO_CHAT_ICON_ROLLOVER),
            sImageLoaderService.getImage(ImageLoaderService.ADD_TO_CHAT_ICON_PRESSED));

    /**
     * The configure chat room button.
     */
    private final SIPCommButton mConfigureChatButton =
        new SIPCommButton(
            null,
            sImageLoaderService.getImage(ImageLoaderService.CHAT_CONFIGURE_ICON),
            sImageLoaderService.getImage(ImageLoaderService.CHAT_CONFIGURE_ICON_ROLLOVER),
            sImageLoaderService.getImage(ImageLoaderService.CHAT_CONFIGURE_ICON_PRESSED));

    /**
     * The current <tt>ChatSession</tt> made known to this instance by the last
     * call to its {@link #chatChanged(ChatPanel)}.
     */
    private ChatSession mChatSession;

    /**
     * The chat container, where this tool bar is added.
     */
    protected final ChatContainer mChatContainer;

    /**
     * The plug-in container contained in this tool bar.
     */
    private final PluginContainer mPluginContainer;

    /**
     * The phone util used to enable/disable buttons.
     */
    private ContactPhoneUtil mContactPhoneUtil = null;

    /**
     * Panel to contain the buttons (file transfer, history, call)
     */
    private JPanel mButtonsPanel;

    /**
     * Panel to contain the presence icon and status
     */
    private JPanel mPresencePanel = new TransparentPanel();

    /**
     * Label to display the presence icon for the current transport
     */
    private JLabel mTransportImage;

    /**
     * Label to display the presence - either as set by the chat contact or from their status if
     * it's not set.
     */
    private JLabel mPresenceDescription;

    /**
     * The default phone number for the 'call' button to call
     */
    private String mDefaultPhoneNumber = null;

    /**
     * A map from ChatContact addresses to ChatParticipantUIs.  The addresses
     * must be stored as case insensitive, as addresses stored on the server
     * may not match the case of addresses stored in our contacts.
     */
    private Map<String, GroupChatParticipant> mChatParticipantMap =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * The list of UI elements for chat participants that were in this dormant
     * chat room
     */
    private List<GroupChatParticipant> mDormantChatParticipantList =
            new ArrayList<>();

    /**
     * The panel that contains all the chat participants
     */
    private JPanel mChatParticipantsPanel = new JPanel();

    /**
     * The scroll pane that views the chat participants panel
     */
    private JScrollPane mChatParticipantsScrollPane =
        new JScrollPane(mChatParticipantsPanel);

    /**
     * The panel that sits at the left of the toolbar
     */
    private JPanel mPresenceContainerPanel = new TransparentPanel();

    /**
     * The "Active Participants:" label
     */
    private final JLabel mActiveParticipantsLabel = new JLabel(
        sResources.getI18NString("service.gui.chat.ACTIVE_PARTICIPANTS"));

    /**
     * The Meta Contact List Service
     */
    private final MetaContactListService mMetaContactListService =
        GuiActivator.getContactListService();

    /**
     * The menu that is shown in a multi-user chat when the user clicks the
     * hamburger
     */
    private MultiUserChatMenu mMultiChatMenu;

    /**
     * The resource management service
     */
    private static final ResourceManagementService sResources =
        GuiActivator.getResources();

    /**
     * The MetaContact List listener for this toolbar, used to update the list
     * of chat participants in a multi-user chat
     */
    private MetaContactListListener mMclListener;
    private JPopupMenu conferenceMenu;

    /**
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     *
     * @param chatContainer The parent <tt>ChatWindow</tt>.
     */
    public ChatToolbar(ChatContainer chatContainer)
    {
        mChatContainer = chatContainer;

        init();

        mPluginContainer
            = new PluginContainer(this, Container.CONTAINER_CHAT_TOOL_BAR);

        mChatContainer.addChatChangeListener(this);

        addComponentListener(this);
    }

    /**
     * Initializes this component.
     */
    protected void init()
    {
        setLayout(new GridBagLayout());
        setOpaque(false);

        // Configure the grid bag constraints
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;

        // The left panel shows the contact status in a one-to-one chat
        mPresenceContainerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        mPresenceContainerPanel.setOpaque(false);

        // The presence panel exists within the left panel it includes a status
        // icon and label
        mPresencePanel.setLayout(new BoxLayout(mPresencePanel, BoxLayout.LINE_AXIS));

        // The chat participants panel holds all the chat participant UI
        // elements when in a multi-user chat
        mChatParticipantsPanel.setBorder(null);
        mChatParticipantsPanel.setOpaque(false);
        mChatParticipantsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        mChatParticipantsPanel.setMinimumSize(new ScaledDimension(0, CHAT_PARTICIPANT_PANEL_HEIGHT));
        mChatParticipantsPanel.setPreferredSize(new ScaledDimension(400, CHAT_PARTICIPANT_PANEL_HEIGHT));
        mChatParticipantsPanel.setBackground(null);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.weighty = 1;
        constraints.insets = new Insets(3, 0, 0, 0);

        // The chat participants scroll pane views the chat participants panel
        mChatParticipantsScrollPane.setHorizontalScrollBarPolicy(
                                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mChatParticipantsScrollPane.getViewport().setOpaque(false);
        mChatParticipantsScrollPane.getViewport().setBorder(null);
        mChatParticipantsScrollPane.setViewportBorder(null);
        mChatParticipantsScrollPane.setOpaque(false);
        mChatParticipantsScrollPane.setBorder(
                      BorderFactory.createCompoundBorder(
                          BorderFactory.createMatteBorder(1, 0, 0, 0, Color.WHITE),
                          BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY)));
        mChatParticipantsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        mChatParticipantsScrollPane.setVisible(false);
        mChatParticipantsScrollPane.setMinimumSize(new ScaledDimension(0,
                                            CHAT_PARTICIPANT_PANEL_HEIGHT));

        add(mChatParticipantsScrollPane, constraints);

        mTransportImage = new JLabel();
        // Add padding to top, left and right, but not bottom to make the
        // padding even between the top tabs and the contact list.
        mTransportImage.setBorder(BorderFactory.createEmptyBorder(2, 6, 0, 2));
        mTransportImage.setAlignmentY(CENTER_ALIGNMENT);
        mPresencePanel.add(mTransportImage);

        mPresenceDescription = new JLabel();
        mPresencePanel.add(mPresenceDescription);
        mPresenceDescription.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        mPresenceDescription.setAlignmentY(CENTER_ALIGNMENT);
        mPresenceContainerPanel.add(mPresencePanel);

        mActiveParticipantsLabel.setOpaque(false);
        ScaleUtils.scaleFontAsDefault(mActiveParticipantsLabel);

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(mPresenceContainerPanel, constraints);

        mButtonsPanel = new JPanel();
        mButtonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        mButtonsPanel.setOpaque(false);

        if (CreateConferenceMenu.isConferenceServiceEnabled())
        {
            mButtonsPanel.add(mWebConferenceButton);
        }

        mButtonsPanel.add(mSendFileButton);

        ChatPanel chatPanel = mChatContainer.getCurrentChat();
        if (chatPanel == null ||
            (chatPanel.getChatSession().getCurrentChatTransport() instanceof SMSChatTransport))
            mSendFileButton.setEnabled(false);

        mButtonsPanel.add(mHistoryButton);

        if (ConfigurationUtils.isCallingEnabled())
        {
            mButtonsPanel.add(mCallButton);
        }

        mButtonsPanel.add(mAddParticipantButton);

        mConfigureChatButton.setVisible(false);
        mButtonsPanel.add(mConfigureChatButton);

        mCallButton.setName("call");
        mCallButton.setToolTipText(sResources.getI18NString(
            "service.gui.CALL_CONTACT"));

        mHistoryButton.setName("history");
        mHistoryButton.setToolTipText(
            sResources.getI18NString("service.gui.HISTORY"));

        // Setup the web conferencing button
        mWebConferenceButton.setName("webConf");
        mWebConferenceButton.setToolTipText(
            sResources.getI18NString("service.gui.conf.START_CONFERENCE"));

        mSendFileButton.setName("sendFile");
        mSendFileButton.setToolTipText(
            sResources.getI18NString("service.gui.SEND_FILE"));
        mSendFileButton.setDisabledImage(
            sImageLoaderService.getImage(ImageLoaderService.SEND_FILE_DISABLED));

        mAddParticipantButton.setName("invite");
        mAddParticipantButton.setToolTipText(
                              sResources.getI18NString("service.gui.INVITE"));
        mAddParticipantButton.setDisabledImage(sImageLoaderService.
            getImage(ImageLoaderService.ADD_TO_CHAT_ICON_DISABLED));

        mConfigureChatButton.setName("configure");
        mConfigureChatButton.setToolTipText(
            sResources.getI18NString("service.gui.chat.MORE"));
        mConfigureChatButton.setDisabledImage(sImageLoaderService.getImage(
            ImageLoaderService.CHAT_CONFIGURE_ICON_DISABLED));

        mCallButton.addActionListener(this);
        mHistoryButton.addActionListener(this);
        mWebConferenceButton.addActionListener(this);
        mSendFileButton.addActionListener(this);
        mAddParticipantButton.addActionListener(this);
        mConfigureChatButton.addActionListener(this);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        add(mButtonsPanel, constraints);

        mMultiChatMenu = new MultiUserChatMenu(chatPanel);
        mMultiChatMenu.setInvoker(mConfigureChatButton);
    }

    /**
     * Sets the images used for the call button. If a contact has multiple
     * numbers then a different set of images are used to make this obvious
     */
    private void setCallButtonImages(MetaContact contact)
    {
        BufferedImageFuture callButtonImage;
        BufferedImageFuture callButtonImagePressed;
        BufferedImageFuture callButtonImageRollover;
        int phoneNumberCount = 1;

        if (contact != null)
        {
            phoneNumberCount = contact.getPhoneNumbers().size();
        }

        if (phoneNumberCount > 1)
        {
            callButtonImage = sImageLoaderService.getImage(
                                  ImageLoaderService.CALL_BUTTON_SMALL_MULTI);
            callButtonImagePressed = sImageLoaderService.getImage(
                                  ImageLoaderService.CALL_BUTTON_SMALL_MULTI_PRESSED);
            callButtonImageRollover = sImageLoaderService.getImage(
                                  ImageLoaderService.CALL_BUTTON_SMALL_MULTI_ROLLOVER);
        }
        else
        {
            callButtonImage = sImageLoaderService.getImage(
                                        ImageLoaderService.CALL_BUTTON_SMALL);
            callButtonImagePressed = sImageLoaderService.getImage(
                                        ImageLoaderService.CALL_BUTTON_SMALL_PRESSED);
            callButtonImageRollover = sImageLoaderService.getImage(
                                        ImageLoaderService.CALL_BUTTON_SMALL_ROLLOVER);
        }

        mCallButton.setBackgroundImage(callButtonImage);
        mCallButton.setPressedImage(callButtonImagePressed);
        mCallButton.setRolloverImage(callButtonImageRollover);
        mCallButton.setDisabledImage(sImageLoaderService.getImage(
                                       ImageLoaderService.CALL_BUTTON_SMALL_DISABLED));
        mCallButton.setEnabled(phoneNumberCount > 0);
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        // Cancel the ChatSessionChangeListener, if any.
        setChatSession(null);

        mChatContainer.removeChatChangeListener(this);

        mPluginContainer.dispose();
        if (mMclListener != null)
            mMetaContactListService.removeMetaContactListListener(mMclListener);

        ProtocolProviderService imProvider = AccountUtils.getImProvider();
        if (imProvider != null)
        {
            OperationSetPresence presenceOpSet =
                imProvider.getOperationSet(OperationSetPresence.class);

            if (presenceOpSet != null)
                presenceOpSet.removeContactPresenceStatusListener(this);
        }
    }

    /**
     * Implements ChatChangeListener#chatChanged(ChatPanel).
     *
     * @param chatPanel the <tt>ChatPanel</tt>, which changed
     */
    public void chatChanged(final ChatPanel chatPanel)
    {
        Runnable doRun = new Runnable()
        {
            public void run()
            {
                if (chatPanel == null)
                {
                    sLog.warn("Chat panel is null - returning");
                    setChatSession(null);
                    updateAddParticipantButton(null);
                    return;
                }

                // We have a chat panel, so set the current chat session to be
                // that panel's chat session.
                setChatSession(chatPanel.getChatSession());

                ProtocolProviderService imProvider = AccountUtils.getImProvider();
                boolean chatOnline = (imProvider != null) && imProvider.isRegistered();

                // We definitely want to disable the add participant button if
                // chat is offline.
                boolean disableAddParticipantButton = !chatOnline;

                if (mChatSession instanceof ConferenceChatSession)
                {
                    updateMultiUserChatUI(mChatSession.getParticipants());
                    mMultiChatMenu.setChatPanel(chatPanel);
                    if (chatOnline)
                    {
                        // The chat account is online so enable the button,
                        // unless the user has left the group chat.
                        mConfigureChatButton.setEnabled(!chatPanel.hasLeft());
                    }
                    else
                    {
                        mConfigureChatButton.setEnabled(false);
                    }

                    if (!mChatParticipantsScrollPane.isVisible())
                    {
                        setMultiChatUI(true);
                    }
                }
                else
                {
                    MetaContact contact
                        = GuiActivator.getUIService().getChatContact(chatPanel);

                    setCallButtonImages(contact);

                    if (contact != null)
                    {
                        // We have a contact, so associate the call button
                        // with all of its phone numbers. We can also enable
                        // file transfer, as long as both chats are online and the
                        // chat supports file transfer (i.e. we don't have an
                        // SMS transport selected and we do support the file
                        // transfer operation set).
                        mDefaultPhoneNumber = null;
                        for (PluginComponent c : mPluginContainer.getPluginComponents())
                            c.setCurrentContact(contact);

                        Contact imContact = contact.getIMContact();
                        PresenceStatus presenceStatus = imContact != null
                            ? imContact.getPresenceStatus()
                            : null;
                        boolean isOnline = presenceStatus != null && presenceStatus.isOnline();

                        mSendFileButton.setEnabled(chatOnline &&
                            !(mChatSession.getCurrentChatTransport() instanceof SMSChatTransport) &&
                            chatPanel.findFileTransferChatTransport() != null &&
                            isOnline);

                        new UpdateCallButtonWorker(contact).start();
                    }
                    else if (mChatSession instanceof SMSChatSession)
                    {
                        // We don't have a contact, but this is an SMS chat so
                        // we have a phone number - set that as the phone
                        // number for the call button.
                        mDefaultPhoneNumber = (String) mChatSession.getDescriptor();
                    }

                    // Next, try to determine the presence text and icon to
                    // display.
                    ProtocolProviderService protocolProvider = null;
                    PresenceStatus status = null;
                    BufferedImageFuture statusImage = null;

                    // The logic for determining status is complicated.
                    // If we have a contact, then we want to show the contact as offline if this client
                    // is not connected to chat, or if the contact has no transport for chat (i.e. isn't
                    // an IM contact). However, if we are connected to this contact for chat, then we pay
                    // attention to the whole contact's presence, not just the IM elements. So e.g. if the
                    // contact is just a non-Accession line reporting DND, we consider them offline as we
                    // can't contact them.  If they have a non-Accession presence 'DND' and an Accession
                    // presence 'online' then we consider them 'DND', because we can message them but
                    // overall 'DND' takes precedence over 'online'. If we don't have a contact, then
                    // we just check the chat transport, there are no other elements to consider.
                    if (mChatSession != null)
                    {
                        ChatTransport chatTransport =
                            mChatSession.getCurrentChatTransport();

                        if (chatTransport != null)
                        {
                            if (contact != null)
                            {
                                Contact imContact = contact.getIMContact();
                                if (imContact != null)
                                {
                                    status = imContact.getPresenceStatus();
                                }
                            }

                            if (status == null)
                            {
                                status = chatTransport.getStatus();
                            }

                            protocolProvider = chatTransport.getProtocolProvider();

                            if ((status != null) && (protocolProvider != null))
                            {
                                statusImage = status.getStatusIcon();
                            }
                        }
                    }

                    String logMessage;

                    // If the status is null, we're probably either still
                    // setting up or in the process of tearing down the chat
                    // connection or the chat session, so disable the add
                    // participant button and the multi user chat menu and set
                    // the contact's presence to offline.
                    boolean haveStatusImage = (statusImage != null);
                    if (!chatOnline || !haveStatusImage)
                    {
                        logMessage =
                            "We're either offline or setting up/tearing down " +
                            "the chat session so disable the toolbar for now.";
                        disableAddParticipantButton = true;
                        mConfigureChatButton.setEnabled(false);
                        statusImage = GlobalStatusEnum.OFFLINE.getStatusIcon();
                        protocolProvider = imProvider;
                    }
                    else
                    {
                        logMessage = "We're online and have a working chat " +
                                     "session - enabling the toolbar.";
                    }

                    sLog.interval("Contact: " + contact + ",",
                                  logMessage,
                                  "Chat Online? " + chatOnline,
                                  "Have Status Image? " + haveStatusImage,
                                  "Status = " + status);

                    ImageIconFuture chatImage =
                        sImageLoaderService.getIndexedProtocolImage(
                            statusImage, protocolProvider).getImageIcon();
                    chatImage.addToLabel(mTransportImage);

                    // The presence string used when we don't have any presence
                    // info for the contact.
                    String noPresenceInfo = sResources.getI18NString(
                        "service.gui.CONTACT_IM_NOT_SUPPORTED");
                    String presenceText;
                    Color presenceColor;

                    if (chatOnline)
                    {
                        String contactStatusMsg = null;

                        // If we've got an underlying IM contact (and we
                        // should if we're online and this isn't SMS), then
                        // use its status.  Otherwise, use the status obtained
                        // from the transport.
                        if (contact != null)
                        {
                            Contact imContact = contact.getIMContact();

                            if (imContact != null)
                            {
                                contactStatusMsg = imContact.getStatusMessage();
                            }
                        }

                        if (contactStatusMsg != null && contactStatusMsg.length() > 0)
                        {
                            presenceText = contactStatusMsg;
                        }
                        else if (status == null || status.equals(GlobalStatusEnum.UNKNOWN))
                        {
                            presenceText = noPresenceInfo;
                        }
                        else
                        {
                            presenceText = status.getStatusName();
                        }
                    }
                    else
                    {
                        presenceText = GlobalStatusEnum.getI18NStatusName(
                                                     GlobalStatusEnum.OFFLINE);
                    }

                    // If the presence text is "no presence info" we want it to
                    // appear grey, otherwise make the text the branded colour.
                    presenceColor = noPresenceInfo.equals(presenceText) ?
                                                       Color.GRAY : TEXT_COLOR;

                    mPresenceDescription.setText(presenceText);
                    ScaleUtils.scaleFontAsDefault(mPresenceDescription);
                    mPresenceDescription.setForeground(presenceColor);

                    // This field has some content, so ensure that accessibility users can navigate to it
                    mPresenceDescription.setFocusable(true);
                }

                // If we concluded that we needed to disable the add
                // participant button, do so now.  Otherwise, pass in the
                // session so that the button is configured appropriately.
                if (disableAddParticipantButton)
                {
                    updateAddParticipantButton(null);
                }
                else
                {
                    updateAddParticipantButton(mChatSession);
                }
            }
        };

        try
        {
            // If this is the EDT, then just run the runnable now. Otherwise we
            // must block this thread until the layout work has been completed.
            // The layout work should be done on the EDT so we use invokeAndWait
            if (SwingUtilities.isEventDispatchThread())
                doRun.run();
            else
                SwingUtilities.invokeAndWait(doRun);
        }
        catch (InvocationTargetException e)
        {
            sLog.error("Exception during update", e);
        }
        catch (InterruptedException e)
        {
            sLog.error("Interupt exception during update", e);
        }
    }

    /**
     * Determines whether the add participant button should be shown, and
     * whether it is disabled.
     */
    private void updateAddParticipantButton(ChatSession chatSession)
    {
        boolean isEnabled = true;
        boolean isVisible = false;

        if (ConfigurationUtils.isMultiUserChatEnabled())
        {
            // The add participant button should be disabled if this an SMS chat
            // session, or if this is a MetaContact chat session and the
            // participant is offline
            if (chatSession instanceof SMSChatSession)
            {
                isEnabled = false;
                isVisible = true;
            }
            else if (chatSession instanceof MetaContactChatSession)
            {
                if (AccountUtils.getImProvider() != null &&
                    AccountUtils.getImProvider().isRegistered())
                {
                    // The descriptor of a MetaContact chat session is always a MetaContact
                    MetaContact metaContact = (MetaContact) chatSession.getDescriptor();

                    // If the MetaContact has an IM contact, the button should be enabled.
                    isEnabled = (metaContact.getIMContact() != null);
                }
                else
                {
                    isEnabled = false;
                }

                isVisible = true;
            }
            else if (chatSession == null)
            {
                isEnabled = false;
                isVisible = true;
            }
        }

        mAddParticipantButton.setVisible(isVisible);
        mAddParticipantButton.setEnabled(isEnabled);
    }

    /**
     * Updates the multi-user chat UI by refreshing the list of participants in
     * the toolbar
     *
     * @param participants an iterator over the number of participants in this
     * chat session
     */
    public void updateMultiUserChatUI(List<ChatContact<?>> participants)
    {
        ProtocolProviderService imProvider = AccountUtils.getImProvider();
        if (imProvider == null || !imProvider.isRegistered())
        {
            // If the provider is not registered, there's no point updating
            // the UI.  Also, if we do, the participants will flicker in and
            // out of view whenever we try to reconnect, which is annoying.
            sLog.debug("Returning without updating as provider is not registered");
            return;
        }

        List<String> participantsToRemove = new ArrayList<>();
        synchronized (mChatParticipantMap)
        {
            participantsToRemove = new ArrayList<>
                    (mChatParticipantMap.keySet());

            for (ChatContact<?> chatParticipant : participants)
            {
                Object descriptor = chatParticipant.getDescriptor();

                if (!(descriptor instanceof ChatRoomMember))
                {
                    sLog.debug(
                        "Returning as descriptor is not ChatRoomMember: " +
                                                                    descriptor);
                    continue;
                }

                // Convert the participant address to lower case to avoid case
                // mis-matches between our local contact list and the server
                // roster.
                String participantAddress =
                    ((ChatRoomMember) descriptor).getContactAddressAsString().toLowerCase();

                if (mChatParticipantMap.keySet().contains(participantAddress))
                {
                    // Don't remove this participant, it is already showing and is
                    // still present in the group chat
                    participantsToRemove.remove(participantAddress);
                }
                else
                {
                    // We haven't added this participant yet, add it now
                    addParticipant(chatParticipant);
                }
            }
        }

        // We now have a list of participants that are showing in the UI but
        // are no longer in the chat room, remove these from the UI now.
        removeParticipants(participantsToRemove);
    }

    /**
     * Adds a chat participant UI element
     *
     * @param chatParticipant the chat participant to add
     */
    private void addParticipant(ChatContact<?> chatParticipant)
    {
        synchronized (mChatParticipantMap)
        {
            Object descriptor = chatParticipant.getDescriptor();

            if (!(descriptor instanceof ChatRoomMember))
            {
                sLog.debug(
                    "Returning as descriptor is not ChatRoomMember: " +
                                                                    descriptor);
                return;
            }

            // Convert the participant address to lower case to avoid case
            // mis-matches between our local contact list and the server roster.
            String participantAddress =
                ((ChatRoomMember) descriptor).getContactAddressAsString().toLowerCase();

            // Ensure we do not add ourselves as a chat participant
            if (chatParticipant.getDescriptor() instanceof ChatRoomMember)
            {
                ProtocolProviderService imProvider = AccountUtils.getImProvider();
                if (imProvider == null ||
                    imProvider.getAccountID().getUserID().equalsIgnoreCase(participantAddress))
                {
                    return;
                }
            }

            GroupChatParticipant participantUI = new GroupChatParticipant(chatParticipant);
            mChatParticipantsPanel.add(participantUI);
            mChatParticipantMap.put(participantAddress, participantUI);
            sLog.debug("Added new participant to UI: " + sanitiseChatAddress(participantAddress));
        }

        resizeChatParticipantsPanel();
    }

    /**
     * Configures the toolbar UI to show or hide the multi-user chat components
     *
     * @param isMultiChat whether the chat panel is a group chat
     */
    private void setMultiChatUI(boolean isMultiChat)
    {
        mChatParticipantsScrollPane.setVisible(isMultiChat);
        mConfigureChatButton.setVisible(isMultiChat);

        if (isMultiChat)
        {
            mPresenceContainerPanel.remove(mPresencePanel);
            mPresenceContainerPanel.add(mActiveParticipantsLabel);

            mCallButton.setVisible(false);
            mSendFileButton.setVisible(false);

            mMclListener = new MainToolBarMCLListener();
            mMetaContactListService.addMetaContactListListener(mMclListener);

            // Add ourselves as a contact presence change listener so we
            // can update the presence icons accordingly. Unless we don't have
            // an IM provider, which will be the case if we've lost contact
            // with the chat server/signed out of chat.
            ProtocolProviderService imProvider = AccountUtils.getImProvider();
            if (imProvider != null)
            {
                OperationSetPresence presenceOpSet =
                    imProvider.getOperationSet(OperationSetPresence.class);

                if (presenceOpSet != null)
                {
                    presenceOpSet.addContactPresenceStatusListener(this);
                }
            }
        }
        else
        {
            mPresenceContainerPanel.remove(mActiveParticipantsLabel);
            mPresenceContainerPanel.add(mPresencePanel);
        }
    }

    /**
     * Removes a chat participant UI element
     *
     * @param participantsToRemove the list of addresses of participants to
     * remove
     */
    private void removeParticipants(List<String> participantsToRemove)
    {
        synchronized (mChatParticipantMap)
        {
            for (String participant : participantsToRemove)
            {
                GroupChatParticipant chatParticipant = mChatParticipantMap.get(participant);
                if (chatParticipant != null)
                {
                    mChatParticipantsPanel.remove(chatParticipant);
                }
                mChatParticipantMap.remove(participant);
                sLog.debug("Removed participant: " + sanitiseChatAddress(participant));
            }
        }

        resizeChatParticipantsPanel();
    }

    /**
     * Disables all of the chat participant UI elements
     */
    public void disableParticipants()
    {
        synchronized (mChatParticipantMap)
        {
            for (GroupChatParticipant participant : mChatParticipantMap.values())
            {
                participant.setEnabled(false);
            }
        }
    }

     /**
     * Returns list of <tt>ChatTransport</tt> (i.e. contact) that supports the
     * specified <tt>OperationSet</tt>.
     *
     * @param transports list of <tt>ChatTransport</tt>
     * @param opSetClass <tt>OperationSet</tt> to find
     * @return list of <tt>ChatTransport</tt> (i.e. contact) that supports the
     * specified <tt>OperationSet</tt>.
     */
    private List<ChatTransport> getOperationSetForCapabilities(
            List<ChatTransport> transports,
            Class<? extends OperationSet> opSetClass)
    {
        List<ChatTransport> list = new ArrayList<>();

        for (ChatTransport transport : transports)
        {
            ProtocolProviderService protocolProvider
                = transport.getProtocolProvider();
            OperationSetContactCapabilities capOpSet
                = protocolProvider.getOperationSet(
                        OperationSetContactCapabilities.class);
            OperationSetPersistentPresence presOpSet
                = protocolProvider.getOperationSet(
                        OperationSetPersistentPresence.class);

            if (capOpSet == null)
            {
                list.add(transport);
            }
            else if (presOpSet != null)
            {
                Contact contact
                    = presOpSet.findContactByID(transport.getName());

                if ((contact != null)
                        && (capOpSet.getOperationSet(contact, opSetClass)
                                != null))
                {
                    // It supports OpSet for at least one of its
                    // ChatTransports
                    list.add(transport);
                }
            }
        }

        return list;
    }

    /**
     * Implements
     * ChatSessionChangeListener#currentChatTransportChanged(ChatSession).
     * @param chatSession the <tt>ChatSession</tt>, which transport has changed
     */
    public void currentChatTransportChanged(ChatSession chatSession)
    {
        if (chatSession == null)
            return;

        ChatTransport currentTransport = chatSession.getCurrentChatTransport();

        if (currentTransport == null)
            return;

        Object currentDescriptor = currentTransport.getDescriptor();

        if (currentDescriptor instanceof Contact)
        {
            mSendFileButton.setEnabled(true);

            Contact contact = (Contact) currentDescriptor;

            for (PluginComponent c : mPluginContainer.getPluginComponents())
                c.setCurrentContact(contact);
        }
        else
        {
            mSendFileButton.setEnabled(false);
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt>, when one of the tool bar buttons is
     * clicked.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        AbstractButton button = (AbstractButton) e.getSource();
        String buttonName = button.getName();

        ChatPanel chatPanel = mChatContainer.getCurrentChat();

        sLog.user(buttonName + " clicked in chat window.");

        if (buttonName.equals("sendFile"))
        {
            String title = sResources.getI18NString("service.gui.SEND_FILE_TITLE");
            SipCommFileChooser scfc = GenericFileDialog.create(
                null, title, SipCommFileChooser.LOAD_FILE_OPERATION,
                    ConfigurationUtils.getSendFileLastDir());
            File selectedFile = scfc.getFileFromDialog();
            if (selectedFile != null)
            {
                ConfigurationUtils.setSendFileLastDir(
                    selectedFile.getParent());
            }
        }
        else if (buttonName.equals("history"))
        {
            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            ChatSession chatSession = chatPanel.getChatSession();

            historyWindowManager.displayHistoryWindowForChatDescriptor(chatSession.getDescriptor(), sLog);
        }
        else if (buttonName.equals("call"))
        {
            call();
        }
        else if (buttonName.equals("invite"))
        {
            // Send an analytic of us creating a new group chat, with the
            // number of active group chats as a parameter.
            String activeChatRooms = "";
            ProtocolProviderService imProvider = AccountUtils.getImProvider();
            if (imProvider != null)
            {
                OperationSetMultiUserChat opSetMuc =
                    imProvider.getOperationSet(OperationSetMultiUserChat.class);

                if (opSetMuc != null)
                {
                    activeChatRooms = String.valueOf(opSetMuc.getActiveChatRooms());
                }
            }

            List<AnalyticsParameter> params = new ArrayList<>();

            params.add(new AnalyticsParameterSimple(
                AnalyticsParameter.NAME_COUNT_GROUP_IMS, activeChatRooms));

            GuiActivator.getAnalyticsService().onEvent(
                AnalyticsEventType.NEW_GROUP_CHAT, params);

            // We're inviting others into this chat, so make sure the person
            // we're currently chatting to is preselected in the add chat
            // participant dialog.
            HashSet<MetaContact> preselected = null;

            if (mChatSession instanceof MetaContactChatSession)
            {
                preselected = new HashSet<>();
                preselected.add((MetaContact)mChatSession.getDescriptor());
            }

            new AddChatParticipantsDialog(chatPanel, false, preselected).setVisible(true);
        }
        else if (buttonName.equals("configure"))
        {
            int x = button.getLocationOnScreen().x + button.getWidth();
            int y = button.getLocationOnScreen().y + button.getHeight();
            mMultiChatMenu.setLocation(x, y);
            mMultiChatMenu.setVisible(true);
        }
        else if (buttonName.equalsIgnoreCase("webConf"))
        {
            // Get the descriptor to determine the Chat type
            Object descriptor = chatPanel.getChatSession().getDescriptor();

            // If the descriptor is a ChatRoomWrapper, we need to get the
            // chatroom for the CreateMeetingMenu to use to know which
            // contacts to pre-select.
            CreateConferenceMenu createMeetingMenu;
            if (descriptor instanceof ChatRoomWrapper)
            {
                sLog.info("Creating CreateMeetingMenu with ChatRoom");
                ChatRoomWrapper wrapper = (ChatRoomWrapper) descriptor;
                ChatRoom chatRoom = wrapper.getChatRoom();
                createMeetingMenu =
                    new CreateConferenceMenu(chatRoom);
            }
            else if (descriptor instanceof MetaContact)
            {
                sLog.info("Creating CreateMeetingMenu with MetaContact");
                MetaContact metaContact = (MetaContact) descriptor;
                createMeetingMenu =
                    new CreateConferenceMenu(metaContact);
            }
            else
            {
                sLog.info(
                    "Creating CreateMeetingMenu with null CreateMeetingMenuContainer");
                createMeetingMenu =
                    new CreateConferenceMenu(null);
            }

            conferenceMenu = createMeetingMenu.getJPopupMenu(mWebConferenceButton);
            conferenceMenu.setVisible(true);
            conferenceMenu.requestFocusInWindow();
        }
    }

    /**
     * Returns the button used to show the history window.
     *
     * @return the button used to show the history window.
     */
    public SIPCommButton getHistoryButton()
    {
        return mHistoryButton;
    }

    /**
     * Sets the current <tt>ChatSession</tt> made known to this instance by the
     * last call to its {@link #chatChanged(ChatPanel)}.
     *
     * @param chatSession the <tt>ChatSession</tt> to become current for this
     * instance
     */
    private void setChatSession(ChatSession chatSession)
    {
        if (mChatSession != chatSession)
        {
            if (mChatSession instanceof MetaContactChatSession ||
                mChatSession instanceof SMSChatSession)
            {
                mChatSession.removeChatTransportChangeListener(this);
            }

            mChatSession = chatSession;

            if (mChatSession instanceof MetaContactChatSession ||
                mChatSession instanceof SMSChatSession)
            {
                mChatSession.addChatTransportChangeListener(this);
            }
        }
    }

    /**
     * Reloads icons for buttons.
     */
    public void loadSkin()
    {
        mHistoryButton.setIconImage(sImageLoaderService.getImage(
                ImageLoaderService.HISTORY_BG));

        mWebConferenceButton.setIconImage(sImageLoaderService.getImage(
            ImageLoaderService.WEB_CONFERENCE_BG));

        mSendFileButton.setIconImage(sImageLoaderService.getImage(
                ImageLoaderService.SEND_FILE_BG));

        mCallButton.setIconImage(sImageLoaderService.getImage(
                ImageLoaderService.CHAT_CALL));

        mAddParticipantButton.setIconImage(sImageLoaderService.getImage(
            ImageLoaderService.ADD_TO_CHAT_ICON));
    }

    /**
     * Establishes a call.
     */
    private void call()
    {
        ChatPanel chatPanel = mChatContainer.getCurrentChat();

        ChatSession chatSession = chatPanel.getChatSession();

        if (chatSession.getDescriptor() instanceof ChatRoomWrapper)
        {
            // If we are in a group chat, then we actually want to create an
            // audio conference
            ChatRoom chatRoom = ((ChatRoomWrapper) chatSession.
                getDescriptor()).getChatRoom();

            sLog.info("Creating audio conference for chat room: " +
                      sanitiseChatRoom(chatRoom.getIdentifier()));

            ConferenceService conferenceService = GuiActivator.getConferenceService();
            conferenceService.createOrAdd(chatRoom, true);
        }
        else if (mDefaultPhoneNumber != null)
        {
            sLog.info("Calling default phone number: " + mDefaultPhoneNumber);

            // If we have a default number, this API will format the phone
            // number then call it
            CallManager.call(mDefaultPhoneNumber, false);
        }
        else
        {
            sLog.info("Getting all phone details for call");
            Class<? extends OperationSet> opSetClass =
                                                   OperationSetBasicTelephony.class;

            List<ChatTransport> telTransports = null;
            if (chatSession != null)
                telTransports = chatSession
                    .getTransportsForOperationSet(opSetClass);

            List<ChatTransport> contactOpSetSupported;

            contactOpSetSupported =
                getOperationSetForCapabilities(telTransports, opSetClass);

            List<UIContactDetail> res = new ArrayList<>();
            for (ChatTransport ct : contactOpSetSupported)
            {
                HashMap<Class<? extends OperationSet>, ProtocolProviderService> m =
                        new HashMap<>();
                m.put(opSetClass, ct.getProtocolProvider());

                UIContactDetailImpl d =
                new UIContactDetailImpl(
                    ct.getName(),
                    ct.getDisplayName(),
                    null,
                    null,
                    null,
                    m,
                    null,
                    ct.getName());
                PresenceStatus status = ct.getStatus();
                BufferedImageFuture statusIconImage = status.getStatusIcon();

                if (statusIconImage != null)
                {
                    d.setStatusIcon(
                        sImageLoaderService.getIndexedProtocolImage(
                            statusIconImage,
                            ct.getProtocolProvider())
                            .getImageIcon());
                }

                res.add(d);
            }

            Point location = new Point(mCallButton.getX(),
                mCallButton.getY() + mCallButton.getHeight());

            SwingUtilities.convertPointToScreen(
                location, mButtonsPanel);

            CallManager.call(mContactPhoneUtil,
                             res,
                             null,
                             false,
                             mCallButton,
                             location);
        }
    }

    /**
     * Updates the relevant chat participant UI elements if appropriate to
     * respond to changes in the underlying MetaContact or ProtoContact. One
     * and only one of either metaContact or chatAddress must be supplied to
     * this method.
     *
     * @param metaContact the MetaContact to update, may be null
     * @param chatAddress the address of the ProtoContact to update, may be null
     */
    protected void updateChatParticipant(MetaContact metaContact,
                                         String chatAddress)
    {
        if (metaContact != null && metaContact.getIMContact() != null)
        {
            chatAddress = metaContact.getIMContact().getAddress();
        }

        Set<GroupChatParticipant> contactsToUpdate = new HashSet<>();

        synchronized (mChatParticipantMap)
        {
            for (Map.Entry<String, GroupChatParticipant> participant : mChatParticipantMap.entrySet())
            {
                // Find any participants that match the given chat address.
                // This string of methods is safe because the key returns a
                // ChatContact. This must be a ChatRoomMember because we are in
                // a conference session.
                if (chatAddress != null)
                {
                    String contactAddress = participant.getKey();
                    if (chatAddress.equalsIgnoreCase(contactAddress))
                    {
                        // One of the participants we are displaying has changed
                        // somehow, re-initialise it.
                        contactsToUpdate.add(participant.getValue());

                        // We have added this UI element to the list of elements to
                        // update, so move on to the next element
                        continue;
                    }
                }

                // Find any participants that match the given MetaContact UID
                if (participant.getValue().mMetaContact != null &&
                    metaContact != null &&
                    participant.getValue().mMetaContact.getMetaUID().equals(metaContact.getMetaUID()))
                {
                    contactsToUpdate.add(participant.getValue());
                }
            }
        }

        // We now have a list of elements to update, do this now
        for (GroupChatParticipant participant : contactsToUpdate)
        {
            participant.updateComponent();
        }

        // A name may have changed which affects the layout of the participants
        // panel, therefore resize it appropriately
        if (!contactsToUpdate.isEmpty())
        {
            resizeChatParticipantsPanel();
        }
    }

    @Override
    public void contactPresenceStatusChanged(ContactPresenceStatusChangeEvent evt)
    {
        // We do not worry about which contact this is for, as it is just as
        // expensive to update all UI elements
        synchronized (mChatParticipantMap)
        {
            for (GroupChatParticipant participant : mChatParticipantMap.values())
            {
                participant.updatePresence();
            }
        }

        synchronized (mDormantChatParticipantList)
        {
            for (GroupChatParticipant participant : mDormantChatParticipantList)
            {
                participant.updatePresence();
            }
        }
    }

    /**
     * Creates a UI element for each participant in the set provided and adds
     * it to the participants panel.
     *
     * @param participants the set of participants for which to add UI elements
     */
    public void initializeParticipants(Set<String> participants)
    {
        synchronized (mDormantChatParticipantList)
        {
            for (String participant : participants)
            {
                // Do not add ourselves as a chat participant
                ProtocolProviderService imProvider = AccountUtils.getImProvider();
                if (imProvider == null ||
                    imProvider.getAccountID().getUserID().equalsIgnoreCase(participant))
                {
                    continue;
                }

                GroupChatParticipant participantUI = new GroupChatParticipant(participant);
                mChatParticipantsPanel.add(participantUI);
                mDormantChatParticipantList.add(participantUI);
            }
        }

        setMultiChatUI(true);
        mActiveParticipantsLabel.setText(sResources.getI18NString(
                                         "service.gui.chat.PAST_PARTICIPANTS"));

        // Disable the 'more' button, it is useless until the chat is active
        mConfigureChatButton.setEnabled(false);

        mChatParticipantsPanel.validate();
        mChatParticipantsPanel.repaint();
    }

    /**
     * Removes any live chat participant UI elements and clears any old
     * values in the dormant chat participant map so the toolbar is ready
     * for accepting dormant chat participants.
     */
    public void convertLiveToDormant()
    {
        synchronized (mChatParticipantMap)
        {
            clearParticipants(
                    new ArrayList<>(mChatParticipantMap.values()));
        }

        synchronized (mDormantChatParticipantList)
        {
            mDormantChatParticipantList.clear();
        }
    }

    /**
     * Removes any dormant chat participant UI elements and clears any old
     * values in the active chat participant map so the toolbar is ready
     * for accepting live chat participants.
     */
    public void convertDormantToLive()
    {
        mActiveParticipantsLabel.setText(sResources.getI18NString(
            "service.gui.chat.ACTIVE_PARTICIPANTS"));

        mConfigureChatButton.setEnabled(true);

        synchronized (mDormantChatParticipantList)
        {
            clearParticipants(mDormantChatParticipantList);
        }

        synchronized (mChatParticipantMap)
        {
            mChatParticipantMap.clear();
        }
    }

    /**
     * Removes all chat participant UI elements and clears all values in all
     * chat participant lists so the toolbar is ready for accepting new chat
     * participants.
     */
    public void clearAllParticipants()
    {
        synchronized (mChatParticipantMap)
        {
            clearParticipants(
                    new ArrayList<>(mChatParticipantMap.values()));
            mChatParticipantMap.clear();
        }

        synchronized (mDormantChatParticipantList)
        {
            clearParticipants(mDormantChatParticipantList);
            mDormantChatParticipantList.clear();
        }
    }

    /**
     * Removes the given chat participant UI elements.
     *
     * @param participantList the chat participants to remove.
     */
    private void clearParticipants(List<GroupChatParticipant> participantList)
    {
        for (GroupChatParticipant element : participantList)
        {
            mChatParticipantsPanel.remove(element);
        }

        mChatParticipantsPanel.validate();
        mChatParticipantsPanel.repaint();
    }

    /**
     * When the window is resized, or participants are added/removed we need to
     * resize the chat participants panel. If this is not done then elements
     * may not be visible, or the scroll pane will scroll too far.
     */
    private void resizeChatParticipantsPanel()
    {
        List<GroupChatParticipant> chatParticipants;

        // We are either showing dormant participants or active participants,
        // so we must examine the correct list
        boolean noActiveParticipants = mChatParticipantMap.isEmpty();

        if (noActiveParticipants)
        {
            synchronized (mDormantChatParticipantList)
            {
                chatParticipants =
                        new ArrayList<>(mDormantChatParticipantList);
            }
        }
        else
        {
            synchronized (mChatParticipantMap)
            {
                chatParticipants =
                        new ArrayList<>(mChatParticipantMap.values());
            }
        }

        // Get the maximum y co-ordinate of the currently displayed UI
        // elements. The panel will have to be higher than this value
        int maxY = 0;
        for (GroupChatParticipant component : chatParticipants)
        {
            int componentY = component.getY();
            int componentHeight = component.getHeight();

            if (componentY + componentHeight > maxY)
            {
                maxY = componentY + componentHeight;
            }
        }

        Dimension newSize = new Dimension((int) getSize().getWidth(), maxY + 3);

        mChatParticipantsPanel.setMinimumSize(newSize);
        mChatParticipantsPanel.setMaximumSize(newSize);
        mChatParticipantsPanel.setPreferredSize(newSize);
        mChatParticipantsPanel.setSize(newSize);

        // Must revalidate for the scroll pane to show/hide the scroll bars
        mChatParticipantsPanel.validate();
        mChatParticipantsPanel.repaint();

        // Ensure we show scroll bars if we need them
        if (mChatParticipantsScrollPane.getHeight() < mChatParticipantsPanel.getHeight())
        {
            mChatParticipantsScrollPane.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }
        else
        {
            mChatParticipantsScrollPane.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        }
        mChatParticipantsScrollPane.revalidate();
        mChatParticipantsScrollPane.repaint();
    }

    @Override
    public void componentResized(ComponentEvent arg0)
    {
        resizeChatParticipantsPanel();
    }

    @Override
    public void componentHidden(ComponentEvent arg0){}

    @Override
    public void componentMoved(ComponentEvent arg0){}

    @Override
    public void componentShown(ComponentEvent arg0){}

    public boolean isConferenceMenuVisible()
    {
        return conferenceMenu != null && conferenceMenu.isVisible();
    }

    public void closeConferenceMenu()
    {
        conferenceMenu.setVisible(false);
        conferenceMenu = null;
    }

    /**
     * Searches for phone numbers in <tt>MetaContact/tt> operation sets.
     * And changes the call button enable/disable state.
     */
    private class UpdateCallButtonWorker
        extends SwingWorker
    {
        /**
         * The current contact.
         */
        private MetaContact mContact;

        /**
         * Has this contact any phone.
         */
        private boolean mIsCallEnabledForContact = false;

        /**
         * Creates worker.
         * @param contact
         */
        UpdateCallButtonWorker(MetaContact contact)
        {
            mContact = contact;
        }

        /**
         * Executes in worker thread.
         * @return
         */
        @Override
        protected Object construct()
        {
            mContactPhoneUtil = ContactPhoneUtil.getPhoneUtil(mContact);
            mIsCallEnabledForContact = mContactPhoneUtil.isCallEnabled();

            return null;
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        protected void finished()
        {
            mCallButton.setEnabled(mIsCallEnabledForContact);
        }
    }

    /**
     * A panel that represents a chat participant in a group chat
     */
    private class GroupChatParticipant extends SIPCommSnakeButton implements ActionListener
    {
        private static final long serialVersionUID = 0L;

        /**
         * The chat participant that this UI represents
         */
        private ChatContact<?> mChatParticipant;

        /**
         * The button resource string
         */
        private static final String IMAGE_RESOURCE_STRING = "service.gui.button.chat.PARTICIPANT";

        /**
         * The presence status of the contact - could be null if there is no
         * underlying IM buddy.
         */
        private PresenceStatus mPresenceStatus;

        /**
         * The address of this participant
         */
        private String mParticipantAddress;

        /**
         * The MetaContact that contains this participant, may be null
         */
        private MetaContact mMetaContact;

        /**
         * Create a new group chat participant UI component
         *
         * @param chatParticipant the chat participant that this UI represents
         */
        public GroupChatParticipant(ChatContact<?> chatParticipant)
        {
            super(chatParticipant.getName(), IMAGE_RESOURCE_STRING, false);
            mChatParticipant = chatParticipant;

            if (chatParticipant.getDescriptor() instanceof ChatRoomMember)
            {
                mParticipantAddress =
                    ((ChatRoomMember) chatParticipant.getDescriptor()).getContactAddressAsString();
                if (mParticipantAddress == null)
                {
                    mParticipantAddress =
                        ((ChatRoomMember) chatParticipant.getDescriptor()).getName();
                }
                else
                {
                    // Convert the user address to lower case to avoid case
                    // mis-matches between our local contact list and the
                    // server roster.
                    mParticipantAddress = mParticipantAddress.toLowerCase();
                }
            }

            addActionListener(this);

            updateComponent();
        }

        /**
         * Create a new chat participant UI component
         *
         * @param participantAddress the participant's contact address
         */
        public GroupChatParticipant(String participantAddress)
        {
            super(participantAddress, IMAGE_RESOURCE_STRING, false);
            mParticipantAddress = participantAddress;

            addActionListener(this);
            updateComponent();
        }

        /**
         * Re-initialises this UI element as we have been notified that the
         * underlying data has changed.
         */
        public void updateComponent()
        {
            ProtocolProviderService imProvider = AccountUtils.getImProvider();
            String accountId = (imProvider != null) ?
                imProvider.getAccountID().getAccountUniqueID() : "unknown";

            mMetaContact = mMetaContactListService.findMetaContactByContact(mParticipantAddress, accountId);

            if (mMetaContact != null)
            {
                // Set up this UI component to display a MetaContact
                setText(mMetaContact.getDisplayName());
                mPresenceStatus = mMetaContact.getIMContact().getPresenceStatus();
            }
            else if (mChatParticipant != null)
            {
                // Set up this UI component to display a SourceContact
                setText(mChatParticipant.getName());
            }
            else
            {
                setText(mParticipantAddress);
            }
        }

        /**
         * Update the presence status of this UI element
         */
        public void updatePresence()
        {
            if (mMetaContact != null)
            {
                Contact imContact = mMetaContact.getIMContact();

                if (imContact != null)
                {
                    mPresenceStatus = imContact.getPresenceStatus();
                }
            }
            revalidate();
            repaint();
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            BufferedImageFuture image;

            // User the icon that represent's the contact's presence if we
            // have it, otherwise use the 'unknown' icon.
            if (mPresenceStatus != null)
            {
                image = Constants.getStatusIcon(mPresenceStatus);
            }
            else
            {
                image = sImageLoaderService.getImage(
                    ImageLoaderService.USER_STATUS_UNKNOWN_ICON);
            }

            if (!isEnabled())
            {
                // The button is disabled, so add transparency to the presence
                // icon.
                ((Graphics2D)g).setComposite(AlphaComposite.getInstance(
                                                AlphaComposite.SRC_OVER, 0.6f));
            }

            g.drawImage(image.resolve(), ScaleUtils.scaleInt(4), ScaleUtils.scaleInt(5), null);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ChatRoomMember chatRoomMember = null;
            if (mChatParticipant != null)
            {
                Object descriptor = mChatParticipant.getDescriptor();
                if (descriptor instanceof ChatRoomMember)
                {
                    chatRoomMember  = (ChatRoomMember) descriptor;
                }
            }

            JPopupMenu clickMenu =
                new ReducedMetaContactRightButtonMenu(mMetaContact, chatRoomMember);
            clickMenu.setInvoker(this);

            int x = getLocationOnScreen().x + getWidth() - ScaleUtils.scaleInt(6);
            int y = getLocationOnScreen().y + getHeight() - ScaleUtils.scaleInt(6);
            clickMenu.setLocation(x, y);
            clickMenu.setVisible(true);
        }

        @Override
        public void setText(String text)
        {
            // We show an icon in the left of this button so we need a small
            // amount of padding to move the text away from the icon.
            super.setText("    " + text);
        }
    }

    /**
     * The listener responsible for updating the list of participants in the
     * chat room (if present) when a MetaContact changes
     */
    private class MainToolBarMCLListener extends MetaContactListAdapter
    {
        @Override
        public void metaContactAdded(MetaContactEvent evt)
        {
            updateChatParticipant(evt.getSourceMetaContact(), null);
        }

        @Override
        public void metaContactRenamed(MetaContactRenamedEvent evt)
        {
            updateChatParticipant(evt.getSourceMetaContact(), null);
        }

        @Override
        public void metaContactRemoved(MetaContactEvent evt)
        {
            updateChatParticipant(evt.getSourceMetaContact(), null);
        }

        @Override
        public void metaContactMoved(MetaContactMovedEvent evt)
        {
            updateChatParticipant(evt.getSourceMetaContact(), null);
        }

        @Override
        public void metaContactModified(MetaContactModifiedEvent evt)
        {
            updateChatParticipant(evt.getSourceMetaContact(), null);
        }

        @Override
        public void protoContactAdded(ProtoContactEvent evt)
        {
            if (ProtocolNames.JABBER.equals(evt.getProtoContact().getProtocolProvider()))
                updateChatParticipant(null, evt.getProtoContact().getAddress());
        }

        @Override
        public void protoContactModified(ProtoContactEvent evt)
        {
            if (ProtocolNames.JABBER.equals(evt.getProtoContact().getProtocolProvider()))
                updateChatParticipant(null, evt.getProtoContact().getAddress());
        }

        @Override
        public void protoContactRemoved(ProtoContactEvent evt)
        {
            if (ProtocolNames.JABBER.equals(evt.getProtoContact().getProtocolProvider()))
                updateChatParticipant(null, evt.getProtoContact().getAddress());
        }

        @Override
        public void protoContactMoved(ProtoContactEvent evt)
        {
            if (ProtocolNames.JABBER.equals(evt.getProtoContact().getProtocolProvider()))
                updateChatParticipant(null, evt.getProtoContact().getAddress());
        }
    }
}
