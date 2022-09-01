// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import java.awt.image.*;

import org.jitsi.service.resources.*;

abstract class AlteredBufferedImagePending
    extends AbstractBufferedImagePending
    implements Resolution<BufferedImage>
{
    private final AbstractBufferedImageFuture mFuture;

    AlteredBufferedImagePending(AbstractBufferedImageFuture future)
    {
        mFuture = future;
    }

    @Override
    public void onResolution(BufferedImage image)
    {
        retrieveImage();
    }

    @Override
    public void retrieveImage()
    {
        mImage = alterImage(mFuture.mImage);
        retrievedImage();
    }

    public abstract BufferedImage alterImage(BufferedImage image);
}