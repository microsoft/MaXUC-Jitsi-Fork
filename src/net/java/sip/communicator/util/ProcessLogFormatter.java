// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.util.logging.LogRecord;

/**
 * Log formatter for the process logger
 */
public class ProcessLogFormatter extends ScLogFormatter
{
    @Override
    protected String getLoggerName(LogRecord record)
    {
        return record.getSourceClassName();
    }
}
