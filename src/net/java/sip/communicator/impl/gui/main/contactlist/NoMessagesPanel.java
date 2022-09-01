// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.text.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.ServiceUtils.*;
import net.java.sip.communicator.util.account.*;

/**
 * The <tt>NoMessagesPanel</tt> replaces the contact list, when a
 * <tt>MessageHistoryFilter</tt> finds no matches. It should give instructions
 * on how the user can start chats
 *
 */
public class NoMessagesPanel extends JPanel
                             implements ServiceListener,
                                        RegistrationStateChangeListener
{
    private static final long serialVersionUID = 1L;

    private static final Logger sLog = Logger.getLogger(NoMessagesPanel.class);

    /**
     * The contact list that this panel is displayed in.
     */
    private final TreeContactList contactList;

    /**
     * The button that provides an action to the user depending on the current
     * scenario
     */
    private final JButton button = new JButton();

    /**
     * The text area that provides helpful information to the user
     */
    private final JTextPane textArea = new JTextPane();

    /**
     * The resource management service
     */
    private ResourceManagementService resources;

    /**
     * The panel that takes up the whole space of the contact list pane
     */
    private TransparentPanel mainPanel;

    /**
     * The panel that contains the buttons
     */
    private TransparentPanel buttonPanel;

    /**
     * The IM protocol provider service
     */
    private ProtocolProviderService imProvider;

    /**
     * Constructor - creates a NoMessagesPanel including setting out the UI
     * elements.
     *
     * @param contactList the contact list that this panel is displayed in.
     */
    public NoMessagesPanel(TreeContactList contactList)
    {
        super(new BorderLayout());

        this.contactList = contactList;

        // Add this panel as a service listener and a registration state change
        // listener so the UI can update as appropriate
        imProvider = AccountUtils.getImProvider();
        if (imProvider != null)
        {
            imProvider.addRegistrationStateChangeListener(this);
        }
        else
        {
            GuiActivator.bundleContext.addServiceListener(this);
        }

        mainPanel = new TransparentPanel(new BorderLayout());
        mainPanel.setVisible(false);
        buttonPanel = new TransparentPanel(new FlowLayout());

        resources = GuiActivator.getResources();
        setBackground(new Color(
                           resources.getColor("service.gui.LIGHT_BACKGROUND")));

        textArea.setOpaque(false);
        textArea.setEditable(false);
        StyledDocument doc = textArea.getStyledDocument();

        MutableAttributeSet standard = new SimpleAttributeSet();
        StyleConstants.setAlignment(standard, StyleConstants.ALIGN_CENTER);
        StyleConstants.setFontFamily(standard, textArea.getFont().getFamily());
        StyleConstants.setFontSize(standard, ScaleUtils.scaleInt(12));
        doc.setParagraphAttributes(0, 0, standard, true);

        buttonPanel.add(button);
        mainPanel.add(textArea, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.NORTH);

        ServiceUtils.getService(GuiActivator.bundleContext,
                                AccountManager.class,
                                new ServiceCallback<AccountManager>()
        {
            @Override
            public void onServiceRegistered(final AccountManager service)
            {
                if (service.isProtocolFactoryProcessed(ProtocolNames.JABBER))
                {
                    init();
                }
                else
                {
                    // Add a listener for the Jabber protocol provider service
                    // being loaded so our UI is not out of date.
                    service.addListener(new AccountManagerListener()
                    {
                        @Override
                        public void handleAccountManagerEvent(AccountManagerEvent event)
                        {
                            if (event.getType() == AccountManagerEvent.STORED_ACCOUNTS_LOADED &&
                                event.getFactory().getProtocolName().equals(ProtocolNames.JABBER))
                            {
                                    // Account manager has loaded the Jabber
                                    // account so call init again to ensure we are
                                    // showing the correct UI
                                    init();

                                    service.removeListener(this);
                            }
                        }
                    });
                }
            }
        });
    }

    private void init()
    {
        // What we show here depends on whether the user:
        // * has a chat account
        // * is signed into chant
        // * has any chat history
        button.setVisible(false);
        mainPanel.setVisible(true);
        imProvider = AccountUtils.getImProvider();
        if (imProvider != null && imProvider.isRegistered())
        {
            // User has a registered IM account so they have no chat messages.
            textArea.setText(resources.getI18NString("service.gui.chat.NO_CHAT_MESSAGES"));
        }
        else
        {
            // User is not registered but that does not mean that AD is not
            // attempting to register in the background.  Use the same logic
            // as applied in the AccountStatusPanel to determine whether to
            // display the 'Chat sign in' button, or not.
            AccountManager accountManager = GuiActivator.getAccountManager();
            List<AccountID> jabberAccounts =
                accountManager.getStoredAccountsforProtocol("Jabber");
            AccountID imAccount = AccountUtils.getImAccount();
            boolean haveImAccount = (imAccount != null);
            boolean signedIntoChat = haveImAccount && imAccount.isEnabled();
            boolean offline = GuiActivator.getGlobalStatusService().isOffline();
            boolean canLogIntoChat = offline && haveImAccount && !signedIntoChat;

            sLog.debug("Can log into chat now " + canLogIntoChat);

            if (jabberAccounts.size() > 0)
            {
                // We could be not logged in because, for example, there is no
                // network connection so don't just throw up the button!
                if (canLogIntoChat)
                {
                    // User has an IM account but it is not currently registered.
                    // Allow them to toggle the enabled state of the provider.
                    String res;
                    if (ConfigurationUtils.isSmsEnabled())
                    {
                        res = "service.gui.chat.ENABLE_CHAT_SMS";
                    }
                    else
                    {
                        res = "service.gui.chat.SIGN_IN_CHAT";
                    }

                    textArea.setText(resources.getI18NString("service.gui.chat.CHAT_ACCOUNT_OFFLINE"));
                    button.setText(resources.getI18NString(res));
                    button.addActionListener(new ToggleChatAccountActionListener());
                    button.setVisible(true);
                    buttonPanel.setVisible(true);
                }
            }
            else
            {
                // User does not have an IM account, allow them to add one from here
                textArea.setText(resources.getI18NString("service.gui.chat.NO_IM_ACCOUNT"));
                button.setText(resources.getI18NString("service.gui.ADD_DESCRIPTION"));
                button.addActionListener(new AddChatAccountActionListener());
                button.setVisible(true);
                buttonPanel.setVisible(true);
            }
        }
    }

    /**
     * An action listener that toggles the IM account enabled state
     */
    private class ToggleChatAccountActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent evt)
        {
            sLog.debug("User has toggled chat account online/offline status");
            AccountManager accountManager = GuiActivator.getAccountManager();
            List<AccountID> jabberAccounts =
                accountManager.getStoredAccountsforProtocol("Jabber");

            for (AccountID jabberAccount : jabberAccounts)
            {
                accountManager.toggleAccountEnabled(jabberAccount);
            }

            // Refresh message history so we don't display message history items
            // when signed out of chat.
            contactList.messageHistoryChanged();
        }
    }

    /**
     * An action listener that shows the chat account
     */
    private class AddChatAccountActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent evt)
        {
            sLog.debug("User has asked to add chat account");
            NewAccountDialog.showNewAccountDialog();
        }
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        // The registration state has changed of the Jabber account, so
        // re-initialize the UI in case we are out of date.
        init();
    }

    @Override
    public void serviceChanged(ServiceEvent evt)
    {
        ServiceReference<?> serviceRef = evt.getServiceReference();

        Object service = GuiActivator.bundleContext.getService(serviceRef);

        sLog.debug("Service " + service + " changed");

        // We don't care if the service isn't the Jabber protocol provider
        if (service instanceof ProtocolProviderService)
        {
            if (((ProtocolProviderService) service).getProtocolName().equals(
                                                          ProtocolNames.JABBER))
            {
                if (imProvider != null)
                {
                    imProvider.removeRegistrationStateChangeListener(this);
                }
                imProvider = ((ProtocolProviderService) service);
                imProvider.addRegistrationStateChangeListener(this);
            }

            GuiActivator.bundleContext.removeServiceListener(this);
        }
    }

    /**
     * Removes all listeners that may be registered for this class
     */
    public void dispose()
    {
        if (imProvider != null)
        {
            imProvider.removeRegistrationStateChangeListener(this);
        }

        GuiActivator.bundleContext.removeServiceListener(this);
    }
}
