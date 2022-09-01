// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.resources.*;

import java.awt.*;

import java.util.function.*;

/**
 * A dialog which allows the user to choose a contact or enter a number to be
 * passed to a callback which consumes a UIContact.
 */
public class ContactWithNumberSelectDialog extends AbstractContactInviteDialog
{
    private static final long serialVersionUID = 1L;

    private static final Logger sLog = Logger.getLogger(ContactWithNumberSelectDialog.class);

    private static final ResourceManagementService sResources = GuiActivator.getResources();

    /**
     * Callback to be called when the user has selected a contact or entered a
     * number and dismissed the dialog by pressing OK.
     */
    private final Consumer<UIContact> mCallback;

    /**
     * @param parent the parent of this dialog, on which this dialog is
     *               centered and which, when closed, causes this dialog to
     *               be closed too
     * @param title the title of this dialog
     * @param callback called when a contact is selected
     */
    public ContactWithNumberSelectDialog(Window parent,
                                         String title,
                                         Consumer<UIContact> callback)
    {
        super(parent,
              title,
              sResources.getI18NString("service.gui.OK"),
              true);

        mCallback = callback;
    }

    @Override
    protected void okPressed(UIContact uiContact,
                             String enteredText,
                             String phoneNumber)
    {
        if (uiContact != null)
        {
            sLog.user("Contact selected: " + uiContact);
            mCallback.accept(uiContact);
            setVisible(false);
            dispose();
        }
        else
        {
            sLog.info("No contact selected");
            showError("service.gui.add.ERROR",
                      "service.gui.NO_CONTACT_SELECTED");
        }
    }
}
