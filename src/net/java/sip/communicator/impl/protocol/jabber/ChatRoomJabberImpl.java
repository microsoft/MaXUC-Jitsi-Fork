/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.Hasher.logHasher;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StanzaError.Condition;
import org.jivesoftware.smackx.delay.DelayInformationManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.MUCRole;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException.MucNotJoinedException;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;
import org.jivesoftware.smackx.muc.UserStatusListener;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jivesoftware.smackx.xevent.MessageEventManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.impl.protocol.jabber.JabberActivator.GroupMembershipAction;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.analytics.AnalyticsParameterSimple;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.insights.enums.InsightsResultCode;
import net.java.sip.communicator.service.insights.parameters.CommonParameterInfo;
import net.java.sip.communicator.service.insights.parameters.JabberParameterInfo;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomConfigurationForm;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ImMessage;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetSpecialMessaging;
import net.java.sip.communicator.service.protocol.OperationSetSpecialMessaging.SpecialMessageHandler;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberPresenceListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomPropertyChangeEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomPropertyChangeFailedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomPropertyChangeListener;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.MessageEvent;
import net.java.sip.communicator.service.protocol.event.MessageFailedEvent;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.JitsiStringUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.resources.ResourceManagementService;

/**
 * Implements chat rooms for jabber. The class encapsulates instances of the
 * jive software <tt>MultiUserChat</tt>.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 * @author Valentin Martinet
 */
public class ChatRoomJabberImpl implements ChatRoom
{
    private static final Logger logger = Logger.getLogger(ChatRoomJabberImpl.class);
    private final ResourceManagementService resources = JabberActivator.getResources();
    private final MessageHistoryService messageHistoryService = JabberActivator.getMessageHistoryService();
    private final MetaContactListService contactListService = JabberActivator.getMetaContactListService();

    /**
     * The multi user chat smack object that we encapsulate in this room.
     */
    private MultiUserChat multiUserChat = null;

    /**
     * The smack message listener that will receive all messages for this room.
     */
    private final SmackMessageListener smackMessageListener = new SmackMessageListener();

    /**
     * The smack member listener that will receive notifications of other
     * members' permissions changes for this room.
     */
    private final MemberListener memberListener = new MemberListener();

    /**
     * The smack subject updated listener that will receive all subject updates
     * for this room.
     */
    private final SmackSubjectUpdatedListener smackSubjectUpdatedListener = new SmackSubjectUpdatedListener();

    /**
     * The user listener that will receive notifications of this user's
     * permissions changes for this room.
     */
    private final UserListener userListener = new UserListener();

    /**
     * Listeners that will be notified of changes in member status in the
     * room such as member joined, left or being kicked or dropped.
     */
    private final Vector<ChatRoomMemberPresenceListener> memberListeners = new Vector<>();

    /**
     * Listeners that will be notified every time
     * a new message is received on this chat room.
     */
    private final Vector<ChatRoomMessageListener> messageListeners = new Vector<>();

    /**
     * Listeners that will be notified every time
     * a chat room property has been changed.
     */
    private final Vector<ChatRoomPropertyChangeListener> propertyChangeListeners = new Vector<>();

    /**
     * The protocol provider that created us
     */
    private final ProtocolProviderServiceJabberImpl provider;

    /**
     * The operation set that created us.
     */
    private final OperationSetMultiUserChatJabberImpl opSetMuc;

    /**
     * The persistent presence operation set.
     */
    private final OperationSetPersistentPresenceJabberImpl opSetPersPres;

    /**
     * The special message operation set.
     */
    private final OperationSetSpecialMessaging opSetSpecialMsg;

    /**
     * The map of members of this chat room to their addresses.  The addresses
     * must be stored as case insensitive, as addresses stored on the server
     * may not match the case of addresses stored in our contacts.
     */
    private final Map<String, ChatRoomMemberJabberImpl> members =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * The nickname of this chat room local user participant.
     */
    private String nickname;

    /**
     * The JID of this chat room local user participant.
     */
    private final Jid localJid;

    /**
     * The subject of this chat room. Keeps track of the subject changes.
     */
    private String oldSubject;

    /**
     * The role of this chat room local user participant.
     */
    private ChatRoomMemberRole role = null;

    /**
     * The corresponding configuration form.
     */
    private ChatRoomConfigurationFormJabberImpl configForm;

    /**
     * Stanza listener waits for rejection of invitations to join room.
     */
    private InvitationRejectionListeners invitationRejectionListeners
        = new InvitationRejectionListeners();

    /**
     * Whether this chat room is active.
     */
    private boolean isActive = false;

    /**
     * Whether this chat room is muted and hence shouldn't display popups or
     * play audio notifications
     */
    private boolean isMuted;

    /**
     * The property we use to store the chat room mute state in the
     * configuration service.
     */
    private static final String MUTE_PROPERTY_NAME = "chatRoomMuted";

    /**
     * The property we use to store the value of subject in the configuration
     * service.
     */
    private static final String SUBJECT_PROPERTY_NAME = "chatRoomSubject";

    /**
     * A lock object for the String that stores the old subject.
     */
    private final Object oldSubjectLock = new Object();

    /**
     * The date when we were invited to this room (will remain null if we
     * weren't invited since the client was last rebooted).
     */
    private Date invitedDate = null;

    /**
     * Creates an instance of a chat room.
     *
     * @param multiUserChat MultiUserChat
     * @param provider a reference to the currently valid jabber protocol
     * provider.
     * @throws OperationFailedException if either the provider is not registered
     *                                  or the user's jid is invalid.
     */
    public ChatRoomJabberImpl(MultiUserChat multiUserChat,
                              ProtocolProviderServiceJabberImpl provider)
    throws OperationFailedException
    {
        this.multiUserChat = multiUserChat;

        this.provider = provider;
        this.opSetMuc = (OperationSetMultiUserChatJabberImpl) provider
            .getOperationSet(OperationSetMultiUserChat.class);
        this.opSetPersPres = (OperationSetPersistentPresenceJabberImpl) provider
            .getOperationSet(OperationSetPersistentPresence.class);
        this.opSetSpecialMsg = (OperationSetSpecialMessagingJabberImpl) provider
            .getOperationSet(OperationSetSpecialMessaging.class);

        String identifier = getIdentifier().toString();
        logger.debug("Creating new chat room with id " + sanitiseChatRoom(identifier));

        // Get the old subject from config, if we have it, as the server
        // won't return a subject when we first connect, even if one is set.
        this.oldSubject = ConfigurationUtils.getChatRoomProperty(
                               provider, identifier, SUBJECT_PROPERTY_NAME);

        // Add all of the listeners that we need
        multiUserChat.addSubjectUpdatedListener(smackSubjectUpdatedListener);
        multiUserChat.addMessageListener(smackMessageListener);
        multiUserChat.addParticipantStatusListener(memberListener);
        multiUserChat.addUserStatusListener(userListener);

        try
        {
            this.provider.getConnection().addStanzaListener(
                    invitationRejectionListeners, StanzaTypeFilter.MESSAGE);
        }
        catch (NullPointerException ex)
        {
            // If the connection to the server is flaky, we risk hitting NPEs here.
            // We need to make sure we catch this to prevent the calling thread
            // from being killed and instead throw an OperationFailedException
            // that will be handled correctly by the calling code.
            String errorText = "Hit NPE trying to get provider connection: " + this.provider;
            logger.error(errorText, ex);
            throw new OperationFailedException(
                      errorText,
                      OperationFailedException.PROVIDER_NOT_REGISTERED,
                      ex);
        }

        // Set the mute state of the chat room
        String mutedConfig = ConfigurationUtils.getChatRoomProperty(
                                 provider, identifier, MUTE_PROPERTY_NAME);

        isMuted = mutedConfig != null && mutedConfig.equals("true");

        String localJidString = provider.getAccountID().getAccountAddress();

        // Until we have a user defined nickname, use the local Jid
        nickname = localJidString;

        try
        {
            localJid = JidCreate.from(localJidString);
        }
        catch (XmppStringprepException ex)
        {
            String errorText = "Account address is not a valid JID! : "
                + localJidString;
            logger.error(errorText, ex);
            throw new OperationFailedException(
                errorText,
                OperationFailedException.GENERAL_ERROR,
                ex);
        }
    }

    /**
     * Returns the MUCUser packet extension included in the packet or <tt>null</tt> if none.
     *
     * @param packet the packet that may include the MUCUser extension.
     * @return the MUCUser found in the packet.
     */
    private MUCUser getMUCUserExtension(Stanza packet)
    {
        if (packet != null)
        {
            // Get the MUC User extension
            return (MUCUser) packet.getExtensionElement("x",
                "http://jabber.org/protocol/muc#user");
        }
        return null;
    }

    /**
     * Adds <tt>listener</tt> to the list of listeners registered to receive
     * events upon modification of chat room properties such as its subject
     * for example.
     *
     * @param listener the <tt>ChatRoomChangeListener</tt> that is to be
     * registered for <tt>ChatRoomChangeEvent</tt>-s.
     */
    public void addPropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        synchronized(propertyChangeListeners)
        {
            if (!propertyChangeListeners.contains(listener))
                propertyChangeListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of listeneres current
     * registered for chat room modification events.
     *
     * @param listener the <tt>ChatRoomChangeListener</tt> to remove.
     */
    public void removePropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        synchronized(propertyChangeListeners)
        {
            propertyChangeListeners.remove(listener);
        }
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time
     * a new message is received on this chat room.
     *
     * @param listener a <tt>MessageListener</tt> that would be notified
     *   every time a new message is received on this chat room.
     */
    public void addMessageListener(ChatRoomMessageListener listener)
    {
        synchronized(messageListeners)
        {
            if (!messageListeners.contains(listener))
                messageListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     *
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(ChatRoomMessageListener listener)
    {
        synchronized(messageListeners)
        {
            messageListeners.remove(listener);
        }
    }

    /**
     * Adds a listener that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     *
     * @param listener a participant status listener.
     */
    public void addMemberPresenceListener(
        ChatRoomMemberPresenceListener listener)
    {
        synchronized(memberListeners)
        {
            if (!memberListeners.contains(listener))
            {
                memberListeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener that was being notified of changes in the status of
     * other chat room participants such as users being kicked, banned, or
     * granted admin permissions.
     *
     * @param listener a participant status listener.
     */
    public void removeMemberPresenceListener(
        ChatRoomMemberPresenceListener listener)
    {
        synchronized(memberListeners)
        {
            memberListeners.remove(listener);
        }
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *   subject.
     * @return the newly created message.
     */
    public ImMessage createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MessageJabberImpl(
                new String(content),
                contentType,
                contentEncoding,
                subject,
                false,
                false,
                false);
    }

    /**
     * Create a Message instance for sending a simple text messages with
     * default (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @param messageUID the UID of the message.
     * @return Message the newly created message
     */
    public ImMessage createMessage(String messageText, String messageUID)
    {
        ImMessage msg
            = new MessageJabberImpl(
                messageText,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING,
                null,
                messageUID,
                false,
                false,
                false);

        return msg;
    }

    /**
     * Returns a <tt>List</tt> of <tt>Member</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>Member</tt> corresponding to all room
     *   members.
     */
    public List<ChatRoomMember> getMembers()
    {
        synchronized (members)
        {
            return new LinkedList<>(members.values());
        }
    }

    /**
     * Adds the specified map of members to the map in this class. Should only
     * be used in testing.
     *
     * @param members A list of members to set as members of this chat room
     */
    @VisibleForTesting
    protected void setMembers(Map<String, ChatRoomMemberJabberImpl> members)
    {
        synchronized (this.members)
        {
            this.members.putAll(members);
        }
    }

    /**
     * Returns a <tt>List</tt> of JIDs corresponding to all current owners of
     * this room.
     *
     * @return a <tt>List</tt> of JIDs corresponding to all current owners of
     * this room.
     */
    private List<Jid> getOwnerJids()
    {
        String loggableIdentifier = sanitiseChatRoom(getIdentifier());
        logger.debug("Getting owner JIDs for chat room " + loggableIdentifier);
        List<Jid> ownerJids = new ArrayList<>();
        try
        {
            Collection<Affiliate> owners = multiUserChat.getOwners();

            if (owners != null)
            {
                for (Affiliate owner : owners)
                {
                    Jid ownerJid = owner.getJid();
                    if ((ownerJid != null) && (!ownerJid.toString().isEmpty()))
                    {
                        ownerJids.add(ownerJid);
                    }
                }
            }
        }
        catch (XMPPErrorException | NoResponseException | NotConnectedException
            | InterruptedException ex)
        {
            logger.error(
                "Failed to get list of owners JIDs for chat room: " + loggableIdentifier, ex);
        }

        logger.debug("Chat room found with " + ownerJids.size() + " owner JIDs");

        return ownerJids;
    }

    /**
     * Returns the number of participants that are currently in this chat
     * room.
     *
     * @return int the number of <tt>Contact</tt>s, currently participating
     *   in this room.
     */
    public int getMembersCount()
    {
        return multiUserChat.getOccupantsCount();
    }

    /**
     * Returns the identifier of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>EntityBareJid</tt> containing the identifier of this
     *   <tt>ChatRoom</tt>.
     */
    public EntityBareJid getIdentifier()
    {
        return multiUserChat.getRoom();
    }

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     *   context of the local chat room.
     */
    public String getUserNickname()
    {
        Resourcepart nickname = multiUserChat.getNickname();
        return nickname != null ? nickname.toString() : null;
    }

    /**
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     *
     * @return the room subject or <tt>null</tt> if the user hasn't joined
     *   the room or the room does not have a subject yet.
     */
    public String getSubject()
    {
        String subject = this.multiUserChat.getSubject();

        // If the server returns null or an empty string, return 'oldSubject'
        // (the most recent subject that we've been told about).  If we don't
        // have that already, try to get it from config.
        if (subject == null || subject.trim().isEmpty())
        {
            synchronized (oldSubjectLock)
            {
                if (oldSubject == null)
                {
                    oldSubject = ConfigurationUtils.getChatRoomProperty(
                                  provider, getIdentifier().toString(), SUBJECT_PROPERTY_NAME);
                }

                subject = oldSubject;
            }
        }

        if (subject != null && subject.trim().isEmpty())
        {
            // Make sure we return null rather than an empty string if we don't
            // have a subject.
            subject = null;
        }

        return subject;
    }

    /**
     * Returns the date when we were invited to this chat room (will be null if
     * we weren't invited since the client was last restarted).
     *
     * @return the date when we were invited to this chat room.
     */
    public Date getInvitedDate()
    {
        return invitedDate;
    }

    /**
     * Sets the date when we were invited to this chat room.
     *
     * @param date the date when we were invited to this chat room.
     */
    public void setInvitedDate(Date date)
    {
        invitedDate = date;
    }

    /**
     * Invites another user to this room.
     *
     * @param userAddress the address of the user to invite to the room.(one
     *   may also invite users not on their contact list).
     * @param reason a reason, subject, or welcome message that would tell
     *   the user why they are being invited.
     */
    public void invite(String userAddress, String reason)
    {
        invite(userAddress, reason, false);
    }

    /**
     * Invites another user to this room.
     *
     * @param userAddress the address of the user to invite to the room as a
     *   string.(one may also invite users not on their contact list).
     * @param reason a reason, subject, or welcome message that would tell
     *   the user why they are being invited.
     * @param suppressTimeoutWarning if true, we won't warn the user if the
     * invitation times out without a response.
     */
    public void invite(String userAddress,
                       String reason,
                       boolean suppressTimeoutWarning)
    {
        if (userAddress == null)
        {
            logger.error("Ignoring invitation to null user address");
            return;
        }

        try
        {
            // Convert the user address to lower case to avoid case mis-matches
            // between our local contact list and the server roster, as we can
            // invite someone to their jid in lower case, even if the jid on the
            // server is stored in a different case.
            invite(JidCreate.entityBareFrom(userAddress.toLowerCase()),
                   reason,
                   suppressTimeoutWarning);
        }
        catch (XmppStringprepException ex)
        {
            logger.error("Could not invite non JID address: " +
                         sanitiseChatAddress(userAddress), ex);
        }
    }

    /**
     * Invites another user to this room.
     *
     * @param userAddress the address of the user to invite to the room as a
     *   JID.(one may also invite users not on their contact list).
     * @param reason a reason, subject, or welcome message that would tell
     *   the user why they are being invited.
     * @param suppressTimeoutWarning if true, we won't warn the user if the
     * invitation times out without a response.
     */
    private void invite(EntityBareJid userAddress,
                        String reason,
                        boolean suppressTimeoutWarning)
    {
        // All participants in the chat room are set to be owners.  This is
        // because their ownership persists over going offline so we can
        // tell who might return to the chat room.  Therefore, before inviting
        // someone to a chat room, we must make them an owner, to be sure that
        // all clients recognise that they have joined the chat room.
        try
        {
            logger.info("Trying to grant ownership and invite " +
                        sanitiseChatAddress(userAddress.toString()));
            // Here, only the 'grantOwnership' method might throw the
            // XMPPException, but both method calls are in the try block so
            // that, if granting ownership fails, we don't try to invite.
            multiUserChat.grantOwnership(userAddress);

            multiUserChat.invite(userAddress, reason);

            // Also send a direct invite, as it will still be sent to the user if they go
            // offline before receiving the standard invite.
            multiUserChat.inviteDirectly(userAddress);

            // Send a message to inform the other chat room members that we've
            // just invited this user to the chat.
            sendMembershipMessage(userAddress, GroupMembershipAction.joined);

            // Add the user to the list of participants now, in case they don't
            // respond to the invite immediately.  This ensures consistency
            // with the list of participants that the other members of the chat
            // room see, as they will receive notification that the user has
            // become an owner and show them in their list of participants
            // immediately, but we won't receive that notification, as we
            // granted their ownership.
            memberListener.participantJoined(JidCreate.entityFullFrom(getIdentifier(),
                Resourcepart.from(userAddress.toString())));
        }
        catch (XMPPErrorException | NotConnectedException |
            InterruptedException | NoResponseException | XmppStringprepException ex)
        {
            logger.error("Error inviting " +
                         sanitiseChatAddress(userAddress.toString()) +
                         "to chatroom.", ex);
        }
    }

    /**
     * Sends a 'special' message to notify the other members of the chat room
     * that the given user either is being invited to or is permanently leaving
     * this chat room.
     *
     * @param userAddress The address of the user who either has been invited
     * or is leaving
     * @param action A GroupMembershipAction from JabberActivator indicating
     * whether the user is joining, leaving or creating the chat room.
     */
    protected void sendMembershipMessage(Jid userAddress,
                                         GroupMembershipAction action)
    {
        try
        {
            // We need to construct the body of the group membership message in
            // the following format:
            // "JID=username@imdomain&Action=created/left/joined"
            // First, get the value of the action parameter for the message based on
            // the passed in value ("&Action=created/left/joined").
            String actionString =
                JabberActivator.GROUP_MEMBERSHIP_ACTION_PARM + action;

            // Then construct the whole message body
            String messageText = JabberActivator.GROUP_MEMBERSHIP_JID_PARM +
                                 userAddress +
                                 actionString;

            // Create the full message
            ImMessage message = opSetSpecialMsg.createSpecialMessage(
                JabberActivator.GROUP_MEMBERSHIP_ID, messageText);

            // Don't fire an event when sending this message, otherwise
            // we'll process it twice - once for the
            // ChatRoomMessageDeliveryEvent that would be generated and
            // once for the ChatRoomMessageReceivedEvent that will be
            // generated when we receive it in the chat room.
            logger.debug("Sending " + message + ", action = " + action +
                                             ", chatroom = " + sanitiseChatRoom(getIdentifier()));
            sendMessage(message, false);
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to send chat room membership message for " +
                                 sanitiseChatAddress(userAddress) + ", action = " + action, ex);
        }
    }

    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join()} methods).
     *
     * @return true if currently we're currently in this chat room and false
     *   otherwise.
     */
    public boolean isJoined()
    {
        return multiUserChat.isJoined();
    }

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an
     *   error occurs while joining the room.
     */
    public void join()
        throws OperationFailedException
    {
        joinAs(provider.getOurJid().toString());
    }

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     *
     * @param joinDate the date when the user joined the chat room, possibly on
     * another client
     * @throws OperationFailedException with the corresponding code if an
     *   error occurs while joining the room.
     */
    public void join(Date joinDate)
        throws OperationFailedException
    {
        joinAs(provider.getOurJid().toString(), null, joinDate);
    }

    /**
     * Joins this chat room with the specified nickname and password so that
     * the user would start receiving events and messages for it.
     *
     * @param userNickname the nickname to use.
     * @param password a password necessary to authenticate when joining the
     *   room.
     * @throws OperationFailedException with the corresponding code if an
     *   error occurs while joining the room.
     */
    public void joinAs(String userNickname, byte[] password)
        throws OperationFailedException
    {
        joinAs(userNickname, password, null);
    }

    /**
     * Joins this chat room with the specified nickname and password so that
     * the user would start receiving events and messages for it.
     * @param password a password necessary to authenticate when joining the
     *   room.
     * @param userNickname the XEP-0045 nickname to use.
     * @param joinDate the date when the user joined the chat room, possibly on
     * another client
     *
     * @throws OperationFailedException with the corresponding code if an
     *   error occurs while joining the room.
     */
    public synchronized void joinAs(String userNickname, byte[] password, Date joinDate)
        throws OperationFailedException
    {
        String chatRoomJid;
        try
        {
            this.assertConnected();
            chatRoomJid = getIdentifier().toString();
        }
        catch (Exception ex)
        {
            String errorMessage = "Failed to join room with nickname: " +
                                  sanitisePeerId(nickname);

            logger.error(errorMessage, ex);

            throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.GENERAL_ERROR,
                    ex);
        }

        try
        {
            // If no nickname is provided, use our full jid.
            nickname = (userNickname == null) ? provider.getOurJid().toString() : userNickname;
            logger.debug("Joining chat room as nickname " +
                         sanitisePeerId(nickname));

            // Check the nickname can be validly expressed as a resourcepart now.
            // Unless an empty string or a string greater than 256 bytes was
            // passed in for userNickname this should always succeed.
            // See https://xmpp.org/extensions/xep-0029.html#sect-idm44926487430880
            Resourcepart nicknameResource;
            try
            {
                nicknameResource = Resourcepart.from(nickname);
            }
            catch (XmppStringprepException ex)
            {
                throw new OperationFailedException("Nickname is invalid", 0, ex);
            }

            if (multiUserChat.isJoined())
            {
                logger.debug("Already joined chat room, jid: " +
                             sanitiseChatRoom(chatRoomJid));
                if (!multiUserChat.getNickname().equals(nicknameResource))
                {
                    multiUserChat.changeNickname(nicknameResource);
                }
                else
                {
                    logger.debug("Nickname not changed so nothing to do, " +
                                 "chat room jid: " +
                                 sanitiseChatRoom(chatRoomJid));
                    return;
                }
            }
            else
            {
                String pwd = (password == null) ? null : new String(password);

                // We need to create a MucEnterConfiguration object that we can
                // pass in to the 'join' method to allow us to request
                // historical messages that have been sent in this chat since
                // we were last online.  First, if we can find any messages in
                // our local history for this chat room (and we did not leave
                // the chat room after the most recent message was sent or
                // received), request all messages since the most recent
                // message that we received.  If there is no local history for
                // this chat room, we haven't been in it before so we're not
                // allowed to see historical messages and we just request
                // history from the timestamp when we were invited
                // (invitedDate).  If we don't have an invited date, we just
                // use the current timestamp.
                MucEnterConfiguration.Builder historyBuilder =
                    multiUserChat.getEnterConfigurationBuilder(nicknameResource);

                Date historyRequestDate = joinDate;

                if (historyRequestDate == null)
                {
                    // Start by setting the date from which we will request
                    // message history to the invited date, then update it
                    // later if we find we've already received messages in this
                    // chat room.
                    historyRequestDate = invitedDate;

                    // Get the most recent group chat message for this chat room
                    // from history.
                    ChatRoomMessageEvent lastMessage =
                              messageHistoryService.findLatestChatMessage(this);

                    // If we haven't received any group chat messages in this
                    // chat room, look for the oldest status message, as that
                    // will have been created when we were first invited
                    // to/created the chat room. Only do this if we haven't got
                    // an invitedDate for the chat room, as we may want to
                    // request history from before our first status message, if
                    // the group chat invitation was sent to us when we were
                    // offline.
                    if ((lastMessage == null) && (invitedDate == null))
                    {
                        lastMessage =
                                   messageHistoryService.findOldestStatus(this);
                    }

                    if (lastMessage != null && !lastMessage.isConversationClosed())
                    {
                        // We didn't leave this chat room since the last message
                        // was sent/received, so we can request all history
                        // since the last message.
                        historyRequestDate = lastMessage.getTimestamp();
                        logger.debug("Chat room not left - requesting history" +
                                     " since last message: " + historyRequestDate +
                                     ", chat room jid: " +
                                     sanitiseChatRoom(chatRoomJid));
                    }

                    if (historyRequestDate == null)
                    {
                        // We don't have an invited date and we haven't found
                        // message history, so request history from the current
                        // time.
                        logger.debug("LastMessageDate is null, invitedDate = " +
                                     invitedDate + ", chat room jid: " +
                                     sanitiseChatRoom(chatRoomJid));
                        historyRequestDate = new Date();
                    }
                }
                else
                {
                    logger.debug(
                        "Received joined date from chat room manager: " +
                                                               historyRequestDate);
                }

                historyBuilder.requestHistorySince(historyRequestDate);

                // Check whether any outstanding join request succeeded
                // since we checked at the start of this method.
                if (multiUserChat.isJoined())
                {
                    logger.debug("Already joined chat room so not joining again, jid :" +
                                 sanitiseChatRoom(chatRoomJid));
                    return;
                }

                historyBuilder.withPassword(pwd);
                historyBuilder.timeoutAfter(provider.getConnection().getReplyTimeout());

                // We joined the chat, so clear any members we think
                // are in the room and set the list of members to match the
                // list of owners on the server.
                synchronized (members)
                {
                    members.clear();
                }

                multiUserChat.join(historyBuilder.build());

                // This is synchronized on the list of members to make sure
                // any ownership change events or join events
                // that we receive after joining are processed
                // after we have finished processing the initial list of owners.
                synchronized (members)
                {
                    members.clear();

                    List<Jid> latestParticipants = getOwnerJids();
                    for (Jid latestParticipant : latestParticipants)
                    {
                        logger.debug("Adding to the list " +
                                     sanitiseChatAddress(latestParticipant.toString()));
                        ChatRoomMemberJabberImpl newMember =
                            new ChatRoomMemberJabberImpl(
                                this,
                                Resourcepart.from(latestParticipant.toString()),
                                latestParticipant);

                        // Convert the participant key to lower case to
                        // avoid case mis-matches between our local contact
                        // list and the server roster.
                        members.put(latestParticipant.toString().toLowerCase(), newMember);
                    }
                }

                // We're joining the chat room so make sure it isn't marked as
                // closed in message history.
                messageHistoryService.setChatroomClosedStatus(this, false);

                // We've joined a new chatroom so increment our count of
                // active chatrooms and check whether we've reached the maximum.
                opSetMuc.updateActiveChatRooms(true);
                opSetMuc.checkForMaxActiveChatRooms();
            }

            ChatRoomMemberJabberImpl member =
                new ChatRoomMemberJabberImpl(this,
                                             nicknameResource,
                                             localJid);

            synchronized (members)
            {
                // Convert the participant key to lower case to avoid case
                // mis-matches between our local contact list and the server
                // roster.
                members.put(nickname.toLowerCase(), member);
            }

            // We don't specify a reason.
            opSetMuc.fireLocalUserPresenceEvent(
                this,
                localJid,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                null,
                joinDate);

            // Now that we've successfully completed everything that needs to
            // be done to finish joining the chat room, we can process any
            // packets that have already been received by the message listener.
            smackMessageListener.setChatRoomInitialized();
        }
        catch (XMPPErrorException ex)
        {
            String errorMessage;

            if(ex.getStanzaError() == null)
            {
                errorMessage
                    = "Failed to join room "
                      + sanitiseChatRoom(chatRoomJid)
                      + " with nickname: "
                      + sanitisePeerId(nickname);

                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.GENERAL_ERROR,
                    ex);
            }
            else if(ex.getStanzaError().getCondition() ==
                StanzaError.Condition.conflict)
            {
                errorMessage
                    = "Failed to join chat room "
                      + sanitiseChatRoom(chatRoomJid)
                      + " with nickname: "
                      + sanitisePeerId(nickname)
                      + ". There was a conflict.";

                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.IDENTIFICATION_CONFLICT,
                    ex);
            }
            else if(ex.getStanzaError().getCondition() ==
                StanzaError.Condition.not_authorized)
            {
                errorMessage
                    = "Failed to join chat room "
                      + sanitiseChatRoom(chatRoomJid)
                      + " with nickname: "
                      + sanitisePeerId(nickname)
                      + ". The chat room requests a password.";

                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.AUTHENTICATION_FAILED,
                    ex);
            }
            else if(ex.getStanzaError().getCondition() ==
                StanzaError.Condition.registration_required)
            {
                errorMessage
                    = "Failed to join chat room "
                      + sanitiseChatRoom(chatRoomJid)
                      + " with nickname: "
                      + sanitisePeerId(nickname)
                      + ". The chat room requires registration.";

                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.REGISTRATION_REQUIRED,
                    ex);
            }
            else if(ex.getStanzaError().getCondition() ==
                StanzaError.Condition.forbidden)
            {
                errorMessage
                    = "Failed to join chat room "
                      + sanitiseChatRoom(chatRoomJid)
                      + " with nickname: "
                      + sanitisePeerId(nickname)
                      + ". You are not permitted to join the chat room.";

                // Only log this at debug level, as it is expected to get this
                // response if someone else removed us from the chat room while
                // we were offline.  Therefore, add a status message to let the
                // user know that they've been removed from the chat room, then
                // tidy up to make sure we treat this as a left chat room.
                logger.debug(errorMessage, ex);
                messageHistoryService.setRemovedFromChatroom(this);
                cleanUpChatRoom(
                    new Date(), LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED, null);
            }
            else
            {
                errorMessage
                    = "Failed to join room "
                      + sanitiseChatRoom(chatRoomJid)
                      + " with nickname: "
                      + sanitisePeerId(nickname)
                      + " and stanza error " + ex.getStanzaError();

                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.GENERAL_ERROR,
                    ex);
            }
        }
        catch (Throwable ex)
        {
            String errorMessage = "Failed to join room "
                                  + sanitiseChatRoom(chatRoomJid)
                                  + " with nickname: "
                                  + sanitisePeerId(nickname);

            logger.error(errorMessage, ex);

            throw new OperationFailedException(
                errorMessage,
                OperationFailedException.GENERAL_ERROR,
                ex);
        }
    }

    /**
     * Joins this chat room with the specified nickname so that the user
     * would start receiving events and messages for it.
     *
     * @param userNickname the nickname to use.
     * @throws OperationFailedException with the corresponding code if an
     *   error occurs while joining the room.
     */
    public void joinAs(String userNickname)
        throws OperationFailedException
    {
        this.joinAs(userNickname, null, null);
    }

    /**
     * Returns that <tt>ChatRoomJabberRole</tt> instance corresponding to the
     * <tt>smackRole</tt>.
     *
     * @param smackRole the smack role as returned by
     * <tt>Occupant.getRole()</tt>.
     * @return ChatRoomMemberRole
     */
    public static ChatRoomMemberRole smackRoleToScRole(MUCRole smackRole)
    {
        if (smackRole != null)
        {
            if (smackRole == MUCRole.moderator)
            {
                return ChatRoomMemberRole.MODERATOR;
            }
            else if (smackRole == MUCRole.participant)
            {
                return ChatRoomMemberRole.MEMBER;
            }
        }

        return ChatRoomMemberRole.GUEST;
    }

    /**
     * Returns the <tt>ChatRoomMember</tt> corresponding to the given smack
     * participant.
     *
     * @param participant the full participant name
     * (e.g. sc-testroom@conference.voipgw.u-strasbg.fr/testuser)
     * @param create if true, create a new <tt>ChatRoomMember</tt> if one
     * doesn't already exist.
     * @return the <tt>ChatRoomMember</tt> corresponding to the given smack
     * participant
     */
    public ChatRoomMemberJabberImpl getMemberForSmackParticipant(
        EntityFullJid participant, boolean create)
    {
        if (participant == null)
        {
            logger.warn(
                "Not looking for member for null participant in chat room " +
                                                               sanitiseChatRoom(getIdentifier()));
            return null;
        }

        logger.debug("Looking for existing member for " +
                     sanitisePeerId(participant) + " in " + sanitiseChatRoom(getIdentifier()));
        ChatRoomMemberJabberImpl participantMember = null;

        // Participant will be in the format chatroomid/nickname.  This
        // extracts the nickname from that string.
        String participantName = participant.getResourcepart().toString();

        Iterator<ChatRoomMember> chatRoomMembers = getMembers().iterator();

        while(chatRoomMembers.hasNext())
        {
            ChatRoomMember member = chatRoomMembers.next();

            String memberName = member.getName();
            String memberAddress = member.getContactAddressAsString();

            // Make sure we try to match both qualified and unqualified
            // nicknames and try matching the fully qualified and unqualified
            // nicknames, as well as the contact's address.
            if (participantName.equalsIgnoreCase(memberName) ||
                participantName.equalsIgnoreCase(getNickName(memberName)) ||
                participant.toString().equalsIgnoreCase(memberAddress) ||
                participantName.equalsIgnoreCase(memberAddress))
            {
                participantMember = (ChatRoomMemberJabberImpl) member;
                break;
            }
        }

        if ((participantMember == null) && create)
        {
            // We didn't find an existing member and we've been asked to
            // create one, so do so.
            participantMember = createMemberForSmackParticipant(participant);
        }

        return participantMember;
    }

    /**
     * Creates a new <tt>ChatRoomMember</tt> corresponding to the given smack
     * participant.
     *
     * @param participant the full participant name
     * (e.g. sc-testroom@conference.voipgw.u-strasbg.fr/testuser)
     * @return the new <tt>ChatRoomMember</tt> corresponding to the given smack
     * participant
     */
    private ChatRoomMemberJabberImpl createMemberForSmackParticipant(
        EntityFullJid participant)
    {
        logger.debug("Trying to create a new member for " + sanitisePeerId(participant));
        ChatRoomMemberJabberImpl newMember = null;
        Boolean createMember = true;

        Resourcepart occupantNick = null;
        Jid occupantJid = null;

        Occupant occupant = multiUserChat.getOccupant(participant);

        if (occupant == null)
        {
            logger.info(
                "No occupant found - figuring out member details for " + sanitisePeerId(participant));

            // Participant will be in the format chatroomid/nickname.  This
            // extracts the nickname from that string.
            String participantName = participant.getResourceOrEmpty().toString();

            if (!participantName.isEmpty())
            {
                // We've found a nickname so save that.
                occupantNick = participant.getResourcepart();

                // If the nickname isn't full qualified, try to construct a
                // fully qualified jid using the nickname and the IM server
                // address.
                if (!participantName.contains("@"))
                {
                    String imServerAddress =
                        provider.getAccountID().getAccountPropertyString(
                            ProtocolProviderFactory.SERVER_ADDRESS);

                    participantName = participantName + "@" + imServerAddress;
                }

                try
                {
                    occupantJid = JidCreate.entityBareFrom(participantName);
                }
                catch (XmppStringprepException ex)
                {
                    logger.error("Error creating Jid from participant name" + sanitisePeerId(participantName)
                        + ". Can't create member", ex);
                    createMember = false;
                }
            }
            else
            {
                logger.debug("No participant name - this must be the chatroom itself" + sanitisePeerId(participant));
                return null;
            }
        }
        else
        {
            // We found an occupant so we can create a member based on its
            // nickname and jid.
            occupantNick = occupant.getNick();
            occupantJid = occupant.getJid();

            // If the Nick is the same as the Jid the server won't bother
            // returning a Jid.  Therefore, if the Jid is null, set it to
            // the Nick.
            if (occupantJid == null)
            {
                try
                {
                    occupantJid = JidCreate.from(occupantNick);
                }
                catch (XmppStringprepException ex)
                {
                    logger.error("Error creating Jid from occupant nickname" +
                                 sanitisePeerId(occupantNick) +
                                 ". Can't create member", ex);
                    createMember = false;
                }
            }
        }

        if (occupantNick != null && createMember)
        {
            logger.debug("Creating new member with jid " + sanitisePeerId(occupantJid));
            newMember = new ChatRoomMemberJabberImpl(this,
                occupantNick,
                occupantJid);
        }
        else
        {
            logger.warn("Occupant nickname was null, so not creating a new ChatRoomMember");
        }

        return newMember;
    }

    @Override
    public void leave(String eventType, String reason)
    {
        leave(false, eventType, reason);
    }

    @Override
    public void leave(boolean sendMessage, String eventType, String reason)
    {
        leave(sendMessage, new Date(), eventType, reason);
    }

    @Override
    public void leave(boolean sendMessage,
                      Date leaveDate,
                      String eventType,
                      String reason)
    {
        String identifier = getIdentifier().toString();

        // Send an analytic of us leaving a group chat, with the number
        // of active group chats as a parameter.
        List<AnalyticsParameter> params = new ArrayList<>();

        params.add(new AnalyticsParameterSimple(
            AnalyticsParameter.NAME_COUNT_GROUP_IMS,
            String.valueOf(opSetMuc.getActiveChatRooms())));

        JabberActivator.getAnalyticsService().onEvent(
            AnalyticsEventType.LEAVE_GROUP_IM, params);

        List<Jid> ownerJids = new ArrayList<>();

        if (sendMessage)
        {
            // Send a message to inform the other chat room members that we're
            // about to leave the chat.
            sendMembershipMessage(localJid, GroupMembershipAction.left);

            // Only try and get owner jids if we've been asked to send a
            // message, as otherwise we're leaving the chat because we've
            // already left on another client so we won't be an owner anymore
            // and won't have permission to get owner jids.
            ownerJids = getOwnerJids();
        }

        // If we are the only user left in the group chat, destroy the chat
        // room on the server as no-one will be able to re-join once everyone
        // has left.
        if ((ownerJids.size() == 1) && (ownerJids.get(0).equals(localJid)))
        {
            logger.info("Destroying chat room " + sanitiseChatRoom(identifier));
            destroy(leaveDate, eventType, reason);
        }
        else
        {
            logger.debug("Leaving chat room " + sanitiseChatRoom(identifier));
            leaveChatRoom(leaveDate, eventType, reason);
        }
    }

    /**
     * Leave this chat room.
     * @param leaveDate the date when the user left the chat room, possibly on
     * another client
     * @param eventType The LocalUserChatRoomPresenceChangeEvent that caused
     * the user to leave.
     * @param reason The reason why the user left, may be null.
     */
    private void leaveChatRoom(Date leaveDate, String eventType, String reason)
    {
        String identifier = getIdentifier().toString();
        logger.info("Trying to leave chatroom " +
                    sanitiseChatRoom(identifier) + ", " +
                    logHasher(getSubject()));

        try
        {
            // When we leave we must revoke our ownership of the chat room, as
            // other clients will use this to know that we have permanently
            // left the chat room, rather than just going offline.
            multiUserChat.revokeOwnership(localJid);
        }
        catch (XMPPErrorException ex)
        {
            // Only log at debug level, as this will happen if another client
            // logged in with our ID already revoked our ownership.
            logger.debug("Failed to revoke ownership in chatroom " +
                         sanitiseChatRoom(identifier) + ", " +
                         logHasher(getSubject()) +
                         ". This may be due" +
                         "to ownership already being revoked", ex);
        }
        catch (NoResponseException | NotConnectedException | InterruptedException ex)
        {
            logger.error("Failed to revoke ownership in chatroom " +
                         sanitiseChatRoom(identifier) + ", " +
                         logHasher(getSubject()), ex);
        }

        try
        {
            multiUserChat.leave();
        }
        catch (MucNotJoinedException ex)
        {
            // The net result is we're not in the chatroom, so fine
        }
        catch (NotConnectedException | NoResponseException | XMPPErrorException
            | InterruptedException ex)
        {
            logger.error("Failed to leave chatroom " +
                         sanitiseChatRoom(identifier) + ", " +
                         logHasher(getSubject()), ex);
        }

        cleanUpChatRoom(leaveDate, eventType, reason);
    }

    @Override
    public void destroy(Date leaveDate, String eventType, String reason)
    {
        String identifier = getIdentifier().toString();
        try
        {
            logger.info("Trying to destroy chatroom " +
                        sanitiseChatRoom(identifier) + ", " +
                        logHasher(getSubject()));

            // No need to revoke our ownership, as we're destroying the room.
            multiUserChat.destroy(null, null);
            cleanUpChatRoom(leaveDate, eventType, reason);
        }
        catch (XMPPErrorException ex)
        {
            // Only log at debug level, as we may have just failed to destroy
            // because we don't have permission or because the chatroom has
            // been deleted by someone else.
            logger.debug("Failed to destroy chatroom " +
                         sanitiseChatRoom(identifier) + ", " +
                         logHasher(getSubject()) + " : " +
                         ex.getMessage(), ex);

            // In case we just didn't have permission to destroy the chatroom,
            // try to leave it and let the owner destroy it later.
            leaveChatRoom(leaveDate, eventType, reason);
        }
        catch (NoResponseException | NotConnectedException | InterruptedException ex)
        {
            logger.error("Failed to destroy chatroom " +
                         sanitiseChatRoom(identifier) + ", " +
                         logHasher(getSubject()) + " : " +
                         ex.getMessage(), ex);
        }
    }

    /**
     * Cleans up the chat room after the user has left.  This does things like
     * remove the chat room from the roster, clears the list of members,
     * deletes the chat room from config, marks it as closed in message history
     * and fires a local user presence event.
     * @param leaveDate the date when the user left the chat room, possibly on
     * another client
     * @param eventType The LocalUserChatRoomPresenceChangeEvent that caused
     * the user to leave.
     * @param reason The reason why the user left, may be null.
     */
    private void cleanUpChatRoom(Date leaveDate, String eventType, String reason)
    {
        logger.debug("Cleaning up chat room: " + sanitiseChatRoom(getIdentifier()));
        // Remove this chat room ID from our roster on the server so that
        // any other clients logged on to this user account will be told that
        // it has been deleted and leave the chat room.
        if (opSetPersPres != null)
        {
            opSetPersPres.getServerStoredContactList().
                                     removeChatRoomFromRoster(getIdentifier().toString());
        }

        // We've left a chatroom so decrement our count of active chatrooms.
        opSetMuc.updateActiveChatRooms(false);
        // Also clear the invited date so that it will be set if we are invited
        // back into the chat room.
        invitedDate = null;
        isActive = false;

        synchronized (members)
        {
            members.clear();
        }

        this.provider.getConnection().removeStanzaListener(
            invitationRejectionListeners);

        // Delete the chat room from config
        ConfigurationUtils.deleteChatRoom(provider, getIdentifier().toString());

        // Set the chat room as closed in history
        messageHistoryService.setChatroomClosedStatus(this, true);

        // Remove the chat room from the cache.  This prevents memory leaks and
        // makes sure we don't try to reuse a dud MUC if we are re-invited to
        // the chat
        opSetMuc.removeChatRoom(this);

        opSetMuc.fireLocalUserPresenceEvent(
            this, localJid, eventType, reason, leaveDate);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    public void sendMessage(ImMessage message)
        throws OperationFailedException
    {
        sendMessage(message, true);
    }

    @Override
    public void sendMessage(ImMessage message, boolean fireEvent)
        throws OperationFailedException
    {
         try
         {
             assertConnected();

             MessageBuilder msg = provider.getConnection().getStanzaFactory().buildMessageStanza();

             msg.setBody(message.getContent());
             //msg.addExtension(new Version());

             MessageEventManager.
                 addNotificationsRequests(msg.build(), true, false, false, true);

             // We send only the content because it doesn't work if we send the
             // Message object.
             multiUserChat.sendMessage(message.getContent());

             if (fireEvent)
             {
                 ChatRoomMessageDeliveredEvent msgDeliveredEvt
                     = new ChatRoomMessageDeliveredEvent(
                         message,
                         this,
                         getIdentifier().toString(),
                         getSubject(),
                         new Date(),
                         false,
                         MessageEvent.GROUP_MESSAGE);
                 // A message has been sent so set the chat room as active
                 isActive = true;

                 fireMessageEvent(msgDeliveredEvt);
             }
             sendTelemetryImSendMessage(true, message);
         }
         catch (NotConnectedException | InterruptedException ex)
         {
             logger.error("Failed to send message " + message, ex);

             if (!message.getContent().contains(JabberActivator.GROUP_MEMBERSHIP_JID_PARM))
             {
                 ChatRoomMessageDeliveredEvent msgDeliveredEvt
                         = new ChatRoomMessageDeliveredEvent(
                         message,
                         this,
                         getIdentifier().toString(),
                         getSubject(),
                         new Date(),
                         false,
                         MessageEvent.GROUP_MESSAGE);
                 msgDeliveredEvt.setFailed(true);
                 sendTelemetryImSendMessage(false, message);
                 fireMessageEvent(msgDeliveredEvt);
             }

             throw new OperationFailedException(
                 "Failed to send message " + message,
                 OperationFailedException.GENERAL_ERROR,
                 ex);
         }
    }

    /**
     * Sends an IM_SEND_MESSAGE telemetry event to Azure
     */
    private void sendTelemetryImSendMessage(boolean delivered, ImMessage message)
    {
        JabberActivator.getInsightsService().logEvent(
                InsightsEventHint.JABBER_IM_SEND_MESSAGE.name(),
                Map.of(
                        JabberParameterInfo.MESSAGE.name(), message,
                        JabberParameterInfo.MESSAGE_DELIVERED.name(), delivered,
                        JabberParameterInfo.MESSAGE_TYPE.name(), MessageEvent.GROUP_MESSAGE
                        )
        );
    }

    /**
     * Sets the subject of this chat room.
     *
     * @param subject the new subject that we'd like this room to have
     * @throws OperationFailedException
     */
    public void setSubject(String subject)
        throws OperationFailedException
    {
        try
        {
            multiUserChat.changeSubject(subject);
        }
        catch (IllegalStateException | NoResponseException | NotConnectedException
            | InterruptedException | XMPPErrorException ex)
        {
            logger.error("Failed to change subject for chat room" +
                         sanitiseChatRoom(getIdentifier()), ex);
            throw new OperationFailedException(
                "Failed to changed subject for chat room" +
                sanitiseChatRoom(getIdentifier()),
                OperationFailedException.FORBIDDEN,
                ex);
        }
    }

    /**
     * Returns a reference to the provider that created this room.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this room.
     */
    public ProtocolProviderService getParentProvider()
    {
        return provider;
    }

    /**
     * Returns local user role in the context of this chatroom.
     *
     * @return ChatRoomMemberRole
     */
    public ChatRoomMemberRole getUserRole()
    {
        if(this.role == null)
        {
            // If there is no nickname, there is no way to match anything.
            // Sometimes while joining we receive the presence and dispatch
            // the user role before the nickname is stored in multiUserChat.
            // Returning guest here is just for the event reporting the new
            // role and that the old role was guest
            if (multiUserChat.getNickname() == null)
            {
                return ChatRoomMemberRole.GUEST;
            }

            Occupant o = multiUserChat.getOccupant(
                JidCreate.entityFullFrom(
                    multiUserChat.getRoom(), multiUserChat.getNickname()));

            if(o == null)
                return ChatRoomMemberRole.GUEST;
            else
                this.role = smackRoleToScRole(o.getRole());
        }

        return this.role;
    }

    /**
     * Sets the new role for the local user in the context of this chatroom.
     *
     * @param role the new role to be set for the local user
     */
    public void setLocalUserRole(ChatRoomMemberRole role)
    {
        this.role = role;
    }

    /**
     * Instances of this class should be registered as
     * <tt>ParticipantStatusListener</tt> in smack and translates events .
     */
    private class MemberListener implements ParticipantStatusListener
    {
        /**
         * Called when an administrator or owner banned a participant from the
         * room. This means that banned participant will no longer be able to
         * join the room unless the ban has been removed.
         *
         * @param participant the participant that was banned from the room
         * (e.g. room@conference.jabber.org/nick).
         * @param actor the administrator that banned the occupant (e.g.
         * user@host.org).
         * @param reason the reason provided by the administrator to ban the
         * occupant.
         */
        @Override
        public void banned(EntityFullJid participant, Jid actor, String reason)
        {
            logger.info(sanitisePeerId(participant) +
                        " has been banned from " +
                        sanitiseChatRoom(getIdentifier()) + " chat room.");

            participantPermanentlyLeft(participant);
        }

        /**
         * Called when an owner grants administrator privileges to a user. This
         * means that the user will be able to perform administrative functions
         * such as banning users and edit moderator list.
         *
         * @param participant the participant that was granted administrator
         * privileges (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void adminGranted(EntityFullJid participant)
        {
            logger.debug("Admin granted to " + sanitisePeerId(participant));
        }

        /**
         * Called when an owner revokes administrator privileges from a user.
         * This means that the user will no longer be able to perform
         * administrative functions such as banning users and edit moderator
         * list.
         *
         * @param participant the participant that was revoked administrator
         * privileges (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void adminRevoked(EntityFullJid participant)
        {
            logger.debug("Admin revoked from " + sanitisePeerId(participant));
        }

        /**
         * Called when a new room occupant has joined the room. Note: Take in
         * consideration that when you join a room you will receive the list of
         * current occupants in the room. This message will be sent for each
         * occupant.
         *
         * @param participant the participant that has just joined the room
         * (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void joined(EntityFullJid participant)
        {
            logger.info(sanitisePeerId(participant) + " has joined the "
                + sanitiseChatRoom(getIdentifier()) + " chat room.");

            participantJoined(participant);
        }

        /**
         * Handles adding a new participant who has just joined the chat room.
         *
         * @param participant The participant who has joined the chat room.
         */
        private void participantJoined(EntityFullJid participant)
        {
            String participantName = participant.getResourcepart().toString();

            ChatRoomMemberJabberImpl member =
                createMemberForSmackParticipant(participant);

            // Convert the address to lower case to avoid case mis-matches
            // between our local contact list and the server roster.
            String occupantAddress = member.getContactAddressAsString().toLowerCase();

            synchronized (members)
            {
                // when somebody changes its nickname we first receive
                // event for its nickname changed and after that that has joined
                // we check is this already joined and if so we skip it
                if (members.containsKey(occupantAddress) ||
                    members.containsKey(participantName))
                {
                    logger.debug("Ignoring joined event as participant " +
                                 sanitisePeerId(participant) + " is already in members list");
                    return;
                }

                String localJidString = localJid.toString();

                if (localJidString.equalsIgnoreCase(occupantAddress) ||
                    localJidString.equalsIgnoreCase(participantName) ||
                    nickname.equalsIgnoreCase(occupantAddress) ||
                    nickname.equalsIgnoreCase(participantName))
                {
                    logger.debug(
                        "Ignoring joined event from ourself " + sanitisePeerId(participant));
                    return;
                }
            }

            logger.debug("Participant joined " + sanitisePeerId(participant));

            synchronized (members)
            {
                members.put(occupantAddress, member);
            }

            //we don't specify a reason
            fireMemberPresenceEvent(member,
                ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
        }

        /**
         * Called when a room occupant has left the room on its own. This means
         * that the occupant was neither kicked nor banned from the room.
         *
         * @param participant the participant that has left the room on its own.
         * (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void left(EntityFullJid participant)
        {
            logger.info(sanitisePeerId(participant) +
                        " has left the " + sanitiseChatRoom(getIdentifier())
                        + " chat room.");

            // Do nothing here as the user has just gone offline.  If they
            // left permanently, we will receive an event to say that their
            // membership has been revoked.
        }

        /**
         * Handles removing a participant who has permanently left the chat room.
         *
         * @param participant The participant who has joined the chat room.
         */
        private void participantPermanentlyLeft(EntityFullJid participant)
        {
            logger.debug("Participant permanently left chat room " +
                         sanitiseChatRoom(getIdentifier()) + ": " +
                         sanitisePeerId(participant));

            ChatRoomMemberJabberImpl member =
                getMemberForSmackParticipant(participant, true);

            ChatRoomMemberJabberImpl removed = null;

            synchronized (members)
            {
                // Convert the address to lower case to avoid case mis-matches
                // between our local contact list and the server roster.
                String memberAddress = member.getContactAddressAsString().toLowerCase();
                removed = members.remove(memberAddress);
                logger.debug("Removed chat room member " + sanitiseChatAddress(memberAddress) + "? " + removed);
            }

            // Only fire an event if we actually removed the member from the
            // list.
            if (removed != null)
            {
                fireMemberPresenceEvent(removed,
                    ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT, null);
            }
        }

        /**
         * Called when a participant changed his/her nickname in the room. The
         * new participant's nickname will be informed with the next available
         * presence.
         *
         * @param participant the participant that has changed his nickname
         * @param newNickname the new nickname that the participant decided to
         * use.
         */
        @Override
        public void nicknameChanged(EntityFullJid participant, Resourcepart newNickname)
        {
            // We don't show the nickname to the user, so we don't care about
            // changes.
            logger.debug("Nickname changed for " +
                         sanitisePeerId(participant) + " to " +
                         sanitisePeerId(newNickname));
        }

        /**
         * Called when an owner revokes a user ownership on the room. This
         * means that the user will no longer be able to change defining room
         * features as well as perform all administrative functions.
         *
         * @param participant the participant that was revoked ownership on the
         * room (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void ownershipRevoked(EntityFullJid participant)
        {
            logger.info("Ownership revoked from " +
                        sanitisePeerId(participant));

            // All participants in the chat room are set to be owners.  This is
            // because their ownership persists over going offline so we can
            // tell who might return to the chat room.  Therefore, when
            // someone wants to leave permanently, they revoke their ownership
            // so we need to mark this participant as permanently left.
            participantPermanentlyLeft(participant);
        }

        /**
         * Called when a room participant has been kicked from the room. This
         * means that the kicked participant is no longer participating in the
         * room.
         *
         * @param participant the participant that was kicked from the room
         * (e.g. room@conference.jabber.org/nick).
         * @param actor the moderator that kicked the occupant from the room
         * (e.g. user@host.org).
         * @param reason the reason provided by the actor to kick the occupant
         * from the room.
         */
        @Override
        public void kicked(EntityFullJid participant, Jid actor, String reason)
        {
            ChatRoomMember member
                = getMemberForSmackParticipant(participant, false);

            ChatRoomMember actorMember = null;
            try
            {
                actorMember =
                    getMemberForSmackParticipant((JidCreate.entityFullFrom(actor.toString())), false);
            }
            catch (XmppStringprepException ex)
            {
                // actorMember can be null, so don't worry too much if we
                // can't get a member from the actor Jid.
                logger.warn("Kick actor " + sanitisePeerId(actor.asEntityFullJidIfPossible()) +
                            " is not a full JID, unable to find member", ex);
            }

            if(member == null)
                return;

            String participantName = participant.getResourcepart().toString();

            // Convert the participant string to lower case to avoid case
            // mis-matches between our local contact list and the server roster.
            synchronized (members)
            {
                members.remove(participantName.toLowerCase());
            }

            fireMemberPresenceEvent(member, actorMember,
                ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED, reason);
        }

        /**
         * Called when an administrator grants moderator privileges to a user.
         * This means that the user will be able to kick users, grant and
         * revoke voice, invite other users, modify room's subject plus all the
         * partcipants privileges.
         *
         * @param participant the participant that was granted moderator
         * privileges in the room (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void moderatorGranted(EntityFullJid participant)
        {
            logger.debug("Moderator granted to " + sanitisePeerId(participant));
        }

        /**
         * Called when a moderator revokes voice from a participant. This means
         * that the participant in the room was able to speak and now is a
         * visitor that can't send messages to the room occupants.
         *
         * @param participant the participant that was revoked voice from the
         * room (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void voiceRevoked(EntityFullJid participant)
        {
            logger.debug("Voice revoked from " + sanitisePeerId(participant));
        }

        /**
         * Called when an administrator grants a user membership to the room.
         * This means that the user will be able to join the members-only room.
         *
         * @param participant the participant that was granted membership in
         * the room (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void membershipGranted(EntityFullJid participant)
        {
            logger.debug("Membership granted to " + sanitisePeerId(participant));
        }

        /**
         * Called when an administrator revokes moderator privileges from a
         * user. This means that the user will no longer be able to kick users,
         * grant and revoke voice, invite other users, modify room's subject
         * plus all the partcipants privileges.
         *
         * @param participant the participant that was revoked moderator
         * privileges in the room (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void moderatorRevoked(EntityFullJid participant)
        {
            logger.debug("Moderator Revoked from " + sanitisePeerId(participant));
        }

        /**
         * Called when a moderator grants voice to a visitor. This means that
         * the visitor can now participate in the moderated room sending
         * messages to all occupants.
         *
         * @param participant the participant that was granted voice in the room
         * (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void voiceGranted(EntityFullJid participant)
        {
            logger.debug("Voice granted to " + sanitisePeerId(participant));
        }

        /**
         * Called when an administrator revokes a user membership to the room.
         * This means that the user will not be able to join the members-only
         * room.
         *
         * @param participant the participant that was revoked membership from
         * the room
         * (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void membershipRevoked(EntityFullJid participant)
        {
            logger.debug("Membership revoked from " + sanitisePeerId(participant));
        }

        /**
         * Called when an owner grants a user ownership on the room. This means
         * that the user will be able to change defining room features as well
         * as perform all administrative functions.
         *
         * @param participant the participant that was granted ownership on the
         * room (e.g. room@conference.jabber.org/nick).
         */
        @Override
        public void ownershipGranted(EntityFullJid participant)
        {
            logger.info("Ownership granted to " + sanitisePeerId(participant));

            // All participants in the chat room are set to be owners.  This is
            // because their ownership persists over going offline so we can
            // tell who might return to the chat room.  Therefore, when
            // someone is invited to join the chat room the inviter also makes
            // them an owner.  This means we need to treat this participant as
            // having joined the chat room.
            participantJoined(participant);
        }
    }

    /**
     * Bans a user from the room. An admin or owner of the room can ban users
     * from a room.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to be banned.
     * @param reason the reason why the user was banned.
     * @throws OperationFailedException if an error occurs while banning a user.
     * In particular, an error can occur if a moderator or a user with an
     * affiliation of "owner" or "admin" was tried to be banned or if the user
     * that is banning have not enough permissions to ban.
     */
    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException
    {
        if (!(chatRoomMember instanceof ChatRoomMemberJabberImpl))
        {
            throw new IllegalArgumentException(
                "The specified member is not an jabber chat room member. " +
                    chatRoomMember);
        }
        // Send an analytic of us banning someone from a group chat, with the
        // number of active group chats as a parameter.
        List<AnalyticsParameter> params = new ArrayList<>();

        params.add(new AnalyticsParameterSimple(
            AnalyticsParameter.NAME_COUNT_GROUP_IMS,
            String.valueOf(opSetMuc.getActiveChatRooms())));

        JabberActivator.getAnalyticsService().onEvent(
            AnalyticsEventType.IM_REMOVE_PARTICIPANT, params);

        // First revoke ownership before we ban as this will kick the
        // "ownership revoked" listener on down-level clients to fully
        // remove the user from the chat, as they don't do this for banned
        // users.  Also, make sure we send the special message telling the
        // user they have been banned before we actually ban them to make
        // sure they receive it.  We do this after revoking ownership to
        // minimise the chances of us sending the message and the ban then
        // failing (banning uses the same server connection and requires
        // the same permissions as revoking ownership, so if the first
        // works, the second should).
        Jid contactAddress =
            ((ChatRoomMemberJabberImpl)chatRoomMember).getContactAddress();
        revokeOwnership(contactAddress);
        sendMembershipMessage(contactAddress, GroupMembershipAction.banned);

        try
        {
            multiUserChat.banUser(contactAddress, reason);
            sendRemoveParticipantAnalytic(InsightsResultCode.SUCCESS);
        }
        catch (XMPPErrorException | NotConnectedException | InterruptedException |
            NoResponseException ex)
        {
            logger.error("Failed to ban participant.", ex);

            // Banning failed so re-invite the user to the chat to put us back
            // into a consistent state.
            invite(contactAddress.toString(), null, true);

            sendRemoveParticipantAnalytic(InsightsResultCode.XMPP_ERROR);
            if (ex instanceof XMPPErrorException)
            {
                StanzaError stanzaError = ((XMPPErrorException) ex).getStanzaError();

                // If a moderator or a user with an affiliation of "owner" or
                // "admin" was intended to be kicked.
                if ((stanzaError != null) &&
                    (stanzaError.getCondition() == StanzaError.Condition.not_allowed))
                {
                    throw new OperationFailedException(
                        "Kicking an admin user or a chat room owner is a "
                        + "forbidden operation.",
                        OperationFailedException.FORBIDDEN);
                }
            }

            throw new OperationFailedException(
                "An error occured while trying to kick the participant.",
                OperationFailedException.GENERAL_ERROR);
        }
    }

    /**
     * Sends an analytic event for removing participant from group chat
     *
     * @param code Mapped value for parameter result
     */
    private void sendRemoveParticipantAnalytic(InsightsResultCode code)
    {
        JabberActivator.getInsightsService().logEvent(
                InsightsEventHint.IM_REMOVE_PARTICIPANT.name(),
                                 Map.of(CommonParameterInfo.INSIGHTS_RESULT_CODE.name(),
                                        code));
    }

    /**
     * Creates the corresponding ChatRoomMemberPresenceChangeEvent and notifies
     * all <tt>ChatRoomMemberPresenceListener</tt>s that a ChatRoomMember has
     * joined or left this <tt>ChatRoom</tt>.
     *
     * @param member the <tt>ChatRoomMember</tt> that this
     * @param eventID the identifier of the event
     * @param eventReason the reason of the event
     */
    private void fireMemberPresenceEvent(ChatRoomMember member,
        String eventID, String eventReason)
    {
        ChatRoomMemberPresenceChangeEvent evt
            = new ChatRoomMemberPresenceChangeEvent(
                this, member, eventID, eventReason);

        logger.trace("Will dispatch the following ChatRoom event: " + evt);

        Iterator<ChatRoomMemberPresenceListener> listeners = null;
        synchronized (memberListeners)
        {
            listeners = new ArrayList<>(
                    memberListeners).iterator();
        }

        while (listeners.hasNext())
        {
            ChatRoomMemberPresenceListener listener = listeners.next();
            listener.memberPresenceChanged(evt);
        }
    }

    /**
     * Creates the corresponding ChatRoomMemberPresenceChangeEvent and notifies
     * all <tt>ChatRoomMemberPresenceListener</tt>s that a ChatRoomMember has
     * joined or left this <tt>ChatRoom</tt>.
     *
     * @param member the <tt>ChatRoomMember</tt> that changed its presence
     * status
     * @param actor the <tt>ChatRoomMember</tt> that participated as an actor
     * in this event
     * @param eventID the identifier of the event
     * @param eventReason the reason of this event
     */
    private void fireMemberPresenceEvent(ChatRoomMember member,
        ChatRoomMember actor, String eventID, String eventReason)
    {
        ChatRoomMemberPresenceChangeEvent evt
            = new ChatRoomMemberPresenceChangeEvent(
                this, member, actor, eventID, eventReason);

        logger.trace("Will dispatch the following ChatRoom event: " + evt);

        Iterable<ChatRoomMemberPresenceListener> listeners;
        synchronized (memberListeners)
        {
            listeners
                = new ArrayList<>(
                    memberListeners);
        }

        for (ChatRoomMemberPresenceListener listener : listeners)
            listener.memberPresenceChanged(evt);
    }

    /**
     * Delivers the specified event to all registered message listeners.
     * @param evt the <tt>EventObject</tt> that we'd like delivered to all
     * registered message listeners.
     */
    private void fireMessageEvent(EventObject evt)
    {
        ArrayList<ChatRoomMessageListener> listeners;
        synchronized (messageListeners)
        {
            listeners
                = new ArrayList<>(messageListeners);
        }

        logger.debug("About to fire MessageEvent to " + listeners.size() +
                                                          " listeners: " + evt);
        for (ChatRoomMessageListener listener : listeners)
        {
            try
            {
                if (evt instanceof ChatRoomMessageDeliveredEvent)
                {
                    listener.messageDelivered(
                        (ChatRoomMessageDeliveredEvent)evt);
                }
                else if (evt instanceof ChatRoomMessageReceivedEvent)
                {
                    listener.messageReceived(
                        (ChatRoomMessageReceivedEvent)evt);
                }
                else if (evt instanceof ChatRoomMessageDeliveryFailedEvent)
                {
                    listener.messageDeliveryFailed(
                        (ChatRoomMessageDeliveryFailedEvent)evt);
                }
            } catch (Throwable ex)
            {
                logger.error("Error delivering multi chat message for " +
                    listener, ex);
            }
        }
    }

    /**
     * A listener that listens for packets of type Message and fires an event
     * to notifier interesting parties that a message was received.
     */
    private class SmackMessageListener
        implements MessageListener
    {
        /**
         * If true, the chat room that this listener belongs to has been
         * initialized.  Before the chat room is initialized, we need to store
         * any packets that are received to be processed once the chat room is
         * initialized.
         */
        private boolean chatRoomInitialized = false;

        /**
         * A list to store packets that are received before the chat room has
         * been initialized that will be processed once the chat room is
         * initialized.
         */
        private final List<Message> pendingMessages = new ArrayList<>();

        /**
         * Process a packet.  The packet will be processed immediately if the
         * chat room is already initialized, otherwise it will be queued and
         * processed once the chat room is initialized.
         *
         * @param message to process.
         */
        public void processMessage(Message message)
        {
            boolean processImmediately;
            synchronized (pendingMessages)
            {
                if (!chatRoomInitialized)
                {
                    logger.debug("Chat room not initialized, adding packet " +
                                 "to pending packets: " + message.getStanzaId() +
                                 ", for " + sanitiseChatRoom(getIdentifier()));
                    pendingMessages.add(message);
                    processImmediately = false;
                }
                else
                {
                    logger.debug("Chat room initialized, processing packet " +
                                 "immediately: " + message.getStanzaId() +
                                 ", for " + sanitiseChatRoom(getIdentifier()));
                    processImmediately = true;
                }
            }

            if (processImmediately)
            {
                if ((message.getStanzaId() != null) &&
                    (!messageHistoryService.findMatchingGroupChatMsgUids(getIdentifier().toString(),
                        Collections.singletonList(message.getStanzaId())).isEmpty()))
                {
                    logger.info("Ignore duplicate message with ID " + message.getStanzaId());
                    return;
                }

                processMessageImmediately(message);
            }
        }

        /**
         * Processes all packets that were received and queued before the chat
         * room was initialized.
         */
        private void setChatRoomInitialized()
        {
            synchronized (pendingMessages)
            {
                logger.debug("Processing pending packets for " + sanitiseChatRoom(getIdentifier()));

                // We've been asked to process the pending packets, so the chat
                // room is now initialized.
                chatRoomInitialized = true;

                // Do a bulk check for duplicate messages.  As this involves a DB operation, doing this
                // in bulk saves significant time compared to doing it individually when there are many.
                List<String> msgUids = pendingMessages.stream()
                    .map(Message::getStanzaId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                Collection<String> msgUidDuplicates =
                    messageHistoryService.findMatchingGroupChatMsgUids(getIdentifier().toString(), msgUids);

                for (Message pendingMessage : pendingMessages)
                {
                    if (msgUidDuplicates.contains(pendingMessage.getStanzaId()))
                    {
                        logger.debug("Skip duplicate message with ID " + pendingMessage.getStanzaId());
                        continue;
                    }

                    processMessageImmediately(pendingMessage);
                }

                pendingMessages.clear();
            }
        }

        /**
         * Process a Message immediately.
         *
         * @param msg to process.
         */
        private void processMessageImmediately(Message msg)
        {
            if (!ConfigurationUtils.isMultiUserChatEnabled())
            {
                logger.warn("Ignoring multi-user chat message as MUC disabled");
                return;
            }

            String msgBody = msg.getBody();
            if(msgBody == null)
                return;

            Jid msgFrom = msg.getFrom();
            String msgFromString = msgFrom.toString();
            if(msgFromString.equalsIgnoreCase(getIdentifier().toString()))
            {
                // When the message comes from the room itself it's a system
                // message.
                // We ignore system messages, as we receive equivalent normal
                // messages that we display instead.  Logging anyway, just in
                // case the server ever sends us a system message that we care
                // about.
                logger.info(
                    "Ignoring system message: " + msgBody + " from " + msgFromString);
                return;
            }

            Date delay = DelayInformationManager.getDelayTimestamp(msg);

            // Initially assume this is a group message.
            int messageType = MessageEvent.GROUP_MESSAGE;

            // Check whether the message body matches one of the 'special'
            // messages that require separate processing before the client can
            // create and fire a MessageEvent for them.  One use of these
            // messages is to inform all members of the chat room when other
            // members are invited or leave permanently.
            Matcher match = JabberActivator.SPECIAL_MESSAGE_REGEX.matcher(msgBody);

            // Store the actual message sender in case we replace msgFrom,
            // which may happen if this is a special message.
            Jid msgSender = msgFrom;

            String chatRoomId = getIdentifier().toString();
            if (match != null && match.matches())
            {
                logger.debug("Matched special message: " + sanitiseChatAddress(msgBody) + " for " + sanitiseChatRoom(chatRoomId));
                // Get the message type
                String type = match.group(1);

                // Get the message content.
                String content = match.group(2);

                List<SpecialMessageHandler> handlers =
                    opSetSpecialMsg.getSpecialMessageHandlers(type);

                // If there is a handler for this special message type, pass it
                // to the handler, unless it is a historical message, as we
                // don't want to handle a special message more than once.  In
                // any case, return so that we don't also process the message
                // as a normal message.
                if (handlers != null)
                {
                    logger.debug("Passing special message of type " + type +
                        " to handlers " + handlers + " with delay: " + delay);
                    handlers.forEach((handler) -> handler.handleSpecialMessage(content, delay, ChatRoomJabberImpl.this));
                    return;
                }

                // The only type of message we support is group membership id messages.
                if (!JabberActivator.GROUP_MEMBERSHIP_ID.equals(type))
                {
                    logger.debug("Ignoring special message: " + msgBody);
                    return;
                }

                Matcher groupMembershipMatcher = JabberActivator.GROUP_MEMBERSHIP_REGEX.matcher(content);

                // If this message matches, we know it is a status message, so
                // we need to update msgFrom and msgBody based on the message
                // content.
                if (groupMembershipMatcher.matches())
                {
                    messageType = MessageEvent.STATUS_MESSAGE;
                    // Chat room participant addresses are always chatroomid/jid
                    msgFromString = chatRoomId + "/" + groupMembershipMatcher.group(1);
                    msgFrom = JidCreate.entityFullFromOrThrowUnchecked(msgFromString);

                    msgBody = groupMembershipMatcher.group(2);

                    // We can just ignore 'created' messages that come in while
                    // we're online - they are only used to inform our offline
                    // clients of when we created a group chat so are only
                    // interesting if we receive them from the archive.
                    if (GroupMembershipAction.created.toString().equals(msgBody))
                    {
                        logger.debug("Ignoring non-archive 'created' message");
                        return;
                    }
                }
                else
                {
                    logger.error("Group membership message failed to match expected format: " + content);
                    return;
                }
            }

            // Sometimes when connecting to rooms they send history when the
            // member is no longer available we create a fake one so the
            // messages to be displayed.
            ChatRoomMemberJabberImpl member =
                getMemberForSmackParticipant(
                    msgFrom.asEntityFullJidIfPossible(),
                    true);

            if (member == null)
            {
                logger.debug("No member found for " + msgFromString +
                                 sanitiseChatRoom(chatRoomId) + ", ignoring message: " + msgBody);
                return;
            }

            // Check whether this message is from our own account
            String fromUserName = msgFrom.getResourceOrEmpty().toString();
            String contactAddress = member.getContactAddressAsString();
            String localJidString = localJid.toString();
            boolean isSentMessage =
                (localJidString.equalsIgnoreCase(contactAddress) ||
                 localJidString.equalsIgnoreCase(fromUserName) ||
                 nickname.equalsIgnoreCase(contactAddress) ||
                 nickname.equalsIgnoreCase(fromUserName));

            // Check whether this is a historical message
            boolean isHistoryMessage = (delay != null);

            // Track whether we need to ban ourselves from this chat room after
            // processing the message.  This is the case for a non-historical
            // "banned" special message referring to our account.
            boolean sendBan = false;
            String senderAddress = null;

            // If this is a status message, get the correct internationalised
            // resource to display as the message.
            if (messageType == MessageEvent.STATUS_MESSAGE)
            {
                // Get the sender address and name, as we made need them to
                // create the string to display as the message.
                senderAddress =
                    JitsiStringUtils.parseBareAddress(
                        msgSender.getResourceOrEmpty().toString());
                String msgResource = null;

                if (GroupMembershipAction.joined.toString().equals(msgBody))
                {
                    msgResource = GroupMembershipAction.joined.toString();

                    // Someone has joined, so add the participant to the list
                    // of participants in the UI.
                    memberListener.participantJoined(
                        msgFrom.asEntityFullJidIfPossible());
                }
                else if (GroupMembershipAction.left.toString().equals(msgBody))
                {
                    msgResource = GroupMembershipAction.left.toString();

                    // NOTE:
                    //  There is a certain race condition due to the participants leaving sequence and WISPA.
                    //  Removing the participant here makes sure that the list updates before WISPA gets to it.
                    //  This is merely a patch. The race condition still exists, and it cannot be fixed
                    //  with existing synchronous mechanisms without significant performance impact in
                    //  the current design.
                    memberListener.participantPermanentlyLeft(msgFrom.asEntityFullJidIfPossible());
                }
                else if (GroupMembershipAction.banned.toString().equals(msgBody))
                {
                    if (isSentMessage)
                    {
                        // We have been banned from the chat room.  If this
                        // message is historical, just use this to display a
                        // status message and don't actually leave.  As, if we
                        // are able to receive an offline banned messages, this
                        // means we were able to join the chat room so are no
                        // longer banned.  This should be because someone
                        // invited us back into the chat while we were offline
                        // so we expect to receive a historical join message
                        // after this one. If it isn't a historical message,
                        // don't actually ban ourselves until we've fired the
                        // message event to be sure that the status message is
                        // displayed in the chat before we've left.
                        logger.debug("User has been banned from " +
                                     sanitiseChatRoom(getIdentifier()) +
                                     ". Historical special message? " + isHistoryMessage);
                        if (!isHistoryMessage)
                        {
                            sendBan = true;
                        }

                        msgResource = GroupMembershipAction.banned.toString();
                    }
                    else
                    {
                        // Another user has been banned from the chat room.
                        msgResource = GroupMembershipAction.banned.toString();

                        // Remove the member representing the user who has
                        // been banned so they won't be displayed in the
                        // participants list in the UI.  This is necessary here
                        // but not when a user chooses to leave, as if they are
                        // banned while they are offline the "ownership
                        // revoked" listener won't get notified.  However, a
                        // user must be online to choose to leave, so we can
                        // rely on the listener being notified.
                        memberListener.participantPermanentlyLeft(msgFrom.asEntityFullJidIfPossible());
                    }
                }

                if (msgResource != null)
                {
                    msgBody = msgResource + "," + senderAddress;
                }
            }

            logger.debug("Received message from " + sanitiseChatAddress(fromUserName)
                         + " in " + sanitiseChatRoom(chatRoomId));

            String msgID = msg.getStanzaId();
            ImMessage newMessage = createMessage(msgBody, msgID);

            if (msg.getType() == Message.Type.error)
            {
                processErrorMessage(msg, chatRoomId, member, fromUserName, newMessage);
                return;
            }

            // If this is a historical message, set the timestamp of the
            // message to the delay in the message, otherwise use the current
            // time.
            Date timestamp = isHistoryMessage ? delay : new Date();

            ChatRoomMessageEvent msgEvt;

            // If our user ID sent this message, fire a message delivered
            // event so that it appears in the UI as being sent by us.
            if (isSentMessage)
            {
                msgEvt
                = new ChatRoomMessageDeliveredEvent(
                    newMessage,
                    ChatRoomJabberImpl.this,
                    ChatRoomJabberImpl.this.getIdentifier().toString(),
                    ChatRoomJabberImpl.this.getSubject(),
                    timestamp,
                    false,
                    messageType);
            }
            else
            {
                msgEvt
                    = new ChatRoomMessageReceivedEvent(
                        newMessage,
                        ChatRoomJabberImpl.this,
                        ChatRoomJabberImpl.this.getIdentifier().toString(),
                        ChatRoomJabberImpl.this.getSubject(),
                        member,
                        timestamp,
                        messageType == MessageEvent.STATUS_MESSAGE,
                        false,
                        messageType);
            }

            msgEvt.setHistoryMessage(isHistoryMessage);

            // A message has been received so set the chat room as active
            isActive = true;

            fireMessageEvent(msgEvt);

            // If the message indicated that we should kick the user listener
            // that bans us from the chat, do so now, as we've finished
            // processing the message.
            if (sendBan)
            {
                userListener.banned(senderAddress, null);
            }
        }

        private void processErrorMessage(Message msg, String chatRoomId,
            ChatRoomMemberJabberImpl member, String fromUserName,
            ImMessage newMessage)
        {
            logger.info("Message error received from " +
                        sanitisePeerId(fromUserName) + " in " + sanitiseChatRoom(chatRoomId));

            StanzaError error = msg.getError();
            Condition errorCode = error.getCondition();
            int errorResultCode = MessageFailedEvent.UNKNOWN_ERROR;
            String errorReason = error.getConditionText();

            if (errorCode == Condition.service_unavailable)
            {
                org.jivesoftware.smackx.xevent.packet.MessageEvent msgEvent =
                    (org.jivesoftware.smackx.xevent.packet.MessageEvent)
                        msg.getExtensionElement("x", "jabber:x:event");

                if (msgEvent != null && msgEvent.isOffline())
                {
                    errorResultCode =
                        MessageFailedEvent.OFFLINE_MESSAGES_NOT_SUPPORTED;
                }
            }

            ChatRoomMessageDeliveryFailedEvent evt =
                new ChatRoomMessageDeliveryFailedEvent(
                    newMessage,
                    ChatRoomJabberImpl.this,
                    member,
                    errorResultCode,
                    errorReason,
                    new Date());

            fireMessageEvent(evt);
        }
    }

    /**
     * A listener that is fired anytime a MUC room changes its subject.
     */
    private class SmackSubjectUpdatedListener implements SubjectUpdatedListener
    {
        /**
         * Notification that subject has changed
         * @param subject the new subject
         * @param from
         */
        public void subjectUpdated(String subject, EntityFullJid from)
        {
            String fromAddress = from != null ? from.toString() : null;
            logger.info("Subject updated by " + sanitisePeerId(fromAddress) +
                        " in room " + sanitiseChatRoom(getIdentifier()));

            if (subject == null || subject.trim().isEmpty())
            {
                logger.warn("Ignoring empty subject update");
                return;
            }

            ChatRoomMemberJabberImpl member = getMemberForSmackParticipant(from, true);

            // If we didn't find a chat room member, this is just a message
            // from the chat room itself telling us what the subject is, so we
            // can just use the 'from' string as the contact address.
            String contactAddress;
            if (member != null)
            {
                contactAddress = member.getContactAddressAsString();
            }
            else if (fromAddress != null)
            {
                contactAddress = fromAddress;
            }
            else
            {
                contactAddress = getIdentifier().toString();
            }

            // Convert the address to lower case to avoid case mis-matches
            // between our local contact list and the server roster.
            if (contactAddress != null)
            {
                contactAddress = contactAddress.toLowerCase();
            }

            synchronized (oldSubjectLock)
            {
                // If we've got a new or changed subject, save it in config.
                if (oldSubject == null || !oldSubject.equals(subject))
                {
                    ConfigurationUtils.updateChatRoomProperty(
                        provider, getIdentifier().toString(), "chatRoomSubject", subject);
                }

                ChatRoomPropertyChangeEvent evt
                    = new ChatRoomPropertyChangeEvent(
                        ChatRoomJabberImpl.this,
                        contactAddress,
                        ChatRoomPropertyChangeEvent.CHAT_ROOM_SUBJECT,
                        oldSubject,
                        subject);

                firePropertyChangeEvent(evt);

                // Keeps track of the subject.
                oldSubject = subject;
            }
        }
    }

    /**
     * A listener that is fired anytime your participant's status in a room
     * is changed, such as the user being kicked, banned, or granted admin
     * permissions.
     */
    private class UserListener implements UserStatusListener
    {

        /**
         * Called when a moderator grants voice to your user. This means that
         * you were a visitor in the moderated room before and now you can
         * participate in the room by sending messages to all occupants.
         */
        public void voiceGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when a moderator revokes voice from your user. This means that
        * you were a participant in the room able to speak and now you are a
        * visitor that can't send messages to the room occupants.
        */
        public void voiceRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.SILENT_MEMBER);
        }

        /**
        * Called when an administrator or owner banned your user from the room.
        * This means that you will no longer be able to join the room unless the
        * ban has been removed.
        *
        * @param actor the administrator that banned your user
        * (e.g. user@host.org).
        * @param reason the reason provided by the administrator to banned you.
        */
        public void banned(String actor, String reason)
        {
            logger.info("Banned from " + sanitiseChatRoom(getIdentifier()) +
                        " by " + logHasher(actor) + ". Reason: " + reason);
            leave(LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED, reason);
        }

        /**
        * Called when an administrator grants your user membership to the room.
        * This means that you will be able to join the members-only room.
        */
        public void membershipGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when an administrator revokes your user membership to the room.
        * This means that you will not be able to join the members-only room.
        */
        public void membershipRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.GUEST);
        }

        /**
        * Called when an administrator grants moderator privileges to your user.
        * This means that you will be able to kick users, grant and revoke
        * voice, invite other users, modify room's subject plus all the
        * participants privileges.
        */
        public void moderatorGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.MODERATOR);
        }

        /**
        * Called when an administrator revokes moderator privileges from your
        * user. This means that you will no longer be able to kick users, grant
        * and revoke voice, invite other users, modify room's subject plus all
        * the participants privileges.
        */
        public void moderatorRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when an owner grants to your user ownership on the room. This
        * means that you will be able to change defining room features as well
        * as perform all administrative functions.
        */
        public void ownershipGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.OWNER);
        }

        /**
        * Called when an owner revokes from your user ownership on the room.
        * This means that you will no longer be able to change defining room
        * features as well as perform all administrative functions.
        */
        public void ownershipRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.ADMINISTRATOR);
        }

        /**
        * Called when an owner grants administrator privileges to your user.
        * This means that you will be able to perform administrative functions
        * such as banning users and edit moderator list.
        */
        public void adminGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.ADMINISTRATOR);
        }

        /**
        * Called when an owner revokes administrator privileges from your user.
        * This means that you will no longer be able to perform administrative
        * functions such as banning users and edit moderator list.
        */
        public void adminRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }
    }

    /**
     * Delivers the specified event to all registered property change listeners.
     *
     * @param evt the <tt>PropertyChangeEvent</tt> that we'd like delivered to
     * all registered property change listeners.
     */
    private void firePropertyChangeEvent(PropertyChangeEvent evt)
    {
        Iterable<ChatRoomPropertyChangeListener> listeners;
        synchronized (propertyChangeListeners)
        {
            listeners = new ArrayList<>(propertyChangeListeners);
        }

        for (ChatRoomPropertyChangeListener listener : listeners)
        {
            if (evt instanceof ChatRoomPropertyChangeEvent)
            {
                listener.chatRoomPropertyChanged(
                    (ChatRoomPropertyChangeEvent) evt);
            }
            else if (evt instanceof ChatRoomPropertyChangeFailedEvent)
            {
                listener.chatRoomPropertyChangeFailed(
                    (ChatRoomPropertyChangeFailedEvent) evt);
            }
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (provider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                +"service before being able to communicate.");
        if (!provider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to communicate.");
    }

    /**
     * Returns the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room. If the user doesn't have
     * permissions to see and change chat room configuration an
     * <tt>OperationFailedException</tt> is thrown.
     *
     * @return the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room
     * @throws OperationFailedException if the user doesn't have
     * permissions to see and change chat room configuration
     */
    public ChatRoomConfigurationForm getConfigurationForm()
        throws OperationFailedException
    {
        Form smackConfigForm = null;

        try
        {
            smackConfigForm = multiUserChat.getConfigurationForm();

            this.configForm
                = new ChatRoomConfigurationFormJabberImpl(
                    multiUserChat, smackConfigForm);
        }
        catch (NoResponseException | NotConnectedException |
            InterruptedException | XMPPErrorException ex)
        {
            if (ex instanceof XMPPErrorException)
            {
                StanzaError stanzaError =
                    ((XMPPErrorException)ex).getStanzaError();

                if (stanzaError != null &&
                   stanzaError.getCondition() == Condition.forbidden)
                {
                    throw new OperationFailedException(
                        "Failed to obtain smack multi user chat config form."
                        + "User doesn't have enough privileges to see the form.",
                        OperationFailedException.NOT_ENOUGH_PRIVILEGES,
                       ex);
                }
                else
                {
                    throw new OperationFailedException(
                        "Failed to obtain smack multi user chat config form.",
                        OperationFailedException.GENERAL_ERROR,
                       ex);
                }
            }
        }

        return configForm;
    }

    /**
     * Determines whether this chat room should be stored in the configuration
     * file or not. If the chat room is persistent it still will be shown after a
     * restart in the chat room list. A non-persistent chat room will be only in
     * the chat room list until the program is running.
     *
     * @return true if this chat room is persistent, false otherwise
     */
    public boolean isPersistent()
    {
        boolean persistent = false;
        EntityBareJid roomName = multiUserChat.getRoom();
        try
        {
            // Do not use getRoomInfo, as it has bug and
            // throws NPE
            DiscoverInfo info =
                ServiceDiscoveryManager.getInstanceFor(provider.getConnection()).
                    discoverInfo(roomName);

            if (info != null)
                persistent = info.containsFeature("muc_persistent");
        }
        catch (Exception ex)
        {
            logger.warn("Could not get persistent state for room :" +
                sanitiseChatRoom(roomName) + "\n", ex);
        }
        return persistent;
    }

    /**
     * Finds the member of this chat room corresponding to the given nick name.
     *
     * @param jabberID the nick name to search for.
     * @return the member of this chat room corresponding to the given nick name.
     */
    public ChatRoomMemberJabberImpl findMemberForNickName(String jabberID)
    {
        synchronized (members)
        {
            // Convert the jabber ID to lower case to avoid case mis-matches
            // between our local contact list and the server roster.
            return members.get(jabberID.toLowerCase());
        }
    }

    /**
    * Revokes ownership privileges from another user. The occupant that loses
    * ownership privileges will become an administrator. Room owners may revoke
    * ownership privileges. Some room implementations will not allow to grant
    * ownership privileges to other users.
    *
    * @param jid the bare XMPP user ID of the user to revoke ownership
    * (e.g. "user@host.org").
    *
    * @throws OperationFailedException
    */
    public void revokeOwnership(Jid jid) throws OperationFailedException
    {
        try
        {
            multiUserChat.revokeOwnership(jid);
        }
        catch (NoResponseException | NotConnectedException | InterruptedException |
            XMPPErrorException ex)
        {
            String errorString =
                "An error occurred revoking ownership privileges from the user: " + sanitisePeerId(jid);
            logger.error(errorString, ex);

            throw new OperationFailedException(
                errorString,
                OperationFailedException.GENERAL_ERROR);
        }
    }

    /**
     * Returns the nickname of the given participant name. For example, for
     * the address "john@xmppservice.com", "john" would be returned. If no @
     * is found in the address we return the given name.
     * @param participantAddress the address of the participant
     * @return the nickname part of the given participant address
     */
    static String getNickName(String participantAddress)
    {
        if (participantAddress == null)
            return null;

        int atIndex = participantAddress.lastIndexOf("@");
        if (atIndex <= 0)
            return participantAddress;
        else
            return participantAddress.substring(0, atIndex);
    }

    /**
     * Returns the internal stack used chat room instance.
     * @return the chat room used in the protocol stack.
     */
    MultiUserChat getMultiUserChat()
    {
        return multiUserChat;
    }

    /**
     * Looks for a MetaContact with the given address.  If one is found, the
     * display name of the MetaContact is returned.  Otherwise, the user
     * address is returned.
     *
     * @param userAddress the address to find a display name for.
     *
     * @return the display name, or the user address as passed in if no display
     * name was found.
     */
    public String getDisplayNameFromUserAddress(String userAddress)
    {
        String displayName = userAddress;

        String accountId = (provider != null) ?
            provider.getAccountID().getAccountUniqueID() : "unknown";

        MetaContact metaContact =
            contactListService.findMetaContactByContact(userAddress, accountId);

        if (metaContact != null)
        {
            displayName = metaContact.getDisplayName();
        }

        return displayName;
    }

    /**
     * Listens for rejection message and delivers system message when received.
     */
    private class InvitationRejectionListeners
        implements StanzaListener
    {
        /**
         * Process incoming packet, checking for muc extension.
         * @param stanza the incoming packet.
         */
        public void processStanza(Stanza stanza)
        {
            try
            {
                MUCUser mucUser = getMUCUserExtension(stanza);

                // Check if the MUCUser informs that the invitee
                // has declined the invitation
                if (mucUser != null
                    && mucUser.getDecline() != null
                    && ((Message) stanza).getType() != Message.Type.error)
                {
                    int messageReceivedEventType = MessageEvent.SYSTEM_MESSAGE;
                    ChatRoomMemberJabberImpl member = new ChatRoomMemberJabberImpl(
                        ChatRoomJabberImpl.this,
                        Resourcepart.from(getIdentifier().toString()),
                        getIdentifier());

                    String from = mucUser.getDecline().getFrom().toString();

                    if(opSetPersPres != null)
                    {
                        Contact c = opSetPersPres.findContactByID(
                            JitsiStringUtils.parseBareAddress(from));
                        if(c != null)
                        {
                            if(!from.contains(c.getDisplayName()))
                                from = c.getDisplayName() + " (" + from + ")";
                        }
                    }

                    String msgBody =
                        resources.getI18NString(
                            "service.gui.INVITATION_REJECTED",
                            new String[]{from, mucUser.getDecline().getReason()});

                    ChatRoomMessageReceivedEvent msgReceivedEvt
                        = new ChatRoomMessageReceivedEvent(
                            createMessage(msgBody, stanza.getStanzaId()),
                            ChatRoomJabberImpl.this,
                            ChatRoomJabberImpl.this.getIdentifier().toString(),
                            ChatRoomJabberImpl.this.getSubject(),
                            member,
                            new Date(),
                            false,
                            false,
                            messageReceivedEventType);

                    fireMessageEvent(msgReceivedEvt);
                }
            } catch (XmppStringprepException ex) {
                logger.error("Chat room name is not a valid JID! : "
                    + localJid, ex);
            }
        }
    }

    @Override
    public boolean isActive()
    {
        return isActive ;
    }

    @Override
    public boolean isMuted()
    {
        return isMuted;
    }

    @Override
    public void toggleMute()
    {
        // Toggle local mute state
        isMuted = !isMuted;
        logger.debug("Set mute of chatroom: " +
                     sanitiseChatRoom(getIdentifier()) +
                     " to " + isMuted);

        // Update the config to the new value
        ConfigurationUtils.updateChatRoomProperty(
            provider,
            getIdentifier().toString(),
            MUTE_PROPERTY_NAME,
            (isMuted ? "true" : null));
    }

    @Override
    public Set<MetaContact> getMetaContactMembers()
    {
        Set<MetaContact> metaContacts = new HashSet<>();
        List<ChatRoomMember> membersList = getMembers();

        for (ChatRoomMember member : membersList)
        {
            Contact contact = member.getContact();

            if (contact != null)
            {
                MetaContact metaContact = contactListService.findMetaContactByContact(contact);

                if (metaContact != null)
                {
                    metaContacts.add(metaContact);
                }
            }
        }

        return metaContacts;
    }

    @Override
    public Set<String> getNonMetaContactMemberJids()
    {
        Set<String> nonContactJids = new HashSet<>();
        List<ChatRoomMember> membersList = getMembers();

        for (ChatRoomMember member : membersList)
        {
            Contact contact = member.getContact();

            if (contact == null ||
                contactListService.findMetaContactByContact(contact) == null &&
                member instanceof ChatRoomMemberJabberImpl )
            {
                // We have found no contact or no MetaContact for this chat
                // room member.  Add its address to the list of jids to return,
                // as long as it isn't our own address.
                Jid contactAddress = ((ChatRoomMemberJabberImpl)member).getContactAddress();
                if (!contactAddress.equals(localJid))
                {
                    nonContactJids.add(contactAddress.toString());
                }
            }
        }

        logger.debug("Returning non-MetaContact members: " + nonContactJids);
        return nonContactJids;
    }

    @Override
    public String getDirectInviteResource()
    {
        // The string used in this menu item varies depending on whether this
        // chat room  can be uplifted to a conference and whether or not a
        // conference is already in progress.
        String resource;
        boolean conferenceCreated =
            JabberActivator.getConferenceService().isConferenceCreated();
        if (canBeUplifedToConference())
        {
            resource = conferenceCreated ?
                                  "service.gui.conf.INVITE_IM_IN_CONFERENCE" :
                                  "service.gui.conf.INVITE_IM_NOT_IN_CONFERENCE";
        }
        else
        {
            resource = conferenceCreated ?
                              "service.gui.conf.INVITE_NON_IM_IN_CONFERENCE" :
                              "service.gui.conf.INVITE_NON_IM_NOT_IN_CONFERENCE";
        }

        return resource;
    }

    @Override
    public String getSelectOthersInviteResource()
    {
        // The string used in this menu item varies depending on whether this
        // chat room  can be uplifted to a conference and whether or not a
        // conference is already in progress.
        String resource;
        boolean conferenceCreated = JabberActivator.getConferenceService().isConferenceCreated();
        if (canBeUplifedToConference())
        {
            resource = "service.gui.conf.INVITE_OTHERS_IM";
        }
        else
        {
            resource = conferenceCreated ?
                         "service.gui.conf.INVITE_OTHERS_NON_IM_IN_CONFERENCE" :
                         "service.gui.conf.INVITE_OTHERS_NON_IM";
        }

        return resource;
    }

    @Override
    public void createConference(boolean createImmediately)
    {
        JabberActivator.getConferenceService().createOrAdd(this, createImmediately);
    }

    @Override
    public boolean canBeUplifedToConference()
    {
        return JabberActivator.getConferenceService().canChatRoomBeUplifted(this);
    }
}
