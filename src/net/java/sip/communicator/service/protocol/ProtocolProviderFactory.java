/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.lang.reflect.*;
import java.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

/**
 * The ProtocolProviderFactory is what actually creates instances of a
 * ProtocolProviderService implementation. A provider factory would register,
 * persistently store, and remove when necessary, ProtocolProviders. The way
 * things are in the SIP Communicator, a user account is represented (in a 1:1
 * relationship) by  an AccountID and a ProtocolProvider. In other words - one
 * would have as many protocol providers installed in a given moment as they
 * would user account registered through the various services.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public abstract class ProtocolProviderFactory
{
    /**
     * The <tt>Logger</tt> used by the <tt>ProtocolProviderFactory</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderFactory.class);

    /**
     * Then name of a property which represents a password.
     */
    public static final String PASSWORD = "PASSWORD"; // lgtm[java/hardcoded-password-field] False positive: it's a property name, not a password value

    /**
     * The name of a property representing the name of the protocol for an
     * ProtocolProviderFactory.
     */
    public static final String PROTOCOL = "PROTOCOL_NAME";

    /**
     * The name of a property representing the path to protocol icons.
     */
    public static final String PROTOCOL_ICON_PATH = "PROTOCOL_ICON_PATH";

    /**
     * The name of a property representing the path to the account icon to
     * be used in the user interface, when the protocol provider service is not
     * available.
     */
    public static final String ACCOUNT_ICON_PATH = "ACCOUNT_ICON_PATH";

    /**
     * The name of a property which represents the AccountID of a
     * ProtocolProvider and that, together with a password is used to login
     * on the protocol network.
     */
    public static final String USER_ID = "USER_ID";

    /**
     * The name that should be displayed to others when we are calling or
     * writing them.
     */
    public static final String DISPLAY_NAME = "DISPLAY_NAME";

    /**
     * The name that should be displayed to the user on call via and chat via
     * lists.
     */
    public static final String ACCOUNT_DISPLAY_NAME = "ACCOUNT_DISPLAY_NAME";

    /**
     * The name of the property under which we store protocol AccountID-s.
     */
    public static final String ACCOUNT_UID = "ACCOUNT_UID";

    /**
     * The name of the property under which we store protocol the address of
     * a protocol centric entity (any protocol server).
     */
    public static final String SERVER_ADDRESS = "SERVER_ADDRESS";

    /**
     * The name of the property under which we store the number of the port
     * where the server stored against the SERVER_ADDRESS property is expecting
     * connections to be made via this protocol.
     */
    public static final String SERVER_PORT = "SERVER_PORT";

    /**
     * The name of the property under which we store protocol the address of
     * a protocol proxy.
     */
    public static final String PROXY_ADDRESS = "PROXY_ADDRESS";

    /**
     * The name of the property under which we store the number of the port
     * where the proxy stored against the PROXY_ADDRESS property is expecting
     * connections to be made via this protocol.
     */
    public static final String PROXY_PORT = "PROXY_PORT";

    /**
     * The name of the property which defines whether proxy is auto configured
     * by the protocol by using known methods such as specific DNS queries.
     */
    public static final String PROXY_AUTO_CONFIG = "PROXY_AUTO_CONFIG";

    /**
     * The property indicating the preferred UDP and TCP
     * port to bind to for clear communications.
     */
    public static final String PREFERRED_CLEAR_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.SIP_PREFERRED_CLEAR_PORT";

    /**
     * The property indicating the preferred TLS (TCP)
     * port to bind to for secure communications.
     */
    public static final String PREFERRED_SECURE_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.SIP_PREFERRED_SECURE_PORT";

    /**
    * The name of the property under which we store the authorization name
    * for the proxy stored against the PROXY_ADDRESS property.
    */
   public static final String AUTHORIZATION_NAME = "AUTHORIZATION_NAME";

    /**
     * The name of the property that indicates whether loose routing should be
     * forced for all traffic in an account, rather than routing through an
     * outbound proxy which is the default for Jitsi.
     */
    public static final String FORCE_PROXY_BYPASS = "FORCE_PROXY_BYPASS";

    /**
     * The name of the property under which we store the user preference for a
     * transport protocol to use (i.e. tcp or udp).
     */
    public static final String PREFERRED_TRANSPORT = "PREFERRED_TRANSPORT";

    /**
     * The name of the property under which we store whether we generate
     * resource values or we just use the stored one.
     */
    public static final String AUTO_GENERATE_RESOURCE = "AUTO_GENERATE_RESOURCE";

    /**
     * The name of the property under which we store resources such as the
     * jabber resource property.
     */
    public static final String RESOURCE = "RESOURCE";

    /**
     * The name of the property under which we store resource priority.
     */
    public static final String RESOURCE_PRIORITY = "RESOURCE_PRIORITY";

    /**
     * The name of the property which defines that the call is encrypted by
     * default
     */
    public static final String DEFAULT_ENCRYPTION = "DEFAULT_ENCRYPTION";

    /**
     * The name of the property that indicates the encryption protocols for this
     * account.
     */
    public static final String ENCRYPTION_PROTOCOL = "ENCRYPTION_PROTOCOL";

    /**
     * The name of the property that indicates the status (enabled or disabled)
     * encryption protocols for this account.
     */
    public static final String ENCRYPTION_PROTOCOL_STATUS
        = "ENCRYPTION_PROTOCOL_STATUS";

    /**
     * The name of the property which defines if to include the ZRTP attribute
     * to SIP/SDP
     */
    public static final String DEFAULT_SIPZRTP_ATTRIBUTE =
        "DEFAULT_SIPZRTP_ATTRIBUTE";

    /**
     * The name of the property which defines the ID of the client TLS
     * certificate configuration entry.
     */
    public static final String CLIENT_TLS_CERTIFICATE =
        "CLIENT_TLS_CERTIFICATE";

    /**
     * The name of the property under which we store if the presence is enabled.
     */
    public static final String IS_PRESENCE_ENABLED = "IS_PRESENCE_ENABLED";

    /**
     * The name of the property under which we store if registration state can
     * be used to set the global status.
     */
    public static final String IS_REGISTRATION_STATUS_ENABLED =
        "IS_REGISTRATION_STATUS_ENABLED";

    /**
     * The name of the property under which we store the offline contact polling
     * period for SIMPLE.
     */
    public static final String POLLING_PERIOD = "POLLING_PERIOD";

    /**
     * The name of the property under which we store the chosen default
     * subscription expiration value for SIMPLE.
     */
    public static final String SUBSCRIPTION_EXPIRATION
                                                = "SUBSCRIPTION_EXPIRATION";

    /**
     * Indicates if the server address has been validated.
     */
    public static final String SERVER_ADDRESS_VALIDATED
                                                = "SERVER_ADDRESS_VALIDATED";

    /**
     * Indicates if the server settings are over
     */
    public static final String IS_SERVER_OVERRIDDEN
                                                = "IS_SERVER_OVERRIDDEN";

    /**
     * Indicates a protocol that would not be shown in the user interface as an
     * account.
     */
    public static final String IS_PROTOCOL_HIDDEN = "IS_PROTOCOL_HIDDEN";

    /**
     * Indicates if the given account is the preferred account.
     */
    public static final String IS_PREFERRED_PROTOCOL = "IS_PREFERRED_PROTOCOL";

    /**
     * The name of the property that would indicate if a given account is
     * currently enabled or disabled.
     */
    public static final String IS_ACCOUNT_DISABLED = "IS_ACCOUNT_DISABLED";

    /**
     * The name of the property that would indicate if a given account is
     * currently being reloaded (i.e. automatically disabled then immediately
     * re-enabled).  The reconnect plugin may choose to do this if an account
     * is failing to reconnect by simply re-registering.
     */
    public static final String IS_ACCOUNT_RELOADING = "IS_ACCOUNT_RELOADING";

    /**
     * Address used to reach voicemail box, by services able to
     * subscribe for voicemail new messages notifications.
     */
    public static final String VOICEMAIL_URI = "VOICEMAIL_URI";

    /**
     * Address used to call to hear your messages stored on the server
     * for your voicemail.
     */
    public static final String VOICEMAIL_CHECK_URI = "VOICEMAIL_CHECK_URI";

    /**
     * The sms default server address.
     */
    public static final String SMS_SERVER_ADDRESS = "SMS_SERVER_ADDRESS";

    /**
     * Keep-alive method used by the protocol.
     */
    public static final String KEEP_ALIVE_METHOD = "KEEP_ALIVE_METHOD";

    /**
     * The interval for keep-alives if any.
     */
    public static final String KEEP_ALIVE_INTERVAL = "KEEP_ALIVE_INTERVAL";

    /**
     * The minimal DTMF tone duration.
     */
    public static final String DTMF_MINIMAL_TONE_DURATION
        = "DTMF_MINIMAL_TONE_DURATION";

    /**
     * Paranoia mode when turned on requires all calls to be secure and
     * indicated as such.
     */
    public static final String MODE_PARANOIA = "MODE_PARANOIA";

    /**
     * The name of the property which defines whether 603 Decline is sent
     * instead of 486 Busy Here on call rejection.
     */
    public static final String SEND_DECLINE_ON_CALL_REJECTION =
        "SEND_DECLINE_ON_CALL_REJECTION";

    /**
     * The <code>BundleContext</code> containing (or to contain) the service
     * registration of this factory.
     */
    private final BundleContext bundleContext;

    /**
     * The name of the protocol this factory registers its
     * <code>ProtocolProviderService</code>s with and to be placed in the
     * properties of the accounts created by this factory.
     */
    private final String protocolName;

    /**
     * A Map from AccountID to Felix ServiceRegistration objects. These ServiceRegistration objects
     * are for ProtocolProviderService objects that are registered with Felix.  This Map is thus called
     * registeredAccounts because it contains Accounts with corresponding PPS objects that are registered
     * with Felix.  It has no implication on whether the corresponding PPS object is registered in any other
     * sense (e.g. with a server, as in XMPP or SIP PPS objects).
     *
     * Accounts are added to this Map in loadAccount (mapped to the newly created PPS object that has just been registered with Felix)
     * and removed from the Map in unloadAccount (at the point that PPS object is shutdown and unregistered with Felix).
     *
     * Access to the map should be synchronized on the object itself.
     */
    private final Map<AccountID, ServiceRegistration<?>> registeredAccounts = new Hashtable<>();

    /**
     * The name of the property that indicates the AVP type.
     * <ul>
     * <li>{@link #SAVP_OFF}</li>
     * <li>{@link #SAVP_MANDATORY}</li>
     * <li>{@link #SAVP_OPTIONAL}</li>
     * </ul>
     */
    public static final String SAVP_OPTION = "SAVP_OPTION";

    /**
     * Always use RTP/AVP
     */
    public static final int SAVP_OFF = 0;

    /**
     * Always use RTP/SAVP
     */
    public static final int SAVP_MANDATORY = 1;

    /**
     * Sends two media description, with RTP/SAVP being first.
     */
    public static final int SAVP_OPTIONAL = 2;

    /**
     * The name of the property that defines the enabled SDES cipher suites.
     * Enabled suites are listed as CSV by their RFC name.
     */
    public static final String SDES_CIPHER_SUITES = "SDES_CIPHER_SUITES";

    /**
     * Creates a new <tt>ProtocolProviderFactory</tt>.
     *
     * @param bundleContext the bundle context reference of the service
     * @param protocolName the name of the protocol
     */
    protected ProtocolProviderFactory(BundleContext bundleContext,
        String protocolName)
    {
        this.bundleContext = bundleContext;
        this.protocolName = protocolName;
    }

    /**
     * Gets the <code>BundleContext</code> containing (or to contain) the
     * service registration of this factory.
     *
     * @return the <code>BundleContext</code> containing (or to contain) the
     *         service registration of this factory
     */
    public BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter. Note that account
     * registration is persistent and accounts that are registered during
     * a particular sip-communicator session would be automatically reloaded
     * during all following sessions until they are removed through the
     * removeAccount method.
     *
     * @param userID the user identifier uniquely representing the newly
     * created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation) specific
     * properties defining the new account.
     * @return the AccountID of the newly created account.
     * @throws java.lang.IllegalArgumentException if userID does not correspond
     * to an identifier in the context of the underlying protocol or if
     * accountProperties does not contain a complete set of account installation
     * properties.
     * @throws java.lang.IllegalStateException if the account has already been
     * installed.
     * @throws java.lang.NullPointerException if any of the arguments is null.
     */
    public abstract AccountID installAccount(String userID,
            Map<String, String> accountProperties)
        throws IllegalArgumentException,
               IllegalStateException,
               NullPointerException;

    /**
     * Returns a copy of the list containing the <tt>AccountID</tt>s of all
     * accounts currently registered in this protocol provider.
     * @return a copy of the list containing the <tt>AccountID</tt>s of all
     * accounts currently registered in this protocol provider.
     */
    public ArrayList<AccountID> getRegisteredAccounts()
    {
        synchronized (registeredAccounts)
        {
            return new ArrayList<>(registeredAccounts.keySet());
        }
    }

    /**
     * @param accountID
     * @return True iff the provided AccountID is in the registeredAccounts Map.
     */
    protected boolean isAccountRegistered(AccountID accountID)
    {
        synchronized (registeredAccounts)
        {
            return registeredAccounts.containsKey(accountID);
        }
    }

    /**
     * Returns the ServiceReference for the protocol provider corresponding to
     * the specified accountID or null if the accountID is unknown.
     * @param accountID the accountID of the protocol provider we'd like to get
     * @return a ServiceReference object to the protocol provider with the
     * specified account id and null if the account id is unknown to the
     * provider factory.
     */
    public ServiceReference<?> getProviderForAccount(AccountID accountID)
    {
        ServiceRegistration<?> registration;

        synchronized (registeredAccounts)
        {
            registration =
                registeredAccounts.get(accountID);
        }
        return (registration == null) ? null : registration.getReference();
    }

    /**
     * Unloads the specified account, and removes it from the list of accounts  (in AccountManager)
     * that this provider factory is handling. If the specified accountID is unknown to the
     * ProtocolProviderFactory, the call has no effect. This method is persistent in nature and once
     * called the account corresponding to the specified ID will not be loaded during future runs of
     * the project.
     *
     * @param accountID the ID of the account to remove.
     */
    public void uninstallAccount(AccountID accountID)
    {
        logger.info("Uninstalling account " + accountID.getLoggableAccountID());

        // Unload the account.
        unloadAccount(accountID);

        // unloadAccount should have removed the account from the Map, but under certain error
        // scenarios won't do. Make sure here.
        registeredAccounts.remove(accountID);

        // Remove the stored account so we can distinguish between a deleted or just a disabled account.
        removeStoredAccount(accountID);
    }

    /**
     * The method stores the specified account in the configuration service
     * under the package name of the source factory. The restore and remove
     * account methods are to be used to obtain access to and control the stored
     * accounts.
     * <p>
     * In order to store all account properties, the method would create an
     * entry in the configuration service corresponding (beginning with) the
     * <tt>sourceFactory</tt>'s package name and add to it a unique identifier
     * (e.g. the current milliseconds.)
     * </p>
     *
     * @param accountID the AccountID corresponding to the account that we would
     * like to store.
     */
    protected void storeAccount(AccountID accountID)
    {
        this.storeAccount(accountID, true);
    }

    /**
     * The method stores the specified account in the configuration service
     * under the package name of the source factory. The restore and remove
     * account methods are to be used to obtain access to and control the stored
     * accounts.
     * <p>
     * In order to store all account properties, the method would create an
     * entry in the configuration service corresponding (beginning with) the
     * <tt>sourceFactory</tt>'s package name and add to it a unique identifier
     * (e.g. the current milliseconds.)
     * </p>
     *
     * @param accountID the AccountID corresponding to the account that we would
     * like to store.
     * @param  isModification if <tt>false</tt> there must be no such already
     * loaded account, it <tt>true</tt> ist modification of an existing account.
     * Usually we use this method with <tt>false</tt> in method installAccount
     * and with <tt>true</tt> or the overridden method in method
     * modifyAccount.
     */
    protected void storeAccount(AccountID accountID, boolean isModification)
    {
        if(!isModification
            && getAccountManager().getStoredAccounts().contains(accountID))
        {
            throw new IllegalStateException(
                "An account for id " + accountID.getLoggableAccountID() + " was already loaded!");
        }

        try
        {
            getAccountManager().storeAccount(this, accountID);
        }
        catch (OperationFailedException ofex)
        {
            throw new UndeclaredThrowableException(ofex);
        }
    }

    /**
     * Saves the password for the specified account after scrambling it a bit so
     * that it is not visible from first sight. (The method remains highly
     * insecure).
     *
     * @param accountID the AccountID for the account whose password we're
     *            storing
     * @param password the password itself
     *
     * @throws IllegalArgumentException if no account corresponding to
     *             <code>accountID</code> has been previously stored
     */
    public void storePassword(AccountID accountID, String password)
        throws IllegalArgumentException
    {
        try
        {
            storePassword(getBundleContext(), accountID, password);
        }
        catch (OperationFailedException ofex)
        {
            throw new UndeclaredThrowableException(ofex);
        }
    }

    /**
     * Saves the password for the specified account after scrambling it a bit
     * so that it is not visible from first sight (Method remains highly
     * insecure).
     * <p>
     * TODO Delegate the implementation to {@link AccountManager} because it
     * knows the format in which the password (among the other account
     * properties) is to be saved.
     * </p>
     *
     * @param bundleContext a currently valid bundle context.
     * @param accountID the <tt>AccountID</tt> of the account whose password is
     * to be stored
     * @param password the password to be stored
     *
     * @throws IllegalArgumentException if no account corresponding to
     * <tt>accountID</tt> has been previously stored.
     * @throws OperationFailedException if anything goes wrong while storing the
     * specified <tt>password</tt>
     */
    protected void storePassword(BundleContext bundleContext,
                                 AccountID    accountID,
                                 String       password)
        throws IllegalArgumentException,
               OperationFailedException
    {
        String accountPrefix
            = findAccountPrefix(
                bundleContext,
                accountID,
                getFactoryImplPackageName());

        if (accountPrefix == null)
        {
            throw
                new IllegalArgumentException(
                        "No previous records found for account ID: "
                            + accountID.getLoggableAccountID()
                            + " in package"
                            + getFactoryImplPackageName());
        }

        CredentialsStorageService credentialsStorage
            = ServiceUtils.getService(
                    bundleContext,
                    CredentialsStorageService.class);

        if (!credentialsStorage.user().storePassword(accountPrefix, password))
        {
            throw
                new OperationFailedException(
                        "CredentialsStorageService failed to storePassword",
                        OperationFailedException.GENERAL_ERROR);
        }
    }

    /**
     * Returns the password last saved for the specified account.
     *
     * @param accountID the AccountID for the account whose password we're
     *            looking for
     *
     * @return a String containing the password for the specified accountID
     */
    public String loadPassword(AccountID accountID)
    {
        return loadPassword(getBundleContext(), accountID);
    }

    /**
     * Returns the password last saved for the specified account.
     * <p>
     * TODO Delegate the implementation to {@link AccountManager} because it
     * knows the format in which the password (among the other account
     * properties) was saved.
     * </p>
     *
     * @param bundleContext a currently valid bundle context.
     * @param accountID the AccountID for the account whose password we're
     *            looking for.
     *
     * @return a String containing the password for the specified accountID.
     */
    protected String loadPassword(BundleContext bundleContext,
                                  AccountID     accountID)
    {
        String accountPrefix = findAccountPrefix(
            bundleContext, accountID, getFactoryImplPackageName());

        if (accountPrefix == null)
            return null;

        CredentialsStorageService credentialsStorage
            = ServiceUtils.getService(
                    bundleContext,
                    CredentialsStorageService.class);

        return credentialsStorage.user().loadPassword(accountPrefix);
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter. This method has a persistent
     * effect. Once created the resulting account will remain installed until
     * removed through the uninstallAccount method.
     *
     * @param accountProperties a set of protocol (or implementation) specific
     *            properties defining the new account.
     * @return the AccountID of the newly loaded account
     */
    public AccountID loadAccount(Map<String, String> accountProperties)
    {
        AccountID accountID = createAccount(accountProperties);

        loadAccount(accountID);

        return accountID;
    }

    /**
     * Creates a protocol provider for the given <tt>accountID</tt> and
     * registers it in the bundle context. This method has a persistent
     * effect. Once created the resulting account will remain installed until
     * removed through the uninstallAccount method.
     *
     * @param accountID the account identifier
     * @return <tt>true</tt> if the account with the given <tt>accountID</tt> is
     * successfully loaded, otherwise returns <tt>false</tt>
     */
    public boolean loadAccount(AccountID accountID)
    {
        // Need to obtain the original user id property, instead of calling
        // accountID.getUserID(), because this method could return a modified
        // version of the user id property.
        String userID = accountID
            .getAccountPropertyString(ProtocolProviderFactory.USER_ID);

        logger.info("Loading account " + protocolName + " with ID " +
                    accountID.getLoggableAccountID());

        // Loading accounts that are already loaded (before unloading them) is a bad idea. It will
        // leak objects (that can't be GC'ed because they created Timers), and over time can cause
        // OOMs. Raise an error log to get a stack trace of how we end up loading accounts without
        // unloading first.
        if (isAccountRegistered(accountID))
        {
            logger.error(
                "Loading already loaded account " + protocolName + " with ID " + accountID.getLoggableAccountID(),
                new IllegalStateException());
        }

        ProtocolProviderService service = createService(userID, accountID);

        logger.info("Loading account with protocolProviderService " + service);

        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(PROTOCOL, protocolName);
        properties.put(USER_ID, userID);

        ServiceRegistration<?> serviceRegistration =
            bundleContext.registerService(ProtocolProviderService.class.getName(),
                                          service, properties);

        if (serviceRegistration == null)
            return false;
        else
        {
            synchronized (registeredAccounts)
            {
                logger.info("Adding registered " + protocolName +
                             " account with ID " + accountID.getLoggableAccountID());
                registeredAccounts.put(accountID, serviceRegistration);
            }
            return true;
        }
    }

    /**
     * Unloads the account corresponding to the given <tt>accountID</tt>.
     * Unregisters and shuts down the corresponding ProtocolProviderService, stops tracking the PPS, and unregisters it
     * as a Felix Service.  But keeps the account in AccountManager, in contrast to the uninstallAccount method.
     *
     * @param accountID the account identifier
     * @return true if an account with the specified ID existed and was unloaded
     * and false otherwise.
     */
    public boolean unloadAccount(AccountID accountID)
    {
        // Unregister the protocol provider.
        ServiceReference<?> serRef = getProviderForAccount(accountID);
        logger.info("Unloading account " +
                    accountID.getLoggableAccountID() +
                    ", " + serRef);

        if (serRef == null)
        {
            return false;
        }

        ProtocolProviderService protocolProviderService =
            (ProtocolProviderService) bundleContext.getService(serRef);

        logger.info("Unloading account with protocolProviderService " + protocolProviderService);

        try
        {
            protocolProviderService.unregister();
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to unregister protocol provider for account : " +
                         accountID.getLoggableAccountID() +
                         " caused by: " + ex);
        }

        protocolProviderService.shutdown();

        ServiceRegistration<?> registration;

        synchronized (registeredAccounts)
        {
            registration = registeredAccounts.remove(accountID);
        }
        if (registration == null)
        {
            return false;
        }

        // Unregister the service.
        registration.unregister();

        return true;
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties.
     *
     * @param accountProperties a set of protocol (or implementation) specific
     * properties defining the new account.
     * @return the AccountID of the newly created account
     */
    public AccountID createAccount(Map<String, String> accountProperties)
    {
        BundleContext bundleContext = getBundleContext();
        if (bundleContext == null)
            throw new NullPointerException(
                "The specified BundleContext was null");

        if (accountProperties == null)
            throw new NullPointerException(
                "The specified property map was null");

        String userID = accountProperties.get(USER_ID);
        if (userID == null)
            throw new NullPointerException(
                "The account properties contained no user id.");

        String protocolName = getProtocolName();
        if (!accountProperties.containsKey(PROTOCOL))
            accountProperties.put(PROTOCOL, protocolName);

        return createAccountID(userID, accountProperties);
    }

    /**
     * Creates a new <code>AccountID</code> instance with a specific user ID to
     * represent a given set of account properties.
     * <p>
     * The method is a pure factory allowing implementers to specify the runtime
     * type of the created <code>AccountID</code> and customize the instance.
     * The returned <code>AccountID</code> will later be associated with a
     * <code>ProtocolProviderService</code> by the caller (e.g. using
     * {@link #createService(String, AccountID)}).
     * </p>
     *
     * @param userID the user ID of the new instance
     * @param accountProperties the set of properties to be represented by the
     *            new instance
     * @return a new <code>AccountID</code> instance with the specified user ID
     *         representing the given set of account properties
     */
    protected abstract AccountID createAccountID(
        String userID, Map<String, String> accountProperties);

    /**
     * Gets the name of the protocol this factory registers its
     * <code>ProtocolProviderService</code>s with and to be placed in the
     * properties of the accounts created by this factory.
     *
     * @return the name of the protocol this factory registers its
     *         <code>ProtocolProviderService</code>s with and to be placed in
     *         the properties of the accounts created by this factory
     */
    public String getProtocolName()
    {
        return protocolName;
    }

    /**
     * Initializes a new <code>ProtocolProviderService</code> instance with a
     * specific user ID to represent a specific <code>AccountID</code>.
     * <p>
     * The method is a pure factory allowing implementers to specify the runtime
     * type of the created <code>ProtocolProviderService</code> and customize
     * the instance. The caller will later register the returned service with
     * the <code>BundleContext</code> of this factory.
     * </p>
     *
     * @param userID the user ID to initialize the new instance with
     * @param accountID the <code>AccountID</code> to be represented by the new
     *            instance
     * @return a new <code>ProtocolProviderService</code> instance with the
     *         specific user ID representing the specified
     *         <code>AccountID</code>
     */
    protected abstract ProtocolProviderService createService(String userID,
        AccountID accountID);

    /**
     * Removes the account with <tt>accountID</tt> from the set of accounts
     * that are persistently stored inside the configuration service.
     *
     * @param accountID the AccountID of the account to remove.
     *
     * @return true if an account has been removed and false otherwise.
     */
    protected boolean removeStoredAccount(AccountID accountID)
    {
        return getAccountManager().removeStoredAccount(this, accountID);
    }

    /**
     * Returns the prefix for all persistently stored properties of the account
     * with the specified id.
     * @param bundleContext a currently valid bundle context.
     * @param accountID the AccountID of the account whose properties we're
     * looking for.
     * @param sourcePackageName a String containing the package name of the
     * concrete factory class that extends us.
     * @return a String indicating the ConfigurationService property name
     * prefix under which all account properties are stored or null if no
     * account corresponding to the specified id was found.
     */
    public static String findAccountPrefix(BundleContext bundleContext,
                                       AccountID     accountID,
                                       String sourcePackageName)
    {
        ServiceReference<?> confReference
            = bundleContext.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) bundleContext.getService(confReference);

        //first retrieve all accounts that we've registered
        List<String> storedAccounts =
            configurationService.user().getPropertyNamesByPrefix(sourcePackageName,
                    true);

        //find an account with the corresponding id.
        for (String accountRootPropertyName : storedAccounts)
        {
            //unregister the account in the configuration service.
            //all the properties must have been registered in the following
            //hierarchy:
            //net.java.sip.communicator.impl.protocol.PROTO_NAME.ACC_ID.PROP_NAME
            String accountUID = configurationService.user().getString(
                accountRootPropertyName //node id
                + "." + ACCOUNT_UID); // propname

            if (accountID.getAccountUniqueID().equals(accountUID))
            {
                return accountRootPropertyName;
            }
        }
        return null;
    }

    /**
     * Returns the name of the package that we're currently running in (i.e.
     * the name of the package containing the proto factory that extends us).
     *
     * @return a String containing the package name of the concrete factory
     * class that extends us.
     */
    private String getFactoryImplPackageName()
    {
        String className = getClass().getName();

        return className.substring(0, className.lastIndexOf('.'));
    }

    /**
     * Prepares the factory for bundle shutdown.
     */
    public void stop()
    {
        logger.info("Preparing to stop all protocol providers of " + this);

        Hashtable<AccountID, ServiceRegistration<?>> registeredAccountsCopy;

        synchronized (registeredAccounts)
        {
            registeredAccountsCopy =
                    new Hashtable<>(registeredAccounts);
            registeredAccounts.clear();
        }

        Enumeration<ServiceRegistration<?>> registrations =
                                              registeredAccountsCopy.elements();
        while (registrations.hasMoreElements())
        {
            ServiceRegistration<?> reg = registrations.nextElement();
            stop(reg);
            reg.unregister();
        }
    }

    /**
     * Shuts down the <code>ProtocolProviderService</code> representing an
     * account registered with this factory.
     *
     * @param registeredAccount the <code>ServiceRegistration</code> of the
     *            <code>ProtocolProviderService</code> representing an account
     *            registered with this factory
     */
    protected void stop(ServiceRegistration<?> registeredAccount)
    {
        ProtocolProviderService protocolProviderService =
            (ProtocolProviderService) getBundleContext().getService(
                registeredAccount.getReference());

        protocolProviderService.shutdown();
    }

    /**
     * Get the <tt>AccountManager</tt> of the protocol.
     *
     * @return <tt>AccountManager</tt> of the protocol
     */
    private AccountManager getAccountManager()
    {
        BundleContext bundleContext = getBundleContext();
        ServiceReference<?> serviceReference =
            bundleContext.getServiceReference(AccountManager.class.getName());

        return (AccountManager) bundleContext.getService(serviceReference);
    }
}
