/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.contactsource;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.util.*;

/**
 * The <tt>ContactDetail</tt> is a detail of a <tt>SourceContact</tt>
 * corresponding to a specific address (phone number, email, identifier, etc.),
 * which defines the different possible types of communication and the preferred
 * <tt>ProtocolProviderService</tt>s to go through.
 *
 * <p>
 * Example: A <tt>ContactDetail</tt> could define two types of communication,
 * by declaring two supported operation sets
 * <tt>OperationSetBasicInstantMessaging</tt> to indicate the support of instant
 * messages and <tt>OperationSetBasicTelephony</tt> to indicate the support of
 * telephony. It may then specify a certain <tt>ProtocolProviderService</tt> to
 * go through only for instant messages. This would mean that for sending an
 * instant message to this <tt>ContactDetail</tt> one should obtain an instance
 * of the <tt>OperationSetBasicInstantMessaging</tt> from the specific
 * <tt>ProtocolProviderService</tt> and send a message through it. However when
 * no provider is specified for telephony operations, then one should try to
 * obtain all currently available telephony providers and let the user make
 * their choice.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class ContactDetail
{
    /**
     * Defines all possible categories for a <tt>ContactDetail</tt>.
     */
    public enum Category
    {
        /**
         * The standard/well-known category of a <tt>ContactDetail</tt>
         * representing personal details, like name, last name, nickname.
         */
        Personal("Personal"),

        /**
         * The standard/well-known category of a <tt>ContactDetail</tt>
         * representing organization details, like organization name and job
         * title.
         */
        Organization("Organization"),

        /**
         * The standard/well-known category of a <tt>ContactDetail</tt>
         * representing an e-mail address.
         */
        Email("Email"),

        /**
         * The standard/well-known category of a <tt>ContactDetail</tt>
         * representing a contact address for instant messaging.
         */
        InstantMessaging("IM"),

        /**
         * The standard/well-known category of a <tt>ContactDetail</tt>
         * representing a phone number.
         */
        Phone("Phone"),

        /**
         * The standard/well-known category of a <tt>ContactDetail</tt>
         * representing a postal address.
         */
        Address("Address"),

        /**
         * The category of <tt>ContactDetail</tt> representing a chat room
         */
        GroupChat("GroupChat");

        /**
         * Current enum value.
         */
        private final String value;

        /**
         * Creates enum whith the specified value.
         *
         * @param value the value to set.
         */
        Category(String value)
        {
            this.value = value;
        }

        /**
         * Gets the value.
         *
         * @return the value
         */
        public String value()
        {
            return value;
        }

    }

    /**
     * Defines all possible sub-categories for a <tt>ContactDetail</tt>.
     */
    public enum SubCategory
    {
        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a name. It could be an organization name or a personal
         * name.
         */
        Name("Name"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a last name.
         */
        LastName("LastName"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a nickname.
         */
        Nickname("Nickname"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a postal code.
         */
        HomePage("HomePage"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing an address of a contact at their home.
         */
        Home("Home"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a mobile contact address (e.g. a cell phone number).
         */
        Mobile("Mobile"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing an address of a contact at their work.
         */
        Work("Work"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a fax number.
         */
        Fax("Fax"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a different number.
         */
        Other("Other"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing an IM network (like for example jabber).
         */
        AIM("AIM"),
        ICQ("ICQ"),
        MSN("MSN"),
        Jabber("Jabber"),
        Skype("Skype"),
        Yahoo("Yahoo"),
        Facebook("Facebook"),
        GoogleTalk("GoogleTalk"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a country/region name.
         */
        CountryRegion("CountryRegion"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a state name.
         */
        State("State"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a city name.
         */
        City("City"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a street address.
         */
        Street("Street"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a postal code.
         */
        PostalCode("PostalCode"),

        /**
         * The standard/well-known label of a <tt>ContactDetail</tt>
         * representing a job title.
         */
        JobTitle("JobTitle");

        /**
         * Current enum value.
         */
        private final String value;

        /**
         * Creates enum whith the specified value.
         *
         * @param value the value to set.
         */
        SubCategory(String value)
        {
            this.value = value;
        }

        /**
         * Gets the value.
         *
         * @return the value
         */
        public String value()
        {
            return value;
        }

    }

    /**
     * The category of this <tt>ContactQuery</tt>.
     */
    private final Category category;

    /**
     * The address of this contact detail. This should be the address through
     * which the contact could be reached by one of the supported
     * <tt>OperationSet</tt>s (e.g. by IM, call).
     */
    protected String contactDetailValue;

    /**
     * The display name of this detail.
     */
    private String detailDisplayName;

    /**
     * The set of labels of this <tt>ContactDetail</tt>. The labels may be
     * arbitrary and may include any of the standard/well-known labels defined
     * by the <tt>LABEL_XXX</tt> constants of the <tt>ContactDetail</tt> class.
     */
    private final Collection<SubCategory> subCategories
        = new LinkedList<>();

    /**
     * A mapping of <tt>OperationSet</tt> classes and preferred protocol
     * providers for them.
     */
    private Map<Class<? extends OperationSet>, ProtocolProviderService>
        preferredProviders;

    /**
     * A mapping of <tt>OperationSet</tt> classes and preferred protocol name
     * for them.
     */
    private Map<Class<? extends OperationSet>, String> preferredProtocols;

    /**
     * A list of all supported <tt>OperationSet</tt> classes.
     */
    private List<Class<? extends OperationSet>> supportedOpSets = null;

    /**
     * Creates a <tt>ContactDetail</tt> by specifying the contact address,
     * corresponding to this detail.
     * @param contactDetailValue the contact detail value corresponding to this
     * detail
     */
    public ContactDetail(String contactDetailValue)
    {
        this(contactDetailValue, null, null, null);
    }

    /**
     * Creates a <tt>ContactDetail</tt> by specifying the contact address,
     * corresponding to this detail.
     * @param contactDetailValue the contact detail value corresponding to this
     * detail
     * @param detailDisplayName the display name of this detail
     */
    public ContactDetail(String contactDetailValue, String detailDisplayName)
    {
        this(contactDetailValue, detailDisplayName, null, null);
    }

    /**
     * Initializes a new <tt>ContactDetail</tt> instance which is to represent a
     * specific contact address and which is to be optionally labeled with a
     * specific set of labels.
     *
     * @param contactDetailValue the contact detail value to be represented by
     * the new <tt>ContactDetail</tt> instance
     * @param category
     */
    public ContactDetail(   String contactDetailValue,
                            Category category)
    {
        this(contactDetailValue, null, category, null);
    }

    /**
     * Initializes a new <tt>ContactDetail</tt> instance which is to represent a
     * specific contact address and which is to be optionally labeled with a
     * specific set of labels.
     *
     * @param contactDetailValue the contact detail value to be represented by
     * the new <tt>ContactDetail</tt> instance
     * @param detailDisplayName the display name of this detail
     * @param category
     */
    public ContactDetail(   String contactDetailValue,
                            String detailDisplayName,
                            Category category)
    {
        this(contactDetailValue, detailDisplayName, category, null);
    }

    /**
     * Initializes a new <tt>ContactDetail</tt> instance which is to represent a
     * specific contact address and which is to be optionally labeled with a
     * specific set of labels.
     *
     * @param contactDetailValue the contact detail value to be represented by
     * the new <tt>ContactDetail</tt> instance
     * @param category
     * @param subCategories the set of sub categories with which the new
     * <tt>ContactDetail</tt> instance is to be labeled.
     */
    public ContactDetail(   String contactDetailValue,
                            Category category,
                            SubCategory[] subCategories)
    {
        this(contactDetailValue, null, category, subCategories);
    }

    /**
     * Initializes a new <tt>ContactDetail</tt> instance which is to represent a
     * specific contact address and which is to be optionally labeled with a
     * specific set of labels.
     *
     * @param contactDetailValue the contact detail value to be represented by
     * the new <tt>ContactDetail</tt> instance
     * @param detailDisplayName the display name of this detail
     * @param category
     * @param subCategories the set of sub categories with which the new
     * <tt>ContactDetail</tt> instance is to be labeled.
     */
    public ContactDetail(   String contactDetailValue,
                            String detailDisplayName,
                            Category category,
                            SubCategory[] subCategories)
    {
        // the value of the detail
        this.contactDetailValue = contactDetailValue;

        if (!StringUtils.isNullOrEmpty(detailDisplayName))
            this.detailDisplayName = detailDisplayName;
        else
            this.detailDisplayName = contactDetailValue;

        // category & labels
        this.category = category;

        if (subCategories != null)
        {
            for (SubCategory subCategory : subCategories)
            {
                if ((subCategory != null)
                    && !this.subCategories.contains(subCategory))
                {
                    this.subCategories.add(subCategory);
                }
            }
        }
    }

    /**
     * Sets a mapping of preferred <tt>ProtocolProviderServices</tt> for
     * a specific <tt>OperationSet</tt>.
     * @param preferredProviders a mapping of preferred
     * <tt>ProtocolProviderService</tt>s for specific <tt>OperationSet</tt>
     * classes
     */
    public void setPreferredProviders(
        Map<Class<? extends OperationSet>, ProtocolProviderService>
                                                            preferredProviders)
    {
        this.preferredProviders = preferredProviders;
    }

    /**
     * Sets a mapping of a preferred <tt>preferredProtocol</tt> for a specific
     * <tt>OperationSet</tt>. The preferred protocols are meant to be set by
     * contact source implementations that don't have a specific protocol
     * providers to suggest, but are able to propose just the name of the
     * protocol to be used for a specific operation. If both - preferred
     * provider and preferred protocol are set, then the preferred protocol
     * provider should be prioritized.
     *
     * @param preferredProtocols a mapping of preferred
     * <tt>ProtocolProviderService</tt>s for specific <tt>OperationSet</tt>
     * classes
     */
    public void setPreferredProtocols(
        Map<Class<? extends OperationSet>, String> preferredProtocols)
    {
        this.preferredProtocols = preferredProtocols;
    }

    /**
     * Creates a <tt>ContactDetail</tt> by specifying the corresponding contact
     * address and a list of all <tt>supportedOpSets</tt>, indicating what are
     * the supporting actions with this contact detail (e.g. sending a message,
     * making a call, etc.)
     * @param supportedOpSets a list of all <tt>supportedOpSets</tt>, indicating
     * what are the supporting actions with this contact detail (e.g. sending a
     * message, making a call, etc.)
     */
    public void setSupportedOpSets(
                            List<Class<? extends OperationSet>> supportedOpSets)
    {
        this.supportedOpSets = supportedOpSets;
    }

    /**
     * Gets the category, if any, of this <tt>ContactQuery</tt>.
     *
     * @return the category of this <tt>ContactQuery</tt> if it has any;
     * otherwise, <tt>null</tt>
     */
    public Category getCategory()
    {
        return category;
    }

    /**
     * Returns the contact address corresponding to this detail.
     *
     * @return the contact address corresponding to this detail
     */
    public String getDetail()
    {
        return contactDetailValue;
    }

    /**
     * Returns the display name of this detail. By default returns the detail
     * value.
     *
     * @return the display name of this detail
     */
    public String getDisplayName()
    {
        return detailDisplayName;
    }

    /**
     * Returns the preferred <tt>ProtocolProviderService</tt> when using the
     * given <tt>opSetClass</tt>.
     *
     * @param opSetClass the <tt>OperationSet</tt> class corresponding to a
     * certain action (e.g. sending an instant message, making a call, etc.).
     * @return the preferred <tt>ProtocolProviderService</tt> corresponding to
     * the given <tt>opSetClass</tt>
     */
    public ProtocolProviderService getPreferredProtocolProvider(
        Class<? extends OperationSet> opSetClass)
    {
        if (preferredProviders != null && preferredProviders.size() > 0 && opSetClass != null)
            return preferredProviders.get(opSetClass);

        return null;
    }

    /**
     * Returns the name of the preferred protocol for the operation given by
     * the <tt>opSetClass</tt>. The preferred protocols are meant to be set by
     * contact source implementations that don't have a specific protocol
     * providers to suggest, but are able to propose just the name of the
     * protocol to be used for a specific operation. If both - preferred
     * provider and preferred protocol are set, then the preferred protocol
     * provider should be prioritized.
     *
     * @param opSetClass the <tt>OperationSet</tt> class corresponding to a
     * certain action (e.g. sending an instant message, making a call, etc.).
     * @return the name of the preferred protocol for the operation given by
     * the <tt>opSetClass</tt>
     */
    public String getPreferredProtocol(Class<? extends OperationSet> opSetClass)
    {
        if (preferredProtocols != null && preferredProtocols.size() > 0 && opSetClass != null)
            return preferredProtocols.get(opSetClass);

        return null;
    }

    /**
     * Returns a list of all supported <tt>OperationSet</tt> classes, which
     * would indicate what are the supported actions by this contact
     * (e.g. write a message, make a call, etc.)
     *
     * @return a list of all supported <tt>OperationSet</tt> classes
     */
    public List<Class<? extends OperationSet>> getSupportedOperationSets()
    {
        return supportedOpSets;
    }

    /**
     * Determines whether the set of labels of this <tt>ContactDetail</tt>
     * contains a specific label. The labels may be arbitrary and may include
     * any of the standard/well-known labels defined by the <tt>LABEL_XXX</tt>
     * constants of the <tt>ContactDetail</tt> class.
     *
     * @param subCategory the subCategory to be determined whether
     * it is contained in this <tt>ContactDetail</tt>
     * @return <tt>true</tt> if the specified <tt>label</tt> is contained in the
     * set of labels of this <tt>ContactDetail</tt>
     */
    public boolean containsSubCategory(SubCategory subCategory)
    {
        return subCategories.contains(subCategory);
    }

    /**
     * Gets the set of labels of this <tt>ContactDetail</tt>. The labels may be
     * arbitrary and may include any of the standard/well-known labels defined
     * by the <tt>LABEL_XXX</tt> constants of the <tt>ContactDetail</tt> class.
     *
     * @return the set of labels of this <tt>ContactDetail</tt>. If this
     * <tt>ContactDetail</tt> has no labels, the returned <tt>Collection</tt> is
     * empty.
     */
    public Collection<SubCategory> getSubCategories()
    {
        return Collections.unmodifiableCollection(subCategories);
    }
}
