// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.diagnostics;

import net.java.sip.communicator.util.UtilActivator;
import org.jitsi.service.resources.ResourceManagementService;

/**
 * The reason why an error report is being created.
 */
public enum ReportReason
{
    /**
     * Used for testing purposes.
     */
    TEST_MESSAGE(false, "TEST MESSAGE"),

    /**
     * Used to auto-pop-up an error report window when the user hits the known error due to Outlook crashing.
     */
    OUTLOOK_CRASHED(false, "OUTLOOK CRASHED"),

    /**
     * Used to auto-pop-up an error report window when the user hits the known database migration error.
     */
    DATABASE_MIGRATION_ERROR(false, "DATABASE MIGRATION ERROR"),

    /**
     *  Used to auto-pop-up an error report window in QA mode only, when PPS get fails and stored config is used.
     */
    UNACKNOWLEDGED_USE_OF_STORED_CONFIG(false, "INFO: CLIENT USING STORED CONFIG ON STARTUP\n" +
            "This indicates that the client currently cannot download subscriber configuration (from SIP-PS). This may be due to your network connection - " +
            "make sure you are connected to the Internet and restart the client. If this error persists, you might need to delete the application data folder for " +
            "the app to ensure new testing does not use cached subscriber data.\n" +
            "\n" +
            "For more information see SFR537020."),

    /**
     *  Used to auto-pop-up an error report window when the user hits an audio config error on Mac.
     */
    MAC_AUDIO_CONFIG_ERROR(false, "MAC AUDIO CONFIG ERROR (SEE SFR 535413)"),

    /**
     * Used to auto-pop-up an error report window on next start-up after a hang on shutdown.
     */
    HANG_ON_SHUTDOWN(false, "HANG ON SHUTDOWN"),

    /**
     * Used to auto-pop-up an error report window on next start-up after a previous OutOfMemory crash.
     */
    OOM_CRASH_LAST_TIME(true, "CRASH LAST TIME"),

    /**
     * Used to auto-pop-up an error report window when the user hits any uncaught exception.
     */
    UNCAUGHT_EXCEPTION(true, "UNCAUGHT EXCEPTION"),

    /**
     * Used to open the manual user error report window.
     */
    USER(false, "MANUAL USER REPORT"),

    /**
     * Used for user-sent call quality feedback.
     */
    CALL_END(false,"CALL END"),

    /**
     * Used to auto-pop-up an error report window when a user hits any other known problem.
     */
    KNOWN_PROBLEM(false, "KNOWN PROBLEM");

    private final boolean onlyInterestedInStackTraces;

    private final String errorName;

    private String preambleText;

    private static final ResourceManagementService mResourceService = UtilActivator.getResources();

    private static final String KNOWN_ISSUE_HEADER =
            mResourceService.getI18NString("plugin.errorreport.KNOWN_ISSUE_HEADER");
    private static final String UNCAUGHT_ISSUE_HEADER =
            mResourceService.getI18NString("plugin.errorreport.UNCAUGHT_EXCEPTION_HEADER");
    private static final String SEND_DIAGS_PROMPT =
            mResourceService.getI18NString("plugin.errorreport.SEND_DIAGS_PROMPT");
    private static final String FEEDBACK_PROMPT =
            mResourceService.getI18NString("plugin.errorreport.POP_UP_HINT");

    static
    {
        TEST_MESSAGE.preambleText = "TEST";
        OUTLOOK_CRASHED.preambleText = mResourceService.getI18NString(
                "plugin.errorreport.OUTLOOK_CRASHED");
        DATABASE_MIGRATION_ERROR.preambleText = mResourceService.getI18NString(
                "plugin.errorreport.DATABASE_MIGRATION");
        MAC_AUDIO_CONFIG_ERROR.preambleText = mResourceService.getI18NString(
                "plugin.errorreport.MAC_AUDIO_CONFIG");
        HANG_ON_SHUTDOWN.preambleText = mResourceService.getI18NString(
                "plugin.errorreport.HANG_ON_SHUTDOWN");
        OOM_CRASH_LAST_TIME.preambleText = mResourceService.getI18NString(
                "plugin.errorreport.CRASH_LAST_TIME");
        UNCAUGHT_EXCEPTION.preambleText = mResourceService.getI18NString(
                "plugin.errorreport.UNCAUGHT_EXCEPTION");
        USER.preambleText = mResourceService.getI18NString(
                "plugin.errorreport.POP_UP_PREAMBLE_MANUAL_REPORT");
        // ErrorReportFrames opened for CALL_END are ErrorReportCallQualityFrames
        // and their text content is handled differently. Ensure UTs test this
        // and fail before this string is shown to the user.
        CALL_END.preambleText = "You should not see this text. A call quality" +
                                "frame should have opened instead.";
        KNOWN_PROBLEM.preambleText = "";
    }

    ReportReason(boolean onlyInterestedInStackTraces,
                 String name)
    {
        this.onlyInterestedInStackTraces = onlyInterestedInStackTraces;
        this.errorName = name;
    }

    /**
     * @return Whether this report is such that the main diags information
     * we need is a stacktrace from an uncaught exception or previous crash
     */
    public boolean onlyInterestedInStackTraces()
    {
        return onlyInterestedInStackTraces;
    }

    /**
     * @return The name of this error type, used for QA and storage
     */
    public String getName()
    {
        return errorName;
    }

    /**
     * Constructs the preamble shown in the error frame depending on error.
     *
     * @return The error frame text for this error type
     */
    public String getPreambleText()
    {
        String hintPreamble = "";
        switch (this)
        {
            case USER: // No pre-amble for user given feedback.
                break;
            case HANG_ON_SHUTDOWN:
            case OOM_CRASH_LAST_TIME:
            case UNCAUGHT_EXCEPTION:
                hintPreamble += UNCAUGHT_ISSUE_HEADER;
                break;
            default:
                hintPreamble += KNOWN_ISSUE_HEADER;
                break;
        }
        hintPreamble += preambleText;
        hintPreamble += (this == USER) ? FEEDBACK_PROMPT : SEND_DIAGS_PROMPT;
        return hintPreamble;
    }
}
