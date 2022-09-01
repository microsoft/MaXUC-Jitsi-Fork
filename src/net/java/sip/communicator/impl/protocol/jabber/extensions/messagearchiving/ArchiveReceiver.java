// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber.extensions.messagearchiving;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.archive.ArchiveCompleteIQ;
import org.jivesoftware.smackx.archive.ArchivedMessageExtensionElement;
import org.jivesoftware.smackx.archive.MessageArchivedExtensionElement;

import net.java.sip.communicator.impl.protocol.jabber.JabberActivator;
import net.java.sip.communicator.impl.protocol.jabber.OperationSetBasicInstantMessagingJabberImpl;
import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.util.Logger;

/**
 * The ArchiveReceiver gets archive messages and passes them on to the message
 * handler in the OperationSetBasicImJabberImpl class
 */
public class ArchiveReceiver implements RegistrationStateChangeListener,
                                        StanzaListener,
                                        StanzaFilter
{
    private static final Logger sLog = Logger.getLogger(ArchiveReceiver.class);

    /**
     * Boolean which indicates whether we got archive messages on the last
     * archive query
     */
    private final AtomicBoolean mGotArchiveMessages = new AtomicBoolean();

    /**
     * Jabber provider that controls the XMPP connection
     */
    private final ProtocolProviderServiceJabberImpl mJabberProvider;

    /**
     * The IM operation set that normally handles messages
     */
    private final OperationSetBasicInstantMessagingJabberImpl mImOpSet;

    /**
     * Object used to request the archive
     */
    private final ArchivePoller mArchivePoller;

    /**
     * A count of the number of messages that we've received.  Just used for
     * logging purposes.
     */
    private final AtomicInteger mMessageReceivedCount = new AtomicInteger();

    public ArchiveReceiver(ProtocolProviderServiceJabberImpl jabberProvider,
                           ArchivePoller archivePoller)
    {
        sLog.info("Created ArchiveReceiver " + this);
        mJabberProvider = jabberProvider;
        mArchivePoller = archivePoller;
        mImOpSet = (OperationSetBasicInstantMessagingJabberImpl)
                       mJabberProvider.getOperationSet(
                           OperationSetBasicInstantMessaging.class);

        synchronized (this)
        {
            mJabberProvider.addRegistrationStateChangeListener(this);

            if (mJabberProvider.isRegistered())
            {
                mJabberProvider.getConnection().removeStanzaListener(this);
                mJabberProvider.getConnection().addStanzaListener(this, this);
            }
        }
    }

    @Override
    public synchronized void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState newState = evt.getNewState();
        sLog.debug("Registration state changed " + newState);

        if (RegistrationState.REGISTERED.equals(newState))
        {
            mJabberProvider.getConnection().removeStanzaListener(this);
            mJabberProvider.getConnection().addStanzaListener(this, this);
        }
    }

    @Override
    public void processStanza(Stanza stanza)
    {
        if (stanza instanceof ArchiveCompleteIQ)
        {
            ArchiveCompleteIQ iq = (ArchiveCompleteIQ) stanza;
            String last = iq.getLast();
            int count = mMessageReceivedCount.getAndSet(0);
            sLog.debug("Query complete, last: " + last + ", count: " + count);

            if (last != null)
            {
                // Last will only exist if we actually got some archive messages.
                mGotArchiveMessages.set(true);
                storeLast(last);

                // If we're still getting messages, then we should carry on and
                // see if there are any more.
                mArchivePoller.onQueryPartComplete();
            }
            else
            {
                if (mGotArchiveMessages.get())
                {
                    // We've finished getting the archive, and have sent some
                    // archive messages.  Thus the UI might be out of date.
                    JabberActivator.getUIService().reloadContactList();
                    mGotArchiveMessages.set(false);
                }

                // Inform the poller that we've finished getting the archive
                mArchivePoller.onQueryComplete();
            }
        }
        else if (stanza instanceof Message)
        {
            Message message = (Message) stanza;

            // This is a message - it's either an archive message, or a message
            // that has been archived
            MessageArchivedExtensionElement maee = findMAEE(message);

            if (maee != null)
            {
                // This message has an "archived" element, thus is a normal,
                // archived message
                if (mArchivePoller.isOnlyUser())
                {
                    // This is the only user logged in, thus store the id.
                    String last = maee.getArchivedId();
                    storeLast(last);
                }
            }
            else
            {
                // This message must be an archive message
                ArchivedMessageExtensionElement amee = findAMEE(message);
                Message archivedMessage = amee.getMessage();
                mMessageReceivedCount.incrementAndGet();

                // New, previously unseen message.  Send it to the im opset
                Date date = amee.getDate();
                mImOpSet.handleMessage(archivedMessage, date);
            }
        }
    }

    /**
     * Store the id of the last message received
     *
     * @param last the archive id of the last message received
     */
    private void storeLast(String last)
    {
        AccountID accountID = mJabberProvider.getAccountID();

        // putAccountProperty updates the in memory copy.  For this to be
        // persisted, we need to call AccountManager.storeAccount
        accountID.putAccountProperty("lastArchiveId", last);

        try
        {
            AccountManager manager = JabberActivator.getAccountManager();
            manager.storeAccount(JabberActivator.getProtocolProviderFactory(),
                                 accountID);
        }
        catch (OperationFailedException e)
        {
            sLog.error("Unable to store last " + last, e);
        }
    }

    @Override
    public boolean accept(Stanza stanza)
    {
        boolean accept;

        // Only want messages and ArchiveCompleteIQ packets
        if (stanza instanceof Message)
        {
            Message message = (Message) stanza;

            // Only want messages that have either an archived extension, or a
            // message extension
            ArchivedMessageExtensionElement amee = findAMEE(message);

            if (amee != null)
            {
                // This is an archived message, ignore it if it is a group
                // chat message - those are handled elsewhere, unless this is
                // the first time we're querying the archive - in which case
                // group chat join and leave messages need to be passed on to
                // the chat room manager.
                Message archivedMessage = amee.getMessage();
                accept = !archivedMessage.getType().equals(Type.groupchat) ||
                         !mArchivePoller.isFirstRequestComplete();
            }
            else
            {
                // Not an archived message - only interested if it has an archived
                // extension.
                accept = findMAEE(message) != null;
            }
        }
        else if (stanza instanceof ArchiveCompleteIQ)
        {
            accept = true;
        }
        else
        {
            accept = false;
        }

        return accept;
    }

    /**
     * Find the MessageArchivedExtensionElement in a message
     *
     * @param msg the message to look for the extension in
     * @return the MessageArchivedExtensionElement or null if not found
     */
    private MessageArchivedExtensionElement findMAEE(Message msg)
    {
        Object ext = msg.getExtensionElement(
            MessageArchivedExtensionElement.ELEMENT_NAME,
            MessageArchivedExtensionElement.NAMESPACE);

        MessageArchivedExtensionElement maee = null;

        if (ext instanceof MessageArchivedExtensionElement)
        {
            maee = (MessageArchivedExtensionElement) ext;
        }

        return maee;
    }

    /**
     * Find the ArchivedMessageExtensionElement in a message
     *
     * @param msg the message to look for the extension in
     * @return the ArchivedMessageExtensionElement or null if not found
     */
    private ArchivedMessageExtensionElement findAMEE(Message msg)
    {
        Object ext = msg.getExtensionElement(
            ArchivedMessageExtensionElement.ELEMENT_NAME,
            ArchivedMessageExtensionElement.NAMESPACE);

        ArchivedMessageExtensionElement amee = null;

        if (ext instanceof ArchivedMessageExtensionElement)
        {
            amee = (ArchivedMessageExtensionElement) ext;
        }

        return amee;
    }

    /**
     * Stop this receiver, removing any registered listeners or filters.
     */
    public void stop()
    {
        sLog.info("Stopping ArchiveReceiver " + this);
        mJabberProvider.removeRegistrationStateChangeListener(this);

        XMPPConnection connection = mJabberProvider.getConnection();

        if (connection != null)
        {
            connection.removeStanzaListener(this);
        }
    }
}
