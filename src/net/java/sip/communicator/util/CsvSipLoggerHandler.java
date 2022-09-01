// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.*;

/**
 * Log handler for the ADV SIP logger
 */
public class CsvSipLoggerHandler extends DefaultFileHandler
{
    public static final String SIP_LOG_FILENAME = "sip-log.csv";
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/" + SIP_LOG_FILENAME;

    public CsvSipLoggerHandler()
        throws IOException,
        SecurityException
    {
        super(CsvSipLoggerHandler.class, DEFAULT_PATTERN);
    }
}
