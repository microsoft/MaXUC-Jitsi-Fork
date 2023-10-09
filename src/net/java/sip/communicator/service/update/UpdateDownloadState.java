// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.update;

/**
 * Enum to represent all the possible states that the update progress bar could be in.
 * Used to conditionally render different contents of the update progress bar dialog on the Electron side
 */
public enum UpdateDownloadState
{
    UPDATE_DOWNLOAD_IN_PROGRESS,
    UPDATE_DOWNLOAD_FAILED,
    UPDATE_DOWNLOAD_CANCELLED,
    UPDATE_VERIFICATION_FAILED
}
