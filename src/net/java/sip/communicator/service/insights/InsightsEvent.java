// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.insights;

import java.util.Map;

/**
 * Common data transfer object class for telemetry events. Convenience class mimicking the Telemetry class.
 */
public class InsightsEvent
{
    private final String eventType;

    /**
     * Maps of properties and metrics. Properties should be well-defined string values, while metrics are reserved for
     * numeric values.
     * <p/>
     * Properties differ from metrics in how they are filtered and displayed on Azure. If a metric is logged as a
     * property, its key will appear in the transaction search, along with the logged value e.g. key 'Count'.
     */
    private final Map<String, String> properties;
    private final Map<String, Double> metrics;

    public InsightsEvent(String eventType)
    {
        this(eventType, null, (Map<String, Double>) null);
    }

    public InsightsEvent(String eventType, Map<String, String> properties)
    {
        this(eventType, properties, null);
    }

    public InsightsEvent(String eventType, String propName, String propValue)
    {
        this(eventType, Map.of(propName, propValue), null);
    }

    public InsightsEvent(String eventType, String metricName, Double metricValue)
    {
        this(eventType, null, Map.of(metricName, metricValue));
    }

    public InsightsEvent(String eventType, Map<String, String> properties, Map<String, Double> metrics)
    {
        this.eventType = eventType;
        this.properties = properties;
        this.metrics = metrics;
    }

    public String getEventType()
    {
        return eventType;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public Map<String, Double> getMetrics()
    {
        return metrics;
    }

    @Override
    public String toString()
    {
        return "eventType=" + eventType + ", properties=" + properties + ", metrics=" + metrics;
    }
}
