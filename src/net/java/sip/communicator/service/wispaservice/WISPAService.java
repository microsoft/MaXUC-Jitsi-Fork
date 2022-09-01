// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.wispaservice;

/**
 * Interface for communicating with the WISPA bundle outside of the plugin
 * folder, such as for sending unprompted notify events.
 */
public interface WISPAService
{
    /**
     * Send a notify event to the client, where the event requires no specific
     * knowledge of client or server state (e.g. start-up hook).
     *
     * @param namespace The namespace to send the event with
     * @param action The name of the event to send
     */
    void notify(WISPANamespace namespace, WISPAAction action);

    /**
     * Send a notify event to the client, with requisite information to use
     * when constructing the message (e.g. a contact whose information has
     * updated).
     *
     * @param namespace The namespace to send the event with
     * @param action The name of the event to send
     * @param data The object with data relevant to the event
     */
    void notify(WISPANamespace namespace, WISPAAction action, Object data);

    /**
     * Java listens to the connection with the Electron client, and if it's been
     * down for a while, tries to restart Electron. This turns that listening
     * on or off.
     *
     * @param allowRelaunch True if we want Electron to be able to be relaunched, false
     *                 if we don't
     */
    void allowElectronRelaunch(boolean allowRelaunch);

    /**
     * Check electron UI status and restarts it if needed.
     *
     * Check if electron UI is running (there must be at least one
     * socket connection open)
     *
     * Restarts the electron client when electron is down and the client
     * is not shutting down.
     */
    void checkAndRestartElectronUI();
}
