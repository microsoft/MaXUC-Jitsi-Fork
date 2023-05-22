/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Keeps a list of all calls currently active and maintained by this protocol
 * provider. Offers methods for finding a call by its ID, peer session
 * and others. This class is meant for use by protocol implementations and
 * cannot be accessed from other bundles.
 * <p>
 * Access that modifies the list of active calls must be synchronized on
 * <tt>this</tt>.
 *
 * @param <T> <tt>Call</tt>
 * @param <U> <tt>OperationSetBasicTelephony</tt>
 * @author Emil Ivov
 */
public abstract class ActiveCallsRepository<T extends Call,
        U extends OperationSetBasicTelephony<? extends ProtocolProviderService>>
    extends CallChangeAdapter
{
    /**
     * The <tt>Logger</tt> used by the <tt>ActiveCallsRepository</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ActiveCallsRepository.class);

    /**
     * A table mapping call ids against call instances.
     */
    private final Hashtable<String, T> activeCalls = new Hashtable<>();

    /**
     * The operation set that created us. Instance is mainly used for firing
     * events when necessary.
     */
    protected final U parentOperationSet;

    /**
     * Creates a new instance of this repository.
     *
     * @param opSet a reference to the
     * <tt>AbstractOperationSetBasicTelephony</tt> extension that created us.
     */
    public ActiveCallsRepository(U opSet)
    {
        this.parentOperationSet = opSet;
    }

    /**
     * Adds the specified call to the list of calls tracked by this repository.
     * @param call CallSipImpl
     */
    public synchronized void addCall(T call)
    {
        activeCalls.put(call.getCallID(), call);
        call.addCallChangeListener(this);
    }

    /**
     * If <tt>evt</tt> indicates that the call has been ended we remove it from
     * the repository.
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     * calls and its old and new state.
     */
    @Override
    public void callStateChanged(CallChangeEvent evt)
    {
        if (evt.getEventType().equals(CallChangeEvent.CALL_STATE_CHANGE) &&
            evt.getNewValue().equals(CallState.CALL_ENDED))
        {
            T sourceCall;
            synchronized (this)
            {
                sourceCall = activeCalls.remove(evt.getSourceCall().getCallID());
            }

            logger.trace("Removing call " + sourceCall + " from the list of active calls because it entered an ENDED state");
            fireCallEvent(CallEvent.CALL_ENDED, sourceCall);
        }
    }

    /**
     * Returns an iterator over all currently active (non-ended) calls.
     * NOTE this includes calls being initialized.
     *
     * @return an iterator over all currently active (non-ended) calls.
     */
    public Iterator<T> getActiveCalls()
    {
        synchronized(activeCalls)
        {
            /*
             * Given that we know the elements that will go into the new List,
             * it is more optimal in terms of memory and execution time to use
             * ArrayList rather than LinkedList.
             */
            return new ArrayList<>(activeCalls.values()).iterator();
        }
    }

    /**
     * Returns the number of calls currently tracked by this repository.
     *
     * @return the number of calls currently tracked by this repository.
     */
    public int getActiveCallCount()
    {
        synchronized (activeCalls)
        {
            return activeCalls.size();
        }
    }

    /**
     * Creates and dispatches a <tt>CallEvent</tt> notifying registered
     * listeners that an event with id <tt>eventID</tt> has occurred on
     * <tt>sourceCall</tt>.
     *
     * @param eventID the ID of the event to dispatch
     * @param sourceCall the call on which the event has occurred.
     */
    protected void fireCallEvent(int eventID, Call sourceCall)
    {
        fireCallEvent(eventID, sourceCall, null);
    }

    /**
     * Creates and dispatches a <tt>CallEvent</tt> notifying registered
     * listeners that an event with id <tt>eventID</tt> has occurred on
     * <tt>sourceCall</tt>.
     * <p>
     * TODO The method is ugly because it can be implemented if
     * <tt>parentOperationSet</tt> is an
     * <tt>AbstractOperationSetBasicTelephony</tt>. But after the move of the
     * latter in the <tt>.service.protocol.media</tt> package, it is not visible
     * here.
     * </p>
     *
     * @param eventID the ID of the event to dispatch
     * @param sourceCall the call on which the event has occurred
     * @param cause the <tt>CallChangeEvent</tt>, if any, which is the cause
     * that necessitated a new <tt>CallEvent</tt> to be fired
     */
    protected abstract void fireCallEvent(
            int eventID,
            Call sourceCall,
            CallChangeEvent cause);
}
