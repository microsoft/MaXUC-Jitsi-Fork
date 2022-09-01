/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.Desktop;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.PreferencesEvent;

/**
 * @author Lubomir Marinov
 */
public final class MacOSXPreferencesRegistration
{
    public static boolean run(final Object userData)
    {
        Desktop desktop = Desktop.getDesktop();

        if (desktop != null)
        {
            desktop.setPreferencesHandler(new PreferencesHandler()
            {
                public void handlePreferences(
                    PreferencesEvent preferencesEvent)
                {
                    ((ToolsMenu) userData).configActionPerformed();
                }
            });
            return true;
        }
        return false;
    }
}
