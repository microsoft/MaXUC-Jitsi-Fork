/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.notificationwiring;

import java.util.*;

import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * Manages the access to the properties file containing all sounds paths.
 *
 * @author Yana Stamcheva
 */
public final class SoundProperties
{
    /**
     * The incoming message sound id.
     */
    public static final String INCOMING_MESSAGE;

    /**
     * The incoming conference sound id.
     */
    public static final String INCOMING_CONFERENCE;

    /**
     * The incoming file sound id.
     */
    public static final String INCOMING_FILE;

    /**
     * The outgoing call sound id.  Not final as we update it on getting data
     * back from EAS (the international country code).
     */
    private static String outgoingCall;

    /**
     * The incoming call sound id.
     */
    public static final String INCOMING_CALL;

    /**
     * The call waiting sound id.
     */
    public static final String CALL_WAITING;

    /**
     * The busy sound id.
     */
    public static final String BUSY;

    /**
     * The dialing sound id.
     */
    public static final String DIALING;

    /**
     * The sound id of the sound played when call security is turned on.
     */
    public static final String CALL_SECURITY_ON;

    /**
     * The sound id of the sound played when a call security error occurs.
     */
    public static final String CALL_SECURITY_ERROR;

    /**
     * The hang up sound id.
     */
    public static final String HANG_UP;

    static
    {
        /*
         * Call NotificationActivator.getResources() once because (1) it's not a trivial
         * getter, it caches the reference so it always checks whether the cache
         * has already been built and (2) accessing a local variable is supposed
         * to be faster than calling a method (even if the method is a trivial
         * getter and it's inlined at runtime, it's still supposed to be slower
         * because it will be accessing a field, not a local variable).
         */
        ResourceManagementService resources =
            NotificationWiringActivator.getResources();

        // Get a string representing the current locale to try to load a
        // localised ringback tone.  If no locale-specific ringback tone is
        // configured, fall back to the default.
        String locale =
            ConfigurationUtils.getCurrentLanguage().toString().toUpperCase();
        String callResource = resources.getSoundPath("OUTGOING_CALL_" + locale);
        outgoingCall = StringUtils.isNullOrEmpty(callResource) ?
            resources.getSoundPath("OUTGOING_CALL") : callResource;

        INCOMING_MESSAGE = resources.getSoundPath("INCOMING_MESSAGE");
        INCOMING_CONFERENCE = resources.getSoundPath("INCOMING_CONFERENCE");
        INCOMING_FILE = resources.getSoundPath("INCOMING_FILE");
        INCOMING_CALL = resources.getSoundPath("INCOMING_CALL");
        CALL_WAITING = resources.getSoundPath("CALL_WAITING");
        BUSY = resources.getSoundPath("BUSY");
        DIALING = resources.getSoundPath("DIAL");
        CALL_SECURITY_ON = resources.getSoundPath("CALL_SECURITY_ON");
        CALL_SECURITY_ERROR = resources.getSoundPath("CALL_SECURITY_ERROR");
        HANG_UP = resources.getSoundPath("HANG_UP");
    }

    private static final Logger logger = Logger.getLogger(SoundProperties.class);

    /**
     * Map from a region's country code to the ringback sound for that region.
     */
    private static final Map<Integer, String> countryCodeToRingSound =
            new HashMap<>();
    static
    {
        countryCodeToRingSound.put(1,"OUTGOING_CALL_EN_US"); // NANP (US, Canada etc)
        countryCodeToRingSound.put(33,"OUTGOING_CALL_FR_FR"); // France
        countryCodeToRingSound.put(34,"OUTGOING_CALL_ES_ES"); // Spain
        countryCodeToRingSound.put(39,"OUTGOING_CALL_IT_IT"); // Italy
        countryCodeToRingSound.put(44,"OUTGOING_CALL_EN_GB"); // UK
        countryCodeToRingSound.put(51,"OUTGOING_CALL_ES_PE"); // Peru
        countryCodeToRingSound.put(52,"OUTGOING_CALL_ES_MX"); // Mexico
        countryCodeToRingSound.put(54,"OUTGOING_CALL_ES_AR"); // Argentina
        countryCodeToRingSound.put(56,"OUTGOING_CALL_ES_CL"); // Chile
        countryCodeToRingSound.put(57,"OUTGOING_CALL_ES_CO"); // Columbia
        countryCodeToRingSound.put(65,"OUTGOING_CALL_EN_GB"); // Singapore (same as UK)
        countryCodeToRingSound.put(66,"OUTGOING_CALL_TH_TH"); // Thailand
        countryCodeToRingSound.put(244,"OUTGOING_CALL_PT_AO"); // Angola
        countryCodeToRingSound.put(357,"OUTGOING_CALL_EL_CY"); // Cyprus
        countryCodeToRingSound.put(421,"OUTGOING_CALL_SK_SK"); // Slovakia
        countryCodeToRingSound.put(504,"OUTGOING_CALL_ES_HN"); // Honduras
        countryCodeToRingSound.put(506,"OUTGOING_CALL_ES_CR"); // Costa Rica
        countryCodeToRingSound.put(507,"OUTGOING_CALL_ES_PA"); // Panama
        countryCodeToRingSound.put(591,"OUTGOING_CALL_ES_BO"); // Bolivia
    }

    /**
     * Update the outgoing call (ringback) sound, based on the user's telephony
     * region.  Called after we have discovered the country code from EAS.
     * @param countryCode The country code for this subscriber.
     */
    public static void updateOutgoingCallSound(int countryCode)
    {
        logger.debug("Setting ring sound based on country code " + countryCode);
        ResourceManagementService resources =
            NotificationWiringActivator.getResources();

        String soundFile = countryCodeToRingSound.get(countryCode);
        String soundPath;

        if (StringUtils.isNullOrEmpty(soundFile))
        {
            logger.error("No ringback sound path configured for country code " +
                         countryCode);
            soundPath = resources.getSoundPath("OUTGOING_CALL");
        }
        else
        {
            soundPath = resources.getSoundPath(soundFile);

            if (StringUtils.isNullOrEmpty(soundPath))
            {
                logger.error(
                    "No default ringing sound for country code: " + countryCode +
                                       " (found file name: " + soundPath + ")");
                soundPath = resources.getSoundPath("OUTGOING_CALL");
            }
        }

        outgoingCall = soundPath;
    }

    /**
     * @return Config string for the outgoing call sound for this region.
     */
    public static String getOutgoingCall()
    {
        return outgoingCall;
    }

    private SoundProperties() {
    }
}
