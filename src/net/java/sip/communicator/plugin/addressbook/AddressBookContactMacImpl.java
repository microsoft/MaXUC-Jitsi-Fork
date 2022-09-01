// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;

/**
 * Mac Address Book implementation of a contact
 */
public class AddressBookContactMacImpl
    extends AbstractAddressBookContact
{
    /**
     * A list of details stored in the Mac Address Book that we need to
     * include when writing back to the address book. These details are not
     * exposed in Accession but must be stored for writing back to Mac Address
     * Book in order to not lose any details
     */
    private List<GenericDetail> serverDetails = new ArrayList<>();

    /**
     * Constructor for a new Mac Address Book contact
     *
     * @param id the Accession ID for this contact
     * @param pps the protocol provider service that created us
     * @param isPersistent whether this contact is persistent
     * @param contactDetails the list of details for this contact
     * @param serverDetails the list of server stored details for this contact
     * @param group the group to which this contact belongs
     */
    AddressBookContactMacImpl(String id,
                             ProtocolProviderService pps,
                             boolean isPersistent,
                             List<GenericDetail> contactDetails,
                             List<GenericDetail> serverDetails,
                             ContactGroup group)
    {
        super(id, pps, isPersistent, contactDetails, group);

        if (serverDetails != null)
        {
            this.serverDetails = serverDetails;
        }
    }

    /**
     * Returns the list of server details for this contact
     *
     * @return the list of server details
     */
    public List<GenericDetail> getServerDetails()
    {
        return serverDetails;
    }
}
