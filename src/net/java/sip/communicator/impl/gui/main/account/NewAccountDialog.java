/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.AccountUtils;

/**
 * The <tt>NewAccountDialog</tt> is the dialog containing the form used to
 * create a new account.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class NewAccountDialog
    extends SIPCommDialog
    implements CreateAccountWindow,
               ActionListener,
               PropertyChangeListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>NewAccountDialog</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(NewAccountDialog.class);

    private final TransparentPanel accountPanel
        = new TransparentPanel(new BorderLayout());

    private final JComboBox<AccountRegistrationWizard> networkComboBox = new JComboBox<>();

    private final JButton addAccountButton = new SIPCommBasicTextButton(
        GuiActivator.getResources().getI18NString("service.gui.ADD"));

    private final JButton cancelButton = new SIPCommBasicTextButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private final EmptyAccountRegistrationWizard emptyWizard
            = new EmptyAccountRegistrationWizard();

    private static NewAccountDialog newAccountDialog;

    private JTextArea errorMessagePane;
    private TransparentPanel mainPanel;

    /**
     * The config service.
     */
    private static final ConfigurationService configService =
        GuiActivator.getConfigurationService();

    /**
     * If account is signing-in ignore close.
     */
    private boolean isCurrentlySigningIn = false;

    /**
     * Status label, show when connecting.
     */
    private final JLabel statusLabel = new JLabel();

    /**
     * The container of the wizard.
     */
    private final AccountRegWizardContainerImpl wizardContainer;

    /**
     * Creates the dialog and initializes the UI.
     */
    public NewAccountDialog()
    {
        super(GuiActivator.getUIService().getMainFrame(), false);

        ResourceManagementService resources = GuiActivator.getResources();

        mainPanel
            = new TransparentPanel(new BorderLayout(5, 5));
        TransparentPanel rightButtonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        TransparentPanel buttonPanel
            = new TransparentPanel(new BorderLayout());

        String title = resources.getI18NString("service.gui.NEW_ACCOUNT");
        if ((title != null) && title.endsWith("..."))
            title = title.substring(0, title.length() - 3);
        this.setTitle(title);
        this.setResizable(false);

        this.getContentPane().add(mainPanel);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel statusPanel = new TransparentPanel(
                new FlowLayout(FlowLayout.CENTER));
        statusPanel.add(statusLabel);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        buttonPanel.add(rightButtonPanel, BorderLayout.EAST);
        buttonPanel.add(statusPanel, BorderLayout.CENTER);

        AccessibilityUtils.setName(addAccountButton, addAccountButton.getText());
        AccessibilityUtils.setName(cancelButton, cancelButton.getText());

        addAccountButton.setMnemonic(GuiActivator.getResources().getI18nMnemonic("service.gui.ADD"));
        cancelButton.setMnemonic(GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        rightButtonPanel.add(addAccountButton);
        rightButtonPanel.add(cancelButton);

        this.addAccountButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        if (org.jitsi.util.OSUtils.IS_MAC)
            rightButtonPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        this.networkComboBox.setRenderer(new NetworkListCellRenderer());
        this.networkComboBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                AccountRegistrationWizard wizard
                    = (AccountRegistrationWizard)
                        networkComboBox.getSelectedItem();

                loadSelectedWizard(wizard);
            }
        });

        mainPanel.add(accountPanel, BorderLayout.CENTER);

        this.initNetworkList();

        wizardContainer
            = (AccountRegWizardContainerImpl)
                GuiActivator.getUIService().getAccountRegWizardContainer();
        /*
         * XXX The wizardContainer will outlive this instance so it is essential
         * to call removePropertyChangeListener on its model in order to prevent
         * a leak of this instance.
         */
        wizardContainer.getModel().addPropertyChangeListener(this);
    }

    /**
     * Detects all currently registered protocol wizards so that we could fill
     * the protocol/network combo with their graphical representation.
     */
    private void initNetworkList()
    {
        // check for preferred wizard
        String prefWName
            = GuiActivator.getResources().getSettingsString(
                    "impl.gui.PREFERRED_ACCOUNT_WIZARD");
        String preferredWizardName
            = (prefWName != null && prefWName.length() > 0) ? prefWName : null;

        ServiceReference<?>[] accountWizardRefs = null;
        try
        {
            accountWizardRefs = GuiActivator.bundleContext
                .getServiceReferences(
                    AccountRegistrationWizard.class.getName(),
                    null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any, add them in this container.
        if (accountWizardRefs != null)
        {
            logger.debug("Found "
                     + accountWizardRefs.length
                     + " already installed providers.");

            // Create a list to sort the wizards
            ArrayList<AccountRegistrationWizard> networksList =
                    new ArrayList<>();
            networksList.ensureCapacity(accountWizardRefs.length);

            AccountRegistrationWizard prefWiz = null;

            for (int i = 0; i < accountWizardRefs.length; i++)
            {
                AccountRegistrationWizard wizard
                    = (AccountRegistrationWizard) GuiActivator.bundleContext
                        .getService(accountWizardRefs[i]);

                networksList.add(wizard);

                // is it the preferred protocol ?
                if(preferredWizardName != null
                    && wizard.getClass().getName().equals(preferredWizardName))
                {
                    prefWiz = wizard;
                }
            }

            // Sort the list
            Collections.sort(networksList,
                            new Comparator<AccountRegistrationWizard>()
            {
                public int compare(AccountRegistrationWizard arg0,
                                   AccountRegistrationWizard arg1)
                {
                    return arg0.getProtocolName().compareToIgnoreCase(
                                    arg1.getProtocolName());
                }
            });

            // Add the items in the combobox
            for (int i=0; i<networksList.size(); i++)
            {
                networkComboBox.addItem(networksList.get(i));
            }

            //if we have a prefered wizard auto select it
            if (prefWiz != null)
            {
                networkComboBox.setSelectedItem(prefWiz);
            }
            else//if we don't we send our empty page and let the wizard choose.
            {
                networkComboBox.insertItemAt(emptyWizard, 0);
                networkComboBox.setSelectedItem(emptyWizard);

                //disable the add button so that it would be
                //clear for the user that they need to choose a network first
                addAccountButton.setEnabled(false);
            }
        }
    }

    /**
     * This method gets called when a property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(evt.getPropertyName().equals(
                WizardModel.CANCEL_BUTTON_ENABLED_PROPERTY))
        {
            if(evt.getNewValue() instanceof Boolean)
                cancelButton.setEnabled(
                        (Boolean)evt.getNewValue());
        }
        else if(evt.getPropertyName().equals(
                WizardModel.NEXT_FINISH_BUTTON_ENABLED_PROPERTY))
        {
            if(evt.getNewValue() instanceof Boolean)
                addAccountButton.setEnabled(
                        (Boolean)evt.getNewValue());
        }
    }

    /**
     * A custom cell renderer for the network combobox.
     */
    private static class NetworkListCellRenderer
        extends JLabel
        implements ListCellRenderer<AccountRegistrationWizard>
    {
        private static final long serialVersionUID = 0L;

        public NetworkListCellRenderer()
        {
            setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AccountRegistrationWizard> list,
            AccountRegistrationWizard wizard,
            int index, boolean isSelected, boolean cellHasFocus)
        {
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

            if (wizard != null)
            {
                setText(wizard.getProtocolName());

                BufferedImageFuture icon = wizard.getIcon();

                icon.getImageIcon().addToLabel(this);
            }

            return this;
        }
    }

    /**
     * Loads the given wizard in the user interface.
     *
     * @param wizard the wizard to load
     */
    private void loadSelectedWizard(AccountRegistrationWizard wizard)
    {
        accountPanel.removeAll();
        // We must first remove the account panel from the main panel, and then
        // add it back again after replacing the wizard form, otherwise, the
        // height of the dialog is sometimes much bigger then it's content.
        mainPanel.remove(accountPanel);

        JComponent simpleWizardForm = (JComponent) wizard.getSimpleForm(false);
        simpleWizardForm.setOpaque(false);

        accountPanel.add(simpleWizardForm);

        //enable the add  button if this is a real protocol
        boolean isEmptyAccountRegistrationWizard
            = (wizard instanceof EmptyAccountRegistrationWizard);

        addAccountButton.setEnabled(!isEmptyAccountRegistrationWizard);
        if (!isEmptyAccountRegistrationWizard)
            getRootPane().setDefaultButton(addAccountButton);

        accountPanel.revalidate();
        accountPanel.repaint();

        mainPanel.add(accountPanel, BorderLayout.CENTER);
        mainPanel.revalidate();

        this.validate();
        this.pack();
    }

    /**
     * Loads the given error message in the current dialog, by re-validating the
     * content.
     *
     * @param errorMessage The error message to load.
     */
    private void loadErrorMessage(String errorMessage)
    {
        if (errorMessagePane == null)
        {
            errorMessagePane = new JTextArea();
            errorMessagePane.setLineWrap(true);
            errorMessagePane.setWrapStyleWord(true);
            errorMessagePane.setOpaque(false);
            errorMessagePane.setForeground(Color.RED);
            errorMessagePane.setEditable(false);

            accountPanel.add(errorMessagePane, BorderLayout.SOUTH);

            if (isVisible())
                pack();

            //WORKAROUND: there's something wrong happening in this pack and
            //components get cluttered, partially hiding the password text
            // field. I am under the impression that this has something to do
            // with the message pane preferred size being ignored (or being 0)
            // which is why I am adding it's height to the dialog. It's quite
            // ugly so please fix if you have something better in mind.
            this.setSize(getWidth(), getHeight()+errorMessagePane.getHeight());
        }

        errorMessagePane.setText(errorMessage);
        AccessibilityUtils.setName(errorMessagePane, errorMessage);

        accountPanel.revalidate();
        accountPanel.repaint();
    }

    /**
     * Handles button actions.
     * @param event the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent event)
    {
        JButton sourceButton = (JButton) event.getSource();
        AccountRegistrationWizard wizard
            = (AccountRegistrationWizard) networkComboBox.getSelectedItem();

        if (sourceButton.equals(addAccountButton))
        {
            logger.user("Clicked on add account button");
            startConnecting(wizardContainer);

            new Thread(new ProtocolSignInThread(wizard)).start();
        }
        else if (sourceButton.equals(cancelButton))
        {
            logger.user("Clicked on cancel button");
            this.dispose();
        }
    }

    /**
     * Shows the new account dialog if the user is permitted to add a new
     * account.
     */
    public static void showNewAccountDialog()
    {
        // The user is only allowed to have at most 1 chat account.  Also, they
        // are only allowed to see any UI relating to adding an account if they
        // have a manually configured chat account.
        boolean isManual = "Manual".equals(ConfigurationUtils.getImProvSource());
        boolean existingImAccount = AccountUtils.getImAccount() != null;

        boolean newAccountUIPermitted = !existingImAccount && isManual;
        logger.debug("New account permitted? ", newAccountUIPermitted,
                     " is Manual ? ",  isManual,  "existing account? ", existingImAccount);

        if (newAccountUIPermitted)
        {
            logger.debug("Displaying new account dialog");

            // The user is permitted to create a new account, so show the new
            // account dialog.
            if (newAccountDialog == null)
                newAccountDialog = new NewAccountDialog();

            newAccountDialog.pack();
            newAccountDialog.setVisible(true);
            newAccountDialog.requestFocus();
        }
        else
        {
            // The user is not permitted to create a new account, so log an error.
            logger.error("Max chat accounts already configured");
        }
    }

    /**
     * Remove the newAccountDialog, when the window is closed.
     * @param isEscaped indicates if the dialog has been escaped
     */
    protected void close(boolean isEscaped)
    {
        if(isCurrentlySigningIn)
            return;

        dispose();
    }

    /**
     * Remove the newAccountDialog on dispose.
     */
    public void dispose()
    {
        if(isCurrentlySigningIn)
            return;

        if (newAccountDialog == this)
            newAccountDialog = null;

        wizardContainer.getModel().removePropertyChangeListener(this);

        super.dispose();
    }

    /**
     * Overrides set visible to disable closing dialog if currently signing-in.
     *
     * @param visible <tt>true</tt> to make this <tt>NewAccountDialog</tt>
     * visible; otherwise, <tt>false</tt>
     */
    @Override
    public void setVisible(boolean visible)
    {
        if(isCurrentlySigningIn)
            return;

        super.setVisible(visible);
    }

    private void startConnecting(AccountRegWizardContainerImpl wizardContainer)
    {
        isCurrentlySigningIn = true;

        addAccountButton.setEnabled(false);
        cancelButton.setEnabled(false);

        statusLabel.setText(GuiActivator.getResources().getI18NString(
                "service.gui.CONNECTING"));

        setCursor(new Cursor(Cursor.WAIT_CURSOR));
    }

    private void stopConnecting(AccountRegWizardContainerImpl wizardContainer)
    {
        isCurrentlySigningIn = false;

        statusLabel.setText("");

        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        addAccountButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }

    /**
     * Makes protocol operations in different thread.
     */
    private class ProtocolSignInThread
        implements Runnable
    {
        /**
         * The wizard to use.
         */
        private final AccountRegistrationWizard wizard;

        /**
         * Creates <tt>ProtocolSignInThread</tt>.
         * @param wizard the wizard to use.
         */
        ProtocolSignInThread(AccountRegistrationWizard wizard)
        {
            this.wizard = wizard;
        }

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p/>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        public void run()
        {
            ProtocolProviderService protocolProvider;
            try
            {
                if(wizard == emptyWizard)
                {
                    loadErrorMessage(GuiActivator.getResources().getI18NString(
                            "service.gui.CHOOSE_NETWORK"));
                }
                protocolProvider = wizard.signin();

                if (protocolProvider != null)
                {
                    wizardContainer.saveAccountWizard(protocolProvider, wizard);

                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            stopConnecting(wizardContainer);

                            NewAccountDialog.this.dispose();
                        }
                    });
                }
                else
                {
                    // no provider, maybe an error, stop connecting
                    // so we can proceed with the actions
                    stopConnecting(wizardContainer);
                }
            }
            catch (OperationFailedException e)
            {
                // make sure buttons don't stay disabled
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        stopConnecting(wizardContainer);
                    }
                });

                // If the sign in operation has failed we don't want to close
                // the dialog in order to give the user the possibility to
                // retry.
                logger.debug("The sign in operation has failed.");

                if (e.getErrorCode()
                        == OperationFailedException.ILLEGAL_ARGUMENT)
                {
                    loadErrorMessage(GuiActivator.getResources().getI18NString(
                            "service.gui.USERNAME_NULL"));
                }
                else if (e.getErrorCode()
                        == OperationFailedException.IDENTIFICATION_CONFLICT)
                {
                    loadErrorMessage(GuiActivator.getResources().getI18NString(
                            "service.gui.USER_EXISTS_ERROR"));
                }
                else if (e.getErrorCode()
                        == OperationFailedException.SERVER_NOT_SPECIFIED)
                {
                    // If there's an IM domain in the config, use that as the
                    // example domain in the error message.  If not, use the
                    // default example domain from resources.
                    String imDomain =
                        configService.global().getString(
                            "net.java.sip.communicator.im.IM_DOMAIN",
                            GuiActivator.getResources().getI18NString(
                                                 "service.gui.EXAMPLE_DOMAIN"));

                    String errorMessage =
                        GuiActivator.getResources().getI18NString(
                                                   "service.gui.SPECIFY_SERVER",
                                                    new String[] {imDomain});

                    loadErrorMessage(errorMessage);
                }
                else if (e.getErrorCode()
                    == OperationFailedException.INTERNAL_SERVER_ERROR)
                {
                    // There was a server error.  The user cannot resolve
                    // this, so close the login window and display an error to
                    // the user.
                    stopConnecting(wizardContainer);
                    NewAccountDialog.this.dispose();

                    new ErrorDialog(
                        GuiActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.LOGIN_INTERNAL_ERROR"))
                        .showDialog();
                }
                else
                {
                    loadErrorMessage(GuiActivator.getResources().getI18NString(
                            "service.gui.ACCOUNT_CREATION_FAILED",
                            new String[]{e.getMessage()}));
                }
            }
            catch (Exception e)
            {
                // make sure buttons don't stay disabled
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        stopConnecting(wizardContainer);
                    }
                });

                // If the sign in operation has failed we don't want to close
                // the dialog in order to give the user the possibility to
                // retry.
                logger.debug("The sign in operation has failed.");

                loadErrorMessage(GuiActivator.getResources().getI18NString(
                        "service.gui.ACCOUNT_CREATION_FAILED",
                        new String[]{e.getMessage()}));
            }
        }
    }

    /**
     * Sets the selected wizard.
     *
     * @param wizard the wizard to select
     * @param isCreateAccount indicates if the selected wizard should be opened
     * in create account mode
     */
    public void setSelectedWizard(  AccountRegistrationWizard wizard,
                                    boolean isCreateAccount)
    {
        networkComboBox.setSelectedItem(wizard);
    }
}
