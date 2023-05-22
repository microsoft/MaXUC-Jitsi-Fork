/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.presence.avatar.imagepicker;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Component allowing the user to easily crop an image
 *
 * @author Damien Roth
 * @author Damian Minkov
 */
public class ImageClipper
    extends JComponent
    implements MouseListener,
               MouseMotionListener
{
    private static final long serialVersionUID = 0L;

    /**
     * Border of the image.
     */
    private static final Color IMAGE_BORDER_COLOR
            = new Color(174, 189, 215);

    /**
     * Image overlay color.
     */
    private static final Color IMAGE_OVERLAY_COLOR
            = new Color(1.0f, 1.0f, 1.0f, 0.4f);

    /**
     * The image that we will crop.
     */
    private BufferedImage image = null;

    /**
     * The rectangle in which we are currently drawing the image.
     */
    private Rectangle imageRect = new Rectangle();

    /**
     * The zone that we will crop later from the image.
     */
    private Rectangle cropZoneRect;

    /**
     * Used for mouse dragging.
     * This is every time the initial X coordinate of the mouse
     * and the coordinates are according the image.
     */
    private int mouseStartX;

    /**
     * Used for mouse dragging.
     * This is every time the initial Y coordinate of the mouse
     * and the coordinates are according the image.
     */
    private int mouseStartY;

    /**
     * Construct an new image cropper
     *
     * @param cropZoneWidth the width of the crop zone
     * @param cropZoneHeight the height of the crop zone
     */
    public ImageClipper(int cropZoneWidth, int cropZoneHeight)
    {
        this.cropZoneRect = new Rectangle(cropZoneWidth, cropZoneHeight);
        updateComponents();

        Dimension d = new Dimension(320, 240);

        this.setSize(d);
        this.setMinimumSize(d);
        this.setPreferredSize(d);

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    /**
     * Update image and cropping zone positions.
     */
    private void updateComponents()
    {
        // Calculate the new cropping zone position.
        int cropZoneX = (this.getWidth() / 2) - (this.cropZoneRect.width / 2);
        int cropZoneY = (this.getHeight() / 2) - (this.cropZoneRect.height / 2);

        // Get the difference between the old and new cropping zone positions.
        int deltaX = cropZoneX - this.cropZoneRect.x;
        int deltaY = cropZoneY - this.cropZoneRect.y;

        // The cropping zone and the image must move together so they are both
        // always centralized.
        this.cropZoneRect.x += deltaX;
        this.cropZoneRect.y += deltaY;
        this.imageRect.x += deltaX;
        this.imageRect.y += deltaY;
    }

    /**
     * Defines the image to be cropped
     *
     * @param image the image to be cropped
     */
    public void setImage(BufferedImage image)
    {
        this.image = image;

        this.imageRect.width = image.getWidth(this);
        this.imageRect.height = image.getHeight(this);

        // put the image in the center
        this.imageRect.x = (this.getWidth() - this.imageRect.width)/2;
        this.imageRect.y = (this.getHeight() - this.imageRect.height)/2;

        // We call this because repaint() is asynchronous, and if it is called
        // multiple times it will just combine the calls into a single function
        // call when it actually executes. Painting is designed to be clever in
        // Swing, hence it's confusing when it goes wrong (see
        // https://www.oracle.com/java/technologies/painting.html for more details).
        // Repainting here triggers updating our components, which mean that the
        // dimensions and coordinates of the cropping rectangle and image are
        // updated. So to ensure the components are updated synchronously,
        // updateComponents is called as well as repaint (duplicated calls are fine).
        updateComponents();

        this.repaint();
    }

    /**
     * Returns the cropped area of the image
     *
     * @return the cropped area
     */
    public Rectangle getCroppedArea()
    {
        Rectangle croppedArea = new Rectangle();

        croppedArea.setSize(this.cropZoneRect.getSize());
        croppedArea.x = this.cropZoneRect.x - this.imageRect.x;
        croppedArea.y = this.cropZoneRect.y - this.imageRect.y;

        return croppedArea;
    }

    /**
     * Paint the component with the image we have and the settings
     * we have for it.
     * @param g the graphics to draw.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        AntialiasingManager.activateAntialiasing(g);

        updateComponents();

        // Draw image
        g.drawImage(this.image, this.imageRect.x, this.imageRect.y,
                this.imageRect.width, this.imageRect.height, this);

        // Select rect
        g.setColor(Color.BLACK);
        g.drawRect(this.cropZoneRect.x, this.cropZoneRect.y,
                this.cropZoneRect.width, this.cropZoneRect.height);

        // Image overlay
        drawImageOverlay(g);

        // Image border
        g.setColor(IMAGE_BORDER_COLOR);
        g.drawRoundRect(this.imageRect.x-2, this.imageRect.y-2,
                this.imageRect.width+3, this.imageRect.height+3, 2, 2);
        g.drawRoundRect(this.imageRect.x-1, this.imageRect.y-1,
                this.imageRect.width+1, this.imageRect.height+1, 2, 2);
    }

    /**
     * Draw an overlay over the parts of the images
     * which are not in the crop zone
     *
     * @param g the Graphics used to draw
     */
    private void drawImageOverlay(Graphics g)
    {
        int width, height;

        g.setColor(IMAGE_OVERLAY_COLOR);

        // left vertical non cropped part
        width = this.cropZoneRect.x - this.imageRect.x;
        if (width > 0)
        {
            g.fillRect(this.imageRect.x, this.imageRect.y,
                    width, this.imageRect.height);
        }

        // right vertical non cropped
        width = this.imageRect.x + this.imageRect.width
                - (this.cropZoneRect.x + this.cropZoneRect.width);
        if (width > 0)
        {
            g.fillRect(
                this.cropZoneRect.x + this.cropZoneRect.width,
                this.imageRect.y,
                width,
                this.imageRect.height);
        }

        // Top horizontal non croppped part
        height = this.cropZoneRect.y - this.imageRect.y;
        if (height > 0)
        {
            g.fillRect(this.cropZoneRect.x, this.imageRect.y,
                    this.cropZoneRect.width, height);
        }

        // Bottom horizontal non croppped part
        height = (this.imageRect.y + this.imageRect.height)
            - (this.cropZoneRect.y + this.cropZoneRect.height);
        if (height > 0)
        {
            g.fillRect(
                this.cropZoneRect.x,
                this.cropZoneRect.y + this.cropZoneRect.height,
                this.cropZoneRect.width,
                height);
        }
    }

    /**
     * Start image cropping action.
     * @param e the mouse event, initial clicking.
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
        // Init the dragging
        mouseStartX = e.getX();
        mouseStartY = e.getY();
    }

    /**
     * Event that user is dragging the mouse.
     * @param e the mouse event.
     */
    @Override
    public void mouseDragged(MouseEvent e)
    {
        // New position of the image
        int newXpos = this.imageRect.x + e.getX() - mouseStartX;
        int newYpos = this.imageRect.y + e.getY() - mouseStartY;

        if(newXpos <= cropZoneRect.x
           && newXpos + imageRect.width
                >= cropZoneRect.x + cropZoneRect.width)
        {
            this.imageRect.x = newXpos;
            mouseStartX = e.getX();
        }

        if(newYpos < cropZoneRect.y
           && newYpos + imageRect.height
                >= cropZoneRect.y + cropZoneRect.height)
        {
            this.imageRect.y = newYpos;
            mouseStartY = e.getY();
        }

        this.repaint();
    }

    /**
     * Not used.
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {}

    /**
     * Not used.
     * @param e
     */
    @Override
    public void mouseEntered(MouseEvent e) {}

    /**
     * Not used.
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {}

    /**
     * Not used.
     * @param e
     */
    @Override
    public void mouseReleased(MouseEvent e) {}

    /**
     * Not used.
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {}
}
