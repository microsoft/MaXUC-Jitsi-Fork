/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.contactlist;

import static java.util.stream.Collectors.toSet;
import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.xml.*;
import org.osgi.framework.*;

import com.drew.lang.annotations.NotNull;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.wispaservice.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the MetaContactListService that would connect to
 * protocol service providers and build it  s contact list accordingly
 * basing itself on the contact list stored by the various protocol provider
 * services and the contact list instance saved on the hard disk.
 * <p>
 *
 * @author Emil Ivov
 */
public class MetaContactListServiceImpl
    implements MetaContactListService,
               ServiceListener,
               ContactPresenceStatusListener,
               ContactCapabilitiesListener,
               StateDumper
{
    /**
     * The <tt>Logger</tt> used by the <tt>MetaContactListServiceImpl</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MetaContactListServiceImpl.class);

    /**
     * The <tt>ContactLogger</tt> used by this class for logging contact
     * operations in more detail
     */
    private static final ContactLogger contactLogger
        = ContactLogger.getLogger();

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    /**
     * The list of protocol providers that we're currently aware of.
     */
    private final Map<String, ProtocolProviderService> currentlyInstalledProviders
        = new Hashtable<>();

    /**
     * The root of the meta contact list.
     */
    final MetaContactGroupImpl rootMetaGroup;

    /**
     * The event handler that will be handling our subscription events.
     */
    private final ContactListSubscriptionListener clSubscriptionEventHandler
        = new ContactListSubscriptionListener();

    /**
     * The event handler that will be handling group events.
     */
    private final ContactListGroupListener clGroupEventHandler
        = new ContactListGroupListener();

    /**
     * The number of milliseconds to wait for confirmations of account
     * modifications before deciding to drop.
     */
    public static final int CONTACT_LIST_MODIFICATION_TIMEOUT = 10000;

    /**
     * Listeners interested in events dispatched upon modification of the meta
     * contact list.
     */
    private final List<MetaContactListListener> metaContactListListeners
        = new Vector<>();

    /**
     * Contains (as keys) <tt>MetaContactGroup</tt> names that are currently
     * being resolved against a given protocol and that this class's
     * <tt>ContactGroupListener</tt> should ignore as corresponding events will
     * be handled by the corresponding methods. The table maps the meta contact
     * group names against lists of protocol providers. An incoming group event
     * would therefore be ignored by the class group listener if and only if it
     * carries a name present in this table and is issued by one of the
     * providers mapped against this groupName.
     */
    private final Hashtable<String, List<ProtocolProviderService>>
        groupEventIgnoreList = new Hashtable<>();

    /**
     * This keeps a list of outstanding contacts which have been added by the
     * client and we are awaiting confirmation from the server that they have
     * been added, as opposed to contacts which have been added on another client
     * and the server is informing us about for the first time.
     */
    private final PendingContactList pendingContacts = new PendingContactList();

    /**
     * The instance of the storage manager which is handling the local copy of
     * our contact list.
     */
    private final MclStorageManager storageManager = new MclStorageManager();

    /**
     * Service exposing an interface for communicating with WISPA, the API for
     * communicating with applications via websockets (e.g. Electron UI).
     */
    private static WISPAService wispaService;

    /**
     * Creates an instance of this class.
     */
    public MetaContactListServiceImpl()
    {
        rootMetaGroup
            = new MetaContactGroupImpl(
                    this,
                    "RootMetaContactGroup",
                    "RootMetaContactGroup");

        // Initialise our connection to WISPA.
        wispaService = ContactlistActivator.getWISPAService();
    }

    /**
     * Starts this implementation of the MetaContactListService. The
     * implementation would first restore a default contact list from what has
     * been stored in a file. It would then connect to OSGI and retrieve any
     * existing protocol providers and if <br>
     * 1) They provide implementations of OperationSetPersistentPresence, it
     * would synchronize their contact lists with the local one (adding
     * subscriptions for contacts that do not exist in the server stored contact
     * list and adding locally contacts that were found on the server but not in
     * the local file).
     * <p>
     * 2) The only provide non persistent implementations of
     * OperationSetPresence, the meta contact list impl would create
     * subscriptions for all local contacts in the corresponding protocol
     * provider.
     * <p>
     * This implementation would also start listening for any newly registered
     * protocol provider implementations and perform the same algorithm with
     * them.
     * <p>
     *
     * @param bc the currently valid OSGI bundle context.
     */
    public void start(BundleContext bc)
    {
        logger.debug("Starting the meta contact list implementation.");
        this.bundleContext = bc;

        //initialize the meta contact list from what has been stored locally.
        try
        {
            storageManager.start(bundleContext, this);
        }
        catch (Exception exc)
        {
            logger.error("Failed loading the stored contact list.", exc);
        }

        // start listening for newly register or removed protocol providers
        bc.addServiceListener(this);

        // first discover the icq service
        // then find the protocol provider service
        ServiceReference<?>[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any, retrieve the root groups for all protocol
        // providers and create the meta contact list
        if (protocolProviderRefs != null)
        {
            logger.debug("Found "
                     + protocolProviderRefs.length
                     + " already installed providers.");
            for (ServiceReference<?> providerRef : protocolProviderRefs)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) bc.getService(providerRef);

                if (provider == null)
                {
                    logger.warn("Provider with reference " + providerRef + " has already been unregistered.");
                    continue;
                }

                this.handleProviderAdded(provider);
            }
        }
    }

    /**
     * Prepares the meta contact list service for shutdown.
     *
     * @param bc the currently active bundle context.
     */
    public void stop(BundleContext bc)
    {
        storageManager.storeContactListAndStopStorageManager();
        bc.removeServiceListener(this);

        //stop listening to all currently installed providers
        for (ProtocolProviderService pp : currentlyInstalledProviders.values())
        {
            OperationSetPersistentPresence opSetPersPresence =
                pp.getOperationSet(OperationSetPersistentPresence.class);

            if(opSetPersPresence !=null)
            {
                opSetPersPresence
                    .removeContactPresenceStatusListener(this);
                opSetPersPresence
                    .removeSubscriptionListener(clSubscriptionEventHandler);
                opSetPersPresence
                    .removeServerStoredGroupChangeListener(clGroupEventHandler);
            }
            else
            {
                //check if a non persistent presence operation set exists.
                OperationSetPresence opSetPresence =
                    pp.getOperationSet(OperationSetPresence.class);

                if(opSetPresence != null)
                {
                    opSetPresence
                        .removeContactPresenceStatusListener(this);
                    opSetPresence
                        .removeSubscriptionListener(clSubscriptionEventHandler);
                }
            }
        }
        currentlyInstalledProviders.clear();
        storageManager.stop();
    }

    /**
     * Adds a listener for <tt>MetaContactListChangeEvent</tt>s posted after
     * the tree changes.
     *
     * @param listener the listener to add
     */
    @Override
    public void addMetaContactListListener(MetaContactListListener listener)
    {
        logger.info("Adding MCL listener " + listener);

        synchronized (metaContactListListeners)
        {
            if(!metaContactListListeners.contains(listener))
                // Add the MCL listener at the beginning of the list - this is
                // so that the listeners that registered first are informed of
                // changes last.
                metaContactListListeners.add(0, listener);
        }
    }

    /**
     * Returns true if there are no known asynchronous responses
     * to contact events remaining to listen out for.
     */
    public boolean hasProcessedAllAsyncEvents(final MetaContact metaContact)
    {
        synchronized (PendingContactList.metaContactToContactIdSet)
        {
            return PendingContactList
                    .metaContactToContactIdSet.stream()
                    .noneMatch(e -> e.getMetaContact().equals(metaContact));
        }
    }

    /**
     * First makes the specified protocol provider create the contact as
     * indicated by <tt>contactID</tt>, and then associates it to the
     * _existing_ <tt>metaContact</tt> given as an argument.
     *
     * @param provider
     *            the ProtocolProviderService that should create the contact
     *            indicated by <tt>contactID</tt>.
     * @param metaContact
     *            the meta contact where that the newly created contact should
     *            be associated to.
     * @param contactID
     *            the identifier of the contact that the specified provider
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    @Override
    public void addNewContactToMetaContact(
        ProtocolProviderService provider,
        MetaContact metaContact, String contactID)
            throws MetaContactListException
    {
        contactLogger.debug(metaContact, "addNewContactToMetaContact");
        addNewContactToMetaContact(provider, metaContact, contactID, true);
    }

    /**
     * First makes the specified protocol provider create the contact as
     * indicated by <tt>contactID</tt>, and then associates it to the
     * _existing_ <tt>metaContact</tt> given as an argument.
     *
     * @param provider
     *            the ProtocolProviderService that should create the contact
     *            indicated by <tt>contactID</tt>.
     * @param metaContact
     *            the meta contact where that the newly created contact should
     *            be associated to.
     * @param contactID
     *            the identifier of the contact that the specified provider
     * @param fireEvent
     *            specifies whether or not an even is to be fire at
     * the end of the method.Used when this method is called upon creation of a
     * new meta contact and not only a new contact.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void addNewContactToMetaContact( ProtocolProviderService provider,
                                            MetaContact metaContact,
                                            String contactID,
                                            boolean fireEvent)
        throws MetaContactListException
    {
        //find the parent group in the corresponding protocol.
        MetaContactGroup parentMetaGroup
            = findParentMetaContactGroup(metaContact);

        if (parentMetaGroup == null)
        {
            throw new MetaContactListException(
                "orphan Contact: " + metaContact
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);
        }

        addNewContactToMetaContact(provider, parentMetaGroup, metaContact,
                contactID, fireEvent);
    }

    /**
     * First makes the specified protocol provider create the contact as
     * indicated by <tt>contactID</tt>, and then associates it to the
     * _existing_ <tt>metaContact</tt> given as an argument.
     *
     * @param provider
     *            the ProtocolProviderService that should create the contact
     *            indicated by <tt>contactID</tt>.
     * @param parentMetaGroup
     *            the meta contact group which is the parent group of the newly
     *            created contact
     * @param metaContact
     *            the meta contact where that the newly created contact should
     *            be associated to.
     * @param contactID
     *            the identifier of the contact that the specified provider
     * @param fireEvent
     *            specifies whether or not an even is to be fired at
     * the end of the method.Used when this method is called upon creation of a
     * new meta contact and not only a new contact.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    private void addNewContactToMetaContact( ProtocolProviderService provider,
                                            MetaContactGroup parentMetaGroup,
                                            MetaContact metaContact,
                                            String contactID,
                                            boolean fireEvent)
        throws MetaContactListException
    {
        OperationSetPersistentPresence opSetPersPresence =
            provider.getOperationSet(OperationSetPersistentPresence.class);
        if (opSetPersPresence == null)
        {
            /** @todo handle non-persistent presence operation sets as well */
            return;
        }

        if (! (metaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(
                    metaContact
                    + " is not an instance of MetaContactImpl");
        }

        ContactGroup parentProtoGroup
            = resolveProtoPath(provider, (MetaContactGroupImpl) parentMetaGroup);

        if (parentProtoGroup == null)
        {
            throw new MetaContactListException(
                "Could not obtain proto group parent for " + metaContact
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);
        }

        PendingContactList.Listener listener = pendingContacts.addListener(provider, contactID);

        synchronized (PendingContactList.metaContactToContactIdSet)
        {
            PendingContactList.metaContactToContactIdSet.add(
                    new MetaContactAndContactID(metaContact, contactID));
        }

        try
        {
            //create the contact in the group
            // if its the root group just call subscribe
            if(parentMetaGroup.equals(rootMetaGroup))
                opSetPersPresence.subscribe(contactID);
            else
                opSetPersPresence.subscribe(parentProtoGroup, contactID);

            //wait for a confirmation event
            listener.waitForEvent(CONTACT_LIST_MODIFICATION_TIMEOUT);
        }
        catch(OperationFailedException ex)
        {
            if(ex.getErrorCode()
               == OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS)
            {
                throw new MetaContactListException(
                "failed to create contact " + contactID
                , ex
                , MetaContactListException.CODE_CONTACT_ALREADY_EXISTS_ERROR);
            }
            else if(ex.getErrorCode()
               == OperationFailedException.NOT_SUPPORTED_OPERATION)
            {
                throw new MetaContactListException(
                "failed to create contact " + contactID
                , ex
                , MetaContactListException.CODE_NOT_SUPPORTED_OPERATION);
            }

            throw new MetaContactListException(
                "failed to create contact " + contactID
                , ex
                , MetaContactListException.CODE_NETWORK_ERROR);
        }
        catch (Exception ex)
        {
            throw new MetaContactListException(
                "failed to create contact " + contactID
                , ex
                , MetaContactListException.CODE_NETWORK_ERROR);
        }
        finally
        {
            // Whatever happens we need to remove the event collector
            // end the ignore filter.
            pendingContacts.removeListener(listener);
        }

        // If the listener's event is null, we didn't receive a response
        // before the timeout expired.
        if (listener.evt == null)
        {
            int errorCode = MetaContactListException.CODE_NETWORK_ERROR;
            Contact srcContact = opSetPersPresence.findContactByID(contactID);

            logger.debug("No contact add event retrieved for " + contactID +
                         ". Contact found on server = " + srcContact);

            if (srcContact != null)
            {
                if(srcContact.isPersistent())
                {
                    // A persistent contact already exists, so set the error
                    // code as such.
                    errorCode =
                        MetaContactListException.CODE_CONTACT_ALREADY_EXISTS_ERROR;
                }
                else
                {
                    try
                    {
                        // If the existing contact is not persistent, it
                        // probably means that the server failed to respond
                        // correctly and the roster entry for this contact on
                        // the server has got into a bad state that won't sort
                        // itself out.  In case that is true, send an
                        // unsubscribe, to clear any broken entries for this
                        // contact from the roster.
                        contactLogger.debug(srcContact,
                                            "Sending unsubscribe after subscription " +
                                            "subscription timeout for " +
                                            contactID);
                        opSetPersPresence.unsubscribe(srcContact);
                    }
                    catch (Exception e)
                    {
                        // Just log if we hit an exception here, as it doesn't
                        // matter if the unsubscribe failed, as we're just
                        // trying to tidy up a potentially broken roster entry.
                        contactLogger.warn("Unsubscribe attempt failed after " +
                            "subscription timeout for " + contactID, e);
                    }
                }
            }

            throw new MetaContactListException(
                "Failed to create a contact with address: "
                + contactID
                , null
                , errorCode);
        }

        // Check whether the subscription failed
        if (listener.evt instanceof SubscriptionEvent &&
            listener.evt.getEventID() ==
            SubscriptionEvent.SUBSCRIPTION_FAILED)
        {
            throw new MetaContactListException(
                "Failed to create a contact with address: "
                + contactID + " "
                + listener.evt.getErrorReason()
                , null
                , MetaContactListException.CODE_UNKNOWN_ERROR);
        }

        //now finally - add the contact to the meta contact
        ( (MetaContactImpl) metaContact).addProtoContact(
            listener.sourceContact);

        //only fire an event here if the calling method wants us to. in case
        //this is the creation of a new contact and not only addition of a
        //proto contact we should remain silent and the calling method will
        //do the eventing.
        if(fireEvent)
        {
            this.fireProtoContactEvent(listener.sourceContact,
                                       ProtoContactEvent.PROTO_CONTACT_ADDED,
                                       null,
                                       metaContact,
                                       null);
        }
        ((MetaContactGroupImpl) parentMetaGroup).addMetaContact(
                (MetaContactImpl)metaContact);
    }

    /**
     * Makes sure that directories in the whole path from the root to the
     * specified group have corresponding directories in the protocol indicated
     * by <tt>protoProvider</tt>. The method does not return before creating
     * all groups has completed.
     *
     * @param protoProvider a reference to the protocol provider where the
     * groups should be created.
     * @param metaGroup a ref to the last group of the path that should be
     * created in the specified <tt>protoProvider</tt>
     *
     * @return e reference to the newly created <tt>ContactGroup</tt>
     */
    private ContactGroup resolveProtoPath(ProtocolProviderService protoProvider,
                                          MetaContactGroupImpl metaGroup)
    {
        Iterator<ContactGroup> contactGroupsForProv = metaGroup
            .getContactGroupsForProvider(protoProvider);

        if (contactGroupsForProv.hasNext())
        {
            //we already have at least one group corresponding to the meta group
            return contactGroupsForProv.next();
        }
        //we don't have a proto group here. obtain a ref to the parent
        //proto group (which may be created along the way) and create it.
        MetaContactGroupImpl parentMetaGroup = (MetaContactGroupImpl)
            findParentMetaContactGroup(metaGroup);
        if (parentMetaGroup == null)
        {
            logger.error(
                "Internal Error - Orphan group: Resolve failed at group " +
                                     logHasher(metaGroup.getGroupName()));
            return null;
        }

        OperationSetPersistentPresence opSetPersPresence =
            protoProvider.getOperationSet(OperationSetPersistentPresence.class);

        //if persistent presence is not supported - just bail
        //we should have verified this earlier anyway
        if (opSetPersPresence == null)
        {
            return null;
        }

        ContactGroup parentProtoGroup;
        // special treatment for the root group (stop the recursion)
        if (parentMetaGroup.getParentMetaContactGroup() == null) {
            parentProtoGroup = opSetPersPresence.
                                    getServerStoredContactListRoot();
        } else {
            parentProtoGroup = resolveProtoPath(protoProvider, parentMetaGroup);
        }

        //create the proto group
        BlockingGroupEventRetriever evtRetriever
            = new BlockingGroupEventRetriever(metaGroup.getGroupName());

        opSetPersPresence.addServerStoredGroupChangeListener(evtRetriever);

        addGroupToEventIgnoreList(metaGroup.getGroupName(), protoProvider);

        try
        {
            //create the group
            opSetPersPresence.createServerStoredContactGroup(
                parentProtoGroup, metaGroup.getGroupName());

            //wait for a confirmation event
            evtRetriever.waitForEvent(CONTACT_LIST_MODIFICATION_TIMEOUT);
        }
        catch (Exception ex)
        {
            throw new MetaContactListException(
                "failed to create contact group " + metaGroup.getGroupName()
                , ex
                , MetaContactListException.CODE_NETWORK_ERROR);
        }
        finally
        {
            //whatever happens we need to remove the event collector
            //and the ignore filter.
            removeGroupFromEventIgnoreList(metaGroup.getGroupName()
                                           , protoProvider);
            opSetPersPresence.removeServerStoredGroupChangeListener(
                evtRetriever);
        }

        //sth went wrong.
        if (evtRetriever.evt == null)
        {
            throw new MetaContactListException(
                "Failed to create a proto group named: "
                + metaGroup.getGroupName()
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);
        }

        //now add the proto group to the meta group.
        metaGroup.addProtoGroup(evtRetriever.evt.getSourceGroup());

        fireMetaContactGroupEvent(
            metaGroup
            , evtRetriever.evt.getSourceProvider()
            , evtRetriever.evt.getSourceGroup()
            , MetaContactGroupEvent.CONTACT_GROUP_ADDED_TO_META_GROUP
            , null);

        return evtRetriever.evt.getSourceGroup();
    }

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>. If no parent is found <tt>null</tt> is returned.
     * @param child the <tt>MetaContactGroup</tt> whose parent group we're
     * looking for. If no parent is found <tt>null</tt> is returned.
     *
     * @return the <tt>MetaContactGroup</tt> that contains <tt>child</tt> or
     * null if no parent was found.
     */
    @Override
    public MetaContactGroup findParentMetaContactGroup(MetaContactGroup child)
    {
        return findParentMetaContactGroup(rootMetaGroup, child);
    }

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>, beginning the search at the specified root. If
     * no parent is found <tt>null</tt> is returned.
     * @param child the <tt>MetaContactGroup</tt> whose parent group we're
     * looking for.
     * @param root the parent where the search should start.
     * @return the <tt>MetaContactGroup</tt> that contains <tt>child</tt> or
     * null if no parent was found.
     */
    private MetaContactGroup findParentMetaContactGroup(
        MetaContactGroupImpl root, MetaContactGroup child)
    {
        return child.getParentMetaContactGroup();
    }

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>.
     * @param child the <tt>MetaContact</tt> whose parent group we're looking
     * for.
     *
     * @return the <tt>MetaContactGroup</tt>
     * @throws IllegalArgumentException if <tt>child</tt> is not an instance of
     * MetaContactImpl
     */
    @Override
    public MetaContactGroup findParentMetaContactGroup(MetaContact child)
    {
        if (! (child instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(child
                                       + " is not a MetaContactImpl instance.");
        }
        return ( (MetaContactImpl) child).getParentGroup();
    }

    /**
     * First makes the specified protocol provider create a contact
     * corresponding to the specified <tt>contactID</tt>, then creates a new
     * MetaContact which will encapsulate the newly created protocol specific
     * contact.
     *
     * @param provider
     *            a ref to <tt>ProtocolProviderService</tt> instance which
     *            will create the actual protocol specific contact.
     * @param metaContactGroup
     *            the MetaContactGroup where the newly created meta contact
     *            should be stored.
     * @param contactID
     *            a protocol specific string identifier indicating the contact
     *            the protocol provider should create.
     * @return the newly created <tt>MetaContact</tt>
     *
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    @Override
    public MetaContact createMetaContact(  ProtocolProviderService provider,
                                    MetaContactGroup metaContactGroup,
                                    String contactID)
        throws MetaContactListException
    {
        if (! (metaContactGroup instanceof MetaContactGroupImpl))
        {
            throw new IllegalArgumentException(metaContactGroup
                + " is not an instance of MetaContactGroupImpl");
        }

        MetaContactImpl newMetaContact = new MetaContactImpl();

        this.addNewContactToMetaContact(provider, metaContactGroup,
            newMetaContact, contactID, false);
            //don't fire a PROTO_CONT_ADDED event we'll
            //fire our own event here.

        fireMetaContactEvent(   newMetaContact,
                                findParentMetaContactGroup(newMetaContact),
                                MetaContactEvent.META_CONTACT_ADDED,
                                null);

        return newMetaContact;
    }

    /**
     * Creates a <tt>MetaContactGroup</tt> with the specified group name.
     * The meta contact group would only be created locally and resolved
     * against the different server stored protocol contact lists upon the
     * creation of the first protocol specific child contact in the respective
     * group.
     *
     * @param parent
     *            the meta contact group inside which the new child group must
     *            be created.
     * @param groupName the name of the <tt>MetaContactGroup</tt> to create.
     * @return the newly created <tt>MetaContactGroup</tt>
     *
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    @Override
    public MetaContactGroup createMetaContactGroup(MetaContactGroup parent,
                                       String groupName)
        throws MetaContactListException
    {
        if (! (parent instanceof MetaContactGroupImpl))
        {
            throw new IllegalArgumentException(
                parent
                + " is not an instance of MetaContactGroupImpl");
        }

        //make sure that "parent" does not already contain a subgroup called
        //"groupName"
        Iterator<MetaContactGroup> subgroups = parent.getSubgroups();

        while(subgroups.hasNext())
        {
            MetaContactGroup group = subgroups.next();

            if(group.getGroupName().equals(groupName))
            {
                throw new MetaContactListException(
                    "Parent " + parent.getGroupName() + " already contains a "
                    + "group called " + groupName,
                    new CloneNotSupportedException("just testing nested exc-s"),
                    MetaContactListException.CODE_GROUP_ALREADY_EXISTS_ERROR);
            }
        }

        // we only have to create the meta contact group here.
        // we don't care about protocol specific groups.
        MetaContactGroupImpl newMetaGroup
            = new MetaContactGroupImpl(this, groupName);

        ( (MetaContactGroupImpl) parent).addSubgroup(newMetaGroup);

        //fire the event
        fireMetaContactGroupEvent(newMetaGroup
            , null, null, MetaContactGroupEvent. META_CONTACT_GROUP_ADDED, null);

        return newMetaGroup;
    }

    /**
     * Renames the specified <tt>MetaContactGroup</tt> as indicated by the
     * <tt>newName</tt> param.
     * The operation would only affect the local meta group and would not
     * "touch" any encapsulated protocol specific group.
     * <p>
     * @param group the group to rename.
     * @param newGroupName the new name of the <tt>MetaContactGroup</tt> to
     * rename.
     */
    @Override
    public void renameMetaContactGroup(MetaContactGroup group,
                                       String newGroupName)
    {
        ( (MetaContactGroupImpl) group).setGroupName(newGroupName);

        Iterator<ContactGroup> groups = group.getContactGroups();

        while (groups.hasNext())
        {
            ContactGroup protoGroup =  groups.next();

            //get a persistent presence operation set
            OperationSetPersistentPresence opSetPresence
                = protoGroup
                    .getProtocolProvider()
                        .getOperationSet(
                            OperationSetPersistentPresence.class);

            if (opSetPresence != null)
            {
                try
                {
                    opSetPresence.renameServerStoredContactGroup(
                        protoGroup, newGroupName);
                }
                catch(Throwable t)
                {
                    logger.error("Error renaming protocol group: "
                        + protoGroup, t);
                }
            }
        }

        fireMetaContactGroupEvent(group, null, null
            , MetaContactGroupEvent.META_CONTACT_GROUP_RENAMED, null);
    }

    @Override
    public List<MetaContact> getDisplayableContactList()
    {
        List<MetaContact> contactList = new ArrayList<>();
        Iterator<MetaContact> metaContactIterator = rootMetaGroup.getChildContacts();
        while(metaContactIterator.hasNext())
        {
            MetaContact contact = metaContactIterator.next();

            List<String> hiddenDetails =
                    contact.getDetails(MetaContact.IS_CONTACT_HIDDEN);
            boolean isHidden = hiddenDetails.size() != 0 && Boolean.parseBoolean(hiddenDetails.get(0));

            if (!isHidden)
            {
                contactList.add(contact);
            }
            else
            {
                logger.debug("Not sending contact " + contact + " because it is a hidden contact");
            }
        }
        return contactList;
    }

    /**
     * Returns the root <tt>MetaContactGroup</tt> in this contact list.
     *
     * @return the root <tt>MetaContactGroup</tt> for this contact list.
     */
    @Override
    public MetaContactGroup getRoot()
    {
        return rootMetaGroup;
    }

    /**
     * Sets the display name for <tt>metaContact</tt> to be <tt>newName</tt>.
     * <p>
     * @param metaContact the <tt>MetaContact</tt> that we are renaming
     * @param newDisplayName a <tt>String</tt> containing the new display name
     * for <tt>metaContact</tt>.
     * @throws IllegalArgumentException if <tt>metaContact</tt> is not an
     * instance that belongs to the underlying implementation.
     */
    @Override
    public void renameMetaContact(MetaContact metaContact, String newDisplayName)
        throws IllegalArgumentException
    {
        contactLogger.debug(metaContact, "renameMetaContact to " +
            logHasher(newDisplayName));
        renameMetaContact(metaContact, newDisplayName, true);
    }

    /**
     * Sets the display name for <tt>metaContact</tt> to be <tt>newName</tt>.
     * <p>
     * @param metaContact the <tt>MetaContact</tt> that we are renaming
     * @param newDisplayName a <tt>String</tt> containing the new display name
     * for <tt>metaContact</tt>.
     * @throws IllegalArgumentException if <tt>metaContact</tt> is not an
     * instance that belongs to the underlying implementation.
     */
    private void renameMetaContact(MetaContact metaContact,
                                   String newDisplayName,
                                   boolean isUserDefined)
        throws IllegalArgumentException
    {
        if (! (metaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(
                metaContact + " is not a MetaContactImpl instance.");
        }

        String oldDisplayName = metaContact.getDisplayName();

        ((MetaContactImpl)metaContact).setDisplayName(newDisplayName);
        if(isUserDefined)
            ((MetaContactImpl)metaContact).setDisplayNameUserDefined(true);

        Iterator<Contact> contacts = metaContact.getContacts();

        while (contacts.hasNext())
        {
            Contact protoContact =  contacts.next();

            //get a persistent presence operation set
            OperationSetPersistentPresence opSetPresence
                = protoContact
                    .getProtocolProvider()
                        .getOperationSet(
                            OperationSetPersistentPresence.class);

            if (opSetPresence != null)
            {
                try
                {
                    opSetPresence.setDisplayName(protoContact, newDisplayName);
                }
                catch(Throwable t)
                {
                    logger.error("Error renaming protocol contact: "
                        + protoContact, t);
                }
            }
        }

        fireMetaContactEvent(new MetaContactRenamedEvent(
            metaContact, oldDisplayName, newDisplayName),
                null);

        //changing the display name has surely brought a change in the order as
        //well so let's tell the others
        fireMetaContactGroupEvent(
                    findParentMetaContactGroup( metaContact )
                    , null
                    , null
                    , MetaContactGroupEvent.CHILD_CONTACTS_REORDERED
                    , null);
    }

    /**
     * Resets display name of the MetaContact to show the value from
     * the underlying contacts.
     * @param metaContact the <tt>MetaContact</tt> that we are operating on
     * @throws IllegalArgumentException if <tt>metaContact</tt> is not an
     * instance that belongs to the underlying implementation.
     */
    @Override
    public void clearUserDefinedDisplayName(MetaContact metaContact)
        throws IllegalArgumentException
    {
        if (! (metaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(
                metaContact + " is not a MetaContactImpl instance.");
        }

        // set display name
        ((MetaContactImpl)metaContact).setDisplayNameUserDefined(false);

        if(metaContact.getContactCount() == 1)
        {
            renameMetaContact(metaContact,
                metaContact.getDefaultContact().getDisplayName(), false);
        }
        else
        {
            // just fire event so the modification is stored
            fireMetaContactEvent(new MetaContactRenamedEvent(
                        metaContact,
                        metaContact.getDisplayName(),
                        metaContact.getDisplayName()),
                            null);
        }
    }

    /**
     * Sets the avatar for <tt>metaContact</tt> to be <tt>newAvatar</tt>.
     * <p>
     * @param metaContact the <tt>MetaContact</tt> that change avatar
     * @param protoContact the <tt>Contact> that change avatar
     * @param newAvatar avatar image bytes
     * @throws IllegalArgumentException if <tt>metaContact</tt> is not an
     * instance that belongs to the underlying implementation.
     */
    public void changeMetaContactAvatar(MetaContact metaContact,
                                        Contact protoContact,
                                        BufferedImageFuture newAvatar)
        throws IllegalArgumentException
    {
        if (! (metaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException("Contact " + protoContact +
                " :" + metaContact + " is not a MetaContactImpl instance.");
        }

        BufferedImageFuture oldAvatar = metaContact.getAvatar(true);
        ((MetaContactImpl) metaContact).cacheAvatar(protoContact, newAvatar);

        fireMetaContactEvent(
            new MetaContactAvatarUpdateEvent(metaContact, oldAvatar, newAvatar),
                null);
    }

    /**
     * Makes the specified <tt>contact</tt> a child of the
     * <tt>newParentMetaGroup</tt> MetaContactGroup. If <tt>contact</tt> was
     * previously a child of a meta contact, it will be removed from its
     * old parent and to a newly created one even if they both are in the same
     * group. If the specified contact was the only child of its previous
     * parent, then the meta contact will also be moved.
     *
     *
     * @param contact the <tt>Contact</tt> to move to the
     * @param newParentMetaGroup the MetaContactGroup where we'd like contact to be moved.
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    @Override
    public void moveContact(Contact contact,
                            MetaContactGroup newParentMetaGroup)
        throws MetaContactListException
    {
        // First create the new meta contact
        MetaContactImpl metaContactImpl = new MetaContactImpl();

        MetaContactGroupImpl newParentMetaGroupImpl
            = (MetaContactGroupImpl)newParentMetaGroup;

        newParentMetaGroupImpl.addMetaContact(metaContactImpl);

        fireMetaContactEvent(metaContactImpl
                             , newParentMetaGroupImpl
                             , MetaContactEvent.META_CONTACT_ADDED
                             , null);

        // Then move the sub contact to the new metacontact container
        moveContact(contact, metaContactImpl);
    }

    /**
     * Makes the specified <tt>contact</tt> a child of the <tt>newParent</tt>
     * MetaContact.
     *
     * @param contact
     *            the <tt>Contact</tt> to move to the
     * @param newParentMetaContact
     *            the MetaContact where we'd like contact to be moved.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    @Override
    public void moveContact(Contact contact,
                            MetaContact newParentMetaContact) throws
        MetaContactListException
    {
        contactLogger.debug(newParentMetaContact, "moveContact");
        if (! (newParentMetaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(
                newParentMetaContact + " is not a MetaContactImpl instance.");
        }

        // Find out whether the contact we're moving is already in a
        // MetaContact.  If it is, remove it from that MetaContact before
        // adding it to the new one.  If not, we can just add it straight to
        // the new MetaContact.
        MetaContactImpl currentParentMetaContact
            = (MetaContactImpl)this.findMetaContactByContact(contact);

        if (currentParentMetaContact != null)
        {
            // If source and destination are the same then there is no need to
            // do anything here.
            if (currentParentMetaContact.equals(newParentMetaContact))
            {
                return;
            }

            currentParentMetaContact.removeProtoContact(contact);
        }

        //get a persistent  presence operation set
        OperationSetPersistentPresence opSetPresence
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetPersistentPresence.class);

        if (opSetPresence == null)
        {
            /** @todo handle non persistent presence operation sets */
        }

        MetaContactGroup newParentGroup
            = findParentMetaContactGroup(newParentMetaContact);

        ContactGroup parentProtoGroup = resolveProtoPath(contact
            .getProtocolProvider(), (MetaContactGroupImpl) newParentGroup);

        //if the contact is not currently in the proto group corresponding to
        //its new metacontact group parent then move it
        if(contact.getParentContactGroup() != parentProtoGroup && opSetPresence != null)
            opSetPresence.moveContactToGroup(contact, parentProtoGroup);

        ( (MetaContactImpl) newParentMetaContact).addProtoContact(contact);

        //fire an event telling everyone that contact has been added to its new
        //parent.
        fireProtoContactEvent(contact,
                              ProtoContactEvent.PROTO_CONTACT_MOVED,
                              currentParentMetaContact,
                              newParentMetaContact,
                              null);

        if (currentParentMetaContact != null)
        {
            // If this was the last contact in the meta contact - remove it.
            // It is true that in some cases the move would be followed by some
            // kind of protocol provider events indicating the change which on
            // its turn may trigger the removal of empty meta contacts. Yet in
            // many cases particularly if parent groups were not changed in
            // the protocol contact list no event would come and the meta
            // contact will remain empty that's why we delete it here and if
            // an event follows it would simply be ignored.
            if (currentParentMetaContact.getContactCount() == 0)
            {
                MetaContactGroupImpl parentMetaGroup =
                    currentParentMetaContact.getParentGroup();

                if (parentMetaGroup != null)
                    parentMetaGroup.removeMetaContact(currentParentMetaContact);

                fireMetaContactEvent(currentParentMetaContact, parentMetaGroup
                    , MetaContactEvent.META_CONTACT_REMOVED, null);
            }
        }
    }

    /**
     * Moves the specified <tt>MetaContact</tt> to <tt>newGroup</tt>.
     *
     * @param metaContact
     *            the <tt>MetaContact</tt> to move.
     * @param newMetaGroup
     *            the <tt>MetaContactGroup</tt> that should be the new parent
     *            of <tt>contact</tt>.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     * @throws IllegalArgumentException if <tt>newMetaGroup</tt> or
     * <tt>metaContact</tt> do not come from this implementation.
     */
    @Override
    public void moveMetaContact(MetaContact metaContact,
                                MetaContactGroup newMetaGroup) throws
        MetaContactListException, IllegalArgumentException
    {
        contactLogger.debug(metaContact, "moveMetaContact");
        if (! (newMetaGroup instanceof MetaContactGroupImpl))
        {
            throw new IllegalArgumentException(newMetaGroup
                                               +
                                               " is not a MetaContactGroupImpl instance");
        }

        if (! (metaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(metaContact
                                               +
                                               " is not a MetaContactImpl instance");
        }

        Iterator<Contact> contacts = metaContact.getContacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();
            if (!contact.getParentContactGroup().canContainSubgroups())
            {
                // We can't move this contact as some of the sub contacts don't
                // support groups.  Tell the user so that they can sort it out.
                throw new MetaContactListException(
                         "Sub-contact doesn't support groups",
                         MetaContactListException.CODE_NOT_SUPPORTED_OPERATION);
            }
        }

        //first remove the meta contact from its current parent:
        MetaContactGroupImpl currentParent
            = (MetaContactGroupImpl) findParentMetaContactGroup(metaContact);

        if (currentParent != null)
            currentParent.removeMetaContact( (MetaContactImpl) metaContact);

        ( (MetaContactGroupImpl) newMetaGroup).addMetaContact(
            (MetaContactImpl) metaContact);

        try
        {
            //first make sure that the new meta contact group path is resolved
            //against all protocols that the MetaContact requires. then move
            //the meta contact in there and move all proto contacts inside it.
            contacts = metaContact.getContacts();

            while (contacts.hasNext())
            {
                Contact protoContact =  contacts.next();

                ContactGroup protoGroup = resolveProtoPath(protoContact
                    .getProtocolProvider(), (MetaContactGroupImpl) newMetaGroup);

                //get a persistent or non persistent presence operation set
                OperationSetPersistentPresence opSetPresence
                    = protoContact
                        .getProtocolProvider()
                            .getOperationSet(
                                OperationSetPersistentPresence.class);

                if (opSetPresence == null)
                {
                    /** @todo handle non persistent presence operation sets */
                }
                else
                {
                    opSetPresence.moveContactToGroup(protoContact, protoGroup);
                }
            }
        }
        catch (Exception ex)
        {
            logger.error("Cannot move contact", ex);

            // now move the contact to previous parent
            ((MetaContactGroupImpl)newMetaGroup).
                removeMetaContact( (MetaContactImpl) metaContact);

            currentParent.addMetaContact((MetaContactImpl) metaContact);

            throw new MetaContactListException(ex.getMessage(),
                MetaContactListException.CODE_MOVE_CONTACT_ERROR);
        }

        // Fire the moved event.
        fireMetaContactEvent(new MetaContactMovedEvent(
                                    metaContact, currentParent, newMetaGroup),
                                        null);
    }

    /**
     * Deletes the specified contact from both the local contact list and (if
     * applicable) the server stored contact list if supported by the
     * corresponding protocol.
     *
     * @param contact the contact to remove.
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    @Override
    public void removeContact(Contact contact) throws MetaContactListException
    {
        contactLogger.debug(contact, "removeContact");

        //remove the contact from the provider and do nothing else
        //updating and/or removing the corresponding meta contact would happen
        //once a confirmation event is received from the underlying protocol
        //provider
        OperationSetPresence opSetPresence
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetPresence.class);

        //in case the provider only has a persistent operation set:
        if (opSetPresence == null)
        {
            opSetPresence =
                (OperationSetPresence) contact.getProtocolProvider()
                    .getOperationSet(OperationSetPersistentPresence.class);

            if (opSetPresence == null)
            {
                throw new IllegalStateException(
                    "Cannot remove a contact from a provider with no presence "
                    + "operation set.");
            }
        }

        try
        {
            opSetPresence.unsubscribe(contact);
        }
        catch (Exception ex)
        {
            String errorTxt = "Failed to remove "
                    + contact + " from its protocol provider. ";

            if(ex instanceof OperationFailedException)
                errorTxt += ex.getMessage();

            throw new MetaContactListException(errorTxt,
                                               ex,
                                               MetaContactListException.
                                               CODE_NETWORK_ERROR);
        }
    }

    /**
     * Removes a listener previously added with <tt>addContactListListener</tt>.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeMetaContactListListener(
        MetaContactListListener listener)
    {
        logger.info("Removing MCL listener " + listener);

        synchronized (metaContactListListeners)
        {
            this.metaContactListListeners.remove(listener);
        }
    }

    /**
     * Removes the specified <tt>metaContact</tt> as well as all of its
     * underlying contacts.
     *
     * @param metaContact
     *            the metaContact to remove.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    @Override
    public void removeMetaContact(MetaContact metaContact) throws
        MetaContactListException
    {
        contactLogger.debug(metaContact, "removeMetaContact");
        Iterator<Contact> protoContactsIter = metaContact.getContacts();

        if (metaContact.getContactCount() == 0)
        {
            contactLogger.error(metaContact, "Asked to remove MetaContact " +
                                             "with no child contacts");

            MetaContactGroupImpl metaContactGroup =
                 (MetaContactGroupImpl) metaContact.getParentMetaContactGroup();

            if (metaContactGroup == null)
            {
                // We've really got confused - refresh the contact list as there
                // is nothing else we can do.
                contactLogger.error(metaContact, "Contact has no parent group");
                UIService uiService = ContactlistActivator.getUIService();

                if (uiService != null)
                {
                    uiService.reloadContactList();
                }
            }
            else
            {
                metaContactGroup.removeMetaContact(metaContact);

                fireMetaContactEvent(metaContact,
                                     metaContact.getParentMetaContactGroup(),
                                     MetaContactEvent.META_CONTACT_REMOVED,
                                     null);
            }
        }

        while (protoContactsIter.hasNext())
        {
            removeContact( protoContactsIter.next());
        }

        //do not fire events. that will be done by the contact listener as soon
        //as it gets confirmation events of proto contact removal

        //the removal of the last contact would also generate an even for the
        //removal of the meta contact itself.
    }

    /**
     * Removes the specified meta contact group, all its corresponding protocol
     * specific groups and all their children.
     *
     * @param groupToRemove
     *            the <tt>MetaContactGroup</tt> to have removed.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    @Override
    public void removeMetaContactGroup(
        MetaContactGroup groupToRemove) throws MetaContactListException
    {
        if (! (groupToRemove instanceof MetaContactGroupImpl))
        {
            throw new IllegalArgumentException(
                groupToRemove
                + " is not an instance of MetaContactGroupImpl");
        }

        try
        {
            //remove all proto groups and then remove the meta group as well.
            Iterator<ContactGroup> protoGroups
                = groupToRemove.getContactGroups();

            while (protoGroups.hasNext())
            {
                ContactGroup protoGroup = protoGroups.next();

                OperationSetPersistentPresence opSetPersPresence
                    = protoGroup
                        .getProtocolProvider()
                            .getOperationSet(
                                OperationSetPersistentPresence.class);

                if (opSetPersPresence == null)
                {
                    /** @todo handle removal of non persistent proto groups */
                    return;
                }

                opSetPersPresence.removeServerStoredContactGroup(protoGroup);
            }
        }
        catch(Exception ex)
        {
            throw new MetaContactListException(ex.getMessage(),
                MetaContactListException.CODE_REMOVE_GROUP_ERROR);
        }

        MetaContactGroupImpl parentMetaGroup = (MetaContactGroupImpl)
            findParentMetaContactGroup(groupToRemove);

        parentMetaGroup.removeSubgroup(groupToRemove);

        fireMetaContactGroupEvent( groupToRemove, null, null
            , MetaContactGroupEvent.META_CONTACT_GROUP_REMOVED, null);
    }

    /**
     * Removes the protocol specific group from the specified meta contact group
     * and removes from meta contacts all proto contacts that belong to the
     * same provider as the group which is being removed.
     * @param metaContainer the MetaContactGroup that we'd like to remove a
     * contact group from.
     * @param groupToRemove the ContactGroup that we'd like removed.
     * @param sourceProvider the ProtocolProvider that the contact group belongs
     * to.
     */
    public void removeContactGroupFromMetaContactGroup(
        MetaContactGroupImpl metaContainer,
        ContactGroup groupToRemove,
        ProtocolProviderService sourceProvider)
    {
        // if we failed to find the metagroup corresponding to proto group
        if(metaContainer == null)
        {
            return;
        }

        /*
         * Go through all meta contacts and remove all contacts that belong to
         * the same provider and are therefore children of the group that is
         * being removed.
         */
        locallyRemoveAllContactsForProvider(metaContainer, groupToRemove);

        fireMetaContactGroupEvent(
            metaContainer,
            sourceProvider,
            groupToRemove,
            MetaContactGroupEvent.CONTACT_GROUP_REMOVED_FROM_META_GROUP, null);
    }

    /**
     * Removes local resources storing copies of the meta contact list. This
     * method is meant primarily to aid automated testing which may depend on
     * beginning the tests with an empty local contact list.
     */
    @Override
    public void purgeLocallyStoredContactListCopy()
    {
        this.storageManager.storeContactListAndStopStorageManager();
        this.storageManager.removeContactListFile();
        logger.trace("Removed meta contact list storage file.");
    }

    /**
     * Goes through the specified group and removes from all meta contacts,
     * protocol specific contacts belonging to the specified
     * <tt>groupToRemove</tt>. Note that this method won't undertake any calls
     * to the protocol itself as it is used only to update the local contact
     * list as a result of a server generated event.
     *
     * @param parentMetaGroup  the MetaContactGroup whose children we should go
     * through
     * @param groupToRemove the proto group that we want removed together with
     * its children.
     */
    private void locallyRemoveAllContactsForProvider(
                        MetaContactGroupImpl parentMetaGroup,
                        ContactGroup         groupToRemove)
    {
        logger.info("Locally remove all contacts from group " + (groupToRemove != null ? groupToRemove.getGroupName() : "null"));

        Iterator<MetaContact> childrenContactsIter = parentMetaGroup.getChildContacts();

        //first go through all direct children.
        while (childrenContactsIter.hasNext())
        {
            MetaContactImpl child
                = (MetaContactImpl) childrenContactsIter.next();

            //Get references to all contacts that will be removed in case we
            //need to fire an event.
            Iterator<Contact> contactsToRemove
                = child.getContactsForContactGroup(groupToRemove);

            child.removeContactsForGroup(groupToRemove);

            //if this was the last proto contact inside this meta contact,
            //then remove the meta contact as well. Otherwise only fire an
            //event.
            if (child.getContactCount() == 0)
            {
                parentMetaGroup.removeMetaContact(child);
                fireMetaContactEvent(child, parentMetaGroup
                                     , MetaContactEvent.META_CONTACT_REMOVED
                                     , null);
            }
            else
            {
                // there are other proto contacts left in the contact child
                //meta contact so we'll have to send an event for each of the
                //removed contacts and not only a single event for the whole
                //meta contact.
                while (contactsToRemove.hasNext())
                {
                    fireProtoContactEvent(
                          contactsToRemove.next()
                        , ProtoContactEvent.PROTO_CONTACT_REMOVED
                        , child
                        , null
                        , null);
                }
            }
        }

        Iterator<MetaContactGroup> subgroupsIter
            = parentMetaGroup.getSubgroups();

        //then go through all subgroups.
        while (subgroupsIter.hasNext())
        {
            MetaContactGroupImpl subMetaGroup
                = (MetaContactGroupImpl) subgroupsIter.next();

            Iterator<ContactGroup> contactGroups
                = subMetaGroup.getContactGroups();

            ContactGroup protoGroup = null;
            while(contactGroups.hasNext())
            {
                protoGroup = contactGroups.next();
                if(groupToRemove == protoGroup.getParentContactGroup())
                    this.locallyRemoveAllContactsForProvider(
                            subMetaGroup, protoGroup);
            }

            //remove the group if there are no children left.
            if(subMetaGroup.countSubgroups() == 0
               && subMetaGroup.countChildContacts() == 0
               && subMetaGroup.countContactGroups() == 0)
            {
                parentMetaGroup.removeSubgroup(subMetaGroup);
                fireMetaContactGroupEvent(
                    subMetaGroup
                    , groupToRemove.getProtocolProvider()
                    , protoGroup
                    , MetaContactGroupEvent.META_CONTACT_GROUP_REMOVED
                    , null);
            }
        }

        parentMetaGroup.removeProtoGroup(groupToRemove);
    }

    /**
     * Returns the MetaContactGroup corresponding to the specified contactGroup
     * or null if no such MetaContactGroup was found.
     * @return the MetaContactGroup corresponding to the specified contactGroup
     * or null if no such MetaContactGroup was found.
     * @param contactGroup
     *            the protocol specific <tt>contactGroup</tt> that we're looking
     *            for.
     */
    @Override
    public MetaContactGroup findMetaContactGroupByContactGroup
        (ContactGroup contactGroup)
    {
        return rootMetaGroup.findMetaContactGroupByContactGroup(contactGroup);
    }

    /**
     * Returns the MetaContact containing the specified contact or null if no
     * such MetaContact was found. The method can be used when for example we
     * need to find the MetaContact that is the author of an incoming message
     * and the corresponding ProtocolProviderService has only provided a
     * <tt>Contact</tt> as its author.
     *
     * @param contact the protocol specific <tt>contact</tt> that we're looking
     *  for.
     *
     * @return the MetaContact containing the specified contact or null if no
     *         such contact is present in this contact list.
     */
    @Override
    public MetaContact findMetaContactByContact(Contact contact)
    {
        return rootMetaGroup.findMetaContactByContact(contact);
    }

    @Override
    public MetaContact findMetaContactByContact(String contactAddress,
                                                String accountID)
    {
        return rootMetaGroup.findMetaContactByContact(contactAddress,
                                                      accountID);
    }

    @Override
    public List<MetaContact> findMetaContactByNumber(String phoneNumber)
    {
        return rootMetaGroup.findMetaContactByNumber(phoneNumber);
    }

    @Override
    public List<MetaContact> findMetaContactsByDetails(ArrayList<GenericDetail> details)
    {
        return rootMetaGroup.findMetaContactsByDetails(details);
    }

    @Override
    public List<MetaContact> findMetaContactByEmail(String emailAddress)
    {
        return rootMetaGroup.findMetaContactByEmail(emailAddress);
    }

    /**
     * Returns the MetaContact that corresponds to the specified metaContactID.
     *
     * @param metaContactID
     *            a String identifier of a meta contact.
     * @return the MetaContact with the specified string identifier or null if
     *         no such meta contact was found.
     */
    @Override
    public MetaContact findMetaContactByMetaUID(String metaContactID)
    {
        return rootMetaGroup.findMetaContactByMetaUID(metaContactID);
    }

    /**
     * Returns the MetaContactGroup that corresponds to the specified
     * metaGroupID.
     *
     * @param metaGroupID
     *            a String identifier of a meta contact group.
     * @return the MetaContactGroup with the specified string identifier or null
     *          if no such meta contact was found.
     */
    @Override
    public MetaContactGroup findMetaContactGroupByMetaUID(String metaGroupID)
    {
        return rootMetaGroup.findMetaContactGroupByMetaUID(metaGroupID);
    }

    /**
     * Returns a list of all <tt>MetaContact</tt>s containing a protocol contact
     * from the given <tt>ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> whose
     * contacts we're looking for.
     * @return a list of all <tt>MetaContact</tt>s containing a protocol contact
     * from the given <tt>ProtocolProviderService</tt> or all contacts if the
     * protocol provider was null.
     */
    @Override
    public Iterator<MetaContact> findAllMetaContactsForProvider(
                                    ProtocolProviderService protocolProvider)
    {
        List<MetaContact> resultList = new ArrayList<>();

        this.findAllMetaContactsForProvider(protocolProvider,
                                            rootMetaGroup,
                                            resultList);

        return resultList.iterator();
    }

    /**
     * Returns a list of all <tt>MetaContact</tt>s contained in the given group
     * and containing a protocol contact from the given
     * <tt>ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> whose
     * contacts we're looking for.
     * @param metaContactGroup the parent group.
     *
     * @return a list of all <tt>MetaContact</tt>s containing a protocol contact
     * from the given <tt>ProtocolProviderService</tt>.
     */
    @Override
    public Iterator<MetaContact> findAllMetaContactsForProvider(
        ProtocolProviderService protocolProvider,
        MetaContactGroup metaContactGroup)
    {
        List<MetaContact> resultList = new LinkedList<>();

        this.findAllMetaContactsForProvider(protocolProvider,
            metaContactGroup, resultList);

        return resultList.iterator();
    }

    /**
     * Returns a list of all <tt>MetaContact</tt>s contained in the given group
     * and containing a protocol contact from the given
     * <tt>ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> whose
     * contacts we're looking for.
     * @param metaContactGroup the parent group.
     * @param resultList the list containing the result of the search.
     */
    private void findAllMetaContactsForProvider(
        ProtocolProviderService protocolProvider,
        MetaContactGroup metaContactGroup,
        List<MetaContact> resultList)
    {
        Iterator<MetaContact> childContacts
            = metaContactGroup.getChildContacts();

        while (childContacts.hasNext())
        {
            MetaContact metaContact = childContacts.next();

            if (protocolProvider == null)
            {
                resultList.add(metaContact);
            }
            else
            {
                Iterator<Contact> protocolContacts
                    = metaContact.getContactsForProvider(protocolProvider);

                if (protocolContacts.hasNext())
                {
                    resultList.add(metaContact);
                }
            }
        }

        Iterator<MetaContactGroup> subGroups
            = metaContactGroup.getSubgroups();

        while (subGroups.hasNext())
        {
            MetaContactGroup subGroup = subGroups.next();

            Iterator<ContactGroup> protocolSubgroups = protocolProvider == null?
                 subGroup.getContactGroups() :
                 subGroup.getContactGroupsForProvider(protocolProvider);

            if (protocolSubgroups.hasNext())
            {
                this.findAllMetaContactsForProvider(protocolProvider,
                                                    subGroup,
                                                    resultList);
            }
        }
    }

    @Override
    public MetaContact findMetaContactForSmsNumber(String smsNumber)
    {
        MetaContact smsMetaContact = null;
        List<MetaContact> matchedContacts = findMetaContactByNumber(smsNumber);

        if ((matchedContacts != null) && (matchedContacts.size() == 1))
        {
            smsMetaContact = matchedContacts.get(0);
        }
        return smsMetaContact;
    }

    /**
     * Goes through the server stored ContactList of the specified operation
     * set, retrieves all protocol specific contacts it contains and makes sure
     * they are all present in the local contact list.
     *
     * @param presenceOpSet
     *            the presence operation set whose contact list we'd like to
     *            synchronize with the local contact list.
     */
    private void synchronizeOpSetWithLocalContactList(
        OperationSetPersistentPresence presenceOpSet)
    {
        try
        {
            ContactGroup rootProtoGroup = presenceOpSet
                .getServerStoredContactListRoot();

            if (rootProtoGroup != null)
            {
                logger.trace("subgroups: "
                             + rootProtoGroup.countSubgroups());
                logger.trace("child contacts: "
                             + rootProtoGroup.countContacts());

                addContactGroupToMetaGroup(rootProtoGroup, rootMetaGroup, true);
                logger.debug("Loaded contacts from local contact list");
            }
        }
        finally
        {
            // Even if something goes wrong loading the contacts from store,
            // make sure we add the listeners that we need:
            logger.info("Adding contact listeners to " + presenceOpSet);
            if (presenceOpSet != null)
            {
                presenceOpSet
                    .addSubscriptionListener(clSubscriptionEventHandler);

                presenceOpSet
                    .addServerStoredGroupChangeListener(clGroupEventHandler);

                presenceOpSet.finishedLoadingUnresolvedContacts();
            }
        }
    }

    /**
     * Creates meta contacts and meta contact groups for all children of the
     * specified <tt>contactGroup</tt> and adds them to <tt>metaGroup</tt>
     * @param protoGroup the <tt>ContactGroup</tt> to add.
     * <p>
     * @param metaGroup the <tt>MetaContactGroup</tt> where <tt>ContactGroup</tt>
     * should be added.
     * @param fireEvents indicates whether or not events are to be fired upon
     * adding subcontacts and subgroups. When this method is called recursively,
     * the parameter should will be false in order to generate a minimal number
     * of events for the whole addition and not an event per every subgroup
     * and child contact.
     */
    private void addContactGroupToMetaGroup(ContactGroup protoGroup,
                                            MetaContactGroupImpl metaGroup,
                                            boolean fireEvents)
    {
        contactLogger.debug("Adding contact group " + protoGroup.getGroupName() +
                     " to meta group " + metaGroup.getGroupName());

        // first register the root group
        metaGroup.addProtoGroup(protoGroup);

        // register subgroups and contacts
        Iterator<ContactGroup> subgroupsIter = protoGroup.subgroups();

        while (subgroupsIter.hasNext())
        {
            ContactGroup group = subgroupsIter.next();

            //continue if we have already loaded this group from the locally
            //stored contact list.
            if(metaGroup.findMetaContactGroupByContactGroup(group) != null)
                continue;

            // right now we simply map this group to an existing one
            // without being cautious and verify whether we already have it
            // registered
            MetaContactGroupImpl newMetaGroup
                = new MetaContactGroupImpl(this, group.getGroupName());

            metaGroup.addSubgroup(newMetaGroup);

            addContactGroupToMetaGroup(group, newMetaGroup, false);

            if (fireEvents)
            {
                this.fireMetaContactGroupEvent(
                        newMetaGroup
                        , group.getProtocolProvider()
                        , group
                        , MetaContactGroupEvent.
                        META_CONTACT_GROUP_ADDED
                        , null);
            }
        }

        // now add all contacts, located in this group
        Iterator<Contact> contactsIter = protoGroup.contacts();
        while (contactsIter.hasNext())
        {
            Contact contact = contactsIter.next();

            //continue if we have already loaded this contact from the locally
            //stored contact list.
            if(metaGroup.findMetaContactByContact(contact) != null)
                continue;

            MetaContactImpl newMetaContact = new MetaContactImpl();

            newMetaContact.addProtoContact(contact);

            // Check whether this contact should be hidden in the contact list
            // by examining the persistent data of the protoContact.
            String persistentData = contact.getPersistentData();

            if (persistentData != null)
            {
                String[] values = persistentData.split(";");
                for (String value : values)
                {
                    String data[] = value.split("=");
                    String detailName = MetaContact.IS_CONTACT_HIDDEN;
                    if (data.length < 2)
                    {
                        continue;
                    }

                    if (data[0].equals(detailName) &&
                        Boolean.parseBoolean(data[1]))
                    {
                        // Protocol contact states whether this MetaContact
                        // should be hidden so set the MetaData for this
                        // MetaContact
                        List<String> details =
                                          newMetaContact.getDetails(detailName);

                        if (details.size() == 0)
                        {
                            // No existing detail so wasn't hidden, add a
                            // detail
                            newMetaContact.addDetail(detailName,
                                                     String.valueOf(true));
                        }
                        else
                        {
                            // There is a detail - set the new value to be true
                            String oldValue = details.get(0);
                            newMetaContact.changeDetail(detailName,
                                                        oldValue,
                                                        String.valueOf(true));
                        }
                    }
                }
            }

            contactLogger.debug(newMetaContact,
                       "Added new MetaContact for " + contact.getDisplayName());
            metaGroup.addMetaContact(newMetaContact);

            if (fireEvents)
            {
                try
                {
                    fireMetaContactEvent(newMetaContact,
                                         metaGroup,
                                         MetaContactEvent.META_CONTACT_ADDED,
                                         null);
                }
                catch (Exception e)
                {
                    // Not much we can do except log and continue
                    contactLogger.error(newMetaContact,
                                        "Uncaught exception adding contact",
                                        e);
                }
            }
        }
    }

    /**
     * Adds the specified provider to the list of currently known providers. In
     * case the provider supports persistent presence the method would also
     * extract all contacts and synchronize them with the local contact list.
     * Otherwise it would start a process where local contacts would be added on
     * the server.
     *
     * @param provider the ProtocolProviderService that we've just detected.
     */
    private synchronized void handleProviderAdded(@NotNull ProtocolProviderService provider)
    {
        logger.info("Adding protocol provider "
                    + provider.getAccountID().getAccountUniqueID());

        // check whether the provider has a persistent presence op set
        OperationSetPersistentPresence opSetPersPresence =
            provider.getOperationSet(OperationSetPersistentPresence.class);

        this.currentlyInstalledProviders.put(
                           provider.getAccountID().getAccountUniqueID(),
                           provider);

        //If we have a persistent presence op set - then retrieve its contact
        //list and merge it with the local one.
        if (opSetPersPresence != null)
        {
            //load contacts, stored in the local contact list and corresponding to
            //this provider.
            try
            {
                // Synchronize on the provider to prevent multiple threads from
                // loading contacts for this provider at the same time
                synchronized (provider)
                {
                    storageManager.extractContactsForAccount(
                        provider.getAccountID().getAccountUniqueID());
                }

                logger.debug("All contacts loaded for account "
                               + provider.getAccountID().getAccountUniqueID());
            }
            catch (XMLException exc)
            {
                logger.error("Failed to load contacts for account "
                             + provider.getAccountID().getAccountUniqueID(), exc);
            }
            synchronizeOpSetWithLocalContactList(opSetPersPresence);
        }
        else
        {
            logger.debug("Service did not have a pers. pres. op. set.");
        }

        /** @todo implement handling non persistent presence operation sets */

        //add a presence status listener so that we could reorder contacts upon
        //status change. NOTE that we MUST NOT add the presence listener before
        //extracting the locally stored contact list or  otherwise we'll get
        //events for all contacts that we have already extracted
        if(opSetPersPresence != null)
            opSetPersPresence.addContactPresenceStatusListener(this);

        // Check if the capabilities operation set is available for this
        // contact and add a listener to it in order to track capabilities'
        // changes for all contained protocol contacts.
        OperationSetContactCapabilities capOpSet
            = provider.getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet != null)
        {
            capOpSet.addContactCapabilitiesListener(this);
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the contacts that it has registered locally.
     *
     * @param provider
     *            the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(
        ProtocolProviderService provider)
    {
        logger.info("Removing protocol provider "
                    + provider.getProtocolName());

        this.currentlyInstalledProviders.
            remove(provider.getAccountID().getAccountUniqueID());

        //get the root group for the provider so that we could remove it.
        OperationSetPersistentPresence persPresOpSet =
            provider.getOperationSet(OperationSetPersistentPresence.class);

        //ignore if persistent presence is not supported.
        if(persPresOpSet != null)
        {
            //we don't care about subscription and presence status events here
            // any longer.
            persPresOpSet.removeContactPresenceStatusListener(this);
            persPresOpSet.removeSubscriptionListener(
                clSubscriptionEventHandler);
            persPresOpSet.removeServerStoredGroupChangeListener(
                clGroupEventHandler);

            ContactGroup rootGroup
                = persPresOpSet.getServerStoredContactListRoot();

            //iterate all sub groups and remove them one by one
            //(we don't simply remove the root group because the mcl storage
            // manager is stupid (i wrote it) and doesn't know root groups exist.
            // that's why it needs to hear an event for every single group.)
            Iterator<ContactGroup> subgroups = rootGroup.subgroups();

            while(subgroups.hasNext())
            {
                ContactGroup group = subgroups.next();
                //remove the group
                this.removeContactGroupFromMetaContactGroup(
                    (MetaContactGroupImpl) findMetaContactGroupByContactGroup(
                                                                        group),
                    group,
                    provider);
            }

            //remove the root group
            this.removeContactGroupFromMetaContactGroup(
                this.rootMetaGroup, rootGroup, provider);
        }

        // Check if the capabilities operation set is available for this
        // contact and remove previously added listeners.
        OperationSetContactCapabilities capOpSet
            = provider.getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet != null)
            capOpSet.removeContactCapabilitiesListener(this);
    }

    /**
     * Registers <tt>group</tt> to the event ignore list. This would make the
     * method that is normally handling events for newly created groups ignore
     * any events for that particular group and leave the responsibility to the
     * method that added the group to the ignore list.
     *
     * @param group the name of the group that we'd like to
     * register.
     * @param ownerProvider the protocol provider that we expect the addition
     * to come from.
     */
    private void addGroupToEventIgnoreList(
        String group,
        ProtocolProviderService ownerProvider)
    {
        if (group == null)
        {
            // Just because the name is null is no reason to give up - but it
            // shouldn't happen so log
            logger.warn("Passed null group name to add");
            group = "null";
        }

        //first check whether registrations in the ignore list already
        //exist for this group.
        if (isGroupInEventIgnoreList(group, ownerProvider))
        {
            return;
        }

        List<ProtocolProviderService> existingProvList
                                        = this.groupEventIgnoreList.get(group);

        if (existingProvList == null)
        {
            existingProvList = new LinkedList<>();
        }

        existingProvList.add(ownerProvider);
        groupEventIgnoreList.put(group, existingProvList);
    }

    /**
     * Verifies whether the specified group is in the group event ignore list.
     * @return true if the group is in the group event ignore list and false
     * otherwise.
     * @param group the group whose presence in the ignore list we'd like to
     * verify.
     * @param ownerProvider the provider that <tt>group</tt> belongs to.
     */
    private boolean isGroupInEventIgnoreList(
        String group, ProtocolProviderService ownerProvider)
    {
        if (group == null)
        {
            // Just because the name is null is no reason to give up - but it
            // shouldn't happen so log
            logger.warn("Passed null group name to check");
            group = "null";
        }

        List<ProtocolProviderService> existingProvList =
            this.groupEventIgnoreList.get(group);

        return existingProvList != null
            && existingProvList.contains(ownerProvider);
    }

    /**
     * Removes the <tt>group</tt> from the group event ignore list so that
     * events concerning this group get treated.
     *
     * @param group the group whose that we'd want out of the ignore list.
     * @param ownerProvider the provider that <tt>group</tt> belongs to.
     */
    private void removeGroupFromEventIgnoreList(
        String group, ProtocolProviderService ownerProvider)
    {
        if (group == null)
        {
            // Just because the name is null is no reason to give up - but it
            // shouldn't happen so log
            logger.warn("Passed null group name to remove");
            group = "null";
        }

        //first check whether the registration actually exists.
        if (!isGroupInEventIgnoreList(group, ownerProvider))
        {
            return;
        }

        List<ProtocolProviderService> existingProvList
                                    = this.groupEventIgnoreList.get(group);

        if (existingProvList.size() < 1)
        {
            groupEventIgnoreList.remove(group);
        }
        else
        {
            existingProvList.remove(ownerProvider);
        }
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and modifies
     * the list of registered protocol providers accordingly.
     *
     * @param event
     *            The <tt>ServiceEvent</tt> object.
     */
    @Override
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = bundleContext.getService(event
            .getServiceReference());

        logger.trace("Received a service event for: "
                     + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        logger.debug("Service is a protocol provider.");

        ProtocolProviderService provider =
            (ProtocolProviderService)sService;
        //first check if the event really means that the accounts is
        //uninstalled/installed (or is it just stopped ... e.g. we could be
        // shutting down, or in the other case it could be just modified) ...
        // before that however, we'd need to get a reference to the service.
        ProtocolProviderFactory sourceFactory = null;

        ServiceReference<?>[] allBundleServices
            = event.getServiceReference().getBundle()
                .getRegisteredServices();

        for (ServiceReference<?> bundleServiceRef : allBundleServices)
        {
            Object service = bundleContext.getService(bundleServiceRef);
            if(service instanceof ProtocolProviderFactory)
            {
                sourceFactory = (ProtocolProviderFactory) service;
                break;
            }
        }

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            logger.debug("Handling registration of a new Protocol Provider.");
            // if we have the PROVIDER_MASK property set, make sure that this
            // provider has it and if not ignore it.
            String providerMask = System
                .getProperty(MetaContactListService.PROVIDER_MASK_PROPERTY);
            if (providerMask != null
                && providerMask.trim().length() > 0)
            {
                String servRefMask = (String) event
                    .getServiceReference()
                    .getProperty(
                        MetaContactListService.PROVIDER_MASK_PROPERTY);

                if (servRefMask == null
                    || !servRefMask.equals(providerMask))
                {
                    logger.debug("Ignoring masked provider: "
                                    + provider.getAccountID());
                    return;
                }
            }

            if(sourceFactory != null
               && currentlyInstalledProviders.containsKey(
                               provider.getAccountID().getAccountUniqueID()))
            {
                logger.debug("An already installed account: "
                            + provider.getAccountID() + ". Modifying it.");
                // the account is already installed and this event is coming
                // from a modification. we don't return here as
                // the account is removed and added again and we must
                // create its unresolved contact and give him a chance to resolve
                // them and not fire new subscription to duplicate the already
                // existing.

                //return;
            }

            this.handleProviderAdded( (ProtocolProviderService) sService);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            if(sourceFactory == null)
            {
                //strange ... we must be shutting down. just bail
                return;
            }

            AccountID accountID = provider.getAccountID();

            // If the account is still registered or is just unloaded but
            // remains stored we remove its contacts but without storing this
            if(ContactlistActivator
                    .getAccountManager()
                        .getStoredAccounts()
                            .contains(accountID))
            {
                logger.debug("Account still installed " + provider.getAccountID());

                //the account is still installed it means we are modifying it.
                // we remove all its contacts from current contactlist
                // but remove the storage manager in order to avoid
                // losing those contacts from the storage
                // as its modification later unresolved contacts will be created
                // which will be resolved from the already modified account
                synchronized(this)
                {
                    this.removeMetaContactListListener(storageManager);
                    this.handleProviderRemoved(
                        (ProtocolProviderService)sService);
                    this.addMetaContactListListener(storageManager);
                }
            }
            else
            {
                logger.debug("Account uninstalled. acc.id="
                         +provider.getAccountID() +". Removing from meta "
                         +"contact list.");
                this.handleProviderRemoved( (ProtocolProviderService) sService);
            }
        }
    }

    /**
     * The class handles the list of pending <tt>Contact</tt>s and
     * <tt>PendingContactListener</tt>s.
     *
     * When a pending contact is created, we call the registered pending
     * contact listener.
     */
    private static class PendingContactList
    {
        /**
         * This class listens for acknowledgement that a contact added on the client
         * has been acknowledged on the server. Instances should be registered with the
         * <tt>PendingContactList</tt> for the contact they are waiting for.
         */
        private static class Listener
        {
            /**
             * Pending contact address
             */
            private final String contactID;

            /**
             * Subscription Event that caused the acknowledgement
             */
            private SubscriptionEvent evt;

            /**
             * Protocol Provider Service for this contact
             */
            private ProtocolProviderService provider;

            /**
             * Resulting server stored contact
             */
            private Contact sourceContact;

            public Listener(ProtocolProviderService pps, String address)
            {
                contactID = address;
                provider = pps;
            }

            public ProtocolProviderService provider()
            {
                return provider;
            }

            public String contactID()
            {
                return contactID;
            }

            /**
             * Wait for the subscription event which acknowledges the contact
             * to occur
             * @param millis
             */
            public synchronized void waitForEvent(long millis)
            {
                // Only wait for the event if it's not already occurred.
                if (evt == null)
                {
                    try
                    {
                        wait(millis);
                    }
                    catch (InterruptedException e)
                    {
                        logger.error(
                            "Interrupted while waiting for contact creation"
                            , e);
                    }
                }
            }

            /**
             * Called by the <tt>PendingContactList</tt> to inform that
             * a matching subscription event has occurred.
             *
             * Note, the contact listener must verify that the subscription
             * event matches the desired contact. It should return true if the
             * contact has been handled, or false if the contact doesn't apply
             * to this listener.
             */
            public synchronized boolean pendingContact(SubscriptionEvent event)
            {
                if (event.getSourceContact().getAddress().equals(contactID) ||
                    event.getSourceContact().equals(contactID))
                {
                    logger.debug("Handling contact: " + event.getSourceContact());

                    this.evt = event;
                    this.sourceContact = event.getSourceContact();
                    this.notifyAll();

                    return true;
                }

                return false;
            }
        }

        /**
         * Outstanding contacts we are waiting to hear from the server about.
         */
        private final Map<ProtocolProviderService, Map<String, Listener>>
            outstanding =
                new HashMap<>();

        /**
         * This keeps a list of outstanding contacts which have been added by the
         * client. The objects are MetaContact objects paired with a corresponding contactID.
         * This contactID is used by us and the server to
         * correlate updates to contacts that we receive on our listeners.
         *
         * Synchronisation policy: all accesses of this Set must be guarded
         * using client-side locking.
         */
        public static final Set<MetaContactAndContactID> metaContactToContactIdSet =
            new HashSet<>();

        /**
         * Listen for a pending contact to be acknowledged by the server.
         *
         * Note, the contact acts as a key, it's not used for checking whether a added contact matches
         * as the server and client name for the contact may not match.
         *
         * @param contact Contact to listen for.
         * @param pps ProtocolProviderService involved
         */
        public synchronized Listener addListener(ProtocolProviderService pps, String contact)
        {
            Listener listener = new Listener(pps, contact);

            logger.debug("Listening for contact: " + contact +
                         " with provider " + pps +
                         " and listener: " + listener);

            Map<String, Listener> providerContacts = outstanding.computeIfAbsent(
                    pps,
                    k -> new HashMap<>());

            if (providerContacts.containsKey(contact))
            {
                logger.warn("Contact: " + contact +
                            " with provider " + pps +
                            " was already pending with a different listener: " +
                            providerContacts.get(contact));
            }

            providerContacts.put(contact, listener);
            return listener;
        }

        /**
         * Handle a contact being added on the server.
         *
         * This method should return true if the contact was a pending add on the client, and
         * has been handled as such, or should return false if it wasn't.
         *
         * @param evt Subscription event indicating a new contact was added.
         */
        public synchronized boolean handle(SubscriptionEvent evt)
        {
            Map<String, Listener> providerContacts = outstanding.get(evt.getSourceProvider());

            if (providerContacts != null)
            {
                for (Map.Entry<String, Listener> entry : providerContacts.entrySet())
                {
                    if (entry.getValue().pendingContact(evt))
                    {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Remove a previously registered listener for a pending contact
         *
         * @param listener Listener to remove.
         */
        public synchronized void removeListener(Listener listener)
        {
            ProtocolProviderService pps = listener.provider();
            String contact = listener.contactID();

            logger.debug("Stopping listening for contact: " + contact +
                         " with provider " + pps +
                         " and listener: " + listener);

            Map<String, Listener> providerContacts = outstanding.get(pps);

            if (providerContacts == null)
            {
                logger.warn("No contacts registered for provider: " + pps +
                         " when unregistering: " + listener);
                return;
            }

            Listener local = providerContacts.get(contact);

            if (local == listener)
            {
                providerContacts.remove(contact);
                synchronized (metaContactToContactIdSet)
                {
                    metaContactToContactIdSet.removeAll(
                            metaContactToContactIdSet.stream()
                                    .filter(e -> e.getContactID().equalsIgnoreCase(contact))
                                    .collect(toSet()));
                }
            }
            else
            {
                logger.warn("Listener registered for contact: " + contact +
                            " was different: " + local +
                            " not: " + listener);
            }

            if (providerContacts.size() == 0)
            {
                outstanding.remove(pps);
            }
        }
    }

    /**
     * The class would listen for events delivered to
     * <tt>SubscriptionListener</tt>s.
     */
    private class ContactListSubscriptionListener
        implements SubscriptionListener
    {
        /**
         * Creates a meta contact for the source contact indicated by the
         * specified SubscriptionEvent, or updates an existing one if there
         * is one. The method would also generate the corresponding
         * <tt>MetaContactEvent</tt>.
         *
         * @param evt the SubscriptionEvent that we'll be handling.
         */
        @Override
        public void subscriptionCreated(SubscriptionEvent evt)
        {
            contactLogger.debug(evt.getSourceContact(),
                            "Subscription created: " + evt +
                            " provider: " + evt.getSourceProvider());

            if (pendingContacts.handle(evt))
            {
                contactLogger.info(evt.getSourceContact(),
                    "Ignoring event as it's been handled as a pending contact");
                return;
            }

            MetaContactGroupImpl parentGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getParentGroup());

            if (parentGroup == null)
            {
                logger.error("Received a subscription for a group that we "
                             + "hadn't seen before! ");
                return;
            }

            MetaContactImpl newMetaContact = new MetaContactImpl();

            newMetaContact.addProtoContact(evt.getSourceContact());

            parentGroup.addMetaContact(newMetaContact);

            //fire the meta contact event.
            fireMetaContactEvent(newMetaContact,
                                 parentGroup,
                                 MetaContactEvent.META_CONTACT_ADDED,
                                 null);

            //make sure we have a local copy of the avatar;
            newMetaContact.getAvatar();
        }

        /**
         * Indicates that a contact/subscription has been moved from one server
         * stored group to another. The way we handle the event depends on
         * whether the source contact/subscription is the only proto contact
         * found in its current MetaContact encapsulator or not.
         * <p>
         * If this is the case (the source contact has no siblings in its current
         * meta contact list encapsulator) then we will move the whole meta
         * contact to the meta contact group corresponding to the new parent
         * ContactGroup of the source contact. In this case we would only fire
         * a MetaContactMovedEvent containing the old and new parents of the
         * MetaContact in question.
         * <p>
         * If, however, the MetaContact that currently encapsulates the source
         * contact also encapsulates other proto contacts, then we will create
         * a new MetaContact instance, place it in the MetaContactGroup
         * corresponding to the new parent ContactGroup of the source contact
         * and add the source contact inside it. In this case we would first
         * fire a metacontact added event over the empty meta contact and then,
         * once the proto contact has been moved inside it, we would also fire
         * a ProtoContactEvent with event id PROTO_CONTACT_MOVED.
         * <p>
         * @param evt a reference to the SubscriptionMovedEvent containing previous
         * and new parents as well as a ref to the source contact.
         */
        @Override
        public void subscriptionMoved(SubscriptionMovedEvent evt)
        {
            logger.trace("Subscription moved: " + evt);

            MetaContactGroupImpl oldParentGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getOldParentGroup());
            MetaContactGroupImpl newParentGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getNewParentGroup());

            if (newParentGroup == null || oldParentGroup == null)
            {
                logger.error("Received a subscription for a group that we "
                             + "hadn't seen before! ");
                return;
            }

            MetaContactImpl currentMetaContact = (MetaContactImpl)
                               findMetaContactByContact(evt.getSourceContact());

            if(currentMetaContact == null)
            {
                logger.warn("Received a move event for a contact that is "
                            +"not in our contact list."
                            , new NullPointerException(
                                    "Received a move event for a contact that "
                                    +"is not in our contact list."));
                return;
            }

             //if the move was caused by us (when merging contacts) then chances
            //are that the contact is already in the right group
            MetaContactGroup currentParentGroup
                = currentMetaContact.getParentMetaContactGroup();

            if(currentParentGroup == newParentGroup)
            {
                return;
            }

            //if the meta contact does not have other children apart from the
            //contact that we're currently moving then move the whole meta
            //contact to the new parent group.
            if( currentMetaContact.getContactCount() == 1 )
            {
                oldParentGroup.removeMetaContact(currentMetaContact);
                newParentGroup.addMetaContact(currentMetaContact);
                fireMetaContactEvent(new MetaContactMovedEvent(
                    currentMetaContact, oldParentGroup, newParentGroup),
                        null);
            }
            //if the source contact is not the only contact encapsulated by the
            //currentMetaContact, then create a new meta contact in the new
            //parent group and move the source contact to it.
            else
            {
                MetaContactImpl newMetaContact = new MetaContactImpl();
                newParentGroup.addMetaContact(newMetaContact);

                //fire an event notifying that a new meta contact was added.
                fireMetaContactEvent(newMetaContact,
                                     newParentGroup,
                                     MetaContactEvent.META_CONTACT_ADDED,
                                     null);

                //move the proto contact and fire the corresponding event
                currentMetaContact.removeProtoContact(evt.getSourceContact());
                newMetaContact.addProtoContact(evt.getSourceContact());

                fireProtoContactEvent(evt.getSourceContact()
                                      , ProtoContactEvent.PROTO_CONTACT_MOVED
                                      , currentMetaContact
                                      , newMetaContact
                                      , null);
            }
        }

        @Override
        public void subscriptionFailed(SubscriptionEvent evt)
        {
            logger.trace("Subscription failed: " + evt);
        }

        /**
         * Events delivered through this method are ignored as they are of no
         * interest to this implementation of the meta contact list service.
         * @param evt the SubscriptionEvent containing the source contact
         */
        @Override
        public void subscriptionResolved(SubscriptionEvent evt)
        {
            //this was a contact we already had so all we need to do is
            //update it's details
            Contact source = evt.getSourceContact();
            MetaContactImpl mc = (MetaContactImpl)findMetaContactByContact(source);

            if(mc != null)
            {
                contactLogger.note(mc, "ProtoContact now resolved " + source);
                fireProtoContactEvent(source,
                                      ProtoContactEvent.PROTO_CONTACT_MODIFIED,
                                      mc,
                                      mc,
                                      null);
            }
        }

        /**
         * In the case where the event refers to a change in the display name
         * we compare the old value with the display name of the corresponding
         * meta contact. If they are equal this means that the user has not
         * specified their own display name for the meta contact and that the
         * display name was using this contact's display name for its own
         * display name. In this case we change the display name of the meta
         * contact to match the new display name of the proto contact.
         * <p>
         * @param evt the <tt>ContactPropertyChangeEvent</tt> containing the source
         * contact and the old and new values of the changed property.
         */
        @Override
        public void contactModified(ContactPropertyChangeEvent evt)
        {
            Contact sourceContact = evt.getSourceContact();
            String propertyName = evt.getPropertyName();

            MetaContactImpl mc =
                (MetaContactImpl)findMetaContactByContact(sourceContact);

            if (mc != null)
            {
                if( ContactPropertyChangeEvent.PROPERTY_DISPLAY_NAME.
                                                           equals(propertyName))
                {
                    if( evt.getOldValue() != null
                        && evt.getOldValue().equals(mc.getDisplayName()))
                    {
                        mc.protoContactChanged(sourceContact);
                    }
                    else
                    {
                        // we get here if the name of a contact has changed but
                        // the meta contact list is not going to reflect any
                        // change because it is not displaying that name. in
                        // this case we simply make sure everyone (e.g. the
                        // storage manager) knows about the change.
                        fireProtoContactEvent(sourceContact,
                                        ProtoContactEvent.PROTO_CONTACT_MODIFIED,
                                        mc,
                                        mc,
                                        null);
                    }

                    // Changing the display name has surely brought a change in
                    // the order as well so let's tell the others
                    fireMetaContactGroupEvent(findParentMetaContactGroup(mc),
                                              null,
                                              null,
                                              MetaContactGroupEvent.CHILD_CONTACTS_REORDERED,
                                              null);
                }
                else if( ContactPropertyChangeEvent.PROPERTY_IMAGE
                                .equals(propertyName))
                {
                    BufferedImageFuture newAvatar =
                        (BufferedImageFuture) evt.getNewValue();

                    if (newAvatar != null)
                    {
                        changeMetaContactAvatar(
                                        mc,
                                        sourceContact,
                                        newAvatar);
                    }
                }
                else if(ContactPropertyChangeEvent.PROPERTY_PERSISTENT_DATA
                                .equals(propertyName)
                        || ContactPropertyChangeEvent.PROPERTY_DISPLAY_DETAILS
                                .equals(propertyName))
                {
                    // if persistent data changed fire an event to store it
                    fireProtoContactEvent(sourceContact,
                                        ProtoContactEvent.PROTO_CONTACT_MODIFIED,
                                        mc,
                                        mc,
                                        null);
                }
            }
            else
            {
                logger.warn("No MetaContact found for event " + propertyName +
                            " from ProtoContact " + sourceContact);
            }
        }

        /**
         * Locates the <tt>MetaContact</tt> corresponding to the contact
         * that has been removed and updates it. If the removed proto contact
         * was the last one in it, then the <tt>MetaContact</tt> is also
         * removed.
         *
         * @param evt the <tt>SubscriptionEvent</tt> containing the contact
         * that has been removed.
         */
        @Override
        public void subscriptionRemoved(SubscriptionEvent evt)
        {
            logger.trace("Subscription removed: " + evt);

            MetaContactImpl metaContact = (MetaContactImpl)
                findMetaContactByContact(evt.getSourceContact());

            MetaContactGroupImpl metaContactGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getParentGroup());

            if (metaContact != null)
            {
                metaContact.removeProtoContact(evt.getSourceContact());
                fireProtoContactEvent(evt.getSourceContact(),
                                      ProtoContactEvent.PROTO_CONTACT_REMOVED,
                                      metaContact,
                                      null,
                                      null);

                //if this was the last protocol specific contact in this meta
                //contact then remove the meta contact as well.
                if (metaContact.getContactCount() == 0)
                {
                    metaContactGroup.removeMetaContact(metaContact);

                    fireMetaContactEvent(metaContact,
                        metaContactGroup,
                        MetaContactEvent.META_CONTACT_REMOVED,
                        null);
                }
            }
        }
    }

    /**
     * The class would listen for events delivered to
     * <tt>ServerStoredGroupListener</tt>s.
     */
    private class ContactListGroupListener
        implements ServerStoredGroupListener
    {
        /**
         * The method is called upon receiving notification that a new server
         * stored group has been created.
         * @param parent  a reference to the <tt>MetaContactGroupImpl</tt> where
         * <tt>group</tt>'s newly created <tt>MetaContactGroup</tt> wrapper
         * should be added as a subgroup.
         * @param group the newly added <tt>ContactGroup</tt>
         * @return the <tt>MetaContactGroup</tt> that now wraps the newly
         *  created <tt>ContactGroup</tt>.
         */
        private MetaContactGroup handleGroupCreatedEvent(
                                             MetaContactGroupImpl parent,
                                             ContactGroup group)
        {
            //if parent already contains a meta group with the same name, we'll
            //reuse it as the container for the new contact group.
            MetaContactGroupImpl newMetaGroup = (MetaContactGroupImpl)parent
                .getMetaContactSubgroup(group.getGroupName());

            //if there was no meta group with the specified name, create a new
            //one
            if(newMetaGroup == null)
            {
                newMetaGroup
                    = new MetaContactGroupImpl(
                            MetaContactListServiceImpl.this,
                            group.getGroupName());
                newMetaGroup.addProtoGroup(group);
                parent.addSubgroup(newMetaGroup);
            }
            else
            {
                newMetaGroup.addProtoGroup(group);
            }

            //check if there were any subgroups
            Iterator<ContactGroup> subgroups = group.subgroups();

            while(subgroups.hasNext())
            {
                ContactGroup subgroup = subgroups.next();
                handleGroupCreatedEvent(newMetaGroup, subgroup);
            }

            Iterator<Contact> contactsIter = group.contacts();

            while (contactsIter.hasNext())
            {
                Contact contact = contactsIter.next();

                MetaContactImpl newMetaContact = new MetaContactImpl();

                newMetaContact.addProtoContact(contact);

                newMetaGroup.addMetaContact(newMetaContact);
            }

            return newMetaGroup;
        }

        /**
         * Adds the source group and its child contacts to the meta contact
         * list.
         * @param evt the ServerStoredGroupEvent containing the source group.
         */
        @Override
        public void groupCreated(ServerStoredGroupEvent evt)
        {
            logger.trace("ContactGroup created: " + evt);

            //ignore the event if the source group is in the ignore list
            if (isGroupInEventIgnoreList(evt.getSourceGroup().getGroupName()
                                         , evt.getSourceProvider()))
            {
                return;
            }

            MetaContactGroupImpl parentMetaGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup( evt.getParentGroup());

            if (parentMetaGroup == null)
            {
                logger.error("Failed to identify a parent where group "
                    + logHasher(logHasher(evt.getSourceGroup().getGroupName())) + "should be placed.");
            }

            // check whether the meta group was already existing before
            // adding proto-groups to it
            boolean isExisting = parentMetaGroup.getMetaContactSubgroup(
                evt.getSourceGroup().getGroupName()) != null;

            // add parent group to the ServerStoredGroupEvent
            MetaContactGroup newMetaGroup
                = handleGroupCreatedEvent(parentMetaGroup, evt.getSourceGroup());

            //if this was the first contact group in the meta group fire an
            //ADDED event. otherwise fire a modification event.
            if(newMetaGroup.countContactGroups() > 1 || isExisting)
            {
                fireMetaContactGroupEvent(
                    newMetaGroup
                    , evt.getSourceProvider()
                    , evt.getSourceGroup()
                    , MetaContactGroupEvent.CONTACT_GROUP_ADDED_TO_META_GROUP
                    , null);
            }
            else
            {
                fireMetaContactGroupEvent(
                    newMetaGroup
                    , evt.getSourceProvider()
                    , evt.getSourceGroup()
                    , MetaContactGroupEvent.META_CONTACT_GROUP_ADDED
                    , null);
            }
        }

        /**
         * Dummy implementation.
         * <p>
         * @param evt a ServerStoredGroupEvent containing the source group.
         */
        @Override
        public void groupResolved(ServerStoredGroupEvent evt)
        {
            //we couldn't care less :)
        }

        /**
         * Updates the local contact list by removing the meta contact group
         * corresponding to the group indicated by the delivered <tt>evt</tt>
         * @param evt the ServerStoredGroupEvent confining the group that has
         * been removed.
         */
        @Override
        public void groupRemoved(ServerStoredGroupEvent evt)
        {
            logger.trace("ContactGroup removed: " + evt);

            MetaContactGroupImpl metaContactGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getSourceGroup());

            if (metaContactGroup == null)
            {
                logger.error(
                    "Received a RemovedGroup event for an orphan grp: "
                    + evt.getSourceGroup());
                return;
            }

            removeContactGroupFromMetaContactGroup(metaContactGroup,
                evt.getSourceGroup(), evt.getSourceProvider());

            //do not remove the meta contact group even if this is the las
            //protocol specific contact group. Contrary to contacts, meta
            //contact groups are to only be remove upon user indication or
            //otherwise it would be difficult for a user to create a new grp.
        }

        /**
         * Nothing to do here really. Oh yes .... we should actually trigger
         * a MetaContactGroup event indicating the change for interested parties
         * but that's all.
         * @param evt the ServerStoredGroupEvent containing the source group.
         */
        @Override
        public void groupNameChanged(ServerStoredGroupEvent evt)
        {
            logger.trace("ContactGroup renamed: " + evt);

            MetaContactGroup metaContactGroup
                = findMetaContactGroupByContactGroup(evt.getSourceGroup());

            fireMetaContactGroupEvent(
                metaContactGroup
                , evt.getSourceProvider()
                , evt.getSourceGroup()
                , MetaContactGroupEvent.CONTACT_GROUP_RENAMED_IN_META_GROUP
                , null);
        }
    }

    /**
     * Creates the corresponding MetaContact event and notifies all
     * <tt>MetaContactListListener</tt>s that a MetaContact is added or
     * removed from the MetaContactList.  If a sourceLister has been provided,
     * that listener is not notified, as it generated the event.
     *
     * @param sourceContact the contact that this event is about.
     * @param parentGroup the group that the source contact belongs or belonged
     * to.
     * @param eventID the id indicating the exact type of the event to fire.
     * @param sourceListener the listener that caused this event to be fired.
     * It this is not null, this listener will not be notified of the given
     * event.
     */
    private synchronized void fireMetaContactEvent(MetaContact sourceContact,
                                      MetaContactGroup parentGroup,
                                      int eventID,
                                      MetaContactListListener sourceListener)
    {
        MetaContactEvent evt
            = new MetaContactEvent(sourceContact, parentGroup, eventID);

        logger.trace("Will dispatch the following mcl event: " + evt);

        for (MetaContactListListener listener : getMetaContactListListeners())
        {
            try
            {
                if (!listener.equals(sourceListener))
                {
                    switch (evt.getEventID())
                    {
                        case MetaContactEvent.META_CONTACT_ADDED:
                            listener.metaContactAdded(evt);
                            break;
                        case MetaContactEvent.META_CONTACT_REMOVED:
                            listener.metaContactRemoved(evt);
                            break;
                        default:
                            logger.error("Unknown event type " + evt.getEventID());
                    }
                }
            }
            catch (Exception e)
            {
                // Don't let exceptions in one listener affect others
                logger.error("Unable to fire event " + evt + " to " + listener, e);
            }
        }
    }

    /**
     * Gets a copy of the list of current <code>MetaContactListListener</code>
     * interested in events fired by this instance.
     *
     * @return an array of <code>MetaContactListListener</code>s currently
     *         interested in events fired by this instance. The returned array
     *         is a copy of the internal listener storage and thus can be safely
     *         modified.
     */
    private MetaContactListListener[] getMetaContactListListeners()
    {
        MetaContactListListener[] listeners;

        synchronized (metaContactListListeners)
        {
            listeners
                = metaContactListListeners.toArray(
                        new MetaContactListListener[
                                metaContactListListeners.size()]);
        }
        return listeners;
    }

    /**
     * Creates the corresponding <tt>MetaContactPropertyChangeEvent</tt>
     * instance and notifies all <tt>MetaContactListListener</tt>s that a
     * MetaContact has been modified. If a sourceLister has been provided,
     * that listener is not notified, as it generated the event.  Synchronized
     * to avoid firing events when we are editing the account.
     *
     * @param event the event to dispatch.
     * @param sourceListener the listener that caused this event to be fired.
     * It this is not null, this listener will not be notified of the given
     * event.
     */
    synchronized void fireMetaContactEvent(MetaContactPropertyChangeEvent event,
                                         MetaContactListListener sourceListener)
    {
        logger.trace("Will dispatch the following mcl property change event: "
                     + event);

        for (MetaContactListListener listener : getMetaContactListListeners())
        {
            try
            {
                if (!listener.equals(sourceListener))
                {
                    if (event instanceof MetaContactMovedEvent)
                    {
                        listener.metaContactMoved( (MetaContactMovedEvent) event);
                    }
                    else if (event instanceof MetaContactRenamedEvent)
                    {
                        listener.metaContactRenamed(
                            (MetaContactRenamedEvent) event);
                    }
                    else if (event instanceof MetaContactModifiedEvent)
                    {
                        listener.metaContactModified(
                            (MetaContactModifiedEvent) event);
                    }
                    else if (event instanceof MetaContactAvatarUpdateEvent)
                    {
                        listener.metaContactAvatarUpdated(
                            (MetaContactAvatarUpdateEvent) event);
                    }
                }
            }
            catch (Exception e)
            {
                // Don't let exceptions in one listener affect others
                logger.error("Unable to fire event " + event + " to " + listener, e);
            }
        }
    }

    /**
     * Creates the corresponding <tt>ProtoContactEvent</tt> instance and
     * notifies all <tt>MetaContactListListener</tt>s that a protocol specific
     * <tt>Contact</tt> has been added moved or removed.  If a sourceLister
     * has been provided, that listener is not notified, as it generated the
     * event.  Synchronized to avoid firing events when we are editing the
     * account.
     *
     * @param source the contact that has caused the event.
     * @param eventName One of the ProtoContactEvent.PROTO_CONTACT_XXX fields
     * indicating the exact type of the event.
     * @param oldParent the <tt>MetaContact</tt> that was wrapping the source
     * <tt>Contact</tt> before the event occurred or <tt>null</tt> if the event
     * is caused by adding a new <tt>Contact</tt>
     * @param newParent the <tt>MetaContact</tt> that is wrapping the source
     * <tt>Contact</tt> after the event occurred or <tt>null</tt> if the event
     * is caused by removing a <tt>Contact</tt>
     * @param sourceListener the listener that caused this event to be fired.
     * It this is not null, this listener will not be notified of the given
     * event.
     */
    private synchronized void fireProtoContactEvent(Contact     source,
                                       String      eventName,
                                       MetaContact oldParent,
                                       MetaContact newParent,
                                       MetaContactListListener sourceListener)
    {
        if (eventName.equals(ProtoContactEvent.PROTO_CONTACT_MODIFIED))
        {
            // Make sure that the parent MetaContact is aware of the change:
            MetaContact metaContact = findMetaContactByContact(source);
            if (metaContact != null)
            {
                metaContact.protoContactChanged(source);
            }
            else
            {
                logger.warn("Meta contact not found for changed proto contact "
                                                                      + source);
            }
        }

        ProtoContactEvent event
            = new ProtoContactEvent(source, eventName, oldParent, newParent );

        logger.trace("Will dispatch the following mcl property change event: "
                     + event);

        for (MetaContactListListener listener : getMetaContactListListeners())
        {
            try
            {
                if (!listener.equals(sourceListener))
                {
                    logger.trace("Dispatching to " + listener.getClass().getName());
                    switch (eventName)
                    {
                        case ProtoContactEvent.PROTO_CONTACT_ADDED:
                            listener.protoContactAdded(event);
                            break;
                        case ProtoContactEvent.PROTO_CONTACT_MOVED:
                            listener.protoContactMoved(event);
                            break;
                        case ProtoContactEvent.PROTO_CONTACT_REMOVED:
                            listener.protoContactRemoved(event);
                            break;
                        case ProtoContactEvent.PROTO_CONTACT_MODIFIED:
                            listener.protoContactModified(event);
                            break;
                    }
                }
            }
            catch (Exception e)
            {
                // Don't let exceptions in one listener affect others
                logger.error("Unable to fire event " + event + " to " + listener, e);
            }
        }
    }

    /**
     * Upon each status notification this method finds the corresponding meta
     * contact and updates the ordering in its parent group.
     * <p>
     * @param evt the ContactPresenceStatusChangeEvent describing the status
     * change.
     */
    @Override
    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent evt)
    {
        MetaContactImpl metaContactImpl =
            (MetaContactImpl) findMetaContactByContact(evt.getSourceContact());

        //ignore if we have no meta contact.
        if(metaContactImpl == null)
            return;

        // Update WISPA with the new contact presence.
        logger.debug("Notifying WISPA of new contact presence");
        if (wispaService != null)
        {
            wispaService.notify(WISPANamespace.CONTACTS,
                WISPAAction.DATA,
                metaContactImpl);
        }

        int oldContactIndex = metaContactImpl.getParentGroup()
            .indexOf(metaContactImpl);

        int newContactIndex = metaContactImpl.reevalContact();

        if(oldContactIndex != newContactIndex)
        {
            fireMetaContactGroupEvent(
                findParentMetaContactGroup(metaContactImpl)
                , evt.getSourceProvider()
                , null
                , MetaContactGroupEvent.CHILD_CONTACTS_REORDERED
                , null);
        }
    }

    /**
     * The method is called from the storage manager whenever a new contact
     * group has been parsed and it has to be created.
     * @param parentGroup the group that contains the meta contact group we're
     * about to load.
     * @param metaContactGroupUID the unique identifier of the meta contact
     * group.
     * @param displayName the name of the meta contact group.
     * @param listener the listener that called this method.  If this is not
     * null, the listener will not be notified about the MetaContactGroupEvent
     * that is generated by loading this group.
     * @return the newly created meta contact group.
     */
    MetaContactGroupImpl loadStoredMetaContactGroup(
        MetaContactGroupImpl parentGroup,
        String metaContactGroupUID,
        String displayName,
        MetaContactListListener listener)
    {
        //first check if the group exists already.
        MetaContactGroupImpl newMetaGroup = (MetaContactGroupImpl) parentGroup
            .getMetaContactSubgroupByUID(metaContactGroupUID);

        //if the group exists then we have already loaded it for another
        //account and we should reuse the same instance.
        if(newMetaGroup != null)
            return newMetaGroup;

        newMetaGroup
            = new MetaContactGroupImpl(this, displayName, metaContactGroupUID);

        parentGroup.addSubgroup(newMetaGroup);

        //I don't think this method needs to produce events since it is
        //currently only called upon initialization ... but it doesn't hurt
        //trying
        fireMetaContactGroupEvent(newMetaGroup, null, null
            , MetaContactGroupEvent.META_CONTACT_GROUP_ADDED, listener);

        return newMetaGroup;
    }

    /**
     * Creates a unresolved instance of the proto specific contact group
     * according to the specified arguments and adds it to
     * <tt>containingMetaContactGroup</tt>
     *
     * @param containingMetaGroup the <tt>MetaContactGroupImpl</tt> where the
     * restored contact group should be added.
     * @param contactGroupUID the unique identifier of the group.
     * @param parentProtoGroup the identifier of the parent proto group.
     * @param persistentData the persistent data last returned by the contact
     * group.
     * @param accountID the ID of the account that the proto group belongs to.
     * @return a reference to the newly created (unresolved) contact group.
     */
    ContactGroup loadStoredContactGroup(MetaContactGroupImpl containingMetaGroup,
                                        String               contactGroupUID,
                                        ContactGroup         parentProtoGroup,
                                        String               persistentData,
                                        String               accountID)
    {
        //get the presence op set
        ProtocolProviderService sourceProvider =
            currentlyInstalledProviders.get(accountID);
        OperationSetPersistentPresence presenceOpSet
            = sourceProvider
                .getOperationSet(OperationSetPersistentPresence.class);

        ContactGroup newProtoGroup = presenceOpSet.createUnresolvedContactGroup(
            contactGroupUID, persistentData,
                (parentProtoGroup == null)
                    ? presenceOpSet.getServerStoredContactListRoot()
                    : parentProtoGroup);

        containingMetaGroup.addProtoGroup(newProtoGroup);

        return newProtoGroup;
    }

    /**
     * The method is called from the storage manager whenever a new contact
     * has been parsed and it has to be created.
     * @param parentGroup the group that contains the meta contact we're about
     * to load.
     * @param metaUID the unique identifier of the meta contact.
     * @param displayName the display name of the meta contact.
     * @param isDisplayNameUserDefined if the display name was chosen by the
     * user. If so, we don't chance it to reflect underlying protocontacts.
     * @param details the details for the contact to create.
     * @param protoContacts a list containing descriptors of proto contacts
     * encapsulated by the meta contact that we're about to create.
     * @param accountID the identifier of the account that the contacts
     * originate from.
     * @param listener the listener that called this method.  If this is not
     * null, the listener will not be notified about the MetaContactEvent that
     * is generated by loading this group.
     * @return the loaded meta contact.
     */
    MetaContactImpl loadStoredMetaContact(
            MetaContactGroupImpl parentGroup,
            String metaUID,
            String displayName,
            boolean isDisplayNameUserDefined,
            Map<String, List<String>> details,
            List<MclStorageManager.StoredProtoContactDescriptor> protoContacts,
            String accountID,
            MetaContactListListener listener)
    {
        contactLogger.note("Loading metacontact " + logHasher(displayName) +
                                                      " (UID:" + metaUID + ")");

        //first check if the meta contact exists already.
        MetaContactImpl newMetaContact
            = (MetaContactImpl)findMetaContactByMetaUID(metaUID);

        if(newMetaContact == null)
        {
            newMetaContact = new MetaContactImpl(metaUID, details);
            newMetaContact.setDisplayName(displayName);
            contactLogger.note(newMetaContact,
                                  "MetaContact didn't exist, so we created it");
        }
        else
        {
            contactLogger.note(newMetaContact, "MetaContact already existed");
        }

        // If the display name is user-defined, prevent it being modified when
        // protocontacts are added or changed.
        newMetaContact.setDisplayNameUserDefined(isDisplayNameUserDefined);

        // Determine if this MetaContact is a favourite
        String favouriteDetail = newMetaContact.getDetail(
                                         MetaContact.CONTACT_FAVORITE_PROPERTY);
        boolean isFavourite = false;
        if (favouriteDetail != null)
        {
            isFavourite = Boolean.parseBoolean(favouriteDetail);
            contactLogger.note(newMetaContact,
                                         "Contact is favorite? " + isFavourite);
        }
        else
        {
            contactLogger.note(newMetaContact,
                                    "Contact status was null - assuming false");
        }

        //create unresolved contacts for the protocontacts associated with this
        //mc
        ProtocolProviderService sourceProvider =
            currentlyInstalledProviders.get(accountID);
        OperationSetPersistentPresence presenceOpSet
            = sourceProvider
                .getOperationSet(OperationSetPersistentPresence.class);

        for (MclStorageManager.StoredProtoContactDescriptor contactDescriptor
                : protoContacts)
        {
            //this contact has already been registered by another meta contact
            //so we'll ignore it. If this is the only contact in the meta
            //contact, we'll throw an exception at the end of the method and
            //cause the mcl storage manager to remove it.
            MetaContact mc = findMetaContactByContact(
                contactDescriptor.contactAddress, accountID);

            if(mc != null)
            {
                contactLogger.warn("Ignoring duplicate proto contact "
                            + contactDescriptor
                            + " accountID=" + accountID
                            + ". The contact was also present in the "
                            + "following meta contact:" + mc);
                continue;
            }

            ContactGroup parentContactGroup =
                contactDescriptor.parentProtoGroup == null ?
                presenceOpSet.getServerStoredContactListRoot() :
                contactDescriptor.parentProtoGroup;
            Contact protoContact = presenceOpSet.createUnresolvedContact(
                contactDescriptor.contactAddress,
                contactDescriptor.persistentData,
                parentContactGroup);

            if (protoContact == null)
            {
                contactLogger.debug(newMetaContact,
                     contactDescriptor.contactAddress +
                     " did not create an unresolved contact when requested." +
                     " Assume it will be loaded later.");
                continue;
            }

            if (protoContact.isResolved())
            {
                logger.warn("Tried to create unresolved contact, but resolved" +
                    " copy already exists");

                // Already been resolved and already been added.  Make sure that
                // the details are in the existing contact
                MetaContact existingContact =
                    findMetaContactByContact(protoContact);

                if (existingContact != null)
                {
                    for (String detailName : details.keySet())
                    {
                        for (String detailValue : details.get(detailName))
                        {
                            existingContact.addDetail(detailName, detailValue);
                        }
                    }
                }
            }
            else
            {
                newMetaContact.addProtoContact(protoContact);
            }
        }

        if(newMetaContact.getContactCount() == 0)
        {
            logger.error("Found an empty meta contact. Throwing an exception "
                + "so that the storage manager would remove it.");
            throw new IllegalArgumentException("MetaContact["
                + newMetaContact
                +"] contains no non-duplicating child contacts.");
        }

        parentGroup.addMetaContact(newMetaContact);

        fireMetaContactEvent(   newMetaContact,
                                parentGroup,
                                MetaContactEvent.META_CONTACT_ADDED,
                                listener);

        logger.trace("Created meta contact: " + newMetaContact);

        return newMetaContact;
    }

    /**
     * Creates the corresponding MetaContactGroup event and notifies all
     * <tt>MetaContactListListener</tt>s that a MetaContactGroup is added or
     * removed from the MetaContactList.  If a sourceLister has been provided,
     * that listener is not notified, as it generated the event.  Synchronized
     * to avoid firing events when we are editing the account.
     *
     * @param source
     *            the MetaContactGroup instance that is added to the
     *            MetaContactList
     * @param provider
     *            the ProtocolProviderService instance where this event occurred
     * @param sourceProtoGroup the proto group associated with this event or
     *            null if the event does not concern a particular source group.
     * @param eventID
     *            one of the METACONTACT_GROUP_XXX static fields indicating the
     *            nature of the event.
     * @param sourceListener the listener that caused this event to be fired.
     * It this is not null, this listener will not be notified of the given
     * event.
     */
    private synchronized void fireMetaContactGroupEvent( MetaContactGroup source,
                                         ProtocolProviderService provider,
                                         ContactGroup sourceProtoGroup,
                                         int eventID,
                                         MetaContactListListener sourceListener)
    {
        MetaContactGroupEvent evt = new MetaContactGroupEvent(
            source, provider, sourceProtoGroup, eventID);

        logger.trace("Will dispatch the following mcl event: " + evt);

        for (MetaContactListListener listener : getMetaContactListListeners())
        {
            try
            {
                if (!listener.equals(sourceListener))
                {
                    switch (eventID)
                    {
                        case MetaContactGroupEvent.META_CONTACT_GROUP_ADDED:
                            listener.metaContactGroupAdded(evt);
                            break;
                        case MetaContactGroupEvent.META_CONTACT_GROUP_REMOVED:
                            listener.metaContactGroupRemoved(evt);
                            break;
                        case MetaContactGroupEvent.CHILD_CONTACTS_REORDERED:
                            listener.childContactsReordered(evt);
                            break;
                        case MetaContactGroupEvent
                            .META_CONTACT_GROUP_RENAMED:
                        case MetaContactGroupEvent
                            .CONTACT_GROUP_RENAMED_IN_META_GROUP:
                        case MetaContactGroupEvent
                            .CONTACT_GROUP_REMOVED_FROM_META_GROUP:
                        case MetaContactGroupEvent
                            .CONTACT_GROUP_ADDED_TO_META_GROUP:
                            listener.metaContactGroupModified(evt);
                            break;
                        default:
                            logger.error("Unknown event type (" + eventID
                                         + ") for event: " + evt);
                    }
                }
            }
            catch (Exception e)
            {
                // Don't let exceptions in one listener affect others
                logger.error("Unable to fire event " + evt + " to " + listener, e);
            }
        }
    }

    /**
     * Utility class used for blocking the current thread until an event
     * is delivered confirming the creation of a particular group.
     */
    private static class BlockingGroupEventRetriever
        implements ServerStoredGroupListener
    {
        private final String groupName;

        public ServerStoredGroupEvent evt = null;

        /**
         * Creates an instance of the retriever that will wait for events
         * confirming the creation of the group with the specified name.
         * @param groupName the name of the group whose birth we're waiting for.
         */
        BlockingGroupEventRetriever(String groupName)
        {
            this.groupName = groupName;
        }

        /**
         * Called whoever an indication is received that a new server stored
         * group is created.
         * @param event a ServerStoredGroupChangeEvent containing a reference to
         * the newly created group.
         */
        @Override
        public synchronized void groupCreated(ServerStoredGroupEvent event)
        {
            if (event.getSourceGroup().getGroupName().equals(groupName))
            {
                this.evt = event;
                this.notifyAll();
            }
        }

        /**
         * Evens delivered through this method are ignored
         * @param event param ignored
         */
        @Override
        public void groupRemoved(ServerStoredGroupEvent event)
        {}

        /**
         * Evens delivered through this method are ignored
         * @param event param ignored
         */
        @Override
        public void groupNameChanged(ServerStoredGroupEvent event)
        {}

        /**
         * Evens delivered through this method are ignored
         * @param event param ignored
         */
        @Override
        public void groupResolved(ServerStoredGroupEvent event)
        {}

        /**
         * Block the execution of the current thread until either a group
         * created event is received or millis milliseconds pass.
         * @param millis the number of millis that we should wait before we
         * determine failure.
         */
        public synchronized void waitForEvent(long millis)
        {
            //no need to wait if an event is already there.
            if (evt == null)
            {
                try
                {
                    this.wait(millis);
                }
                catch (InterruptedException ex)
                {
                    logger.error("Interrupted while waiting for group creation",
                                 ex);
                }
            }
        }
    }

    /**
     * Utility class to assist us with tracking metaContacts and the contactIDs generated
     * by asynchronous server actions on said metaContact.
     */
    private class MetaContactAndContactID
    {
        private final MetaContact metaContact;
        private final String contactID;

        MetaContactAndContactID(MetaContact metaContact, String contactID)
        {
            this.metaContact = metaContact;
            this.contactID = contactID;
        }

        String getContactID()
        {
            return this.contactID;
        }

        MetaContact getMetaContact()
        {
            return this.metaContact;
        }
    }

    /**
     * Notifies this listener that the list of the <tt>OperationSet</tt>
     * capabilities of a <tt>Contact</tt> has changed.
     *
     * @param event a <tt>ContactCapabilitiesEvent</tt> with ID
     * {@link ContactCapabilitiesEvent#SUPPORTED_OPERATION_SETS_CHANGED} which
     * specifies the <tt>Contact</tt> whose list of <tt>OperationSet</tt>
     * capabilities has changed
     */
    @Override
    public void supportedOperationSetsChanged(ContactCapabilitiesEvent event)
    {
        // If the source contact isn't contained in this meta contact we have
        // nothing more to do here.
        MetaContactImpl metaContactImpl
            = (MetaContactImpl) findMetaContactByContact(
                event.getSourceContact());

        //ignore if we have no meta contact.
        if(metaContactImpl == null)
            return;

        Contact contact = event.getSourceContact();

        metaContactImpl.updateCapabilities(contact, event.getOperationSets());

        fireProtoContactEvent(  contact,
                                ProtoContactEvent.PROTO_CONTACT_MODIFIED,
                                metaContactImpl,
                                metaContactImpl,
                                null);
    }

    @Override
    public String getStateDumpName()
    {
        return "MetaContactListService";
    }

    @Override
    public String getState()
    {
        StringBuilder state = new StringBuilder();
        state.append("Listeners: \n")
             .append(metaContactListListeners)
             .append("\n\nRoot group:\n")
             .append(rootMetaGroup);

        return state.toString();
    }
}
