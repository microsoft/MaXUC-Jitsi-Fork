/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap;

import java.util.*;

/**
 * An LdapPersonFound is contained in each LdapEvent
 * sent by an LdapDirectory after a successful LDAP search.
 * Each instance corresponds to a person found in the
 * LDAP directory, as well as its contact addresses.
 *
 * @author Sebastien Mazy
 */
public interface LdapPersonFound
    extends Comparable<LdapPersonFound>
{
    /**
     * Returns the query which this LdapPersonFound is a result of
     *
     * @return the initial query
     */
    LdapQuery getQuery();

    /**
     * Returns the server which this person was found on
     *
     * @return the server
     */
    LdapDirectory getServer();

    /**
     * Sets the name/pseudo found in the directory for this person
     *
     * @param name the name/pseudo found in the directory for this person
     */
    void setDisplayName(String name);

    /**
     * Returns the name/pseudo found in the directory for this person
     *
     * @return the name/pseudo found in the directory for this person
     */
    String getDisplayName();

    /**
     * Sets the title found in the directory for this person
     *
     * @param title the title found in the directory for this person
     */
    void setTitle(String title);

    /**
     * Returns the title found in the directory for this person
     *
     * @return the title found in the directory for this person
     */
    String getTitle();

    /**
     * Sets the first name found in the directory for this person
     *
     * @param firstName the name/pseudo found in the directory for this
     *  person
     */
    void setFirstName(String firstName);

    /**
     * Returns the first name found in the directory for this person
     *
     * @return the first name found in the directory for this person
     */
    String getFirstName();

    /**
     * Sets the surname found in the directory for this person
     *
     * @param surname the surname found in the directory for this person
     */
    void setSurname(String surname);

    /**
     * Returns the surname found in the directory for this person
     *
     * @return the surname found in the directory for this person
     */
    String getSurname();

    /**
     * Sets the organization found in the directory for this person
     *
     * @param organization the organization found in the directory for this
     *  person
     */
    void setOrganization(String organization);

    /**
     * Returns the organization found in the directory for this person
     *
     * @return the organization found in the directory for this person
     */
    String getOrganization();

    /**
     * Sets the department found in the directory for this person
     *
     * @param department the department found in the directory for this
     *  person
     */
    void setDepartment(String department);

    /**
     * Returns the department found in the directory for this person
     *
     * @return the department found in the directory for this person
     */
    String getDepartment();

    /**
     * Sets the location found in the directory for this person
     *
     * @param location the location found in the directory for this
     *  person
     */
    void setLocation(String location);

    /**
     * Returns the location found in the directory for this person
     *
     * @return the location found in the directory for this person
     */
    String getLocation();

    /**
     * Adds a mail address to this person
     *
     * @param mail the mail address
     */
    void addMail(String mail);

    /**
     * Returns mail addresses from this person
     *
     * @return mail addresses from this person
     */
    Set<String> getMail();

    /**
     * Returns telephone numbers from this person
     *
     * @return telephone numbers from this person
     */
    Set<String> getAllPhone();

    /**
     * Adds a work telephone number to this person
     *
     * @param telephoneNumber the work telephone number
     */
    void addWorkPhone(String telephoneNumber);

    /**
     * Returns work telephone numbers from this person
     *
     * @return work telephone numbers from this person
     */
    Set<String> getWorkPhone();

    /**
     * Adds a mobile telephone number to this person
     *
     * @param telephoneNumber the mobile telephone number
     */
    void addMobilePhone(String telephoneNumber);

    /**
     * Returns mobile telephone numbers from this person
     *
     * @return mobile telephone numbers from this person
     */
    Set<String> getMobilePhone();

    /**
     * Adds a home telephone number to this person
     *
     * @param telephoneNumber the home telephone number
     */
    void addHomePhone(String telephoneNumber);

    /**
     * Returns home telephone numbers from this person
     *
     * @return home telephone numbers from this person
     */
    Set<String> getHomePhone();

    /**
     * Adds an other (i.e. not work/mobile/home) telephone number to this person
     *
     * @param telephoneNumber the other telephone number
     */
    void addOtherPhone(String telephoneNumber);

    /**
     * Returns other (i.e. not work/mobile/home) telephone numbers from this
     * person
     *
     * @return other telephone numbers from this person
     */
    Set<String> getOtherPhone();

    /**
     * Adds a Jabber IM address to this person
     *
     * @param jabberAddress the Jabber IM address
     */
    void addJabberIM(String jabberAddress);

    /**
     * Returns Jabber IM addresses from this person
     *
     * @return Jabber IM addresses from this person
     */
    Set<String> getJabberIM();

    /**
     * Returns the distinguished name for this person
     *
     * @return the distinguished name for this person
     */
    String getDN();
}
