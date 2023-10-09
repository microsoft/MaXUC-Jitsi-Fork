/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import javax.swing.*;

/**
 * The <tt>SIPCommMsgTextArea</tt> is a text area defined specially for warning
 * messages. It defines an area with a fixed width and wraps the text within it.
 *
 * @author Yana Stamcheva
 */
public class SIPCommMsgTextArea
    extends JTextArea
{
    private static final long serialVersionUID = 0L;

    /** Unused constructor used to block access to the default inherited constructor. */
    @SuppressWarnings("unused")
    public SIPCommMsgTextArea() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a text area with a fixed width and wraps the text within it.
     * @param text The text to insert in this text area.
     */
    public SIPCommMsgTextArea(String text){
        super(text);

        init();
    }

    private void init()
    {
        this.setEditable(false);
        this.setOpaque(false);

        // Only set a fixed width if the text needs more than one line,
        // otherwise leave it flexible.
        // Note that the number of columns here doesn't always match the number
        // of characters we can display in each line - the setColumns() method
        // actually sets the width to cols*columnWidth, where columnWidth is
        // the width of the largest character ('m') in the current font.
        // That means we can't use the string length to calculate the number of
        // rows we'll have.
        int cols = 40;
        int textWidth = getFontMetrics(getFont()).stringWidth(getText());
        int maxWidth = cols * getColumnWidth();
        int rows = (int)Math.ceil((float)textWidth / maxWidth);
        if (rows > 1)
        {
            this.setLineWrap(true);
            this.setWrapStyleWord(true);
            this.setColumns(cols);
            this.setRows(rows);
        }
        else
        {
            this.setLineWrap(false);
        }
    }
}
