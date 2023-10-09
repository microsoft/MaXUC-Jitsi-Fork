/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;

/**
 * Input event IQ. It is used to transfer key and mouse events through XMPP.
 *
 * @author Sebastien Vincent
 */
public class InputEvtIQ extends IQ
{
    /**
     * The namespace that input event belongs to.
     */
    public static final String NAMESPACE = "https://jitsi.org/protocol/inputevt";

    /**
     * The name of the element that contains the input event data.
     */
    public static final String ELEMENT_NAME = "inputevt";

    /**
     * The name of the argument that contains the input action value.
     */
    public static final String ACTION_ATTR_NAME = "action";

    /**
     * Action of this <tt>InputIQ</tt>.
     */
    private InputEvtAction action = null;

    /**
     * List of remote-control elements.
     */
    private List<RemoteControlExtension> remoteControls =
            new ArrayList<>();

    /**
     * Constructor.
     */
    public InputEvtIQ()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * Get the XML representation of the IQ.
     *
     * @return XML representation of the IQ
     */
    @Override
    public IQChildElementXmlStringBuilder getIQChildElementBuilder(
        IQChildElementXmlStringBuilder xml)
    {
        xml.attribute(ACTION_ATTR_NAME, getAction().toString());

        if(remoteControls.size() > 0)
        {
            xml.rightAngleBracket();

            for(RemoteControlExtension p : remoteControls)
                xml.append(p);
        }
        else
        {
            xml.setEmptyElement();
        }

        return xml;
    }

    /**
     * Sets the value of this element's <tt>action</tt> attribute. The value of
     * the 'action' attribute MUST be one of the values enumerated here. If an
     * entity receives a value not defined here, it MUST ignore the attribute
     * and MUST return a <tt>bad-request</tt> error to the sender. There is no
     * default value for the 'action' attribute.
     *
     * @param action the value of the <tt>action</tt> attribute.
     */
    public void setAction(InputEvtAction action)
    {
        this.action = action;
    }

    /**
     * Returns the value of this element's <tt>action</tt> attribute. The value
     * of the 'action' attribute MUST be one of the values enumerated here. If
     * an entity receives a value not defined here, it MUST ignore the attribute
     * and MUST return a <tt>bad-request</tt> error to the sender. There is no
     * default value for the 'action' attribute.
     *
     * @return the value of the <tt>action</tt> attribute.
     */
    public InputEvtAction getAction()
    {
        return action;
    }

    /**
     * Add a remote-control extension.
     *
     * @param item remote-control extension
     */
    public void addRemoteControl(RemoteControlExtension item)
    {
        remoteControls.add(item);
    }

}
