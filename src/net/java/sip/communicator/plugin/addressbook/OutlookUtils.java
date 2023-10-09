// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import static net.java.sip.communicator.plugin.addressbook.calendar.TimeZoneList.*;

import java.net.*;
import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.plugin.addressbook.calendar.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;

/**
 * A set of utilities for working with Outlook
 */
public class OutlookUtils
{
    // Folder types - indicate where to get properties from
    public static final int FOLDER_TYPE_CONTACTS    = 0;
    public static final int FOLDER_TYPE_APPOINTMENT = 1;
    public static final int FOLDER_TYPE_MEETING     = 2;

    /**
     * The string used to define a contact as a favorite in the contact source.
     */
    public static final String CONTACT_FAVORITE_STATUS = "client_favourite_status";

    /**
     * The string we used to use to define contacts as favourite, retained to
     * maintain back compatibility
     */
    public static final String CONTACT_OLD_FAV_STATUS;
    static
    {
        String value;

        try
        {
            value = AddressBookProtocolActivator.getResources().
                                   getI18NString("plugin.addressbook.FAVORITE");
        }
        catch (Exception e)
        {
            // Only occurs during UTs so not a problem
            value = null;
        }

        CONTACT_OLD_FAV_STATUS = value;
    }

    /**
     * The string used to define an Accession UID in the contact source.
     */
    public static final String ACCESSION_UID = "client_contact_id:";

    /**
     * The string used to as a contact's name when all name fields are blank.
     */
    public static final String CONTACT_UNKNOWN_NAME;

    static
    {
        String value;

        try
        {
            value = AddressBookProtocolActivator.getResources().
                                           getI18NString("service.gui.UNKNOWN");
        }
        catch (Exception e)
        {
            // Only occurs during UTs so not a problem
            value = null;
        }

        CONTACT_UNKNOWN_NAME = value;
    }

    /**
     * The first email address in Outlook
     */
    public static final int dispidEmail1EmailAddress = 12;

    /**
     * The second email address in Outlook
     */
    public static final int dispidEmail2EmailAddress = 13;

    /**
     * The third email address in Outlook
     */
    public static final int dispidEmail3EmailAddress = 14;

    /**
     * The object type of a mailuser in the Address Book of Microsoft Outlook.
     */
    public static final long MAPI_MAILUSER = 0x00000006;

    public static final long MAPI_INIT_VERSION = 0;

    public static final long MAPI_MULTITHREAD_NOTIFICATIONS = 0x00000001;

    /**
     * The flag which signals that MAPI strings should be returned in the
     * Unicode character set.
     */
    public static final long MAPI_UNICODE = 0x80000000;

    /**
     * The IDs of the properties of <tt>MAPI_MAILUSER</tt> which are to be
     * queried by the <tt>MsOutlookAddrBoo kContactQuery</tt> instances.
     */
    public static final long[] MAPI_MAILUSER_PROP_IDS
        = new long[]
        {
            0x3001 /* PR_DISPLAY_NAME */,
            0x3003 /* PR_EMAIL_ADDRESS */,
            0x3A06 /* PR_GIVEN_NAME */,
            0x3A44 /* PR_MIDDLE_NAME */,
            0x3A11 /* PR_SURNAME */,
            0x3A08 /* PR_BUSINESS_TELEPHONE_NUMBER */,
            0x3A1B /* PR_BUSINESS2_TELEPHONE_NUMBER */,
            0x3A09 /* PR_HOME_TELEPHONE_NUMBER */,
            0x3A2F /* PR_HOME2_TELEPHONE_NUMBER */,
            0x3A1C /* PR_MOBILE_TELEPHONE_NUMBER */,
            0x3A1F /* PR_OTHER_TELEPHONE_NUMBER */,
            0x0FFE /* PR_OBJECT_TYPE */,
            0x00008084 /* dispidEmail1OriginalDisplayName */,
            0x00008094 /* dispidEmail2OriginalDisplayName */,
            0x000080A4 /* dispidEmail3OriginalDisplayName */,
            0x3A16 /* PR_COMPANY_NAME */,
            0x0FFF /* PR_ORIGINAL_ENTRYID */,
            0x3A24 /* dispidFax1EmailAddress */,
            0x3A25 /* dispidFax2EmailAddress */,
            0x3A23 /* dispidFax3EmailAddress */,
            0x3A4F /* PR_NICKNAME */,
            0x3A45 /* PR_DISPLAY_NAME_PREFIX */,
            0x3A50 /* PR_PERSONAL_HOME_PAGE */,
            0x3A51 /* PR_BUSINESS_HOME_PAGE */,
            0x3A17 /* PR_TITLE */,
            0x00008062 /* dispidInstMsg */,
            0x00008046, // PR_BUSINESS_ADDRESS_CITY
            0x00008049, // PR_BUSINESS_ADDRESS_COUNTRY
            0x00008048, // PR_BUSINESS_ADDRESS_POSTAL_CODE
            0x00008047, // PR_BUSINESS_ADDRESS_STATE_OR_PROVINCE
            0x00008045, // PR_BUSINESS_ADDRESS_STREET
            0x3A59, // PR_HOME_ADDRESS_CITY
            0x3A5A, // PR_HOME_ADDRESS_COUNTRY
            0x3A5B, // PR_HOME_ADDRESS_POSTAL_CODE
            0x3A5C, // PR_HOME_ADDRESS_STATE_OR_PROVINCE
            0x3A5D, // PR_HOME_ADDRESS_STREET
            0x0000801A, // dispidHomeAddress
            0x0000801B, // dispidWorkAddress
            0x0000804F, // dispidContactUserField1
            0x00008050, // dispidContactUserField2
            0x00008051, // dispidContactUserField3
            0x00008052, // dispidContactUserField4
            0x00008005, // dispidFileUnder
            0x00008006,  // dispidFileUnderId
            0x00008540, // pidLidPropertyDefinitionStream
            0x0E09 //PidTagParentEntryId
        };

    /**
     * The id of the <tt>PR_ATTACHMENT_CONTACTPHOTO</tt> MAPI property.
     */
    public static final long PR_ATTACHMENT_CONTACTPHOTO = 0x7FFF;

    /**
     * The index of the id of the <tt>PR_BUSINESS_TELEPHONE_NUMBER</tt> property
     * in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_BUSINESS_TELEPHONE_NUMBER = 5;

    /**
     * The index of the id of the <tt>PR_BUSINESS2_TELEPHONE_NUMBER</tt>
     * property in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_BUSINESS2_TELEPHONE_NUMBER = 6;

    /**
     * The company name for this contact
     */
    public static final int PR_COMPANY_NAME = 15;

    /**
     * The index of the id of the <tt>PR_DISPLAY_NAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_DISPLAY_NAME = 0;

    /**
     * The index of the id of the <tt>PR_EMAIL_ADDRESS</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_EMAIL_ADDRESS = 1;

    /**
     * The index of the id of the <tt>PR_GIVEN_NAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_GIVEN_NAME = 2;

    /**
     * The index of the id of the <tt>PR_HOME_TELEPHONE_NUMBER</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_HOME_TELEPHONE_NUMBER = 7;

    /**
     * The index of the id of the <tt>PR_HOME2_TELEPHONE_NUMBER</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_HOME2_TELEPHONE_NUMBER = 8;

    /**
     * The index of the id of the <tt>PR_MIDDLE_NAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_MIDDLE_NAME = 3;

    /**
     * The index of the id of the <tt>PR_MOBILE_TELEPHONE_NUMBER</tt> property
     * in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_MOBILE_TELEPHONE_NUMBER = 9;

    /**
     * The index of the id of the <tt>PR_OTHER_TELEPHONE_NUMBER</tt> property
     * in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_OTHER_TELEPHONE_NUMBER = 10;

    /**
     * The index of the id of the <tt>PR_OBJECT_TYPE</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_OBJECT_TYPE = 11;

    /**
     * The index of the id of the <tt>PR_SURNAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_SURNAME = 4;

    /**
     * The index of the id of the <tt>PR_ORIGINAL_ENTRYID</tt> property
     * in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    public static final int PR_ORIGINAL_ENTRYID = 16;

    /**
     * The index of the 1st fax telephone number (business fax).
     */
    public static final int dispidFax1EmailAddress = 17;

    /**
     * The index of the 2nd fax telephone number (home fax).
     */
    public static final int dispidFax2EmailAddress = 18;

    /**
     * The index of the 3rd fax telephone number (other fax).
     */
    public static final int dispidFax3EmailAddress = 19;

    /**
     * The index of the nickname.
     */
    public static final int PR_NICKNAME = 20;

    /**
     * The index of the name prefix.
     */
    public static final int PR_DISPLAY_NAME_PREFIX = 21;

    /**
     * The index of the personnal home page
     */
    public static final int PR_PERSONAL_HOME_PAGE = 22;

    /**
     * The index of the business home page
     */
    public static final int PR_BUSINESS_HOME_PAGE = 23;

    /**
     * The index of the job title.
     */
    public static final int PR_TITLE = 24;

    /**
     * The index of the instant messaging address.
     */
    public static final int dispidInstMsg = 25;

    /**
     * The index of the business city of the postal address.
     */
    public static final int PR_BUSINESS_ADDRESS_CITY = 26;

    /**
     * The index of the business region/country of the postal address.
     */
    public static final int PR_BUSINESS_ADDRESS_COUNTRY = 27;

    /**
     * The index of the business postal code of the postal address.
     */
    public static final int PR_BUSINESS_ADDRESS_POSTAL_CODE = 28;

    /**
     * The index of the business state or province of the postal address.
     */
    public static final int PR_BUSINESS_ADDRESS_STATE_OR_PROVINCE = 29;

    /**
     * The index of the business street of the postal address.
     */
    public static final int PR_BUSINESS_ADDRESS_STREET = 30;

    /**
     * The index of the home city of the postal address.
     */
    public static final int PR_HOME_ADDRESS_CITY = 31;

    /**
     * The index of the home country of the postal address.
     */
    public static final int PR_HOME_ADDRESS_COUNTRY = 32;

    /**
     * The index of the home postal code of the postal address.
     */
    public static final int PR_HOME_ADDRESS_POSTAL_CODE = 33;

    /**
     * The index of the home state or province of the postal address.
     */
    public static final int PR_HOME_ADDRESS_STATE_OR_PROVINCE = 34;

    /**
     * The index of the home street of the postal address.
     */
    public static final int PR_HOME_ADDRESS_STREET = 35;

    /**
     * The index of the display for the home postal address.
     */
    public static final int dispidHomeAddress = 36;

    /**
     * The index of the display for the work postal address.
     */
    public static final int dispidWorkAddress = 37;

    /**
     * The index of the first user-field
     */
    public static final int dispidContactUser1= 38;

    /**
     * The index of the second user-field
     */
    public static final int dispidContactUser2= 39;

    /**
     * The index of the third user-field
     */
    public static final int dispidContactUser3= 40;

    /**
     * The index of the fourth user-field
     */
    public static final int dispidContactUser4= 41;

    /**
     * The index of the File Under property
     */
    public static final int dispidFileUnder=42;

    /**
     * The index of the File Under Id property
     */
    public static final int dispidFileUnderId=43;

    /**
     * The index of the user-defined fields property tag
     */
    public static final int pidLidPropertyDefinitionStream = 44;

    /**
     * The index of the parent entry ID tag
     */
    public static final int pidTagParentEntryId = 45;

    /**
     * The property used for storing the AccessionID in Outlook
     */
    public static final int accessionIDProp = dispidContactUser1;

    /**
     * The property used for storing the Accession favourite status in Outlook
     */
    public static final int accessionFavoriteProp = dispidContactUser2;

    /**
     * The indexes in {@link #MAPI_MAILUSER_PROP_IDS} of the property IDs which
     * are to be represented in an Outlook contact
     */
    public static final int[] CONTACT_DETAIL_PROP_INDEXES
        = new int[]
        {
            PR_EMAIL_ADDRESS,
            PR_GIVEN_NAME,
            PR_MIDDLE_NAME,
            PR_SURNAME,
            PR_BUSINESS_TELEPHONE_NUMBER,
            PR_BUSINESS2_TELEPHONE_NUMBER,
            PR_HOME_TELEPHONE_NUMBER,
            PR_HOME2_TELEPHONE_NUMBER,
            PR_MOBILE_TELEPHONE_NUMBER,
            PR_OTHER_TELEPHONE_NUMBER,
            dispidEmail1EmailAddress,
            dispidEmail2EmailAddress,
            dispidEmail3EmailAddress,
            PR_COMPANY_NAME,
            dispidFax1EmailAddress,
            dispidFax2EmailAddress,
            dispidFax3EmailAddress,
            PR_NICKNAME,
            PR_DISPLAY_NAME_PREFIX,
            PR_PERSONAL_HOME_PAGE,
            PR_BUSINESS_HOME_PAGE,
            PR_TITLE,
            dispidInstMsg,
            PR_BUSINESS_ADDRESS_CITY,
            PR_BUSINESS_ADDRESS_COUNTRY,
            PR_BUSINESS_ADDRESS_POSTAL_CODE,
            PR_BUSINESS_ADDRESS_STATE_OR_PROVINCE,
            PR_BUSINESS_ADDRESS_STREET,
            PR_HOME_ADDRESS_CITY,
            PR_HOME_ADDRESS_COUNTRY,
            PR_HOME_ADDRESS_POSTAL_CODE,
            PR_HOME_ADDRESS_STATE_OR_PROVINCE,
            PR_HOME_ADDRESS_STREET,
            dispidHomeAddress,
            dispidWorkAddress,
            dispidContactUser1,
            dispidContactUser2,
            dispidContactUser3,
            dispidContactUser4,
            dispidFileUnder
        };

    /**
     * The indexes in {@link #MAPI_MAILUSER_PROP_IDS} of the property IDs which
     * are to be represented in an Outlook contact and is displayable to the
     * user.
     */
    public static final int[] DISPLAYABLE_CONTACT_DETAIL_PROP_INDEXES
        = new int[]
        {
            PR_EMAIL_ADDRESS,
            PR_GIVEN_NAME,
            PR_MIDDLE_NAME,
            PR_SURNAME,
            PR_BUSINESS_TELEPHONE_NUMBER,
            PR_BUSINESS2_TELEPHONE_NUMBER,
            PR_HOME_TELEPHONE_NUMBER,
            PR_HOME2_TELEPHONE_NUMBER,
            PR_MOBILE_TELEPHONE_NUMBER,
            PR_OTHER_TELEPHONE_NUMBER,
            dispidEmail1EmailAddress,
            dispidEmail2EmailAddress,
            dispidEmail3EmailAddress,
            PR_COMPANY_NAME,
            dispidFax1EmailAddress,
            dispidFax2EmailAddress,
            dispidFax3EmailAddress,
            PR_NICKNAME,
            PR_DISPLAY_NAME_PREFIX,
            PR_TITLE,
            dispidInstMsg,
            PR_BUSINESS_ADDRESS_CITY,
            PR_BUSINESS_ADDRESS_COUNTRY,
            PR_BUSINESS_ADDRESS_POSTAL_CODE,
            PR_BUSINESS_ADDRESS_STATE_OR_PROVINCE,
            PR_BUSINESS_ADDRESS_STREET,
            PR_HOME_ADDRESS_CITY,
            PR_HOME_ADDRESS_COUNTRY,
            PR_HOME_ADDRESS_POSTAL_CODE,
            PR_HOME_ADDRESS_STATE_OR_PROVINCE,
            PR_HOME_ADDRESS_STREET,
            dispidHomeAddress,
            dispidWorkAddress
        };

    /**
     * The indexes in {@link #MAPI_MAILUSER_PROP_IDS} of the property IDs which
     * represent an identifier which can be used for telephony or persistent
     * presence.
     */
    public static final int[] CONTACT_OPERATION_SET_ABLE_PROP_INDEXES
        = new int[]
        {
            PR_EMAIL_ADDRESS,
            PR_BUSINESS_TELEPHONE_NUMBER,
            PR_BUSINESS2_TELEPHONE_NUMBER,
            PR_HOME_TELEPHONE_NUMBER,
            PR_HOME2_TELEPHONE_NUMBER,
            PR_MOBILE_TELEPHONE_NUMBER,
            PR_OTHER_TELEPHONE_NUMBER,
            dispidEmail1EmailAddress,
            dispidEmail2EmailAddress,
            dispidEmail3EmailAddress,
            dispidFax1EmailAddress,
            dispidFax2EmailAddress,
            dispidFax3EmailAddress,
            dispidInstMsg
        };

    /**
     * A list of name fields that should be filled when creating a new contact
     * in Outlook
     */
    public static final long[] CONTACT_NAME_FIELDS
        = new long[]
        {
            MAPI_MAILUSER_PROP_IDS[PR_DISPLAY_NAME],
            0x0037, // PR_SUBJECT
            0x803F, // Do not know, but set by the MFCMAPI application.
            0x0E1D, // PR_NORMALIZED_SUBJECT
            MAPI_MAILUSER_PROP_IDS[PR_NICKNAME]
        };

    /**
     * The MAPI property ID of the fields that we are interested in for Calendar
     * information
     */
    public static final long[] CALENDAR_FIELDS = new long[]
    {
        0x820D, // Appointment start date
        0x820E, // Appointment end date
        0x8205, // Busy or free
        0x8223, // Recurring or not
        0x8216, // Recurrent pattern (daily, weekly...)
        0x8218, // Accepted or not
        0x8232, // Human readable pattern
        0x8234, // Timezone for the meeting
        0x0E09  // PidTagParentEntryId
    };

    // Indexes into the CALENDAR_FIELDS item:
    public static final int IDX_CALENDAR_START_DATE       = 0;
    public static final int IDX_CALENDAR_END_DATE         = 1;
    public static final int IDX_CALENDAR_BUSY_STATE       = 2;
    public static final int IDX_CALENDAR_RECURRING        = 3;
    public static final int IDX_CALENDAR_REC_PATTERN      = 4;
    public static final int IDX_CALENDAR_ACCEPTED         = 5;
    public static final int IDX_CALENDAR_READABLE_PATTERN = 6;
    public static final int IDX_CALENDAR_TIMEZONE         = 7;
    public static final int IDX_CALENDAR_PARENT_ID        = 8;

    /**
     * Regex to capture time zones of form "UTC/GMT +/- XX:XX"
     */
    private static final Pattern GMT_UTC_PATTERN = Pattern.compile(
             "(?:utc|gmt)" +                 // UTC or GMT
             "[\\s]*" +                      // Any whitespace
            "(\\+|-)" +                      // + or -
            "[\\s]*" +                       // more whitespace
            "([0-9]|0[0-9]|1[0-9]|2[0-3])" + // number of hours
            ":" +                            // colon separating hh:mm
            "([0-5][0-9])",                  // number of minutes
         Pattern.CASE_INSENSITIVE);

    /**
     * Returns the Generic Detail for the given Outlook property index and value
     *
     * @param propIndex The index of the Outlook property
     * @param propValue The value of the Outlook property
     * @return the Generic Detail for this property
     */
    public static GenericDetail getDetail(int propIndex, String propValue)
    {
        switch (propIndex)
        {
        case PR_GIVEN_NAME:
            return new ServerStoredDetails.FirstNameDetail(propValue);
        case PR_MIDDLE_NAME:
            return new ServerStoredDetails.MiddleNameDetail(propValue);
        case PR_SURNAME:
            return new ServerStoredDetails.LastNameDetail(propValue);
        case PR_NICKNAME:
            return new ServerStoredDetails.NicknameDetail(propValue);
        case PR_DISPLAY_NAME_PREFIX:
            return new ServerStoredDetails.DisplayNameDetail(propValue);
        case PR_PERSONAL_HOME_PAGE:
            try
            {
                URL url = new URL(propValue);
                return new ServerStoredDetails.WebPageDetail(url);
            }
            catch (MalformedURLException e)
            {
                return null;
            }
        case dispidContactUser1:
            return new ServerStoredDetails.GenericDetail("UserField1", propValue);
        case dispidContactUser2:
            return new ServerStoredDetails.GenericDetail("UserField2", propValue);
        case dispidContactUser3:
            return new ServerStoredDetails.GenericDetail("UserField3", propValue);
        case dispidContactUser4:
            return new ServerStoredDetails.GenericDetail("UserField4", propValue);
        case pidLidPropertyDefinitionStream:
            return new ServerStoredDetails.GenericDetail("PropertyDefinitionStream", propValue);
        case dispidFileUnder:
            return new ServerStoredDetails.GenericDetail("FileUnder", propValue);
        case dispidFileUnderId:
            return new ServerStoredDetails.GenericDetail("FileUnderId", propValue);
        case PR_COMPANY_NAME:
            return new ServerStoredDetails.WorkOrganizationNameDetail(propValue);
        case PR_BUSINESS_HOME_PAGE:
            try
            {
                URL url = new URL(propValue);
                return new ServerStoredDetails.WorkPageDetail(url);
            }
            catch (MalformedURLException e)
            {
                return null;
            }
        case PR_TITLE:
            return new PersonalContactDetails.WorkTitleDetail(propValue);
        case dispidEmail1EmailAddress:
            return new PersonalContactDetails.EmailAddress1Detail(propValue);
        case dispidEmail2EmailAddress:
            return new PersonalContactDetails.EmailAddress2Detail(propValue);
        case dispidEmail3EmailAddress:
            return null;
        case PR_EMAIL_ADDRESS:
            return null;
        case PR_BUSINESS_TELEPHONE_NUMBER:
            return new ServerStoredDetails.WorkPhoneDetail(propValue);
        case PR_BUSINESS2_TELEPHONE_NUMBER:
            return null;
        case PR_HOME_TELEPHONE_NUMBER:
            return new PersonalContactDetails.HomePhoneDetail(propValue);
        case PR_HOME2_TELEPHONE_NUMBER:
            return null;
        case PR_MOBILE_TELEPHONE_NUMBER:
            return new ServerStoredDetails.MobilePhoneDetail(propValue);
        case PR_OTHER_TELEPHONE_NUMBER:
            return new PersonalContactDetails.OtherPhoneDetail(propValue);
        case dispidFax1EmailAddress:
            return new ServerStoredDetails.FaxDetail(propValue);
        case dispidFax2EmailAddress:
            return null;
        case dispidFax3EmailAddress:
            return null;
        case dispidInstMsg:
            return new PersonalContactDetails.IMDetail(propValue, ProtocolNames.JABBER);
        case PR_BUSINESS_ADDRESS_CITY:
            return new ServerStoredDetails.WorkCityDetail(propValue);
        case PR_BUSINESS_ADDRESS_COUNTRY:
            return new PersonalContactDetails.WorkCountryDetail(propValue);
        case PR_BUSINESS_ADDRESS_POSTAL_CODE:
            return new ServerStoredDetails.WorkPostalCodeDetail(propValue);
        case PR_BUSINESS_ADDRESS_STATE_OR_PROVINCE:
            return new ServerStoredDetails.WorkProvinceDetail(propValue);
        case PR_BUSINESS_ADDRESS_STREET:
            return new ServerStoredDetails.WorkAddressDetail(propValue);
        case PR_HOME_ADDRESS_CITY:
            return new ServerStoredDetails.CityDetail(propValue);
        case PR_HOME_ADDRESS_COUNTRY:
            return new PersonalContactDetails.CountryDetail(propValue);
        case PR_HOME_ADDRESS_POSTAL_CODE:
            return new ServerStoredDetails.PostalCodeDetail(propValue);
        case PR_HOME_ADDRESS_STATE_OR_PROVINCE:
            return new ServerStoredDetails.ProvinceDetail(propValue);
        case PR_HOME_ADDRESS_STREET:
            return new ServerStoredDetails.AddressDetail(propValue);
        case dispidHomeAddress:
            return new ServerStoredDetails.GenericDetail("Full Home Address", propValue);
        case dispidWorkAddress:
            return new ServerStoredDetails.GenericDetail("Full Work Address", propValue);
        default:
            return null;
        }
    }

    /**
     * Gets the set of <tt>ContactDetail</tt> labels to be assigned to a
     * property specified by its index in {@link #MAPI_MAILUSER_PROP_IDS}.
     *
     * @param propIndex the index in <tt>MAPI_MAILUSER_PROP_IDS</tt> of the
     * property to get the <tt>ContactDetail</tt> labels of
     * @return the set of <tt>ContactDetail</tt> labels to be assigned to the
     * property specified by its index in <tt>MAPI_MAILUSER_PROP_IDS</tt>
     */
    public static ContactDetail.SubCategory[] getSubCategories(int propIndex)
    {
        switch (propIndex)
        {
        case PR_GIVEN_NAME:
        case PR_MIDDLE_NAME:
        case PR_COMPANY_NAME:
        case dispidFileUnder:
        case dispidFileUnderId:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Name
                        };
        case PR_SURNAME:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.LastName
                        };
        case PR_NICKNAME:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Nickname
                        };
        case PR_BUSINESS2_TELEPHONE_NUMBER:
        case PR_BUSINESS_TELEPHONE_NUMBER:
        case dispidEmail2EmailAddress:
        case PR_EMAIL_ADDRESS:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Work
                        };
        case PR_HOME2_TELEPHONE_NUMBER:
        case PR_HOME_TELEPHONE_NUMBER:
        case dispidEmail1EmailAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Home
                        };
        case PR_MOBILE_TELEPHONE_NUMBER:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Mobile
                        };
        case PR_OTHER_TELEPHONE_NUMBER:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Other
                        };
        case dispidFax1EmailAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Fax,
                        };
        case dispidEmail3EmailAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Other
                        };
        case PR_TITLE:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.JobTitle
                        };
        case PR_BUSINESS_ADDRESS_CITY:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Work,
                            ContactDetail.SubCategory.City
                        };
        case PR_BUSINESS_ADDRESS_COUNTRY:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Work,
                            ContactDetail.SubCategory.CountryRegion
                        };
        case PR_BUSINESS_ADDRESS_POSTAL_CODE:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Work,
                            ContactDetail.SubCategory.PostalCode
                        };
        case PR_BUSINESS_ADDRESS_STATE_OR_PROVINCE:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Work,
                            ContactDetail.SubCategory.State
                        };
        case PR_BUSINESS_ADDRESS_STREET:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Work,
                            ContactDetail.SubCategory.Street
                        };
        case PR_HOME_ADDRESS_CITY:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Home,
                            ContactDetail.SubCategory.City
                        };
        case PR_HOME_ADDRESS_COUNTRY:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Home,
                            ContactDetail.SubCategory.CountryRegion
                        };
        case PR_HOME_ADDRESS_POSTAL_CODE:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Home,
                            ContactDetail.SubCategory.PostalCode
                        };
        case PR_HOME_ADDRESS_STATE_OR_PROVINCE:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Home,
                            ContactDetail.SubCategory.State
                        };
        case PR_HOME_ADDRESS_STREET:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Home,
                            ContactDetail.SubCategory.Street
                        };
        case dispidHomeAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Home,
                        };
        case dispidWorkAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Work,
                        };
        case dispidContactUser1:
        case dispidContactUser2:
        case dispidContactUser3:
        case dispidContactUser4:
        case pidLidPropertyDefinitionStream:
            return
                new ContactDetail.SubCategory[]
                    {
                        ContactDetail.SubCategory.Other,
                    };
        default:
            return null;
        }
    }

    /**
     * Determines whether a specific index in {@link #MAPI_MAILUSER_PROP_IDS}
     * stands for a property with a phone number value.
     *
     * @param propIndex the index in <tt>MAPI_MAILUSER_PROP_IDS</tt> of the
     * property to check
     * @return <tt>true</tt> if <tt>propIndex</tt> stands for a property with a
     * phone number value; otherwise, <tt>false</tt>
     */
    public static boolean isPhoneNumber(int propIndex)
    {
        switch (propIndex)
        {
        case PR_BUSINESS2_TELEPHONE_NUMBER:
        case PR_BUSINESS_TELEPHONE_NUMBER:
        case PR_HOME2_TELEPHONE_NUMBER:
        case PR_HOME_TELEPHONE_NUMBER:
        case PR_MOBILE_TELEPHONE_NUMBER:
            return true;
        default:
            return false;
        }
    }

    /**
     * Determine whether there is an empty user field that we can use for
     * storing data in Outlook
     *
     * @param props the list of properties from Outlook
     * @return whether there is a spare user field
     */
    public static boolean hasEmptyUserField(Object[] props)
    {
        // Determine whether we have seen this contact before by checking
        // the user fields for the presence of an Accession contact ID
        int userPropIndexes[] = {dispidContactUser1,
                                 dispidContactUser2,
                                 dispidContactUser3,
                                 dispidContactUser4};

        boolean hasEmptyUserField = false;
        for (int propIndex : userPropIndexes)
        {
            if (props[propIndex] == null || props[propIndex].equals(""))
            {
                hasEmptyUserField = true;
                break;
            }
        }

        return hasEmptyUserField;
    }

    /**
     * Determines whether a list of properties contain any details that we
     * support for contacts
     *
     * @param props the list of properties from Outlook
     * @return whether this list of properties contains contact details
     */
    public static boolean hasContactDetails(Object[] props)
    {
        for (int contactPropId : DISPLAYABLE_CONTACT_DETAIL_PROP_INDEXES)
        {
            if (props[contactPropId] != null &&
                props[contactPropId] instanceof String)
            {
                GenericDetail propDetail = getDetail(contactPropId,
                                                  (String)props[contactPropId]);

                // Check the property detail is not null
                if (propDetail != null && propDetail.getDetailValue() != null)
                {
                    // Check the value of the detail is not null or empty
                    if (propDetail.getDetailValue() instanceof String &&
                        ((String) propDetail.getDetailValue()).length() > 0)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gets the MAPI property definition for a given contact detail
     *
     * @param contactDetail the contact contact for which to look up the
     * property definition
     * @return propID the MAPI property definition
     */
    public static long getPropertyForDetail(GenericDetail contactDetail)
    {
        long propID = 0;
        if (contactDetail == null)
        {
            // We only care about string property values as this is all we can
            // write to Outlook
            return propID;
        }

        if (contactDetail instanceof ServerStoredDetails.FirstNameDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_GIVEN_NAME];
        }
        else if (contactDetail instanceof ServerStoredDetails.MiddleNameDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_MIDDLE_NAME];
        }
        else if (contactDetail instanceof ServerStoredDetails.LastNameDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_SURNAME];
        }
        else if (contactDetail instanceof ServerStoredDetails.NicknameDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_NICKNAME];
        }
        else if (contactDetail instanceof ServerStoredDetails.DisplayNameDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_DISPLAY_NAME_PREFIX];
        }
        else if (contactDetail instanceof ServerStoredDetails.WebPageDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_PERSONAL_HOME_PAGE];
        }
        else if (contactDetail.getDetailDisplayName().equals("UserField1"))
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidContactUser1];
        }
        else if (contactDetail.getDetailDisplayName().equals("UserField2"))
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidContactUser2];
        }
        else if (contactDetail.getDetailDisplayName().equals("UserField3"))
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidContactUser3];
        }
        else if (contactDetail.getDetailDisplayName().equals("UserField4"))
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidContactUser4];
        }
        else if (contactDetail.getDetailDisplayName().equals("PropertyDefinitionStream"))
        {
            propID = MAPI_MAILUSER_PROP_IDS[pidLidPropertyDefinitionStream];
        }
        else if (contactDetail.getDetailDisplayName().equals("FileUnder"))
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidFileUnder];
        }
        else if (contactDetail.getDetailDisplayName().equals("FileUnderId"))
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidFileUnderId];
        }
        else if (contactDetail instanceof ServerStoredDetails.WorkOrganizationNameDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_COMPANY_NAME];
        }
        else if (contactDetail instanceof ServerStoredDetails.WorkPageDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_BUSINESS_HOME_PAGE];
        }
        else if (contactDetail instanceof PersonalContactDetails.WorkTitleDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_TITLE];
        }
        else if (contactDetail instanceof PersonalContactDetails.EmailAddress1Detail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidEmail1EmailAddress];
        }
        else if (contactDetail instanceof PersonalContactDetails.EmailAddress2Detail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidEmail2EmailAddress];
        }
        else if (contactDetail instanceof ServerStoredDetails.WorkPhoneDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_BUSINESS_TELEPHONE_NUMBER];
        }
        else if (contactDetail instanceof PersonalContactDetails.HomePhoneDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_HOME_TELEPHONE_NUMBER];
        }
        else if (contactDetail instanceof ServerStoredDetails.MobilePhoneDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_MOBILE_TELEPHONE_NUMBER];
        }
        else if (contactDetail instanceof PersonalContactDetails.OtherPhoneDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_OTHER_TELEPHONE_NUMBER];
        }
        else if (contactDetail instanceof ServerStoredDetails.FaxDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidFax1EmailAddress];
        }
        else if (contactDetail instanceof PersonalContactDetails.IMDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidInstMsg];
        }
        else if (contactDetail instanceof ServerStoredDetails.WorkCityDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_BUSINESS_ADDRESS_CITY];
        }
        else if (contactDetail instanceof PersonalContactDetails.WorkCountryDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_BUSINESS_ADDRESS_COUNTRY];
        }
        else if (contactDetail instanceof ServerStoredDetails.WorkPostalCodeDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_BUSINESS_ADDRESS_POSTAL_CODE];
        }
        else if (contactDetail instanceof ServerStoredDetails.WorkProvinceDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_BUSINESS_ADDRESS_STATE_OR_PROVINCE];
        }
        else if (contactDetail instanceof ServerStoredDetails.WorkAddressDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_BUSINESS_ADDRESS_STREET];
        }
        else if (contactDetail instanceof ServerStoredDetails.CityDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_HOME_ADDRESS_CITY];
        }
        else if (contactDetail instanceof PersonalContactDetails.CountryDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_HOME_ADDRESS_COUNTRY];
        }
        else if (contactDetail instanceof ServerStoredDetails.PostalCodeDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_HOME_ADDRESS_POSTAL_CODE];
        }
        else if (contactDetail instanceof ServerStoredDetails.ProvinceDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_HOME_ADDRESS_STATE_OR_PROVINCE];
        }
        else if (contactDetail instanceof ServerStoredDetails.AddressDetail)
        {
            propID = MAPI_MAILUSER_PROP_IDS[PR_HOME_ADDRESS_STREET];
        }
        else if (contactDetail.getDetailDisplayName().equals("Full Home Address"))
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidHomeAddress];
        }
        else if (contactDetail.getDetailDisplayName().equals("Full Work Address"))
        {
            propID = MAPI_MAILUSER_PROP_IDS[dispidWorkAddress];
        }

        return propID;
    }

    /**
     * Converts windows time in minutes from 1/1/1601 to <tt>Date</tt> object.
     * @param time the number of minutes from 1/1/1601
     * @return the <tt>Date</tt> object
     */
    public static Date windowsTimeToDateObject(long time)
    {
        // Date.parse("1/1/1601") == 11644473600000L
        long date = time * 60000 - 11644473600000L;

        return new Date(date);
    }

    /**
     * Converts the timezone description returned by Outlook to the appropriate
     * Java TimeZone object.
     *
     * @param outlookString the time zone string received from Outlook
     * @return the time zone for the given Outlook description
     */
    public static TimeZone parseMicrosoftTimezone(String outlookString)
    {
        String timeZoneId = null;

        // Null shouldn't happen.  But don't fall over if it does
        if (outlookString == null)
        {
            timeZoneId = TimeZone.getDefault().getID();
        }

        // We expect the outlookString to be a "Windows Display" string.  So
        // look there first for the appropriate Olson string.
        if (timeZoneId == null)
        {
            timeZoneId = getWindowsDisplayToOlsonMap().get(outlookString);
        }

        // We've seen that the outlookString can be a Olson string occasionally.
        // So if we've not found it in the Windows Display map, then check the
        // time zones
        if (timeZoneId == null && getAvailableTimeZones().contains(outlookString))
        {
            timeZoneId = outlookString;
        }

        // If we've failed to match it in the Windows Display and the available
        // time zones, try the "Windows Standard" strings.
        if (timeZoneId == null)
        {
            timeZoneId = getWindowsStandardToOlsonMap().get(outlookString);
        }

        // If we've still failed to understand the outlook string then we might
        // be able to get something close by examining the offset from GMT. If
        // there is one of course...
        if (timeZoneId == null)
        {
            // See if we can extract an offset from the outlook string, and
            // convert it to a time zone
            String[] possibleTzIds = fetchTimeZonesForOffset(outlookString);
            if (possibleTzIds != null && possibleTzIds.length > 0)
            {
                // Many possibilities - just guess the first.
                timeZoneId = possibleTzIds[0];
            }

            // Note that we are guessing here - we don't know this ID, so it
            // should be added to the unknown IDs
            OutlookCalendarDataHandler.addFailedTimeZoneString(outlookString,
                                                               timeZoneId);
        }

        TimeZone timeZone;
        if (timeZoneId != null)
        {
            timeZone = TimeZone.getTimeZone(timeZoneId);
        }
        else
        {
            // If we can't parse the time zone string, just use the default
            // (system time zone).  Using the system time zone ensures that
            // meetings that were created in the same time zone as the user
            // are still scheduled correctly.  Meetings in a different time
            // zone however won't be scheduled correctly.
            timeZone = TimeZone.getDefault();
        }

        return timeZone;
    }

    /**
     * Examine a timezone string from outlook, and try to extract an offset from
     * it.  If we do find an offset, then use that to get a list of possible
     * time zones that we could use
     *
     * @param outlookString The time zone string from Outlook
     * @return An array of time zone IDs, or null if none were found.
     */
    private static String[] fetchTimeZonesForOffset(String outlookString)
    {
        String[] possibleTimeZoneIds = null;

        try
        {
            Matcher matcher = GMT_UTC_PATTERN.matcher(outlookString);
            if (matcher.find(0))
            {
                boolean positive = matcher.group(1).equals("+");
                int hours = Integer.valueOf(matcher.group(2));
                int minutes = Integer.valueOf(matcher.group(3));

                int offset = ((hours * 60) + minutes) * 60_000 * (positive ? 1 : -1);
                possibleTimeZoneIds = TimeZone.getAvailableIDs(offset);
            }
        }
        catch (NumberFormatException e)
        {
            // Will never happen - the regex matching will ensure we only get
            // valid numbers.
        }

        return possibleTimeZoneIds;
    }
}
