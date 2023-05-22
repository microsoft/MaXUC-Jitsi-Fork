/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call.volumecontrol;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;
import javax.swing.plaf.basic.*;

import org.jitsi.service.neomedia.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Represents an UI means to mute the audio stream sent in an associated
 * <tt>Call</tt>.
 */
public abstract class AbstractVolumeControlButton extends InCallButton
                                                  implements FocusListener
{
    private static final long serialVersionUID = 0L;

    // Offsets to make sure that the slider appears in the right place - i.e.
    // just on top of the button
    private static final int Y_OFFSET = -2;
    private static final int X_OFFSET = -1;

    /**
     * Our class logger.
     */
    private static final Logger sLogger
        = Logger.getLogger(AbstractVolumeControlButton.class);

    /**
     * Our volume control.
     */
    protected final VolumeControl mVolumeControl;

    /**
     * Resource for the background of the slider
     */
    private final String mSliderBackgroundRes;

    /**
     * Resource for the background of the slider when muted
     */
    private final String mSliderMutedBackgroundRes;

    /**
     * Resource for the accessibility description of the current volume level
     */
    private final String mVolumeAccessibilityRes;

    /**
     * Current mute state.
     */
    protected boolean mMute = false;

    /**
     * The volume slider that we are showing
     */
    private VolumeSliderPopup mSlider;

    /**
     * The conference whose volume to control
     */
    protected CallConference conference;

    /**
     * Timer used to control the visibility of the slider on mouse-over.
     */
    private final Timer mTimer = new Timer("Volume slider popup timer");

    /**
     * Create a new volume control button to control the volume of a call
     *
     * @param conference The conference whose volume to control
     * @param iconImageID The ID of the icon of this button
     * @param pressedIconImageID The ID of the pressed icon of this button
     * @param volumeControl The object whose volume we are controlling
     * @param bgImageRes Resource for the background of the slider
     * @param bgImageMutedRes Resource for the background of the slider when muted
     * @param tooltipRes Resource for the tooltip
     * @param volumeAccessibilityRes  Resource for the volume description
     */
    public AbstractVolumeControlButton(CallConference conference,
                                       ImageID iconImageID,
                                       ImageID pressedIconImageID,
                                       VolumeControl volumeControl,
                                       String bgImageRes,
                                       String bgImageMutedRes,
                                       String tooltipRes,
                                       String volumeAccessibilityRes)
    {
        super(iconImageID,
              iconImageID,
              null,
              pressedIconImageID,
              pressedIconImageID,
              pressedIconImageID,
              null,
              null,
              GuiActivator.getResources().getI18NString(tooltipRes));

        addFocusListener(this);

        initKeys();

        this.conference = conference;

        mSliderBackgroundRes      = bgImageRes;
        mSliderMutedBackgroundRes = bgImageMutedRes;
        mVolumeAccessibilityRes   = volumeAccessibilityRes;

        // We never want to start a call muted - users don't remember that they
        // muted a call last time so don't expect to start muted.
        mMute = false;
        mVolumeControl = volumeControl;

        // If the system volume is very low (i.e. less than 1%) then reset to
        // not muted.
        // This may block, so dispatch on a new thread.
        new Thread("ResetVolumeThread")
        {
            @Override
            public void run()
            {
                sLogger.debug("Entering ResetVolumeThread");
                float minValue = mVolumeControl.getMinValue();
                float maxValue = mVolumeControl.getMaxValue();

                if (mVolumeControl.getMute() ||
                    volumeVeryQuiet(minValue, maxValue))
                {
                    // After unmuting - the volume resets to half way between
                    // min and max
                    sLogger.debug("Resetting volume level to half way");
                    mVolumeControl.setVolume((minValue + maxValue)/2);
                    mVolumeControl.setMute(false);
                }
                sLogger.debug("Exiting ResetVolumeThread");
            }
        }.start();

        // Add a mouse listener so that we can display the slider pop-up if the
        // user hovers over this button for 150ms.
        addMouseListener(new MouseAdapter()
        {
            private TimerTask task = null;

            @Override
            public void mouseEntered(MouseEvent e)
            {
                task = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                showSlider();
                            }
                        });
                    }
                };

                mTimer.schedule(task, 150);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (task != null)
                    task.cancel();
            }
        });
    }

    /**
     * Show the slider, creating it if necessary
     */
    private void showSlider()
    {
        if (mSlider == null)
        {
            mSlider = new VolumeSliderPopup(mVolumeControl,
                                            this,
                                            mSliderBackgroundRes,
                                            mSliderMutedBackgroundRes);
        }

        // Ensure the accessible name refers to the button, rather than to
        // the value which it holds when being changed
        AccessibilityUtils.setName(this, getToolTipText());

        Point location = calcSliderLocation();
        SwingUtilities.convertPointToScreen(location, getParent());
        mSlider.setVisible(true);
        mSlider.setLocation(location);
    }

    /**
     * Hide the slider
     */
    private void hideSlider()
    {
        mSlider.setVisible(false);
    }

    /**
     * @return the location to display the slider
     */
    private Point calcSliderLocation()
    {
        Dimension preferredSize = mSlider.getPreferredSize();
        int x = getX() + getWidth()  - preferredSize.width  - X_OFFSET;
        int y = getY() + getHeight() - preferredSize.height - Y_OFFSET;

        return new Point(x, y);
    }

    /**
     * Mutes or unmutes the associated <tt>Call</tt> upon clicking this button.
     *
     * @param isMute whether to mute or unmute the call.
     */
    public void setMute(boolean isMute)
    {
        sLogger.debug("setMute: " + isMute);

        if (mMute == isMute)
        {
            sLogger.debug("Ignore duplicate mute instruction.");
            return;
        }

        mMute = isMute;
        setSelected(mMute);
        mVolumeControl.setMute(mMute);

        if (mSlider != null && mSlider.isVisible())
        {
            float minVolume = mVolumeControl.getMinValue();
            float maxVolume = mVolumeControl.getMaxValue();

            if (!mMute && volumeVeryQuiet(minVolume, maxVolume))
            {
                // No longer muted, but were very quiet, set to average of min and max
                mSlider.setLevel((minVolume + maxVolume)/2);
            }

            mSlider.repaint();
        }
    }

    /**
     * Initialize keyboard shortcuts for moving the slider up and down
     */
    private void initKeys()
    {
        InputMap imap = getInputMap();
        ActionMap amap = getActionMap();

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "volumeUp");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "volumeDown");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleMute");

        amap.put("volumeUp", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public void actionPerformed(ActionEvent evt)
            {
                mSlider.moveSlider(BasicSliderUI.POSITIVE_SCROLL);
                setAccessibilityValue();
            }
        });

        amap.put("volumeDown", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public void actionPerformed(ActionEvent evt)
            {
                mSlider.moveSlider(BasicSliderUI.NEGATIVE_SCROLL);
                setAccessibilityValue();
            }
        });

        amap.put("toggleMute", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public void actionPerformed(ActionEvent evt)
            {
                setMute(!mMute);
            }
        });
    }

    /**
     * Sets the accessibility name so that the current slider value is read out
     */
    private void setAccessibilityValue()
    {
        AccessibilityUtils.setName(AbstractVolumeControlButton.this,
                                   GuiActivator.getResources().getI18NString(mVolumeAccessibilityRes,
                                                                             new String[] {Integer.toString(mSlider.getSliderValue())}));
    }

    @Override
    public void focusGained(FocusEvent evt)
    {
        showSlider();
    }

    @Override
    public void focusLost(FocusEvent evt)
    {
        hideSlider();
    }

    /**
     * Called when this component is no longer required
     */
    public void dispose()
    {
        if (mSlider != null)
            mSlider.dispose();

        mTimer.cancel();
    }

    /**
     * Checks the volume control to see if the current volume is very low - i.e.
     * almost muted
     *
     * @param min The minimum value
     * @param max The maximum value
     * @return true if the current value is very close to the minimum value
     */
    private boolean volumeVeryQuiet(float min, float max)
    {
        // "Very quiet" means we are less than 1% of the range above the min.
        return (mVolumeControl.getVolume() - min) < (0.01f * (max - min));
    }
}
