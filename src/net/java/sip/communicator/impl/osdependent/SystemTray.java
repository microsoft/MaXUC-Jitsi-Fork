/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.osdependent;

import java.awt.*;
import java.lang.reflect.*;

import javax.swing.*;

import net.java.sip.communicator.impl.osdependent.TrayIcon.AWTTrayIconPeer;
import net.java.sip.communicator.impl.osdependent.TrayIcon.TrayIconPeer;
import net.java.sip.communicator.util.*;

/**
 * @author Lubomir Marinov
 */
public class SystemTray
{
    /**
     * The <tt>Logger</tt> used by the <tt>SystemTray</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(SystemTray.class);

    private static SystemTray defaultSystemTray;

    public static SystemTray getDefaultSystemTray()
        throws UnsupportedOperationException,
            SecurityException
    {
        if (defaultSystemTray != null)
            return defaultSystemTray;

        SystemTrayPeer peer = null;
        try
        {
            peer = new AWTSystemTrayPeer(java.awt.SystemTray.class);
        }
        catch (Exception ex)
        {
            if(!GraphicsEnvironment.isHeadless())
                logger.error("Failed to initialize java.awt.SystemTray",
                    ex);
        }
        return (defaultSystemTray = new SystemTray(peer));
    }

    private final SystemTrayPeer peer;

    private SystemTray(SystemTrayPeer peer)
    {
        logger.info("Creating system tray with peer " + peer);
        this.peer = peer;
    }

    public void addTrayIcon(TrayIcon trayIcon)
        throws NullPointerException,
        IllegalArgumentException
    {
        if (peer != null)
            peer.addTrayIcon(trayIcon.getPeer());
    }

    SystemTrayPeer getPeer()
    {
        return peer;
    }

    public boolean isSwing()
    {
        if (peer != null)
            return getPeer().isSwing();
        return false;
    }

    interface SystemTrayPeer
    {
        void addTrayIcon(TrayIconPeer trayIconPeer)
            throws NullPointerException,
            IllegalArgumentException;

        TrayIconPeer createTrayIcon(ImageIcon icon,
                                    String tooltip,
                                    Object popup)
            throws IllegalArgumentException,
            UnsupportedOperationException,
                SecurityException;

        boolean isSwing();
    }

    private static class AWTSystemTrayPeer
        implements SystemTrayPeer
    {
        private final Method addTrayIcon;

        private final Object impl;

        private final Class<?> trayIconClass;

        public AWTSystemTrayPeer(Class<?> clazz)
            throws UnsupportedOperationException,
                SecurityException
        {
            Method getDefaultSystemTray;
            try
            {
                getDefaultSystemTray =
                    clazz.getMethod("getSystemTray", (Class<?>[]) null);
                trayIconClass = Class.forName("java.awt.TrayIcon");
                addTrayIcon = clazz.getMethod("add", new Class<?>[]
                { trayIconClass });
            }
            catch (ClassNotFoundException | NoSuchMethodException ex)
            {
                throw new UnsupportedOperationException(ex);
            }

            try
            {
                impl = getDefaultSystemTray.invoke(null, (Object[]) null);
            }
            catch (IllegalAccessException | InvocationTargetException ex)
            {
                throw new UnsupportedOperationException(ex);
            }
        }

        public void addTrayIcon(TrayIconPeer trayIconPeer)
            throws NullPointerException,
            IllegalArgumentException
        {
            try
            {
                addTrayIcon.invoke(impl, new Object[]
                { ((AWTTrayIconPeer) trayIconPeer).getImpl() });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UndeclaredThrowableException(ex);
                if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                throw new UndeclaredThrowableException(cause);
            }
        }

        public TrayIconPeer createTrayIcon(ImageIcon icon, String tooltip,
            Object popup)
            throws IllegalArgumentException,
                UnsupportedOperationException,
                SecurityException
        {
            return new AWTTrayIconPeer(trayIconClass, (icon == null) ? null
                : icon.getImage(), tooltip, popup);
        }

        public boolean isSwing()
        {
            return false;
        }
    }
}
