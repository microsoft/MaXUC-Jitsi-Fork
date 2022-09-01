/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent.macosx;

import java.awt.Desktop;
import java.awt.desktop.AppReopenedEvent;
import java.awt.desktop.AppReopenedListener;

import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.service.gui.*;

/**
 * MacOSX specific dock icon, which will add a dock icon listener in order to
 * show the application each time user clicks on the dock icon.
 *
 * @author Yana Stamcheva
 */
public class MacOSXDockIcon
{
    /**
     * Adds a dock icon listener in order to show the application each time user
     * clicks on the dock icon.
     */
    public static void addDockIconListener()
    {
        Desktop desktop = Desktop.getDesktop();
        if (desktop != null)
        {
            desktop.addAppEventListener(new AppReopenedListener()
            {
                public void appReopened(AppReopenedEvent appReOpenedEvent)
                {
                    UIService uiService = OsDependentActivator.getUIService();

                    if (uiService != null && !uiService.isVisible())
                        uiService.setVisible(true);
                }
            });
        }
    }
}
