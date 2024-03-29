/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;
import org.jitsi.util.StringUtils;

import net.java.sip.communicator.plugin.desktoputil.SIPCommCheckBox;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.desktoputil.TrimTextField;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * @author Yana Stamcheva
 */
public class AccountPanel
    extends TransparentPanel
    implements  DocumentListener,
                ValidatingPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private static final Logger logger = Logger.getLogger(AccountPanel.class);

    private final JPanel userIDPassPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private final JPanel labelsPanel = new TransparentPanel();

    private final JPanel valuesPanel = new TransparentPanel();

    private final JLabel passLabel
        = new JLabel(Resources.getString("service.gui.PASSWORD"));

    private final JPanel emptyPanel = new TransparentPanel();

    private final JTextField userIDField = new TrimTextField();

    private final JPasswordField passField = new JPasswordField();

    private final JCheckBox rememberPassBox = new SIPCommCheckBox(Resources
        .getString("service.gui.REMEMBER_PASSWORD"));

    /**
     * Panel to hold the "change password" button
     */
    private final JPanel changePasswordPanel
            = new TransparentPanel(new BorderLayout(10,10));

    /**
     * "Change password" button
     */
    private final JButton changePasswordButton
        = new JButton(Resources.getString(
            "plugin.jabberaccregwizz.CHANGE_PASSWORD"));

    /**
     * A pane to show a message in the "change password" panel
     */
    private final JEditorPane changePasswordMessagePane = new JEditorPane();

    private final JabberAccountRegistrationForm parentForm;

    private final JRadioButton existingAccountButton;

    private final JRadioButton createAccountButton;

    private final JPanel mainPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private Component registrationForm;

    private Component registerChoicePanel;

    /**
     * Creates an instance of <tt>AccountPanel</tt> by specifying the parent
     * wizard page, where it's contained.
     * @param parentForm the parent form where this panel is contained
     */
    public AccountPanel(final JabberAccountRegistrationForm parentForm)
    {
        super(new BorderLayout());

        this.parentForm = parentForm;
        this.parentForm.addValidatingPanel(this);

        JLabel userIDLabel
            = new JLabel(parentForm.getUsernameLabel());
        ScaleUtils.scaleFontAsDefault(userIDLabel);

        JLabel userIDExampleLabel = new JLabel(parentForm.getUsernameExample());

        AccessibilityUtils.setNameAndDescription(userIDExampleLabel,
                                                 userIDExampleLabel.getText(),
                                                 Resources.getString("plugin.jabberaccregwizz.USER_ID_EXAMPLE_DESCRIPTION"));

        labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

        valuesPanel.setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));

        userIDField.getDocument().addDocumentListener(this);
        rememberPassBox.setSelected(true);

        existingAccountButton = new JRadioButton(
            parentForm.getExistingAccountLabel());

        createAccountButton = new JRadioButton(
            parentForm.getCreateAccountLabel());

        userIDExampleLabel.setForeground(Color.GRAY);
        userIDExampleLabel.setFont(userIDExampleLabel.getFont().
            deriveFont(ScaleUtils.getScaledFontSize(8f)));
        emptyPanel.setMaximumSize(new Dimension(40, 30));
        userIDExampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        labelsPanel.add(userIDLabel);
        labelsPanel.add(emptyPanel);
        ScaleUtils.scaleFontAsDefault(passLabel);
        labelsPanel.add(passLabel);

        // Add some padding to the labels to ensure the alignment is correct
        int top = OSUtils.IS_MAC ? 6 : 1;
        userIDLabel.setBorder(BorderFactory.createEmptyBorder(top, 0, 0, 0));
        passLabel.setBorder(BorderFactory.createEmptyBorder(top, 0, 0, 0));

        AccessibilityUtils.setNameAndDescription(userIDLabel,
            userIDLabel.getText(),
            Resources.getString("plugin.jabberaccregwizz.USERNAME_DESCRIPTION"));
        AccessibilityUtils.setNameAndDescription(passLabel,
            passLabel.getText(),
            Resources.getString("plugin.jabberaccregwizz.PASSWORD_DESCRIPTION"));

        userIDField.setAlignmentX(LEFT_ALIGNMENT);
        ScaleUtils.scaleFontAsDefault(userIDField);
        valuesPanel.add(userIDField);
        userIDExampleLabel.setAlignmentX(LEFT_ALIGNMENT);
        valuesPanel.add(userIDExampleLabel);
        passField.setAlignmentX(LEFT_ALIGNMENT);
        ScaleUtils.scaleFontAsDefault(passField);
        valuesPanel.add(passField);

        AccessibilityUtils.setName(
            userIDField,
            Resources.getString("plugin.jabberaccregwizz.ENTER_USERNAME"));

        AccessibilityUtils.setName(
            passField,
            Resources.getString("plugin.jabberaccregwizz.ENTER_PASSWORD"));

        userIDPassPanel.add(labelsPanel, BorderLayout.WEST);
        userIDPassPanel.add(valuesPanel, BorderLayout.CENTER);

        JPanel southPanel = new TransparentPanel(new BorderLayout());

        String homeLinkString = parentForm.getHomeLinkLabel();

        if (homeLinkString != null && homeLinkString.length() > 0)
        {
            String homeLink = JabberAccRegWizzActivator.getConfigurationService().global().getString(
                    "service.gui.APPLICATION_WEB_SITE");

            southPanel.add( createHomeLink(homeLinkString, homeLink),
                            BorderLayout.EAST);
        }

        userIDPassPanel.add(southPanel, BorderLayout.SOUTH);

        changePasswordPanel.setBorder(BorderFactory.createTitledBorder(
                Resources.getString(
                    "plugin.jabberaccregwizz.CHANGE_PASSWORD")));

        changePasswordButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                // This button is never enabled, and hence never pressed
//                JabberPasswordChangeDialog passwordChangeDialog
//                        = new JabberPasswordChangeDialog();
//                passwordChangeDialog.setVisible(true);
            }
        });

        changePasswordMessagePane.setOpaque(false);
        changePasswordMessagePane.setBorder(BorderFactory.
                createEmptyBorder(0, 5, 10, 0));
        changePasswordMessagePane.setEditable(false);

        changePasswordPanel.add(changePasswordMessagePane, BorderLayout.NORTH);
        changePasswordPanel.add(changePasswordButton, BorderLayout.SOUTH);

        //we only show that when showChangePasswordPanel is called
        changePasswordPanel.setVisible(false);

        this.add(mainPanel, BorderLayout.NORTH);
    }
    /**
     * Creates a register choice panel.
     * @return the created component
     */
    private Component createRegisterChoicePanel()
    {
        JPanel registerChoicePanel = new TransparentPanel(new BorderLayout());

        existingAccountButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (existingAccountButton.isSelected())
                {
                    mainPanel.remove(registrationForm);
                    mainPanel.add(userIDPassPanel, BorderLayout.CENTER);

                    Window window
                        = SwingUtilities.getWindowAncestor(AccountPanel.this);

                    if (window != null)
                        window.pack();
                }
            }
        });

        createAccountButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (createAccountButton.isSelected())
                {
                    mainPanel.remove(userIDPassPanel);
                    mainPanel.add(registrationForm, BorderLayout.CENTER);
                    SwingUtilities.getWindowAncestor(AccountPanel.this).pack();
                }
            }
        });

        ButtonGroup buttonGroup = new ButtonGroup();

        existingAccountButton.setOpaque(false);
        createAccountButton.setOpaque(false);

        buttonGroup.add(existingAccountButton);
        buttonGroup.add(createAccountButton);

        // Add a sign-in prompt to the window.
        JTextArea signInPrompt = new JTextArea();
        ScaleUtils.scaleFontAsDefault(signInPrompt);
        signInPrompt.setEditable(false);
        signInPrompt.setOpaque(false);
        signInPrompt.setLineWrap(true);
        signInPrompt.setWrapStyleWord(true);
        signInPrompt.setFont(signInPrompt.getFont().deriveFont(Font.BOLD));

        // Check whether the IM domain is enforced.  If so, include it in the
        // sign-in text in the login dialog.
        ConfigurationService configService =
            JabberAccRegWizzActivator.getConfigurationService();

        String signInText;

        boolean isManualProv =
            "Manual".equals(ConfigurationUtils.getImProvSource());

        boolean isEnforceImDomain = configService.user().getBoolean(
            "net.java.sip.communicator.im.ENFORCE_IM_DOMAIN", false);

        String imDomain =
            configService.user().getString("net.java.sip.communicator.im.IM_DOMAIN");

        if (isManualProv && isEnforceImDomain &&
            !StringUtils.isNullOrEmpty(imDomain, true))
        {
            ResourceManagementService res =
                JabberAccRegWizzActivator.getResourcesService();

            imDomain = imDomain.split("\\.")[0];

            logger.debug("IM domain enforced - including  " + imDomain +
                " in login dialog");

            signInText = res.getI18NString(
                "plugin.xmppaccountprompter.SIGN_IN_PROMPT_SPECIFIC",
                new String[]{imDomain});
        }
        else
        {
            logger.debug("IM domain not enforced");
            signInText =
                Resources.getString("plugin.xmppaccountprompter.SIGN_IN_PROMPT");
        }

        signInPrompt.setText(signInText);
        AccessibilityUtils.setName(signInPrompt, signInText);

        // Set the size of the JTextArea to ensure that text wrapping works.
        signInPrompt.setSize(new Dimension(
            (int) userIDPassPanel.getPreferredSize().getWidth(), 1));

        registerChoicePanel.add(signInPrompt, BorderLayout.NORTH);

        // By default we select the existing account button.
        existingAccountButton.setSelected(true);

        return registerChoicePanel;
    }

    /**
     * Creates the home link label.
     *
     * @param homeLinkText the text of the home link
     * @param homeLink the link
     * @return the created component
     */
    private Component createHomeLink(   String homeLinkText,
                                        final String homeLink)
    {
        JLabel homeLinkLabel =
            new JLabel("<html><a href='"+ homeLink +"'>"
                + homeLinkText + "</a></html>",
                JLabel.RIGHT);

        homeLinkLabel.setFont(homeLinkLabel.getFont().deriveFont(
            ScaleUtils.getMediumFontSize()));
        homeLinkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        homeLinkLabel.setToolTipText(
                Resources.getString(
                "plugin.simpleaccregwizz.SPECIAL_SIGNUP"));
        homeLinkLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                try
                {
                    JabberAccRegWizzActivator.getBrowserLauncher()
                        .openURL(homeLink);
                }
                catch (UnsupportedOperationException ex)
                {
                    // This should not happen, because we check if the
                    // operation is supported, before adding the sign
                    // up.
                    logger.error("The web sign up is not supported.",
                        ex);
                }
            }
        });

        return homeLinkLabel;
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the
     * UserID field. Enables or disables the "Next" wizard button according to
     * whether the UserID field is empty.
     *
     * @param evt the document event that has triggered this method call.
     */
    public void insertUpdate(DocumentEvent evt)
    {
        parentForm.setServerFieldAccordingToUIN(userIDField.getText());
        parentForm.reValidateInput();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the User ID field. Enables or disables the "Next" wizard button
     * according to whether the User ID field is empty.
     *
     * @param evt the document event that has triggered this method call.
     */
    public void removeUpdate(DocumentEvent evt)
    {
        parentForm.setServerFieldAccordingToUIN(userIDField.getText());
        parentForm.reValidateInput();
    }

    public void changedUpdate(DocumentEvent evt) {}

    /**
     * Returns the username entered in this panel.
     * @return the username entered in this panel
     */
    String getUsername()
    {
        return userIDField.getText();
    }

    /**
     * Sets the username to display in the username field.
     * @param username the username to set
     */
    void setUsername(String username)
    {
        userIDField.setText(username);
        userIDField.setEnabled(false);
    }

    /**
     * Returns the password entered in this panel.
     * @return the password entered in this panel
     */
    char[] getPassword()
    {
        return passField.getPassword();
    }

    /**
     * Sets the password to display in the password field of this panel.
     * @param password the password to set
     */
    void setPassword(String password)
    {
        passField.setText(password);
    }

    /**
     * Indicates if the remember password box is selected.
     * @return <tt>true</tt> if the remember password check box is selected,
     * otherwise returns <tt>false</tt>
     */
    boolean isRememberPassword()
    {
        return rememberPassBox.isSelected();
    }

    /**
     * Selects/deselects the remember password check box depending on the given
     * <tt>isRemember</tt> parameter.
     * @param isRemember indicates if the remember password checkbox should be
     * selected or not
     */
    void setRememberPassword(boolean isRemember)
    {
        rememberPassBox.setSelected(isRemember);
    }

    /**
     * Whether current inserted values into the panel are valid and enough
     * to continue with account creation/modification.
     *
     * @return whether the input values are ok to continue with account
     * creation/modification.
     */
    public boolean isValidated()
    {
        return userIDField.getText() != null
                && userIDField.getText().length() > 0;
    }

    /**
     * Sets to <tt>true</tt> if this panel is opened in a simple form and
     * <tt>false</tt> if it's opened in an advanced form.
     *
     * @param isSimpleForm indicates if this panel is opened in a simple form or
     * in an advanced form
     */
    void setSimpleForm(boolean isSimpleForm)
    {
        JabberAccountCreationFormService createAccountService
            = parentForm.getCreateAccountService();

        mainPanel.removeAll();

        if (isSimpleForm)
        {
            if (createAccountService != null)
            {
                registrationForm = createAccountService.getForm();
                registerChoicePanel = createRegisterChoicePanel();

                mainPanel.add(registerChoicePanel, BorderLayout.NORTH);
            }
            else
            {
                JPanel registerPanel = new TransparentPanel();

                registerPanel.setLayout(
                    new BoxLayout(registerPanel, BoxLayout.Y_AXIS));

                String createAccountInfoString
                    = parentForm.getCreateAccountLabel();

                if (createAccountInfoString != null
                    && createAccountInfoString.length() > 0)
                {
                    registerPanel.add(
                        createRegisterArea(createAccountInfoString));
                }

                String createAccountString
                    = parentForm.getCreateAccountButtonLabel();

                if (createAccountString != null
                        && createAccountString.length() > 0)
                {
                    JPanel buttonPanel = new TransparentPanel(
                            new FlowLayout(FlowLayout.CENTER));

                    buttonPanel.add(createRegisterButton(createAccountString));

                    registerPanel.add(buttonPanel);
                }

                mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
                mainPanel.add(userIDPassPanel);
                mainPanel.add(Box.createVerticalStrut(10));

                if (registerPanel.getComponentCount() > 0)
                {
                    registerPanel.setBorder(
                        BorderFactory.createTitledBorder(""));

                    mainPanel.add(registerPanel);
                }
            }
        }
        else
        {
            mainPanel.add(userIDPassPanel, BorderLayout.NORTH);
            mainPanel.add(changePasswordPanel, BorderLayout.SOUTH);
        }
    }

    /**
     * Indicates if the account information provided by this form is for new
     * account or an existing one.
     * @return <tt>true</tt> if the account information provided by this form
     * is for new account or <tt>false</tt> if it's for an existing one
     */
    boolean isCreateAccount()
    {
        return createAccountButton.isSelected();
    }

    /**
     * Creates the register area.
     *
     * @param text the text to show to the user
     * @return the created component
     */
    private Component createRegisterArea(String text)
    {
        JEditorPane registerArea = new JEditorPane();

        registerArea.setAlignmentX(JEditorPane.CENTER_ALIGNMENT);
        registerArea.setOpaque(false);
        registerArea.setContentType("text/html");
        registerArea.setEditable(false);
        registerArea.setText(text);
        /* Display the description with the font we use elsewhere in the UI. */
        registerArea.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                true);
        registerArea.addHyperlinkListener(new HyperlinkListener()
            {
                public void hyperlinkUpdate(HyperlinkEvent e)
                {
                    if (e.getEventType()
                            .equals(HyperlinkEvent.EventType.ACTIVATED))
                    {
                        JabberAccRegWizzActivator
                            .getBrowserLauncher().openURL(e.getURL().toString());
                    }
                }
            });

        return registerArea;
    }

    /**
     * Creates the register button.
     *
     * @param text the text of the button
     * @return the created component
     */
    private Component createRegisterButton(String text)
    {
        JButton registerButton = new JButton(text);

        registerButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                logger.debug("Reg OK");

                if (parentForm.isWebSignupSupported())
                {
                    parentForm.webSignup();
                }
            }
        });

        return registerButton;
    }
}
