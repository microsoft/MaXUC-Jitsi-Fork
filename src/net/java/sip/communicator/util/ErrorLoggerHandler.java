// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.*;

/**
 * Log handler for errors
 */
public class ErrorLoggerHandler extends DefaultFileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/accession-errors%u-%g.log";

    public ErrorLoggerHandler() throws IOException
    {
        super(ErrorLoggerHandler.class, DEFAULT_PATTERN);
    }
}
