// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * An abstract contact class for Outlook and Mac Address book contacts.
 */
public abstract class AbstractAddressBookContact implements Contact
{
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    private static final PresenceStatus PRESENCE_STATUS =
        new AddressBookPresenceStatus(100, "Available",
            AddressBookProtocolActivator.getResources().getBufferedImage(
                                                "plugin.addressbook.PRESENCE"));

    /**
     * The id of this contact
     */
    private String id = null;

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
     * Whether this contact is a favourite
     */
    protected boolean isFavourite = false;

    /**
     * The current status message of this contact.
     */
    private String statusMessage = null;

    /**
     * The persistent data of this contact
     */
    private String mPersistentData = "";

    /**
     * The protocol provider service that created us
     */
    private ProtocolProviderService pps;

    /**
     * The list of contact details for this contact
     */
    protected List<GenericDetail> contactDetails;

    /**
     * The contact group to which this contact belongs
     */
    private ContactGroup group;

    /**
     * Used to create unresolved contacts with specified id.
     * @param id contact id
     * @param pps the protocol provider service that creates us.
     * @param isPersistent is the contact persistent.
     * @param contactDetails
     */
    AbstractAddressBookContact(String id,
                               ProtocolProviderService pps,
                               boolean isPersistent,
                               List<GenericDetail> contactDetails,
                               ContactGroup group)
    {
        this.id = id;
        this.pps = pps;
        this.isPersistent = isPersistent;
        this.isResolved = false;
        this.contactDetails = contactDetails;
        this.group = group;
    }

    public String getAddress()
    {
        return id;
    }

    public BufferedImageFuture getImage()
    {
        return null;
    }

    public ImageIconFuture getOverlay(Dimension size)
    {
        // By default, overlays are not supported
        return null;
    }

    @Override
    public String toString()
    {
        StringBuffer buff =  new StringBuffer("AddressBookContact[ id=");
        buff.append(logHasher(getAddress())).
            append(", isPersistent=").append(isPersistent).
            append(", isResolved=").append(isResolved).append("]");

        return buff.toString();
    }

    /**
     * Sets the status that this contact is currently in. The method is to
     * only be called as a result of a status update received from the server.
     *
     * @param status the StatusEnum that this contact is currently in.
     */
    void updatePresenceStatus(PresenceStatus status)
    {
        // Nothing to do
    }

    /**
     * Returns a reference to the protocol provider that created the contact.
     * @return a reference to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return pps;
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
     * Specifies whether this contact is to be considered persistent or not. The
     * method is to be used _only_ when a non-persistent contact has been added
     * to the contact list and its encapsulated VolatileBuddy has been repalced
     * with a standard buddy.
     * @param persistent true if the buddy is to be considered persistent and
     * false for volatile.
     */
    void setPersistent(boolean persistent)
    {
        this.isPersistent = persistent;
    }

    /**
     * Returns the persistent data
     * @return the persistent data
     */
    public String getPersistentData()
    {
        return mPersistentData;
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
     * Marks this contact as resolved. A contact is resolved when all contact
     * details have been synchronised with the contact on the server.
     *
     * @param resolved whether this contact is resolved
     */
    public void setResolved(boolean resolved)
    {
        contactLogger.debug(this, "Marking contact as resolved " + resolved);
        isResolved = resolved;
    }

    /**
     * Sets the persistent data of this contact. If there is no associated
     * SourceContact then create one using the persistent data.
     *
     * @param persistentData the persistent data.
     */
    public void setPersistentData(String persistentData)
    {
        mPersistentData = persistentData;
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

    public boolean supportResources()
    {
        return false;
    }

    public Collection<ContactResource> getResources()
    {
        return null;
    }

    public void addResourceListener(ContactResourceListener listener)
    {
        // Not supported
    }

    public void removeResourceListener(ContactResourceListener listener)
    {
        // Not supported
    }

    @Override
    public boolean supportsIMAutoPopulation()
    {
        // Currently only BGContacts support IM autopopulation.
        return false;
    }

    public ContactGroup getParentContactGroup()
    {
        return group;
    }

    public PresenceStatus getPresenceStatus()
    {
        return PRESENCE_STATUS;
    }

    /**
     * A presence status for Outlook and Mac Address Book contacts
     */
    private static class AddressBookPresenceStatus extends PresenceStatus
    {
        protected AddressBookPresenceStatus(int status, String statusName, BufferedImageFuture icon)
        {
            super(status, statusName, icon);
        }
    }

    /**
     * Get the details of a contact
     *
     * @param detailClass The sort of details that we are interested in. If null
     *                    then all details will be returned.
     * @return the details of a contact
     */
    public ArrayList<GenericDetail> getDetails(
                                     Class<? extends GenericDetail> detailClass)
    {
        ArrayList<GenericDetail> details;

        if (detailClass == null)
        {
            details = new ArrayList<>(contactDetails);
        }
        else
        {
            details = new ArrayList<>();
            for (ServerStoredDetails.GenericDetail detail : contactDetails)
            {
                if (detail.getClass().equals(detailClass))
                {
                    details.add(detail);
                }
            }
        }

        return details;
    }

    /**
     * Sets the details of this contact, used for update operations.
     *
     * @param contactDetails
     */
    public void setDetails(List<GenericDetail> contactDetails)
    {
        this.contactDetails = contactDetails;
    }

    /**
     * Gets the favourite status of this contact
     *
     * @return the favourite status of this contact
     */
    public boolean getFavoriteStatus()
    {
        return isFavourite;
    }

    /**
     * Sets the favourite status of this contact
     *
     * @param newStatus whether this contact is a favourite
     */
    public void setFavoriteStatus(boolean newStatus)
    {
        if (isFavourite != newStatus)
        {
            isFavourite = newStatus;
            contactLogger.debug(this, "Setting favorite status to " + newStatus);
            ((AbstractAddressBookProtocolProviderService) getProtocolProvider()).
                                          setContactAsFavorite(id, newStatus);
        }
        else
        {
            contactLogger.debug(this, "Asked to set favorite status to " +
                newStatus + ", but it was already set.");
        }
    }

    public String getDisplayName()
    {
        StringBuilder displayName = new StringBuilder();

        // Get the first and last name details for this contact
        ArrayList<GenericDetail> firstNameDetails =
                          getDetails(ServerStoredDetails.FirstNameDetail.class);
        ArrayList<GenericDetail> lastNameDetails =
                           getDetails(ServerStoredDetails.LastNameDetail.class);

        // Check the details for first name and last name. Construct a display
        // name if one or more is found.
        for (ArrayList<GenericDetail> detail : Arrays.asList(firstNameDetails,
                                                             lastNameDetails))
        {
            if (detail.size() > 0)
            {
                Object detailValue = detail.get(0).getDetailValue();
                if (detailValue instanceof String &&
                    !StringUtils.isNullOrEmpty((String) detailValue))
                {
                    displayName.append(detailValue).append(" ");
                }
            }
        }

        // If no display name has been found then revert to using the resource
        // for unknown contact.
        if (StringUtils.isNullOrEmpty(displayName.toString()))
        {
            return AddressBookProtocolActivator.getResources().
                    getI18NString("service.gui.UNKNOWN");
        }

        return displayName.toString().trim();
    }
}
