/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import net.java.sip.communicator.util.Logger;

/**
 * A straightforward implementation of the IQProvider. Parses custom IQ packets.
 * We receive IQ mailbox packets from google mail servers and we use them to
 * create <tt>Mailbox</tt> objects which contain all the information from the
 * packet.
 *
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 */
public class MailboxIQProvider extends IQProvider<MailboxIQ>
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(MailboxIQProvider.class);

    /**
     * Return an <tt>IQ</tt> (i.e. <tt>Mailbox</tt>) object which will contain
     * the information we get from the parser.
     *
     * @param parser the <tt>XmlPullParser</tt> that we can use to get
     * packet details.
     *
     * @return a new IQ instance which is the result of the XmlPullParser.
     * @throws Exception if an error occurs parsing the XML.
     */
    public MailboxIQ parse(XmlPullParser parser,
                           int initialDepth,
                           XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException, SmackParsingException
    {
        MailboxIQ mailboxIQ = new MailboxIQ();

        String resultTimeStr = parser.getAttributeValue("", "result-time");

        if(resultTimeStr != null)
            mailboxIQ.setResultTime(Long.parseLong( resultTimeStr ));

        String totalMatchedStr = parser.getAttributeValue("", "total-matched");

        if( totalMatchedStr != null )
            mailboxIQ.setTotalMatched(Integer.parseInt( totalMatchedStr ));

        String totalEstimateStr
            = parser.getAttributeValue("", "total-estimate");

        if( totalEstimateStr != null )
            mailboxIQ.setTotalEstimate("1".equals( totalEstimateStr));

        mailboxIQ.setUrl(parser.getAttributeValue("", "url"));

        XmlPullParser.Event eventType = parser.next();
        while(eventType != XmlPullParser.Event.END_ELEMENT)
        {
            if (eventType == XmlPullParser.Event.START_ELEMENT)
            {
                String name = parser.getName();
                if(MailThreadInfo.ELEMENT_NAME.equals(name))
                {
                    //parse mail thread information
                    MailThreadInfo thread = MailThreadInfo.parse(parser);
                    mailboxIQ.addThread(thread);
                }
            }
            else
            {
                logger.trace("xml parser returned eventType=" + eventType);
                logger.trace("parser="+parser.getText());
            }
            eventType = parser.next();
        }

        return mailboxIQ;
    }
}
