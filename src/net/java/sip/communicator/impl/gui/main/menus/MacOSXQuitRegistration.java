/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.Desktop;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitResponse;

import net.java.sip.communicator.util.*;

/**
 * @author Lubomir Marinov
 */
public final class MacOSXQuitRegistration
{
    private static final Logger logger =
        Logger.getLogger(MacOSXQuitRegistration.class);

    public static boolean run(final Object userData)
    {
        Desktop desktop = Desktop.getDesktop();

        if (desktop != null)
        {
            desktop.setQuitHandler(new QuitHandler()
            {
                public void handleQuitRequestWith(QuitEvent quitEvent,
                                              final QuitResponse quitResponse)
                {
                    logger.info("Mac Quit request received");
                    ((FileMenu) userData).beginShutdown(false, quitResponse);

                }
            });

            return true;
        }
        return false;
    }
}
