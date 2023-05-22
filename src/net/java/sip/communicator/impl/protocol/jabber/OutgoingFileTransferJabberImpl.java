/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.File;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.bob.BoBData;
import org.jivesoftware.smackx.bob.ContentId;
import org.jivesoftware.smackx.bob.element.BoBIQ;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail.ThumbnailFile;
import net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail.ThumbnailElement;
import net.java.sip.communicator.service.protocol.AbstractFileTransfer;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.util.Logger;

/**
 * The Jabber protocol extension of the <tt>AbstractFileTransfer</tt>.
 *
 * @author Yana Stamcheva
 */
public class OutgoingFileTransferJabberImpl
    extends AbstractFileTransfer
    implements StanzaListener
{
    /**
     * The logger of this class.
     */
    private final Logger logger
        = Logger.getLogger(OutgoingFileTransferJabberImpl.class);

    private final String id;

    private final Contact receiver;

    private final File file;

    private ThumbnailElement thumbnailElement;

    private final ThumbnailRequestListener thumbnailRequestListener
        = new ThumbnailRequestListener(BoBIQ.ELEMENT, BoBIQ.NAMESPACE,
            IQ.Type.get, IQRequestHandler.Mode.async);

    /**
     * The jabber outgoing file transfer.
     */
    private final OutgoingFileTransfer jabberTransfer;

    private final ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * Creates an <tt>OutgoingFileTransferJabberImpl</tt> by specifying the
     * <tt>receiver</tt> contact, the <tt>file</tt>, the <tt>jabberTransfer</tt>,
     * that would be used to send the file through Jabber and the
     * <tt>protocolProvider</tt>.
     *
     * @param receiver the destination contact
     * @param file the file to send
     * @param jabberTransfer the Jabber transfer object, containing all transfer
     * information
     * @param transferId the unique identifier of this file transfer that is
     * used by the history and the user interface to track this transfer.
     * @param protocolProvider the parent protocol provider
     * @throws XmppStringprepException
     */
    public OutgoingFileTransferJabberImpl(
        Contact receiver,
        File file,
        OutgoingFileTransfer jabberTransfer,
        String transferId,
        ProtocolProviderServiceJabberImpl protocolProvider)
    throws XmppStringprepException
    {
        this.receiver = receiver;
        this.file = file;
        this.jabberTransfer = jabberTransfer;
        this.id = transferId;
        this.protocolProvider = protocolProvider;

        // Add this outgoing transfer as a packet interceptor in
        // order to manage thumbnails.
        if (file instanceof ThumbnailedFile
             && ((ThumbnailedFile) file).getThumbnailData() != null
             && ((ThumbnailedFile) file).getThumbnailData().length > 0)
        {
            if (protocolProvider.isFeatureListSupported(
                            protocolProvider.getFullJid(receiver),
                            new String[]{"urn:xmpp:thumbs:0",
                                "urn:xmpp:bob"}))
            {
                // If this deprecated method is removed we'll need to add an
                // IQInterceptor class to Smack.
                protocolProvider.getConnection().addStanzaInterceptor(
                    this,
                    IQTypeFilter.SET);
            }
        }

        ActiveFileTransferStore.addActiveFileTransfer(this.id, this);
    }

    /**
     * Cancels the file transfer.
     */
    public void cancel()
    {
        this.jabberTransfer.cancel();
    }

    /**
     * Returns the number of bytes already sent to the recipient.
     *
     * @return the number of bytes already sent to the recipient.
     */
    public long getTransferedBytes()
    {
        return jabberTransfer.getBytesSent();
    }

    /**
     * The direction is outgoing.
     * @return OUT.
     */
    public int getDirection()
    {
        return OUT;
    }

    /**
     * Returns the local file that is being transferred or to which we transfer.
     *
     * @return the file
     */
    public File getLocalFile()
    {
        return file;
    }

    /** Returns the number of bytes of the local file that we are sending. */
    public long getFileSize()
    {
        return file.length();
    }

    /**
     * The contact we are sending the file.
     * @return the receiver.
     */
    public Contact getContact()
    {
        return receiver;
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
     * Removes previously added thumbnail request listener.
     */
    public void removeThumbnailRequestListener()
    {
        XMPPConnection connection = protocolProvider.getConnection();
        if (connection != null)
        {
            connection.unregisterIQRequestHandler(thumbnailRequestListener);
        }
    }

    /**
     * Listens for all <tt>StreamInitiation</tt> packets and adds a thumbnail
     * to them if a thumbnailed file is supported.
     */
    public void processStanza(Stanza stanza)
    {
        if (!(stanza instanceof StreamInitiation))
            return;

        // If our file is not a thumbnailed file we have nothing to do here.
        if (!(file instanceof ThumbnailedFile))
            return;

        logger.debug("File transfer packet intercepted"
                     + " in order to add thumbnail.");

        StreamInitiation fileTransferPacket = (StreamInitiation) stanza;

        ThumbnailedFile thumbnailedFile = (ThumbnailedFile) file;

        if (jabberTransfer.getStreamID()
                .equals(fileTransferPacket.getSessionID()))
        {
            StreamInitiation.File baseFile = fileTransferPacket.getFile();

            thumbnailElement = new ThumbnailElement(
                thumbnailedFile.getThumbnailData(),
                thumbnailedFile.getThumbnailMimeType(),
                thumbnailedFile.getThumbnailWidth(),
                thumbnailedFile.getThumbnailHeight());

            ThumbnailFile fileElement = new ThumbnailFile(baseFile, thumbnailElement);

            fileTransferPacket.setFile(fileElement);

            logger.debug("Thumbnail added to file transfer packet");

            // Add the request listener in order to listen for requests coming
            // for the advertised thumbnail.
            if (protocolProvider.getConnection() != null)
            {
                protocolProvider.getConnection().registerIQRequestHandler(
                    thumbnailRequestListener);
            }
        }
        // Remove this stanza interceptor after we're done.
        protocolProvider.getConnection().removeStanzaInterceptor(this);
    }

    /**
     * The <tt>ThumbnailRequestListener</tt> listens for events triggered by
     * the reception of a <tt>BoBIQ</tt> packet. The packet is examined
     * and a <tt>BoBIQ</tt> is created to respond to the thumbnail
     * request received.
     */
    private class ThumbnailRequestListener extends AbstractIqRequestHandler
    {
        protected ThumbnailRequestListener(String element, String namespace,
            Type type, Mode mode)
        {
            super(element, namespace, type, mode);
        }

        @Override
        public IQ handleIQRequest(IQ iqRequest)
        {
            BoBIQ bobIQ = (BoBIQ) iqRequest;
            ContentId BoBIQCid = bobIQ.getContentId();

            if ((BoBIQCid != null)
                    && BoBIQCid.equals(thumbnailElement.getContentId()))
            {
                ThumbnailedFile thumbnailedFile = (ThumbnailedFile) file;
                BoBIQ thumbnailResponse = new BoBIQ(
                    BoBIQCid,
                    new BoBData(
                        thumbnailedFile.getThumbnailMimeType(),
                        thumbnailedFile.getThumbnailData()));
                thumbnailResponse.setTo(bobIQ.getTo());
                thumbnailResponse.setFrom(bobIQ.getFrom());
                thumbnailResponse.setType(IQ.Type.result);

                logger.debug("Send thumbnail response to the receiver for transfer with ID: "
                        + BoBIQCid);

                return thumbnailResponse;
            }
            else
            {
                return null;
            }
        }
    }
}
