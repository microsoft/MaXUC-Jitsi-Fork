// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * Define a callback for getting the Class of Service
 */
public interface CPCosGetterCallback
{
    /**
     * Called when we have received the CoS
     */
    void onCosReceived(CPCos classOfService);
}
