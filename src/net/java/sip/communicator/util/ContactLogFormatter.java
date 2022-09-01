// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.util.logging.*;

/**
 * Log formatter for the contacts logger
 */
public class ContactLogFormatter extends ScLogFormatter
{
    @Override
    protected String getLoggerName(LogRecord record)
    {
        return record.getSourceClassName();
    }
}