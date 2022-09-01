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
import javax.swing.event.*;

import org.jitsi.service.resources.*;

/**
 * A custom component, used to show images in a frame. A rollover for the
 * content image and optional menu in dialog.
 *
 * @author Damien Roth
 */
public class FramedImageWithMenu
    extends FramedImage
    implements MouseListener, PopupMenuListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The dialog containing the menu with actions.
     */
    private JPopupMenu popupMenu;

    /**
     * The parent frame.
     */
    private JFrame mainFrame;

    /**
     * Should we currently draw overlay.
     */
    private boolean drawOverlay = false;

    /**
     * Are we showing custom image or the default one.
     */
    private boolean isDefaultImage = true;

    /**
     * The current image.
     */
    private Image currentImage;

    /**
     * Creates the component.
     * @param mainFrame the parent frame.
     * @param imageIcon the image icon to show as default one.
     * @param width width of component.
     * @param height height of component.
     */
    public FramedImageWithMenu(
        JFrame mainFrame,
        ImageIcon imageIcon,
        int width,
        int height)
    {
        super(imageIcon, width, height);

        this.mainFrame = mainFrame;
        this.addMouseListener(this);
    }

    /**
     * Sets the dialog used for menu for this Image.
     * @param popupMenu the dialog to show as menu. Can be null if no menu
     *        will be available.
     */
    public void setPopupMenu(JPopupMenu popupMenu)
    {
        this.popupMenu = popupMenu;
        if(popupMenu != null)
            this.popupMenu.addPopupMenuListener(this);
    }

    /**
     * Sets the image to display in the frame.
     *
     * @param imageIcon the image to display in the frame
     */
    public void setImageIcon(ImageIconFuture imageIcon)
    {
        setImageIcon(imageIcon.resolve());
    }

    /**
     * Sets the image to display in the frame.
     *
     * @param imageIcon the image to display in the frame
     */
    public void setImageIcon(ImageIcon imageIcon)
    {
        // Intercept the action to validate the user icon and not the default
        Image image = imageIcon == null ? null : imageIcon.getImage();

        super.setImageIcon(image);
        this.isDefaultImage = false;

        this.currentImage = image;
    }

    /**
     * Returns the current image with no rounded corners. Only return the user
     * image and not the default image.
     *
     * @return the current image - null if it's the default image
     */
    public Image getAvatar()
    {
        return (!this.isDefaultImage) ? this.currentImage : this.getImage();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (drawOverlay)
        {
            g = g.create();
            AntialiasingManager.activateAntialiasing(g);

            try
            {
                // Draw black highlight and white shading on rollover
                g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                g.drawOval(1, 1, width - 3, height - 3);

                g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f/2));
                g.fillOval(2, 2, width - 4, height - 4);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Show the avatar dialog as a glasspane of the mainframe
     *
     * @param show show dialogs if sets to TRUE - hide otherwise
     */
    private void showDialog(MouseEvent e, boolean show)
    {
        if (this.popupMenu == null)
        {
            return;
        }

        if (show)
        {
            Point imageLoc = this.getLocationOnScreen();
            Point rootPaneLoc = mainFrame.getRootPane().getLocationOnScreen();

            this.popupMenu.setSize(mainFrame.getRootPane().getWidth(),
                    this.popupMenu.getHeight());

            this.popupMenu.show(this, (rootPaneLoc.x - imageLoc.x),
                    this.getHeight());
        }
        else
        {
            this.drawOverlay = false;
            this.repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        if (this.drawOverlay)
            return;

        this.drawOverlay = true;
        this.repaint();
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        // Remove overlay only if the dialog isn't visible
        if (!popupMenu.isVisible())
        {
            this.drawOverlay = false;
            this.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        showDialog(e, !popupMenu.isVisible());
    }

    /**
     * This method is called before the popup menu becomes visible
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

    /**
     * This method is called before the popup menu becomes invisible
     * Note that a JPopupMenu can become invisible any time
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
        this.drawOverlay = false;
        this.repaint();
    }

    /**
     * This method is called when the popup menu is canceled
     */
    public void popupMenuCanceled(PopupMenuEvent e){}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}
}
