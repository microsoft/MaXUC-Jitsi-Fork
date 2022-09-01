// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

import javax.swing.*;

import org.jitsi.util.*;

import net.java.sip.communicator.util.ConfigurationUtils;

/**
 * Provides a default layout for all options panels.
 */
public class ConfigSectionPanel extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Height used for all non-heading lines.
     */
    private static final int ITEM_LINE_HEIGHT = ScaleUtils.scaleInt(25);

    /**
     * Height used for all heading lines.
     */
    private static final int HEADING_LINE_HEIGHT = ScaleUtils.scaleInt(30);

    /**
     * The color used for the section title and separator. Text color on a mac
     * must always be dark as the background color is not brandable.
     */
    private static final Color SECTION_TITLE_COLOR =
        ConfigurationUtils.useNativeTheme() ?
            new Color(DesktopUtilActivator.getResources().getColor(
                "plugin.desktoputil.SECTION_TITLE_MAC")) :
            new Color(DesktopUtilActivator.getResources().getColor(
                "service.gui.MID_TEXT"));

    /**
     * Creates a config section that may or may not start with a separator.
     *
     * @param withSeparator Whether or not to add a separator.
     */
    public ConfigSectionPanel(boolean withSeparator)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (withSeparator)
        {
            addSeparator(null);
        }
    }

    /**
     * Creates a config section that starts with a separator. The config
     * section may or may not have a title. If the given label is null,
     * the separator takes the full width of the line.
     *
     * @param titleRes The text resource of the title to display
     */
    public ConfigSectionPanel(String titleRes)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        addSeparator(titleRes);
    }

    /**
     * Adds a separator with or without a title. If the given label is null,
     * the separator takes the full width of the line.
     *
     * @param titleRes The text resource of the title to display
     */
    private void addSeparator(String titleRes)
    {
        JPanel pnlSectionName = new TransparentPanel();
        pnlSectionName.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // If we've got a section name, add it here
        if (!StringUtils.isNullOrEmpty(titleRes))
        {
            String title = DesktopUtilActivator.getResources().getI18NString(titleRes);
            c.gridx = c.gridy = 0;
            c.anchor = GridBagConstraints.LINE_START;
            c.gridwidth = 2;
            JLabel label = new JLabel(title);
            label.setBorder(ScaleUtils.createEmptyBorder(0, 0, 0, 10));
            label.setFont(label.getFont().deriveFont(Font.BOLD, ScaleUtils.getDefaultFontSize()));
            label.setForeground(SECTION_TITLE_COLOR);
            pnlSectionName.add(label, c);
        }

        // Now add the separator
        c.gridx = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        JSeparator sep = new JSeparator();
        sep.setForeground(SECTION_TITLE_COLOR);
        sep.setBackground(new Color(0, 0, 0, 0));
        pnlSectionName.add(sep, c);

        resizeAndAdd(pnlSectionName, HEADING_LINE_HEIGHT);
    }

    /**
     * Overrides the add method to make sure all components
     * are added with the correct line height.
     *
     * @param comp the component to be added
     * @return the component argument
     */
    @Override
    public Component add(Component comp)
    {
        return resizeAndAdd(comp, ITEM_LINE_HEIGHT);
    }

    /**
     * Adds a panel without any resizing. Components that are bigger than
     * ITEM_LINE_HEIGHT should be added with this method.
     *
     * @param comp the component to be added
     * @return the component argument
     */
    public Component addPanel(Component comp)
    {
        if (comp instanceof JComponent)
        {
            JComponent jcomp = (JComponent)comp;
            jcomp.setAlignmentX(LEFT_ALIGNMENT);
        }
        return super.add(comp);
    }

    /**
     * Resizes the component to a fixed height and adds it to the panel.
     *
     * @param comp The component to be added.
     * @param height The line height.
     * @return The component argument.
     */
    private Component resizeAndAdd(Component comp, int height)
    {
        if (comp instanceof JComponent)
        {
            Dimension preferredSize = comp.getPreferredSize();
            if (preferredSize != null && preferredSize.height > height)
            {
                height = preferredSize.height;
            }

            JComponent jcomp = (JComponent)comp;
            jcomp.setAlignmentX(LEFT_ALIGNMENT);
            jcomp.setPreferredSize(new Dimension(comp.getWidth(), height));
            jcomp.setMinimumSize(new Dimension(comp.getWidth(), height));
            jcomp.setMaximumSize(
                new Dimension(OptionListConfigurationPanel.MAXIMUM_WIDTH, height));
        }
        return super.add(comp);
    }
}

