/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.CreateConferenceMenu.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The right button menu for external contact sources.
 * @see ExternalContactSource
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SourceContactRightButtonMenu
    extends AbstractContactRightButtonMenu
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger.
     */
    private static final Logger sLog
        = Logger.getLogger(SourceContactRightButtonMenu.class);

    /**
     * The UI contact.
     */
    private final UIContact uiContact;

    /**
     * The source contact.
     */
    private final SourceContact sourceContact;

    /**
     * Call contact menu.
     */
    private SIPCommMenu callContactMenu;

    /**
     * Chat contact menu.
     */
    private JMenuItem chatContactMenu;

    /**
     * View history menu.
     */
    private JMenuItem viewHistoryMenu;

    /**
     * Add contact component.
     */
    private Component addContactComponent;

    /**
     * The resource management service
     */
    private static final ResourceManagementService resources =
                                                    GuiActivator.getResources();

    /**
     * Phone number utils, used for format phone numbers for display in the UI.
     */
    private static final PhoneNumberUtilsService phoneNumberUtils =
                                             GuiActivator.getPhoneNumberUtils();

    /**
     * The multi user chat operation set (will be null if this isn't a group
     * chat source contact)
     */
    private OperationSetMultiUserChat opSetMuc;

    /**
     * The chat room identifier (will be null if this isn't a group
     * chat source contact)
     */
    private String chatRoomId;

    /**
     * Creates an instance of <tt>SourceContactRightButtonMenu</tt> by
     * specifying the <tt>SourceContact</tt>, for which this menu is created.
     * @param uiContact the <tt>SourceUIContact</tt>, for which this menu is
     * created
     */
    public SourceContactRightButtonMenu(SourceUIContact uiContact)
    {
        this.uiContact = uiContact;
        this.sourceContact = uiContact.getSourceContact();

        this.initItems();
    }

    /**
     * Initializes menu items.
     */
    private void initItems()
    {
        // If this source contact is anonymous then do not add any buttons to the menu.
        if (sourceContact.getData(SourceContact.DATA_IS_ANONYMOUS) != null)
        {
            setEnabled(false);
            return;
        }

        @SuppressWarnings("unchecked")
        List<MetaContact> metaContacts = (List<MetaContact>)
                        sourceContact.getData(SourceContact.DATA_META_CONTACTS);

        // Check whether this is a group chat contact
        List<ContactDetail> mucContactDetails = sourceContact.
                             getContactDetails(OperationSetMultiUserChat.class);
        if ((mucContactDetails != null) && (mucContactDetails.size() > 0))
        {
            initChatRoomMenu(mucContactDetails.get(0));
        }
        else if (metaContacts == null || metaContacts.isEmpty())
        {
            // No MetaContact associated with this source contact.
            ContactDetail cDetail = sourceContact
                   .getPreferredContactDetail(OperationSetBasicTelephony.class);
            String phoneNumber = null;

            if (cDetail == null)
            {
                String emphNumber = sourceContact.getEmphasizedNumber();
                if (emphNumber != null &&
                    phoneNumberUtils.isValidNumber(emphNumber))
                {
                   phoneNumber = emphNumber;
                }
            }

            // Call menu.
            if (cDetail != null || phoneNumber != null)
                add(initCallMenu(phoneNumber));

            if (sourceContact.canBeMessaged())
            {
                add(initChatMenu());
                add(initViewHistoryMenu(sourceContact.getEmphasizedNumber()));
            }

            // Add the send email menu, but only if the contact has an email
            // address, because it doesn't make sense to display it disabled
            // for source contacts that don't have an email address, as they
            // can never be modified to add one.
            initEmailMenu(false);

            // If the conference service is enabled, we also want to add the
            // conference menu, which includes a separator, just before the add
            // contact item.
            initConferenceMenu(null);

            // Disable the add contact item from the right button menu if this
            // source contact is also a local contact
            Object data = sourceContact.getData(
                                        SourceContact.DATA_DISABLE_ADD_CONTACT);
            boolean dataDisableAdd = data != null &&
                                     Boolean.parseBoolean(data.toString());
            Object contactType = sourceContact.getData(SourceContact.DATA_TYPE);

            // Add contact should be disabled for IM history entries if the IM
            // provider is null, e.g. if the user has signed out of chat.
            boolean isAddContactDisabled =
                (AccountUtils.getImProvider() == null &&
                    SourceContact.Type.IM_MESSAGE_HISTORY.equals(contactType)) ||
                dataDisableAdd;

            // Only create the menu if the add contact functionality is enabled.
            if (!isAddContactDisabled)
            {
                addContactComponent
                    = TreeContactList.createAddContactMenu(sourceContact);
            }

            if (addContactComponent != null)
            {
                addSeparator();
                add(addContactComponent);
            }

        }
        else if (metaContacts.size() == 1)
        {
            // There's a single contact associated with this source contact.
            // Show the popup menu for that instead:
            MetaContact contact = metaContacts.get(0);
            JPopupMenu subMenu = new MetaContactRightButtonMenu(contact);

            // You can't just add a menu to another menu, you have to add it
            // component by component.  Do that here.
            for (Component comp : subMenu.getComponents())
            {
                add(comp);
            }
        }
        else
        {
            // There are multiple contacts - show a pop-up to allow the user
            // to choose which one to interact with
            for (MetaContact contact : metaContacts)
            {
                // Disable HTML to prevent HTML injection
                JMenu menu = new JMenu();
                menu.putClientProperty("html.disable", Boolean.TRUE);
                menu.setText(contact.getDisplayName());
                JPopupMenu subMenu = new MetaContactRightButtonMenu(contact);

                for (Component comp : subMenu.getComponents())
                {
                    menu.add(comp);
                }

                add(menu);
            }
        }
    }

    @Override
    protected Set<String> getEmailAddresses()
    {
        Set<String> emailAddresses = new HashSet<>();

        // Get all contact details for the source contact then search through
        // them for any email details.
        List<ContactDetail> details = sourceContact.getContactDetails(
                                          OperationSetPersistentPresence.class);

        for (ContactDetail detail : details)
        {
            String address = detail.getDetail();
            if (ContactDetail.Category.Email.equals(detail.getCategory()))
            {
                emailAddresses.add(address);
            }
        }

        return emailAddresses;
    }

    private void initChatRoomMenu(ContactDetail contactDetail)
    {
        chatRoomId = contactDetail.getDetail();
        ProtocolProviderService imProvider = AccountUtils.getImProvider();

        ChatRoom existingChatRoom = null;

        if (imProvider != null &&
            imProvider.isRegistered() &&
            ConfigurationUtils.isMultiUserChatEnabled())
        {
            // Have a registered IM provider
            opSetMuc = imProvider.getOperationSet(
                                               OperationSetMultiUserChat.class);
            try
            {
                existingChatRoom = opSetMuc.findRoom(chatRoomId);
            }
            catch (OperationFailedException | OperationNotSupportedException ex)
            {
                sLog.warn("Failed to find chat room ", ex);
            }
        }

        final ChatRoom chatRoom = existingChatRoom;

        Object isClosedData = sourceContact.getData("isClosed");
        final boolean chatRoomClosed = (isClosedData == null) ?
                                                 false : (Boolean) isClosedData;

        // Whether to disable chat menu items.  We disable them if the chat
        // room is not closed (i.e. the user has not left the chat room) and
        // either we have no chat room object or we have not yet joined the
        // room).
        boolean disableItems = (!chatRoomClosed &&
                                (chatRoom == null || !chatRoom.isJoined()));

        // The start chat menu item
        JMenuItem startChatItem = new ResMenuItem("service.gui.SEND_GROUP_CHAT_TO");
        add(startChatItem);

        startChatItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent evt)
            {
                sLog.user("Right-click, 'send group chat' option selected");

                // Send an analytic of us re-opening an existing group chat,
                // with the number of active group chats as a parameter.
                List<AnalyticsParameter> params = new ArrayList<>();

                params.add(new AnalyticsParameterSimple(
                    AnalyticsParameter.NAME_COUNT_GROUP_IMS,
                    String.valueOf(opSetMuc.getActiveChatRooms())));

                GuiActivator.getAnalyticsService().onEvent(
                    AnalyticsEventType.REOPEN_GROUP_IM, params);

                GuiActivator.getUIService().startGroupChat(
                                chatRoomId,
                                chatRoomClosed,
                                sourceContact.getDisplayName());
            }
        });

        // If the conference service is enabled, we also add conference options
        // between the chat menu items.
        initConferenceMenu(chatRoom);

        addSeparator();

        if (disableItems)
        {
            startChatItem.setEnabled(false);
        }

        ConferenceChatManager conferenceChatManager =
            GuiActivator.getUIService().getConferenceChatManager();
        ChatRoomList chatRoomList = conferenceChatManager.getChatRoomList();
        ChatRoomWrapper chatRoomWrapper =
            chatRoomList.findChatRoomWrapperFromChatRoom(chatRoom);

        if ((chatRoomWrapper == null) && (imProvider != null))
        {
            // We can't find the chat room on the server.  Either we're
            // offline or the chat has been deleted from the server.  In
            // either case, we'll need to make a chat room wrapper with the
            // information we do have so that we can still look up the chat
            // history in our database.
            chatRoomWrapper =
                new ChatRoomWrapper(new ChatRoomProviderWrapper(imProvider),
                                    chatRoomId,
                                    chatRoomId,
                                    sourceContact.getDisplayName());
        }

        if (chatRoomWrapper != null)
        {
            add(initViewHistoryMenu(chatRoomWrapper));
        }
    }

    /**
     * Adds a CreateConferenceMenu to this right button menu (followed by a
     * separator).
     *
     * @param container The CreateConferenceMenuContainer that should be
     * associated with this CreateConferenceMenu
     */
    private void initConferenceMenu(CreateConferenceMenuContainer container)
    {
        SIPCommMenu newConferenceItem = new SIPCommMenu(
            resources.getI18NString("service.gui.conf.CONFERENCE_INVITE"));
        if (CreateConferenceMenu.isConferenceInviteByImEnabled())
        {
            CreateConferenceMenu createConferenceMenu =
                new CreateConferenceMenu(container);
            newConferenceItem.add(
                createConferenceMenu.getSendConferenceInviteJMenuItem());
            newConferenceItem.add(
                createConferenceMenu.getCreateConferenceJMenuItem());
            addSeparator();
            add(newConferenceItem);
        }
    }

    /**
     * Initializes the call menu.
     * @param defaultPhoneNumber the default phone number.  If this is null,
     * all numbers in the contact's details will be added to the call menu.
     * @return the call menu
     */
    private Component initCallMenu(String defaultPhoneNumber)
    {
        callContactMenu = new SIPCommMenu(
            resources.getI18NString("service.gui.CALL"));
        callContactMenu.setEnabled(false);

        if (!ConfigurationUtils.isMenuIconsDisabled())
        {
            GuiActivator.getImageLoaderService()
            .getImage(ImageLoaderService.CALL_16x16_ICON)
            .getImageIcon()
            .addToMenuItem(callContactMenu);
        }

        if (defaultPhoneNumber != null)
        {
            String displayPhoneNumber =
                phoneNumberUtils.formatNumberForDisplay(defaultPhoneNumber);

            addCallMenuItem(defaultPhoneNumber,
                            displayPhoneNumber + " (" +
                                resources.getI18NString(
                                      "impl.protocol.sip.commportal.SMS") + ")",
                            true);
        }
        else
        {
            Iterator<UIContactDetail> details = uiContact
                .getContactDetailsForOperationSet(OperationSetBasicTelephony.class)
                    .iterator();

            while (details.hasNext())
            {
                UIContactDetail detail = details.next();
                String number = detail.getDisplayName();

                if (number == null || number.length() == 0)
                    continue;

                String text = phoneNumberUtils.formatNumberForDisplay(number);

                // Display the type of the number if we know it
                Iterator<String> types = detail.getLabels();
                if (types != null && types.hasNext())
                {
                    // The type indicates what sort of number this detail is. We
                    // only display one, so are only interested in the first
                    String type = types.next();

                    // Using string concatenation is nasty, but matches what is
                    // done in ContactPhoneNumberUtil.getAdditionalNumbers
                    text += " (" + type + ")";
                }

                // Can always be enabled since the contact detail supports
                // telephony
                boolean isEnabled = true;
                addCallMenuItem(number, text, isEnabled);
            }
        }

        return callContactMenu;
    }

    /**
     * Adds a single menu item to the call menu
     *
     * @param number the phone number for this item
     * @param text the text to display for this item
     * @param isEnabled whether this item is clickable
     */
    private void addCallMenuItem(final String number, String text, boolean isEnabled)
    {
        // add all the contacts that support telephony to the call menu
        JMenuItem callContactItem = new JMenuItem();
        ScaleUtils.scaleFontAsDefault(callContactItem);
        callContactItem.setText(text);
        callContactItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                List<ProtocolProviderService> providers
                        = CallManager.getAllTelephonyProviders(number);

                int providersCount = providers.size();

                ContactSourceService contactSourceService =
                    sourceContact.getContactSource();

                // If this is a message history or call history entry, format
                // the number to E.164 before dialling.
                Reformatting reformattingNeeded = Reformatting.NOT_NEEDED;
                if (contactSourceService.getType() == ContactSourceService.MESSAGE_HISTORY_TYPE ||
                    contactSourceService.getType() == ContactSourceService.CALL_HISTORY_TYPE)
                {
                    reformattingNeeded = Reformatting.NEEDED;
                }

                if (providers == null || providersCount == 0)
                {
                    new ErrorDialog(
                        resources.getI18NString("service.gui.CALL_FAILED"),
                        resources.getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                        .showDialog();
                    return;
                }
                else if (providersCount > 1)
                {
                    new ChooseCallAccountDialog(
                        number,
                        sourceContact.getDisplayName(),
                        OperationSetBasicTelephony.class,
                        providers,
                        reformattingNeeded).setVisible(true);
                }
                else // providersCount == 1
                {
                    CallManager.createCall(providers.get(0),
                                           number,
                                           sourceContact.getDisplayName(),
                                           reformattingNeeded);
                }
            }
        });
        callContactItem.setEnabled(isEnabled);
        callContactMenu.add(callContactItem);
        callContactMenu.setEnabled(true);
    }

    /**
     * Initializes the chat menu.
     * @return the chat menu
     */
    private Component initChatMenu()
    {
        chatContactMenu = new ResMenuItem("service.gui.SEND_MESSAGE");

        if (!ConfigurationUtils.isMenuIconsDisabled())
        {
            GuiActivator.getImageLoaderService()
            .getImage(ImageLoaderService.SEND_MESSAGE_16x16_ICON)
            .getImageIcon()
            .addToMenuItem(chatContactMenu);
        }

        chatContactMenu.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                GuiActivator.getUIService().startChat(
                             new String[]{sourceContact.getEmphasizedNumber()});
            }
        });

        return chatContactMenu;
    }

    /**
     * Initializes the view history menu.
     *
     * @param descriptor the history window descriptor
     *
     * @return the view history menu
     */
    private Component initViewHistoryMenu(final Object descriptor)
    {
        viewHistoryMenu = new ResMenuItem("service.gui.VIEW_HISTORY");

        viewHistoryMenu.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                HistoryWindowManager historyWindowManager
                    = GuiActivator.getUIService().getHistoryWindowManager();

                historyWindowManager.displayHistoryWindowForChatDescriptor(descriptor, sLog);
            }
        });

        return viewHistoryMenu;
    }

    /**
     * Reloads icons for menu items.
     */
    public void loadSkin()
    {
        if (ConfigurationUtils.isMenuIconsDisabled())
        {
            return;
        }

        GuiActivator.getImageLoaderService()
        .getImage(ImageLoaderService.CALL_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(callContactMenu);

        GuiActivator.getImageLoaderService()
        .getImage(ImageLoaderService.SEND_MESSAGE_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(chatContactMenu);

        if(addContactComponent instanceof JMenuItem)
        {
            GuiActivator.getImageLoaderService()
            .getImage(ImageLoaderService.ADD_CONTACT_16x16_ICON)
            .getImageIcon()
            .addToMenuItem(((JMenuItem) addContactComponent));
        }
    }
}
