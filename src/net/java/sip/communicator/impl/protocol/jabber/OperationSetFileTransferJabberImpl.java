/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.util.PrivacyUtils.sanitisePeerId;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smackx.bob.element.BoBIQ;
import org.jivesoftware.smackx.bob.provider.BoBIQProvider;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Error;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail.ThumbnailElement;
import net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail.ThumbnailFile;
import net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail.ThumbnailStreamInitiationProvider;
import net.java.sip.communicator.plugin.desktoputil.ImageUtils;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.protocol.AbstractFileTransfer;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactResource;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.IncomingFileTransferRequest;
import net.java.sip.communicator.service.protocol.OperationNotSupportedException;
import net.java.sip.communicator.service.protocol.OperationSetFileTransfer;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetThumbnailedFileFactory;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.FileTransferCreatedEvent;
import net.java.sip.communicator.service.protocol.event.FileTransferListener;
import net.java.sip.communicator.service.protocol.event.FileTransferRequestEvent;
import net.java.sip.communicator.service.protocol.event.FileTransferStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.protocol.jabberconstants.JabberStatusEnum;
import net.java.sip.communicator.util.FileUtils;
import net.java.sip.communicator.util.Logger;

/**
 * The Jabber implementation of the <tt>OperationSetFileTransfer</tt>
 * interface.
 *
 * @author Gregory Bande
 * @author Nicolas Riegel
 * @author Yana Stamcheva
 */
public class OperationSetFileTransferJabberImpl
    implements OperationSetFileTransfer
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetFileTransferJabberImpl.class);

     /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * An active instance of the opSetPersPresence operation set.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * An active instance of the opSetThumbnailedFileFactory operation set.
     */
    private OperationSetThumbnailedFileFactoryImpl opSetThumbnail = null;

    /**
     * The Jabber file transfer manager.
     */
    private FileTransferManager manager = null;

    /**
     * The Jabber file transfer listener.
     */
    private FileTransferRequestListener fileTransferRequestListener;

    /**
     * A list of listeners registered for file transfer events.
     */
    private final Vector<FileTransferListener> fileTransferListeners
        = new Vector<>();

    // Register file transfer features on every established connection
    // to make sure we register them before creating our
    // ServiceDiscoveryManager
    static
    {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener()
        {
            public void connectionCreated(XMPPConnection connection)
            {
                FileTransferNegotiator.getInstanceFor(connection);
            }
        });
    }

    /**
     * String array to represent file transfer feature list.
     */
    private static final String[] FILE_TRANSFER_FEATURES = {
        "http://jabber.org/protocol/si",
        "http://jabber.org/protocol/si/profile/file-transfer"};

    /**
     * Constructor
     * @param provider is the provider that created us
     */
    public OperationSetFileTransferJabberImpl(
            ProtocolProviderServiceJabberImpl provider)
    {
        this.jabberProvider = provider;

        provider.addRegistrationStateChangeListener(
            new RegistrationStateListener());

        // Disable SOCKS5 (fast) file transfers
        FileTransferNegotiator.IBB_ONLY = true;
    }

    /**
     * Attempt to get a thumbnail for a file from the active file transfer store.
     * Returns null if unsuccessful
     *
     * @param transferId The id of the transfer we want to get a thumbnail for
     */
    public byte[] getThumbnail(String transferId)
    {
        byte[] thumbnail = null;
        Object fileTransferFromStore = ActiveFileTransferStore.getFileTransferByID(transferId);

        if (fileTransferFromStore instanceof IncomingFileTransferRequest)
        {
            thumbnail = ((IncomingFileTransferRequest) fileTransferFromStore).getThumbnail();
        }
        else if (fileTransferFromStore instanceof FileTransfer)
        {
            File file = ((FileTransfer) fileTransferFromStore).getLocalFile();
            if (file instanceof ThumbnailedFile)
            {
                thumbnail = ((ThumbnailedFile) file).getThumbnailData();
            }
        }

        return thumbnail;
    }

    /**
     * Attempt to create a thumbnail for a file, provided it is an image.
     * Returns null if unsuccessful
     *
     * @param file The file we want to create a thumbnail for
     */
    public byte[] createThumbnail(File file)
    {
        byte[] thumbnail = null;
        if (file != null && FileUtils.isImage(file.getName()))
        {
            try
            {
                BufferedImage image = ImageIO.read(file);

                if (image != null)
                {
                    thumbnail = ImageUtils.getScaledInstanceInBytes(image,
                                                                    64,
                                                                    64);
                }
            }
            catch (IOException e)
            {
                logger.debug("Could not locate image ", e);
            }
        }
        return thumbnail;
    }

    /**
     * Prepare and send the requested file.
     *
     * @param imContact The contact to send the file to
     * @param filePath The path to the file we wish to send
     * @param transferId The id of the file transfer if retrying
     */
    public void sendFile(Contact imContact,
                         String filePath,
                         String transferId)
    {
        // Create the File object
        File file = new File(filePath);

        // Create a thumbnailed file if possible (file is image and thumbnail factory is available).
        byte[] thumbnail = null;
        if (opSetThumbnail != null)
        {
            thumbnail = createThumbnail(file);
            if (thumbnail != null && thumbnail.length > 0)
            {
                file = opSetThumbnail.createFileWithThumbnail(file,
                                                              64,
                                                              64,
                                                              "image/png",
                                                              thumbnail);
            }
        }

        // Transfer UID
        String uid = transferId != null ? transferId : UUID.randomUUID().toString();

        // Work out contact resource
        ContactResource toContactResource = ((ContactJabberImpl) imContact).getLatestResource();

        // Send the file
        try
        {
            logger.debug("Sending file" + (thumbnail != null ? " with thumbnail" : ""));
            sendFile(toContactResource, imContact, file, uid);
            // Send analytic
            JabberActivator.getAnalyticsService().onEvent(AnalyticsEventType.SEND_FILE_STARTED);
        }
        catch (IllegalStateException | IllegalArgumentException | OperationNotSupportedException e)
        {
            logger.error("Failed to send file", e);
        }
    }

    public FileTransfer sendFile(ContactResource toContactResource,
                                 Contact toContact,
                                 File file,
                                 String transferId)
        throws  IllegalStateException,
                IllegalArgumentException,
                OperationNotSupportedException
    {
        OutgoingFileTransferJabberImpl outgoingTransfer = null;

        try
        {
            assertConnected();

            if(file.length() > getMaximumFileLength())
                throw new IllegalArgumentException(
                    "File length exceeds the allowed one for this protocol");

            Jid receiver;
            // If we've been provided with resource to send to, check it supports file transfer.
            // Otherwise, check whether any resources for this contact support file transfer.
            if (toContactResource != null)
            {
                receiver = JidCreate.fullFrom(toContact.getAddress()
                    + "/" + toContactResource.getResourceName());
                if (!jabberProvider.isFeatureListSupported(
                    receiver,
                    FILE_TRANSFER_FEATURES))
                {
                    logger.debug("This resource doesn't support file transfer");
                    throw new OperationNotSupportedException(
                        "Contact resource/server doesn't support file transfers");
                }
            }
            else
            {
                Iterator<Presence> iter =
                    Roster.getInstanceFor(jabberProvider.getConnection())
                        .getPresences(JidCreate.entityBareFrom(
                            toContact.getAddress())).iterator();

                Presence presence = null;
                while(iter.hasNext())
                {
                    presence = iter.next();
                    if(jabberProvider.isFeatureListSupported(
                        presence.getFrom(),
                        FILE_TRANSFER_FEATURES))
                    {
                        logger.debug("Found resource that supports file transfer");
                        break;
                    }
                }

                // If we didn't find a resource that supports file transfer,
                // throw an exception here.
                if (presence == null)
                {
                    throw new OperationNotSupportedException(
                        "Contact client/server doesn't support file transfers");
                }

                // It is forbidden to send file transfers to the bare JID of a
                // contact so we explicitly use the one we've just found that
                // supports file transfer.
                receiver = presence.getFrom();
            }

            logger.info("Sending file to " + sanitisePeerId(receiver));
            OutgoingFileTransfer transfer
                = manager.createOutgoingFileTransfer(JidCreate.entityFullFrom(receiver));

            outgoingTransfer
                = new OutgoingFileTransferJabberImpl(
                    toContact, file, transfer, transferId, jabberProvider);

            // Notify all interested listeners that a file transfer has been
            // created.
            FileTransferCreatedEvent event
                = new FileTransferCreatedEvent(outgoingTransfer, new Date());

            fireFileTransferCreated(event);

            // Set the response timeout to 24 hours
            OutgoingFileTransfer.setResponseTimeout(24 * 60 * 60 * 1000);

            // Send the file through the Jabber file transfer.
            transfer.sendFile(file, "Sending file");

            // Start the status and progress thread.
            Thread fileTransferProgressThread = new FileTransferProgressThread(transfer, outgoingTransfer);
            JabberActivator.getThreadingService()
                .submit("outgoingFileTransferProgressThread", fileTransferProgressThread);
        }
        catch(XmppStringprepException | SmackException e)
        {
            logger.error("Failed to send file.", e);
        }

        return outgoingTransfer;
    }

    /**
     * Accept the file from an incoming file transfer.
     *
     * @param uid The UID of the transfer to accept
     */
    public void acceptFileTransfer(String uid)
    {
        logger.debug("Accepting file transfer with UID: " + uid);
        // Find file transfer
        IncomingFileTransferRequest fileTransfer;
        try
        {
            fileTransfer = (IncomingFileTransferRequest) ActiveFileTransferStore.getFileTransferByID(uid);
        }
        catch (ClassCastException e)
        {
            logger.warn("Trying to accept a transfer already accepted");
            return;
        }

        if (fileTransfer == null)
        {
            logger.warn("File transfer doesn't exist to accept");
            return;
        }

        // Create file in the location we want to download to (Downloads folder)
        File downloadsFolder = JabberActivator.getFileAccessService().getDefaultDownloadDirectory();
        // Create downloads folder if it doesn't exist
        if (!downloadsFolder.exists())
        {
            if (!downloadsFolder.mkdirs())
            {
                logger.error("Could not create the download directory");
            }
            logger.debug("Download directory created");
        }

        String fileName = fileTransfer.getFileName();
        File downloadFile = new File(downloadsFolder, fileName);

        // If a file with the given name already exists, add an index to the
        // file name.
        int index = 0;
        while (downloadFile.exists())
        {
            int lastDot = fileName.lastIndexOf(".");
            String newFileName;

            if (lastDot != -1)
            {
                // Convert "abc.txt" to "abc-1.txt"
                newFileName = fileName.substring(0, lastDot) +
                            "-" + ++index +
                            fileName.substring(lastDot);
            }
            else
            {
                // Convert "abc" to "abc-1";
                newFileName = fileName + "-" + ++index;
            }

            downloadFile = new File(downloadsFolder, newFileName);
        }

        // Add a thumbnail to the file if it exists and thumbnail factory is available.
        byte[] thumbnail = fileTransfer.getThumbnail();
        if (thumbnail != null && thumbnail.length > 0 && opSetThumbnail != null)
        {
            downloadFile = opSetThumbnail.createFileWithThumbnail(downloadFile,
                                                                  64,
                                                                  64,
                                                                  "image/png",
                                                                  thumbnail);
        }

        // Accept file from file transfer
        fileTransfer.acceptFile(downloadFile);
        // Send analytic
        JabberActivator.getAnalyticsService().onEvent(AnalyticsEventType.RECEIVE_FILE_STARTED);
    }

    /**
     * Cancel the file transfer.
     *
     * @param uid The UID of the transfer to cancel
     */
    public void cancelFileTransfer(String uid)
    {
        logger.debug("Cancelling file transfer with UID: " + uid);
        Object fileTransfer = ActiveFileTransferStore.getFileTransferByID(uid);

        // Cancel is initiated differently, depending on whether this is an incoming or outgoing transfer
        if (fileTransfer instanceof IncomingFileTransferRequest)
        {
            ((IncomingFileTransferRequest) fileTransfer).rejectFile(true);
        }
        else if (fileTransfer instanceof FileTransfer)
        {
            ((FileTransfer) fileTransfer).cancel();
        }
        else
        {
            logger.warn("No matching file transfer found");
        }
    }

    /**
     * Adds the given <tt>FileTransferListener</tt> that would listen for
     * file transfer requests and created file transfers.
     *
     * @param listener the <tt>FileTransferListener</tt> to add
     */
    public void addFileTransferListener(
        FileTransferListener listener)
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
    public void removeFileTransferListener(
        FileTransferListener listener)
    {
        synchronized(fileTransferListeners)
        {
            this.fileTransferListeners.remove(listener);
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected()
        throws  IllegalStateException
    {
        if (jabberProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                +"service before being able to send a file.");
        else if (!jabberProvider.isRegistered())
        {
            // if we are not registered but the current status is online
            // change the current status
            if(opSetPersPresence.getPresenceStatus().isOnline())
            {
                opSetPersPresence.fireProviderStatusChangeEvent(
                    opSetPersPresence.getPresenceStatus(),
                    jabberProvider.getJabberStatusEnum().getStatus(
                        JabberStatusEnum.OFFLINE_STATUS));
            }

            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to send a file.");
        }
    }

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * Supports up to 2GB.
     *
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        return 2147483648L;// = 2048*1024*1024;
    }

    /**
     * Our listener that will tell us when we're registered to
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("The provider changed state from: "
                 + evt.getOldState()
                 + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence =
                    (OperationSetPersistentPresenceJabberImpl) jabberProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

                opSetThumbnail =
                        (OperationSetThumbnailedFileFactoryImpl) jabberProvider
                            .getOperationSet(OperationSetThumbnailedFileFactory.class);

                // Create the Jabber FileTransferManager.
                manager = FileTransferManager.getInstanceFor(jabberProvider.getConnection());

                fileTransferRequestListener = new FileTransferRequestListener();

                ProviderManager.addIQProvider(
                    StreamInitiation.ELEMENT,
                    StreamInitiation.NAMESPACE,
                    new ThumbnailStreamInitiationProvider());

                ProviderManager.addIQProvider(
                    BoBIQ.ELEMENT,
                    BoBIQ.NAMESPACE,
                    new BoBIQProvider());

                manager.addFileTransferListener(fileTransferRequestListener);
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED ||
                     evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                ActiveFileTransferStore.failAllActiveFileTransfers();
                if(fileTransferRequestListener != null
                    && jabberProvider.getConnection() != null)
                {
                    manager.removeFileTransferListener(fileTransferRequestListener);
                }

                ProviderManager.removeIQProvider(
                    StreamInitiation.ELEMENT,
                    StreamInitiation.NAMESPACE);

                ProviderManager.removeIQProvider(
                    BoBIQ.ELEMENT,
                    BoBIQ.NAMESPACE);

                fileTransferRequestListener = null;
                manager = null;
            }
        }
    }

    /**
     * Listener for Jabber incoming file transfer requests.
     */
    private class FileTransferRequestListener
            implements org.jivesoftware.smackx.filetransfer.FileTransferListener
    {
        @Override
        public void fileTransferRequest(FileTransferRequest fileTransferRequest)
        {
            logger.debug("Incoming Jabber file transfer request.");

            // Create a global incoming file transfer request.
            IncomingFileTransferRequestJabberImpl incomingFileTransferRequest
                = new IncomingFileTransferRequestJabberImpl(
                        jabberProvider,
                        OperationSetFileTransferJabberImpl.this,
                        fileTransferRequest);

            // Send a thumbnail request if a thumbnail is advertised in the
            // streamInitiation packet.
            StreamInitiation streamInitiation = fileTransferRequest.getStreamInitiation();
            StreamInitiation.File file = streamInitiation.getFile();

            if (file instanceof ThumbnailFile)
            {
                ThumbnailElement thumbnailElement
                    = ((ThumbnailFile) file).getThumbnailElement();

                if (thumbnailElement != null)
                {
                    incomingFileTransferRequest
                        .createThumbnailListeners(thumbnailElement.getContentId()
                                                                  .getCid());

                    BoBIQ thumbnailRequest = new BoBIQ(thumbnailElement.getContentId());
                    thumbnailRequest.setTo(streamInitiation.getFrom());
                    thumbnailRequest.setFrom(streamInitiation.getTo());
                    thumbnailRequest.setType(IQ.Type.get);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Sending thumbnail request for transfer with ID: "
                            + thumbnailRequest.getContentId());
                    }

                    try
                    {
                        jabberProvider.getConnection().sendStanza(
                            thumbnailRequest);
                    }
                    catch (NotConnectedException | InterruptedException e)
                    {
                        logger.error("Error sending thumbnail request request!", e);
                    }
                }
            }
            // Create an event associated to this global request.
            FileTransferRequestEvent fileTransferRequestEvent
                = new FileTransferRequestEvent(
                    OperationSetFileTransferJabberImpl.this,
                    incomingFileTransferRequest,
                    new Date());

            // Notify the global listener that a request has arrived.
            fireFileTransferRequest(fileTransferRequestEvent);
        }
    }

    /**
     * Delivers the specified event to all registered file transfer listeners.
     *
     * @param event the <tt>EventObject</tt> that we'd like delivered to all
     * registered file transfer listeners.
     */
    void fireFileTransferRequest(FileTransferRequestEvent event)
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
     * Delivers the specified event to all registered file transfer listeners.
     *
     * @param event the <tt>EventObject</tt> that we'd like delivered to all
     * registered file transfer listeners.
     */
    void fireFileTransferRequestThumbnailUpdate(FileTransferRequestEvent event)
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

            listener.fileTransferRequestThumbnailUpdate(event);
        }
    }

    /**
     * Delivers the specified event to all registered file transfer listeners.
     *
     * @param event the <tt>EventObject</tt> that we'd like delivered to all
     * registered file transfer listeners.
     */
    void fireFileTransferRequestRejected(FileTransferRequestEvent event)
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

            listener.fileTransferRequestRejected(event);
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
     * Updates file transfer progress and status while sending or receiving a
     * file.
     */
    protected static class FileTransferProgressThread extends Thread
    {
        private final org.jivesoftware.smackx.filetransfer.FileTransfer
            jabberTransfer;
        private final AbstractFileTransfer fileTransfer;

        private long initialFileSize;

        public FileTransferProgressThread(
            org.jivesoftware.smackx.filetransfer.FileTransfer jabberTransfer,
            AbstractFileTransfer transfer,
            long initialFileSize)
        {
            this.jabberTransfer = jabberTransfer;
            this.fileTransfer = transfer;
            this.initialFileSize = initialFileSize;
        }

        public FileTransferProgressThread(
            org.jivesoftware.smackx.filetransfer.FileTransfer jabberTransfer,
            AbstractFileTransfer transfer)
        {
            this.jabberTransfer = jabberTransfer;
            this.fileTransfer = transfer;
        }

        /**
         * Thread entry point.
         */
        public void run()
        {
            int status;
            long previousProgress = 0;
            long latestProgress;
            String statusReason = "";
            boolean isOutgoing = fileTransfer instanceof OutgoingFileTransferJabberImpl;

            while (true)
            {
                try
                {
                    Thread.sleep(10);

                    status = parseJabberStatus(jabberTransfer.getStatus(), jabberTransfer.getError());
                    latestProgress = fileTransfer.getTransferedBytes();

                    if (status == FileTransferStatusChangeEvent.FAILED
                        || status == FileTransferStatusChangeEvent.COMPLETED
                        || status == FileTransferStatusChangeEvent.CANCELED
                        || status == FileTransferStatusChangeEvent.REFUSED)
                    {
                        if (isOutgoing)
                        {
                            ((OutgoingFileTransferJabberImpl) fileTransfer)
                                .removeThumbnailRequestListener();
                        }

                        ActiveFileTransferStore.removeActiveFileTransfer(fileTransfer.getID());

                        // sometimes a filetransfer can be preparing
                        // and than completed :
                        // transfered in one iteration of current thread
                        // so it won't go through intermediate state - inProgress
                        // make sure this won't happen
                        if(status == FileTransferStatusChangeEvent.COMPLETED
                            && fileTransfer.getStatus()
                                == FileTransferStatusChangeEvent.PREPARING)
                        {
                            fileTransfer.fireStatusChangeEvent(
                                FileTransferStatusChangeEvent.IN_PROGRESS,
                                "Status changed");
                            fileTransfer.fireProgressChangeEvent(
                                System.currentTimeMillis(), latestProgress);
                        }

                        // Send analytics
                        if (status != FileTransferStatusChangeEvent.FAILED)
                        {
                            JabberActivator.getAnalyticsService().onEvent(isOutgoing ? AnalyticsEventType.SEND_FILE_COMPLETED
                                                                                     : AnalyticsEventType.RECEIVE_FILE_COMPLETED,
                                                                          "Result",
                                                                          status == FileTransferStatusChangeEvent.COMPLETED ? "Success"
                                                                                                                            : "Cancelled");
                        }

                        break;
                    }

                    // Only fire events if something has significantly changed!
                    if (status != fileTransfer.getStatus())
                    {
                        fileTransfer.fireStatusChangeEvent(status, "Status changed");
                    }
                    long progressPercentageChange = ((latestProgress - previousProgress)*100)/jabberTransfer.getFileSize();
                    if (progressPercentageChange >= 1)
                    {
                        fileTransfer.fireProgressChangeEvent(
                            System.currentTimeMillis(), latestProgress);
                        previousProgress = latestProgress;
                    }
                }
                catch (InterruptedException e)
                {
                    logger.debug("Unable to sleep thread.", e);
                }
            }

            Error jabberError = jabberTransfer.getError();
            if (jabberError != null)
            {
                String errorMessage = jabberError.getMessage();
                logger.error("An error occurred while transferring file: "
                    +  errorMessage);
                JabberActivator.getAnalyticsService().onEvent(isOutgoing ? AnalyticsEventType.SEND_FILE_FAILED
                                                                         : AnalyticsEventType.RECEIVE_FILE_FAILED,
                                                              "Error message",
                                                              errorMessage);
            }

            Exception jabberException = jabberTransfer.getException();
            if (jabberException != null)
            {
                logger.error("An exception occurred while transferring file: ",
                             jabberException);

                if(jabberException instanceof XMPPErrorException)
                {
                    StanzaError error =
                        ((XMPPErrorException)jabberException)
                            .getStanzaError();
                    if (error != null)
                        if (error.getCondition() == StanzaError.Condition.not_acceptable
                           || error.getCondition() == StanzaError.Condition.forbidden)
                            status = FileTransferStatusChangeEvent.REFUSED;
                }

                statusReason = jabberException.getMessage();
                JabberActivator.getAnalyticsService().onEvent(isOutgoing ? AnalyticsEventType.SEND_FILE_FAILED
                                                                         : AnalyticsEventType.RECEIVE_FILE_FAILED,
                                                              "Error message",
                                                              statusReason);
            }

            if (initialFileSize > 0
                && status == FileTransferStatusChangeEvent.COMPLETED
                && fileTransfer.getTransferedBytes() < initialFileSize)
            {
                status = FileTransferStatusChangeEvent.CANCELED;
            }

            fileTransfer.fireStatusChangeEvent(status, statusReason);
            fileTransfer.fireProgressChangeEvent(
                System.currentTimeMillis(), latestProgress);
        }
    }

    /**
     * Parses the given Jabber status to a <tt>FileTransfer</tt> interface
     * status.
     *
     * @param jabberStatus the Jabber status to parse
     * @param jabberError the Jabber error, if an error occurred.
     * @return the parsed status
     */
    private static int parseJabberStatus(Status jabberStatus, Error jabberError)
    {
        if (jabberStatus.equals(Status.complete))
            return FileTransferStatusChangeEvent.COMPLETED;
        else if (jabberStatus.equals(Status.cancelled))
            return FileTransferStatusChangeEvent.CANCELED;
        else if (jabberStatus.equals(Status.in_progress)
                || jabberStatus.equals(Status.negotiated))
            return FileTransferStatusChangeEvent.IN_PROGRESS;
        else if (jabberStatus.equals(Status.error)) {
            if (Error.no_response.equals(jabberError)) {
                return FileTransferStatusChangeEvent.NO_RESPONSE;
            } else {
                return FileTransferStatusChangeEvent.FAILED;
            }
        }
        else if (jabberStatus.equals(Status.refused))
            return FileTransferStatusChangeEvent.REFUSED;
        else if (jabberStatus.equals(Status.negotiating_transfer))
            return FileTransferStatusChangeEvent.WAITING;
        else if (jabberStatus.equals(Status.negotiating_stream))
            return FileTransferStatusChangeEvent.PREPARING;
        else
             // FileTransfer.Status.initial
            return FileTransferStatusChangeEvent.WAITING;
    }
}
