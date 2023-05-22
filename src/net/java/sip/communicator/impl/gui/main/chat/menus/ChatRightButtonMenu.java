/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChatRightButtonMenu</tt> appears when the user makes a right button
 * click on the chat window conversation area (where sent and received messages
 * are displayed).
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ChatRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                Skinnable
{
    private static final long serialVersionUID = 0L;

    private ChatConversationPanel chatConvPanel;

    private JMenuItem copyMenuItem =
        GuiActivator.getImageLoaderService()
        .getImage(ImageLoaderService.COPY_ICON)
        .getImageIcon()
        .addToMenuItem(new ResMenuItem("service.gui.COPY"));

    private JMenuItem closeMenuItem =
        GuiActivator.getImageLoaderService()
        .getImage(ImageLoaderService.CLOSE_ICON)
        .getImageIcon()
        .addToMenuItem(new ResMenuItem("service.gui.CLOSE"));

    /**
     * Creates an instance of <tt>ChatRightButtonMenu</tt>.
     *
     * @param chatConvPanel The conversation panel, where this menu will apear.
     */
    public ChatRightButtonMenu(ChatConversationPanel chatConvPanel)
    {
        super();

        this.chatConvPanel = chatConvPanel;

        this.init();
    }

    /**
     * Initializes the menu with all menu items.
     */
    private void init()
    {
        this.add(copyMenuItem);

        this.addSeparator();

        this.add(closeMenuItem);

        this.copyMenuItem.setName("copy");
        this.closeMenuItem.setName("service.gui.CLOSE");

        if (ConfigurationUtils.isMenuIconsDisabled())
        {
            this.copyMenuItem.setIcon(null);
            this.closeMenuItem.setIcon(null);
        }

        this.copyMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.copyMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_C,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * Disables the copy item.
     */
    public void disableCopy() {
        this.copyMenuItem.setEnabled(false);
    }

    /**
     * Enables the copy item.
     */
    public void enableCopy() {
        this.copyMenuItem.setEnabled(true);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("copy"))
        {
            this.chatConvPanel.copyConversation();
        }
        else if (itemText.equalsIgnoreCase("save"))
        {
            //TODO: Implement save to file.
        }
        else if (itemText.equalsIgnoreCase("print"))
        {
          //TODO: Implement print.
        }
        else if (itemText.equalsIgnoreCase("service.gui.CLOSE"))
        {
            Window window = this.chatConvPanel
                .getChatContainer().getConversationContainerWindow();

            window.setVisible(false);
            window.dispose();
        }
    }

    /**
     * Reloads menu icons.
     */
    public void loadSkin()
    {
        if (ConfigurationUtils.isMenuIconsDisabled())
        {
            return;
        }

        GuiActivator.getImageLoaderService()
        .getImage(ImageLoaderService.COPY_ICON)
        .getImageIcon()
        .addToMenuItem(copyMenuItem);

        GuiActivator.getImageLoaderService()
        .getImage(ImageLoaderService.CLOSE_ICON)
        .getImageIcon()
        .addToMenuItem(closeMenuItem);
    }
}
