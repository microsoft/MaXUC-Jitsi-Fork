/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.*;

/**
 * Extends DefaultFileHandler, using specific <tt>ConfigFileHandler</tt>
 * properties.
 */
public class HttpFileHandler
    extends DefaultFileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/accession-httpheaders%u.log";

    /**
     * Construct an <tt>HttpFileHandler</tt>.  This will be configured entirely
     * from <tt>LogManager</tt> properties (or their default values).
     * Will change
     * <p>
     * @exception  IOException if there are IO problems opening the files.
     * @exception  SecurityException  if a security manager exists and if
     * the caller does not have <tt>LoggingPermission("control"))</tt>.
     * @exception  NullPointerException if pattern property is an empty String.
     */
    public HttpFileHandler()
        throws  IOException,
                SecurityException
    {
        super(HttpFileHandler.class, DEFAULT_PATTERN);
    }

    @Override
    protected void setFilter() throws SecurityException
    {
        // Do nothing - we want to use the default filter
    }
}
