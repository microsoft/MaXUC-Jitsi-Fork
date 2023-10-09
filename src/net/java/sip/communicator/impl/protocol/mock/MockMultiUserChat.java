/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Multiuser chat functionalities for the mock protocol.
 * @author Damian Minkov
 */
public class MockMultiUserChat
    extends AbstractOperationSetMultiUserChat
{
    /**
     * The protocol provider that created us.
     */
    private MockProvider provider = null;

    private final List<ChatRoom> existingChatRooms = new Vector<>();
    private final List<ChatRoom> joinedChatRooms = new Vector<>();

    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     */
    public MockMultiUserChat(MockProvider provider)
    {
        this.provider = provider;
    }

    /**
     * Returns a list of the chat rooms that we have joined and are currently
     * active in.
     *
     * @return a <tt>List</tt> of the rooms where the user has joined using a
     * given connection.
     */
    public List<ChatRoom> getCurrentlyJoinedChatRooms()
    {
        return joinedChatRooms;
    }

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to. When the method returns the room the
     * local user will not have joined it and thus will not receive messages on
     * it until the <tt>ChatRoom.join()</tt> method is called.
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     * created.
     * @return the newly created <tt>ChatRoom</tt> named <tt>roomName</tt>.
     */
    public ChatRoom createChatRoom(
            String roomName,
            Map<String, Object> roomProperties)
    {
        MockChatRoom room = new MockChatRoom(provider, this, roomName);
        existingChatRooms.add(room);
        return room;
    }

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt> or null if no
     * such room exists.
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt> or null if no such
     * room exists on the server that this provider is currently connected to.
     *
     */
    public ChatRoom findRoom(String roomName)
    {
        for (ChatRoom elem : existingChatRooms)
            if(elem.getIdentifier().equals(roomName))
                return elem;
        return null;
    }

}
