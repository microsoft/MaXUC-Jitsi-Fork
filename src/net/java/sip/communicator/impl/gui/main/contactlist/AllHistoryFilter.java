/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>CallAndMessageHistoryFilter</tt> is a filter over the call and
 * message history contact sources.
 */
public class AllHistoryFilter extends AbstractHistoryFilter
{
    /**
     * Creates an instance of AllHistoryFilter
     *
     * @param contactList the contact list that the filter is applied to.
     */
    public AllHistoryFilter(TreeContactList contactList)
    {
        super(contactList);
    }

    @Override
    public void applyFilter(FilterQuery filterQuery)
    {
        mHistoryTypes.add(ContactSourceService.CALL_HISTORY_TYPE);
        mHistoryTypes.add(ContactSourceService.MESSAGE_HISTORY_TYPE);
        super.applyFilter(filterQuery);
    }
}
