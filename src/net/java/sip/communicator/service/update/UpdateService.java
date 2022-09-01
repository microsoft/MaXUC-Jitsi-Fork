/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
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
     * Checks whether the user is on the latest version of the client, or whether there's a
     * more up-to-date version available for download.
     * @return true iff the client is the latest version available.
     * @throws IOException
     */
    boolean isLatestVersion() throws IOException;
}
