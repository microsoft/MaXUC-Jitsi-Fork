/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import static net.java.sip.communicator.service.protocol.OperationSetBasicTelephony.*;
import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatAddress;
import static org.jitsi.util.Hasher.logHasher;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentLengthHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ReasonHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.google.common.annotations.VisibleForTesting;
import com.metaswitch.maxanalytics.event.AnalyticsResultKt;
import com.metaswitch.maxanalytics.event.CallAction;
import com.metaswitch.maxanalytics.event.CallKt;
import com.metaswitch.maxanalytics.event.CallType;
import com.metaswitch.maxanalytics.event.CommonKt;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.message.Content;
import gov.nist.javax.sip.message.MultipartMimeContent;

import net.java.sip.communicator.impl.protocol.sip.sdp.SdpUtils;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactsource.ContactChangedEvent;
import net.java.sip.communicator.service.contactsource.ContactQuery;
import net.java.sip.communicator.service.contactsource.ContactQueryListener;
import net.java.sip.communicator.service.contactsource.ContactQueryStatusEvent;
import net.java.sip.communicator.service.contactsource.ContactReceivedEvent;
import net.java.sip.communicator.service.contactsource.ContactRemovedEvent;
import net.java.sip.communicator.service.contactsource.ContactSourceService;
import net.java.sip.communicator.service.contactsource.ExtendedContactSourceService;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.insights.InsightEvent;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeerState;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetCallPark;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.emergencylocation.EmergencyCallContext;
import net.java.sip.communicator.service.protocol.event.CallPeerChangeEvent;
import net.java.sip.communicator.service.protocol.media.MediaAwareCallPeer;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import org.jitsi.service.resources.ResourceManagementService;

/**
 * Our SIP implementation of the default CallPeer;
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class CallPeerSipImpl
    extends MediaAwareCallPeer<CallSipImpl,
                               CallPeerMediaHandlerSipImpl,
                               ProtocolProviderServiceSipImpl>
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(CallPeerSipImpl.class);

    /**
     * The sub-type of the content carried by SIP INFO <tt>Requests</tt> for the
     * purposes of <tt>picture_fast_update</tt>.
     */
    static final String PICTURE_FAST_UPDATE_CONTENT_SUB_TYPE
        = "media_control+xml";

    /**
     * The generic error string in resources used when there was an error
     * starting a call.
     */
    private static final String ERROR_STARTING_CALL =
        "impl.protocol.sip.ERROR_STARTING_CALL";

    /**
     * The key for the resource used when there are two possibilities for the
     * name of the call peer, e.g. "John Smith or 1 other".
     */
    private static final String OTHER_STRING_KEY =
                                               "impl.protocol.sip.X_OR_1_OTHER";

    /**
     * The key for the resource used when there are several possibilities for
     * the name of the call peer, e.g. "John Smith or 3 others".
     */
    private static final String OTHERS_STRING_KEY =
                                              "impl.protocol.sip.X_OR_N_OTHERS";
    /**
     * The error string in resources used when there was a problem in our media
     * stack resulting in a call being torn down.
     */
    private static final String MEDIA_ERROR = "impl.protocol.sip.MEDIA_ERROR";

    /**
     * Call Park retrieval code.  Used to recognize when a call was to retrieve
     * a call from a park orbit and display the appropriate orbit name instead
     * of the dial string.
     */
    private static final String callParkRetrieveCode =
        SipActivator.getConfigurationService().user().getString(
            OperationSetCallPark.RETRIEVE_CODE_KEY);

    /**
     * Reference to the Call Park operation set (used to obtain the friendly name
     * for an orbit).
     */
    private final OperationSetCallPark callParkParkOpSet;

    /**
     * The service used to access resources.
     */
    private final ResourceManagementService resources =
                                                    SipActivator.getResources();

    /**
     * The sip address of this peer
     */
    private Address peerAddress;

    /**
     * The contact for this peer
     */
    private MetaContact peerContact = null;

    /**
     * The display name for this CallPeer.  Cached so we don't need to keep
     * refreshing it during a call.
     */
    private String displayName;

    /**
     * Lock controlling access to the display name
     */
    private final Object displayNameLock = new Object();

    private String diversionDisplayValue = null;
    private String analyticsValue = null;

    /**
     * The JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     */
    private Dialog jainSipDialog = null;

    /**
     * The SIP transaction that established this call. This was previously kept
     * in the jain-sip dialog but got deprecated there so we're now keeping it
     * here.
     */
    private Transaction latestInviteTransaction = null;

    /**
     * The jain sip provider instance that is responsible for sending and
     * receiving requests and responses related to this call peer.
     */
    private SipProvider jainSipProvider = null;

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * The <tt>List</tt> of <tt>MethodProcessorListener</tt>s interested in how
     * this <tt>CallPeer</tt> processes SIP signaling.
     */
    private final List<MethodProcessorListener> methodProcessorListeners
        = new LinkedList<>();

    /**
     * The indicator which determines whether the local peer may send
     * <tt>picture_fast_update</tt> to this remote peer (as part of the
     * execution of {@link #requestKeyFrame()}).
     */
    private boolean sendPictureFastUpdate = true;

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param peerAddress the JAIN SIP <tt>Address</tt> of the new call peer.
     * @param owningCall the call that contains this call peer.
     * @param containingTransaction the transaction that created the call peer.
     * @param sourceProvider the provider that the containingTransaction belongs
     * to.
     */
    public CallPeerSipImpl(Address     peerAddress,
                           CallSipImpl owningCall,
                           Transaction containingTransaction,
                           SipProvider sourceProvider)
    {
        super(owningCall);
        this.peerAddress = peerAddress;
        this.messageFactory = getProtocolProvider().getMessageFactory();

        super.setMediaHandler(
                new CallPeerMediaHandlerSipImpl(this)
                {
                    @Override
                    protected boolean requestKeyFrame()
                    {
                        return CallPeerSipImpl.this.requestKeyFrame();
                    }
                });

        setDialog(containingTransaction.getDialog());
        setLatestInviteTransaction(containingTransaction);
        setJainSipProvider(sourceProvider);

        callParkParkOpSet = getProtocolProvider()
                                   .getOperationSet(OperationSetCallPark.class);
    }

    /**
     * Returns a String locator for that peer.
     *
     * @return the peer's address or phone number.
     */
    public String getAddress()
    {
        SipURI sipURI = (SipURI) peerAddress.getURI();

        return sipURI.getUser() + "@" + sipURI.getHost();
    }

    /**
     * Returns a loggable String locator for that peer.
     */
    public String getLoggableAddress()
    {
        return sanitiseChatAddress(getAddress());
    }

    /**
     * Returns full URI of the address.
     *
     * @return full URI of the address
     */
    public String getURI()
    {
        return getPeerAddress().getURI().toString();
    }

    @Override
    public String getDiversionDisplayValue(boolean sendAnalytic)
    {
        if (diversionDisplayValue == null)
        {
            // Get any associated diversion information
            final NameAndNumber diversionValue = extractDiversionValue(getDiversionInfo());

            if (diversionValue.name != null)
            {
                diversionDisplayValue = diversionValue.name;
                analyticsValue = AnalyticsParameter.VALUE_SIGNALLED;
            }
            else if (diversionValue.number != null)
            {
                // First we see if we can get a contact name for this number
                MetaContactListService metaContactListService = ServiceUtils.getService(
                        SipActivator.bundleContext,
                        MetaContactListService.class);

                if (metaContactListService != null)
                {
                    List<MetaContact> metaContactList = metaContactListService
                            .findMetaContactByNumber(diversionValue.number);

                    if (metaContactList.size() > 0)
                    {
                        // If there are multiple contacts, then we simply use the first.
                        // Other parts of the code try and do better, but that is
                        // false accuracy here.
                        diversionDisplayValue = metaContactList.get(0).getDisplayName();
                        analyticsValue = AnalyticsParameter.VALUE_CONTACT_MATCH;
                    }
                }

                if (diversionDisplayValue == null)
                {
                    // We didn't find a matching contact, so just format the existing number
                    diversionDisplayValue = SipActivator.getPhoneNumberUtils().formatNumberForDisplay(diversionValue.number);
                    analyticsValue = AnalyticsParameter.VALUE_NO_MATCH;
                }
            }
        }

        if (sendAnalytic && analyticsValue != null)
        {
            SipActivator.getAnalyticsService().onEvent(AnalyticsEventType.INCOMING_DIVERTED_CALL,
                                                       AnalyticsParameter.PARAM_DIVERSION_NAME,
                                                       analyticsValue);
        }

        return diversionDisplayValue;
    }

    private String getDiversionInfo()
    {
        String diversion = null;

        Transaction transaction = getLatestInviteTransaction();
        if (ConfigurationUtils.displayCallRedirection() && transaction instanceof ServerTransaction)
        {
            ServerTransaction serverTransaction = (ServerTransaction)transaction;
            Request invite = serverTransaction.getRequest();

            // When there are multiple diversion headers, the first header is the
            // latest one, so see if we are configured to show the latest one.
            boolean useLatest = ConfigurationUtils.useLatestRedirection();

            ListIterator<? extends Header> headers = invite.getHeaders("Diversion");
            Header header = null;
            while (headers.hasNext())
            {
                header = headers.next();
                if (useLatest)
                {
                    // Exit the loop if all we want is the first value
                    break;
                }
            }
            if (header != null)
            {
                // Unfortunately JSIP does not have specific support for the
                // Diversion header, so we need to parse the data out ourselves.
                // The header starts "Diversion: " and then the value we want.
                String[] parts = header.toString().split(":", 2);
                if (parts.length > 1)
                {
                    diversion = parts[1].strip();
                    logger.info("This call includes diversion information" + (useLatest ? ", using latest" : ""));
                }
            }
        }

        return diversion;
    }

    /**
     * Returns the address of the remote party (making sure that it corresponds
     * to the latest address we've received) and caches it.
     *
     * @return the most recent <tt>javax.sip.address.Address</tt> that we have
     * for the remote party.
     */
    public Address getPeerAddress()
    {
        Dialog dialog = getDialog();

        if (dialog != null)
        {
            Address remoteParty = dialog.getRemoteParty();

            if (remoteParty != null)
            {
                //update the address we've cached.
                peerAddress = remoteParty;
            }
        }
        return peerAddress;
    }

    /**
     * Returns a human readable name representing this peer. The order of
     * preference for display name is MetaContact -> peerAddress -> Contact ->
     * SIP user name -> blank
     *
     * @return a String containing a name for that peer.
     */
    public String getDisplayName()
    {
        // Need to protect access to the display name member as it is updated
        // asynchronously if we have to query LDAP to obtain a new value
        synchronized (displayNameLock)
        {
            return doGetDisplayName();
        }
    }

    /**
     * Gets the display name, using a cached value if available.
     * Should only be called from getDisplayName
     * as that will ensure that we have the correct locks
     *
     * @return the display name
     */
    private String doGetDisplayName()
    {
        // Try to get the cached name first
        if (displayName != null)
        {
            logger.trace("getDisplayName() returns cached name " +
                         logHasher(displayName) + " for peer.");
            return displayName;
        }

        // We find and store the display name so we don't have to do this
        // processing next time.
        displayName = innerGetDisplayName(resources);

        return displayName;
    }

    /**
     * Actually gets the display name, should only be called from doGetDisplayName.
     *
     * @return the display name
     */
    private String innerGetDisplayName(ResourceManagementService resources)
    {
        // Assume a CRM looked-up name is not appropriate to use unless the
        // various checks in Accession's native contact sources have returned
        // nothing.
        mCanUseCrmLookupDisplayName = false;

        String name = null;
        String peerName = getPeerDisplayName();
        String userName = getPeerUserName();

        List<MetaContact> matchingMetaContacts = findMetaContacts(userName, peerName);

        if (matchingMetaContacts != null)
        {
            // If the caller has just added us as a contact and we have
            // auto-accept enabled, we might receive the call before our
            // client finishes saving the contact, so before the matching
            // contact has a name. This also happens if we manually add a
            // contact at the same time he is calling us.
            //
            // Ideally we should get rid of the "No Name" string. However,
            // we would then have to fix everywhere else in the UI that
            // displayed the name to check to see if the name is not null.
            // That's going to take a while, and run the risk of introducing
            // bugs.
            //
            // So, to prevent showing "No Name" as the call peer name, we
            // simply ignore unnamed contacts - even if we don't find a name
            // anywhere else, it's better to show the calling number.
            List<String> contactNames = new ArrayList<>();
            for (MetaContact match : matchingMetaContacts)
            {
                String displayName = match.getDisplayName();
                if (displayName != null &&
                    !resources.getI18NString("service.gui.UNKNOWN")
                              .equals(displayName))
                {
                    // We'll show only the first name if there are multiple
                    // matches. Try and preserve the name provided by the
                    // remote peer if possible.
                    if (displayName.trim().equalsIgnoreCase(peerName))
                    {
                        contactNames.add(0, displayName);
                        peerContact = match;
                    }
                    else
                    {
                        contactNames.add(displayName);
                        if (peerContact == null)
                        {
                            peerContact = match;
                        }
                    }
                }
                else
                {
                    // No need to hash MetaContact, as its toString() method
                    // does that.
                    logger.debug("Ignoring metacontact match with no name: " +
                                                                        match);
                }
            }

            if (contactNames.size() == 1)
            {
                name = contactNames.get(0);
                logger.debug("Unique metacontact match found for call peer: " +
                                                         logHasher(name));
            }
            else if (contactNames.size() > 1)
            {
                peerContact = null;
                name = getNameFromMultipleMatches(contactNames.get(0),
                                                  contactNames.size());
                logger.debug(contactNames.size() +
                             " metacontact matches found for call peer.");
            }
        }

        if (name == null)
        {
            // If no name is found at this point, it becomes acceptable to use
            // a CRM-matched name. Note, however, that this might be overwritten
            // depending on the success of the external name lookup below.
            mCanUseCrmLookupDisplayName = true;

            // Grab the phone number from the SIP username if possible.
            String peerUserName = getPeerUserName();

            // If at this stage we still haven't resolved the display name, we
            // fire off a thread to check external contact sources for
            // references to this call peer.
            // If this finds a match, it will overwrite any choice of display
            // name made further down in this method.
            logger.debug("Performing external search to match call peer.");
            String number = SipActivator.getPhoneNumberUtils().
                    extractPlainDnFromAddress(peerUserName);
            fireExternalNameLookup(number);
        }

        if (name == null)
        {
            name = peerName;
            logger.debug("Got name from peer address.");
        }

        if (name == null)
        {
            Contact contact = getContact();
            if (contact != null)
            {
                name = contact.getDisplayName();
                logger.debug("Got name from proto contact.");
            }
        }

        if (name == null)
        {
            name = getPeerUserName();
            logger.debug("Got name from SIP username.");
        }

        logger.debug("Name = " + logHasher(name));

        if (name != null)
        {
            if (name.startsWith("sip:"))
            {
                name = name.substring(4);
            }

            if ((callParkParkOpSet != null) &&
                (callParkRetrieveCode != null) &&
                name.startsWith(callParkRetrieveCode))
            {
                String orbitCode = name.substring(callParkRetrieveCode.length());
                name = callParkParkOpSet.getFriendlyNameFromOrbitCode(orbitCode);
            }
        }

        // If after all this, we still don't have a name, just use the address
        if (name == null || name.trim().length() == 0)
        {
            name = getAddress();

            if (name != null)
            {
                name = name.split("@")[0];
            }
        }
        return name;
    }

    /**
     * Asynchronously checks external contact sources for entries that match the
     * address of this call peer. If any are found, the display name of this
     * peer is updated accordingly, overwriting any existing display name.
     *
     * @param number The number to fire an external lookup for.
     */
    public void fireExternalNameLookup(String number)
    {
        logger.entry();

        // No-op if we have no number to lookup.
        if (number != null)
        {

            logger.debug("Schedule external lookup for " + logHasher(number));
            new Thread("ExternalNameLookupThread")
            {
                public void run()
                {
                    logger.debug("Run external lookup for " + logHasher(number));
                    performExternalNameLookup(number);
                }
            }.start();
        }

        logger.exit();
    }

    /**
     * Checks external contact sources for entries that match the address of
     * this call peer. If any are found, the display name of this peer is
     * updated accordingly, overwriting any existing display name.
     * This is a blocking action, and should be called in its own thread.
     *
     * @param number The number to fire an external lookup for.
     */
    private void performExternalNameLookup(String number)
    {
        List<ContactSourceService> contactSourceServices =
            SipActivator.getContactSources();

        if (number == null)
        {
            // We don't have a SIP username, and in the absence of a telephone
            // number we can't look up client details.
            // So just give up looking.
            return;
        }

        // The telephone number itself should be interpreted as a regex literal.
        // For example if it begins '+44' the '+' should not be treated as a
        // special character.
        String queryString = "\\Q" + number + "\\E";

        Pattern query = Pattern.compile(queryString);

        for (ContactSourceService sourceService : contactSourceServices)
        {
            if (sourceService instanceof ExtendedContactSourceService)
            {
                // We only check ExtendedContactSourceServices, as it is not
                // possible to query other contact sources using patterns.
                ContactQuery search;
                try
                {
                    search =
                    ((ExtendedContactSourceService)sourceService).
                                     querySourceForNumber(query);

                    // Create a listener to handle the results of the search
                    // asynchronously.
                    search.addContactQueryListener(
                        new NameQueryListener(
                            ((ExtendedContactSourceService) sourceService)
                                .getClass().getName()));
                }
                catch (UnsupportedOperationException uoex)
                {
                    // Not all services support querying by number.
                    // This one does not, so skip it.
                }
            }
        }
    }

    /**
     * Returns the list of MetaContacts that match this peer, or null if the peer
     * address was invalid or did not match any MetaContacts.
     *
     * @return the MetaContacts that match this peer, or null if the address was
     * invalid or had no matches.
     */
    public static List<MetaContact> findMetaContacts(String userName, String peerName)
    {
        // Try and associate the phone number with a local contact

        logger.info("Searching for metacontacts matching a call peer " +
                    logHasher(userName));

        String phoneNumber = SipActivator.getPhoneNumberUtils().
                extractPlainDnFromAddress(userName);

        // Check that this is a valid telephone number
        if (phoneNumber == null)
        {
            logger.error("Failed to obtain phone number from SIP username " +
                         "during contact lookup for call peer.");
            return null;
        }

        // Search for all contacts that match this SIP username in the
        // MetaContactListService. It is necessary to normalize the SIP
        // username and account ID so that potential matches aren't missed.
        MetaContactListService metaContactListService = ServiceUtils.getService(
                                                SipActivator.getBundleContext(),
                                                MetaContactListService.class);

        List<MetaContact> metaContactList = metaContactListService
                                            .findMetaContactByNumber(phoneNumber);

        if (metaContactList.size() > 1)
        {
            // We've got a list of multiple matching contacts - see if we can
            // use the peer name (or CNAM) to narrow it down to just one match.
            // If we can't narrow it down to one match, then return the original
            // list.
            String loggablePeerName = logHasher(peerName);
            logger.debug("Multiple contacts, looking for peer name " +
                         loggablePeerName);

            if (peerName != null && peerName.trim().length() > 0)
            {
                MetaContact matchedMetaContact = null;

                for (MetaContact metaContact : metaContactList)
                {
                    if (peerName.equals(metaContact.getDisplayName()))
                    {
                        // No need to hash MetaContact, as its toString()
                        // method does that.
                        if (matchedMetaContact != null)
                        {
                            logger.debug("Found second match " + metaContact);
                            matchedMetaContact = null;
                            break;
                        }
                        else
                        {
                            logger.debug("Found first match " + metaContact);
                            matchedMetaContact = metaContact;
                        }
                    }
                }

                if (matchedMetaContact != null)
                {
                    // No need to hash MetaContact, as its toString() method
                    // does that.
                    logger.info("Found metacontact with matching name " +
                                loggablePeerName + " so using it. Discarding " +
                                metaContactList);
                    metaContactList.clear();
                    metaContactList.add(matchedMetaContact);
                }
            }
        }
        else
        {
            // No need to hash MetaContact, as its toString() method does that.
            logger.debug("At most one match " + metaContactList);
        }

        return metaContactList;
    }

    /**
     * @return the display name obtained from the peer address
     */
    private String getPeerDisplayName()
    {
        Address peerAddress = getPeerAddress();
        String peerName = null;

        if (peerAddress != null && peerAddress.getDisplayName() != null)
        {
            peerName = peerAddress.getDisplayName().trim();
        }

        return peerName;
    }

    /**
     * Returns the peer username for this call
     *
     * This method is synchronised in order to protect the displayName member,
     * which is updated asynchronously if we query LDAP to obtain a new value.
     *
     * @return the peer username for this call
     */
    public String getPeerUserName()
    {
        logger.entry();

        URI peerURI = getPeerAddress().getURI();

        if (peerURI instanceof SipURI)
        {
            // This username is in the form of a DN (i.e. 1234567).
            String userName = ((SipURI) peerURI).getUser();

            if (userName != null && userName.length() > 0)
            {
                logger.exit("Peer user name is " + logHasher(userName));
                return userName;
            }
        }
        else
        {
            String name = peerURI.toString();
            logger.exit("Peer user name is " + logHasher(name));
            return name;
        }

        logger.exit("Peer user name is null");
        return null;
    }

    /**
     * Sets a human readable name representing this peer.
     *
     * @param displayName the peer's display name
     */
    public void setDisplayName(String displayName)
    {
        String oldName;

        synchronized (displayNameLock)
        {
            oldName = getDisplayName();

            try
            {
                this.displayName = displayName;

                this.peerAddress.setDisplayName(displayName);
            }
            catch (ParseException ex)
            {
                //couldn't happen
                logger.error(ex.getMessage(), ex);
                throw new IllegalArgumentException(ex.getMessage());
            }
        }

        // Fire the Event - technically should be synchronized, but not to avoid
        // deadlocks.  If it is called concurrently, it's not the end of the world
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_DISPLAY_NAME_CHANGE,
                oldName,
                displayName);
    }

    /**
     * Sets the display name for the call peer after an external query.
     *
     * @param displayName the peer's display name
     */
    private void setDisplayNameFromExternalQuery(String displayName)
    {
        synchronized (displayNameLock)
        {
            // At the moment, the only type of external query that is not a CRM
            // lookup, is an LDAP query, which should take precedence over CRM
            // lookups. Hence, if this method is called (after a successful
            // LDAP query), make sure to prevent CRM lookups from overwriting
            // the display name.
            mCanUseCrmLookupDisplayName = false;

            setDisplayName(displayName);
        }
    }

    /**
     * Sets the JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     * @param dialog the JAIN SIP dialog that has been created by the
     * application for this call.
     */
    public void setDialog(Dialog dialog)
    {
        this.jainSipDialog = dialog;
    }

    /**
     * Returns the JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     *
     * @return the JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     */
    public Dialog getDialog()
    {
        return jainSipDialog;
    }

    /**
     * Gets the dialog call ID for the call.
     *
     * Basically this only applies to SIP calls where the
     * ID is the SIP Call ID.  You'd think getCallID() on this
     * object would return it, but that is something else.
     */
    @Override
    public String getDialogCallID()
    {
        return getDialog().getCallId().getCallId();
    }

    /**
     * Gets the call direction for the call.
     *
     */
    @Override
    public boolean dialogIsServer()
    {
        return getDialog().isServer();
    }

    /**
     * Sets the transaction instance that contains the INVITE which started
     * this call.
     *
     * @param transaction the Transaction that initiated this call.
     */
    public void setLatestInviteTransaction(Transaction transaction)
    {
        logger.debug("Setting latest invite transaction " + transaction);
        this.latestInviteTransaction = transaction;
    }

    /**
     * Returns the transaction instance that contains the INVITE which started
     * this call.
     *
     * @return the Transaction that initiated this call.
     */
    public Transaction getLatestInviteTransaction()
    {
        return latestInviteTransaction;
    }

    /**
     * Sets the jain sip provider instance that is responsible for sending and
     * receiving requests and responses related to this call peer.
     *
     * @param jainSipProvider the <tt>SipProvider</tt> that serves this call
     * peer.
     */
    public void setJainSipProvider(SipProvider jainSipProvider)
    {
        this.jainSipProvider = jainSipProvider;
    }

    /**
     * Returns the jain sip provider instance that is responsible for sending
     * and receiving requests and responses related to this call peer.
     *
     * @return the jain sip provider instance that is responsible for sending
     * and receiving requests and responses related to this call peer.
     */
    public SipProvider getJainSipProvider()
    {
        return jainSipProvider;
    }

    /**
     * Returns the contact corresponding to this peer or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this peer or null
     * if no particular contact has been associated.
     */
    public Contact getContact()
    {
        // if this peer has no call, most probably it means
        // it's disconnected and no more in call
        // and we cannot obtain the contact
        if(getCall() == null)
        {
            return null;
        }

        ProtocolProviderService pps = getCall().getProtocolProvider();
        OperationSetPresenceSipImpl opSetPresence
            = (OperationSetPresenceSipImpl) pps
                .getOperationSet(OperationSetPresence.class);

        if (opSetPresence != null)
            return opSetPresence.resolveContactID(getAddress());
        else
            return null;
    }

    /**
     * Returns the MetaContact corresponding to this peer or null if no
     * particular MetaContact has been associated.
     *
     * @return the <tt>MetaContact</tt> corresponding to this peer or null
     * if no particular MetaContact has been associated.
     */
    @Override
    public MetaContact getMetaContact()
    {
        return peerContact;
    }

    /**
     * Sets the MetaContact corresponding to this peer.
     *
     * @param metaContact the <tt>MetaContact</tt> to associate with this peer.
     */
    @Override
    public void setMetaContact(MetaContact metaContact)
    {
        peerContact = metaContact;
    }

    /**
     * Process the received response to a SIP INFO containing picture_fast_update that
     * we sent to request a new video keyframe
     * @param clientTransaction - the transaction for the original SIP INFO
     * @param response - the SIP response
     */

    void processPictureFastUpdate(ClientTransaction clientTransaction, Response response)
    {
        int rspCode = response.getStatusCode();

        if ((rspCode == Response.UNAUTHORIZED) ||
            (rspCode == Response.PROXY_AUTHENTICATION_REQUIRED))
        {
            try
            {
                logger.info("Authenticate SIP INFO");
                EventPackageSupport.processAuthenticationChallenge(getProtocolProvider(),
                                                                   clientTransaction,
                                                                   response,
                                                                   getJainSipProvider());
            }
            catch (OperationFailedException e)
            {
                // If authentication fails, just log and give up - we should send another request soon if we don't get another keyframe.
                logger.error("Failed to authenticate SIP INFO ", e);
            }
        }
        else if ((rspCode != Response.OK) && sendPictureFastUpdate)
        {
            // Disable the sending of picture_fast_update because it seems to be unsupported by this remote peer.
            logger.warn("Disabling picture_fast_update, we've received SIP rc " + rspCode);
            sendPictureFastUpdate = false;
        }
    }

    boolean processPictureFastUpdate(
            ServerTransaction serverTransaction,
            Request request)
        throws OperationFailedException
    {
        CallPeerMediaHandlerSipImpl mediaHandler = getMediaHandler();
        boolean requested =
            mediaHandler != null && mediaHandler.processKeyFrameRequest();

        Response response;

        try
        {
            response
                = getProtocolProvider().getMessageFactory().createResponse(
                        Response.OK,
                        request);
        }
        catch (ParseException pe)
        {
            throw new OperationFailedException(
                    "Failed to create OK Response.",
                    OperationFailedException.INTERNAL_ERROR,
                    pe);
        }

        if (!requested)
        {
            ContentType ct
                = new ContentType(
                        "application",
                        PICTURE_FAST_UPDATE_CONTENT_SUB_TYPE);
            String content
                = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n"
                    + "<media_control>\r\n"
                    + "<general_error>\r\n"
                    + "Failed to process picture_fast_update request.\r\n"
                    + "</general_error>\r\n"
                    + "</media_control>";

            try
            {
                response.setContent(content, ct);
            }
            catch (ParseException pe)
            {
                throw new OperationFailedException(
                        "Failed to set content of OK Response.",
                        OperationFailedException.INTERNAL_ERROR,
                        pe);
            }
        }

        try
        {
            serverTransaction.sendResponse(response);
        }
        catch (Exception e)
        {
            throw new OperationFailedException(
                    "Failed to send OK Response.",
                    OperationFailedException.INTERNAL_ERROR,
                    e);
        }

        return true;
    }

    /**
     * Reinitializes the media session of the <tt>CallPeer</tt> that this
     * INVITE request is destined to.
     *
     * @param serverTransaction a reference to the {@link ServerTransaction}
     * that contains the reINVITE request.
     */
    public void processReInvite(ServerTransaction serverTransaction)
    {
        logger.debug("Processing REINVITE " + serverTransaction);
        Request invite = serverTransaction.getRequest();

        setLatestInviteTransaction(serverTransaction);

        // SDP description may be in ACKs - bug report Laurent Michel
        String sdpOffer = null;
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
            sdpOffer = SdpUtils.getContentAsString(invite);

        Response response;
        try
        {
            response = messageFactory.createResponse(Response.OK, invite);

            /*
             * If the local peer represented by the Call of this CallPeer is
             * acting as a conference focus, it must indicate it in its Contact
             * header.
             */
            reflectConferenceFocus(response);

            String sdpAnswer;
            if(sdpOffer != null)
                sdpAnswer = getMediaHandler().processOffer( sdpOffer );
            else
                sdpAnswer = getMediaHandler().createOffer();

            response.setContent( sdpAnswer, getProtocolProvider()
                .getHeaderFactory().createContentTypeHeader(
                                "application", "sdp"));

            logger.trace("will send an OK response: " + response);
            serverTransaction.sendResponse(response);
            logger.debug("OK response sent");
        }
        catch (Exception ex)
        {
            // If we fail to create a response, we should send an error
            // response followed by a BYE - because the call is not implicitly
            // torn down by rejecting a reINVITE, but we can't continue it.
            logger.error("Error while trying to send a response", ex);
            getProtocolProvider().sayErrorSilently(
                            serverTransaction, Response.SERVER_INTERNAL_ERROR);
            try
            {
                hangup(HANGUP_REASON_INTERNAL_ERROR,
                       SipActivator.getResources().getI18NString(MEDIA_ERROR));
            }
            catch (OperationFailedException e)
            {
                // Failed to hang up - not much to do but kill the call
                // internally.
                logger.error("Failed to hang up on error", e);
                setState(CallPeerState.FAILED,
                    SipActivator.getResources().getI18NString(MEDIA_ERROR));
            }

            return;
        }

        reevalRemoteHoldStatus();

        fireRequestProcessed(invite, response);
    }

    /**
     * Sets the state of the corresponding call peer to DISCONNECTED and
     * sends an OK response.
     *
     * @param byeTran the ServerTransaction the BYE request arrived in.
     */
    public void processBye(ServerTransaction byeTran)
    {
        Request byeRequest = byeTran.getRequest();
        // Send OK
        Response ok = null;
        try
        {
            ok = messageFactory.createResponse(Response.OK, byeRequest);
        }
        catch (ParseException ex)
        {
            logger.error("Error while trying to send a response to a bye", ex);
            /*
             * No need to let the user know about the error since it doesn't
             * affect them. And just as the comment on sendResponse bellow
             * says, this is not really a problem according to the RFC so we
             * should proceed with the execution bellow in order to gracefully
             * hangup the call.
             */
        }

        if (ok != null)
            try
            {
                byeTran.sendResponse(ok);
                logger.debug("sent response");
            }
            catch (Exception ex)
            {
                /*
                 * This is not really a problem according to the RFC so just
                 * dump to stdout should someone be interested.
                 */
                logger.error("Failed to send an OK response to BYE request,"
                    + "exception was:\n", ex);
            }

        // change status
        boolean dialogIsAlive;
        try
        {
            dialogIsAlive = EventPackageUtils.processByeThenIsDialogAlive(
                            byeTran.getDialog());
        }
        catch (SipException ex)
        {
            dialogIsAlive = false;

            logger.error(
                "Failed to determine whether the dialog should stay alive.",ex);
        }

        //if the Dialog is still alive (i.e. we are in the middle of a xfer)
        //then only stop streaming, otherwise Disconnect.
        if (dialogIsAlive)
        {
            getMediaHandler().close();

            // We've received a BYE - thus the call is now disconnected
            setState(CallPeerState.DISCONNECTED);
        }
        else
        {
            ReasonHeader reasonHeader =
                    (ReasonHeader)byeRequest.getHeader(ReasonHeader.NAME);

            if(reasonHeader != null)
            {
                setState(
                        CallPeerState.DISCONNECTED,
                        reasonHeader.getText(),
                        reasonHeader.getCause());
            }
            else
            {
                setState(CallPeerState.DISCONNECTED);
            }
        }
    }

    /**
     * Sets the state of the specifies call peer as DISCONNECTED.
     *
     * @param serverTransaction the transaction that the cancel was received in.
     */
    public void processCancel(ServerTransaction serverTransaction)
    {
        // Cancels should be OK-ed and the initial transaction - terminated
        // (report and fix by Ranga)
        Request cancel = serverTransaction.getRequest();
        try
        {
            Response ok = messageFactory.createResponse(Response.OK, cancel);
            serverTransaction.sendResponse(ok);

            logger.debug("sent an ok response to a CANCEL request");
        }
        catch (ParseException ex)
        {
            logAndFail("Failed to create an OK Response to a CANCEL.", ex);
            return;
        }
        catch (Exception ex)
        {
            logAndFail("Failed to send an OK Response to a CANCEL.", ex);
            return;
        }

        try
        {
            // stop the invite transaction as well
            Transaction tran = getLatestInviteTransaction();
            // should be server transaction and misplaced cancels should be
            // filtered by the stack but it doesn't hurt checking anyway
            if (!(tran instanceof ServerTransaction))
            {
                logger.error("Received a misplaced CANCEL request!");
                return;
            }

            ServerTransaction inviteTran = (ServerTransaction) tran;
            Request invite = getLatestInviteTransaction().getRequest();
            Response requestTerminated = messageFactory
                .createResponse(Response.REQUEST_TERMINATED, invite);

            inviteTran.sendResponse(requestTerminated);
            logger.debug("sent request terminated response");
        }
        catch (ParseException ex)
        {
            logger.error("Failed to create a REQUEST_TERMINATED Response to "
                + "an INVITE request.", ex);
        }
        catch (Exception ex)
        {
            logger.error("Failed to send a REQUEST_TERMINATED Response to "
                + "an INVITE request.", ex);
        }

        ReasonHeader reasonHeader =
                    (ReasonHeader)cancel.getHeader(ReasonHeader.NAME);

        if(reasonHeader != null)
        {
            setState(
                    CallPeerState.DISCONNECTED,
                    reasonHeader.getText(),
                    reasonHeader.getCause());
        }
        else
            setState(CallPeerState.DISCONNECTED);
    }

    /**
     * Updates the session description and sends the state of the corresponding
     * call peer to CONNECTED.
     *
     * @param serverTransaction the transaction that the ACK was received in.
     * @param ack the ACK <tt>Request</tt> we need to process
     */
    public void processAck(ServerTransaction serverTransaction, Request ack)
    {
        logger.debug("Processing ACK " + serverTransaction);
        ContentLengthHeader contentLength = ack.getContentLength();
        if ((contentLength != null) && (contentLength.getContentLength() > 0))
        {
            try
            {
                getMediaHandler().processAnswer(
                                    SdpUtils.getContentAsString(ack));
            }
            catch (Exception exc)
            {
                logAndFail("There was an error parsing the SDP description of "
                            + getDisplayName() + "(" + getAddress() + ")", exc);
                return;
            }
        }

        // Connect the call if it's not on hold.  Should be called even if call
        // is already connected to unset mute.
        reevalRemoteHoldStatus();
        if (!CallPeerState.isOnHold(getState()))
        {
            setState(CallPeerState.CONNECTED);
            getMediaHandler().start();

            // Set initial mute status to correspond with call mute status.
            // This will unmute calls that were previously muted because of
            // early media and update the mute status of the headset.
            if(this.getCall() != null)
            {
                setMute(this.getCall().isMute());
            }
        }
    }

    /**
     * Handles early media in 183 Session Progress responses. Retrieves the SDP
     * and makes sure that we start transmitting and playing early media that we
     * receive. Puts the call into a CONNECTING_WITH_EARLY_MEDIA state.
     *
     * @param tran the <tt>ClientTransaction</tt> that the response
     * arrived in.
     * @param response the 183 <tt>Response</tt> to process
     */
    public void processSessionProgress(ClientTransaction tran,
                                       Response          response)
    {
        if (response.getContentLength().getContentLength() == 0)
        {
            logger.debug("Ignoring a 183 with no content");
            return;
        }

        ContentTypeHeader contentTypeHeader = (ContentTypeHeader) response
                .getHeader(ContentTypeHeader.NAME);

        if (!contentTypeHeader.getContentType().equalsIgnoreCase("application")
            || !contentTypeHeader.getContentSubType().equalsIgnoreCase("sdp"))
        {
            //This can happen when receiving early media for a second time.
            logger.warn("Ignoring invite 183 since call peer is "
                + "already exchanging early media.");
            return;
        }

        //handle media
        try
        {
            getMediaHandler().processAnswer(
                            SdpUtils.getContentAsString(response));
        }
        catch (Exception exc)
        {
            logAndFail("There was an error parsing the SDP description of "
                + getDisplayName() + "(" + getAddress() + ")", exc);
            return;
        }

        //change status
        setState(CallPeerState.CONNECTING_WITH_EARLY_MEDIA);
        getMediaHandler().start();
    }

    /**
     * Sets our state to CONNECTED, sends an ACK and processes the SDP
     * description in the <tt>ok</tt> <tt>Response</tt>.
     *
     * @param clientTransaction the <tt>ClientTransaction</tt> that the response
     * arrived in.
     * @param ok the OK <tt>Response</tt> to process
     */
    public void processInviteOK(ClientTransaction clientTransaction,
                                Response         ok)
    {
        logger.debug("processInviteOK " + clientTransaction);

        // This is the point an outbound call starts from a users point of view,
        // except for when a call is put on or off hold, in which case the call
        // has already connected before.
        Call call = getCall();
        if (call != null
            && !CallPeerState.CONNECTED.equals(getState())
            && !CallPeerState.isOnHold(getState()))
        {
            call.setUserPerceivedCallStartTime(System.currentTimeMillis());
        }

        boolean connectCall = false;

        synchronized (getMediaHandler())
        {
            // Do nothing if the call is already ended
            if (CallPeerState.DISCONNECTED.equals(getState()) ||
                CallPeerState.FAILED.equals(getState()))
            {
                logger.info("Ignoring a INVITE OK, as state is " + getState());
                return;
            }

            try
            {
                 //Process SDP unless we've just had an answer in a 18X response
                if (!CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(getState()))
                {
                    getMediaHandler()
                        .processAnswer(SdpUtils.getContentAsString(ok));
                }
            }
            //at this point we have already sent our ack so in addition to logging
            //an error we also need to hangup the call peer.
            catch (Exception exc)//Media or parse exception.
            {
                logger.error(
                        "There was an error parsing the SDP description of " +
                        logHasher(getDisplayName()) +
                        "(" + getLoggableAddress() + ")", exc);
                try
                {
                    //we are connected from a SIP point of view (cause we sent our
                    //ACK) so make sure we set the state accordingly or the hangup
                    //method won't know how to end the call.
                    setState(CallPeerState.CONNECTED,
                        "Error:" + exc.getLocalizedMessage());
                    hangup();
                }
                catch (Exception e)
                {
                    logger.error("Error hanging up call", e);
                }
                finally
                {
                    logAndFail("Remote party sent a faulty session description.",
                            exc);
                }
                return;
            }

            // Change hold status if it has changed.
            reevalRemoteHoldStatus();
            if (!CallPeerState.isOnHold(getState()))
            {
                // Logging "this" invokes the toString() method on AbstractCallPeer.
                logger.info("Call is no longer on hold." +
                    " Setting peer state for " + this +
                    " to 'connected'");
                setState(CallPeerState.CONNECTED);
                connectCall = true;
            }
        }

        if (connectCall)
        {
            // Logging "this" invokes the toString() method on AbstractCallPeer.
            logger.debug("Starting media for " + this);
            getMediaHandler().start();

            // Make sure the mute status of the headset is in sync with the new
            // active call
            setMute(this.getCall().isMute());
            logger.debug("Updated mute status from call; now " + isMute());
        }

        try
        {
            // Only send the ACK at the end.  We could send it early (we already
            // got all the info we need and processSdpAnswer() can take a while)
            // but sending it early can lead to a race condition if the switch
            // immediately sends a reINVITE with different SDP.
            getProtocolProvider().sendAck(clientTransaction);
        }
        catch (InvalidArgumentException ex)
        {
            // Shouldn't happen
            logAndFail("Error creating an ACK (CSeq?)", ex);
            return;
        }
        catch (SipException ex)
        {
            logAndFail("Failed to create ACK request!", ex);
            return;
        }

        fireResponseProcessed(ok, null);
    }

    /**
     * Sends a <tt>picture_fast_update</tt> SIP INFO request to this remote
     * peer.
     *
     * @throws OperationFailedException if anything goes wrong while sending the
     * <tt>picture_fast_update</tt> SIP INFO request to this remote peer
     */
    private void pictureFastUpdate()
        throws OperationFailedException
    {
        Request info
            = getProtocolProvider().getMessageFactory().createRequest(
                    getDialog(),
                    Request.INFO);

        //here we add the body
        ContentType ct
            = new ContentType(
                    "application",
                    PICTURE_FAST_UPDATE_CONTENT_SUB_TYPE);
        String content
            = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n"
                + "<media_control>\r\n"
                + "<vc_primitive>\r\n"
                + "<to_encoder>\r\n"
                + "<picture_fast_update/>\r\n"
                + "</to_encoder>\r\n"
                + "</vc_primitive>\r\n"
                + "</media_control>";

        ContentLength cl = new ContentLength(content.length());
        info.setContentLength(cl);

        try
        {
            info.setContent(content.getBytes(), ct);
        }
        catch (ParseException ex)
        {
            logger.error("Failed to construct the INFO request", ex);
            throw new OperationFailedException(
                    "Failed to construct a client the INFO request",
                    OperationFailedException.INTERNAL_ERROR,
                    ex);
        }
        //body ended
        ClientTransaction clientTransaction;
        try
        {
            clientTransaction
                = getJainSipProvider().getNewClientTransaction(info);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                    "Failed to construct a client transaction from the INFO request",
                    ex);
            throw new OperationFailedException(
                    "Failed to construct a client transaction from the INFO request",
                    OperationFailedException.INTERNAL_ERROR,
                    ex);
        }

        try
        {
            if (getDialog().getState()
                == DialogState.TERMINATED)
            {
                //this is probably because the call has just ended, so don't
                //throw an exception. simply log and get lost.
                logger.warn(
                        "Trying to send a dtmf tone inside a "
                            + "TERMINATED dialog.");
                return;
            }

            getDialog().sendRequest(clientTransaction);
            logger.debug("sent request");
        }
        catch (SipException ex)
        {
            throw new OperationFailedException(
                    "Failed to send the INFO request",
                    OperationFailedException.NETWORK_FAILURE,
                    ex);
        }
    }

    /**
     * Ends the call with for this <tt>CallPeer</tt>. Depending on the state
     * of the peer the method would send a CANCEL, BYE, or BUSY_HERE message
     * and set the new state to DISCONNECTED.
     *
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangup()
        throws OperationFailedException
    {
        // By default we hang up by indicating no failure has happened.
        hangup(HANGUP_REASON_NORMAL_CLEARING, null);
    }

    /**
     * Ends the call with for this <tt>CallPeer</tt>. Depending on the state
     * of the peer the method would send a CANCEL, BYE, or BUSY_HERE/DECLINE
     * message and set the new state to DISCONNECTED.
     *
     * @param reasonCode indicates if the hangup is following to a call failure
     * or simply a disconnect indicate by the reason.
     * @param reason the reason of the hangup. If the hangup is due to a call
     * failure, then this string could indicate the reason of the failure
     *
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangup(int reasonCode, String reason)
        throws OperationFailedException
    {
        logger.info("Asked to hangup " + this + " with reason: " + reasonCode + " " + reason);
        // do nothing if the call is already ended
        if (CallPeerState.DISCONNECTED.equals(getState())
            || CallPeerState.FAILED.equals(getState()))
        {
            logger.debug("Ignoring a request to hangup a call peer "
                        + "that is already DISCONNECTED");
            return;
        }

        boolean failed = (reasonCode != HANGUP_REASON_NORMAL_CLEARING);

        CallPeerState peerState = getState();
        if (peerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(peerState))
        {
            hangupUsingBye(reasonCode, reason, failed, false);
        }
        else if (CallPeerState.CONNECTING.equals(getState())
            || CallPeerState.CONNECTING_WITH_EARLY_MEDIA.equals(getState())
            || CallPeerState.ALERTING_REMOTE_SIDE.equals(getState()))
        {
            if (getLatestInviteTransaction() != null)
            {
                // Someone knows about us. Let's be polite and say we are
                // leaving
                try
                {
                    sayCancel();

                    setDisconnectedState(failed, reason, reasonCode);
                }
                catch (OperationFailedException e)
                {
                    // This isn't necessarily a failure -
                    // we expect to see this if the call has been setup while
                    // we attempted to hang it up.
                    logger.info("Failed to hangup call by sending CANCEL", e);

                    // Attempt to hang up the call by sending BYE instead
                    hangupUsingBye(reasonCode, reason, failed, true);
                }
            }
        }
        else if (peerState.equals(CallPeerState.INCOMING_CALL))
        {
            setDisconnectedState(failed, reason, reasonCode);

            // Determine the response code to reject the call with
            boolean sendDeclineOnCallRejection = getProtocolProvider()
                .getAccountID()
                .getAccountPropertyBoolean(
                    ProtocolProviderFactory.SEND_DECLINE_ON_CALL_REJECTION,
                        false);

            if (sendDeclineOnCallRejection)
            {
                sayDecline();
            }
            else
            {
                sayBusyHere();
            }
        }
        // For FAILED and BUSY we only need to update CALL_STATUS
        else if (peerState.equals(CallPeerState.BUSY))
        {
            setDisconnectedState(failed, reason, reasonCode);
        }
        else if (peerState.equals(CallPeerState.FAILED))
        {
            setDisconnectedState(failed, reason, reasonCode);
        }
        else
        {
            setDisconnectedState(failed, reason, reasonCode);
            logger.error("Could not determine call peer state! Unknown state " +
                                                                     peerState);
        }
    }

    /**
     * Hang up this call by sending a SIP BYE.
     *
     * @param reasonCode indicates if the hangup is following to a call failure
     * or simply a disconnect indicate by the reason.
     * @param reason the reason of the hangup. If the hangup is due to a call
     * failure, then this string could indicate the reason of the failure
     * @param failed is this a call failure?
     * @param disconnect should we always disconnect?
     * @throws OperationFailedException the operation failed
     */
    private void hangupUsingBye(int reasonCode,
                                String reason,
                                boolean failed,
                                boolean disconnect)
        throws OperationFailedException
    {
        try
        {
            // Safe to log this peerID as it is simply a UID implemented
            // in MediaAwareCallPeer. No Personal Data involved.
            logger.debug("Sending BYE to hangup call, peerID: " + getPeerID() +
                         " Reason: " + reason +
                         " Code: " + reasonCode);
            boolean dialogIsAlive = sayBye(reasonCode, reason);
            if (disconnect || !dialogIsAlive)
            {
                setDisconnectedState(failed, reason, reasonCode);
            }
        }
        catch(Throwable ex)
        {
            // if we fail to send the bye, lets close the call anyway
            logger.error(
                "Error while trying to hangup, trying to handle!", ex);

            // make sure we end media if exception occurs
            setDisconnectedState(true, null, reasonCode);

            // if it's the handled OperationFailedException, pass it
            if(ex instanceof OperationFailedException)
                throw (OperationFailedException)ex;
        }
    }

    /**
     * Sends a BUSY_HERE response to the peer represented by this instance.
     *
     * @throws OperationFailedException if we fail to create or send the
     * response
     */
    private void sayBusyHere()
        throws OperationFailedException
    {
        logger.info("Reject call with 486 Busy Here");
        sayRejectCall(Response.BUSY_HERE);
    }

    /**
     * Sends a DECLINE response to the peer represented by this instance.
     *
     * @throws OperationFailedException if we fail to create or send the
     * response
     */
    private void sayDecline()
        throws OperationFailedException
    {
        logger.info("Reject call with 603 Decline");
        sayRejectCall(Response.DECLINE);
    }

    /**
     * Sends the passed rejection response code to the peer represented by this
     * instance.  Only the BUSY_HERE and DECLINE response codes are currently
     * supported.
     *
     * @throws OperationFailedException if we fail to create or send the
     * response
     */
    private void sayRejectCall(int responseCode)
        throws OperationFailedException
    {
        if (!(getLatestInviteTransaction() instanceof ServerTransaction))
        {
            logger.error(
                "Cannot send " + responseCode + " in a client transaction");
            throw new OperationFailedException(
                "Cannot send " + responseCode + " in a client transaction",
                OperationFailedException.INTERNAL_ERROR);
        }

        if ((responseCode != Response.BUSY_HERE) &&
            (responseCode != Response.DECLINE))
        {
            logger.error(
                responseCode + " not supported for call rejection");
            throw new OperationFailedException(
                responseCode + " not supported for call rejection",
                OperationFailedException.INTERNAL_ERROR);
        }

        Request request = getLatestInviteTransaction().getRequest();
        Response rejectResponse = null;
        try
        {
            rejectResponse = messageFactory.createResponse(
                                 responseCode, request);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create the " + responseCode + " response!",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        ServerTransaction serverTransaction =
            (ServerTransaction) getLatestInviteTransaction();

        try
        {
            sendTelemetryIncomingCall(CallAction.DECLINED);
            serverTransaction.sendResponse(rejectResponse);
            logger.debug("sent response");
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send the " + responseCode + " response",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
    }

    /**
     * Sends a Cancel request to the peer represented by this instance.
     *
     * @throws OperationFailedException we failed to construct or send the
     * CANCEL request.
     */
    private void sayCancel()
        throws OperationFailedException
    {
        if (getLatestInviteTransaction() instanceof ServerTransaction)
        {
            logger.error("Cannot cancel a server transaction");
            throw new OperationFailedException(
                "Cannot cancel a server transaction",
                OperationFailedException.INTERNAL_ERROR);
        }

        ClientTransaction clientTransaction =
            (ClientTransaction) getLatestInviteTransaction();
        try
        {
            Request cancel = messageFactory.createCancelRequest(clientTransaction);

            ClientTransaction cancelTransaction =
                getJainSipProvider().getNewClientTransaction(
                    cancel);
            cancelTransaction.sendRequest();
            logger.debug("sent request");
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send the CANCEL request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }
    }

    /**
     * Sends a BYE request to <tt>callPeer</tt>.
     *
     * @return <tt>true</tt> if the <tt>Dialog</tt> should be considered
     * alive after sending the BYE request (e.g. when there are still active
     * subscriptions); <tt>false</tt>, otherwise
     *
     * @throws OperationFailedException if we failed constructing or sending a
     * SIP Message.
     */
    private boolean sayBye(int reasonCode, String reason)
        throws OperationFailedException
    {
        Dialog dialog = getDialog();

        Request bye = messageFactory.createRequest(dialog, Request.BYE);

        if(reasonCode != HANGUP_REASON_NORMAL_CLEARING && reason != null)
        {
            int sipCode = convertReasonCodeToSIPCode(reasonCode);

            if(sipCode != -1)
            {
                try
                {
                    // indicate reason for failure
                    // using Reason header rfc3326
                    ReasonHeader reasonHeader =
                        getProtocolProvider().getHeaderFactory()
                            .createReasonHeader(
                                "SIP",
                                sipCode,
                                reason);
                    bye.setHeader(reasonHeader);
                }
                catch(Throwable e)
                {
                    logger.error("Cannot set reason header", e);
                }
            }
        }

        getProtocolProvider().sendInDialogRequest(
                        getJainSipProvider(), bye, dialog);

        /*
         * Let subscriptions such as the ones associated with REFER requests
         * keep the dialog alive and correctly delete it when they are
         * terminated.
         */
        try
        {
            sendTelemetryCallEnd();
            return EventPackageUtils.processByeThenIsDialogAlive(dialog);
        }
        catch (SipException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to determine whether the dialog should stay alive.",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
            return false;
        }
    }

    /**
     * Converts the codes for hangup from OperationSetBasicTelephony one
     * to the sip codes.
     * @param reasonCode the reason code.
     * @return the sip code or -1 if not found.
     */
    private static int convertReasonCodeToSIPCode(int reasonCode)
    {
        switch(reasonCode)
        {
            case HANGUP_REASON_NORMAL_CLEARING :
                return Response.ACCEPTED;
            case HANGUP_REASON_ENCRYPTION_REQUIRED :
                return Response.SESSION_NOT_ACCEPTABLE;
            case HANGUP_REASON_TIMEOUT :
                return Response.REQUEST_TIMEOUT;
            case HANGUP_REASON_BUSY_HERE :
                return Response.BUSY_HERE;
            default : return -1;
        }
    }

    /**
     * Called when an INVITE is first received, this method does some call set
     * up (that would normally be done when the call is answered).  This reduces
     * media cut through as the time consuming set up work is already done.
     */
    public synchronized void preAnswer()
    {
        logger.entry();
        Transaction transaction = getLatestInviteTransaction();

        if (!(transaction instanceof ServerTransaction))
        {
            // Nothing that we can do.
            logger.debug("Unable to pre-answer call");
            return;
        }

        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Request invite = serverTransaction.getRequest();
        String sdpOffer = null;

        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            sdpOffer = SdpUtils.getContentAsString(invite);
        }

        // if the offer was in the invite create an SDP answer
        if (sdpOffer != null && sdpOffer.length() > 0)
        {
            getMediaHandler().preProcessOffer(sdpOffer);
        }
        else
        {
            logger.debug("Unable to preAnswer as no sdpOffer");
        }

        logger.exit();
    }

    /**
     * Indicates a user request to answer an incoming call from this
     * <tt>CallPeer</tt>.
     *
     * Sends an OK response to <tt>callPeer</tt>. Make sure that the call
     * peer contains an SDP description when you call this method.
     *
     * @throws OperationFailedException if we fail to create or send the
     * response.
     */
    public synchronized void answer()
        throws OperationFailedException
    {
        logger.debug("Call answered: " + System.currentTimeMillis());

        Call call = this.getCall();
        call.setUserPerceivedCallStartTime(System.currentTimeMillis());

        Transaction transaction = getLatestInviteTransaction();

        if (!(transaction instanceof ServerTransaction))
        {
            setState(CallPeerState.DISCONNECTED);
            throw new OperationFailedException(
                "Failed to extract a ServerTransaction "
                    + "from the call's associated dialog!",
                OperationFailedException.INTERNAL_ERROR);
        }

        CallPeerState peerState = getState();

        if (peerState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(peerState))
        {
            logger.info("Ignoring user request to answer a CallPeer "
                        + "that is already connected. CP:");
            return;
        }

        ServerTransaction serverTransaction = (ServerTransaction) transaction;
        Request invite = serverTransaction.getRequest();
        Response ok = null;
        try
        {
            ok = messageFactory.createResponse(Response.OK, invite);

            /*
             * If the local peer represented by the Call of this CallPeer is
             * acting as a conference focus, it must indicate it in its Contact
             * header.
             */
            reflectConferenceFocus(ok);
        }
        catch (ParseException ex)
        {
            setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to construct an OK response to an INVITE request",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        // Content
        ContentTypeHeader contentTypeHeader = null;
        try
        {
            // content type should be application/sdp (not applications)
            // reported by Oleg Shevchenko (Miratech)
            contentTypeHeader = getProtocolProvider().getHeaderFactory()
                .createContentTypeHeader("application", "sdp");
        }
        catch (ParseException ex)
        {
            // Shouldn't happen
            setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create a content type header for the OK response",
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        // This is the sdp offer that came from the initial invite,
        // also that invite can have no offer.
        String sdpOffer = null;
        try
        {
            // extract the SDP description.
            // beware: SDP description may be in ACKs so it could be that
            // there's nothing here - bug report Laurent Michel
            ContentLengthHeader cl = invite.getContentLength();
            if (cl != null && cl.getContentLength() > 0)
            {
                sdpOffer = SdpUtils.getContentAsString(invite);
            }

            String sdp;
            // if the offer was in the invite create an SDP answer
            if ((sdpOffer != null) && (sdpOffer.length() > 0))
            {
                sdp = getMediaHandler().processOffer(sdpOffer);
            }
            // if there was no offer in the invite - create an offer
            else
            {
                sdp = getMediaHandler().createOffer();
            }
            ok.setContent(sdp, contentTypeHeader);
        }
        catch (Exception ex)
        {
            //log, the error and tell the remote party. do not throw an
            //exception as it would go to the stack and there's nothing it could
            //do with it.
            logger.error(
                "Failed to create an SDP description for an OK response "
                    + "to an INVITE request!", ex);
            getProtocolProvider().sayError(
                            serverTransaction, Response.NOT_ACCEPTABLE_HERE);

            //do not continue processing - we already canceled the peer here
            setState(CallPeerState.FAILED, ex.getMessage());

            // Show the user an error so they actually have some clue as to
            // why the call failed.
            new ErrorDialog(resources.getI18NString("service.gui.ERROR"),
                ex.getMessage()).showDialog();

            return;
        }

        try
        {
            serverTransaction.sendResponse(ok);
            logger.debug("sent response");
        }
        catch (Exception ex)
        {
            setState(CallPeerState.DISCONNECTED);
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to send an OK response to an INVITE request",
                OperationFailedException.NETWORK_FAILURE, ex, logger);
        }

        fireRequestProcessed(invite, ok);

        // the ACK to our answer might already be processed before we get here
        if (CallPeerState.INCOMING_CALL.equals(getState()))
        {
            if (sdpOffer != null && sdpOffer.length() > 0)
            {
                // We're answering an SDP offer so media can start flowing now
                // - no need to wait for the ACK.
                sendTelemetryIncomingCall(CallAction.ACCEPTED);
                setState(CallPeerState.CONNECTED);
                getMediaHandler().start();

                // Set initial mute status to correspond with call mute status.
                // This will unmute calls that were previously muted because of
                // early media.
                if (this.getCall() != null)
                {
                    setMute(this.getCall().isMute());
                }
            }
            else
            {
                setState(CallPeerState.CONNECTING_INCOMING_CALL);
            }
        }
    }

    /**
     * Puts the <tt>CallPeer</tt> represented by this instance on or off hold.
     *
     * @param onHold <tt>true</tt> to have the <tt>CallPeer</tt> put on hold;
     * <tt>false</tt>, otherwise
     *
     * @throws OperationFailedException if we fail to construct or send the
     * INVITE request putting the remote side on/off hold.
     */
    public void putOnHold(boolean onHold)
        throws OperationFailedException
    {
        logger.info("Put " + this + (onHold ? " on" : " off") + " hold");
        CallPeerMediaHandlerSipImpl mediaHandler = getMediaHandler();
        mediaHandler.setLocallyOnHold(onHold);

        try
        {
            sendReInvite(mediaHandler.createOffer());
        }
        catch (OperationFailedException ex)
        {
            reevalLocalHoldStatus();
            throw ex;
        }
        catch (Exception ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create SDP offer to hold.", OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        reevalLocalHoldStatus();
    }

    /**
     * Sends a reINVITE request to this <tt>CallPeer</tt> within its current
     * <tt>Dialog</tt>.
     *
     * @throws OperationFailedException if sending the reINVITE request fails
     */
    void sendReInvite()
        throws OperationFailedException
    {
        logger.debug("Sending RE-INVITE");
        sendReInvite(getMediaHandler().createOffer());
    }

    /**
     * Sends a reINVITE request with a specific <tt>sdpOffer</tt> (description)
     * within the current <tt>Dialog</tt> with the call peer represented by
     * this instance.
     *
     * @param sdpOffer the offer that we'd like to use for the newly created
     * INVITE request.
     *
     * @throws OperationFailedException if sending the request fails for some
     * reason.
     */
    private void sendReInvite(String sdpOffer)
        throws OperationFailedException
    {
        logger.debug("sendReInvite");
        Dialog dialog = getDialog();
        Request invite = messageFactory.createRequest(dialog, Request.INVITE);

        try
        {
            // Content-Type
            invite.setContent(
                    sdpOffer,
                    getProtocolProvider()
                        .getHeaderFactory()
                            .createContentTypeHeader("application", "sdp"));

            /*
             * If the local peer represented by the Call of this CallPeer is
             * acting as a conference focus, it must indicate it in its Contact
             * header.
             */
            reflectConferenceFocus(invite);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                    "Failed to parse SDP offer for the new invite.",
                    OperationFailedException.INTERNAL_ERROR,
                    ex,
                    logger);
        }

        TransactionState lastTransactionState =
            (getLatestInviteTransaction() == null) ? null :
                                        getLatestInviteTransaction().getState();
        logger.debug("Sending re INVITE - last transaction: " +
                                                          lastTransactionState);

        getProtocolProvider().sendInDialogRequest(
            getJainSipProvider(), invite, dialog);
    }

    /**
     * Creates a <tt>CallPeerSipImpl</tt> from <tt>calleeAddress</tt> and sends
     * them an invite request. The invite request will be initialized according
     * to any relevant parameters in the <tt>cause</tt> message (if different
     * from <tt>null</tt>) that is the reason for creating this call.
     *
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     *
     * @throws OperationFailedException  with the corresponding code if we fail
     *  to create the call or in case someone calls us mistakenly while we
     *  are actually wrapped around an invite transaction.
     */
    public void invite(EmergencyCallContext emergency)
        throws OperationFailedException
    {
        try
        {
            ClientTransaction inviteTran
                = (ClientTransaction) getLatestInviteTransaction();
            Request invite = inviteTran.getRequest();

            // Content-Type of the SDP part
            Content sdpContent = getMediaHandler().createOfferContent();

            if (emergency == null)
            {
                // This INVITE will contain only a single message body part -
                // the SDP offer.
                logger.debug("Body contains SDP offer only");
                invite.setContent(sdpContent.getContent(),
                                  sdpContent.getContentTypeHeader());
            }
            else
            {
                // This INVITE will contain a body with an SDP offer and PIDF-LO
                // emergency call information.
                logger.info("Body contains multipart message with SDP " +
                            "offer and PIDF-LO location information");
                HeaderFactory headerFactory = getProtocolProvider().getHeaderFactory();
                PIDFLOContent pidfloContent = new PIDFLOContent(emergency, headerFactory);

                // Create an empty Multipart content header
                MultipartMimeContent multipartContent = getProtocolProvider().
                        getMessageFactory().createMultipartMimeContent();

                // Add the SDP body part and the PIDF-LO body part to the
                // multipart body.
                multipartContent.addContent(sdpContent);
                multipartContent.addContent(pidfloContent);

                // Add the multipart body to the invite
                invite.setContent(multipartContent,
                                  multipartContent.getContentTypeHeader());

                // Add any extra headers required for messages containing a
                // PIDF-LO body part.
                for (Header extraHeader : pidfloContent.getAdditionalHeaders())
                {
                    invite.addHeader(extraHeader);
                }
            }

            /*
             * If the local peer represented by the Call of this CallPeer is
             * acting as a conference focus, it must indicate it in its Contact
             * header.
             */
            reflectConferenceFocus(invite);

            inviteTran.sendRequest();
            logger.debug("sent request");
        }
        catch (Exception ex)
        {
            String message = ex.getMessage();

            if (message == null || message.equals(""))
                message =
                    SipActivator.getResources().getI18NString(ERROR_STARTING_CALL);

            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                    message,
                    OperationFailedException.NETWORK_FAILURE,
                    ex,
                    logger);
        }
    }

    /**
     * Reflects the value of the <tt>conferenceFocus</tt> property of the
     * <tt>Call</tt> of this <tt>CallPeer</tt> in the specified SIP
     * <tt>Message</tt>.
     *
     * @param message the SIP <tt>Message</tt> in which the value of the
     * <tt>conferenceFocus</tt> property of the <tt>Call</tt> of this
     * <tt>CallPeer</tt> is to be reflected
     * @throws ParseException if modifying the specified SIP <tt>Message</tt> to
     * reflect the <tt>conferenceFocus</tt> property of the <tt>Call</tt> of
     * this <tt>CallPeer</tt> fails
     */
    private void reflectConferenceFocus(javax.sip.message.Message message)
        throws ParseException
    {
        ContactHeader contactHeader
            = (ContactHeader) message.getHeader(ContactHeader.NAME);

        // If this peer has no call, most probably it means it has already
        // disconnected.
        if (contactHeader != null && getCall() != null)
        {
            // we must set the value of the parameter as null
            // in order to avoid wrong generation of the tag - ';isfocus='
            // as it must be ';isfocus'
            if (getCall().isConferenceFocus())
                contactHeader.setParameter("isfocus", null);
            else
                contactHeader.removeParameter("isfocus");
        }
    }
    /**
     * Registers a specific <tt>MethodProcessorListener</tt> with this
     * <tt>CallPeer</tt> so that it gets notified by this instance about the
     * processing of SIP signaling. If the specified <tt>listener</tt> is
     * already registered with this instance, does nothing
     *
     * @param listener the <tt>MethodProcessorListener</tt> to be registered
     * with this <tt>CallPeer</tt> so that it gets notified by this instance
     * about the processing of SIP signaling
     */
    void addMethodProcessorListener(MethodProcessorListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (methodProcessorListeners)
        {
            if (!methodProcessorListeners.contains(listener))
                methodProcessorListeners.add(listener);
        }
    }

    /**
     * Notifies the <tt>MethodProcessorListener</tt>s registered with this
     * <tt>CallPeer</tt> that it has processed a specific SIP <tt>Request</tt>
     * by sending a specific SIP <tt>Response</tt>.
     *
     * @param request the SIP <tt>Request</tt> processed by this
     * <tt>CallPeer</tt>
     * @param response the SIP <tt>Response</tt> this <tt>CallPeer</tt> sent as
     * part of its processing of the specified <tt>request</tt>
     */
    protected void fireRequestProcessed(Request request, Response response)
    {
        Iterable<MethodProcessorListener> listeners;

        synchronized (methodProcessorListeners)
        {
            listeners
                = new LinkedList<>(
                    methodProcessorListeners);
        }

        for (MethodProcessorListener listener : listeners)
            listener.requestProcessed(this, request, response);
    }

    /**
     * Notifies the <tt>MethodProcessorListener</tt>s registered with this
     * <tt>CallPeer</tt> that it has processed a specific SIP <tt>Response</tt>
     * by sending a specific SIP <tt>Request</tt>.
     *
     * @param response the SIP <tt>Response</tt> processed by this
     * <tt>CallPeer</tt>
     * @param request the SIP <tt>Request</tt> this <tt>CallPeer</tt> sent as
     * part of its processing of the specified <tt>response</tt>
     */
    protected void fireResponseProcessed(Response response, Request request)
    {
        Iterable<MethodProcessorListener> listeners;

        synchronized (methodProcessorListeners)
        {
            listeners
                = new LinkedList<>(
                    methodProcessorListeners);
        }

        for (MethodProcessorListener listener : listeners)
            listener.responseProcessed(this, response, request);
    }

    /**
     * Unregisters a specific <tt>MethodProcessorListener</tt> from this
     * <tt>CallPeer</tt> so that it no longer gets notified by this instance
     * about the processing of SIP signaling. If the specified <tt>listener</tt>
     * is not registered with this instance, does nothing.
     *
     * @param listener the <tt>MethodProcessorListener</tt> to be unregistered
     * from this <tt>CallPeer</tt> so that it no longer gets notified by this
     * instance about the processing of SIP signaling
     */
    void removeMethodProcessorListener(MethodProcessorListener listener)
    {
        if (listener != null)
            synchronized (methodProcessorListeners)
            {
                methodProcessorListeners.remove(listener);
            }
    }

    /**
     * Requests a (video) key frame from this remote peer of the associated.
     *
     * @return <tt>true</tt> if a key frame has indeed been requested from this
     * remote peer in response to the call; otherwise, <tt>false</tt>
     */
    private boolean requestKeyFrame()
    {
        logger.debug("Requesting a SIP INFO key frame");
        boolean requested = false;

        if (sendPictureFastUpdate)
        {
            try
            {
                pictureFastUpdate();
                requested = true;
            }
            catch (OperationFailedException ofe)
            {
                /*
                 * Apart from logging, it does not seem like there are a lot of
                 * ways to handle it.
                 */
            }
        }
        return requested;
    }

    /**
     * Updates this call so that it would record a new transaction and dialog
     * that have been recreated because of a re-send, either due to
     * re-authentication or to receiving a 'request pending' response.
     *
     * @param retryTran the new transaction
     */
    public void handleResendRequest(ClientTransaction retryTran)
    {
        // There is a new dialog that will be started with this request. Get
        // that dialog and record it into the Call object for later use (by
        // BYEs for example).
        // if the request was BYE then we need to authorize it anyway even
        // if the call and the call peer are no longer there
        setDialog(retryTran.getDialog());
        setLatestInviteTransaction(retryTran);
        setJainSipProvider(jainSipProvider);
    }

    /**
     * Causes this CallPeer to enter either the DISCONNECTED or the FAILED
     * state.
     *
     * @param failed indicates if the disconnection is due to a failure
     * @param reason the reason of the disconnection
     */
    private void setDisconnectedState(boolean failed, String reason, int reasonCode)
    {
        if (failed)
        {
            setState(CallPeerState.FAILED, reason, reasonCode);
        }
        else
        {
            setState(CallPeerState.DISCONNECTED, reason, reasonCode);
        }
    }

    /**
     * Returns the MetaContact associated with this call peer
     *
     * @return the MetaContact associated with this call peer
     */
    protected MetaContact getPeerContact()
    {
        return peerContact;
    }

    /**
     * Get the appropriate display name to indicate to the user that there are
     * multiple possible names, e.g. "John Smith and 3 others".
     *
     * @param firstMatch The first matching name found.
     * @param numMatches The total number of matching names found.
     * @return A string, consisting of the first name found, followed by
     * "or x other/others" (substituting x, and using the correct pluralization)
     */
    private String getNameFromMultipleMatches(String firstMatch, int numMatches)
    {
        // Default to just showing the first match, in the case of an error.
        String result = firstMatch;

        if (numMatches == 2)
        {
            String[] params = new String[]
            {
                firstMatch
            };

            result = resources.getI18NString(OTHER_STRING_KEY, params);
        }
        else if (numMatches > 2)
        {
            String[] params = new String[]
            {
                firstMatch,
                Integer.toString(numMatches - 1)
            };

            result = resources.getI18NString(OTHERS_STRING_KEY, params);
        }
        else
        {
            logger.error("setStringWithMultipleMatches called with " +
                         numMatches + " matching names; expected > 2.");
        }

        return result;
    }

    @VisibleForTesting
    public static class NameAndNumber
    {
        public final String name;
        public final String number;

        public NameAndNumber(String name, String number)
        {
            this.name = name;
            this.number = number;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NameAndNumber that = (NameAndNumber) o;
            return Objects.equals(name, that.name) && Objects.equals(number, that.number);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(name, number);
        }
    }

    // Parse the value from the diversion header, to get the text that we should
    // use in the UI.
    // Effectively a port of the Android code SIPURIParser, without fixing any
    // of the bugs there!
    // Specifically, I believe this is a valid diversion header
    //   Diversion: <sip:userA>;count=23
    // Note the sip address is an alphabetic name which does not contain an @.
    // This code specifically handles the absence of the @ by keeping
    // numeric digits from the remainder of the string, and thus ends up
    // using the digits from the count at the end of the header.
    @VisibleForTesting
    public static CallPeerSipImpl.NameAndNumber extractDiversionValue(String diversion)
    {
        String name = null;
        String number = null;
        if (diversion != null)
        {
            // We expect various parts in the diversion info
            // - an optional friendly name
            // - an address
            // - optional parameters (not specifically accounted for in the Android code we have ported)
            String uri = diversion;
            if (diversion.startsWith("\""))
            {
                // A friendly name is present.
                // Get the display name from the URI and replace escaped characters.
                name = diversion.substring(1, diversion.lastIndexOf('"'))
                        .replaceAll("\\\\([\\\\|\"])", "$1");

                // I'm not sure if there is another bug here - the parameters
                // may include string values, and if so it's possible that the
                // < or > characters could appear in those strings.
                uri = diversion.substring(diversion.lastIndexOf('<'), diversion.lastIndexOf('>'));
            }

            // I believe the absence of an @ handling is bugged;
            // - a sip address without an @ may be alphabetic, not numeric
            // - even if numeric, we should not include digits that appear after the end of the sip url
            int numberEndIndex = uri.indexOf('@');
            // Note unlike the Android code, we don't do % decoding here,
            // that uses an Android only method, and I see no evidence that
            // the data will be encoded using that method anyway.
            number = uri.substring(uri.indexOf(':') + 1, numberEndIndex < 0 ? uri.length() : numberEndIndex);

            // Strip out invalid chars.
            number = number.replaceAll("[^0-9#*+]+", "");
        }

        return new CallPeerSipImpl.NameAndNumber(name, number);
    }

    /**
     * Class used to update the display name of the peer based on query results
     * from an <tt>ExtendedContactSourceService</tt>.
     */
    private class NameQueryListener implements ContactQueryListener
    {
        /**
         * The lock for all member variables of this class.
         */
        private final Object resultsLock = new Object();

        /**
         * The number of results found by the query so far.
         */
        private int resultsFound = 0;

        /**
         * The display name of the first contact found by the query.
         */
        private String firstNameFound = null;

        /**
         * The type of <tt>ExtendedContactSourceService</tt> the query is
         * against. This is used only for logging purposes.
         */
        private final String serviceType;

        /**
         * Create a new <tt>NameQueryListener</tt>.
         *
         * @param serviceType The type of source service the query is against;
         * this is used for logging.
         */
        public NameQueryListener(String serviceType)
        {
            this.serviceType = serviceType;
        }

        public void contactReceived(
                          ContactReceivedEvent event)
        {
            // A new match has been found.
            SourceContact match = event.getContact();

            synchronized (resultsLock)
            {
                // No need to hash SourceContact, as its toString() method
                // does that.
                resultsFound += 1;
                if (resultsFound == 1)
                {
                    logger.debug(serviceType + " found match for peer " +
                                 "name: " + match);
                    firstNameFound = match.getDisplayName();
                    setDisplayNameFromExternalQuery(firstNameFound);
                }
                else
                {
                    // No need to hash SourceContact, as its toString() method
                    // does that.
                    logger.debug(serviceType + " found alternative match" +
                                 " for peer name: " + match);
                    setDisplayNameFromExternalQuery(
                        getNameFromMultipleMatches(firstNameFound, resultsFound));
                }
            }
        }

        public void queryStatusChanged(
                       ContactQueryStatusEvent event)
        {
            // This indicates the query has cancelled, ended, or hit an error.
            // In all cases, we should stop listening.
            logger.debug(serviceType + " query for call peer name ended. " +
                         resultsFound + " result(s) were found.");
            event.getQuerySource().removeContactQueryListener(this);
        }

        public void contactRemoved(
                           ContactRemovedEvent event)
        {
            // No action required.
        }

        public void contactChanged(
                           ContactChangedEvent event)
        {
            // No action required.
        }
    }

    private void sendTelemetryIncomingCall(CallAction action)
    {
        SipActivator.getInsightService().logEvent(new InsightEvent(CallKt.EVENT_INCOMING_CALL,
                                                                   Map.of(AnalyticsResultKt.PARAM_RESULT,
                                                                          action.getValue$maxanalytics(),
                                                                          CallKt.PARAM_CALL_DIVERTED,
                                                                          "false")));
    }

    private void sendTelemetryCallEnd()
    {
        SipActivator.getInsightService().logEvent(
                new InsightEvent(CallKt.EVENT_CALL_END,
                                 Map.of(
                                         CommonKt.PARAM_TYPE,
                                         isLocalVideoStreaming() ?
                                                 CallType.VIDEO.getValue$maxanalytics() :
                                                 CallType.AUDIO.getValue$maxanalytics()
                                 )
                )
        );
    }
}
