/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Date;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.bob.element.BoBIQ;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;

import net.java.sip.communicator.service.protocol.AbstractFileTransfer;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.IncomingFileTransferRequest;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.event.FileTransferCreatedEvent;
import net.java.sip.communicator.service.protocol.event.FileTransferRequestEvent;
import net.java.sip.communicator.util.Logger;
import org.jitsi.util.OSUtils;

/**
 * Jabber implementation of the incoming file transfer request
 *
 * @author Nicolas Riegel
 * @author Yana Stamcheva
 */
public class IncomingFileTransferRequestJabberImpl
    implements IncomingFileTransferRequest
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(IncomingFileTransferRequestJabberImpl.class);

    private String id;

    /**
     * The Jabber file transfer request.
     */
    private final FileTransferRequest fileTransferRequest;

    private final OperationSetFileTransferJabberImpl fileTransferOpSet;

    private final ProtocolProviderServiceJabberImpl jabberProvider;

    private Contact sender;

    private String thumbnailCid;

    private byte[] thumbnail;

    /**
     * Mac specific fileName pattern that matches "." if it's at the start of a line and every ":", "/".
     */
    private static final String MAC_INVALID_CHARS = "(^\\.)|([:/])";

    /**
     * Windows specific fileName pattern that matches all reserved characters "<>:"/\|?*".
     */
    private static final String WIN_INVALID_CHARS = "[<>:\"/\\\\|?*]";

    /**
     * Creates an <tt>IncomingFileTransferRequestJabberImpl</tt> based on the
     * given <tt>fileTransferRequest</tt>, coming from the Jabber protocol.
     *
     * @param jabberProvider the protocol provider
     * @param fileTransferOpSet file transfer operation set
     * @param fileTransferRequest the request coming from the Jabber protocol
     */
    public IncomingFileTransferRequestJabberImpl(
        ProtocolProviderServiceJabberImpl jabberProvider,
        OperationSetFileTransferJabberImpl fileTransferOpSet,
        FileTransferRequest fileTransferRequest)
    {
        this.jabberProvider = jabberProvider;
        this.fileTransferOpSet = fileTransferOpSet;
        this.fileTransferRequest = fileTransferRequest;

        String fromUserID
            = fileTransferRequest.getRequestor().asBareJid().toString();

        OperationSetPersistentPresenceJabberImpl opSetPersPresence
            = (OperationSetPersistentPresenceJabberImpl)
                jabberProvider
                    .getOperationSet(OperationSetPersistentPresence.class);

        sender = opSetPersPresence.findContactByID(fromUserID);

        this.id = String.valueOf( System.currentTimeMillis())
                    + String.valueOf(hashCode());

        ActiveFileTransferStore.addActiveFileTransfer(this.id, this);
    }

    /**
     * Returns the <tt>Contact</tt> making this request.
     *
     * @return the <tt>Contact</tt> making this request
     */
    public Contact getSender()
    {
        return sender;
    }

    /**
     * Returns the name of the file corresponding to this request.
     *
     * @return the name of the file corresponding to this request
     */
    public String getFileName()
    {
        String fileName = fileTransferRequest.getFileName();

        // OS specific file naming conventions
        fileName = fileName.replaceAll(OSUtils.isWindows() ? WIN_INVALID_CHARS : MAC_INVALID_CHARS, "_");

        // Just in case, validate the fileName and restrict it to a small range
        // of ASCII characters if InvalidPathException is caught.
        try
        {
            Paths.get(fileName);
        }
        catch (InvalidPathException e)
        {
            fileName = fileName.replaceAll("[^ a-zA-Z0-9.-]", "_");
        }

        return fileName;
    }

    /**
     * Returns the size of the file corresponding to this request.
     *
     * @return the size of the file corresponding to this request
     */
    public long getFileSize()
    {
        return fileTransferRequest.getFileSize();
    }

    /**
     * Accepts the file and starts the transfer.
     *
     * @return a boolean : <code>false</code> if the transfer fails,
     * <code>true</code> otherwise
     */
    public FileTransfer acceptFile(File file)
    {
        AbstractFileTransfer incomingTransfer = null;

        IncomingFileTransfer jabberTransfer = fileTransferRequest.accept();
        try
        {
            incomingTransfer
                = new IncomingFileTransferJabberImpl(
                        id, sender, file, jabberTransfer);

            FileTransferCreatedEvent event
                = new FileTransferCreatedEvent(incomingTransfer, new Date());

            fileTransferOpSet.fireFileTransferCreated(event);

            jabberTransfer.receiveFile(file);

            Thread fileTransferProgressThread = new OperationSetFileTransferJabberImpl
                .FileTransferProgressThread(jabberTransfer, incomingTransfer, getFileSize());
            JabberActivator.getThreadingService().submit("incomingFileTransferProgressThread",
                                                         fileTransferProgressThread);
        }
        catch (SmackException | IOException e)
        {
            logger.debug("Receiving file failed.", e);
        }

        return incomingTransfer;
    }

    /**
     * Refuses the file transfer request, and conditionally fires the rejected event.
     */
    public void rejectFile(boolean alwaysFireEvent)
    {
        boolean fireEvent = alwaysFireEvent;
        try
        {
            fileTransferRequest.reject();
            fireEvent = true;
        }
        catch (NotConnectedException | InterruptedException e)
        {
            logger.debug("Rejecting file failed.", e);
        }
        finally
        {
            if (fireEvent)
            {
                fileTransferOpSet.fireFileTransferRequestRejected(
                new FileTransferRequestEvent(fileTransferOpSet, this, new Date()));
            }
        }
    }

    /**
     * The unique id.
     * @return the id.
     */
    public String getID()
    {
        return id;
    }

    /**
     * Returns the thumbnail contained in this request.
     *
     * @return the thumbnail contained in this request
     */
    public byte[] getThumbnail()
    {
        return thumbnail;
    }

    /**
     * Sets the thumbnail content-ID.
     * @param cid the thumbnail content-ID
     */
    public void createThumbnailListeners(String cid)
    {
        this.thumbnailCid = cid;

        if (jabberProvider.getConnection() != null)
        {
            jabberProvider.getConnection().addStanzaListener(
                new ThumbnailResponseListener(),
                new AndFilter(  new StanzaTypeFilter(IQ.class),
                                IQTypeFilter.RESULT));
        }
    }

    /**
     * The <tt>ThumbnailResponseListener</tt> listens for events triggered by
     * the reception of a <tt>ThumbnailIQ</tt> packet. The packet is examined
     * and a file transfer request event is fired when the thumbnail is
     * extracted.
     */
    private class ThumbnailResponseListener implements StanzaListener
    {
        public void processStanza(Stanza stanza)
        {
            // If this is not an IQ packet, we're not interested.
            if (!(stanza instanceof BoBIQ))
                return;

            logger.debug("Thumbnail response received.");

            BoBIQ thumbnailResponse = (BoBIQ) stanza;

            if (thumbnailResponse.getContentId() != null
                && thumbnailResponse.getContentId().getCid().equals(thumbnailCid))
            {
                thumbnail = thumbnailResponse.getBoBData().getContent();

                // Create an event associated to this global request.
                FileTransferRequestEvent fileTransferRequestEvent
                = new FileTransferRequestEvent(
                    fileTransferOpSet,
                    IncomingFileTransferRequestJabberImpl.this,
                    new Date());

                // Notify the global listener that a request has arrived.
                fileTransferOpSet.fireFileTransferRequestThumbnailUpdate(
                    fileTransferRequestEvent);
            }

            if (jabberProvider.getConnection() != null)
            {
                jabberProvider.getConnection()
                    .removeStanzaListener(this);
            }
        }
    }
}
