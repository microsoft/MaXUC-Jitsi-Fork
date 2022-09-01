// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class is designed to be used to help work with APIs that notify of state
 * changes.
 *
 * These APIs are hard to work with because it's not possible to differentiate
 * whether the state change was due to a request made by us (and
 * hence no action is required - like an ack) or if it was due some other event
 * such as user pressing a button (and action is required). See diagrams below.
 *
 * This class has methods to help determine whether a state change event from these
 * APIs act as an ack or if this is a genuine state change that we need to process:
 *
 * - If there are requests that have been sent that are waiting for an ack and
 * haven't yet timed out, we assume that a message received is an ack,
 * - Otherwise we assume this is a genuine event that we need to process.
 *
 *
 * Example diagrams:
 *
 * State change due to request made by Accession
 *
 * Accession            Other API
 *     |                    |
 *     | --setMute(true)--> |
 *     |                    |
 *     | <---mute(true)---- |
 *     |                    |
 *
 *
 * State change due to other events
 *
 * Accession            Other API
 *     |                    |
 *     |                    |  Press mute button
 *     | <---mute(true)---- |
 *     |                    |
 *
 */
public class StateChangeApiAckTracker
{
    /**
     * Timer used for scheduling tasks to time out tracking of an unacked request
     */
    private final Timer timer = new Timer("StateChangeApiAckTracker Timer");

    /**
     * Queue of outstanding timer tasks for timing out waiting for acks
     */
    private final Queue<TimerTask> unackedRequests =
            new ConcurrentLinkedQueue<>();

    /**
     * Notify this object that a request has been sent so that it can keep track
     * of whether responses received are acks.
     *
     * This will only wait for ack responses due to this request for the time
     * specified in timeout.
     *
     * @param timeout is the time in ms that we should wait for an ack for this
     *      request
     */
    public void requestSent(int timeout)
    {
        // Create a task for timing out us waiting for the ack
        TimerTask timeoutTask = new TimerTask()
        {
            public void run()
            {
                // Remove the oldest (head) task. If unackedRequests is empty
                // this won't crash and nothing will happen
                unackedRequests.poll();
            }
        };

        timer.schedule(timeoutTask, timeout);
        unackedRequests.add(timeoutTask);
    }

    /**
     * Notify this object that a response has been received so that it can
     * determine whether the response that was received if an ack for a
     * request or not.
     *
     * The user must notify of every response that was received regardless if
     * the user notifies of this object of requests sent regardless of whether
     * it cares about whether this is an ack or not.
     *
     * @return true if the response was an ack for a request or if this was
     *      a real event.
     */
    public boolean isReceivedMessageAnAck()
    {
        boolean isAck = false;

        // If there are still requests waiting for acks, then this is likely to
        // be an ack
        if (unackedRequests.size() > 0)
        {
            isAck = true;

            // Cancel the task for the oldest request waiting for an ack
            TimerTask task = unackedRequests.poll();
            if (task != null)
            {
                task.cancel();
            }
        }

        return isAck;
    }

    public void stop()
    {
        timer.cancel();
    }
}
