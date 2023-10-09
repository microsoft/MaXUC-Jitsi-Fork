/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.resources;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.impl.resources.ResourceManagementActivator;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.service.resources.ImageIconFuture;
import org.jitsi.service.resources.ResourceManagementService;

/**
 * The abstract class for ResourceManagementService. It listens for
 * {@link ResourcePack} that are registered and exposes them later for use by
 * subclasses. It implements default behaviour for most methods.
 */
public abstract class AbstractResourcesService
        implements ResourceManagementService,
        ServiceListener
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(AbstractResourcesService.class);

    /**
     * Resource string for the JDK locale.
     * IETF language tag formatted in electron style (i.e. contains '-' not '_')
     */
    private static final String PROPERTY_JAVA_LOCALE =
            "net.java.sip.communicator.service.resources.JavaLocale";

    /**
     * The OSGI BundleContext
     */
    private BundleContext bundleContext;

    /**
     * Resources for currently loaded <tt>SettingsPack</tt>.
     */
    private Map<String, String> settingsResources;

    /**
     * Currently loaded settings pack.
     */
    private ResourcePack settingsPack = null;

    /**
     * Resources for currently loaded <tt>LanguagePack</tt>.
     */
    private Map<String, String> languageResources;

    /**
     * Currently loaded language pack.
     */
    private LanguagePack languagePack = null;

    /**
     * The {@link Locale} of <code>languageResources</code> so that the caching
     * of the latter can be used when a string with the same <code>Locale</code>
     * is requested.
     */
    private Locale languageLocale;

    /**
     * Resources for currently loaded <tt>ImagePack</tt>.
     */
    private Map<String, String> imageResources;

    /**
     * Currently loaded image packs.
     */
    private Hashtable<String, ImagePack> imagePacks =
            new Hashtable<>();

    /**
     * Resources for currently loaded <tt>ColorPack</tt>.
     */
    private Map<String, String> colorResources;

    /**
     * Currently loaded color pack.
     */
    private ResourcePack colorPack = null;

    /**
     * Resources for currently loaded <tt>SoundPack</tt>.
     */
    private Map<String, String> soundResources;

    /**
     * Currently loaded sound pack.
     */
    private ResourcePack soundPack = null;

    /**
     * Creates an instance of <tt>AbstractResourcesService</tt>.
     *
     * @param bundleContext the OSGi bundle context
     */
    public AbstractResourcesService(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
        bundleContext.addServiceListener(this);

        colorPack =
                getDefaultResourcePack(ColorPack.class.getName(),
                        ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (colorPack != null)
            colorResources = getResources(colorPack);

        ImagePack imagePack = (ImagePack) getDefaultResourcePack(
            ImagePack.class.getName(),
            ImagePack.RESOURCE_NAME_DEFAULT_VALUE);

        if (imagePack != null)
        {
            imagePacks.put(imagePack.getName(), imagePack);
            imageResources = getResources(imagePack);
        }

        // changes the default locale if set in the config
        ConfigurationService confService =
            ServiceUtils.getService( bundleContext, ConfigurationService.class);
        String defaultLocale =
                (String) confService.global().getProperty(DEFAULT_LOCALE_CONFIG);

        if(defaultLocale != null)
        {
            logger.info("Setting JVM locale to default.");
            Locale.setDefault(
                    ResourceManagementServiceUtils.getLocale(defaultLocale));
        }

        // Convert locale from Java format to electron format
        confService.global().setProperty(PROPERTY_JAVA_LOCALE,
                                       Locale.getDefault().toLanguageTag().replace("_","-"));

        languagePack = (LanguagePack) getDefaultResourcePack(
                LanguagePack.class.getName(),
                LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);

        if (languagePack != null)
        {
            languageLocale = Locale.getDefault();
            languageResources = languagePack.getResources(languageLocale);
        }

        settingsPack =
                getDefaultResourcePack(SettingsPack.class.getName(),
                        SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (settingsPack != null)
            settingsResources = getResources(settingsPack);

        soundPack =
                getDefaultResourcePack(SoundPack.class.getName(),
                        SoundPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (soundPack != null)
            soundResources = getResources(soundPack);
    }

    /**
     * Handles all <tt>ServiceEvent</tt>s corresponding to <tt>ResourcePack</tt>
     * being registered or unregistered.
     *
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = bundleContext.getService(
                event.getServiceReference());

        if (!(sService instanceof ResourcePack))
        {
            return;
        }

        ResourcePack resourcePack = (ResourcePack) sService;
        Map<String, String> resources = getResources(resourcePack);

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            logger.info("Resource registered " + resourcePack);

            if (resourcePack instanceof ColorPack)
            {
                logger.debug("Adding new color pack to resources");
                colorResources.putAll(resources);
            }
            else if (resourcePack instanceof ImagePack)
            {
                logger.debug("Adding new image pack to resources");
                imageResources.putAll(resources);
                imagePacks.put(resourcePack.getName(), (ImagePack)resourcePack);
            }
            else if (resourcePack instanceof LanguagePack)
            {
                logger.debug("Adding new language pack to resources");
                languageLocale = Locale.getDefault();
                languageResources.putAll(resources);
            }
            else if (resourcePack instanceof SettingsPack)
            {
                logger.debug("Adding new settings pack to resources");
                settingsResources.putAll(resources);
            }
            else if (resourcePack instanceof SoundPack)
            {
                logger.debug("Adding new sound pack to resources");
                soundResources.putAll(resources);
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            // FIXME: There is a potential bug here that we don't reinstate the
            // default resources when we remove the old ones (in the case where
            // they were overriding the defaults).
            if (resourcePack instanceof ColorPack)
            {
                colorResources.keySet().removeAll(resources.keySet());
            }
            else if (resourcePack instanceof ImagePack)
            {
                imageResources.keySet().removeAll(resources.keySet());
            }
            else if (resourcePack instanceof LanguagePack)
            {
                languageResources.keySet().removeAll(resources.keySet());
            }
            else if (resourcePack instanceof SettingsPack)
            {
                settingsResources.keySet().removeAll(resources.keySet());
            }
            else if (resourcePack instanceof SoundPack)
            {
                soundResources.keySet().removeAll(resources.keySet());
            }
        }
    }

    /**
     * Searches for the <tt>ResourcePack</tt> corresponding to the given
     * <tt>className</tt> and <tt></tt>.
     *
     * @param className The name of the resource class.
     * @param typeName The name of the type we're looking for.
     * For example: RESOURCE_NAME_DEFAULT_VALUE
     * @return the <tt>ResourcePack</tt> corresponding to the given
     * <tt>className</tt> and <tt></tt>.
     */
    protected ResourcePack getDefaultResourcePack(String className,
                                                  String typeName)
    {
        ServiceReference<?>[] serRefs = null;

        String osgiFilter =
                "(" + ResourcePack.RESOURCE_NAME + "=" + typeName + ")";

        try
        {
            serRefs = bundleContext.getServiceReferences(
                    className,
                    osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain resource packs reference.", exc);
        }

        if ((serRefs != null) && (serRefs.length > 0))
        {
            return (ResourcePack) bundleContext.getService(serRefs[0]);
        }
        return null;
    }

    /**
     * Returns the <tt>Map</tt> of (key, value) pairs contained in the given
     * resource pack.
     *
     * @param resourcePack The <tt>ResourcePack</tt> from which we're obtaining
     * the resources.
     * @return the <tt>Map</tt> of (key, value) pairs contained in the given
     * resource pack.
     */
    protected Map<String, String> getResources(ResourcePack resourcePack)
    {
        return resourcePack.getResources();
    }

    /**
     * All the locales in the language pack.
     * @return all the locales this Language pack contains.
     */
    public Iterator<Locale> getAvailableLocales()
    {
        return languagePack.getAvailableLocales();
    }

    /**
     * Returns the string for given <tt>key</tt> for specified <tt>locale</tt>.
     * It's the real process of retrieving string for specified locale.
     * The result is used in other methods that operate on localized strings.
     *
     * @param key the key name for the string
     * @param locale the Locale of the string
     * @return the resources string corresponding to the given <tt>key</tt> and
     * <tt>locale</tt>
     */
    protected String doGetI18String(String key, Locale locale)
    {
        Map<String, String> stringResources;
        if ((locale != null) && locale.equals(languageLocale))
        {
            stringResources = languageResources;
        }
        else
        {
            stringResources
                    = (languagePack == null)
                    ? null
                    : languagePack.getResources(locale);
        }

        String resourceString =
                (stringResources == null) ? null : stringResources.get(key);

        return resourceString;
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key)
    {
        return getI18NString(key, Locale.getDefault());
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string.
     * @param params the parameters to pass to the localized string
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params)
    {
        return getI18NString(key, params, Locale.getDefault());
    }

    /**
     * Returns an internationalized string corresponding to the given key,
     * taking account of a varying quantity that may need to be applied.
     * The quantity is accessed as just SINGULAR or PLURAL, which is applicable
     * to English, but is a simplification that may not apply to other languages.
     *
     * @param key The identifier of the string.
     * @param quantity the quantity to be considered
     * @param params the parameters to pass to the localized string
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NQuantityString(String key, int quantity, String[] params)
    {
        String quantityKey = key + (quantity == 1 ? ".SINGULAR" : ".PLURAL");

        String string = getI18NString(quantityKey, Locale.getDefault());

        if (string == null)
        {
            // We didn't find a quantity specific key, so check the root key
            logger.debug("Missing resource for quantity key: " + quantityKey);
            string = getI18NString(key, Locale.getDefault());
        }

        if (string != null && params != null)
        {
            string = MessageFormat.format(string, (Object[]) params);
        }

        return string;
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @param locale The locale.
     * @return An internationalized string corresponding to the given key and
     * given locale.
     */
    public String getI18NString(String key, Locale locale)
    {
        return processI18NString(
                doGetI18String(key, locale));
    }

    /**
     * Does the additional processing on the resource string. It removes the
     * first "&" (assumed to be marks the mnemonic) and all escape characters.
     *
     * The logic here is as follows:
     *   - Find the first instance of & in the string
     *   - If it is not escaped as "\&", then remove it
     *   - No subsequent ampersands are modified in any way.
     *   - All occurrences of "\" are removed from the string
     *
     * This is because "&" is used by jitsi to indicate the mnemonic to be used
     * for UI controls, so we don't actually want that to show in the text.
     *
     * The logic is a little flawed - it assumes that a mnemonic will always be
     * the first occurrence of "&" in the string, which would not be the case
     * for a cocktail menu of
     * - Gin \& &Tonic
     * - Gin \& &Ginger
     * - Gin \& &Bitter Lemon
     *
     * Rather than use "\&" to escape a literal "&", we seem to have settled on
     * a convention of using "&&" for the first literal "&", on the expectation
     * that this method will strip the first "&".  Strangely though the method
     * that looks for a mnemonic does not make any allowance for literal "&"s.
     *
     * @param resourceString the resource string to be processed
     * @return the processed string
     */
    private String processI18NString(String resourceString)
    {
        if (resourceString == null)
            return null;

        // Look for the first ampersand
        int mnemonicIndex = resourceString.indexOf('&');

        if (mnemonicIndex == 0
                || (mnemonicIndex > 0
                && resourceString.charAt(mnemonicIndex - 1) != '\\'))
        {
            // If it was the first character of the string, then firstPart
            // will be an empty string, otherwise it is the non-empty string
            // that occurs before the non-escaped ampersand.
            String firstPart = resourceString.substring(0, mnemonicIndex);
            String secondPart = resourceString.substring(mnemonicIndex + 1);

            // In either event, we drop the ampersand character
            resourceString = firstPart.concat(secondPart);
        }

        // Look for escape characters
        if (resourceString.indexOf('\\') > -1)
        {
            // Replace all escape characters - the need to have 4 slashes is because
            // Java uses \ as an escape, so the string is really just 2 slashes long,
            // and regex uses the same, so two slashes from Java means the regex matches
            // a single slash character.
            resourceString = resourceString.replaceAll("\\\\", "");
        }

        return resourceString;
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties
     * file.
     * @param params the parameters to pass to the localized string
     * @param locale The locale.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params, Locale locale)
    {
        String resourceString = getI18NString(key, locale);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return null;
        }

        if(params != null)
        {
            resourceString
                    = MessageFormat.format(resourceString, (Object[]) params);
        }

        return resourceString;
    }

    /**
     * Returns the character after the first '&' in the internationalized
     * string corresponding to <tt>key</tt>
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the character after the first '&' in the internationalized
     * string corresponding to <tt>key</tt>.
     */
    public char getI18nMnemonic(String key)
    {
        return getI18nMnemonic(key, Locale.getDefault());
    }

    /**
     * Returns the character after the first '&' in the internationalized
     * string corresponding to <tt>key</tt>
     *
     * @param key The identifier of the string in the resources properties file.
     * @param locale The locale that we'd like to receive the result in.
     * @return the character after the first '&' in the internationalized
     * string corresponding to <tt>key</tt>.
     */
    public char getI18nMnemonic(String key, Locale locale)
    {
        String resourceString = doGetI18String(key, locale);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return 0;
        }

        int mnemonicIndex = resourceString.indexOf('&');
        if (mnemonicIndex > -1 && mnemonicIndex < resourceString.length() - 1)
        {
            return resourceString.charAt(mnemonicIndex + 1);
        }

        return 0;
    }

    /**
     * Returns the string value of the corresponding configuration key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the string of the corresponding configuration key.
     */
    public String getSettingsString(String key)
    {
        return (settingsResources == null) ? null : settingsResources.get(key);
    }

    /**
     * Returns the int value of the corresponding configuration key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the int value of the corresponding configuration key.
     */
    public int getSettingsInt(String key)
    {
        String resourceString = getSettingsString(key);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return 0;
        }

        return Integer.parseInt(resourceString);
    }

    /**
     * Returns an int stored in config, scaled according to the current display
     * settings. Should be used when retrieving default of previous sizes in
     * config
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the int value of the corresponding configuration key, scaled
     *         according to the current display settings
     */
    public int getScaledSize(String key)
    {
        int unscaled = getSettingsInt(key);

        return ScaleUtils.scaleInt(unscaled);
    }

    /**
     * Returns a stream from a given identifier.
     *
     * @param streamKey The identifier of the stream.
     * @return The stream for the given identifier.
     */
    public InputStream getSettingsInputStream(String streamKey)
    {
        return getSettingsInputStream(streamKey, settingsPack.getClass());
    }

    /**
     * Returns a stream from a given identifier, obtained through the class
     * loader of the given resourceClass.
     *
     * @param streamKey The identifier of the stream.
     * @param resourceClass the resource class through which the resource would
     * be obtained
     * @return The stream for the given identifier.
     */
    public InputStream getSettingsInputStream(  String streamKey,
                                                Class<?> resourceClass)
    {
        String path = getSettingsString(streamKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + streamKey);
            return null;
        }

        return resourceClass.getClassLoader().getResourceAsStream(path);
    }

    /**
     * Returns the image path corresponding to the given key.
     *
     * @param key The identifier of the image in the resource properties file.
     * @return the image path corresponding to the given key.
     */
    @Override
    public String getImagePath(String key)
    {
        return (imageResources == null) ? null : imageResources.get(key);
    }

    /**
     * Loads an image icon from a given image identifier.
     *
     * @param imageString The identifier of the image icon.
     * @return The image icon for the given identifier.
     */
    @Override
    public ImageIconFuture getImage(String imageString)
    {
        BufferedImageFuture image = getBufferedImage(imageString);
        return (image == null) ? null : image.getImageIcon();
    }

    /**
     * Loads an image icon from a given filepath.
     *
     * @param filepath The filepath of the image.
     * @return The image icon corresponding to the given filepath.
     */
    public ImageIconFuture getImageFromPath(String filepath)
    {
        BufferedImageFuture image = getBufferedImageFromPath(filepath);
        return (image == null) ? null : image.getImageIcon();
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageString The identifier of the image.
     * @return The image for the given identifier.
     */
    @Override
    public BufferedImageFuture getBufferedImage(String imageString)
    {
        ImageID imageID = new ImageID(imageString);

        // Handover to the ImageLoaderService to get the Image, correctly scaled
        return ResourceManagementActivator.
            getImageLoaderService().getImage(imageID);
    }

    /**
     * Loads an image from a given path.
     *
     * @param imagePath The path of the image.
     * @return The image for the given identifier.
     */
    @Override
    public BufferedImageFuture getBufferedImageFromPath(String imagePath)
    {
        // Handover to the ImageLoaderService to get the Image, correctly scaled
        return ResourceManagementActivator.
            getImageLoaderService().getImageFromPath(imagePath);
    }

    /**
     * Returns the path of the sound corresponding to the given
     * property key.
     *
     * @param soundKey the key, for the sound path
     * @return the path of the sound corresponding to the given
     * property key.
     */
    @Override
    public String getSoundPath(String soundKey)
    {
        return soundResources.get(soundKey);
    }

    /**
     * Resources for currently loaded <tt>ColorPack</tt>.
     *
     * @return the currently color resources
     */
    protected Map<String, String> getColorResources()
    {
        return colorResources;
    }

    /**
     * Currently loaded image pack.
     *
     * @return the currently loaded image pack
     */
    protected Hashtable<String, ImagePack> getImagePacks()
    {
        return imagePacks;
    }

    /**
     * Currently loaded sound pack.
     *
     * @return the currently loaded sound pack
     */
    protected ResourcePack getSoundPack()
    {
        return soundPack;
    }

}
