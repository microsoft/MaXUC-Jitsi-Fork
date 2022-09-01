// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * Defines a callback for being notified of changes to a particular service
 * indication.
 */
public interface CPDataRegistrationWithDataCallback extends CPDataRegistrationCallback
{
    /**
     * Called when the data has changed in some way
     *
     * @param data The new value for the data
     */
    void onDataChanged(String data);
}
