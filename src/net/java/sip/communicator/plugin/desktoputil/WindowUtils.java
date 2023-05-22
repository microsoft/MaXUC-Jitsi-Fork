/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

import javax.swing.JOptionPane;

import org.jitsi.service.resources.BufferedImageFuture;

/**
 * Utility class for awt windows management.
 *
 * @author Yana Stamcheva
 */
public class WindowUtils
{
    /**
     * The list of all <tt>Window</tt>s owned by this application.
     */
    private static final List<Window> WINDOW_LIST;

    static
    {
        /*
         * WINDOW_LIST is flawed because there are more calls to addWindow than
         * to removeWindow. Java 6 has introduced Window#getWindows so try to
         * use it instead.
         */
        Method Window_getWindows = null;

        try
        {
            Window_getWindows = Window.class.getMethod("getWindows");
        }
        catch (NoSuchMethodException nsme)
        {
            /*
             * Ignore the exception because we are just checking whether the
             * method exists.
             */
        }
        catch (SecurityException se)
        {
        }
        WINDOW_LIST
            = (Window_getWindows == null) ? new ArrayList<>() : null;
    }

    /**
     * Returns an array of all {@code Window}s, both owned and ownerless,
     * created by this application.
     * If called from an applet, the array includes only the {@code Window}s
     * accessible by that applet.
     * <p>
     * <b>Warning:</b> this method may return system created windows, such
     * as a print dialog. Applications should not assume the existence of
     * these dialogs, nor should an application assume anything about these
     * dialogs such as component positions, <code>LayoutManager</code>s
     * or serialization.
     *
     * @return Returns an array of all {@code Window}s.
     */
    public static Window[] getWindows()
    {
        if (WINDOW_LIST == null)
        {
            Method Window_getWindows = null;

            try
            {
                Window_getWindows = Window.class.getMethod("getWindows");
            }
            catch (NoSuchMethodException nsme)
            {
                /* Ignore it because we cannot really do anything useful. */
            }
            catch (SecurityException se)
            {
            }

            Object windows = null;

            if (Window_getWindows != null)
            {
                try
                {
                    windows = Window_getWindows.invoke(null);
                }
                catch (ExceptionInInitializerError eiie)
                {
                    /* Ignore it because we cannot really do anything useful. */
                }
                catch (IllegalAccessException | NullPointerException | InvocationTargetException | IllegalArgumentException iae)
                {
                }
            }

            return
                (windows instanceof Window[])
                    ? (Window[]) windows
                    : new Window[0];
        }
        else
        {
            synchronized (WINDOW_LIST)
            {
                return WINDOW_LIST.toArray(new Window[WINDOW_LIST.size()]);
            }
        }
    }

    /**
     * Adds a {@link Window} into window list
     *
     * @param w {@link Window} to be added.
     */
    public static void addWindow(Window w)
    {
        if (WINDOW_LIST != null)
        {
            synchronized (WINDOW_LIST)
            {
                if (!WINDOW_LIST.contains(w))
                    WINDOW_LIST.add(w);
            }
        }
    }

    /**
     * Removes a {@link Window} into window list
     *
     * @param w {@link Window} to be removed.
     */
    public static void removeWindow(Window w)
    {
        if (WINDOW_LIST != null)
        {
            synchronized (WINDOW_LIST)
            {
                WINDOW_LIST.remove(w);
            }
        }
    }

    /**
     * Set the default icons for a window.
     *
     * @param window the window
     */
    public static void setWindowIcons(Window window)
    {
        try
        {
            List<BufferedImage> logos = new ArrayList<BufferedImage>(6)
            {
                private static final long serialVersionUID = 0L;
                {
                    add(DesktopUtilActivator.getImage(
                        "service.gui.SIP_COMMUNICATOR_LOGO").resolve());
                    add(DesktopUtilActivator.getImage(
                        "service.gui.SIP_COMMUNICATOR_LOGO_20x20").resolve());
                    add(DesktopUtilActivator.getImage(
                        "service.gui.SIP_COMMUNICATOR_LOGO_32x32").resolve());
                    add(DesktopUtilActivator.getImage(
                        "service.gui.SIP_COMMUNICATOR_LOGO_45x45").resolve());
                    add(DesktopUtilActivator.getImage(
                        "service.gui.SIP_COMMUNICATOR_LOGO_64x64").resolve());
                    add(DesktopUtilActivator.getImage(
                        "service.gui.SIP_COMMUNICATOR_LOGO_128x128").resolve());
                }
            };
            window.setIconImages(logos);
            // In order to have the same icon when using option panes
            JOptionPane.getRootFrame().setIconImages(logos);
        }
        catch (Exception e)
        {
            BufferedImageFuture scLogo
                = DesktopUtilActivator.getImage("service.gui.SIP_COMMUNICATOR_LOGO");

            // During tests, we don't actually get a logo
            if (scLogo != null)
            {
                scLogo.addToWindow(window);
                // In order to have the same icon when using option panes
                scLogo.addToWindow(JOptionPane.getRootFrame());
            }
        }
    }

    /**
     * Makes the best effort we can to make a window visible, at the top of the z-order.
     *
     * @param window the window
     */
    public static void makeWindowVisible(Window window, boolean shouldBeAlwaysOnTop)
    {
        // Flags the window should request focus as soon as it is shown
        window.setAutoRequestFocus(true);

        // 'setAlwaysOnTop' will bring the window to front, if supported.
        // Since it's not guaranteed (and the value of 'shouldBeAlwaysOnTop'
        // may be false), the 'toFront' method is called anyway.
        // We set always on top to be true initially because this seems to be
        // a reliable way to bring a window to the front, then cancel it if the
        // window isn't actually supposed to always be on top.
        window.setAlwaysOnTop(true);
        window.setAlwaysOnTop(shouldBeAlwaysOnTop);
        window.toFront();

        // Some windows might wait to be closed before exiting the
        // 'setVisible' method. Therefore, call it after everything apart from
        // our attempts to set focus. These must happen after the window is visible.
        // They won't necessarily be called if setVisible doesn't return though -
        // they're just another attempt to get focus.
        window.setVisible(true);

        window.requestFocus();
        window.requestFocusInWindow();
    }
}
