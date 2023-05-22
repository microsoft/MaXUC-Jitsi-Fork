/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.io.*;

/**
 * The purpose of this interface is to provide an unique way to access to the
 * methods of JFileChooser (javax.swing) or FileDialog (java.awt).
 *
 * It's interesting to use FileDialog under Mac OS X because it uses the native
 * user interface for file selection provided by Mac OS which is more practical
 * than the user interface performed by a JFileChooser (on Mac).
 *
 * Therefore, under other platforms (Microsoft Windows, Linux), the use of
 * JFileChooser instead of FileDialog performs a better user interface for
 * browsing among a file hierarchy.
 *
 * @author Valentin Martinet
 */
public interface SipCommFileChooser
{
    /**
     * Allows to request a 'load file' dialog (optional)
     */
    int LOAD_FILE_OPERATION = 0;

    /**
     * Allows to request a 'save file' dialog (optional)
     */
    int SAVE_FILE_OPERATION = 1;

    /**
     * Instruction to display only files.
     */
    int FILES_ONLY = 0;

    /**
     * Instruction to display only directories in
     * file chooser dialog.
     */
    int DIRECTORIES_ONLY = 1;

    /**
     * Change the selection mode for the file choose.
     * Possible values are DIRECTORIES_ONLY or FILES_ONLY, default is
     * FILES_ONLY.
     *
     * @param mode the mode to use.
     */
    void setSelectionMode(int mode);

    /**
     * Returns the selected file by the user from the dialog.
     *
     * @return File the selected file from the dialog
     */
    File getApprovedFile();

    /**
     * Sets the default path to be considered for browsing among files.
     *
     * @param path the default start path for this dialog
     */
    void setStartPath(String path);

    /**
     * Shows the dialog and returns the selected file.
     *
     * @return File the selected file in this dialog
     */
    File getFileFromDialog();

    /**
     * Adds a file filter to this dialog.
     *
     * @param filter the filter to add
     */
    void addFilter(SipCommFileFilter filter);

    /**
     * Removes a file filter to this dialog.
     *
     * @param filter the filter to remove
     */
    void removeFilter(SipCommFileFilter filter);

    /**
     * Sets whether the AcceptAll file filter should be used.
     *
     * @param used whether the filter should be used
     */
    void useAllFileFilter(Boolean used);

    /**
     * Sets a file filter to this dialog.
     *
     * @param filter the filter to add
     */
    void setFileFilter(SipCommFileFilter filter);

    /**
     * Returns the filter the user has chosen for saving a file.
     *
     * @return SipCommFileFilter the used filter when saving a file
     */
    SipCommFileFilter getUsedFilter();
}
