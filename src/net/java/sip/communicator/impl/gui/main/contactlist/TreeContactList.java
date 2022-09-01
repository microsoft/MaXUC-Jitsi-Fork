/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Map.*;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.callpark.CallParkUIContact;
import net.java.sip.communicator.impl.gui.main.callpark.CallParkWindow;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.callhistory.event.*;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.callhistory.CallRecord;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.managecontact.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.ContactLogger;
import net.java.sip.communicator.util.Logger;

/**
 * The <tt>TreeContactList</tt> is a contact list based on JTree.
 *
 * @author Yana Stamcheva
 */
public class TreeContactList
    extends DefaultTreeContactList
    implements  ContactList,
                ContactQueryListener,
                MetaContactQueryListener,
                MouseListener,
                MouseMotionListener,
                TreeExpansionListener,
                TreeSelectionListener,
                CallHistoryChangeListener,
                SourceContactChangeListener,
                StateDumper
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger.
     */
    private static final Logger sLog =Logger.getLogger(TreeContactList.class);

    /**
     * The logger.
     */
    private static final ContactLogger contactLogger
        = ContactLogger.getLogger();

    /**
     * The default tree model.
     */
    private ContactListTreeModel mTreeModel;

    /**
     * The right button menu.
     */
    private Component mRightButtonMenu;

    /**
     * Indicates if the full right mouse button menu is enabled.
     */
    private boolean mIsRightButtonMenuEnabled = true;

    /**
     * A list of all contacts that are currently "active". An "active" contact
     * is a contact that has been sent a message. The list is used to indicate
     * these contacts with a special icon.
     */
    private final java.util.List<ContactNode> mActiveContacts
        = new ArrayList<>();

    /**
     * A list of all registered <tt>ContactListListener</tt>-s.
     */
    private final java.util.List<ContactListListener> mContactListListeners
        = new ArrayList<>();

    /**
     * A ContactSourceServiceListener to listen for for adding and removing of
     * <tt>ContactSourceService</tt> implementatins.
     */
    private ContactSourceServiceListener mContactSourceServiceListener;

    /**
     * The meta contact list source.
     */
    private final MetaContactListSource mMclSource;

    /**
     * The presence filter.
     */
    private PresenceFilter mPresenceFilter;

    /**
     * The search filter.
     */
    private SearchFilter mSearchFilter;

    /**
     * The favorites filter
     */
    private FavoritesFilter mFavoritesFilter;

    /**
     * The call history filter.
     */
    private CallHistoryFilter mCallHistoryFilter;

    /**
     * The message history filter.
     */
    private MessageHistoryFilter mMessageHistoryFilter;

    /**
     * The call and message history filter.
     */
    private AllHistoryFilter mAllHistoryFilter;

    /**
     * The current filter.
     */
    private ContactListFilter mCurrentFilter;

    /**
     * Indicates if the click on a group node has been already consumed. This
     * could happen in a move contact mode.
     */
    private boolean mIsGroupClickConsumed = false;

    /**
     * A list of all originally registered <tt>MouseListener</tt>-s.
     */
    private MouseListener[] mOriginalMouseListeners;

    /**
     * A map of all the UIContactSources.  Map is from the underlying
     * ContactSourceService to the UIContactSource that wraps around that CSS.
     */
    private final Map<ContactSourceService, UIContactSource> mContactSources =
            new HashMap<>();

    /**
     * The currently used filter query.
     */
    private UIFilterQuery mCurrentFilterQuery;

    /**
     * The thread used to do the filtering.
     */
    private FilterThread mFilterThread;

    /**
     * The timer service used to schedule a refresh of the current filter each
     * time the current day changes.
     * Use a ScheduledExecutorService so that the period we wait is independent
     * of changes in the system clock - using a Timer object would cause the
     * refresh to trigger only when the system clock exceeds the scheduled date,
     * which may vary if the clock is changed.
     */
    private ScheduledExecutorService mRefreshTimer;

    /**
     * Indicates that the received call image search has been canceled.
     */
    private static boolean mImageSearchCanceled = false;

    /**
     * Indicates if the contact buttons should be disabled.
     */
    private boolean mIsContactButtonsVisible = true;

    /**
     * The container, where this contact list component is added.
     */
    private ContactListContainer mParentCLContainer;

    /**
     * Whether the client is allowed to make calls (set in the CoS) and VoIP is
     * enabled by the user in options.  If so, we display call history contacts.
     * Assume VoIP is active until we have received the CoS and checked config.
     */
    private boolean mVoipEnabled = true;

    /**
     * A list of selection listeners for this contact list
     */
    private Set<ContactListSelectionListener> mSelectionListeners =
            new HashSet<>();

    /**
     * True if we should use a reduced menu for the right click menu
     */
    private boolean useReducedMenu = false;

    /**
     * Creates the <tt>TreeContactList</tt>.
     *
     * @param clContainer the container, where this contact list component is
     * added
     */
    public TreeContactList(ContactListContainer clContainer)
    {
        mParentCLContainer = clContainer;

        mMclSource = new MetaContactListSource(this);

        mPresenceFilter = new PresenceFilter(this);
        // Default to the current filter being the presence filter until it is
        // set elsewhere to avoid NPEs accessing the mCurrentFilter.
        mCurrentFilter = mPresenceFilter;
        mSearchFilter = new SearchFilter(this);
        mFavoritesFilter = new FavoritesFilter(this);
        mCallHistoryFilter = new CallHistoryFilter(this);
        mMessageHistoryFilter = new MessageHistoryFilter(this);
        mAllHistoryFilter = new AllHistoryFilter(this);

        // Remove default mouse listeners and keep them locally in order to
        // be able to consume some mouse events for custom use.
        mOriginalMouseListeners = getMouseListeners();
        for (MouseListener listener : mOriginalMouseListeners)
        {
           removeMouseListener(listener);
        }

        addMouseListener(this);
        addMouseMotionListener(this);
        addTreeExpansionListener(this);
        addTreeSelectionListener(this);

        GuiActivator.getContactListService().addMetaContactListListener(mMclSource);

        mTreeModel = new ContactListTreeModel(this);

        setTreeModel(mTreeModel);

        // We hide the root node as it doesn't represent a real group.
        if (isRootVisible())
            setRootVisible(false);

        initKeyActions();

        initContactSources();

        GuiActivator.getCallHistoryService().addCallHistoryChangeListener(this);

        // Listen for changes to SourceContacts that we might be displaying.
        GuiActivator.getMessageHistoryService().addSourceContactChangeListener(this);

        // Start listening to see if the class of service allows VoIP. If not
        // we should not display call history contacts.
        final CPCosGetterCallback callback = new CPCosGetterCallback()
        {
            @Override
            public void onCosReceived(CPCos classOfService)
            {
                // We could use ConfigurationUtils.isVoIPEnabledInCoS() for
                // the VoIP enabled by CoS setting, but there is a race
                // condition between the CoS storeCosFieldsInConfig() method
                // saving this setting to the config file, and this callback
                // being called (where isVoIPEnabledInCoS() would then read
                // that same config file).
                boolean voipEnabledInCoS = classOfService.getSubscribedMashups().isVoipAllowed();
                boolean voipEnabledByUser = ConfigurationUtils.isVoIPEnabledByUser();
                mVoipEnabled = voipEnabledInCoS && voipEnabledByUser;
                sLog.info("CoS received, display history contacts based " +
                            "on voip enabled in CoS = " + voipEnabledInCoS +
                            ", and voip enabled by user = " + voipEnabledByUser);
            }
        };
        GuiActivator.getCosService().getClassOfService(callback);

        DiagnosticsServiceRegistrar.registerStateDumper(this, GuiActivator.bundleContext);

        // Add a listener for the parent window being closed.  This is required
        // so that we can clean up this TreeContactList - unregistering the
        // various listeners added above.
        // This must be done on the EDT as it requires the TCL to have been
        // added to the parent.
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                final TreeContactList tcl = TreeContactList.this;
                Window parent = SwingUtilities.getWindowAncestor(tcl);

                if (parent != null)
                {
                    parent.addWindowListener(new WindowAdapter()
                    {
                        @Override
                        public void windowClosed(WindowEvent e)
                        {
                            sLog.info("Window closing " + tcl);
                            if (mFilterThread != null)
                            {
                                synchronized (mFilterThread)
                                {
                                    mFilterThread.requestShutdown();
                                }
                            }

                            if (mRefreshTimer != null)
                            {
                                mRefreshTimer.shutdownNow();
                            }

                            GuiActivator.getCallHistoryService()
                                        .removeCallHistoryChangeListener(tcl);
                            GuiActivator.getMessageHistoryService()
                                        .removeSourceContactChangeListener(tcl);
                            GuiActivator.getCosService()
                                        .unregisterCallback(callback);
                            GuiActivator.getContactListService()
                                        .removeMetaContactListListener(mMclSource);
                            GuiActivator.bundleContext.removeServiceListener(
                                        mContactSourceServiceListener);
                            DiagnosticsServiceRegistrar.unregisterStateDumper(tcl);
                            mSearchFilter.cleanup();
                        }
                    });
                }
            }
        });
    }

    /**
     * Indicates that a contact has been received for a query.
     * @param event the <tt>ContactReceivedEvent</tt> that notified us
     */
    public void contactReceived(ContactReceivedEvent event)
    {
        final SourceContact sourceContact = event.getContact();

        ContactSourceService contactSource
            = sourceContact.getContactSource();

        UIContact uiContact = null;

        UIContactSource sourceUI = getContactSource(contactSource);

        if (sourceUI == null)
            return;

        uiContact = sourceUI.createUIContact(sourceContact, mMclSource);

        // ExtendedContactSourceService has already matched the
        // SourceContact over the pattern
        if((contactSource instanceof ExtendedContactSourceService)
            || mCurrentFilter.isMatching(uiContact))
        {
            boolean isSorted = (sourceContact.getIndex() > -1);

            addContact(event.getQuerySource(),
                uiContact,
                sourceUI.getUIGroup(),
                isSorted);
        }
        else
        {
            sourceUI.removeUIContact(sourceContact);
            uiContact = null;
        }
    }

    /**
     * @return the presence filter
     */
    public PresenceFilter getPresenceFilter()
    {
        return mPresenceFilter;
    }

    /**
     * @return the search filter
     */
    public SearchFilter getSearchFilter()
    {
        return mSearchFilter;
    }

    /**
     * @return the favorites filter
     */
    public FavoritesFilter getFavoritesFilter()
    {
        return mFavoritesFilter;
    }

    /**
     * @return the callHistory filter
     */
    public CallHistoryFilter getCallHistoryFilter()
    {
        return mCallHistoryFilter;
    }

    /**
     * @return the messageHistory filter
     */
    public MessageHistoryFilter getMessageHistoryFilter()
    {
        return mMessageHistoryFilter;
    }

    /**
     * @return the allHistory filter
     */
    public AllHistoryFilter getAllHistoryFilter()
    {
        return mAllHistoryFilter;
    }

    /**
     * Indicates that a contact has been removed after a search.
     * @param event the <tt>ContactQueryEvent</tt> containing information
     * about the received <tt>SourceContact</tt>
     */
    public void contactRemoved(ContactRemovedEvent event)
    {
        final SourceContact sourceContact = event.getContact();

        ContactSourceService contactSource
            = sourceContact.getContactSource();

        UIContactSource sourceUI = getContactSource(contactSource);

        if (sourceUI == null)
            return;

        UIContact uiContact
            = sourceUI.getUIContact(sourceContact);

        if(uiContact == null)
            return;

        // ExtendedContactSourceService has already matched the
        // SourceContact over the pattern
        if((contactSource instanceof ExtendedContactSourceService)
            || mCurrentFilter.isMatching(uiContact))
        {
            removeContact(uiContact, false);
        }
    }

    /**
     * Indicates that a contact has been updated after a search.
     * @param event the <tt>ContactQueryEvent</tt> containing information
     * about the updated <tt>SourceContact</tt>
     */
    public void contactChanged(ContactChangedEvent event)
    {
        final SourceContact sourceContact = event.getContact();

        ContactSourceService contactSource
            = sourceContact.getContactSource();

        UIContactSource sourceUI = getContactSource(contactSource);

        if (sourceUI == null)
            return;

        UIContact uiContact
            = sourceUI.getUIContact(sourceContact);

        if(!(uiContact instanceof UIContactImpl))
            return;

        ContactNode contactNode = ((UIContactImpl) uiContact).getContactNode();

        if (contactNode != null)
            nodeChanged(contactNode);
    }

    /**
     * Indicates that a <tt>MetaContact</tt> has been received for a search in
     * the <tt>MetaContactListService</tt>.
     * @param event the received <tt>MetaContactQueryEvent</tt>
     */
    public void metaContactReceived(MetaContactQueryEvent event)
    {
        sLog.trace("metaContactReceived");

        MetaContact metaContact = event.getMetaContact();
        MetaContactGroup parentGroup = metaContact.getParentMetaContactGroup();

        UIGroup uiGroup = null;
        if (parentGroup != null)
        {
            if (!mMclSource.isRootGroup(parentGroup))
            {
                synchronized (parentGroup)
                {
                    uiGroup = mMclSource.getUIGroup(parentGroup);

                    if (uiGroup == null)
                    {
                        uiGroup = mMclSource.createUIGroup(parentGroup);
                    }
                }
            }
        }
        else
        {
            // No need to hash MetaContact, as its toString() method does that.
            contactLogger.error(metaContact, "Parent MetaContact Group is null");
        }

        UIContact newUIContact;
        synchronized (metaContact)
        {
            // Only create a UIContact if there isn't already one for this
            // metaContact (i.e. the contact hasn't already been added to the
            // GUI).
            newUIContact = mMclSource.getUIContact(metaContact);

            if (newUIContact == null)
            {
                newUIContact = mMclSource.createUIContact(metaContact);
            }
        }

        addContact(event.getQuerySource(),
                   newUIContact,
                   uiGroup,
                   true);
    }

    /**
     * Indicates that a <tt>MetaGroup</tt> has been received from a search in
     * the <tt>MetaContactListService</tt>.
     * @param event the <tt>MetaContactGroupQueryEvent</tt> that has been
     * received
     */
    public void metaGroupReceived(MetaGroupQueryEvent event)
    {
        MetaContactGroup metaGroup = event.getMetaGroup();

        UIGroup uiGroup;
        synchronized (metaGroup)
        {
            uiGroup = mMclSource.createUIGroup(metaGroup);
        }

        if (uiGroup != null)
        {
            addGroup(uiGroup, true);
        }
    }

    /**
     * Indicates that the status of a query has changed.
     * @param event the <tt>ContactQueryStatusEvent</tt> that notified us
     */
    public void queryStatusChanged(ContactQueryStatusEvent event)
    {
        int eventType = event.getEventType();

        if (eventType == ContactQueryStatusEvent.QUERY_ERROR)
        {
            sLog.info("Contact query error occured: "
                                + event.getQuerySource());
        }
        event.getQuerySource().removeContactQueryListener(this);
    }

    /**
     * Indicates that the status of a query has changed.
     * @param event the <tt>ContactQueryStatusEvent</tt> that notified us
     */
    public void metaContactQueryStatusChanged(MetaContactQueryStatusEvent event)
    {
        int eventType = event.getEventType();

        if (eventType == ContactQueryStatusEvent.QUERY_ERROR)
        {
            sLog.info("Contact query error occured: "
                                + event.getQuerySource());
        }
        event.getQuerySource().removeContactQueryListener(this);
    }

    /**
     * Returns the right button menu opened over the contact list.
     *
     * @return the right button menu opened over the contact list
     */
    public Component getRightButtonMenu()
    {
        return mRightButtonMenu;
    }

    /**
     * Deactivates all active contacts.
     */
    public void deactivateAll()
    {
        for (ContactNode contactNode : mActiveContacts)
        {
            if (contactNode != null)
                contactNode.setActive(false);
        }
        mActiveContacts.clear();
    }

    /**
     * Updates the active state of the contact node corresponding to the given
     * <tt>MetaContact</tt>.
     * @param metaContact the <tt>MetaContact</tt> to update
     * @param isActive indicates if the node should be set to active
     */
    public void setActiveContact(MetaContact metaContact, boolean isActive)
    {
        UIContactImpl uiContact = mMclSource.getUIContact(metaContact);

        if (uiContact == null)
            return;

        ContactNode contactNode = uiContact.getContactNode();

        if (contactNode != null)
        {
            contactNode.setActive(isActive);

            if (isActive)
            {
                mActiveContacts.add(contactNode);
            }
            else
            {
                mActiveContacts.remove(contactNode);
            }

            mTreeModel.nodeChanged(contactNode);
        }
    }

    /**
     * Returns <tt>true</tt> if the given <tt>metaContact</tt> has been
     * previously set to active, otherwise returns <tt>false</tt>.
     * @param contact the <tt>UIContact</tt> to check
     * @return <tt>true</tt> if the given <tt>metaContact</tt> has been
     * previously set to active, otherwise returns <tt>false</tt>
     */
    public boolean isContactActive(UIContactImpl contact)
    {
        ContactNode contactNode = contact.getContactNode();

        return (contactNode == null) ? false : contactNode.isActive();
    }

    @Override
    public void addContact(final UIContact contact,
                           final UIGroup group,
                           final int index,
                           final boolean isGroupSorted)
    {
        addContact(contact, group, index, false, isGroupSorted);
    }

    @Override
    public void addContact(final UIContact contact,
                           final UIGroup group,
                           final boolean isContactSorted,
                           final boolean isGroupSorted)
    {
        addContact(contact, group, -1, isContactSorted, isGroupSorted);
    }

    /**
     * Adds the given <tt>contact</tt> to this list.
     *
     * @param contact the <tt>UIContact</tt> to add
     * @param group the <tt>UIGroup</tt> to add to
     * @param index the index at which to insert the contact (if -1 this will be ignored)
     * @param isContactSorted indicates if the contact should be sorted
     * regarding to the <tt>GroupNode</tt> policy
     * @param isGroupSorted indicates if the group should be sorted regarding to
     * the <tt>GroupNode</tt> policy in case it doesn't exist and should be
     * added
     */
    private void addContact(final UIContact contact,
                            final UIGroup group,
                            final int index,
                            final boolean isContactSorted,
                            final boolean isGroupSorted)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addContact(contact, group, index, isContactSorted, isGroupSorted);
                }
            });
            return;
        }

        if (contact instanceof SourceUIContact)
        {
            ContactSourceService contactSource =
                ((SourceUIContact) contact).getSourceContact().getContactSource();

            if (contactSource.getType() == ContactSourceService.CALL_HISTORY_TYPE)
            {
                // No need to hash UIContact, as its toString() method does that.
                sLog.debug("Adding history contact " + contact +
                             ", voipEnabled = " + mVoipEnabled);
            }
        }

        GroupNode groupNode = null;
        if (group == null ||
            (group instanceof MetaUIGroup))
        {
            // When contact groups are flattened, force all contacts that are
            // in a MetaUI group into the root so they don't appear to be
            // under a contact group.
            groupNode = mTreeModel.getRoot();
        }
        else if (group instanceof UIGroupImpl)
        {
            UIGroupImpl contactImpl = (UIGroupImpl) group;

            groupNode = contactImpl.getGroupNode();

            if (groupNode == null)
            {
                GroupNode parentNode = mTreeModel.getRoot();

                if (isGroupSorted)
                    groupNode = parentNode.sortedAddContactGroup(contactImpl);
                else
                    groupNode = parentNode.addContactGroup(contactImpl);
            }
        }

        if (groupNode == null)
            return;

        contact.setParentGroup(groupNode.getGroupDescriptor());

        if (!(contact instanceof UIContactImpl))
            return;

        // The only updates that the call park window cares about are those
        // from CallParkUIContacts.  If it is notified of other updates,
        // duplicate orbits will appear in the call park window.
        if ((mParentCLContainer instanceof CallParkWindow) &&
            (!(contact instanceof CallParkUIContact)))
        {
            return;
        }

        UIContactImpl contactImpl = (UIContactImpl) contact;

        if (isContactSorted)
            groupNode.sortedAddContact(contactImpl);
        else
            groupNode.addContact(contactImpl, index);

        if ((!mCurrentFilter.equals(mPresenceFilter)
                || !groupNode.isCollapsed()))
        {
            expandGroup(groupNode);
        }
        else
        {
            expandGroup(mTreeModel.getRoot());
        }
    }

    /**
     * Adds the given <tt>contact</tt> to this list.
     * @param query the <tt>MetaContactQuery</tt> that adds the given contact
     * @param contact the <tt>UIContact</tt> to add
     * @param group the <tt>UIGroup</tt> to add to
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    private void addContact(final MetaContactQuery query,
                            final UIContact contact,
                            final UIGroup group,
                            final boolean isSorted)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addContact(query, contact, group, isSorted);
                }
            });
            return;
        }

        // If in the meantime the corresponding query was canceled
        // we don't proceed with adding.
        if (query != null && !query.isCanceled())
            addContact(contact, group, isSorted, true);
    }

    /**
     * Adds the given <tt>contact</tt> to this list.
     * @param query the <tt>ContactQuery</tt> that adds the given contact
     * @param contact the <tt>UIContact</tt> to add
     * @param group the <tt>UIGroup</tt> to add to
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    public void addContact(final ContactQuery query,
                            final UIContact contact,
                            final UIGroup group,
                            final boolean isSorted)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            LowPriorityEventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    addContact(query, contact, group, isSorted);
                }
            });
            return;
        }

        // If in the meantime the filter has changed we don't
        // add the contact.
        if (query != null
                && mCurrentFilterQuery.containsQuery(query))
        {
            addContact(contact, group, isSorted, true);
        }
    }

    /**
     * Removes the node corresponding to the given <tt>MetaContact</tt> from
     * this list.
     * @param contact the <tt>UIContact</tt> to remove
     * @param removeEmptyGroup whether we should delete the group if is empty
     */
    public void removeContact(final UIContact contact,
                              final boolean removeEmptyGroup)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeContact(contact, removeEmptyGroup);
                }
            });
            return;
        }

        if (!(contact instanceof UIContactImpl))
            return;

        UIGroupImpl parentGroup = (UIGroupImpl) contact.getParentGroup();

        if (parentGroup == null)
            return;

        GroupNode parentGroupNode = parentGroup.getGroupNode();

        // If we didn't find the parent, it must be in the root.
        if (parentGroupNode == null)
        {
            getTreeModel().getRoot().removeContact((UIContactImpl) contact);
            return;
        }

        parentGroupNode.removeContact((UIContactImpl) contact);

        // If the parent group is empty remove it.
        if (removeEmptyGroup && parentGroupNode.getChildCount() == 0)
        {
            GroupNode parent = (GroupNode) parentGroupNode.getParent();

            if (parent != null)
                parent.removeContactGroup(parentGroup);
        }
    }

    /**
     * Removes the node corresponding to the given <tt>MetaContact</tt> from
     * this list.
     * @param contact the <tt>UIContact</tt> to remove
     */
    public void removeContact(UIContact contact)
    {
        removeContact(contact, true);
    }

    /**
     * Indicates that the information corresponding to the given
     * <tt>contact</tt> has changed.
     *
     * @param contact the contact that has changed
     */
    public void refreshContact(final UIContact contact)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    refreshContact(contact);
                }
            });
            return;
        }

        if (!(contact instanceof UIContactImpl))
            return;

        mTreeModel.nodeChanged(((UIContactImpl) contact).getContactNode());
    }

    /**
     * Adds the given group to this list.
     * @param group the <tt>UIGroup</tt> to add
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    public void addGroup(final UIGroup group, final boolean isSorted)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addGroup(group, isSorted);
                }
            });
            return;
        }

        if ((!(group instanceof UIGroupImpl)) ||
              (group instanceof MetaUIGroup))
        {
            return;
        }

        UIGroupImpl groupImpl = (UIGroupImpl) group;

        GroupNode groupNode = groupImpl.getGroupNode();

        if (groupNode == null)
            groupNode = mTreeModel.getRoot();

        if (isSorted)
            groupNode.sortedAddContactGroup(groupImpl);
        else
            groupNode.addContactGroup(groupImpl);

        expandGroup(mTreeModel.getRoot());
    }

    /**
     * Removes the given group and its children from the list.
     * @param group the <tt>UIGroup</tt> to remove
     */
    public void removeGroup(final UIGroup group)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeGroup(group);
                }
            });
            return;
        }

        if (!(group instanceof UIGroupImpl))
            return;

        UIGroupImpl parentGroup = (UIGroupImpl) group.getParentGroup();

        GroupNode parentGroupNode;

        if(parentGroup == null)
        {
            if(group.countChildContacts() == 0)
                parentGroupNode = mTreeModel.getRoot();
            else
                return;
        }
        else
            parentGroupNode = parentGroup.getGroupNode();

        // Nothing more to do here if we didn't find the parent.
        if (parentGroupNode == null)
            return;

        parentGroupNode.removeContactGroup((UIGroupImpl) group);

        // If the parent group is empty remove it.
        if (parentGroupNode.getChildCount() == 0)
        {
            GroupNode parent = (GroupNode) parentGroupNode.getParent();

            if (parent != null)
                parent.removeContactGroup(parentGroup);
        }
    }

    /**
     * Removes all entries in this contact list.
     */
    public void removeAll()
    {
        mTreeModel.clear();
    }

    /**
     * Returns a collection of all direct child <tt>UIContact</tt>s of the given
     * <tt>UIGroup</tt>.
     *
     * @param group the parent <tt>UIGroup</tt>
     * @return a collection of all direct child <tt>UIContact</tt>s of the given
     * <tt>UIGroup</tt>
     */
    public Collection<UIContact> getContacts(final UIGroup group)
    {
        if (group != null && !(group instanceof UIGroupImpl))
            return null;

        GroupNode groupNode;

        if (group == null)
            groupNode = mTreeModel.getRoot();
        else
            groupNode = ((UIGroupImpl) group).getGroupNode();

        if (groupNode == null)
            return null;

        Collection<ContactNode> contactNodes = groupNode.getContacts();

        if (contactNodes == null)
            return null;

        Collection<UIContact> childContacts = new ArrayList<>();

        Iterator<ContactNode> contactNodesIter = contactNodes.iterator();
        while (contactNodesIter.hasNext())
        {
            childContacts.add(contactNodesIter.next().getContactDescriptor());
        }

        return childContacts;
    }

    /**
     * Adds a listener for <tt>ContactListEvent</tt>s.
     *
     * @param listener the listener to add
     */
    public void addContactListListener(ContactListListener listener)
    {
        synchronized (mContactListListeners)
        {
            if (!mContactListListeners.contains(listener))
                mContactListListeners.add(listener);
        }
    }

    /**
     * Removes a listener previously added with <tt>addContactListListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeContactListListener(ContactListListener listener)
    {
        synchronized (mContactListListeners)
        {
            mContactListListeners.remove(listener);
        }
    }

    /**
     * If set to true prevents all operations coming in response to a mouse
     * click.
     * @param isGroupClickConsumed indicates if the group click event is
     * consumed by an external party
     */
    public void setGroupClickConsumed(boolean isGroupClickConsumed)
    {
        mIsGroupClickConsumed = isGroupClickConsumed;
    }

    /**
     * Applies the current filter.
     * @return the filter query that keeps track of the filtering results
     */
    public FilterQuery applyCurrentFilter()
    {
        FilterQuery filterQuery = null;

        String currentSearchText = mParentCLContainer == null ?
                              null : mParentCLContainer.getCurrentSearchText();

        if (currentSearchText != null
             && currentSearchText.length() > 0)
        {
            // The clear will automatically apply the default filter after
            // the remove text event is triggered!
            if (!SwingUtilities.isEventDispatchThread())
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        mParentCLContainer.clearCurrentSearchText();
                    }
                });
            else
                mParentCLContainer.clearCurrentSearchText();
        }
        else
        {
            filterQuery = applyFilter(mCurrentFilter);
        }

        return filterQuery;
    }

    /**
     * Applies the given <tt>filter</tt>.
     * @param filter the <tt>ContactListFilter</tt> to apply.
     * @return the filter query
     */
    public FilterQuery applyFilter(ContactListFilter filter)
    {
        sLog.debug("Contact list filter applied: " + filter);

        if (mCurrentFilterQuery != null && !mCurrentFilterQuery.isCanceled())
            mCurrentFilterQuery.cancel();

        mCurrentFilterQuery = new UIFilterQuery(this);

        if (mFilterThread == null)
        {
            mFilterThread = new FilterThread();
            mFilterThread.setFilter(filter);
            mFilterThread.start();
        }
        else
        {
            mFilterThread.setFilter(filter);

            synchronized (mFilterThread)
            {
                mFilterThread.notify();
            }

            if (mRefreshTimer == null)
                initRefreshTimer();
        }

        return mCurrentFilterQuery;
    }

    /**
     * Initialises the timer which refreshes the current filter every new day.
     * This ensures that entries with time-dependent text do not become out of
     * date.
     */
    private void initRefreshTimer()
    {
        mRefreshTimer = Executors.newScheduledThreadPool(1);

        Runnable refreshTask = new Runnable()
        {
            public void run()
            {
                sLog.debug("Refreshing contact list filter on change of day.");
                synchronized (mFilterThread)
                {
                    mFilterThread.notify();
                }

                long refreshDelta = getTimeUntilNextRefresh();
                sLog.debug("Next list refresh will be in " + refreshDelta +
                                                             " milliseconds.");
                mRefreshTimer.schedule(this, refreshDelta,
                                                        TimeUnit.MILLISECONDS);
            }
        };

        mRefreshTimer.schedule(refreshTask, getTimeUntilNextRefresh(),
                                                        TimeUnit.MILLISECONDS);
    }

    /**
     * Calculates the amount of time until the contact list filter needs to be
     * reapplied - i.e. the number of milliseconds until the current day
     * changes.
     *
     * @return The number of ms until tomorrow.
     */
    private long getTimeUntilNextRefresh()
    {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        Calendar now = Calendar.getInstance();

        return tomorrow.getTimeInMillis() - now.getTimeInMillis();
    }

    /**
     * The <tt>SearchThread</tt> is meant to launch the search in a separate
     * thread.
     */
    private class FilterThread extends Thread
    {
        /**
         * Whether the filterThread should run.  When false it should exit.
         */
        private boolean mShouldRun = true;

        private ContactListFilter mFilter;

        public FilterThread()
        {
            super("TreeContactList FilterThread");
        }

        public void setFilter(ContactListFilter filter)
        {
            mFilter = filter;
        }

        public void run()
        {
            while (mShouldRun)
            {
                UIFilterQuery filterQuery = mCurrentFilterQuery;
                ContactListFilter filter = mFilter;

                mTreeModel.clear();

                if (!filterQuery.isCanceled())
                {
                    if (mCurrentFilter == null || !mCurrentFilter.equals(filter))
                        mCurrentFilter = filter;

                    // If something goes wrong in our filters, we don't want the
                    // whole gui to crash.
                    try
                    {
                        sLog.debug("FilterThread.run apply filter");
                        mCurrentFilter.applyFilter(filterQuery);
                    }
                    catch (Throwable t)
                    {
                        sLog.error("One of our contact list filters has crashed.", t);
                    }
                }

                synchronized (this)
                {
                    try
                    {
                        // If in the mean time someone has changed the filter
                        // we don't wait here.
                        if (filterQuery == mCurrentFilterQuery)
                            wait();
                    }
                    catch (InterruptedException e)
                    {
                        sLog.info("Filter thread was interrupted.", e);
                    }
                }
            }
        }

        /**
         * Request thread to terminate immediately.
         */
        public void requestShutdown()
        {
            mShouldRun = false;
            notify();
        }
    }

    /**
     * Sets the current filter to the given <tt>filter</tt>.
     * @param filter the <tt>ContactListFilter</tt> to set as default
     */
    public void setCurrentFilter(ContactListFilter filter)
    {
        mCurrentFilter = filter;
    }

    /**
     * Returns the currently applied filter.
     * @return the currently applied filter
     */
    public ContactListFilter getCurrentFilter()
    {
        return mCurrentFilter;
    }

    /**
     * Returns the currently applied filter.
     * @return the currently applied filter
     */
    public FilterQuery getCurrentFilterQuery()
    {
        return mCurrentFilterQuery;
    }

    /**
     * Indicates if this contact list is empty.
     * @return <tt>true</tt> if this contact list contains no children,
     * otherwise returns <tt>false</tt>
     */
    public boolean isEmpty()
    {
        return (mTreeModel.getRoot().getChildCount() <= 0);
    }

    /**
     * Selects the first found contact node from the beginning of the contact
     * list.
     */
    public void selectFirstContact()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                ContactNode contactNode = mTreeModel.findFirstContactNode();

                if (contactNode != null)
                    setSelectionPath(new TreePath(contactNode.getPath()));
            }
        });
    }

    /**
     * Creates the corresponding ContactListEvent and notifies all
     * <tt>ContactListListener</tt>s that a contact is selected.
     *
     * @param source the contact that this event is about.
     * @param eventID the id indicating the exact type of the event to fire.
     * @param clickCount the number of clicks accompanying the event.
     */
    public void fireContactListEvent(Object source, int eventID, int clickCount)
    {
        ContactListEvent evt = new ContactListEvent(source, eventID, clickCount);

        synchronized (mContactListListeners)
        {
            if (mContactListListeners.size() > 0)
            {
                fireContactListEvent(
                        new Vector<>(mContactListListeners),
                    evt);
            }
        }
    }

    /**
     * Notifies all interested listeners that a <tt>ContactListEvent</tt> has
     * occurred.
     * @param contactListListeners the list of listeners to notify
     * @param event the <tt>ContactListEvent</tt> to trigger
     */
    protected void fireContactListEvent(
            java.util.List<ContactListListener> contactListListeners,
            ContactListEvent event)
    {
        synchronized (contactListListeners)
        {
            for (ContactListListener listener : contactListListeners)
            {
                switch (event.getEventID())
                {
                case ContactListEvent.CONTACT_CLICKED:
                    listener.contactClicked(event);
                    break;
                case ContactListEvent.GROUP_CLICKED:
                    listener.groupClicked(event);
                    break;
                case ContactListEvent.CONTACT_SELECTED:
                    listener.contactSelected(event);
                    break;
                case ContactListEvent.GROUP_SELECTED:
                    listener.groupSelected(event);
                    break;
                default:
                    sLog.error("Unknown event type " + event.getEventID());
                }
            }
        }
    }

    /**
     * Expands the given group node.
     * @param groupNode the group node to expand
     */
    private void expandGroup(GroupNode groupNode)
    {
        final TreePath path = new TreePath(mTreeModel.getPathToRoot(groupNode));

        if (!isExpanded(path))
            if (!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        expandPath(path);
                    }
                });
            }
            else
                expandPath(path);
    }

    /**
     * Manages a mouse click over the contact list.
     *
     * When the left mouse button is clicked on a contact cell different things
     * may happen depending on the contained component under the mouse. If the
     * mouse is double clicked on the "contact name" the chat window is opened,
     * configured to use the default protocol contact for the selected
     * MetaContact. If the mouse is clicked on one of the protocol icons, the
     * chat window is opened, configured to use the protocol contact
     * corresponding to the given icon.
     *
     * When the right mouse button is clicked on a contact cell, the cell is
     * selected and the <tt>ContactRightButtonMenu</tt> is opened.
     *
     * When the right mouse button is clicked on a group cell, the cell is
     * selected and the <tt>GroupRightButtonMenu</tt> is opened.
     *
     * When the middle mouse button is clicked on a cell, the cell is selected.
     * @param e the <tt>MouseEvent</tt> that notified us of the click
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path == null)
        {
            return;
        }

        Object lastComponent = path.getLastPathComponent();

        // We're interested only if the mouse is clicked over a tree node.
        if (!(lastComponent instanceof TreeNode))
        {
            return;
        }

        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == 0)
        {
            return;
        }

        // Don't pass clicks on if they were on a button, it's already been handled
        Point point = e.getLocationOnScreen();
        SwingUtilities.convertPointFromScreen(point, this);
        Component cellComponent = findTreeCellComponent(point);
        if (cellComponent instanceof JButton)
        {
            return;
        }

        if (lastComponent instanceof ContactNode)
        {
            fireContactListEvent(
                ((ContactNode) lastComponent).getContactDescriptor(),
                ContactListEvent.CONTACT_CLICKED, e.getClickCount());
        }
        else if (lastComponent instanceof GroupNode)
        {
            fireContactListEvent(
                ((GroupNode) lastComponent).getGroupDescriptor(),
                ContactListEvent.GROUP_CLICKED, e.getClickCount());
        }
    }

    /**
     * When the right mouse button is clicked on a contact cell, the cell is
     * selected and the <tt>ContactRightButtonMenu</tt> is opened.
     *
     * When the right mouse button is clicked on a group cell, the cell is
     * selected and the <tt>GroupRightButtonMenu</tt> is opened.
     *
     * When the middle mouse button is clicked on a cell, the cell is selected.
     * @param e the <tt>MouseEvent</tt> that notified us of the press
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
        if (!mIsGroupClickConsumed)
        {
            // forward the event to the original listeners
            for (MouseListener listener : mOriginalMouseListeners)
               listener.mousePressed(e);
        }

        TreePath path = getPathForLocation(e.getX(), e.getY());

        // If we didn't find any path for the given mouse location, we have
        // nothing to do here.
        if (path == null)
            return;

        Object lastComponent = path.getLastPathComponent();

        // We're interested only if the mouse is clicked over a tree node.
        if (!(lastComponent instanceof TreeNode))
            return;

        boolean isSelected = path.equals(getSelectionPath());

        // Select the node under the right button click.
        if (!isSelected
            && (e.getModifiers() & InputEvent.BUTTON2_MASK) != 0
                || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                || (e.isControlDown() && !e.isMetaDown()))
        {
            setSelectionPath(path);
        }

        if (mIsRightButtonMenuEnabled)
        {
            // Open right button menu when right mouse is pressed.
            if (lastComponent instanceof ContactNode)
            {
                UIContact uiContact
                    = ((ContactNode) lastComponent).getContactDescriptor();

                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                        || (e.isControlDown() && !e.isMetaDown()))
                {
                    mRightButtonMenu =
                        uiContact.getRightButtonMenu(useReducedMenu);

                    if (mRightButtonMenu.isEnabled())
                        openRightButtonMenu(e.getPoint());
                }
            }
            else if (lastComponent instanceof GroupNode)
            {
                UIGroup uiGroup
                    = ((GroupNode) lastComponent).getGroupDescriptor();

                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
                        || (e.isControlDown() && !e.isMetaDown()))
                {
                    mRightButtonMenu = uiGroup.getRightButtonMenu();

                    openRightButtonMenu(e.getPoint());
                }
            }
        }

        if (lastComponent instanceof ContactNode)
        {
            fireSelectionChanged((ContactNode) lastComponent);
        }

        // If not already consumed dispatch the event to underlying
        // cell buttons.
        if (isSelected && e.getClickCount() < 2)
        {
            dispatchEventToButtons(e);
        }
    }

    /**
     * Forwards the given mouse <tt>event</tt> to the list of original
     * <tt>MouseListener</tt>-s.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseEntered(MouseEvent e)
    {
        // forward the event to the original listeners
        for (MouseListener listener : mOriginalMouseListeners)
            listener.mouseEntered(e);
    }

    /**
     * Forwards the given mouse <tt>event</tt> to the list of original
     * <tt>MouseListener</tt>-s.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseExited(MouseEvent e)
    {
        mRenderer.setMouseOverContact(null);

        // forward the event to the original listeners
        for (MouseListener listener : mOriginalMouseListeners)
           listener.mouseExited(e);
    }

    /**
     * Forwards the given mouse <tt>event</tt> to the list of original
     * <tt>MouseListener</tt>-s.
     * @param event the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseReleased(MouseEvent event)
    {
        dispatchEventToButtons(event);

        // forward the event to the original listeners
        for (MouseListener listener : mOriginalMouseListeners)
           listener.mouseReleased(event);
    }

    /**
     * Opens the current right button menu at the given point.
     * @param contactListPoint the point where to position the menu
     */
    private void openRightButtonMenu(Point contactListPoint)
    {
        // If the menu is null we have nothing to do here.
        if (mRightButtonMenu == null)
            return;

        SwingUtilities.convertPointToScreen(contactListPoint, this);

        if (mRightButtonMenu instanceof JPopupMenu)
            ((JPopupMenu) mRightButtonMenu).setInvoker(this);

        mRightButtonMenu.setLocation(contactListPoint.x, contactListPoint.y);
        mRightButtonMenu.setVisible(true);
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        TreePath path = getClosestPathForLocation(e.getX(), e.getY());

        if (path == null)
            return;

        Object element = path.getLastPathComponent();

        for (Object pathElement : path.getPath())
        {
            if (pathElement instanceof SIPCommButton)
            {
                System.out.println("ELEMENT: " + pathElement);
            }
        }
        mRenderer.setMouseOverContact(element);

        dispatchEventToButtons(e);

        // forward the event to the original listeners
        for (MouseListener listener : mOriginalMouseListeners)
           listener.mouseReleased(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {}

    /**
     * Stores the state of the collapsed group.
     * @param event the <tt>TreeExpansionEvent</tt> that notified us for about
     * the expansion
     */
    public void treeCollapsed(TreeExpansionEvent event)
    {
        Object collapsedNode = event.getPath().getLastPathComponent();

        // For now we only save the group state only if we're in presence
        // filter mode.
        if (collapsedNode instanceof GroupNode
                && mCurrentFilter.equals(mPresenceFilter))
        {
            GroupNode groupNode = (GroupNode) collapsedNode;
            String id = groupNode.getGroupDescriptor().getId();
            if (id != null)
                ConfigurationUtils
                    .setContactListGroupCollapsed(id, true);
        }
    }

    /**
     * Stores the state of the expanded group.
     * @param event the <tt>TreeExpansionEvent</tt> that notified us for about
     * the expansion
     */
    public void treeExpanded(TreeExpansionEvent event)
    {
        Object collapsedNode = event.getPath().getLastPathComponent();

        // For now we only save the group state only if we're in presence
        // filter mode.
        if (collapsedNode instanceof GroupNode
                && mCurrentFilter.equals(mPresenceFilter))
        {
            GroupNode groupNode = (GroupNode) collapsedNode;
            String id = groupNode.getGroupDescriptor().getId();
            if (id != null)
                ConfigurationUtils
                    .setContactListGroupCollapsed(id, false);
        }
    }

    /**
     * Dispatches the given mouse <tt>event</tt> to the underlying buttons.
     * @param event the <tt>MouseEvent</tt> to dispatch
     */
    private void dispatchEventToButtons(MouseEvent event)
    {
        TreePath mousePath = getPathForLocation(event.getX(), event.getY());

        /*
         * XXX The check whether mousePath is equal to null was after the
         * assignment to renderer, in the same if as
         * !mousePath.equals(getSelectionPath()). But the assignment to renderer
         * needs mousePath to be non-null because of the call to
         * mousePath.getLastPathComponent().
         */
        if (mousePath == null)
            return;

        TreeCellRenderer treeRenderer = getCellRenderer();

        if (!(treeRenderer instanceof DefaultContactListTreeCellRenderer))
            return;

        DefaultContactListTreeCellRenderer renderer
            = (DefaultContactListTreeCellRenderer)treeRenderer;

        renderer.getTreeCellRendererComponent(
            this,
            mousePath.getLastPathComponent(),
            true,
            true,
            true,
            getRowForPath(mousePath),
            true);

        // We need to translate coordinates here.
        Rectangle r = getPathBounds(mousePath);
        int translatedX = event.getX() - r.x;
        int translatedY = event.getY() - r.y;

        Component mouseComponent
            = renderer.findComponentAt(translatedX, translatedY);

        if (mouseComponent != null)
            sLog.debug("Dispatch mouse event "
                    + event.getID()
                    + " to component: "
                    + mouseComponent.getClass().getName()
                    + " with bounds: " + mouseComponent.getBounds()
                    + " for x: " + translatedX
                    + " and y: " + translatedY);

        if (mouseComponent instanceof SIPCommButton)
        {
            MouseEvent evt = new MouseEvent(mouseComponent,
                                            event.getID(),
                                            event.getWhen(),
                                            event.getModifiers(),
                                            5, // we're in the button for sure
                                            5, // we're in the button for sure
                                            event.getClickCount(),
                                            event.isPopupTrigger());

            ((SIPCommButton) mouseComponent).getModel().setRollover(true);

            renderer.resetRolloverState(mouseComponent);

            renderer.setToolTip(((SIPCommButton) mouseComponent).getToolTipText());

            if (evt.getClickCount() == 1 &&
                evt.getID() == MouseEvent.MOUSE_RELEASED &&
                evt.getButton() == MouseEvent.BUTTON1)
            {
                // Single, released, left click - pass on to the button
                ((SIPCommButton) mouseComponent).doClick();
            }
            else
            {
                mouseComponent.dispatchEvent(evt);
            }
        }
        else
        {
            renderer.resetRolloverState();
            renderer.setToolTip(null);
        }

        repaint();
    }

    /**
     * Initializes key actions.
     */
    private void initKeyActions()
    {
        InputMap imap = getInputMap();
        ActionMap amap = getActionMap();

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "main-rename");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        imap.put(KeyStroke.getKeyStroke('+'), "openGroup");
        imap.put(KeyStroke.getKeyStroke('-'), "closeGroup");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "openGroup");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "closeGroup");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "right-click");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "right-click");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK), "change-tab");

        // Remove ctrl-tab from normal focus traversal
        Set<AWTKeyStroke> forwardKeys = new HashSet<>(getFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forwardKeys.remove(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK));
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardKeys);

        amap.put("main-rename", new RenameAction());

        amap.put("openGroup", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

            public void actionPerformed(ActionEvent e)
            {
                TreePath selectionPath = getSelectionPath();

                if (selectionPath != null
                    && selectionPath.getLastPathComponent()
                        instanceof GroupNode)
                {
                    GroupNode groupNode
                        = (GroupNode) selectionPath.getLastPathComponent();

                    expandGroup(groupNode);
                }
            }});

        amap.put("closeGroup", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

            public void actionPerformed(ActionEvent e)
            {
                TreePath selectionPath = getSelectionPath();

                if (selectionPath != null
                    && selectionPath.getLastPathComponent()
                        instanceof GroupNode)
                {
                    collapsePath(selectionPath);
                }
            }});

        amap.put("right-click", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                // For multi-selection models, space is used to select the
                // contact so fire an event
                if (getSelectionModel().getSelectionMode() ==
                    TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
                {
                    TreePath selectionPath = getSelectionPath();
                    if (selectionPath != null &&
                        selectionPath.getLastPathComponent() instanceof ContactNode)
                    {
                        fireSelectionChanged((ContactNode) selectionPath.getLastPathComponent());
                    }
                    repaint();
                }
                else
                {
                    UIContact selectedContact = getSelectedContact();
                    if (selectedContact != null)
                    {
                        mRightButtonMenu = selectedContact.
                            getRightButtonMenu(useReducedMenu);
                        if (mRightButtonMenu.isEnabled())
                        {
                            openRightButtonMenu(getSelectedCellLocation());
                        }
                    }
                }
            }
        });

        amap.put("change-tab", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                String selectedTab = ConfigurationUtils.getSelectedTab();
                if (selectedTab.equalsIgnoreCase(MainFrameTabComponent.FAVOURITES_TAB_NAME))
                {
                    GuiActivator.getUIService().getMainFrame().selectTab(MainFrameTabComponent.CONTACTS_TAB_NAME);
                }
                else if (selectedTab.equalsIgnoreCase(MainFrameTabComponent.CONTACTS_TAB_NAME))
                {
                    GuiActivator.getUIService().getMainFrame().selectTab(MainFrameTabComponent.HISTORY_TAB_NAME);
                }
                else if (selectedTab.equalsIgnoreCase(MainFrameTabComponent.HISTORY_TAB_NAME))
                {
                    GuiActivator.getUIService().getMainFrame().selectTab(MainFrameTabComponent.FAVOURITES_TAB_NAME);
                }
            }
        });
    }

    /**
     * Called when the ENTER key was typed when the contact list was selected.
     * Performs the appropriate actions depending on the config.
     */
    public void enterKeyTyped()
    {
        TreePath selectionPath = getSelectionPath();

        if (selectionPath != null)
        {
            Object lastPathComponent = selectionPath.getLastPathComponent();

            if (lastPathComponent instanceof ContactNode)
            {
                UIContact uiContact =
                    ((ContactNode)lastPathComponent).getContactDescriptor();

                if (uiContact instanceof ShowMoreContact)
                {
                    // The user has pressed 'enter' on a ShowMoreContact.
                    // Treat this as though the user had clicked the contact.
                    ContactListEvent event = new ContactListEvent(
                        uiContact, ContactListEvent.CONTACT_CLICKED, 1);
                    ((ShowMoreContact)uiContact).contactClicked(event);
                }
                else
                {
                    // Call or chat to the contact, as defined by config and the
                    // state of the contact.
                    boolean handledAsCall = false;
                    if (GuiActivator.enterDialsNumber())
                    {
                        List<UIContactDetail> contactDetails = uiContact.getContactDetailsForOperationSet(
                                OperationSetBasicTelephony.class);

                        if (!contactDetails.isEmpty())
                        {
                            // This contact can be called
                            Point location = getSelectedCellLocation();
                            SwingUtilities.convertPointToScreen(location, this);
                            startCall(uiContact, false, location);
                            handledAsCall = true;
                        }
                    }

                    if (!handledAsCall)
                    {
                        startChat(uiContact);
                    }
                }
            }
        }
    }

    /**
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when the
     * contact list was selected. Performs the appropriate actions depending
     * on the config.
     */
    public void ctrlEnterKeyTyped()
    {
        // Call or chat to the contact, as defined by config.
        if (GuiActivator.enterDialsNumber())
        {
            startSelectedContactChat();
        }
        else
        {
            Point location = getSelectedCellLocation();
            startCall(getSelectedContact(), false, location);
        }
    }

    /**
     * Gets the location on screen of the centre of the selected cell
     *
     * @return the location on screen of the centre of the selected cell
     */
    private Point getSelectedCellLocation()
    {
        Rectangle cellBounds = getPathBounds(getSelectionPath());
        return new Point(cellBounds.x + cellBounds.width/2,
                                   cellBounds.y + cellBounds.height/2);
    }

    /**
     * Starts a chat with the currently selected contact if any, otherwise
     * nothing happens. A chat is started with only <tt>MetaContact</tt>s for
     * now.
     */
    public void startSelectedContactChat()
    {
        TreePath selectionPath = getSelectionPath();

        if (selectionPath != null
            && selectionPath.getLastPathComponent() instanceof ContactNode)
        {
            UIContact uiContact
                = ((ContactNode) selectionPath.getLastPathComponent())
                    .getContactDescriptor();

            startChat(uiContact);
        }
    }

    /**
     * Starts a chat with the given UIContact.  A chat is started with only
     * <tt>MetaContact</tt>s for now.
     * @param uiContact the contact to start a chat session with
     */
    private void startChat(UIContact uiContact)
    {
        MetaContact metaContact = null;
        if (uiContact instanceof MetaUIContact)
        {
            metaContact = (MetaContact)uiContact.getDescriptor();
        }
        else if (uiContact instanceof SourceUIContact)
        {
            // Attempt to find a MetaContact from this UIContact
            metaContact = getMetaContactFromSource((SourceUIContact)uiContact);
        }

        if (metaContact != null && metaContact.canBeMessaged())
        {
            // Search for the right proto contact to use as default for the
            // chat conversation.
            Contact defaultContact = metaContact.getDefaultContact(
                                       OperationSetBasicInstantMessaging.class);

            // Only open the chat window if the contact supports IM.
            if (defaultContact != null)
            {
                GuiActivator.getUIService().getChatWindowManager()
                .startChat(metaContact);
            }
        }
    }

    /**
     * Calls the given uiContact. The number is reformatted to E.164.
     *
     * @param uiContact the contact to call
     * @param isVideo whether this is a video call
     * @param location the point on screen that initiated this call, used to
     *        display a popup in the correct place when there are multiple
     *        numbers
     */
    public void startCall(UIContact uiContact,
                          boolean isVideo,
                          Point location)
    {
        MetaContact metaContact = null;

        if (uiContact instanceof SourceUIContact)
        {
            // Attempt to find a MetaContact from this UIContact
            metaContact = getMetaContactFromSource(
                                          (SourceUIContact) uiContact);

            if (metaContact != null && uiContact.getContactDetails().size() == 1)
            {
                // If we have a MetaContact and only one UI Contact detail then
                // we should call by contact. Otherwise we should call by number
                // to prevent incorrect call history entries
                List<UIContactDetail> telephonyContacts =
                    uiContact.getContactDetailsForOperationSet(
                    CallManager.getOperationSetForCall(isVideo));

                String contactString = (telephonyContacts.size() < 1) ?
                                         null :
                                         telephonyContacts.get(0).getAddress();

                CallManager.call(metaContact, contactString, isVideo);
                return;
            }
        }

        // We didn't find a MetaContact or there we multiple numbers associated
        // with this UIContact (e.g. a multi-party call history entry), so just
        // call by number
        CallManager.call(uiContact,
                         isVideo,
                         this,
                         location);
    }

    /**
     * Returns a <tt>MetaContact</tt> associated with the given SourceUIContact
     *
     * @param contactDescriptor the SourceUIContact for which to find the
     * MetaContact.
     * @returns the MetaContact associated with this source contact, null if
     * not found.
     */
    public MetaContact getMetaContactFromSource(SourceUIContact contactDescriptor)
    {
        SourceContact contactSource = contactDescriptor.getSourceContact();
        @SuppressWarnings("unchecked")
        List<MetaContact> contacts = (List<MetaContact>)
                        contactSource.getData(SourceContact.DATA_META_CONTACTS);
        MetaContact metaContact = (contacts != null && contacts.size() == 1) ?
                                                         contacts.get(0) : null;

        return metaContact;
    }

    /**
     * Sets the given <tt>treeModel</tt> as a model of this tree. Specifies
     * also some related properties.
     * @param treeModel the <tt>TreeModel</tt> to set.
     */
    private void setTreeModel(TreeModel treeModel)
    {
        setModel(treeModel);
        setRowHeight(0);
        setToggleClickCount(1);
    }

    /**
     * Indicates that a node has been changed. Transfers the event to the
     * default tree model.
     * @param node the <tt>TreeNode</tt> that has been refreshed
     */
    public void nodeChanged(TreeNode node)
    {
        mTreeModel.nodeChanged(node);
    }

    /**
     * Initializes the list of available contact sources for this contact list.
     */
    private void initContactSources()
    {
        sLog.info("initContactSources");
        mContactSourceServiceListener = new ContactSourceServiceListener();
        GuiActivator.bundleContext.addServiceListener(
            mContactSourceServiceListener);

        for (ContactSourceService contactSource : GuiActivator.getContactSources())
        {
            if(!(contactSource instanceof AsyncContactSourceService)
                    || ((AsyncContactSourceService) contactSource)
                            .canBeUsedToSearchContacts())
            {
                sLog.info("Adding external contact source " +
                                                contactSource.getDisplayName());

                ExternalContactSource extContactSource
                    = new ExternalContactSource(contactSource, this);

                synchronized (mContactSources)
                {
                    mContactSources.put(contactSource, extContactSource);
                }
            }
        }
    }

    /**
     * Returns the list of registered contact sources to search in.
     * @return the list of registered contact sources to search in
     */
    public Collection<UIContactSource> getContactSources()
    {
        synchronized (mContactSources)
        {
            return new ArrayList<>(mContactSources.values());
        }
    }

    /**
     * Adds the given contact source to the list of available contact sources.
     *
     * @param contactSource the <tt>ContactSourceService</tt>
     */
    public void addContactSource(ContactSourceService contactSource)
    {
        if(!(contactSource instanceof AsyncContactSourceService)
                || ((AsyncContactSourceService) contactSource)
                        .canBeUsedToSearchContacts())
        {
            sLog.info("Adding contact source " + contactSource.getDisplayName());
            synchronized (mContactSources)
            {
                mContactSources.put(contactSource,
                                new ExternalContactSource(contactSource, this));
            }
        }
    }

    /**
     * Removes the given contact source from the list of available contact
     * sources.
     *
     * @param contactSource
     */
    public void removeContactSource(ContactSourceService contactSource)
    {
        synchronized (mContactSources)
        {
            sLog.info("Removing contact source " + contactSource.getDisplayName());
            mContactSources.remove(contactSource);
        }
    }

    /**
     * Removes all stored contact sources.
     */
    public void removeAllContactSources()
    {
        synchronized (mContactSources)
        {
            sLog.info("Removing all contact sources");
            mContactSources.clear();
        }
    }

    /**
     * Returns the <tt>ExternalContactSource</tt> corresponding to the given
     * <tt>ContactSourceService</tt>.
     * @param contactSource the <tt>ContactSourceService</tt>, which
     * corresponding external source implementation we're looking for
     * @return the <tt>ExternalContactSource</tt> corresponding to the given
     * <tt>ContactSourceService</tt>
     */
    public UIContactSource getContactSource(ContactSourceService contactSource)
    {
        synchronized (mContactSources)
        {
            return mContactSources.get(contactSource);
        }
    }

    /**
     * Returns all <tt>UIContactSource</tt>s of the given type.
     *
     * @param type the type of sources we're looking for
     * @return a list of all <tt>UIContactSource</tt>s of the given type
     */
    public List<UIContactSource> getContactSources(int type)
    {
        List<UIContactSource> sources = new ArrayList<>();

        synchronized (mContactSources)
        {
            for (Entry<ContactSourceService, UIContactSource> entry :
                                                      mContactSources.entrySet())
            {
                if (entry.getKey().getType() == type)
                {
                    sources.add(entry.getValue());
                }
            }
        }

        return sources;
    }

    /**
     * Searches for a source contact image for the given peer string in one of
     * the available contact sources.
     *
     * @param contactString the address of the contact to search an image for
     * @param label the label to set the image to
     * @param imgWidth the desired image width
     * @param imgHeight the desired image height
     */
    public void setSourceContactImage(String contactString,
                                      final JLabel label,
                                      final int imgWidth,
                                      final int imgHeight)
    {
        // Re-init the mImageSearchCanceled.
        mImageSearchCanceled = false;

        // We make a pattern that matches the whole string only.
        Pattern filterPattern = Pattern.compile(
            "^" + Pattern.quote(contactString) + "$", Pattern.UNICODE_CASE);

        Iterator<UIContactSource> mContactSources
            = getContactSources().iterator();

        final Vector<ContactQuery> loadedQueries = new Vector<>();

        while (mContactSources.hasNext())
        {
            // If the image search has been canceled from one of the previous
            // sources, we return here.
            if (mImageSearchCanceled)
                return;

            ContactSourceService contactSource
                = mContactSources.next().getContactSourceService();

            if (contactSource instanceof ExtendedContactSourceService)
            {
                ContactQuery query = ((ExtendedContactSourceService)
                        contactSource).queryContactSource(filterPattern);

                loadedQueries.add(query);

                query.addContactQueryListener(new ContactQueryListener()
                {
                    public void queryStatusChanged(ContactQueryStatusEvent event)
                    {}

                    public void contactReceived(ContactReceivedEvent event)
                    {
                        SourceContact sourceContact = event.getContact();

                        BufferedImageFuture image = sourceContact.getImage();

                        if (image != null)
                        {
                            setScaledLabelImage(
                                label, image, imgWidth, imgHeight);

                            // Cancel all already loaded queries.
                            cancelImageQueries(loadedQueries);

                            mImageSearchCanceled = true;
                        }
                    }

                    /**
                     * Indicates that a contact has been removed after a search.
                     * @param event the <tt>ContactQueryEvent</tt> containing
                     *              information
                     * about the received <tt>SourceContact</tt>
                     */
                    public void contactRemoved(ContactRemovedEvent event)
                    {}

                    /**
                     * Indicates that a contact has been updated after a search.
                     * @param event the <tt>ContactQueryEvent</tt> containing information
                     * about the updated <tt>SourceContact</tt>
                     */
                    public void contactChanged(ContactChangedEvent event)
                    {}
                });

                // If the image search has been canceled from one of the
                // previous sources, we return here.
                if (mImageSearchCanceled)
                    return;

                // Let's see if we find an image in the direct results.
                List<SourceContact> results = query.getQueryResults();

                if (results != null && !results.isEmpty())
                {
                    Iterator<SourceContact> resultsIter = results.iterator();

                    while (resultsIter.hasNext())
                    {
                        BufferedImageFuture image = resultsIter.next().getImage();

                        if (image != null)
                        {
                            setScaledLabelImage(
                                label, image, imgWidth, imgHeight);

                            // Cancel all already loaded queries.
                            cancelImageQueries(loadedQueries);

                            // As we found the image we return here.
                            return;
                        }
                    }
                }
            }
        }

        // If the image search has been canceled from one of the previous
        // sources, we return here.
        if (mImageSearchCanceled)
            return;

        // If we didn't find anything we would check and try to remove the @
        // sign if such exists.
        int atIndex = contactString.indexOf("@");

        // If we find that the contact is actually a number, we get rid of the
        // @ if it exists.
        if (atIndex >= 0
                && StringUtils.isNumber(contactString.substring(0, atIndex)))
        {
            setSourceContactImage(contactString.substring(0, atIndex),
                                  label, imgWidth, imgHeight);
        }
    }

    /**
     * Cancels the list of loaded <tt>ContactQuery</tt>s.
     *
     * @param loadedQueries the list of queries to cancel
     */
    private static void cancelImageQueries(
            Collection<ContactQuery> loadedQueries)
    {
        Iterator<ContactQuery> queriesIter = loadedQueries.iterator();

        while (queriesIter.hasNext())
            queriesIter.next().cancel();
    }

    /**
     * Sets the image of the incoming call notification.
     *
     * @param label the label to set the image to
     * @param image the image to set
     * @param width the desired image width
     * @param height the desired image height
     */
    private static void setScaledLabelImage(
        final JLabel label,
        final BufferedImageFuture image,
        final int width,
        final int height)
    {
        image.getScaledEllipticalIcon(width, height).addToLabel(label);
    }

    /**
     * Create an the add contact menu, taking into account the number of contact
     * details available in the given <tt>sourceContact</tt>.
     *
     * @param sourceContact the external source contact, for which we'd like
     * to create a menu
     * @return the add contact menu
     */
    public static JMenuItem createAddContactMenu(final SourceContact sourceContact)
    {
        JMenuItem addContactComponentTmp = null;
        final List<ContactDetail> details = sourceContact.getContactDetails();
        final String displayName = sourceContact.getDisplayName();
        int detailsCount = details.size();
        ManageContactService addContactService =
            GuiActivator.getManageContactService();

        // Only make one menu item if there is only one contact detail or
        // contacts that support multiple details are supported.
        if ((detailsCount == 1) ||
            ((addContactService != null) &&
             (addContactService.supportsMultiFieldContacts())))
        {
            ImageIconFuture icon = (ConfigurationUtils.isMenuIconsDisabled()) ?
                            null :
                                GuiActivator.getImageLoaderService()
                                .getImage(ImageLoaderService.ADD_CONTACT_16x16_ICON)
                                .getImageIcon();

            addContactComponentTmp = new ResMenuItem("service.gui.ADD_CONTACT");

            if (icon != null)
            {
                icon.addToMenuItem(addContactComponentTmp);
            }

            final ContactDetail[] detailArray = new ContactDetail[detailsCount];

            // 'Extra' details are those that cannot be used as addresses
            // (unlike ContactDetails, which must be usable addresses).  Load
            // any that are saved as data against the source contact.
            @SuppressWarnings("unchecked")
            final Map<String, String> extraDetails =
                (Map<String, String>)sourceContact.getData(
                    SourceContact.DATA_EXTRA_DETAILS);

            addContactComponentTmp.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (sourceContact.getContactSource().getType() ==
                                    ContactSourceService.MESSAGE_HISTORY_TYPE &&
                                    !displayName.contains("@"))
                    {
                        // This is a message history entry and the contact's address
                        // doesn't contain "@" so the address must be an SMS number.
                        sLog.debug("Opening SMS 'add contact' window");
                        TreeContactList.showAddSmsContactDialog(displayName);
                    }
                    else
                    {
                        sLog.debug("Opening 'add contact' window");
                        TreeContactList.showAddContactDialog(displayName,
                            extraDetails,
                            details.toArray(detailArray));
                    }
                }
            });
        }
        // If we have more than one details we would propose a separate menu
        // item for each one of them.
        else if (detailsCount > 1)
        {
            addContactComponentTmp
                = new JMenu(GuiActivator.getResources().getI18NString(
                    "service.gui.ADD_CONTACT"));

            Iterator<ContactDetail> detailsIter = details.iterator();

            while (detailsIter.hasNext())
            {
                final ContactDetail detail = detailsIter.next();

                JMenuItem addMenuItem
                    = new JMenuItem(detail.getDetail());
                ((JMenu) addContactComponentTmp).add(addMenuItem);

                addMenuItem.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        showAddContactDialog(displayName, null, detail);
                    }
                });
            }
        }

        return addContactComponentTmp;
    }

    /**
     * Creates and shows an <tt>AddContactDialog</tt> with a predefined
     * <tt>contactAddress</tt> and <tt>protocolProvider</tt>.
     *
     * @param displayName    the display name to be added
     * @param extraDetails   additional details (i.e. not <tt>ContactDetail</tt>s)
     * to be added
     * @param contactDetails the contact detail to be added
     * @implNote The {@link UIService#showAddUserWindow} method works in a
     * similar way, except it doesn't set the selected account for the adding
     * contact, which is used here.
     */
    public static void showAddContactDialog(String displayName,
                                            Map<String, String> extraDetails,
                                            ContactDetail... contactDetails)
    {
        ManageContactWindow addContactWindow =
            GuiActivator.getAddContactWindow(null);

        if (addContactWindow != null)
        {
            // Try to obtain a preferred provider.
            for (ContactDetail detail : contactDetails)
            {
                ProtocolProviderService preferredProvider = null;
                List<Class<? extends OperationSet>> opSetClasses
                    = detail.getSupportedOperationSets();

                if (opSetClasses != null && opSetClasses.size() > 0)
                {
                    preferredProvider
                        = detail.getPreferredProtocolProvider(
                            opSetClasses.get(0));
                }

                if (preferredProvider != null)
                {
                    // A provider has been found.  Use this for the add contact
                    // window and stop searching for providers.
                    addContactWindow.setSelectedAccount(preferredProvider);
                    break;
                }
            }

            addContactWindow.setContactName(displayName);
            addContactWindow.setContactDetails(contactDetails);
            addContactWindow.setContactExtraDetails(extraDetails);
            addContactWindow.setVisible(true);
        }
    }

    /**
     * Creates and shows an <tt>AddContactDialog</tt> with a predefined
     * phone number.
     * @param phoneNumber the phone number to be added
     */
    public static void showAddSmsContactDialog(String phoneNumber)
    {
        ManageContactWindow addContactWindow =
                                         GuiActivator.getAddContactWindow(null);

        if (addContactWindow != null)
        {
            addContactWindow.setContactAddress(phoneNumber);
            addContactWindow.setVisible(true);
        }
    }

    /**
     * Listens for adding and removing of <tt>ContactSourceService</tt>
     * implementations.
     */
    private class ContactSourceServiceListener
        implements ServiceListener
    {
        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference<?> serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want
            // to know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
                return;

            Object service = GuiActivator.bundleContext.getService(serviceRef);

            // we don't care if the source service is
            // not a contact source service
            if (!(service instanceof ContactSourceService))
                return;

            boolean changed = false;
            switch (event.getType())
            {
            case ServiceEvent.REGISTERED:
                if(!(service instanceof AsyncContactSourceService)
                        || ((AsyncContactSourceService) service)
                                .canBeUsedToSearchContacts())
                {
                    ExternalContactSource contactSource
                        = new ExternalContactSource(
                                (ContactSourceService) service,
                                TreeContactList.this);
                    sLog.info("Adding contact source " + service);
                    synchronized (mContactSources)
                    {
                        mContactSources.put((ContactSourceService) service,
                                            contactSource);
                    }
                    changed = true;
                }
                break;
            case ServiceEvent.UNREGISTERING:
                sLog.info("Removing contact source " + service);
                synchronized (mContactSources)
                {
                    mContactSources.remove(service);
                }
                changed = true;
                break;
            }

            // We don't need to refresh the filter if this is the presence
            // filter, because it doesn't depend on any contact sources.
            if (changed && !mCurrentFilter.equals(mPresenceFilter))
            {
                applyCurrentFilter();
            }
        }
    }

    /**
     * <tt>RenameAction</tt> is invoked when user presses the F2 key. Depending
     * on the selection opens the appropriate form for renaming.
     */
    private class RenameAction
        extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            Object selectedObject = getSelectedValue();

            if (selectedObject instanceof ContactNode)
            {
                UIContact uiContact
                    = ((ContactNode) selectedObject).getContactDescriptor();

                if (!(uiContact instanceof MetaUIContact))
                    return;

                MetaUIContact metaUIContact = (MetaUIContact) uiContact;

                RenameContactDialog dialog = new RenameContactDialog(
                        GuiActivator.getUIService().getMainFrame(),
                        (MetaContact) metaUIContact.getDescriptor());
                Dimension screenSize
                    = Toolkit.getDefaultToolkit().getScreenSize();

                dialog.setLocation(
                        screenSize.width/2 - 200,
                        screenSize.height/2 - 50);

                dialog.setVisible(true);

                dialog.requestFocusInFiled();
            }
            else if (selectedObject instanceof GroupNode)
            {
                UIGroup uiGroup
                    = ((GroupNode) selectedObject).getGroupDescriptor();

                if (!(uiGroup instanceof MetaUIGroup))
                    return;

                MetaUIGroup metaUIGroup = (MetaUIGroup) uiGroup;

                RenameGroupDialog dialog = new RenameGroupDialog(
                        GuiActivator.getUIService().getMainFrame(),
                        (MetaContactGroup) metaUIGroup.getDescriptor());

                Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
                dialog.setLocation(screenSize.width / 2 - 200,
                    screenSize.height / 2 - 50);

                dialog.setVisible(true);

                dialog.requestFocusInFiled();
            }
        }
    }

    public ContactListTreeModel getTreeModel()
    {
        return mTreeModel;
    }

    public MetaContactListSource getMetaContactListSource()
    {
        return mMclSource;
    }

    public Component getComponent()
    {
        return this;
    }

    /**
     * De-selects the given <tt>UIContact</tt> in the contact list.
     *
     * @param uiContact the contact to de-select
     */
    public void removeSelectedContact(final UIContact uiContact)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeSelectedContact(uiContact);
                }
            });
            return;
        }

        if (!(uiContact instanceof UIContactImpl))
            return;

        ContactNode contactNode = ((UIContactImpl) uiContact).getContactNode();
        TreePath[] selectionPaths = getSelectionPaths();

        if (selectionPaths == null || (selectionPaths.length == 0))
            return;

        for (TreePath selectionPath : selectionPaths)
        {
            Object lastPathComponent = selectionPath.getLastPathComponent();
            if (contactNode.equals(lastPathComponent))
            {
              // No need to hash UIContact, as its toString() method does that.
                sLog.debug("Found matching selection path <" + selectionPath +
                                     " > to remove for UIContact " + uiContact);
                removeSelectionPath(selectionPath);
            }
        }
    }

    /**
     * Selects the given <tt>UIContact</tt> in the contact list.
     *
     * @param uiContact the contact to select
     * @param addToSelection whether to add this selected node to the current
     * selection paths, or just to set it as the only selected node
     */
    public void setSelectedContact(final UIContact uiContact,
                                   final boolean addToSelection)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setSelectedContact(uiContact, addToSelection);
                }
            });
            return;
        }

        if (!(uiContact instanceof UIContactImpl))
            return;

        TreePath newSelectionPath = new TreePath(((UIContactImpl) uiContact).
                                            getContactNode().getPath());

        if (addToSelection)
        {
            // Add the selection to the current list of selection paths
            TreePath[] oldSelectionPaths = getSelectionPaths();
            TreePath[] newSelectionPaths;
            if (oldSelectionPaths == null)
            {
                // If nothing is selected then just set the new selection path
                newSelectionPaths = new TreePath[]{newSelectionPath};
            }
            else
            {
                newSelectionPaths = Arrays.copyOf(oldSelectionPaths, oldSelectionPaths.length + 1);
                newSelectionPaths[newSelectionPaths.length-1] = newSelectionPath;
            }

            setSelectionPaths(newSelectionPaths);
        }
        else
        {
            // Just set the selection path to the new selection
            setSelectionPath(newSelectionPath);
        }
    }

    /**
     * Selects the given <tt>UIGroup</tt> in the contact list.
     *
     * @param uiGroup the group to select
     */
    public void setSelectedGroup(final UIGroup uiGroup)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setSelectedGroup(uiGroup);
                }
            });
            return;
        }

        if (!(uiGroup instanceof UIGroupImpl))
            return;

        setSelectionPath(new TreePath(
            ((UIGroupImpl) uiGroup).getGroupNode().getPath()));
    }

    /**
     * Returns the currently selected <tt>UIContact</tt> if there's one.
     *
     * @return the currently selected <tt>UIContact</tt> if there's one.
     */
    public UIContact getSelectedContact()
    {
        TreePath selectionPath = getSelectionPath();

        if (selectionPath != null
            && selectionPath.getLastPathComponent() instanceof ContactNode)
        {
            return ((ContactNode) selectionPath.getLastPathComponent())
                    .getContactDescriptor();
        }

        return null;
    }

    /**
     * Returns the list of selected contacts.
     *
     * @return the list of selected contacts
     */
    public List<UIContact> getSelectedContacts()
    {
        TreePath[] selectionPaths = getSelectionPaths();

        if (selectionPaths == null)
            return null;

        List<UIContact> selectedContacts = new ArrayList<>();

        for (TreePath selectionPath : selectionPaths)
        {
            if (selectionPath.getLastPathComponent() instanceof ContactNode)
            {
                selectedContacts.add(
                    ((ContactNode) selectionPath.getLastPathComponent())
                        .getContactDescriptor());
            }
        }

        return selectedContacts;
    }

    /**
     * Returns the currently selected <tt>UIGroup</tt> if there's one.
     *
     * @return the currently selected <tt>UIGroup</tt> if there's one.
     */
    public UIGroup getSelectedGroup()
    {
        TreePath selectionPath = getSelectionPath();

        if (selectionPath != null
            && selectionPath.getLastPathComponent() instanceof GroupNode)
        {
            return ((GroupNode) selectionPath.getLastPathComponent())
                    .getGroupDescriptor();
        }

        return null;
    }

    /**
     * Enables/disables multiple selection.
     *
     * @param isEnabled <tt>true</tt> to enable multiple selection,
     * <tt>false</tt> - otherwise
     */
    public void setMultipleSelectionEnabled(boolean isEnabled)
    {
        if (isEnabled)
            getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        else
            getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    /**
     * Removes the current selection.
     */
    public void removeSelection()
    {
        TreePath[] selectionPaths = getSelectionPaths();

        if (selectionPaths != null)
            removeSelectionPaths(selectionPaths);
    }

    /**
     * Indicates that a selection has occurred on the tree.
     *
     * @param e the <tt>TreeSelectionEvent</tt> that notified us of the change
     */
    public void valueChanged(TreeSelectionEvent e)
    {
        UIGroup selectedGroup = getSelectedGroup();

        if (selectedGroup != null)
        {
            fireContactListEvent(
                selectedGroup, ContactListEvent.GROUP_SELECTED, 0);
        }
        else
        {
            UIContact selectedContact = getSelectedContact();
            if (selectedContact != null)
            {
                fireContactListEvent(
                    selectedContact, ContactListEvent.CONTACT_SELECTED, 0);
            }
        }
    }

    /**
     * Shows/hides buttons shown in contact row.
     *
     * @param isVisible <tt>true</tt> to show contact buttons, <tt>false</tt> -
     * otherwise.
     */
    public void setContactButtonsVisible(boolean isVisible)
    {
        mIsContactButtonsVisible = isVisible;
    }

    /**
     * Shows/hides buttons shown in contact row.
     *
     * return <tt>true</tt> to indicate that contact buttons are shown,
     * <tt>false</tt> - otherwise.
     */
    public boolean isContactButtonsVisible()
    {
        return mIsContactButtonsVisible;
    }

    /**
     * Enables/disables the right mouse click menu.
     *
     * @param isEnabled <tt>true</tt> to enable right button menu,
     * <tt>false</tt> otherwise.
     */
    public void setRightButtonMenuEnabled(boolean isEnabled)
    {
        mIsRightButtonMenuEnabled = isEnabled;
    }

    /**
     * Finds the cell component at the current mouse position
     *
     * @param point the current mouse position
     * @return cellComponent the cell component at the current mouse position
     */
    private Component findTreeCellComponent(Point point)
    {
        TreePath path = getClosestPathForLocation(point.x, point.y);

        if (path == null)
            return null;

        Object element = path.getLastPathComponent();

        TreeCellRenderer treeCellRenderer = getCellRenderer();

        if (!(treeCellRenderer instanceof DefaultContactListTreeCellRenderer))
            return null;

        DefaultContactListTreeCellRenderer renderer
            = (DefaultContactListTreeCellRenderer)treeCellRenderer;

        // We need to translate coordinates here.
        Rectangle r = getPathBounds(path);
        int translatedX = point.x - r.x;
        int translatedY = point.y - r.y;

        Component cellComponent =
            renderer.findComponentAt(translatedX, translatedY);

        // Get the currently selected contact node
        TreePath selectionPath = getSelectionPath();
        ContactNode selectedContactNode = null;
        if (selectionPath != null
            && selectionPath.getLastPathComponent() instanceof ContactNode)
        {
            selectedContactNode =
                          ((ContactNode) selectionPath.getLastPathComponent());
        }

        // If the mouse is over a button on an unselected cell then do not
        // show the button tool-tip
        boolean isSelectedContact = (selectedContactNode == element);
        if ((cellComponent instanceof SIPCommButton) && !isSelectedContact)
        {
            return null;
        }

        return cellComponent;
    }

    /**
     * Refreshes the contact list if the call history tab is selected
     */
    public void callHistoryChanged(HashMap<CallRecord, Boolean> transactions)
    {
        // If we're currently in the call history view, refresh it. Similarly,
        // if we're in the search view, refresh as we may need to update our
        // search results
        if (mCurrentFilter.equals(mCallHistoryFilter) ||
            mCurrentFilter.equals(mAllHistoryFilter) ||
            mCurrentFilter.equals(mSearchFilter))
        {
            applyFilter(mCurrentFilter);
        }
    }

    /**
     * Refreshes the contact list if the message history tab is selected
     */
    public void messageHistoryChanged()
    {
        // If we're currently in the message history view, refresh it. Similarly,
        // if we're in the search view, refresh as we may need to update our
        // search results
        if (mCurrentFilter.equals(mMessageHistoryFilter) ||
            mCurrentFilter.equals(mAllHistoryFilter) ||
            mCurrentFilter.equals(mSearchFilter))
        {
            applyFilter(mCurrentFilter);
        }
    }

    @Override
    public String getStateDumpName()
    {
        return "TreeContactList";
    }

    @Override
    public String getState()
    {
        StringBuilder state = new StringBuilder();
        GroupNode root = mTreeModel.getRoot();
        getGroupState(state, root);

        return state.toString();
    }

    /**
     * Gets the state of a group and appends it to the StringBuilder that has
     * been passed in
     *
     * @param state the builder to apply the state to
     * @param root The group to search for
     */
    @SuppressWarnings("rawtypes")
    private void getGroupState(StringBuilder state, GroupNode root)
    {
        Enumeration children = root.children();

        while (children.hasMoreElements())
        {
            Object child = children.nextElement();

            if (child instanceof ContactNode)
            {
                ContactNode contact = (ContactNode)child;
                UIContactImpl contactDescriptor = contact.getContactDescriptor();
                state.append(contactDescriptor);

                if (contactDescriptor instanceof MetaUIContact)
                {
                    // No need to hash MetaContact as its toString() method does that.
                    MetaUIContact metaUIContact = (MetaUIContact) contactDescriptor;
                    state.append(", ").append(metaUIContact.getMetaContact());
                }
                else
                {
                    state.append(", ").append(contactDescriptor.getClass());
                }
            }
            else if (child instanceof GroupNode)
            {
                GroupNode group = (GroupNode)child;
                state.append(group.getGroupDescriptor().getDisplayName());
                state.append("\n");
                getGroupState(state, group);
            }
            else
            {
                state.append(child.getClass());
            }

            state.append("\n");
        }
    }

    @Override
    public void sourceContactUpdated(final SourceContact sourceContact, final boolean isRefresh)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    sourceContactUpdated(sourceContact, isRefresh);
                }
            });
            return;
        }

        updateContactList(sourceContact, true, isRefresh);
    }

    @Override
    public void sourceContactAdded(final SourceContact sourceContact)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    sourceContactAdded(sourceContact);
                }
            });
            return;
        }

        updateContactList(sourceContact, false, false);
    }

    /**
     * Updates the contact list displayed in the UI when we are told that a
     * SourceContact has been added or updated.
     *
     * @param sourceContact The SourceContact
     * @param isUpdate Whether this is an update to an existing SourceContact
     * @param isRefresh If this is an update, whether the existing
     * SourceContact should be refreshed in its current place in the list or
     * moved to the top of the list
     */
    private void updateContactList(SourceContact sourceContact,
                                   boolean isUpdate,
                                   boolean isRefresh)
    {
        // Do nothing if we're not currently in the message history view or
        // search view.
        if (!(mCurrentFilter.equals(mMessageHistoryFilter) ||
            mCurrentFilter.equals(mAllHistoryFilter) ||
            mCurrentFilter.equals(mSearchFilter)))
        {
            return;
        }

        // First find the SourceContact's UIContactSource.
        ContactSourceService contactSource = sourceContact.getContactSource();
        UIContactSource sourceUI = getContactSource(contactSource);
        int index = 0;

        if (isUpdate)
        {
            // If this is an update to an existing SourceContact, get the old
            // UI contact and remove it from the contact list and from the
            // SourceContact.
            UIContact oldUIContact = sourceUI.getUIContact(sourceContact);

            if (oldUIContact != null)
            {
                if (isRefresh)
                {
                    ContactNode contactNode = ((UIContactImpl) oldUIContact).getContactNode();

                    if (contactNode != null)
                    {
                        int nodeIndex = contactNode.getParent().getIndex(contactNode);
                        if (nodeIndex != -1)
                        {
                            index = nodeIndex;
                        }
                    }
                    else
                    {
                        // No need to hash UIContact, as its toString() method
                        // does that.
                        contactLogger.warn(
                            "Unable to find contact node for UIContact: " +
                                                                 oldUIContact);
                    }
                }
                removeContact(oldUIContact, false);
            }
            else
            {
                // No need to hash SourceContact, as its toString() method
                // does that.
                contactLogger.warn(
                    "Unable to find UI contact for source contact: " +
                                                                sourceContact);
            }

            sourceUI.removeUIContact(sourceContact);
        }
        else
        {
            // Ensure we aren't showing the no messages panel
            GuiActivator.getUIService().getMainFrame().enableUnknownContactView(false);
        }

        // Create a new UI contact from the SourceContact and add it to the
        // contact list.
        UIContact newUIContact = sourceUI.createUIContact(sourceContact, mMclSource);
        // If we're in the search filter, contacts are displayed by their UI
        // group, otherwise they're just displayed in the root group.
        UIGroup uiGroup = mCurrentFilter.equals(mSearchFilter) ?
                                                   sourceUI.getUIGroup() : null;
        addContact(newUIContact, uiGroup, index, false);
    }

    @Override
    public void addSelectionListener(ContactListSelectionListener listener)
    {
        synchronized (mContactListListeners)
        {
            mSelectionListeners.add(listener);
        }
    }

    /**
     * Fires an event to indicate that a contact has been selected/unselected
     *
     * @param contactNode the contact node that this event concerns
     */
    private void fireSelectionChanged(ContactNode contactNode)
    {
        ArrayList<ContactListSelectionListener> listeners;
        synchronized (mSelectionListeners)
        {
            listeners = new ArrayList<>(mSelectionListeners);
        }
        for (ContactListSelectionListener listener : listeners)
        {
            listener.contactSelected(contactNode);
        }
    }

    /**
     * Make sure that the right click menu is created with a reduced form
     */
    public void useReducedMenu()
    {
        useReducedMenu = true;
    }
}
