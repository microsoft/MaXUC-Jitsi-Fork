/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public abstract class ChatSession implements  MetaContactListListener,
                                              ServiceListener,
                                              RegistrationStateChangeListener
{
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    private static Logger sLog = Logger.getLogger(ChatSession.class);

    /**
     * The chat history filter.
     */
    protected final String[] chatHistoryFilter
        = new String[]{ MessageHistoryService.class.getName(),
                        FileHistoryService.class.getName()};

    /**
     * The list of <tt>ChatContact</tt>s contained in this chat session.
     */
    protected final List<ChatContact<?>> chatParticipants
        = new ArrayList<>();

    /**
     * The list of <tt>ChatTransport</tt>s available in this session.
     */
    protected final List<ChatTransport> chatTransports
        = new LinkedList<>();

    /**
     * The Jabber Protocol Provider Service
     */
    private ProtocolProviderService imProvider;

    /**
     * Returns the descriptor of this chat session.
     *
     * @return the descriptor of this chat session.
     */
    public abstract Object getDescriptor();

    /**
     * Returns <code>true</code> if this chat session descriptor is persistent,
     * otherwise returns <code>false</code>.
     * @return <code>true</code> if this chat session descriptor is persistent,
     * otherwise returns <code>false</code>.
     */
    public abstract boolean isDescriptorPersistent();

    /**
     * Returns a list of all participants contained in this
     * chat session.
     *
     * @return a list of all participants contained in this
     * chat session.
     */
    public List<ChatContact<?>> getParticipants()
    {
        synchronized (chatParticipants)
        {
            return new ArrayList<>(chatParticipants);
        }
    }

    /**
     * Returns all available chat transports for this chat session. Each chat
     * transport is corresponding to a protocol provider.
     *
     * @return all available chat transports for this chat session.
     */
    public Iterator<ChatTransport> getChatTransports()
    {
        List<ChatTransport> chatTransportsCopy = null;

        synchronized (chatTransports)
        {
            chatTransportsCopy = new LinkedList<>(chatTransports);
        }
        return chatTransportsCopy.iterator();
    }

    /**
     * Returns the currently used transport for all operation within this chat
     * session.
     *
     * @return the currently used transport for all operation within this chat
     * session.
     */
    public abstract ChatTransport getCurrentChatTransport();

    /**
     * Returns a list of all <tt>ChatTransport</tt>s contained in this session
     * supporting the given <tt>opSetClass</tt>.
     * @param opSetClass the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ChatTransport</tt>s contained in this session
     * supporting the given <tt>opSetClass</tt>
     */
    public List<ChatTransport> getTransportsForOperationSet(
                                    Class<? extends OperationSet> opSetClass)
    {
        LinkedList<ChatTransport> opSetTransports
            = new LinkedList<>();

        synchronized (chatTransports)
        {
            for (ChatTransport transport : chatTransports)
            {
                ProtocolProviderService protocolProvider =
                                                transport.getProtocolProvider();
                if ((protocolProvider != null) &&
                    (protocolProvider.getOperationSet(opSetClass) != null))
                     opSetTransports.add(transport);
            }
        }
        return opSetTransports;
    }

    /**
     * Returns the <tt>ChatSessionRenderer</tt> that provides the connection
     * between this chat session and its UI.
     *
     * @return The <tt>ChatSessionRenderer</tt>.
     */
    public abstract ChatSessionRenderer getChatSessionRenderer();

    /**
     * Sets the transport that will be used for all operations within this chat
     * session.
     *
     * @param chatTransport The transport to set as a default transport for this
     * session.
     */
    public abstract void setCurrentChatTransport(ChatTransport chatTransport);

    /**
     * Returns the name of the chat. If this chat panel corresponds to a single
     * chat it will return the name of the <tt>MetaContact</tt>, otherwise it
     * will return the name of the chat room.
     *
     * @return the name of the chat
     */
    public abstract String getChatName();

    /**
     * Returns a collection of the last N number of history messages given by
     * count.
     *
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public abstract Collection<Object> getHistory(int count);

    /**
     * Returns the start date of the history of this chat session.
     *
     * @return the start date of the history of this chat session.
     */
    public abstract Date getHistoryStartDate();

    /**
     * Returns the end date of the history of this chat session.
     *
     * @return the end date of the history of this chat session.
     */
    public abstract Date getHistoryEndDate();

    /**
     * Returns the default mobile number used to send sms-es in this session.
     *
     * @return the default mobile number used to send sms-es in this session.
     */
    public abstract String getDefaultSmsNumber();

    /**
     * Sets the default mobile number used to send sms-es in this session.
     *
     * @param smsPhoneNumber The default mobile number used to send sms-es in
     * this session.
     */
    public abstract void setDefaultSmsNumber(String smsPhoneNumber);

    /**
     * Disposes this chat session.
     */
    public abstract void dispose();

    /**
     * Initializes all SMS chat transports for this chat session.
     */
    protected abstract void addSMSChatTransports();

    /**
     * Removes all SMS chat transports for this chat session.
     */
    protected abstract void removeSMSChatTransports();

    /**
     * Returns the ChatTransport corresponding to the given contact.
     *
     * @param contact The contact of the chat transport we're looking for.
     * @param resourceName The name of the resource if any, null otherwise
     * @return The ChatTransport corresponding to the given contact.
     */
    public ChatTransport findChatTransportForContact(Contact contact,
                                                     String resourceName)
    {
        synchronized (chatTransports)
        {
            for (ChatTransport chatTransport : chatTransports)
            {
                String transportResName = chatTransport.getResourceName();

                if (chatTransport.getDescriptor().equals(contact)
                    && (resourceName == null
                        || (transportResName != null
                            && transportResName.equals(resourceName))))
                    return chatTransport;
            }
        }
        return null;
    }

    /**
     * Returns the ChatTransport corresponding to the given SMS number.
     *
     * @param smsNumber The SMS number of the chat transport we're looking for.
     * @return The ChatTransport corresponding to the given SMS number.
     */
    public ChatTransport findChatTransportForSmsNumber(String smsNumber)
    {
        ChatTransport smsTransport = null;

        synchronized (chatTransports)
        {
            for (ChatTransport chatTransport : chatTransports)
            {
                if ((chatTransport instanceof SMSChatTransport) &&
                    (chatTransport.getDescriptor().equals(smsNumber)))
                {
                    smsTransport = chatTransport;
                    break;
                }
            }
        }

        return smsTransport;
    }

    /**
     * Sets the current chat transport to be the one representing the most
     * recently sent/received message in this chat session.
     *
     * <li> If the most recent message sent/received in this chat was an IM or
     * file transfer via IM, this method sets the transport to the IM transport
     * with no resource for the contact that sent/received that message.
     * Sending a message to that transport will mean it is received by all
     * registered resources for that IM address.</li>
     * <li> If the most recent message was an SMS, it sets the transport to the
     * SMS transport that corresponds to the SMS number that sent/received the
     * message.</li>
     * <li> If no such transport is found, it sets the transport to the first
     * one in the list that supports IM.</li>
     */
    public void selectMostRecentChatTransport()
    {
        ChatTransport chatTransport = null;

        // Get the last message in history for this chat session
        Collection<Object> lastMessages = getHistory(1);
        if (lastMessages.size() > 0)
        {
            Object next = lastMessages.iterator().next();

            if (next instanceof OneToOneMessageEvent)
            {
                OneToOneMessageEvent lastMessage = (OneToOneMessageEvent) next;
                if (lastMessage.getEventType() == MessageEvent.SMS_MESSAGE)
                {
                    String peerIdentifier = lastMessage.getPeerIdentifier();
                    chatTransport = findChatTransportForSmsNumber(peerIdentifier);
                    contactLogger.debug("Found SMS transport for " +
                                        logHasher(peerIdentifier) + "?: " + chatTransport);
                }
                else
                {
                    Contact peerContact = lastMessage.getPeerContact();
                    chatTransport = findChatTransportForContact(peerContact, null);
                    contactLogger.debug("Found IM transport for " +
                                           peerContact + "?: " + chatTransport);
                }
            }
            else if (next instanceof FileRecord)
            {
                FileRecord lastMessage = (FileRecord) next;
                Contact peerContact = lastMessage.getContact();
                chatTransport = findChatTransportForContact(peerContact, null);
                contactLogger.debug("Found file transfer IM transport for " +
                                           peerContact + "?: " + chatTransport);
            }
            else
            {
                // We only expect IMs, SMSs or file transfers here, as chat
                // transports don't apply to group chats.
                contactLogger.warn(
                    "Last message not OneToOneMessageEvent or FileRecord: " + next);
            }
        }

        if (chatTransport != null)
        {
            setCurrentChatTransport(chatTransport);
            getChatSessionRenderer().setSelectedChatTransport(chatTransport);
        }
        else
        {
            contactLogger.warn(
                "No transport found - selecting first chat transport");
            selectFirstChatTransport();
        }
    }

    /**
     * Sets the current chat transport to the first one in the list that
     * supports IM, if there is one.  Otherwise, it does nothing.
     */
    public void selectFirstChatTransport()
    {
        ChatTransport firstChatTransport = null;

        synchronized (chatTransports)
        {
            for (ChatTransport chatTransport : chatTransports)
            {
                if (chatTransport.allowsInstantMessage())
                {
                    firstChatTransport = chatTransport;
                    break;
                }
            }
        }

        if (firstChatTransport != null)
        {
            setCurrentChatTransport(firstChatTransport);
            contactLogger.debug(
                "Setting current chat transport to first transport " +
                    firstChatTransport.getResourceName());
            getChatSessionRenderer().setSelectedChatTransport(firstChatTransport);
        }
    }

    /**
     * Adds listeners for the jabber provider registering/unregistering so we
     * can add and remove SMS transports when it does.
     */
    protected void addSmsListeners()
    {
        sLog.info("Adding SMS listeners to " + this);

        // First add a service listener so we will be notified whenever the
        // jabber service starts up.
        GuiActivator.bundleContext.addServiceListener(this);

        // Next check if we already have a jabber provider, if so we can
        // continue setup.
        imProvider = AccountUtils.getImProvider();
        if (imProvider != null)
        {
            handleProviderAdded(imProvider);
        }
    }

    /**
     * Removes listeners for the jabber provider registering/unregistering.
     */
    protected void removeSmsListeners()
    {
        sLog.info("Removing SMS listeners from " + this);
        GuiActivator.bundleContext.removeServiceListener(this);

        if (imProvider != null)
        {
            imProvider.removeRegistrationStateChangeListener(this);
        }
    }

    /**
     * Run when the jabber provider is registered.  Adds a registration state
     * listener to the jabber provider so we can add/remove SMS chat transports
     * when the jabber account registers/unregisters.
     *
     * @param pps the jabber provider that has registered.
     */
    private void handleProviderAdded(ProtocolProviderService pps)
    {
        sLog.info("Jabber provider registered: " + pps);
        imProvider = pps;
        imProvider.addRegistrationStateChangeListener(this);

        // If the jabber account is already registered, we can continue to add
        // SMS transports.
        if (imProvider.isRegistered())
        {
            handleAccountRegistered();
        }
    }

    /**
     * Run when the jabber provider is unregistering.  Removes the registration
     * state listener from the jabber provider and calls the method to remove
     * SMS chat transports.
     *
     * @param pps the jabber provider that is unregistering.
     */
    private void handleProviderRemoved(ProtocolProviderService pps)
    {
        sLog.info("Jabber provider unregistering: " + pps);
        imProvider = null;
        pps.removeRegistrationStateChangeListener(this);
        handleAccountUnregistered();
    }

    /**
     * Run when the jabber account registers and adds SMSChatTransports to the
     * session.
     */
    private void handleAccountRegistered()
    {
        sLog.info("Jabber account registered");
        addSMSChatTransports();

        // As we are adding the chat transports after the provider has
        // registered, we need to make sure we select an up-to-date chat
        // transport instance.  Otherwise, the chat transport instance that was
        // selected before we lost connection may still be selected. That
        // transport will be using the old instance of the IM op set so sending
        // messages via that transport will fail.
        selectMostRecentChatTransport();
    }

    /**
     * Run when the jabber account unregisters and removes SMSChatTransports
     * from the session.
     */
    private void handleAccountUnregistered()
    {
        sLog.info("Jabber account unregistered");
        removeSMSChatTransports();
    }

    @Override
    public void serviceChanged(ServiceEvent evt)
    {
        ServiceReference<?> serviceRef = evt.getServiceReference();

        // If the event is caused by a bundle being stopped, we don't care.
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            return;

        Object service = GuiActivator.bundleContext.getService(serviceRef);

        // We don't care if the source service is not a protocol provider.
        if (!(service instanceof ProtocolProviderService))
            return;

        ProtocolProviderService pps = (ProtocolProviderService) service;

        if (ProtocolNames.JABBER.equals(pps.getProtocolName()))
        {
            int eventType = evt.getType();

            if (ServiceEvent.REGISTERED == eventType)
            {
                sLog.info("Provider registered");
                handleProviderAdded(pps);
            }
            else if (ServiceEvent.UNREGISTERING == eventType)
            {
                sLog.info("Provider unregistering");
                handleProviderRemoved(pps);
            }
        }
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState newState = evt.getNewState();
        sLog.debug("Jabber registration state changed: " + newState);

        if (RegistrationState.REGISTERED.equals(newState))
        {
            handleAccountRegistered();
        }
        else if (RegistrationState.UNREGISTERED.equals(newState) ||
                 RegistrationState.CONNECTION_FAILED.equals(newState))
        {
            handleAccountUnregistered();
        }
    }

    /**
     * Returns the avatar icon of this chat session.
     *
     * @return the avatar icon of this chat session.
     */
    public abstract BufferedImageFuture getChatAvatar();

    /**
     * Gets the indicator which determines whether a contact list of (multiple)
     * participants is supported by this <code>ChatSession</code>. For example,
     * UI implementations may use the indicator to determine whether UI elements
     * should be created for the user to represent the contact list of the
     * participants in this <code>ChatSession</code>.
     *
     * @return <tt>true</tt> if this <code>ChatSession</code> supports a contact
     *         list of (multiple) participants; otherwise, <tt>false</tt>
     */
    public abstract boolean isContactListSupported();

    /**
     * Adds the given {@link ChatSessionChangeListener} to this
     * <tt>ChatSession</tt>.
     *
     * @param l the <tt>ChatSessionChangeListener</tt> to add
     */
    public abstract void addChatTransportChangeListener(
        ChatSessionChangeListener l);

    /**
     * Removes the given {@link ChatSessionChangeListener} to this
     * <tt>ChatSession</tt>.
     *
     * @param l the <tt>ChatSessionChangeListener</tt> to add
     */
    public abstract void removeChatTransportChangeListener(
        ChatSessionChangeListener l);

    @Override
    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {}

    @Override
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {}

    @Override
    public void metaContactGroupRemoved(MetaContactGroupEvent evt)
    {}

    @Override
    public void metaContactModified(MetaContactModifiedEvent evt)
    {}

    @Override
    public void metaContactMoved(MetaContactMovedEvent evt)
    {}

    @Override
    public void metaContactRemoved(MetaContactEvent evt)
    {}

    @Override
    public void metaContactAvatarUpdated(MetaContactAvatarUpdateEvent evt)
    {}

    @Override
    public void metaContactAdded(MetaContactEvent evt)
    {}

    @Override
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {}

    @Override
    public void protoContactAdded(ProtoContactEvent evt)
    {}

    @Override
    public void protoContactModified(ProtoContactEvent evt)
    {}

    @Override
    public void protoContactRemoved(ProtoContactEvent evt)
    {}

    @Override
    public void protoContactMoved(ProtoContactEvent evt)
    {}

    @Override
    public void childContactsReordered(MetaContactGroupEvent evt)
    {}
}
