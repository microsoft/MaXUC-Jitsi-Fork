// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.database;

import java.sql.*;
import java.util.*;

import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a connection to the database.
 */
public class DatabaseConnectionImpl implements DatabaseConnection
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog =
        Logger.getLogger(DatabaseConnectionImpl.class);

    /**
     * The JDBC Connection object for this connection.
     */
    private final Connection mConnection;

    /**
     * Constructs a new DBConnectionImpl object using the java.sql.Connection
     * object passed to it.
     *
     * @param connection A JDBC Connection.
     * @throws SQLException on SQL error.
     */
    public DatabaseConnectionImpl(Connection connection)
        throws SQLException
    {
        sLog.debug("(conn=" + connection.hashCode() + ")");
        mConnection = connection;
        // Always autocommit by default, unless the user explicitly requests a
        // transaction.
        mConnection.setAutoCommit(true);
    }

    @Override
    public ResultSet query(String sql)
        throws SQLException
    {
        return mConnection.createStatement().executeQuery(sql);
    }

    @Override
    public ResultSet query(PreparedStatement preparedStatement)
        throws SQLException
    {
        ResultSet resultSet = preparedStatement.executeQuery();
        preparedStatement.close();

        return resultSet;
    }

    @Override
    public int execute(String sql)
        throws SQLException
    {
        return mConnection.createStatement().executeUpdate(sql);
    }

    @Override
    public int execute(PreparedStatement preparedStatement)
        throws SQLException
    {
        return executeClose(preparedStatement);
    }

    @Override
    public int executeNoLog(PreparedStatement preparedStatement)
        throws SQLException
    {
        return executeClose(preparedStatement);
    }

    /**
     * Execute and close a prepared SQL update, returning the result.
     * @param preparedStatement A prepared SQL update statement.
     * @return The row count or zero, depending on the statement.
     * @throws SQLException on any SQL error.
     */
    private int executeClose(PreparedStatement preparedStatement)
        throws SQLException
    {
        int result = preparedStatement.executeUpdate();
        preparedStatement.close();

        return result;
    }

    @Override
    public int executeNoClose(PreparedStatement preparedStatement)
        throws SQLException
    {
           return preparedStatement.executeUpdate();
    }

    @Override
    public int executeNoLogNoClose(PreparedStatement preparedStatement)
        throws SQLException
    {
        return preparedStatement.executeUpdate();
    }

    @Override
    public PreparedStatement prepare(String sql)
        throws SQLException
    {
        return mConnection.prepareStatement(sql);
    }

    @Override
    public void startTransaction()
        throws SQLException
    {
        sLog.debug("(conn=" + mConnection.hashCode() + ")");
        // JDBC makes this easy - you just turn off auto commit.
        mConnection.setAutoCommit(false);
    }

    @Override
    public void commitTransaction()
        throws SQLException
    {
        sLog.debug("(conn=" + mConnection.hashCode() + ")");
        // Just pass through to JDBC and reinstate auto commit behaviour.
        mConnection.commit();
        mConnection.setAutoCommit(true);
    }

    @Override
    public void close()
        throws SQLException
    {
        sLog.debug("(conn=" + mConnection.hashCode() + ")");
        mConnection.close();
    }

    @Override
    public void log(String logString)
    {
        sLog.debug("(conn=" + mConnection.hashCode() + ") " + logString);
    }

    @Override
    public ResultSet findAfterDate(String tableName,
                                   String orderColName,
                                   String dateColName,
                                   java.util.Date date)
        throws SQLException
    {
        return findByDate(tableName, orderColName, dateColName, date, false);
    }

    @Override
    public ResultSet findAfterDate(String tableName,
                                   String localJidColName,
                                   String localJid,
                                   String remoteJidColName,
                                   String remoteJid,
                                   String dateColName,
                                   String idColName,
                                   java.util.Date date)
        throws SQLException
    {
        return findAfterDate(tableName, localJidColName, localJid,
            remoteJidColName, new ArrayList<>(Arrays.asList(remoteJid)),
            dateColName, idColName, date);
    }

    @Override
    public ResultSet findAfterDate(String tableName,
                                   String localJidColName,
                                   String localJid,
                                   String remoteJidColName,
                                   List<String> remoteJids,
                                   String dateColName,
                                   String idColName,
                                   java.util.Date date)
        throws SQLException
    {
        return findByDate(tableName, localJidColName, localJid, remoteJidColName, remoteJids, dateColName, idColName, date, false);
    }

    @Override
    public ResultSet findBeforeDate(String tableName,
                                    String orderColName,
                                    String dateColName,
                                    java.util.Date date)
        throws SQLException
    {
        return findByDate(tableName, orderColName, dateColName, date, true);
    }

    @Override
    public ResultSet findBeforeDate(String tableName,
                                    String localJidColName,
                                    String localJid,
                                    String remoteJidColName,
                                    String remoteJid,
                                    String dateColName,
                                    String idColName,
                                    java.util.Date date)
                                       throws SQLException
    {
        return findBeforeDate(tableName, localJidColName, localJid,
            remoteJidColName, new ArrayList<>(Arrays.asList(remoteJid)),
            dateColName, idColName, date);
    }

    @Override
    public ResultSet findBeforeDate(String tableName,
                                    String localJidColName,
                                    String localJid,
                                    String remoteJidColName,
                                    List<String> remoteJids,
                                    String dateColName,
                                    String idColName,
                                    java.util.Date date)
        throws SQLException
    {
        return findByDate(tableName, localJidColName, localJid, remoteJidColName, remoteJids, dateColName, idColName, date, true);
    }

    private ResultSet findByDate(String tableName,
                                 String orderColName,
                                 String dateColName,
                                 java.util.Date date,
                                 boolean before)
        throws SQLException
    {
        PreparedStatement preparedStatement = prepare(
        "SELECT * FROM " + tableName + " WHERE " + dateColName + (before ? "<" : ">") + "? ORDER BY " + orderColName + " DESC");
        preparedStatement.setLong(1, date.getTime());

        return query(preparedStatement);
    }

    private ResultSet findByDate(String tableName,
                                 String localJidColName,
                                 String localJid,
                                 String remoteJidColName,
                                 List<String> remoteJids,
                                 String dateColName,
                                 String idColName,
                                 java.util.Date date,
                                 boolean before)
          throws SQLException
     {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM " + tableName + " WHERE " + localJidColName + " =? AND " + remoteJidColName + " IN (?");

        for (int ii=1; ii < remoteJids.size(); ii++)
        {
            sb.append(",?");
        }
        sb.append(") AND " + dateColName + (before ? "<" : ">") + "? ORDER BY " + dateColName + ", " + idColName);

        PreparedStatement preparedStatement = prepare(sb.toString());
        preparedStatement.setString(1, localJid);
        int ii = 2;
        for (String jid : remoteJids)
        {
            preparedStatement.setString(ii, jid);
            ii++;
        }
        preparedStatement.setLong(ii, date.getTime());

        return query(preparedStatement);
    }

    @Override
    public ResultSet findByPeriod(String tableName,
                                  String orderColName,
                                  String dateColName,
                                  java.util.Date startDate,
                                  java.util.Date endDate)
        throws SQLException
    {
         PreparedStatement preparedStatement = prepare(
             "SELECT * FROM " + tableName +
             " WHERE " + dateColName + ">=? AND " + dateColName + " <? ORDER BY " +
             orderColName + " DESC ");

         preparedStatement.setLong(1, startDate.getTime());
         preparedStatement.setLong(2, endDate.getTime());

         return query(preparedStatement);
    }

    @Override
    public ResultSet findZeroLengthCallAtTimeForParticipant(String tableName,
                                                            String startDateColName,
                                                            String endDateColName,
                                                            String callPeerIdsColName,
                                                            java.util.Date startDate,
                                                            String callPeerIds)
            throws SQLException
    {
        PreparedStatement preparedStatement = prepare(
                "SELECT * FROM " + tableName +
                " WHERE " + startDateColName + "=? AND " + endDateColName
                + " =? AND " + callPeerIdsColName + "=?");

        preparedStatement.setLong(1, startDate.getTime());
        preparedStatement.setLong(2, startDate.getTime());
        preparedStatement.setString(3, callPeerIds);

        return query(preparedStatement);
    }

    @Override
    public ResultSet findByPeriod(String tableName,
                                  String localJidColName,
                                  String localJid,
                                  String remoteJidColName,
                                  String remoteJid,
                                  String dateColName,
                                  String idColName,
                                  java.util.Date startDate,
                                  java.util.Date endDate)
        throws SQLException
    {
        return findByPeriod(tableName, localJidColName, localJid,
            remoteJidColName, new ArrayList<>(Arrays.asList(remoteJid)),
            dateColName, idColName, startDate, endDate);
    }

    @Override
    public ResultSet findByPeriod(String tableName,
                                  String localJidColName,
                                  String localJid,
                                  String remoteJidColName,
                                  List<String> remoteJids,
                                  String dateColName,
                                  String idColName,
                                  java.util.Date startDate,
                                  java.util.Date endDate)
        throws SQLException
    {
         StringBuilder sb = new StringBuilder();
         sb.append("SELECT * FROM " + tableName + " WHERE " + localJidColName +
             " =? AND " + remoteJidColName + " IN (?");
         for (int ii=1; ii<remoteJids.size(); ii++)
         {
             sb.append(",?");
         }
         sb.append(") AND " + dateColName + ">=? AND " + dateColName +
             " <? ORDER BY " + dateColName + ", " + idColName);
         PreparedStatement preparedStatement = prepare(sb.toString());

         preparedStatement.setString(1, localJid);
         int ii = 2;
         for (String jid : remoteJids)
         {
             preparedStatement.setString(ii, jid);
             ii++;
         }
         preparedStatement.setLong(ii, startDate.getTime());
         preparedStatement.setLong(ii+1, endDate.getTime());
         return query(preparedStatement);
    }

    @Override
    public ResultSet findLast(String tableName,
                              String orderColName,
                              int count)
        throws SQLException
    {
        PreparedStatement preparedStatement;

        if (count != -1)
        {
            preparedStatement = prepare(
                "SELECT TOP " + count + " * FROM " + tableName + " ORDER BY " +
                orderColName + " DESC");
        }
        else
        {
            preparedStatement = prepare("SELECT * FROM " + tableName +
                " ORDER BY " + orderColName + " DESC");
        }

        return query(preparedStatement);
    }

    @Override
    public ResultSet findLast(String table,
                              String localJidCol,
                              String localJid,
                              String remoteJidCol,
                              String dateCol) throws SQLException
    {
        String constraint = "WHERE " + localJidCol + " = ? ";
        PreparedStatement statement =
             createFindXthQuery(table, remoteJidCol, dateCol, constraint, true);
        statement.setString(1, localJid);

        return query(statement);
    }

    @Override
    public ResultSet findLastByType(String table,
                                    String localJidCol,
                                    String localJid,
                                    String remoteJidCol,
                                    String dateCol,
                                    String typeCol,
                                    int type) throws SQLException
    {
        return findXthByType(table,
                             localJidCol,
                             localJid,
                             remoteJidCol,
                             dateCol,
                             typeCol,
                             type,
                             true);
    }

    @Override
    public ResultSet findFirstByType(String table,
                                     String localJidCol,
                                     String localJid,
                                     String remoteJidCol,
                                     String dateCol,
                                     String typeCol,
                                     int type) throws SQLException
    {
        return findXthByType(table,
                             localJidCol,
                             localJid,
                             remoteJidCol,
                             dateCol,
                             typeCol,
                             type,
                             false);
    }

    /**
     * Return a ResultSet containing the first or last record for each
     * conversation (in the given table) with the given local jid and type equal
     * to the passed in type.
     *
     * Returns the first or last depending on the value of the getLast flag.
     *
     * @param table The name of the table to query.
     * @param localJidCol The name of the localJid column.
     * @param localJid The local JID.
     * @param remoteJidCol The name of the remoteJid column.
     * @param dateCol The name of the column representing the date.
     * @param typeCol The column that contains the type to check against
     * @param type the desired type
     * @return The ResultSet. Never null.
     */
    private ResultSet findXthByType(String table,
                                    String localJidCol,
                                    String localJid,
                                    String remoteJidCol,
                                    String dateCol,
                                    String typeCol,
                                    int type,
                                    boolean getLast) throws SQLException
    {
        String constraint = "WHERE " + localJidCol + " =? AND " +
                                       typeCol + " =? ";
        PreparedStatement statement =
            createFindXthQuery(table, remoteJidCol, dateCol, constraint, getLast);

        statement.setString(1, localJid);
        statement.setInt(2, type);

        return query(statement);
    }

    /**
     * Create the SQL query required to get either the first, or the last message
     * for each remote person in the passed in table.
     *
     * @param table The table to query
     * @param remote The column containing the remote id
     * @param date The column containing the date
     * @param constraint The constraint to restrict the results
     * @param getLast True if we should get the last message. False gets first
     * @return The prepared statement.
     */
    private PreparedStatement createFindXthQuery(String table,
                                                 String remote,
                                                 String date,
                                                 String constraint,
                                                 boolean getLast)
                                                             throws SQLException
    {
        String fn = getLast ? "MAX" : "MIN";
        String sql =
           "SELECT a.* " +
           "FROM " + table + " a " +
           "INNER JOIN (" +
              "SELECT " + remote + ", " + fn + "(" + date + ") " + date + " " +
              "FROM " + table + " " +
              constraint +
              "GROUP BY " + remote +
           ") b ON a." + remote + " = b." + remote + " AND " +
                  "a." + date + " = b." + date;

        return prepare(sql);
    }

    @Override
    public ResultSet findLast(String tableName,
                              String localJidColName,
                              String localJid,
                              String remoteJidColName,
                              String remoteJid,
                              String dateColName,
                              String idColName,
                              int count)
        throws SQLException
    {
        return findLast(tableName, localJidColName, localJid, remoteJidColName,
                        new ArrayList<>(Arrays.asList(remoteJid)), dateColName,
                        idColName, count);
    }

    @Override
    public ResultSet findLast(String tableName,
                              String localJidColName,
                              String localJid,
                              String remoteJidColName,
                              List<String> remoteJids,
                              String dateColName,
                              String idColName,
                              int count)
        throws SQLException
    {
        PreparedStatement preparedStatement;
        String sql;

        StringBuilder remoteJidsConstraint = new StringBuilder(remoteJidColName);
        if (remoteJids.size() == 1)
        {
            remoteJidsConstraint.append(" =?");
        }
        else
        {
            remoteJidsConstraint.append(" IN (?");

            for (int ii=1; ii<remoteJids.size(); ii++)
            {
                remoteJidsConstraint.append(",?");
            }

            remoteJidsConstraint.append(")");
        }

        if (count != -1)
        {
            // First select the most recent 'count' rows, which requires
            // ordering in descending date order, then reverse the
            // order so the caller gets them in ascending date order.
            sql = "WITH BOTTOM AS " +
                      "(SELECT TOP " + count + " * " +
                       "FROM " + tableName + " " +
                       "WHERE " + localJidColName + " =? " +
                           "AND " + remoteJidsConstraint + " " +
                       "ORDER BY " + dateColName + " DESC, " + idColName + " DESC" +
                      ") " +
                  "SELECT * " +
                  "FROM BOTTOM " +
                  "ORDER BY " + dateColName + ", " + idColName;
        }
        else
        {
            sql = "SELECT * " +
                  "FROM " + tableName + " " +
                  "WHERE " + localJidColName + " =? " +
                      "AND " + remoteJidsConstraint + " " +
                  "ORDER BY " + dateColName + ", " + idColName;
        }

        preparedStatement = prepare(sql);
        preparedStatement.setString(1, localJid);

        int ii = 2;
        for (String remoteJid : remoteJids)
        {
            preparedStatement.setString(ii, remoteJid);
            ii++;
        }

        return query(preparedStatement);
    }

    @Override
    public ResultSet findFirstRecordsAfter(String tableName,
                                           String localJidColName,
                                           String localJid,
                                           String remoteJidColName,
                                           String remoteJid,
                                           String dateColName,
                                           String idColName,
                                           java.util.Date date,
                                           int count)
        throws SQLException
    {
        return findFirstRecordsAfter(tableName, localJidColName, localJid,
            remoteJidColName, new ArrayList<>(Arrays.asList(remoteJid)),
            dateColName, idColName, date, count);
    }

    @Override
    public ResultSet findFirstRecordsAfter(String tableName,
                                           String localJidColName,
                                           String localJid,
                                           String remoteJidColName,
                                           List<String> remoteJids,
                                           String dateColName,
                                           String idColName,
                                           java.util.Date date,
                                           int count)
        throws SQLException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT TOP " + count + " * FROM " + tableName + " WHERE " +
        localJidColName + " =? AND " + remoteJidColName + " IN (?");

        for (int ii=1; ii<remoteJids.size(); ii++)
        {
            sb.append(",?");
        }
        sb.append(") AND " + dateColName + ">? ORDER BY " + dateColName +
                                                           ", " + idColName);
        PreparedStatement preparedStatement = prepare(sb.toString());

        preparedStatement.setString(1, localJid);
        int ii = 2;
        for (String jid : remoteJids)
        {
            preparedStatement.setString(ii, jid);
            ii++;
        }
        preparedStatement.setLong(ii, date.getTime());

        return query(preparedStatement);
    }

    @Override
    public ResultSet findLastRecordsBefore(String tableName,
                                           String localJidColName,
                                           String localJid,
                                           String remoteJidColName,
                                           List<String> remoteJids,
                                           String dateColName,
                                           String idColName,
                                           java.util.Date date,
                                           int count)
        throws SQLException
    {
        StringBuilder sb = new StringBuilder();
        // First select the most recent 'count' rows, which requires
        // ordering in descending date order, then reverse the
        // order so the caller gets them in ascending date order.
        sb.append("WITH BOTTOM AS (SELECT TOP " + count + " * FROM " +
            tableName + " WHERE " + localJidColName + " =? AND " +
            remoteJidColName + " IN (?");

        for (int ii = 1; ii < remoteJids.size(); ii++)
        {
            sb.append(",?");
        }

        sb.append(") AND " + dateColName + "<? ORDER BY " +
            dateColName + " DESC, " + idColName + " DESC)" +
            " SELECT * FROM BOTTOM ORDER BY " + dateColName + ", " +
            idColName);

        PreparedStatement preparedStatement = prepare(sb.toString());

        preparedStatement.setString(1, localJid);

        int ii = 2;
        for (String remoteJid : remoteJids)
        {
            preparedStatement.setString(ii, remoteJid);
            ii++;
        }

        preparedStatement.setLong(ii, date.getTime());

        return query(preparedStatement);
    }

    /**
     *
     * @param tableName
     * @param roomJidColName
     * @param roomJid
     * @param msgUidColName
     * @param msgUids
     * @return
     * @throws SQLException
     */
    @Override
    public ResultSet findMatchingGroupChatMsgUids(String tableName,
                                                  String roomJidColName,
                                                  String roomJid,
                                                  String msgUidColName,
                                                  List<String> msgUids)
        throws SQLException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM " + tableName +
            " WHERE " + roomJidColName + " ='" + roomJid + "'" +
            " AND " + msgUidColName + " IN ('");
        sb.append(String.join("','", msgUids));
        sb.append("')");

        PreparedStatement preparedStatement = prepare(sb.toString());

        return query(preparedStatement);
    }

    @Override
    public ResultSet findLastByIntValue(String tableName,
                                        String localJidColName,
                                        String localJid,
                                        String remoteJidColName,
                                        String remoteJid,
                                        String dateColName,
                                        String idColName,
                                        String colName,
                                        int colValue)
    throws SQLException
    {
        PreparedStatement preparedStatement = prepare(
            "SELECT TOP 1 * FROM " + tableName + " WHERE " + localJidColName +
            " =? AND " + remoteJidColName + " =? AND " + colName + " =?" +
            " ORDER BY " + dateColName + " DESC, " + idColName + " DESC");
        preparedStatement.setString(1, localJid);
        preparedStatement.setString(2, remoteJid);
        preparedStatement.setInt(3, colValue);

        return query(preparedStatement);
    }

    @Override
    public ResultSet findFirstByIntValue(String tableName,
                                         String localJidColName,
                                         String localJid,
                                         String remoteJidColName,
                                         String remoteJid,
                                         String dateColName,
                                         String idColName,
                                         String colName,
                                         int colValue)
    throws SQLException
    {
        PreparedStatement preparedStatement = prepare(
            "SELECT TOP 1 * FROM " + tableName + " WHERE " + localJidColName +
            " =? AND " + remoteJidColName +
            " =? AND " + colName + " =?" + " ORDER BY " + dateColName + ", " +
            idColName);
        preparedStatement.setString(1, localJid);
        preparedStatement.setString(2, remoteJid);
        preparedStatement.setInt(3, colValue);

        return query(preparedStatement);
    }

    @Override
    public ResultSet findByStringValue(String tableName,
                                       String localJidColName,
                                       String localJid,
                                       String remoteJidColName,
                                       String remoteJid,
                                       String colName,
                                       String colValue)
    throws SQLException
    {
        PreparedStatement preparedStatement = prepare(
            "SELECT * FROM " + tableName + " WHERE " + localJidColName +
            " =? AND " + remoteJidColName +
            " =? AND " + colName + " =?");
        preparedStatement.setString(1, localJid);
        preparedStatement.setString(2, remoteJid);
        preparedStatement.setString(3, colValue);

        return query(preparedStatement);
    }

    @Override
    public ResultSet findByKeyword(String tableName,
                                   String localJidColName,
                                   String localJid,
                                   String remoteJidColName,
                                   List<String> remoteJids,
                                   String dateColName,
                                   String idColName,
                                   String keywordColName,
                                   String keyword)
        throws SQLException
    {
        StringBuilder sb = new StringBuilder();

        sb.append("SELECT * FROM " + tableName + " WHERE " + localJidColName +
            " =? AND " + remoteJidColName + " IN (?");

        for (int ii = 1; ii < remoteJids.size(); ii++)
        {
            sb.append(",?");
        }

        sb.append(")");

        if (!keyword.isEmpty())
        {
            sb.append(" AND (LOWER(" + keywordColName + ") LIKE ?)");
        }

        sb.append(" ORDER BY " + dateColName + " DESC, " + idColName +
                                                                      " DESC ");

        PreparedStatement preparedStatement = prepare(sb.toString());

        preparedStatement.setString(1, localJid);

        int ii = 2;
        for (String remoteJid : remoteJids)
        {
            preparedStatement.setString(ii, remoteJid);
            ii++;
        }

        if (!keyword.isEmpty())
        {
            preparedStatement.setString(ii, "%" + keyword.toLowerCase() + "%");
        }

        return query(preparedStatement);
    }

    @Override
    public ResultSet findByKeyword(String tableName,
                                   String orderColName,
                                   String keyword,
                                   String[] searchColNames,
                                   int limit)
        throws SQLException
    {
        boolean firstSearchCol = true;
        StringBuilder sb = new StringBuilder();

        if (limit != -1)
        {
            sb.append("SELECT TOP " + limit);
        }
        else
        {
            sb.append("SELECT");
        }

        sb.append(" * FROM " + tableName + " WHERE ");

        for (String searchColName : searchColNames)
        {
            if (!firstSearchCol)
            {
                sb.append(" OR ");
            }
            firstSearchCol = false;

            // Case-insensitive search.
            sb.append(" LOWER(" + searchColName + ") LIKE LOWER(?) ");
        }

        sb.append(" ORDER BY " + orderColName + " DESC");

        PreparedStatement preparedStatement = prepare(sb.toString());

        for (int i = 1; i <= searchColNames.length; i++)
        {
            preparedStatement.setString(i, "%" + keyword + "%");
        }

        return query(preparedStatement);
    }

    @Override
    public ResultSet findUniqueColumnValues(
        String tableName,
        String matchColName,
        String matchValue,
        String uniquesColName)
    throws SQLException
    {
        PreparedStatement preparedStatement = prepare(
            "SELECT DISTINCT " + uniquesColName + " FROM " + tableName + " WHERE " +
            matchColName + " =?");
        preparedStatement.setString(1, matchValue);

        return query(preparedStatement);
    }
}
