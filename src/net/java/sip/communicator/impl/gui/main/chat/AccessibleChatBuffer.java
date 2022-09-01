// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.util.*;

/**
 * A chat buffer that is navigable via accessibility software. It keeps a buffer
 * of all chat messages and allows the user to move through the buffer to
 * re-read previous messages
 */
public class AccessibleChatBuffer
{
    /**
     * The list of chat messages sent and received
     */
    private ArrayList<String> chatBuffer = new ArrayList<>();

    /**
     * The pointer showing the current position the user is up to in the chat
     * buffer
     */
    private int bufferPointer = 0;

    /**
     * The component that is responsible for displaying the accessible
     * information.
     */
    private Component parentComponent;

    /**
     * Create a new chat buffer
     *
     * @param parentComponent the component responsible for displaying the
     * accessible information
     */
    public AccessibleChatBuffer(Component parentComponent)
    {
        this.parentComponent = parentComponent;
    }

    /**
     * Adds a message to the buffer
     *
     * @param author the display name of who sent this message
     * @param message the message sent or received
     * @param date the formatted date/time of the message
     */
    public void addMessage(String author, String message, String date)
    {
        // If the buffer pointer is currently on the last element of the buffer
        // then advance it by one to keep it at the end
        if (bufferPointer == (chatBuffer.size() - 1))
        {
            bufferPointer += 1;
        }

        String combinedMessage = GuiActivator.getResources().getI18NString(
                "service.gui.accessibility.CHAT_MESSAGE",
                new String[] {author, message, date});
        chatBuffer.add(combinedMessage);
        AccessibilityUtils.setName(parentComponent, combinedMessage);
    }

    /**
     * Move the buffer pointer backwards and set the accessibility information
     * on the parent component
     */
    public void getPreviousMessage()
    {
        // Decrease the pointer if possible
        if (bufferPointer > 0)
        {
            bufferPointer -= 1;
        }

        // Set the accessible name for the current chat message
        AccessibilityUtils.setName(parentComponent, chatBuffer.get(bufferPointer));
    }

    /**
     * Move the buffer pointer forwards and set the accessibility information
     * on the parent component
     */
    public void getNextMessage()
    {
        // Advance the pointer if possible
        if (bufferPointer < (chatBuffer.size() - 1))
        {
            bufferPointer += 1;
        }

        // Set the accessible name for the current chat message
        AccessibilityUtils.setName(parentComponent, chatBuffer.get(bufferPointer));
    }
}
