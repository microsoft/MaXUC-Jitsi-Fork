/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.filehistory;

import java.io.*;
import java.sql.*;
import java.util.Date;

import net.java.sip.communicator.service.database.schema.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Structure used for encapsulating data when writing or reading
 * File History Data.
 *
 * @author Damian Minkov
 */
public class FileRecord
{
    private final FileHistoryTable.DIRECTION mDirection;
    private final java.sql.Date mDate;
    private final java.util.Date mTimestamp;
    private final File mFile;
    private final FileHistoryTable.STATUS mStatus;
    private final Contact mContact;
    private final String mId;
    private final boolean mAttention;
    private final long mProgress;
    private final long mFileSize;

    /**
     * Constructs a new FileRecord.
     * @param resultSet The current row in the ResultSet contains the data to
     *        build a FileRecord.
     * @param contact The contact this FileRecord is to/from.
     * @throws SQLException on any SQL error.
     */
    public FileRecord(ResultSet resultSet, Contact contact)
        throws SQLException
    {
        this(resultSet,
             contact,
             resultSet.getInt(FileHistoryTable.COL_STATUS),
             new File(resultSet.getString(FileHistoryTable.COL_FILE)));
    }

    /**
     * Constructs a new FileRecord.
     * @param resultSet The current row in the ResultSet contains the data to
     *        build a FileRecord.
     * @param contact The contact this FileRecord is to/from.
     * @param status An integer value representing the STATUS enum from FileHistoryTable.
     *               1 is complete.
     * @param file The file this FileRecord refers to.
     * @throws SQLException on any SQL error.
     */
    public FileRecord(ResultSet resultSet, Contact contact, int status, File file)
        throws SQLException
    {
        this(contact,
             resultSet.getInt(FileHistoryTable.COL_DIR) == 0,
             new java.util.Date(resultSet.getLong(FileHistoryTable.COL_DATE)),
             file,
             FileHistoryTable.STATUS.values()[status],
             resultSet.getString(FileHistoryTable.COL_FT_ID),
             resultSet.getBoolean(FileHistoryTable.COL_ATTENTION),
             status == 1 ? file.length() : 0,
             file.length());
    }

    /**
     * Constructs a new FileRecord.
     * @param contact The contact this FileRecord is to/from.
     * @param isIncoming Whether the transfer is inbound
     * @param timestamp The timestamp of the FileRecord
     * @param file The file attached to this FileRecord
     * @param status The status of this FileRecord
     * @param id The id of this FileRecord
     */
    public FileRecord(Contact contact,
                      boolean isIncoming,
                      Date timestamp,
                      File file,
                      FileHistoryTable.STATUS status,
                      String id,
                      boolean attention,
                      long progress,
                      long fileSize)
    {
        mContact = contact;
        mDirection = isIncoming ? FileHistoryTable.DIRECTION.IN : FileHistoryTable.DIRECTION.OUT;
        mDate = new java.sql.Date(timestamp.getTime());
        mTimestamp = timestamp;
        mFile = file;
        mStatus = status;
        mId = id;
        mAttention = attention;
        mProgress = progress;
        mFileSize = fileSize;
    }

    /**
     * The direction of the transfer.
     * @return true if inbound
     */

    public boolean isInbound()
    {
        return mDirection == FileHistoryTable.DIRECTION.IN;
    }

    /**
     * The date of the record.
     * @return the date
     */
    public java.sql.Date getDate()
    {
        return mDate;
    }

    /**
     * The date of the record.
     * @return the date
     */
    public java.util.Date getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * The file that was transfered.
     * @return the file
     */
    public File getFile()
    {
        return mFile;
    }

    /**
     * The status of the transfer.
     * @return the status
     */
    public FileHistoryTable.STATUS getStatus()
    {
        return mStatus;
    }

    /**
     * The status of the transfer, as a string for display.
     * @return the status
     */
    public String getStatusString()
    {
        return mStatus.toString();
    }

    /**
     * The contact.
     * @return the contact
     */
    public Contact getContact()
    {
        return mContact;
    }

    /**
     * The id.
     * @return id.
     */
    public String getID()
    {
        return mId;
    }

    /**
     * The attention state - true if the user hasn't seen the transfer yet.
     * @return attention.
     */
    public boolean getAttention()
    {
        return mAttention;
    }

    /**
     * The number of bytes transferred
     * Equal to the file size for complete, 0B for pending
     * @return transferred bytes
     */
    public long getProgress()
    {
        return mProgress;
    }

    /**
     * The size of the entire file in bytes
     */
    public long getFileSize()
    {
        return mFileSize;
    }
}
