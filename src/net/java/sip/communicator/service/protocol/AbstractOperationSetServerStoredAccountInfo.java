/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of
 * {@link OperationSetServerStoredAccountInfo} in order to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific details.
 *
 * @author Damian Minkov
 */
public abstract class AbstractOperationSetServerStoredAccountInfo
    implements OperationSetServerStoredAccountInfo
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>AbstractOperationSetPersistentPresence</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger =
        Logger.getLogger(AbstractOperationSetServerStoredAccountInfo.class);

    /**
     * A list of listeners registered for
     * <tt>ServerStoredDetailsChangeListener</tt>s.
     */
    private final List<ServerStoredDetailsChangeListener>
        serverStoredDetailsListeners
            = new ArrayList<>();

    /**
     * Registers a ServerStoredDetailsChangeListener with this operation set so
     * that it gets notifications of details change.
     *
     * @param listener the <tt>ServerStoredDetailsChangeListener</tt>
     * to register.
     */
    public void addServerStoredDetailsChangeListener(
            ServerStoredDetailsChangeListener listener)
    {
        synchronized (serverStoredDetailsListeners)
        {
            if (!serverStoredDetailsListeners.contains(listener))
                serverStoredDetailsListeners.add(listener);
        }
    }

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon details change.
     *
     * @param listener the <tt>ServerStoredDetailsChangeListener</tt>
     * to unregister.
     */
    public void removeServerStoredDetailsChangeListener(
            ServerStoredDetailsChangeListener listener)
    {
        synchronized (serverStoredDetailsListeners)
        {
            serverStoredDetailsListeners.remove(listener);
        }
    }

    /**
     * Notify all listeners of the corresponding account detail
     * change event.
     *
     * @param source the protocol provider service source
     * @param eventID the int ID of the event to dispatch
     * @param oldValue the value that the changed property had before the change
     *            occurred.
     * @param newValue the value that the changed property currently has (after
     *            the change has occurred).
     */
    public void fireServerStoredDetailsChangeEvent(
            ProtocolProviderService source,
            int eventID, Object oldValue, Object newValue)
    {
        ServerStoredDetailsChangeEvent evt
            = new ServerStoredDetailsChangeEvent(
                    source,
                    eventID,
                    oldValue,
                    newValue);

        Collection<ServerStoredDetailsChangeListener> listeners;
        synchronized (serverStoredDetailsListeners)
        {
            listeners = new ArrayList<>(
                    serverStoredDetailsListeners);
        }

        logger.debug("Dispatching a Contact Property Change Event to"
            + listeners.size() + " listeners. Evt=" + evt);

        for (ServerStoredDetailsChangeListener listener : listeners)
            listener.serverStoredDetailsChanged(evt);
    }
}
