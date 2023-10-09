/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>SIPCommButton</tt> is a very flexible <tt>JButton</tt> that allows
 * to configure its background, its icon, the look when a mouse is over it, etc.
 * It can also allow for drawing of a text label on top.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommButton
    extends JButton
    implements OrderedComponent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private static final ResourceManagementService resources =
                                            DesktopUtilActivator.getResources();

    private BufferedImageFuture bgImage;

    private BufferedImageFuture pressedBgImage;

    private BufferedImageFuture rolloverBgImage;

    private BufferedImageFuture rolloverIconImage;

    private BufferedImageFuture pressedIconImage;

    private BufferedImageFuture iconImage;

    /**
     * The (optional) image to use when this button is disabled
     */
    private BufferedImageFuture disabledImage;

    /**
     * The text to display as a label on the button
     */
    private String text;

    /**
     * The index of the button, used when we want to order our buttons.
     */
    private int index = -1;

    /**
     * Creates a button.
     */
    public SIPCommButton()
    {
        this((BufferedImageFuture) null);
    }

    /**
     * Creates a button with images accessed via a resource with the given
     * prefix.
     *
     * @param imageResPrefix the prefix of the resource for the images for this
     * button.
     */
    public SIPCommButton(String imageResPrefix)
    {
        this(null,
            loadImage(imageResPrefix),
            loadImage(imageResPrefix + "_ROLLOVER"),
            loadImage(imageResPrefix + "_PRESSED"));

        setDisabledImage(loadImage(imageResPrefix + "_DISABLED"));
    }

    /**
     * Creates a button with custom background image and icon image.
     *
     * @param bgImage       The background image.
     * @param rolloverImage The rollover background image.
     * @param pressedImage  The pressed image.
     * @param iconImage     The icon.
     * @param rolloverIconImage The rollover icon image.
     * @param pressedIconImage The pressed icon image.
     */
    public SIPCommButton(   BufferedImageFuture bgImage,
                            BufferedImageFuture rolloverImage,
                            BufferedImageFuture pressedImage,
                            BufferedImageFuture iconImage,
                            BufferedImageFuture rolloverIconImage,
                            BufferedImageFuture pressedIconImage)
    {
        MouseRolloverHandler mouseHandler = new MouseRolloverHandler();

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

        /*
         * Explicitly remove all borders that may be set from the current look
         * and feel.
         */
        this.setContentAreaFilled(false);
        this.setBorder(null);

        this.bgImage = bgImage;
        this.rolloverBgImage = rolloverImage;
        this.pressedBgImage = pressedImage;
        this.rolloverIconImage = rolloverIconImage;
        this.pressedIconImage = pressedIconImage;
        this.iconImage = iconImage;

        if (bgImage != null)
        {
            Image image = bgImage.resolve();
            this.setPreferredSize(new Dimension(image.getWidth(null),
                                                image.getHeight(null)));

            bgImage.getImageIcon().addToButton(this);
        }
    }

    /**
     * Creates a button with custom background image and icon image. Also a
     * custom text label
     *
     * @param text          The text label
     * @param bgImage       The background image.
     * @param rolloverImage The rollover background image.
     * @param pressedImage  The pressed image.
     */
    public SIPCommButton(   String text,
                            BufferedImageFuture bgImage,
                            BufferedImageFuture rolloverImage,
                            BufferedImageFuture pressedImage)
    {
        this(    bgImage,
                 rolloverImage,
                 pressedImage,
                 null,
                 null,
                 null);

        this.text = text;
    }

    /**
     * Creates a button with custom background image and icon image.
     *
     * @param bgImage       The background image.
     * @param pressedImage  The pressed image.
     * @param iconImage     The icon.
     */
    public SIPCommButton(   BufferedImageFuture bgImage,
                            BufferedImageFuture pressedImage,
                            BufferedImageFuture iconImage)
    {
        this(bgImage, null, pressedImage, iconImage, null, null);
    }

    /**
     * Creates a button with custom background image.
     *
     * @param bgImage the background button image
     * @param iconImage the icon of this button
     */
    public SIPCommButton(   BufferedImageFuture bgImage,
                            BufferedImageFuture iconImage)
    {
        this(bgImage, null, iconImage);
    }

    /**
     * Creates a button with custom background image.
     *
     * @param bgImage The background button image.
     */
    public SIPCommButton(BufferedImageFuture bgImage)
    {
        this(bgImage, null);
    }

    /**
     * Resets the background image for this button.
     *
     * @param bgImage the new image to set.
     */
    public void setImage(BufferedImageFuture bgImage)
    {
        this.bgImage = bgImage;

        this.repaint();
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JButton</tt> to
     * paint the button background and icon, and all additional effects of this
     * configurable button.
     *
     * @param g The Graphics object.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        g = g.create();
        try
        {
            internalPaintComponent(g);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Paints this button.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    private void internalPaintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);
        /*
         * As JComponent#paintComponent says, if you do not invoke super's
         * implementation you must honor the opaque property, that is if this
         * component is opaque, you must completely fill in the background in a
         * non-opaque color. If you do not honor the opaque property you will
         * likely see visual artifacts.
         */
        if (isOpaque())
        {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        // Paint pressed state.
        Image paintBgImage = null;
        if (this.getModel().isPressed() && this.pressedBgImage != null)
        {
            paintBgImage = pressedBgImage.resolve();
        }
        else if (this.getModel().isRollover() && this.rolloverBgImage != null)
        {
            paintBgImage = rolloverBgImage.resolve();
        }
        else if (this.bgImage != null)
        {
            // If there's no icon, we make grey the backgroundImage
            // when disabled.
            if (this.disabledImage != null && !isEnabled())
            {
                paintBgImage = disabledImage.resolve();
            }
            else if (this.iconImage == null && !isEnabled())
            {
                paintBgImage = LightGrayFilter.createDisabledImage(bgImage.resolve());
            }
            else
                paintBgImage = bgImage.resolve();
        }

        if (paintBgImage != null)
        {
            g.drawImage(paintBgImage,
                        this.getWidth()/2 - paintBgImage.getWidth(null)/2,
                        this.getHeight()/2 - paintBgImage.getHeight(null)/2,
                        this);
        }

        if (rolloverBgImage == null)
        {
            float visibility = this.getModel().isRollover() ? 0.5f : 0.0f;

            g.setColor(new Color(1.0f, 1.0f, 1.0f, visibility));

            if (this.bgImage == null
                && (isContentAreaFilled() || (visibility != 0.0f)))
            {
                g.fillRoundRect(
                    0, 0, this.getWidth(), this.getHeight(), 8, 8);
            }
        }

        Image paintIconImage = null;
        if (getModel().isPressed() && pressedIconImage != null)
        {
            paintIconImage = pressedIconImage.resolve();
        }
        else if (this.getModel().isRollover() && rolloverIconImage != null)
        {
            paintIconImage = rolloverIconImage.resolve();
        }
        else if (this.iconImage != null)
        {
            if (this.disabledImage != null && !isEnabled())
            {
                paintBgImage = disabledImage.resolve();
            }
            else if (!isEnabled())
            {
                paintIconImage = LightGrayFilter.createDisabledImage(iconImage.resolve());
            }
            else
            {
                paintIconImage = iconImage.resolve();
            }
        }

        if (paintIconImage != null)
            g.drawImage(paintIconImage,
                this.getWidth()/2 - paintIconImage.getWidth(null)/2,
                this.getHeight()/2 - paintIconImage.getHeight(null)/2,
                this);

        if (text != null)
        {
            Dimension stringSize = ComponentUtils.getStringSize(this, text);
            g.setColor(getForeground());
            int x = (this.getSize().width/2 - stringSize.width/2);
            int y = (this.getSize().height/2 + stringSize.height/4);
            g.drawString(text, x, y);
        }
    }

    /**
     * Sets the background image of this button.
     *
     * @param bgImage the background image of this button.
     */
    public void setBackgroundImage(BufferedImageFuture bgImage)
    {
        this.bgImage = bgImage;

        if (bgImage != null)
        {
            Image image = bgImage.resolve();
            this.setPreferredSize(new Dimension(image.getWidth(null),
                                                image.getHeight(null)));

            bgImage.getImageIcon().addToButton(this);
        }
    }

    /**
     * Sets the rollover background image of this button.
     *
     * @param rolloverImage the rollover background image of this button.
     */
    public void setRolloverImage(BufferedImageFuture rolloverImage)
    {
        this.rolloverBgImage = rolloverImage;
    }

    /**
     * Sets the pressed background image of this button.
     *
     * @param pressedImage the pressed background image of this button.
     */
    public void setPressedImage(BufferedImageFuture pressedImage)
    {
        this.pressedBgImage = pressedImage;
    }

    /**
     * Sets the rollover icon image of this button.
     *
     * @param rolloverIconImage the rollover icon image of this button.
     */
    public void setRolloverIcon(BufferedImageFuture rolloverIconImage)
    {
        this.rolloverIconImage = rolloverIconImage;
    }

    /**
     * Sets the pressed icon image of this button.
     *
     * @param pressedIconImage the pressed icon image of this button.
     */
    public void setPressedIcon(BufferedImageFuture pressedIconImage)
    {
        this.pressedIconImage = pressedIconImage;
    }

    /**
     * Sets the icon image of this button.
     *
     * @param iconImage the icon image of this button.
     */
    public void setIconImage(BufferedImageFuture iconImage)
    {
        this.iconImage = iconImage;
    }

    /**
     * Sets all images for this button.
     *
     * @param imageResPrefix the prefix of the resource for the images for this
     * button.
     */
    public void setAllImages(String imageResPrefix)
    {
        setBackgroundImage(loadImage(imageResPrefix));
        setRolloverImage(loadImage(imageResPrefix + "_ROLLOVER"));
        setPressedImage(loadImage(imageResPrefix + "_PRESSED"));
        setDisabledImage(loadImage(imageResPrefix + "_DISABLED"));
    }

    /**
     * Change buttons index when we want to order it.
     * @param index the button index.
     */
    public void setIndex(int index)
    {
        this.index = index;
    }

    /**
     * Returns the current button index we have set, or -1 if none used.
     * @return
     */
    public int getIndex()
    {
        return this.index;
    }

    /**
     * Set the image to use for when this button is disabled
     *
     * @param disabledImage the disabled image
     */
    public void setDisabledImage(BufferedImageFuture disabledImage)
    {
        this.disabledImage = disabledImage;
    }

    /**
     * Load an image from resources and return it
     *
     * @param res The resource of the image
     * @return The image or null if it was not found
     */
    private static BufferedImageFuture loadImage(String res)
    {
        return resources.getBufferedImage(res);
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
