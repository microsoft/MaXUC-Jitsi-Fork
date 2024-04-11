/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.impl.protocol.jabber.OperationSetPersistentPresenceJabberImpl.*;
import static org.jitsi.util.Hasher.logHasher;
import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatAddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.service.protocol.AbstractContact;
import net.java.sip.communicator.service.protocol.ContactGroup;
import net.java.sip.communicator.service.protocol.ContactResource;
import net.java.sip.communicator.service.protocol.ContactType;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactResourceEvent;
import net.java.sip.communicator.service.protocol.jabberconstants.JabberStatusEnum;
import net.java.sip.communicator.util.ContactLogger;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.resources.BufferedImageFuture;

/**
 * The Jabber implementation of the service.protocol.Contact interface.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class ContactJabberImpl
    extends AbstractContact
{
    private static final Logger logger = Logger.getLogger(ContactJabberImpl.class);

    private static final ContactLogger contactLogger =
        ContactLogger.getLogger();

    /**
     * The jid of the user entry in roster.
     */
    private BareJid jid = null;

    /**
     * The image of the contact.
     */
    private BufferedImageFuture image = null;

    /**
     * The status of the contact as per the last status update we've
     * received for it.
     */
    private PresenceStatus status;

    /**
     * A reference to the ServerStoredContactListImpl
     * instance that created us.
     */
    private final ServerStoredContactListJabberImpl ssclCallback;

    /**
     * Whether or not this contact is being stored by the server.
     */
    private boolean isPersistent = false;

    /**
     * Whether or not this contact has been resolved against the
     * server.
     */
    private boolean isResolved = false;

    /**
     * Used to store contact id when creating unresolved contacts.
     */
    private final BareJid tempId;

    /**
     * The current status message of this contact.
     */
    private String statusMessage = null;

    /**
     * The display name of the roster entry.
     */
    private String serverDisplayName = null;

    /**
     * Most recent resource used for this contact.
     */
    private ContactResource mLatestResource = null;

    /**
     * The contact resources list.
     */
    private Map<FullJid, ContactResourceJabberImpl> resources =
            new ConcurrentHashMap<>();

    /**
     * Creates a ContactJabberImpl
     * @param rosterEntry the RosterEntry object that we will be encapsulating.
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     * instance that created us.
     * @param isPersistent determines whether this contact is persistent or not.
     * @param isResolved specifies whether the contact has been resolved against
     * the server contact list
     */
    ContactJabberImpl(RosterEntry rosterEntry,
                      ServerStoredContactListJabberImpl ssclCallback,
                      boolean isPersistent,
                      boolean isResolved)
    {
        this(null, rosterEntry, ssclCallback, isPersistent, isResolved);
    }

    /**
     * Used to create unresolved contacts with specified id.
     *
     * @param id contact id
     * @param ssclCallback the contact list handler that creates us.
     * @param isPersistent is the contact persistent.
     */
    ContactJabberImpl(BareJid id,
                      ServerStoredContactListJabberImpl ssclCallback,
                      boolean isPersistent)
    {
        this(id, null, ssclCallback, isPersistent, false);
    }

    /**
     * Creates a ContactJabberImpl
     * @param id contact id
     * @param rosterEntry the RosterEntry object that we will be encapsulating.
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     * instance that created us.
     * @param isPersistent determines whether this contact is persistent or not.
     * @param isResolved specifies whether the contact has been resolved against
     * the server contact list
     */
    private ContactJabberImpl(BareJid id,
                              RosterEntry rosterEntry,
                              ServerStoredContactListJabberImpl ssclCallback,
                              boolean isPersistent,
                              boolean isResolved)
    {
        // rosterEntry can be null when creating volatile contact
        if(rosterEntry != null)
        {
            this.jid = rosterEntry.getJid();
            this.serverDisplayName = rosterEntry.getName();
        }

        this.tempId = id;
        this.ssclCallback = ssclCallback;
        this.isPersistent = isPersistent;
        this.isResolved = isResolved;

        // Default the contact to "offline" rather then "unknown", as if this
        // is a BG IM contact, it's possible that the contact won't have been
        // created on the server yet and may not be for a while.  We won't
        // receive status for them until that happens and we want to make sure
        // that all such contacts are "offline" in the UI so that the fact that
        // they don't really exist is is hidden from the user.
        ProtocolProviderServiceJabberImpl jabberProvider =
            (ProtocolProviderServiceJabberImpl) getProtocolProvider();
        if (jabberProvider != null)
        {
            this.status = jabberProvider.getJabberStatusEnum().getStatus(
                JabberStatusEnum.OFFLINE_STATUS);
        }
        else
        {
            // The jabber provider may be null if we're running a UT.
            // Otherwise that really should not happen, so log just in case.
            logger.error(
                "Jabber provider is null - this may be OK if we're running a UT!",
                new IllegalStateException());
        }
    }

    /**
     * Returns the Jabber Userid (Bare JID) of this contact as a String.
     * If the JID is null, it returns an empty String.
     *
     * @return the Jabber Userid (Bare JID) of this contact as a String
     */
    public String getAddress()
    {
        BareJid bareJid = getAddressAsJid();
        return bareJid != null ? bareJid.toString() : "";
    }

    /**
     * Returns the Jabber Userid (Bare JID) of this contact as a JXMPP Jid
     * We return the more general Jid class as some upstream methods
     * @return the Jabber Userid (Bare JID) of this contact as a JXMPP Jid
     */
    public BareJid getAddressAsJid()
    {
        return isResolved ? jid : tempId;
    }

    /**
     * Determines whether or not this Contact instance represents the user used
     * by this protocol provider to connect to the service.
     *
     * @return true if this Contact represents us (the local user) and false
     * otherwise.
     */
    public boolean isLocal()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Returns an avatar if one is already present or <tt>null</tt> in case it
     * is not in which case it the method also queues the contact for image
     * updates.
     *
     * @return the avatar of this contact or <tt>null</tt> if no avatar is
     * currently available.
     */
    public BufferedImageFuture getImage()
    {
        return getImage(true);
    }

    /**
     * Returns a reference to the image assigned to this contact. If no image
     * is present and the retrieveIfNecessary flag is true, we schedule the
     * image for retrieval from the server.
     *
     * @param retrieveIfNecessary specifies whether the method should queue
     * this contact for avatar update from the server.
     *
     * @return a reference to the image currently stored by this contact.
     */
    public BufferedImageFuture getImage(boolean retrieveIfNecessary)
    {
        if(image == null && retrieveIfNecessary)
            ssclCallback.addContactForImageUpdate(this);
        return image;
    }

    /**
     * Set the image of the contact
     *
     * @param imgBytes the bytes of the image that we'd like to set.
     */
    public void setImage(BufferedImageFuture imgBytes)
    {
        this.image = imgBytes;
    }

    /**
     * Returns a hashCode for this contact. The returned hashcode is actually
     * that of the Contact's Address
     * @return the hashcode of this Contact
     */
    @Override
    public int hashCode()
    {
        return getAddress().toLowerCase().hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     *
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof String || obj instanceof ContactJabberImpl))
            return false;

        if (obj instanceof ContactJabberImpl
            && ((ContactJabberImpl)obj).getAddress()
                                            .equalsIgnoreCase(getAddress())
            && ((ContactJabberImpl)obj).getProtocolProvider()
                                            == getProtocolProvider())
        {
            return true;
        }

        if (obj instanceof String)
        {
            int atIndex = getAddress().indexOf("@");

            if (atIndex > 0)
            {
                if (getAddress().equalsIgnoreCase((String) obj)
                    || getAddress().substring(0, atIndex)
                        .equalsIgnoreCase((String) obj))
                    {
                        return true;
                    }
            }
            else if (getAddress().equalsIgnoreCase((String) obj))
                return true;
        }

        return false;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    @Override
    public String toString()
    {
        StringBuffer buff =  new StringBuffer("ContactJabberImpl[ id=");
        buff.append(sanitiseChatAddress(getAddress())).
            append(", isPersistent=").append(isPersistent).
            append(", isResolved=").append(isResolved).append("]");

        return buff.toString();
    }

    /**
     * Sets the status that this contact is currently in. The method is to
     * only be called as a result of a status update received from the server.
     * If calling this function, consider whether you also need to call setStatusMessage()
     * to keep state consistent.
     *
     * @param status the JabberStatusEnum that this contact is currently in.
     */
    void updatePresenceStatus(PresenceStatus status)
    {
        this.status = status;
    }

    /**
     * Returns the status of the contact as per the last status update we've
     * received for it. Note that this method is not to perform any network
     * operations and will simply return the status received in the last
     * status update message. If you want a reliable way of retrieving someone's
     * status, you should use the <tt>queryContactStatus()</tt> method in
     * <tt>OperationSetPresence</tt>.
     * @return the PresenceStatus that we've received in the last status update
     * pertaining to this contact.
     */
    public PresenceStatus getPresenceStatus()
    {
        return status;
    }

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact. An alias is not necessarily unique but is
     * often more human readable than an address (or id).
     * @return a String that can be used for referring to this contact when
     * interacting with the user.
     */
    public String getDisplayName()
    {
        if(isResolved)
        {
            RosterEntry entry = ssclCallback.getRosterEntry(jid);
            String name = null;

            if (entry != null)
                name = entry.getName();

            if ((name != null) && (name.trim().length() != 0))
                return name;
        }
        return getAddress();
    }

    /**
     * Returns the display name used when the contact was resolved.
     * Used to detect renames.
     * @return the display name.
     */
    String getServerDisplayName()
    {
        return serverDisplayName;
    }

    /**
     * Changes locally stored server display name.
     * @param newValue
     */
    void setServerDisplayName(String newValue)
    {
        this.serverDisplayName = newValue;
    }

    /**
     * Returns a reference to the contact group that this contact is currently
     * a child of or null if the underlying protocol does not support persistent
     * presence.
     * @return a reference to the contact group that this contact is currently
     * a child of or null if the underlying protocol does not support persistent
     * presence.
     */
    public ContactGroup getParentContactGroup()
    {
        return ssclCallback.findContactGroupFromJid(this.getAddressAsJid());
    }

    /**
     * Returns a reference to the protocol provider that created the contact.
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        ProtocolProviderService protocolProvider = null;
        protocolProvider = ssclCallback.getParentProvider();

        return protocolProvider;
    }

    /**
     * Determines whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     * @return true if the contact is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Resolve this contact against the given entry
     * @param entry the server stored entry
     */
    void setResolved(RosterEntry entry)
    {
        // Update the display name to the latest one from the server, even if
        // the contact is already resolved, as we might have previously set it
        // just to the user ID and the server might know the user's real name.
        this.serverDisplayName = entry.getName();

        if(isResolved && isPersistent)
            return;

        this.isResolved = true;
        this.isPersistent = true;
        this.jid = entry.getJid();
    }

    /**
     * Resolve this contact, as we have received presence for them so they
     * must be in our roster on the server.
     *
     * @param presence the presence received from the server for this contact
     */
    void setResolved(Presence presence)
    {
        if(isResolved && isPersistent)
            return;

        BareJid userID = presence.getFrom().asBareJid();

        this.isResolved = true;
        this.isPersistent = true;
        this.jid = userID;

        // Only set the display name if we don't already have one, as the
        // display name might already be set to the user's real name, which is
        // better than using the user ID.
        if (this.serverDisplayName == null)
        {
            this.serverDisplayName = userID.toString();
        }
    }

    /**
     * Returns the persistent data
     * @return the persistent data
     */
    public String getPersistentData()
    {
        return null;
    }

    /**
     * Determines whether or not this contact has been resolved against the
     * server. Unresolved contacts are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped contacts to their on-line buddies.
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    /**
     * Not used.
     * @param persistentData the persistent data.
     */
    public void setPersistentData(String persistentData)
    {
    }

    /**
     * Get source entry
     * @return RosterEntry
     */
    RosterEntry getSourceEntry()
    {
        return ssclCallback.getRosterEntry(jid);
    }

    /**
     * Return the current status message of this contact.
     *
     * @return the current status message
     */
    public String getStatusMessage()
    {
        return statusMessage;
    }

    /**
     * Sets the current status message for this contact
     * If calling this function, consider whether you also need to call updatePresenceStatus()
     * to keep state consistent.
     * @param statusMessage the message
     */
    protected void setStatusMessage(String statusMessage)
    {
        this.statusMessage = statusMessage;
    }

    /**
     * Indicates if this contact supports resources.
     *
     * @return <tt>false</tt> to indicate that this contact doesn't support
     * resources
     */
    public boolean supportResources()
    {
        return true;
    }

    /**
     * Returns an iterator over the resources supported by this contact.
     *
     * @return an iterator over the resources supported by this contact
     */
    public Collection<ContactResource> getResources()
    {
       return new ArrayList<>(resources.values());
    }

    /**
     * Fetch the contact type as an alternative to <tt>instanceof</tt> checks.
     * Used to differentiate between Jabber and Volatile Jabber.
     */
    @Override
    public ContactType getContactType()
    {
        return ContactType.JABBER;
    }

    /**
     * Set the most recent resource to be used by this contact.
     */
    public void setLatestResource(ContactResource latestResource)
    {
        mLatestResource = latestResource;
    }

    /**
     * @return most recent resource to be used by this contact.
     */
    public ContactResource getLatestResource()
    {
        return mLatestResource;
    }

    /**
     * Finds the <tt>ContactResource</tt> corresponding to the given jid.
     *
     * @param jid the jid for which we're looking for a resource
     * @return the <tt>ContactResource</tt> corresponding to the given jid.
     */
    ContactResource getResourceFromJid(Jid jid)
    {
        return resources.get(jid);
    }

    /**
     * Updates the resources for this contact.
     */
    void updateResources()
    {
        if (jid == null)
        {
            logger.warn(
                "Unable to update resources for jabber contact with null jid");
            return;
        }

        XMPPConnection providerConnection =
            ((ProtocolProviderServiceJabberImpl) getProtocolProvider()).getConnection();

        if (providerConnection != null)
        {
            // Get all of the existing Presences that we know about for this
            // jid.
            Iterator<Presence> it =
                Roster.getInstanceFor(providerConnection).getPresences(
                    jid).iterator();

            // Choose the resource which has the highest priority.  If we have
            // two resources with same priority take the most available.
            while (it.hasNext())
            {
                Presence presence = it.next();
                String loggableBareJid = sanitiseChatAddress(jid);
                FullJid fullJid = presence.getFrom().asFullJidIfPossible();
                Resourcepart resource = fullJid != null ? fullJid.getResourceOrEmpty() : null;

                contactLogger.debug("Processing presence for resource " +
                                    loggableBareJid + "/" + resource);

                if (resource != null && resource.length() > 0)
                {
                    // This Presence has a resource string, so see if we
                    // already have a ContactResource for this resource in our
                    // local map.
                    ContactResourceJabberImpl contactResource =
                        resources.get(fullJid);

                    // Convert the Presence to a PresenceStatus
                    PresenceStatus newPresenceStatus =
                        OperationSetPersistentPresenceJabberImpl.
                            jabberStatusToPresenceStatus(
                                presence,
                                (ProtocolProviderServiceJabberImpl) getProtocolProvider());

                    if (contactResource == null)
                    {
                        // If the resource is from a non-Accession client
                        // sending LSM presence via AMS their resource will be
                        // "ext_client" or "aux_client".
                        boolean isLsmResource =
                                         EXT_CLIENT_RESOURCE.equals(resource) ||
                                         AUX_CLIENT_RESOURCE.equals(resource);

                        if (isLsmResource)
                        {
                            try
                            {
                                // Only one LSM resource is valid per contact at a
                                // time so a new "aux_client" resource should
                                // replace any existing "aux_client" or
                                // "ext_client" resource.  We didn't find a
                                // ContactResource for this LSM resource string in
                                // our local map, so we need to check whether there
                                // is a ContactResource for the alternative LSM
                                // resource string.  If there is, we need to remove
                                // it before adding the new resource.
                                BareJid bareJid = fullJid.asBareJid();
                                Resourcepart altRes = Resourcepart.from(
                                    EXT_CLIENT_RESOURCE.equals(resource) ?
                                        AUX_CLIENT_RESOURCE :
                                            EXT_CLIENT_RESOURCE);
                                FullJid altFullJid = JidCreate.fullFrom(bareJid, altRes);
                                ContactResourceJabberImpl altResource =
                                    resources.remove(altFullJid);

                                if (altResource != null)
                                {
                                    logger.debug(
                                        "Removed old LSM resource for " + sanitiseChatAddress(bareJid) + "/" + altFullJid);

                                    fireContactResourceEvent(
                                        new ContactResourceEvent(this, altResource,
                                            ContactResourceEvent.RESOURCE_REMOVED));
                                }
                            }
                            catch (XmppStringprepException e)
                            {
                                logger.error("Error removing old LSM resource for "
                                    + sanitiseChatAddress(fullJid.asBareJid()), e);
                            }
                        }

                        // LSM resources cannot be messaged directly (but are
                        // required to allow us to choose the correct presence
                        // to display).
                        logger.debug("Adding resource " + resource +
                            ", canBeMessaged = " + !isLsmResource + " for " + loggableBareJid);

                        contactResource =
                            new ContactResourceJabberImpl(fullJid,
                                                          this,
                                                          resource,
                                                          newPresenceStatus,
                                                          presence.getPriority(),
                                                          !isLsmResource);

                        resources.put(fullJid, contactResource);

                        fireContactResourceEvent(
                            new ContactResourceEvent(this, contactResource,
                                ContactResourceEvent.RESOURCE_ADDED));
                    }
                    else
                    {
                        int oldStatus = contactResource.getPresenceStatus().getStatus();
                        int newStatus = newPresenceStatus.getStatus();

                        contactLogger.debug("Received status update for resource " +
                                     loggableBareJid + "/" + resource +
                                     ", canBeMessaged = " +
                                     contactResource.canBeMessaged());

                        if (oldStatus != newStatus)
                        {
                            contactResource.setPresenceStatus(newPresenceStatus);

                            fireContactResourceEvent(
                                new ContactResourceEvent(this, contactResource,
                                    ContactResourceEvent.RESOURCE_MODIFIED));
                        }
                    }
                }
            }

            Iterator<FullJid> resourceIter = resources.keySet().iterator();
            while (resourceIter.hasNext())
            {
                FullJid fullJid = resourceIter.next();
                String fullJidString = fullJid.toString();

                if (!Roster.getInstanceFor(providerConnection).getPresenceResource(fullJid)
                    .isAvailable())
                {
                    ContactResource removedResource = resources.remove(fullJid);

                    if (removedResource != null)
                    {
                        logger.debug(
                            "Removed unavailable resource for " + logHasher(fullJidString));

                        fireContactResourceEvent(
                            new ContactResourceEvent(this, removedResource,
                                ContactResourceEvent.RESOURCE_REMOVED));
                    }
                    else
                    {
                        logger.debug(
                            "No existing resource found to remove for " + logHasher(fullJidString));
                    }
                }
            }
        }
    }
}
