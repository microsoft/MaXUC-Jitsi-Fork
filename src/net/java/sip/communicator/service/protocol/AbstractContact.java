/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.ContactLogger;

import org.jitsi.service.resources.*;

/**
 * An abstract base implementation of the {@link Contact} interface which is to
 * aid implementers.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractContact
    implements Contact
{
    /**
     * The set of <tt>ContactResourceListener</tt>-s registered in this
     * contact.
     */
    private final Set<ContactResourceListener> resourceListeners =
            new HashSet<>();

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        else if (obj == this)
            return true;
        else if (!obj.getClass().equals(getClass()))
            return false;
        else
        {
            Contact contact = (Contact) obj;
            ProtocolProviderService protocolProvider
                = contact.getProtocolProvider();
            ProtocolProviderService thisProtocolProvider
                = getProtocolProvider();

            if (Objects.equals(protocolProvider, thisProtocolProvider))
            {
                String address = contact.getAddress();
                String thisAddress = getAddress();

                return Objects.equals(address, thisAddress);
            }
            else
                return false;
        }
    }

    @Override
    public int hashCode()
    {
        int hashCode = 0;

        ProtocolProviderService protocolProvider = getProtocolProvider();

        if (protocolProvider != null)
            hashCode += protocolProvider.hashCode();

        String address = getAddress();

        if (address != null)
            hashCode += address.hashCode();

        return hashCode;
    }

    /**
     * Indicates if this contact supports resources.
     * <p>
     * This default implementation indicates no support for contact resources.
     *
     * @return <tt>true</tt> if this contact supports resources, <tt>false</tt>
     * otherwise
     */
    public boolean supportResources()
    {
        return false;
    }

    /**
     * Returns a collection of resources supported by this contact or null
     * if it doesn't support resources.
     * <p>
     * This default implementation indicates no support for contact resources.
     *
     * @return a collection of resources supported by this contact or null
     * if it doesn't support resources
     */
    public Collection<ContactResource> getResources()
    {
        return null;
    }

    /**
     * Adds the given <tt>ContactResourceListener</tt> to listen for events
     * related to contact resources changes.
     *
     * @param l the <tt>ContactResourceListener</tt> to add
     */
    public void addResourceListener(ContactResourceListener l)
    {
        synchronized (resourceListeners)
        {
            ContactLogger.getLogger().debug(this, "Adding resourceListener: " + l);
            resourceListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>ContactResourceListener</tt> listening for events
     * related to contact resources changes.
     *
     * @param l the <tt>ContactResourceListener</tt> to remove
     */
    public void removeResourceListener(ContactResourceListener l)
    {
        synchronized (resourceListeners)
        {
            ContactLogger.getLogger().debug(this, "Removing resourceListener: " + l);
            resourceListeners.remove(l);
        }
    }

    /**
     * Notifies all registered <tt>ContactResourceListener</tt>s that an event
     * has occurred.
     *
     * @param event the <tt>ContactResourceEvent</tt> to fire notification for
     */
    protected void fireContactResourceEvent(ContactResourceEvent event)
    {
        ArrayList<ContactResourceListener> listeners;
        synchronized (resourceListeners)
        {
            int count = resourceListeners.size();
            if (count == 0)
            {
                ContactLogger.getLogger().debug(this,
                    "Not firing <" + event + "> - no listeners registered.");
                return;
            }

            listeners = new ArrayList<>(resourceListeners);
            ContactLogger.getLogger().info(this,
                "Firing <" + event + "> to " + count + " listeners.");
        }

        for (ContactResourceListener listener : listeners)
        {
            int lEventType = event.getEventType();

            switch (lEventType)
            {
                case ContactResourceEvent.RESOURCE_ADDED:
                    listener.contactResourceAdded(event);
                    break;
                case ContactResourceEvent.RESOURCE_REMOVED:
                    listener.contactResourceRemoved(event);
                    break;
                case ContactResourceEvent.RESOURCE_MODIFIED:
                    listener.contactResourceModified(event);
            }
        }
    }

    public ImageIconFuture getOverlay(Dimension size)
    {
        // By default, overlays are not supported
        return null;
    }

    @Override
    public boolean supportsIMAutoPopulation()
    {
        // Currently only BGContacts support IM autopopulation.
        return false;
    }
}
