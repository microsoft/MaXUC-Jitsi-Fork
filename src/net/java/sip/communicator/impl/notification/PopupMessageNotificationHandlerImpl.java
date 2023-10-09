/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.notification;

import net.java.sip.communicator.plugin.desktoputil.NotificationInfo;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPANotion;
import net.java.sip.communicator.service.wispaservice.WISPANotionType;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.Logger;

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
     * Pop-up messages are now shown as OS notifications and support for icons,
     * tags, and errors are not yet supported (so will be ignored).
     *
     * @param action the action to act upon
     * @param title the title of the given message
     * @param message the message to use if and where appropriate (e.g. with
     * systray or log notification.)
     */
    public void popupMessage(PopupMessageNotificationAction action,
        String title,
        String message)
    {
        if(StringUtils.isNullOrEmpty(message))
        {
            logger.error("Message is null or empty!");
            return;
        }

        // No popup if we are DND or we have elected to stop popups
        if(NotificationActivator.getGlobalStatusService().isDoNotDisturb() ||
           !NotificationActivator.getConfigurationService().user().getBoolean(
            ENABLE_CHAT_TOASTS,
            true))
        {
            logger.debug("Not showing toast as requested by user");
            return;
        }

        WISPAService wispaService = NotificationActivator.getWispaService();
        if (wispaService == null)
        {
            logger.warn("Cannot show notification as no WISPA service");
            return;
        }

        logger.debug("About to show pop-up message");
        Object notification = new WISPANotion(WISPANotionType.NOTIFICATION,
            new NotificationInfo(title, message));
        wispaService.notify(WISPANamespace.EVENTS, WISPAAction.NOTION,
            notification);
    }
}
