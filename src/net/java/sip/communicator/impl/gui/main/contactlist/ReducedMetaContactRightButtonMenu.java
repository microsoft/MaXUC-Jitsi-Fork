/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The right click menu on contacts in the list of participants in a group chat.
 */
public class ReducedMetaContactRightButtonMenu extends MetaContactRightButtonMenu
{
    private static final long serialVersionUID = 0L;

    /**
     * @param contactItem
     * @param chatRoomMember - The presence or absence of this dictates whether or not this is the menu for a
     * contact in a group chat or not.
     */
    public ReducedMetaContactRightButtonMenu(MetaContact contactItem,
                                             ChatRoomMember chatRoomMember)
    {
        super(contactItem, chatRoomMember);
    }

    @Override
    protected void init()
    {
        initCallMenus();
        initSendMessageItem();

        // Always ask the meeting menu to add a separator as we will always add at least the call menu above it (the call
        // menu is present but greyed out if the contact cannot be called).
        initMeetingMenu(true);

        // Add a separator above the remove from group chat item iff there is a MetaContact, as none of the above
        // menus are displayed if there is no MetaContact (and at least the one will be added if there is one).
        initRemoveFromGroupChatItem((metaContact != null));

        // None of the top three menus will be added if there's no metacontact, and we only add the 'add contact' item
        // if there is a metacontact. Therefore we add a separator above this menu item iff we've added the 'remove
        // from group' item.  So match the if test within that function here.
        initAddContactItem((mChatRoomMember != null) && ConfigurationUtils.isMultiUserChatEnabled());
    }
}
