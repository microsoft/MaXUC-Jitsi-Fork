// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;

import org.eclipse.swt.internal.win32.OS;

import net.java.sip.communicator.plugin.desktoputil.lookandfeel.LookAndFeelManager;
import net.java.sip.communicator.util.Logger;
import org.jitsi.util.OSUtils;

/**
 * The Utility controlling size scaling for fonts and panels when not using the
 * default screen size. This occurs in Windows when the user has enabled
 * accessibility mode, or simply changed the display settings. The user can
 * choose any scale up to 300% so we have to allow continuous values, rather
 * than discrete settings at 125%, 150% etc.
 */
public class ScaleUtils
{
    private static final Logger logger = Logger.getLogger(ScaleUtils.class);

    /**
     * The base DPI that should give a scale factor of 1.0 at 100% resolutions
     */
    private static final int BASE_DPI = 96;

    /**
     * The default font size used throughout the application
     */
    private static final double BASE_FONT_SIZE = 12D;

    /**
     * Medium-large size
     */
    private static final double MEDIUM_LARGE_FONT_SIZE = 11D;

    /**
     * Medium font size
     */
    private static final double MEDIUM_FONT_SIZE = 10D;

    /**
     * Small font size
     */
    private static final double SMALL_FONT_SIZE = 8D;

    /**
     * The applications current scaling
     */
    private static double scale = 1.0;

    /**
     * Set the scale based on the current display settings
     */
    public static void setScale()
    {
        if (OSUtils.IS_WINDOWS)
        {
            // To calculate the display scale, we must get the system DPI and
            // compare this to a base DPI to work out what the current system
            // scale is.
            //
            // This does not account for different scales on different monitors
            int hwnd = OS.GetDC(0);
            int x = OS.GetDeviceCaps(hwnd, OS.LOGPIXELSX);
            int y = OS.GetDeviceCaps(hwnd, OS.LOGPIXELSY);

            // x & y should be the same. If they aren't, take an average
            scale = ((double) (x + y)) / (2*BASE_DPI);

            logger.info("Setting display scale to: " + scale +
                ", using DPI: (" + x + "," + y + ")");
        }
        else
        {
            logger.info("Setting display scale to: " + scale);
        }
    }

    /**
     * Get the current system scale
     * @return The system scale as a double
     */
    public static double getScale2D()
    {
        return scale;
    }

    /**
     * Get the current system scale
     * @return The system scale as an int
     */
    public static int getScale()
    {
        return (int) Math.round(getScale2D());
    }

    /**
     * Get the scaled default font size
     * @return The scaled default font size
     */
    public static float getDefaultFontSize()
    {
        return (float) (BASE_FONT_SIZE * scale);
    }

    /**
     * Get the scaled medium font size
     * @return The scaled medium font size
     */
    public static float getMediumFontSize()
    {
        return (float) (MEDIUM_FONT_SIZE * scale);
    }

    /**
     * Get the scaled medium-large font size
     * @return The scaled medium font size
     */
    public static float getMediumLargeFontSize()
    {
        return (float) (MEDIUM_LARGE_FONT_SIZE * scale);
    }

    /**
     * Get the scaled small font size
     * @return The scaled small font size
     */
    public static float getSmallFontSize()
    {
        return (float) (SMALL_FONT_SIZE * scale);
    }

    /**
     * Get a scaled font size based off a provided size
     * @param fontSize the font size to base off
     * @return the scaled font size
     */
    public static float getScaledFontSize(float fontSize)
    {
        return (float) (fontSize * scale);
    }

    /**
     * Scales an integer using the current display scale
     * @param number integer to scale
     * @return the scaled result
     */
    public static int scaleInt(int number)
    {
        return (int) Math.round(number * scale);
    }

    /**
     * Unscales an integer based on the current display scale
     * @param number integer to unscale
     * @return the unscaled result
     */
    public static int unscaleInt(int number)
    {
        return (int) Math.round(number / scale);
    }

    /**
     * Create an empty border, with all the border widths scaled
     * @param top border width of the top edge in pixels
     * @param left border width of the top edge in pixels
     * @param bottom border width of the top edge in pixels
     * @param right border width of the top edge in pixels
     * @return the empty border created
     */
    public static Border createEmptyBorder(int top, int left, int bottom, int right)
    {
        return BorderFactory.createEmptyBorder(
            scaleInt(top),
            scaleInt(left),
            scaleInt(bottom),
            scaleInt(right));
    }

    /**
     * Constructs a border layout with the specified gaps between components.
     * The horizontal gap is specified by hgap and the vertical gap is
     * specified by vgap.  The gaps are both scaled.
     * @param hgap the horizontal gap
     * @param vgap the vertical gap
     * @return the border layout that was created
     */
    public static BorderLayout createBorderLayout(int hgap, int vgap)
    {
        return new BorderLayout(scaleInt(hgap), scaleInt(vgap));
    }

    /**
     * Adjusts the font size for the component to account for the display
     * magnification.
     *
     * @param component  The Component to scale the font for.
     */
    public static void scaleFontAsDefault(Component component)
    {
        if (OSUtils.IS_WINDOWS)
        {
            Font font = component.getFont();

            // font is only null in UTs where the Swing infrastructure hasn't
            // set-up a font for the component or its parent.
            if (font != null)
            {
                component.setFont(font.deriveFont(getDefaultFontSize()));
            }
        }
    }

    /**
     * Scale the font recursively for the component
     * @param comp the component containing the child components to be scaled
     */
    public static void scaleFontRecursively(Component[] comp)
    {
        // Only apply this to Windows, otherwise we over-scale on Mac.
        if (OSUtils.IS_WINDOWS)
        {
            for (Component component : comp)
            {
                if (component instanceof Container) scaleFontRecursively(((Container) component).getComponents());
                try
                {
                    component.setFont(LookAndFeelManager.getScaledDefaultFont(Font.PLAIN));
                }
                catch (Exception ignored) {}
            }
        }
    }
}
