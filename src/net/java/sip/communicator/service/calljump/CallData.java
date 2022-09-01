// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.calljump;

import org.json.simple.*;

import net.java.sip.communicator.util.*;

/**
 * Container for a call data object returned by the EAS
 */
public class CallData
{
    private static final Logger sLog = Logger.getLogger(CallData.class);

    private final String mCallState;
    private final String mCallerNumber;
    private final String mCallType;
    private final String mCallID;

    /**
     * Creates a new CallData instance.
     *
     * @param json The JSON returned by EAS to parse and store.
     */
    public CallData(JSONObject json)
    {
        sLog.debug("Creating new call data: " + json);
        mCallState    = (String) json.get("callState");
        mCallerNumber = (String) json.get("callerNumber");
        mCallType     = (String) json.get("callType");
        mCallID       = (String) json.get("callID");
    }

    public CallData(String callState, String callerNumber, String callType, String callID)
    {
        sLog.debug("Creating new call data");
        mCallState    = callState;
        mCallerNumber = callerNumber;
        mCallType     = callType;
        mCallID       = callID;
    }

    /**
     * @return the call ID
     */
    public String getCallID()
    {
        return mCallID;
    }

    /**
     * @return the type of call
     */
    public String getCallType()
    {
        return mCallType;
    }

    /**
     * @return the caller's number
     */
    public String getCallerNumber()
    {
        return mCallerNumber;
    }

    /**
     * @return the call state
     */
    public String getCallState()
    {
        return mCallState;
    }

    @Override
    public String toString()
    {
        return "<Call State: " + mCallState +
               ", Caller Number: " + mCallerNumber +
               ", Call Type: " + mCallType +
               ", Call ID: " + mCallID + ">";
    }
}
