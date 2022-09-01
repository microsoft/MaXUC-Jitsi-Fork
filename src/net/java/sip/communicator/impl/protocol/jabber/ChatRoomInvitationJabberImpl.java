/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import org.jxmpp.jid.EntityJid;

import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomInvitation;

/**
 * The Jabber implementation of the <tt>ChatRoomInvitation</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomInvitationJabberImpl
    implements ChatRoomInvitation
{
    private ChatRoom chatRoom;

    private EntityJid inviter;

    private String reason;

    private boolean isHistorical;

    private byte[] password;

    /**
     * Creates an invitation for the given <tt>targetChatRoom</tt>, from the
     * given <tt>inviter</tt>.
     *
     * @param targetChatRoom the <tt>ChatRoom</tt> for which the invitation is
     * @param inviter the <tt>ChatRoomMember</tt>, which sent the invitation
     * @param reason the reason of the invitation
     * @param isHistorical if true, this is a historical invite sent while the
     * user was offline.  Otherwise, this invitation was received in real-time.
     * @param password the password
     */
    public ChatRoomInvitationJabberImpl(ChatRoom targetChatRoom,
                                        EntityJid inviter,
                                        String reason,
                                        boolean isHistorical,
                                        byte[] password)
    {
        this.chatRoom = targetChatRoom;
        this.inviter = inviter;
        this.reason = reason;
        this.isHistorical = isHistorical;
        this.password = password;
    }

    public ChatRoom getTargetChatRoom()
    {
        return chatRoom;
    }

    public EntityJid getInviter()
    {
        return inviter;
    }

    public String getReason()
    {
        return reason;
    }

    public boolean isHistorical()
    {
        return isHistorical;
    }

    public byte[] getChatRoomPassword()
    {
        return password;
    }
}
