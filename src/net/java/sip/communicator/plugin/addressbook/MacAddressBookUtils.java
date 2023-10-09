// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;
import java.util.regex.*;

import org.jitsi.util.*;

import net.java.sip.communicator.plugin.addressbook.MacAddressBookDataHandler.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.PersonalContactDetails.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;

public class MacAddressBookUtils
{
    /**
     * Starts a new native <tt>MacAddressBookDataHandler</tt> instance.
     *
     * @return a pointer to the newly-started native
     * <tt>MacAddressBookDataHandler</tt> instance
     */
    public static native long start();

    /**
     * Stops a native <tt>MacAddressBookDataHandler</tt>.
     *
     * @param ptr the pointer to the native <tt>MacAddressBookDataHandler</tt>
     * to stop
     */
    public static native void stop(long ptr);

    /**
     * Sets notifier delegate.
     *
     * @param ptr the pointer to the native
     * <tt>MacAddressBookDataHandler</tt> to set the delegate for
     * @param delegate the delegate to set
     */
    public static native void setDelegate(long ptr,
                                          NotificationsDelegate delegate);

    /**
     * Calls back to a specific <tt>PtrCallback</tt> for each <tt>ABPerson</tt>
     * found in the Address Book of Mac OS X which matches a specific
     * <tt>String</tt> query.
     *
     * @param query the <tt>String</tt> for which the Address Book of Mac OS X
     * is to be queried.
     * @param callback the <tt>PtrCallback</tt> to be notified about the
     * matching <tt>ABPerson</tt>s
     */
    public static native void foreachPerson(String query,
                                            PtrCallback callback);

    /**
     * The properties of <tt>ABPerson</tt> which are to be queried by the
     * <tt>MacOSXAddrBookContactQuery</tt> instances.
     */
    public static final long[] ABPERSON_PROPERTIES
        = new long[]
        {
            kABAIMInstantProperty(),
            kABEmailProperty(),
            kABFirstNameProperty(),
            kABFirstNamePhoneticProperty(),
            kABICQInstantProperty(),
            kABJabberInstantProperty(),
            kABLastNameProperty(),
            kABLastNamePhoneticProperty(),
            kABMiddleNameProperty(),
            kABMiddleNamePhoneticProperty(),
            kABMSNInstantProperty(),
            kABNicknameProperty(),
            kABPhoneProperty(),
            kABYahooInstantProperty(),
            kABPersonFlags(),
            kABOrganizationProperty(),
            kABMaidenNameProperty(),
            kABBirthdayProperty(),
            kABJobTitleProperty(),
            kABHomePageProperty(),
            kABURLsProperty(),
            kABCalendarURIsProperty(),
            kABAddressProperty(),
            kABOtherDatesProperty(),
            kABRelatedNamesProperty(),
            kABDepartmentProperty(),
            kABNoteProperty(),
            kABTitleProperty(),
            kABSuffixProperty()
        };

    /**
     * The index of the <tt>kABAIMInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABAIMInstantProperty = 0;

    /**
     * The index of the <tt>kABEmailProperty</tt> <tt>ABPerson</tt> property in
     * {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABEmailProperty = 1;

    /**
     * The index of the <tt>kABFirstNameProperty</tt> <tt>ABPerson</tt> property
     * in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABFirstNameProperty = 2;

    /**
     * The index of the <tt>kABFirstNamePhoneticProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABFirstNamePhoneticProperty = 3;

    /**
     * The index of the <tt>kABICQInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABICQInstantProperty = 4;

    /**
     * The index of the <tt>kABJabberInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABJabberInstantProperty = 5;

    /**
     * The index of the <tt>kABLastNameProperty</tt> <tt>ABPerson</tt> property
     * in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABLastNameProperty = 6;

    /**
     * The index of the <tt>kABLastNamePhoneticProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABLastNamePhoneticProperty = 7;

    /**
     * The index of the <tt>kABMiddleNameProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABMiddleNameProperty = 8;

    /**
     * The index of the <tt>kABMiddleNamePhoneticProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABMiddleNamePhoneticProperty = 9;

    /**
     * The index of the <tt>kABMSNInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABMSNInstantProperty = 10;

    /**
     * The index of the <tt>kABNicknameProperty</tt> <tt>ABPerson</tt> property
     * in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABNicknameProperty = 11;

    /**
     * The index of the <tt>kABOrganizationProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABOrganizationProperty = 15;

    /**
     * The index of the <tt>kABPersonFlags</tt> <tt>ABPerson</tt> property in
     * {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABPersonFlags = 14;

    /**
     * The index of the <tt>kABPhoneProperty</tt> <tt>ABPerson</tt> property in
     * {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABPhoneProperty = 12;

    /**
     * The flag which indicates that an <tt>ABRecord</tt> is to be displayed as
     * a company.
     */
    public static final long kABShowAsCompany = 1;

    /**
     * The mask which extracts the <tt>kABShowAsXXX</tt> flag from the
     * <tt>personFlags</tt> of an <tt>ABPerson</tt>.
     */
    public static final long kABShowAsMask = 7;

    /**
     * The index of the <tt>kABYahooInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABYahooInstantProperty = 13;

    /**
     * The index of the <tt>kABMaidenNameProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABMaidenNameProperty = 16;

    /**
     * The index of the <tt>kABBirthdayProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABBirthdayProperty = 17;

    /**
     * The index of the <tt>kABJobTitleProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABJobTitleProperty = 18;

    /**
     * The index of the <tt>kABHomePageProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABHomePageProperty = 19;

    /**
     * The index of the <tt>kABURLsProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABURLsProperty = 20;

    /**
     * The index of the <tt>kABCalendarURIsProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABCalendarURIsProperty = 21;

    /**
     * The index of the <tt>kABAddressProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABAddressProperty = 22;

    /**
     * The index of the <tt>kABOtherDatesProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABOtherDatesProperty = 23;

    /**
     * The index of the <tt>kABRelatedNamesProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABRelatedNamesProperty = 24;

    /**
     * The index of the <tt>kABDepartmentProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABDepartmentProperty = 25;

    /**
     * The index of the <tt>kABNoteProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABNoteProperty = 26;

    /**
     * The index of the <tt>kABTitleProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABTitleProperty = 27;

    /**
     * The index of the <tt>kABSuffixProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABSuffixProperty = 28;

    /**
     * The label strings used by Mac Address Book
     */
    public static final String[] ABLABEL_PROPERTIES
        = new String[]
        {
            kABEmailWorkLabel(),
            kABEmailHomeLabel(),
            kABAddressHomeLabel(),
            kABAddressWorkLabel(),
            kABPhoneWorkLabel(),
            kABPhoneHomeLabel(),
            kABPhoneMobileLabel(),
            kABPhoneMainLabel(),
            kABPhoneWorkFAXLabel(),
            kABHomeLabel(),
            kABWorkLabel(),
            kABOtherLabel()
        };

    /**
     * The index of the <tt>kABEmailWorkLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABEmailWorkLabel = 0;

    /**
     * The index of the <tt>kABEmailHomeLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABEmailHomeLabel = 1;

    /**
     * The index of the <tt>kABAddressHomeLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABAddressHomeLabel = 2;

    /**
     * The index of the <tt>kABAddressWorkLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABAddressWorkLabel = 3;

    /**
     * The index of the <tt>kABPhoneWorkLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABPhoneWorkLabel = 4;

    /**
     * The index of the <tt>kABPhoneHomeLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABPhoneHomeLabel = 5;

    /**
     * The index of the <tt>kABPhoneMobileLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABPhoneMobileLabel = 6;

    /**
     * The index of the <tt>kABPhoneMainLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABPhoneMainLabel = 7;

    /**
     * The index of the <tt>kABPhoneWorkFAXLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABPhoneWorkFAXLabel = 8;

    /**
     * The index of the <tt>kABHomeLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABHomeLabel = 9;

    /**
     * The index of the <tt>kABWorkLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABWorkLabel = 10;

    /**
     * The index of the <tt>kABOtherLabel</tt> label in {@link
     * #ABLABEL_PROPERTIES}.
     */
    public static final int kABOtherLabel = 11;

    /**
     * The key strings used by Mac Address Book
     */
    public static final String[] ABKEY_PROPERTIES
        = new String[]
            {
                kABAddressStreetKey(),
                kABAddressCityKey(),
                kABAddressStateKey(),
                kABAddressZIPKey(),
                kABAddressCountryKey()
            };

    /**
     * The index of the <tt>kABAddressStreetKey</tt> label in {@link
     * #ABKEY_PROPERTIES}.
     */
    public static final int kABAddressStreetKey = 0;

    /**
     * The index of the <tt>kABAddressCityKey</tt> label in {@link
     * #ABKEY_PROPERTIES}.
     */
    public static final int kABAddressCityKey = 1;

    /**
     * The index of the <tt>kABAddressStateKey</tt> label in {@link
     * #ABKEY_PROPERTIES}.
     */
    public static final int kABAddressStateKey = 2;

    /**
     * The index of the <tt>kABAddressZIPKey</tt> label in {@link
     * #ABKEY_PROPERTIES}.
     */
    public static final int kABAddressZIPKey = 3;

    /**
     * The index of the <tt>kABAddressCountryKey</tt> label in {@link
     * #ABKEY_PROPERTIES}.
     */
    public static final int kABAddressCountryKey = 4;

    /**
     * The regex which matches the superfluous parts of an <tt>ABMultiValue</tt>
     * label.
     */
    public static final Pattern LABEL_PATTERN
        = Pattern.compile(
            "kAB|Email|Phone|Label|(\\p{Punct}*)",
            Pattern.CASE_INSENSITIVE);

    public static final String HOME = ABLABEL_PROPERTIES[kABHomeLabel];

    public static final String WORK = ABLABEL_PROPERTIES[kABWorkLabel];

    public static final String OTHER = ABLABEL_PROPERTIES[kABOtherLabel];

    public static final String MOBILE = ABLABEL_PROPERTIES[kABPhoneMobileLabel];

    public static final String STREET = ABKEY_PROPERTIES[kABAddressStreetKey];

    public static final String STATE = ABKEY_PROPERTIES[kABAddressStateKey];

    public static final String POSTALCODE = ABKEY_PROPERTIES[kABAddressZIPKey];

    public static final String COUNTRY = ABKEY_PROPERTIES[kABAddressCountryKey];

    public static final String CITY = ABKEY_PROPERTIES[kABAddressCityKey];

    public static final String NICKNAME = "nickname";

    public static final String WORKFAX = ABLABEL_PROPERTIES[kABPhoneWorkFAXLabel];

    public static final List<Class<? extends GenericDetail>> addressDetails =
        Arrays.asList(ServerStoredDetails.AddressDetail.class,
                      ServerStoredDetails.CityDetail.class,
                      ServerStoredDetails.ProvinceDetail.class,
                      ServerStoredDetails.PostalCodeDetail.class,
                      PersonalContactDetails.CountryDetail.class,

                      ServerStoredDetails.WorkAddressDetail.class,
                      ServerStoredDetails.WorkCityDetail.class,
                      ServerStoredDetails.WorkProvinceDetail.class,
                      ServerStoredDetails.WorkPostalCodeDetail.class,
                      PersonalContactDetails.WorkCountryDetail.class);

    private static final List<Class<? extends GenericDetail>> multiLineDetails =
        Arrays.asList(ServerStoredDetails.AddressDetail.class,
                      ServerStoredDetails.CityDetail.class,
                      ServerStoredDetails.ProvinceDetail.class,
                      ServerStoredDetails.PostalCodeDetail.class,
                      PersonalContactDetails.CountryDetail.class,

                      ServerStoredDetails.WorkAddressDetail.class,
                      ServerStoredDetails.WorkCityDetail.class,
                      ServerStoredDetails.WorkProvinceDetail.class,
                      ServerStoredDetails.WorkPostalCodeDetail.class,
                      PersonalContactDetails.WorkCountryDetail.class,

                      PersonalContactDetails.IMDetail.class,
                      ServerStoredDetails.PhoneNumberDetail.class,
                      ServerStoredDetails.EmailAddressDetail.class);

    /**
     * Gets the <tt>imageData</tt> of a specific <tt>ABPerson</tt> instance.
     *
     * @param person the pointer to the <tt>ABPerson</tt> instance to get the
     * <tt>imageData</tt> of
     * @return the <tt>imageData</tt> of the specified <tt>ABPerson</tt>
     * instance
     */
    public static native byte[] ABPerson_imageData(long person);

    /**
     * Gets the values of a specific set of <tt>ABRecord</tt> properties for a
     * specific <tt>ABRecord</tt> instance.
     *
     * @param record the pointer to the <tt>ABRecord</tt> to get the property
     * values of
     * @param properties the set of <tt>ABRecord</tt> properties to get the
     * values of
     * @return the values of the specified set of <tt>ABRecord</tt> properties
     * for the specified <tt>ABRecord</tt> instance
     */
    public static native Object[] ABRecord_valuesForProperties(
            long record,
            long[] properties);

    /**
     * Returns the unique id of a record.
     * @param record the record which id is retrieved.
     * @return the record id.
     */
    public static native String ABRecord_uniqueId(long record);

    /**
     * Sets property for the supplied person id.
     * @param id the person id
     * @param property the property to use.
     * @param subPropety any sub property if available.
     * @param value the value to set.
     * @return whether the result was successfully added.
     */
    public static native boolean setProperty(
        String id, long property, String subPropety, Object value);

    /**
     * Remove a property.
     * @param id the person id.
     * @param property the property.
     * @return whether the result was successfully removed.
     */
    public static native boolean removeProperty(String id, long property);

    /**
     * Removes a contact from the address book.
     *
     * @param id the person id.
     *
     * @return whether the contact was successfully removed.
     */
    public static native boolean deleteContact(String id);

    /**
     * Creates a new address book contact.
     *
     * @return The identifier of the created contact. null if failed.
     */
    public static native String createContact();

    /**
     * Gets the pointer of the given contact.
     *
     * @param id the person id.
     *
     * @return The pointer of the given contact. Null if failed.
     */
    public static native long getContactPointer(String id);

    /**
     * Gets the value of the <tt>kABAIMInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABAIMInstantProperty</tt> constant
     */
    public static native long kABAIMInstantProperty();

    /**
     * Gets the value of the <tt>kABEmailProperty</tt> constant.
     *
     * @return the value of the <tt>kABEmailProperty</tt> constant
     */
    public static native long kABEmailProperty();

    /**
     * Gets the value of the <tt>kABFirstNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABFirstNameProperty</tt> constant
     */
    public static native long kABFirstNameProperty();

    /**
     * Gets the value of the <tt>kABFirstNamePhoneticProperty</tt> constant.
     *
     * @return the value of the <tt>kABFirstNamePhoneticProperty</tt> constant
     */
    public static native long kABFirstNamePhoneticProperty();

    /**
     * Gets the value of the <tt>kABICQInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABICQInstantProperty</tt> constant
     */
    public static native long kABICQInstantProperty();

    /**
     * Gets the value of the <tt>kABJabberInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABJabberInstantProperty</tt> constant
     */
    public static native long kABJabberInstantProperty();

    /**
     * Gets the value of the <tt>kABLastNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABLastNameProperty</tt> constant
     */
    public static native long kABLastNameProperty();

    /**
     * Gets the value of the <tt>kABLastNamePhoneticProperty</tt> constant.
     *
     * @return the value of the <tt>kABLastNamePhoneticProperty</tt> constant
     */
    public static native long kABLastNamePhoneticProperty();

    /**
     * Gets the value of the <tt>kABMiddleNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABMiddleNameProperty</tt> constant
     */
    public static native long kABMiddleNameProperty();

    /**
     * Gets the value of the <tt>kABMiddleNamePhoneticProperty</tt> constant.
     *
     * @return the value of the <tt>kABMiddleNamePhoneticProperty</tt> constant
     */
    public static native long kABMiddleNamePhoneticProperty();

    /**
     * Gets the value of the <tt>kABMSNInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABMSNInstantProperty</tt> constant
     */
    public static native long kABMSNInstantProperty();

    /**
     * Gets the value of the <tt>kABNicknameProperty</tt> constant.
     *
     * @return the value of the <tt>kABNicknameProperty</tt> constant
     */
    public static native long kABNicknameProperty();

    /**
     * Gets the value of the <tt>kABOrganizationProperty</tt> constant.
     *
     * @return the value of the <tt>kABOrganizationProperty</tt> constant
     */
    public static native long kABOrganizationProperty();

    /**
     * Gets the value of the <tt>kABPersonFlags</tt> constant.
     *
     * @return the value of the <tt>kABPersonFlags</tt> constant
     */
    public static native long kABPersonFlags();

    /**
     * Gets the value of the <tt>kABPhoneProperty</tt> constant.
     *
     * @return the value of the <tt>kABPhoneProperty</tt> constant
     */
    public static native long kABPhoneProperty();

    /**
     * Gets the value of the <tt>kABYahooInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABYahooInstantProperty</tt> constant
     */
    public static native long kABYahooInstantProperty();

    /**
     * Gets the value of the <tt>kABMaidenNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABMaidenNameProperty</tt> constant
     */
    public static native long kABMaidenNameProperty();

    /**
     * Gets the value of the <tt>kABBirthdayProperty</tt> constant.
     *
     * @return the value of the <tt>kABBirthdayProperty</tt> constant
     */
    public static native long kABBirthdayProperty();

    /**
     * Gets the value of the <tt>kABJobTitleProperty</tt> constant.
     *
     * @return the value of the <tt>kABJobTitleProperty</tt> constant
     */
    public static native long kABJobTitleProperty();

    /**
     * Gets the value of the <tt>kABHomePageProperty</tt> constant.
     *
     * @return the value of the <tt>kABHomePageProperty</tt> constant
     */
    public static native long kABHomePageProperty();

    /**
     * Gets the value of the <tt>kABURLsProperty</tt> constant.
     *
     * @return the value of the <tt>kABURLsProperty</tt> constant
     */
    public static native long kABURLsProperty();

    /**
     * Gets the value of the <tt>kABCalendarURIsProperty</tt> constant.
     *
     * @return the value of the <tt>kABCalendarURIsProperty</tt> constant
     */
    public static native long kABCalendarURIsProperty();

    /**
     * Gets the value of the <tt>kABAddressProperty</tt> constant.
     *
     * @return the value of the <tt>kABAddressProperty</tt> constant
     */
    public static native long kABAddressProperty();

    /**
     * Gets the value of the <tt>kABOtherDatesProperty</tt> constant.
     *
     * @return the value of the <tt>kABOtherDatesProperty</tt> constant
     */
    public static native long kABOtherDatesProperty();

    /**
     * Gets the value of the <tt>kABRelatedNamesProperty</tt> constant.
     *
     * @return the value of the <tt>kABRelatedNamesProperty</tt> constant
     */
    public static native long kABRelatedNamesProperty();

    /**
     * Gets the value of the <tt>kABDepartmentProperty</tt> constant.
     *
     * @return the value of the <tt>kABDepartmentProperty</tt> constant
     */
    public static native long kABDepartmentProperty();

    /**
     * Gets the value of the <tt>kABInstantMessageProperty</tt> constant.
     *
     * @return the value of the <tt>kABInstantMessageProperty</tt> constant
     */
    public static native long kABInstantMessageProperty();

    /**
     * Gets the value of the <tt>kABNoteProperty</tt> constant.
     *
     * @return the value of the <tt>kABNoteProperty</tt> constant
     */
    public static native long kABNoteProperty();

    /**
     * Gets the value of the <tt>kABTitleProperty</tt> constant.
     *
     * @return the value of the <tt>kABTitleProperty</tt> constant
     */
    public static native long kABTitleProperty();

    /**
     * Gets the value of the <tt>kABSuffixProperty</tt> constant.
     *
     * @return the value of the <tt>kABSuffixProperty</tt> constant
     */
    public static native long kABSuffixProperty();

    /**
     * Gets the value of the <tt>kABEmailWorkLabel</tt> constant.
     *
     * @return the value of the <tt>kABEmailWorkLabel</tt> constant
     */
    public static native String kABEmailWorkLabel();

    /**
     * Gets the value of the <tt>kABEmailHomeLabel</tt> constant.
     *
     * @return the value of the <tt>kABEmailHomeLabel</tt> constant
     */
    public static native String kABEmailHomeLabel();

    /**
     * Gets the value of the <tt>kABAddressHomeLabel</tt> constant.
     *
     * @return the value of the <tt>kABAddressHomekLabel</tt> constant
     */
    public static native String kABAddressHomeLabel();

    /**
     * Gets the value of the <tt>kABAddressWorkLabel</tt> constant.
     *
     * @return the value of the <tt>kABAddresslWorkLabel</tt> constant
     */
    public static native String kABAddressWorkLabel();

    /**
     * Gets the value of the <tt>kABPhoneWorkLabel</tt> constant.
     *
     * @return the value of the <tt>kABPhoneWorkLabel</tt> constant
     */
    public static native String kABPhoneWorkLabel();

    /**
     * Gets the value of the <tt>kABPhoneHomeLabel</tt> constant.
     *
     * @return the value of the <tt>kABPhoneHomeLabel</tt> constant
     */
    public static native String kABPhoneHomeLabel();

    /**
     * Gets the value of the <tt>kABPhoneMobileLabel</tt> constant.
     *
     * @return the value of the <tt>kABPhoneMobileLabel</tt> constant
     */
    public static native String kABPhoneMobileLabel();

    /**
     * Gets the value of the <tt>kABPhoneMainLabel</tt> constant.
     *
     * @return the value of the <tt>kABPhoneMainLabel</tt> constant
     */
    public static native String kABPhoneMainLabel();

    /**
     * Gets the value of the <tt>kABPhoneWorkFAXLabel</tt> constant.
     *
     * @return the value of the <tt>kABPhoneWorkFAXLabel</tt> constant
     */
    public static native String kABPhoneWorkFAXLabel();

    /**
     * Gets the value of the <tt>kABHomeLabel</tt> constant.
     *
     * @return the value of the <tt>kABHomeLabel</tt> constant
     */
    public static native String kABHomeLabel();

    /**
     * Gets the value of the <tt>kABWorkLabel</tt> constant.
     *
     * @return the value of the <tt>kABWorkLabel</tt> constant
     */
    public static native String kABWorkLabel();

    /**
     * Gets the value of the <tt>kABOtherLabel</tt> constant.
     *
     * @return the value of the <tt>kABOtherLabel</tt> constant
     */
    public static native String kABOtherLabel();

    /**
     * Gets the value of the <tt>kABAddressStreetKey</tt> constant.
     *
     * @return the value of the <tt>kABAddressStreetKey</tt> constant
     */
    public static native String kABAddressStreetKey();

    /**
     * Gets the value of the <tt>kABAddressCityKey</tt> constant.
     *
     * @return the value of the <tt>kABAddressCityKey</tt> constant
     */
    public static native String kABAddressCityKey();

    /**
     * Gets the value of the <tt>kABAddressStateKey</tt> constant.
     *
     * @return the value of the <tt>kABAddressStateKey</tt> constant
     */
    public static native String kABAddressStateKey();

    /**
     * Gets the value of the <tt>kABAddressZIPKey</tt> constant.
     *
     * @return the value of the <tt>kABAddressZIPKey</tt> constant
     */
    public static native String kABAddressZIPKey();

    /**
     * Gets the value of the <tt>kABAddressCountryKey</tt> constant.
     *
     * @return the value of the <tt>kABAddressCountryKey</tt> constant
     */
    public static native String kABAddressCountryKey();

    private static final Logger logger = Logger.getLogger(MacAddressBookUtils.class);

    /**
     * Gets the <tt>displayName</tt> to be set on a contact which is to
     * represent an <tt>ABPerson</tt> specified by the values of its
     * {@link #ABPERSON_PROPERTIES}.
     *
     * @param values the values of the <tt>ABPERSON_PROPERTIES</tt> which
     * represent the <tt>ABPerson</tt> to get the <tt>displayName</tt> of
     * @return the <tt>displayName</tt> to be set on a contact which is to
     * represent the <tt>ABPerson</tt> specified by <tt>values</tt>
     */
    public static String getDisplayName(Object[] values)
    {
        String firstName
            = (values[kABFirstNameProperty] instanceof String) ?
                (String) values[kABFirstNameProperty] :
                "";

        String lastName
            = (values[kABLastNameProperty] instanceof String) ?
                (String) values[kABLastNameProperty] :
                "";

        String displayName = firstName;
        displayName = (firstName.length() == 0) ?
                                       lastName :
                                       displayName + " " + lastName;

        return displayName;
    }

    /**
     * Takes a Mac address book property and value and converts them to a
     * generic detail. This generic detail is added to either the passed
     * contactDetails list or the serverDetails list.
     *
     * The contactDetails list is used if this value is exposed in Accession.
     *
     * The serverDetails list is used if this value is not exposed in
     * Accession, or there is no more space in the Accession list (e.g.
     * more than 2 email addresses)
     *
     * @param property The Mac Address book property
     * @param value The value of the Mac Address Book property
     * @param label the label for the property type (home, work etc)
     * @param additionalDetail the additional detail for the property type
     * (street, city, country/region etc)
     * @param contactDetails the list of contact details for this contact
     * @param serverDetails a list of contact details that Accession does not
     * expose but must include when writing back to Mac Address Book
     */
    public static void convertValueToDetail(int property,
                                            String value,
                                            Object label,
                                            Object additionalDetail,
                                            List<GenericDetail> contactDetails,
                                            List<GenericDetail> serverDetails)
    {
        String category = parseLabel(label);
        if (category == null)
        {
            category = "";
        }

        String subcategory = parseLabel(additionalDetail);
        if (subcategory == null)
        {
            subcategory = "";
        }

        switch (property)
        {
        case kABEmailProperty:
            // Ensure we add this email address in to the correct
            // Accession email address field
            int numEmailDetails = getNumDetailsForType(
                              contactDetails,
                              ServerStoredDetails.EmailAddressDetail.class);

            if (numEmailDetails == 0)
            {
                contactDetails.add(new PersonalContactDetails.EmailAddress1Detail(value));
            }
            else if (numEmailDetails == 1)
            {
                contactDetails.add(new PersonalContactDetails.EmailAddress2Detail(value));
            }
            else
            {
                serverDetails.add(new ServerStoredDetails.EmailAddressDetail(category, value));
            }
            break;
        case kABPhoneProperty:
            if (category.equals(HOME))
            {
                if (getNumDetailsForType(contactDetails, PersonalContactDetails.HomePhoneDetail.class) < 1)
                {
                    contactDetails.add(new PersonalContactDetails.HomePhoneDetail(value));
                }
                else
                {
                    serverDetails.add(new PersonalContactDetails.HomePhoneDetail(value));
                }
            }
            else if (category.equals(WORK))
            {
                if (getNumDetailsForType(contactDetails, ServerStoredDetails.WorkPhoneDetail.class) < 1)
                {
                    contactDetails.add(new ServerStoredDetails.WorkPhoneDetail(value));
                }
                else
                {
                    serverDetails.add(new ServerStoredDetails.WorkPhoneDetail(value));
                }
            }
            else if (category.equals(MOBILE))
            {
                if (getNumDetailsForType(contactDetails, ServerStoredDetails.MobilePhoneDetail.class) < 1)
                {
                    contactDetails.add(new ServerStoredDetails.MobilePhoneDetail(value));
                }
                else
                {
                    serverDetails.add(new ServerStoredDetails.MobilePhoneDetail(value));
                }
            }
            else if (category.equals(WORKFAX))
            {
                if (getNumDetailsForType(contactDetails, ServerStoredDetails.FaxDetail.class) < 1)
                {
                    contactDetails.add(new ServerStoredDetails.FaxDetail(value));
                }
                else
                {
                    serverDetails.add(new ServerStoredDetails.FaxDetail(value));
                }
            }
            else if (category.equals(OTHER))
            {
                if (getNumDetailsForType(contactDetails, PersonalContactDetails.OtherPhoneDetail.class) < 1)
                {
                    contactDetails.add(new PersonalContactDetails.OtherPhoneDetail(value));
                }
                else
                {
                    serverDetails.add(new PersonalContactDetails.OtherPhoneDetail(value));
                }
            }
            else
            {
                serverDetails.add(new ServerStoredDetails.PhoneNumberDetail(category, value));
            }
            break;
        case kABAIMInstantProperty:
            break;
        case kABICQInstantProperty:
            break;
        case kABJabberInstantProperty:
            if (getNumDetailsForType(contactDetails, PersonalContactDetails.IMDetail.class) == 0)
            {
                contactDetails.add(new PersonalContactDetails.IMDetail(value, ProtocolNames.JABBER));
            }
            else
            {
                serverDetails.add(new PersonalContactDetails.IMDetail(value,
                                                           ProtocolNames.JABBER,
                                                           category));
            }
            break;
        case kABMSNInstantProperty:
            break;
        case kABYahooInstantProperty:
            break;
        case kABMaidenNameProperty:
            break;
        case kABFirstNameProperty:
            contactDetails.add(new ServerStoredDetails.FirstNameDetail(value));
            break;
        case kABFirstNamePhoneticProperty:
            break;
        case kABLastNameProperty:
            contactDetails.add(new ServerStoredDetails.LastNameDetail(value));
            break;
        case kABLastNamePhoneticProperty:
            break;
        case kABMiddleNameProperty:
            contactDetails.add(new ServerStoredDetails.MiddleNameDetail(value));
            break;
        case kABMiddleNamePhoneticProperty:
            break;
        case kABNicknameProperty:
            contactDetails.add(new ServerStoredDetails.NicknameDetail(value));
            break;
        case kABBirthdayProperty:
            break;
        case kABURLsProperty:
            break;
        case kABHomePageProperty:
            break;
        case kABOtherDatesProperty:
            break;
        case kABRelatedNamesProperty:
            break;
        case kABNoteProperty:
            break;
        case kABTitleProperty:
            break;
        case kABSuffixProperty:
            break;
        case kABOrganizationProperty:
            contactDetails.add(new ServerStoredDetails.WorkOrganizationNameDetail(value));
            break;
        case kABJobTitleProperty:
            contactDetails.add(new PersonalContactDetails.WorkTitleDetail(value));
            break;
        case kABDepartmentProperty:
            break;
        case kABAddressProperty:
            if (subcategory.equals(HOME))
            {
                if (category.equals(STREET))
                {
                    contactDetails.add(new ServerStoredDetails.AddressDetail(value));
                }
                else if (category.equals(CITY))
                {
                    contactDetails.add(new ServerStoredDetails.CityDetail(value));
                }
                else if (category.equals(STATE))
                {
                    contactDetails.add(new ServerStoredDetails.ProvinceDetail(value));
                }
                if (category.equals(POSTALCODE))
                {
                    contactDetails.add(new ServerStoredDetails.PostalCodeDetail(value));
                }
                if (category.equals(COUNTRY))
                {
                    contactDetails.add(new PersonalContactDetails.CountryDetail(value));
                }
            }
            else if (subcategory.equals(WORK))
            {
                if (category.equals(STREET))
                {
                    contactDetails.add(new ServerStoredDetails.WorkAddressDetail(value));
                }
                else if (category.equals(CITY))
                {
                    contactDetails.add(new ServerStoredDetails.WorkCityDetail(value));
                }
                else if (category.equals(STATE))
                {
                    contactDetails.add(new ServerStoredDetails.WorkProvinceDetail(value));
                }
                if (category.equals(POSTALCODE))
                {
                    contactDetails.add(new ServerStoredDetails.WorkPostalCodeDetail(value));
                }
                if (category.equals(COUNTRY))
                {
                    contactDetails.add(new PersonalContactDetails.WorkCountryDetail(value));
                }
            }
            break;
        }
    }

    /**
     * Gets the Mac address book property definition for a given contact detail
     *
     * @param detail the contact contact for which to look up the
     * property definition
     * @return property the mac address book property definition
     */
    public static long getPropertyforDetail(GenericDetail detail)
    {
         long property = 0;

         if (detail instanceof ServerStoredDetails.FirstNameDetail)
         {
             property = ABPERSON_PROPERTIES[kABFirstNameProperty];
         }
         else if (detail instanceof ServerStoredDetails.LastNameDetail)
         {
             property = ABPERSON_PROPERTIES[kABLastNameProperty];
         }
         else if (detail instanceof ServerStoredDetails.MiddleNameDetail)
         {
             property = ABPERSON_PROPERTIES[kABMiddleNameProperty];
         }
         else if (detail instanceof ServerStoredDetails.NicknameDetail)
         {
             property = ABPERSON_PROPERTIES[kABNicknameProperty];
         }
         else if (detail instanceof ServerStoredDetails.WorkOrganizationNameDetail)
         {
             property = ABPERSON_PROPERTIES[kABOrganizationProperty];
         }
         else if (detail instanceof PersonalContactDetails.WorkTitleDetail)
         {
             property = ABPERSON_PROPERTIES[kABJobTitleProperty];
         }

         return property;
    }

    /**
     * Returns the SubCategory corresponding to the given label.
     *
     * @param label the label to match to a <tt>SubDirectory</tt>
     * @return the <tt>SubDirectory</tt> corresponding to the
     * given label
     */
    private static String parseLabel(Object label)
    {
        if (!(label instanceof String) ||
            StringUtils.isNullOrEmpty((String) label))
        {
            return null;
        }

        String labelString
            = LABEL_PATTERN.matcher((String) label).replaceAll("").trim();

        if (labelString.length() < 1)
            return null;

        String subCategory = null;

        if (labelString.equalsIgnoreCase("home"))
            subCategory = HOME;
        else if (labelString.equalsIgnoreCase("work"))
            subCategory = WORK;
        else if (labelString.equalsIgnoreCase("other"))
            subCategory = OTHER;
        else if (labelString.equalsIgnoreCase("mobile"))
            subCategory = MOBILE;
        else if (labelString.equalsIgnoreCase("street"))
            subCategory = STREET;
        else if (labelString.equalsIgnoreCase("state"))
            subCategory = STATE;
        else if (labelString.equalsIgnoreCase("ZIP"))
            subCategory = POSTALCODE;
        else if (labelString.equalsIgnoreCase("country"))
            subCategory = COUNTRY;
        else if (labelString.equalsIgnoreCase("city"))
            subCategory = CITY;
        else if (labelString.equalsIgnoreCase("InstantMessageUsername"))
            subCategory = NICKNAME;
        else if (labelString.equalsIgnoreCase("workfax") || labelString.equalsIgnoreCase("fax"))
            subCategory = WORKFAX;
        else
            subCategory = labelString;

        return subCategory;
    }

    /**
     * Get the number of details associated with this contact for the given
     * detail type
     *
     * @param contactDetails the list of contact details associated with this
     * contact
     * @param detailType the type of detail to check for
     *
     * @return the number of personal email addresses associated with this
     * contact
     */
    private static int getNumDetailsForType(List<GenericDetail> contactDetails,
                                            Class<? extends GenericDetail> detailType)
    {
        int numDetails = 0;

        for (GenericDetail detail : contactDetails)
        {
            if (detailType.isAssignableFrom(detail.getClass()))
            {
                numDetails++;
            }
        }

        return numDetails;
    }

    /**
     * Determines if the given generic detail relates to a multi line detail
     * in Mac Address Book
     *
     * @param detailType the detail type to check
     * @return whether the given detail relates to a multi line detail in Mac
     * Address Book
     */
    public static boolean isMultiLineDetail(Class<? extends GenericDetail> detailType)
    {
        for (int i = 0; i < multiLineDetails.size(); i++)
        {
            if (multiLineDetails.get(i).isAssignableFrom(detailType))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Writes all email addresses to Mac address book
     *
     * @param id the id of the contact
     * @param emailAddresses the list of email addresses to write
     */
    public static void writeEmailStream(String id,
                                  List<EmailAddressDetail> emailAddresses)
    {
        // Initialize the stream object as twice the size of the list of email
        // address details as the format is:
        // [label1, value1, label2, value2, ...]
        Object[] emailStream = new Object[emailAddresses.size()*2];

        int index = 0;

        for (ServerStoredDetails.EmailAddressDetail emailDetail : emailAddresses)
        {
            String label;
            String value = emailDetail.getEMailAddress();
            if (emailDetail instanceof PersonalContactDetails.EmailAddress1Detail)
            {
                label = ((PersonalContactDetails.EmailAddress1Detail) emailDetail).getCategory();
            }
            else if (emailDetail instanceof PersonalContactDetails.EmailAddress2Detail)
            {
                label = ((PersonalContactDetails.EmailAddress2Detail) emailDetail).getCategory();
            }
            else
            {
                label = emailDetail.getDetailDisplayName();
            }

            if (StringUtils.isNullOrEmpty(label))
            {
                // This happens if the email address was added in Accession.
                // Assume it is a home email
                label = HOME;
            }

            emailStream[index] = value;
            emailStream[index+1] = label;
            index += 2;
        }

        // Write the stream to the address book
        setProperty(id,
                    ABPERSON_PROPERTIES[kABEmailProperty],
                    null,
                    emailStream);
    }

    /**
     * Writes all IM addresses to Mac address book
     *
     * @param id the id of the contact
     * @param imAddresses the list of IM addresses to write
     */
    public static void writeIMStream(String id,
                               List<IMDetail> imAddresses)
    {
        // Initialize the stream object as twice the size of the list of email
        // address details as the format is:
        // [label1, value1, label2, value2, ...]
        Object[] imStream = new Object[imAddresses.size()*2];
        int index = 0;

        for (IMDetail imDetail : imAddresses)
        {
            String label = imDetail.getCategory();
            String value = (String) imDetail.getDetailValue();

            if (StringUtils.isNullOrEmpty(label))
            {
                // This happens if the IM address was added in Accession.
                // Assume it is a home IM address
                label = HOME;
            }

            imStream[index] = value;
            imStream[index+1] = label;
            index += 2;
        }

        // Write the stream to the address book
        setProperty(id,
                    ABPERSON_PROPERTIES[kABJabberInstantProperty],
                    null,
                    imStream);
    }

    /**
     * Writes all phone number to Mac address book
     *
     * @param id the id of the contact
     * @param phoneNumbers the list of phone numbers to write
     */
    public static void writePhoneNumberStream(String id,
                                        List<PhoneNumberDetail> phoneNumbers)
    {
        // Initialize the stream object as twice the size of the list of phone
        // number details as the format is:
        // [label1, value1, label2, value2, ...]
        Object[] phoneNumberStream = new Object[phoneNumbers.size()*2];
        int index = 0;

        for (PhoneNumberDetail phoneDetail : phoneNumbers)
        {
            String label = "";
            String value = (String) phoneDetail.getDetailValue();

            if (phoneDetail instanceof PersonalContactDetails.HomePhoneDetail)
            {
                label = kABPhoneHomeLabel();
            }
            else if (phoneDetail instanceof ServerStoredDetails.WorkPhoneDetail)
            {
                label = kABPhoneWorkLabel();
            }
            else if (phoneDetail instanceof ServerStoredDetails.MobilePhoneDetail)
            {
                label = kABPhoneMobileLabel();
            }
            else if (phoneDetail instanceof PersonalContactDetails.OtherPhoneDetail)
            {
                label = kABOtherLabel();
            }
            else if (phoneDetail instanceof ServerStoredDetails.FaxDetail)
            {
                label = kABPhoneWorkFAXLabel();
            }
            else
            {
                label = kABPhoneHomeLabel();
            }

            if (StringUtils.isNullOrEmpty(label))
            {
                // Assume it is a home phone number
                label = kABPhoneHomeLabel();
            }

            phoneNumberStream[index] = value;
            phoneNumberStream[index+1] = label;
            index += 2;
        }

        // Write the stream to the address book
        setProperty(id,
                    ABPERSON_PROPERTIES[kABPhoneProperty],
                    null,
                    phoneNumberStream);
    }

    /**
     * Writes all postal addresses to Mac address book
     *
     * @param id the id of the contact
     * @param addressDetails the list of addresses to write
     */
    public static void writeAddressDetailsStream(String id,
                                           List<GenericDetail> addressDetails)
    {
        // Initialize the stream object as twice the size of the list of email
        // address details as the format is:
        // [label1, value1, label2, value2, ...]
        ArrayList<Object> homeAddressStream = new ArrayList<>();
        ArrayList<Object> workAddressStream = new ArrayList<>();

        for (GenericDetail addressDetail : addressDetails)
        {
            String value = (String) addressDetail.getDetailValue();
            String label = "";
            String category = "";

            if (addressDetail.getDetailValue() == null)
            {
                continue;
            }

            if (addressDetail instanceof ServerStoredDetails.WorkAddressDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressStreetKey];
                category = WORK;
            }
            else if (addressDetail instanceof ServerStoredDetails.WorkCityDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressCityKey];
                category = WORK;
            }
            else if (addressDetail instanceof ServerStoredDetails.WorkProvinceDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressStateKey];
                category = WORK;
            }
            else if (addressDetail instanceof ServerStoredDetails.WorkPostalCodeDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressZIPKey];
                category = WORK;
            }
            else if (addressDetail instanceof PersonalContactDetails.WorkCountryDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressCountryKey];
                category = WORK;
            }
            else if (addressDetail instanceof ServerStoredDetails.AddressDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressStreetKey];
                category = HOME;
            }
            else if (addressDetail instanceof ServerStoredDetails.CityDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressCityKey];
                category = HOME;
            }
            else if (addressDetail instanceof ServerStoredDetails.ProvinceDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressStateKey];
                category = HOME;
            }
            else if (addressDetail instanceof ServerStoredDetails.PostalCodeDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressZIPKey];
                category = HOME;
            }
            else if (addressDetail instanceof PersonalContactDetails.CountryDetail)
            {
                label = ABKEY_PROPERTIES[kABAddressCountryKey];
                category = HOME;
            }
            else
            {
                // We don't know what to do with this so we log and move on
                logger.error("Asked to parse an unknown address detail: " +
                             addressDetail.getDetailDisplayName() + " " +
                             addressDetail.getClass());
                continue;
            }

            if (category.equals(HOME))
            {
                homeAddressStream.add(value);
                homeAddressStream.add(label);
                homeAddressStream.add(ABLABEL_PROPERTIES[kABHomeLabel]);
            }
            else if (category.equals(WORK))
            {
                workAddressStream.add(value);
                workAddressStream.add(label);
                workAddressStream.add(ABLABEL_PROPERTIES[kABWorkLabel]);
            }
        }

        // If there is a work address to write then write it
        @SuppressWarnings("unchecked")
        ArrayList<Object> combinedAddressStream = (ArrayList<Object>) homeAddressStream.clone();
        combinedAddressStream.addAll(workAddressStream);
        if (combinedAddressStream.size() > 0)
        {
            Object[] addressStream = combinedAddressStream.toArray();
            setProperty(id,
                ABPERSON_PROPERTIES[kABAddressProperty],
                ABLABEL_PROPERTIES[kABHomeLabel],
                addressStream);
        }
    }
}
