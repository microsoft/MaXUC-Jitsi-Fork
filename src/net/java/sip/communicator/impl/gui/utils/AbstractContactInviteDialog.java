// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A class which allows the user to choose a number either from a contact or by
 * entering it themselves.  Abstract so that callers must decide what should be
 * done when a number or contact is chosen.
 */
public abstract class AbstractContactInviteDialog extends ContactInviteDialog
{
    private static final long serialVersionUID = 1L;

    /**
     * The error dialog that we may or may not be showing
     */
    private JDialog mErrorDialog = null;

    /**
     * If true, the user can select offline contacts.
     */
    protected final boolean mCanSelectOffline;

    /**
     * Constructs an <tt>AbstractContactInviteDialog</tt>.
     * @param parent the parent of this dialog, on which this dialog is
     *               centered and which, when closed, causes this dialog to
     *               be closed too
     * @param title the title to show on the top of this dialog
     * @param okButtonText the text to display on the ok button
     * @param canSelectOffline if true, the user can select offline contacts
     */
    public AbstractContactInviteDialog(Window parent,
                                       String title,
                                       String okButtonText,
                                       boolean canSelectOffline)
    {
        super(parent, title);

        mCanSelectOffline = canSelectOffline;

        initContactListData();

        setOkButtonText(okButtonText);
        addOkButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                okPressed(getSelectedContact(),
                          getSelectedString(),
                          getPhoneNumber());
            }
        });

        // On cancel, just hide
        addCancelButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
                dispose();
            }
        });

        // Call UI is always-on-top by default, so should ignore modal dialogs
        // to avoid appearing over the top of them and getting stuck.
        setExcludeModality(true);
    }

    /**
     * Initializes the contact list with the list of contacts that we wish to
     * display.
     */
    protected void initContactListData()
    {
        ContactSourceService contactSource =
                                new TelephonyProtocolContactSourceServiceImpl();

        mContactList.addContactSource(contactSource);
        mContactList.applyCurrentFilter();
    }

    /**
     * Show an error message in a modal dialog.
     *
     * @param titleRes The resource of the title to display
     * @param messageRes The resource of the message to display
     */
    public void showError(String titleRes, String messageRes)
    {
        ResourceManagementService res = GuiActivator.getResources();
        String title = res.getI18NString(titleRes);
        String message = res.getI18NString(messageRes);

        if (mErrorDialog != null)
        {
            mErrorDialog.setVisible(false);
        }

        mErrorDialog = new ErrorDialog(null, title, message);
        mErrorDialog.setModalExclusionType(
                                 Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        mErrorDialog.setVisible(true);

        if(ConfigurationUtils.isCallAlwaysOnTop())
            mErrorDialog.setAlwaysOnTop(true);
    }

    public void dispose()
    {
        if (mErrorDialog != null)
        {
            mErrorDialog.setVisible(false);
            mErrorDialog.dispose();
            mErrorDialog = null;
        }

        super.dispose();
    }

    /**
     * Retrieve a phone number from a UIContact.  Shows a modal pop-up if the
     * contact has multiple numbers and an error if the contact has no numbers.
     *
     * @param contact The contact to get the number from
     * @return The selected number
     */
    protected String getNumberFromContact(UIContact contact)
    {
        String number;

        List<UIContactDetail> numberDetails = getContactDetails(contact);
        int size = numberDetails.size();

        if (size == 0)
        {
            // No details - show an error dialog to the user
            ResourceManagementService res = GuiActivator.getResources();
            mErrorDialog = new ErrorDialog(null,
                                           res.getI18NString("service.gui.ERROR"),
                                           res.getI18NString("service.gui.NO_NUMBER"));

            mErrorDialog.setModal(true);
            mErrorDialog.setVisible(true);

            number = null;
        }
        else if (size == 1)
        {
            number = numberDetails.get(0).getDisplayName();
        }
        else
        {
            // Multiple details - ask the user to select the right one.
            ContactDetailWrapper[] details = new ContactDetailWrapper[size];

            for (int i = 0; i < size; i++)
            {
                details[i] = new ContactDetailWrapper(numberDetails.get(i));
            }

            ContactDetailWrapper detail = (ContactDetailWrapper)
                SIPCommOptionsDialog.showInputDialog(
                                     "service.gui.SELECT_NUMBER_TITLE",
                                     "service.gui.SELECT_NUMBER",
                                     details,
                                     this,
                                     ConfigurationUtils.isCallAlwaysOnTop(),
                                     true);

            if (detail != null)
            {
                number = detail.getAddress();
            }
            else
            {
                number = null;
            }
        }

        return number;
    }

    /**
     * @return A list of contact details that the user can choose from for a
     *         given {@link UIContact}.
     */
    protected List<UIContactDetail> getContactDetails(UIContact contact)
    {
        return contact.
             getContactDetailsForOperationSet(OperationSetBasicTelephony.class);
    }

    /**
     * Called when the user selects a contact, presses ok or presses enter
     *
     * @param uiContact The contact that is currently selected
     * @param enteredText The string that the user entered
     * @param phoneNumber The phone number retrieved from the entered text
     */
    protected abstract void okPressed(UIContact uiContact,
                                      String enteredText,
                                      String phoneNumber);

    /**
     * A convenience class for allowing a contact detail to be displayed nicely
     * in the detail display window while still being easy to transfer a call to
     */
    private static class ContactDetailWrapper
    {
        private final UIContactDetail mContactDetail;

        public ContactDetailWrapper(UIContactDetail ContactDetail)
        {
            mContactDetail = ContactDetail;
        }

        public String getAddress()
        {
            return mContactDetail.getAddress();
        }

        @Override
        public String toString()
        {
            StringBuilder builder =
                                 new StringBuilder(mContactDetail.getAddress());

            Iterator<String> labels = mContactDetail.getLabels();
            String label = labels.hasNext() ? labels.next() : null;

            if (label != null)
            {
                builder.append(" (").append(label).append(")");
            }

            return builder.toString();
        }
    }
}
