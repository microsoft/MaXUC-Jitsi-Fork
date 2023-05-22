/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.globalstatus;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * The global statuses available to the system.
 * @author Damian Minkov
 */
public class GlobalStatusEnum
    extends PresenceStatus
{
    /**
     * Indicates that the user is connected and ready to communicate.
     */
    public static final String ONLINE_STATUS = "Online";

    /**
     * Indicates that the user is disconnected.
     */
    public static final String OFFLINE_STATUS = "Offline";

    /**
     * Indicates that the status of the user is unknown.
     */
    public static final String UNKNOWN_STATUS = "Unknown";

    /**
     * Indicates that the user is away.
     */
    public static final String AWAY_STATUS = "Away";

    /**
     * Indicates that the user is connected and eager to communicate.
     */
    public static final String FREE_FOR_CHAT_STATUS = "FreeForChat";

    /**
     * Indicates that the user is connected but does not want to communicate.
     */
    public static final String DO_NOT_DISTURB_STATUS = "DoNotDisturb";

    /**
     * Indicates that the user is connected but would rather not communicate.
     */
    public static final String BUSY_STATUS = "Busy";

    /**
     * Indicates that the user is on the phone.
     */
    public static final String ON_THE_PHONE_STATUS = "OnThePhone";

    /**
     * Indicates that the user is in a meeting.
     */
    public static final String IN_A_MEETING_STATUS = "InAMeeting";

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final GlobalStatusEnum ONLINE
        = new GlobalStatusEnum(
                85,
                ONLINE_STATUS,
                loadIcon("service.gui.statusicons.USER_ONLINE_ICON"),
                "service.gui.ONLINE");

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    public static final GlobalStatusEnum FREE_FOR_CHAT
        = new GlobalStatusEnum(
                65,
                FREE_FOR_CHAT_STATUS,
                loadIcon("service.gui.statusicons.USER_FFC_ICON"),
                "service.gui.FFC_STATUS");

    /**
     * The Away status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final GlobalStatusEnum AWAY
        = new GlobalStatusEnum(
                41,
                AWAY_STATUS,
                loadIcon("service.gui.statusicons.USER_AWAY_ICON"),
                "service.gui.AWAY_STATUS");

    /**
     * The in a meeting status. Indicates that the user has connectivity but
     * is in a meeting so may not want to be contacted.
     */
    public static final GlobalStatusEnum IN_A_MEETING
        = new GlobalStatusEnum(
                36,
                IN_A_MEETING_STATUS,
                loadIcon("service.gui.statusicons.USER_MEETING_ICON"),
                "service.protocol.status.MEETING");

    /**
     * The on the phone status. Indicates that the user has connectivity but
     * is on the phone so may not want to be contacted.
     */
    public static final GlobalStatusEnum ON_THE_PHONE
        = new GlobalStatusEnum(
                34,
                ON_THE_PHONE_STATUS,
                loadIcon("service.gui.statusicons.USER_ON_THE_PHONE_ICON"),
                "service.protocol.status.ON_THE_PHONE");

    /**
     * The Busy status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final GlobalStatusEnum BUSY
        = new GlobalStatusEnum(
                32,
                BUSY_STATUS,
                loadIcon("service.gui.statusicons.USER_BUSY_ICON"),
                "service.gui.BUSY_STATUS");

    /**
     * The DND status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final GlobalStatusEnum DO_NOT_DISTURB
        = new GlobalStatusEnum(
                30,
                DO_NOT_DISTURB_STATUS,
                loadIcon("service.gui.statusicons.USER_DND_ICON"),
                "service.gui.DND_STATUS");

    /**
     * The Offline  status. Indicates the user does not seem to be connected
     * to any network.
     */
    public static final GlobalStatusEnum UNKNOWN
        = new GlobalStatusEnum(
                1,
                UNKNOWN_STATUS,
                loadIcon("service.gui.statusicons.USER_NO_STATUS_ICON"),
                "service.gui.UNKNOWN_STATUS");

    /**
     * The Offline  status. Indicates the user does not seem to be connected
     * to any network.
     */
    public static final GlobalStatusEnum OFFLINE
        = new GlobalStatusEnum(
                0,
                OFFLINE_STATUS,
                loadIcon("service.gui.statusicons.USER_OFFLINE_ICON"),
                "service.gui.OFFLINE");

    /**
     * The set of states currently supported.
     */
    public static final ArrayList<GlobalStatusEnum> globalStatusSet
        = new ArrayList<>();
    static
    {
        globalStatusSet.add(ONLINE);
        globalStatusSet.add(FREE_FOR_CHAT);
        globalStatusSet.add(AWAY);
        globalStatusSet.add(ON_THE_PHONE);
        globalStatusSet.add(DO_NOT_DISTURB);
        globalStatusSet.add(OFFLINE);
        globalStatusSet.add(IN_A_MEETING);
        globalStatusSet.add(BUSY);
    }

    private static GlobalPresenceStatus offlineStatus =
        new GlobalPresenceStatus(0, OFFLINE_STATUS);

    private static GlobalPresenceStatus doNotDisturbStatus =
        new GlobalPresenceStatus(30, DO_NOT_DISTURB_STATUS);

    private static GlobalPresenceStatus busyStatus =
        new GlobalPresenceStatus(32, BUSY_STATUS);

    private static GlobalPresenceStatus meetingStatus =
        new GlobalPresenceStatus(34, ON_THE_PHONE_STATUS);

    private static GlobalPresenceStatus onThePhoneStatus =
        new GlobalPresenceStatus(36, IN_A_MEETING_STATUS);

    private static GlobalPresenceStatus awayStatus =
        new GlobalPresenceStatus(40, AWAY_STATUS);

    private static GlobalPresenceStatus availableStatus =
        new GlobalPresenceStatus(65, ONLINE_STATUS);

    /**
     * The supported status set stores all statuses supported by this
     * implementation.
     */
    public static final List<PresenceStatus> supportedStatusSet =
            new LinkedList<>();
    static
    {
        supportedStatusSet.add(availableStatus);
        supportedStatusSet.add(awayStatus);
        supportedStatusSet.add(onThePhoneStatus);
        supportedStatusSet.add(doNotDisturbStatus);
        supportedStatusSet.add(offlineStatus);
        supportedStatusSet.add(busyStatus);
        supportedStatusSet.add(meetingStatus);
    }

    private String i18NKey;

    /**
     * Creates a status with the specified connectivity coeff, name and icon.
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     * @param statusIcon the icon associated with this status
     */
    protected GlobalStatusEnum(
        int status,
        String statusName,
        BufferedImageFuture statusIcon,
        String i18NKey)
    {
        super(status, statusName, statusIcon);
        this.i18NKey = i18NKey;
    }

    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static BufferedImageFuture loadIcon(String imagePath)
    {
        return ProtocolProviderActivator.getResourceService()
            .getBufferedImage(imagePath);
    }

    /**
     * Returns the i18n name of the status.
     * @param status the status.
     * @return the i18n name of the status.
     */
    public static String getI18NStatusName(GlobalStatusEnum status)
    {
        return ProtocolProviderActivator.getResourceService()
            .getI18NString(status.i18NKey);
    }

    /**
     * Finds the status with appropriate name and return it.
     * @param name the name we search for.
     * @return the global status.
     */
    public static GlobalStatusEnum getStatusByName(String name)
    {
        for(GlobalStatusEnum gs : globalStatusSet)
        {
            if(gs.getStatusName().equals(name))
                return gs;
        }

        return null;
    }

    /**
     * Returns an iterator over all status instances supported by the global
     * status service
     *
     * @return an <tt>Iterator</tt> over all status instances supported by the
     *         sip provider.
     */
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        return supportedStatusSet.iterator();
    }

    /**
     * An implementation of <tt>PresenceStatus</tt> that enumerates all states
     * that the global status supports
     */
    private static class GlobalPresenceStatus
        extends PresenceStatus
    {
        /**
         * Creates an instance of <tt>GlobalPresenceStatus</tt> with the
         * specified parameters.
         *
         * @param status the connectivity level of the new presence status
         *            instance
         * @param statusName the name of the presence status.
         */
        private GlobalPresenceStatus(int status, String statusName)
        {
            super(status, statusName);
        }
    }
}
