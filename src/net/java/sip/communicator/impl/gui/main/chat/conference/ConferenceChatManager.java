/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;
import java.util.concurrent.*;

import javax.swing.SwingWorker;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms.*;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ConferenceChatManager</tt> is the one that manages both chat room and
 * ad-hoc chat rooms invitations.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Valentin Martinet
 */
public class ConferenceChatManager
    implements  ChatRoomMessageListener,
                ChatRoomInvitationListener,
                ChatRoomInvitationRejectionListener,
                LocalUserChatRoomPresenceListener,
                ServiceListener
{
    private static final Logger logger = Logger.getLogger(ConferenceChatManager.class);

    /**
     * The list of persistent chat rooms.
     */
    private final ChatRoomList chatRoomList = new ChatRoomList();

    /**
     * Creates an instance of <tt>ConferenceChatManager</tt>.
     */
    public ConferenceChatManager()
    {
        // Loads the chat rooms list in a separate thread.
        new Thread("ConferenceChatManager load chat rooms list")
        {
            public void run()
            {
                chatRoomList.loadList();
            }
        }.start();

        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Returns all chat room providers currently contained in the chat room
     * list.
     * @return  all chat room providers currently contained in the chat room
     * list.
     */
    public ChatRoomList getChatRoomList()
    {
        return chatRoomList;
    }

    /**
     * Handles <tt>ChatRoomInvitationReceivedEvent</tt>-s.
     */
    public void invitationReceived(ChatRoomInvitationReceivedEvent evt)
    {
        ChatRoomInvitation invitation = evt.getInvitation();
        String chatRoomId = invitation.getTargetChatRoom().getIdentifier().toString();
        logger.debug("Auto-accepting group chat invitation " + chatRoomId);

        // Send an analytic of us auto-accepting a group chat invite,
        // with the number of active group chats as a parameter.
        List<AnalyticsParameter> params = new ArrayList<>();

        params.add(new AnalyticsParameterSimple(
            AnalyticsParameter.NAME_COUNT_GROUP_IMS,
            String.valueOf(evt.getSourceOperationSet().getActiveChatRooms())));

        GuiActivator.getAnalyticsService().onEvent(
            AnalyticsEventType.ACCEPT_GROUP_IM, params);

        acceptInvitation(invitation);
    }

    public void invitationRejected(ChatRoomInvitationRejectedEvent evt) {}

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDelivered</tt> method.
     * <br>
     * Shows the message in the conversation area and clears the write message
     * area.
     * @param evt the <tt>ChatRoomMessageDeliveredEvent</tt> that notified us
     * that the message was delivered to its destination
     */
    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {
        // Ignore historical messages
        if (evt.getSourceMessage().isArchive())
            return;

        ChatRoom sourceChatRoom = evt.getChatRoom();

        logger.trace("MESSAGE DELIVERED to chat room: " + sourceChatRoom.getIdentifier());

        ChatPanel chatPanel = GuiActivator.getUIService().getChatWindowManager()
            .getMultiChat(sourceChatRoom, false, true);

        if (chatPanel != null)
        {
            String messageType;

            switch (evt.getEventType())
            {
            case MessageEvent.GROUP_MESSAGE:
                messageType = Chat.OUTGOING_MESSAGE;
                break;
            case MessageEvent.STATUS_MESSAGE:
                messageType = Chat.STATUS_MESSAGE;
                break;
            case MessageEvent.ACTION_MESSAGE:
                messageType = Chat.ACTION_MESSAGE;
                break;
            default:
                messageType = null;
                break;
            }

            ImMessage msg = evt.getSourceMessage();

            chatPanel.addMessage(
                evt.getContactAddress(),
                evt.getContactDisplayName(),
                evt.getTimestamp(),
                messageType,
                msg.getContent(),
                msg.getContentType(),
                msg.getMessageUID(),
                null,
                msg.isArchive(),
                msg.isCarbon());
        }
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageReceived</tt> method.
     * <br>
     * Obtains the corresponding <tt>ChatPanel</tt> and process the message
     * there.
     * @param evt the <tt>ChatRoomMessageReceivedEvent</tt> that notified us
     * that a message has been received
     */
    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        // Ignore historical messages
        if (evt.getSourceMessage().isArchive())
            return;

        ChatRoom sourceChatRoom = evt.getChatRoom();

        String messageType = null;

        switch (evt.getEventType())
        {
        case MessageEvent.GROUP_MESSAGE:
            GuiActivator.getAnalyticsService().onEventWithIncrementingCount(AnalyticsEventType.RECEIVE_GROUP_IM, new ArrayList<>());
            messageType = Chat.INCOMING_MESSAGE;
            break;
        case MessageEvent.STATUS_MESSAGE:
            messageType = Chat.STATUS_MESSAGE;
            break;
        case MessageEvent.SYSTEM_MESSAGE:
            messageType = Chat.SYSTEM_MESSAGE;
            break;
        case MessageEvent.ACTION_MESSAGE:
            messageType = Chat.ACTION_MESSAGE;
            break;
        }

        String contactAddress = evt.getContactAddress();
        logger.trace("MESSAGE RECEIVED from contact: " + logHasher(contactAddress));

        ImMessage message = evt.getSourceMessage();

        ChatPanel chatPanel = null;

        ChatWindowManager chatWindowManager = GuiActivator.getUIService().getChatWindowManager();

        chatPanel = chatWindowManager.getMultiChat(sourceChatRoom, true, true);

        String messageContent = message.getContent();

        chatPanel.addMessage(
            contactAddress,
            evt.getContactDisplayName(),
            evt.getTimestamp(),
            messageType,
            messageContent,
            message.getContentType(),
            message.getMessageUID(),
            null,
            message.isArchive(),
            message.isCarbon());

        // Only open the chat window for new chat messages (i.e. not for
        // presence updates from chat room members or for chat room name
        // changes).
        boolean isIncomingMessage = Chat.INCOMING_MESSAGE.equals(messageType);
        boolean openWindowEnabled = ConfigurationUtils.isOpenWindowOnNewChatEnabled();
        if (isIncomingMessage && openWindowEnabled)
        {
            // Don't alert the chat window if the chat is muted.
            if (sourceChatRoom.isMuted())
            {
                chatWindowManager.openChat(chatPanel, false);
            }
            else
            {
                chatWindowManager.openChatAndAlertWindow(chatPanel, false);
            }
        }
        else
        {
            logger.debug("Not opening chat window: isIncomingMessage = " + isIncomingMessage + ", openWindowEnabled = " + openWindowEnabled);
        }
    }

    /**
     * Determines whether a specific <code>ChatRoom</code> is private i.e.
     * represents a one-to-one conversation which is not a channel. Since the
     * interface {@link ChatRoom} does not expose the private property, an
     * heuristic is used as a workaround: (1) a system <code>ChatRoom</code> is
     * obviously not private and (2) a <code>ChatRoom</code> is private if it
     * has only one <code>ChatRoomMember</code> who is not the local user.
     *
     * @param chatRoom
     *            the <code>ChatRoom</code> to be determined as private or not
     * @return <tt>true</tt> if the specified <code>ChatRoom</code> is private;
     *         otherwise, <tt>false</tt>
     */
    static boolean isPrivate(ChatRoom chatRoom)
    {
        if (chatRoom.isJoined() && (chatRoom.getMembersCount() == 1))
        {
            String nickname = chatRoom.getUserNickname();

            if (nickname != null)
            {
                for (ChatRoomMember member : chatRoom.getMembers())
                {
                    if (nickname.equals(member.getName()))
                    {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Implements the <tt>ChatRoomMessageListener.messageDeliveryFailed</tt>
     * method.
     * <br>
     * In the conversation area shows an error message, explaining the problem.
     * @param evt the <tt>ChatRoomMessageDeliveryFailedEvent</tt> that notified
     * us of a delivery failure
     */
    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {
        // Ignore historical messages
        if (evt.getSourceMessage().isArchive())
        {
            return;
        }

        ChatRoom sourceChatRoom = evt.getChatRoom();
        String errorMsg = null;

        /*
         * FIXME ChatRoomMessageDeliveryFailedEvent#getSource() is not a Message
         * instance at the time of this writing and the attempt "(Message)
         * evt.getSource()" seems to be to get the message which failed to be
         * delivered. I'm not sure it's
         * ChatRoomMessageDeliveryFailedEvent#getMessage() but since it's the
         * only message I can get out of ChatRoomMessageDeliveryFailedEvent, I'm
         * using it.
         */
        ImMessage sourceMessage = evt.getSourceMessage();

        ChatRoomMember destMember = evt.getChatRoomMember();

        if (evt.getErrorCode() == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED)
        {
            errorMsg = GuiActivator.getResources().getI18NString("service.gui.MSG_DELIVERY_NOT_SUPPORTED", new String[]{destMember.getName()});
        }
        else if (evt.getErrorCode() == MessageDeliveryFailedEvent.NETWORK_FAILURE)
        {
            errorMsg = GuiActivator.getResources().getI18NString("service.gui.MSG_NOT_DELIVERED");
        }
        else if (evt.getErrorCode() == MessageDeliveryFailedEvent.PROVIDER_NOT_REGISTERED)
        {
            errorMsg = GuiActivator.getResources().getI18NString("service.gui.MSG_SEND_CONNECTION_PROBLEM");
        }
        else if (evt.getErrorCode() == MessageDeliveryFailedEvent.INTERNAL_ERROR)
        {
            errorMsg = GuiActivator.getResources().getI18NString("service.gui.MSG_DELIVERY_INTERNAL_ERROR");
        }
        else
        {
            errorMsg = GuiActivator.getResources().getI18NString("service.gui.MSG_DELIVERY_UNKNOWN_ERROR");
        }

        // Log the error message and internal reason to file
        logger.debug(errorMsg + evt.getReason());

        ChatWindowManager chatWindowManager = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel = chatWindowManager.getMultiChat(sourceChatRoom, true, true);

        chatPanel.addMessage(
            evt.getContactAddress(),
            evt.getContactDisplayName(),
            new Date(),
            Chat.OUTGOING_MESSAGE,
            sourceMessage.getContent(),
            sourceMessage.getContentType(),
            sourceMessage.getMessageUID(),
            null,
            sourceMessage.isArchive(),
            sourceMessage.isCarbon());

        chatPanel.addErrorMessage(destMember.getName(), errorMsg);

        if (ConfigurationUtils.isOpenWindowOnNewChatEnabled())
        {
            chatWindowManager.openChatAndAlertWindow(chatPanel, false);
        }
        else
        {
            logger.debug("Configured not to open window on new chat");
        }
    }

    /**
     * Implements the
     * <tt>LocalUserChatRoomPresenceListener.localUserPresenceChanged</tt>
     * method.
     * @param evt the <tt>LocalUserChatRoomPresenceChangeEvent</tt> that
     * notified us
     */
    public void localUserPresenceChanged(LocalUserChatRoomPresenceChangeEvent evt)
    {
        ChatRoom sourceChatRoom = evt.getChatRoom();
        ChatRoomWrapper chatRoomWrapper = chatRoomList.findChatRoomWrapperFromChatRoom(sourceChatRoom);

        String eventType = evt.getEventType();
        logger.debug("Local user presence changed to " + eventType + " in " + sourceChatRoom.getIdentifier());
        if (LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED.equals(eventType))
        {
            if (chatRoomWrapper != null)
            {
                ChatWindowManager chatWindowManager = GuiActivator.getUIService().getChatWindowManager();
                ChatPanel chatPanel = chatWindowManager.getMultiChat(chatRoomWrapper, false, true);

                // Check if we have already opened a chat window for this chat
                // wrapper and load the real chat room corresponding to the
                // wrapper.
                if (chatPanel != null && chatPanel.isShown())
                {
                    ((ConferenceChatSession) chatPanel.getChatSession()).loadChatRoom(sourceChatRoom);
                }
            }

            sourceChatRoom.addMessageListener(this);
        }
        else if (LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOIN_FAILED.equals(eventType))
        {
            logger.error("Failed to join chat room: " + evt.getReason());
        }
        else if (LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT.equals(eventType))
        {
            // If we have a chat room wrapper for this room it means that some
            // UI is open relating to it.  In that case, make sure we tell the
            // UI that we're no longer in the chat room so it will be updated.
            if (chatRoomWrapper != null)
            {
                closeChatRoom(chatRoomWrapper);
            }

            sourceChatRoom.removeMessageListener(this);
        }
        else if (LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED.equals(eventType) ||
                 LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED.equals(eventType))
        {
            if (chatRoomWrapper != null)
            {
                ChatWindowManager chatWindowManager = GuiActivator.getUIService().getChatWindowManager();
                ChatPanel chatPanel = chatWindowManager.getMultiChat(chatRoomWrapper, false, false);

                // Check if we have already opened a chat window for this chat
                // wrapper and set the left chat room UI if so.
                if (chatPanel != null && chatPanel.isShown())
                {
                    chatPanel.setLeftChatRoomUI(true);
                }
            }

            sourceChatRoom.removeMessageListener(this);
        }
    }

    /**
     * Called to accept an incoming invitation. Adds the invitation chat room
     * to the list of chat rooms and joins it.
     *
     * @param invitation the invitation to accept.
     */
    public void acceptInvitation(ChatRoomInvitation invitation)
    {
        ChatRoom chatRoom = invitation.getTargetChatRoom();
        byte[] password = invitation.getChatRoomPassword();

        // Pass in null as the nickname so we automatically use the default for
        // the protocol provider.
        joinChatRoom(chatRoom, null, password);
    }

    /**
     * Joins the given chat room with the given password and manages all the
     * exceptions that could occur during the join process.
     *
     * @param chatRoomWrapper the chat room to join.
     * @param nickName the nickname we choose for the given chat room.
     * @param password the password.
     */
    public void joinChatRoom(ChatRoomWrapper chatRoomWrapper,
                             String nickName,
                             byte[] password)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if (chatRoom == null)
        {
            logger.error("Failed to join chat room: " +
                         GuiActivator.getResources().getI18NString(
                              "service.gui.CHAT_ROOM_NOT_CONNECTED",
                              new String[]{chatRoomWrapper.getChatRoomName()}));

            return;
        }

        new JoinChatRoomTask(chatRoomWrapper, nickName, password).execute();
    }

    /**
     * Creates a chat room, by specifying the chat room name, the parent
     * protocol provider and eventually, the contacts invited to participate in
     * this chat room.
     *
     * @param protocolProvider the parent protocol provider.
     * @param contacts the contacts invited when creating the chat room.
     * @param reason
     * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
     */
    public ChatRoomWrapper createChatRoom(
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason)
    {
        return this.createChatRoom(null, protocolProvider, contacts, reason);
    }

    /**
     * Creates a chat room, by specifying the chat room name, the parent
     * protocol provider and eventually, the contacts invited to participate in
     * this chat room.
     *
     * @param roomName the name of the room
     * @param protocolProvider the parent protocol provider.
     * @param contacts the contacts invited when creating the chat room.
     * @param reason
     * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
     */
    public ChatRoomWrapper createChatRoom(
        String roomName,
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason)
    {
        return createChatRoom(roomName, protocolProvider, contacts, reason, true, true);
    }

    /**
     * Creates a chat room, by specifying the chat room name, the parent
     * protocol provider and eventually, the contacts invited to participate in
     * this chat room.
     *
     * @param roomName the name of the room
     * @param protocolProvider the parent protocol provider.
     * @param contacts the contacts invited when creating the chat room.
     * @param reason
     * @param join whether we should join the room after creating it.
     * @param persistent whether the newly created room will be persistent.
     * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
     */
    public ChatRoomWrapper createChatRoom(
        String roomName,
        ProtocolProviderService protocolProvider,
        Collection<String> contacts,
        String reason,
        boolean join,
        boolean persistent)
    {
        ChatRoomWrapper chatRoomWrapper = null;

        OperationSetMultiUserChat groupChatOpSet
            = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        // If there's no group chat operation set we have nothing to do here.
        if (groupChatOpSet == null)
        {
            return null;
        }

        ChatRoom chatRoom = null;
        try
        {
            chatRoom = groupChatOpSet.createChatRoom(roomName, null);
            GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.NEW_GROUP_CHAT);
            if (join)
            {
                chatRoom.join();

                for (String contact : contacts)
                {
                    chatRoom.invite(contact, reason);
                }
            }
        }
        catch (OperationFailedException | OperationNotSupportedException ex)
        {
            logger.error("Failed to create chat room.", ex);
        }

        if (chatRoom != null)
        {
            ChatRoomProviderWrapper parentProvider = chatRoomList.findServerWrapperFromProvider(protocolProvider);

            // if there is the same room ids don't add new wrapper as old one
            // maybe already created
            chatRoomWrapper = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

            if (chatRoomWrapper == null)
            {
                chatRoomWrapper = new ChatRoomWrapper(parentProvider, chatRoom);
                chatRoomWrapper.setPersistent(persistent);
                chatRoomList.addChatRoom(chatRoomWrapper);
            }
        }

        return chatRoomWrapper;
    }

    /**
     * Join chat room.
     * @param chatRoomWrapper
     */
    public void joinChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if (chatRoom == null)
        {
            logger.error("Failed to join chat room: " +
                          GuiActivator.getResources().getI18NString(
                              "service.gui.CHAT_ROOM_NOT_CONNECTED",
                              new String[]{chatRoomWrapper.getChatRoomName()}));
            return;
        }

        new JoinChatRoomTask(chatRoomWrapper, null, null).execute();
    }

    /**
     * Joins the given chat room and manages all the exceptions that could
     * occur during the join process.
     *
     * @param chatRoom the chat room to join
     */
    public void joinChatRoom(ChatRoom chatRoom)
    {
        ChatRoomWrapper chatRoomWrapper = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if (chatRoomWrapper == null)
        {
            ChatRoomProviderWrapper parentProvider = chatRoomList.findServerWrapperFromProvider(chatRoom.getParentProvider());
            chatRoomWrapper = new ChatRoomWrapper(parentProvider, chatRoom);
            chatRoomList.addChatRoom(chatRoomWrapper);
        }

        this.joinChatRoom(chatRoomWrapper);
    }

    /**
     * Joins the given chat room and manages all the exceptions that could
     * occur during the join process.
     *
     * @param chatRoom the chat room to join
     * @param nickname the nickname we're using to join
     * @param password the password we're using to join
     */
    public void joinChatRoom(ChatRoom chatRoom,
                             String nickname,
                             byte[] password)
    {
        ChatRoomWrapper chatRoomWrapper = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if (chatRoomWrapper == null)
        {
            ChatRoomProviderWrapper parentProvider = chatRoomList.findServerWrapperFromProvider(chatRoom.getParentProvider());
            chatRoomWrapper = new ChatRoomWrapper(parentProvider, chatRoom);
            chatRoomList.addChatRoom(chatRoomWrapper);
        }

        // Make sure we don't auto-join chat rooms, as we only want to
        // automatically join chat rooms if we find them in the roster.
        chatRoomWrapper.setAutoJoin(false);

        this.joinChatRoom(chatRoomWrapper, nickname, password);
    }

    /**
     * Leaves the given <tt>ChatRoom</tt>.
     *
     * @param chatRoomWrapper the <tt>ChatRoom</tt> to leave.
     */
    public void leaveChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

        if (chatRoom == null)
        {
            logger.error("Failed to leave chat room, it doesn't exist");
            return;
        }

        if (chatRoom.isJoined())
        {
            // Leave the chat room and send a 'special' message to notify the
            // other members of the chat room that we are leaving permanently.
            try
            {
                chatRoom.leave(true, LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT, null);
            }
            catch (IllegalStateException e)
            {
                // If we fail to leave, display a popup saying to try again.
                logger.error("Failed to leave chat room", e);
                String errorTitle = GuiActivator.getResources().getI18NString("service.gui.chat.FAILED_TO_LEAVE_GROUP_CHAT_TITLE");
                String errorMessage = GuiActivator.getResources().getI18NString("service.gui.chat.FAILED_TO_LEAVE_GROUP_CHAT");
                ErrorDialog errorDialog = new ErrorDialog(null, errorTitle, errorMessage);
                errorDialog.showDialog();
                return;
            }
        }
        else
        {
            logger.warn("User tried to leave chat room that Jabber claims they're not in");
        }

        ChatRoomWrapper existChatRoomWrapper = chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);
        closeChatRoom(existChatRoomWrapper);
    }

    /**
     * Closes the chat corresponding to the given chat room wrapper, if such
     * exists.
     *
     * @param chatRoomWrapper the chat room wrapper for which we search a chat
     * to close.
     */
    private void closeChatRoom(ChatRoomWrapper chatRoomWrapper)
    {
        if (chatRoomWrapper == null)
        {
            logger.warn("Unable to close chat room - chatRoomWrapper is null");
            return;
        }

        ChatWindowManager chatWindowManager = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel = chatWindowManager.getMultiChat(chatRoomWrapper, false, false);

        if (chatPanel != null)
        {
            chatWindowManager.closeChat(chatPanel);
        }
        else
        {
            logger.debug("Unable to close chat room as no chat panel found for " + chatRoomWrapper.getChatRoomID());
        }
    }

    /**
     * Handles <tt>ServiceEvent</tt>s triggered by adding or removing a
     * ProtocolProviderService. Updates the list of available chat rooms and
     * chat room servers.
     *
     * @param event The event to handle.
     */
    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object service = GuiActivator.bundleContext.getService(event.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        ProtocolProviderService protocolProvider = (ProtocolProviderService) service;

        Object multiUserChatOpSet = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        if (multiUserChatOpSet != null)
        {
             if (event.getType() == ServiceEvent.REGISTERED)
             {
                 chatRoomList.addChatProvider(protocolProvider);
             }
             else if (event.getType() == ServiceEvent.UNREGISTERING)
             {
                 chatRoomList.removeChatProvider(protocolProvider);
             }
        }
    }

    /**
     * Joins a chat room in an asynchronous way.
     */
    private static class JoinChatRoomTask extends SwingWorker<String, Object>
    {
        private static final String SUCCESS = "Success";
        private static final String AUTHENTICATION_FAILED = "AuthenticationFailed";
        private static final String REGISTRATION_REQUIRED = "RegistrationRequired";
        private static final String PROVIDER_NOT_REGISTERED = "ProviderNotRegistered";
        private static final String SUBSCRIPTION_ALREADY_EXISTS = "SubscriptionAlreadyExists";
        private static final String UNKNOWN_ERROR = "UnknownError";
        private final ChatRoomWrapper chatRoomWrapper;
        private final String nickName;
        private final byte[] password;

        JoinChatRoomTask(ChatRoomWrapper chatRoomWrapper,
                         String nickName,
                         byte[] password)
        {
            this.chatRoomWrapper = chatRoomWrapper;
            this.nickName = nickName;
            this.password = password;
        }

        /**
         * @override {@link SwingWorker}{@link #doInBackground()} to perform
         * all asynchronous tasks.
         * @return SUCCESS if success, otherwise the error code
         */
        public String doInBackground()
        {
            ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

            try
            {
                if (password != null && password.length > 0)
                {
                    chatRoom.joinAs(nickName, password);
                }
                else if (nickName != null)
                {
                    chatRoom.joinAs(nickName);
                }
                else
                {
                    chatRoom.join();
                }

                return SUCCESS;
            }
            catch (OperationFailedException e)
            {
                logger.trace("Failed to join chat room: " + chatRoom.getIdentifier(), e);

                switch (e.getErrorCode())
                {
                case OperationFailedException.AUTHENTICATION_FAILED:
                    return AUTHENTICATION_FAILED;
                case OperationFailedException.REGISTRATION_REQUIRED:
                    return REGISTRATION_REQUIRED;
                case OperationFailedException.PROVIDER_NOT_REGISTERED:
                    return PROVIDER_NOT_REGISTERED;
                case OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS:
                    return SUBSCRIPTION_ALREADY_EXISTS;
                default:
                    return UNKNOWN_ERROR;
                }
            }
        }

        /**
         * @override {@link SwingWorker}{@link #done()} to perform UI changes
         * after the chat room join task has finished.
         */
        protected void done()
        {
            String returnCode = null;
            try
            {
                returnCode = get();
            }
            catch (InterruptedException | ExecutionException ignore)
            {}

            ConfigurationUtils.updateChatRoomStatus(
                chatRoomWrapper.getParentProvider().getProtocolProvider(),
                chatRoomWrapper.getChatRoomID(),
                GlobalStatusEnum.ONLINE_STATUS);

            String errorMessage = null;
            if (AUTHENTICATION_FAILED.equals(returnCode))
            {
                ChatRoomAuthenticationWindow authWindow = new ChatRoomAuthenticationWindow(chatRoomWrapper);
                authWindow.setVisible(true);
            }
            else if (REGISTRATION_REQUIRED.equals(returnCode))
            {
                errorMessage = GuiActivator.getResources()
                        .getI18NString(
                            "service.gui.CHAT_ROOM_REGISTRATION_REQUIRED",
                            new String[]{chatRoomWrapper.getChatRoomName()});
            }
            else if (PROVIDER_NOT_REGISTERED.equals(returnCode))
            {
                errorMessage = GuiActivator.getResources()
                        .getI18NString("service.gui.CHAT_ROOM_NOT_CONNECTED",
                        new String[]{chatRoomWrapper.getChatRoomName()});
            }
            else if (SUBSCRIPTION_ALREADY_EXISTS.equals(returnCode))
            {
                errorMessage = GuiActivator.getResources()
                        .getI18NString("service.gui.CHAT_ROOM_ALREADY_JOINED",
                            new String[]{chatRoomWrapper.getChatRoomName()});
            }
            else
            {
                errorMessage = GuiActivator.getResources()
                        .getI18NString("service.gui.FAILED_TO_JOIN_CHAT_ROOM",
                            new String[]{chatRoomWrapper.getChatRoomName()});
            }

            if (!SUCCESS.equals(returnCode)
                    && !AUTHENTICATION_FAILED.equals(returnCode))
            {
                logger.error("Failed to join chat room: " + errorMessage);
            }
        }
    }
}
