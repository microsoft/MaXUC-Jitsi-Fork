/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.commons.lang3.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.insights.enums.InsightsResultCode;
import net.java.sip.communicator.service.insights.parameters.CommonParameterInfo;
import net.java.sip.communicator.service.managecontact.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.*;
import net.java.sip.communicator.service.protocol.PersonalContactDetails.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The ContactRightButtonMenu is the menu, opened when user clicks with the
 * user could add a subcontact, remove a contact, send message, etc.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class MetaContactRightButtonMenu
    extends AbstractContactRightButtonMenu
    implements  ActionListener,
                PluginComponentListener,
                ContactListListener,
                Skinnable
{
    /**
     * An eclipse generated serial version unique ID
     */
    private static final long serialVersionUID = 3033031652970285857L;

    /**
     * The logger of this class.
     */
    private static final Logger sLog
        = Logger.getLogger(MetaContactRightButtonMenu.class);

    /**
     * The contact logger.
     */
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * The resource management service.
     */
    private static final ResourceManagementService resources =
                                                    GuiActivator.getResources();

    /**
     * The add contact service.
     */
    private static final ManageContactService addContactService =
                                            GuiActivator.getManageContactService();

    /**
     * The string shown over menu items indicating that an operation should be
     * done for all contained contacts.
     */
    private static final String allContactsString
        = resources.getI18NString("service.gui.ALL_CONTACTS");

    /**
     * String for call menu items.
     */
    private static final String callString
        = resources.getI18NString("service.gui.CALL");

    /**
     * String for meeting room menu items.
     */
    private static final String meetingString
        = resources.getI18NString("service.gui.conf.CONFERENCE_INVITE");

    /**
     * The menu responsible for removing a contact.
     */
    private final SIPCommMenu removeContactMenu = new SIPCommMenu();

    /**
     * The menu responsible for calling a contact.
     */
    private final SIPCommMenu callContactMenu = new SIPCommMenu(callString);

    /**
     * The menu responsible for creating or adding participants to a meeting.
     */
    private final SIPCommMenu newMeetingMenu = new SIPCommMenu(meetingString);

    /**
     * The menu responsible for adding a contact.
     */
    private final JMenuItem addContactItem =
        new ResMenuItem("service.gui.ADD_CONTACT");

    /**
     * The menu item responsible for calling a contact.
     */
    private final JMenuItem callItem = new JMenuItem(callString);

    /**
     * The video call menu item.
     */
    private final JMenuItem videoCallItem = new ResMenuItem("service.gui.VIDEO_CALL");

    /**
     * The menu responsible for calling a contact with video.
     */
    private final SIPCommMenu videoCallMenu = new SIPCommMenu(
        resources.getI18NString(
            "service.gui.VIDEO_CALL"));

    /**
     * The send message menu item.
     */
    protected final JMenuItem sendMessageItem = new JMenuItem();

    /**
     * The remove from group chat menu item.
     */
    protected final JMenuItem removeFromGroupChatItem = new JMenuItem();

    /**
     * The send file menu item.
     */
    private final JMenuItem sendFileItem = new ResMenuItem("service.gui.SEND_FILE");

    /**
     * The send SMS menu item.
     */
    protected final JMenuItem sendSmsItem = new ResMenuItem("service.gui.SEND_SMS");

    /**
     * The Group Contact menu item. Behaves slightly differently depending on
     * the contact being shown:
     * 0. IM not enabled - not shown.
     * 1. Contact is an IM contact - show an "add to group contact" menu
     * 2. Contact is a group contact - not present
     * 3. Contact is none of the above - show "add to group" menu but disabled
     */
    private JMenuItem groupContactMenuItem;

    /**
     * The rename contact menu item.
     */
    private final JMenuItem renameContactItem
        = new ResMenuItem("service.gui.RENAME_CONTACT");

    /**
     * The view history menu item.
     */
    private final JMenuItem viewHistoryItem
        = new ResMenuItem("service.gui.VIEW_HISTORY");

    /**
     * Multi protocol contact authorization request menu.
     */
    private final SIPCommMenu multiContactRequestAuthMenu
        = new SIPCommMenu(resources
            .getI18NString("service.gui.RE_REQUEST_AUTHORIZATION"));

    /**
     * Authorization request menu item.
     */
    private final JMenuItem requestAuthMenuItem
        = new ResMenuItem("service.gui.RE_REQUEST_AUTHORIZATION");

    /**
     * The add to / remove from favorites menu item
     */
    private final JMenuItem favoritesMenuItem = new JMenuItem();

    /**
     * The menu item to launch a CRM search for a contact. Not final, as this
     * need to be re-initialized when the menu is created.
     */
    private JMenuItem crmItem;

    /**
     * The <tt>MetaContact</tt> over which the right button was pressed.
     */
    protected final MetaContact metaContact;

    /**
     * The ChatRoomMember that represents the <tt>MetaContact</tt> over which
     * the right button was pressed, if this menu is in a group chat.
     */
    protected final ChatRoomMember mChatRoomMember;

    /**
     * The prefix for the move menu.
     */
    private static final String moveToPrefix = "moveTo:";

    /**
     * The prefix for remove contact menu.
     */
    private static final String removeContactPrefix = "removeContact:";

    /**
     * The prefix for remove protocol contact menu.
     */
    private static final String moveSubcontactPrefix = "moveSubcontact:";

    /**
     * The prefix for call contact menu.
     */
    private static final String callContactPrefix = "callContact:";

    /**
     * The prefix for call phone menu.
     */
    private static final String callPhonePrefix = "callPhone:";

    /**
     * The prefix for video call contact menu.
     */
    private static final String videoCallPrefix = "videoCall:";

    /**
     * The prefix for full screen desktop sharing menu.
     */
    private static final String requestAuthPrefix = "requestAuth:";

    /**
     * The image loader service
     */
    private static final ImageLoaderService imageLoaderService =
        GuiActivator.getImageLoaderService();

    /**
     * The contact to move when the move menu has been chosen.
     */
    private Contact contactToMove;

    /**
     * Indicates if all contacts should be moved when the move to menu is
     * pressed.
     */
    private boolean moveAllContacts = false;

    /**
     * The move dialog.
     */
    private MoveSubcontactMessageDialog moveDialog;

    /**
     * The main window.
     */
    private final AbstractMainFrame mainFrame;

    /**
     * The contact list component.
     */
    private final TreeContactList contactList;

    /**
     * The first unsubscribed contact we found.
     */
    private Contact firstUnsubscribedContact = null;

    /**
     * The phone util we use to check whether to enable/disable buttons.
     */
    private ContactPhoneUtil contactPhoneUtil;

    /**
     * Creates an instance of ContactRightButtonMenu.
     *
     * @param contactItem The MetaContact for which the menu is opened
     */
    public MetaContactRightButtonMenu(MetaContact contactItem)
    {
        this(contactItem, null);
    }

    /**
     * Creates an instance of ContactRightButtonMenu.
     *
     * @param contactItem The MetaContact for which the menu is opened
     * @param chatRoomMember The ChatRoomMember that represents the
     * MetaContact over which the right button was pressed, if this menu is in
     * a group chat.
     */
    public MetaContactRightButtonMenu(MetaContact contactItem,
                                      ChatRoomMember chatRoomMember)
    {
        mainFrame = GuiActivator.getUIService().getMainFrame();
        contactList = mainFrame.getContactListPanel().getContactList();
        metaContact = contactItem;
        contactPhoneUtil = ContactPhoneUtil.getPhoneUtil(metaContact);
        mChatRoomMember = chatRoomMember;

        setLocation(getLocation());

        init();
        initMnemonics();
        loadSkin();
    }

    /**
     * Initializes the menu, by adding all containing menu items.
     */
    protected void init()
    {
        initCallMenus();
        initSendMessageItem();
        // Always display an email menu for a MetaContact, even if it doesn't
        // have any email addresses, as it looks more consistent to show a
        // greyed-out menu item than for it to be missing for some contacts.
        initEmailMenu(true);
        // Always ask the meeting menu to add a separator as we will always add
        // at least the call menu above it (the call menu is present but greyed
        // out if the contact cannot be called).
        initMeetingMenu(true);
        // View and edit contact plugins will always be present for metacontacts
        // in the contacts list, so add separator before them:
        addSeparator();
        initRemoveMenu();
        initFavoritesMenuItem();
        initSendFileItem();
        initHistoryItem();
        initCrmItem();
        // Always ask the add contact menu to add a separator as we will always
        // add at least the remove and favourites menus above here.
        initAddContactItem(true);
        initRequestAuthMenu();
        initPluginComponents();
    }

    /**
     * Add the call and video call menu items, and sets their text/enabled-ness
     * and visibility appropriately
     */
    protected void initCallMenus()
    {
        if (metaContact != null)
        {
            Iterator<Contact> contacts = metaContact.getContacts();

            while (contacts.hasNext())
            {
                Contact contact = contacts.next();
                addContactToCallMenu(contact);
                addPhones(contact);
                addVideoPhones(contact);
            }

            initCallContactMenu();
            initVideoCallMenu();
        }
    }

    @Override
    protected Set<String> getEmailAddresses()
    {
        Set<String> emailAddresses = new HashSet<>();

        Iterator<Contact> contacts = metaContact.getContacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();
            OperationSetServerStoredContactInfo opSet =
                contact.getProtocolProvider().getOperationSet(
                                     OperationSetServerStoredContactInfo.class);

            if (opSet != null)
            {
                String email1 = getDetailStringFromDetail(
                                     EmailAddress1Detail.class, contact, opSet);
                if (StringUtils.isNotBlank(email1))
                {
                    emailAddresses.add(email1);
                }

                String email2 = getDetailStringFromDetail(
                                     EmailAddress2Detail.class, contact, opSet);
                if (StringUtils.isNotBlank(email2))
                {
                    emailAddresses.add(email2);
                }
            }
        }

        return emailAddresses;
    }

    /**
     * Returns the string representation of the contact detail of the given
     * type for the given contact and operation set, or null if none exists.
     *
     * @param detailType the type of contact detail
     * @param contact the contact
     * @param opSet the operation set
     *
     * @return the string representation of the contact detail
     */
    private String getDetailStringFromDetail(
                                      Class<? extends GenericDetail> detailType,
                                      Contact contact,
                                      OperationSetServerStoredContactInfo opSet)
    {
        String detailString = null;

        Iterator<GenericDetail> details = opSet.getDetails(contact, detailType);
        if (details != null && details.hasNext())
        {
            Object detailValue = details.next().getDetailValue();
            if (detailValue != null)
            {
                detailString = detailValue.toString();
            }
        }

        return detailString;
    }

    /**
     * Checks if a contact has associated phones and adds the contact to the
     * call and video call menus appropriately.
     */
    private void addContactToCallMenu(Contact contact)
    {
        ProtocolProviderService protocolProvider = contact.getProtocolProvider();
        OperationSetPersistentPresence presence =
            protocolProvider.getOperationSet(OperationSetPersistentPresence.class);

        String contactAddress = presence.isAddressDisplayable() ?
            contact.getAddress() :
            contact.getDisplayName();

        // add all the contacts that support telephony to the call menu
        if (metaContact.getContactCount() > 1 ||
            contactPhoneUtil.getPhones(contact).size() > 0)
        {
            ImageIconFuture protocolIcon = createContactStatusImage(contact).getImageIcon();

            if (contactPhoneUtil.isCallEnabled(contact))
            {
                addCallMenuContact(contact, protocolIcon);
            }

            if (contactPhoneUtil.isVideoCallEnabled(contact))
            {
                videoCallMenu.add(
                    new ContactMenuItem(contact,
                                        contactAddress,
                                        videoCallPrefix,
                                        protocolIcon));
            }
        }
    }

    /**
     * Adds contact resources to call menu.
     *
     * @param contact the <tt>Contact</tt>, which resources to add
     * @param protocolIcon the protocol icon
     */
    private void addCallMenuContact(Contact contact, ImageIconFuture protocolIcon)
    {
        if (!contact.supportResources())
            return;

        Collection<ContactResource> resources = contact.getResources();

        if (resources == null)
            return;

        String contactAddress = contact.getAddress();

        if (contact.getResources().size() > 1)
        {
            if (callContactMenu.getItemCount() > 0)
            {
                callContactMenu.addSeparator();
            }

            callContactMenu.add(new ContactMenuItem(contact,
                                                    null,
                                                    contactAddress,
                                                    callContactPrefix,
                                                    protocolIcon,
                                                    true));
        }

        Iterator<ContactResource> resourceIter
            = contact.getResources().iterator();

        while (resourceIter.hasNext())
        {
            ContactResource resource = resourceIter.next();

            String resourceName;
            boolean isBold = false;
            ImageIconFuture resourceIcon;
            if (contact.getResources().size() > 1)
            {
                resourceName = resource.getResourceName();
                resourceIcon
                    = imageLoaderService.getIndexedProtocolIcon(
                        resource.getPresenceStatus().getStatusIcon(),
                        contact.getProtocolProvider());
            }
            else
            {
                resourceName = contact.getAddress()
                                + " " + resource.getResourceName();
                resourceIcon
                    = imageLoaderService.getIndexedProtocolIcon(
                        contact.getPresenceStatus().getStatusIcon(),
                        contact.getProtocolProvider());
                // If the resource is only one we don't want to pass it to
                // call operations.
                resource = null;
                isBold = true;
            }

            JMenuItem menuItem = new ContactMenuItem(
                                                contact,
                                                resource,
                                                resourceName,
                                                callContactPrefix,
                                                resourceIcon,
                                                isBold);

            if (contact.getResources().size() > 1)
                menuItem.setBorder(
                    BorderFactory.createEmptyBorder(0, 20, 0, 0));

            callContactMenu.add(menuItem);
        }
    }

    /**
     * Adds call menu phone entries for a contact.
     *
     * @param contact the contact whose phones are to be added to add to menu
     */
    private void addPhones(Contact contact)
    {
        List<ProtocolProviderService> providers =
            CallManager.getTelephonyProviders();

        for(String phone : contactPhoneUtil.getPhones(contact))
        {
            String p = phone.substring(0, phone.lastIndexOf("(") - 1);
            if(providers.size() > 0)
            {
                JMenuItem menuItem = createMenuItem( phone,
                    callPhonePrefix + p,
                    null);

                callContactMenu.add(menuItem);
            }
        }
    }

    /**
     * Adds video related call menu phone entries.
     *
     * @param contact the contact, which phones we're adding
     */
    private void addVideoPhones(Contact contact)
    {
        List<ProtocolProviderService> providers =
            CallManager.getTelephonyProviders();

        for(String vphone : contactPhoneUtil.getVideoPhones(contact))
        {
            String p = vphone.substring(0, vphone.lastIndexOf("(") - 1);
            if(providers.size() > 0)
            {
                JMenuItem vmenuItem = createMenuItem(vphone,
                    videoCallPrefix + p,
                    null);

                videoCallMenu.add(vmenuItem);
            }
        }
    }

    /**
     * Add the call item/menu, and set it's text/enabled-ness and
     * visibility appropriately. Called by initCallMenus()
     */
    private void initCallContactMenu()
    {
        if (callContactMenu.getItemCount() > 1)
        {
            add(callContactMenu);
            callContactMenu.setVisible(ConfigurationUtils.isCallingEnabled());
        }
        else
        {
            if(callContactMenu.getItemCount() > 0)
            {
                JMenuItem item = callContactMenu.getItem(0);
                callItem.setName(item.getName());
            }
            else
            {
                callItem.setName("call");
            }

            ScaleUtils.scaleFontAsDefault(callItem);
            callItem.addActionListener(this);
            add(callItem);
            callItem.setEnabled(contactPhoneUtil.isCallEnabled());
            callItem.setVisible(ConfigurationUtils.isCallingEnabled());
        }
    }

    /**
     * Add the video call menu item, and set it's text/enabled-ness and
     * visibility appropriately. Called by initCallMenus()
     */
    private void initVideoCallMenu()
    {
        if (!ConfigurationUtils.isVideoCallButtonDisabled())
        {
            if (videoCallMenu.getItemCount() > 1)
            {
                add(videoCallMenu);
            }
            else
            {
                if (videoCallMenu.getItemCount() > 0)
                {
                    JMenuItem item = videoCallMenu.getItem(0);
                    videoCallItem.setName(item.getName());
                }
                else
                {
                    videoCallItem.setName("videoCall");
                }

                ScaleUtils.scaleFontAsDefault(videoCallItem);
                videoCallItem.addActionListener(this);
                add(videoCallItem);
                videoCallItem.setEnabled(contactPhoneUtil.isVideoCallEnabled());
            }
        }
    }

    /**
     * Add the send chat menu item, and set its text/enabled-ness and
     * visibility appropriately
     */
    protected void initSendMessageItem()
    {
        if ((metaContact != null) && ConfigurationUtils.isImEnabled())
        {
            ScaleUtils.scaleFontAsDefault(sendMessageItem);
            sendMessageItem.setText(resources.getI18NString("service.gui.SEND_MESSAGE"));
            sendMessageItem.setName("sendMessage");
            sendMessageItem.addActionListener(this);
            add(sendMessageItem);
            sendMessageItem.setEnabled(metaContact.canBeMessaged());
        }
    }

    /**
     * Add the send file menu item, and set it's text/enabled-ness and
     * visibility appropriately
     */
    protected void initSendFileItem()
    {
        if(ConfigurationUtils.isImEnabled())
        {
            sLog.debug("IM is enabled - adding 'Send File' menu item");
            ScaleUtils.scaleFontAsDefault(sendFileItem);
            sendFileItem.setName("sendFile");
            sendFileItem.addActionListener(this);
            addSeparator();
            add(sendFileItem);
            sendFileItem.setEnabled(
                metaContact.getDefaultContact(OperationSetFileTransfer.class) != null &&
                metaContact.getIMContact() != null &&
                metaContact.getIMContact().getPresenceStatus().isOnline());
        }
    }

    /**
     * Add the remove from group chat item.
     * @param addSeparator if true, add a separator before the remove from group chat item.
     */
    protected void initRemoveFromGroupChatItem(boolean addSeparator)
    {
        // Note this if test needs to match the test that decides whether to add a separator
        // above the 'add as contact' menu item in ReducedMetaContactRightButtonMenu.init()
        if ((mChatRoomMember != null) && ConfigurationUtils.isMultiUserChatEnabled())
        {
            // No need to hash mChatRoomMember, as its toString()
            // method does that.
            sLog.debug("Add remove menu for " + mChatRoomMember);
            ScaleUtils.scaleFontAsDefault(removeFromGroupChatItem);
            removeFromGroupChatItem.setText(resources.getI18NString("service.gui.chat.REMOVE_USER"));
            removeFromGroupChatItem.setName("removeFromGroupChat");
            removeFromGroupChatItem.addActionListener(this);

            if (addSeparator)
            {
                addSeparator();
            }

            add(removeFromGroupChatItem);
            removeFromGroupChatItem.setEnabled(AccountUtils.isImProviderRegistered() && mChatRoomMember.getChatRoom().isJoined());
        }
    }

    /**
     * Add the meeting menu item, and set it's text/enabled-ness and
     * visibility appropriately
     * @param addSeparator if true, add a separator before the meeting menu
     * item.
     */
    protected void initMeetingMenu(boolean addSeparator)
    {
        if ((metaContact != null) &&
            CreateConferenceMenu.isConferenceInviteByImEnabled())
        {
            CreateConferenceMenu createMeetingMenu = new CreateConferenceMenu(metaContact);
            newMeetingMenu.add(createMeetingMenu.getSendConferenceInviteJMenuItem());
            newMeetingMenu.add(createMeetingMenu.getCreateConferenceJMenuItem());
            if (addSeparator)
            {
                addSeparator();
            }
            add(newMeetingMenu);
        }
    }

    /**
     * Add the delete contact item/menu, and set it's text/enabled-ness and
     * visibility appropriately
     */
    protected void initRemoveMenu()
    {
        Iterator<Contact> contacts = metaContact.getContacts();
        boolean undeletableContactPresent = false;

        while (contacts.hasNext())
        {
            Contact contact = contacts.next();
            ProtocolProviderService protocolProvider = contact.getProtocolProvider();
            OperationSetPersistentPresence presence =
                protocolProvider.getOperationSet(OperationSetPersistentPresence.class);

            boolean contactDeletable = presence.unsubscribeSupported(contact);
            String contactAddress = presence.isAddressDisplayable() ?
                contact.getAddress() :
                    contact.getDisplayName();

                BufferedImageFuture imageFuture =
                    createContactStatusImage(contact);
                ImageIconFuture protocolIcon =
                    imageFuture == null ? null : imageFuture.getImageIcon();

                ContactMenuItem deleteItem = new ContactMenuItem(contact,
                    contactAddress,
                    removeContactPrefix,
                    protocolIcon);

                removeContactMenu.add(deleteItem);

                deleteItem.setEnabled(contactDeletable);

                if (!contactDeletable && !undeletableContactPresent)
                {
                    undeletableContactPresent = true;
                }
        }

        ImageIconFuture deleteIcon =
            imageLoaderService.getImage(ImageLoaderService.DELETE_16x16_ICON).getImageIcon();

        String removeString = resources.getI18NString("service.gui.REMOVE_CONTACT");

        // There are multiple contacts - create a delete-all menu item and a
        // move all menu item
        if (metaContact.getContactCount() > 1)
        {
            JMenuItem deleteAllItem = createMenuItem(
                allContactsString,
                removeContactPrefix + "allContacts",
                deleteIcon);

            ScaleUtils.scaleFontAsDefault(removeContactMenu);
            removeContactMenu.setText(removeString);
            removeContactMenu.add(deleteAllItem, 0);
            removeContactMenu.insertSeparator(1);

            add(removeContactMenu);
            if (undeletableContactPresent)
            {
                deleteAllItem.setEnabled(false);
            }
        }
        else
        {
            // There is only one contact, so a submenu is unnecessary -
            // just add a single menu item.  It masquerades as an item
            // to delete all contacts as that way we don't have to specify
            // the contact's address.
            JMenuItem removeContactItem = createMenuItem(
                removeString,
                removeContactPrefix + "allContacts",
                deleteIcon);

            add(removeContactItem);
            if (undeletableContactPresent)
            {
                removeContactItem.setEnabled(false);
            }
        }
    }

    /**
     * Add the add to/remove from favourites menu item, and set it's text/
     * enabled-ness and
     * visibility appropriately
     */
    protected void initFavoritesMenuItem()
    {
        if (ConfigurationUtils.getContactFavoritesEnabled())
        {
            // Add the favorites menu item
            String detailName = MetaContact.CONTACT_FAVORITE_PROPERTY;
            String detail = metaContact.getDetail(detailName);
            boolean isFavorite = Boolean.parseBoolean(detail);
            String titleRes = isFavorite ?
                "service.gui.FAVORITES_REMOVE" : "service.gui.FAVORITES_ADD";
            ScaleUtils.scaleFontAsDefault(favoritesMenuItem);
            favoritesMenuItem.setText(resources.getI18NString(titleRes));
            favoritesMenuItem.setName("favorites");
            favoritesMenuItem.addActionListener(this);
            addSeparator();
            add(favoritesMenuItem);
        }
    }

    /**
     * Add the view chat history menu item, and set it's text/enabled-ness and
     * visibility appropriately
     */
    protected void initHistoryItem()
    {
        if(ConfigurationUtils.isImEnabled())
        {
            sLog.debug("IM is enabled - adding 'View Chat History' menu item");
            ScaleUtils.scaleFontAsDefault(viewHistoryItem);
            viewHistoryItem.setName("viewHistory");
            viewHistoryItem.addActionListener(this);
            add(viewHistoryItem);
            viewHistoryItem.setEnabled(metaContact.isImCapable());
        }
    }

    /**
     * Add the CRM menu item, and set it's text/enabled-ness and
     * visibility appropriately
     */
    protected void initCrmItem()
    {
        // Add CRM launch item, if configured
        UrlServiceTools ust = UrlServiceTools.getUrlServiceTools();
        if (ust.isServiceTypeEnabled(ServiceType.CRM))
        {
            sLog.debug("Create menu item for CRM");
            ScaleUtils.scaleFontAsDefault(crmItem);
            add(crmItem);
            crmItem.setName(ServiceType.CRM.getConfigName());
            crmItem.addActionListener(this);
        }
    }

    /**
     * Add the add contact menu item, if appropriate
     * @param addSeparator if true, add a separator before the add contact
     * item.
     */
    protected void initAddContactItem(boolean addSeparator)
    {
        // Only add this menu item if we don't already have a MetaContact and
        // if we do have a ChatRoomMember, as currently this menu item is only
        // used for non-buddy group chat participants.
        if (metaContact == null &&
            mChatRoomMember != null &&
            addContactService != null &&
            addContactService.supportsMultiFieldContacts())
        {
            // No need to hash mChatRoomMember, as its toString()
            // method does that.
            sLog.debug("Creating add contact item for " + mChatRoomMember);
            ScaleUtils.scaleFontAsDefault(addContactItem);
            addContactItem.setName("addContact");
            addContactItem.addActionListener(this);
            if (addSeparator)
            {
                addSeparator();
            }
            add(addContactItem);
        }
    }

    /**
     * Add the re-request authorization item/menu, and set it's text/
     * enabled-ness and visibility appropriately
     */
    protected void initRequestAuthMenu()
    {
        // Check whether unsubscribe is supported by the MetaContact's IM
        // contact.
        boolean unsubscribeSupported = true;
        Contact imContact = metaContact.getIMContact();

        if (imContact != null)
        {
            OperationSetPersistentPresence persPresOpsSet =
                imContact.getProtocolProvider().getOperationSet(
                    OperationSetPersistentPresence.class);

            if (persPresOpsSet != null)
            {
                unsubscribeSupported =
                    persPresOpsSet.unsubscribeSupported(imContact);
            }
        }

        // If the contact supports authorization but is not yet authorized,
        // add the 're-request authorization' menu item, unless unsubscribe
        // isn't supported for the contact, as those contacts will be
        // automatically added and authorized.
        if (unsubscribeSupported)
        {
            Iterator<Contact> contacts = metaContact.getContacts();

            // Add all unsubscribed contacts in the metacontact to the menu
            while (contacts.hasNext())
            {
                Contact contact = contacts.next();
                addToMultiContactRequestAuthMenu(contact);
            }

            Contact defaultContact = metaContact.getDefaultContact();
            int authRequestItemCount = multiContactRequestAuthMenu.getItemCount();

            // If we have more than one request to make.
            if (authRequestItemCount > 1)
            {
                ScaleUtils.scaleFontAsDefault(multiContactRequestAuthMenu);
                add(multiContactRequestAuthMenu);
            }
            // If we have more than one protocol contacts and only one need
            // authorization or we have only one contact that needs
            // authorization.
            else if (authRequestItemCount == 1
                || (metaContact.getContactCount() == 1
                && defaultContact.getProtocolProvider()
                .getOperationSet
                (OperationSetExtendedAuthorizations.class) != null)
                && !SubscriptionStatus.Subscribed
                .equals(defaultContact.getProtocolProvider()
                    .getOperationSet(
                        OperationSetExtendedAuthorizations.class)
                    .getSubscriptionStatus(defaultContact)))
            {
                // And require that the provider is registered
                if (defaultContact.getProtocolProvider().isRegistered())
                {
                    add(requestAuthMenuItem);
                    requestAuthMenuItem.setName("requestAuth");
                    requestAuthMenuItem.addActionListener(this);
                }
            }
        }
    }

    /**
     * Adds protocontact to re-request authorization menu
     */
    private void addToMultiContactRequestAuthMenu(Contact contact)
    {
        ProtocolProviderService protocolProvider = contact.getProtocolProvider();
        OperationSetPersistentPresence presence =
            protocolProvider.getOperationSet(OperationSetPersistentPresence.class);

        boolean contactDeletable = presence.unsubscribeSupported(contact);
        String contactAddress = presence.isAddressDisplayable() ?
            contact.getAddress() :
            contact.getDisplayName();

        // If the contact supports authorization but is not yet
        // authorized, add the 're-request authorization' menu item,
        // unless the contact is not deletable, as in that case,
        // authorization requests are handled automatically instead of
        // by the user.
        OperationSetExtendedAuthorizations authOpSet
            = protocolProvider.getOperationSet(
                OperationSetExtendedAuthorizations.class);

        if (authOpSet != null
            && authOpSet.getSubscriptionStatus(contact) != null
            && !authOpSet.getSubscriptionStatus(contact)
                .equals(SubscriptionStatus.Subscribed)
            && contactDeletable)
        {
            if (firstUnsubscribedContact == null)
            {
                firstUnsubscribedContact = contact;
            }

            ImageIconFuture protocolIcon = createContactStatusImage(contact).getImageIcon();

            multiContactRequestAuthMenu.add(
                new ContactMenuItem(contact,
                                    contactAddress,
                                    requestAuthPrefix,
                                    protocolIcon));
        }
    }

    /**
     * Initializes plug-in components for this container.
     */
    protected void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference<?>[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            sLog.error("Could not obtain plugin reference.", exc);
        }

        List<PluginComponent> components = new ArrayList<>();

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);

                component.setCurrentContact(metaContact);

                if (component.getComponent() != null)
                    components.add(component);
            }
        }

        // Sort the components by position to ensure they are always added to
        // the same place each time:
        Collections.sort(components, new Comparator<PluginComponent>()
        {
            @Override
            public int compare(PluginComponent o1, PluginComponent o2)
            {
                if (o1.getPositionIndex() != o2.getPositionIndex())
                    return o1.getPositionIndex() - o2.getPositionIndex();
                else
                    return o1.getName().compareTo(o2.getName());
            }
        });

        // Finally, add the components
        for (PluginComponent component : components)
        {
            if(component.getPositionIndex() != -1)
                add((Component)component.getComponent(),
                         component.getPositionIndex());
            else
                add((Component)component.getComponent());
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Initializes menu items mnemonics.
     */
    private void initMnemonics()
    {
        sendMessageItem.setMnemonic(
            resources
                .getI18nMnemonic("service.gui.SEND_MESSAGE"));

        char callMnemonic = resources
            .getI18nMnemonic("service.gui.CALL");

        if (callContactMenu.getItemCount() > 1)
        {
            callContactMenu.setMnemonic(callMnemonic);
        }
        else
        {
            callItem.setMnemonic(callMnemonic);
        }

        char videoCallMnemonic = resources
            .getI18nMnemonic("service.gui.VIDEO_CALL");

        if (videoCallMenu.getItemCount() > 1)
        {
            videoCallMenu.setMnemonic(videoCallMnemonic);
        }
        else
        {
            videoCallItem.setMnemonic(videoCallMnemonic);
        }

        removeContactMenu.setMnemonic(resources
            .getI18nMnemonic("service.gui.REMOVE_CONTACT"));
    }

    /**
     * Initializes the call menu items.
     *
     * @param displayName the display name of the menu item
     * @param name the name of the menu item, used to distinguish it in action
     * events
     * @param icon the icon of the protocol
     *
     * @return the created menu item
     */
    private JMenuItem createMenuItem(String displayName,
                                     String name,
                                     ImageIconFuture icon)
    {
        JMenuItem menuItem = new JMenuItem(displayName);
        ScaleUtils.scaleFontAsDefault(menuItem);

        if (!ConfigurationUtils.isMenuIconsDisabled())
        {
            icon.addToMenuItem(menuItem);
        }

        menuItem.setName(name);
        menuItem.addActionListener(this);

        return menuItem;
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Determines which menu item was
     * selected and performs the appropriate operations.
     * @param e the <tt>ActionEvent</tt>, which notified us of the action
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        Contact contact;
        String contactName = (metaContact == null) ?
            mChatRoomMember.getContactAddressAsString() : metaContact.getDisplayName();

        sLog.user("Right-click on " + logHasher(contactName) + ", " +
                          itemName + " option selected");

        if (itemName.equals(addContactItem.getName()))
        {
            // This item is only used for non-buddy members of group chats so
            // we only need to create the IM contact detail.
            ContactDetail addressDetail =
                new ContactDetail(contactName,
                                  ContactDetail.Category.InstantMessaging);

            String displayName = contactName.split("@")[0];

            sLog.debug("Opening add contact window for: " +
                logHasher(displayName) + ", " + logHasher(contactName));
            TreeContactList.showAddContactDialog(displayName, null, addressDetail);
        }
        else if (itemName.equalsIgnoreCase("sendMessage"))
        {
            GuiActivator.getUIService().getChatWindowManager()
                                                       .startChat(metaContact);
        }
        else if (itemName.equalsIgnoreCase("sendSms"))
        {
            Contact defaultSmsContact
                = metaContact.getDefaultContact(OperationSetSmsMessaging.class);

            GuiActivator.getUIService().getChatWindowManager()
                .startChat(metaContact, defaultSmsContact, true);
        }
        else if (itemName.equals(removeFromGroupChatItem.getName()))
        {
            attemptRemoveFromGroupChat();
        }
        else if (itemName.equals("call"))
        {
            call(false, null);

            ContactListFilter filter = contactList.getCurrentFilter();

            String location =
                filter.equals(contactList.getFavoritesFilter()) ? "Favorites" :
                filter.equals(contactList.getAllHistoryFilter()) ? "All History" :
                filter.equals(contactList.getCallHistoryFilter()) ? "History" :
                filter.equals(contactList.getSearchFilter()) ? "Search" :
                "Contact";
            GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.OUTBOUND_CALL,
                                                       "Calling from",
                                                       location);
        }
        else if (itemName.equals("videoCall"))
        {
            call(true, null);
        }
        else if (itemName.equals("sendFile"))
        {
            String title = resources
                                  .getI18NString("service.gui.SEND_FILE_TITLE");
            SipCommFileChooser scfc = GenericFileDialog.create(
                null,
                title,
                SipCommFileChooser.LOAD_FILE_OPERATION,
                ConfigurationUtils.getSendFileLastDir());
            File selectedFile = scfc.getFileFromDialog();
            if(selectedFile != null)
            {
                ConfigurationUtils.setSendFileLastDir(
                        selectedFile.getParent());

                // Obtain the corresponding chat panel.
                ChatPanel chatPanel
                    = GuiActivator.getUIService().
                        getChatWindowManager().getContactChat(metaContact, true);

                GuiActivator.getUIService().
                    getChatWindowManager().openChatAndAlertWindow(chatPanel, true);
            }
        }
        else if (itemName.equals("renameContact"))
        {
            RenameContactDialog dialog = new RenameContactDialog(
                    mainFrame, metaContact);

            dialog.setVisible(true);

            dialog.requestFocusInFiled();
        }
        else if (itemName.equals("viewHistory"))
        {
            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            historyWindowManager.displayHistoryWindowForChatDescriptor(metaContact, sLog);
        }
        else if (itemName.equals("requestAuth"))
        {
            // If we have more than one protocol contacts, but just one of them
            // needs authorization.
            if (firstUnsubscribedContact != null)
                contact = firstUnsubscribedContact;
            // If we have only one protocol contact and it needs authorization.
            else
                contact = metaContact.getDefaultContact();

            requestAuthorization(contact);
        }
        else if (itemName.startsWith(moveToPrefix))
        {
            MetaContactListManager.moveMetaContactToGroup(
                metaContact, itemName.substring(moveToPrefix.length()));
        }
        else if (itemName.startsWith(removeContactPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                MetaContactListManager.removeContact(
                    ((ContactMenuItem) menuItem).getContact());
            }
            else
            {
                MetaContactListManager.removeMetaContact(metaContact);
            }
        }
        else if(itemName.startsWith(moveSubcontactPrefix))
        {
            contactList.addContactListListener(this);
            contactList.setGroupClickConsumed(true);

            // FIXME: set the special cursor while moving a subcontact
            //guiContactList.setCursor(
            //        Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

            moveDialog = new MoveSubcontactMessageDialog(mainFrame, this);

            // Be sure we allow open/close groups in the contactlist if
            // user cancels the action
            moveDialog.addWindowListener(new WindowAdapter()
                {
                    @Override
                    public void windowClosed(WindowEvent e)
                    {
                        contactList.setGroupClickConsumed(false);
                    }
                });
            moveDialog.setVisible(true);

            if(menuItem instanceof ContactMenuItem)
            {
                contactToMove = ((ContactMenuItem) menuItem).getContact();
            }
            else
            {
                moveAllContacts = true;
            }
        }
        else if (itemName.startsWith(callContactPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                ContactMenuItem contactItem = (ContactMenuItem) menuItem;

                call(false,
                    contactItem.getContact(), contactItem.getContactResource());
            }
            else
                call(false,
                    itemName.substring(callContactPrefix.length()));
        }
        else if (itemName.startsWith(videoCallPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                ContactMenuItem contactItem = (ContactMenuItem) menuItem;

                call(true, contactItem.getContact(),
                     contactItem.getContactResource());
            }
            else
                call(true, itemName.substring(videoCallPrefix.length()));
        }
        else if (itemName.startsWith(requestAuthPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                contact = ((ContactMenuItem) menuItem).getContact();
            }
            else
                contact = getContactFromMetaContact(
                    itemName.substring(requestAuthPrefix.length()));

            requestAuthorization(contact);
        }
        else if (itemName.startsWith(callPhonePrefix))
        {
            String phone = itemName.substring(callPhonePrefix.length());

            call(false, phone);
        }
        else if (itemName.equals("favorites"))
        {
            String detailName = MetaContact.CONTACT_FAVORITE_PROPERTY;
            String detail = metaContact.getDetail(detailName);
            boolean isNowFavorite;

            metaContact.setDetail(MetaContact.CONTACT_FAVORITE_TIME_PROPERTY,
                                  String.valueOf(System.currentTimeMillis()));

            if (detail == null)
            {
                // No existing detail so wasn't a favorite, add a new detail
                // making it a favorite.  No need to hash MetaContact, as its
                // toString() method does that.
                contactLogger.info(metaContact, "Favorite status was null, now true");
                metaContact.setDetail(detailName, String.valueOf(true));
                isNowFavorite = true;
            }
            else
            {
                // There is a detail - set the new value to be the opposite of
                // the old value.  No need to hash MetaContact, as its
                // toString() method does that.
                isNowFavorite = !Boolean.parseBoolean(detail);
                contactLogger.info(metaContact,
                    "Favorite status was " + !isNowFavorite + ", now " +
                    isNowFavorite);
                metaContact.setDetail(detailName, String.valueOf(isNowFavorite));
            }

            GuiActivator.getAnalyticsService().onEvent(isNowFavorite ?
                                         AnalyticsEventType.FAVORITES_ADD :
                                         AnalyticsEventType.FAVORITES_REMOVE);
        }
        else if ((crmItem != null) && itemName.equals(crmItem.getName()))
        {
            ServiceType.CRM.launchSelectedCrmService(contactName,
                                                     metaContact.getPreferredPhoneNumber(),
                                                     false);
        }
    }

    /**
     *  Remove the metacontact this menu is associated with from the chat room,
     *  showing a confirmation dialog if necessary.
     */
    public void attemptRemoveFromGroupChat()
    {
        String contactName = (metaContact == null) ?
            mChatRoomMember.getContactAddressAsString() : metaContact.getDisplayName();
        // If the user has not requested to not be prompted to confirm
        // removal of contacts from group chats, display a dialog asking
        // them to confirm.
        boolean removeUser = ConfigurationUtils.getDontAskRemoveFromChat();
        if (!removeUser)
        {
            removeUser = confirmRemoveFromChat(contactName);
        }
        else
        {
            sLog.debug(
                "Not promping user - just removing contact from group chat: " +
                                                logHasher(contactName));
        }

        if (removeUser)
        {
            ChatRoom chatRoom = mChatRoomMember.getChatRoom();

            if (chatRoom != null)
            {
                try
                {
                    // Don't give a reason, as this will be displayed for
                    // downlevel clients but won't be internationalised.
                    // Also, we need to ban the participant, rather than
                    // just kick them out, to ensure that downlevel clients
                    // are unable to re-join the chat room. Banning does
                    // not prevent us from inviting people back into the
                    // room, as we change their affiliation back to owner
                    // when we do that.
                    chatRoom.banParticipant(mChatRoomMember, "");
                }
                catch (OperationFailedException ex)
                {
                    // No need to hash mChatRoomMember, as its toString()
                    // method does that.
                    sLog.warn("Failed to remove chat room member " +
                                mChatRoomMember + " from chat room " + chatRoom, ex);
                    displayRemoveChatUserError(contactName);
                    sendRemoveParticipantAnalytic(InsightsResultCode.XMPP_ERROR);
                }
            }
            else
            {
                sendRemoveParticipantAnalytic(InsightsResultCode.ROOM_JID_NOT_VALID);
                // No need to hash mChatRoomMember, as its toString()
                // method does that.
                sLog.warn("Unable to remove chat room member " +
                            mChatRoomMember + " from null chat room");
                displayRemoveChatUserError(contactName);
            }
        }
    }

    /**
     * Sends an analytic event for removing participant from group chat
     *
     * @param code Mapped value for parameter result
     */
    private void sendRemoveParticipantAnalytic(InsightsResultCode code)
    {
        GuiActivator.getInsightsService().logEvent(
                InsightsEventHint.IM_REMOVE_PARTICIPANT.name(),
                Map.of(
                        CommonParameterInfo.INSIGHTS_RESULT_CODE.name(),
                        code
                ));
    }

    /**
     * Prompts the user to confirm that they would like to remove the contact
     * with the given display name from a group chat.
     *
     * @param displayName the display name of the contact to remove.
     *
     * @return true if the user confirms that they would like to remove the
     * contact, false otherwise.
     */
    public boolean confirmRemoveFromChat(String displayName)
    {
        sLog.debug("Asking user to confirm remove contact from group chat: " +
                                                  logHasher(displayName));
        boolean removeUser = false;

        String title = resources.getI18NString("service.gui.chat.REMOVE_USER");
        String message =
            resources.getI18NString("service.gui.chat.REMOVE_USER_CONFIRM",
                                      new String[]{displayName});
        String okButtonText =
            resources.getI18NString("service.gui.chat.REMOVE_CONFIRM");

        MessageDialog dialog =
            new MessageDialog(null, title, message, okButtonText);
        int returnCode = dialog.showDialog();
        if (returnCode == MessageDialog.OK_RETURN_CODE)
        {
            sLog.user("Remove contact from group chat, ask again");
            removeUser = true;
        }
        else if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
        {
            sLog.user("Remove contact from group chat, don't ask again");
            removeUser = true;
            ConfigurationUtils.setDontAskRemoveFromChat(true);
        }
        else if (returnCode == MessageDialog.CANCEL_RETURN_CODE)
        {
            sLog.user("User cancelled removing participant from group chat");
            sendRemoveParticipantAnalytic(InsightsResultCode.USER_CANCELLED);
        }

        return removeUser;
    }

    /**
     * Displays an error dialog to the user if their request to remove someone
     * from a group chat fails.
     *
     * @param displayName The display name of the contact that we failed to
     * remove.
     */
    public void displayRemoveChatUserError(String displayName)
    {
        sLog.debug("Displaying remove from chat error for " +
                    logHasher(displayName));

        String errorTitle =
            resources.getI18NString("service.gui.chat.REMOVE_USER_ERROR_TITLE");
        String errorMessage =
            resources.getI18NString("service.gui.chat.REMOVE_USER_ERROR",
                                     new String[]{displayName});
        new ErrorDialog(errorTitle, errorMessage).showDialog();
    }

    /**
     * Obtains the <tt>Contact</tt> corresponding to the given address
     * identifier.
     *
     * @param itemID The address of the <tt>Contact</tt>.
     * @return the <tt>Contact</tt> corresponding to the given address
     * identifier.
     */
    private Contact getContactFromMetaContact(String itemID)
    {
        Iterator<Contact> i = metaContact.getContacts();

        while (i.hasNext())
        {
            Contact contact = i.next();

            String id = contact.getAddress()
                    + contact.getProtocolProvider().getProtocolName();

            if (itemID.equals(id))
            {
                return contact;
            }
        }
        return null;
    }

    /**
     * Indicates that a group has been selected during a move operation. Moves
     * the selected contact to the selected group.
     * @param evt the <tt>ContactListEvent</tt> has
     */
    public void groupClicked(ContactListEvent evt)
    {
        moveDialog.dispose();

        UIGroup sourceGroup = evt.getSourceGroup();

        // TODO: may be show a warning message to tell the user that she should
        // select another group.
        if (!(sourceGroup instanceof MetaUIGroup))
            return;

        MetaContactGroup metaGroup
            = (MetaContactGroup) sourceGroup.getDescriptor();

        contactList.removeContactListListener(this);

        // FIXME: unset the special cursor after a subcontact has been moved
        //guiContactList.setCursor(
        //        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if(moveAllContacts)
        {
            MetaContactListManager
                .moveMetaContactToGroup(metaContact, metaGroup);
        }
        else if(contactToMove != null)
        {
            MetaContactListManager
                .moveContactToGroup(contactToMove, metaGroup);
        }

        contactList.setGroupClickConsumed(false);
    }

    /**
     * Implements ContactListListener.contactSelected method in order
     * to move the chosen sub-contact when a meta contact is selected.
     * @param evt the <tt>ContactListEvent</tt> that notified us
     */
    public void contactClicked(ContactListEvent evt)
    {
        UIContact descriptor = evt.getSourceContact();
        // We're only interested in MetaContacts here.
        if (!(descriptor instanceof MetaUIContact))
            return;

        moveContact((MetaContact) descriptor.getDescriptor());
    }

    /**
     * We're not interested in group selection events here.
     */
    public void groupSelected(ContactListEvent evt) {}

    /**
     * We're not interested in contact selection events here.
     */
    public void contactSelected(ContactListEvent evt) {}

    /**
     * Moves the previously chosen sub-contact in the given toMetaContact.
     *
     * @param toMetaContact the MetaContact, where to move the previously
     * chosen sub-contact.
     */
    private void moveContact(MetaContact toMetaContact)
    {
        moveDialog.dispose();

        if(toMetaContact.equals(metaContact))
        {
            new ErrorDialog(
                resources.getI18NString("service.gui.MOVE_SUBCONTACT"),
                resources.getI18NString("service.gui.MOVE_SUBCONTACT_FAILED"))
                .showDialog();
        }
        else
        {
            contactList.removeContactListListener(this);

            // FIXME: unset the special cursor after a subcontact has been moved
            //guiContactList.setCursor(
            //        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if(moveAllContacts)
            {
                MetaContactListManager
                    .moveMetaContactToMetaContact(metaContact, toMetaContact);
            }
            else if(contactToMove != null)
            {
                MetaContactListManager
                    .moveContactToMetaContact(contactToMove, toMetaContact);
            }
        }
    }

    /**
     * Adds the according plug-in component to this container.
     * @param event received event
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(!c.getContainer()
                .equals(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
            return;

        Object constraints
            = StandardUIServiceImpl.getBorderLayoutConstraintsFromContainer(
                c.getConstraints());

        if (c.getComponent() == null)
            return;

        int ix = c.getPositionIndex();

        if (constraints == null)
        {
            if(ix != -1)
                add((Component) c.getComponent(), ix);
            else
                add((Component) c.getComponent());
        }
        else
        {
            if(ix != -1)
                add((Component) c.getComponent(), constraints, ix);
            else
                add((Component) c.getComponent(), constraints);
        }

        c.setCurrentContact(metaContact);

        repaint();
    }

    /**
     * Removes the according plug-in component from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer()
                .equals(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
        {
            remove((Component) c.getComponent());
        }
    }

    /**
     * Obtains the status icon for the given protocol contact and
     * adds to it the account index information.
     * @param protoContact the protocol contact for which to create the image
     * @return the indexed status image
     */
    public BufferedImageFuture createContactStatusImage(Contact protoContact)
    {
        return
            imageLoaderService.getIndexedProtocolImage(
                protoContact.getPresenceStatus().getStatusIcon(),
                protoContact.getProtocolProvider());
    }

    /**
     * Reloads skin related information.
     */
    public void loadSkin()
    {
        if (ConfigurationUtils.isMenuIconsDisabled())
        {
            return;
        }

        imageLoaderService.getImage(ImageLoaderService.CALL_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(callItem);

        imageLoaderService.getImage(ImageLoaderService.VIDEO_CALL)
        .getImageIcon()
        .addToMenuItem(videoCallItem);

        imageLoaderService.getImage(ImageLoaderService.VIDEO_CALL)
        .getImageIcon()
        .addToMenuItem(videoCallMenu);

        imageLoaderService.getImage(ImageLoaderService.SEND_MESSAGE_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(sendMessageItem);

        imageLoaderService.getImage(ImageLoaderService.SEND_FILE_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(sendFileItem);

        imageLoaderService.getImage(ImageLoaderService.SEND_MESSAGE_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(sendSmsItem);

        imageLoaderService.getImage(ImageLoaderService.RENAME_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(renameContactItem);

        imageLoaderService.getImage(ImageLoaderService.HISTORY_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(viewHistoryItem);

        imageLoaderService.getImage(ImageLoaderService.ADD_CONTACT_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(addContactItem);

        imageLoaderService.getImage(ImageLoaderService.DELETE_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(removeContactMenu);

        imageLoaderService.getImage(ImageLoaderService.CALL_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(callContactMenu);

        imageLoaderService.getImage(ImageLoaderService.UNAUTHORIZED_CONTACT_16x16)
        .getImageIcon()
        .addToMenuItem(requestAuthMenuItem);

        imageLoaderService.getImage(ImageLoaderService.UNAUTHORIZED_CONTACT_16x16)
        .getImageIcon()
        .addToMenuItem(multiContactRequestAuthMenu);
    }

    /**
     * Requests authorization for contact.
     *
     * @param contact the contact for which we request authorization
     */
    private void requestAuthorization(final Contact contact)
    {
        final OperationSetExtendedAuthorizations authOpSet
            = contact.getProtocolProvider().getOperationSet(
                OperationSetExtendedAuthorizations.class);

        if (authOpSet == null)
            return;

        // Get the current implementation of the authorization service.
        final AuthorizationRequest request = GuiActivator.
            getAuthorizationHandlerService().createAuthorizationRequest(contact);

        if (request != null)
        {
            authOpSet.reRequestAuthorization(request, contact);
        }
    }

    /**
     * Calls using the CallManager
     * @param isVideo whether video button is pressed
     * @param contact the contact to call
     * @param contactResource the specific contact resource to call
     */
    private void call(boolean isVideo,
                      Contact contact,
                      ContactResource contactResource)
    {
        if (contactResource != null)
        {
            CallManager.call(contact, contactResource, isVideo);
        }
        else
        {
            CallManager.call(contact, isVideo);
        }
    }

    /**
     * Calls using the CallManager
     * @param isVideo whether video button is pressed
     * @param contactName the phone number to call or the contact name
     *                    selected (normally when using prefix), if null
     *                    will call the metacontact
     */
    private void call(boolean isVideo, String contactName)
    {
        if (contactName != null)
        {
            Contact contact = getContactFromMetaContact(contactName);

            // If we failed to find the contact using the method above, try
            // again by searching on phone number.
            if (contact == null)
            {
                List<Contact> contacts =
                               metaContact.getContactByPhoneNumber(contactName);

                if (!contacts.isEmpty())
                {
                    contact = contacts.get(0);
                }
                else
                {
                    sLog.warn("No contact found for detail " + logHasher(contactName));
                }
            }

            // we want to call particular contact
            if (contact != null)
            {
                CallManager.call(metaContact, contactName, isVideo);
                return;
            }
            else
            {
                // we want to call a phoneNumber
                CallManager.call(contactName, isVideo);
                return;
            }
        }

        // just call the metacontact
        CallManager.call(metaContact, null, isVideo);
    }

    /**
     * A JMenuItem corresponding to a specific protocol <tt>Contact</tt>.
     */
    private class ContactMenuItem
        extends JMenuItem
    {
        private static final long serialVersionUID = 0L;

        /**
         * The associated contact.
         */
        private final Contact contact;

        /**
         * The associated contact resource.
         */
        private ContactResource contactResource;

        /**
         * Creates an instance of <tt>ContactMenuItem</tt>.
         *
         * @param contact the associated protocol <tt>Contact</tt>
         * @param displayName the text to display on the menu
         * @param menuName the name of the menu, used by action listeners
         * @param icon the icon associated by this menu item
         */
        public ContactMenuItem(Contact contact,
                               String displayName,
                               String menuName,
                               ImageIconFuture icon)
        {
            this(contact, null, displayName, menuName, icon, false);
        }

        /**
         * Creates an instance of <tt>ContactMenuItem</tt>.
         *
         * @param contact the associated protocol <tt>Contact</tt>
         * @param contactResource the associated <tt>ContactResource</tt>
         * @param displayName the text to display on the menu
         * @param menuName the name of the menu, used by action listeners
         * @param icon the icon associated by this menu item
         * @param isBold indicates if the menu should be shown in bold
         */
        public ContactMenuItem(Contact contact,
                               ContactResource contactResource,
                               String displayName,
                               String menuName,
                               ImageIconFuture icon,
                               boolean isBold)
        {
            super();

            this.contact = contact;
            this.contactResource = contactResource;

            // Disable HTML to prevent HTML injection
            putClientProperty("html.disable", Boolean.TRUE);
            setText(displayName);

            if (icon != null)
            {
                icon.addToMenuItem(this);
            }

            ScaleUtils.scaleFontAsDefault(this);
            setName(menuName);
            if (isBold)
                setFont(getFont().deriveFont(Font.BOLD));
            addActionListener(MetaContactRightButtonMenu.this);
        }

        /**
         * Returns the protocol <tt>Contact</tt> associated with this menu item.
         *
         * @return the protocol <tt>Contact</tt> associated with this menu item
         */
        Contact getContact()
        {
            return contact;
        }

        /**
         * Returns the <tt>ContactResource</tt> associated with this menu item
         * if such exists otherwise returns null.
         *
         * @return the <tt>ContactResource</tt> associated with this menu item
         * if such exists otherwise returns null
         */
        ContactResource getContactResource()
        {
            return contactResource;
        }
    }
}
