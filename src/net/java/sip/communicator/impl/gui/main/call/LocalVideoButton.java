/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a button which starts/stops the streaming of the local video in a
 * <tt>Call</tt> to the remote peer(s).
 */
public class LocalVideoButton
    extends InCallDisableButton
    implements ActionListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>LocalVideoButton</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(LocalVideoButton.class);

    /**
     * Whether this button is permanently disabled.
     */
    private boolean permanentlyDisabled = true;

    /**
     * Lock used during asynchronous setup of the button.
     * Should be held when accessing desiredEnabledState or buttonSetupComplete.
     */
    private Object setupLock = new Object();

    /**
     * Whether setup of the button has finished.
     */
    private boolean buttonSetupComplete = false;

    /**
     * If setEnabled is called during button setup, the call is ignored, but the
     * desired 'enabled' state is stored here so that we can apply the desired
     * enabled state after setup is complete.
     */
    private volatile boolean desiredEnabledState = true;

    /**
     * The call conference that this button controls
     */
    private CallConference callConference;

    /**
     * The call frame that this button belongs to
     */
    private CallFrame parentFrame;

    /**
     * Initializes a new <tt>LocalVideoButton</tt> instance which is to
     * start/stop the streaming of the local video to the remote peers
     * participating in a specific telephony conference.
     *
     * @param conference the <tt>CallConference</tt> to start/stop the streaming
     * of the local video to the remote peers in
     * @param parentFrame the Call Frame that this button belongs to
     */
    public LocalVideoButton(CallConference conference, CallFrame parentFrame)
    {
        super(ImageLoaderService.LOCAL_VIDEO_BUTTON,
              ImageLoaderService.LOCAL_VIDEO_BUTTON_ROLLOVER,
              ImageLoaderService.LOCAL_VIDEO_BUTTON_FOCUS,
              ImageLoaderService.LOCAL_VIDEO_BUTTON_PRESSED,
              ImageLoaderService.LOCAL_VIDEO_BUTTON_ON,
              ImageLoaderService.LOCAL_VIDEO_BUTTON_ON_ROLLOVER,
              ImageLoaderService.LOCAL_VIDEO_BUTTON_ON_FOCUS,
              ImageLoaderService.LOCAL_VIDEO_BUTTON_DISABLED,
              GuiActivator.getResources().getI18NString("service.gui.LOCAL_VIDEO_BUTTON_TOOL_TIP"));

        this.parentFrame = parentFrame;

        callConference = conference;

        // Pre-disable the button until our (asynchronous) setup is complete.
        // We don't want the user to be able to click the button until we're
        // certain it should be enabled.
        super.setEnabled(false);

        // Check to see if a video device is available - if not, the
        // video button will be disabled.
        maybeEnableVideoButton();

        addActionListener(this);
    }

    /**
     * Search for a video device to use. If we find one, allow the video button
     * to be enabled. Otherwise, the video button remains disabled.
     */
    protected void maybeEnableVideoButton()
    {
        // This is blocking, so we dispatch on a new Runnable.
        Runnable videoButtonEnabler = new Runnable()
        {
            @Override
            public void run()
            {
                logger.info("Starting VideoButtonEnabler runnable");
                List<MediaDevice> videoDevices = GuiActivator.getMediaService().
                    getDevices(MediaType.VIDEO, MediaUseCase.CALL);

                for (MediaDevice videoDevice : videoDevices)
                {
                    if (videoDevice.getFormat() != null)
                    {
                        logger.debug("Video device found, don't disable the video button");
                        permanentlyDisabled = false;
                        break;
                    }
                }

                // 'setEnabled' might have been called while we were checking
                // devices. These calls will not have had any effect, so call setEnabled
                // back to make sure the 'enabled' state is up-to-date.
                synchronized (setupLock)
                {
                    buttonSetupComplete = true;
                    setEnabled(desiredEnabledState);
                }

                logger.info("VideoButtonEnabler runnable ending.");

            }
        };

        logger.debug("Scheduling video button updater");
        GuiActivator.getThreadingService().submit("VideoButtonEnabler",
                                                  videoButtonEnabler);
    }

    /**
     * Enables/disables the button. If <tt>this.videoAvailable</tt> is false,
     * keeps the button as it is (i.e. disabled).
     *
     * @param enabled <tt>true</tt> to enable the button, <tt>false</tt> to
     * disable it.
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        // Always use invokeLater to schedule setting the enabled state of the
        // button on the EDT thread.  Otherwise, if we delay setting the button
        // when setup is not yet complete, we may end up undoing a later call
        // to this method that is done after setup is complete.
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                synchronized (setupLock)
                {
                    if (!buttonSetupComplete)
                    {
                        // Button setup is still in progress, so store off the
                        // desired button state; we'll get called back later
                        // when setup is done.
                        logger.info(
                            "Asked to change video button state to " + enabled +
                            " before the button was ready. Deferring.");

                        desiredEnabledState = enabled;
                        return;
                    }
                }

                logger.debug("Setting video button enabled state to: " + enabled);

                /*
                 * Regardless of what CallPanel tells us about the enabled
                 * state of this LocalVideoButton, we have to analyze the state
                 * ourselves because we have to update the tool tip and take
                 * into account the global state of the application.
                 */
                MediaDevice videoDevice
                    = GuiActivator.getMediaService().getDefaultDevice(
                            MediaType.VIDEO,
                            MediaUseCase.CALL);
                String toolTipTextKey;
                boolean videoAvailable;

                /*
                 *  Check whether we can send video and set the appropriate tool
                 *  tip.
                 */
                if (permanentlyDisabled || videoDevice == null ||
                    !videoDevice.getDirection().allowsSending())
                {
                    toolTipTextKey = "service.gui.NO_CAMERA_AVAILABLE";
                    videoAvailable = false;
                }
                else
                {
                    boolean hasVideoTelephony = false;
                    boolean hasEnabledVideoFormat = false;

                    for (Call call : callConference.getCalls())
                    {
                        ProtocolProviderService protocolProvider
                            = call.getProtocolProvider();

                        if (!hasVideoTelephony)
                        {
                            OperationSetVideoTelephony videoTelephony
                                = protocolProvider.getOperationSet(
                                        OperationSetVideoTelephony.class);

                            if (videoTelephony != null)
                                hasVideoTelephony = true;
                        }
                        if (!hasEnabledVideoFormat
                                && ConfigurationUtils.hasEnabledVideoFormat(
                                        protocolProvider))
                        {
                            hasEnabledVideoFormat = true;
                        }

                        if (hasVideoTelephony && hasEnabledVideoFormat)
                            break;
                    }

                    if (!hasVideoTelephony)
                    {
                        toolTipTextKey = "service.gui.NO_VIDEO_FOR_PROTOCOL";
                        videoAvailable = false;
                    }
                    else if(!hasEnabledVideoFormat)
                    {
                        toolTipTextKey = "service.gui.NO_VIDEO_ENCODINGS";
                        videoAvailable = false;
                    }
                    else
                    {
                        toolTipTextKey = "service.gui.LOCAL_VIDEO_BUTTON_TOOL_TIP";
                        videoAvailable = true;
                    }
                }
                setToolTipText(
                        GuiActivator.getResources().getI18NString(toolTipTextKey));

                boolean isEnabled = (videoAvailable && enabled);
                LocalVideoButton.super.setEnabled(isEnabled);

                if (!isEnabled && isSelected())
                {
                    logger.debug(
                        "Disabling video button - make sure it isn't selected");
                    setSelected(false);
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        // If the button has been recently clicked, ignore this click.
        if (!shouldHandleAction())
        {
            logger.info("Ignoring button click as too recently pressed");
            return;
        }

        logger.user("Video button clicked in call window");

        /*
         * CallManager actually enables/disables the local video for the
         * telephony conference associated with the Call so pick up a Call
         * participating in callConference and it should do.
         */
        List<Call> calls = callConference.getCalls();

        boolean selected = !isSelected();
        setSelected(selected);

        if (!calls.isEmpty())
        {
            Call call = calls.get(0);

            CallManager.enableLocalVideo(
                    call,
                    selected);
        }
    }

    @Override
    public void setSelected(boolean selected)
    {
        logger.debug("Setting selected state of the local video button. Selected: " + selected);

        super.setSelected(selected);

        if (!selected)
        {
            // If we have disabled video we should remove the video component
            // in the call frame immediately. Otherwise it would take a couple
            // of seconds for the Call Frame to receive the video event, which
            // results in poor UX.
            parentFrame.removeVideoComponent(true);
        }
    }
}
