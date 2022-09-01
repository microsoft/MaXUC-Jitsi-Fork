// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.service.resources.*;

/**
 * A simple object to help create menu items for adding to existing / creating
 * new group contacts
 */
public class GroupContactMenuUtils
{
    private static final Logger sLog = Logger.getLogger(GroupContactMenuUtils.class);
    private static final Class<OperationSetGroupContacts> OPSET_CLASS = OperationSetGroupContacts.class;
    private static final ResourceManagementService sResources = DesktopUtilActivator.getResources();

    /**
     * The text to display on the 'create group contact' menu item.
     */
    private static final String CREATE_MENU_TEXT =
        sResources.getI18NString("service.gui.groupcontact.MENU_ITEM_CREATE");

    /**
     * The text to display on the "Add Group Contact" menu item
     */
    private static final String CREATE_FILE_MENU_TEXT =
        sResources.getI18NString("service.gui.groupcontact.MENU_ITEM_CREATE_NEWBUTTON");

    /**
     * The text to display on the 'add to group contact' menu item
     */
    private static final String ADD_MENU_TEXT =
        sResources.getI18NString("service.gui.groupcontact.MENU_ITEM_ADD");

    /**
     * The text to display on the 'add participants to group contact' menu item
     */
    private static final String ADD_PARTICIPANTS_MENU_TEXT =
        sResources.getI18NString("service.gui.groupcontact.MENU_ITEM_ADD_ALL");

    /**
     * Create the menu for the "add to group contact" menu item.  This will
     * contain an entry for each of the contact groups, as well as an entry for
     * creating a new group contact containing the contact.
     *
     * @param contact The contact to create the menu for
     */
    public static JMenuItem createGroupContactMenu(MetaContact contact)
    {
        Set<MetaContact> contacts = new HashSet<>();
        contacts.add(contact);
        return createGroupContactMenu(contacts,
                                      null,
                                      contact.getDisplayName(),
                                      true,
                                      ADD_MENU_TEXT);
    }

    /**
     * Create the menu for the "add to group contact" menu item.  This will
     * contain an entry for each of the contact groups, as well as an entry for
     * creating a new group contact with each MetaContact member of the room.
     *
     * @param chatRoom The chatRoom to create the menu for.
     */
    public static JMenuItem createGroupContactSipCommMenu(ChatRoom chatRoom)
    {
        // ChatRoom can be null.
        Set<MetaContact> metaContacts =
            chatRoom == null ? null : chatRoom.getMetaContactMembers();
        String subject = chatRoom == null ? null : chatRoom.getSubject();

        // If any members of the chat room are not MetaContacts, we need to
        // warn the user that they will not be added to the group contact.
        Set<String> nonMetaContacts =
            chatRoom == null ? null : chatRoom.getNonMetaContactMemberJids();
        AccountID imAccount = AccountUtils.getImAccount();
        if (imAccount != null)
        {
            nonMetaContacts.remove(imAccount.getAccountAddress());
        }

        return createGroupContactMenu(metaContacts,
                                      nonMetaContacts,
                                      subject,
                                      false,
                                      ADD_PARTICIPANTS_MENU_TEXT);
    }

    /**
     * @return a menu item for creating a new group contact
     */
    public static JMenuItem createNewGroupContactMenu()
    {
        return createNewGroupContactMenu(null,
                                         null,
                                         getOpSet(),
                                         null,
                                         true,
                                         CREATE_FILE_MENU_TEXT);
    }

    /**
     * Identical to createNewGroupContactMenu except it returns a SIPCommMenuItem
     * rather than a JMenuItem
     *
     * @return a menu item for creating a new group contact
     */
    public static JMenuItem createNewGroupContactSipCommMenu()
    {
        return createNewGroupContactMenu(null,
                                         null,
                                         getOpSet(),
                                         null,
                                         false,
                                         CREATE_FILE_MENU_TEXT);
    }

    /**
     * Enable or disable the menu item depending on whether or not there is a
     * registered GroupContacts operation set.
     *
     * @param menuItem the menu item to enable / disable
     */
    public static void setMenuItemEnabled(JMenuItem menuItem)
    {
        menuItem.setEnabled(getOpSet() != null);
    }

    /**
     * @return the OperationSetGroupContacts or null if one doesn't exist
     */
    private static OperationSetGroupContacts getOpSet()
    {
        ProtocolProviderService provider = getOpSetProvider();
        OperationSetGroupContacts opSet = (provider == null) ?
                                   null : provider.getOperationSet(OPSET_CLASS);
        sLog.debug("Found opSet " + opSet);

        return opSet;
    }

    /**
     * @return the Protocol Provider that implements the group contact op set or
     *         null if one doesn't exist
     */
    private static ProtocolProviderService getOpSetProvider()
    {
        List<ProtocolProviderService> opSetProviders =
                               AccountUtils.getRegisteredProviders(OPSET_CLASS);

        return opSetProviders == null || opSetProviders.isEmpty() ?
                                                   null : opSetProviders.get(0);
    }

    /**
     * Creates the menu which allows a list of MetaContacts to be added to an
     * existing group contact, or new group contact
     *
     * @param metaContacts The MetaContacts to add to the group contact
     * @param nonContacts The addresses of anyone who the user has asked to add
     * to the group contact but who cannot be added as we have no MetaContact
     * for them.
     * @param groupName The name that the new group should have
     * @param isJMenu If true, then a JMenu is created. Otherwise a SIPCommMenu
     * @param menuText The text to put on the menu
     * @return The created MenuItem
     */
    private static JMenuItem createGroupContactMenu(final Set<MetaContact> metaContacts,
                                                    final Set<String> nonContacts,
                                                    String groupName,
                                                    boolean isJMenu,
                                                    String menuText)

    {
        // Overall, this method must get all the group contacts and add them to
        // the sub-menu.
        final ProtocolProviderService provider = getOpSetProvider();

        if (provider == null || !AccountUtils.isImProviderRegistered())
        {
            // If there's no provider, there's no point in continuing.
            sLog.info("No group contact provider " + provider);
            JMenuItem menu = isJMenu ? new JMenuItem(menuText) :
                                       new SIPCommMenuItem(menuText, (BufferedImageFuture) null);
            menu.setEnabled(false);
            return menu;
        }

        JMenu menu = isJMenu ? new JMenu(menuText) :
                               new SIPCommStyledMenu(menuText);
        OperationSetPersistentPresence contactsOpSet =
                 provider.getOperationSet(OperationSetPersistentPresence.class);
        final OperationSetGroupContacts opSet =
                                          provider.getOperationSet(OPSET_CLASS);

        // So first, create a tree set to collect the group contacts into -
        // provide a comparator so that they appear in alphabetical order.
        TreeSet<Contact> groupContacts = new TreeSet<>(new Comparator<>()
        {
            @Override
            public int compare(Contact o1, Contact o2)
            {
                // Order on the display name of each contact
                return String.CASE_INSENSITIVE_ORDER
                        .compare(o1.getDisplayName(),
                                 o2.getDisplayName());
            }
        });

        // Now get all the contacts.
        ContactGroup group = contactsOpSet.getServerStoredContactListRoot();
        Iterator<Contact> contacts = group.contacts();
        while (contacts.hasNext())
        {
            groupContacts.add(contacts.next());
        }

        // Then for each contact add a new menu item
        for (final Contact groupContact : groupContacts)
        {
            JMenuItem contactGroupItem = isJMenu ? new JMenuItem() :
                                                   new SIPCommMenuItem();
            contactGroupItem.setText(groupContact.getDisplayName());
            contactGroupItem.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // No need to hash Contact or MetaContact, as their
                    // toString() methods do that.
                    sLog.info("Adding meta-contacts " + metaContacts +
                                                         " to " + groupContact);

                    // Add the MetaContacts in this group contact to the set of
                    // group contacts to pre-select
                    metaContacts.addAll(opSet.getMetaContactMembers(groupContact));

                    showDialog(groupContact,
                               groupContact.getDisplayName(),
                               metaContacts,
                               nonContacts);
                }
            });

            menu.add(contactGroupItem);
        }

        // Finally, add the "create new group" option
        if (!groupContacts.isEmpty())
        {
            menu.add(isJMenu ? new JSeparator() :
                               SIPCommMenuItem.createSeparator());
        }

        menu.add(createNewGroupContactMenu(metaContacts,
                                           nonContacts,
                                           opSet,
                                           groupName,
                                           isJMenu,
                                           CREATE_MENU_TEXT));

        return menu;
    }

    /**
     * Create a menu item that creates a new group contact
     *
     * @param contacts The contacts to create the group contact for
     * @param nonContacts The addresses of anyone who the user has asked to add
     * to the group contact but who cannot be added as we have no MetaContact
     * for them.
     * @param opSet The operation set that provides group contacts
     * @param groupName The name of the group
     * @param isJMenu If true, then a JMenuItem is created.  Otherwise creates a
     *                SIPCommMenuItem.
     * @param itemText The text to put on the item
     * @return The created menu item
     */
    private static JMenuItem createNewGroupContactMenu(final Set<MetaContact> contacts,
                                                       final Set<String> nonContacts,
                                                       OperationSetGroupContacts opSet,
                                                       final String groupName,
                                                       boolean isJMenu,
                                                       String itemText)
    {
        JMenuItem menuItem = isJMenu ? new JMenuItem(itemText) :
                                       new SIPCommMenuItem(itemText, (BufferedImageFuture) null);

        menuItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // No need to hash Contact or MetaContact, as their toString()
                // methods do that.
                sLog.info("Selecting new group contact for " + contacts);
                showDialog(null, groupName, contacts, nonContacts);
            }
        });

        // Might be no opset.  In which case item should be disabled.
        menuItem.setEnabled(opSet != null);

        return menuItem;
    }

    /**
     * Shows either the add/edit dialog for a group contact directly, or, if
     * 'nonContacts' isn't empty, first shows the warning that some people will
     * not be added to the group contact if they continue.
     *
     * @param groupContact the group contact to edit, will be null if creating
     * a new group contact.
     * @param groupName the name of the group, can be null if creating a new
     * group contact.
     * @param preselectedContacts the contacts to preselect in the contact list.
     * @param nonContacts the addresses of anyone who the user has asked to add
     * to the group contact but who cannot be added as we have no MetaContact
     * for them.
     */
    private static void showDialog(Contact groupContact,
                                   String groupName,
                                   Set<MetaContact> preselectedContacts,
                                   Set<String> nonContacts)
    {
        if (nonContacts != null && nonContacts.size() > 0)
        {
            sLog.info("Displaying NonContactsDialog");
            NonContactsDialog nonContactsDialog = new NonContactsDialog(
                groupContact, groupName, preselectedContacts, nonContacts);
            nonContactsDialog.setModal(true);
            nonContactsDialog.setVisible(true);
        }
        else
        {
            sLog.info("Displaying add/edit dialog for " + groupName);
            JDialog editDialog =
                DesktopUtilActivator.getUIService().createEditGroupContactDialog(
                    groupContact, groupName, preselectedContacts, false);
            editDialog.setVisible(true);
        }
    }

    /**
     * Dialog used to inform the user that some people who they wanted to
     * uplift to a group contact will not be added, as they are not in the
     * user's contact list.
     */
    private static class NonContactsDialog extends ErrorDialog
    {
        private static final long serialVersionUID = 1L;

        /**
         * The title of the dialog.
         */
        private static final String title =
            sResources.getI18NString("service.gui.groupcontact.CANNOT_ADD_TITLE");

        /**
         * The start of the message displayed in the dialog.
         */
        private static final String messageStart = sResources.getI18NString(
            "service.gui.groupcontact.CANNOT_ADD_MESSAGE_START");

        /**
         * The end of the message displayed in the dialog.
         */
        private static final String messageEnd = sResources.getI18NString(
            "service.gui.groupcontact.CANNOT_ADD_MESSAGE_END");

        /**
         * The text to display on the 'continue' button.
         */
        private static final String continueText =
            sResources.getI18NString("service.gui.CONTINUE_ANYWAY");

        /**
         * The text to display on the 'cancel' button.
         */
        private static final String cancelText =
            sResources.getI18NString("service.gui.CANCEL");

        /**
         * The button to continue with adding/edit the group contact.
         */
        private SIPCommBasicTextButton continueButton;

        /**
         * The group contact to edit, will be null if adding a new group
         * contact.
         */
        private final Contact mGroupContact;

        /**
         * The name of the group contact, may be null if adding a new group
         * contact.
         */
        private final String mGroupName;

        /**
         * The contacts to preselect in the contact list, if the user chooses
         * to continue adding the group contact.
         */
        private final Set<MetaContact> mPreselectedContacts;

        /**
         * Creates a new dialog listing the given nonContacts as not going to
         * be added to the group contact.
         *
         * @param groupContact The group contact.
         * @param groupName The group name
         * @param preselectedContacts The MetaContacts to preselect in the
         * contact list.
         * @param nonContacts The addresses of anyone who the user has asked to
         * add to the group contact but who cannot be added as we have no
         * MetaContact for them.
         */
        public NonContactsDialog(Contact groupContact,
                                 String groupName,
                                 Set<MetaContact> preselectedContacts,
                                 Set<String> nonContacts)
        {
            super(null,
                  title,
                  createMessage(nonContacts),
                  ErrorDialog.ErrorType.WARNING,
                  cancelText);

            sLog.debug("Creating 'no contacts' dialog.");
            mGroupContact = groupContact;
            mGroupName = groupName;
            mPreselectedContacts = preselectedContacts;
        }

        @Override
        protected TransparentPanel getButtonsPanel()
        {
            TransparentPanel buttonsPanel = new TransparentPanel();
            continueButton =
                new SIPCommBasicTextButton(continueText);
            continueButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    sLog.info("Continuing to create group contact: " + mGroupName);
                    dispose();

                    JDialog editDialog = DesktopUtilActivator.getUIService().
                        createEditGroupContactDialog(
                            mGroupContact, mGroupName, mPreselectedContacts, false);
                    editDialog.setVisible(true);
                }
            });

            buttonsPanel.add(continueButton);
            buttonsPanel.add(okButton);
            return buttonsPanel;
        }

        /**
         * Creates and returns the message string to display in the dialog
         * regarding the given nonContacts who won't be added to the group
         * contact.
         *
         * @param nonContacts The addresses of anyone who the user has asked to
         * add to the group contact but who cannot be added as we have no
         * MetaContact for them.
         *
         * @return the message string
         */
        private static String createMessage(Set<String> nonContacts)
        {
            // Construct the message to display using the start and end
            // messages from resources and the addresses of any non-contacts in
            // the middle.
            StringBuilder message = new StringBuilder();
            message.append(messageStart);

            for (String nonContact : nonContacts)
            {
                // The start of the message ends in a new line and we also want
                // to put each name on a new line.
                message.append(nonContact + "<br/>");
            }

            message.append(messageEnd);
            return message.toString();
        }
    }
}
