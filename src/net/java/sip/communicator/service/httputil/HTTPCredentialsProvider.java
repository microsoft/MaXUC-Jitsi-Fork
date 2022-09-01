// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.httputil;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.AuthenticationWindow.AuthenticationWindowResult;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.Logger;

import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
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
     * Should we continue retrying, this is set when user hits cancel.
     */
    private boolean retry = true;

    /**
     * The last scope we have used, no problem overriding cause
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
     * Error message.
     */
    private String errorMessage = null;

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

        // Load the stored password for this user.
        CredentialsStorageService credsService = HttpUtilActivator.getCredentialsService();
        String pass = credsService.user() == null ? null : credsService.user().loadPassword(passwordPropertyName);

        // if password is not saved ask user for credentials
        ConfigurationService configurationService = HttpUtilActivator.getConfigurationService();

        if (pass == null)
        {
            // Check for specific properties for the provisioning
            // authentication window:
            // - Find a user-friendly name for the provisioning service
            // - Check for service provider-specific logo
            // - Check for service provider-specific sign-up link
            String serverName =
                configurationService.global().getString(
                    "net.java.sip.communicator.plugin.provisioning.SERVICE_PROVIDER_NAME",
                    authscope.getHost());
            ImageIconFuture icon = DesktopUtilActivator.getResources().getImage("service.gui.BRANDED_LOGO_64x64");
            String signUpLink = HttpUtilActivator.getResources().getSettingsString("plugin.provisioning.SIGN_UP_LINK");

            AuthenticationWindowResult result =
                   AuthenticationWindow.getAuthenticationResult(authUsername,
                                                                null,
                                                                serverName,
                                                                true,
                                                                icon,
                                                                errorMessage,
                                                                signUpLink);

            if(!result.isCanceled())
            {
                authUsername = new String(result.getUserName());
                authPassword = new String(result.getPassword());
                Credentials cred = new UsernamePasswordCredentials(
                        authUsername,authPassword);

                // if password remember is checked lets save passwords,
                // if they seem not correct later will be removed.
                if (result.isRememberPassword())
                {
                    String user = result.getUserName();
                    configurationService.setActiveUser(user);
                    // Store the active user's DN as a salt to protect Personally Identifiable
                    // Information.  If we change active user, then it is right that this will
                    // be updated.
                    Hasher.setSalt(user);

                    String password = new String(result.getPassword());

                    if (credsService.user() != null)
                    {
                        credsService.user().storePassword(passwordPropertyName, password);
                    }
                    else
                    {
                        // The user credentials service hasn't been set up yet.
                        // store value locally until set up is complete
                        logger.debug("The user credentials service hasn't been set up yet. Storing password locally");
                        credsService.storePasswordLocally(passwordPropertyName, password);

                    }
                }

                return cred;
            }

            // well user canceled credentials input stop retry asking him
            // if credentials are not correct
            retry = false;
        }
        else
        {
            // we have saved values lets return them
            authUsername = configurationService.global().getString(usernamePropertyName);
            authPassword = pass;

            return new UsernamePasswordCredentials(authUsername, authPassword);
        }

        return null;
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
        errorMessage = null;
    }

    /**
     * Constructs property name for save if one is not specified.
     * Its in the form
     * HTTP_CREDENTIALS_PREFIX.host.realm.port
     * @param authscope the scope, holds host,realm, port info about
     * the host we are reaching.
     * @return return the constructed property.
     */
    private static String getCredentialProperty(AuthScope authscope)
    {
        StringBuilder pref = new StringBuilder();

        pref.append(HTTP_CREDENTIALS_PREFIX).append(authscope.getHost())
            .append(".").append(authscope.getRealm())
            .append(".").append(authscope.getPort());

        return  pref.toString();
    }

    /**
     * Whether we need to continue retrying.
     * @return whether we need to continue retrying.
     */
    boolean retry()
    {
        return retry;
    }

    /**
     * Returns authentication username if any
     * @return authentication username or null
     */
    public String getAuthenticationUsername()
    {
        return authUsername;
    }

    /**
     * Returns authentication password if any
     * @return authentication password or null
     */
    public String getAuthenticationPassword()
    {
        return authPassword;
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
     * Set the authentication password.
     * @param password The new value for the password.
     */
    public void setAuthenticationPassword(String password)
    {
        authPassword = password;
    }

    /**
     * Mark the authentication as having failed
     */
    public void authenticationFailed()
    {
        // Clear the data, but keep the username
        clear(true);

        errorMessage =
            HttpUtilActivator.getResources().getI18NString(
            "service.gui.AUTHENTICATION_FAILED",
            new String[]
                    {usedScope.getHost()});
    }
}
