// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.sip.communicator.util.Logger;
import org.jitsi.util.Hasher;
import org.jivesoftware.smackx.debugger.xmpp.XmppDebugger;

/**
 * Class to implement smack's XmppDebugger.Logger interface. A wrapper for jitsi's
 * logger.
 */
public class SmackXmppLogger implements XmppDebugger.Logger
{
    // XMPP messages are logged using the specific named logger defined in logging.properties
    private static final Logger sWrappedLogger = Logger.getLogger("jitsi.XmppLogger");
    private static final List<String> ELEMENTS_TO_FILTER = List.of("<iq", "<item", "<presence", "<message");
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("([^@]+)@.+");
    private static final Pattern ATTR_PATTERN = Pattern.compile("(?:from|to|jid|name|nick)='([^']+)'");
    private static final Pattern JID_PATTERN = Pattern.compile("<jid>(.+)</jid>");
    private static final Pattern ACTIVATE_PATTERN = Pattern.compile("<activate>(.+)</activate>");
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("<subject>(.+)</subject>");
    private static final Pattern STATUS_PATTERN = Pattern.compile("<status>(.+)</status>");
    private static final String SUPPRESSED_MESSAGE = "*** details suppressed ***";

    @Override
    public void log(String logString)
    {
        String str = logString;

        // filter iq, presence, message logs
        if (ELEMENTS_TO_FILTER.stream().anyMatch(str::contains))
        {
            // example - peer id contained in "from", "to", "jid", "name" attributes
            str = getRedactedString(str, ATTR_PATTERN, SmackXmppLogger::sanitisePeerId);

            // example - <jid>12345678@domain.com/jitsi-abcd123</jid>
            str = getRedactedString(str, JID_PATTERN, SmackXmppLogger::sanitisePeerId);

            // example - <activate>12345678@domain.com/jitsi-abcd123</activate>
            str = getRedactedString(str, ACTIVATE_PATTERN, SmackXmppLogger::sanitisePeerId);

            // example - <subject>abcdefg 12345</subject>
            str = getRedactedString(str, SUBJECT_PATTERN, Hasher::logHasher);

            // example - <status>Status - Custom Message</status>
            str = getRedactedString(str, STATUS_PATTERN, s -> SUPPRESSED_MESSAGE);
        }

        sWrappedLogger.info(str);
    }

    /**
     * Returns a string with redacted value if it matches the provided pattern.
     *
     * @param original original string
     * @param pattern pattern to match
     * @param redactFunc function to redact found string
     * @return redacted string
     */
    public static String getRedactedString(final String original,
                                           final Pattern pattern,
                                           final Function<String, String> redactFunc)
    {
        if (original == null || original.length() == 0)
        {
            return original;
        }

        final Matcher matcher = pattern.matcher(original);
        if (matcher.find())
        {
            final StringBuilder stringToReturn = new StringBuilder();
            do
            {
                final String stringToRedact = matcher.group(0);
                final String valueToRedact = matcher.group(1);
                matcher.appendReplacement(stringToReturn, stringToRedact.replace(valueToRedact, redactFunc.apply(valueToRedact)));
            } while (matcher.find());

            matcher.appendTail(stringToReturn);
            return stringToReturn.toString();
        }

        return original;
    }

    /**
     * Returns a hashed String if the string value is one of the following formats:
     *      * 123456@domain
     *      * chatroom-123abc@domain
     *      * chatroom-123abc@domain/123456
     *      * chatroom-123abc
     * @param stringValue value to be sanitised
     * @return the Personal Data hashed string
     */
    public static String sanitisePeerId(String stringValue)
    {
        if (stringValue != null)
        {
            // PeerId can have 1-3 components divided by /, hash each component separately
            final String[] components = stringValue.split("/");
            for (int i=0; i<components.length; i++)
            {
                if (components[i].contains("@"))
                {
                    components[i] = getRedactedString(components[i], DOMAIN_PATTERN, Hasher::logHasher);
                }
                else
                {
                    components[i] = Hasher.logHasher(components[i]);
                }
            }

            return String.join("/", components);
        }

        return null;
    }
}
