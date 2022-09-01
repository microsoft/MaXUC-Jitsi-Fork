// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.resources.*;

/**
 * This represents a BufferedImageFuture, where we already have the image
 * available as bytes (e.g. from the network).
 *
 * If we already have a BufferedImage, use BufferedImageAvailable instead.
 */
public class BufferedImageAvailableFromBytes
    extends BufferedImageAvailable
{
    private final byte[] mImageBytes;

    public static BufferedImageFuture fromBytes(byte[] bs)
    {
        BufferedImageFuture future = null;

        if (bs != null && bs.length != 0)
        {
            future = new BufferedImageAvailableFromBytes(bs);
        }

        return future;
    }

    private BufferedImageAvailableFromBytes(byte[] bs)
    {
        super(ImageUtils.getImageFromBytes(bs));

        mImageBytes = bs.clone();
    }

    /**
     * Get a byte array representing this image.
     *
     * This is overridden implementation guaranteed to provide an array
     * containing the same bytes that were passed in to construct the instance.
     *
     * @return byte A byte array
     */
    @Override
    public byte[] getBytes()
    {
        return mImageBytes;
    }
}
