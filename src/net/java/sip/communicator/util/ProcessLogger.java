// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.util.logging.Level;

/**
 * A logging class for logging process information to a separate file.
 */
public class ProcessLogger
{
    private static final java.util.logging.Logger sLoggerDelegate
            = java.util.logging.Logger.getLogger("jitsi.ProcessLogger");

    private static final ProcessLogger sLogger = new ProcessLogger();

    /** Keep track of when we last logged the total number of processes. */
    private static long sLastProcessCountLogTimeMillis = 0;

    /** Don't log the number of processes more than once per minute. */
    private static final long PROCESS_COUNT_LOG_PERIOD_MS = 60000;

    /**
     * Get an instance of the process logger.
     */
    public static ProcessLogger getLogger()
    {
        return sLogger;
    }

    public boolean isTraceEnabled()
    {
        return sLoggerDelegate.isLoggable(Level.FINER);
    }

    public void traceExec(String command)
    {
        logExec(command, Level.FINER);
    }

    public void traceExec(String[] command)
    {
        logExec(command, Level.FINER);
    }

    public boolean isDebugEnabled()
    {
        return sLoggerDelegate.isLoggable(Level.FINE);
    }

    public void debugExec(String command)
    {
        logExec(command, Level.FINE);
    }

    public void debugExec(String[] command)
    {
        logExec(command, Level.FINE);
    }

    public boolean isInfoEnabled()
    {
        return sLoggerDelegate.isLoggable(Level.INFO);
    }

    public void infoExec(String command)
    {
        logExec(command, Level.INFO);
    }

    public void infoExec(String[] command)
    {
        logExec(command, Level.INFO);
    }

    public void warnExec(String command)
    {
        logExec(command, Level.WARNING);
    }

    public void warnExec(String[] command)
    {
        logExec(command, Level.WARNING);
    }

    /**
     * Log out a message in the process logs. Also log the total number of
     * child processes, but rate limit this so that performance isn't impacted.
     *
     * @param msg Simple string message to log
     * @param level The level to log the message at
     */
    private void log(Level level, String msg)
    {
        if (!sLoggerDelegate.isLoggable(level))
            return;

        if (System.currentTimeMillis() - sLastProcessCountLogTimeMillis > PROCESS_COUNT_LOG_PERIOD_MS)
        {
            // The underlying native code occasionally throws RuntimeExceptions here. As we're only logging, catch and ignore them.
            try
            {
                msg += "\nTotal number of processes: " + ProcessHandle.allProcesses().count();
            }
            catch (RuntimeException e)
            {
                sLoggerDelegate.warning("Failed to get list of processes " + e);
            }
            sLastProcessCountLogTimeMillis = System.currentTimeMillis();
        }

        sLoggerDelegate.log(level, msg);
    }

    /**
     * @param command The command that is being executed
     * @param level The level to log the message at
     */
    private void logExec(String command, Level level)
    {
        log(level, "Executing: " + command);
    }

    /**
     * @param command The command that is being executed
     * @param level The level to log the message at
     */
    private void logExec(String[] command, Level level)
    {
        log(level, "Executing: " + String.join(" ", command));
    }
}
