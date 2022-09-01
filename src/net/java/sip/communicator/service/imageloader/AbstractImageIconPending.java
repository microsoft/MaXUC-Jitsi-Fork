// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.imageloader;

import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.resources.*;

/**
 * This is a helper class to make it easy to represent
 * ImageIconFuture which is retrieved asynchronously
 */
public abstract class AbstractImageIconPending extends AbstractImageIconFuture
{
    /**
     * Steps to call when we are finished loading
     */
    private final List<Resolution<ImageIcon>> mResolutions = new LinkedList<>();

    /**
     * Has the icon been retrieved yet?
     */
    private boolean mRetrieved = false;

    /**
     * Implement this to create the requested icon. This may happen asynchronously.
     * Once the requested image has been retrieved, call retrievedIcon.
     *
     * Note, if you subclass AbstractImageIconPending, you must ensure this
     * is called once, which may be in the constructor.
     */
    protected abstract void retrieveIcon();

    /**
     * Cached pending buffered image
     * @see #getImage()
     */
    private AbstractBufferedImagePending mBufferedImage = null;

    @Override
    public synchronized BufferedImageFuture getImage()
    {
        if (mBufferedImage == null)
        {
            final AbstractImageIconFuture parent = this;

            final AbstractBufferedImagePending pending = new AbstractBufferedImagePending()
            {
                @Override
                public void retrieveImage()
                {
                    if (parent.mIcon != null)
                    {
                        mImage = ImageUtils.getBufferedImage(parent.mIcon.getImage());
                    }
                    retrievedImage();
                }
            };

            onResolve(new Resolution<>()
            {
                @Override
                public void onResolution(ImageIcon icon)
                {
                    pending.retrieveImage();
                }
            });

            mBufferedImage = pending;
        }

        return mBufferedImage;
    }

    /**
     * Call this when the requested icon is available.
     */
    protected synchronized void retrievedIcon()
    {
        mRetrieved = true;

        for (Resolution<ImageIcon> resolution : mResolutions)
        {
            resolution.onResolution(mIcon);
        }

        notifyAll();
    }

    @Override
    public ImageIcon resolve()
    {
        ImageIcon icon;

        try
        {
            icon = get();
        }
        catch (Exception e)
        {
            icon = null;
        }

        return icon;
    }

    @Override
    public synchronized void onUiResolve(Resolution<ImageIcon> resolution)
    {
        boolean retrieved;

        synchronized (this)
        {
            retrieved = mRetrieved;
        }

        if (retrieved)
        {
            resolution.onResolution(mIcon);
        }
        else
        {
            super.onUiResolve(resolution);
        }
    }

    @Override
    public synchronized void onResolve(Resolution<ImageIcon> resolution)
    {
        if (mRetrieved)
        {
            resolution.onResolution(mIcon);
        }
        else
        {
            mResolutions.add(resolution);
        }
    }

    @Override
    public synchronized ImageIcon get()
        throws InterruptedException
    {
        if (!mRetrieved)
        {
            wait();
        }

        return mIcon;
    }

    @Override
    public synchronized ImageIcon get(long timeout, TimeUnit unit)
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

        return mIcon;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        // Cancellation isn't supported
        return false;
    }

    @Override
    public boolean isCancelled()
    {
        // Cancellation isn't supported
        return false;
    }

    @Override
    public synchronized boolean isDone()
    {
        return mRetrieved;
    }
}
