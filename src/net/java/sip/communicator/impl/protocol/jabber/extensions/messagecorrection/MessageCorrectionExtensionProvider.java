/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber.extensions.messagecorrection;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

/**
 * Creates Smack packet extensions by parsing <replace /> tags
 * from incoming XMPP packets.
 *
 * @author Ivan Vergiliev
 */
public class MessageCorrectionExtensionProvider
    extends ExtensionElementProvider<MessageCorrectionExtension>
{
    /**
     * Creates a new correction extension by parsing an XML element.
     *
     * @param parser An XML parser.
     * @return A new MesssageCorrectionExtension parsed from the XML.
     * @throws XmlPullParserException
     * @throws IOException
     * @throws SmackParsingException
     */
    public MessageCorrectionExtension parse(XmlPullParser parser,
                                            int initialDepth,
                                            XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException, SmackParsingException
    {
        MessageCorrectionExtension res = new MessageCorrectionExtension(null);

        do
        {
            if (parser.getEventType() == XmlPullParser.Event.START_ELEMENT)
            {
                res.setCorrectedMessageUID(parser.getAttributeValue(
                        null, MessageCorrectionExtension.ID_ATTRIBUTE_NAME));
            }
        }
        while (parser.next() != XmlPullParser.Event.END_ELEMENT);

        return res;
    }
}
