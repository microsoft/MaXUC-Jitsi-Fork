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
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatRoom;

/**
 * Manages chat windows and panels.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 * @author Lyubomir Marinov
 */
public class ChatWindowManager
{
    /**
     * The <tt>Logger</tt> used by the <tt>ChatWindowManager</tt> class and its
     * instances for logging output.
     */
    private static final Logger sLog =
        Logger.getLogger(ChatWindowManager.class);

    /**
     * The <tt>ContactLogger</tt> used by this class for logging contact
     * operations in more detail
     */
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    private final List<ChatPanel> mChatPanels
        = new ArrayList<>();

    private final List <ChatListener> mChatListeners
        = new ArrayList<>();

    /**
     * A lock object to control synchronization of chat panels.
     */
    private final Object mChatSyncRoot = new Object();

    /**
     * Opens the specified <tt>ChatPanel</tt>, optionally brings it to the
     * front but does not alert the window.
     *
     * @param chatPanel the <tt>ChatPanel</tt> to be opened
     * @param setSelected <tt>true</tt> if <tt>chatPanel</tt> (and respectively
     * its <tt>ChatWindow</tt>) should be brought to the front; otherwise,
     * <tt>false</tt>
     */
    public void openChat(final ChatPanel chatPanel,
                         final boolean setSelected)
    {
        openChat(chatPanel, setSelected, false);
    }

    /**
     * Opens the specified <tt>ChatPanel</tt>, optionally brings it to the
     * front and alerts the window.
     *
     * @param chatPanel the <tt>ChatPanel</tt> to be opened
     * @param setSelected <tt>true</tt> if <tt>chatPanel</tt> (and respectively
     * its <tt>ChatWindow</tt>) should be brought to the front; otherwise,
     * <tt>false</tt>
     */
    public void openChatAndAlertWindow(final ChatPanel chatPanel,
                                       final boolean setSelected)
    {
        openChat(chatPanel, setSelected, true);
    }

    /**
     * Opens the specified <tt>ChatPanel</tt>, optionally brings it to the
     * front and optionally alerts the window.
     *
     * @param chatPanel the <tt>ChatPanel</tt> to be opened
     * @param setSelected <tt>true</tt> if <tt>chatPanel</tt> (and respectively
     * its <tt>ChatWindow</tt>) should be brought to the front; otherwise,
     * <tt>false</tt>
     * @param alertWindow if true, the window will be alerted when it is opened.
     */
    private void openChat(final ChatPanel chatPanel,
                          final boolean setSelected,
                          final boolean alertWindow)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    openChat(chatPanel, setSelected, alertWindow);
                }
            });
            return;
        }

        synchronized (mChatSyncRoot)
        {
            ChatContainer chatContainer = chatPanel.getChatContainer();

            if (!chatPanel.isShown())
                chatContainer.addChat(chatPanel);

            chatContainer.openChat(chatPanel, setSelected, alertWindow);
        }
    }

    /**
     * Closes the given chat panel.
     *
     * @param chatPanel the chat panel to close
     */
    public void closeChat(final ChatPanel chatPanel)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    closeChat(chatPanel);
                }
            });
            return;
        }

        synchronized (mChatSyncRoot)
        {
            if (containsChat(chatPanel))
            {
                if (!chatPanel.isWriteAreaEmpty())
                {
                    int answer = showWarningMessage(
                            "service.gui.NON_EMPTY_CHAT_WINDOW_CLOSE",
                            chatPanel);

                    if (answer == JOptionPane.OK_OPTION)
                        closeChatPanel(chatPanel);
                }
                else if (chatPanel.containsActiveFileTransfers())
                {
                    int answer = showWarningMessage(
                                "service.gui.CLOSE_CHAT_ACTIVE_FILE_TRANSFER",
                                chatPanel);

                    if (answer == JOptionPane.OK_OPTION)
                    {
                        chatPanel.cancelActiveFileTransfers();
                        closeChatPanel(chatPanel);
                    }
                }
                else
                {
                    closeChatPanel(chatPanel);
                }
            }
        }
    }

    /**
     * Disposes the given chat window.
     *
     * @param chatContainer the <tt>ChatContainer</tt> to dispose of
     */
    private void closeChat(ChatContainer chatContainer)
    {
        ChatPanel currentChat = chatContainer.getCurrentChat();

        if (currentChat != null)
        {
            closeChatPanel(currentChat);
        }

        // Remove the envelope from the all active contacts in the contact list.
        GuiActivator.getMainFrameContactList().deactivateAll();
    }

    /**
     * Closes the  chat in the specified <tt>ChatContainer</tt> and makes it
     * available for garbage collection.
     *
     * @param chatContainer the <tt>ChatContainer</tt> containing the chat to
     * close
     * @param warningEnabled indicates if the user should be warned that we're
     * closing the chat. This would be done only if there are currently
     * active file transfers or waiting messages.
     */
    void closeChat(ChatContainer chatContainer, boolean warningEnabled)
    {
        synchronized (mChatSyncRoot)
        {
            // If no warning is enabled we just close all chats without asking
            // and return.
            if (!warningEnabled)
            {
                sLog.debug("Warnings are disabled - closing all chats " +
                                     "without checking whether any are in use");
                closeChat(chatContainer);
                return;
            }

            ChatPanel activePanel = null;

            // Before closing the chat window, we need to check whether any of
            // its chat panels are in use (i.e. contain an unsent message, an
            // in-progress file transfer or have received a new message in the
            // last 2 seconds).

            // MultiChatWindow is disabled, which means all chat panels
            // are in their own chat window, so we only need to check the
            // current chat panel before closing the chat window.
            sLog.debug("MultiChatWindow is disabled - checking " +
                                "whether the current chat panel is in use");
            ChatPanel chatPanel = chatContainer.getCurrentChat();
            if (chatPanel != null && chatPanel.isChatInUse())
            {
                activePanel = chatPanel;
            }

            if (activePanel == null)
            {
                sLog.debug("No chat windows are in use, so closing " +
                                                                   "all chats");
                closeChat(chatContainer);
                return;
            }

            if (!activePanel.isWriteAreaEmpty())
            {
                sLog.debug("Found unsent message - warning user " +
                                                  "before closing chat window");
                int answer = showWarningMessage(
                        "service.gui.NON_EMPTY_CHAT_WINDOW_CLOSE",
                        chatContainer.getFrame());

                if (answer == JOptionPane.OK_OPTION)
                    closeChat(chatContainer);
            }
            else if (activePanel.containsActiveFileTransfers())
            {
                sLog.debug("Found active file transfer - warning user " +
                                                  "before closing chat window");
                int answer = showWarningMessage(
                        "service.gui.CLOSE_CHAT_ACTIVE_FILE_TRANSFER",
                        chatContainer.getFrame());

                if (answer == JOptionPane.OK_OPTION)
                {
                    for (ChatPanel nextChatPanel : mChatPanels)
                    {
                        nextChatPanel.cancelActiveFileTransfers();
                    }

                    closeChat(chatContainer);
                }
            }
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * SMS number and optionally creates it if it does not exist.
     * @param smsNumber the SMS number to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified SMS number if such <tt>ChatPanel</tt> does not
     * exist yet
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt>; <tt>null</tt> if there is no such
     * <tt>ChatPanel</tt> and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getContactChat(String smsNumber,
                                    boolean create)
    {
        ChatPanel chatPanel = findChatPanelForDescriptor(smsNumber);

        if (chatPanel == null)
        {
            PhoneNumberUtilsService phoneUtilsService =
                                            GuiActivator.getPhoneNumberUtils();

            // For SMS messages, we tend to create the chat panel with a
            // national number.  However the delivery notification may come
            // back in international format.  Therefore if we failed to find it
            // with the existing peerIdentifier, try converting to national
            // format.
            String nationalSmsNumber =
                           phoneUtilsService.formatNumberToNational(smsNumber);
            chatPanel = findChatPanelForDescriptor(nationalSmsNumber);
        }

        if ((chatPanel == null) && create)
        {
            chatPanel = createChat(smsNumber);
        }

        return chatPanel;
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt> and optionally creates it if it does not exist.
     *
     * @param metaContact the <tt>MetaContact</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>MetaContact</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt>; <tt>null</tt> if there is no such
     * <tt>ChatPanel</tt> and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getContactChat(MetaContact metaContact, boolean create)
    {
        // if we are not creating a ui we don't need any execution
        // in event dispatch thread, lets execute now
        if (!create)
            return getContactChat(metaContact,
                                  null,
                                  null,
                                  create);
        else
        {
            // we may create using event dispatch thread
            MetaContactChatCreateRunnable runnable
                = new MetaContactChatCreateRunnable(
                    metaContact,
                    null,
                    null);
            return runnable.getChatPanel();
        }
    }

    /**
     * Returns the chat panel corresponding to the given meta contact
     *
     * @param metaContact the meta contact.
     * @param protocolContact the protocol specific contact
     * @return the chat panel corresponding to the given meta contact
     */
    public ChatPanel getContactChat(MetaContact metaContact,
                                    Contact protocolContact)
    {
        return getContactChat(metaContact, protocolContact, null);
    }

    /**
     * Returns the chat panel corresponding to the given meta contact
     *
     * @param metaContact the meta contact.
     * @param protocolContact the protocol specific contact
     * @param contactResource the resource from which the contact is writing
     * @return the chat panel corresponding to the given meta contact
     */
    public ChatPanel getContactChat(MetaContact metaContact,
                                    Contact protocolContact,
                                    ContactResource contactResource)
    {
        // we may create using event dispatch thread
        MetaContactChatCreateRunnable runnable
            = new MetaContactChatCreateRunnable(
                metaContact,
                protocolContact,
                contactResource);
        return runnable.getChatPanel();
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt> and optionally creates it if it does not exist.
     * Must be executed on the event dispatch thread as it is creating UI.
     *
     * @param metaContact the <tt>MetaContact</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param protocolContact the <tt>Contact</tt> (respectively its
     * <tt>ChatTransport</tt>) to be selected in the newly created
     * <tt>ChatPanel</tt>; <tt>null</tt> to select the default <tt>Contact</tt>
     * of <tt>metaContact</tt> if it is online or one of its <tt>Contact</tt>s
     * which supports offline messaging
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>MetaContact</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>MetaContact</tt>; <tt>null</tt> if there is no such
     * <tt>ChatPanel</tt> and <tt>create</tt> is <tt>false</tt>
     */
    private ChatPanel getContactChat(
            MetaContact metaContact,
            Contact protocolContact,
            ContactResource contactResource,
            boolean create)
    {
        synchronized (mChatSyncRoot)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(metaContact);

            if ((chatPanel == null) && create)
                chatPanel
                    = createChat(
                        metaContact,
                        protocolContact,
                        contactResource);
            return chatPanel;
        }
    }

    /**
     * Returns the currently selected <tt>ChatPanel</tt>.
     *
     * @return the currently selected <tt>ChatPanel</tt>
     */
    public ChatPanel getSelectedChat()
    {
        ChatPanel selectedChat = null;

        synchronized (mChatSyncRoot)
        {
            Iterator<ChatPanel> chatPanelsIter = mChatPanels.iterator();

            while (chatPanelsIter.hasNext())
            {
                ChatPanel chatPanel = chatPanelsIter.next();

                if (chatPanel.getChatContainer().getFrame().isFocusOwner())
                    selectedChat = chatPanel;
            }

            return selectedChat;
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoomWrapper</tt> and optionally creates it if it does not exist
     * yet.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to get the
     * corresponding <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a new <tt>ChatPanel</tt> for the
     * specified <tt>ChatRoomWrapper</tt> if no such <tt>ChatPanel</tt> exists
     * already; otherwise, <tt>false</tt>
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoomWrapper</tt> or <tt>null</tt> if no such <tt>ChatPanel</tt>
     * exists and <tt>create</tt> is <tt>false</tt>
     */
    private ChatPanel getMultiChatInternal(ChatRoomWrapper chatRoomWrapper, boolean create)
    {
        synchronized (mChatSyncRoot)
        {
            ChatPanel chatPanel = findChatPanelForDescriptor(chatRoomWrapper);

            if ((chatPanel == null) && create)
            {
                chatPanel = createChat(chatRoomWrapper);
            }
            return chatPanel;
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoomWrapper</tt> and optionally creates it if it does not exist
     * yet.
     * Must be executed on the event dispatch thread.
     *
     * @param chatRoomWrapper the <tt>ChatRoomWrapper</tt> to get the
     * corresponding <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a new <tt>ChatPanel</tt> for the
     * specified <tt>ChatRoomWrapper</tt> if no such <tt>ChatPanel</tt> exists
     * already; otherwise, <tt>false</tt>
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoomWrapper</tt> or <tt>null</tt> if no such <tt>ChatPanel</tt>
     * exists and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getMultiChat(ChatRoomWrapper chatRoomWrapper, boolean create)
    {
        if (!create)
            return getMultiChatInternal(chatRoomWrapper, create);
        else
        {
            // tries to execute creating of the ui on the
            // event dispatch thread
            return new CreateChatRoomWrapperRunner(chatRoomWrapper).getChatPanel();
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt> and optionally creates it if it does not exist.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>ChatRoom</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt>; <tt>null</tt> if there is no such <tt>ChatPanel</tt>
     * and <tt>create</tt> is <tt>false</tt>
     */
    private ChatPanel getMultiChatInternal(ChatRoom chatRoom,
                                           boolean create)
    {
        synchronized (mChatSyncRoot)
        {
            ChatRoomList chatRoomList
                = GuiActivator
                    .getUIService()
                        .getConferenceChatManager().getChatRoomList();

            // Search in the chat room's list for a chat room that correspond
            // to the given one.
            ChatRoomWrapper chatRoomWrapper
                = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

            if ((chatRoomWrapper == null) && create)
            {
                ChatRoomProviderWrapper parentProvider
                    = chatRoomList.findServerWrapperFromProvider(
                        chatRoom.getParentProvider());

                chatRoomWrapper = new ChatRoomWrapper(parentProvider, chatRoom);

                chatRoomList.addChatRoom(chatRoomWrapper);
            }

            ChatPanel chatPanel = null;

            if (chatRoomWrapper != null)
            {
                chatPanel = findChatPanelForDescriptor(chatRoomWrapper);
                if (((chatPanel == null) || chatPanel.isDisconnected()) && create)
                {
                    if (chatPanel != null)
                    {
                        sLog.warn("Closing disconnected chat panel for " +
                                  sanitiseChatRoom(chatRoom.getIdentifier()));
                        closeChatPanel(chatPanel);
                    }

                    chatPanel = createChat(chatRoomWrapper);
                }
            }
            else
            {
                sLog.error("Unable to get ChatRoomWrapper for " +
                           sanitiseChatRoom(chatRoom.getIdentifier()));
            }

            return chatPanel;
        }
    }

    /**
     * Gets the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt> and optionally creates it if it does not exist.
     * Must be executed on the event dispatch thread.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to get the corresponding
     * <tt>ChatPanel</tt> of
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>ChatRoom</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @return the <tt>ChatPanel</tt> corresponding to the specified
     * <tt>ChatRoom</tt>; <tt>null</tt> if there is no such <tt>ChatPanel</tt>
     * and <tt>create</tt> is <tt>false</tt>
     */
    public ChatPanel getMultiChat(ChatRoom chatRoom, boolean create)
    {
        if (!create)
        {
            return getMultiChatInternal(chatRoom, create);
        }
        else
        {
            return new CreateChatRoomRunner(chatRoom).getChatPanel();
        }
    }

    /**
     * Starts a chat with the given SMS number.
     * @param smsNumber the destination SMS number
     */
    public void startSMSChat(String smsNumber)
    {
        MetaContact metaContact = GuiActivator.getContactListService().
                                         findMetaContactForSmsNumber(smsNumber);

        SwingUtilities.invokeLater(
                        new RunChatWindow(metaContact, null, smsNumber, false));
    }

    /**
     * Starts a chat with the given <tt>MetaContact</tt>.
     * @param metaContact the destination <tt>MetaContact</tt>
     */
    public void startChat(MetaContact metaContact)
    {
        SwingUtilities.invokeLater(new RunChatWindow(metaContact));
    }

    /**
     * Starts a chat with the given <tt>MetaContact</tt>.
     * @param metaContact the destination <tt>MetaContact</tt>
     * @param protocolContact the protocol contact of the destination
     * @param isSmsMessage indicates if the chat should be opened for an SMS
     * message
     */
    public void startChat(MetaContact metaContact,
                          Contact protocolContact,
                          boolean isSmsMessage)
    {
        SwingUtilities.invokeLater(new RunChatWindow(
                metaContact, protocolContact, null, isSmsMessage));
    }

    public void startChat(String contactString)
    {
        List<ProtocolProviderService> imProviders
            = AccountUtils.getAllProviders(
                    OperationSetBasicInstantMessaging.class);

        if (imProviders.size() < 1)
            throw new IllegalStateException("imProviders");

        Contact contact = null;
        MetaContactListService metaContactListService
            = GuiActivator.getContactListService();
        MetaContact metaContact = null;
        boolean startChat = false;

        for (ProtocolProviderService imProvider : imProviders)
        {
            try
            {
                OperationSetPresence presenceOpSet
                    = imProvider.getOperationSet(OperationSetPresence.class);

                if (presenceOpSet != null)
                {
                    contact = presenceOpSet.findContactByID(contactString);
                    if (contact != null)
                    {
                        metaContact
                            = metaContactListService.findMetaContactByContact(
                                    contact);
                        if (metaContact != null)
                        {
                            startChat = true;
                            break;
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
            }
        }
        if (startChat)
            startChat(metaContact, contact, false);
    }

    /**
     * Opens the chat room that matches the given chat room uid. If one doesn't
     * exist, start a chat room with the given chat room uid
     *
     * @param chatRoomUid the chat room uid corresponding to the chat room to
     * open
     * @param isClosed whether the user has left this chat room
     * @param chatRoomSubject the last known subject of this chat room
     */
    public void startChatRoom(final String chatRoomUid,
                              final boolean isClosed,
                              final String chatRoomSubject)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            // We need to get off the EDT to create the RGCW task since doing
            // so does network operations
            new Thread("StartChatRoom Thread " + chatRoomUid)
            {
                @Override
                public void run()
                {
                    startChatRoom(chatRoomUid, isClosed, chatRoomSubject);
                }
            }.start();

            return;
        }

        SwingUtilities.invokeLater(new RunGroupChatWindow(chatRoomUid,
                                                          isClosed,
                                                          chatRoomSubject));
    }

    /**
     * Removes the non read state of the currently selected chat session. This
     * will result in removal of all icons representing the non read state (like
     * envelopes in contact list).
     *
     * @param chatPanel the <tt>ChatPanel</tt> for which we would like to remove
     * non read chat state
     */
    public void removeNonReadChatState(ChatPanel chatPanel)
    {
        ChatSession chatSession = chatPanel.getChatSession();

        if (chatSession instanceof MetaContactChatSession)
        {
            MetaContact selectedMetaContact
                = (MetaContact) chatSession.getDescriptor();

            TreeContactList clist
                = GuiActivator.getMainFrameContactList();

            // Remove the envelope from the contact when the chat has
            // gained the focus.
            if (clist.isContactActive(selectedMetaContact))
            {
                clist.setActiveContact(selectedMetaContact, false);
            }
        }
    }

    /**
     * Closes the selected chat window.
     *
     * @param chatPanel the chat panel to close.
     */
    private void closeChatPanel(ChatPanel chatPanel)
    {
        sLog.info("Closing chat panel: " + chatPanel);
        ChatContainer chatContainer = chatPanel.getChatContainer();

        if (chatContainer != null)
        {
            chatContainer.removeChat(chatPanel);
        }
        else
        {
            sLog.debug("No chat container found");
        }

        boolean isChatPanelContained;

        synchronized (mChatSyncRoot)
        {
            isChatPanelContained = mChatPanels.remove(chatPanel);
        }

        if (isChatPanelContained)
        {
            chatPanel.dispose();
            fireChatClosed(chatPanel);
        }
        else
        {
            sLog.debug("Chat panel not found - not disposing");
        }
    }

    /**
     * Gets the default <tt>Contact</tt> of the specified <tt>MetaContact</tt>
     * if it is online; otherwise, gets one of its <tt>Contact</tt>s which
     * supports offline messaging.
     *
     * @param metaContact the <tt>MetaContact</tt> to get the default
     * <tt>Contact</tt> of
     * @return the default <tt>Contact</tt> of the specified
     * <tt>MetaContact</tt> if it is online; otherwise, gets one of its
     * <tt>Contact</tt>s which supports offline messaging
     */
    private Contact getDefaultContact(MetaContact metaContact)
    {
        Contact defaultContact = metaContact.getDefaultContact(
                        OperationSetBasicInstantMessaging.class);

        if (defaultContact == null)
        {
            return null;
        }

        ProtocolProviderService defaultProvider
            = defaultContact.getProtocolProvider();

        if (defaultProvider == null)
        {
            return null;
        }

        OperationSetBasicInstantMessaging defaultIM
            = defaultProvider
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        if (defaultIM == null)
        {
            return null;
        }

        if (defaultContact.getPresenceStatus().getStatus() < 1
                && (!defaultIM.isOfflineMessagingSupported()
                        || !defaultProvider.isRegistered()))
        {
            Iterator<Contact> protoContacts = metaContact.getContacts();

            while (protoContacts.hasNext())
            {
                Contact contact = protoContacts.next();
                ProtocolProviderService protoContactProvider
                    = contact.getProtocolProvider();
                OperationSetBasicInstantMessaging protoContactIM
                    = protoContactProvider
                        .getOperationSet(
                            OperationSetBasicInstantMessaging.class);

                if (protoContactIM != null
                    && protoContactIM.isOfflineMessagingSupported()
                       && protoContactProvider.isRegistered())
                {
                    defaultContact = contact;
                }
            }
        }
        return defaultContact;
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given SMS number and saves it in the
     * list of created <tt>ChatPanel</tt>s.
     *
     * @param smsNumber the SMS number to create a <tt>ChatPanel</tt> for
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(String smsNumber)
    {
        ChatContainer chatContainer = getChatContainer();
        ChatPanel chatPanel = new ChatPanel(chatContainer);

        SMSChatSession chatSession = new SMSChatSession(chatPanel, smsNumber);
        chatPanel.setChatSession(chatSession);

        synchronized (mChatSyncRoot)
        {
            mChatPanels.add(chatPanel);
        }

        fireChatCreated(chatPanel);
        return chatPanel;
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given contact and saves it in the
     * list of created <tt>ChatPanel</tt>s.
     *
     * @param metaContact the <tt>MetaContact</tt> for which to create a
     * <tt>ChatPanel</tt>
     * @param protocolContact the <tt>Contact</tt> (respectively its
     * <tt>ChatTransport</tt>) to be selected in the newly created
     * <tt>ChatPanel</tt>; <tt>null</tt> to select the default <tt>Contact</tt>
     * of <tt>metaContact</tt> if it is online or one of its <tt>Contact</tt>s
     * which supports offline messaging
     * @param contactResource the <tt>ContactResource</tt>, to be selected in
     * the newly created <tt>ChatPanel</tt>
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(MetaContact metaContact,
                                 Contact protocolContact,
                                 ContactResource contactResource)
    {
        if (protocolContact == null)
            protocolContact = getDefaultContact(metaContact);

        ChatContainer chatContainer = getChatContainer();
        ChatPanel chatPanel = new ChatPanel(chatContainer);

        MetaContactChatSession chatSession
            = new MetaContactChatSession(chatPanel,
                                         metaContact,
                                         protocolContact,
                                         contactResource);

        chatPanel.setChatSession(chatSession);

        synchronized (mChatSyncRoot)
        {
            mChatPanels.add(chatPanel);
        }

        // If SMS is enabled we need to set the current chat transport for this
        // chat panel to be either IM or SMS based on what the last message
        // in the history for this panel was.
        if (ConfigurationUtils.isSmsEnabled())
        {
            chatSession.selectMostRecentChatTransport();
        }

        fireChatCreated(chatPanel);
        return chatPanel;
    }

    /**
     * Gets a new <tt>ChatContainer</tt> instance.
     *
     * @return a <tt>ChatContainer</tt> instance
     */
    private ChatContainer getChatContainer()
    {
        ChatContainer chatContainer;

        chatContainer = new ChatWindow();
        GuiActivator.getUIService().registerExportedWindow(
            (ExportedWindow) chatContainer);

        return chatContainer;
    }

    /**
     * Creates a <tt>ChatPanel</tt> for the given <tt>ChatRoom</tt> and saves it
     * in the list of created <tt>ChatPanel</tt>s.
     *
     * @param chatRoomWrapper the <tt>ChatRoom</tt>, for which the chat will be
     * created
     * @return The <code>ChatPanel</code> newly created.
     */
    private ChatPanel createChat(ChatRoomWrapper chatRoomWrapper)
    {
        ChatContainer chatContainer = getChatContainer();
        ChatPanel chatPanel = new ChatPanel(chatContainer);

        ConferenceChatSession chatSession
            = new ConferenceChatSession(chatPanel,
                                        chatRoomWrapper);

        chatPanel.setChatSession(chatSession);

        synchronized (mChatSyncRoot)
        {
            mChatPanels.add(chatPanel);
        }

        fireChatCreated(chatPanel);
        return chatPanel;
    }

    /**
     * Finds the <tt>ChatPanel</tt> corresponding to the given chat descriptor.
     *
     * @param descriptor the chat descriptor.
     * @return the <tt>ChatPanel</tt> corresponding to the given chat descriptor
     * if any; otherwise, <tt>null</tt>
     */
    private ChatPanel findChatPanelForDescriptor(Object descriptor)
    {
        ChatPanel matchedChatPanel = null;

        synchronized (mChatSyncRoot)
        {
            for (ChatPanel chatPanel : mChatPanels)
            {
                if (chatPanel.getChatSession().getDescriptor().equals(descriptor))
                {
                    matchedChatPanel = chatPanel;
                    break;
                }
            }
        }

        sLog.debug("Found chat panel: " + matchedChatPanel +
            " for descriptor: " + descriptor);

        return matchedChatPanel;
    }

    /**
     * Notifies the <tt>ChatListener</tt>s registered with this instance that
     * a specific <tt>Chat</tt> has been closed.
     *
     * @param chat the <tt>Chat</tt> which has been closed and which the
     * <tt>ChatListener</tt>s registered with this instance are to be notified
     * about
     */
    private void fireChatClosed(Chat chat)
    {
        synchronized (mChatListeners)
        {
            for (ChatListener listener : mChatListeners)
                listener.chatClosed(chat);
        }
    }

    /**
     * Notifies the <tt>ChatListener</tt>s registered with this instance that
     * a specific <tt>Chat</tt> has been created.
     *
     * @param chat the <tt>Chat</tt> which has been created and which the
     * <tt>ChatListener</tt>s registered with this instance are to be notified
     * about
     */
    private void fireChatCreated(Chat chat)
    {
        synchronized (mChatListeners)
        {
            for (ChatListener listener : mChatListeners)
                listener.chatCreated(chat);
        }
    }

    /**
     * Returns <tt>true</tt> if this chat window contains the given chatPanel;
     * <tt>false</tt>, otherwise.
     *
     * @param chatPanel the chat panel that we're looking for.
     * @return <tt>true</tt> if this chat window contains the given chatPanel;
     * <tt>false</tt>, otherwise
     */
    private boolean containsChat(ChatPanel chatPanel)
    {
        synchronized (mChatSyncRoot)
        {
            return mChatPanels.contains(chatPanel);
        }
    }

    /**
     * Runs the chat window for the specified contact
     */
    private class RunChatWindow implements Runnable
    {
        private MetaContact mMetaContact;

        private Contact mProtocolContact;

        private String mSmsNumber;

        private boolean mIsSmsSelected = false;

        /**
         * Creates an instance of <tt>RunMessageWindow</tt> by specifying the
         *
         * @param metaContact the meta contact to which we will talk.
         */
        public RunChatWindow(MetaContact metaContact)
        {
            mMetaContact = metaContact;
        }

        /**
         * Creates a chat window
         *
         * @param metaContact
         * @param protocolContact
         * @param smsNumber
         * @param isSmsSelected
         */
        public RunChatWindow(MetaContact metaContact,
                             Contact protocolContact,
                             String smsNumber,
                             boolean isSmsSelected)
        {
            mMetaContact = metaContact;
            mProtocolContact = protocolContact;
            mSmsNumber = smsNumber;
            mIsSmsSelected = isSmsSelected;
        }

        /**
         * Opens a chat window
         */
        public void run()
        {
            ChatPanel chatPanel;

            if (mMetaContact == null)
            {
                contactLogger.debug("Getting SMS chat for number " +
                                    logHasher(mSmsNumber));
                chatPanel = getContactChat(mSmsNumber, true);
            }
            else if (mSmsNumber != null)
            {
                contactLogger.debug("Getting MetaContact SMS chat for " +
                                     mMetaContact + " and number " +
                                     logHasher(mSmsNumber));
                chatPanel = getContactChat(mMetaContact, true);

                // Make sure we set the chat transport to the SMS transport for
                // the given number.
                ChatSession chatSession = chatPanel.getChatSession();
                ChatTransport chatTransport =
                         chatSession.findChatTransportForSmsNumber(mSmsNumber);

                if (chatTransport != null)
                {
                    contactLogger.debug("Setting SMS chat transport for " +
                                        logHasher(mSmsNumber));
                    chatSession.setCurrentChatTransport(chatTransport);
                    chatPanel.setSelectedChatTransport(chatTransport);
                }
            }
            else
            {
                contactLogger.debug("Getting MetaContact IM chat for " +
                                                mMetaContact);
                chatPanel = getContactChat(mMetaContact, mProtocolContact);
            }

            if (chatPanel == null)
            {
                sLog.error("Not opening chat as chat panel is null");
            }
            else if (chatPanel.getChatSession() == null)
            {
                sLog.error("Not opening chat: " + chatPanel +
                           " as chat session is null");
            }
            else if (chatPanel.getChatSession().getCurrentChatTransport() == null)
            {
                sLog.error("Not opening chat: " + chatPanel +
                           " with session: " + chatPanel.getChatSession() +
                           " as current transport is null");
            }
            else
            {
                chatPanel.setSmsSelected(mIsSmsSelected);
                // Open the chat but don't alert the window as the window is
                // opening because the user clicked to open it, rather than
                // because of a new message.
                openChat(chatPanel, true);
            }
        }
    }

    /**
     * Opens a group chat window
     */
    private class RunGroupChatWindow implements Runnable
    {
        /**
         * The chat room subject
         */
        private String mChatRoomSubject;

        /**
         * The chat room UID
         */
        private String mChatRoomUid;

        /**
         * Whether the user has previously left this chat room
         */
        private boolean mIsClosed;

        /**
         * The chat room to open
         */
        private ChatRoom mChatRoom;

        /**
         * Creates an instance of <tt>RunGroupChatWindow</tt>
         *
         * @param chatRoomUid the chat room UID
         * @param isClosed whether the user has previously left this chat room
         * @param chatRoomSubject the chat room subject
         */
        public RunGroupChatWindow(String chatRoomUid,
                                  boolean isClosed,
                                  String chatRoomSubject)
        {
            mChatRoomSubject = chatRoomSubject;
            mIsClosed = isClosed;
            mChatRoomUid = chatRoomUid;
        }

        /**
         * Opens a group chat window
         */
        public void run()
        {
            ChatPanel chatPanel;
            sLog.info("Trying to open chat panel for " + mChatRoomUid);

            ProtocolProviderService provider = AccountUtils.getImProvider();

            OperationSetMultiUserChat opSetMuc = provider == null ?
                null : provider.getOperationSet(OperationSetMultiUserChat.class);

            try
            {
                if (opSetMuc != null)
                {
                    mChatRoom = opSetMuc.findRoom(mChatRoomUid);
                }
                else
                {
                    sLog.error("Unable to open group chat " + mChatRoomUid +
                        " as failed to find MUC op set");
                }
            }
            catch (OperationFailedException ex)
            {
                sLog.error("Failed to find chat room on server: " + mChatRoomUid, ex);
            }
            catch (OperationNotSupportedException ex)
            {
                sLog.error("Failed to start chat room: " + mChatRoomUid, ex);
            }

            if (mChatRoom != null)
            {
                sLog.debug("Looking for chat panel for existing chat room");
                chatPanel = getMultiChat(mChatRoom, true);
                // Sometimes the server returns null, even when a subject has
                // been set, so don't update the subject if null is returned.
                String title = mChatRoom.getSubject();
                if (title != null)
                {
                    mChatRoomSubject = title;
                }
            }
            else if (mIsClosed || !provider.isRegistered())
            {
                sLog.debug(
                    "Creating new chat room wrapper for closed or offline chat room");
                ChatRoomList chatRoomList =
                    GuiActivator.getUIService().getConferenceChatManager().getChatRoomList();

                ChatRoomProviderWrapper parentProvider =
                    chatRoomList.findServerWrapperFromProvider(
                        AccountUtils.getImProvider());

                ChatRoomWrapper chatRoomWrapper =
                    new ChatRoomWrapper(
                        parentProvider, mChatRoomUid, mChatRoomUid, mChatRoomSubject);

                chatRoomWrapper.setPersistent(false);
                chatRoomList.addChatRoom(chatRoomWrapper);
                chatPanel = getMultiChat(chatRoomWrapper, true);
            }
            else
            {
                // We were unable to find a chat room for a room that we
                // haven't left, which is wrong.  Just log and return.
                sLog.error("We're online but chat room is null and not closed.");
                return;
            }

            if (chatPanel != null)
            {
                chatPanel.getChatContainer().setTitle(mChatRoomSubject);

                if (mIsClosed)
                {
                    chatPanel.setLeftChatRoomUI(true);
                }

                // Open the chat but don't alert the window as the window is
                // opening because the user clicked to open it, rather than
                // because of a new message.
                openChat(chatPanel, true);
            }
            else
            {
                sLog.error("No chat panel found for " + mChatRoomUid);
            }
        }
    }

    /**
     * Registers a <tt>NewChatListener</tt> to be informed when new <tt>Chats</tt>
     * are created.
     * @param listener listener to be registered
     */
    public void addChatListener(ChatListener listener)
    {
        synchronized (mChatListeners)
        {
            if (!mChatListeners.contains(listener))
                mChatListeners.add(listener);
        }
    }

    /**
     * Removes the registration of a <tt>NewChatListener</tt>.
     * @param listener listener to be unregistered
     */
    public void removeChatListener(ChatListener listener)
    {
        synchronized (mChatListeners)
        {
            mChatListeners.remove(listener);
        }
    }

    /**
     * Displays a custom warning message.
     *
     * @param resourceString The resource name of the message to display.
     * @param parentComponent Determines the Frame in which the dialog is
     * displayed; if null, or if the parentComponent has no Frame, a default
     * Frame is used
     *
     * @return The integer corresponding to the option choosen by the user.
     */
    private static int showWarningMessage(
            String resourceString,
            Component parentComponent)
    {
        SIPCommMsgTextArea msgText
            = new SIPCommMsgTextArea(
                    GuiActivator.getResources().getI18NString(resourceString));
        msgText.setForeground(Color.BLACK);
        JComponent textComponent = msgText;

        return JOptionPane.showConfirmDialog(
                parentComponent,
                textComponent,
                GuiActivator.getResources().getI18NString(
                    "service.gui.WARNING"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Runnable used as base for all that creates chat panels.
     */
    private abstract class AbstractChatPanelCreateRunnable
        implements Runnable
    {
        /**
         * The result panel.
         */
        private ChatPanel mChatPanel;

        /**
         * Returns the result chat panel.
         * @return the result chat panel.
         */
        public ChatPanel getChatPanel()
        {
            try
            {
                if (!SwingUtilities.isEventDispatchThread())
                     SwingUtilities.invokeAndWait(this);
                else
                    run();
            }
            catch(Throwable t)
            {
                sLog.warn("Cannot dispatch on event dispatch thread", t);
                // if we cannot execute on event dispatch thread
                run();
            }

            return mChatPanel;
        }

        /**
         * Runs on event dispatch thread.
         */
        public void run()
        {
            mChatPanel = createChatPanel();
        }

        /**
         * The method that will create the panel.
         * @return the result chat panel.
         */
        protected abstract ChatPanel createChatPanel();
    }

    /**
     * Creates/Obtains chat panel on swing event dispatch thread.
     */
    private class MetaContactChatCreateRunnable
        extends AbstractChatPanelCreateRunnable
    {
        /**
         * The source meta contact.
         */
        private final MetaContact mMetaContact;

        /**
         * The protocol contact used for creating chat panel.
         */
        private final Contact mProtocolContact;

        /**
         * The contact resource, from which the message is sent.
         */
        private final ContactResource mContactResource;

        /**
         * Creates a chat.
         *
         * @param metaContact the from meta contact
         * @param protocolContact the from protocol contact
         * @param contactResource the contact resource, from which the message
         * is sent
         */
        private MetaContactChatCreateRunnable(MetaContact metaContact,
                                              Contact protocolContact,
                                              ContactResource contactResource)
        {
            super();
            mMetaContact = metaContact;
            mProtocolContact = protocolContact;
            mContactResource = contactResource;

            if (mMetaContact == null)
                throw new NullPointerException("Metacontact can not be null");
        }

        /**
         * Runs on event dispatch thread.
         */
        protected ChatPanel createChatPanel()
        {
            return getContactChat(
                mMetaContact,
                mProtocolContact,
                mContactResource,
                true);
        }
    }

    /**
     * Creates chat room wrapper in event dispatch thread.
     */
    private class CreateChatRoomWrapperRunner
        extends AbstractChatPanelCreateRunnable
    {
        /**
         * The source chat room.
         */
        private ChatRoomWrapper mChatRoomWrapper;

        /**
         * Constructs.
         * @param chatRoomWrapper
         */
        private CreateChatRoomWrapperRunner(ChatRoomWrapper chatRoomWrapper)
        {
            super();
            mChatRoomWrapper = chatRoomWrapper;
        }

        /**
         * Runs on event dispatch thread.
         */
        protected ChatPanel createChatPanel()
        {
            return getMultiChatInternal(mChatRoomWrapper, true);
        }
    }

    /**
     * Creates chat room in event dispatch thread.
     */
    private class CreateChatRoomRunner
        extends AbstractChatPanelCreateRunnable
    {
        /**
         * The source chat room.
         */
        private ChatRoom mChatRoom;

        /**
         * Constructs.
         * @param chatRoom
         */
        private CreateChatRoomRunner(ChatRoom chatRoom)
        {
            super();
            mChatRoom = chatRoom;
        }

        /**
         * Runs on event dispatch thread.
         */
        protected ChatPanel createChatPanel()
        {
            return getMultiChatInternal(mChatRoom,
                true);
        }
    }
}
