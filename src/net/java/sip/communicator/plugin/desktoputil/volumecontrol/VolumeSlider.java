/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil.volumecontrol;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicSliderUI;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.event.*;
import org.jitsi.service.resources.*;

/**
 * JComponent containing a horizontal volume slider with a "quiet" icon and a
 * "loud" icon on either side.
 */
public class VolumeSlider
    extends JComponent
    implements ChangeListener,
               MouseListener,
               VolumeChangeListener
{
    private static final long serialVersionUID = 1L;

    /**
     * The multiplier to convert between the float values returned by the
     * VolumeControl to the int values required to set the slider.
     */
    private static final int MULTIPLIER = 100;

    /**
     * The increments in which to increase/decrease the volume slider when
     * clicking on the quiet and loud icons either side. Value chosen to match
     * the behaviour of clicking on the slider track.
     */
    private static final int VOLUME_INCREMENT = 10;

    /**
     * The volume control which this slider sets.
     */
    private BasicVolumeControl mVolumeControl;

    /**
     * The current volume displayed by this slider, which matched the device
     * volume last time we checked.
     */
    private float mVolume;

    /**
     * Last set value of the slider.
     */
    private int mLastValue;

    /**
     * Runnable to pass to the threading service to change the volume.
     */
    private final Runnable setVolumeRunner = new Runnable()
        {
            @Override
            public void run()
            {
                int newValue = mSlider.getValue();

                if (newValue != mLastValue)
                {
                    mVolume = ((float) newValue)/((float) MULTIPLIER);
                    mVolumeControl.setVolume(mVolume);
                    mLastValue = newValue;
                }
            }
        };

    // For the UI...
    private final ResourceManagementService mResources =
                                           DesktopUtilActivator.getResources();
    private String mQuietResource;
    private String mLoudResource;
    private JLabel mRightLabel;
    private JLabel mLeftLabel;
    private JSlider mSlider;

    /**
     * @param volumeControl The volume control that this slider should set.
     */
    public VolumeSlider(BasicVolumeControl volumeControl)
    {
        mVolumeControl = volumeControl;
        mVolumeControl.addVolumeChangeListener(this);

        initializeUI();
    }

    /**
     * Returns the value of this slider. Used for testing.
     *
     * @return The value of the slider.
     */
    protected int getValue()
    {
        return mSlider.getValue();
    }

    /**
     * Returns the multiplier used for conversion between the value ranges of
     * BasicVolumeControl and VolumeSlider.
     *
     * @return The conversion factor from BasicVolumeControl to VolumeSlider.
     */
    static int getMultiplier()
    {
        return MULTIPLIER;
    }

    /**
     * Initialize the UI.
     */
    private void initializeUI()
    {
        switch (mVolumeControl.getMode())
        {
        case INPUT:
            mQuietResource = "service.gui.soundlevel.VOLUME_INPUT_MIN";
            mLoudResource = "service.gui.soundlevel.VOLUME_INPUT_MAX";
            break;
        case OUTPUT:
        default:
            mQuietResource = "service.gui.soundlevel.VOLUME_OUTPUT_MIN";
            mLoudResource = "service.gui.soundlevel.VOLUME_OUTPUT_MAX";
            break;
        }

        mLeftLabel = getLabel(mQuietResource);
        mLeftLabel.addMouseListener(this);

        mRightLabel = getLabel(mLoudResource);
        mRightLabel.addMouseListener(this);

        mSlider = new JSlider(JSlider.HORIZONTAL);
        mSlider.setMinimum((int) (mVolumeControl.getMinValue()*MULTIPLIER));
        mSlider.setMaximum((int) (mVolumeControl.getMaxValue()*MULTIPLIER));
        mSlider.addChangeListener(this);
        mSlider.setOpaque(false);

        refresh();
        mSlider.setUI(new VolumeSliderUI(mSlider));
        setLayout(new BorderLayout());
        add(mLeftLabel, BorderLayout.LINE_START);
        add(mSlider, BorderLayout.CENTER);
        add(mRightLabel, BorderLayout.LINE_END);
    }

    /**
     * Returns the image label for either max or min volume.
     *
     * @param resourceString The resource string referring to either the loud
     *                       resource or quiet resource.
     * @return The corresponding image.
     */
    private JLabel getLabel(String resourceString)
    {
        return mResources.getImage(resourceString).addToLabel(new JLabel());
    }

    /**
     * Get the device volume and update the slider appropriately.
     */
    public void refresh()
    {
        mLastValue = Math.round(mVolumeControl.getVolume() * MULTIPLIER);
        mSlider.setValue(mLastValue);
    }

    /**
     * Called when slider is moved (by any means). Sets our working value for
     * the volume and notifies the SetVolumeThread of the change.
     *
     * Since this listener effects all the necessary changes to the
     * VolumeControl, etc. when the slider is moved, any changes to be made to
     * the volume inside this class which should also result in a change in the
     * slider should be made by way of calling setValue on the slider and not
     * by calling setVolume on the thread.
     */
    @Override
    public void stateChanged(ChangeEvent e)
    {
        DesktopUtilActivator.getThreadingService()
                         .submit("VolumeSlider.stateChanged " +
                                 mVolumeControl.getMode(),
                                 setVolumeRunner);
    }

    /**
     * Called when the level of the volume control changes (for example, if
     * another slider attached to the same volume control is changed). Sets
     * this slider appropriately.
     */
    @Override
    public void volumeChange(VolumeChangeEvent e)
    {
        final int newValue = Math.round(e.getLevel() * MULTIPLIER);

        if (newValue != Math.round(mVolume * MULTIPLIER))
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    mSlider.setValue(newValue);
                }
            });
        }
    }

    /**
     * Moves the slider up and down when the icons either side of the slider
     * are clicked.
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (e.getSource() == mLeftLabel)
        {
            mSlider.setValue(mSlider.getValue() - VOLUME_INCREMENT);
        }
        else
        {
            mSlider.setValue(mSlider.getValue() + VOLUME_INCREMENT);
        }
    }

    @Override
    public void mousePressed(MouseEvent e){}

    @Override
    public void mouseReleased(MouseEvent e){}

    @Override
    public void mouseEntered(MouseEvent e){}

    @Override
    public void mouseExited(MouseEvent e){}

    /**
     * A custom implementation of BasicSliderUI, overriding the thumb and
     * track painting methods.
     *
     * This was added because the default WindowsSliderUI doesn't handle scaling.
     * We can't extend WindowsSliderUI because it isn't exported by swing.
     */
    private final class VolumeSliderUI extends BasicSliderUI
    {
        private final BufferedImageFuture sliderThumb = DesktopUtilActivator.getResources()
                .getBufferedImage("service.gui.soundlevel.THUMB");

        private final int trackThickness = ScaleUtils.scaleInt(2);

        private VolumeSliderUI(JSlider slider)
        {
            super(slider);
        }

        @Override
        protected void calculateThumbSize()
        {
            // We have a custom thumb so must make sure that its size is
            // calculated correctly.
            Image thumb = (sliderThumb).resolve();
            thumbRect.setSize(thumb.getWidth(null), thumb.getHeight(null));
        }

        @Override
        public void paintThumb(Graphics g)
        {
            Image thumb = (sliderThumb).resolve();
            g.drawImage(thumb,
                        thumbRect.x,
                        thumbRect.y,
                        thumb.getWidth(null),
                        thumb.getHeight(null),
                        null);
        }

        @Override
        public void paintTrack(Graphics g)
        {
            Rectangle trackBounds = this.trackRect;
            int cy = (trackBounds.height - trackThickness) / 2;
            int cw = trackBounds.width;
            g.translate(trackBounds.x, trackBounds.y + cy);
            g.setColor(Color.lightGray);
            g.fillRect(0, 0, cw, trackThickness);
            g.translate(-trackBounds.x, -(trackBounds.y + cy));
        }
    }
}
