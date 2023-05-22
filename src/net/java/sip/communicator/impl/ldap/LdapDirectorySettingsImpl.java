/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.ldap;

import java.util.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.util.*;

/**
 * @author Sebastien Mazy
 *
 * Implementation of LdapDirectorySettings
 * a wrapper around the settings needed to create an LdapDirectory
 */
public class LdapDirectorySettingsImpl
    implements LdapDirectorySettings
{
    /**
     * Logger for <tt>LdapDirectorySettingsImpl</tt>
     */
    private static final Logger logger =
        Logger.getLogger(LdapDirectorySettingsImpl.class);

    /**
     * Simple constructor for this class,
     * sets default values,
     * note that you won't be able to create an LdapDirectory with these
     * defaults (empty name, empty hostname forbidden by LdapDirectory)
     */
    public LdapDirectorySettingsImpl()
    {
        setName("");
        setEnabled(true);
        setHostname("");
        setEncryption(Encryption.defaultValue());
        setPort(0);
        setAuth(Auth.defaultValue());
        setBindDN("");
        setPassword("");
        setBaseDN("");
        setScope(Scope.defaultValue());
        setGlobalPhonePrefix("");

        // display name
        List<String> lst = new ArrayList<>();
        lst.add("displayName");
        lst.add("cn");
        lst.add("commonname");
        mapAttributes.put(LdapConstants.NICKNAME, lst);

        // first name
        lst = new ArrayList<>();
        lst.add("givenName");
        lst.add("givenname");
        lst.add("gn");
        mapAttributes.put(LdapConstants.FIRSTNAME, lst);

        // last name
        lst = new ArrayList<>();
        lst.add("sn");
        lst.add("surname");
        mapAttributes.put(LdapConstants.LASTNAME, lst);

        // organization
        lst = new ArrayList<>();
        lst.add("o");
        lst.add("organizationName");
        lst.add("company");
        mapAttributes.put(LdapConstants.ORG, lst);

        // department
        lst = new ArrayList<>();
        lst.add("ou");
        lst.add("orgunit");
        lst.add("organizationalUnitName");
        lst.add("department");
        lst.add("departmentNumber");
        mapAttributes.put(LdapConstants.DEPARTMENT, lst);

        // mail
        lst = new ArrayList<>();
        lst.add("mail");
        lst.add("uid");
        mapAttributes.put("mail", lst);

        //work phone
        lst = new ArrayList<>();
        lst.add("telephoneNumber");
        lst.add("primaryPhone");
        lst.add("companyPhone");
        lst.add("otherTelephone");
        lst.add("tel");
        mapAttributes.put(LdapConstants.WORKPHONE, lst);

        //mobile phone
        lst = new ArrayList<>();
        lst.add("mobilePhone");
        lst.add("mobileTelephoneNumber");
        lst.add("mobileTelephoneNumber");
        lst.add("mobileTelephoneNumber");
        lst.add("carPhone");
        mapAttributes.put(LdapConstants.MOBILEPHONE, lst);

        //home phone
        lst = new ArrayList<>();
        lst.add("homePhone");
        lst.add("otherHomePhone");
        mapAttributes.put(LdapConstants.HOMEPHONE, lst);
    }

    /**
     * Constructor.
     *
     * @param settings existing settings
     */
    public LdapDirectorySettingsImpl(LdapDirectorySettingsImpl settings)
    {
        this();
        setName(settings.getName());
        setEnabled(settings.isEnabled());
        setHostname(settings.getHostname());
        setEncryption(settings.getEncryption());
        setPort(settings.getPort());
        setAuth(settings.getAuth());
        setBindDN(settings.getBindDN());
        setPassword(settings.getPassword());
        setBaseDN(settings.getBaseDN());
        setScope(settings.getScope());
        setGlobalPhonePrefix(settings.getGlobalPhonePrefix());
        mapAttributes = settings.mapAttributes;
        mailSuffix = settings.mailSuffix;
    }

    private static final String ldapConfigPrefix = "net.java.sip.communicator.impl.ldap.directories.dir1";

    private final ConfigurationService configService = LdapActivator.getConfigurationService();

    /**
     * name that will be displayed in the UI
     * e.g. "My LDAP server"
     */
    private String name;

    /**
     * a marker
     */
    private boolean enabled;

    /**
     * the hostname,
     * e.g. "example.com"
     */
    private String hostname;

    /**
     * the encryption protocol
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Encryption
     */
    private Encryption encryption;

    /**
     * The network port number of the remote server
     */
    private int port;

    /**
     * the authentication method
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Auth
     */
    private Auth auth;

    /**
     * the bind distinguished name if authentication is needed
     * e.g. "cn=user,ou=People,dc=example,dc=com"
     */
    private String bindDN;

    /**
     * the password if authentication is needed
     */
    private String password;

    /**
     * distinguished name used as a base for searches
     * e.g. "dc=example,dc=com"
     */
    private String baseDN;

    /**
     * the search scope: one level under the base distinguished name
     * or all the subtree.
     */
    private Scope scope;

    /**
     * The global phone prefix.
     */
    private String globalPhonePrefix;

    /**
     * Mail suffix.
     */
    private String mailSuffix = null;

    /**
     * Attributes map.
     */
    private Map<String, List<String> > mapAttributes =
            new HashMap<>();

    /**
     * simple getter for name
     *
     * @return the name property
     */
    public String getName()
    {
        return name;
    }

    /**
     * simple setter for name
     *
     * @param name the name property
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Simple getter for enabled. Required by LdapDirectorySettings interface.
     *
     * @return whether the server is marked as enabled
     *
     * @see LdapDirectorySettings#isEnabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * simple setter for enabled. Required by LdapDirectorySettings interface.
     *
     * @param enabled whether the server is marked as enabled
     *
     * @see LdapDirectorySettings#setEnabled
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * simple getter for hostname
     *
     * @return the hostname property
     */
    public String getHostname()
    {
        return hostname;
    }

    /**
     * simple setter for hostname
     *
     * @param hostname the hostname property
     */
    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }

    /**
     * simple getter for encryption
     *
     * @return the encryption property
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Encryption
     */
    public Encryption getEncryption()
    {
        return encryption;
    }

    /**
     * simple setter for encryption
     *
     * @param encryption the encryption property
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Encryption
     */
    public void setEncryption(Encryption encryption)
    {
        this.encryption = encryption;
    }

    /**
     * simple getter for port
     *
     * @return the port property
     */
    public int getPort()
    {
        return port;
    }

    /**
     * simple setter for port
     *
     * @param port the port property
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * simple getter for auth
     *
     * @return the auth property
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Auth
     */
    public Auth getAuth()
    {
        return auth;
    }

    /**
     * simple setter for auth
     *
     * @param auth the auth property
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Auth
     */
    public void setAuth(Auth auth)
    {
        this.auth = auth;
    }

    /**
     * simple getter for bindDN
     *
     * @return the bindDN property
     */
    public String getBindDN()
    {
        return bindDN;
    }

    /**
     * Returns the user name associated with the corresponding ldap directory.
     *
     * @return the user name associated with the corresponding ldap directory
     */
    public String getUserName()
    {
        if (bindDN == null)
            return null;

        String userName = null;
        int uidIndex = bindDN.indexOf("uid=");
        if (uidIndex > -1)
        {
            int commaIndex = bindDN.indexOf(",", uidIndex + 5);

            if (commaIndex > -1)
                userName = bindDN.substring(uidIndex + 4, commaIndex);
            else
                userName = bindDN.substring(uidIndex + 4);
        }

        return userName;
    }

    /**
     * simple setter for bindDN
     *
     * @param bindDN the bindDN property
     */
    public void setBindDN(String bindDN)
    {
        this.bindDN = bindDN;
    }

    /**
     * simple getter for password
     *
     * @return the password property
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * simple setter for password
     *
     * @param password the password property
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * simple getter for baseDN
     *
     * @return the baseDN property
     */
    public String getBaseDN()
    {
        return baseDN;
    }

    /**
     * simple setter for baseDN
     *
     * @param baseDN the baseDN property
     */
    public void setBaseDN(String baseDN)
    {
        this.baseDN = baseDN;
    }

    /**
     * Returns the search scope: one level under the base distinguished name
     * or all the subtree. Required by LdapDirectorySettings interface.
     *
     * @return the search scope
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Scope
     * @see LdapDirectorySettings#getScope
     */
    public Scope getScope()
    {
        return scope;
    }

    /**
     * Sets the search scope: one level under the base distinguished name
     * or all the subtree. Required by LdapDirectorySettings interface.
     *
     * @param scope the new search scope
     *
     * @see net.java.sip.communicator.service.ldap.LdapConstants.Scope
     * @see LdapDirectorySettings#setScope
     */
    public void setScope(Scope scope)
    {
        this.scope = scope;
    }

    /**
     * Returns the global prefix to be used when calling phones from this ldap
     * source.
     *
     * @return the global prefix to be used when calling phones from this ldap
     * source
     */
    public String getGlobalPhonePrefix()
    {
        return globalPhonePrefix;
    }

    /**
     * Sets the global prefix to be used when calling phones from this ldap
     * source.
     *
     * @param prefix the global prefix to be used when calling phones from this
     * ldap source
     */
    public void setGlobalPhonePrefix(String prefix)
    {
        this.globalPhonePrefix = prefix;
    }

    /**
     * Checks if both LdapDirectorySettings instance have the same content
     *
     * @return whether both LdapDirectorySettings instance have the same content
     *
     * @see java.lang.Object#equals
     */
    public boolean equals(LdapDirectorySettings other)
    {
        /* enabled is not in equals on purpose */

        return getName().equals(other.getName()) &&
            getHostname().equals(other.getHostname()) &&
            getEncryption().equals(other.getEncryption()) &&
            getPort() == other.getPort() &&
            getAuth().equals(other.getAuth()) &&
            getBindDN().equals(other.getBindDN()) &&
            getPassword().equals(other.getPassword()) &&
            getBaseDN().equals(other.getBaseDN()) &&
            getScope().equals(other.getScope()) &&
            getGlobalPhonePrefix().equals(other.getGlobalPhonePrefix());
    }

    /**
     * Returns the hash code for this instance.
     * It has to be consistent with equals.
     *
     * @return the hash code dor this instance
     *
     * @see java.lang.Object#hashCode
     */
    public int hashCode()
    {
        /* enabled is not in the hashcode on purpose */

        int hash = 7;
        hash = 31 * hash + (null == getName() ? 0 :
            getName().hashCode());
        hash = 31 * hash + (null == getHostname() ? 0 :
            getHostname().hashCode());
        hash = 31 * hash + (null == getEncryption() ? 0 :
            getEncryption().hashCode());
        hash = 31 * hash + this.getPort();
        hash = 31 * hash + (null == getAuth() ? 0 :
            getAuth().hashCode());
        hash = 31 * hash + (null == getBindDN() ? 0 :
            getBindDN().hashCode());
        hash = 31 * hash + (null == getPassword() ? 0 :
            getPassword().hashCode());
        hash = 31 * hash + (null == getScope() ? 0 :
            getScope().hashCode());
        hash = 31 * hash + (null == getBaseDN() ? 0 :
            getBaseDN().hashCode());
        hash = 31 * hash + (null == getGlobalPhonePrefix() ? 0 :
            getGlobalPhonePrefix().hashCode());
        return hash;
    }

    /**
     * Returns mail fields that we will lookup.
     *
     * @return mail fields that we will lookup
     */
    public List<String> getMailSearchFields()
    {
        return mapAttributes.get(LdapConstants.EMAIL);
    }

    /**
     * Set mail fields that we will lookup.
     *
     * @param list of mail fields that we will lookup
     */
    public void setMailSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.EMAIL, list);
    }

    /**
     * Returns mail suffix.
     *
     * @return mail suffix
     */
    public String getMailSuffix()
    {
        return mailSuffix;
    }

    /**
     * Set mail suffix.
     *
     * @param suffix mail suffix
     */
    public void setMailSuffix(String suffix)
    {
        this.mailSuffix = suffix;
    }

    /**
     * Returns work phone fields that we will lookup.
     *
     * @return work phone fields that we will lookup
     */
    public List<String> getWorkPhoneSearchFields()
    {
        return mapAttributes.get(LdapConstants.WORKPHONE);
    }

    /**
     * Set work phone fields that we will lookup.
     *
     * @param list of work phone fields that we will lookup
     */
    public void setWorkPhoneSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.WORKPHONE, list);
    }

    /**
     * Returns mobile phone fields that we will lookup.
     *
     * @return mobile phone fields that we will lookup
     */
    public List<String> getMobilePhoneSearchFields()
    {
        return mapAttributes.get(LdapConstants.MOBILEPHONE);
    }

    /**
     * Set mobile phone fields that we will lookup.
     *
     * @param list of mobile phone fields that we will lookup
     */
    public void setMobilePhoneSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.MOBILEPHONE, list);
    }

    /**
     * Returns home phone fields that we will lookup.
     *
     * @return home phone fields that we will lookup
     */
    public List<String> getHomePhoneSearchFields()
    {
        return mapAttributes.get(LdapConstants.HOMEPHONE);
    }

    /**
     * Set home phone fields that we will lookup.
     *
     * @param list of home phone fields that we will lookup
     */
    public void setHomePhoneSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.HOMEPHONE, list);
    }

    /**
     * Returns title fields that we will lookup.
     *
     * @return title fields that we will lookup
     */
    public List<String> getTitleSearchFields()
    {
        return mapAttributes.get(LdapConstants.TITLE);
    }

    /**
     * Set title fields that we will lookup.
     *
     * @param list of title fields that we will lookup
     */
    public void setTitleSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.TITLE, list);
    }

    /**
     * Returns display name fields that we will lookup.
     *
     * @return display name fields that we will lookup
     */
    public List<String> getDisplayNameSearchFields()
    {
        return mapAttributes.get(LdapConstants.NICKNAME);
    }

    /**
     * Set display name fields that we will lookup.
     *
     * @param list of display name fields that we will lookup
     */
    public void setDisplayNameSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.NICKNAME, list);
    }

    /**
     * Returns first name fields that we will lookup.
     *
     * @return first name fields that we will lookup
     */
    public List<String> getFirstNameSearchFields()
    {
        return mapAttributes.get(LdapConstants.FIRSTNAME);
    }

    /**
     * Set first name fields that we will lookup.
     *
     * @param list of first name fields that we will lookup
     */
    public void setFirstNameSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.FIRSTNAME, list);
    }

    /**
     * Returns last name fields that we will lookup.
     *
     * @return last name fields that we will lookup
     */
    public List<String> getLastNameSearchFields()
    {
        return mapAttributes.get(LdapConstants.LASTNAME);
    }

    /**
     * Set last name fields that we will lookup.
     *
     * @param list of last name fields that we will lookup
     */
    public void setLastNameSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.LASTNAME, list);
    }

    /**
     * Returns organization fields that we will lookup.
     *
     * @return organization fields that we will lookup
     */
    public List<String> getOrganizationSearchFields()
    {
        return mapAttributes.get(LdapConstants.ORG);
    }

    /**
     * Set organization fields that we will lookup.
     *
     * @param list of organization fields that we will lookup
     */
    public void setOrganizationSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.ORG, list);
    }

    /**
     * Returns department fields that we will lookup.
     *
     * @return department fields that we will lookup
     */
    public List<String> getDepartmentSearchFields()
    {
        return mapAttributes.get(LdapConstants.DEPARTMENT);
    }

    /**
     * Set department fields that we will lookup.
     *
     * @param list of department fields that we will lookup
     */
    public void setDepartmentSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.DEPARTMENT, list);
    }

    /**
     * Returns location fields that we will lookup.
     *
     * @return location fields that we will lookup
     */
    public List<String> getLocationSearchFields()
    {
        return mapAttributes.get(LdapConstants.LOCATION);
    }

    /**
     * Set location fields that we will lookup.
     *
     * @param list of location fields that we will lookup
     */
    public void setLocationSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.LOCATION, list);
    }

    /**
     * Returns other (i.e. not work/home/mobile) phone fields that we will
     * lookup.
     *
     * @return other phone fields that we will lookup
     */
    public List<String> getOtherPhoneSearchFields()
    {
        return mapAttributes.get(LdapConstants.OTHERPHONE);
    }

    /**
     * Set other (i.e. not work/home/mobile) phone fields that we will lookup.
     *
     * @param list of other phone fields that we will lookup
     */
    public void setOtherPhoneSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.OTHERPHONE, list);
    }

    /**
     * Returns Jabber IM fields that we will lookup.
     *
     * @return Jabber IM fields that we will lookup
     */
    public List<String> getJabberSearchFields()
    {
        return mapAttributes.get(LdapConstants.JABBER);
    }

    /**
     * Set Jabber IM fields that we will lookup.
     *
     * @param list of Jabber IM fields that we will lookup
     */
    public void setJabberSearchFields(List<String> list)
    {
        mapAttributes.put(LdapConstants.JABBER, list);
    }

    /**
     * Validate settings to ensure LDAP service can safely run.
     *
     * @exception IllegalArgumentException if a settings issue is found.
     */
    public void validateSettings()
    {
        if(!textHasContent(getName()))
            throw new IllegalArgumentException("name has no content.");
        if(!textHasContent(getHostname()))
            throw new IllegalArgumentException("Hostname has no content.");
        if(getAuth() != Auth.NONE && !textHasContent(getBindDN()))
            throw new IllegalArgumentException("Bind DN has no content.");
        if(getAuth() != Auth.NONE && getPassword() == null)
            throw new IllegalArgumentException("password is null.");
        if(getPort() < 0 || getPort() > 65535)
            throw new IllegalArgumentException("Illegal port number.");
        if(getBaseDN() == null)
            throw new IllegalArgumentException("Base DN has no content.");
    }

    /**
     * Merge String elements from a list to a single String separated by space.
     *
     * @param lst list of <tt>String</tt>s
     * @return <tt>String</tt>
     */
    public static String mergeStrings(List<String> lst)
    {
        StringBuilder bld = new StringBuilder();

        for(String s : lst)
        {
            bld.append(s).append(" ");
        }

        return bld.toString();
    }

    /**
     * Merge String elements separated by space into a List.
     *
     * @param attrs <tt>String</tt>
     * @return list of <tt>String</tt>
     */
    public static List<String> mergeString(String attrs)
    {
        StringTokenizer token = new StringTokenizer(attrs, " ");
        List<String> lst = new ArrayList<>();

        while(token.hasMoreTokens())
        {
            lst.add(token.nextToken());
        }

        return lst;
    }

    /**
     * Loads the settings from the config files into the LdapDirectorySetting.
     *
     * @see LdapDirectorySettings#persistentLoad
     */
    public void persistentLoad()
    {
        CredentialsStorageService credentialsService = LdapActivator.getCredentialsService();

        // The account name should be stored under the prefix with no suffix.
        // We actually override the server value with a hardcoded value in ProvisioningServiceImpl.processReceivedConfig().
        // If it's not there for whatever reason, just use the empty string.
        String ldapName = configService.user().getString(ldapConfigPrefix, "");
        setName(ldapName);

        setEnabled(configService.user().getBoolean(ldapConfigPrefix + ".enabled", true));

        setHostname(configService.user().getString(ldapConfigPrefix + ".hostname",""));

        setEncryption(configService.user().getEnum(
                Encryption.class,
                ldapConfigPrefix + ".encryption",
                Encryption.defaultValue()));

        setPort(configService.user().getInt(ldapConfigPrefix + ".port", 0));

        setAuth(configService.user().getEnum(
            Auth.class,
            ldapConfigPrefix + ".auth",
            Auth.defaultValue()));

        setBindDN(configService.user().getString(ldapConfigPrefix + ".bindDN", ""));
        String password = credentialsService.user().loadPassword(ldapConfigPrefix);

        if (password == null)
        {
            setPassword("");
        }
        else
        {
            setPassword(password);
        }

        setScope(configService.user().getEnum(
            Scope.class,
            ldapConfigPrefix + ".scope",
            Scope.defaultValue()));

        setBaseDN(configService.user().getString(ldapConfigPrefix + ".baseDN", ""));

        String mailSuffix = configService.user().getString(ldapConfigPrefix + ".overridemailsuffix");

        if(mailSuffix != null)
        {
            setMailSuffix(mailSuffix);
        }

        String globalPhonePrefix = configService.user().getString(ldapConfigPrefix + ".globalPhonePrefix");

        if (globalPhonePrefix != null)
        {
            setGlobalPhonePrefix(globalPhonePrefix);
        }

        Map<String, String> map = new HashMap<>();
        map.put(LdapConstants.EMAIL, ".overridemail");
        map.put(LdapConstants.WORKPHONE, ".overrideworkphone");
        map.put(LdapConstants.MOBILEPHONE, ".overridemobilephone");
        map.put(LdapConstants.HOMEPHONE, ".overridehomephone");
        map.put(LdapConstants.TITLE, ".overridetitle");
        map.put(LdapConstants.NICKNAME, ".overridedisplayname");
        map.put(LdapConstants.FIRSTNAME, ".overridefirstname");
        map.put(LdapConstants.LASTNAME, ".overridelastname");
        map.put(LdapConstants.ORG, ".overrideorganization");
        map.put(LdapConstants.DEPARTMENT, ".overridedepartment");
        map.put(LdapConstants.LOCATION, ".overridelocation");
        map.put(LdapConstants.OTHERPHONE, ".overrideotherphone");
        map.put(LdapConstants.JABBER, ".overridejabber");

        for (String key : map.keySet())
        {
            // If a configured value can be found, merge the list of strings
            // and save it on the attribute map.
            String ret =
                (String)configService.user().getProperty(ldapConfigPrefix + map.get(key));

            if (ret != null)
            {
                mapAttributes.put(key, mergeString(ret));
            }
        }

        logger.info("Loaded LDAP settings for " + name + " from config" +
                     " with prefix: " + ldapConfigPrefix);
    }

    /**
     * meant for debugging
     *
     * @return a string description of this instance
     */
    public String toString()
    {
        return "LdapDirectorySettings: {\n " +
            getName() + ", \n" +
            getHostname() + ", \n" +
            getEncryption() + ", \n" +
            getPort() + ", \n" +
            getAuth() + ", \n" +
            getBindDN() + ", \n" +
            getPassword() + ", \n" +
            getBaseDN() + ", \n" +
            getGlobalPhonePrefix() + " \n}";
    }

    public LdapDirectorySettings clone()
    {
        return new LdapDirectorySettingsImpl(this);
    }

    /**
     * Used to check method input parameters
     *
     * @return whether the text is not empty
     */
    private boolean textHasContent(String aText)
    {
        return (aText != null) && !aText.trim().isEmpty();
    }
}
