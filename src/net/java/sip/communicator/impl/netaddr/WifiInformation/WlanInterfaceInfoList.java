// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * A Java representation of the native structure WLAN_INTERFACE_INFO_LIST
 *
 * https://docs.microsoft.com/en-us/windows/win32/api/wlanapi/ns-wlanapi-wlan_interface_info_list
 */
@FieldOrder({"dwNumberOfItems", "dwIndex", "InterfaceInfo"})
public class WlanInterfaceInfoList extends Structure
{
    // Fields making up the structure
    public int dwNumberOfItems;
    public int dwIndex;
    public WlanInterfaceInfo[] InterfaceInfo;

    /**
     * Constructor used for creating from native memory.
     *
     * The number of interfaces must be specified so that we can get the size of
     * the InterfaceInfo array correct.  This will fail if called with zero
     * interfaces.  Use {@link WlanInterfaceInfoListEmpty} if there are no
     * interfaces, or if you don't yet know how many there will be.
     *
     * @param p Pointer to the native memory to read
     * @param numInterfaces The number of interfaces in the native memory array.
     *                      Must not be zero.
     */
    public WlanInterfaceInfoList(Pointer p, int numInterfaces)
    {
        super(p);
        InterfaceInfo = new WlanInterfaceInfo[numInterfaces];
        read();
    }
}
