// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef.ULONG;

/**
 * A Java representation of the native structure WLAN_ASSOCIATION_ATTRIBUTES
 *
 * https://docs.microsoft.com/en-us/windows/win32/api/wlanapi/ns-wlanapi-wlan_association_attributes
 */
public class WlanAssociationAttributes extends Structure
{
    // The length in bytes of the binary representation of a MAC address.
    public  static final int MAC_ADDRESS_BINARY_LENGTH = 6;

    // Fields making up the structure
    public Dot11SSID dot11Ssid;
    public int dot11BssType;
    public byte[] dot11Bssid = new byte[MAC_ADDRESS_BINARY_LENGTH];
    public int dot11PhyType;
    public ULONG uDot11PhyIndex;
    public ULONG wlanSignalQuality;
    public ULONG ulRxRate;
    public ULONG ulTxRate;

    @Override
    protected List<String> getFieldOrder()
    {
        return Arrays.asList(new String[] {
                "dot11Ssid",
                "dot11BssType",
                "dot11Bssid",
                "dot11PhyType",
                "uDot11PhyIndex",
                "wlanSignalQuality",
                "ulRxRate",
                "ulTxRate"
        });
    }

    /**
     * @return String representation of a BSSID, formatted as a MAC address,
     * using the default colon separator.
     */
    public String getBssidString()
    {
        return getBssidString(':');
    }

    /**
     *
     * @param separator The separator to use in the MAC address
     * @return String representation of a BSSID, formatted as a MAC address,
     * using the specified separator.
     */
    public String getBssidString(char separator)
    {
        String result = "";

        for (int ii = 0; ii < MAC_ADDRESS_BINARY_LENGTH; ii++)
        {
            if (ii != 0)
            {
                result += separator;
            }

            result += String.format("%02X", (int) dot11Bssid[ii] & 0xFF);
        }

        return result;
    }

    /**
     * @return String representation of the SSID.
     */
    public String getSsidString()
    {
        // The maximum SSID length is 32 characters, so it is safe to convert
        // the ULONG length value to an int - we won't lose information.
        return new String(dot11Ssid.ucSSID, 0, dot11Ssid.uSSIDLength.intValue());
    }
}
