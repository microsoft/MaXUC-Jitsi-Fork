/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.ContactLogger;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChatTransportSelectorBox</tt> represents the send via menu in the
 * chat window. The menu contains all protocol specific transports. In the case
 * of meta contact these would be all contacts for the currently selected meta
 * contact chat.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ChatTransportSelectorBox
    extends SIPCommMenuBar
    implements  ActionListener,
                Skinnable
{
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    private static final Logger logger
        = Logger.getLogger(ChatTransportSelectorBox.class);

    private static final long serialVersionUID = 0L;

    private final Map<ChatTransport, JCheckBoxMenuItem> transportMenuItems =
            new Hashtable<>();

    private final SIPCommMenu menu = new SelectorMenu();

    private final ChatSession chatSession;

    private final ChatPanel chatPanel;

    /**
     * Creates an instance of <tt>ChatTransportSelectorBox</tt>.
     *
     * @param chatPanel the chat panel
     * @param chatSession the corresponding chat session
     * @param selectedChatTransport the chat transport to select by default
     */
    public ChatTransportSelectorBox(ChatPanel chatPanel,
                                    ChatSession chatSession,
                                    ChatTransport selectedChatTransport)
    {
        this.chatPanel = chatPanel;
        this.chatSession = chatSession;

        setPreferredSize(new ScaledDimension(44, 28));
        setMaximumSize(new ScaledDimension(44, 28));
        setMinimumSize(new ScaledDimension(44, 28));

        this.menu.setPreferredSize(new ScaledDimension(44, 44));
        this.menu.setMaximumSize(new ScaledDimension(44, 44));

        this.add(menu);

        this.setBorder(null);
        this.menu.setOpaque(false);
        this.setOpaque(false);

        // as a default disable the menu, it will be enabled as soon as we add
        // a valid menu item
        this.menu.setEnabled(false);

        Iterator<ChatTransport> chatTransports
            = chatSession.getChatTransports();

        while (chatTransports.hasNext())
            this.addChatTransport(chatTransports.next());

        if (this.menu.getItemCount() > 0)
        {
            if (selectedChatTransport != null
                && (selectedChatTransport.allowsInstantMessage()
                || selectedChatTransport.allowsSmsMessage()))
            {
                this.setSelected(selectedChatTransport);
            }
            else
            {
                this.setSelected(menu.getItem(0));
            }
        }
    }

    /**
     * Sets the menu to enabled or disabled. The menu is enabled, as soon as it
     * contains one or more items. If it is empty, it is disabled.
     */
    protected void updateEnableStatus()
    {
        ProtocolProviderService imProvider = AccountUtils.getImProvider();
        boolean imRegistered = imProvider != null && imProvider.isRegistered();
        int itemCount = menu.getItemCount();
        boolean isEnabled = (imRegistered && itemCount > 0);

        logger.debug("Updating enabled status imProvider registered? " +
                                   imRegistered + ", itemCount = " + itemCount);
        menu.setEnabled(isEnabled);
    }

    /**
     * Adds the given chat transport to the "send via" menu.
     * Only add those that support IM.
     *
     * @param chatTransport The chat transport to add.
     */
    public void addChatTransport(ChatTransport chatTransport)
    {
        if (chatTransport.allowsInstantMessage()
            || chatTransport.allowsSmsMessage())
        {
            BufferedImageFuture img = createTransportSelectorImage(chatTransport);

            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(
                            "<html><font size=\""
                            + ScaleUtils.scaleInt(3)
                            + "\">"
                            + "<b>" + chatTransport.getName() + "</b> "
                            + "</font></html>");

            img.getImageIcon().addToMenuItem(menuItem);
            menuItem.addActionListener(this);

            // We only add a new IM transport to the UI if there isn't already
            // one, as we don't want the user to be able to choose between
            // different IM resources.
            boolean addTransportToUI = true;

            synchronized (transportMenuItems)
            {
                if (chatTransport instanceof MetaContactChatTransport)
                {
                    Set<ChatTransport> exsitingChatTransports = transportMenuItems.keySet();

                    for (ChatTransport existingChatTransport : exsitingChatTransports)
                    {
                        if (existingChatTransport instanceof MetaContactChatTransport)
                        {
                            addTransportToUI = false;
                        }
                    }
                }

                this.transportMenuItems.put(chatTransport, menuItem);
            }

            if (addTransportToUI)
            {
                this.menu.add(menuItem);

                updateEnableStatus();
            }
        }
    }

    /**
     * Removes the given chat transport from the "send via" menu. This method is
     * used to update the "send via" menu when a protocol contact is moved or
     * removed from the contact list.
     *
     * @param chatTransport the chat transport to be removed
     */
    public void removeChatTransport(ChatTransport chatTransport)
    {
        // If we are currently in a group chat then don't do anything. We should
        // never change the chat transport of a conference chat session.
        if (chatPanel.getChatSession() instanceof ConferenceChatSession)
        {
            return;
        }

        synchronized (transportMenuItems)
        {
            if ((transportMenuItems != null) &&
                (!transportMenuItems.containsKey(chatTransport)))
                return;

            this.menu.remove(transportMenuItems.get(chatTransport));
            this.transportMenuItems.remove(chatTransport);

            updateEnableStatus();

            // If the chat transport we've removed was an IM transport
            // (MetaContactChatTransport), update the menu to be sure that there
            // is still an IM transport in the menu, if there are any still in
            // the list.  If there are no IM transports left, select the most
            // recently used SMS transport instead.
            if (chatTransport instanceof MetaContactChatTransport)
            {
                Set<ChatTransport> existingChatTransports = transportMenuItems.keySet();
                boolean metaTransportFound = false;

                for (ChatTransport existingChatTransport : existingChatTransports)
                {
                    if (existingChatTransport instanceof MetaContactChatTransport)
                    {
                        contactLogger.debug(
                            "Updating chat transport selector to MetaContactChatTransport " +
                                                            chatTransport.getResourceName());
                        JCheckBoxMenuItem menuItem =
                                  transportMenuItems.get(existingChatTransport);
                        this.menu.add(menuItem);
                        updateEnableStatus();
                        metaTransportFound = true;
                        break;
                    }
                }

                if (!metaTransportFound)
                {
                    contactLogger.debug(
                        "No IM transport found - set most recent transport instead");
                    chatPanel.getChatSession().selectMostRecentChatTransport();
                }
            }
        }
    }

    /**
     * The listener of the chat transport selector box.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();
        logger.user("Chat message type changed");

        synchronized (transportMenuItems)
        {
            for (Map.Entry<ChatTransport, JCheckBoxMenuItem> transportMenuItem
                    : transportMenuItems.entrySet())
            {
                ChatTransport chatTransport = transportMenuItem.getKey();
                if (transportMenuItem.getValue().equals(menuItem))
                {
                    this.setSelected(menuItem,
                                     chatTransport,
                                     new ImageIconAvailable((ImageIcon) menuItem.getIcon()),
                                     true);

                    return;
                }
            }

            logger.debug("Could not find contact for menu item "
                      + menuItem.getText() + ". contactsTable("
                      + transportMenuItems.size()+") is : "
                      + transportMenuItems);
        }
    }

    /**
     * Obtains the status icon for the given chat transport and
     * adds to it the account index information.
     *
     * @param chatTransport The chat transport for which to create the image.
     * @return The indexed status image.
     */
    public BufferedImageFuture createTransportSelectorImage(ChatTransport chatTransport)
    {
        ImageID iconResource = (chatTransport instanceof SMSChatTransport) ?
            ImageLoaderService.SMS_MESSAGE : ImageLoaderService.CHAT_MESSAGE ;
        BufferedImageFuture transportIcon =
                GuiActivator.getImageLoaderService().getImage(iconResource);

        return
            GuiActivator.getImageLoaderService().getIndexedProtocolImage(
                transportIcon,
                chatTransport.getProtocolProvider());
    }

    /**
     * Updates the chat transport presence status.
     *
     * @param chatTransport The chat transport to update.
     */
    public void updateTransportStatus(ChatTransport chatTransport)
    {
        JMenuItem menuItem;
        ImageIconFuture icon;

        if (chatTransport.equals(chatSession.getCurrentChatTransport())
            && !chatTransport.getStatus().isOnline())
        {
            ChatTransport newChatTransport
                = getParentContactTransport(chatTransport);

            ChatTransport onlineTransport = getOnlineTransport();

            if (newChatTransport != null
                && newChatTransport.getStatus().isOnline())
                setSelected(newChatTransport);
            else if (onlineTransport != null)
                setSelected(onlineTransport);
        }

        synchronized (transportMenuItems)
        {
            menuItem = transportMenuItems.get(chatTransport);
        }

        // sometimes it may happen that menuItem is null
        // it was removed for some reason, this maybe due to other bug
        // anyway detect it to avoid NPE
        if (menuItem == null)
            return;

        icon = createTransportSelectorImage(chatTransport).getImageIcon();

        icon.addToMenuItem(menuItem);
        if (menu.getSelectedObject().equals(chatTransport))
        {
            icon.addToButton(this.menu);
        }
    }

    /**
     * In the "send via" menu selects the given contact and sets the given icon
     * to the "send via" menu button.
     *
     * @param menuItem the menu item that is selected
     * @param chatTransport the corresponding chat transport
     * @param icon
     * @param userSelected whether this chat transport change was chosen by the
     * user or happened automatically.
     */
    private void setSelected(JCheckBoxMenuItem menuItem,
                             ChatTransport chatTransport,
                             ImageIconFuture icon,
                             boolean userSelected)
    {
        menuItem.setSelected(true);

        ChatTransport currentChatTransport = this.chatSession.getCurrentChatTransport();

        // If the user has selected an SMS chat transport, just switch to it.
        // Otherwise, if the user chose to switch from SMS to IM, we need to get
        // the chat session to switch to the IM chat transport with no resource
        // so the user will start sending IMs to all registered clients.
        if (!userSelected || (chatTransport instanceof SMSChatTransport))
        {
            this.chatSession.setCurrentChatTransport(chatTransport);
        }
        else if (currentChatTransport instanceof SMSChatTransport)
        {
            this.chatSession.getChatSessionRenderer().resetChatTransport();
        }

        SelectedObject selectedObject = new SelectedObject(icon, chatTransport);

        this.menu.setSelected(selectedObject);

        // Sometimes the transport name or display name can be null, so parse
        // them before building the display name string to avoid NPEs.
        String transportDisplayName = chatTransport.getDisplayName();
        String parsedTransportDisplayName =
            (transportDisplayName == null) ? "" : transportDisplayName;
        String transportName = chatTransport.getName();
        String parsedTransportName = (transportName == null) ? "" : transportName;

        String displayName =
            (!parsedTransportDisplayName.equals(parsedTransportName)) ?
                parsedTransportDisplayName + " (" + parsedTransportName + ")" :
                parsedTransportDisplayName;

        this.menu.setFont(this.menu.getFont().deriveFont(Font.BOLD, ScaleUtils.getDefaultFontSize()));
        this.menu.setToolTipText(displayName);

        chatPanel.setNotificationAreaVisibility(chatTransport);
        ChatWritePanel chatWritePanel = chatPanel.getChatWritePanel();
        chatWritePanel.setSmsLabelVisible(chatTransport.allowsSmsMessage());
        chatWritePanel.updateHintTextForTransport(chatTransport);
    }

    /**
     * Sets the selected contact to the given proto contact.
     * @param chatTransport the proto contact to select
     */
    public void setSelected(ChatTransport chatTransport)
    {
        JCheckBoxMenuItem menuItem = null;

        synchronized (transportMenuItems)
        {
            if (transportMenuItems == null)
            {
                logger.warn("No transport menu items exist");
                return;
            }

            if (chatTransport == null)
            {
                logger.warn("Asked to select null chat transport");
                return;
            }

            menuItem = transportMenuItems.get(chatTransport);
        }

        if (menuItem == null)
        {
            logger.warn("Asked to set invalid chat transport");
            return;
        }

        this.setSelected(
                menuItem,
                chatTransport,
                createTransportSelectorImage(chatTransport).getImageIcon(),
                false);
    }

    /**
     * Returns the protocol menu.
     *
     * @return the protocol menu
     */
    public SIPCommMenu getMenu()
    {
        return menu;
    }

    /**
     * Searches online contacts in the send via combo box.
     *
     * @param chatTransport the chat transport to check
     * @return TRUE if the send via combo box contains online contacts,
     * otherwise returns FALSE.
     */
    private ChatTransport getParentContactTransport(ChatTransport chatTransport)
    {
        synchronized (transportMenuItems)
        {
            for (ChatTransport comboChatTransport : transportMenuItems.keySet())
            {
                if (comboChatTransport.getDescriptor()
                    .equals(chatTransport.getDescriptor())
                    && StringUtils.isNullOrEmpty(
                        comboChatTransport.getResourceName()))
                    return comboChatTransport;
            }
        }
        return null;
    }

    /**
     * Searches online contacts in the send via combo box.
     *
     * @return TRUE if the send via combo box contains online contacts,
     * otherwise returns FALSE.
     */
    private ChatTransport getOnlineTransport()
    {
        synchronized (transportMenuItems)
        {
            for (ChatTransport comboChatTransport : transportMenuItems.keySet())
            {
                if (comboChatTransport.getStatus().isOnline())
                    return comboChatTransport;
            }
        }
        return null;
    }

    /**
     * A custom <tt>SIPCommMenu</tt> that adds an arrow icon to the right of
     * the menu image.
     */
    private class SelectorMenu
        extends SIPCommMenu
    {
        private static final long serialVersionUID = 0L;

        BufferedImageFuture image = GuiActivator.getImageLoaderService().getImage(
                                            ImageLoaderService.DOWN_ARROW_ICON);

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Image image = this.image.resolve();

            g.drawImage(image,
                getWidth() - image.getWidth(this) - 1,
                (getHeight() - image.getHeight(this) - 1)/2,
                this);
        }
    }
}
