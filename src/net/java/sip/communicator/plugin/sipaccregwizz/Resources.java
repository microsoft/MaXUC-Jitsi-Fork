/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

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

    public static ImageID SIP_LOGO
        = new ImageID("service.protocol.sip.SIP_16x16");

    public static ImageID PAGE_IMAGE
        = new ImageID("service.protocol.sip.SIP_64x64");

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
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static BufferedImageFuture getImage(ImageID imageID)
    {
        return getResources().getBufferedImage(imageID.getId());
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
                    .getService(SIPAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
