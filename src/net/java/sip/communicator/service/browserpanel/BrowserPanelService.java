// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.browserpanel;

/**
 * An interface for accessing and showing the native browser window
 */
public interface BrowserPanelService
{
    /**
     * Create a object which displays a web window.
     *
     * @param urlCreator an object which is used to create the URL of the window.
     *                  If this is a CommPortal URL this will happen once the
     *                  token has been validated
     * @param targetRes (Optional) resource ID to signify what this browser
     *                  panel is used for.
     * @return the created pop-up - note this could be null if the native
     *         browser is not supported
     */
    BrowserPanelDisplayer getBrowserPanelDisplayer(UrlCreator urlCreator,
                                                   String targetRes);

    BrowserPanelDisplayer getBrowserPanelDisplayer(UrlCreator urlCreator);

    /**
     * Interface defining an object which is used to create the URL of the
     * browser panel. If this is a CommPortal url this will happen once the
     * token has been validated
     */
    interface UrlCreator
    {
        /**
         * Create the URL of the page to load. If this is not a CommPortal
         * page this will simply return the URL we are trying to navigate to.
         *
         * @param url The CommPortal URL - e.g. "https://example/cust", if
         *        it is a CommPortal page. This can be null if is a not a
         *        CommPortal page.
         * @param redirectionAndTokenForUrl All the fields of the token received from
         *        the server needed to log into the CommPortal page as well as
         *        a redirect if there is an error in the login.
         * @return the URL of the page to display
         */
        String createUrl(String url, String redirectionAndTokenForUrl);
    }
}

