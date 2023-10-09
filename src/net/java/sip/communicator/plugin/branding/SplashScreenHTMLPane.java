/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.branding;

import java.io.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.util.*;

public class SplashScreenHTMLPane
    extends JEditorPane
{
    private static final long serialVersionUID = 0L;
    private final Logger logger = Logger.getLogger(SplashScreenHTMLPane.class);

    private final HTMLDocument document;

    public SplashScreenHTMLPane()
    {
        this.setContentType("text/html");

        this.document
            = (HTMLDocument) this.getDocument();

        this.setDocument(document);

        ScaleUtils.scaleFontAsDefault(this);
        Constants.loadSplashScreenStyle(document.getStyleSheet(), this.getFont());

    }

    public void appendToEnd(String text)
    {
        Element root = document.getDefaultRootElement();
        try
        {
            document.insertAfterEnd(root
                .getElement(root.getElementCount() - 1), text);
        }
        catch (BadLocationException | IOException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
    }
}
