/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.neomedia;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.CaptureDeviceInfo;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.plugin.desktoputil.NotificationInfo;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPANotion;
import net.java.sip.communicator.service.wispaservice.WISPANotionType;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;

/**
 * An abstract listener to the click on the popup message concerning
 * device configuration changes.
 *
 * @author Vincent Lucas
 */
public abstract class AbstractDeviceConfigurationListener
    implements PropertyChangeListener
{
    private static final Logger sLog =
            Logger.getLogger(AbstractDeviceConfigurationListener.class);

    /**
     * The <tt>Logger</tt> for logging about device changes.
     */
    private static final Logger deviceLogger =
            Logger.getLogger("jitsi.DeviceLogger");

    /**
     *  The audio or video configuration form.
     */
    private final ConfigurationForm configurationForm;

    /**
     * Map that keeps the states for notifications that have appeared when a certain device
     * has connected/disconnected. The key for the map is the text for the notification,
     * and the value is the last time the notification appeared and the number of times
     * it was shown to the user. This is used to detect repeated notifications
     * in case a device is malfunctioning and causes a surge of notifications.
     * The malfunctioning is detected by making sure a notification does not appear
     * more than MAX_NOTIFICATION_CHANGES consecutive times with a difference of
     * less than MAX_NOTIFICATION_CHANGES_TIMEFRAME_MS between the appearances
     */
    private final Map<String, NotificationStateInfo> notificationStates = new HashMap<>();

    /**
     * The amount of time that a notification can't be shown
     * if the MAX_NOTIFICATION_CHANGES limit has been reached
     */
    private final int MAX_NOTIFICATION_CHANGES_TIMEFRAME_MS = 60 * 1000;

    /**
     * The maximum number of times a notification can be shown
     * during the MAX_NOTIFICATION_CHANGES_TIMEFRAME_MS
     */
    private final int MAX_NOTIFICATION_CHANGES = 6;

    /**
     * Creates an abstract listener to the click on the popup message concerning
     * device configuration changes.
     *
     * @param configurationForm The audio or video configuration form.
     */
    public AbstractDeviceConfigurationListener(
            ConfigurationForm configurationForm)
    {
        this.configurationForm = configurationForm;
    }

    /**
     * Indicates that user has clicked on the popup message.
     */
    public void popupMessageClicked()
    {
        // Get the UI service
        UIService uiService
            = ServiceUtils.getService(
                    NeomediaActivator.getBundleContext(),
                    UIService.class);

        if(uiService == null)
        {
            return;
        }

        // Shows the audio configuration window.
        ConfigurationContainer configurationContainer
            = uiService.getConfigurationContainer();

        configurationContainer.setSelected(configurationForm);
        configurationContainer.setVisible(true);
    }

    /**
     * Show notification for the new connected/disconnected devices
     * and any pending changes made by selecting a new device
     * @param ev the property change event that concerns the device. This event is
     * PROP_AUDIO_SYSTEM_DEVICES or PROP_VIDEO_SYSTEM_DEVICES (as PROP_DEVICE events
     * are batched) and that means that a device has been physically plugged/unplugged.
     * This event is fired per device and has either new or old value based on
     * whether the device has been plugged or unplugged.
     * This can be seen from where the event is fired in DeviceSystemManager.startDeviceSystemInitThread
     * @param batchedDevicePropertyChangeEvents device changes that are batched
     *                       i.e. a change made by a user in the Settings panel
     * @param configDisabledCheck whether the device config option is enabled
     */
    public void showNotificationIfDevicesHaveChanged(PropertyChangeEvent ev,
                                                     List<DevicePropertyChangeEvent> batchedDevicePropertyChangeEvents,
                                                     String configDisabledCheck) {

        ConfigurationService cfg = NeomediaActivator.getConfigurationService();
        if (cfg.user().getBoolean(configDisabledCheck, false))
        {
            return;
        }

        @SuppressWarnings("unchecked")
        List<CaptureDeviceInfo> disconnectedDevices = (List<CaptureDeviceInfo>) ev.getOldValue();
        @SuppressWarnings("unchecked")
        List<CaptureDeviceInfo> connectedDevices = (List<CaptureDeviceInfo>) ev.getNewValue();

        String title;
        ResourceManagementService r = NeomediaActivator.getResources();
        List<CaptureDeviceInfo> devices;
        boolean newDeviceConnected;

        // At least one new device has been connected.
        if (!connectedDevices.isEmpty())
        {
            title = r.getI18NString("impl.media.configform.AUDIO_DEVICE_CONNECTED");
            devices = connectedDevices;
            newDeviceConnected = true;
        }
        /*
         * At least one old device has been disconnected and no new device
         * has been connected.
         */
        else if (!disconnectedDevices.isEmpty())
        {
            title = r.getI18NString("impl.media.configform.AUDIO_DEVICE_DISCONNECTED");
            devices = disconnectedDevices;
            newDeviceConnected = false;
        }
        else
        {
            /*
             * Neither a new device has been connected nor an old device has
             * been disconnected. Why are we even here in the first place
             * anyway?
             */
            return;
        }

        StringBuilder body = new StringBuilder();
        for (CaptureDeviceInfo device : devices)
        {
            body.append(device.getName()).append("\r\n");
            sLog.debug("Device " +
                       (newDeviceConnected ? "Connected " : "Disconnected ") +
                       device.getName());
        }

        boolean selectedHasChanged = false;

        for (DevicePropertyChangeEvent changeEvent : batchedDevicePropertyChangeEvents)
        {
            CaptureDeviceInfo cdi = changeEvent.refreshAndGetSelectedDevice(ev);
            if ((cdi != null) && !cdi.equals(changeEvent.getPropertyChangeEvent().getOldValue()))
            {
                body.append("\r\n")
                        .append(r.getI18NString(changeEvent.getNotificationTextForDevice()))
                        .append("\r\n  ")
                        .append(cdi.getName());
                selectedHasChanged = true;

                CaptureDeviceInfo oldDevice = (CaptureDeviceInfo) changeEvent.getPropertyChangeEvent().getOldValue();
                deviceLogger.debug("Selected " + changeEvent.getDataFlowType() + " device changed: old=" +
                                   ((oldDevice == null) ? "null" : oldDevice.getName())
                                   + ", new=" + cdi.getName());
            }
        }

        long currentTimestamp = System.currentTimeMillis();

        if ((newDeviceConnected || selectedHasChanged))
        {
            if(isNotificationEligibleToShow(body.toString(), currentTimestamp))
            {
                showPopUpNotification(title, body.toString());
            }
            updateNotificationState(body.toString(), currentTimestamp);
        }

    }

    private void updateNotificationState(String body, long currentTimestamp)
    {
        NotificationStateInfo info = notificationStates.getOrDefault(body, new NotificationStateInfo());
        long previousTimestamp = info.getLastShownTimestamp();

        long diff = currentTimestamp - previousTimestamp;

        // If the notification hasn't appeared in the last expiry period, we can reset it's counter
        if (diff >= MAX_NOTIFICATION_CHANGES_TIMEFRAME_MS)
        {
            info.resetAppearenceCount();
        }
        else
        {
            info.increaseAppearanceCount();
        }

        info.updateLastShownTimestamp(currentTimestamp);
        notificationStates.put(body, info);
    }

    @VisibleForTesting
    public boolean isNotificationEligibleToShow(String body, long currentTimestamp)
    {
        if (!notificationStates.containsKey(body))
        {
            return true;
        }

        NotificationStateInfo info = notificationStates.get(body);

        long previousTimestamp = info.getLastShownTimestamp();
        int appearances = info.getAppearancesCount();

        long diff = currentTimestamp - previousTimestamp;

        // If enough time has passed since we've last shown the notification or
        // the notification hasn't exceeded the maximum number of consecutive appearances
        // then we should show it
        if (diff >= MAX_NOTIFICATION_CHANGES_TIMEFRAME_MS || appearances < MAX_NOTIFICATION_CHANGES)
        {
            return true;
        }

        deviceLogger.debug("Will not show notification. " +
                           "Number of appearances: " + appearances + ", during " + diff / 1000 + " seconds.");
        return false;
    }

   /**
     * Shows a pop-up notification corresponding to a device configuration
     * change.
     *
     * @param title The title of the pop-up notification.
     * @param body  A body text describing the device modifications.
     */
    public void showPopUpNotification(String title, String body)
    {
        WISPAService wispaService = NeomediaActivator.getWispaService();

        if (wispaService == null || (title == null && body == null))
        {
            return;
        }

        String clickText = NeomediaActivator.getResources().getI18NString(
            "impl.media.configform.AUDIO_DEVICE_CONFIG_MANAGMENT_CLICK");

        WISPANotion notion = new WISPANotion(WISPANotionType.NOTIFICATION,
            new NotificationInfo(title, body + "\r\n\r\n" + clickText,
                this::popupMessageClicked));

        wispaService.notify(WISPANamespace.EVENTS, WISPAAction.NOTION, notion);
    }
}

/**
 * This class keeps information about the number of appearances of a notification
 * and the last time that the notification has been displayed
 */
class NotificationStateInfo
{
    private long lastShownTimestamp = 0;
    private int appearancesCount = 0;

    public void updateLastShownTimestamp(long newTimestamp) {
        this.lastShownTimestamp = newTimestamp;
    }

    public void resetAppearenceCount() {
        appearancesCount = 0;
    }

    public void increaseAppearanceCount() {
        this.appearancesCount++;
    }

    public long getLastShownTimestamp()
    {
        return lastShownTimestamp;
    }

    public int getAppearancesCount()
    {
        return appearancesCount;
    }
}