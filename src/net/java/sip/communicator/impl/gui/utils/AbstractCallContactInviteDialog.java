// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;

/**
 * A class which allows the user to choose a number either from a contact or by
 * entering it themselves to invite them to a call. Abstract so that callers must decide
 * what should be done when a number or contact is chosen.
 */
public abstract class AbstractCallContactInviteDialog extends AbstractContactInviteDialog
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an <tt>AbstractCallContactInviteDialog</tt>.
     * @param parent the parent of this dialog, on which this dialog is
     *               centered and which, when closed, causes this dialog to
     *               be closed too
     * @param title the title to show on the top of this dialog
     * @param okButtonText the text to display on the ok button
     */
    public AbstractCallContactInviteDialog(Window parent,
                                           String title,
                                           String okButtonText)
    {
        super(parent, title, okButtonText, true);

        setAlwaysOnTop(true);
    }
}
