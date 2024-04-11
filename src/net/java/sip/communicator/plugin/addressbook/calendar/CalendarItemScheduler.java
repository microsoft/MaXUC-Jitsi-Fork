/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook.calendar;

import java.util.Date;

import org.apache.commons.lang3.tuple.Pair;

import net.java.sip.communicator.plugin.addressbook.AddressBookProtocolActivator;
import net.java.sip.communicator.service.threading.CancellableRunnable;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.ClockUtils;
import net.java.sip.communicator.util.Logger;

/**
 * A class that represents a calendar item. It schedules tasks for the
 * beginning and for the end of the calendar item to update the in a meeting
 * status
 */
public class CalendarItemScheduler
{
    private static final Logger outlookCalendarLogger = Logger.getLogger("jitsi.OutlookCalendarLogger");
    private static final ThreadingService threadingService = AddressBookProtocolActivator.getThreadingService();

    /**
     * We don't bother to schedule meetings under ten seconds in length.
     * It's unnecessary, and risks race conditions where we try to end the
     * meeting before it has started.
     */
    private static final int MIN_MEETING_LENGTH_MILLIS = 10000;

    /**
     * Schedules the start and end tasks of the calendar item.
     */
    static void scheduleTasks(ParsedOutlookMeeting meeting)
    {
        scheduleTasks(meeting.getId(), meeting.getRecurringPattern(), meeting.getStartDate(), meeting.getEndDate());
    }

    static void scheduleTasks(String meetingID,
                              RecurringPattern recurringPattern,
                              Date startDate,
                              Date endDate)
    {
        outlookCalendarLogger.info("Scheduling " + ((recurringPattern != null) ? "recurring " : "") +
                  " meeting " + meetingID + " start at " + startDate + " end at " + endDate);

        Date now = ClockUtils.getDateNow();

        if (startDate.after(endDate))
        {
            // Should never happen.  Log if it does
            outlookCalendarLogger.error("Start time after end time! " + meetingID);
        }
        else if (endDate.before(ClockUtils.getDateNow()))
        {
            // Should never happen.  Log if it does
            outlookCalendarLogger.error("End time before now! " + meetingID);
        }
        else if (endDate.getTime() - startDate.getTime() < MIN_MEETING_LENGTH_MILLIS)
        {
            outlookCalendarLogger.warn("Skipping meeting " + meetingID + " as it is too short " +
                      "(" + (endDate.getTime() - startDate.getTime()) + " ms)");
        }
        else
        {
            threadingService.schedule("Start of Outlook meeting", new EvaluateMeetingPresenceTask(meetingID, null), startDate.getTime() - now.getTime());
            threadingService.schedule("End of Outlook meeting", new EvaluateMeetingPresenceTask(meetingID, recurringPattern), endDate.getTime() - now.getTime());
        }
    }

    private static class EvaluateMeetingPresenceTask extends CancellableRunnable
    {
        private final String meetingID;
        private final RecurringPattern recurringPattern;

        EvaluateMeetingPresenceTask(String meetingID,
                                    RecurringPattern recurringPattern)
        {
            this.meetingID = meetingID;
            this.recurringPattern = recurringPattern;
        }

        @Override
        public void run()
        {
            OutlookCalendarDataHandler.evaluateMeetingPresence();

            if (recurringPattern != null)
            {
                outlookCalendarLogger.info("Create task to schedule next meeting in recurring pattern");

                // Schedule the next instance after a slight delay.  Otherwise
                // there is a risk that the dates of the next instance will be
                // for the instance that has just completed.
                CancellableRunnable nextMeetingTask = new CancellableRunnable()
                {
                    public void run()
                    {
                        // We don't need to take the taskRunningLock here, because it involves
                        // processing on a new CalendarItemScheduler, which is clearly
                        // thread-safe with any processing done on this one.
                        Pair<Date, Date> nextMeeting = recurringPattern.getNextMeeting();

                        if (nextMeeting != null)
                        {
                            scheduleTasks(meetingID, recurringPattern, nextMeeting.getLeft(), nextMeeting.getRight());
                        }
                    }
                };

                threadingService.schedule("Schedule next meeting in recurring pattern for " + meetingID, nextMeetingTask, 1000);
            }
        }
    }
}
