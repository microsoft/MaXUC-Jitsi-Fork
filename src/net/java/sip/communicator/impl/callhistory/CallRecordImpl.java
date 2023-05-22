/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Add Source call to the CallRecord
 * @author Damian Minkov
 */
public class CallRecordImpl
    implements CallRecord
{
    /* The <tt>Call</tt> source of this record. */
    private Call mSourceCall = null;

    /* Indicates the direction of the call - IN or OUT. */
    protected String mDirection;

    /* The start call date. */
    protected Date mStartTime = null;

    /* The end call date. */
    protected Date mEndTime = null;

    /* The UID representing the peer contact for this call */
    private String mCallPeerContactUID = "";

    /* A list of all peer records corresponding to this call record. */
    protected final List<CallPeerRecord> mPeerRecords =
            new Vector<>();

    /* The protocol provider through which the call was made. */
    protected ProtocolProviderService mProtocolProvider;

    /**
     * This is the end reason of the call if any. -1 default value for
     * no reason specified.
     */
    protected int mEndReason = -1;

    /* The UID of this record */
    protected String mUid = null;

    /* The attention state of this record */
    protected Boolean mAttention = false;

    /* Creates CallRecord */
    public CallRecordImpl()
    {
        super();
    }

    /**
     * Creates a Call Record.
     * @param direction String
     * @param startTime Date
     * @param endTime Date
     */
    public CallRecordImpl(String direction,
                          Date startTime,
                          Date endTime)
    {
        mDirection = direction;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    /**
     * Sets the given UID to be the UID of this record
     * @param uid the UID to set
     */
    public void setUid(String uid)
    {
        mUid = uid;
    }

    /**
     * @return the UID of this record
     */
    public String getUid()
    {
        return mUid;
    }

    /**
     * The source call which this record servers
     * @param sourceCall Call
     */
    public void setSourceCall(Call sourceCall)
    {
        mSourceCall = sourceCall;
    }

    /**
     * The Call source of this record
     * @return Call
     */
    public Call getSourceCall()
    {
        return mSourceCall;
    }

    /**
     * Finds a CallPeer with the supplied address
     *
     * @param address String
     * @return CallPeerRecord
     */
    public CallPeerRecord findPeerRecord(String address)
    {
        for (CallPeerRecord item : mPeerRecords)
        {
            if (item.getPeerAddress().equals(address))
                return item;
        }

        return null;
    }

    /**
     * Return Vector of CallPeerRecords
     * @return Vector
     */
    public List<CallPeerRecord> getPeerRecords()
    {
        return mPeerRecords;
    }

    /**
     * Sets the direction of the call.
     * IN or OUT
     * @param direction String
     */
    public void setDirection(String direction)
    {
        mDirection = direction;
    }

    /**
     * Returns the direction of the call
     * IN or OUT
     * @return String
     */
    public String getDirection()
    {
        return mDirection;
    }

    /**
     * Set the time when the call finishes
     * If some peer has no end Time set we set it also
     * @param endTime Date
     */
    public void setEndTime(Date endTime)
    {
        mEndTime = endTime;

        for (CallPeerRecord item : mPeerRecords)
        {
            CallPeerRecordImpl itemImpl = (CallPeerRecordImpl) item;
            if (item.getEndTime() == null)
            {
                itemImpl.setEndTime(endTime);
            }
        }
    }

    /**
     * Returns the time when the call has finished
     * @return Date
     */
    public Date getEndTime()
    {
        return mEndTime;
    }

    /**
     * Sets the time when the call begins
     * @param startTime Date
     */
    public void setStartTime(Date startTime)
    {
        mStartTime = startTime;
    }

    /**
     * The time when the call has began
     * @return Date
     */
    public Date getStartTime()
    {
        return mStartTime;
    }

    /**
     * Sets the given <tt>ProtocolProviderService</tt> used for the call.
     * @param pps the <tt>ProtocolProviderService</tt> to set
     */
    public void setProtocolProvider(ProtocolProviderService pps)
    {
        mProtocolProvider = pps;
    }

    /**
     * Returns the protocol provider used for the call. Could be null if the
     * record has not saved the provider.
     * @return the protocol provider used for the call
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return mProtocolProvider;
    }

    /**
     * This is the end reason of the call if any.
     * @param endReason the reason code.
     */
    public void setEndReason(int endReason)
    {
        mEndReason = endReason;
    }

    /**
     * This is the end reason of the call if any. -1 the default value
     * for no reason specified.
     * @return end reason code if any.
     */
    public int getEndReason()
    {
        return mEndReason;
    }

    /**
     * Sets the UID representing the peer contact for this call
     * @param callPeerMetaContactUID the UID to set for this call
     */
    public void setCallPeerContactUID(String callPeerMetaContactUID)
    {
        mCallPeerContactUID = callPeerMetaContactUID;
    }

    /**
     * Returns the UID representing the peer contact for this call
     * @return the UID representing the peer contact for this call
     */
    public String getCallPeerContactUID()
    {
        return mCallPeerContactUID;
    }

    /**
     * Sets the given attention state
     * @param attention the attention to set
     */
    public void setAttention(Boolean attention)
    {
        mAttention = attention;
    }

    /**
     * @return the attention state of this record
     * true if the historic call requries attention from the user
     */
    public Boolean getAttention()
    {
        return mAttention;
    }

    @Override
    public String toString()
    {
        return "CallRecord-" + mSourceCall + ", " + mUid + ", " + getPeerRecords();
    }
}
