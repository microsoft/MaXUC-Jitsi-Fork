// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * Used to ensure application windows are stacked appropriately. Maintains a
 * list of windows, and places new windows on top of the most recent
 * previously-created window, with a small offset to allow the previous windows
 * to remain visible.
 */
public class WindowStacker extends WindowAdapter
{
    private static final Logger sLog = Logger.getLogger(WindowStacker.class);

    /**
     * The number of pixels below and to the right of the previous window that
     * the new window should be placed.
     */
    private static final int STACKING_OFFSET_PIXELS = 30;

    /**
     * Keeps track of the windows we are currently managing, so that new windows
     * can be positioned relative to the most recent open window. We ensure
     * that all access to this list is done on the EDT thread so no separate
     * synchronization is necessary.
     */
    private final ArrayList<Window> mWindowsList = new ArrayList<>();

    /**
     * If we are not currently tracking the specified window, we begin tracking
     * it and stack it on top of the existing windows. It is placed on top of
     * the most recently created window with a small offset so that a portion
     * of the old window is still visible. If this would put the new window out
     * of bounds, the largest possible offset such that this does not occur is
     * used.  If we are already tracking the window, we do nothing.
     *
     * Note that this method does not set the visibility of the window and it
     * will automatically be run on the EDT thread.
     *
     * Note that this method will not change the position of a new window if
     * there are no existing windows.  The calling code is responsible for
     * setting the position to a sensible default (e.g. centered, or a saved
     * position).
     *
     * @param window The window to track and stack.
     */
    public void addWindow(final Window window)
    {
        // Make sure we're running on the EDT thread.
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addWindow(window);
                }
            });
            return;
        }

        if (mWindowsList.contains(window))
        {
            sLog.debug(
                "Ignoring request to add window that's already added - " +
                                   window.getName() + ": " + window.getClass());
            return;
        }

        sLog.debug("Stacking and tracking a new window - " +
                                   window.getName() + ": " + window.getClass());

        // Add a window listener so we know to stop tracking this window when
        // it is closed.
        window.addWindowListener(this);

        // Pack the window before setting its location, otherwise it will
        // return incorrect width and height.
        window.pack();

        if (mWindowsList.isEmpty())
        {
            // No existing visible windows displayed.  Leave this new window in
            // its default position, which should have been set already.
        }
        else
        {
            Window previousWindow = mWindowsList.get(mWindowsList.size() - 1);

            // Place the new window over the most recent window, with an offset
            // so the old window is still visible.
            int targetX = previousWindow.getX() + STACKING_OFFSET_PIXELS;
            int targetY = previousWindow.getY() + STACKING_OFFSET_PIXELS;

            // Ensure we haven't gone out of bounds.
            GraphicsEnvironment gEnv =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            int screenWidth = gEnv.getMaximumWindowBounds().width;
            int screenHeight = gEnv.getMaximumWindowBounds().height;
            int furthestX = targetX + window.getWidth();
            int furthestY = targetY + window.getHeight();
            int adjustmentX = Math.max(0, (furthestX - screenWidth));
            int adjustmentY = Math.max(0, (furthestY - screenHeight));
            targetX -= adjustmentX;
            targetY -= adjustmentY;

            window.setLocation(targetX, targetY);

            if (window instanceof SIPCommFrame)
            {
                // SIPCommFrames revert to their saved location when setVisible
                // is called. Prevent us jumping back.
                ((SIPCommFrame) window).saveLocation();
            }
        }

        mWindowsList.add(window);
    }

    @Override
    public void windowClosed(final WindowEvent e)
    {
        // Make sure we're running on the EDT thread.
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    windowClosed(e);
                }
            });
            return;
        }

        // One of the windows we are managing has been closed.
        // We shouldn't use it to position future windows, so stop tracking it.
        Window window = e.getWindow();
        sLog.debug("Stopping tracking a window that has been closed - " +
                                   window.getName() + ": " + window.getClass());
        mWindowsList.remove(window);
    }
}
