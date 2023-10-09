// Copyright (c) Microsoft Corporation. All rights reserved.
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

import java.beans.PropertyChangeListener;
import java.net.InetAddress;
import java.security.InvalidParameterException;

import net.java.sip.communicator.service.protocol.emergencylocation.*;

/**
 * A service which allows interaction with CommPortal.  Including
 * <li>requesting service indications from CommPortal</li>
 * <li>sending data to CommPortal</li>
 * <li>registering for notifications to changes to a service indication</li>
 * <li>unregistering for notifications</li>
 * <li>uploading files to CommPortal</li>
 */
public interface CommPortalService
{
    /**
     * Request an action from CommPortal
     *
     * @param callback the callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     * @param isForeground If true then this will happen with high priority
     */
    void getAction(CPDataSenderCallback callback,
                   CPOnNetworkErrorCallback networkErrorCallback,
                   boolean isForeground);

    /**
     * Request a token from CommPortal
     *
     * @param callback the callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     * @param isForeground If true then this will happen with high priority
     * @param allowFurtherTokenGeneration If true then the token generated can
     *        be used to generate further tokens
     */
    void getToken(CPTokenGetterCallback callback,
                  CPOnNetworkErrorCallback networkErrorCallback,
                  boolean isForeground,
                  boolean allowFurtherTokenGeneration);

    /**
     * Request a service indication(s) from CommPortal with a 5 second timeout
     *
     * @param callback the callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     * @param isForeground If true then this will happen with high priority
     */
    void getServiceIndication(CPDataGetterCallback callback,
                              CPOnNetworkErrorCallback networkErrorCallback,
                              boolean isForeground);

    /**
     * Request a service indication(s) from CommPortal with a 10 second timeout
     *
     * @param callback the callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     */
    void getServiceIndicationLongTimeout(CPDataGetterCallback callback,
                                         CPOnNetworkErrorCallback networkErrorCallback);

    /**
     * Post a service indication to CommPortal
     *
     * @param callback the callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     * @param isForeground If true then this will happen with high priority
     */
    void postServiceIndication(CPDataSenderCallback callback,
                               CPOnNetworkErrorCallback networkErrorCallback,
                               boolean isForeground);

    /**
     * Post data to CommPortal (with no SI)
     *
     * @param callback the callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     * @param isForeground If true then this will happen with high priority
     */
    void postData(CPDataSenderCallback callback,
                  CPOnNetworkErrorCallback networkErrorCallback,
                  boolean isForeground);

    /**
     * Upload a file to CommPortal
     *
     * @param callback the callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     */
    void uploadFile(CPFileUploadCallback callback,
                    CPOnNetworkErrorCallback networkErrorCallback);

    /**
     * Ask CommPortal to perform an action
     *
     * @param callback the callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     */
    void performAction(CPActionCallback callback,
                       CPOnNetworkErrorCallback networkErrorCallback);

    /**
     * Register for notifications to changes to a service indication(s)
     * <p/>
     * This registration will continue regardless of any network errors until
     * unregister is called.  However, any data errors will unregister all
     * listeners.
     * <p/>
     * Does nothing if the callback is already registered
     *
     * @param callback The callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     */
    void registerForNotifications(CPDataRegistrationWithoutDataCallback callback,
                                  CPOnNetworkErrorCallback networkErrorCallback);

    /**
     * Register for notifications to changes to a service indication(s) when the
     * notification of change includes the new data.
     * <p/>
     * This registration will continue regardless of any network errors until
     * unregister is called.  However, any data errors will unregister all
     * listeners.
     * <p/>
     * Does nothing if the callback is already registered
     *
     * @param callback The callback for the request
     * @param networkErrorCallback optional parameter which will handle network
     *        errors if present.  Otherwise, these are handled by the service
     */
    void registerForNotificationsWithData(CPDataRegistrationWithDataCallback callback,
                                          CPOnNetworkErrorCallback networkErrorCallback);

    /**
     * Unregister for notifications to changes to a particular service indication
     *
     * @param callback The callback which we originally registered on.  Must be
     *        a registered callback otherwise an exception will be thrown
     * @throws InvalidParameterException if we try to unregister a callback that
     *         isn't registered
     */
    void unregisterForNotifications(CPDataRegistrationCallback callback);

    /**
     * Get the CommPortal customization URL.
     *
     * @return The current URL for the CommPortal customization
     */
    String getUrl();

    /**
     * Register for notifications on changes to the CommPortal short-lived token, when
     * it is needed to navigate to a CPWeb URL in a browser. The token will be
     * requested and returned on the callback.
     *
     * @param callback The callback on which short-lived token is returned
     */
    void getShortLivedTokenForBrowser(CPShortLivedTokenForBrowserCallback callback);

    /**
     * Un-register the callback for changes to the CommPortal short-lived token.
     *
     * @param callback The registered callback
     */
    void unRegisterShortLivedTokenCallback(CPShortLivedTokenForBrowserCallback callback);

    /**
     * Request a specific service indication to retrieve the number of unread
     * fax messages in the mailbox, and return this information to the
     * MessageWaitingOpSet. Required because SIP MWI does not deliver details of
     * fax messages.
     */
    void getFaxMessageCounts();

    /**
     * Returns the latest version of the CommPortal interface that this
     * CommPortal service supports.
     *
     * Can be null if we haven't logged in.
     *
     * @return the latest version
     */
    CommPortalVersion getLatestVersion();

    /**
     * @return true if the user's BG has location data for emergency calling enabled.
     */
    boolean isEmergencyLocationSupportNeeded();

    /**
     * @return the location information for use in emergency calling for the
     *         network address that we are currently using, or null if we don't
     *         have that information.
     */
    LocationAddress determineLocationAddress();

    /**
     * @return the location information for use in emergency calling for the
     *         given address, or null if we don't have that information.
     */
    LocationAddress findLocationForAddress(InetAddress address);

    /**
     * @param contentId
     * @return an EmergencyCallContext object for the given content ID.
     */
    EmergencyCallContext determineEmergencyCallContext(String contentId);

    /**
     * @param stringContact
     * @return true if calls to the given contact are considered emergency
     *              calls and require location information.
     */
    boolean doesCallRequireLocationInformation(String stringContact);

    /**
     * @return true if the user's BG has location data for emergency calling
     *              enabled but any data we have received does not contain
     *              location information for the current network and
     *              the user is not a standalone meeting user.
     */
    boolean isMissingEmergencyLocationInformation();

    /**
     * Registers a listener, which will be notified if the return value of
     * isMissingEmergencyLocationInformation() changes.
     */
    void addMissingLocationUpdateListener(PropertyChangeListener listener);

    /**
     * Method run to notify listeners when the return value of
     * isMissingEmergencyLocationInformation() changes.
     *
     * @param oldIsMissingLocation
     * @param newIsMissingLocation
     */
    void onMissingLocationUpdated(boolean oldIsMissingLocation, boolean newIsMissingLocation);
}
