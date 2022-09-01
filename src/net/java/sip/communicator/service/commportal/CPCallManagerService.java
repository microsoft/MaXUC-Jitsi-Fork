// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

import javax.swing.*;

/**
 * The CP Call Manager Service allows access to the CommPortal Call Manager UI.
 * In particular, it has a single method which returns the CallManager menu
 */
public interface CPCallManagerService
{
    /**
     * Returns the Call Manager menu, or null if no call manager menu is allowed
     *
     * @return the Call Manager menu
     */
    JMenu getCallManagerMenu();

    /**
     * Recreates the call manager menu.  Required if the component in which the
     * call manager menu is displayed changes.
     *
     * @return the new menu to use.
     */
    JMenu recreateCallManagerMenu();
}
