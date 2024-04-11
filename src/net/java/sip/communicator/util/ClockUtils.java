// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * A simple helper class which we use to proxy calls to Date/Calendar
 * in order to make it mockable in tests.
 */
public class ClockUtils
{
    /**
     * Just returns new Date();
     */
    public static Date getDateNow()
    {
        return new Date();
    }

    /**
     * Just returns Calendar.getInstance(timeZone).
     */
    public static Calendar getCalendar(TimeZone timeZone)
    {
        return Calendar.getInstance(timeZone);
    }
}
