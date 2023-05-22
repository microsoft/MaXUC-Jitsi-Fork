/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.*;

/**
 * A class that extends the JButton providing transparency (and therefore no
 * white boxes surrounding buttons) to be used as the standard button with
 * correct look and feel for the OS running the program
 */
public class SIPCommBasicTextButton extends JButton
{
    private static final long serialVersionUID = 0L;

    public SIPCommBasicTextButton(String text)
    {
        setText(text);
        ScaleUtils.scaleFontAsDefault(this);
        setOpaque(false);
    }

    public SIPCommBasicTextButton()
    {
        ScaleUtils.scaleFontAsDefault(this);
        setOpaque(false);
    }
}