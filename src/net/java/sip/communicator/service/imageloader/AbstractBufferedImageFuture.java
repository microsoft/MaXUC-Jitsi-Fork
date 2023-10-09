// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.resources.*;

/**
 * This is a helper class to make it easier to implement BufferedImageFuture.
 *
 * If you've got an image already in memory then prefer to just pass it to
 * BufferedImageAvailable. If you need to do some work to retrieve the image,
 * then prefer extending AbstractBufferedImagePending.
 *
 * If you've got an ImageIcon, see AbstractImageIconFuture.
 */
public abstract class AbstractBufferedImageFuture implements BufferedImageFuture
{
    /**
     * BufferedImage loaded by this image - null if still loading
     */
    protected BufferedImage mImage;

    @Override
    public void onUiResolve(final Resolution<BufferedImage> resolution)
    {
        onResolve(new Resolution<>()
        {
            @Override
            public void onResolution(final BufferedImage image)
            {
                if (SwingUtilities.isEventDispatchThread())
                {
                    resolution.onResolution(image);
                }
                else
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            resolution.onResolution(image);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void addToWindow(final Window window)
    {
        onUiResolve(new Resolution<>()
        {
            @Override
            public void onResolution(BufferedImage image)
            {
                window.setIconImage(image);
            }
        });
    }

    @Override
    public ImageIconFuture getScaledEllipticalIcon(final int width, final int height)
    {
        AlteredImageIconPending alteration = new AlteredImageIconPending(this)
        {
            @Override
            public ImageIcon alterImage(BufferedImage image)
            {
                return ImageUtils.getScaledEllipticalIcon(image, width, height);
            }
        };

        onResolve(alteration);

        return alteration;
    }

    @Override
    public ImageIconFuture getScaledRoundedIcon(final int width, final int height)
    {
        AlteredImageIconPending alteration = new AlteredImageIconPending(this)
        {
            @Override
            public ImageIcon alterImage(BufferedImage image)
            {
                return ImageUtils.getScaledRoundedIcon(image, width, height);
            }
        };

        onResolve(alteration);

        return alteration;
    }

    @Override
    public ImageIconFuture getScaledCircularIcon(final int width, final int height)
    {
        AlteredImageIconPending alteration = new AlteredImageIconPending(this)
        {
            @Override
            public ImageIcon alterImage(BufferedImage image)
            {
                return ImageUtils.getScaledCircularIcon(image, width, height);
            }
        };

        onResolve(alteration);

        return alteration;
    }

    @Override
    public BufferedImageFuture scaleImageWithinBounds(final int width, final int height)
    {
        AlteredBufferedImagePending alteration = new AlteredBufferedImagePending(this)
        {
            @Override
            public BufferedImage alterImage(BufferedImage image)
            {
                return ImageUtils.getBufferedImage(ImageUtils.scaleImageWithinBounds(image, width, height));
            }
        };

        onResolve(alteration);

        return alteration;
    }

    @Override
    public ImageIconFuture scaleIconWithinBounds(final int width, final int height)
    {
        AlteredImageIconPending alteration = new AlteredImageIconPending(this)
        {
            @Override
            public ImageIcon alterImage(BufferedImage image)
            {
                return ImageUtils.scaleIconWithinBounds(image, width, height);
            }
        };

        onResolve(alteration);

        return alteration;
    }

    @Override
    public byte[] getBytes()
    {
        BufferedImage image = resolve();

        if (image == null)
        {
            return new byte[0];
        }
        else
        {
            return ImageUtils.toByteArray(image);
        }
    }
}
