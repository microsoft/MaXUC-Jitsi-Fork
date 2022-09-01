/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import org.jitsi.service.configuration.*;

import com.google.common.cache.*;
import com.sun.management.OperatingSystemMXBean;

import net.java.sip.communicator.service.diagnostics.*;

/**
 * Standard logging methods plus MSw extra USER level.  Logging should conform to
 * Java standards and also match our use in Android.
 * <p>
 * Configuration for all loggers is in logging.properties, or which there are
 * 2 copies to be kept in sync, at: jitsi/lib and jitsi/resources/install
 * <p>
 * Note, there is an [almost] duplicate of this class in org.jitsi.util. Please
 * keep in sync (or maybe we can commonize).
 *
 * @author Emil Ivov
 */
public class Logger
{
    private static final int ONE_HOUR_IN_SECONDS = 3600;

    /**
     * The java.util.Logger that would actually be doing the logging.
     */
    private final java.util.logging.Logger loggerDelegate;

    /**
     * The cache used by interval logging to determine if an event has already
     * been logged
     */
    private Cache<String, Object[]> logCache;

    /**
     * Detail text associated with a particular logger instance, to be prepended
     * to the main log.
     */
    private final String instanceDetail;

    private OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    /**
     * Base constructor
     *
     * @param logger the implementation specific logger delegate that this
     * Logger instance should be created around.
     */
    private Logger(java.util.logging.Logger logger)
    {
        this(logger, "");
    }

    /**
     * Base constructor
     *
     * @param logger the implementation specific logger delegate that this
     * Logger instance should be created around.
     * @param instanceDetail any specific message detail that should be
     * prepended to messages created by this logger.
     */
    private Logger(java.util.logging.Logger logger, String instanceDetail)
    {
        this.loggerDelegate = logger;
        this.instanceDetail = (instanceDetail.length() > 0) ?
                                 "- " + instanceDetail + " - " :
                                 "";
    }

    /**
     * Find or create a logger for the specified class.  If a logger has
     * already been created for that class it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the logging configuration and it will be configured
     * to also send logging output to its parent's handlers.
     * <p>
     * @param clazz The creating class.
     *
     * @return a suitable Logger
     * @throws NullPointerException if the name is null.
     */
    public static Logger getLogger(Class<?> clazz)
        throws NullPointerException
    {
        return getLogger(clazz.getName());
    }

    /**
     * Find or create a logger for the specified class.  If a logger has
     * already been created for that class it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the logging configuration and it will be configured
     * to also send logging output to its parent's handlers.
     * <p>
     * @param clazz The creating class.
     * @param instanceDetail A string to prepend to the start of all records
     * printed by this logger
     *
     * @return a suitable Logger
     * @throws NullPointerException if the name is null.
     */
    public static Logger getLogger(Class<?> clazz, String instanceDetail)
        throws NullPointerException
    {
        return getLogger(clazz.getName(), instanceDetail);
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the logging configuration and it will be configured
     * to also send logging output to its parent's handlers.
     * <p>
     * @param name A name for the logger. This should be a dot-separated name
     * and should normally be based on the class name of the creator, such as
     * "net.java.sip.communicator.MyFunnyClass"
     *
     * @return a suitable Logger
     * @throws NullPointerException if the name is null.
     */
    public static Logger getLogger(String name)
        throws NullPointerException
    {
        return new Logger(java.util.logging.Logger.getLogger(name));
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the logging configuration and it will be configured
     * to also send logging output to its parent's handlers.
     * <p>
     * @param name A name for the logger. This should be a dot-separated name
     * and should normally be based on the class name of the creator, such as
     * "net.java.sip.communicator.MyFunnyClass"
     * @param instanceDetail A string to prepend to the start of all records
     * printed by this logger
     *
     * @return a suitable Logger
     * @throws NullPointerException if the name is null.
     */
    public static Logger getLogger(String name, String instanceDetail)
    {
        return new Logger(java.util.logging.Logger.getLogger(name), instanceDetail);
    }

    /**
     * Get the Diagnostics Service
     *
     * @return the Diagnostics Service
     */
    public static DiagnosticsService getDiagnosticService()
    {
        return UtilActivator.getDiagnosticsService();
    }

    /**
     * @return the directory into which the logs are put
     */
    public static File getLogDirectory()
    {
        ConfigurationService cfg = UtilActivator.getConfigurationService();

        return new File(cfg.global().getScHomeDirLocation() + File.separator +
                        cfg.global().getScHomeDirName() + File.separator +
                        "log");
    }

    /**
     * Utility function to concatenate the string representation of none or more
     * objects.  This will only be called if a particular log level is enabled,
     * making it more efficient when the level is disabled (fairly important
     * for finer and below which are off by default).
     */
    private String concatenateParameters(Object... msgs)
    {
        StringBuilder builder = new StringBuilder();

        for (Object msg : msgs)
        {
            builder.append((msg != null) ? msg.toString() : "null");
        }
        return builder.toString();
    }

    /**
     * Logs an entry in the calling method.
     *
     * Use varargs where possible, i.e. use this: sLog.entry("a", "b", "c");
     * NOT this: sLog.entry("a" + "b" + "c");
     */
    public void entry(Object... msgs)
    {
        if (loggerDelegate.isLoggable(Level.FINEST))
        {
            loggerDelegate.log(Level.FINEST, instanceDetail + "[entry] " +
                                             concatenateParameters(msgs));
        }
    }

    /**
     * Logs exiting the calling method
     *
     * Use varargs where possible, i.e. use this: sLog.exit("a", "b", "c");
     * NOT this: sLog.exit("a" + "b" + "c");
     */
    public void exit(Object... msgs)
    {
        if (loggerDelegate.isLoggable(Level.FINEST))
        {
            loggerDelegate.log(Level.FINEST, instanceDetail + "[exit] " +
                                             concatenateParameters(msgs));
        }
    }

    /**
     * Check if a message with a TRACE level would actually be logged by this
     * logger.
     *
     * @return true if the TRACE level is currently being logged
     */
    public boolean isTraceEnabled()
    {
        return loggerDelegate.isLoggable(Level.FINER);
    }

    /**
     * Log a TRACE message.
     * <p>
     * Use varargs where possible, i.e. use this: sLog.trace("a", "b", "c");
     * NOT this: sLog.trace("a" + "b" + "c");
     * <p>
     * If the logger is currently enabled for the TRACE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msg The message to log
     */
    public void trace(Object... msg)
    {
        if (isTraceEnabled())
        {
            loggerDelegate.finer(instanceDetail + concatenateParameters(msg));
        }
    }

    /**
     * Log a message, with associated Throwable information.
     *
     * @param msg   The message to log
     * @param   t   Throwable associated with log message.
     */
    public void trace(Object msg, Throwable t)
    {
        loggerDelegate.log(Level.FINER,
            instanceDetail + (msg!=null?msg.toString():"null"), t);
    }

    /**
     * Check if a message with a DEBUG level would actually be logged by this
     * logger.
     *
     * @return true if the DEBUG level is currently being logged
     */
    public boolean isDebugEnabled()
    {
        return loggerDelegate.isLoggable(Level.FINE);
    }

    /**
     * Log a DEBUG message.
     * <p>
     * Use varargs where possible, i.e. use this: sLog.debug("a", "b", "c");
     * NOT this: sLog.debug("a" + "b" + "c");
     * <p>
     * This version is required to prevent 'Failed to find debug method errors'
     * in JavaLogger.  Even though the version taking varargs would be expected
     * to handle it.
     *
     * @param msg The message to log
     */
    public void debug(Object msg)
    {
        loggerDelegate.fine(instanceDetail + (msg!=null?msg.toString():"null"));
    }

    /**
     * Log a DEBUG message.
     * <p>
     * Use varargs where possible, i.e. use this: sLog.debug("a", "b", "c");
     * NOT this: sLog.debug("a" + "b" + "c");
     * <p>
     * If the logger is currently enabled for the DEBUG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msgs The messages to log
     */
    public void debug(Object... msgs)
    {
        if (isDebugEnabled())
        {
            loggerDelegate.fine(instanceDetail + concatenateParameters(msgs));
        }
    }

    /**
     * Log a message, with associated Throwable information.
     *
     * @param msg    The message to log
     * @param t  Throwable associated with log message.
     */
    public void debug(Object msg, Throwable t)
    {
        loggerDelegate.log(Level.FINE,
            instanceDetail + (msg!=null?msg.toString():"null"), t);
    }

    /**
     * Logs a message provided that we haven't logged it recently.
     *
     * @param tag Used to determine if we've logged this line before.  Lines
     *            with the same tag will not be logged until an hour since the last
     *            log has passed.
     * @param message The message to log.  Logs with the same tag but different
     *                messages will not be logged unless an hour has passed
     * @param parameters Optional parameters, that can invalidate a tag cache. I.e.
     *                   if the parameters have changed from the last tag, this
     *                   log line will be written
     */
    public void interval(String tag, String message, Object... parameters)
    {
        interval(ONE_HOUR_IN_SECONDS, tag, message, parameters);
    }

    /**
     * Logs a message provided that we haven't logged it recently.
     *
     * @param expirySecs The minimum number of seconds between repeated logs
     *                   with the same tag and parameters.
     * @param tag Used to determine if we've logged this line before.  Lines
     *            with the same tag will not be logged until an hour since the last
     *            log has passed.
     * @param message The message to log.  Logs with the same tag but different
     *                messages will not be logged unless an hour has passed
     * @param parameters Optional parameters, that can invalidate a tag cache. I.e.
     *                   if the parameters have changed from the last tag, this
     *                   log line will be written
     */
    public void interval(int expirySecs,
                         String tag,
                         String message,
                         Object... parameters)
    {
        // Only create the log cache if we actually need it.
        if (logCache == null)
        {
            synchronized (this)
            {
                if (logCache == null)
                {
                    logCache = CacheBuilder.newBuilder()
                                           .expireAfterWrite(expirySecs,
                                                             TimeUnit.SECONDS)
                                           .build();
                }
            }
        }

        if (parameters == null)
            parameters = new Object[0];

        Object[] cachedParameters = logCache.getIfPresent(tag);

        if (cachedParameters == null)
        {
            // First time that this has been logged, so actually log it:
            debug(tag + " " + message + " " + Arrays.toString(parameters));
            logCache.put(tag, parameters);
        }
        else
        {
            // This has been logged before, find out if the parameters have changed
            if (!Arrays.equals(parameters, cachedParameters))
            {
                // They are different.  Log and update the cache.
                debug(tag + " " + message + " " + Arrays.toString(parameters));
                logCache.put(tag, parameters);
            }
        }
    }

    /**
     * Check if a message with an INFO level would actually be logged by this
     * logger.
     *
     * @return true if the INFO level is currently being logged
     */
    public boolean isInfoEnabled()
    {
        return loggerDelegate.isLoggable(Level.INFO);
    }

    /**
     * Log a INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msg The message to log
     */
    public void info(Object msg)
    {
        loggerDelegate.info(instanceDetail + (msg!=null?msg.toString():"null"));
    }

    /**
     * Log a message, with associated Throwable information.
     *
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void info(Object msg, Throwable t)
    {
        loggerDelegate.log(Level.INFO,
                           instanceDetail + (msg!=null?msg.toString():"null"), t);
    }

    /**
     * Log a CONFIG message.
     * <p>
     * Use varargs where possible, i.e. use this: sLog.config("a", "b", "c");
     * NOT this: sLog.config("a" + "b" + "c");
     * <p>
     * If the logger is currently enabled for the CONFIG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msg The message to log
     */
    public void config(Object... msg)
    {
        loggerDelegate.config(instanceDetail + concatenateParameters(msg));
    }

    /**
     * Log a message, with associated Throwable information.
     *
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void config(Object msg, Throwable t)
    {
        loggerDelegate.log(Level.CONFIG,
                           instanceDetail + (msg!=null?msg.toString():"null"), t);
    }

    /**
     * Log a USER message.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msg The message to log
     */
    public void user(Object msg)
    {
        loggerDelegate.log(Level.USER,
                           instanceDetail + (msg!=null?msg.toString():"null"));
    }

    /**
     * Log a message, with associated Throwable information.
     *
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void user(Object msg, Throwable t)
    {
        loggerDelegate.log(Level.USER,
                           instanceDetail + (msg!=null?msg.toString():"null"), t);
    }

    /**
     * Log a WISPA message (i.e. the API to the UI).
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msg The message to log
     */
    public void wispa(Object msg)
    {
        loggerDelegate.log(Level.WISPA,
                           instanceDetail + (msg!=null?msg.toString():"null"));
    }

    /**
     * Log a WISPA message (i.e. the API to the UI), with associated
     * Throwable information.
     *
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void wispa(Object msg, Throwable t)
    {
        loggerDelegate.log(Level.WISPA,
                           instanceDetail + (msg!=null?msg.toString():"null"), t);
    }

    /**
     * Log a WISPA message (i.e. the API to the UI).
     * <p>
     * Use varargs where possible, i.e. use this: sLog.wispa("a", "b", "c");
     * NOT this: sLog.wispa("a" + "b" + "c");
     * <p>
     * If the logger is currently enabled for the WISPA message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msgs The messages to log
     */
    public void wispa(Object... msgs)
    {
        wispa(concatenateParameters(msgs));
    }

    /**
     * Log a WARN message.
     * <p>
     * If the logger is currently enabled for the WARN message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msg The message to log
     */
    public void warn(Object msg)
    {
        loggerDelegate.warning(instanceDetail + (msg!=null?msg.toString():"null"));
    }

    /**
     * Log a message, with associated Throwable information.
     *
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void warn(Object msg, Throwable t)
    {
        loggerDelegate.log(Level.WARNING,
                           instanceDetail + (msg!=null?msg.toString():"null"), t);
    }

    /**
     * Log a ERROR message.
     * <p>
     * If the logger is currently enabled for the ERROR message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msg The message to log
     */
    public void error(Object msg)
    {
        loggerDelegate.severe(instanceDetail + (msg!=null?msg.toString():"null"));
    }

    /**
     * Log a message, with associated Throwable information.
     *
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void error(Object msg, Throwable t)
    {
        loggerDelegate.log(Level.SEVERE,
                           instanceDetail + (msg!=null?msg.toString():"null"), t);
    }

    /**
     * Log a FATAL message.
     * <p>
     * If the logger is currently enabled for the FATAL message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param msg The message to log
     */
    public void fatal(Object msg)
    {
        loggerDelegate.severe(instanceDetail + (msg!=null?msg.toString():"null"));
    }

    /**
     * Log a message, with associated Throwable information.
     *
     * @param msg   The message to log
     * @param t  Throwable associated with log message.
     */
    public void fatal(Object msg, Throwable t)
    {
        loggerDelegate.log(Level.SEVERE,
                           instanceDetail + (msg!=null?msg.toString():"null"), t);
    }

    /**
     * Set logging level for all handlers to FATAL
     */
    public void setLevelFatal()
    {
        setLevel(Level.SEVERE);
    }

    /**
     * Set logging level for all handlers to ERROR
     */
    public void setLevelError()
    {
        setLevel(Level.SEVERE);
    }

    /**
     * Set logging level for all handlers to WARNING
     */
    public void setLevelWarn()
    {
        setLevel(Level.WARNING);
    }

    /**
     * Set logging level for all handlers to INFO
     */
    public void setLevelInfo()
    {
        setLevel(Level.INFO);
    }

    /**
     * Set logging level for all handlers to DEBUG
     */
    public void setLevelDebug()
    {
        setLevel(Level.FINE);
    }

    /**
     * Set logging level for all handlers to TRACE
     */
    public void setLevelTrace()
    {
        setLevel(Level.FINER);
    }

    /**
     * Set logging level for all handlers to ALL (allow all log messages)
     */
    public void setLevelAll()
    {
        setLevel(Level.ALL);
    }

    /**
     * Set logging level for all handlers to OFF (allow no log messages)
     */
    public void setLevelOff()
    {
        setLevel(Level.OFF);
    }

    /**
     * Set logging level for all handlers to <tt>level</tt>
     *
     * @param level the level to set for all logger handlers
     */
    private void setLevel(java.util.logging.Level level)
    {
        Handler[] handlers = loggerDelegate.getHandlers();
        for (Handler handler : handlers)
            handler.setLevel(level);

        loggerDelegate.setLevel(level);
    }

    /**
     * Reinitialize the logging properties and reread the logging configuration.
     * <p>
     * The same rules are used for locating the configuration properties
     * as are used at startup. So if the properties containing the log dir
     * locations have changed, we would read the new configuration.
     */
    public void reset()
    {
        try
        {
            LogManager.getLogManager().reset();
            LogManager.getLogManager().readConfiguration();
        }
        catch (Exception e)
        {
            error("Failed to reinit logger.", e);
        }
    }

    public void logMemoryStats(java.util.logging.Level level)
    {
        loggerDelegate.log(level,
            instanceDetail + "OS memory usage: Physical: " +
                osBean.getTotalPhysicalMemorySize() + " total, " +
                osBean.getFreePhysicalMemorySize() + " free; Swap: " +
                osBean.getTotalSwapSpaceSize() + " total, " +
                osBean.getFreeSwapSpaceSize() + " free.");

        loggerDelegate.log(level,
            instanceDetail + "Current heap memory usage: " + memoryBean.getHeapMemoryUsage());
        loggerDelegate.log(level,
            instanceDetail + "Current non-heap memory usage: " + memoryBean.getNonHeapMemoryUsage());
    }

    public void logStackTrace(String message, java.util.logging.Level level)
    {
        loggerDelegate.log(level,
                           instanceDetail + (message!=null ? message.toString() : "null"),
                           new Throwable(message));
    }

    public void logStackTrace(String message)
    {
        logStackTrace(message, (Level) Level.INFO);
    }

    /**
     * Custom 'super-fine' logging level, used for contacts logs that are too
     * frequent to display in the FINEST logfile.
     */
    public static final Level NOTE;

    static
    {
        // Create new NOTE level in a static block to ensure it's created before
        // we reset the log manager below.
        NOTE = new Level("NOTE", 100);

        // Reset the log manager so that it picks up the new logging level.
        // (logging levels are processed statically in java.util.logging.logger,
        // which has already loaded by the time this block gets hit).
        try
        {
            LogManager.getLogManager().reset();
            LogManager.getLogManager().readConfiguration();
        }
        catch (Exception e)
        {
            getLogger(Logger.class).error("Failed to reinit logger.", e);
        }
    }

    /**
     * Just a wrapper around java.util.logging.Level to allow us to create our
     * own custom logging levels.
     */
    public static class Level extends java.util.logging.Level
    {
        private static final long serialVersionUID = 0L;

        protected Level(String name, int value)
        {
            super(name, value);
        }

        /**
         * USER is a message level for user actions.
         * This level is initialized to <CODE>800</CODE>, i.e.
         * the same as INFO.
         */
        public static final Level USER = new Level("USER", 800);

        /**
         * WISPA is a message level for user actions.
         * This level is initialized to <CODE>800</CODE>, i.e.
         * the same as INFO.
         */
        public static final Level WISPA = new Level("WISPA", 800);
    }
}
