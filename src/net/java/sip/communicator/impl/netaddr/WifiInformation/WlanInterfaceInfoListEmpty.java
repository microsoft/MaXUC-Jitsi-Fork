// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import com.sun.jna.*;
import com.sun.jna.Structure.FieldOrder;

/**
 * A Java representation of the native structure WLAN_INTERFACE_INFO_LIST, with
 * an empty array of interface structures.  Use {@link WlanInterfaceInfoList},
 * when you have more than zero interfaces.
 *
 * https://docs.microsoft.com/en-us/windows/win32/api/wlanapi/ns-wlanapi-wlan_interface_info_list
 */
@FieldOrder({"dwNumberOfItems", "dwIndex"})
public class WlanInterfaceInfoListEmpty
    extends Structure
{
    // The size in bytes of a WLAN_INTERFACE_INFO_LIST structure that contains
    // zero interfaces.  This is the sum of the size of fields in this structure.
    public static final int SIZE = 8;

    public static class ByReference
        extends WlanInterfaceInfoListEmpty
        implements Structure.ByReference
    {
    }

    // Fields making up the structure
    public int dwNumberOfItems;
    public int dwIndex;

    // This structure does NOT contain an "InterfaceInfo" field, because it is
    // used to represent the case where this field is a zero-length array.
    // Once we have determined that we have a non-zero number of interfaces,
    // we can create a WlanInterfaceInfoList structure, which does have an
    // "InterfaceInfo" field.
}
