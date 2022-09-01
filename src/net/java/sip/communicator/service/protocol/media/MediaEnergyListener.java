// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.media;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A class for listening to the media energy of a call
 */
public class MediaEnergyListener implements SoundLevelListener
{
    private static final Logger logger =
        Logger.getLogger(MediaEnergyListener.class);

    public int getMinimumEnergy()
    {
        return mMinimumEnergy;
    }

    public int getMaximumEnergy()
    {
        return mMaximumEnergy;
    }

    public long getStartTime()
    {
        return mStartTime;
    }

    public float getAverageEnergy()
    {
        if (mStartTime == 0)
        {
            return 0.0f;
        }

        long currentTime = updateTotalEnergy();
        long runningTime = currentTime - mStartTime;

        return (float)mTotalEnergy / runningTime;
    }

    /**
     * The minimum energy that we have received.  Initially very large so that
     * it takes the value of the first event
     */
    private int mMinimumEnergy = Integer.MAX_VALUE;

    /**
     * The maximum energy that we have received.  Initially very small so that
     * it takes the value of the first event
     */
    private int mMaximumEnergy = Integer.MIN_VALUE;

    /**
     * Total energy received in units of energy * milliseconds. Thus the average
     * is this total divided by the running time
     */
    private long mTotalEnergy = 0;

    /**
     * The current energy - i.e. the energy of the last event
     */
    private int mCurrentEnergy = 0;

    /**
     * The time we started, i.e. the time of the first event (in milliseconds)
     */
    private long mStartTime = 0;

    /**
     * The time of the last event (in milliseconds)
     */
    private long mLastEventTime = 0;

    /**
     * The type of this listener
     */
    private final String mListenerType;

    /**
     * Constructor
     *
     * @param listenerType The type media that this listener will be listening
     * for (e.g. received or sent)
     */
    public MediaEnergyListener(String listenerType)
    {
        mListenerType = listenerType;
    }

    /**
     * Reset this listener so that it can be used again
     */
    public synchronized void reset()
    {
        mTotalEnergy = 0;
        mCurrentEnergy = 0;
        mStartTime = 0;
        mLastEventTime = 0;

        // Reset the max and min too:
        mMaximumEnergy = Integer.MIN_VALUE;
        mMinimumEnergy = Integer.MAX_VALUE;
    }

    public synchronized void soundLevelChanged(Object source, int level)
    {
        long currentTime = updateTotalEnergy();

        if (mStartTime == 0)
        {
            logger.debug("Got first sound level: " + level +
                                         " on listener type: " + mListenerType);
            mStartTime = currentTime;
        }

        mLastEventTime = currentTime;
        mCurrentEnergy = level;

        if (level > mMaximumEnergy)
            mMaximumEnergy = level;
        if (level < mMinimumEnergy)
            mMinimumEnergy = level;
    }

    /**
     * Update the total energy with what we know so far.
     *
     * Essentially this means that if this is the first event then we do nothing
     * Otherwise, we add how much energy we got between events.  I.e. we add how
     * long the level was set to it's last level.
     *
     * @return the current time
     */
    private long updateTotalEnergy()
    {
        long currentTime = System.currentTimeMillis();

        if (mStartTime != 0)
        {
            long timeSinceLastEvent = currentTime - mLastEventTime;
            mTotalEnergy += timeSinceLastEvent * mCurrentEnergy;
        }

        return currentTime;
    }

    @Override
    public synchronized String toString()
    {
        StringBuilder builder = new StringBuilder("Media Energy Listener ");
        builder.append(mListenerType);
        builder.append("\nAverage: ");

        if (mStartTime == 0)
        {
            // We haven't had any events on this stream yet - the energy is
            // undefined until we do.
            builder.append("Undefined");
        }
        else
        {
            long currentTime = updateTotalEnergy();
            long runningTime = currentTime - mStartTime;

            if (runningTime != 0)
            {
                builder.append((float)mTotalEnergy / runningTime);
            }
            else
            {
                builder.append("Undefined");
            }
        }

        builder.append("\nMaximum value: ")
               .append(mMaximumEnergy)
               .append("\nMinimum value: ")
               .append(mMinimumEnergy);

        return builder.toString();
    }
}
