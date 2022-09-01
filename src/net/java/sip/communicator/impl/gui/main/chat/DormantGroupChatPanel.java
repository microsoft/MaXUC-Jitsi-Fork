// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.ChatRoomWrapper;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The Dormant Group Chat Panel is displayed at the bottom of the chat window
 * in place of the chat write panel. It explains to the user that they have
 * previously left this chat room and offers a button to restart a group chat.
 */
public class DormantGroupChatPanel
    extends TransparentPanel
    implements ActionListener, ServiceListener, RegistrationStateChangeListener
{
    private static final long serialVersionUID = 0L;
    private static final Logger logger = Logger.getLogger(DormantGroupChatPanel.class);

    /**
     * The background image for this panel
     */
    private static final BufferedImageFuture BACKGROUND_IMAGE =
        GuiActivator.getImageLoaderService().getImage(
                              ImageLoaderService.DORMANT_GROUP_CHAT_BACKGROUND);

    /**
     * The chat panel containing this panel
     */
    private final ChatPanel chatPanel;

    /**
     * The button that allows the user to create a new group chat from this
     * panel.
     */
    private final SIPCommSnakeButton createButton = new SIPCommSnakeButton(
        GuiActivator.getResources().getI18NString("service.gui.chat.CREATE_NEW_GROUP"),
        "service.gui.button.park");

    /**
     * The set of participants that have ever participated in this group chat
     */
    private Set<String> participants = new HashSet<>();

    /**
     * The IM provider
     */
    private ProtocolProviderService imProvider;

    /**
     * The resource management service
     */
    private static final ResourceManagementService resources = GuiActivator.getResources();

    /**
     * Creates a new dormant group chat panel to be displayed at the bottom of
     * the chat panel
     *
     * @param chatPanel the chat panel that this panel is displayed in
     */
    public DormantGroupChatPanel(ChatPanel chatPanel)
    {
        this.chatPanel = chatPanel;

        init();

        // Add a listener so we can enable/disable the 'create' button based on
        // whether we are connected to the server.
        GuiActivator.bundleContext.addServiceListener(this);

        ProtocolProviderService imProvider = AccountUtils.getImProvider();
        if (imProvider != null)
        {
            handleJabberProviderAdded(imProvider);
        }
    }

    /**
     * Initializes this panel by setting up the UI
     */
    private void init()
    {
        setLayout(new GridLayout(2, 1, 0, 0));
        // Create the label
        JLabel infoLabel = new JLabel(resources.getI18NString(
                                        "service.gui.chat.DORMANT_CHAT_LABEL"));
        ScaleUtils.scaleFontAsDefault(infoLabel);
        infoLabel.setOpaque(false);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(infoLabel);

        // Initialise the button to disabled - we'll enable it once we know
        // jabber is online.
        createButton.setEnabled(false);
        createButton.addActionListener(this);

        // The button must be put in a panel to center align it
        TransparentPanel buttonPanel = new TransparentPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setAlignmentY(CENTER_ALIGNMENT);
        buttonPanel.add(createButton);
        ((FlowLayout) buttonPanel.getLayout()).setVgap(0);

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        add(buttonPanel);

        initParticipants();
    }

    /**
     * Gets all participants that have ever participated in this chat room
     */
    private void initParticipants()
    {
        ChatRoomWrapper chatRoom = (ChatRoomWrapper) chatPanel.getChatSession().getDescriptor();
        participants = GuiActivator.getMessageHistoryService().
                                  getRoomParticipants(chatRoom.getChatRoomID());

        // Every participant requires a UI element in the main tool bar. Create
        // these now
        ((ChatWindow)chatPanel.getChatContainer()).getChatToolbar().initializeParticipants(participants);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Image backgroundImage = BACKGROUND_IMAGE.resolve();

        // Tile the background image so the background is always painted
        // correctly regardless of size
        int imageWidth = backgroundImage.getWidth(this);
        int imageHeight = backgroundImage.getHeight(this);
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        for (int x = 0 ; x < panelWidth ; x=x + imageWidth)
        {
            for (int y = 0 ; y < panelHeight ; y=y + imageHeight)
            {
                g.drawImage(backgroundImage, x, y, null);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        logger.debug("Opening add chat participant dialog for dormant chat");
        chatPanel.getChatContainer().getFrame().dispose();

        // Pass in null for the chat panel, as our chat panel will be broken if
        // we have lost contact with the chat server since we opened the panel.
        new AddChatParticipantsDialog(
            resources.getI18NString("service.gui.chat.CREATE_NEW_GROUP"),
            resources.getI18NString("service.gui.CREATE_GROUP_CHAT"),
            null,
            true,
            null).setVisible(true);
    }

    @Override
    public void serviceChanged(ServiceEvent event)
    {
        // When the jabber provider comes online, we need to listen for the
        // account registering so we can enable the 'create' button.
        ServiceReference<?> serviceRef = event.getServiceReference();

        // If the event is caused by a bundle being stopped, we don't care.
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            return;

        Object service = GuiActivator.bundleContext.getService(serviceRef);

        if (!(service instanceof ProtocolProviderService))
            return;

        ProtocolProviderService pps = (ProtocolProviderService) service;
        if (!pps.getProtocolName().equals(ProtocolNames.JABBER))
            return;

        int eventType = event.getType();

        if (eventType == ServiceEvent.REGISTERED)
        {
            handleJabberProviderAdded(pps);
        }
        else if (eventType == ServiceEvent.UNREGISTERING)
        {
            handleJabberProviderRemoved();
        }
    }

    /**
     * Executed when the jabber provider is added.
     *
     * @param jabberProvider The jabber protocol provider service
     */
    private void handleJabberProviderAdded(ProtocolProviderService jabberProvider)
    {
        logger.info("Jabber provider added");
        imProvider = jabberProvider;
        imProvider.addRegistrationStateChangeListener(this);

        if (imProvider.isRegistered())
        {
            logger.info("Jabber provider already registered");

            // If the jabber account is already registered, enable the 'create'
            // button.  If not, there's nothing to do as the button will
            // already be disabled.
            handleJabberAccountRegistrationChange(true);
        }
    }

    /**
     * Executed when the jabber provider is removed.
     */
    private void handleJabberProviderRemoved()
    {
        imProvider.removeRegistrationStateChangeListener(this);
        imProvider = null;
        handleJabberAccountRegistrationChange(false);
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            handleJabberAccountRegistrationChange(true);
        }
        else if (evt.getNewState().equals(RegistrationState.UNREGISTERED) ||
                 evt.getNewState().equals(RegistrationState.CONNECTION_FAILED))
        {
            handleJabberAccountRegistrationChange(false);
        }
    }

    /**
     * Enables/disables the 'create' button based on whether the jabber account
     * is registered.
     *
     * @param registered true if the jabber account is registered.
     */
    private void handleJabberAccountRegistrationChange(boolean registered)
    {
        logger.debug("Setting 'create' button to enabled = " + registered);
        createButton.setEnabled(registered);
    }

    /**
     * Removes listeners when this panel is no longer in use.
     */
    public void dispose()
    {
        if (imProvider != null)
        {
            imProvider.removeRegistrationStateChangeListener(this);
        }

        GuiActivator.bundleContext.removeServiceListener(this);
    }
}
