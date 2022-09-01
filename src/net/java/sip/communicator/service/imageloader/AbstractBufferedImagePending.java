// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import java.awt.image.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * This is a helper class to make it easy to represent
 * BufferedImageFuture which is retrieved asynchronously
 */
public abstract class AbstractBufferedImagePending extends AbstractBufferedImageFuture
{
    private static final Logger sLog = Logger.getLogger(AbstractBufferedImagePending.class);

    /**
     * Steps to call when we are finished loading
     */
    private final List<Resolution<BufferedImage>> mResolutions = new LinkedList<>();

    /**
     * Has the image been retrieved yet?
     */
    private boolean mRetrieved = false;

    /**
     * Cached pending image icon
     * @see #getImageIcon()
     */
    private AlteredImageIconPending mImageIcon = null;

    /**
     * Implement this to create the requested image. This may happen asynchronously.
     * Once the requested image has been retrieved, call retrievedImage.
     *
     * Note, if you subclass AbstractBufferedImagePending, you must ensure this
     * is called once, which may be in the constructor.
     */
    protected abstract void retrieveImage();

    /**
     * Call this when the requested image is available.
     */
    protected synchronized void retrievedImage()
    {
        mRetrieved = true;

        for (Resolution<BufferedImage> resolution : mResolutions)
        {
            resolution.onResolution(mImage);
        }

        this.notifyAll();
    }

    @Override
    public synchronized ImageIconFuture getImageIcon()
    {
        if (mImageIcon == null)
        {
            mImageIcon = new AlteredImageIconPending(this)
            {
                @Override
                public ImageIcon alterImage(BufferedImage image)
                {
                    return new ImageIcon(mImage);
                }
            };

            onResolve(mImageIcon);
        }

        return mImageIcon;
    }

    @Override
    public BufferedImage resolve()
    {
        BufferedImage image;

        try
        {
            image = get();
        }
        catch (Exception e)
        {
            sLog.error("Hit exception resolving image: ", e);
            image = null;
        }

        return image;
    }

    @Override
    public synchronized void onResolve(Resolution<BufferedImage> resolution)
    {
        if (mRetrieved)
        {
            resolution.onResolution(mImage);
        }
        else
        {
            mResolutions.add(resolution);
        }
    }

    @Override
    public synchronized BufferedImage get()
        throws InterruptedException
    {
        if (!mRetrieved)
        {
            wait();
        }

        return mImage;
    }

    @Override
    public synchronized BufferedImage get(long timeout, TimeUnit unit)
        throws InterruptedException,
            TimeoutException
    {
        if (!mRetrieved)
        {
            unit.timedWait(this, timeout);
        }

        if (!mRetrieved)
        {
            throw new TimeoutException();
        }

        return mImage;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        // We don't support cancellation.
        return false;
    }

    @Override
    public boolean isCancelled()
    {
        // We don't support cancellation, so always
        // return false to say we haven't been cancelled.
        return false;
    }

    @Override
    public synchronized boolean isDone()
    {
        return mRetrieved;
    }
}
