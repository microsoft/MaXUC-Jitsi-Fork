// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatRoom;
import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The Lost Contact Group Chat Panel is displayed at the bottom of the chat
 * window in place of the chat write panel when the client has lost contact to
 * the group chat represented by the chat panel.  It disables and greys out the
 * chat write panel and automatically closes and reopens the chat window when
 * the client regains contact with the group chat.
 */
public class LostContactGroupChatPanel
    extends TransparentPanel
    implements ServiceListener,
               RegistrationStateChangeListener,
               LocalUserChatRoomPresenceListener
{
    private static final long serialVersionUID = 0L;
    private static final Logger logger = Logger.getLogger(LostContactGroupChatPanel.class);

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
     * The name of the chat room that we have lost connection to.
     */
    private final String chatRoomName;

    /**
     * The IM provider
     */
    private ProtocolProviderService imProvider;

    /**
     * The multi-user chat operation set.
     */
    private OperationSetMultiUserChat opSetMuc;

    /**
     * Creates a new dormant group chat panel to be displayed at the bottom of
     * the chat panel
     *
     * @param chatPanel the chat panel that this panel is displayed in
     */
    public LostContactGroupChatPanel(ChatPanel chatPanel)
    {
        this.chatPanel = chatPanel;
        this.chatRoomName = chatPanel.getChatSession().getChatName();
        logger.debug("Creating new LostContactGroupChatPanel for " + logHasher(chatRoomName));

        // Add a listener so we can enable the button when we regain connection
        // to the server
        GuiActivator.bundleContext.addServiceListener(this);

        ProtocolProviderService imProvider = AccountUtils.getImProvider();
        if (imProvider != null)
        {
            handleJabberProviderAdded(imProvider);
        }
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
    public void serviceChanged(ServiceEvent event)
    {
        // If the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        // When the jabber provider comes online, we need to listen for the
        // account registering so we can dispose of the containing chat panel
        // and open a new one.
        Object service = GuiActivator.bundleContext.getService(event.getServiceReference());
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        ProtocolProviderService pps = (ProtocolProviderService) service;
        if (!pps.getProtocolName().equals(ProtocolNames.JABBER))
        {
            return;
        }

        int eventType = event.getType();

        if (eventType == ServiceEvent.REGISTERED)
        {
            handleJabberProviderAdded(pps);
        }
        else if (eventType == ServiceEvent.UNREGISTERING)
        {
            handleJabberProviderRemoved(pps);
        }
    }

    /**
     * Executed when the jabber provider is added.
     *
     * @param jabberProvider The jabber protocol provider service
     */
    private void handleJabberProviderAdded(ProtocolProviderService jabberProvider)
    {
        imProvider = jabberProvider;
        imProvider.addRegistrationStateChangeListener(this);

        if (imProvider.isRegistered())
        {
            handleJabberAccountRegistered();
        }
    }

    /**
     * Executed when the jabber provider is removed.
     *
     * @param jabberProvider The jabber protocol provider service
     */
    private void handleJabberProviderRemoved(ProtocolProviderService jabberProvider)
    {
        imProvider = null;
        jabberProvider.removeRegistrationStateChangeListener(this);

        if (opSetMuc != null)
        {
            opSetMuc.removePresenceListener(this);
            opSetMuc = null;
        }
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            handleJabberAccountRegistered();
        }
    }

    /**
     * Listens for the user rejoining this chat room when the jabber account
     * becomes registered.
     */
    private void handleJabberAccountRegistered()
    {
        logger.debug("Jabber account registered");
        opSetMuc = imProvider.getOperationSet(OperationSetMultiUserChat.class);

        if (opSetMuc != null)
        {
            try
            {
                // Listen for the user joining this chat room so we can dispose
                // this panel and open a new one.
                opSetMuc.addPresenceListener(this);
                ChatRoom chatRoom = opSetMuc.findRoom(chatRoomName);

                // If we already joined the chat room, we can replace this
                // panel immediately.
                if ((chatRoom != null) && chatRoom.isJoined())
                {
                    handleChatRoomJoined();
                }
            }
            catch (OperationFailedException |
                   OperationNotSupportedException e)
            {
                logger.warn("Failed to find chat room with id " + logHasher(chatRoomName), e);
            }
        }
    }

    /**
     * Disposes of the containing chat panel and starts a new one when we have
     * rejoined the chat room.
     */
    private void handleChatRoomJoined()
    {
        dispose();
        logger.debug(
            "Reopening window for chat room that lost contact: " + logHasher(chatRoomName));
        chatPanel.getChatContainer().getFrame().dispose();
        GuiActivator.getUIService().startGroupChat(chatRoomName, false, null);
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

        if (opSetMuc != null)
        {
            opSetMuc.removePresenceListener(this);
        }

        GuiActivator.bundleContext.removeServiceListener(this);
    }

    @Override
    public void localUserPresenceChanged(LocalUserChatRoomPresenceChangeEvent evt)
    {
        String evtChatRoomName = evt.getChatRoom().getIdentifier().toString();

        if (evtChatRoomName.equals(chatRoomName))
        {
            if (!evt.isLeavingEvent())
            {
                logger.debug(
                    "Received joined presence change event for chatRoom: " +
                    sanitiseChatRoom(evtChatRoomName));
                handleChatRoomJoined();
            }
        }
    }
}
