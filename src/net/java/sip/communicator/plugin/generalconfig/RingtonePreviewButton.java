// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.service.resources.*;

/**
 * The RingtonePreviewButton is used to play (and stop) a preview of the
 * currently selected ringtone.
 */
public class RingtonePreviewButton extends SIPCommButton
                                   implements AudioListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The local <tt>Logger</tt>
     */
    private static final Logger sLog
            = Logger.getLogger(RingtonePreviewButton.class);

    /**
     * The resource key for the 'Play' icon used when no audio is playing
     * (i.e. the button is in an unpressed state), and when the button is not
     * being moused-over.
     */
    private static final String PLAY_NEUTRAL_ICON_KEY =
        "plugin.generalconfig.PLAY_NEUTRAL_ICON";

    /**
     * The resource key for the 'Play' icon used when no audio is playing
     * (i.e. the button is in an unpressed state), and when the button is being
     * moused-over.
     */
    private static final String PLAY_ROLLOVER_ICON_KEY =
        "plugin.generalconfig.PLAY_ROLLOVER_ICON";

    /**
     * The resource key for the 'Stop' icon used when audio is playing
     * (i.e. the button is in a pressed state), and when the button is not
     * being moused-over.
     */
    private static final String STOP_NEUTRAL_ICON_KEY =
        "plugin.generalconfig.STOP_NEUTRAL_ICON";

    /**
     * The resource key for the 'Stop' icon used when audio is playing
     * (i.e. the button is in a pressed state), and when the button is being
     * moused-over.
     */
    private static final String STOP_ROLLOVER_ICON_KEY =
        "plugin.generalconfig.STOP_ROLLOVER_ICON";

    /**
     * The 'Play' icon which is used when no audio is playing (i.e. the button
     * is in an unpressed state), and when the button is being moused-over.
     */
    private static final BufferedImageFuture PLAY_BUTTON_NEUTRAL_ICON =
        Resources.getBufferedImage(PLAY_NEUTRAL_ICON_KEY);

    /**
     * The 'Play' icon which is used when no audio is playing (i.e. the button
     * is in an unpressed state), and when the button is being moused-over.
     */
    private static final BufferedImageFuture PLAY_BUTTON_ROLLOVER_ICON =
        Resources.getBufferedImage(PLAY_ROLLOVER_ICON_KEY);

    /**
     * The 'Stop' icon used when audio is playing (i.e. the button is in a
     * pressed state), and when the button is not being moused-over.
     */
    private static final BufferedImageFuture STOP_BUTTON_NEUTRAL_ICON =
        Resources.getBufferedImage(STOP_NEUTRAL_ICON_KEY);

    /**
     * The 'Stop' icon used when audio is playing (i.e. the button is in a
     * pressed state), and when the button is being moused-over.
     */
    private static final BufferedImageFuture STOP_BUTTON_ROLLOVER_ICON =
        Resources.getBufferedImage(STOP_ROLLOVER_ICON_KEY);

    /**
     * The hover text used when the button is in an 'unselected' state, i.e.
     * when no audio is playing.
     */
    private final String unselectedStateHoverText =
        Resources.getResources().getI18NString("service.gui.PREVIEW_RINGTONE");

    /**
     * The hover text used when the button is in a 'selected' state, i.e. when
     * audio is currently playing.
     */
    private final String selectedStateHoverText =
             Resources.getResources().getI18NString("service.gui.STOP_PREVIEW");

    /**
     * Holds the toggle state of the button. This is necessary because when we
     * receive a click event we need to see if the state has actually changed
     * (if another UI element has focus, clicking the button just gives the
     * button focus).
     */
    private boolean isSelected = false;

    /**
     * The ringtone preview sound that is currently playing.
     */
    private SCAudioClip nowPlaying = null;

    /**
     * A reference to the ringtone selector combo box, so that we can ascertain
     * the currently selected ringtone.
     * It is not possible to simply query the notification service for the
     * current ringtone, because there is a noticeable delay between selecting a
     * ringtone in the dropdown and the notification service having finished
     * registering the file.
     */
    private RingtoneSelectorComboBox ringtoneSelector;

    public RingtonePreviewButton()
    {
        super(PLAY_BUTTON_NEUTRAL_ICON);

        AccessibilityUtils.setName(this, unselectedStateHoverText);

        setToolTipText(unselectedStateHoverText);
        addActionListener(new PreviewButtonStateListener(this));
    }

    /**
     * Halt playback of any ringtone preview that is currently playing.
     * Does nothing if there is no currently-playing preview.
     */
    public void stopAudio()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                deselectButton();
            }
        });

        if (nowPlaying != null)
        {
            nowPlaying.removeAudioListener(this);
            nowPlaying.stop();
        }
    }

    /**
     * Passes the RingtoneSelectorComboBox to this class, so that we can
     * subsequently determine the current ringtone.
     * This cannot be passed in the constructor, because the ringtone selector
     * also takes a reference to this preview button.
     *
     * @param ringtoneSelector The ringtone selector to store.
     */
    public void registerRingtoneSelector(
                                      RingtoneSelectorComboBox ringtoneSelector)
    {
        this.ringtoneSelector = ringtoneSelector;
    }

    /**
     * This method is executed when the audio begins playing.
     * We don't need to do anything to handle this.
     */
    public void onClipStarted()
    {
        // Do nothing.
    }

    /**
     * This method is executed when the current audio clip ceases playing.
     * We then revert to an 'unselected' button state.
     */
    public void onClipEnded()
    {
        // The clip has ended, so revert to the default button state.
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                nowPlaying = null;
                deselectButton();
            }
        });
    }

    /**
     * Sets the button to deselected state, including hover text and internal
     * state variables.
     */
    private void deselectButton()
    {
        setIconImage(PLAY_BUTTON_NEUTRAL_ICON);
        setRolloverIcon(PLAY_BUTTON_ROLLOVER_ICON);
        setToolTipText(unselectedStateHoverText);
        isSelected = false;
        repaint();
    }

    /**
     * Listener used to detect and handle click events on this button.
     */
    private class PreviewButtonStateListener implements ActionListener
    {
        public PreviewButtonStateListener(RingtonePreviewButton previewButton)
        {
        }

        public void actionPerformed(ActionEvent event)
        {
            sLog.user("Ringtone preview button clicked");
            isSelected = !isSelected;

            /* Fire a state property change event as the button's state has changed */
            if (isSelected)
            {
                // The button has just become selected, so we play a preview for
                // the current ringtone.

                if (ringtoneSelector == null)
                {
                    // The ringtone selector hasn't been registered yet, so
                    // we can't play a preview. Revert the button state and
                    // abort.
                    deselectButton();
                    return;
                }

                setIconImage(STOP_BUTTON_NEUTRAL_ICON);
                setRolloverIcon(STOP_BUTTON_ROLLOVER_ICON);
                invalidate();

                RingtonePreviewButton.this.setToolTipText(
                                                        selectedStateHoverText);

                if (nowPlaying != null)
                {
                    // If the button is clicked rapidly, this event may occur
                    // before the previous preview was stopped.
                    // So we ensure here that prior audio is definitely not
                    // playing, and that we are not listening for the end of the
                    // clip anymore.
                    nowPlaying.removeAudioListener(RingtonePreviewButton.this);
                    nowPlaying.stop();
                }

                // Obtain a URI for the currently selected ringtone.
                String path = ringtoneSelector.getCurrentPath();

                AudioNotifierService audioNotifServ
                       = GeneralConfigPluginActivator.getAudioNotifierService();
                nowPlaying = audioNotifServ.createAudio(path);

                if (nowPlaying != null)
                {
                    // Listen for when playback stops, so we can then revert the
                    // button state.
                    nowPlaying.registerAudioListener(
                                                    RingtonePreviewButton.this);

                    // Force the notification to play even if muted
                    nowPlaying.playEvenIfMuted();
                }
            }
            else
            {
                // The button has just become deselected. Stop the audio and
                // revert the button state.
                stopAudio();
            }
        }
    }
}
