// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.util.*;

/**
 * A dialog that allows the user to add or edit a GroupContact.
 */
public class EditGroupContactDialog extends AbstractGroupContactDialog
{
    private static final long serialVersionUID = 1L;
    private static final Logger sLog = Logger.getLogger(EditGroupContactDialog.class);

    /**
     * The text to display in the description panel for the add/edit dialog.
     */
    private static final String EDIT_DESCRIPTION_TEXT =
        sResources.getI18NString("service.gui.groupcontact.EDIT_GROUP_DESCRIPTION");

    /**
     * The text to display on the 'ok' button of the add/edit dialog.
     */
    private static final String EDIT_OK_TEXT =
        sResources.getI18NString("service.gui.groupcontact.EDIT_GROUP_OK_TEXT");

    /**
     * The title used if the user to is adding a group contact.
     */
    private static final String ADD_TITLE =
        sResources.getI18NString("service.gui.groupcontact.ADD_GROUP_TITLE");

    /**
     * The title used if the user to is editing a group contact.
     */
    private static final String EDIT_TITLE =
        sResources.getI18NString("service.gui.groupcontact.EDIT_GROUP_TITLE");

    /**
     * The hint text displayed in the field that allows the user to set the
     * name of a group contact.
     */
    private static final String GROUP_NAME_EDIT =
        sResources.getI18NString("service.gui.groupcontact.ADD_GROUP_NAME_HINT");

    /**
     * The title of an error dialog shown if the user tries to save a group
     * contact with no group name.
     */
    private static final String ERROR_DIALOG_TITLE = sResources.getI18NString(
        "service.gui.groupcontact.ERROR_NO_GROUP_CONTACT_NAME_TITLE");

    /**
     * The error message shown in an error dialog if the user tries to save a
     * group contact with no group name.
     */
    private static final String ERRROR_DIALOG_MESSAGE = sResources.getI18NString(
        "service.gui.groupcontact.ERROR_NO_GROUP_CONTACT_NAME_MSG");

    /**
     * Creates the dialog.
     *
     * @param groupContact the GroupContact (will be null if adding a new
     * GroupContact but must not be null otherwise).
     * @param defaultDisplayName the display name to pre-populate in the group
     * name field.
     * @param contacts a list of contacts to preselect
     * @param showViewOnCancel if true, display the 'view contact' dialog for the
     * edited group contact when this dialog is cancelled.
     */
    public EditGroupContactDialog(Contact groupContact,
                                  String defaultDisplayName,
                                  Set<MetaContact> contacts,
                                  final boolean showViewOnCancel)
    {
        super(groupContact,
              defaultDisplayName,
              EDIT_DESCRIPTION_TEXT,
              (groupContact == null) ? ADD_TITLE : EDIT_TITLE,
              EDIT_OK_TEXT,
              contacts);

        sLog.debug("Showing add/edit group contact dialog");
        setAlwaysOnTop(true);
        addCancelButtonListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent paramActionEvent)
            {
                // If the user cancels this dialog, we only display the view
                // dialog if we've been asked to do so.
                if (showViewOnCancel)
                {
                    sLog.debug(
                        "Showing view dialog on cancel for " + mGroupContact);
                    showViewDialog();
                }
            }
        });
    }

    @Override
    protected JComponent createGroupNameComponent(String name)
    {
        SIPCommTextField groupNameField =
            new SIPCommTextField(GROUP_NAME_EDIT, Chat.MAX_GROUP_CONTACT_NAME_LENGTH);
        Font font =
            groupNameField.getFont().deriveFont(Font.BOLD, ScaleUtils.scaleInt(16));
        groupNameField.setFont(font);
        groupNameField.setText(name);
        groupNameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 1, BORDER_COLOR),
                    ScaleUtils.createEmptyBorder(2, 5, 2, 2)));

        return groupNameField;
    }

    @Override
    protected String getContactCountText()
    {
        int selectedCount = mSelectedContacts.size();
        String contactCountResource = (selectedCount == 1) ?
            "service.gui.groupcontact.EDIT_GROUP_CONTACT_COUNT_TEXT_ONE" :
            "service.gui.groupcontact.EDIT_GROUP_CONTACT_COUNT_TEXT_MANY";

        return sResources.getI18NString(contactCountResource,
                                       new String []{String.valueOf(selectedCount)});
    }

    @Override
    protected void okPressed(UIContact uiContact,
                             String enteredText,
                             String phoneNumber)
    {
        sLog.user("Contacts selected to add to group contact: " +
                                   Arrays.toString(mSelectedContacts.toArray()));

        // Display an error if the user hasn't entered a group name
        String groupNameText = ((SIPCommTextField) groupNameComponent).getText();
        if (StringUtils.isNullOrEmpty(groupNameText))
        {
            sLog.debug("User entered no group name - displaying error dialog");
            new ErrorDialog(ERROR_DIALOG_TITLE, ERRROR_DIALOG_MESSAGE).showDialog();
        }
        else
        {
            OperationSetGroupContacts opSetGroupContacts =
                                                 getOperationSetGroupContacts();
            if (opSetGroupContacts != null)
            {
                if (mGroupContact == null)
                {
                    sLog.info("Creating new group contact: " + logHasher(groupNameText));
                    // Save the new group contact in the member variable so we
                    // have a reference to it to use to display the 'view'
                    // dialog when we dispose.
                    mGroupContact = opSetGroupContacts.createGroupContact(
                                               groupNameText, mSelectedContacts);
                }
                else
                {
                    sLog.info("Updating group contact: " + logHasher(groupNameText));
                    opSetGroupContacts.setDisplayName(
                                                  mGroupContact, groupNameText);
                    opSetGroupContacts.setMetaContactMembers(
                                               mGroupContact, mSelectedContacts);
                }
            }
            else
            {
                sLog.error("No GroupContacts operation set found");
            }

            dispose();

            // When the user submits the add/edit dialog, we replace it with
            // the view dialog to give confirmation that their changes have
            // taken effect.
            showViewDialog();
        }
    }

    /**
     * Shows the View Group Contact dialog, unless we don't have a
     * GroupContact (i.e. the user chose to add a group contact but cancelled
     * the dialog without creating one), in which case it does nothing.
     */
    private void showViewDialog()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                if (mGroupContact != null)
                {
                    // Make sure we remove the cached window.  It will be done
                    // once this dialog is disposed, but we want to create the
                    // view dialog before then.
                    WindowCacher.remove(mGroupContact);
                    JDialog dialog = GuiActivator.getUIService()
                                   .createViewGroupContactDialog(mGroupContact);
                    dialog.setVisible(true);
                }
            }
        });
    }

    /**
     * Add some more preselected contacts to the sets of selected and preselected
     * contacts.
     *
     * @param contacts The contacts to add
     */
    public void addSelectedContacts(Set<MetaContact> contacts)
    {
        super.addPreselectedContacts(contacts);
    }
}
