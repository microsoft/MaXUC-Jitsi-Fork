// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.analytics;

import org.json.simple.JSONObject;

/**
 * Abstract superclass to hide the separate implementations for simple
 * name:value (AnalyticsParameterSimple) and complex name:{structure}
 * (AnalyitcsParameterComplex) from AnalyticsEvent.
 *
 * @see net.java.sip.communicator.msw.impl.analytics.AnalyticsServiceImpl
 */
public abstract class AnalyticsParameter
{
    // Loads of constants for parameter names/value.  These are used for analytics
    // that are sent to SAS.
    // NOTE THESE REPRESENT A NORTHBOUND INTERFACE AND CANNOT BE CHANGED
    // WITHOUT AGREEMENT.
    public static final String NAME_NAME = "name";
    public static final String NAME_PRODUCT = "prd";
    public static final String NAME_OS = "os";
    public static final String NAME_OS_VERSION = "osv";
    public static final String NAME_BUILD_VERSION = "v";
    public static final String NAME_DEV_BUILD = "dev";
    public static final String NAME_CP_DOMAIN = "cpd";
    public static final String NAME_CP_CUST = "cpc";
    public static final String NAME_TIMEZONE = "tz";
    public static final String NAME_BG_NAME = "bg";

    public static final String NAME_CALLID = "cid";
    public static final String NAME_CALL_LENGTH = "len";
    public static final String NAME_DIRECTION = "dir";
    public static final String NAME_CODEC_STRUCT = "cdc";
    public static final String NAME_CODEC_NAME = "n";
    public static final String NAME_CODEC_FREQ = "khz";
    public static final String NAME_NETWORK_INFO = "nwi";
    public static final String NAME_NETWORK_TYPE = "nw";
    public static final String NAME_NETWORK_SSID = "ssid";
    public static final String NAME_NETWORK_RX = "nwr";
    public static final String NAME_NETWORK_TX = "nwt";
    public static final String NAME_RTCP_RR = "rr";
    public static final String NAME_RTCP_XR = "xr";
    public static final String NAME_RR_SSRC = "ssrc";
    public static final String NAME_RR_BYTES_SENT = "bs";
    public static final String NAME_RR_PACKETS_SENT = "ps";
    public static final String NAME_RR_PACKETS_LOST = "lst";
    public static final String NAME_RR_JITTER = "j";
    public static final String NAME_XR_LOSS_RATE = "lr";
    public static final String NAME_XR_DISCARD_RATE = "dr";
    public static final String NAME_XR_BURST_DENSITY = "bdn";
    public static final String NAME_XR_BURST_DURATION = "bdr";
    public static final String NAME_XR_GAP_DENSITY = "gdn";
    public static final String NAME_XR_GAP_DURATION = "gdr";
    public static final String NAME_XR_END_SYSTEM_DELAY = "esd";
    public static final String NAME_XR_ROUND_TRIP = "rtt";
    public static final String NAME_XR_MCQ = "mcq";
    public static final String NAME_JITTER_BUFFER = "jb";
    public static final String NAME_JITTER_NOMINAL = "nm";
    public static final String NAME_JITTER_MAX_SIZE = "mx";
    public static final String NAME_JITTER_ABS_MAX_SIZE = "am";
    public static final String NAME_DROPPED = "dropped";
    public static final String NAME_RTT_SEQ = "rtprtt";
    public static final String NAME_FAILED_ERR = "err";
    public static final String NAME_FAILED_SIP = "sip";

    public static final String VALUE_INBOUND = "in";
    public static final String VALUE_OUTBOUND = "out";
    public static final String VALUE_WIFI = "wifi";
    public static final String VALUE_WIRED = "wired";
    public static final String VALUE_DROPPED_YES = "1";
    public static final String VALUE_AD = "AccessionDesktop";
    public static final String VALUE_OS_WINDOWS = "Windows";
    public static final String VALUE_OS_MAC = "OSX";
    public static final String VALUE_FAILED_SIP = "SIP";
    public static final String VALUE_FAILED_CONFIG = "Bad config";

    public static final String NAME_GROUP_IM_ID = "Grp ID";
    public static final String NAME_COUNT_GROUP_IMS = "Grp Count";

    public static final String NAME_TIME_TO_FIRST_SENT_PACKET = "ttSend";
    public static final String NAME_TIME_TO_FIRST_REC_PACKET = "ttRec";

    // Parameters used on the Daily Running analytic.
    public static final String NAME_SUSBCRIBED_MASHUPS = "Subscribed Mashups";
    public static final String NAME_SRTP_ENABLED = "SRTP Enabled";
    public static final String NAME_SIP_TRANSPORT = "SIP Transport";

    /*
     * Constants for SAS Markers
     */
    public static final int SAS_MARKER_CALLING_DN = 0x01000006;
    public static final int SAS_MARKER_SUBSCRIBER_NUM = 0x01000008;
    public static final int SAS_MARKER_SIP_CALL_ID = 0x010C0001;

    // Lots of constants for non-SAS analytics parameters / values.
    public static final String NAME_ACC_MEET_CREATE_IMMEDIATELY = "Create Immediately";

    public static final String NAME_ACC_MEET_FROM = "From";
    public static final String VALUE_ACC_MEET_FROM_SCRATCH = "Scratch";
    public static final String VALUE_ACC_MEET_FROM_CALL = "Call";
    public static final String VALUE_ACC_MEET_FROM_CONTACT = "Contact";
    public static final String VALUE_ACC_MEET_FROM_GROUP_CONTACT = "Group Contact";
    public static final String VALUE_ACC_MEET_FROM_GRP_CHAT = "Group Chat";
    public static final String VALUE_ACC_MEET_FROM_CLIENT = "Accession Meeting Client";
    public static final String VALUE_ACC_MEET_FROM_EMAIL = "Email";
    public static final String VALUE_ACC_MEET_FROM_URL = "Copy URL";
    public static final String VALUE_ACC_MEET_FROM_PAIRING = "H.323/SIP pairing";
    public static final String VALUE_ACC_MEET_FROM_ROOM_CALLOUT = "H.323/SIP call-out";
    public static final String VALUE_ACC_MEET_FROM_DIAL_OUT = "Dial out";
    public static final String VALUE_ACC_MEET_FROM_ID = "Meeting ID";
    public static final String VALUE_ACC_MEET_FROM_INVITE = "Invite";

    public static final String NAME_ACC_MEET_CONFIG_SETTINGS_CHANGED = "Settings changed";

    public static final String NAME_CALL_JUMP_STATE = "State";
    public static final String MS_SINCE_LAST_HIT = "ms since last hit";

    // Constants for accepting of incoming calls/meetings
    public static final String NAME_NUM_CALLS_IN_PROGRESS = "Number calls in progress";
    public static final String NAME_DIALOG_ANSWERED_WITH = "Answered with";
    public static final String VALUE_MOUSE = "Mouse";
    public static final String VALUE_KEYBOARD = "Keyboard";
    public static final String VALUE_HEADSET = "Headset";

    // Constants for users manually changing their avatar
    public static final String NAME_CHANGE_AVATAR = "ChangeType";
    public static final String VALUE_CHANGE_AVATAR_UPDATE = "Update";
    public static final String VALUE_CHANGE_AVATAR_REMOVE = "Remove";

    public static final String NAME_TIME_ZONE_STRING = "time zone string";

    // Constants for URL services
    public static final String EVENT_COUNT = "Event count";
    public static final String SERVICE_CATEGORY_LAUNCHED = "Category";

    public static final String SERVICE_PROVIDER_IDENTIFIED = "Service provider identified";

    // WebSocket server parameters
    public static final String WEBSOCKET_CONNECTION_CLOSED_REASON = "Reason for closing WebSocket connection";

    // Calls
    public static final String NAME_CALL_RATING = "Rating";

    // Server backoff-related
    public static final String NAME_FINAL_BACKOFF_MS = "Final backoff ms";

    // Emergency location related
    // We match the Android strings, hence a few items are commented out when
    // desktop does not need that value
    public static final String PARAM_PERMISSION_ACTION_PERMISSION_NAME = "Name";
    public static final String VALUE_PERMISSION_LOCATION = "Location";
    public static final String PARAM_PERMISSION_ACTION_TYPE = "Type";
    public static final String VALUE_PERMISSION_ACTION_TYPE_REQUESTED = "Requested";
    public static final String VALUE_PERMISSION_ACTION_TYPE_GRANTED = "Granted";
    public static final String VALUE_PERMISSION_ACTION_TYPE_DENIED = "Denied";
    // public static final String VALUE_PERMISSION_ACTION_TYPE_PERMANENTLY_DENIED = "Permanently Denied";

    public static final String PARAM_DOWNLOAD_EMERGENCY_LOCATIONS_COUNT = "Emergency locations count";
    public static final String PARAM_DOWNLOAD_EMERGENCY_LOCATIONS_BSSID_COUNT =
            "Emergency locations with BSSID count";
    public static final String PARAM_DOWNLOAD_EMERGENCY_LOCATIONS_IP_ADDRESS_COUNT =
            "Emergency locations with IP_ADDRESS count";
    // public static final String PARAM_DOWNLOAD_EMERGENCY_LOCATIONS_TIME = "Download time";
    public static final String PARAM_COMMON_EMERGENCY_LOCATIONS_REASON = "Reason";
    public static final String PARAM_CHECK_EMERGENCY_LOCATIONS_MATCH_FOUND = "Location match";
    public static final String PARAM_LOCATION_SETTINGS_CHANGED = "Setting changed";
    public static final String PARAM_EMERGENCY_CALL_LOCATION_SENT = "Location sent";
    public static final String PARAM_EMERGENCY_CALL_FIRST_ATTEMPT_RESULT = "First attempt result";
    public static final String VALUE_CHECK_EMERGENCY_LOCATIONS_REASON_PERMISSION_GRANTED =
        "Location permission granted";
    public static final String VALUE_CHECK_EMERGENCY_LOCATIONS_REASON_DOWNLOADED_LOCATIONS =
        "Downloaded emergency locations";
    public static final String VALUE_CHECK_EMERGENCY_LOCATIONS_REASON_WIFI_CONNECTED = "Wi-Fi connected";
    public static final String VALUE_CHECK_EMERGENCY_LOCATIONS_REASON_BSSID_CHANGE = "BSSID change detected";
    public static final String VALUE_LOCATION_SETTINGS_CHANGED_LOCATION_PERMISSION = "Location permission";
    public static final String VALUE_LOCATION_SETTINGS_CHANGED_NONE = "None";
    public static final String VALUE_LOCATION_SETTINGS_CHANGED_ALLOWED_ONCE = "Allowed once";
    public static final String VALUE_LOCATION_SETTINGS_CHANGED_ALLOWED_FOREGROUND = "Allowed in foreground";
    public static final String VALUE_CLEAR_EMERGENCY_LOCATION_NO_BSSID = "No BSSID match";
    public static final String VALUE_CLEAR_EMERGENCY_LOCATION_DISCONNECTED_FROM_WIFI = "Disconnected from Wi-Fi";
    public static final String VALUE_CLEAR_EMERGENCY_LOCATION_PERMISSION_DENIED = "Permission denied";
    public static final String VALUE_CLEAR_EMERGENCY_LOCATION_FEATURE_DISABLED = "Feature disabled";

    public static final String PARAM_USING_HTTPS = "Using HTTPS";
    public static final String PARAM_USING_LDAPS = "Using LDAPS";

    public static final String PARAM_DIVERSION_NAME = "Diversion Name";
    public static final String VALUE_SIGNALLED = "SIGNALLED";
    public static final String VALUE_CONTACT_MATCH = "CONTACT_MATCH";
    public static final String VALUE_NO_MATCH = "NO_MATCH";

    /**
     * The maximum length of a single analytics event (as supported by our
     * server).  Note we're not completely accurate in our length checking in
     * this class as it's not important to be completely correct for 2 reasons:
     * - this max length is an underestimate of what the server can support
     * - the server can always reject events if we let something slip through
     *
     * Note the /2 is to convert from length of string to number of bytes
     * (assuming a standard encoding).
     */
    public static final int MAX_EVENT_LENGTH = 9000 / 2;

    protected final String mName;

    /**
     * @param name Name of this parameter.
     */
    public AnalyticsParameter(String name)
    {
        mName = name;
    }

    /**
     * Add this parameter to the JSONObject, if it will not exceed the maximum length,
     * and returns the additional length added.
     *
     * @param jsonObject JSON object to add this parameter to.
     * @return the additional length added.
     */
    public abstract int addToJSON(JSONObject jsonObject, int currentLength);

    /**
     * Get the name of this parameter
     * @return the parameter name
     */
    public String getParameterName()
    {
        return mName;
    }

    /**
     * All parameter names should be lower case and without spaces - this
     * method enforces this requirement
     *
     * @param name The name to be formatted.
     */
    String formatName(String name)
    {
        return name.toLowerCase().replaceAll(" ", "_");
    }

    public abstract boolean equals(Object parameter);
}
