/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>RenameGroupPanel</tt> is where the user could change the name of a
 * meta contact group.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class RenameGroupPanel
    extends TransparentPanel
    implements Skinnable
{
    private static final long serialVersionUID = 0L;

    private JLabel uinLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.NEW_NAME"));

    private JTextField textField = new JTextField();

    private TransparentPanel dataPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
        GuiActivator.getResources()
            .getI18NString("service.gui.RENAME_GROUP_INFO"));

    private JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.RENAME_GROUP"));

    private JLabel iconLabel = new JLabel();

    private TransparentPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private TransparentPanel rightPanel
        = new TransparentPanel(new BorderLayout());

    /**
     * Creates an instance of <tt>RenameGroupPanel</tt> and initializes it.
     */
    public RenameGroupPanel(String groupName)
    {
        super(new BorderLayout());

        this.textField.setText(groupName);
        this.textField.select(0, groupName.length());

        this.setPreferredSize(new Dimension(500, 200));

        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));

        this.infoLabel.setEditable(false);

        this.dataPanel.add(uinLabel, BorderLayout.WEST);

        this.dataPanel.add(textField, BorderLayout.CENTER);

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);

        Font font = infoTitleLabel.getFont();
        infoTitleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(dataPanel);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);

        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

        loadSkin();
    }

    /**
     * Returns the new name entered by the user.
     *
     * @return the new name entered by the user.
     */
    public String getNewName()
    {
        return textField.getText();
    }

    /**
     * Requests the focus in the text field.
     */
    public void requestFocusInField()
    {
        this.textField.requestFocus();
    }

    /**
     * Reloads the icon.
     */
    public void loadSkin()
    {
        GuiActivator.getImageLoaderService().getImage(
            ImageLoaderService.RENAME_DIALOG_ICON)
            .getImageIcon()
            .addToLabel(iconLabel);
    }
}
