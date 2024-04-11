/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.netaddr;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.java.sip.communicator.service.netaddr.event.NetworkConfigurationChangeListener;
import org.jitsi.util.CustomAnnotations.Nullable;

/**
 * The NetworkAddressManagerService
 * @author Emil Ivov
 */
public interface NetworkAddressManagerService
{
    /**
     * Enumeration describing the various states of BSSID retrieval.  On some
     * platforms (macOS), BSSIDs retrieval is controlled by the location
     * services permissions model.  Location services can be turned off as a
     * whole, or blocked for individual applications.
     */
    enum BSSIDAvailability
    {
        // The availability of BSSIDs has not yet been determined.
        UNKNOWN,

        // There is nothing preventing retrieval of BSSIDs.
        AVAILABLE,

        // BSSID retrieval is blocked, as location services are disabled.
        BLOCKED_LOCATION_SERVICES,

        // BSSID retrieval is blocked due to insufficient app permissions.
        BLOCKED_APP_PERMISSIONS,
    }

    /**
     * The default number of binds that a <tt>NetworkAddressManagerService</tt>
     * implementation should execute in case a port is already bound to (each
     * retry would be on a different port).
     */
    int BIND_RETRIES_DEFAULT_VALUE = 50;

    /**
     * Returns an InetAddress instance that represents the localhost, and that
     * a socket can bind upon or distribute to peers as a contact address.
     * <p>
     * This method tries to make for the ambiguity in the implementation of the
     * {@link InetAddress#getLocalHost()} method.
     * (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037).
     * <p>
     * To put it briefly, the issue is about choosing a local source
     * address to bind to or to distribute to peers. It is possible and even
     * quite probable to expect that a machine may dispose with multiple
     * addresses and each of them may be valid for a specific destination.
     * Example cases include:
     * <p>
     * 1) A dual stack IPv6/IPv4 box. <br>
     * 2) A double NIC box with a leg on the Internet and another one in a
     * private LAN <br>
     * 3) In the presence of a virtual interface over a VPN or a MobileIP(v6)
     * tunnel.
     * <p>
     * In all such cases a source local address needs to be chosen according to
     * the intended destination and after consulting the local routing table.
     * <p>
     * @param intendedDestination the address of the destination that we'd like
     * to access through the local address that we are requesting.
     *
     * @return an InetAddress instance representing the local host, and that
     * a socket can bind upon or distribute to peers as a contact address.
     */
    InetAddress getLocalHost(InetAddress intendedDestination);

    /**
     * This functions blocks for at most <code>timeout</code> period of time (with time units as specified), and if
     * the JRE's {@link InetAddress#getLocalHost()} has not returned by then, then this function returns null.
     * If {@link InetAddress#getLocalHost()} returns
     * before the timeout expires, then this function will return at that time rather than waiting for the timeout to
     * expire.
     * <p>
     * The rationale for this function's existence is that there are known problems on MacOS where
     * {@link InetAddress#getLocalHost()} may take 5s+ to load in certain hard-to-reproduce circumstances. Most of the time,
     * however, it seems to take of the order of 70ms. e.g. see <a
     * href="https://stackoverflow.com/questions/10064581/how-can-i-eliminate-slow-resolving-loading-of-localhost-virtualhost-a-2-3-secon">this
     * Stack Overflow post</a>
     * <p>
     * This can be very damaging if this happens during call setup on incoming calls.
     * <p>
     * The mitigations suggested online - namely, editing the /etc/hosts file - are not suitable for
     * our use case, as we can't ask all our users to do that, nor is it practical for us to programmatically make such
     * edits.
     *
     * @param timeout timeout
     * @param unit    time unit for timeout
     * @return nullable InetAddress
     */
    @Nullable
    InetAddress getOsLocalHostWithTimeout(long timeout, TimeUnit unit);

    /**
     * Returns the hardware address (i.e. MAC address) of the specified
     * interface name.
     *
     * @param iface the <tt>NetworkInterface</tt>
     * @return array of bytes representing the layer 2 address
     */
    byte[] getHardwareAddress(NetworkInterface iface);

    /**
     * Checks if a given IP address corresponds to the IP addresses bound to
     * any wifi devices.
     *
     * @param address the address to check.
     * @return true for a WIFI address, false otherwise.
     */
    boolean isAddressWifi(InetAddress address);

    /**
     * Finds the SSID for the current connection
     *
     * @param address the address to check.
     * @return the SSID for the connection if known, or "" otherwise
     */
    String getSSID(InetAddress address);

    /**
     * Finds the BSSID for the current connection
     *
     * @param address the address to check.
     * @return the BSSID for the connection if known, or "" otherwise
     */
    String getBSSID(InetAddress address);

    /**
     * Whether the address is connected to any form of network.
     *
     * @param address the address to check.
     * @return the true if connected
     */
    boolean isConnected(InetAddress address);

    /**
     * @return A list of the NetworkInterfaces on this computer.
     *
     * @param forceRefresh This information is queried in many places, so it is
     *                     cached by NetworkAddressManagerService and refreshed
     *                     automatically every 10 seconds.  Code calling this
     *                     method can force an immediate refresh of the cache by
     *                     passing in 'true' for this parameter (e.g. a force
     *                     refresh is done by the reloadAddressNetworkConnectionMap()
     *                     method and if we have received an event notifying that
     *                     something about the network has changed).
     *
     * @throws SocketException If the native NetworkInterface.getNetworkInterfaces()
     *                         method hits an error querying the network interfaces.
     */
    List<NetworkInterface> getNetworkInterfaces(boolean forceRefresh) throws SocketException;

    /**
     * Queries whether it is currently possible to retrieve BSSIDs on this
     * system.  If <tt>getBSSID()</tt> doesn't return a value for a connected
     * wireless network, this can provide information as to why.
     *
     * @return the enumeration indicating the current BSSID availability.
     */
    BSSIDAvailability getBSSIDAvailability();

    /**
     * Creates a <tt>DatagramSocket</tt> and binds it to on the specified
     * <tt>localAddress</tt> and a port in the range specified by the
     * <tt>minPort</tt> and <tt>maxPort</tt> parameters. We first try to bind
     * the newly created socket on the <tt>preferredPort</tt> port number and
     * then proceed incrementally upwards until we succeed or reach the bind
     * retries limit. If we reach the <tt>maxPort</tt> port number before the
     * bind retries limit, we will then start over again at <tt>minPort</tt>
     * and keep going until we run out of retries.
     *
     * @param laddr the address that we'd like to bind the socket on.
     * @param preferredPort the port number that we should try to bind to first.
     * @param minPort the port number where we should first try to bind before
     * moving to the next one (i.e. <tt>minPort + 1</tt>)
     * @param maxPort the maximum port number where we should try binding
     * before giving up and throwinG an exception.
     *
     * @return the newly created <tt>DatagramSocket</tt>.
     *
     * @throws IllegalArgumentException if either <tt>minPort</tt> or
     * <tt>maxPort</tt> is not a valid port number.
     * @throws IOException if an error occurs while the underlying resolver lib
     * is using sockets.
     * @throws BindException if we couldn't find a free port between
     * <tt>minPort</tt> and <tt>maxPort</tt> before reaching the maximum allowed
     * number of retries.
     */
    DatagramSocket createDatagramSocket(InetAddress laddr,
                                        int preferredPort,
                                        int minPort,
                                        int maxPort)
         throws IllegalArgumentException,
                IOException,
                BindException;

     /**
      * Adds new <tt>NetworkConfigurationChangeListener</tt> which will
      * be informed for network configuration changes.
      * @param listener the listener.
      */
     void addNetworkConfigurationChangeListener(
             NetworkConfigurationChangeListener listener);

     /**
      * Remove <tt>NetworkConfigurationChangeListener</tt>.
      * @param listener the listener.
      */
     void removeNetworkConfigurationChangeListener(
             NetworkConfigurationChangeListener listener);

    /**
     * Clear the cache of network connection information and rebuild it from scratch.
     *
     * This method is called when something happens that is likely to invalidate
     * our current cache.
     */
    void reloadAddressNetworkConnectionMap();

    void requestLocationPermission();
}
