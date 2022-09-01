// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

/**
 * Extend this class when implementing backoff off from a server. It will keep
 * track of the number of connection retries and the current backoff interval.
 *
 *  This class's exported interface is threadsafe.
 */
public abstract class ServerBackoff
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * The maximum number of times that we double the backoff.
     */
    private final int mMaxNumberFailureDoubles;

    /**
     * What the initial backoff interval should be.
     */
    private final long mInitialFailBackoff;

    /**
     * The current backoff, in milliseconds.
     */
    private long mBackoff = -1;

    /**
     * The number of failures since the backoff value was last reset.
     */
    private int mNumberFailuresSinceReset = 0;

    /**
     * Have we hit max doubles? The backoff time will not increase further, but
     * in some cases, we also may want to give up retrying to connect.
     */
    protected boolean mHasHitMaxDoubles = false;

    public ServerBackoff()
    {
        mMaxNumberFailureDoubles = getMaxNumberFailureDoubles();
        mInitialFailBackoff = getInitialFailBackoff();

        logger.debug("Initial backoff: " + mInitialFailBackoff + " ms, max doubles: " + mMaxNumberFailureDoubles);
    }

    public abstract int getMaxNumberFailureDoubles();

    public abstract long getInitialFailBackoff();

    /**
     * Method to be called when there is a connection failure. It will increase
     * the backoff.
     */
    public synchronized void onError()
    {
        mNumberFailuresSinceReset++;
        logger.debug("Failure reported. Number of failures = " + mNumberFailuresSinceReset);

        // The first time we fail we retry immediately; the second time we
        // fail we want to use the initial backoff. So we can go round 2 extra
        // times before hitting the limit.
        if (mNumberFailuresSinceReset <= (mMaxNumberFailureDoubles + 2))
        {
            mHasHitMaxDoubles = false;
            calcBackoff();
        }
        else
        {
            logger.info("Hit the maximum number " + mMaxNumberFailureDoubles + " of failure doubles");
            mHasHitMaxDoubles = true;
        }
    }

    public abstract boolean shouldRetry();

    /**
     * Recalculate the value of the backoff.
     */
    private void calcBackoff()
    {
        if (mNumberFailuresSinceReset == 0)
        {
            // No failures - reset the backoff
            if (mBackoff != -1)
            {
                logger.debug("Backoff reset.");
            }

            mBackoff = -1;
        }
        else if (mNumberFailuresSinceReset == 1)
        {
            mBackoff = -1;
            logger.info("First failure so allow an instant retry");
        }
        else
        {
            // Some failures - set the backoff to be the initial fail backoff
            // doubled for each failure.  The first time we fail we retry
            // immediately; the second time we fail we want to use the initial
            // backoff; so shift by 2 less than the number of failures.
            mBackoff = mInitialFailBackoff << (mNumberFailuresSinceReset - 2);
            logger.info("Backoff increased to " + mBackoff + " ms");
        }
    }

    private void resetBackoffInternal()
    {
        mNumberFailuresSinceReset = 0;
        mHasHitMaxDoubles = false;
        calcBackoff();
    }

    /**
     * Force-reset the backoff value to its initial state.
     */
    public synchronized void resetBackoff()
    {
        logger.info("Asked to reset backoff");
        resetBackoffInternal();
    }

    /**
     * Called if we have successfully communicated with the server.
     */
    public synchronized void onSuccess()
    {
        logger.debug("Success reported");
        resetBackoffInternal();
    }

    /**
     * @return the back off that we should wait until trying again.
     */
    public synchronized long getBackOffTime()
    {
        return mBackoff;
    }

    /**
     * @return true if we should be waiting before our next retry -
     *         i.e. if the back off is greater than 0.
     */
    public synchronized boolean shouldWait()
    {
        return (mBackoff != -1);
    }
}
