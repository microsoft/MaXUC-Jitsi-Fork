/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.managecontact.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The GroupRightButtonMenu is the menu, opened when user clicks with the right
 * mouse button on a group in the contact list. Through this menu the user could
 * add a contact to a group.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class GroupRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                PluginComponentListener,
                Skinnable
{
    private static final long serialVersionUID = 0L;
    private static final Logger sLog = Logger.getLogger(GroupRightButtonMenu.class);

    private final JMenuItem addContactItem
        = new ResMenuItem("service.gui.ADD_CONTACT");

    private final JMenuItem removeGroupItem
        = new ResMenuItem("service.gui.REMOVE_GROUP");

    private final MetaContactGroup group;

    private final AbstractMainFrame mainFrame;

    /**
     * Creates an instance of GroupRightButtonMenu.
     *
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     * @param group The <tt>MetaContactGroup</tt> for which the menu is opened.
     */
    public GroupRightButtonMenu(AbstractMainFrame mainFrame, MetaContactGroup group)
    {
        this.group = group;
        this.mainFrame = mainFrame;

        this.add(addContactItem);
        this.addSeparator();

        if (!ConfigurationUtils.isGroupRemoveDisabled())
        {
            this.add(removeGroupItem);
        }

        this.addContactItem.addActionListener(this);
        this.removeGroupItem.addActionListener(this);

        loadSkin();

        this.initPluginComponents();
    }

    /**
     * Initializes all plugin components.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference<?>[] serRefs = null;

        String osgiFilter =
            "(" + Container.CONTAINER_ID + "="
                + Container.CONTAINER_GROUP_RIGHT_BUTTON_MENU.getID() + ")";

        try
        {
            serRefs =
                GuiActivator.bundleContext.getServiceReferences(
                    PluginComponent.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            sLog.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i++)
            {
                PluginComponent component =
                    (PluginComponent) GuiActivator.bundleContext
                        .getService(serRefs[i]);

                component.setCurrentContactGroup(group);

                this.add((Component) component.getComponent());

                this.repaint();
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Handles the <tt>ActionEvent</tt>. The chosen menu item should correspond
     * to an account, where the new contact will be added. We obtain here the
     * protocol provider corresponding to the chosen account and show the
     * dialog, where the user could add the contact.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem item = (JMenuItem) e.getSource();

        if (item.equals(removeGroupItem))
        {
            if (group != null)
            {
                MetaContactListManager.removeMetaContactGroup(group);
            }
        }
        else if (item.equals(addContactItem))
        {
            ManageContactWindow addContactWindow =
                GuiActivator.getAddContactWindow(null);

            if (addContactWindow != null)
            {
                addContactWindow.setSelectedGroup(group);
                addContactWindow.setVisible(true);
            }
        }
    }

    /**
     * Indicates that a plugin component has been added to this container.
     *
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    /**
     * Indicates that a new plugin component has been added. Adds it to this
     * container if it belongs to it.
     *
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (!c.getContainer().equals(
            Container.CONTAINER_GROUP_RIGHT_BUTTON_MENU))
            return;

        this.add((Component) c.getComponent());

        c.setCurrentContactGroup(group);

        this.repaint();
    }

    /**
     * Indicates that a new plugin component has been removed. Removes it to
     * from this container if it belongs to it.
     *
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer()
            .equals(Container.CONTAINER_GROUP_RIGHT_BUTTON_MENU))
        {
            this.remove((Component) c.getComponent());
        }
    }

    /**
     * Reloads label icons.
     */
    public void loadSkin()
    {
        if (ConfigurationUtils.isMenuIconsDisabled())
        {
            return;
        }

        ImageLoaderService imageLoaderService = GuiActivator.getImageLoaderService();

        imageLoaderService
        .getImage(ImageLoaderService.ADD_CONTACT_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(addContactItem);

        imageLoaderService
        .getImage(ImageLoaderService.DELETE_16x16_ICON)
        .getImageIcon()
        .addToMenuItem(removeGroupItem);
    }
}
