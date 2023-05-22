/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.account;

import java.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The implementation of the <tt>AccountRegistrationWizardContainer</tt>.
 *
 * @author Yana Stamcheva
 */
public class AccountRegWizardContainerImpl
    extends Wizard
    implements  WizardContainer,
                ServiceListener
{
    private static final long serialVersionUID = 0L;
    private static final Logger logger =
        Logger.getLogger(AccountRegWizardContainerImpl.class);

    private AccountRegistrationWizard currentWizard;

    private final ConfigurationService configService
        = GuiActivator.getConfigurationService();

    private final Map<String, AccountRegistrationWizard> registeredWizards =
            new Hashtable<>();

    public AccountRegWizardContainerImpl(AbstractMainFrame mainFrame)
    {
        super(mainFrame);

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.ACCOUNT_REGISTRATION_WIZARD"));

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
            for (ServiceReference<?> serRef : accountWizardRefs)
            {
                String protocolName = (String)
                    serRef.getProperty(ProtocolProviderFactory.PROTOCOL);
                AccountRegistrationWizard wizard = (AccountRegistrationWizard)
                    GuiActivator.bundleContext.getService(serRef);

                this.addAccountRegistrationWizard(protocolName, wizard);
            }
        }

        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Adds the given <tt>AccountRegistrationWizard</tt> to the list of
     * containing wizards.
     *
     * @param wizard the <tt>AccountRegistrationWizard</tt> to add
     */
    public void addAccountRegistrationWizard(   String protocolName,
                                                AccountRegistrationWizard wizard)
    {
        synchronized (registeredWizards)
        {
            registeredWizards.put(protocolName, wizard);
        }
    }

    /**
     * Removes the given <tt>AccountRegistrationWizard</tt> from the list of
     * containing wizards.
     *
     * @param wizard the <tt>AccountRegistrationWizard</tt> to remove
     */
    public void removeAccountRegistrationWizard(String protocolName,
                                                AccountRegistrationWizard wizard)
    {
        synchronized (registeredWizards)
        {
            registeredWizards.remove(protocolName);
        }
    }

    /**
     * Returns the wizard corresponding to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * corresponding wizard we're looking for
     * @return the corresponding wizard
     */
    public AccountRegistrationWizard getProtocolWizard(
                                    ProtocolProviderService protocolProvider)
    {
        AccountRegistrationWizard res
            = registeredWizards.get(protocolProvider.getProtocolDisplayName());

        // compatibility check, some protocols have changed name
        // and when they have those name saved in configuration
        // cannot be edited, so lets check whether there is a wizard
        // with the same protocol name like the one of its provider
        if(res == null)
        {
            //lets find matching protocol name in registered wizards
            Iterator<AccountRegistrationWizard> iter =
                registeredWizards.values().iterator();
            while(iter.hasNext())
            {
                AccountRegistrationWizard wizard = iter.next();
                if(wizard.getProtocolName()
                    .equals(protocolProvider.getProtocolName()))
                {
                    res = wizard;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Saves the (protocol provider, wizard) pair in through the
     * <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider to save
     * @param wizard the wizard to save
     */
    public void saveAccountWizard(ProtocolProviderService protocolProvider,
        AccountRegistrationWizard wizard)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts =
            configService.user().getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.user().getString(accountRootPropName);

            if (accountUID.equals(
                    protocolProvider.getAccountID().getAccountUniqueID()))
            {
                configService.user().setProperty(accountRootPropName + ".wizard",
                    wizard.getClass().getName().replace('.', '_'));

                savedAccount = true;
            }
        }

        if (!savedAccount)
        {
            String accountPackage
                = prefix + ".acc" + Long.toString(System.currentTimeMillis());

            configService.user().setProperty(accountPackage, protocolProvider
                .getAccountID().getAccountUniqueID());

            configService.user().setProperty(accountPackage + ".wizard", wizard);
        }
    }

    /**
     * Returns the currently used <tt>AccountRegistrationWizard</tt>.
     *
     * @return the currently used <tt>AccountRegistrationWizard</tt>
     */
    public AccountRegistrationWizard getCurrentWizard()
    {
        return currentWizard;
    }

    /**
     * Unregisters all pages added by the current wizard.
     */
    public void unregisterWizardPages()
    {
        Iterator<WizardPage> i = this.getCurrentWizard().getPages();

        while (i.hasNext())
        {
            WizardPage page = i.next();

            this.unregisterWizardPage(page.getIdentifier());
        }
    }

    /**
     * Handles registration of a new account wizard.
     */
    public void serviceChanged(ServiceEvent event)
    {
        if(!GuiActivator.isStarted)
            return;

        ServiceReference<?> serRef = event.getServiceReference();
        Object sService = GuiActivator.bundleContext.getService(serRef);

        // we don't care if the source service is not a plugin component
        if (! (sService instanceof AccountRegistrationWizard))
            return;

        String protocolName
            = (String) serRef.getProperty(ProtocolProviderFactory.PROTOCOL);
        AccountRegistrationWizard wizard
            = (AccountRegistrationWizard) sService;

        switch (event.getType()) {
        case ServiceEvent.REGISTERED:
            logger
                .info("Handling registration of a new Account Wizard.");

            this.addAccountRegistrationWizard(protocolName, wizard);
            break;
        case ServiceEvent.UNREGISTERING:
            this.removeAccountRegistrationWizard(protocolName, wizard);
            break;
        }
    }
}
