// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import static org.jitsi.util.SanitiseUtils.sanitise;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.regex.Pattern;

import net.java.sip.communicator.util.*;

/**
 * A button that, when pressed, copies text to the clipboard.  If no text to
 * copy has been set, the button will be disabled.
 */
public class SIPCommCopyButton extends SIPCommButton
                               implements ActionListener
{
    private static final long serialVersionUID = 1L;

    private static final Logger sLog = Logger.getLogger(SIPCommCopyButton.class);

    private static final Pattern MEETING_ID_PATTERN = Pattern.compile("[0-9]+");

    /**
     * The text to be copied to the clipboard when the button is pressed.
     */
    private String mCopyText;

    /**
     * Creates a new SIPCommCopyButton with images accessed via a resource
     * with the given prefix but with no text to copy to the clipboard.
     *
     * @param imageResPrefix the prefix of the resource for the images for this
     * button.
     */
    public SIPCommCopyButton(String imageResPrefix)
    {
        super(imageResPrefix);

        sLog.debug("Creating new copy button");
        addActionListener(this);
        setEnabled(false);
    }

    /**
     * Sets the text to copy when the button is pressed and disables the button
     * if the text is null.
     *
     * @param copyText the text to copy when the button is pressed
     */
    public void setCopyText(String copyText)
    {
        sLog.debug("Setting copy text to: '" + sanitise(copyText, MEETING_ID_PATTERN) + "'");
        mCopyText = copyText;
        setEnabled(mCopyText != null);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        sLog.info("Copy button pressed to copy: '" + sanitise(mCopyText, MEETING_ID_PATTERN) + "'");
        StringSelection selection = new StringSelection(mCopyText);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
