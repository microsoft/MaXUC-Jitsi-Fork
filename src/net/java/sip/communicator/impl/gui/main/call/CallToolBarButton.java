/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * The <tt>CallBarButton</tt> is a button shown in the call window tool bar.
 *
 * @author Yana Stamcheva
 */
public class CallToolBarButton
    extends SIPCommButton
{
    private static final long serialVersionUID = 0L;

    /**
     * The default width of a button in the call tool bar.
     */
    public static final int DEFAULT_WIDTH = 44;

    /**
     * The default height of a button in the call tool bar.
     */
    public static final int DEFAULT_HEIGHT = 38;

    private static final ConfigurationService confService =
                                         GuiActivator.getConfigurationService();

    /**
     * The string used to label this button if it is being shown from within
     * a PopupMenu as a JMenuItem.
     */
    private String menuName;

    /**
     * The property that should be checked to determine if we should be placed
     * in the MoreActionsButton, or independently in the CallPanel.
     */
    private String showInMoreActionsDropdownProp;

    /**
     * We track the associated menu item, if there is one, so we can pass on
     * UI commands directed at the button to it, if required.
     */
    private JMenuItem menuItem;

    /**
     * Creates an instance of <tt>CallToolBarButton</tt>.
     *
     * @param menuName The label that should be used if showing this button as
     * a JMenuItem in the 'more actions' menu.
     * @param showInMoreActionsDropdownProp The config property to check whether
     * we should show this toolbar button in the more actions dropdown instead
     * of as a standalone button.
     */
    public CallToolBarButton(String menuName,
                             String showInMoreActionsDropdownProp)
    {
        this(null, null, menuName, showInMoreActionsDropdownProp);
    }

    /**
     * Creates an instance of <tt>CallToolBarButton</tt> by specifying the icon
     * image and the tool tip text.
     *
     * @param iconImage the icon of this button
     * @param tooltipText the text to show in the button tooltip
     * @param menuName The label that should be used if showing this button as
     * a JMenuItem in the 'more actions' menu.
     * @param showInMoreActionsDropdownProp The config property to check whether
     * we should show this toolbar button in the more actions dropdown instead
     * of as a standalone button.
     */
    public CallToolBarButton(   BufferedImageFuture iconImage,
                                String tooltipText,
                                String menuName,
                                String showInMoreActionsDropdownProp)
    {
        this(iconImage, null, tooltipText, menuName,
                                                 showInMoreActionsDropdownProp);
    }

    /**
     * Creates an instance of <tt>CallToolBarButton</tt> by specifying the icon
     * image, the name of the button and the tool tip text.
     *
     * @param iconImage the icon of this button
     * @param buttonName the name of this button
     * @param tooltipText the text to show in the button tooltip
     * @param menuName The label that should be used if showing this button as
     * a JMenuItem in the 'more actions' menu.
     * @param showInMoreActionsDropdownProp The config property to check whether
     * we should show this toolbar button in the more actions dropdown instead
     * of as a standalone button.
     */
    public CallToolBarButton(   BufferedImageFuture iconImage,
                                String buttonName,
                                String tooltipText,
                                String menuName,
                                String showInMoreActionsDropdownProp)
    {
        super(null, iconImage);
        this.menuName = menuName;
        this.showInMoreActionsDropdownProp = showInMoreActionsDropdownProp;

        setIconImage(iconImage);

        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setName(buttonName);
        setToolTipText(tooltipText);
    }

    /**
     * Determines if this <tt>CallToolBarButton</tt> should be displayed as a
     * menu item inside the <tt>MoreActionsButton</tt> instead of as a
     * standalone button.
     *
     * @return <tt>true</tt> if the button should be contained within
     * 'more actions'; <tt>false</tt> if it should be displayed on its own.
     */
    public boolean showInMoreActionsMenu()
    {
        return confService.user().getBoolean(showInMoreActionsDropdownProp, false);
    }

    /**
     * Returns a menu item to be shown in the MoreActionsButton's popup menu.
     * Clicking the menu item has the same effect as if the button itself were
     * clicked.
     *
     * @return The menu item corresponding to this button.
     */
    public JMenuItem getAsMenuItem()
    {
        if (menuItem == null)
        {
            menuItem = new JMenuItem(menuName);
            menuItem.setEnabled(this.isEnabled());

            // When the menu item is clicked, we should take the same action as
            // if the button were clicked.
            menuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent evt)
                {
                    CallToolBarButton.this.fireActionPerformed(evt);
                }
            });
        }

        return menuItem;
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);

        if (menuItem != null)
            menuItem.setEnabled(b);
    }

    @Override
    public void setVisible(boolean b)
    {
        super.setVisible(b);

        if (menuItem != null)
            menuItem.setVisible(b);
    }

    @Override
    public void setText(String text)
    {
        // Call toolbar buttons never have text, so if setText is called it
        // must be intended to refer to a JMenuItem.
        if (menuItem != null)
            menuItem.setText(text);
    }
}
