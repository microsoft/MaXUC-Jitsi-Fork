/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>SipStatusEnum</tt> gives access to presence states for the Sip
 * protocol. All status icons corresponding to presence states are located with
 * the help of the <tt>imagePath</tt> parameter
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class SipStatusEnum
{
    /**
     * The resources service
     */
    private static final ResourceManagementService resourceService =
        ProtocolProviderActivator.getResourceService();

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final String OFFLINE =
        resourceService.getI18NString("service.protocol.status.OFFLINE");

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final String ONLINE =
        resourceService.getI18NString("service.protocol.status.ONLINE");

    /**
     * The busy status. Indicates that the user has connectivity but is doing
     * something else.
     */
    public static final String BUSY =
        resourceService.getI18NString("service.protocol.status.BUSY");

    /**
     * The On the phone status. Indicates that the user is talking to the phone.
     */
    public static final String ON_THE_PHONE =
        resourceService.getI18NString("service.protocol.status.ON_THE_PHONE");

    /**
     * The Away  status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final String AWAY =
        resourceService.getI18NString("service.protocol.status.AWAY");

    /**
     * The Ringing status. Indicates that the user is currently in the process
     * of negotiating a SIP dialog, so their phone is probably ringing.
     */
    public static final String RINGING = "Ringing";

    /**
     * The Unknown status. Indicate that we don't know if the user is present
     * or not.
     */
    public static final String UNKNOWN =
        resourceService.getI18NString("service.protocol.status.UNKNOWN");

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    private final SipPresenceStatus offlineStatus;

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    private final SipPresenceStatus onlineStatus;

    /**
     * The busy status. Indicates that the user has connectivity but is doing
     * something else.
     */
    private final SipPresenceStatus busyStatus;

    /**
     * The On the phone status. Indicates that the user is talking to the phone.
     */
    private final SipPresenceStatus onThePhoneStatus;

    /**
     * The Ringing status. Indicates that the user is currently in the process
     * of negotiating a SIP dialog, so their phone is probably ringing.
     */
    private final SipPresenceStatus ringingStatus;

    /**
     * The Away  status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    private final SipPresenceStatus awayStatus;

    /**
     * The Unknown status. Indicate that we don't know if the user is present
     * or not.
     */
    private final SipPresenceStatus unknownStatus;

    /**
     * The supported status set stores all statuses supported by this protocol
     * implementation.
     */
    public final List<PresenceStatus> supportedStatusSet
        = new LinkedList<>();

    /**
     * Constructor.
     *
     * @param iconPath path of the icon
     */
    public SipStatusEnum(String iconPath)
    {
        this.offlineStatus = new SipPresenceStatus(
            0,
            OFFLINE,
            loadIcon(iconPath + "/sip16x16-offline.png"));

        this.busyStatus = new SipPresenceStatus(
            30,
            BUSY,
            loadIcon(iconPath + "/sip16x16-busy.png"));

        // The GlobalStatusServiceImpl requires that
        // SipPresenceStatus.ON_THE_PHONE == PresenceStatus.ON_THE_PHONE.
        // Really this should be true for all the statuses here, but I'm
        // concerned that changing them all would break something.
        this.onThePhoneStatus = new SipPresenceStatus(
            PresenceStatus.ON_THE_PHONE_THRESHOLD,
            ON_THE_PHONE,
            loadIcon(iconPath + "/sip16x16-phone.png"));

        // It is fine to have a duplicate icon here as these icons are no
        // longer available for use.
        this.ringingStatus = new SipPresenceStatus(
            32,
            RINGING,
            loadIcon(iconPath + "/sip16x16-phone.png"));

        this.awayStatus = new SipPresenceStatus(
            40,
            AWAY,
            loadIcon(iconPath + "/sip16x16-away.png"));

        this.onlineStatus = new SipPresenceStatus(
            65,
            ONLINE,
            loadIcon(iconPath + "/sip16x16-online.png"));

        this.unknownStatus = new SipPresenceStatus(
            1,
            UNKNOWN,
            loadIcon(iconPath + "/sip16x16-offline.png"));

        // Initialize the list of supported status states.
        supportedStatusSet.add(onlineStatus);
        supportedStatusSet.add(awayStatus);
        supportedStatusSet.add(ringingStatus);
        supportedStatusSet.add(onThePhoneStatus);
        supportedStatusSet.add(busyStatus);
        supportedStatusSet.add(offlineStatus);
    }

    /**
     * Returns the offline sip status.
     *
     * @param statusName the name of the status.
     * @return the offline sip status.
     */
    public SipPresenceStatus getStatus(String statusName)
    {
        if (statusName.equals(ONLINE))
            return onlineStatus;
        else if (statusName.equals(OFFLINE))
            return offlineStatus;
        else if (statusName.equals(BUSY))
            return busyStatus;
        else if (statusName.equals(ON_THE_PHONE))
            return onThePhoneStatus;
        else if (statusName.equals(RINGING))
            return ringingStatus;
        else if (statusName.equals(AWAY))
            return awayStatus;
        else
            return unknownStatus;
    }

    /**
     * Returns an iterator over all status instances supported by the sip
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * sip provider.
     */
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        return supportedStatusSet.iterator();
    }

    /**
     * Loads an image from a given image path.
     * @param imagePath The path to the image resource.
     * @return The image extracted from the resource at the specified path.
     */
    public static BufferedImageFuture loadIcon(String imagePath)
    {
        return ProtocolIconSipImpl.loadIcon(imagePath);
    }

    /**
     * An implementation of <tt>PresenceStatus</tt> that enumerates all states
     * that a SIP contact can currently have.
     */
    private static class SipPresenceStatus
        extends PresenceStatus
    {
        /**
         * Creates an instance of <tt>SipPresneceStatus</tt> with the
         * specified parameters.
         * @param status the connectivity level of the new presence status
         * instance
         * @param statusName the name of the presence status.
         * @param statusIcon the icon associated with this status
         */
        private SipPresenceStatus(  int status,
                                    String statusName,
                                    BufferedImageFuture statusIcon)
        {
            super(status, statusName, statusIcon);
        }
    }
}
