/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.managecontact.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>AddContactDialog</tt> is the dialog containing the form for adding
 * a contact.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class AddContactDialog
    extends SIPCommDialog
    implements  ManageContactWindow,
                ExportedWindow,
                ActionListener,
                WindowFocusListener,
                Skinnable
{
    /**
     * The <tt>Logger</tt> used by the <tt>AddContactDialog</tt> class and its
     * instances for logging output.
     */
    private static final Logger sLog = Logger.getLogger(AddContactDialog.class);

    private static final long serialVersionUID = 0L;

    private final  JLabel accountLabel = new JLabel(
        GuiActivator.getResources().getI18NString(
            "service.gui.SELECT_ACCOUNT") + ": ");

    private final JComboBox<Object> accountCombo = new JComboBox<>();

    private final JLabel groupLabel = new JLabel(
        GuiActivator.getResources().getI18NString(
            "service.gui.SELECT_GROUP") + ": ");

    private JComboBox<Object> groupCombo;

    private final JLabel contactAddressLabel = new JLabel(
        GuiActivator.getResources().getI18NString(
            "service.gui.CONTACT_NAME") + ": ");

    private final JLabel displayNameLabel = new JLabel(
        GuiActivator.getResources().getI18NString(
            "service.gui.DISPLAY_NAME") + ": ");

    private final JTextField contactAddressField = new JTextField();

    private final JTextField displayNameField = new JTextField();

    private final JButton addButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ADD"));

    private final JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private MetaContact metaContact;

    /**
     * Image label.
     */
    private JLabel imageLabel = new JLabel();

    /**
     * Creates an instance of <tt>AddContactDialog</tt> that represents a dialog
     * that adds a new contact to an already existing meta contact.
     *
     * @param parentWindow the parent window of this dialog
     */
    public AddContactDialog(Frame parentWindow)
    {
        super(parentWindow);

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT"));

        groupCombo = createGroupCombo(this);

        this.init();
    }

    /**
     * Creates an <tt>AddContactDialog</tt> by specifying the parent window and
     * a meta contact, to which to add the new contact.
     * @param parentWindow the parent window
     * @param metaContact the meta contact, to which to add the new contact
     */
    public AddContactDialog(Frame parentWindow, MetaContact metaContact)
    {
        this(parentWindow);

        this.metaContact = metaContact;

        groupCombo.setEnabled(false);

        this.setSelectedGroup(metaContact.getParentMetaContactGroup());

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT_TO")
            + " " + metaContact.getDisplayName());
    }

    /**
     * Selects the given protocol provider in the account combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to select
     */
    public void setSelectedAccount(ProtocolProviderService protocolProvider)
    {
        accountCombo.setSelectedItem(protocolProvider);
    }

    /**
     * Selects the given <tt>group</tt> in the group combo box.
     * @param group the <tt>MetaContactGroup</tt> to select
     */
    public void setSelectedGroup(MetaContactGroup group)
    {
        groupCombo.setSelectedItem(group);
    }

    /**
     * Sets the address of the contact to add.
     * @param contactAddress the address of the contact to add
     */
    public void setContactAddress(String contactAddress)
    {
        contactAddressField.setText(contactAddress);
    }

    /**
     * Sets the display name of the contact to add.
     * @param displayName the display name of the contact to add
     */
    public void setDisplayName(String displayName)
    {
        displayNameField.setText(displayName);
    }

    /**
     * Initializes the dialog.
     */
    private void init()
    {
        TransparentPanel labelsPanel
            = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        TransparentPanel fieldsPanel
            = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        initAccountCombo();
        accountCombo.setRenderer(new AccountComboRenderer());

        // we have an empty choice and one account
        if(accountCombo.getItemCount() > 2
            || (accountCombo.getItemCount() == 2))
        {
            labelsPanel.add(accountLabel);
            fieldsPanel.add(accountCombo);
        }

        labelsPanel.add(groupLabel);
        fieldsPanel.add(groupCombo);

        labelsPanel.add(contactAddressLabel);
        fieldsPanel.add(contactAddressField);

        labelsPanel.add(displayNameLabel);
        fieldsPanel.add(displayNameField);

        contactAddressField.getDocument().addDocumentListener(
            new DocumentListener()
            {
                public void changedUpdate(DocumentEvent e) {}

                public void insertUpdate(DocumentEvent e)
                {
                    updateAddButtonState();
                }

                public void removeUpdate(DocumentEvent e)
                {
                    updateAddButtonState();
                }
            });

        TransparentPanel dataPanel = new TransparentPanel(new BorderLayout());

        dataPanel.add(labelsPanel, BorderLayout.WEST);
        dataPanel.add(fieldsPanel);

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout(20, 10));

        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(imageLabel, BorderLayout.WEST);
        mainPanel.add(dataPanel, BorderLayout.CENTER);
        mainPanel.add(createButtonsPanel(), BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(450, 250));
        this.setResizable(false);
        this.addWindowFocusListener(this);

        // All items are now instantiated and could safely load the skin.
        loadSkin();
    }

    /**
     * Creates the buttons panel.
     * @return the created buttons panel
     */
    private Container createButtonsPanel()
    {
        TransparentPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.ADD"));
        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        buttonsPanel.add(addButton);
        buttonsPanel.add(cancelButton);

        // Disable the add button so that it would be clear for the user that
        // they need to choose an account and enter a contact id first.
        addButton.setEnabled(false);

        return buttonsPanel;
    }

    /**
     * Initializes account combo box.
     */
    private void initAccountCombo()
    {
        Iterator<ProtocolProviderService> providers
            = AccountUtils.getRegisteredProviders().iterator();

        accountCombo.addItem(GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_ACCOUNT"));

        accountCombo.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                updateAddButtonState();
            }
        });

        while (providers.hasNext())
        {
            ProtocolProviderService provider = providers.next();

            boolean isHidden = provider.getAccountID().getAccountProperty(
                    ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;

            if(isHidden)
                continue;

            OperationSet opSet
                = provider.getOperationSet(OperationSetPresence.class);

            if (opSet == null)
                continue;

            accountCombo.addItem(provider);

            if (isPreferredProvider(provider.getAccountID()))
                accountCombo.setSelectedItem(provider);
        }

        // if we have only select account option and only one account
        // select the available account
        if(accountCombo.getItemCount() == 2)
            accountCombo.setSelectedIndex(1);
    }

    /**
     * Initializes groups combo box.
     */
    public static JComboBox<Object> createGroupCombo(final Dialog parentDialog)
    {
        final JComboBox<Object> groupCombo = new JComboBox<>();

        groupCombo.setRenderer(new GroupComboRenderer());

        groupCombo.addItem(GuiActivator.getContactListService().getRoot());

        Iterator<MetaContactGroup> groupList
            = GuiActivator.getContactListService().getRoot().getSubgroups();

        while (groupList.hasNext())
        {
            MetaContactGroup group = groupList.next();

            if (!group.isPersistent())
                continue;

            groupCombo.addItem(group);
        }

        return groupCombo;
    }

    /**
     * Indicates that the "Add" buttons has been pressed.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();

        if (button.equals(addButton))
        {
            sLog.user("Add button clicked");
            final ProtocolProviderService protocolProvider
                = (ProtocolProviderService) accountCombo.getSelectedItem();
            final String contactAddress = contactAddressField.getText();
            final String displayName = displayNameField.getText();

            if (!protocolProvider.isRegistered())
            {
                new ErrorDialog(
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ADD_CONTACT_ERROR_TITLE"),
                    GuiActivator.getResources().getI18NString(
                        "service.gui.ADD_CONTACT_NOT_CONNECTED", new String[]
                        { contactAddress })).showDialog();

                return;
            }

            if (displayName != null && displayName.length() > 0)
            {
                addRenameListener(protocolProvider,
                                  metaContact,
                                  contactAddress,
                                  displayName);
            }

            if (metaContact != null)
            {
                new Thread("AddContactDialog.actionPerformed - add new contact")
                {
                    @Override
                    public void run()
                    {
                        GuiActivator.getContactListService()
                            .addNewContactToMetaContact(
                                protocolProvider,
                                metaContact,
                                contactAddress);
                    }
                }.start();
            }
            else
            {
                ContactListUtils.addContact(protocolProvider,
                                            (MetaContactGroup) groupCombo
                                                .getSelectedItem(),
                                            contactAddress);
            }
        }
        dispose();
    }

    /**
     * Indicates that this dialog is about to be closed.
     * @param isEscaped indicates if the dialog is closed by pressing the
     * Esc key
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Indicates that the window has gained the focus. Requests the focus in
     * the text field.
     * @param e the <tt>WindowEvent</tt> that notified us
     */
    public void windowGainedFocus(WindowEvent e)
    {
        this.contactAddressField.requestFocus();
    }

    public void windowLostFocus(WindowEvent e) {}

    /**
     * A custom renderer displaying accounts in a combo box.
     */
    private static class AccountComboRenderer extends DefaultListCellRenderer
    {
        private static final long serialVersionUID = 0L;

        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            if (value instanceof String)
            {
                setIcon(null);
                setText((String) value);
            }
            else if (value instanceof ProtocolProviderService)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) value;

                if (provider != null)
                {
                    BufferedImageFuture protocolImg
                        = provider.getProtocolIcon()
                            .getIcon(ProtocolIcon.ICON_SIZE_16x16);

                    if (protocolImg != null)
                    {
                        GuiActivator.getImageLoaderService()
                        .getIndexedProtocolIcon(protocolImg, provider)
                        .addToLabel(this);
                    }

                    this.setText(provider.getAccountID().getDisplayName());
                }
            }

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    /**
     * A custom renderer displaying groups in a combo box.
     */
    private static class GroupComboRenderer
        extends DefaultListCellRenderer
    {
        private static final long serialVersionUID = 0L;

        @Override
        public Component getListCellRendererComponent(JList<? extends Object> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            if (value instanceof String)
            {
                this.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 0, 0, 0)));
                this.setText((String) value);
            }
            else
            {
                this.setBorder(null);
                MetaContactGroup group = (MetaContactGroup) value;

                if (group.equals(GuiActivator
                    .getContactListService().getRoot()))
                    this.setText(GuiActivator.getResources()
                        .getI18NString("service.gui.SELECT_NO_GROUP"));
                else
                    this.setText(group.getGroupName());
            }

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }

    /**
     * Brings this window to the front.
     */
    public void bringToFront()
    {
        toFront();
    }

    @Override
    public void bringToBack()
    {
        toBack();
    }

    /**
     * Returns this exported window identifier.
     * @return the identifier of this window
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.ADD_CONTACT_WINDOW;
    }

    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * This window can't be maximized.
     */
    public void maximize() {}

    /**
     * This window can't be minimized.
     */
    public void minimize() {}

    /**
     * This method can be called to pass any params to the exported window. This
     * method will be automatically called by
     * {@link UIService#getExportedWindow(WindowID, Object[])} in order to set
     * the parameters passed.
     *
     * @param windowParams the parameters to pass.
     */
    public void setParams(Object[] windowParams) {}

    /**
     * Updates the state of the add button.
     */
    private void updateAddButtonState()
    {
        String contactAddress = contactAddressField.getText();

        if (accountCombo.getSelectedItem()
            instanceof ProtocolProviderService
            && contactAddress != null && contactAddress.length() > 0)
            addButton.setEnabled(true);
        else
            addButton.setEnabled(false);
    }

    /**
     * Reloads resources for this component.
     */
    public void loadSkin()
    {
        GuiActivator.getResources().getImage(
            "service.gui.icons.ADD_CONTACT_DIALOG_ICON")
            .addToLabel(imageLabel);

        imageLabel.setVerticalAlignment(JLabel.TOP);
    }

    /**
     * Returns the first <tt>ProtocolProviderService</tt> implementation
     * corresponding to the preferred protocol
     *
     * @return the <tt>ProtocolProviderService</tt> corresponding to the
     * preferred protocol
     */
    private boolean isPreferredProvider(AccountID accountID)
    {
        String preferredProtocolProp
            = accountID.getAccountPropertyString(
                ProtocolProviderFactory.IS_PREFERRED_PROTOCOL);

        if (preferredProtocolProp != null
            && preferredProtocolProp.length() > 0
            && Boolean.parseBoolean(preferredProtocolProp))
        {
            return true;
        }

        return false;
    }

    /**
     * Adds a rename listener.
     *
     * @param protocolProvider the protocol provider to which the contact was
     * added
     * @param metaContact the <tt>MetaContact</tt> if the new contact was added
     * to an existing meta contact
     * @param contactAddress the address of the newly added contact
     * @param displayName the new display name
     */
    private void addRenameListener(
                                final ProtocolProviderService protocolProvider,
                                final MetaContact metaContact,
                                final String contactAddress,
                                final String displayName)
    {
        GuiActivator.getContactListService().addMetaContactListListener(
            new MetaContactListAdapter()
            {
                public void metaContactAdded(MetaContactEvent evt)
                {
                    if (evt.getSourceMetaContact().getContact(
                            contactAddress, protocolProvider) != null)
                    {
                        renameContact(evt.getSourceMetaContact(), displayName);
                    }
                }

                public void protoContactAdded(ProtoContactEvent evt)
                {
                    if (metaContact != null
                        && evt.getNewParent().equals(metaContact))
                    {
                        renameContact(metaContact, displayName);
                    }
                }
            });
    }

    /**
     * Renames the given meta contact.
     *
     * @param metaContact the <tt>MetaContact</tt> to rename
     * @param displayName the new display name
     */
    private void renameContact(final MetaContact metaContact,
                               final String displayName)
    {
        new Thread("AddContactDialog.renameContact")
        {
            @Override
            public void run()
            {
                GuiActivator.getContactListService()
                    .renameMetaContact(metaContact, displayName);
            }
        }.start();
    }

    public void setContactDetails(ContactDetail... contactDetails)
    {
        String address = (contactDetails == null)?
            null : contactDetails[0].getDetail();
        setContactAddress(address);
    }

    public void setContactName(String name)
    {
        displayNameField.setText(name);
    }

    public void setContactExtraDetails(Map<String, String> extraDetails)
    {
        // Do nothing - details other than the address and name are not
        // supported by this dialog.
    }

    @Override
    public void repaintWindow()
    {
        repaint();
    }

    @Override
    public void saveAndDismiss()
    {
        dispose();
    }

    //This method 'steals' the focus to the add contact window if the user interacted with different processes
    @Override
    public void focusOnWindow()
    {
        super.setAlwaysOnTop(true);
        super.toFront();
        super.requestFocus();
        super.setAlwaysOnTop(false);
    }
}
