/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.awt.*;
import java.io.*;

import javax.swing.*;
import javax.swing.filechooser.*;

/**
 * Utility class that exposes various file-handling methods.
 *
 * @author Yana Stamcheva
 */
public class FileUtils
{
    /**
     * Logger.
     */
    private static final Logger logger = Logger.getLogger(FileUtils.class);

    /**
     * Returns <code>true</code> if the file given by <tt>fileName</tt> is an
     * image, <tt>false</tt> - otherwise.
     *
     * @param fileName the name of the file to check
     * @return <code>true</code> if the file is an image, <tt>false</tt> -
     * otherwise.
     */
    public static boolean isImage(String fileName)
    {
        fileName = fileName.toLowerCase();

        String[] imageTypes = {"jpeg", "jpg", "png", "gif"};

        for (String imageType : imageTypes)
        {
            if (fileName.endsWith(imageType))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the native icon of the given file if one exists, otherwise
     * returns null.
     *
     * @param file the file to obtain icon for
     * @return the native icon of the given file if one exists, otherwise
     * returns null.
     * TODO: Use JNA to implement this under Linux.
     */
    public static Icon getIcon(File file)
    {
        Icon fileIcon = null;

        try
        {
            fileIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
        }
        catch (Exception e)
        {
            logger.debug("Failed to obtain file icon from ShellFolder.", e);

            /* try with another method to obtain file icon */
            try
            {
                fileIcon = new JFileChooser().getIcon(file);
            }
            catch (Exception e1)
            {
                logger.debug("Failed to obtain file icon from JFileChooser.", e1);
            }
        }

        return fileIcon;
    }

    /**
     * Opens the specified file or folder using the OS's native handler.
     *
     * @param path The file or folder to be opened.
     * @throws IOException If the file or folder could not be accessed, or if
     * java.awt.Desktop is not available on the system.
     */
    public static void openFileOrFolder(File path) throws IOException
    {
        if (Desktop.isDesktopSupported())
        {
            Desktop.getDesktop().open(path);
        }
        else
        {
            throw new IOException("java.awt.Desktop not supported on this " +
                                  "system.");
        }
    }

    /**
     * Creates a new File instance from a parent pathname and a child pathname.
     * @param parent parent pathname
     * @param child child pathname
     * @throws NullPointerException If child is null
     * @return newly created File
     */
    public static File createFile(String parent, String child) throws NullPointerException
    {
        return new File(parent, child);
    }

    /**
     * Creates a new File instance from a parent File and a child pathname.
     * @param parent parent File
     * @param child child pathname
     * @throws NullPointerException If child is null
     * @return newly created File
     */
    public static File createFile(File parent, String child) throws NullPointerException
    {
        return new File(parent, child);
    }

    /**
     * Creates a new <code>File</code> instance by converting the given
     * pathname string into an abstract pathname.  If the given string is
     * the empty string, then the result is the empty abstract pathname.
     *
     * @param   pathname  A pathname string
     * @throws  NullPointerException
     *          If the <code>pathname</code> argument is <code>null</code>
     */
    public static File createFile(String pathname) throws NullPointerException
    {
        return new File(pathname);
    }

    /**
     * Wrapper method for deleting the file or directory denoted by this
     * abstract pathname. Made for testing purposes.
     *
     * @param file a file to be deleted
     * @return <tt>true</tt> if and only if the file or directory is
     * successfully deleted; false otherwise
     */
    public static boolean deleteFile(File file)
    {
        return file.delete();
    }
}
