/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.util.ConfigurationUtils;

import org.jitsi.service.resources.*;

/**
 * The main menu bar. This is the menu bar that appears on top of the main
 * window. It contains a file menu, tools menu, view menu and help menu.
 * <p>
 * Note that this container allows also specifying a custom background by
 * modifying the menuBackground.png in the resources/images/impl/gui/common
 * folder.
 * </p>
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class MainMenu
    extends SIPCommWindowMenuBar
{
    private static final long serialVersionUID = 0L;

    /**
     * Initializes a new <tt>MainMenu</tt> instance.
     */
    public MainMenu(AbstractMainFrame mainFrame)
    {
        super("service.gui.DARK_BACKGROUND");

        addMenu(new FileMenu(mainFrame), "service.gui.FILE");

        ConferenceService conferenceService = GuiActivator.getConferenceService();

        // Don't need the Meetings menu if this is a standalone meeting user
        // as the standalone meeting UI will offer all the options here.
        if(!ConfigurationUtils.isStandaloneMeetingUser() &&
            conferenceService != null && conferenceService.isFullServiceEnabled())
        {
            addMenu(new ConferencesMenu(), "service.gui.conf.CONFERENCES");
        }

        addMenu(new ToolsMenu(), "service.gui.TOOLS");
        addMenu(new HelpMenu(mainFrame), "service.gui.HELP");
    }

    private void addMenu(JMenu menu, String key)
    {
        ResourceManagementService resources = GuiActivator.getResources();

        menu.setText(resources.getI18NString(key));
        menu.setMnemonic(resources.getI18nMnemonic(key));
        ScaleUtils.scaleFontAsDefault(menu);

        add(menu);
    }

    /**
     * Determines whether there are selected menus.
     *
     * @return <tt>true</tt> if there are selected menus;otherwise,
     * <tt>false</tt>
     */
    public boolean hasSelectedMenus()
    {
        for (int i = 0, menuCount = getMenuCount(); i < menuCount; i++)
            if (getMenu(i).isSelected())
                return true;
        return false;
    }
}
