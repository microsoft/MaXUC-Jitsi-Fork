/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.shutdown;

import java.awt.desktop.QuitResponse;

import com.drew.lang.annotations.Nullable;

/**
 * Abstracts the shutdown-related procedures of the application so that they
 * can be used throughout various bundles.
 *
 * @author Linus Wallgren
 */
public interface ShutdownService
{
    /**
     * Invokes the UI action commonly associated with the "File &gt; Quit" menu
     * item which begins the application shutdown procedure.
     * <p>
     * The method avoids duplication since the "File &gt; Quit" functionality
     * may be invoked not only from the main application menu but also from the
     * systray, for example.
     * </p>
     */
    void beginShutdown();

    /**
     * Perform shutdown without waiting for the meeting client to close first.
     * This is designed to be called when the client is being killed by e.g. PC shutdown,
     * where we don't have the time to afford waiting.
     */
    void shutdownWithoutWaitingForMeeting();

    /**
     * Invokes the UI action commonly associated with the "File &gt; Quit" menu
     * item which begins the application shutdown procedure.
     * <p>
     * The method avoids duplication since the "File &gt; Quit" functionality
     * may be invoked not only from the main application menu but also from the
     * systray, for example.
     * </p>
     * @param logOut if true, log out the current user and restart showing an
     *               empty login dialog.  Otherwise, simply quit leaving the
     *               current user logged in.
     */
    void beginShutdown(final boolean logOut);

    /**
     * Invokes the UI action commonly associated with the "File &gt; Quit" menu
     * item which begins the application shutdown procedure.
     * <p>
     * The method avoids duplication since the "File &gt; Quit" functionality
     * may be invoked not only from the main application menu but also from the
     * systray, for example.
     * </p>
     * @param logOut if true, log out the current user and restart showing an
     *               empty login dialog.  Otherwise, simply quit leaving the
     *               current user logged in.
     * @param electronTriggered if true, this shut down was triggered by a
     *                          request from the Electron UI.
     */
    void beginShutdown(final boolean logOut, final boolean electronTriggered);

    /**
     * Invokes the UI action commonly associated with the "File &gt; Quit" menu
     * item which begins the application shutdown procedure.
     * <p>
     * The method avoids duplication since the "File &gt; Quit" functionality
     * may be invoked not only from the main application menu but also from the
     * systray, for example.
     * </p>
     * @param logOut if true, log out the current user and restart showing an
     *               empty login dialog.  Otherwise, simply quit leaving the
     *               current user logged in.
     * @param electronTriggered if true, this shut down was triggered by a
     *                          request from the Electron UI.
     * @param macQuitResponse will be null unless the request to quit came from
     *                        the Mac OS.  In that case, we will call back to the
     *                        macQuitResponse once this method has finished executing.
     */
    void beginShutdown(final boolean logOut,
                       final boolean electronTriggered,
                       @Nullable final QuitResponse macQuitResponse,
                       boolean waitForMeetingToQuit);
}
