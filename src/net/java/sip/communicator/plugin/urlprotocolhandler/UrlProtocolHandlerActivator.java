// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.urlprotocolhandler;

import java.beans.*;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;

/**
 * Activator for the URL Protocol Handler.  Responsible for allowing the user to
 * configure whether the app is used by the OS to handle sip, tel, and callto
 * URIs.
 */
public class UrlProtocolHandlerActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>UrlProtocolHandlerActivator</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(UrlProtocolHandlerActivator.class);

    /**
     * OSGi bundle context.
     */
    private static BundleContext context;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The resources service.
     */
    private static ResourceManagementService resourcesService;

    private PropertyChangeListener propChangeListener;

    @Override
    public void start(BundleContext bundleContext)
    {
        // The UrlProtocolHandler bundle is available on Windows only.
        if (!OSUtils.IS_WINDOWS)
            return;

        logger.info("UrlProtocolHandler plugin ... [STARTED]");

        context = bundleContext;

        // Make sure the Jitsi config option and the Windows registry entry
        // that control whether we should be used to handle SIP and TEL
        // protocols are in sync.
        String urlProtocolHandlerAppString =
            getConfigurationService().user().getString(
                ConfigurationUtils.URL_PROTOCOL_HANDLER_APP_PROP);

        logger.debug("Url protocol handler config = " +
                                                   urlProtocolHandlerAppString);

        if(urlProtocolHandlerAppString == null)
        {
            logger.debug("Current user has NO config option to specify default URL handler app.");

            // There is no Jitsi config option to say whether we should be used
            // to handle SIP and TEL protocols, so we must be logging-in with this
            // user for the 1st time.  In that case, if we have calling function
            // make ourselves the default calling app by updating the
            // registry.  Otherwise mark us as not the default
            if (ConfigurationUtils.isCallingEnabled())
            {
                logger.info("Calling enabled - make us the default app.");
                ConfigurationUtils.setProtocolURLHandler(true);
                UrlHandlerApp.setAsOsUrlHandlerApp();
            }
            else
            {
                logger.info("Calling NOT enabled - unset the default app.");
                ConfigurationUtils.setProtocolURLHandler(false);
                UrlHandlerApp.unsetAsOsUrlHandlerApp();
            }
        }
        else
        {
            logger.debug("Current user already has default URL handler config");
            // There is a Jitsi config option to say whether we should be used
            // to handle SIP and TEL protocols.
            boolean isUrlProtocolHandlerApp =
                Boolean.parseBoolean(urlProtocolHandlerAppString);

            // 1st check if it is valid (we must support calling function to be
            // able to handle sip/tel/callto URIs).
            if (!ConfigurationUtils.isCallingEnabled() && isUrlProtocolHandlerApp)
            {
                logger.info("We were the default tel URI handler, but we don't support calling");
                ConfigurationUtils.setProtocolURLHandler(false);
                isUrlProtocolHandlerApp = false;
            }

            // Then check whether the registry setting matches the config and,
            // if it doesn't, update it.
            if(UrlHandlerApp.isAppOsUrlHandler() != isUrlProtocolHandlerApp)
            {
                logger.debug("Updating URL protocol handler app registry entry");

                if(isUrlProtocolHandlerApp)
                {
                    UrlHandlerApp.setAsOsUrlHandlerApp();
                }
                else
                {
                    UrlHandlerApp.unsetAsOsUrlHandlerApp();
                }
            }
        }

        // Add a listener so if the config changes we can update the registry
        // entry accordingly.
        propChangeListener = new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent e)
            {
                boolean enabled = (Boolean) e.getNewValue();
                logger.info(ConfigurationUtils.URL_PROTOCOL_HANDLER_APP_PROP +
                                            " has been changed to " + enabled);

                if (enabled)
                {
                    UrlHandlerApp.setAsOsUrlHandlerApp();
                }
                else
                {
                    UrlHandlerApp.unsetAsOsUrlHandlerApp();
                }
            }
        };
        getConfigurationService().user().addPropertyChangeListener(
            ConfigurationUtils.URL_PROTOCOL_HANDLER_APP_PROP, propChangeListener);
    }

    @Override
    public void stop(BundleContext bundleContext)
    {
        // The UrlProtocolHandler bundle is available on Windows only.
        if (!OSUtils.IS_WINDOWS)
            return;

        logger.info("UrlProtocolHandler plugin ... [STOPPED]");

        getConfigurationService().user().removePropertyChangeListener(
            ConfigurationUtils.URL_PROTOCOL_HANDLER_APP_PROP, propChangeListener);
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
            configService =
                ServiceUtils.getService(context, ConfigurationService.class);
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
            resourcesService =
                ServiceUtils.getService(context, ResourceManagementService.class);
        }

        return resourcesService;
    }
}
