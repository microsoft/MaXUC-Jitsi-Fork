/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap;

import javax.naming.directory.*;

/**
 * Constants used by the LDAP service.
 *
 * @author Sebastien Mazy
 */
public interface LdapConstants
{
    /**
     * security methods used to connect to the remote host
     * and their default port
     */
    enum Encryption
    {
        /**
         * No encryption.
         */
        CLEAR(389, "ldap://"),

        /**
         * SSL encryption.
         */
        SSL(636, "ldaps://");

        private final int defaultPort;
        private final String protocolString;

        Encryption(int defaultPort, String protocolString)
        {
            this.defaultPort = defaultPort;
            this.protocolString = protocolString;
        }

        /**
         * Returns the default port for this security method.
         *
         * @return the default port for this security method.
         */
        public int defaultPort()
        {
            return this.defaultPort;
        }

        /**
         * Returns the protocol string for this security method.
         *
         * @return the protocol string
         */
        public String protocolString()
        {
            return this.protocolString;
        }

        /**
         * Returns default value for encryption.
         *
         * @return default value for encryption
         */
        public static Encryption defaultValue()
        {
            return CLEAR;
        }
    }

    /**
     * Authentication methods.
     */
    enum Auth
    {
        /**
         * No authentication.
         */
        NONE,

        /**
         * Authentication with login and password.
         */
        SIMPLE;

        /**
         * Returns default value for authentication.
         *
         * @return default value for authentication
         */
        public static Auth defaultValue()
        {
            return NONE;
        }
    }

    /**
     * search scope in the directory: one level, subtree
     */
    enum Scope
    {
        /**
         * Subtree search.
         */
        SUB(SearchControls.SUBTREE_SCOPE),

        /**
         * One level search.
         */
        ONE(SearchControls.ONELEVEL_SCOPE);

        private int constant;

        Scope(int constant)
        {
            this.constant = constant;
        }

        /**
         * Returns default value for scope.
         *
         * @return default value for scope
         */
        public static Scope defaultValue()
        {
            return SUB;
        }

        /**
         * Returns the matching constant field from SearchControls
         *
         * @return the matching constant field
         */
        public int getConstant()
        {
            return this.constant;
        }
    }

    /**
     * How long should we wait for the connection to establish?
     * (in ms)
     */
    String LDAP_CONNECT_TIMEOUT = "5000";

    /**
     * How long should we wait for a LDAP response?
     * (in ms)
     */
    String LDAP_READ_TIMEOUT = "60000";

    /**
     * Key for LDAP nickname when setting and getting map values.
     */
    String NICKNAME = "nickname";

    /**
     * Key for LDAP first name when setting and getting map values.
     */
    String FIRSTNAME = "firstName";

    /**
     * Key for LDAP last name when setting and getting map values.
     */
    String LASTNAME = "lastName";

    /**
     * Key for LDAP job title when setting and getting map values.
     */
    String TITLE = "title";

    /**
     * Key for LDAP organization when setting and getting map values.
     */
    String ORG = "organization";

    /**
     * Key for LDAP department when setting and getting map values.
     */
    String DEPARTMENT = "department";

    /**
     * Key for LDAP location when setting and getting map values.
     */
    String LOCATION = "location";

    /**
     * Key for LDAP email addresses when setting and getting map values.
     */
    String EMAIL = "mail";

    /**
     * Key for LDAP work phone when setting and getting map values.
     */
    String WORKPHONE = "workPhone";

    /**
     * Key for LDAP mobile phone when setting and getting map values.
     */
    String MOBILEPHONE = "mobilePhone";

    /**
     * Key for LDAP home phone when setting and getting map values.
     */
    String HOMEPHONE = "homePhone";

    /**
     * Key for LDAP other phone (i.e. not home/work/mobile) when setting and
     * getting map values.
     */
    String OTHERPHONE = "otherPhone";

    /**
     * Key for LDAP Jabber IM addresses when setting and getting map values.
     */
    String JABBER = "jabber";
}
