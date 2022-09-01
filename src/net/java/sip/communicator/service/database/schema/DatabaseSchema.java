// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schema;

/**
 * Any change to any class in the schema requires an increment to the version
 * number.
 *
 * General guidelines:
 *
 * - Store dates as integers (longs) in UTC milliseconds since the epoch.  This
 *   avoids confusion over timezones, keeps the code reasonably simple and fits
 *   well with using Java's Date object in your code.
 *   Assuming your code wants to use Java Date objects, use Date.getTime() to
 *   get a long to store in the database and Date(databasevalue) to get a Date
 *   object back.
 *
 * - Define an enum for any column which has a (reasonably small) finite set of
 *   possible values.  Use an integer column type and store with enum.ordinal().
 *   Retrieve with EnumType.values()[integervalue].
 */
public final class DatabaseSchema
{
    /**
    * The current database version number.
    *
    * History:
    * 1 Database introduced with four tables: CallHistory, FileHistory,
    *   MessageHistory and GroupMessageHistory.
    * 2 Add CallHistoryTable.COL_CALL_ADDED_TO_DB column to CallHistory table.
    * 3 CallHistory table gains the column: colCallRecordAttention
    * 4 FileHistoryTable gains the column: colAttention
    */
    public static final int VERSION = 4;
}
