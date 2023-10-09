/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.account;

import java.beans.*;
import java.util.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The <tt>AccountList</tt> is the list of currently registered accounts shown
 * in the options form.
 *
 * @author Yana Stamcheva
 */
@SuppressWarnings("serial")
public class AccountList
    extends JList<Account>
    implements  ProviderPresenceStatusListener,
                RegistrationStateChangeListener,
                ServiceListener
{
    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(AccountList.class);

    /**
     * Property that is changed when an account is enabled/disabled.
     */
    static final String ACCOUNT_STATE_CHANGED = "ACCOUNT_STATE_CHANGED";

    /**
     * The account list model.
     */
    private final AccountListModel accountListModel = new AccountListModel();

    /**
     * The config panel that is displaying this list.
     */
    private final AccountsConfigurationPanel accountConfigPanel;

    /**
     * Creates an instance of this account list by specifying the parent
     * container of the list.
     *
     * @param parentConfigPanel the container where this list is added.
     */
    public AccountList(AccountsConfigurationPanel parentConfigPanel)
    {
        accountConfigPanel = parentConfigPanel;
        setModel(accountListModel);
        setCellRenderer(new AccountListCellRenderer());

        accountsInit();

        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Initializes the accounts table.
     */
    private void accountsInit()
    {
        AccountManager accountManager = GuiActivator.getAccountManager();

        Iterator<AccountID> storedAccounts
            = accountManager.getStoredAccounts().iterator();

        while (storedAccounts.hasNext())
        {
            AccountID accountID = storedAccounts.next();

            boolean isHidden = accountID.getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_PROTOCOL_HIDDEN, false);

            // Hide the account if configured to do so and we're in restricted
            // config mode.
            if (isHidden)
            {
                logger.debug("Hiding account " + accountID.getLoggableAccountID());
                continue;
            }

            Account uiAccount = null;

            if (accountManager.isAccountLoaded(accountID))
            {
                ProtocolProviderService protocolProvider
                    = AccountUtils.getRegisteredProviderForAccount(accountID);

                if (protocolProvider != null)
                {
                    uiAccount = new Account(protocolProvider);

                    protocolProvider.addRegistrationStateChangeListener(this);

                    OperationSetPresence presence
                        = protocolProvider
                            .getOperationSet(OperationSetPresence.class);

                    if (presence != null)
                    {
                        presence.addProviderPresenceStatusListener(this);
                    }
                }
            }
            else
                uiAccount = new Account(accountID);

            if (uiAccount != null)
                accountListModel.addAccount(uiAccount);
        }
    }

    /**
     * Returns the selected account.
     *
     * @return the selected account
     */
    public Account getSelectedAccount()
    {
        return this.getSelectedValue();
    }

    /**
     * Refreshes the account status icon, when the status has changed.
     *
     * @param evt the <tt>ProviderPresenceStatusChangeEvent</tt> that notified
     * us
     */
    public void providerStatusChanged(
                                final ProviderPresenceStatusChangeEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    providerStatusChanged(evt);
                }
            });
            return;
        }

        accountListModelContentChanged(evt.getProvider());
    }

    public void providerStatusMessageChanged(PropertyChangeEvent evt) {}

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState()
                == Bundle.STOPPING)
        {
            return;
        }
        Object sourceService =
            GuiActivator.bundleContext.getService(event.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(sourceService instanceof ProtocolProviderService))
        {
            return;
        }

        ProtocolProviderService protocolProvider
            = (ProtocolProviderService) sourceService;

        AccountID accountID = protocolProvider.getAccountID();

        // If the protocol provider is hidden we don't want to show it in the
        // list.
        boolean isHidden = (accountID.getAccountProperty
                (ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null);

        if (isHidden)
            return;

        // Add or remove the protocol provider from our accounts list.
        if (event.getType() == ServiceEvent.REGISTERED)
        {
            // Add a presence listener in order to listen for any status
            // changes.
            OperationSetPresence presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

            if (presence != null)
            {
                presence.addProviderPresenceStatusListener(this);
            }

            addAccount(protocolProvider);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            boolean enabled = accountID.isEnabled();
            boolean serverAddressValidated =
                accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.SERVER_ADDRESS_VALIDATED, false);

            // If the account is disabled when it unregisters, this means the
            // user has signed out of chat, so we don't want to delete the
            // account. Unless the reason the account is disabled is because it
            // has never successfully registered, in which case the user
            // probably typed an incorrect username so we do want to delete
            // the account.
            logger.debug(
                "Will remove account " + accountID.getLoggableAccountID() +
                " if it IS enabled (enabled = " + enabled + ") and the " +
                "server address is NOT validated (address validated = " +
                serverAddressValidated +")");
            if (enabled || !serverAddressValidated)
            {
                removeAccount(protocolProvider);
            }
        }
    }

    /**
     * Adds the account given by the <tt><ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to add
     */
    private void addAccount(final ProtocolProviderService protocolProvider)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addAccount(protocolProvider);
                }
            });
            return;
        }

        Account account = accountListModel
            .getAccount(protocolProvider.getAccountID());

        if (account != null)
            account.setProtocolProvider(protocolProvider);
        else
            accountListModel.addAccount(new Account(protocolProvider));

        this.repaint();
        accountConfigPanel.updateButtons();
    }

    /**
     * Removes the account given by the <tt><ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to remove
     */
    private void removeAccount(final ProtocolProviderService protocolProvider)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeAccount(protocolProvider);
                }
            });
            return;
        }

        Account account = accountListModel
            .getAccount(protocolProvider.getAccountID());

        // If the unregistered account is a disabled one we don't want to
        // remove it from our list.
        if (account != null && account.isEnabled())
            accountListModel.removeAccount(account);

        this.repaint();
        accountConfigPanel.updateButtons();
    }

    /**
     * Refreshes the account status icon, when the status has changed.
     */
    public void registrationStateChanged(final RegistrationStateChangeEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    registrationStateChanged(evt);
                }
            });
            return;
        }

        accountListModelContentChanged(evt.getProvider());
    }

    /**
     * Notifies <code>accountListModel</code> that the <code>Account</code>s of
     * a specific <code>ProtocolProviderService</code> has changed.
     *
     * @param protocolProvider
     *            the <code>ProtocolProviderService</code> which had its
     *            <code>Account</code>s changed
     */
    private void accountListModelContentChanged(
        ProtocolProviderService protocolProvider)
    {
        Enumeration<?> accounts = accountListModel.elements();

        while (accounts.hasMoreElements())
        {
            Account account = (Account) accounts.nextElement();

            ProtocolProviderService accountProvider
                = account.getProtocolProvider();

            if (accountProvider == protocolProvider)
                accountListModel.contentChanged(account);
        }
    }

    /**
     * Ensures that the account with the given <tt>accountID</tt> is removed
     * from the list.
     *
     * @param accountID the identifier of the account
     */
    public void ensureAccountRemoved(final AccountID accountID)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    ensureAccountRemoved(accountID);
                }
            });
            return;
        }

        Account account = accountListModel.getAccount(accountID);

        if (account != null)
            accountListModel.removeAccount(account);

        accountConfigPanel.updateButtons();
    }
}
