/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>RenameContactDialog</tt> is the dialog containing the form for
 * renaming a contact.
 *
 * @author Yana Stamcheva
 */
public class RenameContactDialog
    extends SIPCommDialog
    implements ActionListener
{
    private static final long serialVersionUID = 0L;

    private RenameContactPanel renameContactPanel;

    private JButton renameButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.RENAME"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private JButton clearButton = new JButton(
            GuiActivator.getResources().getI18NString(
                "service.gui.RENAME_CLEAR_USER_DEFINED"));

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout());

    private MetaContactListService clist;

    private MetaContact metaContact;

    /**
     * Creates an instance of <tt>RenameContactDialog</tt>.
     *
     * @param mainFrame The main application window.
     * @param metaContact The <tt>MetaContact</tt> to rename.
     */
    public RenameContactDialog(AbstractMainFrame mainFrame,
            MetaContact metaContact)
    {
        super(mainFrame);

        this.setSize(new Dimension(520, 270));

        this.clist = GuiActivator.getContactListService();
        this.metaContact = metaContact;

        this.renameContactPanel = new RenameContactPanel(
                metaContact.getDisplayName());

        this.init();
    }

    /**
     * Initializes the <tt>RenameContactDialog</tt> by adding the buttons,
     * fields, etc.
     */
    private void init()
    {
        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.RENAME_CONTACT"));

        this.getRootPane().setDefaultButton(renameButton);
        this.renameButton.setName("rename");
        this.cancelButton.setName("cancel");
        this.clearButton.setName("clear");

        this.renameButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.RENAME"));
        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.renameButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        this.clearButton.addActionListener(this);

        this.buttonsPanel.add(renameButton);
        this.buttonsPanel.add(cancelButton);

        TransparentPanel allButtonsPanel
                = new TransparentPanel(new BorderLayout());
        TransparentPanel firstButonPanel =
                    new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
        firstButonPanel.add(clearButton);

        // add it only if we have one MetaContact
        if(metaContact.getContactCount() == 1)
            allButtonsPanel.add(firstButonPanel, BorderLayout.WEST);

        allButtonsPanel.add(buttonsPanel, BorderLayout.CENTER);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        this.mainPanel.add(renameContactPanel, BorderLayout.NORTH);
        this.mainPanel.add(allButtonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Handles the <tt>ActionEvent</tt>. In order to rename the contact invokes
     * the <code>renameMetaContact</code> method of the current
     * <tt>MetaContactListService</tt>.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        if (name.equals("rename"))
        {
            if (metaContact != null)
            {
                new Thread("RenameContactDialog.actionPerformed")
                {
                    public void run()
                    {
                        clist.renameMetaContact(
                            metaContact, renameContactPanel.getNewName());
                    }
                }.start();
            }
        }
        else if (name.equals("clear"))
        {
            clist.clearUserDefinedDisplayName(metaContact);
        }
        this.dispose();
    }

    /**
     * Requests the focus in the text field contained in this
     * dialog.
     */
    public void requestFocusInFiled()
    {
        this.renameContactPanel.requestFocusInField();
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }
}
