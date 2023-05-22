/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>PresenceFilter</tt> is used to filter offline contacts from the
 * contact list.
 *
 * @author Yana Stamcheva
 */
public class PresenceFilter
    implements ContactListFilter
{
    /**
     * The <tt>Logger</tt> used by the <tt>PresenceFilter</tt> class and its
     * instances to print debugging information.
     */
    private static final Logger sLog = Logger.getLogger(PresenceFilter.class);

    /**
     * The <tt>ContactLogger</tt> used by the <tt>PresenceFilter</tt> class and its
     * instances to print contact debugging information.
     */
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * The contact list that the filter is applied to.
     */
    private final TreeContactList mContactList;

    /**
     * The meta contact list source.
     */
    private final MetaContactListSource mMclSource;

    /**
     * The initial result count below which we insert all filter results
     * directly to the contact list without firing events.
     */
    private static final int INITIAL_CONTACT_COUNT = 30;

    /**
     * Creates an instance of <tt>PresenceFilter</tt>.
     *
     * @param contactList the contact list that the filter is applied to.
     */
    public PresenceFilter(TreeContactList contactList)
    {
        mContactList = contactList;
        mMclSource = contactList.getMetaContactListSource();
    }

    /**
     * Applies this filter. This filter is applied over the
     * <tt>MetaContactListService</tt>.
     *
     * @param filterQuery the query which keeps track of the filtering results
     */
    public void applyFilter(FilterQuery filterQuery)
    {
        // Create the query that will track filtering.
        MetaContactQuery query = new MetaContactQuery();

        // Add this query to the filterQuery.
        filterQuery.addContactQuery(query);
        // Closes this filter to indicate that we finished adding queries to it.
        filterQuery.close();

        query.addContactQueryListener(mContactList);

        int resultCount = 0;
        addMatching(GuiActivator.getContactListService().getRoot(),
                    query,
                    resultCount);

        if (!query.isCanceled())
            query.fireQueryEvent(MetaContactQueryStatusEvent.QUERY_COMPLETED);
        else
            query.fireQueryEvent(MetaContactQueryStatusEvent.QUERY_CANCELED);
    }

    /**
     * Indicates if the given <tt>uiContact</tt> is matching this filter.
     *
     * @param uiContact the <tt>UIContact</tt> to check
     * @return <tt>true</tt> if the given <tt>uiContact</tt> is matching
     * this filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(UIContact uiContact)
    {
        Object descriptor = uiContact.getDescriptor();
        if (descriptor instanceof MetaContact)
            return isMatching((MetaContact) descriptor);

        return false;
    }

    /**
     * Indicates if the given <tt>uiGroup</tt> is matching this filter.
     *
     * @param uiGroup the <tt>UIGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>uiGroup</tt> is matching
     * this filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(UIGroup uiGroup)
    {
        Object descriptor = uiGroup.getDescriptor();
        return (descriptor instanceof MetaContactGroup);
    }

    /**
     * Returns <tt>true</tt> if the contact is not marked as hidden and either
     * offline contacts are shown or the given <tt>MetaContact</tt> is online,
     * otherwise returns false.
     *
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> if the given <tt>MetaContact</tt> matches this
     * filter
     */
    public boolean isMatching(MetaContact metaContact)
    {
        List<String> hiddenDetails =
                metaContact.getDetails(MetaContact.IS_CONTACT_HIDDEN);

        boolean isHidden = hiddenDetails.size() == 0 ?
                                        false :
                                        Boolean.valueOf(hiddenDetails.get(0));

        return (!isHidden) &&
               (metaContact.getContactCount() > 0);
    }

    /**
     * Adds all contacts contained in the given <tt>MetaContactGroup</tt>
     * matching the current filter and not contained in the contact list.
     *
     * @param metaGroup the <tt>MetaContactGroup</tt>, which matching contacts
     * to add
     * @param query the <tt>MetaContactQuery</tt> that notifies interested
     * listeners of the results of this matching
     * @param resultCount the initial result count we would insert directly to
     * the contact list without firing events
     */
    private void addMatching(MetaContactGroup metaGroup,
                             MetaContactQuery query,
                             int resultCount)
    {
        Iterator<MetaContact> childContacts = metaGroup.getChildContacts();

        while (childContacts.hasNext() && !query.isCanceled())
        {
            MetaContact metaContact = childContacts.next();

            if(isMatching(metaContact))
            {
                try
                {
                    resultCount++;
                    addMatchingMetaContact(metaContact,
                                           metaGroup,
                                           query,
                                           resultCount);
                }
                catch (Exception e)
                {
                    // Something went wrong adding the contact - catch the
                    // exception as it's most likely a bad contact.  Log and
                    // try to deal with the bad contact.  No need to hash
                    // MetaContact, as its toString() method does that.
                    sLog.error("Exception trying to add a matching contact "
                                                              + metaContact, e);

                    // We have seen this where the metaContact doesn't have a
                    // group set.  This happens when a delete fails, in which
                    // case we just remove the contact from the group
                    try
                    {
                        GuiActivator.getContactListService()
                                    .removeMetaContact(metaContact);
                    }
                    catch (Exception f)
                    {
                        // Just give up.  Hope things are improved on restart.
                        // No need to hash MetaContact, as its toString()
                        // method does that.
                        contactLogger.error(metaContact,
                            "Couldn't delete the contact ",
                            f);
                    }
                }
            }
        }

        // If in the meantime the filtering has been stopped we return here.
        if (query.isCanceled())
            return;

        Iterator<MetaContactGroup> subgroups = metaGroup.getSubgroups();
        while(subgroups.hasNext() && !query.isCanceled())
        {
            MetaContactGroup subgroup = subgroups.next();

            UIGroup uiGroup;
            synchronized(subgroup)
            {
                uiGroup = mMclSource.getUIGroup(subgroup);

                if (uiGroup == null)
                {
                    uiGroup = mMclSource.createUIGroup(subgroup);
                }
            }

            mContactList.addGroup(uiGroup, true);

            addMatching(subgroup, query, resultCount);
        }
    }

    /**
     * Add a matching MetaContact to the contact list.
     *
     * @param metaContact The matching contact to add
     * @param metaGroup The group that owns the matching contact
     * @param query The query
     * @param resultCount The number of results we've found so far (including
     *                    this one)
     */
    protected void addMatchingMetaContact(MetaContact metaContact,
                                          MetaContactGroup metaGroup,
                                          MetaContactQuery query,
                                          int resultCount)
    {
        // No need to hash MetaContact as its toString() method does that already.
        contactLogger.debug("Presence filter contact added: " + metaContact);

        if (resultCount <= INITIAL_CONTACT_COUNT)
        {
            UIGroup uiGroup = null;
            if (!mMclSource.isRootGroup(metaGroup))
            {
                synchronized (metaGroup)
                {
                    uiGroup = mMclSource.getUIGroup(metaGroup);

                    if (uiGroup == null)
                    {
                        uiGroup = mMclSource.createUIGroup(metaGroup);
                    }
                }
            }

            UIContact newUIContact;
            synchronized (metaContact)
            {
                // Only create a UIContact if there isn't already one
                // for this metaContact (i.e. the contact hasn't
                // already been added to the GUI).
                newUIContact = mMclSource.getUIContact(metaContact);

                if (newUIContact == null)
                {
                    newUIContact = mMclSource.createUIContact(metaContact);
                }
            }

            if (!query.isCanceled())
            {
                // Add the contact to the contact list but only if the
                // query has not yet been cancelled
                mContactList.addContact(newUIContact, uiGroup, true, true);
                query.setInitialResultCount(resultCount);
            }
        }
        else
        {
            query.fireQueryEvent(metaContact);
        }
    }
}
