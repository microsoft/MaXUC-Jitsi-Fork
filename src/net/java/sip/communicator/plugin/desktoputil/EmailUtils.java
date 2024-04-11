// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.regex.*;

import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.insights.enums.InsightsResultCode;
import net.java.sip.communicator.service.insights.parameters.CommonParameterInfo;

import org.apache.http.client.utils.*;
import org.jitsi.util.*;

/**
 * Utility methods for handling emails.
 */
public class EmailUtils
{
    private static final Logger sLog = Logger.getLogger(EmailUtils.class);

    // The strings used to create the regular expression to validate an e-mail
    // address.  These are the same as used by other Accession clients and
    // CommPortal Web.
    // Email text - allow all characters
    private static final String EMAIL_TEXT_PATTERN = "[a-zA-Z0-9!#$%&\'*+\\-/=?^_`{|}~]+";

    // Email username - a string formed of the above characters with optional single '.' or '\' characters inside
    private static final String EMAIL_USERNAME_PATTERN = EMAIL_TEXT_PATTERN + "([.\\\\]" + EMAIL_TEXT_PATTERN + ")*";

    // Email domain - a string formed of the above characters with optional single '.' characters inside
    private static final String EMAIL_DOMAIN_PATTERN = EMAIL_TEXT_PATTERN + "(\\." + EMAIL_TEXT_PATTERN + ")*";

    // The final e-mail address pattern: Username at the beginning, an '@' character and then the domain at the end
    private static final String EMAIL_ADDRESS_PATTERN = "^" + EMAIL_USERNAME_PATTERN + "@" + EMAIL_DOMAIN_PATTERN + "$";

    /** The regular expression used to validate email addresses */
    private static final Pattern EMAIL_ADDRESS_VALIDATER =
                                          Pattern.compile(EMAIL_ADDRESS_PATTERN);

    /**
     * Checks whether a string represents a valid email address.
     * @param emailString the string to check
     * @return true if the string is a valid email address
     */
    public static boolean isEmailAddress(String emailString)
    {
        Matcher matcher = EMAIL_ADDRESS_VALIDATER.matcher(emailString);
        return matcher.find();
    }

    /**
     * Sends a request to the OS to create a new email in the system's default
     * email client with the given parameters (where specified).
     *
     * @param toAddress the address to populate in the email's "to" field.
     *
     * @return true if the request succeeded, false otherwise.
     */
    public static boolean createEmail(String toAddress,
                                      String subject,
                                      String body)
    {
        // Don't log message body as it can contain Personal Data
        sLog.info("Creating email to: '" + logHasher(toAddress) + "'");

        URI uri = null;
        boolean success = false;

        try
        {
            // First, build a "mailto" URI, adding the to address, subject and
            // body as parameters (if specified).
            URIBuilder builder = new URIBuilder();
            builder.setScheme("mailto");
            if (!StringUtils.isNullOrEmpty(toAddress))
            {
                builder.setHost(toAddress);
            }

            if (!StringUtils.isNullOrEmpty(subject))
            {
                builder.addParameter("subject", subject);
            }

            if (!StringUtils.isNullOrEmpty(body))
            {
                builder.addParameter("body", body);
            }

            uri = builder.build();

            // The URI builder will have escaped all spaces in the subject and
            // body with + signs.  These won't be unescaped correctly when we
            // browse to the URI, so we need to make sure they are escaped with
            // %20 instead. To do this, we first convert the URI to a String
            // and replace all + signs with %20.  This won't replace any +
            // signs that were part of the original subject or body, as these
            // will already have been correctly escaped (as %2B) by the URI
            // builder. Finally, we convert the modified string back to a URI
            // and browse to it.
            String uriString = uri.toString().replaceAll("\\+", "%20");

            // If we added a 'host' (i.e. a To address) the URI will erroneously have added a "//" in after the scheme.
            // Remove it because it's incompatible with certain older versions of Outlook.
            uriString = uriString.replaceAll("mailto://","mailto:");

            // Don't log URI as it can contain Personal Data
            sLog.info("launch mailto URI");
            uri = URI.create(uriString);
            Desktop.getDesktop().browse(uri);
            success = true;

            sendEmailComposeAnalytic(InsightsResultCode.SUCCESS);
        }
        catch (URISyntaxException e)
        {
            sendEmailComposeAnalytic(InsightsResultCode.FAILURE_UNKNOWN);
            sLog.error("Unable to create uri " , e);
        }
        catch (IOException e)
        {
            sendEmailComposeAnalytic(InsightsResultCode.FAILURE_UNKNOWN);
            sLog.error("Unable to browse to " + uri, e);
        }

        return success;
    }

    private static void sendEmailComposeAnalytic(InsightsResultCode resultCode)
    {
        DesktopUtilActivator.getInsightsService().logEvent(
                InsightsEventHint.DESKTOP_UTIL_EMAIL_COMPOSE.name(),
                Map.of(
                        CommonParameterInfo.INSIGHTS_RESULT_CODE.name(),
                        resultCode
                )
        );
    }
}
