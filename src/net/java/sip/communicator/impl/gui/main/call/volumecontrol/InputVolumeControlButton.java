/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.volumecontrol;

import java.beans.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The button which controls the input volume
 */
public class InputVolumeControlButton extends AbstractVolumeControlButton
                                     implements PropertyChangeListener, Runnable
{
    private static final long serialVersionUID = 1L;

    /**
     * Thread which sets whether we are muted or not
     */
    private Thread mMuteRunner;

    public InputVolumeControlButton(CallConference conference)
    {
        super(conference,
              ImageLoaderService.MICROPHONE,
              ImageLoaderService.MUTE_BUTTON,
              GuiActivator.getMediaService().getCaptureVolumeControl(),
              "service.gui.soundlevel.INPUT_BACKGROUND",
              "service.gui.soundlevel.INPUT_BACKGROUND_MUTED",
              "service.gui.MICROPHONE_BUTTON_TOOL_TIP",
              "service.gui.accessibility.MICROPHONE_VOLUME");
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(CallPeer.MUTE_PROPERTY_NAME))
        {
            setMute((Boolean) evt.getNewValue());
        }
    }

    public void setMute(boolean isMute)
    {
        super.setMute(isMute);

        if (mMuteRunner == null)
        {
            // Toggle the Mute state in a new thread to prevent blocking the EDT
            mMuteRunner = new Thread(this, "InputVolumeControlMuteRunner");
            mMuteRunner.setDaemon(true);
            mMuteRunner.start();
        }
    }

    public void run()
    {
        try
        {
            if (conference != null)
            {
                // Set mute status for all participants in the call.
                for (Call call : conference.getCalls())
                {
                    OperationSetBasicTelephony<?> telephony = call
                        .getProtocolProvider()
                        .getOperationSet(OperationSetBasicTelephony.class);
                    telephony.setMute(call, mMute);
                }

                // We make sure that the button state corresponds to the mute state.
                setSelected(mMute);

                // If we unmute the microphone and the volume control is set to
                // min, make sure that the volume control is restored to the
                // initial state.
                if (!mMute &&
                    mVolumeControl.getVolume() == mVolumeControl.getMinValue())
                {
                    mVolumeControl.setVolume(
                        (mVolumeControl.getMaxValue()
                            + mVolumeControl.getMinValue())/2);
                }
            }
        }
        finally
        {
            synchronized (this)
            {
                if (Thread.currentThread().equals(mMuteRunner))
                {
                    mMuteRunner = null;
                    setEnabled(true);
                }
            }
        }
    }

    @Override
    public String getName()
    {
        return "Microphone";
    }
}
