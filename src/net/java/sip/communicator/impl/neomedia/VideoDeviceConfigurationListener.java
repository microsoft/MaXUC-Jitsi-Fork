/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.beans.*;
import java.util.*;

import javax.media.*;

import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a listener which responds to changes in the video device
 * configuration by displaying pop-up notifications summarizing the changes for
 * the user.
 */
public class VideoDeviceConfigurationListener
    extends AbstractDeviceConfigurationListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>VideoDeviceConfigurationListener</tt>
     * class for logging output.
     */
    private static final Logger sLog =
        Logger.getLogger(VideoDeviceConfigurationListener.class);

    /**
     * The last <tt>PropertyChangeEvent</tt> about a video device which has
     * been received.
     */
    private PropertyChangeEvent mVideoPropertyChangeEvent;

    /**
     * Creates a listener to the click on the popup message concerning video
     * device configuration changes.
     *
     * @param configurationForm The video configuration form.
     */
    public VideoDeviceConfigurationListener(
            ConfigurationForm configurationForm)
    {
        super(configurationForm);
    }

    /**
     * Notifies this instance that a property related to the configuration of
     * devices has had its value changed and thus signals that a video device
     * may have been plugged or unplugged.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which describes the name of the
     * property whose value has changed and the old and new values of that
     * property
     */
    public void propertyChange(PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();

        // The list of available video devices has changed.
        if (DeviceConfiguration.PROP_VIDEO_SYSTEM_DEVICES.equals(propertyName))
        {
            sLog.debug("Video Device List was: " +
                ev.getOldValue() + ", now: " + ev.getNewValue());

            @SuppressWarnings("unchecked")
            List<CaptureDeviceInfo> oldDevices
                = (List<CaptureDeviceInfo>) ev.getOldValue();
            @SuppressWarnings("unchecked")
            List<CaptureDeviceInfo> newDevices
                = (List<CaptureDeviceInfo>) ev.getNewValue();

            if (oldDevices.isEmpty())
            {
                oldDevices = null;
            }
            if (newDevices.isEmpty())
            {
                newDevices = null;
            }

            String title;
            ResourceManagementService r = NeomediaActivator.getResources();
            List<CaptureDeviceInfo> devices;
            boolean removal;

            // At least one new device has been connected.
            if (newDevices != null)
            {
                title
                    = r.getI18NString(
                            "impl.media.configform.AUDIO_DEVICE_CONNECTED");
                devices = newDevices;
                removal = false;
            }
            /*
             * At least one old device has been disconnected and no new device
             * has been connected.
             */
            else if (oldDevices != null)
            {
                title
                    = r.getI18NString(
                            "impl.media.configform.AUDIO_DEVICE_DISCONNECTED");
                devices = oldDevices;
                removal = true;
            }
            else
            {
                /*
                 * Neither a new device has been connected nor an old device has
                 * been disconnected. Why are we even here in the first place
                 * anyway?
                 */
                mVideoPropertyChangeEvent = null;
                return;
            }

            StringBuilder body = new StringBuilder();

            for (CaptureDeviceInfo device : devices)
            {
                body.append(device.getName()).append("\r\n");
            }

            DeviceConfiguration devConf = (DeviceConfiguration) ev.getSource();
            VideoSystem videoSystem = devConf.getVideoSystem();
            boolean selectedHasChanged = false;

            if (videoSystem != null)
            {
                if (mVideoPropertyChangeEvent != null)
                {
                    CaptureDeviceInfo cdi =
                        videoSystem.getAndRefreshSelectedDevice();

                    if ((cdi != null)
                            && !cdi.equals(
                                    mVideoPropertyChangeEvent.getOldValue()))
                    {
                        body.append("\r\n")
                            .append(
                                    r.getI18NString(
                                            "impl.media.configform"
                                                + ".VIDEO_DEVICE_SELECTED"))
                            .append("\r\n  ")
                            .append(cdi.getName());
                        selectedHasChanged = true;
                    }
                }
            }

            mVideoPropertyChangeEvent = null;

            /*
             * If an old device has been disconnected and no new device has been
             * connected, show a notification only if any selected device has
             * changed.  Also, only show a notification if video config is
             * enabled.
             */
            ConfigurationService cfg = NeomediaActivator.getConfigurationService();
            boolean videoConfigDisabled = cfg.user().getBoolean(
                NeomediaActivator.VIDEO_CONFIG_DISABLED_PROP, false);
            if ((!removal || selectedHasChanged) && !videoConfigDisabled)
            {
                showPopUpNotification(
                        title,
                        body.toString(),
                        NeomediaActivator.DEVICE_CONFIGURATION_HAS_CHANGED);
            }

            /*
             * The connected video devices have changed.  This may affect
             * whether or not the video buttons in any active call windows
             * should be enabled. Ask the UI Service to check and update this.
             */
            sLog.debug("Video devices have changed - " +
                       "updating video buttons enabled state");
            NeomediaActivator.getUIService().updateVideoButtonsEnabledState();
        }
        /*
         * A new video device has been selected.
         * We will not show a notification, we will remember to report the
         * change after the batch of changes completes.
         */
        else if (VideoDeviceListManager.PROP_DEVICE.equals(propertyName))
        {
            sLog.debug("Video device was: " +
                ev.getOldValue() + ", now: " + ev.getNewValue());
            mVideoPropertyChangeEvent = ev;
        }
    }
}
