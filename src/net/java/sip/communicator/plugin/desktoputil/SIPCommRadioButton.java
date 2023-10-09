/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.*;

import org.jitsi.util.*;

/**
 * @author Ingo Bauersachs
 */
public class SIPCommRadioButton
    extends JRadioButton
{
    private static final long serialVersionUID = 0L;

    private static final boolean setContentAreaFilled = (OSUtils.IS_WINDOWS);

    public SIPCommRadioButton(String text)
    {
        super(text);

        init();
    }

    private void init()
    {
        if (setContentAreaFilled)
            setContentAreaFilled(false);
    }
}
