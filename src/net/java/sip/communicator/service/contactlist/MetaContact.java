/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.contactlist;

import java.util.*;

import net.java.sip.communicator.plugin.desktoputil.CreateConferenceMenu.CreateConferenceMenuContainer;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * A MetaContact is an abstraction used for merging multiple Contacts (most
 * often) belonging to different <tt>ProtocolProvider</tt>s.
 * <p>
 * Instances of a MetaContact are read-only objects that cannot be modified
 * directly but only through the corresponding MetaContactListService.
 * </p>
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public interface MetaContact extends CreateConferenceMenuContainer
{
    /**
     * Detail property for whether a contact is a favorite or not
     */
    String CONTACT_FAVORITE_PROPERTY = "contact_is_favorite";

    /**
     * The name of the detail that holds the time that a contact favorite
     * status was changed
     */
    String CONTACT_FAVORITE_TIME_PROPERTY = "contact_fav_time";

    /**
     * Detail property for whether a contact should be hidden in the UI
     */
    String IS_CONTACT_HIDDEN = "is.hidden";

    /**
     * Detail property for whether a contact typing or not
     */
    String IS_CONTACT_TYPING = "is.typing";

    /**
     * Detail property for whether a contact has an avatar
     */
    String HAS_AVATAR = "has.avatar";

    /**
     * Property used for store in persistent data indicating that this is a
     * contact representing our own SIP line.
     */
    String IS_OWN_LINE = "is.own.line";

    /**
     * Returns the default protocol specific <tt>Contact</tt> to use when
     * communicating with this <tt>MetaContact</tt>.
     * @return the default <tt>Contact</tt> to use when communicating with
     * this <tt>MetaContact</tt>
     */
    Contact getDefaultContact();

    /**
     * Returns the default protocol specific <tt>Contact</tt> to use with this
     * <tt>MetaContact</tt> for a precise operation (IM, call, ...).
     *
     * @param operationSet the operation for which the default contact is needed
     * @return the default contact for the specified operation.
     */
    Contact getDefaultContact(Class<? extends OperationSet> operationSet);

    /**
     * Returns a <tt>java.util.Iterator</tt> with all protocol specific
     * <tt>Contacts</tt> encapsulated by this <tt>MetaContact</tt>.
     * <p>
     * Note to implementors:  In order to prevent problems with concurrency, the
     * <tt>Iterator</tt> returned by this method should not be over the actual
     * list of contacts but rather over a copy of that list.
     * <p>
     * @return a <tt>java.util.Iterator</tt> containing all protocol specific
     * <tt>Contact</tt>s that were registered as subcontacts for this
     * <tt>MetaContact</tt>
     */
    Iterator<Contact> getContacts();

    /**
     * Returns a contact encapsulated by this meta contact, having the specified
     * contactAddress and coming from the indicated ownerProvider.
     * @param contactAddress the address of the contact who we're looking for.
     * @param ownerProvider a reference to the ProtocolProviderService that
     * the contact we're looking for belongs to.
     * @return a reference to a <tt>Contact</tt>, encapsulated by this
     * MetaContact, carrying the specified address and originating from the
     * specified ownerProvider or null if no such contact exists..
     */
    Contact getContact(String contactAddress,
                       ProtocolProviderService ownerProvider);

    /**
     * Returns <tt>true</tt> if the given <tt>protocolContact</tt> is contained
     * in this <tt>MetaContact</tt>, otherwise - returns <tt>false</tt>.
     * @param protocolContact the <tt>Contact</tt> we're looking for
     * @return <tt>true</tt> if the given <tt>protocolContact</tt> is contained
     * in this <tt>MetaContact</tt>, otherwise - returns <tt>false</tt>
     */
    boolean containsContact(Contact protocolContact);

    /**
     * Returns the number of protocol speciic <tt>Contact</tt>s that this
     * <tt>MetaContact</tt> contains.
     * @return an int indicating the number of protocol specific contacts merged
     * in this <tt>MetaContact</tt>
     */
    int getContactCount();

    /**
     * Returns all protocol specific Contacts, encapsulated by this MetaContact
     * and coming from the indicated ProtocolProviderService. If none of the
     * contacts encapsulated by this MetaContact is originating from the
     * specified provider then an empty iterator is returned.
     * <p>
     * Note to implementors:  In order to prevent problems with concurrency, the
     * <tt>Iterator</tt> returned by this method should not be over the actual
     * list of contacts but rather over a copy of that list.
     * <p>
     * @param provider a reference to the <tt>ProtocolProviderService</tt>
     * whose contacts we'd like to get.
     * @return an <tt>Iterator</tt> over all contacts encapsulated in this
     * <tt>MetaContact</tt> and originating from the specified provider.
     */
    Iterator<Contact> getContactsForProvider(
            ProtocolProviderService provider);

    /**
     * Returns all protocol specific Contacts, encapsulated by this MetaContact
     * and supporting the given <tt>opSetClass</tt>. If none of the
     * contacts encapsulated by this MetaContact is supporting the specified
     * <tt>OperationSet</tt> class then an empty list is returned.
     * <p>
     * Note to implementors:  In order to prevent problems with concurrency, the
     * <tt>List</tt> returned by this method should not be the actual list of
     * contacts but rather a copy of that list.
     * <p>
     * @param opSetClass the operation for which the default contact is needed
     * @return a <tt>List</tt> of all contacts encapsulated in this
     * <tt>MetaContact</tt> and supporting the specified <tt>OperationSet</tt>
     */
    List<Contact> getContactsForOperationSet(
            Class<? extends OperationSet> opSetClass);

    /**
     * Returns the MetaContactGroup currently containing this meta contact
     * @return a reference to the MetaContactGroup currently containing this
     * meta contact.
     */
    MetaContactGroup getParentMetaContactGroup();

    /**
     * Returns a list of all contacts that match the given phone number and
     * account ID.
     *
     * @param phoneNumber the phone number to search for
     * @return a list of contacts that match the given phone number
     */
    List<Contact> getContactByPhoneNumber(String phoneNumber);

    /**
     * Returns a list of all contacts that match the given email address
     *
     * @param emailAddress the address to search for
     * @return a list of contacts that match the given address
     */
    List<Contact> getContactByEmail(String emailAddress);

    /**
     * Returns a String identifier (the actual contents is left to
     * implementations) that uniquely represents this <tt>MetaContact</tt>
     * in the containing <tt>MetaContactList</tt>
     * @return String
     */
    String getMetaUID();

    /**
     * Returns a characteristic display name that can be used when including
     * this <tt>MetaContact</tt> in user interface.
     * @return a human readable String that represents this meta contact.
     */
    String getDisplayName();

    /**
     * Returns the first name for this contact, as defined in the stored contact
     * details. Personal contacts are preferred, then BG contacts, and if no
     * first name is available from either of those, we construct one from the
     * IM contact.
     *
     * @return the first name of this contact
     */
    String getFirstName();

    /**
     * Returns the last name for this contact, as defined in the stored contact
     * details. Personal contacts are preferred, then BG contacts, and if no
     * last name is available from either of those, we construct one from the
     * IM contact.
     *
     * @return the last name of this contact
     */
    String getLastName();

    /**
     * Returns the nick name for this contact, as defined in the stored contact
     * details.
     *
     * @return the nickname of this contact
     */
    String getNickname();

    /**
     * Returns the job title for this contact, as defined in the stored contact
     * details.
     *
     * @return the job title of this contact
     */
    String getJobTitle();

    /**
     * Returns the organisation for this contact, as defined in the stored
     * contact details.
     *
     * @return the organisation this contact works at
     */
    String getOrganisation();

    /** Returns the String value of any detail for this contact. */
    String getDetail(Class<? extends GenericDetail> detailType);

    /**
     * Returns the lines of the work address for this contact, as defined in the
     * stored contact details.
     *
     * @return the work address for this contact
     */
    List<String> getWorkAddress();

    /**
     * Returns the lines of the home address for this contact, as defined in the
     * stored contact details.
     *
     * @return the home address for this contact
     */
    List<String> getHomeAddress();

    /**
     * Returns the email addresses associated with this contact, as defined in
     * the stored contact details.
     *
     * @return the list of emails for this contact (currently two)
     */
    List<String> getEmails();

    /**
     * Returns the avatar of this contact, that can be used when including this
     * <tt>MetaContact</tt> in user interface.
     *
     * @return an avatar (e.g. user photo) of this contact.
     */
    BufferedImageFuture getAvatar();

    /**
     * Returns the avatar of this contact, that can be used when including this
     * <tt>MetaContact</tt> in user interface. The isLazy
     * parameter would tell the implementation if it could return the locally
     * stored avatar or it should obtain the avatar right from the server.
     *
     * @param isLazy Indicates if this method should return the locally stored
     * avatar or it should obtain the avatar right from the server.
     * @return an avatar (e.g. user photo) of this contact.
     */
    BufferedImageFuture getAvatar(boolean isLazy);

    /**
     * Returns the general status of the given MetaContact. Detects the
     * status using the priority status table. The priority is defined on
     * the "availability" factor and here the most "available" status is
     * returned.
     *
     * @return PresenceStatus The most "available" status from all
     * sub-contact statuses.
     */
    PresenceStatus getPresence();

    /**
     * Whether this contact is marked as a favourite.
     *
     * @return a boolean determining the favourite status of this contact
     */
    boolean isFavourite();

    /**
     * Whether this contact is typing.
     *
     * @return a boolean determining the typing status of this contact
     */
    boolean isTyping();

    /**
     * Get the custom status message for this MetaContact if it exists. Returns
     * null if no contact has a custom status.
     *
     * @return the custom status message
     */
    String getCustomStatus();

    /**
     * Get the IM address of the IM contact associated with this MetaContact. If
     * no IM contact is present, return null.
     *
     * @return the IM address of this MetaContact, if applicable
     */
    String getIMAddress();

    /**
     * Add an overlay icon to the avatar (if we have one)
     *
     * @param avatar The avatar to add the overlay to
     * @return that new avatar with the overlay added
     */
    ImageIconFuture addOverlayToAvatar(ImageIconFuture avatar);

    /**
     * Returns a String representation of this <tt>MetaContact</tt>.
     * @return a String representation of this <tt>MetaContact</tt>.
     */
    String toString();

    /**
     * Adds a custom detail to this contact.
     * @param name name of the detail.
     * @param value the value of the detail.
     */
    void addDetail(String name, String value);

    /**
     * Sets a custom detail to have a single value, removing all other values.
     * @param name name of the detail.
     * @param value the value of the detail.
     */
    void setDetail(String name, String value);

    /**
     * Gets a custom detail
     * @param name name of the detail.
     * @return the value of the detail, this is the first detail if there are
     *         multiple details and null if there are no details
     */
    String getDetail(String name);

    /**
     * Remove the given detail.
     * @param name of the detail to be removed.
     * @param value value of the detail to be removed.
     */
    void removeDetail(String name, String value);

    /**
     * Remove all details with given name.
     * @param name of the details to be removed.
     */
    void removeDetails(String name);

    /**
     * Change the detail.
     * @param name of the detail to be changed.
     * @param oldValue the old value of the detail.
     * @param newValue the new value of the detail.
     */
    void changeDetail(String name, String oldValue, String newValue);

    /**
     * Get all the details associated with this contact
     * @return a list of the names of the details stored by the contact
     */
    List<String> getAllDetails();

    /**
     * Get all details with given name.
     * @param name the name of the details we are searching.
     * @return list of string values for the details with the given name.
     */
    List<String> getDetails(String name);

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
     * Inform the meta contact that one of its proto contacts has changed
     *
     * This method means that the meta contact doesn't have to listen for proto
     * contact events on the MetaContactList.  Doing this could have large
     * performance implications as there can be many meta contacts
     *
     * @param contact The proto contact that has changed
     */
    void protoContactChanged(Contact contact);

    /**
     * Get the IM contact that is contained within this MetaContact, or null if
     * there is no such contact
     *
     * @return The IM contact within
     */
    Contact getIMContact();

    /**
     * Get the group contact that is contained within this MetaContact, or null
     * if there is no such contact
     *
     * @return The group contact within
     */
    Contact getGroupContact();

    /**
     * Get the personal contact that is contained within this MetaContact, or
     * null if there is no such contact
     *
     * @return the personal contact within
     */
    Contact getPersonalContact();

    /**
     * Get the BG contact that is contained within this MetaContact, or null if
     * there is no such contact
     *
     * @return the BG contact within
     */
    Contact getBGContact();

    /**
     * If it can match the given SMS number to a single <tt>Contact</tt>, this
     * method returns that <tt>Contact</tt>. Otherwise it returns null.
     *
     * @param smsNumber The SMS number to try to match to a <tt>Contact</tt>.
     * @return the matched <tt>Contact</tt>, or null if not exactly one
     *         <tt>Contact</tt> is found.
     */
    Contact getContactForSmsNumber(String smsNumber);

    /**
     * Get all the SMS numbers for this MetaContact
     *
     * @return a set of the retrieved SMS numbers
     */
    Set<String> getSmsNumbers();

    /**
     * Get all the phone number details for this MetaContact, which
     * will contain type information
     *
     * @return a set of the retrieved phone numbers
     */
    Set<GenericDetail> getPhoneNumberDetails();

    /**
     * Get all the phone numbers for this MetaContact
     *
     * @return a set of the retrieved phone numbers
     */
    Set<String> getPhoneNumbers();

    /**
     * Get the preferred phone number for this MetaContact
     *
     * @return the preferred phone number or null if none found
     */
    String getPreferredPhoneNumber();

    /**
     * Whether the user is permitted to chat via SMS or IM to this
     * <tt>MetaContact</tt>.
     *
     * @return <tt>true</tt> if the user should be able to start chats with this
     * <tt>MetaContact</tt>; <tt>false</tt> otherwise.
     */
    boolean canBeMessaged();

    /**
     * Whether the user is permitted to chat via IM to this
     * <tt>MetaContact</tt> - i.e. whether the <tt>MetaContact</tt> contains at
     * least one chat <tt>Contact</tt> which has accepted the user's buddy
     * request (or vice versa).
     *
     * @return <tt>true</tt> if the user should be able to start chats with this
     * <tt>MetaContact</tt>; <tt>false</tt> otherwise.
     */
    boolean isImCapable();

    /**
     * Whether the user is permitted to chat via Group IM to this
     * <tt>MetaContact</tt>.
     *
     * @return <tt>true</tt> if the user should be able to start group chats
     * with this <tt>MetaContact</tt>; <tt>false</tt> otherwise.
     */
    boolean isGroupImCapable();
}
