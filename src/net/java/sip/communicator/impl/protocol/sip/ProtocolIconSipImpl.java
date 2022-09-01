/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents the Sip protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide an sip icon image in two different sizes.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ProtocolIconSipImpl
    implements ProtocolIcon
{
    private final String iconPath;

    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private Hashtable<String, BufferedImageFuture> iconsTable;

    /**
     * A hash table containing the path to the  protocol icon in different sizes.
     */
    private Hashtable<String, String> iconPathsTable;

    /**
     * Creates an instance of this class by passing to it the path, where all
     * protocol icons are placed.
     *
     * @param iconPath the protocol icon path
     */
    public ProtocolIconSipImpl(String iconPath)
    {
        this.iconPath = iconPath;
    }

    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method. Returns
     * an iterator to a set containing the supported icon sizes.
     * @return an iterator to a set containing the supported icon sizes
     */
    public Iterator<String> getSupportedSizes()
    {
        return getIconsTable().keySet().iterator();
    }

    /**
     * Returns <code>true</code> if an icon with the given size is supported,
     * <code>false</code> - otherwise.
     *
     * @param iconSize the size of the icon to search for. One of ICON_SIZE_XXX
     * constants.
     * @return <code>true</code> if an icon with the given size is supported,
     * <code>false</code> - otherwise.
     */
    public boolean isSizeSupported(String iconSize)
    {
        return getIconsTable().containsKey(iconSize);
    }

    /**
     * Returns the icon image in the given size.
     *
     * @param iconSize the size of the icon we're looking for. One of
     * ICON_SIZE_XXX constants.
     * @return the byte array representing the icon.
     */
    public BufferedImageFuture getIcon(String iconSize)
    {
        return getIconsTable().get(iconSize);
    }

    /**
     * Returns a path to the icon with the given size.
     * @param iconSize the size of the icon we're looking for
     * @return the path to the icon with the given size
     */
    public String getIconPath(String iconSize)
    {
        return getIconPathsTable().get(iconSize);
    }

    /**
     * Gets {@link #iconPathsTable} populating it first if necessary.
     *
     * @return {@link #iconPathsTable}
     */
    private synchronized Map<String, String> getIconPathsTable()
    {
        if (iconPathsTable == null)
            loadIconsFromIconPath();
        return iconPathsTable;
    }

    /**
     * Gets {@link #iconsTable} populating it first if necessary.
     *
     * @return {@link #iconsTable}
     */
    private synchronized Map<String, BufferedImageFuture> getIconsTable()
    {
        if (iconsTable == null)
            loadIconsFromIconPath();
        return iconsTable;
    }

    /**
     * Returns the icon image used to represent the protocol connecting state.
     *
     * @return the icon image used to represent the protocol connecting state.
     */
    public BufferedImageFuture getConnectingIcon()
    {
        return loadIcon(iconPath + "/sip-connecting.gif");
    }

    /**
     * Loads an icon with the specified icon size from a file with a specific
     * name located in {@link #iconPath} into {@link #iconsTable} and
     * {@link #iconPathsTable}.
     *
     * @param iconSize the size of the icon to be loaded as defined by the
     * <tt>ProtocolIcon#ICON_SIZE_*</tt> fields
     * @param iconFileName the name of the file in {@link #iconPath} from which
     * the icon is to be loaded
     */
    private void loadIconFromIconPath(String iconSize, String iconFileName)
    {
        String iconFilePath = iconPath + '/' + iconFileName;
        BufferedImageFuture icon = loadIcon(iconFilePath);

        if (icon != null)
        {
            iconsTable.put(iconSize, icon);
            iconPathsTable.put(iconSize, iconFilePath);
        }
    }

    /**
     * Loads the icons to be represented by this instance from {@link #iconPath}
     * into {@link #iconsTable} and {@link #iconPathsTable}.
     */
    private synchronized void loadIconsFromIconPath()
    {
        iconsTable = new Hashtable<>();
        iconPathsTable = new Hashtable<>();

        loadIconFromIconPath(ProtocolIcon.ICON_SIZE_16x16, "sip16x16.png");
        loadIconFromIconPath(ProtocolIcon.ICON_SIZE_32x32, "sip32x32.png");
        loadIconFromIconPath(ProtocolIcon.ICON_SIZE_48x48, "sip48x48.png");
        loadIconFromIconPath(ProtocolIcon.ICON_SIZE_64x64, "sip64x64.png");
    }

    /**
     * Loads an image from a given image path.
     *
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static BufferedImageFuture loadIcon(String imagePath)
    {
        return SipActivator.getResources().getBufferedImageFromPath(imagePath);
    }
}
