// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.account.*;

/**
 * An abstract class that can be used to create dialogs to view, edit or create
 * a GroupContact.
 */
public abstract class AbstractGroupContactDialog
    extends AbstractSelectMultiIMContactsDialog
{
    private static final long serialVersionUID = 1L;
    private static final Logger sLog = Logger.getLogger(AbstractGroupContactDialog.class);
    protected static final ImageLoaderService imageLoaderService = GuiActivator.getImageLoaderService();

    /**
     * The minimum width that this dialog is allowed to be shrunk to. Will also
     * be used as the preferred size, if the user hasn't already changed this.
     */
    private static final int MIN_DIALOG_WIDTH = ScaleUtils.scaleInt(430);

    /**
     * The minimum height that this dialog is allowed to be shrunk to. Will
     * also be used as the preferred size, if the user hasn't already changed
     * this.
     */
    private static final int MIN_DIALOG_HEIGHT = ScaleUtils.scaleInt(475);

    /**
     * The size that the avatar icon should be scaled to.
     */
    protected static final int ICON_SIZE = ScaleUtils.scaleInt(60);

    /**
     * The color to use for the border and separator
     */
    protected static final Color BORDER_COLOR = new Color(
        sResources.getColor("service.gui.BORDER_SHADOW"));

    /**
     * All dialogs that implement this class should appear as the same size and
     * in the same location on screen, as they replace each other, as if
     * changing between different pages of the same dialog.  This means that
     * they should all save their size and location under the same config
     * property. This property normally defaults to the class name of the
     * instance, so we override this to always be the name of this abstract
     * class in this case.
     */
    private static final String SAVE_SIZE_AND_LOCATION_CLASS_NAME =
        "net.java.sip.communicator.impl.gui.main.chat.AbstractGroupContactDialog";

    /**
     * The panel at the top of the dialog that contains the avatar, group name
     * and, if viewing the GroupContact, some action buttons.
     */
    protected final TransparentPanel topPanel = new TransparentPanel();

    /**
     * A label used to display the count either of the current number of
     * members of the GroupContact, if viewing, or the current number of
     * selected MetaContacts to include in the GroupContact if adding or
     * editing.
     */
    protected final JLabel contactCountLabel = new JLabel();

    /**
     * The component that displays/allows the user to edit the display name of
     * the GroupContact.
     */
    protected final JComponent groupNameComponent;

    /**
     * The name of the GroupContact.
     */
    private final String groupName;

    /**
     * The GroupContact to be displayed in this dialog.
     */
    protected Contact mGroupContact;

    /**
     * Creates the dialog.
     *
     * @param groupContact the GroupContact (will be null if adding a new
     * GroupContact but must not be null otherwise).
     * @param defaultDisplayName the display name to pre-populate in the group
     * name field.
     * @param description text to display to the user in the dialog to give
     * them extra information about what they can do with the dialog.
     * @param title the title of the dialog
     * @param okButtonText the text for the OK button
     * @param contacts a list of contacts to preselect
     */
    public AbstractGroupContactDialog(Contact groupContact,
                                      String defaultDisplayName,
                                      String description,
                                      String title,
                                      String okButtonText,
                                      Set<MetaContact> contacts)
    {
        super(title, okButtonText, contacts, true);

        sLog.debug("Showing group contact dialog");
        mGroupContact = groupContact;

        // Create a panel to show at the very top of this dialog. This displays
        // the GroupContact avatar, the name of the GroupContact and, if this
        // is the 'view' dialog, the chat and conference buttons.
        topPanel.setLayout(new BorderLayout(0, 5));
        topPanel.add(createIconLabel(), BorderLayout.WEST);
        topPanel.setBorder(ScaleUtils.createEmptyBorder(10, 10, 10, 10));

        // If no default display name has been provided, check whether we have
        // an existing group contact name to pre-populate as the group name.
        if (defaultDisplayName == null)
        {
            groupName =
                (groupContact == null) ? null : groupContact.getDisplayName();
            sLog.debug("No default group name provided - using name from " +
                       "existing group contact: " + logHasher(groupName));
        }
        else
        {
            groupName = defaultDisplayName;
            sLog.debug("Default group name provided: " + logHasher(groupName));
        }

        // Add the group name component to the top panel.
        groupNameComponent = createGroupNameComponent(groupName);
        TransparentPanel groupNamePanel = new TransparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        groupNamePanel.add(groupNameComponent, gbc);
        topPanel.add(groupNamePanel, BorderLayout.CENTER);
        setTopPanel(topPanel);

        if (description != null)
        {
            // Create a label to display beneath the contact list with some extra
            // information about what the user can do with the dialog.
            JLabel descriptionLabel = new JLabel(description, SwingConstants.CENTER);
            descriptionLabel.setOpaque(false);
            descriptionLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                    ScaleUtils.createEmptyBorder(0, 0, 5, 0),
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR)),
                ScaleUtils.createEmptyBorder(0, 5, 5, 5)));
            bottomPanel.add(descriptionLabel, BorderLayout.NORTH);
        }

        // Create the label that displays the number of contacts in the
        // GroupContact / selected to add to the GroupContact.
        contactCountLabel.setText(getContactCountText());
        contactCountLabel.setOpaque(false);
        contactCountLabel.setBorder(ScaleUtils.createEmptyBorder(5, 10, 5, 5));
        bottomPanel.add(contactCountLabel, BorderLayout.WEST);

        setModal(false);
        setPreferredSize(new Dimension(MIN_DIALOG_WIDTH, MIN_DIALOG_HEIGHT));
        setMinimumSize(new Dimension(MIN_DIALOG_WIDTH, MIN_DIALOG_HEIGHT));
        setSaveSizeAndLocationName(SAVE_SIZE_AND_LOCATION_CLASS_NAME);

        // If the IM provider is not registered, call into a method to dispose
        // this dialog, as we need an IM account to view or edit group contacts.
        if (mImProvider == null || !mImProvider.isRegistered())
        {
            sLog.warn("IM Provider not registered");
            handleImAccountUnregistered();
        }
    }

    @Override
    protected void refreshUI()
    {
        sLog.debug("Refreshing Updating UI");

        // We never disable the 'OK' button on group contact dialogs, as it is
        // possible to create a group contact with no members.
        mOkButton.setEnabled(true);

        // We run this method whenever the user has selected/deselected
        // contacts in the list, therefore we need to update the contact count
        // text.
        if (contactCountLabel != null)
        {
            contactCountLabel.setText(getContactCountText());
        }
    }

    /**
     * Creates and returns the component that displays the group name at the
     * top of the dialog.
     * @param name the group name
     *
     * @return the group name component.
     */
    protected abstract JComponent createGroupNameComponent(String name);

    /**
     * @return the text that displays the number of contacts in the
     * GroupContact / selected to add to the GroupContact.
     */
    protected abstract String getContactCountText();

    @Override
    public boolean includeGroupContacts()
    {
        // We never want the user to be able to add group contacts to a group
        // contact, so don't include them in the contact list in this dialog.
        return false;
    }

    @Override
    public boolean allowDeselection()
    {
        // Users remove contacts from a group contact by deselecting them in
        // the contact list, so deselection must always be permitted.
        return true;
    }

    /**
     * @return The current implementation of OperationSetGroupContacts for the
     * current GroupContacts ProtocolProviderService.
     */
    protected OperationSetGroupContacts getOperationSetGroupContacts()
    {
        OperationSetGroupContacts opsetGroupContacts = null;
        ProtocolProviderService groupContactProvider =
                                         AccountUtils.getGroupContactProvider();

        if (groupContactProvider != null)
        {
            opsetGroupContacts =
                groupContactProvider.getOperationSet(OperationSetGroupContacts.class);
        }
        else
        {
            // Can happen when signing in / out of chat
            sLog.error("No GroupContacts protocol provider found");
        }

        return opsetGroupContacts;
    }

    /**
     * @return the label containing the avatar icon for a GroupContact.
     */
    private JLabel createIconLabel()
    {
        JLabel iconLabel = new JLabel();

        ImageIconFuture imageIcon = ImageUtils.getScaledEllipticalIcon(
                imageLoaderService.getImage(ImageLoaderService.DEFAULT_GROUP_CONTACT_PHOTO),
                ICON_SIZE,
                ICON_SIZE);

        imageIcon.addToLabel(iconLabel);
        iconLabel.setBorder(ScaleUtils.createEmptyBorder(0, 5, 0, 10));

        return iconLabel;
    }

    @Override
    protected void handleImAccountUnregistered()
    {
        sLog.debug("IM provider is unregistered: " + mImProvider);
        super.handleImAccountUnregistered();

        // The IM provider has become unregistered so dispose this dialog, as
        // we need an IM account to view or edit group contacts.  Dispose the
        // dialog later to ensure this is called once the constructor
        // completes, as the calling code makes the dialog visible.
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                dispose();
            }
        });
    }
}