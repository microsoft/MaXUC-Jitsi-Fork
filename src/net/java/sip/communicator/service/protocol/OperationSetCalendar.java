// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

/**
 * OperationSetCalendar offers services to handle meeting information received
 * from an external calendar.
 */
public interface OperationSetCalendar
    extends OperationSet
{
    /**
     * Marks any meetings the user is currently in as "left".
     */
    void markMeetingsAsLeft();
}
