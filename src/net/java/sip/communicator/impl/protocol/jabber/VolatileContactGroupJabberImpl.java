/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.Vector;

import org.jivesoftware.smack.roster.RosterEntry;

/**
 * The Jabber implementation of the Volatile ContactGroup interface.
 *
 * @author Damian Minkov
 */
public class VolatileContactGroupJabberImpl
    extends ContactGroupJabberImpl
{
    /**
     * This contact group name
     */
    private final String contactGroupName;

    /**
     * Creates an Jabber group using the specified group name
     * @param groupName String groupname
     * @param ssclCallback a callback to the server stored contact list
     * we're creating.
     */
    VolatileContactGroupJabberImpl(
                        String groupName,
                        ServerStoredContactListJabberImpl ssclCallback)
    {
        super(null, new Vector<RosterEntry>().iterator(), ssclCallback, false);

        this.contactGroupName = groupName;
    }

    /**
     * Returns the name of this group.
     * @return a String containing the name of this group.
     */
    @Override
    public String getGroupName()
    {
        return contactGroupName;
    }

    /**
     * Returns a string representation of this group, in the form
     * JabberGroup.GroupName, childContacts=[size].
     * @return  a String representation of the object.
     */
    @Override
    public String toString()
    {
        StringBuffer buff = new StringBuffer("VolatileJabberGroup.");
        buff.append(getGroupName())
            .append(", childContacts=")
            .append(countContacts());
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
}
