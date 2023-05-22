/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import org.jitsi.service.resources.*;

/**
 * This interface represents chat room participants. Instances are retrieved
 * through implementations of the <tt>ChatRoom</tt> interface and offer methods
 * that allow querying member properties, such as, moderation permissions,
 * associated chat room and other.
 *
 * @author Emil Ivov
 */
public interface ChatRoomMember
{
    /**
     * Returns the chat room that this member is participating in.
     *
     * @return the <tt>ChatRoom</tt> instance that this member belongs to.
     */
    ChatRoom getChatRoom();

    /**
     * Returns the protocol provider instance that this member has originated
     * in.
     *
     * @return the <tt>ProtocolProviderService</tt> instance that created this
     * member and its containing cht room
     */
    ProtocolProviderService getProtocolProvider();

    /**
     * Returns the contact identifier representing this contact. In protocols
     * like IRC this method would return the same as getName() but in others
     * like Jabber, this method would return a full contact id uri.
     *
     * @return a String (contact address), uniquely representing the contact
     * over the service being used by the associated protocol
     * provider instance/
     */
    String getContactAddressAsString();

    /**
     * Returns the name of this member as it is known in its containing
     * chatroom (aka a nickname). The name returned by this method, may
     * sometimes match the string returned by getContactID() which is actually
     * the address of  a contact in the realm of the corresponding protocol.
     *
     * @return the name of this member as it is known in the containing chat
     * room (aka a nickname).
     */
    String getName();

    /**
     * Returns the avatar of this member, that can be used when including it in
     * user interface.
     *
     * @return an avatar (e.g. user photo) of this member.
     */
    BufferedImageFuture getAvatar();

    /**
     * Returns the protocol contact corresponding to this member in our contact
     * list. The contact returned here could be used by the user interface to
     * check if this member is contained in our contact list and in function of
     * this to show additional information add additional functionality.
     *
     * @return the protocol contact corresponding to this member in our contact
     * list.
     */
    Contact getContact();

    /**
     * Returns the role of this chat room member in its containing room.
     *
     * @return a <tt>ChatRoomMemberRole</tt> instance indicating the role
     * the this member in its containing chat room.
     */
    ChatRoomMemberRole getRole();

    /**
     * Sets the role of this chat room member in its containing room.
     *
     * @param role <tt>ChatRoomMemberRole</tt> instance indicating the role
     * to set for this member in its containing chat room.
     */
    void setRole(ChatRoomMemberRole role);
}
