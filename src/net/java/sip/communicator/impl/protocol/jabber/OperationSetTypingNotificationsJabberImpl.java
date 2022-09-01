/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import static org.jitsi.util.Hasher.logHasher;

import com.google.common.annotations.VisibleForTesting;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateListener;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.xevent.MessageEventManager;
import org.jivesoftware.smackx.xevent.MessageEventNotificationListener;
import org.jivesoftware.smackx.xevent.MessageEventRequestListener;
import org.jxmpp.jid.Jid;

import net.java.sip.communicator.service.protocol.AbstractOperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactResource;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.protocol.jabberconstants.JabberStatusEnum;
import net.java.sip.communicator.util.Logger;

/**
 * Maps SIP Communicator typing notifications to those going and coming from
 * smack lib.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class OperationSetTypingNotificationsJabberImpl
    extends AbstractOperationSetTypingNotifications<ProtocolProviderServiceJabberImpl>
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetTypingNotificationsJabberImpl.class);

    /**
     * An active instance of the opSetPersPresence operation set. We're using
     * it to map incoming events to contacts in our contact list.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * We use this listener to cease the moment when the protocol provider
     * has been successfully registered.
     */
    private ProviderRegListener providerRegListener = new ProviderRegListener();

    /**
     * The manger which send us the typing info and through which we send inf
     */
    private MessageEventManager messageEventManager = null;

    /**
     * Listens for incoming request for typing info
     */
    private JabberMessageEventRequestListener jabberMessageEventRequestListener = null;

    /**
     * Receives incoming typing info
     */
    private IncomingMessageEventsListener incomingMessageEventsListener = null;

//    private final Map<String, String> packetIDsTable
//        = new Hashtable<String, String>();

    /**
     * The listener instance that we use to track chat states according to
     * XEP-0085;
     */
    private SmackChatStateListener smackChatStateListener = null;

    /**
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetTypingNotificationsJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        super(provider);

        provider.addRegistrationStateChangeListener(providerRegListener);
    }

    /**
     * Sends a notification to <tt>notifiedContatct</tt> that we have entered
     * <tt>typingState</tt>.
     *
     * @param notifiedContact the <tt>Contact</tt> to notify
     * @param typingState the typing state that we have entered.
     *
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>notifiedContact</tt> is
     * not an instance belonging to the underlying implementation.
     */
    public void sendTypingNotification(Contact notifiedContact, TypingState typingState)
        throws IllegalStateException, IllegalArgumentException
    {
        sendTypingNotification(notifiedContact, null, typingState);
    }

    /**
     * Sends a notification to the specified <tt>ContatctResource</tt> for the
     * <tt>notifiedContatct</tt> that we have entered <tt>typingState</tt>.
     * @param notifiedContact the <tt>Contact</tt> to notify
     * @param resource the <tt>ContatctResource</tt> (if null, the
     * notification is sent to all of the contact's registered resources).
     * @param typingState the typing state that we have entered.
     *
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>notifiedContact</tt> is
     * not an instance belonging to the underlying implementation.
     */
    @Override
    public void sendTypingNotification(Contact notifiedContact,
                                       ContactResource resource,
                                       TypingState typingState)
        throws IllegalStateException, IllegalArgumentException
    {
        assertConnected();

        if( !(notifiedContact instanceof ContactJabberImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not a Jabber contact."
               + notifiedContact);

        /**
         * Emil Ivov: We used to use this in while we were still using XEP-0022
         * to send typing notifications. I am commenting it out today on
         * 2008-08-20 as we now also support XEP-0085 (see below) and using both
         * mechanisms sends double notifications which, apart from simply being
         * redundant, is also causing the jabber slick to fail.
         *
        String packetID =
            (String)packetIDsTable.get(notifiedContact.getAddress());

        //First do XEP-0022 notifications
        if(packetID != null)
        {
            if(typingState == STATE_TYPING)
            {
                messageEventManager.
                    sendComposingNotification(notifiedContact.getAddress(),
                                              packetID);
            }
            else if(typingState == STATE_STOPPED)
            {
                messageEventManager.
                    sendCancelledNotification(notifiedContact.getAddress(),
                                              packetID);
                packetIDsTable.remove(notifiedContact.getAddress());
            }
        }
        */

        //now handle XEP-0085
        sendXep85ChatState(notifiedContact, resource, typingState);
    }

    /**
     * Converts <tt>state</tt> into the corresponding smack <tt>ChatState</tt>
     * and sends it to contact.
     *
     * @param contact the contact that we'd like to send our state to.
     * @param resource the contact's resource that we'd like to send our state
     * to. If null, the state will be sent to all of the contact's registered
     * resources.
     * @param state the state we'd like to sent.
     */
    private void sendXep85ChatState(Contact contact,
                                    ContactResource resource,
                                    TypingState state)
    {
        Jid jid = (resource == null) ?
                            ((ContactJabberImpl)contact).getAddressAsJid() :
                            ((ContactResourceJabberImpl) resource).getFullJid();

        logger.debug("Sending XEP-0085 chat state=" + state
            + " to " + logHasher(jid));

        Chat chat = ChatManager.getInstanceFor(
            parentProvider.getConnection())
                          .chatWith(jid.asEntityBareJidIfPossible());

        ChatState chatState = typingStateToChatState(state);

        try
        {
            ChatStateManager.getInstance(parentProvider.getConnection())
                .setCurrentState(chatState, chat);
        }
        catch(NotConnectedException | InterruptedException exc)
        {
            //we don't want to bother the user with network exceptions
            //so let's simply log it.
            logger.warn("Failed to send state [" + state + "] to ["
                + logHasher(contact.getAddress()) + "].", exc);
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws java.lang.IllegalStateException
     *             if the underlying stack is not registered and initialized.
     */
    protected void assertConnected()
        throws IllegalStateException
    {
        if(parentProvider != null && !parentProvider.isRegistered()
            && opSetPersPresence.getPresenceStatus().isOnline())
        {
            // if we are not registered but the current status is online
            // change the current status
            opSetPersPresence.fireProviderStatusChangeEvent(
                    opSetPersPresence.getPresenceStatus(),
                    parentProvider.getJabberStatusEnum().getStatus(
                        JabberStatusEnum.OFFLINE_STATUS));
        }

        super.assertConnected();
    }

    /**
     * Our listener that will tell us when we're registered and
     * ready to accept us as a listener.
     */
    private class ProviderRegListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("The provider changed state from: "
                     + evt.getOldState()
                     + " to: " + evt.getNewState());
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence =
                    (OperationSetPersistentPresenceJabberImpl) parentProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

                messageEventManager =
                    MessageEventManager.getInstanceFor(parentProvider.getConnection());

                if(jabberMessageEventRequestListener == null)
                    jabberMessageEventRequestListener = new JabberMessageEventRequestListener();

                if(incomingMessageEventsListener == null)
                    incomingMessageEventsListener = new IncomingMessageEventsListener();

                messageEventManager.addMessageEventRequestListener(
                    jabberMessageEventRequestListener);
                messageEventManager.addMessageEventNotificationListener(
                    incomingMessageEventsListener);

                //according to the smack api documentation we need to do this
                //every time we connect in order to reinitialize the chat state
                //manager (@see http://tinyurl.com/6j9uqs)
                if(smackChatStateListener == null)
                    smackChatStateListener = new SmackChatStateListener();

                ChatStateManager.getInstance(parentProvider.getConnection())
                                .addChatStateListener(smackChatStateListener);
            }
            else if(evt.getNewState() == RegistrationState.UNREGISTERED
                 || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
                 || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                if(parentProvider.getConnection() != null
                    && ChatStateManager.getInstance(parentProvider.getConnection()) != null)
                {
                    ChatStateManager.getInstance(parentProvider.getConnection())
                        .removeChatStateListener(smackChatStateListener);
                }

                smackChatStateListener = null;

                if (messageEventManager != null)
                {
                    messageEventManager.removeMessageEventRequestListener(
                        jabberMessageEventRequestListener);
                    messageEventManager.removeMessageEventNotificationListener(
                        incomingMessageEventsListener);
                }

                jabberMessageEventRequestListener = null;
                incomingMessageEventsListener = null;
            }
        }
    }

    /**
     * Listens for incoming request for typing info
     */
    private class JabberMessageEventRequestListener
        implements MessageEventRequestListener
    {
        public void deliveredNotificationRequested(Jid from, String packetID,
            MessageEventManager messageEventManager)
        {
            try
            {
                messageEventManager.sendDeliveredNotification(from, packetID);
            }
            catch (NotConnectedException | InterruptedException e)
            {
                logger.warn("Failed to send delivered notification.", e);
            }
        }

        public void displayedNotificationRequested(Jid from, String packetID,
            MessageEventManager messageEventManager)
        {
            try
            {
                messageEventManager.sendDisplayedNotification(from, packetID);
            }
            catch (NotConnectedException | InterruptedException e)
            {
                logger.warn("Failed to send displayed notification.", e);
            }
        }

        public void composingNotificationRequested(Jid from, String packetID,
            MessageEventManager messageEventManager)
        {
//            if(packetID != null)
//            {
//                String fromID = StringUtils.parseBareAddress(from);
//                packetIDsTable.put(fromID, packetID);
//            }
        }

        public void offlineNotificationRequested(Jid from, String packetID,
                                                 MessageEventManager
                                                 messageEventManager)
        {}
    }

    /**
     * Receives incoming typing info
     */
    private class IncomingMessageEventsListener
        implements MessageEventNotificationListener
    {
        public void deliveredNotification(Jid from, String packetID)
        {
        }

        public void displayedNotification(Jid from, String packetID)
        {
        }

        public void composingNotification(Jid from, String packetID)
        {
            String fromID =
                (from.asEntityBareJidIfPossible() != null) ?
                    from.asEntityBareJidIfPossible().toString() : "";
            logger.info("Composing notification from ID: " + fromID);

            Contact sourceContact = opSetPersPresence.findContactByID(fromID);

            if(sourceContact == null)
            {
                logger.debug("sourceContact is null, Creating volatile contact");
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(fromID);
                logger.debug("volatile contact created");
            }
            logger.debug("firing a TYPING notification");
            fireTypingNotificationsEvent(sourceContact, TypingState.TYPING);
        }

        public void offlineNotification(Jid from, String packetID)
        {
        }

        public void cancelledNotification(Jid from, String packetID)
        {
            String fromID =
                (from.asEntityBareJidIfPossible() != null) ?
                    from.asEntityBareJidIfPossible().toString() : "";
            logger.info("Notification cancelled from ID: " + fromID);

            Contact sourceContact = opSetPersPresence.findContactByID(fromID);

            if(sourceContact == null)
            {
                logger.debug("sourceContact is null, Creating volatile contact");
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(fromID);
                logger.debug("volatile contact created");
            }
            logger.debug("firing a NOT_TYPING notification");
            fireTypingNotificationsEvent(sourceContact, TypingState.NOT_TYPING);
        }
    }

    /**
     * The listener that we use to track chat state notifications according
     * to XEP-0085.
     */
    private class SmackChatStateListener
        implements ChatStateListener
    {
        /**
         * Called by smack when the state of a chat changes.
         *
         * @param chat the chat that is concerned by this event.
         * @param state the new state of the chat.
         */
        public void stateChanged(Chat chat,
                                 ChatState state,
                                 org.jivesoftware.smack.packet.Message message)
        {
            logger.debug(logHasher(chat.getXmppAddressOfChatPartner().toString())
                + " entered the " + state.name() + " state.");

            String fromID =
                chat.getXmppAddressOfChatPartner().asBareJid().toString();

            // Don't send typing notifications for SMS messages.
            if (fromID.contains("@sms."))
                return;

            Contact sourceContact = opSetPersPresence.findContactByID(fromID);

            if(sourceContact == null)
            {
                //create the volatile contact
                sourceContact = opSetPersPresence.createVolatileContact(fromID);
            }

            TypingState typingState = chatStateToTypingState(state);

            if (typingState != TypingState.UNKNOWN)
            {
                fireTypingNotificationsEvent(sourceContact, typingState);
            }
            else
            {
                logger.debug("Ignoring typing state: " + state);
            }
        }
    }

    @VisibleForTesting
    static TypingState chatStateToTypingState(ChatState chatState)
    {
        TypingState typingState = TypingState.UNKNOWN;

        if (ChatState.composing.equals(chatState))
        {
            typingState = TypingState.TYPING;
        }
        else if (ChatState.paused.equals(chatState))
        {
            typingState = TypingState.PAUSED;
        }
        else if (ChatState.inactive.equals(chatState) ||
                 ChatState.active.equals(chatState) ||
                 ChatState.gone.equals(chatState))
        {
            // We only use chat state to indicate if the user is typing/paused/neither - so
            // interpret any other value as not typing.
            typingState = TypingState.NOT_TYPING;
        }

        return typingState;
    }

    @VisibleForTesting
    static ChatState typingStateToChatState(TypingState typingState)
    {
        ChatState chatState = ChatState.gone;

        if (typingState == TypingState.TYPING)
        {
            chatState = ChatState.composing;
        }
        else if (typingState == TypingState.NOT_TYPING)
        {
            // No MaX clients differentiate between active/inactive/gone chat states, so it doesn't
            // really matter which of those we use to interpret our internal 'not typing' state,
            // but the 'active' state seems the best fit because we go into state NOT_TYPING when
            // opening a chat window/after sending a chat etc.
            chatState = ChatState.active;
        }
        else if (typingState == TypingState.PAUSED)
        {
            chatState = ChatState.paused;
        }

        return chatState;
    }
}
