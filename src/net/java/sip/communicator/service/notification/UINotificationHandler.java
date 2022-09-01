// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.notification;

/**
 * The UINotificationHandler handles UI notifications fired from the NotificationService.
 * It aggregates them and notifies its UINotificationListeners of how many notifications
 * of each type they should display.
 */

public interface UINotificationHandler
    extends NotificationHandler
{
    void handleUINotification(NotificationData data);

    void clearCallNotifications();

    void clearChatNotifications(NotificationData data);

    void addNotificationListener(UINotificationListener listener);
}
