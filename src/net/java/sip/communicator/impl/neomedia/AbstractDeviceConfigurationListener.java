/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.neomedia;

import java.beans.*;

import net.java.sip.communicator.plugin.desktoputil.NotificationInfo;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPANotion;
import net.java.sip.communicator.service.wispaservice.WISPANotionType;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.*;

/**
 * An abstract listener to the click on the popup message concerning
 * device configuration changes.
 *
 * @author Vincent Lucas
 */
public abstract class AbstractDeviceConfigurationListener
    implements PropertyChangeListener
{
    /**
     *  The audio or video configuration form.
     */
    private final ConfigurationForm configurationForm;

    /**
     * Creates an abstract listener to the click on the popup message concerning
     * device configuration changes.
     *
     * @param configurationForm The audio or video configuration form.
     */
    public AbstractDeviceConfigurationListener(
            ConfigurationForm configurationForm)
    {
        this.configurationForm = configurationForm;
    }

    /**
     * Indicates that user has clicked on the popup message.
     *
     * @param ev the event triggered when user clicks on the systray popup
     * message
     */
    public void popupMessageClicked()
    {
        // Get the UI service
        UIService uiService
            = ServiceUtils.getService(
                    NeomediaActivator.getBundleContext(),
                    UIService.class);

        if(uiService == null)
        {
            return;
        }

        // Shows the audio configuration window.
        ConfigurationContainer configurationContainer
            = uiService.getConfigurationContainer();

        configurationContainer.setSelected(configurationForm);
        configurationContainer.setVisible(true);
    }

    /**
     * Shows a pop-up notification corresponding to a device configuration
     * change.
     *
     * @param title The title of the pop-up notification.
     * @param body  A body text describing the device modifications.
     */
    public void showPopUpNotification(String title, String body)
    {
        WISPAService wispaService = NeomediaActivator.getWispaService();

        if (wispaService == null || (title == null && body == null))
        {
            return;
        }

        String clickText = NeomediaActivator.getResources().getI18NString(
            "impl.media.configform.AUDIO_DEVICE_CONFIG_MANAGMENT_CLICK");

        WISPANotion notion = new WISPANotion(WISPANotionType.NOTIFICATION,
            new NotificationInfo(title, body + "\r\n\r\n" + clickText,
                this::popupMessageClicked));

        wispaService.notify(WISPANamespace.EVENTS, WISPAAction.NOTION, notion);
    }
}
