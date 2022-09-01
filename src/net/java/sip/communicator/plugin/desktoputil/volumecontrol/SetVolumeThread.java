/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.volumecontrol;

import org.jitsi.service.neomedia.*;

/**
 * A dedicated thread for setting the volume - prevent blocking the EDT
 */
public class SetVolumeThread extends Thread
{
    /**
     * A boolean set to true if the thread must continue to loop.
     */
    private boolean shouldRun;

    /**
     * The VolumeControl that dows the actual volume adjusting.
     */
    private final VolumeControl volumeControl;

    /**
     * The volume wished by the UI.
     */
    private float volume;

    /**
     * The volume currently set.
     */
    private float lastVolumeSet;

    /**
     * Create a dedicate thread to set the volume.
     *
     * @param volumeControl The VolumeControl that does the actual volume
     *            adjusting.
     */
    public SetVolumeThread(final VolumeControl volumeControl)
    {
        super("VolumeControlSlider: VolumeControl.setVolume");

        this.shouldRun = true;
        this.volumeControl = volumeControl;
        this.lastVolumeSet = volumeControl.getVolume();
        this.volume = this.lastVolumeSet;
    }

    /**
     * Updates and sets the volume if changed.
     */
    public void run()
    {
        while (this.shouldRun)
        {
            synchronized (this)
            {
                // Wait if there is no update yet.
                if (volume == lastVolumeSet)
                {
                    try
                    {
                        this.wait();
                    }
                    catch (InterruptedException iex)
                    {
                    }
                }
                lastVolumeSet = volume;
            }

            // Set the volume to the volume control.
            volumeControl.setVolume(lastVolumeSet);
        }
    }

    /**
     * Sets a new volume value.
     *
     * @param newVolume The new volume to set.
     */
    public void setVolume(float newVolume)
    {
        synchronized (this)
        {
            volume = newVolume;

            // If there is a change, then wake up the thread loop..
            if (volume != lastVolumeSet)
            {
                this.notify();
            }
        }
    }

    /**
     * Ends the thread loop.
     */
    public void end()
    {
        synchronized (this)
        {
            this.shouldRun = false;
            this.notify();
        }
    }
}