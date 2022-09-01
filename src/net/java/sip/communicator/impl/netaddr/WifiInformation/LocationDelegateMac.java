// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import static ca.weblite.objc.RuntimeUtils.sel;

import ca.weblite.objc.Client;
import ca.weblite.objc.NSObject;
import ca.weblite.objc.Proxy;
import ca.weblite.objc.annotations.Msg;

import net.java.sip.communicator.impl.netaddr.NetworkAddressManagerServiceImpl;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService.BSSIDAvailability;
import net.java.sip.communicator.util.Logger;

public class LocationDelegateMac extends NSObject
{
    // Our class logger.
    private static final Logger logger = Logger.getLogger(LocationDelegateMac.class);

    private Proxy proxy;

    enum AuthStatus
    {
        // This is not a valid status
        INVALID(-1),

        // The authorization status has not yet been determined
        NOT_DETERMINED(0),

        // The app has restricted access to location services
        RESTRICTED(1),

        // The app has been denied access to location services (although this
        // could just be because location services as a whole are turned off)
        DENIED(2),

        // The app is authorized to use location services, even when running in
        // the background
        AUTHORIZED_ALWAYS(3),

        // The app is authorized to use location services when running in the
        // foreground (although macOS never grants this permission)
        AUTHORIZED_WHEN_IN_USE(4);

        private final int value;
        AuthStatus(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return this.value;
        }

        public static AuthStatus fromValue(int value)
        {
            AuthStatus result = AuthStatus.INVALID;
            for (AuthStatus status : AuthStatus.values())
            {
                if (status.getValue() == value)
                {
                    result = status;
                }
            }
            return result;
        }
    }

    static
    {
        // Load the CoreLocation library.  This is required in order to request
        // the permissions necessary for retrieving the BSSIDs for WiFi
        // networks.  The location of a machine can often be inferred through
        // BSSID lookups, hence the restriction under the location API.
        MacLibraryUtils.load("CoreLocation",
                             "/System/Library/Frameworks/CoreLocation.framework/CoreLocation");
    }

    // The service to notify when access to BSSIDs changes.
    private final NetworkAddressManagerServiceImpl netAddrSvc;

    // Current availability of BSSIDs.
    private BSSIDAvailability bssidAvailability = BSSIDAvailability.UNKNOWN;

    /**
     * Constructor
     *
     * This is required to ensure that this class gets set up as an Objective-C
     * "NSObject", allowing it to be invoked on callbacks from the macOS
     * location manager.
     *
     * @param service The Network Address Manager Service that created this
     *                delegate
     */
    public LocationDelegateMac(NetworkAddressManagerServiceImpl service)
    {
        super();
        init("NSObject");
        netAddrSvc = service;

        // Get the Objective-C main thread to call our start() method.  This
        // will allow future Objective-C callbacks to invoke this instance of
        // the class (these callbacks must occur on the same thread that created
        // the location manager instance).
        logger.debug("Registering for location manager callbacks");
        this.send("performSelectorOnMainThread:withObject:waitUntilDone:",
                  sel("start"), this, false);
    }

    // According to the documentation, on macOS 11 the delegate method that is
    // called was changed, but as we still support the older version, simply
    // pipe the new version through to our older method.
    // However, even on macOS 11 the old version is still being called.
    @Msg(selector="locationManager:didChangeAuthorization:", signature="v@:@")
    public void locationManagerDidChangeAuthorization(Proxy manager)
    {
        logger.debug("Callback locationManager:didChangeAuthorization: " +
                     "called with manager: " + manager);

        int statusInt = (Integer)proxy.send("authorizationStatus");
        locationManagerDidChangeAuthorizationStatus(manager, statusInt);
    }

    /**
     * Callback invoked by the macOS CLLocationManager whenever the location
     * permissions status changes.  This will always be called at least once
     * when we create the CLLocationManager instance, to tell us the initial state.
     *
     * @param manager The manager instance that is calling us back
     * @param statusInt The new status code
     */
    @Msg(selector="locationManager:didChangeAuthorizationStatus:", signature="v@:@i")
    public void locationManagerDidChangeAuthorizationStatus(Proxy manager, int statusInt)
    {
        AuthStatus status = AuthStatus.fromValue(statusInt);
        logger.debug("Callback locationManager:didChangeAuthorizationStatus: " +
                     "called with manager: " + manager + ", status: " + status.name());

        // Determine the availability state, which is a combination of whether
        // location services are enabled and the authorization state.  We do not
        // separately report the authorization state, as it is, itself, affected
        // by whether location services are enabled.
        if (status.equals(AuthStatus.NOT_DETERMINED))
        {
            logger.info("App has never asked for location permissions");
            bssidAvailability = BSSIDAvailability.UNKNOWN;
        }
        else if (!manager.sendBoolean("locationServicesEnabled"))
        {
            logger.info("Location services are turned off");
            bssidAvailability = BSSIDAvailability.BLOCKED_LOCATION_SERVICES;
        }
        else if (status.equals(AuthStatus.AUTHORIZED_ALWAYS))
        {
            logger.debug("Location services on, app has permissions");
            bssidAvailability = BSSIDAvailability.AVAILABLE;
        }
        else
        {
            logger.info("Location services on, app doesn't have permissions");
            bssidAvailability = BSSIDAvailability.BLOCKED_APP_PERMISSIONS;
        }

        // Now clear the cache of WiFi information, and re-read it.  We do this
        // every time we get any authorization status update, to ensure that
        // we have the latest information.
        logger.debug("Mac location permission changed - reloading network info.");
        netAddrSvc.reloadAddressNetworkConnectionMap();
    }

    /**
     * Callback invoked by the macOS CLLocationManager whenever an error occurs.
     *
     * @param manager The manager instance that is calling us back
     * @param error The error
     */
    @Msg(selector="locationManager:didFailWithError:", signature="v@:@@")
    public void locationManagerDidFailWithError(Proxy manager, Proxy error)
    {
        logger.error("Hit an error in the location manager: " + error);
        bssidAvailability = BSSIDAvailability.UNKNOWN;
    }

    /**
     * Method to start our delegate.  This is invoked by a callback on the main
     * thread of the Objective-C library, which is required in order to get our
     * other callbacks invoked.
     */
    @Msg(selector="start", signature="v@:")
    public void start()
    {
        logger.debug("Creating location manager instance");
        Client c = Client.getInstance();

        // In order to access the CLLocationManager, we need that the
        // CoreLocation framework has been loaded in this process.
        // We can't explicitly load that from Java on Big Sur or above, where
        // that framework has no dynamically accessible library that we can
        // pass to System.load() or System.loadLibrary().
        // However, due presumably to transitive dependencies, if we load
        // the jnavfoundation native library at this point, then CoreLocation is
        // pulled in, and the sendProxy() call which follows is able to
        // succeed, rather than crashing with a native null pointer dereference.
        // However, loading that library then interferes with its proper use in
        // the AV stack (an easy check is that the Options->video preview does
        // not appear), so actually we duplicate that exact library under a
        // different name, and load the alternative named version here.
        System.loadLibrary("jnlocation");

        // Create an instance of the Location Manager, and set ourselves as the
        // delegate (so that we can receive callbacks)
        // We will receive an immediate callback telling us what the current
        // location permission state is.
        proxy = c.sendProxy("CLLocationManager", "new");
        proxy.set("delegate", this);
    }

    /**
     * Query the current BSSID availability.  This can change as permissions are
     * granted/revoked and as location services are enabled/disabled.
     *
     * @return The current BSSID availability status
     */
    public BSSIDAvailability getBSSIDAvailability()
    {
        return bssidAvailability;
    }

    public void requestAuthorization()
    {
        logger.debug("Request location permission");

        this.send("performSelectorOnMainThread:withObject:waitUntilDone:",
                  sel("onMainThread"), this, true);

        logger.debug("request location permission has returned");
    }

    @Msg(selector="onMainThread", signature="v@:")
    public void onMainThread()
    {
        logger.debug("Requesting permission on main thread");
        proxy.send("requestAlwaysAuthorization");
    }
}
