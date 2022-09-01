// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.database.migration;

import java.io.*;
import java.sql.*;

import org.jitsi.service.fileaccess.*;
import org.w3c.dom.*;

import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.database.schema.*;
import net.java.sip.communicator.service.database.util.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * Implements one-off migration of all stored data from the obsolete
 * HistoryService (XML) to the DatabaseService for 1-1 IM histories.
 */
public class DatabaseMigrationMessageHistory extends DatabaseMigration
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog = Logger.getLogger(DatabaseMigrationMessageHistory.class);

    /**
     * The old HistoryService used this value when the SMPP ID column didn't
     * apply.  We don't need it because we simply don't store a value in the
     * database if we don't have an SMPP ID.
     */
    private static final String FAKESMPP_ID = "fakesmpp_id";

    /**
     * @param connection The DatabaseConnection.
     */
    public DatabaseMigrationMessageHistory(FileAccessService fileAccessService,
                                           DatabaseConnection connection)
    {
        super(fileAccessService, connection);
    }

    /**
     */
    protected void populateDatabaseFromOldXmlFiles()
    {
        // All MessageHistory records are under here.
        File histDir = new File(mHistoryDir, "messages" + File.separator +
            "default");

        for (File file : getXmlFilesToProcess(histDir))
        {
            File directory = file.getParentFile();
            String localJid = getLocalJidFromDirectoryName(
                directory.getParentFile().getName());

            if (localJid != null)
            {
                String remoteJid = directory.getName().toLowerCase();

                if (!remoteJid.startsWith(CHATROOM_PREFIX))
                {
                    processXmlFile(file, localJid, remoteJid);
                }
            }
            else
            {
                sLog.info("Ignore (can't extract local JID): " + directory);
            }
        }
    }

    /**
     * Read one XML record and write it to the database.
     * @param node The XML node to process.
     * @param localJid The local JID.
     * @param remoteJid The remote JID.
     */
    protected void processXmlRecord(Element node,
                                    String localJid,
                                    String remoteJid)
        throws SQLException
    {
        // Get what we want from xml file.
        // Ignore these fields:
        // - msgType because always text/plain.
        // - enc because always UTF-8.
        // - isClosed because always false because Group IM only.
        // - subject because dummy value.

        // Cautious approach in case any field is missing.
        String dir = valueFromElement(node, "dir");
        String msg = valueFromElement(node, "msg");
        String uid = valueFromElement(node, "uid");
        String receivedTimestamp = valueFromElement(node, "receivedTimestamp");
        String isRead = valueFromElement(node, "isRead");
        String msgType = valueFromElement(node, "type");
        String isFailed = valueFromElement(node, "isFailed");
        String smppid = valueFromElement(node, "smppid");

        // We have seen some clients with longer than allowed messages.  We
        // don't know how they got there, but one possibility is a non-Accession
        // client because we only police the length when sending.
        msg = truncateString(msg, Chat.MAX_CHAT_MESSAGE_LENGTH);

        // ...convert to the right types for the database and add the record.
        addMessageEntry(
            localJid,
            remoteJid,
            dir.equals("in") ? MessageHistoryTable.DIRECTION.IN :
                               MessageHistoryTable.DIRECTION.OUT,
            msg,
            uid,
            xmlTimestampToDatabaseTimestamp(receivedTimestamp),
            messageTypeStringToEnum(msgType),
            Boolean.parseBoolean(isRead),
            Boolean.parseBoolean(isFailed),
            smppid);
    }

    /**
     * Build the prepared statements once only.
     */
    protected void buildPreparedStatement()
        throws SQLException
    {
        mPreparedStatement =
            mConnection.prepare("INSERT INTO " + MessageHistoryTable.NAME +
            "(" + MessageHistoryTable.COL_LOCAL_JID + "," +
            MessageHistoryTable.COL_REMOTE_JID + "," +
            MessageHistoryTable.COL_DIR + "," +
            MessageHistoryTable.COL_TEXT + "," +
            MessageHistoryTable.COL_MSG_ID + "," +
            MessageHistoryTable.COL_RECEIVED_TIMESTAMP + "," +
            MessageHistoryTable.COL_TYPE + "," +
            MessageHistoryTable.COL_READ + "," +
            MessageHistoryTable.COL_FAILED + "," +
            MessageHistoryTable.COL_SMPP_ID + ")  VALUES (?,?,?,?,?,?,?,?,?,?)");
    }

    /**
     * Add a new 1-1 IM entry into the database.
     * @param localJid The JID of the local party.
     * @param remoteJid The JID of the other party.
     * @param direction
     * @param msg
     * @param msgid
     * @param receivedTimestamp
     * @param msgType
     * @param isRead
     * @param isFailed
     * @param smppid
     */
    private void addMessageEntry(String localJid,
                                 String remoteJid,
                                 MessageHistoryTable.DIRECTION direction,
                                 String msg,
                                 String msgid,
                                 long receivedTimestamp,
                                 MessageHistoryTable.TYPE msgType,
                                 boolean isRead,
                                 boolean isFailed,
                                 String smppid)
        throws SQLException
    {
        // We can't log the actual statement as it contains the full text
        // of the message. Instead, log a copy of the statement with the
        // text removed.
        mConnection.log(DatabaseUtils.createInsertMessageLogString(
            localJid,
            remoteJid,
            direction.ordinal(),
            msgid,
            receivedTimestamp,
            msgType.ordinal(),
            isRead));

        mPreparedStatement.setString(1, localJid);
        mPreparedStatement.setString(2, remoteJid);
        mPreparedStatement.setInt(3, direction.ordinal());
        mPreparedStatement.setString(4, msg);
        mPreparedStatement.setString(5, msgid);
        mPreparedStatement.setLong(6, receivedTimestamp);
        mPreparedStatement.setInt(7, msgType.ordinal());
        mPreparedStatement.setBoolean(8, isRead);
        mPreparedStatement.setBoolean(9, isFailed);
        mPreparedStatement.setString(10,
            smppid.equals(FAKESMPP_ID) ? null : smppid);

        mConnection.executeNoLogNoClose(mPreparedStatement);
    }

    private MessageHistoryTable.TYPE messageTypeStringToEnum(String status)
    {
        MessageHistoryTable.TYPE enumType;

        if (status.equals("sms"))
        {
            enumType = MessageHistoryTable.TYPE.SMS;
        }
        else
        {
            // Catches "im" and anything unexpected.
            enumType = MessageHistoryTable.TYPE.IM;
        }

        return enumType;
    }
}
