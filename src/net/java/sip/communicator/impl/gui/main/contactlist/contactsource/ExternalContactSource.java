/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ExternalContactSource</tt> is the UI abstraction of the
 * <tt>ContactSourceService</tt>.
 *
 * @author Yana Stamcheva
 */
public class ExternalContactSource
    implements UIContactSource
{
    /**
     * The data key of the SourceContactDescriptor object used to store a
     * reference to this object in its corresponding Sourcecontact.
     */
    public static final String UI_CONTACT_DATA_KEY =
        "SourceUIContact.uiContactDescriptor";

    /**
     * The <tt>SourceUIGroup</tt> containing all contacts from this source.
     */
    private final SourceUIGroup mSourceUIGroup;

    /**
     * The contact source.
     */
    private final ContactSourceService mContactSource;

    /**
     * The current custom action contact.
     */
    private static SourceContact sCustomActionContact;

    /**
     * The list of action buttons for this source contact.
     */
    private static Map<ContactAction<SourceContact>, SIPCommButton>
                                    sCustomContactActionButtons;

    /**
     * The list of action buttons for this source service.
     */
    private Map<ContactAction<ContactSourceService>, SIPCommButton>
                                    mCustomServiceActionButtons;

    private final JTree mContactListTree;

    /**
     * Creates an <tt>ExternalContactSource</tt> based on the given
     * <tt>ContactSourceService</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt>, on which this
     * <tt>ExternalContactSource</tt> is based
     */
    public ExternalContactSource(ContactSourceService contactSource,
                                 JTree contactListTree)
    {
        mContactSource = contactSource;
        mContactListTree = contactListTree;

        mSourceUIGroup = new SourceUIGroup(contactSource.getDisplayName(), this);
    }

    /**
     * Returns the corresponding <tt>ContactSourceService</tt>.
     *
     * @return the corresponding <tt>ContactSourceService</tt>
     */
    public ContactSourceService getContactSourceService()
    {
        return mContactSource;
    }

    /**
     * Returns the UI group for this contact source. There's only one group
     * descriptor per external source.
     *
     * @return the group descriptor
     */
    public UIGroup getUIGroup()
    {
        return mSourceUIGroup;
    }

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>sourceContact</tt>.
     *
     * @param sourceContact the <tt>SourceContact</tt>, for which we search a
     * corresponding <tt>UIContact</tt>
     * @param mclSource the <tt>MetaContactListSource</tt>
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>sourceContact</tt>
     */
    public UIContact createUIContact(SourceContact sourceContact,
                                     MetaContactListSource mclSource)
    {
        SourceUIContact descriptor
            = new SourceUIContact(sourceContact, mSourceUIGroup, mclSource);

        sourceContact.setData(UI_CONTACT_DATA_KEY, descriptor);

        return descriptor;
    }

    /**
     * Removes the <tt>UIContact</tt> from the given <tt>sourceContact</tt>.
     * @param sourceContact the <tt>SourceContact</tt>, which corresponding UI
     * contact we would like to remove
     */
    public void removeUIContact(SourceContact sourceContact)
    {
        sourceContact.setData(UI_CONTACT_DATA_KEY, null);
    }

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>SourceContact</tt>.
     * @param sourceContact the <tt>SourceContact</tt>, which corresponding UI
     * contact we're looking for
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>MetaContact</tt>
     */
    public UIContact getUIContact(SourceContact sourceContact)
    {
        return (UIContact) sourceContact.getData(UI_CONTACT_DATA_KEY);
    }

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    public Collection<SIPCommButton> getContactCustomActionButtons(
        final SourceContact sourceContact)
    {
        sCustomActionContact = sourceContact;

        if (sCustomContactActionButtons == null)
            initCustomContactActionButtons();

        Iterator<ContactAction<SourceContact>> customActionsIter
            = sCustomContactActionButtons.keySet().iterator();

        Collection<SIPCommButton> availableCustomActionButtons
            = new LinkedList<>();

        while (customActionsIter.hasNext())
        {
            ContactAction<SourceContact> contactAction
                = customActionsIter.next();

            SIPCommButton actionButton =
                sCustomContactActionButtons.get(contactAction);

            if (isContactActionVisible(contactAction, sourceContact))
            {
                availableCustomActionButtons.add(actionButton);
            }
        }

        return availableCustomActionButtons;
    }

    /**
     * Indicates if the given <tt>ContactAction</tt> should be visible for the
     * given <tt>SourceContact</tt>.
     *
     * @param contactAction the <tt>ContactAction</tt> to verify
     * if the given action should be visible
     * @return <tt>true</tt> if the given <tt>ContactAction</tt> is visible for
     * the given <tt>SourceContact</tt>, <tt>false</tt> - otherwise
     */
    private static boolean isContactActionVisible(
                            ContactAction<SourceContact> contactAction,
                            SourceContact contact)
    {
        if (contactAction.isVisible(contact))
            return true;

        return false;
    }

    /**
     * Initializes custom action buttons for this contact source.
     */
    private void initCustomContactActionButtons()
    {
        sCustomContactActionButtons = new LinkedHashMap
                <>();

        for (CustomContactActionsService<SourceContact> ccas
                : getContactActionsServices())
        {
            Iterator<ContactAction<SourceContact>> actionIterator
                = ccas.getCustomContactActions();

            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<SourceContact> ca = actionIterator.next();

                initActionButton(ca, SourceContact.class);
            }
        }
    }

    /**
     * Initializes custom action buttons for this source service.
     */
    private void initCustomServiceActionButtons()
    {
        mCustomServiceActionButtons =
                new LinkedHashMap<>();

        for (CustomContactActionsService<ContactSourceService> ccas
                : getCustomActionsContactServices())
        {
            Iterator<ContactAction<ContactSourceService>> actionIterator
                = ccas.getCustomContactActions();

            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<ContactSourceService> ca = actionIterator.next();

                initActionButton(ca, ContactSourceService.class);
            }
        }
    }

    /**
     * Initializes an action button.
     * @param <T>
     *
     * @param ca the <tt>ContactAction</tt> corresponding to the button.
     */
    @SuppressWarnings ({ "unchecked", "rawtypes" })
    private <T> void initActionButton(final ContactAction ca,
                                      final Class<?> contactSourceClass)
    {
        SIPCommButton actionButton;

        if(contactSourceClass.equals(SourceContact.class))
            actionButton = sCustomContactActionButtons.get(ca);
        else if(contactSourceClass.equals(ContactSourceService.class))
            actionButton = mCustomServiceActionButtons.get(ca);
        else
            return;

        if (actionButton == null)
        {
            actionButton = new SIPCommButton();

            actionButton.setToolTipText(ca.getToolTipText());

            actionButton.setIconImage(ca.getIcon());
            actionButton.setRolloverIcon(ca.getRolloverIcon());
            actionButton.setPressedIcon(ca.getPressedIcon());

            actionButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    JButton button = (JButton)event.getSource();
                    Point location = new Point(button.getX(),
                        button.getY() + button.getHeight());

                    SwingUtilities.convertPointToScreen(
                        location, mContactListTree);

                    TreePath selectionPath
                        = mContactListTree.getSelectionPath();

                    if (selectionPath != null)
                        location.y = location.y
                            + mContactListTree.getPathBounds(
                                selectionPath).y;

                    if(contactSourceClass
                                .equals(SourceContact.class))
                        ca.actionPerformed(
                            sCustomActionContact,
                            location.x,
                            location.y);
                    else if(contactSourceClass
                                .equals(ContactSourceService.class))
                        ca.actionPerformed(
                            mContactSource,
                            location.x,
                            location.y);
                }
            });

            if(contactSourceClass.equals(SourceContact.class))
                sCustomContactActionButtons.put(ca, actionButton);
            else if(contactSourceClass.equals(ContactSourceService.class))
                mCustomServiceActionButtons.put(ca, actionButton);
        }
    }

    /**
     * Returns a list of all custom contact action services.
     *
     * @return a list of all custom contact action services.
     */
    @SuppressWarnings ("unchecked")
    private List<CustomContactActionsService<SourceContact>>
        getContactActionsServices()
    {
        List<CustomContactActionsService<SourceContact>>
            contactActionsServices
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
        {}

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                CustomContactActionsService<SourceContact> customActionService
                    = (CustomContactActionsService<SourceContact>)
                            GuiActivator.bundleContext.getService(serRef);

                if (customActionService.getContactSourceClass()
                        .equals(SourceContact.class))
                {
                    contactActionsServices.add(customActionService);
                }
            }
        }

        GuiActivator.bundleContext.addServiceListener(
            new ContactActionsServiceListener(SourceContact.class));

        return contactActionsServices;
    }

    /**
     * Returns a list of all custom contact action services.
     *
     * @return a list of all custom contact action services.
     */
    @SuppressWarnings ("unchecked")
    private List<CustomContactActionsService<ContactSourceService>>
        getCustomActionsContactServices()
    {
        List<CustomContactActionsService<ContactSourceService>>
            contactActionsServices
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
        {}

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                CustomContactActionsService<ContactSourceService>
                    customActionService
                        = (CustomContactActionsService<ContactSourceService>)
                            GuiActivator.bundleContext.getService(serRef);

                if (customActionService.getContactSourceClass()
                        .equals(ContactSourceService.class))
                {
                    contactActionsServices.add(customActionService);
                }
            }
        }

        GuiActivator.bundleContext.addServiceListener(
            new ContactActionsServiceListener(ContactSourceService.class));

        return contactActionsServices;
    }

    /**
     * The <tt>SourceUIGroup</tt> is the implementation of the UIGroup for the
     * <tt>ExternalContactSource</tt>. It takes the name of the source and
     * sets it as a group name.
     */
    public class SourceUIGroup
        extends UIGroupImpl
    {
        /**
         * The display name of the group.
         */
        private final String mDisplayName;

        /**
         * The corresponding group node.
         */
        private GroupNode mGroupNode;

        private ExternalContactSource mParentUISource;

        /**
         * Creates an instance of <tt>SourceUIGroup</tt>.
         * @param name the name of the group
         */
        public SourceUIGroup(String name,
                             ExternalContactSource parentUISource)
        {
            mDisplayName = name;
            mParentUISource = parentUISource;
        }

        public ExternalContactSource getParentUISource()
        {
            return mParentUISource;
        }

        /**
         * Returns null to indicate that this group doesn't have a parent group
         * and can be added directly to the root group.
         * @return null
         */
        public UIGroup getParentGroup()
        {
            return null;
        }

        /**
         * Returns -1 to indicate that this group doesn't have a source index.
         * @return -1
         */
        public int getSourceIndex()
        {
            int sourceIndex = mContactSource.getIndex();

            if (sourceIndex >= 0)
                return sourceIndex;

            int sourceType = mContactSource.getType();

            // The lower the index, the higher up in the search results a
            // contact source will appear.  We want call history at the bottom,
            // so it gets the maximum value, message history should appear
            // above call history, so it gets the maximum value - 1, everything
            // else appears above that so gets the maximum value - 2.
            if (sourceType == ContactSourceService.CALL_HISTORY_TYPE)
            {
                sourceIndex = Integer.MAX_VALUE;
            }
            else if (sourceType == ContactSourceService.MESSAGE_HISTORY_TYPE)
            {
                sourceIndex = Integer.MAX_VALUE - 1;
            }
            else
            {
                sourceIndex = Integer.MAX_VALUE - 2;
            }

            return sourceIndex;
        }

        /**
         * Returns <tt>false</tt> to indicate that this group is always opened.
         * @return false
         */
        public boolean isGroupCollapsed()
        {
            return false;
        }

        /**
         * Returns the display name of this group.
         * @return the display name of this group
         */
        public String getDisplayName()
        {
            return mDisplayName;
        }

        /**
         * Returns -1 to indicate that the child count is unknown.
         * @return -1
         */
        public int countChildContacts()
        {
            return -1;
        }

        /**
         * Returns -1 to indicate that the child count is unknown.
         * @return -1
         */
        public int countOnlineChildContacts()
        {
            return -1;
        }

        /**
         * Returns the display name of the group.
         * @return the display name of the group
         */
        public Object getDescriptor()
        {
            return mContactSource;
        }

        /**
         * Returns null to indicate that this group doesn't have an identifier.
         * @return null
         */
        public String getId()
        {
            return null;
        }

        /**
         * Returns the corresponding <tt>GroupNode</tt>.
         * @return the corresponding <tt>GroupNode</tt>
         */
        public GroupNode getGroupNode()
        {
            return mGroupNode;
        }

        /**
         * Sets the given <tt>groupNode</tt>.
         * @param groupNode the <tt>GroupNode</tt> to set
         */
        public void setGroupNode(GroupNode groupNode)
        {
            mGroupNode = groupNode;
        }

        /**
         * Returns the right button menu for this group.
         * @return null
         */
        public JPopupMenu getRightButtonMenu()
        {
            return null;
        }

        /**
         * Returns all custom action buttons for this group.
         *
         * @return a list of all custom action buttons for this group
         */
        public Collection<SIPCommButton> getCustomActionButtons()
        {
            if (mCustomServiceActionButtons == null)
                initCustomServiceActionButtons();

            Iterator<ContactAction<ContactSourceService>> customActionsIter
                = mCustomServiceActionButtons.keySet().iterator();

            Collection<SIPCommButton> availableCustomActionButtons
                = new LinkedList<>();

            while (customActionsIter.hasNext())
            {
                ContactAction<ContactSourceService> contactAction
                    = customActionsIter.next();

                SIPCommButton actionButton =
                    mCustomServiceActionButtons.get(contactAction);

                if (contactAction.isVisible(mContactSource))
                {
                    availableCustomActionButtons.add(actionButton);
                }
            }

            return availableCustomActionButtons;
        }
    }

    /**
     * The <tt>ContactActionsServiceListener</tt> listens for service changes
     * in order to update the list of custom action buttons when a new
     * <tt>CustomContactActionsService</tt> is registered or unregistered.
     */
    private class ContactActionsServiceListener
        implements ServiceListener
    {
        Class<?> mContactSourceClass;

        ContactActionsServiceListener(Class<?> contactSourceClass)
        {
            mContactSourceClass = contactSourceClass;
        }

        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference<?> serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want to
            // know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            {
                return;
            }

            Object service = GuiActivator.bundleContext.getService(serviceRef);

            // we don't care if the source service is not a protocol provider
            if (!(service instanceof CustomContactActionsService))
            {
                return;
            }

            @SuppressWarnings("unchecked")
            CustomContactActionsService<SourceContact> cContactActionsService
                = (CustomContactActionsService<SourceContact>) service;

            if(!cContactActionsService
                    .getContactSourceClass().equals(mContactSourceClass))
                return;

            Iterator<ContactAction<SourceContact>> actionIterator
                = cContactActionsService.getCustomContactActions();
            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<SourceContact> ca = actionIterator.next();

                switch (event.getType())
                {
                    case ServiceEvent.REGISTERED:
                        initActionButton(ca, mContactSourceClass);
                        break;
                    case ServiceEvent.UNREGISTERING:
                        if(mContactSourceClass.equals(SourceContact.class))
                            sCustomContactActionButtons.remove(ca);
                        else if(mContactSourceClass.equals(
                            ContactSourceService.class))
                            mCustomServiceActionButtons.remove(ca);
                        break;
                }
            }
        }
    }
}
