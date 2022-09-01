/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * @author Yana Stamcheva
 */
public class ExtListCellRenderer<T>
    extends JPanel
    implements ListCellRenderer<T>
{
    private static final long serialVersionUID = 1L;
    private final JLabel label = new JLabel();
    private boolean isSelected;

    public ExtListCellRenderer()
    {
        super(new BorderLayout());

        this.add(label);
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     */
    @Override
    public Component getListCellRendererComponent(JList<? extends T> list, T value,
            int index, boolean isSelect, boolean cellHasFocus)
    {
        this.label.setText(value.toString());
        this.isSelected = isSelect;

        return this;
    }

    /**
     * Paint a round background for all selected cells.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g);

            if (this.isSelected)
            {
                Graphics2D g2 = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();

                g2.setColor(Constants.SELECTED_COLOR);
                g2.fillRoundRect(0, 0, width, height, 7, 7);

                g2.setColor(Constants.LIST_SELECTION_BORDER_COLOR);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, width - 1, height - 1, 7, 7);
            }
        }
        finally
        {
            g.dispose();
        }
    }
}
