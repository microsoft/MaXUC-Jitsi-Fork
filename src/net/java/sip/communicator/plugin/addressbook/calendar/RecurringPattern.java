/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook.calendar;

import java.nio.*;
import java.text.*;
import java.util.*;

import org.apache.commons.lang3.tuple.Pair;

import net.java.sip.communicator.plugin.addressbook.OutlookUtils;

/**
 * The class represents the recurring pattern structure of calendar item.
 *
 * See PDF attached to http://msdn.microsoft.com/en-us/library/office/cc425490.aspx
 * for details on how this structure is defined.
 *
 * Note that all dates are given in minutes since midnight on the 1st of January
 * 1601.
 */
public class RecurringPattern
{
    /**
     * The maximum number of modifications that we allow.  This is mainly to
     * firewall against trying to create an array of a very large size and thus
     * hitting an Out Of Memory Exception.
     */
    private static final int MAX_NB_MODIFICATION = 16384;

    /**
     * Array with masks for days of week when the calendar item occurs.
     */
    public static int[] DAY_OF_WEEK_MASK =
    {
        0x00000001, // Sunday
        0x00000002, // Monday
        0x00000004, // Tuesday
        0x00000008, // Wednesday
        0x00000010, // Thursday
        0x00000020, // Friday
        0x00000040  // Saturday
    };

    /**
     * The default busy status for the meeting. Exceptions might have a
     * different busy status.
     */
    private final BusyStatusEnum defaultBusyStatus;

    /**
     * An integer that specifies the frequency of the recurring series. Valid
     * values are
     *
     * 0x200A = Daily
     * 0x200B = Weekly
     * 0x200C = Monthly
     * 0x200D = Yearly
     */
    private final short recurFrequency;

    /**
     * An object that specifies the type of recurrence pattern.  This can be one
     * of
     * Day (event occurs every day),
     * Week (every week),
     * Month (every month),
     * MonthNth (occurs every nth month)
     * MonthEnd (every month end),
     * HjMonth (monthly recurrence in the Hijri calendar)
     * HjMonthNth (as above but Hijri calendar)
     * HjMonthEnd (as above but Hijri calendar)
     */
    private PatternType patternType;

    /**
     * The value of calendarType field.
     *
     * Many possible values, but we will only take note of Gregorian entries,
     * either 0x0000 or 0x0001.
     */
    private short calendarType;

    /**
     * An integer that specifies the first ever day, week, or month of a
     * recurring series, dating back to a reference date, which is January 1,
     * 1601, for a Gregorian 35 / 191 calendar.
     *
     * Value and meaning depends on value of the recurFrequency field.
     *
     * Not really required since we can use the startDate field obtained elsewhere
     * but kept for consistency
     */
    @SuppressWarnings("unused")
    private int firstDateTime;

    /**
     * Integer specifying the interval at which the meeting pattern (from
     * patternSpecific fields) occurs.
     *
     * E.g. 1 - 999 for up to 999 daily recurrences
     *      1 - 99 for up to 99 monthly recurrences
     */
    private int period;

    /**
     * The value of slidingFlag field.  Only used for scheduling tasks so not
     * used here.  Kept only for consistency
     */
    @SuppressWarnings("unused")
    private int slidingFlag;

    /**
     * Structure specifying the details of the recurrence pattern.  Details of
     * which vary according to the value of the pattern type field
     */
    private int patternSpecific1;

    /**
     * Structure specifying the details of the recurrence pattern.  Details of
     * which vary according to the value of the pattern type field
     */
    private int patternSpecific2;

    /**
     * Integer specifying the end type of the recurrence.  Has the following
     * possible values
     * 0x2021 = End after date
     * 0x2022 = End after N occurrences
     * 0x2023 = Never end
     * 0xFFFFFFFF = Never end (shouldn't happen but sometimes does)
     */
    private int endType;

    /**
     * Number of times this event occurs.  Value must be computed if we end
     * after a date.
     */
    private int occurenceCount;

    /**
     * Integer specifing the day on which the calendar week begins.
     * Sunday = 0x0
     * Monday = 0x1
     * ...
     * Saturday = 0x6
     */
    private int firstDow;

    /**
     * The number of instances in the deleted instance dates field
     */
    private int deletedInstanceCount;

    /**
     * List with the start dates of deleted OR modified instances.
     */
    private List<Date> deletedInstances = new ArrayList<>();

    /**
     * The number of modified elements.  Must be less than deletedInstanceCount
     * since that also contains modified elements
     */
    private int modifiedInstanceCount;

    /**
     * Array with the start dates of modified instances.
     */
    private int[] modifiedInstances;

    /**
     * Date of the first occurrence
     */
    private Date startDate;

    /**
     * Date of the last occurrence
     */
    private Date endDate;

    /**
     * The timezone in which the meeting was created
     */
    private TimeZone creationTimeZone;

    /**
     * Number of minutes from midnight in the creation timezone to the start of
     * the meeting
     */
    private int startTimeOffset;

    /**
     * Number of minutes from midnight to the end of the meeting
     */
    private int endTimeOffset;

    /**
     * List of exception info structures included in the pattern.
     */
    private List<ExceptionInfo> exceptions;

    /**
     * List of days of week when the calendar item occurs.
     */
    private List<Integer> allowedDaysOfWeek = new LinkedList<>();

    /**
     * The binary data of the pattern.
     */
    private ByteBuffer dataBuffer;

    /**
     * Number of miliseconds in one week.
     */
    private final long ONE_WEEK_IN_MSECS = 604800000;

    /**
     * Parses the binary data that describes the recurrent pattern.
     * @param data the binary data.
     * @param parsedOutlookMeeting the calendar item.
     * @param creationTZ the timezone in which the meeting was created
     * @throws IndexOutOfBoundsException if data can't be parsed.
     */
    RecurringPattern(byte[] data,
                     TimeZone creationTZ,
                     BusyStatusEnum busyStatus)
    throws IndexOutOfBoundsException
    {
        creationTimeZone = creationTZ;
        defaultBusyStatus = busyStatus;
        dataBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // See PDF attached to http://msdn.microsoft.com/en-us/library/office/cc425490.aspx
        // for details on how this structure is defined.  In particular see
        // 2.2.1.44.1 RecurrencePattern Structure.
        // Rough overview:
        // Byte     0         1           2           3
        //          Reader Version        Writer Version
        //          Recur Frequency       Pattern Type
        //          Calendar Type         First Date Time
        //          ...                   Period
        //          ...                   Sliding Flag
        //          ...                   Pattern type specific (variable)
        //          ...                   ...
        //          End Type
        //          Occurrence Count
        //          First Day of Week
        //          Deleted Instance Count
        //          Deleted Instance Dates (variable)
        //          ...
        //          Modified Instance Count
        //          Modified Instance Dates (variable))
        //          ...
        //          Start Date
        //          End Date
        int offset = 4;
        recurFrequency = dataBuffer.getShort(offset);
        offset += 2;

        patternType = PatternType.getFromShort(dataBuffer.getShort(offset));
        offset += 2;

        calendarType = dataBuffer.getShort(offset);
        offset += 2;

        firstDateTime = dataBuffer.getInt(offset);
        offset += 4;

        period = dataBuffer.getInt(offset);
        offset += 4;

        if (period <= 0)
        {
            // Period must be between 1 and 999. If not, there has been an error
            // parsing the data.
            throw new IndexOutOfBoundsException("Period invalid: " + period);
        }

        slidingFlag = dataBuffer.getInt(offset);
        offset += 4;

        // Now we've got the pattern type, we can get the pattern specific data
        switch(patternType)
        {
        case Week:
            patternSpecific1 = dataBuffer.getInt(offset);
            patternSpecific2 = 0;
            offset +=4;

            // Weekly so pattern specific 1 contains the days on which the meeting
            // happens
            for (int day = 0; day < 7; day++)
            {
                if ((patternSpecific1 & (DAY_OF_WEEK_MASK[day])) != 0)
                {
                    allowedDaysOfWeek.add(day + 1);
                }
            }

            break;

        case Month:
        case MonthEnd:
        case HjMonth:
        case HjMonthEnd:
            // Monthly - pattern specific 1 indicates the day on the month.
            patternSpecific1 = dataBuffer.getInt(offset);
            patternSpecific2 = 0;
            offset +=4;
            break;

        case MonthNth:
        case HjMonthNth:
            // Pattern specific 1 indicates a day of week
            // Pattern specific 2 indicates which week of the month.  I.e. 0x1
            // means the first day in the month, 0x2 the second. 0x5 is special
            // and means the last day
            patternSpecific1 = dataBuffer.getInt(offset);
            patternSpecific2 = dataBuffer.getInt(offset + 4);

            // 0x7f means that every weekday is allowed so this is just a month
            // type pattern
            if (patternSpecific1 == 0x7f && patternSpecific2 != 0x5)
            {
                patternType = PatternType.Month;
            }

            for (int day = 0; day < 7; day++)
            {
                if ((patternSpecific1 & (DAY_OF_WEEK_MASK[day])) != 0)
                {
                    allowedDaysOfWeek.add((day) + 1);
                }
            }
            offset +=8;
            break;

        default:
            break;
        }

        //endType
        endType = dataBuffer.getInt(offset);
        offset += 4;

        occurenceCount = dataBuffer.getInt(offset);
        offset += 4;

        firstDow = dataBuffer.getInt(offset);
        offset += 4;

        // Deleted instances
        deletedInstanceCount = dataBuffer.getInt(offset);
        offset += 4;

        for (int i = 0; i < deletedInstanceCount; i++)
        {
            deletedInstances.add(OutlookUtils.windowsTimeToDateObject(dataBuffer.getInt(offset)));
            offset += 4;
        }

        // Modified instances (modifications are changes to individual meetings)
        modifiedInstanceCount  = dataBuffer.getInt(offset);
        offset += 4;

        if (modifiedInstanceCount > MAX_NB_MODIFICATION)
        {
            // Too many modified instances indicates something gone wrong:
            throw new IndexOutOfBoundsException("Modified too large: " + modifiedInstanceCount);
        }

        modifiedInstances = new int[modifiedInstanceCount];

        for(int i = 0; i < modifiedInstanceCount; i ++)
        {
            modifiedInstances[i] = dataBuffer.getInt(offset);
            offset += 4;
        }

        startDate = OutlookUtils.windowsTimeToDateObject(dataBuffer.getInt(offset));
        offset += 4;

        endDate = OutlookUtils.windowsTimeToDateObject(dataBuffer.getInt(offset));
        offset += 4;

        offset += 8;

        startTimeOffset = dataBuffer.getInt(offset);
        offset += 4;

        endTimeOffset = dataBuffer.getInt(offset);
        offset += 4;

        short exceptionCount = dataBuffer.getShort(offset);
        offset += 2;

        if (exceptionCount > MAX_NB_MODIFICATION)
        {
            // Too many exceptions indicates something gone wrong:
            throw new IndexOutOfBoundsException("Exceptions too large: " + exceptionCount);
        }

        // Exceptions - exceptions are times when the meeting will not happen.
        exceptions = new ArrayList<>(exceptionCount);

        for (int i = 0; i < exceptionCount; i++)
        {
            ExceptionInfo exceptionInfo = new ExceptionInfo(offset);
            exceptions.add(exceptionInfo);
            offset += exceptionInfo.sizeInBytes();
        }
    }

    /**
     * Prints the properties of the class for debugging purpose.
     */
    @Override
    public String toString()
    {
        String result = "";
        result += "recurFrequency: " + String.format("%#02x", this.recurFrequency) +
            ", patternType: " + String.format("%#02x", this.patternType.getValue()) +
            ", calendarType: " + String.format("%#02x", this.calendarType) +
            ", endType: " + String.format("%#04x", this.endType) + "\n";

        result += "period: " + this.period +
            ", occurenceCount: " + String.format("%#04x", this.occurenceCount) +
            ", patternSpecific1: " + String.format("%#04x", this.patternSpecific1) +
            ", patternSpecific2: " + String.format("%#04x", this.patternSpecific2) + "\n";

        result += "creation time zone: " + this.creationTimeZone.getID() +
            ", startDate: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startDate) +
            ", startTimeOffset: " + startTimeOffset +
            ", endDate: " +  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endDate) +
            ", endTimeOffset: " + endTimeOffset + "\n";

        // Add the modified instance dates in a comma separated list to save space.
        if (modifiedInstanceCount > 0)
        {
            result += "\nmodified Instance dates:\n";
            for (int i = 0; i < modifiedInstanceCount; i++)
            {
                result += String.format("%#04x", this.modifiedInstances[i]) + "\\" +
                      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(
                           OutlookUtils.windowsTimeToDateObject(this.modifiedInstances[i]))  + ", ";
            }
            result += "\n";
        }

        // Add the deleted instance dates in a comma separated list to save space.
        if (deletedInstanceCount > 0)
        {
            result += "\ndeleted Instance dates:\n";
            for (int i = 0; i < deletedInstanceCount; i++)
            {
                result += new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(
                        deletedInstances.get(i))  + ", ";
            }
            result += "\n";
        }

        if (exceptions.size() > 0)
        {
            result += "Exceptions (start\\end\\originalStart):\n";
            for (ExceptionInfo info : exceptions)
            {
                result += info.toString() + ", ";
            }
        }

        return result;
    }

    /**
     * Checks whether the given date is in the recurrent pattern range or not
     * @param date the date
     * @return <tt>true</tt> if the date is in the pattern range.
     */
    private boolean dateOutOfRange(Date date)
    {
        // Return false immediately if we know this type never ends.
        if (endType == 0x2023 || endType == 0xFFFFFFFF)
            return false;

        Calendar cal = Calendar.getInstance(creationTimeZone);
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Return true if the end date is in the past.
        return cal.getTime().after(endDate);
    }

    /**
     * Return the Start and End date for the next meeting in the series defined by this recurring
     * pattern, or null if there are no more instances. By next, we mean the next instance who's
     * End date is in the future - so it could be a meeting that has or hasn't started.
     * @return
     */
    Pair<Date, Date> getNextMeeting()
    {
        // If the series has ended, then just return.
        if (dateOutOfRange(new Date()))
        {
            return null;
        }

        Date oldStartDate = getMeetingStartDate();
        Date oldEndDate   = getMeetingEndDate();

        switch (patternType)
        {
            case Day:
            {
                return nextDay(oldStartDate, oldEndDate);
            }
            case Week:
            {
                return nextWeek(oldStartDate, oldEndDate);
            }
            case Month:
            case MonthEnd:
            case HjMonth:
            case HjMonthEnd:
            {
                return nextMonth(oldStartDate, false);
            }
            case MonthNth:
            case HjMonthNth:
            {
                if (patternSpecific1 == 0x7f && patternSpecific2 == 0x05)
                {
                    return nextMonth(oldStartDate, true);
                }
                else
                {
                    return nextMonthN(oldStartDate);
                }
            }
        }

        return null;
    }

    /**
     * Finds the next occurrence for daily recurrence.
     * @param startDate the start date of the initial meeting
     * @param endDate the end date of the initial meeting
     * @return the start/end dates of the next occurrence
     */
    private Pair<Date, Date> nextDay(Date startDate, Date endDate)
    {
        // The recurrence type is day, therefore the pattern contains
        // the number of minutes until the next event.  E.g. 1440 for
        // an event that happens every day (1440 = 60 * 24)
        Calendar startCalendar = Calendar.getInstance(creationTimeZone);
        startCalendar.setTime(startDate);

        Calendar endCalendar = Calendar.getInstance(creationTimeZone);
        endCalendar.setTime(endDate);

        // Use days rather than minutes to increment the calendar so
        // that daylight saving time is taken into account.
        int periodInDays = period/1440;

        Calendar currentCalendar = Calendar.getInstance(creationTimeZone);

        // Move forward in time until we hit the future
        while (endCalendar.before(currentCalendar))
        {
            startCalendar.add(Calendar.DATE, periodInDays);
            endCalendar.add(Calendar.DATE, periodInDays);
        }

        // Move forward in time until we hit a non-deleted instance
        // Note that the deleted instances field contains days, not
        // dates - i.e. there is no time.
        // Therefore delCalendar only needs to track dates, not time,
        // so we can set the time to midnight GMT.
        int increments = 0;

        Calendar delCalendar = (Calendar) startCalendar.clone();
        delCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        delCalendar.set(Calendar.HOUR_OF_DAY, 0);
        delCalendar.set(Calendar.MINUTE, 0);
        delCalendar.set(Calendar.SECOND, 0);
        delCalendar.set(Calendar.MILLISECOND, 0);

        while (deletedInstances.contains(delCalendar.getTime()))
        {
            increments++;
            delCalendar.add(Calendar.DATE, periodInDays);
        }

        // We've passed the deletions. Update the start and end calendars
        startCalendar.add(Calendar.DATE, periodInDays * increments);
        endCalendar.add(Calendar.DATE, periodInDays * increments);

        return handleExceptions(startCalendar.getTime(),
                                endCalendar.getTime());
    }

    /**
     * Finds the next occurrence for weekly recurrence.
     * @param startDate the start date of the initial meeting
     * @param endDate the end date of the initial meeting
     * @return the start/end dates of the next occurrence
     */
    private Pair<Date, Date> nextWeek(Date startDate, Date endDate)
    {
        long duration = endDate.getTime() - startDate.getTime();

        // Recurrence type is week, therefore the period is a number of
        // weeks.  I.e. 2 means every 2 weeks.
        Calendar cal = Calendar.getInstance(creationTimeZone);

        // The enum for the firstDow field is the same as Calendar day of
        // week enum + 1 day
        cal.setFirstDayOfWeek(firstDow + 1);
        cal.setTime(startDate);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int index = allowedDaysOfWeek.indexOf(dayOfWeek);

        Date currentDate = new Date();

        // Move forward in time until we hit the future
        if (endDate.before(currentDate))
        {
            cal.setFirstDayOfWeek(Calendar.SUNDAY);
            cal.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(0));
            endDate = new Date(cal.getTimeInMillis() + duration);
            long offset = (currentDate.getTime() - endDate.getTime());

            // 1 week = 604800000 is milliseconds
            offset -= offset % ((long)period * ONE_WEEK_IN_MSECS);

            // Offset is now n * period * number of ms in a week, where
            // n is the largest integer such that oldEndDate + offset is
            // still in the past.

            if (endDate.getTime() + offset  < currentDate.getTime())
            {
                cal.add(Calendar.WEEK_OF_YEAR, (int)(offset / ONE_WEEK_IN_MSECS));
                int i = 1;
                while(((cal.getTimeInMillis() + duration)
                    < (currentDate.getTime())))
                {
                    if(i == allowedDaysOfWeek.size())
                    {
                        cal.add(Calendar.WEEK_OF_YEAR, period);
                        i = 0;
                    }
                    cal.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(i));
                    i++;
                }

                startDate = cal.getTime();
            }
            else
            {
                startDate = new Date(cal.getTimeInMillis() + offset);
            }
        }

        // cal only tracks the date, so we set the time to midnight GMT
        // cal2 tracks the actual meeting time, which is used for scheduling.
        cal.setTime(startDate);
        Calendar cal2 = (Calendar) cal.clone();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        index = allowedDaysOfWeek.indexOf(dayOfWeek) + 1;

        // For each deleted instance, move on to the next valid day
        // of the week. If we run out of days, move to the next week of
        // the year.
        while(deletedInstances.contains(cal.getTime()))
        {
            if(index >= allowedDaysOfWeek.size())
            {
                index = 0;
                cal.add(Calendar.WEEK_OF_YEAR, period);
                cal2.add(Calendar.WEEK_OF_YEAR, period);
            }
            cal.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(index));
            cal2.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(index));
            index++;
        }

        return handleExceptions(cal2.getTime(),
                         new Date(cal2.getTime().getTime() + duration));
    }

    /**
     * Finds the occurrence of the events in the next months
     * @param cal the calendar object
     * @param lastDay if <tt>true</tt> it will return the last day of the month
     * @param period the number of months to add
     * @return the calendar object with set date
     */
    private Calendar incrementMonths(Calendar cal,
                                     boolean lastDay,
                                     int period)
    {
        int dayOfMonth = patternSpecific1;
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, period);

        if(lastDay
            || (cal.getActualMaximum(Calendar.DAY_OF_MONTH) < dayOfMonth))
            dayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return cal;
    }

    /**
     * Finds the next occurrence for monthly recurrence.
     * @param startDate the start date of the previous calendar item.
     * @param lastDay if <tt>true</tt> we are interested in last day of the
     * month
     * @return the start/end dates of the next occurrence
     */
    private Pair<Date, Date> nextMonth(Date startDate, boolean lastDay)
    {
        long duration = getMeetingEndDate().getTime()
                                              - getMeetingStartDate().getTime();
        Calendar cal = Calendar.getInstance(creationTimeZone);
        cal.setTime(startDate);
        Date currentDate = new Date();

        // Move forward in time until we hit the future
        if(cal.getTimeInMillis() + duration < currentDate.getTime())
        {
            Calendar cal2 = Calendar.getInstance(creationTimeZone);
            cal2.setTime(currentDate);
            int years
                = cal2.get(Calendar.YEAR) - cal.get(Calendar.YEAR);
            int months = (years * 12)
                + (cal2.get(Calendar.MONTH) - cal.get(Calendar.MONTH));
            int monthsToAdd = months;
            monthsToAdd -= months % period;
            cal = incrementMonths(cal, lastDay, monthsToAdd);

            if(cal.getTimeInMillis() + duration < currentDate.getTime())
            {
                cal = incrementMonths(cal, lastDay, period);
            }
        }

        // cal only tracks the date, so we set it to midnight GMT
        // cal2 tracks the time, so we keep it in the creation timezone so that
        // DST changes are dealt with properly
        Calendar cal2 = (Calendar) cal.clone();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        while(deletedInstances.contains(cal.getTime()))
        {
            cal = incrementMonths(cal, lastDay, period);
            cal2 = incrementMonths(cal2, lastDay, period);
        }

        startDate = cal2.getTime();
        Date endDate = new Date(startDate.getTime() + duration);

        return handleExceptions(startDate, endDate);
    }

    /**
     * Finds the occurrence of the events in the next months
     * @param startDate the start date if the calendar item
     * @param dayOfWeekInMonth the number of week days occurrences
     * @return the start/end dates of the next occurrence
     */
    private Date getMonthNStartDate(Date startDate, int dayOfWeekInMonth)
    {
        Calendar cal = Calendar.getInstance(creationTimeZone);
        cal.setTime(startDate);

        if(dayOfWeekInMonth == -1)
        {
            Date result = null;
            cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
            for(int day : allowedDaysOfWeek)
            {
                cal.set(Calendar.DAY_OF_WEEK, day);
                if(result == null || result.before(cal.getTime()))
                    result = cal.getTime();
            }
            return result;
        }
        else
            while(dayOfWeekInMonth > 0)
            {
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if(allowedDaysOfWeek.contains(dayOfWeek))
                    dayOfWeekInMonth--;
                if(dayOfWeekInMonth > 0)
                    cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        return cal.getTime();
    }

    /**
     * Finds the next occurrence for monthly Nth recurrence.
     * @param startDate the start date of the previous calendar item.
     * @return the start/end dates of the next occurrence
     */
    private Pair<Date, Date> nextMonthN(Date startDate)
    {
        // If patternSpecific2 == 5, we want the last day of the month (i.e.
        // day -1)
        int dayOfWeekInMonth = (patternSpecific2 == 5? -1 : patternSpecific2);
        long duration = getMeetingEndDate().getTime()
                                              - getMeetingStartDate().getTime();
        Calendar cal = Calendar.getInstance(creationTimeZone);
        cal.setTime(startDate);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.setTime(getMonthNStartDate(cal.getTime(), dayOfWeekInMonth));
        Date currentDate = new Date();

        // Add time until we reach the current date
        if(cal.getTimeInMillis() + duration < currentDate.getTime())
        {
            Calendar cal2 = Calendar.getInstance(creationTimeZone);
            cal2.setTime(currentDate);
            int years
                = cal2.get(Calendar.YEAR) - cal.get(Calendar.YEAR);
            int months = (years * 12)
                + (cal2.get(Calendar.MONTH) - cal.get(Calendar.MONTH));
            int monthsToAdd = months;
            monthsToAdd -= months % period;
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, monthsToAdd);
            cal.setTime(getMonthNStartDate(cal.getTime(), dayOfWeekInMonth));
            while (cal.getTimeInMillis() + duration < currentDate.getTime())
            {
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.add(Calendar.MONTH, period);
                cal.setTime(getMonthNStartDate(cal.getTime(), dayOfWeekInMonth));
            }
        }

        // cal2 tracks the start time, so we keep it in the creation timezone
        // so that DST changes are dealt with correctly.
        // cal only tracks the date, so we set the time to midnight GMT.
        Calendar cal2 = (Calendar) cal.clone();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        while(deletedInstances.contains(cal.getTime()))
        {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, period);
            startDate = null;
            for(int dayOfWeek : allowedDaysOfWeek)
            {
                cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                cal.set(Calendar.DAY_OF_WEEK_IN_MONTH,dayOfWeekInMonth);
                if((cal.after(startDate) && dayOfWeekInMonth == -1)
                    || (cal.before(startDate) && dayOfWeekInMonth != -1)
                    || startDate == null)
                {
                    startDate = cal.getTime();
                    cal2.set(Calendar.YEAR,cal.get(Calendar.YEAR));
                    cal2.set(Calendar.MONTH,cal.get(Calendar.MONTH));
                    cal2.set(Calendar.DATE,cal.get(Calendar.DATE));
                }
            }
        }

        startDate = cal2.getTime();
        Date endDate = new Date(startDate.getTime() + duration);

        return handleExceptions(startDate, endDate);
    }

    /**
     * Given the new dates for a meeting, check to see if there is an exception
     * which happens before that should take priority
     *
     * @param startDate The new start date for the meeting
     * @param endDate The new end date for the meeting
     * @return the scheduler that will handle the meeting.
     */
    private Pair<Date, Date> handleExceptions(Date startDate, Date endDate)
    {
        Date currentDate = new Date();
        ExceptionInfo latestException = null;
        Date nextStartDate;
        Date nextEndDate;

        for (ExceptionInfo exception : exceptions)
        {
            // Ignore not busy meetings
            if (exception.getBusyStatus() != BusyStatusEnum.BUSY)
                continue;

            // Ignore exceptions without dates
            if (exception.getStartDate() == null || exception.getEndDate() == null)
                continue;

            // Ignore exceptions that have already finished
            if (exception.getEndDate().before(currentDate))
                continue;

            if (latestException == null)
            {
                // No previous exception - this one is more relevant.
                latestException = exception;
            }
            else if (exception.getEndDate().before(latestException.getEndDate()))
            {
                // This exception happens before the latest one.  So the latest
                // one needs to be updated
                latestException = exception;
            }
        }

        if (latestException == null)
        {
            nextStartDate = startDate;
            nextEndDate = endDate;
        }
        else
        {
            nextStartDate = latestException.getStartDate();
            nextEndDate = latestException.getEndDate();
        }

        // Final check that we haven't passed the end date of the series
        if (dateOutOfRange(nextStartDate))
        {
            return null;
        }
        else
        {
            return Pair.of(nextStartDate, nextEndDate);
        }
    }

    /**
     * Returns the meeting's end date.
     *
     * @return A date object representing the end of the meeting
     */
    private Date getMeetingEndDate()
    {
        // endTimeOffset is only correct for the creation timezone, so we
        // account for this by calculating the offset from the creation
        // timezone to UTC on the date in question.
        Date originalCreationEndTime =
                      new Date(startDate.getTime() + endTimeOffset * 60 * 1000);
        return applyTimeZoneOffset(originalCreationEndTime);
    }

    /**
     * Returns the meetings start date.
     *
     * @return A date object representing the start of the meeting
     */
    private Date getMeetingStartDate()
    {
        // startTimeOffset is only correct for the creation timezone, so we
        // account for this by calculating the offset from the creation
        // timezone to UTC on the date in question.
        Date originalCreationStartTime =
                    new Date(startDate.getTime() + startTimeOffset * 60 * 1000);
        return applyTimeZoneOffset(originalCreationStartTime);
    }

    /**
     * Returns a Date, adjusted by the offset of the creation timezone
     * @param date
     * @return the corrected Date object
     */
    private Date applyTimeZoneOffset(Date date)
    {
        long timeZoneOffset = creationTimeZone.getOffset(date.getTime());
        return new Date(date.getTime() - timeZoneOffset);
    }

    /**
     * Represents the exception info structure.
     */
    public class ExceptionInfo
    {
        /**
         * The start date of the exception.
         */
        private final Date startDate;

        /**
         * The end date of the exception.
         */
        private final Date endDate;

        /**
         * The original start date of the exception.
         */
        private final Date originalStartDate;

        /**
         * The over ride flags indicate what data in the Exception Info structure
         * has a value different to the recurring series.  The valid flags are:
         * 0x001 - indicates subject, subject length and subject legnth2 fields
         *         are present
         * 0x002 - indicates the meeting type field is present
         * 0x004 - indicates that the reminder delta field is present
         * 0x008 - indicates that the reminder set field is present
         * 0x010 - indicates that the location, location length and length 2 fields
         *         are present
         * 0x020 - indicates that the busy status field is present
         * 0x040 - indicates that the attachment field is present
         * 0x080 - indicates that the subtype field is present
         * 0x100 - indicates that the appointment colour is present
         * 0x200 - indicates that the exception embedded message object has the
         *         PidTagRtfCompressed property on it.
         */
        private final short overrideFlags;

        /**
         * The new busy status of the exception.
         */
        private final BusyStatusEnum busyStatus;

        /**
         * The size of the fixed fields.
         */
        private final int size;

        /**
         * Parses the data of the exception.
         * @param offset the position where the exception starts in the binary
         * data
         */
        public ExceptionInfo(int offset)
        {
            int initialOffset = offset;

            // Exception Info Structure has the following structure
            // Byte     0           1           2           3
            //          Start Date Time
            //          End Date Time
            //          Original Start Date
            //          Override Flags          Subject length*
            //          Subject length 2*       Subject (variable)*
            //          ...
            //          Meeting Type*
            //          Reminder Delta*
            //          Reminder Set*
            //          Location Length*        Location Length 2*
            //          Location (variable)*
            //          ...
            //          Busy Status*
            //          Attachment*
            //          Sub type*
            //          Appointment Colour*
            //          ...
            // Fields marked with a * are optional and only present if the
            // override flag indicates that it is.
            // Note that we only actually care about the dates and the busy state
            // however we still need to parse the entire structure so that we can
            // find out how big it is.
            startDate = applyTimeZoneOffset(
                OutlookUtils.windowsTimeToDateObject(dataBuffer.getInt(offset)));
            offset += 4;

            endDate = applyTimeZoneOffset(
                OutlookUtils.windowsTimeToDateObject(dataBuffer.getInt(offset)));
            offset += 4;

            originalStartDate = applyTimeZoneOffset(
                OutlookUtils.windowsTimeToDateObject(dataBuffer.getInt(offset)));
            offset += 4;

            overrideFlags = dataBuffer.getShort(offset);
            offset += 2;

            // See java doc for the override flag field.
            // Note that most of these fields we don't care about.  But we still
            // need to check that flag value since we need to increment the
            // offset to pass the data that we don't care about.
            boolean subjectPresent       = (overrideFlags & 0x001) != 0;
            boolean meetingPresent       = (overrideFlags & 0x002) != 0;
            boolean reminderDeltaPresent = (overrideFlags & 0x004) != 0;
            boolean reminderSetPresent   = (overrideFlags & 0x008) != 0;
            boolean locationPresent      = (overrideFlags & 0x010) != 0;
            boolean busyPresent          = (overrideFlags & 0x020) != 0;
            boolean attachmentPresent    = (overrideFlags & 0x040) != 0;
            boolean subtypePresent       = (overrideFlags & 0x080) != 0;
            boolean appointmentColorPres = (overrideFlags & 0x100) != 0;

            if (subjectPresent)
            {
                // Subject field length is given by the "subject length 2" field
                offset += 2;
                offset += dataBuffer.getShort(offset);
                offset += 2;
            }

            if (meetingPresent)
            {
                // Meeting type has a fixed length
                offset += 4;
            }

            if (reminderDeltaPresent)
            {
                // Reminder delta is fixed length
                offset += 4;
            }

            if (reminderSetPresent)
            {
                // Reminder set is fixed length
                offset += 4;
            }

            if (locationPresent)
            {
                // Variable length which is given by the location length 2 field
                offset += 2;
                offset += dataBuffer.getShort(offset);
                offset += 2;
            }

            if (busyPresent)
            {
                // Fixed size of 4
                busyStatus = BusyStatusEnum.getFromLong(dataBuffer.getInt(offset));
                offset += 4;
            }
            else
            {
                // No override set - use the default.
                busyStatus = defaultBusyStatus;
            }

            if (attachmentPresent)
            {
                // Attachment has fixed size
                offset += 4;
            }

            if (subtypePresent)
            {
                // Subtype has a fixed size
                offset += 4;
            }

            if (appointmentColorPres)
            {
                // Appointment color field has a fixed size
                offset += 4;
            }

            size = offset - initialOffset;
        }

        /**
         * Returns the size of the exception
         * @return the size of the exception
         */
        public int sizeInBytes()
        {
            return size;
        }

        /**
         * Returns the start date
         * @return the start date
         */
        public Date getStartDate()
        {
            return startDate;
        }

        /**
         * Returns the end date
         * @return the end date
         */
        public Date getEndDate()
        {
            return endDate;
        }

        /**
         * Returns the busy status
         * @return the busy status
         */
        public BusyStatusEnum getBusyStatus()
        {
            return busyStatus;
        }

        /**
         * Prints the properties of the class for debugging purpose.
         */
        @Override
        public String toString()
        {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(startDate) + "\\" +
                   new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(endDate) + "\\" +
                   new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(originalStartDate);
        }
    }

    /**
     * Enum for the type of the pattern.
     */
    public enum PatternType
    {
        /**
         * Daily recurrence.
         */
        Day((short)0x0000),

        /**
         * Weekly recurrence.
         */
        Week((short)0x0001),

        /**
         * Monthly recurrence.
         */
        Month((short)0x0002),

        /**
         * Monthly recurrence (every n-th month).
         */
        MonthNth((short)0x0003),

        /**
         * Monthly recurrence (last x of the month).
         */
        MonthEnd((short)0x004),

        /**
         * Monthly recurrence (using Hijri calendar).
         */
        HjMonth((short)0x000A),

        /**
         * Monthly recurrence (every n-th month, Hijri calendar).
         */
        HjMonthNth((short)0x000B),

        /**
         * Monthly recurrence (last x of the month, Hijri calendar).
         */
        HjMonthEnd((short)0x000C);

        /**
         * The value of the type.
         */
        private final short value;

        /**
         * Constructs new <tt>PatternType</tt> instance.
         * @param value the value.
         */
        PatternType(short value)
        {
            this.value = value;
        }

        /**
         * Returns the value of the <tt>PatternType</tt> instance.
         * @return the value
         */
        public short getValue()
        {
            return value;
        }

        /**
         * Finds the <tt>PatternType</tt> by given value.
         * @param value the value
         * @return the found <tt>PatternType</tt> instance or null if no type is
         * found.
         */
        public static PatternType getFromShort(short value)
        {
            for (PatternType type : values())
            {
                if (type.getValue() == value)
                    return type;
            }

            return null;
        }
    }
}
