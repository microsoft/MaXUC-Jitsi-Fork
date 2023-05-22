/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.msofficecomm;

import org.jitsi.util.*;

import com.sun.jna.platform.win32.*;

/**
 * Reading and writing the registry for default application used by
 * Outlook 2010 and higher integration of presence statuses, IM and calls.
 *
 * @author Hristo Terezov
 */
public class DefaultOutlookIntegrationApp
{
    /**
    * The logger.
    */
   private static final Logger logger
       = Logger.getLogger(DefaultOutlookIntegrationApp.class);

    /**
     * The registry key under which the default Outlook integration application
     * is placed.
     */
    private static String REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY
        = "Software\\IM Providers";

    /**
     * The value under which the default Outlook integration application is
     * placed.
     */
    private static String REGISTRY_DEFAULT_OUTLOOK_APPLICATION_VALUE
        = "DefaultIMApp";

    /**
     * The value is used to unset us as the default Outlook integration
     * application.
     */
    private static String REGISTRY_DEFAULT_OUTLOOK_APPLICATION_UNSET_VALUE
        = "None";

    /**
     * The registry key under which the legacy mode setting is placed.
     */
    private static String REGISTRY_LEGACY_MODE_KEY
        = "Software\\Microsoft\\Office\\Outlook\\Call Integration";

    /**
     * The value under which the legacy mode setting is placed.
     */
    private static String REGISTRY_LEGACY_MODE_VALUE
        = "IMApplication";

    /**
     * @return The current default Outlook integration app set in the registry
     */
    static String getDefaultOutlookApp()
    {
        String defaultOutlookApp = "";
        boolean imKeyExists =
                Advapi32Util.registryKeyExists(
                        WinReg.HKEY_CURRENT_USER,
                        REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY);

        if (imKeyExists)
        {
            boolean defaultOutlookAppExistsInRegistry =
                    Advapi32Util.registryValueExists(
                            WinReg.HKEY_CURRENT_USER,
                            REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY,
                            REGISTRY_DEFAULT_OUTLOOK_APPLICATION_VALUE);

            if (defaultOutlookAppExistsInRegistry)
            {
                defaultOutlookApp =
                        Advapi32Util.registryGetStringValue(
                                WinReg.HKEY_CURRENT_USER,
                                REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY,
                                REGISTRY_DEFAULT_OUTLOOK_APPLICATION_VALUE);

                logger.info("Default Outlook integration app = " +
                                    defaultOutlookApp);
            }
            else
            {
                logger.info("No 'DefaultIMApp' registry entry exists");
            }
        }
        else
        {
            logger.info("No IM Providers key exists");
        }

        return defaultOutlookApp;
    }

    /**
     * Checks whether given application is the default Outlook integration
     * application.
     * @param appName the application name.
     * @return whether it is the default Outlook integration application.
     */
    private static boolean isDefaultOutlookApp(String appName)
    {
        logger.info("Checking whether '" + appName +
                                   "' is the default Outlook integration app");

        return getDefaultOutlookApp().equals(appName);
    }

    /**
     * Checks whether we are the default Outlook integration application.
     * @return whether we are the default Outlook integration application.
     */
    public static boolean isDefaultIMApp()
    {
        String thisAppName = getApplicationName();
        logger.debug("This app name = " + thisAppName);
        return isDefaultOutlookApp(thisAppName);
    }

    /**
     * Sets the given application as default Outlook integration application.
     *
     * @param appName the application name
     */
    public static void setDefaultOutlookApp(String appName)
    {
        logger.info("Setting default Outlook integration app to " + appName);

        if (!Advapi32Util.registryKeyExists(
                                     WinReg.HKEY_CURRENT_USER,
                                     REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY))
        {
            Advapi32Util.registryCreateKey(
                                     WinReg.HKEY_CURRENT_USER,
                                     REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY);
        }

        Advapi32Util.registrySetStringValue(
                                    WinReg.HKEY_CURRENT_USER,
                                    REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY,
                                    REGISTRY_DEFAULT_OUTLOOK_APPLICATION_VALUE,
                                    appName);
    }

    /**
     * Sets us as default Outlook integration application.
     */
    public static void setApplicationAsDefaultApp()
    {
        String appName = getApplicationName();
        if(!isDefaultOutlookApp(appName))
        {
            setDefaultOutlookApp(appName);
        }
    }

    /**
     * Unsets us as default Outlook integration application. Overrides the
     * registry value by setting 'None' as the default Outlook integration
     * application.
     */
    public static void unsetDefaultApp()
    {
        if(isDefaultOutlookApp(getApplicationName()))
        {
            setDefaultOutlookApp(
                             REGISTRY_DEFAULT_OUTLOOK_APPLICATION_UNSET_VALUE);
        }

        // For the purposes of legacy mode, we must also delete the Accession
        // Communicator key from the registry.  If this is present but we are
        // trying to run in legacy mode, click-to-dial/IM won't work from
        // within Outlook.
        if (Advapi32Util.registryKeyExists(
                                    WinReg.HKEY_CURRENT_USER,
                                    REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY +
                                                  "\\" + getApplicationName()))
        {
            Advapi32Util.registryDeleteKey(WinReg.HKEY_CURRENT_USER,
                                    REGISTRY_DEFAULT_OUTLOOK_APPLICATION_KEY +
                                                  "\\" + getApplicationName());
        }
    }

    /**
     * Returns the application name.
     * @return the application name
     */
    private static String getApplicationName()
    {
        return MsOfficeCommActivator.getResources().getSettingsString(
            "service.gui.APPLICATION_NAME");
    }

    /**
     * Whether we are using legacy mode or not.  See
     * [indeterminate link] for a full discussion of
     * this.
     * @return true if legacy mode, false otherwise.
     */
    public static boolean isLegacyMode()
    {
        boolean legacyMode = false;

        logger.debug("Checking for legacy mode");

        if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE,
                                           REGISTRY_LEGACY_MODE_KEY))
        {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE,
                                                 REGISTRY_LEGACY_MODE_KEY,
                                                 REGISTRY_LEGACY_MODE_VALUE))
            {
                String legacyValue = Advapi32Util.registryGetStringValue(
                                                   WinReg.HKEY_LOCAL_MACHINE,
                                                   REGISTRY_LEGACY_MODE_KEY,
                                                   REGISTRY_LEGACY_MODE_VALUE);

                // In legacy mode the current application executable will be
                // stored in the registry entry.  We don't have a means of
                // getting that directly but we can assume the it will be the
                // same as the application name up to any whitespace character.
                String appName = getApplicationName().split("\\s+")[0];

                logger.debug("Have legacy mode string " + legacyValue +
                             " and appName " + appName);

                legacyMode = legacyValue.startsWith(appName);
            }
            else
            {
                logger.debug("Legacy mode value doesn't exist");
            }
        }
        else
        {
            logger.debug("Legacy mode key doesn't exist");
        }

        return legacyMode;
    }
}
