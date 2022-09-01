// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.IOException;

/**
 * Log handler for the DNS logger
 */
public class DnsLoggerHandler extends DefaultFileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/accession-dns%u.log";

    public DnsLoggerHandler() throws IOException, SecurityException
    {
        super(DnsLoggerHandler.class, DEFAULT_PATTERN);
    }
}
