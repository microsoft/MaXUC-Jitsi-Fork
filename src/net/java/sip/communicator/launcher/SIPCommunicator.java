/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.launcher;

import static java.util.stream.Collectors.joining;
import static net.java.sip.communicator.util.launchutils.LaunchArgHandler.sanitiseArgument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.launchutils.LaunchArgHandler;
import net.java.sip.communicator.util.launchutils.LaunchOSUtils;
import net.java.sip.communicator.util.launchutils.SipCommunicatorLock;

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

    private static final List<String> logBuffer = new ArrayList<>();

    /**
     * Starts the SIP Communicator.
     * <p>
     * Does the minimal amount of setup required to analyze our launch
     * arguments and determine if this is a main or secondary instance,
     * before either quitting or handing off to SIPCommunicatorMainInstance
     * if we are the main instance.
     *
     * @param args command line args if any
     *
     * @throws Exception whenever it makes sense.
     */
    public static void main(String[] args) throws Exception
    {
        setSystemProperties();

        // Must set up home directory before we attempt anything else, including
        // logging or starting Felix - hence pass in a log buffer to save
        // off log messages that we want to make later
        HomeDirectoryConfigurer.configure(logBuffer);

        // this needs to be set before any DNS lookup is run
        // 2024 note: the file used by this function seems not to exist, is this dead code?
        setupDnsSettings();

        // have logging and home dir setup - can now handle arguments to our arg handler
        LaunchArgHandler argHandler = LaunchArgHandler.getInstance();
        int argHandlerRes = argHandler.handleArgs(args);

        if ( argHandlerRes == LaunchArgHandler.ACTION_EXIT
             || argHandlerRes == LaunchArgHandler.ACTION_ERROR)
        {
            System.exit(argHandler.getErrorCode());
        }

        logStartup(args);

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
                            + "Parameters = ["
                            + Stream.of(args).map(LaunchArgHandler::sanitiseArgument).collect(joining(", "))
                            + "]\n"
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

        // We are now sure that we're running as a main instance,
        // so hand off to SIPCommunicatorMainInstance which will do
        // the rest of the startup e.g. start Felix
        SIPCommunicatorMainInstance.startup();
    }

    private static void setupDnsSettings()
    {
        File f = new File(System.getProperty(PNAME_SC_HOME_DIR_LOCATION),
                          System.getProperty(PNAME_SC_HOME_DIR_NAME) + File.separator + ".usednsjava");

        if (f.exists()) // CodeQL [SM00697] Not Exploitable. The file/path is not user provided.
        {
            System.setProperty("sun.net.spi.nameservice.provider.1", "dns,dnsjava");
        }
    }

    private static void logStartup(String[] args)
    {
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
            logger.error("With arg: " + sanitiseArgument(arg));
        }

        flushLogBuffers();
    }

    private static void flushLogBuffers()
    {
        for (String message: logBuffer)
        {
            logger.info(message);
        }
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
        if (LaunchOSUtils.isWindows())
        {
            // disable Direct 3D pipeline (used for fullscreen) before
            // displaying anything (frame, ...)
            System.setProperty("sun.java2d.d3d", "false");
        }
        else if (LaunchOSUtils.isMac())
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

}
