// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import com.sun.jna.*;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.*;

/**
 * A Java representation of the native structure WLAN_INTERFACE_INFO
 *
 * https://docs.microsoft.com/en-us/windows/win32/api/wlanapi/ns-wlanapi-wlan_interface_info
 */

@FieldOrder({"GUID", "strInterfaceDescription", "isState"})
public class WlanInterfaceInfo extends Structure
{
    // Size of WlanInterfaceInfo structure in bytes.
    public static final int SIZE = 532;

    // Maximum length of description in 16-bit characters.
    public static final int MAXIMUM_DESCRIPTION_LENGTH = 256;

    // Fields making up the structure
    public Guid.GUID GUID;
    public char[] strInterfaceDescription = new char[MAXIMUM_DESCRIPTION_LENGTH];
    public int isState;

    /**
     * @return Interface description formatted as a String, with any trailing
     * spaces trimmed.
     */
    public String getStringDescription()
    {
        return new String(strInterfaceDescription).trim();
    }
}
