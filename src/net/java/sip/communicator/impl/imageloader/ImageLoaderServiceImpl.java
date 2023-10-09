/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.imageloader;

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.util.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Stores and loads images used throughout this UI implementation.
 */
public class ImageLoaderServiceImpl
        implements ImageLoaderService
{
    /**
     * The <tt>Logger</tt> used by the <tt>ImageLoader</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ImageLoaderService.class);

    /**
     * Stores all images that have been requested to be loaded - both those
     * being loaded and those already loaded.
     */
    private static final Map<ImageID, BufferedImageFuture> loadingImages =
            new Hashtable<>();

    /**
     * Stores all already loaded images.
     */
    private static final Map<URL, BufferedImage> loadedImages =
            new HashMap<>();

    static BufferedImage getCachedImage(URL url)
    {
        synchronized (loadedImages)
        {
            return loadedImages.get(url);
        }
    }

    static void putCachedImage(URL url, BufferedImage image)
    {
        synchronized (loadedImages)
        {
            loadedImages.put(url, image);
        }
    }

    public BufferedImageFuture getImage(ImageID imageID)
    {
        BufferedImageFuture image;

        synchronized (loadingImages)
        {
            if (loadingImages.containsKey(imageID))
            {
                image = loadingImages.get(imageID);
            }
            else
            {
                image = new BufferedImageLoading(imageID);
                loadingImages.put(imageID, image);
            }
        }

        image.resolve();

        return image;
    }

    public BufferedImageFuture getImageFromPath(String imagePath)
    {
        BufferedImageFuture future = new BufferedImageLoading(imagePath);

        future.resolve();

        return future;
    }

    public String getImageUri(ImageID imageID)
    {
        URL imageURL = ImageLoaderActivator.getResources().getImageURL(imageID.getId());

        try
        {
            if (imageURL != null)
                return imageURL.toURI().toString();
        }
        catch (URISyntaxException e)
        {
            logger.debug("Unable to parse image URL to URI.", e);
        }

        return null;
    }

    public ImageIconFuture getAccountStatusImage(ProtocolProviderService pps)
    {
        ImageIconFuture statusIcon;

        OperationSetPresence presence
            = pps.getOperationSet(OperationSetPresence.class);

        BufferedImageFuture statusImage;
        BufferedImageFuture protocolStatusIcon = null;

        if(presence != null)
            protocolStatusIcon = presence.getPresenceStatus().getStatusIcon();

        if (presence != null && protocolStatusIcon != null)
        {
            statusImage = protocolStatusIcon;
        }
        else
        {
            statusImage
                = pps.getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16);

            if (!pps.isRegistered())
            {
                statusImage
                    = new BufferedImageAvailable(LightGrayFilter.createDisabledImage(statusImage.resolve()));
            }
        }

        statusIcon = getIndexedProtocolIcon(statusImage, pps);

        return statusIcon;
    }

    public ImageIconFuture getIndexedProtocolIcon( BufferedImageFuture image,
                                                    ProtocolProviderService pps)
    {
        return getIndexedProtocolImage(image, pps).getImageIcon();
    }

    public BufferedImageFuture getIndexedProtocolImage(
        BufferedImageFuture image, ProtocolProviderService pps)
    {
        UIService uiService = ImageLoaderActivator.getUIService();

        int index = uiService.getProviderUIIndex(pps);

        BufferedImageFuture badged;

        if ((image != null) && index > 0)
        {
            // Since we are specifying the size of the image, we need to make
            // sure it is scaled, including any text
            BufferedImage buffImage =
                new BufferedImage(ScaleUtils.scaleInt(22),
                                  ScaleUtils.scaleInt(16),
                                  BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) buffImage.getGraphics();

            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.DARK_GRAY);
            Font defaultFont = uiService.getDefaultFont();
            g.setFont(defaultFont.deriveFont(Font.BOLD,
                                             ScaleUtils.getScaledFontSize(9f)));
            g.drawImage(image.resolve(), 0, 0, null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g.drawString(Integer.toString(index + 1),
                         ScaleUtils.scaleInt(14),
                         ScaleUtils.scaleInt(8));

            badged = new BufferedImageAvailable(buffImage);
        }
        else
        {
            // No need to scale the image as it must be provided scaled
            badged = image;
        }
        return badged;
    }

    public BufferedImageFuture getImage(BufferedImageFuture bgImageFuture, BufferedImageFuture topImageFuture, int x, int y)
    {
        Image bgImage = bgImageFuture.resolve();
        Image topImage = topImageFuture.resolve();

        BufferedImage buffImage
            = new BufferedImage(bgImage.getWidth(null),
                                bgImage.getHeight(null),
                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) buffImage.getGraphics();

        AntialiasingManager.activateAntialiasing(g);
        g.drawImage(bgImage, 0, 0, null);
        g.drawImage(topImage, x, y, null);

        // Scale the image to fit the current display resolution
        return new BufferedImageAvailable(scaleImage(buffImage));
    }

    public BufferedImageFuture getImage(BufferedImageFuture bgImageFuture, String text, Component c)
    {
        Image bgImage = bgImageFuture.resolve();

        BufferedImage buffImage
            = new BufferedImage(bgImage.getWidth(c),
                                bgImage.getHeight(c),
                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) buffImage.getGraphics();

        AntialiasingManager.activateAntialiasing(g);
        g.setColor(Color.WHITE);
        g.setFont(c.getFont().deriveFont(
            Font.BOLD, ScaleUtils.getScaledFontSize(9f)));
        g.drawImage(bgImage, 0, 0, null);

        FontMetrics fontMetrics = g.getFontMetrics();
        int fontHeight = fontMetrics.getHeight();
        int textWidth = fontMetrics.stringWidth(text);

        g.drawString(
                text,
                (bgImage.getWidth(null) - textWidth)/2 + 1,
                (bgImage.getHeight(null) - fontHeight)/2 + fontHeight - 3);

        // Scale the image to fit the current display resolution
        return new BufferedImageAvailable(scaleImage(buffImage));
    }

    public ImageIconFuture getIconFromPath(String imagePath)
    {
        return getImageFromPath(imagePath).getImageIcon();
    }

    public void clearCache()
    {
        logger.info("Clearing ImageLoaderService cache");
        loadedImages.clear();
        loadingImages.clear();
    }

    public ImageIconFuture getAuthenticationWindowIcon(
        ProtocolProviderService protocolProvider)
    {
        BufferedImageFuture image = null;

        if(protocolProvider != null)
        {
            ProtocolIcon protocolIcon = protocolProvider.getProtocolIcon();

            if(protocolIcon.isSizeSupported(ProtocolIcon.ICON_SIZE_64x64))
                image = protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_64x64);
            else if(protocolIcon.isSizeSupported(ProtocolIcon.ICON_SIZE_48x48))
                image = protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_48x48);
        }

        if (image != null)
            // Scale the image to fit the current display resolution
            return image.getImageIcon();

        return null;
    }

    /**
     * Scale the image so if fits the current displays scale
     * @param image The image to scale
     * @return The scaled image
     */
    static BufferedImage scaleImage(Image image)
    {
        if (image == null)
        {
            return null;
        }

        // Get the scaled dimensions
        int height = ScaleUtils.scaleInt(image.getHeight(null));
        int width = ScaleUtils.scaleInt(image.getWidth(null));

        Image tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Draw the scaled Image onto the new BufferedImage
        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

}
