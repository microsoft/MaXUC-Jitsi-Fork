/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.callhistory;

import static org.jitsi.util.Hasher.logHasher;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Date;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.callhistory.network.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.callhistory.event.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.database.schema.*;
import net.java.sip.communicator.service.database.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The Call History Service stores info about the calls made.
 * Logs calls info for all protocol providers that support basic telephony
 * (i.e. those that implement OperationSetBasicTelephony).
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class CallHistoryServiceImpl
    implements  CallHistoryService,
                CallListener,
                ServiceListener
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog =
        Logger.getLogger(CallHistoryServiceImpl.class);

    private static final String DELIM = ",";

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext mBundleContext = null;

    private final DatabaseService mDatabaseService;

    private final List<CallRecordImpl> mCurrentCallRecords =
            new Vector<>();

    private final CallChangeListener mHistoryCallChangeListener =
        new HistoryCallChangeListener();

    /**
     * A list of all listeners currently registered to listen for call history
     * change events
     */
    private final Set<CallHistoryChangeListener> mCallHistoryChangeListeners
        = new HashSet<>();

    /**
     * Map which stores CallRecord transactions and whether they are being
     * added/removed when not immediately dispatched in an event.
     */
    private HashMap<CallRecord, Boolean> mDatabaseTransactions = new HashMap<CallRecord, Boolean>();

    /**
     * The network call history handler
     */
    private NetworkCallHistoryDataHandler mNetworkCallHistoryHandler;

    public CallHistoryServiceImpl(DatabaseService databaseService)
    {
        mDatabaseService = databaseService;
    }

    /**
     * Returns all the calls made by all the contacts in the supplied
     * <tt>contact</tt> after the given date.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param startDate Date the start date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByStartDate(
        MetaContact contact, Date startDate)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns all the calls added to the DB strictly after the given date
     *
     * @param date
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findCallsAddedToDbAfter(Date date)
    {
        sLog.debug("Find calls added to DB after " + date.getTime());
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<CallRecord> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findAfterDate(CallHistoryTable.NAME,
                                            CallHistoryTable.COL_CALL_END,
                                          CallHistoryTable.COL_CALL_ADDED_TO_DB,
                                          date);

            if (rs != null)
            {
                while (rs.next())
                {
                    result.add(convertDatabaseRecordToCallRecord(rs));
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Call History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns all the calls made by all the contacts in the supplied
     * MetaContact before the given date
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByEndDate(MetaContact contact,
                                                Date endDate)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns all the calls made before the given date.
     *
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByEndDate(Date endDate)
        throws RuntimeException
    {
        sLog.debug("endDate: " + endDate.getTime());
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<CallRecord> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            // Searching on COL_CALL_START may seem odd, but we want to include
            // calls that ended after the end date, but started before it!
            rs = connection.findBeforeDate(CallHistoryTable.NAME,
                                          CallHistoryTable.COL_CALL_END,
                                          CallHistoryTable.COL_CALL_START,
                                          endDate);

            if (rs != null)
            {
                while (rs.next())
                {
                    result.add(convertDatabaseRecordToCallRecord(rs));
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Call History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns all the calls made by all the contacts in the supplied
     * MetaContact between the given dates
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the conversations
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByPeriod(MetaContact contact,
        Date startDate, Date endDate)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns all the calls made between the given dates
     *
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the conversations
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByPeriod(Date startDate, Date endDate)
        throws RuntimeException
    {
        sLog.debug("startDate: " + startDate.getTime() +
            ", endDate: " + endDate.getTime());
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<CallRecord> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findByPeriod(CallHistoryTable.NAME,
                                         CallHistoryTable.COL_CALL_END,
                                         CallHistoryTable.COL_CALL_END,
                                         startDate,
                                         endDate);

            if (rs != null)
            {
                while (rs.next())
                {
                    result.add(convertDatabaseRecordToCallRecord(rs));
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Call History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns all the calls which start and end date both match the provided
     * startDate, and which colCallParticipantIDs matches the provided
     * callPeerIds.
     *
     * @param startDate Date the start/end date of the calls.
     * @param callPeerIds IDs of the call peers.
     * @return Collection of CallRecords with CallPeerRecord.
     */
    public Collection<CallRecord> findZeroLengthCallAtTimeForParticipant(
            Date startDate,
            String callPeerIds)
    {
        sLog.debug("startDate: " + startDate.getTime() +
                   ", callPeerIds: " + callPeerIds);

        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<CallRecord> result = new ArrayList<>(1);

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findZeroLengthCallAtTimeForParticipant(
                CallHistoryTable.NAME,
                CallHistoryTable.COL_CALL_START,
                CallHistoryTable.COL_CALL_END,
                CallHistoryTable.COL_CALL_PARTICIPANT_IDS,
                startDate,
                callPeerIds);

            if (rs != null)
            {
                while (rs.next())
                {
                    result.add(convertDatabaseRecordToCallRecord(rs));
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Call History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns the supplied number of calls by all the contacts
     * in the supplied metacontact
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param count calls count
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findLast(MetaContact contact, int count)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public Collection<CallRecord> findLast(int count) throws RuntimeException
    {
        sLog.debug("count: " + count);
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<CallRecord> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findLast(CallHistoryTable.NAME,
                                     CallHistoryTable.COL_CALL_END,
                                     count);

            if (rs != null)
            {
                while (rs.next())
                {
                    result.add(convertDatabaseRecordToCallRecord(rs));
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Call History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Find the calls made by the supplied peer address.
     * @param address String the (partial) address of the peer
     * @param recordCount the number of records to return (-1 for all records)
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByPeer(String address, int recordCount)
        throws RuntimeException
    {
        sLog.debug("address: " + address + ", recordCount" + recordCount);

        // Do a database lookup.  "address" is a user-supplied string to match
        // against and we want any Call History record where the participant
        // IDs or participant names field is a match.
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<CallRecord> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findByKeyword(
                CallHistoryTable.NAME,
                CallHistoryTable.COL_CALL_END,
                address,
                new String []{
                    CallHistoryTable.COL_CALL_PARTICIPANT_IDS,
                    CallHistoryTable.COL_CALL_PARTICIPANT_NAMES },
                recordCount);

            while (rs.next())
            {
                result.add(convertDatabaseRecordToCallRecord(rs));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Call History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Find the call with the supplied UID.
     *
     * @param uid String the UID of the call record
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public CallRecord findByUid(String uid)
        throws RuntimeException
    {
        sLog.debug("uid: " + uid);

        // Do a database lookup.  "uid" is a user-supplied string to match
        // against and we want any Call History record where the record UID
        // field is a match.
        DatabaseConnection connection = null;
        ResultSet rs = null;
        CallRecord result = null;

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findByKeyword(
                CallHistoryTable.NAME,
                CallHistoryTable.COL_CALL_END,
                uid,
                new String []{CallHistoryTable.COL_CALL_RECORD_UID},
                1);

            while (rs.next())
            {
                result = convertDatabaseRecordToCallRecord(rs);
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Call History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        if (result != null)
        {
            sLog.debug("found a record match");
        }
        else
        {
            sLog.debug("found no records");
        }
        return result;
    }

    /**
     * Update the call attention state.
     *
     * @param record The updated call record to update in the DB
     * @throws RuntimeException
     */
    public void updateAttentionState(CallRecord record)
        throws RuntimeException
    {
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                CallHistoryTable.NAME + " SET " +
                CallHistoryTable.COL_CALL_RECORD_ATTENTION + "=? WHERE " +
                CallHistoryTable.COL_CALL_RECORD_UID + "=?");

            preparedStatement.setBoolean(1, record.getAttention());
            preparedStatement.setString(2, record.getUid());

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not update attention state", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }

        // Indicate that the record has changed to any listeners.
        fireCallHistoryChangeEvent(record, true);
    }

    /**
     * Used to convert a row of a ResultSet to CallRecord and CallPeerRecord
     *
     * @param rs
     * @return Object CallRecord
     */
    static CallRecord convertDatabaseRecordToCallRecord(ResultSet rs)
    {
        CallRecordImpl result = new CallRecordImpl();

        List<String> callPeerIDs = null;
        List<String> callPeerNames = null;
        List<String> callPeerStart = null;
        List<String> callPeerEnd = null;
        List<CallPeerState> callPeerStates = null;

        // Get the values of each column except the peer IDs
        try
        {
            result.setProtocolProvider(getProtocolProvider(
                rs.getString(CallHistoryTable.COL_ACCOUNT_UID)));
            result.setStartTime(
                new Date(rs.getLong(CallHistoryTable.COL_CALL_START)));
            result.setEndTime(new Date(rs.getLong(CallHistoryTable.COL_CALL_END)));
            result.setDirection(convertToCallRecordDirection(
                                 CallHistoryTable.DIRECTION.values()[rs.getInt(
                                              CallHistoryTable.COL_CALL_DIR)]));
            callPeerIDs = getCSVs(rs.getString(CallHistoryTable.COL_CALL_PARTICIPANT_IDS));
            callPeerStart = getCSVs(rs.getString(CallHistoryTable.COL_CALL_PARTICIPANT_START));
            callPeerEnd = getCSVs(rs.getString(CallHistoryTable.COL_CALL_PARTICIPANT_END));
            callPeerStates = getStates(rs.getString(CallHistoryTable.COL_CALL_PARTICIPANT_STATES));
            result.setEndReason(rs.getInt(CallHistoryTable.COL_CALL_END_REASON));
            callPeerNames = getCSVs(rs.getString(CallHistoryTable.COL_CALL_PARTICIPANT_NAMES));
            result.setCallPeerContactUID(rs.getString(CallHistoryTable.COL_CALL_PEER_UID));
            result.setUid(rs.getString(CallHistoryTable.COL_CALL_RECORD_UID));
            result.setAttention(rs.getBoolean(CallHistoryTable.COL_CALL_RECORD_ATTENTION));

            final int callPeerCount = callPeerIDs == null ? 0 : callPeerIDs.size();
            for (int i = 0; i < callPeerCount; i++)
            {
                // As we iterate over the CallPeer IDs we could not be sure that
                // for some reason the start or end call list could result in
                // different size lists, so we check this first.
                Date callPeerStartValue = null;
                Date callPeerEndValue = null;

                if (i < callPeerStart.size())
                {
                    callPeerStartValue
                               = new Date(Long.parseLong(callPeerStart.get(i)));
                }
                else
                {
                    callPeerStartValue = result.getStartTime();
                    sLog.info(
                        "Call history start time list different from ids list. "
                            + "callRecordUid: " + result.getUid() + ", ID list: "
                            + callPeerIDs + ", Start time list: " + callPeerStart);
                }

                if (i < callPeerEnd.size())
                {
                    callPeerEndValue
                                 = new Date(Long.parseLong(callPeerEnd.get(i)));
                }
                else
                {
                    callPeerEndValue = result.getEndTime();
                    sLog.info(
                        "Call history end time list different from ids list: "
                            + "callRecordUid: " + result.getUid() + ", ID list: "
                            + callPeerIDs + ", End time list: " + callPeerEnd);
                }

                // Check if the Peer contact ID is "" (empty string)
                // and if so assign Anonymous to it.
                String callPeerID = callPeerIDs.get(i);
                if (callPeerID == null || "".equals(callPeerID.trim()))
                {
                    callPeerID = CallHistoryActivator.getResources()
                        .getI18NString("service.gui.ANONYMOUS");
                }

                CallPeerRecordImpl cpr =
                    new CallPeerRecordImpl(
                        callPeerID,
                        callPeerStartValue,
                        callPeerEndValue);

                // If there is no record about the states (backward compatibility)
                if (callPeerStates != null && i < callPeerStates.size())
                    cpr.setState(callPeerStates.get(i));
                else
                    sLog.info(
                        "Call history state list different from ids list: "
                            + "callRecordUid: " + result.getUid() + ", ID list: "
                            + callPeerIDs + ", State list: " + callPeerStates);

                result.getPeerRecords().add(cpr);

                if (callPeerNames != null && i < callPeerNames.size())
                    cpr.setDisplayName(callPeerNames.get(i));
            }
        }
        catch (SQLException ex)
        {
            sLog.error("Couldn't parse call history record", ex);
        }

        return result;
    }

    /**
     * Returns list of String items contained in the supplied string
     * separated by DELIM
     * @param str String
     * @return LinkedList
     */
    private static List<String> getCSVs(String str)
    {
        List<String> result = new LinkedList<>();

        if(str == null)
            return result;

        StringTokenizer toks = new StringTokenizer(str, DELIM);
        while(toks.hasMoreTokens())
        {
            result.add(toks.nextToken());
        }
        return result;
    }

    /**
     * Get the delimited strings and converts them to CallPeerState
     *
     * @param str String delimited string states
     * @return LinkedList the converted values list
     */
    private static List<CallPeerState> getStates(String str)
    {
        List<CallPeerState> result =
                new LinkedList<>();
        Collection<String> stateStrs = getCSVs(str);

        for (String item : stateStrs)
        {
            result.add(convertStateStringToState(item));
        }

        return result;
    }

    /**
     * Converts the state string to state
     * @param state String the string
     * @return CallPeerState the state
     */
    private static CallPeerState convertStateStringToState(String state)
    {
        switch (state)
        {
            case CallPeerState._CONNECTED:
                return CallPeerState.CONNECTED;
            case CallPeerState._BUSY:
                return CallPeerState.BUSY;
            case CallPeerState._FAILED:
                return CallPeerState.FAILED;
            case CallPeerState._DISCONNECTED:
                return CallPeerState.DISCONNECTED;
            case CallPeerState._ALERTING_REMOTE_SIDE:
                return CallPeerState.ALERTING_REMOTE_SIDE;
            case CallPeerState._CONNECTING:
                return CallPeerState.CONNECTING;
            case CallPeerState._ON_HOLD_LOCALLY:
                return CallPeerState.ON_HOLD_LOCALLY;
            case CallPeerState._ON_HOLD_MUTUALLY:
                return CallPeerState.ON_HOLD_MUTUALLY;
            case CallPeerState._ON_HOLD_REMOTELY:
                return CallPeerState.ON_HOLD_REMOTELY;
            case CallPeerState._INITIATING_CALL:
                return CallPeerState.INITIATING_CALL;
            case CallPeerState._INCOMING_CALL:
                return CallPeerState.INCOMING_CALL;
            default:
                return CallPeerState.UNKNOWN;
        }
    }

    /**
     * Starts the service. Check the current registered protocol providers
     * which supports BasicTelephony and adds calls listener to them
     *
     * @param bundleContext BundleContext
     */
    public void start(BundleContext bundleContext)
    {
        sLog.debug("Starting the call history implementation.");

        mBundleContext = bundleContext;

        // start listening for newly register or removed protocol providers
        bundleContext.addServiceListener(this);

        ServiceReference<?>[] protocolProviderRefs = null;

        try
        {
            protocolProviderRefs
                = bundleContext.getServiceReferences(
                        ProtocolProviderService.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            sLog.error("Error while retrieving service refs", ex);
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            sLog.debug("Found "
                       + protocolProviderRefs.length
                       + " already installed providers.");

            for (ServiceReference<?> protocolProviderRef : protocolProviderRefs)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService)
                        bundleContext.getService(protocolProviderRef);

                handleProviderAdded(provider);
            }
        }

        mNetworkCallHistoryHandler = new NetworkCallHistoryDataHandler(this,
            CallHistoryActivator.getNetworkAddressService(),
            CallHistoryActivator.getPhoneNumberUtilsService(),
            CallHistoryActivator.getConfigurationService(),
            CallHistoryActivator.getCommPortalService(),
            CallHistoryActivator.getProviderWithMwi());

        mNetworkCallHistoryHandler.start();
    }

    /**
     * Stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        bc.removeServiceListener(this);

        ServiceReference<?>[] protocolProviderRefs = null;

        try
        {
            protocolProviderRefs
                = bc.getServiceReferences(
                        ProtocolProviderService.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            sLog.error("Error while retrieving service refs", ex);
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            for (ServiceReference<?> protocolProviderRef : protocolProviderRefs)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService)
                        bc.getService(protocolProviderRef);

                handleProviderRemoved(provider);
            }
        }

        mNetworkCallHistoryHandler.stop();
    }

    /**
     * Writes the given record to the history service, immediately dispatching an event
     * @param callRecord CallRecord
     * @param peerContactUID
     */
    public void writeCall(CallRecordImpl callRecord, String peerContactUID)
    {
        writeCall(callRecord, peerContactUID, true);
    }

    /**
     * Writes the given record to the history service
     * @param callRecord CallRecord
     * @param peerContactUID
     * @param shouldDispatchEvent Whether to immediately dispatch an event
     */
    public void writeCall(CallRecordImpl callRecord, String peerContactUID, Boolean shouldDispatchEvent)
    {
        StringBuilder callPeerIDs = new StringBuilder();
        StringBuilder callPeerNames = new StringBuilder();
        StringBuilder callPeerStartTime = new StringBuilder();
        StringBuilder callPeerEndTime = new StringBuilder();
        StringBuilder callPeerStates = new StringBuilder();

        for (CallPeerRecord item : callRecord
            .getPeerRecords())
        {
            if (callPeerIDs.length() > 0)
            {
                callPeerIDs.append(DELIM);
                callPeerNames.append(DELIM);
                callPeerStartTime.append(DELIM);
                callPeerEndTime.append(DELIM);
                callPeerStates.append(DELIM);
            }

            // Check if the address isn't null. If it is then just write
            // the empty string.
            String peerAddress = item.getPeerAddress();
            if (peerAddress == null)
            {
                peerAddress = "";
            }

            callPeerIDs.append(peerAddress);
            callPeerNames.append(item.getDisplayName());
            callPeerStartTime.append(item.getStartTime().getTime());
            callPeerEndTime.append(item.getEndTime().getTime());
            callPeerStates.append(item.getState().getStateString());
        }

        String accountID = null;
        if (callRecord.getSourceCall() != null)
        {
            accountID = callRecord.getSourceCall().getProtocolProvider()
                                  .getAccountID().getAccountUniqueID();
        }

        if (callRecord.getUid() == null)
        {
            callRecord.setUid(UUID.randomUUID().toString());
        }

        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        CallHistoryTable.DIRECTION callDirection =
            callRecord.getDirection() == CallRecord.OUT ?
                CallHistoryTable.DIRECTION.OUT : CallHistoryTable.DIRECTION.IN;
        try
        {
            // A workaround for the CFS taking a while to update the Duration
            // field. It first sends us a call event with 0 duration, and then
            // after the call finishes it returns a new one with proper
            // duration,so here we remove the duplicate one with 0 duration.
            findZeroLengthCallAtTimeForParticipant(callRecord.getStartTime(), callPeerIDs.toString())
                    .forEach(record -> removeRecord(record, shouldDispatchEvent));

            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("INSERT INTO " +
                CallHistoryTable.NAME + "(" +
                CallHistoryTable.COL_ACCOUNT_UID + "," +
                CallHistoryTable.COL_CALL_START + "," +
                CallHistoryTable.COL_CALL_END + "," +
                CallHistoryTable.COL_CALL_DIR + "," +
                CallHistoryTable.COL_CALL_PARTICIPANT_IDS + "," +
                CallHistoryTable.COL_CALL_PARTICIPANT_START + "," +
                CallHistoryTable.COL_CALL_PARTICIPANT_END + "," +
                CallHistoryTable.COL_CALL_PARTICIPANT_STATES + "," +
                CallHistoryTable.COL_CALL_END_REASON + "," +
                CallHistoryTable.COL_CALL_PARTICIPANT_NAMES + "," +
                CallHistoryTable.COL_CALL_PEER_UID + "," +
                CallHistoryTable.COL_CALL_RECORD_UID + "," +
                CallHistoryTable.COL_CALL_RECORD_ATTENTION + "," +
                CallHistoryTable.COL_CALL_ADDED_TO_DB +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

            preparedStatement.setString(1, accountID);
            preparedStatement.setLong(2, callRecord.getStartTime().getTime());
            preparedStatement.setLong(3, callRecord.getEndTime().getTime());
            preparedStatement.setInt(4, callDirection.ordinal());
            preparedStatement.setString(5, callPeerIDs.toString());
            preparedStatement.setString(6, callPeerStartTime.toString());
            preparedStatement.setString(7, callPeerEndTime.toString());
            preparedStatement.setString(8, callPeerStates.toString());
            preparedStatement.setInt(9, callRecord.getEndReason());
            preparedStatement.setString(10, callPeerNames.toString());
            preparedStatement.setString(11, peerContactUID);
            preparedStatement.setString(12, callRecord.getUid());
            preparedStatement.setBoolean(13, callRecord.getAttention());
            preparedStatement.setLong(14, Instant.now().toEpochMilli());

            connection.execute(preparedStatement);

            // Fire event or add to transactions to be fired in a later event
            if (shouldDispatchEvent)
            {
                sLog.debug("Send event for add transaction");
                fireCallHistoryChangeEvent(callRecord, true);
            }
            else
            {
                sLog.debug("Store add transaction");
                mDatabaseTransactions.put(callRecord, true);
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to add Call History entry: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Remove a call history record from the history
     *
     * @param record the record to remove
     */
    public void removeRecord(CallRecord record)
    {
        removeRecord(record, true);
    }

    /**
     * Remove a call history record from the history, immediately dispatching an event
     *
     * @param record the record to remove
     * @param shouldDispatchEvent Whether to immediately dispatch an event
     */
    public void removeRecord(CallRecord record, Boolean shouldDispatchEvent)
    {
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("DELETE FROM " +
                CallHistoryTable.NAME + " WHERE " +
                CallHistoryTable.COL_CALL_RECORD_UID + " = ?");

            preparedStatement.setString(1, record.getUid());
            connection.execute(preparedStatement);

            // Fire event or add to transactions to be fired in a later event
            if (shouldDispatchEvent)
            {
                sLog.debug("Send event for remove transaction");
                fireCallHistoryChangeEvent(record, false);
            }
            else
            {
                sLog.debug("Store remove transaction");
                mDatabaseTransactions.put(record, false);
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to delete Call History entry: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * When new protocol provider is registered we check
     * does it supports BasicTelephony and if so add a listener to it
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService
            = mBundleContext.getService(serviceEvent.getServiceReference());

        sLog.trace("Received a service event for: "
            + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        sLog.debug("Service is a protocol provider.");

        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            sLog.debug("Handling registration of a new Protocol Provider.");

            handleProviderAdded((ProtocolProviderService)sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            handleProviderRemoved((ProtocolProviderService) sService);
        }
    }

    /**
     * Used to attach the Call History Service to existing or
     * just registered protocol provider. Checks if the provider has
     * implementation of OperationSetBasicTelephony
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        sLog.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a basic telephony operation set
        OperationSetBasicTelephony<?> opSetTelephony
            = provider.getOperationSet(OperationSetBasicTelephony.class);

        if (opSetTelephony != null)
        {
            opSetTelephony.addCallListener(this);
        }
        else
        {
            sLog.trace("Service did not have a basic telephony op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the calls made by it
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicTelephony<?> opSetTelephony
            = provider.getOperationSet(OperationSetBasicTelephony.class);

        if (opSetTelephony != null)
        {
            opSetTelephony.removeCallListener(this);
        }
    }

    /**
     * CallListener implementation for incoming calls
     * @param event CallEvent
     */
    public void incomingCallReceived(CallEvent event)
    {
        sLog.debug("Incoming call received " + event);
        handleNewCall(event.getSourceCall(), CallRecord.IN);
    }

    /**
     * CallListener implementation for outgoing calls
     * @param event CallEvent
     */
    public void outgoingCallCreated(CallEvent event)
    {
        sLog.debug("Outgoing call created " + event);
        handleNewCall(event.getSourceCall(), CallRecord.OUT);
    }

    /**
     * CallListener implementation for call endings
     * @param event CallEvent
     */
    public void callEnded(CallEvent event)
    {
        // Call has ended - reload the network call history.
        sLog.debug("Call ended " + event);
    }

    /**
     * Adding a record for joining peer
     * @param callPeer CallPeer
     */
    private void handlePeerAdded(CallPeer callPeer)
    {
        CallRecord callRecord = findCallRecord(callPeer.getCall());

        // no such call
        if(callRecord == null)
            return;

        callPeer.addCallPeerListener(new CallPeerAdapter()
        {
            @Override
            public void peerStateChanged(CallPeerChangeEvent evt)
            {
                if(evt.getNewValue().equals(CallPeerState.DISCONNECTED))
                    return;
                else
                {
                    CallPeerRecordImpl peerRecord =
                        findPeerRecord(evt.getSourceCallPeer());

                    if(peerRecord == null)
                        return;

                    CallPeerState newState =
                        (CallPeerState) evt.getNewValue();

                    if (newState.equals(CallPeerState.CONNECTED)
                        && !CallPeerState.isOnHold((CallPeerState)
                                evt.getOldValue()))
                        peerRecord.setStartTime(new Date());

                    peerRecord.setState(newState);

                    //Disconnected / Busy
                    //Disconnected / Connecting - fail
                    //Disconnected / Connected
                }
            }

            @Override
            public void peerDisplayNameChanged(CallPeerChangeEvent evt)
            {
                // If a peer we are interested in has a name change, we should
                // update the display name in its peer record, if one exists.
                CallPeerRecordImpl peerRecord =
                    findPeerRecord(evt.getSourceCallPeer());

                if (peerRecord != null)
                {
                    String oldDisplayName = peerRecord.getDisplayName();
                    String newDisplayName =
                                       evt.getSourceCallPeer().getDisplayName();
                    peerRecord.setDisplayName(newDisplayName);
                    sLog.debug("Call record for call at " +
                                 peerRecord.getStartTime() + "has a new name " +
                                 "for a call peer: used to be '" +
                                 logHasher(oldDisplayName) + "'; now is '" +
                                 logHasher(newDisplayName) +"'");
                }
            }
        });

        Date startDate = new Date();
        CallPeerRecordImpl newRec = new CallPeerRecordImpl(
            callPeer.getAddress(),
            startDate,
            startDate);

        newRec.setDisplayName(callPeer.getDisplayName());

        callRecord.getPeerRecords().add(newRec);
    }

    /**
     * Adding a record for removing peer from call
     * @param callPeer CallPeer
     * @param srcCall Call
     */
    private void handlePeerRemoved(CallPeer callPeer,
                                   Call srcCall)
    {
        CallRecord callRecord = findCallRecord(srcCall);
        String pAddress = callPeer.getAddress();

        if (callRecord == null)
            return;

        CallPeerRecordImpl cpRecord =
            (CallPeerRecordImpl)callRecord.findPeerRecord(pAddress);

        // no such peer
        if(cpRecord == null)
            return;

        if(!callPeer.getState().equals(CallPeerState.DISCONNECTED))
            cpRecord.setState(callPeer.getState());

        CallPeerState cpRecordState = cpRecord.getState();

        if (cpRecordState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(cpRecordState))
        {
            cpRecord.setEndTime(new Date());
        }
    }

    /**
     * Finding a CallRecord for the given call
     *
     * @param call Call
     * @return CallRecord
     */
    private CallRecordImpl findCallRecord(Call call)
    {
        for (CallRecordImpl item : mCurrentCallRecords)
        {
            if (item.getSourceCall().equals(call))
                return item;
        }

        return null;
    }

    /**
     * Returns the peer record for the given peer
     * @param callPeer CallPeer peer
     * @return CallPeerRecordImpl the corresponding record
     */
    private CallPeerRecordImpl findPeerRecord(
        CallPeer callPeer)
    {
        CallRecord record = findCallRecord(callPeer.getCall());

        if (record == null)
            return null;

        return (CallPeerRecordImpl) record.findPeerRecord(
                callPeer.getAddress());
    }

    /**
     * Adding a record for a new call
     * @param sourceCall Call
     * @param direction String
     */
    private void handleNewCall(Call sourceCall, String direction)
    {
        // if call exist. its not new
        for (CallRecordImpl currentCallRecord : mCurrentCallRecords)
        {
            if (currentCallRecord.getSourceCall().equals(sourceCall))
                return;
        }

        CallRecordImpl newRecord = new CallRecordImpl(
            direction,
            new Date(),
            null);
        newRecord.setSourceCall(sourceCall);

        sourceCall.addCallChangeListener(mHistoryCallChangeListener);

        mCurrentCallRecords.add(newRecord);

        // if has already perticipants Dispatch them
        Iterator<? extends CallPeer> iter = sourceCall.getCallPeers();
        while (iter.hasNext())
        {
            handlePeerAdded(iter.next());
        }
    }

    /**
     * Receive events for adding or removing peers from a call
     */
    private class HistoryCallChangeListener implements CallChangeListener
    {
        /**
         * Indicates that a new call peer has joined the source call.
         *
         * @param evt the <tt>CallPeerEvent</tt> containing the source call
         * and call peer.
         */
        public void callPeerAdded(CallPeerEvent evt)
        {
            handlePeerAdded(evt.getSourceCallPeer());
        }

        /**
         * Indicates that a call peer has left the source call.
         *
         * @param evt the <tt>CallPeerEvent</tt> containing the source call
         * and call peer.
         */
        public void callPeerRemoved(CallPeerEvent evt)
        {
            handlePeerRemoved(evt.getSourceCallPeer(),
                                     evt.getSourceCall());
        }

        /**
         * The CallHistoryService listens to active call state changes so that
         * it can rapidly populate the history without having to wait for the server
         * call history (which may not happen for a while if the call was answered
         * elsewhere.  It is also where we 1st know of missed historical calls, so
         * it is where we mark them as needing attention.
         *
         * @param evt the <tt>CallChangeEvent</tt> instance containing the source
         * calls and its old and new state.
         */
        public void callStateChanged(CallChangeEvent evt)
        {
            CallRecordImpl callRecord = findCallRecord(evt.getSourceCall());
            boolean missedCall = false;

            // no such call
            if (callRecord == null)
                return;

            sLog.entry();

            if (evt.getNewValue().equals(CallState.CALL_ENDED))
            {
                if(evt.getOldValue().equals(CallState.CALL_INITIALIZATION))
                {
                    sLog.debug("Call Ended - missed");
                    missedCall = true;

                    // If network call history is enabled, then refresh the call
                    // history list - it may have been answered elsewhere.
                    if (ConfigurationUtils.isNetworkCallHistoryEnabled())
                    {
                        mNetworkCallHistoryHandler.onMissedCall();
                    }

                    callRecord.setEndTime(callRecord.getStartTime());

                    // if call was answered elsewhere, add its reason
                    // so we can distinguish it from missed
                    if(evt.getCause() != null
                           && evt.getCause().getReasonCode() ==
                                CallPeerChangeEvent.NORMAL_CALL_CLEARING)
                    {
                        callRecord.setEndReason(evt.getCause().getReasonCode());
                    }
                }
                else
                {
                    sLog.debug("Call Ended");
                    callRecord.setEndTime(new Date());
                }

                String peerContactUID = getPeerContactUID(callRecord);

                // If the peerContactUID could not be obtained, try getting it
                // from the call peer
                if (peerContactUID.isEmpty())
                {
                    if (evt.getCause() != null)
                    {
                        Object eventSourceCause = evt.getCause().getSource();

                        if (eventSourceCause instanceof AbstractCallPeer)
                        {
                            @SuppressWarnings("rawtypes")

                            MetaContact peerMetaContact =
                                               ((AbstractCallPeer)eventSourceCause)
                                                                 .getMetaContact();

                            if (peerMetaContact != null)
                            {
                                peerContactUID = peerMetaContact.getMetaUID();
                            }
                        }
                    }
                }

                // Set attention to true if it is a missed incoming call.
                // This is the point that attention is set to true for new missed calls:
                // by writing a temporary record that will be combined with the network
                // call history once that arrives.
                if (missedCall && callRecord.getDirection() == CallRecord.IN)
                {
                    callRecord.setAttention(true);
                }

                writeCall(callRecord, peerContactUID);

                // Notify all listeners that the call history has changed
                fireCallHistoryChangeEvent();

                mCurrentCallRecords.remove(callRecord);

                // Call has ended, make sure we request the call history (unless
                // it was missed in which case we are already handling)
                if (!missedCall)
                    mNetworkCallHistoryHandler.loadNetworkCallHistory();
            }

            sLog.exit();
        }
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt> corresponding to the given
     * account identifier.
     * @param accountUID the identifier of the account.
     * @return the <tt>ProtocolProviderService</tt> corresponding to the given
     * account identifier
     */
    private static ProtocolProviderService getProtocolProvider(String accountUID)
    {
        for (ProtocolProviderFactory providerFactory
                : CallHistoryActivator.getProtocolProviderFactories().values())
        {
            ServiceReference<?> serRef;

            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                if (accountID.getAccountUniqueID().equals(accountUID))
                {
                    serRef = providerFactory.getProviderForAccount(accountID);

                    return (ProtocolProviderService) CallHistoryActivator
                        .bundleContext.getService(serRef);
                }
            }
        }
        return null;
    }

    /**
     * Returns the peer UID for the given call record, or an empty string if
     * none is found.
     * @param callRecord the call record to use
     * @return the UID corresponding to the peer for this call record.
     */
    private String getPeerContactUID(CallRecordImpl callRecord)
    {
        return callRecord.getCallPeerContactUID();
    }

    public void addCallHistoryChangeListener(CallHistoryChangeListener listener)
    {
       synchronized(mCallHistoryChangeListeners)
       {
           mCallHistoryChangeListeners.add(listener);
       }
    }

    public void removeCallHistoryChangeListener(CallHistoryChangeListener listener)
    {
        synchronized (mCallHistoryChangeListeners)
        {
            mCallHistoryChangeListeners.remove(listener);
        }
    }

    /**
     * Dispatches an event to all registered call history change listeners.
     */
    public void fireCallHistoryChangeEvent()
    {
        if (mDatabaseTransactions.size() != 0)
        {
            fireCallHistoryChangeEvent(mDatabaseTransactions);
        }
        mDatabaseTransactions = new HashMap<CallRecord, Boolean>();
    }

    /**
     * Dispatches an event to all registered call history change listeners.
     * @param record call record to form data to dispatch in the event
     * @param addedOrChanged true if the record was added or edited in database, false
     *              if deleted.
     */
    public void fireCallHistoryChangeEvent(CallRecord record, Boolean addedOrChanged)
    {
        HashMap<CallRecord, Boolean> transaction = new HashMap<CallRecord, Boolean>();
        transaction.put(record, addedOrChanged);
        fireCallHistoryChangeEvent(transaction);
    }

    /**
     * Dispatches an event to all registered call history change listeners.
     * @param transactions data to dispatch in the event
     */
    public void fireCallHistoryChangeEvent(HashMap<CallRecord, Boolean> transactions)
    {
        CallHistoryChangeListener[] listeners;

        synchronized(mCallHistoryChangeListeners)
        {
            listeners = mCallHistoryChangeListeners.toArray(
                 new CallHistoryChangeListener [mCallHistoryChangeListeners.size()]);
        }

        sLog.debug("Dispatching a CallHistoryChange event for "
                   + transactions.size()
                   + " transactions to "
                   + listeners.length
                   + " listeners.");

        for (CallHistoryChangeListener listener : listeners)
            {
                listener.callHistoryChanged(transactions);
            }
    }

    /**
     * Convert the direction enum used in the database to the direction string
     * used in call records
     * @param dir the direction returned from the database
     * @return the matching string used in call records
     */
    private static String convertToCallRecordDirection(CallHistoryTable.DIRECTION dir)
    {
        switch(dir)
        {
            case IN :
                return CallRecord.IN;
            case OUT :
                return CallRecord.OUT;
            default: return null;
        }
    }

    public NetworkCallHistoryDataHandler getNetworkCallHistoryHandler()
    {
        return mNetworkCallHistoryHandler;
    }
}
