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
 * An abstract implementation of the <tt>FileTransfer</tt> interface providing
 * implementation of status and progress events related methods and leaving all
 * protocol specific methods abstract. A protocol specific implementation could
 * extend this class and implement only <tt>cancel()</tt> and
 * <tt>getTransferredBytes()</tt>.
 *
 * @author Yana Stamcheva
 */
public abstract class AbstractFileTransfer
    implements FileTransfer
{
    private static final Logger logger =
        Logger.getLogger(AbstractFileTransfer.class);

    /**
     * A list of listeners registered for file transfer status events.
     */
    private final Vector<FileTransferStatusListener> statusListeners
        = new Vector<>();

    /**
     * A list of listeners registered for file transfer status events.
     */
    private final Vector<FileTransferProgressListener> progressListeners
        = new Vector<>();

    private int status;

    /**
     * Adds the given <tt>FileTransferProgressListener</tt> to listen for
     * status changes on this file transfer.
     *
     * @param listener the listener to add
     */
    public void addProgressListener(FileTransferProgressListener listener)
    {
        synchronized(progressListeners)
        {
            if(!progressListeners.contains(listener))
            {
                this.progressListeners.add(listener);
            }
        }
    }

    /**
     * Adds the given <tt>FileTransferStatusListener</tt> to listen for
     * status changes on this file transfer.
     *
     * @param listener the listener to add
     */
    public void addStatusListener(FileTransferStatusListener listener)
    {
        synchronized(statusListeners)
        {
            if(!statusListeners.contains(listener))
            {
                this.statusListeners.add(listener);
            }
        }
    }

    /**
     * Removes the given <tt>FileTransferStatusListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeStatusListener(FileTransferStatusListener listener)
    {
        synchronized(statusListeners)
        {
            this.statusListeners.remove(listener);
        }
    }

    /**
     * Returns the current status of the transfer. This information could be
     * used from the user interface to show a progress bar indicating the
     * file transfer status.
     *
     * @return the current status of the transfer
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * Notifies all status listeners that a new
     * <tt>FileTransferStatusChangeEvent</tt> occured.
     * @param newStatus the new status
     */
    public void fireStatusChangeEvent(int newStatus)
    {
        this.fireStatusChangeEvent(newStatus, null);
    }

    /**
     * Notifies all status listeners that a new
     * <tt>FileTransferStatusChangeEvent</tt> occured.
     * @param newStatus the new status
     * @param reason the reason of the status change
     */
    public void fireStatusChangeEvent(int newStatus, String reason)
    {
        // ignore if status is the same
        if(this.status == newStatus)
            return;

        Collection<FileTransferStatusListener> listeners = null;
        synchronized (statusListeners)
        {
            listeners
                = new ArrayList<>(statusListeners);
        }

        logger.debug("Dispatching a FileTransfer Event to" + listeners.size()
            + " listeners. Status=" + status);

        FileTransferStatusChangeEvent statusEvent
            = new FileTransferStatusChangeEvent(
                this, status, newStatus, reason);

        // Updates the status.
        this.status = newStatus;

        Iterator<FileTransferStatusListener> listenersIter
            = listeners.iterator();

        while (listenersIter.hasNext())
        {
            FileTransferStatusListener statusListener = listenersIter.next();

            statusListener.statusChanged(statusEvent);
        }
    }

    /**
     * Notifies all status listeners that a new
     * <tt>FileTransferProgressEvent</tt> occured.
     * @param timestamp the date on which the event occured
     * @param progress the bytes representing the progress of the transfer
     */
    public void fireProgressChangeEvent(long timestamp, long progress)
    {
        Collection<FileTransferProgressListener> listeners = null;
        synchronized (progressListeners)
        {
            listeners
                = new ArrayList<>(progressListeners);
        }

        FileTransferProgressEvent progressEvent
            = new FileTransferProgressEvent(this, timestamp, progress);

        Iterator<FileTransferProgressListener> listenersIter
            = listeners.iterator();

        while (listenersIter.hasNext())
        {
            FileTransferProgressListener statusListener = listenersIter.next();

            statusListener.progressChanged(progressEvent);
        }
    }
}
