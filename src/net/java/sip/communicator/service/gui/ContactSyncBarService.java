// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.gui;

/**
 * A service that allows contact sources to show a synchronization bar at the
 * bottom of the main UI
 */
public interface ContactSyncBarService
{
    /**
     * Called when a contact event has occurred
     */
    void fireContactEvent();
}
