/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap.event;

import java.util.*;

import net.java.sip.communicator.service.ldap.LdapDirectory;

/**
 * An LdapEvent is triggered when
 * the state of the LDAP connection changes.
 * Available states for the moment are :
 * connected
 * disconnected
 * connecting
 * disconnecting
 *
 * @author Sebastien Mazy
 */
public class LdapEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates the possible causes why an event was triggered
     */
    public enum LdapEventCause
    {
        /**
         * New result is available.
         */
        NEW_SEARCH_RESULT,

        /**
         * Search is achieved.
         */
        SEARCH_ACHIEVED,

        /**
         * Search is cancelled.
         */
        SEARCH_CANCELLED,

        /**
         * Problem occurred during search.
         */
        SEARCH_ERROR,
    }

    /**
     * the cause of this event
     */
    private final LdapEventCause cause;

    /**
     * the content of this event
     */
    private final Object content;

    /**
     * Another constructor for this class. Use that one to pass more
     * information to the listener using any Object.
     *
     * @param source the LdapDirectory that is the source of the event
     * @param cause the cause why it was triggered
     * @param content related content
     */
    public LdapEvent(LdapDirectory source, LdapEventCause cause,
            Object content)
    {
        super(source);
        this.cause = cause;
        this.content = content;
    }

    /**
     * @return the cause why this event was triggered
     */
    public LdapEventCause getCause()
    {
        return this.cause;
    }

    /**
     * @return the object embedded in this event, or null if there isn't one
     */
    public Object getContent()
    {
        return this.content;
    }

    /**
     * Returns the LdapDirectory which sent the LdapEvent.
     *
     * @return the LdapDirectory which sent the LdapEvent
     */
    public LdapDirectory getSource()
    {
        return (LdapDirectory) super.getSource();
    }
}
