// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.analytics;

import org.json.simple.*;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.util.*;

/**
 * Subclass of AnalyticsParameter for a simple String analytics value.
 */
public class AnalyticsParameterSimple extends AnalyticsParameter
{
    private static final Logger sLog = Logger.getLogger(AnalyticsParameterSimple.class);

    private final String mValue;

    /**
     * @param name The name of the parameter
     * @param value The value of the parameter
     */
    public AnalyticsParameterSimple(String name,
                                    String value)
    {
        super(name);

        if (value == null)
        {
            sLog.warn("Event with null value: " + name + "," + value);
            value = "null";
        }

        mValue = value;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.analytics.AnalyticsParameter#addToJSON(org.json.JSONObject, int)
     */
    // JSON simple doesn't allow for generics.
    @SuppressWarnings("unchecked")
    public int addToJSON(JSONObject jsonObject,
                         int currentLength)
    {
        int length = mName.length() + mValue.length() + 1;

        // Check we're not exceeding the max length with this parameter
        if (currentLength + length > MAX_EVENT_LENGTH)
        {
            sLog.error("Drop attempt to exceed max event length with param: " + mName);
            return 0;
        }
        else
        {
            jsonObject.put(formatName(mName), mValue);
            return length;
        }
    }

    @VisibleForTesting
    public String getValue()
    {
        return mValue;
    }

    public boolean equals(Object parameter)
    {
        boolean equal = false;
        if (getClass().isInstance(parameter))
        {
            if (((AnalyticsParameter) parameter).getParameterName().equals(mName) &&
                ((AnalyticsParameterSimple) parameter).getValue().equals(mValue))
            {
                equal = true;
            }
        }
        return equal;
    }

    /**
     * Only used for logging
     */
    public String toString()
    {
        return "Name: " + mName + "; Value: " + mValue;
    }
}
