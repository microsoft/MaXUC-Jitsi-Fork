/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.launcher;

import static net.java.sip.communicator.launcher.SIPCommunicator.PNAME_SC_HOME_DIR_NAME;
import static net.java.sip.communicator.util.PrivacyUtils.sanitiseFilePath;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;
import java.util.UUID;

import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.launchutils.LaunchOSUtils;

public class ElectronUILauncher
{
    private static final Logger logger = Logger.getLogger(ElectronUILauncher.class);

    /**
     * The name of the property that stores the Electron executable name on Mac (non-ASCII).
     */
    public static final String PNAME_ELECTRON_APP_NAME =
            "net.java.sip.communicator.ELECTRON_APP_NAME";

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
    /**
     * Configuration property name for the host port we're connecting to.
     */
    private static final String HOSTPORT_PROPERTY =
            "net.java.sip.communicator.plugin.wispa.HOSTPORT";
    private static final String WISPA_DIR_NAME = "wispa";
    private static final String WISPA_SERVER_KEY_STORE = "server.ks.pfx";
    private static final String WISPA_SERVER_TRUST_STORE = "server.ts.pfx";
    private static final String WISPA_SERVER_CERTIFICATE = "server.cert";
    private static final String WISPA_CLIENT_KEY_STORE = "client.ks.pfx";

    /**
     * For local ant builds we use a pre-defined port. This is because a local ant build will be running alongside an
     * Electron instance launched with npm. This means there's no executable that we can pass a port to at runtime so we
     * need Electron and Java to use a pre-defined port in this instance.
     * <p>
     * This MUST match the port defined in the Electron codebase.
     */
    private static final int PORT_DEFAULT = 9092;

    /**
     * When attempting to find an available port, we restrict our search to a range of ports that are unlikely to be in
     * use.
     */
    private static final int PORT_RANGE_START = 9100;
    private static final int PORT_RANGE_END = 9999;

    /**
     * True if we are using a local ant client.  This is a cheap way to check this since the DIR_NAME is Jitsi if and
     * only if the client is running locally from ant.
     */
    private static boolean isLocalAntBuild()
    {
        return "Jitsi".equals(System.getProperty(PNAME_SC_HOME_DIR_NAME));
    }

    /**
     * Starts the electron client. Must be called after the SIPCommunicatorLock is obtained to avoid creating orphaned
     * processes. THIS IS DUPLICATED IN ElectronApiConnector.java because: - sc-launcher (this bundle) starts before all
     * other bundles, so can't use other services - sc-launcher doesn't have a service, so other bundles can't use it IF
     * YOU MAKE CHANGES HERE, MAKE THE SAME CHANGES THERE AS WELL.
     */
    public static void startElectronUI()
    {
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
        setupWispaPropertiesIfRunningAsInstalledApp();

        // We don't want to try to start the Electron UI if we're using a local
        // ant client, as there won't be a bundled Electron UI app that we can
        // start.  This is a cheap way to check this since the DIR_NAME is Jitsi
        // if and only if the client is running locally from ant.
        if (isLocalAntBuild())
        {
            logger.warn("Local ant build so Electron UI must be started with 'npm start'");
            return;
        }

        if (LaunchOSUtils.isMac())
        {
            try
            {
                logger.info("Starting Electron UI on Mac");

                // The ELECTRON_APP_NAME is passed in on the command line, and taken from the
                // non-ASCII product name, which is the name electron uses for its executable on Mac.
                String electronAppName = System.getProperty(PNAME_ELECTRON_APP_NAME);
                // Current working directory is <MaX UC App Dir>/Contents/Java,
                // and the Electron app is <MaX UC App Dir>/Contents/Frameworks/<electronAppName>.app
                File electronApp = new File("../Frameworks/", electronAppName + ".app");
                String electronAppAbsolutePath = electronApp.getAbsolutePath();
                logger.info("Starting Electron UI at: " + sanitiseFilePath(electronAppAbsolutePath));
                new ProcessBuilder("open", electronAppAbsolutePath).start();
                logger.info("Started Electron UI at: " + sanitiseFilePath(electronAppAbsolutePath));
            }
            catch (Exception e)
            {
                logger.error("Hit Exception Starting Electron UI on Mac", e);
                // We hit errors trying to start the electron UI, so re-enable
                // the Java dock and menu icon so that the user has something
                // they can interact with.
                // TODO: ROBUSTNESS https://jira.metaswitch.com/browse/ACM-4440
                // For GA we will need better error checking, specific Exception
                // handling, probably a retry mechanism for starting the Electron
                // client and monitoring to check that it is (still) running.
                //
                // This code is copied directly from OSUtils.showMacDockAndMenu(). We
                // can't use OSUtils from here as it blocks some logs from appearing
                // (see https://jira.metaswitch.com/browse/BUG-4558).
                //
                // DUIR-509 may mean that we can retire this snippet below now that
                // we have better fallback behaviour.
                logger.info("Showing Java app dock icon and menu bar on Mac");

                // False shows the UI; true hides the UI.
                System.setProperty("apple.awt.UIElement", "false");
            }
        }
        else if (LaunchOSUtils.isWindows())
        {
            try
            {
                logger.info("Starting Electron UI on Windows");

                // The SC_HOME_DIR_NAME is passed in on the command line, and taken from the
                // ASCII product name, which is the name electron uses for its executable on Windows.
                String electronAppName = System.getProperty(PNAME_SC_HOME_DIR_NAME);
                // Current working directory is C:\\Program Files (x86)\\<MaX UC App Dir>,
                // and the Electron app is at C:\\Program Files (x86)\\<MaX UC App Dir>\\ui
                File electronApp = new File("ui/", electronAppName + ".exe");
                String electronAppAbsolutePath = electronApp.getAbsolutePath();
                logger.info("Starting Electron UI at: " + sanitiseFilePath(electronAppAbsolutePath));
                new ProcessBuilder(electronAppAbsolutePath).start();
                logger.info("Started Electron UI at: " + sanitiseFilePath(electronAppAbsolutePath));
            }
            catch (Exception e)
            {
                // TODO: ROBUSTNESS https://jira.metaswitch.com/browse/ACM-4440
                // This kind of error logging will not be acceptable for GA. We will
                // return to improve this error messaging under the work done to improve
                // client robustness.
                logger.error("Hit Exception Starting Electron UI on Windows", e);
            }
        }
        else
        {
            logger.error("Can't start Electron UI because the OS is not" +
                         " recognised. OS name is: " + LaunchOSUtils.OS_NAME);
        }
    }

    /**
     * Calculates the port that will be used for the web server to exposed over. For local and FV builds we use a
     * pre-defined port - this is because those builds don't use executables. For installer builds we look through a
     * range of ports randomly until we find one that's free.
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
     * Set up the properties required for WISPA keys and certificates, but only if we're running as part of the
     * installed application (as opposed to a local dev build).
     */
    private static void setupWispaPropertiesIfRunningAsInstalledApp()
    {
        if (!isLocalAntBuild())
        {
            // We're running as part of an installed application.  Use WISPA over WSS.
            setupWispaProperties();
        }
    }

    /**
     * Set up the properties required for WISPA keys and certificates.
     * <p>
     * On starting the Electron app, we pass in the server certificate, so that it can know it has connected to the
     * correct endpoint.  We also pass in an encrypted keystore, containing the client's certificate and private key,
     * along with the passphrase required to decrypt it.  The client presents its certificate when connecting, allowing
     * the server to only accept connections from trusted clients.
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
                          System.getProperty(PNAME_SC_HOME_DIR_NAME) + fileSeparator +
                          WISPA_DIR_NAME + fileSeparator;
        System.setProperty(WISPA_KEYS_DIR_PROPERTY, wispaDir);
        // output relative path than absolute path
        logger.debug("WISPA keys directory: " + fileSeparator + WISPA_DIR_NAME + fileSeparator);

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
}
