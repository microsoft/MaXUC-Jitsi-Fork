/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import org.jitsi.service.neomedia.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An event class representing that an incoming or an outgoing call has been
 * created. The event id indicates the exact reason for this event.
 *
 * @author Emil Ivov
 */
public class CallEvent
    extends EventObject
{
    /**
     * An event id value indicating that this event has been triggered as a
     * result of a call being ended (all its peers have left).
     */
    public static final int CALL_ENDED  = 3;

    /**
     * An event id value indicating that this event has been triggered as a
     * result of an outgoing call.
     */
    public static final int CALL_INITIATED = 1;

    /**
     * An event id value indicating that this event has been triggered as a
     * result of an incoming call.
     */
    public static final int CALL_RECEIVED  = 2;

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Determines whether this event has been fired to indicate an incoming or
     * an outgoing call.
     */
    private final int eventID;

    /**
     * The media types supported by this call, if information is
     * available.
     */
    private final Map<MediaType, MediaDirection> mediaDirections;

    /**
     * The conference of the call for this event. Must be set when creating this
     * event, because when a call ends, the call conference may be released just
     * after creating this event, but its reference will still be necessary in
     * the futur for the UI (i.e to release the call panel),
     */
    private final CallConference conference;

    /**
     * If true, this call has been auto-answered.
     */
    private final boolean isAutoAnswered;

    /**
     * Creates an event instance indicating that an incoming/outgoing call
     * has been created
     *
     * @param call the call that triggered this event.
     * @param eventID determines whether this is an incoming or an outgoing
     * call.
     */
    public CallEvent(Call call, int eventID)
    {
        this(call, eventID, null, false);
    }

    /**
     * Initializes a new <tt>CallEvent</tt> instance which is to represent an
     * event fired by a specific <tt>Call</tt> as its source.
     *
     * @param call the <tt>Call</tt> which will fire the new instance
     * @param eventID the indicator which determines whether the new instance
     * will represent an event notifying that an incoming <tt>Call</tt> was
     * received, an outgoing <tt>Call</tt> was initiated, or a <tt>Call</tt>
     * ended
     * @param mediaDirections
     * @param isAutoAnswered if true this call has been auto-answered, false
     * otherwise.
     */
    public CallEvent(
            Call call,
            int eventID,
            Map<MediaType, MediaDirection> mediaDirections,
            boolean isAutoAnswered)
    {
        super(call);

        this.eventID = eventID;
        this.isAutoAnswered = isAutoAnswered;

        /* Make  */
        Map<MediaType, MediaDirection> thisMediaDirections
            = new HashMap<>();

        if (mediaDirections != null)
            thisMediaDirections.putAll(mediaDirections);
        this.mediaDirections = Collections.unmodifiableMap(thisMediaDirections);

        this.conference = call.getConference();
    }

    /**
     * Returns an event ID int indicating whether this event was triggered by
     * an outgoing or an incoming call.
     *
     * @return on of the CALL_XXX static member ints.
     */
    public int getEventID()
    {
        return this.eventID;
    }

    /**
     * Returns the <tt>Call</tt> that triggered this event.
     *
     * @return the <tt>Call</tt> that triggered this event.
     */
    public Call getSourceCall()
    {
        return (Call)getSource();
    }

    /**
     * Returns the <tt>CallConference</tt> that triggered this event.
     *
     * @return the <tt>CallConference</tt> that triggered this event.
     */
    public CallConference getCallConference()
    {
        return this.conference;
    }

    /**
     * @return true if this call has been auto-answered, false otherwise.
     */
    public boolean isAutoAnswered()
    {
        return isAutoAnswered;
    }

    /**
     * Returns a String representation of this CallEvent.
     *
     * @return A String representation of this CallEvent.
     */
    @Override
    public String toString()
    {
        return
            "CallEvent:[ id=" + getEventID() + " Call=" + getSourceCall() + "]";
    }
}
