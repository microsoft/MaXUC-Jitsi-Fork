// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

/**
 * A listener that dispatches events notifying that a chat room has been created
 */
public interface ChatRoomCreatedListener
{
    /**
     * Notifies that a new chat room has been created
     *
     * @param evt the event to be dispatched
     */
    void chatRoomCreated(ChatRoomCreatedEvent evt);
}
