/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A mock implementation of a persistent presence operation set containing a
 * constant contact list and used for testing the meta contact list.
 * @author Emil Ivov
 */
public class MockPersistentPresenceOperationSet
    extends AbstractOperationSetPersistentPresence<MockProvider>
{
    /**
     * The root of the mock contact list.
     */
    private MockContactGroup contactListRoot = null;

    /**
     * The currently active status message.
     */
    private String statusMessage = "Default Status Message";

    /**
     * Our default presence status.
     */
    private PresenceStatus presenceStatus = MockStatusEnum.MOCK_STATUS_50;

    public MockPersistentPresenceOperationSet(MockProvider provider)
    {
        super(provider);

        contactListRoot = new MockContactGroup("RootMockGroup", provider);
    }

    /**
     * Creates a group with the specified name and parent in the server
     * stored contact list.
     *
     * @param parent the group where the new group should be created
     * @param groupName the name of the new group to create.
     */
    public void createServerStoredContactGroup(ContactGroup parent,
                                               String groupName)
    {
        MockContactGroup newGroup
            = new MockContactGroup(groupName, parentProvider);

        ((MockContactGroup)parent).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    }

    /**
     * A Mock Provider method to use for fast filling of a contact list.
     *
     * @param contactGroup the group to add
     */
    public void addMockGroup(MockContactGroup contactGroup)
    {
        contactListRoot.addSubgroup(contactGroup);
    }

    /**
     * A Mock Provider method to use for fast filling of a contact list. This
     * method would add both the group and fire an event.
     *
     * @param parent the group where <tt>contactGroup</tt> should be added.
     * @param contactGroup the group to add
     */
    public void addMockGroupAndFireEvent(MockContactGroup parent
                                         , MockContactGroup contactGroup)
    {
        parent.addSubgroup(contactGroup);

        this.fireServerStoredGroupEvent(
            contactGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    }

    /**
     * Returns a reference to the contact with the specified ID in case we
     * have a subscription for it and null otherwise/
     *
     * @param contactID a String identifier of the contact which we're
     *   seeking a reference of.
     * @return a reference to the Contact with the specified
     *   <tt>contactID</tt> or null if we don't have a subscription for the
     *   that identifier.
     */
    public Contact findContactByID(String contactID)
    {
        return contactListRoot.findContactByID(contactID);
    }

    /**
     * Sets the specified status message.
     * @param statusMessage a String containing the new status message.
     */
    public void setStatusMessage(String statusMessage)
    {
        this.statusMessage = statusMessage;
    }

    /**
     * Returns the status message that was last set through
     * setCurrentStatusMessage.
     *
     * @return the last status message that we have requested and the aim
     *   server has confirmed.
     */
    public String getCurrentStatusMessage()
    {
        return statusMessage;
    }

    /**
     * Returns the protocol specific contact instance representing the local
     * user.
     *
     * @return the Contact (address, phone number, or uin) that the Provider
     *   implementation is communicating on behalf of.
     */
    public Contact getLocalContact()
    {
        return null;
    }

    /**
     * Returns a PresenceStatus instance representing the state this provider
     * is currently in.
     *
     * @return the PresenceStatus last published by this provider.
     */
    public PresenceStatus getPresenceStatus()
    {
        return presenceStatus;
    }

    /**
     * Returns the root group of the server stored contact list.
     *
     * @return the root ContactGroup for the ContactList stored by this
     *   service.
     */
    public ContactGroup getServerStoredContactListRoot()
    {
        return contactListRoot;
    }

    /**
     * Returns the set of PresenceStatus objects that a user of this service
     * may request the provider to enter.
     *
     * @return Iterator a PresenceStatus array containing "enterable" status
     *   instances.
     */
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        return MockStatusEnum.supportedStatusSet();
    }

    /**
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     *
     * @param contactToMove the <tt>Contact</tt> to move
     * @param newParent the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *   would be placed.
     */
    public void moveContactToGroup(Contact contactToMove,
                                   ContactGroup newParent)
    {
        MockContact mockContact = (MockContact)contactToMove;

        MockContactGroup parentMockGroup = findContactParent(mockContact);

        parentMockGroup.removeContact(mockContact);

        ((MockContactGroup)newParent).addContact(mockContact);

        /** @todo fire an event (we probably need to create a new family of
         * move events) */
    }

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified paramters.
     *
     * @param status the PresenceStatus as returned by
     *   getRequestableStatusSet
     * @param statusMessage the message that should be set as the reason to
     *   enter that status
     * @throws IllegalArgumentException if the status requested is not a
     *   valid PresenceStatus supported by this provider.
     * @throws IllegalStateException if the provider is not currently
     *   registered.
     */
    public void publishPresenceStatus(PresenceStatus status,
                                      String statusMessage) throws
        IllegalArgumentException, IllegalStateException
    {
        PresenceStatus oldPresenceStatus = this.presenceStatus;
        this.presenceStatus = status;
        this.statusMessage = statusMessage;

        this.fireProviderStatusChangeEvent(oldPresenceStatus);
    }

    /**
     * Get the PresenceStatus for a particular contact.
     *
     * @param contactIdentifier the identifier of the contact whose status
     *   we're interested in.
     * @return PresenceStatus the <tt>PresenceStatus</tt> of the specified
     *   <tt>contact</tt>
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *   known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is
     *   not registered/signed on a public service.
     */
    public PresenceStatus queryContactStatus(String contactIdentifier) throws
        IllegalArgumentException, IllegalStateException
    {
        return findContactByID(contactIdentifier).getPresenceStatus();
    }

    /**
     * Sets the presence status of <tt>contact</tt> to <tt>newStatus</tt>.
     * @param contact the <tt>MockContact</tt> whose status we'd like to set.
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    public void changePresenceStatusForContact(MockContact contact
                                               , MockStatusEnum newStatus)
    {
        PresenceStatus oldStatus = contact.getPresenceStatus();
        contact.setPresenceStatus(newStatus);

        fireContactPresenceStatusChangeEvent(
                contact, findContactParent(contact), oldStatus);
    }

    /**
     * Returns the group that is parent of the specified mockGroup  or null
     * if no parent was found.
     * @param mockGroup the group whose parent we're looking for.
     * @return the MockContactGroup instance that mockGroup belongs to or null
     * if no parent was found.
     */
    public MockContactGroup findGroupParent(MockContactGroup mockGroup)
    {
        return contactListRoot.findGroupParent(mockGroup);
    }

    /**
     * Returns the group that is parent of the specified mockContact  or null
     * if no parent was found.
     * @param mockContact the contact whose parent we're looking for.
     * @return the MockContactGroup instance that mockContact belongs to or null
     * if no parent was found.
     */
    public MockContactGroup findContactParent(MockContact mockContact)
    {
        return (MockContactGroup)mockContact.getParentContactGroup();
    }

    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to remove.
     *
     * @throws IllegalArgumentException if <tt>group</tt> was not found in this
     * protocol's contact list.
     */
    public void removeServerStoredContactGroup(ContactGroup group)
        throws IllegalArgumentException
    {
        MockContactGroup mockGroup = (MockContactGroup)group;

        MockContactGroup parent = findGroupParent(mockGroup);

        if(parent == null){
            throw new IllegalArgumentException(
                "group " + group
                + " does not seem to belong to this protocol's contact list.");
        }

        parent.removeSubGroup(mockGroup);

        this.fireServerStoredGroupEvent(
            mockGroup, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
    }

    /**
     * Renames the specified group from the server stored contact list.
     *
     * @param group the group to rename.
     * @param newName the new name of the group.
     */
    public void renameServerStoredContactGroup(ContactGroup group,
                                               String newName)
    {
        ((MockContactGroup)group).setGroupName(newName);

        this.fireServerStoredGroupEvent(
            (MockContactGroup)group, ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
    }

    /**
     * Handler for incoming authorization requests.
     *
     * @param handler an instance of an AuthorizationHandler for
     *   authorization requests coming from other users requesting
     *   permission add us to their contact list.
     */
    public void setAuthorizationHandler(AuthorizationHandler handler)
    {
        /** @todo implement setAuthorizationHandler() */
    }

    /**
     * Persistently adds a subscription for the presence status of the
     * contact corresponding to the specified contactIdentifier and indicates
     * that it should be added to the specified group of the server stored
     * contact list.
     *
     * @param parent the parent group of the server stored contact list
     *   where the contact should be added. <p>
     * @param contactIdentifier the contact whose status updates we are
     *   subscribing for.
     * @throws IllegalArgumentException if <tt>contact</tt> or
     *   <tt>parent</tt> are not a contact known to the underlying protocol
     *   provider.
     * @throws IllegalStateException if the underlying protocol provider is
     *   not registered/signed on a public service.
     */
    public void subscribe(ContactGroup parent, String contactIdentifier) throws
        IllegalArgumentException, IllegalStateException
    {
        MockContact contact = new MockContact(contactIdentifier
                                              , parentProvider);

        ((MockContactGroup)parent).addContact(contact);

        fireSubscriptionEvent(contact,
                              parent,
                              SubscriptionEvent.SUBSCRIPTION_CREATED);
    }

    /**
     * Adds a subscription for the presence status of the contact
     * corresponding to the specified contactIdentifier.
     *
     * @param contactIdentifier the identifier of the contact whose status
     *   updates we are subscribing for. <p>
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *   known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is
     *   not registered/signed on a public service.
     */
    public void subscribe(String contactIdentifier) throws
        IllegalArgumentException, IllegalStateException
    {
        MockContact contact = new MockContact(contactIdentifier
                                              , parentProvider);

        contactListRoot.addContact(contact);

        fireSubscriptionEvent(contact,
                              contactListRoot,
                              SubscriptionEvent.SUBSCRIPTION_CREATED);
    }

    /**
     * Removes a subscription for the presence status of the specified
     * contact.
     *
     * @param contact the contact whose status updates we are unsubscribing
     *   from.
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *   known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is
     *   not registered/signed on a public service.
     */
    public void unsubscribe(Contact contact) throws IllegalArgumentException,
        IllegalStateException
    {
        MockContactGroup parentGroup = (MockContactGroup)((MockContact)contact)
            .getParentContactGroup();

        parentGroup.removeContact((MockContact)contact);

        fireSubscriptionEvent((MockContact)contact,
                                       ((MockContact)contact).getParentContactGroup(),
                                       SubscriptionEvent.SUBSCRIPTION_REMOVED);
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created Contact
     * against the server. The protocol provider may will later try and resolve
     * the contact. When this happens the corresponding event would notify
     * interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     * @return the unresolved <tt>Contact</tt> created from the specified
     * <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData)
    {
        MockContact contact = new MockContact(address, parentProvider);
        contact.setResolved(false);

        ( (MockContactGroup) getServerStoredContactListRoot())
            .addContact(contact);

        fireSubscriptionEvent(contact,
                              getServerStoredContactListRoot(),
                              SubscriptionEvent.SUBSCRIPTION_CREATED);

        return contact;
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created Contact
     * against the server. The protocol provider may will later try and resolve
     * the contact. When this happens the corresponding event would notify
     * interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     * @param parent the group where the unresolved contact is
     * supposed to belong to.
     * @return the unresolved <tt>Contact</tt> created from the specified
     * <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData,
                                           ContactGroup parent)
    {
        MockContact contact = new MockContact(address, parentProvider);
        contact.setResolved(false);

        ( (MockContactGroup) parent).addContact(contact);

        fireSubscriptionEvent(contact,
                              parent,
                              SubscriptionEvent.SUBSCRIPTION_CREATED);

        return contact;
    }

    /**
     * Creates and returns a unresolved contact group from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created
     * <tt>ContactGroup</tt> against the server or the contact itself. The
     * protocol provider will later resolve the contact group. When this happens
     * the corresponding event would notify interested subscription listeners.
     *
     * @param groupUID an identifier, returned by ContactGroup's getGroupUID,
     * that the protocol provider may use in order to create the group.
     * @param persistentData a String returned ContactGroups's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     * @param parentGroup the group under which the new group is to be created
     * or null if this is group directly underneath the root.
     * @return the unresolved <tt>ContactGroup</tt> created from the specified
     * <tt>uid</tt> and <tt>persistentData</tt>
     */
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        MockContactGroup newGroup
            = new MockContactGroup(MockContactGroup.createNameFromUID(groupUID)
                                   , parentProvider);
        newGroup.setResolved(false);

        //if parent is null then we're adding under root.
        if(parentGroup == null)
            parentGroup = getServerStoredContactListRoot();

        ((MockContactGroup)parentGroup).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        return newGroup;
    }
}
