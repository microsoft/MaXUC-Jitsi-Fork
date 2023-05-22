/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import org.jitsi.util.*;

/**
 * Implements <tt>SipCommFileChooser</tt> for Swing's <tt>JFileChooser</tt>.
 *
 * @author Valentin Martinet
 */
public class SipCommFileChooserImpl
    extends JFileChooser
    implements SipCommFileChooser
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Parent component of this dialog (JFrame, Frame, etc)
     */
    private Component parent;

    /**
     * Creates a new <tt>SipCommFileChooserImpl</tt> instance with a defined
     * parent component. Note this is required as Mac will not bring this window
     * into focus unless its parent is correct.
     *
     * @param parent The parent of this component
     * @param title The window title
     * @param operation The type of selection (files, directories or both; save
     *                  or load)
     */
    public SipCommFileChooserImpl(Component parent, String title, int operation)
    {
        super();
        this.parent = parent;
        ScaleUtils.scaleFontRecursively(this.getComponents());

        if (OSUtils.IS_WINDOWS)
        {
            // The current filter is 'All Files', we are about to replace this
            // with our own filter so make sure we match the description
            final String filterDescription = getFileFilter().getDescription();

            setAcceptAllFileFilterUsed(false);

            setFileFilter(new SipCommFileFilter()
            {
                @Override
                public boolean accept(File file)
                {
                    // Filter out any filenames ending in '.lnk' these are Windows
                    // shortcuts and aren't followed by Java.
                    return !(file.getName().toLowerCase().endsWith("lnk"));
                }

                @Override
                public String getDescription()
                {
                    return filterDescription;
                }
            });
        }

        this.setDialogTitle(title);
        this.setDialogType(operation);
    }

    /**
     * Returns the selected file by the user from the dialog.
     *
     * @return File the selected file from the dialog
     */
    public File getApprovedFile()
    {
        return this.getSelectedFile();
    }

    /**
     * Sets the default path to be considered for browsing among files.
     *
     * @param path the default start path for this dialog
     */
    public void setStartPath(String path)
    {
        // If the path is null, we have nothing more to do here.
        if (path == null)
            return;

        // If the path is an URL extract the path from the URL in order to
        // remove the "file:" part, which doesn't work with methods provided
        // by the file chooser.
        try
        {
            URL url = new URL(path);

            path = url.getPath();
        }
        catch (MalformedURLException e) {}

        File file = new File(path);

        setCurrentDirectory(file);

        /*
         * If the path doesn't exist, the intention of the caller may have been
         * to also set a default file name.
         */
        if ((file != null) && !file.isDirectory())
            setSelectedFile(file);
        else
            setSelectedFile(null);
    }

    /**
     * Shows the dialog and returns the selected file.
     *
     * @return File the selected file in this dialog
     */
    public File getFileFromDialog()
    {
        int choice = -1;

        if(this.getDialogType() == JFileChooser.OPEN_DIALOG)
            choice = this.showOpenDialog(parent);
        else
            choice = this.showSaveDialog(parent);

        return
            (choice == JFileChooser.APPROVE_OPTION) ? getSelectedFile() : null;
    }

    /**
     * Adds a file filter to this dialog.
     *
     * @param filter the filter to add
     */
    public void addFilter(SipCommFileFilter filter)
    {
        this.addChoosableFileFilter(filter);
    }

    /**
     * Removes a file filter to this dialog.
     *
     * @param filter the filter to remove
     */
    public void removeFilter(SipCommFileFilter filter)
    {
        this.removeChoosableFileFilter(filter);
    }

    /**
     * Sets whether the AcceptAll file filter should be used.
     *
     * @param used whether the filter should be used
     */
    public void useAllFileFilter(Boolean used)
    {
        this.setAcceptAllFileFilterUsed(used);
    }

    /**
     * Sets a file filter to this dialog.
     *
     * @param filter the filter to add
     */
    public void setFileFilter(SipCommFileFilter filter)
    {
        super.setFileFilter(filter);
    }

    /**
     * Returns the filter the user has chosen for saving a file.
     *
     * @return SipCommFileFilter the used filter when saving a file
     */
    public SipCommFileFilter getUsedFilter()
    {
        return (SipCommFileFilter)this.getFileFilter();
    }

    /**
     * Change the selection mode for the file choose.
     * Possible values are DIRECTORIES_ONLY or FILES_ONLY, default is
     * FILES_ONLY.
     *
     * @param mode the mode to use.
     */
    public void setSelectionMode(int mode)
    {
        super.setFileSelectionMode(mode);
    }
}
