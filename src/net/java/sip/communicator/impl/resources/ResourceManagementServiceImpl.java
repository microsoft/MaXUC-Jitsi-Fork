/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.resources;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;

/**
 * A default implementation of the <tt>ResourceManagementService</tt>.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ResourceManagementServiceImpl
    extends AbstractResourcesService
{
    /**
     * The <tt>Logger</tt> used by the <tt>ResourceManagementServiceImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ResourceManagementServiceImpl.class);

    /**
     * UI Service reference.
     */
    private UIService uiService = null;

    /**
     * Initializes already registered default resource packs.
     */
    public ResourceManagementServiceImpl()
    {
        super(ResourceManagementActivator.bundleContext);

        UIService serv = getUIService();
        if (serv != null)
        {
            serv.repaintUI();
        }
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    private UIService getUIService()
    {
        if (uiService == null)
        {
            uiService
                = ServiceUtils.getService(
                        ResourceManagementActivator.bundleContext,
                        UIService.class);
        }
        return uiService;
    }

    /**
     * Gets a reference to the <tt>UIService</tt> when this one is registered.
     *
     * @param event the <tt>ServiceEvent</tt> that has notified us
     */
    @Override
    public void serviceChanged(ServiceEvent event)
    {
        super.serviceChanged(event);

        Object sService = ResourceManagementActivator.bundleContext
                .getService(event.getServiceReference());

        if (sService instanceof UIService && uiService == null
                && event.getType() == ServiceEvent.REGISTERED)
        {
            uiService = (UIService) sService;
            uiService.repaintUI();
        }
        else if (sService instanceof UIService
                && event.getType() == ServiceEvent.UNREGISTERING)
        {
            if (uiService != null && uiService.equals(sService))
            {
                uiService = null;
            }
        }
    }

    /**
     * Repaints the whole UI when a skin pack has changed.
     */
    protected void onSkinPackChanged()
    {
        UIService serv = getUIService();
        if (serv != null)
        {
            serv.repaintUI();
        }
    }

    /**
     * Returns the int representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the int representation of the color corresponding to the
     * given key. If the key does not exist, returns 0xFFFFFF (white)
     */
    public int getColor(String key)
    {
        return this.getColor(key, 0xFFFFFF);
    }

    /**
     * Returns the int representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @param defaultValue The value to use if the key does not exist
     * @return the int representation of the color corresponding to the
     * given key.
     */
    public int getColor(String key, int defaultValue)
    {
        return this.getColor(key, defaultValue, true);
    }

    /**
     * Returns the int representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @param defaultValue The value to use if the key does not exist
     * @param errorIfMissing Whether to log an error message if this
     *                       colour is missing from the config.  True
     *                       for most, but there are a few optional
     *                       overrides.
     * @return the int representation of the color corresponding to the
     * given key.
     */
    public int getColor(String key, int defaultValue, boolean errorIfMissing)
    {
        // Check in the user's configuration file to see if the color has been
        // overridden
        ScopedConfigurationService user = ResourceManagementActivator.getConfigurationService().user();
        String configColor = user != null ? user.getString(key) : null;
        if (!StringUtils.isNullOrEmpty(configColor))
        {
            return Integer.parseInt(configColor, 16);
        }

        String res = getColorResources().get(key);

        if(StringUtils.isNullOrEmpty(res))
        {
            if (errorIfMissing)
            {
                logger.error("Missing color resource for key: " + key);
            }
            else
            {
                logger.trace("Missing optional color resource for key: " + key);
            }

            return defaultValue;
        }
        else
            return Integer.parseInt(res, 16);
    }

    /**
     * Returns the string representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the string representation of the color corresponding to the
     * given key. If the key does not exist, return "FFFFFF" (white)
     */
    public String getColorString(String key)
    {
        return this.getColorString(key, "FFFFFF");
    }

    /**
     * Returns the string representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @param defaultValue The value to use if the key does not exist
     * @return the string representation of the color corresponding to the
     * given key.
     */
    public String getColorString(String key, String defaultValue)
    {
        // Check in the user's configuration file to see if the color has been
        // overridden
        String configColor = ResourceManagementActivator.getConfigurationService().global().getString(key);
        if (!StringUtils.isNullOrEmpty(configColor))
        {
            return configColor;
        }

        String res = getColorResources().get(key);

        if(res == null)
        {
            logger.error("Missing color resource for key: " + key);

            return defaultValue;
        }
        else
            return res;
    }

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * path.
     *
     * @param path The path to the image file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * path or null if no image is found.
     */
    public InputStream getImageInputStreamForPath(String path)
    {
        if (path == null)
            return null;

        InputStream stream = null;
        for (ImagePack imagePack : getImagePacks().values())
        {
            stream = imagePack.getImageAsStream(path);
            if (stream != null)
            {
                break;
            }
        }

        return stream;
    }

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     *
     * @param streamKey The identifier of the image in the resource properties
     * file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     */
    public InputStream getImageInputStream(String streamKey)
    {
        String path = getImagePath(streamKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + streamKey);
            return null;
        }

        return getImageInputStreamForPath(path);
    }

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given key.
     *
     * @param urlKey The identifier of the image in the resource properties file.
     * @return the <tt>URL</tt> of the image corresponding to the given key
     */
    public URL getImageURL(String urlKey)
    {
        String path = getImagePath(urlKey);

        if (path == null || path.length() == 0)
        {
            logger.info("Missing resource for key: " + urlKey);
            return null;
        }
        return getImageURLForPath(path);
    }

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given path.
     *
     * @param path The path to the given image file.
     * @return the <tt>URL</tt> of the image corresponding to the given path.
     */
    public URL getImageURLForPath(String path)
    {
        URL url = null;
        for (ImagePack imagePack : getImagePacks().values())
        {
            url = imagePack.getImageURL(path);
            if (url != null)
            {
                break;
            }
        }
        return url;
    }

    /**
     * Gets an ArrayList of URLs (as Strings) of the files matching pattern in
     * the directory specified by path. This method first checks the SkinPack,
     * then any ImagePacks for a non-empty directory matching the path
     *
     * @param path The path to the given directory
     * @param pattern The file name pattern for selecting entries in the
     * specified path
     * @return An <tt>ArrayList</tt> of String urls representing the contents
     * of the specified directory which match pattern
     */
    public ArrayList<String> getUrlsFromDirectory(String path, String pattern)
    {
        ArrayList<String> urls = null;
        for (ImagePack imagePack : getImagePacks().values())
        {
            Bundle bundle = FrameworkUtil.getBundle(imagePack.getClass());
            urls = getUrlsFromDirectory(bundle, path, pattern);
            if (!urls.isEmpty())
            {
                break;
            }
        }
        return urls;
    }

    private ArrayList<String> getUrlsFromDirectory(Bundle b, String path, String pattern)
    {
        Enumeration<URL> e = b.findEntries(path, pattern, false);
        ArrayList<String> directoryFilenames = new ArrayList<>();
        if (e != null)
        {
            while (e.hasMoreElements())
            {
                URL url = e.nextElement();
                directoryFilenames.add(url.toString());
            }
        }
        return directoryFilenames;
    }

    /**
     * Returns the <tt>URL</tt> of the sound corresponding to the given
     * property key.
     *
     * @return the <tt>URL</tt> of the sound corresponding to the given
     * property key.
     */
    public URL getSoundURL(String urlKey)
    {
        String path = getSoundPath(urlKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey);
            return null;
        }
        return getSoundURLForPath(path);
    }

    /**
     * Returns the <tt>URL</tt> of the sound corresponding to the given path.
     *
     * @param path the path, for which we're looking for a sound URL
     * @return the <tt>URL</tt> of the sound corresponding to the given path.
     */
    public URL getSoundURLForPath(String path)
    {
        return getSoundPack().getClass().getClassLoader().getResource(path);
    }
}
