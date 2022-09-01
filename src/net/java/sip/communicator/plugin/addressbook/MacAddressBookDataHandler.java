// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;
import java.util.Map.*;

import org.jitsi.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.PersonalContactDetails.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;

/**
 * Data Handler for the Mac Address Book. This handles all communication with
 * the native Mac Address Book code. It receives and sends notifications for
 * contacts being created, edited and deleted.
 */
public class MacAddressBookDataHandler
    extends AbstractAddressBookDataHandler
    implements OperationSetServerStoredContactInfo,
               OperationSetServerStoredUpdatableContactInfo
{
    /**
     * The logger for this class
     */
    private static final Logger logger
        = Logger.getLogger(MacAddressBookDataHandler.class);

    /**
     * The lock object used to synchronize the notification thread.
     */
    private final Object notificationThreadLock = new Object();

    /**
     * The library loader
     */
    static
    {
        logger.debug("Loading the native library");
        System.loadLibrary("jmacosxaddrbook");
    }

    /**
     * A pointer to the native <tt>MacAddressBookDataHandler</tt> instance
     */
    private long macAddressBookPointer;

    /**
     * The thread used to collect the notifications.
     */
    private NotificationThread notificationThread;

    /**
     * The time in ms since the last notification we received from the COM
     * server
     */
    private long lastQueryResultTime;

    /**
     * A map of Accession IDs to Mac Address book IDs. Note that the key and
     * value will only differ for contacts created in Accession.
     */
    private final HashMap<String,String> idMap = new HashMap<>();

    /**
     * A lock object to synchronize access to the ID Map
     */
    private final Object idMapLock = new Object();

    /**
     * Whether we have marked all necessary contacts as resolved and purged
     * all unresolved contacts
     */
    private boolean allContactsResolved = false;

    /**
     * Constructor for this data handler
     *
     * @param parentProvider the protocol provider service that created us
     */
    protected MacAddressBookDataHandler(
        AbstractAddressBookProtocolProviderService parentProvider)
    {
        super(parentProvider);

        logger.debug("MacAddressBookDataHandler started");

        // Add an operation set for presence
        parentProvider.addSupportedOperationSet(
                                           OperationSetPersistentPresence.class,
                                           this);

        // Add an operation set for contact details
        parentProvider.addSupportedOperationSet(
                                      OperationSetServerStoredContactInfo.class,
                                      this);

        // And finally, an operation set for editing the contact details.
        parentProvider.addSupportedOperationSet(
                             OperationSetServerStoredUpdatableContactInfo.class,
                             this);
    }

    @Override
    public void init()
    {
        logger.info("Initialising the Mac Address Book native library");

        macAddressBookPointer = MacAddressBookUtils.start();
        if (0 == macAddressBookPointer)
            throw new IllegalStateException("pointer was zero");
        MacAddressBookUtils.setDelegate(macAddressBookPointer,
                                        new NotificationsDelegate());

        // Start the query to retrieve all contacts
        queryMacAddressBook();
    }

    @Override
    public void uninitialize()
    {
        // Stop the native library from running if necessary
        if (0 != macAddressBookPointer)
        {
            MacAddressBookUtils.stop(macAddressBookPointer);
            macAddressBookPointer = 0;
        }
    }

    /**
     * Delegate class to be notified for addressbook changes.
     */
    public class NotificationsDelegate
    {
        /**
         * Callback method when receiving notifications for inserted items.
         *
         * @param id The id of the contact in Mac Address Book
         */
        public void inserted(long id)
        {
            addNotification(String.valueOf(id), 'i');
        }

        /**
         * Callback method when receiving notifications for updated items.
         *
         * @param id The id of the contact in Mac Address Book
         */
        public void updated(long id)
        {
            addNotification(String.valueOf(id), 'u');
        }

        /**
         * Callback method when receiving notifications for deleted items.
         *
         * @param id The id of the contact in Mac Address Book
         */
        public void deleted(String id)
        {
            addNotification(id, 'd');
        }
    }

    /**
     * Thread used to collect the notification.
     */
    private class NotificationThread
        extends Thread
    {
        /**
         * The list of notification collected.
         */
        private Vector<NotificationIdFunction> contactIds
            = new Vector<>();

        /**
         * Whether the notification thread should start processing events
         */
        private boolean processEvents;

        /**
         * The object to lock on for processing Outlook events
         */
        private final Object processLock = new Object();

        /**
         * Initializes a new notification thread.
         */
        public NotificationThread()
        {
            super("MacAddressBookDataHandler notification thread");
            logger.debug("NotificationThread created");
        }

        /**
         * Dispatches the collected notifications.
         */
        public void run()
        {
            boolean hasMore = false;

            // Do not start processing events unless we have not received a
            // notification from Mac Address Book for over 500ms. This
            // prevents us overloading the native library and missing contacts
            synchronized (processLock)
            {
                do
                {
                    try
                    {
                        // Set processEvents to true so if the timer pops
                        // before being notified we exit the loop
                        processEvents = true;
                        processLock.wait(500);
                    }
                    catch (InterruptedException ex)
                    {
                        logger.error("Interrupted notification thread, returning");
                        return;
                    }
                }
                while (!processEvents);
            }

            // While there are more contacts to process...
            do
            {
                synchronized(notificationThreadLock)
                {
                    hasMore = (contactIds.size() > 0);
                    if (hasMore)
                    {
                        NotificationIdFunction idFunction = contactIds.remove(0);
                        char function = idFunction.getFunction();
                        String idPtrString = idFunction.getId();
                        logger.debug("Processing notification function " + function + " for ptr " + idPtrString);

                        if (function == 'd')
                        {
                            contactDeleted(idPtrString);
                        }
                        else if (function == 'u' || function == 'i')
                        {
                            long idPtr = Long.parseLong(idPtrString);
                            String id = MacAddressBookUtils.ABRecord_uniqueId(idPtr);

                            // Get the properties for this contact from Mac address book
                            Object[] values =
                                    MacAddressBookUtils.ABRecord_valuesForProperties(
                                            idPtr,
                                            MacAddressBookUtils.ABPERSON_PROPERTIES);

                            contactUpdated(id, values);
                        }
                    }
                }
            } while(hasMore);

            if (queryCompleted && !allContactsResolved)
            {
                // Set all contacts resolved to true so we do not process any
                // unresolved contacts again
                logger.info("Retrieved all Mac Address Book contacts, removing" +
                            "any unresolved contacts");
                allContactsResolved = true;

                parentProvider.purgeUnresolvedContacts();
            }
        }

        /**
         * Returns a string representation of the array of objects supplied.
         * The array may be multi-layered.
         *
         * @param values the array of objects to convert to a string
         * @return a string representation of the array of objects supplied
         */
        private String getValuesAsString(Object[] values)
        {
            String strVals = "";
            for (Object value : values)
            {
                if (value instanceof String)
                {
                    strVals = strVals + ", " + (String) value;
                }
                else if (value instanceof Object[])
                {
                    strVals = strVals + getValuesAsString((Object[]) value);
                }
            }
            return strVals;
        }

        /**
         * Adds a new notification. Avoids previous notification for the given
         * contact.
         *
         * @param idPtrString The contact id.
         * @param function The kind of notification: 'd' for deleted, 'u' for
         * updated and 'i' for inserted.
         */
        public void add(String idPtrString, char function)
        {
            logger.debug("Add notification function " + function + " for ptr " + idPtrString);
            synchronized(notificationThreadLock)
            {
                NotificationIdFunction idFunction
                                     = new NotificationIdFunction(idPtrString, function);

                synchronized (processLock)
                {
                    // Alert the notification thread that it should not start
                    // processing the events because we have just received
                    // another
                    notificationThread.processEvents = false;
                    processLock.notify();
                }

                // NotificationIdFunctions are considered the same if they are
                // for the same contact.  Thus remove any outstanding functions
                // as they replaced by this one.
                contactIds.remove(idFunction);
                contactIds.add(idFunction);
            }
        }
    }

    /**
     * Collects a new notification and adds it to the notification thread.
     *
     * @param id The contact id.
     * @param function The kind of notification: 'd' for deleted, 'u' for
     * updated and 'i' for inserted.
     */
    public void addNotification(String id, char function)
    {
        fireContactSyncEvent();

        synchronized(notificationThreadLock)
        {
            if(notificationThread == null
                    || !notificationThread.isAlive())
            {
                notificationThread = new NotificationThread();
                notificationThread.add(id, function);
                notificationThread.start();
            }
            else
            {
                notificationThread.add(id, function);
            }
        }
    }

    /**
     * Runs after the native code has initialized to sync with Mac address
     * book on startup or when switching contact sources to Mac address book.
     */
    private void queryMacAddressBook()
    {
        logger.info("Querying Mac address book for all contacts");
        fireContactSyncEvent();

        MacAddressBookUtils.foreachPerson("",
            new PtrCallback()
            {
                public boolean callback(long id)
                {
                    // Add a notification to the notification thread so it
                    // is dealt with by the Notification Thread
                    addNotification(String.valueOf(id), 'i');
                    return true;
                }

                /**
                 * The callback method that is called from the native Mac code
                 * when the initial query completes
                 */
                public void queryCompleted()
                {
                    // If the notification thread has already completed then
                    // we must call purge now, otherwise the notification
                    // thread will get to it later
                    queryCompleted = true;
                    if(notificationThread == null
                        || !notificationThread.isAlive())
                    {
                        // Set all contacts resolved to true so we do not process any
                        // unresolved contacts again
                        logger.info("Retrieved all Mac Address Book contacts," +
                                    "removing any unresolved contacts");
                        allContactsResolved = true;

                        parentProvider.purgeUnresolvedContacts();
                    }
                }
            });

        // The above code does not block until all contact are discovered.
        // The code below makes sure that the query keeps running while new
        // contacts are still arriving.  Once new contact stop arriving, we
        // can be sure that the query is complete.
        while (lastQueryResultTime + 400 > System.currentTimeMillis())
        {
            try
            {
                MacAddressBookDataHandler.class.wait(400);
            }
            catch (InterruptedException e)
            {
                // Nothing required - keep waiting
            }
        }
    }

    /**
     * Called on an updated or inserted event from Mac Address Book
     *
     * @param id the Address Book ID of this contact
     * @param values the list of properties for this contact.
     */
    private void contactUpdated(String id, Object[] values)
    {
        synchronized (idMapLock)
        {
            // Determine if we have seen this id before
            if (!idMap.containsValue(id))
            {
                logger.debug("Got Updated event for new contact " + id);

                // This is new ID so add it to the set of IDs
                idMap.put(id, id);
            }
        }

        fireContactUpdated(values, getAccessionID(id));
    }

    /**
     * Finds the Accession ID from the Mac Address Book ID
     *
     * @param id the Mac Address Book ID for which to find the Accession ID
     * @return the Accession ID
     */
    private String getAccessionID(String id)
    {
        String accessionID = null;

        synchronized (idMapLock)
        {
            // Find the accession key from the map of contacts
            for (Entry<String, String> contactEntry : idMap.entrySet())
            {
                if (contactEntry == null || contactEntry.getValue() == null)
                {
                    logger.warn("Contact id is null, which is " +
                                        "unexpected and so indicates an " +
                                        "error/broken contact. See SFR536377.");
                }
                else if (contactEntry.getValue().equals(id))
                {
                    accessionID = contactEntry.getKey();
                    break;
                }
            }
        }

        return accessionID;
    }

    /**
     * Called on a deleted event.
     *
     * @param id the Mac Address Book ID of this contact
     */
    private void contactDeleted(String id)
    {
        String accessionID;

        synchronized (idMapLock)
        {
            // Check if we have seen this contact before
            if (!idMap.containsValue(id))
            {
                // If there is no id saved then we don't know about this contact
                // in Accession so there is nothing to do.
                logger.debug("Got a deleted event for an unknown contact " + id);
                return;
            }

            accessionID = getAccessionID(id);
            logger.debug("Got a deleted event for contact " + id);

            idMap.remove(accessionID);
        }

        fireContactDeleted(accessionID);
    }

    /**
     * Notifies the Protocol Provider Service that a contact has been updated.
     *
     * @param values The list of properties for this contact
     * @param id the ID for this contact
     */
    private void fireContactUpdated(Object[] values,
                                    String id)
    {
        logger.debug("Notifying " + parentProvider + " of contact update " + id);

        ((ProtocolProviderServiceMacAddressBookImpl) parentProvider).
                                                     contactUpdated(values, id);
    }

    /**
     * Notifies the Protocol Provider Service that a contact has been deleted
     *
     * @param id the ID for this contact
     */
    private void fireContactDeleted(String id)
    {
        logger.debug("Notifying " + parentProvider + " of contact deletion " + id);

        ((ProtocolProviderServiceMacAddressBookImpl) parentProvider).
                                                        contactDeleted(id);
    }

    /**
     * Sets the details for the contact referenced by the given id. Called
     * when a contact is created or updated in Accession.
     *
     * @param accessionId the ID for the contact to update
     * @param contact the contact to set details for
     * @return whether the operation succeeded
     */
    public boolean setDetails(String accessionId, Contact contact)
    {
        String id;
        if (!(contact instanceof AddressBookContactMacImpl))
        {
            logger.error("Told to update a Mac address book contact that is not a Mac address book contact");
        }

        List<GenericDetail> contactDetails =
                         ((AbstractAddressBookContact) contact).getDetails(null);
        List<GenericDetail> serverDetails =
                       ((AddressBookContactMacImpl) contact).getServerDetails();

        synchronized (idMapLock)
        {
            // Create a new contact if we don't already have an entry for this id
            if (!idMap.containsKey(accessionId))
            {
                id = MacAddressBookUtils.createContact();

                if (id != null)
                {
                    logger.debug("Set details for new contact " + id);
                    idMap.put(accessionId, id);
                }
            }
            else
            {
                id = idMap.get(accessionId);
            }
        }

        if (id == null)
        {
            logger.warn("Asked to set details for contact but failed as id was null");
            return false;
        }

        // Set up lists for the multi-line values as we need to turn these
        // into stream objects later
        List<ServerStoredDetails.EmailAddressDetail> emailAddresses =
                new ArrayList<>();
        List<PersonalContactDetails.IMDetail> imAddresses =
                new ArrayList<>();
        List<ServerStoredDetails.PhoneNumberDetail> phoneNumbers =
                new ArrayList<>();
        List<GenericDetail> addressDetails =
                new ArrayList<>();

        // Go through each detail for this contact and write to the address book
        for (GenericDetail detail : contactDetails)
        {
            // Only write single line values here, we will deal with all
            // multi-line values later
            if (!MacAddressBookUtils.isMultiLineDetail(detail.getClass()))
            {
                long property = MacAddressBookUtils.
                    getPropertyforDetail(detail);

                if (property != 0)
                {
                    String value = (String) ((detail.getDetailValue() == null) ?
                                   "" : detail.getDetailValue());

                    // We have a contact detail and a property id to
                    // write, so send the request to the native code
                    MacAddressBookUtils.setProperty(
                        id,
                        property,
                        null,
                        value);
                }
            }
            else if (detail instanceof ServerStoredDetails.EmailAddressDetail &&
                     detail.getDetailValue() != null)
            {
                emailAddresses.add((EmailAddressDetail) detail);
            }
            else if (detail instanceof PersonalContactDetails.IMDetail &&
                     detail.getDetailValue() != null)
            {
                imAddresses.add((IMDetail) detail);
            }
            else if (detail instanceof ServerStoredDetails.PhoneNumberDetail &&
                     detail.getDetailValue() != null)
            {
                phoneNumbers.add((ServerStoredDetails.PhoneNumberDetail) detail);
            }
            else if (isAddressType(detail) &&
                     detail.getDetailValue() != null)
            {
                addressDetails.add(detail);
            }
        }

        // We now have to go through all the server stored details that we do
        // not expose in Accession as we can only write back to Mac address
        // book as a stream object for all values of a given property
        for (GenericDetail detail : serverDetails)
        {
            if (detail instanceof ServerStoredDetails.EmailAddressDetail &&
                detail.getDetailValue() != null)
            {
                emailAddresses.add((EmailAddressDetail) detail);
            }
            else if (detail instanceof PersonalContactDetails.IMDetail &&
                     detail.getDetailValue() != null)
            {
                imAddresses.add((IMDetail) detail);
            }
            else if (detail instanceof ServerStoredDetails.PhoneNumberDetail &&
                     detail.getDetailValue() != null)
            {
                phoneNumbers.add((ServerStoredDetails.PhoneNumberDetail) detail);
            }
            else if (isAddressType(detail) &&
                     detail.getDetailValue() != null)
            {
                addressDetails.add((ServerStoredDetails.AddressDetail) detail);
            }
        }

        // Create the email object mutli line stream
        MacAddressBookUtils.writeEmailStream(id, emailAddresses);
        MacAddressBookUtils.writeIMStream(id, imAddresses);
        MacAddressBookUtils.writePhoneNumberStream(id, phoneNumbers);
        MacAddressBookUtils.writeAddressDetailsStream(id, addressDetails);

        return true;
    }

    /**
     * Return whether this detail is an address detail
     *
     * @param detail the detail to check
     * @return whether this detail is an address detail
     */
    private boolean isAddressType(GenericDetail detail)
    {
        for (Class<? extends GenericDetail> addressClass : MacAddressBookUtils.addressDetails)
        {
            if (addressClass.isAssignableFrom(detail.getClass()))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Deletes the given contact in Mac Address Book
     *
     * @param accessionId the id for this contact
     * @return Whether the deletion was successful
     */
    public boolean deleteInAddressBook(String accessionId)
    {
        boolean result = false;

        synchronized (idMapLock)
        {
            if (accessionId != null && idMap.containsKey(accessionId))
            {
                logger.debug("Deleting Mac Address book contact " + accessionId);
                result = MacAddressBookUtils.deleteContact(idMap.get(accessionId));
            }

            if (result)
            {
                idMap.remove(accessionId);
            }
        }
        return result;
    }

    @Override
    protected void setContactAsFavourite(String id, boolean isFavourite)
    {
        // Favourite status is not written to Mac Address Book
    }

    @Override
    public void appendState(StringBuilder state)
    {
        state.append("Mac last result time: ").append(lastQueryResultTime).append("\n\n");

        for (Entry<String, String> entry : idMap.entrySet())
        {
            state.append(entry.getKey())
                 .append(", ")
                 .append(entry.getValue())
                 .append("\n");
        }
    }
}
