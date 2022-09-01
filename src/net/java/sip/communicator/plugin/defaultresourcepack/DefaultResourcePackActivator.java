/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.defaultresourcepack;

import java.net.*;
import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 *
 * @author damencho
 */
public class DefaultResourcePackActivator
    implements BundleActivator
{
    private Logger logger =
        Logger.getLogger(DefaultResourcePackActivator.class);

    static BundleContext bundleContext;

    // buffer for resource files found
    private static Hashtable<String, Iterator<String>> resourcesFiles =
            new Hashtable<>();

    public void start(BundleContext bc)
    {
        bundleContext = bc;

        DefaultColorPackImpl colPackImpl =
            new DefaultColorPackImpl();

        Hashtable<String, String> props = new Hashtable<>();
        props.put(ResourcePack.RESOURCE_NAME,
                  ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  ColorPack.class.getName(),
                                        colPackImpl,
                                        props);

        DefaultImagePackImpl imgPackImpl =
            new DefaultImagePackImpl();

        Hashtable<String, String> imgProps = new Hashtable<>();
        imgProps.put(ResourcePack.RESOURCE_NAME,
                    ImagePack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  ImagePack.class.getName(),
                                        imgPackImpl,
                                        imgProps);

        DefaultLanguagePackImpl langPackImpl =
            new DefaultLanguagePackImpl();

        Hashtable<String, String> langProps = new Hashtable<>();
        langProps.put(ResourcePack.RESOURCE_NAME,
                    LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  LanguagePack.class.getName(),
                                        langPackImpl,
                                        langProps);

        DefaultSettingsPackImpl setPackImpl =
            new DefaultSettingsPackImpl();

        Hashtable<String, String> setProps = new Hashtable<>();
        setProps.put(ResourcePack.RESOURCE_NAME,
                      SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  SettingsPack.class.getName(),
                                        setPackImpl,
                                        setProps);

        DefaultSoundPackImpl sndPackImpl =
            new DefaultSoundPackImpl();

        Hashtable<String, String> sndProps = new Hashtable<>();
        sndProps.put(ResourcePack.RESOURCE_NAME,
                      SoundPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  SoundPack.class.getName(),
                                        sndPackImpl,
                                        sndProps);

        logger.info("Default resources ... [REGISTERED]");
    }

    public void stop(BundleContext bc)
    {
    }

    /**
     * Finds all properties files for the given path in this bundle.
     *
     * @param path the path pointing to the properties files.
     * @param pattern the pattern to match properties files on.
     * @return An <tt>Iterator</tt> of the found properties files.
     */
    protected static Iterator<String> findResourcePaths(String path,
                                                        String pattern)
    {
        Iterator<String> bufferedResult = resourcesFiles.get(path + pattern);
        if (bufferedResult != null) {
            return bufferedResult;
        }

        ArrayList<String> propertiesList = new ArrayList<>();

        Enumeration<URL> propertiesUrls = bundleContext.getBundle()
            .findEntries(path, pattern, false);

        if (propertiesUrls != null)
        {
            while (propertiesUrls.hasMoreElements())
            {
                URL propertyUrl = propertiesUrls.nextElement();

                // Remove the first slash.
                String propertyFilePath
                    = propertyUrl.getPath().substring(1);

                // Replace all slashes with dots.
                propertyFilePath = propertyFilePath.replaceAll("/", ".");

                propertiesList.add(propertyFilePath);
            }
        }

        Iterator<String> result = propertiesList.iterator();
        resourcesFiles.put(path + pattern, result);

        return result;
    }
}
