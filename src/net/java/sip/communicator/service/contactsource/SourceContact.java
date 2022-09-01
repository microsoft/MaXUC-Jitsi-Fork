/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>SourceContact</tt> is the result contact of a search in the
 * source. It should be identifier by a display name, an image if available
 * and a telephony string, which would allow to call this contact through the
 * preferred telephony provider defined in the <tt>ContactSourceService</tt>.
 *
 * @author Yana Stamcheva
 */
public interface SourceContact
{
    /**
     * Classes implementing SourceContact must override the toString method
     * and use LogHasher to hash any PII that needs to be returned.
     *
     * @return a string representation of the SourceContact with any PII hashed.
     */
    public String toString();

    /**
     * Key for extra details data.  An 'extra detail' is one that cannot be
     * used as an address by the client e.g. job title.  Details that can be
     * used as addresses should be saved on the <tt>SourceContact</tt> as
     * <tt>ContactDetail</tt>s.
     */
    String DATA_EXTRA_DETAILS = "ExtraDetails";

    /**
     * Key for MetaContact data. Holds the MetaContacts associated with this
     * source contact as a list.
     */
    String DATA_META_CONTACTS = "MetaContact";

    /**
     * Key for storing a MetaContact obtained from a contact query. This
     * differs from teh list of DATA_META_CONTACTS as this data key is liable
     * to change with each filter
     */
    String DATA_FILTER_META_CONTACT = "FilterMetaContact";

    /**
     * Key for whether the contact source is an anonymous contact.
     */
    String DATA_IS_ANONYMOUS= "IsAnonymous";

    /**
     * Key for whether this contact source should expose an add contact
     * function.
     */
    String DATA_DISABLE_ADD_CONTACT = "DisableAddContact";

    /**
     * Key for whether this contact source should expose an add contact
     * function.
     */
    String DATA_TYPE = "Type";

    /**
     * The type of this source contact
     */
    enum Type
    {
        CALL_HISTORY,
        IM_MESSAGE_HISTORY,
        SMS_MESSAGE_HISTORY,
        GROUP_MESSAGE_HISTORY
    }

    /**
     * Returns the display name of this search contact. This is a user-friendly
     * name that could be shown in the user interface.
     *
     * @return the display name of this search contact
     */
    String getDisplayName();

    /**
     * Sets the display name of this contact
     *
     * @param displayName the display name of this contact
     */
    void setDisplayName(String displayName);

    /**
     * The key that can be used to store <tt>SourceContact</tt> ids
     * where need it.
     */
    String DATA_ID = SourceContact.class.getName() + ".id";

    /**
     * Returns the parent <tt>ContactSourceService</tt> from which this contact
     * came from.
     * @return the parent <tt>ContactSourceService</tt> from which this contact
     * came from
     */
    ContactSourceService getContactSource();

    /**
     * Returns the display details of this search contact. This could be any
     * important information that should be shown to the user.
     *
     * @return the display details of the search contact
     */
    String getDisplayDetails();

    /**
     * Returns the details to be displayed in the tooltip of this search
     * contact. This could be any important information that should be shown
     * to the user.
     *
     * @return the tooltip display details of the search contact
     */
    String getTooltipDisplayDetails();

    /**
     * Returns a list of available contact details.
     * @return a list of available contact details
     */
    List<ContactDetail> getContactDetails();

    /**
     * Returns a list of all <tt>ContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class.
     * @param operationSet the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class
     */
    List<ContactDetail> getContactDetails(
            Class<? extends OperationSet> operationSet);

    /**
     * Returns a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category.
     * @param category the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category
     *
     * @throws OperationNotSupportedException if categories aren't supported
     * for call history records
     */
    List<ContactDetail> getContactDetails(
            ContactDetail.Category category)
        throws OperationNotSupportedException;

    /**
     * Returns the preferred <tt>ContactDetail</tt> for a given
     * <tt>OperationSet</tt> class.
     * @param operationSet the <tt>OperationSet</tt> class, for which we would
     * like to obtain a <tt>ContactDetail</tt>
     * @return the preferred <tt>ContactDetail</tt> for a given
     * <tt>OperationSet</tt> class
     */
    ContactDetail getPreferredContactDetail(
            Class<? extends OperationSet> operationSet);

    /**
     * An image (or avatar) corresponding to this search contact. If such is
     * not available this method will return null.
     *
     * @return the byte array of the image or null if no image is available
     */
    BufferedImageFuture getImage();

    /**
     * Gets the user data associated with this instance and a specific key.
     *
     * @param key the key of the user data associated with this instance to be
     * retrieved
     * @return an <tt>Object</tt> which represents the value associated with
     * this instance and the specified <tt>key</tt>; <tt>null</tt> if no
     * association with the specified <tt>key</tt> exists in this instance
     */
    Object getData(Object key);

    /**
     * Sets a user-specific association in this instance in the form of a
     * key-value pair. If the specified <tt>key</tt> is already associated in
     * this instance with a value, the existing value is overwritten with the
     * specified <tt>value</tt>.
     * <p>
     * The user-defined association created by this method and stored in this
     * instance is not serialized by this instance and is thus only meant for
     * runtime use.
     * </p>
     * <p>
     * The storage of the user data is implementation-specific and is thus not
     * guaranteed to be optimized for execution time and memory use.
     * </p>
     *
     * @param key the key to associate in this instance with the specified value
     * @param value the value to be associated in this instance with the
     * specified <tt>key</tt>
     */
    void setData(Object key, Object value);

    /**
     * Returns the status of the source contact. And null if such information
     * is not available.
     * @return the PresenceStatus representing the state of this source contact.
     */
    PresenceStatus getPresenceStatus();

    /**
     * Returns the index of this source contact in its parent.
     *
     * @return the index of this source contact in its parent
     */
    int getIndex();

    /**
     * Sets whether this contact is enabled or not
     *
     * @param enabled true to enable this contact
     */
    void setEnabled(boolean enabled);

    /**
     * Get the enable state of this contact
     *
     * @return true if the contact is enabled
     */
    boolean isEnabled();

    /**
     * Get the emphasized number for this contact. This number will be shown
     * differently in the tooltip to draw attention (e.g. for a call history
     * event for this number).
     *
     * @return the emphasized number
     */
    String getEmphasizedNumber();

    /**
     * Gets the timestamp associated with this contact, if any (e.g. call start
     * time or message sent time for history contacts).
     *
     * @return The timestamp associated with this contact, or null if there is
     * none.
     */
    Date getTimestamp();

    /**
     * Returns whether chat UI should be enabled for this contact.
     * @return whether chat UI should be enabled for this contact.
     */
    boolean canBeMessaged();

    /**
     * Gets a short string that describes the type of this source contact,
     * used for accessibility, and thus is the text equivalent of the
     * visual information conveyed by getImage.
     * @return the description of the image
     */
    String getImageDescription();
}
