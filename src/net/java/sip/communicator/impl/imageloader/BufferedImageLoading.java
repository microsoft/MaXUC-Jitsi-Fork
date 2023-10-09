// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.imageloader;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseFilePath;

import java.awt.image.*;
import java.net.*;

import javax.imageio.*;

import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * Represents an image being loaded from the ImageLoaderService
 */
public class BufferedImageLoading extends AbstractBufferedImagePending implements Runnable
{
    private static final Logger sLog = Logger.getLogger(BufferedImageLoading.class);

    private static final ResourceManagementService sResources = ImageLoaderActivator.getResources();

    /**
     * Image path to load
     */
    private final URL mUrl;

    public BufferedImageLoading(ImageID imageID)
    {
        this(sResources.getImageURL(imageID.getId()), imageID.toString());
    }

    public BufferedImageLoading(String imagePath)
    {
        this(sResources.getImageURLForPath(imagePath), imagePath);
    }

    private BufferedImageLoading(URL url, String entity)
    {
        mUrl = url;

        if (mUrl == null)
        {
            sLog.warn("No image for: " + entity);
        }

        retrieveImage();
    }

    @Override
    protected void retrieveImage()
    {
        if (mUrl == null)
        {
            // No image to load - just mark this as complete
            retrievedImage();
        }
        else if (mImage != null)
        {
            // Image already loaded
            retrievedImage();
        }
        else
        {
            mImage = ImageLoaderServiceImpl.getCachedImage(mUrl);

            if (mImage != null)
            {
                retrievedImage();
            }
            else
            {
                // Load the image on another thread
                ImageLoaderActivator.getThreadingService().submit("Load image: " + sanitiseFilePath(mUrl.toString()), this);
            }
        }
    }

    @Override
    public void run()
    {
        try
        {
            // Scale the image to fit the current display resolution
            BufferedImage image = ImageLoaderServiceImpl.scaleImage(ImageIO.read(mUrl));

            mImage = image;
            retrievedImage();

            ImageLoaderServiceImpl.putCachedImage(mUrl, image);
        }
        catch (Exception ex)
        {
            sLog.error("Failed to load image from path: " + sanitiseFilePath(mUrl.toString()), ex);

            // Although we haven't actually retrieved the image, we need to mark
            retrievedImage();
        }
    }
}
