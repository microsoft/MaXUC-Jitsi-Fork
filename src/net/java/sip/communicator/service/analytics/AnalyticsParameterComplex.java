// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.analytics;

import java.util.*;

import org.json.simple.*;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.util.*;

/**
 * Subclass of AnalyticsParameter for complex analytics data structure.
 */
public class AnalyticsParameterComplex
    extends AnalyticsParameter
{
    private static final Logger sLog = Logger.getLogger(AnalyticsParameterSimple.class);

    private final AnalyticsParameter[] mValues;

    /**
     * @param name  The name of the parameter
     * @param values An array of parameters
     */
    public AnalyticsParameterComplex(String name, AnalyticsParameter[] values)
    {
        super(name);
        mValues = values;
    }

    /**
     * @param name  The name of the parameter
     * @param values A list of parameters
     */
    public AnalyticsParameterComplex(String name, List<AnalyticsParameter> values)
    {
        this(name, values.toArray(new AnalyticsParameter[0]));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.analytics.AnalyticsParameter#addToJSON
     * (org.json.JSONObject, int)
     */
    // JSON simple doesn't allow for generics.
    @SuppressWarnings("unchecked")
    public int addToJSON(JSONObject jsonObject, int currentLength)
    {
        // Beware this method is recursive since mValues may contain
        // other AnalyticsParameterComplex instances.
        //
        // We could do better ignoring long parameters, but this
        // is good enough.  We will spot badly behaved lines and
        // not put them.
        int newLength = currentLength + mName.length();
        JSONObject thisLevelJSONObject = new JSONObject();
        for (AnalyticsParameter value : mValues)
        {
            newLength += value.addToJSON(thisLevelJSONObject, newLength) + 1;
        }

        // Double check we're not exceeding the max length with
        // this parameter.
        if (newLength > MAX_EVENT_LENGTH)
        {
            sLog.error("Drop attempt to exceed max event length with param: " + mName);
            return 0;
        }
        else
        {
            jsonObject.put(formatName(mName), thisLevelJSONObject);
            return newLength - currentLength;
        }
    }

    @VisibleForTesting
    public List<AnalyticsParameter> getValues()
    {
        return Arrays.asList(mValues);
    }

    public boolean equals(Object otherParameter)
    {
        boolean equal = false;
        if (getClass().isInstance(otherParameter) &&
            ((AnalyticsParameter) otherParameter).getParameterName().equals(mName))
        {
            List<AnalyticsParameter> otherParameterValues =
                        ((AnalyticsParameterComplex) otherParameter).getValues();

            if (otherParameterValues.size() == mValues.length)
            {
                // The value of a complex analytics parameter is one
                // or more analytics parameters. This parameter can only be
                // equal to otherParameter if their values are the same.
                equal = true;
                for (AnalyticsParameter value : mValues)
                {
                    if (!(otherParameterValues.contains(value)))
                    {
                        equal = false;
                        break;
                    }
                }
            }
        }
        return equal;
     }

    /**
     * Only used for logging
     */
    public String toString()
    {
        StringBuilder string = new StringBuilder("Name: " + mName + "; Values: ");
        for (AnalyticsParameter param : mValues)
        {
            string.append(param);
            string.append("; ");
        }

        return string.toString();
    }
}
