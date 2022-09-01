// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>FavoritesFilter</tt> is used to get contacts that have been marked
 * as favorites from the contact list
 */
public class FavoritesFilter extends MetaContactListAdapter
    implements  ContactListFilter
{
    private static final Logger sLog = Logger.getLogger(FavoritesFilter.class);
    private static final ContactLogger sContactLog = ContactLogger.getLogger();

    /**
     * The contact list that the filter is applied to.
     */
    private final TreeContactList mContactList;

    /**
     * The meta contact list source.
     */
    private final MetaContactListSource mMclSource;

    /**
     * True if the last time we ran the query there were no favorites.  Defaults
     * to true as, at start of day, we haven't found any contacts.
     */
    private boolean mNoFavorites = true;

    /**
     * Flag indicating whether or not we have registered for meta contact list
     * events.
     */
    private boolean mRegistered = false;

    /**
     * Creates an instance of FavoritesFilter.
     *
     * @param contactList the contact list that the filter is applied to.
     */
    public FavoritesFilter(TreeContactList contactList)
    {
        mContactList = contactList;
        mMclSource = contactList.getMetaContactListSource();
    }

    public boolean isMatching(UIContact uiContact)
    {
        Object descriptor = uiContact.getDescriptor();
        if (descriptor instanceof MetaContact)
            return isMatching((MetaContact)descriptor);

        return false;
    }

    /**
     * Returns true if the passed MetaContact is a favorite or not
     *
     * @param metaContact the contact to examine
     * @return true if it is a favorite
     */
    private boolean isMatching(MetaContact metaContact)
    {
        // Whether a contact is favorite or not is held in the metaContact's
        // details:
        String favoriteDetail =
                   metaContact.getDetail(MetaContact.CONTACT_FAVORITE_PROPERTY);
        boolean isMatching = (Boolean.parseBoolean(favoriteDetail) &&
                             (metaContact.getContactCount() > 0));

        if (isMatching)
        {
            sContactLog.note(metaContact, "Found a favorite");

            if (mNoFavorites)
            {
                sContactLog.note(metaContact,
                             "Have match and previously reported no favorites");

                // Update the no favorites UI as previously we had none, but now we do
                mNoFavorites = false;
                setNoFavouritesUI(false);
            }
        }

        return isMatching;
    }

    public boolean isMatching(UIGroup uiGroup)
    {
        // We don't have favorite groups so just return false
        return false;
    }

    public void applyFilter(FilterQuery filterQuery)
    {
        if (!mRegistered)
        {
            // Not registered - register so that we can redraw if the contacts
            // change. We can't do this in the constructor as the contact list
            // service does not exist at that point.
            GuiActivator.getContactListService()
                        .addMetaContactListListener(this);
            mRegistered = true;
        }

        // Create the query that will track filtering.
        MetaContactQuery query = new MetaContactQuery();

        // Add this query to the filterQuery.
        filterQuery.addContactQuery(query);

        // Close this filter to indicate that we finished adding queries to it.
        filterQuery.close();

        query.addContactQueryListener(mContactList);
        int resultCount =
          addMatching(GuiActivator.getContactListService().getRoot(), query, 0);

        boolean noFavorites = resultCount == 0;
        mNoFavorites = noFavorites;

        sLog.debug("Finished querying for favorites, found " + resultCount);

        setNoFavouritesUI(noFavorites);
    }

    /**
     * Adds all contacts contained in the given <tt>MetaContactGroup</tt>
     * matching the current filter and not contained in the contact list.
     *
     * @param metaGroup the <tt>MetaContactGroup</tt>, which matching contacts
     * to add
     * @param query the <tt>MetaContactQuery</tt> that notifies interested
     * listeners of the results of this matching
     * @param resultCount the number of matching contacts that we have found so
     * far
     * @return the number of contacts that were found
     */
    private int addMatching(MetaContactGroup metaGroup,
                            MetaContactQuery query,
                            int resultCount)
    {
        sLog.debug("Starting to query contacts from metaGroup " + metaGroup);
        Iterator<MetaContact> childContacts = metaGroup.getChildContacts();
        int newContactsCount = 0;

        while (childContacts.hasNext() && !query.isCanceled())
        {
            MetaContact metaContact = childContacts.next();

            if (isMatching(metaContact))
            {
                newContactsCount++;

                // First get or create the UI group that we should add the UI
                // contact to.
                UIGroup uiGroup = null;
                if (!mMclSource.isRootGroup(metaGroup))
                {
                    synchronized (metaGroup)
                    {
                        uiGroup = mMclSource.getUIGroup(metaGroup);

                        if (uiGroup == null)
                            uiGroup = mMclSource.createUIGroup(metaGroup);
                    }
                }

                // Then get or create the UI contact that we should display
                UIContact uiContact;
                synchronized (metaContact)
                {
                    // Only create a UIContact if there isn't already one
                    // for this metaContact (i.e. the contact hasn't
                    // already been added to the GUI).
                    uiContact = mMclSource.getUIContact(metaContact);

                    if (uiContact == null)
                    {
                        uiContact = mMclSource.createUIContact(metaContact);
                    }
                }

                if (!query.isCanceled())
                {
                    // Check to make sure that the query hasn't been cancelled
                    // while we processed the above.
                    mContactList.addContact(uiContact, uiGroup, true, true);
                    query.setInitialResultCount(resultCount + newContactsCount);
                }
            }
        }

        // Update the count
        resultCount = resultCount + newContactsCount;

        // Finally, look through any subgroups:
        Iterator<MetaContactGroup> subgroups = metaGroup.getSubgroups();
        while(subgroups.hasNext() && !query.isCanceled())
        {
            MetaContactGroup subgroup = subgroups.next();

            synchronized(subgroup)
            {
                UIGroup uiGroup = mMclSource.getUIGroup(subgroup);

                if (uiGroup == null)
                {
                    uiGroup = mMclSource.createUIGroup(subgroup);
                }
            }

            resultCount = addMatching(subgroup, query, resultCount);
        }

        return resultCount;
    }

    /**
     * Update the contact list UI according to whether there are any favorites
     * or not
     *
     * Synchronized to prevent issues where one thread is trying to enable the
     * view while another is trying to disable it
     *
     * @param noFavorites true if we have no favorites
     */
    private synchronized void setNoFavouritesUI(boolean noFavorites)
    {
        // Finally, we've finished looking at the contacts.  Activate the "no
        // favorites view" if there are no favorites.  Note that this query may
        // finish after the favorites filter has been removed, thus we need to
        // make sure we only change views if the favorites filter is still
        // active
        AbstractMainFrame mainFrame = GuiActivator.getUIService().getMainFrame();
        boolean favoritesFilterActive =
            mContactList.getCurrentFilter() == mContactList.getFavoritesFilter();

        if (mainFrame != null && favoritesFilterActive)
        {
            sLog.info("Setting no favorites UI " + noFavorites);
            mainFrame.enableUnknownContactView(noFavorites);
        }
    }

    @Override
    public void metaContactModified(MetaContactModifiedEvent evt)
    {
        // Check to see if the favorite status of a contact has changed:
        if (!evt.getModificationName().equals(MetaContact.CONTACT_FAVORITE_PROPERTY))
            return;

        ContactListFilter filter = mContactList.getCurrentFilter();
        if (mContactList.getFavoritesFilter().equals(filter))
        {
            // The contacts list is filtered on favorites - the list of
            // favorites has changed so make it re-draw
            mContactList.applyCurrentFilter();
        }
    }
}
