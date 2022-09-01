// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;

import org.jitsi.service.resources.*;

/**
 * The <tt>NoFavoritesPanel</tt> replaces the contact list, when a
 * <tt>FavoritesFilter</tt> finds no matches. It should give instructions on how
 * the user can add favorites
 */
public class NoFavoritesPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructor - creates a NoFavoritesPanel including setting out the UI
     * elements.
     */
    public NoFavoritesPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        ResourceManagementService res = GuiActivator.getResources();
        setBackground(new Color(
                           res.getColor("service.gui.LIGHT_BACKGROUND")));

        // Get and show the image
        ImageIconFuture hint = res.getImage("service.gui.NO_FAVORITES_HINT");
        JLabel hintPicture = hint.addToLabel(new JLabel());
        hintPicture.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(hintPicture);

        // Add the helpful text
        // First get the resources we need:
        String title = res.getI18NString("service.gui.NO_FAVORITES_TITLE");
        String text = res.getI18NString("service.gui.NO_FAVORITES_TEXT");
        Color color =
          new Color(res.getColor("service.gui.IN_CALL_TOOL_BAR_BORDER_SHADOW"));

        // Use just a single pane to display both title and text and this keeps
        // the two close to one another when the window resizes.  Thus use HTML
        // to display the text nicely.
        JEditorPane textPane = new JEditorPane();
        ScaleUtils.scaleFontAsDefault(textPane);
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setContentType("text/html");
        textPane.setForeground(color);

        textPane.setText("<html><b>" + title + "</b><br/><br/>" + text + "</html>");

        // Centre the text
        StyledDocument doc = (StyledDocument) textPane.getDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        add(textPane);
    }
}
