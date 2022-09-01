// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * The 'more' menu shown in a multi user chat window
 */
public class MultiUserChatMenu
    extends SIPCommPopupMenu implements ActionListener
{
    private static final long serialVersionUID = 0L;
    private static final Logger sLog =
        Logger.getLogger(MultiUserChatMenu.class);

    /**
     * The chat panel that this menu was created in
     */
    private ChatPanel mChatPanel;

    /**
     * The menu item for adding a chat participant
     */
    private JMenuItem mAddParticipantItem;

    /**
     * The menu item for changing the group chat name
     */
    private JMenuItem mChangeGroupNameItem;

    /**
     * The menu item for leaving the group chat
     */
    private JMenuItem mLeaveGroupChatItem;

    /**
     * The menu item for muting the group chat
     */
    private JMenuItem mMuteGroupChatItem;

    /**
     * The menu item for creating a group contact
     */
    private JMenuItem mCreateGroupContactItem;

    /**
     * The separator above the create group contact menu item
     */
    private JSeparator mCreateGroupContactItemSeparator;

    /**
     * The resource management service
     */
    private static final ResourceManagementService sResources =
                                                    GuiActivator.getResources();

    /**
     * Text for the menu item that allows the user to mute the chat.
     * Displayed when the chat is unmuted
     */
    private static final String MUTE_CHAT_TEXT =
                        sResources.getI18NString("service.gui.chat.MUTE_CHAT");

    /**
     * Text for the menu item that allows the user to unmute the chat.
     * Displayed when the chat is muted
     */
    private static final String UNMUTE_CHAT_TEXT =
                      sResources.getI18NString("service.gui.chat.UNMUTE_CHAT");

    /**
     * Creates an instance of <tt>MultiUserChatMenu</tt>.
     */
    public MultiUserChatMenu(ChatPanel chatPanel)
    {
        mChatPanel = chatPanel;
        init();
    }

    /**
     * Initializes the menu, by adding all containing menu items.
     */
    private void init()
    {
        // Need a border on non-mac clients
        if (!OSUtils.IS_MAC)
        {
            Border outside = BorderFactory.createLineBorder(Color.DARK_GRAY);
            Border inside = BorderFactory.createLineBorder(Color.WHITE, 2);
            Border border = BorderFactory.createCompoundBorder(outside, inside);
            setBorder(border);
        }

        mAddParticipantItem = new SIPCommMenuItem(sResources.getI18NString("service.gui.chat.ADD_PARTICIPANTS_TITLE"), (BufferedImageFuture) null);
        mAddParticipantItem.addActionListener(this);
        add(mAddParticipantItem);

        mChangeGroupNameItem = new SIPCommMenuItem(sResources.getI18NString("service.gui.chat.CHANGE_GROUP_NAME"), (BufferedImageFuture) null);
        mChangeGroupNameItem.addActionListener(this);
        add(mChangeGroupNameItem);

        // Item to allow muting the chat
        mMuteGroupChatItem = new SIPCommMenuItem(MUTE_CHAT_TEXT, (BufferedImageFuture) null);
        mMuteGroupChatItem.addActionListener(this);
        add(mMuteGroupChatItem);

        // Update the text in the menu when the menu is opened as the mute state
        // may have changed
        addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
                // Update the mute chat option
                ChatRoom chatRoom = getChatRoom();
                if (chatRoom != null)
                {
                    mMuteGroupChatItem.setText(
                        chatRoom.isMuted() ? UNMUTE_CHAT_TEXT : MUTE_CHAT_TEXT);
                }
                else
                {
                    sLog.error("Hamburger menu displayed for non chat room");
                }

                // Update the create group contact menu item - it might be out
                // of date (it converts the chatroom into a list of contacts
                // when it is created, thus that list might be out of date).
                if (mCreateGroupContactItem != null)
                {
                    remove(mCreateGroupContactItem);
                    remove(mCreateGroupContactItemSeparator);
                }

                // Only add the create group contact menu item (and the
                // separator above it) if group contacts are supported.  Note
                // that we always check whether we need to remove them above,
                // regardless of whether group contacts are currently
                // supported, as it is possible for that to change so there
                // always could be an existing menu item.
                if (ConfigurationUtils.groupContactsSupported())
                {
                    mCreateGroupContactItemSeparator =
                                              SIPCommMenuItem.createSeparator();
                    mCreateGroupContactItem =
                        GroupContactMenuUtils.createGroupContactSipCommMenu(
                                                                 getChatRoom());
                    add(mCreateGroupContactItemSeparator);
                    add(mCreateGroupContactItem);
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        mLeaveGroupChatItem = new SIPCommMenuItem(sResources.getI18NString("service.gui.chat.LEAVE_GROUP_CHAT"), (BufferedImageFuture) null);
        mLeaveGroupChatItem.addActionListener(this);
        add(mLeaveGroupChatItem);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        if (source.equals(mAddParticipantItem))
        {
            sLog.user("Add participants clicked");

            // Pass in the list of current chat room members to the add chat
            // participant dialog, so they will be preselected.
            Set<MetaContact> preselected = null;
            ChatRoom chatRoom = getChatRoom();

            if (chatRoom != null)
            {
                preselected = chatRoom.getMetaContactMembers();
            }

            new AddChatParticipantsDialog(
                              mChatPanel, false, preselected).setVisible(true);
        }
        else if (source.equals(mChangeGroupNameItem))
        {
            sLog.user("Change group name clicked");
            new ChangeGroupNameDialog().setVisible(true);
        }
        else if (source.equals(mMuteGroupChatItem))
        {
            sLog.user("Mute chat toggled");

            ChatRoom chatRoom = getChatRoom();
            if (chatRoom != null)
            {
                // Update the mute state of the window
                chatRoom.toggleMute();
            }
            else
            {
                sLog.error("Attempting to toggle chat room mute state when " +
                                                      "no chat room available");
            }
        }
        else if (source.equals(mLeaveGroupChatItem))
        {
            attemptLeaveGroupChat();
        }
    }

    /**
     * Get the chat room for this chat panel
     *
     * @return the chat room, or null if there is none.
     */
    private ChatRoom getChatRoom()
    {
        if (mChatPanel == null)
            return null;

        ChatSession chatSession = mChatPanel.getChatSession();

        return ((ChatRoomWrapper) chatSession.getDescriptor()).getChatRoom();
    }

    /**
     * Leaves this chat room
     */
    private void leaveChatRoom()
    {
        ConferenceChatManager conferenceManager
                   = GuiActivator.getUIService().getConferenceChatManager();
        conferenceManager.leaveChatRoom(
               (ChatRoomWrapper)mChatPanel.getChatSession().getDescriptor());
    }

    /**
     * Set the chat panel that this menu belongs to
     *
     * @param chatPanel the chat panel that this menu belongs to
     */
    public void setChatPanel(ChatPanel chatPanel)
    {
        mChatPanel = chatPanel;
    }

    /**
     *  Leave the group chat, showing the confirm dialog if necessary
     */
    public void attemptLeaveGroupChat()
    {
        if (!ConfigurationUtils.getDontAskLeaveGroupChat())
        {
            sLog.user("Leave group chat clicked");
            Frame parentFrame = null;
            if (mChatPanel != null)
            {
                parentFrame = mChatPanel.getChatContainer().getFrame();
            }

            MessageDialog dialog = new MessageDialog(parentFrame,
                sResources.getI18NString("service.gui.RESET_DIALOG_TITLE"),
                sResources.getI18NString("service.gui.chat.LEAVE_GROUP_CHAT_DESCRIPTION"),
                sResources.getI18NString("service.gui.chat.LEAVE"));

            int returnCode = dialog.showDialog();

            if (returnCode == MessageDialog.OK_RETURN_CODE)
            {
                sLog.user("Leave chat room, ask again");
                leaveChatRoom();
            }
            else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
            {
                sLog.user("Leave chat room, don't ask again");
                leaveChatRoom();
                ConfigurationUtils.setDontAskLeaveGroupChat(true);
            }
        }
        else
        {
            sLog.info("Leaving chat room without asking");
            leaveChatRoom();
        }
    }

    public ChangeGroupNameDialog getChangeGroupNameDialog()
    {
        return new ChangeGroupNameDialog();
    }

    /**
     * A dialog that allows the user to enter a new group chat name
     */
    public class ChangeGroupNameDialog extends SIPCommDialog
    {
        private static final long serialVersionUID = 0L;

        /**
         * The cancel button that closes this dialog
         */
        private JButton mCancelButton;

        /**
         * The text field where the user can enter the new chat room name
         */
        private JTextField mNameField;

        /**
         * Construct a new dialog
         */
        public ChangeGroupNameDialog()
        {
            super((Frame) mChatPanel.getConversationContainerWindow(), true);

            setTitle(sResources.getI18NString("service.gui.chat.CHANGE_GROUP_NAME"));

            String chatRoomSubject = ((ConferenceChatSession) mChatPanel.getChatSession()).getChatSubject();

            mNameField =
                new SIPCommTextField(null, Chat.MAX_CHAT_ROOM_SUBJECT_LENGTH);
            mNameField.setText(chatRoomSubject);

            // Get the minimum height of the text field by getting the height of
            // a character that could be displayed in the field.
            int minHeight = (int) ComponentUtils.getStringSize(mNameField, "A").getHeight();
            mNameField.setMinimumSize(new Dimension(280, minHeight + 6));
            mNameField.setPreferredSize(new Dimension(280, minHeight + 6));

            TransparentPanel namePanel = new TransparentPanel();
            namePanel.setBorder(BorderFactory.createEmptyBorder(21, 16, 0, 16));
            namePanel.setLayout(new BorderLayout());
            namePanel.add(mNameField, BorderLayout.CENTER);

            mCancelButton = new SIPCommBasicTextButton(
                               sResources.getI18NString("service.gui.CANCEL"));

            mCancelButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // Nothing to do if cancel has been pressed
                    dispose();
                }
            });

            JButton changeButton = new SIPCommBasicTextButton(
                        sResources.getI18NString("plugin.accountinfo.CHANGE"));
            changeButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    String newName = mNameField.getText();

                    // The user must enter a chat room name of up to the
                    // maximum length to continue
                    String errorMessage = null;

                    if (StringUtils.isNullOrEmpty(newName))
                    {
                        errorMessage = sResources.getI18NString(
                            "service.gui.chat.ERROR_ENTER_CHAT_NAME");
                    }

                    if (errorMessage != null)
                    {
                        sLog.debug(
                            "User supplied invalid subject, displaying error: " +
                                                                   errorMessage);
                        ErrorDialog errorDialog = new ErrorDialog(
                            sResources.getI18NString(
                                "service.gui.chat.ERROR_INVALID_CHAT_NAME"),
                            errorMessage,
                            ChangeGroupNameDialog.this);
                        errorDialog.setVisible(true);
                        errorDialog.requestFocus();
                    }
                    else
                    {
                        ConferenceChatSession chatSession = (
                              ConferenceChatSession) mChatPanel.getChatSession();
                        try
                        {
                            chatSession.setChatSubject(newName);
                        }
                        catch (OperationFailedException ex)
                        {
                            sLog.error("Failed to set the chat room subject", ex);
                            String errorTitle = GuiActivator.getResources().getI18NString("service.gui.chat.FAILED_TO_CHANGE_SUBJECT_TITLE");
                            String errorMessageChangeSubject = GuiActivator.getResources().getI18NString("service.gui.chat.FAILED_TO_CHANGE_SUBJECT_CHAT");
                            ErrorDialog errorDialog = new ErrorDialog(null, errorTitle, errorMessageChangeSubject);
                            errorDialog.showDialog();
                        }
                        dispose();
                    }
                }
            });

            // Make the change button the default for this dialog so 'enter'
            // causes the action to be performed
            getRootPane().setDefaultButton(changeButton);

            JPanel buttonPane = new TransparentPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

            if (OSUtils.IS_MAC)
            {
                // Reverse the order of buttons for mac
                buttonPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            }

            buttonPane.add(changeButton);
            buttonPane.add(mCancelButton);

            Container contentPane = getContentPane();
            contentPane.setLayout(new GridLayout(2, 1, 0, 10));
            contentPane.add(namePanel);
            contentPane.add(buttonPane);

            setResizable(false);
        }

        @Override
        protected void close(boolean isEscaped)
        {
            this.dispose();
        }

        @Override
        public void setVisible(boolean isVisible)
        {
            super.setVisible(isVisible);
            mNameField.requestFocusInWindow();
            String text = mNameField.getText();
            int length = (text == null) ? 0 : text.length();
            mNameField.select(0, length);
        }
    }
}