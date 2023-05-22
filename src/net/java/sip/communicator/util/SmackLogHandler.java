// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import static org.jitsi.util.SanitiseUtils.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

public class SmackLogHandler extends DefaultFileHandler
{
    /*
        e.g. org.jivesoftware.smack.roster.Roster$PresencePacketListener.processStanza() Roster not loaded while processing Presence Stanza [to=123456789@ams-domain.com/jitsi-2u9tj1u,from=1234567890@ams-domain.com/jitsi-2u9tj1u,type=unavailable,status=Replaced by new connection,]
        The following regex captures the string within the square brackets i.e., [ and ].
     */
    private static final Pattern STANZA_DATA_PATTERN = Pattern.compile("Stanza \\[(.*)\\]");

    /*
        e.g. Caused by: org.jivesoftware.smack.SmackException$NoResponseException: No response received within reply timeout. Timeout was 5000ms (~5s). StanzaCollector has been cancelled. Waited for response using: AndFilter: (StanzaTypeFilter: Presence, OrFilter: (AndFilter: (FromMatchesFilter (ignoreResourcepart): chatroom-mgv712g3@muc.amsterdam-xmpp.metaswitch.com, MUCUserStatusCodeFilter: status=110), AndFilter: (FromMatchesFilter (full): chatroom-mgv712g3@muc.amsterdam-xmpp.metaswitch.com/3612060116@amsterdam-xmpp.metaswitch.com/jitsi-2u4n31l, StanzaIdFilter: id=UM5MP-22, PresenceTypeFilter: type=error))).
        The following regex captures the string after FromMatchesFilter.
     */
    private static final Pattern FILTER_DATA_PATTERN = Pattern.compile("FromMatchesFilter \\([a-zA-Z]+\\): ([^,]*)");

    /*
        e.g.Text1 - org.jivesoftware.smack.AbstractXMPPConnection.waitForClosingStreamTagFromServer() Exception while waiting for closing stream element from the server XMPPTCPConnection[123456789@ams-domain.com/jitsi-2n33bl8]
        e.g.Text2 - org.jivesoftware.smack.AbstractXMPPConnection.callConnectionClosedOnErrorListener() Connection XMPPTCPConnection[123456789@ams-domain.com/jitsi-2n33bl8] (2) closed with error
        The following regex captures the string within the square brackets i.e., [ and ].
     */
    private static final Pattern TCP_CONNECTION_PATTERN = Pattern.compile("XMPPTCPConnection\\[(.*)\\]");

    private static final List<String> STANZA_ATTRIBUTES = List.of("from=", "to=", "status=");

    // matches the attributes that contain personal data
    private static final Pattern STANZA_ATTRIBUTE_PATTERN = Pattern.compile("(?:from|to|status)=([^,]+)");

    public SmackLogHandler() throws IOException, SecurityException
    {
    }

    @Override
    public synchronized void publish(LogRecord record)
    {
        String log = sanitise(record.getMessage(), STANZA_DATA_PATTERN, SmackLogHandler::sanitiseString);
        log = sanitise(log, FILTER_DATA_PATTERN, PrivacyUtils::sanitisePeerId);
        log = sanitise(log, TCP_CONNECTION_PATTERN, PrivacyUtils::sanitisePeerId);
        record.setMessage(log);

        super.publish(record);
    }

    private static String sanitiseString(String log)
    {
        return sanitiseValuesInList(Arrays.asList(log.split(",")), SmackLogHandler::sanitiseAttributes);
    }

    /**
     * Sanitise string if it contains "from", "to" or "status" name-value pairs.
     * Method sanitisePeerId will sanitise "from" and "to" attributes as chat addresses,
     * and will hash the whole value of "status".
     */
    private static String sanitiseAttributes(String attributes)
    {
        if (STANZA_ATTRIBUTES.stream().anyMatch(attributes::startsWith))
        {
            return sanitise(attributes, STANZA_ATTRIBUTE_PATTERN, PrivacyUtils::sanitisePeerId);
        }
        return attributes;
    }
}
