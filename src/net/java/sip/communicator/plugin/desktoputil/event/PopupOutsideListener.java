/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.event;

/*
 * The following code borrowed from David Bismut, davidou@mageos.com Intern,
 * SETLabs, Infosys Technologies Ltd. May 2004 - Jul 2004 Ecole des Mines de
 * Nantes, France
 */
import java.awt.event.*;
import java.util.*;

/**
 * @author Yana Stamcheva
 */
public interface PopupOutsideListener extends EventListener {
    void popupOutsideOperation(MouseEvent e);
}
