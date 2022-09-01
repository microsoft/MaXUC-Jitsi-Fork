// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

public class AddressBookContactGroup
    implements ContactGroup
{
    /** Protocol provider for which we are creating this group */
    private final ProtocolProviderService mProvider;

    /** Data handler which holds all the details of the contacts */
    private AbstractAddressBookDataHandler mDataHandler;

    /** The subgroups that this group has */
    private final ArrayList<ContactGroup> mSubGroups =
            new ArrayList<>();

    public AddressBookContactGroup(ProtocolProviderService provider,
                                  AbstractAddressBookDataHandler dataHandler)
    {
        mProvider = provider;
        mDataHandler = dataHandler;
    }

    public Iterator<ContactGroup> subgroups()
    {
        return mSubGroups.iterator();
    }

    public int countSubgroups()
    {
        return mSubGroups.size();
    }

    public ContactGroup getGroup(int index)
    {
        // AddressBook does not support groups so return null.
        return null;
    }

    public ContactGroup getGroup(String groupName)
    {
        // AddressBook does not support groups so return null.
        return null;
    }

    public Iterator<Contact> contacts()
    {
        return mDataHandler.getAllContacts();
    }

    public int countContacts()
    {
        return mDataHandler.numberContacts();
    }

    public Contact getContact(String id)
    {
        return mDataHandler.findContactByID(id);
    }

    public boolean canContainSubgroups()
    {
        // AddressBook does not support subgroups
        return false;
    }

    public String getGroupName()
    {
        return "AddressBook";
    }

    public ProtocolProviderService getProtocolProvider()
    {
        return mProvider;
    }

    public ContactGroup getParentContactGroup()
    {
        // AddressBook does not support sub groups so there will only be one
        // AddressBook group. Therefore the parent group will always be null
        return null;
    }

    public boolean isPersistent()
    {
        return true;
    }

    public String getUID()
    {
        // We don't support subgroups thus we can use "AddressBook" as an
        // identifier
        return "AddressBook";
    }

    public boolean isResolved()
    {
        return true;
    }

    public String getPersistentData()
    {
        // No persistent data as we can construct the group from it's members
        return null;
    }
}
