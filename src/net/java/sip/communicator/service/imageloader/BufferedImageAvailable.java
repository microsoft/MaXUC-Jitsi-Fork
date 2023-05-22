// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import java.awt.*;
import java.awt.image.*;
import java.util.concurrent.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.CustomAnnotations.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * This represents a BufferedImageFuture, where we already have the image
 * available as a BufferedImage.
 *
 * If we have a byte array, use BufferedImageAvailableFromBytes instead.
 */
public class BufferedImageAvailable extends AbstractBufferedImageFuture
{
    private final ImageIconFuture mImageIcon;

    BufferedImageAvailable(ImageIconFuture imageIcon, BufferedImage image)
    {
        mImage = image;
        mImageIcon = imageIcon;
    }

    public BufferedImageAvailable(BufferedImage image)
    {
        mImage = image;
        mImageIcon = new ImageIconAvailable(this, image != null ? new ImageIcon(image) : null);
    }

    public BufferedImageAvailable(Image image)
    {
       this(ImageUtils.getBufferedImage(image));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        return false;
    }

    @Override
    public boolean isCancelled()
    {
        return false;
    }

    @Override
    public boolean isDone()
    {
        return true;
    }

    @Override
    public BufferedImage get()
    {
        return mImage;
    }

    @Override
    public BufferedImage get(long timeout, @NotNull TimeUnit unit)
    {
        return mImage;
    }

    @Override
    public ImageIconFuture getImageIcon()
    {
        return mImageIcon;
    }

    @Override
    public BufferedImage resolve()
    {
        return mImage;
    }

    @Override
    public void onResolve(Resolution<BufferedImage> resolution)
    {
        resolution.onResolution(mImage);
    }
}
