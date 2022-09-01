/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.mock;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A mock implementation of a basic telephony operation set
 *
 * @author Damian Minkov
 */
public class MockOperationSetFileTransfer
    implements OperationSetFileTransfer
{
    /**
     * A list of listeners registered for file transfer events.
     */
    private final Vector<FileTransferListener> fileTransferListeners
        = new Vector<>();

    public MockOperationSetFileTransfer()
    {
    }

    /**
     * Sends a file transfer request to the given <tt>toContact</tt> by
     * specifying the local and remote file path and the <tt>fromContact</tt>,
     * sending the file.
     *
     * @return the transfer object
     *
     * @param toContact the contact that should receive the file
     * @param file file to send
     */
    public FileTransfer sendFile(Contact toContact, File file)
        throws IllegalStateException,
               IllegalArgumentException
    {
        return sendFile(null, toContact, file, null);
    }

    public FileTransfer sendFile(ContactResource toContactResource,
                                 Contact toContact,
                                 File file,
                                 String transferId)
        throws IllegalStateException,
               IllegalArgumentException
    {
        MockFileTransferImpl fileTrans = new MockFileTransferImpl(
            toContact,
            file,
            transferId == null ? generateID() : transferId,
            FileTransfer.OUT);

        fireFileTransferCreated(new FileTransferCreatedEvent(fileTrans, new Date()));

        changeFileTransferStatus(fileTrans, FileTransferStatusChangeEvent.PREPARING);

        return fileTrans;
    }

    public void changeFileTransferStatus(FileTransfer ft, int newstatus)
    {
        ((MockFileTransferImpl)ft).fireStatusChangeEvent(newstatus);
    }

    private String generateID()
    {
        // This must be unique, but the test scripts can generate multiple per
        // millisecond, so add a random suffix to ensure uniqueness.
        String id = String.valueOf( System.currentTimeMillis()) + "-" +
            String.valueOf(hashCode()) +
            UUID.randomUUID().toString().substring(0, 8);
        return id;
    }

    public void receiveFile(final File file,
        final Contact from)
    {
        final Date requestDate = new Date();

        final String id = generateID();

        fireFileTransferRequest(
            new FileTransferRequestEvent(
                this,
                new IncomingFileTransferRequest()
                {
                    public String getID()
                    {
                        return id;
                    }

                    public String getFileName()
                    {
                        return file.getName();
                    }

                    public String getFileDescription()
                    {
                        return file.toString();
                    }

                    public long getFileSize()
                    {
                        return file.length();
                    }

                    public Contact getSender()
                    {
                        return from;
                    }

                    public FileTransfer acceptFile(File file)
                    {
                        MockFileTransferImpl fileTrans =
                            new MockFileTransferImpl(
                                    from,
                                    file,
                                    id,
                                    FileTransfer.IN);

                        fireFileTransferCreated(
                            new FileTransferCreatedEvent(fileTrans, requestDate));

                        changeFileTransferStatus(fileTrans,
                            FileTransferStatusChangeEvent.PREPARING);

                        return fileTrans;
                    }

                    public void rejectFile(boolean alwaysFireEvent)
                    {
                    }

                    public byte[] getThumbnail()
                    {
                        return null;
                    }
                }, requestDate));
    }

    /**
     * Sends a file transfer request to the given <tt>toContact</tt> by
     * specifying the local and remote file path and the <tt>fromContact</tt>,
     * sending the file.
     *
     * @return the transfer object
     *
     * @param toContact the contact that should receive the file
     * @param fromContact the contact sending the file
     * @param remotePath the remote file path
     * @param localPath the local file path
     */
    public FileTransfer sendFile(Contact toContact, Contact fromContact, String remotePath, String localPath)
        throws IllegalStateException,
               IllegalArgumentException
    {
        return this.sendFile(toContact, new File(localPath));
    }

    /**
     * Adds the given <tt>FileTransferListener</tt> that would listen for
     * file transfer requests and created file transfers.
     *
     * @param listener the <tt>FileTransferListener</tt> to add
     */
    public void addFileTransferListener(FileTransferListener listener)
    {
        synchronized(fileTransferListeners)
        {
            if(!fileTransferListeners.contains(listener))
            {
                this.fileTransferListeners.add(listener);
            }
        }
    }

    /**
     * Removes the given <tt>FileTransferListener</tt> that listens for
     * file transfer requests and created file transfers.
     *
     * @param listener the <tt>FileTransferListener</tt> to remove
     */
    public void removeFileTransferListener(FileTransferListener listener)
    {
        synchronized(fileTransferListeners)
        {
            this.fileTransferListeners.remove(listener);
        }
    }

    /**
     * Delivers the specified event to all registered file transfer listeners.
     *
     * @param event the <tt>EventObject</tt> that we'd like delivered to all
     * registered file transfer listeners.
     */
    private void fireFileTransferRequest(FileTransferRequestEvent event)
    {
        Iterator<FileTransferListener> listeners = null;
        synchronized (fileTransferListeners)
        {
            listeners = new ArrayList<>
                    (fileTransferListeners).iterator();
        }

        while (listeners.hasNext())
        {
            FileTransferListener listener = listeners.next();

            listener.fileTransferRequestReceived(event);
        }
    }

    /**
     * Delivers the file transfer to all registered listeners.
     *
     * @param event the <tt>FileTransferEvent</tt> that we'd like delivered to
     * all registered file transfer listeners.
     */
    void fireFileTransferCreated(FileTransferCreatedEvent event)
    {
        Iterator<FileTransferListener> listeners = null;
        synchronized (fileTransferListeners)
        {
            listeners = new ArrayList<>
                    (fileTransferListeners).iterator();
        }

        while (listeners.hasNext())
        {
            FileTransferListener listener = listeners.next();

            listener.fileTransferCreated(event);
        }
    }

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        return 2048*1024*1024;
    }

    @Override
    public byte[] getThumbnail(String transferId)
    {
        // Do nothing
        return null;
    }

    @Override
    public byte[] createThumbnail(File file)
    {
        // Do nothing
        return null;
    }

    @Override
    public void sendFile(Contact imContact, String filePath, String transferId)
    {
        // Do nothing
    }

    @Override
    public void acceptFileTransfer(String uid)
    {
        // Do nothing
    }

    @Override
    public void cancelFileTransfer(String uid)
    {
        // Do nothing
    }
}
