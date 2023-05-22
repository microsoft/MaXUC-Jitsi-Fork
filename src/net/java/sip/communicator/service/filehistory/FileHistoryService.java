/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.filehistory;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * File History Service stores info for file transfers from various protocols.
 *
 * @author Damian Minkov
 */
public interface FileHistoryService
{
    /**
     * Returns all the file transfers made after the given date
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param startDate Date the start date of the transfers
     * @return Collection of FileRecords
     */
    Collection<FileRecord> findByStartDate(
            MetaContact contact, Date startDate);

    /**
     * Returns all the file transfers made before the given date
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param endDate Date the end date of the transfers
     * @return Collection of FileRecords
     */
    Collection<FileRecord> findByEndDate(
            MetaContact contact, Date endDate);

    /**
     * Returns all the file transfers made between the given dates
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param startDate Date the start date of the transfers
     * @param endDate Date the end date of the transfers
     * @return Collection of FileRecords
     */
    Collection<FileRecord> findByPeriod(
            MetaContact contact, Date startDate, Date endDate);

    /**
     * Returns the supplied number of file transfers, in ascending date order.
     *
     * @param count filetransfer count
     * @return List, with each entry containing a list of FileRecords
     */
    List<List<FileRecord>> findLastForAll(int count);

    /**
     * Returns the supplied number of file transfers, in ascending date order.
     *
     * @param jid the receiver or sender of the file
     * @param count filetransfer count
     * @return List of FileRecords
     */
    List<FileRecord> findLastForJid(String jid, int count);

    /**
     * Returns the supplied number of file transfers
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param count filetransfer count
     * @return Collection of FileRecords
     */
    Collection<FileRecord> findLast(MetaContact contact, int count);

    /**
     * Returns all the file transfers having the given keyword in the filename
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param keyword keyword
     * @return Collection of FileRecords
     */
    Collection<FileRecord> findByKeyword(
            MetaContact contact, String keyword);

    /**
     * Returns the file transfer with the provided transfer id
     *
     * @param uid id of the transfer
     * @return FileRecord if it exists, null otherwise
     */
    FileRecord findByID(String uid);

    /**
     * Returns the supplied number of recent file transfers after the given date
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param date transfers after date
     * @param count transfers count
     * @return Collection of FileRecords
     */
    Collection<FileRecord> findFirstRecordsAfter(
            MetaContact contact, Date date, int count);

    /**
     * Returns the supplied number of recent file transfers before the given date
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param date transfers before date
     * @param count transfers count
     * @return Collection of FileRecords
     */
    Collection<FileRecord> findLastRecordsBefore(
            MetaContact contact, Date date, int count);

    /**
     * Removes the file transfer with the provided transfer id from the database
     *
     * @param uid id of the transfer
     */
    void removeByID(String uid);

    /**
     * Update the attention state of all file records with the associated jid
     *
     * @param jid the remote jid of the file records to update
     * @param attention the state to update the attention flag to
     */
    void updateAttentionState(String jid, boolean attention);
}
