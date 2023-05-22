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
import javax.swing.plaf.basic.*;

import org.jitsi.service.resources.*;

/**
 * A custom JButton that contains only text. A custom background could be set,
 * which will result in a round cornered background behind the text. Note that
 * you can also set a semi-transparent background. The button also supports a
 * rollover effect.
 *
 * @author Yana Stamcheva
 */
public class SIPCommTextButton extends JButton
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String UIClassID = "BasicButtonUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(UIClassID,
            BasicButtonUI.class.getName());
    }

    private final float[] borderColor
        = Color.DARK_GRAY.getRGBComponents(null);

    private BufferedImageFuture bgImage;

    /**
     * Creates a <tt>SIPCommTextButton</tt>.
     */
    public SIPCommTextButton()
    {
        this("", null);
    }

    /**
     * Creates a <tt>SIPCommTextButton</tt>
     * @param text the text of the button
     */
    public SIPCommTextButton(String text)
    {
        this(text, null);
    }

    public SIPCommTextButton(String text, BufferedImageFuture bgImage)
    {
        super(text);

        this.bgImage = bgImage;

        MouseRolloverHandler mouseHandler = new MouseRolloverHandler();

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

        this.setIcon(null);
        this.setIconTextGap(0);

        /*
         * Explicitly remove all borders that may be set from the current look
         * and feel.
         */
        this.setContentAreaFilled(false);
    }

    public void setBgImage(BufferedImageFuture image)
    {
        this.bgImage = image;
    }

    /**
     * Return the background image.
     *
     * @return the background image of this button
     */
    public BufferedImageFuture getBgImage()
    {
        return bgImage;
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
        Graphics2D g1 = (Graphics2D) g.create();
        try
        {
            internalPaintComponent(g1);
        }
        finally
        {
            g1.dispose();
        }

        super.paintComponent(g);
    }

    /**
     * Paints this button.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    private void internalPaintComponent(Graphics2D g)
    {
        AntialiasingManager.activateAntialiasing(g);

        float visibility = this.getModel().isRollover() ? 0.5f : 0.0f;

        Image bgImage = this.bgImage.resolve();

        if (visibility != 0.0f)
        {
            g.setColor(new Color(borderColor[0], borderColor[1],
                    borderColor[2], visibility));

            if (bgImage != null)
                g.fillRoundRect((this.getWidth() - bgImage.getWidth(null))/2,
                                (this.getHeight() - bgImage.getHeight(null))/2,
                                bgImage.getWidth(null) - 1,
                                bgImage.getHeight(null) - 1,
                                20, 20);
            else
                g.fillRoundRect(0, 0,
                                this.getWidth() - 1, this.getHeight() - 1,
                                20, 20);
        }

        if (bgImage != null)
        {
            g.drawImage(bgImage,
                (this.getWidth() - bgImage.getWidth(null))/2,
                (this.getHeight() - bgImage.getHeight(null))/2, null);
        }
        else
        {
            g.setColor(getBackground());
            g.fillRoundRect(1, 1,
                            this.getWidth() - 2, this.getHeight() - 2,
                            20, 20);
        }
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
       return UIClassID;
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
