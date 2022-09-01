/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * Represents an analogy of <code>Timer</code> which does not have the
 * disadvantage of <code>Timer</code> to always create its thread at
 * construction time. It also allows the currently scheduled
 * <code>TimerTask</code>s to be canceled while still being able to schedule new
 * <code>TimerTask</code>s later on.
 *
 * @author Lubomir Marinov
 */
public class TimerScheduler
{
    private final Logger logger = Logger.getLogger(TimerScheduler.class);

    /**
     * The timer which will handle all scheduled tasks.
     */
    private Timer timer;

    /**
     * Discarding any currently scheduled <code>TimerTask</code>s.
     */
    public synchronized void cancel()
    {
       if (timer != null)
       {
           timer.cancel();
           timer = null;
       }
    }

    /**
     * Gets the timer which handles all scheduled tasks. If it still doesn't
     * exists, a new <tt>Timer</tt> is created.
     *
     * @return the <tt>Timer</tt> which handles all scheduled tasks
     */
    private synchronized Timer getTimer()
    {
       if (timer == null)
           timer = new Timer(true);
       return timer;
    }

    /**
     * Schedules the specified <code>TimerTask</code> for execution after the
     * specified delay.
     *
     * @param task
     *            the <code>TimerTask</code> to be executed after the specified
     *            delay
     * @param delay
     *            the delay in milliseconds before the specified
     *            <code>TimerTask</code> is executed
     */
    public synchronized void schedule(TimerTask task, long delay)
    {
        try
        {
            getTimer().schedule(task, delay);
        }
        catch (IllegalStateException ex)
        {
            logger.error("Attempt to scedule a timer task failed ", ex);

            // Rethrow the exception so it can be handled elsewhere if necessary
            throw ex;
        }
    }

    /**
     * Schedules the specified <code>TimerTask</code> for repeated fixed-delay
     * execution, beginning after the specified delay. Subsequent executions
     * take place at approximately regular intervals separated by the specified
     * period.
     *
     * @param task
     *            the <code>TimerTask</code> to be scheduled
     * @param delay
     *            the delay in milliseconds before the specified
     *            <code>TimerTask</code> is executed
     * @param period
     *            the time in milliseconds between successive executions of the
     *            specified <code>TimerTask</code>
     */
    public synchronized void schedule(TimerTask task, long delay, long period)
    {
        try
        {
            getTimer().schedule(task, delay, period);
        }
        catch (IllegalStateException ex)
        {
            logger.error("Attempt to scedule a timer task failed ", ex);

            // Rethrow the exception so it can be handled elsewhere if necessary
            throw ex;
        }
    }
}
