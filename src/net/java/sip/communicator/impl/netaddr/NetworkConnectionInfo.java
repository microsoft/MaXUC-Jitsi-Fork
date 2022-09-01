// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr;

import static org.jitsi.util.Hasher.logHasher;

import java.util.Objects;

/**
 * Class for storing network connection information for a network interface.
 * This is used to pass around related pieces of information in a single unit.
 */
public class NetworkConnectionInfo
{
    public final boolean mIsWireless;
    public final String mSSID;
    public final String mBSSID;
    public final boolean mConnected;

    public NetworkConnectionInfo(boolean connected)
    {
        this(false, "", "", connected);
    }

    public NetworkConnectionInfo(boolean isWireless, String ssid, String bssid)
    {
        this(isWireless, ssid, bssid, true);
    }

    private NetworkConnectionInfo(boolean isWireless, String ssid, String bssid, boolean connected)
    {
        mIsWireless = isWireless;
        mSSID = ssid;
        mBSSID = bssid;
        mConnected = connected;
    }

    @Override
    public String toString()
    {
        return "NetworkConnectionInfo{" +
               (mConnected ?
                       (mIsWireless ?
                        "SSID: " + logHasher(mSSID) +
                        ", BSSID: " + logHasher(mBSSID) :
                        "wired") :
                       "unconnected") +
               '}';
    }

    @Override
    public boolean equals(Object o)
    {
        // Two NetworkConnectionInfo objects are considered equal if all of
        // their member variables are equal.
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkConnectionInfo that = (NetworkConnectionInfo) o;
        return mIsWireless == that.mIsWireless && mConnected == that.mConnected &&
               Objects.equals(mSSID, that.mSSID) && Objects.equals(mBSSID, that.mBSSID);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mIsWireless, mSSID, mBSSID, mConnected);
    }
}
