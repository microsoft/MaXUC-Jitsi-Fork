/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import com.google.common.annotations.VisibleForTesting;

import org.jitsi.service.resources.*;

/**
 * The class is used to represent the state of the connection of a given
 * ProtocolProvider or Contact. It is up to the implementation to determine the
 * exact states that an object might go through. An IM provider for example
 * might go through states like, CONNECTING, ON-LINE, AWAY, etc, A status
 * instance is represented by an integer varying from 0 to 100, a Status Name
 * and a Status Description.
 *
 * The integer status variable is used so that the users of the service get the
 * notion of whether or not a given Status instance represents a state that
 * allows communication (above 20) and so that it could compare instances
 * between themselves (e.g. for sorting a ContactList for example).
 *
 * A state may not be created by the user. User may request a status change
 * giving parameters requested by the ProtocolProvider. Once a statue is
 * successfully entered by the provider, a ConnectivityStatus instance is
 * conveyed to the user through a notification event.
 *
 * @author Emil Ivov
 */
public class PresenceStatus
    implements Comparable<PresenceStatus>
{
    /**
     * An integer above which all values of the status coefficient indicate that
     * a status with connectivity (communication is possible) but the user is
     * indicating they do not want to be disturbed (they have enabled DND).
     * This indicates presence from a phone, but not an IM client.
     */
    public static final int ONLINE_LSM_THRESHOLD = 1;

    /**
     * An integer above which all values of the status coefficient indicate that
     * a status with connectivity (communication is possible) but the user is
     * indicating they do not want to be disturbed (they have enabled DND).
     * This indicates presence from an IM client.
     */
    public static final int ONLINE_THRESHOLD = 20;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability but the person is rejecting calls. This
     * indicates presence from a phone, but not an IM client.
     */
    public static final int DND_LSM_THRESHOLD = 29;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability but the person is rejecting calls. This
     * indicates presence from an IM client.
     */
    public static final int DND_THRESHOLD = 30;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability but the person is busy. This indicates
     * presence from an IM client.
     */
    public static final int BUSY_THRESHOLD = 32;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability but the person is on the phone. This
     * indicates presence from a phone, but not an IM client.
     */
    public static final int ON_THE_PHONE_LSM_THRESHOLD = 33;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability but the person is on the phone. This
     * indicates presence from an IM client.
     */
    public static final int ON_THE_PHONE_THRESHOLD = 34;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability but the person is in a meeting. This
     * indicates presence from an IM client.
     */
    public static final int MEETING_THRESHOLD = 36;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability but the person is away from the computer.
     * This indicates presence from an IM client.
     */
    public static final int AWAY_THRESHOLD = 41;

    /**
     * An integer representing that the person's phone is ringing.  This
     * doesn't fit into the "threshold" system, but is currently an unused
     * value so will work for this purpose.  This value has been chosen as the
     * "away threshold" exact value is used for the "on-the-phone" state. This
     * indicates presence from an IM client.
     */
    public static final int RINGING_VALUE = 45;

    /**
     * An integer above which all values of the status coefficient indicate
     * availability for calls. This indicates presence from a phone, but not an
     * IM client.
     */
    public static final int AVAILABLE_FOR_CALLS_LSM_THRESHOLD = 46;

    /**
     * An integer above which all values of the status coefficient indicate
     * availability for calls. This indicates presence from an IM client.
     */
    public static final int AVAILABLE_FOR_CALLS_THRESHOLD = 48;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability. This indicates presence from an IM
     * client.
     */
    public static final int AVAILABLE_THRESHOLD = 50;

    /**
     * An integer above which all values of the status coefficient indicate
     * eagerness to communicate. This indicates presence from an IM client.
     */
    public static final int EAGER_TO_COMMUNICATE_THRESHOLD = 80;

    /**
     * An integer indicating the maximum possible value of the status field.
     */
    public static final int MAX_STATUS_VALUE = 100;

    /**
     * The relative importance (to the user and therefore to the UI) of the DND
     * status.
     */
    private static final int DND_IMPORTANCE = 12;

    /**
     * The relative importance (to the user and therefore to the UI) of the DND
     * LSM status.
     */
    private static final int DND_LSM_IMPORTANCE = 11;

    /**
     * The relative importance (to the user and therefore to the UI) of the Busy
     * status.
     */
    private static final int BUSY_IMPORTANCE = 10;

    /**
     * The relative importance (to the user and therefore to the UI) of the On
     * The Phone status.
     */
    private static final int ON_THE_PHONE_IMPORTANCE = 9;

    /**
     * The relative importance (to the user and therefore to the UI) of the On
     * The Phone LSM status.
     */
    private static final int ON_THE_PHONE_LSM_IMPORTANCE = 8;

    /**
     * The relative importance (to the user and therefore to the UI) of the
     * Meeting status.
     */
    private static final int MEETING_IMPORTANCE = 7;

    /**
     * The relative importance (to the user and therefore to the UI) of the
     * Eager to Communicate/Free For Chat status.
     */
    private static final int EAGER_TO_COMMUNICATE_IMPORTANCE = 6;

    /**
     * The relative importance (to the user and therefore to the UI) of the
     * Available status.
     */
    private static final int AVAILABLE_IMPORTANCE = 5;

    /**
     * The relative importance (to the user and therefore to the UI) of the Away
     * status.
     */
    private static final int AWAY_IMPORTANCE = 4;

    /**
     * The relative importance (to the user and therefore to the UI) of the
     * Available for Calls status.
     */
    private static final int AVAILABLE_FOR_CALLS_IMPORTANCE = 3;

    /**
     * The relative importance (to the user and therefore to the UI) of the
     * Offline status.
     */
    private static final int OFFLINE_IMPORTANCE = 2;

    /**
     * The relative importance (to the user and therefore to the UI) of the
     * Available for Calls LSM status.
     */
    private static final int AVAILABLE_FOR_CALLS_LSM_IMPORTANCE = 1;

    /**
     * The relative importance (to the user and therefore to the UI) of the
     * Offline LSM status.
     */
    private static final int OFFLINE_LSM_IMPORTANCE = 0;

    /**
     * An image that graphically represents the status.
     */
    protected final BufferedImageFuture statusIcon;

    /**
     * Represents the connectivity status on a scale from 0 to 100 with 0
     * indicating complete disability for communication and 100 maximum ability
     * and user willingness. Implementors of this service should respect the
     * following indications for status values. 0 - complete disability 1:10 -
     * initializing. 1:20 - trying to enter a state where communication is
     * possible (Connecting ..) 20:50 - communication is possible but might be
     * unwanted, inefficient or delayed(e.g. Away state in IM clients) 50:80 -
     * communication is possible (On - line) 80:100 - communication is possible
     * and user is eager to communicate. (Free for chat! Talk to me, etc.)
     */
    protected final int status;

    /**
     * The name of this status instance (e.g. Away, On-line, Invisible, etc.).
     * This string is translated.
     */
    protected final String statusName;

    /**
     * Keep track if the user's avatar has changed or not.
     */
    private boolean avatarChanged;

    /**
     * Creates an instance of this class using the specified parameters.
     * This method may only be used outside the package in test code.
     *
     * @param status the status variable representing the new instance
     * @param statusName the name of this PresenceStatus
     */
    @VisibleForTesting
    public PresenceStatus(int status, String statusName)
    {
        this(status, statusName, null);
    }

    /**
     * Creates an instance of this class using the specified parameters.
     *
     * @param status the status variable representing the new instance
     * @param statusName the name of this PresenceStatus
     * @param statusIcon an image that graphically represents the status or null
     *            if no such image is available.
     */
    protected PresenceStatus(int status, String statusName, BufferedImageFuture statusIcon)
    {
        this.status = status;
        this.statusName = statusName;
        this.statusIcon = statusIcon;
    }

    /**
     * Returns an integer representing the presence status on a scale from 0 to
     * 100.
     *
     * @return a short indicating the level of availability corresponding to
     *         this status object.
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * Returns the name of this status (such as Away, On-line, Invisible, etc).
     *
     * @return a String variable containing the name of this status instance.
     */
    public String getStatusName()
    {
        return statusName;
    }

    /**
     * Returns a string representation of this provider status. Strings returned
     * by this method have the following format: PresenceStatus:<STATUS_STRING>:
     * <STATUS_MESSAGE> and are meant to be used for logging/debugging purposes.
     *
     * @return a string representation of this object.
     */
    public String toString()
    {
        return "PresenceStatus:" + getStatusName();
    }

    /**
     * Indicates whether the user is Online (can be reached) or not.
     *
     * @return true if the status coefficient is higher than the
     *         ONLINE_THRESHOLD and false otherwise
     */
    public boolean isOnline()
    {
        return getStatus() >= ONLINE_THRESHOLD;
    }

    /**
     * Indicates whether the user is online and available for calls, whether
     * that be on the client or a deskphone. NB: This method originally checked
     * for only chat availability, but for 'Notify when available' to work for
     * non-client lines, it must also cover all LSM statuses.
     *
     * @return true if the status coefficient is higher than the
     *         AVAILABLE_FOR_CALLS_LSM_THRESHOLD and false otherwise
     */
    public boolean isAvailableForCalls()
    {
        return getStatus() >= AVAILABLE_FOR_CALLS_LSM_THRESHOLD;
    }

    /**
     * Compares this instance with the specified object for ordering status for
     * presenting status in the UI. Returns a negative integer, zero, or a
     * positive integer as this status instance is considered to represent
     * less, as much, or more importance from a UI perspective than the one
     * specified by the parameter.
     * <p>
     *
     * @param target the <code>PresenceStatus</code> to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     */
    public int compareTo(PresenceStatus target)
        throws ClassCastException,
        NullPointerException
    {
        int currentImportance = convertAvailabilityToImportance(getStatus());
        int targetImportance =
            convertAvailabilityToImportance(target.getStatus());
        return (currentImportance - targetImportance);
    }

    /**
     * Converts a presence status in terms of availability into a status in
     * terms of importance in displaying in the UI.  e.g. it's more important
     * that a buddy is DND that that they are away (but away is more available).
     * The ordering is (from highest to lowest):
     * <ul>
     * <li>DND</li>
     * <li>On the phone</li>
     * <li>Free for chat/Eager to communicate</li>
     * <li>Available</li>
     * <li>Away</li>
     * <li>Offline</li>
     * </ul>
     *
     * @param availability The presence status to convert
     * @return The importance of the input status from a UI perspective.
     */
    private int convertAvailabilityToImportance(int availability)
    {
        if (availability < ONLINE_LSM_THRESHOLD)
        {
            // Offline LSM
            return OFFLINE_LSM_IMPORTANCE;
        }
        else if (availability < ONLINE_THRESHOLD)
        {
            // Offline
            return OFFLINE_IMPORTANCE;
        }
        else if (availability < DND_LSM_THRESHOLD)
        {
            // DND LSM
            return DND_LSM_IMPORTANCE;
        }
        else if (availability < DND_THRESHOLD)
        {
            // DND
            return DND_IMPORTANCE;
        }
        else if (availability < BUSY_THRESHOLD)
        {
            // Busy
            return BUSY_IMPORTANCE;
        }
        else if (availability < ON_THE_PHONE_LSM_THRESHOLD)
        {
            // On the phone LSM
            return ON_THE_PHONE_LSM_IMPORTANCE;
        }
        else if (availability < ON_THE_PHONE_THRESHOLD)
        {
            // On the phone
            return ON_THE_PHONE_IMPORTANCE;
        }
        else if (availability < MEETING_THRESHOLD)
        {
            // Meeting
            return MEETING_IMPORTANCE;
        }
        else if (availability < AWAY_THRESHOLD)
        {
            return AWAY_IMPORTANCE;
        }
        else if (availability < AVAILABLE_FOR_CALLS_LSM_THRESHOLD)
        {
            // Available for calls LSM
            return AVAILABLE_FOR_CALLS_LSM_IMPORTANCE;
        }
        else if (availability < AVAILABLE_FOR_CALLS_THRESHOLD)
        {
            // Available for calls
            return AVAILABLE_FOR_CALLS_IMPORTANCE;
        }
        else if (availability < EAGER_TO_COMMUNICATE_THRESHOLD)
        {
            // Available
            return AVAILABLE_IMPORTANCE;
        }

        // Free for chat/Eager to communicate
        return EAGER_TO_COMMUNICATE_IMPORTANCE;
    }

    /**
     * Indicates whether some other object is "equal to" this one. To
     * PresenceStatus instances are considered equal if and only if both their
     * connectivity coefficient and their name are equal.
     * <p>
     *
     * @param obj the reference object with which to compare.
     * @return <tt>true</tt> if this presence status instance is equal to the
     *         <code>obj</code> argument; <tt>false</tt> otherwise.
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof PresenceStatus))
            return false;

        PresenceStatus status = (PresenceStatus) obj;

        return status.getStatus() == getStatus()
            && status.getStatusName().equals(getStatusName());
    }

    /**
     * Returns a hash code value for the object. This method is supported for
     * the benefit of hashtables such as those provided by
     * <tt>java.util.Hashtable</tt>.
     * <p>
     *
     * @return a hash code value for this object (which is actually the result
     *         of the getStatusName().hashCode()).
     */
    public int hashCode()
    {
        return getStatusName().hashCode();
    }

    /**
     * Returns an image that graphically represents the status.
     *
     * @return a byte array containing the image that graphically represents the
     *         status or null if no such image is available.
     */
    public BufferedImageFuture getStatusIcon()
    {
        return statusIcon;
    }

    /**
     * Returns whether the user's avatar has changed or not.
     *
     * @return whether the user's avatar has changed or not.
     */
    public boolean getAvatarChanged()
    {
        return avatarChanged;
    }

    /**
     * Set the PresenceStatus's avatarChanged attribute.
     */
    public void setAvatarChanged(boolean changed)
    {
        avatarChanged = changed;
    }
}
