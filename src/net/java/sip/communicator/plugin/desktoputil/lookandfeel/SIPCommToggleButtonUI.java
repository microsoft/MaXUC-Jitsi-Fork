/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.lookandfeel;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;

/**
 * SIPCommToggleButtonUI implementation.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommToggleButtonUI
    extends BasicToggleButtonUI
    implements Skinnable
{
    /**
     * The background button image.
     */
    private static BufferedImageFuture sButtonBG =
        GuiActivator.getImageLoaderService().getImage(ImageLoaderService.BUTTON);

    /**
     * The background image for the pressed button state.
     */
    private static BufferedImageFuture sButtonPressedBG =
        GuiActivator.getImageLoaderService().getImage(
                                        ImageLoaderService.TOGGLE_BUTTON_PRESSED);

    /**
     * Indicates if the button has been pressed.
     */
    private boolean bufferIsPressed = false;

    /**
     * The paint buffer.
     */
    private BufferedImage paintBuffer = null;

    /**
     * The buffered component.
     */
    private Component bufferedComponent = null;

    /**
     * Creates the UI for the given <tt>JComponent</tt>.
     *
     * @param c the <tt>JComponent</tt>, for which to create an UI
     * @return the component UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommToggleButtonUI();
    }

    /**
     * Installs default configurations for the given <tt>AbstractButton</tt>.
     *
     * @param b the button, for which we're installing the defaults
     */
    public void installDefaults(AbstractButton b)
    {
        super.installDefaults(b);

        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(true);
    }

    /**
     * Uninstalls default configurations for the given <tt>AbstractButton</tt>.
     *
     * @param b the button, for which we're uninstalling the defaults
     */
    public void uninstallDefaults(AbstractButton b)
    {
        super.uninstallDefaults(b);

        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setOpaque(true);
    }

    /**
     * Paints this button UI.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     * @param c the <tt>Component</tt> to paint
     */
    public void paint(Graphics g, JComponent c)
    {
        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();

        boolean isShownPressed =
            model.isArmed() && model.isPressed() || model.isSelected();

        // check if the context of the buffer is consistent or else recreate it
        if (paintBuffer == null || c != bufferedComponent
            || bufferIsPressed != isShownPressed)
        {
            // create a buffer in the best available format
            paintBuffer =
                ((Graphics2D) g).getDeviceConfiguration()
                    .createCompatibleImage(c.getWidth(), c.getHeight(),
                        Transparency.TRANSLUCENT);

            // save the context
            bufferedComponent = c;
            bufferIsPressed = isShownPressed;

            BufferedImage leftImg;
            BufferedImage middleImg;
            BufferedImage rightImg;

            int imgWidth;
            int imgHeight;
            int indentWidth = 10;

            if (isShownPressed)
            {
                BufferedImage buttonPressedBG = sButtonPressedBG.resolve();

                imgWidth = buttonPressedBG.getWidth();
                imgHeight = buttonPressedBG.getHeight();

                leftImg = buttonPressedBG.getSubimage(0, 0, 10, imgHeight);
                middleImg =
                    buttonPressedBG
                        .getSubimage(10, 0, imgWidth - 20, imgHeight);
                rightImg =
                    buttonPressedBG
                        .getSubimage(imgWidth - 10, 0, 10, imgHeight);
            }
            else
            {
                BufferedImage buttonBG = sButtonBG.resolve();

                imgWidth = buttonBG.getWidth();
                imgHeight = buttonBG.getHeight();

                leftImg = buttonBG.getSubimage(0, 0, 10, imgHeight);
                middleImg =
                    buttonBG.getSubimage(10, 0, imgWidth - 20, imgHeight);
                rightImg =
                    buttonBG.getSubimage(imgWidth - 10, 0, 10, imgHeight);
            }

            Graphics2D g2 = paintBuffer.createGraphics();

            AntialiasingManager.activateAntialiasing(g2);

            g2.drawImage(leftImg, 0, 0, indentWidth, c.getHeight(), null);
            g2.drawImage(middleImg, indentWidth, 0, c.getWidth() - 2
                * indentWidth, c.getHeight(), null);
            g2.drawImage(rightImg, c.getWidth() - indentWidth, 0, indentWidth,
                c.getHeight(), null);
        }

        AntialiasingManager.activateAntialiasing(g);

        // draw the buffer in the graphics object
        g.drawImage(paintBuffer, 0, 0, c.getWidth(), c.getHeight(), 0, 0, c
            .getWidth(), c.getHeight(), null);

        super.paint(g, c);
    }

    /**
     * Reloads buffered images.
     */
    public void loadSkin()
    {
        sButtonBG =
            GuiActivator.getImageLoaderService().getImage(ImageLoaderService.BUTTON);

        sButtonPressedBG =
            GuiActivator.getImageLoaderService().getImage(
                                      ImageLoaderService.TOGGLE_BUTTON_PRESSED);
    }
}
