// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import static org.jitsi.util.Hasher.logHasher;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

import ca.weblite.objc.Client;
import ca.weblite.objc.Proxy;

import net.java.sip.communicator.impl.netaddr.NetworkConnectionInfo;
import net.java.sip.communicator.util.Logger;

public class WifiInformationMac
{
    /**
     * Our class logger.
     */
    private static Logger logger = Logger.getLogger(WifiInformationMac.class);

    static
    {
        // Load the CoreWLAN framework, required for querying wireless network
        // interfaces.
        MacLibraryUtils.load("CoreWLAN",
                             "/System/Library/Frameworks/CoreWLAN.framework/CoreWLAN");
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
        Set<String> allInterfaces = getInterfaceMacsForIP(address, networkInterfaces);

        Map<String, NetworkConnectionInfo> macsToNetworkConnectionInfo = getWirelessInterfaceMacs();
        Set<String> wirelessInterfaces = macsToNetworkConnectionInfo.keySet();

        // Find the intersection of the two sets.
        allInterfaces.retainAll(wirelessInterfaces);

        NetworkConnectionInfo networkConnectionInfo;
        if (!allInterfaces.isEmpty())
        {
            // At least one interface that has this IP address is wireless, so return the first of them
            networkConnectionInfo = macsToNetworkConnectionInfo.get(allInterfaces.iterator().next());
        }
        else
        {
            // Assume this is a connected wired network connection, not a wireless one
            networkConnectionInfo = new NetworkConnectionInfo(true);
        }

        // Log out details of network
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
        // Get the list of interfaces, indexed by MAC address, and rearrange
        // them to be listed by short interface name.
        Map<String, NetworkConnectionInfo> ifacesByMac = getWirelessInterfaceMacs();
        Map<String, NetworkConnectionInfo> ifacesByName = new HashMap<>();

        try
        {
            for (NetworkInterface iface : networkInterfaces)
            {
                byte[] hardwareAddress = iface.getHardwareAddress();
                if (hardwareAddress != null)
                {
                    String macAddr = bytesToHex(hardwareAddress);
                    if (ifacesByMac.containsKey(macAddr))
                    {
                        NetworkConnectionInfo ifaceInfo = ifacesByMac.get(macAddr);
                        logger.debug("Got info for interface " + iface.getName() + ": " + ifaceInfo);
                        ifacesByName.put(iface.getName(), ifaceInfo);
                    }
                }
            }
        }
        catch (SocketException e)
        {
            logger.warn("Problem getting network interface list", e);
        }

        return ifacesByName;
    }

    private static Set<String> getInterfaceMacsForIP(String address,
                                                     List<NetworkInterface> networkInterfaces)
    {
        Set<String> interfaceList = new HashSet<>();
        logger.debug("Looking to match " + address);

        try
        {
            for (NetworkInterface networkInterface : networkInterfaces)
            {
                logger.debug("Examining interface " + networkInterface);

                // Hardware address can be null if this is an interface which
                // recently gone down
                if (networkInterface.getHardwareAddress() == null)
                    continue;

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                for (InetAddress inetAddress : Collections.list(inetAddresses))
                {
                    logger.debug("Looking at " + inetAddress.getHostAddress());
                    if (inetAddress.getHostAddress().equals(address))
                    {
                        logger.debug("Adding " + bytesToHex(networkInterface.getHardwareAddress()));
                        interfaceList.add(bytesToHex(networkInterface.getHardwareAddress()));
                    }
                }
            }
        }
        catch (SocketException e)
        {
            logger.warn("Problem getting network interface list", e);
        }

        return interfaceList;
    }

    /**
     * Gets a map of MAC addresses to wireless network information.
     *
     * @return A map of MACs to WirelessInfo objects.
     */
    private static Map<String, NetworkConnectionInfo> getWirelessInterfaceMacs()
    {
        Map<String, NetworkConnectionInfo> interfaceMap = new HashMap<>();

        // Obtain reference to Singleton instance of Objective-C client
        Client c = Client.getInstance();

        // Get BSD interface names of all WLAN interface attached to the system
        // It returns a NSSet object containing NSString objects representing
        // BSD interface name
        Proxy allWifiInterfaceNames =
            c.sendProxy("CWInterface", "interfaceNames");

        // Get object enumerator
        Proxy enumerator = allWifiInterfaceNames.sendProxy("objectEnumerator");

        // Get no of interfaces
        int noOfInterfaces = allWifiInterfaceNames.sendInt("count");
        logger.debug("Number of wireless interfaces found: " + noOfInterfaces);

        // Start enumerating objects and for WLAN interface name, get
        // corresponding interface object and its details
        Object next = enumerator.send("nextObject");

        while (next != null)
        {
            try
            {
                String currentIfName = next.toString();
                logger.debug("Found wireless interface: " + currentIfName);

                // Get WLAN interface
                Proxy o = c.sendProxy("CWInterface", "interfaceWithName:",
                                      currentIfName);

                // Get MAC Address - make it consistent with upper case and no punctuation.
                String mac = (o.get("hardwareAddress")).toString();
                Object ssid = o.get("ssid");
                Object bssid = o.get("bssid");

                // Connected wireless networks will always have an SSID.  They
                // may not appear to have a BSSID, as location services
                // permissions can restrict access to this field.
                if (ssid == null)
                {
                    logger.debug("SSID is null for " + logHasher(mac) +
                                 " so it is not a wireless interface");
                }
                else
                {
                    String ssidString = ssid.toString();
                    String bssidString = (bssid != null) ? bssid.toString() : "";
                    NetworkConnectionInfo wirelessInfo = new NetworkConnectionInfo(true, ssidString, bssidString);

                    logger.debug("Adding " + logHasher(mac) + ":" + wirelessInfo);
                    interfaceMap.put(mac.toUpperCase().replaceAll(":", ""),
                                     wirelessInfo);
                }
            }
            catch (Exception e)
            {
                // If we fail to find out about a particular interface,
                // we should just continue to the next one, and not fall over.
                logger.warn("Failed to examine interface: ", e);
            }

            next = enumerator.send("nextObject");
        }

        return interfaceMap;
    }

    // Ugly but fastest way to convert bytes to Hex.
    // https://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
    protected static final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
