/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.*;
import net.java.sip.communicator.service.imageloader.*;

/**
 * The SIPCommToolBar is a <tt>JToolBar</tt>, which has its own drag icon
 * and separator. The drag icon is shown in the beginning of the toolbar,
 * before any of the containing toolbar buttons. It allows to drag the toolbar
 * out of the container, where it is added and show it in a separate window.
 * The separator is a line that could be added between two buttons. This way
 * the developer could visually group buttons with similar functionality.
 *
 * @author Yana Stamcheva
 */
public class SIPCommToolBar
    extends JToolBar
{
    private static final long serialVersionUID = 0L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID =
        SIPCommToolBar.class.getName() +  "ToolBarSeparatorUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            SIPCommToolBarSeparatorUI.class.getName());
    }

    /**
     * Creates an instance of <tt>SIPCommToolBar</tt>.
     */
    public SIPCommToolBar() {
        this.add(Box.createRigidArea(new Dimension(4, 4)));
    }

    /**
     * Adds a separator to this toolbar. The separator is added after
     * the last component in the toolbar.
     */
    public void addSeparator()
    {
        JToolBar.Separator s = new JToolBar.Separator(new Dimension(8, 22));

        if (getOrientation() == VERTICAL) {
            s.setOrientation(JSeparator.HORIZONTAL);
        } else {
            s.setOrientation(JSeparator.VERTICAL);
        }

        add(s);
    }

    /**
     * Overrides the <code>paintBorder</code> method of <tt>JToolBar</tt>
     * to paint the drag icon in the beginning of the toolbar.
     */
    protected void paintBorder(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        BufferedImage dragImage = GuiActivator.getImageLoaderService()
                .getImage(ImageLoaderService.TOOLBAR_DRAG_ICON).resolve();

        g2.drawImage(dragImage, 0, (this.getHeight() - dragImage
                .getHeight(null)) / 2 - 2, null);
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID()
    {
        return uiClassID;
    }
}
