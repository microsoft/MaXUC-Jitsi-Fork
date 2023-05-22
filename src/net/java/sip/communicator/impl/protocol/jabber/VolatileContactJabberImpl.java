/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static org.jitsi.util.Hasher.logHasher;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;

import net.java.sip.communicator.util.Logger;

/**
 * The Jabber implementation for Volatile Contact
 * @author Damian Minkov
 */
public class VolatileContactJabberImpl
    extends ContactJabberImpl
{
    /**
     * The logger.
     */
    private static final Logger logger =
                             Logger.getLogger(VolatileContactJabberImpl.class);

    /**
     * This contact id
     */
    private String contactId = null;

    private final String displayName;

    /**
     * Creates an Volatile JabberContactImpl with the specified id
     * @param id String the user id/address
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     *                     instance that created us.
     * @param displayName
     */
    VolatileContactJabberImpl(String id,
                              ServerStoredContactListJabberImpl ssclCallback,
                              String displayName)
    {
        super(null, ssclCallback, false, false);

        this.contactId = id;
        this.displayName = displayName;
    }

    /**
     * Returns the Jabber Userid of this contact
     * @return the Jabber Userid of this contact
     */
    @Override
    public String getAddress()
    {
        return contactId;
    }

    /**
     * Returns the Jabber Userid of this contact as Jid
     * @return the Jabber Userid of this contact as Jid
     */
    @Override
    public BareJid getAddressAsJid()
    {
        return JidCreate.bareFromOrNull(contactId);
    }

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact. An alias is not necessarily unique but is
     * often more human readable than an address (or id).
     * @return a String that can be used for referring to this contact when
     * interacting with the user.
     */
    @Override
    public String getDisplayName()
    {
        if (!org.jitsi.util.StringUtils.isNullOrEmpty(displayName))
        {
            return displayName;
        }
        else
        {
            logger.debug("Returning contactId because displayName = " +
                                                  logHasher(displayName));
            return contactId;
        }
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    @Override
    public String toString()
    {
        StringBuffer buff =  new StringBuffer("VolatileJabberContact[ id=");
        buff.append(logHasher(getAddress())).append("]");

        return buff.toString();
    }

    /**
     * Determines whether or not this contact group is being stored by the
     * server. Non persistent contact groups exist for the sole purpose of
     * containing non persistent contacts.
     * @return true if the contact group is persistent and false otherwise.
     */
    @Override
    public boolean isPersistent()
    {
        return false;
    }

    /**
     * If a subscriber receives a chat message while offline and connects for
     * the first time with no local config/chat history (i.e. a fresh install),
     * this sending contact will appear as a Volatile
     * Jabber contact. Should the IM address of this Volatile contact
     * match with an existing contact for this user - such as a BG contact -
     * we want to be able to merge the two.
     */
    @Override
    public boolean canBeMerged()
    {
        return true;
    }
}
