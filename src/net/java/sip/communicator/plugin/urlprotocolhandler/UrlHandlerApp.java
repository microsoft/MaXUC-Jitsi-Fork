// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.urlprotocolhandler;

import org.jitsi.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.platform.win32.*;

/**
 * Class for setting and unsetting the app as the handler for SIP and TEL URIs.
 * Does this by playing with the Windows registry.
 * <p>
 * The registry entries must have the following structure.  We support SIP, TEL
 * and CALLTO.  The values set in this class must match those set during the
 * install (see SCRegistrySpec.wxi).
 * <p>
 * http://msdn.microsoft.com/en-us/library/ie/aa767914(v=vs.85).aspx
 * <p>
 * <b>Windows 7 or older</b><br>
 * Note in the example below, we put the keys into <tt>HKEY_CURRENT_USER</tt>
 * rather than <tt>HKEY_CLASSES_ROOT</tt>, and prepend <tt>Software\Classes\
 * <\tt> to the key name.  That's because the running app doesn't have admin
 * privileges and so can't write to <tt>HKEY_CLASSES_ROOT</tt>.  The path below
 * maps over the root values so this behaves the same, but only for the current
 * user.
 * <p><tt>
 * HKEY_CURRENT_USER<br>
 * Software\Classes\sip<br>
 *    (Default) = "URL: SIP Protocol"<br>
 *    URL Protocol = ""<br>
 *    DefaultIcon<br>
 *       (Default) = "<accession icon>"<br>
 *    shell<br>
 *       open<br>
 *          command<br>
 *             (Default) = "<accession exe>" "%1"<br>
 * </tt><p>
 * (and the same for <tt>tel</tt>).
 * <p>
 * <b>Windows 8 or later</b><br>
 * We need to create registry values in our app's URLAssocations key:
 * <p><tt>
 * HKEY_CURRENT_USER<br>
 * Software\<app name>\Capabilities\UrlAssociations<br>
 *    sip = "<app name>.Url.tel"<br>
 *    tel = "<app name>.Url.tel"<br>
 *    callto = "<app name>.Url.tel"<br>
 * </tt><p>
 */
public class UrlHandlerApp
{
    /**
    * The logger.
    */
   private static final Logger logger
       = Logger.getLogger(UrlHandlerApp.class);

   /**
    * Interface that is implemented by all types of Url Registry handlers that
    * provides methods to write to the registry to set/unset our app for handling
    * a Url.
    */
   private interface UrlRegistryHandlerInterface
   {
       /**
        * Updates the registry to set our application as the handler for this URL
        */
       void setAsOsUrlHandlerApp();

       /**
        * Updates the registry to unset our application as the handler for this
        * URL
        */
       void unsetAsOsUrlHandlerApp();

      /**
       * Checks whether this application is the URL Protocol Handler for the
       * protocol type.
       *
       * @return <tt>true</tt> if the registry key exists and is set to this app,
       * otherwise <tt>false</tt>.
       */
      boolean isAppOsUrlHandler();
   }

   /**
    * Different supported URL protocols. These are used for Windows 7 or older.
    */
    private enum UrlProtocols implements UrlRegistryHandlerInterface
    {
        SIP_URI("sip", "URL: SIP Protocol"),
        TEL_URI("tel", "URL: TEL Protocol"),
        CALLTO_URI("callto", "URL: CALLTO protocol");

        /**
         * Top-level key for this registry entry
         */
        public final String keyName;

        /**
         * Key name for the icon property
         */
        public final String iconKeyName;

        /**
         * Key name for the 'shell' key
         */
        public final String shellKeyName;

        /**
         * Key name for the 'shell > open' key
         */
        public final String shellOpenKeyName;

        /**
         * Key name for the 'shell > open > command' key
         */
        public final String shellOpenCommandKeyName;

        /**
         * Default name in the registry
         */
        public final String defaultName;

        /*
         * Strings for the various registry entries (keys and values).
         */
        private static final String REG_VALUE_URL_NAME = "URL Protocol";
        private static final String REG_VALUE_ICON_NAME = "DefaultIcon";
        private static final String REG_VALUE_SHELL_KEY = "shell";
        private static final String REG_VALUE_SHELL_OPEN_KEY =
                                                     REG_VALUE_SHELL_KEY + "\\open";
        private static final String REG_VALUE_SHELL_OPEN_COMMAND_KEY =
                                             REG_VALUE_SHELL_OPEN_KEY + "\\command";

        private static final String REG_VALUE_ICON_VALUE;

        /**
         * Name of the default value for any registry entry (and empty string)
         */
        private static final String REG_DEFAULT_VALUE_NAME = "";

        /**
         * Command that will be run when a SIP or TEL URI is clicked.  We obtain the
         * app name from the registry - it is set during the install.
         */
        private static final String REG_VALUE_COMMAND_VALUE;

        /**
         * The property used in the configuration file to store the full path to the
         * application
         */
        private static String APPLICATION_PATH_NAME_PROP =
                                  "net.java.sip.communicator.APPLICATION_PATH_NAME";

        /*
         * Set <tt>REG_VALUE_ICON_VALUE</tt> and <tt>REG_VALUE_COMMAND_VALUE</tt>,
         * both of which are obtained from the registry (and were set there by the
         * installer).
         */
        static
        {
            String appPath = getInstallerRegistryValue("app-path");

            if (appPath != null)
            {
                logger.debug("Got appPath from registry: " + appPath);
                REG_VALUE_COMMAND_VALUE = "\"" + appPath + "\" \"%1\"";
                UrlProtocolHandlerActivator.getConfigurationService().global().setProperty(
                                 APPLICATION_PATH_NAME_PROP, "\"" + appPath + "\"");
            }
            else
            {
                // We need to set a value that's syntactically correct
                logger.error("Couldn't find app path in registry");
                REG_VALUE_COMMAND_VALUE = "unknown";
            }

            String iconPath = getInstallerRegistryValue("icon-path");

            if (iconPath != null)
            {
                logger.debug("Got iconPath from registry: " + iconPath);
                REG_VALUE_ICON_VALUE = iconPath;
            }
            else
            {
                // We need to set a value that's syntactically correct
                logger.error("Couldn't find icon path in registry");
                REG_VALUE_ICON_VALUE = "unknown";
            }
        }

        UrlProtocols(String keyName, String defaultName)
        {
            this.keyName = "Software\\Classes\\" + keyName;
            this.iconKeyName = this.keyName + "\\" + REG_VALUE_ICON_NAME;
            this.shellKeyName = this.keyName + "\\" + REG_VALUE_SHELL_KEY;
            this.shellOpenKeyName =
                                 this.keyName + "\\" + REG_VALUE_SHELL_OPEN_KEY;
            this.shellOpenCommandKeyName =
                         this.keyName + "\\" + REG_VALUE_SHELL_OPEN_COMMAND_KEY;
            this.defaultName = defaultName;
        }

        /**
         * Sets as the URL protocol handler application for this protocol.
         */
        public void setAsOsUrlHandlerApp()
        {
            /*
             * A reminder of the registry structure - see class comment for source.
             *
             * HKEY_CURRENT_USER
             * Software\Classes\sip                           | key
             *    (Default) = "URL: SIP Protocol"             |  value
             *    URL Protocol = ""                           |  value
             *    DefaultIcon                                 | key
             *       (Default) = "<accession icon>"           |  value
             *    shell                                       | key
             *       open                                     | key
             *          command                               | key
             *             (Default) = "<accession exe>" "%1" |  value
             */
            if (!this.isAppOsUrlHandler())
            {
                logger.debug("Setting Accession as URL Protocol Handler for " + this);

                // First create all the keys...
                String[] keys = {this.keyName,
                                 this.iconKeyName,
                                 this.shellKeyName,
                                 this.shellOpenKeyName,
                                 this.shellOpenCommandKeyName};

                for (String key : keys)
                {
                    try
                    {
                        Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER,
                                                       key);
                    }
                    catch (Win32Exception wex)
                    {
                        logger.error("Failed to create registry key: " + key, wex);
                    }
                }

                // ... then set the values.  A (Default) value is set using a value
                // name of an empty string.
                try
                {
                    Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER,
                                                        this.keyName,
                                                        REG_DEFAULT_VALUE_NAME,
                                                        this.defaultName);
                    Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER,
                                                        this.keyName,
                                                        REG_VALUE_URL_NAME,
                                                        "");
                    Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER,
                                                        this.iconKeyName,
                                                        REG_DEFAULT_VALUE_NAME,
                                                        REG_VALUE_ICON_VALUE);
                    Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER,
                                               this.shellOpenCommandKeyName,
                                               REG_DEFAULT_VALUE_NAME,
                                               REG_VALUE_COMMAND_VALUE);

                }
                catch (Win32Exception wex)
                {
                    logger.error("Failed to set registry values", wex);
                }
            }
        }

        @Override
        public void unsetAsOsUrlHandlerApp()
        {
            if (this.isAppOsUrlHandler())
            {
                // Must delete all sub-keys before top-level keys
                logger.debug(
                    "Unsetting Accession as URL Protocol Handler for " + this);
                String[] keys = {this.shellOpenCommandKeyName,
                                 this.shellOpenKeyName,
                                 this.shellKeyName,
                                 this.iconKeyName,
                                 this.keyName};
                for (String key : keys)
                {
                    try
                    {
                        Advapi32Util.registryDeleteKey(WinReg.HKEY_CURRENT_USER,
                                                       key);
                    }
                    catch (Win32Exception wex)
                    {
                        logger.error("Couldn't delete registry key: " + key, wex);
                    }
                }
            }
        }

        @Override
        public boolean isAppOsUrlHandler()
        {
            boolean isHandler = false;
            boolean handlerRegExists = Advapi32Util.registryKeyExists(
                WinReg.HKEY_CURRENT_USER, this.shellOpenCommandKeyName);

            if (handlerRegExists)
            {
                logger.debug("Handler exists for " + this.keyName);

                // We are the handler if the current handler is set to run the AD
                // client (as opposed to any other program).
                if(Advapi32Util.registryValueExists(
                                              WinReg.HKEY_CURRENT_USER,
                                              this.shellOpenCommandKeyName,
                                              REG_DEFAULT_VALUE_NAME))
                {
                    isHandler = REG_VALUE_COMMAND_VALUE.equals(
                                Advapi32Util.registryGetStringValue(
                                               WinReg.HKEY_CURRENT_USER,
                                               this.shellOpenCommandKeyName,
                                               REG_DEFAULT_VALUE_NAME));
                }
            }
            else
            {
                logger.debug("No URL Protocol registry entry for " + this);
            }

            logger.debug("isHandler? " + isHandler);
            return isHandler;
        }
    }

    /**
     * URls that we should associate our app with to handle calls.
     * UrlAssociations are used for Windows 8 or later.
     */
    private enum UrlAssociations implements UrlRegistryHandlerInterface
    {
        SIP_URI("sip"),
        TEL_URI("tel"),
        CALLTO_URI("callto");

        public final String protocolName;

        private static final String REG_VALUE_APP_URL_ASSOCIATIONS_KEY;

        private static final String REG_VALUE_APP_URL_ASSOCIATIONS_VALUE;

        static
        {
            String appName = UrlProtocolHandlerActivator.getResources().
                getI18NString("service.gui.APPLICATION_NAME_ASCII");

            REG_VALUE_APP_URL_ASSOCIATIONS_KEY = "Software\\" + appName +
                "\\Capabilities\\UrlAssociations";

            REG_VALUE_APP_URL_ASSOCIATIONS_VALUE = appName + ".Url.tel";

        }

        UrlAssociations(String protocolName)
        {
            this.protocolName = protocolName;
        }

        @Override
        public void setAsOsUrlHandlerApp()
        {
            /*
             * Registry structure
             *
             * HKEY_CURRENT_USER
             * Software\<app name>\Capabilities\UrlAssociations
             *    sip = "<app name>.Url.tel"
             *    tel = "<app name>.Url.tel"
             *    callto = "<app name>.Url.tel"
             */
            if (!this.isAppOsUrlHandler())
            {
                logger.debug("Setting Accession as URL Protocol Handler for " + this);

                try
                {
                    Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER,
                                                        REG_VALUE_APP_URL_ASSOCIATIONS_KEY,
                                                        this.protocolName,
                                                        REG_VALUE_APP_URL_ASSOCIATIONS_VALUE);

                }
                catch (Win32Exception wex)
                {
                    logger.error("Failed to set registry valuefor " +
                        this.protocolName + ": " + wex);
                }
            }
        }

        @Override
        public void unsetAsOsUrlHandlerApp()
        {
            if (this.isAppOsUrlHandler())
            {
                logger.debug(
                    "Unsetting Accession as URL Protocol Handler for " + this);

                try
                {
                    Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER,
                        REG_VALUE_APP_URL_ASSOCIATIONS_KEY,
                        this.protocolName);
                }
                catch (Win32Exception wex)
                {
                    logger.error("Failed to delete registry valuefor " +
                        this.protocolName + ": " + wex);
                }
            }
        }

        @Override
        public boolean isAppOsUrlHandler()
        {
            boolean isHandler = false;
            boolean handlerRegExists = Advapi32Util.registryKeyExists(
                WinReg.HKEY_CURRENT_USER, REG_VALUE_APP_URL_ASSOCIATIONS_KEY);

            if (handlerRegExists)
            {
                // We are the handler if the current handler is set to run the AD
                // client (as opposed to any other program).
                isHandler = Advapi32Util.registryValueExists(
                                              WinReg.HKEY_CURRENT_USER,
                                              REG_VALUE_APP_URL_ASSOCIATIONS_KEY,
                                              this.protocolName);
            }
            else
            {
                logger.debug("No Registry key for URL Assocations");
            }

            logger.debug("isHandler? " + isHandler);
            return isHandler;
        }
    }

    // List of all UrlHandlerAppRegistryInterface that represend all the Urls
    // that we can handle
    private static List<UrlRegistryHandlerInterface> urlRegistryHandlers;

    // Populate urlHandlerAppRegistryEntries with all the possible protocols and
    // URLAssociations.
    static
    {
        urlRegistryHandlers = new ArrayList<>();
        urlRegistryHandlers.addAll(Arrays.asList(UrlProtocols.values()));
        urlRegistryHandlers.addAll(Arrays.asList(UrlAssociations.values()));
    }

    /**
     * Finds registry values set by the Accession installer
     * @param key The name of the entry to find
     * @return The value of the key, if present, else <tt>null</tt>.
     */
    private static String getInstallerRegistryValue(String key)
    {
        String value = null;

        // Note that we must use the ASCII name, rather than the normal product
        // name, as the client is installed under the ASCII name.
        String appName = UrlProtocolHandlerActivator.getResources().
                            getI18NString("service.gui.APPLICATION_NAME_ASCII");
        String appRegistryKey = "SOFTWARE\\" + appName;
        logger.debug("Looking for registry entry: " + appRegistryKey);

        boolean keyExists = Advapi32Util.registryKeyExists(
                                     WinReg.HKEY_CURRENT_USER, appRegistryKey);

        if (keyExists)
        {
            boolean appPathExists = Advapi32Util.registryValueExists(
                                                    WinReg.HKEY_CURRENT_USER,
                                                    appRegistryKey,
                                                    key);

            if (appPathExists)
            {
                value = Advapi32Util.registryGetStringValue(
                                                    WinReg.HKEY_CURRENT_USER,
                                                    appRegistryKey,
                                                    key);
            }
        }

        return value;
    }

    /**
     * Checks whether this application is the handler for all Urls
     * registered with the OS.
     * @return whether it is the handler for all Urls.
     */
    public static boolean isAppOsUrlHandler()
    {
        boolean isHandler = true;
        for (UrlRegistryHandlerInterface urlRegistryHandler : urlRegistryHandlers)
        {
            if (!urlRegistryHandler.isAppOsUrlHandler())
            {
                isHandler = false;
                break;
            }
        }
        logger.debug("Is URL handler for all protocols? " + isHandler);
        return isHandler;
    }

    /**
     * Sets us as the URL protocol handler application for all supported Urls.
     */
    public static void setAsOsUrlHandlerApp()
    {
        logger.info("Setting App as URL Protocol Handler");

        for (UrlRegistryHandlerInterface urlRegistryHandler : urlRegistryHandlers)
        {
            urlRegistryHandler.setAsOsUrlHandlerApp();
        }
    }

    /**
     * Unsets us as the URL protocol handler application for all supported
     * Urls.
     */
    public static void unsetAsOsUrlHandlerApp()
    {
        logger.info("Unsetting App as URL Protocol Handler");

        for (UrlRegistryHandlerInterface urlRegistryHandler : urlRegistryHandlers)
        {
            urlRegistryHandler.unsetAsOsUrlHandlerApp();
        }
    }
}
