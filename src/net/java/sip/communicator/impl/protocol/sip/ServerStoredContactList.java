/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.net.URI;
import java.text.*;
import java.util.*;

import javax.sip.address.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * The parent server stored contact list used in
 * <tt>OperationSetPresenceSipImpl</tt> the underling implementation
 * like xcap one extend it with actual contact list modifications.
 *
 * @author Damian Minkov
 */
public abstract class ServerStoredContactList
{
    /**
     * Logger class
     */
    private static final Logger logger =
            Logger.getLogger(ServerStoredContactList.class);

    /**
     * Root group name.
     */
    protected static final String ROOT_GROUP_NAME = "RootGroup";

    /**
     * The provider that is on top of us.
     */
    protected final ProtocolProviderServiceSipImpl sipProvider;

    /**
     * The operation set that created us and that we could use when dispatching
     * subscription events.
     */
    protected final OperationSetPresenceSipImpl parentOperationSet;

    /**
     * Listeners that would receive event notifications for changes in group
     * names or other properties, removal or creation of groups.
     */
    private final Vector<ServerStoredGroupListener> serverStoredGroupListeners;

    /**
     * The root contact group. The container for all SIP contacts and groups.
     */
    protected final ContactGroupSipImpl rootGroup;

    /**
     * Creates a ServerStoredContactList wrapper for the specified BuddyList.
     *
     * @param sipProvider        the provider that has instantiated us.
     * @param parentOperationSet the operation set that created us and that
     *                           we could use for dispatching subscription events
     */
    ServerStoredContactList(
            ProtocolProviderServiceSipImpl sipProvider,
            OperationSetPresenceSipImpl parentOperationSet)
    {
        this.sipProvider = sipProvider;
        this.parentOperationSet = parentOperationSet;
        this.serverStoredGroupListeners =
                new Vector<>();
        this.rootGroup = new ContactGroupSipImpl(ROOT_GROUP_NAME, sipProvider);
    }

    /**
     * Returns the root group of the contact list.
     *
     * @return the root ContactGroup for the ContactList.
     */
    public ContactGroupSipImpl getRootGroup()
    {
        return rootGroup;
    }

    /**
     * Registers the specified group listener so that it would receive events
     * on group modification/creation/destruction.
     *
     * @param listener the ServerStoredGroupListener to register for group
     *                 events.
     */
    public void addGroupListener(ServerStoredGroupListener listener)
    {
        synchronized (serverStoredGroupListeners)
        {
            if (!serverStoredGroupListeners.contains(listener))
            {
                this.serverStoredGroupListeners.add(listener);
            }
        }
    }

    /**
     * Removes the specified group listener so that it won't receive further
     * events on group modification/creation/destruction.
     *
     * @param listener the ServerStoredGroupListener to unregister.
     */
    public void removeGroupListener(ServerStoredGroupListener listener)
    {
        synchronized (serverStoredGroupListeners)
        {
            this.serverStoredGroupListeners.remove(listener);
        }
    }

    /**
     * Creates the corresponding event and notifies all
     * <tt>ServerStoredGroupListener</tt>s that the source group has been
     * removed, changed, renamed or whatever happened to it.
     *
     * @param group   the ContactGroup that has been created/modified/removed.
     * @param eventID the id of the event to generate.
     */
    protected void fireGroupEvent(ContactGroup group, int eventID)
    {
        ServerStoredGroupEvent event = new ServerStoredGroupEvent(
                group,
                eventID,
                parentOperationSet.getServerStoredContactListRoot(),
                sipProvider,
                parentOperationSet);
        logger.trace("Will dispatch the following group event: " + event);
        Iterable<ServerStoredGroupListener> listeners;
        synchronized (serverStoredGroupListeners)
        {
            listeners =
                    new ArrayList<>(
                            serverStoredGroupListeners);
        }
        for (ServerStoredGroupListener listener : listeners)
        {
            if (eventID == ServerStoredGroupEvent.GROUP_REMOVED_EVENT)
            {
                listener.groupRemoved(event);
            }
            else if (eventID == ServerStoredGroupEvent.GROUP_RENAMED_EVENT)
            {
                listener.groupNameChanged(event);
            }
            else if (eventID == ServerStoredGroupEvent.GROUP_CREATED_EVENT)
            {
                listener.groupCreated(event);
            }
            else if (eventID == ServerStoredGroupEvent.GROUP_RESOLVED_EVENT)
            {
                listener.groupResolved(event);
            }
        }
    }

    /**
     * Creates a non resolved contact for the specified address and inside the
     * specified group. The newly created contact would be added to the local
     * contact list as a standard contact but when an event is received from the
     * server concerning this contact, then it will be reused and only its
     * isResolved field would be updated instead of creating the whole contact
     * again. If creation is successful event will be fired.
     *
     * @param parentGroup the group where the unresolved contact is to be
     *                    created.
     * @param contactId   the sip id of the contact to create.
     * @param persistentData a String returned Contact's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     * @return the newly created unresolved <tt>ContactSipImpl</tt>.
     */
    public synchronized ContactSipImpl createUnresolvedContact(
            ContactGroupSipImpl parentGroup, String contactId,
            String persistentData)
    {
        if (parentGroup == null)
        {
            throw new IllegalArgumentException("Parent group cannot be null");
        }
        if (contactId == null || contactId.length() == 0)
        {
            throw new IllegalArgumentException(
                    "Creating contact id name cannot be null or empty");
        }
        Address contactAddress;
        try
        {
            contactAddress = sipProvider.parseAddressString(contactId);
        }
        catch (ParseException ex)
        {
            throw new IllegalArgumentException(
                    String.format("%1s is not a valid SIP identifier",
                            contactId),
                    ex);
        }

        logger.trace("createUnresolvedContact " + contactId);

        ContactSipImpl newUnresolvedContact = new ContactSipImpl(contactAddress,
                sipProvider);
        parentGroup.addContact(newUnresolvedContact);
        newUnresolvedContact.setPersistentData(persistentData);
        fireContactAdded(parentGroup, newUnresolvedContact);
        return newUnresolvedContact;
    }

    /**
     * Creates a non resolved contact group for the specified name. The newly
     * created group would be added to the local contact list as any other group
     * but when an event is received from the server concerning this group, then
     * it will be reused and only its isResolved field would be updated instead
     * of creating the whole group again.
     * <p/>
     *
     * @param parentGroup the group under which the new group is to be created.
     * @param groupName   the name of the group to create.
     * @return the newly created unresolved <tt>ContactGroupSipImpl</tt>.
     */
    public synchronized ContactGroupSipImpl createUnresolvedContactGroup(
            ContactGroupSipImpl parentGroup,
            String groupName)
    {
        if (parentGroup == null)
        {
            throw new IllegalArgumentException("Parent group cannot be null");
        }
        if (groupName == null || groupName.length() == 0)
        {
            throw new IllegalArgumentException(
                    "Creating group name cannot be null or empry");
        }
        logger.trace("createUnresolvedContactGroup " + groupName);
        ContactGroupSipImpl subGroup = new ContactGroupSipImpl(groupName,
                sipProvider);
        subGroup.setResolved(false);
        parentGroup.addSubgroup(subGroup);
        fireGroupEvent(subGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        return subGroup;
    }

        /**
     * Make the parent persistent presence operation set dispatch a contact
     * added event.
     *
     * @param parentGroup the group where the new contact was added.
     * @param contact     the contact that was added.
     */
    protected void fireContactAdded(
            ContactGroupSipImpl parentGroup,
            ContactSipImpl contact)
    {
        // Only need to tell the protocol provider if this contact is not hidden
        if (!contact.isHidden())
        {
            parentOperationSet.fireSubscriptionEvent(
                    contact,
                    parentGroup,
                    SubscriptionEvent.SUBSCRIPTION_CREATED);
        }
    }

    /**
     * Make the parent persistent presence operation set dispatch a subscription
     * moved event.
     *
     * @param oldParentGroup the group where the source contact was located
     *                       before being moved.
     * @param newParentGroup the group that the source contact is currently in.
     * @param contact        the contact that was added.
     */
    protected void fireContactMoved(
            ContactGroupSipImpl oldParentGroup,
            ContactGroupSipImpl newParentGroup,
            ContactSipImpl contact)
    {
        parentOperationSet.fireSubscriptionMovedEvent(
                contact,
                oldParentGroup,
                newParentGroup);
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * removed event.
     *
     * @param parentGroup the group where that the removed contact belonged to.
     * @param contact     the contact that was removed.
     */
    protected void fireContactRemoved(
            ContactGroupSipImpl parentGroup,
            ContactSipImpl contact)
    {
        // Only need to tell the protocol provider if this contact is not hidden
        if (!contact.isHidden())
        {
            parentOperationSet.fireSubscriptionEvent(
                    contact,
                    parentGroup,
                    SubscriptionEvent.SUBSCRIPTION_REMOVED);
        }
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * resolved event.
     *
     * @param parentGroup the group that the resolved contact belongs to.
     * @param contact     the contact that was resolved.
     */
    protected void fireContactResolved(
            ContactGroupSipImpl parentGroup,
            ContactSipImpl contact)
    {
        parentOperationSet.fireSubscriptionEvent(
                contact,
                parentGroup,
                SubscriptionEvent.SUBSCRIPTION_RESOLVED);
    }

    /**
     * Gets all unique contacts from group and all subgroups.
     *
     * @param group the parent of the contacts.
     * @return List of available contacts
     */
    public synchronized List<ContactSipImpl> getUniqueContacts(
            ContactGroupSipImpl group)
    {
        Map<String, ContactSipImpl> uniqueContacts =
                new HashMap<>();
        List<ContactSipImpl> contacts = getAllContacts(group);
        for (ContactSipImpl contact : contacts)
        {
            uniqueContacts.put(contact.getUri(), contact);
        }
        return new ArrayList<>(uniqueContacts.values());
    }

    /**
     * Returns all avaliable contacts from group and all subgroups.
     *
     * @param group the parent of the contacts.
     * @return the list of availcable contacts.
     */
    public synchronized List<ContactSipImpl> getAllContacts(
            ContactGroupSipImpl group)
    {
        List<ContactSipImpl> contacts = new ArrayList<>();
        Iterator<ContactGroup> groupIterator = group.subgroups();
        while (groupIterator.hasNext())
        {
            contacts.addAll(
                    getAllContacts((ContactGroupSipImpl) groupIterator.next()));
        }
        Iterator<Contact> contactIterator = group.contacts();
        while (contactIterator.hasNext())
        {
            ContactSipImpl contact = (ContactSipImpl) contactIterator.next();
            contacts.add(contact);
        }
        return contacts;
    }

    /**
     * Returns all avaliable groups from group and all subgroups.
     *
     * @param group the parent of the contacts.
     * @return the list of availcable groups.
     */
    public synchronized List<ContactGroupSipImpl> getAllGroups(
            ContactGroupSipImpl group)
    {
        List<ContactGroupSipImpl> groups = new ArrayList<>();
        Iterator<ContactGroup> groupIterator = group.subgroups();
        while (groupIterator.hasNext())
        {
            groups.addAll(
                    getAllGroups((ContactGroupSipImpl) groupIterator.next()));
        }
        return groups;
    }

    /**
     * Initializes the server stored list. Synchronize server stored groups and
     * contacts with the local groups and contacts.
     */
    public abstract void init();

    /**
     * Destroys the server stored list.
     */
    public abstract void destroy();

    /**
     * Gets the pres-content image uri.
     *
     * @return the pres-content image uri.
     * @throws IllegalStateException if the user has not been connected.
     */
    public abstract URI getImageUri();

    /**
     * Gets image from the specified uri.
     *
     * @param imageUri the image uri.
     * @return the image.
     */
    public abstract BufferedImageFuture getImage(URI imageUri);

    /**
     * Creates a group with the specified name and parent in the server stored
     * contact list.
     *
     * @param parentGroup the group where the new group should be created.
     * @param groupName   the name of the new group to create.
     * @param persistent  specify whether created contact is persistent ot not.
     * @return the newly created <tt>ContactGroupSipImpl</tt>.
     * @throws OperationFailedException with code NETWORK_FAILURE if creating
     *                                  the group fails because of XCAP server
     *                                  error or with code
     *                                  CONTACT_GROUP_ALREADY_EXISTS if contact
     *                                  group with such name already exists.
     */
    public abstract ContactGroupSipImpl createGroup(
            ContactGroupSipImpl parentGroup, String groupName,
            boolean persistent)
        throws OperationFailedException;

    /**
     * Renames the specified group from the server stored contact list.
     *
     * @param group   the group to rename.
     * @param newName the new name of the group.
     */
    public abstract void renameGroup(
            ContactGroupSipImpl group,
            String newName);

    /**
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     *
     * @param contact        the <tt>Contact</tt> to move
     * @param newParentGroup the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *                       would be placed.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    public abstract void moveContactToGroup(
            ContactSipImpl contact,
            ContactGroupSipImpl newParentGroup)
        throws OperationFailedException;

    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to delete.
     */
    public abstract void removeGroup(ContactGroupSipImpl group);

    /**
     * Creates contact for the specified address and inside the
     * specified group . If creation is successful event will be fired.
     *
     * @param parentGroup the group where the unresolved contact is to be
     *                    created.
     * @param contactId   the sip id of the contact to create.
     * @param persistent  specify whether created contact is persistent or not.
     * @param contactType the contact type to create, if missing null.
     * @param hidden      specify whether the created contact is hidden or not.
     * @return the newly created <tt>ContactSipImpl</tt>.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    public synchronized ContactSipImpl createContact(
        ContactGroupSipImpl parentGroup, String contactId,
        boolean persistent, String contactType, boolean hidden)
        throws OperationFailedException
    {
        return createContact(
            parentGroup, contactId, null, persistent, contactType, hidden);
    }

    /**
     * Creates contact for the specified address and inside the
     * specified group . If creation is successful event will be fired.
     *
     * @param parentGroup the group where the unresolved contact is to be
     *                    created.
     * @param contactId   the sip id of the contact to create.
     * @param displayName the display name of the contact to create
     * @param persistent  specify whether created contact is persistent or not.
     * @param contactType the contact type to create, if missing null.
     * @param hidden      specify whether the created contact is hidden or not.
     * @return the newly created <tt>ContactSipImpl</tt>.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    public abstract ContactSipImpl createContact(
            ContactGroupSipImpl parentGroup,
            String contactId,
            String displayName,
            boolean persistent,
            String contactType,
            boolean hidden)
        throws OperationFailedException;

    /**
     * Removes a contact. If creation is successful event will be fired.
     *
     * @param contact contact to be removed.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    public abstract void removeContact(ContactSipImpl contact)
            throws OperationFailedException;

    /**
     * Renames the specified contact.
     *
     * @param contact the contact to be renamed.
     * @param newName the new contact name.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    public abstract void renameContact(
            ContactSipImpl contact,
            String newName);

    /**
     * The user accepted authorization request for <tt>contact</tt>
     * @param contact the user has accepted.
     */
    public abstract void authorizationAccepted(ContactSipImpl contact);

    /**
     * The user rejected authorization request for <tt>contact</tt>
     * @param contact the user has rejected.
     */
    public abstract void authorizationRejected(ContactSipImpl contact);

    /**
     * The user ignored authorization request for <tt>contact</tt>
     * @param contact the user has ignored.
     */
    public abstract void authorizationIgnored(ContactSipImpl contact);

    /**
     * Whether current contact list supports account image.
     * @return does current contact list supports account image.
     */
    public abstract boolean isAccountImageSupported();

    /**
     * Get current account image from server if any.
     * @return the account image.
     */
    public abstract ImageDetail getAccountImage()
        throws OperationFailedException;

    /**
     * Deletes current account image from server.
     */
    public abstract void deleteAccountImage()
        throws OperationFailedException;

    /**
     * Change the image of the account on server.
     * @param image the new image.
     */
    public abstract void setAccountImage(BufferedImageFuture image)
        throws OperationFailedException;
}
