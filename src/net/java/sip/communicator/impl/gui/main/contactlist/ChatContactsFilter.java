// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The <tt>ChatContactsFilter</tt> is used to filter chat contacts only
 */
public class ChatContactsFilter
    implements ContactListFilter
{
    /**
     * The <tt>Logger</tt> used by the <tt>ChatContactsFilter</tt> class and its
     * instances to print debugging information.
     */
    private static final Logger logger = Logger.getLogger(ChatContactsFilter.class);

    /**
     * The <tt>ContactLogger</tt> used by the <tt>PresenceFilter</tt> class and its
     * instances to print contact debugging information.
     */
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * The contact list to apply this filter to
     */
    private final TreeContactList contactList;

    /**
     * The meta contact list source.
     */
    private final MetaContactListSource mclSource;

    /**
     * The select multi IM contacts dialog that created us
     */
    private AbstractSelectMultiIMContactsDialog selectMultiIMContactsDialog;

    /**
     * If true, offline contacts should be enabled.
     */
    private final boolean enableOfflineContacts;

    /**
     * Create a new chat contacts filter and apply it to the given contact list
     *
     * @param contactList the contact list to apply this filter to
     * @param selectMultiIMContactsDialog the dialog that created us
     * @param enableOfflineContacts if true, offline contacts should be enabled
     */
    public ChatContactsFilter(TreeContactList contactList,
                              AbstractSelectMultiIMContactsDialog selectMultiIMContactsDialog,
                              boolean enableOfflineContacts)
    {
        this.contactList = contactList;
        this.mclSource = contactList.getMetaContactListSource();
        this.selectMultiIMContactsDialog = selectMultiIMContactsDialog;
        this.enableOfflineContacts = enableOfflineContacts;
    }

    @Override
    public void applyFilter(FilterQuery filterQuery)
    {
        // Create the query that will track filtering.
        MetaContactQuery query = new MetaContactQuery();

        // Add this query to the filterQuery.
        filterQuery.addContactQuery(query);
        // Closes this filter to indicate that we finished adding queries to it.
        filterQuery.close();

        query.addContactQueryListener(contactList);

        int resultCount = 0;
        addMatching(GuiActivator.getContactListService().getRoot(),
                    query,
                    resultCount);

        if (!query.isCanceled())
            query.fireQueryEvent(MetaContactQueryStatusEvent.QUERY_COMPLETED);
        else
            query.fireQueryEvent(MetaContactQueryStatusEvent.QUERY_CANCELED);
    }

    @Override
    public boolean isMatching(UIContact uiContact)
    {
        Object descriptor = uiContact.getDescriptor();
        if (descriptor instanceof MetaContact)
            return isMatching((MetaContact) descriptor);

        return false;
    }

    @Override
    public boolean isMatching(UIGroup uiGroup)
    {
        // Do not show groups
        return false;
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
        Contact imContact = metaContact.getIMContact();

        if (selectMultiIMContactsDialog.onlyDisplayRequiredContacts() &&
            (imContact == null ||
            !selectMultiIMContactsDialog.getRequiredIMContacts().contains(imContact)))
        {
            // If we should only display required contacts and this isn't one
            // of them, just return false.
            return false;
        }

        boolean isMatching;

        List<String> hiddenDetails =
                metaContact.getDetails(MetaContact.IS_CONTACT_HIDDEN);
        boolean isHidden = hiddenDetails.size() == 0 ?
                                        false :
                                        Boolean.valueOf(hiddenDetails.get(0));
        if (isHidden)
        {
            // All hidden contacts are not matching
            isMatching = false;
        }
        else if (imContact != null)
        {
            // We've got an IM contact - so it matches
            isMatching = true;
        }
        else if (selectMultiIMContactsDialog.includeGroupContacts())
        {
            // This filter should match group contacts
            isMatching = (metaContact.getGroupContact() != null);
        }
        else
        {
            // Not a match.
            isMatching =  false;
        }

        return isMatching;
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
                    // try to deal with the bad contact:
                    logger.error("Exception trying to add a matching contact "
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
                        // Just give up.  Hope things are improved on restart
                        contactLogger.error(metaContact,
                            "Couldn't delete the contact ",
                            f);
                    }
                }
            }
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
        logger.debug("Chat contact filter contact added: " + metaContact);

        UIGroup uiGroup = null;
        if (!mclSource.isRootGroup(metaGroup))
        {
            synchronized (metaGroup)
            {
                uiGroup = mclSource.getUIGroup(metaGroup);

                if (uiGroup == null)
                    uiGroup = mclSource.createUIGroup(metaGroup);
            }
        }

        // Create a new UI contact for this MetaContact.
        UIContactImpl newUIContact = mclSource.createUIContact(metaContact);

        newUIContact.setEnabled(isContactEnabled(metaContact));

        if (!query.isCanceled())
        {
            // Add the contact to the contact list but only if the
            // query has not yet been cancelled
            contactList.addContact(
                    newUIContact,
                    uiGroup,
                    true,
                    true);

            // Determine whether we need to set this contact as selected
            ArrayList<MetaContact> selectedContacts =
                selectMultiIMContactsDialog.getSelectedContacts();

            if (selectedContacts.contains(metaContact))
            {
                contactList.setSelectedContact(newUIContact, true);
                newUIContact.setSelected(true);
            }

            query.setInitialResultCount(resultCount);
        }
    }

    /**
     * Return true if this contact is, or should be enabled.
     *
     * @param metaContact The contact to check
     * @return True if it should be enabled.
     */
    private boolean isContactEnabled(MetaContact metaContact)
    {
        // This MetaContact either has an IM contact, or it is a group contact
        // (see isMatching which enforces this).
        // If it is a group contact then it should be enabled if either offline
        // group invitations are supported, or if the current IM account is
        // online.
        // If it is an IM contact, then disable it if it is offline and offline
        // group invitations are not allowed.
        // Finally, if the contact is preselected, and the dialog does not allow
        // de-selection then it should be disabled.
        ArrayList<MetaContact> preselectedContacts =
                           selectMultiIMContactsDialog.getPreselectedContacts();

            // Should be enabled if we allow deselection, or it's not selected
        boolean enabled = selectMultiIMContactsDialog.allowDeselection() ||
                          !preselectedContacts.contains(metaContact);

        if (enabled)
        {
            if (metaContact.getGroupContact() != null)
            {
                // Should be enabled if selecting an offline contact is
                // supported, or if the IM account is online.
                enabled = enableOfflineContacts || AccountUtils.isImProviderRegistered();
            }
            else if (metaContact.getIMContact() != null)
            {
                // Should be enabled if selecting an offline contact is
                // supported, or if the IM contact is online.
                enabled = enableOfflineContacts ||
                          metaContact.getIMContact().getPresenceStatus().isOnline();
            }
            else
            {
                // Shouldn't happen - should never have a matching contact which
                // isn't a group contact or an IM contact
                logger.error("Matched a contact we shouldn't " + metaContact);
            }
        }

        logger.interval("ChatContactsFilter.isContactEnabled",
                        "Is contact enabled? ",
                        "Contact:" + metaContact,
                        "Enabled:" + enabled);

        return enabled;
    }
}
