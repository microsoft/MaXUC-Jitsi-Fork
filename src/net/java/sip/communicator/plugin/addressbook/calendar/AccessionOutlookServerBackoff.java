package net.java.sip.communicator.plugin.addressbook.calendar;

import net.java.sip.communicator.util.ServerBackoff;

/**
 * Class for backing off from the AccessionOutlookServer.
 */
public class AccessionOutlookServerBackoff extends ServerBackoff
{
    /** Initial backoff interval is 1000 ms. */
    public static final long INITIAL_FAIL_BACKOFF = 1000;

    /** Double the backoff interval 8 times (~ 4 minutes). */
    public static final int MAX_NUMBER_FAILURE_DOUBLES = 8;

    @Override
    public int getMaxNumberFailureDoubles()
    {
        return MAX_NUMBER_FAILURE_DOUBLES;
    }

    @Override
    public long getInitialFailBackoff()
    {
        return INITIAL_FAIL_BACKOFF;
    }

    /**
     * We should continue trying to connect to AOS, even after the
     * backoff time hits its maximum limit.
     */
    @Override
    public boolean shouldRetry()
    {
        return true;
    }
}
