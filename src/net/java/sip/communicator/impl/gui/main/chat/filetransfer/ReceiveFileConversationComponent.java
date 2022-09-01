/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.filetransfer;

import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
// Disambiguates SwingWorker on Java 6 in the presence of javax.swing.*

/**
 * The <tt>ReceiveFileConversationComponent</tt> is the component shown in the
 * conversation area of the chat window to display a incoming file transfer.
 *
 * @author Yana Stamcheva
 */
public class ReceiveFileConversationComponent
    extends FileTransferConversationComponent
    implements  ActionListener,
                FileTransferStatusListener,
                FileTransferListener
{
    private static final long serialVersionUID = 0L;

    private final Logger logger
        = Logger.getLogger(ReceiveFileConversationComponent.class);

    private final IncomingFileTransferRequest fileTransferRequest;

    private final OperationSetFileTransfer fileTransferOpSet;

    private final ChatPanel chatPanel;

    private final Date date;

    private final String dateString;

    private File downloadFile;

    /**
     * Creates a <tt>ReceiveFileConversationComponent</tt>.
     * @param chatPanel the chat panel
     * @param opSet the <tt>OperationSetFileTransfer</tt>
     * @param request the <tt>IncomingFileTransferRequest</tt>
     * associated with this component
     * @param date the date
     */
    public ReceiveFileConversationComponent(
        ChatPanel chatPanel,
        final OperationSetFileTransfer opSet,
        final IncomingFileTransferRequest request,
        final Date date)
    {
        super(request.getID());
        this.chatPanel = chatPanel;
        this.fileTransferOpSet = opSet;
        this.fileTransferRequest = request;
        this.date = date;
        this.dateString = getDateString(date);

        fileTransferOpSet.addFileTransferListener(this);

        byte[] thumbnail = request.getThumbnail();

        if (thumbnail != null && thumbnail.length > 0)
        {
            ImageIcon thumbnailIcon = new ImageIcon(thumbnail);

            if (thumbnailIcon.getIconWidth() > IMAGE_WIDTH
                || thumbnailIcon.getIconHeight() > IMAGE_HEIGHT)
            {
                thumbnailIcon
                    = ImageUtils.getScaledRoundedIcon(
                        thumbnail, IMAGE_WIDTH, IMAGE_WIDTH);
            }

            imageLabel.setIcon(thumbnailIcon);
        }

        // Ensure we use a friendly contact name if we have one.
        String displayName = AccountUtils.getDisplayNameFromChatAddress(
                             fileTransferRequest.getSender().getDisplayName());

        titleLabel.setText(
            dateString
            + resources.getI18NString(
            "service.gui.FILE_TRANSFER_REQUEST_RECIEVED",
            new String[]{displayName}));

        String fileName
            = getFileLabel(request.getFileName(), request.getFileSize());
        fileLabel.setText(fileName);

        acceptButton.setVisible(true);
        acceptButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                titleLabel.setText(
                    dateString
                    + resources
                    .getI18NString("service.gui.FILE_TRANSFER_PREPARING",
                                   new String[]{displayName}));
                acceptButton.setVisible(false);
                rejectButton.setVisible(false);
                cancelButton.setVisible(true);
                progressBar.setVisible(true);

                downloadFile = createFile(fileTransferRequest);

                new AcceptFile(downloadFile).start();
            }
        });

        rejectButton.setVisible(true);
        rejectButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                fileTransferRequest.rejectFile(false);

                acceptButton.setVisible(false);
                rejectButton.setVisible(false);
                setWarningStyle(true);
                fileLabel.setText("");
                titleLabel.setText(
                    dateString
                    + resources.getI18NString(
                        "service.gui.FILE_TRANSFER_REFUSED"));
                ReceiveFileConversationComponent.this.chatPanel
                    .removeActiveFileTransfer(fileTransferRequest.getID());
            }
        });

        progressBar.setMaximum((int)fileTransferRequest.getFileSize());
    }

    /**
     * RE-IMPLEMENTED IN OPERATIONSETFILETRANSFER
     * Creates the file to download.
     *
     * @return the file to download.
     */
    private File createFile(IncomingFileTransferRequest fileTransferRequest)
    {
        File downloadFile = null;
        File downloadDir = null;

        String incomingFileName = fileTransferRequest.getFileName();
        downloadDir = GuiActivator.getFileAccessService()
            .getDefaultDownloadDirectory();

        if (!downloadDir.exists())
        {
            if (!downloadDir.mkdirs())
            {
                logger.error("Could not create the download directory : "
                    + downloadDir.getAbsolutePath());
            }
            logger.debug("Download directory created : "
                    + downloadDir.getAbsolutePath());
        }

        downloadFile = new File(downloadDir, incomingFileName);

        // If a file with the given name already exists, add an index to the
        // file name.
        int index = 0;
        while (downloadFile.exists())
        {
            int lastDot = incomingFileName.lastIndexOf(".");
            String newFileName;

            if (lastDot != -1)
            {
                // Convert "abc.txt" to "abc-1.txt"
                newFileName = incomingFileName.substring(0, lastDot) +
                              "-" + ++index +
                              incomingFileName.substring(lastDot);
            }
            else
            {
                // Convert "abc" to "abc-1";
                newFileName = incomingFileName + "-" + ++index;
            }

            downloadFile = new File(downloadDir, newFileName);
        }

        // Change the file name to the name we would use on the local file
        // system.
        if (!downloadFile.getName().equals(fileTransferRequest.getFileName()))
        {
            String fileName
                = getFileLabel(downloadFile.getName(),
                               fileTransferRequest.getFileSize());

            fileLabel.setText(fileName);
        }

        return downloadFile;
    }

    /**
     * Handles status changes in file transfer.
     */
    public void statusChanged(FileTransferStatusChangeEvent event)
    {
        FileTransfer fileTransfer = event.getFileTransfer();
        int status = event.getNewStatus();

        // Ensure we use a friendly contact name if we have one.
        String displayName = AccountUtils.getDisplayNameFromChatAddress(
                             fileTransferRequest.getSender().getDisplayName());

        if (status == FileTransferStatusChangeEvent.COMPLETED
            || status == FileTransferStatusChangeEvent.CANCELED
            || status == FileTransferStatusChangeEvent.FAILED
            || status == FileTransferStatusChangeEvent.REFUSED)
        {
            fileTransfer.removeStatusListener(this);
        }

        if (status == FileTransferStatusChangeEvent.PREPARING)
        {
            hideProgressRelatedComponents();

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_PREPARING",
                new String[]{displayName}));
        }
        else if (status == FileTransferStatusChangeEvent.FAILED)
        {
            hideProgressRelatedComponents();

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_RECEIVE_FAILED",
                new String[]{displayName}));

            setWarningStyle(true);
        }
        else if (status == FileTransferStatusChangeEvent.IN_PROGRESS)
        {
            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_RECEIVING_FROM",
                new String[]{displayName}));
            setWarningStyle(false);

            if (!progressBar.isVisible())
            {
                progressBar.setVisible(true);
            }
        }
        else if (status == FileTransferStatusChangeEvent.COMPLETED)
        {
            this.setCompletedDownloadFile(downloadFile);

            hideProgressRelatedComponents();
            cancelButton.setVisible(false);

            openFileButton.setVisible(true);
            openFolderButton.setVisible(true);

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_RECEIVE_COMPLETED",
                new String[]{displayName}));
        }
        else if (status == FileTransferStatusChangeEvent.CANCELED)
        {
            hideProgressRelatedComponents();

            cancelButton.setVisible(false);

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_CANCELED"));
            setWarningStyle(true);
        }
        else if (status == FileTransferStatusChangeEvent.REFUSED)
        {
            hideProgressRelatedComponents();

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_REFUSED",
                new String[]{displayName}));
            cancelButton.setVisible(false);
            openFileButton.setVisible(false);
            openFolderButton.setVisible(false);

            setWarningStyle(true);
        }
        else if (status == FileTransferStatusChangeEvent.NO_RESPONSE)
        {
            hideProgressRelatedComponents();

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_NO_RESPONSE",
                new String[]{displayName}));
            cancelButton.setVisible(false);
            openFileButton.setVisible(false);
            openFolderButton.setVisible(false);

            setWarningStyle(true);
        }
    }

    /**
     * @return the IncomingFileTransferRequest
     */
    public IncomingFileTransferRequest getIncomingFileTransferRequest()
    {
        return fileTransferRequest;
    }

    /**
     * Returns the date of the component event.
     *
     * @return the date of the component event
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * Accepts the file in a new thread.
     */
    private class AcceptFile extends SwingWorker
    {
        private FileTransfer fileTransfer;

        private final File downloadFile;

        public AcceptFile(File downloadFile)
        {
            this.downloadFile = downloadFile;
        }

        public Object construct()
        {
            fileTransfer = fileTransferRequest.acceptFile(downloadFile);

            chatPanel.addActiveFileTransfer(fileTransfer.getID(), fileTransfer);

            // Remove previously added listener, that notified us for request
            // cancellations.
            fileTransferOpSet.removeFileTransferListener(
                ReceiveFileConversationComponent.this);

            // Add the status listener that would notify us when the file
            // transfer has been completed and should be removed from
            // active components.
            fileTransfer.addStatusListener(chatPanel);

            fileTransfer.addStatusListener(
                ReceiveFileConversationComponent.this);

            return "";
        }

        public void finished()
        {
            if (fileTransfer != null)
            {
                setFileTransfer(fileTransfer, fileTransferRequest.getFileSize());
            }
        }
    }

    /**
     * Returns the label to show on the progress bar.
     *
     * @param bytesString the bytes that have been transfered
     * @return the label to show on the progress bar
     */
    protected String getProgressLabel(String bytesString)
    {
        return bytesString
            + " " + resources.getI18NString("service.gui.RECEIVED");
    }

    public void fileTransferCreated(FileTransferCreatedEvent event)
    {}

    public void fileTransferRequestCanceled(FileTransferRequestEvent event)
    {
        IncomingFileTransferRequest request = event.getRequest();

        if (request.equals(fileTransferRequest))
        {
            acceptButton.setVisible(false);
            rejectButton.setVisible(false);

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_CANCELED"));

            setWarningStyle(true);
        }
    }

    public void fileTransferRequestReceived(FileTransferRequestEvent event)
    {}

    /**
     * Updating the file transfer component with the thumbnail.
     *
     * @param event the <tt>EventObject</tt> that we use to update
     * the thumbnail.
     */
    public void fileTransferRequestThumbnailUpdate(FileTransferRequestEvent event)
    {
        IncomingFileTransferRequest request = event.getRequest();
        if (request.equals(fileTransferRequest))
        {
            byte[] thumbnail = event.getRequest().getThumbnail();

            if (thumbnail != null && thumbnail.length > 0)
            {
                ImageIcon thumbnailIcon = new ImageIcon(thumbnail);

                if (thumbnailIcon.getIconWidth() > IMAGE_WIDTH
                    || thumbnailIcon.getIconHeight() > IMAGE_HEIGHT)
                {
                    thumbnailIcon
                        = ImageUtils.getScaledRoundedIcon(
                            thumbnail, IMAGE_WIDTH, IMAGE_WIDTH);
                }

                imageLabel.setIcon(thumbnailIcon);
            }
        }
    }

    public void fileTransferRequestRejected(FileTransferRequestEvent event)
    {}
}
