/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.*;
import net.java.sip.communicator.util.ConfigurationUtils;

/**
 * The <tt>MetaUIContact</tt> is the implementation of the UIContact interface
 * for the <tt>MetaContactListService</tt>. This implementation is based on the
 * <tt>MetaContact</tt>.
 *
 * @author Yana Stamcheva
 */
public class MetaUIContact
    extends UIContactImpl
{
    private static final Logger sLog = Logger.getLogger(MetaUIContact.class);

    /**
     * The key of the user data in <tt>MetaContact</tt> which specifies
     * the avatar cached from previous invocations.
     */
    private static final String AVATAR_DATA_KEY
        = MetaUIContact.class.getName() + ".avatar";

    /**
     * A list of all search strings available for the underlying
     * <tt>MetaContact</tt>.
     */
    private final List<String> mSearchStrings = new LinkedList<>();

    /**
     * The <tt>MetaContact</tt>, on which this implementation is based.
     */
    private MetaContact mMetaContact;

    /**
     * The meta contact list source.
     */
    private final MetaContactListSource mMclSource;

    /**
     * The corresponding <tt>ContactNode</tt> in the contact list component
     * data model.
     */
    private ContactNode mContactNode;

    /**
     * The parent <tt>UIGroup</tt> of this contact.
     */
    private UIGroup mParentUIGroup;

    /**
     * The image loader service
     */
    private static final ImageLoaderService sImageLoaderService =
        GuiActivator.getImageLoaderService();

    /**
     * Phone number utils, used for format phone numbers for display in the UI.
     */
    private static final PhoneNumberUtilsService sPhoneNumberUtils =
        GuiActivator.getPhoneNumberUtils();

    /**
     * Unauthorized contact icon - standard size.
     * Defined here to avoid creating them for each contact.
     */
    public static final ImageIconFuture UNAUTH_ICON =
        sImageLoaderService.getImage(ImageLoaderService.UNAUTHORIZED_CONTACT_PHOTO)
        .getScaledEllipticalIcon(sAvatarWidth, sAvatarHeight);

    /**
     * Unauthorized contact icon - extended size.
     * Defined here to avoid creating them for each contact.
     */
    public static final ImageIconFuture EXT_UNAUTH_ICON =
        sImageLoaderService.getImage(ImageLoaderService.UNAUTHORIZED_CONTACT_PHOTO)
        .getScaledEllipticalIcon(sExtendedAvatarWidth, sExtendedAvatarHeight);

    /**
     * Default contact icon - standard size.
     * Defined here to avoid creating them for each contact.
     */
    public static final ImageIconFuture DEFAULT_ICON =
        sImageLoaderService.getImage(ImageLoaderService.DEFAULT_USER_PHOTO)
        .getScaledEllipticalIcon(sAvatarWidth, sAvatarHeight);

    /**
     * Default contact icon - extended size.
     * Defined here to avoid creating them for each contact.
     */
    public static final ImageIconFuture EXT_DEFAULT_ICON =
        sImageLoaderService.getImage(ImageLoaderService.DEFAULT_USER_PHOTO)
        .getScaledEllipticalIcon(sExtendedAvatarWidth, sExtendedAvatarHeight);

    /**
     * Icon for a GroupContact.
     * Defined here to avoid creating them for each contact.
     */
    private static final ImageIconFuture GROUP_ICON = ImageUtils.getScaledEllipticalIcon(
        sImageLoaderService.getImage(ImageLoaderService.DEFAULT_GROUP_CONTACT_PHOTO_SMALL),
                             sAvatarWidth, sAvatarHeight);

    /**
     * Icon for a GroupContact - extended size.
     * Defined here to avoid creating them for each contact.
     */
    private static final ImageIconFuture EXT_GROUP_ICON = ImageUtils.getScaledEllipticalIcon(
        sImageLoaderService.getImage(ImageLoaderService.DEFAULT_GROUP_CONTACT_PHOTO_SMALL),
                            sExtendedAvatarWidth, sExtendedAvatarHeight);

    /**
     * Creates an instance of <tt>MetaUIContact</tt> by specifying the
     * underlying <tt>MetaContact</tt>, on which it's based.
     * @param metaContact the <tt>MetaContact</tt>, on which this implementation
     * is based
     * @param mclSource the MetaContactListSource that maps MetaUIContacts to
     * MetaContacts
     */
    public MetaUIContact(MetaContact metaContact, MetaContactListSource mclSource)
    {
        mMetaContact = metaContact;
        mMclSource = mclSource;

        initSearchStrings();
    }

    /**
     * Returns the underlying <tt>MetaContact</tt>.
     * @return the underlying <tt>MetaContact</tt>
     */
    public Object getDescriptor()
    {
        return mMetaContact;
    }

    /**
     * Returns the display name of this <tt>MetaUIContact</tt>.
     * @return the display name of this <tt>MetaUIContact</tt>
     */
    public String getDisplayName()
    {
        String displayName = mMetaContact.getDisplayName();

        /*
         * If the MetaContact doesn't tell us a display name, make up a display
         * name so that we don't end up with "Unknown user".
         */
        if ((displayName == null) || (displayName.trim().length() == 0))
        {
            /*
             * Try to get a display name from one of the Contacts of the
             * MetaContact. If that doesn't cut it, use the address of a
             * Contact. Because it's not really clear which address to display
             * when there are multiple Contacts, use the address only when
             * there's a single Contact in the MetaContact.
             */
            Iterator<Contact> contactIter = mMetaContact.getContacts();
            int contactCount = 0;
            String address = null;

            while (contactIter.hasNext())
            {
                Contact contact = contactIter.next();

                contactCount++;

                displayName = contact.getDisplayName();
                if ((displayName == null) || (displayName.trim().length() == 0))
                {
                    /*
                     * As said earlier, only use an address if there's a single
                     * Contact in the MetaContact.
                     */
                    address = (contactCount == 1) ? contact.getAddress() : null;
                }
                else
                    break;
            }
            if ((address != null)
                    && (address.trim().length() != 0)
                    && ((displayName == null)
                            || (displayName.trim().length() == 0)))
                displayName = address;
        }
        return displayName;
    }

    /**
     * Returns the index of the underlying <tt>MetaContact</tt> in its
     * <tt>MetaContactListService</tt> parent group.
     * @return the source index of the underlying <tt>MetaContact</tt>
     */
    public int getSourceIndex()
    {
        MetaContactGroup parentMetaContactGroup =
            mMetaContact.getParentMetaContactGroup();
        if (parentMetaContactGroup == null)
            return -1;
        return parentMetaContactGroup.indexOf(mMetaContact);
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of strings, which can be used
     * to find this contact.
     * @return an <tt>Iterator</tt> over a list of search strings
     */
    public Iterator<String> getSearchStrings()
    {
        return mSearchStrings.iterator();
    }

    /**
     * Returns the general status icon of the given MetaContact. Detects the
     * status using the priority status table. The priority is defined on
     * the "availability" factor and here the most "available" status is
     * returned.
     *
     * @return ImageIcon The icon indicating the most "available" status from
     * all sub-contact statuses.
     */
    public ImageIconFuture getStatusIcon()
    {
        PresenceStatus status = getContactPresenceStatus();

        if (status != null)
            return Constants.getStatusIcon(status).getImageIcon();

        return null;
    }

    /**
     * UI REFRESH DELETION CANDIDATE
     * REFACTORED TO: MetaContactImpl.getPresence()
     *
     * Returns the general status of the given MetaContact. Detects the
     * status using the priority status table. The priority is defined on
     * the "availability" factor and here the most "available" status is
     * returned.
     *
     * @return PresenceStatus The most "available" status from all
     * sub-contact statuses.
     */
    public PresenceStatus getContactPresenceStatus()
    {
        PresenceStatus status = null;
        Iterator<Contact> i = mMetaContact.getContacts();
        while (i.hasNext()) {
            Contact protoContact = i.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();

            if (!protoContact.getProtocolProvider().supportsStatus())
            {
                // Contact doesn't support status
                continue;
            }

            if (status == null)
                status = contactStatus;
            else
                status = (contactStatus.compareTo(status) > 0)
                        ? contactStatus
                        : status;
        }

        return status;
    }

    /**
     * Returns the parent <tt>UIGroup</tt>.
     * @return the parent <tt>UIGroup</tt>
     */
    public UIGroup getParentGroup()
    {
        return mParentUIGroup;
    }

    /**
     * Sets the given <tt>parentGroup</tt> to be the parent <tt>UIGroup</tt>
     * of this <tt>MetaUIContact</tt>.
     * @param parentGroup the parent <tt>UIGroup</tt> to set
     */
    public void setParentGroup(UIGroup parentGroup)
    {
        mParentUIGroup = parentGroup;
    }

    /**
     * Returns the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class.
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class
     */
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
    {
        List<UIContactDetail> details
            = getContactDetailsForOperationSet(opSetClass);

        return (details != null && !details.isEmpty()) ? details.get(0) : null;
    }

    /**
     * Returns a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class.
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class
     */
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass)
    {
        List<UIContactDetail> resultList
            = new LinkedList<>();

        Iterator<Contact> contacts
            = mMetaContact.getContactsForOperationSet(opSetClass).iterator();

        while (contacts.hasNext())
        {
            resultList.add(new MetaContactDetail(contacts.next()));
        }
        return resultList;
    }

    /**
     * Returns a list of all <tt>UIContactDetail</tt>s within this
     * <tt>UIContact</tt>.
     *
     * @return a list of all <tt>UIContactDetail</tt>s within this
     * <tt>UIContact</tt>
     */
    public List<UIContactDetail> getContactDetails()
    {
        List<UIContactDetail> resultList
            = new LinkedList<>();

        Iterator<Contact> contacts = mMetaContact.getContacts();

        while (contacts.hasNext())
        {
            resultList.add(new MetaContactDetail(contacts.next()));
        }
        return resultList;
    }

    /**
     * Gets the avatar of a specific <tt>MetaContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     *
     * @param isExtended indicates if the avatar should be the extended size
     * @return an <tt>ImageIcon</tt> which represents the avatar of the
     * specified <tt>MetaContact</tt>
     */
    public ImageIconFuture getAvatar(boolean isExtended)
    {
        BufferedImageFuture avatarBytes = mMetaContact.getAvatar(true);

        // If there's no avatar we return the relevant silhouette image.
        if(avatarBytes == null)
        {
            ImageIconFuture avatar;

            if (mMetaContact.getGroupContact() != null)
            {
                avatar = isExtended ? EXT_GROUP_ICON : GROUP_ICON;
            }
            else
            {
                avatar = isExtended ? EXT_UNAUTH_ICON : UNAUTH_ICON;
            }

            return mMetaContact.addOverlayToAvatar(avatar);
        }

        // If the cell is selected we return a zoomed version of the avatar
        // image.
        if (isExtended)
        {
            return avatarBytes.getScaledEllipticalIcon(sExtendedAvatarWidth,
                                                       sExtendedAvatarHeight);
        }

        // In any other case try to get the avatar from the cache.
        Object[] avatarCache
            = (Object[]) mMetaContact.getData(AVATAR_DATA_KEY);
        ImageIconFuture avatar = null;

        if ((avatarCache != null) && (avatarCache[0] == avatarBytes))
            avatar = (ImageIconFuture) avatarCache[1];

        // If the avatar isn't available or it's not up-to-date, create it.
        if (avatar == null)
        {
            avatar = avatarBytes.getScaledEllipticalIcon(sAvatarWidth,
                                                         sAvatarHeight);

            // If we have an overlay, then draw it:
            avatar = mMetaContact.addOverlayToAvatar(avatar);
        }

        // Cache the avatar in case it has changed.
        if (avatarCache == null)
        {
            if (avatar != null)
                mMetaContact.setData(
                    AVATAR_DATA_KEY,
                    new Object[] { avatarBytes, avatar });
        }
        else
        {
            avatarCache[0] = avatarBytes;
            avatarCache[1] = avatar;
        }

        return avatar;
    }

    /**
     * UI REFRESH DELETION CANDIDATE
     * REFACTORED TO: MetaContactImpl.getCustomStatus()
     *
     * Returns the display details for the underlying <tt>MetaContact</tt>,
     * based on its associated ProtoContacts.
     *
     * If a status message is explicitly set for a protocontact, we use that.
     *
     * If no status is set for any protocontact, but a protocontact supports
     * presence subscription and is not yet authorized, we generate an
     * authorization-related status message (e.g. "Waiting for Authorization"
     * or "Authorization Failed").
     *
     * Otherwise, we show no status message.
     *
     * If two or more protocontacts have a status message, or if two or more
     * protocontacts have an authorization message to display, we choose one
     * arbitrarily.
     *
     * @return the display details for the underlying <tt>MetaContact</tt>
     */
    public String getDisplayDetails()
    {
        String displayDetails = null;

        Iterator<Contact> protoContacts = mMetaContact.getContacts();

        // Iterate through the underlying protocontacts, and extract the most
        // appropriate status message to be displayed for the parent.
        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();
            ProtocolProviderService protocolProvider =
                protoContact.getProtocolProvider();

            OperationSetExtendedAuthorizations authOpSet =
                protocolProvider.getOperationSet(
                    OperationSetExtendedAuthorizations.class);

            if (authOpSet != null
                && SubscriptionStatus.Subscribed !=
                   authOpSet.getSubscriptionStatus(protoContact))
            {
                OperationSetPersistentPresence persPresOpsSet =
                    protocolProvider.getOperationSet(
                        OperationSetPersistentPresence.class);

                if ((persPresOpsSet != null) &&
                    !persPresOpsSet.unsubscribeSupported(protoContact))
                {
                    // Unsubscribe isn't supported, so the contact will be
                    // automatically added and authorized the next time they
                    // come online.
                    // Therefore 'waiting for authorization' and
                    // 'not authorized' aren't appropriate messages; instead we
                    // just show 'offline'.
                    displayDetails = GuiActivator.getResources()
                        .getI18NString("service.gui.offline");
                }
                else
                {
                    // We're not yet subscribed to this protocontact, so
                    // generate an appropriate status message.
                    SubscriptionStatus status =
                        authOpSet.getSubscriptionStatus(protoContact);

                    if (status == null)
                        displayDetails = GuiActivator.getResources()
                            .getI18NString("service.gui.offline");
                    else if (status.equals(SubscriptionStatus.SubscriptionPending))
                        displayDetails = GuiActivator.getResources()
                            .getI18NString("service.gui.WAITING_AUTHORIZATION");
                    else if (status.equals(SubscriptionStatus.NotSubscribed))
                        displayDetails = GuiActivator.getResources()
                            .getI18NString("service.gui.NOT_AUTHORIZED");
                }
            }
            else
            {
                String statusMessage = protoContact.getStatusMessage();
                if (!StringUtils.isNullOrEmpty(statusMessage))
                {
                    // The contact doesn't need a subscription (or we're
                    // already subscribed) - and already has a status set.
                    // Break out of the loop, as we want to use this in preference
                    // to generating a status ourselves.
                    displayDetails = statusMessage;
                    break;
                }
            }
        }

        return displayDetails;
    }

    /**
     * Returns the tool tip opened on mouse over.
     * @return the tool tip opened on mouse over
     */
    public String getToolTip(String additionalText, String emphasizedNumber)
    {
        String tipText = "";

        Iterator<Contact> i = mMetaContact.getContacts();

        ContactPhoneUtil contactPhoneUtil =
                                   ContactPhoneUtil.getPhoneUtil(mMetaContact);

        Contact protocolContact;

        // Add all the details into various collections so that we can add the
        // details in order of type, rather than the contact that it came from
        Set<String> allPhones = new LinkedHashSet<>();
        List<String> imAddresses = new ArrayList<>();

        while (i.hasNext())
        {
            protocolContact = i.next();

            // Only add the address to the tooltip if the persistent presence
            // operation set is marked as having a user-visible address.
            ProtocolProviderService provider =
                                          protocolContact.getProtocolProvider();

            OperationSetBasicInstantMessaging opSetInstantMessaging = provider
                      .getOperationSet(OperationSetBasicInstantMessaging.class);

            if (opSetInstantMessaging != null)
            {
                imAddresses.add(protocolContact.getAddress());
            }

            // Only add phone numbers if calling function is enabled.
            if (ConfigurationUtils.isCallingEnabled())
            {
                List<String> phones = contactPhoneUtil.getPhones(protocolContact);

                if (phones != null)
                {
                    allPhones.addAll(phones);
                }
            }
        }

        Iterator<String> allPhonesIt = allPhones.iterator();
        while (allPhonesIt.hasNext())
        {
            String phone = allPhonesIt.next();
            String phoneNumber;
            String category = "";

            // The phone will be in the format of something like "(234)
            // 555-1234 (Work)".  We need to separate the number and the
            // category, so try to split the string at the last instance of
            // "(".  We also want to get rid of the brackets from the category
            // entirely.
            try
            {
                int catIndex = phone.lastIndexOf("(");
                phoneNumber = phone.substring(0, catIndex).trim();
                category = phone.substring(catIndex).
                    replace("(", "").replace(")", "").trim();
            }
            catch (Exception ex)
            {
                sLog.warn("Couldn't parse number and category for tooltip: " +
                          logHasher(phone), ex);
                phoneNumber = phone;
            }

            tipText = tipText +
                      "<p style=\"font-size:" +
                      ScaleUtils.scaleInt(9) +
                      "px;color:#333333;\">";

            // In the case of a call history entry, the emphasized number is
            // the number that was actually involved in the call.  We need to
            // display that number in bold. To make it easier to compare, we
            // convert the formatted phone number to national format (e.g.
            // "2345551234"). To ensure we can convert the number, strip its
            // ELC and trim whitespace before trying to convert it.  We don't
            // know how the user might have dialled the call, so we can't do an
            // exact string match, but if either of the numbers ends with the
            // other, we can be sure of a match (e.g. if the user dialled
            // "92345551234" or "5551234").
            String nationalNumber =
                sPhoneNumberUtils.formatNumberToNational(
                                sPhoneNumberUtils.stripELC(phoneNumber).trim());

            if (emphasizedNumber != null &&
                (nationalNumber.endsWith(emphasizedNumber) ||
                emphasizedNumber.endsWith(nationalNumber)))
            {
                tipText = tipText + "<b>" + phoneNumber + "</b>";
            }
            else
            {
                tipText = tipText + phoneNumber;
            }

            tipText = tipText + "<i style=\"color:#888888;\"> " + category + "</i></p>";
        }

        for (String imAddressString : imAddresses)
        {
            String imTipText = "<span style=\"line-height:" +
                               ScaleUtils.scaleInt(40) +
                               "px;\"><p style=\"font-size:" +
                               ScaleUtils.scaleInt(9) +
                               "px;color:#333333;\">" +
                               imAddressString + "</p><span>";

            // In the case of a message history entry, the emphasized number is
            // actually the IM address that was involved in the conversation.
            // We need to display that IM address in bold. The IM address and
            // emphasized number much match exactly (ignoring case), otherwise
            // we may get false-positive matches for phone calls involving the
            // main phone number that is also the beginning of the IM address
            // when the deployment is using CommPortal provisioned IM.
            if (emphasizedNumber != null &&
                imAddressString.equalsIgnoreCase(emphasizedNumber))
            {
                tipText = tipText + "<b>" + imTipText + "</b>";
            }
            else
            {
                tipText = tipText + imTipText;
            }
        }

        // Add the additional text
        if (!StringUtils.isNullOrEmpty(additionalText))
        {
            tipText = tipText + additionalText;
        }

        // Close the html tag
        if (!StringUtils.isNullOrEmpty(tipText))
        {
            tipText = "<html>" + tipText + "</html>";
        }

        return tipText;
    }

    /**
     * Returns the corresponding <tt>ContactNode</tt> in the contact list
     * component data model.
     * @return the corresponding <tt>ContactNode</tt>
     */
    public ContactNode getContactNode()
    {
        return mContactNode;
    }

    /**
     * Sets the corresponding <tt>ContactNode</tt>.
     * @param contactNode the corresponding <tt>ContactNode</tt> in the contact
     * list component data model
     */
    public void setContactNode(ContactNode contactNode)
    {
        mContactNode = contactNode;
        if (contactNode == null)
        {
            mMclSource.removeUIContact(mMetaContact);
        }
    }

    /**
     * Initializes all search strings for this <tt>MetaUIGroup</tt>.
     */
    private void initSearchStrings()
    {
        mSearchStrings.add(mMetaContact.getDisplayName());

        Iterator<Contact> contacts = mMetaContact.getContacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();
            mSearchStrings.add(contact.getDisplayName());
            mSearchStrings.add(contact.getAddress());
        }
    }

    /**
     * Obtains the <tt>MetaContact</tt> represented by this
     * <tt>MetaUIContact</tt>.
     *
     * @return The underlying <tt>MetaContact</tt>.
     */
    public MetaContact getMetaContact()
    {
        return mMetaContact;
    }

    /**
     * The implementation of the <tt>UIContactDetail</tt> interface for the
     * <tt>MetaContactListService</tt>.
     */
    private class MetaContactDetail
        extends UIContactDetailImpl
    {
        /**
         * The underlying protocol contact.
         */
        private Contact mContact;

        /**
         * Creates an instance of <tt>MetaContactDetail</tt> by specifying the
         * underlying protocol <tt>Contact</tt>.
         *
         * @param contact the protocol contact, on which this implementation
         * is based
         */
        public MetaContactDetail(Contact contact)
        {
            super(contact.getAddress(),
                  contact.getDisplayName(),
                  contact.getPresenceStatus().getStatusIcon().getImageIcon(),
                  contact);

            mContact = contact;

            ProtocolProviderService parentProvider
                = contact.getProtocolProvider();

            Iterator<Class<? extends OperationSet>> opSetClasses
                = parentProvider.getSupportedOperationSetClasses().iterator();

            while (opSetClasses.hasNext())
            {
                Class<? extends OperationSet> opSetClass = opSetClasses.next();

                addPreferredProtocolProvider(opSetClass, parentProvider);
                addPreferredProtocol(opSetClass,
                                    parentProvider.getProtocolName());
            }
        }

        /**
         * Returns the presence status of the underlying protocol
         * <tt>Contact</tt>.
         * @return the presence status of the underlying protocol
         * <tt>Contact</tt>
         */
        public PresenceStatus getPresenceStatus()
        {
            return mContact.getPresenceStatus();
        }
    }

    /**
     * Returns the right button menu component.
     * @return the right button menu component
     */
    public JPopupMenu getRightButtonMenu(boolean useReducedMenu)
    {
        JPopupMenu rightButtonMenu;

        if (useReducedMenu)
        {
            rightButtonMenu =
                new ReducedMetaContactRightButtonMenu(mMetaContact, null);
        }
        else if (getMetaContact().getGroupContact() != null)
        {
            rightButtonMenu = new GroupContactRightButtonMenu(mMetaContact);
        }
        else
        {
            rightButtonMenu = new MetaContactRightButtonMenu(mMetaContact);
        }

        return rightButtonMenu;
    }

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    public Collection<SIPCommButton> getContactCustomActionButtons()
    {
        return mMclSource.getContactCustomActionButtons(this);
    }

    @Override
    public boolean canBeMessaged()
    {
        return mMetaContact.canBeMessaged();
    }
}
