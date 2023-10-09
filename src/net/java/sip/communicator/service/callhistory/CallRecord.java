/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.callhistory;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Structure used for encapsulating data when writing or reading
 * Call History Data. Also these records are used for returning data
 * from the Call History Service.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public interface CallRecord
{
    /**
     * The outgoing call direction.
     */
    String OUT = "out";

    /**
     * The incoming call direction.
     */
    String IN = "in";

    /**
     * Sets the given UID to be the UID of this record
     * @param uid the UID to set
     */
    void setUid(String uid);

    /**
     * @return the UID of this record
     */
    String getUid();

    /**
     * The source call which this record servers
     * @param sourceCall Call
     */
    void setSourceCall(Call sourceCall);

    /**
     * The Call source of this record
     * @return Call
     */
    Call getSourceCall();

    /**
     * Finds a CallPeer with the supplied address
     *
     * @param address String
     * @return CallPeerRecord
     */
    CallPeerRecord findPeerRecord(String address);

    /**
     * Return Vector of CallPeerRecords
     * @return Vector
     */
    List<CallPeerRecord> getPeerRecords();

    /**
     * Sets the direction of the call.
     * IN or OUT
     * @param direction String
     */
    void setDirection(String direction);

    /**
     * Returns the direction of the call
     * IN or OUT
     * @return String
     */
    String getDirection();

    /**
     * Set the time when the call finishes
     * If some peer has no end Time set we set it also
     * @param endTime Date
     */
    void setEndTime(Date endTime);

    /**
     * Returns the time when the call has finished
     * @return Date
     */
    Date getEndTime();

    /**
     * Sets the time when the call begins
     * @param startTime Date
     */
    void setStartTime(Date startTime);

    /**
     * The time when the call has began
     * @return Date
     */
    Date getStartTime();

    /**
     * Sets the given <tt>ProtocolProviderService</tt> used for the call.
     * @param pps the <tt>ProtocolProviderService</tt> to set
     */
    void setProtocolProvider(ProtocolProviderService pps);

    /**
     * Returns the protocol provider used for the call. Could be null if the
     * record has not saved the provider.
     * @return the protocol provider used for the call
     */
    ProtocolProviderService getProtocolProvider();

    /**
     * This is the end reason of the call if any.
     * @param endReason the reason code.
     */
    void setEndReason(int endReason);

    /**
     * This is the end reason of the call if any. -1 the default value
     * for no reason specified.
     * @return end reason code if any.
     */
    int getEndReason();

    /**
     * Sets the UID representing the peer contact for this call
     * @param callPeerMetaContactUID the UID to set for this call
     */
    void setCallPeerContactUID(String callPeerMetaContactUID);

    /**
     * Returns the UID representing the peer contact for this call
     * @return the UID representing the peer contact for this call
     */
    String getCallPeerContactUID();

    /**
     * Sets the given attention state
     * @param attention the attention to set
     */
    void setAttention(Boolean attention);

    /**
     * @return the attention state of this record
     * true if the historic call requries attention from the user
     */
    Boolean getAttention();
}
