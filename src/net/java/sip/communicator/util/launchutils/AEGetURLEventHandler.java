/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.launchutils;

/**
 * Registers as listener for kAEGetURL AppleScript events.
 * And will handle any url coming from the OS by passing it to LaunchArgHandler.
 *
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class AEGetURLEventHandler
{
    private LaunchArgHandler launchArgHandler;

    /**
     * The interface for the used callback.
     */
    public interface IAEGetURLListener
    {
        /**
         * Handle the URL event.
         *
         * @param url the URL
         */
        void handleAEGetURLEvent (String url);
    }

    AEGetURLEventHandler(LaunchArgHandler launchArgHandler)
    {
        this.launchArgHandler = launchArgHandler;
    }
}
