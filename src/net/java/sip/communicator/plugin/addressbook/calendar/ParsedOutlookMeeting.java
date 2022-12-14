// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook.calendar;

import static net.java.sip.communicator.plugin.addressbook.OutlookUtils.*;

import java.text.*;
import java.util.*;

import org.jitsi.util.StringUtils;

import net.java.sip.communicator.plugin.addressbook.*;
import net.java.sip.communicator.util.*;

/**
 * A Java object representing an Outlook calendar appointment.
 *
 * Note that instances of this class will not be a perfect copy of the
 * corresponding entry in Outlook since the start date and end date of this
 * object will change as time goes on (if this meeting is recurring or not).
 */
class ParsedOutlookMeeting
{
    private static final Logger sLog = Logger.getLogger(ParsedOutlookMeeting.class);

    /**
     * The ID of the meeting object
     */
    private final String mId;

    /**
     * The state of this meeting (BUSY, FREE and so on).
     */
    private final BusyStatusEnum mBusyStatus;

    /**
     * The response status of this meeting (TENTATIVE, NOT_REQUIRED and so on).
     */
    private final ResponseStatus mResponseStatus;

    /**
     * Optional value indicating if this meeting recurs or not.  Null if doesn't
     */
    private final RecurringPattern mRecurrencyPattern;

    /**
     * Human readable version of the recurring pattern.
     */
    private final String mReadablePattern;

    /**
     * The start date of the meeting.
     */
    private final Date mStartDate;

    /**
     * The end date of the meeting.
     */
    private final Date mEndDate;

    /**
     * The id of the parent folder
     */
    private final String mParentId;

    /**
     * True if the user has marked this instance of the meeting as left. Reset
     * to false when the end task is run or if the start time changes to a time
     * in the future.
     *
     * If a meeting is marked as left, receiving Outlook updates for the meeting
     * doesn't cause the user's presence to change back to "In a Meeting".
     */
    private boolean mMarkedAsLeft;

    /**
     * The object responsible for scheduling the start and end meeting tasks for
     * this meeting
     */
    private CalendarItemScheduler mCalendarItemScheduler;

    public ParsedOutlookMeeting(Object[] data, String id) throws IllegalArgumentException
    {
        mId = id;
        mMarkedAsLeft = false;

        try
        {
            // Format of array should be
            //     [
            //          0. start time (secs),
            //          1. end time (secs),
            //          2. Busy Status,
            //          3. recurring or not,
            //          4. recurring pattern (null if not),
            //          5. response status (accepted or not),
            //          6. human readable recurring pattern
            //          7. timezone of the meeting
            //     ]

            // Creation time zone. Important for scheduling recurrent meetings
            String timeZoneString = (String) data[IDX_CALENDAR_TIMEZONE];
            TimeZone creationTimeZone =
                            OutlookUtils.parseMicrosoftTimezone(timeZoneString);

            // Start and end dates are always given in GMT
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));

            // Start and End dates
            mStartDate = format.parse((String) data[IDX_CALENDAR_START_DATE]);
            mEndDate   = format.parse((String) data[IDX_CALENDAR_END_DATE]);

            if (mStartDate.after(mEndDate))
                throw new IllegalArgumentException("Start time after end time!");

            // Busy status
            mBusyStatus = BusyStatusEnum.getFromLong(
                                           (Long)data[IDX_CALENDAR_BUSY_STATE]);

            // Response Status
            mResponseStatus = ResponseStatus.getFromLong(
                                             (Long)data[IDX_CALENDAR_ACCEPTED]);

            // Finally, the Recurrency pattern.  Do it last as we need to pass a
            // reference to this into the constructor.
            String recurrenceString = (String) data[IDX_CALENDAR_REC_PATTERN];
            RecurringPattern parsedRecurrence = null;

            if (recurrenceString != null && !recurrenceString.isEmpty())
            {
                byte[] hex = hexStringToByteArray(recurrenceString);

                try
                {
                    parsedRecurrence = new RecurringPattern(hex,
                                                            this,
                                                            creationTimeZone,
                                                            mBusyStatus);
                }
                catch (IndexOutOfBoundsException e)
                {
                    // Bad data - assume not recurrent:
                    parsedRecurrence = null;
                }
            }

            mRecurrencyPattern = parsedRecurrence;
            mReadablePattern = String.valueOf(data[IDX_CALENDAR_READABLE_PATTERN]);

            // Store the ParentID
            mParentId = data[IDX_CALENDAR_PARENT_ID].toString();
        }
        catch (NullPointerException | ParseException | ClassCastException e)
        {
            // Indicates bad data - in the case of a ParseException, that it is not a meeting
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Constructor only to be used for UTs. Creates a one-hour meeting with the
     * desired start time. Other values are sensible defaults or null.
     * @param startDate the start time to use for the meeting.
     */
    public ParsedOutlookMeeting(Date startDate)
    {
        mId = "testMeetingId";
        mStartDate = startDate;
        // Set end time one hour later than the start.
        mEndDate = new Date(startDate.getTime() + 1000*60*60);
        mBusyStatus = BusyStatusEnum.BUSY;
        mResponseStatus = ResponseStatus.respAccepted;
        mRecurrencyPattern = null;
        mReadablePattern = "Test readable pattern";
        mParentId = null;
        mMarkedAsLeft = false;
    }

    /**
     * @return the date when this meeting starts
     */
    public Date getStartDate()
    {
        return mStartDate;
    }

    /**
     * @return the date when this meeting ends
     */
    public Date getEndDate()
    {
        return mEndDate;
    }

    /**
     * @return the response status
     */
    public ResponseStatus getResponseStatus()
    {
        return mResponseStatus;
    }

    /**
     * @return the busy status
     */
    public BusyStatusEnum getBusyStatus()
    {
        return mBusyStatus;
    }

    /**
     * @return the recurrency pattern (or null if this meeting is not recurring)
     */
    public RecurringPattern getRecurrencyPattern()
    {
        return mRecurrencyPattern;
    }

    /**
     * Update the scheduler that is responsible for scheduling the start and end
     * meeting tasks for this meeting
     *
     * @param calendarItemScheduler the new item scheduler
     */
    public void setItemTask(CalendarItemScheduler calendarItemScheduler)
    {
        mCalendarItemScheduler = calendarItemScheduler;
    }

    /**
     * @return the scheduler that is responsible for scheduling the start and end
     *         meeting tasks for this meeting.
     */
    public CalendarItemScheduler getItemTask()
    {
        return mCalendarItemScheduler;
    }

    /**
     * @return the parentID of this meeting
     */
    public String getParentId()
    {
        return mParentId;
    }

    /**
     * @return the ID of this meeting
     */
    public String getId()
    {
        return mId;
    }

    /**
     * @return whether the user has marked the meeting as left
     */
    public boolean isMarkedAsLeft()
    {
        return mMarkedAsLeft;
    }

    /**
     * Set whether to treat the meeting as "left"
     * @param left whether to treat the meeting as "left"
     */
    public void markAsLeft(boolean left)
    {
        sLog.debug("Meeting " + mId + " marked as left: " + left);
        mMarkedAsLeft = left;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("\n\nParsedOutlookMeeting, busy = " + mBusyStatus +
                                   ", response = " + mResponseStatus +
                                   ", start = " + mStartDate +
                                   ", end = " + mEndDate +
                                   ", marked as left = " + mMarkedAsLeft);
        if (!StringUtils.isNullOrEmpty(mReadablePattern))
        {
            sb.append(",\nreadable recurrence = " + mReadablePattern);
        }
        if (mRecurrencyPattern != null)
        {
            sb.append(",\n\nrecurrency pattern = \n" + mRecurrencyPattern);
        }
        if (mCalendarItemScheduler != null)
        {
            sb.append(",\n\ncalendar item scheduler = " + mCalendarItemScheduler + "; " + mCalendarItemScheduler.getState());
        }
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Convenience method for converting a string of hex into an array of the
     * bytes that that hex string represents
     *
     * @param s The string to convert
     * @return The byte array represented by the hex.
     */
    protected static byte[] hexStringToByteArray(String s)
    {
        // A byte is represented by 2 characters of a string, thus we need to
        // iterate over the chars in the string 2 at a time
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2)
        {
            // Map the first char of the pair to the top 4 bits and the second
            // char to the bottom 4 bits.
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
                                 Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }
}
