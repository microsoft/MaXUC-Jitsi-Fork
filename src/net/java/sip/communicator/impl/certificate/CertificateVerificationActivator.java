/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.certificate;

import java.security.GeneralSecurityException;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The certificate verification bundle activator.
 *
 * @author Yana Stamcheva
 */
public class CertificateVerificationActivator
    implements BundleActivator
{
    /**
     * The bundle context for this bundle.
     */
    protected static BundleContext bundleContext;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The service giving access to all resources.
     */
    private static ResourceManagementService resourcesService;

    /**
     * The service to store and access passwords.
     */
    private static CredentialsStorageService credService;

    /**
     * Called when this bundle is started.
     *
     * @param bc The execution context of the bundle being started.
     */
    public void start(BundleContext bc)
    {
        bundleContext = bc;

        try
        {
            bundleContext.registerService(
                CertificateService.class.getName(),
                new CertificateServiceImpl(),
                null);
        }
        catch (GeneralSecurityException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bc The execution context of the bundle being stopped.
     */
    public void stop(BundleContext bc)
    {
        configService = null;
        resourcesService = null;
        credService = null;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }

    /**
     * Returns the <tt>CredentialsStorageService</tt>, through which we will
     * access all passwords.
     *
     * @return the <tt>CredentialsStorageService</tt>, through which we will
     * access all passwords.
     */
    public static CredentialsStorageService getCredService()
    {
        if (credService == null)
        {
            credService
                = ServiceUtils.getService(
                        bundleContext,
                        CredentialsStorageService.class);
        }
        return credService;
    }
}
