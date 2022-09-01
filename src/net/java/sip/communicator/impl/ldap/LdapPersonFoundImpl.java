/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import java.util.HashSet;
import java.util.Set;

import net.java.sip.communicator.service.ldap.LdapDirectory;
import net.java.sip.communicator.service.ldap.LdapPersonFound;
import net.java.sip.communicator.service.ldap.LdapQuery;

/**
 * Implementation of LdapPersonFound
 * An LdapPersonFound is contained in each LdapEvent
 * sent by an LdapDirectory after a successful LDAP search.
 * Each instance corresponds to a person found in the
 * LDAP directory, as well as its contact addresses.
 *
 * @author Sebastien Mazy
 */
public class LdapPersonFoundImpl
    implements LdapPersonFound
{
    /**
     * the server on which the person was found
     */
    private LdapDirectoryImpl server;

    /**
     * the query which this LdapPersonFound is a result of
     */
    private final LdapQuery query;

    /**
     * distinguished name for this person in the directory
     */
    private final String dn;

    /**
     * name/pseudo found in the directory for this person
     */
    private String displayName = null;

    /**
     * title found in the directory for this person
     */
    private String title = null;

    /**
     * first name found in the directory for this person
     */
    private String firstName = null;

    /**
     * surname found in the directory for this person
     */
    private String surname = null;

    /**
     * organization found in the directory for this person
     */
    private String organization = null;

    /**
     * department found in the directory for this person
     */
    private String department = null;

    /**
     * location found in the directory for this person
     */
    private String location = null;

    /**
     * the set storing the mail addresses
     */
    private final Set<String> mails = new HashSet<>();

    /**
     * the set storing the work phone numbers
     */
    private final Set<String> workPhoneNumbers = new HashSet<>();

    /**
     * the set storing the mobile phone numbers
     */
    private final Set<String> mobilePhoneNumbers = new HashSet<>();

    /**
     * the set storing the home phone numbers
     */
    private final Set<String> homePhoneNumbers = new HashSet<>();

    /**
     * the set storing the other (i.e. not work/home/mobile) phone numbers
     */
    private final Set<String> otherPhoneNumbers = new HashSet<>();

    /**
     * the set storing the IM addresses
     */
    private final Set<String> jabberIMAddresses = new HashSet<>();

    /**
     * the constructor for this class
     *
     * @param server the server on which this person was found
     * @param dn distinguished name for this person in the directory
     * @param query the search query
     */
    public LdapPersonFoundImpl(LdapDirectoryImpl server, String dn,
            LdapQuery query)
    {
        if(server == null || query == null || dn == null)
            throw new NullPointerException();
        this.server = server;
        this.query = query;
        this.dn = dn;
    }

    /**
     * Returns the query which this Ldap person found is a result of
     *
     * @return the initial query
     */
    public LdapQuery getQuery()
    {
        return this.query;
    }

    /**
     * Returns the server which this person was found on
     *
     * @return the server
     */
    public LdapDirectory getServer()
    {
        return (LdapDirectory) this.server;
    }

    /**
     * Sets the name/pseudo found in the directory for this person
     *
     * @param name the name/pseudo found in the directory for this person
     */
    public void setDisplayName(String name)
    {
        this.displayName = name;
    }

    /**
     * Returns the name/pseudo found in the directory for this person
     *
     * @return the name/pseudo found in the directory for this person
     */
    public String getDisplayName()
    {
        return this.displayName;
    }

    /**
     * Sets the title found in the directory for this person
     *
     * @param title the title found in the directory for this person
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Returns the title found in the directory for this person
     *
     * @return the title found in the directory for this person
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * Sets the first name found in the directory for this person
     *
     * @param firstName the name/pseudo found in the directory for this
     * person
     */
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    /**
     * Returns the first name found in the directory for this person
     *
     * @return the first name found in the directory for this person
     */
    public String getFirstName()
    {
        return this.firstName;
    }

    /**
     * Sets the surname found in the directory for this person
     *
     * @param surname the surname found in the directory for this person
     */
    public void setSurname(String surname)
    {
        this.surname = surname;
    }

    /**
     * Returns the surname found in the directory for this person
     *
     * @return the surname found in the directory for this person
     */
    public String getSurname()
    {
        return this.surname;
    }

    /**
     * Sets the organization found in the directory for this person
     *
     * @param organization the organization found in the directory for this
     *  person
     */
    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    /**
     * Returns the organization found in the directory for this person
     *
     * @return the organization found in the directory for this person
     */
    public String getOrganization()
    {
        return this.organization;
    }

    /**
     * Sets the department found in the directory for this person
     *
     * @param department the department found in the directory for this
     *  person
     */
    public void setDepartment(String department)
    {
        this.department = department;
    }

    /**
     * Returns the department found in the directory for this person
     *
     * @return the department found in the directory for this person
     */
    public String getDepartment()
    {
        return this.department;
    }

    /**
     * Sets the location found in the directory for this person
     *
     * @param location the location found in the directory for this
     *  person
     */
    public void setLocation(String location)
    {
        this.location = location;
    }

    /**
     * Returns the location found in the directory for this person
     *
     * @return the location found in the directory for this person
     */
    public String getLocation()
    {
        return this.location;
    }

    /**
     * Adds a mail address to this person
     *
     * @param mail the mail address
     */
    public void addMail(String mail)
    {
        this.mails.add(mail);
    }

    /**
     * Returns mail addresss from this person
     *
     * @return mail addresss from this person
     */
    public Set<String> getMail()
    {
        Set<String> mail = new HashSet<>();

        mail.addAll(this.mails);

        return mail;
    }

    /**
     * Returns telephone numbers from this person
     *
     * @return telephone numbers from this person
     */
    public Set<String> getAllPhone()
    {
        Set<String> allPhone = new HashSet<>();

        allPhone.addAll(this.workPhoneNumbers);
        allPhone.addAll(this.mobilePhoneNumbers);
        allPhone.addAll(this.homePhoneNumbers);
        allPhone.addAll(this.otherPhoneNumbers);

        return allPhone;
    }

    /**
     * Adds a work telephone number to this person
     *
     * @param telephoneNumber the work telephone number
     */
    public void addWorkPhone(String telephoneNumber)
    {
        this.workPhoneNumbers.add(telephoneNumber);
    }

    /**
     * Returns work telephone numbers from this person
     *
     * @return work telephone numbers from this person
     */
    public Set<String> getWorkPhone()
    {
        Set<String> workPhone = new HashSet<>();

        workPhone.addAll(this.workPhoneNumbers);

        return workPhone;
    }

    /**
     * Adds a mobile telephone number to this person
     *
     * @param telephoneNumber the mobile telephone number
     */
    public void addMobilePhone(String telephoneNumber)
    {
        this.mobilePhoneNumbers.add(telephoneNumber);
    }

    /**
     * Returns mobile telephone numbers from this person
     *
     * @return mobile telephone numbers from this person
     */
    public Set<String> getMobilePhone()
    {
        Set<String> mobilePhone = new HashSet<>();

        mobilePhone.addAll(this.mobilePhoneNumbers);

        return mobilePhone;
    }

    /**
     * Adds a home telephone number to this person
     *
     * @param telephoneNumber the home telephone number
     */
    public void addHomePhone(String telephoneNumber)
    {
        this.homePhoneNumbers.add(telephoneNumber);
    }

    /**
     * Returns home telephone numbers from this person
     *
     * @return home telephone numbers from this person
     */
    public Set<String> getHomePhone()
    {
        Set<String> homePhone = new HashSet<>();

        homePhone.addAll(this.homePhoneNumbers);

        return homePhone;
    }

    /**
     * Adds an other (i.e. not work/mobile/home) telephone number to this person
     *
     * @param telephoneNumber the other telephone number
     */
    public void addOtherPhone(String telephoneNumber)
    {
        this.otherPhoneNumbers.add(telephoneNumber);
    }

    /**
     * Returns other (i.e. not work/mobile/home) telephone numbers from this
     * person
     *
     * @return other telephone numbers from this person
     */
    public Set<String> getOtherPhone()
    {
        Set<String> otherPhone = new HashSet<>();

        otherPhone.addAll(this.otherPhoneNumbers);

        return otherPhone;
    }

    /**
     * Adds a Jabber IM address to this person
     *
     * @param jabberIM the Jabber IM address
     */
    public void addJabberIM(String jabberIM)
    {
        this.jabberIMAddresses.add(jabberIM);
    }

    /**
     * Returns Jabber IM addresses from this person
     *
     * @return Jabber IM addresses from this person
     */
    public Set<String> getJabberIM()
    {
        Set<String> jabberIM = new HashSet<>();

        jabberIM.addAll(this.jabberIMAddresses);

        return jabberIM;
    }

    /**
     * Returns the distinguished name for this person
     *
     * @return the distinguished name for this person
     */
    public String getDN()
    {
        return this.dn;
    }

    /**
     * A string representation of this LdapPersonFoundImpl
     * (created for debugging purposes)
     *
     * @return a printable String
     */
    public String toString()
    {
        return this.getDisplayName();
    }

    /**
     * Compare this object with another ones.
     *
     * @param other other object to compare with
     * @return negative integer if less, 0 if equals and positive integer if
     * over
     */
    public int compareTo(LdapPersonFound other)
    {
        if(this.toString().equals(other.toString()))
            return this.getDN().compareTo((other).getDN());
        else
            return this.toString().compareTo(other.toString());
    }

    /**
     * Test equality between this object and another ones.
     *
     * @return true if the two objects are equal, false otherwise
     */
    public boolean equals(Object o)
    {
        if(!(o instanceof LdapPersonFound) || o == null)
            return false;
        else
            return this.toString().equals(o.toString()) &&
                this.getDN().equals(((LdapPersonFound) o).getDN());
    }
}
