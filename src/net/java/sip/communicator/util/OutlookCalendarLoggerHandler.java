// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.IOException;

/**
 * Log handler for the OutlookCalendarLogger
 */
public class OutlookCalendarLoggerHandler extends DefaultFileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/outlook-calendar%u.log";

    public OutlookCalendarLoggerHandler()
        throws IOException,
        SecurityException
    {
        super(OutlookCalendarLoggerHandler.class, DEFAULT_PATTERN);
    }
}

