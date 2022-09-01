// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredUpdatableContactInfo.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.*;

/**
 * Abstract protocol provider - parent of both the Outlook and Mac Address
 * Book Protocol Provider Services
 */
public abstract class AbstractAddressBookProtocolProviderService
    extends AbstractProtocolProviderService
    implements StateDumper
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractAddressBookProtocolProviderService.class);

    /**
     * The contact logger.
     */
    private static final ContactLogger contactLogger = ContactLogger.getLogger();
    private static final ThreadingService threadingService = AddressBookProtocolActivator.getThreadingService();

    /**
     * Indicates the saved contact store name.
     */
    private static final String CONTACT_STORE_KEY =
                             "net.java.sip.communicator.PERSONAL_CONTACT_STORE";

    /**
     * We use this to lock access to initialisation.
     */
    private final Object initializationLock = new Object();

    /**
     * The identifier of the account that this provider represents.
     */
    private final AccountID mAccountID;

    /**
     * The icon corresponding to the protocol.
     */
    private final AddressBookProtocolIcon addressBookIcon;

    /**
     * Holds the current state of the provider.
     */
    private RegistrationState mCurrentState = RegistrationState.UNREGISTERED;

    /**
     * Holds the current initialisation state of the provider.
     */
    private boolean mIsInitialized = false;

    /**
     * The data handler for this protocol provider. Handles communication with
     * the Address Book.
     */
    private AbstractAddressBookDataHandler mDataHandler;

    /**
     * The contact group for this protocol provider.
     */
    private AddressBookContactGroup mGroup;

    /**
     * A listener for MetaContactModifiedEvents in order to mark the
     * SourceContact's favorite status if it has been changed in the client.
     */
    private final MetaContactListAdapter mclListener = new MetaContactListAdapter()
    {
        @Override
        public void metaContactModified(MetaContactModifiedEvent event)
        {
            // Check if this is a favourites change
            if (MetaContact.CONTACT_FAVORITE_PROPERTY.equals(
                                                   event.getModificationName()))
            {
                boolean isFavourite = Boolean.parseBoolean(
                                                  (String) event.getNewValue());

                contactLogger.info(event.getSourceMetaContact(),
                    "Got favorite change event from address book - now " +
                                           String.valueOf(event.getNewValue()));

                Iterator<Contact> contacts = event.getSourceMetaContact().
                     getContactsForProvider(AbstractAddressBookProtocolProviderService.this);

                // For each contact, set the favorite status so it is
                // written to the Contact Source
                while (contacts.hasNext())
                {
                    Contact contact = contacts.next();

                    if (contact instanceof AbstractAddressBookContact)
                    {
                        contactLogger.note(event.getSourceMetaContact(),
                            "Setting favorite status for source contact " + contact +
                            " to " + String.valueOf(event.getNewValue()));
                        ((AbstractAddressBookContact) contact).setFavoriteStatus(isFavourite);
                    }
                    else
                    {
                        contactLogger.error(event.getSourceMetaContact(),
                            "Contact " + contact + " registered with " +
                            "AbstractAddressBookProtocolProviderService" +
                            " but wasn't an AbstractAddressBookContact");
                    }
                }
            }
        }
    };

    /**
     * Contains a map of all contacts that this protocol provider knows about.
     * Maps from an Accession ID to a Contact.
     */
    protected final Map<String, Contact> contactMap =
            new ConcurrentHashMap<>(1);

    /**
     * boolean indicating whether or not the MclStorageManager has finished
     * loading unresolved contacts from the local store (contactlist.xml)
     */
    private boolean mLoadedAllUnresolvedContacts;

    /**
     * The error dialog that we display on failing to connect to Outlook
     */
    private ErrorDialog mErrorDialog;

    /**
     * Initialised the service implementation, and puts it in a sate where it
     * could interop with other services. It is strongly recommended that
     * properties in this Map be mapped to property names as specified by
     * <tt>AccountProperties</tt>.
     *
     * @param accountID the identifier of the account that this protocol
     * provider represents.
     *
     * @see net.java.sip.communicator.service.protocol.AccountID
     */
    protected AbstractAddressBookProtocolProviderService(AccountID accountID)
    {
        synchronized(initializationLock)
        {
            this.mIsInitialized = false;

            this.mAccountID = accountID;

            mDataHandler = createDataHandler();

            mGroup = new AddressBookContactGroup(this, mDataHandler);

            String protocolIconPath
                = accountID.getAccountPropertyString(
                        ProtocolProviderFactory.PROTOCOL_ICON_PATH);

            addressBookIcon = new AddressBookProtocolIcon(protocolIconPath);

            this.mIsInitialized = true;
        }

        DiagnosticsServiceRegistrar.registerStateDumper(this,
                               AddressBookProtocolActivator.getBundleContext());
    }

    /**
     * @return the data handler we need for this protocol provider
     */
    protected abstract AbstractAddressBookDataHandler createDataHandler();

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for
     * shutdown/garbage collection.
     */
    public void shutdown()
    {
        logger.info("Shut down called");

        // Do the shutdown on a separate thread so we don't block the shutdown
        // thread while we wait for the initialization lock
        threadingService.submit("Address Book Shutdown Thread",
            new Runnable()
            {
                @Override
                public void run()
                {
                    synchronized(initializationLock)
                    {
                        logger.info("Shutting down");

                        if (mIsInitialized)
                        {
                            mDataHandler.uninitialize();
                            mIsInitialized = false;
                        }
                    }
                }
            });

        DiagnosticsServiceRegistrar.unregisterStateDumper(this);
    }

    /**
     * Starts the registration process. Connection details such as
     * registration server, user name/number are provided through the
     * configuration service through implementation specific properties.
     *
     * @param authority the security authority that will be used for resolving
     *        any security challenges that may be returned during the
     *        registration or at any moment while we're registered.
     * @throws OperationFailedException with the corresponding code it the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    public void register(final SecurityAuthority authority)
        throws OperationFailedException
    {
        logger.debug("Registering");

        synchronized(initializationLock)
        {
            if (!mLoadedAllUnresolvedContacts)
            {
                // The MclStorageManager hasn't yet finished loading all the
                // unresolved contacts from the local storage (contactlist.xml)
                // thus we don't want to start getting contacts yet.
                logger.info("Not yet loaded all contacts" + this);
                return;
            }

            RegistrationState currRegState = getRegistrationState();

            if(currRegState == RegistrationState.REGISTERED)
            {
                return;
            }

            mCurrentState = RegistrationState.REGISTERING;
            fireRegistrationStateChanged(
                currRegState,
                mCurrentState,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                null);

            // Initialize the data handler by starting the native libraries
            synchronized (this)
            {
                try
                {
                    mDataHandler.init();
                }
                catch (OperationFailedException e)
                {
                    logger.error("Failed to start plugin", e);

                    currRegState = mCurrentState;
                    mCurrentState = RegistrationState.CONNECTION_FAILED;
                    fireRegistrationStateChanged(
                        currRegState,
                        mCurrentState,
                        RegistrationStateChangeEvent.REASON_CONNECTION_FAILURE,
                        null);

                    Object contactStore =
                        AddressBookProtocolActivator.getConfigService().
                            user().getProperty(CONTACT_STORE_KEY);
                    if (!ProtocolNames.ADDRESS_BOOK.equals(contactStore))
                    {
                        // This shouldn't happen.  But if it does there's no
                        // point in showing the error dialog - the user doesn't
                        // care
                        logger.warn("Unable to init Outlook contacts" +
                                     " but user doesn't care. " + contactStore);
                    }
                    else if (mErrorDialog == null || !mErrorDialog.isVisible())
                    {
                        // We aren't showing any error message yet, so create it
                        logger.debug("No error dialog yet");
                        ResourceManagementService res =
                                    AddressBookProtocolActivator.getResources();
                        String title = res.getI18NString(
                                "service.gui.LOCAL_CONTACT_SOURCE_ERROR_TITLE");
                        String text = res.getI18NString(
                                 "service.gui.LOCAL_CONTACT_SOURCE_ERROR_TEXT");

                        mErrorDialog = new ErrorDialog(null, title, text);
                        mErrorDialog.addWindowListener(new WindowAdapter()
                        {
                            @Override
                            public void windowClosed(WindowEvent e)
                            {
                                // Clear the stored reference - no need for it.
                                mErrorDialog = null;
                            }
                        });

                        mErrorDialog.showDialog();
                    }

                    // If we stay registered, the reconnect plugin will keep
                    // re-attempting and the user will be bombarded with the
                    // same error message above.  It's a better user experience
                    // to just give up, and tell the user to restart if they
                    // think it's just a transient error.
                    //
                    // TODO: A better option might be to ask if the user wants
                    // to [permanently?] switch to using Accession contacts
                    // instead, and if so retry using Accession contacts instead.
                    unregister();
                    throw e;
                }

                logger.debug("Address Book Plugin started");

                // Register this protocol provider as a MetaContactListListener so
                // changes to a MetaContact can be written to the Address Book.
                AddressBookProtocolActivator.getMetaContactListService()
                .addMetaContactListListener(mclListener);
            }

            currRegState = getRegistrationState();
            mCurrentState = RegistrationState.REGISTERED;
            fireRegistrationStateChanged(
                currRegState,
                mCurrentState,
                RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                null);
        }
    }

    /**
     * Returns the AccountID that uniquely identifies the account represented
     * by this instance of the ProtocolProviderService.
     * @return the id of the account represented by this provider.
     */
    public AccountID getAccountID()
    {
        return mAccountID;
    }

    /**
     * Returns the contact group for this protocol provider.
     *
     * @return the contact group for this protocol provider.
     */
    public AddressBookContactGroup getGroup()
    {
        return mGroup;
    }

    /**
     * Returns the state of the registration of this protocol provider
     * @return the <tt>RegistrationState</tt> that this provider is
     * currently in or null in case it is in a unknown state.
     */
    public RegistrationState getRegistrationState()
    {
        return mCurrentState;
    }

    /**
     * Whether the protocol provider has completed initialization
     * @return the initialization state
     */
    public boolean isInitialized()
    {
        return this.mIsInitialized;
    }

    /**
     * Ends the registration of this protocol provider with the service.
     */
    public void unregister()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            // Make sure we aren't going to block the EDT while we wait for the
            // initializationLock
            threadingService.submit("CS unregister",
                new Runnable()
                {
                    public void run()
                    {
                        unregister();
                    }
                }
            );

            return;
        }

        logger.info("Unregister " + this);

        synchronized(initializationLock)
        {
            mDataHandler.uninitialize();
            this.mIsInitialized = false;

            RegistrationState currRegState = getRegistrationState();
            mCurrentState = RegistrationState.UNREGISTERED;
            fireRegistrationStateChanged(
                currRegState,
                mCurrentState,
                RegistrationStateChangeEvent.REASON_USER_REQUEST, null);

            AddressBookProtocolActivator.getMetaContactListService()
                                    .removeMetaContactListListener(mclListener);
        }
    }

    /**
     * Indicates if the XMPP transport channel is using a TLS secured socket.
     *
     * @return True when TLS is used, false otherwise.
     */
    public boolean isSignalingTransportSecure()
    {
        return true;
    }

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM, or others for
     * example).
     *
     * @return a String containing the short name of the protocol this
     *   service is taking care of.
     */
    public String getProtocolName()
    {
        return ProtocolNames.ADDRESS_BOOK;
    }

    /**
     * Returns the "transport" protocol of this instance used to carry the
     * control channel for the current protocol service.
     *
     * @return The "transport" protocol of this instance: TCP, TLS or UNKNOWN.
     */
    public TransportProtocol getTransportProtocol()
    {
        return TransportProtocol.UNKNOWN;
    }

    /**
     * Returns the protocol icon.
     * @return the protocol icon
     */
    public AddressBookProtocolIcon getProtocolIcon()
    {
        return addressBookIcon;
    }

    /**
     * This protocol doesn't support status
     * @return false
     */
    @Override
    public boolean supportsStatus()
    {
        return false;
    }

    @Override
    public boolean useRegistrationForStatus()
    {
        // We don't want to use this account's registration state to set
        // status
        return false;
    }

    @Override
    public <T extends OperationSet> void addSupportedOperationSet(
                                                   Class<T> opsetClass, T opset)
    {
        // Just expose this method to the data handler
        super.addSupportedOperationSet(opsetClass, opset);
    }

    /**
     * Returns all the contacts that this protocol provider knows about
     * @return
     */
    public Iterator<Contact> getAllContacts()
    {
        return contactMap.values().iterator();
    }

    /**
     * Deletes the given contact
     *
     * @param contact the contact to delete
     * @param deleteInContactStore If true then the contact will be deleted both
     *        locally and in the underlying store.  Otherwise will only be deleted
     *        locally.
     */
    protected abstract void deleteContact(Contact contact,
                                          boolean deleteInContactStore);

    /**
     * Creates a contact with the given identifier
     *
     * @param contactIdentifier
     */
    protected abstract void createContact(String contactIdentifier);

    /**
     * Sets the list of details for a contact
     *
     * @param contact the contact for which to set the details
     * @param details the list of details to set
     * @param listener the listener to inform of success or failure
     */
    protected abstract void setContactDetails(Contact contact,
                                          ArrayList<GenericDetail> details,
                                          ContactUpdateResultListener listener);

    /**
     * Sets the favourite status of the given contact in the contact source
     *
     * @param isFavourite whether this contact is a favourite
     */
    public void setContactAsFavorite(String id, boolean isFavourite)
    {
        mDataHandler.setContactAsFavourite(id, isFavourite);
    }

    /**
     * Updates the favourite status of either the contact that we've been passed
     * or the parent MetaContact.
     * <p/>
     * If this contact is a favourite contact, and the parent MetaContact is not
     * then it will update the MetaContact to be a favourite.
     * <p/>
     * If this contact is not a favourite but the parent is, then it will update
     * this contact to remember that it is a favourite
     *
     * @param contact the contact to set the favourite status for
     * @param isFavourite whether this contact is a favorite or not.
     */
    public void updateFavoriteStatus(AbstractAddressBookContact contact,
                                     boolean isFavourite)
    {
        String detailName = MetaContact.CONTACT_FAVORITE_PROPERTY;
        MetaContact metaContact = AddressBookProtocolActivator.
              getMetaContactListService().findMetaContactByContact(contact);

        // If we have a Meta Contact then set the favourite detail
        // appropriately.
        if (metaContact != null)
        {
            if (isFavourite)
            {
                contactLogger.note(metaContact,
                    "Making MetaContact favorite to match child AddressBookContact " +
                    contact);
                metaContact.setDetail(detailName, String.valueOf(true));
            }
            else if (Boolean.parseBoolean(metaContact.getDetail(detailName)))
            {
                contactLogger.note(metaContact,
                    "Making AddressBookContact favorite " +  contact +
                                           " to match the parent MetaContact.");
                contact.setFavoriteStatus(true);
            }
        }
        else
        {
            contactLogger.warn(contact, "Can't update fav status as no MetaContact");
        }
    }

    @Override
    public boolean supportsReconnection()
    {
        return false;
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try to
     * establish a network connection and resolve the newly created Contact
     * against the server. The protocol provider will later try and resolve
     * the contact. When this happens the corresponding event would notify
     * interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     *            method during a previous run and that has been persistently
     *            stored locally.
     * @return the unresolved <tt>Contact</tt> created from the specified
     *         <tt>address</tt> and <tt>persistentData</tt>
     */
    public abstract Contact createUnresolvedContact(String address,
                                                    String persistentData);

    /**
     * Removes any contacts that are marked as unresolved. This must be called
     * after the contact server has finished notifying us of contacts.
     *
     */
    public void purgeUnresolvedContacts()
    {
        contactLogger.info("Purge unresolved contacts");
        int nbUnresolvedContacts = 0;

        for (Contact contact : contactMap.values())
        {
            if (!contact.isResolved())
            {
                // This is an unresolved contact so delete it and remove it
                // from the contact map.
                contactLogger.debug("Removing contact " + contact.getAddress() +
                             " as it was not returned by the contact source");
                nbUnresolvedContacts++;
                deleteContact(contact, false);
            }
        }

        if (nbUnresolvedContacts > 50)
        {
            contactLogger.warn("Purging large number of unresolved contacts " +
                                                          nbUnresolvedContacts);
        }

        contactLogger.info("Purge finished " + nbUnresolvedContacts);
    }

    /**
     * Called when the operation set tells us that all unresolved contacts have
     * now been loaded from the contact store (contact list.xml) by the
     * MclStorageManager
     */
    protected void onAllUnresovledContactsLoaded()
    {
        synchronized (initializationLock)
        {
            mLoadedAllUnresolvedContacts = true;
        }

        try
        {
            register(null);
        }
        catch (OperationFailedException e)
        {
            // Nothing to do - the exception will already have been handled by
            // the code which threw it.
            logger.error("Exception thrown registering", e);
        }
    }

    @Override
    public String getStateDumpName()
    {
        return "AddressBookContacts";
    }

    @Override
    public String getState()
    {
        StringBuilder state = new StringBuilder();

        state.append("Loaded all unresolved: ")
             .append(mLoadedAllUnresolvedContacts)
             .append("\n")
             .append("Initialised? ")
             .append(mIsInitialized)
             .append("\n\n");

        for (Contact contact : contactMap.values())
        {
            state.append(logHasher(contact.getDisplayName()))
                 .append(", ")
                 .append(logHasher(contact.getAddress()))
                 .append(", ")
                 .append(contact.isResolved())
                 .append(", ")
                 .append(logHasher(contact.getPersistentData()))
                 .append("\n");
        }

        if (mDataHandler != null)
            mDataHandler.appendState(state);

        return state.toString();
    }
}
