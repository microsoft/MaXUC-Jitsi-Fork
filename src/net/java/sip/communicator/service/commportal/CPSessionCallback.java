// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * An interface used for requesting a CommPortal session ID
 */
public interface CPSessionCallback
{
    /**
     * Called when the CommPortal session ID has been retrieved
     *
     * @param commPortalUrl The URL of the CommPortal customisation that the
     *        session ID is valid for - e.g. "https://example/cust"
     * @param sessionId The session ID
     */
    void onSessionIdRetrieved(String commPortalUrl, String sessionId);
}
