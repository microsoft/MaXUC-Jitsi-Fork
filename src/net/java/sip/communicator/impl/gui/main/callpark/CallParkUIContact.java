// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.callpark;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbit;

import org.jitsi.service.resources.*;

/**
 * UI contact representing a Call Park orbit
 */
public class CallParkUIContact extends UIContactImpl
{
    /**
     * The orbit this contact represents
     */
    private final CallParkOrbit mOrbit;

    /**
     * The UI group that this UI contact is a member of
     */
    private UIGroup mGroup;

    /**
     * The contact node that this contact is represented by
     */
    private ContactNode mContactNode;

    public CallParkUIContact(CallParkOrbit orbit, UIGroup group)
    {
        mOrbit = orbit;
        mGroup = group;
    }

    @Override
    public CallParkOrbit getDescriptor()
    {
        return mOrbit;
    }

    @Override
    public String getDisplayName()
    {
        return mOrbit.getFriendlyName() == null ? mOrbit.getOrbitCode() :
                                                  mOrbit.getFriendlyName();
    }

    @Override
    public String getDisplayDetails()
    {
        return "";
    }

    @Override
    public int getSourceIndex()
    {
        try
        {
            return Integer.parseInt(mOrbit.getOrbitCode().replaceAll("[^\\d]", ""));
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    @Override
    public String getToolTip(String additionalText, String boldNumber)
    {
        return null;
    }

    @Override
    public Component getRightButtonMenu(boolean useReducedMenu)
    {
        return null;
    }

    @Override
    public UIGroup getParentGroup()
    {
        return mGroup;
    }

    @Override
    public void setParentGroup(UIGroup parentGroup)
    {
    }

    @Override
    public Iterator<String> getSearchStrings()
    {
        return null;
    }

    @Override
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
    {
        return null;
    }

    @Override
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass)
    {
        return null;
    }

    @Override
    public List<UIContactDetail> getContactDetails()
    {
        return null;
    }

    @Override
    public Collection<SIPCommButton> getContactCustomActionButtons()
    {
        return null;
    }

    @Override
    public boolean canBeMessaged()
    {
        return false;
    }

    @Override
    public ContactNode getContactNode()
    {
        return mContactNode;
    }

    @Override
    public void setContactNode(ContactNode contactNode)
    {
        mContactNode = contactNode;
    }

    @Override
    public ImageIconFuture getStatusIcon()
    {
        return null;
    }

    @Override
    public ImageIconFuture getAvatar(boolean isExtended)
    {
        return null;
    }

    /**
     * @return the contact group to which this orbit belongs
     */
    public ContactGroup getGroup()
    {
        return (ContactGroup) mGroup.getDescriptor();
    }
}
