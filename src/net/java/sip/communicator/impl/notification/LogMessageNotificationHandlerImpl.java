/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import static net.java.sip.communicator.service.notification.LogMessageNotificationAction.ERROR_LOG_TYPE;
import static net.java.sip.communicator.service.notification.LogMessageNotificationAction.INFO_LOG_TYPE;
import static net.java.sip.communicator.service.notification.LogMessageNotificationAction.TRACE_LOG_TYPE;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the <tt>LogMessageNotificationHandler</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class LogMessageNotificationHandlerImpl
    implements LogMessageNotificationHandler
{
    /**
     * The logger that will be used to log messages.
     */
    private Logger logger
        = Logger.getLogger(LogMessageNotificationHandlerImpl.class);

    /**
     * {@inheritDoc}
     */
    public String getActionType()
    {
        return NotificationAction.ACTION_LOG_MESSAGE;
    }

    /**
     * Logs a message through the sip communicator Logger.
     *
     * @param action the action to act upon
     * @param message the message coming from the event
     */
    public void logMessage(LogMessageNotificationAction action, String message)
    {
        switch (action.getLogType())
        {
            case ERROR_LOG_TYPE:
                logger.error(message);
                break;
            case INFO_LOG_TYPE:
                logger.info(message);
                break;
            case TRACE_LOG_TYPE:
                logger.trace(message);
                break;
        }
    }
}
