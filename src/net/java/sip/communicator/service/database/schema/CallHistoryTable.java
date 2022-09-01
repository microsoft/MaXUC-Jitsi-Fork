// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schema;

import net.java.sip.communicator.service.database.schemautil.*;

/**
 * The SQL table for Call History.  One row per call.
 */
public interface CallHistoryTable
{
    String NAME = "CallHistoryTable";

    // Column names.  ID uniquely identifies each row.
    String COL_ID = "colID";
    String COL_ACCOUNT_UID = "colAccountUID";
    String COL_CALL_START = "colCallStart";
    String COL_CALL_END = "colCallEnd";
    String COL_CALL_DIR = "colDir";
    String COL_CALL_PARTICIPANT_IDS = "colCallParticipantIDs";
    String COL_CALL_PARTICIPANT_START = "colCallParticipantStart";
    String COL_CALL_PARTICIPANT_END = "colCallParticipantEnd";
    String COL_CALL_PARTICIPANT_STATES = "colCallParticipantStates";
    String COL_CALL_END_REASON = "colCallEndReason";
    String COL_CALL_PARTICIPANT_NAMES = "colCallParticipantNames";
    String COL_CALL_PEER_UID = "colCallPeerUID";
    String COL_CALL_RECORD_UID = "colCallRecordUid";
    String COL_CALL_ADDED_TO_DB = "colTimeAddedToDb"; // Added in version 2.
    String COL_CALL_RECORD_ATTENTION = "colCallRecordAttention"; // Added in version 3.

    // For COL_CALL_DIR
    enum DIRECTION
    {
        IN,
        OUT
    }

    // Note this TABLE is for the DB at version 1.  Additional columns are added in the migrations in DatabaseServiceImpl.
    DBTable TABLE = new DBTable(NAME, DBTable.STORAGE.MEMORY)
        .addColumn(new PrimaryKey(COL_ID).notNull())
        .addColumn(new TextDBColumn(COL_ACCOUNT_UID))
        .addColumn(new LongDBColumn(COL_CALL_START).notNull())
        .addColumn(new LongDBColumn(COL_CALL_END).notNull())
        .addColumn(new IntDBColumn(COL_CALL_DIR).notNull())
        .addColumn(new TextDBColumn(COL_CALL_PARTICIPANT_IDS).notNull())
        .addColumn(new TextDBColumn(COL_CALL_PARTICIPANT_START).notNull())
        .addColumn(new TextDBColumn(COL_CALL_PARTICIPANT_END).notNull())
        .addColumn(new TextDBColumn(COL_CALL_PARTICIPANT_STATES).notNull())
        .addColumn(new IntDBColumn(COL_CALL_END_REASON).notNull())
        .addColumn(new TextDBColumn(COL_CALL_PARTICIPANT_NAMES))
        .addColumn(new TextDBColumn(COL_CALL_PEER_UID))
        .addColumn(new TextDBColumn(COL_CALL_RECORD_UID).notNull())
        ;

    // Queries are usually by start date or end date, so index both.
    DBIndex INDEX_CALL_START =
        new DBIndex("CallHistoryIndexCallStart", NAME)
            .addColumn(new IndexColumn(COL_CALL_START));
    DBIndex INDEX_CALL_END =
        new DBIndex("CallHistoryIndexCallEnd", NAME)
            .addColumn(new IndexColumn(COL_CALL_END));
}
