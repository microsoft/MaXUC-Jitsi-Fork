/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.PasswordFuture;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.impl.credentialsstorage.ScopedCredentialsStorageServiceImpl;
import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.protocol.AbstractProtocolProviderService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderActivator;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.UserCredentials;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.util.ConfigurationUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.util.Logger;
import org.jitsi.util.StringUtils;

/**
 * Login to Jabber using username & password.
 *
 * @author Stefan Sieber
 */
public class LoginByPasswordStrategy
    implements JabberLoginStrategy
{
    private final AbstractProtocolProviderService protocolProvider;
    private final AccountID accountID;

    private String password;

    private Logger logger = Logger.getLogger(LoginByPasswordStrategy.class);

    /**
     * Create a login strategy that logs in using user credentials (username
     * and password)
     * @param protocolProvider  protocol provider service to fire registration
     *                          change events.
     * @param accountID The accountID to use for the login.
     */
    public LoginByPasswordStrategy(AbstractProtocolProviderService protocolProvider,
                                   AccountID accountID)
    {
        this.protocolProvider = protocolProvider;
        this.accountID = accountID;
    }

    /**
     * Loads the account passwords as preparation for the login.
     *
     * @param authority SecurityAuthority to obtain the password
     * @param reasonCode reason why we're preparing for login
     * @return UserCredentials in case they need to be cached for this session
     *         (i.e. password is not persistent)
     */
    public UserCredentials prepareLogin(SecurityAuthority authority,
                                        int reasonCode)
    {
       return loadPassword(authority, reasonCode);
    }

    /**
     * Determines whether the strategy is ready to perform the login.
     *
     * @return True when the password was successfully loaded.
     */
    public boolean loginPreparationSuccessful()
    {
        return password != null;
    }

    /**
     * Performs the login on an XMPP connection using SASL PLAIN.
     *
     * @param connection The connection on which the login is performed.
     * @param userName The username for the login.
     * @param resource The XMPP resource.
     * @return always true.
     * @throws XMPPException
     * @throws InterruptedException
     * @throws IOException
     * @throws SmackException
     * @throws XmppStringprepException
     */
    public boolean login(AbstractXMPPConnection connection,
                         String userName,
                         String resource)
        throws XMPPException, SmackException, IOException, InterruptedException
    {
        connection.login(userName, new PasswordFuture()
        {
            @Override
            public String get()
            {
                return password;
            }
        }, Resourcepart.from(resource));

        return true;
    }

    /**
     * Prepares an SSL Context that is customized SSL context.
     *
     * @param cs The certificate service that provides the context.
     * @param trustManager The TrustManager to use within the context.
     * @return An initialized context for the current provider.
     * @throws GeneralSecurityException
     */
    public SSLContext createSslContext(CertificateService cs,
                                       X509TrustManager trustManager)
        throws GeneralSecurityException
    {
        return cs.getSSLContext(trustManager);
    }

    /**
     * Load the password from the account configuration or ask the user.
     *
     * @param authority SecurityAuthority
     * @param reasonCode the authentication reason code. Indicates the reason of
     *            this authentication.
     * @return The UserCredentials in case they should be cached for this
     *         session (i.e. are not persistent)
     */
    private UserCredentials loadPassword(SecurityAuthority authority,
                                         int reasonCode)
    {
        UserCredentials cachedCredentials = null;

        // Verify whether a password has already been stored for this account.
        // For CommPortal IM we use the CommPortal password instead of the
        // Jabber one.
        boolean isCommPortalIM = "CommPortal".equals(ConfigurationUtils.getImProvSource());

        if (isCommPortalIM)
        {
            String CPAccountPasswordPrefix = "net.java.sip.communicator.plugin.provisioning.auth";

            CredentialsStorageService credentialsStorage =
                JabberActivator.getCredentialsStorageService();
            password = credentialsStorage.user().loadPassword(CPAccountPasswordPrefix);

            logger.debug("Using stored CommPortal password for Jabber account");
        }
        else if (reasonCode != SecurityAuthority.WRONG_PASSWORD)
        {
            password = JabberActivator.getProtocolProviderFactory().loadPassword(accountID);

            logger.debug("Using stored Jabber password for Jabber account");
        }

        //decode
        if (password == null)
        {
            //create a default credentials object
            UserCredentials credentials = new UserCredentials();
            credentials.setUserName(accountID.getUserID());

            String accountDisplayName = JabberActivator.getResources().
                            getI18NString(
                                "plugin.jabberaccregwizz.PROTOCOL_DESCRIPTION");

            String displayName = StringUtils.isNullOrEmpty(accountDisplayName) ?
                                                  accountID.getDisplayName() :
                                                  accountDisplayName;

            // Request a password from the user
            credentials = authority.obtainCredentials(
                    displayName,
                    credentials,
                    reasonCode,
                    "jabber");

            // In case user has cancelled the login window
            if (credentials == null)
            {
                protocolProvider.fireRegistrationStateChanged(
                        protocolProvider.getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST,
                        "No credentials provided");
                return null;
            }

            // Extract the password the user passed us.
            char[] pass = credentials.getPassword();

            // The user didn't provide us a password (cancelled the operation)
            if (pass == null)
            {
                protocolProvider.fireRegistrationStateChanged(
                        protocolProvider.getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST,
                        "No password entered");
                return null;
            }
            password = new String(pass);

            if (credentials.isPasswordPersistent())
            {
                ProtocolProviderFactoryJabberImpl jabberFactory =
                                   JabberActivator.getProtocolProviderFactory();

                jabberFactory.storePassword(accountID, password);

                ConfigurationService configService =
                            ProtocolProviderActivator.getConfigurationService();

                String className = jabberFactory.getClass().getName();
                String packageName = className.substring(0, className.lastIndexOf('.'));

                // Determine the account prefix that is used in the configuration file
                String accountPrefix = ProtocolProviderFactory.findAccountPrefix(
                                       JabberActivator.getBundleContext(),
                                       accountID,
                                       packageName);

                // Load the encrypted password from the configuration file
                String encryptedPasswordProperty = accountPrefix + "." +
                        ScopedCredentialsStorageServiceImpl.ACCOUNT_ENCRYPTED_PASSWORD;
                String encryptedPassword = (String) configService.user().getProperty(
                                                           encryptedPasswordProperty);

                JabberActivator.getAccountManager().reloadAccountCredentials(
                                                             accountID,
                                                             password,
                                                             encryptedPassword);
            }
            else
                cachedCredentials = credentials;
        }
        return cachedCredentials;
    }
}
