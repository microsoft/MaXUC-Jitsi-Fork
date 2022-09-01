// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;

/**
 * Outlook implementation of a contact
 */
public class AddressBookContactOutlookImpl
    extends AbstractAddressBookContact
{
    /**
     * The contact logger
     */
    private static ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * Constructor for a new Outlook contact
     *
     * @param id the Accession ID for this contact
     * @param pps the protocol provider service that created us
     * @param isPersistent whether this contact is persistent
     * @param contactDetails the list of details for this contact
     * @param group the group to which this contact belongs
     * @param isFavourite whether to create this contact as a favourite
     */
    AddressBookContactOutlookImpl(String id,
                                  ProtocolProviderService pps,
                                  boolean isPersistent,
                                  List<GenericDetail> contactDetails,
                                  ContactGroup group,
                                  boolean isFavourite)
    {
        super(id, pps, isPersistent, contactDetails, group);
        contactLogger.debug(this,
                            "Created AddressBookContactOutlookImpl; isFavorite=" +
                                isFavourite);
        this.isFavourite = isFavourite;
    }
}
