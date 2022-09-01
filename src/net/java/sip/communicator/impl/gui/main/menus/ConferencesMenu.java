// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.jitsi.service.resources.ResourceManagementService;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.plugin.desktoputil.ResMenuItem;
import net.java.sip.communicator.plugin.desktoputil.SIPCommMenu;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * Conferences Menu which appears in the menu bar of the Accession main window.
 * Enables user to join, create and schedule meetings and to view recorded and
 * scheduled meetings.
 */
public class ConferencesMenu
    extends SIPCommMenu
    implements ActionListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>ConferencesMenu</tt> class and its
     * instances for logging output.
     */
    private static final Logger sLog = Logger.getLogger(ConferencesMenu.class);

    // Menu item names
    private static final String JOIN_CONFERENCE = "joinConference";
    private static final String SCHEDULE_CONFERENCE = "scheduleConference";
    private static final String MANAGE_WEBINARS = "manageWebinars";
    private static final String NEW_CONFERENCE = "newConference";
    private static final String VIEW_RECORDED_CONFERENCES = "viewRecordedConferences";
    private static final String VIEW_SCHEDULED_CONFERENCES = "viewScheduledConferences";

    private static final ResourceManagementService sResources = GuiActivator.getResources();
    private static final ConferenceService sConferenceService = GuiActivator.getConferenceService();

    /**
     * Scheduled conferences menu item.
     */
    private final JMenuItem mScheduledConferencesItem;

    /**
     * Scheduled conferences menu item.
     */
    private final JMenuItem mRecordedConferencesItem;

    /**
     * New conference menu item.
     */
    private final JMenuItem mNewConferenceItem;

    /**
     * Schedule conference menu item.
     */
    private final JMenuItem mScheduleConferenceItem;

    /**
     * Manage webinars menu item.
     */
    private final JMenuItem mManageWebinarsItem;

    /**
     * Join conference menu item.
     */
    private final JMenuItem mJoinConferenceItem;

    public ConferencesMenu()
    {
        setText(sResources.getI18NString("service.gui.conf.CONFERENCES"));
        setMnemonic(sResources.getI18nMnemonic("service.gui.conf.CONFERENCES"));

        mScheduledConferencesItem = new ResMenuItem("service.gui.conf.SCHEDULED_CONFERENCES");
        add(mScheduledConferencesItem);
        mScheduledConferencesItem.setName(VIEW_SCHEDULED_CONFERENCES);
        mScheduledConferencesItem.addActionListener(this);

        mRecordedConferencesItem = new ResMenuItem("service.gui.conf.RECORDED_CONFERENCES");
        add(mRecordedConferencesItem);
        mRecordedConferencesItem.setName(VIEW_RECORDED_CONFERENCES);
        mRecordedConferencesItem.addActionListener(this);

        addSeparator();

        mNewConferenceItem = new ResMenuItem("service.gui.conf.CREATE_NEW_CONFERENCE");
        add(mNewConferenceItem);
        mNewConferenceItem.setName(NEW_CONFERENCE);
        mNewConferenceItem.addActionListener(this);

        mScheduleConferenceItem = new ResMenuItem("service.gui.conf.SCHEDULE_CONFERENCE");
        add(mScheduleConferenceItem);
        mScheduleConferenceItem.setName(SCHEDULE_CONFERENCE);
        mScheduleConferenceItem.addActionListener(this);

        mManageWebinarsItem = new ResMenuItem("service.gui.conf.WEBINAR_SETTINGS");
        add(mManageWebinarsItem);
        mManageWebinarsItem.setName(MANAGE_WEBINARS);
        mManageWebinarsItem.addActionListener(this);
        // Initially set invisible until we verify webinars are enabled in
        // fireMenuSelected()
        mManageWebinarsItem.setVisible(false);

        addSeparator();

        mJoinConferenceItem = new ResMenuItem("service.gui.conf.JOIN_CONFERENCE");
        add(mJoinConferenceItem);
        mJoinConferenceItem.setName(JOIN_CONFERENCE);
        mJoinConferenceItem.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        sLog.user(itemName + " clicked in the Conferences menu");

        switch (itemName)
        {
            case VIEW_SCHEDULED_CONFERENCES:
                sConferenceService.showScheduledConferences();
                break;
            case VIEW_RECORDED_CONFERENCES:
                sConferenceService.showRecordedConferences();
                break;
            case NEW_CONFERENCE:
                sConferenceService.createOrAdd(false);
                break;
            case SCHEDULE_CONFERENCE:
                sConferenceService.showScheduler();
                break;
            case MANAGE_WEBINARS:
                sConferenceService.showWebinarManagement();
                break;
            case JOIN_CONFERENCE:
                sConferenceService.showJoinConferenceDialog();
                break;
        }
    }

    @Override
    public void fireMenuSelected()
    {
        sLog.info("Firing conferences menu.");
        // Update the text on the meeting menu item depending on whether
        // we're in a meeting.
        String createMeetingRes = sConferenceService.isConferenceCreated() ?
            "service.gui.conf.ADD_CONFERENCE_PARTICIPANTS" :
                "service.gui.conf.CREATE_NEW_CONFERENCE";
        mNewConferenceItem.setText(sResources.getI18NString(createMeetingRes));

        boolean createEnabled = sConferenceService.isFullServiceEnabled();
        boolean joinEnabled = sConferenceService.isJoinEnabled();

        mScheduledConferencesItem.setEnabled(createEnabled);
        mRecordedConferencesItem.setEnabled(createEnabled);
        mNewConferenceItem.setEnabled(createEnabled);
        mScheduleConferenceItem.setEnabled(createEnabled);
        mJoinConferenceItem.setEnabled(joinEnabled);

        // Only show the webinar management item if webinars are assigned.
        mManageWebinarsItem.setVisible(ConfigurationUtils.isWebinarsEnabled());
    }
}
