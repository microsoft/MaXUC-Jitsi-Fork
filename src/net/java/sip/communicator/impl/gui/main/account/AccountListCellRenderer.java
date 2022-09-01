/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>AccountListCellRenderer</tt> is the cell renderer used from the
 * {@link AccountList}.
 *
 * @author Yana Stamcheva
 */
public class AccountListCellRenderer
    extends TransparentPanel
    implements ListCellRenderer<Account>
{
    private static final long serialVersionUID = 0L;

    /**
     * The background color of the odd rows.
     */
    private static final Color rowColor
        = new Color(GuiActivator.getResources()
                .getColor("service.gui.LIGHT_BACKGROUND"));

    /**
     * The label used to show account name and icon.
     */
    private final JLabel accountLabel = new JLabel();

    /**
     * The label used to show status name and icon.
     */
    private final JLabel statusLabel = new JLabel();

    /**
     * Indicates if the current row is selected.
     */
    private boolean isSelected = false;

    /**
     * The current account value.
     */
    private Account account;

    /**
     * The current index.
     */
    private int index;

    /**
     * Constraints used to layout components in this panel.
     */
    private final GridBagConstraints constraints = new GridBagConstraints();

    /**
     * Creates an instance of this cell renderer.
     */
    public AccountListCellRenderer()
    {
        super(new GridBagLayout());

        this.setPreferredSize(new Dimension(100, 38));
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        accountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountLabel.setFont(accountLabel.getFont().deriveFont(Font.BOLD));

        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1f;
        add(accountLabel, constraints);

        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 0f;
        add(statusLabel, constraints);
    }

    /**
     * Returns the renderer component for the cell given by all the parameters.
     * @param list the parent list
     * @param value the value of the cell
     * @param index the index of the cell
     * @param isSelected indicates if the cell is selected
     * @param cellHasFocus indicates if the cell has the focus
     * @return the component rendering the cell
     */
    @Override
    public Component getListCellRendererComponent(JList<? extends Account> list, Account value,
        int index, boolean isSelected, boolean cellHasFocus)
    {
        this.account = value;

        ImageIconFuture accountIcon = account.getIcon();

        if (accountIcon != null)
            accountIcon.addToLabel(accountLabel);

        accountLabel.setText(account.getName());

        ImageIconFuture statusIcon = account.getStatusIcon();

        if (statusIcon != null)
        {
            statusIcon.addToLabel(statusLabel);
        }

        String statusName = account.getStatusName();

        if (statusName != null)
            statusLabel.setText(statusName);

        setEnabled(list.isEnabled());
        setFont(list.getFont());

        if (!account.isEnabled())
        {
            accountLabel.setForeground(Color.GRAY);
            statusLabel.setForeground(Color.GRAY);
        }
        else
        {
            accountLabel.setForeground(Color.BLACK);
            statusLabel.setForeground(Color.BLACK);
        }

        this.index = index;
        this.isSelected = isSelected;

        this.setBounds(0, 0, list.getWidth(), getPreferredSize().height);

        if (isSelected)
        {
            AccessibilityUtils.setNameAndDescription(list,
                account.getProtocolName(),
                account.getName());
        }

        return this;
    }

    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected.
     * @param g the <tt>Graphics</tt> object
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        try
        {
            internalPaintComponent(g);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Paint a background for all groups and a round blue border and background
     * when a cell is selected.
     * @param g the <tt>Graphics</tt> object
     */
    private void internalPaintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        if (index%2 > 0)
        {
            g2.setColor(rowColor);
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        }

        if (this.isSelected)
        {
            g2.setColor(Constants.SELECTED_COLOR);
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
    }
}
