// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import java.awt.image.*;
import java.util.concurrent.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

/**
 * This represents a ImageIconFuture, where we already have the ImageIcon
 * available - e.g. as we've done some complex combination of multiple images
 */
public class ImageIconAvailable extends AbstractImageIconFuture
{
    private final BufferedImageFuture mBufferedImage;

    public ImageIconAvailable(ImageIcon imageIcon)
    {
        mIcon = imageIcon;
        mBufferedImage = new BufferedImageAvailable(this, (BufferedImage) mIcon.getImage());
    }

    ImageIconAvailable(BufferedImageFuture bufferedImage, ImageIcon imageIcon)
    {
        mIcon = imageIcon;
        mBufferedImage = bufferedImage;
    }

    @Override
    public ImageIcon resolve()
    {
        return mIcon;
    }

    @Override
    public void onUiResolve(Resolution<ImageIcon> resolution)
    {
        resolution.onResolution(mIcon);
    }

    @Override
    public void onResolve(Resolution<ImageIcon> resolution)
    {
        resolution.onResolution(mIcon);
    }

    @Override
    public boolean isDone()
    {
        return true;
    }

    @Override
    public boolean isCancelled()
    {
        return false;
    }

    @Override
    public boolean cancel(boolean cancelled)
    {
        return false;
    }

    @Override
    public ImageIcon get()
    {
        return mIcon;
    }

    @Override
    public ImageIcon get(long time, TimeUnit unit)
    {
        return mIcon;
    }

    public BufferedImageFuture getImage()
    {
        return mBufferedImage;
    }
}
