/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.*;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.osdependent.SystemTray.SystemTrayPeer;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;

/**
 * @author Lubomir Marinov
 */
public class TrayIcon
{
    private static final Logger logger = Logger.getLogger(TrayIcon.class);

    public static final MessageType ERROR_MESSAGE_TYPE =
        java.awt.TrayIcon.MessageType.ERROR;

    public static final MessageType INFO_MESSAGE_TYPE =
        java.awt.TrayIcon.MessageType.INFO;

    public static final MessageType NONE_MESSAGE_TYPE =
        java.awt.TrayIcon.MessageType.NONE;

    public static final MessageType WARNING_MESSAGE_TYPE =
        java.awt.TrayIcon.MessageType.WARNING;

    private final TrayIconPeer peer;

    public TrayIcon(ImageIcon icon, String tooltip, Object popup)
        throws IllegalArgumentException,
        UnsupportedOperationException,
            SecurityException
    {
        SystemTrayPeer systemTrayPeer =
            SystemTray.getDefaultSystemTray().getPeer();
        if (systemTrayPeer != null)
        {
            peer = systemTrayPeer.createTrayIcon(icon, tooltip, popup);
            logger.info("Creating tray icon with peer " + peer);
        }
        else
        {
            logger.warn("Creating tray icon with null peer");
            peer = null;
        }
    }

    public void addActionListener(ActionListener listener)
    {
        if (peer != null)
            peer.addActionListener(listener);
    }

    public void addMouseListener(MouseListener listener)
    {
        if (peer != null)
            peer.addMouseListener(listener);
    }

    public void addBalloonActionListener(ActionListener listener)
    {
        if (peer != null)
            peer.addBalloonActionListener(listener);
    }

    public void displayMessage(String caption, String text, MessageType messageType)
        throws NullPointerException
    {
        if (peer != null)
        {
            logger.debug("About to display message");
            peer.displayMessage(caption, text, messageType);
        }
        else
        {
            logger.warn("Cannot display message - peer is null");
        }
    }

    TrayIconPeer getPeer()
    {
        return peer;
    }

    public void setIcon(ImageIcon icon) throws NullPointerException
    {
        if (peer != null)
            peer.setIcon(icon);
    }

    public void setIconAutoSize(boolean autoSize)
    {
        if (peer != null)
            peer.setIconAutoSize(autoSize);
    }

    interface TrayIconPeer
    {
        void addActionListener(ActionListener listener);

        void addMouseListener(MouseListener listener);

        void addBalloonActionListener(ActionListener listener);

        void displayMessage(String caption, String text, MessageType messageType)
            throws NullPointerException;

        void setIcon(ImageIcon icon) throws NullPointerException;

        void setIconAutoSize(boolean autoSize);
    }

    static class AWTTrayIconPeer
        implements TrayIconPeer
    {
        private final Method addActionListener;

        private final Method addMouseListener;

        private final Method displayMessage;

        private final Object impl;

        private final Class<?> messageTypeClass;

        private final Method setIcon;

        private final Method setIconAutoSize;

        public AWTTrayIconPeer(Class<?> clazz, Image image, String tooltip,
            Object popup)
            throws IllegalArgumentException,
            UnsupportedOperationException,
                SecurityException
        {
            Constructor<?> constructor;
            try
            {
                if (popup instanceof JPopupMenu)
                {
                    constructor = clazz.getConstructor(new Class<?>[]
                        { Image.class, String.class });
                }
                else
                {
                    constructor = clazz.getConstructor(new Class<?>[]
                        { Image.class, String.class, PopupMenu.class });
                }
                addActionListener =
                    clazz.getMethod("addActionListener", new Class<?>[]
                    { ActionListener.class });
                addMouseListener =
                    clazz.getMethod("addMouseListener", new Class<?>[]
                    { MouseListener.class });
                messageTypeClass =
                    Class.forName("java.awt.TrayIcon$MessageType");
                displayMessage =
                    clazz.getMethod("displayMessage", new Class<?>[]
                    { String.class, String.class, messageTypeClass });
                setIcon = clazz.getMethod("setImage", new Class<?>[]
                { Image.class });
                setIconAutoSize =
                    clazz.getMethod("setImageAutoSize", new Class<?>[]
                    { boolean.class });
            }
            catch (ClassNotFoundException | NoSuchMethodException ex)
            {
                throw new UnsupportedOperationException(ex);
            }

            try
            {
                if (popup instanceof JPopupMenu)
                {
                    impl = constructor.newInstance(
                        new Object[] { image, tooltip });
                    addMouseListener(new AWTMouseAdapter((JPopupMenu) popup));
                }
                else
                {
                    impl = constructor.newInstance(
                        new Object[] { image, tooltip, popup });
                }
            }
            catch (IllegalAccessException | InstantiationException ex)
            {
                throw new UnsupportedOperationException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UnsupportedOperationException(ex);
                if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                if (cause instanceof HeadlessException)
                    throw (HeadlessException) cause;
                if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                throw new UnsupportedOperationException(cause);
            }
        }

        public void addActionListener(ActionListener listener)
        {
            try
            {
                addActionListener.invoke(getImpl(), new Object[]
                { listener });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }

        public void addMouseListener(MouseListener listener)
        {
            try
            {
                addMouseListener.invoke(getImpl(), new Object[] { listener });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }

        public void addBalloonActionListener(ActionListener listener)
        {
            // java.awt.TrayIcon doesn't support addBalloonActionListener()
        }

        public void displayMessage(String caption, String text, MessageType messageType)
            throws NullPointerException
        {
            try
            {
                displayMessage.invoke(getImpl(), new Object[]
                { caption, text, getMessageType(messageType) });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }

        public Object getImpl()
        {
            return impl;
        }

        private Object getMessageType(MessageType messageType)
        {
            Object[] constants = messageTypeClass.getEnumConstants();
            String name;
            switch (messageType)
            {
            case ERROR:
                name = "ERROR";
                break;
            case INFO:
                name = "INFO";
                break;
            case NONE:
                name = "NONE";
                break;
            case WARNING:
                name = "WARNING";
                break;
            default:
                throw new IllegalArgumentException("messageType");
            }
            for (int i = 0; i < constants.length; i++)
            {
                Object constant = constants[i];
                if (name.equals(constant.toString()))
                    return constant;
            }
            throw new IllegalArgumentException("messageType");
        }

        public void setIcon(ImageIcon icon) throws NullPointerException
        {
            try
            {
                setIcon.invoke(getImpl(), new Object[]
                { (icon == null) ? null : icon.getImage() });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }

        public void setIconAutoSize(boolean autoSize)
        {
            try
            {
                setIconAutoSize.invoke(getImpl(), new Object[]
                { autoSize });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }
    }

    /**
     * Extended mouse adapter to show the JPopupMenu in Java 6
     * Based on : http://weblogs.java.net/blog/ixmal/archive/2006/05/using_jpopupmen.html
     * And : http://weblogs.java.net/blog/alexfromsun/archive/2008/02/jtrayicon_updat.html
     *
     * Use a hidden JWindow (JDialog for Windows) to manage the JPopupMenu.
     *
     * @author Damien Roth
     */
    private static class AWTMouseAdapter
        extends MouseAdapter
    {
        private JPopupMenu popup = null;
        private Window hiddenWindow = null;

        public AWTMouseAdapter(JPopupMenu p)
        {
            this.popup = p;
            this.popup.addPopupMenuListener(new PopupMenuListener()
            {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e)
                {}

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
                {
                    if (hiddenWindow != null)
                    {
                        hiddenWindow.dispose();
                        hiddenWindow = null;
                    }
                }

                public void popupMenuCanceled(PopupMenuEvent e)
                {
                    if (hiddenWindow != null)
                    {
                        hiddenWindow.dispose();
                        hiddenWindow = null;
                    }
                }
            });
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            showPopupMenu(e);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            showPopupMenu(e);
        }

        private void showPopupMenu(MouseEvent e)
        {
            if (e.isPopupTrigger() && popup != null)
            {
                if (hiddenWindow == null)
                {
                    if (OSUtils.IS_WINDOWS)
                    {
                        hiddenWindow = new JDialog((Frame) null);
                        ((JDialog) hiddenWindow).setUndecorated(true);
                    }
                    else
                        hiddenWindow = new JWindow((Frame) null);

                    hiddenWindow.setAlwaysOnTop(true);
                    Dimension size = popup.getPreferredSize();

                    Point centerPoint = GraphicsEnvironment
                                            .getLocalGraphicsEnvironment()
                                                .getCenterPoint();

                    if(e.getY() > centerPoint.getY())
                        hiddenWindow
                            .setLocation(e.getX(), e.getY() - size.height);
                    else
                        hiddenWindow
                            .setLocation(e.getX(), e.getY());

                    hiddenWindow.setVisible(true);

                    popup.show(
                            ((RootPaneContainer)hiddenWindow).getContentPane(),
                            0, 0);

                    // popup works only for focused windows
                    hiddenWindow.toFront();
                }
            }
        }
    }
}
