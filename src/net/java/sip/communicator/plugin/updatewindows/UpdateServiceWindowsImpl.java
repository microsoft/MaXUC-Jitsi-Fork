/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.updatewindows;

import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.httputil.HTTPResponseResult;
import net.java.sip.communicator.service.httputil.HttpUtils;
import net.java.sip.communicator.service.update.UpdateDownloadState;
import net.java.sip.communicator.service.update.UpdateInfo;
import net.java.sip.communicator.service.update.UpdateProgressInformation;
import net.java.sip.communicator.service.update.UpdateService;
import net.java.sip.communicator.service.update.UpdateState;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPAMotion;
import net.java.sip.communicator.service.wispaservice.WISPAMotionType;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.service.version.Version;
import org.jitsi.util.StringUtils;

/**
 * Implements checking for software updates, downloading and applying them (on Windows)
 *
 * Relies on using an exe wrapped msi installer.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class UpdateServiceWindowsImpl
    implements UpdateService
{
    /**
     * The <tt>Logger</tt> used by the <tt>UpdateServiceWindowsImpl</tt> class for logging output.
     */
    private static final Logger logger = Logger.getLogger(UpdateServiceWindowsImpl.class);

    /**
     * The name of the property which specifies the update link in the
     * configuration file.
     */
    private static final String PROP_UPDATE_LINK
        = "net.java.sip.communicator.UPDATE_LINK";

    /**
     * Name of the encrypted password in the configuration service.
     */
    private static final String PROPERTY_ENCRYPTED_PASSWORD
        = "net.java.sip.communicator.plugin.provisioning.auth";

    /**
     * Name of the username in the configuration service.
     */
    private static final String PROPERTY_USERNAME
        = "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";

    /**
     * The link pointing to the ChangeLog of the update.
     */
    private static String changesLink;

    /**
     * The link pointing at the download of the update.
     */
    private static String sDownloadLink;

    /**
     * The latest version of the software found at the configured update
     * location.
     */
    private static String latestVersion;

    /**
     * The flag which indicates whether an update is a forced update
     */
    private static boolean forceUpdate;

    /**
     * The flag which indicates whether an update has been canceled.
     * Volatile due to potential caching via threads when an update download job
     * is run
     */
    private static volatile boolean updateDownloadCanceled;

    @Override
    public synchronized void checkForUpdates(
            final boolean isUserTriggered)
    {
        if (!isUserTriggered &&
            (ConfigurationUtils.isAutoUpdateCheckingDisabledForUser() ||
             ConfigurationUtils.isAutoUpdateCheckingDisabledGlobally()))
        {
            logger.info("Automatic update checking is disabled.");
            return;
        }

        logger.info("Checking for updates");
        forceUpdate = UpdateWindowsActivator.checkForceUpdate();
        Runnable runnable = () -> doUpdate(isUserTriggered, forceUpdate);
        if (EventQueue.isDispatchThread())
        {
            runnable.run();
        }
        else
        {
            EventQueue.invokeLater(runnable);
        }
    }

    @Override
    public void forceUpdate()
    {
        doUpdate(false, true);
    }

    private synchronized void doUpdate(final boolean isUserTriggered,
                                       final boolean forceUpdate)
    {
        logger.info("Doing update. User triggered? " + isUserTriggered + ".  Force Update? " + forceUpdate);
        Runnable checkForUpdates = () -> checkServerForUpdate(isUserTriggered, forceUpdate);
        UpdateWindowsActivator.getThreadingService().submit("Check for updates", checkForUpdates);
    }

    @VisibleForTesting
    void checkServerForUpdate(final boolean isUserTriggered, final boolean forceUpdate)
    {
        // We've got an installer, so next we need to shut down the
        // client to install the new version.  Before we do that, we
        // display a dialog to warn the user of this and ask them to
        // confirm they are happy to proceed.  That is unless we have
        // been asked to force upgrade, in which case we simply shut
        // down, as the user has no choice.
        UpdateServiceWindowsImpl.forceUpdate = forceUpdate;
        String currentVersion = getCurrentVersion().toString();

        try
                {
                    if (isLatestVersion())
                    {
                        if (forceUpdate)
                        {
                            // We were asked to force the user to upgrade but
                            // we are already up to date - reset the minimum
                            // version flag.
                            logger.error(
                                    "Asked to force update but no new version");
                            UpdateWindowsActivator.getConfiguration().user().removeProperty(
                                       "net.java.sip.communicator.MIN_VERSION");
                        }

                        if (isUserTriggered)
                        {
                            logger.info("Notifying user that there aren't any" +
                                        " new updates");
                            notifyUpdateInfo(latestVersion,
                                             currentVersion,
                                             UpdateState.UP_TO_DATE);
                        }
                    }
                    else
                    {
                        logger.info("Notifying user about new update: " +
                                    " Forced: " + forceUpdate);
                        notifyUpdateInfo(latestVersion,
                                         currentVersion,
                                         forceUpdate ? UpdateState.UPDATE_FORCED : UpdateState.UPDATE_OPTIONAL);
                    }
                }
                catch (IOException e)
                {
                    logger.warn("Error connecting to update server: " +
                                e.getMessage());

                    // Could not communicate properly with the update server.
                    // Only show feedback to the user in case the check for
                    // update action was manually triggered.
                    if (isUserTriggered)
                    {
                        logger.warn("Notifying that the connection is down");
                        notifyUpdateInfo(latestVersion,
                                         currentVersion,
                                         forceUpdate ? UpdateState.ERROR_FORCED : UpdateState.ERROR_OPTIONAL);
                    }
                }
            }

    /**
     * Sends update information to be rendered on the Electron side
     * @param latestVersion string of the latest version of the client
     * @param currentVersion string of the current version of the client
     */
    private static void notifyUpdateInfo(String latestVersion, String currentVersion, UpdateState updateState)
    {
        UpdateInfo updateInfo = new UpdateInfo(latestVersion, currentVersion, updateState);
        WISPAMotion wispaMotion = new WISPAMotion(WISPAMotionType.UPDATE_INFO, updateInfo);
        WISPAService wispaService = UpdateWindowsActivator.getWISPAService();
        wispaService.notify(WISPANamespace.EVENTS, WISPAAction.MOTION, wispaMotion);
    }

    /**
     * Sends update progress bar information to be rendered on the Electron side
     * @param size size of the update
     * @param transferredSize size of the update transferred so far
     * @param updateDownloadState indicates if updates is in-progress, failed or cancelled by user action
     */
    private static void notifyUpdateInstallationProgress(long size, long transferredSize, UpdateDownloadState updateDownloadState)
    {
        UpdateProgressInformation updateProgressInformation = new UpdateProgressInformation(size, transferredSize, updateDownloadState);
        WISPAMotion wispaMotion = new WISPAMotion(WISPAMotionType.UPDATE_PROGRESS_INFO, updateProgressInformation);
        WISPAService wispaService = UpdateWindowsActivator.getWISPAService();
        wispaService.notify(WISPANamespace.EVENTS, WISPAAction.MOTION, wispaMotion);
    }

    /**
     * Tries to create a new <tt>FileOutputStream</tt> for a temporary file into
     * which the setup is to be downloaded. Because temporary files generally
     * have random characters in their names and the name of the setup may be
     * shown to the user, first tries to use the name of the URL to be
     * downloaded because it likely is prettier.
     * NOTE, by default, this creates a temporary .exe file, so will fail if trying to
     * install from an .msi!
     *
     * @param url the <tt>URL</tt> of the file to be downloaded
     * @param extension the extension of the <tt>File</tt> to be created or
     * <tt>null</tt> for the default (which may be derived from <tt>url</tt>)
     * @param dryRun <tt>true</tt> to generate a <tt>File</tt> in
     * <tt>tempFile</tt> and not open it or <tt>false</tt> to generate a
     * <tt>File</tt> in <tt>tempFile</tt> and open it
     * @param tempFile a <tt>File</tt> array of at least one element which is to
     * receive the created <tt>File</tt> instance at index zero (if successful)
     * @return the newly created <tt>FileOutputStream</tt>
     * @throws IOException if anything goes wrong while creating the new
     * <tt>FileOutputStream</tt>
     */
    private static FileOutputStream createTempFileOutputStream(
            URL url,
            String extension,
            boolean dryRun,
            File[] tempFile)
        throws IOException
    {
        /*
         * Try to use the name from the URL because it isn't a "randomly"
         * generated one.
         */
        String path = url.getPath();

        File tf = null;
        FileOutputStream tfos = null;

        if ((path != null) && (path.length() != 0))
        {
            int nameBeginIndex = path.lastIndexOf('/');
            String name;

            if (nameBeginIndex > 0)
            {
                name = path.substring(nameBeginIndex + 1);
                nameBeginIndex = name.lastIndexOf('\\');
                if (nameBeginIndex > 0)
                    name = name.substring(nameBeginIndex + 1);
            }
            else
                name = path;

            /*
             * Make sure the extension of the name is EXE so that we're able to
             * execute it later on.
             */
            int nameLength = name.length();

            if (nameLength != 0)
            {
                int baseNameEnd = name.lastIndexOf('.');

                if (extension == null)
                    extension = ".exe";
                if (baseNameEnd == -1)
                    name += extension;
                else if (baseNameEnd == 0)
                {
                    if (!extension.equalsIgnoreCase(name))
                        name += extension;
                }
                else
                    name = name.substring(0, baseNameEnd) + extension;

                try
                {
                    String tempDir = System.getProperty("java.io.tmpdir");

                    if ((tempDir != null) && (tempDir.length() != 0))
                    {
                        tf = new File(tempDir, name);
                        if (!dryRun)
                            tfos = new FileOutputStream(tf);
                    }
                }
                catch (FileNotFoundException | SecurityException fnfe)
                {
                    // Ignore it because we'll try File#createTempFile().
                }
            }
        }

        // Well, we couldn't use a pretty name so try File#createTempFile().
        // TODO: this shouldn't just hardcode to use an exe if the extension
        // is explicitly set in the parameters.
        if ((tfos == null) && !dryRun)
        {
            tf = File.createTempFile("setup", ".exe");
            tfos = new FileOutputStream(tf);
        }

        tempFile[0] = tf;
        return tfos;
    }

    /**
     * Downloads a remote file specified by its URL into a local file.
     *
     * @param url the URL of the remote file to download
     * @return the local <tt>File</tt> into which <tt>url</tt> has been
     * downloaded or <tt>null</tt> if there was no response from the
     * <tt>url</tt>
     * @throws IOException if an I/O error occurs during the download
     */
    private static File download(String url)
        throws Exception
    {
        final File[] tempFile = new File[1];
        FileOutputStream tempFileOutputStream = null;
        boolean deleteTempFile = true;
        updateDownloadCanceled = false;

        tempFileOutputStream
            = createTempFileOutputStream(
                    new URL(url),
                    /*
                     * The default extension, possibly derived from url, is
                     * fine. Besides, we do not really have information about
                     * any preference.
                     */
                    null,
                    /* Do create a FileOutputStream. */
                    false,
                    tempFile);
        try
        {
            HTTPResponseResult res = HttpUtils.openURLConnection(url);

            if (res != null)
            {
                InputStream content = res.getContent();

                /*
                 * Set the maximum value of the ProgressMonitor to the size of
                 * the file to download.
                 */
                long totalSize = res.getContentLength();

                // getContentLength() often fails, probably due to incompatibility with underlying apache code
                // and HTTP headers returned by some file servers.  If we get nothing, manually inspect the
                // HTTP response for an x-acd-content-length header.
                if (totalSize == -1)
                {
                    logger.info("Failed to get total size. Look for x-acd-content-length header.");
                    String xAcdContentLengthHeader = res.getResponseHeader("x-acd-content-length");
                    if (xAcdContentLengthHeader != null)
                    {
                        logger.info("xAcdContentLengthHeader returned " + xAcdContentLengthHeader);
                        try
                        {
                            totalSize = Long.parseLong(xAcdContentLengthHeader);
                        }
                        catch (Exception e)
                        {
                            logger.warn("Error looking for x-acd-content-length header", e);
                        }
                    }
                }

                logger.info("Downloading " + url + " of size: " + totalSize);

                long bytesRead = 0;
                long totalAttempts = 0;

                try (BufferedOutputStream output = new BufferedOutputStream(
                        tempFileOutputStream))
                {
                    int attempts = 0;
                    int read = -1;
                    byte[] buff = new byte[1024];
                    long lastProgressTime = System.currentTimeMillis();

                    while (!updateDownloadCanceled && (read = content.read(buff)) != -1)
                    {
                        bytesRead += read;
                        attempts++;
                        totalAttempts++;

                        // We only want to log every 1000 attempts or so, or we
                        // will fill the logs with irrelevant detail
                        if (attempts > 1000)
                        {
                            attempts = 0;
                            logger.debug("Read " +
                                                 bytesRead + " / " +
                                                 totalSize +
                                                 " in " + totalAttempts +
                                                 " reads");
                        }

                        // Send progress bar updates every 250 ms
                        if (System.currentTimeMillis() - lastProgressTime >= 250) {
                            lastProgressTime = System.currentTimeMillis();
                            notifyUpdateInstallationProgress(totalSize, bytesRead, UpdateDownloadState.UPDATE_DOWNLOAD_IN_PROGRESS);
                        }

                        output.write(buff, 0, read);
                    }
                }
                catch (IOException e)
                {
                    notifyUpdateInstallationProgress(0, 0, forceUpdate ? UpdateDownloadState.UPDATE_DOWNLOAD_FAILED_MANDATORY : UpdateDownloadState.UPDATE_DOWNLOAD_FAILED_OPTIONAL);
                    logger.error(e);
                    throw e;
                }
                finally
                {
                    logger.debug("Finished reading " +
                                         bytesRead + " / " + totalSize +
                                         " in " + totalAttempts + " reads");

                    tempFileOutputStream = null;
                }

                // notify Electron client that the update was canceled with whether it's mandatory or optional
                if (!updateDownloadCanceled) {
                    deleteTempFile = false;
                }
            }
            else
            {
                throw new Exception("No HTTP result found for url " + url);
            }
        }
        catch (Exception e)
        {
            notifyUpdateInstallationProgress(0, 0, forceUpdate ? UpdateDownloadState.UPDATE_DOWNLOAD_FAILED_MANDATORY : UpdateDownloadState.UPDATE_DOWNLOAD_FAILED_OPTIONAL);
            logger.error(e);
            throw e;
        }
        finally
        {
            try
            {
                if (tempFileOutputStream != null)
                    tempFileOutputStream.close();
            }
            finally
            {
                if (deleteTempFile && (tempFile[0] != null))
                {
                    tempFile[0].delete();
                    tempFile[0] = null;
                }
            }
        }
        return tempFile[0];
    }

    /**
     * Gets the current (software) version.
     *
     * @return the current (software) version
     */
    private static Version getCurrentVersion()
    {
        return UpdateWindowsActivator.getVersionService().getCurrentVersion();
    }

    @Override
    public boolean isLatestVersion() throws IOException
    {
        String updateLink
            = UpdateWindowsActivator.getConfiguration().user().getString(
                    PROP_UPDATE_LINK);
        logger.info("Got update link: " + updateLink);

        if(updateLink == null)
        {
            updateLink
                = Resources.getUpdateConfigurationString("update_link");
        }

        if (updateLink == null)
        {
            logger.warn("Updates are disabled, faking latest version.");
        }
        else
        {
            updateLink = processUpdateLink(updateLink);

            HTTPResponseResult res
                = HttpUtils.openURLConnection(updateLink);

            if (res != null)
            {
                Properties props = new Properties();
                try (InputStream in = res.getContent())
                {
                    props.load(in);
                }

                latestVersion = props.getProperty("last_version");
                logger.debug("Got latest version: " + latestVersion);
                sDownloadLink = props.getProperty("download_link");
                logger.debug("Got download link: " + sDownloadLink);

                // If a latest version hasn't been supplied by the update
                // server, we must consider the current version the latest.
                if (StringUtils.isNullOrEmpty(latestVersion))
                {
                    logger.warn("Latest version not supplied");
                    latestVersion = getCurrentVersion().toString(true);
                    return true;
                }

                changesLink
                    = updateLink.substring(
                            0,
                            updateLink.lastIndexOf("/") + 1)
                        + props.getProperty("changes_html");
                logger.debug("Got changes link: " + changesLink);

                try
                {
                    Version latestVersionObj =
                        UpdateWindowsActivator.getVersionService().parseVersionString(latestVersion);

                    if(latestVersionObj != null)
                    {
                        return latestVersionObj.compareTo(
                                    getCurrentVersion()) <= 0;
                    }
                    else
                    {
                        logger.error("Version obj not parsed("
                                            + latestVersion + ")");
                    }
                }
                catch(Exception e)
                {
                    logger.error("Error parsing version string", e);
                }

                // Fallback to a lexicographically comparison
                // of version strings in case of an error
                return latestVersion.compareTo(
                            getCurrentVersion().toString()) <= 0;
            }
            else
            {
                logger.warn("Could not establish connection to update server");

                // Throw exception that will indicate that we could not
                // connect to the update server.
                throw new IOException("Could not connect to server " +
                                      updateLink);
            }
        }

        return true;
    }

    /**
     * Process the update link that we've been passed, replacing the username
     * and password parameters with the correct values
     *
     * @param updateLink The update link to process
     * @return The processed update link
     */
    @SuppressWarnings("deprecation")
    private static String processUpdateLink(String updateLink)
    {
        if (updateLink.contains("${password}"))
        {
            CredentialsStorageService creds = UpdateWindowsActivator.getCredsService();
            String password = creds.user().loadPassword(PROPERTY_ENCRYPTED_PASSWORD);

            if (password != null)
            {
                logger.debug("Replacing password in update link");
                password = URLEncoder.encode(password);
                updateLink = updateLink.replace("${password}", password);
            }
        }

        if (updateLink.contains("${directorynumber}"))
        {
            ConfigurationService cfg = UpdateWindowsActivator.getConfiguration();
            String username = cfg.global().getString(PROPERTY_USERNAME);

            if (username != null)
            {
                logger.debug("Replacing directory number in update link");
                updateLink = updateLink.replace("${directorynumber}", username);
            }
        }

        return updateLink;
    }

    @Override
    public void forceQuitApplication()
    {
        logger.error("Forcing quit of application");
        ServiceUtils.shutdownAll(UpdateWindowsActivator.bundleContext);
    }

    @Override
    public void updateIfAvailable()
    {
        try
        {
            if (!isLatestVersion() && (sDownloadLink != null))
            {
                windowsUpdateInNewThread();
            }
            else
            {
                logger.warn("Asked to update when no update is available");
            }
        }
        catch (IOException e)
        {
            notifyUpdateInstallationProgress(0, 0, forceUpdate ? UpdateDownloadState.UPDATE_DOWNLOAD_FAILED_MANDATORY : UpdateDownloadState.UPDATE_DOWNLOAD_FAILED_OPTIONAL);
            logger.warn("Error updating", e);
        }
    }

    private static void windowsUpdateInNewThread()
    {
        Runnable update = () -> {
                windowsUpdate();
        };

        UpdateWindowsActivator.getThreadingService().submit("Client update - Windows", update);
    }

    /**
     * Implements the very update procedure on Windows which includes without
     * being limited to:
     * <ol>
     * <li>Downloads the setup in a temporary directory.</li>
     * <li>Warns that the update procedure will shut down the application.</li>
     * <li>Executes the setup in a separate process and shuts down the
     * application.</li>
     * </ol>
     *
     */
    private static void windowsUpdate()
    {
        logger.info("Update client");

        File msi = null;
        boolean deleteMsi = true;

        try
        {
            // It is misleading to put the installer file in a variable 'msi'
            // as it is in fact an exe.
            msi = download(sDownloadLink);

            if (msi != null)
            {
                ResourceManagementService resources = Resources.getResources();

                // Now build a command to execute the installer in a new process
                // The command is built-up for the exe wrapping the msi installer,
                // this allows the --wait-parent option (assumed to wait
                // for the old AD the have shutdown before updating files).
                logger.debug("Shutting down to start upgrade");
                List<String> command = new ArrayList<>();

                command.add(msi.getCanonicalPath());
                command.add("--wait-parent");

                // Log to a file in the logging directory
                File loggingFile = new File(Logger.getLogDirectory(), "install.log");
                command.add("/L*v");
                command.add(loggingFile.getAbsolutePath());

                // Set some installer options so this just overwrites the existing
                // AD without asking the user anything.
                command.add(
                        "SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR=\""
                            + System.getProperty("user.dir")
                            + "\"");

                String asciiAppName = resources.getSettingsString("service.gui.APPLICATION_NAME_ASCII") != null ?
                                      resources.getSettingsString("service.gui.APPLICATION_NAME_ASCII") : "";
                command.add("SIP_COMMUNICATOR_AUTOUPDATE_APP_NAME=\""
                                                     + asciiAppName + "\"");

                deleteMsi = false;

                /*
                 * The setup has been downloaded. Now start it and shut
                 * down.
                 */
                logger.info("Running command: " + command);
                new ProcessBuilder(command).start();

                logger.info("Closing app to install update. Installer properties:"
                    + "SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR=" + System.getProperty("user.dir")
                    + ", SIP_COMMUNICATOR_AUTOUPDATE_APP_NAME=" + asciiAppName);
                ServiceUtils.shutdownAll(UpdateWindowsActivator.bundleContext);
            }
        }
        catch (Exception exception)
        {
            notifyUpdateInstallationProgress(0, 0, forceUpdate ? UpdateDownloadState.UPDATE_DOWNLOAD_FAILED_MANDATORY : UpdateDownloadState.UPDATE_DOWNLOAD_FAILED_OPTIONAL);
            logger.error("Error downloading update", exception);
        }
        finally
        {
            /*
             * If we've failed, delete the temporary file into which the setup
             * was supposed to be or has already been downloaded.
             */
            if (deleteMsi && (msi != null))
            {
                msi.delete();
                msi = null;
            }
        }
    }

    @Override
    public void cancelUpdateDownload()
    {
        notifyUpdateInstallationProgress(0, 0, UpdateDownloadState.UPDATE_DOWNLOAD_CANCELLED);
        if (forceUpdate)
        {
            checkForUpdates(true);
        }
        updateDownloadCanceled = true;
    }
}
