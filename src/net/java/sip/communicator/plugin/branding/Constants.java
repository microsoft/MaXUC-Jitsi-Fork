/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.io.*;

import javax.swing.text.html.*;

/**
 * All look and feel related constants are stored here.
 *
 * @author Yana Stamcheva
 */
public class Constants
{
    /*
     * ======================================================================
     * -------------------- FONTS AND COLOR CONSTANTS ------------------------
     * ======================================================================
     */

    /**
     * Dark blue color used in the about window and the splash screen.
     */
    public static final Color TITLE_COLOR
        = new Color(BrandingActivator.getResources()
                .getColor("service.gui.SPLASH_SCREEN_TITLE_COLOR"));

    /**
     * Text color used in the about window and the splash screen.
     */
    public static final String TEXT_COLOR
        = BrandingActivator.getResources()
            .getColorString("service.gui.SPLASH_SCREEN_TEXT_COLOR");

    /*
     * ======================================================================
     * --------------------------- FONT CONSTANTS ---------------------------
     * ======================================================================
     */

    /**
     * Temporary method to load the css style used in the chat window.
     *
     * @param style
     */
    public static void loadSimpleStyle(StyleSheet style)
    {
        importStyle(style, "service.gui.HTML_TEXT_STYLE", null);
    }

    /**
     * Import the CSS style for the splash screens into the given StyleSheet
     * object.
     *
     * @param style The StyleSheet to be populated.
     * @param defaultFont The font to use for all text types
     */
    public static void loadSplashScreenStyle(StyleSheet style, Font defaultFont)
    {
        importStyle(style, "service.gui.SPLASH_SCREEN_STYLE", defaultFont);
    }

    /**
     * Import CSS style data from a given resource into a specified style sheet.
     * @param style The StyleSheet to be populated.
     * @param source The settings key pointing to the CSS data to be imported.
     * @param defaultFont The default font to use for all text types
     */
    public static void importStyle(StyleSheet style, String source, Font defaultFont)
    {
        InputStream is = BrandingActivator.getResources().
                                                 getSettingsInputStream(source);

        Reader r = new BufferedReader(new InputStreamReader(is));
        try
        {
            style.loadRules(r, null);
            r.close();
        }
        catch (IOException e)
        {
        }

        if (defaultFont != null)
            style.addRule(
                    "body, div, h1, h2, h3, h4, h5, h6, h7, p, td, th { "
                    + "font-family: "
                    + defaultFont.getName()
                    + "; font-size: "
                    + defaultFont.getSize()
                    + "pt; }");
    }
}
