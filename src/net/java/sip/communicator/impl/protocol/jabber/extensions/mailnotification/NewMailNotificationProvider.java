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
 * A straightforward implementation of an <tt>IQProvider</tt>. Parses custom
 * IQ packets related to new mail notifications from Google servers.
 * We receive IQ packets from the mail server to notify us that new mails are
 * available.
 *
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 */
public class NewMailNotificationProvider
    extends IQProvider<NewMailNotificationIQ>
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(NewMailNotificationProvider.class);

    /**
     * Returns an <tt>NewMailNotification</tt> instance containing the result
     * of the XMPP's packet parsing.
     *
     * @param parser the <tt>XmlPullParser</tt> that has the content of the
     * packet.
     * @return a new <tt>NewMailNotification</tt> instance with the result from
     * the <tt>XmlPullParser</tt>.
     */
    public NewMailNotificationIQ parse(XmlPullParser parser,
                                       int initialDepth,
                                       XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException, SmackParsingException
    {
        logger.debug("NewMailNotificationProvider.getChildElementXML usage");
        NewMailNotificationIQ iq = new NewMailNotificationIQ();

        return iq;
    }
}
