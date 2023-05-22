/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.contactlist;

import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.*;

/**
 * @author Emil Ivov
 */
public class ContactlistActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(ContactlistActivator.class);

    private static MetaContactListServiceImpl mclServiceImpl  = null;

    private static FileAccessService fileAccessService;

    private static AccountManager accountManager;

    private static BundleContext bundleContext;

    /**
     * A reference to the ConfigurationService implementation instance that
     * is currently registered with the bundle context.
     */
    private static ConfigurationService configurationService = null;

    private static PhoneNumberUtilsService sPhoneNumberUtils;

    private static UIService sUiService;

    private static ConferenceService sConferenceService;

    /**
     * Reference to the WISPA interface, used for sending and receiving data via
     * a websocket connection using our custom API.
     */
    private static WISPAService sWISPAService;

    /**
     * Called when this bundle is started.
     *
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context)
    {
        bundleContext = context;

        logger.debug("Service Impl: " + getClass().getName() + " [  STARTED ]");

        mclServiceImpl = new MetaContactListServiceImpl();

        //reg the icq account man.
        context.registerService(MetaContactListService.class.getName(),
                mclServiceImpl, null);

        mclServiceImpl.start(context);

        // Add the MCL manager as a state dumper:
        DiagnosticsServiceRegistrar.registerStateDumper(mclServiceImpl, bundleContext);

        logger.debug("Service Impl: " + getClass().getName() + " [REGISTERED]");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
        logger.trace("Stopping the contact list.");
        if (mclServiceImpl != null)
            mclServiceImpl.stop(context);
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }

    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(accountManager == null)
        {
            accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);
        }
        return accountManager;
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
        if (configurationService == null)
        {
            ServiceReference<?> confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService)bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * @return a reference to the PhoneNumberUtils service
     */
    public static PhoneNumberUtilsService getPhoneNumberUtils()
    {
        if (sPhoneNumberUtils == null)
        {
            sPhoneNumberUtils = ServiceUtils.getService(bundleContext,
                                                PhoneNumberUtilsService.class);
        }

        return sPhoneNumberUtils;
    }

    /**
     * @return a reference to the UI Service
     */
    public static UIService getUIService()
    {
        if (sUiService == null)
        {
            sUiService = ServiceUtils.getService(bundleContext, UIService.class);
        }

        return sUiService;
    }

    /**
     * @return a reference to the Conference Service
     */
    public static ConferenceService getConferenceService()
    {
        if (sConferenceService == null)
        {
            sConferenceService = ServiceUtils.getService(bundleContext,
                                                         ConferenceService.class);
        }

        return sConferenceService;
    }

    /**
     * @return a reference to the WISPA (Websocket Interface Simplification
     * Plugin API) service
     */
    public static WISPAService getWISPAService()
    {
        if (sWISPAService == null)
        {
            sWISPAService = ServiceUtils.getService(bundleContext,
                                                    WISPAService.class);
        }

        return sWISPAService;
    }

    /**
     * @return a reference to the MetaContactList Service
     */
    public static MetaContactListService getContactListService()
    {
        return mclServiceImpl;
    }
}
