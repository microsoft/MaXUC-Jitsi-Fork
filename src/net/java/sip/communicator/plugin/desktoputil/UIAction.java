/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.*;

/**
 * A wrapper around AbstractAction, so we can differ our Actions.
 * Used when adding actions to the action map,
 * and actions will be triggered by keystrokes from the inputMap of the RootPane.
 *
 * @author Damian Minkov
 */
public abstract class UIAction
    extends AbstractAction
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;
}
