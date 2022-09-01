// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.threading;

import java.util.concurrent.atomic.*;

/**
 * Class for tasks to be scheduled using the threading service that can be
 * cancelled.  Cancelling a task while it is running does nothing
 */
public abstract class CancellableRunnable implements Runnable
{
    /**
     * True if this task has been cancelled
     */
    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    /**
     * Cancel this task, ensuring that it won't be run
     */
    public void cancel()
    {
        isCancelled.set(true);
    }

    /**
     * @return true if the task has been cancelled.
     */
    public boolean isCancelled()
    {
        return isCancelled.get();
    }
}