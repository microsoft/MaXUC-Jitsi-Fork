// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.*;
import java.net.*;

import org.jitsi.service.configuration.*;

/**
 * Utility class which contains methods for detecting when AD is restricted by a captive wifi portal,
 * and methods which aim to deal with the situation.
 */
public class CaptiveWiFiUtils
{
    private static final Logger logger = Logger.getLogger(CaptiveWiFiUtils.class);

    /**
     * Default URL for the server which we request a http response code from.
     * Must be HTTP (not HTTPS) to make captive portal detection work.
     */
    private static final String DEFAULT_CAPTIVE_WIFI_SERVER_URL =
                                   "http://clients3.google.com/generate_204";

    /**
     * The resource where we store the URL for the server
     * which we request a http response from
     */
    private static final String CFG_CAPTIVE_WIFI_SERVER_URL =
                                  "net.java.sip.communicator.util.CAPTIVE_WIFI_SERVER_URL";

    /**
     * HTTP connection timeout.  We expect a response pretty much straight away.
     */
    private static final int CONNECTION_TIMEOUT_MS = 2 * 1000;

    /**
     * Method which checks if AD is restricted by a captive WiFi portal
     * @return True if AD is restricted by a captive WiFi portal, false otherwise
     */
    public static boolean isCaptivePortal()
    {
        ConfigurationService configService =
                                       UtilActivator.getConfigurationService();

        if (configService != null)
        {
            // On new install user() will return null until the user is configured.
            String serverUrlString = (configService.user() != null) ?
                configService.user().getString(CFG_CAPTIVE_WIFI_SERVER_URL,
                                               DEFAULT_CAPTIVE_WIFI_SERVER_URL) :
                DEFAULT_CAPTIVE_WIFI_SERVER_URL;

            try
            {
                logger.debug("Try getting a response from " + serverUrlString);
                URL serverUrl = new URL(serverUrlString);
                HttpURLConnection connectionToServer =
                    (HttpURLConnection) serverUrl.openConnection(); // lgtm[java/non-https-url] URL to determine captive portal must be HTTP, no data transferring

                connectionToServer.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                connectionToServer.setReadTimeout(CONNECTION_TIMEOUT_MS);

                // Do not allow the http request to access a cache - it
                // must connect to the live server to get the response
                connectionToServer.setUseCaches(false);

                // This server is designed to return a 204 response code. If
                // the response code received is not 204, it means the request
                // has been redirected and therefore AD is restricted by the
                // portal.
                boolean captivePortal =
                    (connectionToServer.getResponseCode() != 204);

                logger.debug("Connected to captive wifi: " + captivePortal);
                return captivePortal;
            }
            catch (MalformedURLException e)
            {
                // The URL is malformed
                logger.error("MalformedURLException for URL " + serverUrlString, e);
            }
            catch (IOException e)
            {
                // A connection could not be made. This means that
                // Accession probably is not restricted by a captive
                // wifi portal. More likely, there is no internet connection.
                logger.warn("IOException", e);
            }
        }
        else
        {
            logger.error("Config Service is null - " +
                         "unable to check for captive wifi portal");
        }

        return false;
    }
}
