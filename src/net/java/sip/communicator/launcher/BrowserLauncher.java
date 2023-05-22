/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.launcher;

import java.awt.Desktop;
import java.net.URI;

import net.java.sip.communicator.util.ProcessLogger;
import net.java.sip.communicator.util.ProcessUtils;

/**
 * A simple implementation of the BrowserLauncherService. Checks the operating
 * system and launches the appropriate browser.
 *
 * @author Yana Stamcheva
 */
public class BrowserLauncher
{
    private static final ProcessLogger processLogger = ProcessLogger.getLogger();

    /**
     * Creates a <tt>LaunchBrowser</tt> thread for the specified <tt>url</tt>.
     *
     * @param url the url we'd like to launch a browser for.
     */
    public void openURL(String url)
    {
        new LaunchBrowser(url).start();
    }

    /**
     * Launch browser in a separate thread.
     */
    private static class LaunchBrowser extends Thread
    {
        /**
         * The URL we'd be launching a browser for.
         */
        private final String url;

        /**
         * Creates a new instance.
         *
         * @param url the url we'd like to launch a browser for.
         */
        public LaunchBrowser(String url)
        {
            super("BrowserLauncherThread");
            this.url = url;
        }

        /**
         * On mac, asks FileManager to open the url, on Windows uses
         * FileProtocolHandler to do so, on Linux, loops through a list of
         * known browsers until we find one that seems to work.
         */
        @SuppressWarnings("deprecation")
        public void run()
        {
            try
            {
                /*
                 * XXX The detection of the operating systems is the
                 * responsibility of OSUtils. It used to reside in the util.jar
                 * which is in the classpath but it is now in libjitsi.jar which
                 * is not in the classpath.
                 */
                String osName = System.getProperty("os.name");

                if ((osName != null) && osName.startsWith("Mac"))
                {
                    URI uri = new URI(url);
                    Desktop.getDesktop().browse(uri);
                }
                else if ((osName != null) && osName.startsWith("Windows"))
                {
                    String command = "rundll32 url.dll,FileProtocolHandler " + url;
                    processLogger.traceExec(command);
                    Process p = Runtime.getRuntime().exec(command);
                    p.waitFor();
                    ProcessUtils.dispose(p);
                }
                else
                {
                   /* Linux and other Unix systems */
                   String[] browsers = {"firefox", "iceweasel", "opera",
                             "konqueror", "epiphany", "mozilla", "netscape" };

                   String browser = null;

                   for (int i = 0; i < browsers.length && browser == null; i ++)
                   {
                       String[] command = new String[]{"which", browsers[i]};
                       processLogger.traceExec(command);
                       if (Runtime.getRuntime().exec(command).waitFor() == 0)
                       {
                           browser = browsers[i];
                           break;
                       }
                   }
                   if (browser == null)
                   {
                       throw new Exception("Could not find web browser");
                   }
                   else
                   {
                       String[] command = new String[]{browser, url};
                       processLogger.traceExec(command);
                       Runtime.getRuntime().exec(command);
                   }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
