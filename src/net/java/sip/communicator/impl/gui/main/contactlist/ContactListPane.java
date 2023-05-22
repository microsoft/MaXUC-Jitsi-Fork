/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatRoom;
import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The contactlist panel not only contains the contact list but it has the role
 * of a message dispatcher. It process all sent and received messages as well as
 * all typing notifications. Here are managed all contact list mouse events.
 *
 * @author Yana Stamcheva
 */
public class ContactListPane
    extends SIPCommScrollPane
    implements  MessageListener,
                TypingNotificationsListener,
                FileTransferListener,
                ContactListListener,
                PluginComponentListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final MainFrame mMainFrame;

    private TreeContactList mContactList;

    private static final Logger sLog = Logger.getLogger(ContactListPane.class);

    private final ChatWindowManager mChatWindowManager;

    /**
     * The name of the property to specify the vertical scroll unit for the
     * contact list pane.
     */
    private static final String sVerticalScrollUnitProperty
             = "net.java.sip.communicator.impl.gui.main.contactlist.ScrollUnit";

    /**
     * Creates the contactlist scroll panel defining the parent frame.
     *
     * @param mainFrame The parent frame.
     */
    public ContactListPane(MainFrame mainFrame)
    {
        this.mMainFrame = mainFrame;

        this.mChatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();

        this.setHorizontalScrollBarPolicy(
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        this.setBorder(BorderFactory.createEmptyBorder());

        // Set the vertical scroll increment iff there is a property set for it.
        int scrollUnit = GuiActivator.getConfigurationService().global().getInt(
                                                     sVerticalScrollUnitProperty,
                                                     -1);
        if (scrollUnit != -1)
        {
            sLog.debug("Overring scroll unit to: " + scrollUnit);
            this.getVerticalScrollBar().setUnitIncrement(scrollUnit);
        }

        this.initPluginComponents();
        this.mContactList = new TreeContactList(mainFrame);
    }

    /**
     * Initializes the contact list.
     *
     * @param contactListService The MetaContactListService which will be used
     *            for a contact list data model.
     */
    public void initList(MetaContactListService contactListService)
    {
        TransparentPanel transparentPanel
            = new TransparentPanel(new BorderLayout());

        transparentPanel.add(mContactList, BorderLayout.NORTH);

        this.setViewportView(transparentPanel);

        transparentPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        this.mContactList.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        this.mContactList.addContactListListener(this);
    }

    /**
     * Returns the contact list.
     *
     * @return the contact list
     */
    public TreeContactList getContactList()
    {
        return mContactList;
    }

    /**
     * Implements the ContactListListener.contactSelected method.
     * @param evt the <tt>ContactListEvent</tt> that notified us
     */
    public void contactClicked(ContactListEvent evt)
    {
        // We're interested only in two click events.
        if (evt.getClickCount() != 2)
            return;

        UIContact uiContact = evt.getSourceContact();

        // We're only interested in MetaContacts and SourceContacts
        if (!(uiContact.getDescriptor() instanceof MetaContact ||
              uiContact.getDescriptor() instanceof SourceContact))
            return;

        String clickAction = ConfigurationUtils.getContactDoubleClickAction();
        sLog.debug("User double clicked on contact, action: " + clickAction);

        if (clickAction == null ||
            clickAction.equals("CALL"))
        {
            // Can just make call directly
            callContact(uiContact);
        }
        else
        {
            // Otherwise we need a MetaContact, try and find it
            MetaContact contact = null;
            ContactDetail contactDetail = null;

            if (uiContact.getDescriptor() instanceof MetaContact)
            {
                contact = (MetaContact) uiContact.getDescriptor();
            }
            else if (uiContact.getDescriptor() instanceof SourceContact)
            {
                // We've got a source contact, look for the MetaContact in the
                // data
                SourceContact sourceContact = (SourceContact)
                                                      uiContact.getDescriptor();

                @SuppressWarnings("unchecked")
                List<MetaContact> metaContacts = (List<MetaContact>)
                        sourceContact.getData(SourceContact.DATA_META_CONTACTS);
                contact = (metaContacts != null && metaContacts.size() == 1) ?
                                                     metaContacts.get(0) : null;

                // We could be on a group chat entry. If so, find the contact
                // detail relating to the group chat
                List<ContactDetail> mucContactDetails = new ArrayList<>();
                mucContactDetails = sourceContact.getContactDetails(OperationSetMultiUserChat.class);

                if ((mucContactDetails != null) && (mucContactDetails.size() > 0))
                {
                    contactDetail = mucContactDetails.get(0);
                }
            }

            if (contact == null)
            {
                // No contact, we might be on a group chat entry
                if (contactDetail != null)
                {
                    SourceContact sourceContact =
                                      (SourceContact) uiContact.getDescriptor();
                    String chatRoomId = contactDetail.getDetail();

                    if (clickAction.equals("IM"))
                    {
                        sLog.info("Start group chat with id " + sanitiseChatRoom(chatRoomId) +
                                          " from double click on contact list");

                        GuiActivator.getUIService().startGroupChat(
                                         chatRoomId,
                                         (Boolean)sourceContact.getData("isClosed"),
                                         sourceContact.getDisplayName());
                    }
                    else if (clickAction.equals("CONF"))
                    {
                        sLog.info(
                            "Start conference with group chat with id " +
                            sanitiseChatRoom(chatRoomId) + " from double click on contact list");

                        if (CreateConferenceMenu.isConferenceInviteByImEnabled())
                        {
                            ProtocolProviderService imProvider =
                                                  AccountUtils.getImProvider();

                            if (imProvider != null &&
                                imProvider.isRegistered() &&
                                ConfigurationUtils.isMultiUserChatEnabled())
                            {
                                OperationSetMultiUserChat opSetMuc =
                                    imProvider.getOperationSet(
                                               OperationSetMultiUserChat.class);
                                try
                                {
                                    ChatRoom chatRoom =
                                                  opSetMuc.findRoom(chatRoomId);
                                    GuiActivator.getConferenceService()
                                        .createOrAdd(chatRoom, false);
                                }
                                catch (OperationFailedException | OperationNotSupportedException ex)
                                {
                                    sLog.warn("Failed to find chat room ", ex);
                                }
                            }
                            else
                            {
                                sLog.debug("User double clicked to start "
                                           + "conf but IM/MUC is unavailable.");
                            }
                        }
                        else
                        {
                            sLog.debug("User double clicked to start conf " +
                                                      "when conf is disabled.");
                        }
                    }
                }

                sLog.debug("Double click but no contact for action");
            }
            else if (clickAction.equals("IM"))
            {
                GuiActivator.getUIService()
                            .getChatWindowManager()
                            .startChat(contact);
            }
            else if (clickAction.equals("VIEW"))
            {
                GuiActivator.getViewContactService()
                            .createViewContactWindow(mMainFrame, contact)
                            .setVisible(true);
            }
            else if (clickAction.equals("CONF"))
            {
                sLog.info("Start conference from contact from double " +
                                                       "click on contact list");

                if (CreateConferenceMenu.isConferenceInviteByImEnabled())
                {
                    GuiActivator.getConferenceService().createOrAdd(contact, false);
                }
                else
                {
                    sLog.debug("User double clicked to start conf when " +
                                                           "conf is disabled.");
                }
            }
        }
    }

    /**
     * Calls the contact provided by uiContact based on the following logic:
     * <li/> If there is just one telephony contact then call that
     * <li/> If this is a locally stored contact or an LDAP contact then use
     * the configured order of preferences to determine which number to call
     * <li/> If this is a call history contact then call the number associated
     * with the call history event. If the call history contact matches a
     * locally stored contact then ensure this information is passed to the
     * CallManager.
     *
     * @param uiContact The UIContact to call
     */
    private void callContact(UIContact uiContact)
    {
        // Attempt to find the MetaContact associated with this UIContact
        MetaContact metaContact = getMetaContactFromSource(uiContact);
        String contactAddress = null;

        if (uiContact instanceof SourceUIContact)
        {
            // This is a source contact - could be from history or LDAP. Get
            // the default address for calling this contact.
            UIContactDetail contactDetail = uiContact.getDefaultContactDetail(
                                              OperationSetBasicTelephony.class);

            if (contactDetail != null)
            {
                contactAddress = contactDetail.getAddress();
                if (contactAddress != null)
                {
                    contactAddress = contactAddress.split("@")[0];
                }
            }
        }
        else if (metaContact != null)
        {
            contactAddress = metaContact.getPreferredPhoneNumber();
        }
        else
        {
            // We can not call contacts that are not MetaContacts or
            // SourceUIContacts.
            return;
        }

        // If there is only one telephony contact then call it. Otherwise
        // check the order of preference.
        if (contactAddress != null && !contactAddress.isEmpty())
        {
            CallManager.createCall(contactAddress, Reformatting.NEEDED);
        }
    }

    /**
     * Returns a <tt>MetaContact</tt> associated with the given UIContact
     *
     * @param uiContact the UIContact for which to find the
     * MetaContact.
     * @returns the MetaContact associated with this UIContact, null if
     * not found.
     */
    private MetaContact getMetaContactFromSource(UIContact uiContact)
    {
        MetaContact metaContact = null;

        if (uiContact.getDescriptor() instanceof MetaContact)
        {
            metaContact =  (MetaContact) uiContact.getDescriptor();
        }
        else if (uiContact instanceof SourceUIContact)
        {
            SourceContact contactSource = ((SourceUIContact) uiContact)
                                                            .getSourceContact();

            @SuppressWarnings("unchecked")
            List<MetaContact> metaContacts = (List<MetaContact>)
                        contactSource.getData(SourceContact.DATA_META_CONTACTS);

            if (metaContacts != null && metaContacts.size() == 1)
                metaContact = metaContacts.get(0);
        }

        return metaContact;
    }

    /**
     * Implements the ContactListListener.groupSelected method.
     * @param evt the <tt>ContactListEvent</tt> that notified us
     */
    public void groupClicked(ContactListEvent evt) {}

    /**
     * We're not interested in group selection events here.
     */
    public void groupSelected(ContactListEvent evt) {}

    /**
     * We're not interested in contact selection events here.
     */
    public void contactSelected(ContactListEvent evt) {}

    /**
     * UI REFRESH DELETION CANDIDATE
     * (PARTIALLY) REFACTORED TO:
     * MessagingNamespaceHandler.notifyUpdateChatMessage()
     *
     * When a message is received determines whether to open a new chat window
     * or chat window tab, or to indicate that a message is received from a
     * contact which already has an open chat. When the chat is found checks if
     * in mode "Auto popup enabled" and if this is the case shows the message in
     * the appropriate chat panel.
     *
     * @param evt the event containing details on the received message
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        // Nothing to do if this is an archive message
        if (evt.getSourceMessage().isArchive())
            return;

        MetaContactListService contactListService =
                                           GuiActivator.getContactListService();
        MetaContact metaContact = null;
        Contact protocolContact = evt.getPeerContact();
        ContactResource contactResource = evt.getContactResource();
        String peerIdentifier = evt.getPeerIdentifier();
        ImMessage message = evt.getSourceMessage();
        int eventType = evt.getEventType();

        if (eventType == MessageEvent.SMS_MESSAGE)
        {
            sLog.debug("MESSAGE RECEIVED from SMS number: " + logHasher(peerIdentifier));
            metaContact =
                contactListService.findMetaContactForSmsNumber(peerIdentifier);
        }
        else
        {
            sLog.debug("MESSAGE RECEIVED from contact: " + evt.getPeerContact());

            metaContact =
                contactListService.findMetaContactByContact(protocolContact);
        }

        if(metaContact != null || peerIdentifier != null)
        {
            messageReceived(protocolContact,
                            contactResource,
                            metaContact,
                            peerIdentifier,
                            message,
                            eventType,
                            evt.getTimestamp(),
                            evt.getCorrectedMessageUID());
        }
        else
        {
            sLog.debug(
                "MetaContact not found for protocol contact: " + protocolContact);
        }
    }

    /**
     * UI REFRESH DELETION CANDIDATE
     * (PARTIALLY) REFACTORED TO:
     * MessagingNamespaceHandler.notifyUpdateChatMessage()
     *
     * When a message is received determines whether to open a new chat window
     * or chat window tab, or to indicate that a message is received from a
     * contact which already has an open chat. When the chat is found checks if
     * in mode "Auto popup enabled" and if this is the case shows the message in
     * the appropriate chat panel.
     *
     * @param protocolContact the source contact of the event
     * @param contactResource the resource from which the contact is writing
     * @param metaContact the metacontact containing <tt>protocolContact</tt>
     * @param message the message to deliver
     * @param eventType the event type
     * @param timestamp the timestamp of the event
     * @param correctedMessageUID the identifier of the corrected message
     */
    private void messageReceived(final Contact protocolContact,
                                 final ContactResource contactResource,
                                 final MetaContact metaContact,
                                 final String smsNumber,
                                 final ImMessage message,
                                 final int eventType,
                                 final Date timestamp,
                                 final String correctedMessageUID)
    {
        if (eventType == MessageEvent.CHAT_MESSAGE && metaContact == null)
        {
            // There's not a lot that we can do in this case.  Log and ignore the
            // message.
            sLog.error("Got an IM message for a null MetaContact " + message);
            return;
        }

        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    messageReceived(protocolContact,
                                    contactResource,
                                    metaContact,
                                    smsNumber,
                                    message,
                                    eventType,
                                    timestamp,
                                    correctedMessageUID);
                }
            });
            return;
        }

        // Obtain the corresponding chat panel.
        final ChatPanel chatPanel;

        if (eventType == MessageEvent.SMS_MESSAGE)
        {
            if (metaContact == null)
            {
                chatPanel = mChatWindowManager.getContactChat(
                                      smsNumber, true);
            }
            else
            {
                chatPanel = mChatWindowManager.getContactChat(
                                    metaContact, true);
            }
        }
        else
        {
            chatPanel = mChatWindowManager.getContactChat(metaContact,
                                                         protocolContact,
                                                         contactResource);
        }

        if (metaContact != null)
        {
            // Show an envelope on the sender contact in the contact list and
            // in the systray.
            if (!chatPanel.isChatFocused())
                mContactList.setActiveContact(metaContact, true);
        }

        // We don't need to automatically open the Java/Swing chat window since we're already
        // showing the conversation in the Electron UI.

        String resourceName = (contactResource != null)
                ? contactResource.getResourceName()
                : null;

        ChatTransport chatTransport = null;
        ChatSession chatSession = chatPanel.getChatSession();

        if (eventType == MessageEvent.SMS_MESSAGE)
        {
            sLog.debug("Setting SMS chat transport for " + logHasher(smsNumber));
            chatTransport = chatSession.findChatTransportForSmsNumber(smsNumber);
        }
        else
        {
            sLog.debug("Setting IM chat transport for " + protocolContact +
                                                 ", resource: " + resourceName);
            chatTransport = chatSession.findChatTransportForContact(
                                                 protocolContact, resourceName);
        }

        if (chatTransport != null)
        {
            chatSession.setCurrentChatTransport(chatTransport);
            chatPanel.setSelectedChatTransport(chatTransport);
        }
    }

    /**
     * When a sent message is delivered shows it in the chat conversation panel.
     *
     * @param evt the event containing details on the message delivery
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
    }

    /**
     * Shows a warning message to the user when message delivery has failed.
     *
     * @param evt the event containing details on the message delivery failure
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
        sLog.error(evt);

        ChatPanel.OfflineWarning offlineWarning = null;

        Contact sourceContact = evt.getPeerContact();
        String peerIdentifier = evt.getPeerIdentifier();
        String displayName = "";
        MetaContact metaContact = null;

        if (evt.getEventType() == MessageEvent.SMS_MESSAGE)
        {
            displayName = peerIdentifier;
            metaContact = GuiActivator.getContactListService().
                                    findMetaContactForSmsNumber(peerIdentifier);
        }
        else
        {
            displayName = sourceContact.getDisplayName();
            metaContact = GuiActivator.getContactListService().
                                        findMetaContactByContact(sourceContact);
        }

        if (metaContact != null)
        {
            displayName = metaContact.getDisplayName();
        }

        if (evt.getErrorCode()
                == MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED)
        {
            // We now know that offline messages are unsupported, so update
            // the offline warning to be more specific to reflect that.
            offlineWarning =
                ChatPanel.OfflineWarning.OFFLINE_QUEUE_UNSUPPORTED_WARNING;
        }
        else if (evt.getErrorCode()
            == MessageDeliveryFailedEvent.OFFLINE_MESSAGE_QUEUE_FULL)
        {
            // We now know that this contact's offline message queue is full,
            // so update the offline warning to be more specific to reflect
            // that.
            offlineWarning = ChatPanel.OfflineWarning.OFFLINE_QUEUE_FULL_WARNING;
        }

        ChatPanel chatPanel;

        if (metaContact == null)
        {
            sLog.debug("Getting SMS chat for number " + logHasher(peerIdentifier));
            chatPanel = mChatWindowManager.getContactChat(peerIdentifier, true);
        }
        else if (peerIdentifier != null)
        {
            sLog.debug("Getting MetaContact SMS chat for " + logHasher(displayName) +
                       " and number " + logHasher(peerIdentifier));
            chatPanel = mChatWindowManager.getContactChat(metaContact, true);

            // Make sure we set the chat transport to the SMS transport for
            // the given number.
            ChatSession chatSession = chatPanel.getChatSession();
            ChatTransport chatTransport =
                       chatSession.findChatTransportForSmsNumber(peerIdentifier);

            if (chatTransport != null)
            {
                sLog.debug("Setting SMS chat transport for " + logHasher(peerIdentifier));
                chatSession.setCurrentChatTransport(chatTransport);
                chatPanel.setSelectedChatTransport(chatTransport);
            }
        }
        else
        {
            sLog.debug("Getting MetaContact IM chat for " + metaContact);
            chatPanel = mChatWindowManager.getContactChat(metaContact, sourceContact);
        }

        if (offlineWarning != null)
        {
            sLog.debug("Updating offline warning");
            chatPanel.updateOfflineWarning(offlineWarning);
        }

        // We don't need to automatically open the Java/Swing chat window since we're already
        // showing the conversation in the Electron UI.
    }

    /**
     * Informs the user what is the typing state of his chat contacts.
     *
     * @param evt the event containing details on the typing notification
     */
    public void typingNotificationReceived(TypingNotificationEvent evt)
    {
        sLog.entry();

        MetaContact metaContact =
            GuiActivator.getContactListService().findMetaContactByContact(evt.getSourceContact());
        sLog.debug("Typing notification (" + evt.getTypingState() + ") received for " + metaContact);

        ChatPanel chatPanel = mChatWindowManager.getContactChat(metaContact, false);
        boolean isTyping = false;

        if (chatPanel != null)
        {
            String contactName = metaContact.getDisplayName();
            String notificationMsg;

            if (contactName.equals(""))
            {
                contactName = GuiActivator.getResources()
                    .getI18NString("service.gui.UNKNOWN");
            }

            TypingState typingState = evt.getTypingState();

            if (typingState == TypingState.TYPING)
            {
                notificationMsg = GuiActivator.getResources().getI18NString(
                    "service.gui.CONTACT_TYPING",
                    new String[]{contactName});

                chatPanel.addTypingNotification(notificationMsg);
                isTyping = true;
            }
            else if (typingState == TypingState.PAUSED)
            {
                notificationMsg = GuiActivator.getResources().getI18NString(
                        "service.gui.CONTACT_PAUSED_TYPING",
                        new String[]{contactName});

                chatPanel.addTypingNotification(notificationMsg);
            }
            else
            {
                chatPanel.removeTypingNotification();
            }

            metaContact.setDetail(MetaContact.IS_CONTACT_TYPING, String.valueOf(isTyping));
            GuiActivator.getWISPAService().notify(WISPANamespace.CONTACTS, WISPAAction.DATA, metaContact);
        }

        sLog.exit();
    }

    /**
     * When a request has been received we show it to the user through the
     * chat session renderer.
     *
     * @param event <tt>FileTransferRequestEvent</tt>
     * @see FileTransferListener#fileTransferRequestReceived(FileTransferRequestEvent)
     */
    public void fileTransferRequestReceived(FileTransferRequestEvent event)
    {
        IncomingFileTransferRequest request = event.getRequest();

        Contact sourceContact = request.getSender();

        MetaContact metaContact = GuiActivator.getContactListService()
            .findMetaContactByContact(sourceContact);

        final ChatPanel chatPanel
            = mChatWindowManager.getContactChat(metaContact, sourceContact);

        chatPanel.addIncomingFileTransferRequest(
            event.getFileTransferOperationSet(), request, event.getTimestamp());

        ChatTransport chatTransport
            = chatPanel.getChatSession()
                .findChatTransportForContact(sourceContact, null);

        chatPanel.setSelectedChatTransport(chatTransport);

        if (ConfigurationUtils.isOpenWindowOnNewChatEnabled())
        {
            // Opens the chat panel with the new message in the UI thread.
            mChatWindowManager.openChatAndAlertWindow(chatPanel, false);
        }
        else
        {
            sLog.debug("Configured not to open window on new chat");
        }
    }

    /**
     * Nothing to do here.
     * @param event the <tt>FileTransferCreatedEvent</tt> that notified us
     */
    public void fileTransferRequestThumbnailUpdate(FileTransferRequestEvent event)
    {}

    /**
     * Nothing to do here, because we already know when a file transfer is
     * created.
     * @param event the <tt>FileTransferCreatedEvent</tt> that notified us
     */
    public void fileTransferCreated(FileTransferCreatedEvent event)
    {}

    /**
     * Called when a new <tt>IncomingFileTransferRequest</tt> has been rejected.
     * Nothing to do here, because we are the one who rejects the request.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the
     * received request which was rejected.
     */
    public void fileTransferRequestRejected(FileTransferRequestEvent event)
    {
    }

    /**
     * Called when an <tt>IncomingFileTransferRequest</tt> has been canceled
     * from the contact who sent it.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the
     * request which was canceled.
     */
    public void fileTransferRequestCanceled(FileTransferRequestEvent event)
    {
    }

    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference<?>[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CONTACT_LIST.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            sLog.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                PluginComponent component
                    = (PluginComponent)
                        GuiActivator.bundleContext.getService(serRef);

                Object selectedValue = getContactList().getSelectedValue();

                if(selectedValue instanceof MetaContact)
                {
                    component.setCurrentContact((MetaContact)selectedValue);
                }
                else if(selectedValue instanceof MetaContactGroup)
                {
                    component
                        .setCurrentContactGroup((MetaContactGroup)selectedValue);
                }

                String pluginConstraints = component.getConstraints();
                Object constraints = null;

                if (pluginConstraints != null)
                    constraints = StandardUIServiceImpl
                        .getBorderLayoutConstraintsFromContainer(
                            pluginConstraints);
                else
                    constraints = BorderLayout.SOUTH;

                this.add((Component)component.getComponent(), constraints);

                this.repaint();
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Adds the plugin component given by <tt>event</tt> to this panel if it's
     * its container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent pluginComponent = event.getPluginComponent();

        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!pluginComponent.getContainer()
                .equals(Container.CONTAINER_CONTACT_LIST))
            return;

        Object constraints = StandardUIServiceImpl
            .getBorderLayoutConstraintsFromContainer(
                    pluginComponent.getConstraints());

        if (constraints == null)
            constraints = BorderLayout.SOUTH;

        this.add((Component) pluginComponent.getComponent(), constraints);

        Object selectedValue = getContactList().getSelectedValue();

        if(selectedValue instanceof MetaContact)
        {
            pluginComponent
                .setCurrentContact((MetaContact)selectedValue);
        }
        else if(selectedValue instanceof MetaContactGroup)
        {
            pluginComponent
                .setCurrentContactGroup((MetaContactGroup)selectedValue);
        }

        this.revalidate();
        this.repaint();
    }

    /**
     * Removes the plugin component given by <tt>event</tt> if previously added
     * in this panel.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!c.getContainer()
                .equals(Container.CONTAINER_CONTACT_LIST))
            return;

        this.remove((Component) c.getComponent());
    }
}
