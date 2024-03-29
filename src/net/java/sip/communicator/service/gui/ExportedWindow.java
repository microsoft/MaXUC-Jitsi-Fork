/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.gui;

/**
 * A window that could be shown, hidden, resized, moved, etc. Meant to be used
 * from other services to show an application window, like for example a
 * "Configuration" or "Add contact" window.
 *
 * @author Yana Stamcheva
 */
public interface ExportedWindow
{
    /**
     * The add contact window identifier.
     */
    WindowID ADD_CONTACT_WINDOW
        = new WindowID("AddContactWindow");

    /**
     * The about window identifier.
     */
    WindowID ABOUT_WINDOW
        = new WindowID("AboutWindow");

    /**
     * The chat window identifier.
     */
    WindowID CHAT_WINDOW
        = new WindowID("ChatWindow");

    /**
     * The main (contact list) window identifier.
     */
    WindowID MAIN_WINDOW
        = new WindowID("MainWindow");

    /**
     * The call park window identifier.
     */
    WindowID CALL_PARK_WINDOW
        = new WindowID("CallParkWindow");

    /**
     * Returns the WindowID corresponding to this window. The window id should
     * be one of the defined in this class XXX_WINDOW constants.
     *
     * @return the WindowID corresponding to this window
     */
    WindowID getIdentifier();

    /**
     * Returns TRUE if the component is visible and FALSE otherwise.
     *
     * @return <code>true</code> if the component is visible and
     * <code>false</code> otherwise.
     */
    boolean isVisible();

    /**
     * Returns TRUE if this component is currently the focused component,
     * FALSE - otherwise.
     * @return TRUE if this component is currently the focused component,
     * FALSE - otherwise.
     */
    boolean isFocused();

    /**
     * Shows or hides this component.
     * @param isVisible indicates whether to set this window visible or hide it
     */
    void setVisible(boolean isVisible);

    /**
     * Brings the focus to this window.
     */
    void bringToFront();

    /**
     * Moves this window into the background
     */
    void bringToBack();

    /**
     * Resizes the window with the given width and height.
     *
     * @param width The new width.
     * @param height The new height.
     */
    void setSize(int width, int height);

    /**
     * Moves the window to the given coordinates.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    void setLocation(int x, int y);

    /**
     * Minimizes the window.
     */
    void minimize();

    /**
     * Maximizes the window.
     */
    void maximize();

    /**
     * The source of the window
     * @return the source of the window
     */
    Object getSource();

    /**
     * This method can be called to pass any params to the exported window. This
     * method will be automatically called by
     * {@link UIService#getExportedWindow(WindowID, Object[])} in order to set
     * the parameters passed.
     *
     * @param windowParams the parameters to pass.
     */
    void setParams(Object[] windowParams);

}
