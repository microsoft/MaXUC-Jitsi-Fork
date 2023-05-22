/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.util.*;

/**
 * The tooltip shown over a contact in the contact list.
 *
 * @author Yana Stamcheva
 */
public class ExtendedTooltip
    extends JToolTip
{
    private static final long serialVersionUID = 0L;

    /**
     * The largest width that we allow the tool tip to have
     */
    private static final int MAX_TEXT_WIDTH = 400;

    private static final Logger logger
        = Logger.getLogger(ExtendedTooltip.class);

    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID =
        ExtendedTooltip.class.getName() +  "ToolTipUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            ImageToolTipUI.class.getName());
    }

    private final JLabel imageLabel = new JLabel();

    private final JLabel titleLabel = new JLabel();

    private final JPanel linesPanel = new JPanel();

    private final JTextArea bottomTextArea = new JTextArea();

    private int textWidth = 0;

    private int textHeight = 0;

    private boolean isListViewEnabled;

    /**
     * Created a <tt>MetaContactTooltip</tt>.
     * @param isListViewEnabled indicates if the list view is enabled
     */
    public ExtendedTooltip(boolean isListViewEnabled)
    {
        this.isListViewEnabled = isListViewEnabled;

        this.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        JPanel centerPanel = new JPanel(new BorderLayout());

        mainPanel.setOpaque(false);
        mainPanel.setBackground(null);
        centerPanel.setOpaque(false);
        centerPanel.setBackground(null);
        linesPanel.setOpaque(false);
        linesPanel.setBackground(null);
        bottomTextArea.setOpaque(false);
        bottomTextArea.setBackground(null);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        if (isListViewEnabled)
        {
            linesPanel.setLayout(
                new BoxLayout(linesPanel, BoxLayout.Y_AXIS));

            mainPanel.add(imageLabel, BorderLayout.WEST);
            mainPanel.add(centerPanel, BorderLayout.CENTER);

            centerPanel.add(titleLabel, BorderLayout.NORTH);
            centerPanel.add(linesPanel, BorderLayout.CENTER);
        }
        else
        {
            titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            mainPanel.add(imageLabel, BorderLayout.CENTER);
            mainPanel.add(titleLabel, BorderLayout.NORTH);
        }

        bottomTextArea.setEditable(false);
        bottomTextArea.setLineWrap(true);
        bottomTextArea.setWrapStyleWord(true);
        bottomTextArea.setFont(bottomTextArea.getFont().
            deriveFont(ScaleUtils.getScaledFontSize(10f)));
        mainPanel.add(bottomTextArea, BorderLayout.SOUTH);

        final Window parentWindow
            = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getActiveWindow();

        // Hide the tooltip when the parent window hides.
        /*
         * FIXME The parentWindow will surely outlive this ExtendedTooltip so
         * adding a WindowFocusListener without removing the same
         * WindowFocusListener later on is guaranteed to cause a memory leak.
         */
        if (parentWindow != null)
            parentWindow.addWindowFocusListener(new WindowFocusListener()
            {
                public void windowLostFocus(WindowEvent e)
                {
                    Window popupWindow
                        = SwingUtilities.getWindowAncestor(
                            ExtendedTooltip.this);

                    if ((popupWindow != null)
                        && popupWindow.isVisible()
                        // The popup window should normally be a JWindow, so we
                        // check here explicitly if for some reason we didn't
                        // get something else.
                        && (popupWindow instanceof JWindow))
                    {
                        logger.info("Tooltip window ancestor to hide: "
                                + popupWindow);

                        popupWindow.setVisible(false);
                    }
                }

                public void windowGainedFocus(WindowEvent e) {}
            });

        this.add(mainPanel);
    }

    /**
     * Sets the given image to this tooltip.
     *
     * @param imageIcon The image icon to set.
     */
    public void setImage(ImageIcon imageIcon)
    {
        imageLabel.setIcon(imageIcon);
    }

    /**
     * Sets the title of the tooltip. The text would be shown in bold on the top
     * of the tooltip panel.
     *
     * @param titleText The title of the tooltip.
     */
    public void setTitle(String titleText)
    {
        if (titleText == null)
            return;

        titleLabel.setText(titleText);

        Dimension labelSize
            = ComponentUtils.getStringSize(titleLabel,titleText);
        recalculateTooltipSize(labelSize.width, labelSize.height);
    }

    /**
     * Adds an icon-string list, which would appear on the right of the image
     * panel.
     *
     * @param icon the icon to show
     * @param text the name to show
     */
    public void addLine(Icon icon,
                        String text)
    {
        JLabel lineLabel = new JLabel(  text,
                                        icon,
                                        JLabel.LEFT);

        linesPanel.add(lineLabel);

        Dimension labelSize = calculateLabelSize(lineLabel);

        recalculateTooltipSize(labelSize.width, labelSize.height);
    }

    /**
     * Adds an icon-string list, which would appear on the right of the image
     * panel.
     *
     * @param icon the icon to show
     * @param text the name to show
     * @param leftIndent left indent of the label
     */
    public void addSubLine(Icon icon,
                        String text,
                        int leftIndent)
    {
        JLabel lineLabel = new JLabel(  text,
                                        icon,
                                        JLabel.LEFT);

        lineLabel.setBorder(
            BorderFactory.createEmptyBorder(0, leftIndent, 0, 0));
        lineLabel.setFont(lineLabel.getFont().
            deriveFont(ScaleUtils.getScaledFontSize(9f)));
        lineLabel.setForeground(Color.DARK_GRAY);

        linesPanel.add(lineLabel);

        Dimension labelSize = calculateLabelSize(lineLabel);

        recalculateTooltipSize(labelSize.width + leftIndent, labelSize.height);
    }

    /**
     * Adds the given array of labels as one line in this tool tip.
     *
     * @param labels the labels to add
     */
    public void addLine(JLabel[] labels)
    {
        Dimension lineSize = null;
        JPanel labelPanel = null;

        if (labels.length > 0)
        {
            labelPanel = new TransparentPanel(
                new FlowLayout(FlowLayout.LEFT, 2, 0));
            linesPanel.add(labelPanel);
        }
        else
            return;

        if (labelPanel != null)
            for (JLabel label : labels)
            {
                labelPanel.add(label);
                if (lineSize == null)
                    lineSize = calculateLabelSize(label);
                else
                    lineSize = new Dimension(
                        lineSize.width + calculateLabelSize(label).width,
                        lineSize.height);
            }

        recalculateTooltipSize(lineSize.width, lineSize.height);
    }

    /**
     * Clear all lines.
     */
    public void removeAllLines()
    {
        linesPanel.removeAll();
    }

    /**
     * Sets the text that would appear on the bottom of the tooltip.
     * @param text the text to set
     */
    public void setBottomText(String text)
    {
        this.bottomTextArea.setText(text);
    }

    /**
     * Calculates label size.
     *
     * @param label the label, which size we should calculate
     * @return the Dimension indicating the label size
     */
    private Dimension calculateLabelSize(JLabel label)
    {
        Icon icon = label.getIcon();
        String text = label.getText();

        int iconWidth = 0;
        int iconHeight = 0;
        if (icon != null)
        {
            iconWidth = icon.getIconWidth();
            iconHeight = icon.getIconHeight();
        }

        int labelWidth
            = ComponentUtils.getStringWidth(label, text)
                + iconWidth
                + label.getIconTextGap();

        int textHeight = ComponentUtils.getStringSize(label, text).height;

        int labelHeight = (iconHeight > textHeight) ? iconHeight : textHeight;

        return new Dimension(labelWidth, labelHeight);
    }

    /**
     * Re-calculates the tooltip size.
     *
     * @param newTextWidth the width of the newly added text that should be
     * added to the global width
     * @param newTextHeight the height of the newly added text that should be
     * added to the global height
     */
    private void recalculateTooltipSize(int newTextWidth, int newTextHeight)
    {
        if (textWidth < newTextWidth)
            textWidth = newTextWidth;

        textHeight += newTextHeight;

        if (textWidth > MAX_TEXT_WIDTH)
            textWidth = MAX_TEXT_WIDTH;
    }

    /**
     * Customized UI for this MetaContactTooltip.
     */
    public static class ImageToolTipUI extends MetalToolTipUI
    {
        static ImageToolTipUI sharedInstance = new ImageToolTipUI();

        /**
         * Creates the UI.
         * @param c
         * @return
         */
        public static ComponentUI createUI(JComponent c)
        {
            return sharedInstance;
        }

        /**
         * Overwrite the UI paint method to do nothing in order fix double
         * painting of the tooltip text.
         * @param g the <tt>Graphics</tt> object
         * @param c the component used to render the tooltip
         */
        @Override
        public void paint(Graphics g, JComponent c)
        {}

        /**
         * Override ComponentUI update method to set visibility of bottomText.
         * @param g <tt>Graphics</tt> object
         * @param c the component used to render the tooltip
         */
        @Override
        public void update(Graphics g, JComponent c)
        {
            JTextArea bottomTextArea =
                ((ExtendedTooltip)c).bottomTextArea;

            String bottomText = bottomTextArea.getText();
            if(bottomText == null || bottomText.length() <= 0)
                bottomTextArea.setVisible(false);
            else
                bottomTextArea.setVisible(true);
            super.update(g, c);
        }

        /**
         * Returns the size of the given component.
         * @param c the component used to render the tooltip
         * @return the size of the given component.
         */
        @Override
        public Dimension getPreferredSize(JComponent c)
        {
            ExtendedTooltip tooltip = (ExtendedTooltip)c;

            Icon icon = tooltip.imageLabel.getIcon();
            int width = 0;
            if (icon != null)
                width += icon.getIconWidth();

            if (tooltip.isListViewEnabled)
                width += tooltip.textWidth + 15;
            else
                width = tooltip.textWidth > width ? tooltip.textWidth : width;

            int imageHeight = 0;
            if (icon != null)
                imageHeight = icon.getIconHeight();

            int height = 0;
            if (tooltip.isListViewEnabled)
            {
                height = imageHeight > tooltip.textHeight
                    ? imageHeight : tooltip.textHeight;
            }
            else
                height = imageHeight + tooltip.textHeight;

            String bottomText = tooltip.bottomTextArea.getText();
            if(bottomText != null && bottomText.length() > 0)
            {
                // Seems a little messy, but sets the proper size.
                tooltip.bottomTextArea.setColumns(5);
                tooltip.bottomTextArea.setSize(0,0);
                tooltip.bottomTextArea.setSize(
                    tooltip.bottomTextArea.getPreferredSize());

                height += tooltip.bottomTextArea.getPreferredSize().height;
            }

            return new Dimension(width, height);
        }
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID()
    {
        return uiClassID;
    }
}
