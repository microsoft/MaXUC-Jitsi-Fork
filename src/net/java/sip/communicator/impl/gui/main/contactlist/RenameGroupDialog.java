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
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>RenameGroupDialog</tt> is the dialog containing the form for
 * renaming a group.
 *
 * @author Yana Stamcheva
 */
public class RenameGroupDialog
    extends SIPCommDialog
    implements ActionListener
{
    private static final long serialVersionUID = 0L;

    private RenameGroupPanel renameGroupPanel;

    private JButton renameButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.RENAME"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout());

    private MetaContactListService clist;

    private MetaContactGroup metaGroup;

    /**
     * Creates an instance of <tt>RenameGroupDialog</tt>.
     *
     * @param mainFrame The main application window.
     * @param metaGroup The <tt>MetaContactGroup</tt> to rename.
     */
    public RenameGroupDialog(AbstractMainFrame mainFrame,
            MetaContactGroup metaGroup) {
        super(mainFrame);

        this.setSize(new Dimension(520, 270));

        this.clist = GuiActivator.getContactListService();
        this.metaGroup = metaGroup;

        this.renameGroupPanel = new RenameGroupPanel(metaGroup.getGroupName());

        this.init();
    }

    /**
     * Initializes the <tt>RenameGroupDialog</tt> by adding the buttons,
     * fields, etc.
     */
    private void init()
    {
        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.RENAME_GROUP"));

        this.getRootPane().setDefaultButton(renameButton);
        this.renameButton.setName("rename");
        this.cancelButton.setName("cancel");

        this.renameButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.RENAME"));
        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.renameButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.buttonsPanel.add(renameButton);
        this.buttonsPanel.add(cancelButton);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        this.mainPanel.add(renameGroupPanel, BorderLayout.NORTH);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Handles the <tt>ActionEvent</tt>. In order to rename the group invokes
     * the <code>renameMetaContactGroup</code> method of the current
     * <tt>MetaContactListService</tt>.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        if (name.equals("rename"))
        {
            if (metaGroup != null)
            {
                new Thread("RenameGroupDialog.actionPerformed") {
                    public void run() {
                        clist.renameMetaContactGroup(
                                metaGroup, renameGroupPanel.getNewName());
                    }
                }.start();
            }
            this.dispose();
        }
        else
            this.dispose();
    }

    /**
     * Requests the focus in the text field contained in this
     * dialog.
     */
    public void requestFocusInFiled()
    {
        this.renameGroupPanel.requestFocusInField();
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }
}
