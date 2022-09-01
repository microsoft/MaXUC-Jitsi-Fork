/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.swing.text.html.StyleSheet;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.LookAndFeelManager;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.resources.BufferedImageFuture;

/**
 * All look and feel related constants are stored here.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class Constants
{
    private static final Logger logger = Logger.getLogger(Constants.class);

    /*
     * ======================================================================
     * -------------------- FONTS AND COLOR CONSTANTS ------------------------
     * ======================================================================
     */
    /**
     * Background color for even records in call history.
     */
    public static Color CALL_HISTORY_EVEN_ROW_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.LIGHT_BACKGROUND"));

    /**
     * The start color used to paint a gradient selected background of some
     * components.
     */
    public static Color SELECTED_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * The end color used to paint a gradient selected background of some
     * components.
     */
    public static Color SELECTED_GRADIENT_COLOR
        = new Color(GuiActivator.getResources()
            .getColor("service.gui.LIGHT_HIGHLIGHT_GRADIENT"));

    /**
     * The start color used to paint a gradient mouse over background of some
     * components.
     */
    public static Color GRADIENT_DARK_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.LIGHT_BACKGROUND"));

    /**
     * The end color used to paint a gradient mouse over background of some
     * components.
     */
    public static Color GRADIENT_LIGHT_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.LIGHT_BACKGROUND"));

    /**
     * A color between blue and gray used to paint some borders.
     */
    public static Color BORDER_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.BORDER_SHADOW"));

    /**
     * A color between blue and gray (darker than the other one), used to paint
     * some borders.
     */
    public static Color LIST_SELECTION_BORDER_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.BORDER_SHADOW"));

    /**
     * The color used to paint the background of contact list groups.
     */
    public static Color CONTACT_LIST_GROUP_BG_COLOR
        = new Color(GuiActivator.getResources()
                .getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * The end color used to paint a gradient mouse over background of some
     * components.
     */
    public static Color CONTACT_LIST_GROUP_BG_GRADIENT_COLOR
        = new Color(GuiActivator.getResources().
            getColor("service.gui.LIGHT_HIGHLIGHT"));

    /**
     * The text colour to use for UI elements in the header of the main frame
     */
    public static Color MAIN_FRAME_HEADER_TEXT_COLOR
        = ConfigurationUtils.useNativeTheme() ? Color.BLACK :
            new Color(GuiActivator.getResources().getColor(
                "service.gui.MAIN_FRAME_HEADER_TEXT",
                GuiActivator.getResources().getColor(
                    "service.gui.DARK_TEXT"),
                    false));

    /*
     * ======================================================================
     * --------------------------- FONT CONSTANTS ---------------------------
     * ======================================================================
     */

    /**
     * The default <tt>Font</tt> object used through this ui implementation.
     */
    public static final Font FONT = LookAndFeelManager.getScaledDefaultFont(Font.PLAIN);

    /*
     * ======================================================================
     * ------------------------ PROTOCOL NAMES -------------------------------
     * ======================================================================
     */
    /**
     * The Jabber protocol.
     */
    public static final String JABBER = "Jabber";

    /**
     * The SIP protocol.
     */
    public static final String SIP = "SIP";

    /*
     * ======================================================================
     * ------------------------ OTHER CONSTANTS ------------------------------
     * ======================================================================
     */
    /**
     * The default path, where chat window styles could be found.
     */
    public static final String DEFAULT_STYLE_PATH
        = "resources/styles";

    /*
     * ======================================================================
     * ------------------------ SPECIAL CHARS LIST --------------------------
     * ======================================================================
     */
    /**
     * A list of all special chars that should be escaped for some reasons.
     */
    private static final int[] specialChars = new int[]
    {
        KeyEvent.VK_PLUS,
        KeyEvent.VK_MINUS,
        KeyEvent.VK_SPACE,
        KeyEvent.VK_ENTER,
        KeyEvent.VK_LEFT,
        KeyEvent.VK_RIGHT
    };

    /**
     * Checks if the given char is in the list of application special chars.
     *
     * @param charCode The char code.
     */
    public static boolean isSpecialChar(int charCode) {
        for (int specialChar : specialChars) {
            if (specialChar == charCode)
                return true;
        }
        return false;
    }

    /**
     * Returns the image corresponding to the given presence status.
     * @param status The presence status.
     * @return the image corresponding to the given presence status.
     */
    public static BufferedImageFuture getStatusIcon(PresenceStatus status)
    {
        ImageLoaderService imageLoaderService =
            GuiActivator.getImageLoaderService();

        if(status != null)
        {
            int connectivity = status.getStatus();

            if(connectivity < PresenceStatus.ONLINE_LSM_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_OFFLINE_LSM_ICON);
            }
            else if(connectivity < PresenceStatus.ONLINE_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_OFFLINE_ICON);
            }
            else if(connectivity < PresenceStatus.DND_LSM_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_DND_LSM_ICON);
            }
            else if(connectivity < PresenceStatus.DND_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_DND_ICON);
            }
            else if(connectivity < PresenceStatus.BUSY_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_BUSY_ICON);
            }
            else if(connectivity < PresenceStatus.ON_THE_PHONE_LSM_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_ON_THE_PHONE_LSM_ICON);
            }
            else if(connectivity < PresenceStatus.ON_THE_PHONE_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_ON_THE_PHONE_ICON);
            }
            else if(connectivity < PresenceStatus.MEETING_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_MEETING_ICON);
            }
            else if(connectivity < PresenceStatus.AWAY_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_AWAY_ICON);
            }
            else if(connectivity == PresenceStatus.RINGING_VALUE)
            {
                // This is an even more special status as it is only used for
                // SIP dialog presence.
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_RINGING_ICON);
            }
            else if(connectivity < PresenceStatus.AVAILABLE_FOR_CALLS_LSM_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_AVAILABLE_FOR_CALLS_LSM_ICON);
            }
            else if(connectivity < PresenceStatus.AVAILABLE_FOR_CALLS_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_AVAILABLE_FOR_CALLS_ICON);
            }
            else if(connectivity < PresenceStatus.AVAILABLE_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_AWAY_ICON);
            }
            else if(connectivity
                        < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_ONLINE_ICON);
            }
            else if(connectivity < PresenceStatus.MAX_STATUS_VALUE)
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_ONLINE_ICON);
            }
            else
            {
                return imageLoaderService
                    .getImage(ImageLoaderService.USER_OFFLINE_ICON);
            }
        }
        else
        {
            return imageLoaderService
                .getImage(ImageLoaderService.USER_OFFLINE_ICON);
        }
    }

    /**
     * Loads a chat window style.
     */
    public static void loadAdiumStyle(){

        new File(Constants.class.getClassLoader()
            .getResource(DEFAULT_STYLE_PATH + "/TotallyClear").toString());
    }

    /**
     * Temporary method to load the css style used in the chat window.
     * @param styleSheet style sheet
     * @param defaultFont default font
     */
    public static void loadSimpleStyle(StyleSheet styleSheet, Font defaultFont)
    {
        Reader r =
            new BufferedReader(
                new InputStreamReader(
                    GuiActivator.getResources().getSettingsInputStream(
                        "service.gui.HTML_TEXT_STYLE")));

        if (defaultFont != null)
            styleSheet.addRule(
                "body, div, h1, h2, h3, h4, h5, h6, h7, p, td, th { "
                    + "font-family: "
                    + defaultFont.getName()
                    + "; font-size: "
                    + ScaleUtils.scaleInt(defaultFont.getSize())
                    + "pt; }");

        try
        {
            styleSheet.loadRules(r, null);
        }
        catch (IOException ex)
        {
            logger.error("Failed to load CSS stream.", ex);
        }
        finally
        {
            try
            {
                r.close();
            }
            catch (IOException ex)
            {
                logger.error("Failed to close CSS stream.", ex);
            }
        }
    }

    /**
     * Reloads constants.
     */
    public static void reload()
    {
        CALL_HISTORY_EVEN_ROW_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.LIGHT_BACKGROUND"));

        SELECTED_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.LIGHT_HIGHLIGHT"));

        GRADIENT_DARK_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.LIGHT_BACKGROUND"));

        GRADIENT_LIGHT_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.LIGHT_BACKGROUND"));

        BORDER_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.BORDER_SHADOW"));

        LIST_SELECTION_BORDER_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.BORDER_SHADOW"));

        CONTACT_LIST_GROUP_BG_COLOR
            = new Color(GuiActivator.getResources()
                    .getColor("service.gui.LIGHT_HIGHLIGHT"));

        CONTACT_LIST_GROUP_BG_GRADIENT_COLOR
            = new Color(GuiActivator.getResources().
                getColor("service.gui.LIGHT_HIGHLIGHT"));
    }
}
