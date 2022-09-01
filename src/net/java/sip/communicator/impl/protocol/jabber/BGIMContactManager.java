/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import static org.jitsi.util.Hasher.logHasher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.service.gui.ContactSyncBarService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredContactInfo;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkPhoneDetail;
import net.java.sip.communicator.service.protocol.event.ContactPropertyChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.protocol.event.SubscriptionEvent;
import net.java.sip.communicator.service.protocol.event.SubscriptionListener;
import net.java.sip.communicator.service.protocol.event.SubscriptionMovedEvent;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.ContactLogger;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;

/**
 * This class manages the mapping of BG contacts to corresponding IM contacts
 * when the client is using CommPortal-provisioned IM.  In addition, it also
 * handles automatic creation and deletion of IM contacts when corresponding BG
 * contacts are added and deleted when the client has auto IM population enabled.
 */
public class BGIMContactManager implements RegistrationStateChangeListener,
                                           ServiceListener,
                                           SubscriptionListener
{
    /**
     * The logger.
     */
    private static final Logger sLogger =
                               Logger.getLogger(BGIMContactManager.class);

    /**
     * Contact logger for detailed logs about contact events.
     */
    private static final ContactLogger sContactLogger =
                                                      ContactLogger.getLogger();

    /**
     * Delay after which to retry failed contact updates (1 day).
     */
    private static final long RETRY_DELAY = 24 * 60 * 60 * 1000;

    /**
     * Delay after which to process a contact operation
     */
    private static final int CONTACT_OPERATION_DELAY = 2 * 1000;

    /**
     * The bundle context.
     */
    private final BundleContext mContext = JabberActivator.getBundleContext();

    /**
     * The configuration service.
     */
    private static final ConfigurationService sConfig =
                                      JabberActivator.getConfigurationService();

    /**
     * The name of the protocol for BG contacts.
     */
    private static final String BG_CONTACT_PROTOCOL_NAME = "BG_CommPortal";

    /**
     * A map containing all BG contacts, mapping work number to contact.
     */
    private final ConcurrentHashMap<String, Contact> mBgNumbers =
            new ConcurrentHashMap<>();

    /**
     * A map containing the IM Addresses of contacts that need to be added or
     * deleted once the IM account is registered.  This is a map of IM address
     * to a boolean saying whether the contact should be added or deleted.
     */
    private final ConcurrentHashMap<String, Boolean> mContactsToUpdate =
            new ConcurrentHashMap<>();

    /**
     * A map containing the IM Addresses of contacts that failed a contact
     * update so the update needs to be retried.  This is a map of IM address
     * to a boolean saying whether the contact should be added or deleted.
     */
    private final ConcurrentHashMap<String, Boolean> mContactsToRetry =
            new ConcurrentHashMap<>();

    /**
     * A timer for scheduling creation or deletion of IM contacts.  We do these
     * operations after a slight delay (see {@link #CONTACT_OPERATION_DELAY}) to
     * allow us to prevent deleting and recreating an IM contact
     */
    private final Timer mContactTimer = new Timer("Contact Operation Timer");

    /**
     * A map from IM addresses to a scheduled task which will either create or
     * delete the associated IM contact.  This allows us to cancel a scheduled
     * task if we receive an opposite task a little later.
     */
    private final HashMap<String, IMContactTask> mOutstandingContactTasks =
            new HashMap<>();

    /**
     * Set to true once the IM account is registered.
     */
    private boolean mAccountRegistered = false;

    /**
     * The Jabber ProtocolProviderService.
     */
    private ProtocolProviderService mJabberProvider;

    /**
     * The PersistentPresence OperationSet associated with the Jabber
     * ProtocolProviderService.
     */
    private OperationSetPersistentPresence mJabberOpSet;

    /**
     * The PersistentPresence OperationSet associated with the BG
     * ProtocolProviderService.
     */
    private OperationSetPersistentPresence mBGOpSet;

    /**
     * Timer for scheduling retries of failed contact updates.
     */
    private final Timer mRetryTimer = new Timer("Retry Timer");

    /**
     * Creates a new instance of the manager, adds listeners for contact and
     * service changes and schedules a task to retry failed contact updates.
     *
     * @param provider The Jabber Protocol Provider
     */
    public BGIMContactManager(ProtocolProviderServiceJabberImpl provider)
    {
        sLogger.info("Creating a new BGIMContactManager");

        // Add a service listener so that we can listen for the BG service
        // being registered.
        mContext.addServiceListener(this);

        // Check whether the service had already registered before we added
        // the service listener.
        ServiceReference<?>[] ppsRefs = null;
        try
        {
            ppsRefs = mContext.getServiceReferences(
                ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            // This shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            sLogger.error("Error while retrieving service refs", ex);
        }

        if (ppsRefs != null)
        {
            for (ServiceReference<?> ppsRef : ppsRefs)
            {
                ProtocolProviderService pps
                    = (ProtocolProviderService) mContext.getService(ppsRef);

                if (BG_CONTACT_PROTOCOL_NAME.equals(pps.getProtocolName()))
                {
                    handleBGServiceRegistered(pps);
                    break;
                }
            }
        }

        // Store the Jabber provider and add a registration state changed
        // listener to it.
        mJabberProvider = provider;
        mJabberProvider.addRegistrationStateChangeListener(this);

        // Check whether the provider registered while we were setting up the
        // listener.
        if (RegistrationState.REGISTERED.equals(
                                        mJabberProvider.getRegistrationState()))
        {
            handleJabberAccountRegistered();
        }

        // Schedule the task to run every 24 hours that retries failed contact
        // updates then makes sure we have IM contacts for all BG contacts.
        TimerTask retryTask = new TimerTask()
        {
            @Override
            public void run()
            {
                retryFailedContactUpdates();
                if ("CommPortal".equals(ConfigurationUtils.getImProvSource()) &&
                    ConfigurationUtils.autoPopulateIMEnabled())
                {
                    sContactLogger.info("Adding Missing IM contacts");
                    addMissingIMContacts();
                }
            }
        };

        mRetryTimer.schedule(retryTask, RETRY_DELAY);
    }

    /**
     * Returns whether the given IM contact was created automatically to
     * correspond to a BG contact.
     *
     * @param imContact The IM Contact.
     * @return Whether the IM contact was created automatically to correspond
     *         to a BG contact.
     */
    public boolean isBGAutoCreatedIMContact(Contact imContact)
    {
        boolean isBGAutoCreated = false;

        // This must be false if auto-populating IM is turned off by the branding.
        if (ConfigurationUtils.autoPopulateIMEnabled())
        {
            // Otherwise, see if we can find a BG contact corresponding to the IM contact's work number.
            // The work number will be null if we're not using CommPortal-provisioned IM.
            String workNumber =  getWorkNumberFromIMContact(imContact);
            isBGAutoCreated = (workNumber == null) ? false : mBgNumbers.containsKey(workNumber);
        }

        return isBGAutoCreated;
    }

    /**
     * Returns the corresponding BG contact (if any) for the given IM contact.
     *
     * Note that this method will try to match both manually-added and, if
     * enabled in the branding, automatically provisioned IM contacts.
     *
     * @param imContact The IM contact.
     * @return The corresponding BG contact, or null if none exists.
     */
    public Contact getBGContactForIMContact(Contact imContact)
    {
        // Work number will be null if we're not using CommPortal-provisioned IM.
        String workNumber = getWorkNumberFromIMContact(imContact);
        return (workNumber == null) ? null : mBgNumbers.get(workNumber);
    }

    @Override
    public void serviceChanged(ServiceEvent evt)
    {
        // This method checks whether the service event was the BG service
        // registering or unregistering.  If so, it calls a method to add or
        // remove a listener for subscription events so that we know when BG
        // contacts have been added and deleted and we can add and delete the
        // corresponding IM contacts.
        ServiceReference<?> serviceRef = evt.getServiceReference();

        // If the event is caused by a bundle being stopped, we don't care.
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            return;

        Object service = mContext.getService(serviceRef);

        // We don't care if the source service is not a protocol provider.
        if (!(service instanceof ProtocolProviderService))
            return;

        ProtocolProviderService pps = (ProtocolProviderService) service;

        if (BG_CONTACT_PROTOCOL_NAME.equals(pps.getProtocolName()))
        {
            int eventType = evt.getType();

            if (ServiceEvent.REGISTERED == eventType)
            {
                handleBGServiceRegistered(pps);
            }
            else if (ServiceEvent.UNREGISTERING == eventType)
            {
                handleBGServiceUnregistering(pps);
            }
        }
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState newState = evt.getNewState();

        if (RegistrationState.REGISTERED.equals(newState))
        {
            handleJabberAccountRegistered();
        }
        else if (RegistrationState.UNREGISTERED.equals(newState))
        {
            handleJabberAccountUnregistered();
        }
    }

    /**
     * Adds a subscription listener to the BG service so that we are notified
     * when BG contacts are added and deleted.
     *
     * @param pps ProtocolProviderService
     */
    private void handleBGServiceRegistered(ProtocolProviderService pps)
    {
        sLogger.info("BG service registered");

        // Add a listener for subscription events so that we know when BG
        // contacts have been added and deleted and we can add and delete the
        // corresponding IM contacts.
        mBGOpSet = pps.getOperationSet(OperationSetPersistentPresence.class);
        mBGOpSet.addSubscriptionListener(this);

        // Add any BG contacts that the BG service has already loaded, as we
        // may not otherwise receive events for them.
        Iterator<Contact> bgContacts =
                           mBGOpSet.getServerStoredContactListRoot().contacts();

        while (bgContacts.hasNext())
        {
            handleContactUpdated(bgContacts.next(), true);
        }
    }

    /**
     * Removes the subscription listener from the BG service.
     *
     * @param pps ProtocolProviderService
     */
    private void handleBGServiceUnregistering(ProtocolProviderService pps)
    {
        sLogger.info("BG service unregistering");

        if (mBGOpSet != null)
        {
            mBGOpSet.removeSubscriptionListener(this);
        }
    }

    /**
     * Adds and deletes IM contacts corresponding to any BG contacts that were
     * added or deleted while the IM account was still registering.
     */
    private void handleJabberAccountRegistered()
    {
        sLogger.info("Jabber Account Registered");

        // Store persistent presence operation set.  We have to do this after
        // the account is registered, otherwise the operation set will be null.
        getJabberOpSet();

        // Set the account registered to true so that we will send any future
        // requests to add or delete IM contacts immediately that we receive a
        // contact changed event.
        synchronized (this)
        {
            mAccountRegistered = true;
        }

        // Add and delete any contacts that we were notified about while the
        // account wasn't registered.  Do this on a new thread in case there
        // are a lot of contacts so that we don't block this event being
        // passed on to other listeners.
        new Thread("BGIMUpdateThread")
        {
            public void run()
            {
                synchronized (BGIMContactManager.this)
                {
                    for (Map.Entry<String, Boolean> contactToUpdate :
                                                   mContactsToUpdate.entrySet())
                    {
                        String imAddress = contactToUpdate.getKey();
                        boolean add = contactToUpdate.getValue();

                        sContactLogger.debug("Processing pending contact " +
                                             "update: " +
                                             logHasher(imAddress) + ", " + add);
                        updateIMContact(imAddress, add);
                    }

                    // Clear the map, as we've processed its contents.
                    mContactsToUpdate.clear();
                }
            }
        }.start();
    }

    /**
     * Sets the jabber account to be unregistered.
     */
    private void handleJabberAccountUnregistered()
    {
        sLogger.info("Jabber Account Unregistered");

        // Set the account registered to false so that we will stop sending
        // requests to add or delete IM contacts until the account is
        // registered again. Also set the jabber opset to null so that we will
        // get a reference to the new op set when the account re-registers.
        synchronized (this)
        {
            mAccountRegistered = false;
            mJabberOpSet = null;
        }
    }

    /**
     * @return the stored jabber PersistentPresence OperationSet if one exists,
     * otherwise it first gets the OperationSet from the stored jabber
     * ProtocolProviderService and stores it before returning it.
     *
     */
    private OperationSetPersistentPresence getJabberOpSet()
    {
        if (mJabberOpSet == null)
        {
            mJabberOpSet = mJabberProvider.getOperationSet(
                                          OperationSetPersistentPresence.class);
        }

        return mJabberOpSet;
    }

    /**
     * Adds or removes a contact's work number from the list of BG numbers
     * and, if the client is using CommPortal IM, it adds or deletes a
     * corresponding IM contact.
     *
     * @param updatedContact The BG contact that has been added or deleted.
     * @param add If true, the contact's work number will be added to the list
     * and a corresponding IM contact will be added.  Otherwise, they will be
     * removed.
     */
    private void handleContactUpdated(Contact updatedContact, boolean add)
    {
        // No need to hash Contact, as its toString() method does that.
        if (!updatedContact.supportsIMAutoPopulation())
        {
            sContactLogger.debug("Ignoring contact that does not support " +
                                 "IM autopopulation: " + updatedContact);
            return;
        }

        sContactLogger.debug(
            "Handling contact update: " + updatedContact + ", " + add);

        String workNumber = getWorkNumberFromBGContact(updatedContact);

        if (workNumber != null)
        {
            synchronized (this)
            {
                if (add)
                {
                    mBgNumbers.put(workNumber, updatedContact);
                }
                else
                {
                    mBgNumbers.remove(workNumber);
                }
            }

            if (ConfigurationUtils.isImEnabled())
            {
                if ("CommPortal".equals(ConfigurationUtils.getImProvSource()) &&
                    ConfigurationUtils.autoPopulateIMEnabled())
                {
                    String imDomain = sConfig.user().getString(
                                      "net.java.sip.communicator.im.IM_DOMAIN");

                    String imAddress = workNumber + "@" + imDomain;
                    String loggableImAddress = logHasher(imAddress);

                    synchronized (this)
                    {
                        if (mAccountRegistered)
                        {
                            sContactLogger.debug("Account registered. " +
                                                 "Update: " +
                                                 loggableImAddress + ", " + add);
                            updateIMContact(imAddress, add);
                        }
                        else
                        {
                            sContactLogger.debug("Account not registered. " +
                                                 "Pending update: " +
                                                 loggableImAddress + ", " + add);
                            mContactsToUpdate.put(imAddress, add);
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds or deletes an IM contact.
     *
     * @param imAddress The IM address of the contact to add or delete.
     * @param add If true, the contact will be added, if false, it will be
     * deleted.
     */
    private void updateIMContact(String imAddress, boolean add)
    {
        synchronized (mOutstandingContactTasks)
        {
            // Get and remove the current task (if it exists) from the map of
            // outstanding tasks.  Note: this map does not contain tasks that
            // are currently being executed
            IMContactTask existingTask =
                                     mOutstandingContactTasks.remove(imAddress);
            IMContactTask newTask = null;

            if (existingTask == null)
            {
                // No task - thus schedule one.
                newTask = new IMContactTask(imAddress, add);
            }
            else if (existingTask.mAdd != add)
            {
                // There is a task but it is for the opposite operation that we
                // have been asked to do.  They cancel each other out so cancel
                // the existing task and don't create the new task
                existingTask.cancel();
            }
            else
            {
                // There is a task which exactly matches this one.  Cancel the
                // existing task and schedule a new one
                sContactLogger.warn("Asked to schedule task that already exists " +
                                    logHasher(imAddress) + ", " + add);
                existingTask.cancel();
                newTask = new IMContactTask(imAddress, add);
            }

            if (newTask != null)
            {
                // Got a new task - add it to the map and execute it.
                mOutstandingContactTasks.put(imAddress, newTask);
                mContactTimer.schedule(newTask, CONTACT_OPERATION_DELAY);
            }
        }
    }

    /**
     * Retries adding and deleting any IM contacts that failed on their last
     * update attempt.
     */
    private void retryFailedContactUpdates()
    {
        // First we need to make a copy of the map of contacts to retry then
        // clear the map, so that we have an empty map to add update failures
        // to that happen while we're retrying.  Synchronize this so that we
        // can't add any new entries to the map between when we copy it and
        // when we clear it.
        HashMap<String, Boolean> tempContactsToRetry;
        synchronized (this)
        {
            tempContactsToRetry = new HashMap<>(mContactsToRetry);
            mContactsToRetry.clear();
        }

        for (Map.Entry<String, Boolean> contactToRetry :
                                                 tempContactsToRetry.entrySet())
        {
            String imAddress = contactToRetry.getKey();
            boolean add = contactToRetry.getValue();

            sContactLogger.debug("Retrying failed contact update: " +
                                 logHasher(imAddress) + ", " + add);
            updateIMContact(imAddress, add);
        }
    }

    /**
     * Checks whether an IM contact already exists for every BG number and, if
     * not, it adds one.
     */
    private void addMissingIMContacts()
    {
        synchronized (mBgNumbers)
        {
            OperationSetPersistentPresence jabberOpSet = getJabberOpSet();
            if (jabberOpSet != null)
            {
                for (Map.Entry<String, Contact> bgNumberEntry :
                                                          mBgNumbers.entrySet())
                {
                    String bgNumber = bgNumberEntry.getKey();
                    String imDomain = sConfig.user().getString(
                                      "net.java.sip.communicator.im.IM_DOMAIN");
                    String imAddress = bgNumber + "@" + imDomain;

                    Contact imContact = jabberOpSet.findContactByID(imAddress);
                    if (imContact == null)
                    {
                        sContactLogger.debug("Adding missing IM contact for " +
                                             logHasher(imAddress));
                        updateIMContact(imAddress, true);
                    }
                }
            }
            else
            {
                sContactLogger.warn(
                        "Unable to add missing IM contacts as no op set");
            }
        }
    }

    /**
     * Gets the work number (if it exists) from a BG Contact
     *
     * @param bgContact The BG Contact
     * @return The BG Contact's work number, or null if none found.
     */
    private String getWorkNumberFromBGContact(Contact bgContact)
    {
        String workNumber = null;

        if (bgContact != null)
        {
            OperationSetServerStoredContactInfo opSet = bgContact
                    .getProtocolProvider()
                    .getOperationSet(OperationSetServerStoredContactInfo.class);

            if (opSet != null)
            {
                Iterator<GenericDetail> workPhoneDetails =
                             opSet.getDetails(bgContact, WorkPhoneDetail.class);

                if (workPhoneDetails != null && workPhoneDetails.hasNext())
                {
                    // Use the first work phone detail, as there shouldn't be
                    // more than one.
                    Object workPhoneDetailValue =
                                       workPhoneDetails.next().getDetailValue();

                    if (workPhoneDetailValue != null)
                    {
                        workNumber = workPhoneDetailValue.toString();
                    }
                }
            }
        }

        return workNumber;
    }

    /**
     * Gets the work number from an IM Contact, if we're using
     * CommPortal-provisioned IM.
     *
     * @param imContact The IM Contact.
     * @return The IM Contact's work number, or null if we're not using
     *         CommPortal-provisioned IM.
     */
    private String getWorkNumberFromIMContact(Contact imContact)
    {
        String workNumber = null;

        if (imContact != null)
        {
            // The first part of the contact's IM address will only map to a work
            // number of a BG contact if CommPortal IM is enabled.
            if ("CommPortal".equals(ConfigurationUtils.getImProvSource()))
            {
                String imContactAddress = imContact.getAddress();

                if (imContactAddress != null)
                {
                    workNumber = imContactAddress.split("@")[0];
                }
            }
        }

        return workNumber;
    }

    @Override
    public void subscriptionCreated(SubscriptionEvent evt)
    {
        // A BG Contact has been created. Run the method to handle adding a
        // corresponding IM contact.
        handleContactUpdated(evt.getSourceContact(), true);
    }

    @Override
    public void subscriptionRemoved(SubscriptionEvent evt)
    {
        // A BG Contact has been removed. Run the method to handle adding a
        // corresponding IM contact.
        handleContactUpdated(evt.getSourceContact(), false);
    }

    @Override
    public void subscriptionResolved(SubscriptionEvent evt)
    {
        // A BG Contact from the local contact list has been resolved as
        // existing on the server. Run the method to handle adding a
        // corresponding IM contact because loading from local store does not
        // fire any events so this is the only way we ever know of these
        // contacts.
        handleContactUpdated(evt.getSourceContact(), true);
    }

    @Override
    public void subscriptionFailed(SubscriptionEvent evt)
    {
        // Not required
    }

    @Override
    public void subscriptionMoved(SubscriptionMovedEvent evt)
    {
        // Not required
    }

    @Override
    public void contactModified(ContactPropertyChangeEvent evt)
    {
        // Not required
    }

    /**
     * A task which will either create or delete an IM contact as required
     */
    private class IMContactTask extends TimerTask
    {
        /**
         * If true, then will add a new IM contact.  Otherwise delete it
         */
        private final boolean mAdd;

        /**
         * The address of the IM contact to add or removed
         */
        private final String mAddress;

        /**
         * The address of the IM contact to add or removed, hashed so that it
         * can be logged without logging PII.
         */
        private final String mLoggableAddress;

        public IMContactTask(String contactAddress, boolean add)
        {
            mAddress = contactAddress;
            mAdd = add;
            mLoggableAddress = logHasher(mAddress);
        }

        @Override
        public void run()
        {
            // Make sure that we remove the contact task from the map.
            synchronized (mOutstandingContactTasks)
            {
                IMContactTask outstandingTask =
                                      mOutstandingContactTasks.remove(mAddress);

                // Contact task has already been removed!  Thus this task should
                // not be executed
                if (outstandingTask == null)
                {
                    sContactLogger.info("Executing task that has been cancelled " +
                                        mLoggableAddress + ", " + mAdd);
                    return;
                }
            }

            try
            {
                OperationSetPersistentPresence jabberOpSet = getJabberOpSet();
                if (jabberOpSet != null)
                {
                    Contact imContact = jabberOpSet.findContactByID(mAddress);

                    if (mAdd && (imContact == null))
                    {
                        sContactLogger.debug(
                            "About to add IM contact " + mLoggableAddress);
                        fireContactSyncEvent();
                        jabberOpSet.subscribe(mAddress);
                    }
                    else if (!mAdd && (imContact != null))
                    {
                        sContactLogger.debug(
                            "About to remove IM contact " + mLoggableAddress);
                        fireContactSyncEvent();
                        jabberOpSet.unsubscribe(imContact);
                    }
                }
                else
                {
                    sContactLogger.warn("Null op set - unable to update IM contact " +
                                        mLoggableAddress);
                    addToRetryList();
                }
            }
            catch (Exception e)
            {
                // Make sure we catch any type of exception that we might hit,
                // as otherwise we will abort adding any further contacts.
                sContactLogger.error(
                        "Failed to update IM contact " + mLoggableAddress, e);
                addToRetryList();
            }
        }

        /**
         * Adds this IM contact update to a retry list.
         */
        private void addToRetryList()
        {
            synchronized (BGIMContactManager.this)
            {
                sContactLogger.debug("Adding IM contact update to retry list " +
                                     mLoggableAddress + ", " + mAdd);
                mContactsToRetry.put(mAddress, mAdd);
            }
        }

        /**
         * Fires an event to trigger displaying the 'synchronizing contacts'
         * bar.
         */
        private void fireContactSyncEvent()
        {
            ContactSyncBarService contactSyncBarService =
                                     JabberActivator.getContactSyncBarService();

            if (contactSyncBarService != null)
            {
                contactSyncBarService.fireContactEvent();
            }
        }
    }

    /*
     * Do any necessary clean up.
     */
    protected void cleanUp()
    {
        sLogger.info("Cleanup");

        // Stop listening for stuff
        mContext.removeServiceListener(this);
        mJabberProvider.removeRegistrationStateChangeListener(this);
        if (mBGOpSet != null)
        {
            mBGOpSet.removeSubscriptionListener(this);
        }

        // Make sure we cancel the timers, they're expensive to leak.
        mContactTimer.cancel();
        mRetryTimer.cancel();
    }
}
