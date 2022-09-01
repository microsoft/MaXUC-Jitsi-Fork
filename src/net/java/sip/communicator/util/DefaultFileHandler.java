/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.*;
import java.util.logging.*;

/**
 * Simple file logging <tt>Handler</tt>.
 * Extends java.util.logging.FileHandler and adds the special component to
 * the file pattern - %s which is replaced at runtime with sip-communicator's
 * home directory. If the pattern option is missing creates log
 * directory in sip-communicator's home directory.
 * If the directory is missing create it.
 *
 * @author Damian Minkov
 */
public class DefaultFileHandler
    extends java.util.logging.FileHandler
{
    /**
     * The default log filename pattern to use when there isn't a pattern
     * specified in logging.properties.
     */
    private static final String DEFAULT_PATTERN = "/log/accession%u.log";

    /**
     * Initialize a <tt>FileHandler</tt> to write to a set of files.  When
     * (approximately) the given limit has been written to one file,
     * another file will be opened.  The output will cycle through a set
     * of count files.
     * <p>
     * The <tt>FileHandler</tt> is configured based on <tt>LogManager</tt>
     * properties (or their default values) except that the given pattern
     * argument is used as the filename pattern, the file limit is
     * set to the limit argument, and the file count is set to the
     * given count argument.
     * <p>
     * The count must be at least 1.
     *
     * @param pattern  the pattern for naming the output file
     * @param limit  the maximum number of bytes to write to any one file
     * @param count  the number of files to use
     * @exception  IOException if there are IO problems opening the files.
     * @exception  SecurityException  if a security manager exists and if
     * the caller does not have <tt>LoggingPermission("control")</tt>.
     * @exception IllegalArgumentException if limit < 0, or count < 1.
     * @exception  IllegalArgumentException if pattern is an empty string
     */
    public DefaultFileHandler(String pattern, int limit, int count)
        throws IOException, SecurityException
    {
        super(pattern, limit, count);

        setFilter();
    }

    /**
     * Construct a default <tt>FileHandler</tt>.  This will be configured
     * entirely from <tt>LogManager</tt> properties (or their default values).
     * <p>
     * @exception  IOException if there are IO problems opening the files.
     * @exception  SecurityException  if a security manager exists and if
     * the caller does not have <tt>LoggingPermission("control"))</tt>.
     * @exception  NullPointerException if pattern property is an empty String.
     */
    public DefaultFileHandler()
        throws  IOException,
                SecurityException
    {
        this(DefaultFileHandler.class, DEFAULT_PATTERN);
    }

    /**
     * Construct a <tt>FileHandler</tt>.  This will be configured based on
     * <tt>LogManager</tt> properties (or their default values) except that
     * the given pattern argument is used as the filename pattern.
     * Will change
     * <p>
     * @param clazz  the specific FileHandler class, for finding properties in
     * logging.properties
     * @param defaultPattern  the pattern for naming the output file if there
     * isn't one in <tt>logging.properties</tt>
     * @exception  IOException if there are IO problems opening the files.
     * @exception  SecurityException  if a security manager exists and if
     * the caller does not have <tt>LoggingPermission("control"))</tt>.
     * @exception  NullPointerException if pattern property is an empty String.
     */
    public DefaultFileHandler(Class<?> clazz, String defaultPattern)
        throws IOException,
               SecurityException
    {
        // If a pattern is passed to java.util.logging.FileHandler, then it
        // will ignore the limit and count from logging.properties, so we must
        // get those values here and pass them in.
        super(getPattern(clazz, defaultPattern),
              getLimit(clazz),
              getCount(clazz));

        setFilter();
    }

    /**
     * Does this log record represent a HTTP header log, which should only
     * be logged to -httpheaders?
     *
     * @param record
     * @return true if it represents a HTTP header log
     */
    public static boolean isHttpHeaderLog(LogRecord record)
    {
        return record != null &&
            "org.apache.http.impl.conn.DefaultClientConnection".equals(record.getSourceClassName()) &&
            record.getLevel() != null &&
            record.getLevel().intValue() <= Level.FINE.intValue();
    }

    /**
     * Does this log record represent an XMPP log?
     *
     * @param record
     * @return true if it represents an XMPP log
     */
    public static boolean isXmppLog(LogRecord record)
    {
        return record != null &&
            "net.java.sip.communicator.impl.protocol.jabber.SmackXmppLogger".equals(record.getSourceClassName());
    }

    /**
     * Sets the filter to reject some of the logs that we don't want in this file
     */
    protected void setFilter() throws SecurityException
    {
        setFilter(new Filter()
        {
            @Override
            public boolean isLoggable(LogRecord record)
            {
                // The default client connection class is very spammy, and is
                // already logged by the http header logs. Same with XMPP logs.
                return !isHttpHeaderLog(record) && !isXmppLog(record);
            }
        });
    }

    /**
     * Returns the limit size for one log file or default 5MB.
     * @return the limit size
     */
    private static int getLimit(Class<?> clazz)
    {
        String limitStr = LogManager.getLogManager().getProperty(
            clazz.getName() + ".limit");

        // Default value - 5MB is a reasonable size.
        int limit = 5000000;

        try
        {
            limit = Integer.parseInt(limitStr);
        }
        catch (Exception ex)
        {
            // The properties file was not read correctly - use the default
            // value for limit.
        }

        return limit;
    }

    /**
     * Returns the count of the log files or the default value 1;
     * @return file count
     */
    private static int getCount(Class<?> clazz)
    {
        String countStr = LogManager.getLogManager().getProperty(
            clazz.getName() + ".count");

        // Default value of one file.
        int count = 1;

        try
        {
            count = Integer.parseInt(countStr);
        }
        catch (Exception ex)
        {
            // The properties file was not read correctly - use the default
            // value for count.
        }

        return count;
    }

    /**
     * Substitute %s in the pattern and creates the directory if it
     * doesn't exist.
     *
     * @return the file pattern.
     */
    private static String getPattern(Class<?> clazz, String defaultPattern)
    {
        String pattern = LogManager.getLogManager().getProperty(
            clazz.getName() + ".pattern");

        String homeLocation = System.getProperty(
            "net.java.sip.communicator.SC_HOME_DIR_LOCATION");
        String dirName = System.getProperty(
            "net.java.sip.communicator.SC_HOME_DIR_NAME");

        if(homeLocation == null || dirName == null)
        {
            // if pattern is missing and dir name or home location properties are
            // also not defined its most probably running from source or testing -
            // lets create log directory in working dir.
            if(pattern == null)
                pattern = "." + defaultPattern;
        }
        else
        {
            if(pattern == null)
                pattern = homeLocation + "/" + dirName + defaultPattern;
            else
                pattern = pattern.replaceAll("\\%s",
                    homeLocation + "/" + dirName);
        }

        checkDestinationDirectory(pattern);

        return pattern;
    }

    /**
     * Creates the directory in the pattern.
     *
     * @param pattern the directory we'd like to check.
     */
    private static void checkDestinationDirectory(String pattern)
    {
        try
        {
            int ix = pattern.lastIndexOf('/');

            if(ix != -1)
            {
                String dirName = pattern.substring(0, ix);
                dirName = dirName.replaceAll(
                                "%h", System.getProperty("user.home"));
                dirName = dirName.replaceAll(
                                "%t", System.getProperty("java.io.tmpdir"));

                new File(dirName).mkdirs();
            }
        }
        catch (Exception e){}
    }
}
