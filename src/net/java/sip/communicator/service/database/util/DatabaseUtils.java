// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.util;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import java.sql.*;

import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.database.schema.*;

/**
 * Utility class that exposes a method to make life easier.
 */
public class DatabaseUtils
{
    /** Strings to use to replace message text in logs. */
    private static final String DUMMY_MESSAGE_TEXT = "XXX";
    private static final String DUMMY_SUBJECT = "YYY";

    /**
     * Convenience method to simplify code for closing a DatabaseConnection.
     * @param connection The connection to close.  Safe to call if null.
     */
    public static void safeClose(DatabaseConnection connection)
    {
        try
        {
            if (connection != null)
            {
                connection.close();
            }
        }
        catch (SQLException e)
        {
        }
    }

    /**
     * Convenience method to simplify code for closing a DatabaseConnection.
     * @param resultSet The ResultSet to close.  Safe to call if null.
     */
    public static void safeClose(ResultSet resultSet)
    {
        try
        {
            if (resultSet != null)
            {
                resultSet.close();
            }
        }
        catch (SQLException e)
        {
        }
    }

    /**
     * Convenience method to simplify code for closing a DatabaseConnection
     * and a ResultSet.
     *
     * According to http://hsqldb.org/doc/src/org/hsqldb/jdbc/JDBCResultSet.html,
     * - A ResultSet persists after closing a connection so it is important that
     *   we explicitly close each ResultSet.
     * - A ResultSet is automatically closed when the statement object that
     *   generated it is closed or executed again.
     *
     * @param connection The connection to close.  Safe to call if null.
     * @param resultSet The ResultSet to close.  Safe to call if null.
     */
    public static void safeClose(DatabaseConnection connection,
                                 ResultSet resultSet)
    {
        try
        {
            if (resultSet != null)
            {
                resultSet.close();
            }
        }
        catch (SQLException e)
        {
        }

        safeClose(connection);
    }

    /**
     * Convenience method to simplify code for closing a DatabaseConnection
     * and a ResultSet.
     * This is necessary, as some methods only return the connection to the
     * pool rather than actually closing it, so the prepared statement would
     * not be closed.
     * @param connection The connection to close.  Safe to call if null.
     * @param preparedStatement The PreparedStatement to close.  Safe to call
     * if null.
     */
    public static void safeClose(DatabaseConnection connection,
                                 PreparedStatement preparedStatement)
    {
        try
        {
            if (preparedStatement != null)
            {
                preparedStatement.close();
            }
        }
        catch (SQLException e)
        {
        }

        safeClose(connection);
    }

    /**
     * Create the string to use when logging INSERT statements for one-to-one
     * messages. The contents of messages must not be written to the log.
     *
     * @param accountJid account JID
     * @param remoteJid remote JID
     * @param dir direction of the message
     * @param msgUid message ID
     * @param timestamp message timestamp
     * @param type message type
     * @param read whether the message is read
     * @return the string to use in the log
     */
    public static String createInsertMessageLogString(String accountJid,
        String remoteJid, int dir, String msgUid, Long timestamp, int type,
        boolean read)
    {
        return "[INSERT INTO " + MessageHistoryTable.NAME + "(" +
                                 MessageHistoryTable.COL_LOCAL_JID + "," +
                                 MessageHistoryTable.COL_REMOTE_JID + "," +
                                 MessageHistoryTable.COL_DIR + "," +
                                 MessageHistoryTable.COL_TEXT + "," +
                                 MessageHistoryTable.COL_MSG_ID + "," +
                                 MessageHistoryTable.COL_RECEIVED_TIMESTAMP + "," +
                                 MessageHistoryTable.COL_TYPE + "," +
                                 MessageHistoryTable.COL_READ + "," +
                                 MessageHistoryTable.COL_FAILED + ") VALUES (" +
                                 sanitisePeerId(accountJid) + "," + sanitisePeerId(remoteJid) + "," + dir + "," +
                                 DUMMY_MESSAGE_TEXT + "," + msgUid + "," +
                                 timestamp + "," + type + "," + read + ",false)]";
    }

    /**
     * Create the string to use when logging INSERT statements for one-to-one
     * messages. The contents of messages must not be written to the log.
     *
     * @param accountJid account JID
     * @param senderJid sender JID
     * @param roomJid chat room JID
     * @param dir direction of the message
     * @param msgUid message ID
     * @param timestamp message timestamp
     * @param type message type
     * @param read whether the message is read
     * @param closed whether the message is closed
     * @param subject message subject
     * @return the string to use in the log
     */
    public static String createInsertGroupMessageLogString(String accountJid,
        String senderJid, String roomJid, int dir, String msgUid, Long timestamp,
        int type, boolean read, boolean closed, String subject)
    {
        return "[INSERT INTO " + GroupMessageHistoryTable.NAME + "(" +
                         GroupMessageHistoryTable.COL_LOCAL_JID + "," +
                         GroupMessageHistoryTable.COL_SENDER_JID + "," +
                         GroupMessageHistoryTable.COL_ROOM_JID + "," +
                         GroupMessageHistoryTable.COL_DIR + "," +
                         GroupMessageHistoryTable.COL_TEXT + "," +
                         GroupMessageHistoryTable.COL_MSG_ID + "," +
                         GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP + "," +
                         GroupMessageHistoryTable.COL_TYPE + "," +
                         GroupMessageHistoryTable.COL_READ + "," +
                         GroupMessageHistoryTable.COL_FAILED + "," +
                         GroupMessageHistoryTable.COL_LEFT + "," +
                         GroupMessageHistoryTable.COL_SUBJECT + ") VALUES (" +
                         sanitisePeerId(accountJid) + "," + sanitisePeerId(senderJid) + "," + sanitiseChatRoom(roomJid) + "," + dir +
                         "," + DUMMY_MESSAGE_TEXT + "," + msgUid + "," +
                         timestamp + "," + type + "," + read + ",false," +
                         closed + "," + DUMMY_SUBJECT + ")]";
    }

    /**
     * Create the string to use when logging INSERT statements for file transfer
     * messages. The file path must not be written to the log.
     *
     * @param accountJid account JID
     * @param remoteJid remote JID
     * @param ftUid transfer ID
     * @param dir direction of the transfer
     * @param timestamp transfer timestamp
     * @param status status of the file transfer
     * @param read whether the transfer has been seen
     * @return the string to use in the log
     */
    public static String createInsertTransferLogString(String accountJid,
        String remoteJid, String ftUid, int dir, Long timestamp, int status,
        boolean attention)
    {
        return "[INSERT INTO " + FileHistoryTable.NAME + "(" +
                                 FileHistoryTable.COL_LOCAL_JID + "," +
                                 FileHistoryTable.COL_REMOTE_JID + "," +
                                 FileHistoryTable.COL_FT_ID + "," +
                                 FileHistoryTable.COL_FILE + "," +
                                 FileHistoryTable.COL_DIR + "," +
                                 FileHistoryTable.COL_DATE + "," +
                                 FileHistoryTable.COL_STATUS + "," +
                                 FileHistoryTable.COL_ATTENTION + ")  VALUES (" +
                                 sanitisePeerId(accountJid) + "," + sanitisePeerId(remoteJid) + "," + ftUid + "," +
                                 DUMMY_MESSAGE_TEXT + "," + dir + "," + timestamp + "," + status + "," + attention + ")]";
    }
}
