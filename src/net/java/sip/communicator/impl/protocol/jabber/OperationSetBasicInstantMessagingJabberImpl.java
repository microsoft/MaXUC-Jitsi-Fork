/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.impl.protocol.jabber.OperationSetPersistentPresenceJabberImpl.EXT_CLIENT_RESOURCE;
import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.Hasher.logHasher;
import static org.jivesoftware.smack.filter.MessageTypeFilter.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.archive.ArchivedMessageExtensionElement;
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation;
import org.jivesoftware.smackx.xevent.MessageEventManager;
import org.jivesoftware.smackx.xhtmlim.XHTMLManager;
import org.jivesoftware.smackx.xhtmlim.XHTMLText;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.delay.DelayInformationManager;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.impl.protocol.jabber.JabberActivator.GroupMembershipAction;
import net.java.sip.communicator.impl.protocol.jabber.extensions.messagecorrection.MessageCorrectionExtension;
import net.java.sip.communicator.impl.protocol.jabber.extensions.messagecorrection.MessageCorrectionExtensionProvider;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.phonenumberutils.PhoneNumberUtilsService;
import net.java.sip.communicator.service.protocol.AbstractOperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactResource;
import net.java.sip.communicator.service.protocol.ImMessage;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.SubscriptionStatus;
import net.java.sip.communicator.service.protocol.OperationSetMessageCorrection;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetSpecialMessaging;
import net.java.sip.communicator.service.protocol.OperationSetSpecialMessaging.SpecialMessageHandler;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageEvent;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.threading.CancellableRunnable;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Html2Text;
import net.java.sip.communicator.util.JitsiStringUtils;
import net.java.sip.communicator.util.Logger;

/**
 * A straightforward implementation of the basic instant messaging operation
 * set.
 *
 * @author Damian Minkov
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 * @author Emil Ivov
 */
public class OperationSetBasicInstantMessagingJabberImpl
    extends AbstractOperationSetBasicInstantMessaging
    implements OperationSetMessageCorrection, StanzaListener
{
    /**
     * Our class logger
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetBasicInstantMessagingJabberImpl.class);

    /**
     * A table mapping contact addresses to full jids that can be used to
     * target a specific resource (rather than sending a message to all logged
     * instances of a user).
     */
    private final Map<Jid, TargetAddress> jids = new Hashtable<>();

    /**
     * The smackMessageListener instance listens for incoming messages.
     * Keep a reference of it so if anything goes wrong we don't add
     * two different instances.
     */
    private SmackMessageListener smackMessageListener = null;

    /**
     * Contains the complete jid of a specific user and the time that it was
     * last used so that we could remove it after a certain point.
     */
    private class TargetAddress
    {
        /** The last complete JID (including resource) that we got a msg from*/
        EntityFullJid jid;

        /** The time that we last sent or received a message from this jid */
        long lastUpdatedTime;
    }

    /**
     * The number of milliseconds that we preserve threads with no traffic
     * before considering them dead.
     */
    private static final long JID_INACTIVITY_TIMEOUT = 10*60*1000;//10 min.

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * The chat room manager, responsible for joining and leaving chat rooms.
     */
    private final ChatRoomManager chatRoomManager;

    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * A reference to the special instant messaging operation set.
     */
    private OperationSetSpecialMessaging opSetSpecialMsg = null;

    /**
     * The html namespace used as feature
     * XHTMLManager.namespace
     */
    private static final String HTML_NAMESPACE =
        "http://jabber.org/protocol/xhtml-im";

    /**
     * Config key for the IM domain
     */
    private static final String IM_DOMAIN_PROP =
                                       "net.java.sip.communicator.im.IM_DOMAIN";

    /**
     * The SMS domain
     */
    private String smsDomain;

    /**
    * A map of fragmented message IDs to FragmentedMessage objects.  A
    * LinkedHashMap is used to ensure the order in which the fragmented
    * messages were received is preserved.
    */
   private final Map<String, FragmentedMessage> fragmentedMessages =
            new LinkedHashMap<>();

    /**
     * Creates an instance of this operation set.
     * @param provider a reference to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetBasicInstantMessagingJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        jabberProvider = provider;
        provider.addRegistrationStateChangeListener(
                        new RegistrationStateListener());

        MessageCorrectionExtensionProvider extProvider =
                new MessageCorrectionExtensionProvider();
        ProviderManager.addExtensionProvider(
                MessageCorrectionExtension.ELEMENT_NAME,
                MessageCorrectionExtension.NAMESPACE,
                extProvider);

        chatRoomManager = jabberProvider.getChatRoomManager();

        // Msw Customisation
        // If we're using CommPortal IM, determine the SMS domain in case the
        // user wants to send SMSs.
        if ("CommPortal".equals(ConfigurationUtils.getImProvSource()))
        {
            String imDomain = JabberActivator.getConfigurationService().
                                             user().getString(IM_DOMAIN_PROP);

            if (imDomain != null)
            {
                // Remove whitespace and any '@' characters that the service
                // provider may have included in the IM domain in SIP PS. Then
                // prepend '@sms.' to it to create the SMS domain.
                String trimmedDomain = imDomain.trim().replace("@","");
                if (trimmedDomain.length() != 0)
                {
                    smsDomain = "@sms." + trimmedDomain;
                    logger.info("Got SMS domain: " + smsDomain);
                }
            }
        }
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @return the newly created message.
     */
    public ImMessage createMessage(String content, String contentType)
    {
        return createMessage(content, contentType, DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param encoding the encoding of the message that we will be sending.
     * @param subject the Subject of the message that we'd like to create.
     *
     * @return the newly created message.
     */
    public ImMessage createMessage(String content, String contentType,
        String encoding, String subject)
    {
        return new MessageJabberImpl(content, contentType, encoding, subject, false, false, false);
    }

    /**
     * Create a Message instance
     *
     * @param content the contents of the message
     * @param contentType the MIME-type for the content
     * @param messageUID the UID of the message
     * @param isArchive true if this message is an archive message
     * @param isOffline true if this message is an offline message
     * @param isCarbon  true if this message is a carbon message
     * @return the created message
     */
    private ImMessage createMessage(String content,
                                  String contentType,
                                  String messageUID,
                                  boolean isArchive,
                                  boolean isOffline,
                                  boolean isCarbon)
    {
        return new MessageJabberImpl(content,
                                     contentType,
                                     DEFAULT_MIME_ENCODING,
                                     null,
                                     messageUID,
                                     isArchive,
                                     isOffline,
                                     isCarbon);
    }

    /**
     * Determines whether the protocol provider (or the protocol itself) support
     * sending and receiving offline messages. Most often this method would
     * return true for protocols that support offline messages and false for
     * those that don't. It is however possible for a protocol to support these
     * messages and yet have a particular account that does not (i.e. feature
     * not enabled on the protocol server). In cases like this it is possible
     * for this method to return true even when offline messaging is not
     * supported, and then have the sendMessage method throw an
     * OperationFailedException with code - OFFLINE_MESSAGES_NOT_SUPPORTED.
     *
     * @return <tt>true</tt> if the protocol supports offline messages and
     * <tt>false</tt> otherwise.
     */
    public boolean isOfflineMessagingSupported()
    {
        return true;
    }

    /**
     * Determines whether the protocol supports the supplied content type
     * for the given contact.
     *
     * @param contentType the type we want to check
     * @param contact contact which is checked for supported contentType
     * @return <tt>true</tt> if the contact supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType, Contact contact)
    {
        if (!(contact instanceof ContactJabberImpl))
        {
            throw new IllegalArgumentException(
                "The specified contact is not a Jabber contact."
                + contact);
        }

        // by default we support default mime type, for other mimetypes
        // method must be overridden
        if (contentType.equals(DEFAULT_MIME_TYPE))
        {
            return true;
        }
        else if (contentType.equals(HTML_MIME_TYPE))
        {
            Jid contactJID = ((ContactJabberImpl)contact).getAddressAsJid();
            Jid toJID = getJidForAddress(contactJID);

            if (toJID == null)
            {
                toJID = contactJID;
            }

            return jabberProvider.isFeatureListSupported(toJID, HTML_NAMESPACE);
        }

        return false;
    }

    /**
     * Returns a reference to an open chat with the specified
     * <tt>jid</tt> if one exists or creates a new one otherwise.
     *
     * @param jid the Jabber ID that we'd like to obtain a chat instance for.
     *
     * @return a reference to an open chat with the specified
     * <tt>jid</tt> if one exists or creates a new one otherwise.
     */
    public Chat obtainChatInstance(Jid jid)
    {
        XMPPConnection jabberConnection
            = jabberProvider.getConnection();

        // This creates a chat if it doesn't already exist
        return ChatManager.getInstanceFor(jabberConnection)
            .chatWith(jid.asEntityBareJidIfPossible());
    }

    /**
     * Remove from our <tt>jids</tt> map all entries that have not seen any
     * activity (i.e. neither outgoing nor incoming messages) for more than
     * JID_INACTIVITY_TIMEOUT. Note that this method is not synchronous and that
     * it is only meant for use by the {@link #getJidForAddress(String)} and
     * {@link #putJidForAddress(String, String)}
     */
    private void purgeOldJids()
    {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<Jid, TargetAddress>> entries
            = jids.entrySet().iterator();

        while( entries.hasNext() )
        {
            Map.Entry<Jid, TargetAddress> entry = entries.next();
            TargetAddress target = entry.getValue();

            if (currentTime - target.lastUpdatedTime
                            > JID_INACTIVITY_TIMEOUT)
                entries.remove();
        }
    }

    /**
     * Returns the last jid that the party with the specified <tt>address</tt>
     * contacted us from or <tt>null</tt> if we don't have a jid for the
     * specified <tt>address</tt> yet. The method would also purge all entries
     * that haven't seen any activity (i.e. no one has tried to get or remap it)
     * for a delay longer than <tt>JID_INACTIVITY_TIMEOUT</tt>.
     *
     * @param address the <tt>address</tt> that we'd like to obtain a jid for.
     *
     * @return the last jid that the party with the specified <tt>address</tt>
     * contacted us from or <tt>null</tt> if we don't have a jid for the
     * specified <tt>address</tt> yet.
     */
    private EntityFullJid getJidForAddress(Jid address)
    {
        synchronized(jids)
        {
            purgeOldJids();
            TargetAddress ta = jids.get(address);

            if (ta == null)
                return null;

            ta.lastUpdatedTime = System.currentTimeMillis();

            return ta.jid;
        }
    }

    /**
     * Maps the specified <tt>address</tt> to <tt>jid</tt>. The point of this
     * method is to allow us to send all messages destined to the contact with
     * the specified <tt>address</tt> to the <tt>jid</tt> that they last
     * contacted us from.
     *
     * @param address the bare address (i.e. no resource included) of the
     * contact that we'd like to set a jid for.
     * @param jid the jid (i.e. address/resource) that the contact with the
     * specified <tt>address</tt> last contacted us from.
     */
    private void putJidForAddress(BareJid address, EntityFullJid jid)
    {
        synchronized(jids)
        {
            purgeOldJids();

            TargetAddress ta = jids.get(address);

            if (ta == null)
            {
                ta = new TargetAddress();
                jids.put(address, ta);
            }

            ta.jid = jid;
            ta.lastUpdatedTime = System.currentTimeMillis();
        }
    }

    /**
     * Helper function used to send a message to a contact, with the given
     * extensions attached.
     *
     * @param to The contact to send the message to (this should be null if an
     * SMS number has been specified)
     * @param toResource The resource to send the message to or null if no
     * resource has been specified
     * @param smsNumber The SMS number to send the message to (this should be
     * null if a contact has been specified).
     * @param message The message to send.
     * @param extensions The XMPP extensions that should be attached to the
     * message before sending.
     * @return The MessageDeliveryEvent that resulted after attempting to
     * send this message, so the calling function can modify it if needed.
     */
    private MessageDeliveredEvent sendMessage(Contact to,
                                              ContactResource toResource,
                                              String smsNumber,
                                              ImMessage message,
                                              ExtensionElement[] extensions)
    {
        if ((smsNumber == null) && !(to instanceof ContactJabberImpl))
        {
           throw new IllegalArgumentException(
               "The specified contact is not a Jabber contact."
               + to);
        }

        try
        {
            assertConnected();

            MessageBuilder msgBuilder = MessageBuilder.buildMessage(message.getMessageUID());
            msgBuilder.ofType(org.jivesoftware.smack.packet.Message.Type.chat);

            Jid toJID = null;
            int messageType;

            if (smsNumber != null)
            {
                // An SMS number has been supplied so this is an SMS
                if (smsDomain != null)
                {
                    messageType = MessageEvent.SMS_MESSAGE;

                    // Change the number to national format before sending a
                    // message event so that the number is displayed
                    // consistently in the chat window and message history.
                    PhoneNumberUtilsService phoneNumberUtils =
                                          JabberActivator.getPhoneNumberUtils();
                    smsNumber = phoneNumberUtils.formatNumberToE164(smsNumber);

                    if (phoneNumberUtils.isValidSmsNumber(smsNumber))
                    {
                        Jid smsAddress = JidCreate.from(smsNumber + smsDomain);
                        toJID = getJidForAddress(smsAddress);

                        if (toJID == null)
                        {
                            toJID = smsAddress;
                        }

                        if (to == null)
                        {
                            // No contact has been specified.  This means this an
                            // SMS message sent to a non-contact number. Try and
                            // match it to a contact now, in case a matching
                            // contact exists.
                            MetaContact metaContact =
                                    JabberActivator.getMetaContactListService().
                                         findMetaContactForSmsNumber(smsNumber);
                            if (metaContact != null)
                            {
                                to = metaContact.getContactForSmsNumber(smsNumber);
                            }
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException(
                                   "Invalid SMS number supplied: " + smsNumber);
                    }
                }
                else
                {
                    throw new IllegalArgumentException("No SMS domain supplied");
                }
            }
            else
            {
                ContactJabberImpl toContact = (ContactJabberImpl) to;

                // No SMS number has been supplied so this is an IM
                messageType = MessageEvent.CHAT_MESSAGE;

                // If a specific resource has been provided, send the message
                // to that.
                if (toResource != null)
                {
                    toJID = (toResource.equals(ContactResource.BASE_RESOURCE))
                            ? toContact.getAddressAsJid()
                            : ((ContactResourceJabberImpl) toResource).getFullJid();
                }

                // If we don't yet have a JID, see if we already have a JID for
                // the contact's address and use that.
                if (toJID == null)
                {
                    toJID = getJidForAddress(toContact.getAddressAsJid());
                }

                // If we still don't have a JID, just use the contact's address
                // as the JID so we'll send to all registered resources.
                if (toJID == null)
                {
                    toJID = toContact.getAddressAsJid();
                }
            }

            Chat chat = obtainChatInstance(toJID);

            msgBuilder.to(toJID);

            for (ExtensionElement ext : extensions)
            {
                msgBuilder.addExtension(ext);
            }

            // No need to hash PII as explicitly sending/receiving message.
            logger.trace("Will send a message to:" + sanitiseChatAddress(toJID.toString())
                        + " chat.jid=" + chat.getXmppAddressOfChatPartner());

            MessageDeliveredEvent msgDeliveryPendingEvt
                = new MessageDeliveredEvent(message, to, smsNumber, messageType);

            msgDeliveryPendingEvt
                = messageDeliveryPendingTransform(msgDeliveryPendingEvt);

            if (msgDeliveryPendingEvt == null)
                return null;

            String content = msgDeliveryPendingEvt.getSourceMessage().getContent();

            if (message.getContentType().equals(HTML_MIME_TYPE))
            {
                msgBuilder.setBody(Html2Text.extractText(content));

                // Check if the other user supports XHTML messages
                // make sure we use our discovery manager as it caches calls
                if (jabberProvider.isFeatureListSupported(
                        chat.getXmppAddressOfChatPartner(),
                        HTML_NAMESPACE))
                {
                    // Add the XHTML text to the message
                    XHTMLManager.addBody(
                        msgBuilder,
                        new XHTMLText(null, "en").append(content).appendCloseBodyTag());
                }
            }
            else
            {
                // this is plain text so keep it as it is.
                msgBuilder.setBody(content);
            }

            //msgBuilder.addExtension(new Version());

            org.jivesoftware.smack.packet.Message msg = msgBuilder.build();

            MessageEventManager.
                addNotificationsRequests(msg, true, false, false, true);

            chat.send(msg);

            return new MessageDeliveredEvent(message, to, smsNumber, messageType);
        }
        catch (XmppStringprepException | NotConnectedException | InterruptedException | IllegalStateException ex)
        {
            MessageDeliveredEvent msgDeliveredEvt =
                new MessageDeliveredEvent(message,
                                          to,
                                          smsNumber,
                                          new Date(),
                                          null,
                                          MessageEvent.CHAT_MESSAGE,
                                          true);
            logger.error("message not sent", ex);
            return msgDeliveredEvt;
        }
    }

    @Override
    public void sendInstantMessage(String smsNumber, ImMessage message)
        throws IllegalStateException, IllegalArgumentException
    {
        sendInstantMessage(null, null, smsNumber, message, true);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance of ContactImpl.
     */
    public void sendInstantMessage(Contact to, ImMessage message)
        throws IllegalStateException, IllegalArgumentException
    {
        sendInstantMessage(to, null, message);
    }

    @Override
    public void sendInstantMessage(Contact to,
                                   ContactResource toResource,
                                   ImMessage message,
                                   boolean fireEvent)
        throws IllegalStateException, IllegalArgumentException
    {
        sendInstantMessage(to, toResource, null, message, fireEvent);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt>. Provides a default implementation of this method.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param toResource the resource to which the message should be send
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    @Override
    public void sendInstantMessage(Contact to,
                                   ContactResource toResource,
                                   ImMessage message)
        throws  IllegalStateException,
                IllegalArgumentException
    {
        sendInstantMessage(to, toResource, null, message, true);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt>. Provides a default implementation of this method.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param toResource the resource to which the message should be send
     * @param smsNumber the SMS number to which the message should be sent
     * @param message the <tt>Message</tt> to send.
     * @param fireEvent whether we should fire an event once the message
     * has been sent.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    private void sendInstantMessage(Contact to,
                                    ContactResource toResource,
                                    String smsNumber,
                                    ImMessage message,
                                    boolean fireEvent)
        throws  IllegalStateException,
                IllegalArgumentException
    {
        MessageDeliveredEvent msgDelivered =
            sendMessage(to, toResource, smsNumber, message, new ExtensionElement[0]);

        if (fireEvent)
        {
            fireMessageEvent(msgDelivered);
        }
    }

    /**
     * Replaces the message with ID <tt>correctedMessageUID</tt> sent to
     * the contact <tt>to</tt> at their <tt>ContactResource</tt>
     * <tt>resource</tt> with the message <tt>message</tt>
     *
     * @param to The contact to send the message to.
     * @param resource The contact resource to send the message to (if null,
     * the message will be sent to all of the contact's registered resources)
     * @param message The new message.
     * @param correctedMessageUID The ID of the message being replaced.
     */
    public void correctMessage(Contact to,
                               ContactResource resource,
                               ImMessage message,
                               String correctedMessageUID)
    {
        ExtensionElement[] exts = new ExtensionElement[1];
        exts[0] = new MessageCorrectionExtension(correctedMessageUID);

        ContactResource contactResource =
            (resource == null) ? ContactResource.BASE_RESOURCE : resource;
        MessageDeliveredEvent msgDelivered
            = sendMessage(to, contactResource, null, message, exts);
        msgDelivered.setCorrectedMessageUID(correctedMessageUID);
        fireMessageEvent(msgDelivered);
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected()
        throws IllegalStateException
    {
        if (opSetPersPresence == null)
        {
            throw
                new IllegalStateException(
                        "The provider must be signed on the service before"
                            + " being able to communicate.");
        }
        else
            opSetPersPresence.assertConnected();
    }

    /**
     * Converts all pending message fragments and sends them on as complete
     * messages, regardless of whether all fragments have been received.
     */
    public void processPendingMessageFragments()
    {
        logger.info("Processing pending message fragments");
        if (smackMessageListener != null)
        {
            smackMessageListener.processPendingMessageFragments();
        }
    }

    /**
     * Handle a message
     *
     * @param msg the message to handle
     * @param date the date the message was sent / received
     */
    public void handleMessage(org.jivesoftware.smack.packet.Message msg, Date date)
    {
        smackMessageListener.handleMessage(msg, date);
    }

    /**
     * @return a reference to the message history service
     */
    private MessageHistoryService getHistoryService()
    {
        return JabberActivator.getMessageHistoryService();
    }

    /**
     * Our listener that will tell us when we're registered to
     */
    private class RegistrationStateListener
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

            if (evt.getNewState() == RegistrationState.REGISTERING)
            {
                opSetPersPresence
                    = (OperationSetPersistentPresenceJabberImpl)
                        jabberProvider.getOperationSet(
                                OperationSetPersistentPresence.class);

                opSetSpecialMsg
                      = jabberProvider.getOperationSet(
                          OperationSetSpecialMessaging.class);

                if (smackMessageListener == null)
                {
                    smackMessageListener = new SmackMessageListener();
                }
                else
                {
                    // make sure this listener is not already installed in this
                    // connection
                    jabberProvider.getConnection()
                        .removeStanzaListener(smackMessageListener);
                }

                jabberProvider.getConnection().addStanzaListener(
                        smackMessageListener,
                        new AndFilter(
                            new NotFilter(GROUPCHAT),
                            StanzaTypeFilter.MESSAGE,
                            new ArchiveMessageStanzaFilter()));
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED
                || evt.getNewState() == RegistrationState.CONNECTION_FAILED
                || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED)
            {
                if (jabberProvider.getConnection() != null)
                {
                    if (smackMessageListener != null)
                        jabberProvider.getConnection().removeStanzaListener(
                            smackMessageListener);
                }

                smackMessageListener = null;
            }
        }
    }

    /**
     * The listener that we use in order to handle incoming messages.
     */
    private class SmackMessageListener
        implements StanzaListener
    {
        /**
         * Handles incoming messages and dispatches whatever events are
         * necessary.
         * @param stanza the packet that we need to handle (if it is a message).
         */
        public void processStanza(Stanza stanza)
        {
            if (!(stanza instanceof org.jivesoftware.smack.packet.Message))
                return;

            Object multiChatExtension =
                stanza.getExtensionElement("x", "http://jabber.org/protocol/muc#user");

            // If this is a multi-user chat message, ignore it, as it will be
            // handled elsewhere.
            if (multiChatExtension != null)
                return;

            org.jivesoftware.smack.packet.Message msg =
                (org.jivesoftware.smack.packet.Message)stanza;

            handleMessage(msg, null);
        }

        /**
         * Handle an incoming message and dispatch whatever events are
         * necessary
         * @param msg the message to handle
         * @param date the date this message was sent / received, or null
         *             if this message is not an archive message.
         */
        public void handleMessage(org.jivesoftware.smack.packet.Message msg,
                                  Date date)
        {
            String msgBody = msg.getBody();

            // If this is a carbon then extract the original message that has been
            // enclosed in the Forwarded element.
            boolean isCarbon = false;
            CarbonExtension carbon = CarbonExtension.from(msg);
            if (carbon != null)
            {
                Forwarded<org.jivesoftware.smack.packet.Message> forwarded =
                    carbon.getForwarded();

                if (forwarded != null)
                {
                    Stanza packet = forwarded.getForwardedStanza();

                    if (packet instanceof org.jivesoftware.smack.packet.Message)
                    {
                        // If the message that encloses the carbon is of type
                        // error, this is unexpected and probably means there
                        // is an issue with the server.  We do not want to try
                        // and process messages like this, so we instead log
                        // as many details as we can about the error and then
                        // return.
                        if (msg.getType() == org.jivesoftware.smack.packet.Message.Type.error)
                        {
                            StanzaError msgError = msg.getError();
                            logger.warn("Received an error carbon - ignoring" +
                                        ", Error Condition: " + msgError.getCondition() +
                                        ", Error Type: " + msgError.getType() +
                                        ", Error Message: " + msgError.getDescriptiveText() +
                                        ", Packet ID: " + packet.getStanzaId());
                            return;
                        }

                        // XEP-0280 specifies that all carbons must
                        // come from our bare jid.
                        Jid fromJid = msg.getFrom();

                        if (fromJid == null || !fromJid.equals(jabberProvider.getOurBareJid()))
                        {
                            logger.warn("Received a carbon copy with wrong from!");
                            return;
                        }

                        // We need to set the timestamp to the message delay,
                        // which will be non-null if the carbon was sent while
                        // this client was offline.  Note that this delay is
                        // stored in the message that encloses the carbon, so
                        // we need to get it before we replace msg with the
                        // enclosed Message that represents the carbon.
                        date = DelayInformationManager.getDelayTimestamp(msg);
                        msg = (org.jivesoftware.smack.packet.Message)packet;
                        msgBody = msg.getBody();
                        isCarbon = true;
                    }
                    else
                    {
                        logger.error("Ignoring forwarded carbon none message");
                        return;
                    }
                }
                else
                {
                    logger.error("Ignoring carbon message without a forward");
                    return;
                }
            }

            // If there's no message body, ignore the message, unless it's an
            // error message, as the message body is in the original message
            // that this error refers to.
            if (msgBody == null &&
                msg.getType() != org.jivesoftware.smack.packet.Message.Type.error)
            {
                logger.debug("Ignoring non-error message with no body");
                return;
            }

            // Check whether the message body matches one of the 'special'
            // messages that require separate processing before the client can
            // create and fire a MessageEvent for them.  One use of these
            // messages is to allow the client to stitch together an SMS that
            // has been split over several messages.
            Matcher match = null;
            if (msgBody != null)
            {
                match = JabberActivator.SPECIAL_MESSAGE_REGEX.matcher(msgBody);
            }

            if (match != null && match.matches())
            {
                String type = match.group(1);
                String content = match.group(2);
                processSpecialMessage(msg, type, content, date, isCarbon);
            }
            else
            {
                // This isn't a 'special' message, so check if it is a group
                // chat invite message that we've received from the archive.
                // If it is, it indicates that we already joined the group chat
                // on another client, so we need to tell the chat room manager
                // the date when we did join to be sure we request the correct
                // history when we join on this client.
                ExtensionElement inviteExt =
                              msg.getExtensionElement(
                                  GroupChatInvitation.ELEMENT,
                                  GroupChatInvitation.NAMESPACE);

                if (inviteExt instanceof GroupChatInvitation)
                {
                    if (date != null)
                    {
                        String chatRoomIdString =
                            ((GroupChatInvitation) inviteExt).getRoomAddress();

                        if (chatRoomIdString != null)
                        {
                            Jid chatRoomId = JidCreate.fromOrNull(chatRoomIdString);

                            if (chatRoomId != null)
                            {
                                logger.debug("Found direct invite for " +
                                    chatRoomId + " on " + date + " in the archive");
                                chatRoomManager.chatRoomJoinDateReceived(chatRoomId, date);
                            }
                            else
                            {
                                logger.warn("Received invalid JID for chatroom ID:" +
                                    chatRoomIdString);
                            }
                        }
                    }
                }
                else
                {
                    // This must just be a 'normal' message, so process it
                    // immediately, unless it's a group chat message from the
                    // archive.  In that case, it can be ignored since they are
                    // retrieved using DiscussionHistory instead.
                    String parsedFrom =
                        msg.getFrom() != null ? msg.getFrom().asBareJid().toString() : null;
                    String parsedTo =
                        msg.getTo() != null ? msg.getTo().asBareJid().toString() : null;

                    // Note that the type _should_ always be present, but if it
                    // isn't then we can fall back to other identifying features
                    // such as no to or from, or the to or from fields starting
                    // with "chatroom-"
                    if (org.jivesoftware.smack.packet.Message.Type.groupchat.equals(msg.getType()) ||
                        parsedTo == null ||
                        parsedFrom == null ||
                        parsedTo.equals(parsedFrom) ||
                        parsedTo.startsWith(OperationSetMultiUserChat.CHATROOM_ID_PREFIX) ||
                        parsedFrom.startsWith(OperationSetMultiUserChat.CHATROOM_ID_PREFIX))
                    {
                        return;
                    }

                    createAndFireMessageEvent(msg, msgBody, date, isCarbon);
                }
            }
        }

        /**
         * Processes a 'special' message.  For example, correlates an XMPP
         * packet ID to an SMPP packet ID, or stitches together different
         * fragments of an SMS that is split over multiple messages.
         *
         * @param msg The message
         * @param type The type of special message (e.g. 'correlator' or
         * 'fragment')
         * @param content The special message content
         * @param date the date this message was sent / received, or null
         *             if this message is not an archive message or a delayed
         *             carbon.
         * @param isCarbon Indicates if the message is a XEP-0280 carbon
         */
        private void processSpecialMessage(
                                      org.jivesoftware.smack.packet.Message msg,
                                      String type,
                                      String content,
                                      Date date,
                                      boolean isCarbon)
        {
            logger.debug("Received a special message of type " + type);

            SpecialMessageHandler handler =
                opSetSpecialMsg.getSpecialMessageHandler(type);

            // If there is a handler for this special message type, pass it
            // to the handler, unless it is an archive message (i.e. we've
            // been given a date when it was sent) or carbon message, as we
            // don't want to handle a special message more than once.
            if (handler != null)
            {
                if ((date == null) && !isCarbon)
                {
                    logger.debug("Passing special message of type " + type +
                        " to handler " + handler);
                    handler.handleSpecialMessage(content,
                        DelayInformationManager.getDelayTimestamp(msg));
                }
                else
                {
                    logger.debug(
                        "Not passing archive/carbon special message of type " +
                                           type + " to handler " + handler);
                }
            }
            else if (JabberActivator.CORRELATOR_ID.equals(type))
            {
                processCorrelatorMessage(msg, content);
            }
            else if (JabberActivator.FRAGMENT_ID.equals(type))
            {
                processFragmentMessage(msg, content, date, isCarbon);
            }
            else if (JabberActivator.GROUP_MEMBERSHIP_ID.equals(type))
            {
                processGroupMembershipMessage(msg, content, date);
            }
            else
            {
                logger.error("Unknown Message Type ID: " + type);
            }
        }

        /**
         * Processes a fragmented 'special' message.  It extracts details of
         * the message fragment from the message and stores them so that it
         * can stitch together the different SMS fragments to make a single
         * complete message.
         *
         * @param msg The message
         * @param content The special message content
         * @param date the date this message was sent / received, or null
         *             if this message is not an archive message or a delayed
         *             carbon.
         * @param isCarbon True if this is a XEP-0280 carbon message.
         */
        private void processFragmentMessage(
            org.jivesoftware.smack.packet.Message msg,
            String content,
            Date date,
            boolean isCarbon)
        {
            // This is a fragmented message.  Check whether it is a single
            // part or multi-part message.
            Matcher fragment =
                JabberActivator.FRAGMENT_MULTIPART_REGEX.matcher(content);

            if (fragment.find())
            {
                try
                {
                    String multiPartId = fragment.group(1);
                    int numParts = Integer.parseInt(fragment.group(2));
                    int partNum = Integer.parseInt(fragment.group(3));
                    String partBody = fragment.group(4);
                    logger.debug("Found a multi-part message with ID " + multiPartId);

                    if (partNum > numParts || numParts < 1 || partNum < 1)
                    {
                        logger.error(
                            "Unexpected multi-part message number: " + content);
                        return;
                    }

                    if (numParts == 1)
                    {
                        // Only 1 part so treat as a single part message
                        createAndFireMessageEvent(msg, partBody, date, isCarbon);
                        return;
                    }

                    // partNum is 1 indexed, but we want it to be zero
                    // indexed to save it in an array of fragments
                    partNum = partNum - 1;

                    synchronized (fragmentedMessages)
                    {
                        // Check whether we have already received any of
                        // the other fragments of this multipart message.
                        FragmentedMessage fragmentedMessage =
                            fragmentedMessages.get(multiPartId);

                        if (fragmentedMessage == null)
                        {
                            logger.debug("No existing message found - " +
                                         "creating one for multi-part ID " +
                                                               multiPartId);
                            // Create a new fragmented message to store
                            // this fragment and add it to the map of
                            // fragmented messages so that we can find and
                            // update it when further fragments arrive.
                            fragmentedMessage = new FragmentedMessage(multiPartId,
                                                                      numParts,
                                                                      msg,
                                                                      date,
                                                                      isCarbon);
                            fragmentedMessage.insertFragment(partNum, partBody);
                            fragmentedMessages.put(multiPartId, fragmentedMessage);
                        }
                        else
                        {
                            logger.debug("Found existing message - " +
                                         "inserting fragment for multi-part ID " +
                                                                     multiPartId);
                            // Insert this new fragment into the existing
                            // message.
                            fragmentedMessage.insertFragment(partNum, partBody);

                            // If we've now received the final fragment of
                            // this multi-part message, send the complete
                            // message immediately.
                            if (fragmentedMessage.isMessageBodyComplete())
                            {
                                logger.debug(
                                    "Message complete - sending immediately");
                                fragmentedMessage.sendMessageImmediately();
                            }
                        }
                    }
                }
                catch (NumberFormatException e)
                {
                    logger.error("Unexpected multi-part message format: " +
                                                                content, e);
                }
            }
            else
            {
                // We've received a message with only one fragment, so we
                // can send it immediately.
                fragment =
                    JabberActivator.FRAGMENT_SINGLEPART_REGEX.matcher(content);

                if (fragment.find())
                {
                    logger.debug("Found single part message");
                    createAndFireMessageEvent(msg, fragment.group(1), date, isCarbon);
                }
                else
                {
                    logger.error("Failed to find fragment: " + content);
                }
            }
        }

        /**
         * Processes a correlator 'special' message.  It correlates an XMPP
         * packet ID to an SMPP packet ID so that, if we receive an error for
         * the SMSC containing only an SMPP ID, we can find the XMPP ID of the
         * message that failed.
         *
         * @param msg The message
         * @param content The special message content
         */
        private void processCorrelatorMessage(
            org.jivesoftware.smack.packet.Message msg, String content)
        {
            Matcher correlatorMatcher =
                JabberActivator.CORRELATOR_REGEX.matcher(content);

            if (correlatorMatcher.matches())
            {
                // XMPP ID <-> SMPP ID message correlator
                String xmppId = correlatorMatcher.group(1);
                String smppId = correlatorMatcher.group(2);

                // We need the SMS number used to send the message to look
                // it up in the history.  First remove the resource from
                // the from address, then get rid of everything after "@".
                String smsNumber =
                    msg.getFrom().asBareJid().getLocalpartOrThrow().toString();

                if (!getHistoryService().updateSmppId(smsNumber, xmppId, smppId))
                {
                    logger.error("Failed to save SMPP ID " + smppId +
                       " for XMPP ID " + xmppId + " and peer ID " + logHasher(smsNumber));
                }
            }
            else
            {
                logger.error(
                    "Correlator failed to match expected format: " + content);
            }
        }

        /**
         * Processes a group membership 'special' message.  If we find any
         * joined, left or created messages from our own jid (node and domain only,
         * so that we can process messages we sent from another client), we inform the
         * chat room manager of the timestamp when we joined or left the chat
         * room so that it can request the correct history when joining the
         * room.
         *
         * @param msg The message
         * @param content The special message content
         * @param date the date this message was sent / received, or null
         *             if this message is not an archive message or a delayed
         *             carbon.
         */
        private void processGroupMembershipMessage(
            org.jivesoftware.smack.packet.Message msg, String content, Date date)
        {
            Matcher groupMembershipMatcher =
                JabberActivator.GROUP_MEMBERSHIP_REGEX.matcher(content);

            if (groupMembershipMatcher.matches())
            {
                String messagejid = groupMembershipMatcher.group(1);

                // Only want to compare node and domain (not resource) of JIDs
                // so we can match messages sent from this account on a
                // different client
                if (jabberProvider.getOurBareJid().toString().equals(
                    JitsiStringUtils.parseBareAddress(messagejid)))
                {
                    Jid chatRoomId = msg.getTo();
                    String msgBody = groupMembershipMatcher.group(2);

                    // It's possible that this message has been delayed, so we can
                    // get a more accurate join/leave timestamp for the group.
                    if (date == null)
                    {
                        date = DelayInformationManager.getDelayTimestamp(msg);
                    }

                    if (GroupMembershipAction.joined.toString().equals(msgBody) ||
                        GroupMembershipAction.created.toString().equals(msgBody))
                    {
                        logger.debug("Got an archive joined or created " +
                                     "message for ourself for chat room " + sanitiseChatRoom(chatRoomId));
                        chatRoomManager.chatRoomJoinDateReceived(chatRoomId, date);
                    }
                    else if (GroupMembershipAction.left.toString().equals(msgBody))
                    {
                        logger.debug("Got an archive left message for " +
                                     "ourself for chat room " + sanitiseChatRoom(chatRoomId));
                        chatRoomManager.chatRoomLeaveReceived(chatRoomId, date);
                    }
                }
            }
            else
            {
                logger.error("Group membership message failed to match " +
                             "expected format: " + content);
            }
        }

        /**
         * Creates and fires a MessageEvent for the given message and message
         * body
         *
         * @param message The message
         * @param msgBody The message body
         * @param date the date this message was sent / received, or null
         *             if this message is not an archive message.
         * @param isCarbon A flag indicating if this is a Carbon message.
         */
        private void createAndFireMessageEvent(
            org.jivesoftware.smack.packet.Message message,
            String msgBody,
            Date date,
            boolean isCarbon)
        {
            org.jivesoftware.smack.packet.Message msg = message;
            // Only archive messages and offline message carbons will already
            // have a timestamp extracted at this point.  Therefore, this must
            // be an archive message if it isn't a carbon but we do have a
            // timestamp.
            boolean isArchive = (date != null) && !isCarbon;
            String address = jabberProvider.getAccountID().getAccountAddress();
            boolean isSent = (isArchive || isCarbon) && address.equals(
                                  msg.getFrom().asBareJid().toString());
            Jid otherAddress = isSent ? msg.getTo() : msg.getFrom();
            BareJid otherUserIdAsJid = otherAddress.asBareJid();
            String otherUserId = otherUserIdAsJid.toString();
            String xmppId = msg.getStanzaId();
            boolean isError =
                (msg.getType() == org.jivesoftware.smack.packet.Message.Type.error);

            MessageEvent record = null;

            // iOS clients from before V2.13 don't add an ID to the message.
            if (xmppId == null)
            {
                // If this message is an archive message and has no ID, then
                // ignore it as we have no way of knowing if we've seen it
                // before or not.
                if (isArchive)
                {
                    logger.error("Ignoring message without ID, from " + sanitiseChatAddress(otherAddress));
                    return;
                }

                // If it is not an archive message, generate a random UID for
                // it so that we can correctly store it in and retrieve it from
                // history.
                xmppId = UUID.randomUUID().toString();
                logger.debug("Generated random UID " + xmppId +
                             " for message without ID, from " + sanitiseChatAddress(otherAddress));
            }
            else
            {
                // See if this message has been received before
                record = getHistoryService().findByXmppId(otherUserId, xmppId);
            }

            logger.debug("Message received: is archive " + isArchive +
                         ", is carbon " + isCarbon +
                         ", is sent " + isSent +
                         ", is error " + isError +
                         ", other user " + sanitiseChatAddress(otherUserId) +
                         ", received before? " + (record != null) +
                         ", xmpp id " + xmppId);

            if ((record != null) && !isError)
            {
                // This message has been received before.  If it is an error
                // for a message we've previously sent, this is expected so we
                // just carry on processing.  If it is an archive message then
                // there is nothing further to do.  Otherwise, we need to mark
                // the message as unread (since it must be an offline message
                // thus not seen elsewhere).
                if (!isArchive && !record.isMessageRead())
                {
                    // Update the message to be unread.
                    getHistoryService().updateReadStatus(otherUserId, xmppId, false);
                }

                return;
            }

            Date timestamp;
            boolean isOfflineMessage = false;

            // If we don't already have a timestamp for this message, see if
            // there is a delay on the message that we need to use as the
            // timestamp.  If there isn't, this message was sent in real-time
            // so set the timestamp to the current time.
            if (date == null)
            {
                timestamp = DelayInformationManager.getDelayTimestamp(msg);
                if (timestamp == null)
                {
                    timestamp = new Date();
                }
                else if (!isCarbon)
                {
                    // Found a delay for non-carbon - offline message.
                    isOfflineMessage = true;
                }
            }
            else
            {
                timestamp = date;
            }

            ImMessage newMessage = createMessage(msgBody, DEFAULT_MIME_TYPE,
                                xmppId, isArchive, isOfflineMessage, isCarbon);

            //check if the message is available in xhtml
            ExtensionElement ext = msg.getExtension(
                            "http://jabber.org/protocol/xhtml-im");

            if (ext != null)
            {
                XHTMLExtension xhtmlExt
                    = (XHTMLExtension)ext;

                //parse all bodies
                Iterator<? extends CharSequence> bodies = xhtmlExt.getBodies().iterator();
                StringBuffer messageBuff = new StringBuffer();
                while (bodies.hasNext())
                {
                    String body = bodies.next().toString();
                    messageBuff.append(body);
                }

                if (messageBuff.length() > 0)
                {
                    // we remove body tags around message cause their
                    // end body tag is breaking
                    // the visualization as html in the UI
                    String receivedMessage =
                        messageBuff.toString()
                        // removes body start tag
                        .replaceAll("<[bB][oO][dD][yY].*?>","")
                        // removes body end tag
                        .replaceAll("</[bB][oO][dD][yY].*?>","");

                    // for some reason &apos; is not rendered correctly
                    // from our ui, lets use its equivalent. Other
                    // similar chars(< > & ") seem ok.
                    receivedMessage =
                            receivedMessage.replaceAll("&apos;", "&#39;");

                    newMessage = createMessage(receivedMessage,
                            HTML_MIME_TYPE, xmppId, isArchive, isOfflineMessage, isCarbon);
                }
            }

            ExtensionElement correctionExtension =
                    msg.getExtension(MessageCorrectionExtension.NAMESPACE);
            String correctedMessageUID = null;
            if (correctionExtension != null)
            {
                correctedMessageUID = ((MessageCorrectionExtension)
                        correctionExtension).getCorrectedMessageUID();
                logger.debug("Found corrected message UID " +
                             correctedMessageUID + " in message " + xmppId);
            }

            Contact sourceContact = null;
            String smsNumber = null;
            int messageType;

            if (otherUserId.contains("@sms."))
            {
                if (!ConfigurationUtils.isSmsEnabled())
                {
                    logger.warn("Ignoring SMS received when SMS is disabled");
                    return;
                }

                // The sender's domain contains '@sms.' so this is an SMS.
                messageType = MessageEvent.SMS_MESSAGE;
                String unformattedNumber = otherUserId.split("@")[0];
                // Update the number to national format before sending a
                // message event so that the number is displayed consistently
                // in the chat window and message history.
                smsNumber = JabberActivator.getPhoneNumberUtils().
                                       formatNumberToNational(unformattedNumber);
                MetaContact metaContact =
                                    JabberActivator.getMetaContactListService().
                                         findMetaContactForSmsNumber(smsNumber);
                if (metaContact != null)
                {
                    sourceContact = metaContact.getContactForSmsNumber(smsNumber);
                }

                logger.debug("SMS received from number " + logHasher(smsNumber) +
                                                 ", contact : " + sourceContact);
            }
            else
            {
                // The sender's domain doesn't contain '@sms.' so this is an IM.
                messageType = MessageEvent.CHAT_MESSAGE;
                sourceContact = opSetPersPresence.findContactByID(otherUserId);
                // No need to hash Contact, as its toString() method does that.
                logger.debug("IM received from contact : " + sourceContact);
            }

            if (isError)
            {
                StanzaError msgError = msg.getError();
                String errorMessage = msgError.getDescriptiveText();
                StanzaError.Type errorType = msgError.getType();
                StanzaError.Condition errorCondition = msgError.getCondition();

                logger.info("Message error '" + errorMessage  +
                            "' received from " + sanitiseChatAddress(otherUserId) +
                            ", type: " + errorType +
                            ", condition: " + errorCondition +
                            ", id: " + xmppId);

                // Set a specific failure code, if we can determine one,
                // otherwise default to "unknown error".
                int errorResultCode = MessageDeliveryFailedEvent.UNKNOWN_ERROR;
                if (errorCondition == StanzaError.Condition.internal_server_error)
                {
                    logger.debug(
                        "Offline message queue is full for " + sanitiseChatAddress(otherUserId));
                    errorResultCode =
                        MessageDeliveryFailedEvent.OFFLINE_MESSAGE_QUEUE_FULL;

                }
                else if (errorCondition == StanzaError.Condition.service_unavailable)
                {
                    org.jivesoftware.smackx.xevent.packet.MessageEvent msgEvent =
                        (org.jivesoftware.smackx.xevent.packet.MessageEvent)
                            msg.getExtensionElement("x", "jabber:x:event");
                    if (msgEvent != null && msgEvent.isOffline())
                    {
                        logger.debug(
                            "Offline messages are not supported by " + sanitiseChatAddress(otherUserId));
                        errorResultCode =
                            MessageDeliveryFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED;
                    }
                }

                String failedMessageXmppId = null;
                String uid = newMessage.getMessageUID();
                if (uid == null || uid.isEmpty())
                {
                    logger.error("No id on XMPP error message: " + msgError +
                                 " with condition: " + errorCondition);
                }
                else if (uid.equals(JabberActivator.FAKE_XMPP_ID))
                {
                    logger.debug("Found an SMPP error");
                    // This represents an error from the SMSC with an SMPP ID,
                    // rather than an XMPP ID.  Therefore, to create a
                    // MessageDeliveryFailed event with the correct XMPP ID, we
                    // need to look up the XMPP ID for the SMPP ID in message
                    // history.
                    Matcher smppIdMatcher =
                        JabberActivator.SMPP_ID_REGEX.matcher(errorMessage);
                    if (smppIdMatcher.find())
                    {
                        String smppId = smppIdMatcher.group(1);

                        // We need the IM address or SMS number used to send
                        // the failed message to look it up in the history.
                        String peerId = (smsNumber == null) ? otherUserId : smsNumber;

                        // Try to find a message event representing the message
                        // that failed.
                        MessageEvent failedMessage =
                            getHistoryService().findBySmppId(peerId, smppId);

                        if (failedMessage != null)
                        {
                            // Get the XMPP ID from the failed message.
                            failedMessageXmppId =
                                failedMessage.getSourceMessage().getMessageUID();

                            logger.debug("Found XMPP ID " + failedMessageXmppId +
                                " for SMPP ID " + smppId + " and peerID " + sanitisePeerId(peerId));

                            // Create a new message with the correct XMPP ID
                            String encoding = newMessage.getEncoding();
                            newMessage = createMessage(
                                msgBody, encoding, failedMessageXmppId, isArchive, isOfflineMessage, isCarbon);
                        }
                        else
                        {
                            logger.error(
                                "Failed to find XMPP ID for SMPP ID: " +
                                     smppId + " errorMessage: " + errorMessage);
                        }
                    }
                    else
                    {
                        logger.error("Failed to find SMPP ID in message: " +
                                                                  errorMessage);
                    }
                }
                else
                {
                    logger.debug("Found an XMPP error for XMPP ID " + uid);
                    failedMessageXmppId = uid;
                }

                if (failedMessageXmppId != null &&
                    (sourceContact != null || smsNumber != null))
                {
                    MessageDeliveryFailedEvent failedEvent =
                        new MessageDeliveryFailedEvent(newMessage,
                                                       sourceContact,
                                                       smsNumber,
                                                       correctedMessageUID,
                                                       failedMessageXmppId,
                                                       errorResultCode,
                                                       messageType);

                    // Check that we have a message in history with this XMPP
                    // ID for this peer.  If we haven't, this is almost
                    // certainly an error related to a failure to deliver a
                    // typing notification to an offline client.  In that
                    // case, there's nothing we can mark as failed, so we can
                    // do nothing other than log and return.  We use the peer
                    // ID from the event for the lookup, as the event will
                    // have parsed that out correctly.
                    String peerId = failedEvent.getPeerIdentifier();
                    MessageEvent failedMessage =
                        getHistoryService().findByXmppId(peerId,
                                                         failedMessageXmppId);

                    if (failedMessage != null)
                    {
                        fireMessageEvent(failedEvent);
                    }
                    else
                    {
                        logger.warn("Unable to find message with id " +
                                     failedMessageXmppId + " for peer id " +
                                     sanitisePeerId(peerId) + " to mark as failed.");
                    }
                }

                // We've finished processing the error now so can just return.
                return;
            }

            //cache the jid (resource included) of the contact that's sending us
            //a message so that all following messages would go to the resource
            //that they contacted us from.
            if (otherAddress.asEntityFullJidIfPossible() != null)
            {
                putJidForAddress(otherUserIdAsJid,
                                 otherAddress.asEntityFullJidIfPossible());
            }

            // No need to hash PII as explicitly sending/receiving message.
            logger.trace("just mapped: " + sanitiseChatAddress(otherUserId)
                                + " to " + sanitiseChatAddress(otherAddress));

            if (sourceContact == null)
            {
                logger.trace("received a message from an unknown contact: "
                             + otherUserId);

                // No need to create a volatile contact for SMS messages.
                if (messageType != MessageEvent.SMS_MESSAGE)
                {
                    logger.trace("creating volatile contact for " + sanitiseChatAddress(otherUserId));
                    sourceContact = opSetPersPresence
                        .createVolatileContact(otherUserId);
                }
            }

            // Check to see if the date is after now (which could happen
            // if the server time doesn't match ours).  If it is then we
            // just set the time to be a bit before now.
            long currentTime = System.currentTimeMillis();
            if (timestamp.getTime() > currentTime)
            {
                timestamp.setTime(currentTime - 1);
            }

            // Create the event to send.
            MessageEvent msgEvt;

            if (isSent)
            {
                // This is a message that the user sent themselves
                msgEvt = new MessageDeliveredEvent(newMessage,
                                                   sourceContact,
                                                   smsNumber,
                                                   timestamp,
                                                   correctedMessageUID,
                                                   messageType,
                                                   false);
            }
            else if (messageType == MessageEvent.CHAT_MESSAGE)
            {
                ContactResource resource = ((ContactJabberImpl) sourceContact).
                                              getResourceFromJid(msg.getFrom());

                ((ContactJabberImpl) sourceContact).setLatestResource(resource);

                msgEvt = new MessageReceivedEvent(newMessage,
                                                  sourceContact,
                                                  resource,
                                                  timestamp,
                                                  correctedMessageUID,
                                                  (isArchive || isCarbon),
                                                  messageType);
            }
            else
            {
                msgEvt = new MessageReceivedEvent(newMessage,
                                                  smsNumber,
                                                  sourceContact,
                                                  timestamp,
                                                  (isArchive || isCarbon),
                                                  messageType);
            }

            fireMessageEvent(msgEvt);
        }

        /**
         * Converts all pending message fragments and sends them on as complete
         * messages, regardless of whether all fragments have been received.
         */
        protected void processPendingMessageFragments()
        {
            logger.info("Processing pending message fragments");

            // Copy the list of FragmentedMessages so that we don't try to keep
            // the lock while we're cancelling the timer task.
            Collection<FragmentedMessage> values;
            synchronized (fragmentedMessages)
            {
                values =
                        new ArrayList<>(fragmentedMessages.values());
            }

            // Cancel the timer task for all FragmentedMessages. This will
            // cause us to process whatever fragments we've already received
            // for each message.
            for (FragmentedMessage fragmentedMessage : values)
            {
                fragmentedMessage.sendMessageImmediately();
            }
        }
    }

    /**
     * A representation of a message that is received in multiple
     * fragments.
     */
    private class FragmentedMessage
    {
        /**
         * The number of ms after which we process a fragmented message,
         * even if all of its fragments haven't yet arrived.
         */
        private static final long FRAGMENT_TIMEOUT = 300000;

        /**
         * The string used to represent any missing fragments if a message
         * is processed before all the fragments have been received.
         */
        private static final String ELLIPSIS = "...";

        /**
         * The ID shared by all fragments that make up this message.
         */
        private final String fragmentId;

        /**
         * The number of fragments that make up this message.
         */
        private final int numFragments;

        /**
         * The first fragmented message received.  This is used to create
         * the MessageEvent that will eventually be fired once all of the
         * fragments have been received.
         */
        private final org.jivesoftware.smack.packet.Message smackMessage;

        /**
         * An array to contain all of the fragments that make up this
         * message.
         */
        private final String[] fragments;

        /**
         * A runnable that will process the message fragments and pass
         * them on to the createAndFireMessageEvent method after a given
         * timeout to ensure the user still sees the message, even if
         * some fragments are never received.
         */
        private final CancellableRunnable fragmentRunnable;

        /**
         * The date that this fragment was received / sent.  Null if this fragment
         * is not an archive fragment
         */
        private final Date date;

        /**
         * Whether the fragment was received as a XEP-0280 carbon message.
         */
        private final boolean isCarbon;

        /**
         * Creates a new FragmentedMessage, which represents a message that
         * is received in multiple fragments.
         *
         * @param fragmentId the ID shared by all fragments that make up
         * this message
         * @param numFragments the number of fragments that make up this
         * message
         * @param smackMessage the first fragmented message received
         * @param date the date this message was sent / received, or null
         *             if this message is not an archive message.
         * @param isCarbon true if the fragments are being received as XEP-0280
         *                 carbons
         */
        public FragmentedMessage(String fragmentId,
                                 int numFragments,
                                 org.jivesoftware.smack.packet.Message smackMessage,
                                 Date date,
                                 boolean isCarbon)
        {
            logger.debug("Creating new FragmentedMessage: " +
                         " fragmentId" + fragmentId + ", numFragments: " + numFragments);
            this.date = date;
            this.fragmentId = fragmentId;
            this.numFragments = numFragments;
            this.smackMessage = smackMessage;
            this.fragments = new String[numFragments];
            this.fragmentRunnable = createRunnable();
            this.isCarbon = isCarbon;
            ThreadingService threading = JabberActivator.getThreadingService();
            threading.schedule("FragmentedMessage-" + fragmentId,
                               fragmentRunnable,
                               FRAGMENT_TIMEOUT);
        }

        /**
         * Inserts a message fragment at the given index
         * @param fragmentIndex the index in which to insert the fragment
         * @param fragment the fragment to insert
         */
        protected void insertFragment(int fragmentIndex, String fragment)
        {
            if (fragmentIndex < numFragments)
            {
                synchronized (fragments)
                {
                    logger.debug(
                        "Inserting fragment at index " + fragmentIndex);
                    fragments[fragmentIndex] = fragment;
                }
            }
            else
            {
                logger.error(
                    "Asked to insert fragment with invalid index " +
                            fragmentIndex + ", numFragments = " + numFragments);
            }
        }

        /**
         * Returns true if all fragments of the message have been received.
         *
         * @return true if all fragments of the message have been received
         */
        protected boolean isMessageBodyComplete()
        {
            boolean isComplete = true;
            synchronized (fragments)
            {
                for(String fragment : fragments)
                {
                    if (fragment == null)
                    {
                        isComplete = false;
                        break;
                    }
                }
            }
            return isComplete;
        }

        /**
         * Returns all fragments of the message concatenated together to
         * make a single string to use as the message body.
         *
         * @return all fragments of the message concatenated together to
         * make a single string to use as the message body
         */
        private String toMessageBody()
        {
            StringBuilder messageBody = new StringBuilder();

            synchronized (fragments)
            {
                for(String fragment : fragments)
                {
                    // If a fragment is missing, we replace it with an
                    // ellipsis.
                    if (fragment == null)
                    {
                        fragment = ELLIPSIS;
                    }

                    messageBody = messageBody.append(fragment);
                }
            }

            return messageBody.toString();
        }

        /**
         * Returns the first fragmented message received.
         *
         * @return the message
         */
        private org.jivesoftware.smack.packet.Message getSmackMessage()
        {
            return smackMessage;
        }

        /**
         * Creates and returns a timer task that will process the message
         * fragments and pass them on to the rest of the client as a
         * complete message after a given timeout.
         *
         * @return the timer task
         */
        private CancellableRunnable createRunnable()
        {
            return new CancellableRunnable()
            {
                private boolean hasRun = false;

                @Override
                public synchronized void run()
                {
                    logger.debug("Running for " + fragmentId);

                    if (hasRun)
                    {
                        logger.debug(
                            "Not processing fragmented message as task already ran");
                    }
                    else
                    {
                        processFragmentedMessage();
                    }

                    hasRun = true;
                }

                /**
                 * Processes a fragmented message once it is ready to be
                 * sent on to the rest of the client as a complete message.
                 */
                private void processFragmentedMessage()
                {
                    // First we remove the FragmentedMessage from the map,
                    // as we're ready to process it as a complete message.
                    FragmentedMessage fragmentedMessage;
                    synchronized (fragmentedMessages)
                    {
                        fragmentedMessage =
                            fragmentedMessages.remove(fragmentId);
                    }

                    // If we didn't find the fragmented message in the
                    // list, it must have already been processed, so do
                    // nothing here.
                    if (fragmentedMessage != null)
                    {
                        logger.debug("Processing FragmentedMessage with ID " +
                                      fragmentedMessage.fragmentId);
                        smackMessageListener.createAndFireMessageEvent(
                                        fragmentedMessage.getSmackMessage(),
                                        fragmentedMessage.toMessageBody(),
                                        date,
                                        isCarbon);
                    }
                }
            };
        }

        /**
         * Sends the fragmented message immediately, regardless of whether
         * all of the fragments have been received.
         */
        protected void sendMessageImmediately()
        {
            logger.debug("Sending message immediately for " + fragmentId);
            fragmentRunnable.run();
        }
    }

    /**
     * A filter that prevents this operation set from handling archive messages.
     */
    private static class ArchiveMessageStanzaFilter implements StanzaFilter
    {
        // Import handrolled classed to Jitsi and match on that?
        @Override
        public boolean accept(Stanza stanza)
        {
            ExtensionElement ext = stanza.getExtensionElement(
                    ArchivedMessageExtensionElement.ELEMENT_NAME,
                    ArchivedMessageExtensionElement.NAMESPACE);

            // Accept it if this isn't an archived message.  I.e. if the ext is
            // null or the wrong type.
            return !(ext instanceof ArchivedMessageExtensionElement);
        }
    }

    @Override
    public boolean isContactImCapable(Contact contact)
    {
        boolean isContactImCapable = false;

        // A list to store parameters to include in the interval log
        List<String> logParams = new ArrayList<>();

        // First check whether our contact request has been authorised - if
        // not, we cannot message this contact.
        boolean isAuthorised;
        boolean isCommPortalIm = "CommPortal".equals(ConfigurationUtils.getImProvSource());
        boolean autoPopulateIMEnabled = ConfigurationUtils.autoPopulateIMEnabled();
        logParams.add("isCommPortalIm:" + isCommPortalIm);
        logParams.add("autoPopulateIMEnabled:" + autoPopulateIMEnabled);
        if (isCommPortalIm && autoPopulateIMEnabled)
        {
            // CommPortal IM with autoPopulate - no contacts appear as request pending, so always
            // treat them as if they are authorised.
            isAuthorised = true;
        }
        else
        {
            // Manual IM - can only be messaged if the contact request really
            // has been authorised
            OperationSetExtendedAuthorizations authOpSet =
                jabberProvider.getOperationSet(
                    OperationSetExtendedAuthorizations.class);

            SubscriptionStatus status = authOpSet.getSubscriptionStatus(contact);
            isAuthorised = (status == SubscriptionStatus.Subscribed);
            logParams.add("status:" + status);
        }

        logParams.add("isAuthorised:" + isAuthorised);
        if (isAuthorised)
        {
            // The contact is authorised so return true if the contact either
            // has no resources (so we have to assume it can be messaged) or
            // has some resources and at least one of them is not the
            // "ext_client" resource that indicates that the contact has not
            // been registered on an Accession phone.
            Collection<ContactResource> resources = contact.getResources();
            boolean hasResources = !resources.isEmpty();
            if (!hasResources)
            {
                isContactImCapable = true;
            }
            else
            {
                for (ContactResource resource : resources)
                {
                    if (!EXT_CLIENT_RESOURCE.equals(resource.getResourceName()))
                    {
                        isContactImCapable = true;
                        break;
                    }
                }
            }

            logParams.add("hasResources:" + hasResources);
        }

        logParams.add("isContactImCapable:" + isContactImCapable);

        // No need to hash Contact, as its toString() method does that.
        logger.interval("isContactImCapable:" + contact,
                         "Is Contact IM capable?",
                         logParams.toArray());

        return isContactImCapable;
    }

    @Override
    public void processStanza(Stanza stanza)
        throws NotConnectedException, InterruptedException, NotLoggedInException
    {
     // No work to do - allow the connection to continue
        logger.error("Dropped stanza - ID: " + stanza.getStanzaId());
    }
}
