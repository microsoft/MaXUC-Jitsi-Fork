/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.globaldisplaydetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.util.StringUtils;
import org.osgi.framework.BundleContext;

import net.java.sip.communicator.service.globaldisplaydetails.GlobalDisplayDetailsService;
import net.java.sip.communicator.service.globaldisplaydetails.event.GlobalAvatarChangeEvent;
import net.java.sip.communicator.service.globaldisplaydetails.event.GlobalDisplayDetailsListener;
import net.java.sip.communicator.service.globaldisplaydetails.event.GlobalDisplayNameChangeEvent;
import net.java.sip.communicator.service.protocol.AccountInfoUtils;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.OperationSetAvatar;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredAccountInfo;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.ServerStoredDetails;
import net.java.sip.communicator.service.protocol.event.AccountManagerEvent;
import net.java.sip.communicator.service.protocol.event.AccountManagerListener;
import net.java.sip.communicator.service.protocol.event.AvatarEvent;
import net.java.sip.communicator.service.protocol.event.AvatarListener;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.protocol.event.ServerStoredDetailsChangeEvent;
import net.java.sip.communicator.service.protocol.event.ServerStoredDetailsChangeListener;
import net.java.sip.communicator.util.AvatarCacheUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.util.account.AccountUtils;

/**
 * The <tt>GlobalDisplayNameImpl</tt> offers generic access to a global
 * display name for the local user.
 * <p>
 *
 * @author Yana Stamcheva
 */
public class GlobalDisplayDetailsImpl
    implements  GlobalDisplayDetailsService,
                RegistrationStateChangeListener,
                ServerStoredDetailsChangeListener,
                AvatarListener,
                AccountManagerListener
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(GlobalDisplayDetailsImpl.class);

    /**
     * Property to disable auto answer menu.
     */
    private static final String GLOBAL_DISPLAY_NAME_PROP =
        "net.java.sip.communicator.impl.gui.main.presence.GLOBAL_DISPLAY_NAME";

    /**
     * The display details listeners list.
     */
    private final List<GlobalDisplayDetailsListener> displayDetailsListeners
        = new ArrayList<>();

    /**
     * Link to AccountManager so we can register for its events
     */
    private final AccountManager mAccountManager;

    /**
     * The current first name.
     */
    private String currentFirstName;

    /**
     * The current last name.
     */
    private String currentLastName;

    /**
     * The current display name.
     */
    private String currentDisplayName;

    /**
     * The provisioned display name.
     */
    private String provisionedDisplayName;

    /**
     * The global avatar.
     */
    private static BufferedImageFuture globalAvatar;

    /**
     * The global display name.
     */
    private String globalDisplayName;

    /**
     * Creates an instance of <tt>GlobalDisplayDetailsImpl</tt>.
     */
    public GlobalDisplayDetailsImpl(BundleContext bundleContext)
    {
        mAccountManager
            = ServiceUtils.getService(bundleContext, AccountManager.class);
        mAccountManager.addListener(this);

        provisionedDisplayName
            = GlobalDisplayDetailsActivator.getConfigurationService().user()
                .getString(GLOBAL_DISPLAY_NAME_PROP, null);

        setGlobalDisplayAvatar(GlobalDisplayDetailsActivator.getResources()
            .getBufferedImage("service.gui.DEFAULT_USER_PHOTO"));
        Iterator<ProtocolProviderService> providersIter
            = AccountUtils.getRegisteredProviders().iterator();
        while (providersIter.hasNext())
        {
            providersIter.next().addRegistrationStateChangeListener(this);
        }
    }

    /**
     * Returns the global display name to be used to identify the local user.
     *
     * @return a string representing the global local user display name
     */
    public String getGlobalDisplayName()
    {
        if (!StringUtils.isNullOrEmpty(provisionedDisplayName))
            return provisionedDisplayName;

        return globalDisplayName;
    }

    /**
     * Sets the global local user display name.
     *
     * @param displayName the string representing the display name to set as
     * a global display name
     */
    public void setGlobalDisplayName(String displayName)
    {
        globalDisplayName = displayName;
    }

    /**
     * Returns the global avatar for the local user.
     *
     * @return a byte array containing the global avatar for the local user
     */
    public BufferedImageFuture getGlobalDisplayAvatar()
    {
        return globalAvatar;
    }

    /**
     * Sets the global display avatar for the local user.
     *
     * @param avatar the byte array representing the avatar to set
     */
    public void setGlobalDisplayAvatar(BufferedImageFuture avatar)
    {
        if (avatar != null)
        {
            logger.debug("Set display avatar");
            globalAvatar = avatar;

            fireGlobalAvatarEvent(avatar);
        }
    }

    /**
     * Adds the given <tt>GlobalDisplayDetailsListener</tt> to listen for change
     * events concerning the global display details.
     *
     * @param l the <tt>GlobalDisplayDetailsListener</tt> to add
     */
    public void addGlobalDisplayDetailsListener(GlobalDisplayDetailsListener l)
    {
        synchronized (displayDetailsListeners)
        {
            if (!displayDetailsListeners.contains(l))
                displayDetailsListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>GlobalDisplayDetailsListener</tt> listening for
     * change events concerning the global display details.
     *
     * @param l the <tt>GlobalDisplayDetailsListener</tt> to remove
     */
    public void removeGlobalDisplayDetailsListener(
        GlobalDisplayDetailsListener l)
    {
        synchronized (displayDetailsListeners)
        {
            if (displayDetailsListeners.contains(l))
                displayDetailsListeners.remove(l);
        }
    }

    @Override
    public void handleAccountManagerEvent(AccountManagerEvent event)
    {
        /*
         * We need to listen to registrationStateEvents for all
         * ProtocolProviderServices.  In the constructor for this class
         * we look for all registered providers, and listen on them.  We
         * also add ourselves as listeners to any ProtocolProviderServices
         * that register as bundles after this class is instantiated by the
         * serviceChanged method in the GlobalDisplayDetailsActivator.
         * This should mean that we add this class as a listener whether it
         * is instantiated before or after the ProtocolProviderServices
         * are created.
         *
         * But there is a window in ProtocolProviderFactory.loadAccount(),
         * in which if this class is instantiated, we won't be a listener:
         * - this class will have been instantiated too late to catch the
         * serviceChanged method kicked off from the ProtocolProviderService
         * registering as a service, but too early for the
         * ProtocolProviderFactory to have added the associated account
         * in its list of registered accounts.
         *
         * To fix this window condition, we listen for the
         * STORED_ACCOUNTS_LOADED event which is fired after the
         * ProtocolProviderFactory adds its accounts as 'registered'
         * - so will be returned by the call to
         * AccountUtils.getRegisteredProviders().
         */
        logger.debug("Received account manager event " + event.getType());

        if (AccountManagerEvent.STORED_ACCOUNTS_LOADED == event.getType())
        {
            Iterator<ProtocolProviderService> providersIter =
                AccountUtils.getRegisteredProviders().iterator();

            while (providersIter.hasNext())
            {
                providersIter.next().addRegistrationStateChangeListener(this);
            }
        }
    }

    /**
     * Updates account information when a protocol provider is registered.
     * @param evt the <tt>RegistrationStateChangeEvent</tt> that notified us
     * of the change
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        logger.debug("registrationStateChanged event " + evt +
            " for provider " + evt.getProvider());

        ProtocolProviderService protocolProvider = evt.getProvider();

        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            /*
             * Check the support for OperationSetServerStoredAccountInfo prior
             * to starting the Thread because only a couple of the protocols
             * currently support it and thus starting a Thread that is not going
             * to do anything useful can be prevented.
             */
            OperationSetServerStoredAccountInfo accountInfoOpSet
                = protocolProvider.getOperationSet(
                        OperationSetServerStoredAccountInfo.class);

            if (accountInfoOpSet != null)
            {
                /*
                 * FIXME Starting a separate Thread for each
                 * ProtocolProviderService is uncontrollable because the
                 * application is multi-protocol and having multiple accounts is
                 * expected so one is likely to end up with a multitude of
                 * Threads. Besides, it not very clear when retrieving the first
                 * and last name is to stop so one ProtocolProviderService being
                 * able to supply both the first and the last name may be
                 * overwritten by a ProtocolProviderService which is able to
                 * provide just one of them.
                 */
                new UpdateAccountInfo(protocolProvider, accountInfoOpSet, false)
                    .start();
            }

            OperationSetAvatar avatarOpSet
                = protocolProvider.getOperationSet(OperationSetAvatar.class);
            if (avatarOpSet != null)
                avatarOpSet.addAvatarListener(this);

            OperationSetServerStoredAccountInfo serverStoredAccountInfo
                = protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);
            if (serverStoredAccountInfo != null)
                serverStoredAccountInfo.addServerStoredDetailsChangeListener(
                        this);
        }
        else if (evt.getNewState().equals(RegistrationState.UNREGISTERING)
                || evt.getNewState().equals(RegistrationState.CONNECTION_FAILED))
        {
            OperationSetAvatar avatarOpSet
                = protocolProvider.getOperationSet(OperationSetAvatar.class);
            if (avatarOpSet != null)
                avatarOpSet.removeAvatarListener(this);

            OperationSetServerStoredAccountInfo serverStoredAccountInfo
                = protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);
            if (serverStoredAccountInfo != null)
                serverStoredAccountInfo.removeServerStoredDetailsChangeListener(
                        this);
        }
    }

    /**
     * Called whenever a new avatar is defined for one of the protocols that we
     * have subscribed for.
     *
     * @param event the event containing the new image
     */
    public void avatarChanged(AvatarEvent event)
    {
        BufferedImageFuture avatar = event.getNewAvatar();

        if (avatar.resolve() != null)
        {
            AvatarCacheUtils.cacheAvatar(
                event.getSourceProvider(), avatar);

            setGlobalDisplayAvatar(avatar);
        }
        else
        {
            // Avatar has been removed, so delete from cache for this
            // protocol provider
            AvatarCacheUtils.deleteCachedAvatar(event.getSourceProvider());

            setGlobalDisplayAvatar(GlobalDisplayDetailsActivator.getResources()
                .getBufferedImage("service.gui.DEFAULT_USER_PHOTO"));
        }
    }

    /**
     * Registers a ServerStoredDetailsChangeListener with the operation sets
     * of the providers, if a provider change its name we use it in the UI.
     *
     * @param evt the <tt>ServerStoredDetailsChangeEvent</tt>
     * the event for name change.
     */
    public void serverStoredDetailsChanged(ServerStoredDetailsChangeEvent evt)
    {
        if(!StringUtils.isNullOrEmpty(provisionedDisplayName))
            return;

        if(evt.getNewValue() instanceof
                ServerStoredDetails.DisplayNameDetail
            && (evt.getEventID() == ServerStoredDetailsChangeEvent.DETAIL_ADDED
                || evt.getEventID()
                    == ServerStoredDetailsChangeEvent.DETAIL_REPLACED))
        {
            ProtocolProviderService protocolProvider = evt.getProvider();
            OperationSetServerStoredAccountInfo accountInfoOpSet
                = protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);

            new UpdateAccountInfo(  evt.getProvider(),
                                    accountInfoOpSet,
                                    true).start();
        }
    }

    /**
     * Queries the operations sets to obtain names and display info.
     * Queries are done in separate thread.
     */
    private class UpdateAccountInfo
        extends Thread
    {
        /**
         * The protocol provider.
         */
        private ProtocolProviderService protocolProvider;

        /**
         * The account info operation set to query.
         */
        private OperationSetServerStoredAccountInfo accountInfoOpSet;

        /**
         * Indicates if the display name and avatar should be updated from this
         * provider even if they already have values.
         */
        private boolean isUpdate;

        /**
         * Constructs with provider and opset to use.
         * @param protocolProvider the provider.
         * @param accountInfoOpSet the opset.
         * @param isUpdate indicates if the display name and avatar should be
         * updated from this provider even if they already have values.
         */
        UpdateAccountInfo(
            ProtocolProviderService protocolProvider,
            OperationSetServerStoredAccountInfo accountInfoOpSet,
            boolean isUpdate)
        {
            super("UpdateAccountInfoThread");
            this.protocolProvider = protocolProvider;
            this.accountInfoOpSet = accountInfoOpSet;
        }

        @Override
        public void run()
        {
            try
            {
                logger.debug("run UpdateAccountInfo thread for " + protocolProvider);
                BufferedImageFuture avatar
                    = AvatarCacheUtils
                        .getCachedAvatar(protocolProvider);

                if (avatar == null ||
                    AvatarCacheUtils.shouldInvalidateCache(protocolProvider))
                {
                    if (avatar != null)
                    {
                        logger.info("Avatar for account " + protocolProvider +
                                    " has expired - let's get a new one.");
                    }

                    BufferedImageFuture accountImage
                        = AccountInfoUtils
                            .getImage(accountInfoOpSet);

                    // do not set empty images
                    if (accountImage != null)
                    {
                        logger.debug("Got image, add it to the cache");
                        setGlobalDisplayAvatar(accountImage);

                        AvatarCacheUtils.cacheAvatar(
                            protocolProvider, accountImage);
                    }
                }
                else
                {
                    setGlobalDisplayAvatar(avatar);
                }

                if(!StringUtils.isNullOrEmpty(provisionedDisplayName)
                    || (!StringUtils.isNullOrEmpty(globalDisplayName) && !isUpdate))
                    return;

                if (currentFirstName == null)
                {
                    String firstName = AccountInfoUtils
                        .getFirstName(accountInfoOpSet);

                    if (!StringUtils.isNullOrEmpty(firstName))
                    {
                        currentFirstName = firstName;
                    }
                }

                if (currentLastName == null)
                {
                    String lastName = AccountInfoUtils
                        .getLastName(accountInfoOpSet);

                    if (!StringUtils.isNullOrEmpty(lastName))
                    {
                        currentLastName = lastName;
                    }
                }

                if (currentFirstName == null && currentLastName == null)
                {
                    String displayName = AccountInfoUtils
                        .getDisplayName(accountInfoOpSet);

                    if (displayName != null)
                        currentDisplayName = displayName;
                }

                setGlobalDisplayName();
                }
            catch (IllegalStateException e)
            {
                logger.error("Unable to update account info", e);
            }
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        protected void setGlobalDisplayName()
        {
            String accountName = null;
            if (!StringUtils.isNullOrEmpty(currentFirstName))
            {
                accountName = currentFirstName;
            }

            if (!StringUtils.isNullOrEmpty(currentLastName))
            {
                /*
                 * If accountName is null, don't use += because
                 * it will make the accountName start with the
                 * string "null".
                 */
                if (StringUtils.isNullOrEmpty(accountName))
                    accountName = currentLastName;
                else
                    accountName += " " + currentLastName;
            }

            if (currentFirstName == null && currentLastName == null)
            {
                if (currentDisplayName != null)
                    accountName = currentDisplayName;
            }

            globalDisplayName = accountName;

            if (!StringUtils.isNullOrEmpty(globalDisplayName))
            {
                fireGlobalDisplayNameEvent(globalDisplayName);
            }
        }
    }

    /**
     * Notifies all interested listeners of a global display details change.
     *
     * @param displayName the new display name
     */
    private void fireGlobalDisplayNameEvent(String displayName)
    {
        List<GlobalDisplayDetailsListener> listeners;
        synchronized (displayDetailsListeners)
        {
            listeners = Collections.unmodifiableList(displayDetailsListeners);
        }

        Iterator<GlobalDisplayDetailsListener> listIter
            = listeners.iterator();
        while (listIter.hasNext())
        {
            listIter.next().globalDisplayNameChanged(
                new GlobalDisplayNameChangeEvent(this, displayName));
        }
    }

    /**
     * Notifies all interested listeners of a global display details change.
     *
     * @param avatar the new avatar
     */
    private void fireGlobalAvatarEvent(BufferedImageFuture avatar)
    {
        List<GlobalDisplayDetailsListener> listeners;
        synchronized (displayDetailsListeners)
        {
            listeners = Collections.unmodifiableList(displayDetailsListeners);
        }

        Iterator<GlobalDisplayDetailsListener> listIter
            = listeners.iterator();
        while (listIter.hasNext())
        {
            listIter.next().globalDisplayAvatarChanged(
                new GlobalAvatarChangeEvent(this, avatar));
        }
    }
}
