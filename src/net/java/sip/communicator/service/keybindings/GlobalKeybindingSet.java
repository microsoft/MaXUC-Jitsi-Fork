/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.keybindings;

import java.awt.*;
import java.util.*;
import java.util.List;
//disambiguation

/**
 * Global keybinding set interface.
 *
 * @author Sebastien Vincent
 */
public interface GlobalKeybindingSet
{
    /**
     * Provides current keybinding mappings.
     * @return mapping of keystrokes to the string representation of the actions
     * they perform
     */
    Map<String, List<AWTKeyStroke>> getBindings();

    /**
     * Resets the bindings and notifies the observer's listeners if they've
     * changed.
     * @param bindings new keybindings to be held
     */
    void setBindings(Map<String, List<AWTKeyStroke>> bindings);
}
