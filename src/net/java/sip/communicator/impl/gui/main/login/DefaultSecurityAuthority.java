/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.login;

import java.util.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.AuthenticationWindow.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Utility class that can be used in cases where components other than the main
 * user interface may need to launch provider registration. At the time I am
 * writing this, the <tt>DefaultSecurityAuthority</tt> is being used by the
 * systray and uri handlers.
 *
 * @author Yana Stamcheva
 * @author Emil Ivov
 */
public class DefaultSecurityAuthority
    implements SecurityAuthority
{
    /**
     * The logger of this class.
     */
    private final Logger logger
        = Logger.getLogger(DefaultSecurityAuthority.class);

    private ProtocolProviderService protocolProvider;

    /**
     * This field controls whether the user name is editable in the dialog
     * where we prompt the user to re-enter their credentials if they are wrong.
     * Setting this to 'true' changes the dialog without actually acting on
     * modifications to the field, so leave it as false.
     */
    private boolean isUserNameEditable = false;

    /**
     * Creates an instance of <tt>SecurityAuthorityImpl</tt>.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> for this
     * <tt>SecurityAuthority</tt>.
     */
    public DefaultSecurityAuthority(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Implements the <code>SecurityAuthority.obtainCredentials</code> method.
     * Creates and show an <tt>AuthenticationWindow</tt>, where user could enter
     * its password.
     * @param realm The realm that the credentials are needed for.
     * @param userCredentials the values to propose the user by default
     * @param reasonCode indicates the reason for which we're obtaining the
     * credentials.
     * @param resSuffix A suffix added to the end of the resources to allow
     * resource strings to be over-ridden by the calling code
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    public UserCredentials obtainCredentials(
            String realm,
            UserCredentials userCredentials,
            int reasonCode,
            String resSuffix)
    {
        ResourceManagementService res = GuiActivator.getResources();

        String imRealm = res.getI18NString(
            "plugin.jabberaccregwizz.PROTOCOL_DESCRIPTION");

        String imProvSource = ConfigurationUtils.getImProvSource();
        AccountManager accountManager = GuiActivator.getAccountManager();

        // Find out whether this is an IM login (the 'realm 'parameter is
        // 'jabber') and the IM account is provisioned automatically
        // ('IM_PROVISION_SOURCE' in config is set to 'CommPortal'), as this
        // affects what we want to display to the user.
        boolean isImRealm = realm.equals(imRealm);
        boolean isCommPortalIM = "CommPortal".equals(imProvSource) && isImRealm;
        AccountID jabberAccount = null;
        if (isImRealm)
        {
            // This is an IM login, so try to find the jabber account.
            List<AccountID> jabberAccounts =
                accountManager.getStoredAccountsforProtocol(ProtocolNames.JABBER);
            int numAccounts = jabberAccounts.size();

            if (numAccounts != 1)
            {
                logger.warn("Found " + numAccounts + " jabber accounts");
            }

            if (numAccounts > 0)
            {
                jabberAccount = jabberAccounts.get(0);
            }
        }

        String errorMessage = null;
        String userName = userCredentials.getUserName();
        char[] password = userCredentials.getPassword();

        String root = "service.gui.AUTHENTICATION_FAILED";
        String cancelButtonText = null;
        boolean serverAddressValidated = false;

        if (reasonCode == WRONG_PASSWORD
            || reasonCode == WRONG_USERNAME)
        {
            errorMessage =
                 res.getI18NString(root + "." + resSuffix, new String[]{realm});

            if (errorMessage == null)
            {
                errorMessage = res.getI18NString(root, new String[]{realm});
            }

            if (jabberAccount != null)
            {
                // If we have a jabber account, this is an IM login. If the
                // jabber account has previously been registered, this may be a
                // temporary problem, so we make the 'cancel' button sign out
                // of chat rather than deleting the account. Therefore, we need
                // to make sure the 'cancel' button text correctly reflects
                // what will happen when it is clicked.
                serverAddressValidated = jabberAccount.getAccountPropertyBoolean(
                    ProtocolProviderFactory.SERVER_ADDRESS_VALIDATED, false);

                logger.debug("Server address validated for " +
                              jabberAccount.getAccountAddress() + "? " +
                                                        serverAddressValidated);

                String menuResource;
                if (serverAddressValidated)
                {
                    menuResource = ConfigurationUtils.isSmsEnabled() ?
                                           "service.gui.chat.DISABLE_CHAT_SMS" :
                                           "service.gui.chat.SIGN_OUT_CHAT";
                }
                else
                {
                    menuResource = "service.gui.REMOVE_ACCOUNT";
                }

                cancelButtonText = res.getI18NString(menuResource);
            }
        }

        // If we're using CommPortal IM login, we don't want to expose to the
        // user the fact that this is a separate IM account login so remove
        // the domain name from the displayed error message.
        if (isCommPortalIM)
        {
            logger.debug("CommPortal IM login - removing domain from " +
                                         "username displayed in error message");
            errorMessage =
                res.getI18NString(root, new String[]{userName.split("@")[0]});
        }

        AuthenticationWindowResult result = null;
        ImageIconFuture icon = null;

        // If this is a CommPortal IM login, we don't want to display the
        // jabber account name or icon, so only set them if we're not.
        String loginUserName = "";
        String usernameLabel = "";

        if (!isCommPortalIM)
        {
            logger.debug(
                "Not CommPortal IM login - setting image icon and username");
            icon = GuiActivator.getImageLoaderService().
                                  getAuthenticationWindowIcon(protocolProvider);
            loginUserName = userName;
            usernameLabel =
                res.getI18NString("service.gui.IDENTIFIER." + resSuffix);
        }

        // We display the password label and field in all cases.
        String passwordLabel = res.getI18NString("service.gui.PASSWORD." + resSuffix);

        logger.debug("Displaying login window to the user");
        result = AuthenticationWindow.getAuthenticationResult(loginUserName,
                                                              password,
                                                              realm,
                                                              isUserNameEditable,
                                                              icon,
                                                              usernameLabel,
                                                              passwordLabel,
                                                              errorMessage,
                                                              cancelButtonText);

        if (!result.isCanceled())
        {
            logger.debug("User submitted login credentials");
            userCredentials.setPassword(result.getPassword());
            userCredentials.setPasswordPersistent(
                result.isRememberPassword());

            // If we're using CommPortal IM login, just use the original
            // username when submitting the login, as we didn't give the user
            // the option to change it.  Also, as the password is the same for
            // all accounts, update the stored credentials with the new
            // password submitted by the user.
            if (isCommPortalIM)
            {
                logger.debug("CommPortal IM login - logging in with original" +
                    " username and saving password in credentials service");
                userCredentials.setUserName(userName);

                CredentialsStorageService credService =
                    GuiActivator.getCredentialsService();

                credService.user().storePassword(
                    "net.java.sip.communicator.plugin.provisioning.auth",
                    new String(result.getPassword()));
            }
            else
            {
                logger.debug("Not CommPortal login - logging in with " +
                    "user-submitted username");
                userCredentials.setUserName(result.getUserName());
            }
        }
        else
        {
            logger.debug("User cancelled login");
            userCredentials.setUserName(null);
            userCredentials = null;

            // If we have a jabber account, this is an IM login. If so, make
            // sure either we sign out of chat or the account is fully removed
            // so that we don't keep alerting the user of the failure to
            // connect.
            if (jabberAccount != null)
            {
                if (serverAddressValidated)
                {
                    logger.info(
                        "Server address validated - taking IM account offline");
                    accountManager.toggleAccountEnabled(jabberAccount);
                }
                else
                {
                    logger.info(
                        "Server address not validatd - deleting IM account");
                    jabberAccount.deleteAccount(false);
                }
            }
        }

        return userCredentials;
    }

    /**
     * Implements the <code>SecurityAuthority.obtainCredentials</code> method.
     * Creates and show an <tt>AuthenticationWindow</tt>, where user could enter
     * its password.
     * @param realm The realm that the credentials are needed for.
     * @param userCredentials the values to propose the user by default
     * @param reasonCode indicates the reason for which we're obtaining the
     * credentials.
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    public UserCredentials obtainCredentials(
            String realm,
            UserCredentials userCredentials,
            int reasonCode)
    {
        return obtainCredentials(realm,
                                 userCredentials,
                                 reasonCode,
                                 null);
    }

    /**
     * Implements the <code>SecurityAuthority.obtainCredentials</code> method.
     * Creates and show an <tt>AuthenticationWindow</tt>, where user could enter
     * its password.
     * @param realm The realm that the credentials are needed for.
     * @param userCredentials the values to propose the user by default
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    public UserCredentials obtainCredentials(
            String realm,
            UserCredentials userCredentials)
    {
        return this.obtainCredentials(realm, userCredentials,
            SecurityAuthority.AUTHENTICATION_REQUIRED);
    }

    /**
     * Sets the userNameEditable property, which indicates if the user name
     * could be changed by user or not.
     *
     * @param isUserNameEditable indicates if the user name could be changed by
     * user
     */
    public void setUserNameEditable(boolean isUserNameEditable)
    {
        this.isUserNameEditable = isUserNameEditable;
    }

}
