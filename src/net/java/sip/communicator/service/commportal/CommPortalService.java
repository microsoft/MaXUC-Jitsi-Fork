// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

import java.beans.PropertyChangeListener;
import java.net.InetAddress;
import java.security.InvalidParameterException;
import java.util.regex.*;
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
     */
    void getToken(CPTokenGetterCallback callback,
                  CPOnNetworkErrorCallback networkErrorCallback,
                  boolean isForeground);

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
     * Register for notifications on changes to the CommPortal session ID, when
     * it is needed to navigate to a CPWeb URL in a browser. The session ID will
     * requested and returned on the callback.
     *
     * @param callback The callback on which session ID is returned
     */
    void getSessionIdForBrowser(CPSessionCallback callback);

    /**
     * Un-register the callback for changes to the CommPortal session ID
     *
     * @param callback The registered callback
     */
    void unRegisterSessionCallback(CPSessionCallback callback);

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
     * Returns a session ID substitute which is able to be logged. We don't
     * log the whole session ID due to security concerns, but it is useful for
     * L3 to be able to correlate with server-side logs, so we print an
     * obfuscated version which strips out the central characters.
     *
     * @param sessionId The session ID to modify
     * @return  There are three cases:
     *          1) the ID has >= 20 characters. Return a string of the first 8
     *          and last 4 characters from the session ID, with "__removed__" in
     *          between. If it is not at least 8 characters long, it returns null.
     *          2) the ID has <20 but >=16 characters. Just return the first 8
     *          characters followed by "__removed__" as any more characters would
     *          compromise security.
     *          3) the ID has <=15 characters. We never expect this to happen.
     *          Return "__Redacted as less than 16 characters.__";
     */
    static String obfuscateSessionId(String sessionId)
    {
        String obfuscatedSessionId;
        if (sessionId == null)
        {
            obfuscatedSessionId = null;
        }
        else if (sessionId.length() >= 20)
        {
            obfuscatedSessionId = sessionId.substring(0,8) + "__removed__" +
                                  sessionId.substring(sessionId.length() - 4);
        }
        else if (sessionId.length() >= 16)
        {
            obfuscatedSessionId = sessionId.substring(0,8) + "__removed__";
        }
        else
        {
            // This should never happen
            obfuscatedSessionId = "__Redacted as less than 16 characters.__";
        }
        return obfuscatedSessionId;
    }

    /**
     * Returns a URL which is able to be logged. We use this when a URL contains
     * a session ID. We can't print the whole URl for security reasons, so we
     * remove a central part of the session ID. This still gives L3 the
     * information they need to be able to reconstruct the URL from server logs.
     *
     * @param url The URL to remove the session ID from
     * @return The same URL, but with the session ID modified as in
     *         obfuscateSessionId(). If no session ID is found, it returns the
     *         url input.
     */
    static String getLoggableURL(String url)
    {
        // The pattern to match could be "/session" + {at least 8 hex
        // characters} + "/" or it could be "session=" + {at least 8 hex
        // characters} + "&". We need to check both. 8 is an arbitrary number
        // which is low enough that it shouldn't miss any session IDs, but
        // high enough that it won't give false positives.
        String[] possibleMatches = {"/session(\\p{XDigit}{8,})/",
                "session=(\\p{XDigit}{8,})&"};
        String stringToReturn = url;

        for (String sessionIdRegex: possibleMatches)
        {
            Pattern pattern = Pattern.compile(sessionIdRegex);
            Matcher matcher = pattern.matcher(url);

            if (matcher.find())
            {
                String sessionId = matcher.group(1);
                stringToReturn = stringToReturn.replaceAll(sessionId,
                                                           obfuscateSessionId(sessionId));
                break;
            }
        }
        return stringToReturn;
    }

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
     *              location information for the current network.
     */
    boolean isMissingEmergencyLocationInformation();

    /**
     * Registers a listener, which will be notified if the return value of
     * isMissingEmergencyLocationInformation() changes.
     */
    void addMissingLocationUpdateListener(PropertyChangeListener listener);

    /**
     * Removes a previously registered listener, so it will no longer be notified
     * if the return value of isMissingEmergencyLocationInformation() changes.
     * If listener was not previously registered or is null, does nothing.
     */
    void removeMissingLocationUpdateListener(PropertyChangeListener listener);

    /**
     * Method run to notify listeners when the return value of
     * isMissingEmergencyLocationInformation() changes.
     *
     * @param oldIsMissingLocation
     * @param newIsMissingLocation
     */
    void onMissingLocationUpdated(boolean oldIsMissingLocation, boolean newIsMissingLocation);
}
