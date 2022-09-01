// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.IOException;

/**
 * Log handler for the device logger
 */
public class DeviceLoggerHandler extends DefaultFileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/accession-devices%u.log";

    public DeviceLoggerHandler()
        throws IOException,
        SecurityException
    {
        super(DeviceLoggerHandler.class, DEFAULT_PATTERN);
    }
}

