// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.browserpanel;

/**
 * The BrowserPanelDisplayer is an object whose sole purpose is to show or hide
 * a browser panel window.  It ensures that the window is only shown once if
 * set visible is called twice in a row.
 */
public interface BrowserPanelDisplayer
{
    /**
     * Shows or hides the browser panel window.  If the window does not exist
     * then it will be created then shown
     *
     * @param visible If true then the window will be made visible.  Otherwise
     *                it will be hidden
     */
    void setVisible(boolean visible);
}
