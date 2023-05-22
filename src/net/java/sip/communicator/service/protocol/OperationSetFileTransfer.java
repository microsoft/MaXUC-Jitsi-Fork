/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.io.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * The File Transfer Operation Set provides an interface towards those functions
 * of a given protocol, that allow transferring files among users.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface OperationSetFileTransfer
    extends OperationSet
{
    /**
     * Attempt to get a thumbnail for a file from the active file transfer store.
     * Returns null if unsuccessful
     *
     * @param transferId The id of the transfer we want to get a thumbnail for
     */
    byte[] getThumbnail(String transferId);

    /**
     * Attempt to create a thumbnail for a file, provided it is an image.
     * Returns null if unsuccessful
     *
     * @param file The file we want to create a thumbnail for
     */
    byte[] createThumbnail(File file);

    /**
     * Prepare and send the requested file.
     *
     * @param imContact The contact to send the file to
     * @param filePath The path to the file we wish to send
     * @param transferId The id of the file transfer if retrying
     */
    void sendFile(Contact imContact,
                  String filePath,
                  String transferId);

    /**
     * Sends the specified local file to the given <tt>ContactResource</tt> if
     * provided or to the given <tt>Contact</tt> if no <tt>ContactResource</tt>
     * is specified.
     *
     * @param toContactResource the resource that should receive the file
     * @param toContact the contact that should receive the file
     * @param file the file to send
     * @param transferId the unique identifier of this file transfer
     * @return the transfer object
     *
     * @throws IllegalStateException if the protocol provider is not registered
     * or connected
     * @throws IllegalArgumentException if some of the arguments doesn't fit the
     * protocol requirements
     * @throws OperationNotSupportedException if the given contact client or
     * server does not support file transfers
     */
    FileTransfer sendFile(ContactResource toContactResource,
                          Contact toContact,
                          File file,
                          String transferId)
        throws  IllegalStateException,
                IllegalArgumentException,
                OperationNotSupportedException;

    /**
     * Accept the file from an incoming file transfer.
     *
     * @param uid The UID of the transfer to accept
     */
    void acceptFileTransfer(String uid);

    /**
     * Cancel the file transfer.
     *
     * @param uid The UID of the transfer to cancel
     */
    void cancelFileTransfer(String uid);

    /**
     * Adds the given <tt>FileTransferListener</tt> that would listen for
     * file transfer requests and created file transfers.
     *
     * @param listener the <tt>FileTransferListener</tt> to add
     */
    void addFileTransferListener(
            FileTransferListener listener);

    /**
     * Removes the given <tt>FileTransferListener</tt> that listens for
     * file transfer requests and created file transfers.
     *
     * @param listener the <tt>FileTransferListener</tt> to remove
     */
    void removeFileTransferListener(
            FileTransferListener listener);

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * @return the file length that is supported.
     */
    long getMaximumFileLength();
}
