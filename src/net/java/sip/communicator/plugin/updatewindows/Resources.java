/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.updatewindows;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * Implements methods to facilitate dealing with resources in the update
 * plug-in.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class Resources
{
    /**
     * The <tt>Logger</tt> used by the <tt>Resources</tt> class for logging
     * output.
     */
    private static Logger logger = Logger.getLogger(Resources.class);

    /**
     * The name of the configuration file of the update plug-in.
     */
    private static final String UPDATE_CONFIGURATION_FILE
        = "update-location.properties";

    /**
     * The <tt>ResourceManagementService</tt> registered in the
     * <tt>BundleContext</tt> of the update plug-in.
     */
    private static ResourceManagementService resources;

    /**
     * The properties found in the configuration file of the update plug-in.
     */
    private static Properties updateConfiguration;

    /**
     * Gets the <tt>ResourceManagementService</tt> registered in the
     * <tt>BundleContext</tt> of the update plug-in.
     *
     * @return the <tt>ResourceManagementService</tt> (if any) registered in the
     * <tt>BundleContext</tt> of the update plug-in
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
        {
            resources
                = ServiceUtils.getService(
                    UpdateWindowsActivator.bundleContext,
                    ResourceManagementService.class);
        }
        return resources;
    }

    /**
     * Gets a <tt>String</tt> value associated with a specific key in the
     * configuration file of the update plug-in.
     *
     * @param key the key to get the associated <tt>String</tt> value of
     * @return the <tt>String</tt> value (if any) associated with the specified
     * <tt>key</tt> in the configuration file of the update plug-in
     */
    public static String getUpdateConfigurationString(String key)
    {
        if (updateConfiguration == null)
        {
            updateConfiguration = new Properties();

            File updateConfigurationFile = new File(UPDATE_CONFIGURATION_FILE);

            if (updateConfigurationFile.exists())
            {

                try (InputStream updateConfigurationInputStream = new FileInputStream(
                        updateConfigurationFile))
                {
                    updateConfiguration.load(updateConfigurationInputStream);
                }
                catch (IOException ioe)
                {
                    logger.error(
                            "Could not load the configuration file of the update"
                                    + " plug-in.",
                            ioe);
                }
            }
            else
            {
                logger.info("No configuration file specified for the update"
                            + " plug-in.");
            }
        }

        return updateConfiguration.getProperty(key);
    }
}
