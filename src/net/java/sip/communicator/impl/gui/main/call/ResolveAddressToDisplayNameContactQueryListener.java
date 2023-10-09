/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.contactsource.*;
import org.jitsi.util.*;

/**
 * Listener for a contact query, used in order to resolve a contact adress
 * into a display name.
 *
 * @author Vincent Lucas
 */
public class ResolveAddressToDisplayNameContactQueryListener
    implements ContactQueryListener
{
    /**
     * The query we are looking for events.
     */
    private ContactQuery query;

    /**
     * The display name corresponding to the contact address.
     */
    private String resolvedName;

    /**
     * Indicates that a contact has been updated after a  search.
     */
    public void contactChanged(ContactChangedEvent event)
    {
        // NOT USED
    }

    /**
     * Indicates that a new contact has been received for a search.
     */
    public void contactReceived(ContactReceivedEvent event)
    {
        SourceContact contact = event.getContact();
        if(contact != null)
        {
            this.resolvedName = contact.getDisplayName();
            if(isFound())
            {
                this.stop();
            }
        }
    }

    /**
     * Indicates that a contact has been removed after a search.
     */
    public void contactRemoved(ContactRemovedEvent event)
    {
        // NOT USED
    }

    /**
     * Indicates that the status of a search has been changed.
     */
    public void queryStatusChanged(ContactQueryStatusEvent event)
    {
        this.stop();
    }

    /**
     * Tells if the query is still running.
     *
     * @return True if the query is still running. False otherwise.
     */
    public boolean isRunning()
    {
        return this.query != null;
    }

    /**
     * Stops this ResolvedContactQueryListener.
     */
    public synchronized void stop()
    {
        if(this.query != null)
        {
            this.query.removeContactQueryListener(this);
            this.query.cancel();
            this.query = null;
        }
    }

    /**
     * Tells if the query has found a match to resolve the contact address.
     *
     * @return True if the query has found a match to resolve the contact
     * address. False otherwise.
     */
    public boolean isFound()
    {
        return !StringUtils.isNullOrEmpty(resolvedName);
    }

}
