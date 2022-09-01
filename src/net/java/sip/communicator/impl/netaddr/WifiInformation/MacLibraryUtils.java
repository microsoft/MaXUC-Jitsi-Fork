// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import net.java.sip.communicator.util.Logger;
import org.jitsi.util.OSUtils;

public class MacLibraryUtils
{
    private static final Logger logger = Logger.getLogger(MacLibraryUtils.class);

    /**
     * Load the specified library, if required.  Explicit loading of libraries
     * is not necessary on macOS >= Big Sur, as the system ships with a built-in
     * dynamic linker cache of all system-provided libraries.
     *
     * @param name The name of the library being loaded (for logging purposes)
     * @param path The path to the library to load
     */
    public static void load(String name, String path)
    {
        OSUtils.MacOSVersion macOSVersion = OSUtils.getMacVersion();
        if (macOSVersion != null &&
            !macOSVersion.isGreaterOrEqual(OSUtils.MacOSVersion.BIG_SUR_10_16))
        {
            try
            {
                logger.info("Trying to load " + name +
                            " for macOSVersion " + macOSVersion.getString());
                System.load(path);
            }
            catch (UnsatisfiedLinkError error)
            {
                logger.error("Failed to load library: " + name, error);
            }
        }
        else
        {
            logger.info("Not necessary to load " + name +
                        ", as macOSVersion = " + macOSVersion.getString());
        }
    }
}
