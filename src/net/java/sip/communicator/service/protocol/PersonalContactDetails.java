// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.ServerStoredDetails.EmailAddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PhoneNumberDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.StringDetail;

/**
 * Class containing all the data types that personal contacts support but that
 * are not covered by one of the ServerStoredDetails
 */
public class PersonalContactDetails
{
    /**
     * Class for displaying the "Other" phone number field of a contact
     */
    public static class OtherPhoneDetail extends PhoneNumberDetail
    {
        public OtherPhoneDetail(String otherNumber)
        {
            super(otherNumber);
        }
    }

    /**
     * Class for displaying the "Home" phone number field of a contact
     */
    public static class HomePhoneDetail extends PhoneNumberDetail
    {
        public HomePhoneDetail(String homeNumber)
        {
            super(homeNumber);
        }
    }

    /**
     * Class for displaying the job title field of a contact
     */
    public static class WorkTitleDetail extends StringDetail
    {
        public WorkTitleDetail(String jobTitle)
        {
            super("Job Title", jobTitle);
        }
    }

    /**
     * Class for displaying the "Preferred Email" field of a contact, where
     * the value specifies which detail has the address e.g. "Work", not
     * "john@software.com".
     */
    public static class PreferredEmailDetail extends StringDetail
    {
        public PreferredEmailDetail(String preferredEmail)
        {
            super("Preferred Email", preferredEmail);
        }
    }

    /**
     * Class for displaying the "Preferred Number" field of a contact, where
     * the value specifies which detail has the address e.g. "Home", not
     * "01234567890".
     */
    public static class PreferredNumberDetail extends StringDetail
    {
        public PreferredNumberDetail(String preferredPhone)
        {
            super("Preferred Phone Number", preferredPhone);
        }
    }

    /**
     * Class for the email address 1 field of a Contact
     */
    public static class EmailAddress1Detail extends EmailAddressDetail
    {
        /**
         * Used for Mac Address Book to store what category this email address
         * is (home, work, etc).
         */
        private final String category;

        public EmailAddress1Detail(String value)
        {
            this(value, "");
        }

        public EmailAddress1Detail(String value, String category)
        {
            super("Email address", value);
            this.category = category;
        }

        public String getCategory()
        {
            return category;
        }
    }

    /**
     * Class for the email address 2 field of a Contact
     */
    public static class EmailAddress2Detail extends EmailAddressDetail
    {
        /**
         * Used for Mac Address Book to store what category this email address
         * is (home, work, etc).
         */
        private final String category;

        public EmailAddress2Detail(String value)
        {
            this(value, "");
        }

        public EmailAddress2Detail(String value, String category)
        {
            super("Email address", value);
            this.category = category;
        }

        public String getCategory()
        {
            return category;
        }
    }

    /**
     * Class for the country/region detail of the contact.
     * <P/>
     * Used when the locale is not known
     */
    public static class CountryDetail extends GenericDetail
    {
        public CountryDetail(String value)
        {
            super("Country/region detail", value);
        }
    }

    /**
     * Class for work country/region detail of the contact.
     * <P/>
     * Used when the locale is not known
     */
    public static class WorkCountryDetail extends GenericDetail
    {
        public WorkCountryDetail(String value)
        {
            super("Work country/region detail", value);
        }
    }

    public static class IMDetail extends GenericDetail
    {
        private String protocolName;

        /**
         * Used for Mac Address Book to store what category this IM address is
         * (home, work, etc).
         */
        private final String category;

        public IMDetail(String value, String protocolName, String category)
        {
            super("IMDetail", value);
            this.protocolName = protocolName;
            this.category = category;
        }

        public IMDetail(String value, String protocolName)
        {
            this(value, protocolName, "");
        }

        public String getProtocolName()
        {
            return this.protocolName;
        }

        public String getCategory()
        {
            return this.category;
        }
    }
}
