/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.updatewindows;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.ResMenuItem;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>PluginComponent</tt> for the "Check for Updates" menu
 * item.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class CheckForUpdatesMenuItemComponent
    extends AbstractPluginComponent
{
    /**
     * The <tt>Logger</tt> used by the <tt>CheckForUpdatesMenuItemComponent</tt>
     * class for logging output.
     */
    private static final Logger logger =
        Logger.getLogger(CheckForUpdatesMenuItemComponent.class);

    /**
     * The "Check for Updates" menu item.
     */
    private JMenuItem checkForUpdatesMenuItem;

    /**
     * Initializes a new "Check for Updates" menu item.
     *
     * @param container the container of the update menu component
     */
    public CheckForUpdatesMenuItemComponent(Container container)
    {
        super(container);
    }

    /**
     * Gets the UI <tt>Component</tt> of this <tt>PluginComponent</tt>.
     *
     * @return the UI <tt>Component</tt> of this <tt>PluginComponent</tt>
     * @see PluginComponent#getComponent()
     */
    public JMenuItem getComponent()
    {
        if(checkForUpdatesMenuItem == null)
        {
            checkForUpdatesMenuItem
                = new ResMenuItem("plugin.updatechecker.UPDATE_MENU_ENTRY");
            checkForUpdatesMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    logger.user("Check for updates clicked in the help menu");
                    UpdateWindowsActivator.getUpdateService().checkForUpdates(true);
                }
            });
        }

        return checkForUpdatesMenuItem;
    }

    /**
     * Gets the name of this <tt>PluginComponent</tt>.
     *
     * @return the name of this <tt>PluginComponent</tt>
     * @see PluginComponent#getName()
     */
    public String getName()
    {
        return
            Resources.getResources().getI18NString(
                    "plugin.updatechecker.UPDATE_MENU_ENTRY");
    }
}
