/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ShowMoreContact
    extends UIContactImpl
    implements ContactListListener
{
    /**
     * The string associated with this contact.
     */
    private final String mShowMoreString
        = GuiActivator.getResources().getI18NString("service.gui.SHOW_MORE");

    /**
     * The contact list that the contacts are displayed in.
     */
    private final ContactList mContactList;

    /**
     * The parent group.
     */
    private UIGroup mParentGroup;

    /**
     * The contact node corresponding to this contact.
     */
    private ContactNode mContactNode;

    /**
     * The parent contact query, which added the contact.
     */
    private final ContactQuery mContactQuery;

    /**
     * The query results.
     */
    private final List<SourceContact> mQueryResults;

    /**
     * The count of shown contacts corresponding to the underlying query.
     */
    private int mShownResultsCount;

    /**
     * The maximum result count by show.
     */
    private int mMaxResultCount;

    /**
     * Creates an instance of <tt>MoreInfoContact</tt>.
     * @param contactList the contact list that the contacts are displayed in
     * @param contactQuery the contact query
     * @param queryResults the result list
     * @param maxResultCount the maximum result count
     */
    public ShowMoreContact(ContactList contactList,
                           ContactQuery contactQuery,
                           List<SourceContact> queryResults,
                           int maxResultCount)
    {
        mContactList = contactList;
        mContactQuery = contactQuery;
        mQueryResults = queryResults;
        mMaxResultCount = maxResultCount;

        // The contact list is already showing a number of results.
        mShownResultsCount = maxResultCount;

        contactList.addContactListListener(this);
    }

    /**
     * Returns the descriptor of this contact.
     *
     * @return the descriptor of this contact
     */
    public Object getDescriptor()
    {
        return mShowMoreString;
    }

    /**
     * Returns an empty string to indicate that this contact has no display
     * name.
     *
     * @return an empty string
     */
    public String getDisplayName()
    {
        return "";
    }

    /**
     * Returns null to indicate that there are no display details.
     *
     * @return null
     */
    public String getDisplayDetails()
    {
        return null;
    }

    /**
     * Returns Integer.MAX_VALUE to indicate that this contact should be placed
     * at the end of its parent group.
     *
     * @return Integer.MAX_VALUE
     */
    public int getSourceIndex()
    {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns null to indicate that this contact has no avatar.
     *
     * @param isExtended indicates if the avatar should be the extended size
     * @return null
     */
    public ImageIconFuture getAvatar(boolean isExtended)
    {
        return null;
    }

    /**
     * Returns null to indicate that this contact has no status icon.
     *
     * @return null
     */
    public ImageIconFuture getStatusIcon()
    {
        return null;
    }

    /**
     * Returns an extended tooltip for this contact.
     *
     * @return the created tooltip
     */
    public String getToolTip(String additionalText, String boldNumber)
    {
        String tooltip = GuiActivator.getResources()
                                .getI18NString("service.gui.SHOW_MORE_TOOLTIP");

        return tooltip;
    }

    /**
     * Returns null to indicate that this contact has no right button menu.
     *
     * @return null
     */
    public JPopupMenu getRightButtonMenu(boolean useReducedMenu)
    {
        return null;
    }

    /**
     * Returns the parent group of this contact.
     *
     * @return the parent group of this contact
     */
    public UIGroup getParentGroup()
    {
        return mParentGroup;
    }

    /**
     * Sets the parent group of this contact
     *
     * @param parentGroup the parent group of this contact
     */
    public void setParentGroup(UIGroup parentGroup)
    {
        mParentGroup = parentGroup;
    }

    /**
     * Returns null to indicate that this contact cannot be searched.
     *
     * @return null
     */
    public Iterator<String> getSearchStrings()
    {
        return null;
    }

    /**
     * Returns the corresponding contact node.
     *
     * @return the corresponding contact node
     */
    public ContactNode getContactNode()
    {
        return mContactNode;
    }

    /**
     * Sets the corresponding contact node.
     *
     * @param contactNode the contact node to set
     */
    public void setContactNode(ContactNode contactNode)
    {
        mContactNode = contactNode;

        // contactNode is null, when the ui contact is removed/cleared
        // we must free resources
        if (contactNode == null)
        {
            mContactList.removeContactListListener(this);
        }
    }

    /**
     * Returns null to indicate that this contact has no contact details.
     *
     * @param opSetClass the <tt>OperationSet</tt> class, which details we're
     * looking for
     * @return null
     */
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
    {
        return null;
    }

    /**
     * Returns null to indicate that this contact has no contact details.
     *
     * @return null
     */
    public List<UIContactDetail> getContactDetails()
    {
        return null;
    }

    /**
     * Returns null to indicate that this contact has no contact details.
     *
     * @param opSetClass the <tt>OperationSet</tt> class, which details we're
     * looking for
     * @return null
     */
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass)
    {
        return null;
    }

    /**
     * Indicates that a contact has been clicked in the contact list. Show some
     * more contacts after the "show more" has been clicked
     *
     * @param evt the <tt>ContactListEvent</tt> that notified us
     */
    public void contactClicked(ContactListEvent evt)
    {
        if (evt.getSourceContact().equals(this))
        {
            List<SourceContact> contacts
                = new ArrayList<>(mQueryResults);

            int newCount = mShownResultsCount + mMaxResultCount;

            int resultSize = contacts.size();

            int maxCount = (resultSize > newCount) ? newCount : resultSize;

            mContactList.removeContact(this);

            for (int i = mShownResultsCount; i < maxCount; i++)
            {
                mContactList.contactReceived(
                    new ContactReceivedEvent(mContactQuery, contacts.get(i)));
            }

            mShownResultsCount = maxCount;

            if (mShownResultsCount < resultSize
                || (mContactQuery.getStatus() != ContactQuery.QUERY_COMPLETED
                && mContactQuery.getStatus() != ContactQuery.QUERY_ERROR))
            {
                mContactList.addContact(
                    mContactQuery,
                    this,
                    mContactList.getContactSource(
                        mContactQuery.getContactSource()).getUIGroup(),
                    false);

                // The ContactListListener was removed when the ShowMoreContact
                // was removed from the contact list, so we need to add it
                // again.
                mContactList.addContactListListener(this);
            }
        }
    }

    public void groupClicked(ContactListEvent evt) {}

    /**
     * We're not interested in group selection events here.
     */
    public void groupSelected(ContactListEvent evt) {}

    /**
     * We're not interested in contact selection events here.
     */
    public void contactSelected(ContactListEvent evt) {}

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    public Collection<SIPCommButton> getContactCustomActionButtons()
    {
        return null;
    }
}
