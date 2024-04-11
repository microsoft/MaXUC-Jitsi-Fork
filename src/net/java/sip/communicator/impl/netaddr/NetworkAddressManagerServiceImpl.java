/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.impl.netaddr.WifiInformation.LocationDelegateMac;
import net.java.sip.communicator.impl.netaddr.WifiInformation.WifiInformationMac;
import net.java.sip.communicator.impl.netaddr.WifiInformation.WifiInformationWindows;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.netaddr.event.ChangeEvent;
import net.java.sip.communicator.service.netaddr.event.NetworkConfigurationChangeListener;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.NetworkUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.util.CustomAnnotations.Nullable;
import org.jitsi.util.Hasher;
import org.jitsi.util.OSUtils;

/**
 * This implementation of the Network Address Manager allows you to
 * intelligently retrieve the address of your localhost according to the
 * destinations that you will be trying to reach.
 *
 * @author Emil Ivov
 */
public class NetworkAddressManagerServiceImpl
    implements NetworkAddressManagerService, NetworkConfigurationChangeListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(NetworkAddressManagerServiceImpl.class);

    /**
     * The socket that we use for dummy connections during selection of a local
     * address that has to be used when communicating with a specific location.
     */
    DatagramSocket localHostFinderSocket = null;

    /**
     * A random (unused)local port to use when trying to select a local host
     * address to use when sending messages to a specific destination.
     */
    private static final int RANDOM_ADDR_DISC_PORT = 55721;

    /**
     * The name of the property containing the number of binds that we should
     * execute in case a port is already bound to (each retry would be on
     * a new random port).
     */
    public static final String BIND_RETRIES_PROPERTY_NAME
        = "net.java.sip.communicator.service.netaddr.BIND_RETRIES";

    /**
     * A map from an InetAddress to its NetworkConnectionInfo data structure.
     */
    private final Map<InetAddress, NetworkConnectionInfo> addressNetworkConnectionInfoMap =
            new HashMap<>();

    /**
     * A thread which periodically scans network interfaces and reports
     * changes in network configuration.
     */
    private NetworkConfigurationWatcher networkConfigurationWatcher = null;

    /**
     * The threading service.
     */
    private static final ThreadingService threadingService =
            NetaddrActivator.getThreadingService();

    /**
     * Dummy IP address if we fail to get the local address we're using
     */
    private static final byte[] DUMMY_ADDRESS =
            new byte[]{(byte) 0xc0, (byte) 0xa8, (byte) 0x00, (byte) 0x64};

    /**
     * String representation of the above dummy address
     */
    public static final String DUMMY_ADDRESS_STRING = "192.168.0.100";

    /**
     * A list to store a cache of our network interfaces, as returned by the
     * native method NetworkInterface.getNetworkInterfaces().
     *
     * This cache is refreshed automatically at least every 10 seconds, but keeping
     * this cache prevents us repeatedly querying the OS when nothing has changed.
     */
    private static List<NetworkInterface> mNetworkInterfaces = new ArrayList<>();

    // Reference to macOS-specific Location Manager Delegate.  This is used
    // to query details of whether the OS will allow us to read BSSIDs.
    private LocationDelegateMac locationDelegateMac;

    // We keep track of the current set of network interfaces
    private final Map<Object, Set<InetAddress>> interfaces = new HashMap<>();

    /**
     * Constructor
     */
    public NetworkAddressManagerServiceImpl()
    {
        if (OSUtils.IS_MAC)
        {
            // Create the delegate so that it starts listening for auth callbacks.
            locationDelegateMac = new LocationDelegateMac(this);
        }
    }

    /**
      * Initializes this network address manager service implementation.
      */
     public void start()
     {
         this.localHostFinderSocket = initRandomPortSocket();

         // Listen for network configuration changes:
         addNetworkConfigurationChangeListener(this);
     }

     /**
      * Kills all threads/processes launched by this thread (if any) and
      * prepares it for shutdown. You may use this method as a reinitialization
      * technique (you'll have to call start afterwards)
      */
     public void stop()
     {
         removeNetworkConfigurationChangeListener(this);

         try
         {
             if(networkConfigurationWatcher != null)
                 networkConfigurationWatcher.halt();
         }
         finally
         {
             logger.exit();
         }
     }

    /**
     * Returns an InetAddress instance that represents the localhost, and that
     * a socket can bind upon or distribute to peers as a contact address.
     *
     * @param intendedDestination the destination that we'd like to use the
     * localhost address with.
     *
     * @return an InetAddress instance representing the local host, and that
     * a socket can bind upon or distribute to peers as a contact address.
     */
    public synchronized InetAddress getLocalHost(InetAddress intendedDestination)
    {
        InetAddress localHost = null;

        logger.trace("Querying for a localhost address for intended destination '" + intendedDestination + "'");

        /* use native code (JNI) to find source address for a specific
         * destination address on Windows XP SP1 and over.
         *
         * For other systems, we used method based on DatagramSocket.connect
         * which will returns us source address. The reason why we cannot use it
         * on Windows is because its socket implementation returns the any
         * address...
         */
        String osVersion;

        if (OSUtils.IS_WINDOWS &&
            !(osVersion = System.getProperty("os.version")).startsWith("4") && /* 95/98/Me/NT */
            !osVersion.startsWith("5.0")) /* 2000 */
        {
            byte[] src = Win32LocalhostRetriever.getSourceForDestination(intendedDestination.getAddress());

            if (src == null)
            {
                logger.warn("Failed to get localhost ");
            }
            else
            {
                try
                {
                    localHost = InetAddress.getByAddress(src);
                }
                catch(UnknownHostException uhe)
                {
                    logger.warn("Failed to get localhost", uhe);
                }
            }
        }
        else
        {
            // No point in making sure that the localHostFinderSocket is initialized. Better let it throw a NullPointerException.
            localHostFinderSocket.connect(intendedDestination, RANDOM_ADDR_DISC_PORT);
            localHost = localHostFinderSocket.getLocalAddress();
            localHostFinderSocket.disconnect();
        }

        // Windows socket implementations return the any address so we need to find something else here ...
        // InetAddress's getLocalHost() seems to work better on Windows so let's hope it'll do the trick.
        if (localHost == null)
        {
            try
            {
                // CHECKSTYLE.OFF: Regexp
                // Justified as we usually execute this only on Windows, as localHost is usually not set to null
                // by the 'else' block above
                localHost = InetAddress.getLocalHost();
                // CHECKSTYLE.ON: Regexp
            }
            catch (UnknownHostException e)
            {
                logger.warn("Failed to get localhost ", e);
            }
        }

        if (localHost == null || localHost.isAnyLocalAddress())
        {
            logger.trace("Socket returned null or the ANY local address '" + localHost + "'." + " Trying a workaround.");
            try
            {
                // All that's inside the if is an ugly IPv6 hack
                if (intendedDestination instanceof Inet6Address)
                {
                    localHost = findFirstGloballyRoutableAddress();
                }
                else
                // An IPv4 destination
                {
                    // First try the easy way  but not on Mac as this won't work and will give us the wrong value.
                    if (!OSUtils.IS_MAC)
                    {
                        // CHECKSTYLE.OFF: Regexp
                        // Justified as we are not on a Mac here
                        localHost = InetAddress.getLocalHost();
                        // CHECKSTYLE.ON: Regexp
                    }

                    // Make sure we got an IPv4 address.
                    if (!(localHost instanceof Inet4Address))
                    {
                        localHost = findFirstNonLoopbackAddress();
                    }
                }
            }
            catch (Exception e)
            {
                //sigh ... ok leave localHost as null
                logger.warn("Failed to get localhost", e);
            }
        }

        if (localHost == null || localHost.isAnyLocalAddress())
        {
            // If none of the above has worked just return a dummy address
            logger.debug("Still failed to get an IP address, use a dummy one");
            try
            {
                localHost = InetAddress.getByAddress("dummyhost", DUMMY_ADDRESS);
            }
            catch (UnknownHostException e)
            {
                logger.debug("Failed to setup dummy Inet address", e);
            }
        }

        logger.trace("Returning the localhost address '" + localHost + "'");
        return localHost;
    }

    @Nullable
    @Override
    public InetAddress getOsLocalHostWithTimeout(long timeout, TimeUnit unit)
    {
        Callable<InetAddress> worker = () -> {
            try
            {
                // CHECKSTYLE.OFF: Regexp
                // Justified as we have mitigated the potentially-bad timeout on Mac
                return InetAddress.getLocalHost();
                // CHECKSTYLE.ON: Regexp
            }
            catch (UnknownHostException e)
            {
                return null;
            }
        };

        Future<InetAddress> futureLocalAddress = threadingService.submit("getOsLocalHostWithTimeout", worker);

        try
        {
            return futureLocalAddress.get(timeout, unit);
        }
        catch (InterruptedException | ExecutionException e)
        {
            logger.error("Exception in getLocalHost(), returning null", e);
            return null;
        }
        catch (TimeoutException e)
        {
            logger.error("Timed out waiting for getLocalHost(), returning null");
            return null;
        }
    }

    /**
     * @return The first globally routable ipv6 address we find on the machine
     *         (and hope it's a good one!).
     * @throws SocketException If we hit an error getting the list of network interfaces.
     */
    private InetAddress findFirstGloballyRoutableAddress() throws SocketException
    {
        List<NetworkInterface> ifaces = getNetworkInterfaces(false);

        for (NetworkInterface iface : ifaces)
        {
            Enumeration<InetAddress> addresses = iface.getInetAddresses();

            while (addresses.hasMoreElements())
            {
                InetAddress address = addresses.nextElement();

                if ((address instanceof Inet6Address) &&
                    !address.isAnyLocalAddress() &&
                    !address.isLinkLocalAddress() &&
                    !address.isLoopbackAddress() &&
                    !address.isSiteLocalAddress())
                {
                    logger.debug("Returning first globally routable address: " +
                                 getLoggableAddress(address));
                    return address;
                }
            }
        }

        logger.debug("No globally routable address found - returning null.");
        return null;
    }

    /**
     * @return The first non-loopback interface we find.
     * @throws SocketException If we hit an error getting the list of network interfaces.
     */
    private InetAddress findFirstNonLoopbackAddress() throws SocketException
    {
        List<NetworkInterface> ifaces = getNetworkInterfaces(false);

        for (NetworkInterface iface : ifaces)
        {
            Enumeration<InetAddress> addresses = iface.getInetAddresses();

            while (addresses.hasMoreElements())
            {
                InetAddress address = addresses.nextElement();

                if ((address instanceof Inet4Address) && !address.isLoopbackAddress())
                {
                    logger.debug("Returning first non-loopback address: " +
                                 getLoggableAddress(address));
                    return address;
                }
            }
        }

        logger.debug("No non-loopback address found - returning null.");
        return null;
    }

    /**
     * Returns the hardware address (i.e. MAC address) of the specified
     * interface name.
     *
     * @param iface the <tt>NetworkInterface</tt>
     * @return array of bytes representing the layer 2 address or null if
     * interface does not exist
     */
    public byte[] getHardwareAddress(NetworkInterface iface)
    {
        try
        {
            return iface.getHardwareAddress();
        }
        catch (SocketException e)
        {
            logger.warn(
                    "MAC address of " + iface.getName() + " unavailable: " + e
                            .getMessage());
        }

        return null;
    }

    /**
     * This method gets called when a bound property is changed.
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        //there's no point in implementing this method as we have no way of
        //knowing whether the current property change event is the only event
        //we're going to get or whether another one is going to follow.

        //in the case of a STUN_SERVER_ADDRESS property change for example
        //there's no way of knowing whether a STUN_SERVER_PORT property change
        //will follow or not.

        //Reinitialization will therefore only happen if the reinitialize()
        //method is called.
        logger.debug("Property changed " + evt != null ? evt.getPropertyName() : null);
    }

    /**
     * Initializes and binds a socket on a random port number. The method
     * would try to bind on a random port and retry 5 times until a free port
     * is found.
     *
     * @return the socket that we have initialized on a random port number.
     */
    private DatagramSocket initRandomPortSocket()
    {
        DatagramSocket resultSocket = null;
        String bindRetriesStr
            = NetaddrActivator.getConfigurationService().global().getString(
                BIND_RETRIES_PROPERTY_NAME);

        int bindRetries = 5;

        if (bindRetriesStr != null)
        {
            try
            {
                bindRetries = Integer.parseInt(bindRetriesStr);
            }
            catch (NumberFormatException ex)
            {
                logger.error(bindRetriesStr
                             + " does not appear to be an integer. "
                             + "Defaulting port bind retries to " + bindRetries
                             , ex);
            }
        }

        int currentlyTriedPort = NetworkUtils.getRandomPortNumber();

        //we'll first try to bind to a random port. if this fails we'll try
        //again (bindRetries times in all) until we find a free local port.
        for (int i = 0; i < bindRetries; i++)
        {
            try
            {
                resultSocket = new DatagramSocket(currentlyTriedPort);
                //we succeeded - break so that we don't try to bind again
                break;
            }
            catch (SocketException exc)
            {
                if (!exc.getMessage().contains("Address already in use"))
                {
                    logger.fatal("An exception occurred while trying to create"
                                 + " a local host discovery socket.", exc);
                    return null;
                }
                //port seems to be taken. try another one.
                logger.debug("Port " + currentlyTriedPort
                     + " seems in use.");
                currentlyTriedPort
                    = NetworkUtils.getRandomPortNumber();
                logger.debug("Retrying bind on port "
                     + currentlyTriedPort);
            }
        }

        return resultSocket;
    }

    /**
     * Creates a <tt>DatagramSocket</tt> and binds it to the specified
     * <tt>localAddress</tt> and a port in the range specified by the
     * <tt>minPort</tt> and <tt>maxPort</tt> parameters. We first try to bind
     * the newly created socket on the <tt>preferredPort</tt> port number
     * (unless it is outside the <tt>[minPort, maxPort]</tt> range in which case
     * we first try the <tt>minPort</tt>) and then proceed incrementally upwards
     * until we succeed or reach the bind retries limit. If we reach the
     * <tt>maxPort</tt> port number before the bind retries limit, we will then
     * start over again at <tt>minPort</tt> and keep going until we run out of
     * retries.
     *
     * @param laddr the address that we'd like to bind the socket on.
     * @param preferredPort the port number that we should try to bind to first.
     * @param minPort the port number where we should first try to bind before
     * moving to the next one (i.e. <tt>minPort + 1</tt>)
     * @param maxPort the maximum port number where we should try binding
     * before giving up and throwing an exception.
     *
     * @return the newly created <tt>DatagramSocket</tt>.
     *
     * @throws IllegalArgumentException if either <tt>minPort</tt> or
     * <tt>maxPort</tt> is not a valid port number or if <tt>minPort >
     * maxPort</tt>.
     * @throws IOException if an error occurs while the underlying resolver lib
     * is using sockets.
     * @throws BindException if we couldn't find a free port between
     * <tt>minPort</tt> and <tt>maxPort</tt> before reaching the maximum allowed
     * number of retries.
     */
    public DatagramSocket createDatagramSocket(InetAddress laddr,
                                               int preferredPort,
                                               int minPort,
                                               int maxPort)
        throws IllegalArgumentException,
               IOException,
               BindException
    {
        // make sure port numbers are valid
        if (!NetworkUtils.isValidPortNumber(minPort)
                        || !NetworkUtils.isValidPortNumber(maxPort))
        {
            throw new IllegalArgumentException("minPort (" + minPort
                            + ") and maxPort (" + maxPort + ") "
                            + "should be integers between 1024 and 65535.");
        }

        // make sure minPort comes before maxPort.
        if (minPort > maxPort)
        {
            throw new IllegalArgumentException("minPort (" + minPort
                            + ") should be less than or "
                            + "equal to maxPort (" + maxPort + ")");
        }

        // if preferredPort is not  in the allowed range, place it at min.
        if (minPort > preferredPort || preferredPort > maxPort)
        {
            throw new IllegalArgumentException("preferredPort ("+preferredPort
                            +") must be between minPort (" + minPort
                            + ") and maxPort (" + maxPort + ")");
        }

        ConfigurationService config = NetaddrActivator
                        .getConfigurationService();

        int bindRetries = config.global().getInt(BIND_RETRIES_PROPERTY_NAME,
                        BIND_RETRIES_DEFAULT_VALUE);

        int port = preferredPort;
        for (int i = 0; i < bindRetries; i++)
        {
            try
            {
                // If we fail to get the local IP address use a dummy IP address in
                // our signaling, but we don't want to use that for real -
                // just allow the OS to choose the IP address.
                if (laddr.isAnyLocalAddress() ||
                    DUMMY_ADDRESS_STRING.equals(laddr.getHostAddress()))
                {
                    return new DatagramSocket(port);
                }
                else
                {
                    return new DatagramSocket(port, laddr);
                }
            }
            catch (SocketException se)
            {
                logger.info(
                "Retrying a bind because of a failure to bind to address "
                    + laddr + " and port " + port);
                logger.trace("Since you care, here's a stack:", se);
            }

            port ++;

            if (port > maxPort)
                port = minPort;
        }

        throw new BindException("Could not bind to any port between "
                        + minPort + " and " + (port -1));
    }

    /**
      * Adds new <tt>NetworkConfigurationChangeListener</tt> which will
      * be informed for network configuration changes.
      *
      * @param listener the listener.
      */
     public synchronized void addNetworkConfigurationChangeListener(
         NetworkConfigurationChangeListener listener)
     {
         if(networkConfigurationWatcher == null)
         {
             networkConfigurationWatcher = new NetworkConfigurationWatcher(this);
             networkConfigurationWatcher.start();
         }

         networkConfigurationWatcher
             .addNetworkConfigurationChangeListener(listener);
     }

     /**
      * Remove <tt>NetworkConfigurationChangeListener</tt>.
      *
      * @param listener the listener.
      */
     public synchronized void removeNetworkConfigurationChangeListener(
         NetworkConfigurationChangeListener listener)
     {
        if(networkConfigurationWatcher != null)
            networkConfigurationWatcher
                .removeNetworkConfigurationChangeListener(listener);
     }

    @Override
    public boolean isAddressWifi(InetAddress address)
    {
        logger.debug("Looking up WiFi state of " + getLoggableAddress(address));
        boolean result = false;
        NetworkConnectionInfo networkConnectionInfo = getNetworkConnectionInfoForAddress(address);

        if (networkConnectionInfo != null)
        {
            result = networkConnectionInfo.mIsWireless;
        }

        return result;
    }

    @Override
    public String getSSID(InetAddress address)
    {
        logger.debug("Looking up SSID of " + getLoggableAddress(address));
        String ssid = "";
        NetworkConnectionInfo networkConnectionInfo = getNetworkConnectionInfoForAddress(address);

        if (networkConnectionInfo != null)
        {
            ssid = networkConnectionInfo.mSSID;
        }

        return ssid;
    }

    @Override
    public boolean isConnected(InetAddress address)
    {
        NetworkConnectionInfo networkConnectionInfo = getNetworkConnectionInfoForAddress(address);
        return networkConnectionInfo != null && networkConnectionInfo.mConnected;
    }

    @Override
    public String getBSSID(InetAddress address)
    {
        logger.debug("Looking up BSSID of " + getLoggableAddress(address));
        String bssid = "";
        NetworkConnectionInfo networkConnectionInfo = getNetworkConnectionInfoForAddress(address);

        if (networkConnectionInfo != null)
        {
            bssid = networkConnectionInfo.mBSSID;
        }

        return bssid;
    }

    @Override
    public List<NetworkInterface> getNetworkInterfaces(boolean forceRefresh)
            throws SocketException
    {
        // This method logs whether or not we return the cache to help spot errors
        // that may be caused either by using the cache when something has changed
        // so we should instead refresh it, or by refreshing too often.
        // Logged as trace as they otherwise would be too spammy.
        if (forceRefresh || mNetworkInterfaces.isEmpty())
        {
            logger.trace("Refreshing network interfaces. Forced refresh? " + forceRefresh);
            mNetworkInterfaces =
                    Collections.list(NetworkInterface.getNetworkInterfaces());
        }
        else
        {
            logger.trace("Returning cache of network interfaces.");
        }

        return mNetworkInterfaces;
    }

    @Override
    public BSSIDAvailability getBSSIDAvailability()
    {
        // No logging in this function - it may be being called regularly
        // which could lead to spam, after all this is just an accessor function.
        BSSIDAvailability result;

        if (OSUtils.IS_MAC)
        {
            // macOS applies restrictions that can prevent BSSIDs from being
            // retrieved.  Get the current status.
            result = locationDelegateMac.getBSSIDAvailability();
        }
        else
        {
            // Always report BSSIDs as available when not running on macOS.
            result = BSSIDAvailability.AVAILABLE;
        }

        return result;
    }
    private NetworkConnectionInfo getNetworkConnectionInfoForAddress(InetAddress address)
    {
        return getNetworkConnectionInfoForAddress(address, false);
    }

    /**
     * @param address      The address to get the network connection info for
     * @param forceUpdate  Whether to force an update of the network info, and
     *                     overwrite any existing cached entry
     * @return A network connection info structure, which may be cached.
     */
    private NetworkConnectionInfo getNetworkConnectionInfoForAddress(InetAddress address,
                                                                     boolean forceUpdate)
    {
        NetworkConnectionInfo networkConnectionInfo;

        synchronized (addressNetworkConnectionInfoMap)
        {
            networkConnectionInfo = addressNetworkConnectionInfoMap.get(address);
        }

        if ((networkConnectionInfo == null) || forceUpdate)
        {
            // Look up the address outside the synchronized block since it can
            // take a while to work out the result.
            logger.debug("Add/update address in map: " + getLoggableAddress(address));
            try
            {
                networkConnectionInfo =
                        getNetworkConnectionInfoInternal(address, getNetworkInterfaces(forceUpdate));
                synchronized (addressNetworkConnectionInfoMap)
                {
                    addressNetworkConnectionInfoMap.put(address, networkConnectionInfo);
                }
            }
            catch (SocketException e)
            {
                logger.error("Socket error getting network interfaces", e);
            }
        }

        return networkConnectionInfo;
    }

    @VisibleForTesting
    static String getLoggableAddress(InetAddress address)
    {
        if (address != null)
        {
            // Do NOT call getHostName() - it will do a DNS query if the host hasn't been resolved yet.
            // toString() only uses a cached version, and returns <hostname>/<hostaddress>.
            return Arrays.stream(address.toString().split("/")).map(Hasher::logHasher).collect(Collectors.joining("/"));
        }
        else
        {
            return "";
        }
    }

    /**
     * Get the network connection info for the IP address.
     *
     * @param address the address to check.
     * @return NetworkConnectionInfo for the address
     */
    private static NetworkConnectionInfo getNetworkConnectionInfoInternal(InetAddress address,
                                                                          List<NetworkInterface> networkInterfaces)
    {
        logger.debug("Getting info for " + getLoggableAddress(address));
        try
        {
            if (OSUtils.IS_WINDOWS)
            {
                logger.debug("Windows");
                return new WifiInformationWindows().getNetworkConnectionInfo(
                    address.getHostAddress(), networkInterfaces);
            }
            else if (OSUtils.IS_MAC)
            {
                logger.debug("Mac");
                return new WifiInformationMac().getNetworkConnectionInfo(
                        address.getHostAddress(), networkInterfaces);
            }
            else
            {
                logger.info(String.format(
                    "Asked for info on %s but OS '%s' is unsupported",
                    getLoggableAddress(address),
                    System.getProperty("os.name")));
                return new NetworkConnectionInfo(false);
            }
        }
        catch (Throwable e)
        {
            // This isn't a critical operation, so never allow a failure to get
            // the network connection info affect the rest of the program. Catch all
            // exceptions and log.
            logger.error("Failed to get network info on " + getLoggableAddress(address), e);
            return new NetworkConnectionInfo(false);
        }
    }

    /**
     * Get the network information for all the WiFi interfaces present in the
     * system.
     *
     * @return A map from short interface name to network info, containing only
     * WiFi interfaces.
     */
    static Map<String, NetworkConnectionInfo> getWiFiInterfaceInfo(List<NetworkInterface> networkInterfaces)
    {
        logger.debug("Getting network info for all WiFi interfaces");
        try
        {
            if (OSUtils.IS_WINDOWS)
            {
                logger.debug("Windows");
                return new WifiInformationWindows().getWiFiInterfaceInfo(networkInterfaces);
            }
            else if (OSUtils.IS_MAC)
            {
                logger.debug("Mac");
                return new WifiInformationMac().getWiFiInterfaceInfo(networkInterfaces);
            }
            else
            {
                logger.info(System.getProperty("os.name") + "' is unsupported");
                return new HashMap<>(0);
            }
        }
        catch (Throwable e)
        {
            // This isn't a critical operation, so never allow a failure to get
            // the network connection info affect the rest of the program. Catch all
            // exceptions and log.
            logger.error("Failed to get wifi info", e);
            return new HashMap<>(0);
        }
    }

    private void startNetworkLookupThread(final InetAddress address)
    {
        startNetworkLookupThread(address, false);
    }

    /**
     * Start a new thread to look up the network info for the IP address.
     * Note that this is required since the look up can be very slow.
     *
     * @param address the address to look up.
     */
    private void startNetworkLookupThread(final InetAddress address, boolean forceUpdate)
    {
        String threadName = "InetAddress lookup thread " + getLoggableAddress(address);
        Runnable networkLookupThread = () -> {
            // This call will trigger us to cache the wireless info.
            String loggableAddress = getLoggableAddress(address);
            logger.debug("Looking up address: " + loggableAddress + ". Force update? " + forceUpdate);
            NetworkConnectionInfo connectionInfo =
                    getNetworkConnectionInfoForAddress(address, forceUpdate);

            // Determine the emergency location address
            CommPortalService commPortalService = NetaddrActivator.getCommPortalService();
            if (commPortalService != null && commPortalService.isEmergencyLocationSupportNeeded())
            {
                // If we've been asked to force update, this means something may have changed
                // about the network that we're not automatically notified about.  In that case,
                // we run determineLocationAddress to check for emergency location for all
                // addresses on the system.  Otherwise, we just check for the address we've
                // been asked to lookup, as long as it is connected.
                if (forceUpdate)
                {
                    commPortalService.determineLocationAddress();
                }
                else if (connectionInfo.mConnected)
                {
                    logger.debug("Address is connected: " + loggableAddress);
                    commPortalService.findLocationForAddress(address);
                }
                else
                {
                    logger.debug("Address is not connected: " + loggableAddress +
                                 ". Not looking for location info.");
                }
            }
            else
            {
                logger.debug("Emergency Location Support not needed");
            }
        };

        threadingService.submit(threadName, networkLookupThread);
    }

    @Override
    public void configurationChanged(ChangeEvent event)
    {
        // All configuration changes need us to fetch the network info for the
        // affected address.
        InetAddress address = event.getAddress();
        logger.debug("Configuration change " + event);
        Object source = event.getSource();

        switch (event.getType())
        {
        case ChangeEvent.ADDRESS_UP:
            // Look for any existing addresses for this source
            Set<InetAddress> addresses = interfaces.get(source);
            if (addresses == null)
            {
                // This is a new source we have not seen yet, so start with an
                // empty set of addresses
                addresses = new HashSet<>();
                interfaces.put(source, addresses);
            }

            // It doesn't make sense for this to be called with a null address
            // but let's be ultra defensive
            if (address != null)
            {
                addresses.add(address);
                startNetworkLookupThread(address);
            }
            break;

        case ChangeEvent.IFACE_UP:
            // Nothing to do - we are only told the interface is up after we
            // have been told about all the individual addresses, and we have
            // been acting on each of those individual addresses anyway.
            break;

        case ChangeEvent.ADDRESS_DOWN:
            // Look for any existing addresses for this source.
            // Since we often only get ADDRESS_DOWN after the IFACE_DOWN
            // has already been called, this may return null
            addresses = interfaces.get(source);
            if (addresses != null)
            {
                addresses.remove(address);
            }

            reloadAddressNetworkConnectionMap();
            break;

        case ChangeEvent.IFACE_DOWN:
            interfaces.remove(source);

            // Clear the map as it's most likely not relevant anymore. Then
            // re-populate it:
            reloadAddressNetworkConnectionMap();
            break;

        case ChangeEvent.WIFI_INFO_CHANGED:
            // There has been a change in WiFi info for this interface (e.g.
            // BSSID) - load in the new details.  We force an update, as we know
            // we need to update the WiFi info, even though the interface hasn't
            // gone up or down.
            startNetworkLookupThread(address, true);
            break;
        }

        StringBuilder interfacesSummary = new StringBuilder("==Network interfaces==:\n");
        for (Object i : interfaces.keySet())
        {
            interfacesSummary.append(i).append("[");
            Set<InetAddress> addresses = interfaces.get(i);

            for (InetAddress inetAddress : addresses)
            {
                NetworkConnectionInfo networkConnectionInfo = getNetworkConnectionInfoForAddress(inetAddress);

                interfacesSummary.append(getLoggableAddress(inetAddress))
                        .append(networkConnectionInfo == null ? " null" : "")
                        .append(networkConnectionInfo != null && networkConnectionInfo.mIsWireless ? " wifi" : "")
                        .append(networkConnectionInfo != null && networkConnectionInfo.mConnected ? " connected" : "")
                        .append(",");
            }
            interfacesSummary.append("]\n");
        }
        interfacesSummary.append("==end==");

        logger.debug(interfacesSummary);
    }

    @Override
    public void reloadAddressNetworkConnectionMap()
    {
        logger.info("Reloading Address Network Connection Map");
        synchronized (addressNetworkConnectionInfoMap)
        {
            addressNetworkConnectionInfoMap.clear();
        }

        List<NetworkInterface> interfaces;
        try
        {
            // We've been asked to reload so ensure we refresh the cache of network interfaces.
            interfaces = getNetworkInterfaces(true);

            int threadCount = 0;

            // Every interface
            for (NetworkInterface networkInterface : interfaces)
            {
                logger.debug("Examining network interface " + networkInterface);

                Enumeration<InetAddress> addresses =
                                            networkInterface.getInetAddresses();

                // Every address on the interface
                while (addresses.hasMoreElements())
                {
                    InetAddress address = addresses.nextElement();
                    threadCount++;
                    startNetworkLookupThread(address);
                }
            }

            logger.info("spun up " + threadCount + " threads to do network connection lookups");
        }
        catch (SocketException e)
        {
            // Not a lot that can be done - the map will be re-populated by the
            // requests as they come in.
            logger.error("Exception getting network interfaces", e);
        }
    }

    @Override
    public void requestLocationPermission()
    {
        locationDelegateMac.requestAuthorization();
    }
}
