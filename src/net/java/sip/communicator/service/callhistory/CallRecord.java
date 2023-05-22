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
    public static final String OUT = "out";

    /**
     * The incoming call direction.
     */
    public static final String IN = "in";

    /**
     * Sets the given UID to be the UID of this record
     * @param uid the UID to set
     */
    public void setUid(String uid);

    /**
     * @return the UID of this record
     */
    public String getUid();

    /**
     * The source call which this record servers
     * @param sourceCall Call
     */
    public void setSourceCall(Call sourceCall);

    /**
     * The Call source of this record
     * @return Call
     */
    public Call getSourceCall();

    /**
     * Finds a CallPeer with the supplied address
     *
     * @param address String
     * @return CallPeerRecord
     */
    public CallPeerRecord findPeerRecord(String address);

    /**
     * Return Vector of CallPeerRecords
     * @return Vector
     */
    public List<CallPeerRecord> getPeerRecords();

    /**
     * Sets the direction of the call.
     * IN or OUT
     * @param direction String
     */
    public void setDirection(String direction);

    /**
     * Returns the direction of the call
     * IN or OUT
     * @return String
     */
    public String getDirection();

    /**
     * Set the time when the call finishes
     * If some peer has no end Time set we set it also
     * @param endTime Date
     */
    public void setEndTime(Date endTime);

    /**
     * Returns the time when the call has finished
     * @return Date
     */
    public Date getEndTime();

    /**
     * Sets the time when the call begins
     * @param startTime Date
     */
    public void setStartTime(Date startTime);

    /**
     * The time when the call has began
     * @return Date
     */
    public Date getStartTime();

    /**
     * Sets the given <tt>ProtocolProviderService</tt> used for the call.
     * @param pps the <tt>ProtocolProviderService</tt> to set
     */
    public void setProtocolProvider(ProtocolProviderService pps);

    /**
     * Returns the protocol provider used for the call. Could be null if the
     * record has not saved the provider.
     * @return the protocol provider used for the call
     */
    public ProtocolProviderService getProtocolProvider();

    /**
     * This is the end reason of the call if any.
     * @param endReason the reason code.
     */
    public void setEndReason(int endReason);

    /**
     * This is the end reason of the call if any. -1 the default value
     * for no reason specified.
     * @return end reason code if any.
     */
    public int getEndReason();

    /**
     * Sets the UID representing the peer contact for this call
     * @param callPeerMetaContactUID the UID to set for this call
     */
    public void setCallPeerContactUID(String callPeerMetaContactUID);

    /**
     * Returns the UID representing the peer contact for this call
     * @return the UID representing the peer contact for this call
     */
    public String getCallPeerContactUID();

    /**
     * Sets the given attention state
     * @param attention the attention to set
     */
    public void setAttention(Boolean attention);

    /**
     * @return the attention state of this record
     * true if the historic call requries attention from the user
     */
    public Boolean getAttention();
}
