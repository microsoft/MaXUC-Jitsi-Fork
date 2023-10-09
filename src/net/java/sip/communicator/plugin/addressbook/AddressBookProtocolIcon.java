/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * Represents the protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide it to the service.
 */
public class AddressBookProtocolIcon
    implements ProtocolIcon
{
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
    public AddressBookProtocolIcon(String iconPath)
    {
        // Nothing to do
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
}

