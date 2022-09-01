// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.managecontact;

import java.awt.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.SourceContact;

/**
 * Service for creating a 'Manage Contact' window.
 */
public interface ManageContactService
{
    /**
     * Create an <tt>AddContactWindow</tt>.
     *
     * @param parentWindow The parent of this window.
     * @param contact The source contact containing details that the add window
     * will be populated with , or null if this window is for a new contact.
     * @return    A newly created 'Add Contact' window.
     */
    ManageContactWindow createAddContactWindow(Frame parentWindow,
                                               SourceContact contact);

    /**
     * Create an <tt>EditContactWindow</tt>.
     *
     * @param parentWindow    The parent of this window.
     * @param metaContact    The metacontact thats the contact to
     * have its data displayed and changed.
     * @return    A newly created 'Edit Contact' window.
     */
    ManageContactWindow createEditContactWindow(Frame parentWindow,
                                                MetaContact metaContact);

    /**
     * @return   Whether the AddContactService implementation supports adding
     * a contact with multiple fields e.g. phone, email, postal address.
     */
    boolean supportsMultiFieldContacts();
}
