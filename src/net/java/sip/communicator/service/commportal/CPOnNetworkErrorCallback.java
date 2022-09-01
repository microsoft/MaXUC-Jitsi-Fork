// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * Defines a callback to handle network errors
 */
public interface CPOnNetworkErrorCallback
{
    /**
     * Called when there has been an error due to the network
     *
     * @param error the error that we hit
     */
    void onNetworkError(CPNetworkError error);
}
