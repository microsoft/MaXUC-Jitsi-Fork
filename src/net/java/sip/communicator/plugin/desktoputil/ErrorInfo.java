// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import net.java.sip.communicator.plugin.desktoputil.ErrorDialog.OnDismiss;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog.Icon;

public class ErrorInfo
{
    private String title;

    private String message;

    private String configOption;

    private String buttonText;

    private OnDismiss dismissAction;

    private Icon icon;

    public ErrorInfo(String title, String message, String configOption,
        String buttonText, OnDismiss dismissAction, Icon icon)
    {
        this.title = title;
        this.message = message;
        this.configOption = configOption;
        this.buttonText = buttonText;
        this.dismissAction = dismissAction;
        this.icon = icon;
    }

    public String getTitle()
    {
        return title;
    }

    public String getMessage()
    {
        return message;
    }

    public String getConfigOption()
    {
        return configOption;
    }

    public String getButtonText()
    {
        return buttonText;
    }

    public OnDismiss getDismissAction()
    {
        return dismissAction;
    }

    public Icon getIcon()
    {
        return icon;
    }

    public Runnable getAction()
    {
        return null;
    }
}
