/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.Base64;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.PrivacyUtils;
import net.java.sip.communicator.util.ServiceUtils;

/**
 * Represents an implementation of <tt>AccountManager</tt> which loads the
 * accounts in a separate thread.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class AccountManager
{
    /**
     * The delay in milliseconds the background <tt>Thread</tt> loading the
     * stored accounts should wait before dying so that it doesn't get recreated
     * for each <tt>ProtocolProviderFactory</tt> registration.
     */
    private static final long LOAD_STORED_ACCOUNTS_TIMEOUT = 30000;

    /**
     * The <tt>BundleContext</tt> this service is registered in.
     */
    private final BundleContext bundleContext;

    /**
     * The <tt>AccountManagerListener</tt>s currently interested in the
     * events fired by this manager.
     */
    private final List<AccountManagerListener> listeners =
            new LinkedList<>();

    /**
     * The queue of <tt>ProtocolProviderFactory</tt> services awaiting their
     * stored accounts to be loaded.
     */
    private final Queue<ProtocolProviderFactory> loadStoredAccountsQueue =
            new LinkedList<>();

    /**
     * The <tt>Thread</tt> loading the stored accounts of the
     * <tt>ProtocolProviderFactory</tt> services waiting in
     * {@link #loadStoredAccountsQueue}.
     */
    private Thread loadStoredAccountsThread;

    /**
     * The <tt>Logger</tt> used by this <tt>AccountManagerImpl</tt> instance for
     * logging output.
     */
    private final Logger logger = Logger.getLogger(AccountManager.class);

    /**
     * The set of <tt>AccountID</tt>s, corresponding to all stored accounts.
     */
    private final Set<AccountID> storedAccounts = new HashSet<>();

    /**
     * A set containing all factories that we have been told about
     */
    private Set<ProtocolProviderFactory> factories =
            new HashSet<>();

    /**
     * The prefix of the account unique identifier.
     */
    private static final String ACCOUNT_UID_PREFIX = "acc";

    /**
     * Initializes a new <tt>AccountManagerImpl</tt> instance loaded in a
     * specific <tt>BundleContext</tt> (in which the caller will usually
     * later register it).
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the new
     *            instance is loaded (and in which the caller will usually later
     *            register it as a service)
     */
    public AccountManager(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;

        this.bundleContext.addServiceListener(new ServiceListener()
        {
            public void serviceChanged(ServiceEvent serviceEvent)
            {
                AccountManager.this.serviceChanged(serviceEvent);
            }
        });
    }

    /**
     * Implements AccountManager#addListener(AccountManagerListener).
     * @param listener the <tt>AccountManagerListener</tt> to add
     */
    public void addListener(AccountManagerListener listener)
    {
        synchronized (listeners)
        {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    /**
     * Loads the accounts stored for a specific
     * <tt>ProtocolProviderFactory</tt>.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> to load the
     *            stored accounts of
     */
    private void doLoadStoredAccounts(ProtocolProviderFactory factory)
    {
        ConfigurationService configService
            = ProtocolProviderActivator.getConfigurationService();
        String factoryPackage = getFactoryImplPackageName(factory);
        List<String> accounts
            = configService.user().getPropertyNamesByPrefix(factoryPackage, true);

        logger.info("Discovered " + accounts.size() + " stored "
                    + factoryPackage + " accounts");

        for (Iterator<String> storedAccountIter = accounts.iterator();
                storedAccountIter.hasNext();)
        {
            String storedAccount = storedAccountIter.next();

            // If the property is not related to an account we skip it.
            int dotIndex = storedAccount.lastIndexOf(".");
            if (!storedAccount.substring(dotIndex + 1)
                    .startsWith(ACCOUNT_UID_PREFIX))
                continue;

            // Redact the subscriber phone number from logs in any
            // property where it appears.
            logger.info("Loading account " +
                         PrivacyUtils
                         .sanitiseDirectoryNumberWithAccPrefix(storedAccount));

            List<String> storedAccountProperties =
                configService.user().getPropertyNamesByPrefix(storedAccount, false);
            Map<String, String> accountProperties =
                    new Hashtable<>();
            boolean disabled = false;
            CredentialsStorageService credentialsStorage
                = ServiceUtils.getService(
                        bundleContext,
                        CredentialsStorageService.class);

            for (Iterator<String> storedAccountPropertyIter
                        = storedAccountProperties.iterator();
                    storedAccountPropertyIter.hasNext();)
            {
                String property = storedAccountPropertyIter.next();
                String value = configService.user().getString(property);

                //strip the package prefix
                property = property.substring(storedAccount.length() + 1);

                if (ProtocolProviderFactory.IS_ACCOUNT_DISABLED.equals(property))
                    disabled = Boolean.parseBoolean(value);
                // Decode passwords.
                else if (ProtocolProviderFactory.PASSWORD.equals(property)
                        && !credentialsStorage.user().isStoredEncrypted(storedAccount))
                {
                    if ((value != null) && value.length() != 0)
                    {
                        /*
                         * TODO Converting byte[] to String using the platform's
                         * default charset may result in an invalid password.
                         */
                        value = new String(Base64.decode(value));
                    }
                }

                if (value != null)
                    accountProperties.put(property, value);
            }

            try
            {
                AccountID accountID = factory.createAccount(accountProperties);

                // If for some reason the account id is not created we move to
                // the next account.  Also, if this account has already been
                // loaded then we move to the next account.
                if (accountID == null || storedAccounts.contains(accountID))
                    continue;

                synchronized (storedAccounts)
                {
                    logger.info("Adding account " + accountID.getLoggableAccountID() +
                                " to stored accounts");
                    storedAccounts.add(accountID);
                }
                if (!disabled)
                {
                    factory.loadAccount(accountID);
                }
            }
            catch (Exception ex)
            {
                /*
                 * Swallow the exception in order to prevent a single account
                 * from halting the loading of subsequent accounts.
                 */
                logger.error("Failed to load account", ex);
            }
        }
    }

    /**
     * Looks through the accounts that we previously loaded for this factory
     * and remove any that have been removed in config but not here
     *
     * @param factory The factory whose accounts we should be looking at
     */
    private void unloadOldAccounts(ProtocolProviderFactory factory)
    {
        logger.info("Unload old accounts from factory " + factory);
        String protocolName = factory.getProtocolName();
        ConfigurationService configService =
                            ProtocolProviderActivator.getConfigurationService();
        String factoryPackage = getFactoryImplPackageName(factory);
        List<String> accountConfigStrings =
                   configService.user().getPropertyNamesByPrefix(factoryPackage, true);
        Set<String> storedUIDs = new HashSet<>();
        List<AccountID> accountsToRemove = new ArrayList<>();

        // Get the UIDs of the accounts that are stored in config:
        for (String accountConfigString : accountConfigStrings)
        {
            if (accountConfigString.startsWith(factoryPackage + ".acc"))
            {
                // This is the config string for the account - get the UID
                storedUIDs.add(configService.user().getString(
                                         accountConfigString + ".ACCOUNT_UID"));
            }
        }

        // Now that we know which accounts are still stored, find the accounts
        // that are stored but not in config
        for (AccountID account : storedAccounts)
        {
            if (account.getProtocolName().equals(protocolName))
            {
                // This is an account that we are interested in - check to see
                // if it still exists in config:
                String uid = account.getAccountUniqueID();

                if (!storedUIDs.contains(uid))
                {
                    accountsToRemove.add(account);
                }
            }
        }

        // And now actually remove the account
        for (AccountID account : accountsToRemove)
        {
            logger.info("Removing stored account as config changed " +
                        account.getLoggableAccountID());

            try
            {
                factory.uninstallAccount(account);
            }
            catch (Exception e)
            {
                // Not much we can do, just log
                logger.error("Error unloading account " +
                             account.getLoggableAccountID(), e);
            }
        }

        fireStoredAccountsLoaded(factory);
    }

    /**
     * Notifies the registered {@link #listeners} that the stored accounts of a
     * specific <tt>ProtocolProviderFactory</tt> have just been loaded.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> which had its
     *            stored accounts just loaded
     */
    private void fireStoredAccountsLoaded(ProtocolProviderFactory factory)
    {
        logger.debug("Running fireStoredAccountsLoaded for " + factory);
        AccountManagerEvent event =
            new AccountManagerEvent(this,
                AccountManagerEvent.STORED_ACCOUNTS_LOADED, factory, null);

        fireEvent(event);
    }

    /**
     * Fire an event to the Account Management listeners
     *
     * @param event the event to fire
     */
    public void fireEvent(AccountManagerEvent event)
    {
        AccountManagerListener[] listeners;
        synchronized (this.listeners)
        {
            listeners =
                this.listeners
                    .toArray(new AccountManagerListener[this.listeners.size()]);
        }

        for (AccountManagerListener listener : listeners)
        {
            listener.handleAccountManagerEvent(event);
            logger.debug("Sent account event for listener " + listener +
                                                         " and event " + event);
        }
    }

    /**
     * Returns the package name of the <tt>factory</tt>.
     * @param factory the factory which package will be returned.
     * @return the package name of the <tt>factory</tt>.
     */
    private String getFactoryImplPackageName(ProtocolProviderFactory factory)
    {
        String className = factory.getClass().getName();

        return className.substring(0, className.lastIndexOf('.'));
    }

    /**
     * Searches for stored account with <tt>uid</tt> in stored
     * configuration. The <tt>uid</tt> is the one generated when creating
     * accounts with prefix <tt>ACCOUNT_UID_PREFIX</tt>.
     *
     * @return <tt>AccountID</tt> if there is any account stored in configuration
     * service with <tt>uid</tt>,
     * <tt>null</tt> otherwise.
     */
    public AccountID findAccountID(String uid)
    {
        ServiceReference<?>[] factoryRefs = null;

        try
        {
            factoryRefs
                = bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error(
                "Failed to retrieve the registered ProtocolProviderFactories",
                ex);
        }

        if ((factoryRefs != null) && (factoryRefs.length > 0))
        {
            ConfigurationService configService
                = ProtocolProviderActivator.getConfigurationService();

            for (ServiceReference<?> factoryRef : factoryRefs)
            {
                ProtocolProviderFactory factory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(factoryRef);

                String factoryPackage = getFactoryImplPackageName(factory);
                List<String> storedAccountsProps
                    = configService.user()
                        .getPropertyNamesByPrefix(factoryPackage, true);

                for (Iterator<String> storedAccountIter =
                         storedAccountsProps.iterator();
                     storedAccountIter.hasNext();)
                {
                    String storedAccount = storedAccountIter.next();
                    if (!storedAccount.endsWith(uid))
                        continue;

                    String accountUID = configService.user().getString(
                        storedAccount //node id
                        + "." + ProtocolProviderFactory.ACCOUNT_UID);// propname

                    for (AccountID acc : storedAccounts)
                    {
                        if (acc.getAccountUniqueID().equals(accountUID))
                            return acc;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Loads the accounts stored for a specific
     * <tt>ProtocolProviderFactory</tt> and notifies the registered
     * {@link #listeners} that the stored accounts of the specified
     * <tt>factory</tt> have just been loaded
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> to load the
     *            stored accounts of
     */
    private void loadStoredAccounts(ProtocolProviderFactory factory)
    {
        doLoadStoredAccounts(factory);

        fireStoredAccountsLoaded(factory);
    }

    /**
     * Notifies this manager that a specific
     * <tt>ProtocolProviderFactory</tt> has been registered as a service.
     * The current implementation queues the specified <tt>factory</tt> to
     * have its stored accounts as soon as possible.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> which has been
     *            registered as a service.
     */
    private void protocolProviderFactoryRegistered(
        ProtocolProviderFactory factory)
    {
        factories.add(factory);
        queueLoadStoredAccounts(factory);
    }

    /**
     * Queues a specific <tt>ProtocolProviderFactory</tt> to have its stored
     * accounts loaded as soon as possible.
     * <p/>
     * Also unloads any accounts that have been loaded and removed in config but
     * not yet unloaded
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> to be queued for
     *            loading its stored accounts as soon as possible
     */
    private void queueLoadStoredAccounts(ProtocolProviderFactory factory)
    {
        synchronized (loadStoredAccountsQueue)
        {
            loadStoredAccountsQueue.add(factory);
            loadStoredAccountsQueue.notifyAll();

            if (loadStoredAccountsThread == null)
            {
                loadStoredAccountsThread = new Thread("AccountManager load stored accounts")
                {
                    @Override
                    public void run()
                    {
                        runInLoadStoredAccountsThread();
                    }
                };
                loadStoredAccountsThread.setDaemon(true);
                loadStoredAccountsThread.start();
            }
        }
    }

    /**
     * Implements AccountManager#removeListener(AccountManagerListener).
     * @param listener the <tt>AccountManagerListener</tt> to remove
     */
    public void removeListener(AccountManagerListener listener)
    {
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    /**
     * Running in {@link #loadStoredAccountsThread}, loads the stored accounts
     * of the <tt>ProtocolProviderFactory</tt> services waiting in
     * {@link #loadStoredAccountsQueue}
     */
    private void runInLoadStoredAccountsThread()
    {
        boolean interrupted = false;
        while (!interrupted)
        {
            try
            {
                ProtocolProviderFactory factory;

                synchronized (loadStoredAccountsQueue)
                {
                    factory = loadStoredAccountsQueue.poll();
                    logger.debug("Examining account " + factory);

                    if (factory == null)
                    {
                        /*
                         * Technically, we should be handing spurious wakeups.
                         * However, we cannot check the condition in a queue.
                         * Anyway, we just want to keep this Thread alive long
                         * enough to allow it to not be re-created multiple
                         * times and not handing a spurious wakeup will just
                         * cause such an inconvenience.
                         */
                        try
                        {
                            loadStoredAccountsQueue
                                .wait(LOAD_STORED_ACCOUNTS_TIMEOUT);
                        }
                        catch (InterruptedException ex)
                        {
                            logger
                                .warn(
                                    "The loading of the stored accounts has"
                                        + " been interrupted",
                                    ex);
                            interrupted = true;
                            break;
                        }
                        factory = loadStoredAccountsQueue.poll();
                    }
                    if (factory != null)
                        loadStoredAccountsQueue.notifyAll();
                }

                if (factory != null)
                {
                    try
                    {
                        unloadOldAccounts(factory);
                        loadStoredAccounts(factory);
                    }
                    catch (Throwable ex)
                    {
                        /*
                         * Swallow the exception in order to prevent a single
                         * factory from halting the loading of subsequent
                         * factories.
                         */
                        logger.error("Failed to load accounts for " + factory,
                            ex);
                    }
                }
            }
            finally
            {
                synchronized (loadStoredAccountsQueue)
                {
                    if (!interrupted && (loadStoredAccountsQueue.size() <= 0))
                    {
                        if (loadStoredAccountsThread == Thread.currentThread())
                        {
                            loadStoredAccountsThread = null;
                            loadStoredAccountsQueue.notifyAll();
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Notifies this manager that an OSGi service has changed. The current
     * implementation tracks the registrations of
     * <tt>ProtocolProviderFactory</tt> services in order to queue them for
     * loading their stored accounts.
     *
     * @param serviceEvent the <tt>ServiceEvent</tt> containing the event
     *            data
     */
    private void serviceChanged(ServiceEvent serviceEvent)
    {
        switch (serviceEvent.getType())
        {
        case ServiceEvent.REGISTERED:
            Object service
                = bundleContext.getService(serviceEvent.getServiceReference());

            if (service instanceof ProtocolProviderFactory)
            {
                protocolProviderFactoryRegistered(
                    (ProtocolProviderFactory) service);
            }
            break;
        default:
            break;
        }
    }

    /**
     * Stores an account represented in the form of an <tt>AccountID</tt>
     * created by a specific <tt>ProtocolProviderFactory</tt>.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> which created the
     * account to be stored
     * @param accountID the account in the form of <tt>AccountID</tt> to be
     * stored
     * @throws OperationFailedException if anything goes wrong while storing the
     * account
     */
    public void storeAccount(
            ProtocolProviderFactory factory,
            AccountID accountID)
        throws OperationFailedException
    {
        synchronized (storedAccounts)
        {
            logger.debug("Adding account " +
                         accountID.getLoggableAccountID() +
                         " to stored accounts");
            storedAccounts.add(accountID);
        }

        ConfigurationService configurationService
            = ProtocolProviderActivator.getConfigurationService();
        String factoryPackage = getFactoryImplPackageName(factory);

        // First check if such accountID already exists in the configuration.
        List<String> storedAccounts =
            configurationService.user().getPropertyNamesByPrefix(factoryPackage, true);
        String accountUID = accountID.getAccountUniqueID();
        String accountNodeName = null;

        for (Iterator<String> storedAccountIter = storedAccounts.iterator();
             storedAccountIter.hasNext();)
        {
            String storedAccount = storedAccountIter.next();

            // If the property is not related to an account we skip it.
            int dotIndex = storedAccount.lastIndexOf(".");
            if (!storedAccount.substring(dotIndex + 1)
                    .startsWith(ACCOUNT_UID_PREFIX))
            {
                logger.warn("Account config missing " + ACCOUNT_UID_PREFIX +
                    ": " + storedAccount.substring(dotIndex + 1));
                continue;
            }

            String storedAccountUID =
                configurationService.user().getString(storedAccount + "."
                    + ProtocolProviderFactory.ACCOUNT_UID);
            if (StringUtils.isNullOrEmpty(storedAccountUID))
            {
                logger.warn("No ACCOUNT_UID for account - skipping");
                continue;
            }

            if (storedAccountUID.equals(accountUID))
                accountNodeName = configurationService.user().getString(storedAccount);
        }

        Map<String, Object> configurationProperties
            = new HashMap<>();

        // Create a unique node name of the properties node that will contain
        // this account's properties.
        if (accountNodeName == null)
        {
            accountNodeName
                = ACCOUNT_UID_PREFIX + Long.toString(System.currentTimeMillis());

            // set a value for the persistent node so that we could later
            // retrieve it as a property
            configurationProperties.put(
                    factoryPackage /* prefix */ + "." + accountNodeName,
                    accountNodeName);

            // register the account in the configuration service.
            // we register all the properties in the following hierarchy
            //net.java.sip.communicator.impl.protocol.PROTO_NAME.ACC_ID.PROP_NAME
            configurationProperties.put(factoryPackage// prefix
                + "." + accountNodeName // node name for the account id
                + "." + ProtocolProviderFactory.ACCOUNT_UID, // propname
                accountID.getAccountUniqueID()); // value
        }

        // store the rest of the properties
        Map<String, String> accountProperties = accountID.getAccountProperties();

        for (Map.Entry<String, String> entry : accountProperties.entrySet())
        {
            String property = entry.getKey();
            String value = entry.getValue();
            String secureStorePrefix = null;

            // If the property is a password, store it securely.
            if (property.equals(ProtocolProviderFactory.PASSWORD))
            {
                String accountPrefix = factoryPackage + "." + accountNodeName;
                secureStorePrefix = accountPrefix;
            }
            else if(property.endsWith("." + ProtocolProviderFactory.PASSWORD))
            {
                secureStorePrefix = factoryPackage + "." + accountNodeName +
                    "." + property.substring(0, property.lastIndexOf("."));
            }

            if(secureStorePrefix != null)
            {
                CredentialsStorageService credentialsStorage
                        = ServiceUtils.getService(
                                bundleContext,
                                CredentialsStorageService.class);

                // encrypt and store
                if ((value != null)
                        && (value.length() != 0)
                        && !credentialsStorage.user().storePassword(
                                secureStorePrefix,
                                value))
                {
                    throw
                        new OperationFailedException(
                                "CredentialsStorageService failed to"
                                    + " storePassword",
                                OperationFailedException.GENERAL_ERROR);
                }
            }
            else
            {
                configurationProperties.put(
                        factoryPackage // prefix
                            + "." + accountNodeName // a unique node name for the account id
                            + "." + property, // propname
                        value); // value
            }
        }

        // clear the password if missing property, modification can request
        // password delete
        if(!accountProperties.containsKey(ProtocolProviderFactory.PASSWORD))
        {
            CredentialsStorageService credentialsStorage
                    = ServiceUtils.getService(
                            bundleContext,
                            CredentialsStorageService.class);
            credentialsStorage.user().removePassword(
                factoryPackage + "." + accountNodeName);
        }

        if (configurationProperties.size() > 0)
            configurationService.user().setProperties(configurationProperties);

        logger.debug("Stored account for id " + accountID.getLoggableAccountID()
                    + " for package " + factoryPackage);
    }

    /**
     * Removes the account with <tt>accountID</tt> from the set of accounts
     * that are persistently stored inside the configuration service.
     *
     * @param factory the <tt>ProtocolProviderFactory</tt> which created the
     * account to be stored
     * @param accountID the AccountID of the account to remove.
     * @return true if an account has been removed and false otherwise.
     */
    public boolean removeStoredAccount(ProtocolProviderFactory factory,
        AccountID accountID)
    {
        logger.info("Attempting to delete account with ID " +
            accountID.getLoggableAccountID() + "...");
        synchronized (storedAccounts)
        {
            storedAccounts.remove(accountID);
        }

        /*
         * We're already doing it in #unloadAccount(AccountID) - we're figuring
         * out the ProtocolProviderFactory by the AccountID.
         */
        if (factory == null)
        {
            factory
                = ProtocolProviderActivator.getProtocolProviderFactory(
                        accountID.getProtocolName());
        }

        String factoryPackage = getFactoryImplPackageName(factory);

        // remove the stored password explicitly using credentials service
        CredentialsStorageService credentialsStorage
            = ServiceUtils.getService(
                    bundleContext,
                    CredentialsStorageService.class);
        String accountPrefix =
            ProtocolProviderFactory.findAccountPrefix(bundleContext, accountID,
                factoryPackage);

        credentialsStorage.user().removePassword(accountPrefix);

        ConfigurationService configurationService
            = ServiceUtils.getService(
                    bundleContext,
                    ConfigurationService.class);
        //first retrieve all accounts that we've registered
        List<String> storedAccounts
            = configurationService.user().getPropertyNamesByPrefix(
                factoryPackage, true);

        //find an account with the corresponding id.
        for (String accountRootPropertyName : storedAccounts)
        {
            logger.debug("Considering account root property name '" +
                            accountRootPropertyName + "'...");
            //unregister the account in the configuration service.
            //all the properties must have been registered in the following
            //hierarchy:
            //net.java.sip.communicator.impl.protocol.PROTO_NAME.ACC_ID.PROP_NAME
            String accountUID = configurationService.user().getString(
                accountRootPropertyName //node id
                + "." + ProtocolProviderFactory.ACCOUNT_UID); // propname
            logger.debug("Extracted an account UID for account " +
                         accountID.getLoggableAccountID() + ".");

            // Sometimes matching lines are actually not account declarations.
            // If this happens we should just move on.
            if (accountUID == null)
            {
                logger.debug("Skipping null UID.");
                continue;
            }

            if (accountUID.equals(accountID.getAccountUniqueID()))
            {
                logger.debug("Found the desired UID. Deleting associated config.");
                //retrieve the names of all properties registered for the
                //current account.
                List<String> accountPropertyNames
                    = configurationService.user().getPropertyNamesByPrefix(
                        accountRootPropertyName, false);

                //set all account properties to null in order to remove them.
                for (String propName : accountPropertyNames)
                    configurationService.user().setProperty(propName, null);

                //and now remove the parent too.
                configurationService.user().setProperty(accountRootPropertyName, null);

                fireEvent(new AccountManagerEvent(this,
                                                  AccountManagerEvent.ACCOUNT_REMOVED,
                                                  factory,
                                                  accountID));

                return true;
            }
        }
        return false;
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of all stored
     * <tt>AccountID</tt>s. The list of stored accounts include all registered
     * accounts and all disabled accounts. In other words in this list we could
     * find accounts that aren't loaded.
     * <p>
     * In order to check if an account is already loaded please use the
     * #isAccountLoaded(AccountID accountID) method. To load an account use the
     * #loadAccount(AccountID accountID) method.
     *
     * @return an <tt>Iterator</tt> over a list of all stored
     * <tt>AccountID</tt>s
     */
    public Collection<AccountID> getStoredAccounts()
    {
        synchronized (storedAccounts)
        {
            return new Vector<>(storedAccounts);
        }
    }

    /**
     * Returns a <tt>List</tt> of all stored <tt>AccountID</tt>s for the given
     * protocol.
     *
     * @param protocolName
     *
     * @return the <tt>List</tt> of all stored <tt>AccountID</tt>s for the
     *         given protocol.
     */
    public List<AccountID> getStoredAccountsforProtocol(String protocolName)
    {
        synchronized (storedAccounts)
        {
            logger.debug(
                "Getting all stored accounts for protocol " + protocolName);
            List<AccountID> protocolAccounts = new ArrayList<>();

            for (AccountID storedAccount : storedAccounts)
            {
                String storedProtocolName = storedAccount.getProtocolName();

                if (!StringUtils.isNullOrEmpty(storedProtocolName) &&
                    storedProtocolName.equals(protocolName))
                {
                    protocolAccounts.add(storedAccount);
                }
            }

            return protocolAccounts;
        }
    }

    /**
     * Loads the account corresponding to the given <tt>AccountID</tt>. An
     * account is loaded when its <tt>ProtocolProviderService</tt> is registered
     * in the bundle context. This method is meant to load the account through
     * the corresponding <tt>ProtocolProviderFactory</tt>.
     *
     * @param accountID the identifier of the account to load
     * @throws OperationFailedException if anything goes wrong while loading the
     * account corresponding to the specified <tt>accountID</tt>
     */
    public void loadAccount(AccountID accountID)
        throws OperationFailedException
    {
        logger.info("Load account" + accountID.getLoggableAccountID());

        // If the account with the given id is already loaded we have nothing
        // to do here.
        if (isAccountLoaded(accountID))
        {
            logger.debug("Account " + accountID.getLoggableAccountID() +
                        " already loaded");
            return;
        }

        ProtocolProviderFactory providerFactory
            = ProtocolProviderActivator.getProtocolProviderFactory(
                accountID.getProtocolName());

        if(providerFactory.loadAccount(accountID))
        {
            logger.debug("Account " + accountID.getLoggableAccountID() +
                        " loaded successfully");
            accountID.putAccountProperty(
                ProtocolProviderFactory.IS_ACCOUNT_DISABLED,
                String.valueOf(false));
            // Finally store the modified properties.
            storeAccount(providerFactory, accountID);
        }

        fireEvent(new AccountManagerEvent(this,
                                          AccountManagerEvent.ACCOUNT_LOADED,
                                          providerFactory,
                                          accountID));
    }

    /**
     * Returns the protocol provider service associated with the given Account
     *
     * @param accountID the Account for which to find the protocol provider
     * service.
     * @return the protocol provider service for this account.
     */
    public ProtocolProviderService getProviderForAccount(AccountID accountID)
    {
        ProtocolProviderFactory providerFactory
            = ProtocolProviderActivator.getProtocolProviderFactory(
                                                   accountID.getProtocolName());

        ServiceReference<?> providerRef = providerFactory.getProviderForAccount(accountID);

        ProtocolProviderService protocolProvider = null;

        // The ServiceReference may be null if the accountID refers to an
        // unknown account or the provider is not registered.
        if (providerRef != null)
        {
            protocolProvider =
               (ProtocolProviderService) ProtocolProviderActivator.bundleContext
                                                       .getService(providerRef);
        }

        return protocolProvider;
    }

    /**
     * Unloads the account corresponding to the given <tt>AccountID</tt>. An
     * account is unloaded when its <tt>ProtocolProviderService</tt> is
     * unregistered in the bundle context. This method is meant to unload the
     * account through the corresponding <tt>ProtocolProviderFactory</tt>.
     *
     * @param accountID the identifier of the account to load
     * @throws OperationFailedException if anything goes wrong while unloading
     * the account corresponding to the specified <tt>accountID</tt>
     */
    public void unloadAccount(AccountID accountID)
        throws OperationFailedException
    {
        logger.debug("Unloading account with ID " +
                     accountID.getLoggableAccountID());

        // If the account with the given id is already unloaded we have nothing
        // to do here.
        if (!isAccountLoaded(accountID))
        {
            logger.debug("Account already unloaded!");
            return;
        }

        ProtocolProviderFactory providerFactory
            = ProtocolProviderActivator.getProtocolProviderFactory(
                accountID.getProtocolName());

        // Obtain the protocol provider.
        ServiceReference<?> serRef
            = providerFactory.getProviderForAccount(accountID);

        // If there's no such provider we have nothing to do here.
        if (serRef == null)
        {
            logger.debug("No Provider for account");
            return;
        }

        ProtocolProviderService protocolProvider =
            (ProtocolProviderService) bundleContext.getService(serRef);

        // Set the account icon path for unloaded accounts.
        String iconPathProperty = accountID.getAccountPropertyString(
            ProtocolProviderFactory.ACCOUNT_ICON_PATH);

        if (iconPathProperty == null &&
            protocolProvider.getProtocolIcon() != null)
        {
            accountID.putAccountProperty(
                ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                protocolProvider.getProtocolIcon()
                    .getIconPath(ProtocolIcon.ICON_SIZE_32x32));
        }

        accountID.putAccountProperty(
            ProtocolProviderFactory.IS_ACCOUNT_DISABLED,
            String.valueOf(true));

        if (!providerFactory.unloadAccount(accountID))
        {
            accountID.putAccountProperty(
                ProtocolProviderFactory.IS_ACCOUNT_DISABLED,
                String.valueOf(false));
        }
        // Finally store the modified properties.
        storeAccount(providerFactory, accountID);

        fireEvent(new AccountManagerEvent(this,
                                          AccountManagerEvent.ACCOUNT_REMOVED,
                                          providerFactory,
                                          accountID));
    }

    /**
     * Toggles whether the given account is online or offline
     *
     * @param accountID the ID of the account to take online or offline
     */
    public void toggleAccountEnabled(AccountID accountID)
    {
        logger.info("Toggling online/offline state of account " +
                     accountID.getLoggableAccountID());

        // Set whether we're taking the account online or offline to be the
        // opposite of the account's current state.
        boolean setOnline = !accountID.isEnabled();

        try
        {
            if (setOnline)
            {
                logger.debug("Loading account: " +
                             accountID.getLoggableAccountID());
                loadAccount(accountID);
            }
            else
            {
                logger.debug("Unloading account: " +
                             accountID.getLoggableAccountID());
                unloadAccount(accountID);
            }

            WISPAService wispaService = ServiceUtils.getService(bundleContext, WISPAService.class);
            if (wispaService != null)
            {
                wispaService.notify(WISPANamespace.SETTINGS, WISPAAction.UPDATE);
            }
            else
            {
                logger.warn("Could not notify WISPA about chat state update. Could not get wispa service");
            }
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to load/unload account " +
                         accountID.getLoggableAccountID(), ex);
        }
    }

    /**  Refresh the account status for electron */
    public void refreshAccountInfo()
    {
        WISPAService wispaService = ServiceUtils.getService(bundleContext, WISPAService.class);
        if (wispaService != null)
        {
            wispaService.notify(WISPANamespace.SETTINGS, WISPAAction.UPDATE);
        }
        else
        {
            logger.warn("Could not notify WISPA about chat state update.");
        }
    }

    /**
     * Checks if the account corresponding to the given <tt>accountID</tt> is
     * loaded. An account is loaded if its <tt>ProtocolProviderService</tt> is
     * registered in the bundle context. By default all accounts are loaded.
     * However the user could manually unload an account, which would be
     * unregistered from the bundle context, but would remain in the
     * configuration file.
     *
     * @param accountID the identifier of the account to load
     * @return <tt>true</tt> to indicate that the account with the given
     * <tt>accountID</tt> is loaded, <tt>false</tt> - otherwise
     */
    public boolean isAccountLoaded(AccountID accountID)
    {
        return storedAccounts.contains(accountID) && accountID.isEnabled();
    }

    /**
     * Called when the config under-pinning an account might have changed
     * (unprompted by the user). This will then load any new accounts and
     * unload any old ones
     */
    public void accountsChanged()
    {
        for (ProtocolProviderFactory factory : factories)
            queueLoadStoredAccounts(factory);
    }

    /**
     * Called when the credentials for an account changes. Updates the
     * in memory cache of the password fields.
     *
     * @param accountID the account ID whose credentials have changed
     * @param password the new password for this account
     * @param encryptedPassword the new encrypted password for this account
     */
    public void reloadAccountCredentials(AccountID accountID,
                                         String password,
                                         String encryptedPassword)
    {
        // Save the plain text and encrypted password in memory for this account
        accountID.putAccountProperty(ScopedCredentialsStorageService.ACCOUNT_ENCRYPTED_PASSWORD,
                                     encryptedPassword);
        accountID.putAccountProperty(ProtocolProviderFactory.PASSWORD,
                                     password);
    }
}
