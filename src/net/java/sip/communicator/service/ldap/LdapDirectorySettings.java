/*
 se* Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.ldap;

import java.util.*;

/**
 * A wrapper around the settings needed to create an LdapDirectory
 * This object is mandatory to create an LdapServer. It's also the
 * retrieved object when calling getSettings() from LdapServer. It
 * also be used to retrieve, remove or store settings in the
 * persistent configuration.
 *
 * @author Sebastien Mazy
 */
public interface LdapDirectorySettings
    extends LdapConstants,
            Cloneable
{
    /**
     * simple getter for name
     *
     * @return the name property
     */
    String getName();

    /**
     * simple setter for name
     *
     * @param name the name property
     */
    void setName(String name);

    /**
     * simple getter for enabled
     *
     * @return whether the server is marked as enabled
     */
    boolean isEnabled();

    /**
     * simple setter for enabled
     *
     * @param enabled whether the server is marked as enabled
     */
    void setEnabled(boolean enabled);

    /**
     * simple getter for hostname
     *
     * @return the hostname property
     */
    String getHostname();

    /**
     * simple setter for hostname
     *
     * @param hostname the hostname property
     */
    void setHostname(String hostname);

    /**
     * simple getter for encryption
     *
     * @return the encryption property
     *
     * @see LdapConstants.Encryption
     */
    Encryption getEncryption();

    /**
     * simple setter for encryption
     *
     * @param encryption the encryption property
     *
     * @see LdapConstants.Encryption
     */
    void setEncryption(Encryption encryption);

    /**
     * simple getter for port
     *
     * @return the port property
     */
    int getPort();

    /**
     * simple setter for port
     *
     * @param port the port property
     */
    void setPort(int port);

    /**
     * simple getter for auth
     *
     * @return the auth property
     *
     * @see LdapConstants.Auth
     */
    Auth getAuth();

    /**
     * simple setter for auth
     *
     * @param auth the auth property
     *
     * @see LdapConstants.Auth
     */
    void setAuth(Auth auth);

    /**
     * simple getter for bindDN
     *
     * @return the bindDN property
     */
    String getBindDN();

    /**
     * simple setter for bindDN
     *
     * @param bindDN the bindDN property
     */
    void setBindDN(String bindDN);

    /**
     * simple getter for password
     *
     * @return the password property
     */
    String getPassword();

    /**
     * simple setter for password
     *
     * @param password the password property
     */
    void setPassword(String password);

    /**
     * simple getter for baseDN
     *
     * @return the baseDN property
     */
    String getBaseDN();

    /**
     * simple setter for baseDN
     *
     * @param baseDN the baseDN property
     */
    void setBaseDN(String baseDN);

    /**
     * Returns the search scope: one level under the base distinguished name
     * or all the subtree.
     *
     * @return the search scope
     *
     * @see LdapConstants.Scope
     */
    Scope getScope();

    /**
     * Sets the search scope: one level under the base distinguished name
     * or all the subtree.
     *
     * @param scope the new search scope
     *
     * @see LdapConstants.Scope
     */
    void setScope(Scope scope);

    /**
     * Returns mail fields that we will lookup.
     *
     * @return mail fields that we will lookup
     */
    List<String> getMailSearchFields();

    /**
     * Returns mail suffix.
     *
     * @return mail suffix
     */
    String getMailSuffix();

    /**
     * Set mail suffix.
     *
     * @param suffix mail suffix
     */
    void setMailSuffix(String suffix);

    /**
     * Returns work phone fields that we will lookup.
     *
     * @return work phone fields that we will lookup
     */
    List<String> getWorkPhoneSearchFields();

    /**
     * Returns mobile phone fields that we will lookup.
     *
     * @return mobile phone fields that we will lookup
     */
    List<String> getMobilePhoneSearchFields();

    /**
     * Returns home phone fields that we will lookup.
     *
     * @return home phone fields that we will lookup
     */
    List<String> getHomePhoneSearchFields();

    /**
     * Returns the global prefix to be used when calling phones from this ldap
     * source.
     *
     * @return the global prefix to be used when calling phones from this ldap
     * source
     */
    String getGlobalPhonePrefix();

    /**
     * Sets the global prefix to be used when calling phones from this ldap
     * source.
     *
     * @param prefix the global prefix to be used when calling phones from this ldap
     * source
     */
    void setGlobalPhonePrefix(String prefix);

    /**
     * Returns title fields that we will lookup.
     *
     * @return title fields that we will lookup
     */
    List<String> getTitleSearchFields();

    /**
     * Returns display name fields that we will lookup.
     *
     * @return display name fields that we will lookup
     */
    List<String> getDisplayNameSearchFields();

    /**
     * Returns first name fields that we will lookup.
     *
     * @return first name fields that we will lookup
     */
    List<String> getFirstNameSearchFields();

    /**
     * Returns last name fields that we will lookup.
     *
     * @return last name fields that we will lookup
     */
    List<String> getLastNameSearchFields();

    /**
     * Returns organization fields that we will lookup.
     *
     * @return organization fields that we will lookup
     */
    List<String> getOrganizationSearchFields();

    /**
     * Returns department fields that we will lookup.
     *
     * @return department fields that we will lookup
     */
    List<String> getDepartmentSearchFields();

    /**
     * Returns location fields that we will lookup.
     *
     * @return location fields that we will lookup
     */
    List<String> getLocationSearchFields();

    /**
     * Returns other (i.e. not work/home/mobile) phone fields that we will
     * lookup.
     *
     * @return other phone fields that we will lookup
     */
    List<String> getOtherPhoneSearchFields();

    /**
     * Returns Jabber IM fields that we will lookup.
     *
     * @return Jabber IM fields that we will lookup
     */
    List<String> getJabberSearchFields();

    /**
     * Validate settings to ensure LDAP service can safely run.
     *
     * @exception IllegalArgumentException if a settings issue is found.
     */
    void validateSettings();

    /**
     * Loads the settings from the config files into the LdapDirectorySetting.
     *
     * @see LdapDirectorySettings#persistentLoad
     */
    void persistentLoad();

    /**
     * Checks if both LdapDirectorySettings instance have the same content
     *
     * @param other object to compare
     * @return whether both LdapDirectorySettings instance have the same content
     *
     * @see java.lang.Object#equals
     */
    boolean equals(LdapDirectorySettings other);

    /**
     * Returns the hash code for this instance.
     * It has to be consistent with equals.
     *
     * @return the hash code dor this instance
     *
     * @see java.lang.Object#hashCode
     */
    int hashCode();

    /**
     * Clone this object.
     *
     * @return clone of this object
     */
    LdapDirectorySettings clone();
}
