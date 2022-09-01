/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.argdelegation;

import java.awt.Desktop;
import java.awt.desktop.OpenURIEvent;
import java.awt.desktop.OpenURIHandler;
import java.lang.reflect.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.launchutils.*;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * Activates the <tt>ArgDelegationService</tt> and registers a URI delegation
 * peer with the util package arg manager so that we would be notified when the
 * application receives uri arguments.
 *
 * @author Emil Ivov
 */
public class ArgDelegationActivator
    implements BundleActivator
{
    /**
     * A reference to the bundle context that is currently in use.
     */
    private static BundleContext bundleContext = null;

    /**
     * A reference to the delegation peer implementation that is currently
     * handling uri arguments.
     */
    private ArgDelegationPeerImpl delegationPeer = null;

    /**
     * A reference to the <tt>UIService</tt> currently in use in
     * SIP Communicator.
     */
    private static UIService uiService = null;

    private static ConfigurationService configurationService = null;

    /**
     * Starts the arg delegation bundle and registers the delegationPeer with
     * the util package URI manager.
     *
     * @param bc a reference to the currently active bundle context.
     */
    public void start(BundleContext bc)
    {
        bundleContext = bc;
        delegationPeer = new ArgDelegationPeerImpl(bc);
        bc.addServiceListener(delegationPeer);

        //register our instance of delegation peer.
        LaunchArgHandler.getInstance().setDelegationPeer(delegationPeer);

        // Register as a service so that other services may know if we are
        // started.
        bundleContext.registerService(ArgDelegationPeer.class.getName(),
            delegationPeer,
            null);

        if(OSUtils.IS_MAC)
        {
            // For Mac, we register which URIs we will respond to in the
            // "cfbundleurltypes.start" section of jitsi/resources/install/build.xml.
            // Currently these are sip, callto, tel, maxuccall, xmpp, accessionlogin
            // and, if branded in, an accession meeting URI.  This does mean that we
            // will always register to be able to handle call and chat related URIs,
            // even if the user's CoS does not allow calling/IM, as we cannot know
            // at build time what the user's CoS will be.  This isn't a problem, as
            // we only register as a possible app to handle those URIs, so if the
            // user does try to navigate to one of those URIs, they will be prompted
            // to choose an app to handle them.  If the user were to choose MaX UC
            // to handle a call/chat URI when that user doesn't have calling/IM,
            // the client will simply ignore the URI.
            Desktop desktop = Desktop.getDesktop();

            if(desktop != null)
            {
                // if this fails its most probably cause using older java than
                // 10.6 Update 3 and 10.5 Update 8
                // and older native method for registering uri handlers
                // should be working
                try
                {
                    Method method = desktop.getClass()
                        .getMethod("setOpenURIHandler", OpenURIHandler.class);

                    OpenURIHandler handler = new OpenURIHandler() {
                        public void openURI(OpenURIEvent evt)
                        {
                            delegationPeer.handleUri(evt.getURI().toString());
                        }
                    };

                    method.invoke(desktop, handler);
                }
                catch(Throwable ex)
                {}
            }
        }
    }

    /**
     * Unsets the delegation peer instance that we set when we start this
     * bundle.
     *
     * @param bc an instance of the currently valid bundle context.
     */
    public void stop(BundleContext bc)
    {
        uiService = null;
        bc.removeServiceListener(delegationPeer);
        delegationPeer = null;
        LaunchArgHandler.getInstance().setDelegationPeer(null);
    }

    /**
     * Returns a reference to an UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to an UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static UIService getUIService()
    {
        if(uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
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
}
