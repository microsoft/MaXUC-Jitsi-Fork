// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.update;

/**
 * This class contains information about the progress of our update download. The properties are serialized via protobuf
 * to be passed on to the Electron application. The properties determine how our update progress bar dialog will be rendered.
 */
public class UpdateProgressInformation
{
    private long size;
    private long transferredSize;
    private UpdateDownloadState updateDownloadState;
    private UpdateState updateState;

    public UpdateProgressInformation(long size, long transferredSize,
                                     UpdateDownloadState updateDownloadState,
                                     UpdateState updateState)
    {
        this.size = size;
        this.transferredSize = transferredSize;
        this.updateDownloadState = updateDownloadState;
        this.updateState = updateState;
    }

    public long getSize()
    {
        return size;
    }

    public long getTransferredSize()
    {
        return transferredSize;
    }

    public UpdateDownloadState getUpdateDownloadState()
    {
        return updateDownloadState;
    }

    public UpdateState getUpdateState()
    {
        return updateState;
    }

    public String toString()
    {
        return "Size: " + getSize() + ", Transferred Size: " + getTransferredSize() +
               ", Update Download State: " + getUpdateDownloadState() +
               ", Update State: " + getUpdateState();
    }
}
