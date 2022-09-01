// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.IOException;

/**
 * Gathers utility functions related to Process objects.
 */
public class ProcessUtils
{
    private static final Logger logger = Logger.getLogger(ProcessUtils.class);

    /**
     * Properly dispose of the given process object by closing its input, output
     * and error streams, then destroying it. This should be called on every
     * Process that we have finished with, in order to prevent handle leaks.
     */
    public static void dispose(Process process)
    {
        if (process == null)
            return;

        try
        {
            process.getInputStream().close();
            process.getOutputStream().close();
            process.getErrorStream().close();
        }
        catch (IOException e)
        {
            logger.error("Could not close streams of process " + process.pid(), e);
        }
        finally
        {
            process.destroy();
        }
    }
}
