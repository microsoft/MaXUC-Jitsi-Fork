/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

import java.util.*;

import org.w3c.dom.*;

/**
 * The Presence Rules provide-devices element. Allows a watcher to see "device"
 * information present in the presence document.
 * <p/>
 * Compliant with rfc5025.
 *
 * @author Grigorii Balutsel
 */
public class ProvideDevicePermissionType
{
    /**
     * The all-devides element.
     */
    private AllDevicesType allDevices;

    /**
     * The list of deviceID elements.
     */
    private List<DeviceIdType> devices;

    /**
     * The list of occurrenceId elements.
     */
    private List<OccurrenceIdType> occurrences;

    /**
     * The list of class elements.
     */
    private List<ClassType> classes;

    /**
     * The list of elements.
     */
    private List<Element> any;

    /**
     * Sets the value of the allDevices property.
     *
     * @param allDevices the allDevices to set.
     */
    public void setAllDevices(AllDevicesType allDevices)
    {
        this.allDevices = allDevices;
    }

    /**
     * Gets the value of the allDevices property.
     *
     * @return the allDevices property.
     */
    public AllDevicesType getAllDevices()
    {
        return allDevices;
    }

    /**
     * Gets the value of the devices property.
     *
     * @return the devices property.
     */
    public List<DeviceIdType> getDevices()
    {
        if (devices == null)
        {
            devices = new ArrayList<>();
        }
        return devices;
    }

    /**
     * Gets the value of the occurrences property.
     *
     * @return the occurrences property.
     */
    public List<OccurrenceIdType> getOccurrences()
    {
        if (occurrences == null)
        {
            occurrences = new ArrayList<>();
        }
        return occurrences;
    }

    /**
     * Gets the value of the classes property.
     *
     * @return the classes property.
     */
    public List<ClassType> getClasses()
    {
        if (classes == null)
        {
            classes = new ArrayList<>();
        }
        return classes;
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public List<Element> getAny()
    {
        if (any == null)
        {
            any = new ArrayList<>();
        }
        return any;
    }

    /**
     * The Presence Rules all-devices element.
     * <p/>
     * Compliant with rfc5025
     *
     * @author Grigorii Balutsel
     */
    public static class AllDevicesType
    {
    }

    /**
     * The Presence Rules deviceID element.
     * <p/>
     * Compliant with rfc5025
     *
     * @author Grigorii Balutsel
     */
    public static class DeviceIdType
    {
        /**
         * The element value.
         */
        private String value;

        /**
         * Creates deviceID element with value.
         *
         * @param value the elemenent value to set.
         */
        public DeviceIdType(String value)
        {
            this.value = value;
        }

        /**
         * Gets the value of the value property.
         *
         * @return the value property.
         */
        public String getValue()
        {
            return value;
        }

        /**
         * Sets the value of the value property.
         *
         * @param value the value to set.
         */
        public void setValue(String value)
        {
            this.value = value;
        }
    }
}
