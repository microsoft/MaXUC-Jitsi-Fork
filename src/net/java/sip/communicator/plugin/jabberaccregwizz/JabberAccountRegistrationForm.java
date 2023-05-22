/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.wizard.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>JabberAccountRegistrationForm</tt>.
 *
 * @author Yana Stamcheva
 */
public class JabberAccountRegistrationForm
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final AccountPanel accountPanel;

    private final ConnectionPanel connectionPanel;

    private final SecurityPanel securityPanel;

    private final JabberAccountRegistrationWizard wizard;

    /**
     * The panels which value needs validation before we continue.
     */
    private List<ValidatingPanel> validatingPanels =
            new ArrayList<>();

    /**
     * Creates an instance of <tt>JabberAccountRegistrationForm</tt>.
     * @param wizard the parent wizard
     */
    public JabberAccountRegistrationForm(JabberAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        accountPanel = new AccountPanel(this);

        connectionPanel = new ConnectionPanel(this);

        securityPanel = new SecurityPanel(this.getRegistration(), false);
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    void init()
    {
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JabberAccountCreationFormService createService =
            getCreateAccountService();
        if (createService != null)
            createService.clear();

        add(accountPanel, BorderLayout.NORTH);
    }

    /**
     * Parse the server part from the jabber id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     * @param userName the account user name
     */
    void setServerFieldAccordingToUIN(String userName)
    {
        if (!wizard.getRegistration().isServerOverridden())
        {
            connectionPanel.setServerAddress(getServerFromUserName(userName));
        }
    }

    /**
     * Enables/disables the next/finish button of the parent wizard.
     * @param isEnabled <tt>true</tt> to enable the next button, <tt>false</tt>
     * otherwise
     */
    private void setNextFinishButtonEnabled(boolean isEnabled)
    {
        UIService uiService = JabberAccRegWizzActivator.getUIService();

        if (uiService != null)
        {
            uiService.getAccountRegWizardContainer().
                setNextFinishButtonEnabled(isEnabled);
        }
    }

    /**
     * Call this to trigger revalidation of all the input values
     * and change the state of next/finish button.
     */
    void reValidateInput()
    {
        for(ValidatingPanel panel : validatingPanels)
        {
            if(!panel.isValidated())
            {
                setNextFinishButtonEnabled(false);
                return;
            }
        }

        setNextFinishButtonEnabled(true);
    }

    /**
     * Adds panel to the list of panels with values which need validation.
     * @param panel ValidatingPanel.
     */
    public void addValidatingPanel(ValidatingPanel panel)
    {
        validatingPanels.add(panel);
    }

    /**
     * Return the server part of the jabber user name.
     *
     * @param userName the username.
     * @return the server part of the jabber user name.
     */
    static String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            return userName.substring(delimIndex + 1);
        }

        return null;
    }

    /**
     * Return the user part of the user name (i.e. the string before the @
     * sign).
     *
     * @param userName the username.
     * @return the user part of the jabber user name.
     */
    public static String getUserFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            return userName.substring(0, delimIndex);
        }

        return userName;
    }

    /**
     * Returns the server address.
     *
     * @return the server address
     */
    String getServerAddress()
    {
        return wizard.getRegistration().getServerAddress();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     *
     * @param registration the JabberAccountRegistration
     * @return <tt>true</tt> if the page is correctly committed
     */
    public boolean commitPage(JabberAccountRegistration registration)
    {
        String userID = null;
        char[] password = null;
        String serverAddress = null;
        String serverPort = null;

        if (accountPanel.isCreateAccount())
        {
            NewAccount newAccount
                = getCreateAccountService().createAccount();

            if (newAccount != null)
            {
                userID = newAccount.getUserName();
                password = newAccount.getPassword();
                serverAddress = newAccount.getServerAddress();
                serverPort = newAccount.getServerPort();

                if (serverAddress == null)
                    setServerFieldAccordingToUIN(userID);
            }
            else
            {
                // If we didn't succeed to create our new account, we have
                // nothing more to do here.
                return false;
            }
        }
        else
        {
            userID = accountPanel.getUsername();

            if(userID == null || userID.trim().length() == 0)
                throw new IllegalStateException("No user ID provided.");

            if(userID.indexOf('@') < 0
               && registration.getDefaultUserSuffix() != null)
                userID = userID + '@' + registration.getDefaultUserSuffix();

            password = accountPanel.getPassword();
            serverAddress = connectionPanel.getServerAddress();
            serverPort = connectionPanel.getServerPort();
        }

        registration.setUserID(userID);
        registration.setPassword(new String(password));
        registration.setRememberPassword(accountPanel.isRememberPassword());
        registration.setClientCertificateId(
            connectionPanel.getClientTlsCertificateId());
        registration.setServerAddress(serverAddress);
        registration.setServerOverridden(connectionPanel.isServerOverridden());
        registration.setSendKeepAlive(connectionPanel.isSendKeepAlive());
        registration.setResourceAutogenerated(
            connectionPanel.isAutogenerateResourceEnabled());
        registration.setResource(connectionPanel.getResource());

        if (serverPort != null)
            registration.setPort(Integer.parseInt(serverPort));

        String priority = connectionPanel.getPriority();
        if (priority != null)
            registration.setPriority(Integer.parseInt(priority));

        registration.setDTMFMethod(connectionPanel.getDTMFMethod());
        registration.setDtmfMinimalToneDuration(
            connectionPanel.getDtmfMinimalToneDuration());

        securityPanel.commitPanel(registration);

        return true;
    }

    /**
     * Returns a simple version of this registration form.
     * @return the simple form component
     */
    public Component getSimpleForm()
    {
        JabberAccountCreationFormService createAccountService
            = getCreateAccountService();

        if (createAccountService != null)
            createAccountService.clear();

        // Indicate that this panel is opened in a simple form.
        accountPanel.setSimpleForm(true);

        return accountPanel;
    }

    /**
     * Returns the username example.
     * @return the username example string
     */
    public String getUsernameExample()
    {
        return wizard.getUserNameExample();
    }

    /**
     * Sign ups through the web.
     */
    public void webSignup()
    {
        wizard.webSignup();
    }

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return wizard.isWebSignupSupported();
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    public JabberAccountCreationFormService getCreateAccountService()
    {
         return wizard.getCreateAccountService();
    }

    /**
     * Returns the display label used for the jabber id field.
     * @return the jabber id display label string.
     */
    protected String getUsernameLabel()
    {
        return wizard.getUsernameLabel();
    }

    /**
     * Returns the current jabber registration holding all values.
     * @return jabber registration.
     */
    public JabberAccountRegistration getRegistration()
    {
        return wizard.getRegistration();
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getCreateAccountButtonLabel()
    {
        return wizard.getCreateAccountButtonLabel();
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    protected String getCreateAccountLabel()
    {
        return wizard.getCreateAccountLabel();
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getExistingAccountLabel()
    {
        return wizard.getExistingAccountLabel();
    }

    /**
     * Return the string for home page link label.
     * @return the string for home page link label
     */
    protected String getHomeLinkLabel()
    {
        return wizard.getHomeLinkLabel();
    }

    /**
     * Returns the wizard that created the form
     * @return The form wizard
     */
    public JabberAccountRegistrationWizard getWizard()
    {
        return wizard;
    }
}
