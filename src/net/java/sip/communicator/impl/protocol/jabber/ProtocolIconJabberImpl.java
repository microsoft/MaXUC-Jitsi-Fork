/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.Hashtable;
import java.util.Iterator;

import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.protocol.ProtocolIcon;
import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.service.resources.ResourceManagementService;

/**
 * Represents the Jabber protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide a Jabber icon image in two different sizes.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ProtocolIconJabberImpl
    implements ProtocolIcon
{
    /**
     * The path where all protocol icons are placed.
     */
    private final String iconPath;

    private static ResourceManagementService resourcesService;

    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private final Hashtable<String, BufferedImageFuture> iconsTable
        = new Hashtable<>();

    /**
     * A hash table containing the path to the protocol icon in different sizes.
     */
    private final Hashtable<String, String> iconPathsTable
        = new Hashtable<>();

    /**
     * Creates an instance of this class by passing to it the path, where all
     * protocol icons are placed.
     *
     * @param iconPath the protocol icon path
     */
    public ProtocolIconJabberImpl(String iconPath)
    {
        this.iconPath = iconPath;

        ImageLoaderService imageLoaderService = JabberActivator.getImageLoaderService();

        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16,
            imageLoaderService.getImage(ImageLoaderService.JABBER_IM_LOGO_16x16));

        iconsTable.put(ProtocolIcon.ICON_SIZE_32x32,
            imageLoaderService.getImage(ImageLoaderService.JABBER_IM_LOGO_32x32));

        iconsTable.put(ProtocolIcon.ICON_SIZE_48x48,
            imageLoaderService.getImage(ImageLoaderService.JABBER_IM_LOGO_48x48));
    }

    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method. Returns
     * an iterator to a set containing the supported icon sizes.
     * @return an iterator to a set containing the supported icon sizes
     */
    public Iterator<String> getSupportedSizes()
    {
        return iconsTable.keySet().iterator();
    }

    /**
     * Returns TRUE if a icon with the given size is supported, FALSE-otherwise.
     *
     * @return TRUE if a icon with the given size is supported, FALSE-otherwise.
     */
    public boolean isSizeSupported(String iconSize)
    {
        return iconsTable.containsKey(iconSize);
    }

    /**
     * Returns the icon image in the given size.
     * @param iconSize the icon size; one of ICON_SIZE_XXX constants
     */
    public BufferedImageFuture getIcon(String iconSize)
    {
        return iconsTable.get(iconSize);
    }

    /**
     * Returns a path to the icon with the given size.
     * @param iconSize the size of the icon we're looking for
     * @return the path to the icon with the given size
     */
    public String getIconPath(String iconSize)
    {
        return iconPathsTable.get(iconSize);
    }

    /**
     * Returns the icon image used to represent the protocol connecting state.
     * @return the icon image used to represent the protocol connecting state
     */
    public BufferedImageFuture getConnectingIcon()
    {
        ImageLoaderService imageLoaderService = JabberActivator.getImageLoaderService();

        return imageLoaderService.getIconFromPath(iconPath + "/status16x16-connecting.gif")
            .getImage();
    }

    /**
     * Get the <tt>ResourceMaangementService</tt> registered.
     *
     * @return <tt>ResourceManagementService</tt> registered
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference<?> serviceReference = JabberActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService
                = (ResourceManagementService)JabberActivator.bundleContext
                    .getService(serviceReference);
        }

        return resourcesService;
    }
}
