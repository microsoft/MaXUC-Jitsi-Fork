/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.launcher;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;
import org.apache.felix.main.Main;

import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ScStdOut;
import net.java.sip.communicator.util.launchutils.LaunchArgHandler;
import net.java.sip.communicator.util.launchutils.SipCommunicatorLock;
import org.jitsi.util.StringUtils;

/**
 * Starts the SIP Communicator.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Emil Ivov
 * @author Sebastien Vincent
 */
public class SIPCommunicator
{
    private static final Logger logger = Logger.getLogger(SIPCommunicator.class);

    /**
     * The name of the property that stores our home dir location.
     */
    public static final String PNAME_SC_HOME_DIR_LOCATION =
            "net.java.sip.communicator.SC_HOME_DIR_LOCATION";

    /**
     * The name of the property that stores our home dir name.
     */
    public static final String PNAME_SC_HOME_DIR_NAME =
            "net.java.sip.communicator.SC_HOME_DIR_NAME";

    /**
     * The name of the property that stores the previous app name, if the name
     * of the app has changed over upgrade.
     */
    public static final String PNAME_SC_OLD_APP_NAME =
            "net.java.sip.communicator.SC_OLD_APP_NAME";

    /**
     * Configuration property name for the host port we're connecting to.
     */
    private static final String HOSTPORT_PROPERTY =
            "net.java.sip.communicator.plugin.wispa.HOSTPORT";

    /**
     * Configuration property name for the WISPA keys directory.
     */
    public static final String WISPA_KEYS_DIR_PROPERTY =
            "net.java.sip.communicator.plugin.wispa.KEYS_DIR";

    /**
     * Configuration property name for the WISPA client key store passphrase.
     */
    public static final String WISPA_CLIENT_PASSPHRASE_PROPERTY =
            "net.java.sip.communicator.plugin.wispa.CLIENT_PASSPHRASE";

    /**
     * Configuration property name for the WISPA client key store filename.
     */
    public static final String WISPA_CLIENT_KEY_STORE_PROPERTY =
            "net.java.sip.communicator.plugin.wispa.CLIENT_KEY_STORE";

    /**
     * Configuration property name for the WISPA server key store filename.
     */
    public static final String WISPA_SERVER_KEY_STORE_PROPERTY =
            "net.java.sip.communicator.plugin.wispa.SERVER_KEY_STORE";

    /**
     * Configuration property name for the WISPA server trust store filename.
     */
    public static final String WISPA_SERVER_TRUST_STORE_PROPERTY =
            "net.java.sip.communicator.plugin.wispa.SERVER_TRUST_STORE";

    /**
     * Configuration property name for the WISPA server certificate filename.
     */
    public static final String WISPA_SERVER_CERTIFICATE_PROPERTY =
            "net.java.sip.communicator.plugin.wispa.SERVER_CERTIFICATE";

    private static final String WISPA_DIR_NAME = "wispa";
    private static final String WISPA_SERVER_KEY_STORE = "server.ks.pfx";
    private static final String WISPA_SERVER_TRUST_STORE = "server.ts.pfx";
    private static final String WISPA_SERVER_CERTIFICATE = "server.cert";
    private static final String WISPA_CLIENT_KEY_STORE = "client.ks.pfx";

    /**
     * The currently active name.
     */
    private static String overridableDirName = "Jitsi";

    /**
     * Legacy home directory names that we can use if current dir name
     * is the currently active name (overridableDirName).
     */
    private static String[] legacyDirNames =
        {".sip-communicator", "SIP Communicator"};

    /**
     * Name of the possible configuration file names (used under macosx).
     */
    private static String[] legacyConfigurationFileNames =
        {"sip-communicator.properties", "jitsi.properties",
         "sip-communicator.xml", "jitsi.xml"};

    /**
     * Message that will be logged to record an attempt to migrate the app data
     * folder. Required because the migration takes place before the logger is
     * created.
     */
    private static String mAppDataMigrationLogMessage = "";

    /**
     * Message that will be logged to record an attempt to migrate the saved
     * calls/meetings folder. Required because the migration takes place before
     * the logger is created.
     */
    private static String mRecordingsDirectoryMigrationLogMessage = "";

    /**
     * As logging is uninitialised until after the start-up process, we must
     * store log messages as strings to print later. We use this enum to
     * determine which operation we are logging (stored in the respective string#
     * above).
     */
    private enum UserDataFolderType
    {
        APP_DATA,
        RECORDINGS
    }

    /**
     * The name of a previous version of the app, if defined in branding.
     */
    private static String oldAppName = System.getProperty(PNAME_SC_OLD_APP_NAME);

    /**
     * The current name of the app.
     */
    private static String appName = System.getProperty(PNAME_SC_HOME_DIR_NAME);

    /**
     * DO NOT use the OSUtils values for Windows/Mac in main() or methods called from it -
     * loading that class at this point will load logging configuration from the libjitsi
     * logger that prevents the first logs from here.
     */
    private static String OS_NAME = System.getProperty("os.name");

    /**
     * For local ant builds we use a pre-defined port. This is because a local
     * ant build will be running alongside an Electron instance launched with
     * npm. This means there's no executable that we can pass a port to at
     * runtime so we need Electron and Java to use a pre-defined port in this
     * instance.
     *
     * This MUST match the port defined in the Electron codebase.
     */
    private static final int PORT_DEFAULT = 9092;

    /**
     * When attempting to find an available port, we restrict our search to a
     * range of ports that are unlikely to be in use.
     */
    private static final int PORT_RANGE_START = 9100;
    private static final int PORT_RANGE_END = 9999;

    /**
     * Starts the SIP Communicator.
     *
     * @param args command line args if any
     *
     * @throws Exception whenever it makes sense.
     */
    public static void main(String[] args) throws Exception
    {
        setSystemProperties();

        /*
         * SC_HOME_DIR_* are specific to the OS so make sure they're configured
         * accordingly before any other application-specific logic depending on
         * them starts (e.g. Felix).
         */
        // Don't add logging before we set the home dir.
        setScHomeDir();

        // Similarly for potentially moving the recorded calls/meetings folder
        // in the event of a name change.
        setRecordingsDir();

        // this needs to be set before any DNS lookup is run
        File f = new File(System.getProperty(PNAME_SC_HOME_DIR_LOCATION),
                          System.getProperty(PNAME_SC_HOME_DIR_NAME) + File.separator + ".usednsjava");

        if (f.exists())
        {
            System.setProperty("sun.net.spi.nameservice.provider.1", "dns,dnsjava");
        }

        //first - pass the arguments to our arg handler
        LaunchArgHandler argHandler = LaunchArgHandler.getInstance();
        int argHandlerRes = argHandler.handleArgs(args);

        if ( argHandlerRes == LaunchArgHandler.ACTION_EXIT
             || argHandlerRes == LaunchArgHandler.ACTION_ERROR)
        {
            System.exit(argHandler.getErrorCode());
        }

        logger.error("*******************************************************");
        logger.error("*               Starting new instance!                *");
        logger.error("*******************************************************");
        logger.error(String.format(
            "JRE: %s %s v%s %s",
            System.getProperty("java.vendor"),
            System.getProperty("java.runtime.name"),
            System.getProperty("java.version"),
            System.getProperty("os.arch")
        ));

        for (final String arg : args)
        {
            logger.error("With arg: " + arg);
        }

        // If we attempted to rename the user data folder due to an app name
        // change log the attempt now.
        if ((mAppDataMigrationLogMessage != null) &&
            (!mAppDataMigrationLogMessage.equals("")))
        {
            logger.info("App data migration message: " + mAppDataMigrationLogMessage);
        }

        // If we attempted to rename the recorded calls folder due to an app name
        // change log the attempt now.
        if ((mRecordingsDirectoryMigrationLogMessage != null) &&
                (!mRecordingsDirectoryMigrationLogMessage.equals("")))
        {
            logger.info("Recordings migration message: " + mRecordingsDirectoryMigrationLogMessage);
        }

        logger.info("Default JVM locale = " + Locale.getDefault());

        //lock our config dir so that we would only have a single instance of
        //sip communicator, no matter how many times we start it (use mainly
        //for handling sip: uris after starting the application)
        if (argHandlerRes != LaunchArgHandler.ACTION_CONTINUE_LOCK_DISABLED)
        {
            switch (new SipCommunicatorLock().tryLock(args))
            {
                case SipCommunicatorLock.LOCK_ERROR:
                    logger.error("Failed to lock SIP Communicator's "
                                    + "configuration directory.\n"
                                    + "Try launching with the --multiple param.");
                    System.exit(SipCommunicatorLock.LOCK_ERROR);
                    break;
                case SipCommunicatorLock.ALREADY_STARTED:
                    logger.error(
                        "SIP Communicator is already running and will "
                        + "handle your parameters (if any).\n"
                        + "Parameters = " + Arrays.toString(args) + "\n"
                        + "Launch with the --multiple param to override this "
                        + "behaviour.");

                    //we exit with success because for the user that's what it is.
                    System.exit(SipCommunicatorLock.SUCCESS);
                    break;
                case SipCommunicatorLock.SUCCESS:
                    //Successfully locked, continue as normal.
                    break;
            }
        }

        // Must call after obtaining SipCommunicatorLock to avoid creating orphaned UI processes
        startElectronUI();

        /*
         * Load Jabra and Plantronics Libraries. The libraries are loaded here as a workaround as loading them after Accession has started results
         * in Accession not being able to find the libraries. However, we wait until determining we're the main instance, to avoid loading them
         * if we're only going to pass off the URI to another instance, above. This only affects Windows.
         */
        if (isWindows())
        {
            try
            {
                System.loadLibrary("libjabra");
            }
            catch (Error | Exception e)
            {
                logger.error("Unable to load libjabra library: " +
                    e.getMessage());
            }

            try
            {
                System.loadLibrary("libpltwrapper");
            }
            catch (Error | Exception e)
            {
                logger.error("Unable to load libpltwrapper library: " +
                    e.getMessage());
            }
        }

        //there was no error, continue;
        System.setOut(new ScStdOut(System.out));
        Main.main(new String[0]);
    }

    private static boolean isMac()
    {
        if (OS_NAME != null)
        {
            return OS_NAME.startsWith("Mac");
        }
        else
        {
            return false;
        }
    }

    private static boolean isWindows()
    {
        if (OS_NAME != null)
        {
            return OS_NAME.startsWith("Windows");
        }
        else
        {
            return false;
        }
    }

    /**
     * Sets the system properties net.java.sip.communicator.SC_HOME_DIR_LOCATION
     * and net.java.sip.communicator.SC_HOME_DIR_NAME (if they aren't already
     * set) in accord with the OS conventions specified by the name of the OS.
     *
     * Please leave the access modifier as package (default) to allow launch-
     * wrappers to call it.
     */
    static void setScHomeDir()
    {
        /*
         * Though we'll be setting the SC_HOME_DIR_* property values depending
         * on the OS running the application, we have to make sure we are
         * compatible with earlier releases i.e. use
         * ${user.home}/.sip-communicator if it exists (and the new path isn't
         * already in use).
         */
        String location = System.getProperty(PNAME_SC_HOME_DIR_LOCATION);

        boolean isHomeDirnameForced = (appName != null);

        if ((location == null) || (appName == null))
        {
            String defaultLocation = System.getProperty("user.home");
            String defaultName = ".jitsi";

            // Whether we should check legacy names
            // 1) when such name is not forced we check
            // 2) if such is forced and is the overridableDirName check it
            //      (the later is the case with name transition SIP Communicator
            //      -> Jitsi or Accession Communicator -> MaX UC)
            boolean checkLegacyDirNames = (appName == null) || appName.equals(overridableDirName);

            if (isMac())
            {
                if (location == null)
                {
                    location = System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support";
                }
                if (appName == null)
                {
                    appName = "Jitsi";
                }
            }
            else if (isWindows())
            {
                /*
                 * Primarily important on Vista because Windows Explorer opens
                 * in %USERPROFILE% so .sip-communicator is always visible. But
                 * it may be a good idea to follow the OS recommendations and
                 * use APPDATA on pre-Vista systems as well.
                 */
                if (location == null)
                {
                    location = System.getenv("APPDATA");
                }
                if (appName == null)
                {
                    appName = "Jitsi";
                }
            }

            /* If there're no OS specifics, use the defaults. */
            if (location == null)
            {
                location = defaultLocation;
            }
            if (appName == null)
            {
                appName = defaultName;
            }

            /*
             * As it was noted earlier, make sure we're compatible with previous
             * releases. If the home dir name is forced (set as system property)
             * doesn't look for the default dir.
             */
            if (!isHomeDirnameForced
                && (!new File(location, appName).isDirectory())
                && new File(defaultLocation, defaultName).isDirectory())
            {
                location = defaultLocation;
                appName = defaultName;
            }

            // If we need to check legacy names and there is no current home dir already created
            if (checkLegacyDirNames && !checkHomeFolderExist(location, appName))
            {
                // now check whether some of the legacy dir names
                // exists, and use it if exist
                for (int i = 0; i < legacyDirNames.length; i++)
                {
                    // check the platform specific directory
                    if (checkHomeFolderExist(location, legacyDirNames[i]))
                    {
                        appName = legacyDirNames[i];
                        break;
                    }

                    // now check it and in the default location
                    if (checkHomeFolderExist(defaultLocation, legacyDirNames[i]))
                    {
                        appName = legacyDirNames[i];
                        location = defaultLocation;
                        break;
                    }
                }
            }

            System.setProperty(PNAME_SC_HOME_DIR_LOCATION, location);
            System.setProperty(PNAME_SC_HOME_DIR_NAME, appName);
        }

        // If the app name has changed over upgrade and the new home directory
        // hasn't been created yet, move the old user data to the new home directory.
        // Check that an old app name has been set, and is not equal to the
        // current app name, and also that the new app doesn't already have a
        // full directory - we check for sip-communicator.bin, as the parent
        // folder and log folder are created during install.
        if (!StringUtils.isNullOrEmpty(oldAppName) &&
            !(oldAppName.equals(appName)) &&
            !(new File(location, appName + "\\sip-communicator.bin").isDirectory()))
        {
            migrateExistingUserFolder(location, oldAppName, appName,
                                      UserDataFolderType.APP_DATA);
        }

        // when we end up with the home dirs, make sure we have log dir
        File logDirectory = new File(location, appName + File.separator + "log");
        logDirectory.mkdirs();

        System.out.println("Log directory is " + logDirectory);
    }

    /**
     * As well as migrating user data during a name change, we might have to
     * move a user's recorded calls/meetings folder, by default saved in
     * {user.home}/{application.name}. The user may have changed this option,
     * in which case this method simply deletes the original default folder.
     */
    @VisibleForTesting
    static void setRecordingsDir()
    {
        String location = System.getProperty("user.home");
        if (!StringUtils.isNullOrEmpty(oldAppName) &&
            !(oldAppName.equals(appName)) &&
            (new File(location, oldAppName)).exists() &&
            !(new File(location, appName).isDirectory()))
        {
            migrateExistingUserFolder(location, oldAppName, appName,
                                      UserDataFolderType.RECORDINGS);
        }
    }

    /**
     * Checks whether home folder exists.
     * Special situation checked under macosx, due to created folder
     * of the new version of the updater we may end up with our
     * settings in 'SIP Communicator' folder and having 'Jitsi' folder
     * created by the updater(its download location).
     * So we check not only the folder exist but whether it contains
     * any of the known configuration files in it.
     *
     * @param parent the parent folder
     * @param name the folder name to check.
     * @return whether folder exists.
     */
    static boolean checkHomeFolderExist(String parent, String name)
    {
        if (isMac())
        {
            for (int i = 0; i < legacyConfigurationFileNames.length; i++)
            {
                if (new File(new File(parent, name), legacyConfigurationFileNames[i]).exists())
                {
                    return true;
                }
            }

            return false;
        }

        return new File(parent, name).isDirectory();
    }

    /**
     * Sets some system properties specific to the OS that needs to be set at
     * the very beginning of a program (typically for UI related properties,
     * before AWT is launched).
     */
    private static void setSystemProperties()
    {
        // setup here all system properties that need to be initialized at
        // the very beginning of an application
        if (isWindows())
        {
            // disable Direct 3D pipeline (used for fullscreen) before
            // displaying anything (frame, ...)
            System.setProperty("sun.java2d.d3d", "false");
        }
        else if (isMac())
        {
            // On Mac OS X when switch in fullscreen, all the monitors goes
            // fullscreen (turns black) and only one monitors has images
            // displayed. So disable this behavior because somebody may want
            // to use one monitor to do other stuff while having other ones with
            // fullscreen stuff.
            System.setProperty("apple.awt.fullscreencapturealldisplays", "false");
        }

        // This property was set to 'true' before V2.31, as we do not support
        // IPv6. In V2.31, it was set to 'false' (the Java default for the setting)
        // because a value of 'true' interacted badly with JDK 11 (no connections
        // could be made). This means Java now uses an underlying IPv6 stack if
        // available. However, we still do not officially support using IPv6.
        System.setProperty("java.net.preferIPv4Stack", "false");
    }

    /**
     * Renames a given directory to use the new app name, e.g. app data or
     * recorded meetings. Used when the app name has changed over upgrade.  If
     * files are in use then attempts a copy instead.
     *
     * @param location The parent directory of the user data directory.
     * @param oldName The old name of the user data directory.
     * @param newName The new name of the user data directory.
     * @param folderType What message we want to write logs to, depending on what
     *                type of file transfer is being made.
     */
    private static void migrateExistingUserFolder(String location,
                                                  String oldName,
                                                  String newName,
                                                  UserDataFolderType folderType)
    {
        String logMessages = "";
        logMessages += "Attempting to migrate user data: ";
        Path oldPath = Paths.get(location, oldName);
        String result = "Success";

        File oldDir = new File(location, oldName);
        File newDir = new File(location, newName);

        try
        {
            // File.rename and Files.move look the same, but I've seen
            // move() move half a directory when a file is locked in the
            // source.   Stackoverflow wasn't helpful, so I'm assuming this is
            // better.
            if (!oldDir.renameTo(newDir))
            {
                throw new IOException("Failed to rename directory " + oldName + " to " + newName);
            }

            // The log file doesn't exist yet, so save a message to be logged later.
            logMessages += "Renamed user data directory in " + location + " from " + oldName + " to " + newName;
        }
        catch (IOException e)
        {
            if (!Files.exists(oldPath))
            {
                result = "Error - Couldn't find source directory.";
            }
            else if (Files.exists(oldPath.resolve(newName)))
            {
                result = "Error - Destination directory already existed.";
            }
            else
            {
                result = "Error.";
            }
            result += " IOException: " + e.getMessage();

            // The log file doesn't exist yet, so save a message to be logged later.
            logMessages += "Failed to rename user data directory in "
                + location + " from " + oldName + " to " + newName + ". Result: " + result + "\r\n";

            // OK, so that didn't work.  Now try a copy which is ugly,
            // but as a backup will have to do.
            logMessages += "Attempting copy....\r\n";

            try
            {
                copyAndDeleteDirectory("", oldDir, newDir);
            }
            catch (IOException ex)
            {
                result += " IOException copying " + ex.getMessage();
                logMessages += result;
            }
        }
        logMessages += "Failed to migrate user data due to an unknown reason.";

        // As we can only log once the logger has been created, we set the
        // persistent string to match our log messages here.
        if (folderType == UserDataFolderType.APP_DATA)
        {
            mAppDataMigrationLogMessage = logMessages;
        }
        else if (folderType == UserDataFolderType.RECORDINGS)
        {
            mRecordingsDirectoryMigrationLogMessage = logMessages;
        }
    }

    /**
     * True if we are using a local ant client.  This is a cheap way to check
     * this since the DIR_NAME is Jitsi if and only if the client is running
     * locally from ant.
     */
    private static boolean isLocalAntBuild()
    {
        return "Jitsi".equals(appName);
    }

    /**
     * Starts the electron client.
     * Must be called after the SIPCommunicatorLock is obtained to avoid creating
     * orphaned processes. THIS IS DUPLICATED IN ElectronApiConnector.java because:
     * - sc-launcher (this bundle) starts before all other bundles, so can't use other services
     * - sc-launcher doesn't have a service, so other bundles can't use it
     * IF YOU MAKE CHANGES HERE, MAKE THE SAME CHANGES THERE AS WELL.
     */
    public static void startElectronUI()
    {
        // The SC_HOME_DIR_NAME is passed in on the command line, and taken from the
        // ASCII product name, which is the name electron uses for its executable.
        String electronAppName = System.getProperty(PNAME_SC_HOME_DIR_NAME);

        // Calculate the port that we want to host the web server on. Once we
        // know what port we are going to use we save it in properties so
        // that we can access it in ElectronAPIConnector which actually sets
        // up the web server and passes it as an argument to the Electron
        // executable so that it knows what port to connect to.
        int port = getPort();
        System.setProperty(HOSTPORT_PROPERTY, String.valueOf(port));

        // Generate the required config for WISPA keys and certificates, but
        // only if we're running as part of an installed application.  We do not
        // pass this information to the Electron app yet, as the referenced
        // certificates won't get created until the Electron API connector starts.
        setupWispaPropertiesIfRequired();

        // We don't want to try to start the Electron UI if we're using a local
        // ant client, as there won't be a bundled Electron UI app that we can
        // start.  This is a cheap way to check this since the DIR_NAME is Jitsi
        // if and only if the client is running locally from ant.
        if (isLocalAntBuild())
        {
            logger.warn("Local ant build so Electron UI must be started with 'npm start'");
            return;
        }

        if (isMac())
        {
            try
            {
                logger.info("Starting Electron UI on Mac");

                // Current working directory is <MaX UC App Dir>/Contents/Java,
                // and the Electron app is <MaX UC App Dir>/Contents/Frameworks/<electronAppName>.app
                File electronApp = new File("../Frameworks/", electronAppName + ".app");
                String electronAppAbsolutePath = electronApp.getAbsolutePath();
                logger.info("Starting Electron UI at: " + electronAppAbsolutePath);
                new ProcessBuilder("open", electronAppAbsolutePath).start();
                logger.info("Started Electron UI at: " + electronAppAbsolutePath);
            }
            catch (Exception e)
            {
                logger.error("Hit Exception Starting Electron UI on Mac", e);
                // We hit errors trying to start the electron UI, so re-enable
                // the Java dock and menu icon so that the user has something
                // they can interact with.
                // TODO: ROBUSTNESS https://[indeterminate link]
                // For GA we will need better error checking, specific Exception
                // handling, probably a retry mechanism for starting the Electron
                // client and monitoring to check that it is (still) running.
                //
                // This code is copied directly from OSUtils.showMacDockAndMenu(). We
                // can't use OSUtils from here as it blocks some logs from appearing
                // (see https://[indeterminate link]
                //
                // DUIR-509 may mean that we can retire this snippet below now that
                // we have better fallback behaviour.
                if (isMac())
                {
                    logger.info("Showing Java app dock icon and menu bar on Mac");

                    // False shows the UI; true hides the UI.
                    System.setProperty("apple.awt.UIElement", "false");
                }
            }
        }
        else if (isWindows())
        {
            try
            {
                logger.info("Starting Electron UI on Windows");
                // Current working directory is C:\\Program Files (x86)\\<MaX UC App Dir>,
                // and the Electron app is at C:\\Program Files (x86)\\<MaX UC App Dir>\\ui

                File electronApp = new File("ui/", electronAppName + ".exe");
                String electronAppAbsolutePath = electronApp.getAbsolutePath();
                logger.info("Starting Electron UI at: " + electronAppAbsolutePath);
                new ProcessBuilder(electronAppAbsolutePath).start();
                logger.info("Started Electron UI at: " + electronAppAbsolutePath);
            }
            catch (Exception e)
            {
                // TODO: ROBUSTNESS https://[indeterminate link]
                // This kind of error logging will not be acceptable for GA. We will
                // return to improve this error messaging under the work done to improve
                // client robustness.
                logger.error("Hit Exception Starting Electron UI on Windows", e);
            }
        }
        else
        {
            logger.error("Can't start Electron UI because the OS is not" +
                         " recognised. OS name is: " + OS_NAME);
        }
    }

    /**
     * Calculates the port that will be used for the web server to exposed over.
     * For local and FV builds we use a pre-defined port - this is because
     * those builds don't use executables. For installer builds we look through
     * a range of ports randomly until we find one that's free.
     *
     * @return The port that the Java instance will be exposed over.
     */
    private static int getPort()
    {
        if (isLocalAntBuild())
        {
            // Clients started from 'ant' (i.e. local dev or FV clients) should
            // use the pre-defined port. This is because for ant clients, the
            // corresponding Electron frontend is not running as an executable
            // so we can't pass an argument to it indicating the port to connect
            // to.
            logger.info("Local ant build, use port " + PORT_DEFAULT);
            return PORT_DEFAULT;
        }
        else
        {
            // Installer builds should find an available port to use. This is
            // achieved by randomly testing ports until we find a free one.
            logger.info("Installer build, find an available port");
            return getAvailablePort();
        }
    }

    /**
     * Picks ports at random and checks if they are available.
     *
     * @return The number of the first available free port.
     */
    private static int getAvailablePort()
    {
        // Keep picking ports at random until we find one that's available.
        // Fine for this to be infinite loop, worst case is we never find a free
        // port - if this happens then the user has bigger issues (they have 900
        // ports all in use).
        while (true)
        {
            // Calculate a number in range from PORT_RANGE_START up to
            // PORT_RANGE_END.
            int port = PORT_RANGE_START + new Random().nextInt(1 + PORT_RANGE_END - PORT_RANGE_START);

            try
            {
                logger.debug("Check if port " + port + " is available.");
                ServerSocket socket = new ServerSocket(port);

                // If we get this far then we were able to bind to the port,
                // therefore assume it's available.
                socket.close();
                logger.info("Port " + port + " is available.");
                return port;
            }
            catch (IOException e)
            {
                // We failed to bind to the port, then assume the port is not
                // available.
                logger.debug("Port " + port + " is not available.");
            }
        }
    }

    /**
     * Set up the properties required for WISPA keys and certificates, but only
     * if we're running as part of the installed application (as opposed to a
     * local dev build).
     */
    private static void setupWispaPropertiesIfRequired()
    {
        if (!isLocalAntBuild())
        {
            // We're running as part of an installed application.  Use WISPA over WSS.
            setupWispaProperties();
        }
    }

    /**
     * Set up the properties required for WISPA keys and certificates.
     *
     * On starting the Electron app, we pass in the server certificate, so that
     * it can know it has connected to the correct endpoint.  We also pass in an
     * encrypted keystore, containing the client's certificate and private key,
     * along with the passphrase required to decrypt it.  The client presents
     * its certificate when connecting, allowing the server to only accept
     * connections from trusted clients.
     */
    public static void setupWispaProperties()
    {
        // Generate a one-time passphrase for the WISPA client key store.
        String passphrase = UUID.randomUUID().toString();
        System.setProperty(WISPA_CLIENT_PASSPHRASE_PROPERTY, passphrase);

        // Derive the path for a directory containing WISPA client and server
        // keys and certificates.
        String fileSeparator = System.getProperty("file.separator");
        String wispaDir = System.getProperty(SIPCommunicator.PNAME_SC_HOME_DIR_LOCATION) + fileSeparator +
                          System.getProperty(SIPCommunicator.PNAME_SC_HOME_DIR_NAME) + fileSeparator +
                          WISPA_DIR_NAME + fileSeparator;
        System.setProperty(WISPA_KEYS_DIR_PROPERTY, wispaDir);
        logger.debug("WISPA keys directory: " + wispaDir);

        // Derive and record the paths of the various key stores and certificates.
        String serverKeyStore = wispaDir + WISPA_SERVER_KEY_STORE;
        System.setProperty(WISPA_SERVER_KEY_STORE_PROPERTY, serverKeyStore);
        String serverTrustStore = wispaDir + WISPA_SERVER_TRUST_STORE;
        System.setProperty(WISPA_SERVER_TRUST_STORE_PROPERTY, serverTrustStore);
        String clientKeyStore = wispaDir + WISPA_CLIENT_KEY_STORE;
        System.setProperty(WISPA_CLIENT_KEY_STORE_PROPERTY, clientKeyStore);
        String serverCertificate = wispaDir + WISPA_SERVER_CERTIFICATE;
        System.setProperty(WISPA_SERVER_CERTIFICATE_PROPERTY, serverCertificate);
    }

    /**
     * Recursively copies the old to the new, and delete the old.
     * Deals with files or directories.
     *
     * @param depth Trace depth for diags
     * @param src Old location
     * @param dest New location
     */
    @VisibleForTesting
    static void copyAndDeleteDirectory(String depth, File src, File dest) throws IOException
    {
        depth = depth + "  ";

        if (src.isDirectory())
        {
            // Make a new directory, and then recurse.
            File srcDir = src;
            File destDir = dest;

            if (!destDir.exists())
            {
                mAppDataMigrationLogMessage += depth + "Creating directory " + dest + "\r\n";
                destDir.mkdir();
            }

            for (String file: srcDir.list())
            {
                File srcFile = new File(srcDir, file);
                File destFile = new File(destDir, file);

                copyAndDeleteDirectory(depth, srcFile, destFile);
            }
        }
        else
        {
            // Just copy the file.
            mAppDataMigrationLogMessage += depth + "Manually copying file " + src + " -> " + dest + "\r\n";

            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Clean up the old file, but swallow errors.
        try
        {
           Files.delete(src.toPath());
           mAppDataMigrationLogMessage += depth + "  delete old success\r\n";
        }
        catch (Exception e)
        {
           mAppDataMigrationLogMessage += depth + "  delete old failed: " + e.getMessage() + "\r\n";
        }
    }

    /**
     * Overwrites the old app name with a new value. Only use in unit tests!
     *
     * @param name The name to set as the replacement old app name.
     */
    @VisibleForTesting
    static void setOldAppName(String name)
    {
        oldAppName = name;
    }
}
