/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.HTML.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.filetransfer.FileTransferConversationComponent;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.impl.protocol.jabber.JabberActivator.GroupMembershipAction;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChatConversationPanel</tt> is the panel, where all sent and received
 * messages appear. All data is stored in an HTML document. An external CSS file
 * is applied to the document to provide the look&feel. All smileys and link
 * strings are processed and finally replaced by corresponding images and HTML
 * links.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
public class ChatConversationPanel
    extends SIPCommScrollPane
    implements  HyperlinkListener,
                MouseListener,
                ClipboardOwner,
                Skinnable
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>ChatConversationPanel</tt> class and
     * its instances for logging output.
     */
    private static final Logger sLog
        = Logger.getLogger(ChatConversationPanel.class);

    /**
     * The path to the image file to use for the default avatar for contacts
     * with no specific avatar.
     */
    private static final String DEFAULT_AVATAR_PATH =
        GuiActivator.getResources().getImageURL(
            "service.gui.DEFAULT_USER_PHOTO_SMALL").toString();

    /**
     * A regular expression that matches a <div> tag and its contents.
     * The opening tag is group 1, and the tag contents is group 2 when
     * a match is found.
     */
    private static final Pattern DIV_PATTERN = ChatHtmlUtils.DIV_PATTERN;

    /**
     * The component rendering chat conversation panel text.
     */
    private final JTextPane mChatTextPane = new MyTextPane();

    /**
     * The editor kit used by the text component.
     */
    private final HTMLEditorKit mEditorKit;

    /**
     * The document used by the text component.
     */
    private HTMLDocument mDocument;

    /**
     * The parent container.
     */
    private final ChatConversationContainer mChatContainer;

    /**
     * The menu shown on right button mouse click.
     */
    private final ChatRightButtonMenu mRightButtonMenu;

    /**
     * The currently shown href.
     */
    private String mCurrentHref;

    /**
     * The timestamp of the last incoming message.
     */
    private Date mLastIncomingMsgTimestamp = new Date(0);

    /**
     * The timestamp of the last message.
     */
    private Date mLastMessageTimestamp = new Date(0);

    /**
     * The type of the last message.
     */
    private String mLastMessageType = null;

    /**
     * The indicator which determines whether an automatic scroll to the bottom
     * of {@link #mChatTextPane} is to be performed.
     */
    private boolean mScrollToBottomIsPending = false;

    /**
     * The UID of the most recently received message.
     */
    private String mLastMessageUID = null;

    /**
     * The UID of the last message received before the most recently received
     * message.  This is used to know which ID to append a message to if we are
     * asked to process a consecutive message more than once (e.g. by both the
     * ChatPanel and the HistoryWindow).
     */
    private String mPreviousMessageUID = null;

    /**
     * The UIDs of all messages that are currently being displayed in this chat
     * panel. This is used to ensure we don't display duplicate messages.
     * Access to this set is synchronized on scrollToBottomRunnable (matching
     * the synchronization of access to the HTML document).
     */
    private final Set<String> mCurrentlyDisplayedMessageUids = new HashSet<>();

    /**
     * The implementation of the routine which scrolls {@link #mChatTextPane} to its
     * bottom.
     */
    private final Runnable scrollToBottomRunnable = new Runnable()
    {
        /*
         * Implements Runnable#run().
         */
        public void run()
        {
            JScrollBar verticalScrollBar = getVerticalScrollBar();

            if (verticalScrollBar != null)
            {
                // We need to call both methods in order to be sure to scroll
                // to the bottom of the text even when the user has selected
                // something (changed the caret) or when a new tab has been
                // added or the window has been resized.
                verticalScrollBar.setValue(verticalScrollBar.getMaximum());
                mChatTextPane.setCaretPosition(mChatTextPane.getDocument().getLength());
            }
        }
    };

    /**
     * The accessible chat buffer for this chat session
     */
    private AccessibleChatBuffer mAccessibleChatBuffer;

    /**
     * Creates an instance of <tt>ChatConversationPanel</tt>.
     *
     * @param chatContainer The parent <tt>ChatConversationContainer</tt>.
     */
    public ChatConversationPanel(ChatConversationContainer chatContainer)
    {
        mEditorKit = new SIPCommHTMLEditorKit(this);

        mChatContainer = chatContainer;

        mRightButtonMenu = new ChatRightButtonMenu(this);

        mDocument = (HTMLDocument) mEditorKit.createDefaultDocument();

        mChatTextPane.setEditorKitForContentType("text/html", mEditorKit);
        mChatTextPane.setEditorKit(mEditorKit);
        mChatTextPane.setEditable(false);
        mChatTextPane.setDocument(mDocument);
        mChatTextPane.setDragEnabled(true);

        mAccessibleChatBuffer = chatContainer.getAccessibleChatBuffer();

        mChatTextPane.putClientProperty(
            JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        Constants.loadSimpleStyle(
            mDocument.getStyleSheet(), mChatTextPane.getFont());

        mChatTextPane.addHyperlinkListener(this);
        mChatTextPane.addMouseListener(this);

        setWheelScrollingEnabled(true);

        setViewportView(mChatTextPane);

        setBorder(null);

        setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        ToolTipManager.sharedInstance().registerComponent(mChatTextPane);

        /*
         * When we append a new message (regardless of whether it is a string or
         * an UI component), we want to make it visible in the viewport of this
         * JScrollPane so that the user can see it.
         */
        ComponentListener componentListener = new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                synchronized (scrollToBottomRunnable)
                {
                    if (!mScrollToBottomIsPending)
                        return;
                    mScrollToBottomIsPending = false;

                    /*
                     * Yana Stamcheva, pointed out that Java 5 (on Linux only?)
                     * needs invokeLater for JScrollBar.
                     */
                    SwingUtilities.invokeLater(scrollToBottomRunnable);
                }
            }
        };

        mChatTextPane.addComponentListener(componentListener);
        getViewport().addComponentListener(componentListener);
    }

    /**
     * Overrides Component#setBounds(int, int, int, int) in order to determine
     * whether an automatic scroll of #mChatTextPane to its bottom will be
     * necessary at a later time in order to keep its vertical scroll bar to its
     * bottom after the realization of the resize if it is at its bottom before
     * the resize.
     */
    @Override
    public void setBounds(int x, int y, int width, int height)
    {
        synchronized (scrollToBottomRunnable)
        {
            JScrollBar verticalScrollBar = getVerticalScrollBar();

            if (verticalScrollBar != null)
            {
                BoundedRangeModel verticalScrollBarModel
                    = verticalScrollBar.getModel();

                if ((verticalScrollBarModel.getValue()
                                + verticalScrollBarModel.getExtent()
                            >= verticalScrollBarModel.getMaximum())
                        || !verticalScrollBar.isVisible())
                    mScrollToBottomIsPending = true;
            }
        }

        super.setBounds(x, y, width, height);
    }

    /**
     * Retrieves the contents of the sent message with the given ID.
     *
     * @param messageUID The ID of the message to retrieve.
     * @return The contents of the message, or null if the message is not found.
     */
    public String getMessageContents(String messageUID)
    {
        synchronized (scrollToBottomRunnable)
        {
            Element root = mDocument.getDefaultRootElement();
            Element e = mDocument.getElement(
                root,
                Attribute.ID,
                ChatHtmlUtils.MESSAGE_TEXT_ID + messageUID);

            if (e == null)
            {
                sLog.warn("Could not find message with ID " + messageUID);
                return null;
            }

            Object original_message = e.getAttributes().getAttribute(
                    ChatHtmlUtils.ORIGINAL_MESSAGE_ATTRIBUTE);
            if (original_message == null)
            {
                sLog.warn("Message with ID " + messageUID +
                        " does not have original_message attribute");
                return null;
            }

            return original_message.toString();
        }
    }

    /**
     * Convert the new format of to a human-readable translated string.
     * The old format was a pre-translated string, the new format is made
     * up of 2 or 3 parts, separated by ",":
     * 1. Status type (Join, leave, change subject)
     * 2. Triggerer
     * 3. (Optional) subject (the value the subject was changed to)
     *
     * @param message The status message content to convert
     * @param messageAuthor The address of the send of the message
     * @param provider The protocol provider service
     */
    private String convertStatusMessageContent(String message,
                                               String messageAuthor,
                                               ProtocolProviderService provider)
    {
        String[] splitContent = message.split(",");
        if (splitContent.length < 2)
        {
            // Old style status message, just return it straight back
            return message;
        }
        // New style status message, convert to translated human-readable
        // string.
        String localAddress = provider.getAccountID().getAccountAddress();
        boolean isSentMessage = messageAuthor.equalsIgnoreCase(localAddress);
        if (splitContent[0].toString().equals(GroupMembershipAction.joined.toString()))
        {
            message = isSentMessage ? "service.gui.CHAT_ROOM_USER_JOINED" :
                        "service.gui.CHAT_ROOM_OTHER_USER_JOINED";
        }
        else if (splitContent[0].toString().equals(GroupMembershipAction.left.toString()))
        {
            message = isSentMessage ? "service.gui.CHAT_ROOM_USER_LEFT" :
                        "service.gui.CHAT_ROOM_OTHER_USER_LEFT";
        }
        else if (splitContent[0].toString().equals(GroupMembershipAction.banned.toString()))
        {
            message = isSentMessage ? "service.gui.CHAT_ROOM_USER_REMOVED_YOU" :
                            localAddress.equalsIgnoreCase(splitContent[1]) ?
                            "service.gui.CHAT_ROOM_YOU_REMOVED_OTHER_USER" :
                            "service.gui.CHAT_ROOM_OTHER_USER_REMOVED";
        }
        else if (splitContent[0].toString().equals(MessageHistoryService.SUBJECT))
        {
            message = isSentMessage ? "service.gui.CHAT_ROOM_USER_SUBJECT_CHANGED" :
                        "service.gui.CHAT_ROOM_OTHER_USER_SUBJECT_CHANGED";
            String subject = null;
            if (splitContent.length == 3)
            {
                // Subject with no commas in
                subject = splitContent[2];
            }
            else
            {
                // Subject contains commas, join all parts of the subject
                subject = String.join(",", Arrays.copyOfRange(splitContent,
                                                              2,
                                                              splitContent.length));
            }
            return GuiActivator.getResources().getI18NString(message, new String[]{subject});
        }
        return GuiActivator.getResources().getI18NString(message, new String[]{splitContent[1]});
    }

    /**
     * Processes the message given by the parameters.  Synchronized as we want
     * to make sure we've finished processing one message before we start
     * processing the next.
     *
     * @param chatMessage the message
     * @param keyword a substring of <tt>chatMessage</tt> to be highlighted upon
     * display of <tt>chatMessage</tt> in the UI
     * @param protocolProvider the protocol provider
     * @param contactJid the contact's JID (IM address)
     * @return the processed message
     */
    public synchronized String processMessage(ChatMessage chatMessage,
                                              String keyword,
                                              ProtocolProviderService protocolProvider,
                                              String contactJid)
    {
        String messageUID = chatMessage.getMessageUID();
        Date messageTimestamp = chatMessage.getDate();
        boolean isCarbon = chatMessage.isCarbon();
        boolean isArchive = chatMessage.isArchive();
        sLog.debug("Processing message: " + messageUID);

        // When the chat window is opened, only the most recent few historical
        // messages are displayed.  If the user has had some chat
        // conversations on other clients, we may receive carbons or archive
        // messages from those conversations going back further than that.
        // Therefore, we only display carbons/archive messages here if they
        // were sent more recently than the latest message that we have
        // already added to the chat window.  This is important because,
        // otherwise, they would end up being added into the window after the
        // most recent X messages have been loaded from history so they would
        // appear out of order.  These messages are still saved in history, so
        // the user can view them at any time by opening the chat history
        // window.
        if ((mLastMessageTimestamp.compareTo(messageTimestamp) > 0) &&
            (isCarbon || isArchive))
        {
            sLog.debug("Not displaying old carbon/archive message as its timestamp (" +
                       messageTimestamp + ") is older than the latest message displayed (" +
                       mLastMessageTimestamp + "). ID = " + messageUID +
                       ", isCarbon? " + isCarbon + ", isArchive? " + isArchive);
            return null;
        }

        // If the message contains no text it is probably an error report for a
        // previously sent message.  Try to find the previous message in history
        // so we can display its text and timestamp along with the error.
        if (chatMessage.getMessage() == null)
        {
            MessageHistoryService msgHistoryService =
                                        GuiActivator.getMessageHistoryService();
            MessageEvent messageEvent =
                         msgHistoryService.findByXmppId(contactJid, messageUID);
            if (messageEvent != null)
            {
                sLog.debug("Found message in history for id " + messageUID);
                chatMessage.setMessage(messageEvent.getSourceMessage().getContent());
                chatMessage.setDate(messageEvent.getTimestamp());
            }
            else
            {
                sLog.warn(
                    "Unable to find message in history with id " + messageUID);
            }
        }

        String contactAddress = chatMessage.getContactAddress();

        // If there is no contact address, this will just be a dummy history
        // message so return null so we don't display it.
        if (contactAddress == null)
        {
            return null;
        }

        String contentType = chatMessage.getContentType();

        // remove rogue font sizes as passed from Commportal Communicator
        if ("text/html".equals(contentType))
        {
            chatMessage.setMessage(
                chatMessage.getMessage().replaceAll(
                    "font size='\\d'", "font size='" +
                    ScaleUtils.scaleInt(5) + "'") );
        }

        // Add this message to the accessible chat buffer
        if (mAccessibleChatBuffer == null)
        {
            mAccessibleChatBuffer = getChatContainer().getAccessibleChatBuffer();
        }

        if (mAccessibleChatBuffer != null)
        {
            mAccessibleChatBuffer.addMessage(
                    chatMessage.getContactDisplayName(),
                    chatMessage.getMessage(),
                    ChatHtmlUtils.getDateAndTimeString(chatMessage.getDate()));
        }

        // Check whether we should append this message to the previous one.
        boolean shouldAppendMessage = ((mLastMessageUID != null) &&
            isConsecutiveMessage(
                chatMessage, mLastMessageUID, mLastMessageTimestamp, mLastMessageType));

        String messageType = chatMessage.getMessageType();

        // If the history window is open when we receive a message, we will
        // process it twice - once for the main chat window and once for
        // the history window.  Therefore, we only want to update the
        // mLastMessageUID and mPreviousMessageUID the first time we process
        // a new message, otherwise we would append to the wrong message.
        if ((messageUID == null) ||
            (mLastMessageUID == null) ||
            !messageUID.equals(mLastMessageUID))
        {
            sLog.debug("About to replace last message: " + mLastMessageUID +
                ", " + mLastMessageTimestamp + ", " + mLastMessageType +
                " with: " + messageUID + ", " + messageTimestamp + ", " + messageType);
            mPreviousMessageUID = mLastMessageUID;
            mLastMessageUID = messageUID;
            mLastMessageTimestamp = messageTimestamp;
            mLastMessageType = messageType;
        }

        // If this is a consecutive message don't go through the initiation
        // and just append it.
        if (shouldAppendMessage)
        {
            appendConsecutiveMessage(
                chatMessage, keyword, contentType, mPreviousMessageUID);
            return null;
        }

        String contactDisplayName = chatMessage.getContactDisplayName();
        if (contactDisplayName == null
                || contactDisplayName.trim().length() <= 0)
        {
            contactDisplayName = contactAddress;
        }
        else
        {
            // for some reason &apos; is not rendered correctly from our ui,
            // lets use its equivalent. Also replace < and > to prevent HTML injection.
            contactDisplayName
                = contactDisplayName.replaceAll("&apos;", "&#39;")
                                    .replaceAll("<", "&lt;")
                                    .replaceAll(">", "&gt;");
        }

        String messageTitle = chatMessage.getMessageTitle();
        String message = chatMessage.getMessage();
        String errorMessage = chatMessage.getErrorMessage();
        String chatString = "";
        String endHeaderTag = "";

        String startSystemDivTag
            = "<DIV id=\"systemMessage\" style=\"color:#627EB7;\">";
        String endDivTag = "</DIV>";

        String startPlainTextTag
            = ChatHtmlUtils.createStartPlainTextTag(contentType);
        String endPlainTextTag
            = ChatHtmlUtils.createEndPlainTextTag(contentType);

        switch (messageType)
        {
            case Chat.INCOMING_MESSAGE:
                mLastIncomingMsgTimestamp = new Date();

                chatString = ChatHtmlUtils.createIncomingMessageTag(
                        mLastMessageUID,
                        contactAddress,
                        contactDisplayName,
                        getContactAvatar(protocolProvider, contactAddress),
                        messageTimestamp,
                        ChatHtmlUtils.formatMessage(message,
                                                    contentType,
                                                    keyword),
                        contentType,
                        messageType,
                        isGroupChat());
                break;
            case Chat.OUTGOING_MESSAGE:
                chatString = ChatHtmlUtils.createOutgoingMessageTag(
                        mLastMessageUID,
                        contactAddress,
                        messageTimestamp,
                        ChatHtmlUtils.formatMessage(message,
                                                    contentType,
                                                    keyword),
                        contentType,
                        messageType,
                        errorMessage);
                break;
            case Chat.INCOMING_SMS_MESSAGE:
                chatString = ChatHtmlUtils.createIncomingMessageTag(
                        mLastMessageUID,
                        contactAddress,
                        contactDisplayName,
                        getContactAvatar(protocolProvider, contactAddress),
                        messageTimestamp,
                        ChatHtmlUtils.formatMessage(message,
                                                    contentType,
                                                    keyword),
                        contentType,
                        messageType,
                        false);
                break;
            case Chat.OUTGOING_SMS_MESSAGE:
                chatString = ChatHtmlUtils.createOutgoingMessageTag(
                        mLastMessageUID,
                        contactAddress,
                        messageTimestamp,
                        ChatHtmlUtils.formatMessage(message,
                                                    contentType,
                                                    keyword),
                        contentType,
                        messageType,
                        errorMessage);
                break;
            case Chat.STATUS_MESSAGE:
                chatString = "<h4 style=\"font-size:" +
                        ScaleUtils.getMediumFontSize() +
                        "pt\" id=\"" + mLastMessageUID + "\" date=\"" +
                        messageTimestamp + "\">";
                endHeaderTag = "</h4>";
                String formattedMessage = String.format(convertStatusMessageContent(message,
                                                                                    contactAddress,
                                                                                    protocolProvider),
                                                        contactDisplayName);

                chatString += formattedMessage + endHeaderTag;
                break;
            case Chat.ACTION_MESSAGE:
                chatString = "<p id=\"actionMessage\" date=\""
                        + messageTimestamp + "\">";
                endHeaderTag = "</p>";

                chatString += "* " + GuiUtils.formatTime(messageTimestamp)
                        + " " + contactDisplayName + " "
                        + message
                        + endHeaderTag;
                break;
            case Chat.SYSTEM_MESSAGE:
                chatString
                        += startSystemDivTag + startPlainTextTag
                        + ChatHtmlUtils.formatMessage(message,
                                                      contentType,
                                                      keyword)
                        + endPlainTextTag + endDivTag;
                break;
            case Chat.ERROR_MESSAGE:
                chatString = "<h6 id=\""
                        + ChatHtmlUtils.MESSAGE_HEADER_ID
                        + "\" date=\""
                        + messageTimestamp + "\">";

                endHeaderTag = "</h6>";

                String errorIcon = "<IMG SRC='"
                        + GuiActivator.getImageLoaderService().getImageUri(
                        ImageLoaderService.EXCLAMATION_MARK)
                        + "' </IMG>";

                chatString += errorIcon
                        + messageTitle
                        + endHeaderTag + "<h5>" + message + "</h5>";
                break;
        }

        return chatString;
    }

    /**
     * Return whether this is a group chat
     *
     * @return whether this is a group chat
     */
    private boolean isGroupChat()
    {
        return mChatContainer.getChatType().equals(
            ChatConversationContainer.ChatType.GROUP_CHAT);
    }

    /**
     * Processes the message given by the parameters.
     *
     * @param chatMessage the message.
     * @return the formatted message
     */
    public String processMessage(ChatMessage chatMessage,
                                 ProtocolProviderService protocolProvider,
                                 String contactAddress)
    {
        return processMessage(chatMessage,
                              null,
                              protocolProvider,
                              contactAddress);
    }

    /**
     * Appends a consecutive message to the document.
     *
     * @param chatMessage the message to append
     * @param keyword the keyword
     * @param contentType the content type of the message
     * @param previousUID the UID of the message that the new message should be
     * appended to
     */
    public void appendConsecutiveMessage(final ChatMessage chatMessage,
                                         final String keyword,
                                         final String contentType,
                                         final String previousUID)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    appendConsecutiveMessage(chatMessage,
                                             keyword,
                                             contentType,
                                             previousUID);
                }
            });
            return;
        }

        String messageUID = chatMessage.getMessageUID();
        sLog.debug("Appending consecutive message UID: " + messageUID);

        String thisMsgAddress = chatMessage.getContactAddress();
        String newMessage = ChatHtmlUtils.createMessageTag(
                                    messageUID,
                                    thisMsgAddress,
                                    ChatHtmlUtils.formatMessage(chatMessage.getMessage(),
                                        contentType,
                                        keyword),
                                    contentType,
                                    chatMessage.getDate(),
                                    false);

        synchronized (scrollToBottomRunnable)
        {
            Element lastMsgElement = mDocument.getElement(
                ChatHtmlUtils.MESSAGE_TEXT_ID + previousUID);

            if (lastMsgElement == null)
            {
                sLog.warn("Unable to find last message with ID " +
                                ChatHtmlUtils.MESSAGE_TEXT_ID + previousUID);
                return;
            }

            String lastMsgAddress =
                (String) lastMsgElement.getAttributes().getAttribute(Attribute.NAME);
            boolean duplicateId = isDuplicateUid(messageUID);

            if (lastMsgAddress == null ||
                !lastMsgAddress.equals(thisMsgAddress) ||
                duplicateId)
            {
                // This really shouldn't happen, so log at warning level. If it
                // does, it probably means we've been given a duplicate message,
                // so the safest thing to do is just to ignore the message.
                sLog.warn(
                    "Ignoring request to append message from " + thisMsgAddress +
                    " to messages from " + lastMsgAddress +
                    ". Duplicate ID? " + duplicateId);
                return;
            }

            try
            {
                Element parentElement = lastMsgElement.getParentElement();
                mDocument.insertBeforeEnd(parentElement, newMessage);

                // Need to call explicitly scrollToBottom, because for some
                // reason the componentResized event isn't fired every time
                // we add text.
                SwingUtilities.invokeLater(scrollToBottomRunnable);
            }
            catch (BadLocationException | IOException ex)
            {
                sLog.error("Could not replace chat message", ex);
            }
        }

        finishMessageAdd(newMessage, contentType, messageUID);
    }

    /**
     * Check whether we have already added an element to the UI with the given
     * ID.  Will return false if the ID is null, as we allow multiple UI
     * elements with a null ID, as they should just be formatting/padding,
     * rather than messages themselves.
     *
     * @param uid the ID to check
     *
     * @return true if we have already displayed a UI element with the given
     * ID, false otherwise.
     */
    public boolean isDuplicateUid(String uid)
    {
        boolean isDuplicate = false;

        if (uid != null)
        {
            synchronized (mCurrentlyDisplayedMessageUids)
            {
                isDuplicate = mCurrentlyDisplayedMessageUids.contains(uid);

                if (!isDuplicate)
                {
                    mCurrentlyDisplayedMessageUids.add(uid);
                }
            }
        }

        return isDuplicate;
    }

    /**
     * Replaces the contents of the message with ID of the corrected message
     * specified in chatMessage with this message and/or adds the error message
     * specified in chatMessage to the message with the ID of the failed message
     * specified in chatMessage.
     *
     * @param chatMessage A <tt>ChatMessage</tt> that contains all the required
     * information to update the old message.
     */
    public void updateMessage(final ChatMessage chatMessage)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateMessage(chatMessage);
                }
            });
            return;
        }

        String correctedUID = chatMessage.getCorrectedMessageUID();
        String failedUID = chatMessage.getFailedMessageUID();
        String messageUID = chatMessage.getMessageUID();
        String newMessage = null;

        synchronized (scrollToBottomRunnable)
        {
            Element root = mDocument.getDefaultRootElement();

            Element correctedMsgElement = null;
            if (correctedUID != null)
            {
                sLog.debug("Trying to correct messaage with UID " + correctedUID);
                correctedMsgElement =
                    mDocument.getElement(root,
                                         Attribute.ID,
                                         ChatHtmlUtils.MESSAGE_TEXT_ID + correctedUID);

                if (correctedMsgElement != null)
                {
                    String contactAddress
                        = (String) correctedMsgElement.getAttributes()
                            .getAttribute(Attribute.NAME);

                    newMessage = ChatHtmlUtils.createMessageTag(
                        messageUID,
                        contactAddress,
                        ChatHtmlUtils.formatMessage(chatMessage.getMessage(),
                                      chatMessage.getContentType(),
                                      ""),
                        chatMessage.getContentType(),
                        chatMessage.getDate(),
                        true);
                }
                else
                {
                    sLog.warn("Could not find message with ID " + correctedUID +
                                                             " for correction");
                }
            }

            Element errorMsgElement = null;
            String errorMessage = null;
            if (failedUID != null)
            {
                sLog.debug(
                    "Trying to mark as failed message with UID " + failedUID);
                errorMsgElement =
                    mDocument.getElement(root,
                                         Attribute.ID,
                                         ChatHtmlUtils.ERROR_MESSAGE_ID + failedUID);

                if (errorMsgElement != null)
                {
                    String errorText = chatMessage.getErrorMessage();

                    if ((errorText != null) && !(errorText.trim().isEmpty()))
                    {
                        errorMessage =
                            ChatHtmlUtils.createMessageErrorTag(failedUID, errorText);
                    }
                    else
                    {
                        sLog.warn("No error message provided for ID " + failedUID);
                    }
                }
                else
                {
                    sLog.warn("Could not find message with ID " + failedUID +
                                                          " to mark as failed");
                }
            }

            try
            {
                // For corrected messages, we actually write a brand new
                // message to history with a new message ID.  Therefore, only
                // actually add this message to the document if a message with
                // this ID isn't already displayed.  For error messages, we
                // update an existing message without changing its ID, so
                // there's no need to check the list of currently displayed IDs
                // or to add to the list in that case.
                if ((correctedMsgElement != null) &&
                    (newMessage != null) && !isDuplicateUid(messageUID))
                {
                    mDocument.setOuterHTML(correctedMsgElement, newMessage);
                }

                if ((errorMsgElement != null) && (errorMessage != null))
                {
                    mDocument.setOuterHTML(errorMsgElement, errorMessage);
                }

                // Need to call explicitly scrollToBottom, because for some
                // reason the componentResized event isn't fired every time
                // we add text.
                SwingUtilities.invokeLater(scrollToBottomRunnable);
            }
            catch (BadLocationException | IOException ex)
            {
                sLog.error("Could not replace chat message", ex);
            }
        }

        if (newMessage != null)
        {
            finishMessageAdd(
                newMessage, chatMessage.getContentType(), messageUID);
        }
    }

    /**
     * Appends the given string to the end of the latest message contained in
     * this panel.
     *
     * @param message the message string to append
     * @param contentType the content type of the message
     */
    public void appendMessageToEnd(final String message,
                                   final String contentType)
    {
        appendMessageToEnd(message, contentType, mLastMessageUID);
    }

    /**
     * Appends the given string to the end of the message with the given ID
     * contained in this panel.
     *
     * @param message the message string to append
     * @param contentType the content type of the message
     * @param messageUID the UID of the message to append this one to.
     */
    public void appendMessageToEnd(final String message,
                                   final String contentType,
                                   final String messageUID)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    appendMessageToEnd(message, contentType, messageUID);
                }
            });
            return;
        }

        if (message == null)
            return;

        synchronized (scrollToBottomRunnable)
        {
            Element root = mDocument.getDefaultRootElement();
            Element element = mDocument.getElement(
                                    root,
                                    Attribute.ID,
                                    ChatHtmlUtils.MESSAGE_TEXT_ID + messageUID);

            // It's possible that a new message has already been loaded from
            // history if it was received before we made the chat panel
            // visible.  Therefore, ignore this message if a message with a
            // matching UID is already in the conversation panel.
            if (element != null || isDuplicateUid(messageUID))
            {
                sLog.debug("Ignoring duplicate message: " + messageUID +
                                                      ", element = " + element);
                return;
            }

            try
            {
                mDocument.insertBeforeEnd(
                            // the body element
                            root.getElement(root.getElementCount() - 1),
                            // the message to insert
                            message);

                // Need to call explicitly scrollToBottom, because for some
                // reason the componentResized event isn't fired every time we
                // add text.
                SwingUtilities.invokeLater(scrollToBottomRunnable);
            }
            catch (BadLocationException | IOException e)
            {
                sLog.error("Insert in the HTMLDocument failed.", e);
            }
        }

        String lastElemContent = getElementContent(messageUID, message);

        if (lastElemContent != null)
            finishMessageAdd(
                getElementContent(messageUID, message),
                contentType,
                messageUID);
    }

    /**
     * Performs all operations needed in order to finish the adding of the
     * message to the document.
     *
     * @param message the message string
     * @param contentType the content type of the message string
     * @param messageUID the UID of the message to finish adding
     */
    private void finishMessageAdd(String message,
                                  String contentType,
                                  String messageUID)
    {
        /*
         * Replacements will be processed only on text in <PLAINTEXT> tags
         */
        if (contentType != ChatHtmlUtils.TEXT_CONTENT_TYPE)
        {
            return;
        }

        /*
         * Replacements will be processed only if it is enabled in the
         * property.
         */
        ConfigurationService cfg = GuiActivator.getConfigurationService();

        if (cfg.user().getBoolean(ReplacementProperty.REPLACEMENT_ENABLE, true)
                || cfg.user().getBoolean(
                        ReplacementProperty.getPropertyName("SMILEY"),
                        true))
        {
            processReplacement(ChatHtmlUtils.MESSAGE_TEXT_ID + messageUID,
                               message,
                               contentType);
        }
    }

    /**
    * Formats the given message. Processes the messages and replaces links to
    * video/image sources with their previews or any other substitution. Spawns
    * a separate thread for replacement.
    *
    * @param messageID the id of the message element in the HTML Document.
    * @param chatString the message.
    */
    private void processReplacement(final String messageID,
                                    final String chatString,
                                    final String contentType)
    {
        SwingWorker worker = new SwingWorker()
        {
            /**
             * Called on the event dispatching thread (not on the worker thread)
             * after the <code>construct</code> method has returned.
             */
            public void finished()
            {
                String newMessage = (String) get();

                if (newMessage != null && !newMessage.equals(chatString))
                {
                    synchronized (scrollToBottomRunnable)
                    {
                        mScrollToBottomIsPending = true;

                        try
                        {
                            Element elem = mDocument.getElement(messageID);
                            mDocument.setOuterHTML(elem, newMessage);
                        }
                        catch (BadLocationException | IOException ex)
                        {
                            sLog.error("Could not replace chat message", ex);
                        }
                    }
                }
            }

            public Object construct()
            {
                Matcher divMatcher = DIV_PATTERN.matcher(chatString);
                String openingTag = "";
                String msgStore = chatString;
                String closingTag = "";
                if (divMatcher.find())
                {
                    openingTag = divMatcher.group(1);
                    msgStore = divMatcher.group(2);
                    closingTag = divMatcher.group(3);
                }

                // The msgStore will contain either plain text, or mixture of
                // plain text and other text.  We only want to add replacement
                // media (e.g. emoji or emoticon images) to the plain text.
                // Therefore, split the msgStore into plain- text parts and
                // non-plain-text parts and perform replacements correctly.
                StringBuilder message = new StringBuilder(openingTag);

                for (String textPart : msgStore.split(ChatHtmlUtils.END_PLAINTEXT_TAG))
                {
                    // We should perform replacements on only the <PLAINTEXT>
                    // parts. The performRaplcementsOnString method is
                    // responsible for re-adding any Plaintext tags around
                    // bits of the message to be displayed as text.
                    String[] subParts = textPart.split(ChatHtmlUtils.START_PLAINTEXT_TAG);
                    if (subParts.length == 2)
                    {
                        message.append(subParts[0]).append(ChatHtmlUtils.performReplacementsOnString(subParts[1]));
                    }
                    else if (subParts.length > 0)
                    {
                        // No terminator - we are outside a plaintext block.
                        message.append(subParts[0]);
                    }
                }
                return message.append(closingTag).toString();
            }
        };
        worker.start();
    }

    /**
     * Opens a link in the default browser when clicked and shows link url in a
     * popup on mouseover.
     *
     * @param e The HyperlinkEvent.
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED)
        {
            String href = e.getDescription();

            mCurrentHref = href;
        }
        else if (e.getEventType() == HyperlinkEvent.EventType.EXITED)
        {
            mCurrentHref = "";
        }
    }

    /**
     * Returns the text pane of this conversation panel.
     *
     * @return The text pane of this conversation panel.
     */
    public JTextPane getChatTextPane()
    {
        return mChatTextPane;
    }

    /**
     * Returns the time of the last received message.
     *
     * @return The time of the last received message.
     */
    public Date getLastIncomingMsgTimestamp()
    {
        return mLastIncomingMsgTimestamp;
    }

    /**
     * When a right button click is performed in the editor pane, a popup menu
     * is opened.
     * In case of the Scheme being internal, it won't open the Browser but
     * instead it will trigger the forwarded action.
     *
     * @param e The MouseEvent.
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        Point p = e.getPoint();
        SwingUtilities.convertPointToScreen(p, e.getComponent());

        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (e.isControlDown() && !e.isMetaDown()))
        {
            openContextMenu(p);
        }
        else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0
            && mCurrentHref != null && mCurrentHref.length() != 0)
        {
            URI uri;
            try
            {
                uri = new URI(mCurrentHref);
            }
            catch (URISyntaxException e1)
            {
                sLog.error("Invalid URL", e1);
                return;
            }
            if (!"jitsi".equals(uri.getScheme()))
            {
                GuiActivator.getBrowserLauncher().openURL(mCurrentHref);
            }

            // After opening the link remove the mCurrentHref to avoid
            // clicking on the window to gain focus to open the link again.
            mCurrentHref = "";
        }
    }

    /**
     * Opens this panel context menu at the given point.
     *
     * @param p the point where to position the left-top cornet of the context
     *            menu
     */
    private void openContextMenu(Point p)
    {
        if (mChatTextPane.getSelectedText() != null)
        {
            mRightButtonMenu.enableCopy();
        }
        else
        {
            mRightButtonMenu.disableCopy();
        }
        mRightButtonMenu.setInvoker(mChatTextPane);
        mRightButtonMenu.setLocation(p.x, p.y);
        mRightButtonMenu.setVisible(true);
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {}

    /**
     * Returns the chat container.
     *
     * @return the chat container
     */
    public ChatConversationContainer getChatContainer()
    {
        return mChatContainer;
    }

    /**
     * Copies the selected conversation panel content to the clipboard.
     */
    public void copyConversation()
    {
        mChatTextPane.copy();
    }

    /**
     * Creates new document and all the messages that will be processed in the
     * future will be appended in it.
     */
    public void clear()
    {
        synchronized (scrollToBottomRunnable)
        {
            mDocument = (HTMLDocument) mEditorKit.createDefaultDocument();
            Constants.loadSimpleStyle(
                mDocument.getStyleSheet(), mChatTextPane.getFont());
        }
    }

    /**
     * Sets the given document to the editor pane in this panel.
     *
     * @param document the document to set
     */
    public void setContent(final HTMLDocument document)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setContent(document);
                }
            });
            return;
        }

        synchronized (scrollToBottomRunnable)
        {
            mScrollToBottomIsPending = true;

            mDocument = document;
            mChatTextPane.setDocument(mDocument);
        }
    }

    /**
     * Sets the default document contained in this panel, created on init or
     * when clear is invoked.
     */
    public void setDefaultContent()
    {
        synchronized (scrollToBottomRunnable)
        {
            setContent(mDocument);
        }
    }

    /**
     * Returns the document contained in this panel.
     *
     * @return the document contained in this panel
     */
    public HTMLDocument getContent()
    {
        return (HTMLDocument) mChatTextPane.getDocument();
    }

    /**
     * Returns the right button popup menu.
     *
     * @return the right button popup menu
     */
    public ChatRightButtonMenu getRightButtonMenu()
    {
        return mRightButtonMenu;
    }

    /**
     * Extend Editor pane to add URL tooltips.
     */
    private class MyTextPane
        extends JTextPane
    {
        private static final long serialVersionUID = 0L;

        /**
         * Returns the string to be used as the tooltip for <i>event</i>.
         *
         * @param event the <tt>MouseEvent</tt>
         * @return the string to be used as the tooltip for <i>event</i>.
         */
        @Override
        public String getToolTipText(MouseEvent event)
        {
            return
                ((mCurrentHref != null) && (mCurrentHref.length() != 0))
                    ? mCurrentHref
                    : null;
        }
    }

    /**
     * Adds a custom component at the end of the conversation.
     *
     * @param component the component to add at the end of the conversation.
     */
    public void addComponent(FileTransferConversationComponent component)
    {
        synchronized (scrollToBottomRunnable)
        {
            StyleSheet styleSheet = mDocument.getStyleSheet();
            Style style
                = styleSheet
                    .addStyle(
                        StyleConstants.ComponentElementName,
                        styleSheet.getStyle("body"));

            // The image must first be wrapped in a style
            style
                .addAttribute(
                    AbstractDocument.ElementNameAttribute,
                    StyleConstants.ComponentElementName);

            TransparentPanel wrapPanel
                = new TransparentPanel(new BorderLayout());

            wrapPanel.add(component, BorderLayout.NORTH);

            style.addAttribute(StyleConstants.ComponentAttribute, wrapPanel);
            style.addAttribute(Attribute.ID,
                               ChatHtmlUtils.MESSAGE_TEXT_ID + component.getId());
            SimpleDateFormat sdf
                = new SimpleDateFormat(ChatHtmlUtils.DATE_FORMAT);
            style.addAttribute(ChatHtmlUtils.DATE_ATTRIBUTE,
                                sdf.format(component.getDate()));

            mScrollToBottomIsPending = true;

            // We need to reinitialize the last message ID, because we don't
            // want components to be taken into account.
            mLastMessageUID = null;

            // Insert the component style at the end of the text
            // Put a <br> element before and after first though so that there
            // is a bit of padding surrounding the component
            try
            {
                appendMessageToEnd("<br>", "text/html", mLastMessageUID);
                mDocument
                    .insertString(mDocument.getLength(), "ignored text", style);
                appendMessageToEnd("<br>", "text/html", mLastMessageUID);
            }
            catch (BadLocationException e)
            {
                sLog.error("Insert in the HTMLDocument failed.", e);
            }
        }
    }

    /**
     * Reloads images.
     */
    public void loadSkin()
    {
        getRightButtonMenu().loadSkin();
    }

    /**
     * Returns the avatar corresponding to the account of the given
     * <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the protocol provider service, which account
     * avatar we're looking for
     * @param contactAddress
     * @return the avatar corresponding to the account of the given
     * <tt>protocolProvider</tt>
     */
    private static String getContactAvatar(
                                    ProtocolProviderService protocolProvider,
                                    String contactAddress)
    {
        // Return the path to the default avatar file if we don't find a
        // specific avatar for this contact.
        String avatarPath = DEFAULT_AVATAR_PATH;

        if (contactAddress == null)
        {
            sLog.warn("Asked to get avatar for null contact address");
        }
        else
        {
            if (protocolProvider == null)
            {
                sLog.warn(
                    "Asked to get avatar for null protocol provider for " +
                    logHasher(contactAddress));

                // The protocol provider sometimes can be null if we hit a race
                // condition on start-up, therefore see if we can get a
                // reference to it now.
                protocolProvider = AccountUtils.getImProvider();
            }

            if (protocolProvider != null)
            {
                File avatarFile = AvatarCacheUtils.getCachedAvatarFile(
                    protocolProvider, contactAddress);

                if (avatarFile.exists() && avatarFile.length() > 0)
                {
                    avatarPath = "file:" + avatarFile.getAbsolutePath();
                }
            }
        }

        return avatarPath;
    }

    /**
     * Returns true if this is a consecutive message.
     *
     * @param chatMessage the message to verify
     * @param lastMsgUID the ID of the message sent/received before the one to
     * verify
     * @param lastMsgTimestamp the timestamp of the message sent/received
     * before the one to verify
     * @param lastMsgType the type of the message sent/received before the one
     * to verify
     * @return <tt>true</tt> if the given message is a consecutive message,
     * <tt>false</tt> - otherwise
     */
    private boolean isConsecutiveMessage(ChatMessage chatMessage,
                                         String lastMsgUID,
                                         Date lastMsgTimestamp,
                                         String lastMsgType)
    {
        // Always treat failed messages and SMSs as separate messages
        String messageType = chatMessage.getMessageType();
        if (lastMsgUID == null || chatMessage.getFailedMessageUID() != null ||
            messageType.equals(Chat.INCOMING_SMS_MESSAGE) ||
            messageType.equals(Chat.OUTGOING_SMS_MESSAGE) ||
            messageType.equals(Chat.ERROR_MESSAGE) ||
            lastMsgType.equals(Chat.INCOMING_SMS_MESSAGE) ||
            lastMsgType.equals(Chat.OUTGOING_SMS_MESSAGE) ||
            lastMsgType.equals(Chat.ERROR_MESSAGE))
        {
            return false;
        }

        Element lastMsgElement;

        synchronized (scrollToBottomRunnable)
        {
            lastMsgElement = mDocument.getElement(
                ChatHtmlUtils.MESSAGE_TEXT_ID + lastMsgUID);
        }

        if (lastMsgElement == null)
        {
            sLog.warn("Could not find message with ID " + lastMsgUID);
            return false;
        }

        AttributeSet attributes = lastMsgElement.getAttributes();
        String contactAddress
            = (String) attributes
                .getAttribute(Attribute.NAME);
        Date msgDate = chatMessage.getDate();

        if (contactAddress != null
                && (messageType.equals(Chat.INCOMING_MESSAGE)
                    || messageType.equals(Chat.OUTGOING_MESSAGE))
                && contactAddress.equals(chatMessage.getContactAddress())
                // And if the new message is within a minute from the last one.
                && ((msgDate.getTime()
                    - lastMsgTimestamp.getTime())
                        < 60000))
        {
            sLog.debug(
                "Found consecutive message UID: " + chatMessage.getMessageUID() +
                ", following last message UID: " + lastMsgUID);
            return true;
        }

        return false;
    }

    /**
     * Releases the resources allocated by this instance throughout its lifetime
     * and prepares it for garbage collection.
     */
    public void dispose()
    {
        super.dispose();

        clear();
    }

    /**
     *
     * @param elementId
     * @param message
     * @return
     */
    private String getElementContent(String elementId, String message)
    {
        Pattern p = Pattern.compile(
            ".*(<div.*id=[\\\"']"
            + ChatHtmlUtils.MESSAGE_TEXT_ID
            + elementId
            + "[\\\"'].*?</div>)", Pattern.DOTALL);

        Matcher m = p.matcher(message);

        if (m.find())
        {
            return m.group(1);
        }

        return null;
    }
}
