// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schema;

import net.java.sip.communicator.service.database.schemautil.*;

/**
 * The SQL table for 1-1 IM History.  One row per 1-1 message.
 */
public interface MessageHistoryTable
{
    String NAME = "MessageHistoryTable";

    // Column names.  ID uniquely identifies each row.
    // Because the user can log out of one IM account and into another, we
    // store their JID (localJID) in every row.
    String COL_ID = "colID";
    String COL_LOCAL_JID = "colLocalJID";
    String COL_REMOTE_JID = "colRemoteJID";
    String COL_DIR = "colDir";
    String COL_TEXT = "colText";
    String COL_MSG_ID = "colMsgid";
    String COL_RECEIVED_TIMESTAMP = "colReceivedTimestamp";
    String COL_TYPE = "colType";
    String COL_READ = "colRead";
    String COL_FAILED = "colFailed";
    String COL_SMPP_ID = "colSmppid";

    // For COL_DIR
    enum DIRECTION
    {
        IN,
        OUT
    }

    // For COL_TYPE
    enum TYPE
    {
        IM,
        SMS
    }

    DBTable TABLE = new DBTable(NAME, DBTable.STORAGE.CACHED)
        .addColumn(new PrimaryKey(COL_ID).notNull())
        .addColumn(new TextDBColumn(COL_LOCAL_JID).notNull())
        .addColumn(new TextDBColumn(COL_REMOTE_JID).notNull())
        .addColumn(new IntDBColumn(COL_DIR).notNull())
        .addColumn(new TextDBColumn(COL_TEXT, 5000))
        .addColumn(new TextDBColumn(COL_MSG_ID).notNull())
        .addColumn(new LongDBColumn(COL_RECEIVED_TIMESTAMP).notNull())
        .addColumn(new IntDBColumn(COL_TYPE).notNull())
        .addColumn(new BoolDBColumn(COL_READ).notNull())
        .addColumn(new IntDBColumn(COL_TYPE).notNull())
        .addColumn(new BoolDBColumn(COL_FAILED).notNull())
        .addColumn(new TextDBColumn(COL_SMPP_ID))
        ;

    // Queries are usually by local JID and remote JID, then ordered by date
    // and then by message ID.  Multiple messages with the same date should be
    // rare, so don't bother indexing that.
    DBIndex INDEX_JIDS =
        new DBIndex("MessageHistoryIndexJIDS", NAME)
            .addColumn(new IndexColumn(COL_LOCAL_JID))
            .addColumn(new IndexColumn(COL_REMOTE_JID));
    DBIndex INDEX_JIDS_AND_DATE =
        new DBIndex("MessageHistoryIndexJIDSAndDate", NAME)
            .addColumn(new IndexColumn(COL_LOCAL_JID))
            .addColumn(new IndexColumn(COL_REMOTE_JID))
            .addColumn(new IndexColumn(COL_RECEIVED_TIMESTAMP));
    // We also need to do lookups by JIDs and message ID.
    DBIndex INDEX_JIDS_AND_MSG_ID =
        new DBIndex("MessageHistoryIndexLocalJIDAndMsgID", NAME)
            .addColumn(new IndexColumn(COL_LOCAL_JID))
            .addColumn(new IndexColumn(COL_REMOTE_JID))
            .addColumn(new IndexColumn(COL_MSG_ID));
    // And by JIDs and SMPP ID.
    DBIndex INDEX_JIDS_AND_SMPP_ID =
        new DBIndex("MessageHistoryIndexLocalJIDAndSMPPID", NAME)
            .addColumn(new IndexColumn(COL_LOCAL_JID))
            .addColumn(new IndexColumn(COL_REMOTE_JID))
            .addColumn(new IndexColumn(COL_SMPP_ID));
}
