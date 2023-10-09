/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.jabberaccregwizz;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import java.awt.*;
import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * The <tt>JabberAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Jabber protocol. It should allow
 * the user to create and configure a new Jabber account.
 *
 * @author Yana Stamcheva
 */
public class JabberAccountRegistrationWizard
    extends DesktopAccountRegistrationWizard
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(JabberAccountRegistrationWizard.class);

    /**
     * The first wizard page.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * The registration object, where all properties related to the account
     * are stored.
     */
    private JabberAccountRegistration registration;

    /**
     * The <tt>ProtocolProviderService</tt> of this account.
     */
    private ProtocolProviderService protocolProvider;

    /**
     * The create account form.
     */
    private JabberAccountCreationForm createAccountService;

    /**
     * The configuration service.
     */
    private ConfigurationService configService;

    /**
     * If true, the IM domain is forced to be the one specified in SIP PS.
     */
    private static final String ENFORCE_IM_DOMAIN_PROP =
                               "net.java.sip.communicator.im.ENFORCE_IM_DOMAIN";

    /**
     * String representing the name of the IM domain in the configuration
     * service.
     */
    private static final String IM_DOMAIN_PROP =
                                       "net.java.sip.communicator.im.IM_DOMAIN";

    /**
     * Creates an instance of <tt>JabberAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public JabberAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        configService = JabberAccRegWizzActivator.getConfigurationService();

        setWizardContainer(wizardContainer);

        wizardContainer
            .setFinishButtonText(Resources.getString("service.gui.SIGN_IN"));
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    public BufferedImageFuture getIcon()
    {
        return Resources.getBufferedImage(Resources.PROTOCOL_ICON);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     * @return String
     */
    public String getProtocolName()
    {
        return Resources.getString("plugin.jabberaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator<WizardPage> getPages()
    {
        return getPages(new JabberAccountRegistration());
    }

    /**
     * Returns the set of pages contained in this wizard.
     *
     * @param registration the registration object
     * @return Iterator
     */
    public Iterator<WizardPage> getPages(JabberAccountRegistration registration)
    {
        java.util.List<WizardPage> pages = new ArrayList<>();

        // create new registration, our container needs the pages
        // this means this is a new wizard and we must reset all data
        // it will be invoked and when the wizard cleans and unregister
        // our pages, but this fix don't hurt in this situation.
        this.registration = registration;

        if (firstWizardPage == null)
            firstWizardPage = new FirstWizardPage(this);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
     */
    public Iterator<Map.Entry<String,String>> getSummary()
    {
        Hashtable<String,String> summaryTable = new Hashtable<>();

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.USERNAME"),
            registration.getUserID());

        summaryTable.put(
            Resources.getString("service.gui.REMEMBER_PASSWORD"),
            Boolean.toString(registration.isRememberPassword()));

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.SERVER"),
            registration.getServerAddress());

        summaryTable.put(
            Resources.getString("service.gui.PORT"),
            String.valueOf(registration.getPort()));

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.ENABLE_KEEP_ALIVE"),
            String.valueOf(registration.isSendKeepAlive()));

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.RESOURCE"),
            registration.getResource());

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.PRIORITY"),
            String.valueOf(registration.getPriority()));

        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.DTMF_METHOD"),
            registration.getDTMFMethod());

        summaryTable.put(
            Resources.getString(
                "plugin.sipaccregwizz.DTMF_MINIMAL_TONE_DURATION"),
            registration.getDtmfMinimalToneDuration());

        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account defined in this wizard.
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        if (("Manual".equals(ConfigurationUtils.getImProvSource())) &&
            configService.global().getBoolean(ENFORCE_IM_DOMAIN_PROP, false))
        {
            // The IM domain has been enforced by the service provider.
            // Strip off any domain that the user may have entered and add
            // the IM domain from config to the username.
            String domain = configService.user().getString(IM_DOMAIN_PROP);

            if (!StringUtils.isNullOrEmpty(domain))
            {
                logger.info("Enforcing IM domain " + domain);

                String username = registration.getUserID();
                username = username.split("@")[0] + "@" + domain;
                registration.setUserID(username);
                registration.setServerAddress(domain);
            }
            else
            {
                String errorMessage = "IM domain enforced but not supplied";
                logger.error(errorMessage);
                throw new OperationFailedException(
                    errorMessage, OperationFailedException.INTERNAL_SERVER_ERROR);
            }
        }
        else
        {
            // Check the registration contains a server name.  If it doesn't, this
            // method will throw an OperationFailedException and we will display
            // an error to the user.
            getServerFromRegistration();
        }

        return signin(  registration.getUserID(),
                        registration.getPassword());
    }

    /**
     * Get the server name from the registration
     *
     * @return The server name
     * @throws OperationFailedException if there is no server name in the
     * registration.
     */
    private String getServerFromRegistration() throws OperationFailedException
    {
        String userName = registration.getUserID();
        String serverAddress = registration.getServerAddress();
        String serverName = null;

        if (serverAddress != null && serverAddress.length() > 0)
        {
            logger.debug("Server address from registration: " + serverAddress);
            serverName = serverAddress;
        }
        else
        {
            logger.debug("Server address from username: " + serverAddress);
            serverName = getServerFromUserName(userName);
        }

        if (serverName == null || serverName.length() == 0)
        {
            String errorMessage = "No server specified for username " + sanitiseChatAddress(userName);
            logger.error(errorMessage);
            throw new OperationFailedException(errorMessage,
                OperationFailedException.SERVER_NOT_SPECIFIED);
        }

        return serverName;
    }

    /**
     * Installs the account defined in this wizard.
     *
     * @param userName the user name to sign in with
     * @param password the password to sign in with
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public ProtocolProviderService signin(String userName, String password)
        throws OperationFailedException
    {
        logger.info("Sign in to account");

        boolean rememberPassword = false;

        // If firstWizardPage is null we are requested sign-in from
        // another registration form, so we must create the firstWizardPage in
        // order to set default values.
        if(firstWizardPage == null)
        {
            firstWizardPage = new FirstWizardPage(this);

            // Always remember the password if we are signing-in from another
            // form.
            rememberPassword = true;
        }

        // Initialize the form values, in case we are calling this method from
        // another registration form.
        AccountPanel accPanel = (AccountPanel)firstWizardPage.getSimpleForm();
        accPanel.setUsername(userName);
        accPanel.setPassword(password);

        // Remember the password if the box is already ticked on the
        // form or if we came from another registration form.
        accPanel.setRememberPassword(password != null &&
                                     (accPanel.isRememberPassword() ||
                                     rememberPassword));

        firstWizardPage.commitPage();

        if(!firstWizardPage.isCommitted())
            throw new OperationFailedException("Could not confirm data.",
                OperationFailedException.GENERAL_ERROR);

        ProtocolProviderFactory factory
            = JabberAccRegWizzActivator.getJabberProtocolProviderFactory();

        return this.installAccount(
            factory,
            registration.getUserID(),  // The user id may get changed.Server
                                       // part can be added in the data
                                       // commit.
            password);
    }

    /**
     * Creates an account for the given user and password.
     *
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param userName the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @throws OperationFailedException if the operation didn't succeed
     */
    protected ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory,
        String userName,
        String passwd)
        throws OperationFailedException
    {
        logger.info("Preparing to install account for user " + sanitiseChatAddress(userName));
        Hashtable<String, String> accountProperties
            = new Hashtable<>();

        accountProperties.put(ProtocolProviderFactory.IS_PREFERRED_PROTOCOL,
            Boolean.toString(isPreferredProtocol()));
        accountProperties.put(ProtocolProviderFactory.PROTOCOL, getProtocol());
        String protocolIconPath = getProtocolIconPath();
        if (protocolIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.PROTOCOL_ICON_PATH,
                                    protocolIconPath);

        String accountIconPath = getAccountIconPath();
        if (accountIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                                    accountIconPath);

        // Password is null in case of SSO login.
        if (registration.isRememberPassword() && passwd != null)
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        //accountProperties.put("SEND_KEEP_ALIVE",
        //                      String.valueOf(registration.isSendKeepAlive()));

        String serverName = getServerFromRegistration();

        if (registration.isServerOverridden())
        {
            accountProperties.put(
                ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                Boolean.toString(true));
        }
        else
        {
            accountProperties.put(
                ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                Boolean.toString(false));
        }

        if (userName.indexOf('@') < 0 &&
            registration.getDefaultUserSuffix() != null)
        {
            userName = userName + '@' + registration.getDefaultUserSuffix();
        }

        accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
            serverName);

        String smsServerAddress = registration.getSmsServerAddress();

        String clientCertId = registration.getClientCertificateId();
        if (clientCertId != null)
        {
            accountProperties.put(
                    ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE,
                    clientCertId);
        }
        else
        {
            accountProperties.remove(
                    ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE);
        }

        if (smsServerAddress != null)
        {
            accountProperties.put(  ProtocolProviderFactory.SMS_SERVER_ADDRESS,
                                    smsServerAddress);
        }

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
                            String.valueOf(registration.getPort()));

        accountProperties.put(ProtocolProviderFactory.AUTO_GENERATE_RESOURCE,
                        String.valueOf(registration.isResourceAutogenerated()));

        accountProperties.put(ProtocolProviderFactory.RESOURCE,
                            registration.getResource());

        accountProperties.put(ProtocolProviderFactory.RESOURCE_PRIORITY,
                            String.valueOf(registration.getPriority()));

        String accountDisplayName = registration.getAccountDisplayName();

        if (accountDisplayName != null && accountDisplayName.length() > 0)
            accountProperties.put(  ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME,
                                    accountDisplayName);

        if(registration.getDTMFMethod() != null)
            accountProperties.put("DTMF_METHOD",
                registration.getDTMFMethod());
        else
            accountProperties.put("DTMF_METHOD",
                registration.getDefaultDTMFMethod());

        accountProperties.put(
                ProtocolProviderFactory.DTMF_MINIMAL_TONE_DURATION,
                registration.getDtmfMinimalToneDuration());

        accountProperties.put(ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                Boolean.toString(registration.isDefaultEncryption()));

        // Sets the ordered list of encryption protocols.
        registration.addEncryptionProtocolsToProperties(
                accountProperties);

        // Sets the list of encryption protocol status.
        registration.addEncryptionProtocolStatusToProperties(
                accountProperties);

        accountProperties.put(ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
                Boolean.toString(registration.isSipZrtpAttribute()));

        accountProperties.put(ProtocolProviderFactory.SDES_CIPHER_SUITES,
            registration.getSDesCipherSuites());

        try
        {
            logger.debug("Will install account for user " + sanitiseChatAddress(userName)
                         + " with the following properties."
                         + accountProperties);

            AccountID accountID = providerFactory.installAccount(
                userName,
                accountProperties);

            ServiceReference<?> serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
                JabberAccRegWizzActivator.bundleContext
                .getService(serRef);
        }
        catch (IllegalArgumentException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Username, password or server is null.",
                OperationFailedException.ILLEGAL_ARGUMENT);
        }
        catch (IllegalStateException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Account already exists.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (Throwable exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Failed to add account.",
                OperationFailedException.GENERAL_ERROR);
        }

        return protocolProvider;
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     *
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public JabberAccountRegistration getRegistration()
    {
        if (registration == null)
            registration = new JabberAccountRegistration();

        return registration;
    }

    /**
     * Returns the size of this wizard.
     * @return the size of this wizard
     */
    public Dimension getSize()
    {
        return new Dimension(300, 480);
    }

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        String userNameExample =
            Resources.getString("plugin.xmppaccountprompter.USERNAME_HELP");

        // If there's an IM domain in the config, use that in the example
        // domain.  If not, use the default example domain from resources.
        String imDomain =
            configService.user().getString("net.java.sip.communicator.im.IM_DOMAIN",
                Resources.getString("service.gui.EXAMPLE_DOMAIN"));

        userNameExample = userNameExample + '@' + imDomain;

        return userNameExample;
    }

    /**
     * Parse the server part from the jabber id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     *
     * @param userName the full JID that we'd like to parse.
     *
     * @return returns the server part of a full JID
     */
    protected String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            String newServerAddr = userName.substring(delimIndex + 1);
            return newServerAddr;
        }

        return null;
    }

    /**
     * Opens the Gmail signup URI in the OS's default browser.
     */
    public void webSignup()
    {
    }

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return false;
    }

    /**
     * Returns a simple account registration form that would be the first form
     * shown to the user. Only if the user needs more settings she'll choose
     * to open the advanced wizard, consisted by all pages.
     *
     * @param isCreateAccount indicates if the simple form should be opened as
     * a create account form or as a login form
     * @return a simple account registration form
     */
    public Object getSimpleForm(boolean isCreateAccount)
    {
        // when creating first wizard page, create and new
        // AccountRegistration to avoid reusing old instances and
        // data left from old registrations
        return getSimpleForm(new JabberAccountRegistration(), isCreateAccount);
    }

    /**
     * Returns the first wizard page.
     *
     * @param registration the registration object
     * @param isCreateAccount indicates if the simple form should be opened as
     * a create account form or as a login form
     * @return the first wizard page.
     */
    public Object getSimpleForm(JabberAccountRegistration registration,
                                boolean isCreateAccount)
    {
        this.registration = registration;

        firstWizardPage = new FirstWizardPage(this);

        return firstWizardPage.getSimpleForm();
    }

    /**
     * Returns the protocol name as listed in "ProtocolNames" or just the name
     * of the service.
     * @return the protocol name
     */
    public String getProtocol()
    {
        return ProtocolNames.JABBER;
    }

    /**
     * Returns the protocol icon path.
     * @return the protocol icon path
     */
    public String getProtocolIconPath()
    {
        return null;
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    public String getAccountIconPath()
    {
        return null;
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    protected JabberAccountCreationFormService getCreateAccountService()
    {
        if (createAccountService == null)
            createAccountService = new JabberAccountCreationForm();

        return createAccountService;
    }

    /**
     * Returns the display label used for the jabber id field.
     * @return the jabber id display label string.
     */
    protected String getUsernameLabel()
    {
        return Resources.getString("plugin.jabberaccregwizz.USERNAME");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getCreateAccountButtonLabel()
    {
        return Resources.getString(
            "plugin.jabberaccregwizz.NEW_ACCOUNT_TITLE");
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    protected String getCreateAccountLabel()
    {
        return Resources.getString(
            "plugin.jabberaccregwizz.REGISTER_NEW_ACCOUNT_TEXT");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getExistingAccountLabel()
    {
        return Resources.getString("plugin.jabberaccregwizz.EXISTING_ACCOUNT");
    }

    /**
     * Return the string for home page link label.
     * @return the string for home page link label
     */
    protected String getHomeLinkLabel()
    {
        return null;
    }

    /**
     * Return the wizard's protocolProvider, if the wizard modifies an
     * account, null if it creates a new one
     * @return the wizard's protocolProvider
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return null;
    }
}
