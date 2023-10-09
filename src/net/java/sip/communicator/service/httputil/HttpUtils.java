/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.httputil;

import static net.java.sip.communicator.util.PrivacyUtils.getLoggableCPURL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.PreLoginUtils;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.StringUtils;

/**
 *
 * HttpUtils enables
 * 1. to query http locations, e.g. for self-signed certificates,
 * host verify on certificates, updateWindows plugin to check for new updates
 * 2. accessing password-protected servers
 *  a) by asking for the credentials displaying an AuthenticationWindow
 *  b) for retrieving SIP-PS configuration by sending the credentials securely
 *      if the server has support for it,
 * 3. storing and reusing credentials for accessing the servers.
 *
 * @author Damian Minkov
 */
public class HttpUtils
{
    /**
     * The <tt>Logger</tt> used by the <tt>HttpUtils</tt> class for logging
     * output.
     */
    private static final Logger logger = Logger.getLogger(HttpUtils.class);

    /**
     * Maximum number of http redirects (301, 302, 303).
     */
    private static final int MAX_REDIRECTS = 10;

    /**
     * Socket timeout value for the http request.
     */
    private static final int SOCKET_TIMEOUT = 10000;

    /**
     * Connection timeout value for the http request.
     */
    private static final int CONNECTION_TIMEOUT = 10000;

    /**
     * User-agent String for the http request
     */
    private static final String USER_AGENT = "Accession Communicator/"
                                                + System.getProperty("sip-communicator.version");

    public enum HttpMethod
    {
        POST,
        GET
    }

    /**
     * Opens a connection to the <tt>address</tt>.
     * @param address the address to contact.
     * @return the result if any or null if connection was not possible
     * or cancelled by user.
     */
    public static HTTPResponseResult openURLConnection(String address)
    {
        try
        {
            HttpGet httpGet = new HttpGet(address);

            HttpClient httpClient = getHttpClient();
            HttpResponse response = executeMethod(httpClient, httpGet);

            if ((response == null) || (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK))
            {
                return null;
            }

            return new HTTPResponseResult(response, httpClient, null);
        }
        catch(Throwable t)
        {
            logger.error("Cannot open connection to:" + getLoggableCPURL(address), t);
        }

        return null;
    }

    /**
     * Executes POST to the <tt>address</tt>, taking the query params from the
     * URL.
     * @param address the address to contact.
     * @return the result if any or null if connection was not possible.
     */
    public static HTTPResponseResult executePostExtractingParams(String address)
    {
        HTTPResponseResult httpResponseResult = null;

        try
        {
            List<NameValuePair> params = new ArrayList<>();
            String[] splitUrl = address.split("\\?", 2);
            String query = "";

            if (splitUrl.length >= 2)
            {
                logger.info("URL has query params, extracting");
                query = splitUrl[1];
            }

            Pattern pattern = Pattern.compile("([^&=]+)=([^&]*)");
            Matcher matcher = pattern.matcher(query);

            while (matcher.find())
            {
                String name = matcher.group(1);
                String value = matcher.group(2);
                params.add(new BasicNameValuePair(name, value));
            }

            HttpClient httpClient = getHttpClient();
            HttpResponse response = executeUsingPost(httpClient, address, params);

            if (response != null) {
                httpResponseResult = new HTTPResponseResult(response, httpClient, null);
            }
        }
        catch(Throwable t)
        {
            logger.error("Cannot execute POST to:" + getLoggableCPURL(address), t);
        }

        return httpResponseResult;
    }

    /**
    * Executes the method and return the result. Handle ask for password
    * when hitting password-protected site.
    * Keep asking for password till user clicks cancel or enters correct
    * password. When 'remember password' is checked, attempt to save the password.
    * Only correct values are saved; if the username and password are not correct,
    * their values will be cleared.
    * @param httpClient the configured http client to use.
    * @param req the request for now it is get or post.
    * @return the HttpResponse
    */
    private static HttpResponse executeMethod(HttpClient httpClient, HttpRequestBase req)
            throws AuthenticationException, IOException
    {
        // Do this when response (first execution) is null or till we are unauthorized
        // Don't repeat on Forbidden as we don't want that to behave like
        // incorrect password, but like "error".
        HttpResponse response = null;
        int redirects = 0;

        while (response == null ||
               response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
        {
            // if we were unauthorized, let's clear the method and recreate it
            // for new connection with new credentials.
            if (response != null &&
                (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED))
            {
                logger.debug("Will retry http connect and " +
                             "credentials input as latest are not correct!");

                throw new AuthenticationException("Authorization needed");
            }
            else
            {
                response = httpClient.execute(req);
            }

            // check for post redirect as post redirects are not handled
            // automatically
            // RFC2616 (10.3 Redirection 3xx).
            // The second request (forwarded method) can only be a GET or HEAD.
            Header locationHeader = response.getFirstHeader("location");

            if (locationHeader != null &&
                req instanceof HttpPost &&
                (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY ||
                 response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY ||
                 response.getStatusLine().getStatusCode() == HttpStatus.SC_SEE_OTHER) &&
                redirects < MAX_REDIRECTS)
            {
                HttpRequestBase oldreq = req;
                oldreq.abort();

                String newLocation = locationHeader.getValue();

                // append query string if any
                HttpEntity en = ((HttpPost) oldreq).getEntity();
                if (en instanceof StringEntity)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    en.writeTo(out);
                    newLocation += "?" + out.toString(StandardCharsets.UTF_8);
                }

                req = new HttpGet(newLocation);
                req.setParams(oldreq.getParams());
                req.setHeaders(oldreq.getAllHeaders());

                redirects++;
                response = httpClient.execute(req);
            }
        }

        return response;
    }

    /**
     * Send either a GET or POST request
     * @param address HTTP address.
     * @param httpMethod - GET or POST.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * @param urlParams HttpUrlParams object encapsulating the names and values of the URL params to use.
     * @Param String prepopulatedUsername Username to prepopulate in a dialog if we need to show one to
     * prompt the user to enter a password.
     * @return the HttpResponseResult or null either if both POST and GET methods
     * did not succeed or the credentials input window was cancelled.
     */
    public static HTTPResponseResult sendHttpRequest(String address,
                                                     HttpMethod httpMethod,
                                                     String usernamePropertyName,
                                                     String passwordPropertyName,
                                                     HttpUrlParams urlParams,
                                                     Credentials savedCredentials)
            throws IOException, URISyntaxException
    {

        HTTPResponseResult response = null;

        // Will retry if no authentication exception is thrown while executing
        AuthenticationException authEx;

        HTTPCredentialsProvider credentialsProvider = new HTTPCredentialsProvider(usernamePropertyName, passwordPropertyName);

        Credentials userCredentials = null;

        // We may have:
        // - no saved credentials, in which case we ask the user to enter them
        // - saved credentials with a username only, in which case we prepopulate the password dialog with this
        // - saved credentials with username and password, in which case we use these so the user has nothing to enter
        if (savedCredentials != null)
        {
            logger.debug("Using saved credentials");

            credentialsProvider.setAuthenticationUsername(savedCredentials.getUserPrincipal().getName());
            if (savedCredentials.getPassword() != null)
            {
                logger.debug("Saved credentials contain a password");
                userCredentials = savedCredentials;
            }
        }

        if (userCredentials == null)
        {
            userCredentials = urlParams.haveUsernameAndPassword() ? getUserCredentials(credentialsProvider, address) : null;
        }

        do
        {
            try
            {
                response = sendHttpRequestWithCredentials(httpMethod,
                                                          userCredentials,
                                                          address,
                                                          urlParams);

                storeUsernameAndPassword(passwordPropertyName, userCredentials);

                authEx = null;
            }
            catch (AuthenticationException ex)
            {
                authEx = ex;
                credentialsProvider.authenticationFailed();
            }
        }
        while (authEx != null);

        // cancelled or no result
        return response;
    }

    /**
     * @param httpMethod - GET or POST.
     * @param credentials the credentials object
     * @param address HTTP address.
     * @param urlParams A ProvisioningUrlParams object encapsulating the names and values of the URL params to use.
     * @return the HttpResponseResult or null either if POST/GET methods did not
     * succeed or the credentials input window was cancelled.
     */
    private static HTTPResponseResult sendHttpRequestWithCredentials(
            HttpMethod httpMethod,
            Credentials credentials,
            String address,
            HttpUrlParams urlParams)
            throws AuthenticationException, URISyntaxException, IOException
    {
        List<NameValuePair> parameters = urlParams.getParams(credentials);

        HttpClient httpClient = getHttpClient();
        HttpResponse response;
        if (httpMethod == HttpMethod.GET)
        {
            response = executeUsingGet(httpClient, address, parameters);
        }
        else
        {
            response = executeUsingPost(httpClient, address, parameters);
        }

        if (response == null)
        {
            return null;
        }

        return new HTTPResponseResult(response, httpClient, credentials);
    }

    /**
     * Gets the credentials from the provider object or prompts user to enter one
     * @param inProv The credentials Provider object
     * @param address the address host and port for which the credentials are requested
     * @return Credentials the credentials stored / asked from user
     * @throws MalformedURLException the exception from URL construction, if any
     */
    private static Credentials getUserCredentials(HTTPCredentialsProvider inProv, String address)
            throws MalformedURLException
    {
        logger.debug("Get user credentials");

        // if we have username and password in the parameters, let's
        // retrieve their values
        Credentials creds;
        boolean credsProvided;

        URL url = new URL(address);

        do
        {
            if (PreLoginUtils.isShutdownInitiated())
            {
                // Return immediately, we need to do so to allow Felix to close
                // the bundle - because otherwise it can't as the thread is blocked.
                logger.info("Shutdown initiated, just continue");
                return null;
            }

            creds = inProv.getCredentials(new AuthScope(url.getHost(), url.getPort()));

            credsProvided = credentialsProvided(creds);
            if (!credsProvided)
            {
                // User has not entered a username or SSO login failed. So
                // display a login failure and prompt them to log in again.
                logger.info("No username entered / SSO failed");
                showLoginFailedError();
            }
        } while (!credsProvided);

        return creds;
    }

    private static void storeUsernameAndPassword(String passwordPropertyName, Credentials userCredentials)
    {
        if (userCredentials == null || userCredentials.getUserPrincipal() == null)
        {
            // Not saving anything as we don't have credentials.
            return;
        }

        CredentialsStorageService credsService = HttpUtilActivator.getCredentialsService();
        ConfigurationService configurationService = HttpUtilActivator.getConfigurationService();

        String authUsername = userCredentials.getUserPrincipal().getName();
        configurationService.setActiveUser(authUsername);

        if (credsService.user() != null)
        {
            credsService.user().storePassword(passwordPropertyName, userCredentials.getPassword());
        }
        else
        {
            // The user credentials service hasn't been set up yet.
            // store value locally until set up is complete
            logger.debug("The user credentials service hasn't been set up yet. Storing password locally");
            credsService.storePasswordLocally(passwordPropertyName, userCredentials.getPassword());
        }
    }

    private static void showLoginFailedError()
    {
        ResourceManagementService resources = HttpUtilActivator.getResources();
        String errorMessage;

        if (PreLoginUtils.didSSOLoginFail)
        {
            logger.debug("Login with SSO failed, show correct error message");
            errorMessage = resources.getI18NString("service.gui.LOGIN_MICROSOFT_ERROR");
        }
        else if (ConfigurationUtils.isEmailLoginEnabled())
        {
            errorMessage = resources.getI18NString("plugin.provisioning.ERROR_MISSING_USERNAME_EMAIL");
        }
        else
        {
            errorMessage = resources.getI18NString("plugin.provisioning.ERROR_MISSING_USERNAME");
        }

        ErrorDialog errorDialog = new ErrorDialog(HttpUtilActivator.getResources().getI18NString("service.gui.ERROR"), errorMessage);
        errorDialog.setModal(true);
        errorDialog.showDialog();
    }

    /**
     * Returns true if:
     *  - credentials are not null and username is provided
     *  - credentials are SSOCredentials (we don't need to check the contents
     *  of SSOCredentials as if ever invalid credentials are returned, we
     *  display an error and force the user to logout so the instanceof
     *  check is sufficient).
     */
    private static boolean credentialsProvided(Credentials credentials)
    {
        return credentials instanceof SSOCredentials ||
               (credentials != null &&
               credentials.getUserPrincipal() != null &&
               !StringUtils.isNullOrEmpty(
                       credentials.getUserPrincipal().getName()));
    }

    /**
     * execute the post method with the given address URL by
     * sending the parameters in the body of the request
     * @param httpClient the HttpClient
     * @param address the post url
     * @param parameters list of parameters as name value pair
     * @return the response from the execution of the HTTP request
     * @throws URISyntaxException from the URL construction
     * @throws AuthenticationException when user is not authorised to access
     * @throws IOException from the SSLContext construction
     */
    @VisibleForTesting
    static HttpResponse executeUsingPost(HttpClient httpClient,
                                         String address,
                                         List<NameValuePair> parameters)
            throws URISyntaxException, AuthenticationException, IOException
    {
        HttpPost postMethod = new HttpPost(address);
        postMethod.setURI(new URI(address));
        postMethod.setHeader("Content-Type", "application/x-www-form-urlencoded");
        postMethod.setHeader("Cache-Control", "No-Transform");
        postMethod.setEntity(new UrlEncodedFormEntity(parameters));

        return executeMethod(httpClient, postMethod);
    }

    /**
     * execute the get method with the given address URL by
     * appending the parameters to the URL
     * @param httpClient the HttpClient
     * @param address the post url
     * @param parameters list of parameters as name value pair
     * @return the response from the execution of the HTTP request
     * @throws URISyntaxException from the URL construction
     * @throws AuthenticationException when user is not authorised to access
     * @throws IOException from the SSLContext construction
     */
    @VisibleForTesting
    static HttpResponse executeUsingGet(HttpClient httpClient,
                                        String address,
                                        List<NameValuePair> parameters)
            throws URISyntaxException, AuthenticationException, IOException
    {
        HttpGet getMethod = new HttpGet(address);

        // Uses String UTF-8 to keep compatible with android version and
        // older versions of the http client libs, as the one used
        // in debian (4.1.x)
        String s = URLEncodedUtils.format(parameters, StandardCharsets.UTF_8);
        getMethod.setURI(new URI(address + "?" + s));

        return executeMethod(httpClient, getMethod);
    }

    /**
     * Returns the preconfigured http client,
     * using CertificateVerificationService, timeouts, user-agent,
     * hostname verifier, proxy settings are used from global java settings,
     * if protected site is hit asks for credentials
     * using util.swing.AuthenticationWindow.
     */
    private static HttpClient getHttpClient() throws IOException
    {
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
                .setMaxRedirects(MAX_REDIRECTS)
                .build();

        SSLContext sslCtx;
        try
        {
            sslCtx = HttpUtilActivator.getCertificateVerificationService()
                    .getSSLContext(
                            HttpUtilActivator.getCertificateVerificationService()
                                    .getTrustManager());
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException(e.getMessage());
        }

        return HttpClients.custom()
                .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
                .setDefaultRequestConfig(requestConfig)
                .setSSLContext(sslCtx)
                .setUserAgent(USER_AGENT)
                .build();
    }

    /**
     * A simple helper method which executes GET on the passed url. Used for
     * endpoints which don't require authentication.
     * @param url URL string to execute GET on.
     * @return A response from the GET call.
     * @throws IOException In case the fetch fails.
     */
    public static String executeGet(String url) throws IOException
    {
        HttpClient httpClient = HttpUtils.getHttpClient();
        HttpResponse response = httpClient.execute(new HttpGet(url));
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }

    /**
     * A simple helper method which takes the url, opens the connection and
     * return the input stream.
     * @param url URL string to execute on.
     * @throws IOException In case the connection fails.
     */
    public static InputStream openConnectionToUrl(String url) throws IOException
    {
        URL brandingUrl = new URL(url);
        return brandingUrl.openConnection().getInputStream(); // lgtm[java/non-ssl-connection] All CDAP URLs are HTTPS and CDAP servers don't support HTTP.
    }
}
