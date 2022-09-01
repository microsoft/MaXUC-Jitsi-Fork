// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.callpark;

import java.awt.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A UI group that represents a department or contact group of orbits
 */
public class CallParkUIGroup extends UIGroupImpl
{
    /**
     * The contact group that this object represents
     */
    private final ContactGroup mGroup;

    /**
     * The parent group of this group
     */
    private final CallParkUIGroup mParentGroup;

    /**
     * The group node that represents this object in the UI
     */
    private GroupNode mNode;

    /**
     * The number of orbits contained in this group that are busy
     */
    private int mBusyOrbits;

    /**
     * The total number of orbits in this group and all its subgroups
     */
    private int mOrbits;

    public CallParkUIGroup(ContactGroup group, CallParkUIGroup parentGroup)
    {
        mGroup = group;
        mNode = parentGroup == null ? null : parentGroup.getGroupNode();
        mParentGroup = parentGroup;
    }

    @Override
    public ContactGroup getDescriptor()
    {
        return mGroup;
    }

    @Override
    public String getDisplayName()
    {
        return mGroup.getGroupName();
    }

    @Override
    public int getSourceIndex()
    {
        return 0;
    }

    @Override
    public CallParkUIGroup getParentGroup()
    {
        return mParentGroup;
    }

    @Override
    public boolean isGroupCollapsed()
    {
        return false;
    }

    @Override
    public int countOnlineChildContacts()
    {
        return mGroup.countContacts();
    }

    @Override
    public int countChildContacts()
    {
        return mGroup.countContacts();
    }

    @Override
    public String getId()
    {
        return mGroup.getUID();
    }

    @Override
    public Component getRightButtonMenu()
    {
        return null;
    }

    @Override
    public GroupNode getGroupNode()
    {
        return mNode;
    }

    @Override
    public void setGroupNode(GroupNode groupNode)
    {
        mNode = groupNode;
    }

    /**
     * @return the number of orbits that are busy
     */
    protected int getCountBusyOrbits()
    {
        return mBusyOrbits;
    }

    /**
     * Update the number of orbits that are busy in this group
     *
     * @param busyOrbits the number of busy orbits
     */
    protected void setBusyOrbits(int busyOrbits)
    {
        mBusyOrbits = busyOrbits;
    }

    /**
     * @return all the orbits in this group and it's subgroups
     */
    protected int getCountAllOrbits()
    {
        return mOrbits;
    }

    /**
     * Update the number of orbits that are in this group
     *
     * @param orbits the number of busy orbits
     */
    protected void setAllOrbits(int orbits)
    {
        mOrbits = orbits;
    }
}
