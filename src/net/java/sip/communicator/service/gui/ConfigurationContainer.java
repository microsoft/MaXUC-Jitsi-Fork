/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.gui;

public interface ConfigurationContainer
{
    /**
     * Shows or hides this configuration container depending on the value of
     * parameter <code>visible</code>.
     *
     * @param visible if <code>true</code>, shows the main application window;
     *            otherwise, hides the main application window.
     */
    void setVisible(boolean visible);

    /**
     * Selects the given <tt>ConfigurationForm</tt> if it exists in this
     * container.
     *
     * @param configForm the <tt>ConfigurationForm</tt> to select
     */
    void setSelected(ConfigurationForm configForm);

    /**
     * Selects the <tt>ConfigurationForm</tt> with this title if it exists in
     * this container.
     *
     * @param title the title of the <tt>ConfigurationForm</tt> to select
     */
    void setSelected(String title);

    /**
     * Makes <tt>ConfigurationForm</tt> visible and brings the it to front,
     * setting the selected settings panel to <tt>tab</tt>.
     */
    void bringToFront(String tab);
}
