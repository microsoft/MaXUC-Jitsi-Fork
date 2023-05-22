/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.delay.DelayInformationManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.InvitationRejectionListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException.NotAMucServiceException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.packet.MUCUser.Decline;
import org.jivesoftware.smackx.muc.packet.MUCUser.Invite;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.impl.protocol.jabber.JabberActivator.GroupMembershipAction;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.protocol.AbstractOperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomInvitation;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationNotSupportedException;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.ChatRoomCreatedEvent;
import net.java.sip.communicator.service.protocol.event.ContactPropertyChangeEvent;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.protocol.event.SubscriptionEvent;
import net.java.sip.communicator.service.protocol.event.SubscriptionListener;
import net.java.sip.communicator.service.protocol.event.SubscriptionMovedEvent;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * A jabber implementation of the multi user chat operation set.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class OperationSetMultiUserChatJabberImpl extends AbstractOperationSetMultiUserChat
    implements SubscriptionListener
{
    /**
     * This class logger.
     */
    private static final Logger sLog = Logger.getLogger(OperationSetMultiUserChatJabberImpl.class);

    /**
     * The currently valid Jabber protocol provider service implementation.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * The currently valid Jabber persistent presence operation set
     * implementation.
     */
    private final OperationSetPersistentPresenceJabberImpl presenceOpSet;

    /**
     * A list of the rooms that are currently open by this account. Note that
     * we have not necessarily joined these rooms, we might have simply been
     * searching through them.
     */
    private final Hashtable<EntityBareJid, ChatRoom> chatRoomCache = new Hashtable<>();

    /**
     * A secondary cache that will be used so we can get the information of
     * the rooms the user joined when the registration status go off.
     * */
    private final Hashtable<EntityBareJid, ChatRoom> backupChatRoomCache = new Hashtable<>();

    /**
     * The registration listener that would get notified when the underlying
     * Jabber provider gets registered.
     */
    private final RegistrationStateListener providerRegListener = new RegistrationStateListener();

    /**
     * The smack invitation listener that will be notified when we are invited
     * to join a chat room.
     */
    private final SmackInvitationListener smackInvitationListener = new SmackInvitationListener();

    /**
     * Whether the server supports multi user chat.  Default to true, as we may
     * receive multi user chat packets from the server immediately on start-up
     * and we don't want to reject them just because we haven't yet been able
     * to check whether the server supports multi user chat yet.  The packets
     * will still be ignored if group IM is disabled in the user's CoS.
     */
    private boolean isMultiChatSupported = true;

    /**
     * The config property containing the maximum permitted number of
     * concurrent active chatrooms. This should be set to the same value as
     * the server config for 'max_user_conferences'.  This is defaulted to
     * 1000.
     */
    public static final String MAX_ACTIVE_CHATROOMS_PROP =
        "net.java.sip.communicator.impl.gui.main.chat.MAX_ACTIVE_CHATROOMS";

    /**
     * The maximum number of active chat rooms.  This is set to 75% of the
     * maximum specified in config so that we start deleting old chat rooms
     * before we reach the real maximum to be sure we won't overload the
     * server.
     */
    public final int maxActiveChatRooms;

    /**
     * The number of currently active chat rooms
     */
    private AtomicInteger activeChatRooms = new AtomicInteger();

    /**
     * Timer used to schedule the multi-user chat task.
     */
    private final Timer mMultiChatTimer = new Timer("checkMUC");

    /**
     * How often, in ms, the multi-chat task should run (every 24 hours)
     */
    private static final int MUC_CHECK_TIMER_DELAY = 24*60*60*1000;

    /**
     * We must wait until the client has finished processing any group chats
     * found in the initial roster on start-up before handling any new
     * invitations (see SFR 541779).  This boolean tracks whether this is
     * complete.  Access to it is synchronized on the mInvitationToChatRoomList
     * list below.
     */
    private boolean mInitialRosterProcessed = false;

    /**
     * Any group chat invitations that are received before the client has
     * finished processing group chats found in the initial roster on start-up
     * are queued in this list (see SFR 541779).  Access to this list, as well
     * as to the mInitialRosterProcessed boolean above, is synchronized on this
     * list.
     */
    private final List<InvitationToChatRoom> mInvitationToChatRoomList = new ArrayList<>();

    /**
     * Instantiates the user operation set with a currently valid instance of
     * the Jabber protocol provider.
     * @param jabberProvider a currently valid instance of
     * ProtocolProviderServiceJabberImpl.
     */
    OperationSetMultiUserChatJabberImpl(
                        ProtocolProviderServiceJabberImpl jabberProvider)
    {
        this.jabberProvider = jabberProvider;
        int maxActiveChatRoomsConfig =
            JabberActivator.getConfigurationService().global().getInt(
                                               MAX_ACTIVE_CHATROOMS_PROP, 1000);

        // Only allow users to configure 75% of the maximum configured on the
        // server to be sure we don't overload the server.
        maxActiveChatRooms =  (int) Math.round((maxActiveChatRoomsConfig * 0.75));

        jabberProvider.addRegistrationStateChangeListener(providerRegListener);

        presenceOpSet =
            (OperationSetPersistentPresenceJabberImpl) jabberProvider.
                getOperationSet(OperationSetPersistentPresence.class);
        presenceOpSet.addSubscriptionListener(this);

        // Set up a timer to check whether multi user chat is supported on the
        // server. This will run whenever the Jabber protocol registers, and
        // once every day from then. This task may take a while to process,
        // hence it is done once per day rather than on demand.
        TimerTask multiChatTask = new TimerTask()
        {
            @Override
            public void run()
            {
                setMultiChatSupported();
            }
        };

        mMultiChatTimer.scheduleAtFixedRate(multiChatTask,
                                            MUC_CHECK_TIMER_DELAY,
                                            MUC_CHECK_TIMER_DELAY);
    }

    /**
     *  This is a class which stores the information for one invitation to a
     *  chat room. It is used to allow us to queue up invitations sent while we
     *  were offline until we have finished processing any group chats found in
     *  the initial roster on start-up (see SFR 541779).
     */
    private class InvitationToChatRoom
    {
        /**
         * The XMPPConnection that received the invitation.
         */
        public final XMPPConnection mConn;

        /**
         * The chat room jid that invitation refers to.
         */
        public final MultiUserChat mRoom;

        /**
         * The inviter that sent the invitation. (e.g. crone1@shakespeare.lit).
         */
        public final EntityJid mInviter;

        /**
         * The reason why the inviter sent the invitation.
         */
        public final String mReason;

        /**
         * The password to use when joining the room.
         */
        public final String mPassword;

        /**
         * The message used by the inviter to send the invitation.
         */
        public final Message mMessage;

        /**
         * @param conn the XMPPConnection that received the invitation.
         * @param room the room that invitation refers to.
         * @param inviter the inviter that sent the invitation.
         * (e.g. crone1@shakespeare.lit).
         * @param reason the reason why the inviter sent the invitation.
         * @param password the password to use when joining the room.
         * @param message the message used by the inviter to send the invitation.
         */
        public InvitationToChatRoom(
            XMPPConnection conn,
            MultiUserChat room,
            EntityJid inviter,
            String reason,
            String password,
            Message message)
        {
            mConn = conn;
            mRoom = room;
            mInviter = inviter;
            mReason = reason;
            mPassword = password;
            mMessage = message;
        }
    }

    /**
     * Accept the invitation to a chat room.
     *
     * @param conn the XMPPConnection that received the invitation.
     * @param room the chat room jid that the invitation refers to.
     * @param inviter the inviter that sent the invitation.
     * (e.g. crone1@shakespeare.lit).
     * @param reason the reason why the inviter sent the invitation.
     * @param password the password to use when joining the room.
     * @param message the message used by the inviter to send the invitation.
     */
    private void acceptInvitation(
        XMPPConnection conn,
        MultiUserChat room,
        EntityJid inviter,
        String reason,
        String password,
        Message message)
    {
        ChatRoomJabberImpl chatRoom;
        EntityBareJid roomNameAsJid = room.getRoom();
        try
        {
            chatRoom = (ChatRoomJabberImpl) findRoom(roomNameAsJid.toString());
            if (chatRoom == null)
            {
                MultiUserChat muc = MultiUserChatManager.getInstanceFor(conn)
                        .getMultiUserChat(roomNameAsJid);
                chatRoom = new ChatRoomJabberImpl(muc, jabberProvider);
                fireChatRoomCreated(new ChatRoomCreatedEvent(chatRoom));
            }

            // If no invited date has been set on the chat room, this is
            // the first invite we've received for this chat room so set
            // the date.  If there is a delay in the invite message, this
            // is a historical invite, so we need to set the invited time
            // to the delay time. Otherwise, it is a real-time invite, so
            // we just use the current time.
            boolean isHistorical;
            if (chatRoom.getInvitedDate() == null)
            {
                Date timestamp = DelayInformationManager.getDelayTimestamp(message);

                if(timestamp == null)
                {
                    timestamp = new Date();
                    sLog.debug("Setting invitedDate to current timestamp for " +
                            sanitiseMUCRoom(room));
                    isHistorical = false;
                }
                else
                {
                    sLog.debug("Setting historical invitedDate " +
                               timestamp + " for " +
                               sanitiseMUCRoom(room));
                    isHistorical = true;
                }

                chatRoom.setInvitedDate(timestamp);
            }
            else
            {
                // If an invited date has been set, we can ignore this
                // invitation as it is a duplicate.
                sLog.debug("Invited date is not null - returning as no" +
                             " need to try to join again: " +
                           sanitiseMUCRoom(room));
                return;
            }

            if (password != null)
                fireInvitationEvent(
                        chatRoom, inviter, reason, isHistorical, password.getBytes());
            else
                fireInvitationEvent(
                        chatRoom, inviter, reason, isHistorical, null);

            // Add this chat room ID to our roster on the server so that
            // any other clients logged on to this user account will learn
            // about this chat room and join it.
            presenceOpSet.getServerStoredContactList().
                    addChatRoomToRoster(roomNameAsJid.toString());
        }
        catch (OperationFailedException | OperationNotSupportedException ex)
        {
            sLog.error("Failed to find room with name: " +
                       sanitiseMUCRoom(room), ex);
        }
    }

    /**
     * When the roster has finished being processed at start-up, we add any chat
     * room invitations which have come in while we were offline.
     */
    public void onRosterProcessed()
    {
        synchronized (mInvitationToChatRoomList)
        {
            sLog.info("Processing stored invitations.");
            mInitialRosterProcessed = true;
            for (InvitationToChatRoom invite : mInvitationToChatRoomList)
            {
                acceptInvitation(invite.mConn,
                                 invite.mRoom,
                                 invite.mInviter,
                                 invite.mReason,
                                 invite.mPassword,
                                 invite.mMessage);
            }
        }
    }

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to.
     *
     * @param roomName the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     *   created.
     *
     * @throws OperationFailedException if the room couldn't be created for
     * some reason (e.g. room already exists; user already joined to an
     * existent room or user has no permissions to create a chat room).
     * @throws OperationNotSupportedException if chat room creation is not
     * supported by this server
     *
     * @return ChatRoom the chat room that we've just created.
     */
    public ChatRoom createChatRoom(
            String roomName,
            Map<String, Object> roomProperties)
        throws OperationFailedException,
               OperationNotSupportedException
    {
        //first make sure we are connected and the server supports multichat
        assertSupportedAndConnected();

        // If a room name hasn't been provided, we're creating a new chat
        // room on the server, otherwise we're trying to join a chat room that
        // has already been created on the server.
        ChatRoom room = null;
        if (roomName == null)
        {
            sLog.debug("No room name provided so create one");
            boolean gotValidRoomName = false;

            while (!gotValidRoomName)
            {
                // We create a random room name and make sure it doesn't
                // already exist on the server before continuing.
                roomName = CHATROOM_ID_PREFIX + StringUtils.randomString(8);

                if (findRoom(roomName) == null)
                {
                    sLog.debug("Room doesn't exist so name is valid: " + sanitiseChatRoom(roomName));
                    gotValidRoomName = true;
                }
            }
        }
        else
        {
            sLog.debug("Room name provided so try to find it: " + sanitiseChatRoom(roomName));
            room = findRoom(roomName);
        }

        if (room == null)
        {
            sLog.info("Find room returns null.");

            MultiUserChat muc = null;
            try
            {
                muc = MultiUserChatManager.getInstanceFor(getXmppConnection())
                    .getMultiUserChat(getCanonicalRoomName(roomName));
                Resourcepart chatNick =
                    Resourcepart.from(
                        getXmppConnection().getUser().getLocalpart().toString());
                muc.create(chatNick);

            }
            catch (Exception ex)
            {
                sLog.error("Failed to create chat room.", ex);
                    throw new OperationFailedException(
                        "Failed to create chat room",
                        OperationFailedException.GENERAL_ERROR,
                        ex);
            }

            try
            {
                // Fillable form accepts only "form" type
                // sendConfigurationForm will convert it to "submit" type
                DataForm.Builder builder = DataForm.builder(DataForm.Type.form);
                muc.sendConfigurationForm(new FillableForm(builder.build()));
            }
            catch (XMPPErrorException | NoResponseException | NotConnectedException |
                InterruptedException ex)
            {
                sLog.error("Failed to send config form.", ex);
            }

            room = createLocalChatRoomInstance(muc);

            // Make sure we set ourself as a room owner, as other clients use
            // the list of owners to find out which offline users might return
            // to a group chat.
            room.setLocalUserRole(ChatRoomMemberRole.OWNER);

            // Send a 'special' message to the room to say that we created it
            // to be sure that any other clients that we have who are currently
            // offline will know when we joined the chat room, as they will
            // receive the message in the archive.
            ((ChatRoomJabberImpl) room).sendMembershipMessage(
                jabberProvider.getOurJid(),
                GroupMembershipAction.created);

            // Add this chat room ID to our roster on the server so that
            // any other clients logged on to this user account will learn
            // about this chat room and join it.
            presenceOpSet.getServerStoredContactList().
                                      addChatRoomToRoster(room.getIdentifier().toString());

            // We've joined a new chatroom so increment our count of
            // active chatrooms and check whether we've reached the maximum.
            updateActiveChatRooms(true);
            checkForMaxActiveChatRooms();
        }
        return room;
    }

    /**
     * Creates a <tt>ChatRoom</tt> from the specified smack
     * <tt>MultiUserChat</tt>.
     *
     * @param muc the smack MultiUserChat instance that we're going to wrap our
     * chat room around.
     *
     * @return ChatRoom the chat room that we've just created.
     */
    private ChatRoom createLocalChatRoomInstance(MultiUserChat muc)
        throws OperationFailedException
    {
        synchronized(chatRoomCache)
        {
            ChatRoomJabberImpl chatRoom
                = new ChatRoomJabberImpl(muc, jabberProvider);
            fireChatRoomCreated(new ChatRoomCreatedEvent(chatRoom));
            cacheChatRoom(chatRoom);

            // Add the contained in this class SmackInvitationRejectionListener
            // which will dispatch all rejection events to the
            // ChatRoomInvitationRejectionListener.
            muc.addInvitationRejectionListener(
                new SmackInvitationRejectionListener(chatRoom));

            return chatRoom;
        }
    }

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt> or null
     * if that room does not exist.
     *
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking
     *   for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt> if it exists, null
     * otherwise.
     * @throws OperationFailedException if an error occurs while trying to
     * discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public synchronized ChatRoom findRoom(String roomName)
        throws OperationFailedException, OperationNotSupportedException
    {
        EntityBareJid canonicalRoomName = getCanonicalRoomName(roomName);
        ChatRoom room = chatRoomCache.get(canonicalRoomName);
        if (room == null)
        {
            room = backupChatRoomCache.get(canonicalRoomName);
        }

        if (room != null)
        {
            return room;
        }

        try
        {
            XMPPConnection connection = getXmppConnection();

            // When running offline or at start of day, the XMPPConnection might not exist or not be
            // connected. Trying to get the ServiceDiscoveryManager with a disconnected
            // XMPPConnection results in an exception, so bail out before that happens.
            if (connection == null || !connection.isConnected() || connection.getUser() == null)
                return null;

            // throws Exception if room does not exist
            // do not use MultiUserChat.getRoomInfo as there is a bug which
            // throws NPE
            ServiceDiscoveryManager.getInstanceFor(connection).
                discoverInfo(canonicalRoomName);

            MultiUserChat muc = MultiUserChatManager.getInstanceFor(getXmppConnection())
                .getMultiUserChat(canonicalRoomName);

            room = new ChatRoomJabberImpl(muc, jabberProvider);
            chatRoomCache.put(canonicalRoomName, room);
            fireChatRoomCreated(new ChatRoomCreatedEvent(room));
            return room;
        }
        catch (XMPPErrorException ex)
        {
            sLog.debug(
                "No existing chat room found for: " + sanitiseChatRoom(canonicalRoomName.toString()));
            return null;
        }
        catch (NoResponseException | NotConnectedException | InterruptedException ex)
        {
            sLog.debug(
                "Error finding chat room for:  " + sanitiseChatRoom(canonicalRoomName.toString()), ex);
            return null;
        }
    }

    /**
     * Returns a list of the chat rooms that we have joined and are currently
     * active in.
     *
     * @return a <tt>List</tt> of the rooms where the user has joined using
     *   a given connection.
     */
    public List<ChatRoom> getCurrentlyJoinedChatRooms()
    {
        synchronized(chatRoomCache)
        {
            List<ChatRoom> joinedRooms
                = new LinkedList<>(this.chatRoomCache.values());
            Iterator<ChatRoom> joinedRoomsIter = joinedRooms.iterator();

            while (joinedRoomsIter.hasNext())
                if (!joinedRoomsIter.next().isJoined())
                    joinedRoomsIter.remove();
            return joinedRooms;
        }
    }

    /**
     * @return the number of currently active chat rooms
     */
    public int getActiveChatRooms()
    {
        return activeChatRooms.get();
    }

    /**
     * Returns the <tt>List</tt> of <tt>String</tt>s indicating chat rooms
     * currently available on the server that this protocol provider is
     * connected to.
     *
     * @return a <tt>java.util.List</tt> of the name <tt>String</tt>s for chat
     * rooms that are currently available on the server that this protocol
     * provider is connected to.
     *
     * @throws OperationFailedException if we faile retrieving this list from
     * the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public List<String> getExistingChatRooms()
        throws OperationFailedException, OperationNotSupportedException
    {
        assertSupportedAndConnected();

        List<String> list = new LinkedList<>();

        //first retrieve all conference service names available on this server
        Iterator<DomainBareJid> serviceNames = null;
        try
        {
            serviceNames = MultiUserChatManager.getInstanceFor(getXmppConnection())
                    .getMucServiceDomains().iterator();
        }
        catch (XMPPErrorException ex )
        {
            XMPPErrorConvertor.reThrowAsOperationFailedException(
                "Failed to retrieve Jabber conference service names", ex);
        }
        catch (NoResponseException | NotConnectedException | InterruptedException ex)
        {
            throw new OperationFailedException(
                "Failed to retrieve Jabber conference service names"
                , OperationFailedException.GENERAL_ERROR
                , ex);
        }

        //now retrieve all chat rooms currently available for every service name
        while(serviceNames.hasNext())
        {
            DomainBareJid serviceName = serviceNames.next();
            List<HostedRoom> roomsOnThisService = new LinkedList<>();

            try
            {
                Map<EntityBareJid, HostedRoom> roomsHostedBy =
                        MultiUserChatManager.getInstanceFor(getXmppConnection())
                                .getRoomsHostedBy(serviceName);
                roomsOnThisService
                    .addAll(roomsHostedBy.values());
            }
            catch (XMPPErrorException | NotAMucServiceException | NoResponseException |
                NotConnectedException | InterruptedException ex)
            {
                sLog.error("Failed to retrieve rooms for serviceName="
                             + serviceName, ex);
                //continue bravely with other service names
                continue;
            }

            //now go through all rooms available on this service
            Iterator<HostedRoom> serviceRoomsIter = roomsOnThisService.iterator();

            //add the room name to the list of names we are returning
            while(serviceRoomsIter.hasNext())
                list.add(
                    serviceRoomsIter.next().getJid().toString());
        }

        /** @todo maybe we should add a check here and fail if retrieving chat
         * rooms failed for all service names*/

        return list;
    }

    /**
     * Returns true if <tt>contact</tt> supports multi user chat sessions.
     *
     * @param contact reference to the contact whose support for chat rooms
     *   we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports
     *   chatrooms.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
     *   method
     */
    public boolean isMultiChatSupportedByContact(Contact contact)
    {
        if(contact.getProtocolProvider()
            .getOperationSet(OperationSetMultiUserChat.class) != null)
            return true;

        return false;
    }

    @Override
    public boolean isMultiChatSupported()
    {
        return isMultiChatSupported;
    }

    /**
     * Determines whether the XMPP server supports multi user chat
     */
    private void setMultiChatSupported()
    {
        Collection<DomainBareJid> mucServiceNames;
        try
        {
            XMPPConnection xmppConnection = getXmppConnection();
            boolean enabled = false;
            if (xmppConnection != null)
            {
                mucServiceNames = MultiUserChatManager.getInstanceFor(xmppConnection)
                        .getMucServiceDomains();
                enabled = mucServiceNames.size() > 0;
            }
            else
            {
                sLog.warn(
                    "Unable to check if MUC is supported - XMPP connection is null");
            }

            sLog.debug("Setting MUC supported on server to " + enabled);
            isMultiChatSupported = enabled;
        }
        catch (NoResponseException | NotConnectedException | InterruptedException |
            XMPPErrorException ex)
        {
            // Don't set isMultiUserChatSupported to disabled if we hit an
            // error here, as we may just still be starting up and we may
            // receive multi user chat packets from the server immediately on
            // start-up and we don't want to reject them just because we
            // haven't yet been able to check whether the server supports multi
            // user chat yet.  The packets will still be ignored if group IM is
            // disabled in the user's CoS.
            sLog.error("Failed to obtain MUC service names", ex);
        }
    }

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the connection to use for sending the rejection.
     * @param rejectReason the reason to reject the given invitation
     */
    public void rejectInvitation(ChatRoomInvitation invitation,
        String rejectReason)
    {
        try
        {
            MultiUserChatManager.getInstanceFor(getXmppConnection())
                    .decline(
                            invitation.getTargetChatRoom().getIdentifier(),
                            JidCreate.entityBareFrom(invitation.getInviter()),
                            rejectReason);
        }
        catch (NotConnectedException | XmppStringprepException | InterruptedException ex)
        {
            sLog.error("Failed to reject invitation to chat room " +
                       sanitiseChatRoom(invitation.getTargetChatRoom().getIdentifier()), ex);
        }
    }

     /**
     * @return the XMPPConnection currently in use by the jabber provider or
     * null if jabber provider has yet to be initialized.
     */
    private XMPPConnection getXmppConnection()
    {
        return (jabberProvider == null)
            ? null
            :jabberProvider.getConnection();
    }

    /**
     * Makes sure that we are properly connected and that the server supports
     * multi user chats.
     *
     * @throws OperationFailedException if the provider is not registered or
     * the xmpp connection not connected.
     */
    private void assertSupportedAndConnected()
        throws OperationFailedException, OperationNotSupportedException
    {
        XMPPConnection connection = getXmppConnection();
        //throw an exception if the provider is not registered or the xmpp
        //connection not connected.
        if (!jabberProvider.isRegistered() || !connection.isConnected())
        {
            throw new OperationFailedException(
                "Provider not connected to jabber server"
                , OperationFailedException.NETWORK_FAILURE);
        }
    }

    /**
     * In case <tt>roomName</tt> does not represent a complete room id, the
     * method returns a canonincal chat room name in the following form:
     * roomName@muc-servicename.jabserver.com. In case <tt>roomName</tt> is
     * already a canonical room name, the method simply returns it without
     * changing it.
     *
     * @param roomName the name of the room that we'd like to "canonize".
     *
     * @return the canonincal name of the room (which might be equal to
     * roomName in case it was already in a canonical format).
     *
     * @throws OperationFailedException if we fail retrieving the conference
     * service name
     */
    private EntityBareJid getCanonicalRoomName(String roomName)
        throws OperationFailedException
    {
        String resourceString = "";
        try
        {
            if (roomName.indexOf('@') > 0)
            {
                return JidCreate.entityBareFrom(roomName);
            }

            Iterator<DomainBareJid> serviceNamesIter =
                    MultiUserChatManager.getInstanceFor(getXmppConnection())
                            .getMucServiceDomains().iterator();

            if (serviceNamesIter.hasNext())
            {
                resourceString = serviceNamesIter.next().toString();
                return JidCreate.entityBareFrom(roomName + "@" + resourceString);
            }

        }
        catch (InterruptedException | NotConnectedException | XMPPErrorException |
            NoResponseException ex)
        {
            sLog.error("Failed to retrieve conference service name for user: "
                + jabberProvider.getAccountID().getLoggableAccountID()
                + " on server: "
                + jabberProvider.getAccountID().getService()
                , ex);
            throw new OperationFailedException(
                "Failed to retrieve conference service name for user: "
                + jabberProvider.getAccountID().getLoggableAccountID()
                + " on server: "
                + jabberProvider.getAccountID().getService()
                , OperationFailedException.GENERAL_ERROR
                , ex);

        }
        catch (XmppStringprepException ex)
        {
            sLog.error("Failed to construct canonical room name: "
                + roomName + "/" + resourceString
                + " for user: "
                + jabberProvider.getAccountID().getLoggableAccountID()
                + " on server: "
                + jabberProvider.getAccountID().getService()
                , ex);
            throw new OperationFailedException(
                "Failed to construct canonical room name: "
                    + roomName + "/" + resourceString
                    + " for user: "
                    + jabberProvider.getAccountID().getLoggableAccountID()
                    + " on server: "
                    + jabberProvider.getAccountID().getService()
                    , OperationFailedException.GENERAL_ERROR
                    , ex);

        }

        //hmmmm strange.. no service name returned. we should probably throw an
        //exception
        throw new OperationFailedException(
            "Failed to retrieve MultiUserChat service names."
            , OperationFailedException.GENERAL_ERROR);
    }

    /**
     * Adds <tt>chatRoom</tt> to the cache of chat rooms that this operation
     * set is handling.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to cache.
     */
    private void cacheChatRoom(ChatRoom chatRoom)
    {
        this.chatRoomCache.put(chatRoom.getIdentifier(), chatRoom);
    }

    /**
     * Returns a reference to the chat room named <tt>chatRoomName</tt> or
     * null if the room hasn't been cached yet.
     *
     * @param chatRoomName the name of the room we're looking for.
     *
     * @return the <tt>ChatRoomJabberImpl</tt> instance that has been cached
     * for <tt>chatRoomName</tt> or null if no such room has been cached so far.
     */
    public ChatRoomJabberImpl getChatRoom(EntityBareJid chatRoomName)
    {
        return (ChatRoomJabberImpl)this.chatRoomCache.get(chatRoomName);
    }

    /**
     * Remove a chat room from the chat room cache
     *
     * @param chatRoom the chat room to remove
     */
    public void removeChatRoom(ChatRoomJabberImpl chatRoom)
    {
        chatRoomCache.remove(chatRoom.getIdentifier());
    }

    /**
     * Returns the list of currently joined chat rooms for
     * <tt>chatRoomMember</tt>.
     * @param chatRoomMember the member we're looking for
     * @return a list of all currently joined chat rooms
     * @throws OperationFailedException if the operation fails
     * @throws OperationNotSupportedException if the operation is not supported
     */
    public List<String> getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember)
        throws OperationFailedException, OperationNotSupportedException
    {
        assertSupportedAndConnected();

        List<String> joinedRooms = new ArrayList<>();

        try
        {
            List<EntityBareJid> joinedRoomsList =
                    MultiUserChatManager.getInstanceFor(getXmppConnection())
                            .getJoinedRooms(
                                    JidCreate.entityFullFrom(
                                            chatRoomMember.getContactAddressAsString()));
            joinedRooms = joinedRoomsList.stream()
                                         .map(x -> x.toString())
                                         .collect(Collectors.toList());

        }
        catch (XMPPErrorException ex)
        {
            String errTxt = "Failed to get currently joined chatrooms.";
            sLog.error(errTxt, ex);
            XMPPErrorConvertor.reThrowAsOperationFailedException(errTxt, ex);
        }
        catch (NoResponseException | NotConnectedException | XmppStringprepException |
            InterruptedException ex)
        {
            String errTxt = "Failed to get currently joined chatrooms.";
            sLog.error(errTxt, ex);
            throw new OperationFailedException(
                errTxt,
                OperationFailedException.GENERAL_ERROR,
                ex);
        }
        return joinedRooms;
    }

    /**
     * Delivers a <tt>ChatRoomInvitationReceivedEvent</tt> to all
     * registered <tt>ChatRoomInvitationListener</tt>s.
     *
     * @param targetChatRoom the room that invitation refers to
     * @param inviter the inviter that sent the invitation
     * @param reason the reason why the inviter sent the invitation
     * @param isHistorical if true, this is a historical invite sent while the
     * user was offline.  Otherwise, this invitation was received in real-time.
     * @param password the password to use when joining the room
     */
    public void fireInvitationEvent(
        ChatRoom targetChatRoom,
        EntityJid inviter,
        String reason,
        boolean isHistorical,
        byte[] password)
    {
        ChatRoomInvitationJabberImpl invitation
            = new ChatRoomInvitationJabberImpl( targetChatRoom,
                                                inviter,
                                                reason,
                                                isHistorical,
                                                password);

        fireInvitationReceived(invitation);
    }

    /**
     * A listener that is fired anytime an invitation to join a MUC room is
     * received.
     */
    private class SmackInvitationListener
        implements InvitationListener
    {
        /**
         * Called when the an invitation to join a MUC room is received.<p>
         *
         * If the room is password-protected, the invitee will receive a
         * password to use to join the room. If the room is members-only, the
         * the invitee may be added to the member list.
         *
         * On startup, if the roster has not yet been processed, this adds the
         * invitations to a list to be executed later. If the roster has been
         * processed, it accepts the invitations.
         *
         * @param conn the XMPPConnection that received the invitation.
         * @param room the room that invitation refers to.
         * @param inviter the inviter that sent the invitation.
         * (e.g. crone1@shakespeare.lit).
         * @param reason the reason why the inviter sent the invitation.
         * @param password the password to use when joining the room.
         * @param message the message used by the inviter to send the invitation.
         * @param invitation the raw invitation received with the message.
         */
        @Override
        public void invitationReceived(
            XMPPConnection conn,
            MultiUserChat room,
            EntityJid inviter,
            String reason,
            String password,
            Message message,
            Invite invitation)
        {
            String roomName = room.getRoom().toString();

            if (!ConfigurationUtils.isMultiUserChatEnabled())
            {
                sLog.warn("Ignoring multi-user chat invite as MUC disabled");
                return;
            }

            synchronized (mInvitationToChatRoomList)
            {
                if (mInitialRosterProcessed)
                {
                    sLog.info("The roster has already been processed," +
                              " so accepting invitation for room " +
                              sanitiseChatRoom(roomName));
                    acceptInvitation(conn,
                                     room,
                                     inviter,
                                     reason,
                                     password,
                                     message);
                }
                else
                {
                    sLog.info("The roster has not yet been processed, so" +
                              " adding invitation to list for room " +
                              sanitiseChatRoom(roomName));
                    mInvitationToChatRoomList.add(
                        new InvitationToChatRoom(conn,
                                                 room,
                                                 inviter,
                                                 reason,
                                                 password,
                                                 message));
                }
            }

        }
    }

    /**
     * A listener that is fired anytime an invitee declines or rejects an
     * invitation.
     */
    private class SmackInvitationRejectionListener
        implements InvitationRejectionListener
    {
        /**
         * The chat room for this listener.
         */
        private ChatRoom chatRoom;

        /**
         * Creates an instance of <tt>SmackInvitationRejectionListener</tt> and
         * passes to it the chat room for which it will listen for rejection
         * events.
         *
         * @param chatRoom chat room for which this instance will listen for
         * rejection events
         */
        public SmackInvitationRejectionListener(ChatRoom chatRoom)
        {
            this.chatRoom = chatRoom;
        }

        /**
         * Called when the invitee declines the invitation.
         *
         * @param invitee the invitee that declined the invitation.
         * (e.g. hecate@shakespeare.lit).
         * @param reason the reason why the invitee declined the invitation.
         * @param message - the message used to decline the invitation.
         * @param rejection - the raw decline found in the message
         */
        @Override
        public void invitationDeclined(EntityBareJid invitee,
            String reason, Message message,
            Decline rejection)
        {
            sLog.debug("Group chat invitation rejected by " +
                       sanitiseChatAddress(invitee));
            fireInvitationRejectedEvent(chatRoom, invitee.toString(), reason);
        }
    }

    /**
     * Our listener that will tell us when we're registered to Jabber and the
     * smack MultiUserChat is ready to accept us as a listener.
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
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                sLog.info("adding an Invitation listener to the smack muc");

                addSmackInvitationListener();

                setMultiChatSupported();
                // Clearing the backup cache, given we will now repopulate the original cache to be used
                // while the client RegistrationState is registered.
                backupChatRoomCache.clear();
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED
                || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                // Save the information of the cache to the backup one
                if(!chatRoomCache.isEmpty())
                {
                    backupChatRoomCache.putAll(chatRoomCache);
                }
                sLog.info("Clearing cached chatrooms.");
                // clear cached chatrooms as there are no longer valid
                chatRoomCache.clear();
                synchronized (mInvitationToChatRoomList) {
                    mInitialRosterProcessed = false;
                }
            }
        }
    }

    /**
     * Updates corresponding chat room members when a contact has been modified
     * in our contact list.
     * @param evt the <tt>SubscriptionEvent</tt> that notified us
     */
     public void contactModified(ContactPropertyChangeEvent evt)
     {
         Contact modifiedContact = evt.getSourceContact();

         this.updateChatRoomMembers(modifiedContact);
     }

     /**
      * Updates corresponding chat room members when a contact has been created
      * in our contact list.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionCreated(SubscriptionEvent evt)
     {
         Contact createdContact = evt.getSourceContact();

         this.updateChatRoomMembers(createdContact);
     }

     /**
      * Not interested in this event for our member update purposes.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionFailed(SubscriptionEvent evt) {}

     /**
      * Not interested in this event for our member update purposes.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionMoved(SubscriptionMovedEvent evt) {}

     /**
      * Updates corresponding chat room members when a contact has been removed
      * from our contact list.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionRemoved(SubscriptionEvent evt)
     {
         // Set to null the contact reference in all corresponding chat room
         // members.
         this.updateChatRoomMembers(null);
     }

     /**
      * Not interested in this event for our member update purposes.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionResolved(SubscriptionEvent evt) {}

     /**
      * Finds all chat room members, which name corresponds to the name of the
      * given contact and updates their contact references.
      *
      * @param contact the contact we're looking correspondences for.
      */
     private void updateChatRoomMembers(Contact contact)
     {
         if (contact == null)
             return;

         Enumeration<ChatRoom> chatRooms = chatRoomCache.elements();

         while (chatRooms.hasMoreElements())
         {
             ChatRoomJabberImpl chatRoom =
                 (ChatRoomJabberImpl) chatRooms.nextElement();

             ChatRoomMemberJabberImpl member
                 = chatRoom.findMemberForNickName(contact.getAddress());

             if (member != null)
             {
                 member.setContact(contact);
                 member.setAvatar(contact.getImage());
             }
         }
     }

     /**
      * Sets up the Operation Set to be ready for connection to the jabber
      * server.
      */
     protected void prepareForConnection()
     {
         sLog.debug("Preparing OpSetMuc for jabber connection");

         // Currently all that is required here is to reset the number of
         // active chat rooms to 0.
         activeChatRooms.set(0);
     }

     /**
      * Increments or decrements the stored number of active chatrooms.
      *
      * @param add If true, the number is incremented, otherwise it is decremented.
      */
     protected void updateActiveChatRooms(boolean add)
     {
         if (add)
         {
             activeChatRooms.incrementAndGet();
             sLog.debug(
                 "Incremented number of chat rooms to " + activeChatRooms);
         }
         else
         {
             activeChatRooms.decrementAndGet();
             sLog.debug(
                 "Decremented number of chat rooms to " + activeChatRooms);
         }
     }

     /**
      * Checks whether we have reached the maximum number of active chat rooms.
      * If so, find the IDs of the least recently active chat rooms and leave
      * them.
      */
     protected void checkForMaxActiveChatRooms()
     {
         if (activeChatRooms.get() > maxActiveChatRooms)
         {
             // We've reached the maximum active chatrooms, so leave the 10%
             // of the least used chat rooms.
             int roomsToLeave =  (int) Math.round((maxActiveChatRooms * 0.1));

             sLog.info("Need to leave " + roomsToLeave + " chat rooms");
             MessageHistoryService messageHistoryService =
                 JabberActivator.getMessageHistoryService();
             List<String> chatRoomIdsToLeave =
                 messageHistoryService.findLeastActiveChatrooms(roomsToLeave);

             for (String id : chatRoomIdsToLeave)
             {
                 ChatRoom chatRoom = null;
                 try
                 {
                     chatRoom = findRoom(id);
                 }
                 catch (OperationFailedException | OperationNotSupportedException ex)
                 {
                     sLog.error("Failed to find chatroom for id " +
                                sanitiseChatRoom(id), ex);
                 }

                 if (chatRoom != null)
                 {
                     chatRoom.leave(
                         true, LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT, null);
                 }
             }
         }
     }

    /**
     * Adds the smack invitation listener for the current XMPP connection.
     */
    public void addSmackInvitationListener()
    {
        MultiUserChatManager.getInstanceFor(getXmppConnection())
                .addInvitationListener(smackInvitationListener);
    }

    public void cleanUp()
    {
        sLog.info("Cleanup");

        jabberProvider.removeRegistrationStateChangeListener(providerRegListener);
        mMultiChatTimer.cancel();
    }

    /**
     * For strings containing a (sub)string of the form
     * "chatroom-abcd1234@im_domain.com(01234@im_domain.com)", this method will produce a
     * hashed replacement that reads
     * "chatroom-HASH-VALUE(hash)@im_domain.com(ANOTHER-HASH-VALUE(hash)@im_domain.com)".
     */
    private static String sanitiseMUCRoom(MultiUserChat room)
    {
        final StringBuilder stringToReturn = new StringBuilder();

        String[] split = room.toString().split("\\(");

        stringToReturn.append(sanitiseChatRoom(split[0]));
        stringToReturn.append("(");
        stringToReturn.append(sanitiseChatAddress(split[1]));

        return stringToReturn.toString();
    }
}
