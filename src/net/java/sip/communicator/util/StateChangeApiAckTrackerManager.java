// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Manages multiple StateChangeApiAckTracker, one for each value of the enum
 * with the event types that need ack tracking.
 *
 * @param <T> enum with the types of events that need ack tracking.
 */
public class StateChangeApiAckTrackerManager<T extends Enum<T>>
{
    private static final Logger logger =
        Logger.getLogger(StateChangeApiAckTrackerManager.class);

    // Default time we wait for acks until we give up.
    private int ackTimeout = 2000;

    // Map that maps the request types from the enum to its ack tracker.
    private Map<T, StateChangeApiAckTracker> ackTrackers;

    // The enum type
    private Class<T> enumType;

    public StateChangeApiAckTrackerManager(Class<T> enumType)
    {
        this.enumType = enumType;
        ackTrackers = Collections.synchronizedMap(
                new EnumMap<>(
                        enumType)
                );

        // Create the ack trackers
        resetAckTrackers();
    }

    /**
     * Set the amount of time we should wait until we timeout waiting for acks
     *
     * @param timeout amount of time in ms
     */
    public void setAckTimeout(int timeout)
    {
        ackTimeout = timeout;
    }

    /**
     * Recreates (or creates if they don't exist yet) the ack trackers for
     * all the request types.
     */
    public void resetAckTrackers()
    {
        for (T requestType: enumType.getEnumConstants())
        {
            // If an ack tracker already exists, this will replace the old one
            // with the one created here
            StateChangeApiAckTracker oldAckTracker = ackTrackers.get(requestType);
            if (oldAckTracker != null)
            {
                oldAckTracker.stop();
            }
            ackTrackers.put(requestType, new StateChangeApiAckTracker());
        }
    }

    /**
     * Notifies the appropriate ack tracker for the requestType that the request
     * has been sent.
     *
     * @param requestType is the type of request
     */
    public void requestSent(T requestType)
    {
        StateChangeApiAckTracker ackTracker = ackTrackers.get(requestType);

        if (ackTracker != null)
        {
            ackTracker.requestSent(ackTimeout);
        }
        else
        {
            logger.error("No ack tracker for the request that was sent.");
        }
    }

    /**
     * Notifies the appropriate ack tracker for the requestType that a response
     * has been received and returns whether the received message is an ack.
     *
     * @param requestType is the type of request
     *
     * @return true if the response was an ack for a request or if this was
     *      a real event.
     */
    public boolean isReceivedMessageAnAck(T requestType)
    {
        StateChangeApiAckTracker ackTracker = ackTrackers.get(requestType);

        // Default isAck to false so that the response is treated as an actual
        // event if for some reason we don't have an ack tracker for this
        // request type, although this should never happen.
        boolean isAck = false;

        if (ackTracker != null)
        {
            isAck = ackTracker.isReceivedMessageAnAck();
        }
        else
        {
            logger.error("No ack tracker for the response that was received.");
        }

        return isAck;
    }
}
