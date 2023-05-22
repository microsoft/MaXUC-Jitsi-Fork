/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.service.protocol.ContactGroupType.JABBER;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactGroup;
import net.java.sip.communicator.service.protocol.ContactGroupType;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * A dummy ContactGroup implementation representing the ContactList root for
 * Jabber contact lists.
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class RootContactGroupJabberImpl
    extends AbstractContactGroupJabberImpl
{
    private String ROOT_CONTACT_GROUP_NAME = "ContactListRoot";

    /**
     * Maps all JIDs in our roster to the actual contacts so that we could
     * easily search the set of existing contacts. Note that we only store
     * lower case strings in the left column because JIDs in XMPP are not case
     * sensitive.
     */
    private final List<ContactGroup> subGroups = new LinkedList<>();

    private boolean isResolved = false;

    /**
     * An empty list that we use when returning an iterator.
     */
    private Map<Jid, Contact> contacts = new Hashtable<>();

    private final ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * Creates a ContactGroup instance.
     */
    RootContactGroupJabberImpl(
        ProtocolProviderServiceJabberImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * The ContactListRoot is the only group that can contain subgroups.
     *
     * @return true (always)
     */
    public boolean canContainSubgroups()
    {
        return true;
    }

    /**
     * Returns the name of this group which is always
     * <tt>ROOT_CONTACT_GROUP_NAME</tt>.
     *
     * @return a String containing the name of this group.
     */
    public String getGroupName()
    {
        return ROOT_CONTACT_GROUP_NAME;
    }

    /**
     * Removes the specified contact from this contact group
     * @param contact the contact to remove.
     */
    void removeContact(ContactJabberImpl contact)
    {
        contacts.remove(contact.getAddressAsJid());
    }

    /**
     * Adds the specified contact to the end of this group.
     * @param contact the new contact to add to this group
     */
    void addContact(ContactJabberImpl contact)
    {
        contacts.put(contact.getAddressAsJid(), contact);
    }

    /**
     * Adds the specified group to the end of the list of sub groups.
     * @param group the group to add.
     */
    void addSubGroup(ContactGroupJabberImpl group)
    {
        if (group == null)
            throw new NullPointerException("Sub group cannot be null");

        synchronized (subGroups)
        {
            subGroups.add(group);
        }
    }

    /**
     * Removes the specified from the list of sub groups
     * @param group the group to remove.
     */
    void removeSubGroup(ContactGroupJabberImpl group)
    {
        synchronized (subGroups)
        {
            removeSubGroup(subGroups.indexOf(group));
        }
    }

    /**
     * Removes the sub group with the specified index.
     * @param index the index of the group to remove
     */
    void removeSubGroup(int index)
    {
        synchronized (subGroups)
        {
            subGroups.remove(index);
        }
    }

    /**
     * Returns the number of subgroups contained by this
     * <tt>RootContactGroupImpl</tt>.
     *
     * @return an int indicating the number of subgroups that this
     *   ContactGroup contains.
     */
    public int countSubgroups()
    {
        synchronized (subGroups)
        {
            return subGroups.size();
        }
    }

    /**
     * Returns null as this is the root contact group.
     * @return null as this is the root contact group.
     */
    public ContactGroup getParentContactGroup()
    {
        return null;
    }

    /**
     * Returns the subgroup with the specified index.
     *
     * @param index the index of the <tt>ContactGroup</tt> to retrieve.
     * @return the <tt>ContactGroup</tt> with the specified index.
     */
    public ContactGroup getGroup(int index)
    {
        synchronized (subGroups)
        {
            return subGroups.get(index);
        }
    }

    /**
     * Returns the subgroup with the specified name.
     * @param groupName the name of the <tt>ContactGroup</tt> to retrieve.
     * @return the <tt>ContactGroup</tt> with the specified index.
     */
    public ContactGroup getGroup(String groupName)
    {
        Iterator<ContactGroup> subgroups = subgroups();
        while (subgroups.hasNext())
        {
            ContactGroup grp = subgroups.next();

            if (grp.getGroupName().equals(groupName))
                return grp;
        }

        return null;
    }

    /**
     * Returns the <tt>Contact</tt> with the specified address or
     * identifier.
      * @param id the address or identifier of the <tt>Contact</tt> we are
     * looking for.
     * @return the <tt>Contact</tt> with the specified id or address.
     */
    public Contact getContact(String id)
    {
        try
        {
            return findContact(JidCreate.from(id.toLowerCase()));
        }
        catch (XmppStringprepException e)
        {
            return null;
        }
    }

    /**
     * Returns the contact encapsulating with the specified name or
     * null if no such contact was found.
     *
     * @param id the id for the contact we're looking for.
     * @return the <tt>ContactJabberImpl</tt> corresponding to the specified
     * jid or null if no such contact existed.
     */
    ContactJabberImpl findContact(Jid id)
    {
        if(id == null)
            return null;
        return (ContactJabberImpl)contacts.get(id);
    }

    /**
     * Returns an iterator over the sub groups that this
     * <tt>ContactGroup</tt> contains.
     *
     * @return a java.util.Iterator over the <tt>ContactGroup</tt>
     *   children of this group (i.e. subgroups).
     */
    public Iterator<ContactGroup> subgroups()
    {
        // Take a copy of the list before returning it to avoid
        // ConcurrentModificationExceptions
        synchronized (subGroups)
        {
            return new ArrayList<>(subGroups).iterator();
        }
    }

    /**
     * Returns the number, which is always 0, of <tt>Contact</tt> members
     * of this <tt>ContactGroup</tt>
     * @return an int indicating the number of <tt>Contact</tt>s, members
     * of this <tt>ContactGroup</tt>.
     */
    public int countContacts()
    {
        return contacts.size();
    }

    /**
     * Returns an Iterator over all contacts, member of this
     * <tt>ContactGroup</tt>.
     * @return a java.util.Iterator over all contacts inside this
     * <tt>ContactGroup</tt>
     */
    public Iterator<Contact> contacts()
    {
        // Take a copy of the map before returning it to avoid
        // ConcurrentModificationExceptions
        Map<Jid, Contact> contactsCopy =
                new Hashtable<>(contacts);
        return contactsCopy.values().iterator();
    }

    /**
     * Returns a string representation of the root contact group that contains
     * the count of all subgroups and subcontacts of this group.
     *
     * @return  a string representation of this root contact group.
     */
    public String toString()
    {
        StringBuffer buff = new StringBuffer(getGroupName());
        buff.append(".subGroups=")
            .append(countSubgroups())
            .append(".rootContacts=")
            .append(countContacts());
        return buff.toString();
    }

    /**
     * Returns the protocol provider that this group belongs to.
     * @return a reference to the ProtocolProviderService instance that this
     * ContactGroup belongs to.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Determines whether or not this contact group is being stored by the
     * server. Non persistent contact groups exist for the sole purpose of
     * containing non persistent contacts.
     * @return true if the contact group is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return true;
    }

    /**
     * Returns null as no persistent data is required and the group name is
     * sufficient for restoring the contact.
     * <p>
     * @return null as no such data is needed.
     */
    public String getPersistentData()
    {
        return null;
    }

    @Override
    public final ContactGroupType groupType()
    {
        return JABBER;
    }

    /**
     * Determines whether or not this group has been resolved against the
     * server. Unresolved groups are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped groups to their on-line buddies.
     * @return true if the group has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    /**
     * Returns a <tt>String</tt> that uniquely represnets the group. In this we
     * use the name of the group as an identifier. This may cause problems
     * though, in clase the name is changed by some other application between
     * consecutive runs of the sip-communicator.
     *
     * @return a String representing this group in a unique and persistent
     * way.
     */
    public String getUID()
    {
        return getGroupName();
    }
}
