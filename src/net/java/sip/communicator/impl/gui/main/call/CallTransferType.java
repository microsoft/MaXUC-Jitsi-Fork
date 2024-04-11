// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.GuiActivator;

/**
 * Enum for call transfer types.
 */
public enum CallTransferType
{
    ATTENDED(GuiActivator.getResources().getI18NString("service.gui.ATTENDED_TRANSFER_CALL_TITLE"),
             GuiActivator.getResources().getI18NString("service.gui.ATTENDED_TRANSFER_CALL_BUTTON")),
    UNATTENDED(GuiActivator.getResources().getI18NString("service.gui.UNATTENDED_TRANSFER_CALL_TITLE"),
               GuiActivator.getResources().getI18NString("service.gui.UNATTENDED_TRANSFER_CALL_BUTTON"));

    /**
     * Call transfer dialog title
     */
    private final String title;

    /**
     * Call transfer dialog "OK button" text
     */
    private final String okButtonText;

    CallTransferType(String title, String okButtonText)
    {
        this.title = title;
        this.okButtonText = okButtonText;
    }

    public String getTitle()
    {
        return title;
    }

    public String getOkButtonText()
    {
        return okButtonText;
    }
}
