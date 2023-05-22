/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat.history;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.filetransfer.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>HistoryWindow</tt> is the window, where user could view or search
 * in the message history. The <tt>HistoryWindow</tt> could contain the history
 * for one or a group of <tt>MetaContact</tt>s.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class HistoryWindow
    extends SIPCommFrame
    implements  ChatConversationContainer,
                MessageListener,
                ChatRoomMessageListener,
                Skinnable
{
    private static final long serialVersionUID = 0L;
    private static final Logger sLog = Logger.getLogger(HistoryWindow.class);

    private static final String[] HISTORY_FILTER
        = new String[]
                {
                    MessageHistoryService.class.getName(),
                    FileHistoryService.class.getName()
                };

    /**
     * The horizontal and vertical spacing between the UI components of this
     * instance defined in one place for the purposes of consistency. Hopefully,
     * one day it should be defined at a global application level to achieve
     * consistency with the UI elsewhere.
     */
    private static final int SPACING = 5;

    private ChatConversationPanel mChatConvPanel;

    private final JPanel mMainPanel
        = new TransparentPanel(new BorderLayout(SPACING, SPACING));

    /**
     * Wrapper panel containing mDatesPanel and mExportPanel.
     */
    private TransparentPanel mLeftPanel;

    private DatesPanel mDatesPanel;

    /**
     * Container panel for the export chat button
     */
    private ExportPanel mExportPanel;

    private Object mHistoryContact;

    private MetaHistoryService mHistory;

    private Hashtable<Date, HTMLDocument> mDateHistoryTable
        = new Hashtable<>();

    private String mSearchKeyword;

    private Set<Date> mDatesDisplayed = new LinkedHashSet<>();

    private AccessibleChatBuffer mAccessibleBuffer;

    /**
     * If the <code>mHistoryContact</code> is a <code>MetaContact</code> or a
     * String (therefore SMS number),
     * contains the <code>OperationSetBasicInstantMessaging</code> instances to
     * which this <code>HistoryWindow</code> has added itself as a
     * <code>MessageListener</code>.
     */
    private java.util.List<OperationSetBasicInstantMessaging>
        mBasicInstantMessagings;

    /**
     * If the <code>mHistoryContact</code> is a <code>ChatRoomWrapper</code>,
     * we store it here.
     */
    private ChatRoomWrapper mChatRoomWrapper;

    /**
     * If the <code>mHistoryContact</code> is a <code>ChatRoomWrapper</code>,
     * specifies the <code>ChatRoom</code> to which this
     * <code>HistoryWindow</code> has added itself as a
     * <code>ChatRoomMessageListener</code>.
     */
    private ChatRoom mChatRoom;

    /**
     * The search panel for this window
     */
    private SearchPanel mSearchPanel;

    /**
     * Creates an instance of the <tt>HistoryWindow</tt>.
     * @param historyContact the <tt>MetaContact</tt> or the <tt>ChatRoom</tt>
     */
    public HistoryWindow(Object historyContact)
    {
        mHistoryContact = historyContact;

        mChatConvPanel = new ChatConversationPanel(this);
        mChatConvPanel.getChatTextPane()
            .setTransferHandler(new ExtendedTransferHandler());

        mHistory = GuiActivator.getMetaHistoryService();

        mDatesPanel = new DatesPanel(this);

        // Create the left hand panel which will contain the mDatesPanel and
        // mExportPanel.
        mLeftPanel = new TransparentPanel();
        mLeftPanel.setLayout(new BorderLayout());

        // Initialise the exportPanel.
        mExportPanel = new ExportPanel(historyContact);

        initPanels();

        initDates();

        pack();

        if (historyContact instanceof MetaContact)
        {
            MetaContact metaContact = (MetaContact) historyContact;

            setTitle(GuiActivator.getResources().getI18NString(
                "service.gui.HISTORY_CONTACT",
                new String[]{metaContact.getDisplayName()}));

            Iterator<Contact> protoContacts = metaContact.getContacts();

            mBasicInstantMessagings
                = new ArrayList<>();
            while (protoContacts.hasNext())
            {
                Contact protoContact = protoContacts.next();

                OperationSetBasicInstantMessaging basicInstantMessaging
                    = protoContact
                        .getProtocolProvider()
                            .getOperationSet(
                                OperationSetBasicInstantMessaging.class);

                if (basicInstantMessaging != null)
                {
                    basicInstantMessaging.addMessageListener(this);
                    mBasicInstantMessagings.add(basicInstantMessaging);
                }
            }
        }
        else if (historyContact instanceof ChatRoomWrapper)
        {
            mChatRoomWrapper = (ChatRoomWrapper) historyContact;
            mChatRoom = mChatRoomWrapper.getChatRoom();

            // If there is no chat room, it means either we've left the room
            // or we are offline.  Either way, we won't receive any messages,
            // so we don't need a listener.
            if (mChatRoom != null)
            {
                mChatRoom.addMessageListener(this);
            }

            String subject = mChatRoomWrapper.getChatRoomSubject();
            if (subject != null)
            {
                setTitle(GuiActivator.getResources().getI18NString(
                    "service.gui.HISTORY_CONTACT",
                    new String[]{subject}));
            }
        }
        else if (historyContact instanceof String)
        {
            String smsNumber = (String) historyContact;

            setTitle(GuiActivator.getResources().getI18NString(
                "service.gui.HISTORY_CONTACT",
                new String[]{smsNumber}));

            mBasicInstantMessagings =
                    new ArrayList<>();

            ProtocolProviderService smsImProvider = AccountUtils.getImProvider();

            if (smsImProvider != null)
            {
                OperationSetBasicInstantMessaging basicInstantMessaging =
                    smsImProvider.getOperationSet(OperationSetBasicInstantMessaging.class);
                basicInstantMessaging.addMessageListener(this);
                mBasicInstantMessagings.add(basicInstantMessaging);
            }
        }
    }

    /**
     * Constructs the window, by adding all components and panels.
     */
    private void initPanels()
    {
        mMainPanel.setBorder(
            BorderFactory.createEmptyBorder(SPACING, SPACING, SPACING, SPACING));
        mMainPanel.setPreferredSize(new ScaledDimension(500, 400));

        mSearchPanel = new SearchPanel(this);

        // Add the DatesPanel and ExportPanel to the LeftPanel.
        mLeftPanel.add(mDatesPanel, BorderLayout.CENTER);
        mLeftPanel.add(mExportPanel, BorderLayout.SOUTH);

        mMainPanel.add(mSearchPanel, BorderLayout.NORTH);
        mMainPanel.add(mChatConvPanel, BorderLayout.CENTER);
        mMainPanel.add(mLeftPanel, BorderLayout.WEST);

        getContentPane().add(mMainPanel);

        setMinimumSize(getPreferredSize());

        JTextField searchTextField = mSearchPanel.getSearchTextField();
        mAccessibleBuffer = new AccessibleChatBuffer(searchTextField);
        searchTextField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
                    && (e.getKeyCode() == KeyEvent.VK_U))
                {
                    if (mAccessibleBuffer != null)
                    {
                        mAccessibleBuffer.getPreviousMessage();
                    }
                }
                else if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
                    && (e.getKeyCode() == KeyEvent.VK_D))
                {
                    if (mAccessibleBuffer != null)
                    {
                        mAccessibleBuffer.getNextMessage();
                    }
                }
            }
        });
    }

    /**
     * Initializes the history with a list of all dates, for which a history
     * with the given contact is available.
     */
    private void initDates()
    {
        new DatesLoader().start();
    }

    /**
     * Shows a history for a given period.
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     */
    public void showHistoryByPeriod(Date startDate, Date endDate)
    {
        if ((mSearchKeyword == null || mSearchKeyword.length() == 0)
                && mDateHistoryTable.containsKey(startDate))
        {
            HTMLDocument document = mDateHistoryTable.get(startDate);

            mChatConvPanel.setContent(document);
        }
        else
        {
            mChatConvPanel.clear();

            new MessagesLoader(startDate, endDate).start();
        }
    }

    /**
     * Shows a history for a given keyword.
     * @param keyword the keyword to search
     */
    public void showHistoryByKeyword(String keyword)
    {
        // Replace null search with empty string
        if (keyword == null)
            keyword = "";

        mChatConvPanel.clear();
        mDatesPanel.setLastSelectedIndex(-1);

        new KeywordDatesLoader(keyword).start();

        mSearchKeyword = keyword;
    }

    /**
     * Shows the history given by the collection into a ChatConversationPanel.
     * @param historyRecords a collection of history records
     * @return an <tt>HTMLDocument</tt> containing the history given by
     * <tt>historyRecords</tt>
     */
    private HTMLDocument createHistory(Collection<Object> historyRecords)
    {
        if ((historyRecords != null) && (historyRecords.size() > 0))
        {
            Iterator<Object> i = historyRecords.iterator();
            String processedMessage = "";
            while (i.hasNext())
            {
                Object o = i.next();

                ChatMessage chatMessage = null;
                ProtocolProviderService protocolProvider = null;

                if (o instanceof MessageEvent)
                {
                    MessageEvent evt = (MessageEvent) o;

                    protocolProvider = evt.getProtocolProvider();

                    if (evt.isDisplayed())
                    {
                        chatMessage = new ChatMessage(
                                evt.getContactAddress(),
                                evt.getContactDisplayName(),
                                evt.getTimestamp(),
                                evt.getMessageType(),
                                null,
                                evt.getSourceMessage().getContent(),
                                evt.getSourceMessage().getContentType(),
                                evt.getSourceMessage().getMessageUID(),
                                null,
                                null,
                                evt.getErrorMessage(),
                                false,
                                false);
                    }
                }
                else if (o instanceof FileRecord)
                {
                    FileRecord fileRecord = (FileRecord) o;

                    protocolProvider
                        = fileRecord.getContact().getProtocolProvider();

                    FileHistoryConversationComponent component
                        = new FileHistoryConversationComponent(fileRecord);

                    mChatConvPanel.addComponent(component);
                }

                if (chatMessage != null)
                {
                    String contactAddress = chatMessage.getContactAddress();

                    if (contactAddress == null)
                    {
                        sLog.warn("Processing history for null contact address");
                    }

                    processedMessage = mChatConvPanel.processMessage(
                            chatMessage,
                            mSearchKeyword,
                            protocolProvider,
                            contactAddress);

                    mChatConvPanel.appendMessageToEnd(processedMessage,
                        ChatHtmlUtils.TEXT_CONTENT_TYPE);
                }
            }
        }

        mChatConvPanel.setDefaultContent();

        return mChatConvPanel.getContent();
    }

    /**
     * Implements <tt>ChatConversationContainer.setStatusMessage</tt> method.
     */
    public void addTypingNotification(String message) {}

    /**
     * Implements <tt>ChatConversationContainer.getWindow</tt> method.
     * @return this window
     */
    public Window getConversationContainerWindow()
    {
        return this;
    }

    /**
     * Indicates that the window is closing. Removes all message listeners when
     * closing.
     * @param e the <tt>WindowEvent</tt> that notified us
     */
    protected void windowClosing(WindowEvent e)
    {
        super.windowClosing(e);

        /*
         * Remove all listeners in order to have this instance ready for garbage
         * collection.
         */
        if (mBasicInstantMessagings != null)
        {
            for (OperationSetBasicInstantMessaging basicInstantMessaging
                    : mBasicInstantMessagings)
                basicInstantMessaging.removeMessageListener(this);
            mBasicInstantMessagings = null;
        }
        if (mChatRoom != null)
        {
            mChatRoom.removeMessageListener(this);
            mChatRoom = null;
        }
    }

    /**
     * Returns the next date from the history.
     * When <tt>date</tt> is the last one, we return the current date,
     * means we are loading today messages (till now).
     *
     * @param date The date which indicates where to start.
     * @return the date after the given date
     */
    public Date getNextDateFromHistory(Date date)
    {
        Iterator<Date> iterator = mDatesDisplayed.iterator();
        while (iterator.hasNext())
        {
            Date curr = iterator.next();
            if (curr.equals(date))
            {
                if (iterator.hasNext())
                    return iterator.next();
                else
                    break;
            }
        }

        return new Date(System.currentTimeMillis());
    }

    /**
     * Loads history dates.
     */
    private class DatesLoader extends Thread
    {
        public void run()
        {
            Collection<Object> msgList =
                mHistory.findByEndDate(HISTORY_FILTER,
                                       mHistoryContact,
                                       new Date(System.currentTimeMillis()));

            if (msgList != null)
            for (Object o : msgList)
            {
                Date date = new Date(0);

                if (o instanceof MessageEvent)
                {
                    MessageEvent evt = (MessageEvent)o;
                    date = evt.getTimestamp();
                }
                else if (o instanceof FileRecord)
                {
                    FileRecord fileRecord = (FileRecord) o;
                    date = fileRecord.getDate();
                }

                boolean containsDate = false;
                Iterator<Date> iterator = mDatesDisplayed.iterator();
                while (iterator.hasNext())
                {
                    Date currDate = iterator.next();
                    containsDate
                        = (GuiUtils.compareDatesOnly(date, currDate) == 0);

                    if (containsDate)
                        break;
                }

                if (!containsDate)
                {
                    mDatesDisplayed.add(date);
                }
            }

            if ((msgList != null) && (msgList.size() > 0))
            {
                Runnable updateDatesPanel = new Runnable() {
                    public void run() {
                        Date date = null;
                        for (Date curr : mDatesDisplayed)
                        {
                            date = curr;
                            if (!mDatesPanel.containsDate(date))
                                mDatesPanel.addDate(date);
                        }
                        //Initializes the conversation panel with the data of
                        //the last conversation.
                        mDatesPanel.setSelected(0);
                    }
                };
                SwingUtilities.invokeLater(updateDatesPanel);
            }
        }
     }

    /**
     * Loads history messages in the right panel.
     */
    private class MessagesLoader extends Thread
    {
        private final Date mStartDate;
        private final Date mEndDate;

        /**
         * Creates a MessageLoader thread charged to load history messages in
         * the right panel.
         *
         * @param startDate the start date of the history to load
         * @param endDate the end date of the history to load
         */
        public MessagesLoader (Date startDate, Date endDate)
        {
            mStartDate = startDate;
            mEndDate = endDate;
        }

        public void run()
        {
            final Collection<Object> msgList = mHistory.findByPeriod(
                HISTORY_FILTER, mHistoryContact, mStartDate, mEndDate);

            Runnable updateMessagesPanel = new Runnable()
            {
                public void run()
                {
                    HTMLDocument doc = createHistory(msgList);

                    if (mSearchKeyword == null || mSearchKeyword.length() == 0)
                    {
                        mDateHistoryTable.put(mStartDate, doc);
                    }
                }
            };
            SwingUtilities.invokeLater(updateMessagesPanel);
        }
    }

    /**
     * Loads dates found for keyword.
     */
    private class KeywordDatesLoader extends Thread
    {
        private Vector<Date> mKeywordDatesVector = new Vector<>();
        private final String mKeyword;

        /**
         * Creates a KeywordDatesLoader thread charged to load a list of dates
         * of messages found by the given keyword.
         *
         * @param keyword the keyword to search for
         */
        public KeywordDatesLoader(String keyword)
        {
            mKeyword = keyword;
        }

        public void run()
        {
            Collection<Object> msgList =
                mHistory.findByKeyword(HISTORY_FILTER, mHistoryContact, mKeyword);

            if (msgList != null)
            for (Object o : msgList)
            {
                Date date = new Date(0);

                if (o instanceof MessageEvent)
                {
                    MessageEvent evt = (MessageEvent)o;
                    date = evt.getTimestamp();
                }
                else if (o instanceof FileRecord)
                {
                    FileRecord fileRecord = (FileRecord) o;
                    date = fileRecord.getDate();
                }

                long milisecondsPerDay = 24*60*60*1000;
                for (Date date1 : mDatesDisplayed)
                {
                    if (Math.floor(date1.getTime()/milisecondsPerDay)
                        == Math.floor(date.getTime()/milisecondsPerDay)
                        && !mKeywordDatesVector.contains(date1))
                    {
                        mKeywordDatesVector.add(date1);
                    }
                }
            }

            Runnable updateDatesPanel = new Runnable()
            {
                public void run()
                {
                    mDatesPanel.removeAllDates();
                    if (mKeywordDatesVector.size() > 0)
                    {
                        Date date = null;
                        for (int i = 0; i < mKeywordDatesVector.size(); i++)
                        {
                            date = mKeywordDatesVector.get(i);

                            /* I have tried to remove and add dates in the
                             * datesList. A lot of problems occured because
                             * it seems that the list generates selection events
                             * when removing elements. This was solved but after
                             * that a problem occured when one and the same
                             * selection was done twice.
                             *
                             * if (!mKeywordDatesVector.contains(date)) {
                             *    mDatesPanel.removeDate(date);
                             * }
                             * else {
                             *    if (!mDatesPanel.containsDate(date)) {
                             *        mDatesPanel.addDate(date);
                             *    }
                            }*/
                            if (!mDatesPanel.containsDate(date))
                                 mDatesPanel.addDate(date);
                        }
                        mDatesPanel.setSelected(mDatesPanel.getDatesNumber() - 1);
                    }
                    else
                    {
                        mChatConvPanel.setDefaultContent();
                    }
                }
            };
            SwingUtilities.invokeLater(updateDatesPanel);
        }
    }

    /**
     * Implements the <tt>SIPCommFrame</tt> close method, which is invoked when
     * user presses the Esc key. Checks if the popup menu is visible and if
     * this is the case hides it, otherwise saves the current history window
     * size and location and disposes the window.
     * @param isEscaped indicates if the window has been closed by pressing the
     * Esc key
     */
    protected void close(boolean isEscaped)
    {
        if (mChatConvPanel.getRightButtonMenu().isVisible())
        {
            mChatConvPanel.getRightButtonMenu().setVisible(false);
        }
        else
        {
            GuiActivator.getUIService().getHistoryWindowManager()
                .removeHistoryWindowForChatDescriptor(mHistoryContact);

            mDatesPanel.dispose();
            mChatConvPanel.dispose();

            dispose();
        }
    }

    /**
     * Implements MessageListener.messageReceived method in order to refresh the
     * history when new message is received.
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        // Only handle the message if it is relevant to this History Window.
        if (messageIsRelevant(evt))
        {
            handleMessageEvent(evt, Chat.INCOMING_MESSAGE);
        }
    }

    /**
     * Implements MessageListener.messageDelivered method in order to refresh the
     * history when new message is sent.
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
        // Only handle the message if it is relevant to this History Window.
        if (messageIsRelevant(evt))
        {
            handleMessageEvent(evt, Chat.OUTGOING_MESSAGE);
        }
    }

    /**
     * Refreshes the history when a new message is sent or received.
     *
     * @param evt The MessageEvent associated with the new message
     */
    private void handleMessageEvent(MessageEvent evt, String direction)
    {
        ImMessage sourceMessage = evt.getSourceMessage();

        processMessage(
            evt.getContactAddress(),
            evt.getContactDisplayName(),
            evt.getProtocolProvider(),
            evt.getTimestamp(),
            evt.getMessageType(),
            sourceMessage.getContent(),
            sourceMessage.getContentType(),
            sourceMessage.getMessageUID(),
            evt.getErrorMessage());
    }

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {}

    /**
     * Processes the message given by the parameters.
     * @param address the message source or destination address
     * @param displayName the displayName of the source or destination address
     * @param protocolProvider the protocol provider
     * @param timestamp the timestamp of the message
     * @param messageType INCOMING or OUTGOING
     * @param messageContent the content text of the message
     * @param messageContentType the content type of the message
     * @param messageUID The ID of the message.
     * @param errorMessage the error message to display (will be null if the
     *                     message hasn't failed)
     */
    private void processMessage(String address,
                                String displayName,
                                ProtocolProviderService protocolProvider,
                                Date timestamp,
                                String messageType,
                                String messageContent,
                                String messageContentType,
                                String messageUID,
                                String errorMessage)
    {
        if (!((mHistoryContact instanceof MetaContact) ||
              (mHistoryContact instanceof ChatRoomWrapper) ||
              (mHistoryContact instanceof String)))
            return;

        // If dates aren't yet loaded we don't process the message.
        if (mDatesPanel.getDatesNumber() < 1)
            return;

        Date lastDate = mDatesPanel.getDate(0);

        if (lastDate != null
            && GuiUtils.compareDatesOnly(lastDate, timestamp) == 0)
        {
            HTMLDocument document = mDateHistoryTable.get(lastDate);

            if (document != null)
            {
                ChatMessage chatMessage = new ChatMessage(
                    address,
                    displayName,
                    timestamp,
                    messageType,
                    null,
                    messageContent,
                    messageContentType,
                    messageUID,
                    null,
                    null,
                    errorMessage,
                    false,
                    false);

                String processedMessage = mChatConvPanel.processMessage(
                    chatMessage,
                    mSearchKeyword,
                    protocolProvider,
                    address);

                appendMessageToDocument(document, processedMessage);
            }
        }
        else if (lastDate == null
            || GuiUtils.compareDatesOnly(lastDate, timestamp) < 0)
        {
            long milisecondsPerDay = 24*60*60*1000;

            Date date = new Date(timestamp.getTime()
                - timestamp.getTime() % milisecondsPerDay);

            mDatesDisplayed.add(date);
            if (!mDatesPanel.containsDate(date))
                 mDatesPanel.addDate(date);
        }
    }

    /**
     * Appends the given string at the end of the given html document.
     *
     * @param doc the document to append to
     * @param chatString the string to append
     */
    private void appendMessageToDocument(HTMLDocument doc, String chatString)
    {
        Element root = doc.getDefaultRootElement();

        try
        {
            doc.insertBeforeEnd(
                // the body element
                root.getElement(root.getElementCount() - 1),
                // the message to insert
                chatString);
        }
        catch (BadLocationException | IOException e)
        {
            sLog.error("Insert in the HTMLDocument failed.", e);
        }
    }

    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {
        handleMessageEvent(evt, Chat.OUTGOING_MESSAGE);
    }

    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt) {}

    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        handleMessageEvent(evt, Chat.INCOMING_MESSAGE);
    }

    public AccessibleChatBuffer getAccessibleChatBuffer()
    {
        return mAccessibleBuffer;
    }

    /**
     * Re-process history.
     */
    public void loadSkin()
    {
        mDateHistoryTable.clear();

        Date startDate = mDatesPanel.getDate(mDatesPanel.getLastSelectedIndex());
        mChatConvPanel.clear();

        new MessagesLoader(startDate, getNextDateFromHistory(startDate)).start();
    }

    @Override
    public ChatType getChatType()
    {
        return (mChatRoomWrapper != null) ? ChatType.GROUP_CHAT :
                                            ChatType.ONE_TO_ONE_CHAT;
    }

    /**
     * Check whether the peer for the received message matches the contact
     * (either a MetaContact, or a String representing an SMS number) for this
     * History Window.
     * This is necessary because the MessageListener listens for all sent and
     * received messages, not just messages for this conversation.
     * @param evt the received message event
     * @return true if the peer matches the contact for this window
     */
    private boolean messageIsRelevant(OneToOneMessageEvent evt)
    {
        MetaContactListService contactListService =
            GuiActivator.getContactListService();
        int eventType = evt.getEventType();
        boolean messageRelevant = false;

        if (eventType == MessageEvent.SMS_MESSAGE)
        {
            if (mHistoryContact instanceof MetaContact)
            {
                MetaContact metaContact = contactListService.
                          findMetaContactForSmsNumber(evt.getPeerIdentifier());

                if ((metaContact != null) &&
                    (metaContact.equals(mHistoryContact)))
                {
                    messageRelevant = true;
                }
            }
            else if (mHistoryContact instanceof String)
            {
                if (evt.getPeerIdentifier().equals(mHistoryContact))
                {
                    messageRelevant = true;
                }
            }
        }
        else if ((eventType == MessageEvent.CHAT_MESSAGE) &&
                 (mHistoryContact instanceof MetaContact))
        {
            MetaContact metaContact =
             contactListService.findMetaContactByContact(evt.getPeerContact());

            if ((metaContact != null) && (metaContact == mHistoryContact))
            {
                messageRelevant = true;
            }
        }

        return messageRelevant;
    }
}
