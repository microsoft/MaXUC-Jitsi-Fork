/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.launcher;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseFilePath;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.util.launchutils.LaunchOSUtils;
import org.jitsi.util.StringUtils;

/**
 * Handles setup of SIPCommunicator's home directory.
 * <p>
 * Note that this class does not have a Logger because the logger can't be set up until we have a log directory to log
 * to - moreover, any logs made after setting up that directory but before log cleaning will be deleted anyway.
 * Hence, any logs this class wants to make are added as string to the logBuffer argument of {@link #configure(List)}.
 */
public class HomeDirectoryConfigurer
{
    /**
     * The currently active name.
     */
    private static final String overridableDirName = "Jitsi";
    /**
     * Legacy home directory names that we can use if current dir name is the currently active name
     * (overridableDirName).
     */
    private static final String[] legacyDirNames =
            {".sip-communicator", "SIP Communicator"};
    /**
     * Name of the possible configuration file names (used under macosx).
     */
    private static final String[] legacyConfigurationFileNames =
            {"sip-communicator.properties", "jitsi.properties",
                    "sip-communicator.xml", "jitsi.xml"};
    /**
     * Message that will be logged to record an attempt to migrate the app data folder. Required because the migration
     * takes place before the logger is created.
     */
    private static String mAppDataMigrationLogMessage = "";
    /**
     * Message that will be logged to record an attempt to migrate the saved calls/meetings folder. Required because the
     * migration takes place before the logger is created.
     */
    private static String mRecordingsDirectoryMigrationLogMessage = "";
    /**
     * Message that will be logged to record cleaning old logs. Required because the migration takes place before the
     * logger is created.
     */
    private static String mCleaningLogsLogMessage = "";
    /**
     * The name of a previous version of the app, if defined in branding.
     */
    static String oldAppName = System.getProperty(SIPCommunicator.PNAME_SC_OLD_APP_NAME);
    /**
     * The current name of the app.
     */
    static String appName = System.getProperty(SIPCommunicator.PNAME_SC_HOME_DIR_NAME);

    /**
     * Main entry point - sets up home directory.
     * <p>
     * Ensures that it exists, is in the right place (migrating existing directories if necessary), and that it's clean
     * (from a privacy PoV).
     *
     * @param logBuffer buffer for logs we would like to make in future once setup is complete
     */
    static void configure(List<String> logBuffer)
    {
        // SC_HOME_DIR_* are specific to the OS so make sure they're configured
        // accordingly before any other application-specific logic depending on
        // them starts (e.g. Felix or any logging).
        setScHomeDir();

        // Also deliberately called before logging anything.
        ensureLogDirectoryIsClean();

        // Similarly for potentially moving the recorded calls/meetings folder
        // in the event of a name change.
        setRecordingsDir();

        // Record logs that we want to make later once logging is actually setup
        addLogsToLogBuffer(logBuffer);
    }

    private static void addLogsToLogBuffer(List<String> logBuffer)
    {
        // Log any clean-up we've done.
        if ((mCleaningLogsLogMessage != null) &&
            (!mCleaningLogsLogMessage.equals("")))
        {
            String msg = "Cleaning old logs message: " + mCleaningLogsLogMessage;
            logBuffer.add(msg);
        }

        // If we attempted to rename the user data folder due to an app name
        // change log the attempt now.
        if ((mAppDataMigrationLogMessage != null) &&
            (!mAppDataMigrationLogMessage.equals("")))
        {
            String msg = "App data migration message: " + mAppDataMigrationLogMessage;
            logBuffer.add(msg);
        }

        // If we attempted to rename the recorded calls folder due to an app name
        // change log the attempt now.
        if ((mRecordingsDirectoryMigrationLogMessage != null) &&
            (!mRecordingsDirectoryMigrationLogMessage.equals("")))
        {
            String msg = "Recordings migration message: " + mRecordingsDirectoryMigrationLogMessage;
            logBuffer.add(msg);
        }
    }

    /**
     * Sets the system properties net.java.sip.communicator.SC_HOME_DIR_LOCATION and
     * net.java.sip.communicator.SC_HOME_DIR_NAME (if they aren't already set) in accord with the OS conventions
     * specified by the name of the OS.
     */
    private static void setScHomeDir()
    {
        /*
         * Though we'll be setting the SC_HOME_DIR_* property values depending
         * on the OS running the application, we have to make sure we are
         * compatible with earlier releases i.e. use
         * ${user.home}/.sip-communicator if it exists (and the new path isn't
         * already in use).
         */
        String location = System.getProperty(SIPCommunicator.PNAME_SC_HOME_DIR_LOCATION);

        boolean isHomeDirnameForced = (appName != null);

        if ((location == null) || (appName == null))
        {
            String defaultLocation = System.getProperty("user.home");
            String defaultName = ".jitsi";

            // Whether we should check legacy names
            // 1) when such name is not forced we check
            // 2) if such is forced and is the overridableDirName check it
            //      (the latter is the case with name transition SIP Communicator
            //      -> Jitsi or Accession Communicator -> MaX UC)
            boolean checkLegacyDirNames = (appName == null) || appName.equals(overridableDirName);

            if (LaunchOSUtils.isMac())
            {
                if (location == null)
                {
                    location = System.getProperty("user.home") + File.separator + "Library" + File.separator +
                               "Application Support";
                }
                if (appName == null)
                {
                    appName = "Jitsi";
                }
            }
            else if (LaunchOSUtils.isWindows())
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

            /* If there are no OS specifics, use the defaults. */
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
                for (String legacyDirName : legacyDirNames)
                {
                    // check the platform specific directory
                    if (checkHomeFolderExist(location, legacyDirName))
                    {
                        appName = legacyDirName;
                        break;
                    }

                    // now check it and in the default location
                    if (checkHomeFolderExist(defaultLocation, legacyDirName))
                    {
                        appName = legacyDirName;
                        location = defaultLocation;
                        break;
                    }
                }
            }

            System.setProperty(SIPCommunicator.PNAME_SC_HOME_DIR_LOCATION, location);
            System.setProperty(SIPCommunicator.PNAME_SC_HOME_DIR_NAME, appName);
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

        System.out.println("Log directory is " + logDirectory);
    }

    /**
     * V3.10 privacy improvements require that we remove all logs written by pre-V3.10 clients, as they may expose
     * Personal Data. If this is an upgrade FROM V3.10+, do nothing - we don't want to delete logs unnecessarily.
     * <p>
     * This method must be called before logging is enabled, otherwise log files will be locked and attempts to delete
     * them will fail.
     * <p>
     * We use the existence of a file to track whether the log dir is clean. We do this rather than use the config
     * service as we need to delete logs before they get written to (and locked) and we always start logging before the
     * config service is running.
     */
    private static void ensureLogDirectoryIsClean()
    {
        String homeDirectory = System.getProperty(SIPCommunicator.PNAME_SC_HOME_DIR_LOCATION);
        File logDirectory = new File(homeDirectory, appName + File.separator + "log");
        File flagLogDirectoryClean = new File(homeDirectory, appName + File.separator + ".log-dir-clean");

        if (!flagLogDirectoryClean.exists())
        {
            mCleaningLogsLogMessage = "Log directory is unclean.";
            if (cleanDirectory(logDirectory))
            {
                try
                {
                    flagLogDirectoryClean.createNewFile();
                    mCleaningLogsLogMessage += " Marked log directory as clean.";
                }
                catch (IOException ex)
                {
                    mCleaningLogsLogMessage += " Writing log directory clean flag failed.";
                }
            }
            else
            {
                mCleaningLogsLogMessage += " Could not clean the log directory.";
            }
        }
    }

    /**
     * Returns true if 1) the log directory has been successfully cleaned of all old log files, or 2) in the case of a
     * fresh install, where the log directory does not yet exist.
     * <p>
     * This ignores garbage collection (gc) and msoutlook logs, as the gc logs do not expose Personal Data, and the
     * msoutlook logs are not included in feedback reports. These files are typically locked on launch and cannot be
     * deleted anyway.
     */
    private static boolean cleanDirectory(File logDirectory)
    {
        if (!logDirectory.isDirectory())
        {
            mCleaningLogsLogMessage += " No existing log directory - OK.";
            return true;
        }

        mCleaningLogsLogMessage += " Existing log directory to clean.";

        int deletionFailures = 0;
        FilenameFilter ignoreExcluded = (dir, file) -> !file.startsWith("gc") &&
                                                       !file.startsWith("msoutlook");
        File[] filesToRemove = logDirectory.listFiles(ignoreExcluded);
        if (filesToRemove != null)
        {
            for (File f : filesToRemove)
            {
                if (!f.delete())
                {
                    mCleaningLogsLogMessage += " Failed to remove file " +
                                               f.getName();
                    deletionFailures++;
                }
            }
        }

        return deletionFailures == 0;
    }

    /**
     * As well as migrating user data during a name change, we might have to move a user's recorded calls/meetings
     * folder, by default saved in {user.home}/{application.name}. The user may have changed this option, in which case
     * this method simply deletes the original default folder.
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
     * Checks whether home folder exists. Special situation checked under macosx, due to created folder of the new
     * version of the updater we may end up with our settings in 'SIP Communicator' folder and having 'Jitsi' folder
     * created by the updater(its download location). So we check not only the folder exist but whether it contains any
     * of the known configuration files in it.
     *
     * @param parent the parent folder
     * @param name   the folder name to check.
     * @return whether folder exists.
     */
    private static boolean checkHomeFolderExist(String parent, String name)
    {
        if (LaunchOSUtils.isMac())
        {
            for (String legacyConfigurationFileName : legacyConfigurationFileNames)
            {
                if (new File(new File(parent, name), legacyConfigurationFileName).exists())
                {
                    return true;
                }
            }

            return false;
        }

        return new File(parent, name).isDirectory();
    }

    /**
     * Renames a given directory to use the new app name, e.g. app data or recorded meetings. Used when the app name has
     * changed over upgrade.  If files are in use then attempts a copy instead.
     *
     * @param location   The parent directory of the user data directory.
     * @param oldName    The old name of the user data directory.
     * @param newName    The new name of the user data directory.
     * @param folderType What message we want to write logs to, depending on what type of file transfer is being made.
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
                           + sanitiseFilePath(location) + " from " + oldName + " to " + newName + ". Result: " +
                           result + "\r\n";

            // OK, so that didn't work.  Now try a copy which is ugly,
            // but as a backup will have to do.
            logMessages += "Attempting copy....\r\n";

            try
            {
                copyAndDeleteDirectory("", oldDir, newDir);
            }
            catch (IOException ex)
            {
                result += " IOException copying " + sanitiseFilePath(ex.getMessage());
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
     * Recursively copies the old to the new, and delete the old. Deals with files or directories.
     *
     * @param depth Trace depth for diags
     * @param src   Old location
     * @param dest  New location
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

            for (String file : srcDir.list())
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

    /**
     * As logging is uninitialised until after the start-up process, we must store log messages as strings to print
     * later. We use this enum to determine which operation we are logging (stored in the respective string# above).
     */
    private enum UserDataFolderType
    {
        APP_DATA,
        RECORDINGS
    }
}
