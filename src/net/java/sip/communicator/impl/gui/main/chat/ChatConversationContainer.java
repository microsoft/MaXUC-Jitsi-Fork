/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;

/**
 * The <tt>ChatConversationContainer</tt> is used as an abstraction of the
 * conversation area, which is included in both the chat window and the history
 * window.
 *
 * @author Yana Stamcheva
 */
public interface ChatConversationContainer
{
    /**
     * An enum of chat types that this container can display
     */
    enum ChatType
    {
        ONE_TO_ONE_CHAT,
        GROUP_CHAT
    }

    /**
     * Returns the window, where this chat conversation container is contained.
     * (the chat window, the history window, etc)
     *
     * @return the window, where this chat conversation container is contained.
     */
    Window getConversationContainerWindow();

    /**
     * Sets the given status message to this conversation container.
     *
     * @param statusMessage the status message to set
     */
    void addTypingNotification(String statusMessage);

    /**
     * Gets the accessible chat buffer
     *
     * @return the accessible chat buffer
     */
    AccessibleChatBuffer getAccessibleChatBuffer();

    /**
     * Gets the chat type that this container is showing
     *
     * @return the chat type this container is showing
     */
    ChatType getChatType();
}
