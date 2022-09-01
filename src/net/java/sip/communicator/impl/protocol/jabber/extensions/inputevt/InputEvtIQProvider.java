/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

/**
 * Implements an <tt>IQProvider</tt> which parses incoming <tt>InputEvtIQ</tt>s.
 *
 * @author Sebastien Vincent
 */
public class InputEvtIQProvider extends IQProvider<InputEvtIQ>
{
    /**
     * Parse the Input IQ sub-document and returns the corresponding
     * <tt>InputEvtIQ</tt>.
     *
     * @param parser XML parser
     * @return <tt>InputEvtIQ</tt>
     * @throws Exception if something goes wrong during parsing
     */
    public InputEvtIQ parse(XmlPullParser parser,
                            int initialDepth,
                            XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException, SmackParsingException
    {
        InputEvtIQ inputEvtIQ = new InputEvtIQ();
        InputEvtAction action
            = InputEvtAction.parseString(
                    parser.getAttributeValue("", InputEvtIQ.ACTION_ATTR_NAME));

        inputEvtIQ.setAction(action);

        boolean done = false;

        while (!done)
        {
            switch (parser.next())
            {
            case START_ELEMENT:
                // <remote-control>
                if (RemoteControlExtensionProvider.ELEMENT_REMOTE_CONTROL
                        .equals(parser.getName()))
                {
                    RemoteControlExtensionProvider provider
                        = new RemoteControlExtensionProvider();
                    RemoteControlExtension item
                        = (RemoteControlExtension)
                            provider.parse(parser);

                    inputEvtIQ.addRemoteControl(item);
                }
                break;

            case END_ELEMENT:
                if (InputEvtIQ.ELEMENT_NAME.equals(parser.getName()))
                    done = true;
                break;

            default:
                break;
            }
        }

        return inputEvtIQ;
    }
}
