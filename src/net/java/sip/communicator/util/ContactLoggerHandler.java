// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.*;

/**
 * Log handler for the contact logger
 */
public class ContactLoggerHandler extends DefaultFileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/accession-contacts%u.log";

    public ContactLoggerHandler()
        throws IOException,
        SecurityException
    {
        super(ContactLoggerHandler.class, DEFAULT_PATTERN);
    }
}
