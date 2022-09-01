// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * Defines a callback for being notified of changes to a particular service
 * indication.
 */
public interface CPDataRegistrationWithoutDataCallback extends CPDataRegistrationCallback
{
    /**
     * Called when the data has changed in some way
     */
    void onDataChanged();
}
