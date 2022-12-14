/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.atomic.*;

import javax.swing.*;
import javax.swing.text.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.filetransfer.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.database.schema.FileHistoryTable;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.notification.NotificationData;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.GuiUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

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
                Skinnable,
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
     * The configuration service.
     */
    private final ConfigurationService mConfigService =
        GuiActivator.getConfigurationService();

    /**
     * The ChatTransport used for the most recently received message in this
     * panel.
     */
    private ChatTransport mLatestChatTransport;

    /**
     * A task that resets the current chat transport to send to all registered
     * resources for this chat session after a given period of time since we
     * last received a message.
     */
    private TimerTask mChatTransportResetTask;

    /**
     * Timer used to schedule the task to reset the current chat transport to
     * send to all registered resources for this chat session.
     */
    private static Timer mTimer = new Timer("Global chat transport reset timer", true);

    /**
     * Component that contains any notification - either typing or offline warning
     */
    private final JLabel mNotificationArea;

    /**
     * The typing notification icon.
     */
    private final ImageIconFuture mTypingIcon = GuiActivator.getResources()
        .getImage("service.gui.icons.TYPING");

    /**
     * The default number of ms since we last received a chat message after
     * which we reset the current chat transport to send to all registered
     * resources for this chat session (default 5 mins).
     */
    private static final int CHAT_TRANSPORT_RESET_DEFAULT_DELAY = 300000;

    /**
     * The property which holds the configured number of ms since we last
     * received a chat message after which we reset the current chat transport
     * to send to all registered resources for this chat session.
     */
    private static final String CHAT_TRANSPORT_RESET_DELAY_PROP =
        "net.java.sip.communicator.impl.gui.main.chat.CHAT_TRANSPORT_RESET_DELAY";

    /**
     * The property which holds the maximum permissible length of XMPP message
     * that the user may send.
     */
    private static final String MAX_XMPP_MESSAGE_LENGTH_PROP =
        "net.java.sip.communicator.impl.gui.main.chat.MAX_XMPP_MESSAGE_LENGTH";

    /**
     * The property which holds the error message shown if the user tries to
     * send an XMPP message which is too long.
     */
    private static final String XMPP_MESSAGE_TOO_LONG_PROP =
        "service.gui.XMPP_MESSAGE_TOO_LONG";

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
     * Buffer to store received messages.  This should be only used to store messages that are received
     * whilst the history is being loaded, to make sure that new messages are displayed after old loaded
     * messages.
     */
    private final Vector<Object> mIncomingEventBuffer = new Vector<>();

    /**
     * If true, the chat panel has finished loading history.
     */
    private final AtomicBoolean mIsHistoryLoaded = new AtomicBoolean();

    /**
     * Stores all active  file transfer requests and effective transfers with
     * the identifier of the transfer.
     */
    private final Hashtable<String, Object> mActiveFileTransfers
        = new Hashtable<>();

    /**
     * The ID of the message being corrected, or <tt>null</tt> if
     * not correcting any message.
     */
    private String mCorrectedMessageUID = null;

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
     * @param loadHistory whether to load chat history into this chat panel
     * when it first becomes visible.
     */
    public ChatPanel(ChatContainer chatContainer,
                     boolean loadHistory)
    {
        super(new BorderLayout());

        mChatContainer = chatContainer;

        // If we're not going to load history, mark it as already loaded.
        mIsHistoryLoaded.set(!loadHistory);

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
                                           mChatSession.getDescriptor() + ">");
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

        if (mChatTransportResetTask != null)
        {
            mChatTransportResetTask.cancel();
        }

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
     * Returns the ID of the last message sent in this chat, or <tt>null</tt>
     * if no messages have been sent yet.
     *
     * @return the ID of the last message sent in this chat, or <tt>null</tt>
     * if no messages have been sent yet.
     */
    public String getLastSentMessageUID()
    {
        return mLastSentMessageUID;
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
     * Process history messages.
     *
     * @param historyList The collection of messages coming from history.
     */
    private void processHistory(Collection<Object> historyList)
    {
        sLog.debug("Processing history.");
        Iterator<Object> iterator = historyList.iterator();

        while (iterator.hasNext())
        {
            Object o = iterator.next();
            String historyString = "";

            if (o instanceof MessageEvent)
            {
                MessageEvent evt = (MessageEvent) o;
                ImMessage sourceMessage = evt.getSourceMessage();

                // Only add the message if it should be displayed to the user
                if (evt.isDisplayed())
                {
                    historyString = processHistoryMessage(
                        evt.getContactAddress(),
                        evt.getContactDisplayName(),
                        evt.getTimestamp(),
                        evt.getMessageType(),
                        sourceMessage.getContent(),
                        sourceMessage.getContentType(),
                        sourceMessage.getMessageUID(),
                        evt.getErrorMessage());
                }
            }
            else if (o instanceof FileRecord)
            {
                // If a file transfer is received when the chat window is
                // closed, it may already have made it into the chat history
                // at the point when the chat window is opened.  If so, it
                // would be added to the chat window twice - once when loaded
                // from history here and once as a new incoming file transfer,
                // after history has been loaded into the window.  Also, the
                // file transfer UI element added from history in that case
                // would be broken, as history is not designed to handle
                // active file transfers.  Therefore, to avoid displaying a
                // duplicate, broken UI element, we ignore any active file
                // transfers whilst loading history here.
                FileRecord fileRecord = (FileRecord) o;
                if (!FileHistoryTable.STATUS.ACTIVE.equals(fileRecord.getStatus()))
                {
                    FileHistoryConversationComponent component
                        = new FileHistoryConversationComponent(fileRecord);
                    mConversationPanel.addComponent(component);
                }
                else
                {
                    sLog.debug(
                        "Not loading active file transfer from history: " +
                                                           fileRecord.getID());
                }
            }

            if (historyString != null)
            {
                mConversationPanel.appendMessageToEnd(
                    historyString, ChatHtmlUtils.TEXT_CONTENT_TYPE);
            }
        }
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param contactAddress the address of the contact sending the message
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE
     * @param message the message text
     * @param contentType the content type
     * @param contactAddress the address of the contact sending the message
     * @param isArchive indicates that this message is an archive message
     * @param isCarbon indicates that this message is a carbon message
     */
    @Override
    public void addMessage(String contactAddress,
                           Date date,
                           String messageType,
                           String message,
                           String contentType,
                           boolean isArchive,
                           boolean isCarbon)
    {
        addMessage(contactAddress,
                   null,
                   date,
                   messageType,
                   message,
                   contentType,
                   null,
                   null,
                   isArchive,
                   isCarbon);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param contactAddress the address of the contact sending the message
     * @param displayName the display name of the contact
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE
     * @param message the message text
     * @param contentType the content type
     * @param messageUID The ID of the message.
     * @param correctedMessageUID The ID of the message being replaced.
     * @param isArchive indicates that this message is an archive message
     * @param isCarbon indicates that this message is a carbon message
     */
    public void addMessage(String contactAddress,
                           String displayName,
                           Date date,
                           String messageType,
                           String message,
                           String contentType,
                           String messageUID,
                           String correctedMessageUID,
                           boolean isArchive,
                           boolean isCarbon)
    {
        addMessage(contactAddress,
                   displayName,
                   date,
                   messageType,
                   message,
                   contentType,
                   messageUID,
                   correctedMessageUID,
                   null,
                   null,
                   isArchive,
                   isCarbon);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param contactAddress the address of the contact sending the message
     * @param displayName the display name of the contact
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE
     * @param message the message text
     * @param contentType the content type
     * @param messageUID The ID of the message.
     * @param correctedMessageUID The ID of the message being replaced.
     * @param failedMessageUID The ID of the message that failed to send that
     *                         this message should replace (can be null).
     * @param errorMessage The error to display about the message failure
     *                     (can be null).
     * @param isArchive indicates that this message is an archive message
     * @param isCarbon indicates that this message is a carbon message
     */
    public void addMessage(String contactAddress,
                           String displayName,
                           Date date,
                           String messageType,
                           String message,
                           String contentType,
                           String messageUID,
                           String correctedMessageUID,
                           String failedMessageUID,
                           String errorMessage,
                           boolean isArchive,
                           boolean isCarbon)
    {
        sLog.debug("Getting display name for " + logHasher(contactAddress));
        String contactDisplayName =
                    AccountUtils.getDisplayNameFromChatAddress(contactAddress);

        // Always display the latest display name for the contact.
        if (!contactDisplayName.equals(contactAddress))
        {
            displayName = contactDisplayName;
        }

        ChatMessage chatMessage = new ChatMessage(contactAddress,
                                                  displayName,
                                                  date,
                                                  messageType,
                                                  null,
                                                  message,
                                                  contentType,
                                                  messageUID,
                                                  correctedMessageUID,
                                                  failedMessageUID,
                                                  errorMessage,
                                                  isArchive,
                                                  isCarbon);

        addChatMessage(chatMessage);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param contactAddress the address of the contact sending the message
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE
     * @param title the title of the message
     * @param message the message text
     * @param contentType the content type
     * @param isArchive true if the message to add is an archive message
     * @param isCarbon indicates that this message is a carbon message
     */
    public void addMessage(String contactAddress,
                           Date date,
                           String messageType,
                           String title,
                           String message,
                           String contentType,
                           boolean isArchive,
                           boolean isCarbon)
    {
        String contactDisplayName =
                    AccountUtils.getDisplayNameFromChatAddress(contactAddress);

        ChatMessage chatMessage = new ChatMessage(contactAddress,
                                                  contactDisplayName,
                                                  date,
                                                  messageType,
                                                  title,
                                                  message,
                                                  contentType,
                                                  null,
                                                  null,
                                                  isArchive,
                                                  isCarbon);

        addChatMessage(chatMessage);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param chatMessage the chat message to add
     */
    private void addChatMessage(final ChatMessage chatMessage)
    {
        // We need to be sure that chat messages are added in the event dispatch
        // thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    addChatMessage(chatMessage);
                }
            });
            return;
        }

        if (!mIsHistoryLoaded.get())
        {
            synchronized (mIncomingEventBuffer)
            {
                sLog.debug(
                    "Adding new chat message to incoming event buffer: " +
                                                   chatMessage.getMessageUID());
                mIncomingEventBuffer.add(chatMessage);
            }
        }
        else
        {
            displayChatMessage(chatMessage);
        }
    }

    /**
     * Adds the given error message to the chat window conversation area.
     *
     * @param contactAddress the address of the contact, for which the error occured
     * @param message the error message
     */
    public void addErrorMessage(String contactAddress,
                                String message)
    {
        addMessage(contactAddress,
                    new Date(),
                    Chat.ERROR_MESSAGE,
                    "",
                    message,
                    "text",
                    false,
                    false);
    }

    /**
     * Adds the given error message to the chat window conversation area.
     *
     * @param contactAddress the address of the contact, for which the error occurred
     * @param title the title of the error
     * @param message the error message
     */
    public void addErrorMessage(String contactAddress,
                                String title,
                                String message)
    {
        addMessage(contactAddress,
                   new Date(),
                   Chat.ERROR_MESSAGE,
                   title,
                   message,
                   "text",
                   false,
                   false);
    }

    /**
     * Displays the given file transfer request and either sends the file or
     * sets new message notifications, as appropriate.
     *
     * @param component the FileTransferConversationComponent to display.
     */
    public void displayFileTransfer(FileTransferConversationComponent component)
    {
        getChatConversationPanel().addComponent(component);

        if (component instanceof SendFileConversationComponent)
        {
            SendFileConversationComponent sendFileComponent =
                                     (SendFileConversationComponent) component;
            sendFile(sendFileComponent);
        }
        if (component instanceof ReceiveFileConversationComponent)
        {
            if (isChatFocused())
            {
                sLog.debug(
                    "Chat panel focussed - clear new message notifications");
                // We've received a new message so make sure we don't just refresh
                // the 'Recent' tab entry in place, as we want it to move to the
                // top of the list.
                clearNewMessageNotifications(false);
            }
            else
            {
                // The chat panel is not in focus, so set a new message
                // notification for sender address, if we have one.
                IncomingFileTransferRequest request =
                    ((ReceiveFileConversationComponent) component).
                                              getIncomingFileTransferRequest();
                if (request != null)
                {
                    Contact sender = request.getSender();
                    if (sender != null)
                    {
                        String senderAddress = sender.getAddress();
                        if (!StringUtils.isNullOrEmpty(senderAddress))
                        {
                            setNewMessageNotification(senderAddress);
                        }
                    }
                }
            }
        }
    }

    /**
     * Displays the given chat message and sets new message notifications, if
     * necessary.
     *
     * @param chatMessage the chat message to display
     */
    private void displayChatMessage(ChatMessage chatMessage)
    {
        String correctedUID = chatMessage.getCorrectedMessageUID();
        boolean isCorrected = correctedUID != null &&
                mConversationPanel.getMessageContents(correctedUID) != null;

        String failedUID = chatMessage.getFailedMessageUID();
        boolean isFailed = failedUID != null;

        if (isFailed)
        {
            if (mConversationPanel.getMessageContents(failedUID) == null)
            {
                // If the failed message isn't currently being displayed in
                // the panel, there's nothing for us to mark as failed, so
                // just return.
                sLog.debug(
                    "Failed message not loaded in chat panel: " + failedUID);
                return;
            }
        }

        // If an existing message is either being corrected or has failed to be
        // delivered, update the existing message.  Otherwise, append a new
        // message to the end of the conversation.
        if (isCorrected || isFailed)
        {
            applyMessageUpdate(chatMessage);
        }
        else
        {
            appendChatMessage(chatMessage);
        }

        String messageType = chatMessage.getMessageType();

        // We only want to set or clear unread message notifications based on
        // incoming messages.
        if (messageType.equals(Chat.INCOMING_MESSAGE) ||
            messageType.equals(Chat.INCOMING_SMS_MESSAGE))
        {
            if (isChatFocused() || chatMessage.isCarbon())
            {
                sLog.debug("Chat panel focussed or carbon - " +
                           "clear new message notifications");
                // We've received a new message so make sure we don't just
                // refresh the 'Recent' tab entry in place, as we want it to
                // move to the top of the list.
                // If this is a carbon then we must have responded to any
                // new messages on another client, so the notification can be
                // cleared. This is not true for archive messages, which can
                // be received alongside new messages, so do not clear the
                // notifications in that case.
                clearNewMessageNotifications(false);
            }
            else if (!chatMessage.isArchive())
            {
                // The chat panel is not in focus, so set a new message
                // notification for the IM address / SMS number that sent the
                // message.  If this is a MetaContactChatSession, we don't
                // know which of the MetaContact's IM addresses / SMS numbers
                // sent the message so we need to get the address that sent
                // the message from the ChatMessage.  Otherwise, we can just
                // use the chat name, as that will be either the ID of the
                // chat room for a ConferenceChatSession or the SMS number for
                // an SMSChatSession.
                //
                String contactAddress =
                    (mChatSession instanceof MetaContactChatSession) ?
                                              chatMessage.getContactAddress() :
                                              mChatSession.getChatName();
                sLog.debug("Chat panel not focussed - " +
                           "set new message notification from " + logHasher(contactAddress));
                setNewMessageNotification(contactAddress);
            }
        }
        else if (messageType.equals(Chat.OUTGOING_MESSAGE))
        {
            mLastSentMessageUID = chatMessage.getMessageUID();
            if (chatMessage.isCarbon())
            {
                sLog.debug("Carbon for sent message - " +
                           "clear new message notifications");
                // If this is a carbon then we must have responded to any
                // new messages on another client, so the notification can be
                // cleared. This is not true for archive messages, which can
                // be received alongside new messages, so do not clear the
                // notifications in that case.
                clearNewMessageNotifications(false);
            }
        }
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and appends it at the end of the conversationPanel
     * document.
     *
     * @param chatMessage the message to append
     */
    private void appendChatMessage(ChatMessage chatMessage)
    {
        ChatTransport transport = mChatSession.getCurrentChatTransport();

        ProtocolProviderService protocolProvider;
        String name;
        if (transport == null)
        {
            if (chatMessage.getMessageType().equals(Chat.ERROR_MESSAGE))
            {
                protocolProvider = AccountUtils.getImProvider();
                name = "";
            }
            else
            {
                return;
            }
        }
        else
        {
            protocolProvider = transport.getProtocolProvider();
            name = transport.getName();
        }

        String processedMessage
            = mConversationPanel.processMessage(chatMessage,
                                                protocolProvider,
                                                name);

        mConversationPanel.appendMessageToEnd(
            processedMessage, chatMessage.getContentType());

        // If we haven't received an incoming message for a while, we reset
        // the current chat transport to send to all registered resources for
        // this chat session.  Therefore, if this is an incoming message, we
        // need to restart the timer that resets the chat transport.
        if (chatMessage.getMessageType().equals(Chat.INCOMING_MESSAGE))
        {
            sLog.debug("Incoming chat message - reset chat transport timer");
            resetChatTransportTimer();

            ChatTransport currentChatTransport = transport;
            String resourceName = currentChatTransport.getResourceName();

            if (resourceName != null)
            {
                sLog.debug("Updating mLatestChatTransport to " + resourceName );
                mLatestChatTransport = currentChatTransport;
            }
        }
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing and updates it either to correct its text or add an error
     * message.
     *
     * @param message The message containing the updated text or error message.
     */
    private void applyMessageUpdate(ChatMessage message)
    {
        mConversationPanel.updateMessage(message);
    }

    /**
     * Passes the message to the contained <code>ChatConversationPanel</code>
     * for processing.
     *
     * @param contactAddress The name of the contact sending the message.
     * @param contactDisplayName the display name of the contact sending the
     * message
     * @param date The time at which the message is sent or received.
     * @param messageType The type of the message. One of OUTGOING_MESSAGE
     * or INCOMING_MESSAGE.
     * @param message The message text.
     * @param contentType the content type of the message (html or plain text)
     * @param messageId The ID of the message.
     * @param errorMessage the error message to display (will be null if the
     *                     message hasn't failed)
     * @return a string containing the processed message.
     */
    private String processHistoryMessage(String contactAddress,
                                         String contactDisplayName,
                                         Date date,
                                         String messageType,
                                         String message,
                                         String contentType,
                                         String messageId,
                                         String errorMessage)
    {
        String displayName =
                    AccountUtils.getDisplayNameFromChatAddress(contactAddress);

        if (displayName == null)
        {
            // If there is no display name, this will just be a dummy history
            // message so return null so we don't display it.
            return null;
        }

        // Always display the latest display name for the contact.
        if (!displayName.equals(contactAddress))
        {
            contactDisplayName = displayName;
        }

        ChatMessage chatMessage = new ChatMessage(
            contactAddress, contactDisplayName, date, messageType, null, message,
            contentType, messageId, null, null, errorMessage, false, false);

        ChatTransport chatTransport = mChatSession.getCurrentChatTransport();

        // ChatTransport can be null if the other party is offline or has been
        // deleted.
        //
        // One way to hit this is: we are offline, the other party sends a
        // message to us (which the server queues) then the other party goes
        // offline.  When we come online, we receive the message which triggers
        // the ChatWindow to open and this code to be executed, but there is no
        // ChatTransport.
        ProtocolProviderService protocolProvider = (chatTransport != null) ?
            chatTransport.getProtocolProvider() :
            AccountUtils.getImProvider();

        String chatName = (chatTransport != null) ? chatTransport.getName() : "";

        return mConversationPanel.processMessage(
            chatMessage,
            protocolProvider,
            chatName);
    }

    /**
     * Refreshes write area editor pane. Deletes all existing text
     * content.
     */
    public void refreshWriteArea()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    refreshWriteArea();
                }
            });
            return;
        }

        mWriteMessagePanel.clearWriteArea();
    }

    /**
     * Adds text to the write area editor.
     *
     * @param text The text to add.
     */
    public void addTextInWriteArea(String text)
    {
        JEditorPane editorPane = mWriteMessagePanel.getEditorPane();
        editorPane.setText(editorPane.getText() + text);
    }

    /**
     * Returns the text contained in the write area editor.
     * @param mimeType the mime type
     * @return The text contained in the write area editor.
     */
    public String getTextFromWriteArea(String mimeType)
    {
        if (mimeType.equals(
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE))
        {
            return mWriteMessagePanel.getText();
        }
        else
        {
            return mWriteMessagePanel.getTextAsHtml();
        }
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
        if (!isWriteAreaEmpty())
        {
            new Thread("ChatPanel sendButtonDoClick")
            {
                @Override
                public void run()
                {
                    sendMessage();
                }
            }.start();
        }

        //make sure the focus goes back to the write area
        requestFocusInWriteArea();
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
     * Sends the given file through the currently selected chat transport by
     * using the given fileComponent to visualize the transfer process in the
     * chat conversation panel.
     *
     * @param fileComponent the file component to use for visualization
     */
    public void sendFile(final SendFileConversationComponent fileComponent)
    {
        final ChatTransport sendFileTransport
            = findFileTransferChatTransport();

        setSelectedChatTransport(sendFileTransport);

        final File file = fileComponent.getFile();

        if (file.length() > sendFileTransport.getMaximumFileLength())
        {
            addMessage(
                mChatSession.getCurrentChatTransport().getName(),
                new Date(),
                Chat.ERROR_MESSAGE,
                GuiActivator.getResources()
                    .getI18NString("service.gui.FILE_TOO_BIG",
                    new String[]{
                        sendFileTransport.getMaximumFileLength()/1024/1024
                        + " MB"}),
                "",
                "text",
                false,
                false);

            fileComponent.setFailed();

            return;
        }

        SwingWorker worker = new SwingWorker()
        {
            @Override
            public Object construct()
                throws Exception
            {
                final String fileTransferId = fileComponent.getId();
                final FileTransfer fileTransfer
                    = sendFileTransport.sendFile(file, fileTransferId);

                addActiveFileTransfer(fileTransferId, fileTransfer);

                // Add the status listener that would notify us when the file
                // transfer has been completed and should be removed from
                // active components.
                fileTransfer.addStatusListener(ChatPanel.this);

                fileComponent.setProtocolFileTransfer(fileTransfer);

                return "";
            }

            @Override
            public void catchException(Throwable ex)
            {
                sLog.error("Failed to send file.", ex);

                if (ex instanceof IllegalStateException)
                {
                    addErrorMessage(
                        mChatSession.getCurrentChatTransport().getName(),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.MSG_SEND_CONNECTION_PROBLEM"));
                }
                else if (ex instanceof OperationNotSupportedException)
                {
                    // Remote client doesn't support file transfers.
                    addErrorMessage(
                        mChatSession.getCurrentChatTransport().getName(),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.MSG_REMOTE_DOES_NOT_SUPPORT"));
                }
                else
                {
                    addErrorMessage(
                        mChatSession.getCurrentChatTransport().getName(),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.MSG_DELIVERY_ERROR",
                            new String[]{ex.getMessage()}));
                }
            }
        };

        worker.start();
    }

    /**
     * Sends the given file through the currently selected chat transport.
     *
     * @param file the file to send
     */
    public void sendFile(final File file)
    {
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    sendFile(file);
                }
            });
            return;
        }

        final ChatTransport fileTransferTransport
            = findFileTransferChatTransport();

        // If there's no operation set we show some "not supported" messages
        // and we return.
        if (fileTransferTransport == null)
        {
            sLog.error("Failed to send file.");

            addErrorMessage(
                mChatSession.getChatName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.FILE_SEND_FAILED",
                    new String[]{file.getName()}),
                GuiActivator.getResources().getI18NString(
                    "service.gui.FILE_TRANSFER_NOT_SUPPORTED"));

            return;
        }

        // By default, set the recipient display name to the display name of the
        // underlying chat contact.
        String displayName = fileTransferTransport.getDisplayName();

        if (getChatSession() instanceof MetaContactChatSession)
        {
            // We know we're chatting to a single MetaContact, so use their name
            // as the recipient display name.
            MetaContactChatSession session = (MetaContactChatSession) getChatSession();
            displayName = session.getMetaContact().getDisplayName();
        }

        final SendFileConversationComponent fileComponent
            = new SendFileConversationComponent(
                this,
                displayName,
                file);

        if (!mIsHistoryLoaded.get())
        {
            synchronized (mIncomingEventBuffer)
            {
                mIncomingEventBuffer.add(fileComponent);
            }
        }
        else
            displayFileTransfer(fileComponent);
    }

    /**
     * Sends the text contained in the write area as an SMS message or an
     * instance message depending on the "send SMS" check box.
     */
    protected void sendMessage()
    {
        if (mWriteMessagePanel.isSmsSelected())
        {
            sendSmsMessage();
        }
        else
        {
            sendInstantMessage();
        }
    }

    /**
     * Sends the text contained in the write area as an SMS message.
     */
    public void sendSmsMessage()
    {
        String messageText = getTextFromWriteArea(
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);

        ChatTransport smsChatTransport = mChatSession.getCurrentChatTransport();
        if (!smsChatTransport.allowsSmsMessage())
        {
            Iterator<ChatTransport> chatTransports
                = mChatSession.getChatTransports();

            while (chatTransports.hasNext())
            {
                ChatTransport transport = chatTransports.next();

                if (transport.allowsSmsMessage())
                {
                    smsChatTransport = transport;
                    break;
                }
            }
        }

        // If there's no operation set we show some "not supported" messages
        // and we return.
        if (!smsChatTransport.allowsSmsMessage())
        {
            sLog.error("Failed to send SMS.");

            refreshWriteArea();

            addMessage(
                smsChatTransport.getName(),
                new Date(),
                Chat.OUTGOING_MESSAGE,
                messageText,
                "plain/text",
                false,
                false);

            addErrorMessage(
                smsChatTransport.getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.SEND_SMS_NOT_SUPPORTED"));

            return;
        }

        smsChatTransport.addSmsMessageListener(
                new SmsMessageListener(smsChatTransport));

        // We open the send SMS dialog.
        SendSmsDialog smsDialog
            = new SendSmsDialog(this, smsChatTransport, messageText);

        smsDialog.setPreferredSize(new Dimension(400, 200));
        smsDialog.setVisible(true);
    }

    /**
     * Implements the <tt>ChatPanel.sendMessage</tt> method. Obtains the
     * appropriate operation set and sends the message, contained in the write
     * area, through it.
     */
    protected void sendInstantMessage()
    {
        String htmlText;
        String plainText;

        // read the text and clear it as quick as possible
        // to avoid double sending if the user hits enter too quickly
        synchronized(mWriteMessagePanel)
        {
            if (isWriteAreaEmpty())
                return;

            // Trims the html message, as it sometimes contains a lot of empty
            // lines, which causes some problems to some protocols.
            htmlText = getTextFromWriteArea(
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE).trim();

            plainText = getTextFromWriteArea(
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE).trim();

            // clear the message earlier
            // to avoid as much as possible to not sending it twice (double enter)
            refreshWriteArea();
        }

        if (mChatSession.getCurrentChatTransport() == null)
        {
            addErrorMessage("", GuiActivator.getResources().getI18NString(
                "service.gui.MSG_NOT_DELIVERED"));
            return;
        }

        String messageText;
        String mimeType;
        if (mChatSession.getCurrentChatTransport().isContentTypeSupported(
                    OperationSetBasicInstantMessaging.HTML_MIME_TYPE)
             && (htmlText.contains("<b")
                || htmlText.contains("<i")
                || htmlText.contains("<u")
                || htmlText.contains("<font")))
        {
            messageText = htmlText;
            mimeType = OperationSetBasicInstantMessaging.HTML_MIME_TYPE;
        }
        else
        {
            messageText = plainText;
            mimeType = OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE;
        }

        // Ensure the message is not too long.
        int arbitraryDefaultMessageLength = Chat.MAX_CHAT_MESSAGE_LENGTH;
        int maxLength = mConfigService.global().getInt(MAX_XMPP_MESSAGE_LENGTH_PROP,
                                             arbitraryDefaultMessageLength);
        if (messageText.length() > maxLength)
        {
            sLog.warn("User tried to send a message which was too long.");
            String[] params = {Integer.toString(maxLength)};
            addErrorMessage(
                mChatSession.getCurrentChatTransport().getName(),
                GuiActivator.getResources().getI18NString(
                    XMPP_MESSAGE_TOO_LONG_PROP, params));

            // Restore the write-area text so the user can edit it.
            synchronized(mWriteMessagePanel)
            {
                setMessage(messageText);
                JEditorPane writeArea = mWriteMessagePanel.getEditorPane();
                writeArea.setCaretPosition(writeArea.getDocument().getLength());
            }

            return;
        }

        // Ensure that there is at least one participant in this conversation
        // who can be messaged (i.e. we haven't been defriended by all the
        // participants).
        boolean participantsOnline = false;
        for (ChatContact<?> chatContact : mChatSession.getParticipants())
        {
            Object descriptor = chatContact.getDescriptor();
            if (descriptor instanceof MetaContact)
            {
                MetaContact contact = (MetaContact)descriptor;
                participantsOnline = contact.canBeMessaged();
                if (participantsOnline)
                {
                    break;
                }
            }
            else
            {
                // Give non-metacontact participants the benefit of the doubt,
                // and assume they can be messaged.
                participantsOnline = true;
                break;
            }
        }

        if (!participantsOnline)
        {
            sLog.warn("User tried to send a message after the recipient stopped being a buddy.");
            for (ChatContact<?> chatContact : mChatSession.getParticipants())
            {
                sLog.info("chatContact name " + logHasher(chatContact.getName()) +
                    ", UID " + logHasher(chatContact.getUID()) + " can't be messaged");
            }

            String[] params = {Integer.toString(maxLength)};
            addErrorMessage(
                mChatSession.getCurrentChatTransport().getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_NOT_POSSIBLE", params));

            return;
        }

        try
        {
            // Before sending the message, check whether the last message we
            // received was from a different chat transport than the one the
            // chat session is currently set to use and, if so, reset to
            // sending messages to all registered resources for this session.
            checkChatTransport();

            if (isMessageCorrectionActive()
                    && mChatSession.getCurrentChatTransport()
                        .allowsMessageCorrections())
            {
                mChatSession.getCurrentChatTransport().correctInstantMessage(
                        messageText, mimeType, mCorrectedMessageUID);
            }
            else
            {
                mChatSession.getCurrentChatTransport().sendInstantMessage(
                        messageText, mimeType);
            }
            stopMessageCorrection();
        }
        catch (IllegalStateException ex)
        {
            sLog.error("Failed to send message.", ex);

            addMessage(
                mChatSession.getCurrentChatTransport().getName(),
                new Date(),
                Chat.OUTGOING_MESSAGE,
                messageText,
                mimeType,
                false,
                false);

            addErrorMessage(
                mChatSession.getCurrentChatTransport().getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_SEND_CONNECTION_PROBLEM"));
        }
        catch (Exception ex)
        {
            sLog.error("Failed to send message.", ex);

            refreshWriteArea();

            addMessage(
                mChatSession.getCurrentChatTransport().getName(),
                new Date(),
                Chat.OUTGOING_MESSAGE,
                messageText,
                mimeType,
                false,
                false);

            addErrorMessage(
                mChatSession.getCurrentChatTransport().getName(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_ERROR"));
        }

        getChatWritePanel().updateTypingState(TypingState.NOT_TYPING);

        AnalyticsService analytics = GuiActivator.getAnalyticsService();

        analytics.onEventWithIncrementingCount(AnalyticsEventType.SEND_IM, new ArrayList<>());

        if(mChatSession instanceof ConferenceChatSession)
        {
            analytics.onEventWithIncrementingCount(AnalyticsEventType.SEND_GROUP_IM, new ArrayList<>());
        }
    }

    /**
     * Enters editing mode for the last sent message in this chat.
     */
    public void startLastMessageCorrection()
    {
        startMessageCorrection(mLastSentMessageUID);
    }

    /**
     * Enters editing mode for the message with the specified id - puts the
     * message contents in the write panel and changes the background.
     *
     * @param correctedMessageUID The ID of the message being corrected.
     */
    public void startMessageCorrection(String correctedMessageUID)
    {
        if (mChatSession.getCurrentChatTransport().allowsMessageCorrections())
        {
            if (!showMessageInWriteArea(correctedMessageUID))
            {
                return;
            }

            mCorrectedMessageUID = correctedMessageUID;
            Color bgColor = new Color(GuiActivator.getResources()
                .getColor("service.gui.CHAT_EDIT_MESSAGE_BACKGROUND"));
            mWriteMessagePanel.setEditorPaneBackground(bgColor);
        }
    }

    /**
     * Shows the last sent message in the write area, either in order to
     * correct it or to send it again.
     *
     * @return <tt>true</tt> on success, <tt>false</tt> on failure.
     */
    public boolean showLastMessageInWriteArea()
    {
        return showMessageInWriteArea(mLastSentMessageUID);
    }

    /**
     * Shows the message with the specified ID in the write area, either
     * in order to correct it or to send it again.
     *
     * @param messageUID The ID of the message to show.
     * @return <tt>true</tt> on success, <tt>false</tt> on failure.
     */
     public boolean showMessageInWriteArea(String messageUID)
     {
         String messageContents = mConversationPanel.getMessageContents(
             messageUID);

         if (messageContents == null)
         {
             return false;
         }

         messageContents = processMessageTextForWriteAreaDisplay(messageContents);
         refreshWriteArea();
         setMessage(messageContents);

         return true;
     }

     /*
      * Removes and replaces HTML to allow a message being edited to display correctly in
      * the write area (as the refreshText method needs plaintext input)
      *
      * @param message The text of the message to be edited
      *
      */
     private String processMessageTextForWriteAreaDisplay(String message)
     {
         return GuiUtils.removeLinkTags(
             GuiUtils.replaceHTMLCharCodes(
             GuiUtils.replaceBreakTags(
             GuiUtils.removePlaintextTags(message))));
     }

    /**
     * Exits editing mode, clears the write panel and the background.
     */
    public void stopMessageCorrection()
    {
        mCorrectedMessageUID = null;
        mWriteMessagePanel.setEditorPaneBackground(Color.WHITE);
        refreshWriteArea();
    }

    /**
     * Returns whether a message is currently being edited.
     *
     * @return <tt>true</tt> if a message is currently being edited,
     * <tt>false</tt> otherwise.
     */
    public boolean isMessageCorrectionActive()
    {
        return mCorrectedMessageUID != null;
    }

    /**
     * Listens for SMS messages and shows them in the chat.
     */
    private class SmsMessageListener implements MessageListener
    {
        /**
         * Initializes a new <tt>SmsMessageListener</tt> instance.
         *
         * @param chatTransport Currently unused
         */
        public SmsMessageListener(ChatTransport chatTransport)
        {
        }

        @Override
        public void messageDelivered(MessageDeliveredEvent evt)
        {
            ImMessage msg = evt.getSourceMessage();

            Contact contact = evt.getPeerContact();

            addMessage(
                contact.getDisplayName(),
                new Date(),
                Chat.OUTGOING_MESSAGE,
                msg.getContent(),
                msg.getContentType(),
                msg.isArchive(),
                msg.isCarbon());

            addMessage(
                    contact.getDisplayName(),
                    new Date(),
                    Chat.ACTION_MESSAGE,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.SMS_SUCCESSFULLY_SENT"),
                    "text",
                    msg.isArchive(),
                    msg.isCarbon());
        }

        @Override
        public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
        {
            sLog.error(evt.getReason());

            String errorMsg = null;

            ImMessage sourceMessage = (ImMessage) evt.getSource();

            Contact sourceContact = evt.getPeerContact();
            String peerIdentifier = evt.getPeerIdentifier();
            String displayName = "";
            MetaContact metaContact = null;

            if (sourceContact != null)
            {
                displayName = sourceContact.getDisplayName();
                metaContact = GuiActivator.getContactListService().
                                        findMetaContactByContact(sourceContact);
            }
            else
            {
                displayName = peerIdentifier;
                metaContact = GuiActivator.getContactListService().
                                    findMetaContactForSmsNumber(peerIdentifier);
            }

            if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED)
            {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_NOT_SUPPORTED",
                    new String[]{displayName});
            }
            else if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.NETWORK_FAILURE)
            {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_NOT_DELIVERED");
            }
            else if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED)
            {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_SEND_CONNECTION_PROBLEM");
            }
            else if (evt.getErrorCode()
                    == MessageDeliveryFailedEvent.INTERNAL_ERROR)
            {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_INTERNAL_ERROR");
            }
            else
            {
                errorMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.MSG_DELIVERY_UNKNOWN_ERROR");
            }

            // Log the error message and internal reason to file
            sLog.debug(errorMsg + evt.getReason());

            if (metaContact != null)
            {
                displayName = metaContact.getDisplayName();
            }

            String messageType =
                (evt.getEventType() == MessageEvent.SMS_MESSAGE) ?
                    Chat.OUTGOING_SMS_MESSAGE : Chat.OUTGOING_MESSAGE;

            addMessage(
                    displayName,
                    new Date(),
                    messageType,
                    sourceMessage.getContent(),
                    sourceMessage.getContentType(),
                    sourceMessage.isArchive(),
                    sourceMessage.isCarbon());

            addErrorMessage(
                    displayName,
                    errorMsg);
        }

        @Override
        public void messageReceived(MessageReceivedEvent evt) {}
    }

    /**
     * Loads history messages ignoring the message with the specified id.
     */
    private void loadHistory()
    {
        SwingWorker historyWorker = new SwingWorker()
        {
            private Collection<Object> historyList;

            @Override
            public Object construct()
            {
                // If there are events (i.e. received IMs/file transfers) before we've even started
                // loading the history, then these were messages received that didn't cause the chat
                // window to be visible (because of a user setting to not open chat windows on
                // receiving IMs). Make sure we load enough messages from history to display all unseen
                // messages.
                int numNewEvents;
                synchronized(mIncomingEventBuffer)
                {
                    numNewEvents = mIncomingEventBuffer.size();
                }

                // Load the last N=CHAT_HISTORY_SIZE messages from history, or the number of newly
                // received messages if that's larger.
                historyList = mChatSession.getHistory(
                    Math.max(numNewEvents, ConfigurationUtils.getChatHistorySize()));

                return historyList;
            }

            /**
             * Called on the event dispatching thread (not on the worker thread)
             * after the <code>construct</code> method has returned.
             */
            @Override
            public void finished()
            {
                if (historyList != null && historyList.size() > 0)
                {
                    processHistory(historyList);
                }

                // Add incoming events accumulated while the history was loading
                // at the end of the chat.
                addIncomingEvents();
            }
        };

        historyWorker.start();
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
     * Schedules a task to reset the current chat transport to send to all
     * registered resources for this chat session.  This method also first
     * cancels the existing chat transport reset task, if one exists.
     */
    private void resetChatTransportTimer()
    {
        if (mChatTransportResetTask != null)
        {
            sLog.debug(
                    "Cancelling chat transport reset task on incoming message");
            mChatTransportResetTask.cancel();
        }

        mChatTransportResetTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (!(mChatSession.getCurrentChatTransport() instanceof SMSChatTransport))
                {
                    sLog.debug("Resetting chat transport as no incoming " +
                                 "message received (" + this + " : " +
                                 mChatSession.getChatName() + ")");
                    resetChatTransport();
                }
            }
        };

        long delay = mConfigService.global().getLong(
            CHAT_TRANSPORT_RESET_DELAY_PROP, CHAT_TRANSPORT_RESET_DEFAULT_DELAY);
        sLog.debug("Scheduling chat transport reset task in " + delay + "ms");
        mTimer.schedule(mChatTransportResetTask, delay);
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

        ReceiveFileConversationComponent component
            = new ReceiveFileConversationComponent(
                this, fileTransferOpSet, request, date);

        if (!mIsHistoryLoaded.get())
        {
            synchronized (mIncomingEventBuffer)
            {
                mIncomingEventBuffer.add(component);
            }
        }
        else
        {
            displayFileTransfer(component);
        }
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
                    sLog.info("Not adding " + participantAddress + " as they "
                                            + "are already in the chat room");
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
     * Sets the location of the split pane divider.
     *
     * @param location the location of the divider given by the pixel count
     * between the left bottom corner and the left bottom divider location
     */
    public void setDividerLocation(int location)
    {
        int dividerLocation = mMessagePane.getHeight() - location;

        mMessagePane.setDividerLocation(dividerLocation);
        mMessagePane.revalidate();
        mMessagePane.repaint();
    }

    /**
     * Returns the contained divider location.
     *
     * @return the contained divider location
     */
    public int getDividerLocation()
    {
        return mMessagePane.getHeight() - mMessagePane.getDividerLocation();
    }

    /**
     * Returns the contained divider size.
     *
     * @return the contained divider size
     */
    public int getDividerSize()
    {
        return mMessagePane.getDividerSize();
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
     * Adds all events accumulated in the incoming event buffer to the
     * chat conversation panel.
     */
    private void addIncomingEvents()
    {
        synchronized (mIncomingEventBuffer)
        {
            sLog.debug("Adding incoming events");
            Iterator<Object> eventBufferIter = mIncomingEventBuffer.iterator();

            while (eventBufferIter.hasNext())
            {
                Object incomingEvent = eventBufferIter.next();

                if (incomingEvent instanceof ChatMessage)
                {
                    displayChatMessage((ChatMessage) incomingEvent);
                }
                else if (incomingEvent instanceof FileTransferConversationComponent)
                {
                    displayFileTransfer(
                             (FileTransferConversationComponent)incomingEvent);
                }
            }

            // We've finished adding incoming events, which means we've also
            // finished loading history.  We set this flag here, rather than at
            // the end of the loadHistory method to be sure that no new events
            // are added to the chat panel while we are still processing
            // incoming events after we finish loading history.
            mIsHistoryLoaded.set(true);
        }
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

    /**
     * Gets the caret position in the chat editor.
     * @return index of caret in message being composed
     */
    @Override
    public int getCaretPosition()
    {
        return getChatWritePanel().getEditorPane().getCaretPosition();
    }

    /**
     * Causes the chat to validate its appearance (suggests a repaint operation
     * may be necessary).
     */
    @Override
    public void promptRepaint()
    {
        getChatWritePanel().getEditorPane().repaint();
    }

    @Override
    public void setVisible(boolean isVisible)
    {
        super.setVisible(isVisible);

        if (isVisible)
        {
            // Make sure the UI is displaying the correct connected state, as
            // we're making the UI visible.
            refreshUIConnectedState();

            // We're making the panel visible so, if we haven't already loaded
            // history, do so now.  We wait until we're making the panel
            // visible to load history, partly because there is no point in
            // loading it until then, but mostly because Swing may not render
            // the messages correctly if the panel is not visible when the
            // messages are added.
            if (!mIsHistoryLoaded.get())
            {
                sLog.debug("Loading history.");
                loadHistory();
            }
        }
    }

    /**
     * Reloads chat messages.
     */
    @Override
    public void loadSkin()
    {
        getChatConversationPanel().clear();
        loadHistory();
        getChatConversationPanel().setDefaultContent();
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
                                                           chatRoom.getIdentifier());
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
                                                           chatRoom.getIdentifier());
                if (mChatSession != null)
                {
                    ((ConferenceChatSession) mChatSession).initChatRoom();
                }
                else
                {
                    sLog.debug("Chat session is null - " +
                               "unable to initialize chat room : "  +
                                                           chatRoom.getIdentifier());
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
     * Sets a new message notification for a new message from the given contact
     * ID (IM address or SMS number).
     *
     * @param contactId The contact ID (IM address or SMS number) from which
     * we have received a new message.
     */
    private void setNewMessageNotification(String contactId)
    {
        sLog.debug("Setting new message notification from " + contactId);
        // We've received a new message so make sure we don't just refresh the
        // 'Recent' tab entry in place, as we want it to move to the top of the
        // list.
        setLastMessageReadStatus(false, false);
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
