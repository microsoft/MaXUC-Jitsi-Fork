/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.util.*;

/**
 * The implementation of the ProtocolProviderFactory for getting contacts from
 * Outlook and the Mac Address Book
 */
public class ProtocolProviderFactoryAddressBookImpl
    extends ProtocolProviderFactory
{
    /**
     * The logger for this class
     */
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderFactoryAddressBookImpl.class);

    /**
     * The Protocol Provider Service created by this factory
     */
    private AbstractAddressBookProtocolProviderService mAddressBookService;

    /**
     * Creates an instance of the ProtocolProviderFactoryAddressBookImpl.
     */
    protected ProtocolProviderFactoryAddressBookImpl()
    {
        super(AddressBookProtocolActivator.getBundleContext(), ProtocolNames.ADDRESS_BOOK);
    }

    /**
     * Stores a new account for the newly added Address Book service.
     */
    protected void init()
    {
        logger.debug("Got new protocol provider service: " + ProtocolNames.ADDRESS_BOOK);

        Hashtable<String, String> accountProperties
                    = new Hashtable<>();

        accountProperties.put(ProtocolProviderFactory.PROTOCOL,
            ProtocolNames.ADDRESS_BOOK);
        accountProperties.put(ProtocolProviderFactory.IS_PROTOCOL_HIDDEN,
                              Boolean.TRUE.toString());
        accountProperties.put(ProtocolProviderFactory.IS_ACCOUNT_DISABLED,
                              Boolean.TRUE.toString());
        accountProperties.put(USER_ID, ProtocolNames.ADDRESS_BOOK);

        AccountID accountID = new AddressBookAccountID(ProtocolNames.ADDRESS_BOOK,
                                                      accountProperties);

        // Make sure we haven't seen this account id before.
        if (isAccountRegistered(accountID))
        {
            throw new IllegalStateException(
                "An account for id " + ProtocolNames.ADDRESS_BOOK + " was already installed!");
        }

        // Store the account configuration but don't load the account here -
        // the <tt>AccountManager</tt> will take care of that.
        this.storeAccount(accountID, false);
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter. This method has a persistent
     * effect. Once created the resulting account will remain installed until
     * removed through the uninstall account method.
     *
     * @param userIDStr the user identifier for the new account
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account
     */
    @Override
    public AccountID installAccount(String userIDStr,
                                    Map<String, String> accountProperties)
    {
        logger.debug("Installing Address Book account");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        if (accountProperties == null)
            throw new NullPointerException(
                    "The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        AccountID accountID = new AddressBookAccountID(userIDStr,
                                                       accountProperties);

        // Ensure we haven't seen this account id before.
        if (isAccountRegistered(accountID))
        {
            throw new IllegalStateException("An account for id " + userIDStr + " was already installed!");
        }

        // First store the account and only then load it as the load generates
        // an osgi event, the osgi event triggers (through the UI) a call to
        // the register() method and it needs to access the configuration
        // service and check for a password.
        this.storeAccount(accountID, false);

        accountID = loadAccount(accountProperties);

        return accountID;
    }

    /**
     * Creates the platform specific address book service
     *
     * @param userID the user ID to initialize the new instance with
     * @param accountID the <code>AccountID</code> to be represented by the new
     *            instance
     * @return the platform specific address book service
     */
    @Override
    protected ProtocolProviderService createService(String userID,
                                                    AccountID accountID)
    {
        if (OSUtils.IS_WINDOWS)
        {
            logger.debug("Creating new Outlook service");
            mAddressBookService =
                              new ProtocolProviderServiceOutlookImpl(accountID);
        }
        else
        {
            logger.debug("Creating new Mac Address Book service");
            mAddressBookService =
                       new ProtocolProviderServiceMacAddressBookImpl(accountID);
        }

        storeAccount(accountID);
        return mAddressBookService;
    }

    /**
     * Create an account.
     *
     * @param userID the user ID
     * @param accountProperties the properties associated with the user ID
     * @return new <tt>AccountID</tt>
     */
    protected AccountID createAccountID(String userID,
            Map<String, String> accountProperties)
    {
        return new AddressBookAccountID(userID, accountProperties);
    }

    @Override
    public boolean loadAccount(AccountID accountID)
    {
        return super.loadAccount(accountID);
    }

    @Override
    public boolean unloadAccount(AccountID accountID)
    {
        return super.unloadAccount(accountID);
    }
}
