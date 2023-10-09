/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.jitsi.service.resources.ImageIconFuture;
import org.jitsi.util.OSUtils;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.main.chat.conference.ConferenceChatManager;
import net.java.sip.communicator.impl.gui.main.chat.conference.ConferenceChatSession;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.gui.ChatRoomWrapper;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.notification.NotificationData;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.IncomingFileTransferRequest;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.SubscriptionStatus;
import net.java.sip.communicator.service.protocol.OperationSetFileTransfer;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.FileTransferStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.FileTransferStatusListener;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceListener;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.AccountUtils;

/**
 * The <tt>ChatPanel</tt> is the panel, where users can write and send messages,
 * view received messages. A ChatPanel is created for a contact or for a group
 * of contacts in case of a chat conference. There is always one default contact
 * for the chat, which is the first contact which was added to the chat.
 * Each ChatPanel corresponds to a ChatWindow.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
@SuppressWarnings("serial")
public class ChatPanel
    extends TransparentPanel
    implements  ChatSessionRenderer,
                Chat,
                ChatConversationContainer,
                LocalUserChatRoomPresenceListener,
                FileTransferStatusListener,
                RegistrationStateChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPanel</tt> class and its
     * instances for logging output.
     */
    private static final Logger sLog = Logger.getLogger(ChatPanel.class);

    private final JSplitPane mMessagePane
        = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private final JPanel mTopPanel = new JPanel(new BorderLayout());

    private final ChatConversationPanel mConversationPanel;

    /**
     * The panel that the user types in to send messages in this chat panel.
     */
    private final ChatWritePanel mWriteMessagePanel;

    /**
     * The panel that will replace the mWriteMessagePanel if we lose contact to
     * the chat room represented by this chat panel.
     */
    private LostContactGroupChatPanel mLostContactGroupChatPanel;

    /**
     * The panel that will replace the mWriteMessagePanel if we have
     * permanently left the chat room represented by this chat panel.
     */
    private DormantGroupChatPanel mDormantGroupChatPanel;

    private final ChatContainer mChatContainer;

    /**
     * The minimum height of the panel that the user types in to send
     * messages in this chat panel.
     */
    private static final int MIN_MESSAGE_PANEL_HEIGHT = ScaleUtils.scaleInt(28);

    /**
     * The default height of the panel that the user types in to send
     * messages in this chat panel.
     */
    private static final int DEFAULT_MESSAGE_PANEL_HEIGHT = ScaleUtils.scaleInt(72);

    /**
     * The ChatTransport used for the most recently received message in this
     * panel.
     */
    private ChatTransport mLatestChatTransport;

    /**
     * Component that contains any notification - either typing or offline warning
     */
    private final JLabel mNotificationArea;

    /**
     * The typing notification icon.
     */
    private final ImageIconFuture mTypingIcon = GuiActivator.getResources()
        .getImage("service.gui.icons.TYPING");

    private boolean mIsShown = false;

    /**
     * The chat session.
     */
    private ChatSession mChatSession;

    /**
     * The IM provider
     */
    private ProtocolProviderService mImProvider;

    /**
     * Stores all active  file transfer requests and effective transfers with
     * the identifier of the transfer.
     */
    private final Hashtable<String, Object> mActiveFileTransfers
        = new Hashtable<>();

    /**
     * The ID of the last sent message in this chat.
     */
    private String mLastSentMessageUID = null;

    private AccessibleChatBuffer mAccessibleBuffer;

    /**
     * Whether the user has previously left this chat room
     */
    private boolean mHasLeft = false;

    /**
     * Whether the chat panel has become disconnected from the server without
     * the user leaving the group chat (e.g. the user has signed out of chat
     * but left the chat window open).
     */
    private boolean mIsDisconnected = false;

    /**
     * Enum representing the different warnings that may be displayed in the
     * notification area if the contact we are talking to in this chat panel
     * is offline.
     */
    public enum OfflineWarning
    {
        /**
         * Warning message to display in the chat panel if the contact is
         * offline and can receive no more offline messages, as their offline
         * message queue is full.
         */
        OFFLINE_QUEUE_FULL_WARNING(
            GuiActivator.getResources().getI18NString("service.gui.MSG_OFFLINE_FULL"),
            Color.BLACK),

        /**
         * Warning message to display in the chat panel if the contact is
         * offline and does not support receiving offline messages.
         */
        OFFLINE_QUEUE_UNSUPPORTED_WARNING(
            GuiActivator.getResources().getI18NString("service.gui.MSG_OFFLINE_UNSUPPORTED"),
            Color.BLACK),

        /**
         * Default warning message to display in the chat panel if the contact
         * is offline.
         */
        DEFAULT_OFFLINE_WARNING(
            GuiActivator.getResources().getI18NString("service.gui.MSG_OFFLINE"),
            Color.GRAY);

        /**
         * The string to display as the offline warning text.
         */
        private String mMessage;

        /**
         * The color to use to for the offline warning text.
         */
        private Color mTextColor;

        OfflineWarning(String message, Color textColor)
        {
            mMessage = message;
            mTextColor = textColor;
        }

        /**
         * @return the string to display as the offline warning text.
         */
        public String getMessage()
        {
            return mMessage;
        }

        /**
         * @return the color to use to for the offline warning text.
         */
        public Color getTextColor()
        {
            return mTextColor;
        }
    }

    /**
     * Creates a <tt>ChatPanel</tt> which is added to the given chat window.
     *
     * @param chatContainer The parent window of this chat panel.
     */
    public ChatPanel(ChatContainer chatContainer)
    {
        super(new BorderLayout());

        mChatContainer = chatContainer;
        mConversationPanel = new ChatConversationPanel(this);
        mConversationPanel.setPreferredSize(new Dimension(400, 200));
        mConversationPanel.getChatTextPane()
            .setTransferHandler(new ChatTransferHandler(this));

        mConversationPanel.getVerticalScrollBar().setUnitIncrement(10);

        mTopPanel.setBackground(Color.WHITE);
        mTopPanel.setBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY));
        mNotificationArea = createNotificationArea();
        mTopPanel.add(mNotificationArea, BorderLayout.SOUTH);

        mWriteMessagePanel = new ChatWritePanel(this);

        mAccessibleBuffer = new AccessibleChatBuffer(mWriteMessagePanel);

        mMessagePane.setBorder(null);
        mMessagePane.setOpaque(false);
        mMessagePane.addPropertyChangeListener(
            new DividerLocationListener());

        mMessagePane.setDividerSize(3);
        mMessagePane.setResizeWeight(1.0D);
        setBottomPanel(mWriteMessagePanel);

        mMessagePane.setTopComponent(mTopPanel);

        add(mMessagePane, BorderLayout.CENTER);

        if (OSUtils.IS_MAC)
        {
            setOpaque(true);
            setBackground(
                new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAC_PANEL_BACKGROUND")));
        }

        addComponentListener(new TabSelectionComponentListener());
    }

    /**
     * Sets the panel that will appear at the bottom of this chat panel to be
     * the given panel and sizes it correctly.
     *
     * @param bottomPanel The panel to appear at the bottom of this chat panel
     */
    private void setBottomPanel(TransparentPanel bottomPanel)
    {
        sLog.debug("Setting bottom panel to " + bottomPanel);

        int preferredMessagePanelHeight = DEFAULT_MESSAGE_PANEL_HEIGHT;

        // The dormant chat panel displayed when we have left the group chat
        // must be at least default height.
        int minimumMessagePanelHeight = mHasLeft ?
            DEFAULT_MESSAGE_PANEL_HEIGHT : MIN_MESSAGE_PANEL_HEIGHT;
        sLog.debug("Setting minimum chat area to " + minimumMessagePanelHeight);

        // If the user has resized the chat write area to a custom value,
        // that value will be saved in config.  Therefore, we use the custom
        // size if we have one, unless we have left the chat room and the
        // custom size is smaller than the default value, as we need the write
        // area to be at least the default size to display the dormant group
        // chat panel correctly.
        int chatAreaSize = ConfigurationUtils.getChatWriteAreaSize();
        if (chatAreaSize > 0)
        {
            if (chatAreaSize > DEFAULT_MESSAGE_PANEL_HEIGHT || !mHasLeft)
            {
                sLog.debug("Using custom chat area size " + chatAreaSize);
                preferredMessagePanelHeight = chatAreaSize;
            }
            else
            {
                sLog.debug(
                    "Using default chat area size " + preferredMessagePanelHeight);
            }
        }

        Dimension writeMessagePanelDefaultSize =
            new Dimension(500, preferredMessagePanelHeight);
        Dimension writeMessagePanelMinSize = new Dimension(500, minimumMessagePanelHeight);
        Dimension writeMessagePanelMaxSize = new Dimension(500, 100);

        bottomPanel.setMinimumSize(writeMessagePanelMinSize);
        bottomPanel.setMaximumSize(writeMessagePanelMaxSize);
        bottomPanel.setPreferredSize(writeMessagePanelDefaultSize);

        mMessagePane.setBottomComponent(bottomPanel);
    }

    /**
     * Sets the chat session to associate to this chat panel.
     * @param chatSession the chat session to associate to this chat panel
     */
    public void setChatSession(ChatSession chatSession)
    {
        ChatSession previousChatSession = mChatSession;
        mChatSession = chatSession;
        mImProvider = AccountUtils.getImProvider();

        mTopPanel.add(mConversationPanel, BorderLayout.CENTER);

        if (mImProvider != null)
        {
            sLog.debug(
                "Adding registration state change listener to chat panel for <" +
                                             mChatSession.getDescriptor() + ">");
            // Listen for registration state changes so we can update the UI
            // if the user loses connection.
            mImProvider.addRegistrationStateChangeListener(this);
        }

        // Make sure the UI is displaying the correct connected state.
        refreshUIConnectedState();

        if (mChatSession instanceof MetaContactChatSession ||
            mChatSession instanceof SMSChatSession)
        {
            mWriteMessagePanel.setTransportSelectorBoxVisible(true);

            // Show or hide the offline warning depending on the status of the
            // contact we are chatting to
            updateOfflineWarning(OfflineWarning.DEFAULT_OFFLINE_WARNING);

            //Enables to change the protocol provider by simply pressing the
            // CTRL-P key combination
            ActionMap amap = getActionMap();

            amap.put("ChangeProtocol", new ChangeTransportAction());

            InputMap imap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

            imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                KeyEvent.CTRL_DOWN_MASK), "ChangeProtocol");

            mChatContainer.updateContainer(this);
        }
        else if (mChatSession instanceof ConferenceChatSession)
        {
            if (!(previousChatSession instanceof ConferenceChatSession))
            {
                // We have just uplifted a chat session to a conference, so we
                // should clear the conversation panel
                sLog.debug("Clearing the conversation panel as conversation uplifted to conference");
                mConversationPanel.clear();
                mConversationPanel.setDefaultContent();
            }

            ConferenceChatSession confSession
                = (ConferenceChatSession) mChatSession;

            mWriteMessagePanel.setTransportSelectorBoxVisible(false);

            // Set the title of this window to be the chat room subject
            mChatContainer.setTitle(confSession.getChatSubject());

            mChatContainer.updateContainer(this);

            if (mImProvider != null)
            {
                OperationSetMultiUserChat opSetMuc =
                    mImProvider.getOperationSet(OperationSetMultiUserChat.class);

                if (opSetMuc != null)
                {
                    // Listen for the user leaving this chat room so we can
                    // clear any new message notifications in that case.
                    opSetMuc.addPresenceListener(this);
                }
            }
        }
    }

    /**
     * Either show or hide the offline warning depending on the status of the
     * participants of the current chat session.
     *
     * @param warningMessage The warning message to display if there are
     * offline contacts in the current chat session.
     */
    public void updateOfflineWarning(OfflineWarning warningMessage)
    {
        // For each contact we are chatting with, check to see if any are
        // offline, if so we show the offline warning.  We default the text to
        // display as the offline warning to an empty string, rather than
        // simply hiding the notification area when all contacts are online.
        // This is to prevent the UI elements from jumping around whenever the
        // offline warning is added and removed.
        String textToDisplay = "";
        Color textColor = Color.GRAY;

        // The chat session might be null if we've lost connection to the chat
        // server/signed out of chat.  If so, we don't show the offline
        // warning, as we are offline and we can't check the status of the
        // contact.
        if (mChatSession != null && mImProvider != null && mImProvider.isRegistered())
        {
            for (ChatContact<?> chatContact : mChatSession.getParticipants())
            {
                Object descriptor = chatContact.getDescriptor();

                if (descriptor instanceof MetaContact)
                {
                    MetaContact contact = (MetaContact)descriptor;
                    Contact imContact = contact.getIMContact();

                    if (imContact == null)
                        continue;

                    // Get the authOpSet so that we can check the subscription
                    // state
                    OperationSetExtendedAuthorizations authOpSet =
                        imContact.getProtocolProvider().getOperationSet(
                                      OperationSetExtendedAuthorizations.class);

                    SubscriptionStatus status =
                        authOpSet.getSubscriptionStatus(imContact);

                    if (!imContact.getPresenceStatus().isOnline() ||
                        (status != null && status != SubscriptionStatus.Subscribed))
                    {
                        // The contact is offline, or we are not subscribed to
                        // this contact - either way we should show the
                        // warning, so set the text to display to the requested
                        // warning message.
                        textToDisplay = warningMessage.getMessage();
                        textColor = warningMessage.getTextColor();
                        sLog.debug("Setting offline warning to '" +
                                   textToDisplay + "' for " + imContact);
                        break;
                    }
                }
            }
        }
        else
        {
            sLog.debug(
                    "Not showing contact offline warning - chat session offline <" +
                    (mChatSession != null ? mChatSession.getDescriptor() : "null chat session") + ">");
        }

        mNotificationArea.setText(textToDisplay);
        mNotificationArea.setForeground(textColor);
    }

    /**
     * Returns the chat session associated with this chat panel.
     * @return the chat session associated with this chat panel
     */
    public ChatSession getChatSession()
    {
        return mChatSession;
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        sLog.debug(
            "Disposing chat panel for <" + mChatSession.getDescriptor() + ">");

        if (mLostContactGroupChatPanel != null)
        {
            mLostContactGroupChatPanel.dispose();
        }

        if (mDormantGroupChatPanel != null)
        {
            mDormantGroupChatPanel.dispose();
        }

        mWriteMessagePanel.dispose();
        mChatSession.dispose();
        mConversationPanel.dispose();

        if (mImProvider != null)
        {
            sLog.debug(
                "Removing registration state change listener from chat panel for <" +
                                                 mChatSession.getDescriptor() + ">");

            mImProvider.removeRegistrationStateChangeListener(this);

            OperationSetMultiUserChat opSetMuc =
                mImProvider.getOperationSet(OperationSetMultiUserChat.class);

            if (opSetMuc != null)
            {
                opSetMuc.removePresenceListener(this);
            }
        }
    }

    /**
     * Returns the chat window, where this chat panel is added.
     *
     * @return the chat window, where this chat panel is added
     */
    public ChatContainer getChatContainer()
    {
        return mChatContainer;
    }

    /**
     * Returns the chat window, where this chat panel
     * is located. Implements the
     * <tt>ChatConversationContainer.getConversationContainerWindow()</tt>
     * method.
     *
     * @return ChatWindow The chat window, where this
     * chat panel is located.
     */
    @Override
    public Window getConversationContainerWindow()
    {
        return mChatContainer.getFrame();
    }

    /**
     * Adds a typing notification message to the conversation panel.
     *
     * @param typingNotification the typing notification to show
     */
    @Override
    public void addTypingNotification(final String typingNotification)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    addTypingNotification(typingNotification);
                }
            });
            return;
        }

        mNotificationArea.setText(typingNotification);

        if (typingNotification != null && !typingNotification.equals(" "))
        {
            mTypingIcon.addToLabel(mNotificationArea);
        }
        else
        {
            mNotificationArea.setIcon(null);
        }

        revalidate();
        repaint();
    }

    /**
     * Adds a typing notification message to the conversation panel,
     * saying that typin notifications has not been delivered.
     *
     * @param typingNotification the typing notification to show
     */
    public void addErrorSendingTypingNotification(
                    final String typingNotification)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    addErrorSendingTypingNotification(typingNotification);
                }
            });
            return;
        }

        mNotificationArea.setText(typingNotification);

        if (typingNotification != null && !typingNotification.equals(" "))
        {
            mTypingIcon.addToLabel(mNotificationArea);
        }
        else
            mNotificationArea.setIcon(null);

        revalidate();
        repaint();
    }

    /**
     * Removes the typing notification message from the conversation panel.
     */
    public void removeTypingNotification()
    {
        addTypingNotification(" ");
    }

    /**
     * Creates and returns the component to be used as the notification area
     *
     * @return the notification area
     */
    private JLabel createNotificationArea()
    {
        JLabel notificationArea = new JLabel(" ", SwingConstants.CENTER);

        notificationArea.setPreferredSize(new Dimension(500, 20));
        notificationArea.setForeground(Color.GRAY);
        notificationArea.setFont(notificationArea.getFont().deriveFont(
            ScaleUtils.getScaledFontSize(11f)));
        notificationArea.setVerticalTextPosition(JLabel.BOTTOM);
        notificationArea.setHorizontalTextPosition(JLabel.LEFT);
        notificationArea.setIconTextGap(0);

        return notificationArea;
    }

    /**
     * Returns the conversation panel, contained in this chat panel.
     *
     * @return the conversation panel, contained in this chat panel
     */
    public ChatConversationPanel getChatConversationPanel()
    {
        return mConversationPanel;
    }

    /**
     * Returns the write area panel, contained in this chat panel.
     *
     * @return the write area panel, contained in this chat panel
     */
    public ChatWritePanel getChatWritePanel()
    {
        return mWriteMessagePanel;
    }

    /**
     * Every time the chat panel is shown we set it as a current chat panel.
     * This is done here and not in the Tab selection listener, because the tab
     * change event is not fired when the user clicks on the close tab button
     * for example.
     */
    private class TabSelectionComponentListener
        extends ComponentAdapter
    {
        @Override
        public void componentShown(ComponentEvent evt)
        {
            Component component = evt.getComponent();
            Container parent = component.getParent();

            if (!(parent instanceof JTabbedPane))
                return;

            JTabbedPane tabbedPane = (JTabbedPane) parent;

            if (tabbedPane.getSelectedComponent() != component)
                return;

            mChatContainer.setCurrentChat(ChatPanel.this);
        }
    }

    /**
     * Requests the focus in the write message area.
     */
    public void requestFocusInWriteArea()
    {
        getChatWritePanel().getEditorPane().requestFocus();
    }

    /**
     * Checks if the editor contains text.
     *
     * @return TRUE if editor contains text, FALSE otherwise.
     */
    public boolean isWriteAreaEmpty()
    {
        Document doc = getChatWritePanel().getEditorPane().getDocument();

        try
        {
            String text = doc.getText(0, doc.getLength());

            if (text == null || text.equals(""))
                return true;
        }
        catch (BadLocationException e)
        {
            sLog.error("Failed to obtain document text.", e);
        }

        return false;
    }

    /**
     * Cuts the write area selected content to the clipboard.
     */
    public void cut()
    {
        mWriteMessagePanel.getEditorPane().cut();
    }

    /**
     * Copies either the selected write area content or the selected
     * conversation panel content to the clipboard.
     */
    public void copy()
    {
        JTextComponent textPane = mConversationPanel.getChatTextPane();

        if (textPane.getSelectedText() == null)
            textPane = mWriteMessagePanel.getEditorPane();

        textPane.copy();
    }

    /**
     * Copies the selected write panel content to the clipboard.
     */
    public void copyWriteArea()
    {
        mWriteMessagePanel.getEditorPane().copy();
    }

    /**
     * Pastes the content of the clipboard to the write area.
     */
    public void paste()
    {
        JEditorPane editorPane = mWriteMessagePanel.getEditorPane();

        editorPane.paste();
        try
        {
            mWriteMessagePanel.refreshCurrentText();
        }
        catch (BadLocationException e)
        {
            sLog.error("Failed to refresh current text after paste action", e);
        }
        editorPane.requestFocus();
    }

    /**
     * Sends current write area content.
     */
    public void sendButtonDoClick()
    {
//        never called, send button is hidden.
//        if (!isWriteAreaEmpty())
//        {
//            new Thread("ChatPanel sendButtonDoClick")
//            {
//                @Override
//                public void run()
//                {
//                    sendMessage();
//                }
//            }.start();
//        }
//
//        //make sure the focus goes back to the write area
//        requestFocusInWriteArea();
    }

    /**
     * Returns TRUE if this chat panel is added to a container (window)
     * which is shown on the screen, FALSE - otherwise.
     *
     * @return TRUE if this chat panel is added to a container (window)
     * which is shown on the screen, FALSE - otherwise
     */
    public boolean isShown()
    {
        return mIsShown;
    }

    /**
     * Marks this chat panel as shown or hidden.
     *
     * @param isShown TRUE to mark this chat panel as shown, FALSE - otherwise
     */
    public void setShown(boolean isShown)
    {
        mIsShown = isShown;
    }

    /**
     * Brings the <tt>ChatWindow</tt> containing this <tt>ChatPanel</tt> to the
     * front.
     */
    @Override
    public void setChatVisible()
    {
        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        chatWindowManager.openChatAndAlertWindow(this, true);
    }

    /**
     * Implements the <tt>Chat.isChatFocused</tt> method. Returns TRUE if this
     * chat panel is the currently selected panel and if the chat window, where
     * it's contained is active.
     *
     * @return true if this chat panel has the focus and false otherwise.
     */
    @Override
    public boolean isChatFocused()
    {
        ChatPanel currentChatPanel = mChatContainer.getCurrentChat();

        return (currentChatPanel != null
                && currentChatPanel.equals(this)
                && mChatContainer.getFrame().isActive());
    }

    /**
     * Implements the <tt>Chat.isChatInUse</tt> method. Returns TRUE if this
     * chat panel is in use (i.e. contains an unsent message, an in-progress
     * file transfer or has received a new message in the last 2 seconds).
     *
     * @return true if this chat panel is in use and false otherwise.
     */
    @Override
    public boolean isChatInUse()
    {
        long lastMsgTime =
            getChatConversationPanel().getLastIncomingMsgTimestamp().getTime();

        return (!isWriteAreaEmpty() || containsActiveFileTransfers()
            || (System.currentTimeMillis() - lastMsgTime) < 2000);
    }

    /**
     * Returns the message written by user in the chat write area.
     *
     * @return the message written by user in the chat write area
     */
    @Override
    public String getMessage()
    {
        Document writeEditorDoc
            = mWriteMessagePanel.getEditorPane().getDocument();

        try
        {
            return writeEditorDoc.getText(0, writeEditorDoc.getLength());
        }
        catch (BadLocationException e)
        {
            return mWriteMessagePanel.getEditorPane().getText();
        }
    }

    /**
     * Sets the given message as a message in the chat write area, and
     * refreshes the text to allow correct emoji display
     *
     * @param message the text that would be set to the chat write area
     */
    @Override
    public void setMessage(String message)
    {
        mWriteMessagePanel.getEditorPane().setText(message);
        try
        {
            mWriteMessagePanel.refreshText(message, false);
        }
        catch (BadLocationException e)
        {
            sLog.error("Failed to refresh editor text.", e);
        }
    }

    /**
     * Changes the "Send as SMS" check box state.
     *
     * @param isSmsSelected <code>true</code> to set the "Send as SMS" check box
     * selected, <code>false</code> - otherwise.
     */
    public void setSmsSelected(boolean isSmsSelected)
    {
        mWriteMessagePanel.setSmsSelected(isSmsSelected);
    }

    /**
     * The <tt>ChangeProtocolAction</tt> is an <tt>AbstractAction</tt> that
     * opens the menu, containing all available protocol contacts.
     */
    private class ChangeTransportAction
        extends AbstractAction
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            mWriteMessagePanel.openChatTransportSelectorBox();
        }
    }

    /**
     * Renames all occurrences of the given <tt>chatContact</tt> in this chat
     * panel.
     *
     * @param chatContact the contact to rename
     * @param name the new name
     */
    @Override
    public void setContactName(ChatContact<?> chatContact, String name)
    {
        if (getChatSession() instanceof MetaContactChatSession)
        {
            // We're in a chat with a single MetaContact. Therefore the title is
            // just that contact's name, and we should update it.
            if (mChatContainer.getCurrentChat() == this)
            {
                mChatContainer.setTitle(name);
            }
        }
    }

    /**
     * Adds the given chatTransport to the given send via selector box.
     *
     * @param chatTransport the transport to add
     */
    @Override
    public void addChatTransport(ChatTransport chatTransport)
    {
        mWriteMessagePanel.addChatTransport(chatTransport);
    }

    /**
     * Removes the given chat status state from the send via selector box.
     *
     * @param chatTransport the transport to remove
     */
    @Override
    public void removeChatTransport(ChatTransport chatTransport)
    {
        mWriteMessagePanel.removeChatTransport(chatTransport);
    }

    /**
     * Selects the given chat transport in the send via box.
     *
     * @param chatTransport the chat transport to be selected
     */
    @Override
    public void setSelectedChatTransport(ChatTransport chatTransport)
    {
        mWriteMessagePanel.setSelectedChatTransport(chatTransport);
    }

    /**
     * Sets the visibility of the notification area based on the chat transport
     * that has been selected.  Notifications aren't displayed for SMS
     * transports.
     *
     * @param chatTransport the chat transport that has just been set
     */
    public void setNotificationAreaVisibility(ChatTransport chatTransport)
    {
        mNotificationArea.setVisible(!(chatTransport instanceof SMSChatTransport));
    }

    /**
     * Updates the status of the given chat transport in the send via selector
     * box and notifies the user for the status change.
     * @param chatTransport the <tt>chatTransport</tt> to update
     */
    @Override
    public void updateChatTransportStatus(final ChatTransport chatTransport)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    updateChatTransportStatus(chatTransport);
                }
            });
            return;
        }

        mWriteMessagePanel.updateChatTransportStatus(chatTransport);

        // Status has changed so update the offline warning too
        updateOfflineWarning(OfflineWarning.DEFAULT_OFFLINE_WARNING);
    }

    /**
     * Checks whether the current chat transport is different from the last
     * specific transport that was used to send a message in this panel.  If
     * so, it resets the current chat transport to send to all registered
     * resources for the session.
     */
    public void checkChatTransport()
    {
        ChatTransport currentChatTransport =
            mChatSession.getCurrentChatTransport();

        // SMSChatTransports don't have separate resources so there's nothing
        // to do here.
        if (currentChatTransport instanceof SMSChatTransport)
            return;

        String currentChatTransportName =
            (currentChatTransport == null) ?
                null : currentChatTransport.getResourceName();

        String latestChatTransportName =
            (mLatestChatTransport == null) ?
                null : mLatestChatTransport.getResourceName();

        sLog.debug("Current chat transport = " + currentChatTransportName +
                     ", Latest chat transport = " + latestChatTransportName);

        // We need to reset the current chat transport to send to all
        // registered resources for the session only if the current chat
        // transport is different from the latest one that we received a
        // message on.
        // This doesn't apply if the latest transport is null, as that means
        // this is the first message in this panel.
        if ((mLatestChatTransport != null) &&
            !mLatestChatTransport.equals(currentChatTransport))
        {
            sLog.debug("Resetting chat transport due to resource change");
            resetChatTransport();
        }

        // We've finished checking, so update the mLatestChatTransport.
        if (currentChatTransportName != null)
        {
            sLog.debug(
                "Updating mLatestChatTransport to " + currentChatTransportName);
            mLatestChatTransport = currentChatTransport;
        }
    }

    @Override
    public void resetChatTransport()
    {
        MetaContactChatTransport metaContactChatTransport = null;
        Iterator<ChatTransport> chatTransports = mChatSession.getChatTransports();

        // We need to set the chat transport back to the default IM transport.
        // If there is more than one IM transport, there will be one
        // MetaContactChatTransport with a null resource (that supports IM) and
        // this is the one we need. If there is only one IM transport there
        // will just be a single MetaContactChatTransport that supports IM
        // (and with a non-null resource) and that is the one we need.
        while (chatTransports.hasNext())
        {
            ChatTransport transport = chatTransports.next();

            if (transport instanceof MetaContactChatTransport &&
                transport.allowsInstantMessage())
            {
                // Store the MetaContactChatTransport in case we only find one.
                metaContactChatTransport = (MetaContactChatTransport) transport;

                if (transport.getResourceName() == null)
                {
                    mChatSession.setCurrentChatTransport(transport);
                    setSelectedChatTransport(transport);
                    return;
                }
            }
        }

        // We haven't found a MetaContactChatTransport with null resource, so
        // use the only one that we did find.
        if (metaContactChatTransport != null)
        {
            mChatSession.setCurrentChatTransport(metaContactChatTransport);
            setSelectedChatTransport(metaContactChatTransport);
        }
        else
        {
            sLog.warn("Unable to reset chat transport - " +
                                                "no suitable transports found");
        }
    }

    /**
     * Adds the given <tt>chatContact</tt> to the list of chat contacts
     * participating in the corresponding to this chat panel chat.
     * @param chatContact the contact to add
     */
    @Override
    public void addChatContact(ChatContact<?> chatContact)
    {
        // If the user has left this chat room then this means we have been
        // re-invited to this room. Therefore we need to remove the dormant UI
        // from this panel and set it up as active.
        if (mHasLeft)
        {
            setLeftChatRoomUI(false);
        }

        // We just call updateContainer as this adds and removes contacts from
        // the UI based on the current active participants list in the chat
        // session.
        getChatContainer().updateContainer(this);
    }

    /**
     * Removes the given <tt>chatContact</tt> from the list of chat contacts
     * participating in the corresponding to this chat panel chat.
     * @param chatContact the contact to remove
     */
    @Override
    public void removeChatContact(ChatContact<?> chatContact)
    {
        // We just call updateContainer as this adds and removes contacts from
        // the UI based on the current active participants list in the chat
        // session.
        getChatContainer().updateContainer(this);
    }

    /**
     * Removes all chat contacts from the contact list of the chat.
     */
    @Override
    public void removeAllChatContacts()
    {
        // We just call updateContainer as this adds and removes contacts from
        // the UI based on the current active participants list in the chat
        // session.
        getChatContainer().updateContainer(this);
    }

    @Override
    public void refreshAllChatContacts()
    {
        // We just call updateContainer as this adds and removes contacts from
        // the UI based on the current active participants list in the chat
        // session. Though, first clear all active participants, as the chat
        // room that they reference may have changed (e.g. if we lost and
        // regained connection to the server) and this ensures that the new
        // participants definitely reference the latest objects.
        ChatWindow chatWindow = (ChatWindow)getChatContainer();
        chatWindow.getChatToolbar().clearAllParticipants();
        chatWindow.updateContainer(this);
    }

    /**
     * Sets the given <tt>subject</tt> to this chat.
     * @param subject the subject to set
     */
    @Override
    public void setChatSubject(String subject)
    {
        getChatContainer().setTitle(subject);
    }

    /**
     * Adds the given <tt>IncomingFileTransferRequest</tt> to the conversation
     * panel in order to notify the user of the incoming file.
     *
     * @param fileTransferOpSet the file transfer operation set
     * @param request the request to display in the conversation panel
     * @param date the date on which the request has been received
     */
    public void addIncomingFileTransferRequest(
        final OperationSetFileTransfer fileTransferOpSet,
        final IncomingFileTransferRequest request,
        final Date date)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    addIncomingFileTransferRequest(
                        fileTransferOpSet, request, date);
                }
            });
            return;
        }

        addActiveFileTransfer(request.getID(), request);
    }

    /**
     * Returns the first chat transport for the current chat session that
     * supports file transfer.
     *
     * @return the first chat transport for the current chat session that
     * supports file transfer.
     */
    public ChatTransport findFileTransferChatTransport()
    {
        // We currently don't support file transfer in group chats.
        if ((mChatSession == null) || (mChatSession instanceof ConferenceChatSession))
            return null;

        ChatTransport currentChatTransport
            = mChatSession.getCurrentChatTransport();

        if (currentChatTransport == null)
            return null;

        if (currentChatTransport.getProtocolProvider()
                .getOperationSet(OperationSetFileTransfer.class) != null)
        {
            return currentChatTransport;
        }
        else
        {
            Iterator<ChatTransport> chatTransportsIter
                = mChatSession.getChatTransports();

            while (chatTransportsIter.hasNext())
            {
                ChatTransport chatTransport = chatTransportsIter.next();

                Object fileTransferOpSet
                    = chatTransport.getProtocolProvider()
                        .getOperationSet(OperationSetFileTransfer.class);

                if (fileTransferOpSet != null)
                    return chatTransport;
            }
        }

        return null;
    }

    /**
     * Returns the first chat transport for the current chat session that
     * supports group chat.
     *
     * @return the first chat transport for the current chat session that
     * supports group chat.
     */
    public ChatTransport findInviteChatTransport()
    {
        if (mChatSession != null)
        {
            ChatTransport currentChatTransport
                = mChatSession.getCurrentChatTransport();

            if (currentChatTransport != null)
            {
                ProtocolProviderService protocolProvider
                    = currentChatTransport.getProtocolProvider();

                // We choose between OpSets for multi user chat...
                if (protocolProvider.getOperationSet(
                    OperationSetMultiUserChat.class) != null)
                {
                    return mChatSession.getCurrentChatTransport();
                }
            }

            Iterator<ChatTransport> chatTransportsIter
                = mChatSession.getChatTransports();

            while (chatTransportsIter.hasNext())
            {
                ChatTransport chatTransport = chatTransportsIter.next();

                Object groupChatOpSet
                    = chatTransport.getProtocolProvider()
                        .getOperationSet(OperationSetMultiUserChat.class);

                if (groupChatOpSet != null)
                    return chatTransport;
            }
        }

        return null;
    }

    /**
     * Invites the given <tt>chatContacts</tt> to this chat.
     *
     * @param inviteChatTransport the chat transport to use to send the invite
     * @param chatContacts the contacts to invite
     * @param reason the reason of the invitation
     * @param createChatRoom whether to create a new chat room for these contacts
     * @param chatRoomSubject the subject of the chat room
     */
    public void inviteContacts(ChatTransport inviteChatTransport,
                               Collection<String> chatContacts,
                               String reason,
                               boolean createChatRoom,
                               String chatRoomSubject)
    {
        ChatSession conferenceChatSession = null;

        HashSet<String> addChatContacts = new HashSet<>(chatContacts);

        // If this is either a non-group chat session (so we're uplifting a
        // one-to-one chat) or we've been explicitly asked to create a new
        // chat room, we need to create the chat room before we can invite
        // contacts.
        if (mChatSession instanceof MetaContactChatSession || createChatRoom)
        {
            // Make sure we don't invide the user's SMS address to a group chat.
            if (!(inviteChatTransport instanceof SMSChatTransport))
            {
                addChatContacts.add(inviteChatTransport.getName());
            }

            ConferenceChatManager conferenceChatManager
                = GuiActivator.getUIService().getConferenceChatManager();

            // the chat session is set regarding to which OpSet is used for MUC
            if (inviteChatTransport.getProtocolProvider().
                    getOperationSet(OperationSetMultiUserChat.class) != null)
            {
                ChatRoomWrapper chatRoomWrapper
                    = conferenceChatManager.createChatRoom(
                        inviteChatTransport.getProtocolProvider(),
                        addChatContacts,
                        reason);

                if (chatRoomWrapper == null)
                {
                    sLog.error("Failed to create chat room");
                    return;
                }

                ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
                if (chatRoom == null)
                {
                    sLog.error("Failed to find chat room");
                    return;
                }

                // Now the chat room has been created, we can set its initial
                // subject.
                try
                {
                    // Set the subject on the server so it will be passed on
                    // to the other participants.
                    chatRoom.setSubject(chatRoomSubject);
                }
                catch (OperationFailedException ex)
                {
                    sLog.error("Failed to set chat room name, " + chatRoomSubject);
                }

                // Also save the subject in config so that we have a record of
                // the latest subject, as the server sometimes returns null
                // when asked for the subject, even when one has been set.
                ConfigurationUtils.updateChatRoomProperty(
                   chatRoomWrapper.getParentProvider().getProtocolProvider(),
                   chatRoomWrapper.getChatRoomID(), "chatRoomSubject", chatRoomSubject);

                conferenceChatSession
                    = new ConferenceChatSession(this, chatRoomWrapper);
            }

            if (conferenceChatSession != null)
            {
                setChatSession(conferenceChatSession);

                // Hide the notification area, as it isn't used in a group chat
                // and also to ensure we clear any old typing notifications
                // leftover from the one-to-one chat.
                mNotificationArea.setVisible(false);
            }
        }
        // We're already in a conference chat, go ahead and invite the contacts
        else if (mChatSession instanceof ConferenceChatSession)
        {
            conferenceChatSession = mChatSession;
            try
            {
                ((ConferenceChatSession) conferenceChatSession).setChatSubject(chatRoomSubject);
            }
            catch (OperationFailedException ex)
            {
                sLog.error("Failed to set chat room name, " + chatRoomSubject);
            }

            List<ChatContact<?>> participants =
                ((ConferenceChatSession) conferenceChatSession).getParticipants();

            // Don't add contacts if they are already in the chat room
            for (ChatContact<?> participant : participants)
            {
                String participantAddress = ((ChatRoomMember)participant.getDescriptor()).
                                                            getContactAddressAsString();
                if (addChatContacts.contains(participantAddress))
                {
                    addChatContacts.remove(participantAddress);
                    sLog.info("Not adding " + sanitiseChatAddress(participantAddress) +
                              " as they are already in the chat room");
                }
            }

            // Add the contacts
            for (String contactAddress : addChatContacts)
            {
                conferenceChatSession.getCurrentChatTransport()
                    .inviteChatContact(contactAddress, reason);
            }
        }
    }

    /**
     * Handles file transfer status changed in order to remove completed file
     * transfers from the list of active transfers.
     * @param event the file transfer status change event the notified us for
     * the change
     */
    @Override
    public void statusChanged(FileTransferStatusChangeEvent event)
    {
        FileTransfer fileTransfer = event.getFileTransfer();

        int newStatus = event.getNewStatus();

        if (newStatus == FileTransferStatusChangeEvent.COMPLETED
            || newStatus == FileTransferStatusChangeEvent.CANCELED
            || newStatus == FileTransferStatusChangeEvent.FAILED
            || newStatus == FileTransferStatusChangeEvent.REFUSED
            || newStatus == FileTransferStatusChangeEvent.NO_RESPONSE)
        {
            removeActiveFileTransfer(fileTransfer.getID());
            fileTransfer.removeStatusListener(this);
        }
    }

    /**
     * Returns <code>true</code> if there are active file transfers, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if there are active file transfers, otherwise
     * returns <code>false</code>
     */
    public boolean containsActiveFileTransfers()
    {
        return !mActiveFileTransfers.isEmpty();
    }

    /**
     * Cancels all active file transfers.
     */
    public void cancelActiveFileTransfers()
    {
        Enumeration<String> activeKeys = mActiveFileTransfers.keys();

        while (activeKeys.hasMoreElements())
        {
            // catchall so if anything happens we still
            // will close the chat/window
            try
            {
                String key = activeKeys.nextElement();
                Object descriptor = mActiveFileTransfers.get(key);

                if (descriptor instanceof IncomingFileTransferRequest)
                {
                    ((IncomingFileTransferRequest) descriptor).rejectFile(false);
                }
                else if (descriptor instanceof FileTransfer)
                {
                    ((FileTransfer) descriptor).cancel();
                }
            }
            catch(Throwable t)
            {
                sLog.error("Cannot cancel file transfer.", t);
            }
        }
    }

    /**
     * Stores the current divider position.
     */
    private class DividerLocationListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (evt.getPropertyName()
                    .equals(JSplitPane.DIVIDER_LOCATION_PROPERTY))
            {
                int dividerLocation = (Integer) evt.getNewValue();

                int writeAreaSize = mMessagePane.getHeight() - dividerLocation
                                    - mMessagePane.getDividerSize();

                if (writeAreaSize >= MIN_MESSAGE_PANEL_HEIGHT)
                {
                    ConfigurationUtils.setChatWriteAreaSize(writeAreaSize);
                }
                else
                {
                    sLog.warn("Asked to set a write area size of " +
                                writeAreaSize + " below the minimum of " +
                                MIN_MESSAGE_PANEL_HEIGHT);
                }
            }
        }
    }

    /**
     * Returns the accessible chat buffer associated with this chat session
     *
     * @return the accessible chat buffer associated with this chat session
     */
    @Override
    public AccessibleChatBuffer getAccessibleChatBuffer()
    {
        return mAccessibleBuffer;
    }

    /**
     * Adds the given file transfer <tt>id</tt> to the list of active file
     * transfers.
     *
     * @param id the identifier of the file transfer to add
     * @param descriptor the descriptor of the file transfer
     */
    public void addActiveFileTransfer(String id, Object descriptor)
    {
        synchronized (mActiveFileTransfers)
        {
            mActiveFileTransfers.put(id, descriptor);
        }
    }

    /**
     * Removes the given file transfer <tt>id</tt> from the list of active
     * file transfers.
     * @param id the identifier of the file transfer to remove
     */
    public void removeActiveFileTransfer(String id)
    {
        synchronized (mActiveFileTransfers)
        {
            mActiveFileTransfers.remove(id);
        }
    }

    @Override
    public void setVisible(boolean isVisible)
    {
//        never called, because ChatWindow setVisible is commented out.
//
//        super.setVisible(isVisible);
//
//        if (isVisible)
//        {
//            // Make sure the UI is displaying the correct connected state, as
//            // we're making the UI visible.
//            refreshUIConnectedState();
//
//            // We're making the panel visible so, if we haven't already loaded
//            // history, do so now.  We wait until we're making the panel
//            // visible to load history, partly because there is no point in
//            // loading it until then, but mostly because Swing may not render
//            // the messages correctly if the panel is not visible when the
//            // messages are added.
//            if (!mIsHistoryLoaded.get())
//            {
//                sLog.debug("Loading history.");
//                loadHistory();
//            }
//        }
    }

    @Override
    public void localUserPresenceChanged(LocalUserChatRoomPresenceChangeEvent evt)
    {
        ChatRoom evtChatRoom = evt.getChatRoom();

        ChatRoom chatRoom =
            ((ChatRoomWrapper) mChatSession.getDescriptor()).getChatRoom();

        if (evtChatRoom.equals(chatRoom))
        {
            if (evt.isLeavingEvent())
            {
                sLog.debug(
                    "User left the group chat - clear notifications: " +
                    sanitiseChatRoom(chatRoom.getIdentifier()));
                // We haven't received a new message in this chat room -
                // we've just left it, so make sure we just update the
                // 'Recent' tab entry in place, rather than moving it to
                // the top of the list.
                clearNewMessageNotifications(true);
            }
            else
            {
                // If this is not a leaving event, the user has joined the
                // chat room, so initialize the chat room.
                sLog.debug(
                    "User joined the group chat - initialize chat room: " +
                    sanitiseChatRoom(chatRoom.getIdentifier()));
                if (mChatSession != null)
                {
                    ((ConferenceChatSession) mChatSession).initChatRoom();
                }
                else
                {
                    sLog.debug("Chat session is null - " +
                               "unable to initialize chat room : "  +
                               sanitiseChatRoom(chatRoom.getIdentifier()));
                }
            }
        }
    }

    /**
     * Sets the last message in this chat panel as read
     *
     * @param isRead If true, mark the message as read, otherwise mark it as
     * unread.
     * @param isRefresh If true, the existing history entry for this message
     * should be refreshed in its place in the 'Recent' tab, rather than moved
     * to the top of the list.
     */
    private void setLastMessageReadStatus(boolean isRead, boolean isRefresh)
    {
        MessageHistoryService msgHistoryService =
                                        GuiActivator.getMessageHistoryService();

        if (msgHistoryService != null)
        {
            // We need to pass either a ChatRoom, MetaContact or SMS number
            // to the message history service so it can find the correct
            // conversation to set as read/unread.  For MetaContactChatSessions
            // and SMSChatSessions, this is the just session descriptor.  For
            // ConferenceChatSessions, the ChatRoomWrapper is the descriptor,
            // so we need to get the ChatRoom from that.
            Object descriptor;
            if (mChatSession instanceof ConferenceChatSession)
            {
                descriptor =
                    ((ChatRoomWrapper) mChatSession.getDescriptor()).getChatRoom();
            }
            else
            {
                descriptor = mChatSession.getDescriptor();
            }

            msgHistoryService.setLastMessageReadStatus(descriptor, isRead, isRefresh);
        }
    }

    /**
     * Clears the new message notification displayed for this conversation on
     * the 'Chats' tab of the 'Recent' tab.
     *
     * @param isRefresh If true, the existing history entry for this
     * notification should be refreshed in its place in the 'Recent' tab,
     * rather than moved to the top of the list.
     */
    protected void clearNewMessageNotifications(boolean isRefresh)
    {
        setLastMessageReadStatus(true, isRefresh);

        Object descriptor = mChatSession.getDescriptor();

        // We clear the message notification by calling stopNotification
        // on the NotificationService.
        // If the chat session's descriptor is a
        // MetaContact, we need to clear any notifications for all IM
        // addresses or SMS numbers related to that MetaContact.  Otherwise,
        // we can just clear the notification for the chat name, which will
        // either be a chat room ID or an SMS number.
        sLog.debug("Clearing new message notifications for " + descriptor);

        String displayName = "";
        ArrayList<Object> notificationsToStop = new ArrayList<Object>();

        if (descriptor instanceof MetaContact)
        {
            MetaContact contact = (MetaContact) descriptor;
            Contact imContact = contact.getIMContact();

            if (imContact != null)
            {
                displayName = imContact.getAddress();
                notificationsToStop.add(imContact);
            }
            if (ConfigurationUtils.isSmsEnabled())
            {
                // If there are multiple SMS numbers associated with this
                // contact, we don't know which one the chat notification is
                // indexed by, so send all of them to notification service.
                notificationsToStop.add(new LinkedList<>(contact.getSmsNumbers()));
            }
        }
        else
        {
            displayName = mChatSession.getChatName();
            // We can only have either a chat room or an SMS to a number that isn't a contact
            notificationsToStop.add(descriptor instanceof ChatRoomWrapper ?
                               ((ChatRoomWrapper) descriptor).getChatRoom() :
                               displayName);
        }
        for (Object notification : notificationsToStop)
        {
            HashMap<String, Object> extras = new HashMap<>();
            extras.put(NotificationData.MESSAGE_NOTIFICATION_TAG_EXTRA, notification);
            GuiActivator.getNotificationService().stopNotification(
                    new NotificationData("IncomingMessage",
                                         displayName,
                                         null,
                                         null,
                                         extras)
            );
        }
    }

    /**
     * Sets the UI of this panel to be that of a chat room that the user has
     * previously left.
     *
     * @param hasLeft whether the chat room has been left
     */
    public void setLeftChatRoomUI(boolean hasLeft)
    {
        // There's nothing to do if we already set the correct UI.
        if (hasLeft != mHasLeft)
        {
            mHasLeft = hasLeft;

            if (mHasLeft)
            {
                // Make sure we tidy up any existing dormant group chat panel
                // before creating a new one.
                if (mDormantGroupChatPanel != null)
                {
                    mDormantGroupChatPanel.dispose();
                }

                ((ChatWindow)getChatContainer()).getChatToolbar().convertLiveToDormant();
                mDormantGroupChatPanel = new DormantGroupChatPanel(this);
                setBottomPanel(mDormantGroupChatPanel);
                mMessagePane.setEnabled(false);
            }
            else
            {
                setBottomPanel(mWriteMessagePanel);
                mMessagePane.setEnabled(true);
                ((ChatWindow)getChatContainer()).getChatToolbar().convertDormantToLive();

                // We have just uplifted a chat session to a conference, so we
                // should clear the conversation panel
                sLog.debug("Clearing the conversation panel as conversation uplifted to conference");
                mConversationPanel.clear();
                mConversationPanel.setDefaultContent();
            }
        }

        getChatContainer().updateContainer(this);
    }

    /**
     * @return Whether the user has left the chat room.
     */
    public boolean hasLeft()
    {
        return mHasLeft;
    }

    /**
     * @return Whether the chat panel has become disconnected from the server.
     */
    public boolean isDisconnected()
    {
        return mIsDisconnected;
    }

    @Override
    public ChatType getChatType()
    {
        ChatType chatType = ChatType.ONE_TO_ONE_CHAT;
        if (mChatSession != null && mChatSession instanceof ConferenceChatSession)
        {
            chatType = ChatType.GROUP_CHAT;
        }

        return chatType;
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        sLog.debug("Registration state changed to '" + evt.getNewState() +
                                                            "' - updating UI");

        // The registration state has changed so refresh connected state of
        // the UI.
        refreshUIConnectedState();
    }

    /**
     * Updates the connected state of the UI (e.g. lost contact panel for
     * group chat, offline warning for one-to-one chats and the chat write
     * area hint text and enabled state).
     */
    private void refreshUIConnectedState()
    {
        // If this is a group chat and the IM provider isn't registered,
        // display the lost contact group chat panel.
        if (mChatSession instanceof ConferenceChatSession &&
            mImProvider != null &&
            !mImProvider.isRegistered())
        {
            sLog.debug("Jabber account unregistered");

            // Don't set a lost contact group chat panel if:
            // - We've left the chat, as we don't want to replace the
            //   dormant group chat panel and we don't want to try to
            //   automatically reconnect to the chat room once we re-register.
            // - Or the chat panel isn't visible.  In that case there is no
            //   need to display it when we reconnect.
            if (!mHasLeft)
            {
                ChatWindow chatWindow = (ChatWindow)getChatContainer();
                if (chatWindow.isVisible())
                {
                    mIsDisconnected = true;
                    mWriteMessagePanel.setVisible(false);

                    // Make sure we tidy up any existing lost contact group chat
                    // panel before creating a new one.
                    if (mLostContactGroupChatPanel != null)
                    {
                        mLostContactGroupChatPanel.dispose();
                    }

                    mLostContactGroupChatPanel = new LostContactGroupChatPanel(this);
                    setBottomPanel(mLostContactGroupChatPanel);
                    mMessagePane.setEnabled(false);
                    chatWindow.getChatToolbar().disableParticipants();
                }
                else
                {
                    sLog.debug(
                        "Ignoring unregistered event as the chat is not visible");
                }
            }
            else
            {
                sLog.debug("Ignoring unregistered event as we've left the chat");
            }
        }

        updateOfflineWarning(OfflineWarning.DEFAULT_OFFLINE_WARNING);
        getChatWritePanel().updateEditorPaneState();
        getChatContainer().updateContainer(this);
    }
}
