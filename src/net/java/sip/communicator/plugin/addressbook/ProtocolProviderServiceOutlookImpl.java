// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.insights.parameters.SipParameterInfo;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredUpdatableContactInfo.ContactUpdateResultListener;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import com.google.gson.*;

/**
 * The Outlook implementation of the Protocol Provider Service
 */
public class ProtocolProviderServiceOutlookImpl
    extends AbstractAddressBookProtocolProviderService
{
    /**
     * The logger for this class
     */
    private static final Logger logger =
                     Logger.getLogger(ProtocolProviderServiceOutlookImpl.class);

    /**
     * The contact logger for this class
     */
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * The data handler for this protocol provider service. It handles all
     * communication with the COM server.
     */
    private OutlookDataHandler outlookDataHandler;

    /**
     * The constructor for this protocol provider service
     *
     * @param accountID The account ID for which to create this account
     */
    protected ProtocolProviderServiceOutlookImpl(AccountID accountID)
    {
        super(accountID);
        logger.info("Created Outlook protocol provider " + accountID.getLoggableAccountID());
    }

    @Override
    protected AbstractAddressBookDataHandler createDataHandler()
    {
        // Create the Outlook Data Handler if necessary
        if (outlookDataHandler == null)
        {
            logger.info("Created new Outlook Data Handler");
            outlookDataHandler = new OutlookDataHandler(this);
        }

        return outlookDataHandler;
    }

    /**
     * Called by the Outlook Data Handler when a contact has been updated or
     * added.
     *
     * @param props The list of string contact details for this contact
     * @param accessionID The ID of the contact in Accession
     */
    public void contactUpdated(Object[] props, String accessionID)
    {
        AbstractAddressBookContact contact =
            (AbstractAddressBookContact) contactMap.get(accessionID);

        if (contact != null)
        {
            if (contactNeedsUpdating(contact, props))
            {
                // Need to update a contact
                // No need to hash Contact, as its toString() method does that.
                contactLogger.debug(contact, "Updating contact");
                updateContactFromOutlook(props, contact);
            }
            else
            {
                // We do not need to update the contact details, but we do
                // mark it as resolved
                // No need to hash Contact, as its toString() method does that.
                contactLogger.note(contact, "Not updating as contact details match the cache");
                contact.setResolved(true);
            }
        }
        else
        {
            // Need to create a contact
            contact = createContactFromOutlook(props, accessionID);
            // No need to hash Contact, as its toString() method does that.
            contactLogger.debug(contact, "Created new contact");
        }

        // Ensure we set the favourite status appropriately for this contact
        boolean isFavourite = isContactFavorite(props);
        updateFavoriteStatus(contact, isFavourite);
    }

    /**
     * Compare a contact already loaded in the client with a list of Outlook
     * properties. If any properties are different then return true.
     *
     * @param contact the contact already loaded in the client
     * @param props the list of Outlook properties
     * @return whether the contact requires updating from Outlook
     */
    private boolean contactNeedsUpdating(AbstractAddressBookContact contact,
        Object[] props)
    {
        List<GenericDetail> newContactDetails = getContactDetails(props);
        ArrayList<GenericDetail> oldContactDetails = contact.getDetails(null);

        // We have two lists so checking whether each contains the other
        // determines whether the lists are identical. Do not use the 'equals'
        // method as this requires order to be preserved the lists and we do
        // not care about ordering.
        if (oldContactDetails.containsAll(newContactDetails) &&
            newContactDetails.containsAll(oldContactDetails))
        {
            return false;
        }

        return true;
    }

    /**
     * Determines whether this contact is marked as a favourite in the source
     *
     * @param props the list of contact details for this contact
     * @return whether this contact is marked as a favourite in the source
     */
    private boolean isContactFavorite(Object[] props)
    {
        boolean isFavorite = false;

        Object prop = props[OutlookUtils.accessionFavoriteProp];
        if (OutlookUtils.CONTACT_FAVORITE_STATUS.equals(prop) ||
            OutlookUtils.CONTACT_OLD_FAV_STATUS.equals(prop))
        {
            isFavorite = true;
        }

        return isFavorite;
    }

    /**
     * Creates a contact from the list of details from Outlook
     *
     * @param props the contact details for the new contact
     * @param accessionID the ID of the contact to create
     */
    private AbstractAddressBookContact createContactFromOutlook(
                                             Object[] props,
                                             String accessionID)
    {
        // Contact needs creating
        // No need to hash accessionID as it is not PII
        contactLogger.debug("Creating Outlook contact " + accessionID);

        List<GenericDetail> contactDetails = getContactDetails(props);

        AddressBookContactOutlookImpl contact =
                        new AddressBookContactOutlookImpl(accessionID,
                                                          this,
                                                          true,
                                                          contactDetails,
                                                          getGroup(),
                                                          isContactFavorite(props));

        // Mark this contact as resolved as it has originated from Outlook
        contact.setResolved(true);

        // Update the persistent data for this contact
        Gson gson = new Gson();
        contact.setPersistentData(gson.toJson(props));

        // Put the contact in the map
        contactMap.put(accessionID, contact);

        // Fire the subscription event
        try
        {
            outlookDataHandler.fireSubscriptionEvent(
                                    contact,
                                    getGroup(),
                                    SubscriptionEvent.SUBSCRIPTION_CREATED);
        }
        catch (Exception e)
        {
            logger.error("Exception firing new contact", e);
        }

        return contact;
    }

    /**
     * Updates a known contact using the list of contactDetails from Outlook
     *
     * @param props the new list of contact details
     * @param contact the contact to update
     */
    private void updateContactFromOutlook(Object[] props,
                                          AbstractAddressBookContact contact)
    {
        // No need to hash Contact, as its toString() method does that.
        contactLogger.note(contact, "Updating contact from Outlook");

        // Contact needs updating, first delete the subscription
        List<GenericDetail> contactDetails = getContactDetails(props);
        try
        {
            outlookDataHandler.fireSubscriptionEvent(
                                    contact,
                                    getGroup(),
                                    SubscriptionEvent.SUBSCRIPTION_REMOVED);
        }
        catch (Exception e)
        {
            logger.error("Exception firing delete contact for modify", e);
        }

        // Mark this contact as resolved as it has been updated from Outlook
        contact.setResolved(true);

        // Update the contact
        contact.setDetails(contactDetails);
        Gson gson = new Gson();
        contact.setPersistentData(gson.toJson(props));

        // Recreate the subscription with the modified contact
        try
        {
            outlookDataHandler.fireSubscriptionEvent(
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
     * Called by the Outlook Data Handler when a contact has been deleted
     *
     * @param accessionID the ID of the contact that has been deleted
     */
    public void contactDeleted(String accessionID)
    {
        Contact contact = contactMap.get(accessionID);
        // No need to hash Contact, as its toString() method does that.
        contactLogger.debug(contact, "Contact has been deleted");

        if (contact == null)
        {
            // No contact exists, so nothing to do
            return;
        }

        // Remove the contact from the map
        contactMap.remove(accessionID);

        AddressBookProtocolActivator
                .getInsightsService()
                .logEvent(InsightsEventHint.COMMON_HINT_CONTACT_DELETED.name(),
                          Map.of(SipParameterInfo.CONTACTS_DELETED.name(),
                                 // No need to do casting like in ServerStoredContactListJabberImpl, already a `Contact`
                                 List.of(contact)));

        // Fire the subscription removed event
        try
        {
            outlookDataHandler.fireSubscriptionEvent(
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
    protected void deleteContact(Contact contact, boolean deleteInOutlook)
    {
        // No need to hash Contact, as its toString() method does that.
        contactLogger.info(contact, "Asked to delete contact " + deleteInOutlook);

        String accessionID = findIDFromContact(contact);

        // If we have found the Accession ID then continue with the delete
        // process.
        if (deleteInOutlook)
        {
            if (accessionID != null)
            {
                outlookDataHandler.deleteInOutlook(accessionID);
                contactMap.remove(accessionID);
            }
            else
            {
                contactLogger.warn(contact, "Asked to delete contact but could not find matching contact in map");
            }
        }

        // Finally fire the subscription removed event. Note we always do this
        // regardless of what has happened on Outlook to avoid confusing the
        // user.
        outlookDataHandler.fireSubscriptionEvent(
                                    contact,
                                    getGroup(),
                                    SubscriptionEvent.SUBSCRIPTION_REMOVED);
    }

    /**
     * Gets the Accession ID for the given contact
     *
     * @param contact the contact to find the Accession ID for
     * @return the Accession ID for the given contact
     */
    private String findIDFromContact(Contact contact)
    {
        // We might be able to get the Accession ID quickly by using the
        // contact address
        String accessionID = contactMap.containsKey(contact.getAddress()) ?
                                                    contact.getAddress() :
                                                    null;

        // If we couldn't find the key from the contact address then search
        // the map by comparing contacts. This is defensive code and we don't
        // expect it to be hit - contact.getAddress() should be enough.
        if (accessionID == null)
        {
            // Find the accession key from the map of contacts
            for (Entry<String, Contact> contactEntry : contactMap.entrySet())
            {
                if (contactEntry.getValue().equals(contact))
                {
                    accessionID = contactEntry.getKey();
                    break;
                }
            }
        }
        return accessionID;
    }

    @Override
    protected void createContact(String accessionID)
    {
        // Contact needs creating
        // No need to hash accessionID as it is not PII
        contactLogger.debug("Creating Outlook contact " + accessionID);

        // There are currently no details, we will add them later
        ArrayList<GenericDetail> contactDetails = new ArrayList<>();
        AddressBookContactOutlookImpl contact =
                        new AddressBookContactOutlookImpl(accessionID,
                                                          this,
                                                          true,
                                                          contactDetails,
                                                          getGroup(),
                                                          false);

        // Put the contact in the map
        contactMap.put(accessionID, contact);

        // Fire the subscription event
        try
        {
            outlookDataHandler.fireSubscriptionEvent(
                                    contact,
                                    getGroup(),
                                    SubscriptionEvent.SUBSCRIPTION_CREATED);
        }
        catch (Exception e)
        {
            logger.error("Exception firing new contact", e);
        }
    }

    @Override
    protected void setContactDetails(Contact contact,
                                     ArrayList<GenericDetail> details,
                                     ContactUpdateResultListener listener)
    {
        // First update the contact details for the contact object within
        // Accession
        // No need to hash Contact, as its toString() method does that.
        contactLogger.debug(contact, "setContactDetails");

        // Outlook contacts are filed based on a separate 'FileUnder' property.
        // We don't want to change this unnecessarily so get the value now
        ArrayList<GenericDetail> oldContactDetails =
                     ((AddressBookContactOutlookImpl) contact).getDetails(null);

        String fileUnder = null;

        for (GenericDetail oldDetail : oldContactDetails)
        {
            if (oldDetail.getDetailDisplayName().equals("FileUnder"))
            {
                fileUnder = (String) oldDetail.getDetailValue();
            }
        }

        // Check if the name of this contact has changed. If it has then we will
        // have to recreate the File Under tag.
        String oldDisplayName = contact.getDisplayName();
        ((AddressBookContactOutlookImpl) contact).setDetails(details);
        String newDisplayName = contact.getDisplayName();
        boolean nameChanged = !(oldDisplayName.equals(newDisplayName));
        String accessionID = findIDFromContact(contact);

        if (nameChanged)
        {
            // The contact's name has changed so we must change how it is filed
            // in Outlook.
            fileUnder = contact.getDisplayName();
        }

        if (accessionID != null)
        {
            if (outlookDataHandler.setDetails(accessionID,
                                              details,
                                              contact.getDisplayName(),
                                              fileUnder))
            {
                listener.updateSucceeded();
            }
            else
            {
                // No need to hash Contact, as its toString() method does that.
                contactLogger.error(contact, "Failed to set details for contact");
                listener.updateFailed(true);
            }
        }
        else
        {
            logger.error("Asked to set details for a contact but could not " +
                         "find the existing Accession ID");
            listener.updateFailed(true);
        }
    }

    /**
     * Creates a list of ContactDetails from the given list of Outlook
     * properties.
     *
     * @param props a list of Outlook properties for this contact
     * @return a list of ContactDetails
     */
    private List<GenericDetail> getContactDetails(Object[] props)
    {
        List<GenericDetail> contactDetails = new LinkedList<>();

        // Loop over each contact detail we support and add it to the list of
        // contact details if it is not null
        for (int i = 0; i < OutlookUtils.CONTACT_DETAIL_PROP_INDEXES.length; i++)
        {
            int property = OutlookUtils.CONTACT_DETAIL_PROP_INDEXES[i];
            Object value = props[property];

            // We only support string values
            if (value instanceof String)
            {
                String stringValue = (String) value;

                if (stringValue.length() != 0)
                {
                    // Get the generic detail for this property type
                    GenericDetail genericDetail =
                                            OutlookUtils.getDetail(property,
                                                                   stringValue);
                    if (genericDetail != null)
                    {
                        contactDetails.add(genericDetail);
                    }
                }
            }
        }

        return contactDetails;
    }

    @Override
    public Contact createUnresolvedContact(String address,
                                           String persistentData)
    {
        String loggableAddress = logHasher(address);
        if (outlookDataHandler.isQueryComplete())
        {
            contactLogger.warn("Not creating unresolved contact: " +
                                loggableAddress +
                                " as all contacts have been resolved from Outlook");
            return null;
        }

        if (contactMap.containsKey(address))
        {
            contactLogger.warn("Ignoring cached contact: " + loggableAddress +
                         " as it has already been resolved from Outlook");
            return null;
        }

        if (persistentData == null)
        {
            contactLogger.error("Contact: " + loggableAddress +
                                " has no persistentData");
            return null;
        }

        // Contact needs creating
        contactLogger.debug(
                "Creating Outlook contact from cache " + loggableAddress);

        // First convert the JSON string to a list of objects
        Object[] props;

        try
        {
            Gson gson = new Gson();
            props = gson.fromJson(persistentData, Object[].class);
        }
        catch (JsonSyntaxException ex)
        {
            // We cannot log persistentData in case it contains PII.
            contactLogger.error("Contact: " + loggableAddress + " persistent data couldn't be parsed", ex);
            return null;
        }

        if (props == null)
        {
            // We cannot log persistentData in case it contains PII.
            contactLogger.error("Contact: " + loggableAddress + " no persistent data.");
            return null;
        }

        // Convert the list of properties to contact details
        List<GenericDetail> contactDetails = getContactDetails(props);

        AbstractAddressBookContact contact =
                  new AddressBookContactOutlookImpl(address,
                                                    this,
                                                    true,
                                                    contactDetails,
                                                    getGroup(),
                                                    isContactFavorite(props));

        // No need to hash Contact, as its toString() method does that.
        contactLogger.trace("Set persistent data on contact: " + contact);
        contact.setPersistentData(persistentData); // Otherwise the contact is created with no persistent data

        // Put the contact in the map
        contactMap.put(address, contact);

        return contact;
    }
}
