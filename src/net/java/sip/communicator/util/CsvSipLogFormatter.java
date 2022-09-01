// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.util.logging.*;

public class CsvSipLogFormatter
    extends java.util.logging.Formatter
{
      /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(record.getMessage());
        sb.append("\n");
        return sb.toString();
    }
}
