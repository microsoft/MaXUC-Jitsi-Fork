/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>ConfigFormListCellRenderer</tt> is the custom cell renderer used in
 * the Jitsi's <tt>ConfigFormList</tt>. It extends TransparentPanel
 * instead of JLabel, which allows adding different buttons and icons to the
 * cell.
 * <br>
 * The cell border and background are repainted.
 *
 * @author Yana Stamcheva
 */
public class ConfigFormListCellRenderer
    extends TransparentPanel
    implements ListCellRenderer<ConfigFormDescriptor>
{
    private static final long serialVersionUID = 0L;

    /**
     * The size of the gradient used for painting the selected background of
     * some components.
     */
    public static final int SELECTED_GRADIENT_SIZE = 5;

    private final JLabel textLabel = new EmphasizedLabel("");

    private final JLabel iconLabel = new JLabel();

    private boolean isSelected = false;

    /**
     * Initialize the panel containing the node.
     */
    public ConfigFormListCellRenderer()
    {
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 5, 3));
        this.setLayout(new BorderLayout(0, 0));
        this.setPreferredSize(new ScaledDimension(66, 56));

        Font font = getFont();
        this.textLabel.setFont(font.deriveFont(
            ScaleUtils.getScaledFontSize(11f)));

        this.iconLabel.setHorizontalAlignment(JLabel.CENTER);
        this.textLabel.setHorizontalAlignment(JLabel.CENTER);

        this.add(iconLabel, BorderLayout.CENTER);
        this.add(textLabel, BorderLayout.SOUTH);
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     *
     * Returns this panel that has been configured to display the meta contact
     * and meta contact group cells.
     */
    @Override
    public Component getListCellRendererComponent(JList<? extends ConfigFormDescriptor> list,
            ConfigFormDescriptor cfDescriptor,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        ImageIconFuture icon = cfDescriptor.getConfigFormIcon();
        if(icon != null)
            icon.addToLabel(iconLabel);

        String title = cfDescriptor.getConfigFormTitle();
        if (title != null)
            textLabel.setText(title);

        this.isSelected = isSelected;

        if (isSelected)
        {
            AccessibilityUtils.setName(list, title);
        }

        return this;
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JPanel</tt>
     * to provide a custom look for this panel. A gradient background is
     * painted when the panel is selected and when the mouse is over it.
     * @param g the <tt>Graphics</tt> object
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        if (isSelected)
        {
            g2.setPaint(new Color(100, 100, 100, 100));
            g2.fillRoundRect(   0, 0,
                               this.getWidth(), this.getHeight(),
                               10, 10);

            g2.setColor(Color.GRAY);
            g2.drawRoundRect(   0, 0,
                               this.getWidth() - 1, this.getHeight() - 1,
                               10, 10);
        }
    }
}
