/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

/**
 * The <tt>ChatSessionRenderer</tt> is the connector between the
 * <tt>ChatSession</tt> and the <tt>ChatPanel</tt>, which represents the UI
 * part of the chat.
 *
 * @author Yana Stamcheva
 */
public interface ChatSessionRenderer
{
    /**
     * Sets the name of the given chat contact.
     *
     * @param chatContact the chat contact to be modified.
     * @param name the new name.
     */
    void setContactName(ChatContact<?> chatContact, String name);

    /**
     * Adds the given chat transport to the UI.
     *
     * @param chatTransport the chat transport to add.
     */
    void addChatTransport(ChatTransport chatTransport);

    /**
     * Removes the given chat transport from the UI.
     *
     * @param chatTransport the chat transport to remove.
     */
    void removeChatTransport(ChatTransport chatTransport);

    /**
     * Adds the given chat contact to the UI.
     *
     * @param chatContact the chat contact to add.
     */
    void addChatContact(ChatContact<?> chatContact);

    /**
     * Removes the given chat contact from the UI.
     *
     * @param chatContact the chat contact to remove.
     */
    void removeChatContact(ChatContact<?> chatContact);

    /**
     * Removes all chat contacts from the contact list of the chat.
     */
    void removeAllChatContacts();

    /**
     * Refreshes all chat contacts from the contact list of the chat.
     */
    void refreshAllChatContacts();

    /**
     * Updates the status of the given chat transport.
     *
     * @param chatTransport the chat transport to update.
     */
    void updateChatTransportStatus(ChatTransport chatTransport);

    /**
     * Sets the given <tt>chatTransport</tt> to be the selected chat transport.
     *
     * @param chatTransport the <tt>ChatTransport</tt> to select
     */
    void setSelectedChatTransport(ChatTransport chatTransport);

    /**
     * Sets the chat subject.
     *
     * @param subject the new subject to set.
     */
    void setChatSubject(String subject);

    /**
     * Resets the current chat transport to send to all registered resources
     * for the session.
     */
    void resetChatTransport();
}
