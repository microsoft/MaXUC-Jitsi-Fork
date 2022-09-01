// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

/**
 * A JMenu that is styled so that it fits with the SIPCommMenuItem
 */
public class SIPCommStyledMenu extends JMenu
{
    private static final long serialVersionUID = 1L;

    // Colors
    private static final Color DISABLED_TEXT_COLOR = new Color(0, 0, 0, 127);
    private static final Color ENABLED_TEXT_COLOR = new Color(0, 0, 0);
    private static final Color HOVER_COLOUR = new Color(
        DesktopUtilActivator.getResources().getColor(
            "service.gui.LIGHT_HIGHLIGHT",
            DesktopUtilActivator.getResources()
                .getColor("service.gui.LIGHT_HIGHLIGHT")
        ));

    public SIPCommStyledMenu(String text)
    {
        super(text);

        setFont(new JLabel().getFont().deriveFont(
                                Font.PLAIN, ScaleUtils.getScaledFontSize(11f)));
    }

    /**
     * Arrow drawn on the right hand side of the
     */
    private final BufferedImageFuture mRightArrow = DesktopUtilActivator.getResources()
                         .getImage("service.gui.icons.CLOSED_GROUP").getImage();

    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2d = (Graphics2D)g;
        g2d.clearRect(0, 0, getWidth(), getHeight());

        int PADDING = 2;
        int x = 20;
        int y = PADDING;

        if (isSelected() && isEnabled())
        {
            // Mouse is over, set the background to be the hover colour
            g2d.setColor(HOVER_COLOUR);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        // Draw the text in the centre of the item
        String text = getText();
        g2d.setFont(getFont());
        g2d.setColor(isEnabled() ? ENABLED_TEXT_COLOR : DISABLED_TEXT_COLOR);
        Dimension size = ComponentUtils.getStringSize(this, text);
        y = (getHeight() + size.height) / 2 - 2 * PADDING;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.drawString(text, x, y);

        BufferedImage rightArrow = mRightArrow.resolve();

        x = getWidth() - rightArrow.getWidth(null) - PADDING;
        y = (getHeight() - rightArrow.getHeight(null)) / 2;
        g2d.drawImage(rightArrow, x, y, null);
    }
}
