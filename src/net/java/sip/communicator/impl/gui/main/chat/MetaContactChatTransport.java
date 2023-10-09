/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactResource;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetContactCapabilities;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusListener;
import net.java.sip.communicator.util.Logger;

/**
 * The single chat implementation of the <tt>ChatTransport</tt> interface that
 * provides abstraction to protocol provider access.
 *
 * @author Yana Stamcheva
 */
public class MetaContactChatTransport
    implements  ChatTransport,
                ContactPresenceStatusListener
{
    /**
     * The logger.
     */
    private static final Logger sLog =
        Logger.getLogger(MetaContactChatTransport.class);

    /**
     * The parent <tt>ChatSession</tt>, where this transport is available.
     */
    private final MetaContactChatSession mParentChatSession;

    /**
     * The associated protocol <tt>Contact</tt>.
     */
    private final Contact mContact;

    /**
     * The resource associated with this contact.
     */
    private ContactResource mContactResource;

    /**
     * The protocol presence operation set associated with this transport.
     */
    private final OperationSetPresence mPresenceOpSet;

    /**
     * Creates an instance of <tt>MetaContactChatTransport</tt> by specifying
     * the parent <tt>chatSession</tt> and the <tt>contact</tt> associated with
     * the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param contact the <tt>Contact</tt> associated with this transport
     */
    public MetaContactChatTransport(MetaContactChatSession chatSession,
                                    Contact contact)
    {
        this(chatSession, contact, null);
    }

    /**
     * Creates an instance of <tt>MetaContactChatTransport</tt> by specifying
     * the parent <tt>chatSession</tt> and the <tt>contact</tt> associated with
     * the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param contact the <tt>Contact</tt> associated with this transport
     * @param contactResource the <tt>ContactResource</tt> associated with the
     * contact
     */
    public MetaContactChatTransport(MetaContactChatSession chatSession,
                                    Contact contact,
                                    ContactResource contactResource)
    {
        mParentChatSession = chatSession;
        mContact = contact;
        mContactResource = contactResource;

        mPresenceOpSet = mContact.getProtocolProvider()
            .getOperationSet(OperationSetPresence.class);

        if (mPresenceOpSet != null)
            mPresenceOpSet.addContactPresenceStatusListener(this);
    }

    /**
     * Returns the contact associated with this transport.
     *
     * @return the contact associated with this transport
     */
    public Contact getContact()
    {
        return mContact;
    }

    /**
     * Returns the contact address corresponding to this chat transport.
     *
     * @return The contact address corresponding to this chat transport.
     */
    public String getName()
    {
        return mContact.getAddress();
    }

    /**
     * Returns the display name corresponding to this chat transport.
     *
     * @return The display name corresponding to this chat transport.
     */
    public String getDisplayName()
    {
        return mContact.getDisplayName();
    }

    /**
     * Returns the resource name of this chat transport. This is for example the
     * name of the user agent from which the contact is logged.
     *
     * @return The display name of this chat transport resource.
     */
    public String getResourceName()
    {
        if (mContactResource != null)
            return mContactResource.getResourceName();

        return null;
    }

    /**
     * Returns the presence status of this transport.
     *
     * @return the presence status of this transport.
     */
    public PresenceStatus getStatus()
    {
        if (mContactResource != null)
            return mContactResource.getPresenceStatus();
        else
            return mContact.getPresenceStatus();
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     *
     * @return the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return mContact.getProtocolProvider();
    }

    /**
     * Returns <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     */
    public boolean allowsInstantMessage()
    {
        // First try to ask the capabilities operation set if such is
        // available.
        OperationSetContactCapabilities capOpSet = getProtocolProvider()
            .getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet != null)
        {
            if (capOpSet.getOperationSet(
                mContact, OperationSetBasicInstantMessaging.class) != null)
            {
                return true;
            }
        }
        else if (mContact.getProtocolProvider()
            .getOperationSet(OperationSetBasicInstantMessaging.class) != null)
            return true;

        return false;
    }

    /**
     * Returns <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>.
     */
    public boolean allowsSmsMessage()
    {
        return false;
    }

    /**
     * Returns <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>.
     */
    public boolean allowsTypingNotifications()
    {
        return mContact.getProtocolProvider().getOperationSet(OperationSetTypingNotifications.class) != null;
    }

    /**
     * Sends a typing notification state.
     *
     * @param typingState the typing notification state to send
     */
    public void sendTypingNotification(TypingState typingState)
    {
        // If this chat transport does not support typing notifications we do
        // nothing here.
        if (!allowsTypingNotifications())
        {
            return;
        }

        ProtocolProviderService protocolProvider = mContact.getProtocolProvider();
        OperationSetTypingNotifications tnOperationSet
            = protocolProvider.getOperationSet(OperationSetTypingNotifications.class);

        // if protocol is not registered or contact is offline don't
        // try to send typing notifications
        if (protocolProvider.isRegistered() &&
            mContact.getPresenceStatus().getStatus() >= PresenceStatus.ONLINE_THRESHOLD)
        {
            try
            {
                tnOperationSet.sendTypingNotification(mContact, mContactResource, typingState);
            }
            catch (Exception ex)
            {
                sLog.error("Failed to send typing notifications.", ex);
            }
        }
    }

    public void inviteChatContact(String contactAddress, String reason) {}

    /**
     * Indicates that a contact has changed its status.
     * @param evt The presence event containing information about the
     * contact status change.
     */
    public void contactPresenceStatusChanged(
                                        ContactPresenceStatusChangeEvent evt)
    {
        if (evt.getSourceContact().equals(mContact))
        {
            updateContactStatus();
        }
    }

    /**
     * Updates the status of this contact with the new given status.
     */
    private void updateContactStatus()
    {
        // Update the status of the given contact in the "send via" selector
        // box.
        mParentChatSession.getChatSessionRenderer()
            .updateChatTransportStatus(this);
    }

    /**
     * Removes all previously added listeners.
     */
    public void dispose()
    {
        if (mPresenceOpSet != null)
            mPresenceOpSet.removeContactPresenceStatusListener(this);
    }

    /**
     * Returns the descriptor of this chat transport.
     * @return the descriptor of this chat transport
     */
    public Object getDescriptor()
    {
        return mContact;
    }

    @Override
    public String toString()
    {
        return "<" + super.toString() + ", Contact = " + mContact +
               ", Resource = " + mContactResource + ">";
    }
}
