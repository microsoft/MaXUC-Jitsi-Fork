/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.notification;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * An implementation of the <tt>PopupMessageNotificationHandler</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class PopupMessageNotificationHandlerImpl
    implements PopupMessageNotificationHandler
{
    private static final String ENABLE_CHAT_TOASTS =
        "net.java.sip.communicator.plugin.generalconfig.messageconfig.ENABLE_CHAT_TOASTS";

    /**
     * The logger that will be used to log messages.
     */
    private Logger logger
        = Logger.getLogger(PopupMessageNotificationHandlerImpl.class);

    /**
     * {@inheritDoc}
     */
    public String getActionType()
    {
        return NotificationAction.ACTION_POPUP_MESSAGE;
    }

    /**
     * Shows the given <tt>PopupMessage</tt>
     *
     * @param action the action to act upon
     * @param title the title of the given message
     * @param message the message to use if and where appropriate (e.g. with
     * systray or log notification.)
     * @param icon the icon to show in the notification if and where
     * appropriate
     * @param tag additional info to be used by the notification handler
     * @param isError whether the pop-up is due to an error
     */
    public void popupMessage(PopupMessageNotificationAction action,
        String title,
        String message,
        BufferedImageFuture icon,
        Object tag,
        boolean isError)
    {
        // No popup if we are DND or we have elected to stop popups
        if(NotificationActivator.getGlobalStatusService().isDoNotDisturb() ||
           !NotificationActivator.getConfigurationService().user().getBoolean(
            ENABLE_CHAT_TOASTS,
            true))
        {
            logger.debug("Not showing toast as requested by user");
            return;
        }

        SystrayService systray = NotificationActivator.getSystray();
        if(systray == null)
        {
            logger.warn("Cannot show toast as no systray service");
            return;
        }

        if(!StringUtils.isNullOrEmpty(message))
        {
            logger.debug("About to show pop-up message");
            systray.showPopupMessage(
                    new PopupMessage(title, message, icon, tag, isError));
        }
        else
        {
            logger.error("Message is null or empty!");
        }
    }

    /**
     * Adds a listener for <tt>SystrayPopupMessageEvent</tt>s posted when user
     * clicks on the system tray popup message.
     *
     * @param listener the listener to add
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener)
    {
        SystrayService systray = NotificationActivator.getSystray();
        if(systray == null)
            return;

        systray.addPopupMessageListener(listener);
    }

    /**
     * Removes a listener previously added with
     * <tt>addPopupMessageListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        SystrayService systray = NotificationActivator.getSystray();
        if(systray == null)
            return;

        systray.removePopupMessageListener(listener);
    }
}
