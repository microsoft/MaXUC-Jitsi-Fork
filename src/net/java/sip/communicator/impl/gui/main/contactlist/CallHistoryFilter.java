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
 * The <tt>CallHistoryFilter</tt> is a filter over the history contact sources.
 *
 * @author Yana Stamcheva
 */
public class CallHistoryFilter extends AbstractHistoryFilter
{
    /**
     * Creates an instance of CallHistoryFilter
     *
     * @param contactList the contact list that the filter is applied to.
     */
    public CallHistoryFilter(TreeContactList contactList)
    {
        super(contactList);
    }

    /**
     * Applies this filter and stores the result in the given <tt>treeModel</tt>.
     *
     * @param filterQuery the <tt>FilterQuery</tt> that tracks the results of
     * this filtering
     */
    @Override
    public void applyFilter(FilterQuery filterQuery)
    {
        mHistoryTypes.add(ContactSourceService.CALL_HISTORY_TYPE);
        mFilterQuery = filterQuery;
        super.applyFilter(filterQuery);
    }

    @Override
    public boolean isMatching(UIContact uiContact)
    {
        return false;
    }
}
