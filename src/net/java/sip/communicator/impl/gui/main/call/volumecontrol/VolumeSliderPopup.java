/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.volumecontrol;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.volumecontrol.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.resources.*;

/**
 * A class for displaying a volume slider inline (i.e. with the rest of the call
 * control buttons) on a call panel.
 */
class VolumeSliderPopup extends JWindow
{
    private static final long serialVersionUID = 1L;

    /**
     * The logger for this class
     */
    private static final Logger sLog = Logger.getLogger(VolumeSliderPopup.class);

    /**
     * The multiplier would just convert the float volume value coming from
     * the service to the int value needed for the volume control slider
     * component.
     */
    private static final int MULTIPLIER = 100;

    /**
     * The image to use for the thumb when the control is not at the minimum
     * value
     */
    private static final BufferedImageFuture sliderThumb = GuiActivator.getResources()
                                       .getBufferedImage("service.gui.soundlevel.THUMB");

    /**
     * The image to use for the thumb when the control is at the minimum value
     */
    private static final BufferedImageFuture sliderThumbMuted = GuiActivator.getResources()
                                 .getBufferedImage("service.gui.soundlevel.THUMB_MUTED");

    /**
     * Image used for the background when not muted
     */
    private final BufferedImageFuture mBackgroundImage;

    /**
     * Image used for the background when muted
     */
    private final BufferedImageFuture mBackgroundMutedImage;

    /**
     * The slider component.
     */
    private final JSlider mVolumeSlider;

    /**
     * The dedicated thread to set the volume.
     */
    private final SetVolumeThread mSetVolumeThread;

    /**
     * The volume control that this slider shows and controls
     */
    private final VolumeControl mVolumeControl;

    /**
     * The button that this slider is overlaying.
     */
    private final AbstractVolumeControlButton mButton;

    /**
     * The UI for this slider
     */
    private VolumeSliderUI mVolumeSliderUI;

    /**
     *  Last time volume change was logged, or zero if first time through
     *  (if test will always succeed first time).
     *  Used to ensure logs are not unnecessarily duplicated.
     */
    private long sliderTime;

    /**
     * Constructor
     *
     * @param control The volume control object that we are controlling
     * @param button  The button responsible for this volume slider
     * @param bgImageRes The resource for the background of this slider
     * @param bgMutedImageRes The resource for the background when muted
     */
    public VolumeSliderPopup(VolumeControl control,
                             AbstractVolumeControlButton button,
                             String bgImageRes,
                             String bgMutedImageRes)
    {
        // Mark this window as a child of the window containing the button so
        // that it appears on top of the call panel
        super(SwingUtilities.getWindowAncestor(button));
        mButton = button;
        mVolumeControl = control;
        setFocusable(false);

        ResourceManagementService res = GuiActivator.getResources();
        mBackgroundImage      = res.getBufferedImage(bgImageRes);
        mBackgroundMutedImage = res.getBufferedImage(bgMutedImageRes);

        mSetVolumeThread = new SetVolumeThread(mVolumeControl);
        mSetVolumeThread.start();

        mVolumeSlider = createVolumeSlider();

        mVolumeSlider.setFocusable(false);

        // Adds a change listener to the slider in order to correctly set
        // the volume through the VolumeControl service, on user change.
        mVolumeSlider.addChangeListener(new ChangeListener()
        {
            private int mLastVolume = mVolumeSlider.getMaximum();

            public void stateChanged(ChangeEvent e)
            {
                // Prevent logging of each step in the slider
                if (System.currentTimeMillis() - sliderTime > 5000)
                {
                    sliderTime = System.currentTimeMillis();
                    sLog.user(mButton.getName() + " slider changed in " +
                                      "call window");
                }
                JSlider source = (JSlider) e.getSource();
                int volume = source.getValue();
                mSetVolumeThread.setVolume((float) volume/MULTIPLIER);

                // Make sure the button is muted if the user moves the slider to
                // the bottom. Similarly unmute the button if it is not minimum.
                int minimum = mVolumeSlider.getMinimum();
                if ((mLastVolume == minimum || mButton.mMute) &&
                    volume != minimum)
                {
                    // Were muted, now are not
                    mButton.setMute(false);
                }
                else if (mLastVolume != minimum && volume == minimum)
                {
                    // Were not muted, now are
                    mButton.setMute(true);
                }

                mLastVolume = volume;
            }
        });

        // The slider is very much tied to the location and size of the parent.
        // If either of these change, then the slider should be hidden.
        SwingUtilities.getWindowAncestor(button)
                      .addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent event)
            {
                setVisible(false);
            }

            public void componentMoved(ComponentEvent event)
            {
                setVisible(false);
            }
        });

        add(mVolumeSlider);
        setMinimumSize(mVolumeSlider.getPreferredSize());
        this.setBackground(new Color(0,0,0,0));
    }

    /**
     * @return the custom slider which controls the volume.
     */
    private JSlider createVolumeSlider()
    {
        int minimum = (int) (mVolumeControl.getMinValue()*MULTIPLIER);
        int maximum = (int) (mVolumeControl.getMaxValue()*MULTIPLIER);
        int value = (int) (mVolumeControl.getVolume()*MULTIPLIER);
        final JSlider slider =
                         new JSlider(JSlider.VERTICAL, minimum, maximum, value);

        mVolumeSliderUI = new VolumeSliderUI(slider);
        slider.setUI(mVolumeSliderUI);
        slider.setOpaque(false);

        // Listen for mouse click events at the bottom of the slider
        slider.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                sLog.user(mButton.getName() + " mute toggled to: " +
                                  !mButton.mMute + " in call window");
                // The bottom of this pop-up is a square area that the user
                // can click on and that should perform the same action as the
                // underlying button.
                if (e.getPoint().y > getHeight() - getWidth())
                {
                    // User clicked at the bottom of the slider.  This is the
                    // part that overlaps the button, thus we should delegate
                    // to the button
                    mButton.setMute(!mButton.mMute);
                }
                else
                {
                    // The user clicked elsewhere - treat as a click in the
                    // scroll track.
                    mVolumeSliderUI.doScrollDueToClickInTrack();
                }
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                // Mouse exited slider so hide it.  Run later as mouse listeners
                // on other UI components (in particular, the InputVolumeControl
                // Button) will try to show the slider if it is hidden.
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        setVisible(false);
                    }
                });
            }
        });

        slider.setPreferredSize(new Dimension(mBackgroundImage.resolve().getWidth(null),
                                              mBackgroundImage.resolve().getHeight(null)));

        return slider;
    }

    /**
     * Moves the slider in the given direction
     *
     * @param direction the direction in which to move the slider
     */
    public void moveSlider(int direction)
    {
        mVolumeSliderUI.scrollByUnit(direction);
    }

    /**
     * Set the level of the volume control and make sure that the UI updates to
     * display the correct value:
     *
     * @param level the level to give the volume
     */
    public void setLevel(float level)
    {
        mSetVolumeThread.setVolume(level);
        mVolumeSlider.setValue((int) (level * MULTIPLIER));

        // Move the thumb to be in the right place.
        // x is simple - the x co-ordinate of the track rectangle
        // y is harder - the track y +
        //               the appropriate amount of the track height -
        //               half the height of the thumb
        int x = mVolumeSliderUI.mTrackRect.x;
        int y = mVolumeSliderUI.mTrackRect.y +
                (int)(mVolumeSliderUI.mTrackRect.height * level) -
                (mVolumeSliderUI.mThumbRect.height / 2);

        mVolumeSliderUI.setThumbLocation(x, y);
    }

    /**
     * Get the slider value
     * @return the value represented by the slider
     */
    public int getSliderValue()
    {
        return mVolumeSlider.getValue();
    }

    @Override
    public void dispose()
    {
        super.dispose();

        mSetVolumeThread.end();
    }

    /**
     * Class defining the UI of the volume slider
     */
    private final class VolumeSliderUI extends BasicSliderUI
    {
        /**
         * The last direction that the user wanted us to move when they clicked
         * on the track
         */
        private int mClickInTrackDirection;

        /**
         * The rectangle in which the thumb moves.
         */
        private Rectangle mTrackRect;

        /**
         * The rectangle containing the thumb
         */
        private Rectangle mThumbRect;

        private VolumeSliderUI(JSlider slider)
        {
            super(slider);
        }

        @Override
        protected TrackListener createTrackListener(JSlider slider)
        {
            // Override the default track listener since we don't want to always
            // pass click events to the track.  In fact, we only pass the events
            // on if they are actually over the track.
            return new TrackListener()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (eventOverTrack(e))
                        super.mouseClicked(e);
                }

                @Override
                public void mousePressed(MouseEvent e)
                {
                    if (eventOverTrack(e))
                        super.mousePressed(e);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    if (eventOverTrack(e))
                        super.mouseReleased(e);
                }

                /**
                 * Check to see if an event is over the track
                 *
                 * @param e The event to check
                 * @return true if the event is over the track.
                 */
                private boolean eventOverTrack(MouseEvent e)
                {
                    return e.getPoint().y < getHeight() - getWidth();
                }
            };
        }

        /**
         * @return true if the button is in the muted state.
         */
        private boolean isMuted()
        {
            // "Selected" means the button is muted.
            return mButton == null ? false : mButton.isSelected();
        }

        @Override
        protected void scrollDueToClickInTrack(int dir)
        {
            // Don't scroll when clicking in the track. The user might have
            // clicked at the bottom, where the slider overlaps the button.
            // Instead, remember the direction we should have moved so that
            // we can move in that direction later.
            mClickInTrackDirection = dir;
        }

        /**
         * Cause the thumb to be moved in the direction of the previous click in
         * the scroll track
         */
        public void doScrollDueToClickInTrack()
        {
            super.scrollDueToClickInTrack(mClickInTrackDirection);
        }

        @Override
        public void paint(Graphics g, JComponent c)
        {
            // Draw our own image on the background then call the super
            // class version - this ensures that the track, thumb and so on
            // get drawn.
            Image bg = (isMuted() ? mBackgroundMutedImage : mBackgroundImage).resolve();
            g.drawImage(bg, 0, 0, null);

            super.paint(g, c);
        }

        @Override
        public void paintThumb(Graphics g)
        {
            Image thumb = (isMuted() ? sliderThumbMuted : sliderThumb).resolve();
            g.drawImage(thumb,
                thumbRect.x,
                thumbRect.y,
                thumb.getWidth(null),
                thumb.getHeight(null),
                null);

            mThumbRect = thumbRect;
        }

        @Override
        protected void calculateThumbSize()
        {
            // We have a custom thumb so must make sure that its size is
            // calculated correctly.
            Image thumb = (isMuted() ? sliderThumbMuted : sliderThumb).resolve();
            thumbRect.setSize(thumb.getWidth(null), thumb.getHeight(null));
        }

        @Override
        public void paintTrack(Graphics g)
        {
            // No track required, the custom background image has it already
        }

        @Override
        public void paintFocus(Graphics g)
        {
            // Nothing required - don't want a focus
        }

        @Override
        protected void calculateTrackRect()
        {
            // Set the track rectangle to overlap the track in the image
            trackRect.x = ScaleUtils.scaleInt(3);
            trackRect.y = ScaleUtils.scaleInt(14);
            trackRect.width = thumbRect.width;
            trackRect.height = ScaleUtils.scaleInt(51);

            mTrackRect = trackRect;
        }
    }
}