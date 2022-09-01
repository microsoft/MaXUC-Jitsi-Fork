// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.notification;

import static org.jitsi.util.Hasher.logHasher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.java.sip.communicator.plugin.notificationwiring.NotificationManager;
import net.java.sip.communicator.service.notification.NotificationAction;
import net.java.sip.communicator.service.notification.NotificationData;
import net.java.sip.communicator.service.notification.UINotificationHandler;
import net.java.sip.communicator.service.notification.UINotificationListener;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.util.Logger;

public class UINotificationHandlerImpl
    implements UINotificationHandler
{
    /**
     * We only want a single notification per chat, so store an identifying string
     * for each chat that has unread messages. This string will be one of:
     * - the group chat ID (for a group chat)
     * - the chat contact's IM address
     * - the chat contact's SMS number
     */
    private Set<String> unreadChatNotifications = new HashSet<>();
    private int missedCallNotifications = 0;
    private int messageWaitingNotifications = 0;

    private final Set<UINotificationListener> listeners = new HashSet<>();

    /**
     * The <tt>Logger</tt> used by the <tt>UINotificationHandlerImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
            = Logger.getLogger(UINotificationHandlerImpl.class);

    @Override
    public void addNotificationListener(UINotificationListener listener)
    {
        synchronized (listeners)
        {
            listeners.add(listener);
            listener.updateUI(missedCallNotifications,
                              unreadChatNotifications.size(),
                              messageWaitingNotifications);
        }
    }

    /**
     * Calculates the number of unacknowledged notifications (based on
     * the receipt of a new notification) of each type
     * and notifies any listeners.
     * @param data associated with the notification
     */
    @Override
    public synchronized void handleUINotification(NotificationData data)
    {
        if (data == null) {
            logger.logStackTrace("Called handleUINotification with null NotificationData parameter",
                                 Logger.Level.SEVERE);
            return;
        }
        switch(data.getEventType())
        {
            case NotificationManager.MISSED_CALL:
                missedCallNotifications += 1;
                break;
            case NotificationManager.INCOMING_MESSAGE:
                // Find string identifying the chat that generated this message
                String chatDescriptorString = getChatDescriptorString(
                        data.getExtra(NotificationData.MESSAGE_NOTIFICATION_TAG_EXTRA));
                if (chatDescriptorString != null)
                {
                    unreadChatNotifications.add(chatDescriptorString);
                }
                break;
            case NotificationManager.MESSAGE_WAITING:
                messageWaitingNotifications = (int)
                        data.getExtra(NotificationData.MESSAGE_WAITING_COUNT_EXTRA);
                break;
            default:
                logger.logStackTrace("Received notification with unknown event type",
                                     Logger.Level.SEVERE);
                break;
        }

        logger.debug("Notifying UINotificationListeners of unacknowledged notifications: " +
                        "missed calls: " + missedCallNotifications,
                        ", unread chats: " + unreadChatNotifications.size(),
                        ", messages waiting: " + messageWaitingNotifications);
        notifyListeners();
    }

    @Override
    public void clearCallNotifications()
    {
        if (missedCallNotifications > 0)
        {
            missedCallNotifications = 0;
            logger.info("Clearing missed call notifications");
            notifyListeners();
        }
    }

    @Override
    public void clearChatNotifications(NotificationData data)
    {
        String chatSource = getChatDescriptorString(data.getExtra(NotificationData.MESSAGE_NOTIFICATION_TAG_EXTRA));
        if (chatSource != null && unreadChatNotifications.remove(chatSource))
        {
            logger.info("Clearing chat notification for chat source: " + logHasher(chatSource));
            notifyListeners();
        }
    }

    /**
     * Returns a string describing the source of the chat which generated a notification,
     * @param chatDescriptor Object containing this information stored in the extras
     *                       field of the NotificationData
     */
    private String getChatDescriptorString(Object chatDescriptor)
    {
        String chatDescriptionString = null;
        if (chatDescriptor instanceof Contact)
        {
            chatDescriptionString = ((Contact)chatDescriptor).getAddress();
        }
        else if (chatDescriptor instanceof ChatRoom)
        {
            chatDescriptionString = ((ChatRoom)chatDescriptor).getIdentifier().toString();
        }
        else if (chatDescriptor instanceof String)
        {
            chatDescriptionString = (String)chatDescriptor;
        }
        else if (chatDescriptor instanceof List)
        {
            // There might be multiple SMS addresses associated with this chat
            // notification, so check if any of them are already used as the index
            // for that chat in our list. Otherwise, return the last one.
            List smsAddressList = (List) chatDescriptor;
            for (Object entry : smsAddressList)
            {
                chatDescriptionString = (String) entry;

                if (unreadChatNotifications.contains(entry))
                {
                        break;
                }
            }
        }
        else
        {
            logger.warn("Received a chat notification without a clear source");
        }

        return chatDescriptionString;
    }

    @Override
    public String getActionType()
    {
        return NotificationAction.ACTION_DISPLAY_UI_NOTIFICATIONS;
    }

    private void notifyListeners()
    {
        synchronized (listeners)
        {
            for (UINotificationListener listener : listeners)
            {
                listener.updateUI(missedCallNotifications,
                                  unreadChatNotifications.size(),
                                  messageWaitingNotifications);
            }
        }
    }
}