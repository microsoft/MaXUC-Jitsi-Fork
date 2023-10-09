// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil.lookandfeel;

import java.awt.*;
import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.DesktopUtilActivator;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;

/**
 * Manages the Look and Feel of the application
 */
public class LookAndFeelManager
{
    private static final Logger logger =
        Logger.getLogger(LookAndFeelManager.class);

    private static final ResourceManagementService sResources =
            DesktopUtilActivator.getResources();

    private static final ConfigurationService sConfig =
            DesktopUtilActivator.getConfigurationService();

    private static final Color TEXT_COLOR =
            new Color(sResources.getColor("service.gui.DARK_TEXT"));

    /**
     * We normally set the main client font to the OS default font.  As well as
     * helping the client to match the overall look and feel of the OS, doing
     * this should also ensure that the font used will support all characters
     * needed to display the OS's configured default language correctly.
     * However, we have some Thai users who set their OS language to English, as
     * the Thai language does not translate well to short one-word instructions,
     * so apps and clients tend to be in English.  The default Windows font in
     * English (Segoe UI) does not support Thai characters, which means that any
     * Thai characters that those users may type into the client (e.g. in IMs or
     * contact names), do not display correctly on the Windows client.  To
     * handle this, we have a tailored branding option that overrides the
     * default font that the client uses on Windows from the one that is defined
     * in the OS.  For example, we recommend that Thai customers set this
     * branding option to Leelawadee, as that is the font that looks closest to
     * Segoe UI of those included in the standard Windows fonts that also
     * support both the Thai code page (874) and the Latin 1 code page (1252)
     * used for English and other Western European languages.  See SFR 541835
     * for more details.
     */
    private static final String OVERRIDE_DEFAULT_WIN_FONT_NAME =
            sConfig.global().getString(
                    "net.java.sip.communicator.plugin.desktoputil.OVERRIDE_DEFAULT_WIN_FONT_NAME",
                    "");

    /**
     * The default font to use on Windows. It's a "logical" font name,
     * which is mapped to a "physical" font using a font configuration file
     * {@see https://docs.oracle.com/en/java/javase/11/intl/font-configuration-files.htm}.
     * We provide a customized font configuration file {in lib/fontconfig.properties}),
     * where "sansserif" logical font is mapped to "Segoe UI" physical font, as
     * that is the default English font on Windows.
     * This may get overridden by the above branding option.
     */
    private static String DEFAULT_WIN_FONT_NAME = "sansserif";

    /**
     * The default font to use on Mac, set as "Verdana" in settings.
     */
    private static final String DEFAULT_MAC_FONT_NAME =
            sResources.getSettingsString("service.gui.FONT_NAME");

    /**
     * @return The default font, scaled to match the current OS scaling.
     * @param fontStyle The style to use for the font (e.g. Font.PLAIN, Font.BOLD).
     */
    public static Font getScaledDefaultFont(int fontStyle)
    {
        String defaultFontName = OSUtils.isWindows() ?
                DEFAULT_WIN_FONT_NAME : DEFAULT_MAC_FONT_NAME;
        Font defaultWinFont = new Font(defaultFontName,
                                       fontStyle,
                                       (int) ScaleUtils.getDefaultFontSize());
        logger.debug("Returning default font: " + defaultWinFont);
        return defaultWinFont;
    }

    /**
     * Set up the Look and Feel for the whole Application
     */
    public static void setLookAndFeel()
    {
        // Show tooltips immediately and specify a custom background.  Note that
        // we must set up the tooltip defaults before the rest of the look and
        // feel, otherwise the main contact list styling will be incorrect, but
        // we may end up subsequently overriding the tooltip font, if we need to
        // update all fonts in fixWindowsUIDefaults below.
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();

        UIManager.put("ToolTip.background",
                      new Color(sResources.getColor("service.gui.TOOLTIP_BACKGROUND")));

        if (OSUtils.IS_WINDOWS)
        {
            // Scale the tooltip and native windows Fonts.
            scaleFont("ToolTip.font");
            scaleFont("Panel.font");
        }

        toolTipManager.setInitialDelay(500);
        toolTipManager.setDismissDelay(60000);
        toolTipManager.setEnabled(true);

        // we need to set the UIDefaults class loader so that it may access
        // resources packed inside OSGI bundles
        UIManager.put("ClassLoader",
                          LookAndFeelManager.class.getClass().getClassLoader());

        /*
         * Attempt to use the OS-native LookAndFeel instead of
         * SIPCommLookAndFeel.
         */
        String laf = UIManager.getSystemLookAndFeelClassName();
        boolean lafIsSet = false;

        /*
         * SIPCommLookAndFeel used to be set in case the system L&F was the same
         * as the cross-platform L&F. Unfortunately, SIPCommLookAndFeel is now
         * broken because its classes are loaded by different ClassLoaders which
         * results in exceptions. That's why the check
         * !laf.equals(UIManager.getCrossPlatformLookAndFeelClassName()) is
         * removed from the if statement bellow and thus the cross-platform L&F
         * is preferred over SIPCommLookAndFeel.
         */
        if (laf != null)
        {
            /*
             * Swing does not have a LookAndFeel which integrates with KDE and
             * the cross-platform LookAndFeel is plain ugly. KDE integrates with
             * GTK+ so try to use the GTKLookAndFeel when running in KDE.
             */
            String gtkLookAndFeel
                = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";

            try
            {
                UIManager.setLookAndFeel(laf);
                lafIsSet = true;

                UIDefaults uiDefaults = UIManager.getDefaults();
                if (OSUtils.IS_WINDOWS)
                    fixWindowsUIDefaults(uiDefaults);
                // Workaround for SC issue #516
                // "GNOME SCScrollPane has rounded and rectangular borders"
                if (laf.equals(gtkLookAndFeel)
                        || laf.equals("com.sun.java.swing.plaf.motif.MotifLookAndFeel"))
                {
                    uiDefaults.put(
                        "ScrollPaneUI",
                        new javax.swing.plaf.metal.MetalLookAndFeel()
                            .getDefaults().get("ScrollPaneUI"));
                }
            }
            catch (ClassNotFoundException ex)
            {
                /*
                 * Ignore the exceptions because we're only trying to set
                 * the native LookAndFeel and, if it fails, we'll use
                 * SIPCommLookAndFeel.
                 */
            }
            catch (InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException ex)
            {
            }
        }

        if (!lafIsSet)
        {
            try
            {
                SIPCommLookAndFeel lf = new SIPCommLookAndFeel();
                SIPCommLookAndFeel.setCurrentTheme(
                        new SIPCommDefaultTheme());

                // Check the isLookAndFeelDecorated property and set the
                // appropriate default decoration.
                if (Boolean.parseBoolean(sResources.getSettingsString(
                        "impl.gui.IS_LOOK_AND_FEEL_DECORATED")))
                {
                    JFrame.setDefaultLookAndFeelDecorated(true);
                    JDialog.setDefaultLookAndFeelDecorated(true);
                }

                UIManager.setLookAndFeel(lf);
            }
            catch (UnsupportedLookAndFeelException ex)
            {
                logger.error("The provided Look & Feel is not supported.", ex);
            }
        }
    }

    /**
     * Replace a Font with an equivalent, but scaled in size.
     * @param fontPropertyName name of the font as a UIManager property.
     */
    private static void scaleFont(String fontPropertyName)
    {
        try
        {
            Font scaledFont = ((Font) UIManager.get(fontPropertyName)).deriveFont(ScaleUtils.getDefaultFontSize());
            UIManager.put(fontPropertyName, scaledFont);
        }
        catch (Exception e)
        {
            logger.error("Failed to scale " + fontPropertyName, e);
        }
    }

    private static void fixWindowsUIDefaults(UIDefaults uiDefaults)
    {
        /*
         * Windows actually uses different fonts for the controls in windows and
         * the controls in dialogs. Unfortunately, win.defaultGUI.font may not
         * be the font Windows will use for controls in windows but the one to
         * be used for dialogs. And win.messagebox.font will be the font for
         * windows but the L&F will use it for OptionPane which in turn should
         * rather use the font for dialogs. So swap the meanings of the two to
         * get standard fonts in the windows while compromising that dialogs may
         * appear in it as well (if the dialogs are created as non-OptionPanes
         * and in this case SIP Communicator will behave as Mozilla Firefox and
         * Eclipse with respect to using the window font for the dialogs).
         */
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Object menuFont = toolkit.getDesktopProperty("win.menu.font");
        Object messageboxFont
            = toolkit.getDesktopProperty("win.messagebox.font");
        logger.debug("win.messagebox.font: " + messageboxFont +
                     ", win.menu.font: " + menuFont);

        Object messageFont = uiDefaults.get("OptionPane.font");
        Object controlFont = uiDefaults.get("Panel.font");
        logger.debug("OptionPane.font: " + messageFont +
                     ", Panel.font: " + controlFont);

        // Before we fix up the fonts, check whether the Windows font has been
        // overridden in the branding and ensure we have saved the correct default
        // Windows font name to return when we are asked for it by the UI code.
        if (OVERRIDE_DEFAULT_WIN_FONT_NAME.isEmpty())
        {
            // The default Windows font hasn't been overridden.
            logger.info("Default Windows font not overridden: " +
                         DEFAULT_WIN_FONT_NAME);
        }
        else
        {
            // The default Windows font has been overridden, so store that
            // as the default Windows font name and make sure we do fix up the
            // Windows UI default fonts.
            DEFAULT_WIN_FONT_NAME = OVERRIDE_DEFAULT_WIN_FONT_NAME;
            logger.info("Default Windows font overridden to " +
                        OVERRIDE_DEFAULT_WIN_FONT_NAME);
        }

        // Set Windows font to UI defaults.
        messageFont = getScaledDefaultFont(Font.PLAIN);
        logger.info("Updating fonts to messageFont: " + messageFont +
                     ", controlFont: " + controlFont);

        uiDefaults.put("OptionPane.font", controlFont);
        uiDefaults.put("OptionPane.messageFont", controlFont);
        uiDefaults.put("OptionPane.buttonFont", controlFont);
        uiDefaults.put("PasswordField.font", controlFont);

        uiDefaults.put("Button.font", messageFont);
        uiDefaults.put("CheckBox.font", messageFont);
        uiDefaults.put("ComboBox.font", messageFont);
        uiDefaults.put("EditorPane.font", messageFont);
        uiDefaults.put("FormattedTextField.font", messageFont);
        uiDefaults.put("Label.font", messageFont);
        uiDefaults.put("List.font", messageFont);
        uiDefaults.put("RadioButton.font", messageFont);
        uiDefaults.put("Panel.font", messageFont);
        uiDefaults.put("ProgressBar.font", messageFont);
        uiDefaults.put("ScrollPane.font", messageFont);
        uiDefaults.put("Slider.font", messageFont);
        uiDefaults.put("Spinner.font", messageFont);
        uiDefaults.put("TabbedPane.font", messageFont);
        uiDefaults.put("Table.font", messageFont);
        uiDefaults.put("TableHeader.font", messageFont);
        uiDefaults.put("TextField.font", messageFont);
        uiDefaults.put("TextPane.font", messageFont);
        uiDefaults.put("TitledBorder.font", messageFont);
        uiDefaults.put("ToggleButton.font", messageFont);
        uiDefaults.put("ToolTip.font", messageFont);
        uiDefaults.put("Tree.font", messageFont);
        uiDefaults.put("Viewport.font", messageFont);
        uiDefaults.put("CheckBox.foreground", TEXT_COLOR);
        uiDefaults.put("Label.foreground", TEXT_COLOR);
        uiDefaults.put("TextArea.foreground", TEXT_COLOR);
        uiDefaults.put("EditorPane.foreground", TEXT_COLOR);
        uiDefaults.put("TextPane.foreground", TEXT_COLOR);

        // Workaround for bug 6396936 (http://bugs.sun.com): WinL&F : font for
        // text area is incorrect.
        uiDefaults.put("TextArea.font", uiDefaults.get("TextField.font"));
    }
}
