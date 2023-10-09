/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.utils;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>InviteContactListFilter</tt> is a <tt>SearchFilter</tt> that filters
 * the contact list to fit invite operations.
 *
 * @author Yana Stamcheva
 */
public class InviteContactListFilter
    extends SearchFilter
{
    /**
     * Creates an instance of <tt>InviteContactListFilter</tt>.
     *
     * @param sourceContactList the contact list to filter
     */
    public InviteContactListFilter(TreeContactList sourceContactList)
    {
        super(sourceContactList);
    }

    /**
     * Applies this filter to the default contact source.
     * @param filterQuery the query that tracks this filter.
     */
    public void applyFilter(FilterQuery filterQuery)
    {
        filterQuery.setMaxResultShown(-1);

        Collection<UIContactSource> filterSources
            = mSourceContactList.getContactSources();

        Iterator<UIContactSource> filterSourceIter = filterSources.iterator();

        // If we have stopped filtering in the meantime we return here.
        if (filterQuery.isCanceled())
            return;

        // Then we apply the filter on all its contact sources.
        while (filterSourceIter.hasNext())
        {
            final UIContactSource filterSource
                = filterSourceIter.next();

            // Do not include message history in the invite contact list filter
            if (filterSource.getContactSourceService().getType() == ContactSourceService.MESSAGE_HISTORY_TYPE)
            {
                continue;
            }

            // If we have stopped filtering in the meantime we return here.
            if (filterQuery.isCanceled())
                return;

            ContactQuery query = applyFilter(filterSource);

            if (query.getStatus() == ContactQuery.QUERY_IN_PROGRESS)
                filterQuery.addContactQuery(query);
        }

        // Closes this filter to indicate that we finished adding queries to it.
        if (filterQuery.isRunning())
            filterQuery.close();
        else if (!mSourceContactList.isEmpty() &&
                 !GuiActivator.enterDialsNumber())
            // Select the first contact if 'enter' doesn't dial the searched
            // number, otherwise the first contact would be called when 'enter'
            // is pressed.
            mSourceContactList.selectFirstContact();
    }
}