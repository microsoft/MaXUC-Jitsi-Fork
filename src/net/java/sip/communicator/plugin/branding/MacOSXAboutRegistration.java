/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.awt.Desktop;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;

/**
 * @author Lubomir Marinov
 */
public final class MacOSXAboutRegistration
{
    /**
     * Show the about dialog on Mac OS X.
     *
     * @return true if the Mac OS X application is not null
     */
    public static boolean run()
    {
        Desktop desktop = Desktop.getDesktop();
        if (desktop != null)
        {
            desktop.setAboutHandler(new AboutHandler()
            {
                public void handleAbout(AboutEvent aboutEvent)
                {
                    AboutWindowPluginComponent.actionPerformed();
                }
            });
            return true;
        }
        return false;
    }
}
