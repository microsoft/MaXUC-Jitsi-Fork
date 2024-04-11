// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.wispaservice;

/**
 * Enum to determine which type of WISPA Motion is sent
 */
public enum WISPAMotionType
{
    SHUTDOWN,
    RATING,
    UPDATE_INFO,
    CONFERENCING_EDUCATION,
    UPDATE_PROGRESS_INFO,
    ERROR,
    REFRESH_SSO_TOKEN,
    SHOW_LOGGING_IN,
    DISPLAY_CONTACT,
    NOTIFY_CONTACT_NOW_AVAILABLE,
    FETCH_GRAPH_API_ACCESS_TOKEN,
}
