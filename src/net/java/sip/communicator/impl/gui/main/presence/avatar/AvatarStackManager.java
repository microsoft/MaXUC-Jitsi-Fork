/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence.avatar;

import java.awt.image.*;
import java.io.*;

import javax.imageio.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.fileaccess.*;

/**
 * Take cares of storing(deleting, moving) images with the given indexes.
 */
public class AvatarStackManager
{
    /**
     * The logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(AvatarStackManager.class);

    /**
     * The folder where user avatars are stored.
     */
    private static final String STORE_DIR = "avatarcache" + File.separator
        + "userimages" + File.separator;

    /**
     * Load the image at the defined index from user directory
     * @param index the image index
     * @return the image
     */
    public static BufferedImage loadImage(int index)
    {
        File imageFile;

        try
        {
            String imagePath = STORE_DIR + index + ".png";

            imageFile
                = GuiActivator.getFileAccessService().getPrivatePersistentActiveUserFile(
                        imagePath);
        }
        catch (Exception e)
        {
            logger.error("Unable to access stored image at index " + index, e);
            return null;
        }

        // If the file don't exists, there is no more images to get
        if (!imageFile.exists())
            return null;

        try
        {
            return ImageIO.read(imageFile);
        }
        catch (IOException ioe)
        {
            logger.error("Failed to read file " + imageFile, ioe);
            return null;
        }
    }

    /**
     * Moves images.
     * @param oldIndex the old index.
     * @param newIndex the new index.
     */
    private static void moveImage(int oldIndex, int newIndex)
    {
        String oldImagePath = STORE_DIR + oldIndex + ".png";
        String newImagePath = STORE_DIR + newIndex + ".png";

        try
        {
            FileAccessService fas = GuiActivator.getFileAccessService();
            File oldFile = fas.getPrivatePersistentActiveUserFile(oldImagePath);

            if (oldFile.exists())
            {
                File newFile = fas.getPrivatePersistentFile(newImagePath);

                oldFile.renameTo(newFile);
            }
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

    /**
     * Removes the oldest image and as its with lower index.
     * Moves all indexes. Ant this way we free one index.
     * @param nbImages
     */
    public static void popFirstImage(int nbImages)
    {
        for (int i=nbImages-1; i>0; i--)
            moveImage(i, i-1);
    }

    /**
     * Stores an image.
     * @param image the image
     * @param index of the image.
     */
    public static void storeImage(BufferedImage image, int index)
    {
        String imagePath = STORE_DIR + index + ".png";

        try
        {
            FileAccessService fas = GuiActivator.getFileAccessService();
            File storeDir = fas.getPrivatePersistentActiveUserDirectory(STORE_DIR);

            // if dir doesn't exist create it
            storeDir.mkdirs();

            File file = fas.getPrivatePersistentActiveUserFile(imagePath);

            ImageIO.write(image, "png", file);
        }
        catch (IOException | SecurityException e)
        {
            logger.error("Failed to store image at index " + index, e);
        }
    }
}
