// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * An OperationSet that provides methods for the management of Group Contacts.
 * This includes methods to create, edit and get information about Group
 * Contacts. It also includes a method to start a group chat, as this is quite
 * a complex operation so it makes sense to have a single place that can be
 * called into to do this.  There is no method to start a conference, as the
 * conference service already has a single method that can be called to do
 * that.
 */
public interface OperationSetGroupContacts extends OperationSet
{
    /**
     * Creates a new GroupContact with the given display name and set of
     * MetaContact members.
     *
     * @param displayName the display name
     * @param metaContacts the set of MetaContact members
     *
     * @return the newly created group contact
     */
    Contact createGroupContact(String displayName,
                               Set<MetaContact> metaContacts);

    /**
     * Sets the display name of the given GroupContact.
     *
     * @param groupContact the GroupContact
     * @param displayName the display name
     */
    void setDisplayName(Contact groupContact, String displayName);

    /**
     * Sets the current MetaContact members of the given GroupContact to the
     * given set of MetaContacts, discarding any existing set of MetaContact
     * members.
     *
     * @param groupContact the GroupContact
     * @param metaContacts the set of MetaContacts
     */
    void setMetaContactMembers(Contact groupContact,
                               Set<MetaContact> metaContacts);

    /**
     * Returns a set of all of the IM contacts who are members of this
     * GroupContact.
     *
     * @param groupContact the GroupContact
     * @return the set of IM contact members
     */
    Set<Contact> getIMContactMembers(Contact groupContact);

    /**
     * Returns a set of all of the MetaContacts who are members of this
     * GroupContact.
     *
     * @param groupContact the GroupContact
     * @return the set of MetaContact members
     */
    Set<MetaContact> getMetaContactMembers(Contact groupContact);

    /**
     * Returns true if this group contact has at least one member who is
     * IM-capable, false otherwise.
     *
     * @param groupContact the GroupContact
     * @return whether this GroupContact has IM-capable members.
     */
    boolean hasImCapableMembers(Contact groupContact);

    /**
     * Starts a new group chat with the members of the given GroupContact.  The
     * group chat will have the subject '<group_displayname> - <date>'
     *
     * @param groupContact the GroupContact
     */
    void startGroupChat(Contact groupContact);
}
