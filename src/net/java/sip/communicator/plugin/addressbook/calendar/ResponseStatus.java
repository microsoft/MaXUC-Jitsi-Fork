// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook.calendar;

/**
 * Response statuses of the calendar events (meeting objects).
 */
public enum ResponseStatus
{
    /**
     * No response is required for this object.
     */
    respNone(0x00000000L),

    /**
     * This meeting belongs to the organizer.
     */
    respOrganized(0x00000001L),

    /**
     * This value on the attendee's meeting indicates that the attendee has
     * tentatively accepted the meeting request.
     */
    respTentative(0x00000002L),

    /**
     * This value on the attendee's meeting t indicates that the attendee
     * has accepted the meeting request.
     */
    respAccepted(0x00000003L),

    /**
     * This value on the attendee's meeting indicates that the attendee has
     * declined the meeting request.
     */
    respDeclined(0x00000004L),

    /**
     * This value on the attendee's meeting indicates the attendee has not
     * yet responded.
     */
    respNotResponded(0x00000005L);

    /**
     * The ID of the property
     */
    private final long id;

    ResponseStatus(long id)
    {
        this.id = id;
    }

    /**
     * Finds <tt>ResponseStatuse</tt> instance by given value of the status.
     * @param value the value of the status we are searching for.
     * @return the status or <tt>FREE</tt> if no status is found.
     */
    public static ResponseStatus getFromLong(long value)
    {
        for(ResponseStatus state : values())
        {
            if(state.getID() == value)
            {
                return state;
            }
        }
        return respNone;
    }

    /**
     * Returns the ID of the status.
     * @return the ID of the status.
     */
    private long getID()
    {
        return id;
    }
}
