/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatAddress;

import java.util.Map;

import org.jivesoftware.smack.xml.SmackXmlParser;
import org.jivesoftware.smack.xml.stax.StaxXmlPullParserFactory;
import org.osgi.framework.BundleContext;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.AccountManagerEvent;
import net.java.sip.communicator.util.JitsiStringUtils;
import net.java.sip.communicator.util.Logger;

/**
 * The Jabber implementation of the ProtocolProviderFactory.
 * @author Damian Minkov
 */
public class ProtocolProviderFactoryJabberImpl
    extends ProtocolProviderFactory
{
    private static final Logger logger = Logger.getLogger(ProtocolProviderFactoryJabberImpl.class);

    /**
     * Creates an instance of the ProtocolProviderFactoryJabberImpl.
     */
    protected ProtocolProviderFactoryJabberImpl()
    {
        super(JabberActivator.getBundleContext(), ProtocolNames.JABBER);

        SmackXmlParser.setXmlPullParserFactory(new StaxXmlPullParserFactory());

        // Initializes smack iq and extension providers common for all protocol
        // provider instances.
        ProviderManagerExt.load();
    }

    /**
     * Ovverides the original in order give access to protocol implementation.
     *
     * @param accountID the account identifier.
     */
    protected void storeAccount(AccountID accountID)
    {
        super.storeAccount(accountID);
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
    public AccountID installAccount( String userIDStr,
                                     Map<String, String> accountProperties)
    {
        logger.info("Install account " + sanitiseChatAddress(userIDStr));

        BundleContext context
            = JabberActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException(
                    "The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        if (accountProperties == null)
            throw new NullPointerException(
                    "The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        // if server address is null, we must extract it from userID
        if(accountProperties.get(SERVER_ADDRESS) == null)
        {
            String serverAddress = JitsiStringUtils.parseServer(userIDStr);

            if (serverAddress != null)
                accountProperties.put(SERVER_ADDRESS,
                    JitsiStringUtils.parseServer(userIDStr));
            else throw new IllegalArgumentException(
                "Should specify a server for user name " + sanitiseChatAddress(userIDStr) + ".");
        }

        // if server port is null, we will set default value
        accountProperties.putIfAbsent(SERVER_PORT,
                                      "5222");

        AccountID accountID = new JabberAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if (isAccountRegistered(accountID))
        {
            throw new IllegalStateException("An account for id " + sanitiseChatAddress(userIDStr) + " was already installed!");
        }

        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (through the UI) a call to
        //the register() method and it needs to access the configuration service
        //and check for a password.
        this.storeAccount(accountID, false);

        loadAccount(accountID);

        // Tell everyone that this account has been loaded.
        // Normally, AccountManager.loadAccount would do this for us.  But we
        // don't call that because we store the account first.
        AccountManager am = JabberActivator.getAccountManager();
        am.fireEvent(new AccountManagerEvent(am,
                                             AccountManagerEvent.ACCOUNT_LOADED,
                                             this,
                                             accountID));

        return accountID;
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
        return new JabberAccountID(userID, accountProperties);
    }

    protected ProtocolProviderService createService(String userID,
        AccountID accountID)
    {
        logger.info("Creating ProtocolProviderService for account ID " + sanitiseChatAddress(accountID.toString()));
        ProtocolProviderServiceJabberImpl service = new ProtocolProviderServiceJabberImpl();

        service.initialize(userID, accountID);
        return service;
    }
}
