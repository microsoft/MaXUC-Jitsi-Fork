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
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
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

import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
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
     * Whether the Authentication Window has been closed by the user, without logging in.
     */
    public static boolean sAuthWindowCancelled = false;

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
            HTTPCredentialsProvider credentialsProvider = new HTTPCredentialsProvider(
                    null, null);

            HttpClient httpClient = getHttpClient();
            HttpResponse response = executeMethod(httpClient, credentialsProvider, httpGet);

            if (response == null)
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
    * Executes the method and return the result. Handle ask for password
    * when hitting password-protected site.
    * Keep asking for password till user clicks cancel or enters correct
    * password. When 'remember password' is checked, attempt to save the password.
    * Only correct values are saved; if the username and password are not correct,
    * their values will be cleared.
    * @param httpClient the configured http client to use.
    * @param provider the credential provider object
    * @param req the request for now it is get or post.
    * @return the HttpResponse, or null if the request is forbidden or
    * some other error has occurred.
    */
    private static HttpResponse executeMethod(HttpClient httpClient,
                                      HTTPCredentialsProvider provider,
                                      HttpRequestBase req)
            throws AuthenticationException, IOException
    {
        // Do this when response (first execution) is null or till we are unauthorized
        // Don't repeat on Forbidden as we don't want that to behave like
        // incorrect password, but like "error".
        HttpResponse response = null;
        int redirects = 0;
        while (response == null
              || response.getStatusLine().getStatusCode()
                 == HttpStatus.SC_UNAUTHORIZED)
        {
            // if we were unauthorized, let's clear the method and recreate it
            // for new connection with new credentials.
            if (response != null
               && (response.getStatusLine().getStatusCode()
                   == HttpStatus.SC_UNAUTHORIZED))
            {
                logger.debug("Will retry http connect and " +
                             "credentials input as latest are not correct!");

                throw new AuthenticationException("Authorization needed");
            }
            else
            {
                response = httpClient.execute(req);
            }

            // The HTTPCredentialsProvider gets the AuthenticationWindowResult when
            // the user presses 'Log In', 'Cancel' or the 'X' in the Authentication Window.
            // HTTPCredentialsProvider only stops re-trying to get correct login details
            // if the user clicked cancel or 'X'.
            sAuthWindowCancelled = !provider.retry();

            // If the user clicked cancel no need to retry. Stop trying
            if (sAuthWindowCancelled)
            {
                logger.debug("User cancelled credentials input.");
                break;
            }

            // check for post redirect as post redirects are not handled
            // automatically
            // RFC2616 (10.3 Redirection 3xx).
            // The second request (forwarded method) can only be a GET or HEAD.
            Header locationHeader = response.getFirstHeader("location");

            if (locationHeader != null
               && req instanceof HttpPost
               &&  (response.getStatusLine().getStatusCode()
                    == HttpStatus.SC_MOVED_PERMANENTLY
                    || response.getStatusLine().getStatusCode()
                       == HttpStatus.SC_MOVED_TEMPORARILY
                    || response.getStatusLine().getStatusCode()
                       == HttpStatus.SC_SEE_OTHER)
               && redirects < MAX_REDIRECTS)
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

        // if we finally managed to log in, return the result.
        if (response != null
           && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        {
            return response;
        }

        // if user has cancelled, no result is needed.
        return null;
    }

    /**
     * Whether the Authentication Window has been closed by the user, without logging in.
     * @return true if the user closed the window
     */
    public static boolean authWindowCancelled()
    {
        return sAuthWindowCancelled;
    }

    /**
     * Posting form to <tt>address</tt>. For submission, we use POST method
     * which is "application/x-www-form-urlencoded" encoded.
     * Failing data retrieval through POST request, falls back on GET request
     * @param address HTTP address.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * @param prepopulatedUsername the value to put in the username field of the
     * login dialog, if no username was found in config.
     * @param formParamNames the parameter names to include in post.
     * @param formParamValues the corresponding parameter values to use.
     * @param usernameParamIx the index of the username parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @param passwordParamIx the index of the password parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @return the HttpResponseResult or null either if both POST and GET methods
     * did not succeed or the credentials input window was cancelled.
     */
    public static HTTPResponseResult sendDataAsPostWithFallbackToGet(String address,
                                                                     String usernamePropertyName,
                                                                     String passwordPropertyName,
                                                                     String prepopulatedUsername,
                                                                     List<String> formParamNames,
                                                                     List<String> formParamValues,
                                                                     int usernameParamIx,
                                                                     int passwordParamIx)
            throws IOException, URISyntaxException
    {

        HTTPResponseResult response = null;

        // Will retry if no authentication exception is thrown while executing
        AuthenticationException authEx;
        HTTPCredentialsProvider credentialsProvider = new HTTPCredentialsProvider(
                            usernamePropertyName, passwordPropertyName);
        credentialsProvider.setAuthenticationUsername(prepopulatedUsername);
        Credentials userCredentials = getUserCredentials(credentialsProvider, address,
                                               formParamNames, usernameParamIx, passwordParamIx);
        do
        {
            try
            {
                // execute post
                response = sendDataAsPostWithFallbackToGet(credentialsProvider,
                                                           userCredentials,
                                                           address,
                                                           formParamNames,
                                                           formParamValues,
                                                           usernameParamIx,
                                                           passwordParamIx);

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
     * Posting form to <tt>address</tt>. For submission, we use POST request
     * which is "application/x-www-form-urlencoded" encoded.
     * If POST request does not return a 200 OK, we fall back to GET request
     * @param prov the credentialsProvider object
     * @param credentials the credentials object
     * @param address HTTP address.
     * @param formParamNames the parameter names to include in post.
     * @param formParamValues the corresponding parameter values to use.
     * @param usernameParamIx the index of the username parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @param passwordParamIx the index of the password parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @return the HttpResponseResult or null either if POST/GET methods did not
     * succeed or the credentials input window was cancelled.
     */
    private static HTTPResponseResult sendDataAsPostWithFallbackToGet(
            HTTPCredentialsProvider prov,
            Credentials credentials,
            String address,
            List<String> formParamNames,
            List<String> formParamValues,
            int usernameParamIx,
            int passwordParamIx)
            throws AuthenticationException, URISyntaxException, IOException
    {
        logger.debug("Getting config using POST method");

        List<NameValuePair> parameters = new ArrayList<>();
        if (formParamNames != null)
        {
            for (int paramIdx = 0; paramIdx < formParamNames.size(); paramIdx++)
            {
                if (paramIdx == usernameParamIx && credentials != null)
                { // we are on the username index, insert retrieved username value
                    parameters.add(new BasicNameValuePair(formParamNames.get(paramIdx),
                                                          credentials.getUserPrincipal().getName()));
                }
                else if (paramIdx == passwordParamIx && credentials != null)
                { // we are on the password index, insert retrieved password val
                    parameters.add(new BasicNameValuePair(formParamNames.get(paramIdx), credentials.getPassword()));
                }
                else // common name value pair, all info is present
                {
                    parameters.add(new BasicNameValuePair(formParamNames.get(paramIdx), formParamValues.get(paramIdx)));
                }
            }
        }

        HTTPResponseResult httpResponseResult = null;
        HttpClient httpPostClient = getHttpClient();
        HttpResponse response = executeUsingPost(httpPostClient, prov, address, parameters);

        boolean fallBackToGet = false;

        if (response == null)
        {
            logger.info("Getting config using POST failed with null response");
            fallBackToGet = true;
        }
        else if (response.getStatusLine() == null)
        {
            logger.info("Getting config using POST failed - can't get HTTP response status code");
            fallBackToGet = true;
        }
        else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
        {
            logger.info("Getting config using POST failed with HTTP response status code: " +
                        response.getStatusLine().getStatusCode());
            fallBackToGet = true;
        }
        else if (response.getEntity() == null)
        {
            logger.info("Getting config using POST failed - response entity is null");
            fallBackToGet = true;
        }
        else if (response.getEntity().getContentLength() == 0)
        {
            logger.info("Getting config using POST failed with empty entity content");
            fallBackToGet = true;
        }
        else
        {
            logger.debug("Continuing with successful POST request");
            httpResponseResult = new HTTPResponseResult(response, httpPostClient, credentials);
        }

        if (fallBackToGet)
        {
            logger.info("Falling back to getting config using GET");
            HttpClient httpGetClient = getHttpClient();
            response = executeUsingGet(httpGetClient, prov, address, parameters);
            httpResponseResult =  new HTTPResponseResult(response, httpGetClient, credentials);
        }

        return httpResponseResult;
    }

    /**
     * Gets the credentials from the provider object or prompts user to enter one
     * @param inProv The credentials Provider object
     * @param address the address host and port for which the credentials are requested
     * @param formParamNames Name of the parameters that will be sent with the request
     * @param usernameParamIx index of the username parameter
     * @param passwordParamIx index of the password parameter
     * @return Credentials the credentials stored / asked from user
     * @throws MalformedURLException the exception from URL construction, if any
     */
    private static Credentials getUserCredentials(
            HTTPCredentialsProvider inProv,
            String address,
            List<String> formParamNames,
            int usernameParamIx,
            int passwordParamIx)
            throws MalformedURLException
    {
        // if we have username and password in the parameters, let's
        // retrieve their values
        Credentials creds = null;
        if (usernameParamIx != -1
            && usernameParamIx < formParamNames.size()
            && passwordParamIx != -1
            && passwordParamIx < formParamNames.size())
        {
            URL url = new URL(address);

            // don't allow empty username
            while (creds == null
                   || creds.getUserPrincipal() == null
                   || StringUtils.isNullOrEmpty(
                    creds.getUserPrincipal().getName()))
            {
                creds = inProv.getCredentials(
                        new AuthScope(url.getHost(), url.getPort()));

                sAuthWindowCancelled = !inProv.retry();

                // it was cancelled by the user, let's stop processing
                if (creds == null && sAuthWindowCancelled)
                {
                    return null;
                }

                if (creds == null
                    || creds.getUserPrincipal() == null
                    || StringUtils.isNullOrEmpty(
                        creds.getUserPrincipal().getName()))
                {
                    // User has not entered a username. So display a login
                    // failure and prompt them to log in again.
                    logger.info("No username entered");

                    String errorMessage;

                    if (ConfigurationUtils.isEmailLoginEnabled())
                    {
                        errorMessage =
                                HttpUtilActivator.getResources().getI18NString(
                                        "plugin.provisioning.ERROR_MISSING_USERNAME_EMAIL");
                    }
                    else
                    {
                        errorMessage =
                                HttpUtilActivator.getResources().getI18NString(
                                        "plugin.provisioning.ERROR_MISSING_USERNAME");
                    }

                    new ErrorDialog(HttpUtilActivator.getResources()
                        .getI18NString("service.gui.ERROR"), errorMessage)
                        .showDialog();
                }
            }
        }
        return creds;
    }

    /**
     * execute the post method with the given address URL by
     * sending the parameters in the body of the request
     * @param httpClient the HttpClient
     * @param credentialsProvider provider for the credentials
     * @param address the post url
     * @param parameters list of parameters as name value pair
     * @return the response from the execution of the HTTP request
     * @throws URISyntaxException from the URL construction
     * @throws AuthenticationException when user is not authorised to access
     * @throws IOException from the SSLContext construction
     */
    @VisibleForTesting
    static HttpResponse executeUsingPost(HttpClient httpClient,
                                         HTTPCredentialsProvider credentialsProvider,
                                         String address,
                                         List<NameValuePair> parameters)
            throws URISyntaxException, AuthenticationException, IOException
    {
        HttpPost postMethod = new HttpPost(address);
        postMethod.setURI(new URI(address));
        postMethod.setHeader("Content-Type", "application/x-www-form-urlencoded");
        postMethod.setHeader("Cache-Control", "No-Transform");
        postMethod.setEntity(new UrlEncodedFormEntity(parameters));

        // execute post
        return executeMethod(httpClient, credentialsProvider, postMethod);
    }

    /**
     * execute the get method with the given address URL by
     * appending the parameters to the URL
     * @param httpClient the HttpClient
     * @param credentialsProvider provider for the credentials
     * @param address the post url
     * @param parameters list of parameters as name value pair
     * @return the response from the execution of the HTTP request
     * @throws URISyntaxException from the URL construction
     * @throws AuthenticationException when user is not authorised to access
     * @throws IOException from the SSLContext construction
     */
    @VisibleForTesting
    static HttpResponse executeUsingGet(HttpClient httpClient,
                                        HTTPCredentialsProvider credentialsProvider,
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

        // execute post
        return executeMethod(httpClient, credentialsProvider, getMethod);
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
}
