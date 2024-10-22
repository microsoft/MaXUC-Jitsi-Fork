/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.contactlist;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.PersonalContactDetails.*;
import net.java.sip.communicator.util.*;

/**
 * A default implementation of the <code>MetaContact</code> interface.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class MetaContactImpl
    extends DataObject
    implements MetaContact
{
  /**
   * A property that determines which contact type the display name should be
   * taken from (if that type is present in the MetaContact's list of
   * contacts).
   */
    static final String PROPERTY_DEFAULT_CONTACT_PROTOCOL =
                             "net.java.sip.communicator.PERSONAL_CONTACT_STORE";

    /** The name of the protocol for BG contacts */
    private static final String BG_CONTACT_PROTOCOL_NAME = "BG_CommPortal";

    /**
     * Logger for <tt>MetaContactImpl</tt>.
     */
    private static final Logger logger
        = Logger.getLogger(MetaContactImpl.class);

    /**
     * Contact logger for this class
     */
    private static final ContactLogger contactLogger
        = ContactLogger.getLogger();

    /**
     * A vector containing all protocol specific contacts merged in this
     * MetaContact.
     */
    private final List<Contact> protoContacts = new Vector<>();

    /**
     * Map of capabilities of the meta contact - keys are Strings containing the name of OpSet classes,
     * values are Sets of protoContacts that have those OpSets as capabilities.
     */
    private final Map<String, Set<Contact>> capabilities = new HashMap<>();

    /**
     * Phone number utils object for this meta contact
     */
    private final PhoneNumberUtilsService utils
        = ContactlistActivator.getPhoneNumberUtils();

    /**
     * An id uniquely identifying the meta contact in this contact list.
     */
    private final String uid;

    /**
     * Returns a human readable string used by the UI to display the contact.
     */
    private String displayName = "";

    /**
     * The contact that should be chosen by default when communicating with this
     * meta contact.
     */
    private Contact defaultContact = null;

    /**
     * A locally cached copy of an avatar that we should return for lazy calls
     * to the getAvatarMethod() in order to speed up display.
     */
    private BufferedImageFuture cachedAvatar = null;

    /**
     * A flag that tells us whether or not we have already tried to restore
     * an avatar from cache. We need this to know whether a <tt>null</tt> cached
     * avatar implies that there is no locally stored avatar or that we simply
     * haven't tried to retrieve it. This should allow us to only interrogate
     * the file system if haven't done so before.
     */
    private boolean avatarFileCacheAlreadyQueried = false;

    /**
     * A callback to the meta contact group that is currently our parent. If
     * this is an orphan meta contact that has not yet been added or has been
     * removed from a group this callback is going to be null.
     */
    private MetaContactGroupImpl parentGroup = null;

    /**
     * Hashtable containing the contact details.
     * Name -> Value or Name -> (List of values).
     */
    private Map<String, List<String>> details;

    /**
     * Whether user has renamed this meta contact.
     */
    private boolean isDisplayNameUserDefined = false;

    /**
     * Which contact type the display name should be taken from (if that type
     * is present in the MetaContact's list of contacts).
     */
    private String defaultContactProtocol = "";

    /**
     * Creates new meta contact with a newly generated meta contact UID.
     */
    MetaContactImpl()
    {
        //create the uid
        this.uid = String.valueOf(System.currentTimeMillis())
                   + String.valueOf(hashCode());
        this.details = null;

        contactLogger.note(this, "New metacontact");
    }

    /**
     * Creates a new meta contact with the specified UID. This constructor
     * MUST ONLY be used when restoring contacts stored in the contactlist.xml.
     *
     * @param metaUID the meta uid that this meta contact should have.
     * @param details the already stored details for the contact.
     */
    MetaContactImpl(String metaUID, Map<String, List<String>> details)
    {
        this.uid = metaUID;
        this.details = details;

        contactLogger.note(this, "Loading metacontact from contactlist");
    }

    /**
     * Returns the number of protocol specific <tt>Contact</tt>s that this
     * <tt>MetaContact</tt> contains.
     *
     * @return an int indicating the number of protocol specific contacts
     *   merged in this <tt>MetaContact</tt>
     */
    public int getContactCount()
    {
        int numContacts;
        synchronized (getParentGroupModLock())
        {
            numContacts = protoContacts.size();
        }
        return numContacts;
    }

    /**
     * Returns a Contact, encapsulated by this MetaContact and coming from
     * the specified ProtocolProviderService.
     * <p>
     * In order to prevent problems with concurrency, the <tt>Iterator</tt>
     * returned by this method is not be over the actual list of contacts but
     * over a copy of that list.
     *
     * @param provider a reference to the <tt>ProtocolProviderService</tt>
     *   that we'd like to get a <tt>Contact</tt> for.
     * @return a <tt>Contact</tt> encapsulated in this <tt>MetaContact</tt>
     *   and originating from the specified provider.
     */
    public Iterator<Contact> getContactsForProvider(
                                    ProtocolProviderService provider)
    {
        LinkedList<Contact> providerContacts = new LinkedList<>();

        synchronized (getParentGroupModLock())
        {
            for (Contact contact : protoContacts)
            {
                if(contact.getProtocolProvider() == provider)
                    providerContacts.add( contact );
            }
        }

        return providerContacts.iterator();
    }

    /**
     * Returns all protocol specific Contacts, encapsulated by this MetaContact
     * and supporting the given <tt>opSetClass</tt>. If none of the
     * contacts encapsulated by this MetaContact is supporting the specified
     * <tt>OperationSet</tt> class then an empty iterator is returned.
     * <p>
     * @param opSetClass the operation for which the default contact is needed
     * @return a <tt>List</tt> over all contacts encapsulated in this
     * <tt>MetaContact</tt> and supporting the specified <tt>OperationSet</tt>
     */
    public List<Contact> getContactsForOperationSet(
                                    Class<? extends OperationSet> opSetClass)
    {
        LinkedList<Contact> opSetContacts = new LinkedList<>();

        synchronized (getParentGroupModLock())
        {
            for (Contact contact : protoContacts)
            {
                ProtocolProviderService contactProvider
                    = contact.getProtocolProvider();
                // First try to ask the capabilities operation set if such is
                // available.
                OperationSetContactCapabilities capOpSet
                    = contactProvider.getOperationSet(
                            OperationSetContactCapabilities.class);

                if (capOpSet != null)
                {
                    Set<Contact> capContacts = capabilities.get(opSetClass.getName());

                    if ((capContacts != null) && capContacts.contains(contact))
                    {
                        opSetContacts.add(contact);
                    }
                }
                else if (contactProvider.getOperationSet(opSetClass) != null)
                {
                    opSetContacts.add(contact);
                }
            }
        }

        return opSetContacts;
    }

    /**
     * Returns contacts, encapsulated by this MetaContact and belonging to
     * the specified protocol ContactGroup.
     * <p>
     * In order to prevent problems with concurrency, the <tt>Iterator</tt>
     * returned by this method is not be over the actual list of contacts but
     * over a copy of that list.
     *
     * @param parentProtoGroup a reference to the <tt>ContactGroup</tt>
     *   whose children we'd like removed..
     * @return an Iterator over all <tt>Contact</tt>s encapsulated in this
     * <tt>MetaContact</tt> and belonging to the specified proto ContactGroup.
     */
    public Iterator<Contact> getContactsForContactGroup(ContactGroup parentProtoGroup)
    {
        List<Contact> providerContacts = new LinkedList<>();

        synchronized (getParentGroupModLock())
        {
            for (Contact contact : protoContacts)
            {
                if (contact.getParentContactGroup() == parentProtoGroup)
                {
                    providerContacts.add(contact);
                }
            }
        }

        return providerContacts.iterator();
    }

    /**
     * Returns a contact encapsulated by this meta contact, having the specified
     * contactAddress and coming from the indicated ownerProvider.
     * <p>
     * @param contactAddress the address of the contact who we're looking for.
     * @param ownerProvider a reference to the ProtocolProviderService that
     * the contact we're looking for belongs to.
     * @return a reference to a <tt>Contact</tt>, encapsulated by this
     * MetaContact, carrying the specified address and originating from the
     * specified ownerProvider or null if no such contact exists..
     */
    public Contact getContact(String contactAddress,
                              ProtocolProviderService ownerProvider)
    {
        synchronized (getParentGroupModLock())
        {
            for (Contact contact : protoContacts)
            {
                if(contact.getProtocolProvider() == ownerProvider
                   && (contact.getAddress().equalsIgnoreCase(contactAddress)
                        || contact.equals(contactAddress)))
                    return contact;
            }
        }

        return null;
    }

    /**
     * Returns a contact encapsulated by this meta contact, having the specified
     * contactAddress and coming from a provider with a mathing
     * <tt>accountID</tt>. The method returns null if no such contact exists.
     * <p>
     * @param contactAddress the address of the contact who we're looking for.
     * @param accountID the identifier of the provider that the contact we're
     * looking for must belong to.
     * @return a reference to a <tt>Contact</tt>, encapsulated by this
     * MetaContact, carrying the specified address and originating from the
     * ownerProvider carryign <tt>accountID</tt>.
     */
    public Contact getContact(String contactAddress,
                              String accountID)
    {
        synchronized (getParentGroupModLock())
        {
            for (Contact contact : protoContacts)
            {
                if(  contact.getProtocolProvider().getAccountID()
                        .getAccountUniqueID().equalsIgnoreCase(accountID)
                   && contact.getAddress().equalsIgnoreCase(contactAddress))
                    return contact;
            }
        }

        return null;
    }

    public List<Contact> getContactByPhoneNumber(String phoneNumber)
    {
        // Convert the input number into E164 format - that way we can compare
        // two strings and say they represent the same number even if one is in
        // a different format to the other.
        String phoneNumberE164 = utils.formatNumberToE164(phoneNumber);

        ArrayList<Class<? extends GenericDetail>> detailClasses =
                new ArrayList<>();
        detailClasses.add(PhoneNumberDetail.class);

        if (utils.getEASRegion().equals("IT"))
        {
            // This is a workaround for Italian numbers which are saved without
            // ELC number prefixed, in which case we don't find a match by default.
            // For example if a contact is saved as 390324812345, and we search
            // by 0390324812345 (as that's how we receive it from SIP, with ELC
            // prefixed), we wouldn't have a match. So here we strip the ELC number
            // first and then format to E164 and try searching with that as well.
            String numberWithoutElc = utils.stripELC(phoneNumber);
            numberWithoutElc = utils.formatNumberToE164(numberWithoutElc);
            logger.info("Searching with ELC stripped: " + logHasher(numberWithoutElc));
            return getContactByValues(detailClasses, phoneNumberE164, numberWithoutElc);
        }

        return getContactByValues(detailClasses, phoneNumberE164);
    }

    @Override
    public List<Contact> getContactByEmail(String emailAddress)
    {
        ArrayList<Class<? extends GenericDetail>> detailClasses =
                new ArrayList<>();
        detailClasses.add(EmailAddressDetail.class);
        detailClasses.add(PersonalContactDetails.IMDetail.class);

        return getContactByValues(detailClasses, emailAddress);
    }

    /**
     * Return the contacts that match the input values
     *
     * @param values The values to search on
     * @param detailClasses The type of values that we are interested in
     * @return The matching contacts
     */
    private List<Contact> getContactByValues(
            List<Class<? extends GenericDetail>> detailClasses,
            String... values)
    {
        List<Contact> contactList = new LinkedList<>();

        synchronized (getParentGroupModLock())
        {
            for (Contact contact : protoContacts)
            {
                try
                {
                    ProtocolProviderService protoProvider =
                        contact.getProtocolProvider();

                    OperationSetServerStoredContactInfo contactInfoOpSet =
                        protoProvider.getOperationSet(
                            OperationSetServerStoredContactInfo.class);
                    if (contactInfoOpSet == null)
                    {
                        continue;
                    }

                    Iterator<GenericDetail> contactInfo =
                             contactInfoOpSet.getAllDetailsForContact(contact);

                    // Search through each server stored phone number detail for a
                    // match with userName
                    contactLoop:
                    while (contactInfo.hasNext())
                    {
                        GenericDetail detail = contactInfo.next();

                        for (Class<? extends GenericDetail> detailClass : detailClasses)
                        {
                            if (detailClass.isAssignableFrom(detail.getClass()))
                            {
                                String detailNumber =
                                    utils.formatNumberToE164(detail.toString());

                                for (String value : values)
                                {
                                    if (value.equalsIgnoreCase(detailNumber))
                                    {
                                        // Found a contact match, add this to the list of
                                        // contact matches
                                        contactList.add(contact);
                                        break contactLoop;
                                    }
                                }
                            }
                        }
                    }
                }
                catch(Exception e)
                {
                    logger.warn("Could not get server stored details for" +
                                         "contact" + logHasher(contact.getDisplayName()), e);
                }
            }
        }

        return contactList;
    }

    /**
     * Returns <tt>true</tt> if the given <tt>protocolContact</tt> is contained
     * in this <tt>MetaContact</tt>, otherwise - returns <tt>false</tt>.
     * @param protocolContact the <tt>Contact</tt> we're looking for
     * @return <tt>true</tt> if the given <tt>protocolContact</tt> is contained
     * in this <tt>MetaContact</tt>, otherwise - returns <tt>false</tt>
     */
    public boolean containsContact(Contact protocolContact)
    {
        boolean containsContact;
        synchronized (getParentGroupModLock())
        {
            containsContact = protoContacts.contains(protocolContact);
        }
        return containsContact;
    }

    /**
     * Returns a <tt>java.util.Iterator</tt> over all protocol specific
     * <tt>Contacts</tt> encapsulated by this <tt>MetaContact</tt>.
     * <p>
     * In order to prevent problems with concurrency, the <tt>Iterator</tt>
     * returned by this method is not over the actual list of contacts but over
     * a copy of that list.
     * <p>
     * @return a <tt>java.util.Iterator</tt> over all protocol specific
     * <tt>Contact</tt>s that were registered as subcontacts for this
     * <tt>MetaContact</tt>
     */
    public Iterator<Contact> getContacts()
    {
        LinkedList<Contact> contactsCopy;
        synchronized (getParentGroupModLock())
        {
            contactsCopy = new LinkedList<>(protoContacts);
        }
        return contactsCopy.iterator();
    }

    /**
     * Currently simply returns the most connected protocol contact. We should
     * add the possibility to choose it also according to preconfigured
     * preferences.
     *
     * @return the default <tt>Contact</tt> to use when communicating with
     *   this <tt>MetaContact</tt>
     */
    public Contact getDefaultContact()
    {
        if(defaultContact == null)
        {
            PresenceStatus currentStatus = null;

            synchronized (getParentGroupModLock())
            {
                for (Contact protoContact : protoContacts)
                {
                    PresenceStatus contactStatus = protoContact.getPresenceStatus();

                    if (currentStatus != null)
                    {
                        if (currentStatus.getStatus() < contactStatus.getStatus())
                        {
                            currentStatus = contactStatus;
                            defaultContact = protoContact;
                        }
                    }
                    else
                    {
                        currentStatus = contactStatus;
                        defaultContact = protoContact;
                    }
                }
            }
        }
        return defaultContact;
    }

    /**
     * Returns a default contact for a specific operation (call,
     * file transfer, IM ...)
     *
     * @param operationSet the operation for which the default contact is needed
     * @return the default contact for the specified operation.
     */
    public Contact getDefaultContact(Class<? extends OperationSet> operationSet)
    {
        Contact defaultOpSetContact = null;

        Contact defaultContact = getDefaultContact();

        // if the current default contact supports the requested operationSet
        // we use it
        if (defaultContact != null)
        {
            ProtocolProviderService contactProvider
                = defaultContact.getProtocolProvider();

            // First try to ask the capabilities operation set if such is
            // available.
            OperationSetContactCapabilities capOpSet = contactProvider
                .getOperationSet(OperationSetContactCapabilities.class);

            if (capOpSet != null)
            {
                Set<Contact> capContacts = capabilities.get(operationSet.getName());

                if (capContacts != null && capContacts.contains(defaultContact))
                {
                    defaultOpSetContact = defaultContact;
                }
            }
            else if (contactProvider.getOperationSet(operationSet) != null)
            {
                defaultOpSetContact = defaultContact;
            }
        }

        if (defaultOpSetContact == null)
        {
            PresenceStatus currentStatus = null;

            synchronized (getParentGroupModLock())
            {
                for (Contact protoContact : protoContacts)
                {
                    ProtocolProviderService contactProvider
                        = protoContact.getProtocolProvider();

                    // First try to ask the capabilities operation set if such is
                    // available.
                    OperationSetContactCapabilities capOpSet = contactProvider
                        .getOperationSet(OperationSetContactCapabilities.class);

                    // We filter to care only about contact which support
                    // the needed opset.
                    if (capOpSet != null)
                    {
                        Set<Contact> capContacts = capabilities.get(operationSet.getName());

                        if (capContacts == null
                                || !capContacts.contains(protoContact))
                        {
                            continue;
                        }
                    }
                    else if (contactProvider.getOperationSet(operationSet) == null)
                    {
                        continue;
                    }

                    PresenceStatus contactStatus = protoContact.getPresenceStatus();

                    if (currentStatus != null)
                    {
                        if (currentStatus.getStatus()
                                < contactStatus.getStatus())
                        {
                            currentStatus = contactStatus;
                            defaultOpSetContact = protoContact;
                        }
                    }
                    else
                    {
                        currentStatus = contactStatus;
                        defaultOpSetContact = protoContact;
                    }
                }
            }
        }
        return defaultOpSetContact;
    }

    /**
     * Returns a String identifier (the actual contents is left to
     * implementations) that uniquely represents this <tt>MetaContact</tt> in
     * the containing <tt>MetaContactList</tt>
     *
     * @return a String uniquely identifying this meta contact.
     */
    public String getMetaUID()
    {
        return uid;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details and any PII (currently just display name) hashed.
     *
     * @return  a string representation of this contact with hashed PII.
     */
    @Override
    public String toString()
    {
        StringBuffer buff = new StringBuffer("MetaContact[DisplayName=")
            .append(logHasher(displayName))
            .append(", UID="+getMetaUID())
            .append(", Hashcode="+hashCode())
            .append(", Favorite=" + getDetails(MetaContact.CONTACT_FAVORITE_PROPERTY)+"]");

        return buff.toString();
    }

    /**
     * Get a single detail (if it exists) from a contact wrapped by this
     * MetaContact. Refactored from AbstractContactDisplayer.
     *
     * @param detailType The type of detail that we are looking for
     * @param contact The contact to look through
     * @return The detail type that we've found
     */
    private String getDetailFromContact(
            Class<? extends GenericDetail> detailType, Contact contact)
    {
        String value = null;

        if (contact != null)
        {
            OperationSetServerStoredContactInfo opSet = contact
                    .getProtocolProvider()
                    .getOperationSet(OperationSetServerStoredContactInfo.class);

            Iterator<GenericDetail> details =
                    opSet.getDetails(contact, detailType);

            if (details != null && details.hasNext())
            {
                // Only use the first value found.
                Object detailValue = details.next().getDetailValue();

                if (detailValue != null)
                {
                    value = detailValue.toString();
                }
            }
        }

        return value;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getFirstName()
    {
        String firstName = getDetail(FirstNameDetail.class);

        if (firstName == null)
        {
            Contact imContact = getIMContact();
            if (imContact != null)
            {
                // If there is only a display name (only possible for an IM
                // buddy), return the string up to the first bit of whitespace.
                firstName = StringUtils.split(imContact.getDisplayName())[0];
            }
        }

        return firstName;
    }

    public String getLastName()
    {
        String lastName = getDetail(LastNameDetail.class);

        if (lastName == null)
        {
            Contact imContact = getIMContact();
            if (imContact != null)
            {
                // If there is only a display name (only possible for an IM
                // buddy), return everything after the first whitespace in the
                // name (may be nothing).
                String fullName = imContact.getDisplayName();
                String[] names = StringUtils.split(fullName);

                if (names.length > 1)
                {
                    lastName = StringUtils.removeStart(fullName, names[0]).trim();
                }
            }
        }

        return lastName;
    }

    public String getNickname()
    {
        return getDetail(NicknameDetail.class);
    }

    public String getJobTitle()
    {
        return getDetail(WorkTitleDetail.class);
    }

    public String getOrganisation()
    {
        return getDetail(WorkOrganizationNameDetail.class);
    }

    public List<String> getWorkAddress()
    {
        // Refactored from UserEditableContactDisplayer.
        return Arrays.asList(
                getDetail(WorkAddressDetail.class),
                getDetail(WorkCityDetail.class),
                getDetail(WorkProvinceDetail.class),
                getDetail(WorkPostalCodeDetail.class),
                getDetail(PersonalContactDetails.WorkCountryDetail.class)
        );
    }

    public List<String> getHomeAddress()
    {
        // Refactored from UserEditableContactDisplayer.
        return Arrays.asList(
                getDetail(AddressDetail.class),
                getDetail(CityDetail.class),
                getDetail(ProvinceDetail.class),
                getDetail(PostalCodeDetail.class),
                getDetail(PersonalContactDetails.CountryDetail.class)
        );
    }

    public List<String> getEmails()
    {
        // Refactored from UserEditableContactDisplayer.
        return Arrays.asList(
                getDetail(EmailAddress1Detail.class),
                getDetail(EmailAddress2Detail.class)
        );
    }

    @Override
    public String getMicrosoftEmail()
    {
        return getDetail(AADEmailAddressDetail.class);
    }

    /**
     * Gets a specific detail from this contact's stored contacts, so we may
     * expose it in top-level methods.
     *
     * @param detailType The class of the detail we want
     * @return A string represenation of that detail for this contact
     */
    @Override
    public String getDetail(Class<? extends GenericDetail> detailType)
    {
        String value;

        // First look at the personal contact
        value = getDetailFromContact(detailType, getPersonalContact());

        // Then, if we don't have a value, look at the BG contact:
        if (value == null)
        {
            value = getDetailFromContact(detailType, getBGContact());
        }

        return value;
    }

    /**
     * Determines if display name was changed for
     * this <tt>MetaContact</tt> in user interface.
     * @return whether display name was changed by user.
     */
    boolean isDisplayNameUserDefined()
    {
        return isDisplayNameUserDefined;
    }

    /**
     * Changes that display name was changed for
     * this <tt>MetaContact</tt> in user interface.
     * @param value control whether display name is user defined
     */
    void setDisplayNameUserDefined(boolean value)
    {
        this.isDisplayNameUserDefined = value;
    }

    /**
     * Queries a specific protocol <tt>Contact</tt> for its avatar. Beware that
     * this method could cause multiple network operations. Use with caution.
     *
     * @param contact the protocol <tt>Contact</tt> to query for its avatar
     * @return an array of <tt>byte</tt>s representing the avatar returned by
     * the specified <tt>Contact</tt> or <tt>null</tt> if the specified
     * <tt>Contact</tt> did not or failed to return an avatar
     */
    private BufferedImageFuture queryProtoContactAvatar(Contact contact)
    {
        try
        {
            BufferedImageFuture contactImage = contact.getImage();

            if (contactImage != null)
                return contactImage;
        }
        catch (Exception ex)
        {
            logger.error("Failed to get the photo of contact " + contact, ex);
        }
        return null;
    }

    public void clearCachedAvatar()
    {
        cachedAvatar = null;
        avatarFileCacheAlreadyQueried = false;
    }

    /**
     * Returns the avatar of this contact, that can be used when including this
     * <tt>MetaContact</tt> in user interface. The isLazy parameter would tell
     * the implementation if it could return the locally stored avatar or it
     * should obtain the avatar right from the server.
     *
     * @param isLazy Indicates if this method should return the locally stored
     * avatar or it should obtain the avatar right from the server.
     * @return an avatar (e.g. user photo) of this contact.
     */
    public BufferedImageFuture getAvatar(boolean isLazy)
    {
        BufferedImageFuture result = null;
        if (!isLazy)
        {
            // the caller is willing to perform a lengthy operation so let's
            // query the proto contacts for their avatars.
            Iterator<Contact> protoContacts = getContacts();

            while( protoContacts.hasNext())
            {
                Contact contact = protoContacts.next();
                result = queryProtoContactAvatar(contact);

                // if we got a result from the above, then let's cache and
                // return it.
                if (result != null)
                {
                    cacheAvatar(contact, result);
                    return result;
                }
            }
        }

        //if we get here then the caller is probably not willing to perform
        //network operations and opted for a lazy retrieve (... or the
        //queryAvatar method returned null because we are calling it too often)
        if (cachedAvatar != null)
        {
            //we already have a cached avatar, so let's return it
            return cachedAvatar;
        }

        //no cached avatar. let's try the file system for previously stored
        //ones. (unless we already did this)
        if (avatarFileCacheAlreadyQueried)
        {
            return null;
        }
        avatarFileCacheAlreadyQueried = true;

        Iterator<Contact> iter = this.getContacts();

        while (iter.hasNext())
        {
            Contact protoContact = iter.next();

            cachedAvatar = AvatarCacheUtils.getCachedAvatar(protoContact);
            /*
             * Caching a zero-length avatar happens but such an avatar isn't
             * very useful.
             */
            if (cachedAvatar != null)
            {
                return cachedAvatar;
            }
        }

        return null;
    }

    /**
     * Get the presence of a MetaContact by getting the most available of its
     * sub-contacts' presences. Refactored from MetaUIContact.
     *
     * @return the presence to display for this contact
     */
    public PresenceStatus getPresence()
    {
        PresenceStatus status = null;
        Iterator<Contact> i = getContacts();
        while (i.hasNext()) {
            Contact protoContact = i.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();

            if (!protoContact.getProtocolProvider().supportsStatus())
            {
                // Contact doesn't support status
                continue;
            }

            if (status == null)
                status = contactStatus;
            else
                status = (contactStatus.compareTo(status) > 0)
                        ? contactStatus
                        : status;
        }

        return status;
    }

    @Override
    public boolean isFavourite()
    {
        return Boolean.parseBoolean(
                getDetail(MetaContact.CONTACT_FAVORITE_PROPERTY));
    }

    @Override
    public boolean isTyping()
    {
        return Boolean.parseBoolean(
                getDetail(MetaContact.IS_CONTACT_TYPING));
    }

    @Override
    public String getCustomStatus()
    {
        // Refactored from MetaUIContact.

        String statusMessage = null;
        Iterator<Contact> protoContacts = getContacts();

        // Iterate through the underlying protocontacts, and extract the most
        // appropriate status message to be displayed for the parent.
        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();
            ProtocolProviderService protocolProvider =
                    protoContact.getProtocolProvider();

            OperationSetExtendedAuthorizations authOpSet =
                    protocolProvider.getOperationSet(
                            OperationSetExtendedAuthorizations.class);

            if (authOpSet != null
                && SubscriptionStatus.Subscribed !=
                   authOpSet.getSubscriptionStatus(protoContact))
            {
                // We aren't subscribed to this contact, so the custom status
                // should be displayed based on authorisation presence. See
                // getSubscriptionStatus().
                continue;
            }
            if (protoContact.getStatusMessage() != null
                     && protoContact.getStatusMessage().length() > 0)
            {
                statusMessage = protoContact.getStatusMessage();
                break;
            }
        }

        // A status will be of the form "PRESENCE - CUSTOM MESSAGE". This method
        // should only return "CUSTOM MESSAGE", so chop as needed.
        if (statusMessage != null)
        {
            String[] split = statusMessage.split(" - ", 2);
            statusMessage = split.length == 2 ? split[1] : null;
        }

        return statusMessage;
    }

    @Override
    public String getIMAddress()
    {
        return getIMContact() != null ? getIMContact().getAddress() : null;
    }

    /**
     * Returns an avatar that can be used when presenting this
     * <tt>MetaContact</tt> in user interface. The method would also make sure
     * that we try the network for new versions of avatars.
     *
     * @return an avatar (e.g. user photo) of this contact.
     */
    public BufferedImageFuture getAvatar()
    {
        return getAvatar(false);
    }

    public ImageIconFuture addOverlayToAvatar(ImageIconFuture avatarFuture)
    {
        ImageIconFuture result = avatarFuture;
        ImageIcon avatar = avatarFuture.resolve();

        if (avatar == null)
        {
            logger.warn("Failed to resolve avatar");
            return result;
        }

        // First get the overlay:
        ImageIcon overlay = null;

        Dimension size = new Dimension(avatar.getIconWidth(),
                                       avatar.getIconHeight());

        ArrayList<Contact> copyProtoContacts;
        synchronized (getParentGroupModLock())
        {
            copyProtoContacts = new ArrayList<>(protoContacts);
        }

        for (Contact contact : copyProtoContacts)
        {
            ImageIconFuture overlayFuture = contact.getOverlay(size);

            // Use the first valid overlay we find.
            if (overlayFuture != null)
            {
                overlay = overlayFuture.resolve();

                if (overlay != null)
                {
                    break;
                }
            }
        }

        // Then, if we've found one, use it
        if (overlay != null)
        {
            BufferedImage combined = new BufferedImage(size.width,
                                                       size.height,
                                                       BufferedImage.TYPE_INT_ARGB);

            Graphics g = combined.getGraphics();
            g.drawImage(avatar.getImage(), 0, 0, null);

            // Draw the overlay in the bottom right corner:
            int x = size.width  - overlay.getIconWidth();
            int y = size.height - overlay.getIconHeight();
            g.drawImage(overlay.getImage(), x, y, null);

            result =  new ImageIconAvailable(new ImageIcon(combined));
        }

        return result;
    }

    /**
     * Sets a name that can be used when displaying this contact in user
     * interface components.
     * @param displayName a human readable String representing this
     * <tt>MetaContact</tt>
     */
    void setDisplayName(String displayName)
    {
        synchronized (getParentGroupModLock())
        {
            if (parentGroup != null)
                parentGroup.lightRemoveMetaContact(this);

            this.displayName = (displayName == null) ? "" : displayName;

            if (parentGroup != null)
                parentGroup.lightAddMetaContact(this);
        }
    }

    /**
     * Adds the specified protocol specific contact to the list of contacts
     * merged in this meta contact. The method also keeps up to date the
     * contactsOnline field which is used in the compareTo() method.
     *
     * @param contact the protocol specific Contact to add.
     */
    void addProtoContact(Contact contact)
    {
        synchronized (getParentGroupModLock())
        {
            if (parentGroup != null)
                parentGroup.lightRemoveMetaContact(this);

            this.protoContacts.add(contact);

            // Re-init the default contact.
            defaultContact = null;

            // Get the default contact protocol from the config
            defaultContactProtocol = ContactlistActivator
                              .getConfigurationService().user()
                              .getString(PROPERTY_DEFAULT_CONTACT_PROTOCOL, "");

            // Update the display name if we need to
            maybeUpdateDisplayName();

            if (parentGroup != null)
                parentGroup.lightAddMetaContact(this);

            ProtocolProviderService contactProvider
                = contact.getProtocolProvider();

            // Check if the capabilities operation set is available for this
            // contact and add a listener to it in order to track capabilities'
            // changes for all contained protocol contacts.
            OperationSetContactCapabilities capOpSet
                = contactProvider
                    .getOperationSet(OperationSetContactCapabilities.class);

            if (capOpSet != null)
            {
                addCapabilities(contact,
                    capOpSet.getSupportedOperationSets(contact));
            }
        }
    }

    /**
     * Possibly update the display name of this meta contact.  Should be called
     * either when a ProtoContact is added, or when an existing ProtoContact is
     * modified
     */
    private void maybeUpdateDisplayName()
    {
        if (isDisplayNameUserDefined)
            // If the user set the display name, they don't want us to change it
            return;

        String newDisplayName = findDisplayNameFromContacts(getContacts());

        if ((newDisplayName != null) && (!newDisplayName.equals(displayName)))
        {
            contactLogger.debug(this, "Updating display name to " + logHasher(newDisplayName));
            fireMetaContactRenamed(displayName, newDisplayName);
            displayName = newDisplayName;
        }
    }

    /**
     * Selects the best display name from the given contacts that we should
     * use for this MetaContact.
     *
     * @param contacts iterator for the considered source contacts
     * @return the new display name, or <tt>null</tt> if no name is found
     */
    @VisibleForTesting
    String findDisplayNameFromContacts(Iterator<? extends Contact> contacts)
    {
        String defaultProtocol = defaultContactProtocol.trim();
        String newDisplayName = null;
        boolean currentDisplayNameIsIMName = false;

        /*
         * Description of the algorithm:
         *
         * 0) If there are no contacts, return null.
         * 1) If one of the contacts has a protocol which matches the user-selected default protocol
         *    e.g. CommPortal or address book, use that contact's display name (as long as it's non-empty).
         * 2) If there is no such contact, return the display name of the first non-IM contact we find.
         * 3) If there is no such contact, return the display name of the first contact we find.
         * We prefer non-IM contacts to IM contacts for choosing the display name because
         *   a) IM contact names are almost certainly added by a UCM mobile client auto-adding BG contacts
         *      as IM contacts, and thus most likely to be out-of-date (e.g. if the BG contact has since
         *      changed names)
         *   b) having stricter rules here (even if arbitrary) will make the behaviour more deterministic.
         *      choosing the first protocontact from an arbitrary list is not good.
         */
        while (contacts.hasNext())
        {
            // Look through each contact and see if we can use its display name
            Contact contact = contacts.next();

            if (contact.getProtocolProvider()
                       .getProtocolDisplayName()
                       .equalsIgnoreCase(defaultProtocol))
            {
                // Default contact, should use its display name:
                String contactDisplayName = contact.getDisplayName();
                if (contactDisplayName != null &&
                    contactDisplayName.trim().length() != 0)
                {
                    contactLogger.note(this, "Using display name from default contact");
                    newDisplayName = contactDisplayName;
                    break;
                }
            }
            else
            {
                // Not a default contact, use it if we not yet got a display name, or if the best
                // option for a display name we have so far is an IM name.
                if ((newDisplayName == null) || (currentDisplayNameIsIMName))
                {
                    contactLogger.note(this, "Getting display name from " + contact);
                    newDisplayName = contact.getDisplayName();
                    currentDisplayNameIsIMName = (contact.getProtocolProvider() instanceof ProtocolProviderServiceJabberImpl);
                }
            }
        }
        return newDisplayName;
    }

    /**
     * Called by MetaContactListServiceImpl after a contact has changed its
     * status, so that ordering in the parent group is updated. The method also
     * elects the most connected contact as default contact.
     *
     * @return the new index at which the contact was added.
     */
    int reevalContact()
    {
        synchronized (getParentGroupModLock())
        {
            //first lightremove or otherwise we won't be able to get hold of the
            //contact
            if (parentGroup != null)
            {
                parentGroup.lightRemoveMetaContact(this);
            }

            int maxContactStatus = 0;

            for (Contact contact : protoContacts)
            {
                int contactStatus = contact.getPresenceStatus()
                        .getStatus();

                if(maxContactStatus < contactStatus)
                {
                    maxContactStatus = contactStatus;
                    this.defaultContact = contact;
                }
            }
            //now read it and the contact would be automatically placed
            //properly by the containing group
            if (parentGroup != null)
            {
                return parentGroup.lightAddMetaContact(this);
            }
        }

        return -1;
    }

    /**
     * Removes the specified protocol specific contact from the contacts
     * encapsulated in this <code>MetaContact</code>. The method also updates
     * the total status field accordingly. And updates its ordered position
     * in its parent group. If the display name of this <code>MetaContact</code>
     * was the one of the removed contact, we update it.
     *
     * @param contact the contact to remove
     */
    void removeProtoContact(Contact contact)
    {
        synchronized (getParentGroupModLock())
        {
            if (parentGroup != null)
                parentGroup.lightRemoveMetaContact(this);
            this.protoContacts.remove(contact);

            if (defaultContact == contact)
                defaultContact = null;

            if ((protoContacts.size() > 0)
                    && displayName.equals(contact.getDisplayName()))
            {
                maybeUpdateDisplayName();
            }

            if (parentGroup != null)
                parentGroup.lightAddMetaContact(this);

            removeCapabilities(contact);

            // If we've removed a contact, then we may need to update the name
            maybeUpdateDisplayName();
        }
    }

    /**
     * Removes all proto contacts that belong to the specified ContactGroup.
     *
     * @param contactGroup the group whose children we want removed.
     */
    void removeContactsForGroup(ContactGroup contactGroup)
    {
        getContactsForContactGroup(contactGroup).forEachRemaining(contact -> removeProtoContact(contact));
    }

    /**
     * Sets <tt>parentGroup</tt> as a parent of this meta contact. Do not
     * call this method with a null argument even if a group is removing
     * this contact from itself as this could lead to race conditions (imagine
     * another group setting itself as the new parent and you removing it).
     * Use unsetParentGroup instead.
     *
     * @param parentGroup the <tt>MetaContactGroupImpl</tt> that is currently a
     * parent of this meta contact.
     * @throws NullPointerException if <tt>parentGroup</tt> is null.
     */
    void setParentGroup(MetaContactGroupImpl parentGroup)
    {
        if (parentGroup == null)
            throw new NullPointerException("Do not call this method with a "
                + "null argument even if a group is removing this contact "
                + "from itself as this could lead to race conditions "
                + "(imagine another group setting itself as the new "
                + "parent and you  removing it). Use unsetParentGroup "
                + "instead.");

        synchronized (getParentGroupModLock())
        {
            this.parentGroup = parentGroup;
        }
    }

    /**
     * If <tt>parentGroup</tt> was the parent of this meta contact then it
     * sets it to null. Call this method when removing this contact from a
     * meta contact group.
     * @param parentGrp the <tt>MetaContactGroupImpl</tt> that we don't want
     * considered as a parent of this contact any more.
     */
    void unsetParentGroup(MetaContactGroupImpl parentGrp)
    {
        synchronized(getParentGroupModLock())
        {
            if (parentGroup == parentGrp)
                parentGroup = null;
        }
    }

    /**
     * Returns the group that is currently holding this meta contact.
     *
     * @return the group that is currently holding this meta contact.
     */
    MetaContactGroupImpl getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Returns the MetaContactGroup currently containing this meta contact
     * @return a reference to the MetaContactGroup currently containing this
     * meta contact.
     */
    public MetaContactGroup getParentMetaContactGroup()
    {
        return getParentGroup();
    }

    /**
     * Adds a custom detail to this contact.
     * @param name name of the detail.
     * @param value the value of the detail.
     */
    public void addDetail(String name, String value)
    {
        if (details == null)
            details = new Hashtable<>();

        List<String> values = details.computeIfAbsent(name,
                                                      k -> new ArrayList<>());

        values.add(value);

        fireMetaContactModified(name, null, value);
    }

    /**
     * Sets a custom detail to have a single value, removing all other values,
     * and sends a MetaContactModified event.
     *
     * @param name name of the detail.
     * @param value the value of the detail.
     */
    public void setDetail(String name, String value)
    {
        if (details == null)
            details = new Hashtable<>();

        List<String> values = new ArrayList<>();
        values.add(value);
        details.put(name, values);

        fireMetaContactModified(name, null, value);
    }

    /**
     * Gets a custom detail
     * @param name name of the detail.
     * @return value the value of the detail, this is the first detail if there
     *              are multiple details and null if there are no details
     */
    public String getDetail(String name)
    {
        List<String> details = getDetails(name);
        return details.isEmpty() ? null : details.get(0);
    }

    /**
     * Change the detail.
     * @param name of the detail to be changed.
     * @param oldValue the old value of the detail.
     * @param newValue the new value of the detail.
     */
    public void changeDetail(String name, String oldValue, String newValue)
    {
        if (details == null)
            return;

        List<String> values = details.get(name);
        if(values == null)
            return;

        int changedIx = values.indexOf(oldValue);
        if(changedIx == -1)
            return;

        values.set(changedIx, newValue);

        fireMetaContactModified(name, oldValue, newValue);
    }

    /**
     * Fires a new <tt>MetaContactModifiedEvent</tt> which is to notify about a
     * modification with a specific name of this <tt>MetaContact</tt> which has
     * caused a property value change from a specific <tt>oldValue</tt> to a
     * specific <tt>newValue</tt>.
     *
     * @param modificationName the name of the modification which has caused
     * a new <tt>MetaContactModifiedEvent</tt> to be fired
     * @param oldValue the value of the property before the modification
     * @param newValue the value of the property after the modification
     */
    private void fireMetaContactModified(
            String modificationName,
            Object oldValue,
            Object newValue)
    {
        MetaContactGroupImpl parentGroup = getParentGroup();

        if (parentGroup != null)
        {
            MetaContactModifiedEvent evt =
                new MetaContactModifiedEvent(
                    this, modificationName, oldValue, newValue);

            parentGroup.getMclServiceImpl().fireMetaContactEvent(evt, null);
        }
    }

    /**
     * Fires a new <tt>MetaContactRenamedEvent</tt> which is to notify about renaming this
     * <tt>MetaContact</tt>.
     *
     * @param oldName the name of the contact before the modification
     * @param newName the name of the contact after the modification
     */
    private void fireMetaContactRenamed(String oldName, String newName)
    {
        MetaContactGroupImpl parentGroup = getParentGroup();

        if (parentGroup != null)
        {
            MetaContactRenamedEvent evt = new MetaContactRenamedEvent(this, oldName, newName);
            parentGroup.getMclServiceImpl().fireMetaContactEvent(evt, null);
        }
    }

    /**
     * Get all the details associated with this contact
     * @return a list of the names of the details stored by the contact
     */
    public List<String> getAllDetails()
    {
        Set<String> allDetails = (details == null) ? null : details.keySet();

        if (allDetails == null)
            return new ArrayList<>();
        else
            return new ArrayList<>(allDetails);
    }

    /**
     * Gets all details with a given name.
     *
     * @param name the name of the details we are searching for
     * @return a <tt>List</tt> of <tt>String</tt>s which represent the details
     * with the specified <tt>name</tt>
     */
    public List<String> getDetails(String name)
    {
        List<String> values = (details == null) ? null : details.get(name);

        if(values == null)
            values = new ArrayList<>();
        else
            values = new ArrayList<>(values);
        return values;
    }

    /**
     * Stores avatar bytes in the given <tt>Contact</tt>.
     *
     * @param protoContact The contact in which we store the avatar.
     * @param avatarBytes The avatar image bytes.
     */
    public void cacheAvatar( Contact protoContact,
        BufferedImageFuture avatarBytes)
    {
        this.cachedAvatar = avatarBytes;
        this.avatarFileCacheAlreadyQueried = true;

        AvatarCacheUtils.cacheAvatar(protoContact, avatarBytes);
    }

    /**
     * Updates the capabilities for the given contact.
     *
     * @param contact the <tt>Contact</tt>, which capabilities have changed
     * @param opSets the new updated set of operation sets
     */
    public void updateCapabilities(Contact contact,
                                   Map<String, ? extends OperationSet> opSets)
    {
        removeCapabilities(contact);
        addCapabilities(contact, opSets);
    }

    /**
     * Remove all capabilities for the given contact.
     *
     * @param contact the <tt>Contact</tt>, which capabilities we remove
     */
    private void removeCapabilities(Contact contact)
    {
        Iterator<Map.Entry<String, Set<Contact>>> mapIterator = capabilities.entrySet().iterator();

        while (mapIterator.hasNext())
        {
            Map.Entry<String, Set<Contact>> entry = mapIterator.next();

            Set<Contact> contactsForCap = entry.getValue();

            contactsForCap.remove(contact);

            if (contactsForCap.size() == 0)
            {
                mapIterator.remove();
            }
        }
    }

    /**
     * Adds the capabilities of the given contact.
     *
     * @param contact the <tt>Contact</tt>, which capabilities we add
     * @param opSets the map of operation sets supported by the contact
     */
    private void addCapabilities(Contact contact,
                                 Map<String, ? extends OperationSet> opSets)
    {
        Iterator<String> contactNewCaps = opSets.keySet().iterator();

        while (contactNewCaps.hasNext())
        {
            String newCap = contactNewCaps.next();

            Set<Contact> capContacts = null;

            if (!capabilities.containsKey(newCap))
            {
                capContacts = new HashSet<>();
                capContacts.add(contact);
                capabilities.put(newCap, capContacts);
            }
            else
            {
                capContacts = capabilities.get(newCap);

                if (!capContacts.contains(contact))
                {
                    capContacts.add(contact);
                }
            }
        }
    }

    /**
     * Gets the sync lock for use when modifying {@link #parentGroup}.
     *
     * @return the sync lock for use when modifying {@link #parentGroup}
     */
    private Object getParentGroupModLock()
    {
        /*
         * XXX The use of uid as parentGroupModLock is a bit unusual but a
         * dedicated lock enlarges the shallow runtime size of this instance and
         * having hundreds of MetaContactImpl instances is not unusual for a
         * multi-protocol application. With respect to parentGroupModLock being
         * unique among the MetaContactImpl instances, uid is fine because it is
         * also supposed to be unique in the same way.
         */
        return uid;
    }

    public void protoContactChanged(Contact contact)
    {
        contactLogger.debug(this, "proto contact changed");

        if (!containsContact(contact))
        {
            logger.warn("Informed of proto contact event of another metacontact");
            return;
        }

        synchronized (getParentGroupModLock())
        {
            maybeUpdateDisplayName();
        }
    }

    @Override
    public Contact getIMContact()
    {
        return getSingleContactForOpSet(OperationSetBasicInstantMessaging.class);
    }

    @Override
    public Contact getPersonalContact()
    {
        return getSingleContactForOpSet(OperationSetServerStoredUpdatableContactInfo.class);
    }

    /**
     * Find the _single_ contact that implements the operation set.  Note that
     * this contact might not exist, thus this method can validly return null.
     *
     * @param klass The operation set of the contact to look for
     * @return The contact which implements that operation set.
     */
    private Contact getSingleContactForOpSet(Class<? extends OperationSet> klass)
    {
        List<Contact> contacts = getContactsForOperationSet(klass);

        // There should only be at most 1 contact so just return the
        // first and warn if there are multiple
        if (contacts.size() > 1)
            contactLogger.error(this, "MetaContact has multiple contacts of type " + klass);

        return contacts.size() == 0 ? null : contacts.get(0);
    }

    /**
     * Get the BG contact that is contained within a MetaContact, or null if
     * there is no such contact
     *
     * @return the BG contact within
     */
    public Contact getBGContact()
    {
        Contact bgContact = null;

        Iterator<Contact> contacts = getContacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            if (contact.getProtocolProvider()
                       .getProtocolName()
                       .equals(BG_CONTACT_PROTOCOL_NAME))
            {
                // There should only be one BG contact so can safely return
                // this one.
                bgContact = contact;
                break;
            }
        }

        return bgContact;
    }

    @Override
    public Contact getContactForSmsNumber(String smsNumber)
    {
        Contact smsContact = null;

        List<Contact> contacts = getContactByPhoneNumber(smsNumber);
        if ((contacts != null) && (contacts.size() == 1))
        {
            smsContact = contacts.get(0);
        }
        return smsContact;
    }

    @Override
    public Set<String> getSmsNumbers()
    {
        Set<String> smsNumbers = new HashSet<>();
        Set<String> phoneNumbers = getPhoneNumbers();

        PhoneNumberUtilsService phoneNumberUtils =
                                     ContactlistActivator.getPhoneNumberUtils();

        for (String phoneNumber : phoneNumbers)
        {
            if (phoneNumberUtils.isValidSmsNumber(phoneNumber))
            {
                smsNumbers.add(phoneNumber);
            }
            else
            {
                contactLogger.debug("Ignoring invalid SMS number " + phoneNumber);
            }
        }

        return smsNumbers;
    }

    /**
     * TODO: this is a bit of a mess, but should do what we expect it to for now.
     *
     * Ideally, this method should return a list of all the known numbers for the
     * contact, of any type (e.g. work, mobile, etc.). We want to return a set
     * to ensure uniqueness of numbers within each type. E.g. we don't want to
     * have the same work number replicated twice, which would happen because
     * each BG contact will be replicated as a CP or Outlook contact with the
     * same work number if you ever edit them and save the changes (even if
     * you click save in the Java UI with no changes).
     *
     * However, there are some problems when doing that:
     *     1) GenericDetail overrides Object.equals() but *not* Object.hashCode() -
     * this means no hash-based data structure (e.g. HashSet) that compares two
     * GenericDetail objects will be able to differentiate them (and ensure uniqueness)
     * unless it's comparing the exact same object, and the custom equals() method
     * is useless. As GenericDetail is referenced hundreds of times in the codebase,
     * and the method below is quite new and isolated at the time of writing,
     * and we're writing a fix for the method in a hurry, we don't try to fix this larger problem.
     *
     * Note - a List (e.g. LinkedList) would be able to do the correct comparisons
     * using its contains method, which *will* just use the GenericDetail.equals() method, but:
     *     2) The GenericDetail class overrides Object.equals() but doesn't do
     * any comparison of *type* - it just compares the "display name" and "value"
     * of the objects. Unfortunately, in some cases we just rely on the object type to
     * distinguish between some types of numbers. E.g. HomePhoneDetail and
     * OtherPhoneDetail are both just PhoneNumberDetail (with the same "Phone" display name),
     * so if they have the same value (weird, but acceptable use-case), they will
     * be perceived as the same object, and filtered out. See 1) for a comment
     * on why we don't want to change GenericDetail.equals().
     *
     * To work around both issues, we use a HashMap that will use the type of
     * object as the key and a single PhoneNumberDetail as the value. This should:
     *     a) Help with the problem 1) since the hash comparison operations would
     * be done on the key - a String, which has well defined equals and hashCode methods.
     *     b) Ensure that if different types of number have the same value, they are
     * preserved.
     *     c) Ensure no duplicate numbers of the same type, since it would always
     * have at most 1 number of each type. Note - as of the time of writing, WISPA/Electron UI
     * support multiple numbers of each type. However, the rest of the Java codebase doesn't
     * support that anyway, and would need a much larger re-work to do so (in which we
     * expect the current method to be changed as well), so this should be fine.
     *
     * @return A set of the contact's numbers, with at most one number per type
     */
    @Override
    public Set<GenericDetail> getPhoneNumberDetails()
    {
        Map<String, GenericDetail> phoneNumbersWithTypes = new HashMap<>();

        Iterator<Contact> contacts = getContacts();
        while(contacts.hasNext())
        {
            Contact contact = contacts.next();

            ProtocolProviderService provider = contact.getProtocolProvider();
            OperationSetServerStoredContactInfo opSet =
                provider.getOperationSet(OperationSetServerStoredContactInfo.class);

            // If the provider doesn't support the server stored contact info
            // operation set, just continue as the contact won't have any stored
            // phone numbers.
            if (opSet == null)
            {
                continue;
            }

            // Get all of the contact's phone numbers
            Iterator<? extends GenericDetail> phoneDetails =
               opSet.getDetailsAndDescendants(contact, PhoneNumberDetail.class);

            while (phoneDetails.hasNext())
            {
                GenericDetail phoneDetail = phoneDetails.next();
                // Only add the detail if it has a value
                String phoneValue = (String)phoneDetail.getDetailValue();
                if (phoneValue != null && phoneValue.length() > 0)
                {
                    phoneNumbersWithTypes.put(phoneDetail.getClass().toString(), phoneDetail);
                }
            }
        }

        // Note that we can "safely" stick all values into a HashSet because
        // all of their hashes will be distinct, and they will never be confused
        // for one another. If we ever fix GenericDetail to override its hashCode
        // method, this could cause a problem - see issue 2) in javadoc, but
        // it would be no worse than what we used to have here before.
        return new HashSet<>(phoneNumbersWithTypes.values());
    }

    @Override
    public Set<String> getPhoneNumbers()
    {
        // No logging here as it would be spammy
        Set<String> phoneNumbers = new HashSet<>();

        // Get phone numbers with their types
        Set<GenericDetail> phoneDetails = getPhoneNumberDetails();

        PhoneNumberUtilsService phoneNumberUtils =
                                    ContactlistActivator.getPhoneNumberUtils();

        for (GenericDetail phoneDetail : phoneDetails)
        {
            // getPhoneNumberDetails guarantees each phoneDetail is not
            // null/empty. so no need to check here.
            String phoneNumber = (String)phoneDetail.getDetailValue();
            String formattedNumber = phoneNumberUtils.formatNumberToLocalOrE164(phoneNumber);
            phoneNumbers.add(formattedNumber);
        }

        return phoneNumbers;
    }

    @Override
    public String getPreferredPhoneNumber()
    {
        Map<String, String> phoneNumberPref = getPhoneNumberOrderPref();

        Iterator<Contact> contacts = getContacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            if (contact == null)
            {
                continue;
            }

            ProtocolProviderService provider = contact.getProtocolProvider();
            OperationSetServerStoredContactInfo opSet =
                provider.getOperationSet(OperationSetServerStoredContactInfo.class);

            // If the provider doesn't support the server stored contact info
            // operation set, just continue as the contact won't have any stored
            // phone numbers.
            if (opSet == null)
            {
                continue;
            }

            // Get all the contact details
            Iterator<? extends GenericDetail> phoneDetails =
                   opSet.getDetailsAndDescendants(contact, PhoneNumberDetail.class);

            while (phoneDetails.hasNext())
            {
                GenericDetail phoneDetail = phoneDetails.next();
                String phoneNumber = (String)phoneDetail.getDetailValue();

                String detailClassName = phoneDetail.getClass().getName();

                // Only interested in a detail if it has a phone number and the
                // detail type is in our preferred types
                if (phoneNumber != null &&
                    phoneNumber.length() > 0 &&
                    phoneNumberPref.containsKey(detailClassName) &&
                    phoneNumberPref.get(detailClassName).equals(""))
                {
                    phoneNumberPref.put(detailClassName, phoneNumber);
                }
            }
        }

        String preferred = null;

        for (String phoneNumber : phoneNumberPref.values())
        {
            if (phoneNumber != null && !phoneNumber.isEmpty())
            {
                preferred = phoneNumber;
                break;
            }
        }

        return preferred;
    }

    /**
     * Hard coded phone number type preference
     * @return Ordered phone number type preference
     */
    private Map<String, String> getPhoneNumberOrderPref()
    {
        Map<String, String> phoneNumberPref =
                new LinkedHashMap<>();

        phoneNumberPref.put(
                       ServerStoredDetails.WorkPhoneDetail.class.getName(), "");
        phoneNumberPref.put(
                 ServerStoredDetails.WorkMobilePhoneDetail.class.getName(), "");
        phoneNumberPref.put(
                     ServerStoredDetails.MobilePhoneDetail.class.getName(), "");
        phoneNumberPref.put(
                    PersonalContactDetails.HomePhoneDetail.class.getName(), "");
        phoneNumberPref.put(
                   PersonalContactDetails.OtherPhoneDetail.class.getName(), "");

        return phoneNumberPref;
    }

    @Override
    public boolean canBeMessaged()
    {
        // A contact can be messaged if they are either SMS, IM or group IM
        // capable.
        boolean canBeMessaged = (isGroupImCapable() || isSmsCapable() || isImCapable());

        logger.interval("CanBeMessaged:" + uid,
                        "Can MetaContact be messaged?",
                        "canBeMessaged:" + canBeMessaged);

        return canBeMessaged;
    }

    /**
     * Returns true if contact has associated sms numbers.  Note that SMS
     * needs to be enabled for the method to return true
     *
     * @return True if contact is SMS capable, false otherwise.
     */
    private boolean isSmsCapable()
    {
        // If SMS is enabled and the contact has at least one valid SMS number,
        // they are SMS capable.
        boolean isSmsCapable = false;

        if (ConfigurationUtils.isSmsEnabled())
        {
            Set<String> smsNumbers = getSmsNumbers();
            if(smsNumbers.size() > 0)
            {
                isSmsCapable = true;
            }
        }

        return isSmsCapable;
    }

    @Override
    public boolean isImCapable()
    {
        boolean isImCapable = false;

        OperationSetBasicInstantMessaging imOpSet = null;
        Contact imContact = getIMContact();
        if (imContact != null &&
            ConfigurationUtils.isImEnabled())
        {
            // We can message a MetaContact with an IM contact if the IM op set
            // says that the IM contact can be messaged.
            imOpSet = imContact.getProtocolProvider().getOperationSet(
                                       OperationSetBasicInstantMessaging.class);

            if (imOpSet != null)
            {
                isImCapable = imOpSet.isContactImCapable(imContact);
            }
        }

        logger.interval("isImCapable:" + uid,
                        "Is MetaContact IM capable?",
                        "imContact:" + imContact,
                        "imOpSet:" + imOpSet,
                        "isImCapable:" + isImCapable);

        return isImCapable;
    }

    @Override
    public boolean isGroupImCapable()
    {
        boolean isGroupImCapable = false;

        // To use group IM we need multi-user chat to be enabled.
        if (ConfigurationUtils.isMultiUserChatEnabled())
        {
            // IM contacts can be uplifted to group chats
            if (getIMContact() != null)
            {
                isGroupImCapable = true;
            }
        }

        logger.interval("isGroupImCapable:" + uid,
                        "isGroupImCapable:" + isGroupImCapable);

        return isGroupImCapable;
    }

    @Override
    public String getDirectInviteResource()
    {
       // The string used in this menu item varies depending on whether this
       // contact can be uplifted to a conference and whether or not a
       // conference is already in progress.
       String resource;
       boolean conferenceCreated =
           ContactlistActivator.getConferenceService().isConferenceCreated();
       if (canBeUplifedToConference())
       {
           resource = conferenceCreated ?
               "service.gui.conf.INVITE_IM_IN_CONFERENCE" :
               "service.gui.conf.INVITE_IM_NOT_IN_CONFERENCE";
       }
       else
       {
           resource = conferenceCreated ?
               "service.gui.conf.INVITE_NON_IM_IN_CONFERENCE" :
               "service.gui.conf.INVITE_NON_IM_NOT_IN_CONFERENCE";
       }

       return resource;
    }

    @Override
    public String getSelectOthersInviteResource()
    {
        // The string used in this menu item varies depending on whether this
        // contact can be uplifted to a conference and whether or not a
        // conference is already in progress.
        String resource;
        boolean conferenceCreated =
            ContactlistActivator.getConferenceService().isConferenceCreated();
        if (canBeUplifedToConference())
        {
            resource = "service.gui.conf.INVITE_OTHERS_IM";
        }
        else
        {
            resource = conferenceCreated ?
                "service.gui.conf.INVITE_OTHERS_NON_IM_IN_CONFERENCE" :
                "service.gui.conf.INVITE_OTHERS_NON_IM";
        }

        return resource;
    }

    @Override
    public void createConference(boolean createImmediately)
    {
        ContactlistActivator.getConferenceService().
            createOrAdd(MetaContactImpl.this, createImmediately);
    }

    @Override
    public boolean canBeUplifedToConference()
    {
        return ContactlistActivator.getConferenceService().canContactBeUplifted(this);
    }
}
