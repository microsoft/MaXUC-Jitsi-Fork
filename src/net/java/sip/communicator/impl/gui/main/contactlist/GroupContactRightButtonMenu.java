/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

/**
 * The right click menu on a group contact in the main contacts list.
 */
public class GroupContactRightButtonMenu extends MetaContactRightButtonMenu
{
    private static final long serialVersionUID = 0L;

    public GroupContactRightButtonMenu(MetaContact contactItem)
    {
        super(contactItem);
    }

    @Override
    protected void init()
    {
        boolean messageSupported = ConfigurationUtils.isMultiUserChatEnabled();
        boolean meetingSupported = GuiActivator.getConferenceService().isFullServiceEnabled();

        if (messageSupported)
        {
            initSendMessageItem();
        }

        if (meetingSupported)
        {
            // Ask the meeting menu to include a separator if we added a
            // message item above.
            initMeetingMenu(messageSupported);
        }
        // View and edit contact plugins will always be present, so add
        // separator before them as long as we have already added at least one
        // of the above 2 items.
        if (messageSupported || meetingSupported)
        {
            addSeparator();
        }
        initRemoveMenu();
        initFavoritesMenuItem();
        initPluginComponents();
    }
}
