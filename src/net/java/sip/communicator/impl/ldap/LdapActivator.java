/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.util.*;

/**
 * Activates the LDAP service
 *
 * @author Sebastien Mazy
 */
public class LdapActivator implements BundleActivator
{
    /**
     * the logger for this class
     */
    private static Logger logger = Logger.getLogger(LdapActivator.class);

    /**
     * The <tt>BundleContext</tt> in which the LDAP plug-in is started.
     */
    private static BundleContext bundleContext = null;

    /**
     * The service through which we access resources.
     */
    private static ResourceManagementService resourceService = null;

    /**
     * The service through which we access config.
     */
    private static ConfigurationService configService = null;

    /**
     * The service used to manipulate the format of phone numbers.
     */
    private static PhoneNumberUtilsService phoneNumberUtilsService = null;

    private static CertificateService certService = null;
    private static CredentialsStorageService credentialsService = null;

    private static LdapFactory factory = new LdapFactoryImpl();

    private static AnalyticsService sAnalyticsService;

    private static final String LDAP_HOSTNAME_CONFIG_KEY = "net.java.sip.communicator.impl.ldap.directories.dir1.hostname";

    /**
     * List of contact source service registrations.
     */
    private static Map<LdapContactSourceService, ServiceRegistration<?>> cssList = new HashMap<>();

    /**
     * Starts the LDAP service
     *
     * @param bundleContext BundleContext
     */
    public void start(BundleContext bundleContext)
    {
        LdapActivator.bundleContext = bundleContext;

        try
        {
            logger.entry();

            LdapDirectorySettings settings = factory.createServerSettings();

            settings.persistentLoad();

            if (settings.isEnabled())
            {
                LdapDirectory directory = factory.createServer(settings);
                registerContactSource(directory);

                AnalyticsService analytics = getAnalyticsService();
                analytics.onEvent(AnalyticsEventType.LDAP_ENABLED,
                    AnalyticsParameter.PARAM_USING_LDAPS,
                    Boolean.toString(LdapConstants.Encryption.SSL.equals(settings.getEncryption())));
            }

            logger.trace("LDAP Service ...[REGISTERED]");

        }
        finally
        {
            logger.exit();
        }
    }

    /**
     * Stops the LDAP service
     *
     * @param bundleContext BundleContext
     */
    public void stop(BundleContext bundleContext)
    {
        for(Map.Entry<LdapContactSourceService, ServiceRegistration<?>> entry :
            cssList.entrySet())
        {
            if (entry.getValue() != null)
            {
                try
                {
                    entry.getValue().unregister();
                }
                finally
                {
                    entry.getKey().stop();
                }
            }
        }
        cssList.clear();
    }

    /**
     * Returns a reference to a ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * ResourceManagementService.
     */
    public static ResourceManagementService getResourceService()
    {
        if (resourceService == null)
        {
            ServiceReference<?> confReference
                = bundleContext.getServiceReference(
                        ResourceManagementService.class.getName());
            resourceService
                = (ResourceManagementService) bundleContext.getService(
                        confReference);
        }
        return resourceService;
    }

    /**
     * See the comment where this function is used in LdapSSLSocketFactoryDelegate for why this
     * function exists.
     *
     * @return LDAP hostname from config.
     */
    static String getLdapHostnameFromConfig()
    {
        return getConfigurationService().user().getString(LDAP_HOSTNAME_CONFIG_KEY);
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null)
        {
            configService = ServiceUtils.getService(bundleContext,
                ConfigurationService.class);
        }

        return configService;
    }

    public static AnalyticsService getAnalyticsService()
    {
        if (sAnalyticsService == null)
        {
            sAnalyticsService =
                    ServiceUtils.getService(bundleContext, AnalyticsService.class);
        }

        return sAnalyticsService;
    }

    /**
     * Returns a reference to a CredentialsStorageConfigurationService
     * implementation currently registered in the bundle context or null if no
     * such implementation was found.
     *
     * @return a currently valid implementation of the
     * CredentialsStorageService.
     */
    static CredentialsStorageService getCredentialsService()
    {
        if (credentialsService == null)
        {
            ServiceReference<?> confReference
                = bundleContext.getServiceReference(
                        CredentialsStorageService.class.getName());
            credentialsService
                = (CredentialsStorageService) bundleContext.getService(
                        confReference);
        }
        return credentialsService;
    }

    /**
     * Returns a reference to a PhoneNumberUtilsService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the PhoneNumberUtilsService.
     */
    public static PhoneNumberUtilsService getPhoneNumberUtilsService()
    {
        if (phoneNumberUtilsService == null)
        {
            phoneNumberUtilsService = ServiceUtils.getService(bundleContext,
                PhoneNumberUtilsService.class);
        }

        return phoneNumberUtilsService;
    }

    /**
     * Gets the <tt>CertificateService</tt> to be used by the functionality of
     * the addrbook plug-in.
     *
     * @return the <tt>CertificateService</tt> to be used by the functionality
     *         of the addrbook plug-in.
     */
    static CertificateService getCertificateService()
    {
        if (certService == null)
        {
            certService
                = ServiceUtils.getService(
                        bundleContext,
                        CertificateService.class);
        }
        return certService;
    }

    /**
     * Enable contact source service with specified LDAP directory.
     *
     * @param ldapDir LDAP directory
     */
    private void registerContactSource(LdapDirectory ldapDir)
    {
        LdapContactSourceService css = new LdapContactSourceService(
                ldapDir);
        ServiceRegistration<?> cssServiceRegistration = null;

        try
        {
            cssServiceRegistration
                = bundleContext.registerService(
                        ContactSourceService.class.getName(),
                        css,
                        null);
        }
        finally
        {
            if (cssServiceRegistration == null)
            {
                css.stop();
                css = null;
            }
            else
            {
                cssList.put(css, cssServiceRegistration);
            }
        }
    }

    /**
     * @return the LdapFactory, used to create LdapDirectory-s, LdapDirectorySettings, LdapQuery, ...
     */
    static LdapFactory getFactory()
    {
        return factory;
    }
}
