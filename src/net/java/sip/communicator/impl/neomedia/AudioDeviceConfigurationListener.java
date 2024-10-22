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

import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceListManager;
import org.jitsi.impl.neomedia.device.DataFlow;
import org.jitsi.impl.neomedia.device.DeviceConfiguration;
import org.jitsi.impl.neomedia.device.NotifyDeviceListManager;
import org.jitsi.impl.neomedia.device.PlaybackDeviceListManager;

import net.java.sip.communicator.service.gui.ConfigurationForm;
import net.java.sip.communicator.util.Logger;

/**
 * A listener to the click on the popup message concerning audio
 * device configuration changes.
 */
public class AudioDeviceConfigurationListener
    extends AbstractDeviceConfigurationListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>AudioDeviceConfigurationListener</tt>
     * class for logging output.
     */
    private static final Logger sLog =
        Logger.getLogger(AudioDeviceConfigurationListener.class);

    /**
     * The <tt>Logger</tt> for logging about device changes.
     */
    private static final Logger deviceLogger =
        Logger.getLogger("jitsi.DeviceLogger");

    /**
     * A map that holds batched changes to device configurations.
     * Events are batched to display a single notification to the user
     * based on information from multiple events.
     * For example, if a device is disconnected, the event for the newly selected device
     * (based on user preference) will be fired first and batched.
     * Once the event for the disconnected device comes in, it will be processed
     * along with the batched events. The user will then see a notification
     * indicating the disconnected device and the newly selected device.
     */
    private final Map<String, DevicePropertyChangeEvent> batchedDevicePropertyChangeEvents = new HashMap<>();

    /**
     * Creates a listener to the click on the popup message concerning audio
     * device configuration changes.
     *
     * @param configurationForm The audio configuration form.
     */
    public AudioDeviceConfigurationListener(
            ConfigurationForm configurationForm)
    {
        super(configurationForm);
    }

    /**
     * Notifies this instance that a property related to the configuration of
     * devices has had its value changed and thus signals that an audio device
     * may have been plugged or unplugged.
     *
     * This method relies on receiving the events for property changes
     * related to the configuration of the changed devices
     * i.e. the changes from PROP_DEVICE, which are batched, before receiving
     * the change in the connected or disconnected audio device
     * i.e. PROP_AUDIO_SYSTEM_DEVICES
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which describes the name of the
     * property whose value has changed and the old and new values of that
     * property
     */
    public void propertyChange(PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();

        /*
         * The list of available capture, notification and/or playback devices
         * has changed.
         */
        if (DeviceConfiguration.PROP_AUDIO_SYSTEM_DEVICES.equals(propertyName))
        {
            sLog.debug("Audio Device List was: " + ev.getOldValue() + ", now: " + ev.getNewValue());

            showNotificationIfDevicesHaveChanged(ev,
                                                 new ArrayList<>(batchedDevicePropertyChangeEvents.values()),
                                                 NeomediaActivator.AUDIO_CONFIG_DISABLED_PROP);

            batchedDevicePropertyChangeEvents.clear();

            deviceLogger.debug("Finished processing device change");
        }
        /*
         * A new capture, notification or playback devices has been selected.
         * We will not show a notification, we will remember to report the
         * change after the batch of changes completes.
         */
        else if (CaptureDeviceListManager.PROP_DEVICE.equals(propertyName))
        {
            sLog.debug("Audio capture device was: " + ev.getOldValue() + ", now: " + ev.getNewValue());
            CapturePropertyChangeEvent captureEvent = new CapturePropertyChangeEvent(ev);
            batchedDevicePropertyChangeEvents.put(captureEvent.getDataFlowType().toString(), captureEvent);
        }
        else if (NotifyDeviceListManager.PROP_DEVICE.equals(propertyName))
        {
            sLog.debug("Audio notify device was: " + ev.getOldValue() + ", now: " + ev.getNewValue());
            NotifyPropertyChangeEvent notifyEvent = new NotifyPropertyChangeEvent(ev);
            batchedDevicePropertyChangeEvents.put(notifyEvent.getDataFlowType().toString(), notifyEvent);
        }
        else if (PlaybackDeviceListManager.PROP_DEVICE.equals(propertyName))
        {
            sLog.debug("Audio playback device was: " + ev.getOldValue() + ", now: " + ev.getNewValue());
            PlaybackPropertyChangeEvent playbackEvent = new PlaybackPropertyChangeEvent(ev);
            batchedDevicePropertyChangeEvents.put(playbackEvent.getDataFlowType().toString(), playbackEvent);
        }
    }
}

/**
 * This class provides common code for the different types of audio change events
 */
abstract class AbstractAudioPropertyChangeEvent extends DevicePropertyChangeEvent {
    AbstractAudioPropertyChangeEvent(DataFlow type, String i18NString, PropertyChangeEvent event) {
        super(type, i18NString, event);
    }

    public CaptureDeviceInfo refreshAndGetSelectedDevice(PropertyChangeEvent ev) {
        DeviceConfiguration devConf = (DeviceConfiguration) ev.getSource();
        AudioSystem audioSystem = devConf.getAudioSystem();
        if(audioSystem != null)
            return audioSystem.getAndRefreshSelectedDevice(getDataFlowType());
        else return null;
    }
}

/**
 * This class keeps information about a capture device change event
 */
class CapturePropertyChangeEvent extends AbstractAudioPropertyChangeEvent
{
    CapturePropertyChangeEvent(PropertyChangeEvent event)
    {
        super(DataFlow.CAPTURE, "impl.media.configform.AUDIO_DEVICE_SELECTED_AUDIO_IN", event);
    }
}

/**
 *  This class keeps information about a playback device change event
 */
class PlaybackPropertyChangeEvent extends AbstractAudioPropertyChangeEvent
{
    PlaybackPropertyChangeEvent(PropertyChangeEvent event)
    {
        super(DataFlow.PLAYBACK, "impl.media.configform.AUDIO_DEVICE_SELECTED_AUDIO_OUT", event);
    }
}

/**
 * This class keeps information about a notify device change event
 */
class NotifyPropertyChangeEvent extends AbstractAudioPropertyChangeEvent
{
    NotifyPropertyChangeEvent(PropertyChangeEvent event)
    {
        super(DataFlow.NOTIFY, "impl.media.configform.AUDIO_DEVICE_SELECTED_AUDIO_NOTIFICATIONS", event);
    }
}