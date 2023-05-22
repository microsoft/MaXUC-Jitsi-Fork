/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import java.beans.*;

import org.jitsi.util.*;

import net.java.sip.communicator.plugin.generalconfig.*;
import net.java.sip.communicator.service.headsetmanager.HeadsetManagerService;
import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.Logger;

/**
 * Listens for idle events from SystemActivityNotifications Service.
 *
 * @author Damian Minkov
 */
public class AutoAwayWatcher
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(AutoAwayWatcher.class);

    /**
     * Listens for idle events.
     */
    private IdleListener idleListener = null;

    private SystemActivityNotificationsService systemActivityService =
           GeneralConfigPluginActivator.getSystemActivityNotificationsService();

    /**
     * The headset manager service
     */
    HeadsetManagerService mHeadsetManager = GeneralConfigPluginActivator.
        getheadsetManagerService();

    private W32SessionNotifier mW32SessionNotifier = null;
    private Thread mSessionNotifierThread = null;

    /**
     * Creates AutoAway handler.
     */
    public AutoAwayWatcher()
    {
        if (Preferences.isEnabled())
        {
            start();
        }

        Preferences.addEnableChangeListener(
                new PropertyChangeListener()
                {
                    public void propertyChange(PropertyChangeEvent evt)
                    {
                        if(Boolean.parseBoolean((String) evt.getNewValue()))
                            start();
                        else
                            stop();
                    }
                }
        );

        // listens for changes in configured value.
        Preferences.addTimerChangeListener(
            new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent evt)
                {
                    stop();
                    start();
                }
            }
        );

        // Set up the session notifier to alert us whenever the user locks or
        // unlocks their workstation
        if (OSUtils.IS_WINDOWS)
        {
            logger.info("Started the W32 Session Notifier");
            mW32SessionNotifier = new W32SessionNotifier(this);

            mSessionNotifierThread = new Thread(mW32SessionNotifier);
            mSessionNotifierThread.setDaemon(true);
            mSessionNotifierThread.setName(this.getClass().toString());
            mSessionNotifierThread.start();
        }
    }

    /**
     * Starts and add needed listeners.
     */
    private void start()
    {
        if (idleListener == null)
        {
            idleListener = new IdleListener();

            systemActivityService.addIdleSystemChangeListener(
                    Preferences.getTimer() * 60 * 1000,
                    idleListener);
            systemActivityService
                .addSystemActivityChangeListener(idleListener);
        }
    }

    /**
     * Stops and removes the listeners.
     */
    public void stop()
    {
        if (mW32SessionNotifier != null)
        {
            mW32SessionNotifier.stop();
        }

        if (idleListener != null)
        {
            systemActivityService.removeIdleSystemChangeListener(
                    idleListener);
            systemActivityService
                .removeSystemActivityChangeListener(idleListener);

            idleListener = null;
        }
    }

    /**
     * Change the global status to be away or not.
     *
     * @param isAway true if we are now away.
     */
    public void changeGlobalAwayState(boolean isAway)
    {
        logger.debug("Updating away to " + isAway);
        GeneralConfigPluginActivator.getGlobalStatusService().setAway(isAway);
    }

    /**
     * Called when the computer's locked state changes.
     *
     * @param locked true if the user's computer is locked. False otherwise.
     */
    private void lockStateChanged(boolean locked)
    {
        logger.debug("Lock state changed to: " + locked);

        // Update the global away state as if the computer has been locked or
        // unlocked the away state has also changed.
        changeGlobalAwayState(locked);
        mHeadsetManager.lockStateChanged(locked);
    }

    /**
     * Listener waiting for idle state change.
     */
    private class IdleListener
        implements SystemActivityChangeListener
    {
        /**
         * Listens for activities and set corresponding statuses.
         *
         * @param event the <tt>NotificationActionTypeEvent</tt>, which is
         */
        public void activityChanged(SystemActivityEvent event)
        {
            logger.debug("Activity changed " + event.getEventID());
            switch(event.getEventID())
            {
                case SystemActivityEvent.EVENT_SCREEN_LOCKED:
                    lockStateChanged(true);
                    break;
                case SystemActivityEvent.EVENT_SCREEN_UNLOCKED:
                    lockStateChanged(false);
                    break;
                case SystemActivityEvent.EVENT_DISPLAY_SLEEP:
                case SystemActivityEvent.EVENT_SCREENSAVER_START:
                case SystemActivityEvent.EVENT_SYSTEM_IDLE:
                    changeGlobalAwayState(true);
                    break;
                case SystemActivityEvent.EVENT_DISPLAY_WAKE:
                case SystemActivityEvent.EVENT_SCREENSAVER_STOP:
                case SystemActivityEvent.EVENT_SYSTEM_IDLE_END:
                    changeGlobalAwayState(false);
                    break;
            }
        }
    }
}
