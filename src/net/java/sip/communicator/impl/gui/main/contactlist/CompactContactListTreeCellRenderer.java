/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The <tt>CompactContactListCellRenderer</tt> is the compact version of the
 * custom cell renderer used in Jitsi's <tt>ContactList</tt>.  It extends
 * <tt>ContactListTreeCellRenderer</tt>
 */
public class CompactContactListTreeCellRenderer
    extends DefaultContactListTreeCellRenderer
{
    private static final long serialVersionUID = 0L;

    private static final Logger sLog =
        Logger.getLogger(CompactContactListTreeCellRenderer.class);

    /**
     * The height of the status icon.  All status icons should be the same
     * dimensions so we can simply use the height of the default no status
     * icon to calculate this.  The value returned here is already scaled to
     * the OS's current scaling so there is no need to scale it again.  Also,
     * given we're only getting this value once per contact list (not per
     * contact cell), it is fine to resolve the image immediately, as there is
     * no noticeable performance impact so it is not worth adding a resolution
     * listener.
     */
    private static final int STATUS_ICON_HEIGHT =
                                 USER_NO_STATUS_ICON.resolve().getIconHeight();

    public CompactContactListTreeCellRenderer()
    {
        // The GridBag layout used for each cell rendered by this object. The
        // buttons are added dynamically from right to left depending on the
        // contact's abilities. Group nodes use the same layout and repurpose
        // the labels to show the correct items.
        // ----------------------------------------------------
        // |        | contact addr |   IM   |  Call  | Video  |
        // | avatar |--------------| Button | Button | Button |
        // |        | icon | label |        |        |        |
        // ----------------------------------------------------

        // Initialize class variables. The height is the default height of a
        // JPanel, scaled accordingly.
        int height = ScaleUtils.scaleInt(18);

        // Each row (apart from the show more item) has 2 rows of text and some padding:
        mRowHeight = 2 * height + ScaleUtils.scaleInt(10);
        mSelectedRowHeight = mRowHeight;
        mShowMoreHeight = height + ScaleUtils.scaleInt(3);

        mAvatarHeight = mRowHeight - ScaleUtils.scaleInt(10);
        mAvatarWidth = mAvatarHeight;
        mExtendedAvatarHeight = mAvatarHeight;
        mExtendedAvatarWidth = mAvatarHeight;

        // The border around this panel
        mLeftBorder = 0;
        mTopBorder = 0;
        mBottomBorder = 0;
        mRightBorder = ScaleUtils.scaleInt(4);

        initializeLabelConstraints();
    }

    @Override
    protected void initializeLabelConstraints()
    {
        // Initialize the GridBag constraints for each label.
        //
        // gridx and gridy denote the position within the GridBag that the
        // label should be placed.
        // gridheight and gridwidth denote how many cells the label uses
        // within the GridBag
        // weightx and weighty define how the cells within the GridBag are
        // drawn
        mAvatarLabelConstraints.anchor = GridBagConstraints.CENTER;
        mAvatarLabelConstraints.fill = GridBagConstraints.VERTICAL;
        mAvatarLabelConstraints.gridx = 0;
        mAvatarLabelConstraints.gridy = 0;
        mAvatarLabelConstraints.gridheight = 2;
        mAvatarLabelConstraints.gridwidth = 1;
        mAvatarLabelConstraints.weightx = 0;
        mAvatarLabelConstraints.weighty = 1;

        mNameLabelConstraints.anchor = GridBagConstraints.WEST;
        mNameLabelConstraints.fill = GridBagConstraints.NONE;
        mNameLabelConstraints.gridx = 1;
        mNameLabelConstraints.gridy = 0;
        mNameLabelConstraints.gridheight = 1;
        mNameLabelConstraints.gridwidth = 2;
        mNameLabelConstraints.weightx = 1;
        mNameLabelConstraints.weighty = 0;

        mStatusLabelConstraints.anchor = GridBagConstraints.CENTER;
        mStatusLabelConstraints.fill = GridBagConstraints.VERTICAL;
        mStatusLabelConstraints.gridx = 1;
        mStatusLabelConstraints.gridy = 1;
        mStatusLabelConstraints.gridheight = 1;
        mStatusLabelConstraints.gridwidth = 1;
        mStatusLabelConstraints.weightx = 0;
        mStatusLabelConstraints.weighty = 0;

        mDisplayDetailsLabelConstraints.anchor = GridBagConstraints.CENTER;
        mDisplayDetailsLabelConstraints.fill = GridBagConstraints.VERTICAL;
        mDisplayDetailsLabelConstraints.gridx = 2;
        mDisplayDetailsLabelConstraints.gridy = 1;
        mDisplayDetailsLabelConstraints.gridheight = 1;
        mDisplayDetailsLabelConstraints.gridwidth = 1;
        mDisplayDetailsLabelConstraints.weightx = 1;
        mDisplayDetailsLabelConstraints.weighty = 0;

        // Initialize the common constraints for the action buttons
        int selectedPadding = mIsSelected ? ScaleUtils.scaleInt(2) : 0;
        int actionButtonPadding =
            (mRowHeight - BUTTON_HEIGHT - selectedPadding) / 2;

        mActionButtonConstraints.insets.set(actionButtonPadding, 0,
                                           actionButtonPadding, 0);
        mActionButtonConstraints.anchor = GridBagConstraints.CENTER;
        mActionButtonConstraints.fill = GridBagConstraints.HORIZONTAL;
        mActionButtonConstraints.gridy = 0;
        mActionButtonConstraints.gridwidth = 1;
        mActionButtonConstraints.gridheight = 2;
        mActionButtonConstraints.weightx = 0;
        mActionButtonConstraints.weighty = 0;

        // Selected icon constraints
        // Initialize the common constraints for the action buttons
        mSelectedIconConstraints.insets = new Insets(0, 0, 0, 0);
        mSelectedIconConstraints.anchor = GridBagConstraints.CENTER;
        mSelectedIconConstraints.fill = GridBagConstraints.NONE;
        mSelectedIconConstraints.gridy = 0;
        mSelectedIconConstraints.gridx = 4;
        mSelectedIconConstraints.gridwidth = 1;
        mSelectedIconConstraints.gridheight = 2;
        mSelectedIconConstraints.weightx = 0;
        mSelectedIconConstraints.weighty = 0;
    }

    @Override
    protected void addLabels(int nameLabelGridWidth)
    {
        // There are many small changes in this method compared to the parent
        // method. The differences are in how the group nodes are displayed
        // and the gridbag constraints. The parent method relies on
        // nameLabelGridWidth to position labels whereas this method does not.

        // Remove all labels and recreate them. The GridBag constraints
        // for each label are explicitly set here for safety's sake.
        remove(mNameLabel);
        remove(mAvatarLabel);
        remove(mDisplayDetailsLabel);
        remove(mStatusLabel);

        // Re-initialize the label constraints for safety
        initializeLabelConstraints();

        // Set up the internal padding for contacts and group nodes
        Insets groupNodeInsets = new Insets(0, 0, V_GAP, H_GAP);

        // Set up the padding around each label for contact nodes
        int statusBottomPadding = ScaleUtils.scaleInt(6);
        int statusTopPadding = (mRowHeight / 2) -
                                      STATUS_ICON_HEIGHT - statusBottomPadding;
        int avatarPadding = (mRowHeight - mAvatarHeight) / 2;

        // If the cell is selected we add a 1px border to top and bottom, make
        // allowances for this in the insets.
        int selectedPadding = mIsSelected ? 1 : 0;

        Insets nameLabelInsets = new Insets(statusBottomPadding -
                                                ScaleUtils.scaleInt(4) -
                                                selectedPadding,
                                            0,
                                            statusBottomPadding - 1,
                                            0);
        Insets avatarLabelInsets = new Insets(avatarPadding - selectedPadding,
                                              avatarPadding,
                                              avatarPadding - selectedPadding,
                                              avatarPadding);
        Insets statusLabelInsets = new Insets(statusTopPadding,
                                              0,
                                              statusBottomPadding -
                                                               selectedPadding,
                                              0);
        Insets displayDetailsLabelInsets = new Insets(statusTopPadding,
                                                      ScaleUtils.scaleInt(4),
                                                      statusBottomPadding -
                                                               selectedPadding,
                                                      0);

        // Set the padding for contacts and group nodes. Right align the
        // display details label for group nodes.
        if (mTreeNode instanceof GroupNode)
        {
            mDisplayDetailsLabelConstraints.anchor = GridBagConstraints.EAST;
            nameLabelInsets = groupNodeInsets;
            avatarLabelInsets = groupNodeInsets;
            statusLabelInsets = groupNodeInsets;
            displayDetailsLabelInsets = groupNodeInsets;
        }
        else
        {
            mDisplayDetailsLabelConstraints.anchor = GridBagConstraints.WEST;
        }

        mNameLabelConstraints.insets = nameLabelInsets;
        mAvatarLabelConstraints.insets = avatarLabelInsets;
        mStatusLabelConstraints.insets = statusLabelInsets;
        mDisplayDetailsLabelConstraints.insets = displayDetailsLabelInsets;

        // Add the labels
        add(mAvatarLabel, mAvatarLabelConstraints);
        add(mNameLabel, mNameLabelConstraints);
        add(mStatusLabel, mStatusLabelConstraints);
        add(mDisplayDetailsLabel, mDisplayDetailsLabelConstraints);
    }

    @Override
    protected void initButtonsPanel(UIContact uiContact,
                                    UIContact uiSourceContact)
    {
        removeActionButtons(true);

        boolean isMouseOver = (mMouseOverContact != null &&
                               (mMouseOverContact == uiContact ||
                                mMouseOverContact == uiSourceContact));

        // If the cell is not selected, or the contact is anonymous then
        // there is nothing to do here
        if (isContactAnonymous(uiContact) || (!isMouseOver && !mIsSelected))
            return;

        List<UIContactDetail> telephonyContacts = new
                LinkedList<>();

        UIContactImpl contactDescriptor =
            ((ContactNode) mTreeNode).getContactDescriptor();

        if (!(contactDescriptor instanceof MetaUIContact) &&
            !(contactDescriptor instanceof ShowMoreContact))
        {
            telephonyContacts.addAll(
                contactDescriptor.getContactDetailsForOperationSet(
                    OperationSetBasicTelephony.class));
        }
        else if (uiContact.getDescriptor() instanceof MetaContact)
        {
            ContactPhoneUtil contactPhoneUtil = ContactPhoneUtil.getPhoneUtil(
                (MetaContact)uiContact.getDescriptor());

            telephonyContacts.addAll(contactPhoneUtil.getAdditionalNumbers());
        }

        // Convert the list of telephony contacts into a set of telephony
        // details in order to remove duplicate phone numbers
        Set<String> telephonyDetails = new HashSet<>();
        for (UIContactDetail telephonyContact : telephonyContacts)
        {
            telephonyDetails.add(telephonyContact.getAddress());
        }

        int telephonyDetailCount = telephonyDetails.size();

        boolean hasPhone = (telephonyDetailCount > 0);
        boolean canCallContact = canCallContact(uiContact, hasPhone);

        setCallButtonImages(telephonyDetailCount);

        boolean isMessageHistoryContact = false;

        SourceContact sourceContact = null;
        UIContact contact = (uiSourceContact != null) ? uiSourceContact : uiContact;
        if (contact instanceof SourceUIContact)
        {
            sourceContact = ((SourceUIContact) contact).getSourceContact();
            isMessageHistoryContact = sourceContact.getContactSource().getType() ==
                                      ContactSourceService.MESSAGE_HISTORY_TYPE;
        }

        // Initialize a list to hold all the buttons to be added.
        Collection<SIPCommButton> contactButtons
            = new LinkedList<>();

        // Add an IM button (if IM is enabled) regardless of whether the
        // contact is an IM contact or not - it will be disabled if we can't
        // chat to it.  Don't add an IM button if this isn't a contact, unless
        // it is a history entry that can be messaged.
        if (ConfigurationUtils.isImEnabled() &&
            (!canAddContact(uiContact) ||
                (uiContact instanceof SourceUIContact && uiContact.canBeMessaged())))
        {
            // IM is enabled so we may be able to chat to the contact if we
            // have an IM account
            ProtocolProviderService imProvider = AccountUtils.getImProvider();

            if (imProvider != null)
            {
                // We have an IM account, so add the chat button
                boolean isGroupContact = isGroupContact(uiContact);
                boolean isImOnline = imProvider.isRegistered();

                boolean isViewableChatRoom = false;
                if (isMessageHistoryContact && (sourceContact != null))
                {
                    // This is a message history SourceContact, so it might
                    // represent a chat room that the user should be able to
                    // view.
                    isViewableChatRoom = isViewableChatRoom(sourceContact);
                }

                // A contact is suitable for enabling the chat button if one of
                // the following is true:
                //  - It is an IM contact.
                //  - It is a message history contact that represents a chat
                //    room that the user should be able to view.
                //  - It is a group contact and group chat is enabled.
                boolean isSuitableContact =
                    isIMContact(uiContact, hasPhone) ||
                    isViewableChatRoom ||
                    (isGroupContact && ConfigurationUtils.isMultiUserChatEnabled());

                String toolTipRes =
                    !isSuitableContact ? "service.gui.SEND_MESSAGE_NO_ADDRESS" :
                        !isImOnline ? "service.gui.SEND_MESSAGE_NO_SERVICE" :
                            isGroupContact ? "service.gui.SEND_GROUP_CHAT_TO" :
                                "service.gui.SEND_MESSAGE";

                // Finally, we only actually enable the chat button if all of
                // the following is true:
                //  - The contact is suitable for enabling the chat button (see
                // above).
                //  - The contact can currently be messaged.
                boolean enabled = isSuitableContact &&
                                  uiContact.canBeMessaged();

                if (isMouseOver)
                {
                    // The contact must be IM-capable and must have either
                    // accepted our buddy request (or vice-versa) or be a Group
                    // Contact.
                    mRolloverChatButton.setEnabled(enabled);

                    mRolloverChatButton.setToolTipText(GuiActivator.getResources()
                                                          .getI18NString(toolTipRes));
                    mRolloverChatButton.setDisabledImage(sImageLoaderService.getImage(
                        ImageLoaderService.CHAT_BUTTON_SMALL_DISABLED));

                    contactButtons.add(mRolloverChatButton);
                }
                else
                {
                    // The contact must be IM-capable and must have either
                    // accepted our buddy request (or vice-versa) or be a Group
                    // Contact.
                    mChatButton.setEnabled(enabled);

                    mChatButton.setToolTipText(GuiActivator.getResources()
                                                          .getI18NString(toolTipRes));
                    mChatButton.setDisabledImage(sImageLoaderService.getImage(
                        ImageLoaderService.CHAT_BUTTON_SMALL_DISABLED));

                    contactButtons.add(mChatButton);
                }
            }
        }

        // Add the call button if this isn't a message history entry and calls are enabled.
        // But disable the button if the contact has no phone numbers
        if (!isMessageHistoryContact && ConfigurationUtils.isCallingEnabled())
        {
            if (isMouseOver)
            {
                mRolloverCallButton.setEnabled(hasPhone && canCallContact);
                String toolTipRes = !hasPhone ? "service.gui.CALL_CONTACT_NO_PHONE" :
                                    !canCallContact ? "service.gui.CALL_CONTACT_NO_SERVICE" :
                                    "service.gui.CALL_CONTACT";
                mRolloverCallButton.setToolTipText(GuiActivator.getResources()
                                                      .getI18NString(toolTipRes));
                contactButtons.add(mRolloverCallButton);
            }
            else
            {
                mCallButton.setEnabled(hasPhone && canCallContact);
                String toolTipRes = !hasPhone ? "service.gui.CALL_CONTACT_NO_PHONE" :
                                    !canCallContact ? "service.gui.CALL_CONTACT_NO_SERVICE" :
                                    "service.gui.CALL_CONTACT";
                mCallButton.setToolTipText(GuiActivator.getResources()
                                                      .getI18NString(toolTipRes));
                contactButtons.add(mCallButton);
            }
        }

        // Add the invite to meeting button if appropriate.
        if (canInviteContactToMeeting(uiContact, hasPhone))
        {
            if (isMouseOver)
            {
                contactButtons.add(mRolloverInviteToMeetingButton);
            }
            else
            {
                contactButtons.add(mInviteToMeetingButton);
            }
        }

        // Add the video button if appropriate.
        if (canVideoCallContact(uiContact, hasPhone))
        {
            if (isMouseOver)
            {
                contactButtons.add(mRolloverCallVideoButton);
            }
            else
            {
                contactButtons.add(mCallVideoButton);
            }
        }

        // Add the add contact button if appropriate.
        if (canAddContact(uiContact))
        {
            if (isMouseOver)
            {
                contactButtons.add(mRolloverAddContactButton);
            }
            else
            {
                contactButtons.add(mAddContactButton);
            }
        }

        // Add all buttons that this contact supports.
        // Start adding the buttons at GridBag x co-ordinate 3. This is the
        // cell to the right of the name label.
        int gridX = 3;

        // xCoord is the number of pixels from the left hand edge of the pane
        // to the left hand edge of the button.
        int xCoord = mTreeContactList.getWidth();

        // buttonGap is the number of pixels between each action button
        int buttonGap = ScaleUtils.scaleInt(3);
        xCoord -= mRightBorder + (contactButtons.size() *
                                                   (BUTTON_HEIGHT + buttonGap));

        // Add the buttons to the pane and update the value of x accordingly
        Iterator<SIPCommButton> buttonsIter = contactButtons.iterator();

        while (buttonsIter.hasNext())
        {
            SIPCommButton actionButton = buttonsIter.next();
            xCoord += addButton(actionButton, gridX, xCoord, false) + buttonGap;
            gridX++;
        }

        // The list of the contact actions. We will create a button for every
        // action
        Collection<SIPCommButton> contactActions
            = uiContact.getContactCustomActionButtons();

        if ((contactActions != null) && (contactActions.size() > 0))
        {
            initContactActionButtons(contactActions, gridX, xCoord);
            gridX++;
        }
        else
        {
            addLabels(mNameLabelConstraints.gridwidth);
        }

        if (mLastAddedButton != null)
            setButtonBg(mLastAddedButton, gridX, true);

        setBounds(0, 0, mTreeContactList.getWidth(), getPreferredSize().height);
    }

    /**
     * Returns true if both this SourceContact represents a viewable chat room,
     * false otherwise.
     *
     * @param sourceContact the SourceContact to check.
     *
     * @return true if the user should be able to view the chat room, false
     * otherwise.
     */
    private boolean isViewableChatRoom(SourceContact sourceContact)
    {
        // If there are multi-user chat details, this is a history entry for a
        // chat room so we can continue.
        List<ContactDetail> mucContactDetails = sourceContact.
            getContactDetails(OperationSetMultiUserChat.class);

        return ((mucContactDetails != null) && (mucContactDetails.size() > 0));
    }

    /**
     * Sets the images used for the call button. If a contact has multiple
     * numbers then a different set of images are used to make this obvious
     * @param phoneNumberCount the number of phone numbers that this contact has
     */
    private void setCallButtonImages(int phoneNumberCount)
    {
        BufferedImageFuture callButtonImage;
        BufferedImageFuture callButtonImagePressed;
        BufferedImageFuture callButtonImageRollover;

        if (phoneNumberCount > 1)
        {
            callButtonImage = sImageLoaderService.getImage(
                                  ImageLoaderService.CALL_BUTTON_SMALL_MULTI);
            callButtonImagePressed = sImageLoaderService.getImage(
                                  ImageLoaderService.CALL_BUTTON_SMALL_MULTI_PRESSED);
            callButtonImageRollover = sImageLoaderService.getImage(
                                  ImageLoaderService.CALL_BUTTON_SMALL_MULTI_ROLLOVER);
        }
        else
        {
            callButtonImage = sImageLoaderService.getImage(
                                        ImageLoaderService.CALL_BUTTON_SMALL);
            callButtonImagePressed = sImageLoaderService.getImage(
                                        ImageLoaderService.CALL_BUTTON_SMALL_PRESSED);
            callButtonImageRollover = sImageLoaderService.getImage(
                                        ImageLoaderService.CALL_BUTTON_SMALL_ROLLOVER);
        }

        mCallButton.setIconImage(callButtonImage);
        mCallButton.setPressedIcon(callButtonImagePressed);
        mCallButton.setRolloverIcon(callButtonImageRollover);
        mCallButton.setDisabledImage(sImageLoaderService.getImage(
                                       ImageLoaderService.CALL_BUTTON_SMALL_DISABLED));

        mRolloverCallButton.setIconImage(callButtonImage);
        mRolloverCallButton.setPressedIcon(callButtonImagePressed);
        mRolloverCallButton.setRolloverIcon(callButtonImageRollover);
        mRolloverCallButton.setDisabledImage(sImageLoaderService.getImage(
                                       ImageLoaderService.CALL_BUTTON_SMALL_DISABLED));
    }

    @Override
    protected int getButtonYBounds()
    {
        return mActionButtonConstraints.insets.top;
    }

    @Override
    protected JLabel getGroupNameLabel()
    {
        return mAvatarLabel;
    }

    @Override
    protected JLabel getGroupDetailsLabel()
    {
        return mDisplayDetailsLabel;
    }

    @Override
    protected JLabel getGroupBlankLabel()
    {
        return mNameLabel;
    }

    @Override
    protected GridBagConstraints getGroupBlankLabelConstraints()
    {
        // Set the gridwidth as this may have changed if the previous cell was
        // a contact node
        mNameLabelConstraints.gridwidth = 1;
        return mNameLabelConstraints;
    }

    @Override
    protected void setBorder()
    {
         /*
         * !!! When changing border values we should make sure that we
         * recalculate the X and Y coordinates of the buttons added in
         * initButtonsPanel and initContactActionButtons functions. If not
         * correctly calculated problems may occur when clicking buttons!
         */
        if (mTreeNode instanceof ContactNode &&
            mIsSelected &&
            !(((ContactNode) mTreeNode).getContactDescriptor() instanceof
                    ShowMoreContact))
        {
            setBorder(BorderFactory.createCompoundBorder(
                             BorderFactory.createMatteBorder(1,
                                                             0,
                                                             1,
                                                             0,
                                                             Color.WHITE),
                             BorderFactory.createEmptyBorder(mTopBorder,
                                                             mLeftBorder,
                                                             mBottomBorder,
                                                             mRightBorder)));
        }
        else // GroupNode || ShowMoreContact
        {
            super.setBorder();
        }
    }
}
