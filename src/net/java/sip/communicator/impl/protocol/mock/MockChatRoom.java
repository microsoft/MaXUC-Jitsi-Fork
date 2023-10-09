/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 *
 * @author Damian Minkov
 */
public class MockChatRoom
    implements ChatRoom
{
    private MockProvider provider;

    private MockMultiUserChat parentOpSet = null;

    private EntityBareJid identifier;

    private String subject;

    private String nickname;

    private boolean joined = false;

    private List<ChatRoomMember> members = new Vector<>();

    /**
     * Currently registered member presence listeners.
     */
    private final List<ChatRoomMemberPresenceListener> memberPresenceListeners
        = new Vector<>();

    /**
     * Currently registered property change listeners.
     */
    private final List<ChatRoomPropertyChangeListener> propertyChangeListeners
        = new Vector<>();

    /**
     * Currently registered message listeners.
     */
    private final List<ChatRoomMessageListener> messageListeners
        = new Vector<>();

    /**
     * Creates an instance of <tt>MockChatRoom</tt> by specifying the
     * corresponding protocol provider, the multi user chat operation set
     * and the room name.
     * @param provider the corresponding <tt>ProtocolProviderService</tt>
     * @param parentOpSet the corresponding <tt>OperationSetMultiUserChat</tt>
     * @param roomName the name of the room
     * @throws XmppStringprepException
     */
    public MockChatRoom(MockProvider provider,
                        MockMultiUserChat parentOpSet,
                        String roomName)
    {
        try
        {
            this.identifier = JidCreate.entityBareFrom(roomName);
        }
        catch (XmppStringprepException e)
        {
            throw new IllegalArgumentException("Roomname was not an EntityBareJid!: " +
                roomName, e);
        }

        this.provider = provider;
        this.parentOpSet = parentOpSet;
    }

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join()
        throws OperationFailedException
    {
        joinAs(null, null, null);
    }

    /**
     * Joins this chat room so that the user would start receiving events and
     * messages for it. The method uses the nickname of the local user and the
     * specified password in order to enter the chatroom.
     *
     * @param password the password to use when authenticating on the chatroom.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join(byte[] password)
        throws OperationFailedException
    {
        joinAs(null, password, null);
    }

    /**
     * Joins this chat room with the specified nickname so that the user would
     * start receiving events and messages for it. If the chatroom already
     * contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void joinAs(String nickname)
        throws OperationFailedException
    {
        joinAs(nickname, null, null);
    }

    /**
     * Joins this chat room with the specified nickname and password so that the
     * user would start receiving events and messages for it. If the chatroom
     * already contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     * @param password a password necessary to authenticate when joining the
     * room.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void joinAs(String nickname, byte[] password)
        throws OperationFailedException
    {
        joinAs(nickname, password, null);
    }

    /**
     * Joins this chat room with the specified nickname and password so that the
     * user would start receiving events and messages for it. If the chatroom
     * already contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     * @param password a password necessary to authenticate when joining the
     * room.
     */
    public void joinAs(String nickname, byte[] password, Date joinDate)
    {
        if(nickname == null)
            nickname = getParentProvider().getAccountID().getUserID();

        this.nickname = nickname;
        this.joined = true;

        Contact mockContact = new MockContact(nickname,
            (MockProvider) getParentProvider());

        MockChatRoomMember newMember =
            new MockChatRoomMember( nickname,
                                    this,
                                    ChatRoomMemberRole.MEMBER,
                                    mockContact,
                                    null);

        members.add(newMember);

        parentOpSet
            .fireLocalUserPresenceEvent(
                this,
                null,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED, null);
    }

    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join()} methods).
     *
     * @return true if currently we're currently in this chat room and false
     * otherwise.
     */
    public boolean isJoined()
    {
        return joined;
    }

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     */
    public void leave(String eventType, String reason)
    {
        leave(false, new Date(), eventType, reason);
    }

    @Override
    public void leave(boolean sendMessage, Date leaveDate, String eventType, String reason)
    {
    }

    /**
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     * <p>
     * To be notified every time the room's subject change you should add a
     * <tt>ChatRoomChangelistener</tt> to this room.
     * {@link #addPropertyChangeListener(ChatRoomPropertyChangeListener)}
     * <p>
     * To change the room's subject use {@link #setSubject(String)}.
     *
     * @return the room subject or <tt>null</tt> if the user hasn't joined the
     * room or the room does not have a subject yet.
     */
    public String getSubject()
    {
        return subject;
    }

    /**
     * Sets the subject of this chat room. If the user does not have the right
     * to change the room subject, or the protocol does not support this, or
     * the operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     *
     * @param subject the new subject that we'd like this room to have
     */
    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     * context of the local chat room.
     */
    public String getUserNickname()
    {
        return nickname;
    }

    /**
     * Changes the local user's nickname in the context of this chatroom.
     * If the operation is not supported by the underlying implementation, the
     * method throws an OperationFailedException with the corresponding code.
     *
     * @param nickname the new nickname within the room.
     *
     */
    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    /**
     * Adds a listener that will be notified of changes in our participation in
     * the room such as us being kicked, join, left...
     *
     * @param listener a member participation listener.
     */
    public void addMemberPresenceListener(
        ChatRoomMemberPresenceListener listener)
    {
        if(!memberPresenceListeners.contains(listener))
            memberPresenceListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes in the
     * participation of other chat room participants such as users being kicked,
     * join, left.
     *
     * @param listener a member participation listener.
     */
    public void removeMemberPresenceListener(
        ChatRoomMemberPresenceListener listener)
    {
        memberPresenceListeners.remove(listener);
    }

    /**
     * Adds a listener that will be notified of changes in the property of the
     * room such as the subject being change or the room state being changed.
     *
     * @param listener a property change listener.
     */
    public void addPropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        if(!propertyChangeListeners.contains(listener))
            propertyChangeListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes in the property of
     * the chat room such as the subject being change or the room state being
     * changed.
     *
     * @param listener a property change listener.
     */
    public void removePropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        propertyChangeListeners.remove(listener);
    }

    /**
     * Invites another user to this room.
     * <p>
     * If the room is password-protected, the invitee will receive a password to
     * use to join the room. If the room is members-only, the invitee may
     * be added to the member list.
     *
     * @param userAddress the address of the user to invite to the room.(one
     * may also invite users not on their contact list).
     * @param reason a reason, subject, or welcome message that would tell the
     * the user why they are being invited.
     */
    public void invite(String userAddress, String reason)
    {
    }

    /**
     * Returns a <tt>List</tt> of <tt>ChatRoomMember</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>ChatRoomMember</tt> instances
     * corresponding to all room members.
     */
    public List<ChatRoomMember> getMembers()
    {
        return members;
    }

    /**
     * Returns the number of participants that are currently in this chat room.
     * @return int the number of <tt>Contact</tt>s, currently participating in
     * this room.
     */
    public int getMembersCount()
    {
        return members.size();
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     * @param listener a <tt>MessageListener</tt> that would be notified every
     * time a new message is received on this chat room.
     */
    public void addMessageListener(ChatRoomMessageListener listener)
    {
        if(!messageListeners.contains(listener))
            messageListeners.add(listener);
    }

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(ChatRoomMessageListener listener)
    {
        messageListeners.remove(listener);
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now subject.
     * @return the newly created message.
     */
    public ImMessage createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MockMessage(new String(content), contentType,
            contentEncoding, subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public ImMessage createMessage(String messageText, String messageUid)
    {
        return new MockMessage(messageText);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    public void sendMessage(ImMessage message)
        throws OperationFailedException
    {
        sendMessage(message, true);
    }

    @Override
    public void sendMessage(ImMessage message, boolean fireEvent)
    {
        ChatRoomMessageDeliveredEvent evt =
            new ChatRoomMessageDeliveredEvent(
                    message,
                    this,
                    getIdentifier().toString(),
                    getSubject(),
                    new Date(),
                    false,
                    MessageEvent.GROUP_MESSAGE);

        for (ChatRoomMessageListener elem : messageListeners)
            elem.messageDelivered(evt);
    }

    /**
     * Returns a reference to the provider that created this room.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this room.
     */
    public ProtocolProviderService getParentProvider()
    {
        return provider;
    }

    /**
     * Methods for manipulating mock operation set as
     * deliver(receive) messageop
     *
     * @param msg the message that we are to deliver.
     * @param from delivered from
     */
    public void deliverMessage(ImMessage msg, String from)
    {
        ChatRoomMember fromMember = null;
        for (ChatRoomMember elem : members)
        {
            if(elem.getName().equals(from))
            {
                fromMember = elem;
                break;
            }
        }

        if(fromMember == null)
            return;

        ChatRoomMessageReceivedEvent evt =
            new ChatRoomMessageReceivedEvent(
                    msg,
                    this,
                    this.getIdentifier().toString(),
                    this.getSubject(),
                    fromMember,
                    new Date(),
                    true,
                    false,
                    MessageEvent.GROUP_MESSAGE);

        for (ChatRoomMessageListener elem : messageListeners)
            elem.messageReceived(evt);
    }

    /**
     * Returns the identifier of this <tt>ChatRoom</tt>. The identifier of the
     * chat room would have the following syntax:
     * [chatRoomName]@[chatRoomServer]@[accountID]
     *
     * @return a <tt>String</tt> containing the identifier of this
     * <tt>ChatRoom</tt>.
     */
    public EntityBareJid getIdentifier()
    {
        return identifier;
    }

    /**
     * Bans a user from the room. Not implemented for mock chat rooms.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to be banned
     * @param reason the reason why the user was banned
     */
    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
    {}

    /**
     * Returns the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room. Not implemented for mock
     * chat rooms.
     *
     * @return the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room
     */
    public ChatRoomConfigurationForm getConfigurationForm()
    {
        return null;
    }

    /**
     * Determines whether this chat room should be stored in the configuration
     * file or not. If the chat room is persistent it still will be shown after a
     * restart in the chat room list. A non-persistent chat room will be only in
     * the chat room list until the program is running.
     *
     * @return true if this chat room is persistent, false otherwise
     */
    public boolean isPersistent()
    {
        return true;
    }

    /**
     * Revokes the ownership role for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke ownership role
     * for
     */
    public void revokeOwnership(Jid address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the local user role.
     * @return the local user role
     */
    public ChatRoomMemberRole getUserRole()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Sets the local user role.
     * @param role the role to set
     */
    public void setLocalUserRole(ChatRoomMemberRole role)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isActive()
    {
        return false;
    }

    @Override
    public void destroy(Date leaveDate, String eventType, String reason)
    {
    }

    @Override
    public boolean isMuted()
    {
        return false;
    }

    @Override
    public void toggleMute()
    {
    }

    @Override
    public void join(Date joinDate)
    {
    }

    @Override
    public void leave(boolean sendMessage, String eventType, String reason)
    {
    }

    @Override
    public Set<MetaContact> getMetaContactMembers()
    {
        return null;
    }

    @Override
    public String getDirectInviteResource()
    {
        return null;
    }

    @Override
    public String getSelectOthersInviteResource()
    {
        return null;
    }

    @Override
    public void createConference(boolean createImmediately)
    {
    }

    @Override
    public boolean canBeUplifedToConference()
    {
        return false;
    }

    @Override
    public Set<String> getNonMetaContactMemberJids()
    {
        return null;
    }
}
