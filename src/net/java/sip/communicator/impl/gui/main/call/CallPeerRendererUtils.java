/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

/**
 * An utility class that reassembles common methods used by different
 * <tt>CallPeerRenderer</tt>s.
 *
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class CallPeerRendererUtils
{
    /**
     * Adds the given <tt>KeyListener l</tt> to the given <tt>component</tt>.
     *
     * @param component the component to which we add <tt>l</tt>
     * @param l the <tt>KeyListener</tt> to add
     */
    public static void addKeyListener(Component component, KeyListener l)
    {
        component.addKeyListener(l);
        if (component instanceof Container)
        {
            Component[] components = ((Container) component).getComponents();

            for (Component c : components)
                addKeyListener(c, l);
        }
    }

    /**
     * Gets the first <tt>Frame</tt> in the ancestor <tt>Component</tt>
     * hierarchy of a specific <tt>Component</tt>.
     * <p>
     * The located <tt>Frame</tt> (if any) is often used as the owner of
     * <tt>Dialog</tt>s opened by the specified <tt>Component</tt> in
     * order to provide natural <tt>Frame</tt> ownership.
     *
     * @param component the <tt>Component</tt> which is to have its
     * <tt>Component</tt> hierarchy examined for <tt>Frame</tt>
     * @return the first <tt>Frame</tt> in the ancestor
     * <tt>Component</tt> hierarchy of the specified <tt>Component</tt>;
     * <tt>null</tt>, if no such <tt>Frame</tt> was located
     */
    public static Frame getFrame(Component component)
    {
        while (component != null)
        {
            Container container = component.getParent();

            if (container instanceof Frame)
                return (Frame) container;

            component = container;
        }
        return null;
    }

    /**
     * Sets the given <tt>background</tt> color to the given <tt>component</tt>.
     *
     * @param component the component to which we set the background
     * @param background the background color to set
     */
    public static void setBackground(Component component, Color background)
    {
        component.setBackground(background);
        if (component instanceof Container)
        {
            Component[] components = ((Container) component).getComponents();

            for (Component c : components)
                setBackground(c, background);
        }
    }
}
