/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.util.*;

/**
 * A custom styled HTML editor pane.
 *
 * @author Yana Stamcheva
 */
public class StyledHTMLEditorPane
    extends JEditorPane
{
    /**
     * The serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger for this class.
     */
    private final Logger logger = Logger.getLogger(StyledHTMLEditorPane.class);

    /**
     * The editor kit of this editor pane.
     */
    private final HTMLEditorKit editorKit;

    /**
     * The document of this editor pane.
     */
    private final HTMLDocument document;

    /**
     * Creates an instance of <tt>StyledHTMLEditorPane</tt>.
     */
    public StyledHTMLEditorPane()
    {
        editorKit = new SIPCommHTMLEditorKit(this);

        this.document = (HTMLDocument) editorKit.createDefaultDocument();

        this.setContentType("text/html");
        this.setEditorKitForContentType("text/html", editorKit);
        this.setEditorKit(editorKit);
        this.setDocument(document);

        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    }
}
