/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.launcher;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.apache.felix.main.Main;

import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ScStdOut;
import net.java.sip.communicator.util.launchutils.LaunchOSUtils;

/**
 * Represents a main instance of SIPCommunicator i.e. one that holds a SIPCommunicator lock and has its home directory
 * set up.
 * <p>
 * Performs setup required after we know we are the main instance but before starting any OSGI bundles - see Javadoc on
 * {@link #startup()}.
 */
public class SIPCommunicatorMainInstance
{
    private static final Logger logger = Logger.getLogger(SIPCommunicatorMainInstance.class);

    /**
     * The registry key under which the default Outlook integration application is placed. There can only be one such
     * key.
     */
    private static final String REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY
            = "Software\\IM Providers";
    private static final String REGISTRY_DEFAULT_OUTLOOK_APPLICATION_VALUE
            = "DefaultIMApp";
    private static final String REGISTRY_DEFAULT_OUTLOOK_APPLICATION_UNSET_VALUE
            = "None";

    /**
     * Performs setup required after we know we are the main instance but before starting any OSGI bundles, such as:
     * <ul>
     *     <li>Starting Electron</li>
     *     <li>Loading native libraries</li>
     * </ul>
     */
    static void startup() throws Exception
    {
        // Must call after obtaining SipCommunicatorLock to avoid creating orphaned UI processes
        ElectronUILauncher.startElectronUI();

        loadNativeLibraries();

        // Clean up this unused registry entry deprecated in V3.20
        unsetDefaultOutlookApp();

        System.setOut(new ScStdOut(System.out));

        // Finally, start Felix which will takeover startup from here by
        // starting bundles
        Main.main(new String[0]);
    }

    private static void loadNativeLibraries()
    {
        if (LaunchOSUtils.isWindows())
        {
            // Load libraries to support various headsets.
            // The libraries are loaded here as a workaround as loading them after MaX UC has started results
            // in us not being able to find the libraries. However, we wait until determining we're the main instance,
            // to avoid loading them if we're only going to pass off the URI to another instance, above. This only affects Windows.
            String[] libraries = {"libjabra", "libyealinkusbsdk"};
            for (String library : libraries)
            {
                try
                {
                    System.loadLibrary(library);
                    logger.info("Loaded " + library);
                }
                catch (Error | Exception e)
                {
                    logger.error("Unable to load " + library + " library: " +
                                 e.getMessage());
                }
            }
        }
    }

    /**
     * If there's a default Outlook IM app set in the registry for MaX UC, remove that registry entry
     */
    private static void unsetDefaultOutlookApp()
    {
        try
        {
            if (defaultOutlookAppIsSet())
            {
                Advapi32Util.registrySetStringValue(
                        WinReg.HKEY_CURRENT_USER,
                        REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY,
                        REGISTRY_DEFAULT_OUTLOOK_APPLICATION_VALUE,
                        REGISTRY_DEFAULT_OUTLOOK_APPLICATION_UNSET_VALUE);
                logger.info("Default outlook registry key unset.");
            }
        }
        catch (Exception e)
        {
            logger.info("Hit an exception while checking/setting the Outlook registry key.");
        }
    }

    private static boolean defaultOutlookAppIsSet()
    {
        return LaunchOSUtils.isWindows() &&
               outlookRegistryKeyExists() &&
               outlookRegistryValueExists() &&
               outlookRegistryKeyMatchesAppName();
    }

    private static boolean outlookRegistryKeyExists()
    {
        return Advapi32Util.registryKeyExists(
                WinReg.HKEY_CURRENT_USER,
                REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY);
    }

    private static boolean outlookRegistryValueExists()
    {
        return Advapi32Util.registryValueExists(
                WinReg.HKEY_CURRENT_USER,
                REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY,
                REGISTRY_DEFAULT_OUTLOOK_APPLICATION_VALUE);
    }

    private static boolean outlookRegistryKeyMatchesAppName()
    {
        String defaultOutlookApp =
                Advapi32Util.registryGetStringValue(
                        WinReg.HKEY_CURRENT_USER,
                        REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY,
                        REGISTRY_DEFAULT_OUTLOOK_APPLICATION_VALUE);

        return defaultOutlookApp.equals(HomeDirectoryConfigurer.appName);
    }
}
