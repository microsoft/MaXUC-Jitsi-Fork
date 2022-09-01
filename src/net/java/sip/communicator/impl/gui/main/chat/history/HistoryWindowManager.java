/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.history;
import net.java.sip.communicator.util.Logger;

import java.util.*;

import javax.swing.JFrame;

/**
 * Manages all history windows within the gui.
 *
 * @author Yana Stamcheva
 */
public class HistoryWindowManager
{
    /**
     * Map from a chat session descriptor to the history window that is being
     * displayed for that conversation.
     *
     * Note that this map is based on a weak hash map so that the windows aren't
     * leaked.
     *
     * Note further that the map is synchronized.
     */
    private final Map<Object, HistoryWindow> mMessageHistory
        = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Checks if there's an open history window for the given chat session
     * descriptor.
     *
     * @param chatSessionDescriptor the chat session descriptor to check for
     * @return TRUE if there's an opened history window for the given chat
     *         session, FALSE otherwise.
     */
    public boolean containsHistoryWindowForChatDescriptor(Object chatSessionDescriptor)
    {
        return mMessageHistory.containsKey(chatSessionDescriptor);
    }

    /**
     * Returns the history window for the given chat session descriptor.
     *
     * @param chatSessionDescriptor the chat session descriptor to search for
     * @return the history window for the given chat session
     */
    public HistoryWindow getHistoryWindowForChatDescriptor(Object chatSessionDescriptor)
    {
        return mMessageHistory.get(chatSessionDescriptor);
    }

    /**
     * Adds a history window for a given chat session descriptor in the table of
     * opened history windows.
     *
     * @param chatSessionDescriptor the chat session descriptor to add
     * @param historyWindow the history window to add
     */
    public void addHistoryWindowForChatDescriptor(Object chatSessionDescriptor,
        HistoryWindow historyWindow)
    {
        mMessageHistory.put(chatSessionDescriptor, historyWindow);
    }

    /**
     * Removes the history window for the given chat session descriptor.
     *
     * @param chatSessionDescriptor the chat session descriptor for which to
     *        remove the history window
     */
    public void removeHistoryWindowForChatDescriptor(Object chatSessionDescriptor)
    {
        mMessageHistory.remove(chatSessionDescriptor);
    }

    public void displayHistoryWindowForChatDescriptor(Object chatSessionDescriptor, Logger log)
    {
        HistoryWindow historyWindow;

        log.info("Displaying history window for contact");
        if (containsHistoryWindowForChatDescriptor(chatSessionDescriptor))
        {
            log.debug("Bringing existing window to front");
            historyWindow = getHistoryWindowForChatDescriptor(chatSessionDescriptor);

            if (historyWindow.getState() == JFrame.ICONIFIED)
            {
                historyWindow.setState(JFrame.NORMAL);
            }
        }
        else
        {
            log.debug("Creating history window");
            historyWindow = new HistoryWindow(chatSessionDescriptor);

            addHistoryWindowForChatDescriptor(chatSessionDescriptor, historyWindow);
        }

        historyWindow.setVisible(true);
        historyWindow.setAlwaysOnTop(true);
        historyWindow.toFront();
        historyWindow.setAlwaysOnTop(false);
    }
}
