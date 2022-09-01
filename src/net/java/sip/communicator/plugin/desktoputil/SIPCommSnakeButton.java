// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * Class representing a "snake" button.
 *
 * A snake button is a button which has a left hand side (the head), a repeating
 * middle, then a tail.
 *
 * All snake buttons should have their component images defined in the same way:
 * "button_name"."position"."state"
 * Where
 * "position" is one of "LEFT", "RIGHT" or "MIDDLE"
 * "state" is one of "NORMAL", "PRESSED" or "ROLLOVER"
 *
 * The middle image should be very thin - ideally 1 pixel so that it tiles
 * correctly.
 *
 * Note that any of the right, left and middle images can be null - in this
 * case they won't be drawn.  But at least one should be non-null.
 */
public class SIPCommSnakeButton extends JButton implements MouseListener
{
    private static final long serialVersionUID = 1L;
    private static int textPadding = ScaleUtils.scaleInt(3);

    static final ResourceManagementService sRes = UtilActivator.getResources();

    // Images for this button:
    private final BufferedImageFuture mLeftNormal;
    private final BufferedImageFuture mMiddleNormal;
    private final BufferedImageFuture mRightNormal;
    private final BufferedImageFuture mLeftPressed;
    private final BufferedImageFuture mMiddlePressed;
    private final BufferedImageFuture mRightPressed;
    private final BufferedImageFuture mLeftRollover;
    private final BufferedImageFuture mMiddleRollover;
    private final BufferedImageFuture mRightRollover;
    private final BufferedImageFuture mLeftDisabled;
    private final BufferedImageFuture mMiddleDisabled;
    private final BufferedImageFuture mRightDisabled;

    // Mouse states:
    private boolean mMouseOver = false;
    private boolean mMouseClicked = false;

    /**
     * The text we are displaying
     */
    private String mText;

    /**
     * If true, then the button will expand to take up all available space,
     * rather than being as wide as the text requires
     */
    private final boolean mExpand;

    /**
     * The expected size of this button - can be queried to find out how large
     * the button needs to be before it has been properly expanded.
     */
    private Dimension mExpectedSize;

    /** Alpha value to use in faded versions of this button. */
    private static final float DIMMED_BUTTON_ALPHA = 0.8f;

    /**
     * Create a new SIPCommSnakeButton where the right hand side image might be
     * treated separately to the rest of the button
     *
     * @param text The text to display
     * @param imageRes The resource under which the images are stored
     */
    public SIPCommSnakeButton(String text, String imageRes)
    {
        this(text, imageRes, false, false);
    }

    /**
     * Create a new SIPCommSnakeButton where the right hand side image might be
     * treated separately to the rest of the button
     *
     * @param text The text to display
     * @param imageRes The resource under which the images are stored
     * @param expand If true, then the button will expand to fill all space
     */
    public SIPCommSnakeButton(String text, String imageRes, boolean expand)
    {
        this(text, imageRes, expand, false);
    }

    /**
     * Create a new SIPCommSnakeButton where the right hand side image might be
     * treated separately to the rest of the button
     *
     * @param text The text to display
     * @param imageRes The resource under which the images are stored
     * @param expand If true, then the button will expand to fill all space
     * @param dimmed If true, the button will be displayed at reduced alpha value
     */
    public SIPCommSnakeButton(String text, String imageRes, boolean expand, boolean dimmed)
    {
        super(text);
        setOpaque(false);

        // Load up all the images:
        mLeftNormal     = loadImage(imageRes + "." + "LEFT"   + "." + "NORMAL");
        mMiddleNormal   = loadImage(imageRes + "." + "MIDDLE" + "." + "NORMAL");
        mRightNormal    = loadImage(imageRes + "." + "RIGHT"  + "." + "NORMAL");
        mLeftPressed    = loadImage(imageRes + "." + "LEFT"   + "." + "PRESSED");
        mMiddlePressed  = loadImage(imageRes + "." + "MIDDLE" + "." + "PRESSED");
        mRightPressed   = loadImage(imageRes + "." + "RIGHT"  + "." + "PRESSED");
        mLeftRollover   = loadImage(imageRes + "." + "LEFT"   + "." + "ROLLOVER");
        mMiddleRollover = loadImage(imageRes + "." + "MIDDLE" + "." + "ROLLOVER");
        mRightRollover  = loadImage(imageRes + "." + "RIGHT"  + "." + "ROLLOVER");
        mLeftDisabled   = loadImage(imageRes + "." + "LEFT"   + "." + "DISABLED");
        mMiddleDisabled = loadImage(imageRes + "." + "MIDDLE" + "." + "DISABLED");
        mRightDisabled  = loadImage(imageRes + "." + "RIGHT"  + "." + "DISABLED");

        mText = text;
        mExpand = expand;

        // Set the size of this button
        setSize();

        // Remove the border as it looks bad in non-aero displays
        setBorder(BorderFactory.createEmptyBorder());

        addMouseListener(this);

        // Lower the opacity of the button to dim its appearance.
        if (dimmed)
        {
            Color foregroundColor = getForeground();
            setForeground(new Color(foregroundColor.getRed(),
                                    foregroundColor.getGreen(),
                                    foregroundColor.getBlue(), (int) (255 *
                    DIMMED_BUTTON_ALPHA)));
        }
    }

    /**
     * Sets the size of this button to be appropriate to the custom background
     */
    private void setSize()
    {
        BufferedImage left = mLeftNormal == null ? null : mLeftNormal.resolve();
        BufferedImage middle = mMiddleNormal == null ? null : mMiddleNormal.resolve();
        BufferedImage right = mRightNormal == null ? null : mRightNormal.resolve();

        // Set the preferred size to account for the new background.
        // Width is width of the left + text + text padding + right + some padding
        // Height is height of one of the images, we assume they're the same height,
        // or the text if they are null.
        int x = (left == null ? 0 : left.getWidth(null)) +
                ComponentUtils.getStringSize(this, mText).width +
                (right == null ? 0 : right.getWidth(null)) +
                2 * textPadding;
        int y = left != null ? left.getHeight(null) :
                right != null ? right.getHeight(null) :
                middle != null ? middle.getHeight(null) :
                ComponentUtils.getStringSize(this, mText).height;

        mExpectedSize = new Dimension(x, y);
        setPreferredSize(mExpectedSize);
        setMinimumSize(mExpectedSize);
    }

    @Override
    public void setFont(Font font)
    {
        super.setFont(font.deriveFont(ScaleUtils.getDefaultFontSize()));

        if (mText != null)
        {
            // If the font changes, the size will have changed too.
            setSize();
        }
    }

    @Override
    public void setText(String text)
    {
        if (mText != null)
            mText = text;

        if (mText != null)
            setSize();

        super.setText(text);
    }

    /**
     * Load an image from resources and return it
     *
     * @param res The resource of the image
     * @return The image or null if it was not found
     */
    private BufferedImageFuture loadImage(String res)
    {
        return sRes.getBufferedImage(res);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        BufferedImageFuture leftImageFuture = mLeftNormal;
        BufferedImageFuture rightImageFuture = mRightNormal;
        BufferedImageFuture middleImageFuture = mMiddleNormal;

        if (isEnabled())
        {
            if (mMouseClicked)
            {
                leftImageFuture = mLeftPressed;
                rightImageFuture = mRightPressed;
                middleImageFuture = mMiddlePressed;
            }
            else if (mMouseOver)
            {
                leftImageFuture = mLeftRollover;
                rightImageFuture = mRightRollover;
                middleImageFuture = mMiddleRollover;
            }
        }
        else
        {
            leftImageFuture = mLeftDisabled;
            rightImageFuture = mRightDisabled;
            middleImageFuture = mMiddleDisabled;
        }

        Image leftImage = leftImageFuture.resolve();
        Image rightImage = rightImageFuture.resolve();
        Image middleImage = middleImageFuture.resolve();

        // Set the co-ordinates of where we will start drawing
        int x = 0;
        int y = 0;

        g = g.create();

        try
        {
            boolean truncateText = false;
            AntialiasingManager.activateAntialiasing(g);
            Dimension stringSize = ComponentUtils.getStringSize(this, mText);

            // As JComponent#paintComponent says, if you do not invoke super's
            // implementation you must honor the opaque property, that is if
            // this component is opaque, you must completely fill in the
            // background in a non-opaque color. If you do not honor the opaque
            // property you will likely see visual artifacts.
            if (isOpaque())
            {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            // Draw the left hand image
            if (leftImage != null)
            {
                g.drawImage(leftImage, x, y, null);
                x += leftImage.getWidth(null);
            }

            // Remember where the left image ends so that we can draw the text
            // in the right place
            int origx = x + textPadding;

            // We expect that the central image is thin and should be tiled.
            // Thus keep adding it until we've covered all of the centre
            int centreWidth = 0;
            if (middleImage != null)
            {
                int middleWidth = middleImage.getWidth(null);

                if (mExpand)
                {
                    // We are configured to expand to fill all available space,
                    // thus the width of the centre is simply the width of the
                    // entire, less the left and right images.
                    centreWidth = getWidth() -
                            (leftImage != null ? leftImage.getWidth(null) : 0) -
                            (rightImage != null ? rightImage.getWidth(null) : 0);

                    // Need to change the "origx" value so as to draw the text
                    // in the centre
                    if (centreWidth > stringSize.width)
                        origx += (centreWidth - stringSize.width) / 2;
                }
                else
                {
                    centreWidth = stringSize.width + (2 * textPadding);

                    // If there is no right image, then add a little bit of padding
                    // to the right otherwise it looks off-centre
                    if (rightImage == null)
                        centreWidth += textPadding;

                    if (getParent() != null)
                    {
                        Dimension size = getParent().getSize();

                        if (getWidth() > size.width)
                        {
                            // This component is too big for the space available
                            centreWidth = size.width -
                                (leftImage != null ? leftImage.getWidth(null) : 0) -
                                (rightImage != null ? rightImage.getWidth(null) : 0);

                            truncateText = true;
                        }
                    }
                }

                for (int i = 0; i < centreWidth; i += middleWidth)
                {
                    g.drawImage(middleImage, x, y, null);
                    x += middleWidth;
                }
            }

            // Draw the right hand image
            if (rightImage != null)
            {
                g.drawImage(rightImage, x, y, null);
            }

            // Finally, draw the text on top of the background (but only if we
            // have a middle image to draw onto)
            if (middleImage != null)
            {
                if (isEnabled())
                    g.setColor(getForeground());
                else
                    g.setColor(Color.GRAY);

                y = (middleImage.getHeight(null) + stringSize.height - 8) / 2;

                // Truncate the text if we need to.
                String text = mText;

                if (truncateText)
                {
                    // Make the string shorter until it is short enough.  But
                    // don't go too short.
                    while (ComponentUtils.getStringWidth(this, text) > centreWidth &&
                           text.length() > 10)
                    {
                        text = text.substring(0, text.length() - 5) + "...";
                    }
                }

                g.drawString(text, origx, y);
            }
        }
        finally
        {
            g.dispose();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        // Nothing required
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        mMouseOver = true;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        // Update mouse clicked too, we don't want the button to be stuck "down"
        mMouseOver = false;
        mMouseClicked = false;
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        mMouseClicked = true;
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        mMouseClicked = false;
        repaint();
    }

    /**
     * @return The expected size of this button - can be used to find out
     * how large the button needs to be before it has been properly expanded.
     */
    Dimension getExpectedSize()
    {
        return mExpectedSize;
    }
}
