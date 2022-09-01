// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import java.awt.image.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

abstract class AlteredImageIconPending
    extends AbstractImageIconPending
    implements Resolution<BufferedImage>
{
    private final AbstractBufferedImageFuture mFuture;

    AlteredImageIconPending(AbstractBufferedImageFuture future)
    {
        mFuture = future;
    }

    @Override
    public void onResolution(BufferedImage image)
    {
        retrieveIcon();
    }

    @Override
    public void retrieveIcon()
    {
        if (mFuture.mImage != null)
        {
            mIcon = alterImage(mFuture.mImage);
        }

        retrievedIcon();
    }

    public abstract ImageIcon alterImage(BufferedImage image);
}