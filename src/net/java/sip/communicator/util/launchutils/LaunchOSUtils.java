/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util.launchutils;

/**
 * OS util class that is safe to be used very early in setup i.e. during the scope of
 * {@link net.java.sip.communicator.launcher.SIPCommunicator#main(String[])} before Felix is started.
 * <p>
 * DO NOT use the OSUtils values for Windows/Mac in main() or methods called from it - loading that class at this point
 * will load logging configuration from the libjitsi logger that prevents the first logs from this point in setup.
 */
public class LaunchOSUtils
{
    public static final String OS_NAME = System.getProperty("os.name");

    public static boolean isMac()
    {
        if (OS_NAME != null)
        {
            return OS_NAME.startsWith("Mac");
        }
        else
        {
            return false;
        }
    }

    public static boolean isWindows()
    {
        if (OS_NAME != null)
        {
            return OS_NAME.startsWith("Windows");
        }
        else
        {
            return false;
        }
    }
}
