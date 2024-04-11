// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static org.jitsi.util.SanitiseUtils.sanitise;

import java.util.List;
import java.util.regex.Pattern;

import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.PrivacyUtils;
import org.jitsi.util.Hasher;
import org.jivesoftware.smackx.debugger.xmpp.XmppDebugger;

/**
 * Class to implement smack's XmppDebugger.Logger interface. A wrapper for jitsi's
 * logger.
 */
public class SmackXmppLogger implements XmppDebugger.Logger
{
    // XMPP messages are logged using the specific named logger defined in logging.properties
    private final Logger wrappedLogger = Logger.getLogger("jitsi.XmppLogger");
    private static final List<String> ELEMENTS_TO_FILTER = List.of("<iq", "<item", "<presence", "<message");
    private static final Pattern ATTR_PATTERN = Pattern.compile("(?:from|to|jid|name|nick)='([^']+)('|$)");
    private static final Pattern JID_PATTERN = Pattern.compile("<jid>(.+)</jid>");
    private static final Pattern ACTIVATE_PATTERN = Pattern.compile("<activate>(.+)</activate>");
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("<subject>(.+)</subject>");
    private static final Pattern STATUS_PATTERN = Pattern.compile("<status>(.+)</status>");
    private static final Pattern FULL_JID_WITH_DN_AS_RESOURCE_PATTERN = Pattern.compile("^.*/\\d+$");

    @Override
    public void log(String str)
    {

        // filter iq, presence, message logs
        if (ELEMENTS_TO_FILTER.stream().anyMatch(str::contains))
        {
            // example - peer id contained in "from", "to", "jid", "name" attributes
            str = sanitise(str, ATTR_PATTERN, SmackXmppLogger::sanitiseJid);

            // example - <jid>12345678@domain.com/jitsi-abcd123</jid>
            str = sanitise(str, JID_PATTERN, SmackXmppLogger::sanitiseJid);

            // example - <activate>12345678@domain.com/jitsi-abcd123</activate>
            str = sanitise(str, ACTIVATE_PATTERN, SmackXmppLogger::sanitiseJid);

            // example - <subject>abcdefg 12345</subject>
            str = sanitise(str, SUBJECT_PATTERN, Hasher::logHasher);

            // example - <status>Status - Custom Message</status>
            str = sanitise(str, STATUS_PATTERN, Hasher::logHasher);
        }

        wrappedLogger.info(str);
    }

    /**
     * We don't need to hash the resource, because it consists of user-agent and then random string,
     * unless it is DN.
     * @param jid to sanitise
     * @return Jid with relevant information hashed
     */
    private static String sanitiseJid(String jid)
    {
        final boolean isLastComponentDN = FULL_JID_WITH_DN_AS_RESOURCE_PATTERN.matcher(jid).matches();
        return PrivacyUtils.sanitisePeerId(jid, isLastComponentDN);
    }
}
