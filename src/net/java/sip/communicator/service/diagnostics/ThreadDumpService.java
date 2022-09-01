// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.diagnostics;

import java.io.*;

/**
 * Abstracts the Thread Dump procedure so that it can be used throughout the
 * application.
 */
public interface ThreadDumpService
{
    /**
     * Generate a list of threads which includes details of any deadlocks that have
     * been detected
     *
     * @return a string suitable for writing to a log file
     */
    String getThreads();

    /**
     * Dump the list of threads to a file.
     *
     * This contains details of any deadlocks that have
     * been detected
     *
     * @throws IOException if we failed to write the thread dump
     */
    void dumpThreads() throws IOException;

    void triggerHeapDump() throws IOException;
}
