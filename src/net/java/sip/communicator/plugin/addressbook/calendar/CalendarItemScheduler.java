/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addressbook.calendar;

import java.util.*;
import java.util.concurrent.atomic.*;

import net.java.sip.communicator.plugin.addressbook.*;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

/**
 * A class that represents a calendar item. It schedules tasks for the
 * beginning and for the end of the calendar item to update the in a meeting
 * status
 */
public class CalendarItemScheduler
{
    private static final Logger sLog = Logger.getLogger(CalendarItemScheduler.class);

    /**
     * The <tt>Timer</tt> instance that schedules the tasks.
     */
    private static final Timer sTimer = new Timer("Outlook Meeting Scheduler");

    /**
     * A count of the number of meetings we are in
     */
    private static final AtomicInteger sMeetingCount = new AtomicInteger(0);

    /**
     * A set containing the meetings that we are in - used only for diagnostic
     * purposes
     */
    private static final HashSet<String> sMeetingIds = new HashSet<>();

    /**
     * A set containing any time zone strings received from Outlook that we were
     * unable to parse. Used for diags.
     */
    private static HashSet<String> sFailedTimeZoneStrings = new HashSet<>();

    /**
     * We don't bother to schedule meetings under ten seconds in length.
     * It's unnecessary, and risks race conditions where we try to end the
     * meeting before it has started.
     */
    private static final int MIN_MEETING_LENGTH_MILLIS = 10000;

    /**
     * The meeting that this scheduler is scheduling tasks for
     */
    private final ParsedOutlookMeeting meeting;

    /**
     * The ID of the meeting that this scheduler is scheduling tasks for
     */
    private final String meetingId;

    /**
     * The status of the calendar item.
     */
    private final BusyStatusEnum state;

    /**
     * The start date of the calendar item.
     */
    private final Date startDate;

    /**
     * The end date of the calendar item.
     */
    private final Date endDate;

    /**
     * The <tt>RecurringPattern</tt> instance associated with the calendar item.
     * This must be <tt>null</tt> if the calendar item is not recurring.
     */
    private RecurringPattern pattern;

    /**
     * True if the start task has run
     */
    private boolean startTaskRun = false;

    /**
     * True if the end task has run
     */
    private boolean endTaskRun = false;

    /**
     * The task that will be executed at the beginning of the task.
     */
    private TimerTask startTask = new TimerTask()
    {
        @Override
        public void run()
        {
            synchronized (sMeetingIds)
            {
                sMeetingIds.add(meetingId);
            }

            int nbMeetings = sMeetingCount.incrementAndGet();
            GlobalStatusService statusService = getGlobalStatusService();
            sLog.debug("Start meeting task running " + this +
                                                  " nb meetings " + nbMeetings);

            // Only update presence if the meeting is not marked as left.
            if ((nbMeetings == 1 || !statusService.isInMeeting()) &&
                                                      !meeting.isMarkedAsLeft())
            {
                sLog.info("Updating to be in a meeting");
                statusService.setInMeeting(true);
            }

            startTaskRun = true;
        }

        public String toString()
        {
            return CalendarItemScheduler.class.getName() + '@' +
                Integer.toHexString(hashCode()) + " : id " + meetingId;
        }
    };

    /**
     * The task that will be executed at the end of the task.
     */
    private TimerTask endTask = new TimerTask()
    {
        @Override
        public void run()
        {
            synchronized (sMeetingIds)
            {
                sMeetingIds.remove(meetingId);
            }

            int nbRemainingMeetings = sMeetingCount.decrementAndGet();
            sLog.debug("End meeting task running " + this +
                                       ", nb remaining " + nbRemainingMeetings);

            if (nbRemainingMeetings == 0)
            {
                sLog.info("Updating to leave the meeting");
                getGlobalStatusService().setInMeeting(false);
            }
            else if (nbRemainingMeetings < 0)
            {
                sLog.error("Meeting end event when not in meeting. Now in " +
                           nbRemainingMeetings + " meetings");
            }

            // If the meeting was marked as left, reset that here so that
            // recurring meetings will have correct presence.
            if (meeting.isMarkedAsLeft())
            {
                meeting.markAsLeft(false);
            }

            // If we have a pattern, then this meeting recurs.  Thus create a
            // new scheduler to schedule the tasks for the next instance.
            if (pattern != null)
            {
                // Schedule the next instance after a slight delay.  Otherwise
                // there is a risk that the dates of the next instance will be
                // for the instance that has just completed.
                TimerTask nextMeetingTask = new TimerTask()
                {
                    public void run()
                    {
                        CalendarItemScheduler nextTask = pattern.next();
                        pattern = null;

                        if (nextTask != null)
                            nextTask.scheduleTasks();
                    }
                };

                sTimer.schedule(nextMeetingTask, 1000);
            }

            endTaskRun = true;
        }

        public String toString()
        {
            return CalendarItemScheduler.class.getName() + '@' +
                Integer.toHexString(hashCode()) + " : id " + meetingId;
        }
    };

    /**
     * Constructs new <tt>CalendarItemScheduler</tt> instance.
     *
     * @param meeting the meeting that we are scheduling
     */
    public CalendarItemScheduler(ParsedOutlookMeeting meeting)
    {
        this(meeting, meeting.getStartDate(), meeting.getEndDate());
    }

    /**
     * Constructs new <tt>CalendarItemScheduler</tt> instance for a particular
     * start and end date
     *
     * @param meeting the meeting that we are scheduling
     * @param startDate when the meeting starts
     * @param endDate when the meeting finishes
     */
    public CalendarItemScheduler(ParsedOutlookMeeting meeting,
                                 Date startDate,
                                 Date endDate)
    {
        sLog.debug("Creating a new meeting " + this);
        this.meeting   = meeting;
        this.startDate = startDate;
        this.endDate   = endDate;
        this.state     = meeting == null ? null : meeting.getBusyStatus();
        this.pattern   = meeting == null ? null : meeting.getRecurrencyPattern();
        this.meetingId = meeting == null ? null : meeting.getId();

        if (this.startDate.after(this.endDate))
        {
            sLog.error("Start date after end date! " + this.startDate + " vs " + this.endDate);
        }

        // Store this scheduler in the meeting behind it.  This means that we
        // can cancel these tasks when / if the meeting is edited.
        if (meeting != null)
            meeting.setItemTask(this);
    }

    /**
     * Utility method to get the global status service.
     *
     * Package-private for UT overriding purposes.
     * @return the global status service
     */
    GlobalStatusService getGlobalStatusService()
    {
        return AddressBookProtocolActivator.getGlobalStatusService();
    }

    /**
     * Returns the <tt>RecurringPattern</tt> instance associated with the
     * calendar item.
     * @return the <tt>RecurringPattern</tt> instance associated with the
     * calendar item.
     */
    public RecurringPattern getPattern()
    {
        return pattern;
    }

    /**
     * Schedules the start and end tasks of the calendar item.
     */
    public void scheduleTasks()
    {
        Date startDate = getStartDate();
        sLog.info("Scheduling this " + this +
                  " start at " + startDate +
                  " end at " + endDate +
                  " is marked as left " + meeting.isMarkedAsLeft());

        // If the start date is in the future mark the meeting as not left, as
        // the user might attend the meeting at the new time.
        if (startDate.after(new Date()) && meeting.isMarkedAsLeft())
        {
            sLog.debug("Un-mark the meeting as left because new start time " +
                                                           "is in the future.");
            meeting.markAsLeft(false);
        }

        if (startDate.after(endDate))
        {
            // Should never happen.  Log if it does
            sLog.error("Start time after end time! " + this);
        }
        else if (endDate.before(new Date()))
        {
            // Should never happen.  Log if it does
            sLog.error("End time before now! " + this);
        }
        else if (endDate.getTime() - startDate.getTime() < MIN_MEETING_LENGTH_MILLIS)
        {
            sLog.warn("Skipping calendar item " + this + " as it is too short " +
                      "(" + (endDate.getTime() - startDate.getTime()) + " ms)");
        }
        else
        {
            sTimer.schedule(startTask, startDate);
            sTimer.schedule(endTask, endDate);
        }
    }

   /**
    * Removes the task.
    */
    public void cancelTasks()
    {
        sLog.info("Cancel tasks for this meeting " + this);
        // Run on the timer so that we avoid any threading issues
        sTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if (startTaskRun && !endTaskRun)
                {
                    // We've started the meeting but not finished it - therefore
                    // we must be in a meeting
                    int nbRemainingMeetings = sMeetingCount.decrementAndGet();
                    sLog.info("Decreased number meetings to " +
                                             nbRemainingMeetings + ", " + this);

                    if (nbRemainingMeetings == 0)
                    {
                        getGlobalStatusService().setInMeeting(false);
                    }

                    synchronized (sMeetingIds)
                    {
                        sMeetingIds.remove(meetingId);
                    }
                }
                else if (endTaskRun && !startTaskRun)
                {
                    // This should never happen.  But it has been seen in the
                    // field thus cope with it here.
                    int nbRemainingMeetings = sMeetingCount.incrementAndGet();
                    sLog.error("Increased meeting count as end run but not start"
                                           + nbRemainingMeetings + ", " + this);
                }

                startTask.cancel();
                endTask.cancel();
            }
        }, 0);
    }

    /**
     * Returns the free busy status of the calendar item.
     * @return the free busy status of the calendar item.
     */
    public BusyStatusEnum getStatus()
    {
        return state;
    }

    /**
     * Returns the start date of the calendar item
     * @return the start date of the calendar item
     */
    public Date getStartDate()
    {
        return startDate;
    }

    /**
     * Returns the end date of the calendar item
     * @return the end date of the calendar item
     */
    public Date getEndDate()
    {
        return endDate;
    }

    /**
     * Sets the <tt>RecurringPattern</tt> associated with the calendar item.
     * @param pattern the pattern to set
     */
    public void setPattern(RecurringPattern pattern)
    {
        this.pattern = pattern;
    }

    /**
     * @return a count of the number of meetings that we are in.
     */
    public static int getNbMeetingsInProgress()
    {
        return sMeetingCount.get();
    }

    /**
     * @return a collection of the Ids of the meetings that we are in.
     */
    public static Collection<String> getIdsMeetingsInProgress()
    {
        synchronized (sMeetingIds)
        {
            return new HashSet<>(sMeetingIds);
        }
    }

    /**
     * @return a set of the time zone strings we've been unable to parse
     */
    public static HashSet<String> getFailedTimeZoneStrings()
    {
        synchronized (sFailedTimeZoneStrings)
        {
            return new HashSet<>(sFailedTimeZoneStrings);
        }
    }

    /**
     * Add the given string to the set of failed time zone strings, if it's not
     * already in it.
     * If we've been unable to parse a string, it's probably because there's no
     * matching entry in the ZONEMAPPINGS list in TimeZoneList. If there is a
     * matching entry, the problem could be that the JRE doesn't recognise the
     * matching time zone ID.
     *
     * @param unknownTz the unrecognised string
     * @param usedTz the TimeZone ID actually used
     */
    public static void addFailedTimeZoneString(String unknownTz, String usedTz)
    {
        synchronized (sFailedTimeZoneStrings)
        {
            if (sFailedTimeZoneStrings.add(unknownTz))
            {
                // Only log if we haven't seen this string before
                sLog.warn("Unrecognised timezone string " + unknownTz +
                                              ". Using timezone ID: " + usedTz);

                // Send an analytic
                AddressBookProtocolActivator
                        .getAnalyticsService()
                        .onEvent(AnalyticsEventType.UNRECOGNIZED_OUTLOOK_TIME_ZONE,
                                 AnalyticsParameter.NAME_TIME_ZONE_STRING,
                                 unknownTz);
            }
        }
    }
}
