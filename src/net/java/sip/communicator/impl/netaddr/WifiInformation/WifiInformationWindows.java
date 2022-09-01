// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import static org.jitsi.util.Hasher.logHasher;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.netaddr.NetworkConnectionInfo;
import net.java.sip.communicator.util.*;

import com.sun.jna.*;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.*;

public class WifiInformationWindows
{
    /**
     * Our class logger.
     */
    private static Logger logger = Logger.getLogger(WifiInformationWindows.class);

    /**
     * Constants
     */
    private static final int RETURN_SUCCESS = 0;

    /**
     * Set this to true to log hexdumps while debugging JNA native memory issues.
     */
    public static boolean HEXDUMP_LOGGING_ON = false;

    static
    {
        // Don't crash - just throw an exception.
        Native.setProtected(true);
    }

    /**
     * Generates a NetworkConnectionInfo object for the given address.
     *
     * @param address The IP address to generate network connection info for.
     * @param networkInterfaces The list of all network interfaces on this device
     *                          to use to find the network connection info.
     * @return The NetworkConnectionInfo object for the given address.
     */
    public NetworkConnectionInfo getNetworkConnectionInfo(String address,
                                                          List<NetworkInterface> networkInterfaces)
    {
        logger.debug("Getting network connection info for " + logHasher(address));
        Set<NetworkInterface> allInterfaces = getInterfacesForIP(address, networkInterfaces);

        NetworkConnectionInfo networkConnectionInfo;

        if (allInterfaces.size() == 1 && ignoreThisInterface(allInterfaces.iterator().next()))
        {
            // The only interface we matched is ignored, which is the state we
            // often find ourselves in when we are not physically connected to
            // either a wired or wireless network.
            // Create an "unconnected" network connection info object.
            networkConnectionInfo = new NetworkConnectionInfo(false);
        }
        else
        {
            Map<String, NetworkConnectionInfo> namesToWiFiInfo = getWirelessInterfaces();
            Set<String> wirelessInterfaces = namesToWiFiInfo.keySet();

            Set<String> matchedInterfaceNames = new HashSet<>(allInterfaces.size());
            for (NetworkInterface networkInterface : allInterfaces)
            {
                matchedInterfaceNames.add(networkInterface.getDisplayName());
            }

            // Find the intersection of the two sets.
            matchedInterfaceNames.retainAll(wirelessInterfaces);

            if (!matchedInterfaceNames.isEmpty())
            {
                // At least one interface that has this IP address is wireless, so return the first of them
                networkConnectionInfo = namesToWiFiInfo.get(matchedInterfaceNames.iterator().next());
            }
            else
            {
                // This must be a connected, wired network connection
                networkConnectionInfo = new NetworkConnectionInfo(true);
            }
        }

        // Log out details of the network connection, including hashed SSID and BSSID if wireless
        logger.debug("Got network info: " + networkConnectionInfo);

        return networkConnectionInfo;
    }

    /**
     * Generates a map of WIFI interfaces on this device indexed by their short
     * interface name.
     * @param networkInterfaces The list of all network interfaces on this device
     *                          to use to find the WIFI interface info.
     * @return The map of WIFI interfaces on this device indexed by their short
     *         interface name.
     */
    public Map<String, NetworkConnectionInfo> getWiFiInterfaceInfo(List<NetworkInterface> networkInterfaces)
    {
        // Get the list of interfaces, indexed by display name, and rearrange
        // them to be listed by short interface name.
        Map<String, NetworkConnectionInfo> ifacesByDispName = getWirelessInterfaces();
        Map<String, NetworkConnectionInfo> ifacesByName = new HashMap<>();

        for (NetworkInterface iface : networkInterfaces)
        {
            if (ifacesByDispName.containsKey(iface.getDisplayName()))
            {
                NetworkConnectionInfo ifaceInfo = ifacesByDispName.get(iface.getDisplayName());
                logger.debug("Got info for interface " + iface.getName() + ": " + ifaceInfo);
                ifacesByName.put(iface.getName(), ifaceInfo);
            }
        }

        return ifacesByName;
    }

    private boolean ignoreThisInterface(NetworkInterface networkInterface)
    {
        String name = networkInterface.getDisplayName();
        String lower = name.toLowerCase(Locale.ROOT);

        // This is heuristic - there doesn't seem to be a way to really determine this.
        // TAP is the name of the Windows driver used by many VPN implementations. We
        // search for that in a case-sensitive way to try and avoid false positives,
        // but the other checks are case-insensitive.
        return networkInterface.isVirtual() ||
               lower.contains("virtual") ||
               lower.contains("loopback") ||
               lower.contains("vpn") ||
               lower.contains("vethernet") ||
               name.contains("TAP");
    }

    private static Set<NetworkInterface> getInterfacesForIP(String address,
                                                            List<NetworkInterface> networkInterfaces)
    {
        Set<NetworkInterface> interfaceSet = new HashSet<>();

        for (NetworkInterface networkInterface : networkInterfaces)
        {
            Enumeration<InetAddress> inetAddresses =
                networkInterface.getInetAddresses();

            for (InetAddress inetAddress : Collections.list(inetAddresses))
            {
                if (inetAddress.getHostAddress().equals(address))
                {
                    interfaceSet.add(networkInterface);
                }
            }
        }

        return interfaceSet;
    }

    /**
     * Calls into the "wlanapi" library via JNA to retrieve information about
     * wireless networks that is not available via pure-Java interfaces.
     */
    private static Map<String, NetworkConnectionInfo> getWirelessInterfaces()
    {
        Map<String, NetworkConnectionInfo> interfaceMap = new HashMap<>();

        // References to hold values returned by the native library.
        PointerByReference negotiatedVersion = new PointerByReference();
        PointerByReference clientHandle = new PointerByReference();
        PWlanInterfaceInfoList.ByReference ppInterfaceInfoList =
            new PWlanInterfaceInfoList.ByReference();

        // Open a handle to the wlanapi and enumerate the wireless interfaces.
        if ((WlanApi.INSTANCE.WlanOpenHandle(2, null, negotiatedVersion,
            clientHandle.getPointer()) == RETURN_SUCCESS) &&
            (WlanApi.INSTANCE.WlanEnumInterfaces(
                clientHandle.getValue(), null, ppInterfaceInfoList) == RETURN_SUCCESS))
        {
            WlanInterfaceInfoList infList = null;
            int numInterfaces = ppInterfaceInfoList.WlanInterfaceInfoList.dwNumberOfItems;
            logger.debug("Number of wireless interfaces: " + numInterfaces);

            // If we have zero entries in the list, we mustn't try to create the
            // JNA structure, as it doesn't cope with an array of zero-length.
            if (numInterfaces > 0)
            {
                infList = new WlanInterfaceInfoList(
                        ppInterfaceInfoList.WlanInterfaceInfoList.getPointer(),
                        numInterfaces);

                // Log a hexdump of the returned native memory
                logReturnedMemory(
                        "WlanInterfaceInfoList",
                        ppInterfaceInfoList.WlanInterfaceInfoList.getPointer(),
                        WlanInterfaceInfoListEmpty.SIZE + (numInterfaces * WlanInterfaceInfo.SIZE),
                        1);
            }

            for (int ii = 0; ii < numInterfaces; ii++)
            {
                // For each interface, extract the description, SSID and BSSID.
                WlanInterfaceInfo interfaceInfo = infList.InterfaceInfo[ii];
                String interfaceDescription = interfaceInfo.getStringDescription();
                String ssid = "";
                String bssid = "";

                try
                {
                    // References to hold values returned by the native library.
                    IntByReference pDataSize = new IntByReference();
                    PointerByReference ppData = new PointerByReference();

                    // References to values passed in to the native library.
                    GUID.ByReference guidPtr = new GUID.ByReference(interfaceInfo.GUID);

                    logger.debug("Querying interface: " + interfaceDescription);

                    int rc = WlanApi.INSTANCE.WlanQueryInterface(
                        clientHandle.getValue(), //Pointer hClientHandle,
                        guidPtr, //Pointer pInterfaceGuid,
                        7, //int OpCode=wlan_intf_opcode_current_connection
                        null, //Pointer pReserved,
                        pDataSize, //Pointer pdwDataSize,
                        ppData, //PointerByReference ppData,
                        null); //Pointer pWlanOpcodeValueType);

                    logger.debug("No exception, RC=" + rc);

                    if (rc == RETURN_SUCCESS)
                    {
                        // Log a hexdump of the returned native memory.
                        logReturnedMemory("WlanConnectionAttributes",
                                          ppData.getValue(),
                                          pDataSize.getValue(),
                                          WinDef.WORD.SIZE);

                        WlanConnectionAttributes wca = new WlanConnectionAttributes(ppData.getValue());
                        ssid = wca.wlanAssociationAttributes.getSsidString();
                        bssid = wca.wlanAssociationAttributes.getBssidString();
                    }

                    logger.debug("SSID=" + logHasher(ssid));
                    logger.debug("BSSID=" + logHasher(bssid));
                }
                catch (Throwable e)
                {
                    logger.error("Exception in query interface", e);
                }

                interfaceMap.put(interfaceDescription,
                                 new NetworkConnectionInfo(true, ssid, bssid));
            }
        }
        else
        {
            logger.warn("Failed to get interfaces from Windows");
        }

        // Close our handle to the wlanapi library, allowing memory to be freed.
        WlanApi.INSTANCE.WlanCloseHandle(clientHandle.getValue(), null);

        return interfaceMap;
    }

    /**
     * A utility function to log the contents of the returned memory as a hex
     * dump.  Can be useful when debugging JNA native library issues, but not
     * used in production.
     */
    private static void logReturnedMemory(String name, Pointer memPointer, int memSize, int wordSize)
    {
        // Set this value to true to enable hexdump logging.
        if (!HEXDUMP_LOGGING_ON){
            return;
        }

        byte[] raw = memPointer.getByteArray(0, memSize * wordSize);
        String hexStr = "";
        int numBytes = 0;

        for (byte currByte : raw)
        {
            // We have to cast the byte to an integer to use the format parameter.
            // Do bitwise AND to ensure that "negative" values don't mess-up our
            // output.
            hexStr += String.format("%02X", (int)currByte & 0xFF);
            numBytes++;

            if (numBytes % 8 == 0){
                hexStr += " ";
            }
            if (numBytes % 32 == 0){
                hexStr += "\n";
            }
            if (numBytes % 256 == 0)
            {
                hexStr += "\n";
            }
        }

        // Dump the memory to the log file
        logger.error("Memory dump of " + name + " from JNA (" +
                     memSize + " x " + wordSize + "-byte words => " + numBytes + " bytes):\n" +
                     hexStr);
    }

}
