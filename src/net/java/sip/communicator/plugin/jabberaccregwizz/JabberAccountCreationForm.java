/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jxmpp.jid.parts.Localpart;

/**
 * Dialog for adding a new Jabber account.
 *
 * @author Nicolas Grandclaude
 * @author Yana Stamcheva
 */
public class JabberAccountCreationForm
    extends TransparentPanel
    implements  JabberAccountCreationFormService
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private static final Logger logger = Logger
        .getLogger(JabberAccountCreationForm.class);

    private JabberServerChooserDialog jabberServerChooserDialog;

    // Panels
    private JPanel userIDPassPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel serverPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    // Labels
    private JLabel serverLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.CSERVER"));

    private JLabel userIDLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.USERNAME"));

    private JLabel passLabel
        = new JLabel(Resources.getString("service.gui.PASSWORD"));

    private JLabel pass2Label
        = new JLabel(Resources.getString(
            "plugin.jabberaccregwizz.PASSWORD_CONFIRM"));

    private JLabel portLabel
        = new JLabel(Resources.getString("service.gui.PORT"));

    // Textfield
    private JTextField serverField = new JTextField();

    private JTextField userIDField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private JPasswordField pass2Field = new JPasswordField();

    private JTextField portField = new JTextField("5222");

    // Button
    private JButton chooseButton = new JButton();

    /**
     * The error text pane.
     */
    private final JTextPane errorPane = new JTextPane();

    /**
     * Creates an instance of <tt>JabberNewAccountDialog</tt>.
     */
    public JabberAccountCreationForm()
    {
        initErrorArea();

        labelsPanel.add(serverLabel);
        labelsPanel.add(userIDLabel);
        labelsPanel.add(passLabel);
        labelsPanel.add(pass2Label);
        labelsPanel.add(portLabel);

        userIDField.setColumns(30);

        serverPanel.add(serverField, BorderLayout.CENTER);
        serverPanel.add(chooseButton, BorderLayout.EAST);
        valuesPanel.add(serverPanel);
        valuesPanel.add(userIDField);
        valuesPanel.add(passField);
        valuesPanel.add(pass2Field);
        valuesPanel.add(portField);

        userIDPassPanel.add(labelsPanel, BorderLayout.WEST);
        userIDPassPanel.add(valuesPanel, BorderLayout.CENTER);

        chooseButton.setText(
            Resources.getString("plugin.jabberaccregwizz.CHOOSE"));
        chooseButton.setMnemonic(
            Resources.getMnemonic("plugin.jabberaccregwizz.CHOOSE"));

        // Choose button open the JabberServerChooserDialog
        chooseButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                jabberServerChooserDialog = new JabberServerChooserDialog();
                if (jabberServerChooserDialog.isOK) // OK pressed in
                                                    // JabberServerChooserDialog
                {
                    serverField
                        .setText(jabberServerChooserDialog.serverSelected);
                }
            }
        });

        this.add(userIDPassPanel, BorderLayout.CENTER);
    }

    /**
     * Connects to the chosen server and creates a new account with Smack.
     *
     * @param server the server domain
     * @param port TCP port to connect
     * @param username the account username
     * @param password the account password
     */
    private boolean createJabberAccount(String server, int port, String username,
        String password)
    {
        try
        {
            XMPPTCPConnectionConfiguration.Builder configBuidler =
                XMPPTCPConnectionConfiguration.builder();
            configBuidler.setHost(server);
            configBuidler.setPort(port);

            XMPPTCPConnectionConfiguration config = configBuidler.build();

            XMPPTCPConnection xmppConnection = new XMPPTCPConnection(config);
            xmppConnection.connect();

            AccountManager accountManager = AccountManager.getInstance(xmppConnection);
            accountManager.createAccount(Localpart.from(username), password);

            return true;
        }
        catch (XMPPErrorException exc)
        {
            if (exc.getStanzaError().getCondition() == StanzaError.Condition.conflict)
            {
                showErrorMessage(Resources.getString(
                        "plugin.jabberaccregwizz.USER_EXISTS_ERROR"));

                logger.error(
                    "Error when created a new Jabber account :" +
                    " user already exist");
            }
            else
            {
                showErrorMessage(Resources.getString(
                        "plugin.jabberaccregwizz.UNKNOWN_XMPP_ERROR"));
            }
            return false;
        }
        catch (Exception exc)
        {
            logger.error("Unknown exception when creating a new Jabber account: "
                , exc);
            return false;
        }
    }

    /**
     * Creates an account.
     *
     * @return the created account
     */
    public NewAccount createAccount()
    {
        String userID = userIDField.getText();
        char[] password = passField.getPassword();
        char[] password2 = pass2Field.getPassword();
        String server = serverField.getText();
        String port = portField.getText();

        if (new String(password).equals(new String(password2)))
        {
            // the two password fields are the same
            boolean result = createJabberAccount(server,
                5222,
                userID,
                new String(password));

            if (result)
            {
                return new NewAccount(
                    getCompleteUserID(userID, server), password, server, port);
            }
        }
        else
        {
            showErrorMessage(Resources.getString(
                    "plugin.jabberaccregwizz.NOT_SAME_PASSWORD"));
        }

        return null;
    }

    /**
     * Returns the create account form.
     *
     * @return the create account form
     */
    public Component getForm()
    {
        return this;
    }

    /**
     * Clears all fields contained in this form.
     */
    public void clear()
    {
        userIDField.setText("");
        passField.setText("");
        pass2Field.setText("");
        serverField.setText(Resources.getSettingsString(
            "plugin.jabberaccregwizz.NEW_ACCOUNT_DEFAULT_SERVER"));
        errorPane.setText("");

        userIDPassPanel.remove(errorPane);
    }

    /**
     * Creates the error area component.
     */
    private void initErrorArea()
    {
        SimpleAttributeSet attribs = new SimpleAttributeSet();
        StyleConstants.setAlignment(attribs, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setFontFamily(attribs, errorPane.getFont().getFamily());
        StyleConstants.setForeground(attribs, Color.RED);
        errorPane.setParagraphAttributes(attribs, true);
        errorPane.setPreferredSize(new Dimension(100, 50));
        errorPane.setMinimumSize(new Dimension(100, 50));
        errorPane.setOpaque(false);
    }

    /**
     * Shows the given error message.
     *
     * @param text the text of the error
     */
    private void showErrorMessage(String text)
    {
        errorPane.setText(text);

        if (errorPane.getParent() == null)
            userIDPassPanel.add(errorPane, BorderLayout.NORTH);

        SwingUtilities.getWindowAncestor(this).pack();
    }

    /**
     * Returns the complete user id, by adding to it the server part.
     *
     * @param userID the username
     * @param server the server address
     * @return the complete user id
     */
    private String getCompleteUserID(String userID, String server)
    {
        if (!userID.contains("@") && server != null && server.length() > 0)
        {
            return userID + "@" + server;
        }

        return userID;
    }
}
