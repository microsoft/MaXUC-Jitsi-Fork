/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.*;
import java.awt.geom.*;

import javax.swing.*;

/**
 * Utility class for component related operations. For example: calculating size
 * of strings depending on the component, obtaining component index, updating
 * component tree UI, etc.
 *
 * @author Yana Stamcheva
 */
public class ComponentUtils
{
    /**
     * Returns the width in pixels of a text.
     *
     * @param c the component where the text is contained
     * @param text the text to measure
     * @return the width in pixels of a text.
     */
    public static int getStringWidth(Component c, String text)
    {
        return SwingUtilities.computeStringWidth(c
                .getFontMetrics(c.getFont()), text);
    }

    /**
     * Returns the size of the given text computed towards to the given
     * component.
     *
     * @param c the component where the text is contained
     * @param text the text to measure
     * @return the dimensions of the text
     */
    public static Dimension getStringSize(Component c, String text)
    {
        // get metrics from the graphics
        FontMetrics metrics = c.getFontMetrics(c.getFont());
        // get the height of a line of text in this font and render context
        int hgt = metrics.getHeight();
        // get the advance of my text in this font and render context
        int adv = metrics.stringWidth(text);
        // calculate the size of a box to hold the text with some padding.
        return new Dimension(adv+2, hgt+2);
    }

    /**
     * Returns the bounds of the given string.
     *
     * @param text the string to measure
     * @return the bounds of the given string
     */
    public static Rectangle2D getDefaultStringSize(String text)
    {
        Font font = UIManager.getFont("Label.font");
        FontRenderContext frc = new FontRenderContext(null, true, false);
        TextLayout layout = new TextLayout(text, font, frc);

        return layout.getBounds();
    }

    /**
     * A simple minded look and feel change: ask each node in the tree
     * to <code>updateUI()</code> -- that is, to initialize its UI property
     * with the current look and feel.
     *
     * @param c UI component.
     */
    public static void updateComponentTreeUI(Component c)
    {
        updateComponentTreeUI0(c);
        c.invalidate();
        c.validate();
        c.repaint();
    }

    /**
     * Returns the index of the given component in the given container.
     *
     * @param c the Component to look for
     * @param container the parent container, where this component is added
     * @return the index of the component in the container or -1 if no such
     * component is contained in the container
     */
    public static int getComponentIndex(Component c, Container container)
    {
        for (int i = 0, count = container.getComponentCount(); i < count; i++)
        {
            if (container.getComponent(i).equals(c))
                return i;
        }
        return -1;
    }

    /**
     * Simulates a mouse click on the given component, for use when the UI was
     * written assuming mouse operation only, but perhaps we need to allow for
     * keyboard operation as well.
     * @param source The component being interacted with
     */
    public static void simulateMouseClick(Component source)
    {
        final MouseEvent dummyClick =
                new MouseEvent(source,
                               MouseEvent.MOUSE_CLICKED,
                               System.currentTimeMillis(),
                               0, 0, 0, 0, 0, 1, false, 0);
        MouseListener[] mouseListeners = source.getMouseListeners();
        for (MouseListener listener : mouseListeners)
        {
            listener.mousePressed(dummyClick);

            if (dummyClick.isConsumed())
            {
                // Event was consumed, so don't pass to any other listeners.
                break;
            }
        }
    }

    /**
     * Repaints UI tree recursively.
     *
     * @param c UI component.
     */
    private static void updateComponentTreeUI0(Component c)
    {
        if (c instanceof JComponent)
        {
            JComponent jc = (JComponent) c;
            jc.invalidate();
            jc.validate();
            jc.repaint();
            JPopupMenu jpm =jc.getComponentPopupMenu();
            if(jpm != null && jpm.isVisible() && jpm.getInvoker() == jc)
            {
                updateComponentTreeUI(jpm);
            }
        }
        Component[] children = null;
        if (c instanceof JMenu)
        {
            children = ((JMenu)c).getMenuComponents();
        }
        else if (c instanceof java.awt.Container)
        {
            children = ((java.awt.Container)c).getComponents();
        }
        if (children != null)
        {
            for(int i = 0; i < children.length; i++)
                updateComponentTreeUI0(children[i]);
        }
    }
}
