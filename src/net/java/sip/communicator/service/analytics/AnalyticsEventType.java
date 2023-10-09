// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.analytics;

import com.metaswitch.maxanalytics.event.ImKt;
import com.metaswitch.maxanalytics.event.RateAppKt;

/**
 * The complete list of events that can be sent to the analytics server and
 * maybe also SAS.
 *
 * Most events are just for 'Accession analytics' (for Metaswitch consumption only),
 * but some are also SAS events (those given a CommPortal ID and name in the
 * constructor). These events are sent to SAS via CP and if configured, SAS will
 * forward to the SP's analytics server (likely Splunk) as required as 'SAS analytics'.
 *
 * @see net.java.sip.communicator.msw.impl.analytics.AnalyticsServiceImpl
 */
public enum AnalyticsEventType
{
    // SAS subset - these events are sent to SAS (via CP) and (if the SP has
    // SAS analytics enabled) SAS will send them onto the SP's analytics
    // engine.
    // They are ALSO sent directly to the Msw splunk instance (for
    // Accession analytics).
    SAS_MARKER_ONLY("Marker only", -1, "Marker only", true, false), // Only send markers, not the event
    CALL_ENDED("Call ended", 1, "Call ended", false, false),
    SYS_RUNNING("SYS running", 3, "Running", false, false),
    CALL_FAILED("Call failed", 4, "Call failed", false, false),

    // These events are sent directly to the Msw splunk instance (for
    // Accession analytics), they are NOT sent to SAS.

    // Life-cycle
    APP_STARTED("Application started"),
    CDAP_PROVISIONING_URI_MATCH("App launched using CDAP login link"),
    GOT_COS("Class of service retrieved"),
    CONFIG_RETRIEVED("Config retrieved"),
    GETTING_CONFIG_FAILED("Get Cfg Failed"),
    AUTO_UPDATE_OFF("Auto-update disabled for user"),
    AUTO_UPDATE_ON("Auto-update enabled for user"),
    SHUTDOWN("Application being shut down"),
    ELECTRON_DOWN("Electron can't be reached"),
    CP_TOKEN_RETRIEVED("CommPortal token retrieved"),
    SECURE_CONNECTION_ESTABLISHED("Secure connection established", true),

    // Errors
    UNCAUGHT_EXCEPTION("Uncaught Exception"),
    OOM_LAST_TIME("Out of memory last time"),
    CLIENT_HUNG("Client hung on last shutdown"),
    DEADLOCK("Deadlock Detected"),
    EDT_HANG("EDT hang detected"),
    EDT_RECOVERED("EDT hang recovered"),
    SCHEDULED_EXECUTOR_BAD_STATE("Scheduled Executor in Bad State"),
    UNCAUGHT_ELECTRON_EXCEPTION("Uncaught Electron exception"),
    CREATE_ERROR_EMAIL_FOLLOW_UP("Create an email draft as follow-up to an error report"),

    // Calls
    DIALPAD_CLOSED("User closed dialpad dialog"),
    DIALPAD_OPENED("User opened dialpad dialog"),
    OUTLOOK_CALL("Outlook Call"),
    OUTBOUND_CALL("User making a call"),
    INCOMING_ANSWERED("User answered incoming call"),
    INCOMING_ANSWERED_SHORTCUT("User answered incoming call using global keyboard shortcut"),
    INCOMING_REJECT("User rejected incoming call"),
    INCOMING_REJECT_SHORTCUT("User rejected incoming call using global keyboard shortcut"),
    HANGUP_CALL_SHORTCUT("User ended ongoing call using global keyboard shortcut"),
    FOCUS_CALL_SHORTCUT("User focused call using global keyboard shortcut"),
    MUTE_CALL_SHORTCUT("User used the mute call global keyboard shortcut"),
    PTT_CALL_SHORTCUT("User used the push to talk global shortcut"),
    INCOMING_REJECT_IM("User rejected incoming call with IM"),
    CUT_THRU("Media cut through"),
    MEDIA_PRECREATE("Media precreate"),
    ASKED_TO_MERGE("User asked to merge calls"),
    START_TRANSFER("User is trying to transfer call"),
    TRANSFERRED_CALL("User successfully transferred call"),
    CALL_PUSHED("Jump pushed call"),
    CALL_PULLED("Jump pulled call"),
    CALL_JUMP_COMPLETE("Jump complete"),
    CALL_RATING("User rating call"),
    CLICK_TO_DIAL("Click to dial", true),
    ENABLE_VIDEO("Enable local video for a call", true),
    CALL_PARKED("Call parked", true),
    CALL_PARK_PICKED_UP("Call park picked up", true),
    CALL_WITH_DIRECT_CALLING_DISABLED("Call made with direct calling disabled", true),
    INCOMING_DIVERTED_CALL("Incoming Diverted Call"),

    IM_CONNECTED("IM connected"),
    IM_SIGNED_IN("User sign in IM"),
    OUTLOOK_IM("Outlook IM"),
    USER_CHAT_FROM_SEARCHBAR("User initiating SMS from searchbar"),
    NEW_GROUP_CHAT("New group IM"),
    REOPEN_GROUP_IM("Reopen group IM"),
    ACCEPT_GROUP_IM("Accept group IM"),
    LEAVE_GROUP_IM("Leave group IM"),
    IM_REMOVE_PARTICIPANT("IM remove participant"),
    SEND_IM("Send an IM", true),
    RECEIVE_IM("Receive an IM", true),
    SEND_GROUP_IM("Send a group IM", true),
    RECEIVE_GROUP_IM("Receive a group IM", true),
    XMPP_AUTHENTICATION_BACKOFF_SUCCESS("Event - XMPP authentication backoff success", true),
    XMPP_AUTHENTICATION_BACKOFF_HIT_MAX_FAILURES("Event - XMPP authentication backoff hit max failures", true),
    CHAT_LIST_REQUEST_BEFORE_IM_PROVIDER_READY("Failed to provide chat list because IM provider not ready", true),
    CHAT_AUTHORISATION_REQUEST_RECEIVED("Received chat authorisation request from an external contact"),

    // File Transfer
    SEND_FILE_STARTED("Started outgoing file transfer"),
    SEND_FILE_COMPLETED("Outgoing file transfer completed"),
    SEND_FILE_FAILED("Outgoing file transfer failed"),
    RECEIVE_FILE_STARTED("Started incoming file transfer"),
    RECEIVE_FILE_COMPLETED("Incoming file transfer completed"),
    RECEIVE_FILE_FAILED("Incoming file transfer failed"),

    // Contacts
    VIEW_CONTACT_DETAILS("User asked to view contact", true),
    EDIT_CONTACT("User asked to edit contact"),
    ADD_CONTACT("User asked to add contact"),
    FAVORITES_ADD("User adding contact to favorites"),
    FAVORITES_REMOVE("User removing contact from favorites"),
    USER_CHANGE_AVATAR("User changed their avatar"),
    LDAP_CONTACT_FOUND("LDAP search found a contact", true),
    LDAP_ENABLED("LDAP enabled"),

    // Other user actions
    VOICEMAIL_OPEN("User opening voicemail window"),
    HELP_OPENED("Help opened"),
    OPENING_SETTINGS("User opening settings menu"),
    BCM_DATA_CHANGE("User changed BCM data"),
    ECM_DATA_CHANGE("User changed ECM data"),
    // ACCESSIBILITY_ON("User turned on accessibility mode"), // Unused
    // ACCESSIBILITY_OFF("User turned off accessibility mode"), // Unused
    ERROR_WINDOW_OPEN("User opened error report window"),
    SEND_ERROR_REPORT("User sending error report"),
    SET_CUSTOM_STATUS("Set a custom status", true),
    THIRD_PARTY_INTEGRATION_LAUNCHED("Third party service launched", true),
    VERSION_UPDATE_DIALOG_USER_NOTIFIED("Upgrade - User notified of new version available"),
    VERSION_UPDATE_USER_CHECK_FOR_UPDATES("Upgrade - User requested a check for updates"),
    VERSION_UPDATE_DIALOG_USER_ACCEPT("Upgrade - User accepted update"),
    VERSION_UPDATE_DIALOG_USER_DISMISS("Upgrade - User dismissed update"),
    VERSION_UPDATE_USER_CANCEL_DOWNLOAD("Upgrade - User cancelled update download"),
    VERSION_UPDATE_VERIFY_SUCCESS("Upgrade - Successful"),
    VERSION_UPDATE_ERROR("Upgrade - Error"),

    // MaX Meeting
    ACC_MEET_USER_CREATE("User Created new Meeting"),
    ACC_MEET_INVITE("Inviting participants to Meeting"),
    ACC_MEET_ADDING("Adding Participant to Meeting"),
    ACC_MEET_NOT_JOINED("Not joining Meeting"),
    ACC_MEET_JOINED("User Joined Meeting"),
    ACC_MEET_JOIN_FAILED("User Failed to Join Meeting"),
    ACC_MEET_ENDED("Meeting ended"),
    ACC_MEET_SCHEDULED("Open scheduled Meetings"),
    ACC_MEET_RECORDED("Open recorded Meetings"),
    ACC_MEET_SCHEDULER("Open Meeting scheduler"),
    ACC_MEET_INSTALLED("Installed Meeting client"),
    ACC_MEET_START("Meeting Started"),
    ACC_MEET_SET_CONFIG("Changing Meeting settings"),
    ACC_MEET_ADD_ASSISTANT("Assign scheduling privileges"),
    ACC_MEET_REMOVE_ASSISTANT("Revoke scheduling privileges"),

    // N-Series Conference Widget
    NSERIES_CONF_START("User launched N-Series Conference via widget"),

    // Misc
    UNRECOGNIZED_OUTLOOK_TIME_ZONE("Unrecognized Outlook time zone"),

    // Emergency location related
    // Commented out items are not used by Desktop, but are by Mobile,
    // so are included here to document the differences between the clients.
    PERMISSION_ACTION("Permission Action"),
    DOWNLOAD_EMERGENCY_LOCATIONS("Event - Download emergency locations"),
    EMERGENCY_CALL_HANDLED_BY_APP("Feature - Emergency call handled by app"),
    // CLEAR_EMERGENCY_LOCATION("Event - Clear emergency location"),
    EMERGENCY_CALL_RETRY("Event - Emergency call retry"),
    TIMEOUT_GETTING_EMERGENCY_LOCATION("Event - Timeout getting location for emergency call"),
    // CHECK_EMERGENCY_LOCATIONS("Event - Check emergency locations"),
    // LOCATION_SETTINGS_CHANGED("Setting changed"),
    // EMERGENCY_CALL_HANDLED_BY_OS("Feature - Emergency call handled by OS"),

    EVENT_RATE_APP(RateAppKt.EVENT_RATE_APP),
    EVENT_IM_WINDOW_LAUNCH(ImKt.EVENT_IM_WINDOW_LAUNCH),
    ;

   /**
     * Current enum value.
     */
    private final String mEventName;

    /*
     * The ID of this event on CommPortal (or 0 if it is not
     * supported there).
     */
    public final int mCommPortalID;

    /*
     * The ID of this event on CommPortal (or null if it is not
     * supported there).
     */
    public final String mCommPortalName;

    /*
     * Whether a trail for this event should be kept (which
     * is a SAS concept).  Needs to be true if we are going to
     * use the trail later (eg on the end of a call).  Should
     * be false otherwise.
     */
    public final boolean mKeepTrail;

    /**
     * Create a diff between aggregated and not aggregated Event Types
     * Aggregated events are events that should not be immediately sent for the
     * analytics, to preserve bandwidth, so they increment a variable which will
     * be sent in the end of some amount of time to the server with the amount
     * of times that action was called.
     */
    public final boolean mIsAggregated;

    /**
     * Creates enum with the specified value.
     *
     * @param value the value to set.
     */
    AnalyticsEventType(String value)
    {
        this(value, 0, null, false, false);
    }

    /** Creates enum with specified value and aggregated value flag */
    AnalyticsEventType(String value, boolean isAggregated)
    {
        this(value, 0, null, false, isAggregated);
    }

    /**
     * @param eventName The string representation of the event
     * @param commPortalID ID of this event in CommPortal, or
     *        0 if it is not an event that get sent to CommPortal
     */
    AnalyticsEventType(String eventName,
                       int commPortalID,
                       String commPortalName,
                       boolean keepTrail,
                       boolean isAggregated)
    {
        this.mEventName = eventName;
        this.mCommPortalID = commPortalID;
        this.mCommPortalName = commPortalName;
        this.mKeepTrail = keepTrail;
        this.mIsAggregated = isAggregated;
    }

    /**
     * Gets the String representation.
     * Note, this value is used when sending an event to analytics.
     *
     * @return the value
     */
    public String toString()
    {
        return mEventName;
    }

}
