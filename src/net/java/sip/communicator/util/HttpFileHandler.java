/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import static net.java.sip.communicator.util.PrivacyUtils.getLoggableCPURL;
import static org.jitsi.util.SanitiseUtils.sanitise;

import java.io.IOException;
import java.util.regex.Pattern;

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

    private static final Pattern COOKIE_PATTERN = Pattern.compile("Cookie: [^=]+=(.+)");

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
        setFilter(logRecord ->
        {
            // first remove Personal Data from URL
            if (logRecord.getMessage().contains("HTTP/") || logRecord.getMessage().contains("Location:"))
            {
                logRecord.setMessage(getLoggableCPURL(logRecord.getMessage()));
            }
            else if (logRecord.getMessage().contains("Cookie"))
            {
                logRecord.setMessage(
                        sanitise(logRecord.getMessage(), COOKIE_PATTERN, PrivacyUtils::obfuscateSessionId));
            }
            return true;
        });
    }
}
