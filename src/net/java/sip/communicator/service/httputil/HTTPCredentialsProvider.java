// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.httputil;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.launchutils.ProvisioningParams;

import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.Hasher;

/**
 * The provider asking for password that is inserted into httpclient.
 */
public class HTTPCredentialsProvider
    implements CredentialsProvider
{
    private static final Logger logger = Logger.getLogger(HTTPCredentialsProvider.class);

    /**
     * The prefix used when storing credentials for sites when no property
     * is provided.
     */
    private static final String HTTP_CREDENTIALS_PREFIX =
        "net.java.sip.communicator.util.http.credential.";

    /**
     * The last scope we have used, no problem overriding because
     * we use new HTTPCredentialsProvider instance for every
     * httpclient/request.
     */
    private AuthScope usedScope = null;

    /**
     * The property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     */
    private String usernamePropertyName = null;

    /**
     * The property to use to retrieve/store
     * password value if protected site is hit, for password
     * CredentialsStorageService service is used.
     */
    private String passwordPropertyName = null;

    /**
     * Authentication username if any.
     */
    private String authUsername = null;

    /**
     * Authentication password if any.
     */
    private String authPassword = null;

    /**
     * Creates HTTPCredentialsProvider.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * CredentialsStorageService service is used.
     */
    HTTPCredentialsProvider(String usernamePropertyName,
                            String passwordPropertyName)
    {
        this.usernamePropertyName = usernamePropertyName;
        this.passwordPropertyName = passwordPropertyName;
    }

    /**
     * Not used.
     */
    public void setCredentials(AuthScope authscope, Credentials credentials)
    {}

    /**
     * Get the {@link org.apache.http.auth.Credentials credentials} for the
     * given authentication scope.
     *
     * @param authscope the {@link org.apache.http.auth.AuthScope
     *                  authentication scope}
     * @return the credentials
     * @see #setCredentials(org.apache.http.auth.AuthScope,
     *      org.apache.http.auth.Credentials)
     */
    @Override
    public Credentials getCredentials(AuthScope authscope)
    {
        this.usedScope = authscope;

        // if we have specified password and username property will use them
        // if not create one from the scope/site we are connecting to.
        if(passwordPropertyName == null)
        {
            passwordPropertyName = getCredentialProperty(authscope);
        }
        if(usernamePropertyName == null)
        {
            usernamePropertyName = getCredentialProperty(authscope);
        }

        if (PreLoginUtils.isLoggedInViaSSO())
        {
            // If user was logged in with SSO we need to fetch a new token. To do that, we ask
            // Electron to fetch a new token using the silent flow.
            logger.info("Was logged in with SSO - retrieve a new SSO token");
            String ssoToken = PreLoginUtils.refreshSSOToken();
            return new SSOCredentials(ssoToken);
        }

        // Otherwise, if user is not logged in ask user for credentials.
        CredentialsStorageService credsService = HttpUtilActivator.getCredentialsService();
        ConfigurationService configurationService = HttpUtilActivator.getConfigurationService();
        String pass = credsService.user() == null ? null : credsService.user().loadPassword(passwordPropertyName);

        if (!PreLoginUtils.isLoggedIn())
        {
            // User is not logged in, wait for Electron to send login info.
            PreLoginUtils.awaitEvent(PreLoginUtils.EventType.LOGIN);
            // User won't be able to change service provider anymore, update the flag.
            PreLoginUtils.isServiceProviderChosen = true;

            if (PreLoginUtils.isShutdownInitiated())
            {
                // Return immediately, avoid setting active user, let it fail.
                // We need to do so to allow Felix to close the bundle - because
                // otherwise it can't as the thread is blocked.
                logger.info("Shutdown initiated, just continue");
                return null;
            }

            if (PreLoginUtils.hasSSOToken())
            {
                logger.info("User logging in with SSO.");
                Hasher.setSalt(PreLoginUtils.currentUsername);
                return new SSOCredentials(PreLoginUtils.currentSSOToken);
            }
            else
            {
                logger.info("User logging in with username and password");
                return getUsernamePasswordCredentials();
            }
        }
        else
        {
            // Or, if we have saved values (username and password) we return them.
            authUsername = configurationService.global().getString(usernamePropertyName);
            authPassword = pass;

            return new UsernamePasswordCredentials(authUsername, authPassword);
        }
    }

    private UsernamePasswordCredentials getUsernamePasswordCredentials()
    {
        if (ProvisioningParams.getPassword() != null)
        {
            // Login link was clicked and password was provided, login with that.
            logger.info("Logging in with a link");
            authUsername = ProvisioningParams.getSubscriber();
            authPassword = ProvisioningParams.getPassword();
        }
        else
        {
            authUsername = PreLoginUtils.currentUsername;
            authPassword = PreLoginUtils.currentPassword;
        }

        // Store the active user's DN as a salt to protect Personally Identifiable
        // Information.  If we change active user, then it is right that this will
        // be updated.
        Hasher.setSalt(authUsername);

        return new UsernamePasswordCredentials(authUsername, authPassword);
    }

    /**
     * Clear saved password. Used when we are in situation that
     * saved username and password are no longer valid.
     */
    public void clear()
    {
        clear(false);
    }

    public void clear(boolean keepUsername)
    {
        if(usedScope != null)
        {
            if(passwordPropertyName == null)
            {
                passwordPropertyName = getCredentialProperty(usedScope);
            }
            if(usernamePropertyName == null)
            {
                usernamePropertyName = getCredentialProperty(usedScope);
            }

            HttpUtilActivator.getConfigurationService().user().removeProperty(usernamePropertyName);
            HttpUtilActivator.getCredentialsService().user().removePassword(passwordPropertyName);
        }

        if (!keepUsername)
        {
            authUsername = null;
        }

        authPassword = null;
    }

    /**
     * Constructs property name for save if one is not specified.
     * It is in the form
     * HTTP_CREDENTIALS_PREFIX.host.realm.port
     * @param authscope the scope, holds host,realm, port info about
     * the host we are reaching.
     * @return return the constructed property.
     */
    private static String getCredentialProperty(AuthScope authscope)
    {
        return HTTP_CREDENTIALS_PREFIX + authscope.getHost() +
                      "." + authscope.getRealm() +
                      "." + authscope.getPort();
    }

    /**
     * Set the authentication username.
     * @param username The new value for the username.
     */
    public void setAuthenticationUsername(String username)
    {
        authUsername = username;
    }

    /**
     * Mark the authentication as having failed - instruct Electron to show an
     * error popup.
     */
    public void authenticationFailed()
    {
        // Clear the data, but keep the username
        clear(true);

        ResourceManagementService resources = HttpUtilActivator.getResources();
        String errorMessage =
            resources.getI18NString(
            "service.gui.AUTHENTICATION_FAILED",
            new String[]{usedScope.getHost()});

        String title = resources.getI18NString("service.gui.ERROR");
        ErrorDialog errorDialog = new ErrorDialog(title, errorMessage);
        errorDialog.setModal(true);
        errorDialog.showDialog();
    }
}
