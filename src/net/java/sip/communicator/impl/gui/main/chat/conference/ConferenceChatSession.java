/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat.conference;

import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * An implementation of <tt>ChatSession</tt> for conference chatting.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Valentin Martinet
 */
public class ConferenceChatSession
    extends ChatSession
    implements  ChatRoomMemberPresenceListener,
                ChatRoomPropertyChangeListener
{
    /**
     * The logger of this class.
     */
    private static final Logger logger
        = Logger.getLogger(ConferenceChatSession.class);

    /**
     * The current chat transport used for messaging.
     */
    private ChatTransport currentChatTransport;

    /**
     * The chat room wrapper, which is the descriptor of this chat session.
     */
    private final ChatRoomWrapper chatRoomWrapper;

    /**
     * The object used for rendering.
     */
    private final ChatSessionRenderer sessionRenderer;

    /**
     * The user's own chat user ID.
     */
    private final String userID;

    /**
     * The list of all <tt>ChatSessionChangeListener</tt>-s registered to listen
     * for transport modifications.
     */
    private final java.util.List<ChatSessionChangeListener>
        chatTransportChangeListeners
            = new Vector<>();

    /**
     * Creates an instance of <tt>ConferenceChatSession</tt>, by specifying the
     * sessionRenderer to be used for communication with the UI and the chatRoom
     * corresponding to this conference session.
     *
     * @param sessionRenderer the renderer to be used for communication with the
     * UI.
     * @param chatRoomWrapper the chat room corresponding to this conference
     * session.
     */
    public ConferenceChatSession(ChatSessionRenderer sessionRenderer,
                                 ChatRoomWrapper chatRoomWrapper)
    {
        this.sessionRenderer = sessionRenderer;
        this.chatRoomWrapper = chatRoomWrapper;

        currentChatTransport = new ConferenceChatTransport(chatRoomWrapper.getChatRoom());

        // Save our own user ID so that we can check whether chat room
        // property changed or member presence changed events have come from
        // our own account.
        ProtocolProviderService imProvider = AccountUtils.getImProvider();
        userID = (imProvider == null) ?
                                   "" : imProvider.getAccountID().getUserID();

        synchronized (chatTransports)
        {
            chatTransports.add(currentChatTransport);
        }

        // Add listeners and participants for this chat room.
        initChatRoom();
    }

    /**
     * Returns the descriptor of this chat session.
     *
     * @return the descriptor of this chat session.
     */
    public Object getDescriptor()
    {
        return chatRoomWrapper;
    }

    /**
     * Disposes this chat session.
     */
    public void dispose()
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
        if (chatRoom != null)
        {
            chatRoom.removeMemberPresenceListener(this);
            chatRoom.removePropertyChangeListener(this);
        }
    }

    /**
     * Returns the name of the chat room.
     *
     * @return the name of the chat room.
     */
    public String getChatName()
    {
        return chatRoomWrapper.getChatRoomName();
    }

    /**
     * Returns the subject of the chat room.
     *
     * @return the subject of the chat room.
     */
    public String getChatSubject()
    {
        return chatRoomWrapper.getChatRoomSubject();
    }

    /**
     * Sets the subject of the chat room.
     *
     * @param subject the subject of the chat room.
     * @throws OperationFailedException if the operation failed
     */
    public void setChatSubject(String subject) throws OperationFailedException
    {
        chatRoomWrapper.getChatRoom().setSubject(subject);
    }

    /**
     * Returns the currently used transport for all operation within this chat
     * session.
     *
     * @return the currently used transport for all operation within this chat
     * session.
     */
    public ChatTransport getCurrentChatTransport()
    {
        return currentChatTransport;
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     *
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistory(int count)
    {
        final MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return metaHistory.findLast(
            chatHistoryFilter,
            chatRoomWrapper,
            ConfigurationUtils.getChatHistorySize());
    }

    /**
     * Sets the transport that will be used for all operations within this chat
     * session.
     *
     * @param chatTransport The transport to set as a default transport for this
     * session.
     */
    public void setCurrentChatTransport(ChatTransport chatTransport)
    {
        this.currentChatTransport = chatTransport;
        for (ChatSessionChangeListener l : chatTransportChangeListeners)
        {
            l.currentChatTransportChanged(this);
        }
    }

    /**
     * Returns the <tt>ChatSessionRenderer</tt> that provides the connection
     * between this chat session and its UI.
     *
     * @return The <tt>ChatSessionRenderer</tt>.
     */
    public ChatSessionRenderer getChatSessionRenderer()
    {
        return sessionRenderer;
    }

    /**
     * Invoked when <tt>ChatRoomMemberPresenceChangeEvent</tt> are received.
     * When a new <tt>ChatRoomMember</tt> has joined the chat adds it to the
     * list of chat participants on the right of the chat window. When a
     * <tt>ChatRoomMember</tt> has left or quit, or has being kicked it's
     * removed from the chat window.
     * @param evt the <tt>ChatRoomMemberPresenceChangeEvent</tt> that notified
     * us
     */
    public void memberPresenceChanged(ChatRoomMemberPresenceChangeEvent evt)
    {
        ChatRoom sourceChatRoom = (ChatRoom) evt.getSource();

        if (!sourceChatRoom.equals(chatRoomWrapper.getChatRoom()))
            return;

        String eventType = evt.getEventType();
        ChatRoomMember chatRoomMember = evt.getChatRoomMember();

        if (eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED))
        {
            ConferenceChatContact chatContact = new ConferenceChatContact(chatRoomMember);
            logger.debug("Member joined: " + logHasher(chatContact.getName()) + "/" +
                sanitiseChatAddress(chatContact.getUID()));

            // Check if not ever present in the chat room. In some cases, the
            // considered chatroom member may appear twice in the chat contact
            // list panel.
            synchronized (chatParticipants)
            {
                if (!chatParticipants.contains(chatContact))
                {
                    chatParticipants.add(chatContact);
                }
            }
            sessionRenderer.addChatContact(chatContact);
        }
        else if (eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT)
            || eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED)
            || eventType.equals(ChatRoomMemberPresenceChangeEvent.MEMBER_QUIT))
        {
            String chatRoomMemberName = chatRoomMember.getName();
            String chatRoomMemberAddress = chatRoomMember.getContactAddressAsString();
            logger.debug(
                "Member left: " + logHasher(chatRoomMemberName) + "/" + sanitisePeerId(chatRoomMemberAddress));

            ChatContact<?> participantToRemove = null;
            synchronized (chatParticipants)
            {
                for (ChatContact<?> chatParticipant : chatParticipants)
                {
                    ChatRoomMember participantDescriptor =
                        (ChatRoomMember) chatParticipant.getDescriptor();

                    if (participantDescriptor.equals(chatRoomMember) ||
                        participantDescriptor.getContactAddressAsString().
                                         equalsIgnoreCase(chatRoomMemberAddress))
                    {
                        participantToRemove = chatParticipant;
                        break;
                    }
                }

                if (participantToRemove != null)
                {
                    boolean success = chatParticipants.remove(participantToRemove);
                    logger.debug("Just removed " + sanitiseChatAddress(participantToRemove.getUID()) +
                                 ", success = " + success);
                }
            }

            if (participantToRemove != null)
            {
                sessionRenderer.removeChatContact(participantToRemove);
            }

            // If we left the chat, close the window.
            if (((chatRoomMemberName != null) && chatRoomMemberName.equalsIgnoreCase(userID)) ||
                ((chatRoomMemberAddress != null) && chatRoomMemberAddress.equalsIgnoreCase(userID)))
            {
                logger.debug("We have left the chat - closing the chat window");
                ((ChatPanel) sessionRenderer).getChatContainer().getFrame().dispose();
            }
        }
    }

    public void chatRoomPropertyChangeFailed(
        ChatRoomPropertyChangeFailedEvent event) {}

    /**
     * Updates the chat panel when a property of the chat room has been modified.
     *
     * @param evt the event containing information about the property change
     */
    public void chatRoomPropertyChanged(ChatRoomPropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(
            ChatRoomPropertyChangeEvent.CHAT_ROOM_SUBJECT))
        {
            String newSubject = (String) evt.getNewValue();

            sessionRenderer.setChatSubject(newSubject);
        }
    }

    /**
     * Returns <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     */
    public boolean isDescriptorPersistent()
    {
        return true;
    }

    /**
     * Loads the given chat room in the 'this chat' conference panel. Loads all
     * members and adds all corresponding listeners.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to load
     */
    public void loadChatRoom(ChatRoom chatRoom)
    {
        logger.debug("Loading chat room: " +
                     sanitiseChatRoom(chatRoom.getIdentifier()));
        // Re-init the chat transport, as we have a new chat room object.
        currentChatTransport = new ConferenceChatTransport(chatRoomWrapper.getChatRoom());

        synchronized (chatTransports)
        {
            chatTransports.clear();
            chatTransports.add(currentChatTransport);
        }

        // Remove all existing contacts.
        sessionRenderer.removeAllChatContacts();

        // Add the new list of members.
        for (ChatRoomMember member : chatRoom.getMembers())
        {
            sessionRenderer.addChatContact(new ConferenceChatContact(member));
        }

        // Add all listeners to the new chat room.
        chatRoom.addPropertyChangeListener(this);
        chatRoom.addMemberPresenceListener(this);

        // Load the subject of the chat room.
        sessionRenderer.setChatSubject(chatRoom.getSubject());
    }

    /**
     * Returns the avatar icon of this chat session.
     *
     * @return the avatar icon of this chat session.
     */
    public BufferedImageFuture getChatAvatar()
    {
        return null;
    }

    /**
     * Initializes the chat room.
     */
    public void initChatRoom()
    {
        // If there is no chat room object in the wrapper, it means we've left
        // the room, and we won't receive any messages, so we don't need any
        // listeners.
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
        if (chatRoom != null)
        {
            logger.debug("Adding member presence listeners for: " +
                         sanitiseChatRoom(chatRoomWrapper.getChatRoomName()));
            chatRoom.addMemberPresenceListener(this);
            chatRoom.addPropertyChangeListener(this);
        }

        // Make sure we add chat participants who joined before we created our
        // listeners.
        synchronized (chatParticipants)
        {
            // Clear the participants list before we add the current members to
            // avoid duplicates if we are reinitialising the chat room.
            chatParticipants.clear();

            if ((chatRoom != null) && chatRoom.isJoined())
            {
                for (ChatRoomMember member : chatRoom.getMembers())
                {
                    logger.debug("Adding chat participant for " +
                                 sanitisePeerId(member.getContactAddressAsString()));
                    chatParticipants.add(new ConferenceChatContact(member));
                }
            }
            else
            {
                logger.info("Not adding participants, not joined chat room yet");
            }
        }

        // Tell the chat panel to update the UI to include these participants.
        sessionRenderer.refreshAllChatContacts();
    }

    /**
     * Adds the given <tt>ChatSessionChangeListener</tt> to the list of
     * transport listeners.
     * @param l the listener to add
     */
    public void addChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (chatTransportChangeListeners)
        {
            if (!chatTransportChangeListeners.contains(l))
                chatTransportChangeListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>ChatSessionChangeListener</tt> from contained
     * transport listeners.
     * @param l the listener to remove
     */
    public void removeChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (chatTransportChangeListeners)
        {
            chatTransportChangeListeners.remove(l);
        }
    }

    @Override
    protected void addSMSChatTransports()
    {
        // Group chats don't support SMS so nothing to do here.
    }

    @Override
    protected void removeSMSChatTransports()
    {
        // Group chats don't support SMS so nothing to do here.
    }
}
