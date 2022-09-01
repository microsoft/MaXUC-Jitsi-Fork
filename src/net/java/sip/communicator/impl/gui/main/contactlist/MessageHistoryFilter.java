/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MessageHistoryFilter</tt> is a filter over the message history
 * contact sources.
 */
public class MessageHistoryFilter extends AbstractHistoryFilter
{
    private Logger sLog = Logger.getLogger(MessageHistoryFilter.class);

    /**
     * Creates an instance of MessageHistoryFilter
     *
     * @param contactList the contact list that the filter is applied to.
     */
    public MessageHistoryFilter(TreeContactList contactList)
    {
        super(contactList);
    }

    @Override
    public void applyFilter(FilterQuery filterQuery)
    {
        mHistoryTypes.add(ContactSourceService.MESSAGE_HISTORY_TYPE);
        super.applyFilter(filterQuery);
        setNoMessagesUI(mResultCount == 0);
    }

    /**
     * Update the contact list UI according to whether there are any message
     * history entries
     *
     * Synchronized to prevent issues where one thread is trying to enable the
     * view while another is trying to disable it
     *
     * @param noMessages true if we have no message history entries
     */
    private synchronized void setNoMessagesUI(boolean noMessages)
    {
        // Finally, we've finished looking at the contacts.  Activate the "no
        // messages view" if there are no messages.  Note that this query may
        // finish after the filter has been removed, thus we need to make sure
        // we only change views if the filter is still active
        AbstractMainFrame mainFrame = GuiActivator.getUIService().getMainFrame();
        boolean messageFilterActive =
            mContactList.getCurrentFilter() == mContactList.getMessageHistoryFilter();

        if (mainFrame != null && messageFilterActive)
        {
            sLog.info("Setting no messages UI " + noMessages);
            mainFrame.enableUnknownContactView(noMessages);
        }
    }
}
