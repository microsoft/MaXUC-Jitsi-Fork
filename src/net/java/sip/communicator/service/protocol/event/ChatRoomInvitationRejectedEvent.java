/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>ChatRoomInvitationRejectedEvent</tt>s indicates the reception of a
 * rejection of an invitation.
 *
 * @author Emil Ivov
 * @author Stephane Remy
 * @author Yana Stamcheva
 */
public class ChatRoomInvitationRejectedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>ChatRoom</tt> for which the initial invitation was.
     */
    private ChatRoom chatRoom;

    /**
     * The invitee that rejected the invitation.
     */
    private String invitee;

    /**
     * The reason why this invitation is rejected or null if there is no reason
     * specified.
     */
    private String reason;

    /**
     * The exact date at which this event occured.
     */
    private Date timestamp;

    /**
     * Creates a <tt>ChatRoomInvitationRejectedEvent</tt> representing the
     * rejection of an invitation, rejected by the given <tt>invitee</tt>.
     *
     * @param source the <tt>OperationSetMultiUserChat</tt> that dispatches this
     * event
     * @param chatRoom the <tt>ChatRoom</tt> for which the initial invitation
     * was
     * @param invitee the name of the invitee that rejected the invitation
     * @param reason the reason of the rejection
     * @param timestamp the exact date when the event ocurred
     */
    public ChatRoomInvitationRejectedEvent( OperationSetMultiUserChat source,
                                            ChatRoom chatRoom,
                                            String invitee,
                                            String reason,
                                            Date timestamp)
    {
        super(source);

        this.chatRoom = chatRoom;
        this.invitee = invitee;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    /**
     * Returns the multi user chat operation set that dispatches this event.
     *
     * @return the multi user chat operation set that dispatches this event
     */
    public OperationSetMultiUserChat getSourceOperationSet()
    {
        return (OperationSetMultiUserChat)getSource();
    }

    /**
     * Returns the <tt>ChatRoom</tt> for which the initial invitation was.
     *
     * @return the <tt>ChatRoom</tt> for which the initial invitation was
     */
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    /**
     * Returns the name of the invitee that rejected the invitation.
     *
     * @return the name of the invitee that rejected the invitation
     */
    public String getInvitee()
    {
        return invitee;
    }

    /**
     * Returns the reason for which the <tt>ChatRoomInvitation</tt> is rejected.
     *
     * @return the reason for which the <tt>ChatRoomInvitation</tt> is rejected.
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * A timestamp indicating the exact date when the event ocurred.
     * @return a Date indicating when the event ocurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }
}
