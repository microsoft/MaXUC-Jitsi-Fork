/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import net.java.sip.communicator.plugin.desktoputil.CreateConferenceMenu.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a chat channel/room/rendez-vous point/ where multiple chat users
 * could rally and communicate in a many-to-many fashion.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 * @author Valentin Martinet
 */
public interface ChatRoom extends CreateConferenceMenuContainer
{
    /**
     * Returns the identifier of this <tt>ChatRoom</tt>. The identifier of the
     * chat room would have the following syntax:
     * [chatRoomName]@[chatRoomServer]
     *
     * @return a @EntityBareJid containing the identifier of this
     * <tt>ChatRoom</tt>.
     */
    EntityBareJid getIdentifier();

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    void join()
        throws OperationFailedException;

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     * @param joinDate the date when we joined the chat room, possibly on a
     * different client.
     *
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    void join(Date joinDate)
        throws OperationFailedException;

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
    void joinAs(String nickname)
        throws OperationFailedException;

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
    void joinAs(String nickname, byte[] password)
        throws OperationFailedException;

    /**
     * Joins this chat room with the specified nickname and password so that the
     * user would start receiving events and messages for it. If the chatroom
     * already contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     * @param password a password necessary to authenticate when joining the
     * room.
     * @param joinDate the date when we joined the chat room, possibly on a
     * different client.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    void joinAs(String nickname, byte[] password, Date joinDate)
        throws OperationFailedException;

    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join()} methods).
     *
     * @return true if currently we're currently in this chat room and false
     * otherwise.
     */
    boolean isJoined();

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     * @param eventType The LocalUserChatRoomPresenceChangeEvent that caused
     * the user to leave.
     * @param reason The reason why the user left, may be null.
     */
    void leave(String eventType, String reason);

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     * @param sendMessage If true, send a message to that chat room just before
     * leaving to inform the other users that we're about to leave the chat.
     * @param eventType The LocalUserChatRoomPresenceChangeEvent that caused
     * the user to leave.
     * @param reason The reason why the user left, may be null.
     */
    void leave(boolean sendMessage, String eventType, String reason);

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     * @param sendMessage If true, send a message to that chat room just before
     * leaving to inform the other users that we're about to leave the chat.
     * @param leaveDate the date when we left the chat room, possibly on a
     * different client.
     * @param eventType The LocalUserChatRoomPresenceChangeEvent that caused
     * the user to leave.
     * @param reason The reason why the user left, may be null.
     */
    void leave(boolean sendMessage,
               Date leaveDate,
               String eventType,
               String reason);

    /**
     * Destroy this chat room both locally and on the server.
     * @param leaveDate the date when we left the chat room, possibly on a
     * different client.
     * @param eventType The LocalUserChatRoomPresenceChangeEvent that caused
     * the user to leave.
     * @param reason The reason why the user left, may be null.
     */
    void destroy(Date leaveDate, String eventType, String reason);

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
    String getSubject();

    /**
     * Sets the subject of this chat room. If the user does not have the right
     * to change the room subject, or the protocol does not support this, or
     * the operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     *
     * @param subject the new subject that we'd like this room to have
     * @throws OperationFailedException if the user doesn't have the right to
     * change this property.
     */
    void setSubject(String subject)
        throws OperationFailedException;

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     * context of the local chat room.
     */
    String getUserNickname();

    /**
     * Returns the local user's role in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the role currently being used by the local user in the context of
     * the chat room.
     */
    ChatRoomMemberRole getUserRole();

    /**
     * Changes the local user's nickname in the context of this chatroom.
     *
     * @param role the new role to set for the local user.
     *
     */
    void setLocalUserRole(ChatRoomMemberRole role)
    ;

    /**
     * Adds a listener that will be notified of changes in our participation in
     * the room such as us being kicked, join, left...
     *
     * @param listener a member participation listener.
     */
    void addMemberPresenceListener(
            ChatRoomMemberPresenceListener listener);

    /**
     * Removes a listener that was being notified of changes in the
     * participation of other chat room participants such as users being kicked,
     * join, left.
     *
     * @param listener a member participation listener.
     */
    void removeMemberPresenceListener(
            ChatRoomMemberPresenceListener listener);

    /**
     * Adds a listener that will be notified of changes in the property of the
     * room such as the subject being change or the room state being changed.
     *
     * @param listener a property change listener.
     */
    void addPropertyChangeListener(
            ChatRoomPropertyChangeListener listener);

    /**
     * Removes a listener that was being notified of changes in the property of
     * the chat room such as the subject being change or the room state being
     * changed.
     *
     * @param listener a property change listener.
     */
    void removePropertyChangeListener(
            ChatRoomPropertyChangeListener listener);

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
    void invite(String userAddress, String reason);

    /**
     * Returns a <tt>List</tt> of <tt>ChatRoomMember</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>ChatRoomMember</tt> instances
     * corresponding to all room members.
     */
    List<ChatRoomMember> getMembers();

    /**
     * Returns the number of participants that are currently in this chat room.
     * @return the number of <tt>Contact</tt>s, currently participating in
     * this room.
     */
    int getMembersCount();

    /**
     * Returns a <tt>List</tt> of <tt>MetaContact</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>MetaContact</tt> instances
     * corresponding to all room members.
     */
    Set<MetaContact> getMetaContactMembers();

    /**
     * Returns a <tt>Set</tt> of <tt>String</tt>s corresponding to the jids of
     * all members currently participating in this room who are not
     * MetaContacts.
     *
     * @return a <tt>Set</tt> of <tt>String</tt>s corresponding to the jids.
     */
    Set<String> getNonMetaContactMemberJids();

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     * @param listener a <tt>MessageListener</tt> that would be notified every
     * time a new message is received on this chat room.
     */
    void addMessageListener(ChatRoomMessageListener listener);

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    void removeMessageListener(ChatRoomMessageListener listener);

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now subject.
     * @return the newly created message.
     */
    ImMessage createMessage(byte[] content, String contentType,
                          String contentEncoding, String subject);

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @param messageUID the UID of the message.
     * @return Message the newly created message
     */
    ImMessage createMessage(String messageText, String messageUID);

    /**
     * Sends the <tt>message</tt> to this chat room.
     *
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    void sendMessage(ImMessage message)
        throws OperationFailedException;

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param message the <tt>Message</tt> to send.
     * @param fireEvent whether we should fire an event once the message
     * has been sent.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    void sendMessage(ImMessage message, boolean fireEvent)
        throws OperationFailedException;

    /**
     * Returns a reference to the provider that created this room.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this room.
     */
    ProtocolProviderService getParentProvider();

    /**
     * Bans a user from the room. An administrator or owner of the room can ban
     * users from a room. A banned user will no longer be able to join the room
     * unless the ban has been removed. If the banned user was present in the
     * room then he/she will be removed from the room and notified that he/she
     * was banned along with the reason (if provided) and the user who initiated
     * the ban.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to be banned.
     * @param reason the reason why the user was banned.
     * @throws OperationFailedException if an error occurs while banning a user.
     * In particular, an error can occur if a moderator or a user with an
     * affiliation of "owner" or "admin" was tried to be banned or if the user
     * that is banning have not enough permissions to ban.
     */
    void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException;

    /**
     * Kicks a visitor or participant from the room.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to kick from the room
     * @param reason the reason why the participant is being kicked from the
     * room
     * @throws OperationFailedException if an error occurs while kicking the
     * participant. In particular, an error can occur if a moderator or a user
     * with an affiliation of "owner" or "administrator" was intended to be
     * kicked; or if the participant that intended to kick another participant
     * does not have kicking privileges;
     */
    void kickParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException;

    /**
     * Returns the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room. If the user doesn't have
     * permissions to see and change chat room configuration an
     * <tt>OperationFailedException</tt> is thrown.
     *
     * @return the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room
     * @throws OperationFailedException if the user doesn't have
     * permissions to see and change chat room configuration
     */
    ChatRoomConfigurationForm getConfigurationForm()
        throws OperationFailedException;

    /**
     * Determines whether this chat room should be stored in the configuration
     * file or not. If the chat room is persistent it still will be shown after a
     * restart in the chat room list. A non-persistent chat room will be only in
     * the chat room list until the program is running.
     *
     * @return true if this chat room is persistent, false otherwise
     */
    boolean isPersistent();

    /**
    * Grants administrator privileges to another user. Room owners may grant
    * administrator privileges to a member or unaffiliated user. An
    * administrator is allowed to perform administrative functions such as
    * banning users and edit moderator list.
    *
    * @param address the user address of the user to grant administrator
    * privileges (e.g. "user@host.org").
    */
    void grantAdmin(Jid address);

    /**
    * Grants membership to a user. Only administrators are able to grant
    * membership. A user that becomes a room member will be able to enter a room
    * of type Members-Only (i.e. a room that a user cannot enter without being
    * on the member list).
    *
    * @param address the user address of the user to grant membership
    * privileges (e.g. "user@host.org").
    */
    void grantMembership(Jid address);

    /**
    * Grants moderator privileges to a participant or visitor. Room
    * administrators may grant moderator privileges. A moderator is allowed to
    * kick users, grant and revoke voice, invite other users, modify room's
    * subject plus all the partcipants privileges.
    *
    * @param nickname the nickname of the occupant to grant moderator
    * privileges.
    */
    void grantModerator(Resourcepart nickname);

    /**
    * Grants ownership privileges to another user. Room owners may grant
    * ownership privileges. Some room implementations will not allow to grant
    * ownership privileges to other users. An owner is allowed to change
    * defining room features as well as perform all administrative functions.
    *
    * @param address the user address of the user to grant ownership
    * privileges (e.g. "user@host.org").
    */
    void grantOwnership(Jid address);

    /**
    * Grants voice to a visitor in the room. In a moderated room, a moderator
    * may want to manage who does and does not have "voice" in the room. To have
    * voice means that a room occupant is able to send messages to the room
    * occupants.
    *
    * @param nickname the nickname of the visitor to grant voice in the room
    * (e.g. "john").
    */
    void grantVoice(Resourcepart nickname);

    /**
    * Revokes administrator privileges from a user. The occupant that loses
    * administrator privileges will become a member. Room owners may revoke
    * administrator privileges from a member or unaffiliated user.
    *
    * @param address the user address of the user to grant administrator
    * privileges (e.g. "user@host.org").
    */
    void revokeAdmin(Jid address);

    /**
    * Revokes a user's membership. Only administrators are able to revoke
    * membership. A user that becomes a room member will be able to enter a room
    * of type Members-Only (i.e. a room that a user cannot enter without being
    * on the member list). If the user is in the room and the room is of type
    * members-only then the user will be removed from the room.
    *
    * @param address the user address of the user to revoke membership
    * (e.g. "user@host.org").
    */
    void revokeMembership(Jid address);

    /**
    * Revokes moderator privileges from another user. The occupant that loses
    * moderator privileges will become a participant. Room administrators may
    * revoke moderator privileges only to occupants whose affiliation is member
    * or none. This means that an administrator is not allowed to revoke
    * moderator privileges from other room administrators or owners.
    *
    * @param nickname the nickname of the occupant to revoke moderator
    * privileges.
    */
    void revokeModerator(Resourcepart nickname);

    /**
    * Revokes ownership privileges from another user. The occupant that loses
    * ownership privileges will become an administrator. Room owners may revoke
    * ownership privileges. Some room implementations will not allow to grant
    * ownership privileges to other users.
    *
    * @param address the user address of the user to revoke ownership
    * (e.g. "user@host.org").
    *
    * @throws OperationFailedException
    */
    void revokeOwnership(Jid address) throws OperationFailedException;

    /**
    * Revokes voice from a participant in the room. In a moderated room, a
    * moderator may want to revoke an occupant's privileges to speak. To have
    * voice means that a room occupant is able to send messages to the room
    * occupants.
    * @param nickname the nickname of the participant to revoke voice
    * (e.g. "john").
    */
    void revokeVoice(Resourcepart nickname);

    /**
     * Returns true if the chat room is active. An active chat room is one that
     * has had a message sent or received during this instance of Accession
     * running.
     *
     * @return true if the chat room is active
     */
    boolean isActive();

    /**
     * Returns true if we've already displayed a toast to notify the user that
     * they have received historical messages in this chat room, so that we
     * don't display a new toast for every historical message.
     *
     * @return true if we've already displayed a toast.
     */
    boolean historyNotificationDisplayed();

    /**
     * Sets whether we've already displayed a toast to notify the user that
     * they have received historical messages in this chat room.  This is used
     * to ensure that we don't display a new toast for every historical
     * message.
     *
     * @param displayed Should be set to true if we've already displayed a
     * toast.
     *
     */
    void setHistoryNotificationDisplayed(boolean displayed);

    /**
     * Whether the Chat Room is currently muted and hence if it shouldn't
     * display toasts or play sound notifications
     *
     * @return whether to chat room is muted
     */
    boolean isMuted();

    /**
     * Invert whether the Chat Room is currently muted
     */
    void toggleMute();
}
