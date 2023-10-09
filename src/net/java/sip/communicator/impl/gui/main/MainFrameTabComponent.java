// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

/**
 * An object representing the UI of a tab on the main frame. It implements
 * <tt>MouseListener</tt> in order to activate the tab when it is clicked. It
 * implements <tt>UINotificationListener</tt> as the tab is able to display a
 * notification counter for notifications (currently missed calls and unread
 * chats)
 */
public class MainFrameTabComponent
{
    /**
     * The string for the Favourites tab
     */
    public static final String FAVOURITES_TAB_NAME =
        "service.gui.TAB_FAVORITES";

    /**
     * The string for the Contacts tab
     */
    public static final String CONTACTS_TAB_NAME = "service.gui.TAB_CONTACTS";

    /**
     * The string for the History tab
     */
    public static final String HISTORY_TAB_NAME = "service.gui.TAB_HISTORY";

    private MainFrameTabComponent()
    {
    }
}
