/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.media.*;
import javax.swing.*;
import javax.swing.event.*;

import org.jitsi.impl.neomedia.device.*;
import org.jitsi.util.*;

import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.Logger;

/**
 * Implements <tt>ComboBoxModel</tt> for a specific <tt>DeviceConfiguration</tt>
 * so that the latter may be displayed and manipulated in the user interface as
 * a combo box.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 */
public class DeviceConfigurationComboBoxModel
    implements ComboBoxModel<Object>,
               PropertyChangeListener,
               KeyListener
{
    /**
     * Logger for this class.
     */
    private static Logger sLog =
        Logger.getLogger(DeviceConfigurationComboBoxModel.class);

    /**
     * The <tt>Logger</tt> for logging about device changes.
     */
    private static final Logger deviceLogger =
        Logger.getLogger("jitsi.DeviceLogger");

    /**
     * Audio Capture Device.
     */
    public static final int AUDIO_CAPTURE = 3;

    /**
     * Audio device for notification sounds.
     */
    public static final int AUDIO_NOTIFY = 5;

    /**
     * Audio playback device.
     */
    public static final int AUDIO_PLAYBACK = 4;

    /**
     * Video device.
     */
    public static final int VIDEO = 2;

    /**
     * The current device configuration.
     */
    private final DeviceConfiguration mDeviceConfiguration;

    /**
     * All the active/plugged-in devices.
     */
    private final List<CaptureDevice> mActiveDevices = new ArrayList<>();

    /**
     * All the devices ever seen.  Mapping is name to UID, something like:
     * Name: Integrated Webcam
     * UID:  usb#vid_0c45pid_64d0mi_00#7fd8a11100000#{65e8773d-8f56-11d0-a3b9-00a0c9223196}\global
     */
    private LinkedHashMap<String, String> mDeviceNames =
            new LinkedHashMap<>();

    /**
     * The <tt>ListDataListener</tt>s registered with this instance.
     */
    private final List<ListDataListener> mListeners
        = new ArrayList<>();

    /**
     * The type of the media for this combo.
     */
    private final int mType;

    /**
     * The index to use for selecting devices when using keyboard navigation.
     * This is necessary because selected devices always go to the top of the
     * list, therefore a custom key manager must be used.
     */
    private int mKeyboardIndex = 0;

    /**
     * The combo box that this model is for.
     */
    private JComboBox<Object> mComboBox;

    /**
     * The device currenly selected in mComboBox.
     */
    CaptureDevice mSelectedDevice;

    /**
     * Object used to synchronize access to the current list of devices
     * available in the combo box.
     */
    private final Object mDevicesLock = new Object();

    /**
     * @param deviceConfiguration The current device configuration
     * @param type The device type, e.g. video, capture, notify, playback
     * @param comboBox The combo box that this model is for
     */
    public DeviceConfigurationComboBoxModel(
            DeviceConfiguration deviceConfiguration,
            int type,
            JComboBox<Object> comboBox)
    {
        if (deviceConfiguration == null)
            throw new IllegalArgumentException("deviceConfiguration");
        if ((type != AUDIO_CAPTURE) &&
            (type != AUDIO_NOTIFY) &&
            (type != AUDIO_PLAYBACK) &&
            (type != VIDEO))
            throw new IllegalArgumentException("type");

        mComboBox = comboBox;
        mDeviceConfiguration = deviceConfiguration;
        mType = type;
        mDeviceConfiguration.addPropertyChangeListener(this);

        getActiveDevices();
    }

    @Override
    public void addListDataListener(ListDataListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException("listener");

        if (!mListeners.contains(listener))
        {
            mListeners.add(listener);
        }
    }

    /**
     * Change of the content of the device combo box.
     *
     * @param index0 the first index to have changed (or -1 for start)
     * @param index1 the last index to have changed (or -1 for end)
     */
    protected void fireContentsChanged(int index0, int index1)
    {
        ListDataListener[] listeners
            = mListeners.toArray(
                    new ListDataListener[mListeners.size()]);
        ListDataEvent event
            = new ListDataEvent(
                    this,
                    ListDataEvent.CONTENTS_CHANGED,
                    index0,
                    index1);

        for (ListDataListener listener : listeners)
        {
            listener.contentsChanged(event);
        }
    }

    /**
     * Extracts the active devices for the current type into mActiveDevices.
     */
    private void getActiveDevices()
    {
        synchronized (mDevicesLock)
        {
            // Reset cached values.  The next call to getSelectedDevice() will
            // recalculate mSelectedDevice.  We recalculate mActiveDevices here.
            mActiveDevices.clear();
            mSelectedDevice = null;

            AudioSystem audioSystem;
            VideoSystem videoSystem;
            List<? extends CaptureDeviceInfo> infos = null;

            switch (mType)
            {
                case AUDIO_CAPTURE:
                    audioSystem = mDeviceConfiguration.getAudioSystem();
                    infos = (audioSystem == null) ? null :
                        audioSystem.getActiveDevices(DataFlow.CAPTURE);
                    mDeviceNames = (audioSystem == null) ? null :
                        audioSystem.getAllKnownDevices(DataFlow.CAPTURE);
                    break;
                case AUDIO_NOTIFY:
                    audioSystem = mDeviceConfiguration.getAudioSystem();
                    infos = (audioSystem == null) ? null :
                        audioSystem.getActiveDevices(DataFlow.NOTIFY);
                    mDeviceNames = (audioSystem == null) ? null :
                        audioSystem.getAllKnownDevices(DataFlow.NOTIFY);
                    break;
                case AUDIO_PLAYBACK:
                    audioSystem = mDeviceConfiguration.getAudioSystem();
                    infos = (audioSystem == null) ? null :
                        audioSystem.getActiveDevices(DataFlow.PLAYBACK);
                    mDeviceNames = (audioSystem == null) ? null :
                        audioSystem.getAllKnownDevices(DataFlow.PLAYBACK);
                    break;
                case VIDEO:
                    videoSystem = mDeviceConfiguration.getVideoSystem();
                    infos = (videoSystem == null) ? null :
                        videoSystem.getActiveDevices();
                    mDeviceNames = (videoSystem == null) ? null :
                        videoSystem.getAllKnownDevices();
                    break;
                default:
                    throw new IllegalStateException("Bad type = " + mType);
            }

            final int deviceCount = (infos == null) ? 0 : infos.size();

            if (deviceCount > 0)
            {
                for (CaptureDeviceInfo info : infos)
                {
                    mActiveDevices.add(new CaptureDevice(info));
                }
            }

            deviceLogger.debug("Found active devices: " + mActiveDevices);
        }
    }

    @Override
    public Object getElementAt(int index)
    {
        synchronized (mDevicesLock)
        {
            // When this is called with an index of -1 we must return the selected
            // item
            if ((index < 0) || (index > getSize()))
            {
                Object selectedItem = getSelectedItem();
                if (selectedItem instanceof CaptureDevice)
                {
                    Device info = (Device) ((CaptureDevice) selectedItem).getInfo();
                    if (info == null)
                    {
                        return NeomediaActivator.getResources().getI18NString(
                            "impl.media.configform.NO_DEVICE");
                    }

                    if (mDeviceNames != null)
                    {
                        for (String deviceName : mDeviceNames.keySet())
                        {
                            if (info.getUID().equals(mDeviceNames.get(deviceName)))
                            {
                                return deviceName;
                            }
                        }
                    }
                    else
                    {
                        sLog.warn("mDeviceNames is null!");
                    }
                }
                return selectedItem;
            }

            return (mDeviceNames != null) ? mDeviceNames.keySet().toArray()[index] : null;
        }
    }

    /**
     * Extracts the devices selected by the configuration.
     * @return <tt>CaptureDevice</tt> selected
     */
    private CaptureDevice getSelectedDevice()
    {
        synchronized (mDevicesLock)
        {
            if (mSelectedDevice != null)
            {
                return mSelectedDevice;
            }
        }

        AudioSystem audioSystem;
        Device info;

        switch (mType)
        {
            case AUDIO_CAPTURE:
                audioSystem = mDeviceConfiguration.getAudioSystem();
                info = (audioSystem == null) ? null :
                    audioSystem.getAndRefreshSelectedDevice(DataFlow.CAPTURE);
                break;
            case AUDIO_NOTIFY:
                audioSystem = mDeviceConfiguration.getAudioSystem();
                info = (audioSystem == null) ? null :
                    audioSystem.getAndRefreshSelectedDevice(DataFlow.NOTIFY);
                break;
            case AUDIO_PLAYBACK:
                audioSystem = mDeviceConfiguration.getAudioSystem();
                info = (audioSystem == null) ? null :
                    audioSystem.getAndRefreshSelectedDevice(DataFlow.PLAYBACK);
                break;
            case VIDEO:
                VideoSystem videoSystem = mDeviceConfiguration.getVideoSystem();
                info = (videoSystem == null) ? null :
                    videoSystem.getAndRefreshSelectedDevice();
                break;
            default:
                throw new IllegalStateException("type");
        }

        synchronized (mDevicesLock)
        {
            for (CaptureDevice device : mActiveDevices)
            {
                if (device.equals(info))
                {
                    deviceLogger.debug("Selected (" + mType + ") device is " + device);
                    mSelectedDevice = device;
                    return device;
                }
            }
        }

        sLog.debug("No selected device found for type " + mType);
        return null;
    }

    @Override
    public Object getSelectedItem()
    {
        return getSelectedDevice();
    }

    @Override
    public int getSize()
    {
        synchronized (mDevicesLock)
        {
            int size = (mDeviceNames == null) ? 0 : mDeviceNames.keySet().size();
            deviceLogger.debug("Num devices found = " + size);
            return size;
        }
    }

    /**
     * Notifies this instance about changes in the values of the properties of
     * {@link #mDeviceConfiguration} so that this instance keeps itself
     * up-to-date with respect to the list of devices.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which describes the name of the
     * property whose value has changed and the old and new values of that
     * property
     */
    public void propertyChange(final PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();

        if (((DeviceConfiguration.PROP_AUDIO_SYSTEM_DEVICES.equals(propertyName)) && (mType != VIDEO)) ||
            ((DeviceConfiguration.PROP_VIDEO_SYSTEM_DEVICES.equals(propertyName)) && (mType == VIDEO)))
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                sLog.debug("Received device change notif, reset active devices.");

                getActiveDevices();
                fireContentsChanged(0, getSize() - 1);
            }
            else
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                propertyChange(ev);
                            }
                        });
            }
        }
    }

    @Override
    public void removeListDataListener(ListDataListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException("listener");

        mListeners.remove(listener);
    }

    /**
     * Selects and saves the new choice.  Works for audio or video devices.
     *
     * @param deviceName the device we choose.
     * @return true if the device was successfully changed
     */
    private boolean setSelectedDevice(String deviceName)
    {
        // We cannot clear the selection of DeviceConfiguration.
        if (StringUtils.isNullOrEmpty(deviceName))
        {
            return false;
        }

        CaptureDevice device = null;

        synchronized (mDevicesLock)
        {
            // Find the CaptureDevice from the device uid.
            String deviceUID = mDeviceNames.get(deviceName);
            sLog.user("Device type " + mType + " changed to: " +
                              deviceName + ", UID: " + deviceUID);

            if (deviceUID == null)
            {
                sLog.warn("Device " + deviceName + " has a null UID - is the device connected?");
                return false;
            }

            for (CaptureDevice thisDevice : mActiveDevices)
            {
                if (deviceUID.equals(((Device) thisDevice.getInfo()).getUID()))
                {
                    sLog.debug("Selected (" + mType + ") device is " + thisDevice);
                    deviceLogger.debug("Selected " + getMediaType() +
                        " device changed: " + thisDevice.getInfo().getName());
                    device = thisDevice;
                    break;
                }
            }

            if (device == null)
            {
                sLog.warn("Failed to find device " + deviceName + " with type " + mType + "!");
                return false;
            }

            if (device.equals(mSelectedDevice))
            {
                sLog.debug("Selected device is unchanged");
                return false;
            }

            // Clear the cached value.  The next call to getSelectedDevice() will
            // recalculate it.
            mSelectedDevice = null;
        }

        if (mType == VIDEO)
        {
            VideoSystem videoSystem = mDeviceConfiguration.getVideoSystem();
            if (videoSystem != null)
            {
                videoSystem.setSelectedDevice((Device) device.getInfo());
            }
        }
        else
        {
            AudioSystem audioSystem = mDeviceConfiguration.getAudioSystem();

            if (audioSystem != null)
            {
                switch (mType)
                {
                    case AUDIO_CAPTURE:
                        audioSystem.setSelectedDevice(
                                DataFlow.CAPTURE,
                                ((Device) device.getInfo()));
                        break;
                    case AUDIO_NOTIFY:
                        audioSystem.setSelectedDevice(
                                DataFlow.NOTIFY,
                                ((Device) device.getInfo()));
                        break;
                    case AUDIO_PLAYBACK:
                        audioSystem.setSelectedDevice(
                                DataFlow.PLAYBACK,
                                ((Device) device.getInfo()));
                        break;
                }
            }
        }

        return true;
    }

    /**
     * Called when user has clicked on an item in the combo box.
     * @param item A String containing the text from the selected entry.
     */
    @Override
    public void setSelectedItem(Object item)
    {
        String deviceName = (String)item;

        if (isDeviceAvailable(deviceName))
        {
            if (setSelectedDevice(deviceName))
            {
                // Reset our device list and then update the combo box.
                sLog.debug("Reset active devices");
                getActiveDevices();
                fireContentsChanged(0, getSize() - 1);
            }
        }
        else
        {
            sLog.debug("Ignore attempt to set disabled device: " + deviceName);
        }
    }

    /**
     * Checks whether the specified device name is for a device that is
     * currently available (i.e. plugged in).
     *
     * @param deviceName The name to check
     * @return whether the device is available
     */
    protected boolean isDeviceAvailable(String deviceName)
    {
        if (StringUtils.isNullOrEmpty(deviceName))
        {
            sLog.error("Blank device name!", new Throwable());
            return false;
        }

        synchronized (mDevicesLock)
        {
            return (mDeviceNames.get(deviceName) != null);
        }
    }

    /**
     * Encapsulates a <tt>CaptureDeviceInfo</tt> for the purposes of its display
     * in the user interface.
     */
    public static class CaptureDevice
    {
        /**
         * The encapsulated info.
         */
        public final CaptureDeviceInfo mInfo;

        /**
         * Creates the wrapper.
         * @param info the info object we wrap.
         */
        public CaptureDevice(CaptureDeviceInfo info)
        {
            mInfo = info;
        }

        public CaptureDeviceInfo getInfo()
        {
            return mInfo;
        }

        /**
         * Determines whether the <tt>CaptureDeviceInfo</tt> encapsulated by
         * this instance is equal (by value) to a specific
         * <tt>CaptureDeviceInfo</tt>.
         *
         * @param cdi the <tt>CaptureDeviceInfo</tt> to be determined whether it
         * is equal (by value) to the <tt>CaptureDeviceInfo</tt> encapsulated by
         * this instance
         * @return <tt>true</tt> if the <tt>CaptureDeviceInfo</tt> encapsulated
         * by this instance is equal (by value) to the specified <tt>cdi</tt>;
         * otherwise, <tt>false</tt>
         */
        public boolean equals(CaptureDeviceInfo cdi)
        {
            return Objects.equals(mInfo, cdi);
        }

        /**
         * Gets a human-readable <tt>String</tt> representation of this
         * instance.
         *
         * @return a <tt>String</tt> value which is a human-readable
         * representation of this instance
         */
        @Override
        public String toString()
        {
            String s;

            if (mInfo == null)
            {
                s = NeomediaActivator.getResources().getI18NString(
                        "impl.media.configform.NO_DEVICE");
            }
            else
            {
                s = mInfo.getName();
            }
            return s;
        }
    }

    @Override
    public void keyPressed(KeyEvent evt)
    {
        // Nothing to do if there is only one item in the list
        if (getSize() == 1)
        {
            return;
        }

        if (evt.getKeyCode() == KeyEvent.VK_DOWN)
        {
            // On key down, select the next item from the list of devices. If we
            // reach the end then reset the count to the 2nd item as the first
            // will be the one currently selected
            mKeyboardIndex = (mKeyboardIndex < (getSize() - 1)) ?
                             mKeyboardIndex + 1 :
                             1;
        }
        else if (evt.getKeyCode() == KeyEvent.VK_UP)
        {
            // On key up, select the last element from the list. This is because
            // any selected device will be at the top of the list, so we always
            // want to roll around to the bottom
            mKeyboardIndex = getSize() - 1;
        }
        else
        {
            return;
        }

        Object selectedName;
        try
        {
            synchronized (mDevicesLock)
            {
                selectedName = mDeviceNames.keySet().toArray()[mKeyboardIndex];
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            sLog.error("User attempted to change device but hit an exception ", e);
            return;
        }

        setSelectedItem(selectedName);

        // Set the accessible information for this item
        AccessibilityUtils.setName(mComboBox, selectedName.toString());
    }

    @Override
    public void keyReleased(KeyEvent evt)
    {
        // Nothing to do
    }

    @Override
    public void keyTyped(KeyEvent evt)
    {
        // Nothing to do
    }

    // Returns the Media Type as a string
    private String getMediaType()
    {
        String mediaType;

        if (mType == AUDIO_CAPTURE)
        {
            mediaType = "Capture";
        }
        else if (mType == AUDIO_NOTIFY)
        {
            mediaType = "Notify";
        }
        else if (mType == AUDIO_PLAYBACK)
        {
            mediaType = "Playback";
        }
        else if (mType == VIDEO)
        {
            mediaType = "Video";
        }
        else
        {
            mediaType = "Unknown";
        }

        return mediaType;
    }
}
