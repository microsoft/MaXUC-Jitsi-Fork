// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.neomedia;

import javax.swing.*;

import org.jitsi.service.audionotifier.*;

/**
 * An implementation of AudioListener that disables a linked button during playback.
 */

public class AudioPlaybackListener
    implements AudioListener
{
    /**
     * The JButton linked to the audio clip.
     */
    protected JButton mButton;

    /**
     * Constructs an AudioListener with a linked button which displays a play
     * icon whilst not playing and a stop icon whilst playing.
     *
     * @param targetButton The button to be linked to the audio listener.
     */
    public AudioPlaybackListener(JButton targetButton)
    {
        mButton = targetButton;
    }

    public void onClipStarted()
    {
        NeomediaActivator.getResources().getBufferedImage(
            "plugin.notificationconfig.STOP_ICON").getImageIcon().
            addToButton(mButton);
    }

    public void onClipEnded()
    {
        NeomediaActivator.getResources().getBufferedImage(
            "plugin.notificationconfig.PLAY_ICON").getImageIcon().
            addToButton(mButton);
    }
}
