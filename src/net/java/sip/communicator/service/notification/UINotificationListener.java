// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.notification;

public interface UINotificationListener
{
    void updateUI(int callNotifications,
                  int chatNotifications,
                  int messageWaitingNotifications);
}
