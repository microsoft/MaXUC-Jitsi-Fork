// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jxmpp.jid.Jid;

import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationNotSupportedException;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceChangeEvent;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * The ChatRoomManager class handles joining and leaving chat rooms based on
 * roster changes and join/leave/create/invite messages received from the
 * message archive.
 */
public class ChatRoomManager
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(ChatRoomManager.class);

    /**
     * The jabber protocol provider
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * The multi user chat operation set.
     */
    private OperationSetMultiUserChatJabberImpl opSetMuc;

    /**
     * Boolean indicating whether or not we are currently waiting for the
     * initial message archive. Used to indicate whether we can expect to
     * receive any more join/leave timestamps from the archive.
     */
    private final AtomicBoolean waitingForArchive = new AtomicBoolean();

    /**
     * Boolean indicating whether we have finished processing the initial
     * roster request.  Used to indicate whether we can expect anymore requests
     * to join/leave chat rooms from the initial roster.
     */
    private final AtomicBoolean rosterProcessed = new AtomicBoolean();

    /**
     * Set of chat room Ids that the roster has told us we need to join.
     */
    private final Set<Jid> chatRoomIdsToJoin = new HashSet<>();

    /**
     * Set of chat room Ids that the roster has told us we need to leave.
     */
    private final Set<Jid> chatRoomIdsToLeave = new HashSet<>();

    /**
     * Map of chat room Ids to the date when we joined the chat room, according
     * to the message archive.
     */
    private final Map<Jid,Date> chatRoomJoinDates = new HashMap<>();

    /**
     * Map of chat room Ids to the date when we left the chat room, according
     * to the message archive.
     */
    private final Map<Jid,Date> chatRoomLeaveDates = new HashMap<>();

    /**
     *  Lock object to synchronize access to the join and leave sets and maps.
     */
    private final Object joinLeaveLock = new Object();

    /**
     * A timer to schedule the requests to join and leave chat rooms so that
     * they all happen on one thread in the correct order but don't block
     * start-up.
     */
    private Timer joinAndLeaveChatRoomTimer;

    /**
     * If we fail to join a chat room (despite having a healthy XMPP connection), retry every 15s.
     */
    private static final int JOIN_CHAT_ROOM_RETRY_DELAY_MS = 15000;

    /**
     * Creates a chat room manager instance.
     *
     * @param provider the jabber protocol provider.
     */
    ChatRoomManager(ProtocolProviderServiceJabberImpl provider)
    {
        logger.info("Creating chat room manager");
        jabberProvider = provider;
        waitingForArchive.set(true);
    }

    /**
     * Sets up the chat room manager to be ready for connection to the jabber
     * server.
     */
    protected void prepareForConnection()
    {
        logger.debug("prepareForConnection");
        synchronized (joinLeaveLock)
        {
            logger.info("Setting up chat room manager for jabber connection");

            chatRoomIdsToJoin.clear();
            chatRoomIdsToLeave.clear();
            chatRoomJoinDates.clear();
            chatRoomLeaveDates.clear();

            rosterProcessed.set(false);
            waitingForArchive.set(true);

            if (joinAndLeaveChatRoomTimer != null)
            {
                logger.debug("Cancel join chat room timer ready to make new one");
                joinAndLeaveChatRoomTimer.cancel();
            }

            joinAndLeaveChatRoomTimer = new Timer("Join chat room timer");
        }
    }

    /**
     * Returns the jabber implementation of the MultiUserChat operation set.
     *
     * @return the jabber implementation of the MultiUserChat operation set.
     */
    private OperationSetMultiUserChatJabberImpl getOpSetMuc()
    {
        // If we haven't already got a reference to the operation set, get it
        // from the jabber provider.  We can't get this when the chat room
        // manager is created, as it isn't available yet.
        if (opSetMuc == null)
        {
            opSetMuc = (OperationSetMultiUserChatJabberImpl)
                    (jabberProvider.getOperationSet(OperationSetMultiUserChat.class));
        }

        return opSetMuc;
    }

    /**
     * Used by the roster to indicate that we need to join the chat room with
     * the given ID.
     *
     * @param chatRoomId the ID of the chat room to join.
     */
    protected void chatRoomReceivedFromRoster(Jid chatRoomId)
    {
        boolean joinNow = false;
        Date joinDate;

        synchronized (joinLeaveLock)
        {
            // We've found this chat room ID in the roster, so we know we need
            // to join it, but only join it now if we've already received the
            // date at which we joined from the message archive, or if we're
            // not waiting for any more messages from the archive.
            joinDate = chatRoomJoinDates.remove(chatRoomId);

            if (joinDate != null || !waitingForArchive.get())
            {
                joinNow = true;
            }
            else
            {
                logger.debug(
                    "Adding chat room " + chatRoomId + " to chat rooms to join");
                chatRoomIdsToJoin.add(chatRoomId);
            }
        }

        if (joinNow)
        {
            logger.debug(
                "Joining chat room " + chatRoomId + " with date " + joinDate);
            addAndJoinChatRoomOnTimer(chatRoomId, joinDate);
        }
    }

    /**
     * Used by the roster to indicate that we need to leave the chat room with
     * the given ID.
     *
     * @param chatRoomId the ID of the chat room to leave.
     */
    protected void chatRoomLeftFromRoster(Jid chatRoomId)
    {
        boolean leaveNow = false;
        Date leaveDate;

        synchronized (joinLeaveLock)
        {
            // We've not found this chat room ID in the roster, so we know we
            // need to leave it, but only leave now if we've already received
            // the date at which we left from the message archive, or if we're
            // not waiting for any more messages from the archive.
            leaveDate = chatRoomLeaveDates.remove(chatRoomId);

            if (leaveDate != null || !waitingForArchive.get())
            {
                leaveNow = true;
            }
            else
            {
                logger.debug("Adding chat room " + chatRoomId + " to chat rooms to leave");
                chatRoomIdsToLeave.add(chatRoomId);
            }
        }

        if (leaveNow)
        {
            logger.debug("Leaving chat room " + chatRoomId + " with date " + leaveDate);
            removeAndLeaveChatRoomOnTimer(chatRoomId, leaveDate);
        }
    }

    /**
     * Used by the jabber basic instant messaging op set to notify the chat
     * room manager that it has received a chat room join date in an archive
     * message.
     *
     * @param chatRoomId the ID of the chat room to join.
     * @param joinDate the date at which we joined the chat room.
     */
    protected void chatRoomJoinDateReceived(Jid chatRoomId, Date joinDate)
    {
        synchronized (joinLeaveLock)
        {
            if (chatRoomIdsToJoin.remove(chatRoomId))
            {
                logger.debug("Joining chat room " + chatRoomId + " with date " + joinDate);
                addAndJoinChatRoomOnTimer(chatRoomId, joinDate);
            }
            else
            {
                logger.debug("Adding chat room " + chatRoomId + " to chat rooms to join");
                chatRoomJoinDates.put(chatRoomId, joinDate);
            }
        }
    }

    /**
     * Used by the jabber basic instant messaging op set to notify the chat
     * room manager that it has received a chat room leave date in an archive
     * message.
     *
     * @param chatRoomId the ID of the chat room to leave.
     * @param leaveDate the date at which we left the chat room.
     */
    protected void chatRoomLeaveReceived(Jid chatRoomId, Date leaveDate)
    {
        synchronized (joinLeaveLock)
        {
            if (chatRoomIdsToLeave.remove(chatRoomId))
            {
                logger.debug("Leaving chat room " + chatRoomId + " with date " + leaveDate);
                removeAndLeaveChatRoomOnTimer(chatRoomId, leaveDate);
            }
            else
            {
                logger.debug("Adding chat room " + chatRoomId + " to chat rooms to leave");
                chatRoomLeaveDates.put(chatRoomId, leaveDate);
            }
        }
    }

    /**
     * Executed when we have finished processing the initial roster so we no
     * longer block joining or leaving chat rooms on having received roster
     * updates.
     */
    protected void rosterProcessed()
    {
        rosterProcessed.set(true);

        if (!waitingForArchive.get())
        {
            logger.debug("Roster and archive query complete");
            finishJoinAndLeaveChatRooms();
        }
    }

    /**
     * Sets whether or not we are processing the initial message archive query.
     * If we are, we block joining or leaving a chat room until we have
     * received the date when we joined/left the chat room from the archive.
     *
     * @param isComplete if true, we are have finished processing the initial message archive query.
     */
    public void setArchiveQueryComplete(boolean isComplete)
    {
        // Whether we are waiting for the archive depends on whether the archive query is complete.
        waitingForArchive.set(!isComplete);

        if (isComplete && rosterProcessed.get())
        {
            logger.debug("Roster and archive query complete");
            finishJoinAndLeaveChatRooms();
        }
    }

    /**
     * Executed once we have both finished processing the roster and receiving
     * the initial archive request to join and leave all chat rooms that are
     * still pending.
     */
    private void finishJoinAndLeaveChatRooms()
    {
        synchronized (joinLeaveLock)
        {
            logger.info("Finishing join and leave chat rooms");

            for (Jid chatRoomIdToJoin : chatRoomIdsToJoin)
            {
                logger.debug("Joining chat room " + chatRoomIdToJoin);
                addAndJoinChatRoomOnTimer(
                    chatRoomIdToJoin, chatRoomJoinDates.remove(chatRoomIdToJoin));
            }

            for (Jid chatRoomIdToLeave : chatRoomIdsToLeave)
            {
                logger.debug("Leaving chat room " + chatRoomIdToLeave);
                removeAndLeaveChatRoomOnTimer(
                    chatRoomIdToLeave, chatRoomLeaveDates.remove(chatRoomIdToLeave));
            }

            chatRoomIdsToJoin.clear();
            chatRoomIdsToLeave.clear();
            chatRoomJoinDates.clear();
            chatRoomLeaveDates.clear();
        }

        // When the roster has finished being processed after startup, we
        // process any chat room invitations sent while we were offline. This
        // order is crucial to prevent a race condition (see SFR 541779).
        logger.info("The roster is processed: adding invitations.");
        getOpSetMuc().onRosterProcessed();

        OperationSetPersistentPresenceJabberImpl opSetPersPres =
            (OperationSetPersistentPresenceJabberImpl) jabberProvider.
                     getOperationSet(OperationSetPersistentPresence.class);

        // While we were processing the initial roster, the server stored
        // contact list was caching any new roster updates that it received to
        // be sure we don't get out of sync.  Now that we've processed all of
        // the chat rooms in the initial roster, we can ask the server stored
        // contact list to join/leave any chat rooms based those cached updates.
        if (opSetPersPres != null)
        {
            opSetPersPres.getServerStoredContactList().processPendingRosterUpdates();
        }
        else
        {
            // This really shouldn't happen!
            logger.error("Persistent Presence Op Set is null - " +
                         "unable to process pending roster updates!");
        }
    }

    /**
     * Schedules a timer task to delete the existing config for the chat room
     * with the given ID, mark it as left in message history and, if the user
     * has joined the room, leave the chat room on the server.
     *
     * @param chatRoomId the ID of the chat room
     * @param leaveDate the date at which we left the chat room, possibly on
     * another client.
     */
    private void removeAndLeaveChatRoomOnTimer(final Jid chatRoomId,
                                               final Date leaveDate)
    {
        // Schedule a task on the timer to leave the chat room to ensure that
        // real-time join/leave requests that we receive from the server are
        // executed in the order that they are received.
        joinAndLeaveChatRoomTimer.schedule(new TimerTask()
        {
            public void run()
            {
                removeAndLeaveChatRoom(chatRoomId, leaveDate);
            }
        },0);
    }

    /**
     * Deletes the existing config for the chat room with the given ID, marks
     * it as left in message history and, if the user has joined the room,
     * leaves the chat room on the server.
     *
     * @param chatRoomId the ID of the chat room
     * @param leaveDate the date at which we left the chat room, possibly on
     * another client.
     */
    private void removeAndLeaveChatRoom(Jid chatRoomId, Date leaveDate)
    {
        logger.info(
            "Removing chat room with id " + chatRoomId + " and date " + leaveDate);
        String chatRoomIdString = chatRoomId.toString();
        ConfigurationUtils.deleteChatRoom(jabberProvider, chatRoomIdString);

        ChatRoom chatRoom = null;

        // First, try and find the chat room with this id.
        try
        {
            chatRoom = getOpSetMuc().findRoom(chatRoomIdString);
        }
        catch (OperationFailedException | OperationNotSupportedException e)
        {
            logger.error("Failed to find chat room with id: "
                + chatRoomId, e);
        }

        // If we find a chat room, mark it as left in history so that it is
        // displayed correctly in the 'Recent' tab.
        if (chatRoom != null)
        {
            // If we've been asked to leave the room and we are currently in
            // the room, leave it now.
            if (chatRoom.isJoined())
            {
                logger.info("Leaving chat room with id: " + chatRoomId);
                chatRoom.leave(false,
                               leaveDate,
                               LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT,
                               null);
            }

            MessageHistoryService msgHistoryService =
                                     JabberActivator.getMessageHistoryService();
            if (msgHistoryService != null)
            {
                logger.debug("Setting chat room as closed: " + chatRoomId);
                msgHistoryService.setChatroomClosedStatus(chatRoom, true);
            }
            else
            {
                logger.warn("Unable to set chat room as closed as message " +
                                      "history service is null: " + chatRoomId);
            }
        }
    }

    /**
     * Joins the chat room with the given ID and saves the chat room's details
     * to config.
     *
     * @param chatRoomId the id of the chat room to join and add to config.
     * @param joinDate the date at which we joined the chat room, possibly on
     * another client.
     */
    private void addAndJoinChatRoomOnTimer(final Jid chatRoomId, final Date joinDate)
    {
        addAndJoinChatRoomOnTimer(chatRoomId, joinDate, 0);
    }

    private void addAndJoinChatRoomOnTimer(final Jid chatRoomId, final Date joinDate, final int delay)
    {
        // Schedule a task on the timer to join the chat room so
        // we join them one at a time without blocking start-up.
        joinAndLeaveChatRoomTimer.schedule(new TimerTask()
        {
            public void run()
            {
                boolean success = false;
                try
                {
                    success = addAndJoinChatRoom(chatRoomId, joinDate);
                }
                catch (Throwable th)
                {
                    logger.error("addAndJoinChatRoom ended unexpectedly!: ", th);
                }

                if (!success)
                {
                    logger.error("Failed to join chat room. Try again in " + JOIN_CHAT_ROOM_RETRY_DELAY_MS + "ms");
                    addAndJoinChatRoomOnTimer(chatRoomId, joinDate, JOIN_CHAT_ROOM_RETRY_DELAY_MS);
                }
            }
        }, delay);
    }

    /**
     * Joins the chat room with the given ID and saves the chat room's details
     * to config.
     *
     * @param chatRoomId the id of the chat room to join and add to config.
     * @param joinDate the date at which we joined the chat room, possibly on
     * another client.
     * @return success Whether we successfully joined the group chat or not.
     */
    private boolean addAndJoinChatRoom(Jid chatRoomId, Date joinDate)
    {
        logger.info(
            "Joining chat room with id " + chatRoomId + " and date " + joinDate);

        String chatRoomIdString = chatRoomId.toString();
        ConfigurationUtils.saveChatRoom(
             jabberProvider, chatRoomIdString, chatRoomIdString, chatRoomIdString, null);

        ChatRoom chatRoom = null;

        try
        {
            // First, see if we can find an existing chat room with this ID.
            logger.debug("Looking for existing chat room: " + chatRoomId);
            chatRoom = getOpSetMuc().findRoom(chatRoomIdString);

            // If we don't find an existing chat room, create one.
            if (chatRoom == null)
            {
                logger.debug("Creating new chat room with id: " + chatRoomId);
                chatRoom = getOpSetMuc().createChatRoom(chatRoomIdString, null);
            }

            // Finally , join the chat room.
            if (chatRoom != null)
            {
                logger.debug("Joining chat room with id: " + chatRoomId);
                chatRoom.join(joinDate);
            }
            else
            {
                logger.error("Failed to find or create chat room with id: " + chatRoomId);
                return false;
            }
        }
        catch (OperationFailedException | OperationNotSupportedException e)
        {
            logger.error("Failed to find or create chat room with id: "
                + chatRoomId, e);
            return false;
        }

        return true;
    }

    /**
     * Cancel the join and leave chat room timer.
     */
    public void cleanUp()
    {
        logger.info("Cleanup");

        if (joinAndLeaveChatRoomTimer != null)
        {
            logger.debug("Cancel join chat room timer");
            joinAndLeaveChatRoomTimer.cancel();
        }
    }
}
