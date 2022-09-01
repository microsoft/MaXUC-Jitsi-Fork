// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * A simple class for caching windows so that they can be re-shown, rather than
 * creating multiple instances.
 */
public class WindowCacher
{
    private static final Logger sLog = Logger.getLogger(WindowCacher.class);

    /**
     * A map from an object Key to the Window for that Key.  Used to prevent
     * opening multiple windows for the same key.
     */
    private static final HashMap<Object, Window> sWindowMap =
            new HashMap<>();

    /**
     * Get the cached window for the passed in key
     *
     * @param key the key to the cache
     * @return the cached window
     */
    public static Window get(Object key)
    {
        sLog.debug("Getting window at " + key);
        synchronized (sWindowMap)
        {
            return sWindowMap.get(key);
        }
    }

    /**
     * Remove a cached window stored under the key
     *
     * @param key the key of the window to remove
     */
    public static void remove(Object key)
    {
        sLog.debug("Removing window at " + key);
        synchronized (sWindowMap)
        {
            sWindowMap.remove(key);
        }
    }

    /**
     * Cache a window
     *
     * @param key the key to use to cache the window
     * @param window the window to cache
     */
    public static void put(final Object key, Window window)
    {
        sLog.debug("Caching window at " + key);
        Window cachedWindow;

        synchronized (sWindowMap)
        {
            cachedWindow = sWindowMap.get(key);
            sWindowMap.put(key, window);
        }

        if (cachedWindow != null)
        {
            // Shouldn't happen.  But if it does, just dispose of the old one.
            sLog.warn("Cache already contains window for " + key);
            cachedWindow.setVisible(false);
            cachedWindow.dispose();
        }

        window.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentHidden(ComponentEvent e)
            {
                sLog.debug("Contact window closing");
                remove(key);
            }
        });

        window.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                sLog.debug("Contact window being closed");
                remove(key);
            }
        });
    }
}
