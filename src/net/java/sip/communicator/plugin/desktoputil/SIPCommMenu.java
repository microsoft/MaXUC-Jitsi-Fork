/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * The <tt>SIPCommMenu</tt> is very similar to a JComboBox. The main
 * component here is a JLabel only with an icon. When user clicks on the icon a
 * popup menu is opened, containing a list of icon-text pairs from which the
 * user could choose one item. When user selects the desired item, the icon of
 * the selected item is set to the main component label.
 *
 * @author Yana Stamcheva
 */
public class SIPCommMenu
    extends JMenu
{
    private static final long serialVersionUID = 1L;
    private Object selectedObject;

    /**
     * Creates an instance of <tt>SIPCommMenu</tt>.
     */
    public SIPCommMenu()
    {
        super();

        init();
    }

    /**
     * Creates an instance of <tt>SIPCommMenu</tt> by specifying
     * the text and the icon.
     * @param text the text of the menu
     * @param defaultIcon the menu icon
     */
    public SIPCommMenu(String text, Icon defaultIcon)
    {
        super(text);

        this.setIcon(defaultIcon);
        init();
    }

    /**
     * Creates an instance of <tt>SIPCommMenu</tt> by specifying the
     * initialy selected item.
     *
     * @param text The item that is initialy selected.
     */
    public SIPCommMenu(String text)
    {
        super(text);
        init();
    }

    private void init()
    {
        MouseRolloverHandler mouseHandler = new MouseRolloverHandler();

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
        ScaleUtils.scaleFontAsDefault(this);

        // Hides the popup menu when the parent window loses focus.
        getPopupMenu().addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent evt)
            {
                Window parentWindow;

                Component parent = SIPCommMenu.this.getParent();

                // If this is a submenu get the invoker first.
                if (parent instanceof JPopupMenu)
                    parentWindow = SwingUtilities.getWindowAncestor(
                        ((JPopupMenu) parent).getInvoker());
                else
                    parentWindow
                        = SwingUtilities.getWindowAncestor(SIPCommMenu.this);

                if (!parentWindow.isActive())
                {
                    getPopupMenu().setVisible(false);
                }

                parentWindow.addWindowListener(new WindowAdapter()
                {
                    public void windowDeactivated(WindowEvent e)
                    {
                        JPopupMenu popupMenu = getPopupMenu();

                        if (popupMenu != null && popupMenu.isVisible())
                            popupMenu.setVisible(false);
                    }
                });
            }
        });
    }

    /**
     * Adds an item to the "choice list" of this selector box.
     *
     * @param text The text of the item.
     * @param icon The icon of the item.
     * @param actionListener The <tt>ActionListener</tt>, which handles the
     * case, when the item is selected.
     */
    public void addItem(String text, Icon icon, ActionListener actionListener)
    {
        JMenuItem item = new JMenuItem(text, icon);

        item.addActionListener(actionListener);

        this.add(item);
    }

    /**
     * Selects the given item.
     *
     * @param selectedObject The object to select.
     */
    public void setSelected(SelectedObject selectedObject)
    {
        if (selectedObject.getIcon() != null)
            selectedObject.getIcon().addToButton(this);

        if (selectedObject.getText() != null)
            this.setText(selectedObject.getText());

        if (selectedObject.getObject() != null)
            this.setSelectedObject(selectedObject.getObject());
    }

    /**
     * Selects the given object.
     *
     * @param o The <tt>Object</tt> to select.
     */
    public void setSelectedObject(Object o)
    {
        this.selectedObject = o;
    }

    /**
     * Returns the selected object.
     *
     * @return the selected object.
     */
    public Object getSelectedObject()
    {
        return this.selectedObject;
    }

    /**
     * Sets the isMouseOver property value and repaints this component.
     *
     * @param isMouseOver <code>true</code> to indicate that the mouse is over
     * this component, <code>false</code> - otherwise.
     */
    public void setMouseOver(boolean isMouseOver)
    {
        this.repaint();
    }

    /**
     * Paints this component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        Graphics g2 = g.create();
        try
        {
            internalPaintComponent(g2);
        }
        finally
        {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    /**
     * Paints a rollover effect when the mouse is over this menu.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    private void internalPaintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        float visibility = getModel().isRollover() ? 0.5f : 0.0f;

        g.setColor(new Color(1.0f, 1.0f, 1.0f, visibility));

        g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);

        g.setColor(UIManager.getColor("Menu.foreground"));
    }

    /**
     * Perform a rollover on mouse over.
     */
    private class MouseRolloverHandler
        implements  MouseListener,
                    MouseMotionListener
    {
        @Override
        public void mouseMoved(MouseEvent e)
        {
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (isEnabled())
            {
                getModel().setRollover(false);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            if (isEnabled())
            {
                getModel().setRollover(true);
            }
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
        }
    }
}
