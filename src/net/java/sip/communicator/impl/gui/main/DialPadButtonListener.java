// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

/**
 * Interface for listeners to buttons on a dial pad being pressed
 */
public interface DialPadButtonListener
{
    /**
     * Called when a dial pad button has been pressed
     *
     * @param name the name of the button that has been pressed
     */
    void dialButtonPressed(String name);
}
