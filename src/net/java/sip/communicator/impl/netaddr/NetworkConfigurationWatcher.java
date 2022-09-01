/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import java.net.*;
import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.*;

/**
 * Periodically checks the current network interfaces to track changes
 * and fire events on those changes.
 *
 * @author Damian Minkov
 */
public class NetworkConfigurationWatcher extends Thread
    implements SystemActivityChangeListener,
               ServiceListener,
               Runnable
{
    /**
     * Our class logger.
     */
    private static  Logger logger =
        Logger.getLogger(NetworkConfigurationWatcher.class);

    /**
     * The current active interfaces.
     */
    private Map<String, List<InetAddress>> activeInterfaces
            = new HashMap<>();

    /**
     * Interval between checks of the current network configuration.
     */
    private static final int CHECK_INTERVAL = 10000; // 10 sec.

    /**
     * Maximum amount of time between interface check logs.
     */
    private static final int LOGGING_INTERVAL_SECONDS = 600; // 10 mins

    /**
     * Description text of the Error thrown in getNetworkInterfaces()
     * that we wish to swallow and log
     */
    private static final String SFR540880_ERROR_DESCRIPTION = "IP Helper Library GetIpAddrTable function failed";

    /**
     * Whether thread checking for network notifications is running.
     */
    private boolean isRunning = false;

    /**
     * This will be set to true if we find that the state of the network
     * connections has changed since we last checked. If so, we'll check
     * whether we have have become (dis)connected to/from a captive WiFi
     * network. Default this to true so that we always check this the first
     * time that the network interfaces are checked.
     */
    private boolean networkChanged = true;

    /**
     * Service we use to listen for network changes.
     */
    private SystemActivityNotificationsService
            systemActivityNotificationsService = null;

    /**
     * The thread dispatcher of network change events.
     */
    private NetworkEventDispatcher eventDispatcher =
            new NetworkEventDispatcher();

    private final NetworkAddressManagerServiceImpl mNetworkAddressManagerService;

    /**
     * HashMap to memoize the result of interface.isLoopback() for each
     * interface we have encountered. We do this because the method call has
     * been found to take ~100ms in practice, which is too long for large
     * numbers of interfaces.
     */
    private final HashMap<NetworkInterface, Boolean> loopbackCheckCache =
            new HashMap<>();

    /**
     * Boolean which shows if AD was restricted by a captive portal on last check.
     */
    private boolean mWasPreviousStateCaptivePortal = false;

    private Map<String, NetworkConnectionInfo> wifiInterfaces = new HashMap<>(0);

    /**
     * Inits configuration watcher.
     */
    NetworkConfigurationWatcher(NetworkAddressManagerServiceImpl networkAddressManagerService)
    {
        super("NetworkConfigurationWatcher");
        mNetworkAddressManagerService = networkAddressManagerService;

        try
        {
            checkNetworkInterfaces(mNetworkAddressManagerService.getNetworkInterfaces(false),
                                   false, 0, true);
        }
        catch (SocketException e)
        {
            logger.error("Error checking network interfaces", e);
        }
        catch (Error e)
        {
            processJavaLangError(e);
        }
    }

    /**
     * Adds new <tt>NetworkConfigurationChangeListener</tt> which will
     * be informed for network configuration changes.
     * @param listener the listener.
     */
    void addNetworkConfigurationChangeListener(
        NetworkConfigurationChangeListener listener)
    {
        eventDispatcher.addNetworkConfigurationChangeListener(listener);

        initialFireEvents(listener);

        NetaddrActivator.getBundleContext().addServiceListener(this);

        if(this.systemActivityNotificationsService == null)
        {
            SystemActivityNotificationsService systActService
                = ServiceUtils.getService(
                        NetaddrActivator.getBundleContext(),
                        SystemActivityNotificationsService.class);

            handleNewSystemActivityNotificationsService(systActService);
        }
    }

    /**
     * Used to fire initial events to newly added listeners.
     * @param listener the listener to fire.
     */
    private void initialFireEvents(
            NetworkConfigurationChangeListener listener)
    {
        try
        {
            if (mWasPreviousStateCaptivePortal)
            {
                // We have recently detected that we are restricted by a captive wifi portal.
                NetworkEventDispatcher.fireChangeEvent(new ChangeEvent(
                    this, ChangeEvent.NOW_RESTRICTED_BY_CAPTIVE_WIFI), listener);
            }

            List<NetworkInterface> networkInterfaces =
                    mNetworkAddressManagerService.getNetworkInterfaces(false);

            for (NetworkInterface networkInterface : networkInterfaces)
            {
                // Ignore any loopback interfaces; they don't give us network
                // connectivity so we don't care about them.
                if(isLoopback(networkInterface))
                    continue;

                // If the interface is up and has some valid (i.e. non-local)
                // address, then add it to our list of active interfaces.
                if(networkInterface.isUp())
                {
                    Enumeration<InetAddress> as =
                        networkInterface.getInetAddresses();
                    boolean hasAddress = false;
                    while (as.hasMoreElements())
                    {
                        InetAddress inetAddress = as.nextElement();
                        if(inetAddress.isLinkLocalAddress())
                            continue;

                        hasAddress = true;
                        NetworkEventDispatcher.fireChangeEvent(
                            new ChangeEvent(
                                    networkInterface.getName(),
                                    ChangeEvent.ADDRESS_UP,
                                    inetAddress,
                                    false,
                                    true),
                            listener);
                    }

                    if(hasAddress)
                        NetworkEventDispatcher.fireChangeEvent(
                            new ChangeEvent(networkInterface.getName(),
                                ChangeEvent.IFACE_UP, null, false, true),
                            listener);
                }
            }
        }
        catch (SocketException e)
        {
            logger.error("Error checking network interfaces", e);
        }
        catch (Error e)
        {
            processJavaLangError(e);
        }
    }

    /**
     * Saves the reference for the service and
     * add a listener if the desired events are supported. Or start
     * the checking thread otherwise.
     * @param newService
     */
    private void handleNewSystemActivityNotificationsService(
            SystemActivityNotificationsService newService)
    {
        if(newService == null)
            return;

        this.systemActivityNotificationsService = newService;
        this.systemActivityNotificationsService.addSystemActivityChangeListener(this);
    }

    /**
     * Remove <tt>NetworkConfigurationChangeListener</tt>.
     * @param listener the listener.
     */
    void removeNetworkConfigurationChangeListener(
        NetworkConfigurationChangeListener listener)
    {
        eventDispatcher.removeNetworkConfigurationChangeListener(listener);
    }

    /**
     * When new protocol provider is registered we add needed listeners.
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        ServiceReference<?> serviceRef = serviceEvent.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know we are shutting down
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object sService = NetaddrActivator.getBundleContext()
                .getService(serviceRef);

        if(sService instanceof SystemActivityNotificationsService)
        {
            switch (serviceEvent.getType())
            {
                case ServiceEvent.REGISTERED:
                    if(this.systemActivityNotificationsService != null)
                        break;

                    handleNewSystemActivityNotificationsService(
                        (SystemActivityNotificationsService)sService);
                    break;
                case ServiceEvent.UNREGISTERING:
                    ((SystemActivityNotificationsService)sService)
                        .removeSystemActivityChangeListener(this);
                    break;
            }

            return;
        }
    }

    /**
     * Stop.
     */
    public void halt()
    {
        logger.info("NetworkConfigurationWatcher halted");

        if(isRunning)
        {
            synchronized(this)
            {
                isRunning = false;
                notifyAll();
            }
        }

        if(eventDispatcher != null)
            eventDispatcher.stop();
    }

    /**
     * This method gets called when a notification action for a particular event
     * type has been changed. We are interested in sleep and network
     * changed events.
     *
     * @param event the <tt>NotificationActionTypeEvent</tt>, which is
     * dispatched when an action has been changed.
     */
    public void activityChanged(SystemActivityEvent event)
    {
        logger.info("Notified of SystemActivityEvent: " + event);

        if(event.getEventID() == SystemActivityEvent.EVENT_SLEEP)
        {
            // on standby lets fire down to all interfaces
            // so they can reconnect
            logger.info("Sleep event detected. Down all interfaces");
            downAllInterfaces();
        }
        else if(event.getEventID() == SystemActivityEvent.EVENT_NETWORK_CHANGE)
        {
            try
            {
                // We've been notified of a network change, so make sure we refresh the cache of network interfaces.
                checkNetworkInterfaces(mNetworkAddressManagerService.getNetworkInterfaces(true),
                                       true, 0, true);
            }
            catch (SocketException e)
            {
                logger.error("Error checking network interfaces", e);
            }
            catch (Error e)
            {
                processJavaLangError(e);
            }
        }
        else if(event.getEventID() == SystemActivityEvent.EVENT_DNS_CHANGE)
        {
            try
            {
                eventDispatcher.fireChangeEvent(
                    new ChangeEvent(event.getSource(), ChangeEvent.DNS_CHANGE));
            }
            catch(Throwable t)
            {
                logger.error("Error dispatching dns change.");
            }
        }
    }

    /**
     * Down all interfaces and fire events for it.
     */
    private void downAllInterfaces()
    {
        Iterator<String> iter = activeInterfaces.keySet().iterator();
        while (iter.hasNext())
        {
            String niface = iter.next();
            eventDispatcher.fireChangeEvent(new ChangeEvent(niface,
                    ChangeEvent.IFACE_DOWN, true));
        }
        activeInterfaces.clear();
    }

    /**
     * Actively poll for information on the WiFi interfaces, even if we haven't
     * been informed of any changes.  This is necessary to catch cases where
     * multiple WiFi Access Points in a mesh use the same SSID, but different
     * BSSIDs.  If we detect any such changes, then fire events that will cause
     * a complete re-scanning of the affected interfaces/addresses, including
     * dependent information like location (for emergency calling).
     *
     * @param networkInterfaces the list of all network interfaces on the system.
     */
    private void pollWiFiInterfaces(List<NetworkInterface> networkInterfaces)
    {
        // Get a map of the current WiFi information, indexed by interface name.
        Map<String, NetworkConnectionInfo> newWifiInts =
                NetworkAddressManagerServiceImpl.getWiFiInterfaceInfo(networkInterfaces);

        // We are not attempting to notice changes in the number of interfaces, or interfaces going
        // up and down (that is handled elsewhere).  All we're looking for is any changes to the
        // info (e.g. SSID/BSSID) for active WiFi interfaces.
        for (String iface : activeInterfaces.keySet())
        {
            if (wifiInterfaces.containsKey(iface) && newWifiInts.containsKey(iface) &&
                !wifiInterfaces.get(iface).equals(newWifiInts.get(iface)))
            {
                logger.debug("Detected WiFi changes for interface: " + iface);
                for (InetAddress addr : activeInterfaces.get(iface))
                {
                    eventDispatcher.fireChangeEvent(new ChangeEvent(iface, ChangeEvent.WIFI_INFO_CHANGED, addr));
                }
            }
        }

        // Save off the new WiFi interface information so that we can compare next time.
        wifiInterfaces = newWifiInts;
    }

    /**
     * Check the currently active interfaces to see whether any new interfaces
     * are available, or any previously-active ones have gone offline.
     * Also check the addresses available on each interface to see whether they
     * have changed since the last poll.
     * Fire IFACE_UP, IFACE_DOWN, ADDRESS_UP and ADDRESS_DOWN events as
     * appropriate.
     * @param networkInterfaces the network interfaces to check
     * @param fireEvents whether to fire IFACE_UP/DOWN and ADDRESS_UP/DOWN
     * events if we notice a change in configuration. Usually this should be
     * <tt>true</tt>, but is set to false on first run to query active interfaces
     * without firing events.
     * @param upEventDelay milliseconds to wait before firing IFACE_UP and
     * ADDRESS_UP events, as the OS may notify us of new interfaces and
     * addresses before they have full connectivity (e.g. dns resolution on
     * linux)
     * @param forceLogging true if we should write the result of the check to a
     * log, even if a similar log has been written recently.
     */
    private void checkNetworkInterfaces(
            List<NetworkInterface> networkInterfaces,
            boolean fireEvents,
            int upEventDelay,
            boolean forceLogging)
        throws SocketException
    {
        long checkStartTime = System.currentTimeMillis();

        // Map of interface name (e.g. eth0) against the list of active
        // addresses on that interface.
        Map<String, List<InetAddress>> latestInterfaces =
                new HashMap<>();

        // Query currently-active interfaces and store them to latestInterfaces.
        for (NetworkInterface networkInterface : networkInterfaces)
        {
            if(isLoopback(networkInterface))
                continue;

            List<InetAddress> addresses = getAddresses(networkInterface);
            if (addresses.size() == 0)
                continue;

            if (networkInterface.isUp())
            {
                latestInterfaces.put(networkInterface.getName(), addresses);
            }
        }

        long checkDuration = System.currentTimeMillis() - checkStartTime;
        String logHeading = "Result of interface state check:\n" +
                            "Check duration: " + checkDuration + "ms\n";

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Previously Active:\n");
        for (Map.Entry<String, List<InetAddress>> en : activeInterfaces.entrySet())
        {
            logMessage.append(en.getKey());
            logMessage.append(":");
            logMessage.append(en.getValue());
            logMessage.append("\n");
        }

        logMessage.append("Currently Active:\n");
        for (Map.Entry<String, List<InetAddress>> en : latestInterfaces.entrySet())
        {
            logMessage.append(en.getKey());
            logMessage.append(":");
            logMessage.append(en.getValue());
            logMessage.append("\n");
        }

        // Convert to string, ignoring the last newline.
        String logMessageStr = logMessage.substring(0, logMessage.length() - 1);

        if (forceLogging)
        {
            // We've explicitly been asked to log the interface details.
            logger.info(logHeading + logMessageStr);
        }
        else
        {
            // We've not been asked to log interface details, but do so anyway
            // if either we haven't logged them in a while, or if the interface
            // list has changed.
            logger.interval(LOGGING_INTERVAL_SECONDS,
                            this.getClass().getName(),
                            logHeading,
                            logMessageStr);
        }

        // Build a list of all newly-down interfaces (i.e. those we previously
        // thought were active but were missing from our latest query).
        List<String> deadInterfaces = new ArrayList<>(
                activeInterfaces.keySet());
        List<String> latestSet = new ArrayList<>(latestInterfaces.keySet());
        deadInterfaces.removeAll(latestSet);

        // Fire IFACE_DOWN events for all expired interfaces, and ADDRESS_DOWN
        // events for all associated addresses.
        for (int i = 0; i < deadInterfaces.size(); i++)
        {
            String iface = deadInterfaces.get(i);

            if (!latestSet.contains(iface))
            {
                networkChanged = true;

                if (fireEvents)
                {
                    eventDispatcher.fireChangeEvent(new ChangeEvent(
                                                iface, ChangeEvent.IFACE_DOWN));
                }

                List<InetAddress> addresses = activeInterfaces.get(iface);
                if (addresses != null)
                {
                    for (InetAddress address : addresses)
                    {
                        if (fireEvents)
                        {
                            eventDispatcher.fireChangeEvent(
                                new ChangeEvent(address,
                                                ChangeEvent.ADDRESS_DOWN));
                        }
                    }
                }

                activeInterfaces.remove(iface);
            }
        }

        // Now check whether any still-active interfaces have lost any of their
        // bound addresses.
        Iterator<Map.Entry<String, List<InetAddress>>> latestIfaceIter =
                                         activeInterfaces.entrySet().iterator();
        while (latestIfaceIter.hasNext())
        {
            Map.Entry<String, List<InetAddress>> entry = latestIfaceIter.next();
            Iterator<InetAddress> addresses = entry.getValue().iterator();

            // Iterate over the addresses previously associated with the
            // interface. If an address is missing from our latest query data,
            // then it has gone down.
            while (addresses.hasNext())
            {
                InetAddress addr = addresses.next();

                List<InetAddress> newAddresses = latestInterfaces.get(
                                                                entry.getKey());

                if (newAddresses != null && !newAddresses.contains(addr))
                {
                    networkChanged = true;

                    if (fireEvents)
                    {
                        eventDispatcher.fireChangeEvent(new ChangeEvent(
                               entry.getKey(), ChangeEvent.ADDRESS_DOWN, addr));
                    }

                    // Remove 'addr' from the activeInterfaces map.
                    addresses.remove();
                }
            }
        }

        if (upEventDelay > 0 && latestInterfaces.size() != 0)
        {
            // Wait a while before firing UP events, as the OS may notify us
            // of new interfaces / addresses prematurely - they may not yet
            // be fully connected.
            synchronized(this)
            {
                try
                {
                    wait(upEventDelay);
                }
                catch (InterruptedException ex) {}
            }
        }

        // Now check our interfaces to see whether they have acquired any
        // new active addresses in our most recent query.  If we find any
        // changes, store the ChangeEvents in a list to fire later.  This is
        // because we want to fire the events after checking whether we're
        // restricted by a captive WiFi portal, but we only actually want to
        // check whether that is the case if we know that something in the
        // network has changed.
        List<ChangeEvent> upEventsToFire = new ArrayList<>();
        latestIfaceIter = latestInterfaces.entrySet().iterator();
        while (latestIfaceIter.hasNext())
        {
            Map.Entry<String, List<InetAddress>> iface = latestIfaceIter.next();

            // Iterate over the most recent addresses for the interface.
            // Anything not in activeInterfaces is a newly-added address.
            for (InetAddress addr : iface.getValue())
            {
                List<InetAddress> oldAddresses = activeInterfaces.get(
                                                                iface.getKey());
                if (oldAddresses != null && !oldAddresses.contains(addr))
                {
                    networkChanged = true;

                    if (fireEvents)
                    {
                        upEventsToFire.add(new ChangeEvent(
                                 iface.getKey(), ChangeEvent.ADDRESS_UP, addr));
                    }

                    oldAddresses.add(addr);
                }
            }
        }

        // Finally, add any newly-active interfaces (i.e. those in our latest
        // query, but not yet in activeInterfaces). Fire IFACE_UP for each,
        // and also fire ADDRESS_UP for each associated address.
        Iterator<String> oldInterfaces = activeInterfaces.keySet().iterator();

        // Remove all previously-known interfaces from the latestInterfaces
        // list, leaving only those for which we need to fire events.
        while (oldInterfaces.hasNext())
        {
            latestInterfaces.remove(oldInterfaces.next());
        }

        latestIfaceIter = latestInterfaces.entrySet().iterator();
        while (latestIfaceIter.hasNext())
        {
            networkChanged = true;
            final Map.Entry<String, List<InetAddress>> entry =
                                                         latestIfaceIter.next();
            for(InetAddress addr : entry.getValue())
            {
                if (fireEvents)
                {
                    upEventsToFire.add(new ChangeEvent(
                                 entry.getKey(), ChangeEvent.ADDRESS_UP, addr));
                }
            }

            if (fireEvents)
            {
                upEventsToFire.add(
                        new ChangeEvent(entry.getKey(), ChangeEvent.IFACE_UP));
            }

            activeInterfaces.put(entry.getKey(), entry.getValue());
        }

        if (fireEvents)
        {
            // Before firing UP events, check if we are restricted by a
            // captive wifi portal. If an interface is UP, it is useless if we
            // are connected to a captive portal, so inform listeners of this
            // first.
            if (networkChanged)
            {
                checkCaptivePortal();
                networkChanged = false;
            }

            if (!upEventsToFire.isEmpty())
            {
                // if we haven't waited before, lets wait here
                // and give time to underlying os to configure fully the
                // network interface (receive and store dns config)
                if (upEventDelay == 0)
                {
                    synchronized(this)
                    {
                        try
                        {
                            wait(500);
                        }
                        catch (InterruptedException ex) {}
                    }
                }

                for (ChangeEvent changeEvent : upEventsToFire)
                {
                    eventDispatcher.fireChangeEvent(changeEvent);
                }
            }
        }
    }

    /**
     * Main loop of this thread.
     */
    public void run()
    {
        long last = 0;
        boolean isAfterStandby = false;
        isRunning = true;
        logger.info("NetworkConfigurationWatcher has started");

        while(isRunning)
        {
            long curr = System.currentTimeMillis();

            // if time spent between checks is more than 4 times
            // longer than the check interval we consider it as a
            // new check after standby
            if(!isAfterStandby && last != 0)
                isAfterStandby = (last + 4*CHECK_INTERVAL - curr) < 0;

            if(isAfterStandby)
            {
                // oo standby lets fire down to all interfaces
                // so they can reconnect
                downAllInterfaces();

                // we have fired events for standby, make it to false now
                // so we can calculate it again next time
                isAfterStandby = false;

                last = curr;

                // give time to interfaces
                synchronized(this)
                {
                    try{
                        wait(CHECK_INTERVAL);
                    }
                    catch (Exception e){}
                }

                continue;
            }

            try
            {
                boolean networkIsUP = activeInterfaces.size() > 0;

                // We're checking for changes in the network interfaces,
                // so make sure we refresh the cache.
                List<NetworkInterface> networkInterfaces =
                        mNetworkAddressManagerService.getNetworkInterfaces(true);
                checkNetworkInterfaces(networkInterfaces, true, 1000, false);

                CommPortalService commPortalService = NetaddrActivator.getCommPortalService();
                if (commPortalService != null && commPortalService.isEmergencyLocationSupportNeeded())
                {
                    // Poll the active WiFi interfaces so that we can discover any changes to
                    // BSSID that occur without the interface going up/down.  This uses quite
                    // a lot of resources, so only do this if we need emergency location support.
                    pollWiFiInterfaces(networkInterfaces);
                }

                // fire that network has gone up
                if(!networkIsUP && activeInterfaces.size() > 0)
                {
                    isAfterStandby = false;
                }

                // save the last time that we checked
                last = System.currentTimeMillis();
            }
            catch (SocketException e)
            {
                logger.error("Error checking network interfaces", e);
            }
            catch (Error e)
            {
                processJavaLangError(e);
            }

            synchronized(this)
            {
                try
                {
                    wait(CHECK_INTERVAL);
                }
                catch (InterruptedException e)
                {
                    // No harm done by waking early.
                }
            }
        }

        logger.info("NetworkConfigurationWatcher has stopped.");
    }

    /**
     * Check whether the given interface is loopback or not. Cache the result to
     * avoid an expensive OS call each time we see the interface.
     * @param iface The interface to check
     * @return True if the interface is loopback; false otherwise.
     * @throws SocketException if the OS call fails.
     */
    private boolean isLoopback(NetworkInterface iface) throws SocketException
    {
        Boolean result = loopbackCheckCache.get(iface);

        if (result == null)
        {
            result = iface.isLoopback();
            loopbackCheckCache.put(iface, result);
        }

        return result;
    }

    /**
     * Gets the InetAddresses associated with the given interface.
     * @param iface The interface to query.
     * @return A List of InetAddresses attached to the interface, excluding any
     * link-local addresses.
     */
    private List<InetAddress> getAddresses(NetworkInterface iface)
    {
        List<InetAddress> addresses = new ArrayList<>();

        Enumeration<InetAddress> as = iface.getInetAddresses();
        while (as.hasMoreElements())
        {
            InetAddress inetAddress = as.nextElement();

            if (inetAddress.isLinkLocalAddress())
                continue;

            addresses.add(inetAddress);
        }

        return addresses;
    }

    /**
     * Checks if the user is restricted by a captive wifi portal. A change event is
     * fired when the user becomes restricted or unrestricted by a wifi portal
     */
    private synchronized void checkCaptivePortal()
    {
        boolean isCaptivePortal = CaptiveWiFiUtils.isCaptivePortal();

        if (!mWasPreviousStateCaptivePortal && isCaptivePortal)
        {
            // AD is now restricted by a captive wifi portal, when it wasn't before
            logger.debug("Now restricted by a captive wifi portal");
            eventDispatcher.fireChangeEvent(new ChangeEvent(
                this, ChangeEvent.NOW_RESTRICTED_BY_CAPTIVE_WIFI));
        }

        if (mWasPreviousStateCaptivePortal && !isCaptivePortal)
        {
            // AD previously was restricted by a captive wifi portal, but now is not
            logger.debug("No longer restricted by a captive wifi portal");
            eventDispatcher.fireChangeEvent(new ChangeEvent(
                this, ChangeEvent.NO_LONGER_RESTRICTED_BY_CAPTIVE_WIFI));
        }

        mWasPreviousStateCaptivePortal = isCaptivePortal;
    }

    /*
     * An Error can be thrown from native code called by getNetworkInterfaces()
     * This error has been seen since V2.28 (see SFR 540880)
     * We don't know what causes it and it does not seem to cause
     * any user-visible symptoms
     *
     * This method swallows and logs the error if it is related to SFR 540880
     * (determined from the description) and re-throws it otherwise
     */
    private void processJavaLangError(Error e)
    {
        if (e.getMessage().contains(SFR540880_ERROR_DESCRIPTION))
        {
            logger.error("Error checking network interfaces: "
                         + e.getMessage(), e);
        }
        else
        {
            throw e;
        }

    }
}