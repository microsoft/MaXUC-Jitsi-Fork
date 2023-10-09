/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import static net.java.sip.communicator.util.PrivacyUtils.REDACTED;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.text.Normalizer.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.PersonalContactDetails.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MetaContactListSource</tt> is an abstraction of the
 * <tt>MetaContactListService</tt>, which makes the correspondence between a
 * <tt>MetaContact</tt> and an <tt>UIContact</tt> and between a
 * <tt>MetaContactGroup</tt> and an <tt>UIGroup</tt>. It is also responsible
 * for filtering of the <tt>MetaContactListService</tt> through a given pattern.
 *
 * @author Yana Stamcheva
 */
public class MetaContactListSource
    implements  ContactPresenceStatusListener,
                MetaContactListListener
{
    /**
     * The logger.
     */
    private static final Logger sLog
        = Logger.getLogger(MetaContactListSource.class);

    private static final ContactLogger sContactLogger
    = ContactLogger.getLogger();

    /**
     * If we're running in QA mode, we can log details of what the user types
     * into the search box, otherwise we can't as that may contain PII.
     */
    protected boolean mQAMode;

    /**
     * The initial result count below which we insert all filter results
     * directly to the contact list without firing events.
     */
    private static final int INITIAL_CONTACT_COUNT = 30;

    /**
     * The list of action buttons for this meta contact.
     */
    private Map<ContactAction<Contact>, SIPCommButton> mCustomActionButtons;

    /**
     * Currently selected custom action contact.
     */
    private MetaUIContact mCustomActionContact;

    /**
     * The contact list that is using this as its MetaContactListSource
     */
    private final TreeContactList mContactList;

    /**
     * Map of MetaContacts to UIContactImpls
     */
    private final Map<MetaContact, UIContactImpl> mUiContacts =
            new ConcurrentHashMap<>();

    /**
     * Map of MetaContactGroups to UIGroupImpls
     */
    private final Map<MetaContactGroup, UIGroupImpl> mUiGroups =
            new ConcurrentHashMap<>();

    public MetaContactListSource(TreeContactList contactList)
    {
        mContactList = contactList;
        getQAMode();
    }

    /**
     * Used purely to separate construction from getting config for UTs.
     */
    protected void getQAMode()
    {
        mQAMode = GuiActivator.getConfigurationService().global().getBoolean(
            "net.java.sip.communicator.QA_MODE", false);
    }

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt>, which corresponding UI
     * contact we're looking for
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>MetaContact</tt>
     */
    public UIContactImpl getUIContact(MetaContact metaContact)
    {
        return mUiContacts.get(metaContact);
    }

    /**
     * Returns the <tt>UIGroup</tt> corresponding to the given
     * <tt>MetaContactGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which UI group we're
     * looking for
     * @return the <tt>UIGroupImpl</tt> corresponding to the given
     * <tt>MetaContactGroup</tt>
     */
    public UIGroupImpl getUIGroup(MetaContactGroup metaGroup)
    {
        return mUiGroups.get(metaGroup);
    }

    /**
     * Creates a <tt>UIContact</tt> for the given <tt>metaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt> for which we would like to
     * create an <tt>UIContact</tt>
     * @return an <tt>UIContact</tt> for the given <tt>metaContact</tt>
     */
    public UIContactImpl createUIContact(final MetaContact metaContact)
    {
        sContactLogger.debug("CreateUIContact from " + metaContact);
        MetaUIContact uiContact = new MetaUIContact(metaContact, this);
        mUiContacts.put(metaContact, uiContact);
        return uiContact;
    }

    /**
     * Removes the <tt>UIContact</tt> from the given <tt>metaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt>, which corresponding UI
     * contact we would like to remove
     */
    public void removeUIContact(MetaContact metaContact)
    {
        mUiContacts.remove(metaContact);
    }

    /**
     * Creates a <tt>UIGroupDescriptor</tt> for the given <tt>metaGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt> for which we would like to
     * create an <tt>UIContact</tt>
     * @return a <tt>UIGroup</tt> for the given <tt>metaGroup</tt>
     */
    public UIGroup createUIGroup(MetaContactGroup metaGroup)
    {
        MetaUIGroup uiGroup = new MetaUIGroup(metaGroup, this);
        mUiGroups.put(metaGroup, uiGroup);
        return uiGroup;
    }

    /**
     * Removes the descriptor from the given <tt>metaGroup</tt>.
     * @param metaGroup the <tt>MetaContactGroup</tt>, which descriptor we
     * would like to remove
     */
    public void removeUIGroup(MetaContactGroup metaGroup)
    {
        mUiGroups.remove(metaGroup);
    }

    /**
     * Indicates if the given <tt>MetaContactGroup</tt> is the root group.
     * @param group the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>group</tt> is the root group,
     * <tt>false</tt> - otherwise
     */
    public boolean isRootGroup(MetaContactGroup group)
    {
        return group.equals(GuiActivator.getContactListService().getRoot());
    }

    /**
     * Filters the <tt>MetaContactListService</tt> to match the given
     * <tt>filterPattern</tt> and stores the result in the given
     * <tt>treeModel</tt>.
     * @param filterPattern the pattern to filter through
     * @return the created <tt>MetaContactQuery</tt> corresponding to the
     * query this method does
     */
    public MetaContactQuery queryMetaContactSource(final Pattern filterPattern)
    {
        final MetaContactQuery query = new MetaContactQuery();
        sLog.debug("Querying " + this + " with pattern " + filterPattern + " on query " + query);

        new Thread(
            "MetaContactListSource@" + this.hashCode() + ".queryMetaContactSource")
        {
            public void run()
            {
                int resultCount = 0;
                queryMetaContactSource( filterPattern,
                        GuiActivator.getContactListService().getRoot(),
                        query,
                        resultCount);

                if (!query.isCanceled())
                    query.fireQueryEvent(
                        MetaContactQueryStatusEvent.QUERY_COMPLETED);
                else
                    query.fireQueryEvent(
                        MetaContactQueryStatusEvent.QUERY_CANCELED);
            }
        }.start();

        return query;
    }

    /**
     * Filters the children in the given <tt>MetaContactGroup</tt> to match the
     * given <tt>filterPattern</tt> and stores the result in the given
     * <tt>treeModel</tt>.
     * @param filterPattern the pattern to filter through
     * @param parentGroup the <tt>MetaContactGroup</tt> to filter
     * @param query the object that tracks the query
     * @param resultCount the initial result count we would insert directly to
     * the contact list without firing events
     */
    public void queryMetaContactSource(Pattern filterPattern,
                                       MetaContactGroup parentGroup,
                                       MetaContactQuery query,
                                       int resultCount)
    {
        sLog.debug("Query " + this + " for " + filterPattern + " on " + query);
        Iterator<MetaContact> childContacts = parentGroup.getChildContacts();

        while (childContacts.hasNext() && !query.isCanceled())
        {
            MetaContact metaContact = childContacts.next();

            if (isMatching(filterPattern, metaContact))
            {
                resultCount++;

                try
                {
                    addMatchingMetaContact(parentGroup,
                                           query,
                                           resultCount,
                                           metaContact);
                }
                catch (Exception e)
                {
                    // Something went wrong adding the contact - catch the
                    // exception as it's most likely a bad contact.  Log and
                    // try to deal with the bad contact:
                    sLog.error("Exception trying to add a matching contact "
                                                              + metaContact, e);
                    resultCount--;

                    // We have seen this where the metaContact doesn't have a
                    // group set.  This happens when a delete fails, in which
                    // case we just remove the contact from the group
                    try
                    {
                        GuiActivator.getContactListService()
                                    .removeMetaContact(metaContact);
                    }
                    catch (MetaContactListException mcle)
                    {
                        // Just give up.  Hope things are improved on restart
                        sLog.error("Couldn't delete the contact "
                                                           + metaContact, mcle);
                    }
                }
            }
        }

        // If in the meantime the query is canceled we return here.
        if(query.isCanceled())
            return;

        Iterator<MetaContactGroup> subgroups = parentGroup.getSubgroups();
        while (subgroups.hasNext() && !query.isCanceled())
        {
            MetaContactGroup subgroup = subgroups.next();

            queryMetaContactSource(filterPattern, subgroup, query, resultCount);
        }

        sLog.debug("Query " + this + " for " + filterPattern + " found " + resultCount);
    }

    /**
     * Add a matching MetaContact to the contact list.
     *
     * @param metaContact The matching contact to add
     * @param parentGroup The group that owns the matching contact
     * @param query The query
     * @param resultCount
     */
    private void addMatchingMetaContact(MetaContactGroup parentGroup,
                                        MetaContactQuery query,
                                        int resultCount,
                                        MetaContact metaContact)
    {
        sLog.debug("addMatchingMetaContact " + metaContact);

        if (resultCount <= INITIAL_CONTACT_COUNT)
        {
            UIGroup uiGroup = null;
            if (!isRootGroup(parentGroup))
            {
                synchronized (parentGroup)
                {
                    uiGroup = getUIGroup(parentGroup);

                    if (uiGroup == null)
                        uiGroup = createUIGroup(parentGroup);
                }
            }

            UIContact newUIContact;
            synchronized (metaContact)
            {
                // Only create a UIContact if there isn't already one
                // for this metaContact (i.e. the contact hasn't
                // already been added to the GUI).
                newUIContact = getUIContact(metaContact);

                if (newUIContact == null)
                {
                    newUIContact = createUIContact(metaContact);
                }
            }

            mContactList.addContact(
                newUIContact,
                uiGroup,
                true,
                true);

            query.setInitialResultCount(resultCount);
        }
        else
            query.fireQueryEvent(metaContact);
    }

    /**
     * Checks if the given <tt>metaContact</tt> is matching the given
     * <tt>filterPattern</tt>.
     * A <tt>MetaContact</tt> would be matching the filter if it is not hidden
     * and one of the following is true:<br>
     * - its display name contains the filter string
     * - at least one of its child protocol contacts has a display name or an
     * address that contains the filter string.
     * @param filterPattern the filter pattern to check for matches
     * @param metaContact the <tt>MetaContact</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>metaContact</tt> is
     * matching the current filter and is not hidden, otherwise returns
     * <tt>false</tt>
     */
    public boolean isMatching(Pattern filterPattern, MetaContact metaContact)
    {
        sLog.debug("isMatching");

        // If the filter pattern is null, that means the user hasn't typed
        // anything into the search box so all contacts match, therefore return
        // true.
        if (filterPattern == null)
        {
            sLog.debug("Returning true as no filter pattern has been entered");
            return true;
        }

        // Check whether this MetaContact should be hidden from the contact list
        List<String> hiddenDetails =
            metaContact.getDetails(MetaContact.IS_CONTACT_HIDDEN);

        boolean isHidden = hiddenDetails.size() == 0 ?
                                    false :
                                    Boolean.valueOf(hiddenDetails.get(0));
        if (isHidden)
        {
            sLog.debug("Returning false as is hidden");
            return false;
        }

        // Process the display name so that accents and apostrophes are ignored
        Matcher matcher = filterPattern.matcher(Normalizer.normalize(
                            metaContact.getDisplayName(), Form.NFD)
                            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                            .replaceAll("'", ""));

        if(matcher.find())
        {
            sLog.debug("Returning true as found display name 1");
            return true;
        }

        Iterator<Contact> contacts = metaContact.getContacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            // Process the display name to avoid NPEs and so that accents and
            // apostrophes are ignored.
            String contactDisplayName = contact.getDisplayName();
            String parsedContactDisplayName =
                (contactDisplayName == null) ? "" : contactDisplayName;
            String displayName =
                  Normalizer.normalize(parsedContactDisplayName, Form.NFD)
                            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                            .replaceAll("'", "");

            matcher = filterPattern.matcher(displayName);

            if (matcher.find())
            {
                sLog.debug("Returning true as found display name 2");
                return true;
            }

            // Only add the address as a searchable string if the persistent
            // presence operation set is marked as having a user-visible
            // address.
            Map<String, OperationSet> opSets =
                contact.getProtocolProvider().getSupportedOperationSets();
            String ppOpSetKey = OperationSetPersistentPresence.class.getName();

            if (opSets.containsKey(ppOpSetKey))
            {
                OperationSetPersistentPresence presence =
                    (OperationSetPersistentPresence)opSets.get(ppOpSetKey);

                if (presence.isAddressDisplayable())
                {
                    matcher = filterPattern.matcher(parsedContactDisplayName);

                    if (matcher.find())
                    {
                        sLog.debug("Returning true as found presence address");
                        return true;
                    }
                }
            }

            // Check in the details too, if this contact has any
            String ssdOpSetKey = OperationSetServerStoredContactInfo.class.getName();
            if (opSets.containsKey(ssdOpSetKey))
            {
                OperationSetServerStoredContactInfo info =
                    (OperationSetServerStoredContactInfo)opSets.get(ssdOpSetKey);

                Iterator<GenericDetail> allDetails =
                                          info.getAllDetailsForContact(contact);
                while (allDetails.hasNext())
                {
                    GenericDetail detail = allDetails.next();

                    // We are only interested in some details:
                    if (!(detail instanceof NameDetail ||
                          detail instanceof PhoneNumberDetail ||
                          detail instanceof IMDetail ||
                          detail instanceof EmailAddressDetail))
                        continue;

                    if ((detail instanceof PhoneNumberDetail) &&
                        (!ConfigurationUtils.isCallingEnabled()))
                    {
                        // Detail is phone number but we don't support calling
                        sLog.debug("Skip phone detail");
                        continue;
                    }

                    if (detail.getDetailValue() != null)
                    {
                        String detailValue = detail.toString();

                        // If this is an IM or Email detail, then remove the
                        // domain - we don't want it as it causes confusion.
                        if (detail instanceof IMDetail ||
                            detail instanceof EmailAddressDetail)
                        {
                            int atIndex = detailValue.indexOf('@');

                            if (atIndex != -1)
                            {
                                detailValue = detailValue.substring(0, atIndex);
                            }
                        }

                        matcher = filterPattern.matcher(detailValue);

                        if (matcher.find())
                        {
                            String valueToLog = mQAMode ? detailValue : REDACTED;
                            sLog.debug("Return true as found in detail: " + valueToLog);
                            return true;
                        }
                    }
                }
            }
        }

        sLog.debug("Return default false");
        return false;
    }

    /**
     * Checks if the given <tt>metaGroup</tt> is matching the current filter. A
     * group is matching the current filter only if it contains at least one
     * child <tt>MetaContact</tt>, which is matching the current filter.
     * @param filterPattern the filter pattern to check for matches
     * @param metaGroup the <tt>MetaContactGroup</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>metaGroup</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    public boolean isMatching(Pattern filterPattern, MetaContactGroup metaGroup)
    {
        Iterator<MetaContact> contacts = metaGroup.getChildContacts();

        while (contacts.hasNext())
        {
            MetaContact metaContact = contacts.next();

            if (isMatching(filterPattern, metaContact))
                return true;
        }
        return false;
    }

    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent evt)
    {
        sLog.debug("contactPresenceStatusChanged");

        final Contact sourceContact = evt.getSourceContact();
        final MetaContact metaContact
            = GuiActivator.getContactListService().findMetaContactByContact(
                    sourceContact);

        if (metaContact == null)
            return;

        if (shouldIgnoreContactUpdates())
        {
            sLog.debug("Ignoring presence change for " + sourceContact +
                                                          " in " + metaContact);
            return;
        }

        boolean uiContactCreated = false;

        UIContactImpl uiContact;

        synchronized (metaContact)
        {
            uiContact = getUIContact(metaContact);

            if (uiContact == null)
            {
                uiContact = createUIContact(metaContact);
                uiContactCreated = true;
            }
        }

        ContactListFilter currentFilter = mContactList.getCurrentFilter();

        boolean matches = false;

        if (currentFilter != null)
        {
            if (currentFilter instanceof ContactListSearchFilter)
            {
                // If this is the search filter, then get the pattern and use
                // our own matches method
                ContactListSearchFilter searchFilter =
                                     (ContactListSearchFilter)currentFilter;
                Pattern pattern = searchFilter.getMetaContactPattern();
                matches = isMatching(pattern, metaContact);
            }
            else
            {
                // Otherwise, just ask the filter if this matches.
                matches = currentFilter.isMatching(uiContact);
            }
        }

        if (uiContactCreated)
        {
            if (matches)
            {
                MetaContactGroup parentGroup
                    = metaContact.getParentMetaContactGroup();

                UIGroup uiGroup = null;
                if (!isRootGroup(parentGroup))
                {
                    synchronized (parentGroup)
                    {
                        uiGroup = getUIGroup(parentGroup);

                        if (uiGroup == null)
                            uiGroup = createUIGroup(parentGroup);
                    }
                }

                sLog.debug("Add matching contact due to status change: " +
                           uiContact);

                mContactList
                    .addContact(uiContact, uiGroup, true, true);
            }
            else
                removeUIContact(metaContact);
        }
        else
        {
            if (!matches)
            {
                sLog.debug("Remove unmatching contact due to status change: " +
                           uiContact);
                mContactList.removeContact(uiContact);
            }
            else
                mContactList
                    .nodeChanged(uiContact.getContactNode());
        }

        if (mContactList.getCallHistoryFilter().equals(currentFilter) ||
            mContactList.getMessageHistoryFilter().equals(currentFilter) ||
            mContactList.getAllHistoryFilter().equals(currentFilter) ||
            mContactList.getSearchFilter().equals(currentFilter))
        {
            // Search or history are showing.  Therefore repaint the contact
            // list as the status of some of the items may have changed
            mContactList.repaint();
        }
    }

    /**
     * Reorders contact list nodes, when <tt>MetaContact</tt>-s in a
     * <tt>MetaContactGroup</tt> has been reordered.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {
        MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();
        UIGroupImpl uiGroup;

        ContactListTreeModel treeModel = mContactList.getTreeModel();

        synchronized (metaGroup)
        {
            if (isRootGroup(metaGroup))
                uiGroup = treeModel.getRoot().getGroupDescriptor();
            else
                uiGroup = getUIGroup(metaGroup);
        }

        if (uiGroup != null)
        {
            GroupNode groupNode = uiGroup.getGroupNode();

            if (groupNode != null)
                groupNode.sort(treeModel);
        }
    }

    /**
     * Adds a node in the contact list, when a <tt>MetaContact</tt> has been
     * added in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAdded(final MetaContactEvent evt)
    {
        metaContactAdded(evt.getSourceMetaContact(),
                        evt.getParentGroup());
    }

    /**
     * Adds a node in the contact list, when a <tt>MetaContact</tt> has been
     * added in the <tt>MetaContactListService</tt>.
     * @param metaContact to add to the contact list.
     * @param parentGroup the group we add in.
     */
    private void metaContactAdded(final MetaContact metaContact,
                                 final MetaContactGroup parentGroup)
    {
        sLog.debug("metaContactAdded");

        if (shouldIgnoreContactUpdates())
        {
            sContactLogger.debug("Ignoring MetaContact added " + metaContact);
            return;
        }

        UIContact uiContact;

        synchronized (metaContact)
        {
            uiContact = getUIContact(metaContact);

            // If there's already an UIContact for this meta contact, we have
            // nothing to do here.
            if (uiContact != null)
                return;

            uiContact = createUIContact(metaContact);
        }

        TreeContactList contactList = mContactList;
        ContactListFilter currentFilter = contactList == null ?
                                          null : contactList.getCurrentFilter();

        if (currentFilter != null && currentFilter.isMatching(uiContact))
        {
            UIGroup uiGroup = null;
            if (!isRootGroup(parentGroup))
            {
                synchronized (parentGroup)
                {
                    uiGroup = getUIGroup(parentGroup);

                    if (uiGroup == null)
                        uiGroup = createUIGroup(parentGroup);
                }
            }

            contactList
                .addContact(uiContact, uiGroup, true, true);
        }
        else
            removeUIContact(metaContact);
    }

    /**
     * Adds a group node in the contact list, when a <tt>MetaContactGroup</tt>
     * has been added in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {
        final MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        UIGroup uiGroup;

        synchronized (metaGroup)
        {
            uiGroup = getUIGroup(metaGroup);

            // If there's already an UIGroup for this meta contact, we have
            // nothing to do here.
            if (uiGroup != null)
                return;

            uiGroup = createUIGroup(metaGroup);
        }

        ContactListFilter currentFilter
            = mContactList.getCurrentFilter();

        if (currentFilter.isMatching(uiGroup))
            mContactList.addGroup(uiGroup, true);
        else
            removeUIGroup(metaGroup);

        // iterate over the contacts, they may need to be displayed
        // some protocols fire events for adding contacts after firing
        // that group has been created and this is not needed, but some don't
        Iterator<MetaContact> iterContacts = metaGroup.getChildContacts();
        while(iterContacts.hasNext())
        {
            metaContactAdded(iterContacts.next(), metaGroup);
        }
    }

    /**
     * Notifies the tree model, when a <tt>MetaContactGroup</tt> has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        final MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        UIGroupImpl uiGroup;
        synchronized (metaGroup)
        {
            uiGroup = getUIGroup(metaGroup);
        }

        if (uiGroup != null)
        {
            GroupNode groupNode = uiGroup.getGroupNode();

            if (groupNode != null)
                mContactList.getTreeModel()
                    .nodeChanged(groupNode);
        }
    }

    /**
     * Removes the corresponding group node in the contact list, when a
     * <tt>MetaContactGroup</tt> has been removed from the
     * <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactGroupEvent</tt> that notified us
     */
    public void metaContactGroupRemoved(final MetaContactGroupEvent evt)
    {
        MetaContactGroup metaGroup = evt.getSourceMetaContactGroup();

        UIGroup uiGroup;
        synchronized (metaGroup)
        {
            uiGroup = getUIGroup(metaGroup);
        }

        if (uiGroup != null)
            mContactList.removeGroup(uiGroup);
    }

    /**
     * Notifies the tree model, when a <tt>MetaContact</tt> has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactModified(final MetaContactModifiedEvent evt)
    {
        MetaContact metaContact = evt.getSourceMetaContact();

        UIContactImpl uiContact;
        synchronized (metaContact)
        {
            uiContact = getUIContact(metaContact);
        }

        if (uiContact != null)
        {
            ContactNode contactNode
                = uiContact.getContactNode();

            if (contactNode != null)
                mContactList.nodeChanged(contactNode);
        }
    }

    /**
     * Performs needed operations, when a <tt>MetaContact</tt> has been
     * moved in the <tt>MetaContactListService</tt> from one group to another.
     * @param evt the <tt>MetaContactMovedEvent</tt> that notified us
     */
    public void metaContactMoved(final MetaContactMovedEvent evt)
    {
        sLog.debug("metaContactMoved");

        // fixes an issue with moving meta contacts where removeContact
        // will set data to null in swing thread and it will be after we have
        // set the data here, so we also move this set to the swing thread
        // to order the calls of setData.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    metaContactMoved(evt);
                }
            });
            return;
        }

        final MetaContact metaContact = evt.getSourceMetaContact();
        final MetaContactGroup oldParent = evt.getOldParent();
        final MetaContactGroup newParent = evt.getNewParent();

        synchronized (metaContact)
        {
            UIContact uiContact = getUIContact(metaContact);

            if (uiContact == null)
                return;

            UIGroup oldUIGroup;

            if (isRootGroup(oldParent))
            {
                oldUIGroup =
                    mContactList.getTreeModel().getRoot().getGroupDescriptor();
            }
            else
            {
                synchronized (oldParent)
                {
                    oldUIGroup = getUIGroup(oldParent);
                }
            }

            if (oldUIGroup != null)
            {
                mContactList.removeContact(uiContact);
            }

            // Add the contact to the new place.
            uiContact = createUIContact(metaContact);

            UIGroup newUIGroup = null;
            if (!isRootGroup(newParent))
            {
                synchronized (newParent)
                {
                    newUIGroup = getUIGroup(newParent);

                    if (newUIGroup == null)
                    {
                        newUIGroup = createUIGroup(newParent);
                    }
                }
            }

            ContactListFilter currentFilter = mContactList.getCurrentFilter();

            if (currentFilter.isMatching(uiContact))
            {
                mContactList.addContact(uiContact, newUIGroup, true, true);
            }
            else
            {
                removeUIContact(metaContact);
            }
        }
    }

    /**
     * Removes the corresponding contact node in the contact list, when a
     * <tt>MetaContact</tt> has been removed from the
     * <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactRemoved(final MetaContactEvent evt)
    {
        MetaContact metaContact = evt.getSourceMetaContact();

        UIContact uiContact;
        synchronized (metaContact)
        {
            uiContact
                = getUIContact(metaContact);
        }

        if (uiContact != null)
            mContactList.removeContact(uiContact);
    }

    /**
     * Refreshes the corresponding node, when a <tt>MetaContact</tt> has been
     * renamed in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactRenamedEvent</tt> that notified us
     */
    public void metaContactRenamed(final MetaContactRenamedEvent evt)
    {
        MetaContact metaContact = evt.getSourceMetaContact();

        UIContactImpl uiContact;
        synchronized (metaContact)
        {
            uiContact = getUIContact(metaContact);
        }

        if (uiContact != null)
        {
            ContactNode contactNode = uiContact.getContactNode();

            if (contactNode != null)
                mContactList.nodeChanged(contactNode);
        }
    }

    /**
     * Notifies the tree model, when the <tt>MetaContact</tt> avatar has been
     * modified in the <tt>MetaContactListService</tt>.
     * @param evt the <tt>MetaContactEvent</tt> that notified us
     */
    public void metaContactAvatarUpdated(final MetaContactAvatarUpdateEvent evt)
    {
        MetaContact metaContact = evt.getSourceMetaContact();

        UIContactImpl uiContact;
        synchronized (metaContact)
        {
            uiContact = getUIContact(metaContact);
        }

        if (uiContact != null)
        {
            ContactNode contactNode = uiContact.getContactNode();

            if (contactNode != null)
            {
                mContactList.nodeChanged(contactNode);
            }
        }
    }

    /**
     * Adds a contact node corresponding to the parent <tt>MetaContact</tt> if
     * this last is matching the current filter and wasn't previously contained
     * in the contact list.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        sLog.debug("protoContactAdded");

        final MetaContact metaContact = evt.getNewParent();

        if (shouldIgnoreContactUpdates())
        {
            sLog.debug("Ignoring ProtoContact added to " + metaContact);
            return;
        }

        UIContact parentUIContact;
        boolean parentUIContactCreated = false;
        synchronized (metaContact)
        {
            parentUIContact = getUIContact(metaContact);

            if (parentUIContact == null)
            {
                parentUIContactCreated = true;
                parentUIContact = createUIContact(metaContact);
            }
        }

        if (parentUIContact != null && parentUIContactCreated)
        {
            ContactListFilter currentFilter = mContactList.getCurrentFilter();

            if (currentFilter.isMatching(parentUIContact))
            {
                MetaContactGroup parentGroup =
                    metaContact.getParentMetaContactGroup();

                UIGroup uiGroup = null;
                if (!isRootGroup(parentGroup))
                {
                    synchronized(parentGroup)
                    {
                        uiGroup = getUIGroup(parentGroup);

                        if (uiGroup == null)
                        {
                            uiGroup = createUIGroup(parentGroup);
                        }
                    }
                }

                mContactList.addContact(parentUIContact, uiGroup, true, true);
            }
            else
            {
                removeUIContact(metaContact);
            }
        }
    }

    /**
     * Notifies the UI representation of the parent <tt>MetaContact</tt> that
     * this contact has been modified.
     *
     * @param evt the <tt>ProtoContactEvent</tt> that notified us
     */
    public void protoContactModified(ProtoContactEvent evt)
    {
        MetaContact metaContact = evt.getNewParent();

        UIContactImpl uiContact;
        synchronized (metaContact)
        {
            uiContact = getUIContact(metaContact);
        }

        if (uiContact != null)
        {
            ContactNode contactNode = uiContact.getContactNode();

            if (contactNode != null)
            {
                mContactList.nodeChanged(contactNode);
            }
        }
    }

    /**
     * Adds the new <tt>MetaContact</tt> parent and removes the old one if the
     * first is matching the current filter and the last is no longer matching
     * it.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        sLog.debug("protoContactMoved");

        final MetaContact oldParent = evt.getOldParent();
        final MetaContact newParent = evt.getNewParent();

        if (shouldIgnoreContactUpdates())
        {
            sLog.debug(
                "Ignoring ProtoContact move from " + oldParent + " to " + newParent);
            return;
        }

        UIContact oldUIContact = null;

        if (oldParent != null)
        {
            synchronized (oldParent)
            {
                oldUIContact = getUIContact(oldParent);
            }
        }

        // Remove old parent if not matching.
        if (oldUIContact != null
            && !mContactList.getCurrentFilter().isMatching(oldUIContact))
        {
            mContactList.removeContact(oldUIContact);
        }

        UIContact newUIContact;
        boolean newUIContactCreated = false;
        synchronized (newParent)
        {
            // Add new parent if matching.
            newUIContact = getUIContact(newParent);

            if (newUIContact == null)
            {
                newUIContactCreated = true;
                newUIContact = createUIContact(newParent);
            }
        }

        // if the contact is not created already created, we are just merging
        // don't do anything
        if (newUIContact != null && newUIContactCreated)
        {
            if (mContactList.getCurrentFilter().isMatching(newUIContact))
            {
                MetaContactGroup parentGroup =
                    newParent.getParentMetaContactGroup();

                UIGroup uiGroup = null;
                if (!isRootGroup(parentGroup))
                {
                    synchronized (parentGroup)
                    {
                        uiGroup = getUIGroup(parentGroup);

                        if (uiGroup == null)
                        {
                            uiGroup = createUIGroup(parentGroup);
                        }
                    }
                }

                mContactList.addContact(newUIContact, uiGroup, true, true);
            }
            else
            {
                removeUIContact(newParent);
            }
        }
    }

    /**
     * Removes the contact node corresponding to the parent
     * <tt>MetaContact</tt> if the last is no longer matching the current filter
     * and wasn't previously contained in the contact list.
     * @param evt the <tt>ProtoContactEvent</tt> that notified us
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        final MetaContact oldParent = evt.getOldParent();

        UIContactImpl oldUIContact;
        synchronized (oldParent)
        {
            oldUIContact = getUIContact(oldParent);
        }

        if (oldUIContact != null)
        {
            ContactNode contactNode = oldUIContact.getContactNode();

            if (contactNode != null)
            {
                mContactList.nodeChanged(contactNode);
            }
        }
    }

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    public Collection<SIPCommButton> getContactCustomActionButtons(
            final MetaUIContact metaContact)
    {
        mCustomActionContact = metaContact;

        if (mCustomActionButtons == null)
            initCustomActionButtons();

        Iterator<ContactAction<Contact>> customActionsIter
            = mCustomActionButtons.keySet().iterator();

        Collection<SIPCommButton> availableCustomActionButtons
            = new LinkedList<>();

        while (customActionsIter.hasNext())
        {
            ContactAction<Contact> contactAction = customActionsIter.next();
            SIPCommButton actionButton = mCustomActionButtons.get(contactAction);

            if (isContactActionVisible(contactAction,
                (MetaContact) metaContact.getDescriptor()))
            {
                availableCustomActionButtons.add(actionButton);
            }
        }

        return availableCustomActionButtons;
    }

    /**
     * Indicates if the given <tt>ContactAction</tt> should be visible for the
     * given <tt>MetaContact</tt>.
     *
     * @param contactAction the <tt>ContactAction</tt> to verify
     * @param metaContact the <tt>MetaContact</tt> for which we verify if the
     * given action should be visible
     * @return <tt>true</tt> if the given <tt>ContactAction</tt> is visible for
     * the given <tt>MetaContact</tt>, <tt>false</tt> - otherwise
     */
    private boolean isContactActionVisible(ContactAction<Contact> contactAction,
                                           MetaContact metaContact)
    {
        Iterator<Contact> contactDetails = metaContact.getContacts();

        while (contactDetails.hasNext())
        {
            if (contactAction.isVisible(contactDetails.next()))
                return true;
        }

        return false;
    }

    /**
     * Initializes custom action buttons for this contact source.
     */
    private void initCustomActionButtons()
    {
        mCustomActionButtons
            = new LinkedHashMap<>();

        for (CustomContactActionsService<Contact> ccas
                : getContactActionsServices())
        {
            Iterator<ContactAction<Contact>> actionIterator
                = ccas.getCustomContactActions();

            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<Contact> ca = actionIterator.next();

                SIPCommButton actionButton = mCustomActionButtons.get(ca);

                if (actionButton == null)
                {
                    actionButton = new SIPCommButton();

                    actionButton.setToolTipText(ca.getToolTipText());

                    actionButton.setIconImage(ca.getIcon());
                    actionButton.setRolloverIcon(ca.getRolloverIcon());
                    actionButton.setPressedIcon(ca.getPressedIcon());

                    actionButton.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            List<UIContactDetail> contactDetails
                                = mCustomActionContact.getContactDetails();

                            UIContactDetailCustomAction contactAction
                                = new UIContactDetailCustomAction(ca);

                            if (contactDetails.size() > 1)
                            {
                                ChooseUIContactDetailPopupMenu
                                detailsPopupMenu
                                    = new ChooseUIContactDetailPopupMenu(
                                        (JButton) e.getSource(),
                                        mCustomActionContact.getContactDetails(),
                                        contactAction);

                                detailsPopupMenu.showPopupMenu();
                            }
                            else if (contactDetails.size() == 1)
                            {
                                JButton button = (JButton) e.getSource();
                                Point location = new Point(button.getX(),
                                    button.getY() + button.getHeight());

                                SwingUtilities.convertPointToScreen(
                                    location, mContactList);

                                location.y = location.y
                                    + mContactList.getPathBounds(
                                            mContactList.getSelectionPath()).y;

                                contactAction.actionPerformed(
                                    contactDetails.get(0),
                                    location.x,
                                    location.y);
                            }
                        }
                    });

                    mCustomActionButtons.put(ca, actionButton);
                }
            }
        }
    }

    /**
     * @return true if we should not apply contact updates that we are informed
     * about (e.g. presence updates) to contacts that we are displaying in the
     * contact list, false otherwise.
     */
    private boolean shouldIgnoreContactUpdates()
    {
        // The InviteContactListFilter is used by the ContactInviteDialog which
        // is used for the 'Call transfer', 'Add participants to call' and
        // 'Linking contacts to existing IM contact' dialogs.  In these dialogs
        // it is valid for more than one MetaUIContact to be displayed for the
        // same MetaContact, as it filters multiple contact sources that might
        // match (contacts, call history etc.).  Therefore, these
        // MetaUIContacts cannot be saved in the mUiContacts map in this class.
        //  This means we have to ignore contact changes that we are informed
        // about (e.g. presence updates or MetaContact changes) if the
        // InviteContactListFilter is active, as we don't have access to the UI
        // contacts that it is displaying.  If we don't do this, we'll end up
        // adding a probably duplicate, definitely broken, MetaUIContact to the
        // top of the contact list for each contact change.  This is
        // non-trivial to fix properly so we have to apply this workaround
        // until this is done (tracked in SFR 489432).
        return mContactList.getCurrentFilter() instanceof InviteContactListFilter;
    }

    /**
     * An implementation of <tt>UIContactDetail</tt> for a custom action.
     */
    private class UIContactDetailCustomAction
        implements UIContactDetailAction
    {
        /**
         * The contact action.
         */
        private final ContactAction<Contact> mContactAction;

        /**
         * Creates an instance of <tt>UIContactDetailCustomAction</tt>.
         *
         * @param contactAction the contact action this detail is about
         */
        public UIContactDetailCustomAction(ContactAction<Contact> contactAction)
        {
            mContactAction = contactAction;
        }

        /**
         * Performs the contact action on button click.
         */
        public void actionPerformed(UIContactDetail contactDetail,
                                    int x,
                                    int y)
        {
            mContactAction.actionPerformed(
                (Contact) contactDetail.getDescriptor(), x, y);
        }
    }

    /**
     * Returns a list of all custom contact action services.
     *
     * @return a list of all custom contact action services.
     */
    @SuppressWarnings ("unchecked")
    private List<CustomContactActionsService<Contact>>
        getContactActionsServices()
    {
        List<CustomContactActionsService<Contact>> contactActionsServices
            = new ArrayList<>();

        ServiceReference<?>[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                    CustomContactActionsService.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {
            sLog.error("GuiActivator : " + e);
        }

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                CustomContactActionsService<?> customActionService
                    = (CustomContactActionsService<?>)
                            GuiActivator.bundleContext.getService(serRef);

                if (customActionService.getContactSourceClass()
                        .equals(Contact.class))
                {
                    contactActionsServices.add(
                        (CustomContactActionsService<Contact>)
                            customActionService);
                }
            }
        }
        return contactActionsServices;
    }
}
