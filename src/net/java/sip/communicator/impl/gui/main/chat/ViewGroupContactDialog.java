// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * A dialog that displays all contacts in a GroupContact.  The user may start a
 * meeting or a group chat with those contacts from this dialog.
 */
public class ViewGroupContactDialog
    extends AbstractGroupContactDialog
    implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private static final Logger sLog = Logger.getLogger(ViewGroupContactDialog.class);

    /**
     * The conference service.
     */
    private static final ConferenceService conferenceService =
        GuiActivator.getConferenceService();

    /**
     * The meta contact list service.
     */
    private static final MetaContactListService contactListService =
        GuiActivator.getContactListService();

    /**
     * The text to display on the 'ok' button of the view dialog.
     */
    private static final String VIEW_OK_TEXT =
        sResources.getI18NString("service.gui.groupcontact.VIEW_GROUP_OK_TEXT");

    /**
     * The text to display on the 'cancel' button of the view dialog.
     */
    private static final String VIEW_CANCEL_TEXT =
        sResources.getI18NString("service.gui.OK");

    /**
     * The title of the view dialog.
     */
    private static final String VIEW_TITLE =
        sResources.getI18NString("service.gui.groupcontact.VIEW_GROUP_TITLE");

    /**
     * The tooltip for the chat button
     */
    private static final String CHAT_TOOL_TIP =
        sResources.getI18NString("service.gui.CHAT");

    /**
     * The tooltip for the conference button
     */
    private static final String CONF_TOOL_TIP =
        sResources.getI18NString("service.gui.conf.CONFERENCE_INVITE");

    /**
     * The icon to display if the group contact is a favourite.
     */
    private static final ImageIconFuture FAV_ICON =
        imageLoaderService.getImage(ImageLoaderService.FAVORITE_ICON)
        .getImageIcon();

    /**
     * The panel used to contain the action buttons (chat and meeting) at the
     * top of this dialog.
     */
    private final TransparentPanel actionsPanel = new TransparentPanel();

    /**
     * The label used to display the group name.
     */
    private JLabel groupNameLabel;

    /**
     * The chat button
     */
    private final SIPCommButton chatButton = new SIPCommButton(
        null,
        imageLoaderService.getImage(ImageLoaderService.CHAT_BUTTON_SMALL),
        imageLoaderService.getImage(ImageLoaderService.CHAT_BUTTON_SMALL_ROLLOVER),
        imageLoaderService.getImage(ImageLoaderService.CHAT_BUTTON_SMALL_PRESSED));

    /**
     * The conference button
     */
    private final SIPCommButton conferenceButton = new SIPCommButton(
        null,
        imageLoaderService.getImage(ImageLoaderService.CONFERENCE_BUTTON),
        imageLoaderService.getImage(ImageLoaderService.CONFERENCE_BUTTON_ROLLOVER),
        imageLoaderService.getImage(ImageLoaderService.CONFERENCE_BUTTON_PRESSED));

    /**
     * Creates the dialog.
     *
     * @param groupContact the GroupContact - must not be null.
     */
    public ViewGroupContactDialog(Contact groupContact)
    {
        super(groupContact,
              groupContact.getDisplayName(),
              null,
              VIEW_TITLE,
              VIEW_OK_TEXT,
              null);

        sLog.debug("Showing view group contact dialog");

        // Add the action buttons to the dialog.
        addActionButtons();

        // Set the text on the cancel button. Also, to match the standard 'view
        // contact' dialog, we need to make there be no default button and
        // reverse the order of the buttons from the standard order on this
        // dialog to be the opposite of the normal order for this OS.
        mCancelButton.setText(VIEW_CANCEL_TEXT);
        this.getRootPane().setDefaultButton(null);
        buttonsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        refreshUI();

        // Set up the contact list with the correct look for a group contact.
        mContactList.setMultipleSelectionEnabled(false);
        mContactList.setContactButtonsVisible(true);
        mContactList.setRightButtonMenuEnabled(true);

        // Whenever the window comes into focus we need to update the UI in
        // case something about this group contact has been changed elsewhere.
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowActivated(WindowEvent arg0)
            {
                refreshUI();
            }
        });
    }

    @Override
    protected void refreshUI()
    {
        sLog.debug("Updating UI");
        super.refreshUI();
        updateFavoriteStatus();

        // The actionsPanel will be null if this method is called from the
        // super constructor.
        if (actionsPanel == null)
        {
            return;
        }

        // We only make the buttons visible if their relevant service is
        // enabled.
        boolean confButtonVisible = CreateConferenceMenu.isConferenceInviteByImEnabled();
        boolean groupChatbuttonVisible = ConfigurationUtils.isMultiUserChatEnabled();
        conferenceButton.setVisible(confButtonVisible);
        chatButton.setVisible(groupChatbuttonVisible);

        // If no buttons are visible, we need to hide the panel
        actionsPanel.setVisible(confButtonVisible || groupChatbuttonVisible);

        // Only enable the buttons if this group contact has at least one
        // member.
        boolean buttonsEnabled = getRequiredIMContacts().size() > 0;
        conferenceButton.setEnabled(buttonsEnabled);
        chatButton.setEnabled(buttonsEnabled);
    }
    /**
     * Adds the chat button and a conference button to the dialog.
     */
    private void addActionButtons()
    {
        // Add the buttons but start with the entire buttons UI not visible, as
        // we'll set visibility and enabled state when we refresh the UI, based
        // on which services are available.
        TransparentPanel buttonsPanel = new TransparentPanel();
        buttonsPanel.add(chatButton);
        chatButton.setToolTipText(CHAT_TOOL_TIP);
        chatButton.setName("chat");
        chatButton.setDisabledImage(imageLoaderService.getImage(
            ImageLoaderService.CHAT_BUTTON_SMALL_DISABLED));
        chatButton.setBorder(ScaleUtils.createEmptyBorder(5, 5, 5, 5));
        chatButton.addActionListener(this);
        chatButton.setVisible(false);

        buttonsPanel.add(conferenceButton);
        conferenceButton.setToolTipText(CONF_TOOL_TIP);
        conferenceButton.setName("conf");
        conferenceButton.setDisabledImage(imageLoaderService.getImage(
            ImageLoaderService.CONFERENCE_BUTTON_DISABLED));
        conferenceButton.setBorder(ScaleUtils.createEmptyBorder(5, 5, 5, 5));
        conferenceButton.addActionListener(this);
        conferenceButton.setVisible(false);

        actionsPanel.setLayout(new GridBagLayout());
        actionsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                ScaleUtils.createEmptyBorder(2, 2, 2, 2),
                BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COLOR)),
            ScaleUtils.createEmptyBorder(0, 5, 0, 0)));
        actionsPanel.add(buttonsPanel);
        actionsPanel.setVisible(false);
        topPanel.add(actionsPanel, BorderLayout.EAST);
    }

    @Override
    public boolean onlyDisplayRequiredContacts()
    {
        return true;
    }

    @Override
    public Set<Contact> getRequiredIMContacts()
    {
        Set<Contact> contacts;
        OperationSetGroupContacts opSet = getOperationSetGroupContacts();

        if (opSet != null)
        {
            contacts = opSet.getIMContactMembers(mGroupContact);

            // Cache our latest set of members in the super class so we can
            // display them if we lose connection to the IM server so do not
            // have a group contacts op set.
            setRequiredIMContacts(contacts);
        }
        else
        {
            // This can happen when signing in and out of chat.  Display the
            // most recently cached list of contacts from the super class, as
            // we can't get the latest list from the operation set.
            contacts = super.getRequiredIMContacts();
        }

        return contacts;
    }

    @Override
    protected String getContactCountText()
    {
        int membersCount = getRequiredIMContacts().size();
        String contactCountResource = (membersCount == 1) ?
            "service.gui.groupcontact.VIEW_GROUP_CONTACT_COUNT_TEXT_ONE" :
            "service.gui.groupcontact.VIEW_GROUP_CONTACT_COUNT_TEXT_MANY";

        return sResources.getI18NString(contactCountResource,
                                       new String []{String.valueOf(membersCount)});
    }

    @Override
    protected JComponent createGroupNameComponent(String name)
    {
        groupNameLabel = new JLabel(name);
        groupNameLabel.putClientProperty("html.disable", Boolean.TRUE);
        Font font =
            groupNameLabel.getFont().deriveFont(Font.BOLD, ScaleUtils.scaleInt(16));
        groupNameLabel.setFont(font);
        groupNameLabel.setBorder(ScaleUtils.createEmptyBorder(2, 5, 2, 2));
        groupNameLabel.setOpaque(false);
        groupNameLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        updateFavoriteStatus();
        return groupNameLabel;
    }

    /**
     * Adds or removes the favourite icon in the UI for this group contact, as
     * appropriate.
     */
    private void updateFavoriteStatus()
    {
        // The groupNameLabel will be null if this method is called from the
        // super constructor.
        if (groupNameLabel == null)
        {
            return;
        }

        // Add a star if this contact is a favourite
        MetaContact metaContact =
            contactListService.findMetaContactByContact(mGroupContact);
        if (metaContact != null)
        {
            String favDetail =
                metaContact.getDetail(MetaContact.CONTACT_FAVORITE_PROPERTY);

            if (Boolean.parseBoolean(favDetail))
            {
                FAV_ICON.addToLabel(groupNameLabel);
            }
            else
            {
                groupNameLabel.setIcon(null);
            }
        }
        else
        {
            sLog.warn("No MetaContact found for GroupContact " + mGroupContact);
        }
    }

    @Override
    protected void okPressed(UIContact uiContact,
                             String enteredText,
                             String phoneNumber)
    {
        String displayName = mGroupContact.getDisplayName();
        sLog.debug("User chose to edit group contact " + mGroupContact);

        OperationSetGroupContacts opSetGroupContacts = getOperationSetGroupContacts();
        Set<MetaContact> contacts = new HashSet<>();
        if (opSetGroupContacts != null)
        {
            contacts = opSetGroupContacts.getMetaContactMembers(mGroupContact);
        }
        else
        {
            sLog.error("No Group Contact Operation Set found");
            contacts = new HashSet<>();
        }

        dispose();
        JDialog dialog = GuiActivator.getUIService()
                           .createEditGroupContactDialog(mGroupContact,
                                                         displayName,
                                                         contacts,
                                                         true);
        dialog.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        AbstractButton button = (AbstractButton) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equalsIgnoreCase("chat"))
        {
            sLog.info("Starting chat from group contact");
            OperationSetGroupContacts opSetGroupContacts =
                getOperationSetGroupContacts();

            if (opSetGroupContacts != null)
            {
                opSetGroupContacts.startGroupChat(mGroupContact);
            }
            else
            {
                sLog.error("No Group Contact Operation Set found");
            }
        }
        else if (buttonName.equalsIgnoreCase("conf"))
        {
            sLog.info("Start conference from group contact");
            MetaContact metaContact =
                contactListService.findMetaContactByContact(mGroupContact);
            if (metaContact != null)
            {
                conferenceService.createOrAdd(metaContact, true);
            }
            else
            {
                sLog.error("No MetaContact found for GroupContact " +
                                                mGroupContact);
            }
        }
    }
}
