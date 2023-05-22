/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;

import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.desktoputil.wizard.SecurityPanel;

/**
 * The <tt>SIPAccountRegistrationForm</tt>.
 *
 * @author Yana Stamcheva
 * @author Grogorii Balutsel
 */
public class SIPAccountRegistrationForm
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final AccountPanel accountPanel;
    private final ConnectionPanel connectionPanel;
    private final SecurityPanel securityPanel;
    private final PresencePanel presencePanel;

    private final SIPAccountRegistrationWizard wizard;

    /**
     * The panels which value needs validation before we continue.
     */
    private List<ValidatingPanel> validatingPanels =
            new ArrayList<>();

    /**
     * Creates an instance of <tt>SIPAccountRegistrationForm</tt>.
     * @param wizard the parent wizard
     */
    public SIPAccountRegistrationForm(SIPAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());
        this.wizard = wizard;

        accountPanel = new AccountPanel(this);
        connectionPanel = new ConnectionPanel(this);
        securityPanel = new SecurityPanel(this.getRegistration(), true);
        presencePanel = new PresencePanel(this);
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    void init()
    {
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        accountPanel.initAdvancedForm();

        SIPAccountCreationFormService createService = getCreateAccountService();
        if (createService != null)
            createService.clear();

        add(accountPanel, BorderLayout.NORTH);
    }

    /**
     * Parse the server part from the sip id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     * @param userName the account user name
     * @return the server address
     */
    String setServerFieldAccordingToUIN(String userName)
    {
        String serverAddress = getServerFromUserName(userName);

        connectionPanel.setServerFieldAccordingToUIN(serverAddress);

        return serverAddress;
    }

    /**
     * Enables/disables the next/finish button of the parent wizard.
     * @param isEnabled <tt>true</tt> to enable the next button, <tt>false</tt>
     * otherwise
     */
    private void setNextFinishButtonEnabled(boolean isEnabled)
    {
        SIPAccRegWizzActivator.getUIService().getAccountRegWizardContainer()
            .setNextFinishButtonEnabled(isEnabled);
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
     * Return the server part of the sip user name.
     *
     * @param userName the username.
     * @return the server part of the sip user name.
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
     * Saves the user input when the "Next" wizard buttons is clicked.
     *
     * @param registration the SIPAccountRegistration
     * @return
     */
    public boolean commitPage(SIPAccountRegistration registration)
    {
        String userID = null;
        char[] password = null;
        String serverAddress = null;
        String proxyAddress = null;
        String xcapRoot = null;
        if (accountPanel.isCreateAccount())
        {
            NewAccount newAccount
                = getCreateAccountService().createAccount();
            if (newAccount != null)
            {
                userID = newAccount.getUserName();
                password = newAccount.getPassword();
                serverAddress = newAccount.getServerAddress();
                proxyAddress = newAccount.getProxyAddress();
                xcapRoot = newAccount.getXcapRoot();

                if (serverAddress == null)
                    serverAddress = setServerFieldAccordingToUIN(userID);

                if (proxyAddress == null)
                    proxyAddress = serverAddress;
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
            userID = accountPanel.getUserID();

            if(getServerFromUserName(userID) == null
                && registration.getDefaultDomain() != null)
            {
                // we have only a username and we want to add
                // a default domain
                userID = userID + "@" + registration.getDefaultDomain();
                setServerFieldAccordingToUIN(userID);
            }

            password = accountPanel.getPassword();
            serverAddress = connectionPanel.getServerAddress();
            proxyAddress = connectionPanel.getProxy();
        }

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        registration.setUserID(userID);

        if (password != null)
            registration.setPassword(new String(password));

        registration.setRememberPassword(accountPanel.isRememberPassword());

        registration.setServerAddress(serverAddress);

        registration.setProxy(proxyAddress);

        String displayName = accountPanel.getDisplayName();
        registration.setDisplayName(displayName);

        String authName = connectionPanel.getAuthenticationName();
        if(authName != null && authName.length() > 0)
            registration.setAuthorizationName(authName);

        registration.setServerPort(connectionPanel.getServerPort());
        registration.setProxyPort(connectionPanel.getProxyPort());

        registration.setPreferredTransport(
            connectionPanel.getSelectedTransport());

        registration.setProxyAutoConfigure(
            connectionPanel.isProxyAutoConfigureEnabled());

        registration.setEnablePresence(
            presencePanel.isPresenceEnabled());
        registration.setPresenceType(
            presencePanel.getPresenceType());
        registration.setForceP2PMode(
            presencePanel.isForcePeerToPeerMode());
        registration.setTlsClientCertificate(
            connectionPanel.getCertificateId());
        registration.setPollingPeriod(
            presencePanel.getPollPeriod());
        registration.setSubscriptionExpiration(
            presencePanel.getSubscriptionExpiration());

        // set the keepalive method only if its not already set by some custom
        // extending wizard like sip2sip
        if(registration.getKeepAliveMethod() == null)
            registration.setKeepAliveMethod(
                connectionPanel.getKeepAliveMethod());

        registration.setKeepAliveInterval(
            connectionPanel.getKeepAliveInterval());

        registration.setDTMFMethod(
            connectionPanel.getDTMFMethod());
        registration.setDtmfMinimalToneDuration(
            connectionPanel.getDtmfMinimalToneDuration());

        SIPAccRegWizzActivator.getUIService().getAccountRegWizardContainer()
            .setBackButtonEnabled(true);

        securityPanel.commitPanel(registration);

        if(xcapRoot != null)
        {
            registration.setXCapEnable(true);
            registration.setClistOptionServerUri(xcapRoot);
        }
        else
        {
            registration.setXCapEnable(presencePanel.isXCapEnable());
            registration.setXiVOEnable(presencePanel.isXiVOEnable());
            registration.setClistOptionServerUri(
                    presencePanel.getClistOptionServerUri());
        }

        registration.setClistOptionUseSipCredentials(
                presencePanel.isClistOptionUseSipCredentials());
        registration.setClistOptionUser(presencePanel.getClistOptionUser());
        registration.setClistOptionPassword(
            new String(presencePanel.getClistOptionPassword()));
        registration.setMessageWaitingIndications(
            connectionPanel.isMessageWaitingEnabled());
        registration.setVoicemailURI(connectionPanel.getVoicemailURI());
        registration.setVoicemailCheckURI(connectionPanel.getVoicemailCheckURI());

        return true;
    }

    /**
     * Returns a simple version of this registration form.
     * @return the simple form component
     */
    public Component getSimpleForm()
    {
        SIPAccountCreationFormService createAccountService
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
     * Returns the sign up link name.
     * @return the sign up link name
     */
    public String getWebSignupLinkName()
    {
        return wizard.getWebSignupLinkName();
    }

    /**
     * Returns the forgot password link name.
     *
     * @return the forgot password link name
     */
    public String getForgotPasswordLinkName()
    {
        return wizard.getForgotPasswordLinkName();
    }

    /**
     * Returns the forgot password link.
     *
     * @return the forgot password link
     */
    public String getForgotPasswordLink()
    {
        return wizard.getForgotPasswordLink();
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    public SIPAccountCreationFormService getCreateAccountService()
    {
         return wizard.getCreateAccountService();
    }

    /**
     * Returns the display label used for the sip id field.
     * @return the sip id display label string.
     */
    protected String getUsernameLabel()
    {
        return wizard.getUsernameLabel();
    }

    /**
     * Returns the current sip registration holding all values.
     * @return sip registration.
     */
    public SIPAccountRegistration getRegistration()
    {
        return wizard.getRegistration();
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
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    protected String getCreateAccountLabel()
    {
        return wizard.getCreateAccountLabel();
    }

    /**
     * Selects the create account button.
     */
    void setCreateButtonSelected()
    {
        accountPanel.setCreateButtonSelected();
    }
}
