/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The <tt>AccountsConfigurationPanel</tt> is the panel containing the accounts
 * list and according buttons shown in the options form.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class AccountsConfigurationPanel
    extends ConfigurationPanel
    implements ActionListener,
               ListSelectionListener,
               PropertyChangeListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>AccountsConfigurationPanel</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger =
        Logger.getLogger(AccountsConfigurationPanel.class);

    private final AccountList accountList;

    private final JButton newButton =
        new SIPCommBasicTextButton(GuiActivator.getResources().getI18NString(
            "service.gui.ADD"));

    private final JButton removeButton =
        new SIPCommBasicTextButton(GuiActivator.getResources().getI18NString(
            "service.gui.DELETE"));

    /**
     * Indicates whether IM is provisioned automatically or by the user.  If
     * by the user, we will display the 'add' and 'delete' buttons on the
     * accounts panel.
     */
    private static final String IM_PROVISION_SOURCE_PROP =
                             "net.java.sip.communicator.im.IM_PROVISION_SOURCE";

    /**
     * The config service.
     */
    private static final ConfigurationService configService =
        GuiActivator.getConfigurationService();

    private JLabel maxAccountsLabel = new JLabel();

    /**
     * Creates and initializes this account configuration panel.
     */
    public AccountsConfigurationPanel()
    {
        super(new BorderLayout());

        accountList = new AccountList(this);

        /*
         * It seems that we can only delete one account at a time because our
         * confirmation dialog asks for one account.
         */
        accountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.setPreferredSize(new Dimension(500, 400));

        JScrollPane accountListPane = new JScrollPane();

        accountListPane.getViewport().add(accountList);
        accountListPane.getVerticalScrollBar().setUnitIncrement(30);

        this.add(accountListPane, BorderLayout.CENTER);

        JPanel buttonsPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        newButton.addActionListener(this);
        removeButton.addActionListener(this);

        this.newButton.setMnemonic(GuiActivator.getResources().getI18nMnemonic(
                "service.gui.ADD"));
        this.removeButton
            .setMnemonic(GuiActivator.getResources().getI18nMnemonic(
                "service.gui.DELETE"));

        AccessibilityUtils.setNameAndDescription(newButton,
            newButton.getText(),
            GuiActivator.getResources().getI18NString("service.gui.ADD_DESCRIPTION"));

        AccessibilityUtils.setNameAndDescription(removeButton,
            removeButton.getText(),
            GuiActivator.getResources().getI18NString("service.gui.DELETE_DESCRIPTION"));

        buttonsPanel.add(newButton);
        buttonsPanel.add(removeButton);

        JPanel southPanel = new TransparentPanel(new BorderLayout());
        southPanel.add(maxAccountsLabel, BorderLayout.WEST);
        southPanel.add(buttonsPanel, BorderLayout.EAST);

        this.add(southPanel, BorderLayout.SOUTH);

        accountList.addListSelectionListener(this);
        accountList.addPropertyChangeListener(
            AccountList.ACCOUNT_STATE_CHANGED, this);
        updateButtons();

        setMinimumSize(new Dimension(600, 400));

        // Listen for changes to whether the user can configure their own chat
        // account and change the visibility of the 'add' and 'delete' buttons
        // accordingly.
        configService.user().addPropertyChangeListener(IM_PROVISION_SOURCE_PROP,
                                                new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent e)
            {
                logger.debug(e.getPropertyName() + " has changed to " +
                    e.getNewValue() + ". Updating 'add account' menu item.");
                updateButtons();
            }
        });
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks on on the
     * buttons. Shows the account registration wizard when user clicks on "New".
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object sourceButton = evt.getSource();

        if (sourceButton.equals(newButton))
        {
            logger.user("Add new account clicked");
            NewAccountDialog.showNewAccountDialog();
        }
        else if (sourceButton.equals(removeButton))
        {
            logger.user("Delete account clicked");
            Account account = accountList.getSelectedAccount();

            if (account == null)
                return;

            AccountID accountID = account.getAccountID();

            ProtocolProviderFactory providerFactory =
                AccountUtils.getProtocolProviderFactory(
                    accountID.getProtocolName());

            if (providerFactory != null)
            {
                MessageDialog dialog = new MessageDialog(
                    null,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_ACCOUNT"),
                    GuiActivator.getResources()
                        .getI18NString("service.gui.REMOVE_ACCOUNT_MESSAGE"),
                    GuiActivator.getResources().getI18NString(
                            "service.gui.REMOVE"),
                    false);

                int result = dialog.showDialog();

                if (result == MessageDialog.OK_RETURN_CODE)
                {
                    logger.user("Delete account confirmed");
                    accountID.deleteAccount(true);
                    accountList.ensureAccountRemoved(accountID);

                    // Notify the corresponding wizard that the account
                    // would be removed.
                    AccountRegWizardContainerImpl wizardContainer
                        = (AccountRegWizardContainerImpl) GuiActivator
                            .getUIService().getAccountRegWizardContainer();

                    ProtocolProviderService protocolProvider =
                                              account.getProtocolProvider();
                    AccountRegistrationWizard wizard =
                                           protocolProvider == null ? null :
                        wizardContainer.getProtocolWizard(protocolProvider);

                    if (wizard != null)
                        wizard.accountRemoved(protocolProvider);
                }
            }
        }
    }

    /**
     * Updates enabled states of the buttons of this
     * <tt>AccountsConfigurationPanel</tt> to reflect their applicability to the
     * current selection in <tt>accountList</tt>.
     */
    public void updateButtons()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateButtons();
                }
            });
            return;
        }

        Account account = accountList.getSelectedAccount();
        boolean enabled = (account != null);

        // Users are only allowed to have at most 1 chat account.  Also, they
        // are only allowed to see any UI relating to adding an account if they
        // have a manually configured chat account.
        boolean isManual = "Manual".equals(ConfigurationUtils.getImProvSource());
        boolean existingImAccount = AccountUtils.getImAccount() != null;

        boolean newAccountPermitted = !existingImAccount && isManual;
        logger.debug("New account permitted? ", newAccountPermitted,
                     " is Manual ? ",  isManual,  "existing account? ", existingImAccount);

        String message = GuiActivator.getResources().getI18NString(
                "service.gui.MAX_CHAT_ACCOUNTS_CONFIGURED");
        maxAccountsLabel.setText("<html><p>" + message + "</p></html>");

        // Make the 'Add' and 'Delete' buttons and the 'maximum accounts
        // label' visible or invisible based on whether the chat account is
        // configured to be set up by the user.
        newButton.setEnabled(newAccountPermitted);
        newButton.setVisible(isManual);
        maxAccountsLabel.setVisible(!newAccountPermitted && isManual);
        removeButton.setEnabled(enabled);
        removeButton.setVisible(isManual);
    }

    /**
     * Implements ListSelectionListener#valueChanged(ListSelectionEvent).
     * @param e the <tt>ListSelectionEvent</tt> that notified us
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
            updateButtons();
    }

    /**
     * This method gets called when a property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        // update buttons whenever an account changes its state
        updateButtons();
    }
}
