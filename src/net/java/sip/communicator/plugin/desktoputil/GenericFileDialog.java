/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

import javax.swing.*;

/**
 * This class is the entry point for creating a file dialog regarding to the OS.
 *
 * If the current operating system is Apple Mac OS X, we create an AWT
 * FileDialog (user interface is more practical under Mac OS than a
 * JFileChooser), else, a Swing JFileChooser.
 *
 * @author Valentin Martinet
 */
public class GenericFileDialog
{
    private static final String LABEL_FOREGROUND = "Label.foreground";

    /**
     * Creates a file dialog (AWT's FileDialog or Swing's JFileChooser)
     * regarding to user's operating system.
     *
     * @param parent the parent Frame/JFrame of this dialog
     * @param title dialog's title
     * @param fileOperation
     * @return a SipCommFileChooser instance
     */
    public static SipCommFileChooser create(
            Component parent,
            String title,
            int fileOperation)
    {
        int operation;

        // This currently doesn't work on Mac because of an incompatibility
        // between SWT and Oracle Java 7 (currently u40).  When that is
        // resolved, this code can be reinstated.  In the meantime, we make do
        // with Swing-style file dialogs on Mac.
//        if (OSUtils.IS_MAC)
//        {
//            switch (fileOperation)
//            {
//            case SipCommFileChooser.LOAD_FILE_OPERATION:
//                operation = FileDialog.LOAD;
//                break;
//            case SipCommFileChooser.SAVE_FILE_OPERATION:
//                operation = FileDialog.SAVE;
//                break;
//            default:
//                throw new IllegalArgumentException("fileOperation");
//            }
//
//            if (parent == null)
//                parent = new Frame();
//
//            return new SipCommFileDialogImpl(parent, title, operation);
//        }
//        else
        {
            switch (fileOperation)
            {
            case SipCommFileChooser.LOAD_FILE_OPERATION:
                operation = JFileChooser.OPEN_DIALOG;
                break;
            case SipCommFileChooser.SAVE_FILE_OPERATION:
                operation = JFileChooser.SAVE_DIALOG;
                break;
            default:
                throw new IllegalArgumentException("fileOperation");
            }

            // The background colour of the Windows File Chooser is not
            // brandable and is always white. Therefore the text colour of this
            // window must also not be brandable.
            UIDefaults uiDefaults = UIManager.getDefaults();

            Color originalForeground = (Color) uiDefaults.get(LABEL_FOREGROUND);

            uiDefaults.put(LABEL_FOREGROUND, Color.BLACK);

            SipCommFileChooserImpl scfc = new SipCommFileChooserImpl(parent,
                title,operation);

            uiDefaults.put(LABEL_FOREGROUND, originalForeground);

            return scfc;
        }
    }

    /**
     * Creates a file dialog (AWT FileDialog or Swing JFileChooser) regarding to
     * user's operating system.
     *
     * @param parent the parent Frame/JFrame of this dialog
     * @param title dialog's title
     * @param fileOperation
     * @param path start path of this dialog
     * @return SipCommFileChooser an implementation of SipCommFileChooser
     */
    public static SipCommFileChooser create(
        Component parent, String title, int fileOperation, String path)
    {
        SipCommFileChooser scfc
            = GenericFileDialog.create(parent, title, fileOperation);

        if(path != null)
            scfc.setStartPath(path);
        return scfc;
    }
}
