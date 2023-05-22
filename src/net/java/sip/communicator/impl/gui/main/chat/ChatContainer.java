/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;

/**
 *
 * @author Yana Stamcheva
 */
public interface ChatContainer
{
    /**
     * Sets the title of this chat container.
     *
     * @param title the title to set
     */
    void setTitle(String title);

    /**
     * Adds a given <tt>ChatPanel</tt> to this chat window.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    void addChat(ChatPanel chatPanel);

    /**
     * Removes a given <tt>ChatPanel</tt> from this chat window.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to remove.
     */
    void removeChat(ChatPanel chatPanel);

    /**
     * Opens the specified <tt>ChatPanel</tt> and optionally brings it to the
     * front.
     *
     * @param chatPanel the <tt>ChatPanel</tt> to be opened
     * @param setSelected <tt>true</tt> if <tt>chatPanel</tt> (and respectively
     * this <tt>ChatContainer</tt>) should be brought to the front; otherwise,
     * <tt>false</tt>
     * @param alertWindow if true, the window will be alerted when it is opened.
     */
    void openChat(ChatPanel chatPanel,
                  boolean setSelected,
                  boolean alertWindow);

    /**
     * Returns the <tt>ChatPanel</tt> for this <tt>ChatContainer</tt>.
     *
     * @return a <tt>ChatPanel</tt>.
     */
    ChatPanel getCurrentChat();

    /**
     * Selects the chat tab which corresponds to the given <tt>MetaContact</tt>.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to select.
     */
    void setCurrentChat(ChatPanel chatPanel);

    /**
     * Returns the frame to which this container belongs.
     *
     * @return the frame to which this container belongs
     */
    Frame getFrame();

    /**
     * Adds the given <tt>ChatChangeListener</tt>.
     *
     * @param listener the listener to add
     */
    void addChatChangeListener(ChatChangeListener listener);

    /**
     * Removes the given <tt>ChatChangeListener</tt>.
     *
     * @param listener the listener to remove
     */
    void removeChatChangeListener(ChatChangeListener listener);

    /**
     * Updates the container to ensure the menu items are correct
     *
     * @param chatPanel the chat for which we want to update the window
     */
    void updateContainer(ChatPanel chatPanel);
}
