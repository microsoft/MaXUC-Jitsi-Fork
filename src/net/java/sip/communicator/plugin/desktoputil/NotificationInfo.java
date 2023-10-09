// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

public class NotificationInfo
{
  private String title;

  private String message;

  private Runnable action = null;

  public NotificationInfo(String title, String message)
  {
    this.title = title;
    this.message = message;
  }

  public NotificationInfo(String title, String message, Runnable action)
  {
    this(title, message);
    this.action = action;
  }

  public String getTitle()
  {
    return title;
  }

  public String getMessage()
  {
    return message;
  }

  public Runnable getAction()
  {
    return action;
  }
}
