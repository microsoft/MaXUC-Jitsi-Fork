/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
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
     * The last <tt>PropertyChangeEvent</tt> about an audio capture device which
     * has been received.
     */
    private PropertyChangeEvent mCapturePropertyChangeEvent;

    /**
     * The last <tt>PropertyChangeEvent</tt> about an audio notification device
     * which has been received.
     */
    private PropertyChangeEvent mNotifyPropertyChangeEvent;

    /**
     * The last <tt>PropertyChangeEvent</tt> about an audio playback device
     * which has been received.
     */
    private PropertyChangeEvent mPlaybackPropertyChangeEvent;

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
            sLog.debug("Audio Device List was: " +
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
                mCapturePropertyChangeEvent = null;
                mNotifyPropertyChangeEvent = null;
                mPlaybackPropertyChangeEvent = null;
                return;
            }

            StringBuilder body = new StringBuilder();

            for (CaptureDeviceInfo device : devices)
            {
                body.append(device.getName()).append("\r\n");
                deviceLogger.debug("Audio Device " +
                    (removal ? "Disconnected " : "Connected ") +
                    device.getName());
            }

            DeviceConfiguration devConf = (DeviceConfiguration) ev.getSource();
            AudioSystem audioSystem = devConf.getAudioSystem();
            boolean selectedHasChanged = false;

            if (audioSystem != null)
            {
                if (mCapturePropertyChangeEvent != null)
                {
                    CaptureDeviceInfo cdi
                        = audioSystem.getAndRefreshSelectedDevice(
                                DataFlow.CAPTURE);

                    if ((cdi != null)
                            && !cdi.equals(
                                    mCapturePropertyChangeEvent.getOldValue()))
                    {
                        body.append("\r\n")
                            .append(
                                    r.getI18NString(
                                            "impl.media.configform"
                                                + ".AUDIO_DEVICE_SELECTED_AUDIO_IN"))
                            .append("\r\n  ")
                            .append(cdi.getName());
                        selectedHasChanged = true;
                        CaptureDeviceInfo oldDevice = (CaptureDeviceInfo)
                            mCapturePropertyChangeEvent.getOldValue();
                        deviceLogger.debug(
                            "Selected Capture device changed: old=" +
                            ((oldDevice == null) ? "null" : oldDevice.getName())
                            + ", new=" + cdi.getName());
                    }
                }

                if (mPlaybackPropertyChangeEvent != null)
                {
                    CaptureDeviceInfo cdi
                        = audioSystem.getAndRefreshSelectedDevice(
                                DataFlow.PLAYBACK);

                    if ((cdi != null)
                            && !cdi.equals(
                                    mPlaybackPropertyChangeEvent.getOldValue()))
                    {
                        body.append("\r\n")
                            .append(
                                    r.getI18NString(
                                            "impl.media.configform"
                                                + ".AUDIO_DEVICE_SELECTED_AUDIO_OUT"))
                            .append("\r\n  ")
                            .append(cdi.getName());
                        selectedHasChanged = true;
                        CaptureDeviceInfo oldDevice = (CaptureDeviceInfo)
                            mPlaybackPropertyChangeEvent.getOldValue();
                        deviceLogger.debug(
                            "Selected Playback device changed: old=" +
                            ((oldDevice == null) ? "null" : oldDevice.getName())
                            + ", new=" + cdi.getName());
                    }
                }

                if (mNotifyPropertyChangeEvent != null)
                {
                    CaptureDeviceInfo cdi
                        = audioSystem.getAndRefreshSelectedDevice(
                                DataFlow.NOTIFY);

                    if ((cdi != null)
                            && !cdi.equals(
                                    mNotifyPropertyChangeEvent.getOldValue()))
                    {
                        body.append("\r\n")
                            .append(
                                    r.getI18NString(
                                            "impl.media.configform"
                                                + ".AUDIO_DEVICE_SELECTED_AUDIO_NOTIFICATIONS"))
                            .append("\r\n  ")
                            .append(cdi.getName());
                        selectedHasChanged = true;
                        CaptureDeviceInfo oldDevice = (CaptureDeviceInfo)
                            mNotifyPropertyChangeEvent.getOldValue();
                        deviceLogger.debug(
                            "Selected Notification device changed: old=" +
                            ((oldDevice == null) ? "null" : oldDevice.getName())
                            + ", new=" + cdi.getName());
                    }
                }
            }
            mCapturePropertyChangeEvent = null;
            mNotifyPropertyChangeEvent = null;
            mPlaybackPropertyChangeEvent = null;

            /*
             * If an old device has been disconnected and no new device has been
             * connected, show a notification only if any selected device has
             * changed.  Also, only show a notification if audio config is
             * enabled.
             */
            ConfigurationService cfg = NeomediaActivator.getConfigurationService();
            boolean audioConfigDisabled = cfg.user().getBoolean(
                NeomediaActivator.AUDIO_CONFIG_DISABLED_PROP, false);
            if ((!removal || selectedHasChanged) && !audioConfigDisabled)
            {
                showPopUpNotification(
                        title,
                        body.toString(),
                        NeomediaActivator.DEVICE_CONFIGURATION_HAS_CHANGED);
            }

            deviceLogger.debug("Finished processing device change");
        }
        /*
         * A new capture, notification or playback devices has been selected.
         * We will not show a notification, we will remember to report the
         * change after the batch of changes completes.
         */
        else if (CaptureDeviceListManager.PROP_DEVICE.equals(propertyName))
        {
            sLog.debug("Audio capture device was: " +
                ev.getOldValue() + ", now: " + ev.getNewValue());
            mCapturePropertyChangeEvent = ev;
        }
        else if (NotifyDeviceListManager.PROP_DEVICE.equals(propertyName))
        {
            sLog.debug("Audio notify device was: " +
                ev.getOldValue() + ", now: " + ev.getNewValue());
            mNotifyPropertyChangeEvent = ev;
        }
        else if (PlaybackDeviceListManager.PROP_DEVICE.equals(propertyName))
        {
            sLog.debug("Audio playback device was: " +
                ev.getOldValue() + ", now: " + ev.getNewValue());
            mPlaybackPropertyChangeEvent = ev;
        }
    }
}
