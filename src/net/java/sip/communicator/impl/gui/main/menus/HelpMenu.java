/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.event.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>HelpMenu</tt> is a menu in the main application menu bar.
 *
 * @author Yana Stamcheva
 * @author Thomas Hofer
 * @author Lyubomir Marinov
 */
public class HelpMenu
    extends SIPCommMenu
    implements ActionListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>PluginContainer</tt> which implements the logic related to
     * dealing with <tt>PluginComponent</tt>s on behalf of this
     * <tt>HelpMenu</tt>.
     */
    private final PluginContainer pluginContainer;

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     *
     * @param mainFrame the parent window
     */
    public HelpMenu(AbstractMainFrame mainFrame)
    {
        ResourceManagementService resources = GuiActivator.getResources();

        setMnemonic(resources.getI18nMnemonic("service.gui.HELP"));
        setText(resources.getI18NString("service.gui.HELP"));

        pluginContainer
            = new PluginContainer(this, Container.CONTAINER_HELP_MENU);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is
     * selected.
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
    }
}
