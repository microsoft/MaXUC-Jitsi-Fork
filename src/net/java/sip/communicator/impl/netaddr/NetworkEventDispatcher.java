/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import java.util.*;

import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.util.*;

/**
 * The class implements a dispatch event thread. The thread will
 * fire event every time it is added through the <tt>fireChangeEvent()</tt>
 * method and would then deliver it to a registered listener if any.
 * (No fire would be performed until we have a
 * <tt>NetworkConfigurationChangeListener</tt>). If the event has time set
 * we used it as a delay before dispatching the event.
 * <p>
 *
 * @author Damian Minkov
 */
public class NetworkEventDispatcher
    implements Runnable
{
    /**
     * Our class logger.
     */
    private static Logger logger =
        Logger.getLogger(NetworkEventDispatcher.class);

    /**
     * Listeners for network configuration changes.
     */
    private final List<NetworkConfigurationChangeListener> listeners =
            new ArrayList<>();

    /**
     * The events to dispatch.
     */
    private final Map<ChangeEvent, Integer> eventsToDispatch
            = new LinkedHashMap<>();

    /**
     * start/stop indicator.
     */
    private boolean stopped = true;

    /**
     * The thread that runs this dispatcher.
     */
    private Thread dispatcherThread = null;

    /**
     * Adds new <tt>NetworkConfigurationChangeListener</tt> which will
     * be informed for network configuration changes.
     * @param listener the listener.
     */
    void addNetworkConfigurationChangeListener(
        NetworkConfigurationChangeListener listener)
    {
        synchronized(listeners)
        {
            if(!listeners.contains(listener))
            {
                listeners.add(listener);

                if(dispatcherThread == null)
                {
                    dispatcherThread = new Thread(this, "NetworkEventDispatcher");
                    dispatcherThread.start();
                }
            }
        }
    }

    /**
     * Remove <tt>NetworkConfigurationChangeListener</tt>.
     * @param listener the listener.
     */
    void removeNetworkConfigurationChangeListener(
        NetworkConfigurationChangeListener listener)
    {
        synchronized(listeners)
        {
            listeners.remove(listener);
        }
    }

    /**
     * Fire ChangeEvent.
     * @param evt the event to fire.
     */
    protected void fireChangeEvent(ChangeEvent evt)
    {
        this.fireChangeEvent(evt, 0);
    }

    /**
     * Fire ChangeEvent.
     * @param evt the event to fire.
     */
    protected void fireChangeEvent(ChangeEvent evt, int wait)
    {
        synchronized(eventsToDispatch)
        {
            eventsToDispatch.put(evt, wait);

            eventsToDispatch.notifyAll();

            if(dispatcherThread == null && listeners.size() > 0)
            {
                dispatcherThread = new Thread(this, "NetworkEventDispatcher");
                dispatcherThread.start();
            }
        }
    }

    /**
     * Fire ChangeEvent.
     * @param evt the event to fire.
     */
    static void fireChangeEvent(ChangeEvent evt,
                                 NetworkConfigurationChangeListener listener)
    {
        try
        {
            logger.trace("firing event to " + listener + " evt=" + evt);

            listener.configurationChanged(evt);
        } catch (Throwable e)
        {
            logger
                .warn("Error delivering event:" + evt + ", to:" + listener, e);
        }
    }

    /**
     * Runs the waiting thread.
     */
    public void run()
    {
        try
        {
            stopped = false;

            while(!stopped)
            {
                Map.Entry<ChangeEvent, Integer> eventToProcess = null;
                List<NetworkConfigurationChangeListener> listenersCopy;

                synchronized(eventsToDispatch)
                {
                    if(eventsToDispatch.size() == 0)
                    {
                        try {
                            eventsToDispatch.wait();
                        }
                        catch (InterruptedException iex){}
                    }

                    //no point in dispatching if there's no one
                    //listening
                    if (listeners.size() == 0)
                        continue;

                    //store the ref of the listener in case someone resets
                    //it before we've had a chance to notify it.
                    listenersCopy = new ArrayList
                            <>(listeners);

                    Iterator<Map.Entry<ChangeEvent, Integer>> iter =
                            eventsToDispatch.entrySet().iterator();
                    if(iter.hasNext())
                    {
                        eventToProcess = iter.next();
                        iter.remove();
                    }
                }

                if(eventToProcess != null && listenersCopy != null)
                {
                    if(eventToProcess.getValue() > 0)
                        synchronized(this)
                        {
                            try{
                                wait(eventToProcess.getValue());
                            }catch(Throwable t){}
                        }

                    logger.info("Network ChangeEvent fired: " +
                                                       eventToProcess.getKey());
                    for (int i = 0; i < listenersCopy.size(); i++)
                    {
                        fireChangeEvent(eventToProcess.getKey(),
                                        listenersCopy.get(i));
                    }
                }

                eventToProcess = null;
                listenersCopy = null;
            }
        } catch(Throwable t)
        {
            logger.error("Error dispatching thread ended unexpectedly", t);
        }
    }

    /**
     * Interrupts this dispatcher so that it would no longer disptach events.
     */
    public void stop()
    {
        synchronized(eventsToDispatch)
        {
            stopped = true;
            eventsToDispatch.notifyAll();

            dispatcherThread = null;
        }
    }

    /**
     * Returns <tt>true</tt> if this dispatcher is currently running and
     * delivering events when available and <tt>false</tt>
     * otherwise.
     *
     * @return <tt>true</tt> if this dispatcher is currently running and
     * delivering events when available and <tt>false</tt>
     * otherwise.
     */
    public boolean isRunning()
    {
        return !stopped;
    }
}
