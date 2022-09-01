/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>WritePanelRightButtonMenu</tt> appears when the user makes a right
 * button click on the chat window write area (where user types messages).
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class WritePanelRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                Skinnable
{
    private static final long serialVersionUID = 0L;

    private ChatContainer mChatContainer;

    private JMenuItem mCutMenuItem = new ResMenuItem("service.gui.CUT");
    private JMenuItem mCopyMenuItem = new ResMenuItem("service.gui.COPY");
    private JMenuItem mPasteMenuItem = new ResMenuItem("service.gui.PASTE");
    private JMenuItem mCloseMenuItem = new ResMenuItem("service.gui.CLOSE");

    /**
     * Creates an instance of <tt>WritePanelRightButtonMenu</tt>.
     *
     * @param chatContainer The window owner of this popup menu.
     */
    public WritePanelRightButtonMenu(ChatContainer chatContainer)
    {
        super();

        mChatContainer = chatContainer;

        init();
    }

    /**
     * Initializes this menu with menu items.
     */
    private void init()
    {
        add(mCutMenuItem);
        add(mCopyMenuItem);
        add(mPasteMenuItem);

        addSeparator();

        add(mCloseMenuItem);

        mCopyMenuItem.setName("copy");
        mCutMenuItem.setName("cut");
        mPasteMenuItem.setName("paste");
        mCloseMenuItem.setName("service.gui.CLOSE");

        mCopyMenuItem.addActionListener(this);
        mCutMenuItem.addActionListener(this);
        mPasteMenuItem.addActionListener(this);
        mCloseMenuItem.addActionListener(this);

        mCutMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_X,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        mCopyMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_C,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        mPasteMenuItem.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_V,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        loadSkin();
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

        if (itemText.equalsIgnoreCase("cut"))
        {
            mChatContainer.getCurrentChat().cut();
        }
        else if (itemText.equalsIgnoreCase("copy"))
        {
            mChatContainer.getCurrentChat().copyWriteArea();
        }
        else if (itemText.equalsIgnoreCase("paste"))
        {
            mChatContainer.getCurrentChat().paste();
        }
        else if (itemText.equalsIgnoreCase("service.gui.CLOSE"))
        {
            mChatContainer.getFrame().setVisible(false);
            mChatContainer.getFrame().dispose();
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

        ImageLoaderService imageLoaderService =
            GuiActivator.getImageLoaderService();

        imageLoaderService.getImage(ImageLoaderService.CUT_ICON)
        .getImageIcon()
        .addToMenuItem(mCutMenuItem);

        imageLoaderService.getImage(ImageLoaderService.COPY_ICON)
        .getImageIcon()
        .addToMenuItem(mCopyMenuItem);

        imageLoaderService.getImage(ImageLoaderService.PASTE_ICON)
        .getImageIcon()
        .addToMenuItem(mPasteMenuItem);

        imageLoaderService.getImage(ImageLoaderService.CLOSE_ICON)
        .getImageIcon()
        .addToMenuItem(mCloseMenuItem);
    }

    /**
     * Provides a popup menu with custom entries followed by default
     * operation entries (copy, paste, close)
     *
     * @param entries custom menu entries to be added
     * @return right click menu
     */
    public JPopupMenu makeMenu(List <JMenuItem> entries)
    {
        JPopupMenu rightMenu = new JPopupMenu();

        for (JMenuItem entry : entries)
        {
            rightMenu.add(entry);
        }

        if (!entries.isEmpty()) rightMenu.addSeparator();

        rightMenu.add(mCopyMenuItem);
        rightMenu.add(mCutMenuItem);
        rightMenu.add(mPasteMenuItem);

        rightMenu.addSeparator();

        rightMenu.add(mCloseMenuItem);

        return rightMenu;
    }
}
