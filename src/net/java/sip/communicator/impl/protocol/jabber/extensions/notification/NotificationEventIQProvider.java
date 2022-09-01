/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.notification;

import java.io.IOException;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

/**
 * The NotificationEventIQProvider parses Notification Event packets.
 *
 * @author Damian Minkov
 */
public class NotificationEventIQProvider
    extends IQProvider<NotificationEventIQ>
{
    /**
     * Parse the IQ sub-document and create an IQ instance. Each IQ must have a
     * single child element. At the beginning of the method call, the xml parser
     * will be positioned at the opening tag of the IQ child element. At the end
     * of the method call, the parser <b>must</b> be positioned on the closing
     * tag of the child element.
     *
     * @param parser an XML parser.
     * @return a new IQ instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    public NotificationEventIQ parse(XmlPullParser parser,
                    int initialDepth,
                    XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException, SmackParsingException
    {
        NotificationEventIQ result = new NotificationEventIQ();

        boolean done = false;
        while (!done)
        {
            XmlPullParser.Event eventType = parser.next();
            if(eventType == XmlPullParser.Event.START_ELEMENT)
            {
                switch (parser.getName())
                {
                    case NotificationEventIQ.EVENT_NAME:
                        result.setEventName(parser.nextText());
                        break;
                    case NotificationEventIQ.EVENT_VALUE:
                        result.setEventValue(parser.nextText());
                        break;
                    case NotificationEventIQ.EVENT_SOURCE:
                        result.setEventSource(parser.nextText());
                        break;
                }
            }
            else if(eventType == XmlPullParser.Event.END_ELEMENT)
            {
                if(parser.getName().equals(NotificationEventIQ.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return result;
    }
}
