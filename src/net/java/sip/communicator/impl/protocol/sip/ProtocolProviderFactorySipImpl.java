/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import java.util.*;

import org.jitsi.service.resources.ResourceManagementService;
import org.osgi.framework.*;

import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A SIP implementation of the protocol provider factory interface.
 *
 * @author Emil Ivov
 */
public class ProtocolProviderFactorySipImpl
    extends ProtocolProviderFactory
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderFactorySipImpl.class);

    /**
     * Access to the resource management service
     */
    private final ResourceManagementService mResourceService =
            SipActivator.getResources();

    /**
     * Constructs a new instance of the ProtocolProviderFactorySipImpl.
     */
    ProtocolProviderFactorySipImpl()
    {
        super(SipActivator.getBundleContext(), ProtocolNames.SIP);
    }

    /**
     * Ovverides the original in order not to save the XCAP_PASSWORD field.
     *
     * @param accountID the account identifier.
     */
    protected void storeAccount(AccountID accountID)
    {
        logger.debug("Storing account ID " +
                     (accountID != null ? accountID.getLoggableAccountID() : null));
        storeXCapPassword(accountID);
        super.storeAccount(accountID);
    }

    /**
     * Stores XCAP_PASSWORD property.
     *
     * @param accountID the account identifier.
     */
    private void storeXCapPassword(AccountID accountID)
    {
        // don't use getAccountPropertyString as it will query
        // credential storage, use getAccountProperty to see is there such
        // property in the account properties provided.
        // if xcap password property exist, store it through credentialsStorage
        // service
        Object password = accountID.getAccountProperty(
                ServerStoredContactListSipImpl.XCAP_PASSWORD);
        if (password != null)
        {
            CredentialsStorageService credentialsStorage
                = ServiceUtils.getService(
                getBundleContext(),
                CredentialsStorageService.class);
            String accountPrefix = accountID.getAccountUniqueID() + ".xcap";
            credentialsStorage.user().storePassword(accountPrefix, (String)password);
            // remove unsecured property
            accountID.removeAccountProperty(
                    ServerStoredContactListSipImpl.XCAP_PASSWORD);
        }
    }

    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr the user identifier uniquely representing the newly
     *   created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account.
     * @throws IllegalArgumentException if userID does not correspond to an
     *   identifier in the context of the underlying protocol or if
     *   accountProperties does not contain a complete set of account
     *   installation properties.
     * @throws IllegalStateException if the account has already been
     *   installed.
     * @throws NullPointerException if any of the arguments is null.
     */
    public AccountID installAccount( String userIDStr,
                                 Map<String, String> accountProperties)
    {
        logger.info("Install account " + sanitiseChatAddress(userIDStr));

        BundleContext context = SipActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException("The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");
        if (accountProperties == null)
            throw new NullPointerException("The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        if (!accountProperties.containsKey(PROTOCOL))
            accountProperties.put(PROTOCOL, ProtocolNames.SIP);

        AccountID accountID = createAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if (isAccountRegistered(accountID))
        {
            throw new IllegalStateException("An account for id " + accountID.getLoggableAccountID() + " was already installed!");
        }

        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (through the UI) a call to
        //the register() method and it needs to access the configuration service
        //and check for a password.
        this.storeAccount(accountID, false);

        try
        {
            accountID = loadAccount(accountProperties);
        }
        catch(RuntimeException exc)
        {
            //it might happen that load-ing the account fails because of a bad
            //initialization. if this is the case, make sure we remove it.
            this.removeStoredAccount(accountID);

            throw exc;
        }

        return accountID;
    }

    /**
     * When we don't have all the config required to perform a SIP register, we
     * inform the user to contact their service provider.
     */
    private void noSIPConfigErrorDialog()
    {
        logger.error("Missing SIP config - inform user to contact SP");
        String message = mResourceService.getI18NString(
                "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT");
        String title = mResourceService.getI18NString(
                "service.gui.WARNING");
        new ErrorDialog(title, message).showDialog();
    }

    @Override
    public AccountID createAccount(Map<String, String> accountProperties)
    {
        // If calling is not enabled, i.e. the user doesn't use SIP, we don't
        // want to show this error message.
        if (ConfigurationUtils.isCallingEnabled() &&
            (accountProperties == null ||
            accountProperties.get(USER_ID) == null))
        {
            // We do not have all the config required to perform a SIP
            // register, so we need to tell the user to contact their SP.
            noSIPConfigErrorDialog();
        }

        return super.createAccount(accountProperties);
    }

    /**
     * Creates a new <code>SipAccountID</code> instance with a specific user
     * ID to represent a given set of account properties.
     *
     * @param userID the user ID of the new instance
     * @param accountProperties the set of properties to be represented by the
     *            new instance
     * @return a new <code>AccountID</code> instance with the specified user ID
     *         representing the given set of account properties
     */
    @Override
    protected AccountID createAccountID(String userID, Map<String, String> accountProperties)
    {
        // serverAddress == null is OK because of registrarless support
        String serverAddress = accountProperties.get(SERVER_ADDRESS);

        return new SipAccountID(userID, accountProperties, serverAddress);
    }

    /**
     * Initializes a new <code>ProtocolProviderServiceSipImpl</code> instance
     * with a specific user ID to represent a specific <code>AccountID</code>.
     *
     * @param userID the user ID to initialize the new instance with
     * @param accountID the <code>AccountID</code> to be represented by the new
     *            instance
     * @return a new <code>ProtocolProviderService</code> instance with the
     *         specific user ID representing the specified
     *         <code>AccountID</code>
     */
    @Override
    protected ProtocolProviderService createService(String userID, AccountID accountID)
    {
        logger.info("Creating ProtocolProviderService for account ID " +
                    (accountID != null ? accountID.getLoggableAccountID() : null));
        ProtocolProviderServiceSipImpl service = new ProtocolProviderServiceSipImpl();

        try
        {
            service.initialize(userID, (SipAccountID) accountID);

            // We store again the account in order to store all properties added
            // during the protocol provider initialization.
            storeAccount(accountID);
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to initialize account", ex);
            throw new IllegalArgumentException("Failed to initialize account"
                + ex.getMessage());
        }
        return service;
    }
}
