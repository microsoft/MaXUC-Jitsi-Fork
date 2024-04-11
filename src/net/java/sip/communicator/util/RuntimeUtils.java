// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

/**
 * A simple helper class which we use to proxy calls to Runtime.getRuntime()
 * in order to make it mockable in tests.
 */
public class RuntimeUtils
{
    /**
     * Just proxies the call to Runtime.getRuntime().addShutdownHook().
     */
    public static void addShutdownHook(Thread hook)
    {
        Runtime.getRuntime().addShutdownHook(hook);
    }

    /**
     * Calls Runtime.getRuntime().halt(systemExitCode).
     */
    public static void halt(int systemExitCode)
    {
        Runtime.getRuntime().halt(systemExitCode);
    }
}
