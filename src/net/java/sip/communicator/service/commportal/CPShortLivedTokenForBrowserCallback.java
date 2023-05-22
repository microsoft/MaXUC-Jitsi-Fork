// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * An interface used for dealing with a short-lived token
 * when obtained for use in the browser.
 */
public interface CPShortLivedTokenForBrowserCallback {

    /**
     * Called when the token has been obtained from CommPortal to be used in the browser.
     *
     * @param commPortalUrl The URL of the CommPortal customisation that the
     *       token is valid for - e.g. "https://example/cust"
     * @param tokenAsString A conversion of all the short-lived token fields
     *       that is in the format needed for CommPortal login.
    */
    void onShortLivedTokenReceivedForBrowser(String commPortalUrl, String tokenAsString);
}
