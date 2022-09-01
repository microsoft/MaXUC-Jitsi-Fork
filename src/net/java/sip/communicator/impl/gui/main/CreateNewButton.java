/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.border.Border;

import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.main.chat.AddChatParticipantsDialog;
import net.java.sip.communicator.plugin.desktoputil.CreateConferenceMenu;
import net.java.sip.communicator.plugin.desktoputil.GroupContactMenuUtils;
import net.java.sip.communicator.plugin.desktoputil.SIPCommButton;
import net.java.sip.communicator.plugin.desktoputil.SIPCommMenuItem;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.managecontact.ManageContactWindow;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.AccountUtils;

/**
 * The 'create new' button in the contact list.
 */
public class CreateNewButton extends SIPCommButton implements ActionListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>CreateNewButton</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(CreateNewButton.class);

    /**
     * The image shown when not rolled over
     */
    static final BufferedImageFuture BG_IMAGE =
        GuiActivator.getImageLoaderService().getImage(
                             ImageLoaderService.CONTACT_LIST_CREATE_NEW_BUTTON);

    /**
     * The image shown when rolled over
     */
    private static final BufferedImageFuture ROLLOVER_IMAGE =
        GuiActivator.getImageLoaderService().getImage(
                    ImageLoaderService.CONTACT_LIST_CREATE_NEW_BUTTON_ROLLOVER);

    /**
     * The image shown when pressed
     */
    private static final BufferedImageFuture PRESSED_IMAGE =
        GuiActivator.getImageLoaderService().getImage(
                     ImageLoaderService.CONTACT_LIST_CREATE_NEW_BUTTON_PRESSED);

    /**
     * The resource management service
     */
    private static final ResourceManagementService resources =
                                                    GuiActivator.getResources();

    /**
     * The menu that opens when this button is pressed
     */
    private final JPopupMenu menu = new JPopupMenu();

    /**
     * The 'add contact' item in the menu that opens when this button is
     * pressed
     */
    private final SIPCommMenuItem newContact = new SIPCommMenuItem(
        resources.getI18NString("service.gui.ADD_NEW_CONTACT"), (BufferedImageFuture) null);

    /**
     * The 'new group chat' item in the menu that opens when this button is
     * pressed
     */
    private final SIPCommMenuItem newGroupChat = new SIPCommMenuItem(
        resources.getI18NString("service.gui.chat.CREATE_NEW_GROUP"), (BufferedImageFuture) null);

    /**
     * the 'new group contact' item in the menu that opens when this button is
     * pressed
     */
    private final JMenuItem newGroupContact =
                       GroupContactMenuUtils.createNewGroupContactSipCommMenu();

    /**
     * The separator above the 'create' and 'schedule a conference' items
     */
    private final JSeparator conferenceSeparator =
        SIPCommMenuItem.createSeparator();

    /**
     * The separator above the 'new group chat' item
     */
    private final JSeparator groupChatSeparator =
        SIPCommMenuItem.createSeparator();

    /**
     * The 'create a conference' item in the menu that opens when this button
     * is pressed
     */
    private final SIPCommMenuItem newConference = new SIPCommMenuItem(
        resources.getI18NString("service.gui.conf.CREATE_NEW_CONFERENCE"), (BufferedImageFuture) null);

    /**
     * The 'schedule a conference' item in the menu that opens when this button
     * is pressed
     */
    private final SIPCommMenuItem scheduleConference = new SIPCommMenuItem(
        resources.getI18NString("service.gui.conf.SCHEDULE_CONFERENCE"), (BufferedImageFuture) null);

    /**
     * The 'manage webinars' item in the menu that opens when this button
     * is pressed
     */
    private final SIPCommMenuItem manageWebinars = new SIPCommMenuItem(
        resources.getI18NString("service.gui.conf.WEBINAR_SETTINGS"), (BufferedImageFuture) null);

    /**
     * Creates an instance of <tt>CreateNewButton</tt>.
     */
    public CreateNewButton()
    {
        super(BG_IMAGE,
              ROLLOVER_IMAGE,
              PRESSED_IMAGE,
              null,
              null,
              null);

        logger.debug("Creating new CreateNewButton");

        setImage(BG_IMAGE);
        setRolloverImage(ROLLOVER_IMAGE);

        Image bgImage = getBackgroundImage().resolve();

        this.setPreferredSize(new Dimension(bgImage.getWidth(this),
                                            bgImage.getHeight(this)));

        setBorder(BorderFactory.createEmptyBorder());

        // Need a border on non-mac clients
        if (!OSUtils.IS_MAC)
        {
            Border outside = BorderFactory.createLineBorder(Color.DARK_GRAY);
            Border inside = BorderFactory.createLineBorder(Color.WHITE, 2);
            Border border = BorderFactory.createCompoundBorder(outside, inside);
            menu.setBorder(border);
        }

        newContact.addActionListener(event ->
        {
            logger.user("'Add Contact' selected");
            ManageContactWindow addContactWindow =
                GuiActivator.getAddContactWindow(null);

            if (addContactWindow != null)
            {
                addContactWindow.setVisible(true);
            }
            else
            {
                logger.warn("Failed to get add contact window");
            }
        });

        newGroupChat.addActionListener(event ->
        {
            logger.user("'New Group Chat' selected");
            new AddChatParticipantsDialog(
                resources.getI18NString("service.gui.chat.CREATE_NEW_GROUP"),
                resources.getI18NString("service.gui.CREATE_GROUP_CHAT"),
                null,
                true,
                null).setVisible(true);
        });

        newConference.addActionListener(event ->
        {
            logger.user("'Create a conference' selected");
            GuiActivator.getConferenceService().createOrAdd(false);
        });

        scheduleConference.addActionListener(event ->
        {
            logger.user("'Schedule a conference' selected");
            GuiActivator.getConferenceService().showScheduler();
        });

        manageWebinars.addActionListener(event ->
        {
            logger.user("'Manage Webinars' selected");
            GuiActivator.getConferenceService().showWebinarManagement();
        });

        // Initially set manage webinars button invisible to avoid it being
        // seen by users without webinars assigned.
        manageWebinars.setVisible(false);

        menu.add(newContact);
        menu.add(newGroupContact);
        menu.add(groupChatSeparator);
        menu.add(newGroupChat);
        menu.add(conferenceSeparator);
        menu.add(newConference);
        menu.add(scheduleConference);
        menu.add(manageWebinars);

        menu.setInvoker(CreateNewButton.this);

        addActionListener(this);

        // Add descriptions for screen readers
        AccessibilityUtils.setNameAndDescription(this,
            resources.getI18NString("service.gui.CREATE_NEW"),
            resources.getI18NString("service.gui.CREATE_NEW_DESCRIPTION"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        logger.user("'Create New' menu selected");

        boolean conferenceServiceEnabled =
                                 CreateConferenceMenu.isConferenceServiceEnabled();
        if (conferenceServiceEnabled)
        {
            // Update the text on the meeting menu item depending on whether
            // we're in a meeting.
            String createMeetingResource =
                GuiActivator.getConferenceService().isConferenceStarted() ?
                                "service.gui.conf.ADD_CONFERENCE_PARTICIPANTS" :
                                "service.gui.conf.CREATE_NEW_CONFERENCE";
            newConference.setText(resources.getI18NString(createMeetingResource));
        }

        conferenceSeparator.setVisible(conferenceServiceEnabled);
        newConference.setVisible(conferenceServiceEnabled);
        scheduleConference.setVisible(conferenceServiceEnabled);

        // Only set manageWebinars visible if the user has webinars assigned.
        manageWebinars.setVisible(ConfigurationUtils.isWebinarsEnabled());

        boolean mucEnabled = ConfigurationUtils.isMultiUserChatEnabled();

        newGroupChat.setVisible(mucEnabled);
        groupChatSeparator.setVisible(mucEnabled);

        logger.debug("MUC is " + (mucEnabled ?
                                         "enabled - show group chat option " :
                                         "disabled - hide group chat option"));

        // If we don't have any registered chat providers, we need to
        // disable the group chat and conference options.
        boolean chatProvidersExist = AccountUtils.isImProviderRegistered();

        logger.debug("Enabling/disabling group chat options based on " +
                     "existing chat providers = " + chatProvidersExist);
        newGroupChat.setEnabled(chatProvidersExist);

        // New group contact should only be visible if group contacts are supported
        newGroupContact.setVisible(ConfigurationUtils.groupContactsSupported());
        GroupContactMenuUtils.setMenuItemEnabled(newGroupContact);

        Point locationOnScreen = CreateNewButton.this.getLocationOnScreen();
        menu.setLocation(locationOnScreen.x, locationOnScreen.y + getHeight());
        menu.setVisible(true);
    }
}
