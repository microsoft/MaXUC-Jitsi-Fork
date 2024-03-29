/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;

/**
 * This class implements input event extension.
 *
 * @author Sebastien Vincent
 */
public class RemoteControlExtension implements ExtensionElement
{
    /**
     * AWT event that represents our <tt>RemoteControlExtension</tt>.
     */
    private final ComponentEvent event;

    /**
     * Size of the panel that contains video
     */
    private final Dimension videoPanelSize;

    /**
     * Constructor.
     *
     * @param event AWT event
     */
    public RemoteControlExtension(ComponentEvent event)
    {
        this.event = event;
        this.videoPanelSize = null;
    }

    /**
     * Get <tt>ComponentEvent</tt> that represents our
     * <tt>InputExtensionItem</tt>.
     *
     * @return AWT <tt>ComponentEvent</tt>
     */
    public ComponentEvent getEvent()
    {
        return event;
    }

    /**
     * Get the element name of the <tt>ExtensionElement</tt>.
     *
     * @return "remote-control"
     */
    public String getElementName()
    {
        return RemoteControlExtensionProvider.ELEMENT_REMOTE_CONTROL;
    }

    /**
     * Returns the XML namespace of the extension sub-packet root element.
     * The namespace is always "https://jitsi.org/protocol/inputevt".
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace()
    {
        return RemoteControlExtensionProvider.NAMESPACE;
    }

    /**
     * Get the XML representation.
     *
     * @return XML representation of the item
     */
    public String toXML()
    {
        String ret = null;

        if(event == null)
        {
            return null;
        }

        if(event instanceof MouseEvent)
        {
            MouseEvent e = (MouseEvent)event;

            switch(e.getID())
            {
            case MouseEvent.MOUSE_DRAGGED:
            case MouseEvent.MOUSE_MOVED:
                if(videoPanelSize != null)
                {
                    Point p = e.getPoint();
                    double x = (p.getX() / videoPanelSize.width);
                    double y = (p.getY() / videoPanelSize.height);
                    ret = RemoteControlExtensionProvider.getMouseMovedXML(x, y);
                }
                break;
            case MouseEvent.MOUSE_WHEEL:
                MouseWheelEvent ew = (MouseWheelEvent)e;
                ret = RemoteControlExtensionProvider.getMouseWheelXML(
                        ew.getWheelRotation());
                break;
            case MouseEvent.MOUSE_PRESSED:
                ret = RemoteControlExtensionProvider.getMousePressedXML(
                        e.getModifiers());
                break;
            case MouseEvent.MOUSE_RELEASED:
                ret = RemoteControlExtensionProvider.getMouseReleasedXML(
                        e.getModifiers());
                break;
            default:
                break;
            }
        }
        else if(event instanceof KeyEvent)
        {
            KeyEvent e = (KeyEvent)event;
            int keycode = e.getKeyCode();
            int key = e.getKeyChar();

            if(key != KeyEvent.CHAR_UNDEFINED)
            {
                keycode = e.getKeyChar();
            }
            else
            {
                keycode = e.getKeyCode();
            }

            if(keycode == 0)
            {
                return null;
            }

            switch(e.getID())
            {
            case KeyEvent.KEY_PRESSED:
                ret = RemoteControlExtensionProvider.getKeyPressedXML(keycode);
                break;
            case KeyEvent.KEY_RELEASED:
                ret = RemoteControlExtensionProvider.getKeyReleasedXML(keycode);
                break;
            case KeyEvent.KEY_TYPED:
                ret = RemoteControlExtensionProvider.getKeyTypedXML(keycode);
                break;
            default:
                break;
            }
        }

        return ret;
    }

    @Override
    public String toXML(XmlEnvironment enclosingNamespace)
    {
        return toXML();
    }
}
