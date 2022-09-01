/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.sysactivity;

import net.java.sip.communicator.util.*;

/**
 * @author Damian Minkov
 */
public class SystemActivityNotifications
{
    private static final Logger logger = Logger.getLogger(SystemActivityNotifications.class);

    /**
     * Notify that computers is going to sleep.
     */
    public static final int NOTIFY_SLEEP = 0;

    /**
     * Notify that computer is waking up after stand by.
     */
    public static final int NOTIFY_WAKE = 1;

    /**
     * Computer display has stand by.
     */
    public static final int NOTIFY_DISPLAY_SLEEP = 2;

    /**
     * Computer display wakes up after stand by.
     */
    public static final int NOTIFY_DISPLAY_WAKE = 3;

    /**
     * Screensaver has been started.
     */
    public static final int NOTIFY_SCREENSAVER_START = 4;

    /**
     * Screensaver will stop.
     */
    public static final int NOTIFY_SCREENSAVER_WILL_STOP = 5;

    /**
     * Screensaver has been stopped.
     */
    public static final int NOTIFY_SCREENSAVER_STOP = 6;

    /**
     * Screen has been locked.
     */
    public static final int NOTIFY_SCREEN_LOCKED = 7;

    /**
     * Screen has been unlocked.
     */
    public static final int NOTIFY_SCREEN_UNLOCKED = 8;

    /**
     * A change in network configuration has occurred.
     */
    public static final int NOTIFY_NETWORK_CHANGE = 9;

    /**
     * A change in dns configuration has occurred.
     */
    public static final int NOTIFY_DNS_CHANGE = 10;

    /**
     * Notifies for start of process of ending desktop session,
     * logoff or shutdown.
     */
    public static final int NOTIFY_QUERY_ENDSESSION = 11;

    /**
     * All processes have been informed about ending session, now notify for
     * the actual end session.
     */
    public static final int NOTIFY_ENDSESSION = 12;

    /**
     * On Mac this points to a native AISleepNotification object. On Windows it points to the thread running the 'message loop'.
     */
    private static long ptr;

    /**
     * Init native library.
     */
    static
    {
        try
        {
            System.loadLibrary("sysactivitynotifications");

            ptr = allocAndInit();
            if (ptr == -1)
            {
                ptr = 0;
            }
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
                logger.warn("Failed to initialize native counterpart", t);
        }
    }

    /**
     * Allocate native resources and gets a pointer.
     *
     * @return
     */
    private static native long allocAndInit();

    /**
     * Returns the when was last input in milliseconds. The time when there was
     * any activity on the computer.
     *
     * @return the last input in milliseconds
     */
    public static native long getLastInput();

    /**
     * Whether native library is loaded.
     *
     * @return whether native library is loaded.
     */
    public static boolean isLoaded()
    {
        return (ptr != 0);
    }

    /**
     * Sets delegate.
     *
     * @param delegate
     */
    public static void setDelegate(NotificationsDelegate delegate)
    {
        if (isLoaded())
        {
            setDelegate(ptr, delegate);
        }
    }

    public static native void setDelegate(long ptr, NotificationsDelegate delegate);

    /**
     * Start.
     */
    public static void start()
    {
        if (isLoaded())
        {
            start(ptr);
        }
    }

    private static native void start(long ptr);

    /**
     * Stop.
     */
    public static void stop()
    {
        if (isLoaded())
        {
            stop(ptr);
            release(ptr);
            ptr = 0;
        }
    }

    private static native void stop(long ptr);

    private static native void release(long ptr);

    /**
     * Delegate class to be notified about changes.
     */
    public interface NotificationsDelegate
    {
        /**
         * Callback method when receiving notifications.
         *
         * @param type
         */
        void notify(int type);

        /**
         * Callback method when receiving special network notifications.
         *
         * @param family family of network change (ipv6, ipv4)
         * @param luidIndex unique index of interface
         * @param name name of the interface
         * @param type of the interface
         * @param connected whether interface is connected or not.
         */
        void notifyNetworkChange(
                int family,
                long luidIndex,
                String name,
                long type,
                boolean connected);
    }
}
