// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.plugin.desktoputil.ResMenuItem;
import net.java.sip.communicator.plugin.desktoputil.SIPCommPopupMenu;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.util.skin.Skinnable;

/**
 * The dialog which is shown on the <tt>TransferCallButton</tt> click and
 * provides the user an option between attended and
 * unattended call transfer actions.
 */
public class TransferCallMenu extends SIPCommPopupMenu implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The invoker component.
     */
    private final JComponent invoker;

    /**
     * Creates an instance of the <tt>TransferCallMenu</tt>.
     *
     * @param invoker     the invoker component
     * @param initialPeer the initial peer we're aiming to transfer
     */
    public TransferCallMenu(JComponent invoker, CallPeer initialPeer)
    {
        this.invoker = invoker;
        this.init();

        JMenuItem unattendedTransferMenuItem = new ResMenuItem("service.gui.UNATTENDED_TRANSFER_MENU_ITEM");
        JMenuItem attendedTransferMenuItem = new ResMenuItem("service.gui.ATTENDED_TRANSFER_MENU_ITEM");

        unattendedTransferMenuItem.addActionListener(event -> CallManager
                .openCallTransferDialog(initialPeer, CallTransferType.UNATTENDED));
        attendedTransferMenuItem.addActionListener(event -> CallManager
                .openCallTransferDialog(initialPeer, CallTransferType.ATTENDED));

        this.add(unattendedTransferMenuItem);
        this.add(attendedTransferMenuItem);
    }

    /**
     * Initializes and add some common components.
     */
    private void init()
    {
        setInvoker(invoker);

        this.add(createInfoLabel());
        this.addSeparator();
        this.setFocusable(true);
    }

    /**
     * Shows this popup menu relative to its invoker location.
     */
    public void showPopupMenu()
    {
        Point location = new Point(invoker.getX(),
                                   invoker.getY() + invoker.getHeight());

        SwingUtilities.convertPointToScreen(location, invoker.getParent());
        setLocation(location);
        setVisible(true);
    }

    /**
     * Creates the info label.
     *
     * @return the created info label
     */
    private Component createInfoLabel()
    {
        JLabel infoLabel = new JLabel();
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD, ScaleUtils.getDefaultFontSize()));
        infoLabel.setText(GuiActivator.getResources().getI18NString("service.gui.TRANSFER_CALL_MENU_TITLE"));

        return infoLabel;
    }

    /**
     * Reloads all menu items.
     */
    @Override
    public void loadSkin()
    {
        Component[] components = getComponents();
        for (Component component : components)
        {
            if (component instanceof Skinnable)
            {
                Skinnable skinnableComponent = (Skinnable) component;
                skinnableComponent.loadSkin();
            }
        }
    }
}
