// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.util.Logger;
import org.jitsi.util.StringUtils;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatAddress;

/**
 * A dialog that shows all IM contacts. The user may select multiple contacts
 * to invite them to a group chat
 */
public class AddChatParticipantsDialog
    extends AbstractSelectMultiIMContactsDialog
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AddChatParticipantsDialog.class);

    /**
     * The chat panel that this dialog belongs to
     */
    private ChatPanel chatPanel;

    /**
     * Whether to create a new chat room for these participants, or to add them
     * to an existing room
     */
    private final boolean createNew;

    /**
     * The text field where the user must enter the chat room subject
     */
    private final JTextField chatRoomSubjectField;

    /**
     * The color to use for the border and separator
     */
    private static final Color BORDER_COLOR = new Color(125, 125, 125);

    /**
     * Create the dialog with the default title and 'ok' button text.
     *
     * @param chatPanel the chat panel that this dialog belongs to (this may
     * be null if we are creating a brand new group chat)
     * @param createNew whether to create a new chat room for these participants
     * @param contacts a list of contacts to preselect
     */
    public AddChatParticipantsDialog(ChatPanel chatPanel,
                                    boolean createNew,
                                    Set<MetaContact> contacts)
    {
        this(sResources.getI18NString("service.gui.chat.ADD_PARTICIPANTS_TITLE"),
             sResources.getI18NString("service.gui.chat.ADD_SELECTED_CONTACTS"),
             chatPanel,
             createNew,
             contacts);
    }

    /**
     * Create the dialog with a custom title and 'ok' button text.
     *
     * @param title the title of the dialog
     * @param okButtonText the text for the OK button
     * @param chatPanel the chat panel that this dialog belongs to (this may
     * be null if we are creating a brand new group chat)
     * @param createNew whether to create a new chat room for these participants
     * @param contacts a list of contacts to preselect
     */
    public AddChatParticipantsDialog(String title,
                                    String okButtonText,
                                    ChatPanel chatPanel,
                                    boolean createNew,
                                    Set<MetaContact> contacts)
    {
        super(title,
              okButtonText,
              contacts,
              true);

        logger.debug("Showing add chat participants dialog");

        this.chatPanel = chatPanel;
        this.createNew = createNew;

        // Create a panel to show at the very top of this dialog. This allows
        // the user to set the name of the chat room
        TransparentPanel panel = new TransparentPanel();
        panel.setLayout(new BorderLayout(0, 5));
        JLabel contentLabel = new JLabel(
                sResources.getI18NString("service.gui.chat.ADD_PARTICIPANTS_TO"));
        ScaleUtils.scaleFontAsDefault(contentLabel);
        contentLabel.setOpaque(false);
        panel.add(contentLabel, BorderLayout.NORTH);

        String subject = null;
        // If this is an active conference chat session already then set the
        // text in the subject field to the chat room name
        if (!createNew &&
            this.chatPanel != null &&
            this.chatPanel.getChatSession() instanceof ConferenceChatSession)
        {
            subject = ((ConferenceChatSession)
                this.chatPanel.getChatSession()).getChatSubject();
        }

        // If this isn't already an active conference chat session, set the
        // text in the subject field to the default chat room subject.
        if (subject == null)
        {
            logger.debug("Subject is null so setting default subject");
            String dateString = GuiUtils.formatDate(new Date());
            subject = sResources.getI18NString(
                "service.gui.chat.DEFAULT_CHAT_ROOM_NAME") + " - " + dateString;
        }

        chatRoomSubjectField =
            new SIPCommTextField(null, Chat.MAX_CHAT_ROOM_SUBJECT_LENGTH);
        chatRoomSubjectField.setText(subject);
        chatRoomSubjectField.setBorder(BorderFactory.createCompoundBorder(
                                  BorderFactory.createLineBorder(BORDER_COLOR),
                                  BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        ScaleUtils.scaleFontAsDefault(chatRoomSubjectField);
        panel.add(chatRoomSubjectField, BorderLayout.CENTER);

        // This panel should have padding and a line beneath it. This is
        // created by compounding three borders (gap, line, gap).
        panel.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createEmptyBorder(5, 0, 6, 0),
                  BorderFactory.createCompoundBorder(
                      BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                      BorderFactory.createEmptyBorder(0, 7, 11, 7))));
        setTopPanel(panel);
        setAlwaysOnTop(true);
    }

    @Override
    protected void okPressed(UIContact uiContact,
                             String enteredText,
                             String phoneNumber)
    {
        logger.user("Contacts selected to add to chat: " +
                                   Arrays.toString(mSelectedContacts.toArray()));

        // The user must enter a chat room name of up to the maximum length to
        // continue (if not already present)
        String errorMessage = null;
        String subjectText = chatRoomSubjectField.getText();

        if (StringUtils.isNullOrEmpty(subjectText))
        {
            errorMessage =
                sResources.getI18NString("service.gui.chat.ERROR_ENTER_CHAT_NAME");
        }

        if (errorMessage != null)
        {
            new ErrorDialog(
                sResources.getI18NString("service.gui.chat.ERROR_INVALID_CHAT_NAME"),
                errorMessage).showDialog();
        }
        else
        {
            if (chatPanel != null)
            {
                chatPanel.setLeftChatRoomUI(false);
            }

            // Make sure we dispose this frame before inviting participants,
            // otherwise the main frame will steal focus back from the chat
            // panel when we dispose. Calling super to override the logging in
            // the dispose method overridden here.
            super.dispose();

            inviteContacts(subjectText);
        }
    }

    @Override
    public void dispose()
    {
        logger.user("New group chat dialog cancelled");
        super.dispose();
    }

    /**
     * Invites the selected contacts to the chat conference with the given
     * subject.
     *
     * @param subject The subject of the chat room.
     */
    private void inviteContacts(String subject)
    {
        // Get the IM addresses of the contacts to add
        Set<String> selectedContactAddresses = getContactsToInvite();

        // Invite all selected.
        if (selectedContactAddresses.size() > 0)
        {
            if (chatPanel == null)
            {
                logger.info("Chat panel is null - create new one");
                createRoomAndInvite(subject, selectedContactAddresses);
            }
            else
            {
                chatPanel.inviteContacts(chatPanel.findInviteChatTransport(),
                                         selectedContactAddresses,
                                         null,
                                         createNew,
                                         subject);
            }
        }
    }

    /**
     * @return a collection of the IM addresses of the contacts that we should
     *         invite
     */
    private HashSet<String> getContactsToInvite()
    {
        HashSet<String> contactsToInvite = new HashSet<>();

        for (MetaContact metaContact : mSelectedContacts)
        {
            // Group contacts
            Contact groupContact = metaContact.getGroupContact();
            if (groupContact != null)
            {
                ProtocolProviderService pps = groupContact.getProtocolProvider();
                OperationSetGroupContacts opSet =
                            pps.getOperationSet(OperationSetGroupContacts.class);

                if (opSet == null)
                {
                    logger.warn("Could not get opSet for groupContact " + metaContact);
                    continue;
                }

                for (Contact contact : opSet.getIMContactMembers(groupContact))
                {
                    contactsToInvite.add(contact.getAddress());
                }

                // A MetaContact should not contain anything else.
                continue;
            }

            // IM Contacts - should exist given that there is no group contact
            Contact imContact = metaContact.getIMContact();
            if (imContact == null)
            {
                logger.warn("Could not get IM contact for MetaContact " + metaContact);
                continue;
            }

            contactsToInvite.add(imContact.getAddress());
        }

        logger.debug("Returning contacts to invite: " + sanitiseChatAddress(contactsToInvite.toString()));
        return contactsToInvite;
    }

    /**
     * Creates a new chat room and invites the contacts.
     *
     * @param subject The subject of the new chat room.
     * @param selectedContactAddresses The contacts to invite.
     */
    private void createRoomAndInvite(
        String subject, Collection<String> selectedContactAddresses)
    {
        if (mImProvider != null)
        {
            // Don't specify a room name (jid), as it will be generated
            // automatically.
            AbstractUIServiceImpl uiService = GuiActivator.getUIService();
            ChatRoomWrapper chatRoomWrapper =
                uiService.getConferenceChatManager().
                    createChatRoom(null,
                                   mImProvider,
                                   selectedContactAddresses,
                                   "",
                                   true,
                                   true);

            if (chatRoomWrapper != null)
            {
                try
                {
                    chatRoomWrapper.getChatRoom().setSubject(subject);
                }
                catch (OperationFailedException e)
                {
                    logger.error(
                        "Failed to set chat room subject: " + subject, e);
                }

                ChatWindowManager chatWindowManager = uiService.getChatWindowManager();
                chatPanel = chatWindowManager.getMultiChat(chatRoomWrapper, true);

                if (chatPanel != null)
                {
                    // This line should probably be removed when we are able to
                    // use group chats on the refresh client
                    chatWindowManager.openChat(chatPanel, true);
                }
                else
                {
                    logger.error("Failed to create chat panel for new chat room");
                }
            }
            else
            {
                logger.error("Failed to create new chat room");
            }
        }
        else
        {
            logger.error("Failed to get IM protocol provider");
        }
    }
}
