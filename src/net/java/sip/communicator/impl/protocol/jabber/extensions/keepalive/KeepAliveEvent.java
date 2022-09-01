/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive;

import org.jivesoftware.smack.packet.IQ;
import org.jxmpp.jid.Jid;

/**
 * KeepAlive Event. Events are sent if there are no received packets
 * for a specified interval of time.
 * XEP-0199: XMPP Ping.
 *
 * @author Damian Minkov
 */
public class KeepAliveEvent extends IQ
{
    /**
     * Element name for ping.
     */
    public static final String ELEMENT_NAME = "ping";

    /**
     * Namespace for ping.
     */
    public static final String NAMESPACE = "urn:xmpp:ping";

    /**
     * Constructs empty packet
     */
    public KeepAliveEvent()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * Construct packet for sending.
     *
     * @param from the address of the contact that the packet coming from.
     * @param to the address of the contact that the packet is to be sent to.
     */
    public KeepAliveEvent(Jid from, Jid to)
    {
        super(ELEMENT_NAME, NAMESPACE);

        if (to == null)
        {
            throw new IllegalArgumentException("Parameter cannot be null");
        }

        setType(Type.get);
        setTo(to);
        setFrom(from);
    }

    /**
     * Returns the sub-element XML section of this packet
     *
     * @return the packet as XML.
     */
    @Override
    public IQChildElementXmlStringBuilder getIQChildElementBuilder(
        IQChildElementXmlStringBuilder xml)
    {
        xml.setEmptyElement();
        return xml;
    }
}
