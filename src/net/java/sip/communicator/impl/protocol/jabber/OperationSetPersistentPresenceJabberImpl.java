/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.Hasher.logHasher;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smackx.nick.packet.Nick;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.diagnostics.DiagnosticsServiceRegistrar;
import net.java.sip.communicator.service.diagnostics.StateDumper;
import net.java.sip.communicator.service.imageloader.BufferedImageAvailableFromBytes;
import net.java.sip.communicator.service.protocol.AbstractOperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.AuthorizationHandler;
import net.java.sip.communicator.service.protocol.AuthorizationRequest;
import net.java.sip.communicator.service.protocol.AuthorizationResponse;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactGroup;
import net.java.sip.communicator.service.protocol.ContactGroupType;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.SubscriptionStatus;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.ContactPropertyChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.protocol.event.ServerStoredGroupListener;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;
import net.java.sip.communicator.service.protocol.jabberconstants.JabberStatusEnum;
import net.java.sip.communicator.service.protocol.jabberconstants.JabberStatusEnum.JabberPresenceStatus;
import net.java.sip.communicator.util.ContactLogger;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.util.StringUtils;

/**
 * The Jabber implementation of a Persistent Presence Operation set. This class
 * manages our own presence status as well as subscriptions for the presence
 * status of our buddies. It also offers methods for retrieving and modifying
 * the buddy contact list and adding listeners for changes in its layout.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class OperationSetPersistentPresenceJabberImpl
    extends AbstractOperationSetPersistentPresence<
                ProtocolProviderServiceJabberImpl>
{
    /**
     * The logger.
     */
    private static final Logger sLog =
        Logger.getLogger(OperationSetPersistentPresenceJabberImpl.class);

    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * Divider between the status message and any (optional) custom mood.
     * This must match what all clients (including mobile) expect to see - since
     * clients must split on this in order to obtain the (defined) status
     * separately from any user-defined custom mood.
     * DON'T CHANGE THIS WITHOUT CONSIDERING INTEROP WITH BACK-LEVEL AND MOBILE
     * CLIENTS!
     */
    private static final String CUSTOM_MOOD_DIVIDER = " - ";

    /**
     * The resource string that will be used in the from address of presence
     * updates from non-Accession phones sent by AMS when the contact is not
     * also using an Accession phone.
     */
    public static final String EXT_CLIENT_RESOURCE = "ext_client";

    /**
     * The resource string that will be used in the from address of presence
     * updates from non-Accession phones sent by AMS when the contact is also
     * using an Accession phone.
     */
    public static final String AUX_CLIENT_RESOURCE = "aux_client";

    /**
     * Status string sent by AMS to indicate that an LSM line has unregistered.
     */
    private static final String UNREGISTERED_LSM_STATUS = "Unregistered";

    /**
     * String format for logging the time that a presence was recorded.
     * @see ContactChangesListener.ProcessedPresence
     */
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd HH:mm:ss");

    /**
     * Contains our current status message. Note that this field would only
     * be changed once the server has confirmed the new status message and
     * not immediately upon setting a new one.
     */
    private String currentStatusMessage = "";

    /**
     * The presence status that we were last notified of entering.
     * The initial one is OFFLINE
     */
    private PresenceStatus currentStatus;

    /**
     * Remember the last status message that we sent to the server, so that
     * we can avoid repeatedly sending the same message.
     */
    private String mLastBaseStatusMessage = "";

    /**
     * A map containing bindings between SIP Communicator's jabber presence
     * status instances and Jabber status codes.<br>
     * Note that there are multiple jabber impl strings that map to 'extended
     * away' (such as 'on the phone' or 'in a meeting').
     */
    private static Map<String, Presence.Mode> scToJabberModesMappings
        = new Hashtable<>();
    static
    {
        scToJabberModesMappings.put(JabberStatusEnum.AWAY,
                                    Presence.Mode.away);
        scToJabberModesMappings.put(JabberStatusEnum.ON_THE_PHONE,
                                    Presence.Mode.xa);
        scToJabberModesMappings.put(JabberStatusEnum.IN_MEETING,
                                    Presence.Mode.xa);
        scToJabberModesMappings.put(JabberStatusEnum.BUSY,
                                    Presence.Mode.xa);
        scToJabberModesMappings.put(JabberStatusEnum.EXTENDED_AWAY,
                                    Presence.Mode.xa);
        scToJabberModesMappings.put(JabberStatusEnum.DO_NOT_DISTURB,
                                    Presence.Mode.dnd);
        scToJabberModesMappings.put(JabberStatusEnum.FREE_FOR_CHAT,
                                    Presence.Mode.chat);
        scToJabberModesMappings.put(JabberStatusEnum.AVAILABLE,
                                    Presence.Mode.available);
    }

    /**
     * Map from Jabber status codes to Accession's jabber impl status strings.
     * Note that 'extended away' could map to multiple jabber impl strings
     * (such as 'on the phone' or 'in a meeting'); this map returns the more
     * generic 'busy' for all of these.
     */
    private static final Map<Presence.Mode, String> scPresenceModeToJabberMappings
        = new Hashtable<>();
    static
    {
        scPresenceModeToJabberMappings.put(Presence.Mode.available,
                                           JabberStatusEnum.AVAILABLE);
        scPresenceModeToJabberMappings.put(Presence.Mode.away,
                                           JabberStatusEnum.AWAY);
        scPresenceModeToJabberMappings.put(Presence.Mode.chat,
                                           JabberStatusEnum.FREE_FOR_CHAT);
        scPresenceModeToJabberMappings.put(Presence.Mode.dnd,
                                           JabberStatusEnum.DO_NOT_DISTURB);
        scPresenceModeToJabberMappings.put(Presence.Mode.xa,
                                           JabberStatusEnum.BUSY);
    }

    /**
     * The server stored contact list that will be encapsulating smack's
     * buddy list.
     */
    private final ServerStoredContactListJabberImpl ssContactList;

    /**
     * The engine used to add and delete IM contacts to correspond to BG
     * contacts when the client is using CommPortal-provisioned IM.
     */
    private final BGIMContactManager bgIMContactManager;

    /**
     * Listens for subscriptions.
     */
    private JabberSubscriptionListener subscriptionStanzaListener = null;

    /**
     * Current resource priority. 10 is default value.
     */
    private int resourcePriority = 10;

    /**
     * Manages statuses and different user resources.
     */
    private ContactChangesListener contactChangesListener = null;

    /**
     * Manages the presence extension to advertise the SHA-1 hash of this
     * account avatar as defined in XEP-0153.
     */
    private VCardTempXUpdatePresenceExtension vCardTempXUpdatePresenceExtension
        = null;

    /**
     * The presence status to use for contacts who are either offline or who
     * we are waiting to authorize us to see their presence.
     */
    private final PresenceStatus offlineStatus = parentProvider.
        getJabberStatusEnum().getStatus(JabberStatusEnum.OFFLINE_STATUS);

    /**
     * The presence status to use for contacts who are offline and we know that
     * they are not using an Accession phone.
     */
    private final PresenceStatus offlineLsmStatus = parentProvider.
        getJabberStatusEnum().getStatus(JabberStatusEnum.OFFLINE_LSM_STATUS);

    /**
     * Set to true once the roster has loaded.
     */
    private boolean rosterLoaded = false;

    /**
     * A list of presence packets that need to be processed once we've finished
     * loading contacts from the server's roster.
     */
    private final List<Presence> pendingPresenceRequests = new ArrayList<>();

    /**
     * A list of unsubscribes that need to be processed once we've finished
     * loading contacts from the server's roster.  Note that we should be able
     * to get away without an equivalent for subscribes as we should still be
     * able to add new entries to the roster while it isn't fully loaded.
     */
    private final List<Contact> pendingUnsubRequests = new ArrayList<>();

    /**
     * An object to provide thread safety on the above three variables.  Any
     * user must have this lock before accessing them.
     */
    private final Object pendingUpdatesLock = new Object();

    private final RegistrationStateListener registrationStateListener;

    /**
     * Creates the OperationSet.
     * @param provider the parent provider.
     * @param infoRetreiver retrieve contact information.
     */
    public OperationSetPersistentPresenceJabberImpl(
        ProtocolProviderServiceJabberImpl provider,
        InfoRetreiver infoRetreiver)
    {
        super(provider);
        sLog.info("Create OperationSetPersistentPresenceJabberImpl " + this);

        currentStatus =
            parentProvider.getJabberStatusEnum().getStatus(
                JabberStatusEnum.OFFLINE_STATUS);

        ssContactList = new ServerStoredContactListJabberImpl(
            this , provider, infoRetreiver);

        DiagnosticsServiceRegistrar.registerStateDumper(ssContactList,
                                                 JabberActivator.bundleContext);

        registrationStateListener = new RegistrationStateListener();
        parentProvider.addRegistrationStateChangeListener(registrationStateListener);

        // We always create an instance of the BGIMContactManager here,
        // even if we are not using BG contacts and CommPortal-provisioned IM.
        // This is so that we are listening for BG contact events and are
        // keeping  a record of which work phone numbers correspond to BG
        // contacts, in case this config is enabled on scheduled config refresh.
        bgIMContactManager = new BGIMContactManager(provider);
    }

    /**
     * Registers a listener that would receive events upon changes in server
     * stored groups.
     *
     * @param listener a ServerStoredGroupChangeListener impl that would
     *   receive events upon group changes.
     */
    public void addServerStoredGroupChangeListener(ServerStoredGroupListener
        listener)
    {
        ssContactList.addGroupListener(listener);
    }

    /**
     * Creates a group with the specified name and parent in the server
     * stored contact list.
     *
     * @param parent the group where the new group should be created
     * @param groupName the name of the new group to create.
     * @throws OperationFailedException if such group already exists
     */
    public void createServerStoredContactGroup(ContactGroup parent,
                                               String groupName)
        throws OperationFailedException
    {
        assertConnected();

        if (!parent.canContainSubgroups())
           throw new IllegalArgumentException(
               "The specified contact group cannot contain child groups. Group:"
               + parent );

        ssContactList.createGroup(groupName);
    }

    /**
     * Creates a non-persistent contact for the specified address. This would
     * not create (if necessary) a group for volatile contacts or add them to
     * existing groups. Volatile contacts are not shown in the contact list.
     * @param id the address of the contact to create.
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    public ContactJabberImpl createVolatileContact(String id)
    {
        return createVolatileContact(id, null);
    }

    private ContactJabberImpl createVolatileContact(String id, String displayName)
    {
        ContactJabberImpl contact = ssContactList.createVolatileContact(id, displayName);
        // No need to hash Contact, as its toString() method does that.
        contactLogger.debug(contact, "createVolatileContact");
        return contact;
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     *   method during a previous run and that has been persistently stored
     *   locally.
     * @param parentGroup the group where the unresolved contact is supposed
     *   to belong to.
     * @return the unresolved <tt>Contact</tt> created from the specified
     *   <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData,
                                           ContactGroup parentGroup)
    {
        if(! (parentGroup instanceof ContactGroupJabberImpl ||
              parentGroup instanceof RootContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "Argument is not an jabber contact group (group="
                + parentGroup + ")");

        try
        {
        ContactJabberImpl contact =
            ssContactList.createUnresolvedContact(
                parentGroup,
                JidCreate.bareFrom(address));

        contact.setPersistentData(persistentData);
        // No need to hash Contact, as its toString() method does that.
        contactLogger.debug(contact, "createUnresolvedContact in group");

        return contact;
        }
        catch (XmppStringprepException e)
        {
            throw new IllegalArgumentException("Invalid JID", e);
        }
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     *   method during a previous run and that has been persistently stored
     *   locally.
     * @return the unresolved <tt>Contact</tt> created from the specified
     *   <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData)
    {
        Contact contact = createUnresolvedContact(  address
                                       , persistentData
                                       , getServerStoredContactListRoot());
        // No need to hash Contact, as its toString() method does that.
        contactLogger.debug(contact, "createUnresolvedContact with data");
        return contact;
    }

    /**
     * Creates and returns a unresolved contact group from the specified
     * <tt>address</tt> and <tt>persistentData</tt>.
     *
     * @param groupUID an identifier, returned by ContactGroup's
     *   getGroupUID, that the protocol provider may use in order to create
     *   the group.
     * @param persistentData a String returned ContactGroups's
     *   getPersistentData() method during a previous run and that has been
     *   persistently stored locally.
     * @param parentGroup the group under which the new group is to be
     *   created or null if this is group directly underneath the root.
     * @return the unresolved <tt>ContactGroup</tt> created from the
     *   specified <tt>uid</tt> and <tt>persistentData</tt>
     */
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        return ssContactList.createUnresolvedContactGroup(groupUID);
    }

    /**
     * Returns a reference to the contact with the specified ID in case we
     * have a subscription for it and null otherwise/
     *
     * @param contactID a String identifier of the contact which we're
     *   seeking a reference of.
     * @return a reference to the Contact with the specified
     *   <tt>contactID</tt> or null if we don't have a subscription for
     *   that identifier.
     */
    public Contact findContactByID(String contactID)
    {
        try
        {
            return ssContactList.findContactById(JidCreate.from(contactID));
        }
        catch (XmppStringprepException ex)
        {
            contactLogger.warn("Could not find contact for non-JID ID: " + logHasher(contactID));
            return null;
        }
    }

    /**
     * Returns a reference to the contact with the specified ID in case we
     * have a subscription for it and null otherwise/
     *
     * @param contactID a @Jid identifier of the contact which we're
     *   seeking a reference of.
     * @return a reference to the Contact with the specified
     *   <tt>contactID</tt> or null if we don't have a subscription for
     *   that identifier.
     */
    public Contact findContactByID(Jid contactID)
    {
        return ssContactList.findContactById(contactID);
    }

    /**
     * Returns the status message that was confirmed by the server. This is the
     * translated version of the status message.
     *
     * @return the last status message that we have requested and the aim
     *   server has confirmed.
     */
    public String getCurrentStatusMessage()
    {
        return currentStatusMessage;
    }

    /**
     * Returns a PresenceStatus instance representing the state this provider
     * is currently in.
     *
     * @return the PresenceStatus last published by this provider.
     */
    public PresenceStatus getPresenceStatus()
    {
        return currentStatus;
    }

    /**
     * Returns the server stored contact list.
     *
     * @return the ServerStoredContactListJabberImpl stored by this service.
     */
    public ServerStoredContactListJabberImpl getServerStoredContactList()
    {
        return ssContactList;
    }

    /**
     * Returns the root group of the server stored contact list.
     *
     * @return the root ContactGroup for the ContactList stored by this
     *   service.
     */
    public ContactGroup getServerStoredContactListRoot()
    {
        return ssContactList.getRootGroup();
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
        return parentProvider.getJabberStatusEnum().getSupportedStatusSet();
    }

    /**
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     *
     * @param contactToMove the <tt>Contact</tt> to move
     * @param newParent the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *   would be placed.
     * @throws IllegalArgumentException if the arguments are not
     * Jabber contact objects.
     */
    public void moveContactToGroup(Contact contactToMove,
                                   ContactGroup newParent)
            throws IllegalArgumentException
    {
        assertConnected();

        if( !(contactToMove instanceof ContactJabberImpl) )
        {
            throw new IllegalArgumentException(
                "The specified contact is not a jabber contact. " +
                contactToMove);
        }

        if (newParent.groupType() != ContactGroupType.JABBER)
        {
            throw new IllegalArgumentException(
                "The specified group is not a jabber contact group. "
                + newParent);
        }

        ssContactList.moveContact((ContactJabberImpl)contactToMove,
                                  (AbstractContactGroupJabberImpl)newParent);
    }

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified parameters.
     *
     * @param status the PresenceStatus as returned by
     *   getRequestableStatusSet
     * @param customStatusMessage the message that should be set as the reason
     *   to enter that status
     * @throws IllegalArgumentException if the status requested is not a
     *   valid PresenceStatus supported by this provider.
     * @throws IllegalStateException if the provider is not currently
     *   registered.
     */
    public void publishPresenceStatus(PresenceStatus status,
                                      String customStatusMessage)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        assertConnected();

        JabberStatusEnum jabberStatusEnum =
            parentProvider.getJabberStatusEnum();
        boolean isValidStatus = false;
        JabberPresenceStatus jabberStatus =
            jabberStatusEnum.getStatus(JabberStatusEnum.UNKNOWN_STATUS);

        for (Iterator<PresenceStatus> supportedStatusIter
                        = jabberStatusEnum.getSupportedStatusSet();
             supportedStatusIter.hasNext();)
        {
            PresenceStatus nextStatus = supportedStatusIter.next();

            if (nextStatus.equals(status))
            {
                try
                {
                    jabberStatus = (JabberPresenceStatus) nextStatus;
                    isValidStatus = true;
                }
                catch (ClassCastException ccex)
                {
                    sLog.error("Couldn't convert status to jabber status! " +
                                 nextStatus.getClass() + " - " + nextStatus);
                }
                break;
            }
        }
        if (!isValidStatus)
            throw new IllegalArgumentException(status
                + " is not a valid Jabber status");

        if (status.equals(jabberStatusEnum.getStatus(JabberStatusEnum.OFFLINE_STATUS)))
        {
            mLastBaseStatusMessage = "";
            parentProvider.unregister();
        }
        else
        {
            XMPPConnection connection = parentProvider.getConnection();
            PresenceBuilder presenceBuilder = connection.getStanzaFactory()
                                                        .buildPresenceStanza()
                                                        .ofType(Presence.Type.available);

            presenceBuilder.setMode(presenceStatusToJabberMode(status));
            presenceBuilder.setPriority(resourcePriority);

            // Set the status message. The smack library will make sure this is
            // XML safe
            String baseStatusMessage = jabberStatus.getBaseStatusName();
            baseStatusMessage = maybeAddCustomMood(baseStatusMessage, customStatusMessage);
            presenceBuilder.setStatus(baseStatusMessage);

            // Only send to the server if the status message or the avatar
            // has changed.  Otherwise we increase load on the server and all
            // clients in our roster.
            if (!baseStatusMessage.equals(mLastBaseStatusMessage) ||
                status.getAvatarChanged())
            {
                mLastBaseStatusMessage = baseStatusMessage;
                try
                {
                    connection.sendStanza(presenceBuilder.build());
                }
                catch (NotConnectedException | InterruptedException e)
                {
                    throw new OperationFailedException(
                        "Could not send new presence status",
                        OperationFailedException.NETWORK_FAILURE,
                        e
                    );
                }
                status.setAvatarChanged(false);
            }
        }

        fireProviderStatusChangeEvent(currentStatus, status);

        String translatedStatusMessage = jabberStatus.getStatusName();
        translatedStatusMessage = maybeAddCustomMood(translatedStatusMessage,
                                                     customStatusMessage);

        if (!getCurrentStatusMessage().equals(translatedStatusMessage))
        {
            String oldStatusMessage = getCurrentStatusMessage();
            currentStatusMessage = translatedStatusMessage;
            fireProviderStatusMessageChangeEvent(oldStatusMessage,
                                                 getCurrentStatusMessage());
        }
    }

    /**
     * If the custom mood is not empty, append it (and the appropriate divider)
     * to the passed-in status string to get a string like "Busy - writing
     * reviews".
     * @param statusMessage The status message
     * @param customStatusMessage The custom mood, which can be null or empty
     * @return String containing status message plus the custom mood, if any.
     * E.g. <tt>Online</tt> or <tt>Online - custom message</tt>.
     */
    private String maybeAddCustomMood(String statusMessage,
                                      String customStatusMessage)
    {
        if (!org.jitsi.util.StringUtils.isNullOrEmpty(customStatusMessage))
        {
            return statusMessage + CUSTOM_MOOD_DIVIDER + customStatusMessage;
        }
        else
        {
            return statusMessage;
        }
    }

    /**
     * Gets the <tt>PresenceStatus</tt> of a contact with a specific
     * <tt>String</tt> identifier.
     *
     * @param contactIdentifier the identifier of the contact whose status we're
     * interested in.
     * @return the <tt>PresenceStatus</tt> of the contact with the specified
     * <tt>contactIdentifier</tt>
     * @throws IllegalArgumentException if the specified
     * <tt>contactIdentifier</tt> does not identify a contact known to the
     * underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service
     */
    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException
    {
        /*
         * As stated by the javadoc, IllegalStateException signals that the
         * ProtocolProviderService is not registered.
         */
        assertConnected();

        XMPPConnection xmppConnection = parentProvider.getConnection();

        if (xmppConnection == null)
        {
            throw
                new IllegalArgumentException(
                        "The provider/account must be signed on in order to"
                            + " query the status of a contact in its roster");
        }

        BareJid contactJid;
        try
        {
            contactJid = JidCreate.bareFrom(contactIdentifier);
        }
        catch (XmppStringprepException e)
        {
            throw new IllegalArgumentException(
                "The contact was not a valid Jid.", e);
        }

        // Get all presence statuses associated with this contact identifier
        Iterator<Presence> it =
            Roster.getInstanceFor(xmppConnection)
                  .getPresences(contactJid)
                  .iterator();
        List<PresenceStatus> statuses = new ArrayList<>();
        while (it.hasNext())
        {
            statuses.add(jabberStatusToPresenceStatus(it.next(), parentProvider));
        }

        // Get the most important status, as defined by their natural ordering
        return Collections.max(statuses);
    }

    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to remove.
     */
    public void removeServerStoredContactGroup(ContactGroup group)
    {
        assertConnected();

        if( !(group instanceof ContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an jabber contact group: " + group);

        ssContactList.removeGroup(((ContactGroupJabberImpl)group));
    }

    /**
     * Removes the specified group change listener so that it won't receive
     * any further events.
     *
     * @param listener the ServerStoredGroupChangeListener to remove
     */
    public void removeServerStoredGroupChangeListener(ServerStoredGroupListener
        listener)
    {
        ssContactList.removeGroupListener(listener);
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
        assertConnected();

        if( !(group instanceof ContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an jabber contact group: " + group);

        ssContactList.renameGroup((ContactGroupJabberImpl)group, newName);
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
        if(subscriptionStanzaListener == null)
        {
            XMPPConnection connection = parentProvider.getConnection();

            if (connection != null)
            {
                sLog.info("Creating new subscriptionStanzaListener");
                subscriptionStanzaListener = new JabberSubscriptionListener();
                connection.addStanzaListener(subscriptionStanzaListener,
                                             new StanzaTypeFilter(Presence.class));
            }
            else
            {
                sLog.info(
                    "Unable to set authorizationhandler - provider not yet connected");
                return;
            }
        }

        sLog.info("Setting authorizationhandler on existing packet listener");
        subscriptionStanzaListener.handler = handler;
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
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   subscribing fails due to errors experienced during network
     *   communication
     */
    public void subscribe(ContactGroup parent, String contactIdentifier)
        throws IllegalArgumentException, IllegalStateException,
               OperationFailedException
    {
        assertConnected();

        if(! (parent instanceof ContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "Argument is not an jabber contact group (group="
                            + parent + ")");

        contactLogger.debug("subscribe id " + sanitisePeerId(contactIdentifier));
        ssContactList.addContact(parent, contactIdentifier);
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
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   subscribing fails due to errors experienced during network
     *   communication
     */
    public void subscribe(String contactIdentifier) throws
        IllegalArgumentException, IllegalStateException,
        OperationFailedException
    {
        assertConnected();

        contactLogger.debug(
                "subscribe with id " + sanitisePeerId(contactIdentifier));
        ssContactList.addContact(contactIdentifier);
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
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   unsubscribing fails due to errors experienced during network
     *   communication
     */
    public void unsubscribe(Contact contact) throws IllegalArgumentException,
        IllegalStateException, OperationFailedException
    {
        assertConnected();

        if(! (contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "Argument is not an jabber contact (contact=" + contact + ")");

        synchronized (pendingUpdatesLock)
        {
            // If the roster hasn't fully loaded, yet we can't just remove the
            // contact as we may not be able to find it.  We therefore just
            // store off the operation for later.
            if (rosterLoaded)
            {
                ssContactList.removeContactFromServer((ContactJabberImpl)contact);
            }
            else
            {
                // No need to hash Contact, as its toString() method does that.
                sLog.debug("Roster not yet loaded, storing for later " +
                             "processing: " + contact);
                pendingUnsubRequests.add(contact);
            }
        }
    }

    /**
     * Converts the specified jabber status to one of the status fields of the
     * JabberStatusEnum class.
     *
     * @param presence the Jabber Status
     * @param jabberProvider the parent provider.
     * @return a PresenceStatus instance representation of the Jabber Status
     * parameter. The returned result is one of the JabberStatusEnum fields.
     */
    public static PresenceStatus jabberStatusToPresenceStatus(
        Presence presence, ProtocolProviderServiceJabberImpl jabberProvider)
    {
        JabberStatusEnum jabberStatusEnum = jabberProvider.getJabberStatusEnum();
        int jabberStatusEnumVariant = -1;
        Presence.Mode mode = presence.getMode(); // Never null - see SFR 528024
        boolean isAvailable = presence.isAvailable();
        String statusMessage = presence.getStatus();
        Jid from = presence.getFrom();
        String resource = from.getResourceOrEmpty().toString();

        // Check whether this is a resource from a non-Accession phone so we
        // can decide whether to use the IM or LSM-only version of the presence.
        boolean isExtLsmPresence = EXT_CLIENT_RESOURCE.equals(resource);
        boolean isAuxLsmPresence = AUX_CLIENT_RESOURCE.equals(resource);
        boolean isLsmPresence = isExtLsmPresence || isAuxLsmPresence;

        statusMessage = statusMessage == null ? null : statusMessage.toLowerCase();

        boolean isOnPhone = statusMessage != null &&
                            (statusMessage.startsWith(JabberStatusEnum.ON_THE_PHONE.toLowerCase())
                             || statusMessage.startsWith(JabberStatusEnum.ON_THE_PHONE_LSM.toLowerCase()));

        // Translation is as follows:
        // * Mode Available means available or offline
        // * Mode Away means "away" - unless the contact is using an old client.
        //   In this case, the message might be set to "On the phone", in which
        //   case the user is on the phone.
        // * Mode DND means DND.
        // * Mode Extended Away has three possible meanings:
        //   1. On the phone
        //   2. Busy
        //   3. In a meeting
        //   These are distinguished by the status message
        switch (mode)
        {
        case available:
            // Available mode is the default, so need to also check the presence type
            // to see if user is actually available
            if (isAvailable)
            {
                if (isExtLsmPresence)
                {
                    // This presence is from a user who is not using an
                    // Accession phone, so use LSM only status.
                    jabberStatusEnumVariant = JabberStatusEnum.AVAILABLE_FOR_CALLS_LSM_STATUS;
                }
                else if (isAuxLsmPresence)
                {
                    // This presence indicates that the user is using both an
                    // Accession and a non-Accession phone, so use the
                    // 'Available for calls' IM status.
                    jabberStatusEnumVariant = JabberStatusEnum.AVAILABLE_FOR_CALLS_STATUS;
                }
                else
                {
                    // This presence indicates that the user is using only an
                    // Accession phone, so use the available IM status.
                    jabberStatusEnumVariant = JabberStatusEnum.AVAILABLE_STATUS;
                }
            }
            else
            {
                // The user is offline. Non-Accession phones do not signal an
                // unavailable status, so we can just set the offline IM
                // status.
                jabberStatusEnumVariant = JabberStatusEnum.OFFLINE_STATUS;
            }
            break;

        case away:
            if (isOnPhone)
            {
                jabberStatusEnumVariant = isLsmPresence ?
                    JabberStatusEnum.ON_THE_PHONE_LSM_STATUS : JabberStatusEnum.ON_THE_PHONE_STATUS;
            }
            else
            {
                jabberStatusEnumVariant = JabberStatusEnum.AWAY_STATUS;
            }
            break;

        case chat:
            jabberStatusEnumVariant = JabberStatusEnum.FREE_FOR_CHAT_STATUS;
            break;

        case dnd:
            jabberStatusEnumVariant = isLsmPresence ?
                JabberStatusEnum.DO_NOT_DISTURB_LSM_STATUS :
                JabberStatusEnum.DO_NOT_DISTURB_STATUS;
            break;

        case xa:
            if (statusMessage != null)
            {
                if (statusMessage.startsWith(JabberStatusEnum.BUSY.toLowerCase()))
                {
                    jabberStatusEnumVariant = JabberStatusEnum.BUSY_STATUS;
                }
                else if (isOnPhone)
                {
                    jabberStatusEnumVariant = isLsmPresence ?
                        JabberStatusEnum.ON_THE_PHONE_LSM_STATUS :
                        JabberStatusEnum.ON_THE_PHONE_STATUS;
                }
                else if (statusMessage.startsWith(JabberStatusEnum.IN_MEETING.toLowerCase()))
                {
                    jabberStatusEnumVariant = JabberStatusEnum.IN_MEETING_STATUS;
                }
                else if (statusMessage.equalsIgnoreCase(UNREGISTERED_LSM_STATUS))
                {
                    // This indicates that a non-Accession line has
                    // unregistered.
                    if (isExtLsmPresence)
                    {
                        // This presence is from a user who is not also
                        // using an Accession phone, so use LSM only status.
                        jabberStatusEnumVariant = JabberStatusEnum.OFFLINE_LSM_STATUS;
                    }
                    else
                    {
                        // This presence indicates that the user is also
                        // using an Accession phone, so use the offline IM
                        // status.
                        jabberStatusEnumVariant = JabberStatusEnum.OFFLINE_STATUS;
                    }
                }
            }

            if (jabberStatusEnumVariant == -1)
            {
                // Assume busy if we don't have a message, or don't
                // recognise the one we've got.  It's probably another
                // language and thus not unexpected.
                jabberStatusEnumVariant = JabberStatusEnum.BUSY_STATUS;
            }
            break;

        default:
            // Unknown mode
            jabberStatusEnumVariant = JabberStatusEnum.UNKNOWN_STATUS;
        }

        return jabberStatusEnum.getStatus(jabberStatusEnumVariant);
    }

    /**
     * Converts the specified JabberStatusEnum member to the corresponding
     * Jabber Mode
     *
     * @param status the jabberStatus
     * @return a PresenceStatus instance
     */
    public static Presence.Mode presenceStatusToJabberMode(
            PresenceStatus status)
    {
        return scToJabberModesMappings.get(status.getStatusName());
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws IllegalStateException if the underlying stack is not registered
     * and initialized.
     */
    void assertConnected()
        throws IllegalStateException
    {
        if (parentProvider == null)
        {
            throw
                new IllegalStateException(
                        "The provider must be non-null and signed on the"
                            + " Jabber service before being able to"
                            + " communicate.");
        }
        if (!parentProvider.isRegistered())
        {
            // if we are not registered but the current status is online
            // change the current status
            if((currentStatus != null) && currentStatus.isOnline())
            {
                fireProviderStatusChangeEvent(
                    currentStatus,
                    parentProvider.getJabberStatusEnum().getStatus(
                            JabberStatusEnum.OFFLINE_STATUS));
            }

            throw
                new IllegalStateException(
                        "The provider must be signed on the Jabber service"
                            + " before being able to communicate.");
        }
        if (!parentProvider.isConnected()) {
            throw new IllegalStateException(
                    "Network connection is currently unavailable");
        }
    }

    /**
     * Fires provider status change.
     *
     * @param oldStatus old status
     * @param newStatus new status
     */
    public void fireProviderStatusChangeEvent(PresenceStatus oldStatus, PresenceStatus newStatus)
    {
        if (!oldStatus.equals(newStatus))
        {
            currentStatus = newStatus;

            super.fireProviderStatusChangeEvent(oldStatus, newStatus);

            if (newStatus.equals(offlineStatus))
            {
                // Send event notifications saying that all our buddies are offline. The protocol does not implement
                // top level buddies nor subgroups for top level groups so a simple nested loop would be enough.
                Iterator<ContactGroup> groupsIter = getServerStoredContactListRoot().subgroups();

                while (groupsIter.hasNext())
                {
                    ContactGroup group = groupsIter.next();
                    Iterator<Contact> contactsIter = group.contacts();

                    while(contactsIter.hasNext())
                    {
                        flagProviderOfflineToContact((ContactJabberImpl)contactsIter.next());
                    }
                }

                // Do the same for all contacts in the root group
                Iterator<Contact> contactsIter = getServerStoredContactListRoot().contacts();

                while (contactsIter.hasNext())
                {
                    flagProviderOfflineToContact((ContactJabberImpl)contactsIter.next());
                }
            }
        }
    }

    /**
     * Per-contact processing off the back of the provider going offline.
     * @param contact the contact in question
     */
    private void flagProviderOfflineToContact(ContactJabberImpl contact)
    {
        PresenceStatus oldContactStatus = contact.getPresenceStatus();

        if (!oldContactStatus.isOnline())
        {
            // Nothing to do if it wasn't online before.
            return;
        }

        contact.updatePresenceStatus(offlineStatus);
        contact.setStatusMessage(offlineStatus.getStatusName());
        fireContactPresenceStatusChangeEvent(contact, contact.getParentContactGroup(), oldContactStatus, offlineStatus);
    }

    /**
     * Sets the display name for <tt>contact</tt> to be <tt>newName</tt>.
     * <p>
     * @param contact the <tt>Contact</tt> that we are renaming
     * @param newName a <tt>String</tt> containing the new display name for
     * <tt>metaContact</tt>.
     * @throws IllegalArgumentException if <tt>contact</tt> is not an
     * instance that belongs to the underlying implementation.
     */
    public void setDisplayName(Contact contact, String newName)
        throws IllegalArgumentException
    {
        assertConnected();

        if(!(contact instanceof ContactJabberImpl))
        {
            throw new IllegalArgumentException(
                "Argument is not an jabber contact (contact=" + contact + ")");
        }

        RosterEntry entry = ((ContactJabberImpl)contact).getSourceEntry();

        if (entry == null)
        {
            return;
        }

        try
        {
            entry.setName(newName);
        }
        catch (NotConnectedException | InterruptedException | XMPPErrorException |
            NoResponseException e)
        {
            // The generic signature inherited from
            // AbstractOperationSetPersistentPresence only throws
            // IllegalArgumentException.
            throw new IllegalArgumentException("Could not update name", e);
        }
    }

    /**
     * Processes pending presence packets that were received while we were
     * loading contacts from the server's roster.
     */
    private void processPendingPresencePackets()
    {
        sLog.info("Finished loading jabber roster - process pending packets");

        // If the jabber account has become unregistered since we received
        // the pending requests, subscriptionStanzaListener may be null.
        // If so, just clear the list of pending requests, as the server
        // will re-send them when the account reconnects.
        for (Presence pendingPresenceRequest : pendingPresenceRequests)
        {
            JabberSubscriptionListener subscriptionStanzaListener = this.subscriptionStanzaListener;
            if (subscriptionStanzaListener == null)
            {
                sLog.warn("Subscription packet listener is null");
                break;
            }

            sLog.info("Processing pending " +
                         pendingPresenceRequest.getType() +
                         " message from " +
                         sanitisePeerId(pendingPresenceRequest.getFrom().toString()));
            subscriptionStanzaListener.processPresence(pendingPresenceRequest);
        }

        pendingPresenceRequests.clear();
    }

    /**
     * Processes pending unsubscribes that were received while we were loading
     * contacts from the server's roster.
     */
    private void processPendingUnsubscribes()
    {
        sLog.info("Finished loading jabber roster - " +
                    "process pending unsubscribes");

        for (Contact cur_contact : pendingUnsubRequests)
        {
            try
            {
                ssContactList.removeContactFromServer((ContactJabberImpl)cur_contact);
            }
            catch (OperationFailedException e)
            {
                // If an operation fails, there's not much we can do.  Let's
                // assume the contact isn't present any longer - if that's
                // wrong the user should still see the contact and be able to
                // manually delete it.
                // No need to hash Contact, as its toString() method does that.
                sLog.error("Failed to unsubscribe IM contact " +
                             cur_contact, e);
            }
        }

        pendingUnsubRequests.clear();
    }

    /**
     * Our listener that will tell us when we're registered to server
     * and is ready to accept us as a listener.
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            sLog.debug("The Jabber provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if (evt.getNewState() != RegistrationState.REGISTERED)
            {
                // Ensure we resend our status next time we are registered.
                mLastBaseStatusMessage = "";
            }

            if (evt.getNewState() == RegistrationState.REGISTERING)
            {
                // we will add listener for RosterPackets
                // as this will indicate when one is received
                // and we are ready to dispatch the contact list
                // note that our listener will be added just before the
                // one used in the Roster itself, but later we
                // will wait for it to be ready
                // (inside method XMPPConnection.getRoster())
                parentProvider.getConnection().addStanzaListener(
                    new ServerStoredListInit(),
                    new StanzaTypeFilter(RosterPacket.class));
            }
            else if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                fireProviderStatusChangeEvent(
                    currentStatus,
                    parentProvider
                        .getJabberStatusEnum()
                            .getStatus(JabberStatusEnum.AVAILABLE_STATUS));

                createContactPhotoPresenceListener();
                createAccountPhotoPresenceInterceptor();
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED
                  || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
                  || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                //since we are disconnected, we won't receive any further status
                //updates so we need to change by ourselves our own status as
                //well as set to offline all contacts in our contact list that
                //were online
                PresenceStatus oldStatus = currentStatus;
                currentStatus = parentProvider.getJabberStatusEnum().getStatus(
                        JabberStatusEnum.OFFLINE_STATUS);

                fireProviderStatusChangeEvent(oldStatus, currentStatus);

                ssContactList.cleanup();

                sLog.info("Clearing existing subscription packet listener " +
                            "due to entering unregistered state.");
                subscriptionStanzaListener = null;

                XMPPConnection connection = parentProvider.getConnection();
                if (connection != null &&
                    Roster.getInstanceFor(connection) != null)
                {
                    Roster.getInstanceFor(connection).removeRosterListener(contactChangesListener);
                }

                DiagnosticsServiceRegistrar.unregisterStateDumper(contactChangesListener);
                contactChangesListener = null;
            }
        }
    }

    /**
     * Fires the status change, respecting resource priorities.
     * @param presence the presence changed.
     */
    void firePresenceStatusChanged(Presence presence)
    {
        if(contactChangesListener != null)
            contactChangesListener.handlePresenceStatusChanged(presence);
    }

    /**
     * Manage changes of statuses by resource.
     */
    class ContactChangesListener
        implements RosterListener, StateDumper
    {
        /**
         * Store events for later processing, used when initializing contact
         * list, and when reading the statuses during a statedump to prevent a
         * ConcurrentModificationException. Access to this field and
         * storedPresences is synchronized on storedPresences.
         */
        private boolean storeEvents = false;

        /**
         * Stored presences for later processing.  Access to this field and storeEvents is
         * synchronized on storedPresences.
         */
        private final List<Presence> storedPresences = new ArrayList<>();

        /**
         * Map containing all statuses for a userID. The statuses for each
         * userID are stored in a TreeSet of ProcessedPresence. They are sorted
         * by how important their presence is, determined by PresenceComparator.
         */
        private final Map<String, TreeSet<ProcessedPresence>> statuses =
                new Hashtable<>();

        /**
         * Not used here.
         * @param addresses list of addresses added
         */
        public void entriesAdded(Collection<Jid> addresses)
        {}

        /**
         * Not used here.
         * @param addresses list of addresses updated
         */
        public void entriesUpdated(Collection<Jid> addresses)
        {}

        /**
         * Not used here.
         * @param addresses list of addresses deleted
         */
        public void entriesDeleted(Collection<Jid> addresses)
        {}

        /**
         * Received on resource status change.
         * @param presence presence that has changed
         */
        public void presenceChanged(Presence presence)
        {
            handlePresenceStatusChanged(presence);
        }

        /**
         * Status message received from the IM server is un-translated (i.e.
         * in English).  We must convert that to the translated version as
         * understood by the rest of the Accession code.
         *
         * @param presence the new presence that needs translating
         */
        private Presence translatePresence(Presence presence)
        {
            Presence translatedPresence = presence;
            String originalStatusString = presence.getStatus();

            if (!StringUtils.isNullOrEmpty(originalStatusString))
            {
                // If the presence is from a non-Accession client sending LSM
                // presence via AMS their resource will be "ext_client" or
                // "aux_client".  If AMS is indicating an offline presence, it
                // will send a status string of "Unregistered", which will
                // never be translated.  Therefore, we can just pass those
                // presence updates straight on without checking if we need to
                // translate it first.
                String resource =
                    presence.getFrom().getResourceOrEmpty().toString();
                boolean isLsm = AUX_CLIENT_RESOURCE.equals(resource) ||
                                EXT_CLIENT_RESOURCE.equals(resource);
                boolean isUnregisteredLsmStatus =
                        isLsm && UNREGISTERED_LSM_STATUS.equals(originalStatusString);

                if (!isUnregisteredLsmStatus)
                {
                    // Parse the status string to figure out the correct status to
                    // display
                    String[] split = originalStatusString.split(CUSTOM_MOOD_DIVIDER, 2);
                    String status = split[0];
                    String custom;

                    if (split.length == 2)
                    {
                      custom = split[1];
                    }
                    else
                    {
                      custom = null;
                    }

                    // Convert the status to the i18ned equivalent
                    JabberStatusEnum jabberStatusEnum =
                                               parentProvider.getJabberStatusEnum();
                    JabberPresenceStatus jabberStatus =
                                     jabberStatusEnum.getStatusFromBaseName(status, isLsm);

                    if ((jabberStatus ==
                            jabberStatusEnum.getStatus(JabberStatusEnum.UNKNOWN_STATUS)) &&
                        !jabberStatusEnum.isTheBaseUnknownString(status))
                    {
                        // We didn't recognise the string. Two possibilities.
                        // - We didn't understand the status as it was already
                        //   translated (e.g. it came from a back-level client).
                        //   Test for this and carry on if we're ok.
                        // - We just don't understand the status at all. Perhaps
                        //   it's something new (e.g. from an up-level client).
                        //   Rather than just displaying 'unknown', choose the best
                        //   match based on the presence mode.
                        JabberPresenceStatus translatedJabberStatus =
                                         jabberStatusEnum.getStatusFromName(status);

                        if (translatedJabberStatus !=
                            jabberStatusEnum.getStatus(JabberStatusEnum.UNKNOWN_STATUS))
                        {
                            // It was a translated string, probably from a back-
                            // level client
                            status = translatedJabberStatus.getStatusName();
                            contactLogger.trace(
                                         "Was untranslated - now '" + status + "'");
                        }
                        else
                        {
                            // The status string was not recognised.  Use the mode
                            // in the received presence object to choose a better
                            // status than 'unknown'.
                            Mode mode = presence.getMode();
                            sLog.debug(
                                "Unrecognized status from Jabber server: " +
                                logHasher(originalStatusString) + " (mode: " + mode + ")");

                            if (mode != null)
                            {
                                status = scPresenceModeToJabberMappings.get(mode);
                            }
                        }
                    }
                    else
                    {
                        // We understood the status - now get the translated version
                        status = jabberStatus.getStatusName();
                    }

                    // Now we have the i18ned status, add back the custom mood
                    status = maybeAddCustomMood(status, custom);
                    translatedPresence =
                        new Presence(presence).asBuilder()
                                              .setStatus(status)
                                              .build();
                }
            }

            contactLogger.trace("Presence changed for " + sanitisePeerId(presence.getFrom()));

            return translatedPresence;
        }

        /**
         * Sets store events to true.  While this is true, we will cache all
         * received presence updates for processing later. This is used when we
         * are still processing the roster after registering so that we don't
         * fire presence events for contacts who have not yet been resolved.
         */
        void storeEvents()
        {
            synchronized (storedPresences)
            {
                sLog.info("Setting storeEvents to true.");
                storeEvents = true;
            }
        }

        /**
         * Processes all presence updates that were stored while store events
         * was set to true. Also sets store events to false so that future
         * presence updates will be processed as soon as they are received.
         */
        void processStoredEvents()
        {
            List<Presence> storedPresencesToFire;

            // We don't want to call firePresenceStatusChanged whilst holding the storedPresences
            // lock because that calls into Smack code that will require an XMPPConnection lock
            // which may be owned by another thread that also needs the storedPresences lock,
            // causing a deadlock.  Therefore, copy the list, release the lock, then iterate through
            // the copy of the list.
            synchronized (storedPresences)
            {
                sLog.info("Processing stored events and setting storeEvents to false.");
                storeEvents = false;
                storedPresencesToFire = new ArrayList<>(storedPresences);
                storedPresences.clear();
            }

            for (Presence p : storedPresencesToFire)
            {
                firePresenceStatusChanged(p);
            }
        }

        /**
         * Called when we receive a presence update from a buddy.  If we are
         * storing events to be processed later, this adds the presence update
         * to the list of stored presences.  Otherwise, it fires a presence
         * status change event immediately.
         *
         * @param presence the newly received Presence
         */
        private void handlePresenceStatusChanged(Presence presence)
        {
            // We don't want to call firePresenceStatusChanged whilst holding the storedPresences
            // lock because that calls into Smack code that will require an XMPPConnection lock
            // which may be owned by another thread that also needs the storedPresences lock,
            // causing a deadlock.  Therefore, we effectively store the value of storeEvents for
            // use after releasing the lock.
            boolean storedForLater = false;

            synchronized (storedPresences)
            {
                if (storeEvents)
                {
                    storedPresences.add(presence);
                    storedForLater = true;
                }
            }

            if (!storedForLater)
            {
                firePresenceStatusChanged(presence);
            }
        }

        /**
         * Fires the status change, respecting resource priorities.
         *
         * @param presence the presence changed.
         */
        void firePresenceStatusChanged(Presence presence)
        {
            if (!parentProvider.isConnected())
            {
                sLog.debug("Ignoring status changed event as disconnected");
                return;
            }

            try
            {
                // The presence will contain a status message in English.  Make
                // sure it is changed to the correct language.
                presence = translatePresence(presence);

                Jid from = presence.getFrom();
                BareJid userID = from.asBareJid();
                String userIDString = userID.toString();
                String loggableUserID = sanitisePeerId(userIDString);
                String resource = from.getResourceOrEmpty().toString();

                contactLogger.debug("Received new status for buddy " +
                                    loggableUserID + "/" + sanitisePeerId(resource));

                PresenceStatus status = jabberStatusToPresenceStatus(presence, parentProvider);

                if (userIDString.equalsIgnoreCase(parentProvider.getAccountID().getAccountAddress()))
                {
                    parsePresenceUpdateFromOurOwnJid(resource, status);
                    return;
                }

                // All contact statuses that are received from all its
                // resources and are ordered by which is most important to the
                // local user.
                TreeSet<ProcessedPresence> userStatus = statuses.get(userIDString);
                if (userStatus == null)
                {
                    userStatus = new TreeSet<>();
                    statuses.put(userIDString, userStatus);
                }
                else
                {
                    // Remove any existing status for this resource.  If it is
                    // online we will update its value with the new status
                    // below.
                    Iterator<ProcessedPresence> iter = userStatus.iterator();
                    while (iter.hasNext())
                    {
                        Presence p = iter.next().getPresence();
                        if ((p.getFrom().getResourceOrEmpty().toString()).equals(resource))
                        {
                            iter.remove();
                        }
                    }
                }

                // We don't want to add the offline status to the set
                if (!status.equals(parentProvider.getJabberStatusEnum().
                                    getStatus(JabberStatusEnum.OFFLINE_STATUS)))
                {
                    userStatus.add(new ProcessedPresence(presence));
                }

                Presence currentPresence;
                if (userStatus.size() == 0)
                {
                    /*
                     * We no longer have statuses for this resource so it
                     * doesn't make sense to retain (1) the TreeSet and (2) its
                     * slot in the statuses Map.
                     */
                    sLog.debug("No stored presence found for " + loggableUserID);
                    currentPresence = presence;
                    statuses.remove(userIDString);
                }
                else
                {
                    /*
                     * There is some presence for this resource - we use the
                     * highest importance one - i.e. the last in the TreeSet.
                     */
                    ProcessedPresence mostImportantStatus = userStatus.last();
                    currentPresence = mostImportantStatus.getPresence();
                    contactLogger.debug("Most important presence stored for " +
                                  loggableUserID + " is from resource " +
                                  sanitisePeerId(currentPresence.getFrom().getResourceOrEmpty()) +
                                 ", mode: " + currentPresence.getMode() +
                                 ", type: " + currentPresence.getType() +
                                 ", message: " + logHasher(currentPresence.getStatus()) +
                                 ", time processed: " + mostImportantStatus.getTimeStamp());
                }

                // Bail out now if the userID is for a chatroom.
                if (userIDString.startsWith(OperationSetMultiUserChat.CHATROOM_ID_PREFIX))
                {
                    sLog.debug("Don't process presence update for chatroom " + loggableUserID);
                    return;
                }

                ContactJabberImpl sourceContact = ssContactList.findContactById(userID);

                if (sourceContact == null)
                {
                    sLog.warn("No source contact found for id=" + loggableUserID);
                    return;
                }

                ContactGroup parent = sourceContact.getParentContactGroup();

                // If the sourceContact isn't already resolved, mark it as
                // such, as receiving presence from the server proves they are
                // still in our roster.
                if (!sourceContact.isResolved())
                {
                    contactLogger.warn(
                        "Received presence for unresolved contact " + loggableUserID);
                    sourceContact.setResolved(presence);
                    ssContactList.fireContactResolved(parent, sourceContact);
                }

                String statusMessage = currentPresence.getStatus();

                // If the status is Offline, then we don't want to display the
                // Status message
                if (status.equals(offlineStatus) ||
                    status.equals(offlineLsmStatus))
                {
                    statusMessage = null;
                }

                PresenceStatus oldStatus
                    = sourceContact.getPresenceStatus();
                PresenceStatus newStatus
                    = jabberStatusToPresenceStatus(
                            currentPresence,
                            parentProvider);

                // Presence stanzas give us information about which resources
                // are available for this contact. Update our store.
                sourceContact.updateResources();

                // when old and new status are the same do nothing
                // no change
                String oldStatusMsg = sourceContact.getStatusMessage();
                String newStatusMsg = statusMessage;

                if (oldStatus.equals(newStatus))
                {
                    if ((oldStatusMsg == null && newStatusMsg == null) ||
                        (oldStatusMsg != null && oldStatusMsg.equals(newStatusMsg)))
                    {
                        return;
                    }
                }

                // statuses may be the same and only change in status message
                sourceContact.setStatusMessage(statusMessage);

                // If we have previously been subscribed to see a contact's
                // status but this subscription has since been removed, or if
                // we have a one-way subscription to a contact, the server
                // still sometimes sends us presence updates for the contact.
                // Therefore, check whether we are actually subscribed to see
                // the status of this contact.
                ProtocolProviderService protocolProvider = sourceContact.getProtocolProvider();
                OperationSetExtendedAuthorizations authOpSet = protocolProvider.
                    getOperationSet(OperationSetExtendedAuthorizations.class);

                if (authOpSet != null
                    && authOpSet.getSubscriptionStatus(sourceContact) != null
                    && !authOpSet.getSubscriptionStatus(sourceContact)
                        .equals(SubscriptionStatus.Subscribed))
                {
                    // We are not subscribed to see the status of this
                    // contact, so set it to offline.
                    // No need to hash Contact, as its toString() method does
                    // that.
                    newStatus = offlineStatus;
                    sLog.debug("Not subscribed to presence for " +
                                  sourceContact +
                                  ", setting presence to offline");
                }

                sourceContact.updatePresenceStatus(newStatus);

                sLog.debug("Will Dispatch the contact status event.");
                fireContactPresenceStatusChangeEvent(sourceContact, parent,
                    oldStatus, newStatus);
            }
            catch (IllegalStateException | IllegalArgumentException ex)
            {
                sLog.error("Failed changing status", ex);
            }
        }

        protected void parsePresenceUpdateFromOurOwnJid(String resource, PresenceStatus status)
        {
            // The presence status update is from ourself, so we will
            // ignore it, unless it is from an "aux_client" or
            // "ext_client" resource, as that means it is from a
            // non-Accession phone, so we need to use it to update our
            // 'remotely on the phone' status.
            if (AUX_CLIENT_RESOURCE.equals(resource) ||
                EXT_CLIENT_RESOURCE.equals(resource))
            {
                JabberStatusEnum jabberStatusEnum =
                                   parentProvider.getJabberStatusEnum();

                boolean isOnThePhone =
                    status.equals(jabberStatusEnum.getStatus(
                        JabberStatusEnum.ON_THE_PHONE_STATUS)) ||
                    status.equals(jabberStatusEnum.getStatus(
                        JabberStatusEnum.ON_THE_PHONE_LSM_STATUS));

                GlobalStatusService globalStatusService =
                    JabberActivator.getGlobalStatusService();

                if (globalStatusService != null)
                {
                    sLog.debug("Setting remotely on the phone to " +
                                  isOnThePhone);
                    globalStatusService.setIsOnThePhone(isOnThePhone, false);
                }
                else
                {
                    sLog.debug("GlobalStatusService is null - cannot" +
                                 " set remotely on the phone to " +
                                 isOnThePhone);
                }
            }
        }

        @Override
        public String getStateDumpName()
        {
            return "JabberContactPresences";
        }

        /**
         * Dump the presences from all online resources for each contact, in
         * order of importance. The final presence in a list is the overall
         * presence that is shown for that contact in the client.
         *
         * Example output:
         * -----
         * Bare JID --> Presence from resources in asc. importance [resource, type, mode, status, time processed]:
         *
         * f9993a11e7(hash): [15ng0eywh0(hash), available, xa, mjj6srr6ph(hash), 2021_01_19 16:48:49],
         * a399b598d1(hash): [dg3wcfplfv(hash), available, away, f71sfn0sas(hash), 2021_01_19 15:40:22],
         * bec6b11b67(hash): [06lh3adnu3(hash), available, available, 9420wmwwzz(hash), 2021_01_19 16:48:49],
         * 5a50fe7040(hash): [k7tng2809x(hash), available, available, null, 2021_01_19 15:40:22], [AccMobAnd-41ca, available, xa, 9xg5rxwj0l(hash), 2021_01_19 16:48:49],
         * -----
         */
        @Override
        public String getState()
        {
            StringBuilder state = new StringBuilder();

            state.append("Bare JID --> Presence from resources in asc. importance [resource, type, mode, status, time processed]:\n");

            // Prevent ConcurrentModificationException
            storeEvents();

            for (Map.Entry<String, TreeSet<ProcessedPresence>> entry : statuses.entrySet())
            {
                state.append("\n").append(logHasher(entry.getKey())).append(": ");

                for (ProcessedPresence status : entry.getValue())
                {
                    state.append(status.dumpResourcePresenceInfo()).append(", ");
                }
            }

            processStoredEvents();

            return state.toString();
        }

        /**
         * A wrapper for a Presence that was processed by the
         * ContactChangesListener. When initialised, the current timestamp is
         * recorded to keep track of when the presence was processed.
         */
        private class ProcessedPresence implements Comparable<ProcessedPresence>
        {
            private final Presence presence;
            private final Long timeProcessed;
            private String _timeStamp;

            ProcessedPresence(Presence presence)
            {
                this.presence = presence;
                timeProcessed = System.currentTimeMillis();
            }

            public Presence getPresence()
            {
                return presence;
            }

            public String getTimeStamp()
            {
                if (_timeStamp == null)
                {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(timeProcessed);
                    _timeStamp = dateFormat.format(cal.getTime());
                }

                return _timeStamp;
            }

            public String dumpResourcePresenceInfo()
            {
                return "[" +
                       sanitisePeerId(presence.getFrom().getResourceOrEmpty()) + ", " +
                       presence.getType() + ", " +
                       presence.getMode() + ", " +
                       logHasher(presence.getStatus()) + ", " +
                       getTimeStamp() +
                       "]";
            }

            /**
             * The natural ordering of two ProcessedPresence objects is defined
             * by comparing the importance of their PresenceStatus, then by the
             * length of their status message, then by how recent the presence
             * was processed and finally by their resource strings.
             */
            @Override
            public int compareTo(ProcessedPresence o)
            {
                Presence other = o.getPresence();

                // First, compare by status in ascending order of
                // importance to the user.
                PresenceStatus status1 =
                        jabberStatusToPresenceStatus(presence, parentProvider);
                PresenceStatus status2 =
                        jabberStatusToPresenceStatus(other, parentProvider);
                int statusCompare = status1.compareTo(status2);

                if (statusCompare == 0)
                {
                    // If the statuses have the same importance,
                    // compare the status messages. We should
                    // display the longer one over the shorter one
                    // so that custom status messages win over
                    // default ones.
                    String message1 = presence.getStatus();
                    String message2 = other.getStatus();

                    if (message1 != null && message2 != null)
                    {
                        statusCompare = message1.length() - message2.length();
                    }
                    else if (message1 == null && message2 != null)
                    {
                        statusCompare = -1;
                    }
                    else if (message1 != null && message2 == null)
                    {
                        statusCompare = 1;
                    }
                }

                if (statusCompare == 0)
                {
                    // If the above comparisons are equal, take the presence
                    // that was most recently processed.
                    statusCompare = timeProcessed.compareTo(o.timeProcessed);
                }

                if (statusCompare == 0)
                {
                    // Finally compare by the resource they are from using
                    // standard String comparator.
                    String resource1 = presence.getFrom().getResourceOrEmpty().toString();
                    String resource2 = other.getFrom().getResourceOrEmpty().toString();
                    statusCompare = resource1.compareTo(resource2);
                }

                return statusCompare;
            }
        }
    }

    /**
     * Listens for subscription events coming from stack.
     */
    private class JabberSubscriptionListener
        implements StanzaListener
    {
        /**
         * The authorization handler.
         */
        AuthorizationHandler handler = null;

        ThreadPoolExecutor mSubcribeThreadPool;

        private JabberSubscriptionListener()
        {
            /*
             * Create our own thread pool for processing SUBSCRIBE requests.  These requests are fairly uncommon
             * (they are requests from other IM users to add you as a contact) but they can sometimes come in a
             * huge flood at start of day (e.g. if you are logging in to a line just added to the BG), so we want
             * our own pool for processing them so as not to flood any other pool used for other things as well.
             * We want an unbounded queue so that no SUBSCRIBEs are dropped, hence using a LinkedBlockingQueue
             * (note this means that the maximumPoolSize parameter to ThreadPoolExecutor has no effect). We also
             * want to shrink the pool down to 0 for the long periods where we have no requests to process; this
             * requires setting allowCoreThreadTimeOut on the pool.  We use a 60s timeout as that's used in the
             * examples provided in the Executors class.
             */
            mSubcribeThreadPool = new ThreadPoolExecutor(10, 10,
                                        60L, TimeUnit.SECONDS,
                                                         new LinkedBlockingQueue<>());
            mSubcribeThreadPool.allowCoreThreadTimeOut(true);
        }

        /**
         * Process packets.
         * @param stanza packet received to be processed
         */
        public void processStanza(final Stanza stanza)
        {
            Presence presence = (Presence) stanza;

            if (presence == null)
                return;

            synchronized (pendingUpdatesLock)
            {
                Type presenceType = presence.getType();
                String fromIDString = presence.getFrom().toString();
                String loggableFromId = sanitisePeerId(fromIDString);

                if (rosterLoaded)
                {
                    sLog.debug("Roster loaded - processing " + presenceType +
                                 " message from " + loggableFromId);
                    processPresence(presence);
                }
                else
                {
                    sLog.debug("Roster not loaded - queuing " + presenceType +
                                 " message from " + loggableFromId);
                    pendingPresenceRequests.add(presence);
                }
            }
        }

        /**
         * Process presence packets.
         * @param presence presence packet received to be processed.
         */
        private void processPresence(final Presence presence)
        {
            final Presence.Type presenceType = presence.getType();
            final BareJid fromID = presence.getFrom().asBareJid();
            final String fromIDString = fromID.toString();
            final String loggableFromID = sanitisePeerId(fromIDString);

            if (presenceType == Presence.Type.subscribe)
            {
                // Run in different thread to prevent blocking the packet
                // dispatch thread and we don't receive anything till we unblock it
                mSubcribeThreadPool.submit(
                    new Runnable() {
                public void run()
                {
                    sLog.debug(loggableFromID
                                    + " wants to add you to its contact list");

                    // buddy want to add you to its roster
                    ContactJabberImpl srcContact
                        = ssContactList.findContactById(fromID);

                    String nick = null;
                    String loggableNick = null;

                    Object nickExtension =
                            presence.getExtensionElement("nick", Nick.NAMESPACE);

                    if (nickExtension instanceof Nick)
                    {
                        nick = ((Nick) nickExtension).getName();
                        loggableNick = logHasher(nick);
                        sLog.debug("Found nick for " + loggableFromID +
                                     ": " + loggableNick);
                    }

                    Presence.Type responsePresenceType = null;

                    if(srcContact == null)
                    {
                        sLog.debug("Creating volatile contact for " +
                                loggableFromID + " with nick " + loggableNick);
                        srcContact = createVolatileContact(fromIDString, nick);
                    }
                    else
                    {
                        if(srcContact.isPersistent())
                        {
                            // Log this at info level to diagnose bug where
                            // we don't respond to a reciprocal friend
                            // request.
                            sLog.info("Persistent contact already " +
                                        "exists for " + loggableFromID);

                            responsePresenceType = Presence.Type.subscribed;
                        }
                        else
                        {
                            sLog.debug("Non-persistent contact found " +
                                                      "for " + loggableFromID);
                        }
                    }

                    if (responsePresenceType == null)
                    {
                        AuthorizationRequest req = new AuthorizationRequest();
                        boolean isInSameBG = bgIMContactManager.getBGContactForIMContact(srcContact) != null;
                        AuthorizationResponse response
                            = handler.processAuthorisationRequest(req, srcContact, isInSameBG);

                        if (response.equals(AuthorizationResponse.ACCEPT))
                        {
                            responsePresenceType = Presence.Type.subscribed;

                            sLog.info("Sending Accepted Subscription");
                        }
                        else if (response.equals(AuthorizationResponse.REJECT))
                        {
                            responsePresenceType = Presence.Type.unsubscribed;
                            sLog.info("Sending Rejected Subscription");
                        }

                        // Send analytics
                        JabberActivator.getAnalyticsService().onEvent(
                                AnalyticsEventType.CHAT_AUTHORISATION_REQUEST_RECEIVED,
                                AnalyticsParameter.PARAM_AUTH_RESPONSE, response.toString(),
                                AnalyticsParameter.PARAM_EXTERNAL_CONTACT, Boolean.toString(!isInSameBG));
                    }

                    // subscription ignored
                    if(responsePresenceType == null)
                    {
                        sLog.warn("Ignoring presence packet " +
                                    presence +
                                    " of type " + presenceType +
                                    " from " + loggableFromID);
                        return;
                    }

                    // If we're accepting the presence request and if we have a
                    // nick, create a roster entry before sending the subscribed
                    if ((Type.subscribed == responsePresenceType) && (nick != null))
                    {
                        sLog.debug("Adding roster entry for " + loggableFromID +
                                " as " + loggableNick);
                        try
                        {
                            Roster roster = Roster.getInstanceFor(
                                parentProvider.getConnection());

                            if (roster != null)
                            {
                                roster.createItem(fromID, nick, null);
                            }
                        }
                        catch (XMPPErrorException | NotLoggedInException |
                            NoResponseException | NotConnectedException |
                            InterruptedException e)
                        {
                            sLog.warn("Failed to add roster entry for " +
                                        loggableFromID + " as " +
                                        loggableNick + " due to ", e);
                        }
                    }

                    XMPPConnection connection = parentProvider.getConnection();
                    Presence responsePacket = connection.getStanzaFactory()
                                                        .buildPresenceStanza()
                                                        .ofType(responsePresenceType)
                                                        .build();
                    responsePacket.setTo(fromID);

                    try
                    {
                        connection.sendStanza(responsePacket);
                    }
                    catch (NotConnectedException | InterruptedException e)
                    {
                        sLog.warn("Failed to send presence reply for " +
                            loggableFromID + " as " +
                            loggableNick + " due to ", e);
                    }
                        }
                    }
                );
            }
            else if (presenceType == Presence.Type.unsubscribed)
            {
                sLog.trace(loggableFromID + " does not allow your subscription");

                ContactJabberImpl contact
                    = ssContactList.findContactById(fromID);

                if(contact != null)
                {
                    try{
                        ssContactList.removeContactFromServer(contact);
                    }
                    catch(OperationFailedException e)
                    {
                        sLog.error(
                                "Cannot remove contact that unsubscribed.");
                    }
                }
            }
        }
    }

    /*
     * Returns the jabber account resource priority property value.
     *
     * @return the jabber account resource priority property value

    public int getResourcePriority()
    {
        return resourcePriority;
    }*/

    /**
     * Updates the jabber account resource priority property value.
     *
     * @param resourcePriority the new priority to set
     */
    public void setResourcePriority(int resourcePriority)
    {
        this.resourcePriority = resourcePriority;
    }

    /**
     * Runnable that resolves our list against the server side roster.
     * This thread is the one which will call getRoster for the first time.
     * And if roster is currently processing will wait for it (the wait
     * is internal into XMPPConnection.getRoster method).
     */
    private class ServerStoredListInit
        implements Runnable,
                   StanzaListener
    {
        public void run()
        {
            sLog.debug("Starting jabber ServerStoredListInit");

            // Set rosterLoaded to false so that we'll queue any presence
            // packets that arrive while the roster is loading.
            synchronized (pendingUpdatesLock)
            {
                rosterLoaded = false;
            }

            XMPPConnection connection = parentProvider.getConnection();
            // No point doing any of this processing if the connection has already gone.
            if (connection == null)
            {
                sLog.info("Connection already gone, don't initialise roster");
                return;
            }

            // we are already notified lets remove us from the packet
            // listener
            connection.removeStanzaListener(this);

            // init ssList
            contactChangesListener = new ContactChangesListener();
            ssContactList.init(contactChangesListener);

            DiagnosticsServiceRegistrar.registerStateDumper(
                    contactChangesListener, JabberActivator.bundleContext);

            // We've finished loading the roster now so call the methods to
            // process the presence packets/unsubscribe requests that arrived
            // while it was loading.  Ensure we do presence first in case we
            // have both a presence update and unsubscribe for the same
            // subscriber.
            sLog.debug("Finished initializing jabber server stored contact list");

            synchronized (pendingUpdatesLock)
            {
                rosterLoaded = true;
                processPendingPresencePackets();
                processPendingUnsubscribes();
            }
        }

        /**
         * When roster packet with no error is received we are ready
         * to dispatch the contact list, doing it in different thread
         * to avoid blocking xmpp packet receiving.
         * @param stanza the roster packet
         */
        public void processStanza(Stanza stanza)
        {
            // don't process packets that are errors
            if(stanza.getError() != null)
            {
                return;
            }

            new Thread(this, getClass().getName()).start();
        }
    }

    /**
     * Creates an interceptor which modifies presence packet in order to add the
     * the element name "x" and the namespace "vcard-temp:x:update" in order to
     * advertise the avatar SHA-1 hash.
     */
    public void createAccountPhotoPresenceInterceptor()
    {
        // Verifies that we creates only one interceptor of this type.
        if (this.vCardTempXUpdatePresenceExtension == null)
        {
            byte[] avatar = null;
            try
            {
                // Retrieves the current server avatar.
                VCard vCard = VCardManager.getInstanceFor(parentProvider.getConnection())
                        .loadVCard();
                avatar = vCard.getAvatar();
            }
            catch (XMPPException | InterruptedException| NotConnectedException
                | NoResponseException ex)
            {
                sLog.info("Can not retrieve account avatar for "
                    + parentProvider.getOurJidAsString(), ex);
            }

            // Creates the presence extension to generates the  the element
            // name "x" and the namespace "vcard-temp:x:update" containing
            // the avatar SHA-1 hash.
            this.vCardTempXUpdatePresenceExtension =
                new VCardTempXUpdatePresenceExtension(avatar);
        }

        // Intercepts all sent presence packet in order to add the
        // photo tag.
        parentProvider.getConnection().addPresenceInterceptor(
                this.vCardTempXUpdatePresenceExtension,
                (stanza) -> true);
    }

    /**
     * Updates the presence extension to advertise a new photo SHA-1 hash
     * corresponding to the new avatar given in parameter.
     *
     * @param imageBytes The new avatar set for this account.
     */
    public void updateAccountPhotoPresenceExtension(byte[] imageBytes)
    {
        try
        {
            // If the image has changed, then updates the presence extension and
            // send immediately a presence packet to advertise the photo update.
            if (this.vCardTempXUpdatePresenceExtension.updateImage(imageBytes))
            {
                currentStatus.setAvatarChanged(true);
                this.publishPresenceStatus(currentStatus,
                    JabberActivator.getGlobalStatusService().getCustomStatus());
            }
        }
        catch(OperationFailedException ex)
        {
            sLog.info("Can not send presence extension to broadcast photo update", ex);
        }

    }

    /**
     * Creates a listener to call a parser which manages presence packets with
     * the element name "x" and the namespace "vcard-temp:x:update".
     */
    public void createContactPhotoPresenceListener()
    {
        // Registers the listener.
        parentProvider.getConnection().addStanzaListener(
            new StanzaListener()
            {
                public void processStanza(Stanza stanza)
                {
                    // Calls the parser to manages this presence packet.
                    parseContactPhotoPresence(stanza);
                }
            },
            // Creates a filter to only listen to presence packet with the
            // element name "x" and the namespace "vcard-temp:x:update".
            new AndFilter(StanzaTypeFilter.PRESENCE,
                new StanzaExtensionFilter(
                    VCardTempXUpdatePresenceExtension.ELEMENT_NAME,
                    VCardTempXUpdatePresenceExtension.NAMESPACE)
                )
            );
    }

    /**
     * Parses a contact presence packet with the element name "x" and the
     * namespace "vcard-temp:x:update", in order to decide if the SHA-1 avatar
     * contained in the photo tag represents a new avatar for this contact.
     *
     * @param stanza The packet received to parse.
     */
    public void parseContactPhotoPresence(Stanza stanza)
    {
        // Retrieves the contact ID and its avatar that Jitsi currently
        // managed concerning the peer that has sent this presence packet.
        BareJid userID = stanza.getFrom().asBareJid();
        ContactJabberImpl sourceContact
            = ssContactList.findContactById(userID);

        // If this contact is not yet in our contact list, then there is no need
        // to manage this photo update.
        if(sourceContact == null)
        {
            return;
        }

        BufferedImageFuture currentAvatar = sourceContact.getImage(false);

        // Get the packet extension which contains the photo tag.
        StandardExtensionElement defaultExtensionElement =
            (StandardExtensionElement) stanza.getExtensionElement(
                    VCardTempXUpdatePresenceExtension.ELEMENT_NAME,
                    VCardTempXUpdatePresenceExtension.NAMESPACE);
        if(defaultExtensionElement != null)
        {
            try
            {
                String packetPhotoSHA1 =
                    defaultExtensionElement.getAttributeValue("photo");
                // If this presence packet has a photo tag with a SHA-1 hash
                // which differs from the current avatar SHA-1 hash, then Jitsi
                // retrieves the new avatar image and updates this contact image
                // in the contact list.
                if(packetPhotoSHA1 != null &&
                   !packetPhotoSHA1.equals(
                           currentAvatar == null ?
                               "":
                               VCardTempXUpdatePresenceExtension.getImageSha1(currentAvatar.getBytes())
                               )
                   )
                {
                    byte[] newAvatar;

                    // If there is an avatar image, retrieves it.
                    if(!packetPhotoSHA1.isEmpty())
                    {
                        // Retrieves the new contact avatar image.
                        VCard vCard = VCardManager.getInstanceFor(parentProvider.getConnection())
                            .loadVCard(userID.asEntityBareJidOrThrow());
                        newAvatar = vCard.getAvatar();
                    }
                    // Else removes the current avatar image, since the contact
                    // has removed it from the server.
                    else
                    {
                        newAvatar = new byte[0];
                    }

                    // Sets the new avatar image to the Jitsi contact.
                    BufferedImageFuture bif = BufferedImageAvailableFromBytes.fromBytes(newAvatar);
                    sourceContact.setImage(bif);
                    // Fires a property change event to update the contact list.
                    this.fireContactPropertyChangeEvent(
                        ContactPropertyChangeEvent.PROPERTY_IMAGE,
                        sourceContact,
                        currentAvatar,
                        bif);
                }
            }
            catch(XMPPErrorException | NoResponseException | NotConnectedException |
                InterruptedException ex)
            {
                sLog.info("Cannot retrieve vCard from: " +
                            sanitisePeerId(stanza.getFrom()));
                sLog.trace("vCard retrieval exception was: ", ex);
            }
        }
    }

    @Override
    public boolean unsubscribeSupported(Contact contact)
    {
        // Jabber contacts cannot be deleted if they were created
        // automatically to match a BG contact.  Manually added jabber
        // contacts that don't correspond to a BG contact can be deleted.
        return !bgIMContactManager.isBGAutoCreatedIMContact(contact);
    }

    /**
     * Returns the corresponding BG contact (if any) for the given jabber
     * contact.
     *
     * @param contact The jabber contact.
     * @return The corresponding BG contact, or null if none exists.
     */
    public Contact findBGContactForJabberContact(Contact contact)
    {
        return bgIMContactManager.getBGContactForIMContact(contact);
    }

    @Override
    public void finishedLoadingUnresolvedContacts()
    {
        // Just tell the parent provider that we've finished loading the
        // unresolved contacts from the MclStorageManager.
        contactLogger.info("Finished loading unresolved contacts");
        parentProvider.onAllUnresovledContactsLoaded();
    }

    public void cleanUp()
    {
        sLog.info("Cleanup " + this);

        parentProvider.removeRegistrationStateChangeListener(registrationStateListener);

        if (bgIMContactManager != null)
        {
            bgIMContactManager.cleanUp();
        }
    }
    public void subscribeToIMContact(Contact contact)
    {
        bgIMContactManager.subscribeToIMContact(contact);
    }
}
