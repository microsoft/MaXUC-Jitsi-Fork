/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * The <tt>SourceUIContact</tt> is the implementation of the UIContact for the
 * <tt>ExternalContactSource</tt>.
 *
 * @author Yana Stamcheva
 */
public class SourceUIContact
    extends UIContactImpl
{
    private static final Logger sLog = Logger.getLogger(SourceUIContact.class);

    /**
     * Phone number utils, used for format phone numbers for display in the UI.
     */
    private static final PhoneNumberUtilsService sPhoneNumberUtils =
                                             GuiActivator.getPhoneNumberUtils();

    /**
     * The corresponding <tt>SourceContact</tt>, on which this abstraction is
     * based.
     */
    private final SourceContact mSourceContact;

    /**
     * The corresponding <tt>ContactNode</tt> in the contact list component.
     */
    private ContactNode mContactNode;

    /**
     * The parent <tt>UIGroup</tt>.
     */
    private ExternalContactSource.SourceUIGroup mUiGroup;

    /**
     * The meta contact list source.
     */
    private final MetaContactListSource mMclSource;

    /**
     * The search strings for this <tt>UIContact</tt>.
     */
    private final List<String> mSearchStrings = new LinkedList<>();

    /**
     * Creates an instance of <tt>SourceUIContact</tt> by specifying the
     * <tt>SourceContact</tt>, on which this abstraction is based and the
     * parent <tt>UIGroup</tt>.
     *
     * @param contact the <tt>SourceContact</tt>, on which this abstraction
     * is based
     * @param parentGroup the parent <tt>UIGroup</tt>
     * @param mclSource the <tt>MetaContactListSource</tt>
     */
    public SourceUIContact(SourceContact contact,
                           ExternalContactSource.SourceUIGroup parentGroup,
                           MetaContactListSource mclSource)
    {
        mSourceContact = contact;
        mUiGroup = parentGroup;
        mMclSource = mclSource;

        if (contact.getContactDetails() != null)
            for(ContactDetail detail : contact.getContactDetails())
            {
                sLog.trace("Found contact detail" + detail.getCategory());

                if(detail.getDetail() != null)
                {
                    sLog.debug("Add contact detail: " +
                                           logHasher(detail.getDetail()));
                    mSearchStrings.add(detail.getDetail());
                }
            }

        mSearchStrings.add(contact.getDisplayName());
    }

    /**
     * Returns the display name of the underlying <tt>SourceContact</tt>.
     * @return the display name
     */
    public String getDisplayName()
    {
        return mSourceContact.getDisplayName();
    }

    /**
     * Returns the parent <tt>UIGroup</tt>.
     * @return the parent <tt>UIGroup</tt>
     */
    public UIGroup getParentGroup()
    {
        return mUiGroup;
    }

    /**
     * The parent group of source contacts could not be changed.
     *
     * @param parentGroup the parent group to set
     */
    public void setParentGroup(UIGroup parentGroup) {}

    /**
     * Returns -1 to indicate that the source index of the underlying
     * <tt>SourceContact</tt> is unknown.
     * @return -1
     */
    public int getSourceIndex()
    {
        return mSourceContact.getIndex();
    }

    /**
     * Returns null to indicate unknown status of the underlying
     * <tt>SourceContact</tt>.
     * @return null
     */
    public ImageIconFuture getStatusIcon()
    {
        PresenceStatus status = mSourceContact.getPresenceStatus();

        if (status != null)
            return Constants.getStatusIcon(status).getImageIcon();

        return GuiActivator.getImageLoaderService().getImage(ImageLoaderService.USER_NO_STATUS_ICON)
            .getImageIcon();
    }

    /**
     * Returns the image corresponding to the underlying <tt>SourceContact</tt>.
     *
     * @param isExtended indicates if the avatar should be the extended size
     * @return an <tt>ImageIcon</tt> which represents the avatar of the
     * specified <tt>MetaContact</tt>
     */
    public ImageIconFuture getAvatar(boolean isExtended)
    {
        BufferedImageFuture image = mSourceContact.getImage();

        if (image != null)
        {
            ImageIcon icon = image.getImageIcon().resolve();
            int width;
            int height;

            if (isExtended)
            {
                width = UIContactImpl.sExtendedAvatarWidth;
                height = UIContactImpl.sExtendedAvatarHeight;
            }
            else
            {
                width = UIContactImpl.sAvatarWidth;
                height = UIContactImpl.sAvatarHeight;
            }

            // Scale the avatar if it is too big for the selected size.
            if (icon.getIconWidth() > width ||
                icon.getIconHeight() > height)
            {
                icon = ImageUtils.getScaledEllipticalIcon(
                    icon.getImage(),
                    width, height);
            }

            return new ImageIconAvailable(icon);
        }
        else
            return null;
    }

    /**
     * Returns the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class
     */
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
    {
        List<UIContactDetail> details
            = getContactDetailsForOperationSet(opSetClass);

        if (details != null && !details.isEmpty())
            return details.get(0);
        return null;
    }

    /**
     * Returns the underlying <tt>SourceContact</tt> this abstraction is about.
     * @return the underlying <tt>SourceContact</tt>
     */
    public Object getDescriptor()
    {
        return mSourceContact;
    }

    /**
     * Returns the display details for the underlying <tt>SourceContact</tt>.
     * @return the display details for the underlying <tt>SourceContact</tt>
     */
    public String getDisplayDetails()
    {
        return mSourceContact.getDisplayDetails();
    }

    /**
     * Returns the tooltip display details for the underlying <tt>SourceContact</tt>.
     * @return the tooltip display details for the underlying <tt>SourceContact</tt>
     */
    public String getTooltipDisplayDetails()
    {
        return mSourceContact.getTooltipDisplayDetails();
    }

    /**
     * Returns a list of all contained <tt>UIContactDetail</tt>s.
     *
     * @return a list of all contained <tt>UIContactDetail</tt>s
     */
    public List<UIContactDetail> getContactDetails()
    {
        List<UIContactDetail> resultList
            = new LinkedList<>();

        Iterator<ContactDetail> details
            = mSourceContact.getContactDetails().iterator();

        while (details.hasNext())
        {
            ContactDetail detail = details.next();

            resultList.add(new SourceContactDetail(
                        detail,
                        getInternationalizedLabel(detail.getCategory()),
                        getInternationalizedLabels(
                            detail.getSubCategories().iterator()),
                        null, mSourceContact));
        }
        return resultList;
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

        Iterator<ContactDetail> details
            = mSourceContact.getContactDetails().iterator();

        while (details.hasNext())
        {
            ContactDetail detail = details.next();

            List<Class<? extends OperationSet>> supportedOperationSets
                = detail.getSupportedOperationSets();

            if ((supportedOperationSets != null)
                    && supportedOperationSets.contains(opSetClass)
                    && !detail.getDetail().isEmpty())
            {
                resultList.add(new SourceContactDetail(
                    detail,
                    getInternationalizedLabel(detail.getCategory()),
                    getInternationalizedLabels(
                        detail.getSubCategories().iterator()),
                    opSetClass,
                    mSourceContact));
            }
        }
        return resultList;
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
     * Returns the corresponding <tt>ContactNode</tt> from the contact list
     * component.
     * @return the corresponding <tt>ContactNode</tt>
     */
    public ContactNode getContactNode()
    {
        return mContactNode;
    }

    /**
     * Returns the corresponding <tt>SourceContact</tt> for this component
     * @return the corresponding <tt>SourceContact</tt>
     */
    public SourceContact getSourceContact()
    {
        return mSourceContact;
    }

    /**
     * Sets the corresponding <tt>ContactNode</tt>.
     * @param contactNode the corresponding <tt>ContactNode</tt>
     */
    public void setContactNode(ContactNode contactNode)
    {
        mContactNode = contactNode;

        if (contactNode == null)
            mUiGroup.getParentUISource().removeUIContact(mSourceContact);

        // If the source contact is linked to a MetaContact, it will actually
        // be being displayed as a MetaUIContact which we'll also need to
        // update.
        @SuppressWarnings("unchecked")
        List<MetaContact> metaContacts = (List<MetaContact>)
            mSourceContact.getData(SourceContact.DATA_META_CONTACTS);

        MetaContact metaContact =
            (metaContacts != null && metaContacts.size() > 0) ?
                metaContacts.get(0) : null;

        if (metaContact != null)
        {
            UIContactImpl contact = mMclSource.getUIContact(metaContact);
            if (contact != null)
            {
                contact.setContactNode(contactNode);
            }
        }
    }

    /**
     * The implementation of the <tt>UIContactDetail</tt> interface for the
     * external source <tt>ContactDetail</tt>s.
     */
    protected static class SourceContactDetail
        extends UIContactDetailImpl
    {
        /**
         * Creates an instance of <tt>SourceContactDetail</tt> by specifying
         * the underlying <tt>detail</tt> and the <tt>OperationSet</tt> class
         * for it.
         * @param detail the underlying <tt>ContactDetail</tt>
         * @param category detail category string
         * @param subCategories the detail list of sub-categories
         * @param opSetClass the <tt>OperationSet</tt> class for the
         * preferred protocol provider
         * @param sourceContact the source contact
         */
        public SourceContactDetail(ContactDetail detail,
                                   String category,
                                   Collection<String> subCategories,
                                   Class<? extends OperationSet> opSetClass,
                                   SourceContact sourceContact)
        {
            super(detail.getDetail(),
                  detail.getDetail(),
                  category,
                  subCategories,
                  null,
                  null,
                  null,
                  detail);

            ContactSourceService contactSource
                = sourceContact.getContactSource();

            if (contactSource instanceof ExtendedContactSourceService)
            {
                String prefix = ((ExtendedContactSourceService) contactSource)
                    .getPhoneNumberPrefix();

                if (prefix != null)
                    setPrefix(prefix);
            }

            addPreferredProtocolProvider(opSetClass,
                detail.getPreferredProtocolProvider(opSetClass));
            addPreferredProtocol(opSetClass,
                detail.getPreferredProtocol(opSetClass));
        }

        /**
         * Creates an instance of <tt>SourceContactDetail</tt> by specifying
         * the underlying <tt>detail</tt> and the <tt>OperationSet</tt> class
         * for it.
         *
         * @param displayName the display name
         * @param sourceContact the source contact
         */
        public SourceContactDetail(String displayName,
                                   SourceContact sourceContact)
        {
            super(displayName,
                  displayName,
                  null,
                  null,
                  null,
                  null,
                  null,
                  sourceContact);
        }

        /**
         * Returns null to indicate that this detail doesn't support presence.
         * @return null
         */
        public PresenceStatus getPresenceStatus()
        {
            return null;
        }
    }

    /**
     * Returns the <tt>JPopupMenu</tt> opened on a right button click over this
     * <tt>SourceUIContact</tt>.
     * @return the <tt>JPopupMenu</tt> opened on a right button click over this
     * <tt>SourceUIContact</tt>
     */
    public JPopupMenu getRightButtonMenu(boolean useReducedMenu)
    {
        return new SourceContactRightButtonMenu(this);
    }

    /**
     * Returns the tool tip opened on mouse over.
     *
     * @param additionalText any additional text to display in this tooltip
     * @param emphasizedNumber if this number is found then it is emphasized in
     *        the tooltip
     * @return the tool tip opened on mouse over
     */
    public String getToolTip(String additionalText, String emphasizedNumber)
    {
        String tipText = "<html>";

        try
        {
            List<ContactDetail> details = mSourceContact.getContactDetails(
                            ContactDetail.Category.Phone);

            if (details != null && details.size() > 0)
                tipText = addDetailsToToolTip(details,
                                              tipText,
                                              emphasizedNumber);

            details = mSourceContact.getContactDetails(
                ContactDetail.Category.InstantMessaging);

            if (details != null && details.size() > 0)
                tipText = addDetailsToToolTip(details,
                                              tipText,
                                              emphasizedNumber);
        }
        catch (OperationNotSupportedException e)
        {
            // Categories aren't supported. This is the case for history
            // records.
            List<ContactDetail> allDetails = mSourceContact.getContactDetails();

            tipText = addDetailsToToolTip(allDetails,
                                          tipText,
                                          emphasizedNumber);
        }

        if (!StringUtils.isNullOrEmpty(additionalText))
        {
            tipText = tipText + additionalText;
        }

        tipText = tipText + "</html>";
        return tipText;
    }

    private String addDetailsToToolTip(List<ContactDetail> details,
                                       String toolTip,
                                       String emphasizedNumber)
    {
        sLog.debug("addDetailsToToolTip");
        ContactDetail contactDetail;

        Iterator<ContactDetail> detailsIter = details.iterator();
        while (detailsIter.hasNext())
        {
            contactDetail = detailsIter.next();
            Collection<ContactDetail.SubCategory> subCategories
                = contactDetail.getSubCategories();

            String categoryString = null;

            // Do not add group chat details to the tooltip
            if (ContactDetail.Category.GroupChat.equals(contactDetail.getCategory()))
                continue;

            // Do not add phone details if calling function is disabled
            if (ContactDetail.Category.Phone.equals(contactDetail.getCategory()) &&
                !ConfigurationUtils.isCallingEnabled())
                {
                     sLog.debug("Skip adding phone details");
                     continue;
                }

            if (subCategories != null && subCategories.size() > 0)
            {
                // Just take the first category we find
                categoryString =
                     getInternationalizedLabel(subCategories.iterator().next());
            }

            String detail = contactDetail.getDetail();

            if (StringUtils.isNullOrEmpty(detail))
            {
                continue;
            }

            toolTip = toolTip +
                      "<p style=\"font-size:" +
                      ScaleUtils.scaleInt(9) +
                      "px;color:#333333;\">";

            // If this number matches the emphasized number, display it in bold.
            String nationalNumber = sPhoneNumberUtils.formatNumberToNational(detail);
            boolean isEmphasizedNumber =
                (emphasizedNumber != null && nationalNumber.contains(emphasizedNumber));

            if (isEmphasizedNumber)
            {
                toolTip = toolTip + "<b>";
            }

            // If this is a phone number, format it nicely before displaying it
            // in the UI.
            String formattedDetail = detail;
            if (ContactDetail.Category.Phone.equals(contactDetail.getCategory()))
            {
                formattedDetail = sPhoneNumberUtils.formatNumberForDisplay(detail);
            }

            if (StringUtils.isNullOrEmpty(categoryString))
            {
                toolTip = toolTip + formattedDetail;
            }
            else
            {
                toolTip = toolTip + formattedDetail +
                          " <i style=\"color:#888888;\">" + categoryString;
            }

            if (isEmphasizedNumber)
            {
                toolTip = toolTip + "</b>";
            }

            toolTip = toolTip + "</p>";
        }

        return toolTip;
    }

    /**
     * Returns the internationalized category corresponding to the given
     * <tt>ContactDetail.Category</tt>.
     *
     * @param category the <tt>ContactDetail.SubCategory</tt>, for which we
     * would like to obtain an internationalized label
     * @return the internationalized label corresponding to the given category
     */
    protected String getInternationalizedLabel(ContactDetail.Category category)
    {
        if (category == null)
            return null;

        String categoryString = null;

        ResourceManagementService resources = GuiActivator.getResources();

        switch(category)
        {
        case Address:
            categoryString = resources.getI18NString("service.gui.ADDRESS");
            break;
        case Email:
            categoryString = resources.getI18NString("service.gui.EMAIL");
            break;
        case Personal:
            categoryString = resources.getI18NString("service.gui.PERSONAL");
            break;
        case Organization:
            categoryString = resources.getI18NString("service.gui.ORGANIZATION");
            break;
        case Phone:
            categoryString = resources.getI18NString("service.gui.PHONE");
            break;
        case InstantMessaging:
            categoryString = resources.getI18NString("service.gui.IM");
            break;
        case GroupChat:
            // No-op added to remove a warning - perhaps we should handle it?
            break;
        }

        return categoryString;
    }

    /**
     * Returns a collection of internationalized string corresponding to the
     * given subCategories.
     *
     * @param subCategories an Iterator over a list of
     * <tt>ContactDetail.SubCategory</tt>s
     * @return a collection of internationalized string corresponding to the
     * given subCategories
     */
    protected Collection<String> getInternationalizedLabels(
        Iterator<ContactDetail.SubCategory> subCategories)
    {
        Collection<String> labels = new LinkedList<>();

        while (subCategories.hasNext())
        {
            labels.add(getInternationalizedLabel(subCategories.next()));
        }

        return labels;
    }

    /**
     * Returns the internationalized label corresponding to the given category.
     *
     * @param subCategory the <tt>ContactDetail.SubCategory</tt>, for which we
     * would like to obtain an internationalized label
     * @return the internationalized label corresponding to the given category
     */
    protected String getInternationalizedLabel(
                                ContactDetail.SubCategory subCategory)
    {
        if (subCategory == null)
            return null;

        String label;
        ResourceManagementService resources = GuiActivator.getResources();

        switch(subCategory)
        {
        case City:
            label = resources.getI18NString("service.gui.CITY");
            break;
        case Country:
            label = resources.getI18NString("service.gui.COUNTRY");
            break;
        case Fax:
            label = resources.getI18NString("service.gui.FAX");
            break;
        case Home:
            label = resources.getI18NString("service.gui.HOME");
            break;
        case HomePage:
            label = resources.getI18NString("service.gui.HOME_PAGE");
            break;
        case JobTitle:
            label = resources.getI18NString("service.gui.JOB_TITLE");
            break;
        case LastName:
            label = resources.getI18NString("service.gui.LAST_NAME");
            break;
        case Mobile:
            label = resources.getI18NString("service.gui.MOBILE_PHONE");
            break;
        case Name:
            label = resources.getI18NString("service.gui.NAME");
            break;
        case Nickname:
            label = resources.getI18NString("service.gui.NICKNAME");
            break;
        case Other:
            label = resources.getI18NString("service.gui.OTHER");
            break;
        case PostalCode:
            label = resources.getI18NString("service.gui.POSTAL_CODE");
            break;
        case Street:
            label = resources.getI18NString("service.gui.STREET");
            break;
        case Work:
            label = resources.getI18NString("service.gui.WORK_PHONE");
            break;
        case AIM:
        case ICQ:
        case Jabber:
        case MSN:
        case Yahoo:
        case Skype:
        case GoogleTalk:
        case Facebook:
            label = subCategory.value();
            break;
        default:
            label = null;
            break;
        }

        return label;
    }

    /**
     * Returns all custom action buttons for this notification contact.
     *
     * @return a list of all custom action buttons for this notification contact
     */
    public Collection<SIPCommButton> getContactCustomActionButtons()
    {
        if (mSourceContact != null)
        {
            return mUiGroup.getParentUISource()
                    .getContactCustomActionButtons(mSourceContact);
        }

        return null;
    }

    /**
     * Returns whether chat UI should be enabled for this contact.
     */
    @Override
    public boolean canBeMessaged()
    {
        return mSourceContact.canBeMessaged();
    }
}
