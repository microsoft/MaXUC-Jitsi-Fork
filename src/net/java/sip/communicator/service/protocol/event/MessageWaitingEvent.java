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
 * <tt>MessageWaitingEvent<tt> indicates a message waiting event
 * is received.
 *
 * @author Damian Minkov
 */
public class MessageWaitingEvent
        extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The URI we can use to reach messages from provider that is firing
     * the event.
     */
    private String account;

    /**
     * Number of new/unread messages.
     */
    private int unreadMessages = 0;

    /**
     * Constructs the Event with the given source, typically the provider,
     * and number of unread messages.
     *
     * @param source the protocol provider from which this event is coming.
     * @param account the account URI we can use to reach the messages.
     * @param unreadMessages the unread messages.
     */
    public MessageWaitingEvent(
            ProtocolProviderService source,
            String account,
            int unreadMessages)
    {
        super(source);

        this.account = account;
        this.unreadMessages = unreadMessages;
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt> which originated this event.
     *
     * @return the source <tt>ProtocolProviderService</tt>
     */
    public ProtocolProviderService getSourceProvider()
    {
        return (ProtocolProviderService) getSource();
    }

    /**
     * The URI we can use to reach messages from provider that is firing
     * the event.
     * @return account URI.
     */
    public String getAccount()
    {
        return account;
    }

    /**
     * Number of new/unread messages.
     * @return Number of new/unread messages.
     */
    public int getUnreadMessages()
    {
        return unreadMessages;
    }
}
