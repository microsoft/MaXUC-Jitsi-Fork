// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * A custom menu item which displays nicely in the Call Manager pop-up menus.
 * This either represents a title item - just a BOLD string, or a selectable
 * item that has (from left to right)
 * <li>A tick if the item is selected</li>
 * <li>An optional icon</li>
 * <li>Some text</li>
 */
public class SIPCommMenuItem extends JMenuItem
{
    private static final long serialVersionUID = 1L;

    // Colors
    protected static final Color DISABLED_TEXT_COLOR = new Color(0, 0, 0, 127);
    protected static final Color ENABLED_TEXT_COLOR = new Color(0, 0, 0);

    /**
     * Padding to place between UI elements
     */
    private static final int PADDING = 2;

    /**
     * A "tick" to display when the item is selected
     */
    private final BufferedImageFuture SELECTED_ICON = UtilActivator.getResources()
                            .getBufferedImage("plugin.callmanager.TICK");

    /**
     * Colour to use on items that have the mouse over them
     */
    private final Color HOVER_COLOUR = new Color(
        UtilActivator.getResources().getColor(
            "service.gui.LIGHT_HIGHLIGHT",
            UtilActivator.getResources()
                .getColor("service.gui.LIGHT_HIGHLIGHT")
        ));

    /**
     * Optional image to display
     */
    private final BufferedImageFuture mImage;

    /**
     * Creates a menu item for a normal, selectable item with no text set and a
     * null image.
     */
    public SIPCommMenuItem()
    {
        this("");
    }

    public SIPCommMenuItem(String text)
    {
        this(text, (BufferedImageFuture) null);
    }

    /**
     * Creates a menu item for a normal, selectable item.  Note that, on Mac
     * only, if the image passed in here is not null, the size of the menu item
     * will be fixed to the correct size for the current text and image.
     * Therefore, to update the text or image on Mac, replace the menu item
     * with a  new one, rather than just updating the existing one, otherwise
     * the text or image may not fit correctly in the menu.
     *
     * @param text The text to display
     * @param icon The optional image to display
     */
    public SIPCommMenuItem(String text, ImageIconFuture icon)
    {
        this(text, icon.getImage());
    }

    /**
     * Creates a menu item for a normal, selectable item.  Note that, on Mac
     * only, if the image passed in here is not null, the size of the menu item
     * will be fixed to the correct size for the current text and image.
     * Therefore, to update the text or image on Mac, replace the menu item
     * with a  new one, rather than just updating the existing one, otherwise
     * the text or image may not fit correctly in the menu.
     *
     * @param text The text to display
     * @param image The optional image to display
     */
    public SIPCommMenuItem(String text, BufferedImageFuture image)
    {
        super(text);

        mImage = image;

        setFont(new JLabel().getFont().deriveFont(
            Font.PLAIN, ScaleUtils.getScaledFontSize(11f)));
        setOpaque(false);

        // Update the size - only required on Macs as on Macs the images
        // appear clipped otherwise.  This will cause the size of the menu item
        // to be fixed, even if the text or image are later updated.
        if (OSUtils.IS_MAC && (mImage != null))
        {
            int height = mImage.resolve().getHeight(null) + PADDING*2;
            setPreferredSize(new Dimension(getPreferredSize().width, height));
        }
    }

    /**
     * Create a menu item for a non-focusable, non-selectable title with bold
     * text.
     *
     * @param title The title text to display
     */
    public static JLabel createHeading(String title)
    {
        JLabel label = new JLabel(title);

        // Make the text bold
        label.setFont(label.getFont().deriveFont(
                Font.BOLD, ScaleUtils.getScaledFontSize(11f)));

        return label;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2d = (Graphics2D)g;
        g2d.clearRect(0, 0, getWidth(), getHeight());

        int x = PADDING;
        int y = PADDING;

        if (isArmed() && isEnabled())
        {
            // Mouse is over, set the background to be the hover colour
            g2d.setColor(HOVER_COLOUR);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        if (!isEnabled())
        {
            // Make all items slightly transparent so they appear disabled
            AlphaComposite composite = AlphaComposite
                            .getInstance(AlphaComposite.SRC_OVER, 0.4f);
            g2d.setComposite(composite);
        }

        // If we are selected then draw the tick
        if (isSelected())
        {
            g2d.drawImage(SELECTED_ICON.resolve(), x, y, null);
        }

        // Increase the x coordinate regardless of whether the tick is
        // drawn or not - all items will then align nicely.
        x += SELECTED_ICON.resolve().getWidth(null) + PADDING;

        // Draw the image if we've got it.
        if (mImage != null)
        {
            Image image = mImage.resolve();
            g2d.drawImage(image, x, y, null);
            x += image.getWidth(null);
            x += PADDING;
        }

        // Draw the text in the centre of the item.
        g2d.setFont(getFont());
        g2d.setColor(isEnabled() ? ENABLED_TEXT_COLOR : DISABLED_TEXT_COLOR);
        Dimension size = ComponentUtils.getStringSize(this, getText());
        y = (getHeight() + size.height) / 2 - 2* PADDING;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g2d.drawString(getText(), x, y);
    }

    /**
     * Creates a separator ready to add to the menu.
     *
     * @return the separator that has been created
     */
    public static JSeparator createSeparator()
    {
        // Make a separator that displays nicely
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setBorder(BorderFactory.createEmptyBorder());
        separator.setOpaque(true);

        return separator;
    }
}
