/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import java.beans.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.plugin.generalconfig.*;

/**
 * Preferences for the Status Update
 *
 * @author Thomas Hofer
 *
 */
public final class Preferences
{
    /**
     * Property indicating whether status change on away is enabled.
     */
    private static final String ENABLE
        = "net.java.sip.communicator.plugin.statusupdate.enable";

    /**
     * Property indicating the time in minutes to consider a pc in idle state.
     */
    private static final String TIMER
        = "net.java.sip.communicator.plugin.statusupdate.timer";

    /**
     * The default value to be displayed and to be considered
     * for {@link Preferences#TIMER}.
     */
    public static final int DEFAULT_TIMER = 15;

    /**
     * The minimum value that can be set for {@link Preferences#TIMER}.
     */
    public static final int MIN_TIMER = 1;

    /**
     * The maximum value that can be set for {@link Preferences#TIMER}.
     */
    public static final int MAX_TIMER = 180;

    /**
     * Whether change status on away is enabled.
     * @return whether change status on away is enabled.
     */
    static boolean isEnabled()
    {
        ConfigurationService cfg =
            GeneralConfigPluginActivator.getConfigurationService();
        return cfg.user().getBoolean(ENABLE, false);
    }

    /**
     * Returns the time in minutes to consider a pc in idle state.
     * @return  the time in minutes to consider a pc in idle state.
     */
    static int getTimer()
    {
        ConfigurationService cfg =
            GeneralConfigPluginActivator.getConfigurationService();
        return cfg.user().getInt(Preferences.TIMER, DEFAULT_TIMER);
    }

    /**
     * Save data in the configuration file
     * @param enabled is enabled
     * @param timer the time value to save
     */
    static void saveData(boolean enabled, String timer)
    {
        ConfigurationService cfg
            = GeneralConfigPluginActivator.getConfigurationService();

        cfg.user().setProperty(Preferences.ENABLE, Boolean.toString(enabled));
        cfg.user().setProperty(Preferences.TIMER, timer);
    }

    /**
     * Adds listener to detect property changes.
     * @param listener the listener to notify.
     */
    static void addEnableChangeListener(PropertyChangeListener listener)
    {
        // listens for changes in configuration enable/disable
        GeneralConfigPluginActivator
            .getConfigurationService().user()
                .addPropertyChangeListener(
                        ENABLE,
                        listener);
    }

    /**
     * Adds listener to detect timer property changes.
     * @param listener the listener to notify.
     */
    static void addTimerChangeListener(PropertyChangeListener listener)
    {
        // listens for changes in configuration enable/disable
        GeneralConfigPluginActivator
            .getConfigurationService().user()
                .addPropertyChangeListener(
                        TIMER,
                        listener);
    }
}
