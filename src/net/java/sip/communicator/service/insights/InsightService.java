// Copyright (c) Microsoft Corporation. All rights reserved.
// Highly Confidential Material
package net.java.sip.communicator.service.insights;

import java.beans.PropertyChangeEvent;

import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * <tt>InsightService<tt> interacts with and configures the Application Insights
 * telemetry agent, and wraps common telemetry logging.
 */
public interface InsightService
{

    boolean isTelemetryEnabled();

    /**
     * Tests to see if the telemetry can be enabled, and make an attempt to enable it
     *
     * @param event - property change event that can expand the conditions of the test
     */
    boolean tryToEnableTelemetry(PropertyChangeEvent event);

    /**
     * Enable telemetry - the only case the method should fail is when the connection string is blank
     * or null
     */
    boolean enableTelemetry();

    /**
     * Starts the service - adds relevant listeners.
     */
    void start();

    /**
     * Log an event. Should be used when telemetry needs to be passed with some parameters and/or metrics
     */
    void logEvent(InsightEvent insightEvent);

    /**
     * Log an event by type as a shorthand, when no other parameters and metrics are needed
     *
     * @param eventName - Insight type, a MaxAnalytics event name
     */
    void logEvent(String eventName);

    /**
     * Log any <tt>Telemetry</tt> predefined in the com.microsoft.applicationinsights.telemetry package. This overload
     * should be used when a defined event fits better with a <tt>Telemetry</tt> object instead of an
     * <tt>InsightEvent</tt> e.g. <tt>TraceTelemetry</tt> in case of trace collection
     */
    void logEvent(Telemetry telemetry);

}
