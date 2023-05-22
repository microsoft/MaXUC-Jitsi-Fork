/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;
import org.jxmpp.jid.Jid;

/**
 * Dispatched to notify interested parties that a change in our presence in
 * the source chat room has occured. Changes may include us being kicked, join,
 * left, etc.
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class LocalUserChatRoomPresenceChangeEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that this event was triggered as a result of the local
     * participant joining a chat room.
     */
    public static final String LOCAL_USER_JOINED = "LocalUserJoined";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant failed to join a chat room.
     */
    public static final String LOCAL_USER_JOIN_FAILED = "LocalUserJoinFailed";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant leaving a chat room.
     */
    public static final String LOCAL_USER_LEFT = "LocalUserLeft";

   /**
    * Indicates that this event was triggered as a result of the local
    * participant being kicked from a chat room.
    */
    public static final String LOCAL_USER_KICKED = "LocalUserKicked";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant beeing disconnected from the server brutally, or ping timeout.
     */
    public static final String LOCAL_USER_DROPPED = "LocalUserDropped";

    /**
     * The <tt>ChatRoom</tt> to which the change is related.
     */
    private ChatRoom chatRoom = null;

    /**
     * The local user id that this event relates to.
     */
    private Jid localUserId = null;

    /**
     * The type of this event.
     */
    private String eventType = null;

    /**
     * An optional String indicating a possible reason as to why the event
     * might have occurred.
     */
    private String reason = null;

    /**
     * The UID of this event
     */
    private String uid;

    /**
     * The timestamp of when this event was created.
     */
    private final Date timestamp;

    /**
     * Creates a <tt>ChatRoomLocalUserPresenceChangeEvent</tt> representing that
     * a change in local participant presence in the source chat room has
     * occured.
     *
     * @param source the <tt>OperationSetMultiUserChat</tt>, which produced this
     * event
     * @param chatRoom the <tt>ChatRoom</tt> that this event is about
     * @param localUserId the ID of the local user whose presence has changed
     * @param eventType the type of this event.
     * @param reason the reason explaining why this event might have occurred
     * @param timestamp the timestamp when the event occurred.
     */
    public LocalUserChatRoomPresenceChangeEvent(OperationSetMultiUserChat source,
                                                ChatRoom chatRoom,
                                                Jid localUserId,
                                                String eventType,
                                                String reason,
                                                Date timestamp)
    {
        super(source);

        this.chatRoom = chatRoom;
        this.localUserId = localUserId;
        this.eventType = eventType;
        this.uid = UUID.randomUUID().toString();
        this.timestamp = (timestamp == null) ? new Date() : timestamp;

        if (reason == null)
        {
            ResourceManagementService resources =
                                     ProtocolProviderActivator.getResourceService();
            String reasonResource = null;
            if (eventType.equals(LOCAL_USER_JOINED))
            {
                reasonResource = "service.gui.CHAT_ROOM_USER_JOINED";
            }
            else if (eventType.equals(LOCAL_USER_LEFT))
            {
                reasonResource = "service.gui.CHAT_ROOM_USER_LEFT";
            }

            if (reasonResource != null)
            {
                reason =  resources.getI18NString(reasonResource);
            }
        }

        this.reason = reason;
    }

    /**
     * Returns the <tt>OperationSetMultiUserChat</tt>, where this event has
     * occurred.
     *
     * @return the <tt>OperationSetMultiUserChat</tt>, where this event has
     * occurred
     */
    public OperationSetMultiUserChat getMultiUserChatOpSet()
    {
        return (OperationSetMultiUserChat) getSource();
    }

    /**
     * Returns the <tt>ChatRoom</tt>, that this event is about.
     *
     * @return the <tt>ChatRoom</tt>, that this event is about
     */
    public ChatRoom getChatRoom()
    {
        return this.chatRoom;
    }

    /**
     * Returns the local user id that this event relates to.
     *
     * @return the local user id that this event relates to.
     */
    public Jid getLocalUserId()
    {
        return localUserId;
    }

    /**
     * A reason string indicating a human readable reason for this event.
     *
     * @return a human readable String containing the reason for this event,
     * or null if no particular reason was specified
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Returns the type of this event which could be one of the LOCAL_USER_XXX
     * member fields.
     *
     * @return one of the LOCAL_USER_XXX fields indicating the type of this event.
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * Returns the timestamp of this event
     *
     * @return the timestamp of this event
     */
    public Date getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns the UID of this event
     *
     * @return the UID of this event
     */
    public String getUID()
    {
        return uid;
    }

    /**
     * Returns true if this this event represents the user leaving the chat
     * room, false otherwise.
     *
     * @return true if this this event represents the user leaving the chat
     * room, false otherwise.
     */
    public boolean isLeavingEvent()
    {
        return !(eventType.equals(
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED));
    }

    /**
     * Returns a String representation of this event.
     *
     * @return String representation of this event
     */
    public String toString()
    {
        return "ChatRoomLocalUserPresenceChangeEvent[type="
            + getEventType()
            + " sourceRoom="
            + getChatRoom()
            + "]";
    }
}
