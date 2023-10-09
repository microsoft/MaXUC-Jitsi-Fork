// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.service.conference.ConferenceService;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * A component that creates menu items that allow the user to create or add
 * participants to a conference.
 */
public class CreateConferenceMenu
{
    private static final Logger logger = Logger.getLogger(CreateConferenceMenu.class);

    /**
     * The resource management service
     */
    private static final ResourceManagementService resources =
                                            DesktopUtilActivator.getResources();

    /**
     * The container used to retrieve one or more IM contacts to preselect to
     * join the conference and to determine the resource to use to get the text
     * for the menu items.
     */
    private final CreateConferenceMenuContainer createConferenceMenuContainer;

    /**
     * The resource used to get the text to display on the menu item that
     * directly sends a conference invite without opening the dialog to allow
     * the user to select other contacts to invite.
     */
    private final String directInviteResource;

    /**
     * The resource used to get the text to display on the menu item that
     * opens the dialog to allow the user to select other contacts to invite to
     * the conference.
     */
    private final String selectOthersInviteResource;

    /**
     * Creates an instance of <tt>CreateConferenceMenu</tt>.
     *
     * @param container The container used to retrieve one or more IM
     * contacts to preselect to join the conference and to determine the resource
     * to use for the menu items. If the container is null, this conference
     * menu is not associated with a source contact that has a MetaContact,
     * CallConference or ChatRoom associated with it (i.e. an SMS or call
     * history from a non-contact). If it is, we handle this by using the
     * default resources for the text and displaying a 'create new conference'
     * menu to allow the user to select contacts manually.
     */
    public CreateConferenceMenu(CreateConferenceMenuContainer container)
    {
        logger.debug(
            "Creating new CreateConferenceMenu - container = " + container);

        createConferenceMenuContainer = container;

        // Get the text for the sendConferenceInvite menu item from the container
        // if we have one, otherwise use the default text.
        if (createConferenceMenuContainer == null)
        {
            // As the container is null, we won't be able to preselect any
            // contacts, but we can still display a "create new conference"
            // window for the user to manually select contacts.
            logger.warn("CreateConferenceMenu created with null container - " +
                                                   "cannot preselect contacts");

            // The actual text that we use for the menu item will depend on
            // whether we're already in a conference.
            boolean conferenceCreated =
                DesktopUtilActivator.getConferenceService().isConferenceCreated();
            directInviteResource = conferenceCreated ?
                             "service.gui.conf.INVITE_NON_IM_IN_CONFERENCE" :
                             "service.gui.conf.INVITE_NON_IM_NOT_IN_CONFERENCE";
            selectOthersInviteResource = conferenceCreated ?
                         "service.gui.conf.INVITE_OTHERS_NON_IM_IN_CONFERENCE" :
                         "service.gui.conf.INVITE_OTHERS_NON_IM";
        }
        else
        {
            directInviteResource =
                createConferenceMenuContainer.getDirectInviteResource();
            selectOthersInviteResource =
                createConferenceMenuContainer.getSelectOthersInviteResource();
        }
    }

    /**
     * Sets up the actions to perform when the 'create conference' menu item
     * is selected.
     */
    protected void setupCreateConferenceItem(JMenuItem createConference)
    {
        createConference.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                logger.user("Selected option to open create conference dialog, " +
                                    "container = " +
                                    createConferenceMenuContainer);

                // Don't create the conference immediately so that the user can
                // choose to add additional participants, as well as the
                // contacts represented by the descriptor.
                if (createConferenceMenuContainer != null)
                {
                    createConferenceMenuContainer.createConference(false);
                }
                else
                {
                    // If there is no container we don't have any contacts to
                    // use to create the conference, so just display a "create
                    // new conference" contact selector window instead.
                    DesktopUtilActivator.getConferenceService().createOrAdd(true);
                }
            }
        });

        // Menu items are their own good enough accessibility descriptions.
    }

    /**
     * Sets up the actions to perform when the 'send conference invite' menu item
     * is selected.
     */
    protected void setupSendConferenceInviteItem(JMenuItem sendConferenceInvite)
    {
        sendConferenceInvite.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                logger.user("Selected option to send conferences invites immediately, " +
                            "container = " + createConferenceMenuContainer);

                // Create the conference immediately, as the user has selected
                // just to invite the contacts represented by the descriptor.
                if (createConferenceMenuContainer != null)
                {
                    createConferenceMenuContainer.createConference(true);
                }
                else
                {
                    // If there is no container we don't have any contacts to
                    // use to create the conference, so just display a "create
                    // new conference" contact selector window instead.
                    DesktopUtilActivator.getConferenceService().createOrAdd(false);
                }
            }
        });

        // Only enable the send conference invite button if the container can
        // be directly uplifted to a conference.
        sendConferenceInvite.setEnabled(createConferenceMenuContainer != null &&
            createConferenceMenuContainer.canBeUplifedToConference());

        // Menu items are their own good enough accessibility descriptions.
    }

    /**
     * Returns a JPopupMenu containing a 'send conference invite' SIPCommMenuItem
     * and a 'create conference' SIPCommMenuItem.
     *
     * @param owner The component that triggers this menu to be displayed.
     *
     * @return The JPopupMenu containing the SIPCommMenuItems
     */
    public JPopupMenu getJPopupMenu(Component owner)
    {
        logger.info("Getting JPopupMenu");
        JPopupMenu menu = new JPopupMenu();

        // Need a border on non-mac clients
        if (!OSUtils.IS_MAC)
        {
            Border outside = BorderFactory.createLineBorder(Color.DARK_GRAY);
            Border inside = BorderFactory.createLineBorder(Color.WHITE, 2);
            Border border = BorderFactory.createCompoundBorder(outside, inside);
            menu.setBorder(border);
        }

        SIPCommMenuItem sendConferenceInvite = new SIPCommMenuItem(
            resources.getI18NString(directInviteResource), (BufferedImageFuture) null);
        setupSendConferenceInviteItem(sendConferenceInvite);

        SIPCommMenuItem createConference = new SIPCommMenuItem(
            resources.getI18NString(selectOthersInviteResource), (BufferedImageFuture) null);
        setupCreateConferenceItem(createConference);

        menu.add(sendConferenceInvite);
        menu.add(createConference);

        menu.setInvoker(owner);

        Point locationOnScreen = owner.getLocationOnScreen();
        menu.setLocation(locationOnScreen.x, locationOnScreen.y + owner.getHeight());
        menu.setVisible(false);

        return menu;
    }

    /**
     * @return a 'send conference invite' JMenuItem
     */
    public JMenuItem getSendConferenceInviteJMenuItem()
    {
        logger.info("Getting 'send conference invite' JMenuItem");

        JMenuItem sendConferenceInvite = new ResMenuItem(directInviteResource);
        setupSendConferenceInviteItem(sendConferenceInvite);
        return sendConferenceInvite;
    }

    /**
     * @return a 'create conference' JMenuItem.
     */
    public JMenuItem getCreateConferenceJMenuItem()
    {
        logger.info("Getting 'create conference' JMenuItem");

        JMenuItem createConference = new ResMenuItem(selectOthersInviteResource);
        setupCreateConferenceItem(createConference);
        return createConference;
    }

    /**
     * @return true if there is a currently enabled conference service
     * implementation, false otherwise.
     */
    public static boolean isConferenceServiceEnabled()
    {
        ConferenceService mConferenceService =
            DesktopUtilActivator.getConferenceService();
        return (mConferenceService != null
            && mConferenceService.isFullServiceEnabled());
    }

    /**
     * @return true if there is a currently enabled conference service
     * implementation AND IM is also enabled.
     */
    public static boolean isConferenceInviteByImEnabled()
    {
        return isConferenceServiceEnabled() && ConfigurationUtils.isImEnabled();
    }

    /**
     * An interface to define a container used to retrieve one or more IM
     * contacts to preselect to join the conference and to determine the resource
     * to use to get the text for the 'send conference invite' item in the menu.
     */
    public interface CreateConferenceMenuContainer
    {
        /**
         * @return the resource to use to get the text to display on the menu
         * item that directly sends a conference invite without opening the
         * dialog to allow the user to select other contacts to invite.
         */
        String getDirectInviteResource();

        /**
         * @return the resource to use to get the text to display on the menu
         * item that opens the dialog to allow the user to select other
         * contacts to invite to the conference.
         */
        String getSelectOthersInviteResource();

        /**
         * Asks the conference service either to create a new conference or add
         * participants to a conference that is in progress.
         *
         * @param createImmediately If true, the conference service will only
         * invite the contacts associated with this container to the conference,
         * otherwise it will first display a dialog to allow the user to select
         * additional contacts.
         */
        void createConference(boolean createImmediately);

        /**
         * Returns true if this container can be uplifted directly to a
         * conference.
         *
         * @return true if this container can be uplifted directly to a
         * conference.
         */
        boolean canBeUplifedToConference();
    }
}
