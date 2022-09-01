// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An event that is fired when a new chat room is created
 */
public class ChatRoomCreatedEvent
    extends EventObject
{
    private static final long serialVersionUID = 0L;

    /**
     * The chat room that this event refers to
     */
    private ChatRoom chatRoom;

    /**
     * Creates a new chat room created event
     *
     * @param chatRoom the chat room that was created
     */
    public ChatRoomCreatedEvent(ChatRoom chatRoom)
    {
        super(chatRoom);

        this.chatRoom = chatRoom;
    }

    /**
     * Returns the chat room that was created
     *
     * @return the chat room that was created
     */
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }
}
