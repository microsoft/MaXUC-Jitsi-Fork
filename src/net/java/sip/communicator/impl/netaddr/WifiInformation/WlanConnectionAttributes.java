// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * A Java representation of the native structure WLAN_CONNECTION_ATTRIBUTES
 *
 * https://docs.microsoft.com/en-us/windows/win32/api/wlanapi/ns-wlanapi-wlan_connection_attributes
 */
@FieldOrder({
    "isState",
    "wlanConnectionMode",
    "strProfileName",
    "wlanAssociationAttributes",
    "wlanSecurityAttributes"
})
public class WlanConnectionAttributes extends Structure
{
    // The maximum length of the wireless profile name in 16-bit characters.
    public static final int WLAN_MAX_NAME_LENGTH = 256;

    // Fields making up the structure
    public int isState;
    public int wlanConnectionMode;
    public char[] strProfileName = new char[WLAN_MAX_NAME_LENGTH];
    public WlanAssociationAttributes wlanAssociationAttributes;
    public WlanSecurityAttributes wlanSecurityAttributes;

    /**
     * Constructor used for creating from native memory.
     *
     * @param p Pointer to the native memory to read
     */
    public WlanConnectionAttributes(Pointer p)
    {
        super(p);
        read();
    }
}
