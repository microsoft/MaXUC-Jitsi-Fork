/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.systray;

import net.java.sip.communicator.service.systray.event.*;

/**
 * The <tt>SystrayService</tt> manages the system tray icon, menu and messages.
 * It is meant to be used by all bundles that want to show a system tray message.
 *
 * @author Yana Stamcheva
 */
public interface SystrayService
{
    /**
     * Shows the given <tt>PopupMessage</tt>
     *
     * @param popupMessage the message to show
     */
    void showPopupMessage(PopupMessage popupMessage);

    /**
     * Adds a listener for <tt>SystrayPopupMessageEvent</tt>s posted when user
     * clicks on the system tray popup message.
     *
     * @param listener the listener to add
     */
    void addPopupMessageListener(SystrayPopupMessageListener listener);

    /**
     * Removes a listener previously added with <tt>addPopupMessageListener</tt>.
     *
     * @param listener the listener to remove
     */
    void removePopupMessageListener(SystrayPopupMessageListener listener);

    /**
     * Set the handler which will be used for popup message
     * @param popupHandler the handler to use
     * @return the previously used popup handler
     */
    PopupMessageHandler setActivePopupMessageHandler(
            PopupMessageHandler popupHandler);

    /**
     * Selects the best available popup message handler
     */
    void selectBestPopupMessageHandler();

    /**
     * Uninitialize this service
     */
    void uninitializeService();

    /**
     * Updates the taskbar overlay icon based on the number of relevant
     * outstanding notifications
     * @param notificationCount the number of outstanding notifications
     */
    void updateTaskbarOverlay(int notificationCount);
}
