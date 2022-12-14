/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import static org.jitsi.util.Hasher.logHasher;

import java.net.URI;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.sipconstants.*;
import net.java.sip.communicator.util.*;

import org.jitsi.util.xml.XMLUtils;
import org.w3c.dom.*;

/**
 * Sip presence implementation (SIMPLE).
 *
 * Compliant with rfc3261, rfc3265, rfc3856, rfc3863, rfc4480 and rfc3903
 *
 * @author Benoit Pradelle
 * @author Lyubomir Marinov
 * @author Emil Ivov
 * @author Grigorii Balutsel
 */
public abstract class OperationSetPresenceSipImpl
    extends AbstractOperationSetPersistentPresence<ProtocolProviderServiceSipImpl>
    implements MethodProcessor,
               RegistrationStateChangeListener
{
    private static final Logger logger
        = Logger.getLogger(OperationSetPresenceSipImpl.class);

    /**
     * Server stored contact list, responsible for generate corresponding
     * events to all action that are made with XCAP contacts and
     * groups.
     */
    protected ServerStoredContactList ssContactList;

    /**
     * The default expiration value of a request as defined for the presence
     * package in rfc3856. This is the value used when there is no Expires
     * header in the received subscription requests.
     */
    protected static final int PRESENCE_DEFAULT_EXPIRE = 3600;

    /**
     * How many seconds before a timeout should we refresh our state
     */
    protected static final int REFRESH_MARGIN = 60;

    /**
     * User chosen expiration value of any of our subscriptions.
     * Currently, the value is the default value defined in the rfc.
     */
    protected int subscriptionDuration;

    /**
     * The timer which will handle all the scheduled tasks
     */
    protected final TimerScheduler timer = new TimerScheduler();

    /**
     * The currently active status message.
     */
    protected String statusMessage = "Default Status Message";

    /**
     * Our default presence status.
     */
    protected PresenceStatus presenceStatus;

    /**
     * If we should be totally silenced, just doing local operations
     */
    protected final boolean presenceEnabled;

    protected final SipStatusEnum sipStatusEnum;

    /**
     * The id used in <tt><tuple></tt>  and <tt><person></tt> elements
     * of pidf documents.
     */
    private static final String TUPLE_ID = "t" + (long)(Math.random() * 10000);
    private static final String PERSON_ID = "p" + (long)(Math.random() * 10000);

    // pidf elements and attributes - some public for use by emergency location
    public static final String PRESENCE_ELEMENT = "presence";
    public static final String NS_ELEMENT       = "xmlns";
    public static final String PIDF_NS_VALUE    = "urn:ietf:params:xml:ns:pidf";
    public static final String ENTITY_ATTRIBUTE = "entity";
    protected static final String TUPLE_ELEMENT    = "tuple";
    public static final String ID_ATTRIBUTE     = "id";
    protected static final String STATUS_ELEMENT   = "status";
    protected static final String ONLINE_STATUS    = "open";
    protected static final String OFFLINE_STATUS   = "closed";
    protected static final String BASIC_ELEMENT    = "basic";
    protected static final String CONTACT_ELEMENT  = "contact";
    protected static final String NOTE_ELEMENT     = "note";
    // rpid elements and attributes - public since also used by emergency location
    private static final String RPID_NS_ELEMENT     = "xmlns:rpid";
    private static final String RPID_NS_VALUE       =
                                            "urn:ietf:params:xml:ns:pidf:rpid";
    public static final String DM_NS_ELEMENT       = "xmlns:dm";
    public static final String DM_NS_VALUE         =
                                    "urn:ietf:params:xml:ns:pidf:data-model";
    private static final String NS_PERSON_ELT       = "dm:person";
    private static final String NS_ACTIVITY_ELT     = "rpid:activities";
    private static final String NS_AWAY_ELT         = "rpid:away";
    private static final String NS_BUSY_ELT         = "rpid:busy";
    private static final String NS_OTP_ELT          = "rpid:on-the-phone";
    private static final String NS_STATUS_ICON_ELT  = "rpid:status-icon";

    // namespace wildcard
    protected static final String ANY_NS              = "*";

    /**
     * The <code>EventPackageNotifier</code> which provides the ability of this
     * instance to act as a notifier for the presence event package.
     */
    protected EventPackageNotifier notifier;

    /**
     * The <code>EventPackageSubscriber</code> which provides the ability of
     * this instance to act as a subscriber for the presence event package.
     */
    protected EventPackageSubscriber subscriber;

    /**
     * The authorization handler, asking client for authentication.
     */
    protected AuthorizationHandler authorizationHandler = null;

    /**
     * The name of the event package this object is to implement and carry
     * in the Event and Allow-Events headers.
     */
    protected final String eventPackage;

    /**
     * The sub-type of the content type of the NOTIFY bodies to be announced,
     * expected and supported by the subscriptions to be managed by this
     * object.
     */
    protected final String contentSubType;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * specified parent <tt>provider</tt>.
     * @param provider the ProtocolProviderServiceSipImpl instance that
     * created us.
     * @param presenceEnabled if we are activated or if we don't have to
     * handle the presence informations for contacts
     * @param forceP2PMode if we should start in the p2p mode directly
     * @param pollingPeriod the period between two poll for offline contacts
     * @param subscriptionExpiration the default subscription expiration value
     * to use
     * @param eventPackage the name of the event package the new instance is to
     * implement and carry in the Event and Allow-Events headers
     * @param contentSubType the sub-type of the content type of the NOTIFY
     * bodies to be announced, expected and supported by the subscriptions to be
     * managed by the new instance
     */
    public OperationSetPresenceSipImpl(
        ProtocolProviderServiceSipImpl provider,
        boolean presenceEnabled,
        boolean forceP2PMode,
        int pollingPeriod,
        int subscriptionExpiration,
        String eventPackage,
        String contentSubType)
    {
        super(provider);

        this.contentSubType = contentSubType;
        this.eventPackage = eventPackage;

        // if xivo is enabled use it, otherwise keep old behaviour
        // and enable xcap, it will check and see its not configure and will
        // silently do nothing and leave local storage
        if(provider.getAccountID().getAccountPropertyBoolean(
            ServerStoredContactListXivoImpl.XIVO_ENABLE, false))
        {
            this.ssContactList = new ServerStoredContactListXivoImpl(
                provider, this);
        }
        else
        {
            this.ssContactList = new ServerStoredContactListSipImpl(
                provider, this);

            provider.addSupportedOperationSet(
                OperationSetContactTypeInfo.class,
                new OperationSetContactTypeInfoImpl(this));

            subscribeForBasicLSM();
        }

        //add our registration listener
        this.parentProvider.addRegistrationStateChangeListener(this);

        this.presenceEnabled = presenceEnabled;

        this.subscriptionDuration
        = 900;

        // Notifier part of the presence event package and PUBLISH
        this.parentProvider.registerMethodProcessor(Request.SUBSCRIBE, this);
        this.parentProvider.registerMethodProcessor(Request.NOTIFY, this);
        this.parentProvider.registerMethodProcessor(Request.PUBLISH, this);
        this.parentProvider.registerEvent("presence");

        this.sipStatusEnum = parentProvider.getSipStatusEnum();
        this.presenceStatus = sipStatusEnum.getStatus(SipStatusEnum.OFFLINE);

        logger.debug(
                "SIP presence initialized with :"
                + eventPackage + ", "
                + presenceEnabled + ", "
                + forceP2PMode + ", "
                + pollingPeriod + ", "
                + subscriptionExpiration
                + " for " + this.parentProvider.getOurDisplayName());
    }

    /**
     * Subscribes this client for basic LSM where the user's own line is
     * monitored.
     */
    private void subscribeForBasicLSM()
    {
        // The SIP address, formed with a directory number instead of a SIP
        // username.
        String address =
            parentProvider.getDirectoryNumber()
            + "@" + parentProvider.getAccountID().getService();

        logger.info("Creating SIP Contact for LSM: " + logHasher(address));

        // Create a SIP contact for the current SIP user so they can
        // monitor their own line.
        createBasicLsmSubscriber(address, true, ssContactList.getRootGroup());
    }

    /**
     * Create a SIP subscriber in order to subscribe for LSM presence.
     * @param address
     * @param isOwnLine <tt>true</tt> if this contact represents the user's own
     * line (used to show the correct presence status of the subscriber)
     * @param group The contact group to which this contact will belong
     * @return The SIP contact
     */
    private ContactSipImpl createBasicLsmSubscriber(String address,
                                                    boolean isOwnLine,
                                                    ContactGroupSipImpl group)
    {
        logger.debug("Creating SIP LSM Contact: " + logHasher(address) +
                     ", " + isOwnLine);

        ContactSipImpl lsmContact;

        // Create a SIP contact for the current SIP user so they can
        // monitor their own line.
        try
        {
            lsmContact = ssContactList.createContact(
                                                   group,
                                                   address,
                                                   true,
                                                   null,
                                                   true);

            lsmContact.setPersistentData(MetaContact.IS_OWN_LINE +
                                             "=" + Boolean.toString(isOwnLine));
        }
        catch (OperationFailedException ex)
        {
            logger.warn("Failed to create SIP contact for LSM: ", ex);
            lsmContact = null;
        }

        return lsmContact;
    }

    /**
     * Create a Call Park subscriber.  This is a SIP subscriber subscribed for
     * LSM notifications for this park orbit.
     * @param parkOrbit The orbit code
     * @param group The group (department) to which this subscriber belongs
     * @return The new Call Park SIP contact
     */
    public ContactSipImpl createCallParkSubscriber(String parkOrbit,
                                                   ContactGroupSipImpl group)
    {
        logger.info("Create SIP contact to monitor park orbit: " + parkOrbit);

        String address = parkOrbit + "@" +
                                     parentProvider.getAccountID().getService();
        return createBasicLsmSubscriber(address, false, group);
    }

    /**
     * Registers a listener that would receive events upon changes in server
     * stored groups.
     *
     * @param listener a ServerStoredGroupChangeListener impl that would receive
     *                 events upon group changes.
     */
    public void addServerStoredGroupChangeListener(
            ServerStoredGroupListener listener)
    {
        ssContactList.addGroupListener(listener);
    }

    /**
     * Removes the specified group change listener so that it won't receive
     * any further events.
     *
     * @param listener the ServerStoredGroupChangeListener to remove
     */
    public void removeServerStoredGroupChangeListener(
            ServerStoredGroupListener listener)
    {
        ssContactList.removeGroupListener(listener);
    }

    /**
     * Returns a PresenceStatus instance representing the state this provider is
     * currently in. Note that PresenceStatus instances returned by this method
     * MUST adequately represent all possible states that a provider might
     * enter during its lifecycle, including those that would not be visible
     * to others (e.g. Initializing, Connecting, etc ..) and those that will be
     * sent to contacts/buddies (On-Line, Eager to chat, etc.).
     *
     * @return the PresenceStatus last published by this provider.
     */
    public PresenceStatus getPresenceStatus()
    {
        return this.presenceStatus;
    }

    /**
     * Returns the root group of the server stored contact list.
     *
     * @return the root ContactGroup for the ContactList stored by this
     *   service.
     */
    public ContactGroup getServerStoredContactListRoot()
    {
        return this.ssContactList.getRootGroup();
    }

    /**
     * Creates a group with the specified name and parent in the server
     * stored contact list.
     *
     * @param parentGroup the group where the new group should be created
     * @param groupName the name of the new group to create.
     * @throws OperationFailedException
     */
    public void createServerStoredContactGroup(ContactGroup parentGroup,
                                               String groupName)
            throws OperationFailedException
    {
        if (!(parentGroup instanceof ContactGroupSipImpl))
        {
            String errorMessage = String.format(
                    "Group %1s does not seem to belong to this protocol's " +
                            "contact list", parentGroup.getGroupName());
            throw new IllegalArgumentException(errorMessage);
        }
        ContactGroupSipImpl sipGroup = (ContactGroupSipImpl) parentGroup;
        ssContactList.createGroup(sipGroup, groupName, true);
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
     * @param persistentData a String returned ContactGroups's
     * getPersistentData() method during a previous run and that has been
     * persistently stored locally.
     * @param parentGroup the group under which the new group is to be created
     * or null if this is group directly underneath the root.
     * @return the unresolved <tt>ContactGroup</tt> created from the specified
     * <tt>uid</tt> and <tt>persistentData</tt>
     */
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        //if parent is null then we're adding under root.
        if(parentGroup == null)
        {
            parentGroup = getServerStoredContactListRoot();
        }
        String groupName = ContactGroupSipImpl.createNameFromUID(groupUID);
        return ssContactList.createUnresolvedContactGroup(
                (ContactGroupSipImpl) parentGroup, groupName);
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
        if (!(group instanceof ContactGroupSipImpl))
        {
            String errorMessage = String.format(
                    "Group %1s does not seem to belong to this protocol's " +
                            "contact list", group.getGroupName());
            throw new IllegalArgumentException(errorMessage);
        }
        ssContactList.renameGroup((ContactGroupSipImpl) group, newName);
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
        if (!(contactToMove instanceof ContactSipImpl))
        {
            return;
        }
        try
        {
            ssContactList.moveContactToGroup((ContactSipImpl) contactToMove,
                    (ContactGroupSipImpl) newParent);

            if (this.presenceEnabled)
            {
                subscriber.subscribe(new PresenceSubscriberSubscription(
                        (ContactSipImpl)contactToMove));
            }
        }
        catch (OperationFailedException ex)
        {
            throw new IllegalStateException(
                    "Failed to move contact " + contactToMove.getAddress(), ex);
        }
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
    {
        if (!(group instanceof ContactGroupSipImpl))
        {
            String errorMessage = String.format(
                    "Group %1s does not seem to belong to this protocol's " +
                            "contact list", group.getGroupName());
            throw new IllegalArgumentException(errorMessage);
        }
        ContactGroupSipImpl sipGroup = (ContactGroupSipImpl) group;
        ssContactList.removeGroup(sipGroup);
    }

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified parameters.
     *
     * @param status the PresenceStatus as returned by
     *   getRequestableStatusSet
     * @param statusMsg the message that should be set as the reason to
     *   enter that status
     * @throws IllegalArgumentException if the status requested is not a
     *   valid PresenceStatus supported by this provider.
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   publishing the status fails due to a network error.
     */
    public abstract void publishPresenceStatus(PresenceStatus status,
                                               String statusMsg)
            throws IllegalArgumentException,
            IllegalStateException,
            OperationFailedException;

    /**
     * Notifies all registered listeners of the new event.
     *
     * @param oldValue the presence status we were in before the change.
     */
    public void fireProviderMsgStatusChangeEvent(String oldValue)
    {
        fireProviderStatusMessageChangeEvent(oldValue, this.statusMessage);
    }

    /**
     * Returns the set of PresenceStatus objects that a user of this service
     * may request the provider to enter. Note that the provider would most
     * probably enter more states than those returned by this method as they
     * only depict instances that users may request to enter. (e.g. a user
     * may not request a "Connecting..." state - it is a temporary state
     * that the provider enters while trying to enter the "Connected" state).
     *
     * @return Iterator a PresenceStatus array containing "enterable"
     * status instances.
     */
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        return sipStatusEnum.getSupportedStatusSet();
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
    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException
    {
        Contact contact = resolveContactID(contactIdentifier);

        if (contact == null)
            throw
                new IllegalArgumentException(
                        "contact " + contactIdentifier + " unknown");

        return contact.getPresenceStatus();
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
    public void subscribe(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        subscribe(this.ssContactList.getRootGroup(), contactIdentifier);
    }

    /**
     * Persistently adds a subscription for the presence status of the
     * contact corresponding to the specified contactIdentifier and indicates
     * that it should be added to the specified group of the server stored
     * contact list.
     *
     * @param parentGroup the parent group of the server stored contact list
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
    public void subscribe(ContactGroup parentGroup, String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        this.subscribe(parentGroup, contactIdentifier, null);
    }

    /**
     * Persistently adds a subscription for the presence status of the
     * contact corresponding to the specified contactIdentifier and indicates
     * that it should be added to the specified group of the server stored
     * contact list.
     *
     * @param parentGroup the parent group of the server stored contact list
     *   where the contact should be added. <p>
     * @param contactIdentifier the contact whose status updates we are
     *   subscribing for.
     * @param contactType the contact type to create, if missing null.
     * @throws IllegalArgumentException if <tt>contact</tt> or
     *   <tt>parent</tt> are not a contact known to the underlying protocol
     *   provider.
     * @throws IllegalStateException if the underlying protocol provider is
     *   not registered/signed on a public service.
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   subscribing fails due to errors experienced during network
     *   communication
     */
    void subscribe(ContactGroup parentGroup, String contactIdentifier,
                         String contactType)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        assertConnected();

        if (!(parentGroup instanceof ContactGroupSipImpl))
        {
            String errorMessage = String.format(
                    "Group %1s does not seem to belong to this protocol's " +
                            "contact list",
                    parentGroup.getGroupName());
            throw new IllegalArgumentException(errorMessage);
        }
        //if the contact is already in the contact list
        ContactSipImpl contact = resolveContactID(contactIdentifier);

        if (contact != null)
        {
            if(contact.isPersistent())
            {
                throw new OperationFailedException(
                    "Contact " + contactIdentifier + " already exists.",
                    OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
            }
            else
            {
            // we will remove it as we will created again
            // this is the case when making a non persistent contact to
            // a persistent one
            ssContactList.removeContact(contact);
        }
        }
        contact = ssContactList.createContact((ContactGroupSipImpl) parentGroup,
                contactIdentifier, true, contactType, false);
        if (this.presenceEnabled)
        {
            subscriber.subscribe(new PresenceSubscriberSubscription(contact));
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    protected void assertConnected()
        throws IllegalStateException
    {
        if (this.parentProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                + "service before being able to communicate.");
        if (!this.parentProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                + "being able to communicate.");
    }

    /**
     * Removes a subscription for the presence status of the specified contact.
     * @param contact the contact whose status updates we are unsubscribing
     * from.
     *
     * @throws OperationFailedException with code NETWORK_FAILURE if
     * unsubscribing fails due to errors experienced during network
     * communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void unsubscribe(Contact contact)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        assertConnected();

        if (!(contact instanceof ContactSipImpl))
        {
            throw new IllegalArgumentException("The contact is not a SIP contact");
        }
        ContactSipImpl sipContact = (ContactSipImpl) contact;

        /*
         * Does not assert if there is no subscription because if the user
         * becomes offline he has terminated the subscription and so we have
         * no subscription of this contact but we wont to remove it.
         * Does not assert on connected cause have already has made the check.
         */
        unsubscribe(sipContact, false);
        ssContactList.removeContact(sipContact);
    }

    /**
     * Removes a subscription for the presence status of the specified contact
     * and optionally asserts that the specified contact has an existing
     * subscription prior to attempting the unregistration.
     *
     * @param sipcontact
     *            the contact whose status updates we are unsubscribing from.
     * @param assertConnectedAndSubscribed
     *            <tt>true</tt> to assert that the specified contact has an
     *            existing subscription prior to attempting the unregistration;
     *            <tt>false</tt> to not perform the respective checks
     * @throws OperationFailedException
     *             with code NETWORK_FAILURE if unsubscribing fails due to
     *             errors experienced during network communication
     * @throws IllegalArgumentException
     *             if <tt>contact</tt> is not a contact known to the underlying
     *             protocol provider
     * @throws IllegalStateException
     *             if the underlying protocol provider is not registered/signed
     *             on a public service.
     */
    protected void unsubscribe(
            ContactSipImpl sipcontact,
            boolean assertConnectedAndSubscribed)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        // handle the case of a distant presence agent is used
        // and test if we are subscribed to this contact
        if (this.presenceEnabled && sipcontact.isResolvable())
        {
            if (assertConnectedAndSubscribed)
                assertConnected();

            subscriber
                .unsubscribe(
                    getAddress(sipcontact),
                    assertConnectedAndSubscribed);
        }

        // remove any trace of this contact
        terminateSubscription(sipcontact);
    }

    /**
     * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
     * proper event handler.
     *
     * @param responseEvent the responseEvent that we received
     *            ProtocolProviderService.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public abstract boolean processResponse(ResponseEvent responseEvent);

    /**
     * Terminate the subscription to a contact presence status
     *
     * @param contact the contact concerned
     */
    private void terminateSubscription(ContactSipImpl contact)
    {
        if (contact == null)
        {
            logger.error("null contact provided, can't terminate" +
                    " subscription");
            return;
        }

        // we don't remove the contact
        changePresenceStatusForContact(
            contact,
            sipStatusEnum.getStatus(SipStatusEnum.UNKNOWN));
        contact.setResolved(false);
    }

    /**
     * Process a request from a distant contact
     *
     * @param requestEvent the <tt>RequestEvent</tt> containing the newly
     *            received request.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public abstract boolean processRequest(RequestEvent requestEvent);

    /**
     * Called when a dialog is terminated
     *
     * @param dialogTerminatedEvent DialogTerminatedEvent
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent)
    {
        // never fired
        return false;
    }

    /**
     * Called when an IO error occurs
     *
     * @param exceptionEvent IOExceptionEvent
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processIOException(IOExceptionEvent exceptionEvent)
    {
        // never fired
        return false;
    }

    /**
     * Called when a transaction is terminated
     *
     * @param transactionTerminatedEvent TransactionTerminatedEvent
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processTransactionTerminated(
        TransactionTerminatedEvent transactionTerminatedEvent)
    {
        // nothing to do
        return false;
    }

    /**
     * Called when a timeout occur
     *
     * @param timeoutEvent TimeoutEvent
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processTimeout(TimeoutEvent timeoutEvent)
    {
        logger.error("timeout reached, it looks really abnormal: " +
                timeoutEvent.toString());
        return false;
    }

    /**
     * Sets the presence status of <tt>contact</tt> to <tt>newStatus</tt>.
     *
     * @param contact the <tt>ContactSipImpl</tt> whose status we'd like
     * to set.
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    protected void changePresenceStatusForContact(
        ContactSipImpl contact,
        PresenceStatus newStatus)
    {
        PresenceStatus oldStatus = contact.getPresenceStatus();

        contact.setPresenceStatus(newStatus);
        fireContactPresenceStatusChangeEvent(
                contact, contact.getParentContactGroup(), oldStatus);
    }

    /**
     * Returns a <code>ContactSipImpl</code> with a specific ID in case we have
     * a subscription for it and <tt>null<tt> otherwise.
     *
     * @param contactID
     *            a String identifier of the contact which is to be retrieved
     * @return the <code>ContactSipImpl</code> with the specified
     *         <code>contactID</code> or <tt>null</tt> if we don't have a
     *         subscription for the specified identifier
     */
    public ContactSipImpl findContactByID(String contactID)
    {
        return this.ssContactList.getRootGroup().findContactByID(contactID);
    }

    /**
     * Returns the protocol specific contact instance representing the local
     * user.
     *
     * @param destination the destination that we would be sending our contact
     * information to.
     *
     * @return a ContactSipImpl instance that represents us.
     */
    public ContactSipImpl getLocalContactForDst(ContactSipImpl destination)
    {
        return getLocalContactForDst(destination.getSipAddress());
    }

    /**
     * Returns the protocol specific contact instance representing the local
     * user.
     *
     * @param destination the destination that we would be sending our contact
     * information to.
     *
     * @return a ContactSipImpl instance that represents us.
     */
    public ContactSipImpl getLocalContactForDst(Address destination)
    {
        Address sipAddress = parentProvider.getOurSipAddress(destination);
        ContactSipImpl res
            = new ContactSipImpl(sipAddress, this.parentProvider);

        res.setPresenceStatus(this.presenceStatus);
        return res;
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
        this.authorizationHandler = handler;
    }

    /**
     * Returns the status message that was confirmed by the server
     *
     * @return the last status message that we have requested and the server
     * has confirmed.
     */
    public String getCurrentStatusMessage()
    {
        return this.statusMessage;
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
     *
     * @return the unresolved <tt>Contact</tt> created from the specified
     * <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(
        String address, String persistentData)
    {
        return createUnresolvedContact(address
                , persistentData
                , getServerStoredContactListRoot());
    }

    /**
    * Creates and returns a unresolved contact from the specified
    * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
    * to establish a network connection and resolve the newly created Contact
    * against the server. The protocol provider may will later try and resolve
    * the contact. When this happens the corresponding event would notify
    * interested subscription listeners.
     * @param persistentData a String returned Contact's getPersistentData()
    * method during a previous run and that has been persistently stored
    * locally.
     * @param contactId an identifier of the contact that we'll be creating.
     * @param parent the group where the unresolved contact is
    * supposed to belong to.
     * @return the unresolved <tt>Contact</tt> created from the specified
    * <tt>address</tt> and <tt>persistentData</tt>
    */
    public Contact createUnresolvedContact(String contactId,
                         String persistentData,
                         ContactGroup parent)
    {
        return ssContactList.createUnresolvedContact((ContactGroupSipImpl)
                parent, contactId, persistentData);
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. This method would have no
     * effect on the server stored contact list.
     *
     * @param contactAddress the address of the volatile contact we'd like to
     * create.
     * @param displayName the Display Name of the volatile contact we'd like to
     * create.
     * @return the newly created volatile contact.
     */
    public ContactSipImpl createVolatileContact(String contactAddress,
                                                String displayName)
    {
        try
        {
            // Check whether a volatile group already exists and if not create one
            ContactGroupSipImpl volatileGroup = getNonPersistentGroup();
            // if the parent volatile group is null then we create it
            if (volatileGroup == null)
            {
                ContactGroupSipImpl rootGroup =
                        this.ssContactList.getRootGroup();
                volatileGroup = ssContactList
                        .createGroup(rootGroup,
                            SipActivator.getResources().getI18NString(
                                "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME"),
                            false);
            }

            if (displayName != null)
                return ssContactList.createContact( volatileGroup,
                                                    contactAddress,
                                                    displayName,
                                                    false,
                                                    null,
                                                    false);
            else
                return ssContactList.createContact( volatileGroup,
                                                    contactAddress,
                                                    false,
                                                    null,
                                                    false);
            }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to create volatile contact: " +
                         logHasher(displayName) + " - " +
                         logHasher(contactAddress), ex);
            return null;
        }
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. This method would have no
     * effect on the server stored contact list.
     *
     * @param contactAddress the address of the volatile contact we'd like to
     * create.
     *
     * @return the newly created volatile contact.
     */
    public ContactSipImpl createVolatileContact(String contactAddress)
    {
        return createVolatileContact(contactAddress, null);
    }

    /**
     * Returns the volatile group or null if this group has not yet been
     * created.
     *
     * @return a volatile group existing in our contact list or <tt>null</tt>
     * if such a group has not yet been created.
     */
    private ContactGroupSipImpl getNonPersistentGroup()
    {
        for (int i = 0;
             i < getServerStoredContactListRoot().countSubgroups();
             i++)
        {
            ContactGroupSipImpl gr = (ContactGroupSipImpl)
                getServerStoredContactListRoot().getGroup(i);

            if(!gr.isPersistent())
            {
                return gr;
            }
        }

        return null;
    }

    /**
     * Tries to find a <code>ContactSipImpl</code> which is identified either by
     * a specific <code>contactID</code> or by a derivation of it.
     *
     * @param contactID
     *            the identifier of the <code>ContactSipImpl</code> to retrieve
     *            either by directly using it or by deriving it
     * @return a <code>ContactSipImpl</code> which is identified either by the
     *         specified <code>contactID</code> or by a derivation of it
     */
    ContactSipImpl resolveContactID(String contactID)
    {
        ContactSipImpl res = findContactByID(contactID);

        if (res == null)
        {
            // we try to resolve the conflict by removing "sip:" from the id
            if (contactID.startsWith("sip:"))
                res = findContactByID(contactID.substring(4));

            if (res == null)
            {
                int domainBeginIndex = contactID.indexOf('@');

                // we try to remove the part after the '@'
                if (domainBeginIndex > -1)
                {
                    res
                        = findContactByID(
                            contactID.substring(0, domainBeginIndex));

                    // try the same thing without sip:
                    if ((res == null) && contactID.startsWith("sip:"))
                        res
                            = findContactByID(
                                contactID.substring(4, domainBeginIndex));
                }

                if (res == null)
                {
                    // sip:user_name@ip_address:5060;transport=udp
                    int domainEndIndex = contactID.indexOf(":", 4);

                    // if port is absent try removing the params after ;
                    if (domainEndIndex < 0)
                        domainEndIndex = contactID.indexOf(";", 4);

                    if (domainEndIndex > -1)
                        res
                            = findContactByID(
                                contactID.substring(4, domainEndIndex));
                }
            }
        }
        return res;
    }

    /**
     * Returns a new valid xml document.
     *
     * @return a correct xml document or null if an error occurs
     */
    Document createDocument()
    {
        try
        {
            return XMLUtils.createDocument();
        }
        catch (Exception e)
        {
            logger.error("Can't create xml document", e);
            return null;
        }
    }

    /**
     * Convert a xml document
     *
     * @param document the document to convert
     *
     * @return a string representing <tt>document</tt> or null if an error
     * occur
     */
    String convertDocument(Document document)
    {
        try
        {
            return XMLUtils.createXml(document);
        }
        catch (Exception e)
        {
            logger.error("Can't convert the xml document into a string", e);
            return null;
        }
    }

    /**
     * Convert a xml document
     *
     * @param document the document as a String
     *
     * @return a <tt>Document</tt> representing the document or null if an
     * error occur
     */
    protected Document convertDocument(String document)
    {
        try
        {
            Document doc = XMLUtils.createDocument(document);
            return doc;//XMLUtils.createDocument(document);
        }
        catch (Exception e)
        {
            logger.error("Can't convert the string into a xml document", e);
            return null;
        }
    }

    /**
     * Converts the <tt>PresenceStatus</tt> of <tt>contact</tt> into a PIDF
     * document.
     *
     * @param contact The contact which interest us
     *
     * @return a PIDF document representing the current presence status of
     * this contact or null if an error occurs.
     */
     public byte[] getPidfPresenceStatus(ContactSipImpl contact)
     {
         Document doc = this.createDocument();

         if (doc == null)
             return null;

         String contactUri = contact.getSipAddress().getURI().toString();

         // <presence>
         Element presence = doc.createElement(PRESENCE_ELEMENT);
         presence.setAttribute(NS_ELEMENT, PIDF_NS_VALUE);
         presence.setAttribute(RPID_NS_ELEMENT, RPID_NS_VALUE);
         presence.setAttribute(DM_NS_ELEMENT, DM_NS_VALUE);
         presence.setAttribute(ENTITY_ATTRIBUTE, contactUri);
         doc.appendChild(presence);

         // <person>
         Element person = doc.createElement(NS_PERSON_ELT);
         person.setAttribute(ID_ATTRIBUTE, PERSON_ID);
         presence.appendChild(person);

         // <activities>
         Element activities = doc.createElement(NS_ACTIVITY_ELT);
         person.appendChild(activities);

         // <status-icon>
         URI imageUri = ssContactList.getImageUri();
         if(imageUri != null)
         {
             Element statusIcon = doc.createElement(NS_STATUS_ICON_ELT);
             statusIcon.setTextContent(imageUri.toString());
             person.appendChild(statusIcon);
         }

         // the correct activity
         if (contact.getPresenceStatus()
                         .equals(sipStatusEnum.getStatus(SipStatusEnum.AWAY)))
         {
             Element away = doc.createElement(NS_AWAY_ELT);
             activities.appendChild(away);
         }
         else if (contact.getPresenceStatus()
                         .equals(sipStatusEnum.getStatus(SipStatusEnum.BUSY)))
         {
             Element busy = doc.createElement(NS_BUSY_ELT);
             activities.appendChild(busy);
         }
         else if (contact.getPresenceStatus()
                 .equals(sipStatusEnum.getStatus(SipStatusEnum.ON_THE_PHONE)))
         {
             Element otp = doc.createElement(NS_OTP_ELT);
             activities.appendChild(otp);
         }

         // <tuple>
         Element tuple = doc.createElement(TUPLE_ELEMENT);
         tuple.setAttribute(ID_ATTRIBUTE, TUPLE_ID);
         presence.appendChild(tuple);

         // <status>
         Element status = doc.createElement(STATUS_ELEMENT);
         tuple.appendChild(status);

         // <basic>
         Element basic = doc.createElement(BASIC_ELEMENT);
         if (contact.getPresenceStatus()
                     .equals(sipStatusEnum.getStatus(SipStatusEnum.OFFLINE)))
         {
             basic.appendChild(doc.createTextNode(OFFLINE_STATUS));
         }
         else
         {
             basic.appendChild(doc.createTextNode(ONLINE_STATUS));
         }
         status.appendChild(basic);

         // <contact>
         Element contactUriEl = doc.createElement(CONTACT_ELEMENT);
         Node cValue = doc.createTextNode(contactUri);
         contactUriEl.appendChild(cValue);
         tuple.appendChild(contactUriEl);

         // <note> we write our real status here, this status SHOULD not be
         // used for automatic parsing but some (bad) IM clients do this...
         // we don't use xml:lang here because it's not really relevant
         Element noteNodeEl = doc.createElement(NOTE_ELEMENT);
         noteNodeEl.appendChild(doc.createTextNode(contact.getPresenceStatus()
                 .getStatusName()));
         tuple.appendChild(noteNodeEl);

         String res = convertDocument(doc);
         if (res == null)
             return null;

         return res.getBytes();
     }

     /**
      * Sets the contact's presence status using the PIDF document provided.
      * In case of conflict (more than one status per contact) the last valid
      * status in the document is used.
      * This implementation is very tolerant to be more compatible with bad
      * implementations of SIMPLE. The limit of the tolerance is defined by
      * the CPU cost: as far as the tolerance costs nothing more in well
      * structured documents, we do it.
      *
      * @param presenceDoc the pidf document to use
      */
     protected abstract void setPidfPresenceStatus(String presenceDoc);

     /**
      * Secured call to XMLUtils.getText (no null returned but an empty string)
      *
      * @param node the node with which call <tt>XMLUtils.getText()</tt>
      *
      * @return the string contained in the node or an empty string if there is
      * no text information in the node.
      */
     protected String getTextContent(Element node)
     {
         String res = XMLUtils.getText(node);

         if (res == null)
         {
             logger.warn("no text for element '" + node.getNodeName() + "'");
             return "";
         }

         return res;
     }

     /**
      * Associate the provided presence state to the contacts considering the
      * current presence states and priorities.
      *
      * @param presenceState The presence state to associate to the contacts
      * @param contacts A list of <contact, priority> concerned by the
      *  presence status.
      * @param curStatus The list of the current presence status ordered by
      *  priority (highest priority first).
      *
      * @return a Vector containing a list of <contact, priority, status>
      *  ordered by priority (highest first). Null if a parameter is null.
      */
     protected List<Object[]> setStatusForContacts(
         PresenceStatus presenceState,
         Iterable<Object[]> contacts,
         List<Object[]> curStatus)
     {
         // test parameters
         if (presenceState == null || contacts == null || curStatus == null)
             return null;

         // for each contact in the list
         for (Object[] tab : contacts)
         {
             Contact contact = (Contact) tab[0];
             float priority = (Float) tab[1];

             // for each existing contact
             int pos = 0;
             boolean skip = false;
             for (int i = 0; i < curStatus.size(); i++)
             {
                 Object[] tab2 = curStatus.get(i);
                 Contact curContact = (Contact) tab2[0];
                 float curPriority = (Float) tab2[1];

                 // save the place where to add this contact in the list
                 if (pos == 0 && curPriority <= priority)
                 {
                     pos = i;
                 }

                 if (curContact.equals(contact))
                 {
                     // same contact but with an higher priority
                     // simply ignore this new status affectation
                     if (curPriority > priority)
                     {
                         skip = true;
                         break;
                     // same contact but with a lower priority
                     // replace the old status with this one
                     }
                     else if (curPriority < priority)
                     {
                         curStatus.remove(i);
                     // same contact and same priority
                     // consider the reachability of the status
                     }
                     else
                     {
                         PresenceStatus curPresence = (PresenceStatus) tab2[2];
                         if (curPresence.getStatus() >=
                             presenceState.getStatus())
                         {
                             skip = true;
                             break;
                         }

                         curStatus.remove(i);
                     }

                     i--;
                 }
             }

             if (skip)
                 continue;

             // insert the new entry
             curStatus.add(
                 pos,
                 new Object[] { contact, new Float(priority), presenceState });
         }

         return curStatus;
     }

     /**
      * Forces the poll of all contacts to update their current states.
      */
     protected void forcePollAllContacts()
     {
         // Subcribe to each contact in the list
         for (ContactSipImpl contact : ssContactList
                 .getAllContacts(ssContactList.getRootGroup()))
         {
             forcePollContact(contact);
         }
     }

     /**
      * Forces the poll of a contact to update its current state.
      *
      * @param contact the contact to poll
      */
     public void forcePollContact(ContactSipImpl contact)
     {
         if (!this.presenceEnabled
             || !contact.isResolvable()
             || !contact.isPersistent())
             return;

         // Attempt to subscribe.
         try
         {
             subscriber.poll(new PresenceSubscriberSubscription(contact));
         }
         catch (OperationFailedException ex)
         {
             logger.error("Failed to create and send the subcription", ex);
         }
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

        if (!(contact instanceof ContactSipImpl))
        {
            throw new IllegalArgumentException("The contact is not a SIP " +
                    "contact");
        }

        ssContactList.renameContact((ContactSipImpl) contact, newName);
    }

    /**
     * Return current server-stored contact list implementation.
     * @return current server-stored contact list implementation.
     */
    protected ServerStoredContactList getSsContactList()
    {
        return ssContactList;
    }

    /**
     * Cancels the timer which handles all scheduled tasks and disposes of the
     * currently existing tasks scheduled with it.
     */
    protected void cancelTimer()
    {
        timer.cancel();
    }

    /**
     * The method is called by a ProtocolProvider implementation whenever
     * a change in the registration state of the corresponding provider had
     * occurred. The method is particularly interested in events stating
     * that the SIP provider has unregistered so that it would fire
     * status change events for all contacts in our buddy list.
     *
     * @param evt ProviderStatusChangeEvent the event describing the status
     *            change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState newState = evt.getNewState();

        if (newState.equals(RegistrationState.UNREGISTERING))
        {
            registrationStateUnregistering();
        }
        else if (newState.equals(RegistrationState.REGISTERED))
        {
            registrationStateRegistered();
        }
        else if (newState.equals(RegistrationState.CONNECTION_FAILED))
        {
            registrationStateConnectionFailed();
        }
    }

    /**
     * Handle the behaviour when the registration state has changed to
     * RegistrationState.UNREGISTERING
     */
    protected void registrationStateUnregistering()
    {
        logger.info("Account unregistered - disabling SIP presence");

        // Stop any task associated with the timer
        cancelTimer();
        // Destroy XCAP contacts
        ssContactList.destroy();
        // This will not be called by anyone else, so call it the method
        // will terminate every active subscription
        try
        {
            publishPresenceStatus(
                sipStatusEnum.getStatus(SipStatusEnum.OFFLINE), "");
        }
        catch (OperationFailedException e)
        {
            logger.error("can't set the offline mode", e);
        }
    }

    /**
     * Handle the behaviour when the registration state has changed to
     * RegistrationState.REGISTERED
     */
    protected void registrationStateRegistered()
    {
        logger.debug("enter registered state");

        // Init XCAP contacts
        ssContactList.init();

        if (this.presenceEnabled)
        {
            // Recreate our subscriber object in order to renew our subscription
            // on the new registration.
            logger.info("SIP account registered - creating SIP presence subscriptions");
            this.subscriber =
                new EventPackageSubscriber(this.parentProvider,
                                           this.eventPackage,
                                           this.subscriptionDuration,
                                           this.contentSubType,
                                           this.timer,
                                           REFRESH_MARGIN);
        }
        else
        {
            logger.info("SIP account reregistered, but SIP presence is not enabled");
            this.subscriber = null;
        }

        try
        {
            publishPresenceStatus(
                sipStatusEnum.getStatus(SipStatusEnum.ONLINE), "");
        }
        catch (OperationFailedException e)
        {
            logger.error("Can't set the online mode", e);
        }
    }

    /**
     * Refreshes all presence subscriptions for this subscriber.
     */
    public void refreshPresenceSubscriptions()
    {
        logger.debug("Refresh Presence subscriptions? " + this.presenceEnabled +
            " (subscriber: " + this.subscriber + ")");

        if (this.presenceEnabled && (this.subscriber != null))
        {
            forcePollAllContacts();
        }
    }

    /**
     * Handle the behaviour when the registration state has changed to
     * RegistrationState.CONNECTION_FAILED
     */
    protected void registrationStateConnectionFailed()
    {
        logger.info("SIP connection failed - disabling SIP presence");

        // Destroy XCAP contacts
        ssContactList.destroy();
        // if connection failed we have lost network connectivity
        // we must fire that all contacts has gone offline
        for (ContactSipImpl contact : ssContactList
                .getAllContacts(ssContactList.getRootGroup()))
        {
            PresenceStatus oldContactStatus
                    = contact.getPresenceStatus();
            if (subscriber != null)
            {
                try
                {
                    subscriber.removeSubscription(getAddress(contact));
                }
                catch (OperationFailedException ex)
                {
                    // No need to hash Contact, as its toString() method does
                    // that.
                    logger.warn("Failed to remove subscription to contact " +
                                                                   contact, ex);
                }
            }
            if (!oldContactStatus.isOnline())
            {
                continue;
            }
            contact.setPresenceStatus(
                    sipStatusEnum.getStatus(SipStatusEnum.OFFLINE));
            fireContactPresenceStatusChangeEvent(
                    contact
                    , contact.getParentContactGroup()
                    , oldContactStatus);
        }
        // stop any task associated with the timer
        cancelTimer();

        // update ourself and the UI that our status is OFFLINE
        // don't call publishPresenceStatus as we are in connection failed
        // and it seems we have no connectivity and there is no sense in
        // sending packest(PUBLISH)
        PresenceStatus oldStatus = this.presenceStatus;
        this.presenceStatus = sipStatusEnum.getStatus(SipStatusEnum.OFFLINE);

        this.fireProviderStatusChangeEvent(oldStatus);
    }

    /**
     * Gets the identifying address of a specific <code>ContactSipImpl</code> in
     * the form of a <code>Address</code> value.
     *
     * @param contact
     *            the <code>ContactSipImpl</code> to get the address of
     * @return a new <code>Address</code> instance representing the identifying
     *         address of the specified <code>ContactSipImpl</code>
     *
     * @throws OperationFailedException parsing this contact's address fails.
     */
    protected Address getAddress(ContactSipImpl contact)
        throws OperationFailedException
    {
        try
        {
            return parentProvider.parseAddressString(contact.getAddress());
        }
        catch (ParseException ex)
        {
            //Shouldn't happen
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "An unexpected error occurred while constructing the address",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
            return null;//unreachable but necessary.
        }
    }

    /**
     * Factory method for creating OperationSetPresenceSipImpl classes based
     * on presence type.
     * @param provider the ProtocolProviderServiceSipImpl instance that
     * created us.
     * @param presenceEnabled if we are activated or if we don't have to
     * handle the presence informations for contacts
     * @param pollingPeriod the period between two poll for offline contacts
     * @param subscriptionExpiration the default subscription expiration value
     * to use
     * @return an operation set for the type of SIP presence being used.
     */
    public static OperationSetPersistentPresence createOperationSetPresenceSipImpl(
        ProtocolProviderServiceSipImpl provider,
        boolean presenceEnabled,
        int pollingPeriod,
        int subscriptionExpiration)
    {
        return new OperationSetPresenceSipDialogImpl(
            provider,
            presenceEnabled,
            pollingPeriod,
            subscriptionExpiration);
    }

    /**
     * Represents a subscription of a specific <code>ContactSipImpl</code> to
     * our presence event package.
     *
     * @author Lubomir Marinov
     */
    protected class PresenceNotifierSubscription
        extends EventPackageNotifier.Subscription
    {
        /**
         * The <code>ContactSipImpl</code> which is the subscriber this
         * subscription notifies.
         */
        private final ContactSipImpl contact;

        /**
         * Initializes a new <code>PresenceNotifierSubscription</code> instance
         * which is to represent a subscription of a <code>ContactSipImpl</code>
         * specified by <code>Address</code> to our presence event package.
         *
         * @param fromAddress
         *            the <code>Address</code> of the
         *            <code>ContactSipImpl</code> subscribed to our presence
         *            event package. If no <code>ContactSipImpl</code> with the
         *            specified <code>Address</code> exists in our contact list,
         *            a new one will be created.
         * @param eventId
         *            the value of the id tag to be placed in the Event headers
         *            of the NOTIFY requests created for the new instance and to
         *            be present in the received Event headers in order to have
         *            the new instance associated with them
         */
        public PresenceNotifierSubscription(Address fromAddress, String eventId)
        {
            super(fromAddress, eventId);

            // try to find which contact is concerned
            ContactSipImpl contact
                = resolveContactID(fromAddress.getURI().toString());

            // if we don't know him, create him
            if (contact == null)
            {
                contact = new ContactSipImpl(fromAddress, parentProvider);

                // <tricky time>
                // this ensure that we will publish our status to this contact
                // without trying to subscribe to him
                contact.setResolved(true);
                contact.setResolvable(false);
                // </tricky time>
            }

            // No need to hash Contact, as its toString() method does that.
            logger.debug(contact + " wants to watch your presence status");

            this.contact = contact;
        }

        /**
         * Determines whether the <tt>Address</tt>/Request URI of this
         * <tt>Subscription</tt> is equal to a specific <tt>Address</tt> in the
         * sense of identifying one and the same resource.
         *
         * @param address the <tt>Address</tt> to be checked for value equality
         * to the <tt>Address</tt>/Request URI of this <tt>Subscription</tt>
         * @return <tt>true</tt> if the <tt>Address</tt>/Request URI of this
         * <tt>Subscription</tt> is equal to the specified <tt>Address</tt> in
         * the sense of identifying one and the same resource
         * @see EventPackageSupport.Subscription#addressEquals(Address)
         */
        @Override
        protected boolean addressEquals(Address address)
        {
            String addressString = address.getURI().toString();
            String id1 = addressString;
            // without sip:
            String id2 = addressString.substring(4);
            int domainBeginIndex = addressString.indexOf('@');
            // without the domain
            String id3 = addressString.substring(0, domainBeginIndex);
            // without sip: and the domain
            String id4 = addressString.substring(4, domainBeginIndex);

            String contactAddressString = contact.getAddress();

            // test by order of probability to be true will probably save 1ms :)
            return
                contactAddressString.equals(id2)
                    || contactAddressString.equals(id1)
                    || contactAddressString.equals(id4)
                    || contactAddressString.equals(id3);
        }

        /**
         * Creates content for a notify request using the specified
         * <tt>subscriptionState</tt> and <tt>reason</tt> string.
         *
         * @param subscriptionState the state that we'd like to deliver in the
         * newly created <tt>Notify</tt> request.
         * @param reason the reason string that  we'd like to deliver in the
         * newly created <tt>Notify</tt> request.
         *
         * @return the String bytes of the newly created content.
         */
        protected byte[] createNotifyContent(String subscriptionState,
                        String reason)
        {
            return getPidfPresenceStatus(getLocalContactForDst(contact));
        }
    }

    /**
     * Represents a subscription to the presence event package of a specific
     * <code>ContactSipImpl</code>.
     *
     * @author Lubomir Marinov
     */
    private class PresenceSubscriberSubscription
        extends EventPackageSubscriber.Subscription
    {
        /**
         * The <code>ContactSipImpl</code> which is the notifier this
         * subscription is subscribed to.
         */
        private final ContactSipImpl contact;

        /**
         * Initializes a new <code>PresenceSubscriberSubscription</code>
         * instance which is to represent a subscription to the presence event
         * package of a specific <code>ContactSipImpl</code>.
         *
         * @param contact the <code>ContactSipImpl</code> which is the notifier
         * the new subscription is to subscribed to
         *
         * @throws OperationFailedException if we fail extracting
         * <tt>contact</tt>'s address.
         */
        public PresenceSubscriberSubscription(ContactSipImpl contact)
            throws OperationFailedException
        {
            super(OperationSetPresenceSipImpl.this.getAddress(contact));

            this.contact = contact;
        }

        /*
         * Implements
         * EventPackageSubscriber.Subscription#processActiveRequest(RequestEvent
         * , byte[]).
         */
        protected void processActiveRequest(
            RequestEvent requestEvent,
            byte[] rawContent)
        {
            if (rawContent != null)
                setPidfPresenceStatus(new String(rawContent));

            SubscriptionStateHeader stateHeader =
                (SubscriptionStateHeader)requestEvent.getRequest()
                        .getHeader(SubscriptionStateHeader.NAME);

            if(stateHeader != null)
            {
                if(SubscriptionStateHeader.PENDING
                        .equals(stateHeader.getState()))
                {
                    contact.setSubscriptionState(
                            SubscriptionStateHeader.PENDING);
                }
                else if(SubscriptionStateHeader.ACTIVE
                        .equals(stateHeader.getState()))
                {
                    // if contact was in pending state
                    // our authorization request was accepted
                    if(SubscriptionStateHeader.PENDING
                            .equals(contact.getSubscriptionState())
                       && authorizationHandler != null)
                    {
                        authorizationHandler.processAuthorizationResponse(
                                new AuthorizationResponse(
                                        AuthorizationResponse.ACCEPT, ""),
                                contact);
                    }
                    contact.setSubscriptionState(
                            SubscriptionStateHeader.ACTIVE);
                }
            }
        }

        /*
         * Implements
         * EventPackageSubscriber.Subscription#processFailureResponse(
         * ResponseEvent, int).
         */
        protected void processFailureResponse(
            ResponseEvent responseEvent,
            int statusCode)
        {
            // we probably won't be able to communicate with the contact
            changePresenceStatusForContact(
                contact, sipStatusEnum.getStatus(
                    (Response.TEMPORARILY_UNAVAILABLE == statusCode)
                        ? SipStatusEnum.OFFLINE
                        : SipStatusEnum.UNKNOWN));

            // we'll never be able to resolve this contact
            if ((Response.UNAUTHORIZED != statusCode)
                    && (Response.PROXY_AUTHENTICATION_REQUIRED != statusCode))
                contact.setResolvable(false);
        }

        /*
         * Implements
         * EventPackageSubscriber.Subscription#processSuccessResponse(
         * ResponseEvent, int).
         */
        protected void processSuccessResponse(
            ResponseEvent responseEvent,
            int statusCode)
        {
            switch (statusCode)
            {
            case Response.OK:
            case Response.ACCEPTED:
                try
                {
                    if (!contact.isResolved())
                    {
                        ContactGroup parentGroup =
                            contact.getParentContactGroup();

                        // if contact is not in the contact list
                        // create it, and add to parent, later will be resolved
                        if(resolveContactID(contact.getAddress()) == null)
                        {
                            ((ContactGroupSipImpl) parentGroup)
                                .addContact(contact);

                            // pretend that the contact is created
                            fireSubscriptionEvent(
                                contact,
                                parentGroup,
                                SubscriptionEvent.SUBSCRIPTION_CREATED);
                        }

                        contact.setResolved(true);

                        // inform the listeners that the contact is created
                        fireSubscriptionEvent(
                                contact,
                                parentGroup,
                                SubscriptionEvent.SUBSCRIPTION_RESOLVED);

                        // No need to hash Contact, as its toString() method
                        // does that.
                        logger.debug("contact " + contact + " resolved");
                    }
                }
                catch (NullPointerException e)
                {
                    // should not happen
                    logger.debug(
                                "failed to finalize the subscription of the contact",
                                e);
                }
                break;
            }
        }

        /**
         * Implements the corresponding <tt>SipListener</tt> method by
         * terminating the corresponding subscription and polling the related
         * contact.
         *
         * @param requestEvent the event containing the request that was \
         * terminated.
         * @param reasonCode a String indicating the reason of the termination.
         */
        protected void processTerminatedRequest(
                            RequestEvent requestEvent, String reasonCode)
        {
            terminateSubscription(contact);

            // if the reason is "de-activated" we remove the contact
            // as he unsubscribed, we won't bother him with subscribe requests
            if (SubscriptionStateHeader.DEACTIVATED.equals(reasonCode))
                try
                {
                    ssContactList.removeContact(contact);
                }
                catch(OperationFailedException e)
                {
                    logger.error(
                            "Cannot remove contact that unsubscribed", e);
                }

            SubscriptionStateHeader stateHeader =
                (SubscriptionStateHeader)requestEvent.getRequest()
                        .getHeader(SubscriptionStateHeader.NAME);

            if(stateHeader != null
                && SubscriptionStateHeader.TERMINATED
                    .equals(stateHeader.getState()))
            {
                if(SubscriptionStateHeader.REJECTED
                        .equals(stateHeader.getReasonCode()))
                {
                    if(SubscriptionStateHeader.PENDING
                        .equals(contact.getSubscriptionState()))
                    {
                        authorizationHandler.processAuthorizationResponse(
                            new AuthorizationResponse(
                                AuthorizationResponse.REJECT, ""),
                                contact);
                    }

                    // as this contact is rejected we mark it as not resolvable
                    // so we won't subscribe again (in offline poll task)
                    contact.setResolvable(false);
                }

                contact.setSubscriptionState(
                        SubscriptionStateHeader.TERMINATED);
            }
        }
    }
}
