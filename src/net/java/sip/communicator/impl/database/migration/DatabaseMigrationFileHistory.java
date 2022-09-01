// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.database.migration;

import java.io.*;
import java.sql.*;

import org.jitsi.service.fileaccess.*;
import org.w3c.dom.*;

import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.database.schema.*;
import net.java.sip.communicator.util.*;

/**
 * Implements one-off migration of all stored data from the obsolete
 * HistoryService (XML) to the DatabaseService for the File Transfer histories.
 */
public class DatabaseMigrationFileHistory extends DatabaseMigration
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog = Logger.getLogger(DatabaseMigrationFileHistory.class);

    /**
     * @param connection The DatabaseConnection.
     */
    public DatabaseMigrationFileHistory(FileAccessService fileAccessService,
                                        DatabaseConnection connection)
    {
        super(fileAccessService, connection);
    }

    /**
     */
    protected void populateDatabaseFromOldXmlFiles()
    {
        // All FileHistory records are under here.
        File histDir = new File(mHistoryDir, "filehistory" + File.separator +
            "default");

        for (File file : getXmlFilesToProcess(histDir))
        {
            File directory = file.getParentFile();
            String remoteJid = directory.getName().toLowerCase();
            String localJid = getLocalJidFromDirectoryName(
                directory.getParentFile().getName());

            if (localJid != null)
            {
                processXmlFile(file, localJid, remoteJid);
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
        // Cautious approach in case any field is missing.
        String id = valueFromElement(node, "id");
        String file = valueFromElement(node, "file");
        String dir = valueFromElement(node, "dir");
        String date = valueFromElement(node, "date");
        String status = valueFromElement(node, "status");

        // ...convert to the right types for the database and add the record.
        addFileTransferEntry(
            localJid,
            remoteJid,
            id,
            file,
            dir.equals("in") ? FileHistoryTable.DIRECTION.IN : FileHistoryTable.DIRECTION.OUT,
            xmlTimestampToDatabaseTimestamp(date),
            fileTransferStatusStringToEnum(status));
    }

    /**
     * Build the prepared statement once only.
     */
    protected void buildPreparedStatement()
        throws SQLException
    {
        mPreparedStatement = mConnection.prepare("INSERT INTO " + FileHistoryTable.NAME +
            "(" + FileHistoryTable.COL_LOCAL_JID + "," +
            FileHistoryTable.COL_REMOTE_JID + "," +
            FileHistoryTable.COL_FT_ID + "," +
            FileHistoryTable.COL_FILE + "," +
            FileHistoryTable.COL_DIR + "," +
            FileHistoryTable.COL_DATE + "," +
            FileHistoryTable.COL_STATUS + ")  VALUES (?,?,?,?,?,?,?)");
    }

    /**
     * Add a new File Transfer entry into the database.
     * @param localJid The JID of the local party.
     * @param remoteJid The JID of the other party.
     * @param ftid ID of a FileTransfer object.
     * @param filename To set in the database.
     * @param direction To set in the database.
     * @param date To set in the database.
     * @param status To set in the database.
     */
    private void addFileTransferEntry(String localJid,
                                      String remoteJid,
                                      String ftid,
                                      String filename,
                                      FileHistoryTable.DIRECTION direction,
                                      long date,
                                      FileHistoryTable.STATUS status)
        throws SQLException
    {
        mPreparedStatement.setString(1, localJid);
        mPreparedStatement.setString(2, remoteJid);
        mPreparedStatement.setString(3, ftid);
        mPreparedStatement.setString(4, filename);
        mPreparedStatement.setInt(5, direction.ordinal());
        mPreparedStatement.setLong(6, date);
        mPreparedStatement.setInt(7, status.ordinal());

        mConnection.executeNoClose(mPreparedStatement);
    }

    private FileHistoryTable.STATUS fileTransferStatusStringToEnum(String status)
    {
        FileHistoryTable.STATUS enumStatus;

        switch (status)
        {
            case "active":
                enumStatus = FileHistoryTable.STATUS.ACTIVE;
                break;
            case "completed":
                enumStatus = FileHistoryTable.STATUS.COMPLETED;
                break;
            case "canceled":
                enumStatus = FileHistoryTable.STATUS.CANCELED;
                break;
            case "refused":
                enumStatus = FileHistoryTable.STATUS.REFUSED;
                break;
            default:
                // Catches "failed" and anything unexpected.
                enumStatus = FileHistoryTable.STATUS.FAILED;
                break;
        }

        return enumStatus;
    }
}
