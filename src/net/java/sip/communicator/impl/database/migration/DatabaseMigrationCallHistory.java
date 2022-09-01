// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.database.migration;

import java.io.*;
import java.sql.*;

import org.jitsi.service.fileaccess.*;
import org.w3c.dom.*;

import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.database.schema.*;

/**
 * Implements one-off migration of all stored data from the obsolete
 * HistoryService (XML) to the DatabaseService for the Call histories.
 */
public class DatabaseMigrationCallHistory extends DatabaseMigration
{
    /**
     * @param connection The DatabaseConnection.
     */
    public DatabaseMigrationCallHistory(FileAccessService fileAccessService,
                                        DatabaseConnection connection)
    {
        super(fileAccessService, connection);
    }

    /**
     */
    protected void populateDatabaseFromOldXmlFiles()
    {
        // Fixed location for the one CallHistory.
        File histDir = new File(mHistoryDir, "callhistory" + File.separator +
            "default" + File.separator + "default");

        for (File file : getXmlFilesToProcess(histDir))
        {
            processXmlFile(file, null, null);
        }
    }

    /**
     * Read one XML record and write it to the database.
     * @param node The XML node to process.
     * @param localJid The local JID (not used).
     * @param remoteJid The remote JID (not used).
     */
    protected void processXmlRecord(Element node,
                                    String localJid,
                                    String remoteJid)
        throws SQLException
    {
        // Cautious approach in case any field is missing.
        String accountUID = valueFromElement(node, "accountUID");
        String callStart = valueFromElement(node, "callStart");
        String callEnd = valueFromElement(node, "callEnd");
        String dir = valueFromElement(node, "dir");
        String callParticipantIDs = valueFromElement(node, "callParticipantIDs");
        String callParticipantStart = valueFromElement(node, "callParticipantStart");
        String callParticipantEnd = valueFromElement(node, "callParticipantEnd");
        String callParticipantStates = valueFromElement(node, "callParticipantStates");
        String callEndReason = valueFromElement(node, "callEndReason");
        String callParticipantNames = valueFromElement(node, "callParticipantNames");
        String callPeerUID = valueFromElement(node, "callPeerUID");
        String callRecordUid = valueFromElement(node, "callRecordUid");
        String callRecordAttention = valueFromElement(node, "callRecordAttention");

        // ...convert to the right types for the database and add the record.
        addCallHistoryEntry(
            accountUID,
            xmlTimestampToDatabaseTimestamp(callStart),
            xmlTimestampToDatabaseTimestamp(callEnd),
            dir.equals("in") ? CallHistoryTable.DIRECTION.IN : CallHistoryTable.DIRECTION.OUT,
            callParticipantIDs,
            xmlTimestampsToDatabaseTimestamps(callParticipantStart),
            xmlTimestampsToDatabaseTimestamps(callParticipantEnd),
            callParticipantStates,
            Integer.parseInt(callEndReason),
            callParticipantNames,
            callPeerUID,
            callRecordUid,
            Boolean.parseBoolean(callRecordAttention));
    }

    /**
     * Build the prepared statement once only.
     */
    protected void buildPreparedStatement()
        throws SQLException
    {
        mPreparedStatement = mConnection.prepare("INSERT INTO " + CallHistoryTable.NAME +
            "(" + CallHistoryTable.COL_ACCOUNT_UID + "," +
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
            CallHistoryTable.COL_CALL_RECORD_UID + ")  VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
    }

    /**
     * Add a new File Transfer entry into the database.
     * @param accountUid
     */
    private void addCallHistoryEntry(String accountUid,
                                     long startDate,
                                     long endDate,
                                     CallHistoryTable.DIRECTION direction,
                                     String participantIds,
                                     String participantStart,
                                     String participantEnd,
                                     String participantStates,
                                     int endReason,
                                     String participantNames,
                                     String peerUid,
                                     String recordUid,
                                     Boolean recordAttention)
        throws SQLException
    {
        mPreparedStatement.setString(1, accountUid);
        mPreparedStatement.setLong(2, startDate);
        mPreparedStatement.setLong(3, endDate);
        mPreparedStatement.setInt(4, direction.ordinal());
        mPreparedStatement.setString(5, participantIds);
        mPreparedStatement.setString(6, participantStart);
        mPreparedStatement.setString(7, participantEnd);
        mPreparedStatement.setString(8, participantStates);
        mPreparedStatement.setInt(9, endReason);
        mPreparedStatement.setString(10, participantNames);
        mPreparedStatement.setString(11, peerUid);
        mPreparedStatement.setString(12, recordUid);
        mPreparedStatement.setBoolean(13, recordAttention);

        mConnection.executeNoClose(mPreparedStatement);
    }
}
