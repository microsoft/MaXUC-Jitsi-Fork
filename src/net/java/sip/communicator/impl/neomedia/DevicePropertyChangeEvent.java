/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.neomedia;

import java.beans.PropertyChangeEvent;
import javax.media.CaptureDeviceInfo;

import org.jitsi.impl.neomedia.device.DataFlow;

/**
 * This abstract class is used to keep information about an event that
 * has been received regarding a change in the connected devices.
 * The purpose of this class is to extract common code between the events
 * that will allow for easier processing.
 */
public abstract class DevicePropertyChangeEvent
{
    /**
     * An enum parameter that indicates the media flow
     */
    DataFlow dataFlowType;

    /**
     * Notification text for the device
     */
    String notificationTextForDevice;

    /**
     * The event for which the information is stored and
     * will be processed
     */
    PropertyChangeEvent propertyChangeEvent;

    /**
     * Constructs a new DevicePropertyChangeEvent with the given data flow type, translation string, and property change event.
     * @param dataFlowType the media flow of the event
     * @param notificationTextForDevice the text that will be shown in the notification for the device
     * @param propertyChangeEvent the property change event to store information about
     */
    DevicePropertyChangeEvent(DataFlow dataFlowType, String notificationTextForDevice, PropertyChangeEvent propertyChangeEvent)
    {
        this.dataFlowType = dataFlowType;
        this.notificationTextForDevice = notificationTextForDevice;
        this.propertyChangeEvent = propertyChangeEvent;
    }

    public DataFlow getDataFlowType()
    {
        return dataFlowType;
    }

    public String getNotificationTextForDevice()
    {
        return notificationTextForDevice;
    }

    public PropertyChangeEvent getPropertyChangeEvent()
    {
        return propertyChangeEvent;
    }

    /**
     * Refreshes the media device and gets the newly selected device
     * @param ev the property change event that contains the source of the device configuration
     * @return the selected device or null if the configuration is not available
     */
    abstract CaptureDeviceInfo refreshAndGetSelectedDevice(PropertyChangeEvent ev);
}
