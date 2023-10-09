// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTextArea;

import net.java.sip.communicator.util.ConfigurationUtils;

/**
 * Provides a default layout for all configuration panels that consist of a list
 * of options.
 */
public class OptionListConfigurationPanel extends ConfigurationPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The color used for text in hint panels.
     */
    private static final Color HINT_TEXT_COLOR =
            new Color(DesktopUtilActivator.getResources().getColor(
                 "service.gui.MID_TEXT"));

    /**
     * Indicates max width for components.
     */
    public static final int MAXIMUM_WIDTH = ScaleUtils.scaleInt(600);

    /**
     * The horizontal margin around the main content pane.
     */
    private static final int HORIZONTAL_MARGIN = 40;

    /**
     * The top margin for the main content pane.
     */
    private static final int TOP_MARGIN = 0;

    /**
     * The bottom margin for the main content pane.
     */
    private static final int BOTTOM_MARGIN = 10;

    /**
     *  Text color config labels
     */
    protected static final Color TEXT_COLOR =
        ConfigurationUtils.useNativeTheme() ? Color.BLACK :
            new Color(DesktopUtilActivator.getResources().getColor(
                "service.gui.DARK_TEXT"));

    public OptionListConfigurationPanel()
    {
        super(new BorderLayout());

        setBorder(ScaleUtils.createEmptyBorder(
            TOP_MARGIN, HORIZONTAL_MARGIN, BOTTOM_MARGIN, HORIZONTAL_MARGIN));
    }

    /**
     * Creates a simple panel displaying a hint
     *
     * @param hintRes The text resource of the hint to display
     * @return the hint panel
     */
    protected static JTextArea createHintPanel(String hintRes)
    {
        JTextArea helpLabel = new JTextArea();
        ScaleUtils.scaleFontAsDefault(helpLabel);
        String text = DesktopUtilActivator.getResources().getI18NString(hintRes);

        helpLabel.setText(text);
        helpLabel.setOpaque(false);
        helpLabel.setEditable(false);
        helpLabel.setLineWrap(true);
        helpLabel.setWrapStyleWord(true);
        helpLabel.setFocusable(false);
        helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpLabel.setBorder(ScaleUtils.createEmptyBorder(3, 4, 0, 0));
        helpLabel.setForeground(HINT_TEXT_COLOR);

        int textWidth = helpLabel.getFontMetrics(helpLabel.getFont()).stringWidth(text);

        // Strings that are very similar lengths to MAXIMUM_WIDTH don't
        // wrap properly, so we calculate the number of rows using a
        // slightly smaller width than the maximum width.
        int rows = (int)Math.ceil(textWidth / (0.9 * (float)MAXIMUM_WIDTH));
        helpLabel.setPreferredSize(
            new Dimension(MAXIMUM_WIDTH,
                          rows * (int)helpLabel.getPreferredSize().getHeight()));

        return helpLabel;
    }
}
