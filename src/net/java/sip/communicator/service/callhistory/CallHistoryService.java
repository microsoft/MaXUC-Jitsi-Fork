/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.callhistory;

import java.util.*;

import net.java.sip.communicator.impl.callhistory.network.NetworkCallHistoryDataHandler;
import net.java.sip.communicator.service.callhistory.event.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * The Call History Service stores info about calls made from various protocols
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public interface CallHistoryService
{
    /**
     * The name of the key used to store the direction of call history items.
     */
    String CALL_HISTORY_DIRECTION_KEY = "direction";

    /**
     * Returns all the calls made by all the contacts
     * in the supplied <tt>contact</tt> after the given date.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param startDate Date the start date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    Collection<CallRecord> findByStartDate(MetaContact contact,
                                           Date startDate)
        throws RuntimeException;

    /**
     * Returns all the calls made by all the contacts
     * in the supplied <tt>contact</tt> before the given date.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    Collection<CallRecord> findByEndDate(MetaContact contact,
                                         Date endDate)
        throws RuntimeException;

    /**
     * Returns all the calls made by all the contacts
     * in the supplied <tt>contact</tt> between the given dates.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    Collection<CallRecord> findByPeriod(MetaContact contact,
                                        Date startDate,
                                        Date endDate)
        throws RuntimeException;

    /**
     * Returns all the calls added to the DB strictly after the given date.
     *
     * @param startDate
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    Collection<CallRecord> findCallsAddedToDbAfter(Date startDate)
        throws RuntimeException;

    /**
     * Returns all the calls made before the given date.
     *
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    Collection<CallRecord> findByEndDate(Date endDate)
        throws RuntimeException;

    /**
     * Returns all the calls made between the given dates.
     *
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    Collection<CallRecord> findByPeriod(Date startDate, Date endDate)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent calls made by all the contacts
     * in the supplied <tt>contact</tt>.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param count calls count
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    Collection<CallRecord> findLast(MetaContact contact, int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent calls.
     * If count is -1, return all calls.
     *
     * @param count calls count
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    Collection<CallRecord> findLast(int count)
        throws RuntimeException;

    /**
     * Find the calls made by the supplied peer address
     * @param address String the address of the peer
     * @param recordCount the number of records to return
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    Collection<CallRecord> findByPeer(String address, int recordCount)
        throws RuntimeException;

    /**
     * Find the calls made by the supplied call record UID.
     *
     * @param uid String the UID of the call record
     * @return CallRecord with CallPeerRecord
     * @throws RuntimeException
     */
    CallRecord findByUid(String uid)
        throws RuntimeException;

    /**
     * Update the call attention state.
     *
     * @param record The updated call record to update in the DB
     * @throws RuntimeException
     */
    void updateAttentionState(CallRecord record)
        throws RuntimeException;

    /**
     * Adding change listener for monitoring when call history changes
     *
     * @param listener the call history change listener to register
     */
    void addCallHistoryChangeListener(
            CallHistoryChangeListener listener);

    /**
     * Remove a registered change listener.
     *
     * @param listener the call history change listener to remove
     */
    void removeCallHistoryChangeListener(
            CallHistoryChangeListener listener);

    /**
     * Retrieves the NetworkCallHistoryHandler attribute
     */
     NetworkCallHistoryDataHandler getNetworkCallHistoryHandler();
}
