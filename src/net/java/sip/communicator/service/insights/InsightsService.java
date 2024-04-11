// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.insights;

import java.util.Map;

import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * <tt>InsightsService<tt> interacts with and configures the Application Insights
 * telemetry agent, and wraps common telemetry logging.
 */
public interface InsightsService
{
    /**
     * Log an event. Should be used when telemetry needs to be passed with some parameters and/or metrics
     */
    void logEvent(InsightsEvent insightEvent);

    /**
     * Log an event by type as a shorthand, when no other parameters and metrics are needed
     *
     * @param eventName - event name
     */
    void logEvent(String eventName);

    /**
     * Log any <tt>Telemetry</tt> predefined in the com.microsoft.applicationinsights.telemetry package. This overload
     * should be used when a defined event fits better with a <tt>Telemetry</tt> object instead of an
     * <tt>InsightsEvent</tt> e.g. <tt>TraceTelemetry</tt> in case of trace collection
     */
    void logEvent(Telemetry telemetry);

    /**
     * Log any event you want to obscure behind a hint.
     *
     * @param hint - String describing your event. Some common hints may be found in <tt>InsightsEventHint</tt>
     * @param parameterInfo - useful data that you might need to properly parse your event
     */
    void logEvent(String hint, Map<String, Object> parameterInfo);

    /**
     * Sets up the initial environment, and start log collection
     */
    void startCollection();
}
