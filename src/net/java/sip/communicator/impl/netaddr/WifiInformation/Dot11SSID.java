// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.WinDef.ULONG;

/**
 * A Java representation of the native structure DOT11_SSID
 *
 * https://docs.microsoft.com/en-us/windows/win32/nativewifi/dot11-ssid
 */
@FieldOrder({"uSSIDLength", "ucSSID"})
public class Dot11SSID extends Structure
{
    // The maximum length of an SSID in bytes.
    public static final int DOT11_SSID_MAX_LENGTH = 32;

    // Fields making up the structure
    public ULONG uSSIDLength;
    public byte[] ucSSID = new byte[DOT11_SSID_MAX_LENGTH];
}
