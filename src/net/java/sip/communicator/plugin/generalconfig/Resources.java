/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;

import net.java.sip.communicator.service.resources.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 *
 * @author Yana Stamcheva
 */
public class Resources
{
    private static ResourceManagementService resourcesService;

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        return getResources().getI18NString(key);
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @param params parameters for I18.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key, String[] params)
    {
        return getResources().getI18NString(key, params);
    }

    /**
     * Returns an application property string corresponding to the given key.
     * @param key The key of the string.
     * @return A string corresponding to the given key.
     */
    public static String getSettingsString(String key)
    {
        return getResources().getSettingsString(key);
    }

    /**
     * Loads an image from a given image identifier.
     * @param imageId The identifier of the image.
     * @return The image for the given identifier.
     */
    public static BufferedImageFuture getBufferedImage(String imageId)
    {
        return getResources().getBufferedImage(imageId);
    }

    /**
     * Loads an image from a given image identifier.
     * @param imageId The identifier of the image.
     * @return The image for the given identifier.
     */
    public static ImageIconFuture getImage(String imageId)
    {
        return getResources().getImage(imageId);
    }

    /**
     * Returns the color corresponding to the given resource key.
     * @param key The key of the color
     * @return A color corresponding to the given key.
     */
    public static Color getColor(String key)
    {
        return new Color(getResources().getColor(key));
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils
                    .getService(GeneralConfigPluginActivator.bundleContext);
        return resourcesService;
    }
}
