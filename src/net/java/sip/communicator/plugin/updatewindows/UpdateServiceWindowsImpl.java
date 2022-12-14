/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.updatewindows;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.SIPCommBasicTextButton;
import net.java.sip.communicator.plugin.desktoputil.SIPCommDialog;
import net.java.sip.communicator.plugin.desktoputil.SIPCommFrame;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.SuccessDialog;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.desktoputil.WindowUtils;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.gui.PopupDialog;
import net.java.sip.communicator.service.httputil.HTTPResponseResult;
import net.java.sip.communicator.service.httputil.HttpUtils;
import net.java.sip.communicator.service.update.UpdateService;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.service.version.Version;
import org.jitsi.util.OSUtils;
import org.jitsi.util.StringUtils;

import com.google.common.annotations.VisibleForTesting;

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
     * Name of property to force update.
     */
    private static final String FORCE_UPDATE = "net.java.sip.communicator.plugin.update.FORCE_UPDATE";

    /**
     * The link pointing to the ChangeLog of the update.
     */
    private static String changesLink;

    /**
     * The <tt>JDialog</tt>, if any, which is associated with the currently
     * executing "Check for Updates". While the "Check for Updates"
     * functionality cannot be entered, clicking the "Check for Updates" menu
     * item will bring it to the front.
     */
    private static JDialog checkForUpdatesDialog;

    /**
     * The link pointing at the download of the update.
     */
    private static String sDownloadLink;

    /**
     * The indicator/counter which determines how many methods are currently
     * executing the "Check for Updates" functionality so that it is known
     * whether it can be entered.
     */
    private static int inCheckForUpdates = 0;

    /**
     * The latest version of the software found at the configured update
     * location.
     */
    private static String latestVersion;

    /**
     * Invokes "Check for Updates".
     *
     * @param isUserTriggered <tt>true</tt> if the user is explicitly asking to
     * check for notifications, in which case they are to be notified if they
     * have the newest version already; and also if we should notify them when
     * they perform this action while disconnected; otherwise, <tt>false</tt>
     */
    public synchronized void checkForUpdates(
            final boolean isUserTriggered)
    {
        Runnable runnable = () -> doUpdate(isUserTriggered, false);
        if (EventQueue.isDispatchThread())
        {
            runnable.run();
        }
        else
        {
            EventQueue.invokeLater(runnable);
        }
    }

    /**
     * Forces the client to update to the most recent version by showing the
     * "new version available" pop-up. If this is cancelled then the application
     * will exit
     */
    public void forceUpdate()
    {
        doUpdate(false, true);
    }

    private synchronized void doUpdate(final boolean isUserTriggered,
                                       final boolean forceUpdate)
    {
        if (inCheckForUpdates > 0)
        {
            logger.info("Already in check for updates - " + inCheckForUpdates);

            if (checkForUpdatesDialog != null)
            {
                checkForUpdatesDialog.setVisible(true);
                checkForUpdatesDialog.toFront();
                WindowUtils.makeWindowVisible(checkForUpdatesDialog, true);
            }
            return;
        }

        Runnable checkForUpdates = () -> checkServerForUpdate(isUserTriggered, forceUpdate);
        UpdateWindowsActivator.getThreadingService().submit("Check for updates", checkForUpdates);
    }

    @VisibleForTesting
    void checkServerForUpdate(final boolean isUserTriggered, final boolean forceUpdate)
    {
        enterCheckForUpdates(null);

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

                            ResourceManagementService res
                                                     = Resources.getResources();

                            String[] msgArgs =
                                   new String[]{getCurrentVersion().toString()};
                            String message = res.getI18NString(
                               "plugin.updatechecker.DIALOG_NOUPDATE", msgArgs);
                            String title = res.getI18NString(
                                  "plugin.updatechecker.DIALOG_NOUPDATE_TITLE");
                            SuccessDialog dialog = new SuccessDialog(null, title, message);
                            WindowUtils.makeWindowVisible(dialog, true);
                        }
                    }
                    else
                    {
                        logger.info("Notifying user about new update: " +
                                    " Forced: " + forceUpdate);

                        showNewVersionAvailableDialog(forceUpdate);
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

                        ResourceManagementService res = Resources.getResources();

                        String message = res.getI18NString(
                            "plugin.updatechecker.DIALOG_NOCONN");
                        String title = res.getI18NString(
                           "plugin.updatechecker.DIALOG_NOCONN_TITLE");

                        new ErrorDialog(null, title, message).setVisible(true);
                    }
                    }
                finally
                {
                    exitCheckForUpdates(null);
                }
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
                // Track the progress of the download.
                final ProgressMonitorInputStream input = new ProgressMonitorInputStream(null, url, content);

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

                input.getProgressMonitor().setMaximum((int) totalSize);

                logger.info("Downloading " + url + " of size: " + totalSize);

                try
                {
                    long bytesRead = 0;
                    long totalAttempts = 0;

                    try (BufferedOutputStream output = new BufferedOutputStream(
                            tempFileOutputStream))
                    {
                        int attempts = 0;
                        int read = -1;
                        byte[] buff = new byte[1024];

                        while ((read = input.read(buff)) != -1)
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

                            output.write(buff, 0, read);
                        }
                    }
                    catch (IOException e)
                    {
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
                    deleteTempFile = false;
                }
                finally
                {
                    // Close the input on the EDT, as otherwise the call to close
                    // can block and never return
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                logger.debug("Close input");
                                input.close();
                                logger.debug("Input closed");
                            }
                            catch (IOException e)
                            {
                                // Just log it because we've already downloaded
                                // the setup and that's what matters most.
                                logger.info("Failed to close download stream", e);
                            }
                        }
                    });
                }
                            }
            else
                            {
                throw new Exception("No HTTP result found for url " + url);
                            }
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
     * Notifies this <tt>UpdateCheckActivator</tt> that a method is entering the
     * "Check for Updates" functionality and it is thus not allowed to enter it
     * again.
     *
     * @param checkForUpdatesDialog the <tt>JDialog</tt> associated with the
     * entry in the "Check for Updates" functionality if any. While "Check for
     * Updates" cannot be entered again, clicking the "Check for Updates" menu
     * item will bring the <tt>checkForUpdatesDialog</tt> to the front.
     */
    private static synchronized void enterCheckForUpdates(
            JDialog checkForUpdatesDialog)
    {
        logger.debug("Enter check for updates, is: " + inCheckForUpdates);

        inCheckForUpdates++;
        if (checkForUpdatesDialog != null)
        {
            UpdateServiceWindowsImpl.checkForUpdatesDialog = checkForUpdatesDialog;
        }
    }

    /**
     * Notifies this <tt>UpdateCheckActivator</tt> that a method is exiting the
     * "Check for Updates" functionality and it may thus be allowed to enter it
     * again.
     *
     * @param checkForUpdatesDialog the <tt>JDialog</tt> which was associated
     * with the matching call to {@link #enterCheckForUpdates(JDialog)} if any
     */
    private static synchronized void exitCheckForUpdates(
            JDialog checkForUpdatesDialog)
    {
        logger.debug("Exit check for updates, is: " + inCheckForUpdates);

        if (inCheckForUpdates <= 0)
            throw new IllegalStateException("inCheckForUpdates");
        else
        {
            inCheckForUpdates--;
            if ((checkForUpdatesDialog != null)
                    && (UpdateServiceWindowsImpl.checkForUpdatesDialog == checkForUpdatesDialog))
                UpdateServiceWindowsImpl.checkForUpdatesDialog = null;
        }
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

    /**
     * Determines whether we are currently running the latest version.
     *
     * @return <tt>true</tt> if we are currently running the latest version;
     * otherwise, <tt>false</tt>
     * @throws IOException in case we could not connect to the update server
     */
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

    /**
     * Shows dialog informing about new version with button Install
     * which triggers the update process.
     *
     * @param forceUpdate If true then the application will quit if
     *        the update is cancelled
     */
    private static void showNewVersionAvailableDialog(final boolean forceUpdate)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    showNewVersionAvailableDialog(forceUpdate);
                }
            });

            return;
        }

        /*
         * Before showing the dialog, we'll enterCheckForUpdates() in order to
         * notify that it is not safe to enter "Check for Updates" again. If we
         * don't manage to show the dialog, we'll have to exitCheckForUpdates().
         * If we manage though, we'll have to exitCheckForUpdates() but only
         * once depending on its modality.
         */
        final boolean[] exitCheckForUpdates = new boolean[] { false };
        final JDialog dialog = new SIPCommDialog()
        {
            private static final long serialVersionUID = 0L;

            protected void close(boolean escaped)
            {
                synchronized (exitCheckForUpdates)
                {
                    if (exitCheckForUpdates[0])
                    {
                        exitCheckForUpdates(this);
                    }

                    dispose();

                    if (forceUpdate)
                    {
                        forceQuitApplication();
                    }
                }
            }
        };
        ResourceManagementService resources = Resources.getResources();

        String titleRes = forceUpdate ? "plugin.updatechecker.DIALOG_TITLE_FORCE" :
                                        "plugin.updatechecker.DIALOG_TITLE";
        dialog.setTitle(resources.getI18NString(titleRes));
        dialog.setResizable(false);

        JTextArea contentMessage = new JTextArea(0, 30);
        contentMessage.setLineWrap(true);
        contentMessage.setWrapStyleWord(true);
        ScaleUtils.scaleFontAsDefault(contentMessage);
        contentMessage.setOpaque(false);
        contentMessage.setEditable(false);

        String msgRes = forceUpdate ?
                                  "plugin.updatechecker.DIALOG_MESSAGE_FORCE" :
                                  "plugin.updatechecker.DIALOG_MESSAGE";
        String appName =
            resources.getSettingsString("service.gui.APPLICATION_NAME");
        String dialogMsg = resources.getI18NString(msgRes,
                                                   new String[]{appName});

        UpdateWindowsActivator.getConfiguration().user().setProperty(FORCE_UPDATE, forceUpdate);

        if (latestVersion != null)
        {
            dialogMsg += "\n\n" +
                resources.getI18NString("plugin.updatechecker.DIALOG_MESSAGE_2",
                                        new String[] {appName, latestVersion});
        }

        contentMessage.setText(dialogMsg);

        JPanel contentPane = new SIPCommFrame.MainContentPane();
        contentMessage.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        contentPane.add(contentMessage, BorderLayout.NORTH);

        JPanel buttonPanel = new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        String closeButtonRes = forceUpdate ? "service.gui.FORCE_QUIT" :
                                              "plugin.updatechecker.BUTTON_CLOSE";
        final JButton closeButton = new SIPCommBasicTextButton(resources.getI18NString(closeButtonRes));

        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                logger.user("User closed update dialog");
                dialog.dispose();
                if (exitCheckForUpdates[0])
                {
                    exitCheckForUpdates(dialog);
                }

                if (forceUpdate)
                {
                    forceQuitApplication();
                }
            }
        });

        if(sDownloadLink != null)
        {
            JButton installButton
                = new SIPCommBasicTextButton(
                        resources.getI18NString(
                                "plugin.updatechecker.BUTTON_INSTALL"));

            installButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    logger.user("User clicked install button");
                    if(OSUtils.IS_WINDOWS64)
                    {
                        logger.info("Using x64 download link");
                        sDownloadLink = sDownloadLink.replace("x86", "x64");
                    }

                    dialog.dispose();

                    // Exit check for updates.  Make sure that we don't do so again
                    synchronized (exitCheckForUpdates)
                    {
                        if (exitCheckForUpdates[0])
                            exitCheckForUpdates(dialog);

                        exitCheckForUpdates[0] = false;
                    }

                    windowsUpdateInNewThread(forceUpdate);
                            }
            });

            buttonPanel.add(installButton);
        }
        else
        {
            logger.error("Download link was null when constructing Install " +
                         " button for update");
        }

        buttonPanel.add(closeButton);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setModal(forceUpdate);
        dialog.setContentPane(contentPane);
        dialog.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation(
                screenSize.width/2 - dialog.getWidth()/2,
                screenSize.height/2 - dialog.getHeight()/2);

        synchronized (exitCheckForUpdates)
        {
            enterCheckForUpdates(dialog);
            exitCheckForUpdates[0] = true;
        }
        try
        {
            logger.info("Showing update dialog; forceUpdate=" + forceUpdate);
            WindowUtils.makeWindowVisible(dialog, true);
        }
        finally
        {
            synchronized (exitCheckForUpdates)
            {
                if (exitCheckForUpdates[0] && dialog.isModal())
                    exitCheckForUpdates(dialog);
            }
        }
    }

    /**
     * Quit the application using the shutdown service to ensure a clean and
     * consistent shutdown behaviour.
     */
    private static void forceQuitApplication()
    {
        logger.error("Forcing quit of application");
        ServiceUtils.shutdownAll(UpdateWindowsActivator.bundleContext);
    }

    /**
     * Update to a later version if one's available, without showing the user a pop-up asking them.
     * @throws IOException
     */
    public void updateIfAvailable()
    {
        try
        {
            if (!isLatestVersion() && (sDownloadLink != null))
            {
                windowsUpdateInNewThread(false);
            }
            else
            {
                logger.warn("Asked to update when no update is available");
            }
        }
        catch (IOException e)
        {
            logger.warn("Error updating", e);
        }
    }

    private static void windowsUpdateInNewThread(boolean forceUpdate)
    {
        Runnable update = () -> {
            enterCheckForUpdates(null);
            try
            {
                windowsUpdate(forceUpdate);
            }
            finally
            {
                exitCheckForUpdates(null);
            }
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
     * @param forceUpdate - whether this update is enforced
     */
    private static void windowsUpdate(boolean forceUpdate)
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

                // We've got an installer, so next we need to shut down the
                // client to install the new version.  Before we do that, we
                // display a dialog to warn the user of this and ask them to
                // confirm they are happy to proceed.  That is unless we have
                // been asked to force upgrade, in which case we simply shut
                // down, as the user has no choice.
                boolean[] startUpgradeConfirmed = new boolean[]{forceUpdate};
                if (!startUpgradeConfirmed[0])
                {
                    SwingUtilities.invokeAndWait(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            startUpgradeConfirmed[0] =
                                UpdateWindowsActivator.getUIService().getPopupDialog().showConfirmPopupDialog(
                                        resources.getI18NString("plugin.updatechecker.DIALOG_WARN"),
                                        resources.getI18NString("plugin.updatechecker.DIALOG_TITLE"),
                                        PopupDialog.YES_NO_OPTION,
                                        PopupDialog.QUESTION_MESSAGE)
                                == PopupDialog.YES_OPTION;
                        }
                    });
                }

                if (startUpgradeConfirmed[0])
                {
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
                else
                {
                    logger.debug("User cancelled upgrade so not shutting down.");
                }
            }
        }
        catch (Exception exception)
        {
            String title;
            String msg;
            String buttonText;
            logger.error("Error downloading update", exception);
            ResourceManagementService res = Resources.getResources();

            if (exception instanceof IOException)
            {
                title = res.getI18NString("plugin.updatechecker.DIALOG_UPDATE_FAILED_TITLE");
                msg = res.getI18NString("plugin.updatechecker.DIALOG_UPDATE_FAILED_BODY");
                buttonText = res.getI18NString("service.gui.OK");
            }
            else
            {
                title = res.getI18NString("plugin.updatechecker.DIALOG_NOUPDATE_TITLE");
                msg = res.getI18NString("plugin.updatechecker.DIALOG_MISSING_UPDATE");
                buttonText = null;
            }

            ErrorDialog errorDialog = new ErrorDialog(null,
                                                      title,
                                                      msg,
                                                      (String)null,
                                                      buttonText);
            if (forceUpdate)
            {
                // If this is a forced update we must close the program when the
                // error dialog is dismissed
                errorDialog.setModal(true);
                errorDialog.setVisible(true);
                forceQuitApplication();
            }
            else
            {
                // Otherwise just show the error dialog
                errorDialog.setVisible(true);
            }
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
}
