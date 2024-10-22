/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.service.insights.parameters.JabberParameterInfo.CONTACT_ADD_COUNT;
import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.Hasher.logHasher;
import static org.jitsi.util.SanitiseUtils.sanitiseValuesInList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.google.common.annotations.VisibleForTesting;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Domainpart;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.impl.protocol.jabber.OperationSetPersistentPresenceJabberImpl.ContactChangesListener;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.diagnostics.StateDumper;
import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.insights.parameters.SipParameterInfo;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactGroup;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ServerStoredDetails;
import net.java.sip.communicator.service.protocol.event.ContactPropertyChangeEvent;
import net.java.sip.communicator.service.protocol.event.ServerStoredGroupEvent;
import net.java.sip.communicator.service.protocol.event.ServerStoredGroupListener;
import net.java.sip.communicator.service.protocol.event.SubscriptionEvent;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.AvatarCacheUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.ContactLogger;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.PrivacyUtils;
import org.jitsi.service.resources.BufferedImageFuture;

/**
 * This class encapsulates the Roster class. Once created, it will
 * register itself as a listener to the encapsulated Roster and modify it's
 * local copy of Contacts and ContactGroups every time an event is generated
 * by the underlying framework. The class would also generate
 * corresponding sip-communicator events to all events coming from smack.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class ServerStoredContactListJabberImpl implements StateDumper
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(ServerStoredContactListJabberImpl.class);

    private static final ContactLogger contactLogger =
        ContactLogger.getLogger();

    /**
     * The jabber list that we encapsulate
     */
    private Roster roster = null;

    /**
     * The root <code>ContactGroup</code>. The container for all jabber buddies
     * and groups.
     */
    private final RootContactGroupJabberImpl mRootGroup;

    /**
     * The operation set that created us and that we could use when dispatching
     * subscription events.
     */
    private final OperationSetPersistentPresenceJabberImpl parentOperationSet;

    /**
     * The provider that is on top of us.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * The chat room manager, responsible for joining and leaving chat rooms.
     */
    private final ChatRoomManager chatRoomManager;

    /**
     * Listeners that would receive event notifications for changes in group
     * names or other properties, removal or creation of groups.
     */
    private final Vector<ServerStoredGroupListener> serverStoredGroupListeners
        = new Vector<>();

    /**
     *  Thread retrieving images for contacts
     */
    private ImageRetriever imageRetriever = null;

    /**
     *  Lock for accessing the above thread.
     */
    private final Object imageRetrieverLock = new Object();

    /**
     * Listens for roster changes.
     */
    private ChangeListener rosterChangeListener = null;

    /**
     * Retrieve contact information.
     */
    private InfoRetreiver infoRetreiver = null;

    /**
     * Set to true once we have finished processing the initial server roster.
     */
    private boolean rosterProcessed = false;

    /**
     * A listener that is notified when a contact's presence changes.
     */
    private ContactChangesListener mPresenceChangeListener;

    /**
     * A list of roster changes received while we were still processing the
     * initial roster.
     */
    private final List<PendingRosterChange> pendingRosterChanges =
            new ArrayList<>();

    /**
     * Delay required between populating the roster and reading contacts from
     * it to avoid reading from a roster which is not fully populated. (SFR 479241)
     */
    @VisibleForTesting
    long rosterPopulationDelayMillis = 6000;

    /**
     * A simple enum to define the different types of roster changes that are
     * handled by this class.
     */
    private enum RosterChangeType
    {
        ENTRY_ADDED
        {
            @Override
            public void handleEntry(ChangeListener listener, Jid id)
            {
                listener.handleEntryAdded(id);
            }
        },

        ENTRY_DELETED
        {
            @Override
            public void handleEntry(ChangeListener listener, Jid id)
            {
                listener.handleEntryDeleted(id);
            }
        },

        ENTRY_UPDATED
        {
            @Override
            public void handleEntry(ChangeListener listener, Jid id)
            {
                listener.handleEntryUpdated(id);
            }
        };

        /**
         * Handles the local changes required as a result of a change on the
         * server to the roster entry with the given id that the given change
         * listener was notified about.
         *
         * @param listener the change listener
         * @param id the roster entry id
         */
        public abstract void handleEntry(ChangeListener listener, Jid id);

        /**
         * Called when the given change listener receives notification of a
         * change to the roster entries with the given ids in the given server
         * stored contact list implementation.
         *
         * @param listener the change listener
         * @param ids the list of roster entry ids
         * @param list the server stored contact list implementation
         */
        public void onRosterEntryChange(ChangeListener listener,
                                        Collection<Jid> ids,
                                        ServerStoredContactListJabberImpl list)
        {
            synchronized (list.pendingRosterChanges)
            {
                for (Jid id : ids)
                {
                    if (list.rosterProcessed)
                    {
                        logger.debug(
                            "Roster processed - handling entry " +
                                            sanitisePeerId(id) + " , " + this);
                        handleEntry(listener, id);
                    }
                    else
                    {
                        logger.debug(
                            "Roster not processed - caching entry " +
                                            sanitisePeerId(id) + " , " + this);
                        list.pendingRosterChanges.add(
                            new PendingRosterChange(id, this));
                    }
                }
            }
        }
    }

    /**
     * Creates a ServerStoredContactList wrapper for the specified BuddyList.
     *
     * @param parentOpSet the operation set that created us and that
     * we could use for dispatching subscription events
     * @param provider the provider that has instantiated us.
     * @param iRetreiver retrieve contact information.
     */
    ServerStoredContactListJabberImpl(
        OperationSetPersistentPresenceJabberImpl parentOpSet,
        ProtocolProviderServiceJabberImpl        provider,
        InfoRetreiver iRetreiver)
    {
        //We need to init these as early as possible to ensure that the provider
        //and the operationsset would not be null in the incoming events.
        parentOperationSet = parentOpSet;

        jabberProvider = provider;
        mRootGroup = new RootContactGroupJabberImpl(jabberProvider);
        infoRetreiver = iRetreiver;
        chatRoomManager = provider.getChatRoomManager();
    }

    /**
     * Returns the root group of the contact list.
     *
     * @return the root ContactGroup for the ContactList
     */
    public ContactGroup getRootGroup()
    {
        return mRootGroup;
    }

    /**
     * Returns the roster entry associated with the given XMPP address or
     * <tt>null</tt> if the user is not an entry in the roster.
     *
     * @param user the XMPP address of the user (e.g. "jsmith@example.com").
     * The address is a bare JID (e.g. "domain" or "user@domain/resource").
     *
     * @return the roster entry or <tt>null</tt> if it does not exist.
     */
    RosterEntry getRosterEntry(BareJid user)
    {
        if (roster == null)
        {
            // If roster is null, might also be null.
            String loggableUser = user == null ? null : sanitisePeerId(user);
            contactLogger.debug("Null roster looking for entry " + loggableUser);

            return null;
        }
        else
        {
            return roster.getEntry(user);
        }
    }

    /**
     * Returns the roster group with the specified name, or <tt>null</tt> if the
     * group doesn't exist.
     *
     * @param name the name of the group.
     * @return the roster group with the specified name.
     */
    RosterGroup getRosterGroup(String name)
    {
        return roster.getGroup(name);
    }

    /**
     * Registers the specified group listener so that it would receive events
     * on group modification/creation/destruction.
     * @param listener the ServerStoredGroupListener to register for group events
     */
    void addGroupListener(ServerStoredGroupListener listener)
    {
        synchronized(serverStoredGroupListeners)
        {
            if (!serverStoredGroupListeners.contains(listener))
                serverStoredGroupListeners.add(listener);
        }
    }

    /**
     * Removes the specified group listener so that it won't receive further
     * events on group modification/creation/destruction.
     * @param listener the ServerStoredGroupListener to unregister
     */
    void removeGroupListener(ServerStoredGroupListener listener)
    {
        synchronized(serverStoredGroupListeners)
        {
            serverStoredGroupListeners.remove(listener);
        }
    }

    /**
     * Creates the corresponding event and notifies all
     * <tt>ServerStoredGroupListener</tt>s that the source group has been
     * removed, changed, renamed or whatever happened to it.
     * @param group the ContactGroup that has been created/modified/removed
     * @param eventID the id of the event to generate.
     */
    private void fireGroupEvent(ContactGroupJabberImpl group, int eventID)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        ServerStoredGroupEvent evt = new ServerStoredGroupEvent(
                  group
                , eventID
                , parentOperationSet.getServerStoredContactListRoot()
                , jabberProvider
                , parentOperationSet);

        logger.trace("Will dispatch the following grp event: " + evt);

        Iterable<ServerStoredGroupListener> listeners;
        synchronized (serverStoredGroupListeners)
        {
            listeners
                = new ArrayList<>(
                    serverStoredGroupListeners);
        }

        /**
         * Sometimes contact statuses are received before the groups and
         * contacts are being created. This is a problem when we don't have
         * already created unresolved contacts. So we will check contact
         * statuses to be sure they are correct.
         */
        if(eventID == ServerStoredGroupEvent.GROUP_CREATED_EVENT)
        {
            Iterator<Contact> iter = group.contacts();
            while (iter.hasNext())
            {
                ContactJabberImpl c = (ContactJabberImpl)iter.next();

                // roster can be null, receiving system messages from server
                // before we are log in
                if (roster != null)
                {
                    processPresenceInfo(roster.getPresences(
                        c.getAddressAsJid().asBareJid()).iterator());
                }
            }
        }

        for (ServerStoredGroupListener listener : listeners)
        {
            if (eventID == ServerStoredGroupEvent.GROUP_REMOVED_EVENT)
                listener.groupRemoved(evt);
            else if (eventID == ServerStoredGroupEvent.GROUP_RENAMED_EVENT)
                listener.groupNameChanged(evt);
            else if (eventID == ServerStoredGroupEvent.GROUP_CREATED_EVENT)
                listener.groupCreated(evt);
            else if (eventID == ServerStoredGroupEvent.GROUP_RESOLVED_EVENT)
                listener.groupResolved(evt);
        }
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * removed event.
     * @param parentGroup the group where that the removed contact belonged to.
     * @param contact the contact that was removed.
     */
    void fireContactRemoved( ContactGroup parentGroup,
                                     ContactJabberImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            logger.debug("No presence op. set available. Bailing out.");
            return;
        }
        // No need to hash Contact, as its toString() method does that.
        logger.trace("Removing " + contact
                        + " from " + parentGroup.getGroupName());

        // dispatch
        parentOperationSet.fireSubscriptionEvent(contact, parentGroup,
            SubscriptionEvent.SUBSCRIPTION_REMOVED);
    }

    /**
     * Make the parent persistent presence operation set dispatch a subscription
     * moved event.
     * @param oldParentGroup the group where the source contact was located
     * before being moved
     * @param newParentGroup the group that the source contact is currently in.
     * @param contact the contact that was added
     */
    private void fireContactMoved( ContactGroup oldParentGroup,
                                   ContactGroupJabberImpl newParentGroup,
                                   ContactJabberImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        //dispatch
        parentOperationSet.fireSubscriptionMovedEvent(
            contact, oldParentGroup, newParentGroup);
    }

    /**
     * Retrns a reference to the provider that created us.
     * @return a reference to a ProtocolProviderServiceImpl instance.
     */
    ProtocolProviderServiceJabberImpl getParentProvider()
    {
        return jabberProvider;
    }

    /**
     * Returns the ConntactGroup with the specified name or null if no such
     * group was found.
     * <p>
     * @param name the name of the group we're looking for.
     * @return a reference to the ContactGroupJabberImpl instance we're looking for
     * or null if no such group was found.
     */
    public ContactGroupJabberImpl findContactGroup(String name)
    {
        Iterator<ContactGroup> contactGroups = mRootGroup.subgroups();

        // make sure we ignore any whitespaces
        name = name.trim();

        while(contactGroups.hasNext())
        {
            ContactGroupJabberImpl contactGroup
                = (ContactGroupJabberImpl) contactGroups.next();

            if (contactGroup.getGroupName() != null &&
                contactGroup.getGroupName().trim().equals(name))
                return contactGroup;
        }

        return null;
    }

    /**
     * Find a group with the specified Copy of Name. Used to track when
     * a group name has changed
     * @param name String
     * @return ContactGroupJabberImpl
     */
    private ContactGroupJabberImpl findContactGroupByNameCopy(String name)
    {
        Iterator<ContactGroup> contactGroups = mRootGroup.subgroups();

        // make sure we ignore any whitespaces
        name = name.trim();

        while(contactGroups.hasNext())
        {
            ContactGroupJabberImpl contactGroup
                = (ContactGroupJabberImpl) contactGroups.next();

            if (contactGroup.getNameCopy() != null &&
                contactGroup.getNameCopy().trim().equals(name))
                return contactGroup;
        }
        return null;
    }

    /**
     * Returns the Contact with the specified id or null if
     * no such id was found.
     *
     * @param id the id of the contact to find.
     * @return the <tt>Contact</tt> carrying the specified
     * <tt>screenName</tt> or <tt>null</tt> if no such contact exits.
     */
    public ContactJabberImpl findContactById(Jid id)
    {
        Iterator<ContactGroup> contactGroups = mRootGroup.subgroups();
        ContactJabberImpl result = null;
        BareJid userId = id.asBareJid();

        while (contactGroups.hasNext())
        {
            ContactGroupJabberImpl contactGroup
                = (ContactGroupJabberImpl)contactGroups.next();

            result = contactGroup.findContact(userId);

            if (result != null)
                return result;
        }

        // Check for private contacts
        ContactGroupJabberImpl volatileGroup = getNonPersistentGroup();
        if (volatileGroup != null)
        {
            result = volatileGroup.findContact(id);

            if (result != null)
            {
                return result;
            }
        }

        // Try the root group now
        return mRootGroup.findContact(userId);
    }

    /**
     * Returns the ContactGroup containing a contact with the specified Jid or null
     * if no such group exists.
     *
     * @param contactAddress the address of the contact whose parent group we're looking for.
     * @return the <tt>ContactGroup</tt> containing the specified
     * <tt>contactAddress</tt> or <tt>null</tt> if no such group exists.
     */
    public ContactGroup findContactGroupFromJid(Jid contactAddress)
    {
        Iterator<ContactGroup> contactGroups = mRootGroup.subgroups();

        while(contactGroups.hasNext())
        {
            ContactGroupJabberImpl contactGroup
                = (ContactGroupJabberImpl)contactGroups.next();

            if( contactGroup.findContact(contactAddress)!= null)
                return contactGroup;
        }

        if ( mRootGroup.findContact(contactAddress) != null)
            return mRootGroup;

        return null;
    }

    /**
     * Adds a new contact with the specified screenname to the list under a
     * default location.
     * @param id the id of the contact to add.
     * @throws OperationFailedException
     */
    public void addContact(String id)
        throws OperationFailedException
    {
        addContact(null, id);
    }

    /**
     * Adds a new contact with the specified screenname to the list under the
     * specified group.
     * @param id the id of the contact to add.
     * @param parent the group under which we want the new contact placed.
     * @throws OperationFailedException if the contact already exist
     */
    public void addContact(ContactGroup parent, String id)
        throws OperationFailedException
    {
        logger.trace
             ("Adding contact " + sanitisePeerId(id) + " to parent=" + parent);

        EntityBareJid completeID = parseAddressString(id);
        String loggableCompleteID = sanitisePeerId(completeID);
        contactLogger.debug("addContact id " + loggableCompleteID);

        //if the contact is already in the contact list and is not volatile,
        //then only broadcast an event
        ContactJabberImpl existingContact = findContactById(completeID);

        if( existingContact != null
            && existingContact.isPersistent() )
        {
            logger.debug("Contact " + loggableCompleteID
                            + " already exists in group "
                            + existingContact.getParentContactGroup());
            contactLogger.info("addContact - already exists");
            throw new OperationFailedException(
                "Contact " + loggableCompleteID + " already exists.",
                OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }

        try
        {
            String[] parentNames = null;

            if(parent != null)
                parentNames = new String[]{parent.getGroupName()};

            addEntryToRoster(completeID, completeID.toString(), parentNames);
            contactLogger.info("addContact - success");
        }
        catch (XMPPErrorException ex)
        {
            String errTxt = "Error adding new jabber entry";
            logger.error(errTxt, ex);
            contactLogger.error("addContact - failed");

            XMPPErrorConvertor.reThrowAsOperationFailedException(errTxt, ex);
        }
        catch (NotLoggedInException | InterruptedException | NotConnectedException
            | NoResponseException ex)
        {
            throw new OperationFailedException(
                    "Could not add contact",
                    OperationFailedException.INTERNAL_ERROR,
                    ex);
        }
    }

    /**
     * Adds an entry to the server stored roster.
     *
     * @param userID The user's ID.
     * @param userName The user's nickname.
     * @param groups The list of group names the entry will belong to, or null
     * if the roster entry won't belong to a group.
     */
    private synchronized void addEntryToRoster(BareJid userID,
                                               String userName,
                                               String[] groups)
        throws NoResponseException, XMPPErrorException, NotLoggedInException,
        NotConnectedException, InterruptedException
    {
        if (roster != null)
        {
            // Get our display name to put on the subscribe as a nick
            String displayName =
                    JabberActivator.getGlobalDisplayDetailsService()
                                   .getGlobalDisplayName();

            // use custom reply timeout because some XMPP may send "result" IQ late (> 5 seconds).
            roster.createItemAndRequestSubscription(userID, userName, groups, displayName,
                ProtocolProviderServiceJabberImpl.CUSTOM_SMACK_PACKET_REPLY_TIMEOUT_FOR_ROSTER);
        }
        else
        {
            logger.warn("Roster is null - this is expected if jabber has " +
                                                         "become unregistered");
        }
    }

    /**
     * Adds an entry to the roster for the given chat room id.
     *
     * @param id the id of chat room to add
     */
    public void addChatRoomToRoster(String id)
    {
        logger.info("Adding chat room to roster: " + sanitiseChatRoom(id));
        try
        {
            EntityBareJid completeId = parseAddressString(id);

            if (getRosterEntry(completeId) != null)
            {
                logger.debug("Chat room already in roster: " + sanitiseChatRoom(id));
                return;
            }

            addEntryToRoster(completeId, id, null);
            logger.info("Successfully added chat room to roster: " + sanitiseChatRoom(id));
        }
        catch (XMPPException | OperationFailedException | InterruptedException |
            NotConnectedException | NoResponseException | NotLoggedInException ex)
        {
            logger.error("Failed to add roster entry for chat room: " + sanitiseChatRoom(id), ex);
        }
    }

    /**
     * Removes the entry from the roster for the given chat room id.
     *
     * @param id the id of chat room to remove
     */
    public void removeChatRoomFromRoster(String id)
    {
        logger.debug("Removing chat room from roster: " + sanitiseChatRoom(id));

        try
        {
            EntityBareJid completeId = parseAddressString(id);
            RosterEntry entry = getRosterEntry(completeId);

            if (entry != null)
            {
                roster.removeEntry(entry);
                logger.info("Successfully removed chat room from roster: " + sanitiseChatRoom(id));
            }
        }
        catch (XMPPException | OperationFailedException | InterruptedException |
            NotConnectedException | NoResponseException | NotLoggedInException ex)
        {
            logger.error("Failed to remove roster entry for chat room " + sanitiseChatRoom(id), ex);
        }
    }

    /**
     * Creates a non-persistent contact for the specified address.
     * This just creates a contact and does not create any group.
     * @param id the address of the contact to create.
     * @param displayName contact name
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    ContactJabberImpl createVolatileContact(String id, String displayName)
    {
        contactLogger.debug("Creating volatile contact with id " + sanitisePeerId(id) +
                            " and display name " + logHasher(displayName));
        return new VolatileContactJabberImpl(id, this, displayName);
    }

    /**
     * Creates a non resolved contact for the specified address and inside the
     * specified group. The newly created contact would be added to the local
     * contact list as a standard contact but when an event is received from the
     * server concerning this contact, then it will be reused and only its
     * isResolved field would be updated instead of creating the whole contact
     * again.
     *
     * @param parentGroup the group where the unresolved contact is to be
     * created
     * @param id the Address of the contact to create.
     * @return the newly created unresolved <tt>ContactImpl</tt>
     */
    ContactJabberImpl createUnresolvedContact(ContactGroup parentGroup,
                                              BareJid id)
    {
        ContactJabberImpl newUnresolvedContact = new ContactJabberImpl(id, this, false);

        if (parentGroup instanceof ContactGroupJabberImpl)
        {
            ((ContactGroupJabberImpl)parentGroup).addContact(newUnresolvedContact);
        }
        else if (parentGroup instanceof RootContactGroupJabberImpl)
        {
            ((RootContactGroupJabberImpl)parentGroup).addContact(newUnresolvedContact);
        }

        fireContactAdded(parentGroup, newUnresolvedContact);

        return newUnresolvedContact;
    }

    /**
     * Creates a non resolved contact group for the specified name. The newly
     * created group would be added to the local contact list as any other group
     * but when an event is received from the server concerning this group, then
     * it will be reused and only its isResolved field would be updated instead
     * of creating the whole group again.
     * <p>
     * @param groupName the name of the group to create.
     * @return the newly created unresolved <tt>ContactGroupImpl</tt>
     */
    ContactGroupJabberImpl createUnresolvedContactGroup(String groupName)
    {
        ContactGroupJabberImpl newUnresolvedGroup =
            new ContactGroupJabberImpl(groupName, this);

        mRootGroup.addSubGroup(newUnresolvedGroup);

        fireGroupEvent(newUnresolvedGroup
                        , ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        return newUnresolvedGroup;
    }

    /**
     * Creates the specified group on the server stored contact list.
     * @param groupName a String containing the name of the new group.
     * @throws OperationFailedException with code CONTACT_GROUP_ALREADY_EXISTS
     * if the group we're trying to create is already in our contact list.
     */
    public void createGroup(String groupName)
        throws OperationFailedException
    {
        logger.trace("Creating group: " + groupName);

        ContactGroupJabberImpl existingGroup = findContactGroup(groupName);

        if( existingGroup != null && existingGroup.isPersistent() )
        {
            logger.debug("ContactGroup " + groupName + " already exists.");
            throw new OperationFailedException(
                           "ContactGroup " + groupName + " already exists.",
                OperationFailedException.CONTACT_GROUP_ALREADY_EXISTS);
        }

        RosterGroup newRosterGroup = roster.createGroup(groupName);

        ContactGroupJabberImpl newGroup =
            new ContactGroupJabberImpl(newRosterGroup,
                                       new Vector<RosterEntry>().iterator(),
                                       this,
                                       true);
        mRootGroup.addSubGroup(newGroup);

        fireGroupEvent(newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        logger.trace("Group " + groupName+ " created.");
    }

    /**
     * Removes the specified group from the buddy list.
     * @param groupToRemove the group that we'd like removed.
     */
    public void removeGroup(ContactGroupJabberImpl groupToRemove)
    {
        try
        {
            // first copy the item that will be removed
            // when iterating over group contacts and removing them
            // concurrent exception occures
            Vector<Contact> localCopy = new Vector<>();
            Iterator<Contact> iter = groupToRemove.contacts();

            while (iter.hasNext())
            {
                localCopy.add(iter.next());
            }

            iter = localCopy.iterator();
            while (iter.hasNext())
            {
                ContactJabberImpl item = (ContactJabberImpl) iter.next();
                if(item.isPersistent())
                    roster.removeEntry(item.getSourceEntry());
            }
        }
        catch (XMPPException| InterruptedException | NotConnectedException |
            NoResponseException | NotLoggedInException ex)
        {
            logger.error("Error removing group", ex);
        }
    }

    /**
     * Removes a contact from the serverside list
     * Event will come for successful operation
     * We expect to remove the contact locally later off the back of receiving a RosterListener callback.
     * @param contactToRemove ContactJabberImpl
     */
    void removeContactFromServer(ContactJabberImpl contactToRemove)
        throws OperationFailedException
    {
        // No need to hash Contact, as its toString() method does that.
        contactLogger.info(contactToRemove, "removeContact");
        try
        {
            RosterEntry entry = contactToRemove.getSourceEntry();

            if (entry != null)
            {
                // Delete this entry on the roster.
                roster.removeEntry(entry);

                JabberActivator.getInsightsService().logEvent(
                    InsightsEventHint.JABBER_IM_DELETE_ROSTER_ENTRY.name(),
                    null
                );
            }

            // Removing this from the roster now will mean we mightn't hear about any "remove"
            // stanza (via the handleEntryDeleted() callback), so we should remove it locally now.
            removeContactLocally(contactToRemove);
        }
        catch (XMPPErrorException ex)
        {
            String errTxt = "Error removing contact";
            logger.error(errTxt, ex);

            XMPPErrorConvertor.reThrowAsOperationFailedException(errTxt, ex);
        }
        catch (InterruptedException | NotLoggedInException | NotConnectedException |
            NoResponseException ex)
        {
            throw new OperationFailedException(
                    "Could not remove contact",
                    OperationFailedException.GENERAL_ERROR,
                    ex);
        }
    }

    /**
     * Renames the specified group according to the specified new name..
     * @param groupToRename the group that we'd like removed.
     * @param newName the new name of the group
     */
    public void renameGroup(ContactGroupJabberImpl groupToRename, String newName)
    {
        try
        {
            groupToRename.getSourceGroup().setName(newName);
            groupToRename.setNameCopy(newName);
        }
        catch (NotConnectedException | NoResponseException | InterruptedException |
            XMPPErrorException ex)
        {
            logger.error("Could not rename " + groupToRename + " to " + newName, ex);
        }
    }

    /**
     * Moves the specified <tt>contact</tt> to the group indicated by
     * <tt>newParent</tt>.
     * @param contact the contact that we'd like moved under the new group.
     * @param newParent the group where we'd like the parent placed.
     */
    public void moveContact(ContactJabberImpl contact,
                            AbstractContactGroupJabberImpl newParent)
    {
        if (contact.getParentContactGroup() != newParent)
        {
            // No need to hash Contact, as its toString() method does that.
            logger.warn("Contact moving between groups: " + contact + " from " +
                        contact.getParentContactGroup().getUID() + " to " +
                        newParent.getUID());
        }

        // when the contact is not persistent, coming
        // from NotInContactList group, we need just to add it to the list
        if(!contact.isPersistent())
        {
            try
            {
                addContact(newParent, contact.getAddress());
                return;
            }
            catch(OperationFailedException ex)
            {
                logger.error("Cannot move contact! ", ex);
            }
        }

        newParent.addContact(contact);

        try
        {
            // will create the entry with the new group so it can be removed
            // from other groups if any
            addEntryToRoster(contact.getSourceEntry().getJid(),
                contact.getDisplayName(),
                new String[]{newParent.getGroupName()});
        }
        catch (InterruptedException | NotLoggedInException | NotConnectedException |
            NoResponseException | XMPPErrorException ex)
        {
            logger.error("Cannot move contact! ", ex);
        }
    }

    /**
     * Sets a reference to the currently active and valid instance of
     * roster that this list is to use for retrieving
     * server stored information
     */
    void init(OperationSetPersistentPresenceJabberImpl.ContactChangesListener
                  presenceChangeListener)
    {
        logger.info("Initializing jabber server stored contact list");
        mPresenceChangeListener = presenceChangeListener;

        synchronized (pendingRosterChanges)
        {
            rosterProcessed = false;
            mPresenceChangeListener.storeEvents();
        }

        XMPPConnection connection = jabberProvider.getConnection();

        if (connection != null)
        {
            roster = Roster.getInstanceFor(connection);
        }
        else
        {
            logger.warn("XMPP connection is null - not setting roster");
        }

        // We don't set imageRetriever to null in cleanup, to stop further calls to addContactForImageUpdate from creating a new
        // Thread with an object that mightn't be used again.  Set it to null here, so we can create a new one from this point.
        // Out of an abundance of caution, call quit() again, in case cleanup() hasn't been called since the last init().
        synchronized (imageRetrieverLock)
        {
            if (imageRetriever != null)
            {
                imageRetriever.quit();
            }
            imageRetriever = null;
        }

        // Sleep for six seconds after populating the roster, to work around
        // SFR 479241 whereby the roster is not fully populated when we try to
        // read contacts from it.
        try
        {
            Thread.sleep(rosterPopulationDelayMillis);
        }
        catch (InterruptedException ex)
        {
            logger.error(ex);
        }

        if (roster != null)
        {
            roster.addRosterListener(mPresenceChangeListener);
            roster.setSubscriptionMode(Roster.SubscriptionMode.manual);

            // Create and add the roster change listener before we initialize the
            // roster to make sure we don't miss any roster events that happen
            // while we're processing the initial roster.
            rosterChangeListener = new ChangeListener();
            roster.addRosterListener(rosterChangeListener);

            try
            {
                initRoster();
            }
            catch (Throwable ex)
            {
                // There are often exceptions thrown from initRoster() if another thread is disconnecting us from XMPP.
                // Just ignore any exceptions if the roster is null (i.e. we've called cleanup()) - otherwise re-throw.
                if (roster == null)
                {
                    logger.info("Ignoring exception as disconnecting: " + ex);
                }
                else
                {
                    logger.error("Failed to initialize roster", ex);
                    throw ex;
                }
            }
        }
        else
        {
            logger.warn("Roster is null - this is expected if jabber has become unregistered");
        }
    }

    /**
     * Processes roster changes received while we were still processing the
     * initial roster.
     */
    protected void processPendingRosterUpdates()
    {
        logger.debug("Processing pending roster updates");

        if (rosterChangeListener != null)
        {
            rosterChangeListener.processPendingChanges();
        }
        else
        {
            logger.warn("rosterChangeListener is null - this is expected if " +
                                              "jabber has become unregistered");
        }

        // Workaround for DATA_LIST being requested before we have received all messages
        // from the server. Sending DATA_LIST to Electron again to ensure most up-to-date
        // data when we start up the app. Send dummy object to ensure null catching logic
        // is not hit.
        WISPAService wispaService = JabberActivator.getWispaService();
        wispaService.notify(WISPANamespace.MESSAGING, WISPAAction.DATA_LIST, new Object());
    }

    /**
     * Cleanups references and listeners.
     */
    void cleanup()
    {
        logger.info("Cleanup " + this);

        synchronized (imageRetrieverLock)
        {
            if (imageRetriever != null)
            {
                imageRetriever.quit();
            }
        }

        if (roster != null)
        {
            roster.removeRosterListener(rosterChangeListener);
        }

        rosterChangeListener = null;
        roster = null;
    }

    /**
     * When the protocol is online this method is used to fill or resolve
     * the current contact list.
     */
    @VisibleForTesting
    void initRoster()
    {
        logger.info("Initializing jabber roster");

        // Get a list of all of the chat room IDs that we have stored in
        // config.  We'll delete any that we don't find in the roster, as this
        // indicates that we've already left this chat room on another client.
        List<Jid> chatRoomIdsToRemove =
                ConfigurationUtils.getAllChatRoomIds(jabberProvider);

        if (roster.getEntryCount() > 0)
        {
            int contactsAdded = 0;

            for (RosterEntry item : roster.getEntries())
            {
                final BareJid userID = item.getJid();

                if (userID != null &&
                    userID.toString().startsWith(OperationSetMultiUserChat.CHATROOM_ID_PREFIX))
                {
                    logger.debug("Found roster entry for chat room " + sanitisePeerId(userID));
                    // We've found a chat room in the roster.  We need to join
                    // the chat room and add its details to our config.
                    chatRoomIdsToRemove.remove(userID);
                    chatRoomManager.chatRoomReceivedFromRoster(userID);
                }
                else
                {
                    contactLogger.debug(
                            "Found roster entry for contact " + sanitisePeerId(userID));
                    ContactJabberImpl contact = findContactById(userID);

                    // some services automatically add contacts from their
                    // addressbook to the roster and those contacts are
                    // with subscription none. If such already exist,
                    // remove them. This is typically our own contact
                    if (!isEntryDisplayable(item))
                    {
                        if (contact != null)
                        {
                            removeContactLocally(contact);
                        }
                        continue;
                    }

                    if (contact == null)
                    {
                        contactLogger.debug(
                                "Adding new contact for " + sanitisePeerId(userID));

                        // if there is no such contact create it
                        contact = new ContactJabberImpl(item, this, true, true);
                        mRootGroup.addContact(contact);

                        contactsAdded++;
                        fireContactAdded(mRootGroup, contact);
                    }
                    else
                    {
                        contactLogger.debug(
                                "Resolving existing contact for " + sanitisePeerId(userID));

                        // if contact exist so resolve it
                        contact.setResolved(item);

                        //fire an event saying that the unfiled contact has been
                        //resolved
                        fireContactResolved(mRootGroup, contact);
                    }

                    if (roster != null)
                    {
                        processPresenceInfo(roster.getPresences(userID).iterator());
                    }
                    else
                    {
                        logger.warn("Returning as roster has become null - this " +
                                          "is expected if jabber has become unregistered");
                        // Return as it doesn't make sense to continue trying to
                        // process roster entries if the roster has become null.
                        return;
                    }
                }
            }

            if (contactsAdded > 0)
            {
                JabberActivator.getInsightsService().logEvent(
                        InsightsEventHint.CONTACT_IM_INSERT.name(),
                        Map.of(CONTACT_ADD_COUNT.name(), contactsAdded));
            }
        }

        // Any chat room ids that are still in the list were not found on the
        // server so need to be marked as left in history and removed from
        // config.
        logger.info("About to leave " + chatRoomIdsToRemove.size() + " chat rooms");
        for (Jid chatRoomIdToRemove : chatRoomIdsToRemove)
        {
            chatRoomManager.chatRoomLeftFromRoster(chatRoomIdToRemove);
        }

        // Tell the chat room manager that we have now finished sending it chat
        // rooms to join and leave based on the initial roster.
        chatRoomManager.rosterProcessed();

        // now search all root contacts for unresolved ones
        removeUnresolvedContacts();
    }

    /**
     * Remove our local view of a contact - to be called when we are told by the server that it has
     * removed the contact from its list, or we have a contact that doesn't (and shouldn't) exist on
     * the server.  This function must only be called for contacts whose parent contact group is an
     * AbstractContactGroupJabberImpl.
     * @param contact
     */
    private void removeContactLocally(ContactJabberImpl contact)
    {
        // Remove the avatar from the cache before sending the contact removed event, as the latter will
        // stop us being able to update the metacontact's avatar to blank.
        AvatarCacheUtils.deleteCachedAvatar(contact);

        AbstractContactGroupJabberImpl parent = (AbstractContactGroupJabberImpl) contact.getParentContactGroup();
        if (parent == null)
        {
            logger.debug("Parent contact group not found, contact already removed");
            return;
        }

        parent.removeContact(contact);
        fireContactRemoved(parent, contact);

        if (parent instanceof ContactGroupJabberImpl)
        {
            ContactGroupJabberImpl groupImpl = (ContactGroupJabberImpl) parent;

            // if the group is empty remove it from
            // root group. This group will be removed
            // from server if empty
            if (groupImpl.countContacts() == 0)
            {
                mRootGroup.removeSubGroup(groupImpl);
                fireGroupEvent(groupImpl, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
            }
        }
    }

    /**
     * Dispatches presence status changed for each item in the iterator provided
     * @param presenceIterator collection of presence items
     */
    private void processPresenceInfo(Iterator<Presence> presenceIterator)
    {
        while (presenceIterator.hasNext())
        {
            Presence presence = presenceIterator.next();
            parentOperationSet.firePresenceStatusChanged(presence);
        }
    }

    /**
     * Removes contacts that existed locally but weren't on the roster
     */
    private void removeUnresolvedContacts()
    {
        Iterator<Contact> iter = mRootGroup.contacts();
        List<ContactJabberImpl> contactsToRemove = new ArrayList<>();

        while (iter.hasNext())
        {
            ContactJabberImpl contact = (ContactJabberImpl)iter.next();
            if (!contact.isResolved())
            {
                contactsToRemove.add(contact);
            }
        }

        // Create a duplicate list with members cast to `Contact` to avoid casting Exceptions
        List<Contact> contactsToRemoveDuplicate = new ArrayList<>(contactsToRemove);
        JabberActivator
                .getInsightsService()
                .logEvent(InsightsEventHint.COMMON_HINT_CONTACT_DELETED.name(),
                          Map.of(SipParameterInfo.CONTACTS_DELETED.name(),
                                 contactsToRemoveDuplicate));

        for (ContactJabberImpl contact : contactsToRemove)
        {
            // No need to hash Contact, as its toString() method does that.
            contactLogger.debug("Removing unresolved jabber contact " + contact);
            removeContactLocally(contact);
        }
    }

    /**
     * Returns the volatile group
     *
     * @return ContactGroupJabberImpl
     */
    private ContactGroupJabberImpl getNonPersistentGroup()
    {
        for (int i = 0; i < getRootGroup().countSubgroups(); i++)
        {
            ContactGroupJabberImpl gr =
                (ContactGroupJabberImpl)getRootGroup().getGroup(i);

            if(!gr.isPersistent())
                return gr;
        }

        return null;
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * added event.
     * @param parentGroup the group where the new contact was added
     * @param contact the contact that was added
     */
    void fireContactAdded( ContactGroup parentGroup,
                           ContactJabberImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        // if we are already registered(roster != null) and we are currently
        // creating the contact list, presences maybe already received
        // before we have created the contacts, so lets check
        if(roster != null)
        {
            processPresenceInfo(roster.getPresences(contact.getAddressAsJid()
                .asBareJid()).iterator());
        }

        // dispatch
        parentOperationSet.fireSubscriptionEvent(contact, parentGroup,
            SubscriptionEvent.SUBSCRIPTION_CREATED);
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * resolved event.
     * @param parentGroup the group that the resolved contact belongs to.
     * @param contact the contact that was resolved
     */
    void fireContactResolved( ContactGroup parentGroup,
                              ContactJabberImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        // if we are already registered(roster != null) and we are currently
        // creating the contact list, presences maybe already received
        // before we have created the contacts, so lets check
        if(roster != null)
        {
            processPresenceInfo(roster.getPresences(contact.getAddressAsJid()
                .asBareJid()).iterator());
        }

        // dispatch
        parentOperationSet.fireSubscriptionEvent(contact, parentGroup,
            SubscriptionEvent.SUBSCRIPTION_RESOLVED);
    }

    /**
     * when there is no image for contact we must retrieve it
     * add contacts for image update
     *
     * @param c ContactJabberImpl
     */
    protected void addContactForImageUpdate(ContactJabberImpl c)
    {
        ImageRetriever local = null;

        synchronized (imageRetrieverLock)
        {
            if (imageRetriever == null)
            {
                imageRetriever = new ImageRetriever(jabberProvider.getAccountID());
                imageRetriever.start();
            }

            local = imageRetriever;
        }

        local.addContact(c);
    }

    /**
     * Some roster entries are not supposed to be seen.
     * Like some services automatically add contacts from their
     * addressbook to the roster and those contacts are with subscription none.
     * Best practices in XEP-0162.
     * - subscription='both' or subscription='to'
     * - ((subscription='none' or subscription='from') and ask='subscribe')
     * - ((subscription='none' or subscription='from')
     *          and (name attribute or group child))
     *
     * @param entry the entry to check.
     *
     * @return is item to be hidden/ignored.
     */
    static boolean isEntryDisplayable(RosterEntry entry)
    {
        if(entry.getType() == RosterPacket.ItemType.both
           || entry.getType() == RosterPacket.ItemType.to)
        {
            return true;
        }
        else if((entry.getType() == RosterPacket.ItemType.none
                    || entry.getType() == RosterPacket.ItemType.from)
                && entry.isSubscriptionPending())
        {
            return true;
        }

        return false;
    }

    /**
     * Receives changes in roster.
     */
    private class ChangeListener
        implements RosterListener
    {
        /**
         * Received event when entry is added to the server stored list
         * @param ids Collection of ids that have been added
         */
        public void entriesAdded(Collection<Jid> ids)
        {
            logger.trace("entriesAdded " + sanitiseValuesInList(ids, PrivacyUtils::sanitisePeerId));

            RosterChangeType.ENTRY_ADDED.onRosterEntryChange(
                this, ids, ServerStoredContactListJabberImpl.this);
        }

        /**
         * Processes a new entry that has been added to the roster.
         *
         * @param id the id of the entry that has been added to the roster.
         */
        private void handleEntryAdded(Jid id)
        {
            String idString = id.toString();
            if (idString.startsWith(OperationSetMultiUserChat.CHATROOM_ID_PREFIX))
            {
                logger.info("Handling added chatroom roster entry " + sanitisePeerId(idString));
                if (ConfigurationUtils.isMultiUserChatEnabled())
                {
                    chatRoomManager.chatRoomReceivedFromRoster(id);
                }
                else
                {
                    logger.warn(
                        "Not adding chat room as multi-user chat is disabled: " + sanitisePeerId(idString));
                }
            }
            else
            {
                logger.info("Handling added roster entry " + sanitisePeerId(idString));
                addEntryToContactList(id);
            }
        }

        /**
         * Adds the entry to our local contactlist.
         * If contact exists and is persistent but not resolved, we resolve it
         * and return it without adding new contact.
         * If the contact exists and is not persistent, we remove it, to
         * avoid duplicate contacts and add the new one.
         * All entries must be displayable before we done anything with them.
         *
         * @param rosterEntryID the entry id.
         * @return the newly created contact.
         */
        private ContactJabberImpl addEntryToContactList(Jid rosterEntryID)
        {
            logger.info("Adding entry to contact list " +
                sanitisePeerId(rosterEntryID));

            if (roster == null)
            {
                logger.warn("Roster is null - this is expected if jabber " +
                            "has become unregistered");
                return null;
            }

            RosterEntry entry = roster.getEntry(rosterEntryID.asBareJid());

            if(!isEntryDisplayable(entry))
                return null;

            // Note there is no need to hash Contact in logs, as its
            // toString() method does that.
            ContactJabberImpl contact =
                findContactById(entry.getJid());

            if(contact != null)
            {
                if(contact.isPersistent())
                {
                    contactLogger.debug(contact, "Marking contact as resolved");
                    contact.setResolved(entry);
                    return contact;
                }
                else if(contact instanceof VolatileContactJabberImpl)
                {
                    ContactGroup oldParentGroup =
                        contact.getParentContactGroup();

                    // if contact is in 'not in contact list'
                    // we must remove it from there in order to correctly
                    // process adding contact
                    // this happens if we accept subscribe request
                    // not from sip-communicator
                    if(oldParentGroup instanceof ContactGroupJabberImpl
                        && !oldParentGroup.isPersistent())
                    {
                        contactLogger.debug(contact,
                            "Removing contact from parent group: " +
                            oldParentGroup);
                        removeContactLocally(contact);
                    }
                    else
                    {
                        contactLogger.debug(contact,
                            "Parent is: " + oldParentGroup);
                    }
                }
                else
                {
                    contactLogger.debug(contact,
                        "Finished adding to entry list");
                    return contact;
                }
            }

            contact = new ContactJabberImpl(
                    entry,
                    ServerStoredContactListJabberImpl.this,
                    true,
                    true);

            Collection<RosterGroup> groups = entry.getGroups();
            if (groups != null)
            {
                int numGroups = groups.size();
                if (numGroups != 0)
                {
                    logger.warn("Roster entry " + sanitisePeerId(entry.getJid()) +
                                " is in " + numGroups + " contact groups");
                }
            }

            contactLogger.debug(contact, "Adding new contact to root group");
            mRootGroup.addContact(contact);
            handleContactAdded(mRootGroup, contact);

            return contact;
        }

        /**
         * Handles a new jabber contact being added by moving it to any exising
         * corresponding MetaContact and firing a relevant contact event.
         *
         * @param parentGroup The group where the new contact was added.
         * @param contact The contact being added.
         */
        private void handleContactAdded(ContactGroup parentGroup,
                                        ContactJabberImpl contact)
        {
            boolean contactEventFired = false;
            Contact bgContact =
                parentOperationSet.findBGContactForJabberContact(contact);

            // If the jabber contact has a corresponding BG contact, try to
            // find the BG contact's MetaContact to add the jabber contact to.
            if (bgContact != null)
            {
                MetaContactListService metaContactListService =
                    JabberActivator.getMetaContactListService();

                MetaContact metaContact =
                    metaContactListService.findMetaContactByContact(bgContact);

                if (metaContact != null)
                {
                    Contact jabberContact = metaContact.getIMContact();

                    // Don't add the jabber conact to the MetaContact if it
                    // already contains a jabber contact.
                    if (jabberContact == null)
                    {
                        // No need to hash Contact or MetaContact, as their
                        // toString() methods do that.
                        logger.debug("Adding new contact " +
                                     contact +
                                     " to MetaContact " +
                                     metaContact);
                        metaContactListService.moveContact(contact, metaContact);
                        contactEventFired = true;
                    }
                }
            }

            // Only fire a contact added event if we didn't already move the
            // jabber contact to an exising MetaContact, as doing that will
            // already have fired a contact event.
            if (!contactEventFired)
            {
                fireContactAdded(parentGroup, contact);
            }
        }

        /**
         * Event when an entry is updated. Something for the entry data
         * or have been added to a new group or removed from one
         * @param ids Collection of ids that have been updated
         */
        public void entriesUpdated(Collection<Jid> ids)
        {
            logger.trace(ids.size() + " entriesUpdated");

            RosterChangeType.ENTRY_UPDATED.onRosterEntryChange(
                this, ids, ServerStoredContactListJabberImpl.this);
        }

        /**
         * Processes an entry that has been updated in the roster.
         *
         * @param id the id of the entry that has been updated in the roster.
         */
        private void handleEntryUpdated(Jid id)
        {
            String idString = id.toString();
            logger.info("Handling updated roster entry " + sanitisePeerId(idString));

            if (idString.startsWith(OperationSetMultiUserChat.CHATROOM_ID_PREFIX))
            {
                // Nothing to do for chat room ID updates
                return;
            }

            RosterEntry entry = roster.getEntry(id.asBareJid());

            ContactJabberImpl contact = addEntryToContactList(id);

            // check for change in display name
            checkForRename(entry.getName(), contact);

            for (RosterGroup gr : entry.getGroups())
            {
                if(findContactGroup(gr.getName()) == null)
                {
                    // such group does not exist. so it must be
                    // renamed one
                    ContactGroupJabberImpl group =
                        findContactGroupByNameCopy(gr.getName());
                    if(group != null)
                    {
                        // just change the source entry
                        group.setSourceGroup(gr);

                        fireGroupEvent(group,
                               ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
                    }
                    else
                    {
                        // strange ???
                    }
                }
                else
                {
                    // the group is found the contact may be moved from
                    // one group to another
                    ContactGroup contactGroup =
                        contact.getParentContactGroup();

                    if(!gr.getName().equals(contactGroup.getGroupName()))
                    {
                        // the contact is moved to another group
                        // first remove it from the original one
                        if (contactGroup instanceof AbstractContactGroupJabberImpl)
                        {
                            ((AbstractContactGroupJabberImpl)contactGroup).removeContact(contact);
                        }

                        // the add it to the new one
                        ContactGroupJabberImpl newParentGroup =
                            findContactGroup(gr.getName());

                        newParentGroup.addContact(contact);

                        fireContactMoved(contactGroup,
                                         newParentGroup,
                                         contact);
                    }
                    else
                    {
                        // check for change in display name

                        checkForRename(entry.getName(), contact);
                    }
                }
            }
        }

        /**
         * Checks the entry and the contact whether the display name has changed.
         * @param newValue new display name value
         * @param contact the contact to check
         */
        private void checkForRename(String newValue,
                                    ContactJabberImpl contact)
        {
            // check for change in display name
            if((newValue != null && contact != null)
               && !newValue.equals(
                    contact.getServerDisplayName()))
            {
                String oldValue = contact.getServerDisplayName();
                contact.setServerDisplayName(newValue);
                parentOperationSet.fireContactPropertyChangeEvent(
                    ContactPropertyChangeEvent.PROPERTY_DISPLAY_NAME,
                    contact, oldValue, newValue);
            }
        }

        /**
         * Event received when entry has been removed from the list
         * @param ids Collection of ids that have been removed
         */
        public void entriesDeleted(Collection<Jid> ids)
        {
            logger.trace(ids.size() + "entries deleted");

            RosterChangeType.ENTRY_DELETED.onRosterEntryChange(
                this, ids, ServerStoredContactListJabberImpl.this);
        }

        /**
         * Processes an entry that has been deleted from the roster.
         *
         * @param id the id of the entry that has been deleted from the roster.
         */
        private void handleEntryDeleted(Jid id)
        {
            String idString = id.toString();
            if (idString.startsWith(OperationSetMultiUserChat.CHATROOM_ID_PREFIX))
            {
                logger.info("Handling deleted chatroom roster entry " + sanitisePeerId(id));
                if (ConfigurationUtils.isMultiUserChatEnabled())
                {
                    chatRoomManager.chatRoomLeftFromRoster(id);
                }
                else
                {
                    logger.warn(
                        "Not deleting chat room as multi-user chat is disabled: " + sanitisePeerId(id));
                }
            }
            else
            {
                logger.info("Handling deleted roster entry " + sanitisePeerId(id));
                ContactJabberImpl contact = findContactById(id);

                if(contact == null)
                {
                    logger.trace("Could not find contact for deleted entry:"
                                    + sanitisePeerId(id));
                    return;
                }

                ContactGroup group = contact.getParentContactGroup();

                if(group == null)
                {
                    logger.trace("Could not find ParentGroup for deleted entry:"
                                    + sanitisePeerId(id));
                    return;
                }

                if (group instanceof AbstractContactGroupJabberImpl)
                {
                    removeContactLocally(contact);
                }
            }
        }

        /**
         * Processes any additions, deletions or updates to the roster that
         * were received while we were still processing the roster that we
         * received on start-up.
         */
        private void processPendingChanges()
        {
            synchronized (pendingRosterChanges)
            {
                logger.info("Processing pending changes");
                rosterProcessed = true;

                for (PendingRosterChange pendingRosterChange : pendingRosterChanges)
                {
                    Jid id = pendingRosterChange.getId();
                    RosterChangeType type = pendingRosterChange.getChangeType();
                    type.handleEntry(this, id);
                }

                pendingRosterChanges.clear();
            }

            // Now that we've finished processing pending roster changes, we
            // can process stored presence updates.
            mPresenceChangeListener.processStoredEvents();
        }

        /**
         * Not used here.
         * @param presence
         */
        public void presenceChanged(Presence presence)
        {}
    }

    /**
     * A class used to store details of a roster change that is received
     * while we're still processing the initial roster that we request on
     * start-up.
     */
    private static class PendingRosterChange
    {
        /**
         * The id of the roster entry that has changed.
         */
        private final Jid mId;

        /**
         * The type of roster change
         */
        private final RosterChangeType mChangeType;

        /**
         * Creates a new PendingRosterChange instance.
         *
         * @param id the id of the roster entry that has changed.
         * @param changeType the type of roster change.
         */
        public PendingRosterChange(Jid id, RosterChangeType changeType)
        {
            mId = id;
            mChangeType = changeType;
        }

        /**
         * @return the id of the roster entry that has changed.
         */
        public Jid getId()
        {
            return mId;
        }

        /**
         * @return the type of roster change.
         */
        public RosterChangeType getChangeType()
        {
            return mChangeType;
        }
    }

    /**
     * Thread retrieving images.
     */
    private class ImageRetriever
        extends Thread
    {
        private ImageRetriever(AccountID account)
        {
            super("Jabber ImageRetriever for: " + account.getLoggableAccountID());
        }

        /**
         * list with the accounts with missing image
         */
        private final List<ContactJabberImpl> contactsForUpdate
            = new Vector<>();

        /**
         * Should we stop.
         */
        private boolean running = false;

        /**
         * Thread entry point.
         */
        public void run()
        {
            try
            {
                Collection<ContactJabberImpl> copyContactsForUpdate = null;
                running = true;
                while (running)
                {
                    synchronized(contactsForUpdate)
                    {
                        if(contactsForUpdate.isEmpty())
                            contactsForUpdate.wait();

                        if(!running)
                            return;

                        copyContactsForUpdate
                            = new Vector<>(contactsForUpdate);
                        contactsForUpdate.clear();
                    }

                    Iterator<ContactJabberImpl> iter
                        = copyContactsForUpdate.iterator();
                    while (iter.hasNext())
                    {
                        ContactJabberImpl contact = iter.next();

                        BufferedImageFuture imgBytes = getAvatar(contact);

                        if(imgBytes != null)
                        {
                            BufferedImageFuture oldImage = contact.getImage(false);

                            contact.setImage(imgBytes);
                            parentOperationSet.fireContactPropertyChangeEvent(
                                ContactPropertyChangeEvent.PROPERTY_IMAGE,
                                contact, oldImage, imgBytes);
                        }
                        else
                            // set an empty image data so it won't be queried again
                            contact.setImage(null);
                    }
                }
            }
            catch (InterruptedException ex)
            {
                logger.error("ImageRetriever error waiting will stop now!", ex);
            }
        }

        /**
         * Add contact for retrieving
         * if the provider is register notify the retriever to get the nicks
         * if we are not registered add a listener to wait for registering
         *
         * @param contact ContactJabberImpl
         */
        synchronized void addContact(ContactJabberImpl contact)
        {
            synchronized(contactsForUpdate)
            {
                if (!contactsForUpdate.contains(contact))
                {
                    contactsForUpdate.add(contact);
                    contactsForUpdate.notifyAll();
                }
            }
        }

        /**
         * Stops this thread.
         */
        void quit()
        {
            synchronized(contactsForUpdate)
            {
                running = false;
                contactsForUpdate.notifyAll();
            }
        }

        /**
         * Retrieves the avatar.
         * @param contact the contact.
         * @return the contact avatar.
         */
        private BufferedImageFuture getAvatar(ContactJabberImpl contact)
        {
            BufferedImageFuture result = null;
            try
            {
                Iterator<ServerStoredDetails.GenericDetail> iter =
                    infoRetreiver.getDetails(contact.getAddress(),
                    ServerStoredDetails.ImageDetail.class);

                if(iter.hasNext())
                {
                    ServerStoredDetails.ImageDetail imgDetail =
                        (ServerStoredDetails.ImageDetail)iter.next();
                    result = imgDetail.getImage();
                }

                return result;
            }
            catch (Exception ex)
            {
                // No need to hash Contact, as its toString() method does that.
                logger.debug(
                        "Cannot load image for contact "
                            + contact
                            + ": "
                            + ex.getMessage(),
                        ex);

                result = null;
            }

            return result;
        }
    }

    /**
     * Completes the identifier with the server part if no server part was
     * previously added.
     *
     * @param id the initial identifier as added by the user
     */
    private EntityBareJid parseAddressString(String id)
        throws OperationFailedException
    {
        try
        {
            Jid temp = JidCreate.from(id);

            if (!temp.hasLocalpart())
            {
                AccountID accountID = jabberProvider.getAccountID();

                EntityBareJid accountJid =
                    JidCreate.entityBareFromOrNull(accountID.getUserID());

                Domainpart domainpart =
                    accountJid != null ? accountJid.getDomain() :
                        Domainpart.from(accountID.getService());

                // If we have domain/resource as the id this will throw as it
                // tries to parse domain/resource as a localpart and is caught
                // a cropper by the "/".
                return JidCreate.entityBareFrom(Localpart.from(id), domainpart);
            }

            return temp.asEntityBareJidOrThrow();
        }
        catch (XmppStringprepException ex)
        {
            throw new OperationFailedException("Could not parse id into address", 0, ex);
        }
    }

    @Override
    public String getStateDumpName()
    {
        return "JabberContacts";
    }

    @Override
    public String getState()
    {
        StringBuilder state = new StringBuilder();

        XMPPConnection connection = jabberProvider.getConnection();
        state.append("Jabber connection: ");
        if (connection != null)
        {
            state.append(connection.getClass().getSimpleName())
                    .append("[")
                    .append(connection.getUser() == null ? "not-authenticated" : sanitisePeerId(connection.getUser()))
                    .append("]");
            state.append("\nConnected? ").append(connection.isConnected());
        }
        else
        {
            state.append("null");
        }

        state.append("\n\nContact name, Contact JID, Subscription pending?, Subscription type\n");

        if (roster != null)
        {
            for (RosterEntry entry : roster.getEntries())
            {
                state.append(logHasher(entry.getName()))
                     .append(", ")
                     .append(sanitiseChatAddress(entry.getJid()))
                     .append(", ")
                     .append(entry.isSubscriptionPending())
                     .append(", ")
                     .append(entry.getType())
                     .append("\n");
            }
        }
        else
        {
            state.append("Null roster\n");
        }

        state.append("\nRoot group: ").append(mRootGroup).append("\n");

        if (mRootGroup != null)
        {
            stringifyGroup(state, mRootGroup);
        }

        return state.toString();
    }

    /**
     * Appends all the contact information within a group to the string builder
     * that is passed in.
     *
     * @param builder The string builder to add the data to
     * @param group The group to stringify
     */
    private void stringifyGroup(StringBuilder builder, ContactGroup group)
    {
        if (group == null)
        {
            builder.append("NULL GROUP");
            return;
        }

        builder.append(group.getGroupName());
        builder.append("\n\nContact name, Contact JID, Persistent?, Resolved?");

        Iterator<Contact> contacts = group.contacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            builder.append("\n")
                   .append(logHasher(contact.getDisplayName()))
                   .append(", ")
                   .append(sanitiseChatAddress(contact.getAddress()))
                   .append(", ")
                   .append(contact.isPersistent())
                   .append(", ")
                   .append(contact.isResolved());
        }

        builder.append("\n\n");
        Iterator<ContactGroup> groups = group.subgroups();
        while (groups.hasNext())
        {
            stringifyGroup(builder, groups.next());
        }
    }
}
