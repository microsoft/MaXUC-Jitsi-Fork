// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.analytics;

import java.util.*;

/**
 * Service used to send events to one or both of SAS and the Msw analytics
 * server.
 *
 * @see net.java.sip.communicator.msw.impl.analytics.AnalyticsServiceImpl
 */
public interface AnalyticsService
{
    /**
     * Test to see if the analytics service is enabled.  This should be used to
     * prevent any unnecessary processing
     *
     * @return true if the analytics service is enabled
     */
    boolean isAnalyticsEnabled();

    /**
     * We can't start the SYS running timer in the constructor because the work
     * it does only makes sense once we are logged in, so we provide a method to
     * be used to start it at the appropriate later stage.
     */
    void startSysRunningTimer();

    /**
     * Called in order to log an event
     *
     * @param eventType the event type to log
     */
    void onEvent(AnalyticsEventType eventType);

    /**
     * Called in order to log an event with parameters
     *
     * @param event the event to log
     * @param parameters an array of parameters and their value.  If there are n
     *        parameters then the array should be of length 2*n with the
     *        parameter name first, then the value
     */
    void onEvent(AnalyticsEventType event,
                 String... parameters);

    /**
     * Called in order to log an event with parameters
     *
     * @param event the event to log
     * @param parameters array of AnalyticsParameters.
     */
    void onEvent(AnalyticsEventType event,
                 List<AnalyticsParameter> parameters);

    /**
     * Called in order to log an event with parameters
     *
     * @param event the event to log
     * @param parametersForCP for use sending analytics to SAS, may contain PII.
     * @param parametersForMSW for use sending analytics to MSW, must NOT contain PII.
     * @param sasMarkerType SAS marker type, or 0 if this event is not marked
     *        in SAS
     * @param sasMarkerValue SAS marker value
     */
    void onEvent(AnalyticsEventType event,
                 List<AnalyticsParameter> parametersForCP,
                 List<AnalyticsParameter> parametersForMSW,
                 int[] sasMarkerType,
                 String[] sasMarkerValue);

    /**
     *  Called in order to log an event for which we keep a count of the number
     *  of times the event occurs. If the event has already occurred, the
     *  event count of the current is incrementally increased. If not, a new
     *  log is created
     *
     * @param event the event to log
     * @param parameters array of AnalyticsParameters.
     */
    void onEventWithIncrementingCount(AnalyticsEventType event,
                                      List<AnalyticsParameter> parameters);

    /**
     *  Called in order to log an event for which we keep a count of the number
     *  of times the event occurs. If the event has already occurred, the
     *  event count of the current is incrementally increased. If not, a new
     *  log is created
     *
     * @param event the event to log
     * @param parameters array of AnalyticsParameters.
     * @param sasMarkerType SAS marker type, or 0 if this event is not marked
     *        in SAS
     * @param sasMarkerValue SAS marker value
     */
    void onEventWithIncrementingCount(AnalyticsEventType event,
                                      List<AnalyticsParameter> parameters,
                                      int[] sasMarkerType,
                                      String[] sasMarkerValue);
}
