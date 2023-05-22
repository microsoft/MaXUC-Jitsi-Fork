/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import javax.swing.tree.*;

public interface ContactListTreeCellRenderer extends TreeCellRenderer
{
    /**
     * Loads the UI for this tree cell renderer
     */
    void loadSkin();

    /**
     * Sets which contact element the mouse is currently over. Required for
     * passing mouse events on from the Tree Contact List
     *
     * @param element The element that is currently under the mouse
     */
    void setMouseOverContact(Object element);
}
