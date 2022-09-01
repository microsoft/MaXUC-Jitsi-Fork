/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The SIPCommMenuUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommMenuUI
    extends BasicMenuUI
{
    /**
     * Creates a new SIPCommMenuUI instance.
     */
    public static ComponentUI createUI(JComponent x)
    {
        return new SIPCommMenuUI();
    }

    /**
     * Draws the background of the menu.
     *
     * @param g the paint graphics
     * @param menuItem menu item to be painted
     * @param bgColor selection background color
     * @since 1.4
     */
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor)
    {
        super.paintBackground(g, menuItem, bgColor);

        g = g.create();
        try
        {
            internalPaintBackground(g, menuItem, bgColor);
        }
        finally
        {
            g.dispose();
        }
    }

    private void internalPaintBackground(Graphics g, JMenuItem menuItem,
        Color bgColor)
    {
        AntialiasingManager.activateAntialiasing(g);

        ButtonModel model = menuItem.getModel();
        Color oldColor = g.getColor();

        int menuWidth = menuItem.getWidth();
        int menuHeight = menuItem.getHeight();

        if (menuItem.isOpaque())
        {
            if (model.isArmed()
                || (menuItem instanceof JMenu && model.isSelected()))
            {
                g.setColor(bgColor);
                g.fillRoundRect(0, 0, menuWidth, menuHeight, 5, 5);

                g.setColor(SIPCommLookAndFeel.getControlDarkShadow());
                g.drawRoundRect(0, 0, menuWidth - 1, menuHeight - 1, 5, 5);
            }
            else
            {
                g.setColor(menuItem.getBackground());
                g.fillRoundRect(0, 0, menuWidth, menuHeight, 5, 5);
            }
            g.setColor(oldColor);
        }
    }
}
