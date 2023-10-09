// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.main.contactlist.ChatContactsFilter;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListSelectionListener;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactNode;
import net.java.sip.communicator.impl.gui.main.contactlist.SearchFilter;
import net.java.sip.communicator.impl.gui.main.contactlist.TreeContactList;
import net.java.sip.communicator.impl.gui.main.contactlist.UIContactImpl;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.MetaUIContact;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.ProtocolContactSourceServiceImpl;
import net.java.sip.communicator.impl.gui.utils.AbstractContactInviteDialog;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.ScaledDimension;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.SearchField;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactsource.ContactQuery;
import net.java.sip.communicator.service.contactsource.ContactReceivedEvent;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.gui.FilterQuery;
import net.java.sip.communicator.service.gui.UIContact;
import net.java.sip.communicator.service.gui.UIContactSource;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetGroupContacts;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusListener;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.util.account.AccountUtils;

/**
 * A dialog that shows all IM contacts. The user may select multiple contacts.
 * This class does nothing with the selected contacts, so must be extended.
 */
public abstract class AbstractSelectMultiIMContactsDialog
    extends AbstractContactInviteDialog
    implements ServiceListener,
               RegistrationStateChangeListener,
               ContactPresenceStatusListener
{
    private static final long serialVersionUID = 1L;

    private static final Logger sLog =
        Logger.getLogger(AbstractSelectMultiIMContactsDialog.class);

    /**
     * A set holding the currently selected MetaContacts
     */
    protected final HashSet<MetaContact> mSelectedContacts =
            new HashSet<>();

    /**
     * A set holding any preselected MetaContacts
     */
    private final HashSet<MetaContact> mPreselectedContacts =
            new HashSet<>();

    /**
     * A set holding the IM Contacts who are required in this dialog.  If the
     * 'onlyDisplayRequiredContacts' method returns true, contacts that are not
     * in this set will not be displayed in the dialog.
     */
    private final Set<Contact> requiredIMContacts = new HashSet<>();

    /**
     * The resource management service
     */
    protected static final ResourceManagementService sResources =
                                                    GuiActivator.getResources();

    /**
     * Filter for contacts to invite.
     */
    private InviteChatContactListFilter mInviteFilter;

    /**
     * The IM Protocol Provider Service
     */
    protected ProtocolProviderService mImProvider;

    /**
     * The presence operation set associated with the IM provider.
     */
    private OperationSetPresence mPresenceOpSet;

    /**
     * Create the dialog.
     *
     * @param title the title of the dialog
     * @param okButtonText the text for the OK button
     * @param contacts a list of contacts to preselect
     * @param canSelectOffline if true, the user can select offline contacts
     */
    public AbstractSelectMultiIMContactsDialog(String title,
                                               String okButtonText,
                                               Set<MetaContact> contacts,
                                               boolean canSelectOffline)
    {
        super(null, title, okButtonText, canSelectOffline);

        sLog.debug("Showing select multi IM contacts dialog");

        if (contacts != null)
        {
            mPreselectedContacts.addAll(contacts);
            mSelectedContacts.addAll(contacts);
        }

        // Disable double clicking on the contact
        mDoubleClickEnabled = false;

        mContactList.useReducedMenu();
        mContactList.setMultipleSelectionEnabled(true);
        mContactList.addSelectionListener(new AddChatContactSelectionListener());

        // In case the IM account goes off- and online while this dialog is
        // open, add a service listener so we can keep our reference to the
        // current IM protocol provider up to date.
        GuiActivator.bundleContext.addServiceListener(this);

        // We probably do already have an active IM provider, so get that now,
        // as the service listener won't be kicked unless the provider goes
        // offline.
        mImProvider = AccountUtils.getImProvider();
        if (mImProvider != null)
        {
            handleImProviderAdded(mImProvider);
        }

        refreshUI();
    }

    @Override
    public void dispose()
    {
        if (mInviteFilter != null)
        {
            mInviteFilter.cleanup();
        }

        removeImListeners();

        super.dispose();
    }

    /**
     * Run whenever something might have changed that means we would want to
     * update the UI (e.g. counts of the number of selected contacts or whether
     * the OK button is enabled).
     */
    protected void refreshUI()
    {
        boolean okButtonEnabled = !mSelectedContacts.isEmpty() &&
                          ((mImProvider != null) && mImProvider.isRegistered());
        sLog.debug("Setting OK button enabled? " + okButtonEnabled);
        mOkButton.setEnabled(okButtonEnabled);
    }

    @Override
    protected void initContactListData()
    {
        ProtocolProviderService imProvider = AccountUtils.getImProvider();

        if (imProvider == null)
        {
            sLog.warn("Unable to get contact list data as IM provider is null");
            return;
        }

        mContactList.addContactSource(
            new ProtocolContactSourceServiceImpl(imProvider,
                OperationSetBasicInstantMessaging.class));

        ProtocolProviderService grpContactProvider =
                                         AccountUtils.getGroupContactProvider();
        if (grpContactProvider == null)
        {
            // We don't need the group contact provider in order to continue,
            // but it should be present.  Hence warn but no return.
            sLog.warn("No group contact provider");
        }
        else
        {
            mContactList.addContactSource(
                new ProtocolContactSourceServiceImpl(grpContactProvider,
                    OperationSetGroupContacts.class));
        }

        mContactList.setCurrentFilter(
            new ChatContactsFilter(mContactList, this, mCanSelectOffline));
        mContactList.applyCurrentFilter();
    }

    /**
     * Returns the list of MetaContacts that are currently selected
     *
     * @return The list of MetaContacts that are currently selected
     */
    public ArrayList<MetaContact> getSelectedContacts()
    {
        synchronized (mSelectedContacts)
        {
            return new ArrayList<>(mSelectedContacts);
        }
    }

    /**
     * Returns the list of preselected MetaContacts
     *
     * @return The list of preselected MetaContacts
     */
    public ArrayList<MetaContact> getPreselectedContacts()
    {
        synchronized (mPreselectedContacts)
        {
            return new ArrayList<>(mPreselectedContacts);
        }
    }

    /**
     * Returns the set of IM Contacts who are required in this dialog. If the
     * 'onlyDisplayRequiredContacts' method returns true, contacts that are not
     * in this set will not be displayed in the dialog.
     *
     * @return The set of required IM Contacts
     */
    public Set<Contact> getRequiredIMContacts()
    {
        synchronized (requiredIMContacts)
        {
            return new HashSet<>(requiredIMContacts);
        }
    }

    /**
     * Replaces the set of IM Contacts who are required in this dialog with
     * the given set. If the 'onlyDisplayRequiredContacts' method returns true,
     * contacts that are not in this set will not be displayed in the dialog.
     *
     * @param contacts The set of required IM Contacts
     */
    public void setRequiredIMContacts(Set<Contact> contacts)
    {
        synchronized (requiredIMContacts)
        {
            requiredIMContacts.clear();
            requiredIMContacts.addAll(contacts);
        }
    }

    /**
     * Add some more preselected contacts to the sets of selected and preselected
     * contacts.
     *
     * @param contacts The contacts to add
     */
    protected void addPreselectedContacts(Set<MetaContact> contacts)
    {
        synchronized (mPreselectedContacts)
        {
            mPreselectedContacts.addAll(contacts);
        }

        synchronized (mSelectedContacts)
        {
            mSelectedContacts.addAll(contacts);
        }

        refreshUI();
    }

    @Override
    protected void initSearchField()
    {
        mInviteFilter = new InviteChatContactListFilter(mContactList);

        mSearchField =
            new SearchField(null,
                            mInviteFilter,
                            false,
                            sResources.getI18NString("service.gui.ENTER_NAME"));
        mSearchField.setPreferredSize(new ScaledDimension(200, 30));
        ScaleUtils.scaleFontAsDefault(mSearchField);
        mSearchField.setContactList(mContactList);
    }

    @Override
    protected abstract void okPressed(UIContact uiContact,
                                      String enteredText,
                                      String phoneNumber);

    /**
     * @return true if GroupContacts should be displayed in the list of IM
     * contacts in this dialog, false otherwise.  Default is true.
     */
    public boolean includeGroupContacts()
    {
        return true;
    }

    /**
     * @return true if the user is allowed to de-select contacts that are
     * preselected in the dialog, false otherwise. Default is false.  Note that
     * users are always allowed to select then de-select any contacts that were
     * not preselected, regardless of what this method returns.
     */
    public boolean allowDeselection()
    {
        return false;
    }

    /**
     * @return true if only contacts in the requiredContacts set should be
     * displayed, false otherwise.  Default is false.
     */
    public boolean onlyDisplayRequiredContacts()
    {
        return false;
    }

    /**
     * Returns true if the given MetaContact should be enabled in the dialog,
     * false otherwise.
     *
     * @param metaContact the MetaContact
     * @return true if the MetaContact should be enabled, false otherwise.
     */
    private boolean shouldBeEnabled(MetaContact metaContact)
    {
        boolean contactEnabled = false;
        boolean isPreselected = mPreselectedContacts.contains(metaContact);

        Contact imContact = metaContact.getIMContact();
        if (imContact != null)
        {
            // If the MetaContact has been preselected, the user cannot
            // deselect it, so disable it in the contact list.  Otherwise,
            // disable it if its presence status is offline and the
            // user is not allowed to select offline contacts.
            boolean isOnline = imContact.getPresenceStatus().isOnline();

            contactEnabled = (!isPreselected || allowDeselection()) &&
                             (mCanSelectOffline || isOnline);
        }
        else if (includeGroupContacts() &&
                 metaContact.getGroupContact() != null)
        {
            // Group contacts are allowed, this is a group contact - enabled.
            contactEnabled = true;
        }

        return contactEnabled;
    }

    /**
     * Run when the IM provider is registered.  Adds a registration state
     * listener to the IM provider so we add/remove a contact presence status
     * listener to/from the IM provider's presence operation set when the IM
     * account registers/unregisters.
     *
     * @param pps the IM provider that has registered.
     */
    private void handleImProviderAdded(ProtocolProviderService pps)
    {
        sLog.info("IM provider registered: " + pps);
        mImProvider = pps;
        mImProvider.addRegistrationStateChangeListener(this);

        // If the IM account is already registered, we can continue to add
        // a contact presence status listener.
        if (mImProvider.isRegistered())
        {
            handleImAccountRegistered();
        }
    }

    /**
     * Run when the IM account registers.  If the user is not allowed to select
     * offline contacts, it adds a contact presence status listener to the IM
     * provider's presence operation set so we can disable/enable contacts as
     * they go offline/online.
     */
    private void handleImAccountRegistered()
    {
        sLog.info("IM account registered");

        if (!mCanSelectOffline && mImProvider != null)
        {
            sLog.info("Adding contact presence listener");
            mPresenceOpSet =
                mImProvider.getOperationSet(OperationSetPresence.class);

            if (mPresenceOpSet != null)
            {
                mPresenceOpSet.addContactPresenceStatusListener(this);
            }
        }

        // Force the contact list to re-apply the current filter immediately to
        // show up-to-date IM contacts.
        mContactList.applyCurrentFilter();
        refreshUI();
    }

    /**
     * Run when the IM provider is unregistering.  Removes the registration
     * state listener from the IM provider and calls the method to remove
     * and contact presence listener and refresh what is displayed in the
     * contact list.
     *
     * @param pps the IM provider that is unregistering.
     */
    private void handleProviderRemoved(ProtocolProviderService pps)
    {
        sLog.info("IM provider unregistering: " + pps);
        mImProvider = null;
        pps.removeRegistrationStateChangeListener(this);
        handleImAccountUnregistered();
    }

    /**
     * Run when the IM account unregisters.  If we have a saved presence
     * Operation Set, remove this class as a contact presence listener from it.
     * Also, refresh the UI to ensure the contact list is up-to-date.
     */
    protected void handleImAccountUnregistered()
    {
        sLog.info("Jabber account unregistered");
        if (mPresenceOpSet != null)
        {
            mPresenceOpSet.removeContactPresenceStatusListener(this);
            mPresenceOpSet = null;
        }

        // Force the contact list to re-apply the current filter immediately to
        // get rid of any out of date IM contacts.
        mContactList.applyCurrentFilter();
        refreshUI();
    }

    @Override
    public void serviceChanged(ServiceEvent evt)
    {
        ServiceReference<?> serviceRef = evt.getServiceReference();

        // If the event is caused by a bundle being stopped, we don't care.
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            return;

        Object service = GuiActivator.bundleContext.getService(serviceRef);

        // We don't care if the source service is not a protocol provider.
        if (!(service instanceof ProtocolProviderService))
            return;

        ProtocolProviderService pps = (ProtocolProviderService) service;

        if (ProtocolNames.JABBER.equals(pps.getProtocolName()))
        {
            int eventType = evt.getType();

            if (ServiceEvent.REGISTERED == eventType)
            {
                sLog.info("IM provider added");
                handleImProviderAdded(pps);
            }
            else if (ServiceEvent.UNREGISTERING == eventType)
            {
                sLog.info("IM provider removed");
                handleProviderRemoved(pps);
            }
        }
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState newState = evt.getNewState();
        sLog.debug("IM registration state changed: " + newState);

        if (RegistrationState.REGISTERED.equals(newState))
        {
            handleImAccountRegistered();
        }
        else if (RegistrationState.UNREGISTERED.equals(newState) ||
                 RegistrationState.CONNECTION_FAILED.equals(newState))
        {
            handleImAccountUnregistered();
        }
    }

    @Override
    public void contactPresenceStatusChanged(ContactPresenceStatusChangeEvent evt)
    {
        Contact sourceContact = evt.getSourceContact();
        MetaContact metaContact =
            GuiActivator.getContactListService().findMetaContactByContact(sourceContact);

        if (metaContact == null)
        {
            return;
        }

        UIContactImpl uiContact = mMetaContactListSource.getUIContact(metaContact);

        if (uiContact == null)
        {
            return;
        }

        // We've found a UI contact for the MetaContact whose presence has
        // changed.  If the presence has switched between an offline state and
        // an online state,  we may need to update the enabled state of the UI
        // contact.
        boolean shouldBeEnabled = shouldBeEnabled(metaContact);
        boolean isEnabled = uiContact.isEnabled();

        if (shouldBeEnabled && !isEnabled)
        {
            sLog.debug("Enabling UI Contact after presence update: " + uiContact);
            uiContact.setEnabled(true);

            // If the contact is not selected, make sure its selection state in
            // TreeContact list is cleared.  This can get out of sync if the UI
            // Contact was already selected when it went offline and the user
            // then clicked on it to deselect it while it was still offline and
            // disabled.
            if (!uiContact.isSelected())
            {
                sLog.debug("Removing selection for contact " + uiContact);
                mContactList.removeSelectedContact(uiContact);
            }
        }
        else if (!shouldBeEnabled && isEnabled)
        {
            sLog.debug("Disabling UI Contact after presence update: " + uiContact);
            uiContact.setEnabled(false);
        }
    }

    /**
     * Removes the listeners associated with the IM provider.
     */
    protected void removeImListeners()
    {
        sLog.info("Removing IM listeners from " + this);
        GuiActivator.bundleContext.removeServiceListener(this);

        if (mImProvider != null)
        {
            mImProvider.removeRegistrationStateChangeListener(this);
        }

        if (mPresenceOpSet != null)
        {
            mPresenceOpSet.removeContactPresenceStatusListener(this);
            mPresenceOpSet = null;
        }
    }

    /**
     * The <tt>InviteChatContactListFilter</tt> is a <tt>SearchFilter</tt> that
     * filters the contact list to fit chat invite operations.
     */
    private class InviteChatContactListFilter extends SearchFilter
    {
        /**
         * Creates an instance of <tt>InviteContactListFilter</tt>.
         *
         * @param sourceContactList the contact list to filter
         */
        public InviteChatContactListFilter(TreeContactList sourceContactList)
        {
            super(sourceContactList);
        }

        @Override
        public void applyFilter(FilterQuery filterQuery)
        {
            filterQuery.setMaxResultShown(-1);

            Collection<UIContactSource> filterSources =
                                        mSourceContactList.getContactSources();

            Iterator<UIContactSource> filterSourceIter = filterSources.iterator();

            // If we have stopped filtering in the meantime we return here.
            if (filterQuery.isCanceled())
                return;

            // Then we apply the filter on all its contact sources.
            while (filterSourceIter.hasNext())
            {
                final UIContactSource filterSource = filterSourceIter.next();

                // Only include the protocol contact source
                if (!(filterSource.getContactSourceService() instanceof
                                              ProtocolContactSourceServiceImpl))
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
            {
                filterQuery.close();
            }
            else if (!mSourceContactList.isEmpty() &&
                     !GuiActivator.enterDialsNumber())
            {
                // Select the first contact if 'enter' doesn't dial the searched
                // number, otherwise the first contact would be called when 'enter'
                // is pressed.
                mSourceContactList.selectFirstContact();
            }
        }

        @Override
        protected void addMatching(List<SourceContact> sourceContacts)
        {
            // Take a copy to prevent Concurrent Modification Exceptions
            List<SourceContact> contacts =
                    new ArrayList<>(sourceContacts);

            for (SourceContact contact : contacts)
            {
                UIContact uiContact = getUIContact(contact);
                if (uiContact != null)
                {
                    mSourceContactList.addContact(uiContact, null, true, true);
                }
            }
        }

        /**
         * Handles a source contact from the query by constructing a UI contact
         * and passing to the contact list.
         *
         * @param contact the source contact returned by the query
         * @return the UI contact constructed from this source contact
         */
        private UIContact getUIContact(SourceContact contact)
        {
            UIContactImpl uiContact = null;

            // Get the MetaContact that represents this Source Contact
            MetaContact metaContact = (MetaContact) contact.getData(
                                        SourceContact.DATA_FILTER_META_CONTACT);

            // If we can't find the MetaContact then ignore this query result -
            // we need a MetaContact to be able to add this buddy to the chat
            // session.
            if (metaContact != null)
            {
                Contact imContact = metaContact.getIMContact();

                if (onlyDisplayRequiredContacts() &&
                    (imContact == null ||
                     !getRequiredIMContacts().contains(imContact)))
                {
                    // If we should only display required contacts and this
                    // isn't one of them, just return null.
                    return null;
                }

                // Create a new UI contact for this MetaContact.
                uiContact = mMclSource.createUIContact(metaContact);

                uiContact.setEnabled(shouldBeEnabled(metaContact));

                // Add the contact to the contact list but only if the
                // query has not yet been cancelled.
                mSourceContactList.addContact(uiContact, null, true, true);

                if (mSelectedContacts.contains(metaContact))
                {
                    mSourceContactList.setSelectedContact(uiContact, true);
                    uiContact.setSelected(true);
                    sLog.info("Setting contact as selected " + uiContact);
                }
            }

            return uiContact;
        }

        @Override
        public void contactReceived(ContactReceivedEvent event)
        {
            UIContact uiContact = getUIContact(event.getContact());
            if (uiContact != null)
            {
                if (mInviteFilter.isMatching(uiContact))
                {
                    mSourceContactList.addContact(uiContact, null, true, true);
                }
                else
                {
                    mSourceContactList.removeContact(uiContact, false);
                }
            }
        }
    }

    /**
     * A listener that responds to contact list selection change events
     */
    private class AddChatContactSelectionListener implements ContactListSelectionListener
    {
        @Override
        public void contactSelected(ContactNode node)
        {
            if (!(node.getContactDescriptor() instanceof MetaUIContact))
            {
                return;
            }

            toggleContact((MetaUIContact) node.getContactDescriptor());
        }

        /**
         * Toggles the selected state of the given contact
         *
         * @param metaUIContact the UI contact for which to toggle the selected
         * state.
         */
        private void toggleContact(MetaUIContact metaUIContact)
        {
            MetaContact metaContact = metaUIContact.getMetaContact();

            boolean contactEnabled = shouldBeEnabled(metaContact);
            boolean isPreselected = mPreselectedContacts.contains(metaContact);

            synchronized (mSelectedContacts)
            {
                if (!mSelectedContacts.contains(metaContact) && contactEnabled)
                {
                    mSelectedContacts.add(metaContact);
                    metaUIContact.setSelected(true);
                }
                else if (!isPreselected || allowDeselection())
                {
                    mSelectedContacts.remove(metaContact);
                    metaUIContact.setSelected(false);
                }

                refreshUI();
            }
        }
    }
}
