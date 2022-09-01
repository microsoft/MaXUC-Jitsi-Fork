/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.filetransfer;

import java.util.*;

import net.java.sip.communicator.service.database.schema.*;
import net.java.sip.communicator.service.filehistory.*;

/**
 * The component used to show a file transfer history record in the chat or
 * history window.
 *
 * @author Yana Stamcheva
 */
public class FileHistoryConversationComponent
    extends FileTransferConversationComponent
{
    private static final long serialVersionUID = 0L;

    private final FileRecord fileRecord;

    public FileHistoryConversationComponent(FileRecord fileRecord)
    {
        super(fileRecord.getID());
        this.fileRecord = fileRecord;

        String contactName = fileRecord.getContact().getDisplayName();

        openFileButton.setVisible(true);
        openFolderButton.setVisible(true);

        String titleString = "";
        if (fileRecord.isInbound())
        {
            if (fileRecord.getStatus().equals(FileHistoryTable.STATUS.COMPLETED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_RECEIVE_COMPLETED",
                    new String[]{contactName});

                setWarningStyle(false);
            }
            else if (fileRecord.getStatus().equals(FileHistoryTable.STATUS.CANCELED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_TRANSFER_CANCELED");

                setWarningStyle(true);
            }
            else if (fileRecord.getStatus().equals(FileHistoryTable.STATUS.FAILED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_RECEIVE_FAILED",
                    new String[]{contactName});

                setWarningStyle(true);
            }
            else if (fileRecord.getStatus().equals(FileHistoryTable.STATUS.REFUSED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_TRANSFER_REFUSED",
                    new String[]{contactName});

                openFileButton.setVisible(false);
                openFolderButton.setVisible(false);

                setWarningStyle(true);
            }
        }
        else if (!fileRecord.isInbound())
        {
            if (fileRecord.getStatus().equals(FileHistoryTable.STATUS.COMPLETED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_SEND_COMPLETED",
                    new String[]{contactName});

                setWarningStyle(false);
            }
            else if (fileRecord.getStatus().equals(FileHistoryTable.STATUS.CANCELED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_TRANSFER_CANCELED");

                setWarningStyle(true);
            }
            else if (fileRecord.getStatus().equals(FileHistoryTable.STATUS.FAILED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_UNABLE_TO_SEND",
                    new String[]{contactName});

                setWarningStyle(true);
            }
            else if (fileRecord.getStatus().equals(FileHistoryTable.STATUS.REFUSED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_SEND_REFUSED",
                    new String[]{contactName});
                setWarningStyle(true);
            }
        }

        this.setCompletedDownloadFile(fileRecord.getFile());

        Date date = fileRecord.getDate();

        titleLabel.setText(
            getDateString(date) + titleString);
        fileLabel.setText(getFileLabel(fileRecord.getFile()));
    }

    /**
     * Returns the date of the component event.
     *
     * @return the date of the component event
     */
    public Date getDate()
    {
        return fileRecord.getDate();
    }

    /**
     * We don't have progress label in history.
     *
     * @return empty string
     */
    protected String getProgressLabel(String bytesString)
    {
        return "";
    }
}
