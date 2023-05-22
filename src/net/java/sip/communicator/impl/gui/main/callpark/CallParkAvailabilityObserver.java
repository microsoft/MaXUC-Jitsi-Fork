/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.callpark;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetCallPark;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkListener;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbit;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbitState;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;

/**
 * The <tt>CallParkAvailabilityObserver</tt> is just a helper class to observe
 * if call park feature is available or not, and when it does become available
 * it notifies Electron of the update via WISPA.
 */
public class CallParkAvailabilityObserver
{
    private final Logger logger = Logger.getLogger(CallParkAvailabilityObserver.class);

    private static final ConfigurationService configService =
            GuiActivator.getConfigurationService();

    /**
     * Indicates whether Call Park is enabled and available for this account. Initialized to 'false' so that we don't
     * display any call park UI until we know there is an OperationSet to control it.
     */
    private boolean callParkConfigured = false;

    /**
     * Whether the client is allowed to make calls (set in the CoS) and VoIP is enabled by the user in options.
     * Assume VoIP is active until we have received the CoS and checked config.
     */
    private boolean voipEnabled = true;

    /**
     * Starts observing if the call park is available. Initially invisible, it will only be set visible when the
     * OperationSetCallPark is found.
     */
    public void observeCallParkAvailability()
    {
        List<ProtocolProviderService> providers =
                GuiActivator.getRegisteredProviders(OperationSetCallPark.class);

        if (providers.isEmpty())
        {
            // This can occasionally happen, when the Call Park opset is taking
            // a while to be registered.  In which case we need to listen for it
            // to be registered.
            logger.info("No Call Park OpSet on start up");

            ServiceListener serviceListener = new ServiceListener()
            {
                @Override
                public void serviceChanged(ServiceEvent event)
                {
                    logger.debug("Service changed " + event);
                    ServiceReference<?> serviceRef = event.getServiceReference();

                    // If the event is caused by a bundle being stopped, we
                    // don't want to know.
                    if (serviceRef.getBundle().getState() == Bundle.STOPPING)
                        return;

                    Object service = GuiActivator.bundleContext.getService(serviceRef);

                    if (service instanceof ProtocolProviderService)
                    {
                        ProtocolProviderService provider =
                                (ProtocolProviderService) service;
                        OperationSet operationSet =
                                provider.getOperationSet(OperationSetCallPark.class);

                        if (operationSet != null)
                        {
                            // We've got the call park op set so handle it.
                            // Unregister the service listener as we don't need it.
                            handleCallParkProviderAdded(provider);
                            GuiActivator.bundleContext.removeServiceListener(this);
                        }
                    }
                }
            };

            GuiActivator.bundleContext.addServiceListener(serviceListener);

            // There is a tiny tiny window where the op set is registered after
            // we try to get the providers but before we register the listener.
            // This code covers that.
            providers = GuiActivator.getRegisteredProviders(OperationSetCallPark.class);
            if (!providers.isEmpty())
            {
                handleCallParkProviderAdded(providers.get(0));
                GuiActivator.bundleContext.removeServiceListener(serviceListener);
            }
        }
        else if (providers.size() == 1)
        {
            // This is the most common case, the tools option is created after
            // the Call Park op set has been registered.
            logger.debug("Singleton call park opset");
            handleCallParkProviderAdded(providers.get(0));
        }
        else
        {
            // Unexpected, should only ever be one.  As a guess, register anyway
            // but it may well be wrong.
            logger.error("Too many call park providers added! " + providers);
            handleCallParkProviderAdded(providers.get(0));
        }
    }

    /**
     * Handle the case that a protocol provider that implements the Call Park operation set has been added.
     *
     * @param provider the provider that has been added.
     */
    private void handleCallParkProviderAdded(ProtocolProviderService provider)
    {
        // Registers a call park listener, and a class of service listener so that
        // it can figure out if call park is allowed or not.
        final OperationSetCallPark callParkOpSet =
                provider.getOperationSet(OperationSetCallPark.class);

        // Lock to prevent both the CoS checking code and the call park config
        // checking code from try to notify Electron of the availability of the
        // call park at the same time.
        final Object callParkLock = new Object();

        callParkOpSet.registerListener(new CallParkListener()
        {
            @Override
            public void onOrbitStateChanged(CallParkOrbit orbit, CallParkOrbitState oldState)
            {
            }

            @Override
            public void onOrbitListChanged()
            {
            }

            @Override
            public void onCallParkEnabledChanged()
            {
                logger.debug("Call park enabled changed to " + callParkOpSet.isEnabled());
                setCallParkVisibility();
            }

            @Override
            public void onCallParkAvailabilityChanged()
            {
                logger.debug("Call park availability changed to " + callParkOpSet.isCallParkAvailable());
                setCallParkVisibility();
            }

            private void setCallParkVisibility()
            {
                synchronized (callParkLock)
                {
                    callParkConfigured = callParkOpSet.isCallParkAvailable() &&
                                         callParkOpSet.isEnabled();
                    notifyCallParkVisibility();
                }
            }
        });

        synchronized (callParkLock)
        {
            callParkConfigured = callParkOpSet.isCallParkAvailable() &&
                                 callParkOpSet.isEnabled();
            notifyCallParkVisibility();
        }

        ClassOfServiceService cos = GuiActivator.getCosService();
        cos.getClassOfService(classOfService -> {
            synchronized (callParkLock)
            {
                // We could use ConfigurationUtils.isVoIPEnabledInCoS() for
                // the VoIP enabled by CoS setting, but there is a race
                // condition between the CoS storeCosFieldsInConfig() method
                // saving this setting to the config file, and this callback
                // being called (where isVoIPEnabledInCoS() would then read
                // that same config file).
                voipEnabled =
                        classOfService.getSubscribedMashups().isVoipAllowed() &&
                        ConfigurationUtils.isVoIPEnabledByUser();
                logger.info("CoS received, refreshing enabled state of call park");
                notifyCallParkVisibility();
            }
        });
    }

    private void notifyCallParkVisibility()
    {
        logger.debug("Set call park enabled " +
                     "based on voipEnabled (" + voipEnabled +
                     ") and callParkConfigured (" + callParkConfigured + ").");

        boolean callParkAvailable = callParkConfigured && voipEnabled;
        configService.user().setProperty(OperationSetCallPark.CALL_PARK_ACTIVE, callParkAvailable);

        // Pass on whether Electron should show its call park menu item by saving
        // that to config then sending a WISPA settings update.
        WISPAService wispaService = GuiActivator.getWISPAService();
        if (wispaService != null)
        {
            wispaService.notify(WISPANamespace.SETTINGS, WISPAAction.UPDATE);
        }
        else
        {
            logger.warn("Could not notify WISPA about call park state update.");
        }
    }
}
