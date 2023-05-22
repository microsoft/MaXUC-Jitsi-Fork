// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database;

import java.sql.*;
import java.util.*;

/**
 * Represents a connection to the database.
 */
public interface DatabaseConnection
{
    /**
     * Issue an SQL query.
     * @param sql An SQL query statement.
     * @return The data produced by the given query.  Never null.
     * @throws SQLException on any SQL error.
     */
    ResultSet query(String sql)
        throws SQLException;

    /**
     * Issue an SQL query.
     * @param preparedStatement A prepared SQL query statement.
     * @return The data produced by the given query.  Never null.
     * @throws SQLException on any SQL error.
     */
    ResultSet query(PreparedStatement preparedStatement)
        throws SQLException;

    /**
     * Execute an SQL update.
     * @param sql An SQL update statement.
     * @return The row count or zero, depending on the statement.
     * @throws SQLException on any SQL error.
     */
    int execute(String sql)
        throws SQLException;

    /**
     * Execute a prepared SQL update and log the SQL statement.
     * @param preparedStatement A prepared SQL update statement.
     * @return The row count or zero, depending on the statement.
     * @throws SQLException on any SQL error.
     */
    int execute(PreparedStatement preparedStatement)
        throws SQLException;

    /**
     * Execute a prepared SQL update without logging the SQL statement.
     * @param preparedStatement A prepared SQL update statement.
     * @return The row count or zero, depending on the statement.
     * @throws SQLException on any SQL error.
     */
    int executeNoLog(PreparedStatement preparedStatement)
        throws SQLException;

    /**
     * Execute but don't close a prepared SQL update, returning the result.
     * @param preparedStatement A prepared SQL update statement.
     * @return The row count or zero, depending on the statement.
     * @throws SQLException on any SQL error.
     */
    int executeNoClose(PreparedStatement preparedStatement)
        throws SQLException;

    /**
     * Execute but don't close a prepared SQL update, returning the result.
     * Doesn't write the SQL statement to the log.
     * @param preparedStatement A prepared SQL update statement.
     * @return The row count or zero, depending on the statement.
     * @throws SQLException on any SQL error.
     */
    int executeNoLogNoClose(PreparedStatement preparedStatement)
        throws SQLException;

    /**
     * Prepare an SQL statement.
     * @param sql
     * @return The prepared statement.
     * @throws SQLException on any SQL error.
     */
    PreparedStatement prepare(String sql)
        throws SQLException;

    /**
     * Start a database transaction.  After calling this, the caller should
     * make execute and/or query calls before calling commitTransaction().
     * @throws SQLException on any SQL error.
     */
    void startTransaction()
        throws SQLException;

    /**
     * Must be called to complete a transaction started with startTransaction().
     * @throws SQLException on any SQL error.
     */
    void commitTransaction()
        throws SQLException;

    /**
     * Must be called when finished with this DatabaseConnection, to free it up
     * for reuse.
     */
    void close()
        throws SQLException;

    /**
     * Log the string, prepended with the hashcode for the JDBC connection
     * @param logString String to log
     */
    void log(String logString);

    /**
     * Return a ResultSet containing all rows strictly after the specified date.
     * Used by CallHistoryService.
     * @param tableName The name of the table to query.
     * @param orderColName The name of the column to order the results by.
     * @param dateColName The name of the column representing the date.
     * @param date The date to use in the query (epoch time).
     * @return The ResultSet in descending order of order column. Never null.
     */
    ResultSet findAfterDate(String tableName,
                            String orderColName,
                            String dateColName,
                            java.util.Date date)
        throws SQLException;

    /**
     * Return a ResultSet containing all records for the given local JID and
     * remote JID strictly after the given date.
     * This version is intended for IM-specific tables.
     * Used by FileHistoryService and MessageHistoryService.
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJid The remote JID.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param date The date to use in the query (epoch time).
     * @return The ResultSet in ascending order of order column. Never null.
     */
    ResultSet findAfterDate(String tableName,
                            String localJidColName,
                            String localJid,
                            String remoteJidColName,
                            String remoteJid,
                            String dateColName,
                            String idColName,
                            java.util.Date date)
        throws SQLException;

    /**
     * Return a ResultSet containing all records for the given local JID and
     * remote JIDs strictly after the given date.
     * This version is intended for IM-specific tables.
     * Used by FileHistoryService and MessageHistoryService.
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJids The remote JIDs.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param date The date to use in the query (epoch time).
     * @return The ResultSet.  Never null.
     */
    ResultSet findAfterDate(String tableName,
                            String localJidColName,
                            String localJid,
                            String remoteJidColName,
                            List<String> remoteJids,
                            String dateColName,
                            String idColName,
                            java.util.Date date)
        throws SQLException;

    /**
     * Return a ResultSet containing all records strictly before the specified date.
     * @param tableName The name of the table to query.
     * @param orderColName The name of the column to order the results by.
     * @param dateColName The name of the column representing the date.
     * @param date The date to use in the query (epoch time).
     * @return The ResultSet.  Never null.
     */
    ResultSet findBeforeDate(String tableName,
                             String orderColName,
                             String dateColName,
                             java.util.Date date)
        throws SQLException;

    /**
     * Return a ResultSet containing all records for the given local JID and
     * remote JID strictly before the specified date.
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJid The remote JID.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param date The date to use in the query (epoch time).
     * @return The ResultSet.  Never null.
     */
    ResultSet findBeforeDate(String tableName,
                            String localJidColName,
                            String localJid,
                            String remoteJidColName,
                            String remoteJid,
                            String dateColName,
                            String idColName,
                            java.util.Date date)
        throws SQLException;

    /**
     * Return a ResultSet containing all records for the given local JID and
     * remote JIDs strictly before the specified date.
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJids The remote JIDs.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param date The date to use in the query (epoch time).
     * @return The ResultSet.  Never null.
     */
    ResultSet findBeforeDate(String tableName,
                             String localJidColName,
                             String localJid,
                             String remoteJidColName,
                             List<String> remoteJids,
                             String dateColName,
                             String idColName,
                             java.util.Date date)
        throws SQLException;

    /**
     * Return a ResultSet containing all rows between the specified dates.
     * @param tableName The name of the table to query.
     * @param orderColName The name of the column to order the results by.
     * @param dateColName The name of the column representing the date.
     * @param startDate The start date (inclusive) to use in the query.
     * @param endDate The end date (exclusive) to use in the query.
     * @return The ResultSet.  Never null.
     */
    ResultSet findByPeriod(String tableName,
                           String orderColName,
                           String dateColName,
                           java.util.Date startDate,
                           java.util.Date endDate)
        throws SQLException;

    /**
     * Return a ResultSet containing all rows which start and end date are
     * exactly as the provided startDate, and which callPeerIds match the
     * provided callPeerIds.
     *
     * @param tableName The name of the table to query.
     * @param startDateColName The name of the column representing the start
     *                         date.
     * @param endDateColName The name of the column representing the end date.
     * @param callPeerIdsColName The name of the column representing the call
     *                           peer ids.
     * @param startDate The start date to use in the query.
     * @param callPeerIds IDs of the call peers to use in the query.
     * @return The ResultSet.  Never null.
     */
    ResultSet findZeroLengthCallAtTimeForParticipant(String tableName,
                                                     String startDateColName,
                                                     String endDateColName,
                                                     String callPeerIdsColName,
                                                     java.util.Date startDate,
                                                     String callPeerIds)
            throws SQLException;

    /**
     * Return a ResultSet containing all records for the given local JID and
     * remote JID between the specified dates.
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJid The remote JID.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param startDate The start date (inclusive) to use in the query.
     * @param endDate The end date (exclusive) to use in the query.
     * @return The ResultSet.  Never null.
     */
    ResultSet findByPeriod(String tableName,
                           String localJidColName,
                           String localJid,
                           String remoteJidColName,
                           String remoteJid,
                           String dateColName,
                           String idColName,
                           java.util.Date startDate,
                           java.util.Date endDate)
        throws SQLException;

    /**
     * Return a ResultSet containing all records for the given local JID and
     * remote JIDs between the specified dates.
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJids The remote JID.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param startDate The start date to use in the query (epoch time).
     * @param endDate The start date to use in the query (epoch time).
     * @return The ResultSet.  Never null.
     */
    ResultSet findByPeriod(String tableName,
                           String localJidColName,
                           String localJid,
                           String remoteJidColName,
                           List<String> remoteJids,
                           String dateColName,
                           String idColName,
                           java.util.Date startDate,
                           java.util.Date endDate)
        throws SQLException;

    /**
     * Return a ResultSet containing the last count rows.
     * @param tableName The name of the table to query.
     * @param orderColName The name of the column to order the results by.
     * @param count The maximum number of rows to return.
     * @return The ResultSet.  Never null.
     */
    ResultSet findLast(String tableName,
                       String orderColName,
                       int count)
        throws SQLException;

    /**
     * Return a ResultSet containing the last record for each conversation (in
     * the given table) with the given local jid.
     *
     * @param table The name of the table to query.
     * @param localJidCol The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidCol The name of the remoteJid column.
     * @param dateCol The name of the column representing the date.
     * @return The ResultSet. Never null.
     */
    ResultSet findLast(String table,
                       String localJidCol,
                       String localJid,
                       String remoteJidCol,
                       String dateCol)
        throws SQLException;

    /**
     * Return a ResultSet containing the last record for each conversation (in
     * the given table) with the given local jid and type equal to the passed in
     * type.
     *
     * @param tableName The name of the table to query.
     * @param localJidCol The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidCol The name of the remoteJid column.
     * @param dateCol The name of the column representing the date.
     * @param typeCol The column that contains the type to check against
     * @param type the desired type
     * @return The ResultSet. Never null.
     */
    ResultSet findLastByType(String tableName,
                             String localJidCol,
                             String localJid,
                             String remoteJidCol,
                             String dateCol,
                             String typeCol,
                             int type)
        throws SQLException;

    /**
     * Return a ResultSet containing the first record for each conversation (in
     * the given table) with the given local jid and type equal to the passed in
     * type.
     *
     * @param tableName The name of the table to query.
     * @param localJidCol The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidCol The name of the remoteJid column.
     * @param dateCol The name of the column representing the date.
     * @param typeCol The column that contains the type to check against
     * @param type the desired type
     * @return The ResultSet. Never null.
     */
    ResultSet findFirstByType(String tableName,
                              String localJidCol,
                              String localJid,
                              String remoteJidCol,
                              String dateCol,
                              String typeCol,
                              int type)
        throws SQLException;

    /**
     * Return a ResultSet containing the last count records for the given local
     * JID and remote JID . If count is -1, return all the rows.
     *
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJid The remote JID.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param count The maximum number of rows to return.
     * @return The ResultSet. Never null.
     */
    ResultSet findLast(String tableName,
                       String localJidColName,
                       String localJid,
                       String remoteJidColName,
                       String remoteJid,
                       String dateColName,
                       String idColName,
                       int count)
        throws SQLException;

    /**
     * Return a ResultSet containing the last count records for the given local
     * JID and remote JIDs. If count is -1, return all the rows.
     *
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJids The remote JIDs.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param count The maximum number of rows to return.
     * @return The ResultSet. Never null.
     */
    ResultSet findLast(String tableName,
                       String localJidColName,
                       String localJid,
                       String remoteJidColName,
                       List<String> remoteJids,
                       String dateColName,
                       String idColName,
                       int count)
        throws SQLException;

    /**
     * Return a ResultSet containing the first count records for the given local
     * JID and remote JID after the specified date.
     *
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJid The remote JID.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param date The date to use in the query (epoch time).
     * @param count The maximum number of rows to return.
     * @return The ResultSet. Never null.
     */
    ResultSet findFirstRecordsAfter(String tableName,
                                    String localJidColName,
                                    String localJid,
                                    String remoteJidColName,
                                    String remoteJid,
                                    String dateColName,
                                    String idColName,
                                    java.util.Date date,
                                    int count)
        throws SQLException;

    /**
     * Return a ResultSet containing the first count records for the given local
     * JID and remote JIDs after the specified date.
     *
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJids The remote JIDs.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param date The date to use in the query (epoch time).
     * @param count The maximum number of rows to return.
     * @return The ResultSet. Never null.
     */
    ResultSet findFirstRecordsAfter(String tableName,
                                    String localJidColName,
                                    String localJid,
                                    String remoteJidColName,
                                    List<String> remoteJids,
                                    String dateColName,
                                    String idColName,
                                    java.util.Date date,
                                    int count)
        throws SQLException;

    /**
     * Return a ResultSet containing the last count records for the given local
     * JID and remote JID before the specified date.
     *
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJids The remote JIDs.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param date The date to use in the query (epoch time).
     * @param count The maximum number of rows to return.
     * @return The ResultSet. Never null.
     */
    ResultSet findLastRecordsBefore(String tableName,
                                    String localJidColName,
                                    String localJid,
                                    String remoteJidColName,
                                    List<String> remoteJids,
                                    String dateColName,
                                    String idColName,
                                    java.util.Date date,
                                    int count)
        throws SQLException;

    /**
     * Return a ResultSet containing the records for the given room JID and any of the given msg UIDs.
     * @param tableName
     * @param roomJidColName
     * @param roomJid
     * @param msgUidColName
     * @param msgUids
     * @return The ResultSet. Never null.
     * @throws SQLException
     */
    ResultSet findMatchingGroupChatMsgUids(String tableName,
                                           String roomJidColName,
                                           String roomJid,
                                           String msgUidColName,
                                           List<String> msgUids)
    throws SQLException;

    /**
     * Return a ResultSet containing all records for the given local JID and
     * remote JID where the value of colName is colValue. Only for int values.
     *
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJid The remote JID.
     * @param dateColName The name of the date column.
     * @param idColName The name of the column for the message/file transfer ID
     * @param colName The name of the column to check.
     * @param colValue The value to match against.
     */
    ResultSet findLastByIntValue(String tableName,
                                 String localJidColName,
                                 String localJid,
                                 String remoteJidColName,
                                 String remoteJid,
                                 String dateColName,
                                 String idColName,
                                 String colName,
                                 int colValue)
        throws SQLException;

    /**
     * Return a ResultSet containing all records for the given local JID and
     * remote JID where the value of colName is colValue. Only for int values.
     *
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJid The remote JID.
     * @param dateColName The name of the date column.
     * @param idColName The name of the column for the message/file transfer ID
     * @param colName The name of the column to check.
     * @param colValue The value to match against.
     */
    ResultSet findFirstByIntValue(String tableName,
                                  String localJidColName,
                                  String localJid,
                                  String remoteJidColName,
                                  String remoteJid,
                                  String dateColName,
                                  String idColName,
                                  String colName,
                                  int colValue)
    throws SQLException;

    /**
     * Return a ResultSet containing all records for the given local JID and
     * remote JID where the value of colName is colValue. Only for String
     * values.
     *
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJid The remote JID.
     * @param colName The name of the column to check.
     * @param colValue The value to match against.
     */
    ResultSet findByStringValue(String tableName,
                                String localJidColName,
                                String localJid,
                                String remoteJidColName,
                                String remoteJid,
                                String colName,
                                String colValue)
        throws SQLException;

    /**
     * Return a ResultSet containing rows that match the specified keyword,
     * case insensitive.
     * One or more columns can be searched.
     *
     * @param tableName The name of the table to query.
     * @param orderColName The name of the column to order the results by.
     * @param keyword
     * @param searchColNames List of columns to search.
     * @param limit the number of records to return (-1 for all records).
     * @return The ResultSet.  Never null.
     */
    ResultSet findByKeyword(String tableName,
                            String orderColName,
                            String keyword,
                            String[] searchColNames,
                            int limit)
        throws SQLException;

    /**
     * Return a ResultSet containing records for the given local JID and remote
     * JID that match the specified keyword, case insensitive. One or more
     * columns can be searched.
     *
     * @param tableName The name of the table to query.
     * @param localJidColName The name of the localJid column
     * @param localJid The local JID.
     * @param remoteJidColName The name of the remoteJid column.
     * @param remoteJids The remote JIDs.
     * @param dateColName The name of the column representing the date.
     * @param idColName The name of the column for the message/file transfer ID
     * @param keywordColName The name of the column to use in the keyword query.
     * @param keyword The keyword to use in the keyword query.
     */
    ResultSet findByKeyword(String tableName,
                            String localJidColName,
                            String localJid,
                            String remoteJidColName,
                            List<String> remoteJids,
                            String dateColName,
                            String idColName,
                            String keywordColName,
                            String keyword)
        throws SQLException;

    /**
     * Return a ResultSet containing a list of unique values for the specified
     * column, and matching the given local JID string value.
     * @param tableName The name of the table to query.
     * @param matchColName The name of the localJid column.
     * @param matchValue The local JID.
     * @param uniquesColName The name of the column to return.
     */
    ResultSet findUniqueColumnValues(String tableName,
                                     String matchColName,
                                     String matchValue,
                                     String uniquesColName)
    throws SQLException;
}
