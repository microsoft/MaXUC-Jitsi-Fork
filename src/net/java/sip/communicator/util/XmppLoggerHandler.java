// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.IOException;

/**
 * Log handler for the XMPP logger
 */
public class XmppLoggerHandler extends DefaultFileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/accession-xmpp%u.log";

    public XmppLoggerHandler() throws IOException, SecurityException
    {
        super(XmppLoggerHandler.class, DEFAULT_PATTERN);
    }
}
