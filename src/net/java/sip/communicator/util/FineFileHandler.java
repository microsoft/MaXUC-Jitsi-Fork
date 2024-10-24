/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.*;

/**
 * Extends DefaultFileHandler, using specific <tt>FineFileHandler</tt>
 * properties.
 */
public class FineFileHandler
    extends DefaultFileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/accession-FINE%u.log";

    /**
     * Construct a <tt>FileHandler</tt>.  This will be configured entirely
     * from <tt>LogManager</tt> properties (or their default values).
     * Will change
     * <p>
     * @exception  IOException if there are IO problems opening the files.
     * @exception  SecurityException  if a security manager exists and if
     * the caller does not have <tt>LoggingPermission("control"))</tt>.
     * @exception  NullPointerException if pattern property is an empty String.
     */
    public FineFileHandler()
        throws  IOException,
                SecurityException
    {
        super(FineFileHandler.class, DEFAULT_PATTERN);
    }
}
