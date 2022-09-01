/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.defaultresourcepack;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * The default image resource pack.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class DefaultImagePackImpl
    implements ImagePack
{
    private Logger logger =
        Logger.getLogger(DefaultImagePackImpl.class);

    private static final String DEFAULT_RESOURCE_PATH
        = "resources.images.images";

    public InputStream getImageAsStream(String path)
    {
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }

    public URL getImageURL(String path)
    {
        return this.getClass().getClassLoader().getResource(path);
    }

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     */
    public Map<String, String> getResources()
    {
        ResourceBundle resourceBundle
            = ResourceBundle.getBundle(DEFAULT_RESOURCE_PATH);

        Map<String, String> resources = new TreeMap<>();

        this.initResources(resourceBundle, resources);

        this.initPluginResources(resources);

        return resources;
    }

    /**
     * Returns the name of this resource pack.
     *
     * @return the name of this resource pack.
     */
    public String getName()
    {
        return "Default Image Resources";
    }

    /**
     * Returns the description of this resource pack.
     *
     * @return the description of this resource pack.
     */
    public String getDescription()
    {
        return "Provide SIP Communicator default Image resource pack.";
    }

    /**
     * Fills the given resource map with all (key,value) pairs obtained from the
     * given <tt>ResourceBundle</tt>. This method will look in the properties
     * files for references to other properties files and will include in the
     * final map data from all referenced files.
     *
     * @param resourceBundle The initial <tt>ResourceBundle</tt>, corresponding
     * to the "main" properties file.
     * @param resources A <tt>Map</tt> that would store the data.
     */
    private void initResources( ResourceBundle resourceBundle,
                                Map<String, String> resources)
    {
        Enumeration<String> imageKeys = resourceBundle.getKeys();

        while (imageKeys.hasMoreElements())
        {
            String key = imageKeys.nextElement();
            String value = resourceBundle.getString(key);

            resources.put(key, value);
            logger.trace("Added resource to image pack: " + key + " : " + value);
        }
    }

    /**
     * Finds all plugin image resources, matching the "images-*.properties"
     * pattern and adds them to this resource pack.
     */
    private void initPluginResources(Map<String, String> resources)
    {
        Iterator<String> pluginProperties = DefaultResourcePackActivator
            .findResourcePaths("resources/images", "images-*.properties");

        while (pluginProperties.hasNext())
        {
            String resourceBundleName = pluginProperties.next();
            logger.debug("Adding plugin images from: " + resourceBundleName);

            ResourceBundle resourceBundle
                = ResourceBundle.getBundle(
                    resourceBundleName.substring(
                        0, resourceBundleName.indexOf(".properties")));

            initResources(resourceBundle, resources);
        }
    }
}
