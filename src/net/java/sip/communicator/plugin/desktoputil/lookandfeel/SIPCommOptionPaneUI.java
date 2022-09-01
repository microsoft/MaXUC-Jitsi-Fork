/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.lookandfeel;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The SIPCommOptionPaneUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommOptionPaneUI extends BasicOptionPaneUI {

    /**
     * Creates a new SIPCommOptionPaneUI instance.
     */
   public static ComponentUI createUI(JComponent x)
   {
       return new SIPCommOptionPaneUI();
   }

   public void paint(Graphics g, JComponent c)
   {
       AntialiasingManager.activateAntialiasing(g);
       super.paint(g, c);
   }
}
