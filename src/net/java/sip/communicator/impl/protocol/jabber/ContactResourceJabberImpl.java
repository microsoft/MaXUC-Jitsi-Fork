/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.parts.Resourcepart;

import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactResource;
import net.java.sip.communicator.service.protocol.PresenceStatus;

/**
 *
 * @author Yana Stamcheva
 */
public class ContactResourceJabberImpl
    extends ContactResource
{
    private final FullJid fullJid;

    /**
     * Creates a <tt>ContactResource</tt> by specifying the
     * <tt>resourceName</tt>, the <tt>presenceStatus</tt> and the
     * <tt>priority</tt>.
     *
     * @param fullJid the full jid corresponding to this contact resource
     * @param contact
     * @param resourceName
     * @param presenceStatus
     * @param priority
     * @param canBeMessaged
     */
    public ContactResourceJabberImpl(   FullJid fullJid,
                                        Contact contact,
                                        Resourcepart resourceName,
                                        PresenceStatus presenceStatus,
                                        int priority,
                                        boolean canBeMessaged)
    {
        super(  contact,
                resourceName.toString(),
                presenceStatus,
                priority,
                canBeMessaged);

        this.fullJid = fullJid;
    }

    /**
     * Returns the full jid corresponding to this contact resource.
     *
     * @return the full jid corresponding to this contact resource
     */
    public FullJid getFullJid()
    {
        return fullJid;
    }

    /**
     * Sets the new <tt>PresenceStatus</tt> of this resource.
     *
     * @param newStatus the new <tt>PresenceStatus</tt> to set
     */
    protected void setPresenceStatus(PresenceStatus newStatus)
    {
        this.presenceStatus = newStatus;
    }
}
