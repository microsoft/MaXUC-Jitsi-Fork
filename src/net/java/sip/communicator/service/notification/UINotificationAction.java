// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.notification;

/**
 * The notification action associated with displaying the number of outstanding
 * notifications to the user. The types of notification include missed calls,
 * unread messages, and voicemails.
 */
public class UINotificationAction
    extends NotificationAction
{
    public UINotificationAction()
    {
        super(NotificationAction.ACTION_DISPLAY_UI_NOTIFICATIONS);
    }
}
