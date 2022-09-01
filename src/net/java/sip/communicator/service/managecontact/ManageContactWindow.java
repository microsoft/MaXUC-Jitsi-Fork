// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.managecontact;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Service for an 'Add Contact' window.
 */
public interface ManageContactWindow
{
    /**
     * Sets the visibility of the 'Add Contact' window.
     *
     * @param visible   whether the window should be visible.
     */
    void setVisible(boolean visible);

    /**
     * Selects the given protocol provider in the window.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to select
     */
    void setSelectedAccount(ProtocolProviderService protocolProvider);

    /**
     * Selects the given group in the window.
     *
     * @param group the <tt>MetaContactGroup</tt> to select
     */
    void setSelectedGroup(MetaContactGroup group);

    /**
     * Sets the name of the contact to add.
     *
     * @param name the name of the contact to add
     */
    void setContactName(String name);

    /**
     * Sets the address of the contact to add.
     *
     * @param contactAddress the address of the contact to add
     */
    void setContactAddress(String contactAddress);

    /**
     * Sets details of the contact to add.
     *
     * @param contactDetails the details of the contact to add
     */
    void setContactDetails(ContactDetail... contactDetails);

    /**
     * Sets extra details (i.e. not <tt>ContactDetail</tt>s of the contact to
     * add.
     *
     * @param extraDetails
     */
    void setContactExtraDetails(Map<String, String> extraDetails);

    /**
     * Save the details as they are and then hide this dialog
     */
    void saveAndDismiss();

    /**
     * Set the focus on the window
     */
    void focusOnWindow();
}