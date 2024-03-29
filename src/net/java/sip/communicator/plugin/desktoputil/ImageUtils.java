/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;

import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.service.resources.ImageIconFuture;
import org.jitsi.util.Logger;
import org.jitsi.util.OSUtils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.CompoundException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

/**
 * Utility methods for image manipulation.
 *
 */
public class ImageUtils
{
    /**
     * The <tt>Logger</tt> used by the <tt>ImageUtils</tt> class for logging
     * output.
     */
    private static final Logger logger = Logger.getLogger(ImageUtils.class);

    /**
     * Different shapes that an image can be cropped to.
     */
    private enum Shape
    {
        /**
         * Ellipse with the same height and width as the scaled image (this
         * will be a circle if the un-cropped image is square).
         */
        ELLIPSE,

        /**
         * Rectangle with the corners rounded to arcs with radius 3 pixels
         */
        ROUNDED_RECTANGLE,

        /**
         * A circle
         */
        CIRCLE
    }

    /**
     * Returns a scaled image fitting within the given bounds while keeping the
     * aspect ratio.
     *
     * @param image the image to scale
     * @param width maximum width of the scaled image
     * @param height maximum height of the scaled image
     * @return the scaled image
     */
    public static Image scaleImageWithinBounds( Image image,
                                                int width,
                                                int height)
    {
        Image scaledImage;

        if (image == null)
        {
            scaledImage = null;
        }
        else
        {
            int initialWidth = image.getWidth(null);
            int initialHeight = image.getHeight(null);

            int scaleHint = Image.SCALE_SMOOTH;
            double originalRatio =
                (double) initialWidth / initialHeight;
            double areaRatio = (double) width / height;

            if(originalRatio > areaRatio)
                scaledImage = image.getScaledInstance(width, -1, scaleHint);
            else
                scaledImage = image.getScaledInstance(-1, height, scaleHint);
        }

        return scaledImage;
    }

    /**
     * Scales the given <tt>image</tt> to fit in the given <tt>width</tt> and
     * <tt>height</tt>.
     * @param image the image to scale
     * @param width the desired width
     * @param height the desired height
     * @return the scaled image
     */
    public static ImageIconFuture scaleIconWithinBounds(BufferedImageFuture image, int width,
        int height)
    {
        return image.scaleIconWithinBounds(width, height);
    }

    /**
     * Scales the given <tt>image</tt> to fit in the given <tt>width</tt> and
     * <tt>height</tt>.
     * @param image the image to scale
     * @param width the desired width
     * @param height the desired height
     * @return the scaled image
     */
    public static ImageIcon scaleIconWithinBounds(Image image, int width,
        int height)
    {
        return new ImageIcon(scaleImageWithinBounds(image, width, height));
    }

    /**
     * Creates a rounded avatar image.
     *
     * @param image image of the initial avatar image.
     * @param width the desired width
     * @param height the desired height
     * @return The rounded corner image.
     */
    public static Image getScaledRoundedImage(  Image image,
                                                int width,
                                                int height)
    {
        return getScaledImage(image, Shape.ROUNDED_RECTANGLE, width, height);
    }

    /**
     * Creates a elliptical avatar image.
     *
     * @param image image of the initial avatar image.
     * @param width the desired width
     * @param height the desired height
     * @return The elliptical image.
     */
    public static Image getScaledEllipticalImage(  Image image,
                                                int width,
                                                int height)
    {
        return getScaledImage(image, Shape.ELLIPSE, width, height);
    }

    /**
     * Creates a circular avatar image.
     *
     * @param image image of the initial avatar image.
     * @param width the desired width
     * @param height the desired height
     * @return The circular image.
     */
    public static Image getScaledCircularImage(Image image,
                                               int width,
                                               int height)
       {
           return getScaledImage(image, Shape.CIRCLE, width, height);
       }

    /**
     * Creates an avatar image in the specified shape.
     *
     * @param image image of the initial avatar image.
     * @param shape the desired shape
     * @param width the desired width
     * @param height the desired height
     * @return The cropped, scaled image.
     */
    private static Image getScaledImage(  Image image,
                                          Shape shape,
                                          int width,
                                          int height)
    {
        ImageIcon scaledImage =
            ImageUtils.scaleIconWithinBounds(image, width, height);
        int scaledImageWidth = scaledImage.getIconWidth();
        int scaledImageHeight = scaledImage.getIconHeight();

        if(scaledImageHeight <= 0 ||
           scaledImageWidth <= 0)
            return null;

        // Just clipping the image would cause jaggies on Windows and Linux.
        // The following is a soft clipping solution based on the solution
        // proposed by Chris Campbell:
        // https://java.sun.com/mailers/techtips/corejava/2006/tt0923.html
        BufferedImage destImage
            = new BufferedImage(scaledImageWidth, scaledImageHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = destImage.createGraphics();

        try
        {
            // Render our clip shape into the image.  Note that we enable
            // antialiasing to achieve the soft clipping effect.
            g.setComposite(AlphaComposite.Src);
            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.WHITE);

            switch (shape)
            {
            case ELLIPSE:
                g.fillOval(0, 0, scaledImageWidth, scaledImageHeight);
                break;
            case ROUNDED_RECTANGLE:
                g.fillRoundRect(0, 0,
                                scaledImageWidth, scaledImageHeight,
                                5, 5);
                break;
            case CIRCLE:
                Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, scaledImageWidth, scaledImageHeight);
                g.fill(circle);
                break;
            }

            // We use SrcAtop, which effectively uses the
            // alpha value as a coverage value for each pixel stored in the
            // destination.  For the areas outside our clip shape, the
            // destination alpha will be zero, so nothing is rendered in those
            // areas. For the areas inside our clip shape, the destination alpha
            // will be fully opaque, so the full color is rendered. At the edges,
            // the original antialiasing is carried over to give us the desired
            // soft clipping effect.
            g.setComposite(AlphaComposite.SrcAtop);
            g.drawImage(scaledImage.getImage(), 0, 0, null);
        }
        finally
        {
            g.dispose();
        }
        return destImage;
    }

    /**
     * Returns a scaled rounded instance of the given <tt>image</tt>.
     * @param image the image to scale
     * @param width the desired width
     * @param height the desired height
     * @return a byte array containing the scaled rounded image
     */
    public static byte[] getScaledInstanceInBytes(
        Image image, int width, int height)
    {
        BufferedImage scaledImage
            = (BufferedImage) getScaledRoundedImage(image, width, height);

        return convertImageToBytes(scaledImage);
    }

    /**
     * Returns a scaled rounded icon from the given <tt>image</tt>, scaled
     * within the given <tt>width</tt> and <tt>height</tt>.
     * @param image the image to scale
     * @param width the maximum width of the scaled icon
     * @param height the maximum height of the scaled icon
     * @return a scaled rounded icon
     */
    public static ImageIcon getScaledRoundedIcon(Image image, int width,
        int height)
    {
        Image scaledImage = getScaledRoundedImage(image, width, height);

        if (scaledImage != null)
            return new ImageIcon(scaledImage);

        return null;
    }

    /**
     * Returns a scaled elliptical icon from the given <tt>image</tt>, scaled
     * within the given <tt>width</tt> and <tt>height</tt>.
     * @param image the image to scale
     * @param width the maximum width of the scaled icon
     * @param height the maximum height of the scaled icon
     * @return a scaled elliptical icon
     */
    public static ImageIconFuture getScaledEllipticalIcon(BufferedImageFuture image, int width,
        int height)
    {
        return image.getScaledEllipticalIcon(width,  height);
    }

    public static ImageIcon getScaledEllipticalIcon(Image image, int width,
        int height)
    {
        Image scaledImage = getScaledEllipticalImage(image, width, height);

        if (scaledImage != null)
            return new ImageIcon(scaledImage);

        return null;
    }

    /**
     * Returns a scaled circular icon from the given <tt>image</tt>, scaled
     * within the given <tt>width</tt> and <tt>height</tt>.
     * @param image the image to scale
     * @param width the maximum width of the scaled icon
     * @param height the maximum height of the scaled icon
     * @return a scaled circular icon
     */
    public static ImageIconFuture getScaledCircularIcon(BufferedImageFuture image, int width,
        int height)
    {
        return image.getScaledCircularIcon(width, height);
    }

    public static ImageIcon getScaledCircularIcon(Image image, int width,
                                                    int height)
    {
        Image scaledImage = getScaledCircularImage(image, width, height);

        if (scaledImage != null)
            return new ImageIcon(scaledImage);

        return null;
    }

    /**
     * Applies a rotation to the given image.
     *
     * @param img The image to rotate.
     * @param quadrants The number of clockwise right-angles to rotate by.
     * @return The rotated image.
     */
    public static BufferedImage rotate(BufferedImage img, int quadrants)
    {
        AffineTransform tx = new AffineTransform();

        // Translate before rotating, to avoid black scaling bars at the edges
        // of the image. Note that the tx.rotate(angle, centerX, centerY)
        // method doesn't solve this problem.
        // We translate so that we're rotating about the centerpoint of the
        // image, perform the rotation, and then retranslate so that the new
        // bottom-left corner of the image is at the origin.
        // Note that AffineTransforms compound in reverse, i.e. transformations
        // are applied in the opposite order to that given.
        if (quadrants % 2 == 1)
        {
            tx.translate(img.getHeight() / 2, img.getWidth() / 2);
        }
        else
        {
            tx.translate(img.getWidth() / 2, img.getHeight() / 2);
        }
        tx.rotate((Math.PI * quadrants) / 2);
        tx.translate(-img.getWidth() / 2, -img.getHeight() / 2);

        // Use high-quality bicubic interpolation to avoid artifacts.
        AffineTransformOp op = new AffineTransformOp(tx,
                                                AffineTransformOp.TYPE_BICUBIC);
        return op.filter(img, null);
    }

    /**
     * Reflects the given image in either or both axes.
     *
     * @param img The image to reflect.
     * @param flipX Whether to flip the x-coordinate (i.e. reflect in the
     * y-axis)
     * @param flipY Whether to flip the y-coordinate (i.e. reflect in the
     * x-axis)
     * @return The reflected image.
     */
    public static BufferedImage flipImage(BufferedImage img, boolean flipX,
                                                                  boolean flipY)
    {
        AffineTransform tx = new AffineTransform();
        tx.scale(flipX ? -1 : 1, flipY ? -1 : 1);
        tx.translate(flipX ? -img.getWidth() : 0, flipY ? -img.getHeight() : 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
        return op.filter(img, null);
    }

    /**
     * Load the specified file from disc, and apply any re-orientation specified
     * in the EXIF metadata.
     * Note that if we fail to parse the MetaData, this will be logged, but no
     * exception will be thrown.
     *
     * @param file The file to load.
     * @param maxSideLength If the image is wider or higher than this, then
     *                      the image will be scaled before being returned.
     * @return The image contained in the file, scaled and with any specified
     *         rotations and reflections applied, or null if no appropriate
     *         {@link ImageReader} is available.
     *
     * @throws IOException If the file cannot be loaded.
     */
    public static BufferedImage loadImageAndCorrectOrientation(File file,
                                                               int maxSideLength)
        throws IOException
    {
        ImageInputStream imageInputStream =
            ImageIO.createImageInputStream(file);

        Iterator<ImageReader> imageReaders
            = ImageIO.getImageReaders(imageInputStream);

        // Check whether there are any available ImageReaders for this file
        if (!imageReaders.hasNext())
        {
            return null;
        }

        // Choose the first available compatible ImageReader
        ImageReader reader = imageReaders.next();
        reader.setInput(imageInputStream, true, true);

        ImageReadParam params = reader.getDefaultReadParam();

        int originalHeight = reader.getHeight(0);
        int originalWidth = reader.getWidth(0);

        if (originalHeight > maxSideLength || originalWidth > maxSideLength)
        {
            // n, where only every nth pixel (scanning across and down) of the
            // image file is stored in the BufferedImage.
            // Since we need to scale the image such that the greater of the
            // two edge lengths is less than the maximum allowed edge length,
            // find what factor greater the longest edge is than the maximum
            // allowed, and only include every this many pixels.
            // Add 1 after integer division to make sure we always skip more
            // pixels rather than fewer (thus ensuring we don't stray over
            // the maximum).
            int skipPixels = originalHeight > originalWidth ?
                originalHeight/maxSideLength + 1 :
                originalWidth/maxSideLength + 1;

            // Setting these parameters means the image will be sampled at a
            // smaller size as it is stored in memory. Loading the full sized
            // image before rescaling may cause an OutOfMemoryError.
            params.setSourceSubsampling(skipPixels, skipPixels, 0, 0);
        }

        BufferedImage image = reader.read(0, params);
        image = correctImageOrientation(file, image);

        imageInputStream.close();

        return image;
    }

    /**
     * @param file The file from which to get the image orientation metadata
     * @param image The image to correct
     * @return The supplied file with any specified rotations and reflections
     *         applied
     * @throws IOException
     */
    private static BufferedImage correctImageOrientation(File file,
                                                         BufferedImage image)
        throws IOException
    {
        if (OSUtils.IS_MAC)
        {
            logger.debug("Loading image; is MacOS so correct EXIF data.");
            int orientation = 1;
            try
            {
                Metadata metadata = ImageMetadataReader.readMetadata(file);
                Directory directory = metadata.getFirstDirectoryOfType(
                    ExifIFD0Directory.class);

                // Directory can be null if there is no EXIF metadata,
                // so only try to pull orientation if it exists
                if (directory != null)
                {
                    orientation = directory.getInt(
                        ExifIFD0Directory.TAG_ORIENTATION);
                }
            }
            catch (CompoundException e)
            {
                logger.warn("Failed to parse metadata for image file ", e);
            }

            image = applyExifOrientation(image, orientation);
        }
        return image;
    }

    /**
     * Applies the specified EXIF orientation code to the image data given.
     *
     * @param img The image to reorient.
     * @param orientation An orientation code from 1 to 8 inclusive.
     * See http://sylvana.net/jpegcrop/exif_orientation.html.
     *
     * @return The reoriented image.
     */
    public static BufferedImage applyExifOrientation(BufferedImage img,
                                                                int orientation)
    {
        // See http://sylvana.net/jpegcrop/exif_orientation.html.
        logger.debug("Loaded image has EXIF orientation " + orientation);
        switch (orientation)
        {
            case 1:
                // Identity orientation - no transformation required.
                break;
            case 2:
                // Flip in X.
                img = flipImage(img, true, false);
                break;
            case 3:
                // Rotate 180 degrees.
                img = rotate(img, 2);
                break;
            case 4:
                // Rotate 180 degrees and flip in X.
                img = rotate(img, 2);
                img = flipImage(img, true, false);
                break;
            case 5:
                // Rotate 90 degrees CW and flip in X.
                img = rotate(img, 1);
                img = flipImage(img, true, false);
                break;
            case 6:
                // Rotate 90 degrees CW.
                img = rotate(img, 1);
                break;
            case 7:
                // Rotate 90 degrees CW and flip in Y.
                img = rotate(img, 1);
                img = flipImage(img, false, true);
                break;
            case 8:
                // Rotate 90 degrees CCW.
                img = rotate(img, 3);
                break;
            default:
                // Unknown orientation - log and do nothing.
                logger.warn("Unknown EXIF orientation " + orientation +
                                                                  " - ignore.");
                break;
        }

        return img;
    }

    /**
     * Returns the buffered image corresponding to the given url image path.
     *
     * @param imagePath the path indicating, where we can find the image.
     *
     * @return the buffered image corresponding to the given url image path.
     */
    public static BufferedImage getBufferedImage(URL imagePath)
    {
        BufferedImage image = null;

        if (imagePath != null)
        {
            try
            {
                image = ImageIO.read(imagePath);
            }
            catch (IOException ex)
            {
                logger.debug("Failed to load image:" + imagePath, ex);
            }
        }
        return image;
    }

    /**
     * Returns the buffered image corresponding to the given image
     * @param source an image
     * @return the buffered image corresponding to the given image
     */
    public static BufferedImage getBufferedImage(Image source)
    {
        if (source == null)
        {
            return null;
        }
        else if (source instanceof BufferedImage)
        {
            return (BufferedImage) source;
        }

        int width = source.getWidth(null);
        int height = source.getHeight(null);

        BufferedImage image
            = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = image.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();

        return image;
    }

    /**
     * Extracts bytes from image.
     * @param image the image.
     * @return the bytes of the image.
     */
    public static byte[] toByteArray(BufferedImage image)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            ImageIO.write(image, "png", out);
        }
        catch (IOException e)
        {
            logger.debug("Cannot convert buffered image to byte[]", e);
            return null;
        }

        return out.toByteArray();
    }

    /**
     * Loads an image from a given bytes array.
     * @param imageBytes The bytes array to load the image from.
     * @return The image for the given bytes array.
     */
    public static BufferedImage getImageFromBytes(byte[] imageBytes)
    {
        BufferedImage image = null;
        try
        {
            image = ImageIO.read(
                    new ByteArrayInputStream(imageBytes));
        }
        catch (Exception e)
        {
            logger.error("Failed to convert bytes to image.", e);
        }
        return image;
    }

    /**
     * Returns a byte array of the given <tt>image</tt>.
     *
     * @param image the image to convert
     * @return a byte array containing the scaled image
     */
    private static byte[] convertImageToBytes(BufferedImage image)
    {
        byte[] scaledBytes = null;

        if (image != null)
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            try
            {
                ImageIO.write(image, "png", outStream);
                scaledBytes = outStream.toByteArray();
            }
            catch (IOException e)
            {
                logger.debug("Could not scale image in bytes.", e);
            }
        }

        return scaledBytes;
    }

    /**
     * Removes all color from an image and makes it partly translucent.
     *
     * @param source picture to be faded
     * @return faded imaged
     */
    public static BufferedImage fadeImage(BufferedImage source)
    {
        int width = source.getWidth();
        int height = source.getHeight();

        BufferedImage image
            = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int row = 0; row < width; ++row)
        {
            for (int col = 0; col < height; ++col)
            {
                int c = source.getRGB(row, col);

                int r =
                    (((c >> 16) & 0xff) + ((c >> 8) & 0xff) + (c & 0xff)) / 3;

                int newRgb = (0xff << 24) | (r << 16) | (r << 8) | r;
                newRgb &= (1 << 24) - 1; // Blanks alpha value
                newRgb |= 128 << 24; // Resets it to the alpha of 128
                image.setRGB(row, col, newRgb);
            }
        }

        return image;
    }
}
