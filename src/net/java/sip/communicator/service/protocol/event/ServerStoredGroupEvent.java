/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Events of this class indicate a change in one of the properties of a
 * ServerStoredGroup.
 *
 * @author Emil Ivov
 */
public class ServerStoredGroupEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that a contact group has been successfully created.
     */
    public static final int GROUP_CREATED_EVENT = 1;

    /**
     * Indicates that a contact group has been successfully deleted.
     */
    public static final int GROUP_REMOVED_EVENT = 2;

    /**
     * Indicates that a group has been successfully renamed.
     */
    public static final int GROUP_RENAMED_EVENT = 3;

    /**
     * Indicates that a group has just been resolved against the server.
     */
    public static final int GROUP_RESOLVED_EVENT = 4;

    private int eventID = -1;
    private ProtocolProviderService sourceProvider = null;
    private OperationSetPersistentPresence parentOperationSet = null;
    private ContactGroup parentGroup = null;

    /**
     * Creates a ServerStoredGroupChangeEvent instance.
     * @param sourceGroup the group that this event is pertaining to.
     * @param eventID an int describing the cause of the event
     * @param parentGroup the group that the source group is a child of.
     * @param sourceProvider a reference to the protocol provider where this is
     * happening
     * @param opSet a reference to the operation set responsible for the event
     */
    public ServerStoredGroupEvent(ContactGroup sourceGroup,
                                  int eventID,
                                  ContactGroup parentGroup,
                                  ProtocolProviderService sourceProvider,
                                  OperationSetPersistentPresence opSet)
    {
        super(sourceGroup);

        this.eventID = eventID;
        this.sourceProvider = sourceProvider;
        this.parentOperationSet = opSet;
        this.parentGroup = parentGroup;
    }

    /**
     * Returns a reference to the <tt>ContactGroup</tt> that this event is
     * pertaining to.
     * @return a reference to the ContactGroup that caused the event.
     */
    public ContactGroup getSourceGroup()
    {
        return (ContactGroup)getSource();
    }

    /**
     * Returns an int describing the cause of this event.
     * @return an int describing the cause of this event.
     */
    public int getEventID()
    {
        return eventID;
    }

    /**
     * Returns a reference to the provider under which the event is being
     * generated
     * @return a ProtocolProviderService instance indicating the provider
     * responsible for the event.
     */
    public ProtocolProviderService getSourceProvider()
    {
        return this.sourceProvider;
    }

    /**
     * Returns the group containing the event source group
     * @return a reference to the <tt>ContactGroup</tt> instance that is parent
     * of the <tt>ContactGroup</tt> which is the source of this event.
     */
    public ContactGroup getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Returns a String representation of this event.
     * @return a String containing details describin this event.
     */
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("ServerStoredGroupEvent:[EventID= ");
        buff.append(getEventID());
        buff.append(" SourceGroup=");
        buff.append(getSource());
        buff.append("]");

        return buff.toString();
    }
}
