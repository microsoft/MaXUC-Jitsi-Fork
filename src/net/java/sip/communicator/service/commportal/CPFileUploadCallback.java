// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

import java.io.File;

/**
 * Defines a callback for uploading files to CP
 */
public interface CPFileUploadCallback
{
    /**
     * @return the file to be uploaded
     */
    File getFile();

    /**
     * @return the server location to upload the file to (e.g. "/upload/file.zip")
     */
    String getUploadLocation();

    /**
     * Called when we have successfully uploaded the file
     */
    void onUploadSuccess();

    /**
     * Called if we fail to upload the file.
     *
     * @param error the reason why we failed to upload the file
     */
    void onDataFailure(CPDataError error);
}
