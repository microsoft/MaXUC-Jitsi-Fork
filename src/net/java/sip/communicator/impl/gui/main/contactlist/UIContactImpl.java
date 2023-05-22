/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>UIContactImpl</tt> class extends the <tt>UIContact</tt> in order to
 * add some more methods specific to the UI implementation.
 *
 * @author Yana Stamcheva
 */
public abstract class UIContactImpl
    extends UIContact
{
    /**
     * Returns the corresponding <tt>ContactNode</tt>. The <tt>ContactNode</tt>
     * is the real node that is stored in the contact list component data model.
     *
     * @return the corresponding <tt>ContactNode</tt>
     */
    public abstract ContactNode getContactNode();

    /**
     * Sets the given <tt>contactNode</tt>. The <tt>ContactNode</tt>
     * is the real node that is stored in the contact list component data model.
     *
     * @param contactNode the <tt>ContactNode</tt> that corresponds to this
     * <tt>UIGroup</tt>
     */
    public abstract void setContactNode(ContactNode contactNode);

    /**
     * Returns the general status icon of the given UIContact.
     *
     * @return PresenceStatus the most "available" status from all
     * sub-contact statuses.
     */
    public abstract ImageIconFuture getStatusIcon();

    /**
     * Gets the avatar of a specific <tt>UIContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     *
     * @param isExtended indicates if the avatar should be the extended size
     * @return an <tt>ImageIcon</tt> which represents the avatar of the
     * specified <tt>MetaContact</tt>
     */
    public abstract ImageIconFuture getAvatar(boolean isExtended);

    /**
     * Standard avatar height for UIContacts.
     */
    protected static final int sAvatarHeight = ScaleUtils.scaleInt(30);

    /**
     * Standard avatar width for UIContacts.
     */
    protected static final int sAvatarWidth = ScaleUtils.scaleInt(30);

    /**
     * Extended avatar height for UIContacts.
     */
    protected static final int sExtendedAvatarHeight = ScaleUtils.scaleInt(45);

    /**
     * Extended avatar width for UIContacts.
     */
    protected static final int sExtendedAvatarWidth = ScaleUtils.scaleInt(45);

    /**
     * Whether this contact is selected in the UI
     */
    private boolean mIsSelected = false;

    @Override
    public boolean canBeMessaged()
    {
        // We only support messaging of MetaContacts, so the UIContactImpl
        // implementation should always return 'false'.
        return false;
    }

    /**
     * Sets this contact as selected in the UI
     *
     * @param isSelected whether this contact is selected
     */
    public void setSelected(boolean isSelected)
    {
        mIsSelected = isSelected;
    }

    /**
     * Whether this contact is selected in the UI
     *
     * @return whether this contact is selected in the UI
     */
    public boolean isSelected()
    {
        return mIsSelected;
    }
}
