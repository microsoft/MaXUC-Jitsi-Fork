/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>FileImageLabel</tt> is a <tt>JLabel</tt> associated with a file. It
 * can be dragged to the file system or some other drop area. It also has an
 * extended tooltip that can show a file preview or any other image.
 *
 * @author Yana Stamcheva
 */
public class FileImageLabel
    extends FileDragLabel
{
    private static final long serialVersionUID = 0L;
    private static final Logger logger = Logger.getLogger(FileImageLabel.class);

    private ImageIcon tooltipIcon;

    private String tooltipTitle;

    /**
     * Sets the icon to show in the tool tip.
     *
     * @param icon the icon to show in the tool tip.
     */
    public void setToolTipImage(ImageIcon icon)
    {
        this.tooltipIcon = scaleFileIcon(icon, 640, 480);
    }

    /**
     * Sets the text of the tool tip.
     *
     * @param text the text to set
     */
    public void setToolTipText(String text)
    {
        super.setToolTipText("");

        this.tooltipTitle = text;
    }

    /**
     * Create tool tip.
     */
    public JToolTip createToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(false);

        if (tooltipIcon != null)
            tip.setImage(tooltipIcon);

        if (tooltipTitle != null)
            tip.setTitle(tooltipTitle);

        tip.setComponent(this);

        return tip;
    }

    /**
     * Sets the file associated with this file drag label.
     *
     * @param file the file associated with this file drag label
     */
    public void setFile(File file)
    {
        super.setFile(file);

        setFileIcon(file);
    }

    /**
     * Returns the string to be used as the tooltip for <i>event</i>. We
     * don't really use this string, but we need to return different string
     * each time in order to make the TooltipManager change the tooltip over
     * the different cells in the JList.
     *
     * @return the string to be used as the tooltip for <i>event</i>.
     */
    public String getToolTipText(MouseEvent event)
    {
        if (tooltipIcon != null)
            return tooltipIcon.toString();

        return "";
    }

    /**
     * Sets the icon for the given file.
     *
     * @param file the file to set an icon for
     */
    private void setFileIcon(File file)
    {
        if (FileUtils.isImage(file.getName()))
        {
            try
            {
                ImageIcon icon = new ImageIcon(file.toURI().toURL());
                this.setToolTipImage(icon);

                ImageIcon image = scaleFileIcon(icon, 64, 64);

                this.setIcon(image);
            }
            catch (MalformedURLException e)
            {
                logger.debug("Could not locate image.", e);

                GuiActivator.getImageLoaderService().getImage(
                    ImageLoaderService.DEFAULT_FILE_ICON)
                    .getImageIcon()
                    .addToLabel(this);
            }
        }
        else
        {
            Icon icon = FileUtils.getIcon(file);

            if (icon == null)
            {
                GuiActivator.getImageLoaderService().getImage(
                    ImageLoaderService.DEFAULT_FILE_ICON)
                    .getImageIcon()
                    .addToLabel(this);
            }
            else
            {
                setIcon(icon);
            }
        }
    }

    /**
     * Returns a scaled instance of the given icon if it exceeds the given
     * bounds.
     * @param icon the icon to scale
     * @param width the scale width
     * @param height the scale height
     * @return  a scaled instance of the given icon if it exceeds the given
     * bounds
     */
    private ImageIcon scaleFileIcon(ImageIcon icon, int width, int height)
    {
        ImageIcon image = null;
        if (icon.getIconWidth() <= width && icon.getIconHeight() <= height)
            image = icon;
        else
            image = ImageUtils
                .getScaledRoundedIcon(icon.getImage(), width, height);

        return image;
    }
}
