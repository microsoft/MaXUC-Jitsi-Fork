/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the <tt>ChatSession</tt> interface that represents a
 * user-to-user chat session.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 *
 */
public class MetaContactChatSession extends ChatSession
                                    implements ContactResourceListener
{
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * The chat transport currently being used by this chat session.
     */
    private ChatTransport mCurrentChatTransport;

    /**
     * The IM contact associated with this chat session.
     */
    private Contact mProtocolContact;

    /**
     * The persistent presence operation set associated with the IM contact.
     */
    private OperationSetPersistentPresence mPresenceOpSet;

    /**
     * A listener used to initialize the chat transports when the IM contact's
     * subscription is resolved.
     */
    private ContactResolutionListener mContactResolutionListener;

    /**
     * The MetaContact that contains the IM contact associated with this chat
     * session.
     */
    private MetaContact mMetaContact;

    /**
     * The UID of the MetaContact that contains the IM contact associated with
     * this chat session.
     */
    private final String mMetaContactUID;

    /**
     * The chat resource used by the IM contact associated with this chat
     * session.
     */
    private final ContactResource mContactResource;

    /**
     * The MetaContactListService.
     */
    private final MetaContactListService mMetaContactListService;

    /**
     * The ChatSessionRenderer that links the chat session to its ChatPanel.
     */
    private final ChatSessionRenderer mSessionRenderer;

    /**
     * A list of listeners registered to receive events when the chat
     * transports associated with this chat session change.
     */
    private final List<ChatSessionChangeListener>
        mChatTransportChangeListeners = new Vector<>();

    /**
     * If true, allow the user to send SMSs in this chat session.
     */
    private final boolean smsEnabled;

    /**
     * Creates an instance of <tt>MetaContactChatSession</tt> by specifying the
     * renderer, which gives the connection with the UI, the meta contact
     * corresponding to the session and the protocol contact and its resource
     * to be used as transport.
     *
     * @param sessionRenderer the renderer, which gives the connection with the UI.
     * @param metaContact the meta contact corresponding to the session and the
     * protocol contact.
     * @param protocolContact the protocol contact to be used as transport.
     * @param contactResource the specific resource to be used as transport
     */
    public MetaContactChatSession(ChatSessionRenderer sessionRenderer,
                                  MetaContact metaContact,
                                  Contact protocolContact,
                                  ContactResource contactResource)
    {
        mSessionRenderer = sessionRenderer;
        mContactResource = contactResource;
        mProtocolContact = protocolContact;
        mMetaContact = metaContact;

        // Obtain the MetaContactListService and add this class to it as a
        // listener of all events concerning the contact list.
        mMetaContactListService = GuiActivator.getContactListService();

        smsEnabled = ConfigurationUtils.isSmsEnabled();

        contactLogger.debug(
            mProtocolContact,
            "Creating chat session with resource " + mContactResource);

        // Ensure we store the MetaContact UID as well as the MetaContact
        // itself, as the MetaContact object may be replaced (e.g. due to a
        // contact merge) so we need to be able to find the correct
        // MetaContact object if that does happen.
        mMetaContactUID = mMetaContact.getMetaUID();
        synchronized (chatParticipants)
        {
            chatParticipants.add(new MetaContactChatContact(mMetaContact));
        }
        initChatTransports();

        if (smsEnabled)
        {
            addSmsListeners();
        }

        // If no current transport is set we choose the one that most recently
        // sent/received a message in this chat session.
        if (mCurrentChatTransport == null)
        {
            selectMostRecentChatTransport();
        }

        if (mMetaContactListService != null)
        {
            mMetaContactListService.addMetaContactListListener(this);
        }
    }

    /**
     * Returns the name of this chat.
     *
     * @return the name of this chat
     */
    public String getChatName()
    {
        String displayName = mMetaContact.getDisplayName();

        if (displayName != null && displayName.length() > 0)
            return mMetaContact.getDisplayName();

        return GuiActivator.getResources().getI18NString("service.gui.UNKNOWN");
    }

    /**
     * Returns the MetaContact we are chatting to.
     *
     * @return The MetaContact we are chatting to.
     */
    public MetaContact getMetaContact()
    {
        return mMetaContact;
    }

    /**
     * UI REFRESH DELETION CANDIDATE
     * REFACTORED TO: MessagingNamespaceHandler.handleGet()
     *
     * Returns a collection of the last N number of messages given by count.
     *
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistory(int count)
    {
        final MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return metaHistory.findLast(chatHistoryFilter, mMetaContact, count);
    }

    /**
     * Returns the start date of the history of this chat session.
     *
     * @return the start date of the history of this chat session.
     */
    public Date getHistoryStartDate()
    {
        Date startHistoryDate = new Date(0);

        MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return startHistoryDate;

        Collection<Object> firstMessage = metaHistory
            .findFirstMessagesAfter(
                chatHistoryFilter, mMetaContact, new Date(0), 1);

        if (firstMessage.size() > 0)
        {
            Iterator<Object> i = firstMessage.iterator();

            Object o = i.next();

            if (o instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent) o;

                startHistoryDate = evt.getTimestamp();
            }
            else if (o instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent) o;

                startHistoryDate = evt.getTimestamp();
            }
            else if (o instanceof FileRecord)
            {
                FileRecord fileRecord = (FileRecord) o;

                startHistoryDate = fileRecord.getDate();
            }
        }

        return startHistoryDate;
    }

    /**
     * Returns the end date of the history of this chat session.
     *
     * @return the end date of the history of this chat session.
     */
    public Date getHistoryEndDate()
    {
        Date endHistoryDate = new Date(0);

        MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return endHistoryDate;

        Collection<Object> lastMessage = metaHistory
            .findLastMessagesBefore(
                chatHistoryFilter, mMetaContact, new Date(Long.MAX_VALUE), 1);

        if (lastMessage.size() > 0)
        {
            Iterator<Object> i1 = lastMessage.iterator();

            Object o1 = i1.next();

            if (o1 instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent) o1;

                endHistoryDate = evt.getTimestamp();
            }
            else if (o1 instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent) o1;

                endHistoryDate = evt.getTimestamp();
            }
            else if (o1 instanceof FileRecord)
            {
                FileRecord fileRecord = (FileRecord) o1;

                endHistoryDate = fileRecord.getDate();
            }
        }

        return endHistoryDate;
    }

    /**
     * Returns the default mobile number used to send sms-es in this session.
     *
     * @return the default mobile number used to send sms-es in this session.
     */
    public String getDefaultSmsNumber()
    {
        String smsNumber = null;

        List<String> detailsList = mMetaContact.getDetails("mobile");

        if (detailsList != null && detailsList.size() > 0)
        {
            smsNumber = detailsList.iterator().next();
        }

        return smsNumber;
    }

    /**
     * Sets the default mobile number used to send sms-es in this session.
     *
     * @param smsPhoneNumber The default mobile number used to send sms-es in
     * this session.
     */
    public void setDefaultSmsNumber(String smsPhoneNumber)
    {
        mMetaContact.addDetail("mobile", smsPhoneNumber);
    }

    /**
     * Initializes all chat transports for this chat session.
     */
    private void initChatTransports()
    {
        contactLogger.info(
            mProtocolContact,
            "Initializing chat transports for contact in MetaContact " +
                                                               mMetaContactUID);

        // As we are initializing chat transports, remove any existing
        // ContactResolutionListener before adding a ContactResolutionListener
        // in case the PersistentPresence OperationSet has been replaced since
        // the last time we initialized.
        removeContactResolutionListener();
        addContactResolutionListener();

        mCurrentChatTransport = null;
        Iterator<Contact> protoContacts = mMetaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();
            if (protoContact.getProtocolProvider().getProtocolName().equals(
                                                          ProtocolNames.JABBER))
            {
                addChatTransports(protoContact,
                              (mContactResource != null) ?
                                  mContactResource.getResourceName() : null,
                              (mProtocolContact != null && protoContact.equals(mProtocolContact)),
                              false);
            }
        }
    }

    /**
     * Returns the currently used transport for all operation within this chat
     * session.
     *
     * @return the currently used transport for all operation within this chat
     * session.
     */
    public ChatTransport getCurrentChatTransport()
    {
        return mCurrentChatTransport;
    }

    /**
     * Sets the transport that will be used for all operations within this chat
     * session.
     *
     * @param chatTransport The transport to set as a default transport for this
     * session.
     */
    public void setCurrentChatTransport(ChatTransport chatTransport)
    {
        this.mCurrentChatTransport = chatTransport;

        for (ChatSessionChangeListener l : mChatTransportChangeListeners)
            l.currentChatTransportChanged(this);

        updateChatContainer();
    }

    public void childContactsReordered(MetaContactGroupEvent evt)
    {}

    /**
     * Implements <tt>MetaContactListListener.metaContactAdded</tt> method.
     * When a MetaContact is added to replace the MetaContact in this chat
     * session (i.e. it has the same UID or its ProtoContact has the same chat
     * address), this method updates this session's MetaContact, protocol
     * contact and chat transports to point to the new MetaContact and
     * ProtoContact.  All of this is necessary to ensure that any open chat
     * windows for the contact continue to work.
     *
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAdded(MetaContactEvent evt)
    {
        MetaContact newMetaContact = evt.getSourceMetaContact();
        Contact imContact = newMetaContact.getIMContact();

        if (newMetaContact.getMetaUID().equals(mMetaContactUID) ||
            (imContact != null &&
                (mProtocolContact != null &&
                    mProtocolContact.getAddress().equalsIgnoreCase(imContact.getAddress()))) ||
            (newMetaContactContainsOldProtoContact(newMetaContact)))
        {
            contactLogger.info("ProtoContact " + mProtocolContact +
                               " in MetaContact " + mMetaContact + " is now " +
                                           imContact + " in " + newMetaContact);

            synchronized (chatParticipants)
            {
                // First, remove the old chat contact from the chat participants
                // and replace it with a new one constructed from the new
                // MetaContact. Also, replace the old MetaContact that was
                // associated with this chat session with the new one.
                chatParticipants.remove(findChatContactByMetaContact(mMetaContact));
                mMetaContact = newMetaContact;
                chatParticipants.add(new MetaContactChatContact(mMetaContact));
            }

            // Next, update the protocol contact and chat transports associated
            // with this chat session.
            contactLogger.info(newMetaContact,
                               "Updating IM contact to " + imContact +
                                      " in MetaContact " + mMetaContactUID);
            if (mProtocolContact != null)
            {
                removeChatTransports(mProtocolContact);
            }

            mProtocolContact = imContact;
            initChatTransports();

            if (smsEnabled)
            {
                // Finally, remove and re-add the SMS chat transports associated
                // with this session, as the phone numbers of the new
                // MetaContact's protocol contacts may be completely different.
                removeSMSChatTransports();
                addSMSChatTransports();
            }

            // If no current transport is set we choose the one we most
            // recently sent/received a message via.
            if (mCurrentChatTransport == null)
            {
                selectMostRecentChatTransport();
            }
        }
    }

    /**
     * Checks whether the new MetaContact contains the same protoContact as we
     * currently have in our current MetaContact.
     *
     * @param newMetaContact the new MetaContact
     * @return whether the new MetaContact contains the same protoContact as we
     *         currently have in our MetaContact
     */
    private boolean newMetaContactContainsOldProtoContact(MetaContact newMetaContact)
    {
        boolean matches = false;
        if (mMetaContact != null)
        {
            Iterator<Contact> protoContacts = mMetaContact.getContacts();
            foundMatch:
            while (protoContacts.hasNext())
            {
                Contact protoContact = protoContacts.next();
                Iterator<Contact> newProtoContacts = newMetaContact.getContacts();

                while (newProtoContacts.hasNext())
                {
                    Contact newProtoContact = newProtoContacts.next();
                    if (newProtoContact == protoContact)
                    {
                        matches = true;
                        break foundMatch;
                    }
                }
            }
        }
        return matches;
    }

    /**
     * Implements <tt>MetaContactListListener.metaContactRenamed</tt> method.
     * When a meta contact is renamed, updates all related labels in this
     * chat panel.
     *
     * @param evt the <tt>MetaContactRenamedEvent</tt> that notified us
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        String newName = evt.getNewDisplayName();

        if (evt.getSourceMetaContact().equals(mMetaContact))
        {
            ChatContact<?> chatContact
                = findChatContactByMetaContact(evt.getSourceMetaContact());
            mSessionRenderer.setContactName(chatContact, newName);
        }
    }

    /**
     * Implements <tt>MetaContactListListener.protoContactAdded</tt> method.
     * When a proto contact is added, updates the "send via" selector box.
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        if (evt.getNewParent().equals(mMetaContact))
        {
            Contact protoContact = evt.getProtoContact();
            contactLogger.info(
                protoContact,
                "Adding chat transports for contact in MetaContact " +
                                                                  mMetaContact);
            addChatTransports(protoContact, null, false, true);
            if (smsEnabled)
            {
                addSMSChatTransports();
            }
        }

        updateChatContainer();
    }

    /**
     * Implements <tt>MetaContactListListener.protoContactMoved</tt> method.
     * When a proto contact is moved, updates the "send via" selector box.
     *
     * @param evt the <tt>ProtoContactEvent</tt> that contains information about
     * the old and the new parent of the contact
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        if (evt.getOldParent() != null &&
            evt.getOldParent().equals(mMetaContact))
        {
            Contact protoContact = evt.getProtoContact();

            contactLogger.debug(
                protoContact,
                "ProtoContact moved from MetaContact " + mMetaContact);

            // If the protoContact that was moved was the chat contact
            // associated with this chat session, update the MetaContact for
            // this chat session to be the protoContact's new parent so that
            // any open chat windows to this contact will still work.  If not,
            // just remove the protoContact's chat transports from this chat
            // session.
            if (protoContact.equals(mProtocolContact) || mProtocolContact == null)
            {
                MetaContact newMetaContact = evt.getNewParent();

                contactLogger.info(
                    mProtocolContact,
                    "Moved ProtoContact was this session's chat contact - " +
                       "updating this session's MetaContact to new parent " +
                                                             newMetaContact);

                synchronized (chatParticipants)
                {
                    // First, remove the old chat contact from the chat
                    // participants and replace it with a new one constructed from
                    // the new MetaContact. Also, replace the old MetaContact that
                    // was associated with this chat session with the new one.
                    chatParticipants.remove(findChatContactByMetaContact(mMetaContact));
                    mMetaContact = newMetaContact;
                    chatParticipants.add(new MetaContactChatContact(mMetaContact));
                }

                if (smsEnabled)
                {
                    // Finally, remove and re-add the SMS chat transports
                    // associated with this session, as the phone numbers of
                    // the new MetaContact's protocol contacts may be
                    // completely different.
                    removeSMSChatTransports();
                    addSMSChatTransports();
                }
            }
            else
            {
                contactLogger.info(
                    protoContact,
                    "Moved ProtoContact not associated with this session");
                protoContactRemoved(evt);
            }
        }
        else if (evt.getNewParent().equals(mMetaContact))
        {
            protoContactAdded(evt);
        }

        updateChatContainer();
    }

    public void metaContactRemoved(MetaContactEvent evt)
    {
        if (evt.getSourceMetaContact().equals(mMetaContact))
        {
            // No MetaContact, so prevent the user from trying to IM/SMS them.
            contactLogger.info(mMetaContact,
                               "Closing chat panel as MetaContact deleted.");
            closeChatPanel();
        }
    }

    /**
     * Implements <tt>MetaContactListListener.protoContactRemoved</tt> method.
     * When a proto contact is removed, updates the "send via" selector box.
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        if (evt.getOldParent().equals(mMetaContact))
        {
            Contact protoContact = evt.getProtoContact();
            contactLogger.info(
                protoContact,
                "Removing chat transports contact in MetaContact " + mMetaContact);

            removeChatTransports(protoContact);

            if (smsEnabled)
            {
                // Remove and re-add the SMS chat transports associated
                // with this session, as the phone numbers of the removed
                // contact may still be present in other protocol contacts in
                // the MetaContact.
                removeSMSChatTransports();
                addSMSChatTransports();
            }
            // If the MetaContact no longer has an IM address then close
            // the Chat Panel.  We only need to handle removing the IM address
            // from a MetaContact here - if the entire MetaContact is removed
            // the metaContactRemoved() handles that.
            else if (mMetaContact.getIMContact() == null)
            {
                closeChatPanel();
            }
        }

        selectMostRecentChatTransport();
        updateChatContainer();
    }

    private void closeChatPanel()
    {
        ChatWindowManager windowManager = GuiActivator.getUIService().
                                                        getChatWindowManager();
        ChatPanel chatPanel = windowManager.getContactChat(mMetaContact, false);

        if (chatPanel != null)
        {
            contactLogger.info(mMetaContact, "Close ChatPanel");
            windowManager.closeChat(chatPanel);
        }
        else
        {
            contactLogger.info(mMetaContact, "Can't close ChatPanel: not found");
        }
    }

    /**
     * Returns the <tt>ChatContact</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     *
     * @param mc the <tt>MetaContact</tt> to search for
     * @return the <tt>ChatContact</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     */
    private ChatContact<?> findChatContactByMetaContact(MetaContact mc)
    {
        List<ChatContact<?>> participantsCopy;
        synchronized (chatParticipants)
        {
            participantsCopy = new ArrayList<>(chatParticipants);
        }

        for (ChatContact<?> chatContact : participantsCopy)
        {
            Object chatSourceContact = chatContact.getDescriptor();
            if (chatSourceContact instanceof MetaContact)
            {
                MetaContact metaChatContact = (MetaContact) chatSourceContact;
                if (metaChatContact.equals(mc))
                    return chatContact;
            }
            else
            {
                assert chatSourceContact instanceof ChatRoomMember;
                ChatRoomMember metaChatContact =
                    (ChatRoomMember)chatSourceContact;
                Contact contact = metaChatContact.getContact();
                MetaContact parentMetaContact
                        = GuiActivator.getContactListService()
                        .findMetaContactByContact(contact);
                if (parentMetaContact != null
                    && parentMetaContact.equals(mc))
                return chatContact;
            }
        }

        return null;
    }

    /**
     * Alerts the chat container that there has been a change in this session.
     * The container can then update the menu items as appropriate
     */
    private void updateChatContainer()
    {
        // Alert the chat window that the MetaContact has changed - it may have
        // to update the status of its menu buttons

        final ChatWindowManager windowManager = GuiActivator.getUIService().
                                                         getChatWindowManager();
        final ChatPanel chatPanel = windowManager.getContactChat(mMetaContact, false);

        if (chatPanel != null)
        {
            final ChatContainer chatWindow = chatPanel.getChatContainer();
            if (chatWindow != null)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        chatWindow.updateContainer(chatPanel);
                    }
                });
            }
        }
    }

    /**
     * Disposes this chat session.
     */
    public void dispose()
    {
        contactLogger.debug(mProtocolContact, "Disposing MetaContactChatSession");

        if (mMetaContactListService != null)
        {
            mMetaContactListService.removeMetaContactListListener(this);
        }

        removeContactResolutionListener();

        synchronized (chatTransports)
        {
            for (ChatTransport chatTransport : chatTransports)
            {
                Object descriptor = chatTransport.getDescriptor();

                if (descriptor instanceof Contact)
                {
                    ((Contact) descriptor).removeResourceListener(this);
                }

                chatTransport.dispose();
            }
        }

        if (smsEnabled)
        {
            removeSmsListeners();
        }
    }

    /**
     * Returns the <tt>ChatSessionRenderer</tt> that provides the connection
     * between this chat session and its UI.
     *
     * @return The <tt>ChatSessionRenderer</tt>.
     */
    public ChatSessionRenderer getChatSessionRenderer()
    {
        return mSessionRenderer;
    }

    /**
     * Returns the descriptor of this chat session.
     *
     * @return the descriptor of this chat session.
     */
    public Object getDescriptor()
    {
        return mMetaContact;
    }

    /**
     * Returns <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     */
    public boolean isDescriptorPersistent()
    {
        if (mMetaContact == null)
            return false;

        Contact defaultContact = mMetaContact.getDefaultContact(
                        OperationSetBasicInstantMessaging.class);

        if (defaultContact == null)
            return false;

        ContactGroup parent = defaultContact.getParentContactGroup();

        boolean isParentPersist = true;
        boolean isParentResolved = true;
        if (parent != null)
        {
            isParentPersist = parent.isPersistent();
            isParentResolved = parent.isResolved();
        }

        if (!defaultContact.isPersistent() &&
            !defaultContact.isResolved() &&
            !isParentPersist &&
            !isParentResolved)
        {
           return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Returns the avatar icon of this chat session.
     *
     * @return the avatar icon of this chat session.
     */
    public BufferedImageFuture getChatAvatar()
    {
        return mMetaContact.getAvatar();
    }

    public void protoContactModified(ProtoContactEvent evt)
    {
        // Nothing needs to be done for this, as the other listeners in this
        // class will get called for events of this type that we care about.
        // We can't even put a log here as it would be too spammy.
    }

    /**
     *  Implements ChatSession#isContactListSupported().
     */
    public boolean isContactListSupported()
    {
        return false;
    }

    public void addChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (mChatTransportChangeListeners)
        {
            if (!mChatTransportChangeListeners.contains(l))
                mChatTransportChangeListeners.add(l);
        }
    }

    public void removeChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (mChatTransportChangeListeners)
        {
            mChatTransportChangeListeners.remove(l);
        }
    }

    @Override
    protected void addSMSChatTransports()
    {
        contactLogger.info(mMetaContact, "Adding SMS chat transports");
        Set<String> phoneNumbers = new HashSet<>();

        // Get a list of all of the MetaContact's valid SMS numbers so that we
        // can add chat transports for them
        phoneNumbers.addAll(mMetaContact.getSmsNumbers());

        for (String phoneNumber : phoneNumbers)
        {
            SMSChatTransport smsChatTransport =
                                        new SMSChatTransport(this, phoneNumber);
            boolean transportAdded = false;

            synchronized (chatTransports)
            {
                if (!chatTransports.contains(smsChatTransport))
                {
                    contactLogger.info(
                        mMetaContact,
                        "Adding SMS chat transport " + smsChatTransport);
                    chatTransports.add(smsChatTransport);
                    transportAdded = true;
                }
            }

            // Do this here to avoid calling outside of the class from within a
            // synchronized block.
            if (transportAdded)
            {
                mSessionRenderer.addChatTransport(smsChatTransport);
            }
        }
    }

    @Override
    protected void removeSMSChatTransports()
    {
        contactLogger.debug(mMetaContact,
                            "Removing all SMS chat transports from this session");
        List<ChatTransport> removedTransports = new ArrayList<>();

        synchronized (chatTransports)
        {
            List<ChatTransport> chatTransportCopy =
                    new ArrayList<>(chatTransports);

            for (ChatTransport chatTransport : chatTransportCopy)
            {
                if (chatTransport instanceof SMSChatTransport)
                {
                    chatTransports.remove(chatTransport);
                    removedTransports.add(chatTransport);
                }
            }
        }

        // Do this here to avoid calling outside of the class from within a
        // synchronized block.
        for (ChatTransport removedTransport : removedTransports)
        {
            mSessionRenderer.removeChatTransport(removedTransport);
        }
    }

    /**
     * Adds all chat transports for the given <tt>contact</tt>.
     *
     * @param contact           the <tt>Contact</tt>, which transports to add
     * @param resourceName      the resource to be pre-selected
     * @param isSelectedContact whether this contact is selected
     * @param resourceAdded     whether this update was triggered by a new
     *                          resource being added for the contact
     */
    private void addChatTransports(Contact contact,
                                   String resourceName,
                                   boolean isSelectedContact,
                                   boolean resourceAdded)
    {
        MetaContactChatTransport chatTransport = null;

        Collection<ContactResource> contactResources = contact.getResources();

        if (contact.supportResources()
            && contactResources != null
            && contactResources.size() > 0)
        {
            Iterator<ContactResource> resourcesIter = contactResources.iterator();

            // Make sure we remove resources that can't be messaged from list
            // so they aren't added to the chat session.
            while (resourcesIter.hasNext())
            {
                ContactResource resource = resourcesIter.next();
                if (!resource.canBeMessaged())
                {
                    contactLogger.debug(
                        contact,
                        "Not adding chat transport for resource that cannot " +
                                  "be messaged: " + resource);
                    resourcesIter.remove();
                }
            }

            if (contactResources.size() > 1)
            {
                chatTransport = new MetaContactChatTransport(this, contact);
                contactLogger.debug(contact,
                                    "Adding chat transport with resources " +
                                               chatTransport);
                addChatTransport(chatTransport);
            }

            resourcesIter = contactResources.iterator();

            while (resourcesIter.hasNext())
            {
                ContactResource resource = resourcesIter.next();
                contactLogger.debug(
                    contact,
                    "Adding chat resource " + resource.getResourceName());

                MetaContactChatTransport resourceTransport
                        =  new MetaContactChatTransport(
                            this,
                            contact,
                            resource,
                            (contact.getResources().size() > 1)
                            ? true : false);

                addChatTransport(resourceTransport);

                if ((resourceName != null
                    && resource.getResourceName().equals(resourceName))
                    || contactResources.size() == 1)
                {
                    contactLogger.debug(
                        contact,
                        "Setting chat transport to resource transport " +
                                          resourceTransport);
                    chatTransport = resourceTransport;
                }
            }
        }

        // If we have found no IM-able resources, add a chat transport with no
        // resources so that we can still send messages to the offline contact,
        // but only if the MetaContact itself is IM-able.
        if (chatTransport == null && mMetaContact.isImCapable())
        {
            chatTransport = new MetaContactChatTransport(this, contact);

            contactLogger.debug(contact,
                                "Adding chat transport with no resources " +
                                               chatTransport);
            addChatTransport(chatTransport);
        }

        if (chatTransport != null)
        {
            // If a new resource has been added to the contact (i.e. they have
            // logged into their chat account on a new device/client), reset the
            // current chat transport, so the contact will receive the next
            // message on all registered clients (including the new one).
            if (resourceAdded && !(mCurrentChatTransport instanceof SMSChatTransport))
            {
                contactLogger.debug(
                    contact,
                    "Resetting current chat transport as new resource added");
                mSessionRenderer.resetChatTransport();
            }
            else
            {
                // If this is the selected contact we set it as a selected transport.
                if (isSelectedContact)
                {
                    contactLogger.debug(
                        contact,
                        "Setting current chat transport to selected transport " +
                                                chatTransport);
                    mCurrentChatTransport = chatTransport;
                    mSessionRenderer.setSelectedChatTransport(chatTransport);
                }
            }
        }
        else
        {
            contactLogger.debug(contact, "No IM chat transports added");
        }

        // Even if we didn't add any chat transports to the contact this time,
        // if it does support resources, we may be able to add chat transports
        // later.  Therefore, we add a resource listener now so that we can do
        // this.
        if (contact.supportResources())
        {
            contactLogger.debug(contact, "Adding resource listener.");
            contact.addResourceListener(this);
        }
    }

    private void addChatTransport(ChatTransport chatTransport)
    {
        synchronized (chatTransports)
        {
            chatTransports.add(chatTransport);
        }

        // Do this here to avoid calling outside of the class from within a
        // synchronized block.
        mSessionRenderer.addChatTransport(chatTransport);
    }

    /**
     * Removes the given <tt>ChatTransport</tt>.
     *
     * @param contact the <tt>ChatTransport</tt>.
     */
    private void removeChatTransports(Contact contact)
    {
        List<ChatTransport> transports;
        List<ChatTransport> removedTransports = new ArrayList<>();
        synchronized (chatTransports)
        {
            transports = new ArrayList<>(chatTransports);

            Iterator<ChatTransport> transportsIter = transports.iterator();
            while (transportsIter.hasNext())
            {
                ChatTransport transport = transportsIter.next();

                if (transport instanceof MetaContactChatTransport)
                {
                    MetaContactChatTransport metaTransport
                    = (MetaContactChatTransport) transport;

                    if (metaTransport.getContact().equals(contact))
                    {
                        chatTransports.remove(metaTransport);
                        removedTransports.add(metaTransport);
                    }
                }
            }
        }

        for (ChatTransport chatTransport : removedTransports)
        {
            mSessionRenderer.removeChatTransport(chatTransport);
        }
    }

    /**
     * Updates the chat transports for the given contact.
     *
     * @param contact           the contact, which related transports to update
     * @param resourceAdded     whether this update was triggered by a new
     *                          resource being added
     */
    private void updateChatTransports(Contact contact, boolean resourceAdded)
    {
        boolean isSelectedContact = false;
        boolean isResourceSelected = false;
        String resourceName = null;

        // We only care about MetaContactChatTransports, as SMSChatTransports
        // don't have different resources.
        if (mCurrentChatTransport instanceof MetaContactChatTransport)
        {
            MetaContactChatTransport currentTransport =
                           (MetaContactChatTransport) getCurrentChatTransport();

            isSelectedContact = currentTransport.getContact().equals(contact);
            isResourceSelected = isSelectedContact &&
                                 currentTransport.getResourceName() != null;
            resourceName = currentTransport.getResourceName();
        }

        removeChatTransports(contact);

        if (isResourceSelected)
            addChatTransports(contact,
                              resourceName,
                              true,
                              resourceAdded);
        else
            addChatTransports(contact,
                              null,
                              isSelectedContact,
                              resourceAdded);
    }

    /**
     * Called when a new <tt>ContactResource</tt> has been added to the list
     * of available <tt>Contact</tt> resources.
     *
     * @param event the <tt>ContactResourceEvent</tt> that notified us
     */
    public void contactResourceAdded(ContactResourceEvent event)
    {
        Contact contact = event.getContact();
        if (mMetaContact.containsContact(contact))
        {
            ContactResource contactResource = event.getContactResource();
            contactLogger.info(
                contact,
                "Contact resource " + contactResource +
                           " added for contact in MetaContact " + mMetaContact);

            // There's no need to update the chat transports if the new
            // resource can't be messaged.
            if (contactResource.canBeMessaged())
            {
                updateChatTransports(contact, true);
            }
        }
    }

    /**
     * Called when a <tt>ContactResource</tt> has been removed to the list
     * of available <tt>Contact</tt> resources.
     *
     * @param event the <tt>ContactResourceEvent</tt> that notified us
     */
    public void contactResourceRemoved(ContactResourceEvent event)
    {
        Contact contact = event.getContact();
        if (mMetaContact.containsContact(contact))
        {
            ContactResource contactResource = event.getContactResource();
            contactLogger.info(
                contact,
                "Contact resource " + contactResource +
                         " removed for contact in MetaContact " + mMetaContact);

            // There's no need to update the chat transports if the removed
            // resource can't be messaged, as it won't be in the current list
            // of chat transports in any case.
            if (contactResource.canBeMessaged())
            {
                updateChatTransports(contact, false);
            }
        }
    }

    /**
     * Called when a <tt>ContactResource</tt> in the list of available
     * <tt>Contact</tt> resources has been modified.
     *
     * @param event the <tt>ContactResourceEvent</tt> that notified us
     */
    public void contactResourceModified(ContactResourceEvent event)
    {
        Contact contact = event.getContact();

        if (mMetaContact.containsContact(contact))
        {
            ChatTransport transport
                = findChatTransportForResource(event.getContactResource());
            contactLogger.info(
                contact,
                "Contact resource " + event.getContactResource() +
                            " modified for contact in MetaContact " + mMetaContact);

            if (transport != null)
                mSessionRenderer.updateChatTransportStatus(transport);
        }
    }

    /**
     * Finds the <tt>ChatTransport</tt> corresponding to the given contact
     * <tt>resource</tt>.
     *
     * @param resource the <tt>ContactResource</tt>, which corresponding
     * transport we're looking for
     * @return the <tt>ChatTransport</tt> corresponding to the given contact
     * <tt>resource</tt>
     */
    private ChatTransport findChatTransportForResource(ContactResource resource)
    {
        List<ChatTransport> transports;
        synchronized (chatTransports)
        {
            transports = new ArrayList<>(chatTransports);
        }

        Iterator<ChatTransport> transportsIter = transports.iterator();
        while (transportsIter.hasNext())
        {
            ChatTransport chatTransport = transportsIter.next();

            if (chatTransport.getDescriptor().equals(resource.getContact())
                && chatTransport.getResourceName() != null
                && chatTransport.getResourceName()
                    .equals(resource.getResourceName()))
                return chatTransport;
        }
        return null;
    }

    /**
     * Adds a ContactResolutionListener to the PersistenPresence Operation Set
     * that listens for contact subscription resolved events so we can update
     * this session's chat transport if one of the MetaContact's contacts
     * becomes resolved.
     */
    private void addContactResolutionListener()
    {
        contactLogger.debug(
            mProtocolContact, "Adding contact resolution listener");

        if (mContactResolutionListener != null)
        {
           contactLogger.debug(mProtocolContact,
               "Not adding ContactResolutionListener as it already exists: " +
                                                    mContactResolutionListener);
           return;
        }

        if ((mPresenceOpSet == null) && (mProtocolContact != null))
        {
            contactLogger.debug(mProtocolContact,
                                "Getting PersistentPresence OpSet for contact");
            ProtocolProviderService provider =
                                         mProtocolContact.getProtocolProvider();
            mPresenceOpSet = (provider == null) ?
                null : provider.getOperationSet(OperationSetPersistentPresence.class);
        }

        if (mPresenceOpSet != null)
        {
            mContactResolutionListener = new ContactResolutionListener();
            mPresenceOpSet.addSubscriptionListener(mContactResolutionListener);
            contactLogger.debug(mProtocolContact,
                "Adding " + mContactResolutionListener + " to " + mPresenceOpSet);
        }
        else
        {
            contactLogger.warn(mProtocolContact,
                               "No persistent presence of set found for contact");
            mContactResolutionListener = null;
        }
    }

    /**
     * If we have added a ContactResolutionListener to the PersistentPresence
     * OperationSet, this method removes the listener.
    */
   private void removeContactResolutionListener()
   {
       if ((mPresenceOpSet != null) && (mContactResolutionListener != null))
       {
           contactLogger.debug(mProtocolContact, "Removing " +
                       mContactResolutionListener + " from " + mPresenceOpSet);
           mPresenceOpSet.removeSubscriptionListener(mContactResolutionListener);
       }

       mPresenceOpSet = null;
       mContactResolutionListener = null;
   }

    /**
     * Listens for contact subscription resolved events so we can update this
     * session's chat transport if one of the MetaContact's contacts becomes
     * resolved.
     */
    private class ContactResolutionListener extends SubscriptionAdapter
    {
        @Override
        public void subscriptionResolved(SubscriptionEvent evt)
        {
            // If this chat session already exists before the contact is
            // resolved, we won't yet have registered a resource listener for
            // this contact.  This means we may miss when some resources are
            // added for the contact and therefore fail to add chat transports
            // for them.  This can happen, for example, if we receive an
            // offline IM on startup that causes the chat panel to open before
            // the contact has been resolved, or if we lose and regain
            // connection to the IM server while the chat panel that uses this
            // chat session is left open.  Therefore, whenever we are told
            // that the contact is resolved, we need to refresh the chat
            // transports.
            Contact sourceContact = evt.getSourceContact();
            if (mMetaContact.containsContact(sourceContact))
            {
                contactLogger.debug(sourceContact,
                                    "Contact resolved - reinitializing chat " +
                                    "transports in MetaContact " +  mMetaContact);

                updateChatTransports(sourceContact, true);
                selectMostRecentChatTransport();
            }
        }
    }
}
