/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.replacement.emoji.EmojiActivator;
import net.java.sip.communicator.plugin.desktoputil.SIPCommMenu;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.ScaledDimension;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.SIPCommLabelUI;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.replacement.InsertableIcon;
import net.java.sip.communicator.service.replacement.TabOfInsertableIcons;
import net.java.sip.communicator.service.replacement.TabbedImagesSelectorService;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.Skinnable;
import org.jitsi.util.OSUtils;

/**
 * The <tt>IconSelectorBox</tt> is the component where user could choose an
 * icon to send.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class IconSelectorBox
    extends SIPCommMenu
    implements MouseListener,
               MouseWheelListener,
               PopupMenuListener,
               Skinnable
{
    private static final Logger sLog
    = Logger.getLogger(IconSelectorBox.class);

    private static final long serialVersionUID = 0L;

    /**
     * The chat write panel.
     */
    private ChatPanel chatPanel;

    /**
     * The icon insertion service.
     */
    private final TabbedImagesSelectorService iconSelectorService;

    /**
     * PopupMenu
     */
    private JPopupMenu popupMenu;

    /**
     * Whether or not the icon selection box is already initialised with the
     * icons
     */
    private boolean isPopupMenuPopulated = false;

    /**
     * Initializes a new <tt>IconSelectorBox</tt> instance.
     */
    public IconSelectorBox()
    {
        this.setOpaque(false);

        // Should explicitly remove any border in order to align correctly the
        // icon.
        this.setBorder(BorderFactory.createEmptyBorder());

        popupMenu = this.getPopupMenu();

        popupMenu.setLayout(new GridBagLayout());

        /*
         * Load the icons and the UI which represents them on demand because
         * they are not always necessary.
         */
        popupMenu.addPopupMenuListener(this);

        // Add a mouse wheel listener so that we don't incorrectly close the
        // pop up menu during scrolling.
        popupMenu.addMouseWheelListener(this);

        this.iconSelectorService = GuiActivator.getSmiliesReplacementSource();

        loadSkin();
    }

    public List<DisplayablePartOfMessage> replaceText(TextPartOfMessage stringMessagePart)
    {
        return  iconSelectorService.replaceText(stringMessagePart);
    }

    /**
     * Sets the chat panel, for which icons would be created.
     *
     * @param chatPanel the chat panel, for which icons would be created
     */
    public void setChat(ChatPanel chatPanel)
    {
        this.chatPanel = chatPanel;
    }

    /**
     * Opens the icons selector box.
     */
    public void open()
    {
        this.doClick();
    }

    /**
     * Returns TRUE if the selector box is opened, otherwise returns FALSE.
     *
     * @return TRUE if the selector box is opened, otherwise returns FALSE
     */
    public boolean isMenuSelected()
    {
        return isPopupMenuVisible();
    }

    /**
     * A custom menu item, which paints round border over selection.
     */
    private static class IconMenuItem
        extends JLabel
    {
        private static final long serialVersionUID = 0L;

        /**
         * Class id key used in UIDefaults.
         */
        private static final String uiClassID =
            IconMenuItem.class.getName() +  "TreeUI";

        /**
         * The insertable icon of this iconMenuItem, containing the filepath
         * and text to add.
         */
        public final InsertableIcon insertableIcon;

       /**
        * The colour of the menu item - used so we can highlight the currently
        * 'hovered over' item.
        */
        private final Color color;

        /**
         * Adds the ui class to UIDefaults.
         */
        static
        {
            UIManager.getDefaults().put(uiClassID,
                SIPCommLabelUI.class.getName());
        }

        /**
         * Initializes a new <tt>iconMenuItem</tt> instance which is to depict
         * a specific <tt>icon</tt>.
         *
         * @param icon representing the icon to be depicted
         */
        public IconMenuItem(InsertableIcon icon)
        {
            super();
            icon.getImageIcon().addToLabel(this);
            setPreferredSize(new Dimension(35, 35));
            setVerticalAlignment(CENTER);
            setHorizontalAlignment(CENTER);
            color = getBackground();
            this.insertableIcon = icon;
        }

        /**
         * Returns the name of the L&F class that renders this component.
         * @return the string "TreeUI"
         * @see JComponent#getUIClassID
         * @see UIDefaults#getUI
         */
        public String getUIClassID()
        {
            return uiClassID;
        }
    }

    /**
     * Highlights the cell to make it clear which has the mouse over it
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseEntered(MouseEvent e)
    {
        IconMenuItem iconMenuItem = (IconMenuItem) e.getSource();
        iconMenuItem.setBackground(iconMenuItem.color.darker());
        iconMenuItem.setOpaque(true);
    }

    /**
     * Reset the cell to its normal background colour
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseExited(MouseEvent e)
    {
        IconMenuItem iconMenuItem = (IconMenuItem) e.getSource();
        iconMenuItem.setBackground(iconMenuItem.color);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        IconMenuItem iconMenuItem = (IconMenuItem) e.getSource();
        ChatWritePanel chatWritePanel = chatPanel.getChatWritePanel();

        // Add a new ImageIcon here, to stop caching (which would cause multiple
        // of the same icons inserted consecutively to be depicted with a single
        // icon).
        chatWritePanel.appendTextAsImage(iconMenuItem.insertableIcon.getTextToAdd(),
                                         EmojiActivator.getResources().getImageFromPath(
                                                                 iconMenuItem.insertableIcon.getFilepath()).resolve());
        chatWritePanel.getEditorPane().requestFocus();
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    /**
     * Implements PopupMenuListener#popupMenuCanceled(PopupMenuEvent). Does
     * nothing.
     * @param e the <tt>PopupMenuEvent</tt>
     */
    public void popupMenuCanceled(PopupMenuEvent e) {}

    /**
     * Implements
     * PopupMenuListener#popupMenuWillBecomeInvisible(PopupMenuEvent). Does
     * nothing.
     * @param e the <tt>PopupMenuEvent</tt>
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

    /**
     * Implements PopupMenuListener#popupMenuWillBecomeVisible(PopupMenuEvent).
     * Creates the UI to represent the icon selector box when it is
     * first necessary
     * @param e the <tt>PopupMenuEvent</tt> that notified us
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
    {
        // Don't populate it again if it's already populated.
        if (isPopupMenuPopulated)
        {
            return;
        }

        // If all emoji icons aren't yet loaded, wait for 5 seconds while this
        // loading happens. Ideally, the loading will finish sometime in this 5
        // seconds (loading takes approximately 10 seconds from program
        // start-up), in which case the loaded menu will automatically display.
        // Otherwise, nothing displays while the loading continues, and the
        // user can try clicking on the button to display the pop up menu
        // again.
        int waitingCounter = 0;
        try
        {
            if (!iconSelectorService.allIconsLoaded())
            {
                setCursor(Cursor.getDefaultCursor());
            }

            while (!iconSelectorService.allIconsLoaded())
            {
                // Waits for 200 x 50ms = 10 seconds before abandoning
                if (waitingCounter > 100)
                {
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }

                setCursor(Cursor.getPredefinedCursor((Cursor.WAIT_CURSOR)));
                TimeUnit.MILLISECONDS.sleep(50);
                waitingCounter++;
            }
        }
        catch (InterruptedException e1)
        {
            sLog.error("Error while waiting for icons to load", e1);
        }

        setCursor(Cursor.getDefaultCursor());
        JPopupMenu popupMenu = (JPopupMenu) e.getSource();
        JTabbedPane tabbedPane = new JTabbedPane();
        List<TabOfInsertableIcons> iconTabs = iconSelectorService.getTabs();

        // Creates each icon tab and adds it to the popup menu
        int tabCounter = 0;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        for (TabOfInsertableIcons iconTab : iconTabs)
        {
            List<InsertableIcon> iconList = iconTab.getIconList();

            JPanel iconPanel = new JPanel();
            iconPanel.setLayout(new GridBagLayout());

            // 8 is the maximum number of columns we can fit in based on the
            // number  and size of tabs and the size of the icons
            int gridColCount = 8;
            int iconCount = 0;

            // Adds the icons to the icons tab
            for (InsertableIcon icon : iconList)
            {
                IconMenuItem iconItem = new IconMenuItem(icon);
                iconItem.addMouseListener(this);

                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                gridBagConstraints.gridx = iconCount % gridColCount;
                gridBagConstraints.gridy = iconCount / gridColCount;
                gridBagConstraints.weightx = 1;
                gridBagConstraints.weighty = 1;
                // This value of padding is just chosen so that it scales without looking
                // too shabby - there is nothing exact about it. The important thing is
                // that at 100% scaling, padding is zero. Also, since we round, it won't
                // scale exactly, but it is good enough until DUIR.
                int padding = OSUtils.IS_WINDOWS ? Math.round((ScaleUtils.getScale() - 1) * 10) : 0;
                gridBagConstraints.ipadx = padding;
                gridBagConstraints.ipady = padding;
                gridBagConstraints.insets = new Insets(padding, 0, padding, 0);

                iconPanel.add(iconItem, gridBagConstraints);

                iconCount++;
            }

            JScrollPane scrollPane = new JScrollPane(iconPanel);
            int paneWidth = OSUtils.IS_WINDOWS ? ScaleUtils.scaleInt(300) : 300;
            int paneHeight = OSUtils.IS_WINDOWS ? ScaleUtils.scaleInt(240) : 240;
            scrollPane.setPreferredSize(new Dimension(paneWidth, paneHeight));
            scrollPane.getVerticalScrollBar().setUnitIncrement(12);
            scrollPane.addMouseWheelListener(new MouseWheelListener(){
                @Override
                public void mouseWheelMoved(MouseWheelEvent e)
                {
                    // We consume the mouse wheel event so that scrolling
                    // within in the scroll pane with a track pad does
                    // not cause the pop up menu to disappear.
                    e.consume();
                }
            });

            // Formats the tabs for the selector box
            JLabel tabLabel = new JLabel();
            tabLabel.setIcon(iconTab.getIconToDisplayOnTab());
            int tabWidth = OSUtils.IS_WINDOWS ? ScaleUtils.scaleInt(30) : 30;
            int tabHeight = OSUtils.IS_WINDOWS ? ScaleUtils.scaleInt(20) : 20;
            tabLabel.setPreferredSize(new Dimension(tabWidth, tabHeight));
            tabLabel.setHorizontalAlignment(JLabel.CENTER);

            // The picture at the top of each tab fits best as 18x18
            tabbedPane.addTab("",
                new ImageIcon(iconTab.getIconToDisplayOnTab().getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)),
                scrollPane);
            tabbedPane.setTabComponentAt(tabCounter, tabLabel);

            tabCounter++;
        }

        popupMenu.add(tabbedPane);

        // To get rid of the dotted border around the tab when a user selects it
        tabbedPane.setFocusable(false);

        isPopupMenuPopulated = true;
    }

    /**
     * Reloads icons in this menu.
     */
    public void loadSkin()
    {
        GuiActivator.getImageLoaderService()
        .getImage(ImageLoaderService.SMILIES_ICON)
        .getImageIcon().addToButton(this);

        this.setPreferredSize(new ScaledDimension(25, 25));
        this.setMinimumSize(new ScaledDimension(25, 25));
    }

    /**
     * Handle a scroll event to ensure we don't incorrectly hide the pop up menu.
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        // We consume the mouse wheel event so that scrolling anywhere outside
        // the scroll pane with either a mouse scroll wheel or track pad does
        // not cause the pop up menu to disappear.
        e.consume();
    }
}
