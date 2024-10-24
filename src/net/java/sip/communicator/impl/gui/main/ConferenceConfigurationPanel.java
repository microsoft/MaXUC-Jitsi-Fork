// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.conference.ConferenceService;

/**
 * A configuration panel displayed in the main options window that allows the
 * user to configure settings relating to an external conferencing application.
 */
public class ConferenceConfigurationPanel extends ConfigurationPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private ConfigurationPanel mConferenceConfigPanel;

   /**
     * Creates the conference configuration panel.
     */
    public ConferenceConfigurationPanel()
    {
        super(new BorderLayout());
        // check whether this conference service is the teams impl, do not
        // create the config form if it is
        ConferenceService conferenceService = GuiActivator.getConferenceService();
        if (conferenceService != null && conferenceService.isLegacyImplementation())
        {
            mConferenceConfigPanel = conferenceService.createConfigPanel();
            add(mConferenceConfigPanel, BorderLayout.CENTER);
        }
    }

    @Override
    public void onRefresh()
    {
        if (mConferenceConfigPanel != null)
        {
            mConferenceConfigPanel.onRefresh();
        }
    }

    @Override
    public void onDeactivated()
    {
        if (mConferenceConfigPanel != null)
        {
            mConferenceConfigPanel.onDeactivated();
        }
    }
}
