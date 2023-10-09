/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.jabberconstants;

import java.util.*;

import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.ServiceUtils.ServiceCallback;

import org.jitsi.service.resources.*;

/**
 * The <tt>JabberStatusEnum</tt> gives access to presence states for the Sip
 * protocol. All status icons corresponding to presence states are located with
 * the help of the <tt>imagePath</tt> parameter
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class JabberStatusEnum
{
    /**
     * The resources service
     */
    private static final ResourceManagementService resourceService =
        ProtocolProviderActivator.getResourceService();

    /**
     * The Online status. Indicate that the user is available for
     * calls and IM.
     */
    public static final String AVAILABLE =
        resourceService.getI18NString("service.protocol.status.AVAILABLE");

    /**
     * The available for calls status. Indicate that the user is available for
     * calls but their IM client is offline.
     */
    public static final String AVAILABLE_FOR_CALLS =
        resourceService.getI18NString("service.protocol.status.AVAILABLE_FOR_CALLS");

    /**
     * The available for calls status. Indicate that the user is available for
     * calls but they have no IM client.
     */
    public static final String AVAILABLE_FOR_CALLS_LSM =
        resourceService.getI18NString("service.protocol.status.AVAILABLE_FOR_CALLS_LSM");

    /**
     * The Away status. Indicates that the user has connectivity but might not
     * be able to immediately act upon initiation of communication.
     */
    public static final String AWAY =
        resourceService.getI18NString("service.protocol.status.AWAY");

    /**
     * The DND status. Indicates that the user connectivity but prefers not
     * to be contacted.
     */
    public static final String DO_NOT_DISTURB =
        resourceService.getI18NString("service.protocol.status.DO_NOT_DISTURB");

    /**
     * The DND status. Indicates that the user has phone but not IM
     * connectivity but prefers not to be contacted.
     */
    public static final String DO_NOT_DISTURB_LSM =
        resourceService.getI18NString("service.protocol.status.DO_NOT_DISTURB_LSM");

    /**
     * The Busy status.  Indicates that the user has connectivity but is busy
     */
    public static final String BUSY =
        resourceService.getI18NString("service.protocol.status.BUSY");

    /**
     * The meeting status.  Indicates that the user has connectivity but is in
     * a meeting
     */
    public static final String IN_MEETING =
        resourceService.getI18NString("service.protocol.status.MEETING");

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    public static final String FREE_FOR_CHAT =
        resourceService.getI18NString("service.protocol.status.FREE_FOR_CHAT");

    /**
     * On The Phone Chat status.
     * Indicates that the user is talking to the phone.
     */
    public static final String ON_THE_PHONE =
        resourceService.getI18NString("service.protocol.status.ON_THE_PHONE");

    /**
     * On The Phone Chat status.
     * Indicates that the user is talking to the phone on a non-IM client.
     */
    public static final String ON_THE_PHONE_LSM =
        resourceService.getI18NString("service.protocol.status.ON_THE_PHONE_LSM");

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    public static final String EXTENDED_AWAY =
        resourceService.getI18NString("service.protocol.status.EXTENDED_AWAY");

    /**
     * Indicates that an IM-enabled client is offline.
     */
    public static final String OFFLINE =
        resourceService.getI18NString("service.protocol.status.OFFLINE");

    /**
     * Indicates a non-IM enabled phone is offline.
     */
    public static final String OFFLINE_LSM =
        resourceService.getI18NString("service.gui.OFFLINE_LSM");

    /**
     * The Unknown status. Indicate that we don't know if the user is present or
     * not.
     */
    public static final String UNKNOWN =
        resourceService.getI18NString("service.protocol.status.UNKNOWN");

    // Base (English) status strings, for use when interoperating with the IM
    // server (and other Accession clients such as AM)

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String AVAILABLE_BASE = "Online";

    /**
     * The available for calls status. Indicate that the user is available for
     * calls but their IM client is offline.<br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String AVAILABLE_FOR_CALLS_BASE = "Availale for Calls";

    /**
     * The available for calls status. Indicate that the user is available for
     * calls but they have no IM client. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String AVAILABLE_FOR_CALLS_LSM_BASE = "Availale for Calls";

    /**
     * The Away status. Indicates that the user has connectivity but might not
     * be able to immediately act upon initiation of communication. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String AWAY_BASE = "Away";

    /**
     * The DND status. Indicates that the user has connectivity but prefers not
     * to be contacted. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String DO_NOT_DISTURB_BASE = "Do Not Disturb";

    /**
     * The DND status. Indicates that the user has phone but not IM
     * connectivity but prefers not to be contacted. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String DO_NOT_DISTURB_LSM_BASE = "Do Not Disturb";

    /**
     * The Busy status.  Indicates that the user has connectivity but is busy
     * <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String BUSY_BASE = "Busy";

    /**
     * The meeting status.  Indicates that the user has connectivity but is in
     * a meeting. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String IN_MEETING_BASE = "In a Meeting";

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String FREE_FOR_CHAT_BASE = "Free For Chat";

    /**
     * On The Phone Chat status.
     * Indicates that the user is talking to the phone. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String ON_THE_PHONE_BASE = "On the Phone";

    /**
     * On The Phone Chat status.
     * Indicates that the user is talking to the phone on a non-IM client.<br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String ON_THE_PHONE_LSM_BASE = "On the Phone";

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String EXTENDED_AWAY_BASE = "Extended Away";

    /**
     * Indicates an Offline status or status with 0 connectivity. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String OFFLINE_BASE = "Offline";

    /**
     * Indicates a non-IM enabled phone is offline. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String OFFLINE_LSM_BASE = "No Presence Info";

    /**
     * The Unknown status. Indicate that we don't know if the user is present or
     * not. <br/>
     * Base (English) string, for use when inter-operating with the IM server.
     */
    private static final String UNKNOWN_BASE = "Unknown";

    /**
     * The integer that indicates the free for chat status.
     */
    public static final int FREE_FOR_CHAT_STATUS = 85;

    /**
     * The integer that indicates the available status.
     */
    public static final int AVAILABLE_STATUS = 65;

    /**
     * The integer that indicates the available for calls status.
     */
    public static final int AVAILABLE_FOR_CALLS_STATUS = 47;

    /**
     * The integer that indicates the available for calls LSM status.
     */
    public static final int AVAILABLE_FOR_CALLS_LSM_STATUS = 46;

    /**
     * The integer that indicates the away status.
     */
    public static final int AWAY_STATUS = 40;

    /**
     * The integer that indicates the extended away status.
     */
    public static final int EXTENDED_AWAY_STATUS = 37;

    /**
     * The integer that indicates the in a meeting status.
     */
    public static final int IN_MEETING_STATUS = 35;

    /**
     * The integer that indicates the on the phone status.
     */
    public static final int ON_THE_PHONE_STATUS = 33;

    /**
     * The integer that indicates the on the phone LSM status.
     */
    public static final int ON_THE_PHONE_LSM_STATUS = 32;

    /**
     * The integer that indicates the busy status.
     */
    public static final int BUSY_STATUS = 31;

    /**
     * The integer that indicates the do not disturb status.
     */
    public static final int DO_NOT_DISTURB_STATUS = 29;

    /**
     * The integer that indicates the do not disturb LSM status.
     */
    public static final int DO_NOT_DISTURB_LSM_STATUS = 28;

    /**
     * The integer that indicates the unknown status.
     */
    public static final int UNKNOWN_STATUS = 2;

    /**
     * The integer that indicates the offline status.
     */
    public static final int OFFLINE_STATUS = 1;

    /**
     * The integer that indicates the offline LSM status.
     */
    public static final int OFFLINE_LSM_STATUS = 0;

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    protected JabberPresenceStatus availableStatus;

    /**
     * The available for calls status. Indicate that the user is available for
     * calls but their IM client is offline.
     */
    private JabberPresenceStatus availableForCallsStatus;

    /**
     * The available for calls status. Indicate that the user is available for
     * calls but they have no IM client.
     */
    private JabberPresenceStatus availableForCallsLsmStatus;

    /**
     * The Away status. Indicates that the user has connectivity but might not
     * be able to immediately act upon initiation of communication.
     */
    private JabberPresenceStatus awayStatus;

    /**
     * The DND status. Indicates that the user has connectivity but prefers not
     * to be contacted.
     */
    private JabberPresenceStatus doNotDisturbStatus;

    /**
     * The DND status. Indicates that the user has phone but not IM
     * connectivity but prefers not to be contacted.
     */
    private JabberPresenceStatus doNotDisturbLsmStatus;

    /**
     * The Busy status.  Indicates that the user has connectivity but is busy
     */
    private JabberPresenceStatus busyStatus;

    /**
     * The meeting status.  Indicates that the user has connectivity but is in a
     * meeting
     */
    private JabberPresenceStatus meetingStatus;

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    private JabberPresenceStatus freeForChatStatus;

    /**
     * Indicates an IM enabled phone is offline.
     */
    private JabberPresenceStatus offlineStatus;

    /**
     * Indicates a non-IM enabled phone is offline.
     */
    private JabberPresenceStatus offlineLsmStatus;

    /**
     * Indicates an On The Phone status.
     */
    private JabberPresenceStatus onThePhoneStatus;

    /**
     * Indicates that the user is talking to the phone on a non-IM client
     */
    private JabberPresenceStatus onThePhoneLsmStatus;

    /**
     * Indicates an Extended Away status or status.
     */
    private JabberPresenceStatus extendedAwayStatus;

    /**
     * The supported status set stores all statuses supported by this protocol
     * implementation.
     */
    private final List<JabberPresenceStatus> supportedStatusSet = new ArrayList<>();

    /**
     * The Unknown status. Indicate that we don't know if the user is present or
     * not.
     */
    private JabberPresenceStatus unknownStatus;

    private static final Map<String, JabberStatusEnum> existingEnums =
            new Hashtable<>();

    /**
     * Returns an instance of JabberStatusEnum for the specified
     * <tt>iconPath</tt> or creates a new one if it doesn't already exist.
     *
     * @param iconPath the location containing the status icons that should
     * be used by this enumeration.
     *
     * @return the newly created JabberStatusEnum instance.
     */
    public static JabberStatusEnum getJabberStatusEnum(String iconPath)
    {
        JabberStatusEnum statusEnum = existingEnums.get(iconPath);

        if(statusEnum != null)
            return statusEnum;

        statusEnum = new JabberStatusEnum();

        existingEnums.put(iconPath, statusEnum);

        return statusEnum;
    }

    /**
     * Creates a new instance of JabberStatusEnum using <tt>iconPath</tt> as the
     * root path where it should be reading status icons from.
     */
    private JabberStatusEnum()
    {
        ServiceUtils.getService(ProtocolProviderActivator.bundleContext,
                                ImageLoaderService.class,
                                new ServiceCallback<>()
        {
            @Override
            public void onServiceRegistered(ImageLoaderService service)
            {
                offlineLsmStatus =
                    new JabberPresenceStatus(OFFLINE_LSM_STATUS, true, OFFLINE_LSM, OFFLINE_LSM_BASE, service.getImage(ImageLoaderService.USER_OFFLINE_LSM_ICON));

                offlineStatus =
                    new JabberPresenceStatus(OFFLINE_STATUS, false, OFFLINE, OFFLINE_BASE, service.getImage(ImageLoaderService.USER_OFFLINE_ICON));

                doNotDisturbLsmStatus =
                    new JabberPresenceStatus(DO_NOT_DISTURB_LSM_STATUS, true, DO_NOT_DISTURB_LSM, DO_NOT_DISTURB_LSM_BASE, service.getImage(ImageLoaderService.USER_DND_LSM_ICON));

                doNotDisturbStatus =
                    new JabberPresenceStatus(DO_NOT_DISTURB_STATUS, false, DO_NOT_DISTURB, DO_NOT_DISTURB_BASE, service.getImage(ImageLoaderService.USER_DND_ICON));

                busyStatus =
                    new JabberPresenceStatus(BUSY_STATUS, false, BUSY, BUSY_BASE, service.getImage(ImageLoaderService.USER_BUSY_ICON));

                onThePhoneLsmStatus =
                    new JabberPresenceStatus(ON_THE_PHONE_LSM_STATUS, true, ON_THE_PHONE_LSM, ON_THE_PHONE_LSM_BASE, service.getImage(ImageLoaderService.USER_ON_THE_PHONE_LSM_ICON));

                onThePhoneStatus =
                    new JabberPresenceStatus(ON_THE_PHONE_STATUS, false, ON_THE_PHONE, ON_THE_PHONE_BASE, service.getImage(ImageLoaderService.USER_ON_THE_PHONE_ICON));

                meetingStatus =
                    new JabberPresenceStatus(IN_MEETING_STATUS, false, IN_MEETING, IN_MEETING_BASE, service.getImage(ImageLoaderService.USER_MEETING_ICON));

                extendedAwayStatus =
                    new JabberPresenceStatus(EXTENDED_AWAY_STATUS, false, EXTENDED_AWAY, EXTENDED_AWAY_BASE, service.getImage(ImageLoaderService.USER_AWAY_ICON));

                awayStatus =
                    new JabberPresenceStatus(AWAY_STATUS, false, AWAY, AWAY_BASE, service.getImage(ImageLoaderService.USER_AWAY_ICON));

                availableForCallsLsmStatus =
                    new JabberPresenceStatus(AVAILABLE_FOR_CALLS_LSM_STATUS, true, AVAILABLE_FOR_CALLS_LSM, AVAILABLE_FOR_CALLS_LSM_BASE, service.getImage(ImageLoaderService.USER_AVAILABLE_FOR_CALLS_LSM_ICON));

                availableForCallsStatus =
                    new JabberPresenceStatus(AVAILABLE_FOR_CALLS_STATUS, true, AVAILABLE_FOR_CALLS, AVAILABLE_FOR_CALLS_BASE, service.getImage(ImageLoaderService.USER_AVAILABLE_FOR_CALLS_ICON));

                availableStatus =
                    new JabberPresenceStatus(AVAILABLE_STATUS, false, AVAILABLE, AVAILABLE_BASE, service.getImage(ImageLoaderService.USER_ONLINE_ICON));

                freeForChatStatus =
                    new JabberPresenceStatus(FREE_FOR_CHAT_STATUS, false, FREE_FOR_CHAT, FREE_FOR_CHAT_BASE, service.getImage(ImageLoaderService.USER_FFC_ICON));

                unknownStatus =
                    new JabberPresenceStatus(UNKNOWN_STATUS, false, UNKNOWN, UNKNOWN_BASE, service.getImage(ImageLoaderService.USER_NO_STATUS_ICON));

                // Initialize the list of supported status states.
                supportedStatusSet.add(freeForChatStatus);
                supportedStatusSet.add(availableStatus);
                supportedStatusSet.add(availableForCallsStatus);
                supportedStatusSet.add(availableForCallsLsmStatus);
                supportedStatusSet.add(awayStatus);
                supportedStatusSet.add(onThePhoneStatus);
                supportedStatusSet.add(onThePhoneLsmStatus);
                supportedStatusSet.add(extendedAwayStatus);
                supportedStatusSet.add(doNotDisturbStatus);
                supportedStatusSet.add(doNotDisturbLsmStatus);
                supportedStatusSet.add(busyStatus);
                supportedStatusSet.add(meetingStatus);
                supportedStatusSet.add(offlineStatus);
                supportedStatusSet.add(offlineLsmStatus);
            }
        });
    }

    /**
     * Returns the Jabber status corresponding to the given integer.
     *
     * @param status the status level.
     * @return the Jabber status.
     */
    public JabberPresenceStatus getStatus(int status)
    {
        JabberPresenceStatus jabberPresenceStatus = unknownStatus;

        for (JabberPresenceStatus supportedStatus : supportedStatusSet)
        {
            if (supportedStatus.getStatus() == status)
            {
                jabberPresenceStatus = supportedStatus;
            }
        }

        return jabberPresenceStatus;
    }

    /**
     * Returns the Jabber status that matches the given status name.  Note
     * that, if more than one status has the same name, the first one that is
     * found will be returned. If it is required to distinguish between
     * different statuses with the same name, use the getStatus(int) method.
     *
     * @param statusName the name of the status.
     * @return the equivalent Jabber status, or <tt>unknown</tt> if there is no
     * match.
     */
    public JabberPresenceStatus getStatusFromName(String statusName)
    {
        JabberPresenceStatus jabberPresenceStatus = unknownStatus;

        for (JabberPresenceStatus presenceStatus : supportedStatusSet)
        {
            if (presenceStatus.getStatusName().equals(statusName))
            {
                jabberPresenceStatus = presenceStatus;
                break;
            }
        }

        return jabberPresenceStatus;
    }

    /**
     * Returns the Jabber status that corresponds to the non-translated base
     * status string (i.e. the status in the XMPP stanza), or Unknown if a
     * corresponding status cannot be found.
     *
     * If the status is NOT for a LSM presence, this method will not return a
     * status that is only valid for LSM lines.
     *
     * If the status IS for an LSM presence, the method will first try to find a
     * LSM-specific status. If there are no matches, it will look through all
     * the other status.
     *
     * Note that there may be multiple valid statuses with the same base name,
     * in which case only one will be returned. If it is required to distinguish
     * between different statuses with the same name, use the getStatus(int)
     * method.
     *
     * @param statusName the name of the status
     * @param isLsm whether this status is for an LSM presence
     * @return the equivalent Jabber status, or <tt>unknown</tt> if there is no
     * match.
     */
    public JabberPresenceStatus getStatusFromBaseName(String statusName,
                                                      boolean isLsm)
    {
        JabberPresenceStatus jabberPresenceStatus = unknownStatus;

        for (JabberPresenceStatus presenceStatus : supportedStatusSet)
        {
            if (!isLsm && presenceStatus.onlyValidForLsm())
            {
                // LSM status is not valid for non-LSM.
                continue;
            }

            if (presenceStatus.getBaseStatusName().equals(statusName))
            {
                jabberPresenceStatus = presenceStatus;

                if (!isLsm || presenceStatus.onlyValidForLsm())
                {
                    // We don't want to break out of the loop if this is LSM AND
                    // the presence status that we found is not specific to LSM,
                    // since we may later find a LSM-specific status.
                    break;
                }
            }
        }

        return jabberPresenceStatus;
    }

    /**
     * @param statusMessage A status message to compare
     * @return <tt>true</tt> if the passed-in status message matches the English
     * string for the <tt>UNKNOWN</tt> status.
     */
    public boolean isTheBaseUnknownString(String statusMessage)
    {
        return UNKNOWN_BASE.equalsIgnoreCase(statusMessage);
    }

    /**
     * Returns an iterator over all status instances supported by the sip
     * provider.
     *
     * @return an <tt>Iterator</tt> over all status instances supported by the
     *         sip provider.
     */
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        List<PresenceStatus> supportedPresenceStatuses =
                new LinkedList<>(supportedStatusSet);
        return supportedPresenceStatuses.iterator();
    }

    /**
     * An implementation of <tt>PresenceStatus</tt> that enumerates all states
     * that a Jabber contact can currently have.
     */
    public static class JabberPresenceStatus
        extends PresenceStatus
    {
        /**
         * The untranslated (English) name of this status instance.  This is
         * required to inter-operate between AD and other Accession clients
         * (such as AM) via the IM server.  Each client needs to be able to
         * recognise each status type in order to display the correct status
         * icon.
         */
        protected final String mBaseStatusName;

        /**
         * Whether this status instance is only valid for LSM presences.
         */
        protected final boolean mOnlyValidForLsm;

        /**
         * Creates an instance of <tt>JabberPresenceStatus</tt> with the
         * specified parameters.
         *
         * @param status the connectivity level of the new presence status
         *            instance
         * @param onlyValidForLsm whether the status is only valid for LSM presences.
         * @param statusName the name of the presence status.
         * @param baseStatusName the untranslated status name, for transmission
         * on the wire.
         * @param statusIcon the icon associated with this status
         */
        private JabberPresenceStatus(int status,
                                     boolean onlyValidForLsm,
                                     String statusName,
                                     String baseStatusName,
                                     BufferedImageFuture statusIcon)
        {
            super(status, statusName, statusIcon);

            mOnlyValidForLsm = onlyValidForLsm;
            mBaseStatusName = baseStatusName;
        }

        /**
         * Returns the untranslated name of this status, for inter-operating
         * with the IM server.
         * @return a String variable containing the fixed (untranslated) name of
         * this status instance.
         */
        public String getBaseStatusName()
        {
            return mBaseStatusName;
        }

        /**
         * Returns true if this status is only valid for LSM presences.
         */
        public boolean onlyValidForLsm()
        {
            return mOnlyValidForLsm;
        }
    }
}
