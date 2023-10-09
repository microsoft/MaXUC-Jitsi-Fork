// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * A Java representation of the native structure WLAN_SECURITY_ATTRIBUTES
 *
 * https://docs.microsoft.com/en-us/windows/win32/api/wlanapi/ns-wlanapi-wlan_security_attributes
 */
@FieldOrder({"bSecurityEnabled", "bOneXEnabled", "dot11AuthAlgorithm", "dot11CipherAlgorithm"})
public class WlanSecurityAttributes extends Structure
{
    // Fields making up the structure
    public boolean bSecurityEnabled;
    public boolean bOneXEnabled;
    public int dot11AuthAlgorithm;
    public int dot11CipherAlgorithm;
}
