// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.contactsource.ContactDetail.Category;
import net.java.sip.communicator.service.contactsource.ContactDetail.SubCategory;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PhoneNumberDetail;

import org.jitsi.service.resources.*;
import org.jitsi.util.StringUtils;

public class TelephonyProtocolContactSourceServiceImpl extends ProtocolContactSourceServiceImpl
{
    private static final BufferedImageFuture NO_AVATAR_IMAGE = GuiActivator.getResources().
        getBufferedImage("service.gui.icons.UNAUTHORIZED_CONTACT_PHOTO");

    public TelephonyProtocolContactSourceServiceImpl()
    {
        super(null, null);
    }

    @Override
    protected ProtocolCQuery createQuery(String queryString, int contactCount)
    {
        return new TelephonyProtocolCQuery(queryString, contactCount);
    }

    @Override
    public String getDisplayName()
    {
        return GuiActivator.getResources()
                           .getI18NString("service.gui.CONTACTS_WITH_NUMBERS");
    }

    private class TelephonyProtocolCQuery extends ProtocolCQuery
    {
        /**
         * The string we are searching on, lower case so as to be able to do
         * case-insensitive look-ups
         */
        private final String mQueryString;
        private final int mContactCount;

        public TelephonyProtocolCQuery(String queryString, int contactCount)
        {
            super(queryString, contactCount);

            mQueryString = queryString == null ? null : queryString.toLowerCase();
            mContactCount = contactCount;
        }

        /**
         * Adds the result for the given group.
         */
        protected void addResultContact(MetaContact metaContact)
        {
            // Check whether this MetaContact should be displayed in this list.
            List<String> hiddenDetails =
                metaContact.getDetails(MetaContact.IS_CONTACT_HIDDEN);

            boolean isHidden = hiddenDetails.size() == 0 ?
                                        false :
                                        Boolean.valueOf(hiddenDetails.get(0));
            if (isHidden)
            {
                return;
            }

            if (getStatus() == QUERY_CANCELED)
                return;

            if(mContactCount > 0 && getQueryResultCount() > mContactCount)
                return;

            String metaDisplayName = metaContact.getDisplayName();
            if (StringUtils.isNullOrEmpty(mQueryString) ||
                metaDisplayName.toLowerCase().contains(mQueryString))
            {
                // Create the contact to display
                GenericSourceContact sourceContact =
                                           createContactForDisplay(metaContact);

                if (sourceContact != null)
                {
                    addQueryResult(sourceContact);
                }
            }
        }

        /**
         * Create a contact to display.  Returns the contact or null, if the
         * contact is not appropriate to display
         *
         * @param metaContact The parent meta contact
         * @return The contact to display, or null if it is not suitable
         */
        private GenericSourceContact
                                createContactForDisplay(MetaContact metaContact)
        {
            Class<OperationSetServerStoredContactInfo> opSetClass =
                                      OperationSetServerStoredContactInfo.class;
            List<Class<? extends OperationSet>> supportedOpSets =
                    new ArrayList<>();
            supportedOpSets.add(OperationSetBasicTelephony.class);
            List<ContactDetail> details = new ArrayList<>();

            // We keep a set of numbers that we've seen already - we don't want
            // to duplicate a detail just because it's of a different type.
            Set<String> numbersSeen = new HashSet<>();

            // For each contact that supports the server stored info, find the
            // phone numbers that it has.
            List<Contact> subContacts =
                           metaContact.getContactsForOperationSet(opSetClass);
            for (Contact contact : subContacts)
            {
                OperationSetServerStoredContactInfo opSet =
                      contact.getProtocolProvider().getOperationSet(opSetClass);

                Iterator<PhoneNumberDetail> contactNumberDetails = opSet.
                     getDetailsAndDescendants(contact, PhoneNumberDetail.class);

                while (contactNumberDetails.hasNext())
                {
                    PhoneNumberDetail phoneDetail = contactNumberDetails.next();
                    String phoneNumber = phoneDetail.getNumber();

                    if (phoneNumber == null ||
                        !numbersSeen.add(phoneNumber))
                        // Set already contained this number - ignore it
                        continue;

                    // Otherwise, convert this number into a detail for the
                    // contact we will return
                    SubCategory[] subCategories = getSubCategories(phoneDetail);
                    ContactDetail detail = new ContactDetail(phoneNumber,
                                                             Category.Phone,
                                                             subCategories);
                    detail.setSupportedOpSets(supportedOpSets);
                    details.add(detail);
                }
            }

            SortedGenericSourceContact sourceContact = null;

            if (!details.isEmpty())
            {
                // There are telephony details, so create the contact to display
                ContactSourceService contactSource =
                                 TelephonyProtocolContactSourceServiceImpl.this;
                String displayName = metaContact.getDisplayName();
                sourceContact = new SortedGenericSourceContact(this,
                                                               contactSource,
                                                               displayName,
                                                               details);

                // Set the display details to be the IM presence of the contact
                // if it exists
                Contact imContact = metaContact.getIMContact();
                if (imContact != null)
                {
                    PresenceStatus presenceStatus = imContact.getPresenceStatus();
                    sourceContact.setPresenceStatus(presenceStatus);
                    sourceContact.setDisplayDetails(presenceStatus.getStatusName());
                }
                else
                {
                    String detail = GuiActivator.getResources().
                          getI18NString("service.gui.CONTACT_IM_NOT_SUPPORTED");
                    sourceContact.setDisplayDetails(detail);
                }

                // And set the avatar
                BufferedImageFuture avatar = metaContact.getAvatar();
                if (avatar == null)
                    avatar = NO_AVATAR_IMAGE;

                sourceContact.setImage(avatar);

                // Finally, set the favourite status:
                String favProperty = MetaContact.CONTACT_FAVORITE_PROPERTY;
                String favourite = metaContact.getDetail(favProperty);
                if (Boolean.parseBoolean(favourite))
                {
                    sourceContact.setData(favProperty, favourite);
                }
            }

            return sourceContact;
        }

        /**
         * Translate the type of the phone number detail into the relevant sub-
         * category
         *
         * @param detail the detail to inspect
         * @return the appropriate array of sub-categories for the detail
         */
        private SubCategory[] getSubCategories(PhoneNumberDetail detail)
        {
            SubCategory subCategory = null;

            if (detail instanceof ServerStoredDetails.FaxDetail)
            {
                subCategory = SubCategory.Fax;
            }
            else if (detail instanceof PersonalContactDetails.HomePhoneDetail)
            {
                subCategory = SubCategory.Home;
            }
            else if (detail instanceof ServerStoredDetails.MobilePhoneDetail)
            {
                subCategory = SubCategory.Mobile;
            }
            else if (detail instanceof ServerStoredDetails.WorkPhoneDetail)
            {
                subCategory = SubCategory.Work;
            }

            return new ContactDetail.SubCategory[]{subCategory};
        }
    }
}
