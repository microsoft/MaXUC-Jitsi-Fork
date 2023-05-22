/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.update;

import java.io.IOException;

/**
 * Checking for software updates service.
 *
 * @author Yana Stamcheva
 */
public interface UpdateService
{
    /**
     * Checks for updates.
     *
     * If automatic updates are disabled, this method won't check for updates,
     * unless this is an update check done manually by the user.
     *
     * @param isUserTriggered <tt>true</tt> if the user is explicitly asking to
     * check for notifications, in which case they are to be notified if they
     * have the newest version already; and also if we should notify them when
     * they perform this action while disconnected; otherwise, <tt>false</tt>
     */
    void checkForUpdates(boolean isUserTriggered);

    /**
     * Forces the client to update to the most recent version by showing the
     * "new version available" pop-up. If this is cancelled then the application
     * will exit
     */
    void forceUpdate();

    /**
     * Update to a later version if one's available, without showing the user a pop-up asking them.
     */
    public void updateIfAvailable();

    /**
     * Determines whether we are currently running the latest version.
     *
     * @return <tt>true</tt> if we are currently running the latest version;
     * otherwise, <tt>false</tt>
     * @throws IOException in case we could not connect to the update server
     */
    boolean isLatestVersion() throws IOException;

    /**
     * Forces the application to quit using the shutdown service to ensure a
     * clean and consistent shutdown behaviour.  Called when a user cancels a
     * mandatory update on Windows.
     */
    public void forceQuitApplication();

    /**
     * Cancels the current update download.
     */
    public void cancelUpdateDownload();
}
