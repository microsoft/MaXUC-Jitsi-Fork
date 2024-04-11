// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.insights.parameters.AddressBookParameterInfo;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;

/**
 * A class for getting and parsing Outlook and Mac Address Book Contacts
 * <p/>
 * Implements the various operation sets required for contacts
 * <p/>
 * Extends the Abstract Operation Set to make contacts easier
 */
public abstract class AbstractAddressBookDataHandler
    extends AbstractOperationSetPersistentPresence<AbstractAddressBookProtocolProviderService>
    implements OperationSetServerStoredContactInfo,
               OperationSetServerStoredUpdatableContactInfo
{
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
    * Whether the initial address book query has completed
    */
    protected boolean queryCompleted = false;

    /**
     * Creates a new data handler for the given parent protocol provider service
     *
     * @param parentProvider
     */
    protected AbstractAddressBookDataHandler(
        AbstractAddressBookProtocolProviderService parentProvider)
    {
        super(parentProvider);
    }

    /**
     * Initializes this data handler by loading any native libraries required
     * @throws OperationFailedException
     */
    public abstract void init() throws OperationFailedException;

    /**
     * Uninitializes this data handler by unloading any native libraries required
     */
    public abstract void uninitialize();

    public void subscribe(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException
    {
        parentProvider.createContact(contactIdentifier);
    }

    public void subscribe(ContactGroup group, String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException
    {
        parentProvider.createContact(contactIdentifier);
    }

    public void unsubscribe(Contact contact)
        throws IllegalArgumentException,
               IllegalStateException
    {
        // No need to hash Contact, as its toString() method does that.
        contactLogger.note(contact, "unsubscribe");
        parentProvider.deleteContact(contact, true);
    }

    public void createServerStoredContactGroup(ContactGroup parent,
        String groupName)
    {
        // Nothing to do
    }

    public void removeServerStoredContactGroup(ContactGroup group)
    {
        // Nothing to do
    }

    public void renameServerStoredContactGroup(ContactGroup group,
        String newName)
    {
        // Nothing to do
    }

    public void moveContactToGroup(Contact contactToMove,
        ContactGroup newParent)
    {
        // Nothing to do
    }

    public ContactGroup getServerStoredContactListRoot()
    {
        return parentProvider.getGroup();
    }

    public Contact createUnresolvedContact(String address,
                                           String persistentData,
                                           ContactGroup parentGroup)
    {
        return parentProvider.createUnresolvedContact(address,
                                                      persistentData);
    }

    public ContactGroup createUnresolvedContactGroup(String groupUID,
                                                     String persistentData,
                                                     ContactGroup parentGroup)
    {
        // Nothing to do
        return null;
    }

    public PresenceStatus getPresenceStatus()
    {
        // Nothing to do
        return null;
    }

    public void publishPresenceStatus(PresenceStatus status,
                                      String statusMessage)
        throws IllegalArgumentException,
               IllegalStateException
    {
        // Nothing to do
    }

    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        // Nothing to do
        return null;
    }

    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException
    {
        // Nothing to do
        return null;
    }

    public Contact findContactByID(String contactID)
    {
        return parentProvider.contactMap.get(contactID);
    }

    public void setAuthorizationHandler(AuthorizationHandler handler)
    {
        // Nothing to do
    }

    public String getCurrentStatusMessage()
    {
        // Nothing to do
        return null;
    }

    public boolean isAddressDisplayable()
    {
        return false;
    }

    public Contact createUnresolvedContact(String address,
        String persistentData)
    {
        return parentProvider.createUnresolvedContact(address,
                                                      persistentData);
    }

    public void setDetailsForContact(Contact contact,
                                     ArrayList<GenericDetail> details,
                                     boolean isCreateOp,
                                     ContactUpdateResultListener listener)
    {
        long startTimestamp = System.currentTimeMillis();
        parentProvider.setContactDetails(contact, details, listener);
        long updateDuration = System.currentTimeMillis() - startTimestamp;

        String countParam = isCreateOp ?
                AddressBookParameterInfo.INSERT_COUNT.name() :
                AddressBookParameterInfo.UPDATE_COUNT.name();
        AddressBookProtocolActivator.getInsightsService().logEvent(
                InsightsEventHint.CONTACT_SYNC_TO_NATIVE.name(),
                Map.of(
                        AddressBookParameterInfo.PARAM_DURATION_MS.name(), updateDuration,
                        countParam, 1
                )
        );
    }

    //-------------------------------------------------------------------------
    // METHODS for OperationSetServerStoredContactInfo
    //-------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public <T extends GenericDetail> Iterator<T> getDetailsAndDescendants(
                                          Contact contact, Class<T> detailClass)
    {
        Iterator<GenericDetail> details = getDetails(contact, null);
        List<T> result = new ArrayList<>();

        while (details.hasNext())
        {
            GenericDetail detail = details.next();

            if (detailClass.isAssignableFrom(detail.getClass()))
            {
                result.add((T)detail);
            }
        }

        return result.iterator();
    }

    public Iterator<GenericDetail> getDetails(Contact sourceContact,
                                     Class<? extends GenericDetail> detailClass)
    {
        AbstractAddressBookContact contact =
                                     (AbstractAddressBookContact) sourceContact;

        ArrayList<GenericDetail> details = (contact == null) ?
                new ArrayList<>() : contact.getDetails(detailClass);

        return details.iterator();
    }

    public Iterator<GenericDetail> getAllDetailsForContact(Contact contact)
    {
        return getDetails(contact, null);
    }

    public Iterator<GenericDetail> requestAllDetailsForContact(Contact contact,
        DetailsResponseListener listener)
    {
        // As the details are cached - request details is exactly the same as
        // get details
        return getDetails(contact, null);
    }

    /**
     * Gets all the contacts from the contact source
     *
     * @return all the contacts from the data source
     */
    public Iterator<Contact> getAllContacts()
    {
        return parentProvider.getAllContacts();
    }

    /**
     * Gets the number of contacts that this data handler knows about
     *
     * @return the number of contacts that this data handler knows about
     */
    public int numberContacts()
    {
        return parentProvider.contactMap.size();
    }

    /**
     * Defines a notification: a combination of a contact identifier and a
     * function.
     */
    protected class NotificationIdFunction
    {
        /**
         * The contact identifier.
         * On Mac this is(/appears to be) a pointer to an ABRecord object in the native code, not an actual ID.
         */
        private String id;

        /**
         * The kind of notification: 'd' for deleted, 'u' for updated and 'i'
         * for inserted.
         */
        private char function;

        /**
         * Creates a new notification.
         *
         * @param id The contact id.
         * @param function The kind of notification: 'd' for deleted, 'u' for
         * updated and 'i' for inserted.
         */
        public NotificationIdFunction(String id, char function)
        {
            this.id = id;
            this.function = function;
        }

        /**
         * Returns the contact identifier.
         * On Mac this is(/appears to be) a pointer to an ABRecord object in the native code, not an actual ID.
         * @return The contact identifier.
         */
        public String getId()
        {
            return this.id;
        }

        /**
         * Returns the kind of notification.
         *
         * @return 'd' for deleted, 'u' for updated and 'i' for inserted.
         */
        public char getFunction()
        {
            return this.function;
        }

        /**
         * Returns if this notification is about the same contact as the one
         * given in parameter.
         *
         * @param obj An NotificationIdFunction to compare with.
         *
         * @return True if this notification is about the same contact has the
         * one given in parameter. False otherwise.
         */
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;

            if (!(obj instanceof NotificationIdFunction))
                return false;

            NotificationIdFunction nifObj = (NotificationIdFunction) obj;

            if (nifObj.id == null)
                return this.id == null;

            return nifObj.id.equals(this.id);
        }

        /**
         * Returns the hash code corresponding to the contact identifier.
         *
         * @return The hash code corresponding to the contact identifier.
         */
        public int hashCode()
        {
            return this.id.hashCode();
        }

        @Override
        public String toString()
        {
            return "NotificationFunction " + function + ", " + id + ", " + hashCode();
        }
    }

    /**
     * Marks the contact with the given ID as a favourite in the contact source
     *
     * @param id the ID of the contact to set
     * @param isFavourite whether to set as a favourite
     */
    protected abstract void setContactAsFavourite(String id, boolean isFavourite);

    /**
     * Fires an event to trigger displaying the 'synchronizing contacts'
     * bar.
     */
    protected void fireContactSyncEvent()
    {
       ContactSyncBarService contactSyncBarService =
                        AddressBookProtocolActivator.getContactSyncBarService();

        if (contactSyncBarService != null)
        {
            contactSyncBarService.fireContactEvent();
        }
    }

    /**
     * Whether the initial contact query has completed
     *
     * @return whether the initial contact query has completed
     */
    public boolean isQueryComplete()
    {
        return queryCompleted;
    }

    @Override
    public void finishedLoadingUnresolvedContacts()
    {
        // Just tell the parent provider that we've finished loading the
        // unresolved contacts from the MclStorageManager.
        contactLogger.info("Finished loading unresolved contacts");
        parentProvider.onAllUnresovledContactsLoaded();
    }

    /**
     * Append the state of this data handler onto the end of the buffer that is
     * passed in
     *
     * @param state The string buidl onto which to add the state
     */
    public abstract void appendState(StringBuilder state);
}
