/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.globalshortcut;

import java.awt.*;

/**
 * This global shortcut service permits to register listeners for global
 * shortcut (i.e. keystroke even if application is not foreground).
 *
 * @author Sebastien Vincent
 */
public interface GlobalShortcutService
{
    /**
     * Value for AWTKeyStroke's modifiers that specify a "special" key shortcut.
     */
    int SPECIAL_KEY_MODIFIERS = 16367;

    /**
     * Registers an action to execute when the keystroke is typed.
     *
     * @param l listener to notify when keystroke is typed
     * @param keyStroke keystroke that will trigger the action
     */
    void registerShortcut(GlobalShortcutListener l,
                          AWTKeyStroke keyStroke);

    /**
     * Unregisters an action to execute when the keystroke is typed.
     *
     * @param l listener to remove
     * @param keyStroke keystroke that will trigger the action
     */
    void unregisterShortcut(GlobalShortcutListener l,
                            AWTKeyStroke keyStroke);

    /**
     * Reload global shortcuts.
     */
    void reloadGlobalShortcuts();

    /**
     * Enable or not global shortcut.
     *
     * @param enable enable or not global shortcut
     */
    void setEnable(boolean enable);
}
