// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;

import net.java.sip.communicator.plugin.desktoputil.SIPCommPopupMenu;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.Skinnable;

/**
 * A base implementation of TransferCallMenu which provides logic required for
 * handling keyboard navigation.
 */
public abstract class AbstractTransferCallMenu extends SIPCommPopupMenu implements Skinnable, MenuKeyListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>AbstractTransferCallMenu</tt> class and its
     * instances for logging output.
     */
    protected static final Logger logger = Logger.getLogger(AbstractTransferCallMenu.class);

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        // Add/remove this listener, so we can handle key inputs for menu events
        if (visible) {
            logger.info("Key menu listener added");
            addMenuKeyListener(this);
        }
        else
        {
            logger.info("Key menu listener removed");
            removeMenuKeyListener(this);
        }
    }

    /**
     * Triggered when a key is pressed on a menu. Adding key listeners to the menu items doesn't work properly, so we're
     * adding a listener to the menu and figuring out which item was clicked on.
     */
    @Override
    public void menuKeyPressed(MenuKeyEvent e)
    {
        int keyCode = e.getKeyCode();
        if (keyCode != KeyEvent.VK_SPACE && keyCode != KeyEvent.VK_ENTER)
        {
            return;
        }
        for (MenuElement element : getSubElements())
        {
            if (element instanceof JMenuItem item && item.isArmed())
            {
                callMenuItemPressed(item);
            }
        }
    }

    @Override
    public void menuKeyReleased(MenuKeyEvent e)
    {
        // Do nothing
    }

    @Override
    public void menuKeyTyped(MenuKeyEvent e)
    {
        // Do nothing
    }

    /**
     * On TransferActiveCallsMenu and TransferCallMenu to implement their own logic.
     */
    protected abstract void callMenuItemPressed(JMenuItem item);
}
