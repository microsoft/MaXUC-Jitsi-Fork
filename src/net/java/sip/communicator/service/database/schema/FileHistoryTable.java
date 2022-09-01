// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schema;

import net.java.sip.communicator.service.database.schemautil.*;

/**
 * The SQL table for File (Transfer) History.  One row per file transfer.
 */
public interface FileHistoryTable
{
    String NAME = "FileHistoryTable";

    // Column names.  ID uniquely identifies each row.
    // Because the user can log out of one IM account and into another, we
    // store their JID (localJID) in every row.
    String COL_ID = "colID";
    String COL_LOCAL_JID = "colLocalJID";
    String COL_REMOTE_JID = "colRemoteJID";
    String COL_FT_ID = "colFtid";
    String COL_FILE = "colFile";
    String COL_DIR = "colDir";
    String COL_DATE = "colDate";
    String COL_STATUS = "colStatus";
    String COL_ATTENTION = "colAttention"; // Added in version 4

    // For COL_DIR.
    enum DIRECTION
    {
        IN,
        OUT
    }

    // For COL_STATUS.
    public enum STATUS
    {
         ACTIVE,
         COMPLETED,
         CANCELED,
         FAILED,
         REFUSED,
         WAITING,
         PREPARING
    }

    DBTable TABLE = new DBTable(NAME, DBTable.STORAGE.CACHED)
        .addColumn(new PrimaryKey(COL_ID).notNull())
        .addColumn(new TextDBColumn(COL_LOCAL_JID).notNull())
        .addColumn(new TextDBColumn(COL_REMOTE_JID).notNull())
        .addColumn(new TextDBColumn(COL_FT_ID).notNull())
        .addColumn(new TextDBColumn(COL_FILE).notNull())
        .addColumn(new IntDBColumn(COL_DIR).notNull())
        .addColumn(new LongDBColumn(COL_DATE).notNull())
        .addColumn(new IntDBColumn(COL_STATUS).notNull())
        ;

    // Queries are usually by local JID and remote JID, then ordered by date
    // and then by file transfer ID.  Multiple messages with the same date
    // should be rare, so don't bother indexing that.
    DBIndex INDEX_JIDS =
        new DBIndex("FileHistoryIndexJIDS", NAME)
            .addColumn(new IndexColumn(COL_LOCAL_JID))
            .addColumn(new IndexColumn(COL_REMOTE_JID));
    DBIndex INDEX_JIDS_AND_DATE =
        new DBIndex("FileHistoryIndexJIDSAndDate", NAME)
            .addColumn(new IndexColumn(COL_LOCAL_JID))
            .addColumn(new IndexColumn(COL_REMOTE_JID))
            .addColumn(new IndexColumn(COL_DATE));
    // We also need to do lookups by local JID and file transfer ID.
    DBIndex INDEX_LOCAL_JID_AND_FT_ID =
        new DBIndex("FileHistoryIndexLocalJIDAndFTID", NAME)
            .addColumn(new IndexColumn(COL_LOCAL_JID))
            .addColumn(new IndexColumn(COL_FT_ID));
}
