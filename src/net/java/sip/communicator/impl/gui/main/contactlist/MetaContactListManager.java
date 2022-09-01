/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MetaContactListManager</tt> is the class through which we make
 * operations with the <tt>MetaContactList</tt>. All methods in this class are
 * static.
 *
 * @author Yana Stamcheva
 */
public class MetaContactListManager
{
    /**
     * The <tt>Logger</tt> used by the <tt>MetaContactListManager</tt> class
     * and its instances for logging output.
     */
    private static final ContactLogger sLog = ContactLogger.getLogger();

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destMetaContact</tt>.
     * @param srcContact the <tt>Contact</tt> to move
     * @param destMetaContact the destination <tt>MetaContact</tt> to move to
     */
    public static void moveContactToMetaContact(Contact srcContact,
                                                MetaContact destMetaContact)
    {
        sLog.debug(destMetaContact, "Moving contact " + srcContact);
        new MoveContactToMetaContactThread(srcContact, destMetaContact).start();
    }

    /**
     * Moves the given <tt>srcMetaContact</tt> to the <tt>destMetaContact</tt>.
     * @param srcMetaContact the <tt>MetaContact</tt> to move
     * @param destMetaContact the destination <tt>MetaContact</tt> to move to
     */
    public static void moveMetaContactToMetaContact(MetaContact srcMetaContact,
                                                    MetaContact destMetaContact)
    {
        sLog.debug(destMetaContact, "Moving meta contact " + srcMetaContact);
        new MoveMetaContactToMetaContactThread(
            srcMetaContact, destMetaContact).start();
    }

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destGroup</tt>.
     * @param srcContact the <tt>Contact</tt> to move
     * @param destGroup the destination <tt>MetaContactGroup</tt> to move to
     */
    public static void moveContactToGroup(Contact srcContact,
                                          MetaContactGroup destGroup)
    {
        sLog.debug(srcContact, "Moving contact to group " + destGroup);
        new MoveContactToGroupThread(srcContact, destGroup).start();
    }

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destGroup</tt>.
     * @param srcContact the <tt>MetaContact</tt> to move
     * @param group the destination <tt>MetaContactGroup</tt> to move to
     */
    public static void moveMetaContactToGroup(MetaContact srcContact,
                                              MetaContactGroup group)
    {
        sLog.debug(srcContact, "Moving meta contact to group " + group);
        new MoveMetaContactThread(srcContact, group).start();
    }

    /**
     * Moves the given <tt>srcContact</tt> to the <tt>destGroup</tt>.
     * @param srcContact the <tt>MetaContact</tt> to move
     * @param groupID the identifier of the destination <tt>MetaContactGroup</tt>
     * to move to
     */
    public static void moveMetaContactToGroup(MetaContact srcContact,
                                              String groupID)
    {
        sLog.debug(srcContact, "Moving meta contact to group " + groupID);
        new MoveMetaContactThread(srcContact, getGroupByID(groupID)).start();
    }

    /**
     * Removes the given <tt>Contact</tt> from its <tt>MetaContact</tt>.
     * @param contact the <tt>Contact</tt> to remove
     */
    public static void removeContact(Contact contact)
    {
        sLog.debug(contact, "Removing contact");
        new RemoveContactThread(contact).start();
    }

    /**
     * Removes the given <tt>MetaContact</tt> from the list.
     * @param metaContact the <tt>MetaContact</tt> to remove
     */
    public static void removeMetaContact(MetaContact metaContact)
    {
        sLog.debug(metaContact, "Removing MetaContact " + metaContact);
        new RemoveMetaContactThread(metaContact).start();
    }

    /**
     * Removes the given <tt>MetaContactGroup</tt> from the list.
     * @param group the <tt>MetaContactGroup</tt> to remove
     */
    public static void removeMetaContactGroup(MetaContactGroup group)
    {
        sLog.debug("Remove meta contact group " + group);
        new RemoveGroupThread(group).start();
    }

    /**
     * Returns the Meta Contact Group corresponding to the given MetaUID.
     *
     * @param metaUID An identifier of a group.
     * @return The Meta Contact Group corresponding to the given MetaUID.
     */
    private static MetaContactGroup getGroupByID(String metaUID)
    {
        return GuiActivator.getContactListService()
            .findMetaContactGroupByMetaUID(metaUID);
    }

    /**
     * Whether the user should be prompted to confirm contact deletion
     */
    private static boolean getDontAskContactDeletion()
    {
        ConfigurationService configService =
                                         GuiActivator.getConfigurationService();

        return configService.user().getBoolean(
                  "net.java.sip.communicator.impl.gui.DONT_ASK_CONTACT_DELETE",
                  false);
    }

    /**
     * Set that the user should not be prompted to confirm contact deletion
     */
    private static void setDontAskContactDeletion(boolean dontAskContactDeletion)
    {
        ConfigurationService configService =
                                         GuiActivator.getConfigurationService();

        configService.user().setProperty(
                "net.java.sip.communicator.impl.gui.DONT_ASK_CONTACT_DELETE",
                dontAskContactDeletion);
    }

    /**
     * Moves the given <tt>Contact</tt> to the given <tt>MetaContact</tt> and
     * asks user for confirmation.
     */
    private static class MoveContactToMetaContactThread extends Thread
    {
        private final Contact srcContact;
        private final MetaContact destMetaContact;

        public MoveContactToMetaContactThread(Contact srcContact,
                                              MetaContact destMetaContact)
        {
            this.srcContact = srcContact;
            this.destMetaContact = destMetaContact;
        }

        @SuppressWarnings("fallthrough") //intentional
        public void run()
        {
            if (!ConfigurationUtils.isMoveContactConfirmationRequested())
            {
                // we move the specified contact
                GuiActivator.getContactListService()
                    .moveContact(srcContact, destMetaContact);

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcContact.getDisplayName(),
                                destMetaContact.getDisplayName()});

            MessageDialog dialog = new MessageDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationUtils.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // we move the specified contact
                GuiActivator.getContactListService().moveContact(
                    srcContact, destMetaContact);
                break;
            }
        }
    }

    /**
     * Moves the given <tt>Contact</tt> to the given <tt>MetaContact</tt> and
     * asks user for confirmation.
     */
    private static class MoveMetaContactToMetaContactThread extends Thread
    {
        private final MetaContact srcMetaContact;
        private final MetaContact destMetaContact;

        public MoveMetaContactToMetaContactThread(MetaContact srcContact,
                                                  MetaContact destMetaContact)
        {
            this.srcMetaContact = srcContact;
            this.destMetaContact = destMetaContact;
        }

        @SuppressWarnings("fallthrough") //intentional
        public void run()
        {
            if (!ConfigurationUtils.isMoveContactConfirmationRequested())
            {
                // We move all subcontacts of the source MetaContact to the
                // destination MetaContact.
                this.moveAllSubcontacts();

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcMetaContact.getDisplayName(),
                                destMetaContact.getDisplayName()});

            MessageDialog dialog = new MessageDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationUtils.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // We move all subcontacts of the source MetaContact to the
                // destination MetaContact.
                this.moveAllSubcontacts();
                break;
            }
        }

        /**
         * Move all subcontacts of the <tt>srcMetaContact</tt> to
         * <tt>destMetaContact</tt>.
         */
        private void moveAllSubcontacts()
        {
            Iterator<Contact> contacts = srcMetaContact.getContacts();
            while(contacts.hasNext())
            {
                GuiActivator.getContactListService().moveContact(
                    contacts.next(), destMetaContact);
            }
        }
    }

    /**
     * Moves the given <tt>Contact</tt> to the given <tt>MetaContactGroup</tt>
     * and asks user for confirmation.
     */
    @SuppressWarnings("fallthrough")
    private static class MoveContactToGroupThread extends Thread
    {
        private final Contact srcContact;
        private final MetaContactGroup destGroup;

        public MoveContactToGroupThread(Contact srcContact,
                                        MetaContactGroup destGroup)
        {
            this.srcContact = srcContact;
            this.destGroup = destGroup;
        }

        public void run()
        {
            if (!ConfigurationUtils.isMoveContactConfirmationRequested())
            {
                // we move the specified contact
                GuiActivator.getContactListService()
                    .moveContact(srcContact, destGroup);

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{   srcContact.getDisplayName(),
                                destGroup.getGroupName()});

            MessageDialog dialog = new MessageDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            switch (dialog.showDialog())
            {
            case MessageDialog.OK_DONT_ASK_CODE:
                ConfigurationUtils.setMoveContactConfirmationRequested(false);
                // do fall through

            case MessageDialog.OK_RETURN_CODE:
                // we move the specified contact
                GuiActivator.getContactListService()
                    .moveContact(srcContact, destGroup);
                break;
            }
        }
    }

    /**
     * Moves the given <tt>MetaContact</tt> to the given
     * <tt>MetaContactGroup</tt> and asks user for confirmation.
     */
    private static class MoveMetaContactThread
        extends Thread
    {
        private final MetaContact srcContact;
        private final MetaContactGroup destGroup;

        public MoveMetaContactThread(MetaContact srcContact,
                                     MetaContactGroup destGroup)
        {
            this.srcContact = srcContact;
            this.destGroup = destGroup;
        }

        @SuppressWarnings("fallthrough")
        public void run()
        {
            if (!ConfigurationUtils.isMoveContactConfirmationRequested())
            {
                // we move the specified contact
                try
                {
                    GuiActivator.getContactListService()
                        .moveMetaContact(srcContact, destGroup);
                }
                catch (MetaContactListException e)
                {
                }

                return;
            }

            String message = GuiActivator.getResources().getI18NString(
                "service.gui.MOVE_SUBCONTACT_QUESTION",
                new String[]{ srcContact.getDisplayName(),
                              destGroup.getGroupName()});

            MessageDialog dialog = new MessageDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE_CONTACT"),
                    message,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.MOVE"));

            try
            {
                switch (dialog.showDialog())
                {
                case MessageDialog.OK_DONT_ASK_CODE:
                    ConfigurationUtils
                        .setMoveContactConfirmationRequested(false);
                    // do fall through

                case MessageDialog.OK_RETURN_CODE:
                    // we move the specified contact
                    GuiActivator.getContactListService()
                        .moveMetaContact(srcContact, destGroup);
                    break;
                }
            }
            catch (MetaContactListException e)
            {
                // Error thrown while moving the contact - assume that it is
                // because a sub contact does not support contact groups
                sLog.error(srcContact, "Unable to move contact", e);

                String title = GuiActivator.getResources()
                    .getI18NString("service.gui.ERROR");
                String errorMessage = GuiActivator.getResources()
                    .getI18NString("service.gui.MOVE_TO_GROUP_NOT_SUPPORTED");
                new ErrorDialog(null, title, errorMessage).showDialog();
            }
        }
    }

    /**
     * Removes a contact from a meta contact in a separate thread.
     */
    private static class RemoveContactThread extends Thread
    {
        private Contact contact;
        public RemoveContactThread(Contact contact)
        {
            this.contact = contact;
        }

        public void run()
        {
            sLog.info(contact, "About to remove contact");

            if (!contact.getProtocolProvider().isRegistered())
            {
                sLog.warn(contact, "Provider not registered, can't delete");
                new ErrorDialog(
                    GuiActivator.getUIService().getMainFrame(),
                    GuiActivator.getResources().getI18NString(
                    "service.gui.ADD_CONTACT_ERROR_TITLE"),
                    GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE_CONTACT_NOT_CONNECTED"),
                    ErrorDialog.ErrorType.WARNING)
                .showDialog();

                return;
            }

            try
            {
                if(!getDontAskContactDeletion())
                {
                    sLog.debug(contact, "Asking user to confirm delete");
                    String message = GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_CONTACT_TEXT",
                        new String[]{contact.getDisplayName()});

                    MessageDialog dialog = new MessageDialog(
                        null,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.REMOVE_CONTACT"),
                        message,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.REMOVE"));

                    int returnCode = dialog.showDialog();

                    if (returnCode == MessageDialog.OK_RETURN_CODE)
                    {
                        sLog.info(contact, "Remove contact, ask again");
                        GuiActivator.getContactListService()
                            .removeContact(contact);
                    }
                    else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
                    {
                        sLog.info(contact, "Remove contact, don't ask again");
                        GuiActivator.getContactListService()
                            .removeContact(contact);

                        setDontAskContactDeletion(true);
                    }
                }
                else
                {
                    sLog.debug(contact, "Just removing contact");
                    GuiActivator.getContactListService().removeContact(contact);
                }
            }
            catch (Exception ex)
            {
                sLog.error(contact, "Failed to remove contact", ex);
                new ErrorDialog(null,
                                GuiActivator.getResources().getI18NString(
                                "service.gui.REMOVE_CONTACT"),
                                ex.getMessage(),
                                ex)
                            .showDialog();
            }
        }
    }

    /**
     * Removes a contact from a meta contact in a separate thread.
     */
    private static class RemoveMetaContactThread extends Thread
    {
        private MetaContact metaContact;
        public RemoveMetaContactThread(MetaContact contact)
        {
            this.metaContact = contact;
        }

        public void run()
        {
            try
            {
                sLog.info(metaContact, "About to remove meta contact");

                if(!getDontAskContactDeletion())
                {
                    sLog.debug(metaContact, "Asking user to confirm MC delete");
                    String message
                        = GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE_CONTACT_TEXT",
                            new String[]{metaContact.getDisplayName()});

                    MessageDialog dialog = new MessageDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE_CONTACT"),
                        message,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE"));

                    int returnCode = dialog.showDialog();

                    if (returnCode == MessageDialog.OK_RETURN_CODE)
                    {
                        sLog.info(metaContact, "Remove MC, ask again");
                        GuiActivator.getContactListService()
                            .removeMetaContact(metaContact);
                    }
                    else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
                    {
                        sLog.info(metaContact, "Remove MC, don't ask again");
                        GuiActivator.getContactListService()
                            .removeMetaContact(metaContact);

                        setDontAskContactDeletion(true);
                    }
                }
                else
                {
                    sLog.debug(metaContact, "Just remove MC");
                    GuiActivator.getContactListService()
                        .removeMetaContact(metaContact);
                }
            }
            catch (MetaContactListException e)
            {
                sLog.error(metaContact, "Unable to delete contact", e);

                String title = GuiActivator.getResources()
                    .getI18NString("service.gui.ERROR");
                String message = GuiActivator.getResources()
                    .getI18NString("service.gui.REMOVE_CONTACT_NOT_CONNECTED");
                new ErrorDialog(null, title, message).showDialog();
            }
        }
    }

    /**
     * Removes a group from the contact list in a separate thread.
     */
    private static class RemoveGroupThread extends Thread
    {
        private MetaContactGroup group;

        public RemoveGroupThread(MetaContactGroup group)
        {
            this.group = group;
        }
        public void run()
        {
            try
            {
                if(!getDontAskContactDeletion()) {
                    String message = GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_CONTACT_TEXT",
                        new String[]{group.getGroupName()});

                    MessageDialog dialog = new MessageDialog(
                        null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE_GROUP"),
                        message,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE"));

                    int returnCode = dialog.showDialog();

                    if (returnCode == MessageDialog.OK_RETURN_CODE)
                    {
                        GuiActivator.getContactListService()
                            .removeMetaContactGroup(group);
                    }
                    else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
                    {
                        GuiActivator.getContactListService()
                            .removeMetaContactGroup(group);

                        setDontAskContactDeletion(true);
                    }
                }
                else
                    GuiActivator.getContactListService()
                        .removeMetaContactGroup(group);
            }
            catch (Exception ex)
            {
                sLog.error("Failed to remove contact group.", ex);
                new ErrorDialog(null,
                                GuiActivator.getResources().getI18NString(
                                "service.gui.REMOVE_GROUP"),
                                ex.getMessage(),
                                ex)
                .showDialog();
            }
        }
    }
}
