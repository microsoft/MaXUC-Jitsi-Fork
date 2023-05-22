/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.contactlist.event;

import static org.jitsi.util.Hasher.logHasher;

import java.beans.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * An abstract event used for meta contact events indicating moving the meta
 * contact or changing its name.
 * <p>
 * @author Emil Ivov
 */
public abstract class MetaContactPropertyChangeEvent
    extends PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that the source meta contact has moved from one location to
     * another. The old and new values contain the previous and the new
     * parent group of this meta contact.
     */
    public static final String META_CONTACT_MOVED = "MetaContactMovedEvent";

    /**
     * Indicates that the meta contact has been renamed. The old and new value
     * arguments contain the old and new names of this contact.
     */
    public static final String META_CONTACT_RENAMED = "MetaContactRenamedEvent";

    /**
     * Indicates that the MetaContactEvent instance was triggered by the
     * removal of a protocol specific contact from an existing MetaContact.
     */
    public static final String PROTO_CONTACT_REMOVED = "ProtoContactRemoved";

    /**
     * Indicates that the MetaContactEvent instance was triggered by the
     * a protocol specific contact to a new MetaContact parent.
     */
    public static final String PROTO_CONTACT_ADDED = "ProtoContactAdded";

    /**
     * Indicates that the MetaContactEvent instance was triggered by moving
     * addition of a protocol specific contact to an existing MetaContact.
     */
    public static final String PROTO_CONTACT_MOVED = "ProtoContactMoved";

    /**
     * Indicates that the MetaContactEvent instance was triggered by the update
     * of an Avatar for one of its encapsulated contacts.
     */
    public static final String META_CONTACT_AVATAR_UPDATE
                                            = "MetaContactAvatarUpdate";

    /**
     * Indicates that the meta contact has been modified. The old and new value
     * arguments contain the old and new values of the modification.
     */
    public static final String PROTO_CONTACT_MODIFIED
                                            = "ProtoContactModifiedEvent";

    /**
     * Indicates that the meta contact has been modified. The old and new value
     * arguments contain the old and new values of the modification.
     */
    public static final String META_CONTACT_MODIFIED
                                            = "MetaContactModifiedEvent";

    /**
     * Creates an instnace of this event.
     * @param source the <tt>MetaContact</tt> that this event is about.
     * @param eventName one of the META_CONTACT_XXXED <tt>String</tt> strings
     * indicating the exact typ of this event.
     * @param oldValue the value of the changed property before the change
     * had occurred.
     * @param newValue the value of the changed property after the chagne has
     * occurred.
     */
    public MetaContactPropertyChangeEvent(MetaContact source,
                             String eventName,
                             Object oldValue,
                             Object newValue)
    {
        super(source, eventName, oldValue, newValue);
    }

    /**
     * Returns a reference to the <tt>MetaContact</tt> that this event is about
     * @return the <tt>MetaContact</tt> that this event is about.
     */
    public MetaContact getSourceMetaContact()
    {
        return (MetaContact)getSource();
    }

    /**
     * Returns a string representation of the object, ensuring that any
     * PII gets hashed.
     *
     * @return a string representation of the object
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append("[propertyName=").append(getPropertyName());
        String oldValue = getOldValue() != null ? getOldValue().toString() : "";
        String newValue = getNewValue() != null ? getNewValue().toString() : "";
        sb.append("; oldValue=").append(logHasher(oldValue));
        sb.append("; newValue=").append(logHasher(newValue));
        sb.append("; propagationId=").append(getPropagationId());
        sb.append("; source=").append(getSource());
        return sb.append("]").toString();
    }
}
