/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.globalshortcut;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.*;
// disambiguation

/**
 * UI shortcut.
 *
 * @author Sebastien Vincent
 */
public class UIShortcut
    implements GlobalShortcutListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>UIShortcut</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(UIShortcut.class);

    /**
     * Keybindings service.
     */
    private KeybindingsService keybindingsService =
        GlobalShortcutActivator.getKeybindingsService();

    /**
     * Callback when an shortcut is typed
     *
     * @param evt <tt>GlobalShortcutEvent</tt>
     */
    public synchronized void shortcutReceived(GlobalShortcutEvent evt)
    {
        AWTKeyStroke keystroke = evt.getKeyStroke();
        logger.debug("Shortcut received " + keystroke);
        GlobalKeybindingSet set = keybindingsService.getGlobalBindings();

        if(keystroke == null)
            return;

        try
        {
            for(Map.Entry<String, List<AWTKeyStroke>> entry :
                set.getBindings().entrySet())
            {
                for(AWTKeyStroke ks : entry.getValue())
                {
                    if(ks == null)
                        continue;

                    if(entry.getKey().equals("contactlist") &&
                        keystroke.getKeyCode() == ks.getKeyCode() &&
                        keystroke.getModifiers() == ks.getModifiers())
                    {
                        ExportedWindow window =
                            GlobalShortcutActivator.getUIService().
                                getExportedWindow(ExportedWindow.MAIN_WINDOW);

                        if(window == null)
                            return;

                        setVisible(window, window.isVisible());
                    }
                }
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;

            logger.info("Failed to execute contactlist action", t);
        }
    }

    /**
     * Set the window visible or not
     *
     * @param window the <tt>ExportedWindow</tt> to set/unset visible.
     * @param visible enable or not the window to be visible
     */
    private void setVisible(final ExportedWindow window, final boolean visible)
    {
        new Thread("MakeVisibleThread")
        {
            public void run()
            {
                if(!visible)
                {
                    window.bringToFront();
                    window.setVisible(true);

                    if(window instanceof Window)
                    {
                        ((Window)window).setAlwaysOnTop(true);
                        ((Window)window).setAlwaysOnTop(false);
                    }
                }
                else
                {
                    window.setVisible(false);
                }
            }
        }.start();
    }
}
