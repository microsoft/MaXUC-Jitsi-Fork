/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.neomedia;

import java.beans.PropertyChangeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.media.CaptureDeviceInfo;

import org.jitsi.impl.neomedia.device.DataFlow;
import org.jitsi.impl.neomedia.device.DeviceConfiguration;
import org.jitsi.impl.neomedia.device.VideoDeviceListManager;
import org.jitsi.impl.neomedia.device.VideoSystem;

import net.java.sip.communicator.service.gui.ConfigurationForm;
import net.java.sip.communicator.util.Logger;

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

    private final Map<String, DevicePropertyChangeEvent> devicePropertyChangeEventsMap = new HashMap<>();

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
     * This method relies on receiving the events for property changes
     * related to the configuration of the changed devices
     * i.e. the changes from PROP_DEVICE, which are batched, before receiving
     * the change in the connected or disconnected video device
     * i.e. PROP_VIDEO_SYSTEM_DEVICES
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
            sLog.debug("Video Device List was: " + ev.getOldValue() + ", now: " + ev.getNewValue());

            showNotificationIfDevicesHaveChanged(ev,
                                                 new ArrayList<>(devicePropertyChangeEventsMap.values()),
                                                 NeomediaActivator.VIDEO_CONFIG_DISABLED_PROP);
            devicePropertyChangeEventsMap.clear();

            /*
             * The connected video devices have changed.  This may affect
             * whether the video buttons in any active call windows
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
            sLog.debug("Video device was: " + ev.getOldValue() + ", now: " + ev.getNewValue());
            VideoPropertyChangeEvent videoPropertyChangeEvent = new VideoPropertyChangeEvent(ev);
            devicePropertyChangeEventsMap.put(videoPropertyChangeEvent.getDataFlowType().toString(), videoPropertyChangeEvent);
        }
    }
}

class VideoPropertyChangeEvent extends DevicePropertyChangeEvent
{
    public VideoPropertyChangeEvent(PropertyChangeEvent event)
    {
        super(DataFlow.VIDEO, "impl.media.configform.VIDEO_DEVICE_SELECTED", event);
    }

    public CaptureDeviceInfo refreshAndGetSelectedDevice(PropertyChangeEvent ev)
    {
        DeviceConfiguration devConf = (DeviceConfiguration) ev.getSource();
        VideoSystem videoSystem = devConf.getVideoSystem();
        if(videoSystem != null)
            return videoSystem.getAndRefreshSelectedDevice();
        else return null;
    }
}