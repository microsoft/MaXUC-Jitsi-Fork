// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

/**
 * A Java representation of the native structure WLAN_SECURITY_ATTRIBUTES
 *
 * https://docs.microsoft.com/en-us/windows/win32/api/wlanapi/ns-wlanapi-wlan_security_attributes
 */
public class WlanSecurityAttributes extends Structure
{
    // Fields making up the structure
    public boolean bSecurityEnabled;
    public boolean bOneXEnabled;
    public int dot11AuthAlgorithm;
    public int dot11CipherAlgorithm;

    @Override
    protected List<String> getFieldOrder()
    {
        return Arrays.asList(new String[] {
                "bSecurityEnabled",
                "bOneXEnabled",
                "dot11AuthAlgorithm",
                "dot11CipherAlgorithm"
        });
    }
}
