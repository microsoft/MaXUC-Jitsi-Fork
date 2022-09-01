// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.Enumeration;
import java.util.Hashtable;

import net.java.sip.communicator.service.protocol.AbstractFileTransfer;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.IncomingFileTransferRequest;
import net.java.sip.communicator.service.protocol.event.FileTransferStatusChangeEvent;
import net.java.sip.communicator.util.Logger;

/** Class that stores and serves all active file transfers by UID. */
public class ActiveFileTransferStore
{
    private static final Logger sLog = Logger.getLogger(ActiveFileTransferStore.class);

    /** Stores all active file transfers by their identifiers. */
    private static final Hashtable<String, Object> sActiveFileTransfers = new Hashtable<>();

    private ActiveFileTransferStore() {}

    /**
     * Adds the given file transfer <tt>id</tt> to the list of active file
     * transfers.
     *
     * @param id the identifier of the file transfer to add
     * @param fileTransfer the descriptor of the file transfer
     */
    public static synchronized void addActiveFileTransfer(String id, Object fileTransfer)
    {
        sLog.debug("Adding file transfer with ID: " + id);
        sActiveFileTransfers.put(id, fileTransfer);
    }

    /**
     * Removes the given file transfer <tt>id</tt> from the list of active
     * file transfers.
     *
     * @param id the identifier of the file transfer to remove
     */
    public static synchronized void removeActiveFileTransfer(String id)
    {
        sLog.debug("Removing file transfer with ID: " + id);
        sActiveFileTransfers.remove(id);
    }

    /**
     * Returns the file transfer
     *
     * @param id the identifier of the file transfer to add
     */
    public static synchronized Object getFileTransferByID(String id)
    {
        sLog.debug("Retrieving file transfer with ID: " + id);
        return sActiveFileTransfers.get(id);
    }

    /** Fail all active file transfers */
    public static synchronized void failAllActiveFileTransfers()
    {
        sLog.debug("Failing all active file transfers, count: " + sActiveFileTransfers.size());
        Enumeration<String> activeKeys = sActiveFileTransfers.keys();

        while (activeKeys.hasMoreElements())
        {
            String key = activeKeys.nextElement();
            Object descriptor = sActiveFileTransfers.get(key);

            if (descriptor instanceof IncomingFileTransferRequest)
            {
                ((IncomingFileTransferRequest) descriptor).rejectFile(true);
            }
            else if (descriptor instanceof FileTransfer)
            {
                ((AbstractFileTransfer) descriptor).fireStatusChangeEvent(FileTransferStatusChangeEvent.FAILED);
            }
        }
    }
}