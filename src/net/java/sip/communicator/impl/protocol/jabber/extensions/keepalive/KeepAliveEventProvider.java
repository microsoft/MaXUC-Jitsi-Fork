/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive;

import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * The KeepAliveEventProvider parses ping iq packets.
 *
 * @author Damian Minkov
 */
public class KeepAliveEventProvider extends IQProvider<KeepAliveEvent>
{
    /**
     * Creates a new KeepAliveEventProvider.
     * ProviderManager requires that every ExtensionElementProvider has a public,
     * no-argument constructor
     */
    public KeepAliveEventProvider()
    {}

    /**
     * Parses a ping iq packet .
     *
     * @param parser an XML parser.
     * @return a new IQ instance.
     */
    public KeepAliveEvent parse(XmlPullParser parser,
                                int initialDepth,
                                XmlEnvironment xmlEnvironment)
         throws XmlPullParserException, IOException, SmackParsingException
    {
        KeepAliveEvent result = new KeepAliveEvent();

        String type = parser.getAttributeValue(null, "type");
        String id = parser.getAttributeValue(null, "id");
        Jid from = JidCreate.from(parser.getAttributeValue(null, "from"));
        Jid to = JidCreate.from(parser.getAttributeValue(null, "to"));

        result.setType(IQ.Type.fromString(type));
        result.setStanzaId(id);
        result.setFrom(from);
        result.setTo(to);

        return result;
    }
}
