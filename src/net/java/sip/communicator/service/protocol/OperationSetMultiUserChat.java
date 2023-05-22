/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Allows creating, configuring, joining and administering of individual
 * text-based conference rooms.
 *
 * @author Emil Ivov
 */
public interface OperationSetMultiUserChat
    extends OperationSet
{
    /**
     * The prefix used for all chatroom IDs.
     */
    String CHATROOM_ID_PREFIX = "chatroom-";

    /**
     * Returns the <tt>List</tt> of <tt>String</tt>s indicating chat rooms
     * currently available on the server that this protocol provider is
     * connected to.
     *
     * @return a <tt>java.util.List</tt> of the name <tt>String</tt>s for chat
     * rooms that are currently available on the server that this protocol
     * provider is connected to.
     *
     * @throws OperationFailedException if we failed retrieving this list from
     * the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi-user chat
     */
    List<String> getExistingChatRooms()
        throws OperationFailedException, OperationNotSupportedException;

    /**
     * Returns a count of the number of currently active chat rooms.
     *
     * @return the number of currently active chat rooms
     */
    int getActiveChatRooms();

    /**
     * Returns a list of the chat rooms that we have joined and are currently
     * active in.
     *
     * @return a <tt>List</tt> of the rooms where the user has joined using a
     * given connection.
     */
    List<ChatRoom> getCurrentlyJoinedChatRooms();

    /**
     * Returns a list of the chat rooms that <tt>chatRoomMember</tt> has joined
     * and is currently active in.
     *
     * @param chatRoomMember the chatRoomMember whose current ChatRooms we will
     * be querying.
     * @return a list of the chat rooms that <tt>chatRoomMember</tt> has
     * joined and is currently active in.
     *
     * @throws OperationFailedException if an error occurs while trying to
     * discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi-user chat
     */
    List<String> getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember)
        throws OperationFailedException, OperationNotSupportedException;

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to. When the method returns the room the
     * local user will not have joined it and thus will not receive messages on
     * it until the <tt>ChatRoom.join()</tt> method is called.
     * <p>
     *
     * @param roomName
     *            the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties
     *            properties specifying how the room should be created;
     *            <tt>null</tt> for no properties just like an empty
     *            <code>Map</code>
     * @throws OperationFailedException
     *             if the room couldn't be created for some reason (e.g. room
     *             already exists; user already joined to an existent room or
     *             user has no permissions to create a chat room).
     * @throws OperationNotSupportedException
     *             if chat room creation is not supported by this server
     *
     * @return the newly created <tt>ChatRoom</tt> named <tt>roomName</tt>.
     */
    ChatRoom createChatRoom(String roomName,
                            Map<String, Object> roomProperties)
        throws OperationFailedException,
               OperationNotSupportedException;

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt> or null
     * if no room with the given name exist on the server.
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt> if it exists, null
     * otherwise.
     *
     * @throws OperationFailedException if an error occurs while trying to
     * discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi-user chat
     */
    ChatRoom findRoom(String roomName)
        throws OperationFailedException, OperationNotSupportedException;

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the invitation we are rejecting.
     * @param rejectReason the reason to reject the invitation (optional)
     */
    void rejectInvitation(ChatRoomInvitation invitation,
                          String rejectReason);

    /**
     * Adds a listener to invitation notifications. The listener will be fired
     * anytime an invitation is received.
     *
     * @param listener an invitation listener.
     */
    void addInvitationListener(ChatRoomInvitationListener listener);

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation events.
     *
     * @param listener the invitation listener to remove.
     */
    void removeInvitationListener(ChatRoomInvitationListener listener);

    /**
     * Adds a listener to invitation notifications. The listener will be fired
     * anytime an invitation is received.
     *
     * @param listener an invitation listener.
     */
    void addInvitationRejectionListener(
            ChatRoomInvitationRejectionListener listener);

    /**
     * Removes the given listener from the list of invitation listeners
     * registered to receive events every time an invitation has been rejected.
     *
     * @param listener the invitation listener to remove.
     */
    void removeInvitationRejectionListener(
            ChatRoomInvitationRejectionListener listener);

    /**
     * Returns true if <tt>contact</tt> supports multi-user chat sessions.
     *
     * @param contact reference to the contact whose support for chat rooms
     * we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports chat rooms.
     */
    boolean isMultiChatSupportedByContact(Contact contact);

    /**
     * Returns true if multi user chat is supported on the server
     *
     * @return true if multi user chat is supported on the server
     */
    boolean isMultiChatSupported();

    /**
     * Adds a listener that will be notified of changes in our participation in
     * a chat room such as us being kicked, joined, left.
     *
     * @param listener a local user participation listener.
     */
    void addPresenceListener(
            LocalUserChatRoomPresenceListener listener);

    /**
     * Removes a listener that was being notified of changes in our
     * participation in a room such as us being kicked, joined, left.
     *
     * @param listener a local user participation listener.
     */
    void removePresenceListener(
            LocalUserChatRoomPresenceListener listener);

    /**
     * Adds a listener that will be notified when a new chat room is created
     *
     * @param listener a chat room created listener
     */
    void addChatRoomCreatedListener(ChatRoomCreatedListener listener);

    /**
     * Removes a listener that was being notified of new chat rooms being
     * created.
     *
     * @param listener a chat room created listener
     */
    void removeChatRoomCreatedListener(ChatRoomCreatedListener listener);
}
