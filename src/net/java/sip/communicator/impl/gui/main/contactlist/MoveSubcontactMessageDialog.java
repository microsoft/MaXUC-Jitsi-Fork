/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>MoveSubcontactMessageDialog</tt> is the dialog shown when user
 * tries to move a subcontact in the contact list. It is meant to inform the
 * user that she should select another meta contact, where the previously
 * choosen contact will be moved.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class MoveSubcontactMessageDialog
    extends SIPCommDialog
    implements Skinnable
{
    private static final long serialVersionUID = 0L;

    private SIPCommMsgTextArea infoArea = new SIPCommMsgTextArea(
        GuiActivator.getResources()
            .getI18NString("service.gui.MOVE_SUBCONTACT_MSG"));

    private JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources()
            .getI18NString("service.gui.MOVE_SUBCONTACT"));

    private JLabel iconLabel = new JLabel();

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private TransparentPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private int dialogWidth = 350;
    private int dialogHeight = 150;

    private AbstractMainFrame mainFrame;
    private ContactListListener clistListener;

    /**
     * Creates an instance of MoveSubcontactMessageDialog and constructs
     * all panels contained in this dialog.
     * @param parentWindow the main application window
     * @param listener the listener that deals with moved contacts
     */
    public MoveSubcontactMessageDialog(AbstractMainFrame parentWindow,
            ContactListListener listener)
    {
        super(parentWindow);

        this.mainFrame = parentWindow;
        this.clistListener = listener;

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.MOVE_SUBCONTACT"));

        this.mainPanel.setPreferredSize(
                new Dimension(dialogWidth, dialogHeight));

        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                dispose();

                mainFrame.getContactListPanel().getContactList()
                    .removeContactListListener(clistListener);

                // FIXME: unset the special cursor after a subcontact has been
                // moved (other related FIXMEs in ContactRightButtonMenu.java)
                //clist.setCursor(
                //        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);

        Font font = infoTitleLabel.getFont();
        infoTitleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoArea);

        this.mainPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.buttonsPanel.add(cancelButton);

        this.mainPanel.add(labelsPanel, BorderLayout.CENTER);
        this.mainPanel.add(iconLabel, BorderLayout.WEST);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        loadSkin();

        this.pack();
    }

    /**
     * Computes the location of this dialog in order to show it on the left
     * or the right side of the main application window.
     * @param parentWindow the main application window
     */
    private void setDialogLocation(JFrame parentWindow)
    {
        int dialogY = (int) Toolkit.getDefaultToolkit()
            .getScreenSize().getHeight()/2 - dialogHeight/2;

        int parentX = parentWindow.getLocation().x;

        if ((parentX - dialogWidth) > 0) {
            this.setLocation(parentX - dialogWidth,
                dialogY);
        }
        else {
            this.setLocation(parentX + parentWindow.getWidth(),
                    dialogY);
        }
    }

    /**
     * Automatically clicks the cancel button when the dialog is closed.
     *
     * @param isEscaped indicates if the dialog has been closed by pressing the
     * Esc key
     */
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * In addition to setVisible this method would also set the dialog location
     * to fit the main frame.
     *
     * @param isVisible indicates if the component should be visible or not
     */
    public void setVisible(boolean isVisible)
    {
        super.setVisible(isVisible);

        this.setDialogLocation(mainFrame);
    }

    /**
     * Reloads the icon.
     */
    public void loadSkin()
    {
        GuiActivator.getImageLoaderService()
        .getImage(ImageLoaderService.INFO_ICON)
        .getImageIcon()
        .addToLabel(iconLabel);
    }
}
