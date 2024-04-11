// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import static java.util.Collections.emptySet;
import static org.jitsi.util.SanitiseUtils.*;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jitsi.util.Hasher;
import org.jitsi.util.SanitiseUtils;

/**
 * Class used to store variables and methods required to sanitise Personal Data from any data we
 * write to log files. Methods exist to sanitise Directory Numbers, IM addresses, SIP call peers etc.,
 * including fields of JSON data that include the aforementioned sensitive information.
 * PrivacyUtils is designed to provide the API used across the codebase to remove sensitive data where required;
 * Additional new methods/variables that need to be added for extra cases in the future should be
 * placed here only if they are for common data used in many classes, otherwise they must be defined in caller class.
 */
public final class PrivacyUtils
{
    public static final String REDACTED = "<redacted>";

    /* BEGINNING OF COMMPORTAL CONFIG CONSTANTS */

    /* These constants are typically found in the accession-config logs,
     * sip-communicator.properties, and other places where MaX logs data
     * regarding the user config it receives from CommPortal.
     */

    /**
     *  PAT and some passwords have a key name ending ENCRYPTED_PASSWORD,
     *  Generalising slightly for safety.
     */
    public static final String PASSWORD = "PASSWORD"; // lgtm[java/hardcoded-password-field] False positive: it's a property name, not a password value
    /**
     * Hardware device details.
     */
    public static final String DEVICE_LIST = "Device_list2";
    /**
     * Dial-in numbers.
     */
    public static final String BRANDING_INFO = "BRANDING_INFO";
    public static final String DIAL_IN_1 = "DIAL_IN_INFO_1";
    public static final String DIAL_IN_2 = "DIAL_IN_INFO_2";
    /**
     * Business group details.
     */
    public static final String EAS_BG_INFO = "EAS_BG_INFO";
    public static final String EAS_BUSINESS_GROUP = "EAS_BUSINESS_GROUP";
    public static final String GROUP_INFO = "GROUP_INFO";
    /**
     * My phones locations.
     */
    public static final String CTD_MY_PHONES = "ctd.myphones";
    /**
     * Scheduled assistants.
     */
    public static final String SCHEDULED_ASSISTANTS = "conference.impls.assistants";

    /**
     * Service URLs available in Tools menu.
     */
    public static final String URL_SERVICES = "gui.main.urlservices";
    /**
     * Suffix for several properties typically logged after receiving CommPortal config.
     * Typically seen in the accession-config log files.
     */
    public static final String ACCOUNT_UID = "ACCOUNT_UID";
    public static final String ACTIVE_USER = "ACTIVE_USER";
    public static final String AUTHORIZATION_NAME = "AUTHORIZATION_NAME";
    public static final String BASE_INFO = "BASE_INFO";
    public static final String CHATROOM = "chatRoom";
    public static final String CHAT_SUBJECT = "chatRoomSubject";
    public static final String COS_DATA = "COS_DATA";
    public static final String CUSTOM_STATUS = "CUSTOM_STATUS";
    public static final String DISPLAY_NAME = "DISPLAY_NAME";
    public static final String ENTITY_CAPS_MANAGER = "EntityCapsManager";
    public static final String GLOBAL_DISPLAY_NAME = "GLOBAL_DISPLAY_NAME";
    public static final String NUMBER = "NUMBER";
    public static final String USERNAME = "USERNAME";
    public static final String USER_ID = "USER_ID";
    public static final String VOICEMAIL_CHECK_URI = "VOICEMAIL_CHECK_URI";
    public static final String VOICEMAIL_URI = "VOICEMAIL_URI";
    public static final String SERVER_ADDRESS = "SERVER_ADDRESS";
    public static final String PREVIOUS_USER_EDUCATION_CHECK = "PREVIOUS_USER_EDUCATION_CHECK";
    public static final String RINGTONE_URI = "RINGTONE_URI";
    public static final String RINGTONE_PATH = "RINGTONE_PATH";
    public static final String SOUND_FILE_DESCRIPTOR = "soundFileDescriptor";
    /**
     * Subset of user (CommPortal) config options whose values expose Personal Data. Matching
     * is done based on simple logic to test if a config string contains any of the following
     * strings i.e. as a suffix.
     */
    public static final List<String> PRIVACY_RELATED_PROPERTIES =
        List.of(
            ACCOUNT_UID,
            ACTIVE_USER,
            AUTHORIZATION_NAME,
            BASE_INFO,
            BRANDING_INFO,
            CHATROOM,
            CHAT_SUBJECT,
            COS_DATA,
            CTD_MY_PHONES,
            SCHEDULED_ASSISTANTS,
            CUSTOM_STATUS,
// I don't know how a decision was made to include device list here.
// Since the handling we do is to remove DNs from the data, and I can't
// see how DNs would be part of that data, I don't think it should be
// considered privacy related.
//            DEVICE_LIST,
            DIAL_IN_1,
            DIAL_IN_2,
            DISPLAY_NAME,
            EAS_BG_INFO,
            EAS_BUSINESS_GROUP,
            ENTITY_CAPS_MANAGER,
            GLOBAL_DISPLAY_NAME,
            GROUP_INFO,
            NUMBER,
            PASSWORD,
            PREVIOUS_USER_EDUCATION_CHECK,
            RINGTONE_PATH,
            RINGTONE_URI,
            SOUND_FILE_DESCRIPTOR,
            URL_SERVICES,
            USERNAME,
            USER_ID);
    public static final Pattern CHATROOM_PATTERN = Pattern.compile("chatroom-([a-z0-9]+)");

    /**
     * Using username part of <a href="https://www.rfc-editor.org/rfc/rfc5322#section-3.2.3">Internet message format</a>
     * The characters have been slightly reordered to move the hyphen to the end, so it is not treated as a range indicator.
     */
    public static final Pattern CHAT_ADDRESS_PATTERN = Pattern.compile("[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+(?=@)");

    public static final Pattern CP_URL_DN = Pattern.compile("(?<=DirectoryNumber=)([0-9]+)(?=&|$)");

    /**
     * Pattern to sanitise JSON values in Call Pull request parameter.
     * E.g. https://server/session<redacted>/line/action.js?returnerrorsnow=true&
     * version=8.0&request=%7B%22reason%22%3A%22singleStepTransfer%22%2C%22extensions%22%3A%7B
     * %22privateData%22%3A%7B%22private%22%3A%7B%22callJumpTargetType%22%3A%22number%22%2C
     * %22targetAlertInfo%22%3A%22info%3DX-MSW-client-jumpto%3Bid%3Dd70c3c24403142408074f420804b1eec%22%7D%7D%7D%2C
     * %22activeCall%22%3A%7B%22callID%22%3A%2250bd0494-487c-4d08-b932-647c95c338f7_%2B%2B1%2B%2B03f3974f10af4d653a86efec%22%2C
     * %22deviceID%22%3A%22123456789%22%7D%2C%22transferredTo%22%3A%22123456789%22%2C
     * %22objectType%22%3A%22SingleStepTransferCall%22%7D
     */
    public static final Pattern CALL_PULL_REQUEST = Pattern.compile("(?<=%22(?:deviceID|transferredTo)%22%3A%22)([0-9]+)(?=%22)");
    public static final Pattern LINE_PATTERN = Pattern.compile("/line[0-9]+/");
    public static final Pattern SUBSCRIBER_DN_AND_ACC = Pattern.compile("(?<=acc)[0-9]+");
    public static final Pattern SUBSCRIBER_DN_ONLY = Pattern.compile("[0-9]+");
    // Valid matches: SUCCESSFUL_CONNECTION.Jabber:+12345678@, SUCCESSFUL_CONNECTION.Jabber:12345678@
    // where only the phone number is highlighted, with or without "+" prefix
    public static final Pattern SUCCESSFUL_JABBER_CONNECTION =
                                            Pattern.compile("(?<=SUCCESSFUL\\_CONNECTION\\.Jabber:)(\\+?[0-9]+)(?=@)");
    // Valid matches: SUCCESSFUL_CONNECTION.SIP:+12345678@, SUCCESSFUL_CONNECTION.SIP:12345678@
    // where only the phone number is highlighted, with or without "+" prefix
    public static final Pattern SUCCESSFUL_SIP_CONNECTION =
                                            Pattern.compile("(?<=SUCCESSFUL\\_CONNECTION\\.SIP:)(\\+?[0-9]+)(?=@)");
    public static final Pattern BG_ACC = Pattern.compile("(?<=bg\\.acc)([0-9]+)");
    public static final Pattern CP_ACC = Pattern.compile("(?<=commportal\\.acc)([0-9]+)");
    public static final Pattern CTD_ACC = Pattern.compile("(?<=ctd\\.acc)([0-9]+)");
    public static final Pattern GUI_ACC = Pattern.compile("(?<=gui\\.accounts\\.acc)([0-9]+)");
    public static final Pattern SIP_ACC = Pattern.compile("(?<=sip\\.acc)([0-9]+)");

    private static final Pattern SIP_REQUEST_ACCOUNT_ID = Pattern.compile("(?<=sip:)([0-9]{7,})");
    private static final Pattern SIP_REQUEST_FROM_NAME = Pattern.compile("(?<=From: \")([^\"]+)(?=\")");

    /**
     * Subset of user (CommPortal) config options whose values expose Personal Data. Matching
     * is done based on regex statements.
     */
    public static final List<Pattern> PRIVACY_PATTERNS = List.of(
            BG_ACC, CP_ACC, CTD_ACC, GUI_ACC, SIP_ACC,
            SUCCESSFUL_JABBER_CONNECTION, SUCCESSFUL_SIP_CONNECTION
    );

    /**
     * Patterns used to detect session IDs in CommPortal URLs.
     * The pattern to match could be "/session" + {at least 8 hex
     * characters} + "/" or it could be "session=" + {at least 8 hex
     * characters} + "&". We need to check both. 8 is an arbitrary number
     * which is low enough that it shouldn't miss any session IDs, but
     * high enough that it won't give false positives.
     */
    public static final List<Pattern> SESSION_PATTERNS = List.of(
            Pattern.compile("/session(\\p{XDigit}{8,})/"),
            Pattern.compile("session=(\\p{XDigit}{8,})&")
    );
    /* END OF COMMPORTAL CONFIG CONSTANTS */

    /**
     * Prevent instantiation.
     */
    private PrivacyUtils()
    {
    }

    /**
     * Returns true if the property should be hashed for privacy reasons.
     */
    public static boolean isPrivacyProperty(final String propertyName)
    {
        return propertyName != null && (
                PRIVACY_RELATED_PROPERTIES.stream()
                        .anyMatch(propertyName::contains) ||
                PRIVACY_PATTERNS.stream()
                        .map(pattern -> pattern.matcher(propertyName))
                        .anyMatch(Matcher::find));
    }

    /**
     * Hashes user DNs i.e. will hash the string "01234" to "HASH-VALUE(hash)".
     */
    public static String sanitiseDirectoryNumber(final Object value)
    {
        return sanitiseNullable(value, obj -> sanitise(obj.toString(), SUBSCRIBER_DN_ONLY));
    }

    /**
     * For strings containing a (sub)string of the form "acc[USER_DN_HERE]", such as
     * "net.java.sip.communicator.impl.protocol.sip.acc01234", this method will produce a hashed replacement
     * that reads "net.java.sip.communicator.impl.protocol.sip.acc_HASH-VALUE(hash)".
     */
    public static String sanitiseDirectoryNumberWithAccPrefix(final Object value)
    {
        return sanitiseNullable(value,
                obj -> sanitise(obj.toString(), SUBSCRIBER_DN_AND_ACC, val -> "_" + Hasher.logHasher(val)));
    }

    /**
     * For strings containing a (sub)string of the form
     * "abc.01234@im_domain.com", this method will produce a hashed replacement
     * that reads "HASH-VALUE(hash)@im_domain.com".
     */
    public static String sanitiseChatAddress(final Object value)
    {
        return sanitiseNullable(value, str -> sanitise(str.toString(), CHAT_ADDRESS_PATTERN));
    }

    /**
     * For strings containing a (sub)string of the form
     * "chatroom-abcd1234" or "chatroom-abcd1234@im_domain.com",
     * this method will produce a hashed replacement
     * that reads "chatroom-HASH-VALUE(hash)" or "chatroom-HASH-VALUE(hash)@im_domain.com".
     */
    public static String sanitiseChatRoom(final Object value)
    {
        return sanitiseNullable(value, obj -> sanitise(obj.toString(), CHATROOM_PATTERN));
    }

    /**
     * Overload of sanitisePeerId where we don't hash the last component.
     */
    public static String sanitisePeerId(final Object value)
    {
        return sanitisePeerId(value, true);
    }

    /**
     * Returns a hashed String if the string value is one of the following formats:
     *      * 123456@domain
     *      * chatroom-123abc@domain
     *      * chatroom-123abc@domain/123456
     *      * chatroom-123abc
     * The input value is split into components (by "/") and each component
     * is sanitised independently according to its format, whether it's chat room ID or DN.
     * @param value value to be sanitised
     * @param hashLastComponent - If there are multiple components (split by "/") should we hash the
     * last one?  For XMPP JIDs we don't want to, in general, because the resource doesn't contain PII.
     * @return the Personal Data hashed string
     */
    public static String sanitisePeerId(final Object value, boolean hashLastComponent)
    {
        if (value != null)
        {
            // PeerId can have 1-3 components separated by /, hash each component separately
            final String[] components = value.toString().split("/");
            int numComponentsToHash = components.length;
            if (!hashLastComponent)
            {
                numComponentsToHash = Math.max(components.length - 1, 1);
            }

            for (int i = 0; i < numComponentsToHash; i++)
            {
                final String component = components[i];
                String hashedValue = sanitiseChatRoom(component);
                if (component.equals(hashedValue))
                {
                    hashedValue = sanitiseChatAddress(component);
                    if (component.equals(hashedValue))
                    {
                        hashedValue = Hasher.logHasher(component);
                    }
                }

                components[i] = hashedValue;
            }

            return String.join("/", components);
        }

        return NULL_STRING;
    }

    /**
     * Handles removing Personal Data: both the meeting ID and the to/from IM addresses
     * included in certain conference messages.
     *
     * @param message the message to sanitise
     */
    public static String sanitiseHiddenMessage(final Object message)
    {
        if (message == null)
        {
            return NULL_STRING;
        }

        // Hash Meeting ID.
        String output = sanitiseJSON(message.toString(),
                                     Set.of("ID"),
                                     emptySet());

        // Hash IM addresses of meeting host and invitee.
        output = sanitiseJSON(output,
                              Set.of("From", "To"),
                              emptySet(),
                              PrivacyUtils::sanitiseChatAddress);
        return output;
    }

    /**
     * Sanitises SIP address and name in INVITE SIP request.
     * @param invite String representation of INVITE request
     * @return sanitised string
     */
    public static String sanitiseInvite(final String invite)
    {
        String sanitisedValue = sanitise(invite, SIP_REQUEST_ACCOUNT_ID);
        return sanitise(sanitisedValue, SIP_REQUEST_FROM_NAME);
    }

    /**
     * Removes PII from any filepath(s) in a string by removing the username.
     *
     * Eg "C:\Users\USERNAME\folder;C:\Users\USERNAME" will be
     * replaced with "C:\Users\<redacted>\folder;C:\Users\<redacted>"
     *
     * If passed something where the username is the last part of the filepath, any following
     * information will be eaten up to the next delimiter
     * example: "C:\Users\USERNAME is a file path" will become "C:\Users\<redacted>"
     *
     * @param stringToSanitise The string to be treated.
     * @return A string with the relevant PII removed
     */
    public static String sanitiseFilePath(String stringToSanitise)
    {
        return SanitiseUtils.sanitisePath(stringToSanitise);
    }

    /**
     * Returns a URL which can be logged. We use this when a URL contains
     * a session ID. We can't print the whole URl for security reasons, so we
     * remove a central part of the session ID. This still gives L3 the
     * information they need to be able to reconstruct the URL from server logs.
     *
     * @param url The URL to remove the session ID from
     * @return The same URL, but with the session ID modified as in
     *         obfuscateSessionId(). If no session ID is found, it returns the
     *         url input.
     */
    public static String getLoggableCPURL(final String url)
    {
        String result = url;

        // The pattern to match could be "/session" + {at least 8 hex
        // characters} + "/" or it could be "session=" + {at least 8 hex
        // characters} + "&". We need to check both. 8 is an arbitrary number
        // which is low enough that it shouldn't miss any session IDs, but
        // high enough that it won't give false positives.
        result = sanitiseFirstPatternMatch(result, SESSION_PATTERNS, PrivacyUtils::obfuscateSessionId);
        result = sanitise(result, LINE_PATTERN, str -> "/line" + REDACTED + "/");
        result = sanitise(result, CP_URL_DN, Hasher::logHasher);
        result = sanitise(result, CALL_PULL_REQUEST, Hasher::logHasher);

        return result;
    }

    /**
     * Returns a session ID substitute which is able to be logged. We don't
     * log the whole session ID due to security concerns, but it is useful for
     * L3 to be able to correlate with server-side logs, so we print a
     * partially redacted version which strips out the central characters.
     *
     * @param sessionId The session ID to modify
     * @return  There are 4 cases:
     *          1) null input is returned unchanged
     *          2) the ID has >= 20 characters. Return a string of the first 8
     *          and last 4 characters from the session ID, with "<redacted>" in
     *          between.
     *          3) the ID has <20 but >=16 characters. Just return the first 8
     *          characters followed by "<redacted>" as any more characters would
     *          compromise security.
     *          4) the ID has <=15 characters. We never expect this to happen.
     *          Return "<redacted as less than 16 characters>";
     */
    public static String obfuscateSessionId(final String sessionId)
    {
        String obfuscatedSessionId;
        if (sessionId == null)
        {
            obfuscatedSessionId = null;
        }
        else if (sessionId.length() >= 20)
        {
            obfuscatedSessionId = sessionId.substring(0,8) + REDACTED +
                                  sessionId.substring(sessionId.length() - 4);
        }
        else if (sessionId.length() >= 16)
        {
            obfuscatedSessionId = sessionId.substring(0,8) + REDACTED;
        }
        else
        {
            // This should never happen
            obfuscatedSessionId = "<redacted as less than 16 characters>";
        }
        return obfuscatedSessionId;
    }
}
