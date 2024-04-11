/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import javax.sip.address.*;
import javax.xml.namespace.*;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;
import org.w3c.dom.*;

/**
 * A simple, straightforward implementation of a SIP Contact.
 *
 * @author Emil Ivov
 * @author Benoit Pradelle
 * @author Lubomir Marinov
 * @author Grigorii Balutsel
 */
public class ContactSipImpl
    extends AbstractContact
{
    /**
     * Property used for store in persistent data indicating that contact
     * is resolved against xcap server.
     */
    private static final String XCAP_RESOLVED_PROPERTY = "xcap.resolved";

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceSipImpl parentProvider;

    /**
     * The group that belong to.
     */
    private ContactGroupSipImpl parentGroup = null;

    /**
     * The presence status of the contact.
     */
    private PresenceStatus presenceStatus;

    /**
     * The image content.
     */
    private BufferedImageFuture image;

    /**
     * Determines whether this contact is persistent, i.e. member of the contact
     * list or whether it is here only temporarily.
     */
    private boolean isPersistent = true;

    /**
     * Determines whether the contact has been resolved (i.e. we have a
     * confirmation that it is still on the server contact list).
     */
    private boolean isResolved = false;

    /**
     * Determines whether this contact can be resolved or if he will be
     * never resolved (for example if he doesn't support SIMPLE)
     */
    private boolean isResolvable = true;

    /**
     * Determines whether the contact has been resolved on the XCAP server.
     */
    private boolean xCapResolved = false;

    /**
     * The XCAP equivalent of SIP contact group.
     */
    private final EntryType entry;

     /**
     * The SIP contact identifier.
     */
    private final Address sipAddress;

    /**
     * Current subscription state of the contact.
     * One of:
     * SubscriptionStateHeader.PENDING,
     * SubscriptionStateHeader.ACTIVE,
     * SubscriptionStateHeader.TERMINATED.
     */
    private String subscriptionState;

    /**
     * Determines whether the contact should be hidden in the contact list.
     */
    private boolean isHidden = false;

    /**
     * Determines whether the contact is our own line.
     */
    private boolean isOwnLine = false;

    /**
     * Creates an instance of a meta contact with the specified string used
     * as a name and identifier.
     *
     * @param contactAddress the identifier of this contact
     *  (also used as a name).
     * @param parentProvider the provider that created us.
     */
    public ContactSipImpl(
                Address contactAddress,
                ProtocolProviderServiceSipImpl parentProvider)
    {
        this.sipAddress = contactAddress;
        this.entry = new EntryType(contactAddress.getURI().toString());
        this.parentProvider = parentProvider;
        this.presenceStatus = parentProvider.getSipStatusEnum()
            .getStatus(SipStatusEnum.UNKNOWN);
    }

    /**
     * Gets the entry.
     *
     * @return the entry
     */
    EntryType getEntry()
    {
        return entry;
    }

    /**
     * Gets the entry's uri.
     *
     * @return the entry' uri.
     */
    public String getUri()
    {
        return entry.getUri();
    }

    /**
     * This method is only called when the contact is added to a new
     * <tt>ContactGroupSipImpl</tt> by the
     * <tt>ContactGroupSipImpl</tt> itself.
     *
     * @param newParentGroup the <tt>ContactGroupSipImpl</tt> that is now
     * parent of this <tt>ContactSipImpl</tt>
     */
    void setParentGroup(ContactGroupSipImpl newParentGroup)
    {
        this.parentGroup = newParentGroup;
    }

    /**
     * Returns a String that can be used for identifying the contact.
     *
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getAddress()
    {
        SipURI sipURI = (SipURI) sipAddress.getURI();
        return sipURI.getUser() + "@" + sipURI.getHost();
    }

    /**
     * Returns the jain-sip Address instance that this contact is wrapping.
     *
     * @return the jain-sip Address instance that this contact is wrapping.
     */
    public Address getSipAddress()
    {
        return sipAddress;
    }

    /**
     * Returns a String that could be used by any user interacting modules
     * for referring to this contact.
     *
     * @return a String that can be used for referring to this contact when
     *   interacting with the user.
     */
    public String getDisplayName()
    {
        if(this.entry.getDisplayName() != null)
        {
            return this.entry.getDisplayName().getValue();
        }

        // If we didn't find a display name we return the user name.
        SipURI sipURI = (SipURI) sipAddress.getURI();
        String sipUserName = sipURI.getUser();

        if (sipUserName != null && sipUserName.length() > 0)
            return sipUserName;

        if(getAddress().startsWith("sip:"))
            return getAddress().substring(4);

        return getAddress();
    }

    /**
     * Sets a String that could be used by any user interacting modules
     * for referring to this contact.
     *
     * @param displayName a human readable name to use for this contact.
     */
    public void setDisplayName(String displayName)
    {
        DisplayNameType displayNameType = new DisplayNameType();
        displayNameType.setValue(displayName);
        this.entry.setDisplayName(displayNameType);
    }

    /**
     * Sets a String that could be used by any user interacting modules for
     * referring to this contact.
     *
     * @param displayName a human readable name to use for this contact.
     */
    public void setDisplayName(DisplayNameType displayName)
    {
        this.entry.setDisplayName(displayName);
    }

    /**
     * Sets the entry custom attributes.
     *
     * @param otherAttributes the custom attributes.
     */
    void setOtherAttributes(Map<QName, String> otherAttributes)
    {
        this.entry.setAnyAttributes(otherAttributes);
    }

    /**
     * Sets the entry custom elements.
     *
     * @param any the custom elements.
     */
    void setAny(List<Element> any)
    {
        this.entry.setAny(any);
    }

    /**
     * Gets the entry custom elements.
     *
     * @return the custom elements.
     */
    List<Element> getAny()
    {
        return this.entry.getAny();
    }

    /**
     * Gets a byte array containing an image (most often a photo or an avatar)
     * that the contact uses as a representation.
     *
     * @return byte[] an image representing the contact.
     */
    public BufferedImageFuture getImage()
    {
        return image;
    }

    /**
     * Returns the status of the contact.
     *
     * @return always IcqStatusEnum.ONLINE.
     */
    public PresenceStatus getPresenceStatus()
    {
        return this.presenceStatus;
    }

    /**
     * Sets <tt>sipPresenceStatus</tt> as the PresenceStatus that this
     * contact is currently in.
     * @param sipPresenceStatus the <tt>SipPresenceStatus</tt>
     * currently valid for this contact.
     */
    public void setPresenceStatus(PresenceStatus sipPresenceStatus)
    {
        this.presenceStatus = sipPresenceStatus;
    }

    /**
     * Returns a reference to the protocol provider that created the contact.
     *
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return parentProvider;
    }

    /**
     * Determines whether or not this contact represents our own identity.
     *
     * @return true in case this is a contact that represents ourselves and
     *   false otherwise.
     */
    public boolean isLocal()
    {
        return false;
    }

    /**
     * Returns the group that contains this contact.
     * @return a reference to the <tt>ContactGroupSipImpl</tt> that
     * contains this contact.
     */
    public ContactGroup getParentContactGroup()
    {
        return this.parentGroup;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("ContactSipImpl[ DisplayName=")
                .append(logHasher(getDisplayName())).append("]");

        return buff.toString();
    }

    /**
     * Determines whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     *
     * @return true if the contact is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Specifies whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     *
     * @param isPersistent true if the contact is persistent and false
     * otherwise.
     */
    public void setPersistent(boolean isPersistent)
    {
        this.isPersistent = isPersistent;
    }

    /**
     * Gets the value of the xCapResolved property.
     *
     * @return the xCapResolved property.
     */
    public boolean isXCapResolved()
    {
        return xCapResolved;
    }

    /**
     * Sets the value of the xCapResolved property.
     *
     * @param xCapResolved the xCapResolved to set.
     */
    public void setXCapResolved(boolean xCapResolved)
    {
        this.xCapResolved = xCapResolved;
    }

    /**
     * Gets the persistent data. Fields are properties separated by ';'
     *
     * @return the persistent data
     */
    public String getPersistentData()
    {
        String persistentData =
           XCAP_RESOLVED_PROPERTY + "=" + xCapResolved + ";" +
           MetaContact.IS_CONTACT_HIDDEN + "=" + isHidden + ";" +
           MetaContact.IS_OWN_LINE + "=" + isOwnLine + ";";

        return persistentData;
    }

    /**
     * Sets the persistent data.
     *
     * @param persistentData the persistent data to set.
     */
    public void setPersistentData(String persistentData)
    {
        if (persistentData == null)
        {
            return;
        }

        String[] values = persistentData.split(";");
        for (String value : values)
        {
            String data[] = value.split("=");
            if (data[0].equals(XCAP_RESOLVED_PROPERTY) && data.length > 1)
            {
                xCapResolved = Boolean.valueOf(data[1]);
            }
            else if (data[0].equals(MetaContact.IS_CONTACT_HIDDEN) && data.length > 1)
            {
                isHidden = Boolean.valueOf(data[1]);
            }
            else if (data[0].equals(MetaContact.IS_OWN_LINE) && data.length > 1)
            {
                isOwnLine = Boolean.valueOf(data[1]);
            }
        }
    }

    /**
     * Determines whether or not this contact has been resolved against the
     * server. Unresolved contacts are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped contacts to their on-line buddies.
     *
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return this.isResolved;
    }

    /**
     * Makes the contact resolved or unresolved.
     *
     * @param resolved  true to make the contact resolved; false to
     *                  make it unresolved
     */
    public void setResolved(boolean resolved)
    {
        this.isResolved = resolved;
    }

    /**
     * Determines whether or not this contact should be hidden in the main UI
     *
     * @return true if the contact should be hidden in the main UI
     */
    public boolean isHidden()
    {
        return this.isHidden;
    }

    /**
     * Determines whether or not this contact can be resolved against the
     * server.
     *
     * @return true if the contact can be resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolvable()
    {
        return this.isResolvable;
    }

    /**
     * Makes the contact resolvable or unresolvable.
     *
     * @param resolvable  true to make the contact resolvable; false to
     * make it unresolvable
     */
    public void setResolvable(boolean resolvable)
    {
        this.isResolvable = resolvable;
    }

    /**
     * Indicates whether some other object is "equal to" this one which in terms
     * of contacts translates to having equal ids. The resolved status of the
     * contacts deliberately ignored so that contacts would be declared equal
     * even if it differs.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this contact has the same id as that of the
     * <code>obj</code> argument.
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ContactSipImpl || obj instanceof String))
            return false;

        if(obj instanceof String)
        {
            String sobj = (String)obj;

            if(sobj.startsWith("sip:"))
                sobj = sobj.substring(4);

            if(getAddress().equalsIgnoreCase(sobj))
                return true;

            SipURI sipURI = (SipURI)sipAddress.getURI();

            if(sipURI.getUser().equalsIgnoreCase(sobj))
                return true;

            return false;
        }

        ContactSipImpl sipContact = (ContactSipImpl) obj;

        return this.getAddress().equals(sipContact.getAddress());
    }

    /**
     * Return the current status message of this contact.
     *
     * @return null as the protocol has currently no support of status messages
     */
    public String getStatusMessage()
    {
        return null;
    }

    /**
     * Current subscription state of the contact.
     * One of:
     * SubscriptionStateHeader.PENDING,
     * SubscriptionStateHeader.ACTIVE,
     * SubscriptionStateHeader.TERMINATED.
     *
     * @return current subscription state.
     */
    public String getSubscriptionState()
    {
        return subscriptionState;
    }

    /**
     * Change current subscription state.
     * @param subscriptionState the new state.
     */
    public void setSubscriptionState(String subscriptionState)
    {
        this.subscriptionState = subscriptionState;
    }

    /**
     * Indicates if this contact supports resources.
     *
     * @return <tt>false</tt> to indicate that this contact doesn't support
     * resources
     */
    public boolean supportResources()
    {
        return false;
    }

    /**
     * Returns a collection of resources supported by this contact or null
     * if it doesn't support resources.
     *
     * @return null, as this contact doesn't support resources
     */
    public Collection<ContactResource> getResources()
    {
        return null;
    }

    /**
     * Fetch the contact type as an alternative to <tt>instanceof</tt> checks
     */
    @Override
    public ContactType getContactType()
    {
        return ContactType.SIP;
    }
}
