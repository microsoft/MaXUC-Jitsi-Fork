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
 * HistoryService (XML) to the DatabaseService for Group IM histories.
 */
public class DatabaseMigrationGroupMessageHistory extends DatabaseMigration
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog = Logger.getLogger(DatabaseMigrationMessageHistory.class);

    /**
     * @param connection The DatabaseConnection.
     */
    public DatabaseMigrationGroupMessageHistory(FileAccessService fileAccessService,
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
                String roomJid = getRoomJidFromDirectoryName(directory.getName());

                if (roomJid != null)
                {
                    processXmlFile(file, localJid, roomJid);
                }
            }
            else
            {
                sLog.info("Ignore (can't extract local JID): " + file);
            }
        }
    }

    /**
     * Read one XML record and write it to the database.
     * @param node The XML node to process.
     * @param localJid The local JID.
     * @param remoteJid The remote JID (room JID in this case).
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

        // Cautious approach in case any field is missing.
        String dir = valueFromElement(node, "dir");
        String msg = valueFromElement(node, "msg");
        String uid = valueFromElement(node, "uid");
        String peer = valueFromElement(node, "peer");
        String receivedTimestamp = valueFromElement(node, "receivedTimestamp");
        String isRead = valueFromElement(node, "isRead");
        String msgType = valueFromElement(node, "type");
        String isFailed = valueFromElement(node, "isFailed");
        String isClosed = valueFromElement(node, "isClosed");
        String subject = valueFromElement(node, "subject");

        // We introduced a limit on subject length after introducing Group IM,
        // so there might be some with a longer subject.
        subject = truncateString(subject, Chat.MAX_CHAT_ROOM_SUBJECT_LENGTH);

        // We have seen some clients with longer than allowed messages.  We
        // don't know how they got there, but one possibility is a non-Accession
        // client because we only police the length when sending.
        msg = truncateString(msg, Chat.MAX_CHAT_MESSAGE_LENGTH);

        // ...convert to the right types for the database and add the record.
        addGroupMessageEntry(
            localJid,
            peer.toLowerCase(),
            remoteJid,
            dir.equals("in") ? GroupMessageHistoryTable.DIRECTION.IN :
                               GroupMessageHistoryTable.DIRECTION.OUT,
            msg,
            uid,
            xmlTimestampToDatabaseTimestamp(receivedTimestamp),
            groupMessageTypeStringToEnum(msgType),
            Boolean.parseBoolean(isRead),
            Boolean.parseBoolean(isFailed),
            Boolean.parseBoolean(isClosed),
            subject);
    }

    /**
     * Build the prepared statements once only.
     */
    protected void buildPreparedStatement()
        throws SQLException
    {
        mPreparedStatement =
            mConnection.prepare("INSERT INTO " + GroupMessageHistoryTable.NAME +
            "(" + GroupMessageHistoryTable.COL_LOCAL_JID + "," +
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
            GroupMessageHistoryTable.COL_SUBJECT + ")  VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
    }

    /**
     * Add a new 1-1 IM entry into the database.
     * @param localJid The JID of the local party.
     * @param senderJid The JID of the sender.  Can be the same as localJid.
     * @param roomJid The chatroom JID.
     * @param direction
     * @param msg
     * @param msgid
     * @param receivedTimestamp
     * @param msgType
     * @param isRead
     * @param isFailed
     * @param isClosed
     * @param subject
     */
    private void addGroupMessageEntry(String localJid,
                                      String senderJid,
                                      String roomJid,
                                      GroupMessageHistoryTable.DIRECTION direction,
                                      String msg,
                                      String msgid,
                                      long receivedTimestamp,
                                      GroupMessageHistoryTable.TYPE msgType,
                                      boolean isRead,
                                      boolean isFailed,
                                      boolean isClosed,
                                      String subject)
        throws SQLException
    {
        // We can't log the actual statement as it contains the full text
        // of the message. Instead, log a copy of the statement with the
        // text removed.
        mConnection.log(DatabaseUtils.createInsertGroupMessageLogString(
            localJid,
            senderJid,
            roomJid,
            direction.ordinal(),
            msgid,
            receivedTimestamp,
            msgType.ordinal(),
            isRead,
            isClosed,
            subject));

        mPreparedStatement.setString(1, localJid);
        mPreparedStatement.setString(2, senderJid);
        mPreparedStatement.setString(3, roomJid);
        mPreparedStatement.setInt(4, direction.ordinal());
        mPreparedStatement.setString(5, msg);
        mPreparedStatement.setString(6, msgid);
        mPreparedStatement.setLong(7, receivedTimestamp);
        mPreparedStatement.setInt(8, msgType.ordinal());
        mPreparedStatement.setBoolean(9, isRead);
        mPreparedStatement.setBoolean(10, isFailed);
        mPreparedStatement.setBoolean(11, isClosed);
        mPreparedStatement.setString(12, subject);

        mConnection.executeNoLogNoClose(mPreparedStatement);
    }

    private GroupMessageHistoryTable.TYPE groupMessageTypeStringToEnum(String status)
    {
        GroupMessageHistoryTable.TYPE enumType;

        if (status.equals("groupchat"))
        {
            enumType = GroupMessageHistoryTable.TYPE.GROUP_IM;
        }
        else
        {
            // Catches "status" and anything unexpected.
            enumType = GroupMessageHistoryTable.TYPE.STATUS;
        }

        return enumType;
    }

    /**
     * Take a directory name and extract the room JID from it.
     * @param directoryName
     * @return The room JID, or null if directory does not represent a chatroom.
     */
    private String getRoomJidFromDirectoryName(String directoryName)
    {
        String roomJid = null;

        // We expect a directoryName of this form:
        // chatroom-xxxxxxxx@AMS-DOMAIN.COM@AMS-DOMAIN.COM
        if (directoryName.startsWith(CHATROOM_PREFIX))
        {
            // The hashcode hasn't been seen the way it is with 1-1, but
            // attempt to remove it just in case.
            String tempString = directoryName.split("$")[0];

            String[] splitParts = tempString.split("@");

            if (splitParts.length >= 2)
            {
                tempString = splitParts[0] + "@" + splitParts[1];

                // We expect lower case but need to cope with old bugs where it
                // might not have been.
                roomJid = tempString.toLowerCase();
            }
        }

        return roomJid;
    }
}
