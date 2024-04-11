// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * Allows user to send hidden messages between clients. Hidden messages are
 * used to pass on information but not displayed to the user.  It allows other
 * classes to register as handlers for these hidden messages.
 */
public interface OperationSetSpecialMessaging extends OperationSet
{
    /**
     * Create a Message instance for sending a hidden text message with
     * default content type and encoding.
     *
     * @param messageType The type of hidden message
     * @param messageText The body of the message
     * @return the created message
     */
    ImMessage createSpecialMessage(String messageType, String messageText);

    /**
     * Register a message handler for an array of types
     *
     * @param handler the handler
     * @param types the types of message that the handler can handle
     */
    void registerSpecialMessageHandler(SpecialMessageHandler handler,
                                       String... types);

    /**
     * Remove the registered handler for an array of types
     *
     * @param types the types to remove the handler for
     */
    void removeSpecialMessageHandler(String... types);

    /**
     * Get the handlers for a particular special message
     *
     * @param type the type of the message
     * @return the handlers for that type
     */
    List<SpecialMessageHandler> getSpecialMessageHandlers(String type);

    /**
     * Interface implemented by objects that wish to be informed of special
     * messages in order that they might handle them.
     */
    interface SpecialMessageHandler
    {
        /**
         * Handle a hidden message
         *
         * @param data the data from the special message
         * @param delay If the message was sent while all our clients were
         * offline, then this is the date that it was sent.  Otherwise, is null.
         */
        void handleSpecialMessage(String data, Date delay);

        /**
         * Handle a hidden message
         *
         * @param data the data from the special message
         * @param delay If the message was sent while all our clients were
         * offline, then this is the date that it was sent.  Otherwise, is null.
         * @param chatRoom the chat room in which we received the message.
         */
        void handleSpecialMessage(String data, Date delay, ChatRoom chatRoom);
    }
}
