// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util.launchutils;

import static net.java.sip.communicator.util.launchutils.LaunchArgHandler.sanitiseArgument;
import static org.jitsi.util.Hasher.logHasher;

import net.java.sip.communicator.util.Logger;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

import org.jitsi.util.StringUtils;

public final class ProvisioningParams
{
    private static final Logger sLog = Logger.getLogger(ProvisioningParams.class);

    // Login URIs are of the form:
    //
    //   'accessionlogin:login?cdapID=<CDAP ID>&subscriber=<SUBSCRIBER>&password=<PASSWORD>'
    //
    // We define here the URI name and type that we enforce for the received
    // URI to be considered valid.
    public static final String URI_NAME = "accessionlogin";
    public static final String URI_TYPE = "login";

    // The valid parameters in a login URI that we should attempt to parse for.
    private static final String PARAM_CDAP_ID_KEY = "cdapid";
    private static final String PARAM_SUBSCRIBER_KEY = "subscriber";
    private static final String PARAM_PASSWORD_KEY = "password";

    // Store the params from the most recent login URI we've received. These are
    // marked as volatile as they are read/written from different threads.
    // Marking them as volatile ensures that read and writes to these variables
    // are synchronised. It saves us having to put a synchronized block around
    // each usage of each variable.
    private static volatile String sCdapID;
    private static volatile String sSubscriber;
    private static volatile String sPassword;

    @VisibleForTesting
    static SubmissionPublisher<Boolean> publisher = new SubmissionPublisher<>();

    /**
     * Suppress the constructor so that we can only use this class via the
     * parseProvisioningLink static method that is public.
     */
    private ProvisioningParams() {}

    /**
     * The public interface that the ProvisioningParams class is used by. This
     * method takes the URI and pulls out the CDAP ID, subscriber and password
     * parameters from the URI and saves them off.
     */
    public static void parseProvisioningLink(String uri)
    {
        sLog.info("Attempt to parse URI: " + sanitiseArgument(uri));

        // We shouldn't ever be passed a null URI to parse but if we are then
        // we should handle it gracefully and just refuse to parse it.
        if (uri == null)
        {
            sLog.info("Received null URI, don't attempt to parse it.");
            return;
        }

        String data = uri.replaceFirst("^" + URI_NAME + ":" , "");
        String[] parts = data.split("\\?", 2);
        String type = parts[0];
        String query = parts[1];

        // We should only go ahead and parse the query params if the URI type
        // is 'login'.
        if (URI_TYPE.equals(type))
        {
            sLog.info("URI type matched '" + URI_TYPE + "'");

            // If there was no "?" in the URI then we have no parameters to
            // parse so we should not attempt to parse the URI any further.
            if (query == null)
            {
                sLog.info("No query part of the URI, don't attempt to parse the URI.");
                return;
            }

            // Create a hash map of key/value pairs from the URI. We define a
            // regex that captures name/value pairs of parameters in the query.
            // The first group is all of the things to the left of the equals
            // characters, i.e. the parameter names, and the second group is all
            // of the things to the right of the equals characters, i.e. the
            // parameter values.
            Map<String, String> params = new HashMap<>();
            Pattern pattern = Pattern.compile("([^&=]+)=([^&]*)");
            Matcher matcher = pattern.matcher(query);

            // Go through the regex and find add all the matches to the hash
            // map so that we can retrieve them after.
            while (matcher.find())
            {
                // Convert parameter name to lowercase before we put it in the
                // hash map so that we can successfully match independently of
                // caseness of the URI parameter names.
                String name = matcher.group(1).toLowerCase();
                String value = matcher.group(2);

                // Log out the parameters we've found. We're careful here to not
                // print the user's password or subscriber DN unless sanitized
                if (PARAM_PASSWORD_KEY.equals(name))
                {
                    sLog.debug("Received '" + name + "' parameter in URI.");
                }
                else if (PARAM_SUBSCRIBER_KEY.equals(name))
                {
                    sLog.debug("Received '" + name + "' parameter with value '" + logHasher(value) + "' in URI.");
                }
                else
                {
                    sLog.debug("Received '" + name + "' parameter with value '" + value + "' in URI.");
                }

                // If the parameter name is 'password' then its value should
                // have been URL encoded. Therefore we should decode the value
                // before we add it to the hashmap.
                if (PARAM_PASSWORD_KEY.equals(name))
                {
                    sLog.debug("Attempt to URL decode password parameter.");

                    // Try and update the value to reflect that it has been
                    // URL decoded.
                    value = java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
                }

                // Add the name/value pair to the hash map.
                params.put(name, value);
            }

            sLog.debug("Parsed URI successfully");

            // Update the most recent stored URI parameters by reading the
            // parameters that we just parsed into the hash map.
            sCdapID = params.get(PARAM_CDAP_ID_KEY);
            sSubscriber = params.get(PARAM_SUBSCRIBER_KEY);
            sPassword = params.get(PARAM_PASSWORD_KEY);

            // We don't care about empty values.
            if (StringUtils.isNullOrEmpty(sCdapID))
            {
                sCdapID = null;
            }
            if (StringUtils.isNullOrEmpty(sSubscriber))
            {
                sSubscriber = null;
            }
            if (StringUtils.isNullOrEmpty(sPassword))
            {
                sPassword = null;
            }

            publisher.submit(true);
        }
    }

    /**
     * Return the name of the URI that we can use to check if this was a CDAP
     * provisioning URI.
     */
    public static String getLoginUriName()
    {
        return URI_NAME;
    }

    /**
     * Getter for the subscriber parameter value part of a parsed URI. We use a
     * getter because we don't want to expose the subscriber as we don't want to
     * be able to set the subscriber other than via parsing a URI.
     */
    public static String getSubscriber()
    {
        return sSubscriber;
    }

    /**
     * Getter for the password parameter value part of a parsed URI. We use a
     * getter because we don't want to expose the password as we don't want to be
     * able to set the password other than via parsing a URI.
     */
    public static String getPassword()
    {
        return sPassword;
    }

    /**
     * Getter for the CDAP ID parameter value part of a parsed URI. We use a
     * getter because we don't want to expose the CDAP ID as we don't want to be
     * able to set the CDAP ID other than via parsing a URI.
     */
    public static String getCdapID()
    {
        return sCdapID;
    }

    /**
     * Clears the saved off information so that if we fail to log in
     * successfully we don't keep trying and failing repeatedly.
     */
    public static void clear()
    {
        sLog.info("Clear saved URI parameters");
        sCdapID = null;
        sSubscriber = null;
        sPassword = null;
    }

    /**
     * Returns true if cdapID, username and password were provided via link.
     */
    public static boolean shouldAutomaticallyLoginViaLink() {
        return sCdapID != null && sSubscriber != null && sPassword != null;
    }

    /**
     * Attaches the subscriber to the publisher, so it receives new events
     * pushed by the publisher.
     */
    public static void observeLoginLinkClicks(Flow.Subscriber<Boolean> subscriber) {
        publisher.subscribe(subscriber);
    }

    /**
     * Unsubscribes all subscribers from the publisher, and closes the publisher.
     */
    public static void unsubscribe() {
        publisher.close();
    }
}
