// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.callhistory.network;

import static org.jitsi.util.Hasher.logHasher;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import net.java.sip.communicator.impl.callhistory.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.phonenumberutils.PhoneNumberUtilsService;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService.GlobalStatusChangeListener;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.ServiceUtils.ServiceCallback;
import org.apache.commons.lang3.time.DateUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.json.simple.*;
import org.json.simple.parser.*;

/**
 * A class for requesting and parsing the network call history from CommPortal.
 */
public class NetworkCallHistoryDataHandler implements CPDataGetterCallback, CPCosGetterCallback
{
    private static Logger sLog = Logger.getLogger(NetworkCallHistoryDataHandler.class);

    /**
     * The place in config where we store the time at which we last got data
     * from the server
     */
    private static final String LAST_SERVER_UPDATE_TIME =
                                 "impl.callhistory.network.NETWORK_UPDATE_TIME";

    /**
     * The place in config where we store the time of the last server record
     * that we've seen.
     */
    private static final String LAST_SERVER_RECORD_TIME =
                                         "impl.callhistory.network.LAST_SERVER";

    /**
     * The place in config where the call list refresh period is stored (ms)
     */
    private static final String CALL_LIST_REFRESH_RATE =
                                        "impl.callhistory.network.REFRESH_RATE";

    /**
     * The SI name that combines EAS and CFS call lists. Should not be used if
     * the subscriber has BCM.
     */
    private static final String SI_NAME_COMBINED = "CallList";

    /**
     * The SI name for CFS call lists only.
     */
    private static final String SI_NAME_CFS = "Meta_Subscriber_CallLists";

    /**
     * The default refresh rate of the call list - 30 minutes.
     */
    private static final long CALL_LIST_DEFAULT_REFRESH = 30 * 60 * 1000;

    /**
     * The object for formatting dates as returned by CommPortal. Date is
     * returned in the format: "29 Sep 09 11:57:07"
     */
    private static final SimpleDateFormat CALL_DATE_FORMAT =
                     new SimpleDateFormat("dd MMM yy HH:mm:ss", Locale.ENGLISH);

    // The services that we use
    private final NetworkAddressManagerService mNetworkService;
    private final PhoneNumberUtilsService mNumberUtilsService;
    private final CallHistoryServiceImpl mCallHistoryService;
    private final ConfigurationService mConfigService;
    private final CommPortalService mCommPortalService;
    private ProtocolProviderService mMwiProvider;
    private GlobalStatusService mGlobalStatusService;
    private NotificationService mNotificationService;

    /**
     * Listener for MWI changes
     */
    private final MessageWaitingListener mMwiListener = new MessageWaitingListener()
    {
        private int mNumberMessages = 0;

        @Override
        public void messageWaitingNotify(MessageWaitingEvent evt)
        {
            sLog.debug("Message Waiting event received " + evt);
            int newNumberMessages = evt.getUnreadMessages();

            if (newNumberMessages > mNumberMessages)
            {
                // If the number of messages has increased, then someone has left a
                // new message and we've probably missed a call - get the history.
                loadNetworkCallHistory();
            }

            mNumberMessages = newNumberMessages;
        }
    };

    /**
     * Listener for changes to the network
     */
    private final NetworkConfigurationChangeListener mNetworkListener =
                                        new NetworkConfigurationChangeListener()
    {
        @Override
        public void configurationChanged(ChangeEvent event)
        {
            if (event.getType() == ChangeEvent.ADDRESS_UP)
            {
                sLog.debug("Network listener says network has changed");
                loadNetworkCallHistory();
            }
        }
    };

    /**
     * Listener for changes to the global status service
     */
    private final GlobalStatusChangeListener mStatusChangeListener =
                                                new GlobalStatusChangeListener()
    {
        @Override
        public void onStatusChanged()
        {
            GlobalStatusEnum globalStatus =
                                         mGlobalStatusService.getGlobalStatus();

            sLog.debug("Status changed, is now " + globalStatus);
            boolean isOnThePhone =
                             globalStatus.equals(GlobalStatusEnum.ON_THE_PHONE);

            if (!isOnThePhone && mIsOnThePhone)
            {
                // Not on the phone thus refresh the list.  Note that this event
                // is fired as soon as we end the call here. It may take a while
                // for the various servers to update the network call history so
                // wait before requesting it:
                sLog.debug("No longer on the phone");
                loadNetworkCallHistory(1000);
            }

            mIsOnThePhone = isOnThePhone;
        }
    };

    /**
     * The set of all call records that are
     * 1. unresolved (i.e. no matching server record)
     * 2. stored locally
     * 3. for missed calls
     *
     * Required as we need to try and resolve them across multiple server
     * refreshes and otherwise we would only consider once.
     */
    private final Set<CallRecord> mUnresolvedLocalMissedCalls =
            new HashSet<>();

    /**
     * Timer used to refresh the call history
     */
    private Timer mTimer =  new Timer("Network Call History timer");

    /**
     * True if the subscriber is allowed to use network call history
     */
    private boolean mNchAllowed = false;

    /**
     * True if there is an outstanding request to get the network call history
     * Make this volatile as when we set mGettingCallHistory = true, we
     * do not want there to be a window where any other threads believe that
     * they are still allowed to get network call history.
     * NOTE: As such, any method setting mGettingCallHistory = true *must*
     * be synchronized, so that we do not race against the check in
     * loadNetworkCallHistory(int) that prevents two requests being issued at
     * once.
     */
    private volatile boolean mGettingCallHistory = false;

    /**
     * True if the user is on the phone.
     */
    private boolean mIsOnThePhone = false;

    /**
     * The service indication to use when requesting the data
     */
    private String mSiName = "";

    /**
     * The date of the last time we got data from CommPortal.
     */
    protected Date mLastRefreshDate;

    /**
     * The date of the latest call history entry that we got from CommPortal.
     * Note that this is required in addition to the last refresh date since the
     * first is a time provided by the client, and the second is a time provided
     * by the server and these may not agree.
     */
    protected Date mLastCommPortalRecordDate;

    /**
     * True if this is the first time we have run the client with network call
     * history
     */
    protected boolean mFirstRunTime;

    /**
     * The name of the thread which handles updating call history entries
     * when we receive new data from the server.
     */
    protected static final String CALL_HISTORY_UPDATE_THREAD =
                                                    "Call History Update thread";

    /**
     * Constructor.  Note that this only creates the data handler, it does not
     * start it.
     *
     * @param callHistoryService the call history service.
     */
    public NetworkCallHistoryDataHandler(CallHistoryServiceImpl callHistoryService,
                                         NetworkAddressManagerService networkService,
                                         PhoneNumberUtilsService numberUtilsService,
                                         ConfigurationService configService,
                                         CommPortalService commPortalService,
                                         ProtocolProviderService mwiProvider)
    {
        sLog.info("Creating data handler");

        mCallHistoryService = callHistoryService;
        mNetworkService     = networkService;
        mNumberUtilsService = numberUtilsService;
        mConfigService      = configService;
        mCommPortalService  = commPortalService;
        mMwiProvider        = mwiProvider;

        getDeferredServices();
        refreshDates();
    }

    /**
     * Gets the services which are not registered when the call history starts.
     */
    protected void getDeferredServices()
    {
        // The global service is not registered when call history starts.  Cope
        // with that happening here:
        ServiceUtils.getService(CallHistoryActivator.bundleContext,
                                GlobalStatusService.class,
                                new ServiceCallback<GlobalStatusService>()
        {
            @Override
            public void onServiceRegistered(GlobalStatusService service)
            {
                sLog.debug("GlobalStatusService has been reigstered");
                mGlobalStatusService = service;

                if (mNchAllowed)
                    mGlobalStatusService.addStatusChangeListener(
                                                         mStatusChangeListener);
            }
        });

        // Similarly for the notification service:
        ServiceUtils.getService(CallHistoryActivator.bundleContext,
                                NotificationService.class,
                                new ServiceCallback<NotificationService>()
        {
            @Override
            public void onServiceRegistered(NotificationService service)
            {
                mNotificationService = service;
            }
        });
    }

    /**
     * Updates the dates of the last call history entry that we received from
     * CommPortal as provided by the server and the client.
     */
    protected void refreshDates()
    {
        long lastServerDate = mConfigService.user().getLong(LAST_SERVER_RECORD_TIME, 0);
        mLastCommPortalRecordDate = new Date(lastServerDate);
        mLastRefreshDate =
            new Date(mConfigService.user().getLong(LAST_SERVER_UPDATE_TIME, 0));

        // If we don't have any saved config, then this must be the first time
        // we have run.
        mFirstRunTime = (lastServerDate == 0);
    }

    /**
     * Starts the network call history data handler.
     */
    public void start()
    {
        // This method first requests the subscriber timezone information, then
        // requests the class of service.
        sLog.info("Start network call history");

        // First things first - we need to know the timezone that we are in:
        CPDataGetterCallback callback = new CPDataGetterCallback()
        {
            @Override
            public void onDataError(CPDataError error)
            {
                // Never mind, we'll use the default timezone
                sLog.error("Data error getting subscriber info " + error);
                CallHistoryActivator.getCosService().getClassOfService(
                                            NetworkCallHistoryDataHandler.this);
            }

            @Override
            public String getSIName()
            {
                return "SubscriberAccountSettings";
            }

            @Override
            public boolean onDataReceived(String data)
            {
                sLog.debug("Subscriber timezone data retrieved");
                boolean dataValid;

                try
                {
                    JSONParser parser = new JSONParser();

                    JSONArray parsedData = (JSONArray) parser.parse(data);
                    JSONObject subData = (JSONObject) parsedData.get(0);
                    subData = (JSONObject) subData.get("data");

                    String timezone = (String) subData.get("Timezone");
                    sLog.info("Got a timezone of " + timezone);
                    TimeZone subTimeZone = TimeZone.getTimeZone(timezone);
                    sLog.info("Parsed timezone to " + subTimeZone.getDisplayName());

                    // Update the timezone of the date formatter
                    CALL_DATE_FORMAT.setTimeZone(subTimeZone);

                    // Got here without exception, data must be valid
                    dataValid = true;

                    // And request the call history data
                    CallHistoryActivator.getCosService().getClassOfService(
                                            NetworkCallHistoryDataHandler.this);
                }
                catch (ParseException e)
                {
                    // It would be useful to have a dump of the data here, but
                    // it is Personally Identifyable Information, so must not
                    // be logged.
                    sLog.error("Error parsing data", e);
                    dataValid = false;
                }
                return dataValid;
            }

            @Override
            public DataFormat getDataFormat()
            {
                return DataFormat.DATA_JS;
            }
        };

        if (mCommPortalService != null)
        {
            mCommPortalService.getServiceIndication(callback, null, false);
        }
        else
        {
            sLog.warn("CommPortal Service not available - NCH not available");
        }
    }

    public void stop()
    {
        // Unregister all listeners
        sLog.info("Stop network call history");
        ClassOfServiceService cos = CallHistoryActivator.getCosService();

        if (cos != null)
        {
            cos.unregisterCallback(this);
        }

        unregisterRefreshTriggers();
    }

    @Override
    public void onCosReceived(CPCos classOfService)
    {
        boolean nchAllowed = classOfService.getCallLogEnabled() &&
                             classOfService.getSubscribedMashups()
                                           .isNetworkCallHistoryAllowed();
        sLog.debug("Class of service received " + nchAllowed);

        if (nchAllowed && !mNchAllowed)
        {
            sLog.info("Cos has changed to allow network call history");
            registerRefreshTriggers();
        }
        else if (!nchAllowed && mNchAllowed)
        {
            sLog.info("Cos has changed to deny network call history");
            unregisterRefreshTriggers();
        }

        setNchAllowed(nchAllowed);

        boolean isBcmSubscriber = !classOfService.getIchAllowed() &&
                                  classOfService.getBCMSubscribed();
        mSiName = isBcmSubscriber ? SI_NAME_CFS : SI_NAME_COMBINED;
        sLog.info("SI name to use is " + mSiName);
    }

    /**
     * Registers the listeners that are used to trigger refresh of the call list
     */
    private synchronized void registerRefreshTriggers()
    {
        // Global status - for user no longer being on the phone:
        if (mGlobalStatusService != null)
            mGlobalStatusService.addStatusChangeListener(mStatusChangeListener);

        // Network - to catch changes to call state when offline.
        mNetworkService.addNetworkConfigurationChangeListener(mNetworkListener);

        // MWI - to catch voicemails being left after a missed call
        if (mMwiProvider == null)
        {
            sLog.debug("MWI provider was null, trying to get it again");
            mMwiProvider = CallHistoryActivator.getProviderWithMwi();
        }

        if (mMwiProvider != null)
        {
            sLog.debug("Got MWI provider");
            mMwiProvider.getOperationSet(OperationSetMessageWaiting.class)
                    .addMessageWaitingNotificationListener(mMwiListener);
        }

        // Timer - to catch calls made on the same line on other clients.
        if (mTimer != null)
            mTimer.cancel();

        // Have an initial delay of 5 seconds.  At the start of day this allows
        // the UI to finish loading so that it is available to show any
        // notifications.
        int initialDelay = 5000;
        long refreshRate = mConfigService.user().getLong(CALL_LIST_REFRESH_RATE,
                                                  CALL_LIST_DEFAULT_REFRESH);
        mTimer = new Timer("Network Call History refresh timer");
        mTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                sLog.debug("Timer popped, refreshing call history");
                loadNetworkCallHistory();
            }
        }, initialDelay, refreshRate);
    }

    /**
     * Removes the listeners that are used to trigger refresh of the call list
     */
    private synchronized void unregisterRefreshTriggers()
    {
        // Global status - for user no longer being on the phone:
        if (mGlobalStatusService != null)
            mGlobalStatusService.removeStatusChangeListener(mStatusChangeListener);

        // Network - to catch changes to call state when offline.
        mNetworkService.removeNetworkConfigurationChangeListener(mNetworkListener);

        // Timer - to catch calls made on the same line on other clients.
        mTimer.cancel();
        mTimer = null;

        // MWI - to catch voicemails being left after a missed call
        if (mMwiProvider != null)
        {
            mMwiProvider.getOperationSet(OperationSetMessageWaiting.class)
                    .removeMessageWaitingNotificationListener(mMwiListener);
        }
    }

    /**
     * Called when we miss a call
     */
    public synchronized void onMissedCall()
    {
        sLog.debug("On missed call");

        // Missing a call means that we should refresh the call list.  However,
        // call entries are only update when the other end of the call hangs up
        // Thus, we need to wait a little while before requesting the data from
        // the server.  In fact, we need to refresh the list twice:
        // 1. After 10 seconds so that we catch the other end hanging up
        // 2. After 60 seconds in case the user thinks about leaving a message
        //    but then decides not to.
        if (mTimer != null)
        {
            mTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    sLog.debug("Missed call timer 1 pop");
                    loadNetworkCallHistory();
                }
            }, 10000);

            mTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    sLog.debug("Missed call timer 2 pop");
                    loadNetworkCallHistory();
                }
            }, 60000);
        }
    }

    /**
     * Requests the call history from the network
     */
    public void loadNetworkCallHistory()
    {
        sLog.debug("Loading call history from network");
        loadNetworkCallHistory(1000);
    }

    /**
     * Requests the call history from the network after a delay (immediately if
     * the delay is 0)
     *
     * @param delay the delay before requesting the data.
     */
    private synchronized void loadNetworkCallHistory(int delay)
    {
        sLog.debug("Requesting call history, allowed: " + mNchAllowed +
                                  ", already getting: " + mGettingCallHistory);

        // If the MWI provider is null, then try and get it again.
        if (mMwiProvider == null)
        {
            sLog.debug("Looking for MWI provider");
            mMwiProvider = CallHistoryActivator.getProviderWithMwi();

            // Only register the listener if the provider was null, and
            // now isn't. Otherwise the listener is already registered.
            if (mMwiProvider != null)
            {
                sLog.debug("Found MWI provider");
                mMwiProvider.getOperationSet(OperationSetMessageWaiting.class)
                            .addMessageWaitingNotificationListener(mMwiListener);
            }
        }

        // Only request the service indication if we are allowed to and if we
        // aren't already getting it.
        if (!mNchAllowed)
        {
            sLog.debug("Ignoring request to get history as mNchAllowed=" + mNchAllowed);
            return;
        }
        else if (mGettingCallHistory)
        {
            sLog.info("Ignoring request to get call history as mGettingCallHistory=" + mGettingCallHistory);
            return;
        }

        mGettingCallHistory = true;

        if (mTimer != null && delay > 0)
        {
            // Got a delay so use a timer to get the data
            mTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    sLog.debug("Get data timer pop");
                    mCommPortalService.getServiceIndication(
                               NetworkCallHistoryDataHandler.this,
                               null, false);
                }
            }, delay);
        }
        else
        {
            // No delay requested - just get it now.
            mCommPortalService.getServiceIndication(this, null, false);
        }
    }

    @Override
    public void onDataError(CPDataError error)
    {
        mGettingCallHistory = false;

        if (error == CPDataError.noSuchObject)
        {
            // Indicates that this subscriber is not allowed to use network call
            // history.  Thus stop this handler.
            sLog.info("Data indicates call history is not enabled");
            setNchAllowed(false);
            stop();
        }
        else
        {
            // It's an error, but not much we can do.  Log and we'll hope it has
            // gone away when we next request the data
            sLog.error("Data error getting call history " + error);
        }
    }

    @Override
    public boolean onDataReceived(String data)
    {
        sLog.debug("Date of last CP record is " + mLastCommPortalRecordDate);
        sLog.debug("Date of last refresh is " + mLastRefreshDate);

        boolean dataGood = true;

        try
        {
            JSONParser parser = new JSONParser();
            List<CallRecordImpl> serverRecords = new ArrayList<>();

            // Get all the local call history records that have been written
            // since we last got the Network Call History and find out the names
            // associated with each call.
            List<CallRecord> localRecords = new ArrayList<>(
                    mCallHistoryService.findCallsAddedToDbAfter(mLastRefreshDate));
            sLog.debug("Got some new local records: " + localRecords.size());

            Map<String, String> numberToNameMap = new HashMap<>();
            for (CallRecord record : localRecords)
            {
                for (CallPeerRecord peerRecord : record.getPeerRecords())
                {
                    // Address is in the form of <number>@<sbc>
                    String number = getNumberFromAddress(peerRecord.getPeerAddress());
                    String name = peerRecord.getDisplayName();

                    numberToNameMap.put(number, name);
                }
            }

            JSONArray parsedData = (JSONArray) parser.parse(data);
            JSONObject callData = (JSONObject) parsedData.get(0);
            callData = (JSONObject) callData.get("data");

            String[] callTypes =
            {
                "AnsweredCalls",
//                "RejectedCalls" - User doesn't care about rejected calls
                "MissedCalls",
                "DialedCalls"
            };

            int nbMissedCalls = 0;

            // Parse the data into call record types
            for (String callType : callTypes)
            {
                JSONObject callTypeObject = (JSONObject) callData.get(callType);
                JSONArray callTypeData = callTypeObject == null ? null :
                                         (JSONArray) callTypeObject.get("Call");

                if (callTypeData != null)
                {
                    sLog.debug("Call type " + callType + ", " + callTypeData.size());
                    for (int i = 0; i < callTypeData.size(); i++)
                    {
                        JSONObject call = (JSONObject) callTypeData.get(i);

                        CallRecordImpl record =
                               convertCallToRecord(call, callType, numberToNameMap);

                        if (record != null)
                        {
                            serverRecords.add(record);

                            if (callType.equals("MissedCalls"))
                                nbMissedCalls++;
                        }
                    }
                }
                else
                {
                    sLog.error("No call data for type " + callType +
                                                         ", " + callTypeObject);
                }
            }

            sLog.debug("Got some server records: " + serverRecords.size());

            if (!serverRecords.isEmpty())
            {
                // We've got some new server records, so update the server time.
                Date latestEndDate = mLastCommPortalRecordDate;
                for (CallRecordImpl record : serverRecords)
                {
                    if (record.getEndTime().getTime() > latestEndDate.getTime())
                    {
                        latestEndDate = record.getEndTime();
                    }
                }

                mLastCommPortalRecordDate = latestEndDate;
                mConfigService.user().setProperty(LAST_SERVER_RECORD_TIME,
                                           mLastCommPortalRecordDate.getTime());
            }

            // Now that we have parsed the entries, we know that we've got good
            // data.  Therefore, start a new thread to process the old and new
            // entries, as processing them can take a long time.
            updateEntriesInNewThread(serverRecords, localRecords, nbMissedCalls);
        }
        catch (org.json.simple.parser.ParseException e)
        {
            // Thrown trying to parse the JSON
            sLog.error("Bad data returned by the server " + data, e);
            dataGood = false;
        }
        catch (java.text.ParseException e)
        {
            // Thrown trying to parse the date of a call
            sLog.error("Date was not returned in expected format " + data, e);
            dataGood = false;
        }
        catch (ClassCastException e)
        {
            // Thrown if the JSON from the server was bad.  Sadly the data
            // contains PII, so cannot be logged.
            sLog.error("ClassCast Error parsing data from the server", e);
            dataGood = false;
        }
        catch (Exception e)
        {
            // We failed to update the call history due to an unexpected
            // exception.
            // Set mGettingCallHistory to false so that we can try again on the
            // next timer kick.
            if (dataGood)
            {
                // dataGood should always be true here, but let's be defensive
                // because if it and mGettingCallHistory are simultaneously
                // false, this task will be rescheduled twice at the same time.
                mGettingCallHistory = false;
            }

            throw new RuntimeException(e);
        }

        return dataGood;
    }

    /**
     * Updating the entries can take a long time.  Thus this method does all the
     * updating on a new thread.
     *
     * @param serverRecords The new records from the server
     * @param localRecords The local records from the local store
     * @param nbMissedCalls The number of new missed calls (from the server)
     */
    private void updateEntriesInNewThread(final List<CallRecordImpl> serverRecords,
                                          final List<CallRecord> localRecords,
                                          final int nbMissedCalls)
    {
        new Thread(CALL_HISTORY_UPDATE_THREAD)
        {
            @Override
            public void run()
            {
                try
                {
                    sLog.debug("Update call history thread starting");

                    // Update the call history by writing or removing the new /
                    // duplicated entries.
                    updateCallHistoryEntries(serverRecords, localRecords);

                    // If there are any missed calls, then add missed call
                    // notifications for them.  Unless this is the first time
                    // the client has run with network call history - we don't
                    // want to notify for very old calls.
                    if (nbMissedCalls != 0 &&
                        mNotificationService != null &&
                        !mFirstRunTime)
                    {
                        sLog.debug("Adding notification for missed calls " +
                                                                 nbMissedCalls);

                        for (int i = 0; i < nbMissedCalls; i++)
                        {
                            mNotificationService.fireNotification(
                                    "MissedCall");
                        }
                    }

                    // Update the last time that we got some data. It's important to do this after adding
                    // any CP call logs above in updateCallHistoryEntries(), so we never find any CP call
                    // logs when calling mCallHistoryService.findCallsAddedToDbAfter(mLastRefreshDate).
                    mLastRefreshDate = new Date();
                    mConfigService.user().setProperty(LAST_SERVER_UPDATE_TIME,
                                               mLastRefreshDate.getTime());
                    mFirstRunTime = false;
                }
                finally
                {
                    mGettingCallHistory = false;
                    sLog.debug("Update call history thread finished");
                }
            }
        }.start();
    }

    /**
     * A helper method to get the number from an address
     *
     * @param address the address to get the number from
     * @return the number, formatted as E164 (if possible).
     */
    private String getNumberFromAddress(String address)
    {
        String number = address == null ? "" : address.split("@")[0];

        if ("anonymous".equals(number) || number == null)
        {
            // Anonymous calls sometimes appear as "anonymous@..." or as the
            // empty string.  Standardise how these are treated by making them
            // all "".
            number = "";
        }
        else
        {
            number = mNumberUtilsService.stripELC(number);
            number = mNumberUtilsService.formatNumberToE164(number);
        }

        return number;
    }

    /**
     * Updates the call history entries to match the server by adding or removing
     * entries as necessary.
     * Checks through the server records to see if there is an outgoing and
     * incoming call which end at the same time and are to/from the same number.
     * If this is the case it is a click-to-dial call which we have initiated
     * so we delete the incoming call so there is not a double entry in the call
     * history.
     * We also retain local knowledge of the attention state for all calls (note this
     * is only relevant for missed calls - all other calls by definition don't need attention).
     * Setting the state to true for those that are newly retrieved from the server.
     * Whenever a new call history entry is added to the local database, WISPA will
     * update the UI.
     *
     * @param serverRecords The new records from the server
     * @param localRecords The records that have been added locally since the
     *                     last time that we got data.
     */
    private void updateCallHistoryEntries(List<CallRecordImpl> serverRecords,
                                          List<CallRecord> localRecords)
    {
        // Ideally, we would log this method thoroughly, as it is the brains of
        // the network call history.  However, if there are too many calls then
        // this might generate log spam.  Thus, only log if there aren't too
        // many.
        boolean log = (serverRecords.size() + localRecords.size()) < 10;
        sLog.debug("updateCallHistoryEntries, logging: " + log);

        List<CallRecordImpl> recordsToWrite = new ArrayList<>();

        // Add all the unresolved missed calls to the list of local records.
        // This is required so that we can resolve them.
        localRecords.addAll(mUnresolvedLocalMissedCalls);

        // Sort the records based on when they started - this ensures that
        // they are entered into the store in the right order (which saves
        // time).
        Comparator<CallRecord> comparator = (record1, record2) -> {
            Date record1Start = record1.getStartTime();
            Date record2Start = record2.getStartTime();

            return record1Start.compareTo(record2Start);
        };

        Collections.sort(serverRecords, comparator);
        Collections.sort(localRecords, comparator);

        boolean historyChanged = false;

        // Go through the local entries and remove them all.  Each entry will be
        // re-added once we have determined which server entries need to be added
        // to the record.  This is required so that we can preserve the order of
        // the events in the record.
        // If there are any local records which match server records, then one of
        // the records will be removed to prevent double entries.
        for (CallRecord localCallRecord : localRecords)
        {
            boolean removeRecord = true;

            if (log)
            {
                sLog.debug("Examining local record " + localCallRecord);
            }

            CallRecordImpl localCallRecordImpl = (CallRecordImpl) localCallRecord;
            List<CallPeerRecord> peerRecords = localCallRecordImpl.getPeerRecords();

            if (peerRecords.size() > 1 ||
                localCallRecordImpl.getDirection().equals(CallRecord.OUT))
            {
                if (log)
                {
                    sLog.debug("Local record has peers? " + peerRecords.size());
                }

                // This is either a conference call, or an outgoing call. In
                // either case we want to keep the local copy since it will
                // have more information than the server.
                // Thus, for each participant in the call look through the server
                // calls and find the matching entry
                for (int i = 0; i < peerRecords.size(); i++)
                {
                    CallPeerRecord peerRecord = peerRecords.get(i);

                    // Only the first entry in a conference can be received.
                    // Otherwise, the direction must be out (incoming calls that
                    // have been merged don't appear in call history as part of
                    // the conference).
                    String direction = i > 0 ? CallRecord.OUT :
                        localCallRecordImpl.getDirection();
                    CallRecord serverRecord =
                                         findMatchingServerRecord(serverRecords,
                                                                  peerRecord,
                                                                  direction);

                    if (log)
                    {
                        sLog.debug("Found matching server record " + serverRecord);
                    }

                    if (serverRecord != null)
                    {
                        serverRecords.remove(serverRecord);
                    }
                }

                // We're going to remove this record, but want it to be present,
                // so add it to the records to write.
                recordsToWrite.add(localCallRecordImpl);
            }
            else if (localCallRecordImpl.getDirection().equals(CallRecord.IN))
            {
                if (log)
                {
                    sLog.debug("Local record is incoming call " + localCallRecordImpl);
                }

                // An incoming call.  We should be able to remove this since the
                // server must know about all of the incoming calls, right?
                // Wrong.  A call which is locally missed might not yet have a
                // corresponding server entry if the call is still in progress -
                // e.g. if the caller is talking to the voicemail server.  In
                // this case we don't want to remove the missed call entry since
                // the user may want to call the caller back immediately and not
                // want to wait for the voicemail message to be left.
                if (localCallRecordImpl.getStartTime().equals(localCallRecordImpl.getEndTime()) &&
                    localCallRecordImpl.getPeerRecords().size() == 1)
                {
                    if (log)
                    {
                        sLog.debug("Local record is missed call");
                    }

                    // This is a missed call.  Try to find a corresponding
                    // server entry
                    CallPeerRecord peerRecord = localCallRecordImpl.getPeerRecords().get(0);
                    CallRecord serverRecord =
                                        findMatchingServerRecord(serverRecords,
                                                                 peerRecord,
                                                                 CallRecord.IN);

                    if (log)
                    {
                        sLog.debug("Found matching server record " + serverRecord);
                    }

                    if (serverRecord == null)
                    {
                        // No server record, so don't remove the entry.  Also
                        // keep track of it so that we can resolve it later
                        removeRecord = false;

                        // Only keep track of this record if it is actually new,
                        // and not an old one that pre-dates the server records.
                        // In essence, that means if this isn't the first time
                        // that we have run the client.
                        if (!mFirstRunTime)
                            mUnresolvedLocalMissedCalls.add(localCallRecordImpl);

                        if (localCallRecordImpl.getEndReason() == CallPeerChangeEvent.NORMAL_CALL_CLEARING)
                        {
                            // This is local record of an incoming call which
                            // has been answered on a different client. Check if
                            // if there is a matching outgoing server record.
                            serverRecord =
                                findMatchingServerRecord(serverRecords,
                                                             peerRecord,
                                                             CallRecord.OUT);

                            if (serverRecord != null &&
                                serverRecord.getEndTime().equals(
                                    (serverRecord).getStartTime()))
                            {
                                // Either:
                                // 1. We initiated a click to dial call but the
                                // other party did not answer.
                                // 2. We are currently still in a click to dial
                                // call
                                // Either way, we do not want to display the
                                // incoming call, so we delete the record.
                                //
                                // Edge case - We could be in an incoming call on a
                                // different client that we did not initiate via
                                // ctd, and whilst this call is going on we have
                                // also called them via this client. In this
                                // unlikely situation the incoming call record
                                // may be incorrectly removed.
                                mUnresolvedLocalMissedCalls.remove(localCallRecordImpl);
                                sLog.debug("Removing local incoming call record");
                                removeRecord = true;
                            }
                        }
                    }
                    else
                    {
                        boolean attention = localCallRecordImpl.getAttention();
                        sLog.debug("Server record not null, copy local attention state: "
                                + attention);
                        serverRecord.setAttention(attention);
                    }
                }
                else
                {
                    // It's now resolved.
                    mUnresolvedLocalMissedCalls.remove(localCallRecordImpl);
                }
            }

            if (removeRecord)
            {
                sLog.debug("Remove local record " + localCallRecordImpl);
                mCallHistoryService.removeRecord(localCallRecordImpl);
                historyChanged = true;
            }
        }

        sLog.debug("Unresolved missed calls is now " + mUnresolvedLocalMissedCalls.size());
        sLog.debug("Unresolved missed call is " + mUnresolvedLocalMissedCalls);

        // The list of call records with any duplicate call records removed.
        //  A duplicate call record is created when a call is initiated
        // using click to dial.
        deleteDuplicateCTDRecords(serverRecords);

        // Add the new records - however, note that we might need to look and
        // see if there is a name which matches the entry in LDAP:
        sLog.debug("Adding new records " + serverRecords.size());
        Map<String, String> addressNameMap = new HashMap<>();

        // At this point the deleteDuplicateCTDRecords method was called
        // and there is only new records from the server.
        // Fill-in the display name and attention state before they are
        // written to the local database.
        for (CallRecordImpl serverRecord : serverRecords)
        {
            CallPeerRecordImpl peerRecord = (CallPeerRecordImpl)
                                                 serverRecord.getPeerRecords().get(0);
            String peerAddress = peerRecord.getPeerAddress();

            if (peerRecord.getDisplayName() != null)
            {
                // We've obtained a display name by other means - use it.
                if (log)
                {
                    sLog.debug("Using known display name " +
                               logHasher(peerRecord.getDisplayName()));
                }
            }
            else if (addressNameMap.containsKey(peerAddress))
            {
                // We've got a name in the map.  Use it
                String displayName = addressNameMap.get(peerAddress);
                if (log)
                {
                    sLog.debug("Using local result " + logHasher(displayName));
                }
                peerRecord.setDisplayName(displayName);
            }
            else
            {
                // Going to have to look the result up in LDAP.
                if (log)
                {
                    sLog.debug("Looking up address in LDAP " + logHasher(peerAddress));
                }
                lookUpNameInLdap(addressNameMap, peerRecord);
            }

            // If we've still not got a display name, then use the address
            if (peerRecord.getDisplayName() == null)
            {
                peerRecord.setDisplayName(peerAddress);
            }

            // Setting the attention to true to be shown as new on electron UI
            // unless this is the first time this client has been run.  In that
            // case, without a local call history to compare to, we have no way
            // of knowing which calls in network call history are actually new,
            // so the first time we download network call history we have to
            // choose between 2 options:
            // 1) Flagging all missed calls in the user's history as new,
            //    meaning they would need to manually mark each individual call
            //    as not new, even though they probably already acknowledged
            //    these calls on another device.
            // 2) Flagging all missed calls in the user's history as not new,
            //    meaning that if any of those missed calls are actually new
            //    since they last logged onto a MaX UC client they won't be
            //    alerted of them on this client.
            // We've gone for option 2 as overall that gives a better UX.
            boolean attention = !mFirstRunTime;
            if (log)
            {
                sLog.debug(
                        "Setting attention based on first time run to " + attention);
            }

            serverRecord.setAttention(attention);
            recordsToWrite.add(serverRecord);
            historyChanged = true;
        }

        // Finally, write the calls to the record, but sort them first!
        Collections.sort(recordsToWrite, comparator);
        sLog.debug("Writing calls " + recordsToWrite.size());
        for (CallRecordImpl recordToWrite : recordsToWrite)
        {
            // Get the peer address to use for this record
            String peerAddress;
            List<CallPeerRecord> peerRecords = recordToWrite.getPeerRecords();

            if (peerRecords == null || peerRecords.isEmpty())
            {
                peerAddress = recordToWrite.getCallPeerContactUID();
            }
            else
            {
                peerAddress = peerRecords.get(0).getPeerAddress();
            }

            // And write it.
            mCallHistoryService.writeCall(recordToWrite, peerAddress);
        }

        if (historyChanged)
        {
            mCallHistoryService.fireCallHistoryChangeEvent();
        }
    }

    /**
     * When we initiate a click to dial call we receive both an incoming and
     * outgoing server record for the same call. If we find such a pair of
     * records, which would have the same number and end time, we delete the
     * incoming call record.
     *
     * @param serverRecords The list of server records which are to be
     * added to the call history.
     * @return The list of server records, with any duplicate call records caused
     * by click to dial calls removed.
     */
    private void deleteDuplicateCTDRecords(List<CallRecordImpl> serverRecords)
    {
        List<CallRecordImpl> incomingCalls = new ArrayList<>();
        List<CallRecordImpl> outgoingCalls = new ArrayList<>();

        for (CallRecordImpl serverRecord : serverRecords)
        {
            if (serverRecord.getDirection().equals(CallRecord.OUT))
            {
                outgoingCalls.add(serverRecord);
            }
            else if (serverRecord.getDirection().equals(CallRecord.IN))
            {
                incomingCalls.add(serverRecord);
            }
        }

        sLog.debug("Checking server records for click to dial calls");

        // Iterate through each incoming call and compare the end time of the
        // incoming call to the end times of each outgoing call
        for (CallRecordImpl incomingCall : incomingCalls)
        {
            long incomingEndTime = incomingCall.getEndTime().getTime();

            for (CallRecordImpl outgoingCall : outgoingCalls)
            {
                long outgoingEndTime = outgoingCall.getEndTime().getTime();
                long timeDifference = Math.abs(incomingEndTime - outgoingEndTime);

                if (timeDifference <= 2000)
                {
                    sLog.debug("Found two calls with the same end time."
                        + " Comparing call numbers");

                    String incomingCallNumber = getNumberFromAddress(
                            incomingCall.getPeerRecords().get(0).getPeerAddress());

                    String outgoingCallNumber = getNumberFromAddress(
                            outgoingCall.getPeerRecords().get(0).getPeerAddress());

                    // Confirm the calls are to/from the same number.
                    if (incomingCallNumber != null &&
                                incomingCallNumber.equals(outgoingCallNumber))
                    {
                        sLog.info("Removing incoming call record from " +
                            logHasher(incomingCallNumber) + " ending at " +
                            incomingCall.getEndTime() + ". It's a click to "
                            + "dial duplicate call record.");

                        serverRecords.remove(incomingCall);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Look up a name for a particular peer record in LDAP.  Once found, update
     * the display name of the peer record to be the result.
     * @param addressNameMap The map in which to store the result of the look up
     * @param peerRecord The record to update once we have a result
     */
    private void lookUpNameInLdap(final Map<String, String> addressNameMap,
                                  final CallPeerRecordImpl peerRecord)
    {
        final String peerAddress = peerRecord.getPeerAddress();
        ExtendedContactSourceService ldapContactSourceService = getLdapContactServiceService();

        if (ldapContactSourceService == null)
        {
            // Nothing we can do, so just log and return
            sLog.debug("No LDAP service to do look up");
            return;
        }

        synchronized (addressNameMap)
        {
            // The telephone number itself should be interpreted as a
            // regex literal.  For example if it begins '+44' the '+'
            // should not be treated as a special character.
            String queryString = "\\Q" + peerAddress + "\\E";
            Pattern pattern = Pattern.compile(queryString);
            ContactQuery query = ldapContactSourceService.querySourceForNumber(pattern);

            query.addContactQueryListener(new ContactQueryListener()
            {
                @Override
                public void queryStatusChanged(ContactQueryStatusEvent event)
                {
                    // This indicates the query has cancelled, ended, or
                    // hit an error. In all cases, we should stop listening.
                    sLog.debug(logHasher(peerAddress) + " query for name ended.");
                    event.getQuerySource().removeContactQueryListener(this);
                    synchronized (addressNameMap)
                    {
                        addressNameMap.put(peerAddress, null);
                        addressNameMap.notifyAll();
                    }
                }

                @Override
                public void contactReceived(ContactReceivedEvent event)
                {
                    // Got a matching contact - stop looking for anymore
                    event.getQuerySource().removeContactQueryListener(this);

                    String name = event.getContact().getDisplayName();
                    sLog.debug(logHasher(peerAddress) + " query for name found" + name);
                    peerRecord.setDisplayName(name);

                    synchronized (addressNameMap)
                    {
                        addressNameMap.put(peerAddress, name);
                        addressNameMap.notifyAll();
                    }
                }

                @Override
                public void contactRemoved(ContactRemovedEvent event)
                {
                    // Nothing required
                }

                @Override
                public void contactChanged(ContactChangedEvent event)
                {
                    // Nothing required
                }
            });

            try
            {
                // Wait until we get a result - but don't wait forever as that
                // might result in a hang.
                addressNameMap.wait(5000);
            }
            catch (InterruptedException e)
            {
                sLog.error("Exception when waiting", e);
            }
        }
    }

    /**
     * Gets the ldap service in a separate method which can be overridden for
     * unit tests
     *
     * @return the ldap service
     */
    protected ExtendedContactSourceService getLdapContactServiceService()
    {
        return (ExtendedContactSourceService)
                                  CallHistoryActivator.getContactSearchService();
    }

    /**
     * Search through the list of call records obtained from the server and see
     * if there is one that matches the given call peer record (which itself is
     * obtained from a local call record).
     *
     * @param serverRecords The call records to look through from the server
     * @param peerRecord The peer record we are trying to match
     * @param direction The direction of the peer record.
     * @return The call record that matches the peer or null if not found
     */
    private CallRecord findMatchingServerRecord(List<CallRecordImpl> serverRecords,
                                                CallPeerRecord peerRecord,
                                                String direction)
    {
        String peerNumber = getNumberFromAddress(peerRecord.getPeerAddress());

        // Look through each call record and see if it matches
        for (CallRecord serverRecord : serverRecords)
        {
            // Unfortunately, we can't just compare the start / end date of the
            // call since the server and client will probably not be exactly the
            // same, and may in fact be completely out of sync.
            //
            // Thus we say they are the same if
            // * the numbers match (in e164)
            // * the directions are the same
            String callNumber = getNumberFromAddress(
                         serverRecord.getPeerRecords().get(0).getPeerAddress());

            // Server records that are from / to international numbers will be
            // in an E164 format WITHOUT the leading plus (!).  Therefore, if
            // the local record starts with a plus, but the server one doesn't
            // add a plus to the server one.
            if (peerNumber.startsWith("+") && !callNumber.startsWith("+"))
            {
                callNumber = "+" + callNumber;
            }

            // Another standard gotcha is that users won't always dial the full
            // number (e.g. excluding the local dialing code) and so the server
            // record number won't exactly match.  Therefore let's only try to
            // match the end of the server record - not an exact match.  Note
            // the following potential flaws here (not seen in practice but
            // worth considering if this area is investigated again):
            // - If the dialled number is a numeric short code, there is a
            //   chance it could match to a longer non-short code and the wrong
            //   record get deleted.
            // - If the dialled number is a short code containing a * or # (or
            //   doesn't match the numbers in the full number), this won't
            //   match and you'll get a duplicate history entry.
            // Neither of these apply if the user has a contact containing the
            // short code as an additional number (as in that case the local
            // record contains the full number).
            if (callNumber.endsWith(peerNumber) &&
                serverRecord.getDirection().equals(direction))
            {
                // We've found a match!
                return serverRecord;
            }
        }

        return null;
    }

    /**
     * Update the value of whether network call history is enabled or not
     *
     * @param nchAllowed the new value of the NCH flag
     */
    private synchronized void setNchAllowed(boolean nchAllowed)
    {
        mNchAllowed = nchAllowed;
        ConfigurationUtils.setNetworkCallHistoryEnabled(mNchAllowed);
    }

    /**
     * Convert a call object (as received from CommPortal) into a call record.
     *
     * @param callObject The call object to parse
     * @param callType The type of call (answered, made, ...)
     * @param numberToNameMap a map of numbers to the name associated with that number
     * @return The matching call record or null if we've seen it before.
     * @throws ParseException if the data is in some way invalid
     */
    private CallRecordImpl convertCallToRecord(JSONObject callObject,
                                               String callType,
                                               Map<String, String> numberToNameMap)
                                    throws java.text.ParseException,
                                           org.json.simple.parser.ParseException
    {
        CallRecordImpl record = new CallRecordImpl();

        // Start date
        String dateTime = (String) callObject.get("DateTime");
        Date startDate = CALL_DATE_FORMAT.parse(dateTime);
        record.setStartTime(startDate);

        // End date
        Date endDate;
        if (callType.equals("MissedCalls"))
        {
            // It's a missed call, Jitsi expects it to be 0-length
            endDate = startDate;
            record.setEndTime(endDate);
        }
        else
        {
            String duration = (String) callObject.get("Duration");
            String[] durationParts = duration.split(":", 3);

            if (durationParts.length != 3)
                throw new org.json.simple.parser.ParseException(0, "Bad duration " + duration);

            int hours = Integer.parseInt(durationParts[0]);
            int mins  = Integer.parseInt(durationParts[1]);
            int secs  = Integer.parseInt(durationParts[2]);

            endDate = DateUtils.addHours(startDate, hours);
            endDate = DateUtils.addMinutes(endDate, mins);
            endDate = DateUtils.addSeconds(endDate, secs);
            record.setEndTime(endDate);
        }

        // This record is one we already know about, since it's at the same time
        // as, or before the last result we know of.  Thus return null
        if (mLastCommPortalRecordDate != null &&
            !mLastCommPortalRecordDate.before(endDate))
            return null;

        // The other person in the call
        String number = (String) callObject.get("DirectoryNumber");

        // Fix EAS feature where international numbers may or may not start with
        // the international access code or "+"...
        if (number != null)
              number = mNumberUtilsService
                                 .maybeFixBrokenInternationalisedNumber(number);

        // Call records that are got from the CFS include all calls that are
        // still in progress.  To avoid getting 2 entries for the same call,
        // we therefore return null if this is an entry for a call that is in
        // progress.
        // All dialled calls are from CFS.  If we are using the CFS SI, then all
        // answered calls are too.  Note that rejected and missed calls are, by
        // definition not going to be in progress.
        boolean isFromCfs = callType.equals("DialedCalls") ||
              (callType.equals("AnsweredCalls") && mSiName.equals(SI_NAME_CFS));

        if (mIsOnThePhone &&
            isFromCfs &&
            startDate.equals(endDate) &&
            number != null)
        {
            // We are on the phone and this record is for a zero-length dialled
            // call. Get the current calls that are in progress and see if this
            // record is for one of these calls.
            UIService uiService = CallHistoryActivator.getUIService();
            Collection<Call> calls = uiService.getInProgressCalls();

            // Note that it is not an error for calls to be empty and for us to
            // be on the phone as line state monitoring may mean we are on the
            // phone elsewhere.
            if (calls != null && !calls.isEmpty())
            {
                String formattedNumber =
                                 mNumberUtilsService.formatNumberToE164(number);

                for (Call call : calls)
                {
                    Iterator<? extends CallPeer> peers = call.getCallPeers();

                    while (peers.hasNext())
                    {
                        CallPeer peer = peers.next();
                        String peerNumber = getNumberFromAddress(peer.getAddress());

                        if (formattedNumber.equals(peerNumber))
                        {
                            // The records match!  I.e. this record is for a
                            // call that is currently in progress.
                            return null;
                        }
                    }
                }
            }
        }

        // The direction
        String direction = callType.equals("DialedCalls") ? CallRecord.OUT :
                                                            CallRecord.IN;
        record.setDirection(direction);

        // The reason the call ended, only used by incoming calls
        if (direction.equals(CallRecord.IN))
        {
            if (callType.equals("AnsweredCalls"))
            {
                // Call was answered, therefore end reason must be "normal call
                // clearing"
                int endReason = CallPeerChangeEvent.NORMAL_CALL_CLEARING;
                record.setEndReason(endReason);
            }

            // No need to set a reason otherwise
        }

        // Format the number before getting it from the map since the number map
        // contains formatted numbers.
        ResourceManagementService resources = getResourceManagementService();
        String name = (number == null) ?
            resources.getI18NString("service.gui.UNKNOWN") :
            numberToNameMap.get(mNumberUtilsService.formatNumberToE164(number));

        List<CallPeerRecord> peerRecords = record.getPeerRecords();

        CallPeerRecordImpl recordPeer = createCallPeerRecordImpl(
                                                    number, startDate, endDate);
        recordPeer.setDisplayName(name);
        peerRecords.add(recordPeer);
        return record;
    }

    /**
     * Creates a call peer record impl in a separate method so that it can
     * be overridden for unit tests
     *
     * @param peerAddress The peer address of the call record
     * @param startTime The start time and date of the call
     * @param endTime The end time and date of the call
     *
     * @return A call peer record impl
     */
    protected CallPeerRecordImpl createCallPeerRecordImpl(String peerAddress,
        Date startTime, Date endTime)
    {
        CallPeerRecordImpl recordPeer = new CallPeerRecordImpl(
                                                peerAddress, startTime, endTime);
        return recordPeer;
    }

    /**
     * Gets the resource management service in a separate method so that it can
     * be overridden for unit tests
     *
     * @return A resource management service
     */
    protected ResourceManagementService getResourceManagementService()
    {
        ResourceManagementService resources = CallHistoryActivator.getResources();
        return resources;
    }

    @Override
    public String getSIName()
    {
        return mSiName;
    }

    @Override
    public DataFormat getDataFormat()
    {
        return DataFormat.DATA_JS;
    }
}
