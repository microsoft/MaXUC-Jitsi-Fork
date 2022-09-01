/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.httputil;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import javax.net.ssl.*;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.*;
import org.apache.http.client.utils.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.*;
import org.apache.http.entity.mime.*;
import org.apache.http.entity.mime.content.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.util.EntityUtils;
import org.jitsi.util.*;

import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * Common http utils querying http locations, handling redirects, self-signed
 * certificates, host verify on certificates, password protection and storing
 * and reusing credentials for password protected sites.
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
     * The name of the file which may be contained in the body of a http
     * response to handle a redirect.
     */
    private static final String HTTP_REDIRECT = "HttpRedirect.htm";

    /**
     * Whether the Authentication Window has been closed by the user, without logging in.
     */
    public static boolean sAuthWindowCancelled = false;

    /**
     * Opens a connection to the <tt>address</tt>.
     * @param address the address to contact.
     * @return the result if any or null if connection was not possible
     * or canceled by user.
     */
    public static HTTPResponseResult openURLConnection(String address)
    {
        try
        {
            HttpGet httpGet = new HttpGet(address);
            DefaultHttpClient httpClient = getHttpClient(httpGet.getURI().getHost());

            HttpResponse response = executeMethod(httpClient, httpGet);

            if (response == null)
            {
                return null;
            }

            return new HTTPResponseResult(response, httpClient);
        }
        catch(Throwable t)
        {
            logger.error("Cannot open connection to:" + CommPortalService.getLoggableURL(address), t);
        }

        return null;
    }

    /**
     * Executes the method and return the result. Handle ask for password
     * when hitting password protected site.
     * Keep asking for password till user clicks cancel or enters correct
     * password. When 'remember password' is checked password is saved, if this
     * password and username are not correct clear them, if there are correct
     * they stay saved.
     * @param httpClient the configured http client to use.
     * @param req the request for now it is get or post.
     * @return the HttpResponse, or null if the request is forbidden or
     * some other error has occurred.
     */
    private static HttpResponse executeMethod(DefaultHttpClient httpClient,
                                            HttpRequestBase req)
        throws Throwable
    {
        // Do this when response (first execution) is null or till we are unauthorized
        // Don't repeat on Forbidden as we don't want that to behave like
        // incorrect password, but like "error".
        HttpResponse response = null;
        int redirects = 0;
        while(response == null
              || response.getStatusLine().getStatusCode()
                    == HttpStatus.SC_UNAUTHORIZED)
        {
            // if we were unauthorized, lets clear the method and recreate it
            // for new connection with new credentials.
            if(response != null
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
            sAuthWindowCancelled = !((HTTPCredentialsProvider)httpClient
                .getCredentialsProvider()).retry();

            // If the user clicked cancel no need to retry. Stop trying
            if(sAuthWindowCancelled)
            {
                logger.debug("User canceled credentials input.");
                break;
            }

            // check for post redirect as post redirects are not handled
            // automatically
            // RFC2616 (10.3 Redirection 3xx).
            // The second request (forwarded method) can only be a GET or HEAD.
            Header locationHeader = response.getFirstHeader("location");

            if(locationHeader != null
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
                if(en instanceof StringEntity)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    en.writeTo(out);
                    newLocation += "?" + out.toString("UTF-8");
                }

                req = new HttpGet(newLocation);
                req.setParams(oldreq.getParams());
                req.setHeaders(oldreq.getAllHeaders());

                redirects++;
                response = httpClient.execute(req);
            }
        }

        // if we finally managed to login return the result.
        if(response != null
            && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        {
            return response;
        }

        // is user has canceled no result needed.
        return null;
    }

    /**
     * Sending form as GET to <tt>address</tt>.
     * @param address HTTP address.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * @param prepopulatedUsername the value with which to prepopulate the username field of the
     * login dialog, if no username was found in config.
     * CredentialsStorageService service is used.
     * @param formParamNames the parameter names to include in the get.
     * @param formParamValues the corresponding parameter values to use.
     * @param usernameParamIx the index of the username parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @param passwordParamIx the index of the password parameter in the
     * <tt>formParamNames</tt> and <tt>formParamValues</tt>
     * if any, otherwise -1.
     * @return the result or null if send was not possible or
     * credentials ask if any was canceled.
     */
    public static HTTPResponseResult sendDataAsGet(String address,
                                   String usernamePropertyName,
                                   String passwordPropertyName,
                                   String prepopulatedUsername,
                                   List<String> formParamNames,
                                   List<String> formParamValues,
                                   int usernameParamIx,
                                   int passwordParamIx)
        throws Throwable
    {
        DefaultHttpClient httpClient;
        HttpGet getMethod;
        HttpResponse response = null;

        // if any authentication exception rise while executing
        // will retry
        AuthenticationException authEx;
        HTTPCredentialsProvider credentialsProvider = null;
        do
        {
            getMethod = new HttpGet(address);
            httpClient = getHttpClient(
                usernamePropertyName, passwordPropertyName,
                getMethod.getURI().getHost(), credentialsProvider);

            try
            {
                // execute post
                response = sendDataAsGet(
                        httpClient,
                        getMethod,
                        address,
                        usernamePropertyName,
                        passwordPropertyName,
                        prepopulatedUsername,
                        formParamNames,
                        formParamValues,
                        usernameParamIx,
                        passwordParamIx);

                authEx = null;
            }
            catch(AuthenticationException ex)
            {
                authEx = ex;

                // lets reuse credentialsProvider
                credentialsProvider = (HTTPCredentialsProvider)
                    httpClient.getCredentialsProvider();

                credentialsProvider.authenticationFailed();
            }
        }
        while(authEx != null);

        // canceled or no result
        if (response == null)
        {
            return null;
        }

        return new HTTPResponseResult(response, httpClient);
    }

    /**
     * Posting form to <tt>address</tt>. For submission we use POST method
     * which is "application/x-www-form-urlencoded" encoded.
     * @param httpClient the http client
     * @param getMethod the get method
     * @param address HTTP address.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * CredentialsStorageService service is used.
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
     * @return the HttpResponse or null if send was not possible or
     * credentials ask if any was canceled.
     */
    private static HttpResponse sendDataAsGet(
                                   DefaultHttpClient httpClient,
                                   HttpGet getMethod,
                                   String address,
                                   String usernamePropertyName,
                                   String passwordPropertyName,
                                   String prepopulatedUsername,
                                   List<String> formParamNames,
                                   List<String> formParamValues,
                                   int usernameParamIx,
                                   int passwordParamIx)
        throws Throwable
    {
        // if we have username and password in the parameters, lets
        // retrieve their values
        Credentials creds = null;
        if(usernameParamIx != -1
            && usernameParamIx < formParamNames.size()
            && passwordParamIx != -1
            && passwordParamIx < formParamNames.size())
        {
            URL url = new URL(address);
            HTTPCredentialsProvider prov = (HTTPCredentialsProvider)
                    httpClient.getCredentialsProvider();

            // don't allow empty username
            while(creds == null
                  || creds.getUserPrincipal() == null
                  || StringUtils.isNullOrEmpty(
                        creds.getUserPrincipal().getName()))
            {
                prov.setAuthenticationUsername(prepopulatedUsername);
                creds =  prov.getCredentials(
                        new AuthScope(url.getHost(), url.getPort()));

                sAuthWindowCancelled = !prov.retry();

                // it was user cancelled lets stop processing
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

                    ErrorDialog errorDialog =
                        new ErrorDialog(
                            null,
                            HttpUtilActivator.getResources().getI18NString(
                                "service.gui.ERROR"),
                            errorMessage);

                    errorDialog.setModal(true);
                    errorDialog.showDialog();
                }
            }
        }

        // construct the name value pairs we will be sending
        List<NameValuePair> parameters = new ArrayList<>();
        // there can be no params
        if(formParamNames != null)
        {
            for(int i = 0; i < formParamNames.size(); i++)
            {
                // we are on the username index, insert retrieved username value
                if(i == usernameParamIx && creds != null)
                {
                    parameters.add(new BasicNameValuePair(
                        formParamNames.get(i), creds.getUserPrincipal().getName()));
                }// we are on the password index, insert retrieved password val
                else if(i == passwordParamIx && creds != null)
                {
                    parameters.add(new BasicNameValuePair(
                        formParamNames.get(i), creds.getPassword()));
                }
                else // common name value pair, all info is present
                {
                    parameters.add(new BasicNameValuePair(
                        formParamNames.get(i), formParamValues.get(i)));
                }
            }
        }

        // Uses String UTF-8 to keep compatible with android version and
        // older versions of the http client libs, as the one used
        // in debian (4.1.x)
        String s = URLEncodedUtils.format(parameters, "UTF-8");
        getMethod.setURI(new URI(address + "?" + s));

        // execute post
        return executeMethod(httpClient, getMethod);
    }

    private static DefaultHttpClient getHttpClient(final String address) throws IOException
    {
        return getHttpClient(null, null, address, null);
    }

    /**
     * Returns the preconfigured http client,
     * using CertificateVerificationService, timeouts, user-agent,
     * hostname verifier, proxy settings are used from global java settings,
     * if protected site is hit asks for credentials
     * using util.swing.AuthenticationWindow.
     * @param usernamePropertyName the property to use to retrieve/store
     * username value if protected site is hit, for username
     * ConfigurationService service is used.
     * @param passwordPropertyName the property to use to retrieve/store
     * password value if protected site is hit, for password
     * CredentialsStorageService service is used.
     * @param credentialsProvider if not null provider will be reused
     * in the new client
     * @param address the address we will be connecting to
     */
    private static DefaultHttpClient getHttpClient(
        String usernamePropertyName,
        String passwordPropertyName,
        final String address,
        HTTPCredentialsProvider credentialsProvider)
        throws IOException
    {
        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000);
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        params.setParameter(ClientPNames.MAX_REDIRECTS, MAX_REDIRECTS);

        DefaultHttpClient httpClient = new DefaultHttpClient(params);

        // Hardcoded or else things break - we *do not* want this to be
        // branded.
        //
        // **** ALSO see WorkItem and SparkleActivator where the
        // user-agent string is duplicated *****
        HttpProtocolParams.setUserAgent(httpClient.getParams(),
                "Accession Communicator"
                + "/"
                + System.getProperty("sip-communicator.version"));

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

        Scheme sch = new Scheme("https", 443, new SSLSocketFactory(sslCtx,
                SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER));
        httpClient.getConnectionManager().getSchemeRegistry().register(sch);

        //TODO: wrap the SSLSocketFactory to use our own DNS resolution
        //TODO: register socketfactory for http to use our own DNS resolution

        // set proxy from default jre settings
        ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
            httpClient.getConnectionManager().getSchemeRegistry(),
        ProxySelector.getDefault());
        httpClient.setRoutePlanner(routePlanner);

        if(credentialsProvider == null)
            credentialsProvider =
                new HTTPCredentialsProvider(
                        usernamePropertyName, passwordPropertyName);
        httpClient.setCredentialsProvider(credentialsProvider);

        return httpClient;
    }

    /**
     * Checks whether we can reach a URL by executing an HTTP GET and looking
     * at the response.
     * @param address The URL we are trying to reach
     * @return True if we have received a '200 OK' response status code, or if
     * the body of the response contains a file which is handling a redirect.
     */
    public static boolean urlIsReachable(String address)
    {
        boolean urlIsReachable = false;
        try
        {
            HttpGet httpGet = new HttpGet(address);
            DefaultHttpClient httpClient = getHttpClient(httpGet.getURI().getHost());

            HttpResponse response = httpClient.execute(httpGet);

            logger.debug("Checking if url " +
                         CommPortalService.getLoggableURL(address) +
                         " is reachable. Received response: " + response);

            if (response != null)
            {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity httpEntity = response.getEntity();

                if (statusCode == HttpStatus.SC_OK ||
                    (statusCode == HttpStatus.SC_FORBIDDEN &&
                    EntityUtils.toString(httpEntity).contains(HTTP_REDIRECT)))
                {
                    // Some sites handle a redirect by returning a 403 forbidden
                    // status and including a HttpRedirect.htm file which
                    // redirects as appropriate.
                    urlIsReachable = true;
                }
            }
        }
        catch(Throwable t)
        {
            logger.error("An error occurred while attempting to reach " +
                         CommPortalService.getLoggableURL(address), t);
        }
        return urlIsReachable;
    }

    /**
     * Whether the Authentication Window has been closed by the user, without logging in.
     * @return true if the user closed the window
     */
    public static boolean authWindowCancelled()
    {
        return sAuthWindowCancelled;
    }
}
