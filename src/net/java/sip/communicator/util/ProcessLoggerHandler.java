// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.IOException;

/**
 * Log handler for the process logger
 */
public class ProcessLoggerHandler extends DefaultFileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/accession-process%u.log";

    public ProcessLoggerHandler()
        throws IOException,
        SecurityException
    {
        super(ProcessLoggerHandler.class, DEFAULT_PATTERN);
    }
}
