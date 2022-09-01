/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>SIPAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class SIPAccRegWizzActivator
    extends AbstractServiceDependentActivator
{
    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private static final Logger logger =
        Logger.getLogger(SIPAccRegWizzActivator.class);

    private static WizardContainer wizardContainer;

    /**
     * A reference to the configuration service.
     */
    private static ConfigurationService configService;

    private static BrowserLauncherService browserLauncherService;

    private static SIPAccountRegistrationWizard sipWizard;

    private static UIService uiService;

    private static CertificateService certService;

    /**
     * Starts this bundle.
     */
    public void start(Object dependentService)
    {
        uiService = (UIService)dependentService;

        wizardContainer = uiService.getAccountRegWizardContainer();

        if (wizardContainer != null)
        {
            sipWizard = new SIPAccountRegistrationWizard(wizardContainer);

            Hashtable<String, String> containerFilter
                = new Hashtable<>();

            containerFilter.put(
                    ProtocolProviderFactory.PROTOCOL,
                    ProtocolNames.SIP);

            bundleContext.registerService(
                AccountRegistrationWizard.class.getName(),
                sipWizard,
                containerFilter);
        }
    }

    /**
     * The dependent class. We are waiting for the ui service.
     * @return the ui service class.
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * The bundle context to use.
     * @param context the context to set.
     */
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    public void stop(BundleContext bundleContext)
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the SIP protocol.
     *
     * @return the <tt>ProtocolProviderFactory</tt> for the SIP protocol
     */
    public static ProtocolProviderFactory getSIPProtocolProviderFactory()
    {
        ServiceReference<?>[] serRefs = null;
        String osgiFilter
            = "("
                + ProtocolProviderFactory.PROTOCOL
                + "="
                + ProtocolNames.SIP
                + ")";

        try
        {
            serRefs
                = bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("SIPAccRegWizzActivator : " + ex);
            return null;
        }

        return
            (serRefs == null)
                ? null
                : (ProtocolProviderFactory)
                    bundleContext.getService(serRefs[0]);
    }

    /**
     * Returns the <tt>UIService</tt>.
     *
     * @return the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        return uiService;
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher() {
        if (browserLauncherService == null) {
            ServiceReference<?> serviceReference = bundleContext
                .getServiceReference(BrowserLauncherService.class.getName());

            browserLauncherService = (BrowserLauncherService) bundleContext
                .getService(serviceReference);
        }

        return browserLauncherService;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null)
        {
            ServiceReference<?> serviceReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService)bundleContext
                .getService(serviceReference);
        }

        return configService;
    }

    /**
     * Returns the <tt>CertificateService</tt> obtained from the bundle
     * context.
     * @return the <tt>CertificateService</tt> obtained from the bundle
     * context
     */
    public static CertificateService getCertificateService()
    {
        if (certService == null)
        {
            ServiceReference<?> serviceReference = bundleContext
                .getServiceReference(CertificateService.class.getName());

            certService = (CertificateService)bundleContext
                .getService(serviceReference);
        }

        return certService;
    }
}
