// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook.calendar;

import static net.java.sip.communicator.plugin.addressbook.OutlookUtils.*;

import java.io.*;
import java.util.*;

import org.json.simple.*;
import org.osgi.framework.*;

import net.java.sip.communicator.plugin.addressbook.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Class responsible for getting and tracking Outlook Meetings
 */
public class OutlookCalendarDataHandler
    implements StateDumper, OperationSetCalendar
{
    private static final Logger sLog = Logger.getLogger(OutlookCalendarDataHandler.class);

    /**
     * A map of all the meetings that we have been told about.  Map is from the
     * ID of the item to the parsed version of that item.
     */
    private final HashMap<String, ParsedOutlookMeeting> mMeetings =
            new HashMap<>();

    /**
     * Outlook client used to make calendar requests
     */
    private final OutlookRpcClient mClient;

    /**
     * A timer which is used to handle updated or new meetings. This is required
     * since we must not request details of a new meeting on the same thread as
     * that which told us about the new meeting.
     */
    private Timer mUpdateExecutor;
    private ServerBackoff backoff;

    /**
     * The ID of the default contact folder within Outlook
     */
    private String mDefaultCalendarFolder;

    public OutlookCalendarDataHandler(OutlookRpcClient client,
                                      AbstractAddressBookProtocolProviderService provider)
    {
        sLog.info("Creating calendar");
        mClient = client;

        // Add an operation set for calendar
        provider.addSupportedOperationSet(OperationSetCalendar.class, this);
    }

    /**
     * Start the client - query for the calendar entries
     */
    public void start()
    {
        sLog.info("Starting calendar sync");
        mDefaultCalendarFolder = mClient.getDefaultCalendarFolder();
        sLog.info("Got default calendar folder: " + mDefaultCalendarFolder);

        mUpdateExecutor = new Timer("OutlookCalendarDataHandler.updateExecutor");
        backoff = new AccessionOutlookServerBackoff();
        mClient.queryCalendar();

        BundleContext context = AddressBookProtocolActivator.getBundleContext();
        DiagnosticsServiceRegistrar.registerStateDumper(this, context);
        sLog.info("Start complete");
    }

    /**
     * Stop the calendar data handler
     */
    public void stop()
    {
        sLog.info("Stopping the calendar");

        // Call notify() in case we're waiting to talk to AOS in a TimerTask in mUpdateExecutor.
        synchronized (this)
        {
            notify();
        }

        Timer executor = mUpdateExecutor;
        if (executor != null)
        {
            mUpdateExecutor = null;
            executor.cancel();
        }

        DiagnosticsServiceRegistrar.unregisterStateDumper(this);

        // Cancel the existing meetings.  If we restart, we will get them again
        Collection<ParsedOutlookMeeting> meetings;

        synchronized (mMeetings)
        {
            meetings = new ArrayList<>(mMeetings.values());
            mMeetings.clear();
        }

        for (ParsedOutlookMeeting meeting : meetings)
        {
            CalendarItemScheduler task = meeting.getItemTask();
            if (task != null)
                task.cancelTasks();
        }
    }

    /**
     * Called when a new meeting has been inserted
     *
     * @param json the meeting
     */
    public void eventInserted(JSONObject json)
    {
        try
        {
            // Structure should be:
            // {
            //     "result":"success|failure"
            //     "props": [ <params> ]
            // }
            String result = (String) json.get("result");

            if (!"success".equals(result))
                throw new IllegalStateException("Bad result " + result);

            String id = (String) json.get("calendarId");
            String type = (String) json.get("type");

            if (isRelevantItem(id, type))
            {
                addNotification(id, 'i');
            }
        }
        catch (IllegalArgumentException e)
        {
            // Not a lot we can do, just log.
            sLog.error("Bad result from json: " + json, e);
        }
        catch (ClassCastException e)
        {
            // Not a lot we can do, just log.
            sLog.error("Unable to cast json: " + json, e);
        }
    }

    /**
     * This ID may be something that this data handler is interested in. Returns
     * true if it is.
     *
     * @param id The ID to check
     * @param type The type of object that the ID represents
     * @return True if it is a relevant ID
     */
    public boolean isRelevantItem(String id, String type)
    {
        // Either we've seen the ID before, or the type will indicate that
        // this is an ID that we care about.  The type indicates what sort of
        // object the ID is for.  It's of the form
        //      "IPM.<something>.<some other things>"
        // Therefore check to see if it contains "Appointment".
        // Note that we can't just check the type, as deleted items have type of
        // "unknown"
        return mMeetings.containsKey(id) ||
               (type != null && type.contains("Appointment"));
    }

    /**
     * Handle a new meeting, scheduling the tasks for it if appropriate
     *
     * @param id The id of the new meeting
     * @param meeting the new meeting
     */
    private void handleNewMeeting(String id, ParsedOutlookMeeting meeting)
    {
        // Nothing to do if this is a declined meeting
        ResponseStatus responseStatus = meeting.getResponseStatus();
        if (responseStatus != ResponseStatus.respNone &&
            responseStatus != ResponseStatus.respAccepted &&
            responseStatus != ResponseStatus.respOrganized)
        {
            return;
        }

        // Nothing to do if we are free during the meeting, or if there is
        // no start / end time
        BusyStatusEnum status = meeting.getBusyStatus();
        Date startDate = meeting.getStartDate();
        Date endDate = meeting.getEndDate();

        if (status != BusyStatusEnum.BUSY || startDate == null || endDate == null)
        {
            return;
        }

        Date currentDate = new Date();

        // Nothing to do if it isn't recurring and has already happened
        RecurringPattern pattern = meeting.getRecurrencyPattern();
        if (endDate.before(currentDate) && pattern == null)
        {
            return;
        }

        // This meeting is busy and either recurs, or hasn't yet happened. So
        // we need to create a scheduler for it to enter and exit the meeting
        // state
        CalendarItemScheduler task = pattern == null ?
                            new CalendarItemScheduler(meeting) : pattern.next();

        // Task could be null if this is a recurrent meeting that has completed
        if (task != null)
        {
            task.scheduleTasks();

            // We should only be storing meetings that are still relevant.
            // Otherwise the list of meetings can become unmanageable.
            synchronized (mMeetings)
            {
                mMeetings.put(id, meeting);
            }
        }
    }

    /**
     * Called when an Outlook entry has been modified
     *
     * @param id The ID of the entry
     * @param operation The operation that has been performed on the meeting
     */
    public void addNotification(final String id,
                                final char operation)
    {
        // This is a notification when the calendar handler hasn't yet started.
        // Ignore it. When the handler does start, then it will request all the
        // data that it needs, including this one.
        if (mUpdateExecutor == null)
            return;

        // Run on a new thread so that the RPC server can respond to the request
        // as soon as possible.
        mUpdateExecutor.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                // This method can be called often, thus use a string builder to
                // build a single log message to reduce log spam.
                StringBuilder msg = new StringBuilder("Notification for item: " + id + ", " + operation);

                try
                {
                    synchronized (this)
                    {
                        if (backoff.shouldWait())
                        {
                            // Backoff because there has been some previous problem communicating with AOS.
                            long backOffTime = backoff.getBackOffTime();
                            sLog.debug("Waiting as AOS problems: " + backOffTime + "ms");

                            // Check for the window where the backoff is reset by another thread between shouldWait() returning
                            // true and calling getBackOffTime().
                            if (backOffTime > 0)
                            {
                                wait(backOffTime);
                            }
                        }
                    }

                    doRun(msg);
                    backoff.onSuccess();
                }
                catch (IOException e)
                {
                    sLog.info("Error communicating to AOS", e);
                    msg.append("Error communicating to AOS:" + e);
                    backoff.onError();
                }
                catch (InterruptedException e)
                {
                    // If we get interrupted, just drop the task.
                    sLog.info("Interrupted while waiting", e);
                    msg.append("Interrupted while waiting:" + e);
                }
                sLog.debug(msg.toString());
            }

            private void doRun(StringBuilder msg) throws IOException
            {
                ParsedOutlookMeeting oldMeeting;

                // Get and remove any old meeting associated with this ID.
                synchronized (mMeetings)
                {
                    oldMeeting = mMeetings.remove(id);
                }

                // Try to create a new meeting from the ID we've been given.
                ParsedOutlookMeeting newMeeting = createMeeting(id);

                if (newMeeting == null && oldMeeting == null)
                {
                    // This is not a meeting - nothing to do here
                    msg.append(" - is not a valid meeting");
                    return;
                }
                else if (newMeeting != null &&
                         !newMeeting.getParentId().equals(mDefaultCalendarFolder))
                {
                    // This meeting is not from the default calendar folder.
                    // This can happen if a user views other people's shared calendars.
                    // We only want to set presence based on the local user's calendar
                    // so drop this meeting.
                    msg.append(" - ignored because parent folder " +  newMeeting.getParentId() +
                               " does not match default folder " + mDefaultCalendarFolder +
                               ". Parsed meeting: " + newMeeting.toString());
                    return;
                }

                if (oldMeeting == null && (operation == 'd' || operation == 'u'))
                {
                    // We've been told that a meeting that we know nothing about
                    // has changed.  It could be that we know about this meeting
                    // under another ID - look to see if this is the case by
                    // comparing the id with the IDs we know already about.
                    List<String> otherIds = new ArrayList<>(mMeetings.keySet());
                    int matchingIdx = mClient.compareIds(id, otherIds);

                    if (matchingIdx != -1)
                    {
                        // The ID we've been passed is for a meeting that we
                        // already know about.  Use it.
                        String newId = otherIds.get(matchingIdx);

                        synchronized (mMeetings)
                        {
                            oldMeeting = mMeetings.remove(newId);
                        }
                    }
                }

                if (oldMeeting != null)
                {
                    msg.append(" - has been updated");

                    // If the old meeting was marked as left the new meeting
                    // should also be marked as left.
                    if ((newMeeting != null) && oldMeeting.isMarkedAsLeft())
                    {
                        newMeeting.markAsLeft(true);
                    }

                    // Cancel the tasks associated with this meeting as we need
                    // to update it
                    CalendarItemScheduler task = oldMeeting.getItemTask();
                    if (task != null)
                        task.cancelTasks();
                }

                // Only handle the meeting if we've got one to handle, and if
                // this isn't a delete.
                if (operation != 'd' && newMeeting != null)
                {
                    msg.append(" - created meeting" + newMeeting);
                    handleNewMeeting(id, newMeeting);
                }
            }
        }, 0);
    }

    /**
     * Look up the properties for a particular ID and try to create a meeting
     * for the resultant properties
     *
     * @param id The id of the potential meeting
     * @return The created meeting or null if this is not a meeting
     * @throws IOException
     */
    private ParsedOutlookMeeting createMeeting(String id) throws IOException
    {
        // Look in the APPOINTMENT folder to find the details for this meeting.
        ParsedOutlookMeeting meeting =
                             createMeeting(id, FOLDER_TYPE_APPOINTMENT);

        return meeting;
    }

    /**
     * Looks up the properties for a particular ID and try to create a meeting
     * with the resultant properties
     *
     * @param id The ID to get the properties
     * @param folderType The folder type to get the properties from
     * @return The created meeting or null if none was created.
     * @throws IOException
     */
    private ParsedOutlookMeeting createMeeting(String id, int folderType) throws IOException
    {
        try
        {
            Object[] props = mClient.IMAPIProp_GetProps(id,
                                                        CALENDAR_FIELDS,
                                                        MAPI_UNICODE,
                                                        folderType);
            return new ParsedOutlookMeeting(props, id);
        }
        catch (IllegalArgumentException e)
        {
            // Indicates this is not an outlook meeting - ignore
        }

        return null;
    }

    @Override
    public String getStateDumpName()
    {
        return "CalendarDataHandler";
    }

    @Override
    public String getState()
    {
        StringBuilder sb = new StringBuilder();
        HashMap<String, ParsedOutlookMeeting> meetings;

        sb.append("Timer: ")
          .append(mUpdateExecutor)
          .append("\n")
          .append("Nb Meetings in: ")
          .append(CalendarItemScheduler.getNbMeetingsInProgress())
          .append("\n")
          .append("Meeting ids: ")
          .append(CalendarItemScheduler.getIdsMeetingsInProgress())
          .append("\n")
          .append("Unrecognised Outlook timezone strings: ");

        for (String failedTimeZone : CalendarItemScheduler.getFailedTimeZoneStrings())
        {
            sb.append(failedTimeZone).append("\n");
        }

        sb.append("\n");

        synchronized (mMeetings)
        {
            meetings = new HashMap<>(mMeetings);
        }

        for (Map.Entry<String, ParsedOutlookMeeting> entry : meetings.entrySet())
        {
            sb.append("\n============================Meeting====================")
              .append("========================\n\n")
              .append(entry.getKey())
              .append(" : ")
              .append(entry.getValue())
              .append("\n\n");
        }

        return sb.toString();
    }

    //-------------------------------------------------------------------------
    // METHODS FOR OperationSetCalendar:
    //-------------------------------------------------------------------------

    /**
     * Mark the current meetings as left so that if we receive further updates
     * from Outlook we don't change presence back to "In a Meeting".
     */
    public void markMeetingsAsLeft()
    {
        Collection<String> currentMeetingIds =
                               CalendarItemScheduler.getIdsMeetingsInProgress();
        synchronized(mMeetings)
        {
            for (String id : currentMeetingIds)
            {
                ParsedOutlookMeeting meeting = mMeetings.get(id);
                if (meeting != null)
                {
                    meeting.markAsLeft(true);
                }
            }
        }
    }
}
