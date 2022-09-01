// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredUpdatableContactInfo.ContactUpdateResultListener;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import com.google.gson.*;

/**
 * The Mac Address Book implementation of the Protocol Provider Service
 */
public class ProtocolProviderServiceMacAddressBookImpl
    extends AbstractAddressBookProtocolProviderService
{
    /**
     * The logger for this class
     */
    private static final Logger logger =
              Logger.getLogger(ProtocolProviderServiceMacAddressBookImpl.class);

    /**
     * The contact logger for this class
     */
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * The data handler for this protocol provider service. It handles all
     * communication with the native address book code
     */
    private MacAddressBookDataHandler macAddressBookDataHandler;

    /**
     * The constructor for this protocol provider service
     *
     * @param accountID The account ID for which to create this account
     */
    protected ProtocolProviderServiceMacAddressBookImpl(AccountID accountID)
    {
        super(accountID);
        logger.info("Created Mac Address Book protocol provider");
    }

    @Override
    protected AbstractAddressBookDataHandler createDataHandler()
    {
        // Create the Mac Address Book Data Handler if necessary
        if (macAddressBookDataHandler == null)
        {
            logger.info("Creating new data handler");
            macAddressBookDataHandler = new MacAddressBookDataHandler(this);
        }

        return macAddressBookDataHandler;
    }

    @Override
    protected void deleteContact(Contact contact, boolean deleteInAddrbook)
    {
        String id = contact.getAddress();

        // No need to hash Contact, as its toString() method does that.
        contactLogger.debug(contact, "Asked to delete contact");

        // If we have got an id then continue with the delete process.
        if (deleteInAddrbook)
        {
            if (id != null)
            {
                macAddressBookDataHandler.deleteInAddressBook(id);
                contactMap.remove(id);
            }
            else
            {
                contactLogger.warn(contact, "Asked to delete contact but it did not have an ID");
            }
        }

        // Finally fire the subscription removed event. Note we always do this
        // regardless of what has happened in Mac Address Boook to avoid
        // confusing the user.
        try
        {
            macAddressBookDataHandler.fireSubscriptionEvent(
                                        contact,
                                        getGroup(),
                                        SubscriptionEvent.SUBSCRIPTION_REMOVED);
        }
        catch (Exception e)
        {
            logger.error("Exception removing contact", e);
        }
    }

    @Override
    protected void createContact(String id)
    {
        // Contact needs creating
        contactLogger.debug("Creating Mac address book contact " + id);

        // There are currently no details, we will add them later
        ArrayList<GenericDetail> contactDetails = new ArrayList<>();
        AbstractAddressBookContact contact =
                        new AddressBookContactMacImpl(id,
                                                      this,
                                                      true,
                                                      contactDetails,
                                                      null,
                                                      getGroup());

        // Put the contact in the map
        contactMap.put(id, contact);

        // Fire the subscription event
        try
        {
            macAddressBookDataHandler.fireSubscriptionEvent(
                                        contact,
                                        getGroup(),
                                        SubscriptionEvent.SUBSCRIPTION_CREATED);
        }
        catch (Exception e)
        {
            contactLogger.error("Exception firing new contact", e);
        }
    }

    @Override
    protected void setContactDetails(Contact contact,
                                     ArrayList<GenericDetail> details,
                                     ContactUpdateResultListener listener)
    {
        // Store the existing contact details in case the edit fails.
        // No need to hash Contact, as its toString() method does that.
        contactLogger.debug(contact, "setContactDetails");
        ArrayList<GenericDetail> oldDetails =
                         ((AbstractAddressBookContact) contact).getDetails(null);

        // Update the contact details for the contact object within Accession
        ((AbstractAddressBookContact) contact).setDetails(details);
        String id = contact.getAddress();

        if (id != null)
        {
            if (macAddressBookDataHandler.setDetails(id, contact))
            {
                listener.updateSucceeded();

                // Contact has been updated so we need to fire a property
                // change event for it. oldValue and newValue are not used so
                // just pass the contact
                macAddressBookDataHandler.fireContactPropertyChangeEvent(
                            ContactPropertyChangeEvent.PROPERTY_DISPLAY_DETAILS,
                            contact,
                            contact,
                            contact);
            }
            else
            {
                contactLogger.error(contact, "Failed to set details for contact");

                // Revert the contact details
                ((AbstractAddressBookContact) contact).setDetails(oldDetails);
                listener.updateFailed(true);
            }
        }
        else
        {
            contactLogger.error(contact, "Asked to set details for a contact but could not " +
                         "find the existing Accession ID");

            // Revert the contact details
            ((AbstractAddressBookContact) contact).setDetails(oldDetails);
            listener.updateFailed(true);
        }
    }

    /**
     * Called by the Mac Address Book Data Handler when a contact has been
     * updated.
     *
     * @param values an array of contact details for this contact
     * @param id The ID of the contact
     */
    public void contactUpdated(Object[] values, String id)
    {
        AbstractAddressBookContact contact =
                       (AbstractAddressBookContact) contactMap.get(id);
        // No need to hash Contact, as its toString() method does that.
        contactLogger.debug(contact, "contactUpdated");

        if (contact != null)
        {
            if (contactNeedsUpdating(contact, values))
            {
                // Need to update a contact
                contactLogger.debug(contact, "Contact needs updating");
                updateContactFromAddressBook(values, id);
            }
            else
            {
                // We do not need to update the contact details, but we do
                // mark it as resolved
                contactLogger.note(contact, "Not updating contact" +
                             " as contact details match the cache");
                contact.setResolved(true);
            }
        }
        else
        {
            // Create the contact
            // No need to hash id as it is not PII
            contactLogger.debug("Creating contact with id " + id);
            createContactFromAddressBook(values, id);
        }
    }

    /**
     * Compare a contact already loaded in the client with a list of Mac
     * AddressBook values. If any properties are different then return true.
     *
     * @param contact the contact already loaded in the client
     * @param values the list of Mac AddressBook values
     * @return whether the contact requires updating
     */
    private boolean contactNeedsUpdating(AbstractAddressBookContact contact,
                                         Object[] values)
    {
        List<GenericDetail> newContactDetails = new LinkedList<>();
        List<GenericDetail> newServerDetails = new LinkedList<>();
        constructContactDetails(values, newContactDetails, newServerDetails);

        List<GenericDetail> oldServerDetails =
                       ((AddressBookContactMacImpl) contact).getServerDetails();

        List<GenericDetail> oldContactDetails = contact.getDetails(null);

        // We have two pairs of contact details. If each matching list contains
        // the other then the lists are identical. Do not use List.equals()
        // method as this requires order to be consistent across all lists
        if (oldContactDetails.containsAll(newContactDetails) &&
            newContactDetails.containsAll(oldContactDetails) &&
            oldServerDetails.containsAll(newServerDetails) &&
            newServerDetails.containsAll(oldServerDetails))
        {
            return false;
        }

        return true;
    }

    /**
     * Creates a contact from the list of details from Mac Address Book
     *
     * @param values the list of contact values for the new contact
     * @param id the ID of the contact to create
     */
    private void createContactFromAddressBook(Object[] values, String id)
    {
        // Contact needs creating

        List<GenericDetail> contactDetails = new LinkedList<>();
        List<GenericDetail> serverDetails = new LinkedList<>();
        constructContactDetails(values, contactDetails, serverDetails);

        AbstractAddressBookContact contact =
                        new AddressBookContactMacImpl(id,
                                                      this,
                                                      true,
                                                      contactDetails,
                                                      serverDetails,
                                                      getGroup());
        // No need to hash id as it is not PII
        contactLogger.debug(contact, "Created Mac Address book contact with id " + id);

        // Mark this contact as resolved as it has originated from Mac Address
        // Book
        contact.setResolved(true);

        // Update the persistent data for this contact
        Gson gson = new Gson();
        contact.setPersistentData(gson.toJson(values));

        // Put the contact in the map
        contactMap.put(id, contact);

        // Fire the subscription event
        try
        {
            macAddressBookDataHandler.fireSubscriptionEvent(
                                    contact,
                                    getGroup(),
                                    SubscriptionEvent.SUBSCRIPTION_CREATED);
        }
        catch (Exception e)
        {
            logger.error("Exception firing new contact", e);
        }
    }

    /**
     * Updates a known contact using the list of contactDetails from Mac
     * Address Book
     *
     * @param values the list of new contact details
     * @param id the id of the contact to update
     */
    private void updateContactFromAddressBook(Object[] values, String id)
    {
        // Contact needs updating
        AbstractAddressBookContact contact =
                           (AbstractAddressBookContact) contactMap.get(id);
        // No need to hash Contact, as its toString() method does that.
        contactLogger.note(contact, "updateContactFromAddressBook " + id);

        // First delete the subscription
        try
        {
            macAddressBookDataHandler.fireSubscriptionEvent(
                                    contact,
                                    getGroup(),
                                    SubscriptionEvent.SUBSCRIPTION_REMOVED);
        }
        catch (Exception e)
        {
            logger.error("Exception firing delete contact for modify", e);
        }

        // Update the contact
        List<GenericDetail> contactDetails = new LinkedList<>();
        List<GenericDetail> serverDetails = new LinkedList<>();
        constructContactDetails(values, contactDetails, serverDetails);
        contact.setDetails(contactDetails);

        // Mark this contact as resolved as it has been updated from Mac
        // Address Book
        contact.setResolved(true);
        Gson gson = new Gson();
        contact.setPersistentData(gson.toJson(values));

        // Recreate the subscription with the modified contact
        try
        {
            macAddressBookDataHandler.fireSubscriptionEvent(
                                    contact,
                                    getGroup(),
                                    SubscriptionEvent.SUBSCRIPTION_CREATED);
        }
        catch (Exception e)
        {
            logger.error("Exception firing create contact for modify", e);
        }
    }

    /**
     * Called by the Mac Address Book Data Handler when a contact has been
     * deleted
     *
     * @param id the ID of the contact that has been deleted
     */
    public void contactDeleted(String id)
    {
        Contact contact = contactMap.get(id);
        // No need to hash Contact, as its toString() method does that.
        contactLogger.debug(contact, "contactDeleted");

        if (contact == null)
        {
            // No contact exists, so nothing to do
            return;
        }

        // Remove the contact from the map
        contactMap.remove(id);

        // Fire the subscription removed event
        try
        {
            macAddressBookDataHandler.fireSubscriptionEvent(
                                    contact,
                                    getGroup(),
                                    SubscriptionEvent.SUBSCRIPTION_REMOVED);
        }
        catch (Exception e)
        {
            logger.error("Exception firing delete contact", e);
        }
    }

    @Override
    public Contact createUnresolvedContact(String address,
                                           String persistentData)
    {
        String loggableAddress = logHasher(address);
        if (macAddressBookDataHandler.isQueryComplete())
        {
            contactLogger.warn("Not creating unresolved contact: " + loggableAddress +
                               " as all contacts have been resolved from Outlook");
            return null;
        }

        if (contactMap.containsKey(address))
        {
            contactLogger.warn("Ignoring cached contact: " + loggableAddress +
                         " as is has already been resolved from Mac AddressBook");
            return null;
        }

        // Contact needs creating
        contactLogger.debug("Creating Mac AddressBook contact from cache " + loggableAddress);

        // First convert the JSON string to a list of objects
        Gson gson = new Gson();
        Object[] values = gson.fromJson(persistentData, Object[].class);

        values = convertJSONValues(values);

        // Update the contact
        List<GenericDetail> contactDetails = new LinkedList<>();
        List<GenericDetail> serverDetails = new LinkedList<>();
        constructContactDetails(values, contactDetails, serverDetails);

        AbstractAddressBookContact contact =
                            new AddressBookContactMacImpl(address,
                                                          this,
                                                          true,
                                                          contactDetails,
                                                          serverDetails,
                                                          getGroup());

        // Put the contact in the map
        contactMap.put(address, contact);

        return contact;
    }

    /**
     * Populates the list of passed contact details and server details
     *
     * @param values a list of address book properties for this contact
     * @param contactDetails a list of contact details that Accession exposes
     * for this contact
     * @param serverDetails a list of contact details that Accession does not
     * expose but must include when writing back to Mac Address Book
     */
    private void constructContactDetails(Object[] values,
                                         List<GenericDetail> contactDetails,
                                         List<GenericDetail> serverDetails)
    {
        // Loop over each value in the list of contact property values
        for (int i = 0; i < MacAddressBookUtils.ABPERSON_PROPERTIES.length; i++)
        {
            int property = i;
            Object value = values[property];

            if (value instanceof String)
            {
                // If the value is a string then we can add the contact detail
                // straight away
                String stringValue = (String) value;

                if (stringValue.length() != 0)
                {
                    if (MacAddressBookUtils.kABPhoneProperty == property)
                        stringValue
                            = PhoneNumberI18nService.normalize(stringValue);

                    MacAddressBookUtils.convertValueToDetail(property,
                                                  stringValue,
                                                  null,
                                                  null,
                                                  contactDetails,
                                                  serverDetails);
                }
            }
            else if (value instanceof Object[])
            {
                // If the value is another list of objects then we need to
                // separately parse each contact detail
                parseMultiDetails((Object[]) value,
                                  property,
                                  null,
                                  contactDetails,
                                  serverDetails);
            }
        }
    }

    /**
     * Parses the input 'multi-value' data array to contact details. A
     * 'multi-value' detail is a field in Mac Address Book that can store
     * multiple values.
     *
     * @param multiValue the values to parse.
     * @param property the current property being parsed.
     * @param additionalProperty the additional property to set for this
     * contact detail
     * @param contactDetails a list of contact details that Accession exposes
     * for this contact
     * @param serverDetails a list of contact details that Accession does not
     * expose but must include when writing back to Mac Address Book
     */
    private void parseMultiDetails(Object[] multiValue,
                                   int property,
                                   String additionalProperty,
                                   List<GenericDetail> contactDetails,
                                   List<GenericDetail> serverDetails)
    {
        if (multiValue == null)
        {
            return;
        }

        // The format of multiValue is (label, value). value may be another
        // multiValue object.
        // Loop through each label-value pair
        for (int multiValueIndex = 0;
                multiValueIndex < multiValue.length;
                multiValueIndex += 2)
        {
            Object subValue = multiValue[multiValueIndex];

            if (subValue instanceof String)
            {
                // This is a straight label-value pair so there is only one
                // contact detail to add
                String stringSubValue = (String) subValue;

                if (stringSubValue.length() != 0)
                {
                    if (MacAddressBookUtils.kABPhoneProperty == property)
                    {
                        // We must normalize phone numbers before adding them
                        stringSubValue = PhoneNumberI18nService
                            .normalize(stringSubValue);
                    }

                    Object label = multiValue[multiValueIndex + 1];

                    MacAddressBookUtils.convertValueToDetail(property,
                                                  stringSubValue,
                                                  label,
                                                  additionalProperty,
                                                  contactDetails,
                                                  serverDetails);
                }
            }
            else if (subValue instanceof Object[])
            {
                // The value of this label-value pair is another multi-line
                // object so recursively continue to parse.
                String subAdditionalProperty = null;

                Object value = multiValue[multiValueIndex + 1];

                if (value instanceof String)
                {
                    subAdditionalProperty = (String) value;
                }

               parseMultiDetails((Object[]) subValue,
                                 property,
                                 subAdditionalProperty,
                                 contactDetails,
                                 serverDetails);
            }
        }
    }

    /**
     * The GSON library deserializes the JSON persistent data into ArrayList
     * types. This recursive function converts all ArrayLists to primitive arrays.
     *
     * @param values the array of values to convert
     * @return an array of primitives
     */
    private Object[] convertJSONValues(Object[] values)
    {
        Object[] newValues = values;

        for (int index = 0; index < values.length; index++)
        {
            Object value = values[index];
            if (value instanceof String)
            {
                // Nothing to do for Strings
                continue;
            }
            else if (value instanceof ArrayList<?>)
            {
                // Convert the ArrayList to a primitive array
                newValues[index] = convertJSONValues(((ArrayList<?>) value).toArray());
            }
        }

        return newValues;
    }
}
