/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.launchutils.ProvisioningParams;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * The <tt>AuthenticationWindow</tt> is the window where the user should type
 * his user identifier and password to login.
 *
 * @author Yana Stamcheva
 */
public class AuthenticationWindow extends SIPCommFrame implements ActionListener
{
    private static final long serialVersionUID = 1L;

    /**
     * The resources service that we use to get stored images and strings used
     * in the window.
     */
    private final ResourceManagementService mResources = DesktopUtilActivator.getResources();

    /**
     * Access to the configuration service
     */
    private final ConfigurationService mConfig = DesktopUtilActivator.getConfigurationService();

    /**
     * A store of the authentication windows that are currently being shown.
     * The key to the map is the server that the window is being shown for.
     */
    private static final Map<String, AuthenticationWindow> windowMap = new
            WeakHashMap<>();

    /**
     * Used for logging.
     */
    private static Logger logger = Logger.getLogger(AuthenticationWindow.class);

    /**
     * Info text area.
     */
    private final JTextArea infoTextArea = new JTextArea();

    /**
     * The uin component.
     */
    private JComponent uinValue;

    /**
     * The password field.
     */
    private final JPasswordField passwdField = new JPasswordField(15);

    /**
     * The string to display on the Log in button
     */
    private static final String loginButtonText = DesktopUtilActivator.getResources().getI18NString("service.gui.LOGIN");

    /**
     * The string to display on the Cancel button
     */
    private String cancelButtonText = mResources.getI18NString("service.gui.CANCEL");

    /**
     * The login button.
     */
    private final JButton loginButton = new SIPCommBasicTextButton(loginButtonText);

    /**
     * The cancel button.
     */
    private final JButton cancelButton = new SIPCommBasicTextButton(cancelButtonText);

    /**
     * The check box indicating if the password should be remembered.
     */
    private final JCheckBox rememberPassCheckBox = new SIPCommCheckBox(
            mResources.getI18NString("service.gui.REMEMBER_PASSWORD"),
                mConfig.global().getBoolean(PNAME_SAVE_PASSWORD_TICKED, false));

    /**
     * Property to disable/enable allow save password option
     * in authentication window. By default it is enabled.
     */
    private static final String PNAME_ALLOW_SAVE_PASSWORD =
        "net.java.sip.communicator.util.swing.auth.ALLOW_SAVE_PASSWORD";

    /**
     * Property to set whether the save password option in
     * the authentication window is ticked by default or not.
     * By default it is not ticked
     */
    private static final String PNAME_SAVE_PASSWORD_TICKED =
        "net.java.sip.communicator.util.swing.auth.SAVE_PASSWORD_TICKED";

    /**
     * The name of the server, for which this authentication window is about.
     */
    private String server;

    /**
     * The user name.
     */
    private String userName;

    /**
     * The password.
     */
    private char[] password;

    /**
     * Indicates if the password should be remembered.
     */
    private boolean isRememberPassword = false;

    /**
     * Indicates if the window has been canceled.
     */
    private boolean isCanceled = false;

    /**
     * The condition that decides whether to continue waiting for data.
     */
    private boolean buttonClicked = false;

    /**
     * Used to override username label text.
     */
    private String usernameLabelText = null;

    /**
     * Used to override password label text.
     */
    private String passwordLabelText = null;

    /**
     * The sign up link if specified.
     */
    private String signupLink = null;

    /**
     * The result of the user interaction with this authentication window
     */
    private AuthenticationWindowResult result = null;

    /**
     * Whether login by email is enabled. This is enabled by default but can be disabled in the branding.
     */
    private boolean mEmailLoginEnabled;

    /**
     * Creates an instance of the <tt>LoginWindow</tt>.
     *
     * @param userName the user name to set by default
     * @param password the password to set by default
     * @param server the server name
     * @param isUserNameEditable indicates if the user name is editable
     * @param icon the icon to display on the left of the authentication window
     * @param usernameLabelText customized username field label text
     * @param passwordLabelText customized password field label text
     * @param errorMessage an error message if this dialog is shown to indicate
     * the user that something went wrong
     * @param signupLink an URL that allows the user to sign up
     * @param cancelButtonText the text to use on the cancel button, or null to
     * use the default value
     */
    private AuthenticationWindow(String userName,
                                char[] password,
                                String server,
                                boolean isUserNameEditable,
                                ImageIconFuture icon,
                                String usernameLabelText,
                                String passwordLabelText,
                                String errorMessage,
                                String signupLink,
                                String cancelButtonText)
    {
        super(false);

        this.usernameLabelText = usernameLabelText;
        this.passwordLabelText = passwordLabelText;
        this.signupLink = signupLink;

        init(userName, password, server, isUserNameEditable, icon, errorMessage, cancelButtonText);
    }

    /**
     * Initializes this authentication window.
     * @param server the server
     * @param isUserNameEditable indicates if the user name is editable
     * @param icon the icon to show on the authentication window
     * @param cancelButtonText the text to use for the cancel button, or null to
     * use the default value
     */
    private void init(String userName,
                      char[] password,
                      String server,
                      boolean isUserNameEditable,
                      ImageIconFuture icon,
                      String errorMessage,
                      String cancelButtonText)
    {
        this.server = server;

        initIcon(icon);

        if (cancelButtonText != null)
        {
            this.cancelButtonText = cancelButtonText;
            cancelButton.setText(cancelButtonText);
        }

        if(!isUserNameEditable)
        {
            this.uinValue = new JLabel();
        }
        else
        {
            this.uinValue = new JTextField();
        }

        this.init();
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.enableKeyActions();
        this.setResizable(false);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setAlwaysOnTop(true);

        /*
         * Workaround for the following bug:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4446522
         * Need to pack() the window after it's opened in order to obtain the
         * correct size of our infoTextArea, otherwise window size is wrong and
         * buttons on the south are cut.
         */
        this.addWindowListener(new WindowAdapter()
        {
            public void windowOpened(WindowEvent e)
            {
                pack();
                removeWindowListener(this);
            }
        });

        if (userName != null)
        {
            if (uinValue instanceof JLabel)
            {
                ((JLabel) uinValue).setText(userName);
            }
            else if (uinValue instanceof JTextField)
            {
                ((JTextField) uinValue).setText(userName);
            }
        }

        if (password != null)
        {
            passwdField.setText(new String(password));
        }

        if(errorMessage != null)
        {
            this.infoTextArea.setForeground(Color.RED);
            this.infoTextArea.setText(errorMessage);
        }
    }

    /**
     * Presents a login window and returns the result from that window.  If a
     * window has already been created and is showing then it will return the
     * result from that window. This method blocks until a result is obtained
     * <p/>
     * Note that we determine whether or not a window has already been created
     * from the server name.  I.e. If we are already showing a window with the
     * same server name then we won't create a new window
     *
     * @param userName the user name to set by default
     * @param password the password to set by default
     * @param server the server name this authentication window is about
     * @param isUserNameEditable indicates if the user name should be editable
     * by the user or not
     * @param allowSavePassword true if the user is allowed to save the result
     *        of the login.
     */
    public static AuthenticationWindowResult
                             getAuthenticationResult(String userName,
                                                     char[] password,
                                                     String server,
                                                     boolean isUserNameEditable,
                                                     boolean allowSavePassword)
    {
        AuthenticationWindow aw = new AuthenticationWindow(userName,
                                                           password,
                                                           server,
                                                           isUserNameEditable,
                                                           null,
                                                           null,
                                                           null,
                                                           null,
                                                           null,
                                                           null);
        aw.setAllowSavePassword(allowSavePassword);

        return getResultFromWindow(server, aw);
    }

    /**
     * Presents a login window and returns the result from that window.  If a
     * window has already been created and is showing then it will return the
     * result from that window. This method blocks until a result is obtained
     * <p/>
     * Note that we determine whether or not a window has already been created
     * from the server name.  I.e. If we are already showing a window with the
     * same server name then we won't create a new window
     *
     * @param userName the user name to set by default
     * @param password the password to set by default
     * @param server the server name this authentication window is about
     * @param isUserNameEditable indicates if the user name should be editable
     * by the user or not
     * @param icon the icon displayed on the left of the authentication window
     * @param usernameLabelText customized username field label text
     * @param passwordLabelText customized password field label text
     * @param errorMessage an error message explaining a reason for opening
     * the authentication dialog (when a wrong password was provided, etc.)
     * @param cancelButtonText the text to use for the cancel button, or null to
     * use the default value
     */
    public static AuthenticationWindowResult
                             getAuthenticationResult(String userName,
                                                     char[] password,
                                                     String server,
                                                     boolean isUserNameEditable,
                                                     ImageIconFuture icon,
                                                     String usernameLabelText,
                                                     String passwordLabelText,
                                                     String errorMessage,
                                                     String cancelButtonText)
    {
        AuthenticationWindow aw = new AuthenticationWindow(userName,
                                                           password,
                                                           server,
                                                           isUserNameEditable,
                                                           icon,
                                                           usernameLabelText,
                                                           passwordLabelText,
                                                           errorMessage,
                                                           null,
                                                           cancelButtonText);

        return getResultFromWindow(server, aw);
    }

    /**
     * Presents a login window and returns the result from that window.  If a
     * window has already been created and is showing then it will return the
     * result from that window. This method blocks until a result is obtained
     * <p/>
     * Note that we determine whether or not a window has already been created
     * from the server name.  I.e. If we are already showing a window with the
     * same server name then we won't create a new window
     *
     * @param userName the user name to set by default
     * @param password the password to set by default
     * @param server the server name this authentication window is about
     * @param isUserNameEditable indicates if the user name should be editable
     * by the user or not
     * @param icon the icon displayed on the left of the authentication window
     * @param errorMessage an error message explaining a reason for opening
     * the authentication dialog (when a wrong password was provided, etc.)
     * @param signupLink an URL that allows the user to sign up
     */
    public static AuthenticationWindowResult
                             getAuthenticationResult(String userName,
                                                     char[] password,
                                                     String server,
                                                     boolean isUserNameEditable,
                                                     ImageIconFuture icon,
                                                     String errorMessage,
                                                     String signupLink)
    {
        AuthenticationWindow aw = new AuthenticationWindow(userName,
                                                           password,
                                                           server,
                                                           isUserNameEditable,
                                                           icon,
                                                           null,
                                                           null,
                                                           errorMessage,
                                                           signupLink,
                                                           null);

        return getResultFromWindow(server, aw);
    }

    /**
     * This method either
     * <ul>
     * <li>shows the window that has been passed in, blocking until the user
     *     closes it in some way</li>
     * <li>or it ignores the window that has been passed in and blocks until
     *     the window that is already showing is dismissed.</li>
     * </ul>
     * Regardless of which dialog it blocks on, it returns the result of the
     * dialog that it blocks on.
     *
     * @param server The server for which we are showing this dialog
     * @param aw The window to show if no other window is showing
     * @return the result from the window.
     */
    private static AuthenticationWindowResult getResultFromWindow(
                                                        String server,
                                                        AuthenticationWindow aw)
    {
        AuthenticationWindowResult result;
        boolean windowAlreadyExists;

        // See if there is already a window showing for this server.  If so,
        // replace the window that was passed in with the window that is
        // already being shown.
        synchronized (windowMap)
        {
            windowAlreadyExists = server != null && windowMap.containsKey(server);

            if (windowAlreadyExists)
            {
                logger.debug("Window already exists for " + server);
                aw = windowMap.get(server);
            }
            else if (server != null)
            {
                logger.debug("Window does not exist for " + server);
                windowMap.put(server, aw);
            }
            else
            {
                logger.debug("Server null, not storing window");
            }
        }

        // Window has not been shown yet, so show it
        if (!windowAlreadyExists)
        {
            aw.setVisible(true);
        }

        while (!aw.buttonClicked)
        {
            // If the CDAP ID has changed (the user may have clicked a CDAP URI
            // for a different service provider whilst on this login window than
            // the service provider they selected in the previous CDAP selection
            // window so we should show an error message and quit.
            if (aw.cdapIdHasChanged())
            {
                logger.warn("User selected CDAP ID and login link CDAP ID" +
                            " do not match. Forcing user to close the app.");

                ErrorDialog dialog =
                    new ErrorDialog(aw,
                                    aw.mResources.getI18NString("service.gui.ERROR"),
                                    aw.mResources.getI18NString("plugin.cdap.selection.CDAP_ID_NOT_MATCHING"),
                                    (String)null,
                                    aw.mResources.getI18NString("plugin.cdap.selection.CLOSE"));

                dialog.setModal(true);
                dialog.showDialog();
                aw.close(true);

                // Closing the dialog means that this return isn't really
                // necessary, but in case that happens more slowly than
                // expected, it is safest to include the return to ensure
                // we don't continue logging in.
                return null;
            }

            // Check if we have received a login URI by checking whether we
            // have parsed the subscriber and password from the URI.
            String subscriberFromUri = ProvisioningParams.getSubscriber();
            String passwordFromUri = ProvisioningParams.getPassword();

            if (subscriberFromUri != null)
            {
                // If we've just received a URI with a subscriber on then we
                // should update the subscriber field with the latest value.
                logger.info("Login URI clicked, filling in subscriber field " +
                            "with: " + subscriberFromUri);

                ((JTextField) aw.uinValue).setText(subscriberFromUri);

                if (passwordFromUri != null)
                {
                    // If the URI also contained a password then we should fill
                    // it in and attempt to log the user in automatically.
                    logger.info("Login URI also included password - " +
                                "automatically attempting login");

                    aw.passwdField.setText(passwordFromUri);
                    aw.loginButton.doClick();
                }
                else
                {
                    // Otherwise if we only received a subscriber on the login
                    // URI then we should clear the password field because
                    // the URI did not contain a password parameter so it's
                    // safer to clear the field than assume the password is
                    // for the subscriber parameter on the most recent login
                    // URI.
                    aw.passwdField.setText("");
                }

                // Since we have used the login URI to update the fields we
                // should clear the login URI so that we can catch when we
                // receive another login URI.
                ProvisioningParams.clear();
            }
            else if (passwordFromUri != null)
            {
                // If there was a password but no subscriber on the URI then
                // we should clear both fields since it doesn't make sense to
                // have a password set with no subscriber set.
                logger.info("Login URI clicked with password but no " +
                            "subscriber - clear both fields");

                ((JTextField) aw.uinValue).setText("");
                aw.passwdField.setText("");

                // Since we have used the login URI to update the fields we
                // should clear the login URI so that we can catch when we
                // receive another login URI.
                ProvisioningParams.clear();
            }
        }

        result = aw.result;
        logger.info("AuthenticationWindow dismissed. Username = " +
                    result.getUserName() + ", cancelled? " +
                    result.isCanceled());

        // Finally, remove the window from the map.
        synchronized (windowMap)
        {
            if (server != null)
            {
                windowMap.remove(server);
            }
        }

        return result;
    }

    /**
     * @return whether or not the CDAP ID on the most recently received URI is
     *         different to the CDAP ID selected by the user in the CDAP
     *         selection window.
     */
    private boolean cdapIdHasChanged()
    {
        // If the user didn't manually select a service provider to
        // open this window, the stored service provider ID will either
        // be empty or set to -1. Therefore, assume a value of -1 if
        // no stored config is found.
        String selectedId = mConfig.global().getString(
                    "net.java.sip.communicator.plugin.cdap.service_provider_id",
                    "-1");

        String cdapIdFromUri = ProvisioningParams.getCdapID();

        return cdapIdFromUri != null &&
               !selectedId.equals("-1") &&
               !selectedId.equals(cdapIdFromUri);
    }

    /**
     * Shows or hides the "save password" checkbox.
     * @param allow the checkbox is shown when allow is <tt>true</tt>
     */
    private void setAllowSavePassword(boolean allow)
    {
        rememberPassCheckBox.setVisible(allow);
    }

    /**
     * Initializes the icon image.
     *
     * @param icon the icon to show on the left of the window
     */
    private void initIcon(ImageIconFuture icon)
    {
        // If an icon isn't provided set the application logo icon by default.
        icon = icon.withAlternative(mResources.getImage("service.gui.SIP_COMMUNICATOR_LOGO_64x64"));

        // Create the icon label.
        JLabel iconLabel = icon.addToLabel(new JLabel());
        iconLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        iconLabel.setAlignmentY(Component.TOP_ALIGNMENT);

        // Add the icon to a panel.
        JPanel iconPanel = new TransparentPanel(new BorderLayout());
        iconPanel.add(iconLabel, BorderLayout.NORTH);

        // Add the panel to the window.
        getContentPane().add(iconPanel, BorderLayout.WEST);
    }

    /**
     * Constructs the <tt>LoginWindow</tt>.
     */
    private void init()
    {
        mEmailLoginEnabled = ConfigurationUtils.isEmailLoginEnabled();
        String title = mResources.getI18NString("service.gui.AUTHENTICATION_WINDOW_TITLE", new String[]{server});
        String text = mResources.getI18NString("service.gui.AUTHENTICATION_REQUESTED_SERVER", new String[]{server});
        String uinText;

        if(usernameLabelText != null)
        {
            uinText = usernameLabelText;
        }
        else
        {
            uinText =  mEmailLoginEnabled ? mResources.getI18NString("service.gui.EMAIL_IDENTIFIER") :
                                            mResources.getI18NString("service.gui.IDENTIFIER");
        }

        String passText;

        if(passwordLabelText != null)
        {
            passText = passwordLabelText;
        }
        else
        {
            passText = mResources.getI18NString("service.gui.PASSWORD");
        }

        setTitle(title);

        infoTextArea.setEditable(false);
        infoTextArea.setOpaque(false);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD, ScaleUtils.getDefaultFontSize()));
        infoTextArea.setText(text);
        AccessibilityUtils.setName(infoTextArea, text);
        infoTextArea.setAlignmentX(0.5f);

        JLabel uinLabel = new JLabel(uinText);
        AccessibilityUtils.setName(uinLabel, uinText);
        uinLabel.setFont(uinLabel.getFont().deriveFont(Font.BOLD, ScaleUtils.getDefaultFontSize()));

        String uinValueAccessibilityName = mEmailLoginEnabled ?
                    mResources.getI18NString("service.gui.USERNAME_EMAIL_ACCESS_NAME") :
                    mResources.getI18NString("service.gui.USERNAME_ACCESS_NAME");

        String uinValueAccessibilityDescription =  mEmailLoginEnabled ?
                     mResources.getI18NString("service.gui.USERNAME_EMAIL_ACCESS_DESCRIPTION") :
                     mResources.getI18NString("service.gui.USERNAME_ACCESS_DESCRIPTION");

        AccessibilityUtils.setNameAndDescription(
                uinValue, uinValueAccessibilityName, uinValueAccessibilityDescription);

        JLabel passwdLabel = new JLabel(passText);
        AccessibilityUtils.setName(passwdLabel, passText);
        passwdLabel.setFont(passwdLabel.getFont().deriveFont(Font.BOLD, ScaleUtils.getDefaultFontSize()));

        AccessibilityUtils.setNameAndDescription(
                        passwdField,
                        mResources.getI18NString("service.gui.PASSWORD_ACCESS_NAME"),
                        mResources.getI18NString("service.gui.PASSWORD_ACCESS_DESCRIPTION"));

        TransparentPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1, 8, 8));

        labelsPanel.add(uinLabel);
        labelsPanel.add(passwdLabel);
        labelsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

        TransparentPanel textFieldsPanel = new TransparentPanel(new GridLayout(0, 1, 8, 8));

        ScaleUtils.scaleFontAsDefault(uinValue);
        ScaleUtils.scaleFontAsDefault(passwdField);
        textFieldsPanel.add(uinValue);
        textFieldsPanel.add(passwdField);

        JPanel southFieldsPanel = new TransparentPanel(new GridLayout(1, 2));

        this.rememberPassCheckBox.setOpaque(false);
        this.rememberPassCheckBox.setBorder(null);

        southFieldsPanel.add(rememberPassCheckBox);
        if (signupLink != null && signupLink.length() > 0)
        {
            southFieldsPanel.add(createWebSignupLabel(mResources.getI18NString("plugin.simpleaccregwizz.SIGNUP"),
                                                      signupLink));
        }
        else
        {
            southFieldsPanel.add(new JLabel());
        }

        boolean allowRememberPassword = true;

        String allowRemPassStr = mResources.getSettingsString(PNAME_ALLOW_SAVE_PASSWORD);

        if(allowRemPassStr != null)
        {
            allowRememberPassword = Boolean.parseBoolean(allowRemPassStr);
        }

        allowRememberPassword = mConfig.global().getBoolean(PNAME_ALLOW_SAVE_PASSWORD,
                                                            allowRememberPassword);

        setAllowSavePassword(allowRememberPassword);

        JPanel buttonPanel = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);

        if (OSUtils.IS_MAC)
        {
            buttonPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }

        JPanel southEastPanel = new TransparentPanel(new BorderLayout());
        southEastPanel.add(buttonPanel, BorderLayout.EAST);

        TransparentPanel mainPanel = new TransparentPanel(new BorderLayout(10, 10));

        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 20));

        JPanel mainFieldsPanel = new TransparentPanel(new BorderLayout(0, 10));
        mainFieldsPanel.add(labelsPanel, BorderLayout.WEST);
        mainFieldsPanel.add(textFieldsPanel, BorderLayout.CENTER);
        mainFieldsPanel.add(southFieldsPanel, BorderLayout.SOUTH);

        mainPanel.add(infoTextArea, BorderLayout.NORTH);
        mainPanel.add(mainFieldsPanel, BorderLayout.CENTER);
        mainPanel.add(southEastPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel, BorderLayout.EAST);

        loginButton.setName("ok");
        AccessibilityUtils.setName(loginButton, loginButtonText);

        cancelButton.setName("cancel");
        AccessibilityUtils.setName(cancelButton, cancelButtonText);

        if (loginButton.getPreferredSize().width > cancelButton.getPreferredSize().width)
        {
            cancelButton.setPreferredSize(loginButton.getPreferredSize());
        }
        else
        {
            loginButton.setPreferredSize(cancelButton.getPreferredSize());
        }

        loginButton.setMnemonic(mResources.getI18nMnemonic("service.gui.OK"));
        cancelButton.setMnemonic(mResources.getI18nMnemonic("service.gui.CANCEL"));

        loginButton.addActionListener(this);
        cancelButton.addActionListener(this);

        this.getRootPane().setDefaultButton(loginButton);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the buttons is
     * clicked. When "Login" button is chosen installs a new account from
     * the user input and logs in.
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if ("ok".equals(buttonName))
        {
            if(uinValue instanceof JLabel)
            {
                userName = ((JLabel) uinValue).getText();
            }
            else if(uinValue instanceof JTextField)
            {
                userName = ((JTextField) uinValue).getText();
                formatUserName();
            }

            password = passwdField.getPassword();
            isRememberPassword = rememberPassCheckBox.isSelected();
        }
        else
        {
            isCanceled = true;
        }

        result = new AuthenticationWindowResult(this);

        // release the caller that opened the window
        buttonClicked = true;

        this.dispose();
    }

    /**
     * Formats the string entered into the username field. If email login
     * is enabled, and the string contains a "@", we assume the subscriber
     * has entered an email and simply strip out any whitespace
     * Otherwise, we assume the subscriber has entered a phone number,
     * and remove any odd formatting and characters
     */
    protected void formatUserName()
    {
        String originalUserName = userName;
        if (mEmailLoginEnabled && userName.contains("@"))
        {
            // This looks like an email address. Removing an invalid character
            // in an email address probably isn't going to result in a valid
            // email so just remove any spaces as these may not be visible to the user.
            userName = userName.replaceAll(" ", "");
        }
        else
        {
            //  This is a phone number. Replace odd formatting and characters
            userName = userName.replaceAll(ConfigurationUtils.getPhoneNumberLoginRegex(), "");
        }

        if (!userName.equals(originalUserName))
        {
            logger.user("Log in attempted with username: "
                    + originalUserName + ". reformatted to: " + userName);
        }
    }

    /**
     * Enables the actions when a key is pressed, for now
     * closes the window when esc is pressed.
     */
    private void enableKeyActions()
    {
        @SuppressWarnings("serial")
        UIAction act = new UIAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                close(true);
            }
        };

        getRootPane().getActionMap().put("close", act);

        InputMap imap = this.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }

    /**
     * Automatically clicks the cancel button, when this window is closed.
     *
     * @param isEscaped indicates if the window has been closed by pressing the
     * Esc key
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Shows this modal dialog.
     *
     * @param isVisible specifies whether we should be showing or hiding the
     * window.
     */
    @Override
    public void setVisible(final boolean isVisible)
    {
        this.setName("AUTHENTICATION");

        if(isVisible)
        {
            addWindowFocusListener(new WindowAdapter()
            {
                public void windowGainedFocus(WindowEvent e)
                {
                    removeWindowFocusListener(this);

                    if ((uinValue instanceof JTextField) && ("".equals(((JTextField) uinValue).getText())))
                    {
                        uinValue.requestFocusInWindow();
                    }
                    else
                    {
                        passwdField.requestFocusInWindow();
                    }
                }
            });
        }

        super.setVisible(isVisible);
    }

    /**
     * Creates the subscribe label.
     * @param linkName the link name
     * @return the newly created subscribe label
     */
    private Component createWebSignupLabel(String linkName, final String linkURL)
    {
        JLabel subscribeLabel =
            new JLabel("<html><a href=''>"
                + linkName
                + "</a></html>",
                JLabel.RIGHT);

        subscribeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        subscribeLabel.setToolTipText(mResources.getI18NString("plugin.simpleaccregwizz.SPECIAL_SIGNUP"));

        subscribeLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                try
                {
                    DesktopUtilActivator.getBrowserLauncher()
                        .openURL(linkURL);
                }
                catch (UnsupportedOperationException ex)
                {
                    // This should not happen, because we check if the
                    // operation is supported, before adding the sign
                    // up.
                    logger.error("The web sign up is not supported.", ex);
                }
            }
        });

        return subscribeLabel;
    }

    /**
     * Class showing the result of an authentication window dismissal.
     */
    public static class AuthenticationWindowResult
    {
        private final boolean resultIsCanceled;
        private final boolean resultRememberPassword;
        private final String resultUserName;
        private final char[] resultPassword;

        public AuthenticationWindowResult(AuthenticationWindow aw)
        {
            resultIsCanceled       = aw.isCanceled;
            resultRememberPassword = aw.isRememberPassword;
            resultUserName         = aw.userName;
            resultPassword         = aw.password;
        }

        /**
         * Indicates if this window has been canceled.
         *
         * @return <tt>true</tt> if this window has been canceled, <tt>false</tt> -
         * otherwise
         */
        public boolean isCanceled()
        {
            return resultIsCanceled;
        }

        /**
         * Returns the user name entered by the user or previously set if the
         * user name is not editable.
         *
         * @return the user name
         */
        public String getUserName()
        {
            return resultUserName;
        }

        /**
         * Returns the password entered by the user.
         *
         * @return the password
         */
        public char[] getPassword()
        {
            return resultPassword;
        }

        /**
         * Indicates if the password should be remembered.
         *
         * @return <tt>true</tt> if the password should be remembered,
         * <tt>false</tt> - otherwise
         */
        public boolean isRememberPassword()
        {
            return resultRememberPassword;
        }
    }
}
