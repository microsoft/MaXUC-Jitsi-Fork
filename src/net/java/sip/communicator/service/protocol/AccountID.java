/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The AccountID is an account identifier that, uniquely represents a specific
 * user account over a specific protocol. The class needs to be extended by
 * every protocol implementation because of its protected
 * constructor. The reason why this constructor is protected is mostly avoiding
 * confusion and letting people (using the protocol provider service) believe
 * that they are the ones who are supposed to instantiate the accountid class.
 * <p>
 * Every instance of the <tt>ProtocolProviderService</tt>, created through the
 * ProtocolProviderFactory is assigned an AccountID instance, that uniquely
 * represents it and whose string representation (obtained through the
 * getAccountUID() method) can be used for identification of persistently stored
 * account details.
 * <p>
 * Account id's are guaranteed to be different for different accounts and in the
 * same time are bound to be equal for multiple installations of the same
 * account.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public abstract class AccountID
{
    /**
     * The <tt>Logger</tt> used by the <tt>AccountID</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(AccountID.class);

    private static final ConfigurationService configService =
                            ProtocolProviderActivator.getConfigurationService();

    /**
     * The protocol display name. In the case of overridden protocol name this
     * would be the new name.
     */
    private final String protocolDisplayName;

    /**
     * The real protocol name.
     */
    private final String protocolName;

    /**
     * Allows a specific set of account properties to override a given default
     * protocol name (e.g. account registration wizards which want to present a
     * well-known protocol name associated with the account that is different
     * from the name of the effective protocol).
     * <p>
     * Note: The logic of the SIP protocol implementation at the time of this
     * writing modifies <tt>accountProperties</tt> to contain the default
     * protocol name if an override hasn't been defined. Since the desire is to
     * enable all account registration wizards to override the protocol name,
     * the current implementation places the specified
     * <tt>defaultProtocolName</tt> in a similar fashion.
     * </p>
     *
     * @param accountProperties a Map containing any other protocol and
     * implementation specific account initialization properties
     * @param defaultProtocolName the protocol name to be used in case
     * <tt>accountProperties</tt> doesn't provide an overriding value
     * @return the protocol name
     */
    private static String getOverriddenProtocolName(
            Map<String, String> accountProperties, String defaultProtocolName)
    {
        String key = ProtocolProviderFactory.PROTOCOL;
        String protocolName = accountProperties.get(key);
        if ((protocolName == null) && (defaultProtocolName != null))
        {
            protocolName = defaultProtocolName;
            accountProperties.put(key, protocolName);
        }
        return protocolName;
    }

    /**
     * Contains all implementation specific properties that define the account.
     * The exact names of the keys are protocol (and sometimes implementation)
     * specific.
     * Currently, only String property keys and values will get properly stored.
     * If you need something else, please consider converting it through custom
     * accessors (get/set) in your implementation.
     */
    protected Map<String, String> accountProperties = null;

    /**
     * A String uniquely identifying the user for this particular account.
     */
    private final String userID;

    /**
     * A String uniquely identifying this account, that can also be used for
     * storing and unambiguously retrieving details concerning it.
     */
    private final String accountUID;

    /**
     * The name of the service that defines the context for this account.
     */
    private final String serviceName;

    /**
     * Property to disable/enable the displaying of the protocol name in the GUI
     */
    private static final String PNAME_HIDE_PROTOCOL_NAME =
        "net.java.sip.communicator.service.protocol.HIDE_PROTOCOL_NAME";

    /**
     * Creates an account id for the specified provider userid and
     * accountProperties.
     * If account uid exists in account properties, we are loading the account
     * and so load its value from there, prevent changing account uid
     * when server changed (serviceName has changed).
     * @param userID a String that uniquely identifies the user.
     * @param accountProperties a Map containing any other protocol and
     * implementation specific account initialization properties
     * @param protocolName the name of the protocol implemented by the provider
     * that this id is meant for.
     * @param serviceName the name of the service (e.g. iptel.org, jabber.org,
     * icq.com) that this account is registered with.
     */
    protected AccountID( String userID,
                         Map<String, String> accountProperties,
                         String protocolName,
                         String serviceName)
    {
        /*
         * Allow account registration wizards to override the default protocol
         * name through accountProperties for the purposes of presenting a
         * well-known protocol name associated with the account that is
         * different from the name of the effective protocol.
         */
        this.protocolDisplayName
            = getOverriddenProtocolName(accountProperties, protocolName);

        this.protocolName = protocolName;
        this.userID = userID;
        this.accountProperties
            = new HashMap<>(accountProperties);
        this.serviceName = serviceName;

        String existingAccountUID =
                accountProperties.get(ProtocolProviderFactory.ACCOUNT_UID);

        if(existingAccountUID == null)
        {
            //create a unique identifier string
            this.accountUID
                = protocolDisplayName
                    + ":"
                    + userID
                    + "@"
                    + ((serviceName == null) ? "" : serviceName);
        }
        else
        {
            this.accountUID = existingAccountUID;
        }
    }

    /**
     * Returns the user id associated with this account.
     *
     * @return A String identifying the user inside this particular service.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Returns a name that can be displayed to the user when referring to this
     * account.
     *
     * @return A String identifying the user inside this particular service.
     */
    public String getDisplayName()
    {
        // If the ACCOUNT_DISPLAY_NAME property has been set for this account
        // we'll be using it as a display name.
        String key = ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME;
        String accountDisplayName = accountProperties.get(key);
        if (accountDisplayName != null && accountDisplayName.length() > 0)
        {
            return accountDisplayName;
        }

        // Otherwise construct a display name.
        String returnValue = getUserID();
        String protocolName = getProtocolDisplayName();

        boolean hideProtocolName = configService.user().getBoolean(
                                               PNAME_HIDE_PROTOCOL_NAME, false);

        if (protocolName != null &&
            protocolName.trim().length() > 0 &&
            !hideProtocolName)
        {
            returnValue += " (" + protocolName + ")";
        }

        return returnValue;
    }

    /**
     * Returns the display name of the protocol.
     *
     * @return the display name of the protocol
     */
    public String getProtocolDisplayName()
    {
        return protocolDisplayName;
    }

    /**
     * Returns the name of the protocol.
     *
     * @return the name of the protocol
     */
    public String getProtocolName()
    {
        return protocolName;
    }

    /**
     * Returns a String uniquely identifying this account, guaranteed to remain
     * the same across multiple installations of the same account and to always
     * be unique for differing accounts.
     * @return String
     */
    public String getAccountUniqueID()
    {
        return accountUID;
    }

    /**
     * Returns a Map containing protocol and implementation account
     * initialization properties.
     * @return a Map containing protocol and implementation account
     * initialization properties.
     */
    public Map<String, String> getAccountProperties()
    {
        return new HashMap<>(accountProperties);
    }

    /**
     * Returns the specific account property.
     *
     * @param key property key
     * @return property value corresponding to property key
     */
    public Object getAccountProperty(Object key)
    {
        return accountProperties.get(key);
    }

    /**
     * Returns the specific account property.
     *
     * @param key property key
     * @param defaultValue default value if the property does not exist
     * @return property value corresponding to property key
     */
    public boolean getAccountPropertyBoolean(Object key, boolean defaultValue)
    {
        String value = getAccountPropertyString(key);
        return (value == null) ? defaultValue : Boolean.parseBoolean(value);
    }

    /**
     * Gets the value of a specific property as a signed decimal integer. If the
     * specified property key is associated with a value in this
     * <tt>AccountID</tt>, the string representation of the value is parsed into
     * a signed decimal integer according to the rules of
     * {@link Integer#parseInt(String)} . If parsing the value as a signed
     * decimal integer fails or there is no value associated with the specified
     * property key, <tt>defaultValue</tt> is returned.
     *
     * @param key the key of the property to get the value of as a
     * signed decimal integer
     * @param defaultValue the value to be returned if parsing the value of the
     * specified property key as a signed decimal integer fails or there is no
     * value associated with the specified property key in this
     * <tt>AccountID</tt>
     * @return the value of the property with the specified key in this
     * <tt>AccountID</tt> as a signed decimal integer; <tt>defaultValue</tt> if
     * parsing the value of the specified property key fails or no value is
     * associated in this <tt>AccountID</tt> with the specified property name
     */
    public int getAccountPropertyInt(Object key, int defaultValue)
    {
        String stringValue = getAccountPropertyString(key);
        int intValue = defaultValue;

        if ((stringValue != null) && (stringValue.length() > 0))
        {
            try
            {
                intValue = Integer.parseInt(stringValue);
            }
            catch (NumberFormatException ex)
            {
                logger.error("Failed to parse account property " + key
                    + " value " + stringValue + " as an integer", ex);
            }
        }
        return intValue;
    }

    /**
     * Returns the account property string corresponding to the given key.
     *
     * @param key the key, corresponding to the property string we're looking
     * for
     * @return the account property string corresponding to the given key
     */
    public String getAccountPropertyString(Object key)
    {
        Object value = getAccountProperty(key);
        return (value == null) ? null : value.toString();
    }

    /**
     * Adds a property to the map of properties for this account identifier.
     *
     * @param key the key of the property
     * @param value the property value
     */
    public void putAccountProperty(String key, String value)
    {
        accountProperties.put(key, value);
    }

    /**
     * Removes specified account property.
     * @param key the key to remove.
     */
    public void removeAccountProperty(String key)
    {
        accountProperties.remove(key);
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <tt>java.util.Hashtable</tt>.
     * <p>
     * @return  a hash code value for this object.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.util.Hashtable
     */
    public int hashCode()
    {
        return (accountUID == null)? 0 : accountUID.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this account id.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     * @see     #hashCode()
     * @see     java.util.Hashtable
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        return (obj != null)
            && getClass().isInstance(obj)
            && userID.equals(((AccountID)obj).userID);
    }

    /**
     * Returns a string representation of this account id (same as calling
     * getAccountUniqueID()).
     *
     * @return  a string representation of this account id.
     */
    public String toString()
    {
        return getAccountUniqueID();
    }

    /**
     * Returns the name of the service that defines the context for this
     * account. Often this name would be an sqdn or even an ipaddress but this
     * would not always be the case (e.g. p2p providers may return a name that
     * does not directly correspond to an IP address or host name).
     * <p>
     * @return the name of the service that defines the context for this
     * account.
     */
    public String getService()
    {
        return this.serviceName;
    }

    /**
     * Returns a string that could be directly used (or easily converted to) an
     * address that other users of the protocol can use to communicate with us.
     * By default this string is set to userid@servicename. Protocol
     * implementors should override it if they'd need it to respect a different
     * syntax.
     *
     * @return a String in the form of userid@service that other protocol users
     * should be able to parse into a meaningful address and use it to
     * communicate with us.
     */
    public String getAccountAddress()
    {
        String userID = getUserID();
        return (userID.indexOf('@') > 0) ? userID
            : (userID + "@" + getService());
    }

    /**
     * Indicates if this account is currently enabled.
     * @return <tt>true</tt> if this account is enabled, <tt>false</tt> -
     * otherwise.
     */
    public boolean isEnabled()
    {
        return !getAccountPropertyBoolean(
            ProtocolProviderFactory.IS_ACCOUNT_DISABLED, false);
    }

    /**
     * Indicates if this account is currently being reloaded (i.e.
     * automatically disabled then immediately re-enabled).  The reconnect
     * plugin may choose to do this if an account is failing to reconnect by
     * simply re-registering.
     * @return <tt>true</tt> if this account is reloading, <tt>false</tt> -
     * otherwise.
     */
    public boolean isReloading()
    {
        return getAccountPropertyBoolean(
            ProtocolProviderFactory.IS_ACCOUNT_RELOADING, false);
    }

    /**
     * Set the account properties.
     *
     * @param accountProperties the properties of the account
     */
    public void setAccountProperties(Map<String, String> accountProperties)
    {
        this.accountProperties = accountProperties;
    }

    /**
     * Returns if the encryption protocol given in parameter is enabled.
     *
     * @param encryptionProtocolName The name of the encryption protocol
     * ("ZRTP", "SDES" or "MIKEY").
     */
    public boolean isEncryptionProtocolEnabled(String encryptionProtocolName)
    {
        // The default value is false, except for ZRTP.
        boolean defaultValue = (encryptionProtocolName.equals("ZRTP"));
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS
                    + "."
                    + encryptionProtocolName,
                defaultValue);
    }

    /**
     * Returns PREFERRED_TRANSPORT for the account, if such a property is set.
     */
    public String getPreferredTransport()
    {
        return getAccountPropertyString(ProtocolProviderFactory.PREFERRED_TRANSPORT);
    }

    /**
     * Sorts the enabled encryption protocol list given in parameter to match
     * the preferences set for this account.
     *
     * @return Sorts the enabled encryption protocol list given in parameter to
     * match the preferences set for this account.
     */
    public List<String> getSortedEnabledEncryptionProtocolList()
    {
        Map<String, Integer> encryptionProtocols
            = getIntegerPropertiesByPrefix(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL,
                    true);
        Map<String, Boolean> encryptionProtocolStatus
            = getBooleanPropertiesByPrefix(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS,
                    true,
                    false);

        List<String> sortedEncryptionProtocols
            = new ArrayList<>(encryptionProtocols.size());

        // First: add all protocol in the right order.
        for (Map.Entry<String, Integer> e : encryptionProtocols.entrySet())
        {
            int index = e.getValue();

            // If the key is set.
            if (index != -1)
            {
                if (index > sortedEncryptionProtocols.size())
                    index = sortedEncryptionProtocols.size();

                String name = e.getKey();

                sortedEncryptionProtocols.add(index, name);
            }
        }

        // Second: remove all disabled protocol.
        int namePrefixLength
            = ProtocolProviderFactory.ENCRYPTION_PROTOCOL.length() + 1;

        for (Iterator<String> i = sortedEncryptionProtocols.iterator();
                i.hasNext();)
        {
            String name = i.next().substring(namePrefixLength);

            if (!encryptionProtocolStatus.get(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS
                        + "."
                        + name))
            {
                i.remove();
            }
        }

        return sortedEncryptionProtocols;
    }

    /**
     * Returns a <tt>java.util.Map</tt> of <tt>String</tt>s containing the
     * all property names that have the specified prefix and <tt>Boolean</tt>
     * containing the value for each property selected. Depending on the value
     * of the <tt>exactPrefixMatch</tt> parameter the method will (when false)
     * or will not (when exactPrefixMatch is true) include property names that
     * have prefixes longer than the specified <tt>prefix</tt> param.
     * <p>
     * Example:
     * <p>
     * Imagine a configuration service instance containing 2 properties
     * only:<br>
     * <code>
     * net.java.sip.communicator.PROP1=value1<br>
     * net.java.sip.communicator.service.protocol.PROP1=value2
     * </code>
     * <p>
     * A call to this method with a prefix="net.java.sip.communicator" and
     * exactPrefixMatch=true would only return the first property -
     * net.java.sip.communicator.PROP1, whereas the same call with
     * exactPrefixMatch=false would return both properties as the second prefix
     * includes the requested prefix string.
     * <p>
     * @param prefix a String containing the prefix (the non dotted non-caps
     * part of a property name) that we're looking for.
     * @param exactPrefixMatch a boolean indicating whether the returned
     * property names should all have a prefix that is an exact match of the
     * the <tt>prefix</tt> param or whether properties with prefixes that
     * contain it but are longer than it are also accepted.
     * @param defaultValue the default value if the key is not set.
     * @return a <tt>java.util.Map</tt> containing all property name String-s
     * matching the specified conditions and the corresponding values as
     * Boolean.
     */
    public Map<String, Boolean> getBooleanPropertiesByPrefix(
            String prefix,
            boolean exactPrefixMatch,
            boolean defaultValue)
    {
        List<String> propertyNames
            = getPropertyNamesByPrefix(prefix, exactPrefixMatch);
        Map<String, Boolean> properties
            = new HashMap<>(propertyNames.size());

        for (String propertyName : propertyNames)
        {
            properties.put(
                    propertyName,
                    getAccountPropertyBoolean(propertyName, defaultValue));
        }

        return properties;
    }

    /**
     * Returns a <tt>java.util.Map</tt> of <tt>String</tt>s containing the
     * all property names that have the specified prefix and <tt>Integer</tt>
     * containing the value for each property selected. Depending on the value
     * of the <tt>exactPrefixMatch</tt> parameter the method will (when false)
     * or will not (when exactPrefixMatch is true) include property names that
     * have prefixes longer than the specified <tt>prefix</tt> param.
     * <p>
     * Example:
     * <p>
     * Imagine a configuration service instance containing 2 properties
     * only:<br>
     * <code>
     * net.java.sip.communicator.PROP1=value1<br>
     * net.java.sip.communicator.service.protocol.PROP1=value2
     * </code>
     * <p>
     * A call to this method with a prefix="net.java.sip.communicator" and
     * exactPrefixMatch=true would only return the first property -
     * net.java.sip.communicator.PROP1, whereas the same call with
     * exactPrefixMatch=false would return both properties as the second prefix
     * includes the requested prefix string.
     * <p>
     * @param prefix a String containing the prefix (the non dotted non-caps
     * part of a property name) that we're looking for.
     * @param exactPrefixMatch a boolean indicating whether the returned
     * property names should all have a prefix that is an exact match of the
     * the <tt>prefix</tt> param or whether properties with prefixes that
     * contain it but are longer than it are also accepted.
     * @return a <tt>java.util.Map</tt> containing all property name String-s
     * matching the specified conditions and the corresponding values as
     * Integer.
     */
    public Map<String, Integer> getIntegerPropertiesByPrefix(
            String prefix,
            boolean exactPrefixMatch)
    {
        List<String> propertyNames
            = getPropertyNamesByPrefix(prefix, exactPrefixMatch);
        Map<String, Integer> properties
            = new HashMap<>(propertyNames.size());

        for (String propertyName : propertyNames)
        {
            properties.put(
                    propertyName,
                    getAccountPropertyInt(propertyName, -1));
        }

        return properties;
    }

    /**
     * Returns a <tt>java.util.List</tt> of <tt>String</tt>s containing the
     * all property names that have the specified prefix. Depending on the value
     * of the <tt>exactPrefixMatch</tt> parameter the method will (when false)
     * or will not (when exactPrefixMatch is true) include property names that
     * have prefixes longer than the specified <tt>prefix</tt> param.
     * <p>
     * Example:
     * <p>
     * Imagine a configuration service instance containing 2 properties
     * only:<br>
     * <code>
     * net.java.sip.communicator.PROP1=value1<br>
     * net.java.sip.communicator.service.protocol.PROP1=value2
     * </code>
     * <p>
     * A call to this method with a prefix="net.java.sip.communicator" and
     * exactPrefixMatch=true would only return the first property -
     * net.java.sip.communicator.PROP1, whereas the same call with
     * exactPrefixMatch=false would return both properties as the second prefix
     * includes the requested prefix string.
     * <p>
     * @param prefix a String containing the prefix (the non dotted non-caps
     * part of a property name) that we're looking for.
     * @param exactPrefixMatch a boolean indicating whether the returned
     * property names should all have a prefix that is an exact match of the
     * the <tt>prefix</tt> param or whether properties with prefixes that
     * contain it but are longer than it are also accepted.
     * @return a <tt>java.util.List</tt>containing all property name String-s
     * matching the specified conditions.
     */
    public List<String> getPropertyNamesByPrefix(
            String prefix,
            boolean exactPrefixMatch)
    {
        List<String> resultKeySet = new LinkedList<>();

        for (String key : accountProperties.keySet())
        {
            int ix = key.lastIndexOf('.');

            if(ix == -1)
                continue;

            String keyPrefix = key.substring(0, ix);

            if(exactPrefixMatch)
            {
                if(prefix.equals(keyPrefix))
                    resultKeySet.add(key);
            }
            else
            {
                if(keyPrefix.startsWith(prefix))
                    resultKeySet.add(key);
            }
        }

        return resultKeySet;
    }

    /**
     * Deletes the account associated with this AccountID and, optionally,
     * uninstalls it via its ProtocolProviderFactory.
     *
     * @param uninstallAccount if true, this method will also uninstall the
     * account via its ProtocolProviderFactory.
     */
    public void deleteAccount(boolean uninstallAccount)
    {
        // Delete all config for this account, including the reconnect plugin
        // config, and disable it, so that the reconnect plugin stops trying to
        // restart the account.
        logger.info("Deleting config for and disabling account for " +
                                    protocolName + ", accountID " + accountUID);
        configService.user().removeAccountConfigForProtocol(protocolName, true);
        putAccountProperty(
            ProtocolProviderFactory.IS_ACCOUNT_DISABLED, String.valueOf(true));

        // If the account manager has started, kick it to unload the deleted
        // account.
        AccountManager accountManager = ProtocolProviderActivator.getAccountManager();
        if (accountManager != null)
        {
            logger.debug("Unloading account: " + userID);
            accountManager.accountsChanged();
        }

        if (uninstallAccount)
        {
            ProtocolProviderFactory providerFactory =
                AccountUtils.getProtocolProviderFactory(protocolName);

            if (providerFactory != null)
            {
                logger.debug("Uninstalling account: " + accountUID);
                providerFactory.uninstallAccount(this);
            }
        }
    }
}
